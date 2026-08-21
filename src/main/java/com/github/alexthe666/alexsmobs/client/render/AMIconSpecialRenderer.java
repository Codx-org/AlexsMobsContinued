package com.github.alexthe666.alexsmobs.client.render;

import net.minecraft.world.item.ItemStack;

/**
 * The ISTER's replacement on >=1.21.4: a {@code minecraft:special} item-model renderer (type id
 * {@code alexsmobs:icon}) that routes back into {@link AMItemstackRenderer#renderByItem}, whose
 * body was deliberately kept compiling on every node for exactly this re-wiring. It serves the three
 * pure-icon items — {@code tab_icon}, {@code fancy_item}, {@code effect_item} — so the creative-tab
 * mob cycle and the 59 advancement icons animate again (#45/#48), plus {@code transmutation_table}
 * (#67), {@code mysterious_worm} (#75) and {@code shattered_dimensional_carver} (#92). The rest of the
 * items the old ISTER drew (shield of the deep, stink ray, …) keep their #21/#23 rebuilt static models:
 * membership is decided by whether the item's {@code renderByItem} branch picks a different <i>model</i>
 * from the display context, not by whether it reads it — the carver only nudges on it, and since #96 the
 * 26.x arm below is handed the real context anyway. The item set itself lives in {@code DataPackMigration}'s
 * {@code LIVE_ICON_ITEMS} — it is chosen by which item models get a {@code minecraft:special}
 * definition, not by anything here.
 *
 * <p><b>Animation and the GUI item atlas (#107).</b> That earlier claim here — that
 * {@code SpecialModelWrapper.update} "unconditionally calls {@code ItemStackRenderState.setAnimated()}"
 * — was <em>false</em>, on every era. The bytecode (1.21.6/1.21.8/1.21.9/1.21.11/26.2, identical
 * shape: {@code hasFoil() -> ifeq}) calls it only inside the {@code if (stack.hasFoil())} branch, and
 * none of these icons has foil. What {@code update} does unconditionally is append the value
 * {@link #extractArgument} returns to the render state's <b>model identity</b> — and from 1.21.6 that
 * identity is the key of the GUI item atlas ({@code GuiRenderer.atlasPositions}; a
 * {@code DynamicAtlasAllocator} inside {@code GuiItemAtlas} on 26.x). {@link ItemStack} does not
 * override {@code equals}/{@code hashCode}, so returning a fresh copy per call handed every icon a
 * <em>fresh atlas slot every frame</em>: the icons did animate, but only as a side effect of missing
 * the cache, and once the slots ran out vanilla destroyed and rebuilt the whole atlas and re-rendered
 * every GUI item in the frame, vanilla's included.
 *
 * <p>So the two halves are separated deliberately. {@link #extractArgument} returns a <b>canonical</b>
 * stack per (item, custom_data), which gives each icon exactly one stable atlas key; and
 * {@code mixin.client.ItemStackRenderStateAtlasMixin} calls {@code setAnimated()} when it sees one of
 * those canonical arguments appended, which is the supported way to ask for the animated path —
 * vanilla then re-renders the item <em>in place</em> in its existing slot once per frame
 * ({@code animatedInPlace = isAnimated() && position != null}) instead of allocating a new one.
 * 1.21.4/1.21.5 have no atlas at all and re-render every frame regardless, so neither half matters
 * there, and the mixin is not compiled below 1.21.6.
 *
 * <p>Registration is loader-split. Vanilla keeps {@code SpecialModelRenderers.ID_MAPPER} private on
 * every era (the "public" reading in dev jars is fabric-api's transitive access widener). NeoForge
 * has a dedicated mod-bus event (see ClientProxy); Forge has neither an event nor an AT for it, and
 * classic-Forge SRG member names make a name-based lookup fragile — so {@link #register()} finds the
 * mapper <b>by field type</b>, which is unique in the class and immune to mappings.
 *
 * <p>Below 1.21.4 this compiles to a dead plain class: the implements clause and every override are
 * gated, and nothing registers or references it there (the ISTER still exists and is wired — Forge/
 * NeoForge via IClientItemExtensions, Fabric via BuiltinItemRendererRegistry).
 */
public class AMIconSpecialRenderer
        //? if >=1.21.4
        /*implements net.minecraft.client.renderer.special.SpecialModelRenderer<ItemStack>*/
{

    /**
     * One shared legacy renderer instance: it holds the {@code renderedEntites} display-entity
     * cache, which all three icon items should share the way the single wired ISTER used to.
     */
    private static final AMItemstackRenderer RENDERER = new AMItemstackRenderer();

    /**
     * Canonical arguments, keyed by (item id, custom_data). See the atlas note in the class javadoc:
     * this map is what makes an icon's model identity stable from one frame to the next. Bounded
     * defensively — the real key space is the six {@code LIVE_ICON_ITEMS} × the display tags the
     * advancement icons carry, i.e. tens of entries, so the cap should never be reached.
     */
    private static final java.util.Map<String, ItemStack> CANONICAL_ARGS = new java.util.concurrent.ConcurrentHashMap<>();

    /** Identity view of {@link #CANONICAL_ARGS}'s values, for {@link #isCanonicalArgument}. */
    private static final java.util.Set<Object> CANONICAL_IDENTITIES = java.util.Collections.synchronizedSet(
            java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>()));

    private static final int CANONICAL_CAP = 512;

    //? if >=1.21.4
    /*@Override*/
    public ItemStack extractArgument(ItemStack stack) {
        // The stack IS the argument: fancy_item/effect_item/tab_icon all read their display NBT
        // (custom data) off it inside renderByItem. Never hand back the caller's stack (the render
        // state would alias a live one) and never hand back a fresh copy either (#107 — the value
        // becomes part of the model identity, which is the GUI atlas key from 1.21.6). One canonical
        // copy per distinct (item, custom_data) satisfies both.
        if (stack == null || stack.isEmpty()) {
            return stack;
        }
        String key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem())
                + "|" + com.github.alexthe666.alexsmobs.misc.AMCompat.getTag(stack);
        ItemStack canonical = CANONICAL_ARGS.get(key);
        if (canonical == null) {
            if (CANONICAL_ARGS.size() >= CANONICAL_CAP) {
                CANONICAL_ARGS.clear();
                CANONICAL_IDENTITIES.clear();
            }
            canonical = stack.copy();
            CANONICAL_ARGS.put(key, canonical);
            CANONICAL_IDENTITIES.add(canonical);
        }
        return canonical;
    }

    /**
     * True for a value {@link #extractArgument} produced. Identity, not equality — the point is to
     * recognise our own arguments among everything else a render state's identity is built from.
     * Called from {@code ItemStackRenderStateAtlasMixin} on 1.21.6+.
     */
    public static boolean isCanonicalArgument(Object element) {
        return element instanceof ItemStack && CANONICAL_IDENTITIES.contains(element);
    }

    // 1.21.4–1.21.8: the special renderer still *draws*, straight into the buffer source the item
    // pipeline handed us.
    //? if >=1.21.4 && <1.21.9 {
    /*@Override
    public void render(ItemStack stack, net.minecraft.world.item.ItemDisplayContext displayContext,
                       com.mojang.blaze3d.vertex.PoseStack poseStack,
                       net.minecraft.client.renderer.MultiBufferSource buffers,
                       int packedLight, int packedOverlay, boolean hasFoil) {
        if (stack != null && !stack.isEmpty()) {
            RENDERER.renderByItem(stack, displayContext, poseStack, buffers, packedLight, packedOverlay);
        }
    }
    *///?}

    // 1.21.9/1.21.10/1.21.11: submit-only. The legacy body records into an AMSubmitBuffers and the
    // flush replays it through SubmitNodeCollector.submitCustomGeometry; the nested entity render
    // inside drawEntityOnScreen unwraps the same wrapper back into the raw collector
    // (AMRenderCompat.renderEntity).
    //? if >=1.21.9 && <26 {
    /*@Override
    public void submit(ItemStack stack, net.minecraft.world.item.ItemDisplayContext displayContext,
                       com.mojang.blaze3d.vertex.PoseStack poseStack,
                       net.minecraft.client.renderer.SubmitNodeCollector collector,
                       int packedLight, int packedOverlay, boolean hasFoil, int outlineColor) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        com.github.alexthe666.alexsmobs.client.render.compat.AMSubmitBuffers buffers =
                new com.github.alexthe666.alexsmobs.client.render.compat.AMSubmitBuffers(collector);
        RENDERER.renderByItem(stack, displayContext, poseStack, buffers, packedLight, packedOverlay);
        buffers.flush();
    }
    *///?}

    // 26.x dropped the ItemDisplayContext parameter, so the context is borrowed from one frame up the
    // stack — ItemStackRenderState still holds it, and mixin/client/ItemStackRenderStateMixin lends it
    // for the duration of the submit that calls us. GUI is only the fallback now (nothing else submits
    // a special model), which matters because the shattered carver's shards pick their pose from it.
    //? if >=26 {
    /*@Override
    public void submit(ItemStack stack, com.mojang.blaze3d.vertex.PoseStack poseStack,
                       net.minecraft.client.renderer.SubmitNodeCollector collector,
                       int packedLight, int packedOverlay, boolean hasFoil, int outlineColor) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        com.github.alexthe666.alexsmobs.client.render.compat.AMSubmitBuffers buffers =
                new com.github.alexthe666.alexsmobs.client.render.compat.AMSubmitBuffers(collector);
        RENDERER.renderByItem(stack, currentDisplayContext(), poseStack, buffers, packedLight, packedOverlay);
        buffers.flush();
    }
    *///?}

    // The borrowed-context stack. A deque rather than a field because an icon's own render can draw
    // another item (the dictionary and the advancement icons both do), which submits a nested render
    // state. Bounded purely as leak insurance: the pop rides an @At("RETURN"), so an exception thrown
    // mid-render would strand an entry, and a stale entry is a wrong pose forever rather than for one
    // frame. Unused below 26, where every arm above is handed the real context outright.
    private static final java.util.ArrayDeque<net.minecraft.world.item.ItemDisplayContext> CONTEXTS = new java.util.ArrayDeque<>();

    public static void pushDisplayContext(net.minecraft.world.item.ItemDisplayContext context) {
        if (CONTEXTS.size() > 32) {
            CONTEXTS.clear();
        }
        CONTEXTS.push(context == null ? net.minecraft.world.item.ItemDisplayContext.GUI : context);
    }

    public static void popDisplayContext() {
        CONTEXTS.poll();
    }

    private static net.minecraft.world.item.ItemDisplayContext currentDisplayContext() {
        net.minecraft.world.item.ItemDisplayContext context = CONTEXTS.peek();
        return context == null ? net.minecraft.world.item.ItemDisplayContext.GUI : context;
    }

    // The unit cube. Extents feed GUI crop/oversize decisions from 1.21.6; if a client session shows
    // a tall mob clipping at the slot edge, widen these rather than scaling the mob.
    //? if >=1.21.6 && <1.21.11 {
    /*@Override
    public void getExtents(java.util.Set<org.joml.Vector3f> out) {
        addExtents(out::add);
    }
    *///?}
    //? if >=1.21.11 {
    /*@Override
    public void getExtents(java.util.function.Consumer<org.joml.Vector3fc> out) {
        addExtents(out::accept);
    }
    *///?}

    private static void addExtents(java.util.function.Consumer<org.joml.Vector3f> out) {
        for (int x = 0; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = 0; z <= 1; z++) {
                    out.accept(new org.joml.Vector3f(x, y, z));
                }
            }
        }
    }

    /**
     * Forge + Fabric registration: put {@code alexsmobs:icon} into the private
     * {@code SpecialModelRenderers.ID_MAPPER}, located by field type. Called from ClientProxy#init,
     * i.e. during mod construction — before the first resource reload can parse an item model.
     * Fails loudly: a miss here would otherwise surface only as silently-static icons.
     */
    //? if >=1.21.4 {
    /*@SuppressWarnings({"rawtypes", "unchecked"})
    public static void register() {
        try {
            net.minecraft.util.ExtraCodecs.LateBoundIdMapper mapper = null;
            for (java.lang.reflect.Field field : net.minecraft.client.renderer.special.SpecialModelRenderers.class.getDeclaredFields()) {
                if (field.getType() == net.minecraft.util.ExtraCodecs.LateBoundIdMapper.class) {
                    field.setAccessible(true);
                    mapper = (net.minecraft.util.ExtraCodecs.LateBoundIdMapper) field.get(null);
                    break;
                }
            }
            if (mapper == null) {
                throw new IllegalStateException("No LateBoundIdMapper field in SpecialModelRenderers");
            }
            mapper.put(com.github.alexthe666.alexsmobs.misc.AMCompat.rl("alexsmobs:icon"), Unbaked.MAP_CODEC);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to register the alexsmobs:icon special model renderer", e);
        }
    }
    *///?}

    /** Stateless: the codec is a unit and bake ignores its context on every era. */
    public static final class Unbaked
            //? if >=26
            /*implements net.minecraft.client.renderer.special.SpecialModelRenderer.Unbaked<ItemStack>*/
            //? if >=1.21.4 && <26
            /*implements net.minecraft.client.renderer.special.SpecialModelRenderer.Unbaked*/
    {
        public static final com.mojang.serialization.MapCodec<Unbaked> MAP_CODEC =
                com.mojang.serialization.MapCodec.unit(new Unbaked());

        //? if >=1.21.4 && <1.21.9 {
        /*@Override
        public net.minecraft.client.renderer.special.SpecialModelRenderer<?> bake(net.minecraft.client.model.geom.EntityModelSet models) {
            return new AMIconSpecialRenderer();
        }
        *///?}
        //? if >=1.21.9 && <26 {
        /*@Override
        public net.minecraft.client.renderer.special.SpecialModelRenderer<?> bake(net.minecraft.client.renderer.special.SpecialModelRenderer.BakingContext context) {
            return new AMIconSpecialRenderer();
        }
        *///?}
        //? if >=26 {
        /*@Override
        public net.minecraft.client.renderer.special.SpecialModelRenderer<ItemStack> bake(net.minecraft.client.renderer.special.SpecialModelRenderer.BakingContext context) {
            return new AMIconSpecialRenderer();
        }
        *///?}

        //? if >=1.21.4
        /*@Override*/
        public com.mojang.serialization.MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}

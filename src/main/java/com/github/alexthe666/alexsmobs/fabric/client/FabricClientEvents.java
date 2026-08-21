package com.github.alexthe666.alexsmobs.fabric.client;

import com.github.alexthe666.alexsmobs.citadel.client.event.EventGetFluidRenderType;
import com.github.alexthe666.alexsmobs.citadel.client.event.EventGetOutlineColor;
import com.github.alexthe666.alexsmobs.citadel.client.event.EventGetStarBrightness;
import com.github.alexthe666.alexsmobs.citadel.client.event.EventPosePlayerHand;
import com.github.alexthe666.alexsmobs.client.event.ClientEvents;
import com.github.alexthe666.alexsmobs.fabric.event.FabricServerEvents;
import com.github.alexthe666.alexsmobs.fabric.forge.common.MinecraftForge;
import com.github.alexthe666.alexsmobs.fabric.forge.client.event.RenderLivingEvent;
import com.github.alexthe666.alexsmobs.fabric.forge.event.TickEvent;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.minecraft.world.phys.HitResult;

/**
 * The client half of the Fabric dispatcher — the counterpart to
 * {@code fabric/event/FabricServerEvents}, and the thing that turns {@code client/event/
 * ClientEvents} from "compiles" into "runs".
 *
 * <p>{@code ClientEvents} itself is shared, byte-identical source on all three loaders: it declares
 * {@code @SubscribeEvent} handlers against Forge event types, which on Fabric are the stubs under
 * {@code fabric/forge/client/event/}. Nothing scans for that annotation here — there is no bus to
 * scan with — so every handler that is live on Fabric is called from this class by name. The
 * consequence is deliberate and checkable: a handler that is <b>not</b> named below does not run,
 * and each one that does not is listed as a divergence in {@code docs/notes/fabric.md} rather than
 * being silently absent.
 *
 * <p><b>What {@link #register} can wire, and what needs a mixin.</b> Everything subscribed in
 * {@code register} rides either the mod's own {@code AMEventBus} — the four vendored-Citadel client
 * hooks, which the {@code mixin/client/**} classes already post on every loader — or a plain Fabric
 * API callback. The rest need a per-frame injection point Fabric API does not expose, so they get a
 * mixin under {@code mixin/fabric/client/**} that calls a {@code fireX} method at the bottom of this
 * file. Done so far: {@code onPreRenderEntity}/{@code onPostRenderEntity}, from
 * {@code FabricLivingEntityRendererMixin}, {@code onCameraSetup}/{@code onRenderWorldLastEvent},
 * from {@code FabricCameraMixin}, and {@code onRenderHand}, from
 * {@code FabricItemInHandRendererMixin}, {@code onFogColor}/{@code onFogDensity}, from
 * {@code FabricFogRendererMixin}, and {@code onRenderNameplate}, from {@code FabricNameTagMixin}.
 * With that last one Wave 3b is complete — every handler {@code ClientEvents} declares is now
 * called from this class.
 *
 * <p>The farseer static overlay is the exception that needs neither: it is drawn from
 * {@link #registerFarseerStatic()} through Fabric API's own HUD hook, because the shared code is a
 * plain static draw call rather than an event handler.
 *
 * <p>The single instance matters: {@code ClientEvents} keeps per-frame state on it
 * ({@code previousLavaVision}), so the handlers must all be called on the same object — which is
 * exactly what {@code MinecraftForge.EVENT_BUS.register(new ClientEvents())} gives the other two
 * loaders.
 */
public final class FabricClientEvents {

    private static final ClientEvents HANDLERS = new ClientEvents();

    private FabricClientEvents() {
    }

    /** Called from {@code ClientProxy#clientInit}, where the other loaders register the bus. */
    public static void register() {
        // The four vendored-Citadel client events. These already fire on Fabric — mixin/client/
        // {LevelRendererMixin, ClientLevelMixin, HumanoidModelMixin, ItemBlockRenderTypesMixin}
        // post them on every loader — so all that was ever missing was a subscriber.
        EventGetOutlineColor.BUS.addListener(HANDLERS::onOutlineEntityColor);
        EventGetStarBrightness.BUS.addListener(HANDLERS::onGetStarBrightness);
        EventPosePlayerHand.BUS.addListener(HANDLERS::onPoseHand);
        EventGetFluidRenderType.BUS.addListener(HANDLERS::onGetFluidRenderType);

        // START only, and only one of the two phases: the handler's own guard is
        // `phase == Phase.START` in shared source, so firing END as well would run it zero extra
        // times and firing END *instead* would run it never.
        ClientTickEvents.START_CLIENT_TICK.register(client ->
                HANDLERS.clientTick(new TickEvent.ClientTickEvent(TickEvent.Phase.START)));

        // The one place the shared source posts an event rather than receiving one: the
        // rocky-chestplate roll cancels the Pre and reposts a Post by hand so the pose it pushed is
        // popped by the normal path. Forge's bus is untyped, so the narrowing is done here.
        MinecraftForge.EVENT_BUS.addListener(event -> {
            if (event instanceof RenderLivingEvent.Post post) {
                HANDLERS.onPostRenderEntity(post);
            }
        });

        registerFarseerStatic();
        registerClientCommands();
        registerEmptyLeftClick();
    }

    /**
     * Swinging at nothing — the one interaction phase that is fired from the client on every loader.
     *
     * <p>Forge patches {@code Minecraft#startAttack}: when the crosshair's hit result is
     * {@code MISS} it posts {@code PlayerInteractEvent.LeftClickEmpty}, which is the <b>only</b>
     * thing that runs an {@link com.github.alexthe666.alexsmobs.item.ILeftClick} item. Two items are
     * that: the falconry glove and the tendon whip. Until this existed, launching a bald eagle or a
     * potoo off your arm — and with it the whole falconry loop, the hooded eagle's first-person
     * flight included — silently did nothing on all 17 Fabric nodes.
     *
     * <p>{@code ClientPreAttackCallback} is the seam, and it needs no mixin: Fabric API fires it from
     * {@code Minecraft#handleKeybinds} just before the attack key is consumed, and its signature is
     * byte-identical from the 1.20.1 fabric-api to 26.2's. Two guards make it Forge's event rather
     * than merely "the attack key moved":
     * <ul>
     *   <li><b>{@code clicks > 0}</b> — the callback also fires on every tick the key is <i>held</i>,
     *       with a zero click count. Forge's fires once per {@code consumeClick}. Without this the
     *       whip would crack 20 times a second.</li>
     *   <li><b>hit result is MISS</b> — a block or an entity under the crosshair is Forge's
     *       {@code LeftClickBlock}/{@code AttackEntityEvent} instead, and the latter is already wired
     *       in {@code FabricServerEvents}.</li>
     * </ul>
     */
    private static void registerEmptyLeftClick() {
        ClientPreAttackCallback.EVENT.register((client, player, clicks) -> {
            if (clicks > 0 && (client.hitResult == null || client.hitResult.getType() == HitResult.Type.MISS)) {
                FabricServerEvents.fireEmptyLeftClick(player);
            }
            // Never cancels: the handler adds behaviour to the swing, it does not replace it.
            return false;
        });
    }

    /**
     * Fabric's route to {@code /shieldpose}, the model-pose development tool. Forge and NeoForge
     * get the same tree from {@code ClientEvents#onRegisterClientCommands}, a game-bus event Fabric
     * has no equivalent of; {@code ClientCommandRegistrationCallback} is the equivalent seam.
     *
     * <p>The dispatcher's source type is the only thing that differs between the three loaders, and
     * {@code AMShieldPoseCommand} leaves it open precisely so this is one line. Note that the tree
     * is built out of plain brigadier: {@code ClientCommandManager} — the class every Fabric
     * tutorial names — was <b>removed</b> in fabric-command-api-v2 3.x, the releases the 26.x nodes
     * pin, so touching it would have cost a Stonecutter arm for nothing.
     *
     * <p>The {@code available()} guard keeps the command out of a shipped jar: it answers false
     * unless the game directory sits inside a checkout of this repo.
     */
    private static void registerClientCommands() {
        net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, buildContext) -> {
                    if (com.github.alexthe666.alexsmobs.client.command.AMShieldPoseCommand.available()) {
                        com.github.alexthe666.alexsmobs.client.command.AMShieldPoseCommand.register(
                                dispatcher, (source, message) -> source.sendFeedback(message));
                    }
                });
    }

    /**
     * The farseer's full-screen static. {@code ClientEvents#renderStaticOverlay} is shared drawing
     * code every loader reaches by its own route — Forge's {@code RenderGuiOverlayEvent} below
     * 1.20.5, {@code AddGuiOverlayLayersEvent} / {@code RegisterGuiLayersEvent} above it, and
     * {@code mixin/client/GuiMixin} on the one Forge node that has neither. This is Fabric's route,
     * and until it existed the effect simply did not draw on any Fabric node.
     *
     * <p>Three arms, and the boundaries are not where the rest of this port's are: Fabric API kept
     * {@code HudRenderCallback} on a bare {@code float} partial tick until <b>1.21</b> handed it a
     * {@code DeltaTracker} (checked against every pinned {@code fabric-api} jar, so this is the API's
     * own timeline, not Minecraft's), and dropped the callback entirely at <b>26</b> in favour of
     * {@code HudElementRegistry}. {@code HudElementRegistry} exists from 1.21.6, but the deprecated
     * callback keeps working right through 1.21.11 — using it for the whole {@code <26} range buys
     * one arm instead of two for no behavioural difference.
     *
     * <p><b>Divergence from Forge/NeoForge, on purpose.</b> Those two insert the layer directly above
     * the camera overlay, i.e. underneath the hotbar and chat. {@code HudRenderCallback} draws after
     * the entire HUD and has no anchor to choose, so all fifteen {@code <26} Fabric nodes already
     * draw the static on top of everything — and 26 uses {@code addLast} rather than
     * {@code attachElementAfter(VanillaHudElements.MISC_OVERLAYS, …)} to match its own siblings
     * rather than the other loaders. For a full-screen tint the difference is whether the hotbar
     * shows through it, which is the smaller inconsistency of the two.
     */
    private static void registerFarseerStatic() {
        //? if <1.21 {
        net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register(
                (guiGraphics, partialTick) -> ClientEvents.renderStaticOverlay(guiGraphics, partialTick));
        //?} elif <26 {
        /*net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register(
                (guiGraphics, deltaTracker) ->
                        ClientEvents.renderStaticOverlay(guiGraphics, deltaTracker.getGameTimeDeltaPartialTick(false)));
        *///?} else {
        /*// The lambda parameters are deliberately left untyped. Naming the first one would mean
        // writing the 26-era name of GuiGraphics, and the !mc26-guigraphics rule is a REVERSIBLE
        // regex: it rewrites that token straight back on every node, and its whole precondition is
        // that root src/ never contains it. An untyped lambda parameter sidesteps the question.
        net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.addLast(
                com.github.alexthe666.alexsmobs.misc.AMCompat.rl(com.github.alexthe666.alexsmobs.AlexsMobs.MODID, "farseer_static"),
                (guiGraphics, deltaTracker) ->
                        ClientEvents.renderStaticOverlay(guiGraphics, deltaTracker.getGameTimeDeltaPartialTick(false)));
        *///?}
    }

    /** The instance every Fabric-side hook must go through — see the note on shared per-frame state. */
    public static ClientEvents handlers() {
        return HANDLERS;
    }

    // ─── ViewportEvent.ComputeCameraAngles ────────────────────────────────────────────────────
    // Fired from mixin/fabric/client/FabricCameraMixin, at the tail of the camera's own setup.

    /**
     * One event, <b>two</b> handlers — the same doubling-up the shared source already does on Forge
     * {@code >=1.21.3}, where {@code RenderLevelStageEvent} was deleted and {@code doWorldLastFrame}
     * moved onto this hook. {@code onCameraSetup} shakes the camera for the Earthquake effect and
     * {@code onRenderWorldLastEvent} runs the per-frame state sweep; both are ungated-on-Fabric
     * handlers in {@code ClientEvents}, so calling one and not the other would silently drop half a
     * feature.
     *
     * <p>The null guard is this class's job, not the mixin's. Both handlers dereference
     * {@code Minecraft.getInstance().player} without checking — safe on Forge because the event only
     * exists inside a level render, and safe here for the same reason, but a camera hook is exactly
     * the kind of thing a future arm might fire from the title screen. The guard costs one branch a
     * frame and turns a hypothetical NPE into a no-op.
     */
    public static void fireComputeCameraAngles(net.minecraft.client.Camera camera, float partialTick) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        com.github.alexthe666.alexsmobs.fabric.forge.client.event.ViewportEvent.ComputeCameraAngles event =
                new com.github.alexthe666.alexsmobs.fabric.forge.client.event.ViewportEvent.ComputeCameraAngles(camera, partialTick);
        HANDLERS.onCameraSetup(event);
        HANDLERS.onRenderWorldLastEvent(event);
    }

    // ─── RenderHandEvent ──────────────────────────────────────────────────────────────────────
    // Fired from mixin/fabric/client/FabricItemInHandRendererMixin, once per hand.

    /**
     * @return {@code true} if a listener cancelled the event, i.e. the caller must skip vanilla's
     *         arm render for this hand. The cancelling branch is the bald-eagle POV, which hides
     *         both hands.
     *
     * <p>One signature for all 17 nodes even though 1.21.9 replaced the buffer source with a
     * collector: the mixin wraps the collector in an {@code AMSubmitBuffers} before it gets here, and
     * {@code MultiBufferSource} is a name the tree already owns on the nodes where vanilla deleted it
     * (the {@code !mc262-multibuffersource} rule redirects it to the vendored compat type). The
     * handler flushes that recorder itself once it has drawn.
     */
    public static boolean fireRenderHand(net.minecraft.world.InteractionHand hand,
                                         net.minecraft.world.item.ItemStack stack,
                                         float partialTick,
                                         com.mojang.blaze3d.vertex.PoseStack poseStack,
                                         net.minecraft.client.renderer.MultiBufferSource buffers,
                                         int packedLight) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null) {
            return false;
        }
        com.github.alexthe666.alexsmobs.fabric.forge.client.event.RenderHandEvent event =
                new com.github.alexthe666.alexsmobs.fabric.forge.client.event.RenderHandEvent(
                        hand, stack, partialTick, poseStack, buffers, packedLight);
        HANDLERS.onRenderHand(event);
        return event.isCanceled();
    }

    // ─── ViewportEvent.ComputeFogColor ────────────────────────────────────────────────────────
    // Fired from mixin/fabric/client/FabricFogRendererMixin, once vanilla has the frame's colour.

    /**
     * @return the event itself, so the caller can read back the three channels the handler may have
     *         rewritten. Returning the event rather than a {@code float[]} keeps the five call sites
     *         — one per API era, because this method's shape changes more than any other on the
     *         client — from having to agree on an index order.
     *
     * <p><b>Never returns {@code null}</b>, and on a null player it returns the event carrying
     * exactly the colour that went in, so every arm can assign the result back unconditionally
     * instead of branching. {@code ClientEvents#onFogColor} dereferences
     * {@code Minecraft.getInstance().player} with no check of its own — safe on Forge, where the
     * event only fires inside a level render, and safe here for the same reason, but fog setup is
     * close enough to the title screen's own render path to be worth the one branch.
     */
    public static com.github.alexthe666.alexsmobs.fabric.forge.client.event.ViewportEvent.ComputeFogColor
            fireComputeFogColor(net.minecraft.client.Camera camera, float partialTick,
                                float red, float green, float blue) {
        com.github.alexthe666.alexsmobs.fabric.forge.client.event.ViewportEvent.ComputeFogColor event =
                new com.github.alexthe666.alexsmobs.fabric.forge.client.event.ViewportEvent.ComputeFogColor(
                        camera, partialTick, red, green, blue);
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null) {
            return event;
        }
        HANDLERS.onFogColor(event);
        return event;
    }

    // ─── ViewportEvent.RenderFog ──────────────────────────────────────────────────────────────
    // Also from FabricFogRendererMixin, but a different set of arms — see the block comment there.

    /**
     * @param near the near plane vanilla computed, the far plane in {@code far}. Both are read back
     *             off the returned event; the handler may write either, both or neither.
     * @return the event, never {@code null}, carrying the values that went in if no handler ran.
     *
     * <p>The partial tick is sourced here rather than threaded in from the mixin. Two reasons: the
     * arms that reach the planes through {@code @ModifyArgs} have no access to the enclosing call's
     * arguments, and {@code ClientEvents#onFogDensity} never reads {@code getPartialTick()} anyway —
     * it asks {@code Minecraft} for the frame time itself, through the same call this line uses, so
     * the value on the event agrees with the one the handler uses by construction.
     */
    public static com.github.alexthe666.alexsmobs.fabric.forge.client.event.ViewportEvent.RenderFog
            fireRenderFog(net.minecraft.client.Camera camera, float near, float far) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        com.github.alexthe666.alexsmobs.fabric.forge.client.event.ViewportEvent.RenderFog event =
                new com.github.alexthe666.alexsmobs.fabric.forge.client.event.ViewportEvent.RenderFog(
                        camera, mc.getFrameTime(), near, far);
        if (mc.player == null || camera == null) {
            return event;
        }
        HANDLERS.onFogDensity(event);
        return event;
    }

    // ─── RenderLivingEvent ────────────────────────────────────────────────────────────────────
    // Fired from mixin/fabric/client/FabricLivingEntityRendererMixin. The mixin is only a *where*
    // (and a per-era argument unpacker); everything below is the *what*, so neither file has to
    // know the other's arms — the same split as Wave 2's mixin/fabric/**.
    //
    // The signature difference between the two arms is exactly one parameter, the render state,
    // and it exists for the same reason the stub carries it: from 1.21.2 the model reads its
    // rotations off the state rather than off the entity, so flipUpsideDown has to mutate it.
    // Entity, partial tick and packed light are handed in on EVERY node — the mixin digs them out
    // of the state where vanilla stopped passing them — which is what keeps ClientEvents on its
    // 1.20.1-shaped `else` arms across all seventeen nodes.

    /**
     * @return {@code true} if a listener cancelled the event, i.e. the caller must skip the vanilla
     *         render body. The one cancelling branch is the rolling rocky chestplate, which draws
     *         the entity itself and reposts a {@code Post} by hand.
     */
    //? if >=1.21.2 {
    /*@SuppressWarnings({"unchecked", "rawtypes"})
    public static boolean firePreRenderLiving(net.minecraft.world.entity.LivingEntity entity,
                                              net.minecraft.client.renderer.entity.LivingEntityRenderer renderer,
                                              float partialTick,
                                              com.mojang.blaze3d.vertex.PoseStack poseStack,
                                              net.minecraft.client.renderer.MultiBufferSource buffers,
                                              int packedLight,
                                              net.minecraft.client.renderer.entity.state.LivingEntityRenderState state) {
        RenderLivingEvent.Pre event =
                new RenderLivingEvent.Pre(entity, renderer, partialTick, poseStack, buffers, packedLight, state);
        HANDLERS.onPreRenderEntity(event);
        return event.isCanceled();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void firePostRenderLiving(net.minecraft.world.entity.LivingEntity entity,
                                            net.minecraft.client.renderer.entity.LivingEntityRenderer renderer,
                                            float partialTick,
                                            com.mojang.blaze3d.vertex.PoseStack poseStack,
                                            net.minecraft.client.renderer.MultiBufferSource buffers,
                                            int packedLight,
                                            net.minecraft.client.renderer.entity.state.LivingEntityRenderState state) {
        HANDLERS.onPostRenderEntity(
                new RenderLivingEvent.Post(entity, renderer, partialTick, poseStack, buffers, packedLight, state));
    }
    *///?} else {
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static boolean firePreRenderLiving(net.minecraft.world.entity.LivingEntity entity,
                                              net.minecraft.client.renderer.entity.LivingEntityRenderer renderer,
                                              float partialTick,
                                              com.mojang.blaze3d.vertex.PoseStack poseStack,
                                              net.minecraft.client.renderer.MultiBufferSource buffers,
                                              int packedLight) {
        RenderLivingEvent.Pre event =
                new RenderLivingEvent.Pre(entity, renderer, partialTick, poseStack, buffers, packedLight);
        HANDLERS.onPreRenderEntity(event);
        return event.isCanceled();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void firePostRenderLiving(net.minecraft.world.entity.LivingEntity entity,
                                            net.minecraft.client.renderer.entity.LivingEntityRenderer renderer,
                                            float partialTick,
                                            com.mojang.blaze3d.vertex.PoseStack poseStack,
                                            net.minecraft.client.renderer.MultiBufferSource buffers,
                                            int packedLight) {
        HANDLERS.onPostRenderEntity(
                new RenderLivingEvent.Post(entity, renderer, partialTick, poseStack, buffers, packedLight));
    }
    //?}

    // ─── RenderNameTagEvent ───────────────────────────────────────────────────────────────────
    // Fired from mixin/fabric/client/FabricNameTagMixin, at the head of the nameplate method.

    /**
     * @return {@code true} if the handler vetoed the nameplate, i.e. the caller must skip drawing
     *         it. The only branch that ever does is the bald-eagle POV hiding the player's own.
     *
     * <p>Takes the entity rather than the render state, so one signature serves all seventeen
     * nodes: the {@code >=1.21.2} arms of the mixin unwrap the state through {@code AMStateAccess}
     * before calling in. A null entity — an {@code AMStateAccess} miss on a state the render-state
     * mixin never captured — returns {@code false} rather than NPEing a per-frame path.
     */
    public static boolean fireRenderNameTag(net.minecraft.world.entity.Entity entity) {
        if (entity == null) {
            return false;
        }
        com.github.alexthe666.alexsmobs.fabric.forge.client.event.RenderNameTagEvent event =
                new com.github.alexthe666.alexsmobs.fabric.forge.client.event.RenderNameTagEvent(entity);
        HANDLERS.onRenderNameplate(event);
        return event.getResult()
                == com.github.alexthe666.alexsmobs.fabric.forge.eventbus.api.Event.Result.DENY;
    }
}

package com.github.alexthe666.alexsmobs.fabric.client;

import com.github.alexthe666.alexsmobs.client.render.item.AMItemRenderProperties;
import com.github.alexthe666.alexsmobs.item.IClientExtensionItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

/**
 * The Fabric stand-in for {@code IClientItemExtensions#getCustomRenderer} — the Forge hook that hands
 * an item a {@code BlockEntityWithoutLevelRenderer} to draw itself with, which in this mod is always
 * {@code AMItemstackRenderer}.
 *
 * <p>Eleven items ask for it, and every one of them has {@code builtin/entity} as its model: the model
 * is a placeholder that resolves to nothing, and the renderer <i>is</i> the item's whole appearance.
 * On Fabric {@link IClientItemExtensions} is a vendored empty type token and nothing ever calls the
 * consumer (see that class), so all eleven drew <b>nothing at all</b> — an invisible inventory slot
 * that still has a name and a tooltip. Reported as "the shattered dimensional carver and a couple
 * others are missing icons in the block menu", on Fabric 1.21.1.
 *
 * <p><b>This is a different fault from the same symptom on {@code >=1.21.4}</b>, and the two need
 * different fixes. There, vanilla deleted the mechanism outright on every loader and the models are
 * rebuilt at build time by {@code DataPackMigration.repairBuiltinEntityModel}. Here the mechanism is
 * alive and merely unwired, so the real renderer can be restored and every item gets its actual look
 * back — the carver's drifting shards, the shield's 3D model, the four in-hand/inventory swaps — not
 * a static approximation. The gate between them is exact: Fabric API's
 * {@code BuiltinItemRendererRegistry} exists in every {@code fabric-api} this tree pins from 1.20.1
 * through 1.21.3 and is gone from 1.21.4 up, the same boundary at which vanilla dropped the BEWLR
 * (checked against the pinned jar for all 17 Fabric nodes, not assumed).
 *
 * <p>{@code DynamicItemRenderer#render} takes exactly {@code renderByItem}'s six parameters, and its
 * descriptor is byte-identical across that whole range, so the adapter is a method reference in all
 * but name and needs no per-version arm.
 *
 * <p>Registered from {@code ClientProxy#clientInit}'s Fabric arm, next to
 * {@link FabricArmorRenderers#register()} — the other half of what {@code IClientItemExtensions} does.
 */
public final class FabricItemRenderers {

    private FabricItemRenderers() {
    }

    /**
     * Registers the shared renderer for every item that asks Forge for it.
     *
     * <p>The item list is deliberately <b>not</b> spelled out here. Each item's own
     * {@code initializeClient} is what declares which extension it wants, exactly as it does for
     * NeoForge in {@code ClientProxy#onRegisterClientExtensions}, so this walks the registry and
     * offers the same consumer. That keeps the registration the single source of truth and means an
     * item added later is picked up without touching this file — and it is what separates the eleven
     * ISTER items from the two armour ones ({@code ItemModArmor}, {@code ItemTarantulaHawkElytra}),
     * which hand back a {@code CustomArmorRenderProperties} instead and belong to
     * {@link FabricArmorRenderers}.
     */
    public static void register() {
        //? if <1.21.4 {
        final com.github.alexthe666.alexsmobs.client.render.AMItemstackRenderer renderer =
                new com.github.alexthe666.alexsmobs.client.render.AMItemstackRenderer();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof IClientExtensionItem extension) {
                extension.initializeClient(properties -> {
                    if (properties instanceof AMItemRenderProperties) {
                        net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry.INSTANCE.register(
                                item,
                                (stack, context, poseStack, buffers, light, overlay) ->
                                        renderer.renderByItem(stack, context, poseStack, buffers, light, overlay));
                    }
                });
            }
        }
        //?}
    }
}

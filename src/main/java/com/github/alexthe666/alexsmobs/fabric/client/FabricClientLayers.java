package com.github.alexthe666.alexsmobs.fabric.client;

import com.github.alexthe666.alexsmobs.client.render.layer.LayerRainbow;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;

/**
 * Fabric twin of {@code client/ClientLayerRegistry}, which is excluded from the compile on this
 * loader ({@code ModPlatformPlugin.configureJava}).
 *
 * <p><b>Why a twin rather than a gate.</b> {@code ClientLayerRegistry#onAddLayers} is a single
 * method whose body already contains seven Stonecutter blocks — six loader/version arms for the
 * player renderer alone, because Forge and NeoForge disagree about how to enumerate player skins in
 * four different MC eras. Stonecutter blocks are siblings and never nest, so that method cannot be
 * wrapped in a {@code !fabric} block. Excluding the file and writing this is the same remedy
 * {@code event/ServerEvents} and {@code client/event/ClientEvents} already use.
 *
 * <p><b>And it collapses the whole thing to one call.</b> Fabric API's callback fires once per
 * living entity renderer <em>including the player's</em>, so all six of those arms — and the
 * reflective renderer lookup, and the {@code DefaultAttributes::hasSupplier} filter that stood in
 * for "is this a living entity" — reduce to the lambda below. The ender dragon needs no special
 * case either: its renderer is not a {@code LivingEntityRenderer}, so the callback never offers it.
 *
 * <p>{@link LayerRainbow} is raw on purpose (see {@code compat/StateRenderLayer}), which is the
 * whole reason the unchecked suppression is here.
 */
public final class FabricClientLayers {

    private FabricClientLayers() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void register() {
        LivingEntityRenderLayerRegistrationCallback.EVENT.register(
                (entityType, renderer, helper, context) -> helper.register(new LayerRainbow(renderer)));
    }
}

package com.github.alexthe666.alexsmobs.client.render;

import net.minecraft.world.entity.EntityType;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Which entity types actually received {@code LayerRainbow}.
 *
 * <p><b>Why this exists.</b> The rainbow jelly is entity-type-agnostic on the server — any
 * {@code LivingEntity} can be fed one and {@code RainbowUtil} writes and syncs the tag — but the
 * rendering is not. Both registration paths can only attach the layer to a
 * {@code LivingEntityRenderer}: {@code ClientLayerRegistry} casts to one, and Fabric API's
 * {@code LivingEntityRenderLayerRegistrationCallback} only ever offers one. Any mod whose renderer
 * extends {@code EntityRenderer} directly is therefore skipped — which is <em>every GeckoLib mob in
 * the game</em>, because {@code GeoEntityRenderer extends EntityRenderer}. Born in Chaos, Alex's
 * Caves' GeckoLib mobs, Ice and Fire, Mowzie's Mobs: all of them ate the jelly and stayed grey.
 *
 * <p>{@code AMRainbowFallback} draws those from the render dispatcher instead, and needs to know
 * which types it must NOT draw, or a vanilla mob would get the glint twice. This set is the answer:
 * populated once at renderer-registration time, never cleared, read on the render thread.
 *
 * <p>It lives apart from {@code AMRainbowFallback} because that class names {@code MultiBufferSource}
 * and is excluded from the compile on >=1.21.9 (the submit pipeline hands renderers no buffer source
 * to wrap), while {@code ClientLayerRegistry} — which calls {@link #markLayered} — is compiled on
 * every node. Nothing here touches the render API, so it compiles everywhere.
 */
public final class AMRainbowLayers {

    private static final Set<EntityType<?>> LAYERED = ConcurrentHashMap.newKeySet();

    private AMRainbowLayers() {
    }

    public static void markLayered(EntityType<?> type) {
        if (type != null) {
            LAYERED.add(type);
        }
    }

    public static boolean hasLayer(EntityType<?> type) {
        return LAYERED.contains(type);
    }
}

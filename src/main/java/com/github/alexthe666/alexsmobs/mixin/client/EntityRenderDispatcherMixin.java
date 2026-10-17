package com.github.alexthe666.alexsmobs.mixin.client;

import com.github.alexthe666.alexsmobs.client.render.AMRainbowFallback;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws the rainbow-jelly glint for entities that could not be given {@code LayerRainbow} — every
 * GeckoLib mob in the game, Born in Chaos included. See {@code AMRainbowFallback} for the mechanism
 * and {@code AMRainbowLayers} for why the layer cannot reach them.
 *
 * <p>⚠️ {@code EntityRenderer} is never named here, not even fully qualified: {@code TAIL} on the
 * dispatcher's own public entry point means the second pass is a plain recursive call and the
 * renderer is never touched. That also keeps rule 5 (docs/notes/mixins.md) trivially satisfied.
 *
 * <p>Two arms, not four. The public {@code render} lost its body-yaw parameter at 1.21.2 and is
 * otherwise unchanged from 1.20.1 to 1.21.8 — the render-state overloads 1.21.5 added sit beside it
 * and {@code LevelRenderer} still calls this one (verified in the bytecode of 1.21.4 and 1.21.8).
 * At 1.21.9 the method becomes {@code submit(S, CameraRenderState, DDD, PoseStack,
 * SubmitNodeCollector)} with no buffer source anywhere, so this whole file is excluded from the
 * compile and pruned from the mixin config from there up.
 */
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    //? if >=1.21.2 {
    /*@Inject(method = "render(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("TAIL"))
    private void alexsmobs$rainbowFallback(Entity entity, double x, double y, double z, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
        MultiBufferSource glint = AMRainbowFallback.begin(entity, bufferSource);
        if (glint != null) {
            try {
                ((EntityRenderDispatcher) (Object) this).render(entity, x, y, z, partialTick, poseStack, glint, packedLight);
            } finally {
                AMRainbowFallback.end();
            }
        }
    }
    *///?} else {
    @Inject(method = "render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("TAIL"))
    private void alexsmobs$rainbowFallback(Entity entity, double x, double y, double z, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
        MultiBufferSource glint = AMRainbowFallback.begin(entity, bufferSource);
        if (glint != null) {
            try {
                ((EntityRenderDispatcher) (Object) this).render(entity, x, y, z, yaw, partialTick, poseStack, glint, packedLight);
            } finally {
                AMRainbowFallback.end();
            }
        }
    }
    //?}
}

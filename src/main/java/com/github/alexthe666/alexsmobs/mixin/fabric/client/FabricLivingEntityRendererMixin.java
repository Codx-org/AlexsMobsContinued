package com.github.alexthe666.alexsmobs.mixin.fabric.client;

import com.github.alexthe666.alexsmobs.fabric.client.FabricClientEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires {@code RenderLivingEvent.Pre} and {@code .Post} around every living-entity render, which is
 * what Forge patches into this same method. Drives the rolling rocky chestplate, the clinging /
 * debilitating-sting flip, the ender-flu shake, the vine lasso and the wandering-trader model swap.
 *
 * <p>The mixin is only a <em>where</em>: it unpacks whatever the era's signature happens to carry
 * into the six values {@code ClientEvents} reads on its 1.20.1-shaped {@code else} arms, and
 * {@code FabricClientEvents} builds the event. That split is what keeps the shared handler free of
 * Fabric arms — see the stub's own header for the "union of every era's payload" reasoning.
 */
// ⚠️ Rule 5: the target is spelled out FULLY QUALIFIED and this file must NEVER
// `import net.minecraft.client.renderer.entity.LivingEntityRenderer;` — the
// `!mc2102-render-import-living` replacement rewrites exactly that statement to the mod's own
// client.render.compat.LivingEntityRenderer on every >=1.21.2 node, which would silently retarget
// @Mixin at a class whose render() takes an AMRenderState. It compiles clean either way and dies at
// mixin-apply time. Nothing version-specific is imported here for the same family of reasons: an
// import is not inside any arm, so it must resolve on all seventeen nodes or not exist at all.
@Mixin(net.minecraft.client.renderer.entity.LivingEntityRenderer.class)
public abstract class FabricLivingEntityRendererMixin {

    // ── Why the selectors carry a full descriptor, against the tree's usual name-only habit ──
    // Both `render` and `submit` are overloaded HERE, by the synthetic bridge the compiler emits
    // for EntityRenderer's erased signature (render(Entity,…) / submit(EntityRenderState,…)). A
    // name-only selector matches the bridge as well, and since the bridge *calls* the real method,
    // every hook would fire twice per entity per frame — two Pre events, two Post events, a doubled
    // rocky-chestplate model and a doubled vine lasso. The descriptor is what disambiguates.
    // verify_mixin_targets.py resolves all four of these against each node's jar, so a drift shows
    // up as a red gate step rather than as a crash.
    //
    // ── Why Post is TAIL and not RETURN ──
    // A HEAD injector with cancellable=true inserts its own `return` at the top of the method, and
    // an @At("RETURN") injector applied afterwards would find it — so Post would also fire on the
    // cancelled path. TAIL takes the LAST return only. Verified by javap: this method has exactly
    // ONE return instruction on all seventeen nodes, so TAIL is the real end of the body and the
    // two injection points cannot collide. (Forge likewise does not post Post when Pre is
    // cancelled, which is why ClientEvents reposts one by hand.)

    //? if <1.21.2 {
    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), cancellable = true)
    private void alexsmobs$preRenderLiving(net.minecraft.world.entity.LivingEntity entity,
                                           float entityYaw,
                                           float partialTick,
                                           com.mojang.blaze3d.vertex.PoseStack poseStack,
                                           net.minecraft.client.renderer.MultiBufferSource buffers,
                                           int packedLight,
                                           CallbackInfo ci) {
        if (FabricClientEvents.firePreRenderLiving(entity,
                (net.minecraft.client.renderer.entity.LivingEntityRenderer) (Object) this,
                partialTick, poseStack, buffers, packedLight)) {
            ci.cancel();
        }
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("TAIL"))
    private void alexsmobs$postRenderLiving(net.minecraft.world.entity.LivingEntity entity,
                                            float entityYaw,
                                            float partialTick,
                                            com.mojang.blaze3d.vertex.PoseStack poseStack,
                                            net.minecraft.client.renderer.MultiBufferSource buffers,
                                            int packedLight,
                                            CallbackInfo ci) {
        FabricClientEvents.firePostRenderLiving(entity,
                (net.minecraft.client.renderer.entity.LivingEntityRenderer) (Object) this,
                partialTick, poseStack, buffers, packedLight);
    }
    //?}

    // 1.21.2 replaced the entity and the partial tick with the render state extracted from it.
    // Neither is gone, only moved: renderstate/EntityRendererMixin records both onto the state
    // (that is what AMStateAccess is for), so this arm hands ClientEvents the same six values the
    // arm above does. Packed light is still a parameter here — EntityRenderState does not carry a
    // light field until 1.21.9.
    //? if >=1.21.2 && <1.21.9 {
    /*@Inject(method = "render(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), cancellable = true)
    private void alexsmobs$preRenderLiving(net.minecraft.client.renderer.entity.state.LivingEntityRenderState state,
                                           com.mojang.blaze3d.vertex.PoseStack poseStack,
                                           net.minecraft.client.renderer.MultiBufferSource buffers,
                                           int packedLight,
                                           CallbackInfo ci) {
        if (!(com.github.alexthe666.alexsmobs.client.render.compat.AMStateAccess.entity(state)
                instanceof net.minecraft.world.entity.LivingEntity entity)) {
            return;
        }
        if (FabricClientEvents.firePreRenderLiving(entity,
                (net.minecraft.client.renderer.entity.LivingEntityRenderer) (Object) this,
                com.github.alexthe666.alexsmobs.client.render.compat.AMStateAccess.partialTick(state),
                poseStack, buffers, packedLight, state)) {
            ci.cancel();
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("TAIL"))
    private void alexsmobs$postRenderLiving(net.minecraft.client.renderer.entity.state.LivingEntityRenderState state,
                                            com.mojang.blaze3d.vertex.PoseStack poseStack,
                                            net.minecraft.client.renderer.MultiBufferSource buffers,
                                            int packedLight,
                                            CallbackInfo ci) {
        if (com.github.alexthe666.alexsmobs.client.render.compat.AMStateAccess.entity(state)
                instanceof net.minecraft.world.entity.LivingEntity entity) {
            FabricClientEvents.firePostRenderLiving(entity,
                    (net.minecraft.client.renderer.entity.LivingEntityRenderer) (Object) this,
                    com.github.alexthe666.alexsmobs.client.render.compat.AMStateAccess.partialTick(state),
                    poseStack, buffers, packedLight, state);
        }
    }
    *///?}

    // 1.21.9 turned rendering into submission: no immediate MultiBufferSource, a SubmitNodeCollector
    // instead, and the packed light moved onto the state as `lightCoords`. AMSubmitBuffers is the
    // bridge the whole tree already uses — it records what a legacy body draws and replays it
    // through the collector — so handing one to the event is what lets ClientEvents keep drawing the
    // rocky chestplate and the vine lasso the pre-1.21.2 way. It is NOT flushed here: the handler
    // flushes it itself (ClientEvents#flushBuffers) once it has finished drawing, and an event that
    // no handler drew into has nothing recorded to replay.
    // The two sub-arms differ by a single package: CameraRenderState moved into `state.level` at 26,
    // and the rule that rewrites it (!mc26-pkg-camerastate) keys on the DOTTED name, so it cannot
    // reach a slash-form descriptor inside an annotation.
    //? if >=1.21.9 && <26 {
    /*@Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At("HEAD"), cancellable = true)
    private void alexsmobs$preRenderLiving(net.minecraft.client.renderer.entity.state.LivingEntityRenderState state,
                                           com.mojang.blaze3d.vertex.PoseStack poseStack,
                                           net.minecraft.client.renderer.SubmitNodeCollector collector,
                                           net.minecraft.client.renderer.state.CameraRenderState camera,
                                           CallbackInfo ci) {
        if (alexsmobs$submitPre(state, poseStack, collector, camera)) {
            ci.cancel();
        }
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At("TAIL"))
    private void alexsmobs$postRenderLiving(net.minecraft.client.renderer.entity.state.LivingEntityRenderState state,
                                            com.mojang.blaze3d.vertex.PoseStack poseStack,
                                            net.minecraft.client.renderer.SubmitNodeCollector collector,
                                            net.minecraft.client.renderer.state.CameraRenderState camera,
                                            CallbackInfo ci) {
        alexsmobs$submitPost(state, poseStack, collector, camera);
    }
    *///?}

    //? if >=26 {
    /*@Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("HEAD"), cancellable = true)
    private void alexsmobs$preRenderLiving(net.minecraft.client.renderer.entity.state.LivingEntityRenderState state,
                                           com.mojang.blaze3d.vertex.PoseStack poseStack,
                                           net.minecraft.client.renderer.SubmitNodeCollector collector,
                                           net.minecraft.client.renderer.state.CameraRenderState camera,
                                           CallbackInfo ci) {
        if (alexsmobs$submitPre(state, poseStack, collector, camera)) {
            ci.cancel();
        }
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("TAIL"))
    private void alexsmobs$postRenderLiving(net.minecraft.client.renderer.entity.state.LivingEntityRenderState state,
                                            com.mojang.blaze3d.vertex.PoseStack poseStack,
                                            net.minecraft.client.renderer.SubmitNodeCollector collector,
                                            net.minecraft.client.renderer.state.CameraRenderState camera,
                                            CallbackInfo ci) {
        alexsmobs$submitPost(state, poseStack, collector, camera);
    }
    *///?}

    // The two >=1.21.9 arms above are identical apart from that one package, so the bodies live
    // here once rather than four times. `camera` is passed through so AMSubmitBuffers gets the
    // frame's real CameraRenderState instead of rebuilding an equivalent one.
    //? if >=1.21.9 {
    /*@org.spongepowered.asm.mixin.Unique
    private boolean alexsmobs$submitPre(net.minecraft.client.renderer.entity.state.LivingEntityRenderState state,
                                        com.mojang.blaze3d.vertex.PoseStack poseStack,
                                        net.minecraft.client.renderer.SubmitNodeCollector collector,
                                        net.minecraft.client.renderer.state.CameraRenderState camera) {
        if (!(com.github.alexthe666.alexsmobs.client.render.compat.AMStateAccess.entity(state)
                instanceof net.minecraft.world.entity.LivingEntity entity)) {
            return false;
        }
        return FabricClientEvents.firePreRenderLiving(entity,
                (net.minecraft.client.renderer.entity.LivingEntityRenderer) (Object) this,
                com.github.alexthe666.alexsmobs.client.render.compat.AMStateAccess.partialTick(state),
                poseStack,
                new com.github.alexthe666.alexsmobs.client.render.compat.AMSubmitBuffers(collector, camera),
                state.lightCoords, state);
    }

    @org.spongepowered.asm.mixin.Unique
    private void alexsmobs$submitPost(net.minecraft.client.renderer.entity.state.LivingEntityRenderState state,
                                      com.mojang.blaze3d.vertex.PoseStack poseStack,
                                      net.minecraft.client.renderer.SubmitNodeCollector collector,
                                      net.minecraft.client.renderer.state.CameraRenderState camera) {
        if (com.github.alexthe666.alexsmobs.client.render.compat.AMStateAccess.entity(state)
                instanceof net.minecraft.world.entity.LivingEntity entity) {
            FabricClientEvents.firePostRenderLiving(entity,
                    (net.minecraft.client.renderer.entity.LivingEntityRenderer) (Object) this,
                    com.github.alexthe666.alexsmobs.client.render.compat.AMStateAccess.partialTick(state),
                    poseStack,
                    new com.github.alexthe666.alexsmobs.client.render.compat.AMSubmitBuffers(collector, camera),
                    state.lightCoords, state);
        }
    }
    *///?}
}

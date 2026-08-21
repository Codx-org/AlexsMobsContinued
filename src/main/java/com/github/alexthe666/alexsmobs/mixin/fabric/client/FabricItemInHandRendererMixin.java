package com.github.alexthe666.alexsmobs.mixin.fabric.client;

import com.github.alexthe666.alexsmobs.fabric.client.FabricClientEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires {@code RenderHandEvent} once per hand, immediately before vanilla draws that hand — the same
 * point Forge fires it, and cancellable with the same meaning ("the hand was not drawn"). Drives
 * three things in {@code ClientEvents#onRenderHand}: suppressing both hands while riding a bald
 * eagle's camera, drawing a perched falconry bird over the glove, and nudging the dimensional carver
 * during its swing.
 *
 * <p><b>Why the target is {@code renderArmWithItem} and not {@code renderHandsWithItems}.</b> The
 * event is per-hand: the handler reads {@code getHand()} and {@code getItemStack()}, and cancelling
 * is meant to drop one hand's render, not the whole pass. {@code renderHandsWithItems} is the pass;
 * {@code renderArmWithItem} is the per-hand callee it invokes once per hand, and it already carries
 * every field the stub needs as a parameter — hand, stack, partial tick, pose stack, buffers and
 * packed light — so nothing has to be dug back out. Forge's own patch sits in exactly the same
 * place: {@code ForgeHooksClient.renderSpecificFirstPersonHand} replaces each
 * {@code renderArmWithItem} call and skips it when the event is cancelled.
 */
@Mixin(net.minecraft.client.renderer.ItemInHandRenderer.class)
public abstract class FabricItemInHandRendererMixin {

    // ── Three arms, two boundaries, and one of them is a pure rename ──
    // 1.21.9 turned rendering into submission, so the buffer parameter became a SubmitNodeCollector;
    // 26.2 then renamed the method render->submit without touching the signature. A name-only check
    // would have called the second one "unchanged" and a signature-only check would have called it
    // "missing"; the descriptors below were read off each era's jar with javap.
    //
    // HEAD + cancellable, because cancelling has to prevent vanilla's body, and vanilla's body IS
    // this method. Full descriptors as always: they cost nothing and let verify_mixin_targets.py
    // prove each arm against the node's real bytecode.

    //? if <1.21.9 {
    @Inject(method = "renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), cancellable = true)
    private void alexsmobs$renderHand(net.minecraft.client.player.AbstractClientPlayer player,
                                      float partialTick,
                                      float pitch,
                                      net.minecraft.world.InteractionHand hand,
                                      float swingProgress,
                                      net.minecraft.world.item.ItemStack stack,
                                      float equippedProgress,
                                      com.mojang.blaze3d.vertex.PoseStack poseStack,
                                      net.minecraft.client.renderer.MultiBufferSource buffers,
                                      int packedLight,
                                      CallbackInfo ci) {
        if (FabricClientEvents.fireRenderHand(hand, stack, partialTick, poseStack, buffers, packedLight)) {
            ci.cancel();
        }
    }
    //?}

    //? if >=1.21.9 && <26.2 {
    /*@Inject(method = "renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
            at = @At("HEAD"), cancellable = true)
    private void alexsmobs$renderHand(net.minecraft.client.player.AbstractClientPlayer player,
                                      float partialTick,
                                      float pitch,
                                      net.minecraft.world.InteractionHand hand,
                                      float swingProgress,
                                      net.minecraft.world.item.ItemStack stack,
                                      float equippedProgress,
                                      com.mojang.blaze3d.vertex.PoseStack poseStack,
                                      net.minecraft.client.renderer.SubmitNodeCollector collector,
                                      int packedLight,
                                      CallbackInfo ci) {
        if (alexsmobs$submitHand(hand, stack, partialTick, poseStack, collector, packedLight)) {
            ci.cancel();
        }
    }
    *///?}

    //? if >=26.2 {
    /*@Inject(method = "submitArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
            at = @At("HEAD"), cancellable = true)
    private void alexsmobs$renderHand(net.minecraft.client.player.AbstractClientPlayer player,
                                      float partialTick,
                                      float pitch,
                                      net.minecraft.world.InteractionHand hand,
                                      float swingProgress,
                                      net.minecraft.world.item.ItemStack stack,
                                      float equippedProgress,
                                      com.mojang.blaze3d.vertex.PoseStack poseStack,
                                      net.minecraft.client.renderer.SubmitNodeCollector collector,
                                      int packedLight,
                                      CallbackInfo ci) {
        if (alexsmobs$submitHand(hand, stack, partialTick, poseStack, collector, packedLight)) {
            ci.cancel();
        }
    }
    *///?}

    // The two >=1.21.9 arms differ only in the method name, so the body lives here once.
    // AMSubmitBuffers is the tree's standard bridge: it records what a legacy draw call emits and
    // replays it through the collector. Note it is deliberately NOT flushed here — ClientEvents'
    // own handler flushes it (`flushBuffers`) after it has finished drawing the falconry bird, and
    // an event no handler drew into has nothing to replay. The no-camera constructor is the one
    // Forge's `handBuffers` uses for this same event: a RenderHandEvent carries no CameraRenderState
    // on any loader, so AMSubmitBuffers rebuilds an equivalent one if it is ever asked.
    //? if >=1.21.9 {
    /*@org.spongepowered.asm.mixin.Unique
    private boolean alexsmobs$submitHand(net.minecraft.world.InteractionHand hand,
                                         net.minecraft.world.item.ItemStack stack,
                                         float partialTick,
                                         com.mojang.blaze3d.vertex.PoseStack poseStack,
                                         net.minecraft.client.renderer.SubmitNodeCollector collector,
                                         int packedLight) {
        return FabricClientEvents.fireRenderHand(hand, stack, partialTick, poseStack,
                new com.github.alexthe666.alexsmobs.client.render.compat.AMSubmitBuffers(collector),
                packedLight);
    }
    *///?}
}

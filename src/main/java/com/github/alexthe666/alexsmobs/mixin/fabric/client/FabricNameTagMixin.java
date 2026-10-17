package com.github.alexthe666.alexsmobs.mixin.fabric.client;

import com.github.alexthe666.alexsmobs.fabric.client.FabricClientEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires {@code RenderNameTagEvent}, the one hook {@code ClientEvents} uses to hide the player's own
 * nameplate while their camera entity is a bald eagle in singleplayer.
 *
 * <p><b>Why this injects into the callee and not the call site.</b> Forge patches the caller —
 * {@code EntityRenderer.render}'s {@code if (shouldShowName(e)) renderNameTag(…)} — but a
 * {@code render}-level hook here would miss {@code RenderTiger}, {@code RenderFarseer} and
 * {@code RenderUnderminer}, which override {@code render} outright without calling {@code super} and
 * invoke {@code this.renderNameTag(...)} themselves. Those three are exactly the renderers that
 * Wave 3b-1 could not reach (see the divergence table in {@code docs/notes/fabric.md}). Targeting
 * the nameplate method itself covers them and every vanilla renderer with one injection per node.
 *
 * <p><b>What this deliberately cannot do: force-ALLOW.</b> From inside the callee a nameplate can be
 * suppressed but not conjured, because vanilla only calls the method when it has already decided to
 * draw one. The mod never sets {@code ALLOW} — it only ever DENYs — so the gap is theoretical, but
 * it is why this is not described as full parity with the Forge event.
 */
// ⚠️ Rule 5: `EntityRenderer` is a compat-shadowed name. This file must NEVER
// `import net.minecraft.client.renderer.entity.EntityRenderer;` — a replacement rule rewrites that
// exact statement to the mod's own client.render.compat.EntityRenderer on the nodes where it
// applies, which would silently retarget @Mixin at the wrong class. It compiles clean either way
// and dies at mixin-apply time. Nothing version-specific is imported here for the same family of
// reasons: an import sits outside every arm, so it must resolve on all seventeen nodes or not
// exist at all — hence the fully-qualified spellings throughout.
@Mixin(net.minecraft.client.renderer.entity.EntityRenderer.class)
public abstract class FabricNameTagMixin {

    // ── Why every selector carries a full descriptor ──
    // The method's name is stable across two of the four boundaries below while its shape is not,
    // which is precisely the failure rule 10 exists for: 1.20.5 appends a partial-tick float and
    // changes NOTHING else, so a name-only selector keeps matching and the injection silently
    // targets the wrong era's method. verify_mixin_targets.py resolves each of these against that
    // node's own jar, so drift surfaces as a red gate step instead of a crash.
    //
    // ── The four boundaries ──
    //   1.20.5  appends a trailing F (partial tick). Name unchanged — a name grep passes.
    //   1.21.2  swaps Entity for the EntityRenderState it was extracted from, changing both the
    //           descriptor and how the entity is reached; AMStateAccess is the bridge, and it is
    //           only compiled >=1.21.2, so it is spelled fully qualified inside the arms.
    //   1.21.9  renames to submitNameTag and swaps buffer+light for collector+camera.
    //   26      renames again to submitNameDisplay and moves CameraRenderState into state/level/.

    //? if <1.20.5 {
    @Inject(method = "renderNameTag(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), cancellable = true)
    private void alexsmobs$renderNameTag(net.minecraft.world.entity.Entity entity,
                                         net.minecraft.network.chat.Component name,
                                         com.mojang.blaze3d.vertex.PoseStack poseStack,
                                         net.minecraft.client.renderer.MultiBufferSource buffers,
                                         int packedLight,
                                         CallbackInfo ci) {
        if (FabricClientEvents.fireRenderNameTag(entity)) {
            ci.cancel();
        }
    }
    //?} elif >=1.20.5 && <1.21.2 {
    /*@Inject(method = "renderNameTag(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IF)V",
            at = @At("HEAD"), cancellable = true)
    private void alexsmobs$renderNameTag(net.minecraft.world.entity.Entity entity,
                                         net.minecraft.network.chat.Component name,
                                         com.mojang.blaze3d.vertex.PoseStack poseStack,
                                         net.minecraft.client.renderer.MultiBufferSource buffers,
                                         int packedLight,
                                         float partialTick,
                                         CallbackInfo ci) {
        if (FabricClientEvents.fireRenderNameTag(entity)) {
            ci.cancel();
        }
    }
    *///?} elif >=1.21.2 && <1.21.9 {
    /*@Inject(method = "renderNameTag(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), cancellable = true)
    private void alexsmobs$renderNameTag(net.minecraft.client.renderer.entity.state.EntityRenderState state,
                                         net.minecraft.network.chat.Component name,
                                         com.mojang.blaze3d.vertex.PoseStack poseStack,
                                         net.minecraft.client.renderer.MultiBufferSource buffers,
                                         int packedLight,
                                         CallbackInfo ci) {
        if (FabricClientEvents.fireRenderNameTag(
                com.github.alexthe666.alexsmobs.client.render.compat.AMStateAccess.entity(state))) {
            ci.cancel();
        }
    }
    *///?} elif >=1.21.9 && <26 {
    /*@Inject(method = "submitNameTag(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At("HEAD"), cancellable = true)
    private void alexsmobs$renderNameTag(net.minecraft.client.renderer.entity.state.EntityRenderState state,
                                         com.mojang.blaze3d.vertex.PoseStack poseStack,
                                         net.minecraft.client.renderer.SubmitNodeCollector collector,
                                         net.minecraft.client.renderer.state.CameraRenderState camera,
                                         CallbackInfo ci) {
        if (FabricClientEvents.fireRenderNameTag(
                com.github.alexthe666.alexsmobs.client.render.compat.AMStateAccess.entity(state))) {
            ci.cancel();
        }
    }
    *///?} else {
    /*// 26 ships submitNameDisplay TWICE: a 4-arg protected entry point that submit() invokes and
    // that a subclass overrides, and a 5-arg protected final one it delegates to that does the
    // drawing. A name-only selector matches both and the hook would fire twice per nameplate; the
    // descriptor pins the 4-arg entry point. Resolved from bytecode, not guessed — see the
    // porting log.
    // ⚠️ These comment lines sit INSIDE this arm's block-comment wrapper on purpose. A bare `//`
    // line placed between `else {` and the wrapper gets its marker stripped when Stonecutter
    // uncomments the arm, and lands in the projection as raw text — which is exactly how this file
    // failed to compile on 26.2-fabric the first time.
    @Inject(method = "submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("HEAD"), cancellable = true)
    private void alexsmobs$renderNameTag(net.minecraft.client.renderer.entity.state.EntityRenderState state,
                                         com.mojang.blaze3d.vertex.PoseStack poseStack,
                                         net.minecraft.client.renderer.SubmitNodeCollector collector,
                                         net.minecraft.client.renderer.state.level.CameraRenderState camera,
                                         CallbackInfo ci) {
        if (FabricClientEvents.fireRenderNameTag(
                com.github.alexthe666.alexsmobs.client.render.compat.AMStateAccess.entity(state))) {
            ci.cancel();
        }
    }
    *///?}
}

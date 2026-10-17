package com.github.alexthe666.alexsmobs.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 26.x only — the whole file is excluded from the compile and pruned from the mixin config below 26
 * (see ModPlatformPlugin.configureJava), because the class it borrows from does not carry the value
 * there.
 *
 * <p>26.1 dropped the {@code ItemDisplayContext} parameter from {@code SpecialModelRenderer#submit}.
 * Every other era hands it straight to {@link com.github.alexthe666.alexsmobs.client.render.AMIconSpecialRenderer},
 * which passes it into the legacy renderer; on 26.x it was hardcoded to {@code GUI}, which is right
 * for the pure-icon items and wrong for the shattered dimensional carver, whose eleven shards each
 * resolve their own {@code item/handheld} display transform from it — so in the hand they rendered in
 * the flat GUI pose instead of pointing away from the player (#96).
 *
 * <p>The context did not go away, it moved one frame up: {@code ItemStackRenderState} still holds it,
 * and its {@code submit} is the sole caller of the private per-layer {@code submit} that invokes the
 * special renderer (javap-verified on 26.1.2 and 26.2 — identical there). Push it for the duration of
 * that call so the renderer can read it. A deque rather than a field because an item's renderer may
 * itself draw another item (the dictionary and the advancement icons both do).
 */
@Mixin(net.minecraft.client.renderer.item.ItemStackRenderState.class)
public abstract class ItemStackRenderStateMixin {

    @Shadow
    net.minecraft.world.item.ItemDisplayContext displayContext;

    @Inject(method = "submit", at = @At("HEAD"))
    private void alexsmobs$pushDisplayContext(com.mojang.blaze3d.vertex.PoseStack poseStack,
                                              net.minecraft.client.renderer.SubmitNodeCollector collector,
                                              int packedLight, int packedOverlay, int outlineColor,
                                              CallbackInfo ci) {
        com.github.alexthe666.alexsmobs.client.render.AMIconSpecialRenderer.pushDisplayContext(this.displayContext);
    }

    @Inject(method = "submit", at = @At("RETURN"))
    private void alexsmobs$popDisplayContext(com.mojang.blaze3d.vertex.PoseStack poseStack,
                                             net.minecraft.client.renderer.SubmitNodeCollector collector,
                                             int packedLight, int packedOverlay, int outlineColor,
                                             CallbackInfo ci) {
        com.github.alexthe666.alexsmobs.client.render.AMIconSpecialRenderer.popDisplayContext();
    }
}

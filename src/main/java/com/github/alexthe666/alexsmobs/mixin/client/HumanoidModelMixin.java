package com.github.alexthe666.alexsmobs.mixin.client;

import com.github.alexthe666.alexsmobs.citadel.client.event.EventPosePlayerHand;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;

/**
 * Fires {@link EventPosePlayerHand} — Alex's Mobs overrides the arm pose while a vine lasso
 * is held. Vendored from Citadel (LGPL-3.0-only).
 */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin extends Model {

    public HumanoidModelMixin(Function<ResourceLocation, RenderType> renderTypeFunction) {
        //? if >=1.21.2 {
        /*super(null, renderTypeFunction);
        *///?} else {
        super(renderTypeFunction);
        //?}
    }

    // 1.21.2 rewrote Humanoid#poseRightArm/poseLeftArm to take the render state (and an ArmPose)
    // instead of the live LivingEntity, and player render states carry no back-reference to the
    // entity — which EventPosePlayerHand needs. So the vine-lasso arm-pose override is inactive on
    // >=1.21.2 (the arm renders in its vanilla pose); the injects compile only below it.
    //? if <1.21.2 {
    @Inject(at = @At("HEAD"), method = "Lnet/minecraft/client/model/HumanoidModel;poseRightArm(Lnet/minecraft/world/entity/LivingEntity;)V", cancellable = true)
    private void alexsmobs_poseRightArm(LivingEntity entity, CallbackInfo ci) {
        EventPosePlayerHand event = new EventPosePlayerHand(entity, (HumanoidModel) (Model) this, false);
        event.post();
        if (event.isHandled()) {
            ci.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "Lnet/minecraft/client/model/HumanoidModel;poseLeftArm(Lnet/minecraft/world/entity/LivingEntity;)V", cancellable = true)
    private void alexsmobs_poseLeftArm(LivingEntity entity, CallbackInfo ci) {
        EventPosePlayerHand event = new EventPosePlayerHand(entity, (HumanoidModel) (Model) this, true);
        event.post();
        if (event.isHandled()) {
            ci.cancel();
        }
    }
    //?}
}

package com.github.alexthe666.alexsmobs.client.render;

import com.github.alexthe666.alexsmobs.client.model.ModelAnaconda;
import com.github.alexthe666.alexsmobs.entity.EntityAnacondaPart;
import com.github.alexthe666.alexsmobs.entity.util.AnacondaPartIndex;
import com.github.alexthe666.alexsmobs.citadel.client.model.AdvancedEntityModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;

public class RenderAnacondaPart extends LivingEntityRenderer<EntityAnacondaPart, AdvancedEntityModel<EntityAnacondaPart>> {
    private final ModelAnaconda<EntityAnacondaPart> neckModel = new ModelAnaconda<>(AnacondaPartIndex.NECK);
    private final ModelAnaconda<EntityAnacondaPart> bodyModel = new ModelAnaconda<>(AnacondaPartIndex.BODY);
    private final ModelAnaconda<EntityAnacondaPart> tailModel = new ModelAnaconda<>(AnacondaPartIndex.TAIL);

    public RenderAnacondaPart(EntityRendererProvider.Context renderManagerIn) {
        super(renderManagerIn, new ModelAnaconda<>(AnacondaPartIndex.NECK), 0.3F);
    }

    //? if >=1.20.5 {
    /*protected void setupRotations(EntityAnacondaPart entity, PoseStack stack, float pitchIn, float yawIn, float partialTickTime, float scale) {
    *///?} else {
    protected void setupRotations(EntityAnacondaPart entity, PoseStack stack, float pitchIn, float yawIn, float partialTickTime) {
    //?}
        float newYaw = entity.yHeadRot;
        if (this.isShaking(entity)) {
            newYaw += (float)(Math.cos((double)entity.tickCount * 3.25D) * Math.PI * (double)0.4F);
        }

        Pose pose = entity.getPose();
        if (pose != Pose.SLEEPING) {
         //   stack.mulPose(Axis.YP.rotationDegrees(180.0F - yawIn));
            stack.mulPose(Axis.YP.rotationDegrees(180.0F - newYaw));
            stack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
        }

        if (entity.deathTime > 0) {
            float f = ((float)entity.deathTime + partialTickTime - 1.0F) / 20.0F * 1.6F;
            f = Mth.sqrt(f);
            if (f > 1.0F) {
                f = 1.0F;
            }

            stack.mulPose(Axis.ZP.rotationDegrees(f * this.getFlipDegrees(entity)));
         } else if (entity.hasCustomName()) {
            String s = ChatFormatting.stripFormatting(entity.getName().getString());
            if (("Dinnerbone".equals(s) || "Grumm".equals(s))) {
                stack.translate(0.0D, (double)(entity.getBbHeight() + 0.1F), 0.0D);
                stack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            }
        }

    }

    // A body segment must never carry a name plate. An anaconda is one head plus up to eight part
    // entities, so anything that lets a name through draws the same label eight times stacked
    // along the snake — reported as "every single part will be named 'Anaconda'". The head entity
    // owns the name; the parts are a rendering/collision implementation detail.
    //
    // Upstream only inherited vanilla's guard here, which still lets a name through. Both
    // signatures are overridden because 1.21.2 moved the hook to (entity, squaredDistance): below
    // that the one-arg form is vanilla's own, above it it is only the compat bridge (see
    // client/render/compat/LivingEntityRenderer), so overriding one alone leaves the other live.
    //? if >=1.21.2 {
    /*@Override
    protected boolean shouldShowName(EntityAnacondaPart entity, double squaredDistance) {
        return false;
    }
    *///?}

    protected boolean shouldShowName(EntityAnacondaPart entity) {
        return false;
    }

    protected void scale(EntityAnacondaPart entitylivingbaseIn, PoseStack matrixStackIn, float partialTickTime) {
        this.model = getModelForType(entitylivingbaseIn.getPartType());
        matrixStackIn.scale(entitylivingbaseIn.getScale(), entitylivingbaseIn.getScale(), entitylivingbaseIn.getScale());
    }

    private AdvancedEntityModel<EntityAnacondaPart> getModelForType(AnacondaPartIndex partType) {
        switch (partType){
            case BODY: return bodyModel;
            case NECK: return neckModel;
            case TAIL: return tailModel;
        }
        return bodyModel;
    }


    public ResourceLocation getTextureLocation(EntityAnacondaPart entity) {
        return RenderAnaconda.getAnacondaTexture(entity.isYellow(), entity.isShedding());
    }
}

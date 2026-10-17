package com.github.alexthe666.alexsmobs.client.render;

import com.github.alexthe666.alexsmobs.misc.AMCompat;

import com.github.alexthe666.alexsmobs.client.model.ModelFly;
import com.github.alexthe666.alexsmobs.entity.EntityFly;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class RenderFly extends MobRenderer<EntityFly, ModelFly> {
    private static final ResourceLocation TEXTURE = AMCompat.rl("alexsmobs:textures/entity/fly.png");

    public RenderFly(EntityRendererProvider.Context renderManagerIn) {
        super(renderManagerIn, new ModelFly(), 0.2F);
    }

    protected void scale(EntityFly entitylivingbaseIn, PoseStack matrixStackIn, float partialTickTime) {
    }

    protected boolean isShaking(EntityFly fly) {
        return fly.isInNether();
    }

    //? if >=1.20.5 {
    /*protected void setupRotations(EntityFly entityLiving, PoseStack matrixStackIn, float ageInTicks, float rotationYaw, float partialTicks, float scale) {
    *///?} else {
    protected void setupRotations(EntityFly entityLiving, PoseStack matrixStackIn, float ageInTicks, float rotationYaw, float partialTicks) {
    //?}
        if (this.isShaking(entityLiving)) {
            rotationYaw += (float)(Math.cos((double)entityLiving.tickCount * 7F) * Math.PI * (double)0.9F);
            float vibrate = 0.05F;
            matrixStackIn.translate((entityLiving.getRandom().nextFloat() - 0.5F)* vibrate, (entityLiving.getRandom().nextFloat() - 0.5F) * vibrate, (entityLiving.getRandom().nextFloat() - 0.5F)* vibrate);
        }
        //? if >=1.20.5 {
        /*super.setupRotations(entityLiving, matrixStackIn, ageInTicks, rotationYaw, partialTicks, scale);
        *///?} else {
        super.setupRotations(entityLiving, matrixStackIn, ageInTicks, rotationYaw, partialTicks);
        //?}
    }

    public ResourceLocation getTextureLocation(EntityFly entity) {
        return TEXTURE;
    }
}

package com.github.alexthe666.alexsmobs.client.render;

import com.github.alexthe666.alexsmobs.misc.AMCompat;

import com.github.alexthe666.alexsmobs.client.model.ModelBoneSerpentBody;
import com.github.alexthe666.alexsmobs.client.model.ModelBoneSerpentTail;
import com.github.alexthe666.alexsmobs.entity.EntityBoneSerpentPart;
import com.github.alexthe666.alexsmobs.citadel.client.model.AdvancedEntityModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;

public class RenderBoneSerpentPart extends LivingEntityRenderer<EntityBoneSerpentPart, AdvancedEntityModel<EntityBoneSerpentPart>> {
    private static final ResourceLocation TEXTURE_BODY = AMCompat.rl("alexsmobs:textures/entity/bone_serpent_mid.png");
    private static final ResourceLocation TEXTURE_TAIL = AMCompat.rl("alexsmobs:textures/entity/bone_serpent_tail.png");
    private final ModelBoneSerpentBody bodyModel = new ModelBoneSerpentBody();
    private final ModelBoneSerpentTail tailModel = new ModelBoneSerpentTail();

    public RenderBoneSerpentPart(EntityRendererProvider.Context renderManagerIn) {
        super(renderManagerIn, new ModelBoneSerpentBody(), 0.3F);
    }

    // Same reasoning as RenderAnacondaPart#shouldShowName — a body segment never carries a name
    // plate, and both signatures need overriding because of the 1.21.2 hook change.
    //? if >=1.21.2 {
    /*@Override
    protected boolean shouldShowName(EntityBoneSerpentPart entity, double squaredDistance) {
        return false;
    }
    *///?}

    protected boolean shouldShowName(EntityBoneSerpentPart entity) {
        return false;
    }

    protected void scale(EntityBoneSerpentPart entitylivingbaseIn, PoseStack matrixStackIn, float partialTickTime) {
        this.model = entitylivingbaseIn.isTail() ? tailModel : bodyModel;
      //  matrixStackIn.scale(1.2F, 1.2F, 1.2F);
    }


    public ResourceLocation getTextureLocation(EntityBoneSerpentPart entity) {
        return entity.isTail() ? TEXTURE_TAIL : TEXTURE_BODY;
    }
}

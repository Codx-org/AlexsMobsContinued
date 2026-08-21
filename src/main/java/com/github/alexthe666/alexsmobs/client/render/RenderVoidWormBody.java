package com.github.alexthe666.alexsmobs.client.render;

import com.github.alexthe666.alexsmobs.misc.AMCompat;

import com.github.alexthe666.alexsmobs.client.model.ModelVoidWormBody;
import com.github.alexthe666.alexsmobs.client.model.ModelVoidWormTail;
import com.github.alexthe666.alexsmobs.client.render.layer.LayerVoidWormGlow;
import com.github.alexthe666.alexsmobs.entity.EntityVoidWormPart;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;

public class RenderVoidWormBody extends LivingEntityRenderer<EntityVoidWormPart, EntityModel<EntityVoidWormPart>> {
    private static final ResourceLocation TEXTURE_BODY = AMCompat.rl("alexsmobs:textures/entity/void_worm/void_worm_body.png");
    private static final ResourceLocation TEXTURE_BODY_HURT = AMCompat.rl("alexsmobs:textures/entity/void_worm/void_worm_body_hurt.png");
    private static final ResourceLocation TEXTURE_BODY_GLOW = AMCompat.rl("alexsmobs:textures/entity/void_worm/void_worm_body_glow.png");
    private static final ResourceLocation TEXTURE_TAIL = AMCompat.rl("alexsmobs:textures/entity/void_worm/void_worm_tail.png");
    private static final ResourceLocation TEXTURE_TAIL_HURT = AMCompat.rl("alexsmobs:textures/entity/void_worm/void_worm_tail_hurt.png");
    private static final ResourceLocation TEXTURE_TAIL_GLOW = AMCompat.rl("alexsmobs:textures/entity/void_worm/void_worm_tail_glow.png");
    private final ModelVoidWormBody bodyModel = new ModelVoidWormBody(0.0F);
    private final ModelVoidWormTail tailModel = new ModelVoidWormTail(0.0F);

    public RenderVoidWormBody(EntityRendererProvider.Context renderManagerIn) {
        super(renderManagerIn, new ModelVoidWormBody(0.0F), 1F);
        this.addLayer(new LayerVoidWormGlow(this, renderManagerIn.getResourceManager(), new ModelVoidWormBody(0.0F)){
            public ResourceLocation getGlowTexture(LivingEntity worm){
                return ((EntityVoidWormPart)worm).isTail() ? TEXTURE_TAIL_GLOW : TEXTURE_BODY_GLOW;
            }
            public boolean isGlowing(LivingEntity worm){
                return !((EntityVoidWormPart)worm).isHurt();
            }
            public float getAlpha(LivingEntity livingEntity){
                EntityVoidWormPart worm = (EntityVoidWormPart) livingEntity;
                return (float) Mth.clamp((worm.getHealth() - worm.getHealthThreshold()) / (worm.getMaxHealth() - worm.getHealthThreshold()), 0, 1F);
            }
        });
    }

    public boolean shouldRender(EntityVoidWormPart worm, Frustum camera, double camX, double camY, double camZ) {
        return worm.getPortalTicks() <= 0 && super.shouldRender(worm, camera, camX, camY, camZ);
    }

    public ResourceLocation getTextureLocation(EntityVoidWormPart entity) {
        if (entity.isHurt()) {
            return entity.isTail() ? TEXTURE_TAIL_HURT : TEXTURE_BODY_HURT;
        } else {
            return entity.isTail() ? TEXTURE_TAIL : TEXTURE_BODY;
        }
    }

    //? if >=1.20.5 {
    /*protected void setupRotations(EntityVoidWormPart entityLiving, PoseStack matrixStackIn, float ageInTicks, float rotationYaw, float partialTicks, float scale) {
    *///?} else {
    protected void setupRotations(EntityVoidWormPart entityLiving, PoseStack matrixStackIn, float ageInTicks, float rotationYaw, float partialTicks) {
    //?}
        Pose pose = entityLiving.getPose();
        if (pose != Pose.SLEEPING) {
            matrixStackIn.mulPose(Axis.YP.rotationDegrees(180.0F - entityLiving.getWormYaw(partialTicks)));
        }
        if (entityLiving.deathTime > 0) {
            float f = ((float) entityLiving.deathTime + partialTicks - 1.0F) / 20.0F * 1.6F;
            f = Mth.sqrt(f);
            if (f > 1.0F) {
                f = 1.0F;
            }
            matrixStackIn.mulPose(Axis.ZP.rotationDegrees(f * this.getFlipDegrees(entityLiving)));
        }

    }

    protected void scale(EntityVoidWormPart entitylivingbaseIn, PoseStack matrixStackIn, float partialTickTime) {
        this.model = entitylivingbaseIn.isTail() ? tailModel : bodyModel;
        matrixStackIn.scale(entitylivingbaseIn.getWormScale(), entitylivingbaseIn.getWormScale(), entitylivingbaseIn.getWormScale());
    }

    // Same reasoning as RenderAnacondaPart#shouldShowName — a body segment never carries a name
    // plate, and both signatures need overriding because of the 1.21.2 hook change. Upstream's
    // body here was a verbatim copy of vanilla's guard, which still lets a name through, so a
    // named void worm drew the same label once per segment down the whole worm.
    //? if >=1.21.2 {
    /*@Override
    protected boolean shouldShowName(EntityVoidWormPart entity, double squaredDistance) {
        return false;
    }
    *///?}

    protected boolean shouldShowName(EntityVoidWormPart entity) {
        return false;
    }
}

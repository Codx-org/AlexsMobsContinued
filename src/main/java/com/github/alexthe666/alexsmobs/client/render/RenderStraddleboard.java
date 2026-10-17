package com.github.alexthe666.alexsmobs.client.render;

import com.github.alexthe666.alexsmobs.misc.AMCompat;

import com.github.alexthe666.alexsmobs.client.model.ModelStraddleboard;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.EntityStraddleboard;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

import static net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;

public class RenderStraddleboard extends EntityRenderer<EntityStraddleboard> {
    private static final ResourceLocation TEXTURE_OVERLAY = AMCompat.rl("alexsmobs:textures/entity/straddleboard_overlay.png");
    private static final ResourceLocation TEXTURE = AMCompat.rl("alexsmobs:textures/entity/straddleboard.png");
    private static final ModelStraddleboard BOARD_MODEL = new ModelStraddleboard();

    public RenderStraddleboard(EntityRendererProvider.Context renderManager) {
        super(renderManager);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityStraddleboard entity) {
        return TEXTURE;
    }

    @Override
    public void render(EntityStraddleboard entityIn, float entityYaw, float partialTicks, PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn) {
        matrixStackIn.pushPose();
        matrixStackIn.mulPose(new Quaternionf().rotateY(180F * Mth.DEG_TO_RAD));
        matrixStackIn.mulPose(Axis.YN.rotationDegrees(Mth.lerp(partialTicks, entityIn.yRotO, entityIn.getYRot()) + 180));
        matrixStackIn.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTicks, entityIn.xRotO, entityIn.getXRot())));
        matrixStackIn.pushPose();
        boolean lava =  entityIn.isVehicle();
        float f2 = entityIn.getRockingAngle(partialTicks);
        if (!Mth.equal(f2, 0.0F)) {
            matrixStackIn.mulPose(Axis.ZP.rotationDegrees(entityIn.getRockingAngle(partialTicks)));
        }
        // The overlay layer is the grey panel a player can dye; an undyed one falls back to the
        // configurable straddleboardPanelColor instead of the hardcoded 0xADC3D7, and the wooden
        // base underneath takes straddleboardBaseColor. The gap between the two is the contrast
        // setting, for packs that paint their background the same grey the panel used to be.
        // Deliberately read here and not in EntityStraddleboard#getColor: that value is synched, so
        // re-colouring it would make two clients disagree about what colour a board *is*.
        int k = entityIn.isDefaultColor() ? AMConfig.straddleboardPanelColor : entityIn.getColor();
        float r = (float)(k >> 16 & 255) / 255.0F;
        float g = (float)(k >> 8 & 255) / 255.0F;
        float b = (float)(k & 255) / 255.0F;
        int base = AMConfig.straddleboardBaseColor;
        float baseR = (float)(base >> 16 & 255) / 255.0F;
        float baseG = (float)(base >> 8 & 255) / 255.0F;
        float baseB = (float)(base & 255) / 255.0F;
        float boardRot = entityIn.prevBoardRot + partialTicks * (entityIn.getBoardRot() - entityIn.prevBoardRot);
        matrixStackIn.mulPose(Axis.ZP.rotationDegrees(boardRot));
        matrixStackIn.mulPose(Axis.XP.rotationDegrees(180));
        matrixStackIn.translate(0, -1.5F - Math.abs(boardRot * 0.007F) - (lava ? 0 : 0.25F), 0);
        BOARD_MODEL.animateBoard(entityIn, entityIn.tickCount + partialTicks);
        VertexConsumer ivertexbuilder2 = bufferIn.getBuffer(RenderType.entityCutoutNoCull(TEXTURE_OVERLAY));
        BOARD_MODEL.renderToBuffer(matrixStackIn, ivertexbuilder2, packedLightIn, NO_OVERLAY, r, g, b, 1.0F);
        VertexConsumer ivertexbuilder = bufferIn.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        BOARD_MODEL.renderToBuffer(matrixStackIn, ivertexbuilder, packedLightIn, NO_OVERLAY, baseR, baseG, baseB, 1.0F);
        matrixStackIn.popPose();
        matrixStackIn.popPose();


    }

}

package com.github.alexthe666.alexsmobs.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public class LayerBasicGlow<T extends LivingEntity> extends RenderLayer<T, EntityModel<T>> {
    private final ResourceLocation texture;
    private final RenderType renderType;

    public LayerBasicGlow(
            //? if >=1.21.2 {
            /*RenderLayerParent<com.github.alexthe666.alexsmobs.client.render.compat.AMRenderState, EntityModel<T>> renderer,
            *///?} else {
            RenderLayerParent<T, EntityModel<T>> renderer,
            //?}
            ResourceLocation texture) {
        super(renderer);
        this.texture = texture;
        this.renderType = RenderType.eyes(texture);
    }

    public boolean shouldCombineTextures() {
        return true;
    }

    @Override
    public void render(PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn, T entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        VertexConsumer ivertexbuilder = bufferIn.getBuffer(renderType);
        com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.renderToBuffer(this.getParentModel(), matrixStackIn, ivertexbuilder, packedLightIn, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

    }

}

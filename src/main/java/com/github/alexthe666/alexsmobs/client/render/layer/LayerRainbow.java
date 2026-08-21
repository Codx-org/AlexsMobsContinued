package com.github.alexthe666.alexsmobs.client.render.layer;

import com.github.alexthe666.alexsmobs.client.render.AMRenderTypes;
import com.github.alexthe666.alexsmobs.entity.util.RainbowUtil;
import com.github.alexthe666.alexsmobs.item.ItemRainbowJelly;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

// This layer is attached to EVERY living entity renderer in the game (see ClientLayerRegistry), so
// from 1.21.2 it must be typed on the VANILLA render state — the compat RenderLayer next to it is
// bound to AMRenderState and would ClassCastException in its generated bridge the first time a
// vanilla mob rendered. StateRenderLayer is that vanilla-state base; it also absorbs the 1.21.9
// submit/render split, which could not be gated here because the class declaration below already
// is and Stonecutter blocks never nest.
//? if >=1.21.2 {
/*public class LayerRainbow extends com.github.alexthe666.alexsmobs.client.render.compat.StateRenderLayer {
*///?} else {
public class LayerRainbow extends RenderLayer {
//?}

    private final RenderLayerParent parent;

    public LayerRainbow(RenderLayerParent parent) {
        super(parent);
        this.parent = parent;
    }

    //? if >=1.21.2 {
    /*@Override
    protected void draw(PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn, net.minecraft.client.renderer.entity.state.EntityRenderState state, float netHeadYaw, float headPitch) {
        Entity entity = com.github.alexthe666.alexsmobs.client.render.compat.AMStateAccess.entity(state);
        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        int i = RainbowUtil.getRainbowType(living);
        if (i <= 0) {
            return;
        }
        ItemRainbowJelly.RainbowType rainbowType = ItemRainbowJelly.RainbowType.values()[Mth.clamp(i - 1, 0, ItemRainbowJelly.RainbowType.values().length - 1)];
        VertexConsumer ivertexbuilder = bufferIn.getBuffer(getRenderType(rainbowType));
        // Vanilla's overlay helper wants the LIVING state; a non-living renderer can never get here
        // (the layer is only registered for types with living attributes) but the guard is free.
        int overlay = state instanceof net.minecraft.client.renderer.entity.state.LivingEntityRenderState livingState
                ? net.minecraft.client.renderer.entity.LivingEntityRenderer.getOverlayCoords(livingState, 0.0F)
                : net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
        float alpha = 0.5F;
        matrixStackIn.pushPose();
        com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.renderToBuffer(this.getParentModel(), matrixStackIn, ivertexbuilder, packedLightIn, overlay, 1.0F, 1.0F, 1.0F, alpha);
        matrixStackIn.popPose();
    }
    *///?} else {
    @Override
    public void render(PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn, Entity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        int i = RainbowUtil.getRainbowType((LivingEntity)entity);
        if(entity instanceof LivingEntity && i > 0) {
            ItemRainbowJelly.RainbowType rainbowType = ItemRainbowJelly.RainbowType.values()[Mth.clamp(i - 1, 0,ItemRainbowJelly.RainbowType.values().length - 1)];
            VertexConsumer ivertexbuilder = bufferIn.getBuffer(getRenderType(rainbowType));
            float alpha = 0.5F;
            matrixStackIn.pushPose();
            com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.renderToBuffer(this.getParentModel(), matrixStackIn, ivertexbuilder, packedLightIn, LivingEntityRenderer.getOverlayCoords((LivingEntity)entity, 0), 1.0F, 1.0F, 1.0F, alpha);
            matrixStackIn.popPose();
        }
    }
    //?}

    private RenderType getRenderType(ItemRainbowJelly.RainbowType rainbowType) {
        return switch (rainbowType) {
            case TRANS -> AMRenderTypes.TRANS_GLINT;
            case NONBI -> AMRenderTypes.NONBI_GLINT;
            case BI -> AMRenderTypes.BI_GLINT;
            case ACE -> AMRenderTypes.ACE_GLINT;
            case WEEZER -> AMRenderTypes.WEEZER_GLINT;
            case BRAZIL -> AMRenderTypes.BRAZIL_GLINT;
            default -> AMRenderTypes.RAINBOW_GLINT;
        };
    }
}

package com.github.alexthe666.alexsmobs.client.render;

import com.github.alexthe666.alexsmobs.misc.AMCompat;

import com.github.alexthe666.alexsmobs.client.model.ModelEnderiophage;
import com.github.alexthe666.alexsmobs.entity.EntityEnderiophage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

public class RenderEnderiophage extends MobRenderer<EntityEnderiophage, ModelEnderiophage> {
    private static final ResourceLocation TEXTURE = AMCompat.rl("alexsmobs:textures/entity/enderiophage.png");
    private static final ResourceLocation TEXTURE_GLOW = AMCompat.rl("alexsmobs:textures/entity/enderiophage_glow.png");
    private static final ResourceLocation TEXTURE_OVERWORLD = AMCompat.rl("alexsmobs:textures/entity/enderiophage_overworld.png");
    private static final ResourceLocation TEXTURE_OVERWORLD_GLOW = AMCompat.rl("alexsmobs:textures/entity/enderiophage_overworld_glow.png");
    private static final ResourceLocation TEXTURE_NETHER = AMCompat.rl("alexsmobs:textures/entity/enderiophage_nether.png");
    private static final ResourceLocation TEXTURE_NETHER_GLOW = AMCompat.rl("alexsmobs:textures/entity/enderiophage_nether_glow.png");

    public RenderEnderiophage(EntityRendererProvider.Context renderManagerIn) {
        super(renderManagerIn, new ModelEnderiophage(), 0.5F);
        this.addLayer(new EnderiophageEyesLayer(this));
    }

    @Nullable
    @Override
    protected RenderType getRenderType(EntityEnderiophage p_230496_1_, boolean p_230496_2_, boolean p_230496_3_, boolean p_230496_4_) {
        ResourceLocation resourcelocation = this.getTextureLocation(p_230496_1_);
        if (p_230496_3_) {
            return RenderType.itemEntityTranslucentCull(resourcelocation);
        } else if (p_230496_2_) {
            return RenderType.entityTranslucent(resourcelocation);
        } else {
            return p_230496_4_ ? RenderType.outline(resourcelocation) : null;
        }
    }

    protected void scale(EntityEnderiophage entitylivingbaseIn, PoseStack matrixStackIn, float partialTickTime) {
        float scale = entitylivingbaseIn.prevEnderiophageScale + (entitylivingbaseIn.getPhageScale() - entitylivingbaseIn.prevEnderiophageScale) * partialTickTime;
        matrixStackIn.scale(0.8F * scale, 0.8F * scale, 0.8F * scale);
    }


    public ResourceLocation getTextureLocation(EntityEnderiophage entity) {
        return entity.getVariant() == 2 ? TEXTURE_NETHER : entity.getVariant() == 1 ? TEXTURE_OVERWORLD : TEXTURE;
    }

    /**
     * ⚠️ Deliberately a {@link RenderLayer} and not vanilla's {@code EyesLayer}, which is what
     * upstream wrote and what this file carried until the fifteenth pass. {@code EyesLayer} is
     * <em>not</em> one of the compat-shadowed simple names, so on 1.21.2+ it stayed the vanilla
     * class while its ten-argument {@code render} — the pre-1.21.2 signature — quietly became an
     * unrelated overload that nothing calls. Vanilla's own {@code EyesLayer#render} ran instead and
     * drew through {@code Model#renderToBuffer}, final since 1.21.2 and walking the empty root the
     * compat {@code EntityModel} hands vanilla, so <b>the glow drew nothing at all on every node
     * ≥1.21.2</b>. The same shape hit {@code RenderGuster} and {@code RenderSpectre}.
     */
    static class EnderiophageEyesLayer extends RenderLayer<EntityEnderiophage, ModelEnderiophage> {

        public EnderiophageEyesLayer(RenderEnderiophage p_i50928_1_) {
            super(p_i50928_1_);
        }

        public void render(PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn, EntityEnderiophage entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            VertexConsumer ivertexbuilder = bufferIn.getBuffer(this.getRenderType(entitylivingbaseIn));
            this.getParentModel().renderToBuffer(matrixStackIn, ivertexbuilder, 15728640, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        }

        public RenderType getRenderType(EntityEnderiophage entity) {
            return AMRenderTypes.getGhost(entity.getVariant() == 2 ? TEXTURE_NETHER_GLOW : entity.getVariant() == 1 ? TEXTURE_OVERWORLD_GLOW : TEXTURE_GLOW);
        }
    }

}

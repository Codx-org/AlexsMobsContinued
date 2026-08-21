package com.github.alexthe666.alexsmobs.client.render;

import com.github.alexthe666.alexsmobs.misc.AMCompat;

import com.github.alexthe666.alexsmobs.client.model.ModelSeal;
import com.github.alexthe666.alexsmobs.client.render.layer.LayerSealItem;
import com.github.alexthe666.alexsmobs.entity.EntitySeal;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class RenderSeal extends MobRenderer<EntitySeal, ModelSeal> {
    private static final ResourceLocation TEXTURE_BROWN_0 = AMCompat.rl("alexsmobs:textures/entity/seal/seal_brown_0.png");
    private static final ResourceLocation TEXTURE_BROWN_1 = AMCompat.rl("alexsmobs:textures/entity/seal/seal_brown_1.png");
    private static final ResourceLocation TEXTURE_ARCTIC_0 = AMCompat.rl("alexsmobs:textures/entity/seal/seal_arctic_0.png");
    private static final ResourceLocation TEXTURE_ARCTIC_1 = AMCompat.rl("alexsmobs:textures/entity/seal/seal_arctic_1.png");
    private static final ResourceLocation TEXTURE_ARCTIC_BABY = AMCompat.rl("alexsmobs:textures/entity/seal/seal_arctic_baby.png");
    private static final ResourceLocation TEXTURE_TEARS = AMCompat.rl("alexsmobs:textures/entity/seal/seal_crying.png");
    private static final ResourceLocation TEXTURE_TONGUE = AMCompat.rl("alexsmobs:textures/entity/seal/seal_tongue.png");

    public RenderSeal(EntityRendererProvider.Context renderManagerIn) {
        super(renderManagerIn, new ModelSeal(), 0.45F);
        this.addLayer(new LayerSealItem(this));
        this.addLayer(new SealTearsLayer(this));
    }

    protected boolean shouldShowName(EntitySeal seal) {
        return super.shouldShowName(seal) || seal.isTearsEasterEgg();
    }

    public ResourceLocation getTextureLocation(EntitySeal entity) {
        if(entity.isArctic()){
            return entity.isBaby() ? TEXTURE_ARCTIC_BABY : entity.getVariant() == 1 ? TEXTURE_ARCTIC_1 : TEXTURE_ARCTIC_0;
        }
        return entity.getVariant() == 1 ? TEXTURE_BROWN_1 : TEXTURE_BROWN_0;
    }

    @Override
    //? if >=1.20.5 && <1.21.2 {
    /*protected void renderNameTag(EntitySeal seal, Component text, PoseStack poseStack, MultiBufferSource bufferSrc, int numberIn, float partialTick) {
    *///?} else {
    protected void renderNameTag(EntitySeal seal, Component text, PoseStack poseStack, MultiBufferSource bufferSrc, int numberIn) {
    //?}
        if(seal.isTearsEasterEgg()){
            double d0 = this.entityRenderDispatcher.distanceToSqr(seal);
            // MC 26.2 gave vanilla its own NAME_TAG_DISTANCE attribute, and NeoForge deleted both
            // its NAMETAG_DISTANCE mod attribute and the ClientHooks helper that read it. The
            // vanilla check is the same comparison against the same value — see
            // LivingEntityRenderer#extractNameTags.
            // On Fabric below 26.2 there is neither the vanilla attribute nor a Forge hook to read,
            // so this is vanilla's own pre-26.2 constant — LivingEntityRenderer#shouldShowName
            // compares distanceToSqr against 4096 (64 blocks), which is also the default value
            // Forge's NAMETAG_DISTANCE attribute carries.
            //? if >=26.2 {
            /*double amNameTagDist = seal.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.NAME_TAG_DISTANCE);
            if (d0 <= amNameTagDist * amNameTagDist) {
            *///?} elif fabric {
            /*if (d0 < 4096.0D) {
            *///?} else {
            if (net.minecraftforge.client.ForgeHooksClient.isNameplateInRenderDistance(seal, d0)) {
            //?}
                boolean flag = !seal.isDiscrete();
                float f = seal.getBbHeight() + 0.5F;
                String[] split = text.getString(512).split(" ");
                StringBuilder recombined = new StringBuilder();
                List<String> strings = new ArrayList<>();
                for(int wordIndex = 0; wordIndex < split.length; wordIndex++){
                    recombined.append(split[wordIndex]).append(" ");
                    if(recombined.length() > 15 || wordIndex == split.length - 1){
                        strings.add(recombined.toString());
                        recombined = new StringBuilder();
                    }
                }
                int i = 10 - 10 * strings.size();

                poseStack.pushPose();
                poseStack.translate(0.0D, (double)f, 0.0D);
                poseStack.mulPose(com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.cameraOrientation(this.entityRenderDispatcher));
                poseStack.scale(-0.025F, -0.025F, 0.025F);
                float f1 = 1F;//Minecraft.getInstance().options.getBackgroundOpacity(1.25F);
                int j = 0XFFFFFFFF;
                Font font = this.getFont();
                String widest = "";
                for(String print : strings) {
                    if(font.width(widest) < font.width(print)){
                        widest = print;
                    }
                }
                float widestCenter = (float)(-font.width(widest) / 2);
                for(String print : strings){
                    float f2 = (float)(-font.width(print) / 2);
                    poseStack.translate(0.0D, 0.0D, 0.1D);
                    AMRenderCompat.drawTextInBatch(font, widest, widestCenter, (float)i, j, false, poseStack, bufferSrc, j, 240);
                    poseStack.translate(0.0D, 0.0D, -0.1D);
                    // 0xFF000001 / 0xFF000000, not upstream's bare 1 / 0: 1.21.2 deleted Font's
                    // "promote an alpha-less colour to opaque" guard, so both of these — the seal's
                    // whole text — drew transparent from there up. See GuiBasicBook#getTextColor.
                    AMRenderCompat.drawTextInBatch(font, print, f2, (float)i, 0XFF000001, false, poseStack, bufferSrc, j, 240);
                    AMRenderCompat.drawTextInBatch(font, print, f2, (float)i, 0XFF000000, false, poseStack, bufferSrc, j, 240);
                    i += 10;
                }

                poseStack.popPose();
            }
        }else{
            //? if >=1.20.5 && <1.21.2 {
            /*super.renderNameTag(seal, text, poseStack, bufferSrc, numberIn, partialTick);
            *///?} else {
            super.renderNameTag(seal, text, poseStack, bufferSrc, numberIn);
            //?}
        }
    }

    static class SealTearsLayer extends RenderLayer<EntitySeal, ModelSeal> {

        public SealTearsLayer(RenderSeal p_i50928_1_) {
            super(p_i50928_1_);
        }

        public void render(PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn, EntitySeal entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            if(entitylivingbaseIn.isTearsEasterEgg()){
                VertexConsumer lead = bufferIn.getBuffer(AMRenderTypes.entityCutoutNoCull(TEXTURE_TEARS));
                this.getParentModel().renderToBuffer(matrixStackIn, lead, packedLightIn, LivingEntityRenderer.getOverlayCoords(entitylivingbaseIn, 0), 1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
    }
}

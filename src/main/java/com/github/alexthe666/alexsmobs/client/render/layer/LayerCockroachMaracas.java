package com.github.alexthe666.alexsmobs.client.render.layer;

import com.github.alexthe666.alexsmobs.misc.AMCompat;

import com.github.alexthe666.alexsmobs.client.model.ModelCockroach;
import com.github.alexthe666.alexsmobs.client.model.layered.AMModelLayers;
import com.github.alexthe666.alexsmobs.client.model.layered.ModelSombrero;
import com.github.alexthe666.alexsmobs.client.render.RenderCockroach;
import com.github.alexthe666.alexsmobs.entity.EntityCockroach;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class LayerCockroachMaracas extends RenderLayer<EntityCockroach, ModelCockroach> {

    private ItemStack maracaStack;
    private final ModelSombrero sombrero;
    private static final ResourceLocation SOMBRERO_TEX = AMCompat.rl("alexsmobs:textures/armor/sombrero.png");

    public LayerCockroachMaracas(RenderCockroach render, EntityRendererProvider.Context renderManagerIn) {
        super(render);
        this.sombrero = new ModelSombrero(renderManagerIn.bakeLayer(AMModelLayers.SOMBRERO));
        // ModelSombrero is a vanilla HumanoidModel, and EntityModel#young defaults to TRUE (read in
        // the bytecode: EntityModel.<init> does iconst_1/putfield). Nothing ever clears it on THIS
        // instance. Both armour paths get it from the wearer - Forge's
        // IClientItemExtensions#getGenericArmorModel calls ForgeHooksClient.copyModelProperties onto
        // the replacement model, and FabricArmorRenderers does the same by hand - but this layer
        // bakes its own model and copies nothing. HumanoidModel does not override renderToBuffer, so
        // AgeableListModel's does the drawing, and its young branch scales the head parts by
        // 1.5F/babyHeadScale = 0.75 and then translates babyYHeadOffset/16 = 1.0 unit down the
        // SCALED axis: 0.75 blocks, 12 pixels, straight into the roach's body, with the hat itself
        // three quarters size. Upstream Alex's Mobs has the same fault. It is invisible from 1.21.2
        // up, where the baby transform moved to mesh-bake time and the field no longer exists, which
        // is why the #55 pose work looked right on 26.2. See #110.
        // This layer already applies its own baby scaling at the top of render(), so the vanilla
        // young transform is pure loss here whatever the roach's age.
        //? if <1.21.2
        this.sombrero.young = false;
    }

    // Built on first render, NOT in the constructor. From MC 26.1 `new ItemStack(item)` reads the
    // item holder's data components, and entity renderers are constructed during the client's first
    // resource reload — before components are bound. Doing it eagerly there throws
    // "NullPointerException: Components not bound yet", which vanilla reports as
    // "Failed to create model for alexsmobs:cockroach" and is a hard client crash on Forge 26.
    private ItemStack maracas() {
        if (maracaStack == null) {
            maracaStack = new ItemStack(AMItemRegistry.MARACA.get());
        }
        return maracaStack;
    }

    public void render(PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn, EntityCockroach entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if(entitylivingbaseIn.hasMaracas()){
            ItemStack stack = maracas();
            ItemInHandRenderer renderer = Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer();
            matrixStackIn.pushPose();
            if (entitylivingbaseIn.isBaby()) {
                matrixStackIn.scale(0.65F, 0.65F, 0.65F);
                matrixStackIn.translate(0.0D, 0.815D, 0.125D);
            }
            matrixStackIn.pushPose();
            translateToHand(0, matrixStackIn);
            matrixStackIn.translate(-0.25F, 0.0F, 0);
            matrixStackIn.scale(1.4F, 1.4F, 1.4F);
            matrixStackIn.mulPose(Axis.XP.rotationDegrees(-90F));
            matrixStackIn.mulPose(Axis.ZP.rotationDegrees(60F));
            com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.renderItemInHand(renderer, entitylivingbaseIn, stack, ItemDisplayContext.GROUND, false, matrixStackIn, bufferIn, packedLightIn);
            matrixStackIn.popPose();
            matrixStackIn.pushPose();
            translateToHand(1, matrixStackIn);
            matrixStackIn.translate(0.25F, 0.0F, 0);
            matrixStackIn.scale(1.4F, 1.4F, 1.4F);
            matrixStackIn.mulPose(Axis.XP.rotationDegrees(90F));
            matrixStackIn.mulPose(Axis.ZP.rotationDegrees(-120F));
            com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.renderItemInHand(renderer, entitylivingbaseIn, stack, ItemDisplayContext.GROUND, false, matrixStackIn, bufferIn, packedLightIn);
            matrixStackIn.popPose();
            matrixStackIn.pushPose();
            translateToHand(2, matrixStackIn);
            matrixStackIn.translate(-0.35F, 0.0F, 0);
            matrixStackIn.scale(1.4F, 1.4F, 1.4F);
            matrixStackIn.mulPose(Axis.XP.rotationDegrees(-90F));
            matrixStackIn.mulPose(Axis.ZP.rotationDegrees(60F));
            com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.renderItemInHand(renderer, entitylivingbaseIn, stack, ItemDisplayContext.GROUND, false, matrixStackIn, bufferIn, packedLightIn);
            matrixStackIn.popPose();
            matrixStackIn.pushPose();
            translateToHand(3, matrixStackIn);
            matrixStackIn.translate(0.35F, 0.0F, 0);
            matrixStackIn.scale(1.4F, 1.4F, 1.4F);
            matrixStackIn.mulPose(Axis.XP.rotationDegrees(90F));
            matrixStackIn.mulPose(Axis.ZP.rotationDegrees(-120F));
            com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.renderItemInHand(renderer, entitylivingbaseIn, stack, ItemDisplayContext.GROUND, false, matrixStackIn, bufferIn, packedLightIn);
            matrixStackIn.popPose();
            if(!entitylivingbaseIn.isHeadless()){
                matrixStackIn.pushPose();
                translateToHand(4, matrixStackIn);
                // DIVERGENCE from upstream (#55) — upstream's sombrero floats about half a block
                // above the roach's head. Its offsets are applied in the HEAD's frame, and the dance
                // rears the abdomen -70 degrees about X, so upstream's "settle the hat as it stands
                // up" term (+0.045/tick) points backwards instead of down and the fixed -0.4 lift is
                // never spent: brim bottom h 32.2 px against a head that tops out at h 22.6.
                // Righting the hat FIRST makes both offsets mean what they say; the numbers are then
                // re-solved so the brim rests on the head across the whole ramp (checked at
                // danceProgress 0 / 2.5 / 5, gap under a pixel at each). The rotation is upstream's,
                // unchanged - it is only the order that moved.
                matrixStackIn.mulPose(Axis.XP.rotationDegrees(60F * entitylivingbaseIn.danceProgress * 0.2F));
                matrixStackIn.translate(0F, 0.15F - entitylivingbaseIn.danceProgress * 0.008F, 0.02F);
                matrixStackIn.scale(0.8F, 0.8F, 0.8F);
                VertexConsumer ivertexbuilder = bufferIn.getBuffer(RenderType.entityCutoutNoCull(SOMBRERO_TEX));
                com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.renderToBuffer(sombrero, matrixStackIn, ivertexbuilder, packedLightIn, LivingEntityRenderer.getOverlayCoords(entitylivingbaseIn, 0.0F), 1.0F, 1.0F, 1.0F, 1.0F);
                matrixStackIn.popPose();
            }
            matrixStackIn.popPose();
        }
    }

    protected void translateToHand(int hand, PoseStack matrixStack) {
        this.getParentModel().root.translateAndRotate(matrixStack);
        this.getParentModel().abdomen.translateAndRotate(matrixStack);
        if (hand == 0) {
            this.getParentModel().right_leg_front.translateAndRotate(matrixStack);
        } else if (hand == 1) {
            this.getParentModel().left_leg_front.translateAndRotate(matrixStack);
        } else if (hand == 2) {
            this.getParentModel().right_leg_mid.translateAndRotate(matrixStack);
        } else if (hand == 3) {
            this.getParentModel().left_leg_mid.translateAndRotate(matrixStack);
        }else{
            this.getParentModel().neck.translateAndRotate(matrixStack);
            this.getParentModel().head.translateAndRotate(matrixStack);
        }
    }
}

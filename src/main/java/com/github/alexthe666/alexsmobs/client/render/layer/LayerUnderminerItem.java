package com.github.alexthe666.alexsmobs.client.render.layer;

import com.github.alexthe666.alexsmobs.client.model.ModelUnderminerDwarf;
import com.github.alexthe666.alexsmobs.client.model.ModelUnderminerHumanoid;
import com.github.alexthe666.alexsmobs.client.render.RenderUnderminer;
import com.github.alexthe666.alexsmobs.entity.EntityUnderminer;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class LayerUnderminerItem extends RenderLayer<EntityUnderminer, EntityModel<EntityUnderminer>> {

    public LayerUnderminerItem(RenderUnderminer render) {
        super(render);
    }

    public void render(PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn, EntityUnderminer entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if(!entitylivingbaseIn.isFullyHidden()){
            ItemStack itemstack = entitylivingbaseIn.getItemBySlot(EquipmentSlot.MAINHAND);
            if(RenderUnderminer.renderWithPickaxe){
                itemstack = new ItemStack(AMItemRegistry.GHOSTLY_PICKAXE.get());
            }
            matrixStackIn.pushPose();
            matrixStackIn.pushPose();
            float f = entitylivingbaseIn.getMainArm() == HumanoidArm.LEFT ? 0.1F : -0.1F;
            float f1 = entitylivingbaseIn.isDwarf() ? 0.5F : 0.45F;
            if(entitylivingbaseIn.isDwarf()){
                matrixStackIn.translate(0F,  1F, 0F);
                f *= 0.3F;
            }else{
                matrixStackIn.translate(0F,  0.2F, 0);
            }
            translateToHand(entitylivingbaseIn.getMainArm(), matrixStackIn);
            matrixStackIn.translate(f,  f1,  -0.15F);

            matrixStackIn.mulPose(Axis.XP.rotationDegrees(-90));
            matrixStackIn.mulPose(Axis.YP.rotationDegrees(180));
            ItemInHandRenderer renderer = Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer();
            // The other half of upstream's ghost look (#69): GhostlyPickaxeBakedModel rewrote every
            // vertex's lightmap to 0x00F000F0, so the tool glowed in the pitch-dark tunnels this mob
            // lives in. That wrapper only exists on <1.21.4 && !fabric; passing full light here is the
            // same thing at the one place the mob's pickaxe is drawn, on every node. Ungated on
            // purpose — where the wrapper does run it is already fullbright, so this changes nothing.
            int light = itemstack.is(AMItemRegistry.GHOSTLY_PICKAXE.get()) ? 0x00F000F0 : packedLightIn;
            com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.renderItemInHand(renderer, entitylivingbaseIn, itemstack, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, false, matrixStackIn, bufferIn, light);
            matrixStackIn.popPose();
            matrixStackIn.popPose();
        }
    }

    protected void translateToHand(HumanoidArm arm, PoseStack matrixStack) {
        if(getParentModel() instanceof ModelUnderminerDwarf){
            ((ModelUnderminerDwarf)getParentModel()).translateToHand(arm, matrixStack);
        }
        // The tall form. Below 1.21.2 this is a plain HumanoidModel and the ArmedModel branch would
        // catch it too; from 1.21.2 it is a wrapper that is not an ArmedModel, so it needs naming.
        else if(getParentModel() instanceof ModelUnderminerHumanoid){
            ((ModelUnderminerHumanoid)getParentModel()).translateToHand(arm, matrixStack);
        }
        // 1.21.9 made ArmedModel generic over the render state and gave translateToHand that state
        // as a leading parameter. Unreachable either way now — the two branches above cover both of
        // this layer's possible parent models.
        //? if <1.21.9 {
        else if(getParentModel() instanceof ArmedModel){
            ((ArmedModel)getParentModel()).translateToHand(arm, matrixStack);
        }
        //?}
    }
}

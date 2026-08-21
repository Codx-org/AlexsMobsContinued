package com.github.alexthe666.alexsmobs.client.render.layer;

import com.github.alexthe666.alexsmobs.ClientProxy;
import com.github.alexthe666.alexsmobs.client.model.ModelAnteater;
import com.github.alexthe666.alexsmobs.client.render.RenderAnteater;
import com.github.alexthe666.alexsmobs.entity.EntityAnteater;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.Entity;

public class LayerAnteaterBaby extends RenderLayer<EntityAnteater, ModelAnteater> {

    public LayerAnteaterBaby(RenderAnteater render) {
        super(render);
    }

    public void render(PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn, EntityAnteater roo, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if(roo.isVehicle() && !roo.isBaby()){
            for(Entity passenger : roo.getPassengers()){
                float riderRot = passenger.yRotO + (passenger.getYRot() - passenger.yRotO) * partialTicks;
                net.minecraft.client.model.EntityModel<?> modelBase = com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.rendererModel(passenger);
                if(modelBase != null){
                    ClientProxy.currentUnrenderedEntities.remove(passenger.getUUID());
                    matrixStackIn.pushPose();
                    translateToPouch(matrixStackIn);
                    matrixStackIn.translate(0, -0.12F, 0.1F);
                    matrixStackIn.mulPose(Axis.ZP.rotationDegrees(180F));
                    matrixStackIn.mulPose(Axis.YP.rotationDegrees(riderRot + 180F));
                    renderEntity(passenger, 0, 0, 0, 0, partialTicks, matrixStackIn, bufferIn, packedLightIn);
                    matrixStackIn.popPose();
                    ClientProxy.currentUnrenderedEntities.add(passenger.getUUID());
                }

            }
        }

    }

    public <E extends Entity> void renderEntity(E entityIn, double x, double y, double z, float yaw, float partialTicks, PoseStack matrixStack, MultiBufferSource bufferIn, int packedLight) {
        com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.renderEntity(entityIn, yaw, partialTicks, matrixStack, bufferIn, packedLight);
    }

    protected void translateToPouch(PoseStack matrixStack) {
        this.getParentModel().root.translateAndRotate(matrixStack);
        this.getParentModel().body.translateAndRotate(matrixStack);
    }
}

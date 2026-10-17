package com.github.alexthe666.alexsmobs.client.render.layer;

import com.github.alexthe666.alexsmobs.ClientProxy;
import com.github.alexthe666.alexsmobs.client.model.ModelCachalotWhale;
import com.github.alexthe666.alexsmobs.client.render.RenderCachalotWhale;
import com.github.alexthe666.alexsmobs.entity.EntityCachalotWhale;
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

public class LayerCachalotWhaleCapturedSquid  extends RenderLayer<EntityCachalotWhale, ModelCachalotWhale> {

    public LayerCachalotWhaleCapturedSquid(RenderCachalotWhale render) {
        super(render);
    }

    // From 1.21.9 up the squid is not drawn in here. That draw goes into the whale's own render
    // submission and gets replayed later in the frame, and on 26.2 it took the whole game down the
    // moment a whale bit a giant squid, with no crash report (reports 1 and 6). The server already
    // moves the squid into the whale's mouth every tick, so there RenderGiantSquid just draws it
    // where it is.
    public void render(PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn, EntityCachalotWhale whale, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        //? if <1.21.9 {
        if(whale.hasCaughtSquid() && whale.isAlive()){
            Entity squid = whale.getCaughtSquid();
            if(squid != null && squid.isAlive()){
                boolean rightSquid = !whale.isHoldingSquidLeft();
                float riderRot = squid.yRotO + (squid.getYRot() - squid.yRotO) * partialTicks;
                net.minecraft.client.model.EntityModel<?> modelBase = com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.rendererModel(squid);
                if(modelBase != null){
                    ClientProxy.currentUnrenderedEntities.remove(squid.getUUID());
                    matrixStackIn.pushPose();
                    translateToPouch(matrixStackIn);
                    matrixStackIn.translate(rightSquid ? -1.2F : 1.2F, -0, -3.4F);
                    matrixStackIn.mulPose(Axis.ZP.rotationDegrees(180F));
                    matrixStackIn.mulPose(Axis.YP.rotationDegrees(riderRot + (rightSquid ? -90F : 90F)));
                    renderEntity(squid, 0, 0, 0, 0, partialTicks, matrixStackIn, bufferIn, packedLightIn);
                    matrixStackIn.popPose();
                    ClientProxy.currentUnrenderedEntities.add(squid.getUUID());
                }
            }
        }
        //?}
    }

    public <E extends Entity> void renderEntity(E entityIn, double x, double y, double z, float yaw, float partialTicks, PoseStack matrixStack, MultiBufferSource bufferIn, int packedLight) {
        com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.renderEntity(entityIn, yaw, partialTicks, matrixStack, bufferIn, packedLight);
    }

    protected void translateToPouch(PoseStack matrixStack) {
        this.getParentModel().root.translateAndRotate(matrixStack);
        this.getParentModel().body.translateAndRotate(matrixStack);
        this.getParentModel().head.translateAndRotate(matrixStack);
        this.getParentModel().jaw.translateAndRotate(matrixStack);
    }
}


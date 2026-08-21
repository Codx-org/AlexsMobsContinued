package com.github.alexthe666.alexsmobs.client.model.layered;

import com.github.alexthe666.alexsmobs.entity.util.Maths;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ModelFrontierCap extends HumanoidModel {
    public ModelPart tail;
    public ModelPart hat;

    public ModelFrontierCap(ModelPart p_170677_) {
        super(p_170677_);
        this.hat = p_170677_.getChild("head").getChild("frontierhat");
        this.tail = hat.getChild("tail");
    }

    public static LayerDefinition createArmorLayer(CubeDeformation deformation) {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(deformation, 0.0F);
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition head = partdefinition.getChild("head");

        PartDefinition front = head.addOrReplaceChild("frontierhat", CubeListBuilder.create().texOffs(32, 32).addBox(-4.0F, -10.5F, -4.0F, 8.0F, 4.0F, 8.0F, deformation), PartPose.offset(0, 0, 0));
        front.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(36, 46).addBox(-1.5F, -0.3F, -1.5F, 3.0F, 13.0F, 3.0F, deformation), PartPose.offsetAndRotation(4.4F, -7.5F, 4.5F, 0.1956514098143546F, -0.03909537541112055F, -0.11728612207217244F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    public ModelFrontierCap withAnimations(LivingEntity entity){
        if(entity != null){
            float partialTick = Minecraft.getInstance().getFrameTime();
            amAnimateTail(entity.walkAnimation.position() + partialTick, entity.walkAnimation.speed(partialTick));
        }
        return  this;
    }

    // 1.21.2 swapped the armour hook's wearer for a render state, and the port dropped the
    // withAnimations() call rather than port it -- so the tail stopped moving on every node from
    // there up. walkAnimationPos/walkAnimationSpeed are the same two numbers
    // walkAnimation.position()/speed(partialTick) gave, already interpolated by the extract pass,
    // so the animation itself is untouched; it only changes where the inputs come from.
    // See docs/notes/bug-reports.md #60.
    //? if >=1.21.2 {
    /*public ModelFrontierCap withAnimations(net.minecraft.client.renderer.entity.state.LivingEntityRenderState state){
        if(state != null){
            amAnimateTail(state.walkAnimationPos, state.walkAnimationSpeed);
        }
        return this;
    }

    // The only seam NeoForge has: its armour hook is handed no wearer at all (just the stack, the
    // layer type and the default model), so there is nothing to animate from there. From 1.21.9
    // this is also where EVERY loader gets it -- EquipmentLayerRenderer only submits the model and
    // setupAnim(state) is re-run at flush, which would otherwise overwrite a pose set beforehand.
    @Override
    public void setupAnim(net.minecraft.client.renderer.entity.state.HumanoidRenderState state) {
        super.setupAnim(state);
        amAnimateTail(state.walkAnimationPos, state.walkAnimationSpeed);
    }
    *///?}

    private void amAnimateTail(float limbSwing, float limbSwingAmount){
        tail.xRot = 0.1956514098143546F + limbSwingAmount * Maths.rad(80) + Mth.cos(limbSwing * 0.3F) * 0.2F * limbSwingAmount;
        tail.yRot = -0.03909537541112055F + limbSwingAmount * Maths.rad(10) - Mth.cos(limbSwing * 0.4F) * 0.3F * limbSwingAmount;
        tail.zRot = -0.11728612207217244F + limbSwingAmount * Maths.rad(10);
    }

}

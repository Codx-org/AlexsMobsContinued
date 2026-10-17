package com.github.alexthe666.alexsmobs.client.model.layered;

import com.github.alexthe666.alexsmobs.entity.util.Maths;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class ModelAMElytra extends HumanoidModel {
    private final ModelPart rightWing;
    private final ModelPart leftWing;

    public ModelAMElytra(ModelPart part) {
        super(part);
        this.leftWing = part.getChild("body").getChild("left_wing");
        this.rightWing = part.getChild("body").getChild("right_wing");
    }

    public static LayerDefinition createLayer(CubeDeformation deformation) {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(deformation, 0.0F);
        PartDefinition partdefinition = meshdefinition.getRoot().getChild("body");
        CubeDeformation cubedeformation = new CubeDeformation(1.0F);
        partdefinition.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(32, 32).addBox(-10.0F, 0.0F, 0.0F, 10.0F, 20.0F, 2.0F, cubedeformation), PartPose.offsetAndRotation(5.0F, 0.0F, 0.0F, 0.2617994F, 0.0F, -0.2617994F));
        partdefinition.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(32, 32).mirror().addBox(0.0F, 0.0F, 0.0F, 10.0F, 20.0F, 2.0F, cubedeformation), PartPose.offsetAndRotation(-5.0F, 0.0F, 0.0F, 0.2617994F, 0.0F, 0.2617994F));
        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    public ModelAMElytra withAnimations(LivingEntity entity){
        if(entity != null) {
            final float partialTick = Minecraft.getInstance().getFrameTime();
            final float limbSwingAmount = entity.walkAnimation.speed(partialTick);
            final float limbSwing = entity.walkAnimation.position() + partialTick;
            setupAnim(entity, limbSwing, limbSwingAmount, entity.tickCount + partialTick, 0, 0);
        }
        return  this;
    }

    // 1.21.2 swapped the armour hook's wearer for a render state, and the port dropped the
    // withAnimations() call rather than port it -- so the wings froze in the folded walking pose on
    // every node from there up, including while gliding (report #44 restored the gliding itself).
    // The state carries the wearer's wing pose directly as elytraRotX/Y/Z: vanilla's extract pass
    // reads them off LivingEntity#elytraAnimationState, which is the exact 0.1-per-tick lerp
    // towards the fall-flying/crouching targets that the entity-driven body below runs by hand.
    // See docs/notes/bug-reports.md #60.
    //? if >=1.21.2 {
    /*public ModelAMElytra withAnimations(net.minecraft.client.renderer.entity.state.LivingEntityRenderState state){
        if(state instanceof net.minecraft.client.renderer.entity.state.HumanoidRenderState humanoid){
            amAnimateWings(humanoid);
        }
        return this;
    }

    // NeoForge's armour hook is handed no wearer at all, and from 1.21.9 setupAnim(state) is re-run
    // at flush on every loader -- see ModelFrontierCap for the full note.
    @Override
    public void setupAnim(net.minecraft.client.renderer.entity.state.HumanoidRenderState state) {
        super.setupAnim(state);
        amAnimateWings(state);
    }

    private void amAnimateWings(net.minecraft.client.renderer.entity.state.HumanoidRenderState state) {
        this.leftWing.x = 5.0F;
        this.leftWing.y = state.isCrouching && !state.isFallFlying ? -1.0F : 0.0F;
        this.leftWing.xRot = state.elytraRotX;
        this.leftWing.yRot = state.elytraRotY;
        this.leftWing.zRot = state.elytraRotZ;
        this.rightWing.x = -this.leftWing.x;
        this.rightWing.y = this.leftWing.y;
        this.rightWing.xRot = this.leftWing.xRot;
        this.rightWing.yRot = -this.leftWing.yRot;
        this.rightWing.zRot = -this.leftWing.zRot;
    }
    *///?}

    public void setupAnim(LivingEntity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float f = 0.2617994F;
        float f1 = -0.2617994F;
        float f2 = 0.0F;
        float f3 = 0.0F;
        if (entityIn.isFallFlying()) {
            float f4 = 1.0F;
            Vec3 vector3d = entityIn.getDeltaMovement();
            if (vector3d.y < 0.0D) {
                Vec3 vector3d1 = vector3d.normalize();
                f4 = 1.0F - (float)Math.pow(-vector3d1.y, 1.5D);
            }

            f = f4 * 0.34906584F + (1.0F - f4) * f;
            f1 = f4 * (-Mth.HALF_PI) + (1.0F - f4) * f1;
        } else if (entityIn.isCrouching()) {
            f = 0.6981317F;
            f1 = -Maths.QUARTER_PI;
            f2 = -1.0F;
            f3 = 0.08726646F;
        }

        this.leftWing.x = 5.0F;
        this.leftWing.y = f2;
        if (entityIn instanceof AbstractClientPlayer) {
            // 1.21.9 removed AbstractClientPlayer's three elytraRot* fields; the identical 0.1-per-tick
            // lerp now lives in LivingEntity#elytraAnimationState, which LivingEntity#tick drives for
            // every living entity (javap-verified). Reading it at partialTick 1.0F gives this tick's
            // fully-advanced value, which is what writing the fields and reading them straight back did.
            //? if >=1.21.9 {
            /*net.minecraft.world.entity.ElytraAnimationState amElytra = entityIn.elytraAnimationState;
            this.leftWing.xRot = amElytra.getRotX(1.0F);
            this.leftWing.yRot = amElytra.getRotY(1.0F);
            this.leftWing.zRot = amElytra.getRotZ(1.0F);
            *///?} else {
            AbstractClientPlayer abstractclientplayerentity = (AbstractClientPlayer)entityIn;
            abstractclientplayerentity.elytraRotX = (float)((double)abstractclientplayerentity.elytraRotX + (double)(f - abstractclientplayerentity.elytraRotX) * 0.1D);
            abstractclientplayerentity.elytraRotY = (float)((double)abstractclientplayerentity.elytraRotY + (double)(f3 - abstractclientplayerentity.elytraRotY) * 0.1D);
            abstractclientplayerentity.elytraRotZ = (float)((double)abstractclientplayerentity.elytraRotZ + (double)(f1 - abstractclientplayerentity.elytraRotZ) * 0.1D);
            this.leftWing.xRot = abstractclientplayerentity.elytraRotX;
            this.leftWing.yRot = abstractclientplayerentity.elytraRotY;
            this.leftWing.zRot = abstractclientplayerentity.elytraRotZ;
            //?}
        } else {
            this.leftWing.xRot = f;
            this.leftWing.zRot = f1;
            this.leftWing.yRot = f3;
        }

        this.rightWing.x = -this.leftWing.x;
        this.rightWing.yRot = -this.leftWing.yRot;
        this.rightWing.y = this.leftWing.y;
        this.rightWing.xRot = this.leftWing.xRot;
        this.rightWing.zRot = -this.leftWing.zRot;
    }
}

package com.github.alexthe666.alexsmobs.client.model;

import com.github.alexthe666.alexsmobs.entity.EntityUnderminer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.HumanoidArm;

/**
 * The underminer's TALL form — 30% of them spawn this way ({@code finalizeSpawn} clears the
 * {@code Dwarf} flag on a 0.3 roll), and it is a plain vanilla humanoid built from
 * {@code AMModelLayers.UNDERMINER}, which is nothing but {@code HumanoidModel.createMesh}.
 *
 * <p>Upstream just held a {@code HumanoidModel<EntityUnderminer>}. 1.21.2 re-keyed that class on
 * {@code HumanoidRenderState}, so this port dropped the tall form and rendered EVERY underminer
 * with the dwarf model — while {@code getTextureLocation} kept handing back {@code underminer_0/1}
 * for the ones that are not dwarves. Dwarf geometry with a humanoid skin is a scrambled mess of
 * UVs, which is bug report #57 ("the miner ghost sometimes has a very strange texture"): 35 nodes,
 * every loader, ~30% of the underminers on each.
 *
 * <p>Two sibling arms rather than one shared body, because the base class is the whole difference:
 *
 * <ul>
 *   <li><b>&lt;1.21.2</b> — literally upstream's type. Nothing to add.
 *   <li><b>1.21.2+</b> — a {@link com.github.alexthe666.alexsmobs.client.render.compat.EntityModel}
 *       (the entity-keyed base this mod's renderers and layers are declared against) that OWNS a
 *       vanilla {@code HumanoidModel} and drives it through a render state it fills itself. Going
 *       through vanilla's own {@code setupAnim} rather than re-deriving the humanoid walk cycle is
 *       what keeps this correct across 1.21.2 → 26.2 without a single version gate in the body.
 * </ul>
 *
 * <p>The state is default-constructed and only the fields upstream's call actually varied are
 * written. That is safe on every version in the range: {@code mainArm}, {@code attackArm},
 * {@code useItemHand}, {@code rightArmPose}/{@code leftArmPose} and {@code swingAnimationType} all
 * carry non-null initialisers in vanilla's own constructors (read from bytecode on 1.21.4 and
 * 26.2), so nothing here can hand {@code setupAnim} a null to switch on.
 *
 * <p>{@code translateToHand} is spelled out instead of delegated because 1.21.9 gave vanilla's
 * version a leading render-state parameter, and a Stonecutter block cannot nest inside the class
 * block above. Vanilla's body is one call on the arm part, which is public on every version.
 */
//? if >=1.21.2 {
/*public class ModelUnderminerHumanoid extends com.github.alexthe666.alexsmobs.client.render.compat.EntityModel<EntityUnderminer> {

    private final HumanoidModel<net.minecraft.client.renderer.entity.state.HumanoidRenderState> inner;
    private final net.minecraft.client.renderer.entity.state.HumanoidRenderState state =
            new net.minecraft.client.renderer.entity.state.HumanoidRenderState();

    public ModelUnderminerHumanoid(ModelPart root) {
        this.inner = new HumanoidModel<>(root);
    }

    @Override
    public void setupAnim(EntityUnderminer entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.state.walkAnimationPos = limbSwing;
        this.state.walkAnimationSpeed = limbSwingAmount;
        this.state.ageInTicks = ageInTicks;
        this.state.yRot = netHeadYaw;
        this.state.xRot = headPitch;
        this.state.isBaby = this.young;
        this.state.isPassenger = this.riding;
        this.state.attackTime = this.attackTime;
        this.state.mainArm = entity.getMainArm();
        this.state.pose = entity.getPose();
        this.inner.setupAnim(this.state);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        this.inner.root().render(poseStack, buffer, packedLight, packedOverlay,
                com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.packColor(red, green, blue, alpha));
    }

    public void translateToHand(HumanoidArm arm, PoseStack poseStack) {
        (arm == HumanoidArm.LEFT ? this.inner.leftArm : this.inner.rightArm).translateAndRotate(poseStack);
    }
}
*///?} else {
public class ModelUnderminerHumanoid extends HumanoidModel<EntityUnderminer> {

    public ModelUnderminerHumanoid(ModelPart root) {
        super(root);
    }
}
//?}

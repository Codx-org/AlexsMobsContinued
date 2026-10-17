package com.github.alexthe666.alexsmobs.client.render.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Pre-1.21.2 {@code EntityModel<T>} — the type parameter is the entity, as it used to be.
 *
 * <p>1.21.2 changed it to a render state, which breaks the ~13 declarations in this mod that name
 * {@code EntityModel<SomeEntity>} as a renderer's or layer's model type. Keeping the simple name
 * means those files only need their import swapped, exactly like the renderers next to this class.
 *
 * <p>The base of this mod's whole model hierarchy — {@code BasicEntityModel} — extends this on
 * 1.21.2+, so every concrete model in the mod is one of these and the declarations stay valid.
 *
 * <p>1.21.2 also made both of {@code Model}'s {@code renderToBuffer} overloads {@code final}, so a
 * model can no longer draw itself through the vanilla entry point. The eight-float form below is
 * the hierarchy's own draw call, invoked directly by {@link LivingEntityRenderer}; the root handed
 * to vanilla is empty, so vanilla's own final render draws nothing.
 */
public abstract class EntityModel<T extends Entity> extends net.minecraft.client.model.EntityModel<AMRenderState> {

	/** Shared and never mutated: no cubes and no children, so every vanilla traversal is a no-op. */
	private static final ModelPart EMPTY_ROOT = new ModelPart(List.of(), Map.of());

	/** Re-declared: {@code EntityModel} dropped these public fields in 1.21.2, and models read them. */
	public boolean young = true;
	public boolean riding;
	public float attackTime;

	protected EntityModel() {
		this(RenderType::entityCutoutNoCull);
	}

	protected EntityModel(Function<ResourceLocation, RenderType> renderType) {
		super(EMPTY_ROOT, renderType);
	}

	/**
	 * The one hook vanilla still calls. It unpacks the render state back into the arguments the
	 * pre-1.21.2 {@code setupAnim} was given.
	 *
	 * <p>{@link AMRenderState#chromeOnly} means the renderer is only re-entering vanilla's body to
	 * reach its leash/name-tag tail, so the animation must not run a second time.
	 */
	@Override
	public final void setupAnim(AMRenderState state) {
		if (state.chromeOnly) {
			return;
		}
		@SuppressWarnings("unchecked")
		T entity = (T) state.entity;
		this.young = state.isBaby;
		this.riding = state.riding;
		this.attackTime = state.attackTime;
		this.prepareMobModel(entity, state.walkAnimationPos, state.walkAnimationSpeed, state.partialTick);
		this.setupAnim(entity, state.walkAnimationPos, state.walkAnimationSpeed, state.ageInTicks, state.yRot, state.xRot);
	}

	/** The draw call. Overridden by {@code BasicEntityModel} to walk its own parts. */
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
			float red, float green, float blue, float alpha) {
	}

	/** 1.21.2 dropped {@code EntityModel#copyPropertiesTo}; it only ever carried these three flags. */
	public void copyPropertiesTo(EntityModel<?> other) {
		other.young = this.young;
		other.riding = this.riding;
		other.attackTime = this.attackTime;
	}

	/**
	 * Overload for a vanilla-model target — armour {@link net.minecraft.client.model.HumanoidModel}s
	 * that layers pose to match the mob. Those carry no young/riding/attackTime fields in 1.21.2+
	 * (that state moved to the render state), so there is nothing to copy. A compat model argument
	 * still binds to the more-specific overload above.
	 */
	public void copyPropertiesTo(net.minecraft.client.model.EntityModel<?> other) {
	}

	public abstract void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch);

	public void prepareMobModel(T entity, float limbSwing, float limbSwingAmount, float partialTick) {
	}
}

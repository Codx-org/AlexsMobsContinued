package com.github.alexthe666.alexsmobs.client.render.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * Pre-1.21.2 {@code EntityRenderer<T>} on top of the render-state architecture.
 *
 * <p>Subclasses keep their single type parameter, their {@code render(T, float, float, PoseStack,
 * MultiBufferSource, int)} override and their {@code getTextureLocation(T)}; this class does the
 * extract pass for them and dispatches.
 */
public abstract class EntityRenderer<T extends Entity>
		extends net.minecraft.client.renderer.entity.EntityRenderer<T, AMRenderState> {

	/**
	 * The state currently being rendered, so the legacy hooks can reach fields that used to be
	 * parameters. {@code EntityRenderer} reuses exactly one state instance per renderer and
	 * rendering is single-threaded, so this is simply the argument of the enclosing
	 * {@link #render(AMRenderState, PoseStack, MultiBufferSource, int)} call.
	 */
	protected AMRenderState renderingState;

	protected EntityRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public AMRenderState createRenderState() {
		return new AMRenderState();
	}

	@Override
	public void extractRenderState(T entity, AMRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.entity = entity;
		state.partialTick = partialTick;
		state.entityYaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
		state.bookFlags = com.github.alexthe666.alexsmobs.client.render.AMBookPose.capture();
	}

	//? if >=1.21.9 {
	/*@Override
	public final void submit(AMRenderState state, PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector collector, net.minecraft.client.renderer.state.CameraRenderState camera) {
		AMSubmitBuffers buffers = new AMSubmitBuffers(collector, camera);
		this.dispatch(state, poseStack, buffers, state.lightCoords);
		buffers.flush();
	}
	*///?} else {
	@Override
	public final void render(AMRenderState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		this.dispatch(state, poseStack, bufferSource, packedLight);
	}
	//?}

	private void dispatch(AMRenderState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		this.renderingState = state;
		@SuppressWarnings("unchecked")
		T entity = (T) state.entity;
		// The draw may be happening long after the animal dictionary cleared its pose flags — see
		// AMBookPose. Restore them for its duration and put back whatever was there.
		int previousBookFlags = com.github.alexthe666.alexsmobs.client.render.AMBookPose.swap(state.bookFlags);
		try {
			this.render(entity, state.entityYaw, state.partialTick, poseStack, bufferSource, packedLight);
		} finally {
			com.github.alexthe666.alexsmobs.client.render.AMBookPose.swap(previousBookFlags);
		}
	}

	/**
	 * The pre-1.21.2 entry point. The default reproduces what the old {@code EntityRenderer#render}
	 * did — leash and name tag — which is all the modern one does too, so subclasses calling
	 * {@code super.render(...)} still get it.
	 */
	public void render(T entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		//? if >=1.21.9 {
		/*AMSubmitBuffers buffers = AMSubmitBuffers.of(bufferSource);
		if (buffers != null) {
			buffers.flush();
			super.submit(this.renderingState, poseStack, buffers.collector(), buffers.camera());
		}
		*///?} else {
		super.render(this.renderingState, poseStack, bufferSource, packedLight);
		//?}
	}

	/**
	 * Re-declared because the modern {@code EntityRenderer} dropped it; the ~16 subclasses in this
	 * mod still implement and call it.
	 */
	public abstract ResourceLocation getTextureLocation(T entity);

	// ---------------------------------------------------------------------------------------
	// Legacy name-tag bridges. 1.21.2 made shouldShowName take a squared distance and renderNameTag
	// take the render state; the renderers that reimplement render(T, …) still call the old
	// entity-only forms. Route them to the modern ones using the state currently being rendered.
	// ---------------------------------------------------------------------------------------

	// `this`, not `super` — see the note on the sibling LivingEntityRenderer bridge: a legacy call
	// must reach the most-derived two-arg override, not step over it.
	protected boolean shouldShowName(T entity) {
		return this.shouldShowName(entity, this.entityRenderDispatcher.distanceToSqr(entity));
	}

	protected void renderNameTag(T entity, net.minecraft.network.chat.Component name, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		// See the note on the sibling LivingEntityRenderer bridge: submitNameTag reads the text off the
		// render state, so `name` is unused from 1.21.9 and carries the same value either way.
		//? if >=26 {
		/*AMSubmitBuffers buffers = AMSubmitBuffers.of(bufferSource);
		if (buffers != null) {
			super.submitNameDisplay(this.renderingState, poseStack, buffers.collector(), buffers.camera());
		}
		*///?} elif >=1.21.9 {
		/*AMSubmitBuffers buffers = AMSubmitBuffers.of(bufferSource);
		if (buffers != null) {
			super.submitNameTag(this.renderingState, poseStack, buffers.collector(), buffers.camera());
		}
		*///?} else {
		super.renderNameTag(this.renderingState, name, poseStack, bufferSource, packedLight);
		//?}
	}
}

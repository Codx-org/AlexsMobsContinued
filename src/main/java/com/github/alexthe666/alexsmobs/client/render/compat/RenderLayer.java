package com.github.alexthe666.alexsmobs.client.render.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;

/**
 * Pre-1.21.2 {@code RenderLayer<T, M>} — first parameter is the entity, as it used to be.
 *
 * <p>Vanilla's layer now receives only {@code (poseStack, bufferSource, packedLight, state, yRot,
 * xRot)}; the old ten-argument form is reconstructed here from {@link AMRenderState}. The values
 * are the same ones {@link LivingEntityRenderer} passes to the model, so a layer sees exactly what
 * it saw before.
 *
 * <p>{@code T} is bounded by {@code Entity}, matching the pre-1.21.2 vanilla bound: {@code
 * LayerRainbow} uses this raw and declares its entity parameter as {@code Entity}, which only
 * overrides if the erasure agrees.
 *
 * <p>From 1.21.9 the vanilla entry point is {@code submit(…, SubmitNodeCollector, …)} rather than
 * {@code render(…, MultiBufferSource, …)}, so the two spellings are separate gated overrides that
 * both funnel into {@link #dispatch}. The {@code submit} arm wraps the collector in its own {@link
 * AMSubmitBuffers} and flushes it, rather than relying on the parent renderer's — a layer of this
 * mod can be attached to a *vanilla* renderer through {@code EntityRenderersEvent.AddLayers} (that
 * is exactly what {@code LayerRainbow} does), in which case there is no compat renderer above it.
 */
public abstract class RenderLayer<T extends Entity, M extends EntityModel<?>>
		extends net.minecraft.client.renderer.entity.layers.RenderLayer<AMRenderState, M> {

	/** The state of the render currently in flight, for subclasses that reach the vanilla statics. */
	protected AMRenderState renderingState;

	public RenderLayer(RenderLayerParent<AMRenderState, M> parent) {
		super(parent);
	}

	//? if >=1.21.9 {
	/*@Override
	public final void submit(PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector collector, int packedLight, AMRenderState state, float yRot, float xRot) {
		AMSubmitBuffers buffers = new AMSubmitBuffers(collector);
		this.dispatch(poseStack, buffers, packedLight, state, yRot, xRot);
		buffers.flush();
	}
	*///?} else {
	@Override
	public final void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, AMRenderState state, float yRot, float xRot) {
		this.dispatch(poseStack, bufferSource, packedLight, state, yRot, xRot);
	}
	//?}

	final void dispatch(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, AMRenderState state, float yRot, float xRot) {
		this.renderingState = state;
		@SuppressWarnings("unchecked")
		T entity = (T) state.entity;
		this.render(poseStack, bufferSource, packedLight, entity,
				state.walkAnimationPos, state.walkAnimationSpeed, state.partialTick,
				state.ageInTicks, yRot, xRot);
	}

	/**
	 * Pre-1.21.2 {@code renderColoredCutoutModel(model, tex, …, entity, r, g, b)}.
	 *
	 * <p>Deliberately <em>not</em> delegated to the vanilla static of that name: the static calls
	 * {@code Model#renderToBuffer}, which 1.21.2 made {@code final}, so it walks the empty root
	 * {@link EntityModel} hands vanilla and draws nothing. Reproducing the two-line body against the
	 * compat model's own eight-float {@code renderToBuffer} is what actually draws.
	 */
	protected final void renderColoredModel(EntityModel<?> model, net.minecraft.resources.ResourceLocation texture,
			PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int color) {
		VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));
		int overlay = net.minecraft.client.renderer.entity.LivingEntityRenderer.getOverlayCoords(this.renderingState, 0.0F);
		model.renderToBuffer(poseStack, consumer, packedLight, overlay,
				ARGB.red(color) / 255.0F, ARGB.green(color) / 255.0F, ARGB.blue(color) / 255.0F, ARGB.alpha(color) / 255.0F);
	}

	public abstract void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T entity,
			float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch);
}

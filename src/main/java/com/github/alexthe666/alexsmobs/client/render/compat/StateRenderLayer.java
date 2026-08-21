package com.github.alexthe666.alexsmobs.client.render.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * A render layer that can be attached to <em>any</em> renderer, this mod's or vanilla's.
 *
 * <p>{@link RenderLayer} next door cannot: it is bound to {@link AMRenderState}, so the bridge
 * method the compiler generates casts whatever state vanilla hands it to that class. Attaching one
 * to a vanilla renderer therefore throws a {@code ClassCastException} on the first frame — which is
 * exactly what {@code LayerRainbow} needs to do, since it is registered against every living
 * entity type in the game through {@code EntityRenderersEvent.AddLayers}.
 *
 * <p>So this base is typed on the vanilla {@link EntityRenderState} and is deliberately <em>raw</em>
 * with respect to its supertype: the erasure of vanilla's {@code render}/{@code submit} is what the
 * two arms below declare, which is what makes them overrides. The entity behind the state comes back
 * through {@link AMStateAccess}, which {@code mixin.renderstate.EntityRendererMixin} stamps onto
 * every state, vanilla ones included.
 *
 * <p>The 1.21.9 submit/render split lives here rather than in the subclass because Stonecutter
 * blocks are siblings and never nest — a subclass that already needs a gated class declaration
 * could not gate its entry point a second time inside it.
 *
 * <p>⚠️ The supertype is spelled fully qualified and must <strong>never</strong> be imported: the
 * {@code !mc2102-render-import-layer} rule rewrites that exact import statement to {@link
 * RenderLayer}, and the same trap has already retargeted two mixins in this tree.
 */
@SuppressWarnings("rawtypes")
public abstract class StateRenderLayer extends net.minecraft.client.renderer.entity.layers.RenderLayer {

	@SuppressWarnings("unchecked")
	protected StateRenderLayer(net.minecraft.client.renderer.entity.RenderLayerParent parent) {
		super(parent);
	}

	//? if >=1.21.9 {
	/*@Override
	public final void submit(PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector collector, int packedLight, EntityRenderState state, float yRot, float xRot) {
		AMSubmitBuffers buffers = new AMSubmitBuffers(collector);
		this.draw(poseStack, buffers, packedLight, state, yRot, xRot);
		buffers.flush();
	}
	*///?} else {
	@Override
	public final void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, EntityRenderState state, float yRot, float xRot) {
		this.draw(poseStack, bufferSource, packedLight, state, yRot, xRot);
	}
	//?}

	/** The one hook a subclass implements, identical on every node. */
	protected abstract void draw(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
			EntityRenderState state, float yRot, float xRot);
}

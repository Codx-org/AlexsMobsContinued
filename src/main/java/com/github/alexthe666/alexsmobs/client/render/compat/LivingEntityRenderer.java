package com.github.alexthe666.alexsmobs.client.render.compat;


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import org.jetbrains.annotations.Nullable;

/**
 * Pre-1.21.2 {@code LivingEntityRenderer<T, M>} on top of the render-state architecture.
 *
 * <p>Two type parameters, not three: {@link AMRenderState} is fixed as the state type, so all ~97
 * renderer declarations in this mod compile unchanged.
 *
 * <p>The bound on {@code M} is the sibling {@link EntityModel} rather than vanilla's
 * {@code EntityModel<? super S>} because the whole hierarchy is written against the eight-float
 * {@code renderToBuffer}. That matters: 1.21.2 made both of {@code Model}'s {@code renderToBuffer}
 * overloads {@code final}, so a model can no longer intercept the vanilla one and its boxes are
 * invisible to it. This renderer therefore has to call the model's own eight-float method itself,
 * which is why {@link #render} reimplements vanilla's body instead of delegating to it.
 */
public abstract class LivingEntityRenderer<T extends LivingEntity, M extends EntityModel<?>>
		extends net.minecraft.client.renderer.entity.LivingEntityRenderer<T, AMRenderState, M> {

	/** See {@link EntityRenderer#renderingState} — same contract. */
	protected AMRenderState renderingState;

	public LivingEntityRenderer(EntityRendererProvider.Context context, M model, float shadowRadius) {
		super(context, model, shadowRadius);
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
		state.attackTime = entity.getAttackAnim(partialTick);
		state.riding = entity.isPassenger();
		state.bookFlags = com.github.alexthe666.alexsmobs.client.render.AMBookPose.capture();
	}

	//? if >=1.21.9 {
	/*@Override
	public void submit(AMRenderState state, PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector collector, net.minecraft.client.renderer.state.CameraRenderState camera) {
		AMSubmitBuffers buffers = new AMSubmitBuffers(collector, camera);
		this.dispatch(state, poseStack, buffers, state.lightCoords);
		buffers.flush();
	}
	*///?} else {
	@Override
	public void render(AMRenderState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
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
	 * The pre-1.21.2 entry point. The default reproduces vanilla {@code LivingEntityRenderer#render}
	 * — draw the model and its layers, then the leash and name tag — reading the per-frame values
	 * out of the render state the dispatcher stashed in {@link #renderingState}. The ~90 renderers
	 * that only override hooks (texture, scale, rotations) get this body; the handful that fully
	 * reimplemented {@code render(T, …)} override it, and those that call {@code super.render(T, …)}
	 * still reach it.
	 */
	public void render(T entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		AMRenderState state = this.renderingState;

		poseStack.pushPose();
		if (state.hasPose(Pose.SLEEPING)) {
			Direction bed = state.bedOrientation;
			if (bed != null) {
				float offset = state.eyeHeight - 0.1F;
				poseStack.translate(-bed.getStepX() * offset, 0.0F, -bed.getStepZ() * offset);
			}
		}

		float scale = state.scale;
		poseStack.scale(scale, scale, scale);
		this.setupRotations(entity, poseStack, state.ageInTicks, state.bodyRot, state.partialTick, scale);
		poseStack.scale(-1.0F, -1.0F, 1.0F);
		this.scale(entity, poseStack, state.partialTick);
		poseStack.translate(0.0F, -1.501F, 0.0F);
		this.model.setupAnim(state);

		boolean bodyVisible = this.isBodyVisible(entity);
		boolean translucent = !bodyVisible && !state.isInvisibleToPlayer;
		// 1.21.9 turned the field into a method.
		//? if >=1.21.9 {
		/*boolean glowing = state.appearsGlowing();
		*///?} else {
		boolean glowing = state.appearsGlowing;
		//?}
		RenderType renderType = this.getRenderType(entity, bodyVisible, translucent, glowing);
		if (renderType != null) {
			VertexConsumer consumer = bufferSource.getBuffer(renderType);
			int overlay = net.minecraft.client.renderer.entity.LivingEntityRenderer.getOverlayCoords(
					state, this.getWhiteOverlayProgress(entity, state.partialTick));
			int tint = ARGB.multiply(translucent ? 654311423 : -1, this.getModelTint(state));
			this.model.renderToBuffer(poseStack, consumer, packedLight, overlay,
					ARGB.red(tint) / 255.0F, ARGB.green(tint) / 255.0F,
					ARGB.blue(tint) / 255.0F, ARGB.alpha(tint) / 255.0F);
		}

		if (this.shouldRenderLayers(state)) {
			this.renderLayers(poseStack, bufferSource, packedLight, state);
		}

		poseStack.popPose();

		// Leash and name tag live in EntityRenderer#render, which is the *grandparent* here — and
		// its leash renderer is private static, so there is no way to call it directly and no
		// super.super. Instead run vanilla's own body with chromeOnly set, which makes every hook
		// below a no-op (no render type, no layers, no animation), leaving only that tail.
		state.chromeOnly = true;
		try {
			this.renderChrome(state, poseStack, bufferSource, packedLight);
		} finally {
			state.chromeOnly = false;
		}
	}

	/**
	 * The layer loop, for the three renderers ({@code RenderUnderminer}, {@code RenderTiger},
	 * {@code RenderFarseer}) that reimplement {@link #render(LivingEntity, float, float, PoseStack,
	 * MultiBufferSource, int)} wholesale and so never reach the call in that body.
	 *
	 * <p>They must not walk {@link #layers} themselves. A layer on this renderer is only ever a
	 * <em>vanilla</em> {@code RenderLayer<AMRenderState, M>}: besides this mod's compat layers the
	 * list also holds {@code LayerRainbow} (a {@link StateRenderLayer}, attached to every living
	 * renderer in the game) and whatever other mods add — Trinkets registers one on Fabric. Casting
	 * the elements down to the compat {@link RenderLayer} to reach its ten-argument {@code render}
	 * therefore {@code ClassCastException}s as soon as such a mob comes into view.
	 */
	protected final void renderAttachedLayers(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		AMRenderState state = this.renderingState;
		if (state != null) {
			this.renderLayers(poseStack, bufferSource, packedLight, state);
		}
	}

	/**
	 * The layer loop. From 1.21.9 a layer submits rather than renders, so it wants the collector
	 * this shim's recorder is wrapping; the recorder is flushed first so the model's own geometry is
	 * submitted ahead of the layers', preserving the pre-1.21.9 draw order.
	 */
	private void renderLayers(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, AMRenderState state) {
		//? if >=1.21.9 {
		/*AMSubmitBuffers buffers = AMSubmitBuffers.of(bufferSource);
		if (buffers == null) {
			return;
		}
		buffers.flush();
		for (net.minecraft.client.renderer.entity.layers.RenderLayer<AMRenderState, M> layer : this.layers) {
			layer.submit(poseStack, buffers.collector(), packedLight, state, state.yRot, state.xRot);
		}
		*///?} else {
		for (net.minecraft.client.renderer.entity.layers.RenderLayer<AMRenderState, M> layer : this.layers) {
			layer.render(poseStack, bufferSource, packedLight, state, state.yRot, state.xRot);
		}
		//?}
	}

	/** Vanilla's leash + name-tag tail, whose entry point is {@code submit} from 1.21.9. */
	private void renderChrome(AMRenderState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		//? if >=1.21.9 {
		/*AMSubmitBuffers buffers = AMSubmitBuffers.of(bufferSource);
		if (buffers != null) {
			buffers.flush();
			super.submit(state, poseStack, buffers.collector(), buffers.camera());
		}
		*///?} else {
		super.render(state, poseStack, bufferSource, packedLight);
		//?}
	}

	// ---------------------------------------------------------------------------------------
	// State-taking hooks: bridged to the legacy entity-taking form, and neutered during the
	// chrome pass above.
	// ---------------------------------------------------------------------------------------

	@Override
	public final ResourceLocation getTextureLocation(AMRenderState state) {
		return this.getTextureLocation(this.entityOf(state));
	}

	@Override
	@Nullable
	protected final RenderType getRenderType(AMRenderState state, boolean bodyVisible, boolean translucent, boolean glowing) {
		return state.chromeOnly ? null : this.getRenderType(this.entityOf(state), bodyVisible, translucent, glowing);
	}

	@Override
	protected final boolean shouldRenderLayers(AMRenderState state) {
		return !state.chromeOnly;
	}

	@Override
	protected final boolean isBodyVisible(AMRenderState state) {
		return this.isBodyVisible(this.entityOf(state));
	}

	@Override
	protected final boolean isShaking(AMRenderState state) {
		return this.isShaking(this.entityOf(state));
	}

	@Override
	protected final float getWhiteOverlayProgress(AMRenderState state) {
		return this.getWhiteOverlayProgress(this.entityOf(state), state.partialTick);
	}

	@Override
	protected final float getFlipDegrees() {
		return this.getFlipDegrees(this.entityOf(this.renderingState));
	}

	/**
	 * Deliberately empty: the shim applies scaling itself via the legacy three-argument form, so
	 * this must do nothing during the chrome pass or it would be applied twice.
	 */
	@Override
	protected final void scale(AMRenderState state, PoseStack poseStack) {
	}

	/** Likewise — {@link #render} calls the legacy six-argument form directly. */
	@Override
	protected final void setupRotations(AMRenderState state, PoseStack poseStack, float bodyRot, float scale) {
	}

	// ---------------------------------------------------------------------------------------
	// The pre-1.21.2 hooks the ~97 renderers in this mod actually override.
	// ---------------------------------------------------------------------------------------

	public abstract ResourceLocation getTextureLocation(T entity);

	@Nullable
	protected RenderType getRenderType(T entity, boolean bodyVisible, boolean translucent, boolean glowing) {
		ResourceLocation texture = this.getTextureLocation(entity);
		if (translucent) {
			return RenderType.itemEntityTranslucentCull(texture);
		} else if (bodyVisible) {
			return this.model.renderType(texture);
		} else {
			return glowing ? RenderType.outline(texture) : null;
		}
	}

	protected boolean isBodyVisible(T entity) {
		return !entity.isInvisible();
	}

	protected boolean isShaking(T entity) {
		return entity.isFullyFrozen();
	}

	protected float getWhiteOverlayProgress(T entity, float partialTicks) {
		return 0.0F;
	}

	protected float getFlipDegrees(T entity) {
		return 90.0F;
	}

	protected void scale(T entity, PoseStack poseStack, float partialTickTime) {
	}

	/**
	 * The default delegates to vanilla's implementation. {@code super.setupRotations} reaches it
	 * even though the state-taking override above is a no-op, and it in turn calls
	 * {@link #getFlipDegrees()} / {@link #isShaking(AMRenderState)}, which bridge back to the
	 * legacy forms — so a subclass that overrides those still gets its behaviour here.
	 */
	protected void setupRotations(T entity, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTicks, float scale) {
		super.setupRotations(this.renderingState, poseStack, rotationYaw, scale);
	}

	// Legacy name-tag + animation bridges. 1.21.2 made shouldShowName take a squared distance,
	// renderNameTag take the render state, and getAttackAnim/getBob take the state; the renderers
	// that reimplement render(T, …) still call the old entity forms. (These duplicate the ones on
	// the sibling compat EntityRenderer — the two shims extend their vanilla counterparts directly,
	// so neither inherits the other's.)
	// `this`, not `super`: the sibling MobRenderer shim overrides the two-arg form to restore
	// vanilla MobRenderer's "only if the mob has a name" clause, and a `super` call here would step
	// straight over it — leaving the three renderers that reimplement render(T, …) labelling every
	// mob after the other 90 stopped.
	protected boolean shouldShowName(T entity) {
		return this.shouldShowName(entity, this.entityRenderDispatcher.distanceToSqr(entity));
	}

	protected void renderNameTag(T entity, net.minecraft.network.chat.Component name, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		// 1.21.9 replaced renderNameTag with submitNameTag, which reads the text off the render state
		// rather than taking it — so `name` is unused there. Every caller in this mod passes the
		// display name, which is exactly what the state carries.
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

	protected float getAttackAnim(T entity, float partialTicks) {
		return entity.getAttackAnim(partialTicks);
	}

	protected float getBob(T entity, float partialTicks) {
		return entity.tickCount + partialTicks;
	}

	/** The old static helper took the entity, not a render state. */
	public static int getOverlayCoords(LivingEntity entity, float whiteOverlayProgress) {
		return net.minecraft.client.renderer.texture.OverlayTexture.pack(
				net.minecraft.client.renderer.texture.OverlayTexture.u(whiteOverlayProgress),
				net.minecraft.client.renderer.texture.OverlayTexture.v(entity.hurtTime > 0 || entity.deathTime > 0));
	}

	@SuppressWarnings("unchecked")
	private T entityOf(AMRenderState state) {
		return (T) state.entity;
	}
}

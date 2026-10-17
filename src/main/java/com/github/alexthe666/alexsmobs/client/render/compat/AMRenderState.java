package com.github.alexthe666.alexsmobs.client.render.compat;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.Entity;

/**
 * The single render state used by every shimmed renderer in this package.
 *
 * <p>1.21.2 split entity rendering into an "extract" pass that copies what the renderer needs out
 * of the entity into a plain data object, and a "render" pass that may only read that object.
 * Alex's Mobs has ~123 renderers, ~33 layers and ~130 models written against the pre-1.21.2 API,
 * where every hook received the live entity. Rather than migrate all of them, this state carries
 * the entity itself plus the handful of per-frame values the old hooks were handed as parameters,
 * and the shims in this package hand them back.
 *
 * <p>That is deliberately the thing vanilla stopped doing. It is safe here because extraction and
 * rendering happen back to back on the render thread within one frame, and because
 * {@code EntityRenderer} reuses exactly one state instance per renderer, so this adds one strong
 * reference per renderer rather than one per entity.
 *
 * <p>{@link #chromeOnly} is the one piece of real machinery — see
 * {@link LivingEntityRenderer#render}.
 */
public class AMRenderState extends LivingEntityRenderState {

	/** The entity being rendered. Valid only between extract and render of the same frame. */
	public Entity entity;

	/** Partial tick for this frame — the old {@code partialTicks} / {@code partialTickTime} arg. */
	public float partialTick;

	/**
	 * Interpolated {@code getYRot()}, i.e. the old {@code entityYaw} argument that the entity
	 * render dispatcher used to pass in. Note this is the entity yaw, not the body yaw — living
	 * renderers took the body rotation from {@link LivingEntityRenderState#bodyRot} instead.
	 */
	public float entityYaw;

	/** The old {@code EntityModel#attackTime}; {@link LivingEntityRenderState} has no equivalent. */
	public float attackTime;

	/** The old {@code EntityModel#riding}. */
	public boolean riding;

	/**
	 * The animal dictionary's per-page pose flags as they stood at extract time — see
	 * {@link com.github.alexthe666.alexsmobs.client.render.AMBookPose}. From 1.21.6 the book's
	 * entity is drawn after the screen's {@code render()} has returned and cleared them, so the
	 * shims re-apply this mask around the draw.
	 */
	public int bookFlags;

	/**
	 * Set while vanilla's {@code LivingEntityRenderer#render} body is being run purely to reach
	 * the leash and name-tag rendering in its {@code EntityRenderer} tail. While it is set the
	 * shim suppresses everything else, so nothing is drawn or animated twice.
	 */
	public boolean chromeOnly;
}

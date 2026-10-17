package com.github.alexthe666.alexsmobs.client.render.compat;


import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Mob;

/**
 * Pre-1.21.2 {@code MobRenderer<T, M>} — two type parameters, on top of
 * {@link LivingEntityRenderer}. 93 of this mod's renderers extend it.
 */
public abstract class MobRenderer<T extends Mob, M extends EntityModel<?>>
		extends LivingEntityRenderer<T, M> {

	public MobRenderer(EntityRendererProvider.Context context, M model, float shadowRadius) {
		super(context, model, shadowRadius);
	}

	@Override
	protected float getShadowRadius(AMRenderState state) {
		return super.getShadowRadius(state) * state.ageScale;
	}

	/**
	 * Vanilla {@code MobRenderer}'s nameplate rule, which this shim would otherwise drop.
	 *
	 * <p>⚠️ This class extends {@link LivingEntityRenderer} — i.e. vanilla
	 * {@code LivingEntityRenderer}, <b>not</b> vanilla {@code MobRenderer} — because the shim exists
	 * to restore the two-type-parameter shape, and the three-parameter {@code MobRenderer} is not on
	 * that path. But "show a name only if the mob actually has one" lives <i>only</i> in
	 * {@code MobRenderer#shouldShowName}: the {@code LivingEntityRenderer} one it calls up to answers
	 * a different question (team visibility, invisibility, is-it-the-camera) and returns {@code true}
	 * for any ordinary visible mob. Inheriting from the wrong side therefore gave all 93 renderers a
	 * permanent floating type name — "Grizzly Bear" over every wild bear — on every node from 1.21.2
	 * up, ever since the shim landed. That is the fault {@code /aac nameplates} was added to paper
	 * over in {@code 2.0.2}; with the clause restored the setting is back to what it says it is.
	 */
	@Override
	protected boolean shouldShowName(T entity, double distanceToCameraSq) {
		return super.shouldShowName(entity, distanceToCameraSq)
				&& (entity.shouldShowName()
						|| entity.hasCustomName() && entity == this.entityRenderDispatcher.crosshairPickEntity);
	}
}

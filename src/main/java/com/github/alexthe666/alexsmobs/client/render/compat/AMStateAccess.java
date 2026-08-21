package com.github.alexthe666.alexsmobs.client.render.compat;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;

/**
 * The entity behind a render state.
 *
 * <p>1.21.2 made {@code EntityRenderState} a pure data object: the renderer extracts what it needs
 * from the entity in one pass and may only read the state in the next. Forge's
 * {@code RenderLivingEvent} and {@code RenderNameTagEvent} followed it, so both now hand out a
 * state where they used to hand out the entity — but the hooks in this mod genuinely need the
 * entity (potion effects, equipment, vine-lasso data, the entity's own random source), and none of
 * that is on any vanilla state.
 *
 * <p>{@code mixin.renderstate.EntityRendererMixin} therefore stashes the entity and the frame's
 * partial tick on every state it extracts, and this interface reads them back. It is a duck typed
 * onto {@code EntityRenderState} itself, so it works for vanilla entities too — the rocky
 * chestplate and the clinging/ender-flu effects apply to players and vanilla mobs, not only to
 * this mod's own.
 *
 * <p>Valid only between extraction and rendering of the same frame, which is the whole window in
 * which anything here is asked for.
 */
public interface AMStateAccess {

	void alexsmobs$capture(Entity entity, float partialTick);

	Entity alexsmobs$entity();

	float alexsmobs$partialTick();

	static Entity entity(EntityRenderState state) {
		return ((AMStateAccess) state).alexsmobs$entity();
	}

	static float partialTick(EntityRenderState state) {
		return ((AMStateAccess) state).alexsmobs$partialTick();
	}
}

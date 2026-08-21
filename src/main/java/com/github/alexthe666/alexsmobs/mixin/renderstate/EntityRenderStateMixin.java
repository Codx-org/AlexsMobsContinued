package com.github.alexthe666.alexsmobs.mixin.renderstate;

import com.github.alexthe666.alexsmobs.client.render.compat.AMStateAccess;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Storage for {@link AMStateAccess} — see there for why this exists.
 *
 * <p>This package is compiled and added to the mixin config only on 1.21.2 and up, where
 * {@code EntityRenderState} exists; below that the renderers still receive the entity directly.
 */
@Mixin(EntityRenderState.class)
public abstract class EntityRenderStateMixin implements AMStateAccess {

    @Unique
    private Entity alexsmobs$entity;

    @Unique
    private float alexsmobs$partialTick;

    @Override
    public void alexsmobs$capture(Entity entity, float partialTick) {
        this.alexsmobs$entity = entity;
        this.alexsmobs$partialTick = partialTick;
    }

    @Override
    public Entity alexsmobs$entity() {
        return this.alexsmobs$entity;
    }

    @Override
    public float alexsmobs$partialTick() {
        return this.alexsmobs$partialTick;
    }
}

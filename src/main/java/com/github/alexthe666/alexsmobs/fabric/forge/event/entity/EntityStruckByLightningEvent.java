package com.github.alexthe666.alexsmobs.fabric.forge.event.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;

/**
 * Fabric stand-in for {@code net.minecraftforge.event.entity.EntityStruckByLightningEvent} —
 * cancellable, which is the whole point of the mod's use: a mob wearing the right charm converts
 * the strike instead of taking it.
 *
 * <p>Fabric has no lightning callback; the dispatcher mixes into {@code Entity#thunderHit}.
 */
public class EntityStruckByLightningEvent extends EntityEvent {

    private final LightningBolt lightning;

    public EntityStruckByLightningEvent(Entity entity, LightningBolt lightning) {
        super(entity);
        this.lightning = lightning;
    }

    public LightningBolt getLightning() {
        return lightning;
    }
}

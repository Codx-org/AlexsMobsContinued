package com.github.alexthe666.alexsmobs.fabric.forge.event.entity.living;

import net.minecraft.world.entity.LivingEntity;

/**
 * Fabric stand-in for {@code net.minecraftforge.event.entity.living.LivingChangeTargetEvent}.
 *
 * <p>Two deterrents cancel it: Bug Pheromones stop insects retargeting the player who applied them,
 * and the Unsettling Kimono makes mobs refuse to target its wearer. Both check
 * {@link #getNewTarget()} against the mob's {@code getLastHurtByMob()}, so a mob that was actually
 * struck still fights back — cancelling is a deterrent, not invulnerability.
 *
 * <p>Fabric has no targeting callback; the dispatcher mixes into {@code Mob#setTarget}.
 */
public class LivingChangeTargetEvent extends LivingEvent {

    private LivingEntity newTarget;

    public LivingChangeTargetEvent(LivingEntity entity, LivingEntity newTarget) {
        super(entity);
        this.newTarget = newTarget;
    }

    public LivingEntity getNewTarget() {
        return newTarget;
    }

    public void setNewTarget(LivingEntity newTarget) {
        this.newTarget = newTarget;
    }
}

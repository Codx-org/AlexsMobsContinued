package com.github.alexthe666.alexsmobs.fabric.forge.event.entity.living;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Fabric stand-in for {@code net.minecraftforge.event.entity.living.LivingAttackEvent} — the
 * <b>pre</b>-mitigation hook, fired before armour and effects are applied, and cancellable to
 * refuse the hit outright.
 *
 * <p>The mod cancels it for the Shield of the Deep (blocks the hit while raised) and a couple of
 * other "immune to this source" behaviours. Forge {@code >=1.21.6} and NeoForge {@code >=1.21}
 * replaced it with {@code LivingIncomingDamageEvent}, but both of those arms are loader-gated, so
 * Fabric takes this one on every node.
 *
 * <p>Distinct from {@link LivingDamageEvent}, which fires after mitigation with the final amount —
 * the mod listens to both and they are not interchangeable.
 */
public class LivingAttackEvent extends LivingEvent {

    private final DamageSource source;
    private final float amount;

    public LivingAttackEvent(LivingEntity entity, DamageSource source, float amount) {
        super(entity);
        this.source = source;
        this.amount = amount;
    }

    public DamageSource getSource() {
        return source;
    }

    public float getAmount() {
        return amount;
    }
}

package com.github.alexthe666.alexsmobs.fabric.forge.event.entity.living;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Fabric stand-in for {@code net.minecraftforge.event.entity.living.LivingDamageEvent} — the
 * <b>post</b>-mitigation hook, carrying the amount that is actually about to be dealt.
 *
 * <p>Three behaviours ride on it: Soulsteal heals the attacker for half the damage dealt, the emu
 * leggings roll a dodge chance against projectiles, and one player-side check cancels the hit
 * entirely. The first needs the mitigated amount, which is why the mod cannot simply fold this into
 * {@link LivingAttackEvent}.
 *
 * <p>{@link #setAmount} is present because Forge has it and the shape must match; this mod only
 * ever cancels, never rescales. The Fabric dispatcher reads both back.
 */
public class LivingDamageEvent extends LivingEvent {

    private final DamageSource source;
    private float amount;

    public LivingDamageEvent(LivingEntity entity, DamageSource source, float amount) {
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

    public void setAmount(float amount) {
        this.amount = amount;
    }
}

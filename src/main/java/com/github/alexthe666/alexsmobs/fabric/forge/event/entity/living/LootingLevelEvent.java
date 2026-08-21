package com.github.alexthe666.alexsmobs.fabric.forge.event.entity.living;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Fabric stand-in for {@code net.minecraftforge.event.entity.living.LootingLevelEvent} — the snow
 * leopard's "+2 looting on its own kills" bonus.
 *
 * <p>⚠️ <b>This class is only reachable on Fabric {@code <1.21}.</b> Its handler is gated
 * {@code forge || <1.21} because 1.21 turned looting into an enchantment <i>effect component</i>
 * and deleted the integer looting level the event existed to bump — that is a vanilla change, so it
 * binds Fabric exactly as it binds NeoForge. The bonus is dropped from 1.21 up on both, and the
 * stub stays compiled-but-unreachable there rather than being gated out, because a type that exists
 * on every node is one less thing for a future editor to trip over.
 */
public class LootingLevelEvent extends LivingEvent {

    private final DamageSource damageSource;
    private int lootingLevel;

    public LootingLevelEvent(LivingEntity entity, DamageSource damageSource, int lootingLevel) {
        super(entity);
        this.damageSource = damageSource;
        this.lootingLevel = lootingLevel;
    }

    public DamageSource getDamageSource() {
        return damageSource;
    }

    public int getLootingLevel() {
        return lootingLevel;
    }

    public void setLootingLevel(int lootingLevel) {
        this.lootingLevel = lootingLevel;
    }
}

package com.github.alexthe666.alexsmobs.fabric.forge.event.entity.living;

import java.util.Collection;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;

/**
 * Fabric stand-in for {@code net.minecraftforge.event.entity.living.LivingDropsEvent}.
 *
 * <p>The mod's one use adds the Vine Lasso back to the drops of a mob that was carrying one, so the
 * lasso is not destroyed with its holder. That means the {@link #getDrops()} collection must be
 * <b>live and mutable</b> — the handler calls {@code add} on it and expects the caller to honour
 * the result. The Fabric dispatcher therefore hands in the real drop list from its
 * {@code LivingEntity#dropAllDeathLoot} mixin, not a copy.
 */
public class LivingDropsEvent extends LivingEvent {

    private final DamageSource source;
    private final Collection<ItemEntity> drops;
    private final int lootingLevel;

    public LivingDropsEvent(LivingEntity entity, DamageSource source, Collection<ItemEntity> drops, int lootingLevel) {
        super(entity);
        this.source = source;
        this.drops = drops;
        this.lootingLevel = lootingLevel;
    }

    public DamageSource getSource() {
        return source;
    }

    public Collection<ItemEntity> getDrops() {
        return drops;
    }

    public int getLootingLevel() {
        return lootingLevel;
    }
}

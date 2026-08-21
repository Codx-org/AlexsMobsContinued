package com.github.alexthe666.alexsmobs.fabric.forge.event.entity.living;

import com.github.alexthe666.alexsmobs.fabric.forge.event.entity.EntityEvent;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Fabric stand-in for {@code net.minecraftforge.event.entity.living.MobSpawnEvent}, the parent of
 * the two spawn hooks this mod listens to.
 *
 * <p>{@link #getEntity()} narrows to {@link Mob} — the same covariant narrowing Forge does, and the
 * handlers rely on it (they pattern-match {@code Spider}, {@code Wolf}, {@code WanderingTrader} …
 * straight off the return value).
 *
 * <p>NeoForge {@code >=1.20.6} split both nested classes out into top-level events, so those arms
 * are loader-gated and Fabric always takes the Forge-shaped one here.
 */
public class MobSpawnEvent extends EntityEvent {

    private final ServerLevelAccessor level;

    public MobSpawnEvent(Mob entity, ServerLevelAccessor level) {
        super(entity);
        this.level = level;
    }

    @Override
    public Mob getEntity() {
        return (Mob) super.getEntity();
    }

    public ServerLevelAccessor getLevel() {
        return level;
    }

    /**
     * Fired when a mob is about to be removed for being far from any player. Result {@code DENY}
     * keeps it alive — used so a mob under Debilitating Sting {@code II} cannot escape by
     * despawning.
     */
    public static class AllowDespawn extends MobSpawnEvent {

        public AllowDespawn(Mob entity, ServerLevelAccessor level) {
            super(entity, level);
        }
    }

    /**
     * Fired after a mob is created but before it joins the level, the hook for "adjust a vanilla
     * mob as it spawns": the elephant-riding wandering trader, and the six config-gated extra goals
     * bolted onto spiders, wolves, polar bears, creepers, cats/foxes, rabbits and dolphins.
     *
     * <p>Because it runs pre-join, the Fabric dispatcher must fire it from the same point vanilla
     * calls {@code Mob#finalizeSpawn}, not from an entity-load callback — goals added after the mob
     * is ticking would be a different behaviour.
     */
    public static class FinalizeSpawn extends MobSpawnEvent {

        public FinalizeSpawn(Mob entity, ServerLevelAccessor level) {
            super(entity, level);
        }
    }
}

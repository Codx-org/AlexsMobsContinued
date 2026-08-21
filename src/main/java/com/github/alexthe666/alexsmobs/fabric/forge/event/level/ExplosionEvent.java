package com.github.alexthe666.alexsmobs.fabric.forge.event.level;

import com.github.alexthe666.alexsmobs.fabric.forge.eventbus.api.Event;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;

/**
 * Fabric stand-in for {@code net.minecraftforge.event.level.ExplosionEvent}.
 *
 * <p>Only {@link Detonate} is used, and only on {@code >=1.20.5}: that version deleted
 * {@code IForgeItem#canBeHurtBy}, which was how a dropped Transmutation Table survived a blast.
 * The replacement removes the item entity from the affected list instead. The gate carries no
 * loader term, so Fabric {@code >=1.20.5} needs this exactly as Forge does.
 *
 * <p>{@link Detonate#getAffectedEntities()} is live — the handler calls {@code removeIf} on it.
 */
public class ExplosionEvent extends Event {

    private final Level level;
    private final Explosion explosion;

    public ExplosionEvent(Level level, Explosion explosion) {
        this.level = level;
        this.explosion = explosion;
    }

    public Level getLevel() {
        return level;
    }

    public Explosion getExplosion() {
        return explosion;
    }

    /**
     * Fired after the blast has picked its blocks and entities but before any of it is applied —
     * the one point where the entity list can still be edited.
     */
    public static class Detonate extends ExplosionEvent {

        private final List<Entity> affectedEntities;

        public Detonate(Level level, Explosion explosion, List<Entity> affectedEntities) {
            super(level, explosion);
            this.affectedEntities = affectedEntities;
        }

        public List<Entity> getAffectedEntities() {
            return affectedEntities;
        }
    }
}

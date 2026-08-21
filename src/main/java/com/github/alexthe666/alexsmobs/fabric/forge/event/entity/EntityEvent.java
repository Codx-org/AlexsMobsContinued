package com.github.alexthe666.alexsmobs.fabric.forge.event.entity;

import com.github.alexthe666.alexsmobs.fabric.forge.eventbus.api.Event;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;

/**
 * Fabric stand-in for {@code net.minecraftforge.event.entity.EntityEvent} — the root of every
 * entity-scoped event the mod listens to, and the reason this hierarchy is a hierarchy at all.
 *
 * <p>{@code getEntity()} is called <b>109 times</b> across the two event files and is expected to
 * come back already narrowed: {@code EntityEvent} gives {@code Entity}, {@code LivingEvent}
 * gives {@code LivingEntity}, {@code PlayerEvent} gives {@code Player}. Forge does that with
 * covariant overrides, so the stubs do too — flattening it to one {@code Entity}-typed getter would
 * force a cast at every one of those call sites and put the two working loaders on a different
 * source text.
 */
public class EntityEvent extends Event {

    private final Entity entity;

    public EntityEvent(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }

    /**
     * Fired when an entity's bounding box is recalculated, so a listener can override the eye
     * height. The mod's one use is {@code EffectClinging}: a clinging player's eyes move to the
     * bottom of the hitbox so the upside-down view lines up.
     *
     * <p>Forge fires this from {@code Entity#refreshDimensions}; the Fabric dispatcher does the same
     * from a mixin there. {@code newSize} is carried but never written by this mod — only the eye
     * height is — so it is exposed read/write for faithfulness and left alone.
     */
    public static class Size extends EntityEvent {

        private final EntityDimensions oldSize;
        private final float oldEyeHeight;
        private EntityDimensions newSize;
        private float newEyeHeight;

        public Size(Entity entity, EntityDimensions oldSize, float oldEyeHeight) {
            super(entity);
            this.oldSize = oldSize;
            this.oldEyeHeight = oldEyeHeight;
            this.newSize = oldSize;
            this.newEyeHeight = oldEyeHeight;
        }

        public EntityDimensions getOldSize() {
            return oldSize;
        }

        public EntityDimensions getNewSize() {
            return newSize;
        }

        public void setNewSize(EntityDimensions newSize) {
            this.newSize = newSize;
        }

        public float getOldEyeHeight() {
            return oldEyeHeight;
        }

        public float getNewEyeHeight() {
            return newEyeHeight;
        }

        public void setNewEyeHeight(float newEyeHeight) {
            this.newEyeHeight = newEyeHeight;
        }
    }
}

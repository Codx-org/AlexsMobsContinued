package com.github.alexthe666.alexsmobs.fabric.forge.event.entity.living;

import com.github.alexthe666.alexsmobs.fabric.forge.event.entity.EntityEvent;
import net.minecraft.world.entity.LivingEntity;

/**
 * Fabric stand-in for {@code net.minecraftforge.event.entity.living.LivingEvent} — narrows
 * {@link #getEntity()} to {@link LivingEntity}, which most of the mod's handlers rely on.
 */
public class LivingEvent extends EntityEvent {

    public LivingEvent(LivingEntity entity) {
        super(entity);
    }

    @Override
    public LivingEntity getEntity() {
        return (LivingEntity) super.getEntity();
    }

    /**
     * Per-entity tick. This is the mod's single busiest hook — {@code onLivingUpdate} drives the
     * lasso, the rainbow effects, the clinging effect, the shoulder-mounted mobs and a dozen
     * armour behaviours, so on Fabric it is the one whose absence is most visible.
     *
     * <p>Forge fires it for every entity; the Fabric dispatcher mixes into {@code LivingEntity#tick}
     * rather than using {@code ServerTickEvents}, because this must also run client-side (several
     * of those behaviours are visual) and must see non-player living entities.
     */
    public static class LivingTickEvent extends LivingEvent {

        public LivingTickEvent(LivingEntity entity) {
            super(entity);
        }
    }
}

package com.github.alexthe666.alexsmobs.fabric.forge.event.entity.living;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Fabric stand-in for {@code net.minecraftforge.event.entity.living.LivingEntityUseItemEvent}.
 *
 * <p>Only the {@link Finish} phase is listened to — a chorus fruit has a one-in-three chance of
 * curing Ender Flu. Forge's {@code Start}/{@code Tick}/{@code Stop} phases have no call site here
 * and are not stubbed.
 */
public class LivingEntityUseItemEvent extends LivingEvent {

    private final ItemStack item;

    public LivingEntityUseItemEvent(LivingEntity entity, ItemStack item) {
        super(entity);
        this.item = item;
    }

    public ItemStack getItem() {
        return item;
    }

    /** Fired once the use duration has elapsed and the item's effect has been applied. */
    public static class Finish extends LivingEntityUseItemEvent {

        public Finish(LivingEntity entity, ItemStack item) {
            super(entity, item);
        }
    }
}

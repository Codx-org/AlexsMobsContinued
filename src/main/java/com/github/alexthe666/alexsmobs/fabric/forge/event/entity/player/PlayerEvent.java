package com.github.alexthe666.alexsmobs.fabric.forge.event.entity.player;

import com.github.alexthe666.alexsmobs.fabric.forge.event.entity.living.LivingEvent;
import net.minecraft.world.entity.player.Player;

/**
 * Fabric stand-in for {@code net.minecraftforge.event.entity.player.PlayerEvent} — narrows
 * {@link #getEntity()} once more, to {@link Player}.
 */
public class PlayerEvent extends LivingEvent {

    public PlayerEvent(Player player) {
        super(player);
    }

    @Override
    public Player getEntity() {
        return (Player) super.getEntity();
    }

    /**
     * "Can this player harvest this block?" — the mod answers yes for any block a
     * {@code ItemGhostlyPickaxe} is holding, so the pickaxe's phasing works on blocks its tier
     * would not normally drop.
     *
     * <p>Fabric has no equivalent callback, so the dispatcher mixes into
     * {@code Player#hasCorrectToolForDrops}.
     */
    public static class HarvestCheck extends PlayerEvent {

        private boolean canHarvest;

        public HarvestCheck(Player player, boolean canHarvest) {
            super(player);
            this.canHarvest = canHarvest;
        }

        public boolean canHarvest() {
            return canHarvest;
        }

        public void setCanHarvest(boolean canHarvest) {
            this.canHarvest = canHarvest;
        }
    }

    /**
     * First moment a player is fully in the world. The mod uses it to hand out the Animal
     * Dictionary on first join (plus the two easter-egg items for Alex's and Carro's UUIDs).
     *
     * <p>Fabric API covers this properly with {@code ServerPlayConnectionEvents.JOIN}, so this is
     * one of the few hooks that needs no mixin.
     */
    public static class PlayerLoggedInEvent extends PlayerEvent {

        public PlayerLoggedInEvent(Player player) {
            super(player);
        }
    }
}

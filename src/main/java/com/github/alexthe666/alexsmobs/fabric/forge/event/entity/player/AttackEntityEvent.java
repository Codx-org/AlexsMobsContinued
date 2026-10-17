package com.github.alexthe666.alexsmobs.fabric.forge.event.entity.player;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Fabric stand-in for {@code net.minecraftforge.event.entity.player.AttackEntityEvent} — fired when
 * a player left-clicks an entity, before the attack resolves.
 *
 * <p>Drives two wearables: the Moose Headgear's charge knockback and the Tiger's Blessing call
 * ("every tiger within 32 blocks now targets what you just hit"). Both need the attacker <i>and</i>
 * the victim, which is why {@link #getTarget()} exists separately from the inherited
 * {@code getEntity()}.
 */
public class AttackEntityEvent extends PlayerEvent {

    private final Entity target;

    public AttackEntityEvent(Player player, Entity target) {
        super(player);
        this.target = target;
    }

    public Entity getTarget() {
        return target;
    }
}

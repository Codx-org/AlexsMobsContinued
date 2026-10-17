package com.github.alexthe666.alexsmobs.fabric.forge.client.event;

import com.github.alexthe666.alexsmobs.fabric.forge.eventbus.api.Event;
import net.minecraft.world.entity.player.Player;

/**
 * Fabric stand-in for {@code net.minecraftforge.client.event.ComputeFovModifierEvent} — Fear and
 * Power Down pin the FOV multiplier to {@code 1.0}, so neither effect's speed change warps the view.
 *
 * <p>Client-only despite living in {@code ServerEvents}: it is registered on the same bus upstream,
 * and splitting it out would mean touching a handler that is otherwise loader-neutral. The Fabric
 * dispatcher must therefore only fire it client-side.
 *
 * <p>{@link #getPlayer()} rather than {@code getEntity()} — this one is not part of the
 * {@code EntityEvent} hierarchy in Forge either, and the handler calls {@code getPlayer()}.
 */
public class ComputeFovModifierEvent extends Event {

    private final Player player;
    private final float fovModifier;
    private float newFovModifier;

    public ComputeFovModifierEvent(Player player, float fovModifier, float newFovModifier) {
        this.player = player;
        this.fovModifier = fovModifier;
        this.newFovModifier = newFovModifier;
    }

    public Player getPlayer() {
        return player;
    }

    /** The multiplier vanilla computed, before any listener touched it. */
    public float getFovModifier() {
        return fovModifier;
    }

    public float getNewFovModifier() {
        return newFovModifier;
    }

    public void setNewFovModifier(float newFovModifier) {
        this.newFovModifier = newFovModifier;
    }
}

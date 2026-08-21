package com.github.alexthe666.alexsmobs.fabric.forge.client.event;

import com.github.alexthe666.alexsmobs.fabric.forge.eventbus.api.Event;
import net.minecraft.world.entity.Entity;

/**
 * Fabric stand-in for {@code net.minecraftforge.client.event.RenderNameTagEvent} — the one hook
 * {@code client/event/ClientEvents} uses to hide the player's own nameplate while their camera is
 * riding a bald eagle in singleplayer.
 *
 * <p>Only the entity is carried. The veto travels through {@link Event.Result#DENY} on the base
 * class, on <b>all seventeen</b> Fabric nodes: {@code ClientEvents}' {@code setResult} call is gated
 * {@code (forge || fabric) || <1.20.6}. That gate used to read {@code forge || <1.20.6}, which meant
 * a Fabric node at MC 1.20.6 or later emitted <b>neither</b> that arm nor the NeoForge
 * {@code TriState} one, leaving an empty guard body — so firing this event changed nothing. Wave
 * 3b-6 widened it; if the {@code fabric} disjunct is ever removed, this event goes silently inert
 * again rather than failing to compile.
 *
 * <p>{@code FabricNameTagMixin} is what fires it, injecting at the head of the nameplate method
 * itself rather than at Forge's call site — see that class for why, and for the one thing this
 * arrangement cannot do (force a nameplate to appear that vanilla declined to draw).
 */
public class RenderNameTagEvent extends Event {

    private final Entity entity;

    public RenderNameTagEvent(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }
}

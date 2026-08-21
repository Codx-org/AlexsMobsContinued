package com.github.alexthe666.alexsmobs.fabric.event;

/**
 * Fabric stand-in for the base class of the mod's <b>own</b> events — Citadel's four client hooks
 * ({@code EventGet*}, {@code EventPosePlayerHand}) and {@code AnimationEvent}.
 *
 * <p>Those are the only events this mod <i>publishes</i>; everything else it merely <i>listens</i>
 * to. On Forge {@code <1.21.6} they extend {@code net.minecraftforge.eventbus.api.Event}, on
 * NeoForge {@code net.neoforged.bus.api.Event}, and on EventBus 7 {@code MutableEvent} — three
 * spellings of "a cancellable payload posted on a bus". Fabric has no bus at all, so the payload
 * half is this class and the bus half is {@link AMEventBus}.
 *
 * <p>Cancellation is folded in here rather than split into a marker interface: EventBus 7 needs the
 * split because its bus type is chosen by it, but nothing on Fabric reads it except
 * {@code AMEventBus#post}, so a plain flag is faithful and much less to gate. An event that is not
 * cancellable on the other loaders simply never has {@link #setCanceled} called on it.
 */
public class AMEvent {

    private boolean canceled;

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }
}

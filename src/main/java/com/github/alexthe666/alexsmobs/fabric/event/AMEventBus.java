package com.github.alexthe666.alexsmobs.fabric.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * The Fabric half of the mod's own event dispatch — see {@link AMEvent} for why this exists at all.
 *
 * <p>Deliberately shaped like <b>EventBus 7</b>'s {@code EventBus}/{@code CancellableEventBus}
 * ({@code create(Class)}, {@code addListener(Consumer)}, {@code post} returning "was cancelled"),
 * because the Forge {@code >=1.21.6} arm of each event class already uses that shape — so the
 * Fabric arm is the same text with a different type name, and the two arms cannot drift apart
 * unnoticed. The {@code Class} argument is accepted and ignored for exactly that reason.
 *
 * <p>Listeners are held in a {@link CopyOnWriteArrayList}: these fire from the render thread
 * ({@code EventGetOutlineColor}, {@code EventGetStarBrightness}, {@code EventPosePlayerHand},
 * {@code EventGetFluidRenderType}) and the server thread ({@code AnimationEvent}) alike, and a
 * listener may legally register another during a post.
 *
 * <p><b>Nothing subscribes to these on Fabric yet</b> — {@code client/event/ClientEvents.java} is
 * excluded from the Fabric compile (see {@code ModPlatformPlugin.configureJava}), so the mod
 * currently posts into an empty bus. That is the same "registers everything, reacts to nothing"
 * state the rest of the Fabric port is in; the bus is here so restoring the handlers is a
 * subscription and not another platform decision.
 */
public final class AMEventBus<T> {

    private final List<Consumer<? super T>> listeners = new CopyOnWriteArrayList<>();

    private AMEventBus() {
    }

    public static <T> AMEventBus<T> create(Class<T> type) {
        return new AMEventBus<>();
    }

    public void addListener(Consumer<? super T> listener) {
        listeners.add(listener);
    }

    /** Dispatches to every listener and reports whether the event came back cancelled. */
    public boolean post(T event) {
        for (Consumer<? super T> listener : listeners) {
            listener.accept(event);
        }
        return event instanceof AMEvent cancellable && cancellable.isCanceled();
    }
}

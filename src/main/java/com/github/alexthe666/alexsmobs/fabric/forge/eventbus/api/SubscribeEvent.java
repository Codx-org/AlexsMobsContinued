package com.github.alexthe666.alexsmobs.fabric.forge.eventbus.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Fabric stand-in for {@code net.minecraftforge.eventbus.api.SubscribeEvent}.
 *
 * <p>Purely a marker here: on Forge and NeoForge the bus scans for it and derives the hook from the
 * parameter type, whereas {@code fabric/event/FabricServerEvents} names each handler explicitly.
 * The annotation exists so the 41 {@code @SubscribeEvent} lines in {@code ServerEvents} — and the
 * further ~20 in {@code ClientEvents} — compile untouched on this loader.
 *
 * <p>{@link RetentionPolicy#RUNTIME} rather than {@code SOURCE} on purpose: it costs nothing, and it
 * keeps the door open for a reflective dispatcher later without a recompile-semantics surprise.
 * {@link #priority()} exists because one handler passes it — see {@link EventPriority}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SubscribeEvent {

    EventPriority priority() default EventPriority.NORMAL;

    boolean receiveCanceled() default false;
}

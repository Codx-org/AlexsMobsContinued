package com.github.alexthe666.alexsmobs.fabric.forge.eventbus.api;

/**
 * Fabric stand-in for {@code net.minecraftforge.eventbus.api.EventPriority}.
 *
 * <p>Declared in Forge's order (highest first), which is the order a Forge bus dispatches in. Only
 * one handler in the mod names a priority at all — {@code ServerEvents#onTooltip} is
 * {@code @SubscribeEvent(priority = EventPriority.LOWEST)}, so its tooltip lines land after every
 * other mod's.
 *
 * <p>{@code FabricServerEvents} has a single subscriber per hook and therefore nothing to order, so
 * the value is accepted and ignored — the enum exists so the annotation argument compiles. If a
 * second subscriber to the same hook is ever added on this loader, sort by {@link #ordinal()}
 * ascending and the Forge semantics come back for free.
 */
public enum EventPriority {
    HIGHEST,
    HIGH,
    NORMAL,
    LOW,
    LOWEST,
}

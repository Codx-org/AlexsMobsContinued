package com.github.alexthe666.alexsmobs.fabric;

/**
 * Fabric stand-in for the mod event bus that {@code AlexsMobs}' constructor threads through
 * everything it wires up — Forge's {@code IEventBus} below 1.21.6, its {@code BusGroup} above,
 * NeoForge's {@code IEventBus} throughout.
 *
 * <p>Deliberately empty. Fabric has no mod bus: its registries are immediate and its callbacks are
 * registered against static {@code Event} objects, so there is nothing for a bus token to carry.
 * The token exists purely so the constructor's ~20 {@code X.DEF_REG.register(modBusEvent);} lines
 * stay byte-identical on all three loaders — {@link com.github.alexthe666.alexsmobs.fabric.registries.DeferredRegister#register(ModBus)}
 * accepts one and forwards to the immediate flush.
 *
 * <p>That is what makes {@code AlexsMobs}' constructor the single source of truth for <b>flush
 * order</b>, which on Fabric is load-bearing (see that {@code DeferredRegister}'s class javadoc).
 * Nothing has to duplicate the order in the Fabric entrypoint.
 */
public final class ModBus {
}

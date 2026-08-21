package com.github.alexthe666.alexsmobs.fabric.forge.event;

import net.minecraft.world.level.Level;

/**
 * Fabric stand-in for {@code net.minecraftforge.event.TickEvent}.
 *
 * <p>Only the {@code <1.21.9} Forge shape is reproduced — a single phase-agnostic
 * {@code LevelTickEvent} with a <b>public field</b> {@code level}, not a getter. Later Forge and
 * NeoForge both split it into {@code Pre}/{@code Post} classes, but those arms are gated
 * {@code forge &&}/{@code neoforge &&} in {@code ServerEvents}, so Fabric always falls through to
 * the plain one. The field spelling is deliberate: {@code tick.level} is what the else-arm reads.
 *
 * <p>The mod never filtered on phase, so the Fabric dispatcher fires this once per level per tick
 * from {@code ServerTickEvents.END_WORLD_TICK} — matching the "both halves subscribed" behaviour
 * the other two loaders settled on.
 *
 * <p>{@link ClientTickEvent} is the one place the mod <b>does</b> filter, so that one carries the
 * phase — see its own note.
 */
public class TickEvent {

    /**
     * The phase discriminator. Only {@link #START} is ever constructed on Fabric: the sole client
     * tick handler ({@code AMItemstackRenderer.incrementTick}) guards on it, and the guard is in
     * shared source, so firing a {@code Phase.END} event as well would run the handler zero times
     * rather than twice. {@code END} exists only because the {@code ==} comparison names the enum.
     */
    public enum Phase {
        START,
        END,
    }

    public static class LevelTickEvent {

        public final Level level;

        public LevelTickEvent(Level level) {
            this.level = level;
        }
    }

    /**
     * The client tick. Unlike {@link LevelTickEvent} this one keeps Forge's <b>public {@code phase}
     * field</b> because the else-arm reads {@code event.phase == TickEvent.Phase.START} directly —
     * Forge and NeoForge both dissolved the phase-tagged event into {@code Pre}/{@code Post}
     * classes later, but those arms are loader-gated, so Fabric always lands on the phase-tagged
     * shape no matter the MC version.
     */
    public static class ClientTickEvent {

        public final Phase phase;

        public ClientTickEvent(Phase phase) {
            this.phase = phase;
        }
    }
}

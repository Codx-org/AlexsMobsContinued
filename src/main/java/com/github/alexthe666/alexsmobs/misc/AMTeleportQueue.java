package com.github.alexthe666.alexsmobs.misc;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.apache.commons.lang3.tuple.Triple;

/**
 * Cross-dimension player teleports queued by {@code EntityVoidPortal} and drained on the next
 * server level tick — a player cannot be moved between dimensions from inside an entity tick.
 * <p>
 * This lives here rather than on {@code ServerEvents} only so that the producer side stays
 * compilable on Fabric, where the whole Forge-event class is excluded from the source set.
 * The consumer is still {@code ServerEvents}, so on Fabric the queue currently fills and is
 * never drained; wiring a Fabric level-tick callback is part of the deferred event work.
 */
public class AMTeleportQueue {

    public static final ObjectList<Triple<ServerPlayer, ServerLevel, BlockPos>> PLAYERS = new ObjectArrayList<>();

    private AMTeleportQueue() {
    }
}

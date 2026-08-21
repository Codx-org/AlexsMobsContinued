package com.github.alexthe666.alexsmobs.fabric.server;

import com.github.alexthe666.alexsmobs.fabric.AlexsMobsFabric;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;

/**
 * Fabric stand-in for {@code net.minecraftforge.server.ServerLifecycleHooks}, reached by the
 * Fabric-only {@code !fab-serverlifecycle} replacement rule.
 *
 * <p>Only {@code getCurrentServer()} is reproduced, because that is the only member this tree
 * calls (one site: {@code AlexsMobs#sendMSGToAll}). Fabric API has no equivalent static accessor,
 * so {@link AlexsMobsFabric} captures the server from {@code ServerLifecycleEvents} and this
 * forwards to it.
 *
 * <p>Like the loaders' version this returns {@code null} before the server starts and after it
 * stops — callers already null-check.
 */
public final class ServerLifecycleHooks {

    private ServerLifecycleHooks() {
    }

    @Nullable
    public static MinecraftServer getCurrentServer() {
        return AlexsMobsFabric.getServer();
    }
}

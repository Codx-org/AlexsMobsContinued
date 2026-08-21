package com.github.alexthe666.alexsmobs.fabric.forge.common;

import com.github.alexthe666.alexsmobs.fabric.event.AMEventBus;

/**
 * Fabric stand-in for {@code net.minecraftforge.common.MinecraftForge} — reduced to the one thing
 * the shared source does with it on a Fabric node: {@code MinecraftForge.EVENT_BUS.post(...)}.
 *
 * <p>There is exactly one such call in the whole Fabric compile —
 * {@code client/event/ClientEvents} reposts a {@code RenderLivingEvent.Post} by hand after the
 * rocky-chestplate roll cancels the {@code Pre}, so that the pose stack it pushed gets popped by
 * the normal Post path instead of being duplicated. Registration goes the other way, through
 * {@code fabric/client/FabricClientEvents}, so nothing here needs a subscribe side beyond what
 * {@link AMEventBus} already offers.
 *
 * <p>The bus is typed {@code <Object>} on purpose. Forge's is untyped and the source relies on
 * that; a listener therefore does its own {@code instanceof} narrowing, which is what the
 * dispatcher does. Reusing {@link AMEventBus} rather than inventing a second bus keeps the mod to
 * one dispatch implementation on Fabric.
 */
public final class MinecraftForge {

    public static final AMEventBus<Object> EVENT_BUS = AMEventBus.create(Object.class);

    private MinecraftForge() {
    }
}

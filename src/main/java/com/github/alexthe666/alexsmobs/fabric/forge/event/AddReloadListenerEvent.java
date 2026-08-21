package com.github.alexthe666.alexsmobs.fabric.forge.event;

import com.github.alexthe666.alexsmobs.fabric.forge.eventbus.api.Event;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.packs.resources.PreparableReloadListener;

/**
 * Fabric stand-in for {@code net.minecraftforge.event.AddReloadListenerEvent} — how the capsid
 * recipe manager gets attached to the datapack reload.
 *
 * <p>The listener the handler registers is a {@code SimpleJsonResourceReloadListener}, so on Fabric
 * the dispatcher can hand it straight to {@code ResourceManagerHelper.get(SERVER_DATA)
 * .registerReloadListener(...)}; the wrapper only needs to supply an id.
 *
 * <p>{@link #getRegistryAccess()} matters from {@code 1.21.2} up, where the recipe codec resolves
 * item and tag references against a {@code HolderLookup.Provider} instead of parsing raw JSON.
 * Passing the real registries is the difference between a tag ingredient resolving and silently
 * decoding as empty — the no-arg fallback in {@code CommonProxy} uses {@code RegistryAccess.EMPTY}
 * and is only correct below 1.21.2.
 */
public class AddReloadListenerEvent extends Event {

    private final RegistryAccess registryAccess;
    private final java.util.List<PreparableReloadListener> listeners;

    public AddReloadListenerEvent(RegistryAccess registryAccess, java.util.List<PreparableReloadListener> listeners) {
        this.registryAccess = registryAccess;
        this.listeners = listeners;
    }

    public void addListener(PreparableReloadListener listener) {
        listeners.add(listener);
    }

    public RegistryAccess getRegistryAccess() {
        return registryAccess;
    }
}

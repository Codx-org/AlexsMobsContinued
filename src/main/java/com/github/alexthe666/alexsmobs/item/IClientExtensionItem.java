package com.github.alexthe666.alexsmobs.item;

import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/**
 * Marker for this mod's items that supply a client render extension (a BEWLR / armor renderer).
 *
 * <p>Through 1.21.1 every such item overrode {@code IForgeItem}/{@code IItemExtension}'s
 * {@code initializeClient(Consumer<IClientItemExtensions>)} default. NeoForge 1.21.2 deleted that
 * hook in favour of the mod-bus {@code RegisterClientExtensionsEvent}, so the items now also
 * implement this interface: {@code ClientProxy.onRegisterClientExtensions} walks the item registry
 * and forwards each provider to the event. On Forge (and NeoForge &lt;1.21.2) the very same method
 * still satisfies the loader's own {@code initializeClient}, so nothing changes there.
 */
public interface IClientExtensionItem {
    void initializeClient(Consumer<IClientItemExtensions> consumer);
}

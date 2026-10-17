package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/**
 * Advancement-icon item drawn entirely by {@code AMItemstackRenderer} — the display subject comes
 * from the stack's NBT. Vendored from Citadel's {@code ItemCustomRender} (LGPL-3.0-only); Citadel
 * registered these as {@code citadel:fancy_item} / {@code citadel:effect_item}.
 * <p>
 * Hidden from the creative tab, as it was in Citadel.
 */
public class ItemCustomRender extends Item implements CustomTabBehavior, IClientExtensionItem {

    public ItemCustomRender(Properties props) {
        super(props);
    }

    @Override
    public void fillItemCategory(CreativeModeTab.Output contents) {
        // intentionally empty — these are advancement icons, not obtainable items
    }

    @Override
    public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer) {
        consumer.accept((IClientItemExtensions) AlexsMobs.PROXY.getISTERProperties());
    }
}

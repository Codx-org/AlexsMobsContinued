package com.github.alexthe666.alexsmobs.fabric.forge.event.entity.player;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Fabric stand-in for {@code net.minecraftforge.event.entity.player.ItemTooltipEvent} — one line,
 * "Insulated with fur", on armour a bison has lined.
 *
 * <p>Two things the dispatcher must honour: the {@link #getToolTip()} list is <b>live</b> (the
 * handler appends to it in place), and the tooltip is built on the client with <b>no player</b>
 * during some screens, so {@link #getEntity()} is nullable here in a way the rest of
 * {@link PlayerEvent} is not. The handler never touches it.
 */
public class ItemTooltipEvent extends PlayerEvent {

    private final ItemStack itemStack;
    private final List<Component> toolTip;

    public ItemTooltipEvent(Player player, ItemStack itemStack, List<Component> toolTip) {
        super(player);
        this.itemStack = itemStack;
        this.toolTip = toolTip;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public List<Component> getToolTip() {
        return toolTip;
    }
}

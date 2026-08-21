package com.github.alexthe666.alexsmobs.fabric.items;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The Fabric stand-in for {@code net.minecraftforge.items.ItemHandlerHelper}, reached through the
 * {@code !fab-itemhandlerhelper} replacement rule. Two of its methods are used in this tree: the
 * three-argument {@code insertItem}, and {@code giveItemToPlayer} for the Animal Dictionary (plus
 * the two joke items Alex and Carro get).
 */
public final class ItemHandlerHelper {

    private ItemHandlerHelper() {
    }

    /**
     * Matches Forge's contract: the input stack is never mutated, and the return value is what
     * could not be inserted.
     */
    public static ItemStack insertItem(IItemHandler handler, ItemStack stack, boolean simulate) {
        if (handler == null || stack.isEmpty()) {
            return stack;
        }
        return handler.insert(stack, simulate);
    }

    /**
     * "Put this in the player's inventory, or at their feet if it will not fit" — Forge's
     * two-argument overload, over vanilla {@code Player#add}/{@code #drop} rather than its
     * {@code PlayerMainInvWrapper}.
     *
     * <p>Two deliberate simplifications against Forge's version: it has no preferred-slot pass
     * (the two-arg overload does not use one either), and the dropped entity keeps its normal
     * pickup delay instead of being cleared and owner-tagged. Both are invisible for the only
     * thing that calls this — a first-login book that lands in an empty inventory.
     */
    public static void giveItemToPlayer(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}

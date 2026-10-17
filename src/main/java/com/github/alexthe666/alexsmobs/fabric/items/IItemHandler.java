package com.github.alexthe666.alexsmobs.fabric.items;

import net.minecraft.world.item.ItemStack;

/**
 * The Fabric stand-in for {@code net.minecraftforge.items.IItemHandler}, reached through the
 * {@code !fab-itemhandler} replacement rule so that the three consumers (the crow's item-frame
 * deposit, the crow's chest deposit and the capsid's output) keep their bodies unchanged.
 * <p>
 * This is deliberately NOT a slot-wise mirror of Forge's interface. Every use in this tree goes
 * through {@code ItemHandlerHelper#insertItem}, i.e. "insert this stack somewhere, tell me what is
 * left over" — so that is the whole contract, and the method is named {@code insert} rather than
 * {@code insertItem} so it cannot be mistaken for Forge's per-slot one.
 * <p>
 * Unlike the other vendored Forge types in this package, this one is NOT inert: {@code AMItemHandlers}
 * implements it over Fabric API's {@code Storage<ItemVariant>}, which covers vanilla containers
 * (through Fabric API's own {@code InventoryStorage} fallback) as well as modded inventories.
 */
@FunctionalInterface
public interface IItemHandler {

    /**
     * Inserts as much of {@code stack} as fits and returns the remainder, leaving {@code stack}
     * itself untouched. Returns the stack unchanged when nothing could be inserted, and
     * {@link ItemStack#EMPTY} when all of it was.
     */
    ItemStack insert(ItemStack stack, boolean simulate);
}

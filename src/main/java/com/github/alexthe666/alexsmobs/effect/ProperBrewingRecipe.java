package com.github.alexthe666.alexsmobs.effect;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.brewing.BrewingRecipe;

import javax.annotation.Nonnull;

/**
 * A brewing recipe whose input is matched against a whole stack rather than just its item.
 *
 * <p>Nearly every input here is a potion bottle, so the item on its own cannot tell the recipes
 * apart — the potion the bottle holds has to match too, which is what {@link #isInput} is for.
 *
 * <p>The input is kept as an {@link ItemStack} rather than an {@link Ingredient} because 1.21.2
 * made Ingredient item-only (it wraps a {@code HolderSet<Item>} now) and it can no longer carry
 * the potion contents. The ingredient handed to super is therefore just the item, and matching is
 * left entirely to {@code isInput} — which is what did the real work before this change anyway.
 */
public class ProperBrewingRecipe extends BrewingRecipe {

    private final ItemStack input;

    public ProperBrewingRecipe(ItemStack input, Ingredient ingredient, ItemStack output) {
        super(Ingredient.of(input.getItem()), ingredient, output);
        this.input = input;
    }


    @Override
    public boolean isInput(@Nonnull ItemStack stack) {
        if (stack == null) {
            return false;
        }
        return ItemStack.isSameItem(stack, input) && ItemStack.isSameItemSameTags(input, stack);
    }

}

package com.github.alexthe666.alexsmobs.fabric.common.brewing;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Fabric stand-in for {@code net.minecraftforge.common.brewing.BrewingRecipe}, reached by the
 * Fabric-only {@code !fab-brewingrecipe} replacement rule. Copied from Forge's class, minus the
 * {@code IBrewingRecipe} interface it implements — nothing in this mod names that interface, and on
 * Fabric there is no registry to hand an implementation of it to.
 *
 * <p>Both loaders let a mod push an arbitrary input-stack-to-output-stack brewing recipe into the
 * per-server {@code PotionBrewing.Builder}. <b>Fabric API has no equivalent.</b> Its
 * {@code FabricPotionBrewingBuilder} only exposes vanilla's own two shapes —
 * {@code registerPotionRecipe} (potion contents change, container item preserved) and
 * {@code registerItemRecipe} (container item changes, potion contents preserved) — and four of this
 * mod's seventeen recipes are neither: {@code lava_bottle -> lava vision potion},
 * {@code poison potion -> poison bottle}, and the two {@code * _bottle -> poison resistance potion}
 * mixes all change the container item <i>and</i> the potion contents in one step.
 *
 * <p>So the recipes are collected by {@link FabricBrewing} instead of being registered, and Fabric
 * mixins consult them from the same places Forge patches. See {@link FabricBrewing} for the wiring.
 */
public class BrewingRecipe {

    private final Ingredient input;
    private final Ingredient ingredient;
    private final ItemStack output;

    public BrewingRecipe(Ingredient input, Ingredient ingredient, ItemStack output) {
        this.input = input;
        this.ingredient = ingredient;
        this.output = output;
    }

    public boolean isInput(ItemStack stack) {
        return this.input.test(stack);
    }

    public boolean isIngredient(ItemStack ingredient) {
        return this.ingredient.test(ingredient);
    }

    public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
        return isInput(input) && isIngredient(ingredient) ? getOutput().copy() : ItemStack.EMPTY;
    }

    public Ingredient getInput() {
        return input;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public ItemStack getOutput() {
        return output;
    }
}

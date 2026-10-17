package com.github.alexthe666.alexsmobs.fabric.common.brewing;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Where {@code AMEffectRegistry.addBrewing} puts its recipes on Fabric, in place of the
 * {@code PotionBrewing.Builder} hook both other loaders provide. See {@link BrewingRecipe} for why
 * Fabric API's brewing API cannot express four of the seventeen.
 *
 * <p>Filled and drained by four Fabric mixins:
 *
 * <ul>
 *   <li>{@code FabricPotionBrewingBuilderMixin} calls {@link #reset()} and re-runs
 *       {@code AMEffectRegistry.registerBrewingRecipes} from {@code PotionBrewing.Builder.build()},
 *       so this list is rebuilt in step with the brewing table it belongs to. Below 1.20.5 there is
 *       no builder and {@code AMEffectRegistry.init()} fills the list once instead.</li>
 *   <li>{@code FabricPotionBrewingMixin} answers for these recipes in {@code isIngredient},
 *       {@code hasMix} and {@code mix} — vanilla's own results are kept whenever they are non-empty,
 *       so vanilla's mixes keep working.</li>
 *   <li>{@code FabricBrewingStandMenuMixin} and {@code FabricBrewingStandBlockEntityMixin} make the
 *       bottom slots accept the non-potion inputs, via {@link #isInput(ItemStack)}.</li>
 * </ul>
 *
 * <p>Recipes are matched in registration order, which is what Forge does too.
 */
public final class FabricBrewing {

    private static final List<BrewingRecipe> RECIPES = new ArrayList<>();

    private FabricBrewing() {
    }

    /** Drops every collected recipe. Call before re-running the registration for a new server. */
    public static void reset() {
        RECIPES.clear();
    }

    public static void register(BrewingRecipe recipe) {
        RECIPES.add(recipe);
    }

    public static List<BrewingRecipe> recipes() {
        return Collections.unmodifiableList(RECIPES);
    }

    /** True if {@code stack} is the bottom-slot input of any collected recipe. */
    public static boolean isInput(ItemStack stack) {
        for (BrewingRecipe recipe : RECIPES) {
            if (recipe.isInput(stack)) {
                return true;
            }
        }
        return false;
    }

    /** True if {@code stack} is the top-slot ingredient of any collected recipe. */
    public static boolean isIngredient(ItemStack stack) {
        for (BrewingRecipe recipe : RECIPES) {
            if (recipe.isIngredient(stack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The output of brewing {@code ingredient} into {@code input}, or {@link ItemStack#EMPTY} if no
     * collected recipe matches. Argument order matches Forge's {@code IBrewingRecipe#getOutput}.
     */
    public static ItemStack mix(ItemStack input, ItemStack ingredient) {
        for (BrewingRecipe recipe : RECIPES) {
            ItemStack output = recipe.getOutput(input, ingredient);
            if (!output.isEmpty()) {
                return output;
            }
        }
        return ItemStack.EMPTY;
    }
}

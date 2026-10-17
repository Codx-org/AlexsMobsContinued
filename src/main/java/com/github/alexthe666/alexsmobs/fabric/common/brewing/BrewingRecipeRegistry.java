package com.github.alexthe666.alexsmobs.fabric.common.brewing;

/**
 * Fabric stand-in for {@code net.minecraftforge.common.brewing.BrewingRecipeRegistry}, reached by
 * the same {@code !fab-brewingrecipe} replacement rule as {@link BrewingRecipe} — the rule rewrites
 * the package, and the {@code Registry} suffix rides along with it.
 *
 * <p>Only needed below {@code 1.20.5}. From there up, both Forge and NeoForge dropped the global
 * registry for a per-server {@code PotionBrewing.Builder} handed to a callback, and
 * {@code AMEffectRegistry} takes its {@code >=1.20.5} arm — which names no registry class at all.
 *
 * <p>{@link #addRecipe} just funnels into {@link FabricBrewing}, which is where the {@code >=1.20.5}
 * Fabric path ends up too. See {@link FabricBrewing} for the mixins that consult them.
 */
public final class BrewingRecipeRegistry {

    private BrewingRecipeRegistry() {
    }

    public static void addRecipe(BrewingRecipe recipe) {
        FabricBrewing.register(recipe);
    }
}

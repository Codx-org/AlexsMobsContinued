package com.github.alexthe666.alexsmobs.misc;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import java.util.function.Supplier;

public class AMRecipeRegistry {
    public static final DeferredRegister<RecipeSerializer<?>> DEF_REG = DeferredRegister.create(Registries.RECIPE_SERIALIZER, AlexsMobs.MODID);
    // 26.1: RecipeSerializer is a record of (MapCodec, StreamCodec) and the (category)->T factory
    // serializers are gone, so each recipe class owns its own singleton SERIALIZER.
    //? if >=26 {
    /*public static final Supplier<RecipeSerializer<?>> MIMICREAM_RECIPE = DEF_REG.register("mimicream_repair", () -> RecipeMimicreamRepair.SERIALIZER);
    public static final Supplier<RecipeSerializer<?>> BISON_UPGRADE = DEF_REG.register("bison_upgrade", () -> RecipeBisonUpgrade.SERIALIZER);
    *///?} else {
    public static final Supplier<RecipeSerializer<?>> MIMICREAM_RECIPE = DEF_REG.register("mimicream_repair", () -> new SimpleCraftingRecipeSerializer<>(RecipeMimicreamRepair::new));
    public static final Supplier<RecipeSerializer<?>> BISON_UPGRADE = DEF_REG.register("bison_upgrade", () -> new SimpleCraftingRecipeSerializer<>(RecipeBisonUpgrade::new));
    //?}

    public static void init(){
    }
}

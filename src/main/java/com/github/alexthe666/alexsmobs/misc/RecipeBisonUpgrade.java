package com.github.alexthe666.alexsmobs.misc;

import com.github.alexthe666.alexsmobs.misc.AMCompat;

import com.github.alexthe666.alexsmobs.block.AMBlockRegistry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class RecipeBisonUpgrade extends CustomRecipe {

    // 26.1 stripped CustomRecipe down to an interface-shaped base with NO constructor (the
    // crafting-book category is a method now) and turned RecipeSerializer into a record of two
    // codecs. So a special recipe is a singleton with its own MAP_CODEC/STREAM_CODEC/SERIALIZER,
    // exactly like vanilla's own RepairItemRecipe.
    //? if >=26 {
    /*public static final RecipeBisonUpgrade INSTANCE = new RecipeBisonUpgrade();
    public static final com.mojang.serialization.MapCodec<RecipeBisonUpgrade> MAP_CODEC = com.mojang.serialization.MapCodec.unit(INSTANCE);
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, RecipeBisonUpgrade> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<RecipeBisonUpgrade> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public RecipeBisonUpgrade() {
    }
    *///?}
    // 1.20.2 moved the recipe id out of Recipe and into RecipeHolder, so CustomRecipe
    // no longer takes a ResourceLocation.
    //? if >=1.20.2 && <26 {
    /*public RecipeBisonUpgrade(CraftingBookCategory category) {
        super(category);
    }
    *///?}
    //? if <1.20.2 {
    public RecipeBisonUpgrade(ResourceLocation idIn, CraftingBookCategory category) {
        super(idIn, category);
    }
    //?}


    // 1.21 hands crafting recipes a CraftingInput record instead of the live CraftingContainer;
    // it exposes the same grid, just under size()/getItem() rather than getContainerSize().
    //? if >=1.21 {
    /*private ItemStack createBoots(net.minecraft.world.item.crafting.CraftingInput container){
        final int size = container.size();
    *///?} else {
    private ItemStack createBoots(Container container){
        final int size = container.getContainerSize();
    //?}
        ItemStack boots = ItemStack.EMPTY;
        int fur = 0;
        for (int j = 0; j < size; ++j) {
            ItemStack itemstack1 = container.getItem(j);
            if (itemstack1.is(AMBlockRegistry.BISON_FUR_BLOCK.get().asItem())) {
                fur++;
            }
        }
        if(fur == 1){
            for (int j = 0; j < size; ++j) {
                ItemStack itemstack1 = container.getItem(j);
                boolean notFurred = !AMCompat.hasTag(itemstack1) || AMCompat.getTag(itemstack1) != null && !AMCompat.getBoolean(AMCompat.getTag(itemstack1), "BisonFur");
                if (!itemstack1.isEmpty() && notFurred && AMCompat.equipmentSlotFor(itemstack1) == EquipmentSlot.FEET) {
                    boots = itemstack1;
                }
            }
            if(!boots.isEmpty()){
                ItemStack stack = boots.copy();
                CompoundTag tag = AMCompat.getOrCreateTag(stack);
                tag.putBoolean("BisonFur", true);
                AMCompat.setTag(stack, tag);
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    //? if >=1.21
    //public boolean matches(net.minecraft.world.item.crafting.CraftingInput inv, Level worldIn) {
    //? if <1.21
    public boolean matches(CraftingContainer inv, Level worldIn) {
        return !createBoots(inv).isEmpty();
    }

    // 1.20.5 swapped assemble's RegistryAccess for a HolderLookup.Provider; 1.21 swapped the
    // container for a CraftingInput; 26.1 dropped the registry argument entirely.
    @Override
    //? if >=26
    //public ItemStack assemble(net.minecraft.world.item.crafting.CraftingInput container) {
    //? if >=1.21 && <26
    //public ItemStack assemble(net.minecraft.world.item.crafting.CraftingInput container, net.minecraft.core.HolderLookup.Provider provider) {
    //? if >=1.20.5 && <1.21
    //public ItemStack assemble(CraftingContainer container, net.minecraft.core.HolderLookup.Provider provider) {
    //? if <1.20.5
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        return createBoots(container);
    }

    // Recipe#canCraftInDimensions was removed in 1.21.2 (sizing is implicit now); keep the method
    // as dead code below that era so the shape is unchanged, just drop the no-longer-valid @Override.
    //? if <1.21.2
    @Override
    public boolean canCraftInDimensions(int x, int y) {
        return x * y >= 2;
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        //? if >=26
        /*return SERIALIZER;*/
        //? if <26
        return (RecipeSerializer<? extends CustomRecipe>) (RecipeSerializer<?>) AMRecipeRegistry.BISON_UPGRADE.get();
    }
}

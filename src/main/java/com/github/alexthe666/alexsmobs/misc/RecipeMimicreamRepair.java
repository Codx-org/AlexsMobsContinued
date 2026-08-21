package com.github.alexthe666.alexsmobs.misc;

import com.github.alexthe666.alexsmobs.misc.AMCompat;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.core.registries.BuiltInRegistries;

public class RecipeMimicreamRepair extends CustomRecipe {
    // 26.1 stripped CustomRecipe down to a constructor-less base and turned RecipeSerializer into
    // a record of two codecs — see RecipeBisonUpgrade for the same shape.
    //? if >=26 {
    /*public static final RecipeMimicreamRepair INSTANCE = new RecipeMimicreamRepair();
    public static final com.mojang.serialization.MapCodec<RecipeMimicreamRepair> MAP_CODEC = com.mojang.serialization.MapCodec.unit(INSTANCE);
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, RecipeMimicreamRepair> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<RecipeMimicreamRepair> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public RecipeMimicreamRepair() {
    }
    *///?}
    // 1.20.2 moved the recipe id out of Recipe and into RecipeHolder, so CustomRecipe
    // no longer takes a ResourceLocation.
    //? if >=1.20.2 && <26 {
    /*public RecipeMimicreamRepair(CraftingBookCategory category) {
        super(category);
    }
    *///?}
    //? if <1.20.2 {
    public RecipeMimicreamRepair(ResourceLocation idIn, CraftingBookCategory category) {
        super(idIn, category);
    }
    //?}

    /**
     * Used to check if a recipe matches current crafting inventory
     */
    // 1.21 hands crafting recipes a CraftingInput record instead of the live CraftingContainer;
    // it exposes the same grid, just under size()/getItem() rather than getContainerSize().
    //? if >=1.21 {
    /*public boolean matches(net.minecraft.world.item.crafting.CraftingInput inv, Level worldIn) {
        final int size = inv.size();
    *///?} else {
    public boolean matches(CraftingContainer inv, Level worldIn) {
        final int size = inv.getContainerSize();
    //?}
        if(!AMConfig.mimicreamRepair){
            return false;
        }
        ItemStack damageableStack = ItemStack.EMPTY;
        int mimicreamCount = 0;

        for (int j = 0; j < size; ++j) {
            ItemStack itemstack1 = inv.getItem(j);
            if (!itemstack1.isEmpty()) {
                if (itemstack1.isDamageableItem() && !isBlacklisted(itemstack1)) {
                    damageableStack = itemstack1;
                } else {
                    if (itemstack1.getItem() == AMItemRegistry.MIMICREAM.get()) {
                        mimicreamCount++;
                    }
                }
            }
        }

        return !damageableStack.isEmpty() && mimicreamCount >= 8;
    }

    public boolean isBlacklisted(ItemStack stack) {
        ResourceLocation name = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return name != null && AMConfig.mimicreamBlacklist.contains(name.toString());
    }

    /**
     * Returns an Item that is the result of this recipe
     */
    // 1.20.5 swapped assemble's RegistryAccess for a HolderLookup.Provider; 1.21 swapped the
    // container for a CraftingInput.
    //? if >=26 {
    /*public ItemStack assemble(net.minecraft.world.item.crafting.CraftingInput inv) {
        final int size = inv.size();
    *///?}
    //? if >=1.21 && <26 {
    /*public ItemStack assemble(net.minecraft.world.item.crafting.CraftingInput inv, net.minecraft.core.HolderLookup.Provider provider) {
        final int size = inv.size();
    *///?}
    //? if >=1.20.5 && <1.21 {
    /*public ItemStack assemble(CraftingContainer inv, net.minecraft.core.HolderLookup.Provider provider) {
        final int size = inv.getContainerSize();
    *///?}
    //? if <1.20.5 {
    public ItemStack assemble(CraftingContainer inv, RegistryAccess registryAccess) {
        final int size = inv.getContainerSize();
    //?}
        ItemStack damageableStack = ItemStack.EMPTY;
        int mimicreamCount = 0;

        for (int j = 0; j < size; ++j) {
            ItemStack itemstack1 = inv.getItem(j);
            if (!itemstack1.isEmpty()) {
                if (itemstack1.isDamageableItem() && !isBlacklisted(itemstack1)) {
                    damageableStack = itemstack1;
                } else {
                    if (itemstack1.getItem() == AMItemRegistry.MIMICREAM.get()) {
                        mimicreamCount++;
                    }
                }
            }
        }

        if (!damageableStack.isEmpty() && mimicreamCount >= 8) {
            ItemStack itemstack2 = damageableStack.copy();

            // The enchantment registry is datapack-loaded from 1.21, so it can't be queried from a
            // static holder any more — but Enchantments.MENDING is itself the ResourceKey, and its
            // location is exactly what the old lookup returned.
            //? if >=1.21 {
            /*ResourceLocation mendingName = Enchantments.MENDING.location();
            *///?} else {
            ResourceLocation mendingName = BuiltInRegistries.ENCHANTMENT.getKey(Enchantments.MENDING);
            //?}

            // 1.20.5 moved BOTH of the things this recipe edits off the item's NBT: enchantments
            // into DataComponents.ENCHANTMENTS, and everything else into DataComponents.CUSTOM_DATA
            // — which an ordinary tool simply does not carry. So `AMCompat.getTag(damageableStack)`
            // returned **null** and assemble() threw an NPE the instant the recipe matched: 8
            // mimicream around any damageable item is the whole duplication feature, so it has been
            // dead on all 44 nodes >=1.20.5 since `2.0.0`. See docs/notes/bug-reports.md #86.
            //
            // The copy keeps every other component for free — ItemStack#copy carries the whole patch
            // — so only the two deliberate removals below have to be redone component-side.
            //? if >=1.20.5 {
            /*CompoundTag customData = AMCompat.getTag(itemstack2);
            if (customData != null && itemstack2.is(AMItemRegistry.GHOSTLY_PICKAXE.get()) && customData.contains("Items")) {
                // getTag already hands back a copy (CustomData#copyTag), so this is safe to mutate.
                customData.remove("Items");
                AMCompat.setTag(itemstack2, customData);
            }
            // Mending must not survive onto the copy: it comes out fully damaged (below), and a
            // Mending copy would repair itself back to new for nothing. Matched on the registered
            // id rather than on the Enchantment/ResourceKey, because that is the one identity that
            // spells the same across the 1.21 datapack-enchantment split.
            // The lambda parameter is named `resourceKey` deliberately: 1.21.11 renamed
            // ResourceKey#location() to identifier(), and stonecutter.gradle.kts renames that call
            // per-site (TagKey#location() survives, so it cannot be blanket-renamed) —
            // "resourceKey.location()" is one of the four named forms.
            net.minecraft.world.item.enchantment.EnchantmentHelper.updateEnchantments(itemstack2, mutable ->
                    mutable.removeIf(holder -> holder.unwrapKey().map(resourceKey -> resourceKey.location().equals(mendingName)).orElse(false)));
            *///?} else {
            // UPSTREAM FIX, same fault one era earlier: this was `damageableStack.getTag().copy()`,
            // and a damageable item only grows a tag once it is damaged or enchanted — so on 1.20.1
            // and 1.20.4 the recipe NPE'd on a pristine tool and worked on a used one. Not
            // getOrCreateTag(): on these versions that attaches the empty tag to the GRID stack.
            CompoundTag existing = AMCompat.getTag(damageableStack);
            CompoundTag compoundnbt = existing == null ? new CompoundTag() : existing.copy();

            if(damageableStack.is(AMItemRegistry.GHOSTLY_PICKAXE.get()) && compoundnbt.contains("Items")){
                compoundnbt.remove("Items");
            }
            ListTag oldNBTList = AMCompat.getList(compoundnbt, "Enchantments", 10);
            ListTag newNBTList = new ListTag();
            for (int i = 0; i < oldNBTList.size(); ++i) {
                CompoundTag compoundnbt2 = AMCompat.getCompound(oldNBTList, i);
                ResourceLocation resourcelocation1 = ResourceLocation.tryParse(AMCompat.getString(compoundnbt2, "id"));
                if (resourcelocation1 == null || !resourcelocation1.equals(mendingName)) {
                    newNBTList.add(compoundnbt2);
                }
            }
            compoundnbt.put("Enchantments", newNBTList);
            AMCompat.setTag(itemstack2, compoundnbt);
            //?}

            itemstack2.setDamageValue(itemstack2.getMaxDamage());
            return itemstack2;
        } else {
            return ItemStack.EMPTY;
        }
    }

    //? if >=1.21 {
    /*public NonNullList<ItemStack> getRemainingItems(net.minecraft.world.item.crafting.CraftingInput inv) {
        NonNullList<ItemStack> nonnulllist = NonNullList.withSize(inv.size(), ItemStack.EMPTY);
    *///?} else {
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer inv) {
        NonNullList<ItemStack> nonnulllist = NonNullList.withSize(inv.getContainerSize(), ItemStack.EMPTY);
    //?}

        for (int i = 0; i < nonnulllist.size(); ++i) {
            ItemStack itemstack = inv.getItem(i);
            if (AMCompat.hasCraftingRemainder(itemstack)) {
                nonnulllist.set(i, AMCompat.craftingRemainder(itemstack));
                //? if >=1.20.5
                //} else if (itemstack.isDamageableItem()) {
                //? if <1.20.5
            } else if (itemstack.getItem().canBeDepleted()) {
                ItemStack itemstack1 = itemstack.copy();
                itemstack1.setCount(1);
                nonnulllist.set(i, itemstack1);
                break;
            }
        }

        return nonnulllist;
    }

    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        //? if >=26
        /*return SERIALIZER;*/
        //? if <26
        return (RecipeSerializer<? extends CustomRecipe>) (RecipeSerializer<?>) AMRecipeRegistry.MIMICREAM_RECIPE.get();
    }

    /**
     * Used to determine if this recipe can fit in a grid of the given width/height
     */
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }
}

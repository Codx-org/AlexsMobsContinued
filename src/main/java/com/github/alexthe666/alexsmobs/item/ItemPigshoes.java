package com.github.alexthe666.alexsmobs.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
// 1.20.5 replaced the EnchantmentCategory enum with a supported-items item tag.
//? if <1.20.5
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.Enchantments;

public class ItemPigshoes extends Item {

    public ItemPigshoes(Item.Properties props) {
        super(props);
    }

    public int getEnchantmentValue() {
        return 1;
    }

    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    // From 1.21 there is no per-item enchanting hook left to override: an enchantment lists what
    // it goes on, so the shoes join the vanilla tag instead (see
    // data/minecraft/tags/items/enchantable/foot_armor.json). Unbreaking/Mending and the curses
    // key off other tags the shoes are not in, so they stay excluded on their own.
    // The tag has been `minecraft:enchantable/foot_armor` since 1.20.5 — read out of the 1.20.6,
    // 1.21, 1.21.4 and 26.2 jars. It was authored here as `foot_armor_enchantable` (the FIELD
    // name, not the id) until 2.0.15, so on every node >=1.20.5 the shoes were in no tag at all
    // and took no enchantment.
    //? if >=1.20.5 && <1.21 {
    /*public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return enchantment.getSupportedItems() == net.minecraft.tags.ItemTags.FOOT_ARMOR_ENCHANTABLE && !enchantment.isCurse() && enchantment != Enchantments.UNBREAKING && enchantment != Enchantments.MENDING;
    }
    *///?}

    //? if <1.20.5 {
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return enchantment.category == EnchantmentCategory.ARMOR_FEET && !enchantment.isCurse() && enchantment != Enchantments.UNBREAKING && enchantment != Enchantments.MENDING;
    }
    //?}
}

package com.github.alexthe666.alexsmobs.enchantment;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
//? if <1.21 {
import net.minecraft.world.item.enchantment.Enchantment;
//?}
//? if <1.20.5 {
import net.minecraft.world.item.enchantment.EnchantmentCategory;
//?}

// Three flat variants rather than one class with nested conditionals: the class *header* itself
// changes on 1.21 (Enchantment became a final record loaded from a datapack, so there is nothing
// to extend and no behaviour to override — see data/alexsmobs/enchantment/*.json and the note in
// AMEnchantmentRegistry). The type is kept on 1.21 so nothing that names it has to be gated.
//? if >=1.21 {
/*public class StraddleEnchantment {
}
*///?}

//? if >=1.20.5 && <1.21 {
/*public class StraddleEnchantment extends Enchantment {

    protected StraddleEnchantment(EnchantmentDefinition definition) {
        super(definition);
    }

    public boolean isTradeable() {
        return super.isTradeable() && AMConfig.straddleboardEnchants;
    }

    public boolean isDiscoverable() {
        return super.isDiscoverable() && AMConfig.straddleboardEnchants;
    }
}
*///?}

// A fourth variant for Fabric below 1.20.5. Same class, minus the two Forge-only hooks at the
// bottom of the //? if <1.20.5 && !fabric arm — there is no super to call for either — plus the two
// overrides that stand in for the STRADDLEBOARD EnchantmentCategory Fabric cannot create. See
// AMEnchantmentRegistry for why isDiscoverable() is hard false here.
//? if fabric && <1.20.5 {
/*public class StraddleEnchantment extends Enchantment {

    protected StraddleEnchantment(Rarity r, EnchantmentCategory type, EquipmentSlot... types) {
        super(r, type, types);
    }

    public int getMinCost(int i) {
        return 6 + (i + 1) * 6;
    }

    public int getMaxCost(int i) {
        return super.getMinCost(i) + 10;
    }

    public int getMaxLevel() {
        return 1;
    }

    public boolean isTradeable() {
        return super.isTradeable() && AMConfig.straddleboardEnchants;
    }

    // Vanilla's default is this.category.canEnchant(item), and the category is the VANISHABLE
    // placeholder, so it has to be answered here instead. This is what the anvil consults, so an
    // enchanted book still applies to a board and to nothing else.
    public boolean canEnchant(ItemStack stack) {
        return stack.getItem() instanceof com.github.alexthe666.alexsmobs.item.ItemStraddleboard;
    }

    // The enchanting table reads the raw `category` field rather than canEnchant, so this is the
    // only lever that keeps a placeholder category from offering straddle enchants on every
    // vanishable item. The cost is that the table never offers them on Fabric below 1.20.5; the
    // real fix is a mixin on EnchantmentHelper#getAvailableEnchantmentResults routing that check
    // through canEnchant, which is exactly the line Forge patches.
    public boolean isDiscoverable() {
        return false;
    }
}
*///?}

//? if <1.20.5 && !fabric {
public class StraddleEnchantment extends Enchantment {

    protected StraddleEnchantment(Rarity r, EnchantmentCategory type, EquipmentSlot... types) {
        super(r, type, types);
    }

    // 1.20.5 froze the cost/level curve into the immutable EnchantmentDefinition; the
    // equivalent Enchantment.dynamicCost(..) values are passed in AMEnchantmentRegistry.
    public int getMinCost(int i) {
        return 6 + (i + 1) * 6;
    }

    public int getMaxCost(int i) {
        return super.getMinCost(i) + 10;
    }

    public int getMaxLevel() {
        return 1;
    }

    public boolean isTradeable() {
        return super.isTradeable() && AMConfig.straddleboardEnchants;
    }

    public boolean isDiscoverable() {
        return super.isDiscoverable() && AMConfig.straddleboardEnchants;
    }

    // Both hooks were loader extensions that 1.20.5 dropped; from then on the
    // supported-items tag plus isDiscoverable() cover the same ground.
    public boolean isAllowedOnBooks() {
        return super.isAllowedOnBooks() && AMConfig.straddleboardEnchants;
    }

    public boolean canApplyAtEnchantingTable(ItemStack stack) {
        return super.canApplyAtEnchantingTable(stack) && AMConfig.straddleboardEnchants;
    }
}
//?}

package com.github.alexthe666.alexsmobs.enchantment;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.item.ItemStraddleboard;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
//? if <1.20.5 {
import net.minecraft.world.item.enchantment.EnchantmentCategory;
//?}
//? if >=1.20.5
/*import com.github.alexthe666.alexsmobs.misc.AMTagRegistry;*/
//? if <1.21
import net.minecraftforge.registries.DeferredRegister;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import com.github.alexthe666.alexsmobs.misc.AMCompat;

public class AMEnchantmentRegistry {

    //? if <1.21 {
    public static final DeferredRegister<Enchantment> DEF_REG = DeferredRegister.create(Registries.ENCHANTMENT, AlexsMobs.MODID);
    //?}

    // 1.21 made enchantments datapack content: Enchantment became a final record and its registry
    // is reloadable, so there is nothing to register from code. The four straddleboard enchantments
    // are declared in data/alexsmobs/enchantment/*.json from that version on, and everything this
    // mod does with them ("what level is this on that stack") needs only the ResourceKey. Keeping
    // them behind a Supplier means every existing `AMEnchantmentRegistry.STRADDLE_X.get()` call
    // site stays textually identical across all versions.
    //
    // What does NOT survive: AMConfig.straddleboardEnchants used to gate isTradeable/
    // isDiscoverable at runtime. A datapack enchantment has no such hooks, so on 1.21+ the four
    // are always obtainable.
    //? if >=1.21 {
    /*public static final Supplier<net.minecraft.resources.ResourceKey<Enchantment>> STRADDLE_JUMP = key("straddle_jump");
    public static final Supplier<net.minecraft.resources.ResourceKey<Enchantment>> STRADDLE_LAVAWAX = key("lavawax");
    public static final Supplier<net.minecraft.resources.ResourceKey<Enchantment>> STRADDLE_SERPENTFRIEND = key("serpentfriend");
    public static final Supplier<net.minecraft.resources.ResourceKey<Enchantment>> STRADDLE_BOARDRETURN = key("board_return");

    private static Supplier<net.minecraft.resources.ResourceKey<Enchantment>> key(String name) {
        net.minecraft.resources.ResourceKey<Enchantment> key = net.minecraft.resources.ResourceKey.create(
                Registries.ENCHANTMENT, com.github.alexthe666.alexsmobs.misc.AMCompat.rl(AlexsMobs.MODID, name));
        return () -> key;
    }
    *///?}

    //? if >=1.20.5 && <1.21 {
    /*// EnchantmentCategory is gone from 1.20.5; "what can this go on" is an item tag, and the
    // old Rarity/getMinCost/getMaxCost/getMaxLevel overrides became EnchantmentDefinition data.
    // Rarity → weight/anvil cost mapping is vanilla's: COMMON 10/1, UNCOMMON 5/2, RARE 2/4.
    public static final Supplier<Enchantment> STRADDLE_JUMP = DEF_REG.register("straddle_jump", () -> new StraddleJumpEnchantment(
            Enchantment.definition(AMTagRegistry.STRADDLEBOARD_ENCHANTABLE, 10, 3, Enchantment.dynamicCost(4, 5), Enchantment.dynamicCost(28, 6), 1, EquipmentSlot.MAINHAND)));
    public static final Supplier<Enchantment> STRADDLE_LAVAWAX = DEF_REG.register("lavawax", () -> new StraddleEnchantment(
            Enchantment.definition(AMTagRegistry.STRADDLEBOARD_ENCHANTABLE, 5, 1, Enchantment.dynamicCost(18, 6), Enchantment.dynamicCost(21, 10), 2, EquipmentSlot.MAINHAND)));
    public static final Supplier<Enchantment> STRADDLE_SERPENTFRIEND = DEF_REG.register("serpentfriend", () -> new StraddleEnchantment(
            Enchantment.definition(AMTagRegistry.STRADDLEBOARD_ENCHANTABLE, 2, 1, Enchantment.dynamicCost(18, 6), Enchantment.dynamicCost(21, 10), 4, EquipmentSlot.MAINHAND)));
    public static final Supplier<Enchantment> STRADDLE_BOARDRETURN = DEF_REG.register("board_return", () -> new StraddleEnchantment(
            Enchantment.definition(AMTagRegistry.STRADDLEBOARD_ENCHANTABLE, 5, 1, Enchantment.dynamicCost(18, 6), Enchantment.dynamicCost(21, 10), 2, EquipmentSlot.MAINHAND)));
    *///?}

    // ⚠️ Behaviour loss, Fabric below 1.20.5 only. EnchantmentCategory is a plain enum until 1.20.5
    // replaces it with the supported_items item tag; Forge patches in an extensible-enum create(...)
    // and Fabric has no equivalent, so there is no way to declare "straddleboards only" as a
    // category here. VANISHABLE is a placeholder that is never consulted: the enchanting table only
    // offers an enchantment whose isDiscoverable() is true, and the Fabric variant of
    // StraddleEnchantment returns false there precisely so the placeholder cannot leak these four
    // onto swords and pickaxes. They stay obtainable through villager book trades (isTradeable is
    // unchanged) and the anvil, which asks canEnchant(ItemStack) — overridden to test the board.
    //? if fabric && <1.20.5 {
    /*public static final EnchantmentCategory STRADDLEBOARD = EnchantmentCategory.VANISHABLE;

    public static final Supplier<Enchantment> STRADDLE_JUMP = DEF_REG.register("straddle_jump", () -> new StraddleJumpEnchantment(Enchantment.Rarity.COMMON, STRADDLEBOARD, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> STRADDLE_LAVAWAX = DEF_REG.register("lavawax", () -> new StraddleEnchantment(Enchantment.Rarity.UNCOMMON, STRADDLEBOARD, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> STRADDLE_SERPENTFRIEND = DEF_REG.register("serpentfriend", () -> new StraddleEnchantment(Enchantment.Rarity.RARE, STRADDLEBOARD, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> STRADDLE_BOARDRETURN = DEF_REG.register("board_return", () -> new StraddleEnchantment(Enchantment.Rarity.UNCOMMON, STRADDLEBOARD, EquipmentSlot.MAINHAND));
    *///?}

    //? if <1.20.5 && !fabric {
    public static final EnchantmentCategory STRADDLEBOARD = EnchantmentCategory.create("straddleboard", (item -> item instanceof ItemStraddleboard));

    public static final Supplier<Enchantment> STRADDLE_JUMP = DEF_REG.register("straddle_jump", () -> new StraddleJumpEnchantment(Enchantment.Rarity.COMMON, STRADDLEBOARD, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> STRADDLE_LAVAWAX = DEF_REG.register("lavawax", () -> new StraddleEnchantment(Enchantment.Rarity.UNCOMMON, STRADDLEBOARD, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> STRADDLE_SERPENTFRIEND = DEF_REG.register("serpentfriend", () -> new StraddleEnchantment(Enchantment.Rarity.RARE, STRADDLEBOARD, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> STRADDLE_BOARDRETURN = DEF_REG.register("board_return", () -> new StraddleEnchantment(Enchantment.Rarity.UNCOMMON, STRADDLEBOARD, EquipmentSlot.MAINHAND));
    //?}
}

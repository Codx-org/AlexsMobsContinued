package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.misc.AMCompat;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.block.AMBlockRegistry;
import com.github.alexthe666.alexsmobs.effect.AMEffectRegistry;
import com.github.alexthe666.alexsmobs.entity.*;
import com.github.alexthe666.alexsmobs.misc.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.citadel.server.block.LecternBooks;
import net.minecraft.core.BlockPos;
//? if >=1.20.2
//import net.minecraft.core.dispenser.BlockSource;
//? if <1.20.2
import net.minecraft.core.BlockSource;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.AbstractProjectileDispenseBehavior;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import com.github.alexthe666.alexsmobs.misc.RegistrationContext;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.registries.DeferredRegister;
import java.util.function.Supplier;

public class AMItemRegistry {
    public static final AMArmorMaterial ROADRUNNER_ARMOR_MATERIAL = new AMArmorMaterial("roadrunner", 18, new int[]{3, 3, 3, 3}, 20, SoundEvents.ARMOR_EQUIP_TURTLE, 0);
    public static final AMArmorMaterial CROCODILE_ARMOR_MATERIAL = new AMArmorMaterial("crocodile", 22, new int[]{2, 5, 7, 3}, 25, SoundEvents.ARMOR_EQUIP_TURTLE, 1);
    public static final AMArmorMaterial CENTIPEDE_ARMOR_MATERIAL = new AMArmorMaterial("centipede", 20, new int[]{6, 6, 6, 6}, 22, SoundEvents.ARMOR_EQUIP_TURTLE, 0.5F);
    public static final AMArmorMaterial MOOSE_ARMOR_MATERIAL = new AMArmorMaterial("moose", 19, new int[]{3, 3, 3, 3}, 21, SoundEvents.ARMOR_EQUIP_TURTLE, 0.5F);
    public static final AMArmorMaterial RACCOON_ARMOR_MATERIAL = new AMArmorMaterial("raccoon", 17, new int[]{3, 3, 3, 3}, 21, SoundEvents.ARMOR_EQUIP_LEATHER, 2.5F);
    public static final AMArmorMaterial SOMBRERO_ARMOR_MATERIAL = new AMArmorMaterial("sombrero", 14, new int[]{2, 2, 2, 2}, 30, SoundEvents.ARMOR_EQUIP_LEATHER, 0.5F);
    public static final AMArmorMaterial SPIKED_TURTLE_SHELL_ARMOR_MATERIAL = new AMArmorMaterial("spiked_turtle_shell", 35, new int[]{3, 3, 3, 3}, 30, SoundEvents.ARMOR_EQUIP_TURTLE, 1F, 0.2F);
    public static final AMArmorMaterial FEDORA_ARMOR_MATERIAL = new AMArmorMaterial("fedora", 10, new int[]{2, 2, 2, 2}, 30, SoundEvents.ARMOR_EQUIP_LEATHER, 0.5F);
    public static final AMArmorMaterial EMU_ARMOR_MATERIAL = new AMArmorMaterial("emu", 9, new int[]{4, 4, 4, 4}, 20, SoundEvents.ARMOR_EQUIP_LEATHER, 0.5F);
    public static final AMArmorMaterial TARANTULA_HAWK_ELYTRA_MATERIAL = new AMArmorMaterial("tarantula_hawk_elytra", 9, new int[]{3, 3, 3, 3}, 5, SoundEvents.ARMOR_EQUIP_LEATHER, 0);
    public static final AMArmorMaterial FROSTSTALKER_ARMOR_MATERIAL = new AMArmorMaterial("froststalker", 9, new int[]{3, 3, 3, 3}, 15, SoundEvents.ARMOR_EQUIP_LEATHER, 0.5F);
    public static final AMArmorMaterial ROCKY_ARMOR_MATERIAL = new AMArmorMaterial("rocky_roller", 20, new int[]{2, 5, 7, 3}, 10, SoundEvents.ARMOR_EQUIP_TURTLE, 0.5F);
    public static final AMArmorMaterial FLYING_FISH_MATERIAL = new AMArmorMaterial("flying_fish", 9, new int[]{1, 1, 1, 1}, 8, SoundEvents.ARMOR_EQUIP_LEATHER, 0F);
    public static final AMArmorMaterial NOVELTY_HAT_MATERIAL = new AMArmorMaterial("novelty_hat", 10, new int[]{2, 2, 2, 2}, 30, SoundEvents.ARMOR_EQUIP_LEATHER, 0F);
    public static final AMArmorMaterial KIMONO_MATERIAL = new AMArmorMaterial("kimono", 8, new int[]{3, 3, 3, 3}, 15, SoundEvents.ARMOR_EQUIP_LEATHER, 0F);

    public static final DeferredRegister<Item> DEF_REG = DeferredRegister.create(Registries.ITEM, AlexsMobs.MODID);

    // Registers an item and, on >=1.21.2, publishes its registry key via RegistrationContext so the
    // Item.Properties mixin can stamp it with setId(...) during construction (mandatory since 1.21.2).
    public static <I extends Item> Supplier<I> regItem(String name, Supplier<I> sup) {
        //? if >=1.21.2 {
        /*return DEF_REG.register(name, () -> {
            RegistrationContext.CURRENT_ID.set(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(AlexsMobs.MODID, name)));
            try { return sup.get(); } finally { RegistrationContext.CURRENT_ID.remove(); }
        });
        *///?} else {
        return DEF_REG.register(name, sup);
        //?}
    }

    static{
        initSpawnEggs();
    }

    public static final Supplier<Item> TAB_ICON = regItem("tab_icon", () -> new ItemTabIcon(new Item.Properties()));
    // Advancement icons that used to live in Citadel (citadel:fancy_item / citadel:effect_item).
    public static final Supplier<Item> FANCY_ITEM = regItem("fancy_item", () -> new ItemCustomRender(new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> EFFECT_ITEM = regItem("effect_item", () -> new ItemCustomRender(new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> ANIMAL_DICTIONARY = regItem("animal_dictionary", () -> new ItemAnimalDictionary(new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> BEAR_FUR = regItem("bear_fur", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> BEAR_DUST = regItem("bear_dust", () -> new ItemBearDust(new Item.Properties().rarity(Rarity.EPIC)));
    public static final Supplier<Item> ROADRUNNER_FEATHER = regItem("roadrunner_feather", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> ROADDRUNNER_BOOTS = regItem("roadrunner_boots", () -> new ItemModArmor(ROADRUNNER_ARMOR_MATERIAL, ArmorItem.Type.BOOTS));
    public static final Supplier<Item> LAVA_BOTTLE = regItem("lava_bottle", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> BONE_SERPENT_TOOTH = regItem("bone_serpent_tooth", () -> new Item(new Item.Properties().fireResistant()));
    public static final Supplier<Item> GAZELLE_HORN = regItem("gazelle_horn", () -> new Item(new Item.Properties().fireResistant()));
    public static final Supplier<Item> CROCODILE_SCUTE = regItem("crocodile_scute", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> CROCODILE_CHESTPLATE = regItem("crocodile_chestplate", () -> new ItemModArmor(CROCODILE_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE));
    public static final Supplier<Item> MAGGOT = regItem("maggot", () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(1).saturationMod(0.2F).build())));
    public static final Supplier<Item> BANANA = regItem("banana", () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(4).saturationMod(0.3F).build())));
    public static final Supplier<Item> ANCIENT_DART = regItem("ancient_dart", () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final Supplier<Item> HALO = regItem("halo", () -> new ItemInventoryOnly(new Item.Properties()));
    public static final Supplier<Item> BLOOD_SAC = regItem("blood_sac", () -> new Item(new Item.Properties()));

    public static final Supplier<Item> MOSQUITO_PROBOSCIS = regItem("mosquito_proboscis", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> BLOOD_SPRAYER = regItem("blood_sprayer", () -> new ItemBloodSprayer(new Item.Properties().durability(100)));
    public static final Supplier<Item> RATTLESNAKE_RATTLE = regItem("rattlesnake_rattle", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> CHORUS_ON_A_STICK = regItem("chorus_on_a_stick", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> SHARK_TOOTH = regItem("shark_tooth", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> SHARK_TOOTH_ARROW = regItem("shark_tooth_arrow", () -> new ItemModArrow(new Item.Properties()));
    public static final Supplier<Item> LOBSTER_TAIL = regItem("lobster_tail", () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationMod(0.4F).meat().build())));
    public static final Supplier<Item> COOKED_LOBSTER_TAIL = regItem("cooked_lobster_tail", () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(6).saturationMod(0.65F).meat().build())));
    public static final Supplier<Item> LOBSTER_BUCKET = regItem("lobster_bucket", () -> new ItemModFishBucket(AMEntityRegistry.LOBSTER, Fluids.WATER, new Item.Properties()));
    public static final Supplier<Item> KOMODO_SPIT = regItem("komodo_spit", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> KOMODO_SPIT_BOTTLE = regItem("komodo_spit_bottle", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> POISON_BOTTLE = regItem("poison_bottle", () -> new Item(new Item.Properties()));
    // 1.21 deleted BowlFoodItem — "leaves a bowl behind" is a FoodProperties field now.
    //? if >=1.21.2 {
    /*public static final Supplier<Item> SOPA_DE_MACACO = regItem("sopa_de_macaco", () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(5).saturationModifier(0.4F).build()).usingConvertsTo(Items.BOWL).stacksTo(1)));
    *///?} elif >=1.21 {
    /*public static final Supplier<Item> SOPA_DE_MACACO = regItem("sopa_de_macaco", () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(5).saturationMod(0.4F).usingConvertsTo(Items.BOWL).build()).stacksTo(1)));
    *///?} else {
    public static final Supplier<Item> SOPA_DE_MACACO = regItem("sopa_de_macaco", () -> new BowlFoodItem(new Item.Properties().food(new FoodProperties.Builder().nutrition(5).saturationMod(0.4F).meat().build()).stacksTo(1)));
    //?}
    public static final Supplier<Item> CENTIPEDE_LEG = regItem("centipede_leg", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> CENTIPEDE_LEGGINGS = regItem("centipede_leggings", () -> new ItemModArmor(CENTIPEDE_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS));
    public static final Supplier<Item> MOSQUITO_LARVA = regItem("mosquito_larva", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> MOOSE_ANTLER = regItem("moose_antler", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> MOOSE_HEADGEAR = regItem("moose_headgear", () -> new ItemModArmor(MOOSE_ARMOR_MATERIAL, ArmorItem.Type.HELMET));
    public static final Supplier<Item> MOOSE_RIBS = regItem("moose_ribs", () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(3).saturationMod(0.6F).meat().build())));
    public static final Supplier<Item> COOKED_MOOSE_RIBS = regItem("cooked_moose_ribs", () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(7).saturationMod(0.85F).meat().build())));
    public static final Supplier<Item> MIMICREAM = regItem("mimicream", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> RACCOON_TAIL = regItem("raccoon_tail", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> FRONTIER_CAP = regItem("frontier_cap", () -> new ItemModArmor(RACCOON_ARMOR_MATERIAL, ArmorItem.Type.HELMET));
    //? if >=1.21.2 {
    /*public static final Supplier<Item> BLOBFISH = regItem("blobfish", () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.4F).build(), net.minecraft.world.item.component.Consumable.builder().onConsume(new net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.POISON, 120, 0), 1F)).build())));
    *///?} else {
    public static final Supplier<Item> BLOBFISH = regItem("blobfish", () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(3).saturationMod(0.4F).meat().effect(new MobEffectInstance(MobEffects.POISON, 120, 0), 1F).build())));
    //?}
    public static final Supplier<Item> BLOBFISH_BUCKET = regItem("blobfish_bucket", () -> new ItemModFishBucket(AMEntityRegistry.BLOBFISH, Fluids.WATER, new Item.Properties()));
    public static final Supplier<Item> FISH_OIL = regItem("fish_oil", () -> new ItemFishOil(new Item.Properties().craftRemainder(Items.GLASS_BOTTLE).food(new FoodProperties.Builder().nutrition(0).saturationMod(0.2F).build())));
    public static final Supplier<Item> MARACA = regItem("maraca", () -> new ItemMaraca(new Item.Properties()));
    public static final Supplier<Item> SOMBRERO = regItem("sombrero", () -> new ItemModArmor(SOMBRERO_ARMOR_MATERIAL, ArmorItem.Type.HELMET));
    public static final Supplier<Item> COCKROACH_WING_FRAGMENT = regItem("cockroach_wing_fragment", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> COCKROACH_WING = regItem("cockroach_wing", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> COCKROACH_OOTHECA = regItem("cockroach_ootheca", () -> new ItemAnimalEgg(new Item.Properties()));
    public static final Supplier<Item> ACACIA_BLOSSOM = regItem("acacia_blossom", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> SOUL_HEART = regItem("soul_heart", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> SPIKED_SCUTE = regItem("spiked_scute", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> SPIKED_TURTLE_SHELL = regItem("spiked_turtle_shell", () -> new ItemModArmor(SPIKED_TURTLE_SHELL_ARMOR_MATERIAL, ArmorItem.Type.HELMET));
    public static final Supplier<Item> SHRIMP_FRIED_RICE = regItem("shrimp_fried_rice", () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(12).saturationMod(1F).build())));
    public static final Supplier<Item> GUSTER_EYE = regItem("guster_eye", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> POCKET_SAND = regItem("pocket_sand", () -> new ItemPocketSand(new Item.Properties().durability(220)));
    public static final Supplier<Item> WARPED_MUSCLE = regItem("warped_muscle", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> HEMOLYMPH_SAC = regItem("hemolymph_sac", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> HEMOLYMPH_BLASTER = regItem("hemolymph_blaster", () -> new ItemHemolymphBlaster(new Item.Properties().durability(150)));
    public static final Supplier<Item> WARPED_MIXTURE = regItem("warped_mixture", () -> new Item(new Item.Properties().rarity(Rarity.RARE).stacksTo(1).craftRemainder(Items.GLASS_BOTTLE)));
    public static final Supplier<Item> STRADDLITE = regItem("straddlite", () -> new Item(new Item.Properties().fireResistant()));
    public static final Supplier<Item> STRADPOLE_BUCKET = regItem("stradpole_bucket", () -> new ItemModFishBucket(AMEntityRegistry.STRADPOLE, Fluids.LAVA, new Item.Properties()));
    public static final Supplier<Item> STRADDLEBOARD = regItem("straddleboard", () -> new ItemStraddleboard(new Item.Properties().fireResistant().durability(220)));
    public static final Supplier<Item> EMU_EGG = regItem("emu_egg", () -> new ItemAnimalEgg(new Item.Properties().stacksTo(8)));
    public static final Supplier<Item> BOILED_EMU_EGG = regItem("boiled_emu_egg", () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(4).saturationMod(1F).meat().build())));
    public static final Supplier<Item> EMU_FEATHER = regItem("emu_feather", () -> new Item(new Item.Properties().fireResistant()));
    public static final Supplier<Item> EMU_LEGGINGS = regItem("emu_leggings", () -> new ItemModArmor(EMU_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS));
    public static final Supplier<Item> PLATYPUS_BUCKET = regItem("platypus_bucket", () -> new ItemModFishBucket(AMEntityRegistry.PLATYPUS, Fluids.WATER, new Item.Properties()));
    public static final Supplier<Item> FEDORA = regItem("fedora", () -> new ItemModArmor(FEDORA_ARMOR_MATERIAL, ArmorItem.Type.HELMET));
    public static final Supplier<Item> DROPBEAR_CLAW = regItem("dropbear_claw", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> KANGAROO_MEAT = regItem("kangaroo_meat", () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(4).saturationMod(0.6F).meat().build())));
    public static final Supplier<Item> COOKED_KANGAROO_MEAT = regItem("cooked_kangaroo_meat", () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(8).saturationMod(0.85F).meat().build())));
    public static final Supplier<Item> KANGAROO_HIDE = regItem("kangaroo_hide", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> KANGAROO_BURGER = regItem("kangaroo_burger", () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(12).saturationMod(1F).meat().build())));
    public static final Supplier<Item> AMBERGRIS = regItem("ambergris", () -> new ItemFuel(new Item.Properties(), 12800));
    public static final Supplier<Item> CACHALOT_WHALE_TOOTH = regItem("cachalot_whale_tooth", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> ECHOLOCATOR = regItem("echolocator", () -> new ItemEcholocator(new Item.Properties().durability(100), ItemEcholocator.EchoType.ECHOLOCATION));
    public static final Supplier<Item> ENDOLOCATOR = regItem("endolocator", () -> new ItemEcholocator(new Item.Properties().durability(25), ItemEcholocator.EchoType.ENDER));
    public static final Supplier<Item> GONGYLIDIA = regItem("gongylidia", () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(3).saturationMod(1.2F).build())));
    public static final Supplier<Item> LEAFCUTTER_ANT_PUPA = regItem("leafcutter_ant_pupa", () -> new ItemLeafcutterPupa(new Item.Properties()));
    public static final Supplier<Item> ENDERIOPHAGE_ROCKET = regItem("enderiophage_rocket", () -> new ItemEnderiophageRocket(new Item.Properties()));
    public static final Supplier<Item> FALCONRY_GLOVE_INVENTORY = regItem("falconry_glove_inventory", () -> new ItemInventoryOnly(new Item.Properties()));
    public static final Supplier<Item> FALCONRY_GLOVE_HAND = regItem("falconry_glove_hand", () -> new ItemInventoryOnly(new Item.Properties()));
    public static final Supplier<Item> FALCONRY_GLOVE = regItem("falconry_glove", () -> new ItemFalconryGlove(new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> FALCONRY_HOOD = regItem("falconry_hood", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> TARANTULA_HAWK_WING_FRAGMENT = regItem("tarantula_hawk_wing_fragment", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> TARANTULA_HAWK_WING = regItem("tarantula_hawk_wing", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> TARANTULA_HAWK_ELYTRA = regItem("tarantula_hawk_elytra", () -> new ItemTarantulaHawkElytra(AMCompat.glider(new Item.Properties().durability(800).rarity(Rarity.UNCOMMON)), TARANTULA_HAWK_ELYTRA_MATERIAL));
    public static final Supplier<Item> MYSTERIOUS_WORM = regItem("mysterious_worm", () -> new ItemMysteriousWorm(new Item.Properties().rarity(Rarity.RARE)));
    public static final Supplier<Item> VOID_WORM_MANDIBLE = regItem("void_worm_mandible", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> VOID_WORM_EYE = regItem("void_worm_eye", () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final Supplier<Item> DIMENSIONAL_CARVER = regItem("dimensional_carver", () -> new ItemDimensionalCarver(new Item.Properties().durability(20).rarity(Rarity.EPIC)));
    public static final Supplier<Item> SHATTERED_DIMENSIONAL_CARVER = regItem("shattered_dimensional_carver", () -> new ItemShatteredDimensionalCarver(new Item.Properties().durability(4).rarity(Rarity.RARE)));
    public static final Supplier<Item> SERRATED_SHARK_TOOTH = regItem("serrated_shark_tooth", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> FRILLED_SHARK_BUCKET = regItem("frilled_shark_bucket", () -> new ItemModFishBucket(AMEntityRegistry.FRILLED_SHARK, Fluids.WATER, new Item.Properties()));
    public static final Supplier<Item> SHIELD_OF_THE_DEEP = regItem("shield_of_the_deep", () -> new ItemShieldOfTheDeep(AMCompat.repairableWith(new Item.Properties().durability(400).rarity(Rarity.UNCOMMON), "shield_of_the_deep")));
    public static final Supplier<Item> MIMIC_OCTOPUS_BUCKET = regItem("mimic_octopus_bucket", () -> new ItemModFishBucket(AMEntityRegistry.MIMIC_OCTOPUS, Fluids.WATER, new Item.Properties()));
    public static final Supplier<Item> FROSTSTALKER_HORN = regItem("froststalker_horn", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> FROSTSTALKER_HELMET = regItem("froststalker_helmet", () -> new ItemModArmor(FROSTSTALKER_ARMOR_MATERIAL, ArmorItem.Type.HELMET));
    public static final Supplier<Item> PIGSHOES = regItem("pigshoes", () -> new ItemPigshoes(new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> STRADDLE_HELMET = regItem("straddle_helmet", () -> new Item(new Item.Properties().fireResistant()));
    public static final Supplier<Item> STRADDLE_SADDLE = regItem("straddle_saddle", () -> new Item(new Item.Properties().fireResistant()));
    //? if >=1.21.2 {
    /*public static final Supplier<Item> COSMIC_COD = regItem("cosmic_cod", () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.3F).build(), net.minecraft.world.item.component.Consumable.builder().onConsume(new net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect(new MobEffectInstance(AMCompat.effect(AMEffectRegistry.ENDER_FLU.get()), 12000), 0.15F)).build())));
    *///?} else {
    public static final Supplier<Item> COSMIC_COD = regItem("cosmic_cod", () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(6).saturationMod(0.3F).effect(new MobEffectInstance(AMCompat.effect(AMEffectRegistry.ENDER_FLU.get()), 12000), 0.15F).build())));
    //?}
    public static final Supplier<Item> SHED_SNAKE_SKIN = regItem("shed_snake_skin", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> VINE_LASSO_INVENTORY = regItem("vine_lasso_inventory", () -> new ItemInventoryOnly(new Item.Properties()));
    public static final Supplier<Item> VINE_LASSO_HAND = regItem("vine_lasso_hand", () -> new ItemInventoryOnly(new Item.Properties()));
    public static final Supplier<Item> VINE_LASSO = regItem("vine_lasso", () -> new ItemVineLasso(new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> ROCKY_SHELL = regItem("rocky_shell", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> ROCKY_CHESTPLATE = regItem("rocky_chestplate", () -> new ItemModArmor(ROCKY_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE));
    public static final Supplier<Item> POTTED_FLUTTER = regItem("potted_flutter", () -> new ItemFlutterPot(new Item.Properties()));
    public static final Supplier<Item> TERRAPIN_BUCKET = regItem("terrapin_bucket", () -> new ItemModFishBucket(AMEntityRegistry.TERRAPIN, Fluids.WATER, new Item.Properties()));
    public static final Supplier<Item> COMB_JELLY_BUCKET = regItem("comb_jelly_bucket", () -> new ItemModFishBucket(AMEntityRegistry.COMB_JELLY, Fluids.WATER, new Item.Properties()));
    public static final Supplier<Item> RAINBOW_JELLY = regItem("rainbow_jelly", () -> new ItemRainbowJelly(new Item.Properties().food(new FoodProperties.Builder().nutrition(1).saturationMod(0.2F).build())));
    public static final Supplier<Item> COSMIC_COD_BUCKET = regItem("cosmic_cod_bucket", () -> new ItemCosmicCodBucket(new Item.Properties()));
    public static final Supplier<Item> MUNGAL_SPORES = regItem("mungal_spores", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> BISON_FUR = regItem("bison_fur", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> LOST_TENTACLE = regItem("lost_tentacle", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> SQUID_GRAPPLE = regItem("squid_grapple", () -> new ItemSquidGrapple(AMCompat.repairableWith(new Item.Properties().durability(450), "squid_grapple")));
    public static final Supplier<Item> DEVILS_HOLE_PUPFISH_BUCKET = regItem("devils_hole_pupfish_bucket", () -> new ItemModFishBucket(AMEntityRegistry.DEVILS_HOLE_PUPFISH, Fluids.WATER, new Item.Properties()));
    public static final Supplier<Item> PUPFISH_LOCATOR = regItem("pupfish_locator", () -> new ItemEcholocator(new Item.Properties().durability(200), ItemEcholocator.EchoType.PUPFISH));
    public static final Supplier<Item> SMALL_CATFISH_BUCKET = regItem("small_catfish_bucket", () -> new ItemModFishBucket(AMEntityRegistry.CATFISH, Fluids.WATER, new Item.Properties()));
    public static final Supplier<Item> MEDIUM_CATFISH_BUCKET = regItem("medium_catfish_bucket", () -> new ItemCatfishBucket(Fluids.WATER, new Item.Properties()));
    public static final Supplier<Item> LARGE_CATFISH_BUCKET = regItem("large_catfish_bucket", () -> new ItemCatfishBucket(Fluids.WATER, new Item.Properties()));
    public static final Supplier<Item> RAW_CATFISH = regItem("raw_catfish", () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationMod(0.3F).meat().build())));
    public static final Supplier<Item> COOKED_CATFISH = regItem("cooked_catfish", () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(5).saturationMod(0.5F).meat().build())));
    public static final Supplier<Item> FLYING_FISH = regItem("flying_fish", () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(3).saturationMod(0.4F).meat().build())));
    public static final Supplier<Item> FLYING_FISH_BOOTS = regItem("flying_fish_boots", () -> new ItemModArmor(FLYING_FISH_MATERIAL, ArmorItem.Type.BOOTS));
    public static final Supplier<Item> FLYING_FISH_BUCKET = regItem("flying_fish_bucket", () -> new ItemModFishBucket(AMEntityRegistry.FLYING_FISH, Fluids.WATER, new Item.Properties()));
    public static final Supplier<Item> FISH_BONES = regItem("fish_bones", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> SKELEWAG_SWORD_INVENTORY = regItem("skelewag_sword_inventory", () -> new ItemInventoryOnly(new Item.Properties()));
    public static final Supplier<Item> SKELEWAG_SWORD_HAND = regItem("skelewag_sword_hand", () -> new ItemInventoryOnly(new Item.Properties()));
    public static final Supplier<Item> SKELEWAG_SWORD = regItem("skelewag_sword", () -> new ItemSkelewagSword(new Item.Properties().stacksTo(1).durability(430)));
    public static final Supplier<Item> NOVELTY_HAT = regItem("novelty_hat", () -> new ItemModArmor(NOVELTY_HAT_MATERIAL, ArmorItem.Type.HELMET));
    public static final Supplier<Item> MUDSKIPPER_BUCKET = regItem("mudskipper_bucket", () -> new ItemModFishBucket(AMEntityRegistry.MUDSKIPPER, Fluids.WATER, new Item.Properties()));
    public static final Supplier<Item> FARSEER_ARM = regItem("farseer_arm", () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final Supplier<Item> SKREECHER_SOUL = regItem("skreecher_soul", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> GHOSTLY_PICKAXE = regItem("ghostly_pickaxe", () -> new ItemGhostlyPickaxe(new Item.Properties()));
    public static final Supplier<Item> ELASTIC_TENDON = regItem("elastic_tendon", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> TENDON_WHIP = regItem("tendon_whip", () -> new ItemTendonWhip(new Item.Properties()));
    public static final Supplier<Item> UNSETTLING_KIMONO = regItem("unsettling_kimono", () -> new ItemModArmor(KIMONO_MATERIAL, ArmorItem.Type.CHESTPLATE));
    public static final Supplier<Item> STINK_BOTTLE = regItem("stink_bottle", () -> new ItemStinkBottle(AMBlockRegistry.SKUNK_SPRAY, new Item.Properties().stacksTo(16)));

    public static final Supplier<Item> STINK_RAY_HAND = regItem("stink_ray_hand", () -> new ItemInventoryOnly(new Item.Properties()));

    public static final Supplier<Item> STINK_RAY_INVENTORY = regItem("stink_ray_inventory", () -> new ItemInventoryOnly(new Item.Properties()));

    public static final Supplier<Item> STINK_RAY_EMPTY_HAND = regItem("stink_ray_empty_hand", () -> new ItemInventoryOnly(new Item.Properties()));

    public static final Supplier<Item> STINK_RAY_EMPTY_INVENTORY = regItem("stink_ray_empty_inventory", () -> new ItemInventoryOnly(new Item.Properties()));

    public static final Supplier<Item> STINK_RAY = regItem("stink_ray", () -> new ItemStinkRay(new Item.Properties().durability(5)));
    public static final Supplier<Item> BANANA_SLUG_SLIME = regItem("banana_slug_slime", () -> new Item(new Item.Properties()));
    // 1.20.5 dropped the Supplier wrapper from FoodProperties.Builder#effect; 1.21 dropped BowlFoodItem.
    //? if >=1.21.2
    //public static final Supplier<Item> MOSQUITO_REPELLENT_STEW = regItem("mosquito_repellent_stew", () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(4).alwaysEdible().saturationModifier(0.3F).build(), net.minecraft.world.item.component.Consumable.builder().onConsume(new net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect(new MobEffectInstance(AMCompat.effect(AMEffectRegistry.MOSQUITO_REPELLENT.get()), 24000), 1.0F)).build()).usingConvertsTo(Items.BOWL).stacksTo(1)));
    //? if >=1.21 && <1.21.2
    //public static final Supplier<Item> MOSQUITO_REPELLENT_STEW = regItem("mosquito_repellent_stew", () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(4).alwaysEat().saturationMod(0.3F).usingConvertsTo(Items.BOWL).effect(new MobEffectInstance(AMCompat.effect(AMEffectRegistry.MOSQUITO_REPELLENT.get()), 24000), 1.0F).build()).stacksTo(1)));
    //? if >=1.20.5 && <1.21
    //public static final Supplier<Item> MOSQUITO_REPELLENT_STEW = regItem("mosquito_repellent_stew", () -> new BowlFoodItem(new Item.Properties().food(new FoodProperties.Builder().nutrition(4).alwaysEat().saturationMod(0.3F).effect(new MobEffectInstance(AMCompat.effect(AMEffectRegistry.MOSQUITO_REPELLENT.get()), 24000), 1.0F).build()).stacksTo(1)));
    // The Supplier overload of FoodProperties.Builder#effect is a Forge patch, added so a mod effect
    // registered later than the item can still be named here; vanilla below 1.20.5 only takes the
    // instance. Fabric therefore builds the MobEffectInstance eagerly — safe because AMEffectRegistry
    // flushes before AMItemRegistry in the AlexsMobs constructor, the same ordering the music discs
    // rely on for their SoundEvents.
    //? if fabric && <1.20.5
    //public static final Supplier<Item> MOSQUITO_REPELLENT_STEW = regItem("mosquito_repellent_stew", () -> new BowlFoodItem(new Item.Properties().food(new FoodProperties.Builder().nutrition(4).alwaysEat().saturationMod(0.3F).effect(new MobEffectInstance(AMCompat.effect(AMEffectRegistry.MOSQUITO_REPELLENT.get()), 24000), 1.0F).build()).stacksTo(1)));
    //? if <1.20.5 && !fabric
    public static final Supplier<Item> MOSQUITO_REPELLENT_STEW = regItem("mosquito_repellent_stew", () -> new BowlFoodItem(new Item.Properties().food(new FoodProperties.Builder().nutrition(4).alwaysEat().saturationMod(0.3F).effect(() -> new MobEffectInstance(AMCompat.effect(AMEffectRegistry.MOSQUITO_REPELLENT.get()), 24000), 1.0F).build()).stacksTo(1)));
    public static final Supplier<Item> TRIOPS_BUCKET = regItem("triops_bucket", () -> new ItemModFishBucket(AMEntityRegistry.TRIOPS, Fluids.WATER, new Item.Properties()));

    // 1.21 deleted RecordItem: a disc is a plain Item carrying a jukebox_playable component that
    // points at a datapack JukeboxSong (see data/alexsmobs/jukebox_song/), which now owns the
    // sound event, the "LudoCrypt - …" label, the track length and the comparator output.
    //? if >=1.21 {
    /*public static final Supplier<Item> MUSIC_DISC_THIME = regItem("music_disc_thime", () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(jukeboxSong("music_disc_thime"))));
    public static final Supplier<Item> MUSIC_DISC_DAZE = regItem("music_disc_daze", () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(jukeboxSong("music_disc_daze"))));

    private static net.minecraft.resources.ResourceKey<JukeboxSong> jukeboxSong(String name) {
        return net.minecraft.resources.ResourceKey.create(Registries.JUKEBOX_SONG, AMCompat.rl(AlexsMobs.MODID, name));
    }
    *///?} elif fabric {
    /*// Vanilla's RecordItem takes the SoundEvent itself (and its constructor is protected — widened in
    // the access widener). Forge patches in a Supplier overload so the sound need not exist yet; here
    // the supplier is resolved eagerly instead, which is safe because AlexsMobs' constructor flushes
    // the sound registry before the item registry.
    public static final Supplier<Item> MUSIC_DISC_THIME = regItem("music_disc_thime", () -> new RecordItem(14, AMSoundRegistry.MUSIC_DISC_THIME.get(), new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 314 * 20));
    public static final Supplier<Item> MUSIC_DISC_DAZE = regItem("music_disc_daze", () -> new RecordItem(14, AMSoundRegistry.MUSIC_DISC_DAZE.get(), new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 191 * 20));
    *///?} else {
    public static final Supplier<Item> MUSIC_DISC_THIME = regItem("music_disc_thime", () -> new RecordItem(14, AMSoundRegistry.MUSIC_DISC_THIME, new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 314 * 20));
    public static final Supplier<Item> MUSIC_DISC_DAZE = regItem("music_disc_daze", () -> new RecordItem(14, AMSoundRegistry.MUSIC_DISC_DAZE, new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 191 * 20));
    //?}

    public static void initSpawnEggs() {
        regItem("spawn_egg_grizzly_bear", () -> AMCompat.spawnEgg(AMEntityRegistry.GRIZZLY_BEAR, 0X693A2C, 0X976144, new Item.Properties()));
        regItem("spawn_egg_roadrunner", () -> AMCompat.spawnEgg(AMEntityRegistry.ROADRUNNER, 0X3A2E26, 0XFBE9CE, new Item.Properties()));
        regItem("spawn_egg_bone_serpent", () -> AMCompat.spawnEgg(AMEntityRegistry.BONE_SERPENT, 0XE5D9C4, 0XFF6038, new Item.Properties()));
        regItem("spawn_egg_gazelle", () -> AMCompat.spawnEgg(AMEntityRegistry.GAZELLE, 0XDDA675,0X2C2925, new Item.Properties()));
        regItem("spawn_egg_crocodile", () -> AMCompat.spawnEgg(AMEntityRegistry.CROCODILE, 0X738940,0XA6A15E, new Item.Properties()));
        regItem("spawn_egg_fly", () -> AMCompat.spawnEgg(AMEntityRegistry.FLY, 0X464241,0X892E2E, new Item.Properties()));
        regItem("spawn_egg_hummingbird", () -> AMCompat.spawnEgg(AMEntityRegistry.HUMMINGBIRD, 0X325E7F,0X44A75F, new Item.Properties()));
        regItem("spawn_egg_orca", () -> AMCompat.spawnEgg(AMEntityRegistry.ORCA, 0X2C2C2C,0XD6D8E4, new Item.Properties()));
        regItem("spawn_egg_sunbird", () -> AMCompat.spawnEgg(AMEntityRegistry.SUNBIRD, 0XF6694F,0XFFDDA0, new Item.Properties()));
        regItem("spawn_egg_gorilla", () -> AMCompat.spawnEgg(AMEntityRegistry.GORILLA, 0X595B5D,0X1C1C21, new Item.Properties()));
        regItem("spawn_egg_crimson_mosquito", () -> AMCompat.spawnEgg(AMEntityRegistry.CRIMSON_MOSQUITO, 0X53403F,0XC11A1A, new Item.Properties()));
        regItem("spawn_egg_rattlesnake", () -> AMCompat.spawnEgg(AMEntityRegistry.RATTLESNAKE, 0XCEB994,0X937A5B, new Item.Properties()));
        regItem("spawn_egg_endergrade", () -> AMCompat.spawnEgg(AMEntityRegistry.ENDERGRADE, 0X7862B3,0x81BDEB, new Item.Properties()));
        regItem("spawn_egg_hammerhead_shark", () -> AMCompat.spawnEgg(AMEntityRegistry.HAMMERHEAD_SHARK, 0X8A92B5,0XB9BED8, new Item.Properties()));
        regItem("spawn_egg_lobster", () -> AMCompat.spawnEgg(AMEntityRegistry.LOBSTER, 0XC43123,0XDD5F38, new Item.Properties()));
        regItem("spawn_egg_komodo_dragon", () -> AMCompat.spawnEgg(AMEntityRegistry.KOMODO_DRAGON, 0X746C4F,0X564231, new Item.Properties()));
        regItem("spawn_egg_capuchin_monkey", () -> AMCompat.spawnEgg(AMEntityRegistry.CAPUCHIN_MONKEY, 0X25211F,0XF1DAB3, new Item.Properties()));
        regItem("spawn_egg_centipede", () -> AMCompat.spawnEgg(AMEntityRegistry.CENTIPEDE_HEAD, 0X342B2E,0X733449, new Item.Properties()));
        regItem("spawn_egg_warped_toad", () -> AMCompat.spawnEgg(AMEntityRegistry.WARPED_TOAD, 0X1F968E,0XFEAC6D, new Item.Properties()));
        regItem("spawn_egg_moose", () -> AMCompat.spawnEgg(AMEntityRegistry.MOOSE, 0X36302A,0XD4B183, new Item.Properties()));
        regItem("spawn_egg_mimicube", () -> AMCompat.spawnEgg(AMEntityRegistry.MIMICUBE, 0X8A80C1,0X5E4F6F, new Item.Properties()));
        regItem("spawn_egg_raccoon", () -> AMCompat.spawnEgg(AMEntityRegistry.RACCOON, 0X85827E,0X2A2726, new Item.Properties()));
        regItem("spawn_egg_blobfish", () -> AMCompat.spawnEgg(AMEntityRegistry.BLOBFISH, 0XDBC6BD,0X9E7A7F, new Item.Properties()));
        regItem("spawn_egg_seal", () -> AMCompat.spawnEgg(AMEntityRegistry.SEAL, 0X483C32,0X66594C, new Item.Properties()));
        regItem("spawn_egg_cockroach", () -> AMCompat.spawnEgg(AMEntityRegistry.COCKROACH, 0X0D0909,0X42241E, new Item.Properties()));
        regItem("spawn_egg_shoebill", () -> AMCompat.spawnEgg(AMEntityRegistry.SHOEBILL, 0X828282,0XD5B48A, new Item.Properties()));
        regItem("spawn_egg_elephant", () -> AMCompat.spawnEgg(AMEntityRegistry.ELEPHANT, 0X8D8987,0XEDE5D1, new Item.Properties()));
        regItem("spawn_egg_soul_vulture", () -> AMCompat.spawnEgg(AMEntityRegistry.SOUL_VULTURE, 0X23262D,0X57F4FF, new Item.Properties()));
        regItem("spawn_egg_snow_leopard", () -> AMCompat.spawnEgg(AMEntityRegistry.SNOW_LEOPARD, 0XACA293,0X26201D, new Item.Properties()));
        regItem("spawn_egg_spectre", () -> AMCompat.spawnEgg(AMEntityRegistry.SPECTRE, 0XC8D0EF,0X8791EF, new Item.Properties()));
        regItem("spawn_egg_crow", () -> AMCompat.spawnEgg(AMEntityRegistry.CROW, 0X0D111C,0X1C2030, new Item.Properties()));
        regItem("spawn_egg_alligator_snapping_turtle", () -> AMCompat.spawnEgg(AMEntityRegistry.ALLIGATOR_SNAPPING_TURTLE, 0X6C5C52,0X456926, new Item.Properties()));
        regItem("spawn_egg_mungus", () -> AMCompat.spawnEgg(AMEntityRegistry.MUNGUS, 0X836A8D,0X45454C, new Item.Properties()));
        regItem("spawn_egg_mantis_shrimp", () -> AMCompat.spawnEgg(AMEntityRegistry.MANTIS_SHRIMP, 0XDB4858,0X15991E, new Item.Properties()));
        regItem("spawn_egg_guster", () -> AMCompat.spawnEgg(AMEntityRegistry.GUSTER, 0XF8D49A,0XFF720A, new Item.Properties()));
        regItem("spawn_egg_warped_mosco", () -> AMCompat.spawnEgg(AMEntityRegistry.WARPED_MOSCO, 0X322F58,0X5B5EF1, new Item.Properties()));
        regItem("spawn_egg_straddler", () -> AMCompat.spawnEgg(AMEntityRegistry.STRADDLER, 0X5D5F6E,0XCDA886, new Item.Properties()));
        regItem("spawn_egg_stradpole", () -> AMCompat.spawnEgg(AMEntityRegistry.STRADPOLE, 0X5D5F6E,0X576A8B, new Item.Properties()));
        regItem("spawn_egg_emu", () -> AMCompat.spawnEgg(AMEntityRegistry.EMU, 0X665346,0X3B3938, new Item.Properties()));
        regItem("spawn_egg_platypus", () -> AMCompat.spawnEgg(AMEntityRegistry.PLATYPUS, 0X7D503E,0X363B43, new Item.Properties()));
        regItem("spawn_egg_dropbear", () -> AMCompat.spawnEgg(AMEntityRegistry.DROPBEAR, 0X8A2D35,0X60A3A3, new Item.Properties()));
        regItem("spawn_egg_tasmanian_devil", () -> AMCompat.spawnEgg(AMEntityRegistry.TASMANIAN_DEVIL, 0X252426,0XA8B4BF, new Item.Properties()));
        regItem("spawn_egg_kangaroo", () -> AMCompat.spawnEgg(AMEntityRegistry.KANGAROO, 0XCE9D65,0XDEBDA0, new Item.Properties()));
        regItem("spawn_egg_cachalot_whale", () -> AMCompat.spawnEgg(AMEntityRegistry.CACHALOT_WHALE, 0X949899,0X5F666E, new Item.Properties()));
        regItem("spawn_egg_leafcutter_ant", () -> AMCompat.spawnEgg(AMEntityRegistry.LEAFCUTTER_ANT, 0X964023,0XA65930, new Item.Properties()));
        regItem("spawn_egg_enderiophage", () -> AMCompat.spawnEgg(AMEntityRegistry.ENDERIOPHAGE, 0X872D83,0XF6E2CD, new Item.Properties()));
        regItem("spawn_egg_bald_eagle", () -> AMCompat.spawnEgg(AMEntityRegistry.BALD_EAGLE, 0X321F18,0XF4F4F4, new Item.Properties()));
        regItem("spawn_egg_tiger", () -> AMCompat.spawnEgg(AMEntityRegistry.TIGER, 0XC7612E,0X2A3233, new Item.Properties()));
        regItem("spawn_egg_tarantula_hawk", () -> AMCompat.spawnEgg(AMEntityRegistry.TARANTULA_HAWK, 0X234763,0XE37B38, new Item.Properties()));
        regItem("spawn_egg_void_worm", () -> AMCompat.spawnEgg(AMEntityRegistry.VOID_WORM, 0X0F1026,0X1699AB, new Item.Properties()));
        regItem("spawn_egg_frilled_shark", () -> AMCompat.spawnEgg(AMEntityRegistry.FRILLED_SHARK, 0X726B6B,0X873D3D, new Item.Properties()));
        regItem("spawn_egg_mimic_octopus", () -> AMCompat.spawnEgg(AMEntityRegistry.MIMIC_OCTOPUS, 0XFFEBDC,0X1D1C1F, new Item.Properties()));
        regItem("spawn_egg_seagull", () -> AMCompat.spawnEgg(AMEntityRegistry.SEAGULL, 0XC9D2DC,0XFFD850, new Item.Properties()));
        regItem("spawn_egg_froststalker", () -> AMCompat.spawnEgg(AMEntityRegistry.FROSTSTALKER, 0X788AC1,0XA1C3FF, new Item.Properties()));
        regItem("spawn_egg_tusklin", () -> AMCompat.spawnEgg(AMEntityRegistry.TUSKLIN, 0X735841,0XE8E2D5, new Item.Properties()));
        regItem("spawn_egg_laviathan", () -> AMCompat.spawnEgg(AMEntityRegistry.LAVIATHAN, 0XD68356,0X3C3947, new Item.Properties()));
        regItem("spawn_egg_cosmaw", () -> AMCompat.spawnEgg(AMEntityRegistry.COSMAW, 0X746DBD,0XD6BFE3, new Item.Properties()));
        regItem("spawn_egg_toucan", () -> AMCompat.spawnEgg(AMEntityRegistry.TOUCAN, 0XF58F33,0X1E2133, new Item.Properties()));
        regItem("spawn_egg_maned_wolf", () -> AMCompat.spawnEgg(AMEntityRegistry.MANED_WOLF, 0XBB7A47,0X40271A, new Item.Properties()));
        regItem("spawn_egg_anaconda", () -> AMCompat.spawnEgg(AMEntityRegistry.ANACONDA, 0X565C22,0XD3763F, new Item.Properties()));
        regItem("spawn_egg_anteater", () -> AMCompat.spawnEgg(AMEntityRegistry.ANTEATER, 0X4C3F3A, 0XCCBCB4, new Item.Properties()));
        regItem("spawn_egg_rocky_roller", () -> AMCompat.spawnEgg(AMEntityRegistry.ROCKY_ROLLER, 0XB0856F, 0X999184, new Item.Properties()));
        regItem("spawn_egg_flutter", () -> AMCompat.spawnEgg(AMEntityRegistry.FLUTTER, 0X70922D, 0XD07BE3, new Item.Properties()));
        regItem("spawn_egg_gelada_monkey", () -> AMCompat.spawnEgg(AMEntityRegistry.GELADA_MONKEY, 0XB08C64, 0XFF4F53, new Item.Properties()));
        regItem("spawn_egg_jerboa", () -> AMCompat.spawnEgg(AMEntityRegistry.JERBOA, 0XDEC58A, 0XDE9D90, new Item.Properties()));
        regItem("spawn_egg_terrapin", () -> AMCompat.spawnEgg(AMEntityRegistry.TERRAPIN, 0X6E6E30, 0X929647, new Item.Properties()));
        regItem("spawn_egg_comb_jelly", () -> AMCompat.spawnEgg(AMEntityRegistry.COMB_JELLY, 0XCFE9FE, 0X6EFF8B, new Item.Properties()));
        regItem("spawn_egg_cosmic_cod", () -> AMCompat.spawnEgg(AMEntityRegistry.COSMIC_COD, 0X6985C7, 0XE2D1FF, new Item.Properties()));
        regItem("spawn_egg_bunfungus", () -> AMCompat.spawnEgg(AMEntityRegistry.BUNFUNGUS, 0X6F6D91, 0XC92B29, new Item.Properties()));
        regItem("spawn_egg_bison", () -> AMCompat.spawnEgg(AMEntityRegistry.BISON, 0X4C3A2E, 0X7A6546, new Item.Properties()));
        regItem("spawn_egg_giant_squid", () -> AMCompat.spawnEgg(AMEntityRegistry.GIANT_SQUID, 0XAB4B4D, 0XD67D6B, new Item.Properties()));
        regItem("spawn_egg_devils_hole_pupfish", () -> AMCompat.spawnEgg(AMEntityRegistry.DEVILS_HOLE_PUPFISH, 0X567BC4, 0X6C4475, new Item.Properties()));
        regItem("spawn_egg_catfish", () -> AMCompat.spawnEgg(AMEntityRegistry.CATFISH, 0X807757, 0X8A7466, new Item.Properties()));
        regItem("spawn_egg_flying_fish", () -> AMCompat.spawnEgg(AMEntityRegistry.FLYING_FISH, 0X7BBCED, 0X6881B3, new Item.Properties()));
        regItem("spawn_egg_skelewag", () -> AMCompat.spawnEgg(AMEntityRegistry.SKELEWAG, 0XD9FCB1, 0X3A4F30, new Item.Properties()));
        regItem("spawn_egg_rain_frog", () -> AMCompat.spawnEgg(AMEntityRegistry.RAIN_FROG, 0XC0B59B, 0X7B654F, new Item.Properties()));
        regItem("spawn_egg_potoo", () -> AMCompat.spawnEgg(AMEntityRegistry.POTOO, 0X8C7753, 0XFFC042, new Item.Properties()));
        regItem("spawn_egg_mudskipper", () -> AMCompat.spawnEgg(AMEntityRegistry.MUDSKIPPER, 0X60704A, 0X49806C, new Item.Properties()));
        regItem("spawn_egg_rhinoceros", () -> AMCompat.spawnEgg(AMEntityRegistry.RHINOCEROS, 0XA19594, 0X827474, new Item.Properties()));
        regItem("spawn_egg_sugar_glider", () -> AMCompat.spawnEgg(AMEntityRegistry.SUGAR_GLIDER, 0X868181, 0XEBEBE0, new Item.Properties()));
        regItem("spawn_egg_farseer", () -> AMCompat.spawnEgg(AMEntityRegistry.FARSEER, 0X33374F, 0X91FF59, new Item.Properties()));
        regItem("spawn_egg_skreecher", () -> AMCompat.spawnEgg(AMEntityRegistry.SKREECHER, 0X074857, 0X7FF8FF, new Item.Properties()));
        regItem("spawn_egg_underminer", () -> AMCompat.spawnEgg(AMEntityRegistry.UNDERMINER, 0XD6E2FF, 0X6C84C4, new Item.Properties()));
        regItem("spawn_egg_murmur", () -> AMCompat.spawnEgg(AMEntityRegistry.MURMUR, 0X804448, 0XB5AF9C, new Item.Properties()));
        regItem("spawn_egg_skunk", () -> AMCompat.spawnEgg(AMEntityRegistry.SKUNK, 0X222D36, 0XE4E5F2, new Item.Properties()));
        regItem("spawn_egg_banana_slug", () -> AMCompat.spawnEgg(AMEntityRegistry.BANANA_SLUG, 0XFFD045, 0XFFF173, new Item.Properties()));
        regItem("spawn_egg_blue_jay", () -> AMCompat.spawnEgg(AMEntityRegistry.BLUE_JAY, 0X5FB7FE, 0X293B42, new Item.Properties()));
        regItem("spawn_egg_caiman", () -> AMCompat.spawnEgg(AMEntityRegistry.CAIMAN, 0X5C5631, 0XBBC45C, new Item.Properties()));
        regItem("spawn_egg_triops", () -> AMCompat.spawnEgg(AMEntityRegistry.TRIOPS, 0X967954, 0XCA7150, new Item.Properties()));
        registerPatternItem("bear");
        registerPatternItem("australia_0");
        registerPatternItem("australia_1");
        registerPatternItem("new_mexico");
        registerPatternItem("brazil");
        for(int i = 0; i <= 10; i++){
            regItem("dimensional_carver_shard_" + i, () -> new ItemInventoryOnly(new Item.Properties()));
        }
    }

    private static void registerPatternItem(String name) {
        // 1.21.5 removed BannerPatternItem (loom patterns are data-driven now); mirror AlexsMobsFP
        // and register a plain stack-1 Item there.
        //? if >=1.21.5 {
        /*regItem("banner_pattern_" + name, () -> new Item((new Item.Properties()).stacksTo(1)));
        *///?} else {
        TagKey<BannerPattern> bannerPatternTagKey = TagKey.create(Registries.BANNER_PATTERN, AMCompat.rl(AlexsMobs.MODID, "pattern_for_" + name));
        regItem("banner_pattern_" + name, () -> new BannerPatternItem(bannerPatternTagKey, (new Item.Properties()).stacksTo(1)));
        //?}
    }

    public static void init() {
        CROCODILE_ARMOR_MATERIAL.setRepairMaterial(Ingredient.of(CROCODILE_SCUTE.get()));
        ROADRUNNER_ARMOR_MATERIAL.setRepairMaterial(Ingredient.of(ROADRUNNER_FEATHER.get()));
        CENTIPEDE_ARMOR_MATERIAL.setRepairMaterial(Ingredient.of(CENTIPEDE_LEG.get()));
        MOOSE_ARMOR_MATERIAL.setRepairMaterial(Ingredient.of(MOOSE_ANTLER.get()));
        RACCOON_ARMOR_MATERIAL.setRepairMaterial(Ingredient.of(RACCOON_TAIL.get()));
        SOMBRERO_ARMOR_MATERIAL.setRepairMaterial(Ingredient.of(Items.HAY_BLOCK));
        SPIKED_TURTLE_SHELL_ARMOR_MATERIAL.setRepairMaterial(Ingredient.of(SPIKED_SCUTE.get()));
        FEDORA_ARMOR_MATERIAL.setRepairMaterial(Ingredient.of(Items.LEATHER));
        EMU_ARMOR_MATERIAL.setRepairMaterial(Ingredient.of(EMU_FEATHER.get()));
        ROCKY_ARMOR_MATERIAL.setRepairMaterial(Ingredient.of(ROCKY_SHELL.get()));
        FLYING_FISH_MATERIAL.setRepairMaterial(Ingredient.of(FLYING_FISH.get()));
        NOVELTY_HAT_MATERIAL.setRepairMaterial(Ingredient.of(Items.BONE));
        KIMONO_MATERIAL.setRepairMaterial(() -> AMCompat.ingredientOf(ItemTags.WOOL));
        LecternBooks.BOOKS.put(AMCompat.rl(AlexsMobs.MODID, "animal_dictionary"), new LecternBooks.BookData(0X606B26, 0XFDF8ED));
    }

    public static void initDispenser(){
        DispenserBlock.registerBehavior(SHARK_TOOTH_ARROW.get(), new AbstractProjectileDispenseBehavior() {
            /**
             * Return the projectile entity spawned by this dispense behavior.
             */
            protected Projectile getProjectile(Level worldIn, Position position, ItemStack stackIn) {
                EntitySharkToothArrow entityarrow = new EntitySharkToothArrow(AMEntityRegistry.SHARK_TOOTH_ARROW.get(), position.x(), position.y(), position.z(), worldIn);
                entityarrow.pickup = EntitySharkToothArrow.Pickup.ALLOWED;
                return entityarrow;
            }
        });
        DispenserBlock.registerBehavior(ANCIENT_DART.get(), new AbstractProjectileDispenseBehavior() {
            protected Projectile getProjectile(Level worldIn, Position position, ItemStack stackIn) {
                EntityTossedItem tossedItem = new EntityTossedItem(worldIn, position.x(), position.y(), position.z());
                tossedItem.setDart(true);
                return tossedItem;
            }
        });
        DispenserBlock.registerBehavior(COCKROACH_OOTHECA.get(), new AbstractProjectileDispenseBehavior() {
            protected Projectile getProjectile(Level worldIn, Position position, ItemStack stackIn) {
                EntityCockroachEgg entityarrow = new EntityCockroachEgg(worldIn, position.x(), position.y(), position.z());
                return entityarrow;
            }
        });
        DispenserBlock.registerBehavior(EMU_EGG.get(), new AbstractProjectileDispenseBehavior() {
            protected Projectile getProjectile(Level worldIn, Position position, ItemStack stackIn) {
                EntityEmuEgg entityarrow = new EntityEmuEgg(worldIn, position.x(), position.y(), position.z());
                return entityarrow;
            }
        });
        DispenserBlock.registerBehavior(ENDERIOPHAGE_ROCKET.get(), new AbstractProjectileDispenseBehavior() {
            protected Projectile getProjectile(Level worldIn, Position position, ItemStack stackIn) {
                EntityEnderiophageRocket entityarrow = new EntityEnderiophageRocket(worldIn, position.x(), position.y(), position.z(), stackIn);
                return entityarrow;
            }
        });
        DispenseItemBehavior bucketDispenseBehavior = new DefaultDispenseItemBehavior() {
            private final DefaultDispenseItemBehavior defaultDispenseItemBehavior = new DefaultDispenseItemBehavior();

            public ItemStack execute(BlockSource blockSource, ItemStack stack) {
                DispensibleContainerItem dispensiblecontaineritem = (DispensibleContainerItem)stack.getItem();
                // 1.20.2 turned BlockSource into a record (pos()/state()/level()).
                //? if >=1.20.2 {
                /*BlockPos blockpos = blockSource.pos().relative(blockSource.state().getValue(DispenserBlock.FACING));
                Level level = blockSource.level();
                *///?}
                //? if <1.20.2 {
                BlockPos blockpos = blockSource.getPos().relative(blockSource.getBlockState().getValue(DispenserBlock.FACING));
                Level level = blockSource.getLevel();
                //?}
                if (dispensiblecontaineritem.emptyContents((Player)null, level, blockpos, (BlockHitResult)null)) {
                    dispensiblecontaineritem.checkExtraContent((Player)null, level, stack, blockpos);
                    return new ItemStack(Items.BUCKET);
                } else {
                    return this.defaultDispenseItemBehavior.dispense(blockSource, stack);
                }
            }
        };
        DispenserBlock.registerBehavior(LOBSTER_BUCKET.get(), bucketDispenseBehavior);
        DispenserBlock.registerBehavior(BLOBFISH_BUCKET.get(), bucketDispenseBehavior);
        DispenserBlock.registerBehavior(STRADPOLE_BUCKET.get(), bucketDispenseBehavior);
        DispenserBlock.registerBehavior(PLATYPUS_BUCKET.get(), bucketDispenseBehavior);
        DispenserBlock.registerBehavior(FRILLED_SHARK_BUCKET.get(), bucketDispenseBehavior);
        DispenserBlock.registerBehavior(MIMIC_OCTOPUS_BUCKET.get(), bucketDispenseBehavior);
        DispenserBlock.registerBehavior(TERRAPIN_BUCKET.get(), bucketDispenseBehavior);
        DispenserBlock.registerBehavior(COMB_JELLY_BUCKET.get(), bucketDispenseBehavior);
        DispenserBlock.registerBehavior(COSMIC_COD_BUCKET.get(), bucketDispenseBehavior);
        DispenserBlock.registerBehavior(DEVILS_HOLE_PUPFISH_BUCKET.get(), bucketDispenseBehavior);
        DispenserBlock.registerBehavior(SMALL_CATFISH_BUCKET.get(), bucketDispenseBehavior);
        DispenserBlock.registerBehavior(MEDIUM_CATFISH_BUCKET.get(), bucketDispenseBehavior);
        DispenserBlock.registerBehavior(LARGE_CATFISH_BUCKET.get(), bucketDispenseBehavior);
        DispenserBlock.registerBehavior(FLYING_FISH_BUCKET.get(), bucketDispenseBehavior);
        DispenserBlock.registerBehavior(MUDSKIPPER_BUCKET.get(), bucketDispenseBehavior);
        ComposterBlock.COMPOSTABLES.put(BANANA.get(), 0.65F);
        ComposterBlock.COMPOSTABLES.put(AMBlockRegistry.BANANA_PEEL.get().asItem(), 1F);
        ComposterBlock.COMPOSTABLES.put(ACACIA_BLOSSOM.get(), 0.65F);
        ComposterBlock.COMPOSTABLES.put(GONGYLIDIA.get(), 0.9F);
    }

}

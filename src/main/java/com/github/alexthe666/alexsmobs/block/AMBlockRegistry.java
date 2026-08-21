package com.github.alexthe666.alexsmobs.block;

import com.github.alexthe666.alexsmobs.misc.AMPlatform;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.entity.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.item.AMBlockItem;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.github.alexthe666.alexsmobs.item.BlockItemAMRender;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import com.github.alexthe666.alexsmobs.misc.RegistrationContext;

public class AMBlockRegistry {
    public static final BlockBehaviour.Properties PURPUR_PLANKS_PROPERTIES = BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.5F, 1.0F).sound(SoundType.WOOD);

    public static final DeferredRegister<Block> DEF_REG = DeferredRegister.create(Registries.BLOCK, AlexsMobs.MODID);
    public static final Supplier<Block> BANANA_PEEL = registerBlockAndItem("banana_peel", () -> new BlockBananaPeel());
    public static final Supplier<Block> HUMMINGBIRD_FEEDER = registerBlockAndItem("hummingbird_feeder", () -> new BlockHummingbirdFeeder());
    public static final Supplier<Block> CROCODILE_EGG = registerBlockAndItem("crocodile_egg", () -> new BlockReptileEgg(AMEntityRegistry.CROCODILE));
    public static final Supplier<Block> GUSTMAKER = registerBlockAndItem("gustmaker", () -> new BlockGustmaker());
    public static final Supplier<Block> STRADDLITE_BLOCK = registerBlockAndItem("straddlite_block", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(1.0F, 1200.0F).sound(SoundType.ANCIENT_DEBRIS)), new Item.Properties().fireResistant(), false);
    public static final Supplier<Block> PLATYPUS_EGG = registerBlockAndItem("platypus_egg", () -> new BlockReptileEgg(AMEntityRegistry.PLATYPUS));
    public static final Supplier<Block> LEAFCUTTER_ANTHILL = registerBlockAndItem("leafcutter_anthill", () -> new BlockLeafcutterAnthill());
    public static final Supplier<Block> LEAFCUTTER_ANT_CHAMBER = registerBlockAndItem("leafcutter_ant_chamber", () -> new BlockLeafcutterAntChamber());
    public static final Supplier<Block> CAPSID = registerBlockAndItem("capsid", () -> new BlockCapsid());
    public static final Supplier<Block> VOID_WORM_BEAK = registerBlockAndItem("void_worm_beak", () -> new BlockVoidWormBeak());
    public static final Supplier<Block> VOID_WORM_EFFIGY = registerBlockAndItem("void_worm_effigy", () -> new BlockVoidWormEffigy());
    public static final Supplier<Block> TERRAPIN_EGG = registerBlockAndItem("terrapin_egg", () -> new BlockTerrapinEgg());
    public static final Supplier<Block> RAINBOW_GLASS = registerBlockAndItem("rainbow_glass", () -> new BlockRainbowGlass());
    public static final Supplier<Block> BISON_FUR_BLOCK = registerBlockAndItem("bison_fur_block", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.6F, 1.0F).sound(SoundType.WOOL)));
    public static final Supplier<Block> BISON_CARPET = registerBlockAndItem("bison_carpet", () -> new BlockBisonCarpet());
    public static final Supplier<Block> SAND_CIRCLE = registerBlockAndItem("sand_circle", () -> AMPlatform.coloredSand(14406560, AMPlatform.copyProperties(Blocks.SAND)), new Item.Properties(), false);
    public static final Supplier<Block> RED_SAND_CIRCLE = registerBlockAndItem("red_sand_circle", () -> AMPlatform.coloredSand(11098145, AMPlatform.copyProperties(Blocks.RED_SAND)), new Item.Properties(), false);
    public static final Supplier<Block> ENDER_RESIDUE = registerBlockAndItem("ender_residue", () -> new BlockEnderResidue());
    public static final Supplier<Block> TRANSMUTATION_TABLE = registerBlockAndItem("transmutation_table", () -> new BlockTransmutationTable(), new Item.Properties().rarity(Rarity.EPIC).fireResistant(), true);
    public static final Supplier<Block> SCULK_BOOMER = registerBlockAndItem("sculk_boomer", () -> new BlockSculkBoomer());
    public static final Supplier<Block> SKUNK_SPRAY = regBlock("skunk_spray", () -> new BlockSkunkSpray());
    public static final Supplier<Block> BANANA_SLUG_SLIME_BLOCK = registerBlockAndItem("banana_slug_slime_block", () -> new BlockBananaSlugSlime());
    public static final Supplier<Block> CRYSTALIZED_BANANA_SLUG_MUCUS = registerBlockAndItem("crystalized_banana_slug_mucus", () -> new BlockCrystalizedMucus());
    public static final Supplier<Block> CAIMAN_EGG = registerBlockAndItem("caiman_egg", () -> new BlockReptileEgg(AMEntityRegistry.CAIMAN));
    public static final Supplier<Block> TRIOPS_EGGS = registerBlockAndItem("triops_eggs", () -> new BlockTriopsEggs());
    /*
        public static final Supplier<Block> PURPUR_PLANKS = registerBlockAndItem("purpur_planks", () -> new Block(PURPUR_PLANKS_PROPERTIES));;
    public static final Supplier<Block> PURPUR_PLANKS_STAIRS = registerBlockAndItem("purpur_planks_stairs", () -> new StairBlock(PURPUR_PLANKS.get().defaultBlockState(), PURPUR_PLANKS_PROPERTIES));;
    public static final Supplier<Block> PURPUR_PLANKS_SLAB = registerBlockAndItem("purpur_planks_slab", () -> new SlabBlock(PURPUR_PLANKS_PROPERTIES));;
    public static final Supplier<Block> PURPUR_PLANKS_WALL = registerBlockAndItem("purpur_planks_wall", () -> new WallBlock(PURPUR_PLANKS_PROPERTIES));;
    public static final Supplier<Block> END_PIRATE_DOOR = registerBlockAndItem("end_pirate_door", () -> new BlockEndPirateDoor());
    public static final Supplier<Block> END_PIRATE_TRAPDOOR = registerBlockAndItem("end_pirate_trapdoor", () -> new TrapDoorBlock(BlockBehaviour.Properties.of(Material.GLASS, MaterialColor.TERRACOTTA_PURPLE).lightLevel((state) -> 3).strength(3.0F).sound(SoundType.GLASS).noOcclusion()));;
    public static final Supplier<Block> END_PIRATE_ANCHOR = registerBlockAndItem("end_pirate_anchor", () -> new BlockEndPirateAnchor());
    public static final Supplier<Block> END_PIRATE_ANCHOR_WINCH = registerBlockAndItem("end_pirate_anchor_winch", () -> new BlockEndPirateAnchorWinch());
    public static final Supplier<Block> END_PIRATE_SHIP_WHEEL = registerBlockAndItem("end_pirate_ship_wheel", () -> new BlockEndPirateShipWheel());
    public static final Supplier<Block> END_PIRATE_FLAG = registerBlockAndItem("end_pirate_flag", () -> new BlockEndPirateFlag());
    public static final Supplier<Block> PHANTOM_SAIL = registerBlockAndItem("phantom_sail", () -> new BlockEndPirateSail(false));
    public static final Supplier<Block> SPECTRE_SAIL = registerBlockAndItem("spectre_sail", () -> new BlockEndPirateSail(true));

     */

    public static Supplier<Block> registerBlockAndItem(String name, Supplier<Block> block){
        return registerBlockAndItem(name, block, new Item.Properties(), false);
    }

    public static Supplier<Block> registerBlockAndItem(String name, Supplier<Block> block, Item.Properties blockItemProps, boolean specialRender){
        Supplier<Block> blockObj = regBlock(name, block);
        final Item.Properties props = blockDescriptionId(blockItemProps);
        AMItemRegistry.regItem(name, () -> specialRender ?  new BlockItemAMRender(blockObj, props) :  new AMBlockItem(blockObj, props));
        return blockObj;
    }

    // Every one of this mod's 36 block items is translated under `block.alexsmobs.<name>` — there is
    // not a single `item.alexsmobs.<name>` key for them. Until 1.21.2 that was automatic, because
    // BlockItem overrode getDescriptionId() to hand back its block's. 1.21.2 deleted that override and
    // made Item#getDescriptionId **final**, resolving the id from a DependantName chosen on the
    // Properties instead — defaulting to the *item* prefix. So from 1.21.2 up, on all three loaders,
    // every block item asked for a key that does not exist and the creative menu showed the raw
    // `item.alexsmobs.banana_peel` (report #30). Opting the properties into the block prefix restores
    // the pre-1.21.2 behaviour; there is no way to do it from the BlockItem subclass.
    private static Item.Properties blockDescriptionId(Item.Properties props) {
        //? if >=1.21.2 {
        /*return props.useBlockDescriptionPrefix();
        *///?} else {
        return props;
        //?}
    }

    // Registers a block and, on >=1.21.2, publishes its registry key via RegistrationContext so the
    // BlockBehaviour.Properties mixin can stamp it with setId(...) during construction (mandatory since 1.21.2).
    public static <B extends Block> Supplier<B> regBlock(String name, Supplier<B> sup){
        //? if >=1.21.2 {
        /*return DEF_REG.register(name, () -> {
            RegistrationContext.CURRENT_ID.set(ResourceKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(AlexsMobs.MODID, name)));
            try { return sup.get(); } finally { RegistrationContext.CURRENT_ID.remove(); }
        });
        *///?} else {
        return DEF_REG.register(name, sup);
        //?}
    }
}

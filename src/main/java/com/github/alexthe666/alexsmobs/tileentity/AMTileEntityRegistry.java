package com.github.alexthe666.alexsmobs.tileentity;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.block.AMBlockRegistry;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;

// No @SubscribeEvent methods here, so no @EventBusSubscriber: NeoForge treats an empty
// automatic-subscriber class as an error.
public class AMTileEntityRegistry {

    public static final DeferredRegister<BlockEntityType<?>> DEF_REG = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AlexsMobs.MODID);

    public static final Supplier<BlockEntityType<TileEntityLeafcutterAnthill>> LEAFCUTTER_ANTHILL = DEF_REG.register("leafcutter_anthill_te", () -> type(TileEntityLeafcutterAnthill::new, AMBlockRegistry.LEAFCUTTER_ANTHILL.get()));
    public static final Supplier<BlockEntityType<TileEntityCapsid>> CAPSID = DEF_REG.register("capsid_te", () -> type(TileEntityCapsid::new, AMBlockRegistry.CAPSID.get()));
    public static final Supplier<BlockEntityType<TileEntityVoidWormBeak>> VOID_WORM_BEAK = DEF_REG.register("void_worm_beak_te", () -> type(TileEntityVoidWormBeak::new, AMBlockRegistry.VOID_WORM_BEAK.get()));
    public static final Supplier<BlockEntityType<TileEntityTerrapinEgg>> TERRAPIN_EGG = DEF_REG.register("terrapin_egg_te", () -> type(TileEntityTerrapinEgg::new, AMBlockRegistry.TERRAPIN_EGG.get()));
    public static final Supplier<BlockEntityType<TileEntityTransmutationTable>> TRANSMUTATION_TABLE = DEF_REG.register("transmutation_table", () -> type(TileEntityTransmutationTable::new, AMBlockRegistry.TRANSMUTATION_TABLE.get()));
    public static final Supplier<BlockEntityType<TileEntitySculkBoomer>> SCULK_BOOMER = DEF_REG.register("sculk_boomer", () -> type(TileEntitySculkBoomer::new, AMBlockRegistry.SCULK_BOOMER.get()));
    //TODO reimplement
    public static final Supplier<BlockEntityType<TileEntityEndPirateDoor>> END_PIRATE_DOOR = null;//DEF_REG.register("end_pirate_door_te", () -> type(TileEntityEndPirateDoor::new, AMBlockRegistry.END_PIRATE_DOOR.get()));
    public static final Supplier<BlockEntityType<TileEntityEndPirateAnchor>> END_PIRATE_ANCHOR = null;// DEF_REG.register("end_pirate_anchor_te", () -> type(TileEntityEndPirateAnchor::new, AMBlockRegistry.END_PIRATE_ANCHOR.get()));
    public static final Supplier<BlockEntityType<TileEntityEndPirateAnchorWinch>> END_PIRATE_ANCHOR_WINCH =  null;//DEF_REG.register("end_pirate_anchor_winch_te", () -> type(TileEntityEndPirateAnchorWinch::new, AMBlockRegistry.END_PIRATE_ANCHOR_WINCH.get()));
    public static final Supplier<BlockEntityType<TileEntityEndPirateShipWheel>> END_PIRATE_SHIP_WHEEL = null;// DEF_REG.register("end_pirate_ship_wheel_te", () -> type(TileEntityEndPirateShipWheel::new, AMBlockRegistry.END_PIRATE_SHIP_WHEEL.get()));
    public static final Supplier<BlockEntityType<TileEntityEndPirateFlag>> END_PIRATE_FLAG = null;// DEF_REG.register("end_pirate_flag_te", () -> type(TileEntityEndPirateFlag::new, AMBlockRegistry.END_PIRATE_FLAG.get()));

    // 1.21.2 deleted BlockEntityType.Builder and made the constructor public.
    //? if >=1.21.2 {
    /*private static <T extends net.minecraft.world.level.block.entity.BlockEntity> BlockEntityType<T> type(BlockEntityType.BlockEntitySupplier<T> factory, net.minecraft.world.level.block.Block block) {
        return new BlockEntityType<>(factory, java.util.Set.of(block));
    }
    *///?} else {
    private static <T extends net.minecraft.world.level.block.entity.BlockEntity> BlockEntityType<T> type(BlockEntityType.BlockEntitySupplier<T> factory, net.minecraft.world.level.block.Block block) {
        return BlockEntityType.Builder.of(factory, block).build(null);
    }
    //?}
}

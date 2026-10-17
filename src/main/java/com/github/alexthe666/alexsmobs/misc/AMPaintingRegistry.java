package com.github.alexthe666.alexsmobs.misc;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import net.minecraft.world.entity.decoration.PaintingVariant;
//? if <1.21 {
import net.minecraftforge.registries.DeferredRegister;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
//?}

//? if <1.21 {
public class AMPaintingRegistry {
    public static final DeferredRegister<PaintingVariant> DEF_REG = DeferredRegister.create(Registries.PAINTING_VARIANT, AlexsMobs.MODID);

    public static final Supplier<PaintingVariant> NFT = DEF_REG.register("nft", () -> new PaintingVariant(32, 32));
    public static final Supplier<PaintingVariant> DOG_POKER = DEF_REG.register("dog_poker", () -> new PaintingVariant(32, 16));
}
//?}

// 1.21 turned PAINTING_VARIANT into a datapack registry (and re-measured width/height in blocks
// rather than pixels), so there is nothing left to register from code — the two variants ship as
// data/alexsmobs/painting_variant/{nft,dog_poker}.json and are listed in the vanilla
// #minecraft:placeable tag exactly as before.
//? if >=1.21 {
/*public class AMPaintingRegistry {
}
*///?}

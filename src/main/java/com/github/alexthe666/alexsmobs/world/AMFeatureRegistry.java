package com.github.alexthe666.alexsmobs.world;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import net.minecraft.world.level.levelgen.feature.Feature;
//? if <26.3 {
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
//?}
import net.minecraftforge.registries.DeferredRegister;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;

// 26.3 split the old Registries.FEATURE in two. FEATURE_TYPE ("worldgen/feature_type") is the
// code registry of MapCodecs that a mod registers, and FEATURE ("worldgen/feature") is now the
// DATAPACK registry of built Feature instances — i.e. it took over the job of CONFIGURED_FEATURE,
// which no longer exists. So what gets registered here changes from an instance to a codec.
//
// ⚠️ The datapack half moves with it: the anthill's JSON lives at
// data/alexsmobs/worldgen/feature/leafcutter_anthill.json on >=26.3 and at
// .../worldgen/configured_feature/leafcutter_anthill.json below it, and the "config" wrapper was
// flattened away. Both files are shipped unconditionally — each node's registry loader only ever
// visits the directory its own version knows about, so the other one is inert rather than wrong.
// A configured-feature file in a registry that no longer exists is logged-and-ignored, so getting
// this wrong stops the anthill generating with every gate still green (rule 8).
public class AMFeatureRegistry {
    //? if >=26.3 {
    /*public static final DeferredRegister<com.mojang.serialization.MapCodec<? extends Feature>> DEF_REG =
            DeferredRegister.create(Registries.FEATURE_TYPE, AlexsMobs.MODID);

    public static final Supplier<com.mojang.serialization.MapCodec<? extends Feature>> LEAFCUTTER_ANTHILL =
            DEF_REG.register("leafcutter_anthill", () -> FeatureLeafcutterAnthill.CODEC);
    *///?} else {
    public static final DeferredRegister<Feature<?>> DEF_REG = DeferredRegister.create(Registries.FEATURE, AlexsMobs.MODID);

    public static final Supplier<Feature<NoneFeatureConfiguration>> LEAFCUTTER_ANTHILL = DEF_REG.register("leafcutter_anthill", () -> new FeatureLeafcutterAnthill(NoneFeatureConfiguration.CODEC));
    //?}

}

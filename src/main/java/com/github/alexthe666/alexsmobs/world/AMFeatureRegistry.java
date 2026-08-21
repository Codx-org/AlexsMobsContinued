package com.github.alexthe666.alexsmobs.world;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.registries.DeferredRegister;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;

public class AMFeatureRegistry {
    public static final DeferredRegister<Feature<?>> DEF_REG = DeferredRegister.create(Registries.FEATURE, AlexsMobs.MODID);

    public static final Supplier<Feature<NoneFeatureConfiguration>> LEAFCUTTER_ANTHILL = DEF_REG.register("leafcutter_anthill", () -> new FeatureLeafcutterAnthill(NoneFeatureConfiguration.CODEC));

}

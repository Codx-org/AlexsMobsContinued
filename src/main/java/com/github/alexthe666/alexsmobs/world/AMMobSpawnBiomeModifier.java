package com.github.alexthe666.alexsmobs.world;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;
import java.util.function.Supplier;

public class AMMobSpawnBiomeModifier implements BiomeModifier {
    // Assigned in AlexsMobs' mod-bus registration (both loaders' DeferredRegister handles
    // implement Supplier), so this file needs no loader-specific registry-object type.
    //? if >=1.20.5 {
    /*public static Supplier<? extends com.mojang.serialization.MapCodec<? extends BiomeModifier>> SERIALIZER;
    *///?} else {
    public static Supplier<? extends Codec<? extends BiomeModifier>> SERIALIZER;
    //?}

    public AMMobSpawnBiomeModifier() {
    }

    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase == Phase.ADD) {
            AMWorldRegistry.addBiomeSpawns(biome, builder);
        }
    }

    // 1.20.5: BiomeModifier serializers are MapCodecs.
    //? if >=1.20.5 {
    /*    public com.mojang.serialization.MapCodec<? extends BiomeModifier> codec() {
        return (com.mojang.serialization.MapCodec)SERIALIZER.get();
    }

    public static com.mojang.serialization.MapCodec<AMMobSpawnBiomeModifier> makeCodec() {
        return com.mojang.serialization.MapCodec.unit(AMMobSpawnBiomeModifier::new);
    }
    *///?} else {
    public Codec<? extends BiomeModifier> codec() {
        return (Codec)SERIALIZER.get();
    }

    public static Codec<AMMobSpawnBiomeModifier> makeCodec() {
        return Codec.unit(AMMobSpawnBiomeModifier::new);
    }
    //?}
}

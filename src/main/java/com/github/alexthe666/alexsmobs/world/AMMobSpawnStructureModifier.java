package com.github.alexthe666.alexsmobs.world;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraftforge.common.world.ModifiableStructureInfo;
import net.minecraftforge.common.world.StructureModifier;
import java.util.function.Supplier;

public class AMMobSpawnStructureModifier implements StructureModifier {

    // Assigned in AlexsMobs' mod-bus registration (both loaders' DeferredRegister handles
    // implement Supplier), so this file needs no loader-specific registry-object type.
    //? if >=1.20.5 {
    /*public static Supplier<? extends com.mojang.serialization.MapCodec<? extends StructureModifier>> SERIALIZER;
    *///?} else {
    public static Supplier<? extends Codec<? extends StructureModifier>> SERIALIZER;
    //?}

    public AMMobSpawnStructureModifier() {
    }

    @Override
    public void modify(Holder<Structure> structure, Phase phase, ModifiableStructureInfo.StructureInfo.Builder builder) {
        if (phase == StructureModifier.Phase.ADD) {
            AMWorldRegistry.modifyStructure(structure, builder);

        }
    }

    // 1.20.5: StructureModifier serializers are MapCodecs.
    //? if >=1.20.5 {
    /*    public com.mojang.serialization.MapCodec<? extends StructureModifier> codec() {
        return (com.mojang.serialization.MapCodec)SERIALIZER.get();
    }

    public static com.mojang.serialization.MapCodec<AMMobSpawnStructureModifier> makeCodec() {
        return com.mojang.serialization.MapCodec.unit(AMMobSpawnStructureModifier::new);
    }
    *///?} else {
    public Codec<? extends StructureModifier> codec() {
        return (Codec)SERIALIZER.get();
    }

    public static Codec<AMMobSpawnStructureModifier> makeCodec() {
        return Codec.unit(AMMobSpawnStructureModifier::new);
    }
    //?}
}

package com.github.alexthe666.alexsmobs.fabric.common.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

/**
 * Fabric stand-in for {@code net.minecraftforge.common.world.BiomeModifier}, reached by the
 * Fabric-only {@code !fab-biomemodifier} replacement rule.
 *
 * <p>Forge's interface also declares {@code DIRECT_CODEC}, {@code REFERENCE_CODEC} and
 * {@code LIST_CODEC}, all of which dispatch through {@code ForgeRegistries.BIOME_MODIFIER_SERIALIZERS}
 * — a registry with no Fabric counterpart, and one nothing in this mod names. Only the two members
 * {@code AMMobSpawnBiomeModifier} and {@code AMLeafcutterAntBiomeModifier} actually implement are
 * vendored.
 *
 * <p>See {@link ModifiableBiomeInfo} for what not having the Forge pipeline costs on this loader.
 */
public interface BiomeModifier {

    void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder);

    // 1.20.5 turned every serializer registry MapCodec-based; below it the two modifiers' codec()
    // overrides return a plain Codec, so the declaration has to change shape with them.
    //? if >=1.20.5 {
    /*MapCodec<? extends BiomeModifier> codec();
    *///?} else {
    com.mojang.serialization.Codec<? extends BiomeModifier> codec();
    //?}

    /** Copied verbatim from Forge — the two modifiers here only ever test {@code ADD}. */
    enum Phase {
        BEFORE_EVERYTHING,
        ADD,
        REMOVE,
        MODIFY,
        AFTER_EVERYTHING
    }
}

package com.github.alexthe666.alexsmobs.fabric.common.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * Fabric stand-in for {@code net.minecraftforge.common.world.StructureModifier}, reached by the
 * Fabric-only {@code !fab-structuremodifier} replacement rule. The structure sibling of
 * {@link BiomeModifier}, vendored on the same terms — see {@link ModifiableStructureInfo} for what
 * is inert on this loader.
 */
public interface StructureModifier {

    void modify(Holder<Structure> structure, Phase phase, ModifiableStructureInfo.StructureInfo.Builder builder);

    // 1.20.5 turned every serializer registry MapCodec-based; below it AMMobSpawnStructureModifier's
    // codec() override returns a plain Codec, so the declaration has to change shape with it.
    //? if >=1.20.5 {
    /*MapCodec<? extends StructureModifier> codec();
    *///?} else {
    com.mojang.serialization.Codec<? extends StructureModifier> codec();
    //?}

    /** Copied verbatim from Forge — {@code AMMobSpawnStructureModifier} only ever tests {@code ADD}. */
    enum Phase {
        BEFORE_EVERYTHING,
        ADD,
        REMOVE,
        MODIFY,
        AFTER_EVERYTHING
    }
}

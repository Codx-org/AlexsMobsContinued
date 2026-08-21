package com.github.alexthe666.alexsmobs.fabric.world;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.config.BiomeConfig;
import com.github.alexthe666.alexsmobs.fabric.common.world.ModifiableBiomeInfo;
import com.github.alexthe666.alexsmobs.world.AMWorldRegistry;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Applies the mod's ~88 natural spawn entries — and the leafcutter anthill feature — on Fabric.
 *
 * <p>Forge and NeoForge reach {@link AMWorldRegistry} through a <b>datapack</b> entry: a registered
 * biome-modifier serializer plus {@code data/alexsmobs/{forge,neoforge}/biome_modifier/*.json}, which
 * the loader applies to every biome as the dynamic registries are built. Fabric has no such datapack
 * hook; its equivalent is {@code BiomeModifications}, a plain <b>registration</b> made once at mod
 * init. So this class is the Fabric half of {@code AMMobSpawnBiomeModifier} and
 * {@code AMLeafcutterAntBiomeModifier}, and those two files are simply never constructed here.
 *
 * <p>The spawn table itself stays where it is. {@code AMWorldRegistry.addBiomeSpawns} is
 * loader-neutral and is the single source of truth for what spawns where, at what weight, in what
 * group size; all this does is give it a builder to fill and then drain what it recorded into
 * Fabric's context. That is the whole reason
 * {@link ModifiableBiomeInfo.MobSpawnSettingsBuilder} records as well as delegating.
 *
 * <p>⚠️ <b>Fabric's argument order is not vanilla's.</b> On {@code >=1.21.5},
 * {@code MobSpawnSettingsContext.addSpawn} is {@code (category, data, weight)}; vanilla's builder is
 * {@code (category, weight, data)}. Both take an {@code int} in the middle-or-last position, so
 * swapping them is a silent behaviour bug, not a compile error.
 */
public final class FabricBiomeModifications {

    /** The placed feature {@code AMLeafcutterAntBiomeModifier} gets from its JSON's "features" field. */
    private static final ResourceKey<PlacedFeature> LEAFCUTTER_ANTHILL = ResourceKey.create(
            Registries.PLACED_FEATURE, com.github.alexthe666.alexsmobs.misc.AMCompat.rl(AlexsMobs.MODID, "leafcutter_anthill"));

    private FabricBiomeModifications() {
    }

    public static void init() {
        BiomeModifications.create(com.github.alexthe666.alexsmobs.misc.AMCompat.rl(AlexsMobs.MODID, "spawns"))
                // ADDITIONS is the phase Fabric documents for "add things to biomes", and it matches
                // Forge's Phase.ADD — the only phase AMMobSpawnBiomeModifier acts on.
                //
                // BiomeSelectors.all() rather than a narrower selector on purpose: the mod's own
                // per-mob biome config (BiomeConfig/SpawnBiomeData, driven by the files under
                // config/alexsmobs/) is what decides whether a biome qualifies, and it understands
                // modded biomes. Filtering here would silently override the player's config.
                .add(ModificationPhase.ADDITIONS, BiomeSelectors.all(), (selection, context) -> {
                    final ModifiableBiomeInfo.BiomeInfo.Builder builder = new ModifiableBiomeInfo.BiomeInfo.Builder();
                    AMWorldRegistry.addBiomeSpawns(selection.getBiomeHolder(), builder);

                    for (ModifiableBiomeInfo.RecordedSpawn spawn : builder.getRecordedSpawns()) {
                        // Fabric API moved the weight out of SpawnerData in lockstep with vanilla:
                        // biome-api-v1 15.0.6 (1.21.4) is addSpawn(category, data), 16.0.7 (1.21.5) is
                        // addSpawn(category, data, weight). Below the boundary the recorded weight is
                        // already inside spawn.data(), so passing it again would be wrong, not just
                        // redundant.
                        //? if >=1.21.5 {
                        /*context.getMobSpawnSettings().addSpawn(spawn.category(), spawn.data(), spawn.weight());
                        *///?} else {
                        context.getMobSpawnSettings().addSpawn(spawn.category(), spawn.data());
                        //?}
                    }

                    // The anthill feature does NOT go through addLeafcutterAntSpawns: that method
                    // takes the HolderSet<PlacedFeature> the Forge modifier decoded from its JSON,
                    // and Fabric's context wants a ResourceKey instead — there is no registry lookup
                    // in scope here to turn one into the other. Its guard is reproduced verbatim, and
                    // both halves of it (the biome test and the config chance) are the shared,
                    // loader-neutral source of truth.
                    if (AMWorldRegistry.testBiome(BiomeConfig.leafcutter_anthill_spawns, selection.getBiomeHolder())
                            && AMConfig.leafcutterAnthillSpawnChance > 0) {
                        context.getGenerationSettings().addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, LEAFCUTTER_ANTHILL);
                    }
                });
    }
}

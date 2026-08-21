package com.github.alexthe666.alexsmobs.fabric.common.world;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Fabric stand-in for {@code net.minecraftforge.common.world.ModifiableBiomeInfo}, reached by the
 * Fabric-only {@code !fab-modbiomeinfo} replacement rule — the same relocated compat-namespace
 * pattern as {@code fabric/common/Tags} and {@code fabric/common/loot/IGlobalLootModifier}.
 *
 * <p>Only the builder half is vendored. Forge's class also carries the original/modified
 * {@code BiomeInfo} records and the {@code applyBiomeModifiers} driver, which belong to Forge's
 * datapack-driven modifier pipeline; this mod never touches them — {@code AMWorldRegistry} only ever
 * receives a {@code BiomeInfo.Builder} and calls two accessors on it.
 *
 * <p>The thing that constructs one on Fabric is
 * {@code fabric/world/FabricBiomeModifications}. Forge/NeoForge reach
 * {@code AMMobSpawnBiomeModifier} through a serializer registry plus
 * {@code data/alexsmobs/neoforge|forge/biome_modifier/*.json}; Fabric's equivalent is Fabric API's
 * {@code BiomeModifications}, which is a registration rather than a datapack entry. Vendoring this
 * builder is what keeps {@code AMWorldRegistry}'s spawn table — the actual data, and loader-neutral
 * — shared across all three loaders instead of gated out on one.
 *
 * <p>That is why both builders <b>record</b> what is added to them as well as delegating: the Fabric
 * callback constructs a builder, hands it to {@code AMWorldRegistry.addBiomeSpawns}, and then drains
 * {@link Builder#getRecordedSpawns()} into Fabric's {@code BiomeModificationContext}. (The
 * {@link GenerationSettingsBuilder} half is recorded too but currently unused — the anthill feature
 * is added by key, because Fabric's context wants a {@code ResourceKey} where Forge's builder wants
 * a {@code Holder}.)
 */
public class ModifiableBiomeInfo {

    private ModifiableBiomeInfo() {
    }

    /** Mirrors Forge's nested {@code BiomeInfo} purely so the nested {@code Builder} path matches. */
    public static final class BiomeInfo {

        private BiomeInfo() {
        }

        public static class Builder {

            private final MobSpawnSettingsBuilder mobSpawnSettings = new MobSpawnSettingsBuilder();
            private final GenerationSettingsBuilder generationSettings = new GenerationSettingsBuilder();

            public MobSpawnSettingsBuilder getMobSpawnSettings() {
                return this.mobSpawnSettings;
            }

            public GenerationSettingsBuilder getGenerationSettings() {
                return this.generationSettings;
            }

            /** Every {@code (category, weight, data)} triple added during this modifier pass. */
            public List<RecordedSpawn> getRecordedSpawns() {
                return this.mobSpawnSettings.recorded;
            }
        }
    }

    /** One entry of {@code AMWorldRegistry}'s spawn table, in the shape Fabric's API wants it. */
    public record RecordedSpawn(MobCategory category, int weight, MobSpawnSettings.SpawnerData data) {
    }

    /**
     * Extends the real vanilla builder so {@code AMCompat.addSpawn} — which is typed to
     * {@code MobSpawnSettings.Builder} and is version-gated but not loader-gated — needs no Fabric
     * arm at all. The override records as well as delegating, so nothing is lost either way.
     */
    public static class MobSpawnSettingsBuilder extends MobSpawnSettings.Builder {

        private final List<RecordedSpawn> recorded = new ArrayList<>();

        // 1.21.5 pulled the weight out of SpawnerData and into the builder call. Below it the vanilla
        // method is the two-argument addSpawn(MobCategory, SpawnerData) with the weight riding along
        // inside the data, so the override has to change shape with it — overriding the wrong arity
        // compiles fine on the class (it is just a new overload) and silently records nothing.
        // RecordedSpawn keeps carrying the weight separately either way, because that is the shape
        // Fabric's context wants on BOTH sides of the boundary.
        //? if >=1.21.5 {
        /*@Override
        public MobSpawnSettings.Builder addSpawn(MobCategory category, int weight, MobSpawnSettings.SpawnerData data) {
            this.recorded.add(new RecordedSpawn(category, weight, data));
            return super.addSpawn(category, weight, data);
        }
        *///?} else {
        @Override
        public MobSpawnSettings.Builder addSpawn(MobCategory category, MobSpawnSettings.SpawnerData data) {
            this.recorded.add(new RecordedSpawn(category, data.getWeight().asInt(), data));
            return super.addSpawn(category, data);
        }
        //?}
    }

    /**
     * Forge hands out a patched vanilla {@code BiomeGenerationSettings.PlainBuilder} whose
     * {@code getFeatures} exposes the per-step list. Vanilla has no such accessor, so the Fabric
     * stand-in owns the lists outright — the one caller
     * ({@code AMWorldRegistry.addLeafcutterAntSpawns}) only ever adds to one of them.
     */
    public static class GenerationSettingsBuilder {

        private final Map<GenerationStep.Decoration, List<Holder<PlacedFeature>>> features =
                new EnumMap<>(GenerationStep.Decoration.class);

        public List<Holder<PlacedFeature>> getFeatures(GenerationStep.Decoration step) {
            return this.features.computeIfAbsent(step, s -> new ArrayList<>());
        }
    }
}

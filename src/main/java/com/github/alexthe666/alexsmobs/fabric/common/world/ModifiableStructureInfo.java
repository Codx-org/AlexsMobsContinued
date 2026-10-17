package com.github.alexthe666.alexsmobs.fabric.common.world;

import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Fabric stand-in for {@code net.minecraftforge.common.world.ModifiableStructureInfo}, reached by
 * the Fabric-only {@code !fab-modstructinfo} replacement rule.
 *
 * <p>The structure sibling of {@link ModifiableBiomeInfo}, but <b>no longer inert</b>: this is a
 * pure recorder, and something now drains it. {@code AMWorldRegistry.modifyStructure} writes its
 * four spawn overrides here — mimicubes in end cities, soul vultures at nether fossils, skelewags
 * at shipwrecks, underminers in anything tagged {@code alexsmobs:spawns_underminers} — and
 * {@link com.github.alexthe666.alexsmobs.fabric.world.FabricStructureSpawns} reads them back out
 * and merges them in through a {@code Structure} mixin, because Fabric API has no
 * structure-modification hook of any kind (its {@code BiomeModifications} covers biomes only).
 *
 * <p><b>This class is not Forge's builder and must not be mistaken for it.</b> Forge's is seeded
 * from the structure's existing settings, so a caller sees vanilla's spawns already in it; this one
 * starts empty and holds only what {@code modifyStructure} added. Reproducing the seeding — keeping
 * vanilla's entries and its {@code BoundingBoxType}, and defaulting a fresh category to
 * {@code PIECE} — is the reader's job, and is done in {@code FabricStructureSpawns.combine}.
 */
public class ModifiableStructureInfo {

    private ModifiableStructureInfo() {
    }

    /** Mirrors Forge's nested {@code StructureInfo} purely so the nested {@code Builder} path matches. */
    public static final class StructureInfo {

        private StructureInfo() {
        }

        public static class Builder {

            private final StructureSettingsBuilder structureSettings = new StructureSettingsBuilder();

            public StructureSettingsBuilder getStructureSettings() {
                return this.structureSettings;
            }
        }
    }

    public static class StructureSettingsBuilder {

        private final Map<MobCategory, StructureSpawnOverrideBuilder> spawnOverrides =
                new EnumMap<>(MobCategory.class);

        public StructureSpawnOverrideBuilder getOrAddSpawnOverrides(MobCategory category) {
            return this.spawnOverrides.computeIfAbsent(category, c -> new StructureSpawnOverrideBuilder());
        }

        /** Everything {@code modifyStructure} added, for a future Fabric wiring to drain. */
        public Map<MobCategory, StructureSpawnOverrideBuilder> getSpawnOverrides() {
            return this.spawnOverrides;
        }
    }

    public static class StructureSpawnOverrideBuilder {

        private final List<RecordedSpawn> spawns = new ArrayList<>();

        /**
         * The weightless form. From 1.21.5 the weight left {@code SpawnerData} and Forge's builder
         * grew the two-argument overload below; on Fabric both are recorded the same way, with the
         * weight defaulted to 1 here because that is the only information the caller supplies.
         */
        public void addSpawn(MobSpawnSettings.SpawnerData data) {
            this.spawns.add(new RecordedSpawn(data, 1));
        }

        public void addSpawn(MobSpawnSettings.SpawnerData data, int weight) {
            this.spawns.add(new RecordedSpawn(data, weight));
        }

        public List<RecordedSpawn> getSpawns() {
            return this.spawns;
        }
    }

    public record RecordedSpawn(MobSpawnSettings.SpawnerData data, int weight) {
    }
}

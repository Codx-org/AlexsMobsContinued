package com.github.alexthe666.alexsmobs.fabric.world;

import com.github.alexthe666.alexsmobs.fabric.AlexsMobsFabric;
import com.github.alexthe666.alexsmobs.fabric.common.world.ModifiableStructureInfo;
import com.github.alexthe666.alexsmobs.world.AMWorldRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * The Fabric driver for this mod's four structure spawn overrides — mimicubes in end cities, soul
 * vultures at nether fossils, skelewags at shipwrecks, and underminers in anything tagged
 * {@code alexsmobs:spawns_underminers}.
 *
 * <p>Forge and NeoForge get these from a {@code StructureModifier} registered as a datapack entry,
 * which the loader runs over every structure while the dynamic registries are being loaded, handing
 * each one a mutable builder. Fabric has no structure-modification hook of any kind — its
 * {@code BiomeModifications} covers biomes only — so the overrides have to be merged in at read
 * time instead, by {@code mixin/fabric/FabricStructureMixin}.
 *
 * <p><b>The four config conditions are not duplicated here.</b> This class calls the same
 * {@link AMWorldRegistry#modifyStructure} the other two loaders call, against the same vendored
 * recorder ({@link ModifiableStructureInfo}), and reads back what it recorded. That is the one
 * difference from {@code AlexsMobsFP}, which grew a parallel {@code AMStructureSpawns} restating
 * every {@code AMConfig} check — two copies that can disagree. Adding a fifth override to
 * {@code modifyStructure} needs no change in this file.
 *
 * <h2>Matching Forge's merge semantics exactly</h2>
 *
 * <p>Forge's builder is seeded with the structure's existing settings ({@code
 * StructureSettingsBuilder.copyOf}, verified against NeoForge's bytecode), so {@code
 * getOrAddSpawnOverrides} on a category the structure already overrides returns a builder that
 * already holds vanilla's entries and vanilla's {@code BoundingBoxType}, and an {@code addSpawn}
 * appends to it. The vendored recorder starts empty instead — it only ever sees this mod's
 * additions — so the seeding is reproduced here, in {@link #combine}: keep vanilla's entries and
 * vanilla's box type, append ours. For a category the structure does <b>not</b> already override,
 * Forge creates the builder with {@link StructureSpawnOverride.BoundingBoxType#PIECE}, and so does
 * this. That default is worth stating because {@code STRUCTURE} is the intuitive guess and it is
 * the wrong one — it would let these mobs spawn anywhere in a structure's bounding box rather than
 * only inside its pieces.
 */
public final class FabricStructureSpawns {

    /**
     * What {@link AMWorldRegistry#modifyStructure} recorded, per structure. Built once, then only
     * read; the {@code volatile} is what publishes it safely to the chunk-generation threads.
     *
     * <p>Keyed by identity because {@link Structure} does not override {@code equals}, and because
     * the mixin has an instance and no id.
     */
    @Nullable
    private static volatile Map<Structure, Map<MobCategory, List<ModifiableStructureInfo.RecordedSpawn>>> extras;

    /**
     * Merged results, cached per structure. {@code spawnOverrides()} is called for every spawn
     * attempt and from several threads at once, so this is a {@link ConcurrentHashMap} rather than
     * a {@code @Unique} field on the mixin: a plain field would be an unsafely-published
     * {@link EnumMap}.
     */
    private static final Map<Structure, Map<MobCategory, StructureSpawnOverride>> merged =
            new ConcurrentHashMap<>();

    private FabricStructureSpawns() {
    }

    /**
     * Returns {@code vanilla} with this mod's overrides merged in, or {@code vanilla} itself when
     * this structure gets none — which is all but four of them.
     */
    public static Map<MobCategory, StructureSpawnOverride> merge(
            Structure structure, Map<MobCategory, StructureSpawnOverride> vanilla) {
        Map<Structure, Map<MobCategory, List<ModifiableStructureInfo.RecordedSpawn>>> recorded = extras();
        if (recorded == null) {
            return vanilla;
        }
        Map<MobCategory, List<ModifiableStructureInfo.RecordedSpawn>> mine = recorded.get(structure);
        if (mine == null) {
            return vanilla;
        }
        return merged.computeIfAbsent(structure, ignored -> {
            Map<MobCategory, StructureSpawnOverride> result = new EnumMap<>(MobCategory.class);
            result.putAll(vanilla);
            mine.forEach((category, spawns) -> result.put(category, combine(vanilla.get(category), spawns)));
            return result;
        });
    }

    /**
     * Builds lazily on first use rather than from a server-lifecycle event, deliberately.
     *
     * <p>The structure registry has to be loaded and its tags bound before {@code
     * modifyStructure} can be run — one of the four conditions is a tag test — and the only Fabric
     * lifecycle event that fires early enough to beat world generation is {@code SERVER_STARTING},
     * which is documented to run <i>before</i> worlds are loaded. Rather than depend on where in
     * that sequence tag binding lands, this builds on the first {@code spawnOverrides()} call,
     * which cannot happen before something is generating or spawning. The server reference comes
     * from {@link AlexsMobsFabric}, which captures it at {@code SERVER_STARTING}.
     *
     * <p>Returns {@code null} only if a structure is somehow queried with no server up, in which
     * case nothing is cached and the next call tries again.
     */
    @Nullable
    private static Map<Structure, Map<MobCategory, List<ModifiableStructureInfo.RecordedSpawn>>> extras() {
        Map<Structure, Map<MobCategory, List<ModifiableStructureInfo.RecordedSpawn>>> local = extras;
        if (local != null) {
            return local;
        }
        synchronized (FabricStructureSpawns.class) {
            if (extras != null) {
                return extras;
            }
            MinecraftServer server = AlexsMobsFabric.getServer();
            if (server == null) {
                return null;
            }
            return extras = build(server.registryAccess());
        }
    }

    private static Map<Structure, Map<MobCategory, List<ModifiableStructureInfo.RecordedSpawn>>> build(
            RegistryAccess access) {
        Map<Structure, Map<MobCategory, List<ModifiableStructureInfo.RecordedSpawn>>> built =
                new IdentityHashMap<>();
        structures(access).forEach(holder -> {
            ModifiableStructureInfo.StructureInfo.Builder builder =
                    new ModifiableStructureInfo.StructureInfo.Builder();
            AMWorldRegistry.modifyStructure(holder, builder);
            Map<MobCategory, List<ModifiableStructureInfo.RecordedSpawn>> recorded =
                    new EnumMap<>(MobCategory.class);
            builder.getStructureSettings().getSpawnOverrides().forEach((category, spawns) -> {
                if (!spawns.getSpawns().isEmpty()) {
                    recorded.put(category, List.copyOf(spawns.getSpawns()));
                }
            });
            if (!recorded.isEmpty()) {
                built.put(holder.value(), recorded);
            }
        });
        return built;
    }

    private static Stream<Holder.Reference<Structure>> structures(RegistryAccess access) {
        // Registry#holders is gone from 1.21.2 on — Registry started implementing
        // HolderLookup.RegistryLookup, whose listElements() is the same stream. The
        // registryOrThrow -> lookupOrThrow half of that rename is handled tree-wide by the
        // !mc2102-lookuporthrow replacement rule, so only this call needs an arm.
        //? if >=1.21.2 {
        /*return access.registryOrThrow(Registries.STRUCTURE).listElements();
        *///?} else {
        return access.registryOrThrow(Registries.STRUCTURE).holders();
        //?}
    }

    /**
     * One category's merged override. {@code existing} is vanilla's, or {@code null} when the
     * structure does not override this category at all.
     */
    private static StructureSpawnOverride combine(
            @Nullable StructureSpawnOverride existing, List<ModifiableStructureInfo.RecordedSpawn> added) {
        StructureSpawnOverride.BoundingBoxType boundingBox = existing != null
                ? existing.boundingBox()
                : StructureSpawnOverride.BoundingBoxType.PIECE;
        //? if >=1.21.5 {
        /*// From 1.21.5 the weight lives beside the SpawnerData rather than inside it, which is why
        // modifyStructure's >=1.21.5 arms pass it separately and RecordedSpawn carries it.
        net.minecraft.util.random.WeightedList.Builder<MobSpawnSettings.SpawnerData> spawns =
                net.minecraft.util.random.WeightedList.builder();
        if (existing != null) {
            for (net.minecraft.util.random.Weighted<MobSpawnSettings.SpawnerData> weighted : existing.spawns().unwrap()) {
                spawns.add(weighted.value(), weighted.weight());
            }
        }
        for (ModifiableStructureInfo.RecordedSpawn spawn : added) {
            spawns.add(spawn.data(), spawn.weight());
        }
        return new StructureSpawnOverride(boundingBox, spawns.build());
        *///?} else {
        // RecordedSpawn.weight is the recorder's dummy 1 on this era and must not be read: below
        // 1.21.5 the configured weight is already inside the SpawnerData that modifyStructure built.
        List<MobSpawnSettings.SpawnerData> spawns = new ArrayList<>();
        if (existing != null) {
            spawns.addAll(existing.spawns().unwrap());
        }
        for (ModifiableStructureInfo.RecordedSpawn spawn : added) {
            spawns.add(spawn.data());
        }
        return new StructureSpawnOverride(boundingBox,
                net.minecraft.util.random.WeightedRandomList.create(spawns));
        //?}
    }
}

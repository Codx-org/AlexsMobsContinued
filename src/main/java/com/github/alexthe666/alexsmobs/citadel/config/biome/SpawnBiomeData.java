package com.github.alexthe666.alexsmobs.citadel.config.biome;

import com.github.alexthe666.alexsmobs.citadel.Citadel;
import com.google.gson.*;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.biome.Biome;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Deprecated(since = "2.6.2")
public class SpawnBiomeData {

    private List<List<SpawnBiomeEntry>> biomes = new ArrayList<>();

    public SpawnBiomeData() {
    }

    private SpawnBiomeData(SpawnBiomeEntry[][] biomesRead) {
        biomes = new ArrayList<>();
        for (SpawnBiomeEntry[] innerArray : biomesRead) {
            for (SpawnBiomeEntry entry : innerArray) {
                // Gson builds these reflectively — it never calls SpawnBiomeEntry's constructor —
                // so the normalisation there covers the shipped DEFAULTS only, not what is read
                // back out of config/alexsmobs/*.json. On a fresh install that is invisible
                // (the defaults are normalised, then written to disk already normalised), but a
                // player who generated their config on an older build has `forge:` strings on
                // disk that nothing would ever rewrite. Hence the second pass here. See #85.
                if (entry != null && entry.type == BiomeEntryType.BIOME_TAG && entry.value != null) {
                    entry.value = conventionTag(entry.value);
                }
            }
            biomes.add(Arrays.asList(innerArray));
        }
    }

    public SpawnBiomeData addBiomeEntry(BiomeEntryType type, boolean negate, String value, int pool) {
        if (biomes.isEmpty() || biomes.size() < pool + 1) {
            biomes.add(new ArrayList<>());
        }
        biomes.get(pool).add(new SpawnBiomeEntry(type, negate, value));
        return this;
    }

    public boolean matches(@Nullable Holder<Biome> biomeHolder, ResourceLocation registryName) {
        for (List<SpawnBiomeEntry> all : biomes) {
            boolean overall = true;
            for (SpawnBiomeEntry cond : all) {
                if (!cond.matches(biomeHolder, registryName)) {
                    overall = false;
                }
            }
            if (overall) {
                return true;
            }
        }
        return false;
    }

    public static class Deserializer implements JsonDeserializer<SpawnBiomeData>, JsonSerializer<SpawnBiomeData> {

        @Override
        public SpawnBiomeData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonobject = json.getAsJsonObject();
            SpawnBiomeEntry[][] biomesRead = GsonHelper.getAsObject(jsonobject, "biomes", new SpawnBiomeEntry[0][0], context, SpawnBiomeEntry[][].class);
            return new SpawnBiomeData(biomesRead);
        }

        @Override
        public JsonElement serialize(SpawnBiomeData src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject jsonobject = new JsonObject();
            jsonobject.add("biomes", context.serialize(src.biomes));
            return jsonobject;
        }
    }

    // NeoForge 1.20.5 moved the cross-mod convention tags out of `forge:` and into the
    // loader-neutral `c:` namespace, renaming a few of them on the way. Spawn-biome entries name
    // their tag as a plain string — in the shipped defaults and in the user's config file alike —
    // so they are normalised here, at the one point every entry passes through. Without this the
    // `forge:is_*` defaults match nothing on NeoForge and the mobs using them never spawn.
    // Forge 26 made the same move (its own `Tags` is almost entirely `cTag(...)` now, with the same
    // names and the same renames), so this arm covers both loaders from there up.
    //
    // FABRIC TAKES IT ON EVERY NODE (#85). `c:` is and always has been the Fabric convention
    // namespace — nothing on that loader has ever defined a single `forge:` biome tag — so before
    // this arm covered Fabric, EVERY `forge:is_*` default matched nothing there and the ~30 mobs
    // whose spawn entries are keyed on one never spawned at all: hammerhead, mimic octopus, mantis
    // shrimp, orca, guster, mungus, … All 17 Fabric nodes, since Milestone 15 / `2.0.0`.
    // Unlike the loaders' own tags, Fabric's come from an OPTIONAL fabric-api module whose contents
    // vary by build, so the eleven tags these defaults name are also shipped by the mod itself —
    // see DataPackMigration.fabricConventionBackfill. Neither the compiler nor
    // verify_convention_tags.py can see this: the tag names are plain Java strings compared against
    // the tags a biome carries, not data-pack references.
    //
    // THREE OF THEM ARE NOT A RENAME BUT A NARROWING, so they route to a tag this mod defines
    // itself (`data/alexsmobs/tags/worldgen/biome/is_*.json`, each of them `#forge:is_*` — which
    // this same migration turns into `#c:is_*` — plus the members the move dropped):
    //   is_snowy     lost frozen_ocean + frozen_river  (both loaders' `c:`, and fabric-api's)
    //   is_plains    lost snowy_plains + meadow
    //   is_wasteland went from [snowy_plains] to EMPTY on NeoForge/Forge 26 (the mod's own Fabric
    //                backfill already copies Forge's definition, so Fabric never lost it)
    // Read out of the Forge 1.20.1, NeoForge 26.2 and fabric-convention-tags-v2 jars. Between them
    // that is eight spawn pools across seven mobs, three of which had NO vanilla biome left:
    // the gelada monkey (is_plains AND is_plateau -> meadow), the tusklin's snowy-plains pool and
    // the moose's. Same shape as #85 and just as invisible — a narrowed tag still resolves, so
    // nothing logs and every gate stays green.
    //? if (neoforge && >=1.20.5) || (forge && >=26) || fabric {
    /*private static String conventionTag(String value) {
        if (value.startsWith("c:")) {
            // A config file written by 2.0.14 or earlier already holds the `c:` spelling — this
            // method rewrote `forge:is_snowy` to `c:is_snowy` back then, and that is what landed
            // on disk. `c:` is not `forge:`, so the alias routing below never sees it again and
            // #89 stays broken for every instance that is not generating its config from scratch
            // — which is most of them, since config/ survives a new world. Re-route here too.
            // Only the three narrowed tags; every other `c:` value is already correct.
            String cPath = value.substring("c:".length());
            return switch (cPath) {
                case "is_snowy", "is_plains", "is_wasteland" -> "alexsmobs:" + cPath;
                default -> value;
            };
        }
        if (!value.startsWith("forge:")) {
            return value;
        }
        String path = value.substring("forge:".length());
        return switch (path) {
            case "is_dense/overworld" -> "c:is_dense_vegetation/overworld";
            case "is_coniferous" -> "c:is_tree/coniferous";
            case "is_snowy", "is_plains", "is_wasteland" -> "alexsmobs:" + path;
            default -> "c:" + path;
        };
    }
    *///?} else {
    private static String conventionTag(String value) {
        return value;
    }
    //?}

    private class SpawnBiomeEntry {
        BiomeEntryType type;
        boolean negate;
        String value;

        public SpawnBiomeEntry(BiomeEntryType type, boolean remove, String value) {
            this.type = type;
            this.negate = remove;
            this.value = type == BiomeEntryType.BIOME_TAG ? conventionTag(value) : value;
        }

        public boolean matches(@Nullable Holder<Biome> biomeHolder, ResourceLocation registryName) {
            if (type.isDepreciated()) {
                Citadel.LOGGER.warn("biome config: BIOME_DICT and BIOME_CATEGORY are no longer valid in 1.19+. Please use BIOME_TAG instead.");
                return false;
            } else {
                if (type == BiomeEntryType.BIOME_TAG) {
                    if (biomeHolder.tags().anyMatch((biomeTagKey -> biomeTagKey.location() != null && biomeTagKey.location().toString().equals(value)))) {
                        return !negate;
                    }
                    return negate;
                } else {
                    if (registryName.toString().equals(value)) {
                        return !negate;
                    }
                    return negate;
                }
            }
        }
    }
}
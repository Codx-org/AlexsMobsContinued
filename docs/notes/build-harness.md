# Build harness

> Read when touching Gradle, the node tree, buildscripts, manifests, or the data-pack/asset migration passes.
>
> Part of the Alex's Mobs Continued porting notes.

## Build system

Copied and adapted from **codxlib**'s harness (the proven one in this workspace):

- `settings.gradle.kts` — declares the Stonecutter node tree. Each node is
  `version("<mc>-<loader>", "<mc>")`; Forge nodes get `buildscript = "build.forgeg.gradle.kts"`,
  NeoForge nodes `build.neoforge.gradle.kts`. `gradle.beforeProject` sets
  `loom.platform = forge` for `*-forge` nodes.
- `stonecutter.properties.toml` — **single source of truth** for mod identity + every node's
  dependency pins (`[forge."<mc>"]` / `[neoforge."<mc>"]`).
- `build-logic/` — the `mod-platform` convention plugin (verbatim from codxlib). It
  **generates** the loader manifest (`mods.toml` / `neoforge.mods.toml`) into
  `build/generated/modManifest` from the `platform { dependencies { … } }` DSL, expands
  `${pack_meta}` in `pack.mcmeta` and `${java}` in `*.mixins.json`, and sets the Forge
  `MixinConfigs` jar-manifest attribute.
- **Forge builds via Architectury Loom** (`dev.architectury:architectury-loom:1.17-SNAPSHOT`,
  forced in `settings.gradle.kts`), *not* ForgeGradle — FG6 does not work on Gradle 9.
  **NeoForge builds via MDG** (`net.neoforged.moddev`).


> **Upstream's own build files** (`build.gradle`, `settings.gradle`, `mods.toml`) are preserved
> for reference in `docs/upstream-build/` (`git mv`'d, not deleted — `rm` is sandbox-blocked).
> The manifest is now generated, so keeping the original in `META-INF/` would duplicate the
> resource. The generated one drops upstream's cosmetic `[modproperties.alexsmobs]
> `catalogueItemIcon` (Catalogue-mod only) — accepted.

### The `minecraft` versionRange must be an EXACT range (Modrinth upload)

Both buildscripts declare `required("minecraft") { forgeLikeVersionRange = exactMcRange(prop("deps.minecraft")) }`
(`exactMcRange` is in `ModPlatformPlugin.kt`). Two separate traps, both fixed there:

1. **The brackets are load-bearing.** A **bare** `"1.20.1"` is a Maven *soft* requirement, which
   Forge/NeoForge read as `[1.20.1,)` — so the jar claims to run on every later MC, and Modrinth's
   upload auto-detect cannot pin a game version. `[1.20.1]` pins exactly one.
2. **The version must have three components.** Modrinth's inference
   (`modrinth/code`, `apps/frontend/src/helpers/infer/version-ranges.ts`) rewrites the Maven `[X]`
   into the **semver range** `X` and feeds it to node-semver's `satisfies`. A two-component range
   like `1.21` is a semver **X-range** meaning `1.21.x`, so `[1.21]` preselected 1.21 *and*
   1.21.1 … 1.21.11 on upload. `exactMcRange` pads to `[1.21.0]`, which is an exact semver version
   and matches only MC 1.21. Three-component nodes (`1.20.1`, `1.21.1`, …) were never affected —
   semver compares component-wise, so `1.21.1` does not match `1.21.11`.

   Padding is safe for the loaders: Maven's `ComparableVersion` normalises trailing zero
   components, so `1.21` and `1.21.0` compare equal. Verified — `:1.21-forge:runServer` reaches
   `Done` with `[1.21.0]`. It matters again for any future two-component MC (`26.1`, `26.2`).

Safe because no node declares `[publish] additionalVersions` — each node maps to exactly one MC
version. Verify after any manifest change:

```bash
unzip -p versions/<node>/build/libs/<jar> META-INF/{neoforge.,}mods.toml | grep -A3 'modId = "minecraft"'
```

Build upload jars with `MOD_IS_RELEASE=true` or they carry a `-SNAPSHOT` suffix (`Context.isSnapshot`).

### Commands

```bash
./gradlew ":1.20.1-forge:compileJava"     # fastest per-node feedback
./gradlew ":1.20.1-forge:build"           # jar for one node
./gradlew ":1.20.1-forge:runClient"       # dev client (needs a GPU)
./gradlew ":1.20.1-forge:runServer"
```

**Multi-node builds must be ONE Gradle invocation** — `./gradlew :a:build :b:build --continue`.
Back-to-back separate `./gradlew` calls collide on the single-use daemon and Stonecutter's
active-version state and fail spuriously. (Learned on OneBlock's 58-node tree.)

`stonecutter.gradle.kts` holds `stonecutter active "1.20.1-forge"` — the node whose sources
live in the root `src/`. All other nodes get root `src/` **projected into**
`versions/<node>/src/` by the `stonecutterGenerate` task.

> ⚠️ **Configuration-time file paths.** In a node buildscript, `file("src/…")` resolves against
> `versions/<node>/`, which does not exist for the active node and is not yet generated for the
> others when loom/MDG configure. Anything read at configuration time (e.g. the access
> transformer) must fall back to `rootProject.file(…)` — see `build.forgeg.gradle.kts`.

### A repo can serve a ZERO-BYTE `maven-metadata.xml`, and that fails resolution outright

The `2.0.15` release build failed on exactly two nodes — `1.20.4-neoforge` and `1.20.6-neoforge` —
with `Could not resolve org.apache.logging.log4j:log4j-api:2.11.+` … `Premature end of file`. It
looks like the flaky-worker failure that rule 1 covers and it is **not**: it reproduces on a retry
of just those two nodes.

`https://maven.neoforged.net/releases/org/apache/logging/log4j/log4j-api/maven-metadata.xml`
returns **HTTP 200 with a zero-byte body** (a genuinely missing path there answers 404 — checked
side by side with curl). Those two nodes are the only ones that pull `net.minecraftforge:unsafe:
0.2.0` (via `neoforge` 20.4.251 / 20.6.139), and it asks for the **dynamic** version `2.11.+`.
Listing a dynamic version queries *every* repository, Gradle cannot parse the empty XML, and it
**aborts the whole resolution** rather than falling through to Maven Central, which has the
metadata. It only surfaced now because Gradle caches dynamic-version listings for 24h, so the
earlier `2.0.14` build was answering from cache.

Fixed in `build.neoforge.gradle.kts` by taking log4j off that repo's menu:

```kotlin
withType<MavenArtifactRepository>().configureEach {
    if (url.toString().contains("maven.neoforged.net")) {
        content { excludeGroupByRegex("org\\.apache\\.logging\\.log4j.*") }
    }
}
```

It has to be `configureEach` rather than a filter on a declaration: the moddev plugin adds the
NeoForged repo itself, so there is no declaration here to attach `content { }` to.

⚠️ **Generalisation: a 200 is not a success.** A repo that answers a missing path with an empty
body poisons any *dynamic* version request that touches it, and the error names the artifact and
the version range — never the repo that broke it. When a dependency that has always resolved
suddenly cannot, curl each candidate repo's metadata URL and look at the **size**, not the status.

### JEI is not available for every node

`compat/jei/**` (3 self-contained files, reached only through JEI's own `@JeiPlugin` classpath scan —
nothing in the mod references them) compiles against the JEI API. JEI **published nothing at all for
1.21.2 or 1.21.3**, and **stopped publishing a Forge flavour after 1.21.1** (its maven jumps
`1.21.1` → `1.21.4`, neoforge/fabric only). So the pin is optional: a node with no `deps.jei` in
`stonecutter.properties.toml` gets `compat/jei/**` excluded from the compile by
`ModPlatformPlugin.configureJava`, and both buildscripts skip the dependency (`propOrNull`, which is
`prop` without the throw-on-missing). Nothing else changes — the mod does not declare JEI in its
manifest either way.

### CodxLib is a real dependency now, and its jar form differs per buildscript

From `2.0.15` the mod requires **CodxLib** (`codx:codxlib:<ver>-<loader>+<mc>`, resolved from
`mavenLocal()` in dev — `cd codxlib && python3 scripts/install_maven_local.py` installs all 58
artifacts). All five buildscripts declare it required in the manifest with a floor of `[1.4.0,)` /
`>=1.4.0`, because `codx.codxlib.api.settings` — the settings framework `config/ConfigHolder.java`
is built on — did not exist before that.

The **dependency configuration is not the same in all five**, and getting it wrong is a resolution
error rather than something subtle:

| buildscript | toolchain | configuration |
|---|---|---|
| `build.fabric.gradle.kts` | Architectury Loom | `modImplementation` |
| `build.forgeg.gradle.kts` | Architectury Loom | `modImplementation` |
| `build.fabricnr.gradle.kts` | loom **no-remap** (26.x) | `implementation` |
| `build.forgenr.gradle.kts` | loom **no-remap** (26.x) | `implementation` |
| `build.neoforge.gradle.kts` | NeoForge moddev | `implementation` |

⚠️ **`fg.deobf(...)` is wrong here.** AMC's Forge nodes are on Architectury Loom, **not**
ForgeGradle — `build.forgeg.gradle.kts` calls `mappings(loom.officialMojangMappings())` and uses
`modCompileOnly` for JEI. Loom has no `fg` extension at all.

⚠️ **Rebuilding CodxLib without bumping its version leaves a STALE remapped jar on the
`modImplementation` nodes only.** Loom caches its remap output under
`.gradle/loom-cache/remapped_mods/remapped/codx/` and
`versions/<node>/build/loom-cache/remapped_working/`, keyed by the artifact coordinate — so
re-running `install_maven_local.py` at the *same* version silently changes nothing there, while the
plain-`implementation` nodes pick the new jar up immediately. The symptom is a compile that fails on
**exactly** the remapping nodes with `cannot be applied to given types` / `cannot find symbol`
against a method you can see with `javap` in `~/.m2`. Cost a full 7-node round: 4 nodes green, 3
red, all three on the new API. Fix: `mv` those caches out of the tree (`rm` is sandbox-blocked) and
re-run —

```bash
mv .gradle/loom-cache/remapped_mods/remapped/codx /tmp/amc-trash/
mv versions/*/build/loom-cache/remapped_working/remapped.codx-codxlib-*.jar /tmp/amc-trash/
```

— or bump `mod.version` in codxlib, which is what a real release does anyway.

### The Fabric access widener is generated, and its template lives OUTSIDE `src/`

`build-logic/src/main/kotlin/AccessWidener.kt` expands the single predicated template
`accesswidener/alexsmobs.accesswidener` into `build/generated/accessWidener/` per node; both Fabric
buildscripts call `generateAccessWidener(prop("mod.fabric.access_widener"))` and the function
registers its own output dir as a resource root, because loom **rewrites** an existing widener entry
in the jar and fails if it is missing. Rationale, era table and the two traps are in
[`fabric.md`](fabric.md#the-access-widener-cannot-be-one-file-and-cannot-be-stonecuttered).

⚠️ The headline for this file: **Stonecutter DOES preprocess `.accesswidener`** — it registers the
extension as a `#`-comment type — so a template with `#?` directives kept anywhere under `src/` fails
at `stonecutterPrepare` with `Extraneous input '{'`. That is the opposite of `.json`, which it leaves
alone (below), and it means "is this file preprocessed?" has to be answered per extension, not
assumed.

### Data-pack migration is a build step, not source conditionals

**Stonecutter does not preprocess `.json`** — verified empirically: a `//? if >=1.20.5` block put
into a recipe was copied through by `stonecutterGenerate` byte-for-byte, and vanilla parses
data-pack JSON with a strict reader that rejects `//`. So every era-dependent data shape is
rewritten at build time by **`build-logic/src/main/kotlin/DataPackMigration.kt`**, hooked into
`processResources.doLast` from `ModPlatformPlugin.configureProcessResources`. There are a dozen of them
now; read the `doLast` chain in `configureProcessResources` for the authoritative list and its gates.
The two that carry the most logic:

- **`migrateTo1205`** (`>=1.20.5`, ~194 files/node) — recipe `result` and advancement
  `display.icon` from `{"item":…,"nbt":"<snbt>"}` to `{"id":…,"components":{…}}`; bare-string
  cooking results into `{"id": …}`; loot `set_nbt`→`set_custom_data` (or `set_potion` when the
  only tag is `Potion`), `copy_nbt`→`copy_custom_data`, nested-table entry `name`→`value`,
  `minecraft:scute`→`minecraft:turtle_scute`. It carries a small SNBT→JSON reader because
  `CustomData.CODEC` is `CompoundTag.CODEC.xmap(…)` (an object), while `set_custom_data` still
  takes SNBT (`TagParser.LENIENT_CODEC`) and is therefore left as a string.
- **`migrateNeoForge`** (every NeoForge node) — see below.

The **asset** passes are the ones that keep shipping bugs, because a bad asset reference is
logged-not-thrown and no gate opens a screen: `writeItemModelDefinitions` (`>=1.21.4`, 305 files — report
#10) and **`retemplateSpawnEggs`** (`>=1.21.5`, 89 files — report #17, where the definition written by the
*first* pass was correct but pointed at a parent 1.21.5 had deleted). Each prints a count; treat a
count of 0 as a failure, not a no-op, and **predict the number before running it** — both have a
known-correct count, and #21 was found only because 9 was predicted and 59 was what the tree actually
contained. A third pass, **`restaticAdvancementIcons`** (`>=1.21.4`, 59 files — report #21), was
**deleted in the #45 implementation (2026-08-08)**: `AMIconSpecialRenderer` draws the 59 advancement
icons live again on ≥1.21.4, reading the very `custom_data` that pass used to strip — do not re-add
it. The three icon items (`tab_icon`/`fancy_item`/`effect_item`, `LIVE_ICON_ITEMS` in
DataPackMigration) instead get a `minecraft:special` definition and an emptied `{}` base model from
`writeItemModelDefinitions`.

Two more passes joined the chain in the thirteenth bug-report pass, and neither fits the
"reshape a JSON that a version renamed" mould the rest of them share:

- **`ghostifyPickaxeTexture`** (`>=1.21.4 || fabric`, i.e. 37 of 49 nodes — report #69) is the first
  pass that edits a **PNG**. The ghostly pickaxe's see-through look is a *render type*, not a texture,
  and the only thing that ever selected it is a Forge `BakedModelWrapper` that exists on
  `<1.21.4 && !fabric`; everywhere else the tool draws solid, so the alpha is lowered to 140 here
  instead. The gate mirrors the wrapper's exactly, and the 12 wrapper-bearing nodes must be left
  alone — additive blending multiplies by source alpha, so lowering it there only dims upstream's own
  look. ⚠️ Read the PNG into a `TYPE_INT_ARGB` raster first: `setRGB` on the `TYPE_3BYTE_BGR` image
  `ImageIO` hands back for an opaque file silently drops the alpha byte, writes the file back
  unchanged and reports success.
- **`migrateDirtTagTo261`** (`>=26.1`, expect exactly 1 file — report #70) re-joins the three-way
  split MC 26.1 made of `#minecraft:dirt`. It is a migration rather than three more lines in
  `am_spawns.json` because **a tag reference to a tag that does not exist is a hard load error**, and
  `#minecraft:grass_blocks` / `#minecraft:moss_blocks` do not exist below 26.1.

⚠️ **A vanilla *tag* can be re-partitioned with no code change at all**, and the result is a silently
narrower predicate — no error, no log line, nothing for a gate to catch. #70 cost fourteen mob spawn
tags plus an item placement on the 26.x nodes. On every MC bump, diff the membership of every
`#minecraft:` tag the data pack references between the old and new **`~/.gradle/caches/fabric-loom/
<mc>/minecraft-client.jar`** — the merged artifacts under `minecraftMaven/` are code-only deobf jars
with no `data/` entries, so a lookup there returns nothing and reads as "the tag is gone".

⚠️ The passes write to the *same* group of files in sequence, so **order inside the `doLast` chain
is load-bearing**: `migrateTo1205` reshapes every advancement icon to `{"id":…,"components":{…}}`
long before the `>=1.21.4` block runs — the shape the live icon renderer now reads at runtime.

Because these passes mutate `processResources`' own output directory, Gradle sees the task as
out-of-date next run and re-copies. If you change the migration logic, **clear
`versions/<node>/build/resources` first** (`mv` it away — `rm` is sandbox-blocked) or stale
relocated files linger in the jar.

⚠️ **Clear it for ALL 49 nodes, not just the ones the change targets.** Editing anything under
`build-logic/` changes the plugin classpath, which makes `processResources` out-of-date on *every*
node — including the ones the new pass does not touch — so they all re-run their `doLast` chain over
a directory that is already migrated. Most passes happen to be idempotent; `migrateTo1205` is not
obviously so, and finding out the hard way costs a full 49-node rebuild anyway. One loop over
`versions/*/build/resources` before the build is cheaper than reasoning about which passes commute.

### NeoForge data namespaces (bit us on 1.20.4 too, silently)

The source tree is authored Forge-side. On a NeoForge node three things must be re-pointed, and
none of them logs anything when wrong — they just do nothing:

| Forge | NeoForge | When |
|---|---|---|
| `data/<ns>/forge/{biome_modifier,structure_modifier}/` | `data/<ns>/neoforge/…` | always |
| `data/forge/loot_modifiers/global_loot_modifiers.json` | `data/neoforge/loot_modifiers/…` | always |
| `"condition": "forge:loot_table_id"` | `"neoforge:loot_table_id"` | always |
| `data/forge/tags/**` + every `forge:<tag>` reference | `data/c/tags/**`, `c:<tag>` | **>= 1.20.5** |

The first three were wrong on **1.20.4-neoforge as well** — so that node shipped with **no mob
spawning and no global loot modifiers** until this was fixed. Renames on the way into `c:`:
`sand`→`sands`, `string`→`strings`, `glass`→`glass_blocks`, `gravel`→`gravels`,
`is_dense/overworld`→`is_dense_vegetation/overworld`, `is_coniferous`→`is_tree/coniferous`
(everything else keeps its path; so do this mod's own convention tags — `heart`, `armors/*`,
`crops/rice`, … — which simply follow their definition into `c:`).

`DefaultBiomes` names ~15 `forge:is_*` biome tags as **plain strings**, in the shipped defaults
*and* in the user's saved config, so the data-pack pass cannot reach them. They are normalised in
Java instead, at the one point every entry passes through:
`citadel/config/biome/SpawnBiomeData$SpawnBiomeEntry`'s constructor calls a Stonecutter-gated
`conventionTag(String)`. Miss this and every `forge:`-tagged spawn entry matches nothing on
NeoForge 1.20.5+.

### 26.x is UNOBFUSCATED, so Forge needs a third buildscript (`build.forgenr.gradle.kts`)

From MC 26.1 the game ships with Mojang names at runtime — there is no SRG namespace, no mappings
tree and nothing for `remapJar` to do. Plain `dev.architectury.loom` still demands `mappings(...)`
and tries to remap, so the Forge 26 node uses **`dev.architectury.loom-no-remap`** via a new
**`build.forgenr.gradle.kts`** (adapted from OneBlock's, which is where each workaround was first
paid for), declared in `settings.gradle.kts` by a `forgeNoRemap(version)` helper. Three consequences:

- The node id stays `26.1.2-forge`, so every `//? if forge` gate in the shared source applies
  unchanged.
- It reads **`accesstransformer_mojmap.cfg`** — the same file the NeoForge nodes read — not the SRG
  `accesstransformer.cfg` the classic Forge nodes read.
- `ModPlatformPlugin` resolves the jar task **lazily** (`remapJar` if it exists, else `jar`), because
  on the no-remap variant those tasks are never created.

Java is **25** here (`Context.javaVersion` switches at `>=26`), and `pack_format` is `101`.

Pins: Forge `64.0.12`, NeoForge `26.1.2.87`.

#### ⚠️ On loom-no-remap + Forge 26, NO MIXIN APPLIES IN A DEV RUN unless you pass `-mixin.config`

The single most expensive thing about this node, and it is invisible until something mixin-dependent
misbehaves. Two facts compound:

- `loom { forge { mixinConfig("…") } }` **does not** reach the dev run's program args on
  `loom-no-remap` the way it does on classic loom.
- **Forge 26 (64.x) has no mixin-config discovery of its own at all** — grepped: there is no
  `mixin.config` / `MixinConfigs` handling anywhere in fmlloader, fmlcore, forgespi,
  forge-transformers, javafmllanguage or the universal/userdev jars. Only
  `dev.architectury:mixin-patched` handles them, off the `MixinConfigs` MANIFEST attribute (which the
  **built jar** carries, from `ModPlatformPlugin`) and the `-mixin.config` ModLauncher argument.

The exploded dev output has neither, so **every mixin silently did nothing** in `runServer`/`runClient`
while the built jar was fine. It surfaced as four `RegisterEvent` NPEs during boot —
`NullPointerException: Block id not set` out of `BlockBehaviour.<init>`, `Item id not set` out of
`SpawnEggItem.<init>`, and a cascading `Registry Object not present` — i.e. `BlockPropertiesMixin` /
`ItemPropertiesMixin` (which stamp `RegistrationContext.CURRENT_ID` onto the 26-mandatory
`Properties.setId`) simply not being there. Nothing in the log mentions mixins.

Fixed in `build.forgenr.gradle.kts` with `runs.configureEach { programArgs("-mixin.config",
"${prop("mod.id")}.mixins.json") }`.

> **The one-line diagnostic for "did any mixin apply?" on a dev run**: grep the log's
> `ModLauncher running: args [...]` line for `-mixin.config, <modId>.mixins.json`. A classic Forge
> node has it; the broken 26 node did not. Check this first whenever a dev run misbehaves in a way
> the built jar does not.


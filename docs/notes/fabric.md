# Fabric — Milestone 15 and the original estimate

> Read when working on a Fabric node. All 17 boot, and since *Wave 5* they react to everything the
> Forge/NeoForge line does — what is left is the deliberate-divergence table near the bottom.
>
> Part of the Alex's Mobs Continued porting notes.

### 🚧 Milestone 15 IN PROGRESS — Fabric, starting at `26.2`

Started 2026-07-30 on the user's "can you add fabric?". Two decisions the user made up front, both
binding: **this repo (AMC) owns the Fabric jars** (the sibling `AlexsMobsFP` becomes a reference/archive,
not a co-publisher on the shared Modrinth slug), and the **first Fabric target is MC 26.2**, back-filling
downward from there — which is the ordering the pre-start estimate at the end of this file already recommends, because loader deltas
are stable across MC versions and downward is the cheap direction.

**All 17 Fabric nodes (MC 1.20.1 → 26.2) now compile**, bringing the tree to **49 nodes**, and
**Waves 1–5 are all closed** — the "does not work yet" list below is now struck through end to end.
`mod.version` is still `1.0.8` and **nothing Fabric has been published**; what remains before shipping
is a release decision, not a feature. The behaviours that stay different on purpose are in the
divergence table under
["Fabric-only behaviour divergences"](#fabric-only-behaviour-divergences-and-where-each-is-written-down).

The five-step gate table immediately below is **historical** — it was run when `26.2-fabric` was the
only Fabric node and the tree had 33. The current all-49 figures are at the end of each wave section.

| Step | Result (33-node tree, `26.2-fabric` only) |
|---|---|
| 33-node `:build` (`MOD_IS_RELEASE=true`) | ✅ `BUILD SUCCESSFUL in 4m 33s` |
| `verify_mixins.py` | ✅ `problems=0` (`26.2-fabric declared=9 missing=0 self-targeted=0`) |
| `verify_mixin_targets.py` | ✅ `jars=33 selectors=297 problems=0` |
| `SOAK=45 scripts/bootgate.sh` × **33** | ✅ **all 33 `DONE`, `rc=0`**, no crash-reports, no non-benign log lines |
| `scripts/clientgate.sh` × **33** | ✅ all 33 reach `Sound engine started`, `rc=0`, no crash-reports |
| `verify_assets.py` | ✅ `asset literals=394 missing=0` |

The 33-node boot gate is the **regression** half: adding Fabric meant reordering the registry flushes in
`AlexsMobs`' shared constructor (see below), which is inert on Forge/NeoForge only in theory. All 32
established nodes still boot clean.

⚠️ `26.2-fabric declared=9` where `26.2-forge`/`26.2-neoforge` declare 13 is **not a gap** — Fletching
Table duplicates the whole `client` list into `mixins` on the Forge-like loaders (see "Fletching Table
writes the `mixins` array" in [`mixins.md`](mixins.md)), and Fabric's split — 3 common, 6 client — is
the correct one. `verify_mixins.py` is green on all three.

#### The harness

- **`settings.gradle.kts`** gains a `fabricNoRemap(version)` helper, mirroring the `forgeNoRemap` one
  Milestone 13 added: 26.x ships unobfuscated, so there is nothing to remap and the node uses
  **`build.fabricnr.gradle.kts`** (arch-loom's no-remap variant). There is deliberately **no**
  `build.fabric.gradle.kts` — a back-filled node below 26.1 will need one, and that is the point at
  which remapping comes back.
- Pins live in `stonecutter.properties.toml` under `[fabric."26.2"]`. Note **`deps.fabric-loader-min`**
  is separate from the build-time `deps.fabric-loader` pin — this is the codxlib `1.3.3` / OneBlock
  `3.3.0` crash, pre-empted: the shipped `fabricloader` range must be **per-MC-version**, never the
  build pin. Read the value out of the pinned fabric-api jar's own `fabric.mod.json`
  (`unzip -p <jar> fabric.mod.json` → `depends.fabricloader`); they are **non-monotonic**, don't guess.
- **The access widener is GENERATED per node** from one predicated template —
  **`accesswidener/alexsmobs.accesswidener`** (repo root) expanded by
  **`build-logic/AccessWidener.kt`** into `build/generated/accessWidener/`, which is what loom
  validates and remaps and what `processResources` puts in the jar. See
  ["The access widener cannot be one file, and cannot be Stonecuttered"](#the-access-widener-cannot-be-one-file-and-cannot-be-stonecuttered)
  below for why. Pre-flight it against every MC version with
  `scripts/aw_check.py accesswidener/alexsmobs.accesswidener <mc>…` before adding a node.

#### The access widener cannot be one file, and cannot be Stonecuttered

Two independent things diverge across the seventeen Fabric nodes, and the AW format punishes both far
harder than the Forge AT does. **An AT entry naming an absent member is a silent no-op** — which is why
`accesstransformer_mojmap.cfg` can list both eras of a moved member and let every node ignore the half
that does not apply. **An AW entry naming an absent member is a hard error**: `validateAccessWidener`
refuses the build.

What diverges:

1. **the header namespace** — `official` on the unobfuscated 26.x nodes (loom-no-remap has no mappings
   tree, so it validates against the game's own names and rejects `named`), `named` below, where classic
   loom remaps named → intermediary on the way into the jar and rejects `official`;
2. **eight entries**, measured against the unpatched Mojmap jars rather than guessed:

   | entry | changes at |
   |---|---|
   | `BlockBehaviour#drops` | `ResourceLocation` → `ResourceKey` at **1.20.5** → `Optional` at **1.21.2** |
   | `Mob#getLootTable` (×2 lines, `accessible` + `extendable`) | tracks `drops`, version for version |
   | `Camera#move` | `(DDD)V` → `(FFF)V` at **1.21** |
   | `EnderDragon#reallyHurt` | `(DamageSource;F)Z` → `(ServerLevel;DamageSource;F)V` at **1.21.2** |
   | `AbstractArrow#setPierceLevel` | `…projectile` → `…projectile.arrow` at **1.21.11** |
   | `SpawnPlacements#register` | `SpawnPlacements$Type` → `SpawnPlacementType` at **1.20.5** |
   | `BlockEntityType#<init>` | drops the trailing datafixer `Type` at **1.21.2** |
   | the four Tabula `…$Deserializer` ctors | `<26` only |

That is six near-identical files if maintained by hand. Instead there is **one template with `#?`
predicates**, expanded per node.

⚠️ **Two traps, both hit while building this:**

- **The template must NOT live under `src/`.** Stonecutter *does* register `.accesswidener` as a
  `#`-comment file type and preprocesses the whole source tree, so a template kept there dies at
  `stonecutterPrepare` with `Extraneous input '{'` — long before any buildscript runs. (The pre-existing
  note in the old widener claiming the format "is NOT preprocessed by Stonecutter" was simply never
  tested.) Hence a plain root-level `accesswidener/` directory that Stonecutter never walks.
- **Native Stonecutter gating would not work anyway.** Its inactive arms come back as `#`-prefixed
  lines, and a widener's header must be the file's **literal first line** — so the one thing that most
  needs gating is the one thing it cannot gate.

`scripts/aw_check.py` understands the same `#?` directives, so it checks each MC version against exactly
the entries that version would be given, and prints the descriptor it **did** find so a moved member gets
a new gated arm rather than a deletion. Its expression evaluator is a hand-written twin of
`evalVersionExpr` in `AccessWidener.kt` (which delegates each *atom* to Stonecutter so version ordering
is decided in one place) — **keep the two in step**. Baseline as of the back-fill: `problems=0` on all
fifteen obfuscated versions.

#### The seam: a relocated compat namespace, NOT a `net.minecraftforge.**` shim

`AlexsMobsFP` shimmed the real Forge package names, and its own roadmap records why that is a dead end
here: `:neoforge:compileJava` fails there because the shim collides with the genuine Forge classes. In
this tree Forge and NeoForge are first-class, so the Fabric replacements live under
**`com.github.alexthe666.alexsmobs.fabric.**` keeping the same simple names**, reached by **23 one-line
`!fab-*` `replacements` rules** keyed on the **fully-qualified** Forge name. 23 files so far:
`registries/DeferredRegister`, `ModBus`, `event/AMEvent(Bus)`, `entity/PartEntity`,
`entity/EntityAttributeCreationEvent`, `config/ForgeConfigSpec`, `items/IItemHandler(Helper)`,
`common/Tags`, `common/brewing/*`, `common/loot/IGlobalLootModifier`, `common/world/*`,
`server/ServerLifecycleHooks`, `client/*`, and the two entrypoints.

`ModPlatformPlugin.kt:194` excludes `**/alexsmobs/fabric/**` from the compile on every non-Fabric node
(`net.fabricmc.**` is simply absent from their classpath). ⚠️ **So a *shared* file that references one of
those classes must spell it fully qualified inside a `fabric` gate — never as an import**, or the 32
non-Fabric nodes stop compiling.

#### ⚠️ On Fabric, the flush ORDER in `AlexsMobs`'s constructor is load-bearing

The one behavioural difference that has actually bitten, and it will bite again on every back-filled node.
Fabric has **no deferred-registration API**, so `fabric/registries/DeferredRegister.register(ModBus)`
**runs the suppliers on the spot**. On Forge/NeoForge the identical call merely *subscribes* and the
loader picks registry order — so the constructor's line order is inert there and load-bearing here.

It failed as:

```
IllegalStateException: Used ender_flu before its registry was flushed — check the flush order
  in the AlexsMobs constructor
  at …fabric.registries.DeferredRegister$Entry.get(DeferredRegister.java:153)
  at …item.AMItemRegistry.lambda$static$102(AMItemRegistry.java:195)   ← the cosmic cod's food component
  at …AlexsMobs.<init>(AlexsMobs.java:210)
```

i.e. the cosmic cod's food component dereferences `AMEffectRegistry.ENDER_FLU` **inside the item's own
supplier**. Fixed by flushing **effects, potions and sounds before items**, with the reason written at the
site. That deliberate error message — rather than a null — is the whole reason the fault was one grep from
diagnosed; keep it.

Before reordering, every cross-registry reference was mapped to prove there is no cycle: only
`AMItemRegistry → AMEffectRegistry` is eager. `AMEffectRegistry → AMItemRegistry` is brewing-only
(`registerBrewingRecipes`, run later), `AMBlockRegistry → AMItemRegistry` is a *registration* not a get,
and `AMCreativeTabRegistry → AMItemRegistry` sits inside lazy lambdas. **Re-run that check before
touching the order again.**

#### ⚠️ Fabric calls by hand what Forge fires as events — so a version gate can become a DOUBLE call

Same family as the flush order above, and it survived every static gate. `AMEntityRegistry` has a
`//? if <1.20.5` line inside `initializeAttributes` calling `registerSpawnPlacements()`: below 1.20.5
Forge has no `SpawnPlacementRegisterEvent`, so upstream piggybacks the placements on the attribute event.
`AlexsMobs.<init>`'s `fabric` arm then calls **both** `initializeAttributes(...)` and
`registerSpawnPlacements()` explicitly, because Fabric has no event bus — so on Fabric `<1.20.5` the
placements registered **twice**, and vanilla 1.20.1's `SpawnPlacements.register` throws rather than
overwriting:

```
IllegalStateException: Duplicate registration for type alexsmobs:grizzly_bear
  at net.minecraft.world.entity.SpawnPlacements.register(SpawnPlacements.java:67)
  at …AMEntityRegistry.registerSpawnPlacements(AMEntityRegistry.java:246)
  at …AlexsMobs.<init>(AlexsMobs.java:283)
```

Fixed by re-gating that line to `<1.20.5 && !fabric`. **The general rule: every `//? if <version>` line
that exists because "Forge fires this from event X" needs `&& !fabric` the moment the Fabric entrypoint
calls the same thing directly.** Both calls are valid Java, so the compiler is silent; only a boot gate
finds it, and only on the nodes where the version gate is live.

#### ⚠️ Fabric's client entrypoint runs INSIDE `Minecraft.<init>`, so half the client is still null

Third of the family, and the one the **client** gate found. `ClientModInitializer#onInitializeClient`
is not Fabric's answer to `FMLClientSetupEvent` — Forge fires that event well after `Minecraft`'s
constructor returns, whereas Fabric calls the entrypoint **from inside** the constructor
(`Minecraft.<init>` → `Hooks.startClient`, `Minecraft.java:458` on 1.20.1). Every field assigned
after that line is still `null` when the mod's client init runs:

```
NullPointerException: Cannot read field "fixedBuffers" because the return value of
  "net.minecraft.client.Minecraft.renderBuffers()" is null
  at …ClientProxy.initRainbowBuffers(ClientProxy.java:496)
  at …ClientProxy.clientInit(ClientProxy.java:318)
  at …fabric.AlexsMobsFabricClient.onInitializeClient(AlexsMobsFabricClient.java:36)
```

Two independent things had to be true for this to ship, which is why it reached a gate:

1. The `//? if !fabric` above it was the **single-line** form, so it gated only the
   `MinecraftForge.EVENT_BUS.register` line and `initRainbowBuffers()` stayed live on Fabric. See the
   scoping note in [`stonecutter.md`](stonecutter.md).
2. `initRainbowBuffers`'s body is itself `//? if <1.20.2 {`, so on 16 of the 17 Fabric nodes the
   method is *empty* and the mis-scoped gate is harmless. **Only `1.20.1-fabric` could ever crash** —
   one node out of 49, which is exactly the density at which a fault gets attributed to that node
   being weird rather than to a rule being wrong.

Fixed by giving the call its own `//? if !fabric`. Fabric 1.20.1 now uses the shared-builder fallback
that every `>=1.20.2` node already uses for those five render types — the same divergence, not a new
one, so it is listed with the others below rather than as a regression.

**What generalises:** `EntityRenderers`/`BlockEntityRenderers`/`MenuScreens.register` are safe from
the entrypoint because they write plain static maps. Anything reached through a `Minecraft.getInstance()`
**field** is not, and must be deferred to a `ClientLifecycleEvents` callback or a first-tick hook.
Read it as a hard split: *registries yes, instance state no.*

#### ⚠️ `c:` is a namespace, not a library — on Fabric nobody is obliged to define the tag you read

The same boot-gate run that found the double registration also found this, and it is the one that would
actually have shipped broken. On Forge and NeoForge the **loader** ships every `Tags` entry, so a
`#forge:`/`#c:` reference always resolves. On Fabric the convention tags come from an *optional* Fabric API
module, and which ones exist depends on the fabric-api build: `fabric-convention-tags-v1` — the only module
present below 1.20.6 — defines **156** tags, and v2 grew to 500+ across a year of releases. Below is what
each pinned fabric-api actually provides, and it is why this could not be gated on the MC version:

| node | fabric-api pin | `c:` tags it defines | missing of the seven |
|---|---|---|---|
| 1.20.1, 1.20.4 | `0.92.11`, `0.97.3` (v1 only) | 156 | all 7 |
| 1.20.6, 1.21 | `0.100.8`, `0.102.0` | 298 / 344 | 5 (`is_swamp`/`is_snowy` arrived) |
| 1.21.2, 1.21.3 | `0.106.1`, `0.114.1` | 377 | 4 (`crops/carrot` arrived) |
| 1.21.1, 1.21.4 → 26.2 | `0.116.15` … `0.155.2` | 493 → 531 | 0 |

Note the **non-monotonicity**: `1.21.1`'s pin is a late backport, so it is complete while `1.21.2` and
`1.21.3` are not. Read the jar, never the MC version.

Referencing an undefined tag is **logged, not thrown** — the referencing tag loads *empty* and cascades.
`c:sands` alone empties `alexsmobs:am_spawns` and the fifteen `*_spawns` tags built on it, so on the two
1.20.x nodes most of the mod stopped spawning while the server still printed `Done (` and exited 0. Exactly
the shape of the Forge-26 `c:` fault of Milestone 13 (see [`build-harness.md`](build-harness.md)), which is
why porting rule 8 exists.

Fixed in `DataPackMigration.backfillFabricConventionTags`: the mod **defines the seven itself**, on *every*
Fabric node — not only the ones whose pin lacks them. A shipped jar meets whatever fabric-api the player
installed, which may be older than the pin. That is safe because tag JSONs **merge**: the values are copied
from fabric-api's own v2 definitions, flattened past the `#c:sands/…` sub-tag indirection (itself
version-dependent), so where the module already defines the tag the union is the module's own set.

The seven: `c:sands`, `c:gravels` (block), `c:seeds`, `c:crops/carrot` (item), `c:is_sandy`, `c:is_swamp`,
`c:is_snowy` (biome). Written in the **plural** folders and before `migrateTo121`, so the singular rename
picks them up.

> **`scripts/verify_convention_tags.py` is the gate step for this.** It diffs every Fabric node's `#c:`
> references against that node's pinned fabric-api jar plus the mod's own `data/c/tags/**`, off the build
> output, in about a second — `nodes=17 problems=0`. The boot gate does fail on `Couldn't load tag` now,
> but only for nodes you actually boot; this answers all 17 without launching anything. Run it after any
> change to the tag data or to the convention-tag passes.

#### Modded `EntityDataSerializer`s go through the loader's registry on Fabric too

Third loader, same trap, and the boundary written here during the `26.2` node was **wrong** — the boot
gate corrected it on `1.21.10-fabric`:

```
IllegalStateException: Tried to register tracked data handler … using TrackedDataHandlerRegistry.register.
This is not allowed as it can lead to desynchronization issues; use FabricTrackedDataRegistry.register instead.
  at …EntityDataSerializers.handler$…$fabric-object-builder-api-v1$onHeadRegister
  at …AMCompat.<clinit>(AMCompat.java:100)
```

Fabric throws from **object-builder `21.1.2`, i.e. the `1.21.5` fabric-api** — the *same* MC line as
NeoForge, not one later. What actually changes at 26.1 is only the **name of the replacement API**:

| fabric-api | guard mixin | API to call |
|---|---|---|
| `< 1.21.5` | none | plain `EntityDataSerializers.registerSerializer` |
| `1.21.5` → `1.21.11` | `TrackedDataHandlerRegistryMixin` | `FabricTrackedDataRegistry.register` |
| `26.1.2`, `26.2` | `EntityDataSerializersMixin` | `FabricEntityDataRegistry.register` |

Same package (`…object.builder.v1.entity`), same `register(ResourceLocation, EntityDataSerializer<?>)`
signature — a pure rename. `AMCompat` now has four Fabric-side arms (`>=26`, `>=1.21.9`, `>=1.21.5`, then
the shared vanilla ones), the 1.21.9 split being only because `COMPOUND_TAG` does not exist below it.
Forge still accepts the vanilla static call on every node.

> ⚠️ **How the wrong boundary survived.** `26.2` was the first Fabric node, so "the class is called
> `FabricEntityDataRegistry`" was true of everything then in the tree, and `>=26` fit. Nothing on the
> compile axis can contradict that: below 26 the arm is simply not projected. **A `>=<newest>` gate written
> on the first node of a new loader is a guess about history, not a finding** — resolve it against the
> older artifacts before the back-fill, or the boot gate will do it for you seven nodes later.
> The list-the-jar probe that settles it:
> `unzip -l <fabric-api jar>` → nested `fabric-object-builder-api-v1-*.jar` →
> `net/fabricmc/fabric/api/object/builder/v1/entity/`.

#### What does NOT work yet on Fabric — read this before shipping anything

`ModPlatformPlugin` now excludes only **two** files from the Fabric compile —
**`client/ClientLayerRegistry.java`** and **`client/render/LavaVisionFluidRenderer.java`** (it prints
`Fabric: ClientLayerRegistry + LavaVisionFluidRenderer are excluded; everything else compiles`).
`event/ServerEvents.java` came in with *Wave 1* and fires from *Wave 2*;
`client/event/ClientEvents.java` came in with *Wave 3a* and fires, in part, from
`fabric/client/FabricClientEvents`.

**The mod still REGISTERS more than it REACTS to on Fabric.** A green five-step gate does not
contradict that — both gates only prove the node boots. The known list, each item deferred by design:

- ~~the remaining **per-frame client hooks**~~ — **all six are now wired**: entity render `Pre`/`Post`
  with *3b-1*, the farseer static overlay with *3b-2*, `ComputeCameraAngles` with *3b-3*,
  `RenderHandEvent` with *3b-4*, the fog pair with *3b-5* and `RenderNameTagEvent` with *3b-6*. What
  remains under this heading is the three renderers that override `render` outright, listed in the
  divergence table;
- ~~Fabric mixins so **multipart entities** are visible to level entity lookups, and one on
  `Entity#collide`~~ — **both landed in *Wave 4***: `FabricLevelMultipartMixin` and
  `FabricMultiPlayerGameModeMixin` for the parts, `FabricEntityMixin#alexsmobs$customCollisions` for
  `ICustomCollisions`;
- ~~Fabric carries no `IEntityWithComplexSpawn` equivalent, so the extra spawn data the 21
  `getEntitySpawningPacket` callers send is **not transmitted**~~ — **this bullet was wrong**, see
  *Wave 4* below: no class implements the interface and neither hook method exists, so there is no
  data being dropped. Documentation defect, no code;
- ~~Fabric API `ArmorRenderer` for the 13 custom armour models~~ — **landed in *Wave 5***:
  `fabric/client/FabricArmorRenderers`, three era arms, **no mixin**;
- ~~the four loot modifiers → `LootTableEvents.MODIFY`; **structure** spawn overrides have no Fabric API
  hook at all~~ — **both landed in *Wave 5***: `FabricLootTableMixin` (not the Fabric API event — it
  only exists from 1.21.6) and `FabricStructureMixin` + `fabric/world/FabricStructureSpawns`;
- ~~brewing (`FabricPotionBrewingBuilder.BUILD` → `FabricBrewing.reset()` + `registerBrewingRecipes`, plus a
  `PotionBrewing` mixin consuming `FabricBrewing.recipes()`)~~ — **landed in *Wave 5***, four mixins,
  and again not through the Fabric API hook;
- ~~`BlockTransmutationTable`'s explode-on-break via `PlayerBlockBreakEvents.AFTER`~~ — **landed in
  *Wave 5***, exactly as written, in `FabricServerEvents.registerBlockBreak`.

⚠️ **This list was wrong when it was written.** It said *"With Wave 5 closed this list is empty"*,
and one item had never been on it at all: **networking**. `AlexsMobs`' three network hooks
(`registerMessage`, `sendMSGToServer`, `sendNonLocal`) had no `fabric` arm, so on Fabric they
compiled to a bare `return` — every one of the ~22 messages was silently dropped, on all 17 nodes,
from Milestone 15 through `2.0.3`. Nothing in the audit caught it because an empty method compiles
and boots green, and because the *feature* the list tracks (multipart attacks, the falconry loop, the
transmutation GUI) each looked wired at its own call site. **Closed in *Wave 6*** — see the section
below. With that, the list really is empty; what remains Fabric-specific is the divergence table at
the bottom of this file — behaviours that are deliberately different, not features that are missing.

**Most of what was left was a PORT, not a design — check `AlexsMobsFP` first.** It ships production
solutions, and its comments record traps it has already paid for:

| item | FP file | shape |
|---|---|---|
| multipart level lookups | `mixin/LevelMultipartMixin` | `@Inject(RETURN)` on `Level#getEntities(Entity, AABB, Predicate)` |
| attacking a part | `mixin/client/MultiPlayerGameModeMixin` | `@Inject(HEAD, cancellable)` on `attack` → `MessageHurtMultipart` |
| structure spawn overrides | `mixin/StructureSpawnOverrideMixin` | `@Inject(RETURN)` on `Structure#spawnOverrides`, merges a map resolved at server start |
| transmutation-table / ghostly-pickaxe block break | `mixin/ServerPlayerGameModeMixin` | `@Redirect` on `hasCorrectToolForDrops` inside `destroyBlock` |
| custom armour models | `mixin/client/HumanoidArmorLayerMixin` | swaps the model `HumanoidArmorLayer` just resolved |

⚠️ **The armour one was not copied, and did not need to be — superseded by *Wave 5*.** FP's mixin is
written with `@ModifyExpressionValue` + `@Local`, and **this tree bundles no MixinExtras** —
deliberately, so no arm depends on whatever Fabric Loader a player happens to have (see 3b-5b). It
would have needed re-expressing in core Mixin against a `>=1.21.2`-era
`renderArmorPiece(HumanoidRenderState, …)` that does not exist on the older nodes. Instead Wave 5 used
**Fabric API's own `ArmorRenderer`**, which is the loader's supported seam for the same job and needs
no mixin at all — see the wave section below. The lesson generalises: **check for a Fabric API
registration point before porting FP's mixin**, because FP shims Forge and reaches for a mixin by
default.

⚠️ **The block-break one was not copied either.** FP `@Redirect`s `hasCorrectToolForDrops` inside
`ServerPlayerGameMode#destroyBlock`, which is one injection covering both jobs. Here they are two
different jobs with two different seams: the ghostly pickaxe's harvest suppression fires
`PlayerEvent.HarvestCheck` from `FabricPlayerMixin` (so it reaches drops, mining speed **and** the
client's crack overlay, not just `destroyBlock`), and the transmutation table's explode-on-break goes
through Fabric API's `PlayerBlockBreakEvents.AFTER`.

Two more things FP's tree does not tell you, because it is single-MC-version and shim-based: every
port here lands in **shared source projected into all 49 nodes**, so anything touching a file the
Forge line already uses must be `//? if fabric`-gated; and FP's `IMultipartEntity` is this repo's
`IMultipartOwner`.

#### Closed since the node first gated green

- **Natural spawns + the leafcutter anthill feature** — `fabric/world/FabricBiomeModifications`. Forge and
  NeoForge reach `AMWorldRegistry` through a **datapack** entry (a registered biome-modifier serializer
  plus `data/alexsmobs/{forge,neoforge}/biome_modifier/*.json`); Fabric has no datapack hook, so its
  equivalent is a one-time `BiomeModifications.create(...).add(ADDITIONS, BiomeSelectors.all(), …)`
  registration made from `AlexsMobsFabric.onInitialize` **after** `BiomeConfig.init()` (it reads the
  per-mob biome files) and **after** `new AlexsMobs()` (it names entity types the registries have just
  filled). The callback builds a `ModifiableBiomeInfo.BiomeInfo.Builder`, hands it to the shared
  loader-neutral `AMWorldRegistry.addBiomeSpawns`, and drains `getRecordedSpawns()` into Fabric's context
  — which is the whole reason `MobSpawnSettingsBuilder` records as well as delegating. The spawn table
  itself is untouched and stays the single source of truth for all three loaders.
  - `BiomeSelectors.all()` is deliberate, not lazy: the mod's own `BiomeConfig`/`SpawnBiomeData` decides
    biome eligibility and understands modded biomes, so filtering here would silently override the
    player's config.
  - The anthill goes in **by `ResourceKey`** rather than through `addLeafcutterAntSpawns`, whose parameter
    is the `HolderSet<PlacedFeature>` the Forge modifier decoded from JSON — there is no registry lookup
    in scope to build one. Its two-part guard (biome test + `leafcutterAnthillSpawnChance > 0`) is
    reproduced verbatim from the shared code.
  - ⚠️ **Fabric's argument order is not vanilla's.** `MobSpawnSettingsContext.addSpawn` is
    `(category, data, weight)`; the vanilla builder is `(category, weight, data)`. Both take an `int` in a
    middle-or-last position, so **swapping them is a silent behaviour bug, not a compile error**.
- **`AlexsMobsFabricClient` now calls `AlexsMobs.PROXY.clientInit()`** — the ~130 `EntityRenderers.register`
  calls, the three `BlockEntityRenderers.register` calls and the transmutation-table screen. Forge/NeoForge
  run that from `FMLClientSetupEvent`, which Fabric has no equivalent of. Without it the mod loads and
  every mob spawns with **no renderer at all**, and *neither gate can see it* — `bootgate.sh` is a
  dedicated server and `clientgate.sh` stops at the title screen.
  - `MenuScreens.register` is **private** in vanilla 26.2 and `MenuScreens$ScreenConstructor` is
    package-private (javap-verified against the **unpatched** `~/.gradle/caches/fabric-loom/26.2/
    minecraft-merged.jar` — Forge patches it public, NeoForge closed it at 20.6 for
    `RegisterMenuScreensEvent`). Both are widened in `alexsmobs.accesswidener`, and `ClientProxy`'s gate
    over that call is now `forge || fabric || <1.20.6`.

> **Verify a silent registration with a throwaway probe, not by reading the code.** Both of the above are
> exactly the shape this file keeps getting burned by — compile-clean, logged-not-thrown, invisible to
> every gate step. The spawn wiring was proved by temporarily logging one line per modified biome from
> inside the callback and running `SOAK=10 scripts/bootgate.sh 26.2-fabric`: **66 biomes, 525 spawn
> entries, zero exceptions**, then the probe was reverted. A green boot alone would have said nothing —
> `BiomeSelectors.all()` matching nothing, or `getRecordedSpawns()` coming back empty, both boot perfectly.

Also noticed in passing and **not** a Fabric issue: `AMAdvancementTriggerRegistry.DEF_REG.register` is
commented out in `AlexsMobs.java` on every loader — a pre-existing latent bug worth raising with the user.

#### Back-filling downward — done: 17 Fabric nodes, 1.20.1 → 26.2

All seventeen declared Fabric nodes now compile. The estimate below was right about the shape — the
cost was one-time, and each node after `26.2` was a compile-fix pass — and right about the direction:
nothing had to be re-derived going down, only re-gated.

The pre-start work list ("a Fabric `<1.21` arm for `EntityVoidWorm.tickDeath`", the `ToolActions`
call, the 2-arg `addSpawn` override, `BrewingRecipeRegistry.addRecipe`, the spawn-placement block,
`ClientProxy.init()`'s `<1.21.4` listener block, `AMPlatform`'s reach/swim attributes and the `<26`
fluid-height arms) is **closed**, plus a good deal it did not predict.

⚠️ **The recurring shape of the whole back-fill, and the thing to expect on any future node: most of
what breaks below 1.20.5 is a *Forge patch to a vanilla class*, not a missing Forge class.** Those do
not show up in an import survey — the source names a vanilla type and calls a method that only exists
on the patched jar. `Entity#getStepHeight`, `EnchantmentCategory#create`, `SpawnPlacements$Type#create`,
`BlockStateBase#isValidSpawn(…, SpawnPlacements$Type, …)`, `FoodProperties$Builder#effect(Supplier,…)`,
`RecordItem`'s constructor, `Enchantment#isAllowedOnBooks`/`#canApplyAtEnchantingTable` were all found
this way and only by compiling. Probe with `javap` against
`~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged/<mc>-*/*.jar`, which is
**unpatched** — that is the only reliable answer to "is this vanilla or is this Forge?".

Two of them needed a *replacement rule* rather than a gate, and the rule direction is worth knowing:
`!mc205-stepheight` rewrites `.getStepHeight()` → `.maxUpStep()` on `>=1.20.5` because the root source
is Forge 1.20.1 and `getStepHeight` is Forge's name there. Fabric below 1.20.5 needs the **same**
rewrite for the opposite reason, so it has its own `!fab-stepheight` group rather than widening that
condition — widening it would also fire on Fabric nodes that must keep the vanilla name.

#### Wave 1 — `ServerEvents` compiles on Fabric: stub the event TYPES, do not fork the file

`ServerEvents` is 1,117 lines, 40 `@SubscribeEvent` handlers and ~81 gate lines. Adding a Fabric arm
to each of them was never on the table. What worked instead, first try:

1. **Stub the ~20 Forge event types** into `fabric/forge/**` — same relocated-compat-namespace pattern
   the tree already uses for `ForgeConfigSpec`, `DeferredRegister`, `IItemHandler` and friends. They
   are dumb carriers: fields, getters, `isCanceled`/`setCanceled`, `Event.Result`. Each one's javadoc
   states **what behaviour rides on it and what the Wave 2 dispatcher must honour** — e.g.
   `VillagerTradesEvent` must fire *once per profession at server start*, not per villager, or the
   fisherman offers ambergris twice.
2. **Redirect the imports with `!fab-fe-*` replacement rules** rather than gating them. The rules are
   keyed on `import net.minecraftforge.event.…` — the `import ` prefix is load-bearing twice over: it
   stops them rewriting the stubs' own javadoc, and it keeps them from racing `!fab-attribevent`,
   which claims a bare FQN.
3. **Audit every `forge || …` gate in the file.** Fabric was falling into the *NeoForge* arm of five
   of them. The stubs are modelled on the **Forge** shapes, so a Fabric node always wants the Forge
   arm: those gates now read `forge || fabric || …`. This is the rule to remember for any future file
   — a `forge || <x>` gate is almost never right once Fabric exists.

The first compile produced **7 errors, none of them an event type** — all four causes were Forge
*patches on vanilla classes* (`Entity#getPersistentData`, `Player.PERSISTED_NBT_TAG`,
`ItemHandlerHelper.giveItemToPlayer`, `BlockState#isScaffolding`), i.e. exactly the shape the
back-fill section above warns about. That is the evidence the architecture was right.

Two things this cost that are worth not re-learning:

- **A replacement rule whose search text is a substring of its own replacement cannot be shadowed.**
  `!mc205-persistednbt` rewrote `Player.PERSISTED_NBT_TAG` → `ServerPlayer.PERSISTED_NBT_TAG` for
  `>=1.20.5`, and Fabric has *neither* spelling. Declaring a Fabric rule ahead of it did not win the
  offset, and any arm spelling the Forge name would itself be rewritten. Both rules are gone; the name
  is now a three-arm `AMCompat.PERSISTED_NBT_TAG` constant that binds the real platform field on
  Forge/NeoForge and Forge's literal value (`"PlayerPersisted"`) on Fabric. See
  [`stonecutter.md`](stonecutter.md).
- **`VillagerTradesEvent` needed a whole-class three-arm gate**, because 1.21.5 moved villager
  professions into a registry and `getType()` became `ResourceKey<VillagerProfession>` — and the class
  body was *already* inside a `<26` block, which blocks never nest inside. When a gated class needs a
  second, finer split, the answer is more arms on the outer block, not an inner one.

**Latent crash found and fixed on the way (not a compile error):** `mixin/LivingEntityMixin` gated the
entire vendored-Citadel data store on `forge || <1.21.8`, so on the Fabric nodes `>=1.21.8` the two
`ICitadelDataEntity` methods simply **vanished from an abstract mixin** — which compiles perfectly and
then throws `AbstractMethodError` the first time the vine lasso, tendon whip, squid grapple, rainbow
dye, rocky chestplate or flying-fish boots are used. Only the `neoforge` arm opts out (it is on a data
attachment); every other gate in that file now spells the other side `forge || fabric`.

#### Wave 2 — firing them: `fabric/event/FabricServerEvents`

One class wires every hook by hand and holds **the same single `ServerEvents` instance** the other
loaders put on the bus (it keeps per-level state — the beached-whale spawner map — so a second
instance would silently double the spawner). `AlexsMobsFabric#onInitialize` calls `init()` after
`new AlexsMobs()`, for the same reason `FabricBiomeModifications` is called there.

Landed and boot-verified (`SOAK=10 scripts/bootgate.sh` green on `1.20.1-fabric` and `26.2-fabric`):

| Forge event | Fabric source | Version split |
|---|---|---|
| `TickEvent.LevelTickEvent` | `ServerTickEvents.START_*_TICK` **and** `END_*_TICK` | `START_WORLD_TICK` → `START_LEVEL_TICK` at `>=26` |
| `PlayerEvent.PlayerLoggedInEvent` | `ServerPlayConnectionEvents.JOIN` | — |
| `PlayerInteractEvent.RightClickItem` | `UseItemCallback` | returns `InteractionResultHolder<ItemStack>` below **1.21.2**, `InteractionResult` at/above |
| `PlayerInteractEvent.EntityInteract` | `UseEntityCallback` | — |
| `PlayerInteractEvent.RightClickBlock` | `UseBlockCallback` | — |
| `AttackEntityEvent` | `AttackEntityCallback` | — |

- ⚠️ **Both tick phases must be registered.** Forge posts `LevelTickEvent` in START *and* END and the
  mod never filtered on phase, so its body runs twice per level per tick there — which is why the
  NeoForge and Forge `>=1.21.9` arms subscribe `Pre` *and* `Post`. Registering one Fabric callback
  would halve the beached-cachalot spawn rate and add a tick of latency to the teleport queue, with
  nothing visibly broken.
- The four interaction callbacks were verified by **javap-diffing the fabric-api jar of every pinned
  node**, not by compiling one and hoping: `UseEntityCallback`/`UseBlockCallback`/
  `AttackEntityCallback` are byte-identical across all 17, and only `UseItemCallback` moves. Worth
  repeating for any further callback — a fabric-api rename (`WORLD`→`LEVEL`) does not track MC
  versions and would otherwise be found one node at a time.
- Cancellation funnels through one `result(event)` helper: Forge's cancel-flag + `cancellationResult`
  pair becomes Fabric's non-`PASS` return. An **uncancelled** event must come back `PASS` even if
  something set a result, or the interaction never reaches vanilla.

##### Batch B — the ones with no callback at all: `mixin/fabric/**`

Eleven more hooks have no Fabric API analogue, so each is fired from a mixin on the exact vanilla
method Forge patches. The split of labour is deliberate and worth keeping: **the mixin is only a
"where"** — build the event, call one `FabricServerEvents.fireX`, turn a `true` back into the right
vanilla refusal — and **`ServerEvents` stays the only "what"**. Neither file then has to know about
the other's MC-version arms.

| Forge event | Injection point | Arms |
|---|---|---|
| `LivingEvent.LivingTickEvent` | `LivingEntity#tick` HEAD | — |
| `LivingAttackEvent` | `LivingEntity#hurt` HEAD, cancel → `return false` | `hurt` → `hurtServer(ServerLevel,…)` at **1.21.2** |
| `LivingDamageEvent` | `LivingEntity#actuallyHurt` HEAD | gained a `ServerLevel` at **1.21.2** |
| `LivingEntityUseItemEvent.Finish` | `LivingEntity#completeUsingItem` HEAD | — |
| `LivingDropsEvent` | `LivingEntity#dropAllDeathLoot` TAIL | gained a `ServerLevel` at **1.21** |
| `LivingChangeTargetEvent` | `Mob#setTarget` HEAD, cancel → field never assigned | — |
| `MobSpawnEvent.AllowDespawn` | `Mob#checkDespawn` HEAD, DENY → cancel | — |
| `MobSpawnEvent.FinalizeSpawn` | `Mob#finalizeSpawn` RETURN | lost its trailing `CompoundTag` at **1.20.5** |
| `EntityStruckByLightningEvent` | `Entity#thunderHit` HEAD | — |
| `EntityEvent.Size` | `Entity#refreshDimensions` TAIL, writes back `eyeHeight` | `<1.20.2` only — Forge deleted the event |
| `ProjectileImpactEvent` | `Projectile#onHit` HEAD | — |

- **All eleven descriptors were javap'd on all 17 Fabric nodes before a line was written** (rule 10).
  That is what produced the "Arms" column, and the point is the negative result: seven of the eleven
  are byte-identical across 1.20.1 → 26.2, so they need no arms at all — which you cannot know from
  reading source, and guessing wrong is a `defaultRequire: 1` crash no gate step can see.
- **`MobSpawnType` needs no arm of its own** even though 1.21.2 renamed it to `EntitySpawnReason`:
  the `!mc2102-spawnreason` replacement rule already rewrites the bare type name tree-wide, and the
  new name is in the same package. Write `MobSpawnType` and let the rule move it — hence
  `finalizeSpawn` has **two** arms, not three.
- Selectors are **name-only** throughout (none of the eleven names is overloaded in its declaring
  class on any node), which is what keeps them immune to descriptor drift *and* correct on the 15
  obfuscated nodes, where a name-only selector still remaps into intermediary.
- **A twelfth class in the package is not an event hook**: `FabricServerPlayerMixin` injects
  `ServerPlayer#restoreFrom` TAIL to carry the `PERSISTED_NBT_TAG` sub-tag across a respawn, which is
  what Forge's *patch* to that same method does. It closes the one divergence in the table below that
  was explicitly deferred to this wave — without it a player who dies is handed a second Animal
  Dictionary. Descriptor is identical on all 17 nodes; TAIL is safe because vanilla's body only copies
  fields and never reloads the new player from NBT.
- **`mixin/fabric/**` cannot live under `alexsmobs/fabric/**`** with the rest of the Fabric-only code
  — a mixin's package has to sit under the config's declared `package`. So it gets its own
  `configureJava` exclude on Forge/NeoForge, plus `DataPackMigration.pruneMixinPackage(…, "fabric.")`
  in `processResources`, because Fletching Table's `@Mixin` scan ignores source-set excludes and a
  config naming an absent class is a hard load failure. Pruned by **prefix**, not class by class, so
  adding a twelfth hook needs no build-logic change. On those two loaders the classes are not merely
  redundant — leaving them in would fire every one of these handlers a **second** time.

Still unfired after Batch B: `LootingLevelEvent`, `ExplosionEvent.Detonate`,
`PlayerEvent.HarvestCheck`, `AddReloadListenerEvent`, both trade events, `ItemTooltipEvent` and
`ComputeFovModifierEvent` — plus `LeftClickEmpty`/`RightClickEmpty`, which Forge fires from the
**client's** swing/use handling and Fabric has no callback for at all. The last four are client-side
and belong with Wave 3.

#### Wave 3a — `ClientEvents` compiles and partly fires on Fabric

The last excluded behaviour file. It is 800 lines, 16 `@SubscribeEvent` handlers, and it drives the
lava-vision goggles, the ender-flu screen shake, the mimicube outline, the star brightness, the rocky
chestplate roll, the clinging flip, the wandering-trader model swap, the bald-eagle camera and the
farseer static. Same architecture as Wave 1/2 and for the same reason: **stub the event types, do not
fork the file.**

The one insight the whole wave rests on, and the thing to reuse on any future client work:

> **On a Fabric node, every `forge && …` and `neoforge && …` gate is false, so the `else` arm is
> selected on all seventeen nodes regardless of MC version.** The else arms in `ClientEvents` carry
> the *1.20.1-Forge* API shape. So if the Fabric stub can answer the 1.20.1 getters everywhere, the
> shared file needs almost no new arms — the version differences move down into what the Fabric
> mixin passes to the stub's constructor.

Hence `fabric/forge/client/event/RenderLivingEvent` carries the **union** of every era's payload
(entity, renderer, partial tick, pose stack, buffer source, packed light, and — from 1.21.2 — the
render state), even though no single Forge version has all of them at once. Forge's own event lost
the entity and partial tick at 1.21.2 and the buffers and packed light at 1.21.9, which is why
`ClientEvents` reads all six through its `rendered*` helpers in the first place. On 1.21.9+ the Wave
3b mixin will hand it an `AMSubmitBuffers` and the state's `lightCoords` — the same two substitutions
the Forge `>=1.21.9` arms make, one level further down.

Only **two** new arms were needed in the shared file, both `fabric && >=1.21.2` (the rocky-roll Post
repost and `flipUpsideDown`), and both are **byte-identical to the `forge && >=1.21.2` arm above
them** — which is why the stub's accessor is named `getState()`. Two arms that read the same cannot
drift apart unnoticed.

New stubs: `ViewportEvent` (+`ComputeFogColor`/`RenderFog`/`ComputeCameraAngles`),
`RenderLivingEvent` (+`Pre`/`Post`), `RenderHandEvent`, `RenderNameTagEvent`, `MinecraftForge`
(a one-field holder for the single `EVENT_BUS.post(...)` in the shared source), and
`TickEvent.Phase`/`TickEvent.ClientTickEvent`. Wired by
[`fabric/client/FabricClientEvents`](../../src/main/java/com/github/alexthe666/alexsmobs/fabric/client/FabricClientEvents.java),
called from `ClientProxy#clientInit` where the other two loaders call
`MinecraftForge.EVENT_BUS.register(new ClientEvents())` — **one instance**, because the handler keeps
`previousLavaVision` on it.

Live now, without a single new mixin:

| Handler | Fabric source |
|---|---|
| `onOutlineEntityColor`, `onGetStarBrightness`, `onPoseHand`, `onGetFluidRenderType` | the mod's **own** `AMEventBus` — `mixin/client/{LevelRenderer,ClientLevel,HumanoidModel,ItemBlockRenderTypes}Mixin` already post these on every loader, so all that was missing was a subscriber |
| `clientTick` | `ClientTickEvents.START_CLIENT_TICK` — START only, because the handler's own guard is `phase == Phase.START` |
| `onPostRenderEntity` (the rocky-roll repost only) | `MinecraftForge.EVENT_BUS`, narrowed with an `instanceof` since Forge's bus is untyped |

Four things this cost that are worth not re-learning:

- ⚠️ **A version-keyed replacement rule fires on Fabric too.** `!mc2102-renderlivingevent` rewrites
  `RenderLivingEvent<?, ?>` → `<?, ?, ?>` on `>=1.21.2` and is keyed on the MC version alone, so the
  *stub's* type-parameter arity has to be gated to match on all three of its class declarations —
  otherwise `26.2-fabric` fails with `wrong number of type arguments; required 2` at five sites in a
  file nobody edited. Check the loader-neutral rule groups before adding any Fabric type that shares
  a name with a Forge one.
- **Forge/NeoForge ship `GameRenderer#loadEffect`/`currentEffect`/`shutdownEffect` widened** and there
  was no accesstransformer entry to copy — the lava-vision post-chain needs them. Three `<1.21.2`
  entries in `accesswidener/alexsmobs.accesswidener`, pre-flighted with `scripts/aw_check.py`. An AW
  entry naming an absent member is a hard error, unlike an AT entry.
- **Two handlers and one helper were gated `!fabric` rather than stubbed**: the `LavaVisionFluidRenderer`
  swap (`<26 && !fabric`), `onPostGameOverlay` (`<1.20.5 && !fabric`) and `updateAllChunks`. That
  avoids `RenderLevelStageEvent`, `RenderGuiOverlayEvent`/`VanillaGuiOverlay` and a `LiquidBlockRenderer`
  access widener entirely. A wildcard `import net.minecraftforge.client.event.*;` does not require any
  particular member to exist, which is what makes gating cheaper than stubbing here.
- `updateAllChunks` had to be **restructured so its null guard is duplicated inside each arm** —
  blocks never nest (rule 4).

**Investigated and deliberately left alone:** `ClientEvents:227` passes the packed light where a
partial tick belongs (`walkAnimation.speed(renderedLight(event))`). `git show 151e36c:…` proves the
pristine Alex's Mobs baseline already read `event.getPackedLight()` there. Upstream's oddity,
faithfully preserved.

#### Wave 3b — the per-frame hooks, all six done

Everything left needs an injection point Fabric API does not expose, so each comes with its own mixin
— the same "the mixin is only a *where*" split as Wave 2 Batch B:

| Forge event | Injection point | Drives |
|---|---|---|
| ✅ `RenderLivingEvent.Pre`/`.Post` | `LivingEntityRenderer#render` / `#submit` | rocky-chestplate roll, clinging flip, ender-flu shake, vine lasso, wandering-trader model swap |
| ✅ `RenderHandEvent` | `ItemInHandRenderer#renderArmWithItem` (**not** `renderHandsWithItems`) | bald-eagle POV hides both hands, perched falconry bird, dimensional-carver swing nudge |
| ✅ `ViewportEvent.ComputeFogColor` / `.RenderFog` | `FogRenderer` — `setupColor`/`computeFogColor` and `setupFog` | lava-vision goggles |
| ✅ `ViewportEvent.ComputeCameraAngles` | `Camera#setup` / `#update` | earthquake shake **and** `doWorldLastFrame()` (bald-eagle camera return) |
| ✅ `RenderNameTagEvent` | `EntityRenderer` — `renderNameTag` / `submitNameTag` / `submitNameDisplay` (the **callee**, not Forge's call site) | bald-eagle POV hides the player's own nameplate, singleplayer only |
| ✅ the farseer static overlay | `HudRenderCallback` below 26, `HudElementRegistry` at/above | `renderStaticOverlay` — the one row that needed no mixin |

**Every remaining row has a target on all 17 nodes** — surveyed by javap over each MC version's Mojmap
jar on 2026-07-31, which corrected two earlier "the class is gone" readings. Nothing here is a
four-arm job like 3b-1; each is three to four arms of its own, and the arm boundaries are *not*
shared between rows:

| target | how it moves across 1.20.1 → 26.2 |
|---|---|
| `GameRenderer#renderLevel` | `(FJLPoseStack;)V` ≤1.20.4 → `(FJ)V` at 1.20.6 → **`(DeltaTracker)V` from 1.21, unchanged through 26.2** |
| `Camera#setup` | `(BlockGetter,Entity,ZZF)V` → `(Level,Entity,ZZF)V` at **1.21.11** → **gone at 26**, where `update(DeltaTracker)V` is the whole camera step |
| `ItemInHandRenderer#renderHandsWithItems` | `MultiBufferSource$BufferSource` → `SubmitNodeCollector` at 1.21.9 → renamed **`submitHandsWithItems`** at 26.2 (still present under the old name at 26.1.2) |
| `FogRenderer` | **not deleted at 1.21.6 — moved** to `client/renderer/fog/`. `computeFogColor` and `setupFog` exist on every node; both change signature at 1.21.2, 1.21.6, 1.21.11 and 26.1.2 |
| `EntityRenderer#renderNameTag` | `(Entity,…)` → `(EntityRenderState,…)` at 1.21.2 → **`submitNameTag`** at 1.21.9 → **`submitNameDisplay`** at 26, which is **overloaded** (4-arg and 5-arg) and so needs full descriptors for the same bridge reason as 3b-1 |

⚠️ The two corrections are worth the general form: **"the class no longer exists at X" is usually
"the class moved at X"**, and a rename is invisible to a class-existence check. The fog rows were
written off for eight nodes on a package-relative grep; `FogRenderer` had simply moved one package
down. Survey by dumping the jar, not by grepping for the old FQN.

##### 3b-1 — `RenderLivingEvent.Pre`/`.Post` (done)

`mixin/fabric/client/FabricLivingEntityRendererMixin` → `FabricClientEvents.firePreRenderLiving` /
`firePostRenderLiving`. Four things about it are worth not re-deriving:

- **The selectors carry full descriptors, against this tree's name-only habit.** `render` and `submit`
  are both overloaded *on this class* by the compiler's bridge for `EntityRenderer`'s erased signature
  (`render(Entity,…)` / `submit(EntityRenderState,…)`), and the bridge *calls* the real method — so a
  name-only selector fires every hook twice per entity per frame. That is a doubled model, not a crash,
  which is exactly the class of bug no gate step can see. The existing "name-only throughout" note
  applies to Wave 2's non-overloaded targets and does not generalise.
- **Four descriptor arms, not three**: `<1.21.2` `render(LivingEntity,F,F,…)`; `1.21.2–1.21.8`
  `render(LivingEntityRenderState,…)`; `1.21.9–1.21.11` `submit(…,SubmitNodeCollector,CameraRenderState)`;
  `>=26` the same with `CameraRenderState` in `…renderer.state.level`. The package move needs its own arm
  because `!mc26-pkg-camerastate` rewrites the **dotted** FQN and a mixin selector is **slashed** — the
  general rule being that no replacement rule in this tree can reach inside a descriptor string.
- **`Post` is `@At("TAIL")`, never `@At("RETURN")`.** The `Pre` injector is `cancellable` and its
  `ci.cancel()` inserts a *new* `return` at the top of the method, which a later `RETURN` scan picks up —
  so `Post` would fire on the cancelled path, which is precisely what Forge does not do and what the
  rocky-chestplate branch compensates for by reposting a `Post` by hand. Verified by bytecode dump on all
  17 nodes that the method body contains **exactly one** `return`, so `TAIL` is unambiguous.
- **What the mixin unpacks** so `ClientEvents` can stay on its 1.20.1-shaped `else` arms: entity and
  partial tick come from the parameters below 1.21.2 and from `AMStateAccess.entity/partialTick(state)`
  above it; packed light is the `int` parameter through 1.21.8 and `state.lightCoords` from 1.21.9 (the
  field does not exist before then — 1.21.2's `EntityRenderState` carries no light at all); and from
  1.21.9 the buffer source handed to the event is a fresh `AMSubmitBuffers(collector, camera)`, which the
  shared `ClientEvents.flushBuffers` finds again through `AMSubmitBuffers.of(…)` and replays.

The build-logic half is one word: `DataPackMigration.clientMixinPackages` gained `"fabric.client."`.
Those prefixes are matched with `startsWith`, so `"client."` does **not** match a nested package, and
without the new entry the mixin stays in the common `mixins` array — where a Fabric dedicated server,
which has no dist cleaner, would try to apply it to a class that is not on its classpath and abort the
launch. Verified in the built jars: the entry is under `client` on all 17 Fabric nodes and absent
entirely on Forge/NeoForge (`pruneMixinPackage(prefix = "fabric.")` already covered the subpackage).

##### 3b-2 — the farseer static overlay (done)

`FabricClientEvents.registerFarseerStatic()`, called from `register()`. The only row of Wave 3b with
no mixin: `ClientEvents.renderStaticOverlay` is a plain static draw call rather than an
`@SubscribeEvent` handler, so it needs a place to be called from and nothing else.

⚠️ **It had never drawn on any Fabric node, and the comment at `ClientEvents#renderStaticOverlay`
said otherwise.** The three registration arms in `ClientProxy` are all `forge`/`neoforge`, and
`mixin/client/GuiMixin` is gated `forge && >=1.21 && <26`; the "every loader reaches this by its own
route" comment was written from the Forge side and read as covering Fabric. Treat a comment that
enumerates loader routes as a claim to verify, not a finding.

Three arms, and **their boundaries are Fabric API's, not Minecraft's** — checked by listing every
pinned `fabric-api` jar rather than inferred from the MC version:

| nodes | hook | why here |
|---|---|---|
| `<1.21` | `HudRenderCallback.EVENT` `(GuiGraphics, float)` | the callback's own signature |
| `1.21`–`1.21.11` | `HudRenderCallback.EVENT` `(GuiGraphics, DeltaTracker)` | Fabric API swapped the partial tick for a `DeltaTracker` at **1.21** |
| `>=26` | `HudElementRegistry.addLast(ResourceLocation, HudElement)` | the callback is **gone**; `HudElement.extractRenderState(…, DeltaTracker)` replaces it |

`HudElementRegistry` first appears at **1.21.6**, but the deprecated callback keeps working through
1.21.11 — using it for the whole `<26` range buys one arm instead of two for no behavioural
difference, and is the reason the middle arm spans six nodes.

**Divergence from Forge/NeoForge, accepted deliberately.** Those two insert the layer immediately
above the camera overlay, i.e. *underneath* the hotbar and chat. `HudRenderCallback` draws after the
whole HUD and offers no anchor, so the fifteen `<26` Fabric nodes necessarily draw the static on top
of everything — and 26 therefore uses `addLast` rather than
`attachElementAfter(VanillaHudElements.MISC_OVERLAYS, …)`, matching its own siblings instead of the
other loaders. Choosing the anchor would also have meant guessing: the 26 classfiles carry no
parameter names, so the argument order of `attachElementAfter` is not verifiable from bytecode. For a
full-screen tint the whole difference is whether the hotbar shows through it.

Two Stonecutter traps were paid for in this one method, both now written up in
[`stonecutter.md`](stonecutter.md): prose between `//?} else {` and the arm's `/*` becomes live code,
and the `>=26` arm must not spell the token that `!mc26-guigraphics` produces — which is why its
lambda parameters are left untyped.

##### 3b-3 — `ViewportEvent.ComputeCameraAngles` (done)

`mixin/fabric/client/FabricCameraMixin` → `FabricClientEvents.fireComputeCameraAngles`. No new stub:
Wave 3a's `ViewportEvent.ComputeCameraAngles(Camera, double)` already had the right shape on all 17
nodes, so this row is a mixin and a dispatcher and nothing else.

**One event, two handlers.** `ClientEvents` subscribes *both* `onCameraSetup` (earthquake camera
shake) and `onRenderWorldLastEvent` (`doWorldLastFrame()` — bald-eagle camera return, lava-vision
chunk refresh) to this one event on Fabric, the second via the `//? if fabric` arm that exists because
Fabric never had `RenderLevelStageEvent` either, exactly like Forge `>=1.21.3`. The dispatcher calls
both, in that order. Firing only the one the event is *named* after would have wired half a feature
and looked correct in every gate.

**The injection point is the callee, deliberately.** The obvious target is the caller — but it moves
three times across the range (`GameRenderer#renderLevel` up to 1.21.9, `#updateCamera` at 1.21.11,
`#update` at 26) while `Camera#setup`/`#update` stays put and, per a jar-wide scan of every
`net/minecraft/client/**` class in each era, has **exactly one caller in the whole client** on every
version checked. Injecting into the callee is therefore both once-per-frame and immune to the caller
moving. `@At("TAIL")`, because the point is to run after the camera holds its final position; javap
confirmed the body has exactly one `return` on 1.20.4 → 26.2, so TAIL is the end of the method and not
one branch of it.

Three arms, boundaries from the descriptor survey above:

| nodes | target |
|---|---|
| `<1.21.11` | `setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V` |
| `1.21.11` | `setup(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;ZZF)V` — the level parameter widened |
| `>=26` | `update(Lnet/minecraft/client/DeltaTracker;)V` — `setup` is gone; `Camera` stopped being handed the world and reads it from a field it owns. Partial tick is `getGameTimeDeltaPartialTick(false)` |

The null guard lives in the dispatcher, not the mixin: both handlers dereference
`Minecraft.getInstance().player` without checking, which is safe only because the event fires inside a
level render — and a camera hook is the kind of thing a later arm could fire from the title screen.

**How the three arms were proved, and why it is the check that matters here:** `verify_mixin_targets.py`
went from **671 to 688 selectors, +17 — one per Fabric node**. A mixin whose arms are wrong does not
fail to compile and does not fail to boot; it fails to *match*, and the only visible symptom is a
selector count that did not rise. Read that number, not the boot markers.

Gate: build rc=0 over all 49 nodes, `verify_mixins` `jars=71 problems=0`, `verify_mixin_targets`
`jars=49 selectors=688 problems=0`, `verify_convention_tags` `nodes=17 problems=0`, `bootgate.sh`
rc=0 with all 17 Fabric nodes DONE.

##### 3b-4 — `RenderHandEvent` (done)

`mixin/fabric/client/FabricItemInHandRendererMixin` → `FabricClientEvents.fireRenderHand`. Wave 3a's
stub was already the right shape, so again: one mixin, one dispatcher.

**The survey moved the target, and the row above had it wrong.** `renderHandsWithItems` is the whole
first-person pass; the event is **per hand** — the handler branches on `getHand()`, and cancelling is
meant to drop one hand's render, not both. The per-hand callee `renderArmWithItem` is invoked once per
hand and already carries hand, stack, partial tick, pose stack, buffers and packed light *as
parameters*, so nothing has to be reconstructed. It is also exactly where Forge patches:
`ForgeHooksClient.renderSpecificFirstPersonHand` replaces each `renderArmWithItem` call and skips it
when the event is cancelled. `@At("HEAD")` + `cancellable`, because vanilla's body **is** this method.

| nodes | target |
|---|---|
| `<1.21.9` | `renderArmWithItem(…Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V` |
| `1.21.9`–`26.1.2` | same name, `…Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V` — 1.21.9 turned rendering into submission |
| `>=26.2` | **`submitArmWithItem`**, same descriptor as the row above — a pure rename |

⚠️ **Those two boundaries are each invisible to one kind of check.** 1.21.9 keeps the name and changes
the descriptor; 26.2 keeps the descriptor and changes the name. A name grep passes 1.21.9 and a
signature grep passes 26.2 — this is rule 10's case in miniature, in one method, twice.

From 1.21.9 the mixin wraps the collector in an `AMSubmitBuffers` before handing it to the stub, which
is what lets `ClientEvents` keep drawing the falconry bird with a legacy immediate-mode call. It is
**not** flushed in the mixin: the handler calls `flushBuffers` itself once it has finished drawing,
and an event no handler drew into has nothing to replay. The no-camera constructor is the one Forge's
own `handBuffers` uses for this event — a `RenderHandEvent` carries no `CameraRenderState` on any
loader.

Gate: build rc=0 over all 49 nodes, `verify_mixins` `jars=71 problems=0`, `verify_mixin_targets`
**`jars=49 selectors=705 problems=0`** (688 + 17, one per Fabric node), `verify_convention_tags`
`nodes=17 problems=0`, `bootgate.sh` rc=0 with all 17 Fabric nodes DONE.

##### 3b-5a — `ViewportEvent.ComputeFogColor` (done)

`mixin/fabric/client/FabricFogRendererMixin` → `FabricClientEvents.fireComputeFogColor`. One row of
the table, five arms — this is the worst-fragmented hook in the whole wave.

**Three independent boundaries, none of which line up:**

| what moves | where |
|---|---|
| the **class** moves `client/renderer/FogRenderer` → `client/renderer/fog/FogRenderer` | 1.21.6 |
| the **method** changes shape | 1.21.2, 1.21.6, 1.21.11, 26 |
| the method stops being **`static`** | 1.21.6 |

Because the class itself moves, **the `@Mixin` annotation is gated** — the only mixin in the tree where
that is true. 1.21.2 and 1.21.11 happen to share a descriptor while sharing nothing else, so the arms
cannot be collapsed.

| nodes | target | static? | how the colour is reached |
|---|---|---|---|
| `<1.21.2` | `setupColor(Lnet/minecraft/client/Camera;FLnet/minecraft/client/multiplayer/ClientLevel;IF)V` | yes | `TAIL`; assign the three `@Shadow` statics `fogRed`/`fogGreen`/`fogBlue` |
| `>=1.21.2 && <1.21.6` | `computeFogColor(…IF)Lorg/joml/Vector4f;` | yes | `RETURN`; mutate `cir.getReturnValue()` |
| `>=1.21.6 && <1.21.11` | `computeFogColor(…IFZ)Lorg/joml/Vector4f;` | **no** | same |
| `>=1.21.11 && <26` | `computeFogColor(…IF)Lorg/joml/Vector4f;` | **no** | same |
| `>=26` | `computeFogColor(…IFLorg/joml/Vector4f;)V` | **no** | `TAIL`; mutate the **out-param** |

⚠️ **A sixth failure mode for rule 10, and the reason this row cost a survey rather than a grep:**
`computeFogColor` **stops being `static` at 1.21.6** while keeping both its name and its descriptor.
That is invisible to a name grep, invisible to `sigdiff.py`, and **javac accepts a mismatched handler**
— a static handler against an instance target throws only at mixin-apply time, on the client, which no
gate step in this tree can reach while the no-client constraint holds. It was caught by dumping the
*access flags* (`survey_fog3.py`), not the signatures. When surveying a target, dump `ACC_STATIC`.

The dispatcher guards `mc.player == null` because `ClientEvents#onFogColor` dereferences
`Minecraft.getInstance().player` with no check of its own, and it **always returns the event, never
`null`**, so every arm can write the result back unconditionally.

Gate `bpib9s8zk`: build rc=0 over all 49 nodes, `verify_mixins` `jars=71 problems=0`,
`verify_mixin_targets` **`jars=49 selectors=722 problems=0`** (705 + 17, one per Fabric node — the
number predicted before the run), `verify_convention_tags` `nodes=17 problems=0`, `bootgate.sh` rc=0
with all 17 Fabric nodes DONE.

##### 3b-5b — `ViewportEvent.RenderFog` (done)

Same mixin, same five-arm split, the other half of the fog row → `FabricClientEvents.fireRenderFog`.
The handler (`ClientEvents#onFogDensity`, `EventPriority.HIGH`) reads `getCamera().getFluidInCamera()`
and the current far plane, and writes near/far.

**The near/far mapping was read out of NeoForge's bytecode, not guessed** — `ViewportEvent$RenderFog`
maps `setNearPlaneDistance` → `FogData.environmentalStart` and `setFarPlaneDistance` →
`FogData.environmentalEnd`. The shared handler already runs against that mapping on NeoForge, so
reproducing it *is* the job; `renderDistanceStart/End`, `skyEnd` and `cloudEnd` are deliberately
untouched.

| nodes | target | how the planes are reached |
|---|---|---|
| `<1.21.2` | `setupFog(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/FogRenderer$FogMode;FZF)V` | `TAIL`; read `RenderSystem.getShaderFogStart()`/`getShaderFogEnd()`, re-push via the setters |
| `>=1.21.2 && <1.21.6` | `setupFog(…Lorg/joml/Vector4f;FZF)Lnet/minecraft/client/renderer/FogParameters;` | `RETURN` + `cancellable`; `FogParameters` is a **record**, so the value is *rebuilt*, not mutated |
| `>=1.21.6 && <1.21.11` | `setupFog(Camera;IZDeltaTracker;FClientLevel;)Lorg/joml/Vector4f;` | `@ModifyArgs` on the `updateBuffer(…)` call — args 3/4 are start/end |
| `>=1.21.11 && <26` | as above minus the `Z` | same `@ModifyArgs` |
| `>=26` | `setupFog(…)Lnet/minecraft/client/renderer/fog/FogData;` | `RETURN`; mutate `environmentalStart`/`environmentalEnd` |

`@ModifyArgs` + `Args` are **core Mixin, not MixinExtras** — deliberate, so no arm depends on a
MixinExtras version being bundled by whatever Fabric Loader a player happens to have. The camera is
taken from `GameRenderer#getMainCamera()` on the two `@ModifyArgs` arms (verified present on exactly
those versions, and **absent at 26.2**, where it is an argument again).

At 26.x, `setupFog` contains **no** `updateBuffer` call — the caller writes the buffer after it
returns (`areturn` at 173) — so mutating the returned `FogData` at `RETURN` is still in time.

⚠️ **This wave's real lesson is about the verifier, not the mixin.** The first gate came back
`jars=49 selectors=739 problems=6` — the `@ModifyArgs` arms "failing" on exactly 1.21.6 → 1.21.11,
Fabric only. The mixin was correct: `method_3211` really does invoke `method_71110` at offset 223 in
the very intermediary jar the verifier reads. The bug was in `verify_mixin_targets.py` —
**javap omits the owner prefix on a same-class reference**:

```
external:  // Method net/minecraft/client/Camera.getBlockPosition:()…
self-call: // Method updateBuffer:(Ljava/nio/ByteBuffer;…)V      ← no owner
```

`normalize_at_target` always builds the needle *with* the owner, so a self-call could never match.
`updateBuffer` is a **private method of `FogRenderer` itself** — the tree's **first self-call `@At`
target** across 54 injections, which is why the bug had never been reachable. Fixed by accepting the
owner-less form **only** when the `@At` owner is the `@Mixin` target, anchored to javap's
`Method `/`Field ` lead-in so a same-named member on a *different* class still fails (negative-tested
against wrong-owner, absent-target and wrong-descriptor). Selector count was **739 before and after**,
so the fix reclassified the six without reducing coverage.

**And the gate's own exit code was `0` while `vmt rc=1`** — the gate script's last command is a
`grep`, so its rc describes the grep. Rule 7 is not a slogan: read the per-step logs.

Gate `bic5v6t23` (+ verifier re-run): build rc=0 over all 49 nodes, `verify_mixins`
`jars=71 problems=0`, `verify_mixin_targets` **`jars=49 selectors=739 problems=0`** (722 + 17),
`verify_convention_tags` `nodes=17 problems=0`, `bootgate.sh` rc=0 with all 17 Fabric nodes DONE.

##### 3b-6 — `RenderNameTagEvent` (done, and Wave 3b with it)

`FabricNameTagMixin` → `FabricClientEvents.fireRenderNameTag`. The handler
(`ClientEvents#onRenderNameplate`) hides **the player's own** nameplate while the camera entity is an
`EntityBaldEagle`, singleplayer only. The only decision it ever makes is DENY.

**It injects into the callee, not Forge's call site.** Forge patches
`EntityRenderer.render`'s `if (shouldShowName(e)) renderNameTag(…)`. Injecting into the nameplate
method itself covers every vanilla renderer with one injection *and* picks up `RenderTiger`,
`RenderFarseer` and `RenderUnderminer` — the three renderers that fully override `render` without
calling `super`, and which Wave 3b-1 provably could not reach (see the divergence table below).

| nodes | target | entity from |
|---|---|---|
| `<1.20.5` | `renderNameTag(Entity;Component;PoseStack;MultiBufferSource;I)V` | param 0 |
| `>=1.20.5 && <1.21.2` | same **+ trailing `F`** (partial tick) | param 0 |
| `>=1.21.2 && <1.21.9` | `renderNameTag(EntityRenderState;Component;PoseStack;MultiBufferSource;I)V` | `AMStateAccess.entity(state)` |
| `>=1.21.9 && <26` | **`submitNameTag`**`(EntityRenderState;PoseStack;SubmitNodeCollector;state/CameraRenderState;)V` | `AMStateAccess.entity(state)` |
| `>=26` | **`submitNameDisplay`**`(EntityRenderState;PoseStack;SubmitNodeCollector;state/**level**/CameraRenderState;)V` | `AMStateAccess.entity(state)` |

The `1.20.5` boundary is rule 10 in miniature: the name does not change, only a trailing `F` is
appended, so a name-only survey passes and the injection silently targets the wrong era. At 26 there
are **two** `submitNameDisplay` overloads — a 4-arg entry point `submit()` calls and a 5-arg `final`
one it delegates to — so a name-only selector would match both and fire the hook twice per nameplate.
The 4-arg one is pinned by descriptor, resolved from bytecode.

**What this cannot do: force-ALLOW.** From inside the callee a nameplate can be suppressed but not
conjured, since vanilla only calls the method once it has decided to draw one. The mod never sets
ALLOW, so nothing is lost — but it is why this is not full parity with the Forge event.

⚠️ **The mixin was inert on its own, and two separate gates had to move with it.** `ClientEvents`'
veto line was gated `//? if forge || <1.20.6`, so on a Fabric node at MC ≥1.20.6 **neither** it nor
the NeoForge `TriState` arm was emitted — an empty guard body. Widening it to
`forge || fabric || <1.20.6` then broke the compile, because the `import
net.minecraftforge.eventbus.api.Event` near the top of the same file carried the **identical**
condition and was equally inert. Both now read that exact string and cross-reference each other;
they sit ~500 lines apart, which is why the first pass missed one. The stub's javadoc, which
documented the empty guard as an accepted divergence, was rewritten to match.

The other cost was self-inflicted and already in the notes: prose placed between `//?} else {` and
the arm's `/*` had its markers stripped and landed in the `26.2-fabric` projection as bare code —
the same trap that hit `registerFarseerStatic`, now recorded as a two-time offender in
[`stonecutter.md`](stonecutter.md).

#### Wave 4 — multipart entities and `ICustomCollisions` (done); one listed gap that was not real

Three files changed, two added. The wave was scoped as three parts and **shrank to two before any
code was written**, which is the part worth remembering.

##### The gap that wasn't: `IEntityWithComplexSpawn`

The deferred list asserted that the extra spawn data the 21 `AMPlatform.getEntitySpawningPacket`
callers send is not transmitted on Fabric. Measured instead of recalled:

```
grep -rn "IEntityAdditionalSpawnData|IEntityWithComplexSpawn"  -> 4 hits, ALL comments in AMPlatform
grep -rn "writeSpawnData|readSpawnData"                        -> 0 hits
grep -rn "AMPlatform.getEntitySpawningPacket"                  -> 21 hits
```

**No class implements the interface and neither hook method exists.** All 21 callers override
`getAddEntityPacket` purely to get a correct add-entity packet, and the `(neoforge || fabric)` arm
builds exactly that — the same arm **16 NeoForge nodes have shipped on**. So: no code, and the two
sentences in `AMPlatform`'s javadoc that asserted data was being dropped were corrected. Kept a ⚠️
there, because the gap becomes real the moment an entity gains genuine spawn data.

Worth stating plainly: a documentation defect that reads as a known-broken feature costs more than
one that reads as a missing note. It was on the "does not work yet" list for two waves.

##### Multipart parts, visible to the level

Parts are **never** in any level's entity storage on any loader. Forge and NeoForge patch vanilla to
keep them in a side map fed by the tracking callbacks and fold that map into world queries; vanilla
has the identical mechanism hard-typed to `EnderDragonPart`, so it is closed to us. `FabricLevel‑
MultipartMixin` injects at `Level#getEntities(Entity, AABB, Predicate)` RETURN and appends the
in-range parts of any nearby `IMultipartOwner`. That one query backs picking, `getEntityCollisions`
and most range lookups, so the cachalot's, giant squid's and laviathan's segments become pickable,
attackable and collidable in one place.

`FabricMultiPlayerGameModeMixin` is the client half: once a part is pickable the player can aim at
one, but the vanilla attack packet cannot carry it — the part's id does not exist server-side. So it
cancels the attack and reports the hit through `MessageHurtMultipart` with the **parent's** id.

⚠️ **That last sentence was true of the source and false of the game until *Wave 6*.** Wave 4 landed
the mixin, the gate went green, and nothing pointed out that `AlexsMobs.sendMSGToServer` had no
Fabric arm — so the mixin cancelled the vanilla attack and then sent nothing, making these three mobs
*less* attackable on Fabric than before it. The pattern to take from this: **a wave that ends in a
`sendMSGToServer` / `sendNonLocal` call has not been verified by a gate**, because the gate cannot
see whether the message arrived. Wave 6 supplies the transport; a client round-trip is what closes
this one.

⚠️ **The `@Shadow public abstract getEntities()` is deliberate — do not "clean it up".** Without
`@Shadow`, Mixin reads it as an implicit overwrite and conforms visibility against the target. The
Mojmap dev jar has it `protected` on every version this tree spans, but at runtime a *coexisting
mod's* access widener can promote it to `public`, and a `protected` overwrite then hard-crashes with
`cannot reduce visibiliy of PUBLIC target method` before the main menu. AlexsMobsFP paid for this on
2026-07-26; the comment is in the file.

⚠️ **The trap FP did not have, and this repo does.** FP is single-MC-version, so its
`MessageHurtMultipart` change was free. Here that file is **shared source projected into all 49
nodes**, and its C2S path is *already live on Forge* — `EntityCachalotPart` and `EntityGiantSquidPart`
send real damage and a real damage type, and there the part id *does* resolve. So the new
`serverPlayer.attack(parent)` branch is gated `//? if fabric`; the 32 Forge/NeoForge nodes see
byte-identical behaviour. Its condition is `holder == null`, which is already how the handler spells
"nothing here is a damage relay" — **not** a `damage == 0 && damageType.isEmpty()` sniff, which would
be a sentinel encoded in a wire format that silently aliases a real zero-damage message.

The upside of FP's shape is worth keeping: the server runs a **full vanilla player attack**, so
cooldown, knockback, enchantments and sweep all apply and no client-supplied damage number is trusted.

##### `Entity#collide`

Went into the **existing** `FabricEntityMixin` rather than a new class — same `@Mixin(Entity.class)`
target, one fewer file. `@Inject(HEAD, cancellable)`, returning
`ICustomCollisions.getAllowedMovementForEntity` when `(Object) this instanceof ICustomCollisions`.
No recursion: that helper runs the vendored `collideBoundingBox2` and never calls `collide` back —
checked rather than assumed, because that exact shape is what made `getEntitySpawningPacket` recurse
into its own callers on NeoForge.

⚠️ **The two `//? if !fabric` gates on `EntityTiger#collide` / `EntityRockyRoller#collide` STAY.**
The first draft of the apply plan said to delete them; that is wrong and would break all 17 Fabric
compiles. Vanilla's `collide` is private on Fabric, so the override cannot exist there no matter what
the mixin does. Only the comments changed — they now point at the mixin instead of saying "until that
lands".

##### Prior art

All three mixins are **ports from `AlexsMobsFP`**, which has been running them in production. Two
naming differences: FP's `IMultipartEntity` is this repo's `IMultipartOwner`, and FP's
`private static final` reach constant became a method-local (this tree's mixins avoid state).

Gate `b3c0gqih1`: build rc=0 over all 49 nodes, `verify_mixins` `jars=71 problems=0`,
`verify_mixin_targets` **`jars=49 selectors=756 problems=0`** (739 + 17, the number predicted before
the run), `verify_convention_tags` `nodes=17 problems=0`, `verify_assets` `literals=394 missing=0`.

#### Wave 5 — loot, structures, block break, brewing, armour models (done; believed to be the last wave, wasn't)

Five items, seven new mixins, three new classes, and **zero** new dependencies. The wave's own
recurring lesson, paid four times out of five: **the Fabric API hook the plan named was the wrong
tool every time**, either because it does not exist across this range or because it cannot express
what the mod does. Check the pinned `fabric-api` jar for the version range **before** planning around
an API — `unzip -l <fabric-api jar>` then the nested module jar, the same probe that settled the
`EntityDataSerializer` boundary above.

##### The four global loot modifiers — `FabricLootTableMixin` + `misc/AMLootModifiers`

Bananas from jungle leaves, acacia blossoms from acacia leaves, ancient darts in jungle-temple
chests, pigshoes from piglin bartering. On the other two loaders all four are **datapack** entries:
`global_loot_modifiers.json` dispatches, and each modifier's json carries a `forge:loot_table_id`
condition Forge evaluates against a queried-table-id it patches onto `LootContext`. **All three
pieces are missing on Fabric** — no dispatch file, no condition type, no id on the context.

- Not `LootTableEvents.MODIFY_DROPS` (what FP uses): it **only exists from 1.21.6**, measured across
  all 17 pinned `fabric-api` versions. Below that the only hook is `MODIFY`, which fires at
  table-load time and hands out a builder — that cannot express "roll a fortune-scaled chance against
  the tool that broke this block". Nor is there one API module spanning the range: `loot v2` is gone
  by 26.1.2, `v3` absent before 1.21.
- So: `@Inject(RETURN)` on `LootTable#getRandomItems(LootContext)`, which is **private** and targeted
  deliberately — it is where the three call paths that matter converge (block drops, bartering, chest
  loot). Descriptor byte-identical on all 17 nodes, so no arms.
- ⚠️ **A known limit, written at the site:** entity death drops take the `Consumer` overloads and
  reach `getRandomItemsRaw` directly, so they never pass through here. None of the four modifiers
  targets an entity table. A fifth one that does needs a second injection **and** care not to
  double-apply.
- **The condition is inverted rather than re-implemented.** Every one of the four conditions is a
  `loot_table_id` test and nothing else, so `AMLootModifiers` keys a `LinkedHashMap` on the table id
  and asks up front. That is why `resolve` hands back the modifier's `doApply` and not its `apply`:
  `apply` would re-test a `conditions` array that is necessarily empty here, and **an empty
  or-of-conditions is always false** — it would have silently dropped every drop. `AMLootModifiers`
  lives in `misc/` precisely so it shares the modifiers' package and can reach their `protected`
  `doApply`.
- ⚠️ The four table ids are **restated in code** next to the json that still drives Forge. Nothing
  pairs them; a fifth modifier means editing both.

##### The four structure spawn overrides — `FabricStructureMixin` + `fabric/world/FabricStructureSpawns`

Mimicubes in end cities, soul vultures at nether fossils, skelewags at shipwrecks, underminers in
anything tagged `alexsmobs:spawns_underminers`. Fabric has **no structure-modification hook of any
kind** (`BiomeModifications` is biomes only), so instead of rewriting `StructureSettings` at
dynamic-registry load, the overrides are merged at **read** time: `@Inject(RETURN)` on
`Structure#spawnOverrides()`, the only method through which a structure's overrides are ever read.
Same injection point FP uses. Concrete method on the abstract base, so one mixin covers modded
structures too; identical signature on all 17 nodes.

- **The config conditions are not duplicated.** This driver calls the same
  `AMWorldRegistry.modifyStructure` the other two loaders call, against the same vendored
  `ModifiableStructureInfo` recorder, and reads back what it recorded. FP grew a parallel
  `AMStructureSpawns` restating every `AMConfig` check — two copies that can disagree. Adding a fifth
  override needs no change here.
- ⚠️ **Forge's merge semantics had to be reproduced exactly**, and the default is counter-intuitive.
  Forge seeds its builder with `StructureSettingsBuilder.copyOf` (verified against NeoForge's
  bytecode), so for a category the structure already overrides you keep vanilla's entries *and*
  vanilla's `BoundingBoxType` and append; for one it does not, Forge creates the builder with
  **`PIECE`**. `STRUCTURE` is the intuitive guess and is wrong — it would let these mobs spawn
  anywhere in the bounding box rather than only inside pieces.
- Read on every spawn attempt, hence the per-structure cache on the driver side, published to the
  chunk-generation threads with a `volatile`.

##### Block break — two jobs, two seams (not FP's one `@Redirect`)

FP `@Redirect`s `hasCorrectToolForDrops` inside `ServerPlayerGameMode#destroyBlock`, covering both
with one injection. Split here on purpose:

- **Ghostly pickaxe** — `FabricPlayerMixin` fires `PlayerEvent.HarvestCheck` from `Player`'s own
  `hasCorrectToolForDrops`, so it reaches drops, mining speed **and** the client's crack overlay, not
  just `destroyBlock`.
- **Transmutation table's explode-on-break** — Fabric API's `PlayerBlockBreakEvents.AFTER`, from
  `FabricServerEvents.registerBlockBreak`. Not a `ServerEvents` hook at all on any loader: Forge and
  NeoForge get it from overriding the loader's `Block#onDestroyedByPlayer`, which vanilla has no
  equivalent of. `AFTER` is server-side only, which is what an explosion wants anyway.

##### Brewing — four mixins, and the API hook used for none of it

Seventeen recipes; **four of them Fabric API structurally cannot express**. Its
`FabricPotionBrewingBuilder` only exposes vanilla's two shapes — `registerPotionRecipe` (contents
change, container preserved) and `registerItemRecipe` (container changes, contents preserved) — while
`lava_bottle → lava-vision potion`, `poison potion → poison bottle` and the two
`*_bottle → poison-resistance` mixes each change **both** in one step. And the interface only exists
from 26.1.2 (it was `FabricBrewingRecipeRegistryBuilder` before, and absent on 1.20.1/1.20.4). So
`AMEffectRegistry.addBrewing` collects into `FabricBrewing` and four mixins consult it from the same
places Forge patches:

| mixin | target | job |
|---|---|---|
| `FabricPotionBrewingBuilderMixin` | `PotionBrewing$Builder#build()` | `reset()` + re-run `registerBrewingRecipes` per server |
| `FabricPotionBrewingMixin` | `isIngredient` / `hasMix` / `mix` | the three methods every brewing path funnels through |
| `FabricBrewingStandMenuMixin` | `BrewingStandMenu$PotionSlot#mayPlaceItem` | bottom slots accept the non-potion inputs |
| `FabricBrewingStandBlockEntityMixin` | `BrewingStandBlockEntity#canPlaceItem` | the hopper copy of the same check |

- ⚠️ **`hasMix(input, ingredient)` and `mix(ingredient, input)` take their two `ItemStack`s in
  opposite orders** — vanilla's own inconsistency, confirmed from `isBrewable`/`doBrew` bytecode on
  1.20.1 and 1.21.5 alike. Two same-typed parameters swapped is a silent behaviour bug, never a
  compile error.
- `hasMix` had to be a RETURN **override**, not a refinement: vanilla bails out before looking at
  anything when the bottle is not one of its own three potion items, which is exactly the case for
  all three custom bottles.
- `canPlaceItem` inlines `PotionSlot`'s four-item check rather than delegating — on every version
  1.20.1→26.2 — which is why the same acceptance test is written twice.
- The `>=1.20.5` era split is static-versus-instance and nothing more (1.20.5 made brewing per-server),
  so those arms have identical bodies. Below 1.20.5 `FabricPotionBrewingBuilderMixin` is inert and
  targets `PotionBrewing` only so the class has a target that exists; the list is filled once from
  `AMEffectRegistry.init()`.
- `reset()` before each `build()` matters: `build()` runs once for `PotionBrewing.EMPTY` and again per
  server, and the list is global — without it the list grows a copy per world load.

##### Custom armour models — `fabric/client/FabricArmorRenderers`, and **no mixin at all**

The one item the plan expected to need era-armed mixin surgery. It needed none: **Fabric API's
`ArmorRenderer` is the loader's own seam for exactly the job** Forge does with
`IClientItemExtensions#getHumanoidArmorModel`. It is handed the armour model vanilla would have
drawn — which is precisely the Forge hook's `_default` argument — so both loaders feed the same
lookup, `CustomArmorRenderProperties#getHumanoidArmorModel`. Fifteen items registered, thirteen with
a hand-built model.

- **It fixes the armour *textures* below 1.21.2 as a side effect.** Alex's Mobs keeps its skins at
  `textures/armor/<item>.png` rather than the vanilla `textures/models/armor/` layout and redirects
  vanilla there through Forge's `getArmorTexture`, which Fabric never had — so every one of the
  fifteen rendered untextured. A renderer names its own texture, so the redirect stops being needed.
  From 1.21.2 vanilla already resolves the same PNG through the generated equipment model
  (`DataPackMigration#migrateEquipmentTo12102`), so there this only adds the models.
- Three era arms, matching the three shapes the interface has had: `LivingEntity` wearer `<1.21.2`;
  `HumanoidRenderState` `1.21.2–1.21.8`; `SubmitNodeCollector` instead of `MultiBufferSource`
  `>=1.21.9`.
- ⚠️ **`ArmorRenderer.renderPart` was deleted at 1.21.9 and its replacement
  `submitTransformCopyingModel` only arrived at 1.21.10** — so 1.21.9 has *neither*, and the top arm
  draws itself through `AMSubmitBuffers` (the same recorder the ~130 legacy render bodies use)
  rather than splitting into a fourth arm.
- ⚠️ **Not `SubmitNodeCollector#submitModel`**, which renders the model object *later* in the frame:
  these thirteen models are shared statics whose part poses and visibility are rewritten per wearer.
- Three more deletions handled without an arm each: `copyPropertiesTo` is gone at 1.21.9 (the top arm
  uses `setupAnim(state)`, which recomputes the same pose from the render state); `setAllVisible` is
  gone at 26.1 (the seven `ModelPart`s are `public final` on all 17 nodes, so vanilla's private
  `HumanoidArmorLayer#setPartVisibility` is reproduced field-by-field); `Model#renderToBuffer` is
  8-arg below 1.21 and 5-arg above, and only the 5-arg form is reachable from a vanilla
  `HumanoidModel` subclass.
- ⚠️ **`ClientEvents.java:257` looks like it contradicts that last point and does not.** Its
  ungated 8-arg `ROCKY_CHESTPLATE_MODEL.renderToBuffer(...)` resolves to a compat-`EntityModel`
  subclass that declares the legacy overload itself — javap the built class before concluding a
  replacement rule is missing.

##### Wave-5 gate (2026-08-01) — the full eight steps, all green

| Step | Result |
|---|---|
| 49-node `:build`, one invocation | ✅ `BUILD SUCCESSFUL in 6m 3s` |
| `verify_mixins.py` | ✅ `jars=49 problems=0` (`26.2-fabric declared=28`) |
| `verify_mixin_targets.py` | ✅ `nodes=49 jars=49 selectors=958 problems=0 skipped=0` |
| `verify_assets.py` | ✅ `literals=394 missing=0` |
| `verify_convention_tags.py` | ✅ `nodes=17 problems=0` |
| `aw_check.py` × 17 | ✅ `problems=0` on all 15 obfuscated MCs (26.1.2/26.2 have no named jars to check — unobfuscated, expected skip) |
| `SOAK=45 bootgate.sh` × 49 | ✅ **49/49 `DONE`, `rc=0`** |
| `SOAK=20 JOBS=4 clientgate_par.sh` × 49 | ✅ **49/49 `READY`, `rc=0`**, zero crash-reports |

⚠️ **The `selectors=958` total is NOT comparable with Wave 4's `756`** — that figure predates the
SNAPSHOT-glob fix documented at `verify_mixin_targets.py:504`, which changed which jars the script
opens at all. **Predict and compare the per-node counts instead**, which is what rule 6 actually
needs: Fabric is `38 / 37 / 38 / 38 / 38`, then `40` on the nine mid-range nodes, then `39 / 39` on
26.1.2 / 26.2. Wave 5's armour work added **no** selectors and those numbers were unchanged from the
mid-wave brewing run, which is the prediction that mattered.

#### Wave 6 — networking (2026-08-04): the gap no audit had on its list

**Fabric had no network transport at all**, from Milestone 15 through `2.0.3`. `AlexsMobs` funnels
every packet through three methods, and each had `!fabric` arms only:

| method | on Forge / NeoForge | on Fabric, before this wave |
|---|---|---|
| `registerMessage(id, clazz, encoder, decoder, handler)` | `SimpleChannel.messageBuilder(...)` / `AMNeoNetwork.register` | nothing |
| `sendMSGToServer(msg)` | `channel.sendToServer` / `AMNeoNetwork.sendToServer` | nothing |
| `sendNonLocal(msg, player)` | `channel.send(PacketDistributor…)` / `AMNeoNetwork.sendToPlayer` | nothing |

So all ~22 messages were **silently dropped in both directions**. Every downstream feature that
crosses the wire was dead: the falconry loop (report **#22**), multipart attacks (Wave 4, above), the
transmutation table, mob-command GUIs, tamed-mob orders, the animal dictionary's server-side bits.

**Why nothing caught it.** An empty method compiles, so the compiler is blind; a boot gate reaches
`Done` without a client ever sending anything, so both gates are blind; and every *call site* is
shared source that reads correctly on all three loaders, so a source audit is blind. The general
rule, worth carrying to the next loader: **a "does it work on Fabric" audit must be organised by
platform seam, not by feature** — enumerate what the shared code calls out to (`AMCompat`, `AMPlatform`,
the network hooks, the proxies) and check each has a live arm, because a feature list only ever
contains features somebody already thought of.

##### The design: one wrapper payload, index-dispatched

`fabric/network/AMFabricNetwork` is the counterpart of Forge's `SimpleChannel` and of `AMNeoNetwork`,
and it is deliberately shaped like the latter. One channel, `alexsmobs:main_channel`, carrying
`{varint index, message body}`; the index is the **registration order from `AlexsMobs#setup`**, which
is shared source and therefore identical on both sides. That keeps all ~22 message classes
byte-identical across all three loaders — no per-message `CustomPacketPayload`, no per-message
`StreamCodec`, nothing to add when a message is added.

`AMFabricNetwork.init()` must run **after** `new AlexsMobs()` in `AlexsMobsFabric#onInitialize`:
`setup` is what fills the registration list the receiver dispatches on. The client half is a separate
class, `fabric/client/FabricClientNetwork`, for one hard reason: **`ClientPlayNetworking` is
client-only API** — Fabric strips it from a dedicated server, so merely *naming* it from a
server-loaded class is a `NoClassDefFoundError`. It installs itself as `AMFabricNetwork`'s serverbound
sink, so the shared `sendMSGToServer` has something to call and a dedicated server has a `null` sink
that logs rather than crashes.

Both halves adapt to `message/AMNetContext`, the same four-method view the NeoForge path uses
(`setPacketHandled`, `enqueueWork`, `getSender`, `isClientSide`).

##### Three API eras, all javap-measured against the pinned `fabric-api` jars

Do **not** take these from memory or from the wiki; they were read off the jars, and the third one is
a rename that no changelog announces loudly.

| nodes | registry | send | receive |
|---|---|---|---|
| `<1.20.5` | none — raw channels | `ServerPlayNetworking.send(player, rl, FriendlyByteBuf)` | `receive(server, player, listener, buf, sender)` |
| `>=1.20.5 && <26.1` | `PayloadTypeRegistry.playS2C()` / `.playC2S()` | `send(player, CustomPacketPayload)` | `PlayPayloadHandler<T>.receive(T, Context)` |
| `>=26.1` | `PayloadTypeRegistry.clientboundPlay()` / `.serverboundPlay()` | same | same |

The `>=26.1` row is a **pure rename** — javap-verified: both spellings are static, no-arg, and return
`PayloadTypeRegistry<RegistryFriendlyByteBuf>`. So it is handled by the repo's standing convention for
Fabric API drift (write the **newer** spelling in source, hop back for older nodes), not by a third
arm: two rules `!fabapi-payload-s2c` / `!fabapi-payload-c2s` in the existing
`-fabric && !>=26` replacement group in `stonecutter.gradle.kts`. They are keyed on the receiver
(`PayloadTypeRegistry.clientboundPlay()`, not the bare word) so nothing else can claim the offset.
This also sidesteps the fact that **Stonecutter blocks never nest** — a `>=26.1` gate could not have
lived inside the `>=1.20.5` payload arm. Below 1.20.5 the arm naming the registry is commented out
entirely, and a rewrite inside a commented arm is harmless, so the group needs no second bound.

##### Two traps, one per era

⚠️ **`RegistryFriendlyByteBuf`, and *when* you encode** (`>=1.20.5`). This is bug report **#24** in a
different costume: `ItemStack.OPTIONAL_STREAM_CODEC` requires a `RegistryFriendlyByteBuf`, and a
hand-allocated `FriendlyByteBuf` can never be cast to one. So the payload arm carries the **message
object**, not a pre-encoded buffer, and encodes late — inside the `StreamCodec`, into the connection's
own registry-aware buffer. Encoding eagerly at send time is the exact mistake that crashed NeoForge.

⚠️ **The raw arm's handler runs on the netty thread, and the buffer dies with the call** (`<1.20.5`).
Both raw receivers therefore **decode immediately** and defer only the *handler* through
`AMNetContext.enqueueWork` (`server.execute` / `client.execute`). Deferring the decode instead reads a
freed buffer. The payload arms have the opposite property — they arrive on the game thread already —
but they go through the same `enqueueWork` so the handlers stay identical; `BlockableEventLoop.execute`
runs inline when it is already on the right thread, so that costs nothing.

##### Verification

All 49 nodes compile in one invocation. **Verify the per-node arm at the bytecode level, not by
grepping the generated source** — an inactive Stonecutter arm is still *present* as a comment, so a
grep reported `1` match on every node and proved nothing. `javap -c -p` on the compiled classes gave
the true picture: raw receiver on 1.20.1 / 1.20.4; `playS2C`+`playC2S` on 1.20.6 → 1.21.11;
`clientboundPlay`+`serverboundPlay` on 26.1.2 and 26.2. Every Fabric node's `sendNonLocal` calls
`AMFabricNetwork.sendToPlayer`, and every one has a `FabricClientNetwork` with a
`ClientPlayNetworking.send`.

**A real client round-trip is still owed** — the boot gate cannot send a packet. The cheapest single
check that exercises both directions is perching a bald eagle and left-clicking (#22).

#### Fabric-only behaviour divergences, and where each is written down

Every one of these compiles, boots and is **silent** — no gate step can see any of them. Each is
commented at its own site with the same reasoning; this is the index, not the explanation.

| What | Where | Nodes |
|---|---|---|
| ~~Brewing recipes collected but never consulted~~ — **closed in *Wave 5***, four mixins drain `FabricBrewing`. What remains is that the seventeen recipes are **invisible to other mods**: they are in this list, not in `PotionBrewing`'s tables, so a recipe-viewer or another mod's brewing patch cannot see them | `fabric/common/brewing/*` | all Fabric |
| ~~The four loot modifiers never run~~ — **closed in *Wave 5*** (`FabricLootTableMixin`). What remains: they fire only on the `getRandomItems(LootContext)` path, so a **fifth** modifier targeting an *entity* loot table would silently not run | `misc/AMLootModifiers` | all Fabric |
| ~~Structure spawn overrides never apply~~ — **closed in *Wave 5*** (`FabricStructureMixin`). What remains: they are merged at read time rather than baked into `StructureSettings`, so another mod reading the settings record directly does not see them | `fabric/world/FabricStructureSpawns` | all Fabric |
| No `RenderLivingEvent.Pre/.Post` on these three: no mod can cancel or restyle their renders. ⚠️ **Wave 3b-1 did not fix this and could not** — `FabricLivingEntityRendererMixin` injects into `LivingEntityRenderer#render`/`#submit`, and all three **fully override** `render` without calling `super` (verified: their only `super` call is the constructor), so the mixin never runs for them. The three Fabric arms in `AMRenderEventCompat` (`firePre` → `false`, `firePost` → nothing) are the live path on `>=1.21.2`, and the hand-written `elif fabric` arms in the renderers themselves on `<1.21.2`. **`RenderNameTagEvent` is no longer part of this row** — Wave 3b-6 targets the nameplate *callee*, which these three call directly (`this.renderNameTag(...)`), so the event does fire for them | `RenderTiger`, `RenderFarseer`, `RenderUnderminer` | all Fabric |
| Void-worm drops are not repositioned to the worm's head | `EntityVoidWorm.tickDeath` | all Fabric |
| No fill-bucket event — another mod cannot veto or redirect emptying the cosmic cod bucket | `ItemCosmicCodBucket` | all Fabric |
| ~~Custom armour **texture** path and armour **model** hook are Forge `IClientItemExtensions`~~ — **closed in *Wave 5***: `fabric/client/FabricArmorRenderers` supplies both, so `ItemModArmor`'s `!fabric` `getArmorTexture` overrides no longer have a Fabric-side gap to leave | `ItemModArmor` | — |
| ~~The **other** half of `IClientItemExtensions` — `getCustomRenderer`, the ISTER — was never wired either, and unlike the armour half it was **not** recorded here. Eleven items whose model is `builtin/entity` therefore drew *nothing*: an invisible inventory slot that still has a name. Shipped that way from Milestone 15 through `2.0.3`~~ — **closed 2026-08-03** (bug report **#23**): `fabric/client/FabricItemRenderers` walks the item registry and hands the ones asking for an `AMItemRenderProperties` to Fabric API's `BuiltinItemRendererRegistry`. ⚠️ Gated `<1.21.4` — that registry is gone from 1.21.4 up (measured across all 17 pins), where the models are rebuilt at build time instead | `FabricItemRenderers`, `AMItemstackRenderer` | Fabric `<1.21.4` |
| No `getArmorTexture` hook, so a **third** mod has no seam to re-point armour textures this mod resolves for a mob (the kangaroo's and mimicube's worn vanilla armour). The path is used exactly as built | `LayerKangarooArmor`, `LayerMimicubeHelmet` | all Fabric |
| ~~The tarantula-hawk elytra does not glide~~ — **closed 2026-08-07** (bug report **#44**), and this row had been **understating the gap the whole time**: the `minecraft:glider` component never "covered it from 1.21.2" because nothing ever *attached* the component, so the elytra was dead on all ≥1.21.2 nodes of **every** loader too (Forge declares `canElytraFly`/`elytraFlightTick` there but no patched class calls them — bytecode-swept). Now: `AMCompat.glider` attaches the component at registration on ≥1.21.2, and Fabric `<1.21.2` gets an `EntityElytraEvents.CUSTOM` handler in `AlexsMobsFabric` mirroring `elytraFlightTick`'s 20-tick drain + the 10-tick `ELYTRA_GLIDE` game event | `ItemTarantulaHawkElytra`, `AMCompat.glider`, `AlexsMobsFabric` | — |
| Reach and swim-speed armour modifiers are dropped where vanilla has no equivalent attribute — the piece still builds, minus that one modifier | `AMPlatform`, `ItemModArmor` | Fabric `<1.21` (swim), `<1.20.5` (reach) |
| The straddleboard **is** enchantable with Unbreaking and Mending | `ItemStraddleboard` | Fabric `<1.21` |
| The four straddle enchantments are never offered by the enchanting table (`isDiscoverable()` is hard false). They remain obtainable via villager book trades + anvil, which ask `canEnchant(ItemStack)` — overridden to test the board | `AMEnchantmentRegistry`, `StraddleEnchantment` | Fabric `<1.20.5` |
| The five leaf-dwellers use `NO_RESTRICTIONS` instead of the custom leaves placement, so their position check is skipped; their own `canXSpawn` predicates still gate light/biome/difficulty | `AMEntityRegistry.PLACE_ON_LEAVES` | Fabric `<1.20.5` |
| The ghostly pickaxe's model is not wrapped — no `BakedModelWrapper`, so it renders as its plain baked model | `GhostlyPickaxeBakedModel`, `ClientProxy` | all Fabric |
| The jerboa begs for vanilla `minecraft:villager_plantable_seeds` rather than `c:seeds`, so **modded** seeds do not trigger it. ⚠️ The boundary is the pinned **fabric-api**, not the MC version | `fabric/common/Tags.Items.SEEDS` | Fabric `<1.21.4` |
| An elephant accepts any `c:chests` — including ender and trapped chests — as a howdah; convention-tags v1 has no wooden-only split | `fabric/common/Tags.Items.CHESTS_WOODEN` | Fabric `1.20.1`, `1.20.4` |
| The five `AMRenderTypes` get no entry in `RenderBuffers.fixedBuffers` and fall back to the shared builder — the entrypoint runs before `renderBuffers` exists. Identical to what `>=1.20.2` does on every loader | `ClientProxy.initRainbowBuffers` | Fabric `1.20.1` |
| Persistent entity NBT is the vendored Citadel `LivingEntity` tag, not a Forge-style store: it is **synched to the client**, and a non-`LivingEntity` gets a throwaway tag instead of a persisted one. Neither reaches the single caller (a server-side boolean on a `ServerPlayer`). Death is covered — `FabricServerPlayerMixin` copies the sub-tag in `restoreFrom`, as Forge's patch does | `AMCompat.getPersistentData` | all Fabric |
| `isScaffolding` answers for the end-pirate anchor and vanilla scaffolding only; a third-party block that overrides Forge's `isScaffolding` is not recognised as climbable | `AMCompat.isScaffolding` | all Fabric |
| Soulsteal heals off the **pre-armour** damage figure. Forge fires `LivingDamageEvent` after armour and magic absorption; the mixin is at `actuallyHurt` HEAD, because an `@Inject` cannot rewrite the target's `float` parameter anyway and the mod only ever cancels here or reads the amount. The heal is clamped to `2 + 2 × level` either way, which is what keeps it small. The two cancelling listeners (mimic octopus, emu leggings) are boolean and unaffected | `mixin/fabric/FabricLivingEntityMixin` | all Fabric |
| `LootingLevelEvent` is not fired, so a snow-leopard kill does not get its **+2 looting bonus**. Deliberately not reproduced: it is already dropped on NeoForge `>=1.21` (looting became an enchantment *effect*), so Fabric matches the majority of the tree rather than the 1.20.x minority | `ServerEvents.onLootingLevel` | all Fabric |
| A mob under Debilitating Sting (amplifier > 0) also survives **peaceful-mode** cleanup for the duration of the effect, and its `noActionTime` is not reset while protected — so once the effect ends it may despawn a little sooner than on Forge. Both fall out of cancelling `checkDespawn` whole: Forge fires `AllowDespawn` inside the non-persistent branch only, and replicating that needs `shouldDespawnInPeaceful`, which vanilla deleted at **1.21.9** | `mixin/fabric/FabricMobMixin` | all Fabric |
| `MobSpawnEvent.FinalizeSpawn` fires from `Mob#finalizeSpawn` itself, where Forge fires from the **call sites** it patched. So it also fires for calls Forge does not wrap — including the mod's own `squid.finalizeSpawn` in the lightning handler. Harmless as written (the only listener acts on a `WanderingTrader`), but it is the reason to keep that listener narrow | `mixin/fabric/FabricMobMixin` | all Fabric |
| `EntityEvent.Size` is handed the **new** `EntityDimensions` where Forge hands it the previous ones. TAIL is the only injection point where the recomputed eye height is available, and the two differ solely on the tick a pose changes — where the handler's `height - eyeHeight` plainly wants the hitbox the eyes are being placed in | `mixin/fabric/FabricEntityMixin` | Fabric `1.20.1` |
| `giveItemToPlayer` has no preferred-slot pass and the dropped entity keeps its normal pickup delay. Invisible for its only caller — a first-login book into an empty inventory | `fabric/items/ItemHandlerHelper` | all Fabric |

The last two exist because **`EnchantmentCategory` and `SpawnPlacements.Type` are plain enums below
1.20.5**. Forge patches an extensible-enum `create(...)` onto both; Fabric has no equivalent, and
neither one can be widened by an access widener because the problem is the enum, not access. The
generic escape in both cases is a placeholder constant plus an override that answers the question the
placeholder would have answered — and where the caller reads the *field* rather than an overridable
method (the enchanting table does exactly that), the honest move was to switch the feature off rather
than let the placeholder leak onto unrelated items.

## Adding Fabric later — where the cost actually is

> Superseded in part by Milestone 15 above, which is the live status. This section is the **estimate made
> before starting** and is kept because its measurements and its two traps are still the plan of record.

The user's plan (2026-07-26): every version in this tree should eventually run on **Fabric** as well, once
the Forge/NeoForge line is finished. Estimated before starting, so the shape is on record:

**The cost is ~all one-time, not per-version.** A Fabric *node* is cheap — the entire vanilla-API migration
(the eight milestones above) is loader-neutral and already gated by MC version, so a new Fabric node
inherits it. What Fabric needs is a **loader-divergence layer** that does not exist in this tree at all,
because this tree *is* Forge code. Once that layer exists, each additional Fabric node is roughly what a
NeoForge node costs today (a dependency pin + a compile-fix pass).

**Measured surface in this repo** (`src/main/java`, 861 files):

| | count |
|---|---|
| files importing `net.minecraftforge.**` | 185 |
| …of which the *only* import is `api.distmarker` (`@OnlyIn`) — trivial | 78 |
| files with a **non-trivial** Forge dependency | **107** |
| `@SubscribeEvent` handlers / distinct Forge event types | **65 / 63** |
| `event/ServerEvents.java` | 1,065 lines |
| access-transformer entries → need an `.accesswidener` | 35 |

The 107 concentrate in `entity` (22), `message` (17), `item` (16), `misc` (12). The subsystems with **no
Fabric equivalent** — each is a build, not a rename: multipart entities (`PartEntity`), biome + structure
spawn modifiers (datapack-driven on Forge/NeoForge, Java `BiomeModifications` on Fabric — 88 entries),
`ForgeConfigSpec`, `SimpleChannel`, capabilities/`IItemHandler`, `FluidType`, `IClientItemExtensions`/ISTER
(16 files), global loot modifiers, and ~⅔ of those 63 events (Fabric API covers maybe a third; the rest are
mixins). **Vendoring Citadel already paid off here** — Fabric has no Citadel at all, and this tree no longer
needs one.

**Empirical evidence from the sibling repo, which already did this once.** `AlexsMobsFP` reached Fabric on a
single MC version via a hand-written `net.minecraftforge.**` shim: `codx/AMUP/fshim` **97 files / 2,262
lines** + `codx/AMUP/mcshim` 32 files / 943 lines + a `fabric` module of **37 files / 2,461 lines (29
mixins)**. It has been at it since ~June 2026 and is **still not 1:1**: `ROADMAP_FABRIC_1TO1.md` has M2 (the
event subsystem, rated **L**), M3 (65 classes, **L**), M4 (GUIs, **M–L**) and M5 open, and 153 `// SLICE:`
markers remain in `common`. So: **one-time L–XL, on the order of two to three of the milestone waves above**
— weeks, not days. Per-version afterwards: **S**, rising to **M** only where Fabric's *own* API changed
shape (1.20.1's old Fabric API; the loader-floor trap below).

**Do NOT copy FP's shim architecture.** FP's own roadmap records why it is a dead end when the real Forge
APIs must coexist: `:neoforge:compileJava` fails there because the shim collides with the genuine Forge
classes (`IShearable` vs `IForgeShearable`). Here Forge and NeoForge are first-class, so a shim is not an
option. Nor should Fabric become a third Stonecutter loader dimension in those 107 files — the version axis
is already 13 deep. The right seam is the one that **already exists**: extend `misc/AMPlatform` (273 lines)
and `misc/AMCompat` (1,714 lines) into a real platform interface with a `fabric` source set, keeping the 107
files loader-neutral and leaving Stonecutter to the version axis.

**Ordering.** Build Fabric **once against the newest node** (where FP's Fabric code is closest to liftable),
verify it, then back-fill Fabric nodes *downward* — the loader deltas are stable across MC versions, so
downward is the cheap direction. Do not interleave Fabric per version as the Forge/NeoForge line advances.

**Two traps already known from this workspace:** the Fabric loader floor in a multi-version manifest must be
**per-MC-version**, never the build pin (the codxlib `1.3.3`/OneBlock `3.3.0` crash — read the pinned
`fabric-api` jar's own `depends.fabricloader`, and don't guess: the values are non-monotonic); and a Fabric
mixin that matches a target method **without `@Shadow`** is an implicit overwrite that passes in dev and
dies in real packs when another mod's access widener promotes the target (FP's `1.0.5` boot crash).

**Scope note.** `AlexsMobsFP` was declared **Fabric-only** on 2026-07-26 and publishes to the same Modrinth
slug (`alexs-mobs-continued`) that this repo targets. Adding Fabric here therefore needs a decision about
which repo owns the Fabric jars — resolve that before building, not after.

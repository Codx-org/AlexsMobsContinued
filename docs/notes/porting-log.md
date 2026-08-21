# Porting log — Milestones 0 to 15

> Historical record, one section per wave. Read the entry for a version before re-touching it; you do not need this file to do ordinary work.
>
> Part of the Alex's Mobs Continued porting notes.

## Contents

- [✅ Milestone 0 — `1.20.1-forge` compiles the pristine source](#milestone-0--1201-forge-compiles-the-pristine-source)
- [✅ Milestone 1 — Citadel vendored, dependency removed](#milestone-1--citadel-vendored-dependency-removed)
- [✅ Milestone 2 — `1.20.4` (Forge & NeoForge)](#milestone-2--1204-forge--neoforge)
- [✅ Milestone 3 — `1.20.6` (Forge & NeoForge) — DataComponents](#milestone-3--1206-forge--neoforge--datacomponents)
- [✅ Milestone 4 — `1.21` + `1.21.1` (Forge & NeoForge)](#milestone-4--121--1211-forge--neoforge)
- [✅ Milestone 5 — `1.21.2` + `1.21.3` (render-state rewrite)](#milestone-5--1212--1213-render-state-rewrite)
- [✅ Milestone 6 — `1.21.4` (Forge & NeoForge) — item model definitions](#milestone-6--1214-forge--neoforge--item-model-definitions)
- [✅ Milestone 7 — `1.21.5` (Forge & NeoForge) — MEGA-WAVE](#milestone-7--1215-forge--neoforge--mega-wave)
- [✅ Milestone 8 — `1.21.6` (Forge & NeoForge) — ValueInput/ValueOutput + EventBus 7](#milestone-8--1216-forge--neoforge--valueinputvalueoutput--eventbus-7)
- [✅ Milestone 9 — `1.21.7` + `1.21.8` (Forge & NeoForge)](#milestone-9--1217--1218-forge--neoforge)
- [✅ Milestone 10 — `1.21.9` (Forge & NeoForge)](#milestone-10--1219-forge--neoforge)
- [✅ Milestone 11 — `1.21.10` (Forge & NeoForge)](#milestone-11--12110-forge--neoforge)
- [✅ Milestone 12 — `1.21.11` (Forge & NeoForge)](#milestone-12--12111-forge--neoforge)
- [✅ Milestone 13 — `26.1.2` (Forge & NeoForge)](#milestone-13--2612-forge--neoforge)
- [✅ Milestone 14 — `26.2` (Forge & NeoForge)](#milestone-14--262-forge--neoforge)
- [🚧 Milestone 15 — Fabric, 17 nodes (`1.20.1` → `26.2`)](#milestone-15--fabric-17-nodes-1201--262)

### ✅ Milestone 0 — `1.20.1-forge` compiles the pristine source

**✅ Milestone 0 COMPLETE** (2026-07-22). `./gradlew ":1.20.1-forge:build"` is **GREEN** and
produces `versions/1.20.1-forge/build/libs/alexsmobs-<ver>-forge+1.20.1.jar`
(26.4 MB, 5222 entries). The pristine upstream source compiles unmodified on the new harness —
only warnings are 100 × `[removal] ResourceLocation(String)`, expected on 1.20.1.

Harness adapted from codxlib: `settings.gradle.kts`, `stonecutter.gradle.kts`,
`stonecutter.properties.toml`, `build.forgeg.gradle.kts`, `build.neoforge.gradle.kts`,
`build-logic/`, `gradle.properties`, templated `pack.mcmeta`, Gradle 9.4.1 wrapper. Only the
`1.20.1-forge` node is active.

### ✅ Milestone 1 — Citadel vendored, dependency removed

**✅ Milestone 1 COMPLETE** (2026-07-22) — **Citadel vendored, dependency removed.** See
"The Citadel constraint" above for the design and the file inventory. `grep -r citadel` over
`src/main/java`, `src/main/resources/data` and `src/main/resources/assets` returns **nothing**.

**Runtime-verified**, not just compiled: headless
`printf 'stop\n' | ./gradlew ":1.20.1-forge:runServer"` reaches `Done (2.898s)!` — **no Citadel
jar present**, Alex's Mobs' own init running, and **zero** advancement parse errors. This is the
gate to re-run after every node is added. (Grep the log for `Parsing error loading` — a bad
advancement/loot JSON is logged, not thrown, so the server still reaches `Done`.)

Jar contents verified by unzipping (don't assume — check):

- generated `META-INF/mods.toml` carries the citadel / forge / minecraft deps + right identity
- `pack.mcmeta` expanded to `"pack_format": 15` (upstream shipped a stale `12`)
- `META-INF/accesstransformer.cfg` (2587 B) survives into the jar
- `MixinConfigs: alexsmobs.mixins.json` in `MANIFEST.MF`

Two files were **added** to satisfy the `mod-platform` plugin, which declares them
unconditionally:

- **`src/main/resources/alexsmobs.mixins.json`** — the plugin always emits
  `[[mixins]] config = "<modId>.mixins.json"`; a declared-but-absent config is a Forge
  hard-failure at load. Started as an empty config; Milestone 1 filled it with the 5 mixins
  Citadel used to supply (1 common + 4 client). **No `refmap` key** — see above.
- **`src/main/resources/assets/icon.png`** — `Loader.kt` hardcodes `logoFile = "assets/icon.png"`;
  copied from `alexs_mobs_thumb.png`.

`javadoc` is disabled in both node buildscripts — upstream carries essentially no doc comments,
and generating it for ~745 files costs minutes per node across 28 nodes.

### ✅ Milestone 2 — `1.20.4` (Forge & NeoForge)

**✅ Milestone 2 COMPLETE** (2026-07-22) — `1.20.4` Forge + NeoForge.

### ✅ Milestone 3 — `1.20.6` (Forge & NeoForge) — DataComponents

**✅ Milestone 3 COMPLETE** (2026-07-23) — `1.20.6` Forge + NeoForge, i.e. the **DataComponents**
break. All **five** active nodes (`1.20.1-forge`, `1.20.4-forge`, `1.20.4-neoforge`,
`1.20.6-forge`, `1.20.6-neoforge`) build green in one invocation and every one of them boots a
dedicated server to `Done` with **no errors other than the four benign `RuntimeDistCleaner`
lines** documented under "The Citadel constraint".

Re-run the gate like this (a `stop` piped into the MDG `runServer` is **not** consumed — the
NeoForge nodes hang until the tool timeout; run them detached and kill instead):

```bash
./gradlew :1.20.1-forge:build :1.20.4-forge:build :1.20.4-neoforge:build \
          :1.20.6-forge:build :1.20.6-neoforge:build --continue
# then per node: runServer, wait for "Done (", SOAK ~45s, kill, and grep the log for
#   '/ERROR]|Couldn.t load tag|Couldn.t parse data file|Parsing error|InvalidMixin'
# plus check versions/<node>/run/crash-reports/ — see the soak note under Milestone 8.
```

### ✅ Milestone 4 — `1.21` + `1.21.1` (Forge & NeoForge)

**✅ Milestone 4 COMPLETE** (2026-07-23) — `1.21` + `1.21.1`, Forge + NeoForge. All **nine** active
nodes build green in one invocation, and all four new ones boot a dedicated server to `Done` with
nothing in the log but the benign `RuntimeDistCleaner` lines (now **five** — `client/GuiMixin`
joined this wave).

What this wave actually cost, beyond the data-folder renames:

- **Forge 51 has no HUD-layer API.** Forge dropped `RenderGuiOverlayEvent` in 1.21 and never
  replaced it, so the farseer's static overlay is drawn from a new **`client/GuiMixin`** on Forge
  `>=1.21`. NeoForge keeps `RegisterGuiLayersEvent` — but 1.21 hands the layer a `DeltaTracker`
  instead of a bare partial tick, so its registration is a second, separate `//? if neoforge && >=1.21`
  block (they are siblings, never nested).
- **A NeoForge-1.21 `replacements` group** in `stonecutter.gradle.kts` covers the pure renames:
  `Tags.Items.SHEARS`→`TOOLS_SHEAR` (it was `TOOLS_SHEARS` for 1.20.5–1.20.6 only — three spellings
  in three versions), `ToolAction(s)`→`ItemAbilit(y|ies)`, `isAddedToWorld`/`onAddedToWorld`→
  `…ToLevel`, `SpawnPlacementRegisterEvent`→`RegisterSpawnPlacementsEvent`, and
  `getNewTarget`/`setNewTarget`→`get/setNewAboutToBeSetTarget`. Rules are ordered **longest-prefix
  first** so a shorter rule cannot eat a longer one's match. Because the event rename is a
  replacement, `AMEntityRegistry.java` needed no source edit at all.
- **`DistExecutor` is gone on NeoForge 1.21 — and the obvious replacement crashes the server.**
  `FMLEnvironment.dist.isClient() ? new ClientProxy() : new CommonProxy()` makes the **JVM verifier**
  prove `ClientProxy` is assignable to `CommonProxy`, which loads the class and trips
  `RuntimeDistCleaner` on a dedicated server (`Failed to create mod instance`). Route it through a
  `Supplier` method reference instead — `((Supplier<CommonProxy>) ClientProxy::new).get()` — so the
  client class name only ever appears in the invokedynamic bootstrap args and is resolved lazily.
  That indirection is exactly what `DistExecutor` used to provide. Both `AlexsMobs.PROXY` and
  `citadel/Citadel.PROXY` needed it.
- **Mod construction**: NeoForge 1.21 deleted `FMLJavaModLoadingContext` and moved
  `registerConfig` off `ModLoadingContext` onto **`ModContainer`**. The constructor takes
  `(IEventBus, ModContainer)` there and stays no-arg on Forge; naming the parameter
  `modLoadingContext` keeps the existing `registerConfig(…)` call site identical on both.
  `ClientProxy.init` gets its bus from `ModLoadingContext.get().getActiveContainer().getEventBus()`.
- **Damage events merged.** NeoForge 1.21 folded `LivingAttackEvent` **and** the cancellable half of
  `LivingDamageEvent` into **`LivingIncomingDamageEvent`**; both `ServerEvents` handlers map onto it.
  `LootingLevelEvent` was **deleted outright** — looting is an enchantment effect now and no event can
  bump it for one killer, so the snow leopard's "+2 looting on its own kills" bonus is **dropped on
  NeoForge ≥1.21** (reproducing it would need a datapack enchantment, i.e. a behaviour change).
- `BlockState.getExpDrop` (fortune/silk-touch became components), `BowItem.customArrow` (+stack),
  `EntityRenderersEvent.AddLayers.getRenderer`→`getEntityRenderer` **on Forge only**, and
  `event.getSkin`→`getPlayerSkin` **on Forge only** all needed per-call-site conditionals.
- **`accesstransformer_mojmap.cfg` lists both eras** of `Camera#move` (`(DDD)V` ≤1.20.6,
  `(FFF)V` ≥1.21). AT files are not preprocessed; an entry that matches nothing is a silent no-op,
  which is what makes this work.
- **Two runtime-only data bugs** that compile fine and are only visible in the server log:
  1. 1.21 deleted the loot symbols that name the looting level — `looting_enchant` and
     `random_chance_with_looting`. An unknown loot id fails the **whole table** to parse (logged,
     not thrown), so 41 tables were silently dropping *all* their loot. Fixed by
     `DataPackMigration.migrateLootTo121` (see below).
  2. NeoForge 1.21 **errors at server start** for any entity that has a spawn entry but never
     registered a spawn placement. `spectre` and `cosmic_cod` were never registered upstream (it
     relied on vanilla's unregistered default) — they are now spelled out explicitly in
     `registerSpawnPlacements()` with `PLACE_NO_RESTRICTIONS` / `MOTION_BLOCKING_NO_LEAVES` /
     always-true predicates, which *is* that default, so no node changes behaviour.

### ✅ Milestone 5 — `1.21.2` + `1.21.3` (render-state rewrite)

**✅ Milestone 5 steps 1–3 COMPLETE** (2026-07-24). The three new nodes are down from ~4,300 errors
to **1,502** (`1.21.3-forge`), **~1,504** (`1.21.2-neoforge`), **~1,594** (`1.21.3-neoforge`), and all
nine established nodes still compile green. What landed:

- **The `ServerLevel` thread** (`!mc2102-*-decl`/`-super` rules + AMCompat helpers): `isInvulnerableTo`,
  `doHurtTarget`, `dropEquipment`, `customServerAiStep`, `kill`, `spawnAtLocation`, `EntityType.create`.
  ~131 files of call sites rewritten. The injected parameter is always named `amLevel`.
- **`Entity#hurt` became `public final void` in 1.21.2** — this was *not* in the era table before.
  The override point moved to the **abstract `hurtServer(ServerLevel, DamageSource, float)`**, with
  `hurtClient(DamageSource)` for the client half and `hurtOrSimulate(DamageSource, float)` as the
  boolean-returning caller-side form. So: the `hurt` override rule is a *rename plus* a parameter
  insertion, `AMCompat.hurt` routes call sites to `hurtOrSimulate`, and **13 classes that extend
  `Entity` directly and never overrode `hurt`** (`EntityCachalotEcho`, `EntityFart`, `EntityGust`,
  `EntityHemolymph`, `EntityIceShard`, `EntityMosquitoSpit`, `EntitySandShot`, `EntitySquidGrapple`,
  `EntityTendonSegment`, `EntityVineLasso`, `EntityVoidPortal`, `EntityVoidWormShot`,
  `EntityMobProjectile`) each got a `//? if >=1.21.2` `hurtServer` block reproducing the old
  `Entity#hurt` default (`isInvulnerableToBase` → `markHurt()` → `false`).
- **`Entity` lost its public `isInvulnerableTo`** — only `LivingEntity` has one now, and it wants a
  `ServerLevel`. `AMCompat.isInvulnerableTo` therefore takes the `LivingEntity`+`ServerLevel` path
  when it can and otherwise inlines `isInvulnerableToBase`'s body (which is `protected`), which is
  also the only sane answer for the multiparts and the client-side callers.
- **The multipart `hurt` client branch is now dead code on ≥1.21.2** and that is fine. `hurtServer`
  never runs client-side, so `EntityCachalotPart`/`EntityGiantSquidPart` no longer send
  `MessageHurtMultipart` from the client. Forge routes an attack on a part to the part server-side
  anyway (`EntityLaviathanPart` never had a client branch at all), so the message was redundancy.
- **`EnderDragon#reallyHurt`** took a `ServerLevel` and stopped returning a boolean;
  `accesstransformer_mojmap.cfg` now lists **both** signatures (the non-matching one is a silent
  no-op, same trick as `Camera#move`), and `EntityVoidWorm.wormAttack` got a conditional.
- **`InteractionResultHolder` → `InteractionResult`**: factories through AMCompat
  (`sidedSuccess`/`success`/`consume`/`pass`/`fail`/`holder`, plus the arity-1
  `sidedSuccess(boolean)` that replaced `InteractionResult.sidedSuccess`), declarations and imports
  through the `!mc2102-irh-*` rules. `PASS`/`FAIL` have no item slot any more, so those two helpers
  drop their stack argument — which matches how the holder was actually used. `ItemInteractionResult`
  (which only ever existed for 1.20.5–1.21.1) folds back into `InteractionResult` too.
- **Cheap sweeps**: `UseAnim`→`ItemUseAnimation` (three rules keyed on `;`/`.`/space — a bare match
  would mangle `getUseAnimation`), `getMin/MaxBuildHeight` (AMCompat, **not** a rename — `getMaxY()`
  is inclusive so it is `+ 1`), Forge's `has/getCraftingRemainingItem` → vanilla
  `ItemStack#getCraftingRemainder`, and `Ingredient.of(TagKey)` → `AMCompat.ingredientOf` (Ingredient
  is `HolderSet`-backed now, so the tag is resolved through `BuiltInRegistries.ITEM.getOrThrow`),
  and the two `SoundEvents` constants that item components reference (`GENERIC_EAT`,
  `HONEY_DRINK`) — and *only* those two — became `Holder<SoundEvent>`, so they take `.value()`.

**✅ Milestone 5 COMPLETE** (2026-07-25) — `1.21.2` (NeoForge) + `1.21.3` (Forge & NeoForge), i.e. the
**render-state rewrite** and the wide 1.21.2 vanilla sweep. All **12** active nodes build green in **one**
invocation (`BUILD SUCCESSFUL in 1m46s`), and the three new nodes each boot a dedicated server to `Done`:
`1.21.3-forge` (0.680s), `1.21.2-neoforge` (2.927s), `1.21.3-neoforge` (2.539s) — nothing in the logs
but the benign lines (client-mixin `RuntimeDistCleaner`/dist warnings, oshi/assets-URL dev noise, the
upstream "Cannot get config value before config is loaded" WARN, the `seal_reward`→vanilla-fishing-junk
loot-validation WARN, and first-run `server.properties` creation).

The ~122 render-state errors and the mechanical buckets from step 5 were all resolved (compile is green;
diffed against `AlexsMobsFP/common/src/main/java/com/github/alexthe666/`, the 26.1 destination shape).
The runtime-only fixes that surfaced only at boot (compile fine, logged-not-thrown or crash-at-load):

- **1.21.2 recipe-ingredient JSON format** — `Ingredient` became a `HolderSet<Item>` whose codec accepts
  ONLY a string (`"minecraft:paper"`, or `"#c:tag"`) or an array of those; the old `{"item":…}`/`{"tag":…}`
  object forms are gone, and an unrecognised shape **fails the whole recipe to parse** (logged, not thrown
  — recipes silently vanish). Rewritten at build time by **`DataPackMigration.migrateIngredientsTo1212`**
  (hooked from `ModPlatformPlugin` in the existing `>=1.21.2` `doLast`): converts every ingredient field
  (`ingredient(s)`, `base`, `addition`, `template`) and the shaped `key` map, over `/recipe(s)/` **and**
  `/capsid_recipes/`. `{"item":X}`→`"X"`, `{"tag":Y}`→`"#Y"`, and **`minecraft:music_discs`→`c:music_discs`**
  (that vanilla item tag was removed at 1.21.2; Forge backfills `forge:*` but not vanilla-removed tags, so
  the merged jar also ships `data/c/tags/item/music_discs.json`). 86 recipes/node rewritten.
- **`CapsidRecipe` (the mod's custom recipe type) on ≥1.21.2** — `SimpleJsonResourceReloadListener` went
  generic over a `Codec<T>`. Rewrote `CapsidRecipeManager` to `extends SimpleJsonResourceReloadListener<CapsidRecipe>`
  with a `RecordCodecBuilder` CODEC. **Two traps:** (1) `Ingredient.CODEC`/`ItemStack.CODEC` need a
  `RegistryOps` to resolve item/tag refs — a bare `super(CODEC, name)` decodes with plain `JsonOps` and
  throws "Can't decode element Reference{…} without registry"; so the ctor takes a `HolderLookup.Provider`
  and calls `super(registries, CODEC, "capsid_recipes")`, threaded from `AddReloadListenerEvent` — Forge's
  **`getRegistries()`** (the reload's TAG-BOUND provider) vs NeoForge's **`getRegistryAccess()`**. (2) the
  CODEC's decode lambda must **NOT** call `Ingredient#items()` — that runs in `prepare()` on a worker thread
  BEFORE tags bind ("Trying to access unbound tag"); drop the empty-ingredient filter and defer tag
  resolution to `matches()` at runtime.
- **Armour repair-tag registry-freeze crash (NeoForge ≥1.21.2 only)** — 1.21.2's `ArmorMaterial` record
  takes a repair `TagKey<Item>`; constructing the `ArmorItem` runs `ArmorMaterial#humanoidProperties` →
  `Item.Properties#repairable(tag)` → `ITEM.getOrThrow(tag)`, which registers an **unbound** tag in the ITEM
  registry. Vanilla binds every such bootstrap-created tag to empty before its registry freeze
  (`BuiltInRegistries#bindBootstrappedTagsToEmpty`); **mod items register AFTER that pass**, so the
  `alexsmobs:repairs/<name>` tags stay unbound and NeoForge's `GameData.freezeData` aborts server load with
  `Unbound tags in registry minecraft:item: [alexsmobs:repairs/centipede, …]` (all 15 armours). Fix:
  **`AMCompat.bindItemTagEmptyForFreeze(tag)`** (`>=1.21.2`) binds the tag to empty from `AMArmorMaterial.material()`
  during `RegisterEvent`, while the ITEM registry is still writable (`((MappedRegistry) BuiltInRegistries.ITEM).bindTag(tag, List.of())`,
  best-effort/try-caught); the datapack (`data/alexsmobs/tags/item/repairs/<name>.json`, relocated
  plural→singular by the migration) rebinds real contents at reload, so anvil repair still works. Forge
  never hit this — its freeze path differs, and 1.21/1.21.1 use the pre-1.21.2 `ArmorMaterial` with a lazy
  `Supplier<Ingredient>` that never calls `getOrThrow` at registration.

### ✅ Milestone 6 — `1.21.4` (Forge & NeoForge) — item model definitions

**✅ Milestone 6 COMPLETE** (2026-07-24) — `1.21.4` (Forge & NeoForge), i.e. the **item-model-definition**
break. All **14** active nodes build green in one invocation, and both new nodes boot a dedicated server to
`Done` with nothing but the benign lines. 1.21.4 removed the whole in-hand baked-model / `ItemProperties`
customisation surface; the decision (pre-authorised) was to **gate the removed client wiring out on
≥1.21.4, preserving the logic as dead code, and accept the cosmetic regressions**:

- **APIs removed in 1.21.4** (verified by `javap` on the forge-1.21.4 merged-srg jar + diffed against
  `AlexsMobsFP`): `BlockEntityWithoutLevelRenderer`/`IClientItemExtensions.getCustomRenderer()`,
  `ItemProperties`, `ItemOverride(s)`/`BakedOverrides`, `ItemRenderer.getModel()` and the
  `render(...,BakedModel)` overload, `Minecraft.getItemColors()`, `ItemTransforms.hasTransform(...)`,
  `RegisterColorHandlersEvent.Item`, `ModelEvent.ModifyBakingResult#getModels()`,
  `MultifaceBlock.getSpreader()`. Still present: `MultifaceSpreader`, `ItemTransform.NO_TRANSFORM` (the
  "no transform" sentinel — replaces `hasTransform`), `ItemRenderer.renderStatic(...)`,
  `Minecraft.getBlockColors()`. `MultifaceBlock` now `implements SimpleWaterloggedBlock` and declares
  `WATERLOGGED` **itself**; `Ingredient#items()` returns `Stream<Holder<Item>>` (was `List` on 1.21.2/3).
- **Cosmetic regressions on ≥1.21.4** (flagged, not fixed): the custom in-hand item renders and gui3d
  layout tweaks on `LayerElephantItem`/`LayerMantisShrimpItem`/`RenderCapsid` (now `if (false)` /
  `renderStatic` fallbacks), the **ghostly-pickaxe fullbright wrap** (`GhostlyPickaxeBakedModel` is an
  inert stub — the `BakedModelWrapper` hooks are gone), the ~8 `ItemProperties` dynamic model states, and
  the leafcutter-ant leaf tint (now via `getBlockColors()` on jungle leaves instead of `getItemColors()`).
  None affect gameplay, spawning, or data.
- **`BlockSkunkSpray` two runtime fixes** (compile-clean, only visible at boot): `getSpreader()` drops its
  `@Override` on ≥1.21.4 (method removed from super); and `createBlockStateDefinition` adds **only `AGE`**
  on ≥1.21.4 — MultifaceBlock now contributes `WATERLOGGED` itself, so `add(WATERLOGGED, AGE)` threw
  `duplicate property: waterlogged` at registration.
- **`music_discs` capsid-recipe latent bug (ALL ≥1.21.2 nodes) — found and fixed this wave.** Vanilla
  removed the `minecraft:music_discs` **bootstrap** item tag at 1.21.2, so an ingredient referencing it
  (the capsid recipe for `music_disc_daze`/`music_disc_thime`) fails to resolve at `prepare()` time — which
  runs on a worker thread **before tags bind** — and the recipe silently vanishes ("Couldn't parse data
  file", logged not thrown). Milestone 5's boot gate never caught it: its grep pattern was `Couldn.t load
  tag`, which does **not** match "Couldn't parse data file". Fix in `DataPackMigration`: an `expandedTags`
  map inlines the tag's two members as a **bare item array ingredient** at build time
  (`"ingredients": [ ["alexsmobs:music_disc_daze","alexsmobs:music_disc_thime"] ]`) — `Ingredient.CODEC`
  accepts a direct array of item ids as a holder set, so there is no tag lookup at all. **Boot-gate grep
  must now include `Couldn.t parse data file`** alongside the existing patterns.

### ✅ Milestone 7 — `1.21.5` (Forge & NeoForge) — MEGA-WAVE

**✅ Milestone 7 COMPLETE** (2026-07-26) — `1.21.5` (Forge & NeoForge), a **MEGA-WAVE** (~1,250 errors/node
on first compile, comparable to 1.21.2; the old era-table note calling 1.21.5 "a small render refinement"
was wrong). All **16** active nodes build green in one invocation, and **every one of the 16 boots a
dedicated server to `Done`** with nothing in the log but the benign lines (5 × client-mixin
`RuntimeDistCleaner`, oshi/assets-URL dev noise, the "Cannot get config value before config is loaded"
WARN, the `seal_reward` loot-validation WARN, first-run `server.properties`/`eula.txt` creation).

Both new nodes needed a `run/eula.txt` with `eula=true` — MDG/loom create it as `false` on first
`runServer` and the server refuses to start ("Failed to load eula.txt"); the 14 established nodes already
had one.

**Mechanical renames** (in `stonecutter.gradle.kts` `>=1.21.5` block):
`net.minecraft.world.entity.animal.Wolf`→`…animal.wolf.Wolf`, `…animal.Sheep`→`…animal.sheep.Sheep`;
`Entity#moveTo`(all overloads)→`snapTo` and `absMoveTo`→`absSnapTo` (vanilla keeps NO `moveTo`, so the
blanket `.moveTo(`→`.snapTo(` is safe); `isInWaterOrBubble()`→`isInWater()` and
`isInWaterRainOrBubble()`→`isInWaterOrRain()` (bare calls — **no** leading dot in the pattern).

**Two traps that only NeoForge 1.21.5 hits** (Forge 1.21.5 needs neither, which is why they surface late):

- **Shield abilities are gone; blocking is the `BLOCKS_ATTACKS` data component.** NeoForge 1.21.5 *removed*
  `ItemAbilities.SHIELD_BLOCK` and `DEFAULT_SHIELD_ACTIONS` outright (Forge 1.21.5 only deprecates them).
  Both halves are gated on the **version**, not the loader, so the two loaders take one path: the *query*
  side (4 call sites — `EntitySharkToothArrow`, `EntityMimicube`, `EntityVoidWormShot`, `EntityCrocodile`)
  goes through `AMCompat.canShieldBlock(stack)` → `stack.has(DataComponents.BLOCKS_ATTACKS)`; the
  *declaration* side (`ItemShieldOfTheDeep`, `ItemSkelewagSword`) drops its `canPerformAction` override to
  `<1.21.5` and instead stamps the component in its ctor via **`AMCompat.shieldProperties(props)`**, using
  vanilla `Items.SHIELD`'s exact numbers (0.25 s block delay, 1.0 disable-cooldown scale, one 90° arc
  `DamageReduction` at 100%, `ItemDamageFunction(3,1,1)`, bypassed by `DamageTypeTags.BYPASSES_SHIELD`).
- **Modded `EntityDataSerializer`s must go through NeoForge's own registry.** NeoForge 1.21.5 makes
  `EntityDataSerializers.registerSerializer` **hard-throw** for any non-vanilla caller
  (`UnsupportedOperationException: Modded EntityDataSerializers must be registered to
  NeoForgeRegistries.ENTITY_DATA_SERIALIZERS …`) — it would desync serializer ids between client and
  server. It throws from `AMCompat.<clinit>`, i.e. as an `ExceptionInInitializerError` inside
  `ModLoadingException`, so read the *cause*. Fix: on `neoforge && >=1.21.5`, `AMCompat.OPTIONAL_UUID` is
  registered through a `DeferredRegister` on `NeoForgeRegistries.Keys.ENTITY_DATA_SERIALIZERS`, wired onto
  the mod bus in `AlexsMobs`'s constructor; Forge keeps the vanilla static call. **Deferring is safe** —
  verified in `SynchedEntityData`/`EntityDataSerializers`: `defineId` only stores the serializer *object*
  (`ID_REGISTRY.define(class)`), and the numeric id is resolved much later at pack time via
  `getSerializedId`.

**The buckets this wave paid off, with the VERIFIED destination API** (javap on the mojmap 1.21.5 merged jar
+ `AlexsMobsFP` 26.1.2 reference). Biggest first — kept as a reference for the nodes above:

1. **`CompoundTag` getters return `Optional` (~850 errors).** Every `getInt/getBoolean/getFloat/getDouble/
   getString/getByte/getLong` now returns `Optional<T>`; use the new `getIntOr(key,default)` /
   `getBooleanOr` / `getFloatOr` / `getDoubleOr` / `getStringOr` / `getByteOr` / `getLongOr` for old
   behaviour. `getCompound(k)`→`getCompoundOrEmpty(k)`, `getList(k[,type])`→`getListOrEmpty(k)` (the type
   arg is gone), `contains(k,type)` 2-arg→`contains(k)` 1-arg. **A blanket sed is UNSAFE** — the tree has
   ~618 `.getX(` sites but some are false positives (`Component.getString()` 0-arg, `JsonUtils.getInt`, map
   getters) that did NOT change; the ~850 `Optional cannot be converted` compile errors precisely mark the
   real CompoundTag sites. Plan: **AMCompat helpers** `getInt(CompoundTag,String)` etc. (`>=1.21.5` body
   `t.getIntOr(k,0)`, else `t.getInt(k)`) + rewrite the erroring call sites to `AMCompat.getX(t,k)`. NOTE
   this dovetails with **1.21.6** (`ValueInput`/`ValueOutput` replace the whole save/load signature) — design
   the AMCompat layer to also carry a `ValueInput` overload so 1.21.6 reuses it.
   - **UUID storage removed from `CompoundTag`**: `putUUID`/`getUUID`/`hasUUID` gone. Use
     `t.store(key, UUIDUtil.CODEC, uuid)` / `t.read(key, UUIDUtil.CODEC)` (→ `Optional<UUID>`). (~210 sites.)
2. **Render-pipeline rewrite (~300 errors) — the hard one.** `RenderType.create(name, VertexFormat, Mode,
   int, boolean, boolean, CompositeState)` is GONE; the new form is
   `create(name, int bufferSize, RenderPipeline, CompositeState)` — a `com.mojang.blaze3d.pipeline.RenderPipeline`
   replaces shader+format+mode. `GlStateManager` (blend funcs) gone → use `RenderSystem`/pipeline state;
   `TransparencyStateShard`, the `RENDERTYPE_*_SHADER`/`*_TRANSPARENCY`/`NO_CULL`/`*_DEPTH_TEST`/
   `COLOR_*_WRITE` `RenderStateShard` constants, `CoreShaders`, `BufferUploader` all reshuffled;
   `ResourceLocation`→`GpuTexture` in a few sites. Casualty is `client/render/AMRenderTypes.java` (custom
   glowing-eye/energy-swirl/ghost render types). Per the **1.21.4 precedent** (gate deep cosmetic client
   render out, flag the regression), the pragmatic option is to make AMRenderTypes fall back to the nearest
   vanilla `RenderType` on `>=1.21.5` rather than rebuild each pipeline — **verify each caller still gets a
   usable RenderType**. `BakedModel` (client.resources.model) is gone → `BlockStateModel`
   (client.renderer.block.model); its only imports are in already-`>=1.21.4`-dead paths (RenderCapsid), so
   gate the imports out too.
3. **`MobSpawnSettings.SpawnerData` lost its weight (184 errors).** Now `SpawnerData(EntityType<?>, int min,
   int max)`; weight moved to a `WeightedList` — `MobSpawnSettings.Builder.addSpawn(MobCategory, int weight,
   SpawnerData)`. All sites are in `world/AMWorldRegistry.java` (+ `misc/AMPlatform.java`), reached through
   the Forge/NeoForge **biome+structure modifier callbacks** (`ModifiableBiomeInfo` /
   `ModifiableStructureInfo`, `.getSpawner(cat).add(...)` / `.getOrAddSpawnOverrides(cat).addSpawn(...)`).
   Check the Forge-55 / NeoForge builder's weighted-add shape before writing an AMCompat helper.
4. **`TamableAnimal` owner is an `EntityReference` now.** `getOwnerUUID()`→`getOwnerReference()` (returns
   `EntityReference<LivingEntity>`; `.getUUID()` off it), `setOwnerUUID(uuid)`→`setOwnerReference(new
   EntityReference<>(uuid))`, `DATA_OWNERUUID_ID` is `Optional<EntityReference<LivingEntity>>`. (~46 sites.)
5. **`ArmorItem` / `SwordItem` removed from vanilla** (armor & weapons are data components now). Files:
   `item/{ItemModArmor,ItemSkelewagSword,AMArmorMaterial,ItemTarantulaHawkElytra,ItemTendonWhip}.java`,
   `entity/EntityKangaroo.java`, `client/render/layer/{LayerKangarooArmor,LayerMimicubeHelmet}.java`,
   `item/AMItemRegistry.java` (the `regItem` overloads, 36 errors). AlexsMobsFP **shims** these
   (`codx.AMUP.mcshim.world.item.ArmorItem`), i.e. vanilla has no drop-in — needs the equippable/tool
   component construction. Design work required; check FP's mcshim for the real vanilla ctor it wraps.
6. **`BlockEntityRenderer#render` gained a camera-pos `Vec3`** (~14 tile renderers: RenderCapsid,
   RenderTransmutationTable, the 6 EndPirate*, RenderVoidWormBeak, …) — decl-rewrite + AMCompat, same
   pattern as the 1.21.2 signature waves.
7. **`EntityDataAccessor` set/get now see `Optional<T>`** where the synched field was made Optional (12+6+4
   sites — owner/target refs), and **`ItemInHandRenderer#renderItem`** signature changed (42 sites, client),
   two `SoundEvents` constants became `Holder<SoundEvent>` (8), `Inventory#items` is private now (4).

**Boot-gate grep must include `Couldn.t parse data file`** (added this session — see the 1.21.4 note; the
old `Couldn.t load tag` pattern missed the music_discs latent bug for a whole milestone).

### ✅ Milestone 8 — `1.21.6` (Forge & NeoForge) — ValueInput/ValueOutput + EventBus 7

**✅ Milestone 8 COMPLETE** (2026-07-26) — `1.21.6` (Forge & NeoForge), i.e. **`ValueInput`/`ValueOutput`**
plus, on Forge only, **EventBus 7**. All **18** active nodes build green in one invocation, and **all 18
boot a dedicated server to `Done` and keep ticking for 45 s with no crash and nothing in the log but the
benign lines**.

**`ValueInput`/`ValueOutput`** (the `>=1.21.6` `!mc2106-*` replacement group). The two interfaces keep
1.21.5's `put*`/`get*Or` method *names*, so a save/load body that only touches primitives compiles unchanged
once the parameter type flips — which is all the `-decl` rules do (keyed on the parameter **type**, so all
three parameter spellings are covered by one rule apiece and no call site can match). Everything the
interfaces **dropped** goes through the AMCompat overload family, whose members deliberately share the
`CompoundTag` versions' names and arities so call-site text stays era-agnostic:

- `BlockEntity#save/loadAdditional` also **lost their `HolderLookup.Provider` parameter** (the `ValueInput`
  carries the registry context). Rather than rewrite the 12 bodies that use `provider`, each rule
  **re-declares `provider` as a local on the same line** — load reads `input.lookup()`, save (which has no
  input) goes through `AMCompat.lookupOf(this)` off the block entity's level.
- `ItemStack.parse`/`save`, `ContainerHelper`'s `CompoundTag` overloads, and
  `SimpleContainer#createTag`/`#fromTag` were **all removed**; the codec-only replacements are wrapped in
  `AMCompat` (`TagValueOutput`/`TagValueInput` are the vanilla adapters, so a caller that owns a raw tag —
  the capsid's update packet — still can). `ValueInput`/`ValueOutput` speak codecs only, so a raw `Tag`
  round-trips through `NbtOps.PASSTHROUGH`.
- **`Mob`'s "restriction" API was renamed "home"** (`getRestrictCenter`→`getHomePosition`,
  `hasRestriction`→`hasHome`, `restrictTo`→`setHomeTo`, …) — pure renames, all call sites.
- **`Entity#canBeCollidedWith` gained the colliding entity** (9 overrides + 1 call site).
- **GUI transforms lost their depth**: `GuiGraphics#pose()` is an `org.joml.Matrix3x2fStack`. Only
  `pushPose`/`popPose` can be blind-renamed (`pushMatrix`/`popMatrix`, keyed on `.pose().`);
  `translate`/`scale` changed **arity** and go through `AMRenderCompat.translateGui`/`scaleGui`.

**EventBus 7 (Forge 56.0.0 only — ~110 errors, the bulk of the wave).** NeoForge is untouched; it has had
its own bus API since 1.20.6. The pure renames are the `>=1.21.6` Forge-only `!fg2106-eb-*` group; every
**shape** change is a source-level Stonecutter gate, because a search-and-replace cannot reach it:

- The api classes moved into sub-packages — `api.bus.{BusGroup,EventBus,CancellableEventBus}`,
  `api.event.{MutableEvent,RecordEvent,InheritableEvent}`, `api.event.characteristic.{Cancellable,…}`,
  `api.listener.{SubscribeEvent,Priority,…}`. **`IEventBus` and `api.Event` are gone outright**, so even an
  *unused* import of them is a compile error and has to be gated.
- **`EventPriority` became `Priority`, a class of `byte` constants.** A bare reversible
  `EventPriority.`→`Priority.` rule would be **UNSAFE** — reversed against root `src/` it turns
  `EventPriority.LOWEST` (which *contains* `Priority.`) into `EventEventPriority.LOWEST`. The two rules are
  keyed on the whole annotation text (`priority = EventPriority.HIGH)`), which does not contain its own
  replacement. Two usages in the tree.
- **Cancellation inverted: a listener cancels by *returning `true`*.** `setCanceled`/`isCanceled` are gone
  and `MutableEvent` carries no cancellation flag. A blanket "rewrite `setCanceled(true)` to `return true`"
  is **not faithful** — several handlers keep working after cancelling (`onStruckByLightning` still converts
  the squid; `onPreRenderEntity` still renders the rolling entity). So each such handler keeps its body
  **verbatim** as a private `…0` method and gains a thin boolean-returning bridge:
  `return AMCompat.cancelIf(() -> body(event));`, with `AMCompat.cancelEvent()` standing in for
  `setCanceled(true)`. The flag is a **`ThreadLocal`** (events fire on both the server and the render
  thread) and saves/restores the previous value, so a handler that re-enters the bus cannot clobber an
  outer post's verdict. 7 bridges (5 in `ServerEvents`, 2 in `ClientEvents`), 11 `cancelEvent()` sites.
- **`ProjectileImpactEvent` is not `Cancellable` in EB7** — it grew a richer `ImpactResult`, and
  `SKIP_ENTITY` is what the old cancel meant at both emu-dodge sites (the projectile ignores the emu and
  keeps flying).
- **`MinecraftForge.EVENT_BUS` kept `register`/`unregister` but lost `post()` entirely.** Posting goes
  through a **per-event static `BUS` field**. Every event this mod fires itself needed one: the 4 vendored
  Citadel client events and `AnimationEvent.Start`/`.Tick` declare `EventBus.create(X.class)` /
  `CancellableEventBus.create(X.class)` (raw type argument, the shape Forge itself uses), and the vanilla
  Forge events fired by hand (`RenderLivingEvent.Pre/.Post`, `RenderNameTagEvent`,
  `LivingEquipmentChangeEvent`, `LivingKnockBackEvent`, `EntityTeleportEvent.EnderEntity`) go through
  `X.BUS.post(…)`. Constructors are **unchanged** for all of those — only the posting mechanism moved.
  - Each Citadel client event also gained a **`post()` instance method** that hides the loader/version
    choice, because `ItemBlockRenderTypesMixin`'s post site already sits inside a `//? if >=1.21.6` block
    and Stonecutter blocks are **siblings, never nested**. The 4 client mixins now just call `event.post()`.
  - `AnimationEvent` moves from `Event` + `@Cancelable` to `MutableEvent` + `InheritableEvent` (a parent
    with event subclasses) + the `Cancellable` **marker interface** — the same shape NeoForge has had since
    1.20.6. `AnimationHandler` reads `Start.BUS.post(event)`'s boolean.
  - `AMPlatform.postCancelled(Event)` is now gated **`<1.21.2`**: it cannot exist above that, since EB7 has
    no common `Event` base to take as a parameter, and its three callers already go through
    `AMRenderEventCompat` from 1.21.2 up.
- **`Event.Result` moved to `net.minecraftforge.common.util.Result`** (an enum with `isAllowed()`/
  `isDefault()`/`isDenied()`, exposed via a `HasResult` interface) — the name-tag veto reads the same but
  off a different type.
- **The two factory helpers that used to convey a veto no longer can**: `ForgeEventFactory.onEnderTeleport`
  (renamed `onEnderManTeleport`) and `ForgeHooks.onLivingKnockBack` still hand the event back, but it has no
  `isCanceled()`. `EntityCosmicCod.teleport` and the three knockback sites (`EntityBison`,
  `EntityKomodoDragon`, `EntityMoose`) construct and post the event directly and read `post()`'s boolean.
- **The mod bus is a `BusGroup`, and `BusGroup` has no `addListener` and no `post`.**
  `FMLJavaModLoadingContext.get().getModBusGroup()` supplies it; listeners go through each event's static
  `getBus(BusGroup)` (or `IModBusEvent.getBus(group, Class<T>)` for the ones that only implement the
  marker, e.g. `ModConfigEvent`). `DeferredRegister.register(BusGroup)` exists, so the ~20 registry calls in
  `AlexsMobs`'s constructor are **unchanged** — which is why the local keeps its `modBusEvent` name.

**`minecraft:tempt_range` — a LATENT CRASH found this wave that had been live on every `>=1.21.2` node since
Milestone 5.** From 1.21.2 on, vanilla's `TemptGoal.canUse` reads the new `minecraft:tempt_range` attribute
instead of a hardcoded 10-block radius, and vanilla only supplies it from `Animal#createAnimalAttributes` —
which **none** of this mod's ~96 attribute builders go through (they all start from `Mob#createMobAttributes`).
The first tick of any of the **39 mobs that use a vanilla `TemptGoal`** therefore threw
`IllegalArgumentException: Can't find attribute minecraft:tempt_range` and killed the server. Fixed centrally:
`AMEntityRegistry.initializeAttributes` now routes all 96 `event.put(TYPE, X.bakeAttributes().build())` calls
through a private `put(event, type, builder)` helper that adds `Attributes.TEMPT_RANGE, 10.0D` (vanilla's own
value) on `>=1.21.2`. Adding it to mobs that are never tempted is inert — nothing else reads the attribute.
Verified against the jars: the attribute **and** the goal's read of it both appear first in **1.21.3's**
predecessor 1.21.2 and not in 1.21.1 (`javap -c … TemptGoal | grep TEMPT_RANGE`).

> ⚠️ **The boot gate must SOAK, not just reach `Done (`.** This bug fires a few seconds *after* `Done (`,
> when a tempted mob first ticks — and the old gate killed the server the moment it saw that line, so eight
> nodes passed it for three milestones while crashing in practice. The gate is now
> **`scripts/bootgate.sh`**: start `runServer` detached, wait for `Done (`, **keep it running for
> `SOAK` (45 s) more**, kill, then grep the log **and list `versions/<node>/run/crash-reports/`**. Stash the
> old crash reports first (`mv` them away — `rm` is sandbox-blocked) or you cannot tell a new crash from a
> stale one.

### ✅ Milestone 9 — `1.21.7` + `1.21.8` (Forge & NeoForge)

**✅ Milestone 9 COMPLETE** (2026-07-26) — `1.21.7` + `1.21.8` (Forge & NeoForge). All **22** active nodes
build green in one invocation (`BUILD SUCCESSFUL in 1m 21s`, `MOD_IS_RELEASE=true`), and the full five-step
gate is green across all 22.

> The first run of that gate was **not** green, and how it failed is the most useful thing in this section.
> Build, both mixin verifiers and the 22-node boot gate were all green; the **client** gate then produced
> `rc=1` on `1.21.7-neoforge` and `1.21.8-neoforge` — from its **crash-report check**, because all 22 nodes
> had already printed the `Sound engine started` ready marker. The crash lands *after* the marker, during
> the first client resource reload. So the marker on its own proves nothing about the two new NeoForge
> nodes, and `clientgate.sh` is only load-bearing here because it also lists `run/crash-reports/`. See the
> `playBidirectional` bullet below for the fault itself.

**These two versions are nearly free on the vanilla axis** — 1.21.7 and 1.21.8 are bugfix releases, so the
entire mod compiled unchanged against them. All three changes the wave needed are **NeoForge platform**
breaks, and each one is confined to a single file:

- **NeoForge 1.21.7 removed `PacketDistributor.sendToServer`.** Serverbound sending moved to the
  client-only `net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(payload, …)`.
  Boundary verified empirically against the merged jars, not guessed: 1.20.6 / 1.21.1 / 1.21.5 / 1.21.6
  have `PacketDistributor.sendToServer` and **no** `ClientPacketDistributor`; 1.21.7 / 1.21.8 have exactly
  the reverse. `PacketDistributor.sendToPlayer` is untouched on every node.
  The one changed line sits inside `AMNeoNetwork`'s already-gated `//? if neoforge && >=1.20.6` block, and
  **Stonecutter blocks are siblings, never nested** — so it is extracted into a new
  **`message/AMNeoSend.java`** carrying two sibling gates (`neoforge && >=1.20.6 && <1.21.7` vs
  `neoforge && >=1.21.7`). On Forge that compilation unit is just a package declaration.
- **NeoForge 1.21.7 SPLIT `playBidirectional`'s handler in two — silently, keeping the old overload.** This
  is the one that shipped a fatal client crash past a green build and a green boot gate. 21.7 added a
  four-argument `playBidirectional(type, codec, serverHandler, clientHandler)` and **redefined the existing
  three-argument form to delegate to it as `(handler, null)`** — i.e. serverbound only. It compiles
  unchanged and the dedicated server is completely unaffected. `NetworkRegistry` now keeps
  `SERVERBOUND_HANDLERS` and `CLIENTBOUND_HANDLERS` as **separate maps** (`PayloadRegistration` no longer
  holds handlers at all) and `register` rejects only a null **serverbound** handler — a null clientbound one
  is legal, because the brand-new `RegisterClientPayloadHandlersEvent` may supply it later. The equally
  brand-new **`ClientNetworkRegistry#setup`** (absent in 21.6) then hard-throws when nothing ever did:

  ```
  java.lang.IllegalStateException: Some clientbound payloads are missing client-side handlers: [alexsmobs:main_channel]
    at …neoforge.client.network.registration.ClientNetworkRegistry.setup(ClientNetworkRegistry.java:90)
  ```

  reported as `ModLoadingCrashException: Mod loading has failed`. Fixed in `AMNeoSend.registerPlay` (same
  two sibling gates as `toServer`): the `<1.21.7` branch calls the three-arg form, the `>=1.21.7` branch
  passes **the same handler twice**, which reproduces the pre-21.7 semantics exactly. The argument order is
  `(serverHandler, clientHandler)` — bytecode-verified, since the names are not obvious: `playToServer`
  passes `(handler, null)` and `playToClient` passes `(null, handler)`. Registering the clientbound handler
  from **common** code is dedicated-server-safe: `NetworkRegistry#register` fills `CLIENTBOUND_HANDLERS`
  with no dist check, and `AMNeoNetwork#handle` names no client-only type.
- **NeoForge 21.8 rejects a mixin-added `EntityDataAccessor` on a vanilla entity class.** `CommonHooks`
  gained `verifyEntityDataAccessorRegistration`, called from `SynchedEntityData#defineId`; it finds the
  merged field by its `@MixinMerged` annotation and refuses it. That is *exactly* what the vendored Citadel
  entity-data store is (`mixin/LivingEntityMixin` merges a static `EntityDataAccessor<CompoundTag>` into
  `LivingEntity`). It throws `IllegalStateException` when `SharedConstants.IS_RUNNING_IN_IDE` and only logs
  a WARN otherwise — i.e. **it kills every dev launch and merely nags in production**, so it surfaced as an
  `ExceptionInInitializerError` out of `LivingEntity.<clinit>` during `Bootstrap.bootStrap`, long before
  anything mod-shaped ran.

  Fixed on **`neoforge && >=1.21.8` only** by moving the store to a **NeoForge data attachment**
  (`misc/AMCitadelDataAttachment`): an `AttachmentType<CompoundTag>` registered through a
  `DeferredRegister` on `NeoForgeRegistries.Keys.ATTACHMENT_TYPES`, with `.serialize(CompoundTag.CODEC
  .fieldOf("data"), tag -> !tag.isEmpty())` and `.sync(ByteBufCodecs.COMPOUND_TAG)`. The platform then does
  the persisting *and* the tracking-player sync itself, so on that node `LivingEntityMixin` drops its
  accessor, its `defineSynchedData` inject **and** both `CitadelData` save/load injects — its two public
  accessors just delegate to `Entity#getData`/`#setData`. Every other node keeps the `SynchedEntityData`
  implementation byte-for-byte; Forge has no attachments, and reworking 21 already-verified nodes to fix
  one would be the wrong trade.

  Three details worth not re-deriving: the `serialize` predicate means entities that never touch the store
  (i.e. almost all of them) add nothing to the save; `Entity#syncData` returns early off a `ServerLevel`,
  so a client-side `set` stays local — the same behaviour `SynchedEntityData#set` had, which both
  `CitadelClientProxy` and the clientbound half of `PropertiesMessage` rely on; and
  `AttachmentType.builder` is **overloaded on `Supplier<T>` and `Function<IAttachmentHolder,T>`**, so
  `CompoundTag::new` is ambiguous — pass an explicit `() -> new CompoundTag()`.

  > The gate proves this node *registers and boots*. It does **not** exercise the store's runtime
  > round-trip — every consumer (`RockyChestplateUtil`, `FlyingFishBootsUtil`, `TendonWhipUtil`,
  > `VineLassoUtil`, `RainbowUtil`, `SquidGrappleUtil`) needs a real player. Test those in-game before
  > publishing a `1.21.8-neoforge` jar.

**The `setCanceled` audit is retired for new nodes** — do not re-run it above 1.21.5. On Forge `>=1.21.6`
(EventBus 7) there are **zero** live `setCanceled` sites, and on NeoForge `setCanceled` exists only on the
`ICancellableEvent` interface (javap-confirmed in `bus-8.0.5.jar`), so an event that loses cancellability
is a **compile error**, not a runtime `UnsupportedOperationException`. The trap documented under Milestone 8
only ever applied to Forge `<1.21.6`.

> ⚠️ When auditing with `grep`, remember Stonecutter's inactive branches are `/* … */`-commented in the
> *generated* sources. A naive grep over `versions/*/build/generated/**` reported 15 live `setCanceled`
> sites on every node **including the EventBus-7 ones**, which is impossible. Strip block **and** line
> comments before counting.

Four smaller things this wave needed: `run/eula.txt` (`eula=true`) pre-created for the four new nodes;
`versionRange` verified as three-component exact (`[1.21.7]` / `[1.21.8]`) in the shipped manifests; the
data-pack migrations confirmed firing with the same counts as the established nodes (194 / 45 / 590 / 54 /
15 / 86); and `scripts/verify_assets.py` run (`asset literals=394 missing=0` — source-level, so
node-independent) although it is still not part of the documented gate.

**One new benign log line, and it is a red herring in crash reports.** NeoForge 1.21.7 added
`OnlyInWarningsHandler`, which walks every mod's `@OnlyIn` usages and logs one **ERROR**-level line each
("the runtime member-stripping behaviour of this annotation is no longer present"): ~20 lines per client
launch here, covering `BlockCapsid#skipRendering`, six `TileEntity*#getRenderBoundingBox` overrides and all
of `client/event/ClientEvents`. It is **harmless** — nothing in this mod ever relied on stripping; those
members are only ever reached from client code. What makes it worth documenting is that it also registers a
**mod-loading issue**, so it becomes the *first* `-- Mod loading issue --` block of every 21.7+ crash report,
with `Failure message: loadwarning.neoforge.onlyin` and `Exception message: <No associated exception found>`.
Read past it to the block that actually has a stacktrace — the `playBidirectional` crash above was the second
of two, and the first is never the cause.

**So the benign-`/ERROR]` filter is three patterns now, and the dist one is not spelled `RuntimeDistCleaner`
everywhere.** Forge logs the blocked client classes from `RuntimeDistCleaner`; **NeoForge logs the identical
thing from `NeoForgeDevDistCleaner`** — grep for both or a NeoForge log reads as full of errors. The count is
also not always "five": it is **5** on a `<1.21.2` node (the five client mixins' targets — `Gui`,
`ClientLevel`, `HumanoidModel`, `ItemBlockRenderTypes`, `LevelRenderer`) and **9** on a `>=1.21.2` one, where
the render-state mixin pair adds `EntityRenderer`, `EntityRenderState`, `EntityModel` and `Model`. Every entry
is a **vanilla client class**; a mod class appearing in that list would be a real fault. Third pattern:
`RealmsClient: Failed to fetch Realms feature flags`, also ERROR-level, in any dev client with no Mojang
session. With `RuntimeDistCleaner|NeoForgeDevDistCleaner|OnlyInWarningsHandler|RealmsClient` filtered, a
healthy log has **zero** `/ERROR]` lines.

### ✅ Milestone 10 — `1.21.9` (Forge & NeoForge)

Code complete 2026-07-27, and the **full five-step gate is green on all 24 nodes** (finished the same
day, after a mid-gate PC crash — see the recovery note below). `mod.version` is deliberately untouched
(`1.0.8`) — **nothing from Milestones 9 or 10 has been published**.

| Step | Result |
|---|---|
| 24-node `:build` (`MOD_IS_RELEASE=true`) | ✅ `BUILD SUCCESSFUL in 3m 13s` |
| `verify_mixins.py` | ✅ `jars=24 problems=0` |
| `verify_mixin_targets.py` | ✅ `jars=24 selectors=225 problems=0` |
| `SOAK=45 scripts/bootgate.sh` × 24 | ✅ all `DONE`, `rc=0`, no crash-reports, no non-benign log lines |
| `scripts/clientgate.sh` × 24 | ✅ all 24 reach `Sound engine started`, `rc=0`, no crash-reports |

Both 1.21.9-forge runtime faults below were found and fixed *by* that boot gate, so its results are
post-fix.

> **Recovering a gate run after a crash — the logs are enough, don't blanket re-run.** The machine died
> mid-sweep. `build/bootgate/soak-<node>.log` and `build/clientgate/cgate-<node>.log` are per-node and
> survive, so a post-hoc pass reconstructs the verdict: `Done (` present, zero `/ERROR]` lines after
> filtering the four benign patterns, and an empty `versions/<node>/run/crash-reports/`. The one thing a
> log **cannot** show is whether the 45 s soak completed — a healthy server writes nothing while idling,
> so the last line is the `Done (` line either way and the file mtime matches it. Only the node that was
> mid-soak is ambiguous; re-run that one. Here 23 nodes reconstructed clean and only `1.21-forge` needed
> re-running.

1.21.9 is a **mega-wave with three parts**, not the one the old era-table row named: (i) a wide
mechanical vanilla sweep, (ii) a full **particle extract/submit rewrite**, (iii) the
**`SubmitNodeCollector`** pipeline for renderers / layers / tile renderers. ~150 errors/node.

**Design (don't re-derive): the 1.21.9 submit pipeline is absorbed in
`client/render/compat/`** exactly like 1.21.2's render-state rewrite, so the ~130 renderers / ~37 layers /
~130 models stay untouched. New pieces there: **`AMSubmitBuffers`** (a recording `MultiBufferSource` +
`SubmitNodeCollector.CustomGeometryRenderer`; ctors `(SubmitNodeCollector, CameraRenderState)` and
`(SubmitNodeCollector)`, statics `of(MultiBufferSource)` / `collectorOf(MultiBufferSource)`, methods
`collector()`/`camera()`/`getBuffer(RenderType)`/`flush()`), plus `compat/BlockEntityRenderer` +
`AMBlockEntityRenderState` for the eight tile renderers. Two accepted fidelity losses, documented in
`AMSubmitBuffers`' header: one extra vertex copy and one recorder per (entity, RenderType), and
**no outline support**.

The **camera state** was the one open design question, and it is answered without an access transformer:
several entry points hand out a collector but no `CameraRenderState` (both NeoForge `RenderLivingEvent`
flavours, `RenderHandEvent` on both loaders, every render layer), and the frame's own state lives on the
private `LevelRenderer#levelRenderState`. Those sites pass the collector-only ctor and `camera()` lazily
rebuilds an equivalent state from `Minecraft.getInstance().gameRenderer.getMainCamera()` — every field of
`CameraRenderState` (`blockPos`, `pos`, `orientation`, `entityPos`, `initialized`) is a straight copy of
something the live `Camera` exposes, so the reconstruction is exact, and it happens at most once per
rendered entity.

**Tile renderers** get the same seam one level down: `compat/BlockEntityRenderer<T>` is an *interface* with
default `createRenderState`/`extractRenderState`/`submit` implementations, and the `!mc2109-tile-import`
replacement points the eight tile renderers' `import …blockentity.BlockEntityRenderer;` at it. Their
`render(T, float, PoseStack, MultiBufferSource, int, int, Vec3)` bodies are untouched; the state
(`AMBlockEntityRenderState`) carries the tile, partial tick and camera position across the extract/submit
split. Below 1.21.9 the file is a bare `package` declaration and the import resolves to vanilla.

**Particles (Step 3).** 1.21.9 split particles into extract + submit too, and the three particles here
that draw **custom geometry** rather than a quad from the particle atlas — `ParticleStaticSpark`,
`ParticleSkulkBoom`, `ParticleBearFreddy` — have no place to put a hand-written `render` body any more.
`ParticleRenderType.CUSTOM` is gone; they return `getGroup() → ParticleRenderType.NO_RENDER` on `>=1.21.9`,
i.e. **they are invisible on those nodes**. Accepted cosmetic regression, in the same class as the 1.21.4
in-hand-model losses: the void-portal static, the skulk boom flash and the Freddy easter-egg particle.
Everything else — every atlas-quad particle, which is all the rest — is unaffected.

**`renderColoredModel` drew nothing from 1.21.2 until this wave** (found while auditing the layer paths,
fixed here, applies to all `>=1.21.2` nodes). `compat/RenderLayer` used to delegate to vanilla's static
`renderColoredCutoutModel`, which calls `Model#renderToBuffer` — made `final` in 1.21.2, so it walked the
empty root that the compat `EntityModel` hands vanilla and emitted no vertices. It now reproduces the
two-line body against the compat model's own eight-float `renderToBuffer`, which is what actually draws.

⚠️ **`AMCompat.getBlockEntityData` returns a LIVE tag below 1.20.5 and a COPY at and above it** — vanilla's
`BlockItem.getBlockEntityData` handed back the stack's own `CompoundTag`, whereas `CustomData#copyTag`
(1.20.5+) and `TypedEntityData#copyTagWithoutId` (1.21.9+, which is what 1.21.9 retyped
`BLOCK_ENTITY_DATA` to) both copy. Anything that *mutates* the returned tag therefore silently stops
having an effect above 1.20.4. Both callers (`BlockTerrapinEgg`, `AMBlockItem`) only read it, and
`AMBlockItem` re-applies the sub-tag on placement explicitly — keep it that way.

**Edits this wave, part 1:**
`AMCompat` — `setOwnerUUID` → `EntityReference.of(uuid)` (ctors went private); `getBlockEntityData` →
`TypedEntityData#copyTagWithoutId`; `spawnEgg` → `new SpawnEggItem(props.spawnEgg(type.get()))` (the
`EntityType` moved into a data component — **getting this wrong registers all ~117 eggs with no mob**).
`GuiBasicBook` + `AMItemstackRenderer` — the two removed dispatcher overrides
(`overrideCameraOrientation`/`setRenderShadow`) gated `<1.21.9`, and `AMItemstackRenderer` grew a
`>=1.21.9` render arm through `AMRenderCompat.renderEntity`. `AMItemstackRenderer` mob-effect sprite →
`Minecraft.getAtlasManager().getAtlasOrThrow(net.minecraft.data.AtlasIds.GUI).getSprite(...)`.
New `AMRenderCompat.cameraOrientation(dispatcher)` (`>=1.21.9`: `dispatcher.camera.rotation()`), with
all four call sites (`RenderSunbird`, `RenderSeal`, `RenderMudBall`, `RenderFarseer`) routed through it.
`TabulaModelBlock` — 9-arg `ItemTransforms` (new `fixedFromBottom` → `NO_TRANSFORM`).
`LayerUnderminerItem` — the dead `else if (… instanceof ArmedModel)` branch gated `<1.21.9`
(`ArmedModel` went generic over the render state). `ClientLayerRegistry` — new forge/neoforge `>=1.21.9`
arms using **`net.minecraft.world.entity.player.PlayerModelType`** (was `PlayerSkin.Model`) and
**`AvatarRenderer`** (was `PlayerRenderer`); Forge's accessors are `getModelTypes()`/`getPlayerRenderer()`,
NeoForge's are `getSkins()`/`getPlayerRenderer()`. The `!mc2109-playerskin` replacement rule was
**deleted** — it pointed at a nested enum that no longer exists, and `ClientLayerRegistry` was its only
consumer. `AMItemHandlers` — split into three arms; NeoForge 1.21.9 rebuilt item transfer on
`ResourceHandler<ItemResource>`, so it is `Capabilities.Item.BLOCK` + `IItemHandler.of(handler)` for the
consumer and `new net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper(capsid, side)` for the
provider.

**Edits this wave, part 2 — each with the VERIFIED destination API:**

| Site(s) | Break | Fix applied |
|---|---|---|
| `AlexsMobs.java`, `citadel/Citadel.java` (**neoforge only**) | `FMLEnvironment.dist` field removed | `FMLEnvironment.getDist()`, new `neoforge && >=1.21.9` arm |
| `client/event/ClientEvents.java` ×6 call sites | `RenderLivingEvent`/`RenderHandEvent` lost `getMultiBufferSource()`; `RenderLivingEvent` also lost `getPackedLight()` | four private helpers in the existing `renderedEntity`/`renderedPartialTick` style: `renderedBuffers(event)` (Forge `new AMSubmitBuffers(event.getNodeCollector(), event.getCameraState())`, NeoForge collector-only), `handBuffers(event)`, `flushBuffers(buffers)` (replays the recorder; a no-op below 1.21.9 — **every recorded body must be flushed or it draws nothing**) and `renderedLight(event)` (`state.lightCoords`; `RenderHandEvent` keeps `getPackedLight()`) |
| `client/event/ClientEvents.java` (**forge only**) | Forge 1.21.9 restructured `TickEvent` into sealed interfaces + `Pre`/`Post` records; no `phase`/`Phase` | subscribe `TickEvent.ClientTickEvent.Pre` — the same shape NeoForge already had |
| `event/ServerEvents.java` (**forge only**) | `LevelTickEvent.level` field → `level()`; the parent interface has no `BUS` | separate `Pre`/`Post` handlers calling `tick.level()` |
| `client/gui/ButtonTransmute.java` | `Button.onPress()` → `onPress(net.minecraft.client.input.InputWithModifiers)` | `!mc2109-onpress-decl` + `-super` rules |
| `LayerKangarooArmor.java`, `LayerMimicubeHelmet.java` | `EquipmentLayerRenderer.renderLayers` is now `<S>(EquipmentClientInfo.LayerType, ResourceKey<EquipmentAsset>, Model<? super S>, S state, ItemStack, PoseStack, SubmitNodeCollector, int light, int outlineColor)` — was 7 args with a raw `Model` and a `MultiBufferSource` | two `>=1.21.9` rules keyed on each call's **trailing** text (the leading part is already rewritten by the earlier `!mc2104-equip-*` rules, so it must not be matched on): insert the `neutralArmorState` both sites already hold, route the buffer through `AMSubmitBuffers.collectorOf(...)`, append `0` for outline colour |
| `EntityStraddleboard.java` **+ 11 silent non-`@Override` copies** | `Entity.lerpMotion(double,double,double)` → `lerpMotion(Vec3)` | two blanket rules re-declaring x/y/z as locals on the same line, so no body moves |
| `EntityVoidWorm.java` ×2 | `shouldDropLoot()` → `shouldDropLoot(ServerLevel)` | `replace("this.shouldDropLoot()", "this.shouldDropLoot(serverLevel)")` — `serverLevel` is already a local at both gated sites |
| `TileEntityCapsid.java` ×2 **+ `EntityKangaroo`** | `Container.startOpen/stopOpen` take `net.minecraft.world.entity.ContainerUser` | two blanket decl rules; no body reads the parameter |
| `item/ItemDimensionalCarver.java` (**forge only**) | `RespawnConfig.getDimensionOrDefault` is not public in the Forge jar | inline `amCfg == null ? Level.OVERWORLD : amCfg.respawnData().dimension()` |
| `mixin/client/LevelRendererMixin.java` | `Entity#getTeamColor` moved out of `LevelRenderer` entirely — the outline colour is baked into the render state by `EntityRenderer#extractRenderState` | a third gated arm **plus** a gated class-level `@Mixin`, target spelled fully qualified. See the `getTeamColor` table in [`mixins.md`](mixins.md) |

**Two runtime-only faults, both `1.21.9-forge` only, both invisible to the compiler and to
`verify_mixins*.py` — the boot gate is the ONLY thing that caught them:**

1. **A `static final Set.of(Items.…)` field in `AMCompat` died in `Bootstrap.bootStrap`.** 1.21.9's
   bootstrap chain is `Items.<clinit>` → `Item.Properties` → `DataComponents` → `EntityType` →
   `LivingEntity`, and `LivingEntity.<clinit>` reaches `AMCompat.<clinit>` because the vendored
   Citadel data store defines a static `EntityDataAccessor` there. So `AMCompat`'s own static init
   observes a **half-initialised `Items`** whose every field is still null, and `MEATS = Set.of(…)`
   threw `NullPointerException: Cannot invoke "Object.hashCode()" because "pe" is null` from
   `ImmutableCollections$SetN`, killing the server before mod loading. The list moved into a
   **lazily-loaded private holder class `AMCompat.Meats`**, so it is built on the first `isMeat()`
   call instead. ⚠️ **Never give `AMCompat` a static field that touches a vanilla registry or
   `Items`/`Blocks` constants** — the class is reachable from vanilla's own bootstrap. NeoForge
   1.21.9 was unaffected only because its `>=1.21.8` arm moved the store to a data attachment, so
   its `LivingEntityMixin` has no static accessor and never pulls `AMCompat` in that early.
2. **Forge 59 took `SpawnPlacementRegisterEvent` AND `EntityAttributeCreationEvent` off the mod
   bus.** Both are plain default-bus events with a static `BUS` field now; their
   `getBus(BusGroup)` survives only as `@Deprecated(forRemoval, since = "1.21.9")` returning that
   same `BUS`. Those two are exactly and only what `AMEntityRegistry`'s
   `@Mod.EventBusSubscriber(bus = MOD)` carried, so `AutomaticEventSubscriber` hard-failed at
   CONSTRUCT with `IllegalArgumentException: BusGroup "modBusForalexsmobs" requires all events on
   it to inherit from interface …IModBusEvent but class …SpawnPlacementRegisterEvent doesn't`.
   Fix: the class annotation is gated to `(forge && <1.21.9) || <1.20.6`, and on `forge &&
   >=1.21.9` `AlexsMobs`' constructor adds both listeners by hand off their static `BUS`. The
   nesting rule forced this shape — the handler sits inside a `>=1.20.5` block, so its
   `@SubscribeEvent` could not be gated in place.
   > The generalisable trap: **an `X.getBus(modBusEvent)` call that still compiles proves nothing
   > about which bus `X` is on.** Forge keeps the method as a deprecated identity shim while the
   > event migrates, so only automatic subscription fails, and only at runtime.

**Traps learned this milestone — carry them forward:**

- ⚠️ **The rule `!mc121-vtx-color` (`stonecutter.gradle.kts:144`) rewrites EVERY `.color(` in the tree** to
  `.setColor(`. So **`ARGB.color(...)` cannot be spelled anywhere** — pack ARGB ints by hand.
- ⚠️ **A method without `@Override` whose supertype signature changed becomes SILENTLY DEAD CODE.** No
  compile error. This bit the 11 `lerpMotion` copies, `EntityKangaroo`'s anonymous `stopOpen`, and the two
  `shouldRenderOffScreen` spellings. When a vanilla signature moves, grep for the *old* spelling, don't
  wait for errors.
- **Both merged jars ship `.java` sources** — a far better oracle than `javap`. Reference trees currently
  on disk: `/tmp/mc2109/` (vanilla `net/minecraft/**` + NeoForge `net/neoforged/**`),
  `/tmp/fg2109x/` (Forge 1.21.9 `net/minecraftforge/**`), `/tmp/fg2108x/`.
- **Don't slice a multi-node Gradle log by task name** — Gradle interleaves output. Slice by path prefix:
  `grep -oE 'versions/1\.21\.9-neoforge/[^:]*\.java:[0-9]+: error'`. And **Gradle mirrors compile output**,
  so `grep -c 'error:'` double-counts — trust the per-node `N errors` summary line.
- `client/render/compat/**` and `mixin/renderstate/**` are excluded from the compile **below 1.21.2**
  (`ModPlatformPlugin.kt:197`), but compat sources still have to compile on 1.21.2–1.21.8 — so anything
  1.21.9-only inside them must be gated, leaving a bare `package` declaration (a legal compilation unit;
  precedent `message/AMNeoSend.java`).

- ⚠️ **A Stonecutter rule keyed on a call's LEADING text can be pre-empted by an earlier rule group.**
  The `replacements` blocks run in file order, so on a 1.21.9 node the `>=1.21.4` group has already
  rewritten `EquipmentModel.LayerType` → `EquipmentClientInfo.LayerType` before the `>=1.21.9` group is
  reached. Key a late rule on **trailing** text that no earlier rule touches.
- **A `>=1.21.9`-only compile fix is not the same as a `>=1.21.9`-only mixin fix.** Three of the four
  mixin faults this project has hit were invisible to the compiler; the two verifier scripts are the only
  thing between them and players. Run both after **every** wave, not just before a publish.

### ✅ Milestone 11 — `1.21.10` (Forge & NeoForge)

Code complete 2026-07-27. **26 active nodes.** `mod.version` still `1.0.8` — nothing from Milestones
9, 10 or 11 has been published.

| Step | Result |
|---|---|
| 26-node `:build` (`MOD_IS_RELEASE=true`) | ✅ `BUILD SUCCESSFUL in 2m 27s` |
| `verify_mixins.py` | ✅ `jars=26 problems=0` |
| `verify_mixin_targets.py` | ✅ `jars=26 selectors=243 problems=0` |
| `SOAK=45 scripts/bootgate.sh` × 26 | ✅ all `DONE`, `rc=0`, no crash-reports, no non-benign log lines |
| `scripts/clientgate.sh` × 26 | ✅ all 26 reach `Sound engine started`, `rc=0`, no crash-reports |

Also checked, as every wave should: the shipped `versionRange` is the exact three-component
`[1.21.10]` on both new nodes; the two new jars' `data/`+`assets/` entry counts match
`1.21.9-neoforge` exactly (so every data-pack migration fired identically); and
`scripts/verify_assets.py` is `asset literals=394 missing=0`.

**1.21.10's entire vanilla surface delta, for this mod, is TWO methods** — this is the cheapest wave
since 1.21.7/1.21.8:

| Site | Break | Fix |
|---|---|---|
| `BlockBehaviour#entityInside` | gained a trailing `boolean` — "the entity's bounding box actually intersects this block", as opposed to merely having swept through it (`Entity#checkInsideBlocks` computes it as `flag \|\| aabb.intersects(pos)`) | a third arm on each of the three overriding blocks |
| NeoForge `IBlockExtension`/`IBlockStateExtension#onDestroyedByPlayer` | gained `ItemStack toolStack` after `Player` (following vanilla `ServerPlayerGameMode#removeBlock`, which gained the same) | `//? if neoforge && >=1.21.10` arm on `BlockTransmutationTable`; body extracted to a private `explodeOnDestroy` so it is not duplicated |

**Forge 60 does NOT take the `onDestroyedByPlayer` change** — `IForgeBlock` keeps the six-argument
form on every node, so the gate is `neoforge && >=1.21.10`, not `>=1.21.10`.

#### A silently-dead override that had been live since Milestone 7

`BlockEndPirateAnchor#entityInside` — the anchor-chain climb assist — **stopped being called on every
`>=1.21.5` node** the moment 1.21.5 added `InsideBlockEffectApplier` to the signature. It carries no
`@Override` (upstream style), so it was never a compile error; it just quietly became a method nobody
calls, for four milestones. It is now a three-arm chain (`>=1.21.10` / `>=1.21.5` / else) over a
shared private `climbChain(BlockState, Entity)`.

`BlockBananaPeel#entityInside` is stale in exactly the same way and was **deliberately left alone**:
its body is empty and `BushBlock` declares no `entityInside` in any version, so the override is a
no-op whether it binds or not.

#### The tool that would have caught it: a DESCRIPTOR-level diff, not a name-level one

The 1.21.10 recon originally concluded "vanilla-free" because it compared vanilla method **names**
per class between versions. `entityInside` still exists by that name — only its parameter list moved
— so a name diff is structurally blind to precisely the change that produces silently-dead code.

`/tmp/sigdiff.py` (keep it, or re-derive it) indexes both versions' **sources jars**
— both merged jars ship `.java`, a far better oracle than `javap` — with a method-declaration regex
(⚠️ `re.M`, or it silently matches nothing and reports a clean diff), normalises each parameter list
to bare simple type names, and reports every method whose old and new parameter sets are **disjoint**.
Run it over `net/minecraft`, `net/neoforged` and `net/minecraftforge`:

```bash
python3 sigdiff.py <old>/net/minecraft <new>/net/minecraft out.json
```

Results for this wave, which is why the port was three lines of real work:

| Axis | Changed methods | Ones this mod touches |
|---|---|---|
| vanilla 1.21.9 → 1.21.10 | 27 (26 of them `entityInside` overrides) + `ServerPlayerGameMode#removeBlock` | `entityInside` |
| NeoForge 21.9 → 21.10 | 11 | `IBlockExtension#onDestroyedByPlayer` |
| Forge 59 → 60 | 3 (`FramePassManager#insertForgePasses`, `RenderTooltipEvent.Pre`, `AttachCapabilitiesEvent`) | none |

**Run this diff at the start of every wave from now on.** It is the only mechanical check for the
"method without `@Override` whose supertype signature changed" trap that the porting rules already warn
about — and it turns that warning into something you can execute rather than remember.

### ✅ Milestone 12 — `1.21.11` (Forge & NeoForge)

Code complete 2026-07-27, full five-step gate green 2026-07-28. **28 active nodes — the node map in
this file is now fully built out.** `mod.version` is still `1.0.8`; **nothing from Milestones 9–12
has been published.**

| Step | Result |
|---|---|
| 28-node `:build` (`MOD_IS_RELEASE=true`) | ✅ `BUILD SUCCESSFUL in 2m 16s` |
| `verify_mixins.py` | ✅ `jars=28 problems=0` |
| `verify_mixin_targets.py` | ✅ `jars=28 selectors=261 problems=0` |
| `SOAK=45 scripts/bootgate.sh` × 28 | ✅ all `DONE`, `rc=0`, no crash-reports, no non-benign log lines |
| `scripts/clientgate.sh` × 28 | ✅ all 28 reach `Sound engine started`, `rc=0`, no crash-reports |

Also checked, as every wave should: shipped `versionRange` is the exact three-component `[1.21.11]`
on both new nodes; the two new jars' `data/`+`assets/` entry counts match `1.21.10-{forge,neoforge}`
exactly per loader (684/3464 and 685/3464), so every data-pack migration fired identically; and
`verify_assets.py` is `asset literals=394 missing=0`.

**117 errors/node on first compile.** The pre-wave recon called this "a package reorganisation, not
an API break" — that was right about the *bulk* and wrong about the *tail*. The 37 package moves and
the `ResourceLocation`→`Identifier` rename really are mechanical, but underneath them sit ~10
genuine API changes, most of which the recon's import-level survey structurally could not see
(they change a *signature* or delete a *method*, not a type name).

> The **descriptor-level diff** Milestone 11 added (`sigdiff.py`, "Run this diff at the start of
> every wave") is exactly the tool that would have surfaced that tail up front, and it was **not**
> run before this wave — the recon was an import survey instead. Every one of the ten source-level
> breaks below was found the expensive way, by compiling. Run the diff first.

**The mechanical half — the `>=1.21.11` `replacements` group:**

- **`ResourceLocation` → `Identifier`**, same package. 926 sites / 218 files, and the **first
  `regex()` rule in the project**. `regex()` takes an **explicit reverse pattern**, which is what makes
  a `\b`-anchored rename exact in both directions — a `string()` rule cannot express one.
  > The pre-wave note said the vendored Tabula members (`getIdentifier`,
  > `parentIdentifier`, `identifier` — 13 sites) had to be renamed off the colliding token first.
  > **They did not**: `\bIdentifier\b` cannot match inside `getIdentifier`/`parentIdentifier`, and
  > `identifier` is a different case. Word boundaries solved it; no mod-internal rename happened.
  > Those Tabula field names are `.tbl` JSON keys, so leaving them alone was also the safer answer.
- ⚠️ **A `regex()` rule must be GROUP-FREE.** Stonecutter splices a `$1` reference out of the
  **original** text at the **original** offset while writing into the already-shifted buffer, so the
  moment a replacement changes length every later group on that file comes back as garbage sliced
  from the wrong place (`"RenderTypes.t.renderer.()"`). The obvious one-regex-with-a-captured-name
  rewrite of the `RenderType` split is therefore impossible — it is **one `string()` rule per
  factory** (12 of them) instead. Group-free regexes like `!mc2111-identifier` are fine.
- **`RenderType` split in two and moved**: the class is now just the render-layer type in
  `net.minecraft.client.renderer.rendertype`, every static factory moved to a sibling `RenderTypes`,
  and `RenderType` became **concrete with a private constructor**. So `AMRenderTypes extends
  RenderType` stops compiling — the `>=1.21.11` arm drops the `extends` entirely, and the three
  vanilla factories callers used to reach through **static inheritance** are re-declared as explicit
  delegates. That inheritance breaking *silently* (the class still exists) is the trap.
- **37 pure package moves**, one `string()` rule each — `net.minecraft.Util`/`BlockUtil` into
  `net.minecraft.util`, the animals into per-mob sub-packages (`animal.fish.*`, `animal.feline.*`,
  `animal.chicken.*`, …), `monster.zombie.*`/`monster.skeleton.*`/`monster.illager.*`,
  `npc.villager.*`, `projectile.arrow.*`, `vehicle.boat.*`, `level.gamerules.GameRules`, and
  `advancements.critereon` → `advancements.criterion` (vanilla fixed its own typo).
  > ⚠️ **A wildcard import is not something a replacement rule can follow.** `ServerEvents` imports
  > `net.minecraft.world.entity.animal.*`; six of those classes moved. Fixed by adding six
  > *redundant* single-type imports whose only purpose is to give the `!mc2111-pkg-*` rules a line to
  > rewrite. They are no-ops on every other node.
- Smaller renames, each deliberately **not** blanket-able: `Entity.hasImpulse` → `needsSync` (keyed
  on the whole `<expr>.hasImpulse = true;` statement, all 25 sites); `ResourceKey#location()` →
  `identifier()` (5 individually-named sites — **`TagKey#location()` survives unchanged**, so a
  blanket rule would break `SpawnBiomeData`); `Camera`'s dropped `get-` prefixes (1.21.10's `Camera`
  already declares **both** `getPosition()` and `position()`, so a bare rename is a silent no-op
  there and cannot be reversed); `ArmPose.THROW_SPEAR` → `SPEAR` **plus** a second rule for the
  unqualified `case THROW_SPEAR:` inside `ModelUnderminerDwarf`'s two switches.
- `VillagerTrades.ItemListing#getOffer` gained a leading `ServerLevel` — declaration-only rewrite
  (neither implementation reads it and nothing in the mod calls `getOffer`).
- `EntityRenderState` lost `hitboxesRenderState` (hitbox rendering moved out of the per-entity
  state); the assignment is replaced with a comment, because the site sits inside `AMRenderCompat`'s
  `>=1.21.9` arm and blocks cannot nest.

**The half the recon missed — real API changes, all source-level:**

| Site | Break | Fix |
|---|---|---|
| `AMCompat.gameRule` | `GameRules` moved package **and** was retyped: `GameRules.Key<BooleanValue>` → `GameRule<Boolean>`, read with `get()` not `getBoolean()`; several rules renamed (`MOB_LOOT`→`MOB_DROPS`, `SPAWN_MOBS`, `ADVANCE_WEATHER`) | a fourth arm on the existing `Rule` enum switch — the one place a vanilla constant is spelled |
| `AMCompat.timeOfDay` (new) | `Level#getTimeOfDay(float)` and `LevelTimeAccess` **deleted**; the day fraction is the `SUN_ANGLE` environment attribute, in degrees | `level.environmentAttributes().getValue(SUN_ANGLE, pos) / 360.0F` — the same division vanilla's own `Time` item property does |
| `AMCompat.difficultyAt` (new) | `getCurrentDifficultyAt` left `Level`; it survives only on `ServerLevelAccessor` | cast behind the `isClientSide` check every call site already has; a client caller gets a PEACEFUL default rather than a CCE |
| `citadel/…/raycoms/ChunkCache` | `LevelReader` gained abstract `environmentAttributes()` | delegate to the backing world — the cache is a block-lookup view of it |
| `client/render/LavaVisionFluidRenderer` | `LiquidBlockRenderer` gained a `MaterialSet` constructor parameter (its five fluid sprites resolve eagerly now) | **`AtlasManager` itself implements `MaterialSet`**, so `Minecraft.getInstance().getAtlasManager()` is the exact set vanilla passes — **no access transformer on the private `BlockRenderDispatcher#materials` needed** |
| `EntityFroststalker.isHotBiome` | `BiomeTags.SNOW_GOLEM_MELTS` **deleted** — "does snow melt here" is spatial now | `environmentAttributes().getValue(SNOW_GOLEM_MELTS, this.position())`, sampled at the entity like vanilla's `SnowGolem` (the old code probed the `(x, 0, z)` column) |
| `EntityTiger`, `EntityAnteater`, `EntityLeafcutterAnt`, `EntityGrizzlyBear` | `NeutralMob` swapped its remaining-ticks pair for an **absolute end time**, and its `lastHurtBy`/anger-target UUID became an `EntityReference` | the existing synched `int` storage stays the source of truth (the mod's own renderers read it client-side); `getPersistentAngerEndTime()` derives from it |
| the same three (**not** `EntityGrizzlyBear`, which never called `updatePersistentAnger`) | 1.21.11 also **dropped the countdown that used to live in `updatePersistentAnger`** — a compile-clean behaviour deletion | `customServerAiStep` decrements by hand |
| `client/render/compat/AMSubmitBuffers` | `VertexConsumer.setColor(int)` **and** `setLineWidth(float)` both became **abstract** (`setColor(int)` used to be a default) | implemented on the private `Recorder`, deliberately **without `@Override`** — the file already sits inside a `>=1.21.9` block and blocks cannot nest, so below 1.21.11 they are simply unused extra methods |

**Two traps worth carrying forward, both new this wave:**

- ⚠️ **A replacement's `to` text must never contain `/* … */`.** Replacements run while the target
  arm is still block-commented, so a nested block comment closes the outer comment early and the
  file explodes in a way that points nowhere near the rule. Use `//` line comments
  (`!mc2111-hitboxstate` does). The same applies to the single-line gate form: the gated line takes
  a `//` prefix, **not** `/*`.
- ⚠️ **An access-transformer entry can need a new SRG id, not just a new package.** 1.21.11 moved
  `AbstractArrow` into `projectile.arrow` *and* renumbered `setPierceLevel` **`m_36767_` →
  `m_443362_`**. NeoForge reads the Mojmap AT and only needed the new package; **Forge reads the SRG
  one and needed both**, which is why it passed on one loader and failed on the other. Verify with
  `javap -p` on loom's `minecraft-merged-srg-at-patched.jar`. Both eras just sit in the file — AT
  files are not preprocessed and a non-matching entry is a silent no-op.

### ✅ Milestone 13 — `26.1.2` (Forge & NeoForge)

Code complete 2026-07-28. **30 active nodes.** `mod.version` still `1.0.8` — nothing from Milestones
9–13 has been published.

| Step | Result |
|---|---|
| 30-node `:build` (`MOD_IS_RELEASE=true`) | ✅ `BUILD SUCCESSFUL in 3m 42s` |
| `verify_mixins.py` | ✅ `jars=30 problems=0` |
| `verify_mixin_targets.py` | ✅ `jars=30 selectors=276 problems=0` |
| `SOAK=45 scripts/bootgate.sh` × 30 | ✅ all `DONE`, `rc=0`, no crash-reports, **and `nonbenign=0` on all 30 — but only after the fix below** |
| `scripts/clientgate.sh` × 30 | ✅ all 30 reach `Sound engine started`, no crash-reports — **but only after TWO further Forge-26 fixes, below** |

The client gate's first 30-node run was **29 READY / 1 FAILED**, and `26.1.2-forge` then failed a
*second* time on a different fault once the first was fixed. Both were client-only and both were
invisible to the build, to both mixin verifiers and to the boot gate. After fixing them, the full
30-node build + both verifiers were re-run green (`jars=30 problems=0`, `selectors=276 problems=0`),
the boot gate was re-run on both 26 nodes (the only ones whose server-side dist behaviour changed),
and the client gate was re-run on `26.1.2-forge`, `26.1.2-neoforge` and a four-node sample spanning
both loaders and the pre-/post-render-state eras (`1.20.1-forge`, `1.21.1-neoforge`, `1.21.5-forge`,
`1.21.9-neoforge`, `1.21.11-forge`) — all green. The other 24 nodes were **not** re-client-gated: the
only shared-source change is a constructor that now does strictly *less* work (see the second fault),
and the constructor is the only part of that file a title-screen gate ever reaches.

Also checked, as every wave should: the shipped `versionRange` is the exact three-component
`[26.1.2]` on both new jars; `scripts/verify_assets.py` is `asset literals=394 missing=0`; and
`26.1.2-neoforge` ships `data=685 assets=3464`, identical to `1.21.11-neoforge`, so every data-pack
migration fired the same way.

#### ⚠️ Forge 26 moved the convention tags to `c:` too — and it hid behind a GREEN boot gate

The 30-node boot gate returned `ALL DONE (rc=0)`, no crash-reports, every node `DONE`. A per-node
`/ERROR]` audit under the **full four-pattern benign filter** — which the gate script was not doing —
then showed `26.1.2-forge  err=42  nonbenign=36`, against `nonbenign=0` on all 29 others:

- **25 × `Couldn't load tag`** — `alexsmobs:underminer_ores` missing `#forge:ores`, plus `#forge:seeds`,
  `#forge:sand`, `#forge:crops/carrot`, `#forge:is_sandy`, `#forge:is_snowy`, `#forge:is_swamp` …
  cascading through `alexsmobs:am_spawns` into **every `*_spawns` tag**, i.e. most of the mod's spawning.
- **11 × `Couldn't parse data file`** — `Missing tag: 'forge:rods/wooden' in 'minecraft:item'`, killing
  the dimensional_carver, echolocator, enderiophage_rocket, flying_fish_boots, maraca, moose_headgear,
  rainbow_glass, shark_tooth_arrow, stink_ray, straddleboard and tendon_whip recipes.

**Forge 64 followed NeoForge into the `c:` namespace** — `net/minecraftforge/common/Tags.java` is almost
entirely `cTag(...)` now (only a handful of genuinely Forge-specific entries such as
`enderman_place_on_blacklist` and `needs_wood_tool` are still `forgeTag(...)`), with the **same names and
the same renames** NeoForge made at 1.20.5: `sands`, `strings`, `glass_blocks`, `gravels`,
`is_dense_vegetation/overworld`, `is_tree/coniferous`, and `dyes/green` via `DyeColor#getTag`. Every tag
name this mod uses was cross-checked against that file before the fix was written.

The fix is the **tag half only** of the NeoForge pass, and it is a separate function for exactly that
reason — `forge:loot_table_id`, `data/forge/loot_modifiers/global_loot_modifiers.json` and
`data/<ns>/forge/<registry>` are **all still read under `forge:`** on Forge 26, so it cannot just call
`migrateNeoForge`:

- **`DataPackMigration.migrateConventionTags`** relocates `data/forge/tags` → `data/c/tags` and rewrites
  every `forge:<path>` reference to `c:<renamed path>`, with `loot_table_id` explicitly exempted. Hooked
  from `ModPlatformPlugin.configureProcessResources` on `ctx.loader is Loader.Forge && eval(mc, ">=26")`.
- **`SpawnBiomeData.conventionTag`**'s gate widens to
  `//? if (neoforge && >=1.20.5) || (forge && >=26)` — the Java half, for the ~15 `forge:is_*` biome
  tags `DefaultBiomes` names as **plain strings** (in the shipped defaults *and* in the user's saved
  config), which no data-pack pass can reach.

  > ⚠️ **CORRECTION (2026-07-30): this widening SHIPPED BROKEN and silently killed most mob spawning
  > on every NeoForge node from MC 1.20.5 to 1.21.11.** It was originally written **without
  > parentheses**, and Stonecutter has no operator precedence (see [`stonecutter.md`](stonecutter.md)), so
  > `neoforge && >=1.20.5 || forge && >=26` parsed as `((neoforge && >=1.20.5) || forge) && >=26` —
  > i.e. it demanded MC ≥ 26 on *both* loaders. The previously-correct `neoforge && >=1.20.5` case was
  > destroyed by the edit that added the Forge one. Confirmed by reading the generated sources:
  > `conventionTag` was block-commented on `1.20.6-neoforge`, `1.21.1-neoforge` and
  > `1.21.11-neoforge`.
  >
  > Consequence: the ~15 plain-string `forge:is_*` biome tags were never normalised to `c:`, so on
  > NeoForge 1.20.5+ they matched nothing and the spawn entries using them never fired. **The failure
  > is completely silent** — an unknown biome tag named as a config string logs nothing at all — which
  > is exactly why eleven nodes carried it through four green five-step gates. Fixed by
  > parenthesizing; verified `active` in the generated sources for `1.20.6-neoforge`,
  > `1.21.1-neoforge`, `1.21.11-neoforge`, `26.1.2-forge`, `26.2-forge` and `26.2-neoforge`, and
  > correctly inactive on `1.20.4-forge`.
  >
  > **No gate can validate this fix** — there is no log line either way. Confirming restored spawning
  > needs an in-game check or a targeted probe, not a green `bootgate.sh`.

Verified in the rebuilt jar (`data/c/tags/item/…` present, `global_loot_modifiers.json` retained,
`recipe/maraca.json` now `"S": "#c:rods/wooden"`, and the only residual `forge:` strings are the five
`loot_table_id` conditions) and then at runtime: `err=42 nonbenign=36` → **`err=6 nonbenign=0`**.

> **The process lesson is bigger than the bug.** A data-pack fault is **logged, not thrown** — the server
> still reaches `Done (` and still exits 0 — and `bootgate.sh` only failed a node on a bad verdict or a
> crash-report. It *printed* the offending lines, but through a filter of just `grep -v RuntimeDistCleaner`,
> so from `1.21.7-neoforge` upward its output is a wall of benign `NeoForgeDevDistCleaner` /
> `OnlyInWarningsHandler` noise and 36 real errors read as more of the same. `bootgate.sh` now filters on
> all four benign patterns and **`rc=1`s on any surviving line**. Had this shipped, the 26 Forge jar would
> have had almost no mob spawning and eleven dead recipes, with nothing in the build to say so.

#### ⚠️ Forge 64 HARD-THROWS on `@OnlyIn` in a mod class — on either dist

The first fault the 30-node client gate found. `26.1.2-forge` died during mod construction; the crash
report's headline (`MixinTransformerError` / `ClassMetadataNotFoundException` on a vendored Citadel
event class) is a **downstream cascade** — the real first failure is at log line 157:

```
Failed to create mod instance. ModID: alexsmobs, class com.github.alexthe666.alexsmobs.AlexsMobs
java.lang.BootstrapMethodError: java.lang.UnsupportedOperationException: Mod class
  com/github/alexthe666/alexsmobs/ClientProxy is annotated with @OnlyIn, this is no longer supported
  as it slowed down startup times
  at …AlexsMobs.<clinit>(AlexsMobs.java:87)          ← the PROXY supplier-method-reference
  at …fml.loading.RuntimeDistCleaner.processClassWithFlags(RuntimeDistCleaner.java:81)
```

**This is NOT a wrong-dist check** — disassemble `RuntimeDistCleaner` (in `fmlloader-26.1.2-64.0.12.jar`;
the `net/minecraftforge/fml/loading/**` package is **absent** from the sources jar in `/tmp/fg2612src/`,
so the loader jar is the only oracle) and the shape is:

```java
var annotations = unpack(classNode.visibleAnnotations);
boolean isModClass = !isMinecraftClass(classNode.name);   // net/minecraft/, com/mojang/, net/minecraftforge/
if (remove(annotations, DIST)) { LOGGER.error("Attempted to load class {} for invalid dist {}"); throw …; }
if (isModClass && !annotations.isEmpty()) throw new UnsupportedOperationException(" … is annotated with @OnlyIn …");
```

i.e. it first strips any `@OnlyIn` naming the *other* dist (that is the familiar, benign "invalid dist"
error, and it only survives for **vanilla** classes) and then refuses **any** `@OnlyIn` that remains on a
non-vanilla class. So a client class annotated `@OnlyIn(Dist.CLIENT)` dies **on the client**. The identical
throw repeats for fields and for methods. NeoForge 26 only logs (`OnlyInWarningsHandler`).

The member half was already handled — see `!mc26-onlyin-member` in `stonecutter.gradle.kts` — and its
comment claimed class-level `@OnlyIn` "must be KEPT". That was wrong for Forge. The class-level half is now
**`!fg26-onlyin-class`** (Forge-only, `>=26`), commenting out all **56** class-level sites. Gated to Forge
deliberately: NeoForge 26 still honours class-level `@OnlyIn` in its dev dist cleaner and that node was
already gate-green, so there was nothing to buy by churning it.

The two rules can never collide because the tree spells **every class-level site `@OnlyIn(Dist.CLIENT)` and
every member-level site `@OnlyIn(value = Dist.CLIENT)`** — a distinction that exists purely so a textual
rule can tell them apart (a line gate cannot: some sit inside Stonecutter blocks, and blocks never nest).
**Keep it that way** when adding an `@OnlyIn` anywhere in this tree. Verify with:

```bash
grep -rho "@OnlyIn([^)]*)" --include="*.java" src/main/java | sort | uniq -c   # expect exactly 2 spellings
```

Dropping the annotation costs nothing on Forge 26 — it cannot block a mod class there, it throws instead —
and the re-run boot gate confirms nothing server-side ever touches the 56 declassified classes.

#### ⚠️ From MC 26.1, `new ItemStack(item)` in a RENDERER CONSTRUCTOR is a hard client crash

The second fault, found by the very next client-gate run once the `@OnlyIn` one was fixed:

```
IllegalArgumentException: Failed to create model for alexsmobs:cockroach
  at EntityRenderers.createEntityRenderers ← EntityRenderDispatcher.onResourceManagerReload
Caused by: java.lang.NullPointerException: Components not bound yet
  at net.minecraft.core.Holder$Reference.components(Holder.java:284)
  at net.minecraft.world.item.ItemStack.<init>(ItemStack.java:249)
  at …client.render.layer.LayerCockroachMaracas.<init>(LayerCockroachMaracas.java:33)
```

`ItemStack`'s constructor now reads the item holder's **data components**, and entity renderers are built
during the client's **first resource reload**, before those are bound. `LayerCockroachMaracas` was the only
site in the tree that built an `ItemStack` at renderer/layer construction time (every other `new ItemStack(`
in `client/**` is inside a `render(...)` body, which runs far later); it is now lazy — a `maracas()`
accessor that builds it on first render. Loader- and version-neutral, so it is **not** gated.

Note vanilla's error message names the **entity**, not the layer or the item — `Failed to create model for
alexsmobs:cockroach` is thrown by `EntityRenderers`, which wraps *anything* a renderer constructor throws.
Read past it to the `Caused by:` chain.

Grep to keep this from coming back:

```bash
grep -rn "new ItemStack(" --include="*.java" src/main/java/com/github/alexthe666/alexsmobs/client
```

Every hit must be inside a render/tick body, never a constructor or a field initialiser.

#### Cost: 426 errors on NeoForge (168 files), 28 on Forge — and the 28 were the expensive ones

The vanilla half is wide but mechanical, and lands almost entirely in `stonecutter.gradle.kts`'s
`>=26` `replacements` group. The Forge half is small in line count and large in design, because
**Forge 64 is a second EventBus-7-scale platform break** for a Forge-authored mod.

**Forge 64 (MC 26) — every break this mod hit, with the verified successor:**

| Break | Fix |
|---|---|
| `net.minecraftforge.fml.DistExecutor` **deleted** (`FMLEnvironment.dist` survives as a public field) | a `forge && >=26` arm in `AlexsMobs` + `citadel/Citadel` using the `Supplier`-method-reference indirection already documented for NeoForge 1.21 (Milestone 4) |
| the whole **sealed `EntityRenderersEvent` hierarchy** and **`RegisterParticleProvidersEvent`** are **off the mod bus** — each has a static `BUS` instead | `EntityRenderersEvent.RegisterLayerDefinitions.BUS.addListener(…)` in `AlexsMobs`' ctor, `EntityRenderersEvent.AddLayers.BUS` / `RegisterParticleProvidersEvent.BUS` in `ClientProxy.init` |
| **`AddGuiOverlayLayersEvent` RESTORED** (a `record … implements SelfDestructing, RecordEvent`, again with a static `BUS`) — Forge has had no HUD-layer API since 1.21 | the farseer static overlay moves off `client/GuiMixin` back onto a registered layer; `GuiMixin` is now gated `forge && >=1.21 && <26` |
| …but its **`addAbove` argument order DIFFERS** from the 1.20.5-era one: `addAbove(Identifier newLayer, Identifier otherLayer, ForgeLayer layer)`, not `(otherLayer, newLayer, layer)` | separate `forge && >=26` arm of `onRegisterGuiLayers`; the callback is 26's extract phase, `(GuiGraphicsExtractor, DeltaTracker)` |
| `RenderNameTagEvent` is `MutableEvent implements Cancellable`; the ctor **loses `displayName`** (it seeds content from `state.nameTag`) and `getResult()`/`setResult()` are gone | new first arm of `AMRenderEventCompat.nameTagContent` — `setContent(displayName)` then read `BUS.post(event)`'s boolean; there is **no force-allow any more**, a listener can only veto. The handler in `ClientEvents` becomes boolean-returning |
| `PlayerInteractEvent.EntityInteract` **deleted** | `!fg26-entityinteract` → `EntityInteractSpecific`, which `Player#interactOn` now fires at the same hook point and before `Entity#interact` (bytecode-verified in the patched `interactOn`) |
| `Tags.Items.SHEARS` → `TOOLS_SHEAR`; `isAddedToWorld`/`onAddedToWorld` → `…ToLevel` | `!fg26-shears` / `!fg26-addedtolevel-*` — a repeat of the NeoForge-1.21 renames, three MC lines later |
| `IGlobalLootModifier` has **no priority concept**, where NeoForge 26.1 made `priority()` abstract | the four loot modifiers' `priority()` override is gated `neoforge && >=26` — a loader gate, not a bare `>=26` |
| `Minecraft#getBlockModelResolver()` is a **NeoForge patch**; the field is private on Forge | `AMRenderCompat.renderSingleBlock` builds `new BlockModelResolver(Minecraft.getInstance().getModelManager())` per call — the class is a stateless public-ctor wrapper, so one code path serves both loaders |

`Level#random` also became `protected` (it was public final), and Forge 64 followed NeoForge 20.6 in
dropping the extensible-enum `PathType.getDanger()` — which was null for every vanilla path type
anyway, so `AbstractPathJob`'s guard is unconditionally `true` there.

**The vanilla half — the `>=26` `replacements` group and its 26 source gates.** Bulk first:

- **The GUI went extract/submit all the way up.** `GuiGraphics` → `GuiGraphicsExtractor` (same
  package); `Screen#render` → `extractRenderState`, `renderBackground` → `extractBackground`,
  `AbstractContainerScreen#renderLabels` → `extractLabels`, `AbstractButton#renderWidget` →
  `extractContents` (that hook has now been renamed in three consecutive versions:
  `renderWidget` ≤1.21.10 → `renderContents` 1.21.11 → `extractContents` 26.1, same parameter list
  each time). The draw methods took the extract vocabulary too: `drawString`→`text`,
  `renderItem`→`item`, `renderTooltip`→`setTooltipForNextFrame`,
  `submitEntityRenderState`→`entity`. Every call-site rule is anchored on the receiver name
  `guiGraphics`, because a bare `.renderItem(` would also hit `AMRenderCompat`'s own.
- **`ChunkPos` became a record** — field reads become accessor calls, `asLong`→`pack`, and the
  `BlockPos` constructor is the static `containing`. 11 anchored rules.
- **Renames with a real semantic**: `Level#getDayTime` → `getOverworldClockTime`;
  `isRaining`/`isThundering` moved from `LevelData` onto `Level`; `setWeatherParameters` moved from
  `ServerLevel` up to `MinecraftServer`; `DimensionDataStorage` → `SavedDataStorage`;
  "light colour" is "light coords" everywhere (`getLightColor` → `getLightCoords`);
  `LightTexture.pack` → `LightCoordsUtil.pack`; `ItemRenderer.getFoilBuffer` →
  `ItemFeatureRenderer.getFoilBuffer`; `BlockRenderDispatcher` → `BlockModelResolver`.
- **Signature changes needing a helper or a decl rule**: `Entity#interact` and `Player#interactOn`
  both gained a hit-location `Vec3`; `Player#displayClientMessage` split into
  `sendSystemMessage`/`sendOverlayMessage` (19 sites, through `AMCompat`);
  `ItemParticleOption` carries an `ItemStackTemplate`, not an `ItemStack`; `ServerBossEvent` wants an
  explicit id; `HumanoidModel#setAllVisible` is gone; `FollowBoatGoal` generalised to
  `FollowPlayerRiddenEntityGoal(this, AbstractBoat.class)`; cat/cow sounds moved behind per-variant
  sound sets, and `SoundEvents.CHICKEN_STEP` became a `Holder`.
- **`PathType`'s two fire constants** were renamed to say what they mean: `DANGER_FIRE` →
  `FIRE_IN_NEIGHBOR`, `DAMAGE_FIRE` → `FIRE`.
- **Deletions needing a source gate**: `net.minecraft.world.ContainerListener` is gone (the surviving
  `world.inventory.ContainerListener` is the *menu* listener, a different contract) and
  `SimpleContainer` lost `addListener` with it — `EntityCatfish` drops the interface, its
  `containerChanged` body being empty; `BlockBehaviour.Properties#hasPostProcess` became
  `postProcess`; `LiquidBlock.STABLE_SHAPE` is gone in favour of `getLiquidCollisionShape()`;
  `BlockRenderDispatcher#renderBreakingTexture` is gone, so `RenderUnderminer`'s destroy animation
  becomes a submit node exactly as `LevelRenderer#submitBlockDestroyAnimation` does it.

#### ⚠️ On MC 26 NO reload listener can decode `ItemStack.CODEC` — use `ItemStackTemplate`

Found by the boot gate: `26.1.2-neoforge` logged 4 × `Couldn't parse capsid recipe … does not have
components yet` and silently shipped **zero** capsid recipes. Compile-clean, server still reaches
`Done (`, and the *Forge* node hid it behind the mixin fault above.

26 made item components datapack-driven. `ItemStack.MAP_CODEC` now reads its id through
**`Item.CODEC_WITH_BOUND_COMPONENTS`**, which hard-errors until components are bound — and binding
happens in `ReloadableServerResources#updateComponentsAndStaticRegistryTags`, called **after the whole
reload instance completes**, i.e. after *every* listener's `prepare()` **and** `apply()`. So there is
no phase of a reload in which an `ItemStack` can be decoded. Moving the decode from `prepare()` to
`apply()` — the fix that works for the 1.21.2 unbound-tag trap — is **structurally incapable** of
working here; don't try it.

Vanilla's own recipes moved to **`net.minecraft.world.item.ItemStackTemplate`** for exactly this
reason: a `record (Holder<Item>, int count, DataComponentPatch)` decoded with plain `Item.CODEC`, with
`create()` materialising the stack later. `CapsidRecipeManager`'s `CODEC` therefore has a `>=26` arm
using `ItemStackTemplate.CODEC.fieldOf("result")`, and `CapsidRecipe` keeps the template and builds the
`ItemStack` lazily on the first `getResult()`. Everything else on that path — the constructor, `apply`,
the unbound-tag caveat — is shared with the `>=1.21.2` arms unchanged.

> The codec is spelled **twice** (`//? if >=26 { … } elif >=1.21.2 { … }`) rather than gated in place:
> the whole field already lives inside the `>=1.21.2` block and **Stonecutter blocks never nest**. Keep
> the two arms in sync.

#### Accepted regressions on ≥26

Same class as the 1.21.4 in-hand-model losses and the 1.21.9 custom particles — cosmetic or
peripheral, gated out with the reason recorded at the site:

| Lost on ≥26 | Why there is no port |
|---|---|
| **Lava Vision**'s clear-lava rendering (the potion's fog half still works) | `LiquidBlockRenderer` became `FluidRenderer` **and** each `SectionCompiler` constructs its own — there is no instance on the dispatcher to subclass and swap in. `LavaVisionFluidRenderer` is excluded from the compile |
| the **vanilla-block-model half of the vendored Tabula loader** (`TabulaModelBlock`, `VanillaTabulaModel`, `BakedTabulaModel`) | `@Deprecated(since = "2.6.2")` upstream and entirely unreachable here; its `BlockElement`/`ItemTransform(s)`/`UnbakedModel` dependencies are all gone or moved. Excluded from the compile, call sites gated `<26` |
| the mod's **villager / wandering-trader trades** | 26.1 made trades registry entries (`ResourceKey<VillagerTrade>`), datapack-driven: `VillagerTrades.ItemListing` is gone and so are NeoForge's `VillagerTradesEvent` / `WandererTradesEvent`. Restoring them means authoring datapack trades — a real feature port, not a gate |

> **The rainbow glass tint used to be on this list and is NOT any more** — it was restored on ≥26
> before this milestone closed. 26.1 replaced the lambda `BlockColor` with a `List<BlockTintSource>`,
> whose *abstract* method `int color(BlockState)` is position-free (it is what inventory/item
> rendering calls) and whose *default* `colorInWorld(BlockState, BlockAndTintGetter, BlockPos)` is
> the world-space one. A position-aware source therefore **cannot be a lambda** — `ClientProxy`
> declares an anonymous class returning `-1` ("no tint") from `color` and the existing
> `RainbowUtil.calculateGlassColor(pos)` from `colorInWorld`.
>
> The two loaders diverge in the **event name only**, and not the way you would guess: Forge 26 kept
> `RegisterColorHandlersEvent.Block` while **NeoForge renamed the nested class to
> `…​.BlockTintSources`** — so `onBlockColors` is a three-arm chain (`forge && >=26`,
> `neoforge && >=26`, else). Both expose the same `register(List<BlockTintSource>, Block...)`.
> On Forge the event is also **off the mod bus** (it is a `record … implements RecordEvent`, *not*
> `IModBusEvent`), so it is subscribed via its static `RegisterColorHandlersEvent.Block.BUS` in
> `ClientProxy.init` alongside `EntityRenderersEvent.AddLayers` and `RegisterParticleProvidersEvent`.

`mixin/client/ItemBlockRenderTypesMixin` is also excluded on ≥26 (`ItemBlockRenderTypes` is gone —
fluid render layers come off `FluidStateModelSet` now). Unlike the four source exclusions it **is** a
mixin, so it is pruned back out of `alexsmobs.mixins.json` as well — the build prints
`Pruned 1 MC-26-absent mixins`, and the 26 nodes declare 13 mixins where 1.21.2–1.21.11 declare 15.

### ✅ Milestone 14 — `26.2` (Forge & NeoForge)

Code complete 2026-07-28. **32 active nodes.** `mod.version` still `1.0.8` — nothing from Milestones
9–14 has been published. Pins: Forge **65.1.0**, NeoForge **26.2.0.35-beta**. Both nodes use the
no-remap Forge buildscript / MDG exactly as 26.1.2 does; nothing about the harness changed.

| Step | Result |
|---|---|
| 32-node `:build` (`MOD_IS_RELEASE=true`) | ✅ `BUILD SUCCESSFUL in 4m 31s` |
| `verify_mixins.py` | ✅ `jars=32 problems=0` |
| `verify_mixin_targets.py` | ✅ `jars=32 selectors=291 problems=0` |
| `SOAK=45 scripts/bootgate.sh` × 32 | ✅ all `DONE`, `rc=0`, no crash-reports, `nonbenign=0` — **but only after the `EntityPredicate` fix below** |
| `scripts/clientgate.sh` × 32 | ✅ all 32 reach `Sound engine started`, `rc=0`, no crash-reports |

Also checked, as every wave should: the shipped `versionRange` is the exact three-component
`[26.2.0]` on both new jars; `verify_assets.py` is `asset literals=394 missing=0`; and each new jar's
`data/`+`assets/` counts match its 26.1.2 counterpart exactly (forge `686/3464`, neoforge
`683/3464`), so every data-pack migration fired identically. The class-list diff against 26.1.2 is
exactly the seven expected 26.2-only vendored classes.

**The descriptor-level `sigdiff.py` was run FIRST this time** (Milestone 12's lesson) and it is what
made the wave predictable. 259 methods changed signature across vanilla 26.1.2 → 26.2, but only
three sit on classes this mod extends: `LivingEntity#{causeExtraKnockback,knockback,blockUsingItem,
blockedByItem}`, `Player#{causeExtraKnockback,blockUsingItem}` and `MoveControl`'s constructor
genericising `Mob` → `T`. The mod overrides none of them and every knockback call already goes
through `AMCompat.knockback` — so there are **no silently-dead overrides** this wave.

#### Three vanilla classes were DELETED, and all three are vendored rather than ported

This is the shape of 26.2 for this mod: not a signature sweep, but vanilla removing types that ~130
hand-written renderers and a dozen mobs are spelled in terms of. Each is vendored behind a
fully-qualified-name replacement rule, which is why the diff is small.

| Deleted | Vendored as | Why vendoring is the right call |
|---|---|---|
| `net.minecraft.client.renderer.MultiBufferSource` | `client/render/compat/MultiBufferSource` | 109 imports plus inline uses across ~130 renderers / ~37 layers / ~130 models. `AMSubmitBuffers` has stood between those bodies and `SubmitNodeCollector` since 1.21.9 — all that was missing on 26.2 was the *type* they name. Nothing outside the mod ever sees one |
| `net.minecraft.util.Tuple` | `misc/Tuple` | Nothing replaced it (vanilla's callers moved to records / `com.mojang.datafixers.util.Pair`). Four files want it — the banana-slug spread queue and the vendored Citadel raycoms path jobs |
| `net.minecraft.world.entity.animal.FlyingAnimal` | `misc/FlyingAnimal` | The mod queries it directly (`TameableAIRide` asks whether its mount flies), so the interface still earns its keep — but see the behaviour note below |
| `com.mojang.blaze3d.vertex.VertexMultiConsumer` | `client/render/compat/AMVertexMultiConsumer` | Vendors vanilla's deleted `.Double`, forwarding the eight abstract `VertexConsumer` methods to two delegates. Keeps the enchantment glint working (`RenderToucan`'s golden toucan, the tusklin gear/armour layers) now that `ItemFeatureRenderer#getFoilBuffer` is gone |

> ⚠️ **Vendoring `FlyingAnimal` restores the TYPE but NOT the BEHAVIOUR.** Vanilla read that
> interface in exactly two places in `LivingEntity` — the air-friction branch of `travel()` and the
> limb-animation call — and 26.2 replaced both with `protected boolean omnidirectionalAirMover()` on
> `Entity` (vanilla's own overriders are `Parrot`, `Bee`, `SulfurCube`). So implementing the vendored
> interface buys nothing on its own. **All 13 mobs that implement it** — `EntityBlobfish`,
> `EntityCatfish`, `EntityCosmaw`, `EntityDevilsHolePupfish`, `EntityEndergrade`, `EntityEnderiophage`,
> `EntityFly`, `EntityFlutter`, `EntityFlyingFish`, `EntityMurmurHead`, `EntitySoulVulture`,
> `EntitySpectre`, `EntitySunbird` — carry a gated `omnidirectionalAirMover()` override so they keep
> their flight friction and wing-flap animation. This is precisely the "compile-clean behaviour
> deletion" class of bug; it was found by reading vanilla's diff, not by the compiler.

#### Forge 65 deleted `IForgeShearable` — and it is a *class declaration* change

`net.minecraftforge.common.IForgeShearable` is gone in Forge 65 (NeoForge keeps `IShearable`), so
`EntityCockroach`, `EntityAlligatorSnappingTurtle`, `EntityBison` and `EntityMungus` each need three
gate insertions: a gated **class-declaration pair** dropping the interface on `forge && >=26.2`, plus
`//? if neoforge || <26.2` over the `@Override` on `isShearable` and on `onSheared`. The methods stay
— the mod calls them itself — only the `@Override` and the `implements` go. Note the gate spelling:
`neoforge || <26.2`, i.e. **a loader gate, not a version gate**, the same shape as 26.1's
`priority()` split.

#### `collectParts` is the one place the two loaders genuinely diverge

`RenderUnderminer`'s destroy animation (rebuilt in Milestone 13 to submit its own block-destroy node)
broke again because 26.2's vanilla `BlockStateModel` declares **only** the context-free
`collectParts(RandomSource, List<BlockStateModelPart>)` — and the two loaders added *different*
context-aware overloads on top of it:

- **NeoForge** extends the interface: `collectParts(BlockAndTintGetter, BlockPos, BlockState, RandomSource, List)`.
- **Forge**'s `IForgeBlockStateModel`: `collectParts(RandomSource, List, ModelData)`, with the model
  data pulled from `level.getModelDataManager().getAtOrEmpty(pos)` (the manager is `@Nullable`).

So `collectParts262` is a private two-arm helper, split out of `renderBreaking` because **blocks never
nest**. ⚠️ **`BlockAndTintGetter` moved client-side in 26.2** (`net.minecraft.client.renderer.block`,
`@OnlyIn(Dist.CLIENT)`) and `Level` no longer implements it — `ClientLevel` does, so the NeoForge arm
casts. That cast is safe here and only here: this is render code, so the level is always a client one.

#### A latent `ClassCastException` on every `>=1.20.5` node, found by a 26.2 compile error

`ClientEvents.onGetStarBrightness` and `onFogDensity` both did
`(EffectPowerDown) instance.getEffect()`. From **1.20.5** `MobEffectInstance#getEffect()` returns a
`Holder<MobEffect>`, and that cast **compiles** — `Holder` is an interface and `EffectPowerDown` is not
final, so javac cannot prove it impossible — then throws at runtime. It only became a compile error on
26.2, where the holder type finally made the cast provably bad. Both sites now go through the
`AMCompat.rawEffect(instance)` helper that already existed for exactly this, with the reason written
at each site. **This is a real bug fix for 26 nodes, not a port change.**

#### ⚠️ 26.2 made `EntityPredicate` a DISPATCHED MAP — 42 advancements silently vanished

The one thing the boot gate caught this wave, and it is the same shape as Milestone 13's Forge-26
`c:` tag fault: compile-clean, the server still reaches `Done (` and still exits 0, and the mod just
quietly loses a feature. `26.2-forge` and `26.2-neoforge` each logged **42 × `Couldn't parse data
file`** plus one `Couldn't load advancements`:

```
Couldn't parse data file 'alexsmobs:alexsmobs/alligator_snapping_turtle' from
  'alexsmobs:advancement/alexsmobs/alligator_snapping_turtle.json':
  DataResult.Error['Unknown registry key in ResourceKey[minecraft:root /
  minecraft:entity_sub_predicate_type]: minecraft:type; … missed input:
  {"type":"alexsmobs:alligator_snapping_turtle"} …']
```

`EntityPredicate` stopped being a flat record. It is now

```java
Codec.dispatchedMap(BuiltInRegistries.ENTITY_SUB_PREDICATE_TYPE.byNameCodec(), c -> c)
```

i.e. **every key of the JSON object must be a registered sub-predicate id**, and the old flat
`"type"` field is not one — its successor is **`entity_type`**. `type_specific` is gone too: each
variant is its own registry entry, so `{"type_specific": {"type": "player", …}}` flattens to
`{"type_specific/player": {…}}` with the inner `type` discriminator dropped.

Fixed at build time — data-pack JSON is not preprocessed by Stonecutter, so this is
**`DataPackMigration.migrateEntityPredicatesTo262`**, hooked from
`ModPlatformPlugin.configureProcessResources` under `>=26.2`. The build now prints
`Rewrote entity predicates in 42 files to the 26.2 dispatched map` — an exact match for the 42
failures.

Three things about that pass worth not re-deriving:

- **It is a pure KEY rewrite, and that was verified, not assumed.** Every codec registered in
  `EntitySubPredicates.bootstrap` is an `xmap` over exactly the codec the corresponding old flat
  field used, so no *value* shape changes. Only `type` → `entity_type` and the `type_specific`
  flattening are real edits; `distance`, `location`, `effects`, `nbt`, `flags`, `equipment`,
  `vehicle`, `passenger`, `targeted_entity`, `team`, `slots`, `components`, `predicates`,
  `stepping_on`, `movement`, `movement_affected_by`, `periodic_tick` all keep their names.
- **It walks an ALLOWLIST of host fields, not a shape heuristic** — `entityPredicateFields` (`entity`,
  `player`, `child`, `attacker`, `projectile`, …) inside a criterion's `conditions`, plus the
  `predicate` of a `minecraft:entity_properties` loot condition. A structural guess would corrupt
  sibling predicate types that happen to share key names: `minecraft:effects_changed` has an
  `effects` field that is a `MobEffectsPredicate`, and `ItemPredicate` has `components`/`predicates`.
  Nested `vehicle`/`passenger`/`targeted_entity` values, and `type_specific/player`'s `looking_at`,
  recurse. The pass is idempotent.
- **Only ADVANCEMENTS broke, which is why this was not obvious.** All 13 loot-table
  `entity_properties` conditions in this mod use `{"flags": {…}}`, and `flags` *is* a valid dispatch
  key in 26.2 — so those files were already correct and stayed byte-identical.

> Reminder that bit here again: **changing migration logic requires clearing
> `versions/<node>/build/resources` first** (`mv` it away — `rm` is sandbox-blocked), or
> `processResources` reuses its own stale output and the jar keeps the old JSON.

#### The rest — the `>=26.2` `replacements` group (30 rules) and 15 source gates

Same house rule as `>=26`: **replacements do not chain**, so every rule here is keyed on the **1.20.1**
spelling and emits the **final 26.2** spelling, doing every hop itself. `stonecutter.gradle.kts` gained
a second stand-down flag `val mc262` (line 48) alongside `mc26`; four earlier rules are wrapped in
`if (!mc262)` so they cannot half-claim an offset a `>=26.2` rule needs whole —
`!mc2104-itemtag-flowers`, `!mc2111-pkg-critereon`, `!mc2111-cam-mainpos`, `!mc26-lightcoords`.

- **`advancements.critereon` split in two** — `advancements.triggers` and `advancements.predicates`
  (26.1 had already renamed `critereon`→`criterion`; 26.2 split it). Five rules, one of them a
  wildcard-import workaround of the kind Milestone 12 documents.
- **`EntityType.X` → `EntityTypes.X`** — **eight individually-spelled rules**, one per constant this
  mod names (`DROWNED`, `ENDER_DRAGON`, `HOGLIN`, `PLAYER`, `SHULKER`, `SQUID`, `WANDERING_TRADER`,
  `WARDEN`). ⚠️ A bare `EntityType.` rule is **not** an option — it would also hit the 122
  `EntityType.Builder` uses, which did not move.
- **Dye-colour collapse**: the 16 variants of each family became one `ColorCollection` holder, so
  `Items.WHITE_CARPET` and friends are gone. `EntityElephant.DYE_COLOR_ITEM_MAP` builds the identical
  map by iterating `DyeColor.values()` and calling `Items.CARPET.pick(color)`.
- **`Blocks.POWDER_SNOW` is no longer nameable in an `immuneTo(...)` list** → replaced with a
  `TagKey<Block>`, backed by a new one-entry datapack tag
  `data/alexsmobs/tags/blocks/powder_snow_immune_to.json`.
- **`emissiveRendering`**'s three-arg `StatePredicate` became a `Predicate<BlockState>` (4 rules).
- Smaller, each anchored: `Bucketable` moved package; `Minecraft…renderBuffers().bufferSource()` →
  `compat.MultiBufferSource.noop()`; `getMainCamera()` → `mainCamera()` and `getPosition()` →
  `position()`; `LevelRenderer.getLightColor(` → `net.minecraft.util.LightCoordsUtil.getLightCoords(`;
  `setScreen(` → `setScreenAndShow(`; `Gui.getMobEffectSprite(` → `Hud.getMobEffectSprite(`;
  `ItemTags.FLOWERS` → `BlockItemTags.SMALL_FLOWERS.item()`.

Source gates worth knowing about, beyond the ones above:

| Site | Change |
|---|---|
| `ClientProxy.updateBiomeVisuals` | section invalidation moved off `LevelRenderer` onto `ClientLevel` (→ the new `LevelExtractor`), and `setSectionRangeDirty` takes **section** coordinates where `setBlocksDirty` took block ones. Upstream's argument list passes `x` where `z` belongs; that is reproduced verbatim so the node refreshes the same volume as every other |
| `ClientEvents.updateAllChunks` | `ViewArea#sections` is private and now a `RotatingSectionStorage`; nothing public marks every section dirty. Its only caller is the Lava Vision renderer swap, already gated out on `>=26` — so the body is simply empty there |
| `RenderSeal.renderNameTag` | 26.2 gave vanilla a `NAME_TAG_DISTANCE` attribute and NeoForge deleted both its `NAMETAG_DISTANCE` attribute and the `ClientHooks` reader. The `>=26.2` arm does vanilla's own comparison (`LivingEntityRenderer#extractNameTags`) |
| `AMRenderCompat.armorFoilBuffer` / `.drawTextInBatch` (new) | `ItemFeatureRenderer#getFoilBuffer` and `Font#drawInBatch` are gone; the first goes through `AMVertexMultiConsumer` + `RenderType.entityGlint()`, the second through `AMSubmitBuffers` + `OrderedSubmitNodeCollector#submitText` |
| `EntityStraddler`'s navigator | `GroundPathNavigation#hasValidPathType` deleted — path-type validity is the node evaluator's job now. Vanilla's `Strider`, which this navigator is copied from, dropped the identical override in the same version, so following it is faithful |
| `AMCompat.knockback` | gained a `>=26.2` arm passing `entity.damageSources().generic(), 0.0F` |

**26.2 is the CEILING — there is no next version to port to.** Checked both mavens 2026-07-28:
Forge's newest MC is `26.2` (`65.1.0`, exactly this repo's pin) and NeoForge's is `26.2`
(`26.2.0.36-beta`, one patch above the pin). Forge also published `26.1`/`26.1.1`, which are the same
API era as the `26.1.2` node already here and are skipped on the same cost/benefit grounds as 1.20.5.
So with Milestone 14 the **Forge + NeoForge line is finished**: every MC version the user asked for,
1.20.1 → 26.2, on both loaders, 32 nodes. Porting forward from here means waiting for a new MC.

**Next:** **Fabric** — deferred, but wanted on every version; see [`fabric.md`](fabric.md). It needs a user
decision first: `AlexsMobsFP` was declared Fabric-only on 2026-07-26 and publishes to the **same**
Modrinth slug, so which repo owns the Fabric jars has to be settled before any code is written.

Run multi-node Gradle in **one** invocation (`./gradlew :a:build :b:build --continue`) — back-to-back
calls collide on the daemon and Stonecutter's active-version state. Boot gate: Forge `runServer` consumes
a piped `stop` and self-terminates; NeoForge does **not** (run detached, watch for `Done (`, then kill).

### 🚧 Milestone 15 — Fabric, 17 nodes (`1.20.1` → `26.2`)

Started 2026-07-30, code complete on the compile axis 2026-07-31. **The tree is now 49 nodes: 32
Forge/NeoForge + 17 Fabric** (`1.20.1`, `1.20.4`, `1.20.6`, `1.21` → `1.21.11`, `26.1.2`, `26.2` — Fabric
has `1.21.2`, which Forge does not, and `1.20.1`, which NeoForge does not). `mod.version` is still
`1.0.8` and **nothing Fabric has been published.**

⚠️ **This milestone is not "Fabric works".** All 17 nodes compile, build and boot, but the compile axis
alone left the mod **registering everything and reacting to almost nothing** — `ServerEvents.java`,
`ClientEvents.java` and `ClientLayerRegistry.java` were all excluded from the Fabric compile by
`ModPlatformPlugin`. Waves 1–2 below close the server half; `ClientEvents` and `ClientLayerRegistry` are
still excluded, so **Fabric has no client event behaviour at all**. Both gates only prove a node boots,
so a fully green table below says nothing about behaviour. The full list of what is missing, and the
24-row table of silent Fabric-only behaviour divergences, is in [`fabric.md`](fabric.md) — read it
before shipping a Fabric jar.

Two user decisions bound the shape before any code was written: **this repo owns the Fabric jars** (the
sibling `AlexsMobsFP`, which publishes to the same Modrinth slug, becomes a reference/archive), and the
**first Fabric target is `26.2`**, back-filling downward. That ordering is what the pre-start estimate in
[`fabric.md`](fabric.md) recommended, and it held: nothing had to be re-derived going down, only re-gated.

#### The seam — a relocated compat namespace, not a Forge shim

The one architectural decision, and the reason this milestone is a wave and not a rewrite. `AlexsMobsFP`
shimmed the real `net.minecraftforge.**` package names; that is a dead end here because Forge and NeoForge
are first-class in this tree and the shim collides with the genuine classes. Instead there are **25 files
under `alexsmobs/fabric/**` keeping the same simple names**, reached by **24 one-line `!fab-*` replacement
rules** keyed on the fully-qualified Forge name, plus **229 `fabric` source gates**. `ModPlatformPlugin`
excludes that package from the 32 non-Fabric nodes. ⚠️ Which means a **shared** file naming one of those
classes must spell it fully qualified inside a `fabric` gate, never as an import — see
[`fabric.md`](fabric.md).

#### Two halves, and the cost was where the estimate said it would be

The estimate called it "~all one-time, not per-version". Measured:

- **`26.2` alone** was the milestone — the compat namespace, the access-widener generator, the
  `fabricNoRemap` buildscript, the biome-modification bridge, `clientInit` wiring, and the registry-flush
  reorder that Fabric's non-deferred `DeferredRegister` forced.
- **The other 16 nodes were compile-fix passes.** Three things cost anything at all: `1.21.11` is the
  first **obfuscated** node (classic loom + a `named`-namespace widener, hence `build.fabric.gradle.kts`
  alongside `build.fabricnr.gradle.kts`), the `1.21.x` band re-gated a handful of Fabric-API boundaries
  downward, and everything **below `1.20.5`** hit the pattern below.

#### ⚠️ Below 1.20.5, most of what breaks is a Forge *patch to a vanilla class*

The single most transferable lesson of the back-fill, and it is invisible to an import survey: the source
names a **vanilla** type and calls a method that only exists on Forge's **patched** jar. `compileJava` is
the only thing that finds these. Found this way: `Entity#getStepHeight`, `EnchantmentCategory#create`,
`SpawnPlacements$Type#create`, `BlockStateBase#isValidSpawn(…, SpawnPlacements$Type, …)`,
`FoodProperties$Builder#effect(Supplier, float)`, `RecordItem`'s constructor, and
`Enchantment#isAllowedOnBooks` / `#canApplyAtEnchantingTable`.

Probe with `javap` against
`~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged/<mc>-*/*.jar` — that jar is
**unpatched**, and it is the only reliable answer to "is this vanilla or is this Forge?".

Two of them are worth calling out because **an access widener cannot fix them**: `EnchantmentCategory` and
`SpawnPlacements$Type` are plain **enums** below 1.20.5 and Forge patches an extensible-enum `create(...)`
onto both. The problem is the enum, not access. Both were resolved with a placeholder constant plus an
override answering the question the placeholder would have answered — and where the caller reads the
*field* rather than an overridable method (vanilla's enchanting table does exactly that), the honest move
was to switch the feature off rather than let `VANISHABLE` leak straddle enchantments onto swords. Both
are logged as deliberate behaviour losses in [`fabric.md`](fabric.md)'s divergence table.

One rule-direction note that will re-confuse the next reader: `!mc205-stepheight` rewrites
`.getStepHeight()` → `.maxUpStep()` on `>=1.20.5` because the root source is **Forge 1.20.1**, where
`getStepHeight` is Forge's name. Fabric below 1.20.5 needs the **same** rewrite for the **opposite**
reason, so it got its own `!fab-stepheight` group — widening the existing condition would also have fired
on Fabric nodes `>=1.20.5`, which must keep the vanilla name.

#### The access widener is generated, and that is not optional

An AT entry naming an absent member is a silent no-op; **an AW entry naming an absent member fails the
build**. Across 17 nodes the header namespace changes (`official` on 26.x no-remap, `named` below) and
eight entries move descriptor or package. So there is **one predicated template** at
`accesswidener/alexsmobs.accesswidener` — deliberately **outside `src/`**, because Stonecutter *does*
preprocess `.accesswidener` and a template kept there dies at `stonecutterPrepare` — expanded per node by
`build-logic/AccessWidener.kt`, and pre-flighted by `scripts/aw_check.py`. Full reasoning, including why
native Stonecutter gating could not have worked, in [`fabric.md`](fabric.md).

#### The gate — five faults, and two of them were in the gate

The gate ran over all **49** nodes (see [`gates.md`](gates.md)) — every step but the client one, which is
still owed 41 nodes. What it caught is the useful part, because the compile axis had been green for a day
by then:

1. **A verifier that had been quietly checking nothing.** `verify_mixin_targets.py` came back
   `jars=49 selectors=303 problems=140` — every failure on one of the 15 **obfuscated** Fabric nodes,
   naming `class_1309`-style targets. Those are *intermediary*; the script knew only SRG and Mojmap and
   was resolving them against a Mojmap jar. Not a mod defect — but note what the loud failure was
   *hiding*: with the target class unresolved the script `continue`s, so **none** of those jars'
   selectors were ever checked. Adding the third mapping branch took it to
   `jars=49 selectors=431 problems=0`; coverage went **303 → 431**. Written up in [`mixins.md`](mixins.md).
2. **A double registration, from a version gate that had never met a second caller.**
   `1.20.1-fabric` died at boot with `Duplicate registration for type alexsmobs:grizzly_bear`:
   `registerSpawnPlacements()` ran once from the `<1.20.5` line inside `initializeAttributes` and once
   from the Fabric entrypoint. Both calls are valid Java, so only a boot gate finds it. Re-gated to
   `<1.20.5 && !fabric`; the general rule is in [`fabric.md`](fabric.md).
3. **A `>=26` gate that was a guess about history.** `1.21.10-fabric` died at
   `AMCompat.<clinit>` with *"Tried to register tracked data handler … use `FabricTrackedDataRegistry`
   instead"*. Fabric's guard on `EntityDataSerializers.registerSerializer` does **not** arrive at 26.1 as
   the `26.2` node's note claimed — it arrives with object-builder `21.1.2`, i.e. the **`1.21.5`**
   fabric-api, the same MC line as NeoForge's. What changes at 26.1 is only the replacement class's
   *name*. `26.2` was the first Fabric node, so "it is called `FabricEntityDataRegistry`" was true of
   everything in the tree at the time and nothing on the compile axis could contradict it. Two more arms
   (`fabric && >=1.21.9`, `fabric && >=1.21.5`) fixed it; the general form is in [`fabric.md`](fabric.md).
4. **Seven convention tags nobody defines on Fabric** — the one that would actually have shipped broken,
   and porting rule 8 almost verbatim. `#c:` resolves on Forge/NeoForge because the *loader* ships the
   tags; on Fabric they come from an optional Fabric API module that grew from 156 tags to 500+ over a
   year, **non-monotonically across the pins**. Six nodes referenced tags their pinned fabric-api does not
   define, and the failure is *logged, not thrown*: `c:sands` alone empties `alexsmobs:am_spawns` and the
   fifteen `*_spawns` tags built on it, while the server still prints `Done (` and exits 0. Most of the
   mod stops spawning and nothing says so. Fixed by defining all seven in
   `DataPackMigration.backfillFabricConventionTags`, on **every** Fabric node — a shipped jar meets
   whatever fabric-api the player installed, not the pin.

5. **A gate that covered one line and meant two** — the first thing the *client* gate found, and the only
   fault of the five that no server-side step could ever reach. `1.20.1-fabric` crashed in
   `ClientProxy.clientInit` on `Minecraft.getInstance().renderBuffers()` being `null`, because the
   `//? if !fabric` above `initRainbowBuffers()` was the **single-line** form and had gated only the
   `MinecraftForge.EVENT_BUS.register` line above it. Fabric's `ClientModInitializer` runs from *inside*
   `Minecraft.<init>`, not after it like `FMLClientSetupEvent`, so the field is not assigned yet. It
   needed two coincidences to reach a gate: the mis-scoped gate, and a method body that is itself
   `//? if <1.20.2 {` — so on 16 of 17 Fabric nodes the leaked call is a no-op and **only `1.20.1-fabric`
   could crash**. Gated the call properly; scoping rule now in [`stonecutter.md`](stonecutter.md), timing
   rule in [`fabric.md`](fabric.md).

> The boot gate had only ever seen two of those six nodes when it found this, which is why the fix came
> with **`scripts/verify_convention_tags.py`** — it answers all 17 Fabric nodes off the build output in
> about a second (`nodes=17 problems=0`), and is now the fourth static step of the gate.

Faults 1 and 3 have the same shape and it is the shape to expect when a milestone adds a **loader** rather
than a version: the tree grew a second answer to a question the harness only knew one answer to, and the
harness kept confidently giving the old one. A mapping namespace in one case, a version boundary in the
other. **Whatever a verifier or a `//?` gate silently assumes is universal is exactly what the new loader
invalidates** — go and enumerate it rather than waiting for the first red node.

Where the gate stands:

| step | result |
|---|---|
| `./gradlew <49 × :build> --continue` (`MOD_IS_RELEASE=true`) | `BUILD SUCCESSFUL`, 49 jars |
| `verify_mixins.py` | `jars=71 problems=0` (71 = 49 + SNAPSHOT/dev duplicates) |
| `verify_mixin_targets.py` | `jars=49 selectors=637 problems=0` (`431` before Wave 2 added `mixin/fabric/**`) |
| `verify_convention_tags.py` | `nodes=17 problems=0` |
| `verify_assets.py` | `asset literals=394 missing=0` |
| `bootgate.sh` × 49, `SOAK=45` | **49 DONE**, no crash reports, no non-benign log lines |
| `clientgate_par.sh`, `SOAK=12–15`, `JOBS=4–5` | ✅ **49 of 49 READY** — every node reaches `Sound engine started`, `rc=0`, no crash reports |

The client row was the last thing Milestone 15 was missing, and it is now closed: all 49 nodes were
run on 2026-08-01 after the user lifted the no-client constraint. This matters because every other
row above is headless — a client-only fault (a missing renderer, a model layer, anything reached
from `Minecraft.getInstance()`) is invisible to all six of them. It also **re-gated fault 5**:
`1.20.1-fabric`, the one node that had FAILED, is now READY, so that fix is verified rather than
merely built. This is the first time any client mixin in the tree has been exercised on a Fabric node.

**Read the exit code, not the verdict count** (rule 7). The aggregate log showed 44 READY against
only 35 `Sound engine started` lines, which looks like 9 silent nodes — it is not. `report()` pipes
its grep through `head -30`, so on a node that logs many `/ERROR]` lines the boot marker is
truncated out of the *summary* while still being present in the per-node log. Verified by grepping
`build/clientgate/cgate-<node>.log` directly for all 49: **0 without a boot marker**.

Nineteen nodes log `/ERROR]` lines without failing. All three classes are benign, recorded here so
they are not re-investigated every sweep:

| class | nodes | why it is noise |
|---|---|---|
| `No data fixer registered for <entity>` (116×) | Fabric 1.20.1 → 1.21 | Vanilla logs this for any entity with no DFU schema, i.e. every modded entity. Already in `clientgate.sh`'s `grep -vE` exclusion. |
| `OnlyInWarningsHandler` (53–133×) | NeoForge ≥1.21.7 | NeoForge advisory that `@OnlyIn` no longer strips members at runtime. What it guards is server-side dist safety, and `bootgate.sh` is green on all 49. Worth re-reading if a dedicated server ever `NoClassDefFoundError`s on a client type. |
| `Couldn't connect to realms` | Forge, various | Network, unrelated to the mod. |

**Parallelism — and where it stops paying.** `scripts/clientgate_par.sh` runs the nodes
concurrently; sequential would have been over an hour. The active node is run **alone, first** — its
sources live in the root `src/`, which is rule 1's real scope, while every non-active node is
projected into its own `versions/<node>/src` and shares no mutable state.

Both settings were then measured on the Wave 3b-6 re-gate rather than guessed:

| `JOBS` | RAM/node | peak RAM | peak VRAM | throughput |
|---|---|---|---|---|
| 4–5 | ~2.2 GiB | 27 GiB | 1.95 GiB | ~24 s/node |
| 12 | ~1.75 GiB | 36 GiB | 2.22 GiB | ~17.9 s/node (49 nodes in **891 s**) |

**3× the jobs bought ~1.35× the throughput**, so scaling is sharply sublinear and `JOBS=12` is the
sensible default. Neither obvious resource explains the ceiling: the GPU is nowhere near it (2.2 of
10.2 GiB — these are title-screen boots), and RAM per node *falls* as `JOBS` rises because Gradle
daemons get shared instead of duplicated, which puts a hypothetical `JOBS=16` at ~43 of 62 GiB. What
actually serialises is the **configuration phase**, behind the file locks on the shared `~/.gradle`
caches; only the forked client JVMs run genuinely in parallel. Past 12, expect single digits.

Two harness fixes came out of the runs. `bootgate.sh` now seeds `run/server.properties` the way it already
seeded `run/eula.txt` — a never-booted node has none, so vanilla logs `Failed to load properties from file`
at ERROR and *then* writes it, which made every node's first run red for a reason unrelated to the mod.
And `No data fixer registered for <type>` became the fifth benign pattern: it is vanilla's dev-only DFU
schema check, once per modded type (**116 lines on `1.20.1-fabric`**), invisible on Forge/NeoForge only
because they patch the method. It clears the bar for widening that filter — this mod ships no datafixers on
*any* loader, so the Forge nodes are in the identical state and merely quieter. `server.properties`
deliberately did **not** clear it, and was fixed at the cause instead.

#### Waves 1–2 — the server half of the behaviour axis (2026-07-31)

The compile axis being green is where the milestone above stops. The behaviour axis is five waves, and
the first two are done: **`ServerEvents` now compiles on Fabric and every hook in it that can fire,
fires.** The mechanics are all in [`fabric.md`](fabric.md) — this is what the waves cost and what they
taught.

- **Wave 1 — the file compiles.** `ServerEvents` names Forge event *types* on every line, so it needed
  the compat namespace extended with stub events under `fabric/forge/**` before the exclude could come
  off. Nothing fired it yet; that is a deliberate split, because "does it compile" and "does it run" are
  different failure surfaces and mixing them would have hidden both.
- **Wave 2 Batch A — six hooks that Fabric API already has.** Level tick, login, and the four
  interaction callbacks. Two things to keep: **both** tick phases must be registered (Forge posts
  `LevelTickEvent` in START and END and the mod never filtered), and an uncancelled interaction must
  return `PASS` even when something set a result.
- **Wave 2 Batch B — eleven hooks with no callback at all**, fired from `mixin/fabric/**` on the exact
  vanilla method Forge patches, plus a twelfth mixin (`ServerPlayer#restoreFrom`) that is not an event
  hook but closes the "second Animal Dictionary after death" divergence. The mixin is only a *where*;
  `ServerEvents` stays the only *what*, so neither file has to know the other's version arms. Doing
  rule 10 first — javap'ing all eleven descriptors across all 17 nodes before writing a line — is what
  produced the useful answer: **seven of the eleven are byte-identical from 1.20.1 to 26.2** and need
  no arms, which no amount of source-reading tells you and which `defaultRequire: 1` turns into a crash
  when guessed wrong.

Three of the eleven are **deliberately not faithful** to Forge, each for a reason that is cheaper than
the arm it would have cost: `checkDespawn` is cancelled whole rather than inside the non-persistent
branch (Forge's shape needs `shouldDespawnInPeaceful`, which vanilla **deleted at 1.21.9**),
`FinalizeSpawn` fires from `Mob#finalizeSpawn` rather than the call sites Forge patched, and
`EntityEvent.Size` gets the *new* dimensions. All three are in the divergence table with their bounded
consequence, which is the standard for closing one of these rather than fixing it.

##### ⚠️ The gate hole: a green verifier that was reading last session's jars

The one thing here that will bite again. `verify_mixins.py` and `verify_mixin_targets.py` glob
`alexsmobs-*-<loader>+<mc>.jar`; a plain `./gradlew :node:build` emits
`alexsmobs-1.0.8-fabric+1.21.11-**SNAPSHOT**.jar`, which **does not match**. So both scripts fell
through to release-named jars left over from an earlier session and reported `problems=0` about code
that no longer existed. It surfaced only because `--node 1.21.11-fabric -v` said `ok (7 selectors)`
when the four new mixins contribute ~10 on their own — i.e. the **count**, not the verdict, is what
caught it.

Two rules out of that, both now in [`gates.md`](gates.md): **build with `MOD_IS_RELEASE=true` before
any verifier step**, and **read the selector count, not just `problems=0`** — a verifier that resolves
nothing is indistinguishable from a verifier that resolves everything if you only look at the verdict.
It is the same failure the milestone above hit as fault 1 (the intermediary-mapping branch), arriving
from a different direction: coverage silently going to zero looks exactly like success.

After the fix, over all 49 nodes rebuilt as release: `verify_mixins.py` `jars=71 problems=0`,
`verify_mixin_targets.py` `jars=49 selectors=637 problems=0` (**431 → 637**, the whole increase being
`mixin/fabric/**` × 17), `verify_convention_tags.py` `nodes=17 problems=0`, and `bootgate.sh` green on
all 17 Fabric nodes. ⚠️ The client gate is **still owed 41 nodes** — everything above is headless, and
Wave 3 (`ClientEvents`, 814 lines) is precisely the code no headless step can reach.

#### Wave 3a — the client half starts: `ClientEvents` compiles and partly fires (2026-07-31)

The last excluded behaviour file came in. Same architecture as Waves 1–2 — stub the event types, do
not fork the file — and it landed cheaper than either of them, for one reason worth carrying forward:

> **On a Fabric node every `forge && …` and `neoforge && …` gate is false, so the `else` arm wins on
> all 17 nodes regardless of MC version — and the else arms carry the 1.20.1-Forge API shape.** Make
> the Fabric stub able to answer the 1.20.1 getters everywhere and the shared file needs almost no new
> arms; the version differences move down into what the (Wave 3b) mixin passes to the stub's
> constructor.

So `fabric/forge/client/event/RenderLivingEvent` carries the **union** of every era's payload — a
shape no real Forge version ever had, because Forge dropped the entity and partial tick at 1.21.2 and
the buffers and packed light at 1.21.9. Total cost in the shared 814-line file: **two** new arms, both
`fabric && >=1.21.2` and both byte-identical to the `forge && >=1.21.2` arm directly above them.

Six stub types (`ViewportEvent`, `RenderLivingEvent`, `RenderHandEvent`, `RenderNameTagEvent`,
`MinecraftForge`, `TickEvent.ClientTickEvent`), two `!fab-fe-*` replacement rules, and
`fabric/client/FabricClientEvents` calling one shared `ClientEvents` instance from
`ClientProxy#clientInit`. Six of the sixteen handlers are live with no new mixin at all — four ride
the mod's **own** `AMEventBus`, which `mixin/client/**` already posts on every loader, so all that was
ever missing was a subscriber.

Three things it cost:

- ⚠️ **A version-keyed replacement rule fires on Fabric too.** `!mc2102-renderlivingevent` is keyed on
  `>=1.21.2` alone, so it rewrote the *stub's* generic arity and `26.2-fabric` failed with `wrong
  number of type arguments; required 2` in a file nobody had edited. Gating the stub's own type
  parameters fixed it. **Check the loader-neutral rule groups before adding a Fabric type that shares
  a name with a Forge one** — this is the third distinct way a replacement rule has surprised this
  tree, after `!mc205-persistednbt` and `!fab-stepheight`.
- Three `GameRenderer` post-chain methods that Forge/NeoForge ship widened have **no accesstransformer
  entry to copy** — they are Forge *patches*, so the AT never mentioned them. Three `<1.21.2`
  accesswidener entries, pre-flighted with `aw_check.py` (an AW naming an absent member is a hard
  error, unlike an AT, which no-ops silently).
- Two handlers and one helper were gated **`!fabric` rather than stubbed**, which is the cheaper move
  whenever the alternative is a stub *plus* an access widener: it avoided `RenderLevelStageEvent`,
  `RenderGuiOverlayEvent`/`VanillaGuiOverlay` and a `LiquidBlockRenderer` widening outright. A
  wildcard `import net.minecraftforge.client.event.*;` needs no particular member to exist.

Verified headless over the whole tree: 49 × `compileJava` **rc=0**, 49 × `build`
(`MOD_IS_RELEASE=true`) **rc=0**, `verify_mixins.py` `jars=71 problems=0`, `verify_mixin_targets.py`
`jars=49 selectors=637 problems=0` (unchanged — 3a adds no mixins, which is itself the check that the
stub-not-mixin route was taken), `verify_convention_tags.py` `nodes=17 problems=0`, `bootgate.sh` over
all 17 Fabric nodes.

⚠️ **The gate row that matters for this wave is the one that has not run.** Every step above is
headless and `ClientEvents` is client-only code: a green boot gate proves *nothing* about it. Wave 3a
is verified to compile, link and not break the server; whether the goggles fog, the outline colours or
the rocky roll actually render correctly is unmeasured until the client gate covers the Fabric nodes.
**Wave 3b** — the six per-frame hooks that need their own mixins (entity render Pre/Post, hand render,
the two fog hooks, camera setup, nameplates, and the farseer HUD overlay) — is not started; its scope
table is in [`fabric.md`](fabric.md).

#### Wave 3b-1 — entity render `Pre`/`Post` on Fabric (2026-07-31)

The first of Wave 3b's six, and the one that carries the most behaviour: rocky-chestplate roll,
clinging / debilitating-sting flip, ender-flu shake, vine lasso, wandering-trader model swap.
One new file, `mixin/fabric/client/FabricLivingEntityRendererMixin`, plus the two dispatchers
`FabricClientEvents.firePreRenderLiving`/`firePostRenderLiving` (written in 3a, unused until now) and
one word of build-logic. The mixin keeps Wave 2's split — it is only a *where* and a per-era argument
unpacker, so `ClientEvents` needed no new arm at all and stays on its 1.20.1-shaped `else` branches on
all seventeen nodes.

Three things cost real time and are worth not paying for twice; the durable form of each is in
[`fabric.md`](fabric.md#wave-3b--the-per-frame-hooks-one-of-six-done).

- **This is the first target in the tree that is genuinely overloaded**, by the compiler's bridge for
  `EntityRenderer`'s erased signature. The repo's "name-only selectors throughout" habit would have
  matched the bridge too, and since the bridge *calls* the real method, every hook would have fired
  twice per entity per frame — a doubled model and a doubled vine lasso, no crash, nothing any gate
  step can see. Hence full descriptors, and hence **four** arms rather than three: `CameraRenderState`
  changes package at 26, and `!mc26-pkg-camerastate` cannot follow it into a mixin selector because
  the rule keys on the dotted FQN and a selector is slashed. That is the general shape — **no
  replacement rule in this tree can reach inside a descriptor string**, so every dotted-name rule
  implies a hand-written descriptor arm on the mixin side.
- **`Post` had to be `TAIL`, not `RETURN`,** because the `cancellable` `Pre` injector inserts its own
  `return` at the top of the method for a later `RETURN` scan to find — which would post `Post` on
  exactly the path Forge does not, i.e. the rocky-roll branch that already reposts one by hand. Settled
  by dumping the bytecode on all 17 nodes and confirming a single `return` opcode, so `TAIL` is
  unambiguous. Worth remembering as a general hazard: **HEAD-cancel plus RETURN is an ordering bug,
  not a style choice.**
- **`DataPackMigration.clientMixinPackages` needed `"fabric.client."` spelled out** — the prefixes are
  `startsWith`-matched against the path below `mixin`, so `"client."` does not match a nested package.
  Without it the entry stays in the common `mixins` array and a Fabric dedicated server, which has no
  dist cleaner, aborts at launch trying to apply a client mixin. This is the same failure the pass was
  written for in the first place, arriving through a new door. Per rule 9 the 49
  `build/resources` trees were `mv`-ed away before rebuilding, or `processResources` would have
  re-emitted the old config and hidden the whole thing.

Verified headless over the whole tree: 49 × `compileJava` **rc=0**, 49 × `build`
(`MOD_IS_RELEASE=true`) **rc=0**, `verify_mixins.py` `jars=71 problems=0`, `verify_mixin_targets.py`
**`jars=49 selectors=671 problems=0`** — the count rising by exactly 34 (17 Fabric nodes × 2 injectors)
is the check that every one of the four descriptor arms resolved against real bytecode rather than one
arm silently matching nothing — `verify_convention_tags.py` `nodes=17 problems=0`, `bootgate.sh` over
all 17 Fabric nodes, and a direct read of the built `alexsmobs.mixins.json` on 1.20.1-fabric,
26.2-fabric and 26.2-forge confirming the entry lands under `client` on Fabric and is absent on the
other two loaders.

⚠️ Same caveat as 3a, only sharper: **this is client-only render code and no client gate has ever run
on a Fabric node.** Everything above proves it links and resolves; whether the rocky chestplate
actually rolls is unmeasured.

#### Wave 3b-2 — the farseer static overlay on Fabric (2026-07-31)

The second of Wave 3b's six, and the only one that needed no mixin: `renderStaticOverlay` is a plain
static draw call, so it wanted a caller rather than an injection point. One new method,
`FabricClientEvents.registerFarseerStatic()`, wired from `register()`. Durable form in
[`fabric.md`](fabric.md#3b-2--the-farseer-static-overlay-done).

**It was found by disbelieving a comment.** The block above `ClientEvents#renderStaticOverlay` said
every loader reaches it "by its own route" and listed three Forge/NeoForge ones — written from the
Forge side, and read for two waves as covering Fabric. It did not: `ClientProxy`'s registration arms
are all `forge`/`neoforge` and `mixin/client/GuiMixin` is gated `forge && >=1.21 && <26`, so the
effect had never drawn on any of the seventeen Fabric nodes. **A comment that enumerates loader
routes is a claim to check against the gates, not a finding.**

The arms are Fabric API's, not Minecraft's, and were read off the pinned `fabric-api` jars rather
than inferred: `HudRenderCallback` takes a bare `float` below **1.21** and a `DeltaTracker` from
1.21; at **26** the callback is gone and `HudElementRegistry.addLast` replaces it. `HudElementRegistry`
exists from 1.21.6, but the deprecated callback still works at 1.21.11, so the middle arm covers the
whole `1.21`–`1.21.11` span — three arms instead of four for no behavioural difference.

`addLast` over `attachElementAfter(VanillaHudElements.MISC_OVERLAYS, …)` was a choice: Forge and
NeoForge insert the layer *under* the hotbar, `HudRenderCallback` cannot, and the 26 classfiles carry
no parameter names so `attachElementAfter`'s argument order is unverifiable from bytecode. Keeping all
17 Fabric nodes consistent with each other beat matching the other two loaders on one node, and the
whole visible difference is whether the hotbar shows through a full-screen tint.

**Both of this wave's real costs were Stonecutter, not Fabric, and both are now in
[`stonecutter.md`](stonecutter.md):** prose written between `//?} else {` and the arm's `/*` is
outside the comment and becomes live code on the nodes that select the arm; and the `>=26` arm must
not contain the token `!mc26-guigraphics` *produces*, because that rule is reversible and its
precondition is that root `src/` never spells it — hence the untyped lambda parameters. The compiler
says `illegal character: '—'` and points at an English sentence, which is unmistakable once seen and
names neither cause.

⚠️ **The third cost was a process one, now porting rule 11.** That broken arm was written *while
the 3b-1 boot gate was running*, so it landed only in the nodes still queued — `26.1.2-fabric` and
`26.2-fabric` failed and read exactly like a 3b-1 high-version regression. The gate was right about a
tree that had changed under it. **`src/**` and `build-logic/**` are frozen for the duration of a
gate.**

Re-verified as one chain after the fix, which is also the first time 3b-1 was boot-gated on
`26.1.2-fabric`/`26.2-fabric`: 49 × `build` (`MOD_IS_RELEASE=true`) **rc=0**, `verify_mixins.py`
`jars=71 problems=0`, `verify_mixin_targets.py` **`jars=49 selectors=671 problems=0`** — unchanged
from 3b-1, which is the expected result since 3b-2 adds no mixin — `verify_convention_tags.py`
`nodes=17 problems=0`, `bootgate.sh` over all 17 Fabric nodes.

Same standing caveat: **no client gate has ever run on a Fabric node**, so nobody has watched the
static draw.

#### Wave 3b-3 — `ViewportEvent.ComputeCameraAngles` on Fabric (2026-07-31)

The third of six. One mixin (`mixin/fabric/client/FabricCameraMixin`) and one dispatcher
(`FabricClientEvents.fireComputeCameraAngles`); Wave 3a's stub already had the right shape on all 17
nodes, so nothing else moved. Durable form in
[`fabric.md`](fabric.md#3b-3--viewporteventcomputecameraangles-done).

**The event feeds two handlers, and only one of them is named after it.** On Fabric `ClientEvents`
subscribes both `onCameraSetup` and `onRenderWorldLastEvent` to `ComputeCameraAngles` — the second
because Fabric, like Forge `>=1.21.3`, never had `RenderLevelStageEvent`, so `doWorldLastFrame()` was
already re-homed onto the camera hook in shared source. A dispatcher that fired only `onCameraSetup`
would have shipped the earthquake shake without the bald-eagle camera return or the lava-vision chunk
refresh, and every gate step would still have been green. **Before writing a dispatcher, grep the
shared source for every handler that takes the event type, not for the handler you came for.**

Rule 10 paid off twice on the injection point. The natural target, `GameRenderer#renderLevel`, moves
three times across the range (`#updateCamera` at 1.21.11, `#update` at 26); the callee `Camera#setup`
does not. A jar-wide scan of `net/minecraft/client/**` in each era showed `Camera#setup`/`#update`
has **exactly one caller in the whole client**, so injecting into the callee is once-per-frame *and*
immune to the caller being renamed again. Three arms instead of four, and the survey also pinned
`@At("TAIL")` as safe — one `return` in the body on 1.20.4 → 26.2.

**The verifier number is the whole proof here.** `verify_mixin_targets.py` went **671 → 688, +17,
exactly one per Fabric node**. A wrong descriptor arm compiles and boots; it only fails to *match*,
and its single symptom is a selector count that did not move. Predicting the delta before running the
gate turns that from something you might notice into something the gate answers.

Gate, one chain: 49 × `build` (`MOD_IS_RELEASE=true`) **rc=0**, `verify_mixins.py` `jars=71
problems=0`, `verify_mixin_targets.py` **`jars=49 selectors=688 problems=0`**,
`verify_convention_tags.py` `nodes=17 problems=0`, `bootgate.sh` **rc=0** with all 17 Fabric nodes
DONE.

Standing caveat unchanged: **no client gate has ever run on a Fabric node**, so nobody has watched the
camera shake.

#### Wave 3b-4 — `RenderHandEvent` on Fabric (2026-07-31)

Fourth of six. `mixin/fabric/client/FabricItemInHandRendererMixin` +
`FabricClientEvents.fireRenderHand`; Wave 3a's stub needed no change. Durable form in
[`fabric.md`](fabric.md#3b-4--renderhandevent-done).

**The survey moved the target, and the wave's own planning table had it wrong.** Both the table and
the earlier descriptor sweep named `ItemInHandRenderer#renderHandsWithItems`, because that is the
method whose *name* matches the event. It is the whole first-person pass; the event is per-hand — the
handler branches on `getHand()` and cancelling is supposed to drop one hand, not both. The per-hand
callee `renderArmWithItem` carries hand, stack, partial tick, pose stack, buffers and light as
parameters, and is precisely where Forge patches
(`ForgeHooksClient.renderSpecificFirstPersonHand` replaces the call and skips it on cancel). **A row
in a survey table is a starting point, not the answer; check what the handler actually reads off the
event before accepting the obvious target.**

⚠️ **The two arm boundaries are each invisible to one kind of check, in the same method.** 1.21.9
keeps `renderArmWithItem` and swaps `MultiBufferSource` for `SubmitNodeCollector`; 26.2 keeps that
descriptor and renames the method to `submitArmWithItem`. A name grep waves the first through and a
signature grep waves the second through — rule 10 twice over, three lines apart. Only a javap dump of
the class per era shows both.

From 1.21.9 the mixin wraps the collector in an `AMSubmitBuffers` before it reaches the stub, using
the no-camera constructor — the same one Forge's `handBuffers` picks, because a `RenderHandEvent`
carries no `CameraRenderState` on any loader. The mixin does not flush it; `ClientEvents` does, after
it has drawn the falconry bird.

Gate, one chain: 49 × `build` (`MOD_IS_RELEASE=true`) **rc=0**, `verify_mixins.py` `jars=71
problems=0`, `verify_mixin_targets.py` **`jars=49 selectors=705 problems=0`** — 688 + 17, predicted
before the run — `verify_convention_tags.py` `nodes=17 problems=0`, `bootgate.sh` **rc=0** with all 17
Fabric nodes DONE.



#### Wave 3b-5 — the fog pair on Fabric (2026-08-01)

`ViewportEvent.ComputeFogColor` (**3b-5a**) and `.RenderFog` (**3b-5b**), both through one new
`mixin/fabric/client/FabricFogRendererMixin` → two new dispatchers on `FabricClientEvents`. Together
they restore the lava-vision goggles on all 17 Fabric nodes. Mechanics, arm tables and the near/far
mapping are in [`fabric.md`](fabric.md); this is what the wave cost and what it taught.

- **The worst-fragmented row in the wave: three independent boundaries that do not line up.** The
  *class* moves (`client/renderer/FogRenderer` → `client/renderer/fog/FogRenderer`) at 1.21.6; the
  *methods* change shape at 1.21.2, 1.21.6, 1.21.11 and 26; and `computeFogColor` stops being
  **`static`** at 1.21.6. 1.21.2 and 1.21.11 end up sharing a descriptor while sharing nothing else.
  Because the class itself moves, **the `@Mixin` annotation had to be gated** — the only one in the
  tree that is. Five arms per hook.

- **A sixth failure mode for rule 10: staticness.** A method can keep its name *and* its descriptor
  and stop being static. That is invisible to a name grep, invisible to `sigdiff.py`, and **javac
  accepts a mismatched handler** — it throws only at mixin-apply time, on a client. Caught by dumping
  **access flags** (`survey_fog3.py`), not signatures. Add `ACC_STATIC` to what a target survey prints.

- **The gate said `rc=0` while a verifier said `rc=1`.** The wave's gate script ends in a `grep`, so
  its exit status describes the grep, not the run. The real verdict —
  `vmt rc=1 :: jars=49 selectors=739 problems=6` — was only in the per-step log. This is exactly what
  rule 7 is for, and it is the second time the marker/exit-code distinction has mattered here.

- **…and those 6 problems were the verifier's fault, not the mixin's.** javap omits the owner prefix
  on a **same-class** reference, so an `@At` target naming a member of the `@Mixin` class itself could
  never match the owner-qualified needle `verify_mixin_targets.py` builds. `FogRenderer`'s private
  `updateBuffer` is the **first self-call `@At` target in the tree** across 54 injections, which is why
  the bug had gone 15 milestones without being reachable. Fixed narrowly (owner-less form accepted
  **only** when the `@At` owner is the `@Mixin` target, anchored to javap's `Method `/`Field ` lead-in)
  and **negative-tested** before being believed — wrong owner, absent target and wrong descriptor all
  still fail. Selector count was **739 both before and after**: the fix reclassified six results
  without reducing coverage. Details in [`mixins.md`](mixins.md).

  > Worth keeping: the count rising exactly as predicted (722 → 739) proved the *selectors* resolved
  > and said nothing about the `@At` targets beneath them. Predicting the number is a good habit; it
  > is not a proof of correctness.

- **Core Mixin over MixinExtras, on purpose.** The two 1.21.6–1.21.11 arms use `@ModifyArgs` with a
  bare `Args`, not `@Local`/`@ModifyExpressionValue`, so no arm depends on a MixinExtras version being
  bundled by whatever Fabric Loader a player happens to have. Same reasoning as the rest of the tree:
  it declares **zero** MixinExtras usage.

Gate `bic5v6t23` + verifier re-run: 49 × `build` (`MOD_IS_RELEASE=true`) **rc=0**, `verify_mixins.py`
`jars=71 problems=0`, `verify_mixin_targets.py` **`jars=49 selectors=739 problems=0`** (722 + 17),
`verify_convention_tags.py` `nodes=17 problems=0`, `bootgate.sh` **rc=0** with all 17 Fabric nodes DONE.

#### Wave 3b-6 — `RenderNameTagEvent` on Fabric, and Wave 3 closes (2026-08-01)

The last of Wave 3b's six per-frame hooks. `FabricNameTagMixin` fires `ClientEvents#onRenderNameplate`,
which hides the player's own nameplate under the bald-eagle POV in singleplayer. Five arms; the full
target table and the force-ALLOW caveat are in [`fabric.md`](fabric.md).

- **The design said two parts; it was five.** The mixin is inert on its own — `ClientEvents`' veto
  line was gated `//? if forge || <1.20.6`, so on Fabric ≥1.20.6 neither it nor the NeoForge
  `TriState` arm was emitted and the guard body was empty. Widening it then failed the compile,
  because the `import net.minecraftforge.eventbus.api.Event` at the top of the *same file* carried
  the **identical** condition and was equally inert. **A gate that is dead in a given configuration
  hides every other gate that depends on it**; you only meet them one compile at a time. Both
  conditions now read the byte-identical string `forge || fabric || <1.20.6` and name each other in
  comments — they are ~500 lines apart.
- **A documented trap, hit anyway.** Prose between `//?} else {` and the arm's `/*` had its `//`
  markers stripped when Stonecutter selected the arm, landing in the `26.2-fabric` projection as raw
  text. This is the second time (after `registerFarseerStatic`); [`stonecutter.md`](stonecutter.md)
  now records it as a repeat offender, along with *why* — the ✗ and ✓ forms differ by four
  characters, and the neighbouring single-line-gate rule says to put prose *above* the `//?`, so the
  two rules point opposite ways depending on the gate shape.
- **Injecting into the callee, not Forge's call site**, is what lets one injection per node also
  cover `RenderTiger`/`RenderFarseer`/`RenderUnderminer` — the three renderers Wave 3b-1 provably
  could not reach. That row in the divergence table shrank rather than grew, which is the first time
  in this milestone that has happened.
- **`verify_mixin_targets` was predicted to go 739 → 756 and did.** With five era arms whose method
  *name* changes only twice across four boundaries, that count is the only cheap evidence every arm
  resolved rather than silently missing.

Gate `b3c0gqih1`: 49 × `build` (`MOD_IS_RELEASE=true`) **rc=0**, `verify_mixins.py`
`jars=71 problems=0`, `verify_mixin_targets.py` **`jars=49 selectors=756 problems=0`**,
`verify_convention_tags.py` `nodes=17 problems=0`, `verify_assets.py` `literals=394 missing=0`.
The gate script no longer ends in a `grep` — `w3b5b-gate.sh` did, which is how it reported `rc=0`
over `problems=6` last wave.

Then `clientgate_par.sh` over all 49 at `JOBS=12`: **`rc=0 elapsed=891s`, 49 verdicts, 0 non-READY,
0 crash reports, 0 nodes without a boot marker** — checked per node rather than off the aggregate
summary, for the `head -30` reason above. A client-only mixin is exactly the change a compile and a
static verifier cannot vouch for, so this run is what makes 3b-6 *verified* rather than merely built.
`bootgate.sh` was **not** re-run for this wave (3b-5b's covered it, rc=0 with all 17 Fabric nodes
DONE); the new mixin is client-dist only and `verify_mixins` is green, but a dist-partition error is
precisely what the server gate would catch, so run it before the next publish.

⚠️ **That owed `bootgate.sh` ran at the start of Wave 4 and was green** — `argc=17`, `rc=0`, 17/17
`DONE`. Debt cleared, but it should not have been carried: the reason to run it was the same one that
made it owed.

⚠️ **And `selectors=756` above is a number to distrust.** See Wave 4 — the verifier that produced it
was reading stale jars.

#### Wave 4 — multipart entities and `ICustomCollisions` on Fabric (2026-08-01)

Two mixins added, three files changed. The code was the small half of this wave.

**Scope shrank from three parts to two before anything was written.** The deferred list claimed Fabric
drops the extra spawn data the 21 `AMPlatform.getEntitySpawningPacket` callers send. Grepped instead of
recalled: **no class implements `IEntityWithComplexSpawn`, and neither `writeSpawnData` nor
`readSpawnData` exists anywhere in the tree.** All 21 callers override `getAddEntityPacket` purely to
get a correct add-entity packet, which is exactly what the `(neoforge || fabric)` arm builds — the arm
16 NeoForge nodes have shipped on. A documentation defect that had been reading as a known-broken
feature for two waves. Fixed the `AMPlatform` javadoc, kept a ⚠️ for if an entity ever gains real spawn
data.

**The port itself.** All three mixins come from `AlexsMobsFP`, which runs them in production:
`FabricLevelMultipartMixin` (`@Inject(RETURN)` on `Level#getEntities(Entity, AABB, Predicate)`,
appending nearby `IMultipartOwner` parts — that one query backs picking, `getEntityCollisions` and most
range lookups), `FabricMultiPlayerGameModeMixin` (cancels the vanilla attack on a part, reports it with
the *parent's* id), and the `collide` injection, which went into the **existing** `FabricEntityMixin`
rather than a new class. FP's `@Shadow public abstract getEntities()` was carried over verbatim
including its comment — without `@Shadow` Mixin treats it as an implicit overwrite and a coexisting
mod's access widener can promote the target to `public`, hard-crashing with `cannot reduce visibiliy of
PUBLIC target method`. FP paid for that on 2026-07-26.

**The one thing FP could not tell us.** FP is single-MC-version, so its `MessageHurtMultipart` change
was free. Here that file is shared source in all 49 nodes and its C2S path is **already live on Forge**
(`EntityCachalotPart`, `EntityGiantSquidPart` send real damage and a real damage type, and there the
part id resolves). So the new `serverPlayer.attack(parent)` branch is `//? if fabric`-gated and the 32
Forge/NeoForge nodes are byte-identical. Its condition is `holder == null` — already the handler's way
of spelling "not a damage relay" — deliberately **not** a `damage == 0 && damageType.isEmpty()` sniff,
which would be a sentinel encoded in a wire format aliasing a real zero-damage message.

⚠️ **The two `//? if !fabric` gates on `EntityTiger#collide` / `EntityRockyRoller#collide` stay.** The
apply plan's first draft said to delete them. Wrong, and it would have broken all 17 Fabric compiles:
vanilla's `collide` is private on Fabric, so the override cannot exist there regardless of the mixin.
Only the comments changed.

##### ⚠️ The verifier hole reopened — and the first fix is why

Predicted `selectors 756 → 807` before the run (3 selectors × 17 Fabric nodes). It came back **756**,
and the gap is what exposed the real fault: `verify_mixin_targets.py` globs
`alexsmobs-*-<loader>+<mc>.jar`, which **cannot match** a `-SNAPSHOT` dev build, so it had been
validating leftover *release*-named jars ~50 min stale — while printing `jars=49`.

**This is the second time.** Waves 1–2 diagnosed the identical hole and fixed it with a *habit* —
"build with `MOD_IS_RELEASE=true` before any verifier step" — patching `verify_mixins.py` but leaving
`verify_mixin_targets.py`'s glob broken and load-bearing on someone remembering. Wave 4 built without
the env var; nothing warned, because silence *is* the failure mode.

Two traps in the diagnosis, both of which impersonate a code bug:

- a hand `ls` of `build/libs/alexsmobs-*.jar` picks the stale jar too, so the new mixins looked absent
  from the jar **and** from `mixins.json` while sitting in `build/classes` — which reads exactly like a
  Fletching Table or source-set fault, and is not one;
- nothing distinguished "49 nodes verified" from "49 stale jars verified, N fresh nodes skipped".

Fixed in the script, not in a habit: glob is now `…+<mc>*.jar`, and a node with a `build/libs` but no
matching jar is **named and exits non-zero**. 309 stale jars moved out of the tree (`rm` is
sandbox-blocked). Re-run: **`nodes=49 jars=49 selectors=807 problems=0 skipped=0`** — the predicted
number exactly. The durable lesson is in [`mixins.md`](mixins.md): *a workaround that depends on
remembering is not a fix for a tooling bug.*

##### Gate

49 × `build` in ONE invocation, `argc=49 rc=0`, 0 errors, 0 FAILED. Then `verify_mixins` `jars=49
problems=0`, `verify_mixin_targets` **`selectors=807 problems=0 skipped=0`**, `verify_assets`
`literals=394 missing=0`, `verify_convention_tags` `nodes=17 problems=0`, `aw_check` `problems=0` on
15/17 (26.1.2 and 26.2 have no cached vanilla named jars — the widener was untouched this wave).
`bootgate.sh` over the 17 Fabric nodes: `rc=0`, 17/17 `DONE`, 0 mixin-apply errors — which is what
exercises the `Level` mixin and its `@Shadow` visibility trap. `clientgate_par.sh` at `JOBS=12` over
all 49 covers `FabricMultiPlayerGameModeMixin`, which is client-dist only and therefore invisible to
every headless step.

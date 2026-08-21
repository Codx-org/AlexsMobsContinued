# API era breaks, version by version

> Read when planning or starting a port to a new MC version.
>
> Part of the Alex's Mobs Continued porting notes.

## Why 1.20.2, 1.20.3 and 1.20.5 are skipped

The user asked for "every big version since 1.20.1". **Three of them drop out** — for
different reasons, so don't lump them together (re-checked against both mavens
2026-07-23):

- **1.20.5** — Forge published **no 1.20.5 build at all** (its metadata jumps 1.20.4 → 1.20.6).
  NeoForge `20.5.x` does exist, but 1.20.5 lived ~10 days before 1.20.6 replaced it, so a
  NeoForge-only node buys nothing the `1.20.6-neoforge` node doesn't already cover.
- **1.20.3** — Forge `49.0.1`/`49.0.2` exist, but their userdev depends on
  `net.neoforged:bootstrap-dev:2.0.0`, which has been **removed from the NeoForge maven**
  (the whole artifact 404s), so the Forge dev toolchain cannot be resolved.
- **1.20.2** — both loaders shipped (Forge `48.x`, NeoForge `20.2.x`); this one is a
  **cost/benefit skip** carried over from codxlib, where it and 1.20.3 were made Fabric-only.

So it is *not* "these versions don't exist". 1.20.2/1.20.3 are the same pre-DataComponents API
era as 1.20.4 — the porting work is already done, and adding them would be a pure toolchain
fight (Forge 48/49 userdev on Gradle 9 + arch-loom, MDG against a NeoForge 20.2/20.3 bundle).

### API era breaks to plan around

| From | Break |
|---|---|
| **1.20.2** | no node of ours sits *on* it, but everything above it does: `MobEffect#isDurationEffectTick` → `shouldApplyEffectTickThisTick` (base impl returns **`false`**), and **`Entity#getPassengersRidingOffset` deleted** in favour of `getPassengerRidingPosition` / the type's `EntityAttachments` — whose `PASSENGER` fallback is the **full** height where the old default was `0.75 × h`, so it moves riders on classes that never declared anything |
| **1.20.5** | **DataComponents** — NBT item data, food properties, armor materials all rewritten |
| **1.21.2** | **entity render-state rewrite** + a very wide vanilla rename/resignature sweep — **by far the biggest break in this plan**, see below. Also **deletes `Font`'s implicit-opacity guard**, so an `int` colour with no alpha byte now draws fully transparent instead of opaque |
| **1.21.4** | item **model definitions** (`assets/<ns>/items/<id>.json`), `builtin/entity` **and the whole ISTER mechanism** removed — an item drawn from its own NBT has nothing to fall back on; **`GuiGraphics#enableScissor` becomes pose-relative** — same signature, so it is invisible to the compiler and to `sigdiff.py` |
| **1.21.5** | `CompoundTag` getters return `Optional`; the **`RenderPipeline`** rewrite of `RenderType`; `SpawnerData` loses its weight; `TamableAnimal` owner becomes an `EntityReference`; `ArmorItem`/`SwordItem` deleted — a **mega-wave**, ~1,250 errors/node. Also **deletes `item/template_spawn_egg`** and both greyscale layer textures, and replaces `Entity#isControlledByLocalInstance` with a **`final`** `isLocalInstanceAuthoritative()` over two new feeders — **the default flips**, so a vehicle carrying a player becomes client-authoritative unless it opts out |
| **1.21.6** | `ValueInput` / `ValueOutput` replace `CompoundTag` save/load; GUI `pose()` becomes a `Matrix3x2fStack`; `Mob`'s "restriction" is renamed "home"; **Forge only** ships **EventBus 7** (a ground-up bus rewrite) |
| **1.21.7** | vanilla is a bugfix release — but **NeoForge only** drops `PacketDistributor.sendToServer` for the client-only `ClientPacketDistributor` |
| **1.21.8** | vanilla is a bugfix release — but **NeoForge only** rejects a mixin-added `EntityDataAccessor` on a vanilla entity (`CommonHooks#verifyEntityDataAccessorRegistration`); the Citadel data store becomes a data attachment there |
| **1.21.9** | a **mega-wave in three parts**, not one: (i) a wide mechanical vanilla sweep, (ii) a full **particle extract/submit rewrite**, (iii) the **`SubmitNodeCollector`** submission pipeline for renderers / layers / tile renderers. ~150 errors/node |
| **1.21.10** | the cheapest wave since 1.21.7 — **two** methods: `BlockBehaviour#entityInside` gained a trailing `boolean`, and **NeoForge only** added an `ItemStack` to `onDestroyedByPlayer` |
| **1.21.11** | `ResourceLocation` → `Identifier`, `RenderType` split into `RenderType`+`RenderTypes`, **37 package moves** — plus a tail of real breaks an import-level survey cannot see: `GameRules` retyped, `getTimeOfDay`/`getCurrentDifficultyAt`/`BiomeTags.SNOW_GOLEM_MELTS` replaced by **environment attributes**, `NeutralMob` anger becomes an absolute end time, `VertexConsumer.setColor(int)`/`setLineWidth` become abstract. 117 errors/node |
| **26.1** | the **GUI** goes extract/submit (`GuiGraphics` → `GuiGraphicsExtractor`, `Screen#render` → `extractRenderState`), `RenderType` factories move to `RenderTypes` **and two of them swap meaning**, block models are rewritten again — 426 errors on NeoForge. **Forge 64** is the expensive half of the wave for a Forge-authored mod: `DistExecutor` deleted, `EntityRenderersEvent`/`RegisterParticleProvidersEvent` taken off the mod bus, `RenderNameTagEvent` reshaped, `AddGuiOverlayLayersEvent` restored with a *different* argument order |
| **26.2** | not a signature sweep but a **deletion** wave: `MultiBufferSource`, `net.minecraft.util.Tuple`, `FlyingAnimal` and `VertexMultiConsumer` are all gone (all four vendored here), `EntityType.X` constants move to `EntityTypes`, the 16 dye-colour variants collapse into `ColorCollection` holders, `advancements.criterion` splits into `triggers`+`predicates`. **Forge 65** deletes `IForgeShearable`. `BlockAndTintGetter` moves client-side, and `BlockStateModel#collectParts` is the one place the two loaders genuinely diverge |

> ⚠️ **26.2 also changed where entity ids come from, and the compiler cannot see it.** Through 26.1.2
> `Entity`'s constructor took an id from a static `ENTITY_COUNTER`, so *every* entity had a unique
> non-zero id the moment it existed, on either side. 26.2 replaced that with
> `Level#getNextEntityId()` — real on `ServerLevel`, **`return 0` on the base `Level`, i.e. on the
> client** — because client ids now arrive from the server with the spawn packet, and made
> `Entity#getId()` **throw** `IllegalStateException: Tried to access entity ID before ID assignment`
> while the id is still 0. Nothing about that is a signature change, so an era diff, a `sigdiff.py`
> run and all 49 compiles stay green; it only shows up as a crash the first time client code reads the
> id of a client-only entity. **Multipart entities are exactly that** — a `PartEntity` never gets a
> spawn packet on any loader — which is how it reached players. See "Attacking a cachalot or giant
> squid crashes on 26.2" in [`bug-reports.md`](bug-reports.md) for the three call sites it hit and the
> fix. When auditing a future version bump, treat *any* entity the mod constructs client-side the same
> way.

> ⚠️ **A version can change what an unchanged signature *means*.** Four of these bit us as shipped bugs
> (all in [`bug-reports.md`](bug-reports.md)'s third pass): 1.21.4's `enableScissor` kept
> `(int,int,int,int)` but started multiplying the rectangle through the current GUI pose; 1.21.5
> deleted a vanilla **asset** every one of this mod's 89 spawn-egg models parents to; 1.21.4 deleted the
> **ISTER**, which is not an API this mod calls but a mechanism three of its items existed to use; and
> **1.21.2 deleted `Font`'s implicit-opacity guard**, which changed the meaning of an `int` the caller
> passes. Neither a compile, a descriptor diff, nor either gate can see that class of break — only
> opening the screen or looking at the item can. When a wave touches rendering coordinates, colours or
> resources, budget a *visual* check.

> ⚠️ **The overload trap: a *widened* vanilla parameter turns an override into dead code, silently.**
> Upstream Alex's Mobs almost never writes `@Override`, so when Mojang changes a signature the
> subclass method quietly becomes an unrelated overload — it compiles on all 49 nodes, no gate
> fails, and the behaviour just stops. `Block#fallOn`'s `float`→`double` at 1.21.5 cost the
> leafcutter anthill and both egg blocks (#61); `MobEffect#applyEffectTick`'s leading `ServerLevel`
> at 1.21.2 plus the `isDurationEffectTick`→`shouldApplyEffectTickThisTick` **rename** at 1.20.2
> together switched **all 18 potion effects** off on **46 of 49** nodes (#66) — and those two
> boundaries are a version apart, so pin each from bytecode rather than assuming one date covers
> both. **`scripts/verify_overrides.py`
> is the detector** — it diffs each compiled mod class's superclass chain against the cached mapped
> vanilla jar. Run it per node after `compileJava`, with `--baseline=1.20.1-forge` to subtract what
> upstream was already carrying. It is a REVIEW list, not a gate, and it is **blind to renames** (see
> its docstring), so pair it with `sigdiff.py` on any wave. Add it to the end of every future port
> wave alongside the two mixin verifiers.

> ⚠️ **1.21.2: an `int` colour without an alpha byte now draws nothing.** Through 1.21.1 `Font#drawInBatch`
> opened with `if ((color & 0xFC000000) == 0) color |= 0xFF000000;` — a caller passing `0x303030` got
> opaque grey. 1.21.2 deleted those two instructions (bytecode-verified: the constant pair is in
> 1.20.4 / 1.20.6 / 1.21.1's `Font` and absent from 1.21.2 on), so the same call now draws at alpha 0.
> Upstream Alex's Mobs, authored against 1.20.1, spells five colours that way — the animal dictionary's
> entire body text and titles, the transmutation table's title, its buttons' XP cost, and the seal's
> floating name. All five were invisible on all 35 nodes from 1.21.2 up. Writing the `FF` out is correct on
> *all* versions, so the fix needs no version gate. **When porting any code that hands a colour to
> vanilla, check the top byte** — `0x` colours copied from pre-1.21.2 source are all suspect.

> ⚠️ This table used to put the render-state rewrite at **1.21.5**. That is **wrong** — verified by
> `javap` against the 1.21.3 jar: `EntityModel<T extends EntityRenderState>` with `setupAnim(T)`,
> `LivingEntityRenderer<T, S extends LivingEntityRenderState, M extends EntityModel<? super S>>`,
> and `EntityRenderers.register(EntityType<? extends T>, EntityRendererProvider<T>)` are all already
> in place in **1.21.2**. 1.21.5 only refines it further.

**Measured cost of 1.21.2** (first compile of `1.21.2-neoforge` / `1.21.3-forge` / `1.21.3-neoforge`):
**~4,800 errors per node**, against 268 for the whole 1.20.6 NeoForge wave. Roughly half is the
render rewrite (~130 models + ~130 renderers + every render layer, all of them Citadel-derived), and
the other half is a wide but mechanical vanilla sweep. Bucketed, largest first:

| Count (1 node) | Change |
|---|---|
| ~500 | `MobSpawnType` → `EntitySpawnReason` |
| ~226 | `EntityRenderers.register` signature |
| ~204 | `LivingEntityRenderer` gained a third type parameter (render state) |
| ~180 | `setupAnim`/`renderToBuffer` overrides no longer match |
| ~166 | `InteractionResultHolder` deleted; `sidedSuccess`/`success`/`fail` fold into `InteractionResult` |
| ~138 each | `getEntity`, `addLayer`, `getModel` now take/return render states |
| ~110 | `EntityType.create` takes an `EntitySpawnReason` |
| ~102 | `Item`/`ItemStack` methods: `spawnAtLocation`, `Ingredient.of`, `getCraftingRemainingItem` |
| ~80 each | `isInvulnerableTo`, `doHurtTarget`, `dropEquipment`, `customServerAiStep` all take a `ServerLevel` |
| — | `Entity#hurt` is `public final void`; the override point is the abstract `hurtServer(ServerLevel, DamageSource, float)`, with `hurtClient(DamageSource)` and `hurtOrSimulate(…)` alongside. `Entity` also **loses** its public `isInvulnerableTo` (`LivingEntity` keeps one, taking a `ServerLevel`) |
| ~72 | `getMinBuildHeight`/`getMaxBuildHeight` → `getMinY`/`getMaxY` — **not** a blind rename: the old max was *exclusive*, `getMaxY()` is *inclusive*, so it is `getMaxY() + 1` |
| ~54 each | `boolean` → `TriState` on Forge hooks; `Ingredient.TagValue` gone |
| ~44 | `SoundEvent` → `Holder<SoundEvent>` |
| smaller | `UseAnim`→`ItemUseAnimation`, `DirectionProperty`→`EnumProperty<Direction>`, `FastColor`→`ARGB`, `registryOrThrow`→`lookupOrThrow`, `Predicate<LivingEntity>`→`TargetingConditions.Selector`, `WalkAnimationState.update`, `updateShape` (8 args now), `ArmorMaterial` moved |

Because render states persist unchanged through 1.21.11, **this migration is paid once and unlocks
every node above it** — but it is a wave in its own right, not a step inside one.


## The silently-dead override, measured (2026-08-06, shipped in `2.0.8`)

Rule 10 says to start a wave with the descriptor-level diff. This is what it costs when that is not
done for a *single* method, and it went unnoticed through nine releases.

**`Item#getUseDuration(ItemStack)` gained a `LivingEntity` param in 1.21** — not 1.21.2, which is
where the rest of the use-API churn sits, and that off-by-one is why it was missed. Upstream never
wrote `@Override` on it, so the 1-arg form kept compiling on every node and simply stopped
overriding anything from 1.21 up. Vanilla's default then answered instead: `0` for anything without
a `CONSUMABLE` or `BLOCKS_ATTACKS` component. A use duration of `0` makes `startUsingItem` a no-op,
so the item re-equips and nothing happens — **the reporter's exact words were "they do not play an
animation, rather moving as if they were just picked up again."**

Ten items override it; **41 of 49 nodes** were affected (everything except 1.20.1/1.20.4/1.20.6):

| Item | Effect |
|---|---|
| blood sprayer, hemolymph blaster, grappling squok, dimensional carver, stink ray, skelewag sword, vine lasso | dead on ≥1.21 |
| shield of the deep | dead on **1.21–1.21.4 only** — `AMCompat.shieldProperties` stamps `BLOCKS_ATTACKS` from 1.21.5, and vanilla's default returns `72000` when that component is present, so the item repaired itself by accident |
| fish oil, rainbow jelly | ate at the `CONSUMABLE` component's rate instead of upstream's 40/64 ticks |

**Why the reporter's own triage was the fastest diagnostic.** They wrote that food, armour and the
echolocator still worked. That is the fault's exact shape: food is component-driven, armour never
calls `startUsingItem`, and `ItemEcholocator` is the one "tool" that acts in `use()` and returns
instead of beginning a use. Believe a report that draws a contrast — it is a free bisect.

**The fix, and the general pattern to reuse.** A replacement rule cannot do this, because three
classes call `this.getUseDuration(stack)` internally. Instead, keep the 1-arg method (it is still
the `<1.21` override *and* the class's own helper) and add a gated 2-arg override that delegates:

```java
//? if >=1.21 {
/*@Override
public int getUseDuration(ItemStack stack, net.minecraft.world.entity.LivingEntity user) {
    return this.getUseDuration(stack);
}
*///?}
public int getUseDuration(ItemStack stack) { … }
```

⚠️ **Put `@Override` on the gated arm.** That is the whole point: it converts this failure mode from
silent to a build error, so the next signature change is caught by the compiler instead of by a
player. The un-gated 1-arg form cannot carry one, since it genuinely overrides nothing on ≥1.21.

### A second instance, found by sweeping rather than reported

**`Item#inventoryTick` became `(ItemStack, ServerLevel, Entity, EquipmentSlot)` at 1.21.5.**
`ItemGhostlyPickaxe` was gated for this when the 1.21.5 wave was done; **`ItemVineLasso` was
missed**, so the lasso's `Swinging` flag went unwritten on all **27** nodes ≥1.21.5. Same fix shape,
with the body factored into an `inventoryTickImpl` — the tree's existing `releaseUsingImpl` idiom.

### Cheap sweep for the rest of this class of fault

Parse method declarations out of `versions/<node>/build/generated/stonecutter/main/java/**` — the
**projected** source, never root `src/`, or every correctly-gated arm reads as a false positive —
and flag any name that vanilla also declares with a different arity. On 26.2 that leaves ten hits;
the two above were real. **The remainder are still open and deliberately not fixed in `2.0.8`:**

- `isValidRepairItem(ItemStack, ItemStack)` on the ghostly pickaxe, skelewag sword, grappling squok,
  tarantula hawk elytra and tendon whip. Vanilla deleted it at 1.21.2 for the `Repairable`
  component. `ItemShieldOfTheDeep` has a documented SLICE dropping exactly this, so these five are
  consistent with a decision already taken — they just never got the comment. Anvil repair with the
  mod's own materials does not work ≥1.21.2.
- `getMaxDamage(ItemStack)` (tendon whip, ghostly pickaxe) and `isEnchantable(ItemStack)`
  (pigshoes) are **Forge-extension** shaped, so they may be live on Forge/NeoForge and dead on
  Fabric. Not verified either way — resolve by adding `@Override` and compiling one node per loader.

### A third and fourth instance, and the one shape the sweep cannot see (2026-08-09, thirteenth pass)

**`BlockBehaviour#use` split at 1.20.5** into `useItemOn(ItemStack, BlockState, …)` — the same slot in
the right-click dispatch — and `useWithoutItem(BlockState, …)`. **All seven** interactive blocks this
mod adds still declared `use`, so every one of them had a dead right-click on **44 of 49 nodes** since
`2.0.0` (report #71). The sweep above finds this one: same name, different arity.

⚠️ **A second boundary hides inside it.** 1.20.5–1.21.1 returns `ItemInteractionResult` and **1.21.2
goes back to plain `InteractionResult`** — one and a half versions apart, in the same signature. The
compat helper (`AMCompat.itemResult`) has to be the identity on one side and a mapping on the other,
and `PASS` maps to `SKIP_DEFAULT_BLOCK_INTERACTION`, not to `PASS`. Pin each from bytecode.

**The shape the sweep is blind to: a deleted *inherited* override.** **1.21.4 deleted
`BaseEntityBlock#getRenderShape`** while `BlockBehaviour#getRenderShape` survived with a `MODEL`
default. The three BER-only blocks in this mod (transmutation table, void worm beak, End Pirate ship
wheel) declared *nothing* — they inherited `INVISIBLE` from `BaseEntityBlock` — so above 1.21.4 they
silently started drawing their placeholder baked models underneath the block entity renderer (report
#67: "the transmutation table has an obsidian texture around it", on 30 nodes).

There is nothing to detect here by any of the usual means: no method on the mod's side changed arity,
no override was lost, `verify_overrides.py` sees a class that overrides nothing and never claimed to,
and the compiler is perfectly happy. **The only detector is `sigdiff.py` run against the *supertype*,
not the mod.** Whenever a wave's diff shows a method leaving an abstract base class the mod extends,
ask what the mod was inheriting from it.

**A related non-signature boundary, for completeness: a vanilla *tag* can be re-partitioned.** MC 26.1
cut `#minecraft:dirt` from ten members to three, re-homing the rest into three new tags (report #70).
No code changed, no error was raised, and the predicate simply got narrower — fourteen mob spawn tags
and one item placement quietly stopped accepting grass on the 26.x nodes. Diff the membership of every
`#minecraft:` tag the data pack references between client jars on each MC bump.

### A fifth and sixth instance, and the shape the *rename rule itself* creates (2026-08-12, sixteenth pass)

Both boundaries below are on the same theme as the ones above, but the first is caused by the port's
own tooling rather than by anything Mojang did to an override.

**`Entity#isControlledByLocalInstance` was deleted at 1.21.5** and replaced by a **`final`**
`isLocalInstanceAuthoritative()` that delegates to two new overridable feeders —
`isLocalClientAuthoritative()` (`protected`, client side) and `isClientAuthoritative()` (`public`,
server side, consulted negated). Both default to asking the controlling passenger, and
`Player.isClientAuthoritative()` returns `true`, so **the default flipped**: any vehicle carrying a
player is now client-authoritative unless it opts out.

The port's rename rule handled it — for *callers*:

```kotlin
string("!mc2105-localauth", true) { replace(".isControlledByLocalInstance()", ".isLocalInstanceAuthoritative()") }
```

⚠️ **The leading dot makes it a call-site rule, and a declaration has no leading dot.** So
`EntityStraddleboard`'s upstream `public boolean isControlledByLocalInstance() { return false; }` was
left behind under a name nothing calls, on 27 nodes, since `2.0.0` (report #79). Nothing can see
this: the compiler is happy, both mixin verifiers are irrelevant, and `verify_overrides.py` is blind
to renames by construction (same blind spot as #66).

**The general rule: every `.method()` replacement in `stonecutter.gradle.kts` has this hole.** When
adding one, check whether the mod *declares* the method as well as calling it — and if the
replacement target is `final`, as it is here, the fix is not a rename at all but a gated override of
whatever feeds it.

**`Entity#getPassengersRidingOffset` was deleted at 1.20.2**, seating moving onto the entity type's
`EntityAttachments`. Two independent faults come out of that one deletion (report #80):

- an entity that **overrode** it lost the override outright — dead code, the classic shape; and
- an entity that **relied on the default** silently changed behaviour, because the old default was
  `bbHeight * 0.75` and the `EntityAttachment.PASSENGER` fallback for a type declaring no attachment
  is `AT_HEIGHT`, i.e. the **full** height.

The second half is worth remembering separately: **a deleted method can move the goalposts for
classes that never mentioned it.** Four entities in this mod seated their rider too high on 47 nodes,
and only three of them had anything in their source to grep for.

Replacement on ≥1.20.2 is `public Vec3 getPassengerRidingPosition(Entity)` (non-final on every
version through 26.2). `EntityType.PLAYER` declares only a `vehicleAttachment`, and `Player`
overrides none of the seating methods on any version — so mobs that ride the player are on the same
fallback, and only escape it here because all five of them self-position in `rideTick()`.

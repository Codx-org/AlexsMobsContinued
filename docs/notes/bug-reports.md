# Community bug-report passes

> Read when a player report comes in, or before re-investigating one listed as blocked.
>
> Part of the Alex's Mobs Continued porting notes.

#### Shoulder pets: `startRiding(x, true)` bypasses the guard that makes sneaking work

Same session, chasing "crows can't be removed from the shoulders even when I sneak". `EntityCrow.rideTick`
*does* dismount correctly (`boardingCooldown == 0 && riding.isShiftKeyDown()`), but
`CrowAIFollowOwner.tick` re-boards with **`crow.startRiding(owner, true)`**, and `force = true` skips
vanilla `Entity#canRide` — which is precisely `!isShiftKeyDown() && boardingCooldown <= 0`, i.e. the two
things that make a shift-dismount stick (`removePassenger` sets the dismounted passenger's
`boardingCooldown = 60`; verified in `Entity`'s bytecode). `maxCircleTime` is `20 + rnd(100)` ticks, so the
crow is back on the shoulder within 1–6 s and the player concludes it cannot be removed. Fixed by gating
the AI's board on a new `EntityCrow.canBoardOwner(owner)` that re-imposes exactly what `force` skipped.
This is **upstream behaviour, not a port regression** — but it matches the report exactly.

`EntitySugarGlider` has no such AI (it only boards from `mobInteract`), so its "the slow falling won't
disappear" report is **not** root-caused. What *is* true is that `rideTick` applies
`SLOW_FALLING(100 ticks, amp 0, ambient, invisible)` every tick and **nothing ever removes it**, so it always
outlives the ride and never expires at all if the glider ever stays mounted. Added
`clearGrantedSlowFalling(mount)`, called from both `rideTick` dismount paths and from a new `stopRiding()`
override; it removes only an *ambient + invisible + amplifier-0* instance, which is the exact shape this mob
applies and not something a potion, beacon or command produces, so a slow falling from elsewhere is left
alone.

**Neither of these can be tested headlessly** — both hinge on a real player's shift-key flag, which is set
server-side only by `ServerboundPlayerCommandPacket` and cannot be forced from the console. Both were
therefore verified with a **real dev client driven by keyboard injection**, and **both are confirmed
fixed in-game** (NeoForge 1.21.1): the crow boards after the owner walks >4 blocks off, dismounts the
instant sneak is held, and stays off for the whole 1–6 s `maxCircleTime` window; the glider's
`active_effects` goes from the ambient/invisible `slow_falling` to "Found no elements" the instant it
dismounts. See "Driving a real dev client" in [`gates.md`](gates.md) for the rig — `ydotool` **mouse** movement does not work
on this Wayland session, only keyboard.

#### Anaconda body segments sink into the floor after the snake is hit

Reported as *"Anaconda's tail clipped through the ground after I hit it"*. Reproduced by RCON measurement
(floor top face y=151.0; after three `player_attack` hits the parts sat at **150.78–150.90** while the body
stayed at 151.0), root-caused, fixed, and re-verified.

`EntityAnacondaPart.tickMultipartPosition` derives each segment's pitch from `getLowPartHeight`, which
probes terrain in coarse **0.2-block steps** starting from the *parent* segment's Y. Knockback lifts the
head ~0.1 blocks off the floor, so the probe at the head's Y reads air and reports a phantom 0.2-block
drop-off. That feeds `partYDest` → `rawAngle` → the segment's `xRot`, and `avg` — the midpoint handed to
`moveTo`, i.e. the entity's **feet** — sinks with it.

Fixed with `EntityAnacondaPart.liftOutOfGround(x, y, z)`, applied to `avg.y` immediately before `moveTo`;
the lifted vector is what the method returns, so the rest of the chain stays connected. It is deliberately
a **point** test rather than the 1×1 slab `isOpaqueBlockAt` uses — that one reports "solid" for a wall
standing *beside* the snake and would lift segments that are perfectly fine. Guarded on `noPhysics` and
`isFluidAt`, and it only ever raises, so it is a no-op once a segment is on the surface. The pitch math
and `isOpaqueBlockAt` are left untouched.

Regressions checked in-game, both clean: swimming is unaffected (pool floor top 150.0, parts sat at
150.32 mid-water, not pinned to the surface), and a step-down is still followed (head on the 150.0 shelf →
leading parts at 150.36/150.56/150.70 while trailing parts stayed at 151.0), so the clamp does not flatten
the body onto one level.

> ⚠️ `isOpaqueBlockAt` has a **separate latent bug, deliberately not touched**: it moves the block's
> collision shape by the *sample's world coordinate* (`.move(vec3.x, vec3.y, vec3.z)`) where vanilla's
> `LivingEntity#suffocatesAt` — which it is copied from — moves by the **`BlockPos`**. The shape is
> therefore displaced by the fractional part of the coordinate. For full cubes it happens not to matter
> (the 1-wide probe box overlaps either way); it misreads slabs and stairs. Fixing it changes snake
> locomotion feel, so it is recorded here rather than changed as a drive-by.

The remaining open report is the **rhinoceros texture glitch**, which was **not reproduced** — see the
task list. Base, angry (red-eye) and potion-layer renders are all correct with the face intact;
`rhinoceros_potion.png` is **horn-only** (399 B, everything else transparent), so a haste potion literally
yields a yellow horn, and at point-blank range the camera sits *inside* that horn and the screen fills
with a featureless pale-yellow mass. That is the leading benign explanation. No render-type regression
either: `AMRenderTypes` does not define `entityCutoutNoCull`, so the potion layer uses vanilla's on every
node, and the ≥1.21.2 compat shims keep the layer signature identical. Needs a screenshot from the
reporter.

The animal-dictionary anaconda page was re-confirmed working incidentally (the book GUI screenshot shows a
crisp, readable index page with entity icons, which also re-confirms the double-background fix).

### The second community bug-report pass (`issues.md`, 2026-07-29 → 07-31)

Eleven reports pasted by the user. Split into 14 tracked items; **12 fixed, 2 blocked on the reporter**
(#3 was unblocked on 2026-08-01, after `2.0.0` shipped).
The first eleven fixes **shipped in `2.0.0`** (2026-08-01), on all 49 nodes; **#3's shipped in `2.0.1`**
(2026-08-01, all 49 nodes again — it was the only change in that release).

⚠️ **Provenance, corrected 2026-08-01:** these did **not** come from this repo's published jars. Every
one that names a version says **`v1.0.5` on Fabric 26.1.2** — i.e. the sibling **`AlexsMobsFP`** line,
which is the only Fabric build that has ever shipped. (An earlier revision of this note said "published
1.0.8 jars, MC ≤ 1.21.6", which would send a future triage at the wrong MC era.) It changes nothing
about the fixes: every root cause was in **shared source** — three are upstream Alex's Mobs faults the
fork inherited (orca, sugar glider, centipede) and the rest hit Forge and NeoForge identically — so all
49 nodes carry the fix. **None of it is a Fabric-parity side effect; Milestone 15 fixed none of these.**

Three of them are **not** port regressions but upstream faults this fork inherited (orca, sugar glider,
centipede); two are port regressions that the whole five-step gate is structurally blind to because it
never constructs an entity or reads an item model (glove/item models, straddleboard); the rest are
upstream bugs made visible by players finally running the newer nodes.

| # | Report | Root cause | Fix |
|---|---|---|---|
| 1 | orca crashes the game in water | `EntityOrca.tick` re-read `getTarget()` after `hurt`, and the hurt can clear it (victim dies / a knockback listener retargets) → NPE mid-tick | use the local `attackTarget` already null-checked at the top of the block |
| 2 | sugar glider crash, `Parameters not allowed … [minecraft:block_state]` | `getForageLoot` passed `BLOCK_STATE` into a `PIGLIN_BARTER` param set, which allows only `THIS_ENTITY`. Ran **every tick**, so it kills the tick loop | drop the parameter — nothing in `gameplay/sugar_glider_reward.json` reads it |
| 4 | anaconda **and** void worm show a name tag over every body part | upstream's `shouldShowName` on the part renderers was a verbatim copy of vanilla's guard, which still lets a name through | `return false` in `RenderAnacondaPart` / `RenderVoidWormBody`; **both** signatures need overriding because of the 1.21.2 hook change |
| 6 | centipede head "always faces south" | `EntityCentipedeHead` clamped `yBodyRot` toward `getYRot()` **without `Mth.wrapDegrees`**, so it chased the long way round the ±180 boundary and settled at 0 (= south); the 2°/tick rate was also far slower than MoveControl's ≤90° swings | `yBodyRot += Mth.clamp(Mth.wrapDegrees(getYRot() - yBodyRot), -10F, 10F)` |
| 8 | straddleboard mounts the player but never moves | the board is deliberately **server-driven** (`isControlledByLocalInstance()` is false) and read `player.zza`, which **1.21.2 stopped populating server-side**. Only the yaw-driven turning animation (which reads the still-synced `getYRot()`) responded — exactly what the reporter described | route through `AMCompat.riderForward(player)` |
| 9 | crow keeps flapping instead of sitting on the shoulder | `tick()` clears the flying flag for a passenger, but the **target** goals are not stood down by riding the way the movement goals are — `AITargetItems#moveTo` re-asserts it every tick a dropped item is in range. The flag alternated once per tick and the synced value flickered | `flying && (isBaby() \|\| isPassenger())` clears it; the animator also gates on `!isSittingOrPassenger` |
| 10 | falconry glove "does not appear worn" | **far bigger than the glove** — see below | `DataPackMigration.writeItemModelDefinitions` |
| 11 | eagle flight camera twitches on the vertical axis | while the player steers, pitch comes from `directFromPlayer`, but vanilla's `LookControl` unconditionally does `setXRot(0)` **every server tick**. Two writers, so the synced pitch alternated between the commanded angle and level flight | an anonymous `LookControl` whose `resetXRotOnTick()` returns `!controlledFlag`, plus `!controlledFlag` in both look goals' `canUse`/`canContinueToUse` |
| 13 | animal dictionary is hard to read | `GuiBasicBook` wrapped by **character count** against a proportional font, so wide-glyph lines ran past their column into the neighbouring one (17 English lines), the final word was folded back onto an already-overflowing line (12 more), and the column/page switch happens at *commit* time so a line accumulated in the wide left column could be drawn in the narrow right one (31 more) | measure with `Font#width` as an **additional** break condition (never weaker, so authored line breaks survive), predict the column switch before measuring, give a width-broken final word its own line, and `hardSplitToWidth` for unspaced scripts (zh/ja/ko hand the wrapper a whole line as one token) |
| 5 | anaconda clips into the ground | already fixed and documented in the previous pass (`EntityAnacondaPart.liftOutOfGround`) | re-confirmed, no new change |
| 12 | eagle can break blocks | **upstream behaviour**, not a port regression | none |
| 3 | attacking a cachalot / giant squid crashes the game | **unblocked 2026-08-01** by a stack trace. 26.2 stopped assigning entity ids client-side and made `Entity#getId()` throw at id 0; a `PartEntity` never gets one — see the section below | `AMCompat.assignClientPartId`, called from the five part constructors. **Not in `2.0.0`** |

#### ⚠️ Every item in the mod was the missing-model cube on every node ≥ 1.21.4

*(20 of them when this was written — Forge and NeoForge only. The tree is **30** nodes ≥ 1.21.4 now that
Fabric is in it. Any node count written down in these notes predates some wave; re-derive it.)*

Report #10 said "the falconry glove does not appear worn". The glove was the visible corner of a
**mod-wide** break: 1.21.4 made an item's model **indirect**. Up to 1.21.3 the client resolved
`assets/<ns>/models/item/<id>.json` by convention; 1.21.4 introduced **item model definitions** at
`assets/<ns>/items/<id>.json`, and the legacy model is now only reachable *through* one. A missing
definition is **not an error** — the item just renders as the missing-model cube, with one
`Missing item model for location <id>` line per item per resource reload. This mod has **280** items
authored against 1.20.1 and not one definition, so **20 of the 32 nodes rendered every item wrong**.
Neither gate could see it: `bootgate.sh` is a dedicated server (no models at all) and `clientgate.sh`
stops at the title screen.

Fixed at build time — data-pack/asset JSON is not preprocessed by Stonecutter — in
**`DataPackMigration.writeItemModelDefinitions`**, hooked from `ModPlatformPlugin` under `>=1.21.4`.
One definition is derived per legacy `models/item/*.json` (**303** of them) rather than committing that
many near-identical files that 12 of the 32 nodes would never read. Three shapes:

- **plain**: `{"model":{"type":"minecraft:model","model":"<ns>:item/<id>"}}`.
- **spawn eggs**: `SpawnEggItem` stopped carrying colours at 1.21.4, so the two tints become
  `minecraft:constant` tint sources over vanilla's `item/template_spawn_egg`. They are read back out of
  `AMItemRegistry.java` by regex, so **the registration stays the single source of truth** — and it is
  read from `rootProject.file(...)`, not the node's projection, because that file is not version-gated.
- **the 16 `builtin/entity` models**, whose parent 1.21.4 deleted along with the ISTER mechanism — a
  definition pointing at one of those still resolves to nothing. `repairBuiltinEntityModel` rewrites
  each, in descending order of fidelity: **paired** (`<id>_hand` + `<id>_inventory` both exist —
  `falconry_glove`, `skelewag_sword`, `stink_ray`, `vine_lasso`) → a `minecraft:select` over
  `minecraft:display_context`, which expresses natively what `AMItemstackRenderer` used to do by hand,
  so **those four get their in-hand look back rather than staying an accepted regression**; else parent
  to `models/block/<id>` if one exists; else `item/generated` over `textures/item/<id>`; else emptied
  (the inert ones — `fancy_item`, `effect_item`, `tab_icon`, and the `*_blocking` override targets no
  item is registered under). Emptied rather than deleted: `models/item` is also the directory the
  definition loop enumerates.

The build prints `Wrote 303 item model definitions for the 1.21.4 item format`. The pass is **idempotent**
— verified with `:<node>:processResources --rerun`: the re-copy restores the pristine `builtin/entity`
source model, so the paired branch fires again and the `select` survives rather than degrading to the
plain shape.

> ⚠️ Verifying this in a jar re-ran straight into the trap [`porting-log.md`](porting-log.md) already records: `ls …libs/*.jar |
> head -1` picks the **alphabetically first** name, and `alexsmobs-1.0.8-…+1.21.4.jar` sorts before
> `…-1.21.4-SNAPSHOT.jar` — i.e. the *release-flavoured* jar from an earlier session, built before this
> fix existed. It showed the plain definition and an unrepaired `builtin/entity` model, which reads
> exactly like the fix not working. Pin the filename, or check the mtime.

> This is the same shape as Milestone 13's Forge-26 `c:` tags and Milestone 14's 26.2 entity
> predicates: **compile-clean, logged-not-thrown, and invisible to every gate step**. Three waves in a
> row have shipped one. When a version changes how a *resource* is addressed, assume the mod is wrong
> until a migration pass says otherwise.

#### Attacking a cachalot or giant squid crashes on 26.2 — client-side parts have no entity id

This is report **#3** ("attacking the giant squid crashes the game"), which sat blocked for want of a
stack trace. The trace arrived 2026-08-01, from a Fabric client:

```
java.lang.IllegalStateException: Tried to access entity ID before ID assignment
    at net.minecraft.world.entity.Entity.getId(Entity.java:438)
    at net.minecraft.client.multiplayer.MultiPlayerGameMode.handler$zbi000$alexsmobs$attackMultipart(…)
    at net.minecraft.client.multiplayer.MultiPlayerGameMode.attack(…)
```

**Root cause is the 26.2 entity-id change**, not the attack chain (see [`api-eras.md`](api-eras.md)):
26.2 moved id assignment out of `Entity`'s constructor into `Level#getNextEntityId()`, which returns
`0` on the client, and made `getId()` throw while the id is 0. A `PartEntity` is never added to any
level's entity storage and never gets a spawn packet on *any* loader, so **every client-side part is
permanently stuck at id 0** and the first read of it crashes. Verified by disassembly, not memory:
26.2's `Entity.getId()` carries the throw and its constructor calls `getNextEntityId()`; 26.1.2 and
every version below still increment the shared `ENTITY_COUNTER`. **26.2 is the only affected node
row** — but all three loaders are affected, in three different places:

| Where | Loaders | When it fires |
|---|---|---|
| `ClientLevel$EntityCallbacks.onTrackingStart` → `partEntities.put(part.getId(), part)` | Forge **only** | **the mob comes into view** — Forge's own part map is id-keyed, so this is the earliest and worst of the three. **NeoForge is not affected here**: its `ClientLevel` keeps a plain `List<PartEntity>` and calls `dragonParts.addAll(Arrays.asList(entity.getParts()))`, never reading an id. Checked by disassembling both patched jars after assuming they matched — they don't |
| `MultiPlayerGameMode#attack` → `new ServerboundAttackPacket(entity.getId())` | Forge, NeoForge | attacking a part (vanilla code, before anything of ours runs) |
| `FabricMultiPlayerGameModeMixin` → `MessageHurtMultipart(part.getId(), …)` | Fabric | attacking a part — **the reported crash** |

Because two of the three are outside our source, dodging the reads one by one could not have worked.
Fixed at the source of the invariant instead: **`AMCompat.assignClientPartId`**, called from all five
`EntityCachalotPart` / `EntityGiantSquidPart` / `EntityLaviathanPart` constructors, gives every
client-side part a unique id again. Deliberate choices:

- **Negative, counting down.** The pre-26.2 counter was shared with real entities, so a client-local
  part id could in principle collide with a genuine one; a negative id cannot. Both places that
  resolve one server-side (`MessageHurtMultipart`'s `part` field, `ServerboundAttackPacket`) already
  treat "no such entity" as the normal answer for a part — `handleAttack` just returns, it does **not**
  disconnect — which is exactly what a client-local id has always produced.
- **Ungated, not `//? if >=26.2`.** It is the same unique-id invariant on every node, and it retires
  the collision risk on the versions that did use the shared counter.
- Server-side parts are untouched (`level().isClientSide()` guard) and keep the id `ServerLevel` gave
  them, which is the one that actually resolves.

It also fixes a second, quieter 26.2 fault on the way: `Entity#equals` compares the raw `id` field, so
at id 0 **every part was `equals` to every other part** and to any other unassigned entity.

The other four multipart classes (`EntityCentipedeBody`, `EntityBoneSerpentPart`, `EntityVoidWormPart`,
`EntityAnacondaPart`) are real spawned entities, not `PartEntity`s, and every `getId()` on them is
already behind `!level().isClientSide()`. They need nothing.

#### Blocked on the reporter — do not re-investigate without new information

- ~~**#3 "attacking the giant squid crashes the game"**~~ — **root-caused and fixed** once the crash
  report arrived (2026-08-01). See the section above; the static audit was looking for an unguarded
  dereference and the fault was a *thrown* `IllegalStateException` from `Entity#getId()`.
- **#7 "blobfish has a bugged texture"** — investigated to exhaustion and **no defect exists**. Every
  hypothesis is dead, so do not re-derive them: the two models each declare their own matching texture
  size (`ModelBlobfish` 32×32, `ModelBlobfishDepressurized` **64×64** — not a mismatch); the artwork is
  intact; the item textures/models resolve; the ≥1.21.2 compat pipeline picks model and texture from the
  same state. The last standing anomaly — `ModelBlobfish`'s zero-width `tail_fin` and zero-depth
  `fin_left/right` paint only *one* of each pair of coplanar faces (`blobfish.png` is transparent at
  u0–9/v24–29 and painted at u10–19/v24–29) — is the ordinary flat-plane idiom, because
  **`RenderType.entityCutoutNoCull` is the model default** (`compat/EntityModel:40`) so the painted face
  shows from both sides and the blank one cutout-discards. **Needs a screenshot.** (Harmless leftover
  noticed on the way: `blobfish_pressurized.png` is byte-identical to `blobfish.png` and referenced
  nowhere — a dead asset.)
- ~~**#15 "i spawned an underminer in my world and it crashed and i cant open my world again"**~~ —
  **root-caused and fixed 2026-08-03**, when two crash reports for the same `ClassCastException`
  arrived (#16 below). The last bullet of this entry had named the exact fault as *speculative*; it
  was the real one. The rest is kept because it is the record of what a server-side gate can and
  cannot see.
  (2026-08-02) — **not reproduced server-side, and the static audit found no defect.** The report
  carries **no MC version, no loader, no mod version and no crash report**, which is the whole
  blocker: a negative result on 4 of 49 nodes proves very little.
  - **What was ruled out, so don't redo it.** `EntityUnderminer` and its goals (`MineGoal`,
    `EtherealMoveController`, `MonsterAIWalkThroughHallsOfStructure` — the last already try/catches
    its structure lookups), `RenderUnderminer` / `ModelUnderminerDwarf` / `LayerUnderminerItem`,
    the three sounds, `ghostly_pickaxe`'s item model (a plain `item/handheld`, so the 1.21.4
    item-model-definition repair path is not implicated), and the `underminer*` config defaults. The
    entity source is effectively identical to the `AlexsMobsFP` reference that is known to work.
    `2.0.2`'s only functional change (`/aac nameplates`) does not touch it.
  - **A new gate closed part of the hole** — `bootgate.sh` never *constructs* an entity, so a
    two-pass RCON harness was built (`scratchpad`, worth re-creating: enable RCON in the node's
    `server.properties`, `forceload`, `summon alexsmobs:underminer` ×6 across y=-40…70, soak, then
    **reboot the same world** — the literal "can't open it again"). On `1.20.1-forge`,
    `1.21.1-neoforge`, `26.2-neoforge` and `26.2-fabric`: every summon accepted, no crash report, no
    `Ticking entity`, and on reboot all six re-loaded and kept ticking. `rc=0`.
  - **What that gate still cannot see, and why the client is the prime suspect.** No player is
    present, so `getNearestPlayer`→hiding→`lookAt`, `pickUpItem`, and the whole damage/
    `HurtByTargetGoal` path never run; `max-tick-time=-1` disables the watchdog; and **rendering is
    entirely untested**. Note that `isDwarf()`/`getVariant()` have **no server-side consumer at all**
    — only `RenderUnderminer` and `LayerUnderminerItem` read them, and `/summon` skips
    `finalizeSpawn`, so the headless harness only ever produced the default dwarf. On singleplayer a
    render crash on a mob standing at the load point reproduces "crashed on spawn, and now the world
    won't open" *exactly*.
  - **The one crash path actually identified — ✅ confirmed 2026-08-03, and it did need a second mod.** On
    `>=1.21.2` the three renderers that override `render` outright (`RenderUnderminer:187`,
    `RenderTiger:143`, `RenderFarseer:184`) iterate `this.layers` and hard-cast every element to
    `compat.RenderLayer`. A layer attached by *another* mod through `EntityRenderersEvent.AddLayers`
    (or Fabric's registration callback) is a plain vanilla `RenderLayer`, so the cast throws
    `ClassCastException` on the first frame the mob is visible — every frame, unrecoverable.
    Fixed in the third pass — see **#16** below. → [`fabric.md`](fabric.md) has the row on these three.
  - Recovery advice given to the reporter meanwhile, still worth reusing: back up the save; the crash
    report names the culprit; if it is one of this mod's entities, removing the mod once lets vanilla
    drop the unknown entity on load, after which the mod can be put back — or delete
    `alexsmobs:underminer` with MCA Selector.

- **#14 "whenever their flying animation plays they teleport around and become really large at the edges
  of vision"** — **the report names no mob.** Ruled out: the `moveTo`→`snapTo` replacement rules; every
  hard position write under `entity/`; `AMEntityRegistry`'s tracking ranges and update intervals; the
  farseer (contains no teleport); the blue jay (its only teleport is a >40-block raccoon catch-up). The
  mobs that *can* teleport at all are `EntityCosmaw`, `EntityVoidWorm`, `EntityVoidWormPart`,
  `EntityCosmicCod`, `EntityVoidPortal`, `EntityBlueJay`. **Needs the mob name or a video.**

### The third community bug-report pass (2026-08-02 → 08-03, against published `2.0.2`)

Four reports, this time against **this repo's own jars** — the first pass where that is true, so every
root cause here is a live regression in a shipped version rather than an inherited fault. Three are
fixed; the fourth is **not reproducible** and is back with the reporter.

| # | Report | Root cause | Fix |
|---|---|---|---|
| 16 | underminer crashes the client the moment it comes into view (`ClassCastException`, two variants) | the three renderers that override `render` outright hard-cast every element of `this.layers` to `compat.RenderLayer`. On `>=1.21.2` that list also holds `compat.StateRenderLayer`s (**our own `LayerRainbow`**) and any layer another mod attaches (**Trinkets**) | `LivingEntityRenderer#renderAttachedLayers` — call the shim's own layer loop instead of iterating and casting |
| 17 | spawn-egg textures are missing | **1.21.5 deleted `item/template_spawn_egg`** and both of its greyscale layers. All 89 of this mod's egg models parent to it → missing-model cube on the 27 nodes ≥ 1.21.5 | vendor the two layer textures and re-point every egg model at them in `DataPackMigration.retemplateSpawnEggs` |
| 18 | animal dictionary shows empty frames, no mob icons (26.1.2 Fabric) | **1.21.4 made `GuiGraphics#enableScissor` transform its rectangle by the current GUI pose.** `EntityLinkButton` was already inside a `translate + scale`, so passing absolute coordinates scissored to ≈twice the button's offset and clipped every icon away — while the frames, drawn outside the scissor, kept rendering | version-gate the rectangle: local-space `(4,4,20,20)` from 1.21.4, the absolute form below it |
| 19 | "raccoons and crows still don't behave as tamed" | **not reproduced** — see below | none yet |
| 20 | found while verifying #18 in-game: the animal dictionary's left page has **no text at all** | **1.21.2 deleted `Font`'s implicit-opacity guard**, so upstream's alpha-less `0x303030` draws at alpha 0. Four more sites share the fault | write the `FF` out at all five sites; correct on every version, so no gate |
| 21 | found while verifying #20 in-game: the dictionary's index page has **a hole where the mob should be** | **1.21.4 deleted the ISTER**, and three registered items (`tab_icon`, `fancy_item`, `effect_item`) had no model at all — their whole appearance was drawn from the stack's NBT | static substitutes at build time: 59 advancement icons become the real item their NBT named; the three models borrow a sprite |

#### #16 — never cast the elements of `this.layers`

Confirmed by two stack traces with different left-hand sides, which is what makes the diagnosis
airtight: `eu.pb4.trinkets.impl.client.render.TrinketRenderLayer` and
`com.github.alexthe666.alexsmobs.client.render.layer.LayerRainbow`, both *"cannot be cast to
com.github.alexthe666.alexsmobs.client.render.compat.RenderLayer"* at `RenderUnderminer.render:187`.
Reported on **MC 1.21.11 / Fabric**, but the code is identical on all 27 `>=1.21.2` nodes and all
three loaders.

Two independent sources of a non-`compat.RenderLayer` element:

- **Another mod's layer.** Trinkets attaches one through the loader's add-layers hook; it is a plain
  vanilla layer and always was. This is the case the earlier speculative note predicted.
- **Our own.** `LayerRainbow` extends `compat.StateRenderLayer`, a *sibling* of `compat.RenderLayer`,
  not a subclass — so the underminer crashed with **no second mod at all** as soon as a rainbow-variant
  layer was in the list. That is why #15's reporter saw it on spawn.

The fix is to stop reimplementing the loop. `compat.LivingEntityRenderer` already has one that
dispatches each layer correctly by type, so it now exposes:

```java
protected final void renderAttachedLayers(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight)
```

which reads the in-flight `AMRenderState` and calls the shim's `renderLayers`. `RenderUnderminer`,
`RenderTiger` and `RenderFarseer` call that on `>=1.21.2` and keep their old explicit loop below it
(where the layer list really is homogeneous). **An `instanceof` filter would have been the wrong fix**
— it would have silently dropped every foreign layer instead of drawing it.

> The general rule: **a renderer never owns the contents of `this.layers`.** Any wholesale `render`
> override must delegate to the vanilla/compat layer loop, because other mods write into that list.

#### #17 — 1.21.5 deleted the spawn-egg template

`assets/minecraft/models/item/template_spawn_egg.json` and its two greyscale textures
(`item/spawn_egg`, `item/spawn_egg_overlay`) were removed in **1.21.5**, when vanilla moved egg tinting
into the item-model definition format. This mod's 89 egg models still parent to that template, so from
1.21.5 up they resolve to nothing.

Note this is the **second** layer of the same wound: report #10 already added
`writeItemModelDefinitions`, whose spawn-egg branch builds a correct `minecraft:constant` tint pair —
*over a parent that no longer exists*. The definition was right and the model underneath it was dead.

Fixed at build time, next to that pass: **`DataPackMigration.retemplateSpawnEggs`**, hooked from
`ModPlatformPlugin` under `>=1.21.5`, rewrites each egg model to
`item/generated` + `alexsmobs:item/spawn_egg` / `alexsmobs:item/spawn_egg_overlay`. The build prints
`Re-pointed 89 spawn-egg models at this mod's own egg layers`; verified in a running 26.2-fabric
server's processed resources.

> ⚠️ **The two vendored PNGs are Mojang's own 1.21.4 art**, copied verbatim into
> `src/main/resources/assets/alexsmobs/textures/item/`. They are the greyscale masks the tints colour,
> so any replacement only has to be the same two shapes — worth redrawing if that provenance matters.

#### #18 — `enableScissor` became pose-relative at 1.21.4

The third resource-era fault in a row (see #10's box), and the same shape: **compile-clean, silent, and
invisible to both gates** — `clientgate.sh` stops at the title screen, so no GUI is ever opened.

Up to 1.21.3, `GuiGraphics#enableScissor(x0,y0,x1,y1)` took raw screen coordinates. From 1.21.4 it
multiplies them through the current GUI pose. `EntityLinkButton#renderWidget` pushes
`translate(getX(), getY())` + `scale(f, f)` before scissoring, and was passing
`getX() + f*4 … getX() + f*20` — so the window landed at roughly `2 × getX()`, off the button, and the
entity render inside it was clipped to nothing. The two `drawBtn` calls sit outside the scissor, which
is exactly why the screenshot shows ~50 intact empty frames.

The correct rectangle from 1.21.4 is the one the pose is *about to* map: `(4, 4, 20, 20)`.

#### #19 — "raccoons and crows don't behave as tamed": not reproducible

Investigated headlessly on **26.2-fabric** with a purpose-built RCON harness (see
[`gates.md`](gates.md)). Every step of both taming paths works server-side:

- **Crow** tames from a thrown `minecraft:pumpkin_seeds` carrying a `Thrower` — ends with `Owner` set
  **and `Command: 1`**, i.e. it follows immediately.
- **Raccoon** tames from a thrown `minecraft:egg` after the wash animation completes. Upstream rolls
  **30%** per washed egg (`EntityRaccoon.postWashItem`), so a run of failures is expected and is not a
  bug — 13 consecutive misses looked like "never tames" until a 24-egg trial produced one.
- **Follow** works: a raccoon with `Owner` + `RacCommand:1` walked 26 blocks to its owner, matching a
  vanilla wolf run as a control.
- **Defend** works: `OwnerHurtByTargetGoal` fired and the tamed raccoon killed the pig that damaged its
  owner.
- The 26.x compat layer underneath is correct — `AMCompat.getOwnerUUID`/`setOwnerUUID`/`setTame`
  disassemble to `getOwnerReference()` / `setOwnerReference(EntityReference.of(uuid))` /
  `setTame(z, true)`; the `ValueOutput`/`ValueInput` save bridging round-trips the owner; and the
  `tags/items` → `tags/item` + `forge:` → `c:` migration resolves `#c:eggs`.

**Two things the harness cannot test**, and they are the open explanations:

1. **The owner was a villager, not a `Player`.** Everything above is owner-type-agnostic in the source,
   but nothing has confirmed it with a real player.
2. **A freshly tamed raccoon starts at `RacCommand = 0` ("wander") and does not follow until you
   right-click it with an empty hand.** This is upstream behaviour, not a port regression, and it
   reproduces the report's wording exactly. The crow does *not* have this step — it auto-sets command 1
   — so if crows also fail to follow, explanation 2 is out and it is a genuine defect.

**Needs from the reporter:** MC version + loader, whether the mob shows as tamed at all (hearts on
taming, name in the dictionary), and whether right-clicking the raccoon with an empty hand changes
anything.


#### #20 — 1.21.2 deleted `Font`'s implicit-opacity guard

Not a report: found by opening the dictionary to check #18's fix and seeing an empty left page. It had
looked like part of #18 (the whole page was suspect) right up until the icons came back and the text
did not.

Through 1.21.1, `Font#drawInBatch` opened with

```java
if ((color & 0xFC000000) == 0) color |= 0xFF000000;
```

so a caller passing a bare `0x303030` got opaque grey. **1.21.2 deleted those two instructions** —
bytecode-verified with `javap` against NeoFormRuntime's recompiled jars: the constant pair is present in
1.20.4 / 1.20.6 / 1.21.1's `Font` and absent from 1.21.2 onward. The same call now draws at alpha 0.

Upstream, authored against 1.20.1, spells five colours with no alpha byte — every one of them invisible
on the 35 nodes ≥ 1.21.2:

| Site | Upstream colour | Player-visible effect |
|---|---|---|
| `GuiBasicBook#getTextColor` | `0x303030` | the animal dictionary's **entire body text** |
| `GuiBasicBook#getTitleColor` | `0x3F3222` | every page title in it |
| `GUITransmutationTable#renderLabels` | `0x4EFF21` | the transmutation table's title |
| `ButtonTransmute#renderWidget` (×3) | `0x80FF20` / `0xFF6060` / `0xC7FFD0` | the XP cost on each transmute button |
| `RenderSeal#renderNameTag` | `1` / `0` | the seal easter egg's floating text |

Only the first was reported; the other four came out of a grep for colour literals once the mechanism
was understood. **Writing the `FF` out is correct on all versions**, so none of the five needed a
Stonecutter gate — which is also why this is worth remembering as a *port* rule rather than a bug: any
colour literal inherited from pre-1.21.2 source is suspect until its top byte is checked.

Verified in-game on `26.2-fabric` (the reporter's own screenshot shape): title and body text both draw.
The other four sites are fixed by the same mechanism but have **not** been looked at in a client.

#### #21 — 1.21.4 deleted the ISTER, and three items were nothing but an ISTER

Also found in-game, one screenshot after #20: with the text back, the dictionary's index page had an
empty rectangle between its title and its text.

`AMItemstackRenderer` (a `BlockEntityWithoutLevelRenderer`) had three branches that drew an item with no
model of its own, entirely from the stack's NBT:

| Item | NBT | What it drew |
|---|---|---|
| `tab_icon` | `DisplayEntityType` | that mob — and with no tag, a **different mob every two seconds** |
| `fancy_item` | `DisplayItem` (+ bob/zoom/spin) | that item, animated |
| `effect_item` | `DisplayEffect` | that mob effect's icon |

1.21.4 deleted `builtin/entity` and the whole ISTER mechanism, so on the 30 nodes ≥ 1.21.4 all three
render nothing. `DataPackMigration.repairBuiltinEntityModel` had already been emptying their models —
and its comment called them *"the inert ones … no item is registered under"*, which is **wrong**: all
three are registered, and between them they back **59 advancement icons**, the creative tab's icon and
the one `item_render` on the dictionary's index page. That stale comment is why the group was skipped in
Milestone 6 and again during #17.

The fix is static substitution at build time (`DataPackMigration.restaticAdvancementIcons`, ≥ 1.21.4),
because the NBT says exactly what each icon meant:

- `DisplayItem` → that item verbatim (8 advancements).
- `DisplayEntityType` → **the mob's own spawn egg** (50) — the one static sprite in the game that means
  one specific mob. All 50 resolve; the pass checks the model file exists rather than assuming.
- `DisplayEffect` → an item from that effect's own advancement chain (1, `ender_flu`).
- The three item models borrow `animal_dictionary`'s sprite, for the two callers that carry no NBT to
  recover a subject from (the creative tab, the index page).

⚠️ **The index-page icon stays an `item_render`.** Converting it to an `entity_render` was the obvious
move — a real animal, closer to the original — but `GuiBasicBook` registers a `Whitespace` for every
`item_render` and **none for an `entity_render`**, so the page's text would have reflowed straight
through the animal. A borrowed sprite keeps the layout identical.

The animation is gone on both counts, and restoring it would mean a `minecraft:special` model renderer
per item — far more than the icons are worth.

Predicted count first, then measured: **59** icons on `26.2-fabric`, and afterwards zero advancements
anywhere in the tree still name one of the three items. **Not yet checked in a client.**

### The fourth community bug-report pass (2026-08-03, against published `2.0.3`)

Two reports, both from the same player, and between them they are the **rest of the two faults the
third pass only half-fixed**. Neither is new damage: both have been shipped-broken since long before
`2.0.0`, and #21's own fix is what made #23 visible as a distinct thing rather than "the ISTER items
are gone".

⚠️ This pass was interrupted mid-flight by a machine crash. What that cost is recorded under
[Publishing state](#publishing-state-after-the-fourth-pass) below — read it before assuming anything
here is in a jar.

| # | Report | Root cause | Fix |
|---|---|---|---|
| 22 | the bald eagle's falconry loop does nothing — launching it, siccing it on an animal, flying it first-person; "anything with right click/left click" | **Fabric only.** Every one of those starts with `ILeftClick#onLeftClick`, and the only thing that runs it is Forge's `PlayerInteractEvent.LeftClickEmpty`, fired from `Minecraft#startAttack`. Fabric has no such event, so the glove's hook was never called on any of the 17 Fabric nodes | `FabricClientEvents.registerEmptyLeftClick` — Fabric API's `ClientPreAttackCallback`, guarded on `clicks > 0` and a `MISS` hit result, into `FabricServerEvents.fireEmptyLeftClick` |
| 23 | the shattered dimensional carver "and a couple others" have no icon in the creative menu | **two different faults with one symptom**, split at 1.21.4 — below it the ISTER exists and was never *wired* on Fabric; from it up vanilla deleted the ISTER on every loader. Either way the item's model is `builtin/entity`, a placeholder that resolves to nothing, so the renderer *is* the whole appearance and losing it leaves an **invisible** slot rather than a missing-model cube | `FabricItemRenderers` below 1.21.4; rebuilt models at build time from 1.21.4 up |

#### #22 — the whole falconry loop hung on one client-only Forge event

Worth stating plainly because the report reads like several features: **launching the eagle, aiming it
at a target, and the hooded first-person flight are one code path**, not three. `ItemFalconryGlove.onLeftClick`
ray-traces for a `pointedEntity`, dismounts the perched `IFalconry` passenger, and calls
`falcon.onLaunch(player, pointedEntity)` — the target the player was looking at *is* the "right click
an animal to attack" the report describes. With the hook never called, all of it is silent.

`ClientPreAttackCallback` needs no mixin and its signature is byte-identical from the 1.20.1 fabric-api
to 26.2's. The two guards are what make it Forge's event rather than merely "the attack key moved" —
see the doc comment on the method for why each is load-bearing (without the click-count guard the
tendon whip, the other `ILeftClick` item, cracks 20 times a second).

#### #23 — two faults, one symptom, split at 1.21.4

⚠️ **Ask which MC version and loader before diagnosing this one.** The first read of the screenshot
was "1.21.4 deleted the ISTER, same as #21" — and that is true, but the reporter is on **Fabric
1.21.1**, where the ISTER still exists. Both faults are real, they produce the *same* empty slot, and
they need different fixes. A fix for either alone leaves most of the tree broken.

What makes the symptom identical is the model. All 16 affected items have `builtin/entity` as their
model, which is a placeholder that resolves to nothing — the ISTER *is* the item's entire appearance.
Lose it and the slot renders **invisible**, keeping its name and tooltip, rather than showing the
black-and-magenta missing-model cube that would have made this obvious years ago.

##### Below 1.21.4, Fabric only — the ISTER exists and was never wired

The mechanism is alive here; this tree simply never connected it. Each of the ~13 items implements
`IClientExtensionItem` with the identical body
`consumer.accept((IClientItemExtensions) AlexsMobs.PROXY.getISTERProperties())`, and the *consumer*
comes from `ClientProxy.onRegisterClientExtensions` — a Forge/NeoForge mod-bus handler with no Fabric
counterpart. On Fabric `IClientItemExtensions` is a vendored empty type token, nothing ever calls the
consumer, and all eleven ISTER items drew nothing at all. **Shipped that way on every Fabric node
since Milestone 15**; `docs/notes/fabric.md`'s divergence list named it as accepted, which it should
not have been.

Fixed by `fabric/client/FabricItemRenderers`, called from `ClientProxy.clientInit`'s Fabric arm next
to `FabricArmorRenderers`. It walks `BuiltInRegistries.ITEM`, offers each `IClientExtensionItem` the
same consumer NeoForge does, and registers the ones that hand back an `AMItemRenderProperties` with
Fabric API's `BuiltinItemRendererRegistry`. Two things worth keeping:

- **The item list is not hardcoded.** Letting each item's own `initializeClient` declare what it wants
  keeps the registration the single source of truth, and is what separates the eleven ISTER items from
  the two armour ones (`ItemModArmor`, `ItemTarantulaHawkElytra`) without a second list to drift.
- **The gate is exact and was measured, not assumed.** `BuiltinItemRendererRegistry` is present in the
  pinned `fabric-api` for every node 1.20.1 → 1.21.3 and absent from 1.21.4 up — the same boundary at
  which vanilla dropped the BEWLR — checked by extracting the nested `fabric-rendering-v1` jar from all
  17 pins. `DynamicItemRenderer#render` takes exactly `renderByItem`'s six parameters with a
  byte-identical descriptor across that range (`javap`), so one `//? if <1.21.4` block covers it with
  no per-version arm.

This is the better of the two outcomes: the real renderer comes back, so the items get their *actual*
look — the carver's drifting shards, the shield's 3D model, the in-hand/inventory swaps — not a static
approximation.

##### From 1.21.4 up, every loader — vanilla deleted the ISTER

Nothing to re-wire, so the models are rebuilt at build time by
`DataPackMigration.repairBuiltinEntityModel`. The 16 `builtin/entity` models split by what there was
left to rebuild from. #21 handled the three NBT-driven icon items; these are the rest:

| Item | What the ISTER drew | Rebuilt as |
|---|---|---|
| `shattered_dimensional_carver` | the eleven `dimensional_carver_shard_*` items stacked, drifting on a sine | `minecraft:composite` of the same eleven models — the stack minus the drift ([`COMPOSITE_SUBSTITUTES`]) |
| `shield_of_the_deep` (+ `_blocking`) | a Java `ModelShieldOfTheDeep`: four cuboids on `textures/armor/shield_of_the_deep.png` | a hand-converted `elements` model, `models/item/shield_of_the_deep_3d.json` ([`REBUILT_MODELS`]), plus a `minecraft:condition` on `using_item` for the blocking pose |
| `end_pirate_anchor`, `end_pirate_anchor_winch`, `end_pirate_ship_wheel` | the block entity — upstream never authored a block model, and pointed each blockstate at a **vanilla** placeholder instead | parent the item model to whatever the blockstate already names (`minecraft:block/crying_obsidian` ×2, `minecraft:block/end_rod`) |

The End Pirate trio is the one that needed new logic. `repairBuiltinEntityModel`'s existing "a block
model exists" branch looks for **the mod's own** `models/block/<id>.json`, which these three do not
have, so all three fell through to the empty default. The new `blockstateModel` branch reads the
blockstate instead. This is exactly what vanilla does for the same two blocks — `assets/minecraft/items/crying_obsidian.json`
and `end_rod.json` are each nothing but a `minecraft:model` pointing at the block model, and both carry
the `display` transforms an item needs. In-world rendering was never affected: that is the BER's job
and it still runs.

Verified by diffing the whole migrated `models/item` + `items` tree on `26.2-fabric` before and after:
**exactly** the three End Pirate models changed, plus the shield's two new files and its definition.
Nothing else moved. Afterwards the only definitions still resolving to an empty model are
`shield_of_the_deep_blocking` and `skelewag_sword_blocking` — inert override targets no item is
registered under (grep-confirmed), unreachable since 1.21.4 deleted `overrides`.

**Not yet checked in a client**, on any of the three.

#### Publishing state after the fourth pass

`2.0.3` shipped to Modrinth (49/49) at 20:13 on 2026-08-03. **Everything in this section landed after
that**, so none of it is in a published jar:

| Change | Edited | Compiled / migrated | In `2.0.3`? |
|---|---|---|---|
| #22 Fabric empty-left-click | 22:51 | ✅ compiled on `26.2-fabric` 22:52 | ❌ |
| #23 carver composite | ≤22:54 | ✅ in the 22:54 migration output | ❌ |
| #23 shield rebuild | 23:03–23:04 | ✅ (verified after the crash) | ❌ |
| #23 End Pirate trio | after the crash | ✅ | ❌ |
| #23 Fabric ISTER wiring | after the crash | ✅ compile-green on all **49** | ❌ |

`2.0.3`'s CurseForge upload never ran either — the ledger holds `2.0.1` and `2.0.2` only. So the
cheapest path is to fold this pass into **`2.0.4`** and ship that to both stores, rather than uploading
a `2.0.3` to CurseForge that is already known to be missing five fixes. Because CurseForge skips
`2.0.3` entirely, `2.0.4`'s changelog **restates the five `2.0.3` fixes** under an "also in this
release" heading — otherwise a CF player updating from `2.0.2` gets no record of them.

**`2.0.4` is built and verified, not uploaded** (2026-08-04): 49 release jars, one per node, no
`-SNAPSHOT`; `verify_mixins` `jars=49 problems=0`; `verify_mixin_targets` `nodes=49 jars=49
selectors=958 problems=0`; `verify_assets` `missing=0`. Both halves of #23 spot-checked in the shipped
jars — `FabricItemRenderers` carries `BuiltinItemRendererRegistry` on `1.21.1-fabric`, is present but
gated empty on `26.2-fabric`, and is absent from `1.21.1-neoforge`; the End Pirate trio's item models
resolve to `minecraft:block/crying_obsidian` ×2 and `minecraft:block/end_rod` on `26.2-fabric`.
⚠️ Rule 9 applies to this build: `versions/*/build/resources` was cleared for all 49 nodes first,
because `DataPackMigration` changed.

[`COMPOSITE_SUBSTITUTES`]: ../../build-logic/src/main/kotlin/DataPackMigration.kt
[`REBUILT_MODELS`]: ../../build-logic/src/main/kotlin/DataPackMigration.kt

### The fifth community bug-report pass (2026-08-04, against published `2.0.3`)

Two reports — the second arriving after its fix was already written — and one thing found while
fixing them. All three are **networking or the data that rides on it**, and the last one is the
largest single gap this port has had.

| # | Report | Root cause | Fix |
|---|---|---|---|
| 24 | opening a kangaroo's inventory crashes the game on NeoForge | `ClassCastException`: `AMCompat.writeItem` casts the buffer to `RegistryFriendlyByteBuf` from 1.20.5 on, but `AMNeoNetwork.wrap` **pre-encoded** each message into a hand-allocated `new FriendlyByteBuf(Unpooled.buffer())`, which can never be that type. Every message carrying an `ItemStack` was affected — the kangaroo's inventory sync (the report), its eat packet, and Citadel's `PropertiesMessage` | the payload now carries the **message object** and encodes late, inside the `StreamCodec`, into the connection's own registry-aware buffer |
| 25 | komodo dragons cannot be tamed — reported against `2.0.3`, **NeoForge 1.21.1** (found here first; the report landed after the fix) | `komodo_dragon_tameables` shipped as an **empty tag** | `minecraft:rotten_flesh`, and a sweep of all 16 `*_tameables` tags |
| — | *(found, not reported)* **no packet has ever worked on Fabric** | `AlexsMobs`' three network hooks had `!fabric` arms only, so on all 17 Fabric nodes they compiled to a bare `return` | `fabric/network/AMFabricNetwork` + `fabric/client/FabricClientNetwork` — [`fabric.md` → Wave 6](fabric.md) |

#### #24 — encode late, or not at all

The fix is one line of design: **a message must be encoded into the buffer the connection hands you,
not into one you allocated.** 1.20.5 made the PLAY protocol's buffer a `RegistryFriendlyByteBuf` —
it carries a `RegistryAccess`, which is what `ItemStack.OPTIONAL_STREAM_CODEC` needs to resolve
components — and there is no way to promote a plain `FriendlyByteBuf` into one after the fact.

Worth noting that **Forge does the same thing and never hit this**: its `ForgePayload` holds the
encoder `Consumer` until vanilla writes the packet, so the encode always lands in the real buffer.
The NeoForge port copied the pre-1.20.5 shape of the wrapper (`{index, byte[]}`), which was correct
when it was written and became wrong at 1.20.5 without a compile error — the cast is inside
`AMCompat`, a version away from the code that broke it.

The mirror change moves **decoding** to the netty thread, where Forge's `SimpleChannel` and vanilla
both already decode. Checked before doing it: no Alex's Mobs decoder touches the level, the client or
any registry beyond the buffer's own, and every handler still hops to the main thread through
`AMNetContext.enqueueWork`.

⚠️ The unknown-index path **had to become a throw**. The old `{index, byte[]}` form could warn and
ignore, because the bytes were a self-contained blob; once the body is decoded inline, the rest of the
buffer is only parseable by the message the index names. An unknown index means the two sides disagree
on registration order, which is a protocol mismatch, so it fails the packet the way vanilla would.

#### #25 — the tameable-tag sweep

Found while reading the komodo's taming code for #24's neighbours; a player report naming the same
symptom arrived afterwards, against published `2.0.3` on **NeoForge 1.21.1**. Confirmed against that
exact artifact — `unzip -p alexsmobs-2.0.3-neoforge+1.21.1.jar
data/alexsmobs/tags/item/komodo_dragon_tameables.json` → `"values": []`, so on the reporter's build
no item can tame the mob. The same jar's `2.0.4` sibling carries `["minecraft:rotten_flesh"]`.

The komodo's tag was the **only** empty one — all 16 `*_tameables` tags now have values,
`verify_convention_tags.py` reports `nodes=17 problems=0`. Upstream shipped it empty, so this is not
a port regression, and there is no in-code default: `EntityKomodoDragon` asks the tag and nothing
else, so an empty tag means "no item tames this mob" with no warning anywhere.

⚠️ **The fix alone will not look like a fix to a player who feeds it one rotten flesh.** Upstream's
`mobInteract` rolls `tameAmount = 58 + random(16)` and tames only when the **held stack count
exceeds** it, then eats the whole stack either way. So fewer than 59 rotten flesh in hand is a
guaranteed failure that still consumes them, and a full 64-stack succeeds 6 times in 16 (~2.7 stacks
on average). That is upstream behaviour and is left alone, but it is worth stating in the changelog
or #25 comes straight back as "still can't tame it".

#### The Fabric networking gap — why a feature-shaped audit missed it

Full write-up in [`fabric.md`](fabric.md) under *Wave 6*; the part that belongs here is what it says
about **reading reports**. Report #22 in the previous pass ("the bald eagle's falconry loop does
nothing on Fabric") was root-caused to a missing `ClientPreAttackCallback` and fixed there — correctly,
that hook really was missing. But the fix ended in `sendMSGToServer`, so #22 was **still broken after
its own fix**, and nothing in the pass could tell, because a boot gate cannot observe a packet.

Two rules out of that:

1. **A fix whose last step is `sendMSGToServer` / `sendNonLocal` is not verified by any gate in this
   repo.** Say so in the report entry, and close it in a client or not at all.
2. **When two reports on the same loader describe unrelated features doing nothing, suspect a shared
   seam.** #22 (falconry), the transmutation table and Wave 4's multipart attacks all looked like three
   separate Fabric gaps and were one.

#### Build state after the fifth pass

**`2.0.4` rebuilt and verified 2026-08-04, not uploaded.** 49 release jars, one per node, no
`-SNAPSHOT`; `verify_mixins` `jars=49 problems=0` (`26.2-fabric declared=28`); `verify_mixin_targets`
`nodes=49 jars=49 selectors=958 problems=0 skipped=0`; `verify_assets` `literals=394 missing=0`;
`verify_convention_tags` `nodes=17 problems=0`. Rule 9 applied — `versions/*/build/resources` cleared
for all 49 first. Boot gate green (`rc=0`) on `1.20.1-fabric`, `1.21.1-fabric`, `26.2-fabric`, one per
networking arm. Spot-checked **in the shipped jars**, not just in source:

| check | result |
|---|---|
| `AMFabricNetwork` arm per era | raw `registerGlobalReceiver` + **no** `AMPayload` class on `1.20.1-fabric`; `playS2C`/`playC2S` on `1.21.1-fabric`; `clientboundPlay`/`serverboundPlay` on `26.2-fabric` |
| `FabricClientNetwork` present | 17/17 Fabric jars |
| `AMFabricNetwork` absent from Forge/NeoForge | 0/32 — the path exclude holds |
| `AlexsMobs#sendNonLocal` reaches the transport | 17/17 Fabric jars call `AMFabricNetwork.sendToPlayer` |
| #24 late encoding | `AMPayload` holds `java.lang.Object message` and `wrap` has no `Unpooled` on `1.21.1`/`26.2-neoforge`. `1.20.4-neoforge` is the only node on the `<1.20.6` arm and keeps the `byte[]` form — correct, it predates `RegistryFriendlyByteBuf` |
| komodo tag | `["minecraft:rotten_flesh"]` in Forge, Fabric and NeoForge jars |

⚠️ **What none of that proves: that a packet arrives.** No gate in this repo sends one. Fabric
networking is shipping on the strength of a bytecode read, and the cheapest real check is #22 —
perch a bald eagle and left-click on a Fabric node.

### The first client session against `2.0.4` (2026-08-04, `1.21.1-neoforge`)

The reporter's exact platform, dev client on a **dedicated** server — a memory connection skips the
packet codec, so singleplayer could not have tested any of this. Results:

| item | verdict | how |
|---|---|---|
| **#25** komodo taming | ✅ closed | `husbandry/tame_an_animal` in the world save, granted 7 s after the two komodos were summoned and 11 s before any other tameable existed |
| **#25** tag resolves at runtime | ✅ closed | `execute if items … contents #alexsmobs:komodo_dragon_tameables` → `Test passed`; the capuchin tag → `Test failed` as a negative control |
| **#24** kangaroo inventory | ✅ closed | GUI opened, sword and chestplate inserted, kangaroo fought with them — **no exception in either log** |
| **#26** kangaroo destroys its weapon | ❌ **new**, now fixed | found by the user in the same session |

**Two RCON techniques worth keeping.** Item tags can be probed server-side with no client at all:
summon an item entity carrying the candidate item and `execute if items entity … contents #tag`.
Always run a **negative control** with a tag that must not match, or a tag that matches everything
reads as a pass. And a mob's inventory can be filled from RCON with `/data merge entity`, which goes
through `readAdditionalSaveData` and fires whatever sync the mod hangs off it — no player input.

⚠️ **`/data merge` did not load the kangaroo's `Items` list** — the stacks came back as
`[{Slot: 0b}, …]` with no `id`/`count`. Unresolved: either the NBT shape was wrong for
`AMCompat.loadItem`, or kangaroo inventories genuinely do not survive an NBT round-trip. Worth a look
before trusting that technique on this entity.

#### #26 — a compat helper that silently does nothing when there is no player

**A kangaroo destroyed its sword on the first swing.** Its armour went the same way. Every node
**≥1.21**, all three loaders — so roughly 40 of the 49. Found in-game; no gate, verifier or compiler
could see it.

Upstream's `EntityKangaroo.damageItem` is this, and it is fine on 1.20.1:

```java
stack.hurt(1, this.getRandom(), null);
if (stack.getDamageValue() <= 0) { stack.shrink(1); }   // "undamageable → consume it"
```

The port routed the first line through `AMCompat.hurtItem`, whose ≥1.21 arm reads:

```java
if (player != null) { stack.hurtAndBreak(amount, player.serverLevel(), player, item -> {}); }
```

1.21 swapped that vanilla call's `RandomSource` for a **`ServerLevel`**, and the only route the
helper had to one was the `ServerPlayer` it is handed. **A mob has no player**, so the kangaroo — the
sole caller passing `null` — got a **no-op**. The damage value stayed `0`, and upstream's next line
read that as "this item cannot be damaged" and destroyed it.

Fixed by routing through the helper the codebase already had for exactly this shape,
`AMCompat.hurtAndBreak(stack, 1, entity, slot)` — it takes the **holding entity**, so it needs no
player and works on every version (`EntityCapuchinMonkey` already used it with a mob). Upstream's
`shrink` retires with it: `hurtAndBreak` breaks the item at zero durability itself. That is a small
deliberate divergence — upstream ignored the return value, so its kangaroo weapons never broke at
all and their damage value climbed past max.

**The lesson, and it generalises past this bug.** A version-gated compat helper that takes a
parameter *only so it can reach something else* will quietly no-op for callers that pass null, and
the compiler sees a correct call. The tell is a `!= null` guard **inside** the helper wrapping the
entire body — it converts a missing argument into silence rather than an error. Grep for that shape.
`hurtItem`'s five remaining callers all pass a real `ServerPlayer`, so they are unaffected — but the
helper is one null away from the same fault, and its `<1.21` arms damage the stack regardless of the
player, so behaviour also **diverges by version** for a null caller.

**Confirmed fixed in-game** on `1.21.1-neoforge` in a second client session the same day, before the
`2.0.4` upload: the kangaroo keeps its sword across repeated swings and the sword takes durability
damage instead of vanishing. So #26 is the one item from the fourth/fifth passes that is fully
client-verified — #16, #21, #22 and #23 are still only compile-and-reason verified.

**Shipped in `2.0.4`** (2026-08-04, Modrinth only, all 49 nodes), together with #22, #23, #24, #25
and Fabric networking.

### The second client session against `2.0.5` (2026-08-04, `26.2-neoforge`)

Started as a check on an unrelated change and found the oldest visible fault in the port.

#### #27 — every mob this mod adds wore its own type name, on every node ≥1.21.2

**Symptom.** A wild, never-named Grizzly Bear renders the label "Grizzly Bear" above it; a wild fly
renders "Fly". Vanilla shows a name only over a mob a player named with a name tag. All three
loaders, every node from **1.21.2** up — roughly 35 of the 49.

**How it hid for so long.** It is not a crash, not a log line, and not a missing texture, so no gate
step and no verifier can see it. And it looks like a *feature*: `2.0.2` had already added
`/aac nameplates` in response to players reporting the names as unwanted, which reframed a bug as a
preference and stopped anyone asking why the names were there. The one reporter whose platform was
known ran `1.21.1` — below the boundary, so their world was correct and their complaint was about
something else entirely.

**Root cause.** `client/render/compat/MobRenderer`, the shim restoring upstream's two-type-parameter
`MobRenderer<T, M>` after 1.21.2 widened vanilla's to three, extends the compat
`LivingEntityRenderer` and so **vanilla `LivingEntityRenderer`, not vanilla `MobRenderer`**. Vanilla
splits the nameplate decision across exactly those two classes: `LivingEntityRenderer#shouldShowName`
answers "is this entity visible to you at all" and returns `true` for any ordinary visible mob;
`MobRenderer#shouldShowName` is the one that ANDs in
`entity.shouldShowName() || entity.hasCustomName() && entity == crosshairPickEntity`. Inheriting
from the wrong side dropped that clause for all **93** of this mod's renderers.

**Fix.** Restore the clause as a `shouldShowName(T, double)` override on the shim. Plus: the legacy
one-arg bridges on compat `EntityRenderer`/`LivingEntityRenderer` delegated with `super.`, which
steps over the new override — changed to `this.`, or `RenderTiger`, `RenderUnderminer` and
`RenderFarseer` (the three that reimplement `render`) would have kept labelling everything.

**Confirmed in-game** on `26.2-neoforge`: wild mobs bare, name-tagged mobs still named.

**Consequence: `/aac nameplates` is deleted**, along with `AMClientSettings`, both registration
seams and its two lang keys. With the fault fixed it could only turn *off* correct behaviour —
including names players applied with a name tag. `2.0.5` had been about to ship the toggle
defaulting to hide, which would have made that a shipped regression.

#### #28 — the nameplate veto never fired on Forge/NeoForge ≥1.21.2

Found while fixing #27. `mixin/renderstate/EntityRendererMixin` captured the entity onto the render
state at `@At("TAIL")` of `extractRenderState`, but that method calls `extractNameTags` partway
through its own body and **that** is where the loader posts `RenderNameTagEvent.CanRender` (verified
in decompiled sources across the range: line ~192 of ~250 on 26.x, ~198 in a method starting at 173
on older 1.21.x). Render states are freshly allocated per entity per frame, so `AMStateAccess.entity`
returned `null` to the hook every time and no veto ever fired.

Scope is **Forge and NeoForge, ≥1.21.2** only — Fabric injects at the draw stage
(`renderNameTag`/`submitNameTag`/`submitNameDisplay`), long after extraction, so its duck was
populated. Fixed by moving the capture to `@At("HEAD")`.

⚠️ **Still load-bearing after `/aac`'s removal**: the surviving reason to veto a nameplate — hiding
the player's own plate while their camera entity is a bald eagle in singleplayer — reads the entity
through the same duck. Before this fix that feature was also broken on Forge/NeoForge ≥1.21.2.

**The rule.** A toggle that only ever turns something *off* deserves one look at why the thing is
on. And `2.0.2` shipped this un-client-verified — had it been checked once, its own inertness would
have surfaced #27 two releases earlier. → [`client-settings.md`](client-settings.md)

### The sixth community bug-report pass (2026-08-05, against published `2.0.5`)

#### #29 — every right-click with a glass bottle crashed the client on Fabric 1.20.1/1.20.4

**Report.** "The game crashes using a bottle", with a full `NullPointerException` from a Fabric
client (`class_1657.method_5996` returned null → `class_1324.method_6194`).

**Reproduce.** Hold a glass bottle on **Fabric 1.20.1 or 1.20.4** and right-click *anywhere*. Not
near lava — anywhere, including filling a bottle at water, which is the ordinary first step of
brewing. Hard client crash, so it takes the world session with it.

**Scope: 2 of 49 nodes**, `1.20.1-fabric` and `1.20.4-fabric`, and present since Milestone 15
brought Fabric up — every release from `2.0.0` to `2.0.5`. The intermediary signature in the trace
is the tell: `method_5996(class_1320)` takes a *raw* `Attribute`, which is the pre-1.20.5 shape, so
the trace alone pins the era before any code is read.

**Root cause.** `ServerEvents#onUseItem` implements upstream's lava-bottle feature and calls the
mod's own copy of vanilla's `Item#getPlayerPOVHitResult`, `ServerEvents#rayTrace`, which read the
block-reach attribute unguarded:

```java
final double d0 = player.getAttribute(AMPlatform.blockReach()).getValue();
```

That is safe on Forge/NeoForge, where the loader adds `ForgeMod.BLOCK_REACH` to every player. It is
not safe on Fabric below 1.20.5: vanilla had no reach attribute at all before then, so
`AMPlatform.blockReach()` **returns `null` by design** on those two nodes and `getAttribute(null)`
answers `null`.

Note the ordering — `rayTrace` runs *before* the lava check, so the config option that nominally
gates the feature (`lavaBottleEnabled`, default on) does not gate the crash, and the crash has
nothing to do with lava.

**Fix.** Null-check both the attribute and the player's instance of it, falling back to **5.0**.
That number is not a guess: vanilla's own `Item#getPlayerPOVHitResult` hardcodes `5.0D` on exactly
these versions — verified in the 1.20.1 and 1.20.4 merged-jar bytecode (`ldc2_w // double 5.0d`) —
and that method is what `rayTrace` is a copy of, so the fallback restores vanilla reach rather than
inventing one.

**Why no gate caught it.** `AMPlatform`'s reach/swim helpers are the port's one family of
*returns-null-on-some-nodes* accessors. The compiler cannot see it, all 49 nodes compile, both
mixin verifiers pass, and the client gate never right-clicks a bottle. The header comment on those
Fabric arms asserted that "every call site null-checks" — it was wrong by exactly one, and that one
was the only call site outside `ItemModArmor`. The comment now says every call site *must*, records
why, and the arms and call sites are cross-referenced.

**The rule.** When a platform helper is allowed to return `null` on a subset of nodes, the count of
its call sites is the whole safety argument — so grep them when adding either a call site or an
arm, and never let a comment stand in for the grep.

### The seventh community bug-report pass (2026-08-05, against published `2.0.5`)

One player, **Fabric 1.21.11**, six reports in one message. Fixing them found four more — two of them
faults that had been shipping invisibly for several releases and were not what any report was about.

| # | Report | Root cause | Scope |
|---|---|---|---|
| 30 | item names show as `item.alexsmobs.banana_peel` | `BlockItem#getDescriptionId` deleted at 1.21.2 | 36 block items, all loaders, ≥1.21.2 |
| 31 | "Gone Bananas" granted on entering a world | `ItemPredicate`'s `tag`/`item` keys dropped at 1.20.5 → matches everything | 2 advancements, all loaders, ≥1.20.5 |
| 32 | crocodile chestplate deletes the player's head | Fabric `ArmorRenderer` registered for items with no model | 3 items, Fabric, ≥1.21.2 |
| 33 | shield of the deep held wrong in first person | fixed, client-confirmed — the **blocking** model's first-person pose, turned face-on so the grip stops filling the frame | 1 item, all loaders, all nodes |
| 34 | laviathan missing from the animal dictionary | per-mob offsets are only coherent at `entity_scale` 1 | 1 mob, all loaders, every version |
| 35 | rainbow glass / bison carpet / triops eggs draw opaque | `render_type` is a Forge extension Fabric ignores | 13 blocks, Fabric, <26.1 |
| 36 | getting close to a frilled shark crashes | `getTarget()` re-read after the bite that cleared it | all nodes, upstream fault |
| 37 | Create Fly stops the server from loading | another mod builds `PotionBrewing` before components bind | all nodes ≥1.20.5, with Create |
| 38 | crocodile chestplate is equipped but invisible | equipment definitions left in the 1.21.2 folder | 15 items, all loaders, ≥1.21.4 |
| 39 | advancement screen draws the missing texture | `display.background` became a bare id at 1.21.5 | all loaders, ≥1.21.5 |

#### #30 — every block item showed its raw translation key

`BlockItem` used to override `getDescriptionId()` to return its *block's* key, which is why this mod
translates all 36 of its block items under `block.alexsmobs.<name>` and ships no `item.alexsmobs.<name>`
key for any of them. **1.21.2 deleted that override and made `Item#getDescriptionId` final**, resolving
the id from a `DependantName` carried on `Item.Properties` and defaulting to the `item.` prefix — so
every one of them asked for a key that does not exist and rendered the key itself. `AMBlockRegistry`
now opts the properties into `useBlockDescriptionPrefix()` on ≥1.21.2; it cannot be done from the
`BlockItem` subclass any more, which is the whole point of the change.

#### #31 — "Gone Bananas" on world entry, and the mantis shrimp bucket on any interaction

**1.20.5 rebuilt `ItemPredicate`**: the mutually exclusive `item`/`tag` pair became one `items` holder
set, `"#alexsmobs:bananas"` for a tag and a bare id (or an array) for items. The trap is that the old
keys are not *rejected* — `ItemPredicate.CODEC` is a record codec of optional fields, so an unknown key
is dropped and what remains decodes as a predicate with **no conditions at all**, which matches every
stack in the game. `alexsmobs:banana`'s `inventory_changed` therefore fired on the player's first
inventory tick, and `alexsmobs:mantis_shrimp_bucket` fired on interacting with a mantis shrimp while
holding anything. Migrated in `DataPackMigration.migrateAdvancement` alongside the icon rewrite, over an
allowlist of the fields that actually hold an `ItemPredicate` (`item`, `items`) because `items` also
names a plain id list elsewhere.

Nothing logs and no gate can see this one: the advancement loads, and it fires — on the wrong thing.

#### #32 — the crocodile chestplate deleted the player's head

`FabricArmorRenderers` registered all fifteen armour items with Fabric API's `ArmorRenderer`. Twelve of
them have a hand-built `Model`; three (crocodile chestplate, centipede and emu leggings) do not, and for
those the renderer fell through to rendering the *context* model — the player's own body — a second time
with the armour texture, which read as the head being replaced. They are now registered only below
1.21.2, where the mod still supplies its own texture path; from 1.21.2 up they are left to the vanilla
equipment layer.

Which is what made **#38** visible.

#### #33 — the shield of the deep is held wrong in first person — **fixed, client-confirmed**

Reported as first person only; the reporter confirmed third person looks right. That one sentence is the
whole constraint, and it is a strong one: **first and third person render the same `elements`**, so no
error in the model geometry can show up in one and not the other. Whatever is wrong lives in the
`display` block, in the blocking-model swap, or in the first-person arm path — not in the fold.

**A geometry theory was tried and falsified — do not retry it.** `models/item/shield_of_the_deep_3d.json`
is `ModelShieldOfTheDeep` plus `AMItemstackRenderer`'s pose folded into element coordinates by hand (see
`DataPackMigration.REBUILT_MODELS`). The theory was that the fold had dropped a half-block, i.e. that the
conversion should be `E = 16 * pose + 8` rather than `E = 16 * pose`, on the reasoning that the ISTER ran
*outside* the `translate(-0.5, -0.5, -0.5)` that an element's own `E/16` is measured inside. Applying the
`+8` broke third person, which had been correct, and left first person no better.

The bytecode says the same thing. In 1.21.11 `ItemStackRenderState$LayerRenderState.submit` contains no
`-0.5` at all; it is inside `ItemTransform.apply`, as the **last** call in the method —

```
apply(leftHand, pose):  translate(translation) ; rotate(rotationXYZ) ; scale(scale) ; translate(-0.5,-0.5,-0.5)
```

— and a `PoseStack` post-multiplies, so the last call written is the first applied to a vertex. It
therefore precedes the display transform and applies equally to a baked model, an ISTER and a
`SpecialModelRenderer`. Both frames are the same frame. `E = 16 * pose`, no half-block term.

Re-deriving the fold from the four `addBox` calls confirms it, and confirms the file was already right:

```
E_x = 6.4 - 16*mx     E_y = -12 + 16*my     E_z = 8 - 16*mz          (16*m = setPos + box coords)
```

reproduces elements 1, 2 and 4 exactly as checked in. Only element 3, the boss, was wrong, by a stray
`+1` on X — `addBox(-4,-1,-3, 3,6,6)` under `setPos(-2,16,0)` gives `9.4 .. 12.4`, the file said
`10.4 .. 13.4`. That one unit is corrected; the rest of the model is back to the coordinates third person
was verified against.

**It is the blocking model, and only the blocking model.** The reporter confirmed the non-blocking pose
is fine and third person is fine; the bad frame is the one captioned *"now i hold right click"*. So the
only thing being changed is `_3d_blocking`'s two `firstperson_*` entries — and since these numbers are
unchanged from Alex's Mobs 1.22.9, where the ISTER obeyed them identically, they were **equally bad on
1.20.1/Forge**. This is a deliberate divergence from upstream, not a port repair, so the same edit goes
into `shield_of_the_deep_blocking.json` (the `<1.21.4` model) and all 49 nodes get it.

**A first-person pose can be computed, not guessed.** Three measured screenshots (vanilla shield blocking
and not, ours not blocking) each landed within ~0.1 block of this model, so use it rather than burning a
25-minute client launch per guess:

```
v_final = arm + T + R · ( E/16 − 0.5 )          arm = (±0.56, −0.52, −0.72)
R = rotationXYZ(rx, ry, rz)                     JOML: X first, then Y, then Z
ndc_y = Y / (|Z| · tan(fov/2))                  fov is VERTICAL, 70° at options.txt `fov:0.0`
ndc_x = X / (|Z| · tan(fov/2) · aspect)
```

`T` is the `display` translation ÷ 16. Screen centre is `(0,0)`, edges `±1`. Two traps in
`ItemTransform.apply`: for the **left hand it negates `translation.x`, `rotation.y` and `rotation.z`** —
so mirroring a right-hand pose means keeping the *same* sign on the JSON `x`, not flipping it (a flipped
`x` moves the item further out to the left instead of in toward the centre, and a shield 0.75 blocks wide
leaves the frame entirely) — and the `translate(-0.5,-0.5,-0.5)` is a **first**-applied term, so it does
not interact with `T` at all.

**The actual fault was `rotation.y`, and four attempts at moving the shield could never have fixed it.**
This mod's shield is held **handle-toward-the-camera**: the handle element occupies model x
`−0.2875 … 0.025` and the plate only `0.025 … 0.0875`, so upstream's `rotation [_, 89, 0]` — near
edge-on — puts the grip about 0.29 blocks *nearer the eye* than the face it is supposed to be behind.
Vanilla's `ShieldModel` is built the other way round, its handle at computed `|Z|` 0.70–1.17 against a
plate at 0.63–0.70, which is why a vanilla shield can be held at `y 180` and never show its grip. Raise
this one and the grip is what fills the frame — the coral-orange slab in the reporter's screenshot, which
is the only warm-coloured region of the texture (69,594 warm pixels while blocking against 528 while
not). It reads as *"the model is turned"*, so every remedy aimed at the rotation the player thought they
saw made it worse.

The settled pose turns the plate **face-on** and buys back the lost coverage with distance and size:

```
_3d_blocking, firstperson_righthand :  rotation [0, 0, 10]   translation [6, 7, -6]   scale [2, 2, 2]
```

`rotation.y 0` puts the plate between the grip and the eye, `z −6` pushes the whole thing 0.375 blocks
further out so the near clip is nowhere near it, and `scale 2` restores the screen coverage that
retreating cost. The `10°` of roll and the extra unit of height came from a later in-client pass. The non-blocking pose ended up **unchanged** at upstream's `rotation [0, 89, 0]`,
`translation [2, -1, 0]` — at rest the shield hangs low enough that the grip never enters frame, and
every attempt to "improve" it was rejected. Third person is untouched throughout.

Confirmed in-client on `1.21.11-fabric`, 2026-08-05.

**The off-hand needed a second fix, and it is a different fault** (2026-08-06). Blocking with the
shield in the *left* hand still showed the spikes edge-on toward the player, with `rotation [0,0,0]` —
so nothing in the display block could be negating anything. The cause is vanilla's own **`case BLOCK`
arm transform for non-`ShieldItem`s, added in 1.21.4**, whose translate and three rotations are all
multiplied by `invert` and therefore **mirrored between hands** before the display block is reached.
That is also why the checked-in `<1.21.4` blocking model was wrong in a second, unreported way: it had
been carrying numbers tuned under a transform that does not exist on those ~20 nodes. Both pose tools
now **solve** each of the four first-person entries out of one authored world pose rather than copying;
the derivation, the exact 1.21.4 boundary and the resulting numbers are in
[`client-settings.md`](client-settings.md).

Confirmed in-client on `1.21.11-fabric`, 2026-08-06: blocking left-handed looks the same as
right-handed. ⚠️ That closes the `>=1.21.4` half only. The `<1.21.4` model's two entries were solved,
never seen — and they are the pair that was *separately* wrong before, so they are the ones with no
prior confirmation of any kind. `1.21.1-fabric`, the reporter's own node, is the one to use.

**The `<1.21.4` half closed 2026-08-07** — the user ran `1.21.1-fabric`, re-tuned the blocking pose
live with `/shieldpose` and confirmed it in-game. The authored world pose moved, so the solver
rewrote **both** era files: `<1.21.4` blocking is now `translation [0,0,0]` both hands (rotations
kept at `±88.05`), and the `>=1.21.4` blocking translation moved `[6,7,-6] → [2.9217,-1.814,-0.2268]`
as the re-solve of the same pose. ⚠️ That means the `>=1.21.4` numbers confirmed on `1.21.11-fabric`
2026-08-06 are no longer the shipped numbers — they derive from the newly-confirmed world pose, but
nobody has re-looked at a `>=1.21.4` client since. Shipped in `2.0.8`.

⚠️ **Do not settle a first-person pose by guessing between client launches** — four blind attempts across
two sessions each cost a relaunch and none converged. What worked in one session was making the pose
adjustable *from inside the running game*: the model JSON is a seam this port already owns, so a command
that rewrites `display.firstperson_*` and then calls `Minecraft#reloadResourcePacks()` (what F3+T does)
needs **no mixin and no render internals**, which is what makes it portable across every era in the tree.
Two tools now do this and share one format: **`scripts/shieldpose.py`** from a shell, and
**`/shieldpose`** (`client/command/AMShieldPoseCommand`) from inside the client with no alt-tab. Both
write all four model files plus every node's `build/resources` copy; the script additionally prints the
projected on-screen box from the model above, and the command self-disables outside a checkout so it
never ships. → [`client-settings.md`](client-settings.md).

**The rule that does hold.** A fault that appears in one display context and not another is in the
`display` block by construction. Check which context the screenshot is of before touching geometry.

#### #34 — the laviathan's dictionary slot was empty

`EntityLinkButton` anchored each icon at a fixed point and then nudged it by the per-mob `offset_x` /
`offset_y` from the book JSON, each multiplied by that mob's `entity_scale`. Those offsets are only
coherent at `entity_scale` 1; the laviathan asks for `(-65, -28)` at scale `0.8`, i.e. 52 px left of an
anchor 11 px inside a 24 px frame, so it landed entirely outside the scissor window. Centre the mob from
its own bounding box and ignore the offsets, as `AlexsMobsFP` already does. That also drops upstream's
second error here — the frame scale `f` was multiplied into the entity size *and* applied again by the
enclosing `scale(f, f)`, squaring it.

#### #35 — thirteen blocks drew opaque on Fabric

`"render_type"` in a block model is a **Forge/NeoForge extension**, not vanilla. Fabric parses the model,
finds a key it does not know and ignores it, so the block falls back to the `SOLID` chunk layer and every
texel draws opaque — rainbow glass became a solid pane, the bison carpet and triops eggs grew a black
border. New `FabricBlockRenderLayers`, called from `ClientProxy#clientInit`'s Fabric arm, across three
measured Fabric API eras; **≥26.1 needs nothing**, because 26.x derives a quad's layer from the sprite's
own pixels (`SpriteContents.computeTransparency`) and deleted the per-block mapping entirely.

#### #36 — approaching a frilled shark crashed the server tick

Upstream calls `getTarget()` four times across the bite, and the bite itself can clear it — a target that
dies, or one whose hurt handler makes the shark re-evaluate — so the reads *after* `AMCompat.hurt` came
back `null` and `addEffect` threw. Read the target once into a local; nothing there needs the live value.
**Upstream fault, not a port regression.**

#### #37 — Create Fly stopped the server from loading

`AMEffectRegistry.registerBrewingRecipes` is called by the loader's brewing-registration event, at a point
where the item registry's data components are bound. But nothing stops another mod building a
`PotionBrewing` of its own, and Create (the Fly port) does exactly that from `RecipeManager.prepare`, on a
worker thread, before binding. Every `createPotion` then died inside `ItemStack`'s constructor with
*"Components not bound yet"* — under the datapack reload, so the server refused to start. A mod that never
touched Alex's Mobs made Alex's Mobs fatal.

Wrapped in a try/catch that skips the early pass and warns once. Skipping costs nothing: the real
bootstrap runs later with everything bound and rebuilds the list from scratch. On Fabric the recipes go
into a global list rather than into the builder, so a run that died part-way also resets it.

#### #38 — fifteen armour pieces were invisible from 1.21.4

Found while re-testing #32: the head no longer disappeared, but the chestplate was not there either.
`migrateEquipmentTo12102` writes equipment definitions to `assets/<ns>/models/equipment/<id>.json`, which
is the **1.21.2** path — **1.21.4 moved them to `assets/<ns>/equipment/<id>.json`** (verified by listing
the shipped client jars: 1.21.2 has only the first, 1.21.4 only the second). New
`relocateEquipmentTo1214`, run after the 1.21.2 pass.

The failure mode is the worst kind: an `asset_id` that resolves to nothing does not warn and does not
draw a missing texture — **the layer is skipped and the piece is simply invisible**. On Fabric only the
three model-less items were affected, because `FabricArmorRenderers` draws the other twelve itself. On
**Forge and NeoForge all fifteen were invisible** on every node ≥1.21.4 — 20 of 49 nodes — and nobody
reported it, because the mod's armour looks like an ordinary missing cosmetic.

#### #39 — the advancement screen's background was the missing texture

Spotted by the user in the same client session. **1.21.5 changed `display.background` from the texture
file to a bare id** — `minecraft:gui/advancements/backgrounds/stone`, expanded by the client into
`textures/<path>.png`. Read out of `story/root.json` in each shipped jar, not recalled: 1.21.4 has the old
form, 1.21.5 the new one. This mod's tab still named `alexsmobs:textures/advancement_background.png`, which
expands to `alexsmobs:textures/textures/advancement_background.png.png` — the exact string the client
logged as missing. A missing background is not fatal, so the tab just drew magenta-and-black behind every
Alex's Mobs advancement from 1.21.5 up. `migrateAdvancementBackgroundsTo1215` inverts the client's
expansion, and leaves a value already in the new form alone.

**Both #38 and #39 are the same rule the notes already carry** ([`build-harness.md`](build-harness.md)):
when a version changes how a *resource* is addressed, assume the mod is wrong. Neither is visible to the
compiler, to either mixin verifier, or to the client gate.

### The eighth community bug-report pass (2026-08-06, against published `2.0.7`)

Two items from the user, unrelated to each other.

#### #40 — a crash on entity construction, only with Moonrise installed

Reported as *"I was in a cherry biome and the game crashed when a tiger came near me"*. **The narrative
was wrong and following it would have wasted the session** — the log holds exactly one crash and it is
`Description: Rendering screen` / `Screen name: GUIAnimalDictionary`, thrown from `Entity.<init>`. The
biome, the tiger and the proximity are all coincidence; the dictionary screen builds preview entities.
Diagnose from the stack, not the prose.

Root cause: **Moonrise's chunk-system patch calls `canBeCollidedWith()` from inside `Entity.<init>`**, via
`ChunkSystemEntity#moonrise$isHardCollidingUncached`, to cache a hard-colliding flag. Its injection lands
*before* the constructor assigns `entityData`. Five of this mod's nine `canBeCollidedWith` overrides read
synched state (`isAlive()` reads health; `EntityStraddleboard.isRemoveLogic()` reads `REMOVE_SOON`
directly), so each dereferenced a null `entityData` and threw.

Verified rather than assumed: `javap -c` on `minecraft-merged-deobf-26.2.jar` shows **zero**
`canBeCollidedWith` call sites in `Entity.<init>` and the `entityData` putfield at offset 380 — i.e.
vanilla never asks this question that early, so the overrides were reasonable and only Moonrise breaks
them. Fixed with `AMCompat.isFullyConstructed(entity)` (`entity.getEntityData() != null`) guarding all
five. **Returning `false` that early is not a behaviour change**: `LivingEntity` assigns health in its own
constructor, later still, so an `isAlive()` that survived would have read zero and answered `false` anyway.

⚠️ This guard belongs on **every** `canBeCollidedWith` override that reads entity state. Nothing in the
compiler or the 49-node gates can see a missing one — it needs Moonrise installed to reproduce. The three
overrides left alone (`EntitySeaBear`, `EntityLaviathanPart`, `EntityGiantSquidPart`) return constants or
read a plain field, so they are already safe.

#### #41 — the kangaroo wore its chestplate like a scarecrow, on every node ≥1.21.2

Reported with a screenshot. **The port had dropped the fix on purpose and said so in a comment** — the
`>=1.21.2` `renderArmorPiece` posed a *neutral* `HumanoidRenderState` and left placement to the
`PoseStack`, calling the lost arm-matching an "accepted tradeoff" (the sibling `AlexsMobsFP` made the same
call, so it was no help as a reference). Upstream does considerably more: it lays the `body` part flat
along the torso (`xRot` 90°, `y` 0.25, `z` −7.6), moves both arms onto the kangaroo's own `arm_left` /
`arm_right` pivots (with a `−4 + sitProgress × 0.25` y-offset and a `−0.5` z-offset), and then draws
**twice** — the arms alone, then the body alone stretched `1.1 × 1.65 × 1.1`. Without any of that the
chestplate renders as a default biped: a floating plate and two arm boxes at humanoid positions.

Restoring it is not a copy-paste, because **1.21.9 split the render pipeline underneath this call** and
both halves of upstream's technique land on the wrong side of it:

| | 1.21.2 – 1.21.8 | ≥ 1.21.9 |
|---|---|---|
| `EquipmentLayerRenderer#renderLayers` | calls `Model#renderToBuffer` immediately | only **submits** `(model, state)` |
| when the parts are posed | never — whatever you set stands | `ModelFeatureRenderer` re-runs **`setupAnim(state)` at flush** |
| when `part.visible` is read | at the call | at flush |
| the `PoseStack` | applied at the call | `poseStack.last().copy()` snapshotted at submit |

So on ≥1.21.9 a pose written onto the model before the call is **overwritten**, and toggling `visible`
between two submits of the same instance leaves both flushes seeing the last value. Only the pose stack
survives untouched. All three facts read out of the decompiled 26.x sources, and the immediate-era
behaviour confirmed by `javap` on the 1.21.4 and 1.21.8 jars — not recalled.

The fix makes both halves flush-safe, in one source that suits every era:

- The pose travels **in the render state** — `KangarooArmorState extends HumanoidRenderState` carries the
  twelve captured arm values — and is applied by an overridden **`setupAnim`**. That is the one hook that
  runs on both sides of the split: immediately when `renderArmorPiece` calls it, and again at flush.
- Each pass owns **its own baked model instance** (`head` / `arms` / `body`), so `visible` is set once in
  the constructor and cannot be clobbered. `visible` lives on `ModelPart`, so this needs three real
  `bakeLayer` calls, not three references to one.
- The state is allocated **per render call**, not shared. Vanilla gets away with one render state per
  entity per frame because `EntityRenderDispatcher.extractEntity` calls `createRenderState` fresh for each
  entity; a single shared state here would hand every kangaroo on screen the *last* one's arms once the
  flush is deferred.

The three `renderLayers` call sites are deliberately written with the **same `armorModel` / `armorState`
local names** so the existing `!mc2109-equip-renderlayers-kangaroo` rule covers all three with one
replacement and still inverts cleanly (Stonecutter replaces every occurrence; three identical strings
reverse identically). That rule's key had to move off `defaultBipedModel`, which now serves only the
`<1.21.2` path.

⚠️ Not yet client-verified — it needs a kangaroo wearing a chestplate on a ≥1.21.2 node.

### The ninth community bug-report pass (2026-08-07, against published `2.0.8`) — OPEN, waiting on the reporter's log

#### #42 — "on NeoForge 26.2, opening the second page of the dictionary crashes" — NOT REPRODUCIBLE on 2.0.8

Reported the day `2.0.8` shipped, and the shape is exactly #40's: the second **index** page of the
animal dictionary holds 50 mobs including four of the five `canBeCollidedWith`-guarded ones
(giant squid, laviathan, rocky roller, terrapin — none are on page one), plus the #41 kangaroo. The
reporter says they are **on 2.0.8 with Moonrise installed** — which is the configuration #40's fix
was for. Everything checked from here says that fix shipped and holds:

- The uploaded `2.0.8+26.2-neoforge` jar (version `QQW1U3G8`) is **byte-identical** (sha256) to the
  local build, and `javap -c` on its `EntityLaviathan` shows `AMCompat.isFullyConstructed` at the
  head of `canBeCollidedWith(Entity)` — note 26.2 changed the signature to take an `Entity`, and the
  port tracked it.
- Moonrise 1.1.0 for NeoForge 26.2 (Modrinth `moonrise-opt`, file
  `Moonrise-NeoForge-1.1.0+87549dd.jar`) was itself disassembled: the **only** method it calls inside
  `Entity.<init>` is `canBeCollidedWith(null)` via `moonrise$isHardCollidingUncached` (plus
  four vanilla-class `instanceof` checks). Every one of our nine overrides is guarded or constant.
- **Two live dev-client sessions on `26.2-neoforge`** (2026-08-07, user driving): the second index
  page rendered fine **without** Moonrise (ruling out a #41-kangaroo regression on this path), and
  fine **with** Moonrise loaded in `run/mods` (its chunk system visibly active in the log). Zero
  exceptions in either log, no crash report written.

So the crash does not reproduce. Open explanations, most likely first: (1) **the reporter is not
actually running 2.0.8** — a launcher that kept the 2.0.7 jar reproduces #40 verbatim; (2) another
mod in their pack (Moonrise usually arrives inside performance packs with five other patchers);
(3) a genuinely different crash on the same page. All three are settled by the same ask: **the
newest file in their `crash-reports/` folder** (or `latest.log`) — the header names the loaded mod
version and the stack names the class. If the header says 2.0.7, the answer is "update for real".

The test Moonrise jar was moved to `/tmp/amc-moonrise/` so it cannot contaminate a client gate;
re-fetch:
`curl -sLO https://cdn.modrinth.com/data/KOHu7RCS/versions/3LUeezsG/Moonrise-NeoForge-1.1.0%2B87549dd.jar`
and drop it in `versions/26.2-neoforge/run/mods/`.

### The tenth community bug-report pass (2026-08-07, against published `2.0.8`)

Three items from one reporter, relayed in translation ("squeaker" = skreecher, "wasp wing elytra" =
tarantula hawk elytra). **The reporter's MC version and loader are unknown** — the only artefact that
came with the report was a `connect 148.251.154.155:27027` line, so they play on a multiplayer
server, which matters for #43.

#### #43 — "when a squeaker sees us, it doesn't spawn the Warden" — NOT REPRODUCIBLE

The port is faithful: `EntitySkreecher` diffed against the `151e36c` baseline shows only compat
seams (`AMCompat.create`/`difficultyAt`/`getBoolean`, era-gated `defineSynchedData`/
`finalizeSpawn`), and the biome tag `alexsmobs:skreechers_can_spawn_wardens`
(`{"values": ["minecraft:deep_dark"]}`) is present in the built jars of every node sampled.

Reproduced the **whole chain working, headlessly, on both era arms**. The chain has exactly two
gate families — `AMCompat.create` splits at `1.21.2`, `EntitySkreecher`'s two gates both at
`1.20.5` — so runs on `1.21.11-fabric` and `1.20.1-fabric` cover every arm (the summon tail itself
is pure common code with no loader seams; no third bracket needed). Both runs: dev server + RCON,
sculk arena `fillbiome`d to deep_dark, NoAI villager as bait, skreecher given a target via
`/damage <skr> 1 minecraft:mob_attack by <villager>` (sets the attacker through `HurtByTargetGoal`,
bypassing the Player-class targeting requirement) — skreecher approached to 2.5 blocks, clapped
~5 s, `SummonedWarden` flipped to `1b`, a Warden spawned and slew the villager. Rig notes (traps
included: `hurt()` adds a 10–20 s `ClingCooldown` before the approach; read `say` markers from the
**server log**, not the RCON body) are in `gates.md` "Testing AI headlessly over RCON".

What a player most plausibly hits, in order: **(1) the biome gate** — the summon checks the biome
at the ground **below** the skreecher against the tag, so a skreecher lured or dragged out of the
deep dark never summons, by design (upstream behaviour, identical); **(2) the one-shot flag** —
each skreecher summons at most one Warden, ever (`hasAttemptedWardenSpawning`, persisted as
`SummonedWarden`), so a previously-triggered skreecher looks broken; **(3) a Warden already
nearby** vetoes the summon; **(4) server-side settings on their multiplayer server** (peaceful
difficulty or a mob-control mod would eat the Warden). **Blocked on the reporter**: need their MC
version, loader, and whether the skreecher was standing over deep_dark when it clapped.

#### #44 — the tarantula-hawk elytra never glides — fixed on all 49 nodes (it worked on 9)

The report is real and **understated**: the wing has only ever glided on **Forge/NeoForge
< 1.21.2** — 9 nodes of 49. Two separate holes with one symptom:

- **≥ 1.21.2, every loader (31 nodes):** 1.21.2 deleted the `canElytraFly`/`elytraFlightTick` item
  hooks in favour of the vanilla `minecraft:glider` data component. NeoForge removed the hooks
  outright; **Forge still declares them but no patched class calls them** — verified by sweeping
  every ≥1.21.2 Forge patched jar's bytecode (⚠️ `grep` on `.class` files needs `-a`, or binary
  detection silently returns nothing — that nearly produced the opposite conclusion). The port
  kept the overrides, which compile fine and are simply never called; nothing ever attached the
  component. This is the same *silently-dead-API* shape as rule 10's signature changes, one level
  up: the hook survives the compiler because it still exists, it just has no caller.
- **Fabric < 1.21.2 (5 nodes):** no item hook ever existed; the seam is Fabric API's
  `EntityElytraEvents.CUSTOM`, which nothing registered. The `fabric.md` divergence row recorded
  this half but wrongly assumed the component covered ≥1.21.2.

The fix, four seams: (1) `AMCompat.glider(props)` attaches `DataComponents.GLIDER` at registration
on ≥1.21.2 — vanilla then drains durability itself and stops the glide when the next damage would
break the item, which is exactly `isUsable`'s rule, and the chest `EQUIPPABLE` component the glide
check also needs comes from the armor-item constructor; (2) `AMItemRegistry.TARANTULA_HAWK_ELYTRA`
wraps its properties in it; (3) the two hook overrides in `ItemTarantulaHawkElytra` are narrowed to
`!fabric && <1.21.2`, the only place they are alive; (4) `AlexsMobsFabric` registers the
`EntityElytraEvents.CUSTOM` handler under `//? if <1.21.2`, mirroring `elytraFlightTick`'s
20-flight-tick drain (via `AMCompat.hurtAndBreak`, the #26 seam) plus the `ELYTRA_GLIDE` game event
every 10 server ticks — vanilla and `FabricElytraItem` both emit it (read from bytecode); it is
what lets sculk sensors hear a glider.

Compile-green on 7 era-spanning nodes in one invocation (`1.20.1-forge`, `1.20.1-fabric`,
`1.21.1-neoforge`, `1.21.1-fabric`, `1.21.3-neoforge`, `1.21.11-fabric`, `26.2-neoforge`) —
covering both hook arms, both Fabric event arms, both glider arms and the 26.x fork.

**Both arms are now verified (2026-08-08).** The ≥1.21.2 glider-component arm was flown in a real
client on `26.2-fabric` (campaign session 1, below). The Fabric <1.21.2 event arm was settled
**headlessly** on `1.21.1-fabric` — gliding does *not* in fact need a player, which is worth
keeping:

> **Any `LivingEntity` is a glide probe.** `updateFallFlying` runs from `aiStep` for every living
> entity, re-validating shared flag 7 against the chest slot each tick, and `LivingEntity` saves
> that flag as the boolean NBT tag `FallFlying`. So: `/summon zombie … {ArmorItems:[{},{},{id:…}]}`,
> force the flag on with `/data merge entity … {FallFlying:1b}` (`readAdditionalSaveData` sets
> shared flag 7 from that tag), and poll `/data get`. Fabric API's elytra mixin injects *inside*
> the already-flying branch, so an NBT-set flag satisfies its precondition exactly as a player's
> jump does. Run three arms — a vanilla elytra, an iron chestplate and ours:
>
> | arm | `FallFlying` after 8 s | x travelled | y lost | `minecraft:damage` |
> |---|---|---|---|---|
> | `minecraft:elytra` | `1b` | 407 | 69 | 15 |
> | `minecraft:iron_chestplate` | `0b` (cleared on tick 1) | 3 | 219 (hit the ground) | — |
> | `alexsmobs:tarantula_hawk_elytra` | `1b` | **401** | **69** | **15** |
>
> The durability column matters as much as the distance: on Fabric <1.21.2 the drain is *our* code
> (the handler calling `AMCompat.hurtAndBreak` every 20 flight ticks — the seam #26 was a bug in),
> not vanilla's, and it tracks the vanilla elytra exactly. The **same probe re-run on
> `1.21.3-neoforge`** gives the same three rows (401/408 blocks, damage 15 on both wings), which
> covers the ≥1.21.2 `minecraft:glider` component arm on a non-Fabric loader.
>
> Script: **`scripts/glidetest.py <rcon-port>`** (with `scripts/rcon.py`). Three traps, each of which produced a
> false negative first: **`NoAI:1b` stops the mob ticking altogether**, so every arm reads as
> "did not glide"; an entity in an **unloaded chunk** neither ticks nor answers a selector, which
> is indistinguishable from the flag being cleared, so force-load the whole glide corridor
> (`forceload add -32 -32 400 64`) and remember a glider covers ~50 blocks/s; and `data get
> <path>` echoes **only the value** (`… entity data: 1b`), never the key, so a parser looking for
> `FallFlying:` matches nothing. `kill @e[type=!player]` also only reaches loaded chunks — a
> survivor from an earlier run at an unloaded coordinate later read as a passing arm.

#### #45 — feature request: the animated cycling mob on the creative-tab icon, on ≥ 1.21.4

Not a bug. The tab icon animates today on all **19 nodes < 1.21.4** — the ISTER
(`AMItemstackRenderer`, cycling through `AMMobIcons` every 40 ticks) on Forge/NeoForge, and the
same renderer through Fabric API's `BuiltinItemRendererRegistry` since #23 on Fabric. On the **30
nodes ≥ 1.21.4** the ISTER concept no longer exists (#21) and the icon is the static rebuilt
model. The reporter's "official version" is 1.20.1 Forge, where it animates.

Restoring it on ≥1.21.4 was assessed and **deferred** — it is feasible but disproportionate:
(a) the modern seam is a client item definition of type `minecraft:special` backed by a
`SpecialModelRenderer`, whose registration is per-loader (NeoForge event; Fabric API exposes
`SpecialBlockRendererRegistry` but **no item equivalent** — measured against the 16.2.x
`fabric-rendering-v1` jars — so Fabric needs the vanilla ID-mapper or a mixin); (b) from 1.21.6
GUI items pre-render into the cached `GuiItemAtlas` keyed on render state, so an animated icon
must defeat that cache or route through `SpecialGuiElementRegistry` — era-specific GUI plumbing;
(c) the 1.21.9 submit-split (#41's table) and the 26.x fork GUI rewrite make it 4–5 distinct
rendering eras, each needing an in-client check on the user's GPU. All cost, cosmetic payoff;
revisit only if the user asks for it.

#### Second message from the same reporter (2026-08-07) — platform revealed: **26.2, running 2.0.7**

Eight items, and the first one gives the game away: *"blood sprayer, hemolymph blaster, grappling
squok, dimensional carver … do not play an animation, rather moving as if they were just picked up
again"* is **verbatim the `startUsingItem` fault that `2.0.8` fixed** — so the reporter is a
version behind, and *everything* in both messages needs re-testing against the current release
before further diagnosis. Their loader is still unknown. Triage:

| Item | Verdict |
|---|---|
| Usable weapons/tools dead, no animation | **Already fixed in `2.0.8`** (the startUsingItem restoration). Reply: update. |
| "Hunter Wasp wing elytra" doesn't fly | **#44**, fixed in tree this pass — 26.2 is the glider-component arm. Independent second report of the same fault. |
| Mobs sink into grass blocks, then climb out | **#46 — NEW, open.** Vague; could be pathfinding clip (vanilla-ish), the Fabric `ICustomCollisions` `collide` injection (Wave 4) misfiring, or a 26.x position-sync artefact. Needs their loader + which mobs + whether it survives `2.0.8`. |
| Skreecher's summon lacks the "beating hearts" effect | **#47 — NEW, open.** The clap effect is `ParticleSkulkBoom` (`SKULK_BOOM`), and it **is** registered (`ClientProxy:673`, `registerSpecial`) — so if it is missing on 26.2 the suspect is the fork's particle-rendering rewrite silently not drawing custom "special" particles, which no gate can see. Needs a 26.2 client with eyes on a clapping skreecher. **Note this also reframes #43**: what the reporter calls "doesn't spawn the Warden" may actually be "the clap has no visible effect" — the headless proof that the summon itself works stands. |
| Advancement icons show egg textures, not animated mobs | **#48 — same root as #45.** The 59 advancement icons are `fancy_item`/`effect_item`/`tab_icon` — pure-ISTER items (see `citadel.md`); ≥1.21.4 shows #21's static stand-in models. Working as designed post-#21; restoring live entities is the same deferred ISTER-successor work as #45. |
| Creative tab icon is "in dictionary form", not animated | **#45**, assessed this pass, deferred. Second vote for it. |
| Advancement screen runs at 25–30 FPS | **#49 — NEW, open.** Needs profiling on a 26.2 client; possibly the same static stand-in models (many large textures), possibly the fork's GUI path. No hypothesis worth writing until measured. |
| Shoebill flaps its legs in flight, upstream doesn't | **#50 — NEW, open, minor.** Animation divergence in `ModelShoebill`/`EntityShoebill`; needs a diff against the `151e36c` baseline animation code before it's even confirmed as a port fault. |

The four genuinely new items (#46, #47, #49, #50) all need either the reporter's loader/repro
details or a 26.2 client session, so none was root-caused in this pass.

### The eleventh community bug-report pass (2026-08-07 → 08-08, against published `2.0.8`)

Five items from the same reporter as the tenth pass (platform still 26.2, loader unknown), plus the
user's greenlight on #45 — *"yeah I want the animated icons"* — which un-defers it; the
implementation is its own section below.

#### #51 — "leafcutter ants are in the water, and some get lost and don't return to their anthill" — NO DIVERGENCE FOUND

`EntityLeafcutterAnt` and its goals diff clean against the `151e36c` baseline apart from the usual
compat seams; the water-avoidance and home-position behaviour are upstream's own (`PathNavigation`
setup and `restrictTo` radius unchanged). Upstream leafcutter ants are genuinely bad swimmers and
do wander off the leash radius when path-blocked — likely upstream behaviour, not a port fault.
**Blocked on the reporter**: a seed/coords repro or a screenshot of "lost" ants would let us
distinguish "upstream AI being upstream" from a real navigation regression on 26.2.

#### #52 — the bison wool rug triggers sculk sensors — FIXED in tree

Real, and an *upstream* gap this port can close cheaply: vanilla wool blocks and carpets are in
`minecraft:dampens_vibrations` (blocks) and the matching item tag, which is what makes wool
sculk-silent. Upstream Alex's Mobs never tagged `bison_fur_block`/`bison_carpet` — wool by any
honest reading — so stepping on or placing the rug pings sensors. Fixed with two data files:
`data/minecraft/tags/{blocks,items}/dampens_vibrations.json` (`replace: false`, the two ids). The
`tags/blocks` → `tags/block` (and `items` → `item`) folder rename on ≥1.20.5 nodes is
DataPackMigration's existing rename pass; verified to land in the projected resources with the #45
build. Deliberate, tiny divergence from upstream — same category as #33's pose fix.

#### #53 — a spawn-egg "square particle" on an unnamed "barrier mob" — FIXED in tree (it's the farseer)

Unblocked by the user's screenshot (2026-08-08): the "barrier mob" is the **farseer**, and the
square is its **emergence portal** drawn as a giant opaque square of TV static. The screenshot's
UI chrome also identified the reporter's platform: **PojavLauncher on Android** (Java Edition via
a mobile GL translation layer — worth remembering for their other render reports, e.g. #47/#49),
on 26.2.

**Root cause — the ≥1.21.5 `STATIC_*` fallbacks lost the mask.** Upstream draws every "static"
effect in two merged passes (`AMRenderTypes.renderMerged`): a shaped mask texture via
`entityTranslucent` that writes depth, then `static.png` re-drawn over the same geometry through a
custom composite (`STATIC_PORTAL`/`STATIC_PARTICLE`/`STATIC_ENTITY`) whose **EQUAL depth test**
clips the static to the mask's opaque pixels and whose glint-texturing shard scrolls it. 1.21.5
removed the custom-composite seam, and the port's fallback arm made all three
`RenderType.entityTranslucent(static.png)` — no depth mask, no scroll. `static.png` is 100%
opaque, so the farseer's emergence portal (`portal_0..3.png`, shaped masks 9%→55% opaque, drawn on
a camera-facing quad scaled 3×) rendered as the whole quad: a ~3-block square of frozen noise.
Shipped since `2.0.0` on all **27** nodes ≥1.21.5, every loader; below 1.21.5 the composite
survives and nothing was ever wrong.

**Fix — bake the mask into the texture; era-gate inside two AMRenderTypes helpers, not at call
sites.** `scripts/bake_static_textures.py` generates static noise masked by each shape's alpha
(deterministic crops, no RNG): `portal_static_{0..3}_{0..3}.png` (4 noise variants per portal
frame) and `static_spark_{0..7}_{0..3}.png` (from vanilla's `generic_N` poof masks — upstream
reads those from the *minecraft* namespace). Two new helpers carry the only gates:

- `renderStaticMasked(source, staticType, maskTex, bakedTex, geometry)` — quad-geometry sites
  (emergence portal, `ParticleStaticSpark`): `<1.21.5` = upstream's merged pair; `≥1.21.5` = one
  `entityTranslucent(bakedTex)` pass. Callers cycle the baked variant every 2 ticks
  (`animationTick/2 % 4`, `age/2 % 4`) to stand in for the scroll shard's flicker.
- `renderStaticOverlay(source, staticType, shaped, geometry)` — model-geometry sites (farseer
  eye/scars, transmutation-table overlay, shattered void-portal arc), where the fallback would
  z-fight opaque noise over the model: `≥1.21.5` simply drops the static pass. Clean degrade;
  bake per-site later if anyone misses the effect.

Because the `<1.21.5` static pass is opaque and EQUAL-depth-clipped to the mask, the baked
textures are invisible on old nodes by construction — no asset era-gating needed. Compile-green on
6 era-spanning nodes (1.20.1-forge, 1.21.4-neoforge, 1.21.5-forge, 1.21.8-fabric, 1.21.11-fabric,
26.2-neoforge); baked textures verified in the projected resources on both sides of the 1.21.5
boundary. **Not yet seen in a client** — owed: summon a farseer on a ≥1.21.5 node and watch the
emergence.

Two neighbours surfaced while reading this code, not fixed here: (1) **#47's likely root** —
`ParticleStaticSpark` and `ParticleSkulkBoom` are `NO_RENDER` on **≥1.21.9** (1.21.9 deleted
`Particle#render`/`ParticleRenderType.CUSTOM`; the port documents the deferral in
ParticleSkulkBoom). The skreecher clap being invisible on 26.2 is that, not the fork's particle
path — it is invisible on all 15 nodes ≥1.21.9. The spark's square-static bug fixed above was
therefore only ever *visible* on 1.21.5–1.21.8 (12 nodes). (2) `LayerVoidWormGlow`'s merged pair
falls back to `RenderType.endPortal()` on ≥1.21.5 — model-shaped geometry, plausible look,
unreported; left alone.

#### #54 — "when you hit the elephant, it doesn't attack you" (reporter: "in the official one it does") — NOT A PORT BUG, live-verified parity

The first triage read the report as "matches upstream, decline"; the user corrected it — the
reporter's claim is that *upstream* elephants DO fight back and this port's don't. So the parity
had to be proven, not asserted. It was, twice over:

**By design (upstream's own code), only tusked adults retaliate.** The port's `EntityElephant`
inner `HurtByTargetGoal.start()` does `if (isBaby() || !isTusked()) { alertOthers(); stop(); }
else super.start()` — byte-identical to baseline `151e36c`, including the odd
`private final boolean hasTuskedAttributes = false` (one dead `refreshDimensions` branch is
upstream's own quirk, not a porting casualty). The charge gate (line ~393) also requires
`isTusked()`. Tusk assignment in `finalizeSpawn`: the first elephant of a herd is always tusked,
every follower is a 50/50 roll — so roughly half of wild elephants (and every baby) run away
instead of attacking, and which one you punched decides what the mod "does". The
`finalizeSpawn` override was verified still alive on 26.2 by javap (rule 10) in the tenth-pass
session.

**And live (2026-08-08, headless RCON on `1.21.11-fabric`, this tree at `2.0.9`):** a
`{Tusked:1b}` adult given `/damage 4 minecraft:mob_attack by <villager>` acquired the villager
and killed it — server log `Villager was slain by Elephant`, ~59 s after the hit (it takes its
time: pathing + windup; a 30 s poll window read "no reaction" and was simply too short). The
control, identical except `{Tusked:0b}`, left its villager at 20.0 health for 90+ s and fled to
the arena edge instead — the tuskless `PanicGoal` arm, exactly as authored.

So retaliation **works in this port**; the reporter almost certainly hit a tuskless (or baby)
elephant and remembered a tusked one from upstream. Nothing to fix and nothing to diverge; the
changelog explains the tusk rule so players can interpret what they see. If the reporter comes
back insisting a *tusked bull* ignored being hit, the thing to ask for is a screenshot (tusks are
visible) plus their MC version/loader.

#### #55 — the cockroach's hat sits slightly off during the maraca dance — NO DIVERGENCE FOUND

`ModelCockroach`'s dance pose and the flags==99 maraca branch in the render path diff clean
against baseline; the hat item's head-slot transform is untouched by any migration pass on any
era. Without a side-by-side screenshot at a named MC version there is nothing actionable —
"maybe it's a little lower" against a three-year-old memory of 1.20.1 is below the noise floor.

#### #45/#48 — the animated icons on ≥1.21.4 — IMPLEMENTED (user-requested, un-deferred)

The tenth pass's deferral was overturned by the user. What shipped in the tree (all of it gated
`>=1.21.4`; the 19 older nodes keep their proven ISTER/`BuiltinItemRendererRegistry` wiring
untouched):

- **`client/render/AMIconSpecialRenderer`** — a `minecraft:special` item-model renderer, type id
  `alexsmobs:icon`, that routes back into `AMItemstackRenderer#renderByItem` (whose body was kept
  compiling on every node for exactly this). Five era arms, each javap-verified:
  `render(…MultiBufferSource…)` on 1.21.4–1.21.8, `submit(…SubmitNodeCollector…)` on
  1.21.9–1.21.11, the context-less `submit` on 26.x (defaults to `ItemDisplayContext.GUI`);
  `getExtents` = `Set<Vector3f>` on 1.21.6–1.21.10, `Consumer<Vector3fc>` on 1.21.11+; the
  `Unbaked` codec is a `MapCodec.unit` with era-gated `bake` signatures (26.x made `Unbaked`
  generic). The ≥1.21.9 arms record into `AMSubmitBuffers` and flush through
  `submitCustomGeometry`; nested entity draws unwrap the same wrapper via
  `AMRenderCompat.renderEntity`.
- **Registration is loader-split, and the private-mapper finding matters beyond this fix**:
  vanilla keeps `SpecialModelRenderers.ID_MAPPER` **private on every era 1.21.4 → 26.2** — the
  "public" reading in loom dev jars is fabric-api's transitive access widener
  (`fabric-transitive-access-wideners-v1`), applied at compile *and* runtime on Fabric only.
  NeoForge has a dedicated mod-bus event (`RegisterSpecialModelRendererEvent`, handler in
  ClientProxy); Forge has no event and no AT, and classic-Forge SRG runtime names make name-based
  reflection fragile — so Forge+Fabric share `AMIconSpecialRenderer.register()`, which finds the
  mapper **by field type** (`ExtraCodecs$LateBoundIdMapper`, unique in the class, immune to
  mappings; `put` is public everywhere and compiled calls remap). Both paths run from
  `ClientProxy.init()` during mod construction, before the first resource reload.
- **`AMItemstackRenderer`** — `drawEntityOnScreen` now takes the `MultiBufferSource` it should
  draw into: on ≥1.21.4 the pipeline hands one down (the wrapper on ≥1.21.9), and the caller owns
  the flush; on <1.21.4 the proven global-`bufferSource()`-plus-`endBatch()` behaviour is
  unchanged. Only two callers (renderByItem and GuiBasicBook's separate copy — the latter
  untouched).
- **Build-logic** — `writeItemModelDefinitions` emits for the three icon items
  `{"model":{"type":"minecraft:special","base":"alexsmobs:item/<id>","model":{"type":"alexsmobs:icon"}}}`
  and empties the base model (identity transforms, same as the old `builtin/entity` parent). The
  **`restaticAdvancementIcons` pass is deleted** — the 59 advancement icons keep their authored
  `custom_data`, which is exactly what the live renderer reads (`DisplayEntityType`/`DisplayItem`/
  `DisplayEffect`); re-adding the pass would freeze the icons *and* starve the renderer.
  `ICON_SUBSTITUTES` (the dictionary-sprite stand-in) is gone with it. Side effect: the animal
  dictionary's index-page `item_render` of `tab_icon` (the #21 hole) now shows the cycling mob,
  matching <1.21.4.
- **GuiItemAtlas caching is a non-issue by construction**: `SpecialModelWrapper.update` calls
  `ItemStackRenderState.setAnimated()` unconditionally on every era 1.21.6 → 26.2
  (bytecode-verified), so special-model stacks are exempt from the GUI atlas cache; 1.21.4/1.21.5
  re-render every frame anyway.

⚠️ **Watch item**: #49 (the 25–30 FPS advancement screen on 26.2) was reported when the 59 icons
were *static*. They are now 59 live entity renders; if #49 worsens, the icons are the first
suspect, and the fallback is throttling `renderByItem`'s advancement-context path, not reverting
to static.

Owed client checks: one node per rendering era — 1.21.4/5 (render arm), 1.21.6–8 (extents + GUI
atlas), 1.21.9–11 (submit arm), 26.x (context-less submit) — creative tab icon cycling,
advancement screen showing animated mobs, dictionary index page, and `/give` of all three items
not crashing.

### The client-verification campaign for `2.0.9` (2026-08-07 → 08-08)

Three dev-client sessions against the tree, run on the user's GPU with their standing
authorisation. Each joined a dedicated server of the same node over
`--quickPlayMultiplayer 127.0.0.1:25565`, driven by RCON plus `ydotool` for the few things RCON
cannot do (opening a screen, pressing F2).

| Session | Node | Verdicts |
|---|---|---|
| 1 | `26.2-fabric` | **#53 ✅** farseer emergence draws as flickering static, not an opaque square. **#45/#48 ✅** creative-tab icon cycles and the advancement icons animate (26.x arm). **#44 ✅** the tarantula-hawk elytra glides (≥1.21.2 arm). **#49 not reproducible** — the advancement screen ran at the 120 fps cap with all 59 icons live. |
| 2 | `1.21.5-neoforge` | **#45/#48 ✅** for the 1.21.4/5 render arm. **#53** second look: correct, and the reason it looked correct *before* the fix too is that 1.21.5 is below the ≥1.21.6 arm this repo's PiP defect lives in. |
| 3 | `1.21.8-forge` | **#45/#48 ✅** for the 1.21.6–8 arm, and **#56 ✅** (below) — the fix's first in-client proof. |
| 4 (headless) | `1.21.1-fabric` + `1.21.3-neoforge` | **#44 ✅** on both arms — the Fabric <1.21.2 `EntityElytraEvents.CUSTOM` handler and the ≥1.21.2 `minecraft:glider` component on a non-Fabric loader — glide distance *and* durability drain matching a vanilla elytra. Via the zombie probe in #44 above; no GPU involved. |
| 5 | `1.21.11-fabric` | **#56 ✅** on Fabric — the dictionary index page's 25 slots each hold a distinct mob. **#45/#48 ✅** for the **1.21.9–11** arm, the last render era never seen. Plus the global-regression sweep: vanilla's inventory player doll still renders. |
| 6 | `26.2-fabric` | The twelfth pass's three visual items, all ✅. **#58** — a golden helmet on the kangaroo's head and an iron chestplate on its body (≥1.21.9 arm). **#59** — a 36-frame tick-stepped capture of the spear wind-up → hold → thrust → hit → return. **#57** — side-by-side `Dwarf:1b`/`Dwarf:0b` underminers, each on its own geometry with its own texture. |
| 7 | `26.2-fabric` | **#62 ✅** — the elephant and the distorted fly are whole on their dictionary pages. **Driven by the user**, not by this session: the dictionary is reached only by clicking, so they navigated and reported. |

**The campaign is complete.** Every render era of #45/#48 (26.x, 1.21.4/5, 1.21.6–8, 1.21.9–11) and
both loader halves of #56 have now been watched in a client, and sessions 6–7 closed all four of the
twelfth pass's visual items.

⚠️ **A click-gated check is not out of reach — it is just not scriptable.** #62 sat unverified for a
session because `ydotool click` is unusable here, and the fix was simply to ask: the user opened the
screen and read it out. Session 5's mouse-free substitutions (an item in the hotbar, an advancement
toast) are worth reaching for first, but where none exists, hand the navigation over rather than
filing the item as unverifiable. Nothing about the dictionary reaches the server log, so there is no
headless substitute for this one.

Session 5 had to work around the fact that **`ydotool click` cannot be used at all on this session**
(it lands at the pointer's pinned `(1,1)` and activates whatever window is in that corner — see
[`gates.md`](gates.md)), which rules out any screen reached by clicking. Two substitutions did the
whole job without a mouse:

- **The creative-tab icon cycle**: the tab row's page arrows need a click, but the icon is an
  ordinary item — `/item replace entity @a hotbar.0 with alexsmobs:tab_icon` puts it in the hotbar,
  where it goes through the same `AMIconSpecialRenderer`. Three F2 screenshots 3–4 s apart held
  **three different mobs**, which is the animation.
- **The advancement icons**: the advancements screen opens with `L`, but selecting the *Alex's Mobs*
  tab needs a click. `/advancement revoke @a everything` then `/advancement grant @a only <id>` for
  three of them draws their icons in the **toasts** instead, through the same path — all three came
  up as distinct, correctly-rendered mobs.

⚠️ `alexsmobs:fancy_item` / `effect_item` given **bare** render as a red barred placeholder. That is
correct, not a regression: those items draw whatever their `custom_data` names, and a hand-given one
has none. Only the advancement-granted copies carry it, which is why the toast is the honest test.

**#49's measurement is now spent.** It was re-measured *with* the icons live, which is exactly the
comparison the watch item asked for, and there is nothing to see at 120 fps on this hardware. It
stays open only because the reporter is on different hardware (PojavLauncher/Android, 26.2) — a
120-fps desktop cap cannot refute a 25–30 fps phone.

#### #56 — the animal dictionary draws the SAME mob in every index slot — **fixed, client-confirmed**

Not reported in words: the user sent three screenshots of the dictionary's index pages, every slot
holding one repeated mob. Found on `1.21.8-forge` during session 3.

**Vanilla's picture-in-picture renderer owns exactly one texture, and the blits that sample it are
deferred past the overwrites.** From 1.21.6 a GUI entity is not drawn where it is submitted:
`GuiGraphics.submitEntityRenderState` files a `GuiEntityRenderState` into the frame's
`GuiRenderState`, and `GuiRenderer.preparePictureInPicture()` renders them all at flush, looking
the renderer up in a `Map` keyed by `state.getClass()` — so every entity in the frame gets the
**same** `GuiEntityRenderer`. `blitTexture` records a `BlitRenderState` holding a live reference to
`this.textureView`, and those blits are drawn after every `prepare()` call has run. They therefore
all sample whatever was rendered **last**.

Vanilla never notices because no vanilla screen draws two entities at once. The dictionary draws
one per index button plus the big mob on the left page.

- **NeoForge fixed it in the loader**, from `neoforge-21.6.20-beta` (MC 1.21.6) onward, via
  `PictureInPictureRendererPool`. Its patch comment states the symptom verbatim: *"In Vanilla, a
  renderer would be used for multiple different states even within the same frame, leading to
  crashes and the last state being used for all blits of that renderer in that frame."* So the
  defect is reachable only on **Forge and Fabric, MC ≥1.21.6 — 16 of 49 nodes**, and has shipped
  since `2.0.0`. Below 1.21.6 `EntityLinkButton` takes the immediate-render arm and is fine, which
  is why session 2 on `1.21.5-neoforge` looked correct on both counts.
- ⚠️ **Do not confuse this with #21 or #45.** The advancement and creative-tab icons render through
  the item atlas, not the PiP path, so they show distinct mobs even while the dictionary does not.
  A screenshot of correct icons is not evidence about the dictionary.

**Fix** — `client/render/AMGuiEntityPipPool` plus `mixin/client/GuiRendererMixin`. The pool hands
out the vanilla renderer for the frame's first GUI entity, so an ordinary one-entity screen
allocates nothing, and a private `GuiEntityRenderer` for each one after; assignment is by position
in the frame, which is stable for a given screen, so a pooled renderer keeps being asked for the
same texture size instead of reallocating. Three seams on `GuiRenderer`: `preparePictureInPicture`
HEAD resets the counter, `close` releases the pooled textures, and a `@Redirect` on the one
`Map.get` inside `preparePictureInPictureState` substitutes the renderer.

- **The `Map.get` is the era-stable hook.** `preparePictureInPictureState` contains exactly one
  `invokeinterface java/util/Map.get` in every era, even though the state type moved package
  (`client/gui/render/state/pip/` on 1.21.6–1.21.11 → `client/renderer/state/gui/pip/` on 26.x).
  `GuiEntityRenderer` stays at `net/minecraft/client/gui/render/pip/` throughout, and
  `preparePictureInPicture()`/`close()` are stable everywhere. Keying the redirect on the state
  type would have needed a per-era `@At` string; keying it on `java.util.Map` needs none.
- **The mixin body is `//? if !neoforge`, and the class still applies there, inert.** NeoForge
  pools these itself and its `preparePictureInPictureState` has a different signature because of
  it. Leaving an empty mixin applied is what lets the mixin config stay loader-uniform.
- **The constructor is the only 26.2 divergence**: `GuiEntityRenderer(EntityRenderDispatcher)`
  there, `(MultiBufferSource.BufferSource, EntityRenderDispatcher)` on 26.1.2 and below. Vanilla
  feeds it `renderBuffers().bufferSource()` (read from `GameRenderer`'s ctor bytecode), so
  `Minecraft.getInstance()` supplies both arguments and no `@Shadow` is needed.
- **Build-logic**: excluded from the compile below 1.21.6 (where `GuiRenderer` does not exist) and
  pruned back out of `alexsmobs.mixins.json` there, the same shape as the render-state mixins.
  ⚠️ The built config lists every client mixin in **both** `mixins` and `client` on Forge/NeoForge
  — that is pre-existing and benign (the dist cleaners block client classes on a server, which is
  where this repo's documented `/ERROR]` lines come from), so seeing the new entry twice is not a
  regression.

**Client-confirmed on `1.21.8-forge` (2026-08-08)**: the index page's 25 slots each hold a
different mob, with the raccoon on the left page. Screenshot in
`versions/1.21.8-forge/run/screenshots/2026-08-08_14.47.12.png`. **And on `1.21.11-fabric` the same
day** — the other affected loader — `versions/1.21.11-fabric/run/screenshots/2026-08-08_16.30.28.png`,
25 distinct mobs again. The global-regression check went with it: vanilla's own survival-inventory
player doll still draws, so handing the frame's first entity to vanilla's renderer and only the
*later* ones to pooled copies leaves single-entity screens on exactly the path they were on.

**Statically verified on 22 nodes** (2026-08-08) — all 16 Forge/Fabric nodes ≥1.21.6, four NeoForge
nodes for the inert arm, and `1.21.1-forge` + `1.21.5-fabric` as below-1.21.6 prune controls. One
Gradle invocation, `BUILD SUCCESSFUL in 4m 1s`; `verify_mixins.py` → `jars=22 problems=0`,
`verify_mixin_targets.py` → `jars=22 selectors=529 problems=0 skipped=27`. **The count was
predicted before the run** (22 built, the other 27 still holding `2.0.8` jars and therefore named
as skipped, which is what that report is for). The prune shows up as `declared=` dropping by one
below the boundary: `1.21.6-forge` 17 vs `1.21.1-forge` 13 by other gates, `1.21.6-fabric` 30 vs
`1.21.5-fabric` 29. Since `defaultRequire: 1` makes a zero-match selector fatal, the three
injectors resolving on every era 1.21.6 → 26.2 is the portability claim proven — the
`java.util.Map.get` redirect included.

---

## The twelfth bug-report pass (2026-08-08) — nine items, unreleased

Nine items from one reporter, arriving after `2.0.9` was built and gate-green. **Eight bugs and one
feature idea (#59).** ⚠️ **The reporter's MC version and loader are unknown**, and five of the nine
turn on that: this port's defects are overwhelmingly *era-gated*, so "it works in the official mod"
narrows a report to a boundary only once you know which side of it the reporter is on. The tenth
pass's reporter turned out to be on **26.2 with an outdated `2.0.7`**, and if that is the same
person here, #58 predates the `2.0.8` kangaroo-armour rewrite (#41) and may already be fixed.
That last guess was **wrong**: one answer from the reporter — *"iron does not show either"* — turned
#58 from a material question into a whole-slot one and root-caused it in an hour. When a report turns
on a boundary, the cheapest move is still to ask for the one fact that splits it.

Three are settled from the source alone and need nothing further.

| # | Report | Verdict |
|---|---|---|
| #57 | miner ghost has "a very strange texture" | **FIXED, client-confirmed** — ≥1.21.2, all loaders (a second, lesser cause is deferred) |
| #58 | helmet invisible on the kangaroo (any material) | **FIXED, client-confirmed** — ≥1.21.9, all loaders |
| #59 | *(idea)* kangaroo charge-up animation when attacking with a spear | **IMPLEMENTED, client-confirmed** — all nodes |
| #60 | raccoon-hat tail does not animate in third person | **FIXED in tree** — ≥1.21.2, all loaders |
| #61 | queen leafcutter ant does not leave the anthill to attack | **FIXED in tree** — ≥1.21.5, all loaders |
| #62 | dictionary mobs missing body parts (elephant, distorted fly) | **FIXED, client-confirmed** — ≥1.21.6, all loaders |
| #63 | cannot tame a bald eagle by throwing it cod | **NOT A BUG** — upstream tames with fish oil |
| #64 | bald eagles fly below ground and do not walk to food | partially diagnosed |
| #65 | skunks are slow to spray after being hit | **NO DIVERGENCE** — upstream's own timing |

#### #57 — the underminer ("miner ghost") renders with a strange texture — FIXED, ≥1.21.2, all loaders

**Two independent causes, on two different boundaries.** The one that is actually "a very strange
texture" is fixed; the other is a cosmetic degradation filed with its own family.

**Cause 1 (fixed) — the tall underminer was drawn on the dwarf's skeleton, ≥1.21.2, 35 nodes.**
`EntityUnderminer#finalizeSpawn` clears the `Dwarf` flag on a `random.nextFloat() < 0.3F` roll, so
**~30% of underminers are the tall, non-dwarf form**, and `RenderUnderminer#getTextureLocation`
duly hands those `underminer_0/1.png`. But the port had sliced the model out and said so:

```java
// SLICE (>=1.21.2): the non-dwarf HumanoidModel form is dropped —
// HumanoidModel is keyed on HumanoidRenderState now
```

so `this.model = DWARF_MODEL` ran unconditionally above 1.21.2 while the texture kept branching.
Dwarf geometry sampling a humanoid skin is scrambled UVs on every visible face — the reporter's
"strange texture", on **35 nodes**, every loader, ~30% of the underminers on each, since `2.0.0`.
Below 1.21.2 upstream's own `HumanoidModel<EntityUnderminer>` is still in use, which is why it reads
as version-dependent.

The fix is a new **`client/model/ModelUnderminerHumanoid`** with two sibling arms, because the base
class *is* the whole difference. `<1.21.2` is literally `extends HumanoidModel<EntityUnderminer>`,
so the 14 already-correct nodes change by nothing. `≥1.21.2` extends the compat
`render/compat/EntityModel<EntityUnderminer>` (the entity-keyed base every renderer and layer in
this mod is declared against), **owns** a vanilla `HumanoidModel<HumanoidRenderState>`, and drives
it through a `HumanoidRenderState` it fills itself in `setupAnim`. Delegating to vanilla's own
`setupAnim` rather than re-deriving the humanoid walk cycle is what keeps it correct from 1.21.2 to
26.2 with **no version gate in the body**. `RenderUnderminer` then loses its gate entirely and runs
upstream's plain `isDwarf() ? DWARF_MODEL : NORMAL_MODEL`, with `NORMAL_MODEL` baked from
`renderManagerIn.bakeLayer(AMModelLayers.UNDERMINER)` — that layer was **already registered on all
49 nodes** (`AMModelLayers.registerAll` is un-gated) and was simply going unused above the boundary.
`LayerUnderminerItem#translateToHand` gains a branch naming the new type, since above 1.21.2 the
wrapper is not an `ArmedModel`.

Three details worth keeping:

- The default-constructed `HumanoidRenderState` is **NPE-safe** on every version in range — `mainArm`,
  `attackArm`, `useItemHand`, `rightArmPose`/`leftArmPose` and `swingAnimationType` all carry non-null
  initialisers in vanilla's own constructor (read from bytecode on 1.21.4 and 26.2), so nothing here
  hands `setupAnim` a null to switch on. Same guarantee `LayerKangarooArmor`'s `KangarooArmorState`
  already relies on.
- `translateToHand` is **spelled out** (`(arm == LEFT ? inner.leftArm : inner.rightArm)
  .translateAndRotate(pose)`) rather than delegated, because 1.21.9 gave vanilla's version a leading
  render-state parameter — and a Stonecutter block **cannot nest** inside the class-level block above
  it. `ModelPart.translateAndRotate(PoseStack)` is public on every version 1.20.1→26.2 (javap), so
  inlining vanilla's one-line body needs no gate at all.
- The render-state fields written here (`mainArm`, `attackTime`) **moved class** across the range —
  `mainArm` to `ArmedEntityRenderState` at 1.21.4, `attackTime`/`attackArm` at 26.2 — but all of them
  still resolve through a `HumanoidRenderState` reference, so no gate is needed to set them.

Compile-green on `1.20.1-forge`, `1.21.1-neoforge`, `1.21.2-neoforge` (the boundary), `1.21.4-forge`,
`1.21.8-neoforge`, `1.21.11-fabric`, `26.2-neoforge`. **Client-confirmed on `26.2-fabric`
(2026-08-08)**, side by side: the `Dwarf:1b` one is stout dwarf geometry wearing
`underminer_dwarf.png` (128×128), the `Dwarf:0b` one is slim humanoid geometry wearing
`underminer_0.png` (64×64, green shirt / blue trousers) and holding its pickaxe. The residual
translucency on both is cause 2, below, not this fault. ⚠️ The NBT key is **`Dwarf`** and the field
defaults to `true`, so a summon tag that omits it yields a **dwarf** — to get the tall form you must
pass `{Dwarf:0b}` explicitly.

**Cause 2 (deferred) — the lost energy-swirl render type, ≥1.21.5, 27 nodes.** Upstream builds a
composite around
`RENDERTYPE_ENERGY_SWIRL_SHADER` — that shader is what makes the underminer read as a ghost. This
port's `AMRenderTypes#getUnderminer` carries a `//? if >=1.21.5` arm that returns plain
`RenderType.entityTranslucent(texture)`, so on **27 nodes** the swirl is gone and what is left is
the flat texture drawn translucently. That is a real regression, but it is a *dulled* ghost, not a
scrambled one — cause 1 above is the one that matches "a very strange texture".

**It is one of a family.** Sweeping `AMRenderTypes` for the same shape finds **15** render types
whose ≥1.21.5 arm collapses to a vanilla approximation — `getGhost`, `getGhostPickaxe`,
`getGhostCrumbling`, `getTransparentMimicube`, `getFrilledSharkTeeth`, `getSunbirdShine`, the six
`getEyes*`/`getFullBright`/`getFreddy`/`getSpectreBones` emissive types, `getSkulkBoom` and
`getFarseerBeam`. **#53 (the farseer square) was this same family**, which is why that one was
found first: it was the most visually violent member. The emissive ones degrade acceptably
(`RenderType.eyes` is a fair stand-in); the shader-bearing ones do not.

**Why they were written that way, and the fix.** From 1.21.5 `RenderType` is fully abstract and
demands a `RenderPipeline`, so the old `CompositeState` builder cannot construct one at all — the
fallbacks were the only thing that compiled. But for *this* member there is a faithful substitute
that costs one line: **vanilla ships its own `energySwirl`**. Verified by javap on the mapped
client jars: `26.1.2` and `26.2` both have
`net.minecraft.client.renderer.rendertype.RenderTypes.energySwirl(Identifier, float, float)` and a
`RenderPipelines.ENERGY_SWIRL`, and passing `0F, 0F` gives the un-scrolled swirl upstream's hand-built
composite was after. ⚠️ **Not yet confirmed for 1.21.5 → 1.21.11** — the loom caches for those
versions hold *intermediary* jars, not mapped ones, so the javap sweep came back empty for them
rather than negative. Confirm against a mapped jar (or NeoFormRuntime's `recompile_*` output, the
same source `verify_mixins.py` uses) before writing the gate, because the class **moved** —
`RenderType.energySwirl` on the older half, `rendertype.RenderTypes.energySwirl` on 26.x.

Deliberately **not** attempted in this pass: it landed after `2.0.9` was already built and verified,
and a render-pipeline change wants its own client check. Group it with **#47** (the ≥1.21.9 custom
particle path) — same file, same era boundary, same root cause.

#### #63 — "you can't tame a bald eagle by throwing it cod" — NOT A BUG

Upstream does not tame bald eagles with fish, and never did. Both halves are identical to the
baseline, `mobInteract` and the tags:

- `bald_eagle_tameables` = **`alexsmobs:fish_oil`**, and only that.
- `bald_eagle_foodstuffs` = `#minecraft:fishes`, whose own comment in the file reads
  **"Heals when given"** — the branch above it requires `getHealth() < getMaxHealth()`.

So cod *heals* a bald eagle; **fish oil tames it**, and by right-clicking the bird with it, not by
throwing it on the ground. `EntityBaldEagle#mobInteract` matches upstream line for line. Nothing to
fix — worth a line in the changelog, since the reporter is unlikely to be the only one who assumed
fish would do it.

⚠️ **Re-challenged by the user** ("bald eagles in the original mod apparently would go after fish on
the ground and would be tamable with fish they like"), and re-verified seven ways against the
pristine baseline `151e36c`: the goal list, `isFood`, `mobInteract`, all four tags, the full entity
diff, the navigators, and the built resources per version (tag folder migration, `#minecraft:fishes`
present throughout). The verdict held, and the two halves of the belief have separate explanations:

- **"Tamable with fish they like"** — `bald_eagle_foodstuffs` (`#minecraft:fishes`) *is* fed to the
  `TemptGoal` alongside the tameables, so **holding cod does make a wild eagle come to you**. It just
  never converts to taming; only fish oil does. Being led around by cod and never tamed by it is
  exactly the shape of experience that produces this report.
- **"Goes after fish on the ground"** — **neither upstream nor this port gives the bald eagle any
  item-pickup goal at all** (`wantsToPickUp` is overridden on exactly one entity in the mod, and it
  is the underminer). What eagles *do* hunt is **live fish entities**, via `bald_eagle_targets` and
  `AITackle` — a bird stooping on a salmon in a river is almost certainly what the memory is of.

Both statements are upstream's design, on all 49 nodes. Nothing to fix; both are now stated plainly
in the changelog rather than left to be re-reported.

#### #65 — "skunks take a while to release the smell when hit" — NO DIVERGENCE

`EntitySkunk`'s whole diff against the baseline is mechanical (the ≥1.20.5 `SynchedEntityData.Builder`
split, `AMCompat.ingredientOf`, `AMCompat.create`, `isClientSide()`); the spray logic is untouched.
Upstream's own numbers are: each hit adds **`harassedTime += 10`**, the spray fires only when
`harassedTime > 200` with `sprayCooldown == 0` and the skunk is not a baby, and `harassedTime`
**decays by 1 every tick**. That is ~21 hits landed quickly enough to outrun the decay — the delay
the reporter is describing is upstream's design, unchanged. Same verdict class as #54 and #55.

#### #64 — bald eagles fly below ground level and do not walk toward food — PARTIALLY DIAGNOSED

Two symptoms, and only the second has a lead so far.

The tempt behaviour is a real divergence in *mechanism*, though not yet shown to be the symptom.
Upstream passes the goal a live two-tag ingredient (`Ingredient.fromValues(... TagValue(TAMEABLES),
TagValue(FOODSTUFFS))`). The port routes it through `AMCompat.ingredientOfTags`, whose **≥1.21.2**
arm iterates `BuiltInRegistries.ITEM.getOrThrow(tag)` and freezes the result into
`HolderSet.direct(holders)` — **a snapshot taken when the goal is constructed**, where upstream held
a reference that re-reads the tag. Any later rebinding (a `/reload`, a datapack that edits either
tag) leaves the eagle tempted by a stale set, and an empty snapshot would make the goal silently
never fire — which is exactly "they don't walk toward food". Whether it is *this* reporter's fault
depends on their version: below 1.21.2 the other arm is used and is faithful.

**The snapshot was reviewed and deliberately NOT changed.** Making it live means hand-rolling a
union `HolderSet<Item>` that re-queries the named sets on every `test()`, across five eras of a
~8-method interface whose signatures shifted more than once — real risk, for a fault with **no
observed symptom**: on a running server tags are bound long before any entity is constructed, so the
snapshot is only ever stale after a mid-session `/reload`. Fixing it would not honestly close #64.
Left recorded here so it is not re-discovered as a suspect; the one call site that *could* bite is
`EntityManedWolf.allFoods`, a memoised `static final` whose snapshot is taken at class-load.

**"Flies below ground level" is not a port divergence as far as the source can show.** The whole of
`EntityBaldEagle` diffs against the baseline as mechanical port shims only (the `SynchedEntityData`
builder split, `AMCompat.getBoolean/getInt`, `AMCompat.hasCraftingRemainder`, the `controlledFlag`
look-control work from #22), and **both navigator classes it swaps between — `GroundPathNavigatorWide`
and `DirectPathNavigator` — carry no version gate at all**, i.e. they are upstream's code verbatim on
all 49 nodes. `switchNavigator` is upstream's too. Vanilla's `TemptGoal#tick` (`moveTo(player,
speed)` beyond 6.25 blocks, `stop()` inside it) is unchanged across 1.20.1→26.2, so a *flying* eagle
tempted by food will be steered by `DirectPathNavigator`, which flies a straight line and ignores
terrain — upstream behaves the same way. **Blocked on the reporter's version and loader**, or on a
client session; there is nothing left to read.

#### #61 — stepping on an anthill does not anger the ants — FIXED, ≥1.21.5, all loaders

**`Block#fallOn` widened its fall distance from `float` to `double` at 1.21.5.** Upstream writes no
`@Override` on it, so above that boundary the mod's `float` form is an unrelated **overload**: it
compiles clean, no gate notices, and it is simply never called. `BlockLeafcutterAnthill#fallOn` is
the anthill's *only* stomp seam — `stepOn` (unchanged across the whole range) is not overridden
there at all — so on **27 nodes** stepping on an anthill did nothing whatsoever.

Two more blocks share the exact fault and were fixed with it: **`BlockReptileEgg`** and
**`BlockTerrapinEgg`** both trample through `fallOn`, so croc/caiman/platypus and terrapin eggs
could not be broken by jumping on them either. Their `stepOn` overrides still worked, which is why
nobody reported those — walking over the eggs still cracked them, at 1-in-100 instead of 1-in-3.

All three are now a gated pair: the `double` form above 1.21.5, the `float` form below, with the
body lifted into a shared private helper on the anthill (it is long) and duplicated in the egg
blocks (it is two lines).

#### #66 — every per-tick potion effect has been inert since 1.20.2 — FIXED, found here, not reported

Not from the reporter. Found by the systemic sweep #61 motivated (below), and it is the largest
silent regression this port has had after Fabric networking. **Two independent dead overrides**, in
the same 18 classes, stacking:

1. **`MobEffect#applyEffectTick` changed twice.** At **1.20.5** it began returning `boolean`; at
   **1.21.2** it gained a **leading `ServerLevel`** parameter. The tree had followed the first
   change but not the second, so from 1.21.2 up the 3-arg form was a dead overload.
2. **`MobEffect#isDurationEffectTick` was RENAMED `shouldApplyEffectTickThisTick` at 1.20.2** —
   and this one is worse than losing an override, because the base implementation is
   `iconst_0; ireturn`: it **returns `false`**. Keeping the old name does not fall back to
   upstream's behaviour, it stops the effect from ticking at all.

⚠️ **Pin that boundary from bytecode, not from the neighbouring one.** The rename is at **1.20.2**;
the `boolean` return on `applyEffectTick` is at **1.20.5**. They are a version apart and it is very
easy to assume one date covers both — the first cut of this fix gated the rename at `>=1.20.5` and
would have shipped **1.20.4-forge and 1.20.4-neoforge still broken**. javap says plainly:
1.20.1 `isDurationEffectTick` + `void applyEffectTick`; 1.20.2 `shouldApplyEffectTickThisTick` +
`void`; 1.20.6 the same name + `boolean`; 1.21.2 on, `boolean` with a leading `ServerLevel`.

Either one alone is fatal, and (2) starts at **1.20.2** — so on 46 of 49 nodes every one of these
did nothing: **BugPheromones, Clinging, DebilitatingSting, Earthquake, EnderFlu, Exsanguination,
Fear, FleetFooted, KnockbackResistance, LavaVision, MosquitoRepellent, Oiled, OrcaMight,
PoisonResistance, PowerDown, Soulsteal, Sunbird, TigersBlessing.** Both are now three-arm and
two-arm gates respectively across all 18 classes.

⚠️ **The rename is exactly what the sweep tool cannot see** — it matches on same-name /
different-descriptor, so a vanilla method that changes *name* looks like a method the mod simply
invented. That half was found by hand. Assume the sweep's output is a floor, not a ceiling.

#### The systemic finding behind #60, #61 and #66 — `scripts/verify_overrides.py`

All three are the same failure: **a mod method that used to override a vanilla one and silently
stopped**, because upstream Alex's Mobs almost never writes `@Override`. This is porting rule 10's
failure mode in its non-mixin form, and **nothing in this project could see it** — not the compiler,
not the 49-node build gates, not either mixin verifier. `scripts/verify_overrides.py` now does:
it walks every compiled mod class's superclass chain into the cached mapped vanilla jar and reports
methods whose name an ancestor declares with an incompatible parameter list. Run it as a **review
list, not a pass/fail gate** — deliberate overloads are legitimate and do show up.

    ./gradlew :26.2-neoforge:compileJava
    python3 scripts/verify_overrides.py --baseline=1.20.1-forge 26.2-neoforge

`--baseline` subtracts what upstream was already carrying (the `causeFallDamage(float,float)`
family has been dead since 1.17 — it matches "the official mod" and is not a port regression).
There is a **backlog of ~33–64 unverified hits per node** left; each needs the same hand-check
against upstream that these three got before it is claimed or fixed.

#### #60 — the raccoon-hat tail does not animate — FIXED, ≥1.21.2, all loaders

Not a dead override — a **deliberate port divergence**, and the source said so in a comment.
`CustomArmorRenderProperties#getHumanoidArmorModel` had `>=1.21.2` arms that dropped
`withAnimations(entityLiving)` on the ground that 1.21.2 swapped the wearer for a render state
"carrying no hook the mod's entity-driven `withAnimations()` calls need". That is not true of this
animation: it needs `walkAnimation.position()` and `.speed(partialTick)`, and the state carries
both as `walkAnimationPos` / `walkAnimationSpeed`, already interpolated by the extract pass.

Three items went through those arms; two are fixed and one deliberately is not:

- **`FRONTIER_CAP`** (the raccoon hat) — the reported one. Restored verbatim; only the two inputs
  now come off the state.
- **`TARANTULA_HAWK_ELYTRA`** — same fault, unreported, and newly *visible* because #44 restored
  the gliding in this very release: the wings stayed folded in the walking pose even mid-glide.
  `HumanoidRenderState` carries `elytraRotX/Y/Z`, which vanilla's extract pass reads off
  `LivingEntity#elytraAnimationState` — the exact 0.1-per-tick lerp upstream's body runs by hand.
- **`FLYING_FISH_BOOTS`** — left neutral **on purpose**. Its flap is driven by
  `FlyingFishBootsUtil.getBoostTicks`, which reads the wearer's citadel NBT; no render state carries
  it, and the neutral pose is the correct out-of-water one. The loss is confined to the water boost.

⚠️ **The seam is not the same on all three loaders, and one has none.** Established by javap:

| Era / loader | Where the wearer's pose is reachable |
|---|---|
| `<1.21.2`, all | the armour hook, with a live `LivingEntity` — worked all along |
| `>=1.21.2` **Forge** | the hook itself — it is handed a `LivingEntityRenderState` |
| `>=1.21.2` **Fabric** | Fabric API's `ArmorRenderer#render`, handed a `HumanoidRenderState` |
| `>=1.21.2` **NeoForge** | **nothing** — its hook takes `(ItemStack, LayerType, Model)`, no wearer |
| `>=1.21.9`, all | `setupAnim(state)`, re-run at flush (`EquipmentLayerRenderer` only submits) |

So the fix lands at three call sites plus one override. The models grew a
`withAnimations(LivingEntityRenderState)` overload **and** an overriding
`setupAnim(HumanoidRenderState)`; Forge's hook and Fabric's renderer call them directly, and the
`setupAnim` override is what carries NeoForge from 1.21.9 up. **NeoForge 1.21.2–1.21.8 has no seam
short of a mixin and is knowingly left unanimated** — the `setupAnim` override is in place there
too, so it starts working for free if that loader ever calls it.

`HumanoidArmorLayer#renderArmorPiece` never calls `setupAnim` on the swapped-in model below 1.21.9
(javap-confirmed on 1.21.3 and 1.21.8), which is why the Fabric arm has to call it by hand — and it
does so **before** `copyPropertiesTo`, so the wearer's real pose wins on the seven standard parts
while the animated children (tail off the hat, wings off the body) survive.

Two sibling models, `ModelRockyChestplate` and `ModelUnsettlingKimono`, also carry dead
`setupAnim(LivingEntity,FFFFF)` overrides above 1.21.2 — **reviewed and benign**: both bodies are
deliberately **empty** (a no-op to suppress the base humanoid pose). Do not "fix" them.

#### #62 — dictionary mobs are missing parts of their body — FIXED, ≥1.21.6, all loaders

Not #56's neighbour after all, and not a layer problem: **the GUI entity's viewport was a fixed
five-blocks-square box, and big mobs were cropped by it.**

`GuiGraphics#submitEntityRenderState(state, scale, translation, rotation, cameraAngle, x0, y0, x1,
y1)` — 1.21.6's replacement for the immediate-mode entity-in-GUI path — treats that rectangle as a
**hard viewport in absolute screen coordinates**, and puts the entity's origin at its centre.
Vanilla's own callers hand it the widget rectangle *on purpose*, because the inventory doll is meant
to be cut off at the panel edge. `AMRenderCompat#submitGuiEntity` has no widget rectangle to pass —
both of this mod's callers give it a centre and a scale — so it invented one:

```java
int half = Math.max(1, Math.round(Math.abs(scale) * 2.5F));
```

`scale` is pixels-per-block, so that is **±2.5 blocks around the origin, on every mob**. The origin
is the entity's feet, so the real constraint is `bbHeight ≤ 2.5`. The elephant is `sized(3.1F, 3.5F)`
— a full block of its back and head fell outside the box, on its own dictionary page *and* in its
index slot, on all **24 nodes ≥ 1.21.6**, every loader, since `2.0.0`. Every mob taller than 2.5
blocks or wider than about 3.5 shared it, which is the reporter's "and other mobs". Below 1.21.6 the
immediate-mode path clips nothing at all, which is why it reads as version-dependent.

The box is now sized from the entity. It must stay **centred** on the anchor — the origin lands at
the centre, so moving the rectangle moves the mob — hence one symmetric half-extent that clears the
model in every direction: `bbHeight` straight up, and, once the book's 30° of pitch has tilted it,
about `0.71*bbWidth + 0.5*bbHeight` sideways. `×1.5 + 0.5` on top is headroom for the many models
that overhang their hitbox, and a **2.5-block floor keeps the old constant as a minimum**, so no mob
that fitted before can start clipping now. Capped at 512 px so a pathological scale cannot ask for a
huge offscreen target.

The two callers need no change: `GuiBasicBook#drawEntityOnScreen` (the detail page) and
`EntityLinkButton#renderEntityInInventory` (the index slots). The index slots keep their explicit
`enableScissor(4, 4, 20, 20)`, which is captured into the PiP state at submit time (bytecode:
`GuiGraphics.submitEntityRenderState` reads `scissorStack.peek()`), so growing the viewport cannot
let an icon spill out of its frame.

Compile-green on `1.21.5-neoforge` (the below-boundary control), `1.21.6-forge`, `1.21.8-neoforge`,
`1.21.11-fabric`, `26.1.2-forge`, `26.2-neoforge`. **Client-confirmed on `26.2-fabric`
(2026-08-08)** — the user opened the dictionary and reported the elephant and the distorted fly, the
two the reporter named, both whole. Session 7 in the campaign table above; the user did the
navigating, because the dictionary is reached only by clicking and `ydotool click` is unusable here.

⚠️ Correct advancement and tab icons still say nothing about this path: those go through the item
atlas, not the GUI PiP. Same caveat as #56.

#### #58 — helmet invisible on the kangaroo — FIXED (2026-08-08), 15 nodes ≥1.21.9

Filed as "diamond or netherite", and the first read found **no material-specific branch anywhere in
the path** — `LayerKangarooArmor` (both eras), `setModelSlotVisible`'s `case HEAD`, `EntityKangaroo`'s
slot handling and `AMCompat.armorOf` all treat diamond and netherite exactly as they treat iron. That
reading was right, and the answer that unblocked it was the user's: **"iron does not show either."**
Not a material fault — the whole HEAD slot.

**Cause: a blanket Stonecutter rename applied across an API *split*.** 1.21.9 replaced
`ModelLayers.ARMOR_STAND_INNER_ARMOR`/`_OUTER_ARMOR` — one full humanoid mesh per size, each carrying
every part, so any slot could be drawn from either — with `ModelLayers.ARMOR_STAND_ARMOR`, an
`ArmorModelSet<ModelLayerLocation>` record with `head()/chest()/legs()/feet()`. This tree met that
with one rule:

```kotlin
string("!mc2109-armorstand-layer", true) { replace("ModelLayers.ARMOR_STAND_OUTER_ARMOR", "ModelLayers.ARMOR_STAND_ARMOR.chest()") }
```

on the assumption that both usages only wanted *a* humanoid layer. But **each slot's mesh keeps only
that slot's parts** (`HumanoidModel.ADULT_ARMOR_PARTS_PER_SLOT`, from 26.2 bytecode: HEAD→`{head}`,
CHEST→`{body,left_arm,right_arm}`, LEGS→`{left_leg,right_leg,body}`, FEET→`{left_leg,right_leg}`), and
the parts it does *not* keep **still exist by name** — `PartDefinition#retainPartsAndChildren` /
`retainExactParts` re-add them via `addOrReplaceChild(key, CubeListBuilder.create(), <same pose>)`,
i.e. cube-less shells at the original pose. So a `HumanoidModel` baked from the chest mesh
**constructs** (`root.getChild("head")` resolves), **poses**, and **submits** — and draws nothing.
No exception, nothing for the compiler, and nothing for any of the four verifiers.

Fixed by making the layer a **per-slot** choice, which needs no replacement rule at all — the rule is
deleted and a comment left in `stonecutter.gradle.kts` in its place:

- `AMRenderCompat#armorStandArmorLayer(EquipmentSlot)` — `ARMOR_STAND_ARMOR.get(slot)` on ≥1.21.9,
  the old INNER (LEGS) / OUTER (everything else) pair below it.
- `LayerKangarooArmor` — all four bakes now ask for their own slot (`headArmorModel` ← HEAD,
  the two chest passes ← CHEST).
- `LayerMimicubeHelmet` — **a second, never-reported instance of the same fault**, found while
  fixing this one: its `defaultBipedModel` is what draws the mimicube's stolen helmet, so that was
  invisible on the same 15 nodes.

Affected: 1.21.9, 1.21.10, 1.21.11, 26.1.2, 26.2 × forge/neoforge/fabric = **15 of 49 nodes**, since
`2.0.0`. Compile-green on `1.20.1-forge`, `1.21.1-fabric`, `1.21.4-neoforge`, `1.21.8-forge`,
`1.21.9-neoforge`, `1.21.11-fabric`, `26.2-neoforge`, with the projections re-checked on both sides of
the boundary. **Client-confirmed on `26.2-fabric` (2026-08-08)** — a golden helmet draws on the
kangaroo's head and an iron chestplate on its body. Session 6 in the campaign table above.

⚠️ **A kangaroo gear test cannot use vanilla's `equipment:{…}` summon tag, and a first attempt at
this check produced a false negative that way.** `EntityKangaroo` overrides `getItemBySlot` to read
its **own** 9-slot `SimpleContainer kangarooInventory` through the synched `HELMET_INDEX` /
`CHEST_INDEX` / `SWORD_INDEX` accessors — vanilla's equipment slots are never consulted, so gear put
there renders nothing and proves nothing. `resetKangarooSlots()` recomputes those indices (best
damage → sword, best armour → helmet/chest) and calls `updateClientInventory()`, which broadcasts
`MessageKangarooInventorySync` for all nine slots — and the aiStep call site is gated on
`tickCount > 5 && !level().isClientSide() && clientArmorCooldown == 0 && this.isTame()`, so **an
untamed kangaroo never syncs its gear to the client either**. A valid probe needs both halves:

```
/summon alexsmobs:kangaroo <x> <y> <z> {Owner:[I;…],Items:[{Slot:0b,id:"minecraft:golden_helmet",count:1},{Slot:1b,id:"minecraft:iron_chestplate",count:1}]}
```

with `Owner` the player's UUID int-array. `HelmetInvIndex`/`ChestInvIndex` then resolve to 0/1 on the
next tick and the layer draws. Same trap applies to #41 and to any future kangaroo-armour work.

⚠️ **The durable lesson**: when a version *splits* one API into several, a blanket rename to one of
the parts compiles everywhere and is wrong wherever the part matters. A refuted hypothesis got us
here — `EntityModelSet#bakeLayer` was suspected of handing out a shared part tree, and javap on
1.21.8, 1.21.11 *and* 26.2 showed it calls `bakeRoot()` per invocation every time. Discarding it is
what forced a look at the **generated** sources, where the rename was plainly visible.

#### #59 — kangaroo charge-up animation when attacking with a spear — IMPLEMENTED (2026-08-08)

A feature idea, not a defect, and a **deliberate divergence from upstream**; built on the user's
go-ahead ("do the kangaroo spear charge up").

Two facts shaped the reading. Alex's Mobs has **no spear item** — `grep -rni spear src/main/java`
returns only `ModelUnderminerDwarf`'s two `case THROW_SPEAR:`, which is vanilla's
`HumanoidModel.ArmPose` for a charging trident. And the kangaroo's armed attack is upstream's
`ANIMATION_PUNCH_R`/`_L`: a 13-tick boxing hook (3 ticks of wind, 3 to the strike, reset 5) that it
throws with whatever it is holding. So "the same charging animations when using a spear" is read as:
*a spear-shaped weapon should be wound up and thrust, not jabbed.*

- **What counts as a spear** is a data tag, `alexsmobs:kangaroo_spears` — `minecraft:trident` plus
  optional (`"required": false`) references to `#c:tools/spear(s)` and `#forge:tools/spear(s)`, so a
  pack or another mod's spear joins in without a code change.
- `EntityKangaroo.ANIMATION_STAB_R` / `_L`, 22 ticks. The hit is on tick **13**
  (`STAB_HIT_TICK`), when the thrust finishes, and knocks the target **straight back** along
  `getYRot()` at 1.0 — the punches hook sideways at ±90° and 0.85.
- `KangarooAIMelee` picks it whenever the main hand is in the tag, in place of the kick/punch coin
  flip; an unarmed kangaroo is untouched, so no existing behaviour changes.
- `ModelKangaroo` holds the keyframes: 5 ticks raising the weapon over the shoulder, **5 holding it
  there** (this is the charge the report asks for), 3 thrusting, 2 on the follow-through, 6 back.
  The held item hangs off the main-hand arm (`LayerKangarooItem#translateToHand`), so raising the
  arm carries the spear with it and the two variants differ only in which arm that is.

An attack therefore costs ~22 ticks rather than the goal's 20 — `amCheckAndPerformAttack` only starts
an animation when the current one is `NO_ANIMATION`, so a spear-armed kangaroo attacks a hair slower.
Compile-green on `1.20.1-forge`, `1.21.1-fabric`, `1.21.4-neoforge`, `1.21.11-fabric`,
`26.2-neoforge`; the tag lands as `tags/items/` on 1.20.1 and `tags/item/` on 26.2.

**Client-confirmed on `26.2-fabric` (2026-08-08)** — the authored numbers read correctly and needed no
tuning. Captured deterministically as 36 frames: `/tick freeze`, then a loop of `/tick step 1` + F2,
assembled into a contact sheet. The sequence is legible frame by frame — rest → stand up → weapon
cocked back over the shoulder → **held there** → thrust forward → the target flashes red on the hit
tick → follow-through → return to rest.

Two rig notes that cost time and generalise to any melee-animation check:
- **A dummy must be vulnerable.** Vanilla's `canBeSeenAsEnemy()` is `!isInvulnerable() &&
  canBeSeenByAnyone()`, so an `Invulnerable:1b` entity is invisible to targeting and will simply
  never be attacked. Use a very tough one instead — `/attribute … minecraft:max_health base set 4000`
  plus `/data merge … {Health:4000f}` **after** summoning (the `Attributes:[…]` summon tag did not
  take), and `knockback_resistance base set 1` so it stays in frame.
- **The kangaroo hops out of frame.** `movement_speed base set 0` pins x/z but it still jumps ~1.7
  blocks, and `jump_strength base set 0` does nothing because the mob overrides `jumpFromGround`. A
  **glass ceiling** two blocks up (`fill … minecraft:glass`) cuts the hop to ~0.5 blocks and makes
  the pose readable.

## The thirteenth bug-report pass (2026-08-08 → 08-09) — five reports, six items, shipped in `2.0.10`

One reporter, five items, delivered twice — the second message added #70 and #71 to the first three.
One (#70) turned out to be far larger than the report and one (#71) was a **44-of-49-node** dead-code
fault that nobody had connected to the words used to report it. **#72 was found by the client session
that verified #71**, and is the more severe of the pair. All six are **client-confirmed on
`26.2-fabric` (2026-08-09)**.

| # | Report | Verdict |
|---|---|---|
| #67 | the transmutation table has an obsidian texture around it | **FIXED** — ≥1.21.4, all loaders |
| #68 | the murmur's hair moves strangely | **FIXED** — ≥1.21.2, all loaders |
| #69 | the mining ghost's pickaxe is not see-through like the official one | **FIXED** — 37 nodes |
| #70 | cannot place the leafcutter pupa on a grass block | **FIXED** — ≥26.1, all loaders |
| #71 | cannot put items into a capsid block | **FIXED** — ≥1.20.5, 44 nodes |
| #72 | *(found here)* putting an item into a capsid **disconnects the player** | **FIXED** — ≥1.20.5, 44 nodes |

### #67 — the transmutation table has "an obsidian texture around it" — FIXED, ≥1.21.4, all loaders

**Two halves, one boundary.** The block and its item are drawn by completely different code and both
were broken above 1.21.4; the reporter saw the block.

**The placed block, 30 nodes.** The transmutation table has no baked model at all — it is drawn
entirely by its BER, and upstream suppressed the model with `getRenderShape → INVISIBLE` inherited
from `BaseEntityBlock`. **1.21.4 deleted `BaseEntityBlock#getRenderShape`**; `BlockBehaviour` keeps
its own, defaulting to `MODEL`. So the three BER-only blocks in this mod — `BlockTransmutationTable`,
`BlockVoidWormBeak`, `BlockEndPirateShipWheel` — silently started drawing their placeholder models
underneath the BER. The table's placeholder is obsidian, which is the report, word for word. Fixed by
declaring `getRenderShape` on all three, **ungated**: the override is legal on both sides of the
boundary, it just stopped being inherited on one.

**The item, same boundary.** `transmutation_table`'s item model was `builtin/entity`, i.e. the
deleted ISTER, which is the #21/#23 family — so it joins the three items already served by
`AMIconSpecialRenderer` and is now the fourth entry in `DataPackMigration.LIVE_ICON_ITEMS`.
`withoutParent` strips the dangling `"parent": "builtin/entity"` while keeping the authored `display`
block, which the `minecraft:special` renderer does read.

⚠️ **A deleted *inherited* override leaves no trace.** No compile error (the method still exists on a
supertype), no mixin failure, no log line — the block simply starts rendering. `verify_overrides.py`
cannot see it either, because the class overrides nothing and never claimed to. The only detector is
the descriptor diff (`sigdiff.py`) on the *supertype*.

### #68 — the murmur's hair moves strangely — FIXED, ≥1.21.2, all loaders

`ModelMurmurHead` drives the hair off upstream's

```java
f1 += Mth.sin(Mth.lerp(partialTicks, entity.walkDistO, entity.walkDist) * 6.0F) * 32.0F;
```

and **1.21.2 deleted `Entity.walkDist` / `walkDistO`**. The port had substituted
`walkAnimation.position()`, which is the nearest-looking survivor and *is not the same quantity*:
vanilla accumulates `walkDist` at `√(dx²+dz²) · 0.6` per move, while the `WalkAnimationState` position
accumulates at roughly seven times that rate. Multiplied by 6 inside a sine, that is a hair
oscillation about **seven times too fast** — "moves in a somewhat strange way" is an accurate
description of it.

Fixed by giving `EntityMurmurHead` the pair back as its own: public `amWalkDist` / `amWalkDistO`,
accumulated in a `move(MoverType, Vec3)` override with vanilla's own formula, and shifted at the top
of `tick()`. The model then runs upstream's line verbatim on every node.

⚠️ **The two are not interchangeable and nothing says so.** Both are floats that grow as the entity
walks, both compile, and the difference only shows as a *rate*. When a deleted field has a
plausible-looking successor, check the accumulation, not the name.

### #69 — the mining ghost's pickaxe is not see-through — FIXED, 37 nodes

**The ghost look was never in the texture.** `ghostly_pickaxe.png` has exactly two alpha values, 0 and
255 — it is a fully opaque sprite. What makes it translucent upstream is the **render type**:
`GhostlyPickaxeBakedModel`, a Forge `BakedModelWrapper`, answers `getRenderTypes` with
`AMRenderTypes.getGhostPickaxe`, whose transparency shard is `LIGHTNING_TRANSPARENCY` — additive
blending, so the tool washes over whatever is behind it. The same wrapper also rewrites every vertex's
lightmap to `0x00F000F0`, so it glows in the pitch-dark tunnels this mob lives in.

That wrapper exists on **`<1.21.4 && !fabric` — 12 of 49 nodes**. 1.21.4 deleted the `BakedModel` hook
it wraps, and Forge's `getRenderTypes` was never a thing on Fabric at all. On the other **37** nodes
nothing selects the custom type, so vanilla's own choice stands, and for a non-`BlockItem`
`ItemBlockRenderTypes.getRenderType` returns `Sheets.translucentItemSheet()` — ordinary alpha
blending (bytecode-checked 1.20.1→26.2). An opaque texture through an alpha-blending render type is
simply a solid pickaxe.

Fixed at the two places the look actually comes from, without reinstating the render type:

- **`DataPackMigration.ghostifyPickaxeTexture`** lowers the sprite's alpha to 140 (~55%) at build
  time, on exactly the 37 wrapper-less nodes. Alpha blending reaches the same place additive blending
  did. The 12 nodes that *do* have the wrapper are deliberately skipped — additive blending multiplies
  by source alpha, so lowering it there would only dim upstream's own look. The pass is idempotent
  (never raises opacity, skips fully transparent pixels).
- **`LayerUnderminerItem`** passes `0x00F000F0` for the pickaxe at the one place the mob's tool is
  drawn — ungated, since where the wrapper runs it is already fullbright.

Rejected routes, each for a concrete reason: a `MultiBufferSource` wrapper **cannot reach the ≥1.21.9
submit path** (`AMRenderCompat.renderItemInHand` hands that era an `AMSubmitBuffers` collector, not a
buffer source); a mixin on `ItemBlockRenderTypes.getRenderType` dies at **1.21.11**, where the method
is deleted and `BlockModelWrapper` holds a baked `Function<ItemStack, RenderType>`; a custom
`ItemModel` in `ItemModels.ID_MAPPER` needs a layer-iteration accessor mixin plus five era arms; and
rebuilding the additive blend on ≥1.21.5 needs a per-loader `RenderPipeline` registration — the
regression `AMRenderTypes`' own header already documents as deferred.

⚠️ **Texture, model and render type are three separate places a "transparency" can live.** Reading the
PNG first (`[(0, 188), (255, 68)]` — no partial alpha anywhere) is what ruled out two of them in one
step.

### #70 — the leafcutter pupa will not go on grass — FIXED, ≥26.1, all loaders

Nothing is wrong with the item. `Item#useOn` is signature-stable 1.20.1→26.2, `ItemLeafcutterPupa` is
byte-for-byte upstream apart from the two compat helpers, and its gate is upstream's own:

```java
if (blockstate.is(AMTagRegistry.LEAFCUTTER_PUPA_USABLE_ON)
        && world.getBlockState(blockpos.below()).is(AMTagRegistry.LEAFCUTTER_PUPA_USABLE_ON)) {
```

`#alexsmobs:leafcutter_pupa_usable_on` is `#alexsmobs:am_spawns`, whose ground list leans on
**`#minecraft:dirt`** — and **MC 26.1 split that tag three ways.** Diffed straight out of the vanilla
client jars:

| version | `#minecraft:dirt` |
|---|---|
| 1.20.1 → 1.21.11 | dirt, grass_block, podzol, coarse_dirt, mycelium, rooted_dirt, moss_block, (pale_moss_block from 1.21.4), mud, muddy_mangrove_roots |
| **26.1 → 26.2** | dirt, coarse_dirt, rooted_dirt |

The seven that left were **re-homed, not deleted**: `#minecraft:grass_blocks` (grass_block, podzol,
mycelium), `#minecraft:moss_blocks` (moss_block, pale_moss_block) and `#minecraft:mud` (mud,
muddy_mangrove_roots). The boundary is exact — 1.21.11 has ten members, 26.1 has three.

So on the **6 nodes ≥26.1** the grass block stopped counting as ground, which is how the reporter met
it. **The pupa is the small half.** `#alexsmobs:am_spawns` is also the base of **fourteen mob spawn
tags** — emu, kangaroo, platypus, komodo dragon, roadrunner, crocodile, caiman, anaconda, mimic
octopus, alligator snapping turtle, rattlesnake, seal, fly, lobster — so all fourteen quietly stopped
spawning on grass on 26.1.2 and 26.2 as well, since `2.0.0`. Nothing logs, nothing throws: a spawn
placement test that returns false is indistinguishable from bad luck.

Fixed by a new resource pass, **`DataPackMigration.migrateDirtTagTo261`**, gated `>=26.1`: every mod
tag naming `#minecraft:dirt` also gets the three successor tags, restoring the pre-26.1 membership
exactly. It is a migration and not three more lines in `am_spawns.json` because **a tag reference to a
tag that does not exist is a hard load error**, and `#minecraft:grass_blocks` / `#minecraft:moss_blocks`
do not exist below 26.1. Verified in the generated resources: both 26.x nodes gain the three entries,
`1.21.11-fabric` (control) is untouched. No Java reads `BlockTags.DIRT`, and a full diff of **every**
vanilla tag this mod's data references (1.21.11 vs 26.2) found `dirt` — block and item — to be the
only one that changed.

⚠️ **Generalise this, it will happen again:** a vanilla tag can be *re-partitioned* without any
version's code changing, and the result is a silently narrower predicate rather than an error. The
detector is cheap — diff the membership of every `#minecraft:` tag the data pack references between
the old and new client jar, which is one script over two zips. Do it on every MC bump.

⚠️ **Read tag data from `~/.gradle/caches/fabric-loom/<mc>/minecraft-client.jar`**, not from the
merged loom artifacts under `minecraftMaven/` — those are code-only deobf jars with no `data/`
entries, so a lookup there returns nothing and reads as "the tag is gone".

### #71 — you cannot put items into a capsid — FIXED, ≥1.20.5, 44 nodes

Not the capsid: **seven blocks had a dead right-click on 44 of 49 nodes**, and had since `2.0.0`.

**1.20.5 split `BlockBehaviour#use`** into `useItemOn(ItemStack, …)` — the same position in the
right-click dispatch — and `useWithoutItem(…)`. A class still declaring `use` therefore overrides
nothing: it compiles, it loads, and vanilla never calls it. Right-clicking the block falls straight
through to the held item's own `useOn`, which for an arbitrary item is a no-op. "You can't place items
inside a capsid block, and even other blocks don't fix that" is exactly that — the *block* never got
the click, so nothing about the held item could change the outcome.

Seven blocks were in this state: `BlockCapsid`, `BlockEndPirateShipWheel`, `BlockLeafcutterAntChamber`,
`BlockHummingbirdFeeder`, `BlockTransmutationTable`, `BlockLeafcutterAnthill`, `BlockEndPirateDoor` —
i.e. **every interactive block this mod adds**. Each now keeps upstream's body in a private `amUse`
and declares two thin arms over it:

```java
//? if >=1.20.5 {
/*protected ItemInteractionResult useItemOn(ItemStack amStack, BlockState state, Level worldIn, …) {
    return AMCompat.itemResult(amUse(state, worldIn, pos, player, handIn, hit));
}
*///?}
//? if <1.20.5 {
public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, …) {
    return amUse(state, worldIn, pos, player, handIn, hit);
}
//?}
```

`AMCompat.itemResult` absorbs the **second** boundary: 1.20.5–1.21.1 returns `ItemInteractionResult`
(so `PASS` has to become `SKIP_DEFAULT_BLOCK_INTERACTION`, not `PASS`), and from **1.21.2** the return
type is plain `InteractionResult` again, where the helper is the identity. Two boundaries a version
and a half apart, in the same signature — pin each from bytecode.

⚠️ **This is the #66 / #61 family again, and it is the largest instance of it so far.** A method that
loses its override target is invisible to the compiler, to all four mixin verifiers and to every gate;
the only thing that sees it is `scripts/verify_overrides.py`, which is a review list rather than a
gate. Run it per node after `compileJava`, with `--baseline=1.20.1-forge`.

### #72 — putting an item into a capsid disconnects you — FIXED, ≥1.20.5, 44 nodes

**Not reported — found by the client session that was verifying #71.** With #71's fix live, the
right-click finally reached the block, and the very first diamond dropped into a capsid **kicked the
player off the server**. Two faults had been stacked: #71 meant nobody could ever get far enough to
hit this one, so it has been latent since `2.0.0` on every node ≥1.20.5, all three loaders.

**Cause: re-wrapping a buffer you were handed.** Vendored Citadel's `PacketBufferUtils` is typed on
the raw `ByteBuf`, and both stack helpers opened with `new FriendlyByteBuf(to)`. That is lossless
below 1.20.5 and destructive from 1.20.5 up: a stack's components need the registries, so
`ItemStack`'s stream codec runs over a `RegistryFriendlyByteBuf` and `AMCompat.writeItem` **casts** to
one — but the connection hands the message its registry-carrying buffer, and the re-wrap produced a
*plain* `FriendlyByteBuf` around the same bytes. The cast then threw `ClassCastException` **inside the
packet encoder**, which vanilla treats as a fatal connection error rather than a logged one.

Fixed with a `wrap(ByteBuf)` that adopts a `FriendlyByteBuf` instead of nesting one, used by all four
helpers (the two NBT ones never hit it — NBT needs no registries — but the rule now holds for the
whole class).

⚠️ **This is the third form of the same mistake** and the pattern is worth naming: **never stage a
payload into a buffer you allocated, and never re-wrap one you were given.** Report #24 was
`AMNeoNetwork` pre-encoding into a hand-allocated `FriendlyByteBuf`; `AMFabricNetwork` was written to
avoid exactly that and carries a javadoc saying so — and the fault was still there, one level below,
in a helper neither network class owns. Only two messages route stacks through it
(`MessageUpdateCapsid`, `MessageUpdateTransmutablesToDisplay`), which is precisely why the symptom was
"the capsid and the transmutation table", the same two blocks #71 named. The kangaroo messages call
`AMCompat.writeItem` straight on the message's own buffer, which is why #24's fix left this untouched.

**Diagnostic worth reusing:** `data get block <pos>` showed the capsid already holding **7 diamonds**
from the pre-crash attempts. The server-side insert had always worked; only the sync packet died. A
"nothing happened and I got kicked" report where the *world state did change* points at the packet,
not the interaction.

Compile-green on `26.2-fabric`, `1.21.1-neoforge`, `1.20.6-forge` and `1.20.1-forge` — both
`writeItem` arms, all three loaders — and client-confirmed on `26.2-fabric`: eight diamonds went into
the capsid across two sessions with no disconnect and `EncoderException|ClassCastException|lost
connection` at **0** in the server log.

### Verification of the thirteenth pass (2026-08-09)

All **49** nodes built in one invocation from a cleared tree (`versions/*/build/resources` moved away
first — rule 9 — because two of the fixes are resource passes), `BUILD SUCCESSFUL`, exit 0.

Predicted the counts before running, per rule 6:

| check | predicted | actual |
|---|---|---|
| `ghostifyPickaxeTexture` fires | 37 nodes (49 − the 12 on `<1.21.4 && !fabric`) | **37**, 0 skipped-as-already-translucent |
| `migrateDirtTagTo261` fires | 6 nodes × 1 file (only `am_spawns` names the tag) | **6 × 1** |
| `verify_mixins.py` | `jars=49` | `jars=49 problems=0` |
| `verify_mixin_targets.py --all-versions` | `jars=49`, ~1006 selectors (no mixin changed this pass) | `nodes=49 jars=49 selectors=1006 problems=0 skipped=0` |
| `verify_assets.py` | `literals=394 missing=0` (unchanged from `2.0.9`) | `literals=394 missing=0` |

⚠️ **The first `verify_mixins.py` run reported `jars=98`** — the `2.0.9` release jars were still in
`versions/*/build/libs/` beside the fresh `-SNAPSHOT` ones, so every node was scanned twice. Green
either way, but it is the same trap as rule 6 in reverse: a count that is *higher* than predicted
means the verifier is reading something you did not build. 147 stale jars moved to
`/tmp/amc-jar-trash/2.0.9-release/` before re-running.

**`verify_overrides.py --baseline=1.20.1-forge` on five era-spanning nodes** (`1.20.6-neoforge`,
`1.21.1-fabric`, `1.21.5-neoforge`, `1.21.11-fabric`, `26.2-neoforge`) confirms the seven #71 blocks
now resolve — no `use` hit remains anywhere — and leaves the known backlog at ~33 per node. Several of
those are player-visible and **not yet triaged**, notably:

- `BlockBananaPeel#entityInside` — signature gained `InsideBlockEffectApplier, boolean`, so the peel
  presumably stopped being slippery.
- `Mob#wantsToPickUp` gained a `ServerLevel` (underminer), `getExperienceReward` gained
  `(ServerLevel, Entity)` (murmur head, void worm), `handleAirSupply` gained a `ServerLevel` (six
  aquatic mobs), `canBeLeashed(Player)` lost its parameter (bone serpent, centipede, void worm),
  `awardKillScore` widened to `Entity` (tiger, bald eagle), `Block#neighborChanged` swapped a
  `BlockPos` for an `Orientation` (four End Pirate blocks), `updateShape` was rewritten wholesale
  (crystalized mucus), `propagatesSkylightDown` lost its level/pos (banana slug slime).

The compat classes' own hits (`compat/EntityModel#renderToBuffer`, `compat/EntityRenderer` and
`compat/LivingEntityRenderer#shouldShowName`) are the shims doing their job and are expected.

## The fourteenth bug-report pass (2026-08-09) — one report, against published `2.0.10`

### #73 — the transmutation table crashes the server — FIXED, 28 nodes

Reported as a bare stack trace, no words:

```
java.lang.IllegalArgumentException: Parameters not allowed in this parameter set: [<parameter minecraft:this_entity>]
    at net.minecraft.world.level.storage.loot.LootParams$Builder.create(LootParams.java:136)
    at ...TileEntityTransmutationTable.createFromLootTable(TileEntityTransmutationTable.java:57)
    ... rollPossiblity → randomizeResults → tick → commonTick → LevelChunk → Level.tickBlockEntities
    → ServerLevel.tick → MinecraftServer.tickChildren/tickServer/runServer
```

**The reporter's platform is `1.20.1-fabric`**, pinned from the trace's own line numbers rather than
asked for: `LootParams.java:136` and `LevelChunk.java:662/716` are identical in 1.20.1 and 1.20.4, but
`MinecraftServer.java` **265 / 671 / 824 / 897** all resolve in the 1.20.1 jar and only `897` does in
1.20.4. (`Thread.java:840` had already narrowed it to a Java-17 version, i.e. ≤1.20.4.) Worth reusing:
**a Fabric trace with no mod name in it is still a version fingerprint** — javap the `LineNumberTable`
of two or three vanilla frames against the cached mapped jars.

**Cause — upstream's, latent for two years, and it is a loader-patch story.** `createFromLootTable`
sets `LootContextParams.THIS_ENTITY` on the builder and then calls `create(LootContextParamSets.EMPTY)`.
`EMPTY` allows *nothing*, so vanilla's "parameters not allowed" check fires on every single roll. It
had never been seen because of who patches that check out:

| platform | check present? | outcome |
|---|---|---|
| **Forge ≤1.21.1** | no — Forge's patch computes the not-allowed set and then never reads it (verified in bytecode on 1.20.1, 1.20.4, 1.20.6, 1.21, 1.21.1) | works; this is what upstream shipped on |
| **Forge ≥1.21.3** | **yes** | crashes |
| **NeoForge, every version 1.20.4→26.2** | no — patched out on all of them | works |
| **vanilla / Fabric, every version** | **yes** | crashes |

The reason Forge's coverage stops at 1.21.1 is that **1.21.2 moved the validation out of
`LootParams.Builder.create` and into the new `ContextMap.Builder.create(ContextKeySet)`** — Forge's
patch stayed on the old method, which from 1.21.2 just delegates, so the check came back. NeoForge
carried its patch across to the new class; Forge did not. So the affected set is **all 17 Fabric nodes
plus Forge 1.21.3 → 26.2 (11 nodes) = 28 of 49**, and **zero NeoForge**, which is why every previous
report of "the transmutation table" (#67, #71, #72 — all from NeoForge/Forge-≤1.21.1 players) stopped
short of it.

**Why now:** the roll fires from `MenuTransmutationTable`'s constructor (`if(!table.hasPossibilities())
setRerollPlayerUUID`), i.e. on the tick after you simply *open* the table. On the 44 nodes ≥1.20.5 the
menu could not be opened at all until **#71** was fixed in `2.0.10`, so `2.0.10` is the release that
exposed it there. On `1.20.1`/`1.20.4` Fabric — where `use` always worked — it has been crashing since
`2.0.0`.

**Fix:** pass `LootContextParamSets.PIGLIN_BARTER`, the vanilla set whose only member is a *required*
`THIS_ENTITY`, so the same parameter map now validates instead of being rejected. Verified in bytecode
on 26.2 that `PIGLIN_BARTER` is `required(THIS_ENTITY)` and nothing else. This is exactly what the
mod's five other "roll one item, entity as context" call sites already do
(`SealAIDiveForItems`, `PlatypusAIDigForItems`, `AnteaterAIRaidNest`, `EntitySugarGlider`,
`EntityDevilsHolePupfish`), so the table is no longer the odd one out.

⚠️ **`ALL_PARAMS` (`minecraft:generic`) is not the answer**, even though it is the param set these
three loot tables themselves declare by omitting `"type"`. Vanilla builds it with `required(...)` for
*every* key, so `create(ALL_PARAMS)` would trade "parameters not allowed" for "missing required
parameters". It is the set a *table* is validated against, never one you hand to a builder.

The other seven `create(...)` sites in the mod were swept and are all sound: `EntityElephant`'s
`create(EMPTY)` sets no parameters, `ShoebillAIFish` builds an empty set for an empty builder, and the
five above already use `PIGLIN_BARTER`.

⚠️ **The generalisation, and it is the same shape as #71/#72:** a vanilla *validation* can be absent on
the loader upstream developed against, so upstream code can carry a hard error that only the port's
other loaders ever see. Do not assume "it works on Forge 1.20.1" means the call is legal — and do not
assume a Forge patch survives a refactor of the method it patched. When a check moves class, re-check
every loader's coverage from bytecode, per MC version.

Compile-green on `1.20.1-forge`, `1.20.1-fabric`, `1.21.5-forge`, `1.21.11-neoforge` and `26.2-fabric`
in one invocation, and **client-confirmed on `1.20.1-fabric`** — the reporter's own node — before the
`2.0.11` upload.

**How it was confirmed, and the two false negatives on the way.** Rig: a dedicated `runServer` +
`runClient --quickPlayMultiplayer 127.0.0.1:25565`, driven over RCON (`scripts/rcon.py`, port 25575)
with `ydotool` for **keyboard only**. Positive evidence, not just an absent crash: after one press of
use on the table, a full-log scan for `Parameters not allowed|IllegalArgumentException|Ticking block
entity` found **0** new lines, and `data get block 8 64 8` showed the roll had actually run —
`Possiblity0: {id: "minecraft:dirt"}, Possiblity1: {id: "minecraft:redstone"},
Possiblity2: {id: "minecraft:gilded_blackstone"}`. A screenshot showed the GUI open with the server
alive.

⚠️ **The table's floating orb is a block-entity render, not its interaction shape.** Pointing the
crosshair at what looks like the table hit the block *behind* it, and the use key looked dead — chased
through key-hold duration, `options.txt` and a disassembly of `Minecraft#handleKeybinds` before **F3**
settled it (`Targeted Block: 8, 64, 3 minecraft:stone`). Aiming steeply down (pitch 35) made it read
`alexsmobs:transmutation_table` and the press worked first try. **Confirm the F3 target line before
concluding a keypress did nothing.** Linux keycodes: **F3 = 61**, F5 = 63 — pressing 63 for F3 just
flips to third person.

⚠️ **The three empty offer rows in the open GUI are correct, not a second bug.**
`GUITransmutationTable#renderItemsTransmute` guards every one on
`!this.menu.getSlot(0).getItem().isEmpty()`, so nothing draws until an item is in the input slot.

## The fifteenth bug-report pass (2026-08-09 → 08-10) — five items, against published `2.0.11`

One reporter, **MC 1.21.11, loader not stated** (they only said "playing on 1.21.11"). Five items,
verbatim:

> - Bugs that need fixing, both in the achievements and in the dictionary; the animals have black
>   textures. and also in the animated animals creative tab
> - The mysterious worm is missing its animation; please add it from the original mod and also fix this.
> - The Emderiophagous video has a part that doesn't match the one in the dictionary where it's listed
>   in its original purple section fix this if possible.
> - Some parts of the animals are not displayed correctly, and the animals in the dictionary have low
>   pixel quality. Please correct this and check the images.
> - Just one question: is it normal for the underside of the tarantula hawk wasp's wings to be dark?

Three faults (#74, #75, #76), one open item needing the screenshots (#77) and one non-bug (#78).
**Nothing here is loader-specific** — every boundary is an MC-version one — so the missing loader
never blocked the pass.

### #74 — mobs render BLACK (or BLUE) in the advancement screen, the animal dictionary and the animated creative-tab icon — FIXED, 15 nodes

One fault, three symptoms, and the reporter listed all three: **every mob this mod draws in a GUI is
lit by the light level of wherever its invisible fake entity happens to be standing.** At night or
underground — most of the time — that is 0, so the mob is a black silhouette.

⚠️ **A second reporter described the same bug as the dictionary mobs being *blue*, and it is not a
different fault.** The fake entity is created and never positioned (`AMCompat.createForDisplay` →
`create(type, level)` + `setId(-1)`, no `moveTo`/`setPos` anywhere), so it stands at **(0, 0, 0) of
the player's own level**, and `getPackedLightCoords` packs `getBrightness(BLOCK, pos)` with
`getBrightness(SKY, pos)` from there. Block light is 0 either way; the *sky* term is whatever y=0 of
that world happens to have — 0 buried in stone (→ black), 15 where the chunk is unloaded or the
column is open (→ the sky-only end of the lightmap, which is blue-tinted at anything short of full
daylight). So the reported colour varies with the player's dimension, time of day and loaded chunks,
and **black, blue and "washed out" are all the same missing write.** Don't re-diagnose by colour.

**Cause — the 1.21.9 render-state migration, exactly the shape of #66/#71.** Through 1.21.8 the light
was an *argument*: `EntityRenderer.render(state, poseStack, buffers, packedLight)`, and
`EntityRenderDispatcher.render(entity, …, packedLight)` above it. 1.21.9 deleted that parameter —
`submit(state, poseStack, collector, camera)` has no light — and moved three per-frame values **onto
`EntityRenderState`**: `lightCoords`, `outlineColor` and `shadowPieces`, all filled by
`extractRenderState`/`finalizeRenderState` from the entity's own block position. javap-verified: the
three fields exist on 1.21.11 / 26.1.2 / 26.2 and **do not exist** on 1.21.6 / 1.21.8.

The port's two GUI seams each dropped the light on the floor at that boundary:

| seam | drives | what it did |
|---|---|---|
| `AMRenderCompat.renderEntity`, `>=1.21.9` arm | the 59 advancement icons and the cycling creative-tab icon (via `AMItemstackRenderer.drawEntityOnScreen`) | took a `packedLight` parameter, called `raw.submit(...)`, and **never used it** — `drawEntityOnScreen` passes `15728880` and it was discarded |
| `AMRenderCompat.submitGuiEntity`, `>=1.21.6` arm | both dictionary sites (index slots + the mob's own page) | built the state and submitted it without ever setting the light |

Below 1.21.9 neither could go wrong: `renderEntity`'s 1.21.2–1.21.8 arm passes `packedLight` straight
to `render(...)`, and vanilla's own `GuiEntityRenderer` hands `EntityRenderDispatcher#render` a
hardcoded `15728880`. **So the affected set is exactly the 15 nodes ≥1.21.9, all three loaders,
broken since `2.0.9`** — which is why it arrived as a 1.21.11 report.

**Fix**, two lines and a helper:

- `renderEntity`'s ≥1.21.9 arm now writes `state.lightCoords = packedLight;` before `submit`, which
  reproduces the pre-1.21.9 call exactly for *every* caller — the in-world nested renders (kangaroo/
  anteater pouch, the squid in a cachalot's mouth, the falconry bird) pass the enclosing renderer's
  light and were equally wrong, just far less visible.
- `submitGuiEntity` now calls the new **`AMRenderCompat.guiEntityFullBright(state)`**, which sets
  `lightCoords = 15728880`, `outlineColor = 0` and `shadowPieces.clear()` — the same three vanilla's
  own `InventoryScreen#extractRenderState` sets, for the same reason.

⚠️ `guiEntityFullBright` is a **separate method taking `Object`** purely because **Stonecutter blocks
never nest**: its body's boundary (1.21.9) sits inside `submitGuiEntity`'s (1.21.6), and a `//? if
>=1.21.9` block cannot be opened inside a `//? if >=1.21.6` one. The `Object` parameter is what lets
the signature exist on nodes where `EntityRenderState` has no such fields.

⚠️ **Generalisation, and it is the fourth of this shape after #30/#66/#71: when a version moves a
value from an argument to a field, the call still compiles.** Deleting a parameter from an interface
method is a compile error at the override; *replacing* it with a field the caller is expected to fill
is invisible — the port kept taking a `packedLight` it no longer had anywhere to put, and neither the
compiler, `verify_mixins.py` nor `verify_overrides.py` can see that. Whenever a bump deletes a
parameter, grep the *callers* for the now-unused local.

### #75 — the mysterious worm lost its animation and went 2D — FIXED, 30 nodes

Not "missing from the port" — **downgraded by the ≥1.21.4 item-model rebuild.** The worm's item model
is `"parent": "builtin/entity"`, i.e. pure ISTER; 1.21.4 deleted the ISTER, and this tree's
`DataPackMigration.repairBuiltinEntityModel` rebuilds every such model into something real. Its
fallback chain ends at "there is a `textures/item/<id>.png`, so make an `item/generated` sprite" —
and `mysterious_worm` has one, so it quietly became **the flat 2D icon** instead of the wriggling 3D
model. The animation was still there; nothing was calling it.

**Fix:** add `mysterious_worm` to **`LIVE_ICON_ITEMS`** in `DataPackMigration`, so it gets a
`minecraft:special` model definition (`{"type":"alexsmobs:icon"}`) routed through
`AMIconSpecialRenderer` → `AMItemstackRenderer.renderByItem`, the same seam #45/#48 restored the
creative-tab and advancement icons on and #67 restored the transmutation table on. Its base model
keeps its full `display` block (`withoutParent()` preserves everything but the parent), so every
transform is upstream's.

⚠️ **The qualifying test for `LIVE_ICON_ITEMS` membership is that the item's `renderByItem` branch
must not read `transformType`** — the 26.x `submit` arm has no display context and hardcodes `GUI`.
The worm's branch (a translate, a rotate, `animateStack`, `renderToBuffer`) reads nothing, so it
qualifies; the in-hand items that *do* branch (falconry glove, vine lasso, …) still must not join.

The set is now five: `tab_icon`, `fancy_item`, `effect_item`, `transmutation_table`,
`mysterious_worm`. Verified in the generated resources on `1.21.11-fabric` and `26.2-neoforge` after
clearing `build/resources` — `assets/alexsmobs/items/mysterious_worm.json` is the `special` model and
`models/item/mysterious_worm.json` kept all seven display entries.

### #76 — the enderiophage's (and guster's and spectre's) glow layer draws nothing — FIXED, 35 nodes

This is the reporter's *"the Enderiophage video has a part that doesn't match the one in the
dictionary"* (the wiki/promo footage shows the phage's glowing purple markings, the game does not)
and part of *"some parts of the animals are not displayed correctly"*.

**Cause — a compat-shadowed name that was never shadowed.** This tree restores the pre-1.21.2 layer
API by rewriting the whole *import statement*
`net.minecraft.client.renderer.entity.layers.RenderLayer` to the compat class, so a mod layer keeps
its ten-argument `render(poseStack, buffers, light, entity, …)` and its entity type parameter. Three
layers were written against vanilla's **`EyesLayer`** instead, which is not in that rule set — so on
≥1.21.2 they stayed vanilla subclasses whose ten-argument `render` is an *overload nothing calls*,
while vanilla's own `EyesLayer#render` ran and drew through `Model#renderToBuffer`. That method is
`final` from 1.21.2 and walks the **empty root** the compat `EntityModel` hands vanilla, so it drew
nothing at all.

| layer | what is lost | note |
|---|---|---|
| `RenderEnderiophage.EnderiophageEyesLayer` | the glowing markings, all three variants | its `getRenderType(entity)` (the variant-aware texture pick) also never ran |
| `RenderGuster.GusterEyesLayer` | the glowing eyes, incl. the soul-sand variant's | |
| `RenderSpectre.SpectreEyesLayer` | the glowing eyes | had **no `render` of its own at all** — only a `renderType()` for vanilla to use |

**Fix:** all three now `extend RenderLayer<TheEntity, TheModel>` (the shadowed import), keeping their
ten-argument bodies; the now-dead `renderType()` overrides are deleted, and Spectre's got the
two-line body written (vanilla's `EyesLayer#render`, against the compat model's own eight-float
`renderToBuffer`). This is the shape `LayerSoulVultureGlow` and `RenderSpectre.SpectreMembraneLayer`
— in the same file — already had.

Affected: **every node ≥1.21.2, all three loaders (35 of 49), since `2.0.0`.**

⚠️ A sweep confirms these were the last three: every other layer in the mod already extends the
shadowed `RenderLayer`, and `LayerRainbow` is the one deliberate exception (`StateRenderLayer`,
because it is attached to *vanilla* renderers). `verify_overrides.py` on `1.21.11-fabric` now reports
no dead override anywhere under `client/render/`.

⚠️ **`verify_overrides.py` is what found this**, and it is worth restating why the compiler could
not: the class *did* override something (vanilla `EyesLayer#renderType`), so nothing was missing —
the ten-argument `render` was simply an extra method. Only a superclass-chain walk sees it.

### #77 — "the animals in the dictionary have low pixel quality" — OPEN, blocked on the screenshots

Investigated and **no cause found**; the reporter said "check the images" but none were attached.
What was ruled out, from the 1.21.11 sources:

- The 1.21.6+ picture-in-picture path is provably **1:1**. `PictureInPictureRenderer.prepare`
  allocates a texture of `(x1-x0) * guiScale × (y1-y0) * guiScale`, scales the model by
  `guiScale * scale`, and blits it back with **NEAREST** filtering. There is no downsample anywhere.
- `GuiEntityRenderer.getTranslateY` returns `j / 2.0F`, so the entity origin lands at the rect's
  centre — which is what `submitGuiEntity`'s comment already claims and what #62's viewport maths
  assumes.
- #62's viewport widening only grows the rect; it does not change how many texture pixels a
  model-pixel gets.
- `AMGuiEntityPipPool` (#56's fix) hands out one renderer per entity per frame; re-read in full, no
  fault.

**Most likely it is #74** — a black-lit mob reads as "low quality" — in which case it is already
fixed. Ask for the images, and ask whether it persists after the next release before spending more
on it.

### #78 — "is it normal for the underside of the tarantula hawk wasp's wings to be dark?" — YES, not a bug

`ModelTarantulaHawk`'s wings are **zero-height boxes** (`addBox(0, 0, -1, 20, 0, 21, 0)` — a 20×21
plane, thickness 0), so each wing is a single flat quad with a downward-facing side. Vanilla's
directional lighting shades a downward normal darkest, which is why the underside is dark. Both
`ModelTarantulaHawk` and `RenderTarantulaHawk` are byte-identical to the upstream baseline, so this
looks exactly the same in the original mod. No change.

## The sixteenth bug-report pass (2026-08-12) — one report, against published `2.0.12`

One report, forwarded verbatim, **MC version and loader not stated**:

> If you use it in lava it will put you on fire not move but have animations of turning and after you
> leave the board it will use the built up momentum and fly off

Four symptoms, **one cause** (#79) — and fixing it turned up a second, unrelated fault in the same
entity that then turned out to be a family of four (#80). Neither is client-verified yet.

⚠️ The diagnosis **predicts the reporter is on 1.21.5 or newer**; on 1.20.1–1.21.4 the board is
unaffected by #79 entirely. Worth confirming with them, but not worth blocking on — the boundary is
pinned from bytecode.

### #79 — the straddleboard freezes, burns its rider and flies off on dismount — FIXED, 27 nodes

**All four symptoms are the same fault: on ≥1.21.5 the board became client-authoritative, and the
client runs no physics for it.**

`EntityStraddleboard` declares, from upstream:

```java
public boolean isControlledByLocalInstance() { return false; }
```

That "false" is load-bearing and deliberate. The board's whole simulation (`tickMovement`) lives in
the **server** branch of `tick()`; the `isClientSide` branch only lerps toward whatever the server
sent. Hand the board to the client and nothing moves it at all.

**1.21.5 deleted `Entity#isControlledByLocalInstance` and split it in two.** The caller-facing
replacement `isLocalInstanceAuthoritative()` is **`final`** — it cannot be overridden — and reads:

| side | it asks | default |
|---|---|---|
| client | `isLocalClientAuthoritative()` (`protected`) | the controlling passenger's answer |
| server | `!isClientAuthoritative()` (`public`) | the controlling passenger's answer, negated |

and `Player.isClientAuthoritative()` returns **`true`**. So any vehicle carrying a player is
client-authoritative unless it says otherwise.

**Why the port did not catch it.** `stonecutter.gradle.kts:1095` renames the *call* form only:

```kotlin
string("!mc2105-localauth", true) { replace(".isControlledByLocalInstance()", ".isLocalInstanceAuthoritative()") }
```

The leading dot is what makes it a call-site rule, and it is what the mod's two other users
(`EntityFarseer:509`, `EntityLaviathan:536`) need. But a **declaration** has no leading dot, so
upstream's override was left behind under a name nothing calls — silently-dead code, invisible to the
compiler, to both mixin verifiers and to `verify_overrides.py` (which is blind to renames by
construction; see #66).

**The failure chain, and how each symptom falls out of it:**

1. The client owns the board and never moves it → **it does not move.**
2. `LocalPlayer` pushes that frozen position to the server every tick as a
   `ServerboundMoveVehiclePacket`; `ServerGamePacketListenerImpl#handleMoveVehicle` does
   `vehicle.absSnapTo(...)`, pinning the *server* board to the frozen client position.
3. `ClientPacketListener#handleEntityPositionSync` skips its entire body when
   `isLocalInstanceAuthoritative()`, so the client also **ignores the server's corrections** — the
   two ends agree on a wrong answer and there is nothing to break the loop.
4. `BOARD_ROT` is **synched data driven by yaw, not by position**, and the yaw still tracks the
   rider → **the turning animation plays normally on a board that is standing still.**
5. The server's `tickMovement()` keeps running: `prev.scale(0.975F).add(moveVec)` with `moveForwards`
   capped at `0.115` converges on `0.115 / 0.025` = **4.6 blocks per tick**, and `deltaMovement` is
   never spent because the position is being overwritten. Dismount, the packets stop, and the board
   **releases all of it at once.**
6. **Lava.** `ItemStraddleboard` places the board at the ray hit using `ClipContext.Fluid.ANY`, so
   right-clicking *while standing in* lava starts the ray inside the fluid shape and spawns the board
   **submerged**. A working board climbs out at `+0.1`/tick (`isInLava()` gravity) in 10–20 ticks;
   a frozen one never does, so the rider sits in lava indefinitely. The board itself is `fireImmune`
   and `extinguishTimer` clears the *player's* fire every server tick — but only while the player is
   actually on it, and reignition wins. **The lava is a trigger, not the fault**: the same freeze
   happens on land, it is just less dramatic.

There is a sixth, latent consequence: with `isLocalInstanceAuthoritative()` false on the *server*,
`Entity#move` skips `setOnGroundWithMovement`, so `onGround()` never became true and the board's
on-land friction term (`0.05` instead of `0.98`) never applied either.

**Affected set: the 27 nodes ≥1.21.5** — 1.21.5/6/7/8/9/10/11, 26.1.2, 26.2 × all three loaders —
**shipped broken since `2.0.0`.**

**Fix.** Override the two *feeders*, not the final method:

```java
//? if >=1.21.5 {
/*@Override
public boolean isClientAuthoritative() { return false; }

@Override
protected boolean isLocalClientAuthoritative() { return false; }
*///?} else {
@Override
public boolean isControlledByLocalInstance() { return false; }
//?}
```

Verified from the cached mapped jars across all 18 MC versions: `isLocalInstanceAuthoritative()` is
`public final` on 1.21.5→26.2 (so overriding *it* would not compile), and both feeders exist and are
overridable on exactly that range.

⚠️ **Generalisation — a rename rule that matches a call form does not rename the declaration.** Every
`.method()` replacement in `stonecutter.gradle.kts` has this hole by construction, and the result is
the port's favourite failure mode: code that compiles everywhere and does nothing. When adding one,
check whether the mod *declares* the method as well as calling it. `addPassenger`'s
`this.isControlledByLocalInstance() && this.lSteps > 0` was correctly rewritten by the same rule and
is unaffected (`lSteps` is always 0 server-side either way).

### #80 — riders sit too high on four entities — FIXED, 47 nodes, never reported

Found while fixing #79, and the straddleboard was only the first of four.

**1.20.2 deleted `Entity#getPassengersRidingOffset()`** and moved the seat onto the entity type's
attachment points. The `EntityAttachment.PASSENGER` fallback for a type that declares none is
**`AT_HEIGHT`** — `(0, height, 0)`, the *full* height — where the old method's vanilla default was
`bbHeight * 0.75`. So two different things went wrong at once:

- an entity that **overrode** the old method lost its override entirely (dead code, same shape as
  #79, and equally invisible), and
- an entity that **relied on the default** silently gained `0.25 × height`.

Four entities are affected. The other seven that declare `getPassengersRidingOffset` (gorilla,
endergrade, komodo dragon, grizzly bear, tusklin, elephant, laviathan) also override
`positionRider(Entity, MoveFunction)` and **self-call** the helper, so they were never broken; the
six that only override `positionRider` (cosmaw, tarantula hawk, bald eagle, warped mosco, skelewag,
crocodile) place their rider by hand.

| entity | rider | height | upstream seat | what ≥1.20.2 gave it |
|---|---|---|---|---|
| straddleboard | the player | 0.35 | `+0.5` (stands **on** the board) | `+0.35` — ankle-deep in it |
| raccoon | the blue jay | 0.9 | `0.45 × h` = 0.405 | 0.9 — floating half a block above its back |
| kangaroo | the joey (`AnimalAIRideParent`) | 1.5 | `0.35 × h` = 0.525 | 1.5 — a block above its mother's head, not in the pouch |
| anteater | the baby (`AnimalAIRideParent`) | 1.1 | vanilla default `0.75 × h` = 0.825 | 1.1 — a quarter block above her back |

Each now carries a gated `getPassengerRidingPosition(Entity)` on `>=1.20.2` restating the upstream
seat, with the original `getPassengersRidingOffset` kept for the two 1.20.1 nodes (the anteater gets
only the new arm — upstream never declared one there, it just wanted vanilla's old default).

Explicitly checked and **not** affected: the five shoulder-riders that ride the **player**
(capuchin monkey, sugar glider, potoo, bald eagle, crow) and the two that ride their target (crimson
mosquito, enderiophage) all override `rideTick()` and set an absolute position, so no attachment is
consulted. `EntityType.PLAYER` declares only a `vehicleAttachment` and no passenger one, and `Player`
overrides none of the seating methods on any version — so that path *would* have drifted if any of
them had relied on it.

### Verification of the sixteenth pass (2026-08-12)

- Both boundaries pinned from the cached mapped vanilla jars across all 18 MC versions
  (`scripts/verify_overrides.py`'s class parser, driven by a small probe script).
- Compile-green in **one invocation** on 7 era-spanning nodes covering both sides of both
  boundaries: `1.20.1-forge`, `1.20.1-fabric`, `1.20.4-forge`, `1.21.4-neoforge`, `1.21.5-neoforge`,
  `1.21.11-fabric`, `26.2-neoforge`.
- Generated Stonecutter projections spot-checked on `1.20.1-fabric` / `1.20.4-forge` /
  `26.2-neoforge`: all gate arms comment and uncomment on the correct side of both boundaries.
- `verify_overrides.py --baseline=1.20.1-forge` on `26.2-neoforge`, `1.21.11-fabric` and
  `1.20.4-forge`: **no riding-, passenger-, vehicle- or authority-related hit remains** on any of
  them. (The ~67 pre-existing untriaged hits are unchanged — still the fourteenth-pass backlog.)
- **No client session yet.** Owed: a ≥1.21.5 node (`26.2-fabric` is the cheapest) — ride the board on
  land and confirm it moves under the rider, then place one from inside a lava pool and confirm it
  surfaces and the rider stops burning. The four seat fixes are visible in the same session (perch a
  blue jay on a raccoon; breed a kangaroo or an anteater).

## The seventeenth bug-report pass (2026-08-12 → 08-13) — six reports, against published `2.0.13`

Six items, `#81`–`#86`, all fixed in the tree and shipped in `2.0.14`. Two of them (`#81`, `#85`)
are *whole families of mobs not working at all* on a large slice of the matrix, and one (`#83`) is
an explicit request to fix something upstream also gets wrong — the first deliberate AI divergence
this port has taken on purpose.

⚠️ **An earlier session wrote a `#81` section into this file describing a `@ModifyVariable` on
`startRiding`'s return value, "49 nodes", and a verification block claiming compile runs that never
happened.** None of it matched the tree. It has been deleted and replaced by what follows; the real
fix is a `@Redirect` on `EntityType#canSerialize`. If a claim in these notes cannot be matched to a
line in `src/`, distrust the note, not the source.

### #81 — a crimson mosquito latches on and never lets go — FIXED, 35 nodes

> *"I'm playing on a server with the latest version (2.0.13+26.2-fabric) and whenever me or another
> player get latched on by a Crimson Mosquito, it just stays there forever without sucking any blood
> or leaving. The only way we can stop it is by having another player kill it or by leaving the
> server."*

**1.21.2 added a guard to `Entity#startRiding` that no mob in this mod can pass when the vehicle is
a player:**

```java
if (!this.level().isClientSide() && !vehicle.type.canSerialize()) return false;
```

`EntityType.PLAYER` is built with `noSave()`, so `canSerialize()` is `false` for it and every
attempt to ride a player is rejected on the server — silently, by return value, with no log line.
Bytecode-checked on both Forge and NeoForge across 1.21.2 → 26.2: **neither loader patches it out**,
so all three need the fix. 35 nodes (everything ≥1.21.2), since `2.0.0`.

The guard is skipped on the client, which is exactly what makes the symptom so strange. The server
broadcasts `MessageMosquitoMountPlayer` unconditionally, *after* its own `startRiding` has already
returned `false`; every client obeys the packet and draws the mosquito latched on, while the server
still believes it is flying. `EntityCrimsonMosquito#rideTick` — which is where the entire
blood-drinking, damage and dismount loop lives — never runs, so it never drinks and never lets go.
Killing it is the only exit, exactly as reported.

**It was never only the mosquito.** The same call fails for all seven mobs that ride a *player*:
the two latchers (crimson mosquito, enderiophage) and the five shoulder-riders (bald eagle, crow,
capuchin monkey, potoo, sugar glider). So on every node ≥1.21.2 no tamed bird or monkey has been
able to perch since `2.0.0` — which also takes the bald eagle's falconry loop with it a second time
(see #22, which was the Fabric-networking half of the same feature).

Fixed by the new `mixin/EntityMixin`, a `@Redirect` on the `canSerialize()` call inside
`startRiding`, waving past exactly the seven riders listed in `AMCompat#ridesUnsaveableVehicles`
and leaving every other entity subject to the vanilla check:

```java
@Redirect(method = "…startRiding(…)Z", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/EntityType;canSerialize()Z"))
private boolean alexsmobs_allowRidingUnsaveableVehicle(EntityType<?> vehicleType) {
    return vehicleType.canSerialize() || AMCompat.ridesUnsaveableVehicles((Entity) (Object) this);
}
```

Two gate details that are easy to get wrong:

- **The enclosing method moves at 1.21.9.** `startRiding(Entity, boolean)` was made `final` and its
  body moved into a three-argument `startRiding(Entity, boolean, boolean)` — the same boundary
  `AMCompat#startRiding` already splits at. The redirected *call* is unchanged across the move, but
  the `method =` descriptor is not, so the mixin has two arms.
- **Below 1.21.2 the guard does not exist**, and with `defaultRequire: 1` an injector that finds
  nothing aborts the launch. The `//?` arms therefore simply end, leaving `@Mixin(Entity.class)`
  with no injectors on the low nodes — which is valid — rather than trying to prune the class out
  of `alexsmobs.mixins.json`, which is one flat array shared by all 49.

⚠️ **`@Redirect`, not MixinExtras' `@ModifyExpressionValue`**: this repo puts MixinExtras on no
loader's compile classpath. The usual "redirects are exclusive" objection costs nothing for one
obscure call in one vanilla method.

### #82 — the Clinging effect does nothing — FIXED, 35 nodes, plus an upstream fault on all 49

> *"The effect clinging doesnt seem to work"*

Two independent faults, one of them upstream's.

**(a) The push, 35 nodes ≥1.21.2.** Clinging *is* a movement effect — everything it does is
`setDeltaMovement` inside `EffectClinging#applyEffectTick`. Through 1.21.1 that ran on **both**
sides, because `LivingEntity#tickEffects` called `MobEffectInstance#tick` on the client too, and a
client-side push is the only kind that survives for a player. **1.21.2 made effect ticking
server-only** (`applyEffectTick` gained its leading `ServerLevel`, and `MobEffectInstance#tick` only
calls it when the level is one). Player movement is client-authoritative, so from 1.21.2 the push
landed on the server's copy of the player and was overwritten by the very next position packet.
Mobs were never affected — they are server-authoritative, so the effect still moves them on every
version, which is why this reads as "the potion does nothing" rather than "the mod is broken".

Fixed with a new `ClientEvents#tickClinging()`, called from all three `clientTick` arms and gated
`>=1.21.2` — it mirrors `applyEffectTick`'s body for `Minecraft.getInstance().player` only.
⚠️ **It must not run below 1.21.2**, where the effect still ticks client-side by itself; the push
would be applied twice and the player would rocket.

**(b) The upside-down flip has never worked, on any version, including upstream.** Every caller
asked "is this entity hanging?" as `hasEffect(CLINGING) && getEyeHeight() < getBbHeight() * 0.45F`
— using a dropped eye height as a proxy. Nothing ever dropped it: the only writer was
`ServerEvents#onEntityResize`, whose guard is
`getActiveEffectsMap().containsKey(AMEffectRegistry.CLINGING)` — a **`Supplier`**, never a key of a
map of `MobEffect`s, so it is unconditionally `false`. That is upstream's own line, so the flip has
never fired in Alex's Mobs at all; the port then lost the event outright at 1.20.2 (eye height moved
into `EntityDimensions` and Forge deleted `EntityEvent.Size`).

Fixed by asking the real question instead: a new `EffectClinging#isFlippedUpsideDown(LivingEntity)`
(`hasEffect(CLINGING) && isUpsideDown(entity)`, the latter being the same ceiling test
`applyEffectTick` already uses), substituted at all three proxy sites in `ClientEvents` — the two
render `Pre`/`Post` flips and the `<1.21.2` first-person `flip.json` post-shader. No dimension
surgery and no mixin.

⚠️ **A proxy for a condition is worth re-deriving whenever the thing it proxies changes owner.**
The eye-height test was never *wrong*, it was reading a value nothing had written for years.

### #83 — seals never dig for treasure and never flee into the water — FIXED, all 49, deliberate divergence

> *"I noticed that seals don't swim in the water after the player feeds them 3 fish, they should
> reach the bottom and search for a treasure. They don't try to reach the water to escape an
> attacking player neither. These two bug were already in the original mod, is it possible to fix
> them in this new one?"*

Confirmed against the pristine baseline: **both are upstream's, on 1.20.1 Forge, unchanged by this
port.** The user asked for them to be fixed anyway, so this is the first place the port knowingly
behaves better than Alex's Mobs rather than identically. Every edit carries an `UPSTREAM FIX (#83)`
comment so a future baseline diff does not read it as porting damage.

Four causes, all in `EntitySeal` and `SealAIDiveForItems`:

1. **Nothing ever pushed a fed seal into the water.** `SealAIDiveForItems#genDigPos` can only pick a
   dig site it can *see* — ±7 blocks of water, or of the seal itself once submerged — and the only
   thing driving `ISemiAquatic#shouldEnterWater` was `swimTimer <= -1000`. A seal fed on a beach
   with `swimTimer` anywhere above that (i.e. for the ~80 s after it last hauled out, and *forever*
   if it had just been basking) simply sat there. `shouldEnterWater()` now short-circuits `true` on
   a new `isSeekingTreasure()`, and `AnimalAIFindWater` — priority 3, 32-block search — does the
   rest of the trip on its own.
2. **An attacked seal fled onto land.** `AnimalAIHerdPanic` paths to a `LandRandomPos`, away from
   the one place a seal is safe; worse, a seal that *had* been swimming (`swimTimer > 600`) and was
   hit at the water's edge answered `shouldLeaveWater() == true` and hauled out straight past its
   attacker. Both gates now short-circuit on a new `isFleeingToWater()`, and `canPanic()` is
   additionally gated on `!isInWaterOrBubble()` — a seal already in the water has escaped, and
   panicking is a land behaviour.
3. **`swimTimer` has no bounds.** It is not a "how long since I last swapped element" counter at
   all: it is the whole time spent on one side *minus* the other. A seal that basked for twenty
   minutes reaches about `-25000`, and then, once it finally gets in, needs that same twenty minutes
   back before `shouldLeaveWater()` can fire; the reverse strands one at sea. Now
   `Mth.clamp(swimTimer, -1200, 800)`, which preserves the intended ~80 s hysteresis and nothing
   else.
4. **`SealAIDiveForItems` declared no goal flags,** so it reserved nothing and `RandomSwimmingGoal`
   (priority 7) and `LookAtPlayerGoal` (8) ran alongside it and kept dragging the seal off the dig
   site — and `digTime` only advances while the seal is within 2 blocks of it, so the 100-tick dig
   could take minutes or never finish. Now `EnumSet.of(Flag.MOVE, Flag.LOOK)`.

⚠️ **Giving the dive goal `MOVE` forced a fifth change.** `SealAIBask` is priority **0** and holds
`Flag.MOVE`, so a basking seal outranks the dive goal outright; upstream got away with that only
because the dive goal reserved nothing and could clear the basking flag from its own `tick()`. Both
basking gates in `EntitySeal#tick` are now additionally suppressed by `isSeekingTreasure()`.

⚠️ `canUse()` also gained a **10-tick search throttle**. `genDigPos()` is a 15-attempt column scan
and the selector polls it every tick for as long as the seal owes a treasure — which, now that the
seal walks to the sea to look for one, is a great deal longer than it used to be.

`isSeekingTreasure()` deliberately requires the feeder to be **online**: `feederUUID` is persisted
and only cleared on delivery, so without that clause a seal whose feeder logged out would never
bask again.

### #84 — items put into a capsid just sit there — FIXED, 17 Fabric nodes

> *"I have a server with the latest version of the mod (2.0.13-fabric+26.2) and when i place an item
> like a disc or a raw cod inside a capsid it doesnt do anything and just stays still inside the
> capsid"*

**The capsid's recipe list has been empty on every Fabric node since Milestone 15.** Forge and
NeoForge register `CapsidRecipeManager` through `AddReloadListenerEvent`; on Fabric that event is
one of the `net.minecraftforge.**` stubs in `fabric/forge/**` and **nothing ever constructs or fires
it**, so `ServerEvents#onAddReloadListener` is dead code there, `apply()` was never called, and
`TileEntityCapsid#getRecipeFor` always returned `null`. No recipe matches, so the capsid never
starts a cycle and the item sits in it forever — exactly the report.

Fixed by registering the manager on Fabric's own seam, `ResourceManagerHelper`, in
`AlexsMobsFabric#onInitialize`. That interface requires an `IdentifiableResourceReloadListener`, so
`CapsidRecipeManager`'s class declaration grows two Fabric arms and a `getFabricId()`.

⚠️ **The registries-taking `registerReloadListener` overload only exists from MC 1.21**
(fabric-resource-loader 1.3.0) — but the manager only *needs* registries from 1.21.2 (where the
capsid codec started resolving item/tag ingredients against the provider; see the `>=1.21.2` arm in
`ServerEvents`). The split is written at **1.21.2**, so it can never land on a node where the
overload it wants is missing.

⚠️ **The generalisation, and it is worth a sweep:** *a Forge event that Fabric stubs out is
invisible to every check this repo has.* It compiles, the stub class exists, the `@SubscribeEvent`
method is well-formed, and no verifier looks at whether anything ever *posts* it. `ServerEvents`
now carries a `NOTE (#84)` on that arm saying so. Anything else registered only through an
`AddReloadListenerEvent`-shaped stub is in the same position.

### #85 — a wide set of mobs do not spawn in their biomes — FIXED, 17 Fabric nodes

> *"Minecraft: 1.21.11 Mod: 2.0.13 A wide set of mobs are not spawning in the biomes they can
> normally be found in. Hammerheads, Mimic Octopi, Mantis Shrimp (Non-Mangrove Variant), Orcas,
> Dusters, Anteaters, and Mungus to name a few that do not spawn when running ONLY Alex's Mobs and
> it's dependencies."*

**Every `forge:is_*` spawn default matched nothing on Fabric.** `DefaultBiomes` names its biome
tags as plain strings, and `SpawnBiomeData#conventionTag` — which rewrites `forge:x` to `c:x` —
was gated `(neoforge && >=1.20.5) || (forge && >=26)`. `c:` is and always has been *the* Fabric
convention namespace; nothing on that loader has ever defined a single `forge:` biome tag. So the
~30 mobs whose spawn entries are keyed on one never spawned at all, on all 17 Fabric nodes, since
Milestone 15 / `2.0.0`. The arm now takes `|| fabric`.

That alone is not enough, because **Fabric's convention tags come from an *optional* fabric-api
module whose contents vary by build**. The mod therefore ships the tags itself, on every Fabric
node, via `DataPackMigration.fabricConventionBackfill` — which grew from 7 entries to **15**, one
per distinct `forge:*` literal in `src/main/java`. Tag JSONs merge, so where the player's fabric-api
defines the tag too the union is simply the module's own set.

⚠️ **Neither the compiler nor `verify_convention_tags.py` can see any of this.** That script diffs
*data-pack references* against the pinned fabric-api jar; these tag names are Java string literals
compared against the tags a biome *carries*. That is precisely why they were missed when the first
seven backfill entries were added.

⚠️ **One entry is copied from Forge's definition, not fabric-api's.** `c:is_wasteland` is literally
`{"values": []}` in every fabric-api build (checked in the 1.20.1, 1.21.11 and 26.2 jars), while
Forge's `forge:is_wasteland` is `[minecraft:snowy_plains]`. It is referenced once — the moose's
first spawn pool, `is_overworld ∧ is_snowy ∧ is_wasteland` — so an empty tag silently costs the
moose its snowy-plains half; the second pool (`… ∧ #minecraft:is_taiga`) is why nobody reported
moose missing outright. Noted so it is not re-derived: **NeoForge ≥1.20.6 aliases `c:is_wasteland`
to an *optional* `#forge:is_wasteland` it no longer ships, and NeoForge 26.1 drops even that**, so
those nodes carry the same empty tag loader-side; this Fabric-only backfill does not reach them.

**The second half, all loaders where `conventionTag` is active (34 nodes).** Gson builds
`SpawnBiomeEntry` **reflectively** — it never calls the constructor the normalisation lives in — so
the rewrite only ever covered the shipped *defaults*, not what is read back out of
`config/alexsmobs/*.json`. On a fresh install that is invisible (the defaults are normalised, then
written to disk already normalised), but a player who generated their config on an older build has
`forge:` strings on disk that nothing would ever rewrite, and updating the mod would not fix their
spawns. `SpawnBiomeData`'s private constructor now re-normalises every deserialised entry.

### #86 — mimicream duping does not work — FIXED, 44 nodes, plus an upstream NPE on the other 5

> *"Mimicream duping just doesnt work"*

Eight mimicream around any damageable item is the whole duplication feature, and
`RecipeMimicreamRepair#assemble` **threw an NPE the instant the recipe matched** on all 44 nodes
≥1.20.5 — so it has been dead since `2.0.0`.

**1.20.5 moved both of the things this recipe edits off the item's NBT**: enchantments into
`DataComponents.ENCHANTMENTS`, and everything else into `DataComponents.CUSTOM_DATA` — which an
ordinary tool simply does not carry. `AMCompat.getTag(damageableStack)` therefore returned `null`
and the next line dereferenced it. Rewritten component-side for that era: `ItemStack#copy` carries
the whole component patch for free, so only the two deliberate *removals* have to be redone —
the ghostly pickaxe's stored `Items`, and Mending (which must not survive onto a copy that comes
out fully damaged, or it repairs itself back to new for nothing).

⚠️ **The Mending strip is matched on the registered id**, not on the `Enchantment` or its
`ResourceKey`, because the id is the one identity that spells the same across the 1.21
datapack-enchantment split. And ⚠️ **its lambda parameter is named `resourceKey` deliberately**:
1.21.11 renamed `ResourceKey#location()` to `identifier()`, and `stonecutter.gradle.kts` renames
that call **per-site** — `TagKey#location()` survives unchanged, so it cannot be blanket-renamed —
so `resourceKey.location()` is one of only five spellings the rules recognise. Writing `key` there
cost a compile failure on the three ≥1.21.11 nodes and is the single most likely way to break this
file again.

**The other 5 nodes had the same fault one era earlier**, unreported: below 1.20.5 the line was
`damageableStack.getTag().copy()`, and a damageable item only grows a tag once it is *damaged or
enchanted* — so on 1.20.1 and 1.20.4 the recipe NPE'd on a pristine tool and worked on a used one.
Now null-checked into a fresh `CompoundTag`. ⚠️ **Not `getOrCreateTag()`** — on those versions that
attaches the empty tag to the stack still sitting in the crafting *grid*.

### Verification of the seventeenth pass (2026-08-12 → 08-13)

- Every boundary read from bytecode, not from a neighbour: the `canSerialize` guard and the
  `startRiding` arity move (1.21.2 / 1.21.9), effect ticking going server-only (1.21.2), the
  component migration (1.20.5). Both loaders' jars checked for a patch-out of the riding guard —
  neither has one.
- Compile-green in **one invocation** on 7 era-spanning nodes: `1.20.1-forge`, `1.20.1-fabric`,
  `1.21.1-fabric`, `1.21.8-neoforge`, `1.21.11-fabric`, `26.2-fabric`, `26.2-neoforge` — covering
  both sides of 1.20.5, 1.21, 1.21.2, 1.21.9 and 26.
- Then `compileJava` + `processResources` for **all 49** nodes in one invocation.
- All 15 `c:` biome tags confirmed present in the generated resources of `1.20.1-fabric`,
  `1.21.11-fabric` and `26.2-fabric`, after clearing `versions/*/build/resources` (rule 9 —
  `DataPackMigration` changed).
- Full `:<node>:build` for **all 49** nodes in one invocation (2026-08-13), from a tree with the
  stale `2.0.13` jars moved out: `BUILD SUCCESSFUL in 3m 10s`, 49 main jars / 147 total, zero
  `-SNAPSHOT`-only strays and zero non-`2.0.14` jars. Then the three verifiers, each **on its
  predicted number**:
  - `verify_mixins.py` → `jars=49 problems=0` (unchanged — the new `EntityMixin` is declared in
    `alexsmobs.mixins.json` on every node and resolves on every node, including the 14 below
    1.21.2 where both of its arms are commented out and it legitimately carries no injectors).
  - `verify_mixin_targets.py` → `nodes=49 jars=49 selectors=1041 problems=0 skipped=0`. Predicted
    as `1006 + 35`: the `2.0.13` baseline plus **one** new `method =` selector on each of the 35
    nodes ≥1.21.2 (the script counts per selector, not per `@At`), which is also the independent
    confirmation that the two `startRiding` arms are gated to exactly the intended node set — a
    number above 1041 would have meant both arms live somewhere.
  - `verify_assets.py` → `literals=394 missing=0` (unchanged). The new
    `alexsmobs:capsid_recipes` reload-listener id is **not** counted, as expected: it is an
    `IdentifiableResourceReloadListener` id, not an asset path.
- **Declaration-hole sweep** (the generalisation from #79): every `replace(".old()", ".new()")` rule
  in `stonecutter.gradle.kts` was cross-checked against declarations of the old name in
  `src/main/java`. The straddleboard's `isControlledByLocalInstance`, already fixed in the sixteenth
  pass, is the only real hole; `AMRenderTypes#entityGlintDirect` is a same-named static delegate
  whose own call sites are renamed, so it is dead above 1.21.2 but harmless, and the two Fabric
  event-stub hits (`LivingChangeTargetEvent`) sit under a NeoForge-only rule.
- ⚠️ **No client session for any of the six.** `2.0.12` and `2.0.13` also shipped without one, so
  this would be the third in a row. What is owed, cheapest first: on any node ≥1.21.2 — perch a
  tamed crow (#81) and drink a Clinging potion under an overhang (#82); on `26.2-fabric` — put a
  raw cod in a capsid (#84) and craft 8 mimicream around a damaged pickaxe (#86); on any Fabric
  node — `/execute` a biome check that hammerheads and orcas now spawn in warm/frozen oceans (#85);
  and feed a seal three fish on a beach (#83), which is the one that needs watching rather than
  looking at.

## The eighteenth bug-report pass (2026-08-14) — two reports, against published `2.0.14`

Two reports from one player, and the second of them turned out to be **three** faults sharing one
root: the `forge:` → `c:` convention-tag move. One of the three was already fixed in `2.0.14`
(#85); the other two had never been reported and are repaired here, along with a systemic sweep
that found six more mobs losing biomes to the same cause.

Neither report stated a mod version or a loader; the second states **Fabric 26.2**.

### #87 — nothing this mod adds can be given Unbreaking or Mending — FIXED, 44 nodes

> *"In the original mod it was able to be combined with unbreaking and mending in an anvil and it
> isnt possible here."*

No item was named, and it did not need to be: on every node **≥1.20.5** the answer was *all of
them*. **1.20.5 deleted the `EnchantmentCategory` enum** and made an enchantment declare what it
goes on as an **item tag** (`supported_items` → `#minecraft:enchantable/*`). Upstream 1.20.1 needed
no data at all — `EnchantmentCategory.BREAKABLE#canEnchant` is `stack.isDamageableItem()`, so every
damageable item in the game was automatically eligible for Unbreaking, Mending and Vanishing. The
tag that replaced it, `#minecraft:enchantable/durability`, is an **explicit list**, and this port
never joined it.

So from `2.0.0`, on 44 of 49 nodes, none of the following could take a single durability
enchantment — in an anvil, at a table, or from `/enchant`:

- the **14 armour pieces** (moose headgear, frontier cap, sombrero, spiked turtle shell, fedora,
  froststalker helmet, novelty hat, crocodile chestplate, rocky chestplate, unsettling kimono,
  centipede leggings, emu leggings, roadrunner boots, flying fish boots) — which also lost
  Protection, Respiration, Thorns, Feather Falling, Depth Strider and Swift Sneak, because the
  per-slot `#minecraft:enchantable/{head,chest,leg,foot}_armor` tags are the same mechanism;
- the **tarantula hawk elytra** (an `ArmorItem`/CHESTPLATE upstream, so Protection is parity) and
  the **shield of the deep**;
- the **eleven damageable gadgets** — blood sprayer, pocket sand, hemolymph blaster, straddleboard,
  echolocator, endolocator, pupfish locator, dimensional carver, shattered dimensional carver,
  squid grapple, stink ray.

The three weapons/tools were fine by accident: `#minecraft:enchantable/durability` includes
`#minecraft:swords` and `#minecraft:pickaxes`, and this mod already puts the skelewag sword in
`#minecraft:swords` and the ghostly pickaxe in `#minecraft:pickaxes`. The **tendon whip** was not —
it `extends SwordItem` below 1.21.5, so upstream gave it the whole WEAPON category, and it is now
the second entry in the mod's `swords.json`.

Fixed by authoring six tag files under `data/minecraft/tags/items/enchantable/` — `durability`
(27 entries), `head_armor`, `chest_armor`, `leg_armor`, `foot_armor`, `equippable`. The folder is
renamed to `tags/item/` at 1.21 by `DataPackMigration.migrateTo121`, which walks the tree, so the
`enchantable/` subfolder comes with it (verified in the projections on both sides of the boundary).

⚠️ **They are deliberately NOT added to the base `#minecraft:{head,chest,leg,foot}_armor` tags.**
Those feed `minecraft:trimmable_armor` as well (read out of the 1.21.8 jar), and a sombrero in a
smithing table is not parity — it is a new feature with no models behind it.

**A second, older fault fell out of this.** `data/minecraft/tags/items/foot_armor_enchantable.json`
existed and had never worked: it is the **Java field name** (`ItemTags.FOOT_ARMOR_ENCHANTABLE`),
not the tag id, which has been `minecraft:enchantable/foot_armor` since 1.20.5 — read out of the
1.20.6, 1.21, 1.21.4 and 26.2 jars. So the **ancient hogshoes**, the one item this port had
actually tried to keep enchantable, were in no tag at all on all 44 nodes. The misnamed file is
retired and the shoes are in the real `foot_armor` tag; `ItemPigshoes`'s comment, which asserted
the wrong id, is corrected.

⚠️ **Generalisation — the fifth of this shape (#30/#66/#71/#74/#87): when a version replaces a
*code* mechanism with a *data* one, the code still compiles and every verifier stays green.** There
is no override to lose and no signature to diff; the mod simply stops appearing in a list it never
knew it was on. `verify_assets.py` cannot see it either — it checks that named assets exist, not
that unnamed ones should.

**Verified headlessly** on `26.2-fabric` over RCON (`/enchant` uses `Enchantment#canEnchant`, the
same predicate `AnvilMenu` uses), with a `NoAI` zombie holding each item in its main hand:

- all 30 items accept `unbreaking` **and** `mending`;
- the hogshoes accept `feather_falling` and `depth_strider` and **refuse** `unbreaking`, `mending`,
  `binding_curse` and `vanishing_curse` — which is upstream's own rule, preserved;
- armour takes `protection`/`respiration`/`thorns`/`swift_sneak`, the sword takes
  `sharpness`/`looting`/`fire_aspect`, the whip takes `sharpness`/`looting`, the pickaxe takes
  `efficiency`/`fortune`, and the shield and carver take `vanishing_curse`.

### #85 (re-reported) — flying fish everywhere, no orcas or cachalots — ALREADY FIXED in `2.0.14`

> *"Since I've been testing this mod in Fabric version 26.2, flying fish are almost the only ones
> that appear in the ocean (including in cold oceans where I don't think they're supposed to appear
> normally), and sperm whales or orcas never spawn."*

This is #85 seen from the other end, and the reporter's three symptoms are one predicate:

| pool | entry |
|---|---|
| `ORCA` 0 | `minecraft:is_ocean` AND `forge:is_cold/overworld` |
| `CACHALOT_WHALE` 0 | `is_overworld` AND `is_ocean` AND `forge:is_cold/overworld` |
| `FLYING_FISH` 0 | `is_overworld` AND `is_ocean` AND **NOT** `forge:is_cold/overworld` AND **NOT** `forge:is_hot/overworld` AND NOT deep ocean |

On Fabric `forge:is_cold/overworld` matched nothing, so the orca's only pool never matched (never
spawns), the cachalot lost its main pool and kept only three registry-name ones, and the flying
fish's two **negated** entries failed **open** — a negated entry on an undefined tag matches
*everything*. Hence "flying fish are almost the only ones, including in cold oceans". The report is
against a version older than `2.0.14`, or a world whose `config/alexsmobs/*.json` predates it.

**Now verified headlessly**, which #85 was still owed (the whole check needs no GPU):

- a fresh `26.2-fabric` install writes **90** spawn configs with **zero** `forge:` values in any of
  them; `orca_spawns.json` and `flying_fish_spawns.json` both carry `c:is_cold/overworld`;
- `execute if biome` at located coordinates resolves `#c:is_cold/overworld` at `frozen_ocean`,
  `ocean` and `cold_ocean` and **not** at `warm_ocean`, and `#c:is_hot/overworld` at none of them —
  so the orca and cachalot pools match again and the flying fish is excluded from cold water.

⚠️ The **on-disk** repair for an existing config (the second normalisation pass, in the private
Gson constructor) is **not** observable from the file: the values are rewritten in memory on load
and the file is never written back. A world upgraded to `2.0.14` keeps `forge:` strings on disk and
still behaves correctly. Don't read the config file to decide whether the fix is present.

### #85 (re-reported again) — no desert mobs either — ALREADY FIXED in `2.0.14`

> *"I forgot to mention that desert mobs, like the hawk tarantula, rattlesnakes, and roadrunners,
> don't spawn either. I haven't checked whether gusters, jerboas, and rain frogs spawn or not."*

Same reporter, same fault, the dry end of it — and the answer to the two they had not checked is
**yes, those were broken too**. Four mobs share one definition, `DefaultBiomes.DESERT`
(`config/DefaultBiomes.java:590`): the **tarantula hawk**, the **jerboa**, the **rain frog** and
the **triops**. Its only vanilla pool is `forge:is_dry/overworld` AND `forge:is_hot/overworld` AND
`forge:is_sandy` AND NOT `minecraft:is_badlands` — on Fabric before `2.0.14` all three positives
matched nothing, so the pool never matched and those four never spawned anywhere but a Terralith
biome. The **roadrunner**, **rattlesnake** and **guster** each carry the same three tags in a later
pool, but keep a `minecraft:is_badlands` pool of their own — which is why they were seen in a mesa
and never in a desert.

The `2.0.14` fix covers it on both halves: the `forge:` → `c:` normalisation, and the mod shipping
its own `c:` definitions (fabric-api is not obliged to define them). Confirmed in the built
`2.0.15` `26.2-fabric` jar — it carries `data/c/tags/worldgen/biome/{is_sandy,is_dry/overworld,
is_hot/overworld}.json`, and `is_sandy` is `["minecraft:desert", "minecraft:badlands",
"minecraft:wooded_badlands", "minecraft:eroded_badlands", "minecraft:beach"]`, so `minecraft:desert`
is the one biome satisfying all four conditions. Nothing to change.

⚠️ Same caveat as above: the reporter's on-disk config still says `forge:`, and that is not
evidence either way.

### #88 — a seal in a frozen ocean wears the brown skin — FIXED, 34 nodes

> *"seals very rarely spawn in frozen oceans, and their original skin blends in with the cold
> biome's skin, even though they're very common in that cold biome in the original mod."*

Two halves. The rarity is #85 again (`SEAL` pool 1 is `is_overworld` AND `is_ocean` AND
`forge:is_cold/overworld`, so on Fabric only the beach pool ever matched). The **skin** is its own
fault, on every `c:` node — all 17 Fabric, NeoForge ≥1.20.5, Forge ≥26 — i.e. **34 of 49**, since
`2.0.0`.

`EntitySeal#isBiomeArctic` tests `#alexsmobs:spawns_white_seals`, which shipped as exactly
`["#forge:is_snowy"]`. **`forge:is_snowy` contains `frozen_ocean` and `frozen_river`; neither `c:`
definition does** — not NeoForge's, not Forge 26's, not fabric-api's (all three read out of the
jars). So the tag the port migrates to is *narrower than the one it came from*, and a seal that
spawns on frozen-ocean ice comes out brown.

### #89 — eight spawn pools lost their vanilla biomes to the same narrowing (found here) — FIXED, 34 nodes

#88 is not a special case, so the whole set was swept: every biome tag named by `DefaultBiomes` was
resolved in three worlds — Forge 1.20.1 (vanilla + forge tags), 26.2 + NeoForge `c:`, and 26.2 +
fabric-api `c:` + this mod's own backfill — and every pool diffed. **Three tags are a narrowing,
not a rename:**

| tag | dropped on the way to `c:` |
|---|---|
| `is_snowy` | `frozen_ocean`, `frozen_river` |
| `is_plains` | `snowy_plains`, `meadow` |
| `is_wasteland` | `snowy_plains` (`c:is_wasteland` is empty on NeoForge/Forge 26; fabric-api ships it empty too, and the mod's own backfill already restored Forge's definition there) |

Between them that is eight pools across eight mobs, and **three of them had no vanilla biome left
at all**:

- **moose** — pool 0 is `is_overworld` AND `is_snowy` AND `is_wasteland`, i.e. snowy plains. Empty.
- **tusklin** — its snowy-plains pool empties, leaving only `ice_spikes`.
- **gelada monkey** (`MEADOWS`) — `is_plains` AND `is_plateau` = `meadow`. Empty. The mob had **no**
  vanilla biome on any `c:` node.
- **raccoon**, **crow**, **bison** — lose meadow and snowy plains.
- **snow leopard** — loses frozen ocean and frozen river.
- **komodo dragon** — the opposite shape, and the one that cannot be fixed by widening a tag:
  `c:is_dense_vegetation/overworld` **gained** `bamboo_jungle` (and mangrove swamp) over
  `forge:is_dense/overworld`, and that tag is **negated** in the komodo's only pool — so a *wider*
  tag *subtracts* biomes, and the komodo lost half its vanilla range.

**Fix.** Three mod-owned alias tags, `data/alexsmobs/tags/worldgen/biome/is_{snowy,plains,wasteland}.json`,
each being the loader's own tag plus the members the move dropped, and `SpawnBiomeData.conventionTag`
routes those three paths to `alexsmobs:` instead of `c:`. `spawns_white_seals` points at the alias
too, which is #88's fix. The komodo instead gets `minecraft:bamboo_jungle` as its own extra pool —
on the nodes still reading `forge:` that biome already matched pool 0, so it is a no-op there
rather than a divergence.

⚠️ The reference to the loader tag inside each alias is written
`{"id": "#forge:is_wasteland", "required": false}` — **optional on purpose**. `DataPackMigration`'s
textual sweep rewrites the `forge:` inside the object exactly as it would a bare string, and
NeoForge ≥26.1 ships **no** `c:is_wasteland` at all, so a required reference would fail the tag
load on those nodes.

⚠️ **Pool indices must be added in non-decreasing order.** `addBiomeEntry` appends *one* list when
`biomes.size() < pool + 1`, so writing the komodo's new pool 5 before pools 1–4 exist throws
`IndexOutOfBoundsException` at class-init. It has to be the last call in the chain.

Two judgement calls worth stating rather than hiding:

- **`deep_frozen_ocean` was never in `forge:is_snowy` either**, so seals there stay brown. That is
  upstream's behaviour, not a remaining bug.
- Restoring `is_snowy` also puts **snow leopards** back on frozen-ocean and frozen-river ice. That
  is upstream's behaviour too, and it is what the tag meant, but it is a visible change.

### Verification for the eighteenth pass

- `processResources` + `compileJava` on six era-spanning nodes in one invocation — `1.20.1-forge`,
  `1.20.6-neoforge`, `1.21.1-fabric`, `1.21.11-fabric`, `26.2-fabric`, `26.2-forge` — green.
- Projections checked on all six: the enchantable tags land under `tags/items/` on 1.20.1/1.20.6
  and `tags/item/` from 1.21 up; the three aliases carry `#forge:` on `1.20.1-forge` and `#c:` on
  every other node, object form and `"required": false` intact.
- Every `alexsmobs:` item id in the new tag files cross-checked against `AMItemRegistry` — none
  unknown.
- `verify_assets.py` → `literals=394 missing=0`. `verify_convention_tags.py` → `nodes=17
  problems=9`, unchanged and all nine the documented `#c:tools/spear(s)` optionals — the new
  `#c:is_snowy`/`is_plains`/`is_wasteland` references are **not** among them, which is the proof
  that the mod's own Fabric backfill covers them.
- **Headless `26.2-fabric` server session over RCON** (no GPU): the `/enchant` matrix above; the
  biome-tag matrix at six located coordinates; and a seal summoned **without NBT** (a `/summon`
  that carries NBT skips `finalizeSpawn`, which is where `setArctic` runs — an easy false negative)
  comes out `Arctic: 1b` in `frozen_ocean` and `0b` in `cold_ocean` and `ocean`.
- `compileJava` + `processResources` for **all 49** nodes in one invocation — green.
- **All 49 projections** checked programmatically: `tags/items/enchantable/` on the **8** nodes
  below 1.21 and `tags/item/enchantable/` on the other **41**, six files each; the alias reference
  optional-object-form on all 49, `forge:` on **15** nodes × 3 and `c:` on **34** × 3 (which is the
  independent confirmation that the `c:` node set is exactly Fabric + NeoForge ≥1.20.5 + Forge ≥26);
  the tendon whip in every `swords.json`; no stale `foot_armor_enchantable.json` anywhere; and
  `spawns_white_seals` = `["#alexsmobs:is_snowy"]` everywhere.
- Full release `:<node>:build` for **all 49** in one invocation (2026-08-14), stale `2.0.14` jars
  moved out first: 147 jars (49 × main+sources+javadoc), **zero `-SNAPSHOT`**, zero non-`2.0.15`.
  Then the three verifiers, each on its predicted number and **all unchanged from `2.0.14`**, as
  expected for a pass that added no mixins and no code-named assets:
  `verify_mixins.py` → `jars=49 problems=0`; `verify_mixin_targets.py` → `nodes=49 jars=49
  selectors=1041 problems=0 skipped=0`; `verify_assets.py` → `literals=394 missing=0`.
- ⚠️ **The 49-node release build failed the first time with twelve `kspKotlin FAILED` tasks and no
  compiler error** — pure contention, not a code fault: the same node built clean on its own, and
  the identical batch succeeded on a plain retry with **`--max-workers=4`** (`BUILD SUCCESSFUL in
  3m 8s`). Rule 1 still holds — one invocation — but cap the workers on the full matrix.
- ⚠️ **No client session.** Nothing in this pass needs one — both fixes are data, and both were
  verified against a running server — but the four items owed from the seventeenth pass still are.

## The nineteenth bug-report pass (2026-08-14 → 08-15) — three items, against published `2.0.14`

A third report from the same player as the eighteenth pass (**Fabric, MC 26.2**, on `2.0.13` or
older), three items in one message:

> *"I forgot to mention that in the cold biomes, only moose and snow leopards appear; I haven't been
> able to spot any tusklins or bison.*
>
> *I could be the only one having this issue, but the Farseer isn't spawning for me in the overworld,
> it is in the End though. Also, the portal created by the shattered dimensional carver looks like a
> black circle instead of the portal texture it's supposed to have. It could be my game that's
> messing up though."*

One is already fixed in the unreleased `2.0.15` — and is the first **independent player
confirmation** of #89, which nobody had reported. One is real, new, and turned out to cover three
render sites rather than the one reported. One is upstream behaviour.

### #89 (re-reported) — only moose and snow leopards in cold biomes — ALREADY FIXED in `2.0.15`

> ⚠️ **Read [#100](#100--the-c--alexsmobs-alias-routing-never-fires-on-an-existing-config--fixed-34-nodes)
> before believing this line.** "Already fixed in `2.0.15`" was true of the *code* and false of every
> instance that already had a `config/alexsmobs/` directory — which is all of NeoForge and Forge 26,
> and Fabric from `2.0.14` on. This same report came back a third time in the twenty-seventh pass,
> and that is why.

Not a new fault: it is the exact symptom #89 predicts, reported by a player who had no idea the
eighteenth pass existed. `c:is_snowy` is a **narrowing** of `forge:is_snowy` and `c:is_plains` of
`forge:is_plains`, and on the 34 `c:` nodes that empties two of the four cold-biome pools:

| Mob | Pools | On a `c:` node before `2.0.15` |
|---|---|---|
| **Tusklin** | `ice_spikes` · `is_snowy ∧ is_plains` | pool 1 is **empty** (`c:is_plains` = plains + sunflower_plains, neither snowy) — only ice spikes left |
| **Bison** | `is_plains ∧ ¬is_savanna ∧ ¬is_hot` · `meadow` | loses **snowy_plains** |
| **Moose** | `is_snowy ∧ is_wasteland` · `is_snowy ∧ is_taiga` | pool 0 empty (`c:is_wasteland` is empty), **survives via taiga** |
| **Snow leopard** | `is_snowy` · `snowy_slopes` · `frozen_peaks` · `jagged_peaks` | **survives via the three peak biomes** |

So in a snowy-plains/ice-spikes region the two that still had a pool are precisely moose and snow
leopards, which is what the player saw. The fix is the three `alexsmobs:is_{snowy,plains,wasteland}`
alias tags already in the tree — see the eighteenth pass. **No further change.**

### #90 — the shattered dimensional carver's portal is a black circle — FIXED, 27 nodes

> *"the portal created by the shattered dimensional carver looks like a black circle instead of the
> portal texture it's supposed to have."*

Real, and the same family as **#53** (the farseer's "square summoning particle"): a
`RenderType` that 1.21.5 deleted, met with a fallback that is not equivalent. Broken on every node
**≥1.21.5** — 27 of 49, all three loaders — since `2.0.0`.

Upstream draws every "TV static" effect as **two passes over the same geometry**: a shaped pass that
writes depth, then `static.png` re-drawn at **EQUAL** depth with glint-scroll UVs
(`AMRenderTypes.STATIC_PORTAL` / `STATIC_PARTICLE` / `STATIC_ENTITY`). 1.21.5 removed the custom
composite render types those three are built from. #53 already split the call sites into two
helpers:

- `renderStaticMasked` — bare quad, the *shape lives in the mask texture's alpha*. Fixed by baking
  the noise into the mask (`scripts/bake_static_textures.py`) and drawing one pass.
- `renderStaticOverlay` — geometry that is *already the right shape*, static layered on top. The
  static pass is simply dropped: a clean degrade.

The shattered void portal was filed under the second, and it is a **mask** site. Its thirteen
textures (3 idle frames + 10 growth frames) are **pure black with an alpha cut-out** — `maxRGB = 0`,
confirmed with PIL on every one of them. They have no content of their own; everything visible about
that portal *was* the static pass. Dropping it leaves exactly what the player described: a solid
black disc.

⚠️ **The distinguishing test is the mask's RGB, not what the geometry looks like.** "Model-shaped
geometry" and "shape lives in the texture" are not the two categories that matter — *"does the
shaped pass draw anything on its own"* is. Checking that across all four `renderStaticOverlay` sites
found **two more** mis-filed the same way, neither ever reported:

- **the farseer's eye** (`farseer_eye.png`, drawn whenever it is firing its beam) and
- **its scars** (`farseer_scars.png`, drawn while it is hurt) —

both `maxRGB = 0`, both drawing as flat black on the mob on the same 27 nodes. The transmutation
table's overlay (`maxRGB = 255`) is genuinely textured and stays an overlay site; it is now the only
one.

Fixed by:

1. **60 new baked textures** — 52 for the shattered portal (13 frames × 4 noise variants, 64×64) and
   8 for the farseer's eye and scars (128×128). `bake_static_textures.py` grew a `shear` argument for
   the 128px pair, because `static.png` is only 64 wide and a straight wrap would tile one visible
   column band; it defaults to 0 so every previously-generated file re-bakes **byte-identically**
   (confirmed — `git status` shows the 16 farseer portal bakes unmodified after a full re-run).
2. **A second `renderStaticMasked` overload** taking the shaped `RenderType` rather than a mask
   `ResourceLocation`, because this site's `<1.21.5` arm must keep upstream's exact
   `entityCutoutNoCull` spelling (the existing overload hardcodes `entityTranslucent`).
3. `RenderVoidPortal` and `RenderFarseer` now cycle the baked variant every 2 ticks for the flicker
   the scroll shard used to provide, and `RenderVoidPortal` grew a private `idleIndex(age)` so the
   frame chosen for the mask and for the baked texture cannot drift apart.

**A companion fault in the same call chain**, found while reading it and fixed here: upstream's
`full_bright` render type is *translucent + emissive*, and the port's `AMRenderTypes.getFullBright`
fell back to **`RenderType.eyes(...)`** on ≥1.21.5 — which is **additive** (`COLOR_WRITE`, no depth
write), so dark pixels of a texture disappear entirely instead of drawing dark. It now falls back to
`entityTranslucentEmissive`, which is what upstream's composite actually is. Three call sites
benefit: the **un**-shattered void portal (mean RGB 19 on its idle frames — most of it was being
eaten), the void worm shot (mean 20) and the pollen ball.

### #91 — the farseer does not spawn in the overworld — NOT A BUG

> *"the Farseer isn't spawning for me in the overworld, it is in the End though."*

**This port is byte-identical to upstream here** and the behaviour is upstream's design:

```java
private static boolean isFarseerArea(ServerLevelAccessor iServerWorld, BlockPos pos) {
    return !AMConfig.restrictFarseerSpawns
        || iServerWorld.getWorldBorder().getDistanceToBorder(pos.getX(), pos.getZ()) < AMConfig.farseerBorderSpawnDistance;
}
```

`restrictFarseerSpawns` defaults to **`true`** and `farseerBorderSpawnDistance` to **100**, so a
farseer can only spawn **within 100 blocks of the world border** — in *any* dimension. The mod's own
animal dictionary says so in as many words ("they can only cross at the very borders of the world,
which makes encounters extremely rare") and points at the **dimensional carver** as the intended way
to reach one. `EntityFarseer.checkFarseerSpawnRules`, `isFarseerArea`, the placement registration and
`DefaultBiomes.FARSEER` were all diffed against the pristine baseline `151e36c`: unchanged.

The asymmetry the reporter noticed is a **spawn-weight** effect, not a dimension gate.
`DefaultBiomes.FARSEER` excludes only `#forge:no_default_monsters` and `mushroom_fields`, so the
entry is added to End biomes as well — where the only competing `MONSTER` entry is the enderman
(weight 10) against the farseer's 30. Near a border in the End the farseer wins ~3 attempts in 4;
near a border in the overworld it competes with zombies, skeletons, creepers, spiders and the rest
for well under a tenth. On a multiplayer server with a small border that is the difference between
"constantly" and "never".

Advice for the reporter rather than a code change: set `restrictFarseerSpawns = false` in
`config/alexsmobs/alexsmobs.toml` to have them spawn anywhere, or use a dimensional carver.

### Verification for the nineteenth pass

- `compileJava` on six era-spanning nodes in one invocation, straddling the 1.21.5 boundary in both
  directions — `1.20.1-forge`, `1.21.4-neoforge`, `1.21.5-neoforge`, `1.21.8-forge`,
  `1.21.11-fabric`, `26.2-fabric` — `BUILD SUCCESSFUL`, `GRADLE_EXIT=0`.
- Projections checked on `1.21.4-neoforge` (below), `1.21.5-neoforge` (above) and `26.2-fabric`
  (above, plus the `RenderType`→`RenderTypes` and `ResourceLocation`→`Identifier` renames): the new
  overload's two arms and `getFullBright`'s two arms resolve to the intended side on each, and the
  26.x projection correctly rewrites both `entityTranslucent` and `entityTranslucentEmissive` to
  `net.minecraft.client.renderer.rendertype.RenderTypes`.
- `processResources` on one node either side: **65** files under
  `entity/void_worm/portal/shattered/` (13 masks + 52 bakes) and **24** `*static*` under
  `entity/farseer/` (16 portal + 8 eye/scars) land in `build/resources`.
- `verify_assets.py` → `literals=394 missing=0`, **unchanged** — correct, and worth stating why:
  every baked texture id is built by string concatenation, so the verifier cannot see any of them,
  exactly as it could not see #53's. The names were cross-checked by hand against the files instead.
- ⚠️ **No client session.** #90 is a purely visual fix and wants one on any ≥1.21.5 node: carve with
  a **shattered** dimensional carver and look at the portal; then a farseer's eye (while it fires)
  and scars (while it is hurt) should be static, not black.

## Twentieth pass (2026-08-15)

Three reports from one player, and the first two are **one fault**.

### #92 — the shattered dimensional carver looks identical to the plain one and lost its 3D animation — FIXED, 30 nodes

> *"I also noticed the texture for the shattered dimensional carver is the same as the dimensional
> carver, is that intended?"*
>
> *"I forgot to mention that the two dimensional pickaxes seem to be identical, and I also think,
> maybe I remember correctly, but in the original mod, the two dimensional pickaxes used to have 3D
> animations or something like that."*

Both sentences describe the same item and the same cause. **There is no
`shattered_dimensional_carver.png` anywhere in this mod, upstream or here** — the item's authored
model is `"parent": "builtin/entity"` with nothing but a `particle` sprite (which points at
`item/dimensional_carver`, the *plain* carver — that is upstream's own choice and only feeds break
particles). Everything a player sees is drawn live by `AMItemstackRenderer#renderByItem`: eleven
`dimensional_carver_shard_*` item models, each nudged every frame by a sine/cosine of
`tick + partialTick` so the shards drift apart and rotate around each other. That drift **is** the
"3D animation", and it is the only thing distinguishing this item from the intact carver.

1.21.4 deleted the ISTER (see #21/#23/#75), and this port's replacement pass had rebuilt the carver
as a **`minecraft:composite` of those same eleven shard models** — the right eleven models, stacked
at **zero offset**. Stacked with no drift they reassemble into the intact carver silhouette and
nothing moves: exactly *"the same texture as the dimensional carver"* and *"used to have 3D
animations"*. Broken on the **30 nodes ≥1.21.4**, all three loaders, since `2.0.0`.

The fix is to stop substituting and let the live renderer draw it: `shattered_dimensional_carver`
is now the sixth member of `LIVE_ICON_ITEMS` in `DataPackMigration.kt`, so it gets a
`minecraft:special` definition of type `alexsmobs:icon` routing back into `renderByItem`, and
`COMPOSITE_SUBSTITUTES` — whose only entry this was — is deleted along with its branch.

⚠️ **The membership test for `LIVE_ICON_ITEMS` was mis-stated and is now corrected in the KDoc.**
The carver had been kept out on the grounds that in-hand items "branch on the display context and
would be flattened". It does read the context — but only for a left-hand-only translate/scale/
rotate and a `GROUND ? packedLight : 240` — and the real context still flows through on 24 of the
30 nodes: only the **26.x `submit` arm** of `AMIconSpecialRenderer` lacks an `ItemDisplayContext`
and hardcodes `GUI`. So the test is not "does it read the context" but **"does the context read
pick a different *model*"** (disqualifying) versus a cosmetic nudge (fine).

The residual cost on the six 26.x nodes — the skipped left-hand nudge and the fullbright — was
written up here as "strictly better than a frozen composite" and accepted. It is **no longer
paid at all**: the twenty-first pass's **#96** found that the context had not been deleted, only
moved one frame up into `ItemStackRenderState`, and `mixin/client/ItemStackRenderStateMixin` now
lends it, so all 30 nodes get the real display context. ⚠️ Worth noting as a habit: a documented
"acceptable degrade" is a standing invitation to look one frame further up.

✅ **Client-confirmed on `26.2-fabric` (2026-08-17)**, in the same session and the same screenshot
as **#96** — the shattered carver in the main hand with its eleven shards visibly drifting apart,
against the plain carver in the off hand as the control.

### #55 (re-reported) — the Mexican cockroach's hat sits high while dancing — NO DIVERGENCE, again

> *"I noticed that the Mexican cockroach's hat sits a bit high; in the official mod, the cockroaches'
> hats stay in the normal position when they dance, but as far as I recall, in this continued
> version of Alex's Mobs, the Mexican cockroach's hat sits a bit high while dancing."*

Same item as #55 above, and the second sweep found the same thing the first did. Every file in the
path was diffed against the pristine baseline (`151e36c`):

| File | Divergence from upstream |
|---|---|
| `ModelSombrero` | **byte-identical** |
| `ModelCockroach` | three import lines only |
| `EntityCockroach` (`danceProgress` field, tick, reset) | none |
| `RenderCockroach` | none |
| `citadel/…/AdvancedModelBox#translateAndRotate` | none (self-contained: `rotationPoint/16`, ZYX, then scale) |
| `LayerCockroachMaracas` | only `AMCompat.rl` / `AMRenderCompat.renderItemInHand` / `renderToBuffer` |

The hat's dance movement is **upstream's own authored code** in `LayerCockroachMaracas` —
`danceProgress * 0.045F` on Y, `* -0.09F` on Z and a `60F * danceProgress * 0.2F` X-rotation — so
the reporter's premise (that the official mod holds the hat still while dancing) does not match the
official mod's source. Vanilla's side is unchanged too: `HumanoidModel.createMesh`'s head pose is
`PartPose.offset(0, 0 + yOffset, 0)` on 26.2, javap-verified, identical to 1.20.1.

Two ways forward, both needing the user: a **screenshot at a named MC version** (if the hat sits
higher here than upstream at the *same* frame, that is a new fault and this table says where it
cannot be), or a **deliberate divergence** — pin the hat still through the dance by dropping the
three compensation terms, which would be a change away from upstream on all 49 nodes.

### #55 (third report, 2026-08-16) — CLOSED as a deliberate divergence

> *"the hat is clearly too high"* — with a screenshot: MC **26.2**, a Mexican cockroach mid-dance,
> the sombrero floating a long way above its head. The "Challenge Complete! Mariachi!" toast dates
> the frame to the moment the maracas were handed over.

The screenshot the two earlier sweeps asked for. It does not overturn the table above — the port is
still faithful — so this is the second of the two ways forward, taken on the user's word.

**Why upstream's own numbers float the hat.** Working in model pixels with feet at `h = 0`
(`h = 24 − y_model`; the renderer's `scale(-1,-1,1)` + `translate(0,-1.501,0)` make +y_model down,
and `RenderCockroach` scales the lot by `0.85`):

| | at rest | dancing (`danceProgress = 5`) |
|---|---|---|
| head origin | h 1.7 | h 20.4 |
| head cube top | h 2.7 | h 22.6 |
| brim bottom (upstream) | h 12.1 | **h 32.2** |

`translateToHand(4, …)` walks `root → abdomen → neck → head`, so every offset after it is applied
**in the head's frame** — and the dance rotates that frame by `-70°` about X
(`progressRotationPrev(abdomen, dp, rad(-70), 0, 0, 5F)`). That is what defeats upstream's
arithmetic:

- the fixed `translate(0F, -0.4F, -0.01F)` means "6.4 px up" only while the roach is flat; through
  the `-70°` it resolves to 2.3 px up and 6.0 px **back**;
- the compensation term `translate(0F, dp * 0.045F, dp * -0.09F)` is written to *settle* the hat
  (+y is down) as the roach stands up — but through the same rotation its Z half dominates and the
  pair comes out as 5.5 px **up** and 5.8 px forward. It raises the hat instead of lowering it.

Net: a **9.6 px (≈0.5 block after the 0.85 render scale) gap** between the head top and the brim, on
a roach whose whole standing silhouette is ~1.2 blocks. The `+60°` X-rotation is fine on its own —
against the abdomen's `-70°` it nets `-10°`, which is the small jaunty tilt that was intended.

**The fix** keeps every one of upstream's constructs and only moves the rotation **before** the
translates, so both offsets are applied in a frame that is within `2° × danceProgress` of upright and
therefore mean what they say. The two translates then collapse into one re-solved line:

```java
matrixStackIn.mulPose(Axis.XP.rotationDegrees(60F * entitylivingbaseIn.danceProgress * 0.2F));
matrixStackIn.translate(0F, 0.15F - entitylivingbaseIn.danceProgress * 0.008F, 0.02F);
matrixStackIn.scale(0.8F, 0.8F, 0.8F);
```

Solved so the brim bottom lands on the head top across the whole ramp, not just at full dance —
`danceProgress` 0 / 2.5 / 5 give brim-vs-head-top of h 3.3 vs 2.7, h 13.39 vs 13.45 and h 22.54 vs
22.6, i.e. under a pixel of daylight at every point. The linear `0.15 − 0.008·dp` is what makes that
hold; a single constant drifts by ~1.5 px across the ramp.

⚠️ **The lever for a bug of this shape is the order, not the numbers.** Both earlier sweeps read the
offsets, found them upstream-identical and stopped — but an offset applied inside a rotated frame is
not the offset it is written as, and no diff can see that. When a pose helper walks an animated
hierarchy (`translateToHand` and its siblings do it in eight of this mod's layers), ask what the
frame is rotated by before reading the translate as a direction.

✅ **Client-confirmed on `26.2-fabric` (2026-08-16)** — the reporter's own platform. Two maraca
cockroaches summoned in front of the player with `{Maracas:1b}` (one plain, one `NoAI:1b` so it could
be walked up to), both reading back `Maracas: 1b, Dancing: 1b` from the server: *"both were dancing
fine and the hat was placed correctly."* The derivation held — no in-client tuning was needed, which
is the payoff for solving the offsets rather than guessing them a client launch at a time.

Applies to all 49 nodes — the arithmetic is era-invariant, and `HumanoidModel.createMesh`'s head pose
and Citadel's `translateAndRotate` are both unchanged across the range (checked in the 26.2 patched
sources and the vendored copy respectively).

### Verification for the twentieth pass

- `processResources` on six era-spanning nodes in one invocation, `build/resources` cleared first
  (rule 9, migration logic changed): `1.21.1-fabric` (below the boundary — control),
  `1.21.4-neoforge`, `1.21.8-forge`, `1.21.11-fabric`, `26.2-neoforge`, `26.2-fabric`.
  `BUILD SUCCESSFUL`, `GRADLE_EXIT=0`.
- Projections confirm the intended split. On all five ≥1.21.4 nodes
  `assets/alexsmobs/items/shattered_dimensional_carver.json` is now
  `{"model":{"type":"minecraft:special","base":"alexsmobs:item/shattered_dimensional_carver","model":{"type":"alexsmobs:icon"}}}`
  and the base model keeps `gui_light: front`, the `particle` texture and all **8** display contexts
  with only `parent` stripped — so `ModelRenderProperties.fromResolvedModel` reads the same
  transforms the ISTER got. `1.21.1-fabric` is untouched: no `items/` definition, model still
  `"parent": "builtin/entity"`.
- The animation clock was traced end to end: the carver branch's `tick` is
  `Minecraft.getInstance().player.tickCount` in normal play (the static `AMItemstackRenderer
  .ticksExisted` only when paused or player-less), so the drift advances regardless of the render
  path. `incrementTick()` is wired on all three loaders — three arms in `ClientEvents#clientTick`,
  the Fabric one through the `fabric/forge/event/TickEvent` shim.
- ⚠️ **No client session.** #92 wants one look on any ≥1.21.4 node: the shattered carver in a hotbar
  slot should show eleven shards drifting apart, not a solid carver.

## Twenty-first pass (2026-08-16) — four items, against published `2.0.14`/`2.0.15`

One reporter, on **Fabric 26.2**. Four items; one needed no code change, one was already fixed in a
release the reporter had not installed, and the other two had each been broken since `2.0.0`.

### #93 — the void worm never appears when a mysterious worm is thrown into the End's void — FIXED, 17 Fabric nodes

> *"The void worm doesnt appear when you throw a mysterious worm into the ends void
> (2.0.14-fabric+26.2)"*

The whole summon is `ItemMysteriousWorm#onEntityItemUpdate`: it watches the dropped `ItemEntity`,
and when it falls below the void in `alexsmobs:void_worm_spawnable` dimensions it consumes the stack
and spawns `EntityVoidWorm`. That method is **`IItemExtension#onEntityItemUpdate` — a Forge-family
extension**. Forge's own patch calls it from the first line of `ItemEntity#tick` and returns early
when it answers true. Fabric has no equivalent, and a grep of the whole tree found **no caller
outside that patch** — not in `fabric/forge/**`, not in `FabricServerEvents`, nowhere. So the hook
has never fired on any of the 17 Fabric nodes and **the void worm has been unsummonable on Fabric
since Milestone 15**, i.e. in every release from `2.0.0` on. The compiler cannot see it (the override
is legal — the interface is a compile-time shim there), and neither can any of the four verifiers.

Fixed with `mixin/fabric/FabricItemEntityMixin`: a `cancellable` `@Inject` at `HEAD` of
`ItemEntity#tick`, cancelling when the hook answers true, which is Forge's early return exactly.
Dispatched off `stack.getItem() instanceof ItemMysteriousWorm` rather than through the interface —
it is the only implementor in the mod, so nothing else pays for the check. `tick()V` is
byte-identical on every Fabric node in the range, so the mixin has no era arms;
`verify_mixin_targets.py` re-checks the descriptor per node regardless.

⚠️ **Generalisation (new, and the reason this survived six years of ports):** *a Forge-family
extension interface method with no Fabric caller is invisible to everything.* It compiles, it
carries `@Override` against the shim, `verify_overrides.py` sees a real override of a real
supertype method, and no gate can tell that nothing on the other loader ever calls it. The way to
find the rest is to enumerate `IForgeItem`/`IItemExtension`/`IForgeBlock`/… methods this mod
overrides and check each for a caller on Fabric — **not** to look for compile or verifier output.

#### ✅ Verified live on `26.2-fabric` (2026-08-17) — and it needs no client and no clicking

The hook watches an **`ItemEntity`**, so the whole chain is reachable from RCON alone; the player
never has to hold, throw or even be in the dimension:

```
execute in minecraft:the_end run forceload add 0 0
execute in minecraft:the_end positioned 0.0 -50.0 0.0 run summon item ~ ~ ~ \
        {Item:{id:"alexsmobs:mysterious_worm",count:1}}
```

Four seconds later, every link of the chain was observable over RCON:

| check | result |
|---|---|
| `tag @e[type=alexsmobs:void_worm] add …` | `Added tag to Void Worm` — was `No entity was found` a moment before |
| `data get … Pos` | `[-0.006, 7.98, 0.399]` — spawned at `y 0` per the source and already rising, so `updatePostSummon` + `setXRot(-90)` ran |
| `data get … Health` | `160.0f` — i.e. exactly `AMConfig.voidWormMaxHealth`, so `setBaseMaxHealth` ran too |
| `tag @e[type=alexsmobs:void_worm_part]` | **39** parts, inside the source's `25 + rand(15)` |
| `tag @e[type=item,nbt={Item:{id:"alexsmobs:mysterious_worm"}}]` | `No entity was found` — `AMCompat.kill(entity)` consumed the stack |

⚠️ Two rig notes. **`forceload add` is required**: the item must *tick* to fall, and an unattended
End chunk is not loaded — the summon succeeds and then nothing ever happens, which reads exactly
like the bug. And the drop height matters: the gate is `getY() < -60` while vanilla removes entities
below `minY - 64` = `-64` in the End, so a **4-block window**. Summoning *at* `-61` would work but is
fragile; summoning at `-50` and letting it fall through crosses the window over several ticks.

⚠️ `SegmentCount` is **not** an NBT key (`data get` answers `Found no elements matching`) — it is
synched entity data. Count `void_worm_part` entities instead.

### #94 — more items that cannot be enchanted (shield of the deep, flying fish boots, …) — ALREADY FIXED in `2.0.15`

> *"i have also found other items that you cannot enchant like the shield of the deep and the flying
> fish boots, etc"*

This is #87, shipped in `2.0.15` the day before the report; the reporter names `2.0.14` in their
first message. Both items are already in
`data/minecraft/tags/items/enchantable/durability.json`, along with the other 25 damageable items —
verified in the shipped file, not just in the source. No change. The reply is "update".

### #95 — the ghostly pickaxe cannot be repaired with phantom membranes in an anvil — FIXED, 35 nodes

> *"i tried to repair the pickaxe the ghostly miner drops with phantom membranes in an anvil like in
> the original mod but it didnt let me, btw i play on fabric 26.2 on the latest mod version"*

Upstream declares the material repair by overriding **`Item#isValidRepairItem(ItemStack, ItemStack)`**.
That method was **deleted at 1.21.2** and replaced by the `DataComponents.REPAIRABLE` component, set
through `Item.Properties#repairable(Item)` / `repairable(TagKey<Item>)` — both javap-verified present
on every node ≥1.21.2 and the old method javap-verified absent. Neither Forge nor NeoForge
re-declares it. So on **all 35 nodes ≥1.21.2, every loader**, five items took no anvil material
repair at all since `2.0.0`:

| item | repair material |
|---|---|
| `ghostly_pickaxe` | `minecraft:phantom_membrane` |
| `tendon_whip` | `alexsmobs:elastic_tendon` |
| `squid_grapple` | `alexsmobs:lost_tentacle` |
| `skelewag_sword` | `minecraft:bone` |
| `shield_of_the_deep` | `alexsmobs:serrated_shark_tooth` **and** `#minecraft:planks` |

Two of them (`shield_of_the_deep`, `skelewag_sword`) already had their override gated `//? if <1.21.2`
with a `// SLICE:` comment saying the repair was *dropped* above — a deliberate, documented loss that
nobody had costed. The other three were ungated overrides of a method that no longer exists, i.e.
dead code with no compile error. The shield's `<1.21.2` body also calls `super.isValidRepairItem`,
which is `#minecraft:planks`; the tag reproduces both, so the behaviour is identical across the
boundary.

Fixed with `AMCompat.repairableWith(Item.Properties, String name)`, applied at registration in
`AMItemRegistry`, which on ≥1.21.2 binds and declares `alexsmobs:repairs/<name>` and below 1.21.2 is
the identity (the surviving gated overrides do the job there). Five new tag files under
`data/alexsmobs/tags/items/repairs/`, in the same shape as the 15 armour tags the mod already ships;
the existing `tags/items` → `tags/item` rename pass carries the subfolder.

Two things settled by reading bytecode rather than guessing:

- **The `TagKey` overload, not the `Item` one.** `repairable(Item)` would need `AMItemRegistry.X.get()`
  evaluated while the properties are being built, i.e. potentially before the target item is
  registered. The tag route also matches what `AMArmorMaterial` already does for the 15 armour pieces,
  so there is one mechanism in the mod rather than two.
- **`bindItemTagEmptyForFreeze(tag)` first.** javap of `Item$Properties#repairable(TagKey)` on 26.2
  shows `acquireBootstrapRegistrationLookup` → `getOrThrow(tag)` → `component(REPAIRABLE, …)`, with
  **no durability precondition** — so the call does not *fail* before `.pickaxe(...)`/`.sword(...)`,
  but it does resolve the tag against a registry that NeoForge freezes. Binding it empty first is the
  same dance the armour materials do; the datapack rebinds it at reload. ⚠️ *"Does not fail"* was read
  as *"works"*, and that is exactly where `2.0.16` went wrong — see the correction below.

⚠️ **Sixth fault of one shape (#30/#66/#71/#74/#87/#95): a version replaces a *code* mechanism with a
*data* one and nothing breaks visibly.** Here it is even quieter than #87 — three of the five classes
kept an ungated `public boolean isValidRepairItem(...)` that overrides nothing above 1.21.2, and
`verify_overrides.py` *did* list them, in among its ~33 untriaged hits per node. The detector was
working; nobody had read its output for these five lines. After the fix they are gone from that list.

#### #95 again — `2.0.16`'s fix worked for two of the five items. Corrected in `2.0.17`

**Disconfirmed in a client** (2026-08-16, `26.2-fabric`): a damaged ghostly pickaxe and a phantom
membrane in an anvil still gave the red X and an empty output. The tag file shipped correctly, and
the shipped jar's bytecode showed `AMCompat.repairableWith` being called at all five registration
sites — the helper was right and it was reached. It was simply **applied too early**.

**`ToolMaterial#applyToolProperties` / `#applySwordProperties` call `Item.Properties#repairable(this
.repairItems())` themselves.** So for the three items whose properties are built by a material, iron's
tag overwrote ours a moment after we set it, and the anvil then wanted *iron ingots*:

| item | class | how the material gets applied |
|---|---|---|
| `ghostly_pickaxe` | `PickaxeItem`/`Item` | `DiggerItem` super ctor (1.21.2–1.21.4) · `Properties#pickaxe` (≥1.21.5) |
| `tendon_whip` | `SwordItem`/`Item` | `SwordItem` super ctor (1.21.2–1.21.4) · `Properties#sword` (≥1.21.5) |
| `skelewag_sword` | `SwordItem`/`Item` | as above |
| `squid_grapple` | plain `Item` | — (no material; `2.0.16` was already correct) |
| `shield_of_the_deep` | plain `Item` | — (ditto) |

Both bands funnel through the same `applyToolProperties` call — only the *caller* moved at 1.21.5,
which is why a grep for `Properties.repairable` inside `DiggerItem`/`SwordItem` came back empty on
every version and read like an all-clear. **Grep the callee, not the caller.**

Fixed with **`AMCompat.repairMaterial(ToolMaterial base, String name)`** (≥1.21.2 only — the type does
not exist below it): it binds `alexsmobs:repairs/<name>` as before and returns a **copy of the record
with that tag in the `repairItems` slot**, so `applyToolProperties` stamps *our* tag. The six-component
ctor and all six accessor names are identical on 1.21.2/1.21.4/1.21.5/1.21.11/26.1.2/26.2 (javap), so
one helper covers all 35 nodes with no gate of its own. The three constructors pass it in **both** their
≥1.21.2 arms; their now-pointless `repairableWith` calls in `AMItemRegistry` are deleted. The two plain
`Item`s keep theirs.

**Verified headlessly on both bands** (`26.2-fabric` and `1.21.4-forge`), which is the check `2.0.16`
should have had and the reason to write it down:

```
item replace block 0 -60 0 container.0 with alexsmobs:ghostly_pickaxe
execute if items block 0 -60 0 container.0 \
    alexsmobs:ghostly_pickaxe[minecraft:repairable={items:"#alexsmobs:repairs/ghostly_pickaxe"}]
execute if items block 0 -60 0 container.5 #alexsmobs:repairs/ghostly_pickaxe   # phantom membrane
```

All five items pass the first form on both nodes, all five materials pass the second, and the negative
control — the pickaxe against `#minecraft:repairs_iron_armor` — **fails**, which is precisely the state
`2.0.16` shipped. Those two `execute if items` shapes are the two halves of
`ItemStack#isValidRepairItem` (component present and naming our tag; tag resolving to the material), so
together they are the whole anvil predicate. ⚠️ `execute if items` exists from **1.21.2**, i.e. across
the entire affected range, and needs no player — `item replace block` into a chest is enough. **Any
future component-level claim about an item should be proved this way rather than by reading the
registration site.**

### #96 — the shattered dimensional carver is held in the wrong orientation — FIXED, 6 nodes (26.x)

> *"the dimensional axe and shattered one are positioned wrongly. they need to face the same way a
> normal pickaxe does. With a point towards and away from the player."*

**Only the shattered one is a bug, and only on 26.x.** The plain `dimensional_carver` has no branch
in `AMItemstackRenderer` at all — it is an ordinary `minecraft:item/handheld` sprite with a plain
`minecraft:model` definition, i.e. byte-for-byte the vanilla pickaxe pose. There is no divergence to
fix; if it still looks wrong to the reporter that needs a screenshot.

The shattered one is the #92 item: since the twentieth pass it is a `minecraft:special` model routing
into `AMItemstackRenderer`, which draws eleven `dimensional_carver_shard_*` models. Its base model's
display block is the identity, so **the pose comes entirely from each shard's own `item/handheld`
transform for the passed display context** — and `AMIconSpecialRenderer`'s `>=26` arm passed a
hardcoded `GUI`. In hand, every shard therefore rendered in the flat inventory pose instead of
pointing away from the player. The 1.21.4→1.21.11 arms were always handed the real context and were
never affected; below 1.21.4 the ISTER gets it too.

The cause is a signature change, not a deletion: **26.1 dropped the `ItemDisplayContext` parameter
from `SpecialModelRenderer#submit`**. The value did not go away, it moved one frame up —
`ItemStackRenderState` still carries `displayContext`, and its `submit` is the **sole** caller of the
private per-layer `submit` that reaches a special renderer (javap-verified on 26.1.2 and 26.2, where
the two classes are identical in this respect). So `mixin/client/ItemStackRenderStateMixin` lends it:
`@Shadow` the field, push it at `HEAD` of `submit`, pop at `RETURN`, and the renderer reads the top
of the stack with `GUI` as the fallback. A `ArrayDeque` rather than a field because an icon's own
render can draw another item (the dictionary and the advancement icons both do), and bounded at 32
purely as leak insurance — the pop rides `@At("RETURN")`, so an exception mid-render would otherwise
strand an entry and a stale entry is a wrong pose *forever* rather than for one frame.

The mixin is 26.x-only on both seams: a source-set `exclude` in `ModPlatformPlugin.configureJava`
below 26, and a `DataPackMigration.pruneMixinEntries` call in `processResources` to match — the
shipped `alexsmobs.mixins.json` is auto-populated from an `@Mixin` scan, so a new mixin needs no hand
edit to the JSON, only those two.

This also retires the "membership test" wording in `AMIconSpecialRenderer` and `DataPackMigration`:
the 26.x arm is no longer context-blind, so the test is purely **"does the context read pick a
different *model*"** on every era, which is what #92 had already narrowed it to.

### Verification for the twenty-first pass

- Compile on **10 era-spanning nodes in one invocation** — `1.20.1-forge`, `1.20.1-fabric`,
  `1.21.1-fabric`, `1.21.3-neoforge`, `1.21.5-forge`, `1.21.8-neoforge`, `1.21.11-fabric`,
  `26.1.2-forge`, `26.2-fabric`, `26.2-neoforge` — covering both sides of the 1.21.2 repair
  boundary, both sides of the 26 submit boundary, and all three loaders.
  `BUILD SUCCESSFUL`, `GRADLE_EXIT=0`. `build/resources` cleared first (rule 9 — `processResources`
  wiring changed).
- Mixin-config membership is exactly as predicted: `ItemStackRenderStateMixin` appears in the
  generated `alexsmobs.mixins.json` on `26.1.2-forge`/`26.2-fabric`/`26.2-neoforge` **only**, in the
  `client` array on Fabric (partitioned) and the `mixins` array on Forge/NeoForge;
  `fabric.FabricItemEntityMixin` on the Fabric nodes only.
- Tag projection checked on both sides of the rename: `tags/items/repairs/` on `1.20.1-forge`,
  `tags/item/repairs/` on `26.2-fabric`, all five files present with the right values.
- Release jars built for the same 10 nodes (`MOD_IS_RELEASE=true`), both new mixin classes confirmed
  in them by `unzip -l` + `javap`.
- `verify_mixins.py` `jars=10 problems=0`; `verify_mixin_targets.py`
  `nodes=49 jars=10 selectors=232 problems=0 skipped=39` (the 39 unbuilt nodes correctly reported as
  *not verified* rather than green); `verify_assets.py` `literals=394 missing=0` (unchanged baseline);
  `verify_convention_tags.py` `nodes=17 problems=9` — the documented by-design `#c:tools/spear(s)`
  optionals, unchanged.
- `verify_overrides.py --node 26.2-neoforge --baseline=1.20.1-forge` → `regressions=33`, the
  documented pre-existing count, and **no `isValidRepairItem` among them** any more.
- ⚠️ Originally shipped with **no client session for any of the three fixes** — and the one that got
  a session first, **#95, turned out to be broken** (see the correction above; refixed in `2.1.0`
  and proved headlessly on both bands). **All three are now closed.** #95 headlessly on both bands;
  **#93 ✅ live on `26.2-fabric` 2026-08-17** (see its own section — it turned out to need no client
  at all, only `forceload` and a `summon item`); **#96 ✅ client-confirmed on `26.2-fabric`
  2026-08-17**, both carvers held at once — the shattered one in the main hand and the plain one in
  the off hand, so the pair is its own control. ⚠️ **Hold both**: the fix is 26.x-only and applies to
  the shattered item alone, and the plain carver lying flat is *correct* (an ordinary
  `item/handheld` sprite, i.e. the vanilla pickaxe pose), so a session that holds only one of them
  cannot tell a fixed pose from an unfixed one.

### `2.0.16` release record (2026-08-16)

Shipped the same day the pass was written, all **49** nodes on **both** stores, committed and pushed
first (`6087982`). Build: stale `2.0.15` jars and all 49 `build/resources` trees moved out (rule 9 —
`ModPlatformPlugin`'s `processResources` wiring changed), one 49-task invocation with
`--max-workers=4`, `BUILD SUCCESSFUL in 9m 21s`, 147 jars, zero `-SNAPSHOT`, zero non-`2.0.16`.

Verifiers, all predicted before running:

| verifier | predicted | got |
|---|---|---|
| `verify_mixins.py` | `jars=49 problems=0` | ✅ |
| `verify_mixin_targets.py` | `selectors=1070` (`1041 + 17 + 12`) | ✅ `nodes=49 jars=49 problems=0 skipped=0` |
| `verify_assets.py` | `literals=394 missing=0` (unchanged — tags are data) | ✅ |
| `verify_convention_tags.py` | `problems=9`, the spear optionals | ✅ |

⚠️ **The selector prediction is only sharp if you first read how the tool counts.**
`verify_mixin_targets.py` counts `method="…"` selectors and **not** `@Shadow` fields — so
`ItemStackRenderStateMixin` contributes 2 per node, not 3, and the answer is 1070 rather than 1076.
Reading that out of the script beforehand is the difference between "exactly as predicted" and
"close enough", and only the first of those can detect a mixin that landed on the wrong node set.

Modrinth: pilot `2.0.16+26.2-fabric` (`A7Gj3bgb`) verified by a fresh single-version GET — file
attached, `fabric`/`26.2`, Fabric API pinned to `lVXlbH4w`, CodxLib project-level, **and the
changelog body confirmed to be `2.0.16`'s** (no stale per-node `modrinth-changelog.md` existed;
checked, not assumed). Then `uploaded=48 skipped=1 failed=0` → **877** project versions, 49 ×
`2.0.16`, split 17/16/16, **zero suspect rows**, no duplicate file rows, every Fabric version
carrying both dependencies and every other version exactly one. Sixth release running with no
post-upload damage; the repair ladder went unused.

CurseForge: pilot `--only 26.2-fabric` (file `8661982`) then `uploaded=48 skipped=1 failed=0`,
**49 unique file ids** — read the id count, not `uploaded`, since the pilot self-skips from the
ledger. Ledger at 10 × 49 = 490.

---

## Twenty-third pass (2026-08-16) — one report, against published `2.0.16`

Same reporter as #45/#47/#49 — PojavLauncher/Android, MC **26.2** (identified from the on-screen
`GUI` button and the 26 fps counter in their screenshot, not asked for). One message, two claims:

> I just found a bug regarding the mining ghost where his spectral pickaxe is missing from the
> dictionary. And his pickaxe isn't shiny transparent either, just like in the official mod.

"Mining ghost" / *Mineiro Fantasma* is the **underminer**. The screenshot is its animal-dictionary
page, and the mob on it is holding nothing at all.

### #97 — the animal dictionary's three "pose for the page" flags are cleared before the page is drawn

**21 nodes (≥1.21.6, all three loaders), since `2.0.0`.** The underminer is the visible half; the
laviathan and the murmur have the same fault on the same nodes and nobody has reported either.

The book's mob is a **fake entity** built by `EntityType#create` — no `finalizeSpawn`, so no
equipment (`EntityUnderminer:249` is where a real one is handed its pickaxe). Upstream covers that
with three static flags read at draw time:

```java
// GUIAnimalDictionary#render
RenderLaviathan.renderWithoutShaking = true;
RenderMurmurBody.renderWithHead      = true;
RenderUnderminer.renderWithPickaxe   = true;
super.render(guiGraphics, x, y, partialTicks);
RenderLaviathan.renderWithoutShaking = false;   // ← already false by the time the mob is drawn
RenderMurmurBody.renderWithHead      = false;
RenderUnderminer.renderWithPickaxe   = false;
```

`LayerUnderminerItem#render` reads `renderWithPickaxe` and substitutes a `GHOSTLY_PICKAXE` when it
is set. That works for as long as `super.render` actually *draws* the entity — which stopped being
true at **1.21.6**. `GuiBasicBook#drawEntityOnScreen`'s `>=1.21.6` arm calls
`AMRenderCompat.submitGuiEntity`, which only **files a picture-in-picture element** into the frame's
render state; `GuiRenderer` renders it after the screen's `render()` has returned. So the sequence is
extract (flags true) → reset to false → **draw** (flags false) → no pickaxe.

This is the third bug from the same 1.21.6 deferral, after **#56** (every GUI entity sampling the
last-rendered texture) and **#62** (the invented viewport clipping the elephant). The pattern is
worth stating on its own: **from 1.21.6, anything a screen sets around its own `render()` call is
gone before its entities are drawn.** A static flag, a `RenderSystem` state, a mutated field —
if a renderer reads it at draw time, it has to travel in the render state instead.

**Fix** — `client/render/AMBookPose.java`, a three-bit mask with `capture()` / `swap(mask)`:

- the two compat shims snapshot it into `AMRenderState#bookFlags` in `extractRenderState`, which
  still runs inside the screen's own call;
- both `dispatch(...)` bodies (`compat/EntityRenderer`, `compat/LivingEntityRenderer`) `swap` it in
  around `this.render(entity, …)` and restore the previous mask in a `finally`.

Chosen over a mixin on the GUI path deliberately: `mixin/client/GuiRendererMixin` — the #56 pool —
is `//? if !neoforge`, so a fix there would miss NeoForge ≥1.21.6 entirely. The compat shims are the
one seam every loader shares. It is a no-op in the world and on 1.21.2–1.21.5, where extraction and
drawing are already in the same call, and the compat package is excluded from the compile below
1.21.2, so there is nothing to gate.

`AMItemstackRenderer#renderByItem` sets the same three flags around its **own** synchronous
`drawEntityOnScreen`, so the creative-tab and advancement icons were never affected — the java that
reads the flags runs inside `submit`, and only the GPU work is deferred there.

### The second claim — "isn't shiny transparent"

Partly a consequence, partly a real and already-documented divergence.

- The **transparency is present** on the reporter's node. `DataPackMigration.ghostifyPickaxeTexture`
  bakes `ghostly_pickaxe.png` down to alpha **140** on exactly the nodes with no Forge model
  wrapper; PIL on `versions/26.2-fabric/build/resources` **and on the shipped
  `alexsmobs-2.0.16-fabric+26.2.jar`** reads alphas `[0, 140]`, while `1.21.1-forge` (which has the
  wrapper) keeps `[0, 255]`. So the pass is doing its job.
- The **shine is genuinely absent** on the 37 wrapper-less nodes, and that is the known #69 degrade:
  upstream's glow is a *render type* (`LIGHTNING_TRANSPARENCY`, additive) selected by
  `GhostlyPickaxeBakedModel`, a Forge `BakedModelWrapper` that only exists on `<1.21.4 && !fabric`.
  Restoring additive blending on ≥1.21.5 means a custom `RenderPipeline` registered per loader; it is
  not a texture or a light value. Deferred, with #47's custom particle path.

Since no pickaxe was drawn at all in the screenshot, the reporter cannot have judged its shine on
that page — expect this half to look different once #97 lands, and re-ask before building a pipeline.

### Verification

- Compile-green on **8 era-spanning nodes in one invocation** — `1.20.1-forge`, `1.21.1-fabric`
  (both below the compat package's 1.21.2 floor, so they prove `AMBookPose` stands alone),
  `1.21.3-neoforge`, `1.21.5-forge` (compat active, below the 1.21.6 deferral), `1.21.8-forge`
  (≥1.21.6, pre-submit), `1.21.11-fabric` (≥1.21.9 submit), `26.2-fabric`, `26.2-neoforge`.
  `BUILD SUCCESSFUL in 1m 15s`, `GRADLE_EXIT=0`.
- No new mixin and no new asset, so all four verifier baselines are unchanged.
- ⚠️ **No client session.** One look at the underminer's dictionary page on any ≥1.21.6 node closes
  it — and the same page closes the laviathan (should stand still, not shake) and the murmur
  (should have its head).

## Twenty-fourth pass (2026-08-16) — one item, shipped in `2.1.0`

Not a player report against a symptom, but the user relaying that *"people are reporting that the
mod wont work nicely with MCA reborn"*
([Luke100000/minecraft-comes-alive](https://github.com/Luke100000/minecraft-comes-alive),
[CurseForge](https://www.curseforge.com/minecraft/mc-mods/minecraft-comes-alive-reborn)) — no
symptom named, so the failure mode had to be derived. The version was bumped `2.0.17` → **`2.1.0`**
for it, at the user's instruction.

### #98 — every interaction this mod adds is dead on MCA Reborn villagers — FIXED, 32 Forge/NeoForge nodes

**Symptom.** On Forge and NeoForge, with MCA Reborn installed: a villager caught with a **vine
lasso can never be released**, **chorus fruit will not cure its ender flu**, and a **sponge will not
wipe the rainbow off it**. The lasso one is the documented upstream report —
[AlexModGuy/AlexsMobs#2299](https://github.com/AlexModGuy/AlexsMobs/issues/2299), *"Lasso with mca
villager is stuck … sometimes leads to a blockage, making the liberation of the villager
impossible"* (open, no comments, filed against 1.20.1 Forge). Killing the villager was the only way
out, because `ServerEvents#onInteractWithEntity` is the **only** detach path
(`VineLassoUtil.lassoTo(null, living)`); `ItemVineLasso` is the throwing side only.

**Cause — the interaction *phase*, not the entity.** Vanilla `Minecraft#startUseItem` sends
`ServerboundInteractPacket` **`INTERACT_AT` first**, and only falls through to **`INTERACT`** when
the first did not consume. Forge and NeoForge fire two different events off that split:

| packet action | loader event | fired |
|---|---|---|
| `INTERACT_AT` | `PlayerInteractEvent.EntityInteractSpecific` | **before** `Entity#interactAt` |
| `INTERACT` | `PlayerInteractEvent.EntityInteract` | before `Player#interactOn` |

Alex's Mobs listened to **`EntityInteract` only**. MCA Reborn's
`net.conczin.mca.entity.VillagerEntityMCA#interactAt` is **`final`** and *consumes* a non-shift
main-hand right-click (it opens MCA's interaction GUI), so the client never falls through, the
`INTERACT` packet is never sent, and `EntityInteract` never fires. Nothing about the villager's
*type* is wrong — it is a genuine `net.minecraft.world.entity.npc.Villager` subclass.

Verified rather than assumed:
- Patched `ServerGamePacketListenerImpl$1` bytecode (NeoFormRuntime `recompile_*_output.jar`) shows
  `CommonHooks.onInteractEntityAt(...)` invoked **before** `Entity.interactAt(...)`.
- `PlayerInteractEvent$EntityInteractSpecific.class` is present in forge 1.20.1 universal, neoforge
  21.1.216 universal and neoforge 26.2.0.37-beta universal — one handler covers all 32 nodes.

**Fabric is not affected**, which is why the upstream report is a Forge one. `FabricServerEvents`
wires Fabric API's `UseEntityCallback`, whose `ServerGamePacketListenerImplMixin` injects into
`handleInteract` *ahead of the dispatch*, so it already runs before `Entity#interactAt` for **both**
packet actions.

**Ruled out while diagnosing** (no change needed, do not re-investigate):
- `#alexsmobs:villagers` and the seven `*_targets` tags built on it. MCA's `MixinEntityType`
  `@ModifyReturnValue`s `EntityType#is(TagKey)` so `mca:male_villager`/`mca:female_villager` report
  membership in **any** tag that contains `minecraft:villager` (gated on `Config.villagerTagsHacks`)
  — the mod's villager-hunting mobs already see MCA villagers.
- The `AbstractVillager`-targeting goals — same reason, MCA extends vanilla `Villager`.
- MCA's `client/MixinLivingEntityRenderer` (Player-only), `MixinServerGamePacketListenerImpl`
  (`handleChat` only) and `MixinVillager` (records the spawn reason) — none touch anything this port
  mixes into.
- MCA issue #557 — unrelated, alexsmobs appears only in the reporter's mod list.

**Fix.** `ServerEvents` now listens to **both** phases. The body moved into a private
`interactWithEntity(Player, ItemStack, Level, Entity, Runnable consume)` and each handler passes the
cancel it owns, so the `//? if forge && >=1.21.6` eventbus-7 shape (`AMCompat.cancelIf` /
`AMCompat.cancelEvent` in place of `setCanceled`) is expressed **once per handler** instead of four
times in the body. ⚠️ The shared method takes the **concrete** pieces, not a `PlayerInteractEvent`:
on modern NeoForge `setCanceled` lives on `ICancellableEvent`, which the *subclasses* implement and
the abstract base does not, so a base-typed parameter would not compile there.

No double-fire: cancelling in the `INTERACT_AT` phase makes the **client** return `SUCCESS` from its
own `interactAt`, so `startUseItem` never sends the `INTERACT` packet at all. And the only
behavioural change for anything that is *not* MCA is on entities whose `interactAt` consumes — this
mod has **zero** `interactAt` overrides of its own (grepped), and the body still acts on exactly its
four narrow conditions.

Fabric gets an `EntityInteractSpecific` inner class in the stub
`fabric/forge/event/entity/player/PlayerInteractEvent`, documented as **deliberately never fired**
— `UseEntityCallback` already covers both actions, so firing it too would apply every handler twice.
It exists so the shared handler compiles there.

⚠️ **Generalisation: an event is a *phase*, not a subject.** A handler on the later of two
interaction phases is invisible to any mod that consumes the earlier one, and nothing in this tree
can see that — it compiles, it registers, it simply never runs. Listen to both phases for anything
that must work on someone else's entity.

### Verification for the twenty-fourth pass

- Compile-green on **10 era-spanning nodes in one invocation** — `1.20.1-forge` (baseline Forge),
  `1.21.2-neoforge` (the `convertTo` boundary), `1.21.5-neoforge`, `1.21.8-forge` and
  `1.21.11-forge` (both **forge && >=1.21.6**, the eventbus-7 arm), `26.2-forge`, `26.2-neoforge`,
  and `1.20.1-fabric` / `1.21.11-fabric` / `26.2-fabric` for the stub. `BUILD SUCCESSFUL in 1m 9s`,
  `GRADLE_EXIT=0`.
- Both arms spot-checked in the `1.21.8-forge` projection: the `>=1.21.6` handlers uncomment to
  `public boolean … AMCompat.cancelIf(…)` and the `else` arm stays commented.
- No new mixin and no new asset, so all four verifier baselines are unchanged.
- ⚠️ **No client session, and none is possible here** — closing this needs MCA Reborn installed on a
  Forge or NeoForge node: lasso an MCA villager and right-click it to free it again.

## Twenty-sixth pass (2026-08-16) — one item, found in a client session, in the tree for `2.1.1`

Not a player report. Nobody filed this one because nobody who hit it could have described it as an
Alex's Mobs bug: the game does not start, and the crash names whichever of the two mods lost a coin
toss.

### #99 — Alex's Mobs and MCA Reborn cannot be installed together: hard crash at bootstrap — FIXED, 35 nodes

Found while setting up the `26.2-fabric` client session that closed #55. MCA Reborn
(`mca-fabric-8.1.9+26.2.jar`) had been left in `versions/26.2-fabric/run/mods/` by the #98 work, and
`:26.2-fabric:runServer` died before the world loaded:

```
[Mixin] @Redirect conflict. Skipping alexsmobs.mixins.json:EntityMixin from mod alexsmobs
  ->@Redirect::alexsmobs_allowRidingUnsaveableVehicle(Lnet/minecraft/world/entity/EntityType;)Z
  with priority 1000, already redirected by mca.mixins.json:MixinEntity from mod mca
  ->@Redirect::mca$allowCarriedVillagersToRidePlayers(...)Z with priority 1000
java.lang.RuntimeException: Mixin transformation of net.minecraft.world.entity.Entity failed
Caused by: InjectionError: Critical injection failure: Redirector alexsmobs_allowRidingUnsaveableVehicle
  ... failed injection check, (0/1) succeeded. Scanned 0 target(s).
  at MixinProcessor.applyMixins -> Bootstrap.bootStrap -> net.minecraft.server.Main.main
```

⚠️ **`BUILD SUCCESSFUL`, exit code 0.** Rule 7 again, in its purest form — the Gradle run task
reports the *launcher's* status, and the launcher exited normally after the game died. The only
evidence is in the log body.

**Both mods redirect the same call, for the same reason.** #81 added a `@Redirect` on the
`EntityType.canSerialize()` call inside `Entity#startRiding`, so this mod's seven player-riding mobs
can mount a player again after 1.21.2 started rejecting unsaveable vehicles. MCA needs exactly the
same hole for its carried children — read out of its jar with `javap`, its handler is:

```java
private boolean mca$allowCarriedVillagersToRidePlayers(EntityType<?> type, Entity vehicle, boolean a, boolean b) {
    if (b && vehicle instanceof Player && (Object) this instanceof VillagerEntityMCA v) {
        AgeState s = v.getAgeState();
        if (s == BABY || s == TODDLER || s == CHILD) return true;
    }
    return type.canSerialize();
}
```

`@Redirect` **replaces** a call, so two of them on one call site are mutually exclusive by design:
Mixin logs the conflict, keeps the first and skips the second. The skip is only fatal because of
`defaultRequire: 1` — and **MCA declares `defaultRequire: 1` too**. So the pack fails to launch
whichever way the application order falls; all that varies is which mod gets blamed in the report.
That makes this strictly worse than #98, which the twenty-fourth pass had just fixed for the same
mod pairing: #98 was dead interactions, this is no game.

**35 nodes** (everything >=1.21.2), all three loaders, since `2.0.14`.

**Fix: `@ModifyExpressionValue` instead of `@Redirect`.** MixinExtras' wrapper injectors modify the
*value* an expression produced rather than replacing the call that produced it, which is explicitly
stackable — any number of mods may wrap the same expression. MCA's redirect still runs, its result
arrives as `original`, and this mod ORs its own answer in:

```java
@com.llamalad7.mixinextras.injector.ModifyExpressionValue(
        method = "Lnet/minecraft/world/entity/Entity;startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;canSerialize()Z"))
private boolean alexsmobs_allowRidingUnsaveableVehicle(boolean original) {
    return original || AMCompat.ridesUnsaveableVehicles((Entity) (Object) this);
}
```

Both era arms change identically (the 1.21.9 move of the body into the three-argument `startRiding`
is untouched by this). It is also simply the better injector for the job: the handler no longer has
to re-call `canSerialize()` itself, so it cannot get that call wrong.

⚠️ **Forge needed a build change; the other two loaders did not.** Fabric Loader and NeoForge both
put MixinExtras on the compile classpath, so this compiled on all 17 Fabric and all 16 NeoForge
nodes untouched. Forge **bundles and bootstraps MixinExtras but does not expose it to javac** —
`package com.llamalad7.mixinextras.injector does not exist`, on Forge alone, on exactly the three
Forge nodes in the first compile sweep. Fixed with `compileOnly("io.github.llamalad7:mixinextras-
common:0.5.4")` in **both** Forge buildscripts (`build.forgeg.gradle.kts` for 1.20.1→1.21.11 and
`build.forgenr.gradle.kts` for the two 26.x nodes — it is easy to patch one and think it is done).
`compileOnly` is exactly right: javac needs the annotation, the jar must not shade it, and the
transformer that reads it is whichever one the loader ships.

The annotation is written **fully qualified, with no import**, so the nodes below 1.21.2 — where
both arms are commented out and the class legitimately declares no injectors — do not reference
MixinExtras at all.

⚠️ **Generalisation, and it is the lesson of #81 turned around: `@Redirect` is a claim of
exclusivity, so choosing it is a bet that no other mod will ever want the same call.** #81's own
javadoc made that bet explicitly — *"the redirect target is one obscure call in one vanilla method,
so the usual 'redirects are exclusive' objection costs nothing here"* — and the bet was lost to the
very mod this port was already writing a compatibility fix for. The tell that should have raised it:
**a call worth redirecting because it blocks a feature you want is, for that reason, worth
redirecting to every other mod that wants the same feature.** Reach for a MixinExtras wrapper
(`@ModifyExpressionValue`, `@WrapOperation`) by default and keep `@Redirect` for cases where you
genuinely must suppress the call.

### Verification for the twenty-sixth pass

- Compile-green on **9 era-spanning nodes in one invocation**, `GRADLE_EXIT=0`: `1.20.1-forge` (the
  below-boundary control, both arms commented out), `1.21.1-fabric` (below-boundary), `1.21.2-
  neoforge` (first node of the `>=1.21.2` arm), `1.21.3-forge`, `1.21.8-forge`, `1.21.11-fabric`,
  `26.2-forge`, `26.2-neoforge`, `26.2-fabric` — i.e. both era arms on all three loaders, and both
  Forge buildscripts.
- ✅ **Runtime-verified with MCA actually installed**, which is what the twenty-fourth pass could not
  do: `mca-fabric-8.1.9+26.2.jar` in `versions/26.2-fabric/run/mods/`, `:26.2-fabric:runServer`
  reaches `Done (0.982s)! For help, type "help"` and `RCON running`, with **no `@Redirect conflict`
  line and no `InjectionError`** anywhere in the log. The same command with the same jar crashed at
  `Bootstrap` before the change, so this is a before/after on one environment.
- ⚠️ **A successful boot is itself the proof that our injector applied**, not merely that it stopped
  crashing: `defaultRequire: 1` is unchanged, so a `@ModifyExpressionValue` that had failed to find
  its target would have aborted the launch exactly as the `@Redirect` did. There is no silent-skip
  outcome left to worry about.
- ✅ **Second environment, and a positive proof rather than the absence of a crash** (2026-08-16):
  `mca-neoforge-8.1.9+26.1.2.jar` in `versions/26.1.2-neoforge/run/mods/` — a different MC version
  *and* a different loader. That build carries the identical collision (javap: `@Redirect`,
  `method="startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z"`,
  `target="Lnet/minecraft/world/entity/EntityType;canSerialize()Z"`, and `mca.mixins.json` again
  declares `defaultRequire: 1`). `:26.1.2-neoforge:runServer` reaches `Done (0.276s)` with both mods
  in the mod list and no conflict line. Re-run with **`JAVA_TOOL_OPTIONS="-Dmixin.debug.export=true"`**
  and `javap` the exported `run/.mixin.out/class/net/minecraft/world/entity/Entity.class`: inside
  `startRiding(Entity,boolean,boolean)` the two handlers are **chained in the right order** —

      41: invokespecial redirect$zzl000$mca$allowCarriedVillagersToRidePlayers:(...)Z
      44: aload_0
      45: swap
      46: invokespecial modifyExpressionValue$zzb000$alexsmobs$allowRidingUnsaveableVehicle:(Z)Z

  i.e. MCA's redirect still *replaces* the call and our wrapper consumes **its** result as
  `original`, exactly as intended. Both mods' behaviour survives; neither was skipped.
  ⚠️ `JAVA_TOOL_OPTIONS` is the way to get a system property into a dev run without editing a
  buildscript — the JVM picks it up itself, so it reaches the forked run task.
- ✅ **`1.21.10-fabric`, same export trick, no MCA available** — `Done (0.323s)` and
  `modifyExpressionValue$zia000$alexsmobs$allowRidingUnsaveableVehicle` present in the transformed
  `Entity`, so the arm applies at runtime on a mid-range node too, not only on 26.x.
- ⚠️ **MCA Reborn cannot be tested on 1.21.10 — or on anything between 1.21.1 and 26.1.2 — because
  it publishes no build there.** Its 1.21+ line is `1.21.1`, then `26.1.2`, then `26.2` (Modrinth
  `1W98a849`, cross-checked against the CurseForge file list for project `535291`), and `1.21.1` is
  *below* `1.21.2`, where neither mod has an injector at all. So the whole overlap between MCA's
  support and this bug's range is **{26.1.2, 26.2}**, both of which are now covered, and both of
  which sit in the same `>=1.21.9` arm. The `>=1.21.2 && <1.21.9` arm is compile- and
  selector-verified only; testing *coexistence* there would need a synthetic stand-in mod, since no
  real MCA exists for it.
- ✅ **Behavioural check done in a client, `26.2-fabric`, 2026-08-17, with MCA actually installed.**
  A crimson mosquito latched onto the player and `data get entity <player> Passengers` returned a
  live `alexsmobs:crimson_mosquito` — i.e. the `startRiding` → `canSerialize()` call that both mods
  wrap succeeded for *our* rider while MCA's redirect was installed on it. MCA itself was exercised
  in the same session (two villagers summoned and named by its own naming system, zero MCA lines in
  the log). Incidentally the mosquito's first target was a **kangaroo** and it rode that too, so the
  ordinary mob-on-mob path is unaffected.
  ⚠️ **`/ride <rider> mount <player>` cannot test this** — vanilla's `RideCommand` rejects a player
  vehicle at the *command* level (`Players can't be ridden`) long before `startRiding` is reached, so
  the AI path is the only route. And the AI path needs the player in **survival**:
  `EntityCrimsonMosquito` dismounts on the next tick from a mount that `instanceof Player &&
  isCreative()`. Give Resistance rather than lowering the difficulty; the latch took ~10 s.

## Twenty-seventh pass (2026-08-16) — one report, against published `2.1.0`

> *"i started a new world and the desert mobs still arent spawning but other mobs are"*

MC version and loader not stated; the wording (**"still"**, and *"i started a new world"* as something
already tried) says this is a follow-up, and the most likely author is the eighteenth/nineteenth
pass's reporter — Fabric, 26.2 — who was told #89 was fixed in `2.0.15`.

The desert half is **not reproducible**. Chasing it found something else, which is the whole value of
the pass: **the #89 fix has never reached anybody who already had a config file**, which is very
nearly everybody, and *"still ... after I started a new world"* is exactly what that produces.

### #100 — the `c:` → `alexsmobs:` alias routing never fires on an existing config — FIXED, 34 nodes

`SpawnBiomeData#conventionTag` is the single point every spawn-biome tag string passes through, and
its first line is:

```java
if (!value.startsWith("forge:")) { return value; }
```

That is correct for the *shipped defaults*, which are written `forge:is_snowy` in `DefaultBiomes`.
It is wrong for the **second** call site, added in `2.0.14` for #85: the private
`SpawnBiomeData(SpawnBiomeEntry[][])` constructor, which re-normalises what Gson read back out of
`config/alexsmobs/*.json`. What is *in* those files depends entirely on which version first wrote
them:

| config first written by | on disk | `conventionTag` sees | result |
|---|---|---|---|
| ≤ `2.0.13`, Fabric | `forge:is_snowy` | `forge:` prefix | → `alexsmobs:is_snowy` ✅ |
| `2.0.14`, Fabric | `c:is_snowy` | not `forge:` → returned unchanged | ❌ stuck on the narrowed tag |
| any version, NeoForge ≥1.20.5 / Forge ≥26 | `c:is_snowy` | not `forge:` → returned unchanged | ❌ |

So on NeoForge and Forge 26 the alias fix has **never** applied to a real install, and on Fabric it
applies only to configs older than `2.0.14`. Read straight off disk in
`versions/1.21.1-neoforge/run/config/alexsmobs/` (files dated 2026-07-23): `moose_spawns.json` holds
`c:is_snowy` + `c:is_wasteland`, `tusklin_spawns.json` `c:is_snowy` + `c:is_plains`,
`gelada_monkey_spawns.json` `c:is_plains` + `c:is_plateau`.

⚠️ **`config/` is not part of the world.** Starting a new world does not regenerate
`config/alexsmobs/*.json`, and neither does updating the mod — the files are written once, when the
directory is empty, and read forever after. That is why the reporter's *"I started a new world"* did
not help and why the eighteenth pass's headline fix looked, from the outside, like it had not
shipped.

Fixed by giving `conventionTag` a second prefix arm, ahead of the `forge:` one:

```java
if (value.startsWith("c:")) {
    String cPath = value.substring("c:".length());
    return switch (cPath) {
        case "is_snowy", "is_plains", "is_wasteland" -> "alexsmobs:" + cPath;
        default -> value;
    };
}
```

Safe in both directions: the three `alexsmobs:` alias tags are each defined as `#forge:is_*`
(rewritten to `#c:is_*` by the resource migration, `"required": false`) **plus** the members the
convention move dropped, so routing a value to the alias can only ever widen the match — it never
removes a biome the player's own `c:` value already had. The `forge && <26` arm is untouched: on
classic Forge `forge:is_snowy` is still the full tag and there is nothing to alias.

⚠️ **Generalisation: a normalisation that runs on *both* the shipped defaults and the persisted file
has to be idempotent over its own output.** This one rewrote `forge:x → c:x`, wrote that to disk, and
then — one release later, when the mapping changed to `forge:x → alexsmobs:x` — could no longer
recognise its own previous output as something needing rewriting. Every migration that reads back
what an earlier version of itself wrote has this shape; the test is to feed it its own output and
check the answer does not change.

### The desert half — NOT REPRODUCIBLE, and here is everything that was excluded

Static, all six desert mobs (`tarantula_hawk`, `roadrunner`, `rattlesnake` → `CREATURE`; `jerboa`,
`rain_frog` → `AMBIENT`; `triops` → `WATER_AMBIENT`):

- **Not a narrowed convention tag.** The `DESERT`/`ROADRUNNER`/`RATTLESNAKE` pools rest on
  `is_dry/overworld`, `is_hot/overworld` and `is_sandy`, none of which is one of #89's three. All
  three contain `minecraft:desert` in **all eight** cached NeoForge versions (20.6 → 26.2), in the
  Forge 26.1.2/26.2 universal jars, and in `fabric-convention-tags-v2-4.3.2`.
- **Not a missing Fabric backfill** — all three are in `DataPackMigration.fabricConventionBackfill`
  with `minecraft:desert` among the members.
- **Not a #70-family vanilla tag re-partition** — `#minecraft:sand`, the base of both
  `tarantula_hawk_spawns` and `rain_frog_spawns`, is unchanged 1.20.1 → 26.2.
- **Not the negated entry** — `minecraft:is_badlands` does not contain `minecraft:desert`, so the
  `negate: true` term passes.
- **Not spawn weights or rolls** — weights 6/12/10/8 and rolls 1/1/0/2/0/0, byte-identical to
  upstream; `rollSpawn` returns `true` outright for `rolls <= 0`.
- **Not `SpawnPlacements` registration** — all three arms of `AMEntityRegistry#placement` read
  correct.
- **Not category drift** — `git show 151e36c:...AMWorldRegistry.java` gives the same six
  `MobCategory` values, the same weights and the same count ranges as the port.

Runtime, `26.2-fabric`, **fresh** world, seed `underminer`: at the nearest desert (`locate biome` →
`[896, 66, 160]`, `forceload`ed first — ⚠️ `execute if biome` reads the live level and returns
nothing on an unloaded chunk, while `locate biome` reads the biome source and does not), all four
DESERT terms resolve — `minecraft:desert`, `#c:is_sandy`, `#c:is_hot/overworld`,
`#c:is_dry/overworld` hit and `#minecraft:is_badlands` does not. `config/alexsmobs/*.json` on disk is
correctly `c:`-normalised. Force-generating 247 chunks then counting inside 400 blocks:
**1 tarantula hawk, 2 roadrunners, 3 rattlesnakes**, against **7 rabbits** as the vanilla control
(rabbit weight 4, hawk weight 6). Same four biome assertions repeated on `1.21.1-neoforge` — the
other loader family, and the one whose `c:` arm has the most history — all pass.

⚠️ **This test structurally cannot see the AMBIENT and WATER_AMBIENT three.** A player-less dedicated
server runs no normal spawn cycles at all (`ServerChunkCache#tickChunks` only spawns in chunks with a
player nearby); the only spawning a headless rig can drive is
`NaturalSpawner#spawnMobsForChunkGeneration`, which handles **`CREATURE` only**. So jerboa, rain frog
and triops are **untested, not cleared** — and the jerboa is the iconic Alex's Mobs desert mob, so it
is the likeliest thing the report actually means. Two upstream conditions make it rare by design and
are not bugs: `canJerboaSpawn` requires `canSeeSky(pos.above())` **and** light ≤ 4, i.e. **outdoors
at night**, and `AMBIENT` has a much lower mob cap than `CREATURE`.

**Blocked on the reporter for**: MC version, loader, and *which* desert mobs — and on a client
session for the AMBIENT half, which no headless rig can reach.

### Verification (twenty-seventh pass)

Compile-green in **one** invocation on five nodes chosen to cover both `conventionTag` arms —
`1.20.1-forge` and `1.21.1-forge` (identity arm), `1.20.1-fabric`, `26.2-fabric`, `1.21.1-neoforge`,
`26.2-forge` (alias arm) — and the projections confirm the split is where it should be: **34 nodes**
take the alias arm (all 17 Fabric + NeoForge ≥1.20.5 + Forge 26.x) and **15** the identity arm
(classic Forge, where `forge:is_*` is still the full tag and there is nothing to alias).

Then proved **end-to-end, headlessly, on `1.21.1-neoforge`** — the loader family on which this fix
has *never* applied to a real install. The rig is the **gelada monkey**, which is the sharpest probe
available: it is `CREATURE` (so chunk-generation spawning reaches it without a player), and its only
vanilla pool is `is_overworld AND is_plains AND is_plateau`. Out of the NeoForge 21.1.216 universal
jar, `c:is_plains = {plains, sunflower_plains}` and `c:is_plateau = {wooded_badlands,
savanna_plateau, cherry_grove, meadow}` — **the intersection is empty**, so with the value the config
file actually holds the gelada has nowhere in vanilla to spawn at all. `alexsmobs:is_plains` adds
`meadow`, and the intersection becomes exactly `{meadow}`.

At the nearest meadow (`locate biome` → `[-1464, 151, 776]`), 169 chunks force-loaded:

```
minecraft:meadow          HIT
#minecraft:is_overworld   HIT
#c:is_plateau             HIT
#alexsmobs:is_plains      HIT
#c:is_plains              — no match          <- the whole bug, in one line
```

and the census inside 300 blocks found **2 gelada monkeys** (71 entities total) in a world whose
`config/alexsmobs/gelada_monkey_spawns.json` still says `c:is_plains` on disk. Before the fix that
number is necessarily **0**.

⚠️ Rig notes, both of which cost a false negative here: `execute … if biome … run say X` returns
**no body over RCON** — grep the server log for the marker instead of reading the reply — and
`execute if biome` needs the chunk **loaded**, so `forceload add` and a wait come first (`locate
biome` reads the biome source and does not load anything, which is why it answers on an ungenerated
region).

#### The desert half, closed in a client (2026-08-17, `26.2-fabric`)

The one desert mob a headless rig structurally cannot reach is the **jerboa** (`MobCategory.AMBIENT`
— `spawnMobsForChunkGeneration` handles `CREATURE` only, and every other spawn cycle is gated on
`anyPlayerCloseEnoughForSpawning`, so *no* player-less server can ever produce one). It was the
likeliest subject of the report and the only untested part of it. Settled with a real client joined
to the dev server, which is the rig to reuse: **`AM_CLIENT_ARGS="--quickPlayMultiplayer
127.0.0.1:25565" ./gradlew ":<node>:runClient"`** against a `runServer` on the same shared `run/`
dir, so RCON drives the world while the client supplies the player. Both buildscript hooks already
exist for exactly this.

At a real desert (`locate biome minecraft:desert` → `[2512, 66, 2672]`, `if biome` confirms), with
every jerboa killed first so the baseline is a true zero and `time set midnight`:

```
t=25s  none     t=100s  none
t=50s  none     t=125s  none
t=75s  none     t=150s  Jerboa      <- from a cleared baseline
```

plus two earlier uncontrolled sightings (2 within 128 blocks on arrival, 1 on the next pass).
**Jerboa spawning works.** The rate is simply low, and it is upstream's own: spawn weight 8, groups
of 1–3, and `canJerboaSpawn` demands `canSeeSky(pos.above())` **and** `canMonsterSpawnInLight`, i.e.
outdoors and dark. A player crossing a desert by day, or one standing in a lit camp, will never see
one — which is a complete and innocent explanation for "the desert mobs still aren't spawning".
Rattlesnakes (`CREATURE`) were present throughout at 2 within 128, matching the headless result.

⚠️ **Check the bat count before calling an AMBIENT spawn broken.** The category cap is 15 and bats
share it; 7 were loaded here. A cave-riddled desert can saturate it and starve the jerboa
completely, and nothing in the log says so.

⚠️ Two rig traps, both of which wasted a round: a **backgrounded** poll script's stdout is buffered
and can show nothing for minutes before flushing everything at once — read the world over RCON in
the foreground instead of trusting an empty task output file — and the player will **wander off**
(this one flew 1700 blocks to y −49 mid-test, unloading every entity being counted), so re-read
`data get entity <player> Pos` beside every count and put the instruction on their screen with
`title`, since `say` returns no body over RCON.

---

## Twenty-eighth pass (2026-08-17) — two items, against published `2.0.16+26.2-fabric`

One reporter, single-player, platform stated in full for once (**26.2, Fabric**):

> *"you are unable to shear Bison"* … *"bison is breaking snow blocks without reason"* (with a
> screenshot of a bison standing in a square of cleared snow on a snowy plain)

One is real and much wider than the words; the other is upstream's own feature.

### #101 — none of the four shearable mobs can be sheared on Fabric — FIXED, 17 nodes

**Symptom.** Right-clicking a bison with shears does nothing. Also true of the **alligator
snapping turtle** (moss / spiked scute), the **cockroach** (headless) and the **mungus**
(mushroom) — every mob in this mod that implements `Shearable`. All 17 Fabric nodes, since
Milestone 15, so it shipped in `2.0.0` through `2.1.1`.

**Cause — a comment that was wrong, believed by four files.** Each of the four entities carries a
gate that drops the loader shearing interface on `fabric || (forge && >=26.2)`:

```java
//? if fabric || (forge && >=26.2)
//public class EntityBison extends Animal implements IAnimatedEntity, Shearable {
//? if neoforge || (forge && <26.2)
public class EntityBison extends Animal implements IAnimatedEntity, Shearable, net.minecraftforge.common.IForgeShearable {
```

and a comment on `isShearable` justifying it:

> *"there, vanilla `Shearable` is the whole contract (`ShearsItem` goes straight through
> `readyForShearing`/`shear`, and the interface gained a `shearItems` default)"*

The parenthesis is false, and the arm is shared with Fabric, so the falsehood cost 17 nodes rather
than 6. Read out of the jars:

- **`ShearsItem#interactLivingEntity` does not exist in vanilla on any version in this range.**
  Swept every cached mapped jar 1.20.1 → 26.2: `interactLivingEntity=0` throughout.
- **Vanilla shears its own mobs from inside each mob's `mobInteract`.** `Sheep#mobInteract`
  bytecode on 26.2: `ItemStack.is(Items.SHEARS)` → `readyForShearing()` →
  `shear(ServerLevel, SoundSource.PLAYERS, stack)` → `gameEvent(SHEAR, player)` → `hurtAndBreak`
  → `SUCCESS_SERVER`. `MushroomCow`, `SnowGolem` and `Bogged` do the same. `Shearable` is a
  *contract*, not a dispatch point — only 8 classes in the 26.2 jar mention it, none of them a
  generic item hook, and it has exactly two methods (no `shearItems`).
- **`ShearsItem#interactLivingEntity` is a loader patch, present on every Forge/NeoForge build
  here** (`=1` on all of them). Below Forge 65 it dispatches on the loader's own
  `IForgeShearable`/`IShearable`; **forge-26.2-65.1.0's dispatches on vanilla `Shearable`**
  (`instanceof net/minecraft/world/entity/Shearable` in the bytecode) — which is what the comment
  was actually describing, and it is true *there*.

So `26.2-forge` works through the patched hook, every other Forge/NeoForge node works through the
loader interface, and Fabric — which has neither — was left with `shear(...)` bodies that nothing
in the game ever called.

**Fix.** Give the four mobs the call site vanilla's own shearable mobs have, gated `//? if fabric`
so no other loader's path changes. One helper, `AMCompat#shearWithShears`, because
`Shearable#shear` is era-split at 1.21.2 (the `!mc2102-shear-decl` rule at
`stonecutter.gradle.kts:944` rewrites the four *declarations*; this is now their only call):

```java
public static <T extends Mob & Shearable> boolean shearWithShears(T mob, Player player, InteractionHand hand, ItemStack stack) {
    if (!stack.is(Tags.Items.SHEARS) || !mob.readyForShearing()) return false;
    if (mob.level().isClientSide()) return true;          // caller hands back a sided success
    mob.shear(/* >=1.21.2: (ServerLevel) mob.level(), */ SoundSource.PLAYERS /*, stack */);
    hurtAndBreak(stack, 1, player, hand);
    return true;
}
```

- The shears test is `stack.is(Tags.Items.SHEARS)`, the same `TagKey` seven other mobs in this mod
  already use — so modded shears work and the Fabric shim in `fabric/common/Tags.java` maps it to
  `c:shear_tools` for free. Not `is(Items.SHEARS)`: that overload was removed in 26.1.
- `hurtAndBreak` skips creative internally on every version, so no `isCreative()` guard.
- The four `shear(...)` bodies were already complete equivalents of their `onSheared(...)` twins
  (the bison and turtle spawn their drops directly; the cockroach and mungus have none by design),
  so nothing had to be re-implemented — only called.
- **The cockroach's branch must come first**, ahead of its `else if (held item is not a maraca &&
  hasMaracas())` arm, which would otherwise answer the shears by dropping the roach's maracas.
- The turtle had **no `mobInteract` at all**, so its whole override is new and Fabric-only.

The false comment is corrected in all four files, with the correction spelled out rather than
deleted.

⚠️ **Generalisation — second of #93's shape, and the more dangerous half.** #93 was a Forge-family
extension method with **no Fabric caller**; this is a Forge-family extension *interface* that the
port deliberately dropped on Fabric while believing vanilla covered it. Both compile, both carry
legal `@Override`s against the shim, both are invisible to `verify_overrides.py` (which sees a real
override of a real supertype method), and no gate can see either. The tell is available and cheap:
**when a gate drops a loader interface, name the thing that is supposed to call the method
instead, and check it exists in the vanilla jar.** Here one `javap` on `ShearsItem` would have
settled it in 2026-07.

### The snow — NOT A BUG, it is upstream's own bison

`EntityBison#breakBlock()`, called every tick from `customServerAiStep()`, destroys any
`Blocks.SNOW` with `LAYERS <= 1` inside the bison's bounding box, slows it to 0.6× horizontally
while it does, and sets a 20-tick cooldown (2 while charging). Diffed against the pristine baseline
`151e36c`: **identical**, statement for statement, call site included — the only difference is the
port routing Forge's `ForgeEventFactory.getMobGriefingEvent` through `AMPlatform.mobGriefing`.

So the square of cleared snow in the screenshot is a bison ploughing through the snow layer it
walked across, which is exactly what it is designed to do. It respects `mobGriefing`, so
`/gamerule mobGriefing false` turns it off. There is no config option for it upstream or here, and
adding one is a feature request, not a fix.

---

## Twenty-ninth pass (2026-08-17) — one report, against published `2.1.2` — a crash on startup

> *"When I try to launch it it says*
> *Alex's Mobs Continued (alexsmobs) failed to load correctly.*
> *`java.lang.NoClassDefFoundError: net/neoforged/neoforge/event/entity/player/PlayerInteractEvent$EntityInteractSpecific`*
> *I've never gotten an error message like this and I don't even know where to begin fixing it. My
> only guess is maybe it's something to do with the neoforge version, I had it at 26.2.21 and things
> worked fine (I think), then I wanted to install JEI and it went "you need an updated version" so I
> updated it and then the problems started."*

The guess is right, and it is the most severe report this port has had: the mod does not start at
all. **The reporter diagnosed it themselves** — the version they were on worked, the version they
moved to does not, and nothing else changed.

### #102 — `2.1.0`–`2.1.2` cannot load on NeoForge ≥ `26.2.0.43-beta` — FIXED, 1 node

**Scope.** `26.2-neoforge`, 1 of 49 nodes, in `2.1.0`, `2.1.1` and `2.1.2`. `2.0.x` is unaffected —
the handler that references the deleted class was added by **#98** (the MCA Reborn interaction fix),
which first shipped in `2.1.0`. Every other node and every other loader is fine.

**Cause.** NeoForge **merged the two interaction events into one** in `26.2.0.43-beta`:

> *[26.2] Combine PlayerEvent.EntityInteract and PlayerEvent.EntityInteractSpecific into one event
> (#3339)*

Read out of the universal jars rather than the changelog alone:

| NeoForge build | `PlayerInteractEvent$EntityInteract` | `…$EntityInteractSpecific` |
|---|---|---|
| `26.1.2.87` (our 26.1.2 pin) | present | present |
| `26.1.2.95` (newest 26.1.2) | present | present |
| `26.2.0.35-beta` (**our 26.2 pin**) | present | present |
| `26.2.0.43-beta` and every build after, incl. stables `.57`/`.58`/`.59` | present | **gone** |

`CommonHooks` collapsed with it: `.35` had `onInteractEntityAt(Player,Entity,HitResult,Hand)`,
`onInteractEntityAt(Player,Entity,Vec3,Hand)` and `onInteractEntity(Player,Entity,Hand)`; `.59` has
only `onInteractEntity(Player,Entity,Hand,Vec3)`.

So the jar compiles here (we pin `.35`), ships, and dies on the player's machine the moment NeoForge
registers the listeners — **a handler's parameter type is resolved at registration**, so a deleted
event class is a hard `NoClassDefFoundError` and the whole mod fails to load. There is nothing soft
about it and no way to catch it; the mod's own `neoforge.mods.toml` declares `neoforge` versionRange
`[1,)`, so the jar happily loads onto a build it cannot run on.

**Why the mod loses nothing by dropping the handler.** 26.2 vanilla merged the phases *itself*:
`Entity#interactAt` is deleted, there is one `Entity#interact(Player, InteractionHand, Vec3)`, and
`Player#interactOn(Entity, InteractionHand, Vec3)` calls the NeoForge hook **before** it. That is
exactly the ordering #98 needed. One `EntityInteract` handler is therefore complete on 26.2, and the
MCA Reborn fix survives untouched.

**The fix.** Gate the second handler to only the loaders and versions where two phases exist:

```java
//? if forge && >=1.21.6 && <26 {
/*@SubscribeEvent
public boolean onInteractWithEntitySpecific(PlayerInteractEvent.EntityInteractSpecific event) { … }
*///?}

//? if (forge && <1.21.6) || (neoforge && <26.2) {
@SubscribeEvent
public void onInteractWithEntitySpecific(PlayerInteractEvent.EntityInteractSpecific event) { … }
//?}
```

Two **sibling** blocks, not one `if`/`else` — Stonecutter blocks never nest, and the two arms differ
along the return-type axis as well as the loader one. The `@SubscribeEvent` moves **inside** each
arm; it had been sitting above the gate, where on the excluded nodes it would dangle.

Three things fall out of it:

- **Fabric** loses a handler that was never called. `FabricServerEvents` fires
  `HANDLER.onInteractWithEntity` from `UseEntityCallback` and nothing anywhere fires the Specific
  stub — the shim class's own javadoc says so (*"Never fired on Fabric, deliberately"*), and a grep
  for `onInteractWithEntity` outside `ServerEvents.java` returns exactly that one call.
- **Forge ≥26** loses an accidental **duplicate registration**, never reported: Forge 64 deleted
  `EntityInteract` and kept `EntityInteractSpecific` — the mirror image of NeoForge's choice — so
  the `!fg26-entityinteract` rename rule was pointing *both* handlers at the same event class.
  Harmless in practice (the body always cancels, and a listener's default `receiveCanceled=false`
  skips the second) but it is gone now.
- **NeoForge <26.2** and **Forge <26** are unchanged, which is 30 of the 32 Forge/NeoForge nodes.

⚠️ **Generalisation, two of them.**

1. **A loader can delete a public event class in a patch release**, and a handler's parameter type is
   loaded when the listener is registered — so it is a startup crash for every player on that build,
   not a silent no-op like most of this port's faults. Nothing in this repo can see it: the compiler
   compiles against the pinned build, all four verifiers pass, and the manifest range says the jar is
   fine. **The pin is a claim about one build, and players run the newest one.** When a loader
   publishes a beta line under an MC version we ship, check the newest build's API against the pin
   before release, not just the pin.
2. **When two loaders consolidate the same pair of events, they can keep opposite survivors.** Forge
   26 kept `EntityInteractSpecific` and deleted `EntityInteract`; NeoForge 26.2 kept `EntityInteract`
   and deleted `EntityInteractSpecific`. A rename rule written for one is exactly backwards for the
   other, and this tree now carries both directions within the same MC version.

### Verification for the twenty-ninth pass

**Compile.** One invocation, 9 era-spanning nodes covering both arms of the gate on all three
loaders — `1.20.1-forge` (void arm), `1.21.8-forge` (boolean arm), `26.1.2-forge`, `26.2-forge`
(rename rule), `1.21.11-neoforge`, `26.1.2-neoforge` (void arm), `26.2-neoforge` (excluded),
`1.20.1-fabric`, `1.21.11-fabric` — `BUILD SUCCESSFUL in 1m 1s`, exit 0.

**What actually compiled**, read out of each node's `ServerEvents.class` with `javap -p` rather than
out of the projected source (the commented-out arms are still *in* the projection, so a grep there
proves nothing):

| Node | handlers in the class file |
|---|---|
| `1.20.1-forge` | `void …EntityInteract`, `void …EntityInteractSpecific` |
| `1.21.8-forge` | `boolean …EntityInteract`, `boolean …EntityInteractSpecific` |
| `26.1.2-forge`, `26.2-forge` | `boolean …EntityInteractSpecific` **only** (was two — the duplicate is gone) |
| `1.21.11-neoforge`, `26.1.2-neoforge` | `void …EntityInteract`, `void …EntityInteractSpecific` |
| **`26.2-neoforge`** | `void …EntityInteract` **only** |
| `1.20.1-fabric`, `1.21.11-fabric` | `void …EntityInteract` only (shim types) |

**The proof, and the reusable check.** Diff every loader class the build *references* against the
class list of the **newest** loader build, not the pinned one:

```bash
unzip -l nf-26.2.0.59.jar | grep -oE 'net/neoforged/[A-Za-z0-9/$_]+\.class' | sed 's/\.class$//' \
  | sort -u > nf59.classes
grep -aohE 'net/neoforged/neoforge/[A-Za-z0-9/$_]+' -r versions/26.2-neoforge/build/classes \
  | sed 's/[;<].*//' | sort -u > used.txt
comm -23 used.txt nf59.classes
```

- **shipped `2.1.2` jar** → exactly **one** line: `PlayerInteractEvent$EntityInteractSpecific`.
  A one-line positive control that reproduces the player's crash from a jar and two zip listings.
- **fixed build** → **empty**, out of 119 NeoForge classes referenced.

⚠️ Write the character class **single-quoted** and use `grep -E`. `"[A-Za-z0-9/$_]"` inside double
quotes expands `$_` as a shell variable, which silently drops `$` from the pattern — every nested
class then compares truncated, on both sides, and the diff comes back clean for the wrong reason.
That happened here and briefly hid the very class being looked for.

Same check on the other three loader/version pairs in the 26.x band, all **empty**:

- `26.2-forge` build vs **forge `26.2-65.1.1`** (newest; we pin `65.1.0`) — the only misses are
  `fml`/`eventbus`/`api.distmarker`, which ship in separate artifacts, not the universal jar.
- `26.1.2-neoforge` build vs **`26.1.2.95`** (newest; we pin `.87`).

**Not verified in a client**, and it does not need one: the failure is a class-load error at listener
registration, so the class file's method list *is* the result. What a client would add is the #98
behaviour on 26.2-NeoForge (lasso release, chorus-fruit cure, sponge) now that only one handler
carries it — worth a look next time a NeoForge node is booted.

### `2.1.3` release record (2026-08-17)

All **49** nodes on both stores. Build `BUILD SUCCESSFUL in 10m 43s` with `MOD_IS_RELEASE=true`,
147 jars, zero `-SNAPSHOT`, zero non-`2.1.3`. Verifiers: `jars=49 problems=0`, `selectors=1070
problems=0` (unchanged from `2.1.2` — no mixin added), `literals=394 missing=0`, `problems=9`.

Both pilots ran on `26.2-neoforge`, the node the fix is for: Modrinth `nV2N18uk`, CurseForge file
`8671232`. Batches `uploaded=48 skipped=1 failed=0` each; Modrinth at **1073** versions, 49 ×
`2.1.3` split 17/16/16, CurseForge ledger 14 × 49 = 686 with 49 unique file ids.

⚠️ One damaged Modrinth version behind `failed=0`, the fourth release running: `2.1.3+1.20.1-fabric`
had empty `dependencies` with everything else correct. Fresh single-version re-read confirmed it was
real, one `PATCH` restored both (Fabric API `xhLT3C5f`, CodxLib project-level), fresh re-read
confirmed the repair.

---

## #103 — "biomes generate as all grass with BOP / BYG / Regions Unexplored" — NOT this mod

**Reported 2026-08-18**, Fabric `1.21.1`, reporter said "Alex's mobs port 2.1.3" (their own modlist
says **2.1.1**), Fabric loader `0.19.3`, modlist <https://mclo.gs/Po7knPB> (470 mods). Symptom: modded
biomes — "specifically biomes like beaches and deserts … with gravel or sand as the main ground
coverage" — generate as bare grass-block-over-dirt. Screenshot was a Regions Unexplored saguaro
desert rendered as grassland. They had found a Reddit thread pinning it on "Alex's Mobs/Citadel" and
noted, correctly, that there is no Citadel in this port.

**Verdict: not this mod, and the mod cannot cause it.** The real conflict is
**TerraBlender × Lithostitched**, reproduced here with a controlled A/B in which Alex's Mobs was
loaded in *both* arms.

### The repro (headless, `1.21.1-fabric`, same seed 1234, fresh world each arm)

`versions/1.21.1-fabric/run/mods/` + the dev classpath (AMC is always on it, so both arms are
"AMC present"). Surface census over RCON: `locate biome <id>`, forceload, then a 13×13 grid of
columns at 4-block spacing, each column filtered by `execute positioned <x> 320 <z> positioned over
ocean_floor run execute if biome ~ ~-1 ~ <id>` and then identified with `… if block ~ ~-1 ~
minecraft:<candidate>`. Script kept at `/tmp/amc-biome/census.py`.

| biome | **arm A**: TerraBlender + GlitchCore + BOP + Lithostitched + RU | **arm B**: Lithostitched + RU only |
|---|---|---|
| `minecraft:desert` | sand 62 / 80 ✅ | sand 59 / 84 ✅ |
| `minecraft:beach` | sand 41, gravel 27 ✅ | sand 49, gravel 28 ✅ |
| `biomesoplenty:dune_beach` | gravel 35, sand 33 ✅ | (BOP absent) |
| `biomesoplenty:lush_desert` | BOP-own blocks ✅ | (BOP absent) |
| **`regions_unexplored:gravel_beach`** | **grass_block 30**, dirt 10, sand 9, gravel 9 ❌ | **gravel 65 / 65** ✅ |
| `regions_unexplored:outback` | — | red_sand 43, terracotta 3 ✅ |

TerraBlender's *own* clients (BOP) are fine in arm A. It is specifically the biomes whose surface
rules arrive through the **other** API that collapse, and they are perfect the moment TerraBlender
is removed. Alex's Mobs is the constant.

### The mechanism (read out of the jars, TerraBlender 4.1.0.8 / Lithostitched 1.8.0+beta5)

`terrablender.mixin.MixinNoiseGeneratorSettings`:

```java
private RuleSource namespacedSurfaceRuleSource;          // built once, never invalidated
private void surfaceRule(CallbackInfoReturnable<RuleSource> cir) {
    if (ruleCategory != null) {
        if (namespacedSurfaceRuleSource == null)
            namespacedSurfaceRuleSource = SurfaceRuleManager.getNamespacedRules(ruleCategory, comp_478);
        cir.setReturnValue(namespacedSurfaceRuleSource);
    }
}
```

`ruleCategory` is set from `LevelUtils.initializeBiomes`, off `MixinMinecraftServer#onInit`.
`getNamespacedRules(category, vanillaRule)` returns a `NamespacedSurfaceRuleSource(vanillaRule,
{"minecraft" -> …} + registered rules)`, whose `NamespacedRule.tryApply` dispatches **by the biome's
namespace** and falls back to `baseRule` for any namespace with no registered rules.

`dev.worldgen.lithostitched.worldgen.surface.SurfaceRuleManager.applySurfaceRules` reads
`NoiseGeneratorSettings.comp_478()` and writes the merged result back through
`NoiseGeneratorSettingsAccessor.setSurfaceRule(…)` — i.e. it **mutates the field**.

So the outcome is pure ordering:

- Lithostitched writes **first** → TerraBlender later builds its namespaced source with
  `base` = the merged rule → `regions_unexplored:*` falls through to the merge → correct.
- TerraBlender caches **first** → its `base` is the pristine vanilla rule, and every later write to
  `comp_478` is dead, because nothing ever clears `namespacedSurfaceRuleSource`.
  `regions_unexplored:*` has no TerraBlender rules, so it falls through to vanilla's default
  clause — **grass block over dirt**. Which is the report, verbatim.

It shows up worst on deserts and beaches for the obvious reason: those are the biomes where the
default clause is furthest from what the biome should be. A modded forest looks fine by accident.

### Why it cannot be this mod

- No `SurfaceRules` / `NoiseGeneratorSettings` / `BiomeSource` / `SurfaceSystem` reference anywhere
  in `src/main/java`. The only two `getChunkSource().getGenerator()` uses (`AMWorldData:232`,
  `EntityMungus:341`) are read-only.
- `alexsmobs.mixins.json` declares no worldgen mixin on any node.
- The Fabric biome hook, `fabric/world/FabricBiomeModifications`, runs in
  `ModificationPhase.ADDITIONS` and does exactly two things: `addSpawn` per recorded spawn, and —
  behind `AMConfig.leafcutterAnthillSpawnChance > 0` — one `addFeature(SURFACE_STRUCTURES,
  alexsmobs:leafcutter_anthill)`. There is no path from either to a surface block.
- Citadel is vendored and relocated into `alexsmobs/citadel/`; there is no Citadel mod on Fabric at
  all, so the Reddit thread's suspect does not exist in this port.

### What to tell a reporter with this symptom

Their pack has **four** independent surface-rule systems fighting over one record field:
TerraBlender (BOP, BYG), Lithostitched (Regions Unexplored — it depends on `lithostitched >=1.7.9`,
not TerraBlender), Biolith (Nature's Spirit), and WorldWeaver's `wover-surface`, whose
`NoiseGeneratorSettingsMixin` writes the same field and logs **"Overwriting an overwritten set of
Surface Rules."** when it is the second writer. That log line, in `latest.log`, is a direct readout
of the collision. The fix is upstream in those mods; the workaround is to drop one of the two
ecosystems, and the bug belongs on Lithostitched's or TerraBlender's tracker.

⚠️ **Generalisation, and it is the reusable half of this pass:** *a mod that caches a value derived
from a mutable field, and never invalidates the cache, silently voids every later writer of that
field.* Both mods here are individually correct — one reads-modifies-writes, one memoises — and the
combination loses data with no exception, no log line and no failed mixin. When several mods write
one vanilla record field, the one that memoises wins, and which one that is depends on load order,
which is why the symptom looks random across packs.

⚠️ **Rig lessons.** `positioned over world_surface` puts you on top of *water* (that heightmap counts
fluids) — use `ocean_floor` and read `~-1`, since `positioned over` lands one **above** the surface
block. And `locate biome` hands back a point that is frequently at the biome's edge or underwater, so
a single-point probe is worthless: verify the biome per column and census a grid.

---

## Thirty-first pass (2026-08-19 → 08-20) — six reports, against published `2.1.3` / `2.1.4`

Six reports from five reporters. **Three fixed faults that had shipped since `2.0.0`** (#104, #105,
#108), **one fixed that arrived with `2.1.0`'s own event work** (#106), **one found while chasing a
report that is itself still blocked** (#107 — the largest of the five and a strong candidate root
cause for the long-open **#49**), and one **NOT REPRODUCIBLE**.

⚠️ **One of the two verdicts this pass first closed as NOT A BUG was wrong.** The straddleboard row
was closed on an asset diff — every file byte-identical to the baseline — without asking what draws
the grey panel. It is a *tint* layer, and it had been drawing at alpha 0 on 14 nodes since `2.0.0`
(**#108**). An asset diff answers "did the port change this file", never "does this file reach the
screen".

| Item | Report | Verdict | Nodes |
|---|---|---|---|
| **#108** | straddleboard's grey part disappears against a gray background | **FIXED** — first closed as NOT A BUG; the panel was drawing at alpha 0. Plus the contrast setting the user asked for | 14 (1.20.6→1.21.3); the setting is 49 |
| **#106** | earthquake effect does not shake the first-person camera | **FIXED** | 14 (NeoForge ≥1.21) |
| — | "crashes when opening the creative menu", `2.1.4` / `1.21.11` / fabric | **BLOCKED on the crash log** — twelve hypotheses eliminated | ? |
| — | mantis shrimp "mesmerized", sinks in place watching a fish | **NOT REPRODUCIBLE** | — |
| **#104** | mob inventories cleared on reload / chunk reload; mosquitoes drop no blood sacs | **FIXED** (two independent faults) | 20 + 24 |
| **#105** | cannot equip the straddlite saddle on a laviathan; leads do not work | **FIXED** | 49 (incl. upstream) |
| **#107** | *(nobody reported it — found while chasing the creative-menu crash)* GUI item atlas thrashed every frame | **FIXED** | 24 (≥1.21.6), 18 of them severe |

---

### #104 — "the mob's inventory gets cleared when reloading" + "mosquitoes never drop blood sacs"

> *"The mob's inventory gets cleared when reloading the game or when the chunk reloads, elephant and
> kangaroo inventory gets cleared… Minecraft version 1.21.1, NeoForge, and the mod version is 2.1.3"*
>
> *"Installed this mod on my Aternos server, Mosquitoes havent dropped blood sacs since i added the
> mod, im only getting the proboscus as a drop… Im playing on fabric 26.1.2"*

Two reporters, two symptoms, **two unrelated faults** — and they only look like one pass because they
were fixed together.

#### (a) `AMCompat.saveInto` discarded the value it was supposed to keep — 20 nodes

`ItemStack#save(HolderLookup.Provider, Tag)` **does not write into the tag it is handed.** It encodes
through `ItemStack.CODEC` with that tag as the codec's *prefix*, and `NbtOps#mergeToMap`
**`shallowCopy()`s a `CompoundTag` prefix and returns the copy** — read out of 1.21.1's `NbtOps`.
Vanilla's own `AbstractChestedHorse#addAdditionalSaveData` adds the **return value** to its list for
exactly that reason.

The port's `1.20.5..1.21.5` arm called it for its side effect and threw the result away:

```java
//?} elif >=1.20.5 {
/*stack.save(provider, tag);      // <- the encoded stack goes into a copy nobody keeps
*///?} else {
```

so every "one `CompoundTag` per inventory slot" tag came back holding **nothing but its `Slot`
byte**. The kangaroo's and elephant's inventories, the catfish's swallowed items and the
straddleboard's own `BoardStack` all saved empty on the 20 nodes in that band (1.20.6, 1.21, 1.21.1,
1.21.3, 1.21.4, 1.21.5 × three loaders, plus 1.21.2 × two), **since `2.0.0`**. The `>=1.21.6` arm was
already correct — it merges an unprefixed encode back into the caller's tag — and the `<1.20.5` arm
uses the old `save(CompoundTag)`, which really does write in place. Only the middle band was wrong,
which is why the reporter's `1.21.1` is in it and a 26.2 player would never have seen it.

Fixed by making the middle arm do what the top arm does:

```java
/*tag.merge((CompoundTag) stack.save(provider, new CompoundTag()));
```

#### (b) `getLootTable()` was deleted on nine entities on the strength of a comment — 24 nodes

1.21.2 moved the default loot table onto `EntityType` and replaced the overridable
`Mob#getDefaultLootTable()` with `Entity/Mob#getLootTable()` returning
`Optional<ResourceKey<LootTable>>`. The port dropped the override from nine entities with a comment
saying it could not be overridden any more. **It can** — the method is not `final` on 1.21.2, 1.21.3,
26.1.2 or 26.2 (read out of the sources), and `LivingEntity#dropFromLootTable` calls it virtually.

Nine entities pick their loot table per state — `EntityCrimsonMosquito` is the reported one (a
**bloated** mosquito rolls the blood-sac table, a starved one only the proboscis), and
`EntityCatfish`, `EntityCockroach`, `EntityFroststalker`, `EntityGuster`, `EntityLaviathan`,
`EntityLeafcutterAnt`, `EntitySoulVulture` and `EntityVoidWorm` do the same thing for their own
variants. **14 conditional loot tables** were silently unreachable on every node ≥1.21.2.

Restored through a new `AMCompat.lootOpt(ResourceLocation)`, gated **`(>=1.21.2 && !forge)`**:

⚠️ **An access transformer is applied by NeoForge's MDG and by Fabric's accesswidener, but NOT by
arch-loom on classic Forge.** The override needs `getLootTable` widened; NeoForge and Fabric get that
from the AT/AW this mod already ships, and classic Forge gets neither at compile time — **nor at
runtime**, because the shipped Forge manifest declares `accessTransformers = [  ]` (the empty-array
defect already recorded in the porting notes, still deliberately unfixed). So Forge ≥1.21.3 and
`26.1.2`/`26.2-forge` — **11 nodes** — keep the documented default-table degrade, with a comment-only
`//?} elif >=1.21.2 {` arm at each of the nine sites saying so. A widening that unlocks an override
silently covers **two of three loaders**; check which before costing a fix at "35 nodes".

---

### #105 — "I'm unable to equip the straddlite saddle on laviathans. The leads also do not work."

> *"it lets me equip to tack but no amount of crouch spamming, normal spamming right click will let
> me equip the saddle. The leads also do not work."* — fabric 26.1.2

`EntityLaviathanPart` declared:

```java
public InteractionResult getEntityInteractionResult(Player player, InteractionHand hand) {
```

**`getEntityInteractionResult` has not existed on `Entity` or `PartEntity` since the MCP era.** It
overrode nothing, nothing called it, and every right-click on a laviathan's head, neck or seat parts
fell straight through `Entity#interact`'s `PASS` — on **every** node, every loader, and in **upstream
Alex's Mobs itself**. The laviathan is almost entirely parts, so the reporter could reach the body
for the tack (one hitbox that is the real entity) and nothing else: no saddle, no lead, no shear, no
feed.

`EntityGiantSquidPart` and `EntityCachalotPart` carry the correct shape. The fix is now identical to
theirs, **including the client-side `MessageInteractMultipart` send** — which is what makes it work on
Fabric, where a part's id does not resolve server-side (see `FabricLevelMultipartMixin`).

⚠️ **Side finding, not fixed here:** `EntityAnacondaPart:79`, `EntityBoneSerpentPart:212` and
`EntityVoidWormPart:426` have the right method name but **lack that Fabric send**, so their
interactions are Forge/NeoForge-only. Nobody has reported it.

---

### #106 — the earthquake effect does not shake the camera in first person

> *"trying to apply the earthquake status effect does not actually render the effect for the player
> in first person / mod version: 2.1.4 / game version: 1.21.1 / mod loader: neoforge"*

The shake is applied from `ViewportEvent.ComputeCameraAngles` by calling `Camera#move(...)`. From
**NeoForge 21.0 (MC 1.21)** onward that event is posted **inside `Camera#setup`** (on 26.x, inside
`alignWithEntity(float)`), immediately **before** `setPosition(Mth.lerp(...))` — which overwrites the
camera position outright. Rotation nudges survive; positional ones are discarded. Classic Forge and
Fabric post it after, so they were fine, and NeoForge `1.20.4`/`1.20.6` are below the change.
**14 nodes** (NeoForge ≥1.21).

Fixed with the tree's second `Camera` seam: `mixin/client/CameraMixin`, three sibling era arms
pinned by descriptor rather than by version guesswork —

| Arm | Target |
|---|---|
| `neoforge && >=1.21 && <1.21.11` | `setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V` |
| `neoforge && >=1.21.11 && <26` | `setup(Lnet/minecraft/world/level/Level;…)V` |
| `neoforge && >=26` | `alignWithEntity(F)V` |

each injecting at `TAIL` and calling the shake body, which was extracted out of the event handler into
`ClientEvents.applyEarthquakeShake(Camera)`. The event handler itself is now gated
`//? if !(neoforge && >=1.21) {`, so no node runs both.

⚠️ **A client session is owed** — this is a visual fix on a loader/version the CI cannot see.

---

### #107 — the GUI item atlas is thrashed every frame by this mod's six live icons

**Nobody reported this.** It was found while eliminating hypotheses for the creative-menu crash below,
and it is the largest item in the pass.

#### The false comment that hid it

`AMIconSpecialRenderer`'s own javadoc had said, since #45/#48 shipped it in `2.0.9`:

> *Animation works without any cache-busting on 1.21.6+ because `SpecialModelWrapper.update`
> unconditionally calls `ItemStackRenderState.setAnimated()` — bytecode-verified on every era through
> 26.2 — which exempts the stack from the GuiItemAtlas cache.*

**That is false on every era.** `javap` on `SpecialModelWrapper.update` from **1.21.6, 1.21.8, 1.21.9,
1.21.11 and 26.2** gives a byte-for-byte identical shape, and the call is inside a foil branch:

```
 0: appendModelIdentityElement(this)
12: ItemStack.hasFoil():Z
15: ifeq 40                              <-- skips the block when there is no foil
..: setFoilType(STANDARD)
31: ItemStackRenderState.setAnimated()   <-- ONLY reachable for foil items
37: appendModelIdentityElement(FoilType.STANDARD)
45: SpecialModelRenderer.extractArgument(stack)
..: setExtents / setupSpecialModel
80: appendModelIdentityElement(arg)      <-- unconditional; OUR value lands here
```

None of the six `LIVE_ICON_ITEMS` is enchanted, so `setAnimated()` was **never** called for them.

#### What actually happened

The value `extractArgument` returns is appended to the render state's **model identity**, and from
**1.21.6** that identity is the **GUI item atlas key**. `AMIconSpecialRenderer.extractArgument`
returned `stack.copy()`, and `ItemStack` does not override `equals`/`hashCode` — it has identity
semantics. So every icon took a **fresh atlas key, and therefore a fresh atlas slot, every single
frame.**

The two atlas generations degrade differently, and the older one is the bad one:

- **1.21.6 → 1.21.11 (18 nodes) — severe.** `GuiRenderer.atlasPositions` is a `Map<Object,
  AtlasPosition>` that is **never evicted per key**; the only removal is `invalidateItemAtlas()`,
  which happens when the atlas fills, and which `close()`s the GPU textures and forces a **full
  recreate plus a re-render of every GUI item in the frame — vanilla's included**. A screen showing
  six live icons therefore drives the whole atlas through destroy-and-rebuild cycles.
- **26.1.2 / 26.2 (6 nodes) — bounded.** The atlas moved to `GuiItemAtlas` +
  `DynamicAtlasAllocator`, whose `endFrame()`/`reclaimSpaceFor(Set)` reclaim slots whose keys are no
  longer needed. The cost is allocator churn and possible resizes rather than unbounded growth.

**24 nodes, all loaders, since `2.0.9`.** Below 1.21.6 there is no atlas at all — items re-render
every frame and model identity is irrelevant — so 1.21.4/1.21.5 were never affected.

This is a strong candidate root cause for open **#49** (*"advancement screen at 25–30 FPS on 26.2"*),
which draws **59** of these icons at once.

#### Why the fix has to be two parts

The churn was also the **only** reason the icons animated. A fresh key each frame meant a guaranteed
cache miss, i.e. a redraw. Fixing the key alone would have frozen all 59 advancement icons and the
cycling creative tab — regressing #45/#48 — so the fix restores animation the supported way at the
same time:

1. **A canonical argument.** `extractArgument` now returns **one** `ItemStack` per distinct
   `(item id, custom_data)` out of a `ConcurrentHashMap`, so the identity is stable frame to frame.
   (It still must not hand back the *caller's* stack — the render state would alias a live one.) A
   512-entry cap clears the map defensively; the icons' `custom_data` space is a few dozen entries.
2. **`mixin/client/ItemStackRenderStateAtlasMixin`.** `@Inject` at `HEAD` of
   `appendModelIdentityElement(Ljava/lang/Object;)V`; if the appended element is one of those
   canonical arguments (identity test through an `IdentityHashMap`-backed set), call `setAnimated()`.
   That is exactly the flag vanilla's own animated items set, and it routes the icons onto the
   supported path: `animatedInPlace = isAnimated() && p != null` → redraw in place on 1.21.6–1.21.11;
   `discardAfterFrame` → free-and-redraw-next-frame on 26.x.

`setAnimated()`, `isAnimated()` and `appendModelIdentityElement` are all **public and
descriptor-identical** on 1.21.6, 1.21.8, 1.21.9, 1.21.11 and 26.2, so the mixin needs **no era arms**.
It uses a cast rather than `@Shadow` on purpose, so the verifier's selector delta stays exactly **+1
per node** and the prediction stays sharp.

Build wiring, following `GuiRendererMixin`'s existing pattern in `ModPlatformPlugin.kt`: excluded from
the compile below 1.21.6 (`sourceSets.main.java.exclude`) **and** pruned back out of
`alexsmobs.mixins.json` in `processResources` (`DataPackMigration.pruneMixinEntries`) — a config
naming an absent class is a hard load failure.

#### Generalisations

⚠️ **A comment asserting how a vanilla or loader hook reaches your code is not evidence; the bytecode
is.** Second instance of this exact shape after **#101**, where a comment claiming
`ShearsItem#interactLivingEntity` was vanilla cost 17 nodes. Here the comment even said
*"bytecode-verified on every era"* — and was wrong on all five eras checked.

⚠️ **A value handed to a framework that uses it as a cache key must have value equality.** Vanilla's
own special renderers return records and enums from `extractArgument`. Returning a fresh `ItemStack`
turns a cache into a per-frame allocator, and nothing warns: no exception, no log line, only frame
time.

⚠️ **A feature that works "by accident" is a defect in waiting.** The icons animated *because* they
missed the cache. Any correct fix to the churn would have broken the animation, and a fix that only
addressed the churn would have shipped a silent regression of two closed reports.

#### Verification

- `compileJava` on **8 era-spanning nodes** in one invocation — `1.20.1-forge`, `1.21.1-neoforge`,
  `1.21.5-neoforge`, `1.21.6-fabric`, `1.21.8-forge`, `1.21.11-fabric`, `26.2-fabric`,
  `26.2-neoforge` — `BUILD SUCCESSFUL`.
- `processResources` on 5 nodes with `build/resources` cleared first: `GRADLE_EXIT=0`, and
  `"Pruned 2 pre-1.21.6 GUI mixins"` fired on **exactly** the two sub-1.21.6 nodes.
- Per-node membership: the atlas mixin is in `alexsmobs.mixins.json` **and** has a compiled `.class`
  on `1.21.6-fabric`, `1.21.11-fabric` and `26.2-neoforge`; absent from **both** on `1.20.1-forge`
  and `1.21.5-neoforge`.
- `MOD_IS_RELEASE=true` build of 5 nodes → `GRADLE_EXIT=0`; `verify_mixin_targets.py --node ×5` →
  **`nodes=5 jars=5 selectors=131 problems=0 skipped=0`**; `verify_mixins.py` `problems=0` with
  `declared` up by exactly 1 on the ≥1.21.6 nodes.
- Remap proof: the shipped `alexsmobs-2.1.5-fabric+1.21.11.jar`'s copy of the mixin carries the
  constant-pool selector **`method_70946`** — loom resolved `appendModelIdentityElement` to a real
  intermediary name and the verifier resolved it back against the intermediary jar.

⚠️ **A ≥1.21.6 client session is owed**: open the creative tab and the advancement screen, confirm the
icons still cycle, and confirm the screen does not stutter.

⚠️ The first verifier run reported `jars=8` for 5 nodes — stale `-SNAPSHOT` jars from an earlier
non-release compile sitting beside the fresh release-named ones. **A count *above* the prediction
means the verifier is reading something you did not build.** Moving them out gave the clean `jars=5`,
and the mixed run left a free proof behind: `1.21.8-forge` read `declared=19` from the old jar and
`declared=20` from the new one.

---

### "Game crashes when opening creative menu" — BLOCKED on the crash log

> *"Alex's Mobs Continued: Game crashes when opening creative menu in Alex's Mobs / 2.1.4 for the
> version / 1.21.11 for minecraft / loader is fabric"*

No crash report, no `latest.log`, no stack trace — `issues.md` carries the sentence and nothing else.
`1.21.11-fabric` is one of the 18 nodes #107 hits hardest, and the creative tab is one of the two
screens that draws live icons, so **#107 is the leading suspect** — but it is a performance fault, not
a crash, and its exhaustion path is provably graceful on both atlas generations, so it cannot be
called the answer without the trace.

Twelve hypotheses eliminated structurally:

1. `getExtents` signature mismatch across the special-renderer eras — descriptors checked, fine.
2. `AMGuiEntityPipPool.substitute` `ClassCastException` — the pool is `//? if !neoforge` and its
   redirect keys on the one stable `Map.get`; unrelated to items.
3. Creative-tab duplicate-stack `IllegalStateException` — no duplicate registrations.
4. `AMSubmitBuffers` `VertexConsumer` completeness — all methods implemented.
5. Unit-cube extents making a PiP oversized in the GUI — extents are set from the model.
6. `extractArgument` returning `null` — it cannot; empty stacks return the stack.
7. `SpecialModelRenderer.submit` signature drift on 1.21.11 — checked, matches the `>=26` boundary
   already handled by `ItemStackRenderStateMixin`.
8. Custom geometry dropped in the GUI — the icons render through `renderByItem`, verified in-client
   on all four rendering eras in the `2.0.9` campaign.
9. `ConcurrentModificationException` from nested submits — the icon render can draw another item, and
   the new canonical map is a `ConcurrentHashMap` for that reason; no vanilla collection is re-entered.
10. Infinite recursion in the carver branch — the shattered carver draws shard models, not itself.
11. `GuiRendererMixin`'s `@Redirect` ambiguity — single `Map.get` call site, verified.
12. **Atlas exhaustion throwing** — decisive: it does **not** throw. 1.21.6→1.21.11 logs
    `"Trying to render too many items in GUI at the same time. Skipping some of them."` and returns;
    26.x's `GuiItemAtlas.getOrUpdate` returns `null`. `invalidateItemAtlas()` `close()`s its GPU
    textures properly, so there is no leak either.

**What to ask for:** the crash report (`crash-reports/crash-*.txt`) or `latest.log`, and the full mod
list. If the "crash" is in fact a freeze or a hang, #107's fix is the thing to test first.

---

### Mantis shrimp "mesmerized" — NOT REPRODUCIBLE

> *"has anyone else seen some odd behavior coming from the mantis shrimp? Out of water they function
> well but I have seen them kinda get… mesmerized in a sense. They just slowly sink in place the
> moment they lock on them and just watches the fish."*

`EntityMantisShrimp`'s hunting chain was read end to end and diffed against the pristine baseline
`151e36c`: no divergence. The described state — target acquired, look goal engaged, no movement — is
what the entity does when its **navigation** cannot produce a path while its **look** control still
tracks, which upstream can reach on its own (a target across a wall, in a one-block pocket, or above
the water column the shrimp will not leave). Sinking while doing it is just the absence of swim
impulse, not a separate fault.

Blocked on: **MC version, loader, whether the shrimp was tamed, which fish, and the setting** (open
ocean vs. an aquarium/tank build). A short clip would settle it in one viewing.

⚠️ Rig lessons from the attempts to reproduce it headlessly, all of which cost a false negative first:
`execute as` does **not** move the position context (use `execute at`); `Invulnerable:1b` makes an
entity untargetable, so a "safe" test fish is no test at all; `noActionTime >= 100` disables stroll
goals in a player-less world, so a headless pool of mobs stands still for reasons that have nothing to
do with the mod; and the attribute is `minecraft:generic.max_health` on 1.21.1.

---

### #108 — the straddleboard's grey panel is invisible in the item icon (1.20.6 → 1.21.3), and the contrast setting

> *"idk if the straddleboard is meant to look like this? i only notice cause i wanted to use Sweety's
> Items resource pack… the gray part of the board disappears on the gray background, will adjust it in
> the next update!"* — `2.1.3+1.21.1-neoforge`

**First filed as NOT A BUG, and that verdict was wrong.** Every straddleboard asset really is
byte-identical to the pristine baseline `151e36c` (`textures/item/straddleboard.png`,
`straddleboard_overlay.png`, `straddle_helmet.png`, `straddle_saddle.png`,
`textures/entity/straddleboard.png`, its overlay, and `models/item/straddleboard.json`), so the first
sweep stopped there and blamed the resource pack. The assets were never the question: the grey panel
is **`layer1`, a tint layer**, and on the reporter's own node it was being drawn at **alpha 0**.

`models/item/straddleboard.json` is `item/generated` with `layer0` = the wooden base and `layer1` =
`straddleboard_overlay`, an all-grey sprite that only ever looks grey because an item colour handler
tints it. Upstream's handler is `(stack, i) -> i < 1 ? -1 : getDyedColor(stack, 0XADC3D7)` — and
`0xADC3D7` has **no alpha byte**. Read out of the jars, both halves:

- `ItemRenderer.renderQuadList` began unpacking the handler's alpha at **1.20.6**, not at 1.21.4.
  `VertexConsumer.putBulkData` gained a fourth float there and the tint goes through
  `FastColor$ARGB32.alpha/red/green/blue`:

  ```
  1.20.1: alpha-calls=0   putBulkData:(…PoseStack$Pose;…BakedQuad;FFFII)V
  1.20.4: alpha-calls=0   …FFFII)V
  1.20.6: alpha-calls=1   …FFFFII)V     <- boundary
  1.21:   alpha-calls=1   …FFFFII)V
  1.21.1: alpha-calls=1   …FFFFII)V
  1.21.2: alpha-calls=1   …FFFFII)V
  1.21.3: alpha-calls=1   …FFFFII)V
  ```

- `DyedItemColor.getOrDefault` wraps a **stored** dye in `ARGB.opaque(rgb())` but returns the
  **fallback verbatim** — `ifnull → iload_1 → ireturn`, no `opaque` on that branch, identical on
  1.20.6 / 1.21.1 / 1.21.3. So a *dyed* board was fine and an *undyed* one drew its panel at alpha 0.

That is **14 nodes** — 1.20.6, 1.21, 1.21.1, 1.21.3 × three loaders plus 1.21.2 × NeoForge/Fabric —
broken since `2.0.0`, and in upstream Alex's Mobs itself from the moment it reached 1.20.6. 1.20.1 and
1.20.4 pass three floats and never look at the top byte, so the two oldest nodes were always right;
from 1.21.4 there are no item colour handlers at all, so the panel simply drew untinted there (the
documented cosmetic loss, below). Vanilla's own leather default is the alpha-carrying `-6265536` for
exactly this reason. The reporter saw a hole where the panel should be, and the pack only made the
hole obvious — with the vanilla slot behind it, a light-grey gap in a light-grey panel reads as
"low contrast", which is why nobody had reported it in six years.

⚠️ **Generalisation: a colour a version starts reading one more byte of does not fail, it draws.**
There is no exception, no log line and no signature change — `int` in, `int` out, on both sides of the
boundary. The tell is the *consumer*: sweep `renderQuadList`/`putBulkData` for an added float, not the
handler for a changed type. And the fallback branch of a vanilla getter is not covered by whatever the
main branch does to its value — read both.

#### The setting the user asked for

> *"add a setting to change the straddleboard contrast."*

Two options in `CommonConfig`, both plain ints in `config/amc.json` so they cycle in `/aac config` and
in the chest menu (`ListValue`/`StringValue` are read-only there, so a hex string was not an option):

| option | default | layer |
|---|---|---|
| `straddleboardBaseColor` | `0xFFFFFF` (16777215, untinted) | `layer0`, the wooden base |
| `straddleboardPanelColor` | `0xADC3D7` (11387351, upstream's grey) | `layer1`, the panel — **fallback only**, a dyed board keeps its dye |

Contrast needs both, because a tint can only ever darken: raising the panel alone cannot brighten it
past the sprite, so the base is what you lower to open the gap.

Both are **client-visual only**. `EntityStraddleboard#getColor`/`#isDefaultColor` and
`ItemStraddleboard#getColor` are untouched — those values are synched, and re-colouring them would
make two clients disagree about what colour a board *is*. `RenderStraddleboard` reads the same two
fields for the entity in the world, so the item icon and the ridden board always match.

New `client/render/AMStraddleboardTint`, which both eras ask for the colour:

- **< 1.21.4 (19 nodes)** — the two existing colour-handler lambdas in `ClientProxy` (the
  Forge/NeoForge `RegisterColorHandlersEvent.Item` arm and the Fabric `ColorProviderRegistry.ITEM`
  one) now call `AMStraddleboardTint.tintOf(stack, colorIn >= 1)`. That single call site is also
  where #108 is fixed: `tintOf` ORs `0xFF000000` in unconditionally.
- **≥ 1.21.4 (30 nodes)** — the item colour handlers are gone; tints live in the item **model
  definition**, `"tints":[…]`, index == tintIndex. `DataPackMigration` now writes
  `"tints":[{"type":"alexsmobs:straddleboard_base"},{"type":"alexsmobs:straddleboard_panel"}]` into
  `assets/alexsmobs/items/straddleboard.json`, and the class implements `ItemTintSource` to supply
  them. ⚠️ A `minecraft:constant` would freeze the value into the parsed model and could not read a
  config or a dye, so these had to be sources of our own. **This also un-does a documented
  regression**: the board's *dye* had not tinted the icon at all on those 30 nodes since `2.0.0`.

`ItemTintSource` is byte-identical 1.21.4 → 26.2 (`int calculate(ItemStack, ClientLevel,
LivingEntity)` + `MapCodec<? extends ItemTintSource> type()`), so no era gate is needed on the
signatures. Registration splits per loader exactly as `alexsmobs:icon` does (#45/#48): NeoForge has
`RegisterColorHandlersEvent$ItemTintSources` on every build from 1.21.4, classic Forge has no such
nested event on any build (checked in the `1.21.4-54.1.17` and `26.2-65.1.0` universal jars — only
`$Block` and `$ColorResolvers`), so Forge and Fabric reflect `ItemTintSources.ID_MAPPER`, found **by
field type** (`ExtraCodecs$LateBoundIdMapper`, the class's only such field — private through 1.21.11,
public from 26.1, so by-name would break and by-type does not).

The class is **not** excluded from the source set below 1.21.4: the `implements` clause and each
`@Override` are individually gated, so it compiles everywhere as an inert plain class holding only
`tintOf` — the same shape as `AMIconSpecialRenderer`, and the reason no `ModPlatformPlugin` exclusion
was needed.

⚠️ Tints recompute **every frame** (`BlockModelWrapper#update`, `CuboidItemModelWrapper` from 26.x
call `calculate` on each update), so `/aac config set` shows up with no resource reload. The returned
`int` is appended to the render state's model identity from 1.21.6 — harmless, because `Integer` has
value equality, which is precisely what #107's `ItemStack` did not.

⚠️ **The `!mc121-vtx-color` rule cost a compile round.** `stonecutter.gradle.kts:546` is
`replace(".color(", ".setColor(")`; the helper was first named for the thing it returns, so every call
to it was rewritten into a vertex setter on 30 nodes. Renamed to `tintOf`. The same rule then
rewrote the token inside the javadoc explaining the rule — replacements reach into comments — so that
sentence now spells none of the matched substring out. (Rule 4 / `stonecutter.md`.)

**Verified:** compile-green on 9 era-spanning nodes in one invocation (`1.20.1-forge`,
`1.20.4-neoforge`, `1.20.6-forge`, `1.21.1-fabric`, `1.21.3-neoforge`, `1.21.4-forge`,
`1.21.4-fabric`, `1.21.11-fabric`, `26.2-neoforge`) — both sides of the 1.20.6 alpha boundary and of
the 1.21.4 API split, all three loaders. The `1.20.1-forge` class file carries `ldc -16777216; ior`,
i.e. the active node compiles from `src/` and its stale generated tree is not what was built.
`processResources` on 6 nodes (with `build/resources` moved aside first — rule 9) emits the two tint
entries on all four ≥1.21.4 nodes, leaves the two `<1.21.4` controls with no `items/` directory, and
leaves the 89 spawn-egg tints unchanged at 90 files carrying `"tints"`.
⚠️ **No client session.** The cheap check is one `1.21.1-neoforge` client (the reporter's own node):
an **undyed** straddleboard in the hotbar must show its grey panel at all, and `/aac config set
straddleboardBaseColor 8421504` must darken the wood under it.

---

### Housekeeping from this pass

- `versions/1.21.1-neoforge/run/server.properties` now has **`server-port=25599`** (25566 collided
  with the user's own live server) and RCON enabled on 25575 with password `underminer`.
- `/tmp/amc-jar-trash/` and `/tmp/res-trash/` hold build output moved aside
  during verification (`rm` is sandbox-blocked — rule 3).
- **Full-matrix verification — DONE (2026-08-20/21), and every number was predicted first.** One
  49-node `bash -c` invocation, `--max-workers=4`, `MOD_IS_RELEASE=true`: `BUILD SUCCESSFUL in
  8m 26s`, `GRADLE_EXIT=0`, 147 jars, 49 mod jars, **zero `-SNAPSHOT`**, zero non-`2.1.5`. Then all
  four verifiers, each on its predicted count:
  - `verify_mixin_targets.py` — **`nodes=49 jars=49 selectors=1108 problems=0 skipped=0`**. The
    prediction was `1070 + 14 + 24`: the documented baseline, plus `CameraMixin` on each of the 14
    NeoForge ≥1.21 nodes, plus the atlas mixin on each of the 24 nodes ≥1.21.6. Hitting it exactly is
    the independent proof that **both** new mixins are gated to precisely the node sets intended —
    nothing else in the run can tell you that. ⚠️ The prediction is only sharp because the tool counts
    `method="…"` selectors and **not** `@Shadow` fields (read at `verify_mixin_targets.py:380`), which
    is also why the atlas mixin deliberately casts instead of shadowing.
  - `verify_mixins.py` — `jars=49 problems=0`, `declared` at 19 on the ≥1.21.6 nodes.
  - `verify_assets.py` — `literals=394 missing=0` (this pass ships no new asset).
  - `verify_convention_tags.py` — `problems=9`, **exit 1 by design**: the nine `#c:tools/spear(s)`
    optionals from #59. Read *which* tags it names; never gate on its exit code.
  ⚠️ 147 stale `2.1.4` jars were moved out of `versions/*/build/libs` **before** the build, and 16
  stale `-SNAPSHOT` sources/javadoc jars afterwards. A verifier that never opened your jars still
  prints a green `jars=49`; a count *above* the prediction means it read something you did not build.
- **Side findings filed, none fixed:** `ItemBearDust.fillItemCategory` is empty, so bear dust never
  appears in the creative tab; `EntityLaviathan#dropEquipment()` is likely a dead override ≥1.21.2;
  the three `canBeLeashed(Player)` overrides (`EntityBoneSerpent:127`, `EntityCentipedeHead:327`,
  `EntityVoidWorm:282`) are dead ≥1.21.5; and the three multipart classes named under #105 lack the
  Fabric interaction send.

---

## Thirty-third pass (2026-08-21) — four reports, against published `2.1.3` / `2.1.4` / `2.1.5`

Four reports from four reporters. **Three real faults, all fixed** (#109, #110, #111); the fourth is
**NOT THIS MOD** and is resolved on mechanism rather than on "we can't reproduce it". Two of the
three had been shipping since `2.0.0`, and one of those (#109) is a **hard crash that makes a world
permanently unloadable** — the most severe thing in this port since #102.

`mod.version` is now **`2.1.6`**.

---

### #109 — "Cachalot Echo crash still not fixed in 2.1.4" — every Forge node from 1.20.4 up

> *"My world became completely unplayable after a `alexsmobs:cachalot_echo` entity spawned. The game
> crashes every time the world loads with: `java.lang.IllegalArgumentException: class
> com.github.alexthe666.alexsmobs.entity.EntityCachalotEcho is not an instance of interface
> net.minecraftforge.entity.IEntityAdditionalSpawnData`. The crash report specifically points to
> `EntityCachalotEcho.getAddEntityPacket()`."*

The reporter had already done the diagnosis; the only open question was why it survived `2.1.4`.
The answer is that it was never *addressed* — nothing in `2.1.4` or `2.1.5` touched this path, and
the fault is not specific to the cachalot echo at all.

**Cause.** `AMPlatform.getEntitySpawnPacket` had a `forge && >=1.20.2` arm reading

```java
return net.minecraftforge.common.ForgeHooks.getEntitySpawnPacket(entity);
```

That helper is **not** a general-purpose add-entity-packet builder. Its first statement, on every
Forge build in the matrix from 1.20.2 up (read out of the userdev sources for **49.2.8**, **50.2.9**
and **60.1.11**), is

```java
if (!(entity instanceof IEntityAdditionalSpawnData add))
    throw new IllegalArgumentException("Entity type " + ... + " does not implement IEntityAdditionalSpawnData");
```

Nothing in this mod implements that interface — measured, not assumed: neither `writeSpawnData` nor
`readSpawnData` exists anywhere in the tree, and the only mentions of the interface were the
comments in `AMPlatform` itself. So **every one of the 21 entities that overrides
`getAddEntityPacket` throws the moment the server tries to send it to a client.**

The pre-1.20.2 spelling, `NetworkHooks.getEntitySpawningPacket`, guards the identical test with an
`if/else` and falls back to the vanilla packet — **which is the only reason `1.20.1-forge` ever
worked**, and the only reason this went unnoticed: 1.20.1 is the active node, the one every ad-hoc
check gets run on.

**Scope: 15 of 16 Forge nodes (1.20.4 → 26.2), since `2.0.0`.** NeoForge and Fabric were always on
the vanilla-packet arms and are unaffected. It is a *world-breaking* crash rather than a one-off:
the entity is saved, so it is re-sent on every load.

**Fix.** All three loaders now build vanilla's own packet directly, which is all any of the 23
callers ever wanted — they override `getAddEntityPacket` to *get* a packet, not to attach a payload.
Four arms, split only on the 1.21 `ServerEntity` parameter and on 1.20.1's surviving `NetworkHooks`:

```java
//? if (neoforge || fabric) && <1.21 {          -> new ClientboundAddEntityPacket(entity)
//? if forge && >=1.20.2 && <1.21 {             -> new ClientboundAddEntityPacket(entity)
//? if >=1.21 {                                 -> new ClientboundAddEntityPacket(entity, serverEntity)
//? if forge && <1.20.2                         -> NetworkHooks.getEntitySpawningPacket(entity)
```

⚠️ **Generalisation: a loader "hook" named after the thing you want is not necessarily a builder for
it — some are contracts, and a contract's precondition is a `throw`.** The rename from
`NetworkHooks.getEntitySpawningPacket` to `ForgeHooks.getEntitySpawnPacket` at 1.20.2 looks like a
pure move, and the port treated it as one. It was not: **the guard changed from `if/else` to
`throw`**, i.e. the *behaviour for the non-implementing case* changed while the name, the signature
and the return type all stayed put. Nothing in the compiler, in any of the four verifiers, or in a
49-node compile can see that — and a client gate cannot either, because the crash needs one of these
21 entities to actually spawn and be sent.

⚠️ Second, cheaper generalisation: **the active node is the one node whose behaviour you never learn
anything about by accident** — every quick check runs there, so a fault that spares it is a fault
nobody trips over. When a gate arm exists *only* for the active node (here `forge && <1.20.2`), read
what the other arm does with extra suspicion.

Compile-verified `GRADLE_EXIT=0` on six nodes covering all four arms.
⚠️ **No client session** — one Forge ≥1.20.4 client, `/summon alexsmobs:cachalot_echo`, closes it.

---

### #110 — the mariachi cockroach's sombrero sits in its body, on every node below 1.21.2

> *"Small (extremely niche) issue — the Mariachi Cockroach easter egg (where you give a cockroach a
> maraca) renders the sombrero lower into the body instead of on the head."*

This is **not** a re-report of #55, though it reads like one. #55 (twenty-fifth pass) was the
upstream offset-inside-a-rotated-frame fault, fixed and **client-confirmed on `26.2-fabric`**. This
one is invisible on 26.2 by construction.

**Cause.** `ModelSombrero` is a vanilla `HumanoidModel`, and **`EntityModel#young` defaults to
`true`** — read in the bytecode, `EntityModel.<init>` does `iconst_1; putfield`. Nothing ever clears
it on *this* layer's instance. `HumanoidModel` does not override `renderToBuffer`, so
`AgeableListModel`'s does the drawing, and its young branch scales the head group by
`1.5F / babyHeadScale` = **0.75** and then translates `babyYHeadOffset / 16` = 1.0 unit down the
**scaled** axis — **0.75 blocks, 12 pixels, straight into the roach's body**, with the hat itself at
three-quarter size.

The two *armour* paths never show this, which is why it survived: Forge's
`IClientItemExtensions#getGenericArmorModel` calls `ForgeHooksClient.copyModelProperties` onto the
replacement model, and Fabric's `FabricArmorRenderers` does `contextModel.copyPropertiesTo(model)`
by hand. Both copy `young` from the wearer. This layer bakes its own model and copies nothing.

**Scope: all 14 nodes below 1.21.2, all three loaders — and upstream Alex's Mobs has the same
fault.** It vanishes from 1.21.2 up because that version **removed `young` / `riding` /
`attackTime` and `copyPropertiesTo` from `EntityModel`** and moved the baby transform to mesh-bake
time, so the field the bug lives in no longer exists. That is exactly why #55's pose work looked
correct on 26.2 and why the reporter — on an unstated version — sees something #55 cannot explain.

**Fix.** One gated line in the constructor:

```java
//? if <1.21.2
this.sombrero.young = false;
```

The layer already applies its own `isBaby()` scale-and-lift at the top of `render()`, so the vanilla
young transform was pure loss whatever the roach's age. With it cleared, the <1.21.2 nodes now draw
the hat at exactly the geometry #55 solved and verified on 26.2 — the two fixes compose rather than
fight.

⚠️ **Generalisation: a model you bake yourself inherits none of the wearer's state, and one of the
fields it does not inherit defaults to `true`.** Anywhere this mod hands a `HumanoidModel` to
`renderToBuffer` without going through a loader's armour hook, ask what `young` is. The tell is a
part that is *both* mispositioned *and* slightly too small — a pure offset bug does not change size.

Compile-verified `GRADLE_EXIT=0` on six nodes straddling the 1.21.2 boundary
(`1.20.1-forge`, `1.20.1-fabric`, `1.21.1-neoforge`, `1.21.2-neoforge`, `1.21.3-forge`,
`26.2-fabric`).
⚠️ **No client session** — any node below 1.21.2, hand a cockroach a maraca.

---

### #111 — Sea Life startup crash: three catfish buckets, one `EntityType`

> *"Alex's Mobs Continued 2.1.3 for Fabric 1.21.11 causes a startup crash when Sea Life 21.11.0 is
> installed. Sea Life indexes fish buckets by their description key, but the medium and large
> catfish buckets both return `entity.alexsmobs.catfish`. This produces a duplicate-key exception in
> Sea Life's `HatcheryBlock.onLoadComplete` method. Could the catfish bucket variants be given
> unique item description or translation keys, such as `item.alexsmobs.medium_catfish_bucket`…?"*

⚠️ **The requested fix cannot work, and the diagnosis is one inference off.** Sea Life does not index
by a description key. `javap` on `fuzs/sealife/world/level/block/HatcheryBlock.class`
(21.11.0) shows `onLoadComplete()` walking `BuiltInRegistries.ITEM`, filtering
`instanceof MobBucketItem`, and doing

```java
builder.put(item.field_7991, item);      // field_7991 : Lnet/minecraft/class_1299;  == EntityType
```

into a guava `ImmutableMap.Builder`. **The key is the `EntityType`**, i.e. `MobBucketItem`'s private
final `type` field. The reason the exception *text* names `entity.alexsmobs.catfish` is that
`EntityType.toString()` is literally `return getDescriptionId();` (javap, 1.21.11) — so guava's
"Multiple entries with same key" message prints a translation key for an object that is not one.
**Renaming any item key changes nothing**; the three buckets would still map to
`AMEntityRegistry.CATFISH` and `ImmutableMap.Builder#build()` would still throw.

This is upstream Alex's Mobs' shape, not a porting fault — upstream registers the same three
`ItemModFishBucket`s on the same one entity type. It has simply never met a mod that builds an
`EntityType → bucket` map.

**Fix — a class split, not a key rename.** New `item/ItemCatfishBucket`, extending plain
`BucketItem` rather than `MobBucketItem`, used for `MEDIUM_CATFISH_BUCKET` and
`LARGE_CATFISH_BUCKET`. `SMALL_CATFISH_BUCKET` **deliberately stays** an `ItemModFishBucket`, so
exactly one AMC bucket claims the catfish entity type and any third-party `EntityType → bucket`
lookup still resolves catfish to a sensible AMC item instead of to nothing.

`MobBucketItem` adds exactly five things over `BucketItem` (verified by javap on every version
1.20.1 → 26.2): the private final `type`, `emptySound`, `checkExtraContent`, `playEmptySound` and a
private `spawn`. `ItemCatfishBucket` therefore re-declares just two overrides —
`checkExtraContent` (delegating to a new package-private
`ItemModFishBucket.spawnFish(EntityType, …)`, so the split costs no duplicated logic) and
`playEmptySound` (restoring `SoundEvents.BUCKET_EMPTY_FISH`; `BucketItem` would play the water
sound). Both are gated at the **1.21.5** `Player` → `LivingEntity` widening, which is the only
signature boundary either method has in the range; `LevelAccessor.playSound` takes `Player` below
and `Entity` above, so one call text serves both arms.

Nothing else keyed off the class: a tree-wide grep found **no** `instanceof ItemModFishBucket`, no
dispenser-behaviour registration and no tooltip branch for catfish (`appendHoverText` branches only
on LOBSTER, TERRAPIN and COMB_JELLY). The size logic keys off the **stack**, not the class —
`addExtraAttributes` tests `stack.is(SMALL/MEDIUM/LARGE_CATFISH_BUCKET)` — so it works unchanged.
Census of the registry: 15 `ItemModFishBucket` registrations over 13 distinct entity types, so
**CATFISH was the only duplicate**; `ItemCosmicCodBucket` is the only subclass and is unaffected.

⚠️ **Generalisation: a reporter who has read a stack trace is usually right about *where* and often
wrong about *what*** — here the mechanism was named confidently and precisely, and the one word that
mattered ("description key") came from an exception message that prints a description key for an
`EntityType`. Read the other mod's bytecode before implementing the fix it asks for; the requested
change would have shipped, been believed, and not worked.

⚠️ Second: **this class of bug scales with the mod's own duplication, not with the other mod.** Any
aquarium/tank mod that maps `EntityType → MobBucketItem` will hit the same thing, so the split
immunises AMC generally rather than working around Sea Life specifically.

Compile-verified `GRADLE_EXIT=0` on seven nodes (`1.20.1-forge`, `1.20.1-fabric`,
`1.20.6-neoforge`, `1.21.4-forge`, `1.21.5-neoforge`, `1.21.11-fabric`, `26.2-forge`), covering both
constructor arms, all three `appendHoverText` arms and both `checkExtraContent` arms.

---

### Oculus `MixinLevelRenderer_EntityListSorting` apply failure — NOT THIS MOD

> *"Every time I try to replace with this mod and its library, I get: `Mixin apply for mod oculus
> failed oculus-batched-entity-rendering.mixins.json:MixinLevelRenderer_EntityListSorting … Implicit
> variable modifier injection failed in …LevelRenderer::batchedentityrendering$sortEntityList`.
> Everything runs fine without `alexsmobs-2.1.5-forge+1.20.1.jar` and
> `codxlib-1.5.0-forge+1.20.1.jar`."*

**Resolved on mechanism, with bytecode evidence, rather than on "cannot reproduce".** Five
independent facts, each checked in a jar:

1. **Ordering.** Oculus/Iris's `MixinLevelRenderer_EntityListSorting` declares **priority 999**.
   AMC's `alexsmobs.mixins.json` (read out of the shipped `2.1.5-forge+1.20.1.jar`) declares **no
   `priority` key at all**, i.e. the default **1000**. In Mixin 0.8.5, `MixinInfo.compareTo` is
   `return this.priority - other.priority` — **ascending**, so **lower priority applies first** — and
   `MixinApplicatorStandard` runs three passes (`ApplicatorPass.MAIN` → `PREINJECT` → `INJECT`), each
   iterating that sorted list in full. So Oculus's injector is *prepared* and *applied* before AMC's
   mixin contributes anything at all to `LevelRenderer`. AMC cannot be upstream of it.
2. **What AMC actually does to `LevelRenderer`.** Exactly one transform: a single `@Redirect` on
   `Lnet/minecraft/world/entity/Entity;getTeamColor()I`. That swaps one `INVOKEVIRTUAL` for an
   `INVOKESTATIC` and **adds no local variable** — which is the only thing that could break an
   *implicit* `@ModifyVariable`. (It is also not removable: `ClientEvents#onOutlineEntityColor`
   consumes the event it fires, for blue jay / rainbow outline tinting.)
3. **CodxLib is inert here.** `codxlib-1.5.0-forge+1.20.1.jar`'s `codxlib.mixins.json` has
   `"mixins": []`. No coremod, no JiJ, no AT.
4. **The `compatibilityLevel: JAVA_17` hypothesis is dead.** `Locals` in Mixin 0.8.5 has **no**
   reference to `MixinEnvironment.CompatibilityLevel` anywhere in its bytecode — it uses the LVT when
   present and `getGeneratedLocalVariableTable` (an ASM `Analyzer`) otherwise. AMC's declared
   compatibility level cannot change how Oculus's locals are computed.
5. **The failing construct is known-fragile and upstream replaced it.** Oculus 1.6.x's handler is an
   *implicit* `@ModifyVariable` at `INVOKE_ASSIGN` on `Iterable.iterator()` with `allow = 1` —
   which resolves only if there is **exactly one** candidate local of type `Iterator` at that point;
   0 or >1 throws `InvalidImplicitDiscriminatorException`, the reported exception. Iris **1.7.0
   rewrote this exact class to MixinExtras `@WrapOperation`** for that reason: `iris-1.6.4`,
   `1.6.9` and `1.6.17` carry the `@ModifyVariable`, `iris-1.7.6+mc1.20.1` carries
   `@WrapOperation`, same class, same priority 999.

**Reply to the reporter:** update Oculus to a build based on Iris 1.7 or newer. If it still fails,
the decisive missing datum is the **full crash report / `latest.log`**, specifically the
`Found N candidate variables` line and the complete mod list — whatever injects the extra `Iterator`
local must be a third mod applying at priority < 999, and the log names it.

⚠️ **Generalisation: "it works when I remove mod X" is evidence about a *set*, never about X.** The
cheap discriminator for a mixin-apply failure is the **priority of the failing mixin against the
priority of every mixin touching the same class** — if the failing one applies first, no later mixin
can be the cause, and that is a one-line check in two JSON files. Do it before reading any bytecode.

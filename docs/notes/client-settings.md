# Client settings, `/shieldpose`, and the (removed) `/aac` command

> Read when adding a player-facing display toggle or a client command, when tuning a held-item
> pose, or when wondering why `/aac` is gone.
>
> Part of the Alex's Mobs Continued porting notes.

## What exists

**No player-facing client command, and no client-side settings file.** There was one between
`2.0.2` and `2.0.4`; `2.0.5` removed the lot, and the cautionary tale below is why.

What does exist is one **development-only** command, `/shieldpose`, described in the next section.
It is registered through the same three loader seams the removed `/aac` used, so those seams are
live code again rather than the comment-only map this note used to be.

## `/shieldpose` — the held-item pose tool

`client/command/AMShieldPoseCommand`. Added 2026-08-06, while #33's blocking pose was still being
tuned in-client.

**What it does.** Rewrites the `display` block of the shield of the deep's four model files and
calls `Minecraft#reloadResourcePacks()` — the same thing F3+T does. The edit lands in the `src/`
copy *and* in every `versions/<node>/build/resources/main` copy, so it is visible immediately in the
running client and survives into the repo with no Gradle run in between.

```
/shieldpose                          show the current numbers
/shieldpose pose normal|blocking     sticky target pose      (default blocking)
/shieldpose ctx first|third|gui|…    sticky display context  (default first)
/shieldpose set   rot|trans <x> <y> <z>      set   scale <v>
/shieldpose nudge rot|trans <x> <y> <z>      nudge scale <v>
/shieldpose reload
```

The sticky pose/context pair is the ergonomic point: a repeated nudge is one short line plus
up-arrow, which is what iterating on a pose actually looks like.

**Why it is safe to have in the tree.** `available()` walks up from `Minecraft.gameDirectory`
looking for `stonecutter.properties.toml` **and** `settings.gradle.kts` together, and all three
registration sites skip the command when there is no match. A player's install has neither above
its `.minecraft`, so a shipped jar never registers the command — it is absent from tab-completion
and writes to nothing. Requiring both markers is deliberate; either alone could be a sibling
checkout.

**Why it does not fight `scripts/shieldpose.py`.** The two share the file list, the context map and
the on-disk format. The script writes with Python's `json.dump(indent="\t")`; the command writes
with Gson through a `JsonWriter` whose indent is set to `"\t"`, and untouched numbers survive
verbatim because `JsonParser` keeps them as lazily-parsed text. **Verified byte-identical** on all
four model files — a round-trip through the command's writer reproduced each file exactly, to the
character. Values the command *writes* follow Python's rule of dropping the decimal point when
whole, so the two remain interchangeable and neither reformats the other's output into the diff.

⚠️ **The two tools are not redundant.** The script prints a projected on-screen bounding box and
needs no running client; the command needs no alt-tab. Keep both.

⚠️ **Everything in [`bug-reports.md`](bug-reports.md) #33 still applies.** First and third person
share one set of `elements`, so a fault in one context and not the other is in the `display` block
by construction — establish which context a screenshot is of before touching geometry.

### The off-hand is not a free mirror (2026-08-06)

**Two separate transforms mirror the off-hand, and each turned the spikes to face the player once.**
The first was found by looking at a *carried* shield, the second by pressing right-click while
carrying one. Neither is visible from the JSON.

**(1) `ItemTransform#apply` negates `translation.x`, `rotation.y` and `rotation.z`** for the left
hand and nothing else — read from the decompiled
`net.minecraft.client.resources.model.cuboid.ItemTransform`, not from memory. So **writing the same
rotation into both hands does not give the same pose in both hands, it gives a mirrored one.** On a
symmetric item nobody notices; on this shield the `normal` pose's yaw of `89°` became `−89°` in the
off-hand, a **178° turn**. Since a shield is normally *carried* in the off-hand, that was the common
case, not the corner case. An earlier version of this note and of `scripts/shieldpose.py` claimed
the two first-person entries were "identical by construction" — **that was wrong**, and being wrong
in the tool's own documentation is what hid the bug: both writers dutifully wrote identical values.

**(2) From 1.21.4, `ItemInHandRenderer`'s `case BLOCK` applies a hardcoded, mirrored arm transform**
to any item that is not a `ShieldItem`:

```java
if (!(itemStack.getItem() instanceof ShieldItem)) {
    poseStack.translate(invert * -0.14142136F, 0.08F, 0.14142136F);
    poseStack.mulPose(Axis.XP.rotationDegrees(-102.25F));
    poseStack.mulPose(Axis.YP.rotationDegrees(invert * 13.365F));
    poseStack.mulPose(Axis.ZP.rotationDegrees(invert * 78.05F));
}
```

`invert` is `+1` right / `−1` left. `ItemShieldOfTheDeep extends Item`, never `ShieldItem`, on every
loader, so it always takes this branch — and no value in the `display` block can cancel a mirror
that is applied *before* it. This is why fix (1) did not fix the blocking pose: its rotation is
`[0, 0, 0]` and negating zero is zero, yet the shield still came out mirrored.

⚠️ **The 1.21.4 boundary is exact and was measured, not recalled** — every 1.20.1 → 26.2 client jar
was disassembled; 1.21.3 has no such constants, 1.21.4 does, and it is vanilla rather than a Forge
patch (checked against a vanilla Fabric-mapped jar). Below 1.21.4 `case BLOCK` is a bare
`applyItemArmTransform`. **So one set of numbers means two different poses on the two sides of that
line**, and the checked-in `<1.21.4` blocking model had been carrying numbers tuned under a
transform that does not exist there — a second, unreported fault on ~20 nodes.

**Both tools therefore solve rather than copy.** `ctx first` treats the typed numbers as the pose in
the frame of the file *this* client renders (`PRIMARY`: index 0 `>=1.21.4`, index 1 below, which in
the command is a stonecutter gate), converts them to a hand- and era-independent world pose, and
solves all four entries back out of it:

```
world orientation  W = A · Q      =>  Q = Aᵀ · W
world offset       P = a + A · T  =>  T = Aᵀ · (P − a)
```

where `(A, a)` is vanilla's own pre-transform for that file and hand — identity where there is none.
The off-hand needs no separate `translation`: its arm transform is the right hand's conjugated by
the x-mirror, so the JSON translation that lands the mirrored position is the **same** one and
`ItemTransform`'s negation of `translation.x` does the mirroring, which is wanted (the item *should*
sit by whichever hand holds it). Its `rotation` is not free that way — we want the orientation
identical, not mirrored — so it is solved separately and then y/z pre-negated to cancel (1).
`applyItemArmTransform`'s own `translate(invert * 0.56, −0.52, −0.72)` cancels out of the solve
entirely, being the same for both files and correctly mirrored; only the Python keeps it, because
its on-screen projection needs an absolute position.

The Java port is checked against the Python by running the extracted maths standalone: same seven
solved triples, to four decimals. `firstright` / `firstleft` skip the solve and write one key raw —
the escape hatch for a deliberately asymmetric pose.

The checked-in result: `normal` is `[0, 89, 0]` right / `[0, −89, 0]` left in both files; `blocking`
is `[0, 0, 10]` / `[−26.229, −5.3438, −167.3458]` with `translation [6, 7, −6]` in the `_3d` file and
`[−102.25, ±13.365, ±88.05]` with `translation [−9.1038, −4.7113, −3.9263]` in the `<1.21.4` one.

Those four entries are a useful arithmetic check on the solve, because they were authored as one
right-hand pose and the other three fall out of it. Adding `10°` of roll to the authored `rotation.z`
moved the off-hand's z by exactly `−10` and the `<1.21.4` right hand's by exactly `+10`, which is what
`Q = Aᵀ · W` predicts: a post-multiplied `Rz` commutes straight through `Rx·Ry·Rz`'s last factor. If a
future edit ever breaks that correspondence, the solve is wrong, not the pose.

**Vanilla's own answer, worth copying when a pose can afford it:** pick a yaw the negation cannot
change. `shield.json` and `shield_blocking.json` both use `rotation.y = 180` for *both* hands, and
−180 ≡ 180. Vanilla lets only `rotation.z` differ — a roll, where a mirror is genuinely correct —
and hand-tunes `translation` per hand rather than relying on the negation at all (blocking: right
`[−15, 5, −11]`, left `[5, 5, −11]`). A yaw of 0 or 180 is the robust choice; anything near ±90 is
the worst case, because that is where the negation swings the model furthest. Vanilla can afford it
because a `ShieldItem` skips transform (2) altogether — this shield cannot.

## The rest of this note: the removed `/aac`

## The cautionary tale: `/aac nameplates`

`2.0.2` added a client toggle that hid the floating names drawn above this mod's mobs, because
players kept reporting those names as unwanted. **The names were a bug in this port, and the
toggle was a workaround for it.** Found 2026-08-04 by looking at a wild, unnamed mob in a
26.2 client and asking why it was labelled at all.

`client/render/compat/MobRenderer` — the shim that restores upstream's two-type-parameter
`MobRenderer<T, M>` after 1.21.2 widened vanilla's to three — extends the compat
`LivingEntityRenderer`, and therefore **vanilla `LivingEntityRenderer`, not vanilla `MobRenderer`**.
That is fine for shadows, models and layers. It is not fine for nameplates, because vanilla splits
the decision across exactly those two classes:

| Class | `shouldShowName` answers | For an ordinary visible mob |
|---|---|---|
| `LivingEntityRenderer` | is this entity visible to you at all — teams, invisibility, is-it-your-camera | **`true`** |
| `MobRenderer` | ANDs in `entity.shouldShowName() \|\| entity.hasCustomName() && entity == crosshairPickEntity` | `false` |

Inheriting from the wrong side dropped the second half for **all 93** of this mod's renderers, so
every Alex's Mobs mob wore a permanent floating type name — "Grizzly Bear" over a wild bear — on
every node **from 1.21.2 up**, on all three loaders. Below 1.21.2 the renderers still import
vanilla's `MobRenderer` (the `!mc2102-render-import-*` replacements only fire at `>=1.21.2`), which
is why a reporter on 1.21.1 never saw it and why this survived so long.

The fix is a `shouldShowName(T, double)` override on the shim restoring vanilla's clause. Two
things about it that are easy to get wrong:

- **The legacy one-arg bridges must call `this.`, not `super.`** The compat `EntityRenderer` and
  `LivingEntityRenderer` both carry a `shouldShowName(T entity)` bridge for the three renderers
  that reimplement `render(T, …)` — `RenderTiger`, `RenderUnderminer`, `RenderFarseer`. They
  delegated with `super.shouldShowName(entity, dist)`, which steps straight over the shim's new
  override and would have left those three labelling every mob after the other 90 stopped.
- **The whole `client/render/compat/**` tree is source-set-excluded below 1.21.2**
  (`ModPlatformPlugin.kt`), so it may use the modern API with no Stonecutter arm.

### The second fault the same investigation turned up

`mixin/renderstate/EntityRendererMixin` captured the entity onto the render state at
`@At("TAIL")` of `extractRenderState`. That method calls `extractNameTags` **partway through its
own body**, and that is where Forge/NeoForge post `RenderNameTagEvent.CanRender` — verified in the
decompiled sources across the range, at line ~192 of ~250 on 26.x and ~198 of a method starting at
173 on the older 1.21.x. Render states are freshly allocated per entity per frame, so
`AMStateAccess.entity` handed the nameplate hook `null` every time and no veto ever fired.

- Scope: **Forge and NeoForge, `>=1.21.2`**. Fabric was unaffected — `FabricNameTagMixin` injects
  at the *draw* stage (`renderNameTag` / `submitNameTag` / `submitNameDisplay`), which runs long
  after extraction, so the duck was already populated there.
- Fixed by moving the capture to `@At("HEAD")`, which is strictly earlier and safe for every other
  reader.
- ⚠️ **This fix is still load-bearing after the command's removal.** The surviving reason to veto a
  nameplate — hiding the player's own plate while their camera entity is a bald eagle in
  singleplayer — reads the entity off the render state through the same duck.

**The rule this leaves behind:** a shipped toggle that only ever *turns something off* is worth
one look at why the thing is on. Had `/aac nameplates` been client-verified when it shipped, its
own inertness on Forge/NeoForge would have surfaced the renderer fault two releases earlier.

## What removal touched

Seven places, which is the useful part of the map if a setting is ever added back:

| Where | What was there |
|---|---|
| `client/AMClientSettings.java` | the Gson two-field config — **deleted** |
| `client/command/AMClientCommands.java` | the brigadier tree, source type left as an open `S` — **deleted** |
| `client/event/ClientEvents#onRenderNameplate` | the `shouldHideNameplate` clause, in all three loader arms |
| `client/event/ClientEvents#onRegisterClientCommands` | the Forge + NeoForge registration arms |
| `fabric/client/FabricClientEvents#registerClientCommands` | the Fabric registration, plus its call site |
| `assets/alexsmobs/lang/en_us.json` | `alexsmobs.command.nameplates.{on,off}` |
| players' `config/alexsmobs-client.json` | now an orphan; harmless, never read again |

`onRenderNameplate` itself **stays** — the bald-eagle case still needs it, and with it the
`FabricNameTagMixin`, `AMRenderEventCompat.nameTagContent` and the three-way spelling of the veto
described below.

## Reusable: where the three loaders meet

Worth not re-deriving, all verified on the 49-node tree.

**A nameplate veto** funnels through `ClientEvents.onRenderNameplate` on every loader:

- Forge/NeoForge post `RenderNameTagEvent` and the handler is `@SubscribeEvent`-scanned.
- Fabric: `mixin/fabric/client/FabricNameTagMixin` → `FabricClientEvents.fireRenderNameTag` calls
  the same handler on the same shared instance.
- The three renderers that override `render` outright fire the event by hand via
  `AMRenderEventCompat.nameTagContent`, so they are covered too.

The handler has three arms because the veto is spelled three ways (boolean return on Forge ≥26,
`TriState` on NeoForge ≥1.20.6, `Event.Result.DENY` elsewhere). ⚠️ The
`forge || fabric || <1.20.6` veto line is coupled by a **grep** to an import gate at the top of the
file — there must stay exactly one copy of it, so change both or neither.

**A client command** — this is the table `/shieldpose` is wired from, and it held up unchanged when
the seams were brought back in 2026-08-06 after `2.0.5` had commented them out:

| Loader | Seam | Source type | Feedback |
|---|---|---|---|
| Forge | `RegisterClientCommandsEvent` (`net.minecraftforge.client.event`) | `CommandSourceStack` | `sendSuccess(() -> msg, false)` |
| NeoForge | `RegisterClientCommandsEvent` (`net.neoforged.neoforge.client.event`) | `CommandSourceStack` | `sendSuccess(() -> msg, false)` |
| Fabric | `ClientCommandRegistrationCallback` (fabric-command-api-v2) | `FabricClientCommandSource` | `sendFeedback(msg)` |

- **`RegisterClientCommandsEvent` is shape-stable across the whole 1.20.1 → 26.2 range** on *both*
  Forge and NeoForge — same `getDispatcher()` returning `CommandDispatcher<CommandSourceStack>`,
  verified by `javap` at both ends. Only the package differs, so there is no version axis at all.
  It still works on Forge's EventBus 7 because `MinecraftForge.EVENT_BUS` is an
  `EventBusMigrationHelper` there, whose `register(Object)` scans `@SubscribeEvent`.
- **`ClientCommandManager` was removed in fabric-command-api-v2 3.x** — the releases the 26.x nodes
  pin — in favour of a `ClientCommands` class. Every Fabric tutorial names it, so this is easy to
  walk into. Sidestep it entirely by building the tree from plain
  `com.mojang.brigadier.builder.LiteralArgumentBuilder.literal`, identical on every node.
- Leave the brigadier source type as an open type parameter `S` and have callers supply only "how
  to say something back" — that is what let one tree compile unchanged on all 49 nodes.

## Reusable: why a setting should not go in `CommonConfig`

The mod's 249 existing options are a `ForgeConfigSpec` registered as a Forge **COMMON** config:
baked once at load into `AMConfig`'s static fields, never written back, and semantically a
pack/server setting. A display toggle is neither — it must be writable at runtime and it means
nothing to a server. Routing a write back through the spec would also mean adding `set`/`save` to
`fabric/config/ForgeConfigSpec`, whose javadoc states in as many words that its API surface is
deliberately closed. A small JSON file costs less and branches on neither loader nor MC version:
Gson ships with Minecraft on all 49 nodes and `Minecraft.gameDirectory` is a public field the whole
way from 1.20.1 to 26.2.

Load it **lazily**, not eagerly at init: Fabric calls `ClientProxy#clientInit` from inside
`Minecraft`'s constructor, so an eager load would read `gameDirectory` on a half-built client.

# Mixins — five fatal modes and the two verifiers

> Read before touching any mixin, source-set exclude, or the `mixins` array. Nothing here is visible to the compiler.
>
> Part of the Alex's Mobs Continued porting notes.

### Fletching Table writes the `mixins` array — never hand it a class the node can't compile

Found and fixed 2026-07-26, after it had **hard-broken all nine `<1.21.2` nodes since Milestone 5** without
anyone noticing (they compiled, jarred and published fine; they just never reached mod loading).

`ModPlatformPlugin.configureFletchingTable` registers `mixins.create("main") { mixin("default",
"<modId>.mixins.json") }`. Fletching Table then **populates that config's `mixins` array itself**, from an
`@Mixin` source scan — and that scan does **not** honour the source-set `exclude`. Two visible consequences:

1. The whole `client` list is duplicated into `mixins` in the built jar (harmless — the classes are there,
   Forge's `RuntimeDistCleaner` blocks them on a server; this is exactly where the 5 benign
   `RuntimeDistCleaner` ERROR lines in every boot log come from, *not* from the `client` list).
2. `mixin/renderstate/**` — excluded from the compile below 1.21.2 by `configureJava`, so **no `.class`
   reaches those jars** — was still declared on every node. A mixin config naming an absent class is a
   **hard load failure**: `InvalidMixinException: The specified mixin '…renderstate.EntityRenderStateMixin'
   was not found`, thrown during `MixinProcessor.prepareConfigs`, i.e. the game dies before FML starts.

`DataPackMigration.addRenderStateMixins` (Milestone 5) was written to *add* the two entries and had been a
silent no-op from day one — its `if (entries.any { it in text }) return 0` guard always fired because
Fletching Table got there first. It is now **`pruneRenderStateMixins`**, called from a **`<1.21.2`**
`processResources.doLast`, which strips the entries (and their separator commas) back out. Expect
`Pruned 2 render-state mixins` from exactly 9 nodes per build.

**The invariant to check after touching mixins, source-set excludes, or the harness — and always before
publishing** — every mixin a jar *declares* must be a class it *ships*. That is now
**`scripts/verify_mixins.py`**, which walks every jar of the current `mod.version`, reads each
`*.mixins.json` out of the jar, and checks `package` + every entry of the `mixins` / `client` / `server`
arrays against the jar's actual entries. Nonzero exit if anything is missing:

```bash
python3 scripts/verify_mixins.py            # jars of the current mod.version
python3 scripts/verify_mixins.py --all-versions
```

Expected shape today: `declared=13 missing=0` on the nine `<1.21.2` nodes and `declared=15 missing=0` on
the nine `>=1.21.2` ones (the two extra are the render-state pair). Verified to have teeth — run against
the stashed broken 1.0.3 jar it reports `missing=2 -> ['renderstate.EntityRenderStateMixin',
'renderstate.EntityRendererMixin']`.

> ⚠️ Do **not** narrow this back to a `renderstate`-only grep (what it used to be). The class that breaks
> next will not be a render-state one, and the failure mode is silent at build time: the jar builds,
> passes tests and publishes, then dies before FML starts on the player's machine.

**It shipped broken to players.** Verified afterwards by downloading and inspecting **all 13** published
Forge/NeoForge jars — do not restate the blast radius from memory, it was overstated twice before the
audit. The truth:

| Release | Published Forge/NeoForge jars | Broken |
|---|---|---|
| **1.0.2** | 8 (1.20.1-forge was never uploaded) | **none** — they predate the render-state mixins entirely (`declared=11 missing=0`) |
| **1.0.3** | 5 (only the new/re-uploaded nodes; the 1.20.x nodes were not re-uploaded) | **2** — `1.21.1-forge` (35 dl) and `1.21.1-neoforge` (116 dl), **151 downloads total** |

So it was **1.0.3 / MC 1.21.1 only**, not "all nine `<1.21.2` nodes across two releases". The user reported
it from the outside; nothing in the build or the boot gate did.

Two process lessons from how long this hid: **(a)** the boot gate must be re-run on the **established**
nodes too, not just the wave's new ones — this broke 1.20.1's *source* the moment 1.21.2's mixins landed
(it just never got re-published), and the last 1.20.1 boot predated it by two days; **(b)** when globbing
for a jar to inspect, `ls … | head -1` picks the **alphabetically first** version (`1.0.2` before `1.0.3`) —
pin the version in the glob or you will audit a stale jar and conclude the fix did not work. A third joined
them in Milestone 8: **(c)** the gate must keep the server **ticking** for a while after `Done (` — see the
`tempt_range` note below. And a fourth, the expensive one, from the release right after this fix:

### A mixin SELECTOR that matches nothing is also a hard crash — and the server gate cannot see it

`verify_mixins.py` proves every declared mixin *class* ships. It says **nothing** about whether that
class's `@Inject`/`@Redirect` **target resolves**, and an injector with zero matches is
`InvalidInjectionException: Critical injection failure`, thrown at mixin APPLY — just as fatal as a
missing class.

The culprit was `mixin/client/LevelRendererMixin`, whose `@Redirect` pinned the **full 1.20.1 descriptor**
of `renderLevel`. That parameter list survives into 1.20.4 unchanged and then changes in nearly every
version after, so the selector resolved on **1.20.1 and 1.20.4 only — 3 nodes of 18**. It had been that way
since the mixin was vendored in Milestone 1, so **every published Forge/NeoForge jar above MC 1.20.4 has
crashed on the client since 1.0.2** — verified by javap on the downloaded jars, not inferred:

| Release | Jars | Client-crashing |
|---|---|---|
| 1.0.2 | 8 | **6** (everything ≥1.20.6) |
| 1.0.3 | 5 | **5** (all were ≥1.21.1; the two 1.21.1 ones died even earlier, on the missing-class bug above) |
| 1.0.6 | 18 | **15** (all but `1.20.1-forge`, `1.20.4-forge`, `1.20.4-neoforge`) |

The user pulled 1.0.6 entirely (all 18 versions deleted from Modrinth) after reporting the crash on
NeoForge 1.21.1. Dedicated **servers were unaffected throughout** — which is the whole reason a green
18-node server gate coexisted with a mod that could not reach the title screen.

Where `Entity#getTeamColor` is actually called (javap-verified across every mojmap jar; neither host method
is overloaded in any version, so a **name-only** selector is both unambiguous and immune to further drift —
that is now what the mixin uses):

| MC | Host method |
|---|---|
| 1.20.1 – 1.21.1 | `LevelRenderer.renderLevel` |
| **1.21.2** – 1.21.8 | `LevelRenderer.renderEntities` |
| 1.21.9+ | `EntityRenderer.extractRenderState` — **the `@Mixin` target class moves too** (done, Milestone 10) |

The 1.21.2 boundary is exactly the existing gate, so that fix was one Stonecutter branch swapping the
annotation. **1.21.9 is not** — the outline colour is baked into the render state now, so the host is a
different *class*, and the class-level `@Mixin(...)` is gated as well. It must be spelled
**fully qualified** (`net.minecraft.client.renderer.entity.EntityRenderer.class`) and the file must never
`import` that name — the `!mc2102-render-import-entity` replacement would retarget it at the mod's own
compat class. Same trap, same shape, as `mixin/renderstate/EntityRendererMixin`; that is now two of the
five compat-shadowed names biting a mixin.

`scripts/verify_mixin_targets.py` is what caught it (`no method named 'renderEntities' declared in
LevelRenderer` on both 1.21.9 jars) — a build, a boot gate and a client gate would all have been green.

**A name-only selector still gets remapped — verified, don't second-guess it.** Only `1.20.1-forge` and
`1.20.4-forge` run **SRG**-named at runtime (Forge switched to Mojmap from **1.20.6**; every NeoForge node
was always Mojmap — check with `javap -v` on any mod class in the built jar and count `m_[0-9]+_` refs).
Architectury Loom's `remapJar` rewrites `method = "renderLevel"` to `m_109599_` on exactly those two nodes
and leaves the Mojmap name alone everywhere else. So dropping the descriptor costs nothing on the SRG
nodes.

⚠️ **A third mapping namespace exists since Milestone 15: `intermediary`.** The 15 **obfuscated** Fabric
nodes (everything below 26.1) ship mixins remapped to `net/minecraft/class_1309`, `method_6091`. That is
neither SRG nor Mojmap, and `verify_mixin_targets.py` originally knew only those two — so it checked every
obfuscated Fabric jar against a **Mojmap** reference jar and reported all 140 target classes as missing.
The fix is a third branch resolving
`~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-intermediary/<mc>-*/*.jar`,
selected by matching `class_\d+`/`method_\d+`/`field_\d+` in the mixin dump.

### ⚠️ The verifier was checking STALE jars, and said `jars=49` while doing it — TWICE

Hit in Waves 1–2 (2026-07-31) and **again in Wave 4** (2026-08-01). It is the most dangerous bug the
harness has had, because its failure mode is a **green run** — and the repeat is the more useful half
of the story, so read the second subsection below even if the first looks familiar.

`verify_mixin_targets.py` globbed `alexsmobs-*-{loader}+{mc}.jar`. A dev build appends
**`-SNAPSHOT`** (only `MOD_IS_RELEASE=true` omits it), so that pattern **cannot match anything
`./gradlew build` produces**. What it matched instead was whatever *release*-named jars happened to
be lying in `build/libs` — in this case a set from an earlier run, ~50 minutes stale and predating
every source change being verified. It then printed `jars=49`, which reads exactly like full
coverage.

So for an unknown number of past waves this verifier was re-validating old artifacts. Both other
symptoms were also present and both look like *code* faults rather than harness faults:

- the count did not move when Wave 4 added three selectors (**this is what exposed it** — the run was
  predicted at 807 beforehand, came back 756, and the gap had to be explained);
- inspecting `versions/<node>/build/libs/alexsmobs-*.jar` by hand picks the stale jar too, so the new
  mixins appeared to be missing from the jar *and* from `mixins.json` while being present in
  `build/classes`. That combination reads as a Fletching Table / source-set bug and is not one.

Two fixes, both in the script:

1. the glob is now `alexsmobs-*-{loader}+{mc}*.jar` — the trailing `*` is load-bearing;
2. a node with a `build/libs` but **no matching jar** is now named and makes the run exit non-zero,
   instead of silently vanishing. Nothing previously distinguished "49 nodes verified" from "49 stale
   jars verified, N fresh nodes skipped".

**Rules this earns.** *Predict the selector count before every verifier run* — it is the only cheap
signal that separates "all arms resolved" from "the verifier never opened your work", and here it was
the sole signal. And when inspecting a built jar by hand, **list `build/libs` first and read the
timestamps**; do not let a glob choose for you. Stale non-SNAPSHOT jars were moved out of the tree
(`rm` is sandbox-blocked → `mv`) so the ambiguity cannot recur silently.

#### ⚠️ Why it came back: the first fix was procedural, and procedure decays

This is the part worth carrying forward. Waves 1–2 diagnosed the **identical** glob hole
([`porting-log.md`](porting-log.md), "The gate hole: a green verifier that was reading last session's
jars") and fixed it by adopting a *habit* — **"build with `MOD_IS_RELEASE=true` before any verifier
step"** — recorded in [`gates.md`](gates.md). `verify_mixins.py` was additionally made to match both
names. `verify_mixin_targets.py`'s glob was left broken, load-bearing on someone remembering the env
var.

Wave 4 built without it. Nothing warned, because the whole failure mode is silence — and the same
hole reopened five days later, in the same script, with the same green verdict.

**A workaround that depends on remembering is not a fix for a tooling bug.** If a script can read the
wrong input, make the script refuse; do not write the correct invocation into a notes file and rely on
the next session finding it. Both fixes above (the `*` and the fail-on-skip) are what should have
shipped the first time. `MOD_IS_RELEASE=true` remains right for *publishing* — it is just not a
verification control any more.

> The failure was loud, but note what it was *hiding*: with the target class unresolved the script
> `continue`s, so **none** of that jar's selectors were ever checked. Total coverage went `303 → 431`
> selectors once the branch existed. A verifier that cannot resolve a namespace does not merely report
> noise — it silently checks nothing.

⚠️ **A self-call `@At` target reads differently in javap, and the verifier used to get it wrong.**
When an `@At(value = "INVOKE"/"FIELD", target = …)` names a member of **the `@Mixin` target class
itself**, javap prints the reference *without* the owner prefix:

```
external:  // Method net/minecraft/client/Camera.getBlockPosition:()…
self-call: // Method updateBuffer:(Ljava/nio/ByteBuffer;…)V      ← no owner
```

`normalize_at_target` always builds its needle *with* the owner, so a self-call could never match and
came back as a false `FAIL ... not present in ...`. Found in Wave 3b-5b on `FabricFogRendererMixin`'s
`@ModifyArgs`, whose target is `FogRenderer`'s own **private** `updateBuffer` — the **first self-call
`@At` target in the tree** across 54 injections, which is why it had never been reachable. The check
now also accepts the owner-less form, **but only when the `@At` owner equals the `@Mixin` target**, and
anchored to javap's `Method `/`Field `/`InterfaceMethod ` lead-in, so a same-named member on a
*different* class still fails.

> Two things worth copying from how that was diagnosed. **The selector count did not move** — `739`
> before and after the fix — so "the number went up as predicted" proves the *selectors* resolved and
> says nothing about the `@At` targets underneath them. And the fix was **negative-tested** (wrong
> owner, absent target, wrong descriptor all still fail) before being believed: a verifier change that
> turns red into green is indistinguishable from one that turns the verifier off.

> ⚠️ **`runServer` can NEVER exercise a client mixin.** `RuntimeDistCleaner` blocks every class in the
> `client` list on a dedicated server, which is precisely why this repo documents those `/ERROR]` lines as
> benign — and that benign-ness is the blind spot. **Two consecutive releases shipped fatal client-side
> mixin faults through a fully green 18-node server gate.** The gate must include a **client** run:
> `scripts/clientgate.sh <nodes…>`, ready marker `Sound engine started` (it lands after mixin apply, mod
> construction, client setup, renderer registration and the first resource reload).
>
> **NeoForge dev clients do not start in this environment out of the box** — `fml_earlydisplay`'s
> `DisplayWindow.setupMinecraftWindow` fails with *"We seem to be having trouble handing off the window"* →
> `Failed to initialize the mod loading system and display`, at `Minecraft.<init>`, i.e. **before**
> `LevelRenderer` is even class-loaded, so it is not a mod fault and proves nothing either way. It is read
> from **`versions/<node>/run/config/fml.toml`**, *not* a system property (`-Dfml.earlyWindowControl=false`
> via `JAVA_TOOL_OPTIONS` is picked up by the JVM and ignored by FML). Set `earlyWindowControl = false` in
> that file per node.

The other four client mixins were audited at the same time and are **fine** on all 18 nodes — record this
so it is not re-derived: `HumanoidModelMixin` already gates its arm-pose injects to `<1.21.2` (the feature
is documented-inactive above that); `ItemBlockRenderTypesMixin` already branches at `>=1.21.6` for
`getRenderLayer`'s `ChunkSectionLayer` return type; `GuiMixin` uses a name-only selector; `ClientLevelMixin`
targets `getStarBrightness(F)F`, which exists through 1.21.10 — **but is ABSENT in 1.21.11**, so it will
need a branch when that node is added.

### A `replacements` rule will happily retarget a `@Mixin` — never `import` a compat-shadowed name

Found by the very first run of the new client gate (2026-07-26), immediately after the selector fix above.
A **second, independent** fatal client crash, on **all nine `>=1.21.2` nodes**, live since Milestone 5:

```
Mixin apply failed alexsmobs.mixins.json:renderstate.EntityRendererMixin
  -> com.github.alexthe666.alexsmobs.client.render.compat.EntityRenderer:
InvalidInjectionException: Invalid descriptor on …@Inject::alexsmobs$captureEntity(…EntityRenderState;F…)!
  Expected (…Lcom/github/alexthe666/alexsmobs/client/render/compat/AMRenderState;F…)
```

`mixin/renderstate/EntityRendererMixin` did `import net.minecraft.client.renderer.entity.EntityRenderer;`
— which is **exactly** the string the `!mc2102-render-import-entity` replacement rewrites to
`client.render.compat.EntityRenderer` on every `>=1.21.2` node. Stonecutter has no idea the file it just
edited is a mixin, so `@Mixin(EntityRenderer.class)` retargeted the mod's **own** compat class, whose
`extractRenderState` takes an `AMRenderState`. Fix: the target is now written **fully qualified** in the
annotation and the file carries **no such import** (a comment in the file says why).

**The trap is structural, not a one-off.** The whole compat-package design (see the render-state section)
depends on those five classes carrying the *same simple names* as the vanilla ones — so any file that
imports `EntityRenderer`, `LivingEntityRenderer`, `MobRenderer`, `RenderLayer` or `EntityModel` is silently
retargeted above 1.21.2. In ordinary mod code that is the entire point; in a **mixin** it is a crash. It
compiles clean either way, because `@Mixin` accepts any class and a handler's parameters are only checked at
apply time.

`verify_mixins.py` now has a **second check for exactly this**: it parses each shipped mixin's class-level
annotations out of the bytecode and fails if any `@Mixin` target (`value = X.class` or `targets = "a.b.C"`)
resolves into `com/github/alexthe666/`. A mixin should never target this mod's own code, so the rule needs
no allowlist. Confirmed against the broken jars first — it flagged precisely the 9 bad nodes and passed the
9 good ones — then against the rebuilt ones (18/18 clean). It reads **class-level** annotations only, so a
mod class merely referenced in a handler body (`AMStateAccess`) is not mistaken for a target.

Process note: this is the **third** distinct fatal-mixin mode in three days, and the first two were each
found only by the gate the previous bug motivated. Run **all four** pre-publish steps below, on **every**
node, every time.

#### `ClientLevelMixin`'s host moved — the fourth mixin retarget

`ClientLevel#getStarBrightness(F)F` is **gone** in 1.21.11 (mapping-verified: it is still on
`ClientLevel` in 1.21.10; in 1.21.11 **no class declares a method of that name**). Star brightness is
an environment attribute, sampled once per frame by
`SkyRenderer.extractRenderState(ClientLevel, float, Camera, SkyRenderState)` into
`SkyRenderState#starBrightness`. So the void worm's star-dimming keeps working by moving the mixin:
the `@Inject` targets `extractRenderState` and writes the field back, and — because the host is a
different **class** — the class-level `@Mixin` is gated too.

That target is spelled **fully qualified and never imported**, for the reason this project already
records twice: a `replacements` rule silently retargets an imported simple name. Same shape as
`renderstate.EntityRendererMixin` and `LevelRendererMixin`. `verify_mixin_targets.py` is what proves
the new selector resolves — the compiler cannot see it, and the client gate only sees it if it
crashes.

#### The mixin selector that only the verifier could see — again

`client/GuiMixin`'s `@Inject` pinned `Gui#render`. **MC 26 renamed it `extractRenderState(GuiGraphicsExtractor, DeltaTracker)`**, so the selector resolved to nothing on both 26 nodes — `InvalidInjectionException` at mixin apply, i.e. a client that never reaches the title screen. The 30-node build was green, `verify_mixins.py` was green, and the compiler had nothing to say; **`verify_mixin_targets.py` is what failed**, with `no method named 'render' declared in net/minecraft/client/gui/Gui`.

The fix was not to re-point the mixin. `GuiMixin` only ever existed because **Forge 51 dropped
`RenderGuiOverlayEvent` in 1.21 and never replaced it** — and Forge 64 brings the layer registry
back. So the mixin closes at 26 (`forge && >=1.21 && <26`) and the overlay goes back through the
event. That is the **fifth** distinct fatal-mixin mode this project has hit, and the third one found
by a verifier rather than by a player.


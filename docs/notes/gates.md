# The pre-publish gate

> Read before publishing, and after any wave. Includes what the gate structurally cannot see.
>
> Part of the Alex's Mobs Continued porting notes.

### The full pre-publish gate

Both halves, in one place. Neither alone is sufficient — the server gate cannot see a client mixin, and
the client gate does not tick a world:

```bash
./gradlew <all node :build tasks> --continue      # ONE invocation; MOD_IS_RELEASE=true for upload jars
python3 scripts/verify_mixins.py                  # declared classes ship, and none targets our own code
python3 scripts/verify_mixin_targets.py           # every injector SELECTOR resolves in the MC jar
python3 scripts/verify_convention_tags.py         # every `#c:` a Fabric node reads is defined somewhere
scripts/bootgate.sh  <all nodes>                  # dedicated server: Done ( + 45 s soak, no crash-reports
scripts/clientgate.sh <all nodes>                 # real client: reaches "Sound engine started"
```

⚠️ **In both gates the crash-report check — not the ready marker — is what has teeth.** Read each script's
exit code, never its `READY`/`DONE` lines. Milestone 9's `playBidirectional` crash fires *after*
`Sound engine started`, so **all 22 nodes printed `READY` while two of them were dying**; the run was only
red because `clientgate.sh` also lists `run/crash-reports/`. Milestone 8's `tempt_range` crash had the same
shape one layer in — a few seconds after `Done (`. A marker means "got this far", never "survived".

`scripts/clientgate.sh` logs to `build/clientgate/`, `scripts/bootgate.sh` to `build/bootgate/`. Both
return nonzero if any node fails, and both stash pre-existing crash reports so a stale one can't be
mistaken for a new one.

## `clientgate_par.sh` — run the client gate in parallel

`scripts/clientgate_par.sh <nodes…>` (env `JOBS`, default 4; `SOAK`, default 15) is a drop-in
parallel sibling of `clientgate.sh`, writing the same `build/clientgate/cgate-<node>.log` files and
applying the same verdict rules. A full 49-node sweep takes ~15 min at `JOBS=5` against well over an
hour sequentially.

⚠️ **The active node is run alone, first, and must stay that way.** Rule 1 ("multi-node Gradle must
be ONE invocation") exists because separate `./gradlew` calls collide on Stonecutter's active-version
state — but that collision is specific to the **active** node, whose sources live in the root
`src/`. Every other node is projected into its own `versions/<node>/src` and shares no mutable state
(`stonecutter.gradle.kts:474`), which is what makes concurrency safe at all. The script reads
`stonecutter active` out of `stonecutter.gradle.kts` and batches it separately; do not "simplify"
that away.

**Sizing it — `JOBS=12` is the recommended default, and going higher is not worth much.** Measured
2026-08-01 on a 24-core / 62 GiB / RTX 3080 box:

| `JOBS` | RAM per node | throughput | full 49-node sweep |
|---|---|---|---|
| 4–5 | ~2.2 GiB | ~24 s/node | ~25 min |
| 12 | ~1.75 GiB | ~17.9 s/node | **891 s** |

**3× the concurrency bought ~1.35× the throughput.** Neither obvious resource is the limit: peak
VRAM was 2.2 of 10.2 GiB (these are title-screen boots, not gameplay), and RAM per node *falls* as
`JOBS` rises because Gradle daemons get shared rather than duplicated — 12 concurrent nodes peaked
at 36 GiB against a 15 GiB baseline, so 16 would still fit in ~43 GiB. The actual limit is the
file-locked shared caches under `~/.gradle`, which serialise the *configuration* phases no matter
how many jobs are asked for; only the forked client JVMs are genuinely parallel. Raising `JOBS`
past 12 buys single-digit percentages, so do it only if the box is otherwise idle.

⚠️ **Do not read the aggregate log's `Sound engine started` count as a node count.** `report()` pipes
its grep through `head -30`, so on a node that logs a lot of `/ERROR]` noise the boot marker is
pushed out of the *summary* while still being in the per-node log. A 2026-08-01 sweep showed 44
READY against 35 markers and all 44 were genuinely up. To audit by hand, grep the per-node
`build/clientgate/cgate-<node>.log` files, not the aggregate — same lesson as the exit-code rule
above, one level down.

Since Milestone 13, `bootgate.sh` **also `rc=1`s on any surviving non-benign ERROR / `Couldn't load tag` /
`Couldn't parse data file` / `Parsing error` / `InvalidMixin` line**, filtered against the benign patterns
(`RuntimeDistCleaner|NeoForgeDevDistCleaner|OnlyInWarningsHandler|RealmsClient`, plus `No data fixer
registered for` since Milestone 15). It used to merely print them, through a `grep -v RuntimeDistCleaner`
that stopped covering NeoForge at 1.21.7 — so a data-pack break, which is **logged and not thrown**, passed
the gate with a green `rc=0`. That is exactly what the Forge-26 `c:` tag fault did (see Milestone 13). Read
the exit code, and if you ever audit a log by hand use the current filter, not the old one.

**The fifth benign pattern, and why it is safe to add one.** `No data fixer registered for <type>` is
vanilla's own dev-only DFU schema check (`Util#fetchChoiceType`, gated on
`SharedConstants.CHECK_DATA_FIXER_SCHEMA`), emitted once per registered type the vanilla schema does not
know — i.e. once per **modded** type, **116 lines on `1.20.1-fabric`**. It shows up only on the obfuscated
Fabric nodes because Forge and NeoForge patch that method to skip non-vanilla namespaces and 26.x stopped
logging it. It admits the one argument that should be required before widening this filter: it **cannot**
indicate an AMC fault, because this mod ships no datafixers on *any* loader — the Forge nodes are in the
identical state and merely quieter. Anything that could differ between loaders does **not** qualify.

**`scripts/verify_mixin_targets.py`** (added 2026-07-26) closes the *third* fatal-mixin mode — the one the
other two steps structurally cannot see. `verify_mixins.py` proves a declared mixin **class** ships;
nothing proved that its `@Inject`/`@Redirect` **selector** matches anything, and an injector with zero
matches is `InvalidInjectionException: Critical injection failure` at mixin apply, exactly as fatal. Per
shipped jar it checks that (1) the `@Mixin` target class exists, (2) every `method = "…"` selector resolves
to a method **declared** in that class (name, plus descriptor when the selector pins one), and (3) every
`@At(INVOKE|FIELD|…, target = "…")` reference actually appears in the bytecode of a matching host method.

Mapping is auto-detected from the jar's own annotations, **three ways**, and the target jar picked
accordingly:

| detected | target jar |
|---|---|
| SRG (`m_109599_`) — only `1.20.1-forge`, `1.20.4-forge` | loom's `~/.gradle/caches/fabric-loom/<mc>/forge/*/minecraft-merged-srg-at-patched.jar` |
| intermediary (`class_1309`) — the 15 obfuscated Fabric nodes | `~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-intermediary/<mc>-*/*.jar` |
| Mojmap — everything else | `versions/<mc>-neoforge/build/moddev/artifacts/neoforge-*-merged.jar` (Mojmap vanilla is identical on both loaders) |

It therefore needs those artifacts on disk — build first. Pass explicit jar paths to check a downloaded
release instead of the local build.

⚠️ **~~Build the gate's jars with `MOD_IS_RELEASE=true`~~ — this rule was the wrong fix, and it failed.**
The hazard was real: a dev jar is `alexsmobs-<ver>-<loader>+<mc>-SNAPSHOT.jar`, both scripts globbed
`alexsmobs-*-<loader>+<mc>.jar` — which does not match — so they fell through to whatever release-named
jar was still sitting in `build/libs` from a previous session and reported a perfectly green
`problems=0` about code that no longer existed. No warning: the jar counts (`jars=49`) come out right,
because every node still *has* a jar. Caught in Milestone 15 Wave 2 only because the new mixins'
selectors were missing from the per-node count (`7 selectors` on a node that should have had 17).

**But making it a habit instead of a code change did not hold.** `verify_mixins.py` was fixed to match
both names; `verify_mixin_targets.py` was not, and stayed load-bearing on someone remembering the env
var. **Wave 4 built without it and reopened the identical hole** — same script, same silent green, five
days later. Both scripts now glob `…+<mc>*.jar`, and `verify_mixin_targets.py` additionally **names any
node that yielded no jar and exits non-zero**, so coverage going to zero can no longer look like
success. Full account in [`mixins.md`](mixins.md).

`MOD_IS_RELEASE=true` is still correct for **publishing**; it is no longer a verification control, and
nothing should depend on it being remembered. Independently of all this: if a node's numbers look
unchanged after adding an injector, check the jar's timestamp before believing it.

The same glob is why `--all-versions` reports failures that are not yours: it happily checks every stale
`1.0.2`/`1.0.6` jar left in `build/libs`, and those legitimately fail against today's MC jars. Use the
default (current `mod.version`) mode to gate, and `--all-versions` only when auditing history.

**Verified to have teeth**: run against the published `1.0.2`/`1.0.3` jars it reproduces the historical
crash exactly — `1.20.4-forge` passes, `1.20.6-forge` and both `1.21.1` jars fail with
`'renderLevel(…PoseStack;FJZ…)V' not found in net/minecraft/client/renderer/LevelRenderer (present: …)`.
Expected shape today (after Milestone 15 Wave 5): **`nodes=49 jars=49 selectors=958 problems=0
skipped=0`**. **Read the selector count, not just `problems=0`** — a mixin whose whole source file
stopped being compiled on a node drops silently out of the config, and a zero-problem run over fewer
selectors than expected is exactly what that looks like.

⚠️ **Predict and compare the count PER NODE, not the total** (`--verbose` prints `ok (N selectors)`
under each jar). The total is not comparable across the 2026-08-01 SNAPSHOT-glob fix at
`verify_mixin_targets.py:504` — before it the script silently re-validated stale release-named jars,
so every historical total in this file (`168` at 18 nodes, `431` before Wave 2 added `mixin/fabric/**`,
`637` before Wave 3b-1, `671`, Wave 4's `756`) was measured over a different jar set than today's.
Per-node counts survive that. The current Fabric shape is `38` on 1.20.1, `37` on 1.20.4, `38` on
1.20.6/1.21/1.21.1, `40` on 1.21.2 → 1.21.11, and `39` on 26.1.2/26.2; Forge/NeoForge run 6–11.

Two smaller things this wave needed: the two new nodes wanted a `run/eula.txt` with `eula=true` (MDG/loom
write `false` on first `runServer`), and four established NeoForge nodes turned out never to have had one.

**`scripts/verify_convention_tags.py`** (added 2026-07-31, Milestone 15) is the static half of the
`Couldn't load tag` check, for **Fabric only**. On Forge/NeoForge the loader defines every convention tag,
so a `#c:` reference always resolves; on Fabric they come from an *optional* Fabric API module whose
contents grew from 156 tags (v1, all that exists below 1.20.6) to 500+ (v2) over a year of releases, and
**non-monotonically across the pins** — `1.21.1`'s late-backport pin is complete while `1.21.2`/`1.21.3`
are not. The script diffs every Fabric node's `#c:` tag references against that node's pinned fabric-api
jar plus the mod's own `data/c/tags/**`, straight off `versions/<node>/build/resources/main`. Expected
shape: **`nodes=17 problems=0`**. It found seven undefined tags on six nodes — `c:sands` alone empties
`alexsmobs:am_spawns` and the fifteen `*_spawns` tags — where the boot gate had only ever seen two of
those nodes. Details and the fix in [`fabric.md`](fabric.md).

**`Failed to load properties from file: server.properties` was a first-run artifact, and is fixed by
*seeding*, not by widening the benign list.** A never-booted node has no `run/server.properties`, so
vanilla logs it at ERROR and only then writes the file — every node's first run was red for a reason that
had nothing to do with the mod. `bootgate.sh` now seeds an empty one (vanilla defaults every absent key)
exactly as it already seeds `eula.txt`, and never overwrites an existing file. Filtering the message
instead would have gone silent the day a `server.properties` genuinely failed to parse; **prefer removing
the cause to extending `benign`.**

#### ⚠️ NEVER edit root `src/` while a multi-node gate is running

A gate walks 49 nodes over tens of minutes, and every node's task re-runs `stonecutterGenerate`,
which re-projects root `src/` into `versions/<node>/src/` **at the moment that node's turn comes
up**. Edit a shared file half-way through and the tree splits in two: the nodes already past the
projection carry the old source, the ones still queued carry the new. The verdict then names nodes,
not the edit — `26.1.2-fabric : FAILED` / `26.2-fabric : FAILED` while the fifteen earlier Fabric
nodes said `DONE` reads exactly like a genuine high-version regression in whatever is being gated.

Cost this once during Wave 3b (2026-07-31): a syntactically broken `>=26` Stonecutter arm was written
into `FabricClientEvents` while the Fabric boot gate ran, and the two failures looked like a 3b-1
regression until the log showed `compileJava FAILED` with `illegal character: '—'` on a line of
English prose. The two real bugs are in [`stonecutter.md`](stonecutter.md); the meta-lesson is that
the gate had told the truth about something that had not existed when it started.

**The habit: while a gate is in flight, `docs/**` and `scripts/**` are fair game, `src/**` and
`build-logic/**` are frozen.** If an edit cannot wait, kill the gate and restart it — a partial gate
is worth less than no gate, because it produces a red verdict you will spend an hour explaining.

#### The gate's remaining hole: neither half ever CONSTRUCTS most of the mod's entities

Found 2026-07-26 while testing the community bug reports. `bootgate.sh` boots an empty world and idles;
`clientgate.sh` stops at the title screen. Between them they exercise registration, mixin apply, data-pack
parsing and world tick — but they never instantiate ~116 of the 117 entity types and never fire most of
the mod's ~63 event handlers. **Two hard crashes lived behind that hole**, both invisible to a fully green
18-node gate, and both plausible causes of the 1.0.6/1.0.7 player reports:

1. **`ProjectileImpactEvent#setCanceled` — hard server crash on every Forge node ≥1.20.4.** Forge's
   `ProjectileImpactEvent` **stopped being `@Cancelable` at 1.20.4** (bytecode-verified: the
   `eventbus/api/Cancelable` annotation is present only in forge 1.20.1's copy; 1.20.4 → 1.21.5 all lack
   it, and every version from 1.20.1 up already has `ImpactResult` + `setImpactResult`). `setCanceled`
   still **compiles** — it is inherited from `Event` — and throws `UnsupportedOperationException` at
   *runtime*, which surfaces as `ReportedException: Ticking entity` and kills the server the first time
   anything is shot at an **emu**. `ServerEvents.onProjectileHit` gated its `setImpactResult` branch at
   `forge && >=1.21.6` (it was written during the EventBus-7 wave, and EB7 was assumed to be where
   cancellation went away). Corrected to **`forge && >=1.20.4`** at both sites. NeoForge is unaffected —
   its `ProjectileImpactEvent` `implements ICancellableEvent` on every node (checked 20.6 → 21.6).
   > The whole class of bug is "a Forge event quietly lost `@Cancelable`". Audited every `setCanceled`
   > site in the tree against every Forge node's merged jar — `EntityStruckByLightningEvent`,
   > `PlayerInteractEvent.{EntityInteract,RightClickBlock}`, `LivingDamageEvent`,
   > `LivingChangeTargetEvent`, `ViewportEvent.RenderFog`, `RenderLivingEvent.Pre`, `RenderHandEvent` —
   > and `ProjectileImpactEvent` is the **only** one affected. Re-run that audit when adding a node.
2. **`EntityTossedItem` cannot be constructed at all on ≥1.20.5.** From 1.20.5,
   `ThrowableItemProjectile#defineSynchedData` seeds `DATA_ITEM_STACK` with
   `new ItemStack(this.getDefaultItem())` instead of `ItemStack.EMPTY`, so `getDefaultItem()` — and
   therefore this class's `isDart()` — runs *from inside* `Entity`'s constructor. The 1.20.5 synched-data
   **builder** rewrite means `this.entityData` is not assigned until `defineSynchedData` returns, so
   `isDart()` NPE'd: `/summon alexsmobs:tossed_item` answered "Unable to summon entity", and the
   **capuchin monkey's throw AI NPE'd out** (`EntityCapuchinMonkey:242`, also `AMItemRegistry:415`).
   `isDart()` is now null-guarded (nothing is a dart at construction time — `setDart` is always called
   afterwards — so `false` is also the *correct* early answer). The related `getItem()` override that
   restores the dynamic dart/cobblestone item had its gate moved **`>=1.21.2` → `>=1.20.5`** for the same
   reason, so darts stop rendering as cobblestone on 1.20.6/1.21/1.21.1 too.

The reproducer is `scratchpad/runtest3.sh` (kept in the session scratchpad): a **Forge** node's loom
`runServer` consumes piped stdin, so a FIFO on fd 3 drives real console commands — `forceload add`, `fill`
a platform, `summon` every id from `AMEntityRegistry`, fire arrows/snowballs at an emu, then soak. NeoForge's
MDG `runServer` does **not** consume piped stdin, so this technique is Forge-only; everything it probes is
loader-neutral except the Forge-only event code, which is where the risk concentrates anyway.
**Worth promoting into `scripts/entitygate.sh` as a fourth gate step** — over RCON, so it covers NeoForge too.

#### Driving a real dev client (for reports that need a player)

`bootgate.sh` idles an empty world and `clientgate.sh` stops at the title screen, so neither can test a
player-facing behaviour. The rig that worked, on a NeoForge node (MDG `runServer` ignores piped stdin, so
**RCON** is the only console channel):

> 💡 **First ask whether it needs a *player* or only a *client*.** "Player-facing" is not the same as
> "needs a GPU", and the difference is a 25-minute client launch on the user's desktop versus a
> headless RCON script. Anything whose state lives on the server and round-trips through NBT can be
> driven by a summoned mob and read with `/data get`: #44's elytra glide looked like it needed a
> player pressing jump, and turned out to be a zombie plus `{FallFlying:1b}` (see
> [`bug-reports.md`](bug-reports.md) #44). Only *rendering* is genuinely GPU-bound. The mistake is
> cheap to make in the other direction too — RCON cannot open a screen or read a pixel, so do not
> try to prove a render fix headlessly.

- `server.properties`: `enable-rcon=true`, `rcon.port=25575`, `rcon.password=…`; drive it from a small
  `rcon.py`. Forge nodes can use piped stdin or a FIFO on fd 3 instead.
- Launch the client into the server directly with `AM_CLIENT_ARGS="--quickPlayMultiplayer 127.0.0.1:25565"`.
  The hook is in **all four** buildscripts' client runs now — `build.forgeg`, `build.neoforge`, and (added
  2026-08-08) `build.fabric` + `build.fabricnr`. Loom's spelling is `programArg(it)`, one arg per call, not
  `programArgs`. Before that a Fabric node silently ignored the variable and sat on the title screen; the
  proof it took is a `Connecting to 127.0.0.1, 25565` line in the client log.
- **`ydotool` mouse movement is broken on this KWin/Wayland session** (the pointer pins at 1,1). Only
  **keyboard** injection works, so rebind everything you need in `run/options.txt`
  (`key_key.use:key.keyboard.v`, `key_key.attack:key.keyboard.b`) and set `pauseOnLostFocus:false`.
  `options.txt` is read at client start and rewritten on exit — SIGKILL the client before editing it.
- 🛑 **Therefore `ydotool click` is worse than useless — it *un*-focuses the game.** The click is
  delivered at the pointer's real position, which is the pinned (1,1) screen corner, so it lands on
  whatever window happens to be in that corner, activates *it*, and Minecraft loses focus. The next
  keystroke then goes to the user's window. Diagnosed 2026-08-08 after two identical sequences:
  `focus.sh` OK → `ydotool click 0xC1` → 2 s later active window is a Dolphin. **Never click; always
  rebind to a key.** A run dir set up in an earlier session is not proof the rebinds are there — this
  node's `options.txt` still had `key_key.use:key.mouse.right`, so check before launching rather
  than after (it costs a full relaunch to fix).
- 💡 **No mouse means any screen reached by *clicking* is out of reach — so route around the screen,
  not the click.** Two that came up verifying the animated icons (#45/#48) on 2026-08-08, both of
  which look GPU-and-mouse-bound and are not:
  - **Creative-tab icon.** Paging the tab row needs a click, but the icon is an ordinary item:
    `/item replace entity @a hotbar.0 with alexsmobs:tab_icon` puts it in the hotbar, where it goes
    through the very same special-model renderer. Three F2 shots a few seconds apart holding three
    different mobs *is* the animation.
  - **Advancement icons.** `L` opens the screen but selecting a mod's tab needs a click. Granting
    the advancement draws its icon in the **toast** instead, same render path:
    `/advancement revoke @a everything` then `grant @a only <id>` for a few.

  The general move: find the *other* place the game already draws the thing, and reach it over RCON.
- Aim with `/tp <player> x y z <yaw> <pitch>` (yaw −90 = +X, 0 = +Z, 180 = −Z; positive pitch looks down)
  and confirm with the F3 overlay's `Targeted Entity` line.
- **The client shares the user's real desktop.** Blind injection lands on whatever window is stacked on
  top — Discord and a Godot editor here. **Always** activate the game window first and verify it took
  before sending anything.
- 🛑 **Match the window by PID, never by caption — and re-check focus before *every* burst.** Cost most
  of a session on 2026-08-08. Sibling projects in this workspace each run their own dev client, and
  three were open at once (`OneDimension` on `1.21.11-fabric`, `ActualPortals` on `1.21.1-fabric`, plus
  this one): a `caption.indexOf("Minecraft") === 0` loop activates the **last** match, so the
  keystrokes went into another project's game. Even the MC version does not disambiguate — two clients
  were both `1.21.11`. `workspace.windowList()[i].pid` does, against the pid from
  `ps -ef | grep dli.env=client`. Focus is also **not sticky**: a client launching elsewhere steals it,
  and so does the user — the run that finally explained the silence found the active window was a
  **YouTube tab in Brave**. So a focus call that succeeded two minutes ago proves nothing now; probe
  `w.active` immediately before each burst, and if the active window is not yours, **stop** rather than
  spray input into someone else's session.
- ⚠️ **The user may be at the keyboard.** They took over the `1.21.8-forge` client mid-session on
  2026-08-08 — disconnected from the test server, joined their own, and ran an elephant test — which
  showed up first as a screenshot that made no sense (a creative menu with a half-typed search). Read
  the client log's `[CHAT]`/`Connecting to` lines before concluding the game is misbehaving; a human
  driving it looks exactly like a bug.
- ⚠️ **`ydotool` needs `YDOTOOL_SOCKET=/run/user/1000/.ydotool_socket`.** Its compiled-in default is
  `/tmp/.ydotool_socket`, which does not exist on this machine, so every call fails with exit 2. The
  daemon *is* running (`ydotool.service`); a bare `pgrep ydotoold` is not enough to conclude input works.
- **Activating a window: KWin scripting, not EWMH.** This is a KDE **Wayland** session and
  `xdotool`/`wmctrl`/`kdotool` are all absent, so there is no `_NET_ACTIVE_WINDOW` to check. Load a JS
  file into KWin and read its `print()` output back from the journal:

      qdbus org.kde.KWin /Scripting org.kde.kwin.Scripting.loadScript /path/focus.js tag   # -> script id
      qdbus org.kde.KWin /Scripting/Script<id> org.kde.kwin.Script.run
      journalctl --user -u plasma-kwin_wayland -n 30 --no-pager

  In the script, iterate `workspace.windowList()` and set `workspace.activeWindow = w`. The dev client's
  `caption` is `Minecraft* <mc> - Singleplayer`. Re-run a probe script afterwards and check `w.active` —
  setting `activeWindow` does **not** guarantee it stuck.
- **Verify focus with F2, not with a screenshot tool.** `grim` fails outright here ("compositor doesn't
  support the screen capture protocol" — KWin exposes the portal, not wlr-screencopy). Minecraft's own
  F2 (keycode 60) writes to `versions/<node>/run/screenshots/`, which both captures the frame *and* is a
  perfect focus probe: a file appears iff the keystroke reached the game. Use it before typing anything.
- 🛑 **A locked screen silently eats every keystroke — check it first.** `LockedHint=yes` from
  `loginctl show-session <id> -p LockedHint` (or `qdbus org.kde.screensaver /ScreenSaver GetActive`)
  means the lock screen holds input and the game will never see it. The failure looks exactly like
  "ydotool is broken": commands exit 0, the log shows nothing, no screenshot appears. Cost a round of
  blind injection on 2026-08-04, one line of which went into the **password field** as a failed unlock.
  **Never work around the lock** — stop and ask the user to unlock.
- `--quickPlaySingleplayer '<save name>'` via `./gradlew :<node>:runClient --args=…` drops straight into
  an existing save and skips all title-screen navigation, which is otherwise unreachable without mouse
  injection. Saves already exist on `1.21.1-fabric`, `26.2-fabric` and six other nodes (`New World`).
- `/ride X mount <player>` is rejected by vanilla ("Players can't be ridden"), so shoulder mounts have to
  go through the mod's own AI or `mobInteract`. Don't `tail` RCON output past the command you care about
  or you will miss that kind of rejection.
- `summon <mob> … {Owner:[I;a,b,c,d]}` yields a **tamed** mob: `TamableAnimal.readAdditionalSaveData`
  calls `setTame(true)` whenever an `Owner` tag is present.
- Worth promoting into `scripts/entitygate.sh` as a fourth gate step.

#### Testing **AI** headlessly over RCON — no client at all

Built 2026-08-03 for the taming report (#19), on `26.2-fabric`, and it settles a behaviour question in
minutes rather than needing the user's GPU. `rcon.py` + a per-question driver script; the whole rig is
`summon`, `data get entity`, `damage … by`, and sleeps. Five traps, each of which cost a run:

- **`pause-when-empty-seconds` freezes the tick loop with no players.** The default is 60, so a
  dedicated server with nobody connected stops ticking a minute in and **every mob stands still** — which
  reads exactly like "the AI is broken". Set it to `0` and restart. Confirm with `time query gametime`
  twice; if the number does not move, nothing you observe means anything.
- **The console command source sits at (0, 0, 0)**, not world spawn, so bare-coordinate `summon`s land in
  unloaded chunks. `forceload add -20 -20 40 20` and build the arena explicitly.
- **Always `Tags:["…"]` your subjects** and select with `@e[tag=…,limit=1]`. Reusing a literal UUID gets
  *"Unable to summon entity due to duplicate UUIDs"* from a stray unloaded copy of an earlier run.
- **Run a vanilla control.** A wolf with the same `Owner` proved the follow harness itself was sound
  before any conclusion was drawn about the raccoon.
- **Random gates need a sample size, not a verdict.** Raccoon taming is a **30%** roll per washed egg;
  13 straight failures looked conclusive and were not. Read the source for the probability *first*, then
  size the trial.

What it *cannot* answer, so don't over-claim from a green run: the owner is a villager rather than a
`Player`, no right-click/`mobInteract` path is exercised, and nothing renders.


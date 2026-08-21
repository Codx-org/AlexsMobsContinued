# Publishing to Modrinth and CurseForge

> Read before uploading. Covers all three loaders since `2.0.0`; the Fabric dependency is
> version-pinned and the uploader used to skip Fabric silently.
>
> ⚠️ **Modrinth only, from `2.0.4` on.** User decision, 2026-08-04: *"we don't upload to curseforge,
> not this mod."* Do **not** offer or run the CurseForge upload for this project again unless the user
> reopens it. The CurseForge section below is kept because the project already exists and holds
> `2.0.1` + `2.0.2` (49 files each, uploaded 2026-08-01 before this decision) — so it is now a
> **frozen, permanently out-of-date mirror**, not a target. `2.0.3` never reached it and `2.0.4` will
> not either. Nothing needs backfilling; that is the point of the decision.
>
> The `2.0.3`-restating "also in this release" section that changelog carried was written while
> CurseForge was still a target; the **fifth** pass rewrote the changelog and dropped it, since a
> Modrinth player updating from `2.0.3` has already seen those fixes. `2.0.4`'s "also in this release"
> now covers only its own fourth-pass fixes (#22, #23).
>
> Historically **two stores from `2.0.1` to `2.0.2`**: Modrinth first (below), CurseForge second
> ([its own section](#publishing-to-curseforge)). They were separate uploads with separate
> scripts, separate tokens and — importantly — *different dependency models*.
>
> Part of the Alex's Mobs Continued porting notes.

### Publishing

Modrinth project **`kYIaHHfw`** = slug **`alexs-mobs-continued`**. The slug **used to be** shared with
the sibling `AlexsMobsFP` repo, which published the Fabric line to it (`amup-fabric-<mc>-<ver>.jar`,
versions 1.0.0/1.0.1/1.0.4/1.0.5 are theirs). **Since `2.0.0` this repo publishes all three loaders**
and owns the numbering; leave the four pre-2.0.0 Fabric rows alone. Upload with
**`scripts/modrinth_upload.py`** (`--check` / `--list` / `--only <key>` / `--force`; token at
`scripts/.mr_token` — falls back to `../OneBlock/scripts/.mr_token` — raw in the `Authorization`
header, **no `Bearer` prefix**). `version_number` is `<ver>+<mc>-<loader>`.

**Dependencies are not uniform across loaders, and one of them must be version-pinned:**

| dep | shape | on |
|---|---|---|
| **CodxLib `6oyMM4yX`** | **project-level** (`version_id: null`) | every node |
| **Fabric API `P7dR8mSH`** | **version-PINNED** to the node's `deps.fabric-api` | Fabric nodes only |

⚠️ **The Fabric API one cannot be project-level.** Each Fabric jar's own `fabric.mod.json` declares
`fabric-api >= <that node's pin>` (`fabricLikeVersionRange` in `build.fabric{,nr}.gradle.kts`), the
releases are per-MC-version, and the pins are **non-monotonic** across the range — a project-level entry
would let an installer fetch a build the jar then refuses to load, which is the same class of failure as
the `fabric-loader` floor bug recorded in the workspace porting notes. `dependencies()` resolves the pin
against `/v2/project/P7dR8mSH/version` by `version_number` and **`SystemExit`s rather than uploading a
Fabric jar without it**. `--list` prints the resolved id per Fabric node — eyeball it before a run.

> The CodxLib dep is **deliberate, not a bug** — nothing in `src/` links codxlib, but the author uses
> `/codxlib help` and `/codxlib versions` to support players, and requiring it guarantees it is installed.
> Don't "fix" it by dropping the dependency. (Consequence: this mod never `register(modInfo())`s, so it
> won't appear in `/codxlib versions` until it does.) Re-checked 2026-08-01 against
> `/v2/project/6oyMM4yX`: its `game_versions` run **1.20.1 → 26.2 on all three loaders**, so it covers
> every one of the 49 nodes and no node has a dependency blocker.

#### The project page body goes stale on its own — check it every release

`PATCH /v2/project/kYIaHHfw` with `{"body": "…"}` replaces the main page markdown in place and touches
nothing else (verified 2026-08-01 — title, `description`, categories, license, links and side-flags all
survived). The **short summary** shown in search results is the separate `description` field, max 256
chars.

⚠️ **It is not generated from anything, so no gate and no verifier can catch it drifting.** Through the
whole 2.0.0 release the body still read *"brought to Minecraft 26.1.2 … on a modern Fabric setup"* — one
MC version and one loader, while the project had just gone to 49 builds across 1.20.1 → 26.2 on three
loaders. Rewritten 2026-08-01 (2095 → 1663 chars) to lead with the version/loader range and to state the
install requirements — **CodxLib, Fabric API on Fabric, and that Citadel is bundled and must not be
installed separately** — none of which the old body mentioned at all, despite being the three things
that produce "it won't launch" reports.

**So: any release that changes the loader or MC-version coverage changes the body too.** The changelog
is per-version and the body is project-wide; updating one is not updating the other.

#### 1.20.1 stays published — decided, don't re-propose

Upstream Alex's Mobs `1.22.9` is **MC 1.20.1 / Forge only**, so exactly one node overlaps it:
**`1.20.1-forge`**. `1.20.1-fabric` does not (upstream never shipped Fabric) and **there is no
NeoForge 1.20.1 build at all** — that node was never in the tree (legacy toolchain, see
`settings.gradle.kts`). So "omit Forge and NeoForge 1.20.1" is a one-file question, not four.

Weighed 2026-08-01 and the answer was **keep, unchanged**:

- 1.20.1 Forge is **84 downloads of ~9,100 project-wide (<1%)**, 82 of them on 1.0.8. The audience is
  overwhelmingly Fabric on 26.x (`fabric=7670` vs `neoforge=899`, `forge=528`).
- It is not the same artifact as upstream's: it bundles Citadel, and it fixes **three faults that are
  upstream's own and still live in the original** — the orca NPE, the sugar-glider forage crash (fires
  every tick, so it re-crashes on return) and the centipede head. Pulling it leaves a crashing 1.20.1
  player with nowhere to go.
- `1.20.1-forge` is the **active Stonecutter node** either way — root `src/` is its projection. This was
  only ever a publishing-filter decision, never a tree change.

If it is ever revisited, the non-destructive middle path is a per-node
`versions/1.20.1-forge/build/libs/modrinth-changelog.md` crediting the original as the canonical 1.20.1
release — not deleting live versions (irreversible, ~15 s each, breaks pinned modpacks).

#### ⚠️ A release build needs `MOD_IS_RELEASE=true`, and forgetting it costs the whole build

`build-logic/src/main/kotlin/Context.kt` derives `isSnapshot` as `!project.envTrue("MOD_IS_RELEASE")`
and appends `-SNAPSHOT` to `fullVersion` when it is true. So a plain

```bash
bash -c './gradlew :a:build :b:build … --continue --max-workers=4'
```

produces 147 jars all named `alexsmobs-<ver>-<loader>+<mc>-SNAPSHOT.jar` — which **both** uploaders
reject, because they construct the exact expected filename and hard-error on a miss (that strictness
is deliberate; see the workspace porting notes on `1.20.1-SNAPSHOT` being parsed as a *game version* by
a looser regex). Nothing warns you during the build; the tell is the filename. Set it on the wrapper,
not inside it:

```bash
MOD_IS_RELEASE=true bash -c './gradlew … --continue --max-workers=4'
```

Cost a full 49-node build on `2.1.2`. **Check one filename in `build/libs` before starting the
upload** — `ls versions/26.2-fabric/build/libs/` is a two-second guard on a seven-minute mistake.

#### ⚠️ The uploader's jar regex is a COVERAGE filter, and it fails green

`JAR_RE` and `LOADER_LABEL` matched only `(forge|neoforge)` until 2026-08-01. Running that against the
49-node tree would have uploaded **32 versions, skipped all 17 Fabric ones, and printed
`uploaded=32 … failed=0`** — a two-thirds release reported as a complete one. Nothing downstream would
have caught it either: the per-version verification pass only looks at versions that *were* created.

**So predict the number before every run and check `--list` prints it.** `--list` is the cheap guard —
it enumerates exactly what the upload loop will walk, and since 2.0.0 it also prints the resolved
Fabric API pin per Fabric node. This is the publishing-side twin of the mixin-verifier rule in
[`gates.md`](gates.md): *a script that never opened your artifacts still prints a green summary.*

#### The changelog is one file, shipped verbatim to every version page — and it IS repairable

`modrinth-changelog.md` at the repo root is uploaded as-is to all 49 versions (a per-node
`versions/<node>/build/libs/modrinth-changelog.md` overrides it, which is how OneBlock states a
per-node Fabric Loader minimum; this repo does not use that yet). Two consequences:

- **Keep it to the CURRENT release only.** It had grown into a running history file, so the 2.0.0 pages
  each shipped with 1.0.8, 1.0.3 and 1.0.2 stacked underneath. Old entries live in git history and on
  their own version pages; they do not need to be in the file.
- **A wrong changelog is NOT a re-upload.** `PATCH /v2/version/{id}` with `{"changelog": "…"}` replaces
  it in place, and **leaves `files`, `loaders`, `game_versions` and `dependencies` untouched** — verified
  by re-reading all 49 after a full-project rewrite (2026-08-01). This is the opposite of CurseForge,
  where per-file relations are set at upload time and there is no endpoint to edit them afterwards.
  ⚠️ **A `204` is not proof the write landed.** Rewriting all 49 again on 2026-08-16 returned
  `patched=49 failed=0`, and a per-version re-read found **one** version (`2.0.16+1.21.11-forge`,
  `oCIVkvFW`) still serving the old body; an identical second PATCH took, also `204`. So verify a bulk
  metadata rewrite the same way an upload is verified — **fresh `GET /v2/version/{id}` per version**,
  not the project list endpoint (which is cached and, here, reported the same single stale row for an
  unrelated reason). The CurseForge half of a post-upload changelog fix is simply **not possible**:
  the upload API has no edit endpoint, so CF files keep whatever text they were uploaded with unless
  someone edits them in the web UI (the CF changelog, unlike CF relations, *is* editable there).

⚠️ **Sanity-check the numbers in the changelog against what was actually PUBLISHED, not against the
node tree.** The 2.0.0 text went live claiming the 1.21.4 item-model break hit "20 of the previous
release's builds". 20 was the count of *nodes ≥1.21.4 in the tree*; 1.0.8 published **18** files, of
which **6** were 1.21.4 or newer. Tree counts and release counts are different numbers and the gate
checks neither.

Release history to date: **1.0.2** (8 jars), **1.0.3** (5), **1.0.6** (18, **deleted by the author** — it
crashed clients), **1.0.7** (18, uploaded 2026-07-26 and **deleted the same day — the author reported it
was still bad**; all 18 removed, `still_live=0`), **1.0.8** (18, uploaded 2026-07-26 — the seven community
bug reports plus the two runtime crashes found behind the gate's entity-construction hole), **2.0.0**
(**49**, uploaded 2026-08-01 — the whole Fabric line plus Milestones 9–15, the 1.21.4+ item-model break,
and the eleven fixes from the second community pass), **2.0.1** (**49**, uploaded 2026-08-01 — a single
fix, the 26.2 client-side `PartEntity` id crash; `uploaded=49 skipped=0 failed=0`, server-side count
re-verified at 49, and two versions spot-checked for file/loader/deps/changelog), **2.0.2** (**49**,
uploaded 2026-08-02 — one feature, the `/aac nameplates` client toggle; `uploaded=49 skipped=0
failed=0`, and the whole set re-read server-side afterwards: 49 live, loaders split 17/16/16, no version
with an empty `files`/`loaders`/`game_versions`, all 17 Fabric rows carrying **two** deps with the Fabric
API one version-pinned and every non-Fabric row exactly one; project total 191 versions). Build upload jars with `MOD_IS_RELEASE=true` or they
carry `-SNAPSHOT`. A multi-node upload **can partially fail silently** — verify the count server-side
afterwards, and remember `GET /v2/project/{id}/version` is **cached** and under-reports straight after a
bulk run; re-read `GET /v2/version/{id}` before concluding anything is broken.

**1.0.8 was verified server-side after upload** (all 18 live; each carries exactly one `game_versions`
entry, one loader, one 25 MB file and the required CodxLib dep, and the changelog rendered). Project total
went 26 → 44 live versions. It shipped through a **five**-step gate — the four in [`gates.md`](gates.md) plus
`scripts/verify_mixin_targets.py`, which is the first check that would have caught the fault that made
1.0.2/1.0.3/1.0.6 crash clients.

**Nothing on this project has ever been featured** (checked 2026-07-26: 26 live versions, zero featured),
so the download panel just serves newest-per-loader — which is why the stale-featured trap that bites
OneBlock does not apply here. The 13 live Forge/NeoForge rows are all 1.0.2/1.0.3, i.e. **every one above
MC 1.20.4 is a client-crasher**; the author's decision was to leave them and publish the fix alongside.

**1.0.7 passed the full four-step gate on all 18 nodes before upload — AND WAS STILL BAD** (2026-07-26).
Record it as a gate failure, not a gate success: four green steps are evidently still not sufficient.

| Step | Result |
|---|---|
| `./gradlew <18 × :build> --continue` (`MOD_IS_RELEASE=true`) | `BUILD SUCCESSFUL in 1m 13s` |
| `python3 scripts/verify_mixins.py` | `jars=18 problems=0` (declared=13/15, missing=0, self-targeted=0) |
| `scripts/clientgate.sh` × 18 | all 18 reach `Sound engine started`, `rc=0` (two batches) |
| `SOAK=45 scripts/bootgate.sh` × 18 | all 18 `DONE`, `rc=0`, **no crash-reports**, no non-benign log lines |

> ⚠️ **The known hole, and the leading hypothesis until the log says otherwise: `runClient` never loads
> the shipped jar.** Both gates are *dev* launches off the source classpath — they never exercise the
> remapped, packaged artifact a player installs. Anything that only exists in the built jar is therefore
> invisible to both: mixin annotation remapping (`remapJar` rewrites them in-place; there is deliberately
> no refmap), the `MixinConfigs` manifest attribute, resource/manifest packaging, and the jar being loaded
> as a mod rather than as a classpath. That is the same shape of blind spot as "runServer cannot see a
> client mixin", one layer out. **The gate needs a step that launches the actual jar in a real profile.**

---

# Publishing to CurseForge

CurseForge project **`1635121`** = slug **`alexs-mobs-continued`**, created in the **Mods** class
(confirmed: `type: Mods`, category `Mobs`). Upload with **`scripts/curseforge_upload.py`**
(`--check` / `--versions` / `--list` / `--only <key>` / `--force` / `--no-deps` / `--dep-slug`),
ported from OneBlock's. Token at `scripts/.cf_token` (header **`X-Api-Token`**, no `Bearer`; one
all-projects token, no scopes), numeric project id at `scripts/.cf_project_id`. All three are
gitignored, along with the ledger.

**First upload was `2.0.1`** (2026-08-01) — the project had zero files before it, so unlike Modrinth
there is no `2.0.0` generation to prune here. `uploaded=48 skipped=1 failed=0 unsupported=0`
(the skip is the single `--only` validation node uploaded first, file id `8555976`); file ids run
`8555976` and `8555987`–`8556053`.

**`2.0.2`** (2026-08-02) ran the same shape — `--only 26.2-fabric` first (file id `8560715`,
relations accepted), then the batch: `uploaded=48 skipped=1 failed=0 unsupported=0`. Offline check
against the ledger passed: 49 jars, 49 `2.0.2/…` entries, none missing, none extra, no duplicate file
ids, split 17 fabric / 16 forge / 16 neoforge.

### What differs from the Modrinth uploader — read before reusing either

| | Modrinth | CurseForge |
|---|---|---|
| version/file identity | `version_number` = `<ver>+<mc>-<loader>` | `displayName` only; no version string |
| MC + loader | strings (`"1.21.4"`, `"neoforge"`) | **numeric ids**, resolved at runtime |
| environment | n/a | **required** — `Client` 9638 + `Server` 9639 |
| dependency version | Fabric API is **version-pinned** | **no version field exists at all** |
| listing existing files | `GET /project/{id}/version` | **no endpoint** — local ledger |
| changelog part | must be a **file** part | must be `--form-string` |
| fixing it afterwards | `PATCH` the version | **impossible for relations** |

- **`gameVersions` are numeric ids resolved by version *type*, never by name.** A name like
  `1.20.2` exists under several types; picking whichever the API returns first is how codxlib hit
  `errorCode 1009 — belongs to an invalid dependency`. `version_index()` filters to the
  `minecraft-<digit>…` types. All 49 nodes resolve, **0 unsupported** — including `26.1.2` (16082)
  and `26.2` (16498), so no `MC_ALIASES` fudge like OneDimension needs.
- **A Mods-class project must also tag an environment** or the upload fails `errorCode 1021`, so
  every file sends four ids: `[mc, loader, 9638, 9639]`.

### ⚠️ Relations carry NO version — the Fabric API pin does not survive the crossing

Modrinth's Fabric nodes carry a **version-pinned** Fabric API dependency, and the uploader
`SystemExit`s rather than ship a Fabric jar without it. **None of that is expressible on
CurseForge.** A CF relation is `{slug, type}` — "requires Fabric API", full stop. There is no
version field in the API *or* the web UI, so don't go looking for one.

What this uploader sends: **`codxlib` on all 49 nodes, plus `fabric-api` on the 17 Fabric ones.**
The floor each Fabric jar actually needs is still enforced — by the loader reading the jar's own
`fabric.mod.json` — it is simply invisible on the store page.

> Consequence for the CF **project page body**: it currently reads *"On Fabric you also need Fabric
> API — each Fabric file lists the exact build it wants."* That sentence is true of the Modrinth
> listing and **false of this one**. The rest of the body is current (1.20.1 → 26.2, three loaders,
> 49 builds, CodxLib required, Citadel bundled), so this is a one-sentence repair, and the page body
> is **web-UI only** — the upload API cannot touch it.

### Relations are per-file and set at upload time — validate on ONE node first

There is no endpoint to edit a file's relations afterwards. Getting them wrong across 49 files means
49 manual web-UI edits or a delete-and-reupload. **So always `--only <node>` a single Fabric node
first** (Fabric exercises *both* relations) and confirm the 200 before the batch. That is exactly
how `2.0.1` was run.

`errorCode 1018 — Invalid slug in project relations` means the referenced project is unknown **or
still awaiting moderation**. On a freshly-created dependency project, **wait a day and retry** —
don't hunt for the "right" slug; OneBlock burned a session on `codx-lib`/`codxlib-api`/`codx` when
`codxlib` had been correct all along.

### The ledger is the only re-run guard, and it is also the only skip mechanism

CurseForge has no endpoint that lists a project's files, so re-runs cannot be made idempotent from
the server. `scripts/.cf_uploaded.json` maps `<ver>/<mc>-<loader>` → file id and is written **after
each success**, so an interrupted run resumes cleanly. Corollaries:

- ⚠️ **The key is version-scoped since `2.0.2`, and it was not before.** For `2.0.1` it was a bare
  `<mc>-<loader>`, i.e. a *project*-wide "have we ever uploaded this node" record — so the first
  bare run of the next release skipped all 49 and printed `uploaded=0 skipped=49 failed=0`. A green
  summary line for a release that never happened, and the same failure shape as the jar-regex trap
  above. The 49 `2.0.1` entries were migrated to `2.0.1/…` in place; prior releases stay in the file
  as history.
- **A bare re-run with the ledger lost double-uploads.** Use `--only` to repair.
- **`--force` no longer starts from an empty ledger** — it ignores the *skips* but still loads and
  extends the file, so a forced run can't erase an earlier release's file ids.
- **Seeding a key by hand holds that node back** — there is no `--skip` flag, and this is how
  OneBlock ran a deliberate 57-of-58 batch. The seeded key must now carry the version prefix.
- Because the script pipes through `tee`, **Python block-buffers and the log stays empty until the
  run ends**. Watch the *ledger* for progress, not the log.

### Verify against the ledger, not against cfwidget

`https://api.cfwidget.com/1635121` is the only keyless window onto the live file list, but it is a
**caching proxy**: straight after this run it still reported **`files: 0`** with all 49 confirmed.
That is the documented behaviour (it showed a stale 7-file list for minutes on codxlib and ignored
cache-bust params). **Never diagnose an upload from it** — trust the 200 + file id. It also never
exposes relations, so it cannot confirm the dependency wiring either.

The check that *is* meaningful is offline: diff the ledger keys against the jars on disk. For
`2.0.1` — 49 jars, 49 ledger entries, no missing, no extra, **no duplicate file ids**, split
17 fabric / 16 forge / 16 neoforge. That is the CurseForge twin of the "predict the count before the
run" rule above, and it catches the same class of silent partial release.

### The changelog is shared with Modrinth, links and all

Both uploaders read the same root `modrinth-changelog.md`. It contains a **Modrinth link for
CodxLib** (`modrinth.com/mod/codxlib`), which now renders on CurseForge pages pointing players at
the other store. Harmless, but if it should say CurseForge there, the fix is a per-node changelog —
and note the CF changelog, unlike the CF relations, **is** editable per file in the web UI.


## `2.0.4` (2026-08-04) — the first Modrinth-only release

49 nodes, `uploaded=49 skipped=0 failed=0`, **CurseForge deliberately not run** (user decision — the
CF project is a frozen mirror holding `2.0.1` + `2.0.2`). Contents: Fabric networking (Wave 6), #22,
#23, #24, #25 and #26.

Nothing new broke, so this entry is mostly a confirmation that the existing rules hold. Two things
worth keeping:

- **The uploader's own `failed=0` is still not the check.** Re-reading `GET /v2/project/{id}/version`
  after the run and diffing the live `version_number`s against the 49 expected keys is — it also
  catches the empty-`files` and duplicate-file-row shapes that a partial upload leaves behind. This
  run: 49 expected, 49 live, none missing, none unexpected, no empty rows, no duplicates, and all
  **17** Fabric versions carrying **2** dependencies (CodxLib + the pinned Fabric API). Note the
  project endpoint is cached and read **48** while the uploader was still on its last node — that lag
  is normal and is not evidence of a lost node.
- **Each node emits three jars** — `<name>.jar`, `-sources.jar`, `-javadoc.jar`. So a raw
  `find … -name '*.jar' | wc -l` over `versions/*/build/libs` reads **147**, not 49, and a count that
  excludes only `-sources` reads 98. `JAR_RE` pins the mod version and the uploader skips both
  classifiers explicitly, so this is only a trap when *predicting* the count by hand for rule 6.

## `2.0.8` (2026-08-07) — 49 live, but the first upload with post-run damage, and three new API lessons

`uploaded=49 skipped=0 failed=0`, yet the post-run verification found **three broken versions** that
stayed broken through **ten fresh single-version reads over five minutes**: `2.0.8+1.21.1-fabric` and
`+1.21.1-neoforge` with `files=[]`/`loaders=[]`/`game_versions=[]` (deps intact), and `+26.2-fabric`
with its file but empty `game_versions` and **empty deps**. Repaired in place; final state 49/49
live, correct deps, loader split 17/16/16. What this run established:

- ⚠️ **The single-version endpoint can ALSO serve the stale empty shape — for 25+ minutes.** After
  repairing, both "file-less" versions showed **two identical file rows**: the original upload's file
  had been attached all along. So `GET /v2/version/{id}` staying empty across five minutes of polling
  is *still* not proof. The workspace rule ("re-read the single version before concluding") is
  necessary but **not sufficient**; if a freshly-uploaded version shows deps but no file, wait
  30+ minutes before touching it, or accept the duplicate row.
- **Repair ladder that works with this PAT** (it can create and edit but returns the generic
  401 `Invalid Authentication Credentials` on `DELETE /v2/version` — that message means *missing
  scope*, not a bad token; `/v2/user` 200s and uploads succeed): metadata holes are fixed cleanly by
  `PATCH /v2/version/{id}` (`loaders`/`game_versions`/`dependencies`, 204, idempotent overwrite);
  a genuinely missing file by `POST /v2/version/{id}/file` (multipart: empty-JSON `data` part + the
  jar) — but see above, the "missing" file may not be missing.
- **The duplicate file rows on `2.0.8+1.21.1-fabric`/`-neoforge` are left in place deliberately.**
  The `primary` flag is on the right row, so the site button and launchers serve one correct jar.
  Removing one row needs `DELETE /v2/version_file/{hash}` — both rows have the **same hash**, so it
  is ambiguous which (or both) dies, the PAT may lack the scope anyway, and a both-rows delete would
  re-open the hole. Same call as the codxlib `1.3.6` incident. Don't "clean this up" later.

## `2.0.9` (2026-08-08) — 49 on **both** stores; CurseForge un-frozen

The first release to ship to Modrinth **and** CurseForge since `2.0.2`. Contents: the tenth,
eleventh and twelfth bug-report passes — #45/#48, #52, #53, #44, #56, #57, #58, #59, #60, #61, #62
and the systemic #66 (every per-tick potion effect inert since 1.20.2).

Pre-flight: stale jars cleared first (147 moved out, so the build could not reuse anything), one
`MOD_IS_RELEASE=true` invocation of 49 `:node:build` tasks (`task count: 49` echoed before the run —
rule 2's arg-count check), then **four** verifiers. `verify_mixins` and `verify_mixin_targets` both
green at `jars=49 selectors=1006 problems=0`; `verify_assets` `literals=394 missing=0`.

**`verify_convention_tags.py` exits 1 on this release and that is expected.** Its 9 problems across
5 Fabric nodes are all the same thing: `#c:tools/spear(s)`, referenced by #59's new
`alexsmobs:kangaroo_spears` tag, is not shipped by fabric-api on 1.20.1, 1.20.4, 1.20.6, 26.1.2 or
26.2 (it exists on 1.21→1.21.11). Every one of those references is declared `"required": false`, so
the loader skips the absent tag silently — which is the entire point of an optional entry. Do not
"fix" this by deleting the references; they are what lets another mod's spear join the behaviour.
⚠️ It means this verifier's exit code alone cannot gate a release — read *which* tags it names.

### Modrinth — 49 live, one node damaged again

`uploaded=48 skipped=1 failed=0` (one node uploaded first as the shape test, then skipped by the
uploader's own live-list re-read). Post-run verification found **`2.0.9+1.21.5-neoforge` with empty
`loaders` AND empty `game_versions`** — file and CodxLib dep intact. A fresh single-version
`GET /v2/version/{id}` confirmed it, so unlike `2.0.8` this was real damage on the first read, not
the stale-empty shape. **Third release running in which a bulk upload silently damages 1–3 nodes;
treat the post-run diff as mandatory, not as due diligence.**

- ⚠️ **NEW — a repair PATCH must set `loaders` BEFORE `game_versions`, in two separate calls.**
  Sending both in one body, or `game_versions` first, fails with **HTTP 400** `editing version
  through v3 route: loader field 'game_versions' does not exist for any loaders supplied` — v2 PATCH
  is routed through v3, which validates the game-version field *against the loaders the version
  currently has*, and a damaged version has none. `{"loaders":[…]}` → 204, then
  `{"game_versions":[…]}` → 204. The one-shot form looks like a malformed-payload error and is not.

Final state re-verified from the live list: **49** versions, split **16 forge / 16 neoforge / 17
fabric**, no empty file rows, no duplicate rows, no missing `game_versions` or `loaders`, all 49
carrying CodxLib and all **17** Fabric ones carrying the version-pinned Fabric API. Project total
**534** versions.

### CurseForge — un-frozen, 49 live

**The Modrinth-only decision of 2026-08-04 is reversed** (user, 2026-08-08): CurseForge is a normal
publishing target again for every release from here on. `uploaded=48 skipped=1 failed=0
unsupported=0`, 49 unique file ids, split 17/16/16, all 49 game-version ids resolving natively.

- **`2.0.3`–`2.0.8` were never uploaded to CurseForge and never will be** — the ledger jumps
  `2.0.2` → `2.0.9`. That gap is permanent and intentional; do not try to backfill it. A CF user
  updating from `2.0.2` therefore crosses six releases at once, and **by explicit choice the CF
  changelog is the same 2.0.9-only text as Modrinth's** (the alternative, a catch-up changelog
  folding in the six, was offered and declined).
- The ledger now holds three versions × 49 = **147** entries. It stays the only re-run guard.

## `2.0.10` (2026-08-09) — 49 on both stores, and the first clean Modrinth run in four releases

Contents: the thirteenth bug-report pass (#67–#71 plus **#72**, found by the verification session
itself). Notable as the first release in a while where **every** fix was client-confirmed before the
upload — six items in one `26.2-fabric` session.

**Pre-flight.** 147 stale jars moved out of `versions/*/build/libs/` first, then all 49 nodes in one
invocation (`BUILD SUCCESSFUL`, 49 release jars, no `-SNAPSHOT`, none on a wrong version). All three
verifiers landed on their predicted numbers — `verify_mixins.py` `jars=49 problems=0`,
`verify_mixin_targets.py --all-versions` `nodes=49 jars=49 selectors=1006 problems=0 skipped=0`,
`verify_assets.py` `literals=394 missing=0`. Nothing this pass touched a mixin or an asset literal,
so all three counts were expected to be identical to `2.0.9`'s, and were.

### ⚠️ `modrinth_upload.py` has no `--help` and no dry-run guard

`main()` tests for `--check`, `--only` and `--list` and **otherwise starts uploading**. So
`python3 scripts/modrinth_upload.py --help` is not a help request — it is a **full 49-node live
batch upload**, which is exactly what happened here; it was killed at 24 of 49.

It was harmless only because the uploader is idempotent by construction: it reads
`existing_version_numbers()` first and skips any `version_number` already on the project, so the
resume run reported `uploaded=25 skipped=24 failed=0` and the total came out right. Two rules follow:

- **Use `--list` to inspect the plan. Never `--help`.** Same for `curseforge_upload.py`, whose
  `main()` has the same fall-through shape (its ledger is the equivalent safety net).
- The intended `--only` pilot on Modrinth never ran as a result. Two of the 24 accidental versions
  were re-read fresh (`GET /v2/version/{id}`) and were well-formed — right loader, game version,
  file, CodxLib dep, version-pinned Fabric API on the Fabric node, changelog body — which stood in
  for the pilot after the fact. Do not rely on that: run the pilot.

Worth adding the guard (an unknown-flag `SystemExit`) before the next release.

### Modrinth — 49 live, zero damage

`uploaded=25 skipped=24 failed=0`, then the mandatory post-run diff: **49 live, split 17 Fabric /
16 NeoForge / 16 Forge, every version carrying CodxLib, every Fabric version its pinned Fabric API,
no empty `files`/`loaders`/`game_versions` and no duplicated file rows.** `2.0.8` and `2.0.9` each
needed in-place `PATCH` repairs and `2.0.7` before them; this one needed none. The diff is still
mandatory — three-for-four is not a fixed bug, and a clean run is only known to be clean *because*
the diff ran.

The project now holds **558** versions. Still zero featured, and the ~245 superseded versions are
still unpruned — both remain unauthorized, not stale.

### CurseForge — pilot then batch, 49 live

`--only 26.2-fabric` first (file id `8608051`, `codxlib` + `fabric-api` relations accepted), then the
full batch: `uploaded=48 skipped=1 failed=0 unsupported=0`, **49 unique file ids**, no `FAIL` lines.
The one skip is the pilot node, held by the ledger entry its own run wrote — the expected shape for a
pilot-then-batch release, and the reason to read the file-id count rather than `uploaded`. The ledger
now holds four versions × 49 = **196** entries.

## `2.0.11` (2026-08-09) — 49 on both stores, one fix, and the guards are finally in

One-fix release (**#73**, the transmutation-table server crash), client-confirmed on `1.20.1-fabric`
before upload. Fresh 49-task release build (`BUILD SUCCESSFUL in 5m 53s`, 49 release jars all
`2.0.11`); all three verifiers on their predicted numbers — `verify_mixins` `jars=49 problems=0`,
`verify_mixin_selectors` `nodes=49 jars=49 selectors=1006 problems=0 skipped=0`, `verify_assets`
`literals=394 missing=0`.

### Both uploaders now reject unknown flags

The `2.0.10` TODO is done. `modrinth_upload.py` and `curseforge_upload.py` each grew
`_reject_unknown_flags(args)`, called as the first statement of `main()`: any argument outside the
recognised set raises `SystemExit`, with value-taking flags skipped over (`--only` on both,
`--dep-slug` on CF). Both verified to reject `--help`. There is still **no dry run** — `--list` is the
inspection flag, and a bare invocation still uploads everything.

### Modrinth — 49 live, one node damaged, and a near-miss on the repair

Pilot `2.0.11+1.20.1-fabric` (`vKs3L7uH`) verified perfect, then `uploaded=48 skipped=1 failed=0`.
Post-run diff: **632** project versions, **49** × `2.0.11`, split fabric 17 / neoforge 16 / forge 16.

One row came back damaged — `2.0.11+1.21.1-fabric` (`VKRzw4jd`), with `loaders`, `game_versions`,
`files` **and** `dependencies` all empty on a *fresh single-version* GET. The metadata half was real:
three `PATCH`es, 204 each, and the **loaders-before-game_versions split is still required** (one
combined body 400s). The file half was **not** — re-reading after the metadata repair showed
`alexsmobs-2.0.11-fabric+1.21.1.jar` had been attached the whole time. ⚠️ **Never `POST /version/{id}/
file` off a single read.** That re-read is the only thing that kept this release from acquiring the
cosmetic duplicate file rows `2.0.8` carries. Final state: **zero suspect rows** across all 49.

### CurseForge — pilot then batch, 49 live

`--only 26.2-fabric` first (file id `8612301`, both relations accepted), then the batch:
`uploaded=48 skipped=1 failed=0 unsupported=0`, **49 unique file ids** (the skip is the pilot, held by
its own ledger entry — read the file-id count, not `uploaded`). Ledger now holds five versions ×
49 = **245** entries.

## `2.0.12` (2026-08-10) — 49/49 on both stores, clean

The fifteenth pass (#74/#75/#76). Nothing new went wrong, which is itself the note: this is the
**second** release in a row with no silent post-upload damage on Modrinth, after four in a row that
had it. The post-run diff is still mandatory — a clean run is not evidence the next one will be.

- Build: 147 stale `2.0.11` jars **and** all 49 `versions/*/build/resources` trees moved out first
  (`DataPackMigration.LIVE_ICON_ITEMS` changed, so rule 9 applied), one 49-task invocation,
  `BUILD SUCCESSFUL in 5m 49s`, 49 release jars, zero `-SNAPSHOT`.
- Verifiers, all on their predicted numbers: `verify_mixins jars=49 problems=0`,
  `verify_mixin_targets jars=49 selectors=1006 problems=0`, `verify_assets literals=394 missing=0`.
  Unchanged from `2.0.11` — expected, since the pass added no mixins and no asset literals.
- Modrinth: pilot `2.0.12+1.21.11-fabric` = `vMuWI9QO`, verified by a **fresh single-version GET**
  (file, loader, game version, Fabric API version-pinned `6qAuTtLR` + CodxLib project-level,
  changelog body) before the batch. Batch `uploaded=48 skipped=1 failed=0`. Post-run diff: **681**
  project versions, 49 × `2.0.12`, split 17/16/16, **zero suspect rows**.
- CurseForge: pilot `--only 26.2-fabric` = file `8615094`, then `uploaded=48 skipped=1 failed=0`,
  **49 unique file ids**. Ledger `scripts/.cf_uploaded.json` is keyed `<version>/<node>` and is now
  at 6 × 49 = 294 (`2.0.1`, `2.0.2`, `2.0.9`, `2.0.10`, `2.0.11`, `2.0.12` — the `2.0.3`–`2.0.8` gap
  is permanent by decision).
- ⚠️ `api.cfwidget.com/1635121` reported **zero** `2.0.12` files minutes after the pilot returned a
  200 with a file id. That is the documented caching-proxy lag, nothing else; the file id is the
  proof of upload, and there is no CF endpoint that lists a project's files.
- ⚠️ **`git push` alone fails on this repo.** The local branch is `master` and it tracks
  `origin/main`, so a bare push refuses with the `branch.autoSetupMerge` advice and silently does
  nothing (`ahead 1` afterwards). Use `git push origin master:main`.
- ⚠️ **Shipped with no client session.** The user was told the risk and chose to ship; all three
  fixes are visual and none has been seen working.

## `2.1.4` (2026-08-19) — both stores, and the first sibling release

- Build `GRADLE_EXIT=0` with `MOD_IS_RELEASE=true`, 147 jars, zero `-SNAPSHOT`, zero non-`2.1.4`.
  All four verifiers on their predicted numbers (`jars=49 problems=0`, `selectors=1070 problems=0`,
  `literals=394 missing=0`, `problems=9`) — the `1070` unchanged from `2.1.3` is the proof the pass
  added no mixin.
- Modrinth: pilot `2.1.4+26.2-fabric` = `6PBRQTMs`, verified by a fresh single-version GET (file,
  `fabric`/`26.2`, Fabric API pinned `lVXlbH4w`, CodxLib project-level, `# 2.1.4` changelog body),
  then `uploaded=48 skipped=1 failed=0`. Project now holds **1122** versions, 49 × `2.1.4`, 17/16/16.
- CurseForge: pilot `--only 26.2-fabric` = file `8686551`, then `uploaded=48 skipped=1 failed=0`,
  **49 unique file ids**; ledger at 15 × 49 = **735**.
- ⚠️ **Fifth release running where `failed=0` hid a damaged version.** `2.1.4+1.21.3-forge`
  (`YztCeAT6`) listed with empty `files` **and** empty `dependencies`, and a **fresh single-version
  re-read agreed**. The dependency `PATCH` was a real repair. The file was not — the jar had been
  attached the whole time, and the `POST /file` produced a **cosmetic duplicate file row** (the same
  outcome as `2.0.8`'s two; leave it alone). **New rule: on a repair, `PATCH` the metadata, re-read
  once more, and only then consider POSTing the jar.** The single-version endpoint's `files` array is
  not trustworthy even fresh; `dependencies`/`loaders`/`game_versions` have been.
- Final sweep: 49 rows, zero suspect rows, 17/17 Fabric with both dependencies, 32/32 Forge/NeoForge
  with CodxLib, one multi-file row (the repaired node).

### Sibling: AlexsMobsContinuedDelight `1.0.0` (same day, both stores)

- Modrinth project `iY124w4e` (slug `alexs-mobs-continued-farmers-delight-refabricated`), **still a
  `draft`** — every endpoint needs the PAT to read, and it must be submitted for review by hand.
  Pilot `1.0.0+26.2-fabric` = `4LOKjEhw`, then `uploaded=14 skipped=1 failed=0`; post-run diff **15
  versions, 0 suspect rows**, fabric 12 / neoforge 2 / forge 1.
- CurseForge project `1658248`, token and id copied from this repo's `scripts/`. Pilot file
  `8686435`, then `uploaded=14 skipped=1 failed=0`, **15 unique file ids**.
- Both uploaders are ports of this repo's. Two things had to change beyond the constants:
  **`MC_ALIASES = {}`** (AMCD's `stonecutter.properties.toml` carries no `deps.minecraft-range`, so
  the CF script's `_assert_manifest_widened` guard correctly refuses the inherited aliases), and a
  **per-loader Farmer's Delight dependency** — Refabricated (`7vxePowz` / `farmers-delight-refabricated`)
  on the 12 Fabric nodes, vectorwing's (`R2OftAxM` / `farmers-delight`) on the 3 Forge/NeoForge ones.
  The two flavours' loader lists are disjoint, so each node names the only one a player could install
  and marks it `required` rather than hedging with `optional`.

## `2.1.5` (2026-08-21) — 49/49 on both stores, and the sixth release running with silent damage

Contents: the thirty-first pass (#104 mob inventories + nine restored loot tables, #105 the
laviathan's dead `getEntityInteractionResult`, #106 the NeoForge earthquake camera, #107 the GUI
atlas churn) and the thirty-second (#108 the straddleboard's alpha-0 panel, plus the two contrast
settings). Commit `d296914`, pushed to `origin/main` before either upload.

Build: one `bash -c`-wrapped invocation of all 49 `:build` tasks, `--max-workers=4`,
`MOD_IS_RELEASE=true` **on the wrapper** — `BUILD SUCCESSFUL in 4m 15s`, 147 jars, zero
`-SNAPSHOT`, zero non-`2.1.5`. Both the 147 stale `2.1.5` dev jars and all 49
`versions/*/build/resources` trees were moved out first (rule 9 — `DataPackMigration` changed).

All four verifiers on their **predicted** numbers:

| verifier | result |
|---|---|
| `verify_mixin_targets.py` | `nodes=49 jars=49 selectors=1108 problems=0 skipped=0` |
| `verify_mixins.py` | `jars=49 problems=0` |
| `verify_assets.py` | `literals=394 missing=0` |
| `verify_convention_tags.py` | `problems=9` (spear optionals, exit 1 by design) |

`1108` is `1070 + 14 + 24` — `CameraMixin` on the 14 NeoForge ≥1.21 nodes and
`ItemStackRenderStateAtlasMixin` on the 24 nodes ≥1.21.6, i.e. the count alone proves both new
mixins are gated to exactly the intended node sets.

### Pilots on the affected node

Both pilots ran on `1.21.11-fabric`, the worst band for #107. Modrinth `37PbKDp2`, verified by a
**fresh single-version GET**: file attached, `fabric` / `1.21.11`, Fabric API pinned to `6qAuTtLR`,
CodxLib project-level, and the changelog body confirmed to be `2.1.5`'s. CurseForge file `8695255`.
Batches: `uploaded=45 skipped=4 failed=0` (four rows already existed from the killed first run) and
`uploaded=48 skipped=1 failed=0`, ledger at 16 × 49 = **784** with 49 unique file ids.

### ⚠️ `failed=0` hid one damaged version — sixth release running

`2.1.5+1.21.7-forge` (`XSlbqkCu`) came back with `files`, `loaders`, `game_versions` **and**
`dependencies` all empty — on the project listing *and* on a fresh single-version re-read, so real
damage rather than the documented endpoint cache. Repaired with the usual ladder, three separate
`PATCH`es in order (`loaders` → `game_versions` → `dependencies`), all `204`.

**The file had been attached all along.** Re-reading after the metadata repair is what showed that,
and is what avoided a duplicate file row — the mistake `2.0.8` and `2.1.4` both made. **Never
`POST /file` off a single read.**

Final sweep: 49 rows, **zero suspect rows**, split 17/16/16, 17/17 Fabric with both dependencies,
32/32 Forge/NeoForge with CodxLib, no duplicate file rows. Modrinth now holds **1171** versions.

### ⚠️ Rig: launch long jobs with `nohup`, not the background-task wrapper

The release build was killed by the harness at **69/147 jars**, and the Modrinth batch after **4**
of 49 rows. Both were harmless — Gradle is incremental and the uploader skips versions that already
exist, so a plain re-run resumed each — but the robust form is:

```bash
nohup ./gradlew $(cat tasks.txt) --continue --max-workers=4 > ~/.cache/amc-trash/build.log 2>&1 &
```

and then watch the log. ⚠️ Python's stdout is **block-buffered** into a redirected log, so an
uploader's per-node lines do not appear until it exits; read live progress from the Modrinth API or
`scripts/.cf_uploaded.json` instead of the log.

### ⚠️ `/tmp` here is a RAM-backed tmpfs — build trash goes to `~/.cache/amc-trash/`

Moving 147 jars into `/tmp` filled the 32G tmpfs to 100%, after which **every** Bash call failed
with `ENOSPC` on its own stdout — including `df`. The recovery is an output-silenced `mv` of the
trash directory onto real disk. Use `/home/niels/.cache/amc-trash/` for all build trash.

---

## `2.1.6` (2026-08-21) — 49/49 on both stores, and the first clean post-upload diff in seven releases

Contents: the thirty-third pass (**#109** the Forge spawn-packet crash, **#110** the sombrero's
`young` transform, **#111** the Sea Life catfish-bucket collision), **#112** (the NeoForge ≥1.21.7
`@OnlyIn` warning screen) and the `/amc` ↔ `/aac` command-name swap.

### ⚠️ The push form has changed

This repo is now the fresh orphan-root `Codx-org/AlexsMobsContinued`, and the local branch is
**`main`** tracking `origin/main`. Every earlier release record here says `git push origin
master:main`; that no longer applies. It is a bare `git push origin main`. Commit `805f600`, pushed
**before** the uploads as always.

### Build and verifiers

One 49-task invocation, `--max-workers=4`, **`MOD_IS_RELEASE=true`** — `BUILD SUCCESSFUL in 14m 40s`,
`GRADLE_EXIT=0`, 147 jars (49 × main+sources+javadoc), **zero `-SNAPSHOT`, zero non-`2.1.6`**.
Rule 9 did **not** apply: `build-logic/` and the migration passes were unchanged, so
`build/resources` was correctly left in place.

All four verifiers on their **predicted** numbers:

| verifier | result |
|---|---|
| `verify_mixins.py` | `jars=49 problems=0` |
| `verify_mixin_targets.py` | **`nodes=49 jars=49 selectors=1108 problems=0 skipped=0`** |
| `verify_assets.py` | `literals=394 missing=0` |
| `verify_convention_tags.py` | `nodes=17 problems=9` (the by-design `#c:item/tools/spear(s)` optionals) |

`selectors=1108` unchanged from `2.1.5` is the independent proof the pass added no mixin — #112 is a
`replacements.string` rule and #109/#110/#111 are all plain source.

### Pilots on `1.21.11-fabric`

Modrinth `WndIyGl0`. The fresh single-version GET proved all five things that one call is for: the
file attached (`alexsmobs-2.1.6-fabric+1.21.11.jar`, 27,678,134 bytes), `loaders: ["fabric"]`,
`game_versions: ["1.21.11"]`, Fabric API pinned to `6qAuTtLR` + CodxLib (`6oyMM4yX`) project-level,
and the changelog body's first line `# 2.1.6`. CurseForge file `8698461`.

Both batches then ran `uploaded=48 skipped=1 failed=0` (the skip is the pilot).

### ✅ The post-upload diff was clean — first time in seven releases

49 rows, 1220 project versions, split 17 fabric / 16 forge / 16 neoforge, **zero suspect rows**, no
dependency-count mismatches, no duplicate file rows. The repair ladder went unused.

**Run it anyway.** Six of the seven releases before this one hid genuinely damaged versions behind
`failed=0`, and there is nothing in a clean run that predicts the next one.

CurseForge ledger: **833** entries = 17 × 49, with **49 unique file ids** for `2.1.6`.

### Rig note: both stores in parallel

The two uploaders are independent (different APIs, different ledgers), so the Modrinth batch and the
CurseForge batch were run **concurrently**, each `nohup`'d to its own log — roughly halving the
wall-clock of a two-store release. Poll each with its own `pgrep -f <script>.py`, not a bare
`pgrep python3`; **a concurrent Claude session was releasing the sibling AlexsCavesContinued repo at
the same time** and its `curseforge_upload.py` is indistinguishable in `ps`. The authoritative check
that no other session touched this project is **this repo's own ledger** (`scripts/.cf_uploaded.json`
— entry count and mtime), never a process listing.

#!/usr/bin/env python3
"""Publish the Alex's Mobs Continued build matrix to CurseForge — one file per (MC x loader) node.

Ported from ``OneBlock/scripts/curseforge_upload.py`` (itself from codxlib's); the API gotchas
in those scripts all still apply. **Differences that matter here:**

  - Project is **Alex's Mobs Continued**, numeric id in ``scripts/.cf_project_id``
    (``1635121``, slug ``alexs-mobs-continued``, created in the **Mods** class).
  - Jars are ``alexsmobs-<ver>-<loader>+<mc>.jar`` (loader first, then ``+mc``) — the same
    naming ``modrinth_upload.py`` matches. **The jar regex is a coverage filter**: it must
    cover all three loaders or the run silently ships two thirds of the tree and still prints
    ``failed=0``. Predict the node count and check ``--list`` before every run.
  - **Two relations, not one.** Every node requires **CodxLib**; **Fabric nodes additionally
    require Fabric API**. CurseForge relations carry **no version** — a relation is
    ``{slug, type}``, full stop — so unlike the Modrinth uploader there is nothing to pin.
    The Fabric API floor each jar actually needs is enforced by the loader reading the jar's
    own ``fabric.mod.json``; don't go looking for a CF version field, there isn't one.

CurseForge's *upload* API is a different beast from Modrinth's:
  - Auth is a header ``X-Api-Token`` (upload tokens: legacy.curseforge.com/account/api-tokens).
    One token covers every project; there are no scopes.
  - ``gameVersions`` are **numeric ids**, not strings. The MC version, the mod loader *and*
    the environment (Client/Server) are all entries in ``GET /api/game/versions`` under
    different ``gameVersionTypeID``s, so each upload sends ``[<mc>, <loader>, client, server]``.
    Resolved by name at runtime rather than hardcoded, because CF adds ids for every new
    MC release.
  - There is **no endpoint to list a project's existing files** (that lives in the separate
    Core API, which needs its own key), so re-runs can't ask the server what's already
    there. We keep a local ledger at ``scripts/.cf_uploaded.json`` and skip nodes recorded
    in it. ``--force`` ignores the ledger — a bare re-run without the ledger double-uploads.
    Seeding a node's key into the ledger by hand is also the only way to hold a node back.

Usage:
  python3 scripts/curseforge_upload.py --check                 # validate token
  python3 scripts/curseforge_upload.py --versions              # dump CF's MC/loader ids
  python3 scripts/curseforge_upload.py --list                  # what would upload (+ id mapping)
  python3 scripts/curseforge_upload.py --only 26.2-fabric      # single node (test upload)
  python3 scripts/curseforge_upload.py                         # upload all not-yet-recorded
  python3 scripts/curseforge_upload.py --force                 # ignore the local ledger
  python3 scripts/curseforge_upload.py --no-deps               # omit the relations

Config:
  Project id  -- numeric, from the project page sidebar. Set ``CURSEFORGE_PROJECT_ID`` or
                 put it in ``scripts/.cf_project_id`` (gitignored). It is NOT the slug.
  Token       -- ``CURSEFORGE_TOKEN`` env or ``scripts/.cf_token`` (gitignored).
"""
import sys, os, re, json, time, glob, subprocess, urllib.request, urllib.error

HERE = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(HERE)  # scripts/ -> repo root
API = "https://minecraft.curseforge.com/api"
UA = "amc-publisher/1.0 (+https://www.curseforge.com/minecraft/mc-mods/alexs-mobs-continued)"
LEDGER = os.path.join(HERE, ".cf_uploaded.json")

# CurseForge expresses file relations by project **slug**, not numeric id, and with no
# version. CF rejects the whole upload with ``errorCode 1018 — Invalid slug in project
# relations`` if a slug is unknown *or* if the referenced project is still awaiting
# moderation. On 1018 against a freshly-created dependency project, wait a day and retry
# rather than hunting for a different slug (that lesson cost a session on OneBlock).
CODXLIB_SLUG = os.environ.get("CODXLIB_CF_SLUG", "codxlib")
FABRIC_API_SLUG = os.environ.get("FABRIC_API_CF_SLUG", "fabric-api")

LOADER_LABEL = {"forge": "Forge", "neoforge": "NeoForge", "fabric": "Fabric"}

# --- one node serving several MC patch releases --------------------------
# The tree is normally one node per MC version, but MC sometimes ships patch releases that are
# API-identical, and then a single node genuinely serves several. `gameVersions` is fixed at upload
# time, so the extra versions have to be listed here.
#
# ⚠️ **The jar's own manifest is the authority, not this table.** CurseForge will happily advertise
# a file against any MC version; the loader then reads the range out of the jar and refuses it. So
# an entry here is only honest if that node also sets `deps.minecraft-range` in
# stonecutter.properties.toml — `_assert_manifest_widened` enforces exactly that and refuses to
# upload otherwise, the same way the Modrinth uploader refuses to ship a Fabric jar without its
# Fabric API pin.
MC_ALIASES = {
    "26.1.2": ["26.1", "26.1.1", "26.1.2"],
}


def mc_label(mc):
    """Display suffix for a node — '26.1.x' when it covers a range, else the bare version."""
    extra = MC_ALIASES.get(mc)
    if not extra:
        return mc
    prefix = ".".join(min(extra, key=lambda v: [int(x) for x in v.split(".")]).split(".")[:2])
    return f"{prefix}.x"


def _toml_section_keys(loader, mc):
    """Keys of the [<loader>."<mc>"] section of stonecutter.properties.toml."""
    keys, inside = set(), False
    with open(os.path.join(PROJECT_ROOT, "stonecutter.properties.toml")) as f:
        for line in f:
            s = line.strip()
            if s.startswith("["):
                inside = s.startswith(f'[{loader}."{mc}"]')
                continue
            if inside and "=" in s and not s.startswith("#"):
                keys.add(s.split("=", 1)[0].strip())
    return keys


def _assert_manifest_widened(node):
    """A node may only claim extra MC versions if its manifest range was widened to match.

    Tagging the store without widening the jar ships something the launcher installs and the
    loader then rejects — the same shape as the fabric-loader floor bug. Fail loudly instead.
    """
    if node["mc"] not in MC_ALIASES:
        return
    if "deps.minecraft-range" not in _toml_section_keys(node["loader"], node["mc"]):
        raise SystemExit(
            f"MC_ALIASES claims {node['key']} also covers {MC_ALIASES[node['mc']]}, but "
            f'[{node["loader"]}."{node["mc"]}"] in stonecutter.properties.toml has no '
            "`deps.minecraft-range` — so the jar still pins one exact version and would be "
            "rejected by the loader on the others. Widen the manifest and rebuild first.")


def mod_version():
    toml = os.path.join(PROJECT_ROOT, "stonecutter.properties.toml")
    with open(toml) as f:
        for line in f:
            s = line.strip()
            if s.startswith("mod.version"):
                return s.split("=", 1)[1].strip().strip('"')
    raise SystemExit("mod.version not found in stonecutter.properties.toml")


MOD_VERSION = mod_version()


def _read_cfg(env, filename, what):
    v = os.environ.get(env)
    if v:
        return v.strip()
    p = os.path.join(HERE, filename)
    if os.path.exists(p):
        with open(p) as f:
            return f.read().strip()
    raise SystemExit(f"No {what}: set {env} or create scripts/{filename}")


def token():
    return _read_cfg("CURSEFORGE_TOKEN", ".cf_token", "token")


def project_id():
    pid = _read_cfg("CURSEFORGE_PROJECT_ID", ".cf_project_id", "project id")
    if not pid.isdigit():
        raise SystemExit(f"Project id must be numeric (got '{pid}') — use the id from the "
                         "project page sidebar, not the URL slug.")
    return pid


def api_get(path):
    req = urllib.request.Request(API + path,
                                 headers={"X-Api-Token": token(), "User-Agent": UA})
    with urllib.request.urlopen(req) as r:
        return json.load(r)


def check():
    try:
        v = api_get("/game/versions")
        print(f"OK  token valid — CurseForge returned {len(v)} game-version entries")
        return v
    except urllib.error.HTTPError as e:
        print(f"FAIL  HTTP {e.code}: {e.read().decode()[:200]}")
        sys.exit(1)


# --- CurseForge game-version id resolution -------------------------------
_LOADER_CF_NAME = {"forge": "Forge", "neoforge": "NeoForge", "fabric": "Fabric"}

_MC_TYPE_SLUG_RE = re.compile(r"^minecraft-\d")

# Alex's Mobs runs on both sides. A Mods-class project *must* tag at least one
# ("errorCode 1021 — You must select at least one version from the environment group").
_ENVIRONMENTS = ("client", "server")


def version_index():
    """{'mc': {name -> id}, 'loader': {lowername -> id}, 'env': {slug -> id}} from CF.

    The same MC version name appears under *several* version types — e.g. '1.20.2' exists
    as id 10236 under type 'minecraft-1-20', 10326 under an unnamed legacy type, and
    10864 under 'addons'. Only the ``minecraft-<digit>…`` types are valid for a project in
    the **Mods** class; sending an id from another type fails the upload with
    ``errorCode 1009 — Invalid game version ID: <id> belongs to an invalid dependency``.
    So filter by version *type*, never by name alone — matching on name and taking
    whatever the API happens to return first is how we hit exactly that error.

    Mod loaders live under the 'modloader' type. Resolved by name so new MC releases need
    no code change.
    """
    types = {t["id"]: t for t in api_get("/game/version-types")}
    mc_type_ids, loader_type_ids, env_type_ids = set(), set(), set()
    for tid, t in types.items():
        slug = (t.get("slug") or "").lower()
        if slug == "modloader":
            loader_type_ids.add(tid)
        elif slug == "environment":
            env_type_ids.add(tid)
        elif _MC_TYPE_SLUG_RE.match(slug):
            mc_type_ids.add(tid)
    mc, loader, env = {}, {}, {}
    for v in api_get("/game/versions"):
        name = v.get("name", "")
        tid = v.get("gameVersionTypeID")
        if tid in loader_type_ids:
            loader.setdefault(name.lower(), v["id"])
        elif tid in env_type_ids:
            env.setdefault(name.lower(), v["id"])
        elif tid in mc_type_ids and re.fullmatch(r"\d+(\.\d+)*", name):
            mc.setdefault(name, v["id"])
    return {"mc": mc, "loader": loader, "env": env}


# --- discover jars -------------------------------------------------------
# ⚠️ COVERAGE FILTER — all three loaders, or the run ships a partial release and still
# reports failed=0. See the module docstring and docs/notes/publishing.md.
JAR_RE = re.compile(r"alexsmobs-" + re.escape(MOD_VERSION) + r"-(forge|neoforge|fabric)\+(.+)\.jar$")


def _vkey(mc):
    # numeric ascending sort: "1.20.1" < "1.21" < "1.21.10" < "26.1.2" < "26.2"
    return tuple(int(x) for x in re.findall(r"\d+", mc))


_LOADER_ORDER = {"forge": 0, "neoforge": 1, "fabric": 2}


def nodes():
    out = []
    pat = os.path.join(PROJECT_ROOT, "versions", "*", "build", "libs", "*.jar")
    for p in glob.glob(pat):
        b = os.path.basename(p)
        # `<mc>` is matched with `.+`, so these siblings also match the pattern.
        if b.endswith("-sources.jar") or b.endswith("-javadoc.jar"):
            continue
        m = JAR_RE.match(b)
        if not m:
            continue
        loader, mc = m.group(1), m.group(2)
        out.append({
            "path": p, "file": b, "loader": loader, "mc": mc,
            "key": f"{mc}-{loader}",
            # mc_label, not mc: a node covering 26.1/26.1.1/26.1.2 reads "26.1.x". It also keeps
            # a re-uploaded file visibly distinct from a superseded one in the web UI, which
            # matters because CF has no delete endpoint and the filenames are identical.
            "name": f"Alex's Mobs Continued {MOD_VERSION} — {LOADER_LABEL[loader]} {mc_label(mc)}",
        })
    out.sort(key=lambda n: (_vkey(n["mc"]), _LOADER_ORDER.get(n["loader"], 9)))
    return out


def relations(node):
    """CodxLib on every node; Fabric API additionally on Fabric ones. No versions — CF
    relations are {slug, type} and carry none (the jar's own manifest is what enforces
    the Fabric API floor)."""
    projects = [{"slug": CODXLIB_SLUG, "type": "requiredDependency"}]
    if node["loader"] == "fabric":
        projects.append({"slug": FABRIC_API_SLUG, "type": "requiredDependency"})
    return {"projects": projects}


def changelog(node=None):
    """Per-node changelog if present, else the shared root one.

    Same files the Modrinth uploader uses. This repo doesn't write per-node ones yet.
    """
    if node:
        p = os.path.join(os.path.dirname(node["path"]), "modrinth-changelog.md")
        if os.path.exists(p):
            with open(p) as f:
                return f.read()
    with open(os.path.join(PROJECT_ROOT, "modrinth-changelog.md")) as f:
        return f.read()


# --- upload --------------------------------------------------------------
class HttpFail(Exception):
    def __init__(self, code, body):
        self.code = code
        self.body = body
        super().__init__(f"HTTP {code}: {body}")


def upload(node, cl, game_version_ids, with_deps=True):
    metadata = {
        "changelog": cl,
        "changelogType": "markdown",
        "displayName": node["name"],
        "gameVersions": game_version_ids,
        "releaseType": "release",
    }
    if with_deps:
        # Relations are per-file and set at upload time — there is NO API to edit them
        # afterwards, so validate on a single --only node before the full run.
        metadata["relations"] = relations(node)
    # --form-string (not -F): the changelog is markdown, and -F would treat a leading
    # '@' or '<' in a value as a file reference. CurseForge parses this part as JSON text,
    # which is exactly what its own documented curl example sends.
    cmd = [
        "curl", "-sS", "-X", "POST", f"{API}/projects/{project_id()}/upload-file",
        "-H", f"X-Api-Token: {token()}",
        "-H", f"User-Agent: {UA}",
        "-w", "\n__HTTP__%{http_code}",
        "--form-string", "metadata=" + json.dumps(metadata),
        "-F", f"file=@{node['path']};type=application/java-archive;filename={node['file']}",
    ]
    p = subprocess.run(cmd, capture_output=True, text=True)
    out = p.stdout
    code = "000"
    if "__HTTP__" in out:
        out, code = out.rsplit("__HTTP__", 1)
        code = code.strip()
    if code != "200":
        raise HttpFail(int(code) if code.isdigit() else 0, out.strip()[:300])
    return json.loads(out)


def ledger_key(node):
    """Ledger keys are **version-scoped** (``<ver>/<mc>-<loader>``).

    They were a bare ``<mc>-<loader>`` for 2.0.1, which made the ledger a *project*-wide
    "have we ever uploaded this node" record — so the first bare run of the *next* release
    skipped all 49 nodes and printed ``uploaded=0 skipped=49 failed=0``, a green line for a
    release that never happened. Scoping by version keeps the resume guard within a release
    (an interrupted run still resumes) while leaving every prior release's file ids in the
    file as history. 2.0.1's entries were migrated to ``2.0.1/…`` when this changed.
    """
    return f"{MOD_VERSION}/{node['key']}"


def load_ledger():
    if os.path.exists(LEDGER):
        with open(LEDGER) as f:
            return json.load(f)
    return {}


def save_ledger(d):
    with open(LEDGER, "w") as f:
        json.dump(d, f, indent=2, sort_keys=True)


# --- main ----------------------------------------------------------------
# Flags that take a value; everything after one of these is that value, not a flag.
_FLAGS_WITH_VALUE = {"--only", "--dep-slug"}
_FLAGS = {"--check", "--versions", "--list", "--force", "--no-deps"} | _FLAGS_WITH_VALUE


def _reject_unknown_flags(args):
    """Same guard as the Modrinth uploader: a bare run UPLOADS every node and there is
    no --help and no dry run, so an unrecognised argument must stop the script rather
    than fall through. Use --list to inspect."""
    i = 0
    while i < len(args):
        a = args[i]
        if a in _FLAGS_WITH_VALUE:
            i += 2
            continue
        if a not in _FLAGS:
            raise SystemExit(
                f"Unrecognised argument {a!r}. There is no --help and no dry run: a bare "
                f"invocation UPLOADS. Known flags: {' '.join(sorted(_FLAGS))}. "
                "Use --list to see what would be uploaded.")
        i += 1


def main():
    args = sys.argv[1:]
    _reject_unknown_flags(args)
    if "--check" in args:
        check(); return
    if "--versions" in args:
        idx = version_index()
        print("Mod loaders:")
        for k, v in sorted(idx["loader"].items()):
            print(f"  {k:22} {v}")
        print(f"\nMinecraft versions ({len(idx['mc'])}):")
        for k in sorted(idx["mc"], key=_vkey):
            print(f"  {k:12} {idx['mc'][k]}")
        return

    global CODXLIB_SLUG
    if "--dep-slug" in args:
        CODXLIB_SLUG = args[args.index("--dep-slug") + 1]
    with_deps = "--no-deps" not in args
    ns = nodes()
    if "--only" in args:
        key = args[args.index("--only") + 1]
        ns = [n for n in ns if key in n["path"] or key in n["file"]]
    if not ns:
        raise SystemExit(f"No jars found for mod_version {MOD_VERSION} under "
                         "versions/*/build/libs/ — build the matrix first "
                         "(MOD_IS_RELEASE=true).")

    idx = version_index()
    env_ids = [idx["env"][e] for e in _ENVIRONMENTS if e in idx["env"]]
    if not env_ids:
        raise SystemExit("CurseForge returned no 'environment' versions — a Mods-class "
                         "project requires at least one (errorCode 1021).")
    missing, ready = [], []
    for n in ns:
        _assert_manifest_widened(n)
        n["mcs"] = MC_ALIASES.get(n["mc"], [n["mc"]])
        mc_ids = [idx["mc"].get(v) for v in n["mcs"]]
        loader_id = idx["loader"].get(_LOADER_CF_NAME[n["loader"]].lower())
        if any(i is None for i in mc_ids) or loader_id is None:
            absent = [v for v, i in zip(n["mcs"], mc_ids) if i is None]
            n["why"] = (f"MC version(s) {absent} not in CurseForge's taxonomy" if absent
                        else f"loader '{n['loader']}' not in CurseForge's taxonomy")
            missing.append(n)
        else:
            n["ids"] = mc_ids + [loader_id] + env_ids
            ready.append(n)

    if "--list" in args:
        for n in ready:
            deps = ",".join(p["slug"] for p in relations(n)["projects"]) if with_deps else "none"
            mcs = ",".join(n["mcs"])
            print(f"{n['key']:22} mc={mcs:22} ids={str(n['ids']):34} deps={deps:20} <- {n['file']}")
        for n in missing:
            print(f"{n['key']:22} SKIP — {n['why']}")
        print(f"\n{len(ready)} uploadable, {len(missing)} unsupported  (mod_version={MOD_VERSION})")
        return

    if missing:
        print(f"WARNING: {len(missing)} node(s) have no CurseForge game version and will be "
              "skipped:")
        for n in missing:
            print(f"  {n['key']:22} — {n['why']}")
        print()

    check()
    # --force ignores the ledger's *skips* but still loads it, so a forced run cannot wipe
    # the record of earlier releases (it used to start from {} and rewrite the file).
    force = "--force" in args
    ledger = load_ledger()
    ok, skip, fail = 0, 0, 0
    for i, n in enumerate(ready, 1):
        lkey = ledger_key(n)
        if not force and lkey in ledger:
            print(f"[{i}/{len(ready)}] SKIP (already uploaded, file id {ledger[lkey]}) {lkey}")
            skip += 1
            continue
        try:
            r = upload(n, changelog(n), n["ids"], with_deps)
            fid = r.get("id")
            print(f"[{i}/{len(ready)}] OK   {n['key']}  -> file id {fid}")
            ledger[lkey] = fid
            save_ledger(ledger)
            ok += 1
        except HttpFail as e:
            print(f"[{i}/{len(ready)}] FAIL {n['key']}  HTTP {e.code}: {e.body}")
            fail += 1
            if e.code in (401, 403):
                print("\nABORT: auth error on a write — the upload token is invalid or has no "
                      "access to this project. Nothing further attempted.")
                break
        time.sleep(1.0)  # CurseForge upload rate limit
    print(f"\nDone. uploaded={ok} skipped={skip} failed={fail} unsupported={len(missing)}")
    if ok:
        print(f"Ledger: {LEDGER}")


if __name__ == "__main__":
    main()

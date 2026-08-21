#!/usr/bin/env python3
"""Publish the Alex's Mobs Continued build matrix to Modrinth — one version per (MC x loader) node.

Ported from OneBlock's ``scripts/modrinth_upload.py`` (itself from codxlib's). The API gotchas
in those scripts all still apply; see the notes at the bottom. **Differences that matter — read
these before reusing either ancestor script here:**

  - Project is ``alexs-mobs-continued`` (``kYIaHHfw``). The slug used to be **shared with the
    sibling AlexsMobsFP repo**, which published the *Fabric* line (1.0.0/1.0.1/1.0.4/1.0.5) to
    the very same project. Since 2026-07-30 **this repo owns the Fabric line too** — Milestone 15
    brought all 17 Fabric nodes to parity — so from ``2.0.0`` this script uploads all three
    loaders and the numbering is no longer split between two repos. The pre-2.0.0 Fabric rows on
    the project are still AlexsMobsFP's; leave them alone.
  - **CodxLib is declared required, deliberately, even though the jar does not link it yet.**
    Read this before "fixing" it: as of 1.0.6 nothing in ``src/`` imports codxlib and the
    generated manifest declares only ``minecraft`` + the loader (Citadel is vendored in-jar), so
    the *mod* has no build dependency. The requirement is a **support-workflow** decision by the
    author (2026-07-26): having CodxLib installed is what makes ``/codxlib help`` produce its
    installed-mods + connected-players report for a player who is asking for help. ``/codxlib
    versions`` and the settings dump stay blank for this mod until it calls
    ``UpdateChecker.register(modInfo())`` (planned for 1.0.7).
    The dep is **project-level (no ``version_id``)** so each MC version resolves its own CodxLib
    build; 1.3.3 and 1.3.5 both cover every node here (verified against the API, 2026-07-26).
  - **Fabric nodes additionally declare a VERSION-PINNED Fabric API dependency**, resolved from
    each node's ``deps.fabric-api`` pin in the toml (see ``dependencies()``). Unlike the CodxLib
    entry this one must *not* be project-level: Fabric API releases are per-MC-version and the
    pins are non-monotonic, so a project-level entry lets an installer fetch a build the jar
    refuses to load. Forge/NeoForge nodes get no such entry.
  - ``version_number`` is ``<ver>+<mc>-<loader>``. The 1.0.2/1.0.3 uploads used a bare ``<ver>``
    for *every* node, which renders as a dozen identical "1.0.3" rows with no way to tell which
    file is which. With 18 nodes that is unusable, so this script disambiguates.
  - Jar names are ``alexsmobs-<ver>-<loader>+<mc>.jar`` (loader first, then ``+mc``).

Reads the built jars under ``versions/*/build/libs/`` and the mod version from
``stonecutter.properties.toml`` (``mod.version``). Build release jars with
``MOD_IS_RELEASE=true`` or they carry a ``-SNAPSHOT`` suffix and won't match.

Auth: ``MODRINTH_TOKEN`` env, or a gitignored ``.mr_token`` next to this script. The PAT needs
**Read user data** + **Create versions** (Write/Delete versions to repair).

Usage:
  python3 scripts/modrinth_upload.py --check                 # validate token only
  python3 scripts/modrinth_upload.py --list                  # show what would upload
  python3 scripts/modrinth_upload.py --only 1.21.1-neoforge  # single node (test upload)
  python3 scripts/modrinth_upload.py                         # upload all not-yet-present
  python3 scripts/modrinth_upload.py --force                 # ignore already-present check

Notes / API gotchas (learned the hard way on codxlib):
  - The create-version POST is multipart; the ``data`` JSON MUST be sent as a *file* part
    (``-F data=@file.json;type=application/json``), never inline — inline mangles JSON on the
    changelog's newlines/quotes, and ``;type=`` is ignored on inline values.
  - If the file part fails to attach, Modrinth still creates the version but silently clears
    loaders/game_versions (they don't stick without a file).
  - A token missing the write scope reports ``401 Invalid Authentication Credentials`` on the
    write endpoint even though it reads ``/v2/user`` fine.
  - v2 wants the raw token in ``Authorization`` — **no** ``Bearer`` prefix.
  - A large multi-node upload **can partially fail silently**; always re-check the live count
    afterwards rather than trusting the summary line.
"""
import sys, os, re, json, time, glob, subprocess, urllib.request, urllib.error

HERE = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(HERE)  # scripts/ -> repo root
API = "https://api.modrinth.com/v2"
PROJECT_ID = "kYIaHHfw"   # modrinth.com/mod/alexs-mobs-continued
CODXLIB_ID = "6oyMM4yX"   # modrinth.com/mod/codxlib — see the module docstring
FABRIC_API_ID = "P7dR8mSH"  # modrinth.com/mod/fabric-api — Fabric nodes only
UA = "amc-publisher/1.0 (+https://modrinth.com/mod/alexs-mobs-continued)"

LOADER_LABEL = {"forge": "Forge", "neoforge": "NeoForge", "fabric": "Fabric"}


def mod_version():
    toml = os.path.join(PROJECT_ROOT, "stonecutter.properties.toml")
    with open(toml) as f:
        for line in f:
            s = line.strip()
            if s.startswith("mod.version"):
                return s.split("=", 1)[1].strip().strip('"')
    raise SystemExit("mod.version not found in stonecutter.properties.toml")


MOD_VERSION = mod_version()


def token():
    tok = os.environ.get("MODRINTH_TOKEN")
    if tok:
        return tok.strip()
    for p in (os.path.join(HERE, ".mr_token"),
              os.path.join(PROJECT_ROOT, "..", "OneBlock", "scripts", ".mr_token")):
        if os.path.exists(p):
            with open(p) as f:
                return f.read().strip()
    raise SystemExit("No token: set MODRINTH_TOKEN or create scripts/.mr_token")


def api_get(path):
    req = urllib.request.Request(API + path, headers={"Authorization": token(), "User-Agent": UA})
    with urllib.request.urlopen(req) as r:
        return json.load(r)


def check():
    try:
        u = api_get("/user")
        print(f"OK  token valid — user '{u.get('username')}' (id {u.get('id')})")
        return u
    except urllib.error.HTTPError as e:
        print(f"FAIL  HTTP {e.code}: {e.read().decode()[:200]}")
        sys.exit(1)


# --- discover jars -------------------------------------------------------
JAR_RE = re.compile(r"alexsmobs-" + re.escape(MOD_VERSION) + r"-(forge|neoforge|fabric)\+(.+)\.jar$")


def _vkey(mc):
    # numeric ascending sort: "1.20.1" < "1.21" < "1.21.10"
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
            "version_number": f"{MOD_VERSION}+{mc}-{loader}",
            "name": f"Alex's Mobs Continued {MOD_VERSION} — {LOADER_LABEL[loader]} {mc}",
        })
    out.sort(key=lambda n: (_vkey(n["mc"]), _LOADER_ORDER.get(n["loader"], 9)))
    return out


# --- Fabric API dependency -----------------------------------------------
# Each Fabric jar's own fabric.mod.json declares `fabric-api >= <deps.fabric-api>` for its MC
# version (build.fabric{,nr}.gradle.kts: fabricLikeVersionRange). The Modrinth version has to say
# the same thing, so the dependency is **version-pinned** to exactly that build rather than
# project-level: a Fabric API release is per-MC-version, and the pins are NOT monotonic across the
# range, so one project-level entry would let an installer fetch a build the jar refuses to load.
# Forge/NeoForge nodes get no such entry — the loader itself is the API there.
def fabric_api_pins():
    """{mc: "<fabric-api version_number>"} from the [fabric."<mc>"] sections of the toml."""
    pins, sec = {}, None
    with open(os.path.join(PROJECT_ROOT, "stonecutter.properties.toml")) as f:
        for line in f:
            s = line.strip()
            m = re.match(r'^\[fabric\."([^"]+)"\]', s)
            if m:
                sec = m.group(1)
                continue
            if s.startswith("["):
                sec = None
                continue
            if sec and s.startswith("deps.fabric-api"):
                pins[sec] = s.split("=", 1)[1].strip().strip('"')
    return pins


_FABRIC_API_IDS = None


def fabric_api_version_id(mc):
    """Modrinth version id of the Fabric API build this node pins, or None if unpinned/unknown."""
    global _FABRIC_API_IDS
    pin = fabric_api_pins().get(mc)
    if not pin:
        return None
    if _FABRIC_API_IDS is None:
        vs = api_get(f"/project/{FABRIC_API_ID}/version")
        _FABRIC_API_IDS = {v["version_number"]: v["id"] for v in vs}
    return _FABRIC_API_IDS.get(pin)


def dependencies(node):
    deps = [
        # Project-level (version_id null) so each MC version resolves its own CodxLib build.
        # Intentional despite the jar not linking codxlib yet — see the module docstring.
        {"project_id": CODXLIB_ID, "version_id": None, "dependency_type": "required"},
    ]
    if node["loader"] == "fabric":
        vid = fabric_api_version_id(node["mc"])
        if vid is None:
            raise SystemExit(
                f"No Modrinth Fabric API version matches the pin for MC {node['mc']} "
                f"({fabric_api_pins().get(node['mc'])!r}). Refusing to upload a Fabric jar "
                "without its Fabric API dependency — fix the pin or the lookup first.")
        deps.append({"project_id": FABRIC_API_ID, "version_id": vid, "dependency_type": "required"})
    return deps


def changelog(node=None):
    """Per-node changelog if present, else the shared root one."""
    if node:
        p = os.path.join(os.path.dirname(node["path"]), "modrinth-changelog.md")
        if os.path.exists(p):
            with open(p) as f:
                return f.read()
    with open(os.path.join(PROJECT_ROOT, "modrinth-changelog.md")) as f:
        return f.read()


# --- multipart POST ------------------------------------------------------
class HttpFail(Exception):
    def __init__(self, code, body):
        self.code = code
        self.body = body
        super().__init__(f"HTTP {code}: {body}")


def post_version(node, cl):
    data = {
        "name": node["name"],
        "version_number": node["version_number"],
        "project_id": PROJECT_ID,
        "file_parts": ["file"],
        "primary_file": "file",
        "game_versions": [node["mc"]],
        "loaders": [node["loader"]],
        "dependencies": dependencies(node),
        "version_type": "release",
        "featured": False,
        "changelog": cl,
        "status": "listed",
    }
    # Attach the data JSON as a file part — inline -F values mangle JSON (the changelog's
    # newlines/quotes) and don't honor ;type=. File parts do.
    data_path = os.path.join(HERE, ".mr_data.json")
    with open(data_path, "w") as f:
        json.dump(data, f)
    cmd = [
        "curl", "-sS", "-X", "POST", API + "/version",
        "-H", f"Authorization: {token()}",
        "-H", f"User-Agent: {UA}",
        "-w", "\n__HTTP__%{http_code}",
        "-F", f"data=@{data_path};type=application/json",
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


# --- main ----------------------------------------------------------------
def existing_version_numbers():
    return {v.get("version_number") for v in api_get(f"/project/{PROJECT_ID}/version")}


# Flags that take a value; everything after one of these is that value, not a flag.
_FLAGS_WITH_VALUE = {"--only"}
_FLAGS = {"--check", "--list", "--force"} | _FLAGS_WITH_VALUE


def _reject_unknown_flags(args):
    """A bare run of this script uploads every node. There is no dry-run mode, so an
    unrecognised argument must NOT fall through to that -- `--help` did exactly that
    once and put 24 versions live before it was killed. Use `--list` to inspect."""
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
    ns = nodes()
    if "--only" in args:
        key = args[args.index("--only") + 1]
        ns = [n for n in ns if key in n["path"] or key in n["file"]]
    if "--list" in args:
        pins = fabric_api_pins()
        for n in ns:
            extra = ""
            if n["loader"] == "fabric":
                extra = f"  +fabric-api {pins.get(n['mc'])} ({fabric_api_version_id(n['mc'])})"
            print(f"{n['version_number']:28} <- {n['file']}{extra}")
        print(f"\n{len(ns)} node(s)  (mod_version={MOD_VERSION})")
        return
    if not ns:
        raise SystemExit(f"No jars found for mod_version {MOD_VERSION} under versions/*/build/libs/ "
                         "— build the matrix first (MOD_IS_RELEASE=true).")
    check()
    existing = set() if "--force" in args else existing_version_numbers()
    ok, skip, fail = 0, 0, 0
    for i, n in enumerate(ns, 1):
        if n["version_number"] in existing:
            print(f"[{i}/{len(ns)}] SKIP (exists) {n['version_number']}")
            skip += 1
            continue
        try:
            v = post_version(n, changelog(n))
            print(f"[{i}/{len(ns)}] OK   {n['version_number']}  -> version id {v.get('id')}")
            ok += 1
        except HttpFail as e:
            print(f"[{i}/{len(ns)}] FAIL {n['version_number']}  HTTP {e.code}: {e.body}")
            fail += 1
            if e.code in (401, 403):
                print("\nABORT: auth/scope error on first write — token lacks the "
                      "Create-versions scope (or org permission). Nothing further attempted.")
                break
        time.sleep(0.7)  # Modrinth rate limit ~300/min
    print(f"\nDone. uploaded={ok} skipped={skip} failed={fail}")


if __name__ == "__main__":
    main()

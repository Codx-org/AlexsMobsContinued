#!/usr/bin/env python3
"""Every `#c:` tag a FABRIC node references must be defined by something on that node.

WHY THIS EXISTS: on Forge and NeoForge the loader ships the whole convention-tag set, so a
`#forge:`/`#c:` reference always resolves. On Fabric they come from an OPTIONAL Fabric API module
whose contents grew version by version — `fabric-convention-tags-v1` (the only one below 1.20.6)
defines 156 tags, v2 grew to 500+ across a year of releases. A reference to one that does not exist
is LOGGED, NOT THROWN: the referencing tag simply loads empty. `c:sands` alone empties
`alexsmobs:am_spawns` and the fifteen `*_spawns` tags built on it, and the server still says
`Done (` and exits 0. That is the same shape as the Forge-26 `c:` fault of Milestone 13.

The boot gate does now fail on `Couldn't load tag`, but only for the nodes you actually boot — this
answers all 17 Fabric nodes in a second, off the build output, before anything is launched.

Provided-by, per node:
  * the node's pinned `deps.fabric-api` jar (its nested convention-tags-v1/v2 jars), and
  * `data/c/tags/**` in the node's own build output (this mod defines a dozen `c:` tags itself,
    plus the seven DataPackMigration.backfillFabricConventionTags writes).

usage: scripts/verify_convention_tags.py [node …]        (default: every *-fabric node)
Requires `./gradlew <node>:build` first — it reads versions/<node>/build/resources/main.
"""
from __future__ import annotations

import glob
import io
import os
import pathlib
import re
import sys
import zipfile

try:
    import tomllib
except ModuleNotFoundError:  # py<3.11
    import tomli as tomllib  # type: ignore

ROOT = pathlib.Path(__file__).resolve().parent.parent
PROPS = tomllib.loads((ROOT / "stonecutter.properties.toml").read_text())


def fabric_api_pin(mc: str) -> str:
    return PROPS["fabric"][mc]["deps"]["fabric-api"]


def api_tags(api: str) -> set[str] | None:
    """Tag paths under `data/c/tags/` that the pinned fabric-api jar defines."""
    jars = glob.glob(os.path.expanduser(
        "~/.gradle/caches/modules-2/files-2.1/net.fabricmc.fabric-api/fabric-api/"
        f"{api}/*/fabric-api-{api}.jar"))
    if not jars:
        return None
    out: set[str] = set()
    with zipfile.ZipFile(jars[0]) as outer:
        # The convention tags live in nested jars (fabric-api is a jar-in-jar bundle), and BOTH
        # modules can be present at once — v1 lingers as a deprecated shim well past v2's arrival.
        for nested in (n for n in outer.namelist()
                       if "convention-tags" in n and n.endswith(".jar")):
            with zipfile.ZipFile(io.BytesIO(outer.read(nested))) as inner:
                out |= {n[len("data/c/tags/"):-len(".json")] for n in inner.namelist()
                        if n.startswith("data/c/tags/") and n.endswith(".json")}
    return out


def check(node: str) -> int:
    mc = node[: -len("-fabric")]
    api = fabric_api_pin(mc)
    provided = api_tags(api)
    if provided is None:
        print(f"### {node}: no cached fabric-api {api} — SKIPPED")
        return 0
    base = ROOT / "versions" / node / "build/resources/main/data"
    if not base.is_dir():
        print(f"### {node}: no build output — SKIPPED (build the node first)")
        return 0

    own = base / "c/tags"
    if own.is_dir():
        provided |= {str(p.relative_to(own))[: -len(".json")] for p in own.rglob("*.json")}

    missing: dict[str, set[str]] = {}
    for path in base.rglob("*.json"):
        rel = path.relative_to(base)
        parts = rel.parts
        # A `#c:foo` inside data/<ns>/tags/<registry…>/x.json resolves in <registry…>. References
        # from anywhere else (recipes, loot tables) carry their registry implicitly and are checked
        # by the boot gate's `Couldn't parse data file` lines instead.
        if len(parts) < 4 or parts[1] != "tags":
            continue
        registry = "/".join(parts[2:-1])
        text = path.read_text()
        if "c:" not in text:
            continue
        for match in re.finditer(r'"#?c:([a-z0-9_./-]+)"', text):
            name = f"{registry}/{match.group(1)}"
            if name not in provided:
                missing.setdefault(name, set()).add(str(rel))

    print(f"### {node}  fabric-api {api}  provides={len(provided)}  missing={len(missing)}")
    for name in sorted(missing):
        sources = sorted(missing[name])
        shown = ", ".join(sources[:4]) + (" …" if len(sources) > 4 else "")
        print(f"    c:{name}   <- {shown}")
    return len(missing)


def main() -> int:
    nodes = sys.argv[1:] or sorted(
        p.name for p in (ROOT / "versions").iterdir() if p.name.endswith("-fabric"))
    problems = sum(check(node) for node in nodes)
    print(f"nodes={len(nodes)} problems={problems}")
    return 1 if problems else 0


if __name__ == "__main__":
    raise SystemExit(main())

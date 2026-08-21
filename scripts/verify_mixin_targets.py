#!/usr/bin/env python3
"""Third mixin gate: prove every injector SELECTOR in a shipped jar actually resolves.

`verify_mixins.py` proves that every mixin a jar *declares* is a class the jar *ships*,
and that no `@Mixin` targets our own code. It says nothing about whether an
`@Inject`/`@Redirect`/... selector matches anything in the target class — and an injector
with zero matches is `InvalidInjectionException: Critical injection failure`, thrown at
mixin apply, i.e. just as fatal as a missing class. That mode shipped to players twice
(`client/LevelRendererMixin` pinned the 1.20.1 descriptor of `renderLevel`, so every jar
above MC 1.20.4 crashed the client).

What this checks, per shipped mixin class, against the Minecraft jar the node runs on:

  1. the `@Mixin` target class exists;
  2. every `method = "..."` selector resolves to at least one method DECLARED in that
     target class (name, and descriptor when the selector pins one);
  3. every `@At(value = "INVOKE"/"INVOKE_ASSIGN"/"FIELD", target = "...")` reference
     actually appears in the bytecode of a matching host method.

Mapping is auto-detected, three ways, because the shipped jar is remapped and the names in
its mixin annotations are whatever the loader runs on:

  * **SRG** (`m_109599_`) -> loom's `minecraft-merged-srg-at-patched.jar`.
    Only `1.20.1-forge` and `1.20.4-forge`.
  * **intermediary** (`net/minecraft/class_1309`, `method_6091`) -> loom's
    `minecraft-merged-intermediary` jar. Every **obfuscated** Fabric node, i.e. all 15 below
    26.1. Checking one of these against a Mojmap jar reports every target class as missing —
    140 bogus failures, which is exactly what it did before this branch existed.
  * **Mojmap** (everything else: all NeoForge, Forge >=1.20.6, and the two unobfuscated
    Fabric nodes 26.1.2/26.2) -> the NeoForge merged jar of the same MC version, whose
    vanilla classes are identical on both loaders.

    python3 scripts/verify_mixin_targets.py               # jars of the current mod.version
    python3 scripts/verify_mixin_targets.py --all-versions
    python3 scripts/verify_mixin_targets.py --node 1.21.6-forge

Nonzero exit if any selector fails to resolve.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
INJECTOR_ANNOTATIONS = {
    "org.spongepowered.asm.mixin.injection.Inject",
    "org.spongepowered.asm.mixin.injection.Redirect",
    "org.spongepowered.asm.mixin.injection.ModifyArg",
    "org.spongepowered.asm.mixin.injection.ModifyArgs",
    "org.spongepowered.asm.mixin.injection.ModifyConstant",
    "org.spongepowered.asm.mixin.injection.ModifyVariable",
    "com.llamalad7.mixinextras.injector.ModifyExpressionValue",
    "com.llamalad7.mixinextras.injector.ModifyReturnValue",
    "com.llamalad7.mixinextras.injector.WrapWithCondition",
    "com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation",
}
# These select a member of some *other* class, not a method of the mixin target.
BYTECODE_AT_KINDS = {"INVOKE", "INVOKE_ASSIGN", "INVOKE_STRING", "FIELD", "NEW"}


def mod_version() -> str:
    text = (ROOT / "stonecutter.properties.toml").read_text()
    m = re.search(r'^mod\.version\s*=\s*"([^"]+)"', text, re.M)
    if not m:
        sys.exit("could not read mod.version from stonecutter.properties.toml")
    return m.group(1)


def javap(*args: str) -> str:
    return subprocess.run(
        ["javap", *args], capture_output=True, text=True, check=False
    ).stdout


# --------------------------------------------------------------------------- parsing


def _sections(dump: str) -> tuple[list[str], list[str], list[str]]:
    """javap -v output splits into header / member body / footer around the bare braces."""
    lines = dump.splitlines()
    try:
        open_i = next(i for i, l in enumerate(lines) if l.rstrip() == "{")
        close_i = max(i for i, l in enumerate(lines) if l.rstrip() == "}")
    except (StopIteration, ValueError):
        return lines, [], []
    return lines[:open_i], lines[open_i + 1 : close_i], lines[close_i + 1 :]


def _annotation_chunks(lines: list[str]) -> list[str]:
    """Every Runtime{,In}VisibleAnnotations block in a run of javap lines."""
    out: list[str] = []
    cur: list[str] | None = None
    header_indent = 0
    for line in lines:
        stripped = line.strip()
        indent = len(line) - len(line.lstrip())
        if stripped.startswith(("RuntimeVisibleAnnotations:", "RuntimeInvisibleAnnotations:")):
            if cur is not None:
                out.append("\n".join(cur))
            cur = []
            header_indent = indent
            continue
        if cur is None:
            continue
        if stripped and indent <= header_indent and re.match(r"^[A-Za-z][\w]*:", stripped):
            out.append("\n".join(cur))
            cur = None
            continue
        if stripped:
            cur.append(stripped)
    if cur is not None:
        out.append("\n".join(cur))
    return out


def member_name(decl: str) -> str | None:
    decl = decl.strip().rstrip(";")
    if decl.startswith("static {}"):
        return "<clinit>"
    head = decl.split("(", 1)[0].strip()
    if not head:
        return None
    last = head.split()[-1]
    if "." in last:
        return "<init>"
    return last


def dump_members(class_dump: str) -> list[dict]:
    """Every method of a javap -v dump: name, descriptor, annotation text, code text."""
    _, body, _ = _sections(class_dump)
    members: list[dict] = []
    cur: dict | None = None
    buf: list[str] = []
    for line in body:
        stripped = line.strip()
        indent = len(line) - len(line.lstrip())
        if indent == 2 and stripped.endswith(";") and "(" in stripped:
            if cur is not None:
                cur["lines"] = buf
                members.append(cur)
            cur, buf = {"decl": stripped, "name": member_name(stripped)}, []
            continue
        if indent == 2 and stripped == "static {};":
            if cur is not None:
                cur["lines"] = buf
                members.append(cur)
            cur, buf = {"decl": stripped, "name": "<clinit>"}, []
            continue
        if cur is not None:
            buf.append(line)
    if cur is not None:
        cur["lines"] = buf
        members.append(cur)
    for m in members:
        desc = [l.strip()[len("descriptor: "):] for l in m["lines"] if l.strip().startswith("descriptor: ")]
        m["descriptor"] = desc[0] if desc else None
        m["ann"] = _annotation_chunks(m["lines"])
        m["code"] = "\n".join(l.strip() for l in m["lines"])
    return members


def class_annotations(class_dump: str) -> list[str]:
    """Annotation chunks attached to the class itself (javap prints them outside the braces)."""
    header, _, footer = _sections(class_dump)
    # The constant pool lives in the header and is not annotation text; only the block
    # after the class declaration counts, and javap puts class annotations in the footer.
    return _annotation_chunks(footer) + _annotation_chunks(
        [l for l in header if not re.match(r"^\s*#\d+ = ", l)]
    )


def split_annotations(text: str) -> list[tuple[str, str]]:
    """(fully-qualified annotation type, body) for each annotation in a javap chunk."""
    joined = re.sub(r"\s+", " ", text)
    out: list[tuple[str, str]] = []
    for m in re.finditer(r"([\w.$]+)\(", joined):
        name = m.group(1)
        if "." not in name:
            continue
        depth = 0
        i = m.end() - 1
        for j in range(i, len(joined)):
            if joined[j] == "(":
                depth += 1
            elif joined[j] == ")":
                depth -= 1
                if depth == 0:
                    out.append((name.replace("$", "."), joined[i + 1 : j]))
                    break
    return out


def string_list(body: str, key: str) -> list[str]:
    """`key=["a","b"]` or `key="a"` out of a javap annotation body."""
    m = re.search(r"(?<![\w.])" + re.escape(key) + r"=\[(.*?)\]", body)
    if m:
        return re.findall(r'"([^"]*)"', m.group(1))
    m = re.search(r"(?<![\w.])" + re.escape(key) + r'="([^"]*)"', body)
    return [m.group(1)] if m else []


def class_list(body: str, key: str) -> list[str]:
    """`key=[class Lx/y/Z;]` -> ['x/y/Z']."""
    m = re.search(re.escape(key) + r"=\[(.*?)\]", body)
    chunk = m.group(1) if m else body
    return re.findall(r"class L([\w/$]+);", chunk)


# --------------------------------------------------------------------------- selectors


def parse_selector(sel: str) -> tuple[str | None, str, str | None]:
    """`Lowner;name(desc)ret` / `name(desc)ret` / `name` -> (owner, name, descriptor)."""
    owner = None
    if sel.startswith("L") and ";" in sel:
        owner, sel = sel[1:].split(";", 1)
    if "(" in sel:
        name, desc = sel.split("(", 1)
        return owner, name, "(" + desc
    return owner, sel, None


def normalize_at_target(target: str) -> str | None:
    """`Lowner;name(desc)ret` or `Lowner;name:Ltype;` -> javap's `owner.name:desc` form."""
    if not target.startswith("L") or ";" not in target:
        return None
    owner, rest = target[1:].split(";", 1)
    if "(" in rest:
        name, desc = rest.split("(", 1)
        return f"{owner}.{name}:(" + desc
    if ":" in rest:
        name, desc = rest.split(":", 1)
        return f"{owner}.{name}:{desc}"
    return None


# --------------------------------------------------------------------------- mc jars


def mc_jar_for(node: str, mc: str, mapping: str) -> Path | None:
    if mapping == "srg":
        base = Path.home() / ".gradle/caches/fabric-loom" / mc / "forge"
        for candidate in sorted(base.glob("*/minecraft-merged-srg-at-patched.jar")):
            return candidate
        for candidate in sorted(base.glob("*/minecraft-merged-srg-patched.jar")):
            return candidate
        return None
    if mapping == "intermediary":
        # One directory per (mc, mappings-hash); intermediary names do not depend on which
        # mappings layer produced it, so any of them answers "does class_1309 exist".
        # The `{mc}-*` glob cannot bleed across versions: it demands a literal '-' right
        # after the version, so `1.21.1-*` never matches a `1.21.10-…` directory.
        base = (
            Path.home()
            / ".gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-intermediary"
        )
        for candidate in sorted(base.glob(f"{mc}-*/*.jar")):
            return candidate
        return None
    # Mojmap: the NeoForge merged bundle for the same MC carries Mojmap vanilla classes.
    for loader_node in (f"{mc}-neoforge", f"{mc}-forge"):
        art = ROOT / "versions" / loader_node / "build/moddev/artifacts"
        for candidate in sorted(art.glob("neoforge-*-merged.jar")):
            return candidate
    base = Path.home() / ".gradle/caches/fabric-loom" / mc / "forge"
    for candidate in sorted(base.glob("*/minecraft-merged-patched.jar")):
        return candidate
    return None


class TargetJar:
    def __init__(self, path: Path, workdir: Path):
        self.path = path
        self.zip = zipfile.ZipFile(path)
        self.names = set(self.zip.namelist())
        self.workdir = workdir
        self._dumps: dict[str, list[dict] | None] = {}

    def members(self, internal: str) -> list[dict] | None:
        if internal in self._dumps:
            return self._dumps[internal]
        entry = internal + ".class"
        if entry not in self.names:
            self._dumps[internal] = None
            return None
        out = self.workdir / entry
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_bytes(self.zip.read(entry))
        dump = javap("-v", "-p", "-cp", str(self.workdir), internal.replace("/", "."))
        parsed = dump_members(dump) if dump else None
        self._dumps[internal] = parsed
        return parsed


# --------------------------------------------------------------------------- checking


def check_jar(jar_path: Path, node: str, mc: str, verbose: bool) -> tuple[int, int]:
    problems: list[str] = []
    checked = 0
    with zipfile.ZipFile(jar_path) as jar, tempfile.TemporaryDirectory() as tmp:
        tmpdir = Path(tmp)
        cfgs = [n for n in jar.namelist() if n.endswith(".mixins.json") and "/" not in n]
        mixin_classes: list[str] = []
        for cfg_name in cfgs:
            cfg = json.loads(jar.read(cfg_name))
            pkg = cfg.get("package", "").replace(".", "/")
            for key in ("mixins", "client", "server"):
                for entry in cfg.get(key, []):
                    internal = f"{pkg}/{entry.replace('.', '/')}"
                    if internal not in mixin_classes:
                        mixin_classes.append(internal)

        mixin_dir = tmpdir / "mixins"
        dumps: dict[str, str] = {}
        for internal in mixin_classes:
            entry = internal + ".class"
            if entry not in jar.namelist():
                continue  # verify_mixins.py owns this failure mode
            out = mixin_dir / entry
            out.parent.mkdir(parents=True, exist_ok=True)
            out.write_bytes(jar.read(entry))
        for internal in mixin_classes:
            if not (mixin_dir / (internal + ".class")).exists():
                continue
            dumps[internal] = javap("-v", "-p", "-cp", str(mixin_dir), internal.replace("/", "."))

        if any(re.search(r"\bm_\d+_\b|\bf_\d+_\b", d) for d in dumps.values()):
            mapping = "srg"
        elif any(
            re.search(r"\bclass_\d+\b|\bmethod_\d+\b|\bfield_\d+\b", d) for d in dumps.values()
        ):
            mapping = "intermediary"
        else:
            mapping = "mojmap"
        mcjar = mc_jar_for(node, mc, mapping)
        if mcjar is None:
            print(f"  ! no Minecraft jar found for {node} (mapping={mapping}) — SKIPPED")
            return 0, 0
        target = TargetJar(mcjar, tmpdir / "mc")
        if verbose:
            print(f"  mapping={mapping} against {mcjar.name}")

        for internal, dump in dumps.items():
            short = internal.split("/", 5)[-1]
            targets: list[str] = []
            for chunk in class_annotations(dump):
                for ann, body in split_annotations(chunk):
                    if ann != "org.spongepowered.asm.mixin.Mixin":
                        continue
                    targets += class_list(body, "value")
                    targets += [t.replace(".", "/") for t in string_list(body, "targets")]
            if not targets:
                continue
            for tgt in targets:
                members = target.members(tgt)
                if members is None:
                    problems.append(f"{short}: @Mixin target class not in {mcjar.name}: {tgt}")
                    continue
                by_name: dict[str, list[str]] = {}
                for m in members:
                    if m["name"] and m["descriptor"]:
                        by_name.setdefault(m["name"], []).append(m["descriptor"])

                for m in dump_members(dump):
                    for chunk in m["ann"]:
                      anns = split_annotations(chunk)
                      for ann, body in anns:
                        if ann not in INJECTOR_ANNOTATIONS:
                            continue
                        selectors = string_list(body, "method")
                        at_kind = None
                        at_target = None
                        # @At is nested inside the injector body; split_annotations
                        # already balanced it out for us.
                        for sub_ann, sub_body in split_annotations(body):
                            if sub_ann.endswith("injection.At"):
                                at_kind = (string_list(sub_body, "value") or [None])[0]
                                at_target = (string_list(sub_body, "target") or [None])[0]
                                break
                        for sel in selectors:
                            checked += 1
                            owner, name, desc = parse_selector(sel)
                            if owner and owner != tgt:
                                problems.append(
                                    f"{short}#{m['name']}: selector owner {owner} != @Mixin target {tgt}"
                                )
                                continue
                            descs = by_name.get(name)
                            if not descs:
                                problems.append(
                                    f"{short}#{m['name']}: no method named '{name}' declared in {tgt}"
                                )
                                continue
                            if desc and not any(d.startswith(desc) for d in descs):
                                problems.append(
                                    f"{short}#{m['name']}: '{name}{desc}' not found in {tgt} "
                                    f"(present: {', '.join(sorted(set(descs)))})"
                                )
                                continue
                            if at_kind in BYTECODE_AT_KINDS and at_target:
                                needle = normalize_at_target(at_target)
                                if needle:
                                    hosts = [
                                        x
                                        for x in members
                                        if x["name"] == name
                                        and (not desc or (x["descriptor"] or "").startswith(desc))
                                    ]
                                    body_text = "\n".join(h["code"] for h in hosts)
                                    # javap OMITS the owner on a same-class reference: an external
                                    # call prints `// Method net/minecraft/class_4184.method_19326:()…`
                                    # but a self-call prints `// Method method_71110:(…)V`. So when the
                                    # @At target's owner IS the @Mixin target, the owner-qualified
                                    # needle can never match and the check reports a false FAIL.
                                    # Accept the owner-less form too — but ONLY in that case, so a
                                    # reference to some other class's same-named member still fails.
                                    # (Found by FabricFogRendererMixin's @ModifyArgs on FogRenderer's
                                    # own private updateBuffer — the tree's first self-call @At.)
                                    if needle not in body_text and needle.startswith(f"{tgt}."):
                                        bare = needle[len(tgt) + 1 :]
                                        # Anchor to javap's `// Method `/`// Field ` lead-in. A plain
                                        # substring test would also match `…/class_999.method_71110:(…`
                                        # — i.e. some OTHER class's same-named member — which is
                                        # exactly the drift this verifier exists to catch.
                                        if re.search(
                                            r"(?:^|\s)(?:Method|Field|InterfaceMethod) "
                                            + re.escape(bare),
                                            body_text,
                                        ):
                                            needle = None
                                    if needle is not None and needle not in body_text:
                                        problems.append(
                                            f"{short}#{m['name']}: @At({at_kind}) target "
                                            f"'{needle}' not present in {tgt}.{name}"
                                        )
    for p in problems:
        print(f"  FAIL {p}")
    return checked, len(problems)


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--all-versions", action="store_true")
    ap.add_argument("--node", action="append", default=None)
    ap.add_argument("-v", "--verbose", action="store_true")
    ap.add_argument(
        "jar",
        nargs="*",
        help="explicit jar paths (node inferred from the alexsmobs-<v>-<loader>+<mc> name)",
    )
    args = ap.parse_args()

    total_checked = 0
    total_problems = 0
    jars = 0
    nodes_seen = []
    skipped = []

    if args.jar:
        for raw in args.jar:
            jar = Path(raw)
            m = re.match(r"alexsmobs-.*-(\w+)\+(.+)\.jar$", jar.name)
            if not m:
                print(f"{jar.name}: cannot infer node from filename — SKIPPED")
                continue
            loader, mc = m.group(1), m.group(2)
            node = f"{mc}-{loader}"
            jars += 1
            print(f"{node}: {jar.name}")
            checked, problems = check_jar(jar, node, mc, args.verbose)
            total_checked += checked
            total_problems += problems
            if not problems and args.verbose:
                print(f"  ok ({checked} selectors)")
        print(f"\njars={jars} selectors={total_checked} problems={total_problems}")
        return 1 if total_problems else 0

    version = None if args.all_versions else mod_version()
    for node_dir in sorted((ROOT / "versions").iterdir()):
        node = node_dir.name
        if args.node and node not in args.node:
            continue
        mc, _, loader = node.rpartition("-")
        nodes_seen.append(node)
        libs = node_dir / "build/libs"
        if not libs.is_dir():
            continue
        before = jars
        # The trailing `*` is load-bearing: a dev build appends `-SNAPSHOT` (only
        # MOD_IS_RELEASE=true omits it). Without it this glob matched release-named jars
        # ONLY, so a normal `./gradlew build` produced jars this verifier never opened —
        # and it still printed `jars=49`, which reads as full coverage. It was instead
        # re-validating whatever release jars happened to be lying in build/libs, of any
        # age. Caught 2026-08-01 when Wave 4 added three selectors and the count did not
        # move. Keep the sources/javadoc filter below; `*` now lets those names through.
        for jar in sorted(libs.glob(f"alexsmobs-*-{loader}+{mc}*.jar")):
            if "-sources" in jar.name or "-javadoc" in jar.name:
                continue
            if version and f"-{version}-" not in jar.name:
                continue
            jars += 1
            print(f"{node}: {jar.name}")
            checked, problems = check_jar(jar, node, mc, args.verbose)
            total_checked += checked
            total_problems += problems
            if not problems and args.verbose:
                print(f"  ok ({checked} selectors)")
        if jars == before:
            skipped.append(node)

    # A node that yielded no jar used to just vanish from the run, which is how the
    # SNAPSHOT glob bug stayed invisible: nothing distinguished "49 nodes verified" from
    # "49 stale jars verified and N fresh nodes silently skipped". Name them and fail.
    if skipped:
        print(f"\n*** {len(skipped)} node(s) had build/libs but no matching jar — "
              f"NOT verified: {', '.join(skipped)}")
        print("    Build them first, or pass --node to scope the run deliberately.")

    print(f"\nnodes={len(nodes_seen)} jars={jars} selectors={total_checked} "
          f"problems={total_problems} skipped={len(skipped)}")
    return 1 if (total_problems or skipped) else 0


if __name__ == "__main__":
    sys.exit(main())

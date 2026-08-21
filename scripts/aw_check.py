#!/usr/bin/env python3
"""Validate a Fabric access widener against the UNPATCHED Mojmap Minecraft jars.

Why this exists: unlike a Forge access transformer, an access-widener entry naming a member
that does not exist is a HARD error (loom's `validateAccessWidener` refuses the build), and
the members this mod widens move between MC versions — `Camera#move` changed descriptor,
`AbstractArrow#setPierceLevel` changed package, and several entries exist only because
Forge/NeoForge ship a PATCHED jar in which they are already public. So a single widener
cannot serve the whole 1.20.1 -> 1.21.11 range and every candidate entry has to be checked
against the real bytecode, per node, before a node is added.

Running the Gradle task would answer the same question, but only one node at a time and only
after a full loom setup; this answers it for every cached version in a couple of seconds, and
prints the descriptor it DID find so a moved member can be fixed rather than merely deleted.

It understands the `#?` predicate directives of the generator's source template
(accesswidener/alexsmobs.accesswidener, expanded per node by
build-logic/AccessWidener.kt), so pointing it at the TEMPLATE checks each version against
exactly the entries that version would be given. Pointed at a plain widener it behaves as
before, since a file with no directives gates nothing.

Usage:
    scripts/aw_check.py <accesswidener-file> <mc-version> [<mc-version> ...]
    scripts/aw_check.py --list-jars <mc-version>

It reads the named-namespace (Mojmap) jars loom has already downloaded under
~/.gradle/caches/fabric-loom/minecraftMaven, deliberately skipping any artifact whose
version string carries a loader name — those are the loader's patched jars, in which
everything this script is trying to detect is already public.
"""

import os
import re
import subprocess
import sys
import zipfile
from pathlib import Path

LOOM_MAVEN = Path.home() / ".gradle/caches/fabric-loom/minecraftMaven/net/minecraft"

# The three artifacts that together hold the whole game. A split-source version publishes
# common + clientonly; an older one publishes a single merged jar.
ARTIFACTS = ("minecraft-common", "minecraft-clientonly", "minecraft-merged")


def vanilla_jars(mc: str) -> list[Path]:
    """Named-namespace jars for `mc`, excluding any loader-patched variant."""
    found: list[Path] = []
    for artifact in ARTIFACTS:
        base = LOOM_MAVEN / artifact
        if not base.is_dir():
            continue
        for version_dir in base.iterdir():
            name = version_dir.name
            if not name.startswith(f"{mc}-"):
                continue
            # "1.21.11-loom.mappings...-v2-forge-1.21.11-61.1.0" is Forge's PATCHED jar, and
            # "-intermediary" is the wrong namespace. Both would hide exactly the differences
            # this script exists to find.
            if "-forge-" in name or "-neoforge-" in name or "intermediary" in artifact:
                continue
            found.extend(sorted(version_dir.glob("*.jar")))
    return found


def class_entries(jars: list[Path]) -> dict[str, Path]:
    """internal/class/Name -> the jar that holds it."""
    index: dict[str, Path] = {}
    for jar in jars:
        with zipfile.ZipFile(jar) as zf:
            for entry in zf.namelist():
                if entry.endswith(".class"):
                    index.setdefault(entry[:-6], jar)
    return index


_JAVAP_CACHE: dict[tuple[str, str], list[str]] = {}


def javap(cls: str, classpath: str) -> list[str]:
    key = (cls, classpath)
    if key not in _JAVAP_CACHE:
        proc = subprocess.run(
            ["javap", "-p", "-s", "-cp", classpath, cls.replace("/", ".")],
            capture_output=True,
            text=True,
        )
        _JAVAP_CACHE[key] = proc.stdout.splitlines() if proc.returncode == 0 else []
    return _JAVAP_CACHE[key]


def members(cls: str, classpath: str) -> dict[str, set[str]]:
    """name -> {descriptors}, from `javap -p -s` output (declaration line, then descriptor)."""
    out: dict[str, set[str]] = {}
    lines = javap(cls, classpath)
    simple = cls.rsplit("/", 1)[-1].rsplit("$", 1)[-1]
    for i, line in enumerate(lines):
        m = re.search(r"descriptor: (\S+)", line)
        if not m or i == 0:
            continue
        decl = lines[i - 1].strip().rstrip(";")
        # A constructor's declaration names the class rather than a member.
        if re.search(rf"\b{re.escape(simple)}\s*\(", decl):
            out.setdefault("<init>", set()).add(m.group(1))
        token = re.split(r"[(\s]", decl.split("(")[0].strip())[-1]
        out.setdefault(token, set()).add(m.group(1))
    return out


def version_tuple(v: str) -> tuple[int, ...]:
    return tuple(int(part) for part in re.findall(r"\d+", v))


def compare(a: str, b: str) -> int:
    """Stonecutter's version ordering, which is plain component-wise numeric comparison.

    Zero-padding matters: "1.21" and "1.21.0" must compare equal, and "26" must outrank
    "1.21.11" (26 > 1 on the first component), which is exactly why this is not a string sort.
    """
    ta, tb = version_tuple(a), version_tuple(b)
    width = max(len(ta), len(tb))
    ta += (0,) * (width - len(ta))
    tb += (0,) * (width - len(tb))
    return (ta > tb) - (ta < tb)


_OPS = {
    ">=": lambda c: c >= 0,
    "<=": lambda c: c <= 0,
    "==": lambda c: c == 0,
    ">": lambda c: c > 0,
    "<": lambda c: c < 0,
    "=": lambda c: c == 0,
}


def _atom(mc: str, token: str) -> bool:
    for op in (">=", "<=", "==", ">", "<", "="):
        if token.startswith(op):
            return _OPS[op](compare(mc, token[len(op):]))
    raise ValueError(f"not a version predicate: {token!r}")


def evaluate(mc: str, expr: str) -> bool:
    """`&&` / `||` / `!` / parens over version predicates — the Python twin of
    `evalVersionExpr` in build-logic/AccessWidener.kt. Keep the two in step: a template that
    passes here and fails there (or vice versa) is worse than no checker at all."""
    tokens = re.findall(r"\(|\)|&&|\|\||!|[^()\s!&|]+", expr)
    if not tokens:
        raise ValueError("empty version predicate")
    pos = 0

    def parse_or() -> bool:
        nonlocal pos

        def parse_and() -> bool:
            nonlocal pos

            def parse_unary() -> bool:
                nonlocal pos
                token = tokens[pos] if pos < len(tokens) else None
                if token is None:
                    raise ValueError(f"unexpected end of predicate in {expr!r}")
                if token == "!":
                    pos += 1
                    return not parse_unary()
                if token == "(":
                    pos += 1
                    inner = parse_or()
                    if pos >= len(tokens) or tokens[pos] != ")":
                        raise ValueError(f"unbalanced '(' in {expr!r}")
                    pos += 1
                    return inner
                if token in (")", "&&", "||"):
                    raise ValueError(f"unexpected {token!r} in {expr!r}")
                pos += 1
                return _atom(mc, token)

            acc = parse_unary()
            while pos < len(tokens) and tokens[pos] == "&&":
                pos += 1
                acc = parse_unary() and acc
            return acc

        acc = parse_and()
        while pos < len(tokens) and tokens[pos] == "||":
            pos += 1
            acc = parse_and() or acc
        return acc

    result = parse_or()
    if pos != len(tokens):
        raise ValueError(f"trailing {' '.join(tokens[pos:])!r} in {expr!r}")
    return result


def active_entries(aw_path: Path, mc: str) -> list[tuple[int, str]]:
    """(lineno, entry) for every line the generator would emit for `mc`."""
    out: list[tuple[int, str]] = []
    block: bool | None = None
    pending: bool | None = None
    for lineno, raw in enumerate(aw_path.read_text().splitlines(), 1):
        line = raw.strip()
        if not line:
            continue
        if line.startswith("#?}"):
            block, pending = None, None
        elif line.startswith("#?{"):
            block = evaluate(mc, line[3:].strip())
        elif line.startswith("#?"):
            pending = evaluate(mc, line[2:].strip())
        elif line.startswith("#") or line.startswith("accessWidener"):
            continue
        else:
            gate = pending if pending is not None else (block if block is not None else True)
            pending = None
            if gate:
                out.append((lineno, line.split("#", 1)[0].strip()))
    return out


def check(aw_path: Path, mc: str) -> int:
    jars = vanilla_jars(mc)
    if not jars:
        print(f"{mc}: no cached vanilla named jars — build a node for it first, skipping")
        return 0
    classpath = ":".join(str(j) for j in jars)
    index = class_entries(jars)

    problems = 0
    # Only the entries THIS version would be given — a template's other arms name members that are
    # supposed to be absent here, and reporting them would drown the real findings.
    for lineno, line in active_entries(aw_path, mc):
        parts = line.split()
        if len(parts) < 3:
            continue
        _access, kind, owner = parts[0], parts[1], parts[2]

        if owner not in index:
            print(f"{mc}: {aw_path.name}:{lineno}: CLASS ABSENT  {owner}")
            problems += 1
            continue
        if kind == "class":
            continue

        name, want = parts[3], parts[4]
        have = members(owner, classpath)
        if name not in have:
            print(f"{mc}: {aw_path.name}:{lineno}: MEMBER ABSENT {owner}#{name}")
            problems += 1
        elif want not in have[name]:
            print(
                f"{mc}: {aw_path.name}:{lineno}: DESCRIPTOR MOVED {owner}#{name}\n"
                f"      declared {want}\n"
                f"      found    {' | '.join(sorted(have[name]))}"
            )
            problems += 1

    print(f"{mc}: {aw_path.name} problems={problems}")
    return problems


def main() -> int:
    args = sys.argv[1:]
    if args and args[0] == "--list-jars":
        for jar in vanilla_jars(args[1]):
            print(jar)
        return 0
    if len(args) < 2:
        print(__doc__)
        return 2
    aw = Path(args[0])
    return 1 if sum(check(aw, mc) for mc in args[1:]) else 0


if __name__ == "__main__":
    raise SystemExit(main())

#!/usr/bin/env python3
"""Two static checks on the mixins a jar ships. Both catch a HARD CRASH at game start.

**1. Every mixin a jar DECLARES must actually be SHIPPED in it.** A config naming an absent
class fails with `InvalidMixinException: The specified mixin '…' was not found` in
`MixinProcessor.prepareConfigs`, i.e. the game dies before FML even starts. This is the check
that would have caught the bug that shipped broken in 1.0.2 and 1.0.3 (nine `<1.21.2` nodes,
~150 downloads that could not launch). Both the `mixins` and `client` arrays are checked:
Fletching Table populates `mixins` itself from an `@Mixin` source scan that does NOT honour the
source-set `exclude`, so an excluded class can be declared without ever being compiled.

**2. No `@Mixin` may target one of THIS MOD's own classes.** Mixing into your own code is never
what you want here, and it is exactly what a Stonecutter `replacements` rule can do behind your
back: `!mc2102-render-import-entity` rewrites `import net.minecraft.client.renderer.entity.
EntityRenderer;` to the mod's `client.render.compat.EntityRenderer` on every >=1.21.2 node, and
it has no idea the file it just edited is a mixin. That retargeted `@Mixin` at the compat class
and crashed all nine of those nodes with `InvalidInjectionException: Invalid descriptor`. It
compiles clean, and check 1 sees nothing wrong, so only reading the shipped bytecode finds it.

    python3 scripts/verify_mixins.py                  # every jar of the current mod.version
    python3 scripts/verify_mixins.py --all-versions   # every jar present, any version

Exit status is nonzero if any jar fails either check.
"""
import sys, os, re, json, glob, struct, zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)


def mod_version():
    with open(os.path.join(ROOT, "stonecutter.properties.toml")) as f:
        for line in f:
            if line.strip().startswith("mod.version"):
                return line.split("=", 1)[1].strip().strip('"')
    raise SystemExit("mod.version not found")


MOD_PKG = "com/github/alexthe666/"


def _parse_cp(b):
    """Constant pool -> {index: value}. Only Utf8 (1) and Class (7) matter here."""
    n = struct.unpack_from(">H", b, 8)[0]
    off, cp, i = 10, {}, 1
    while i < n:
        tag = b[off]
        off += 1
        if tag == 1:                       # Utf8
            ln = struct.unpack_from(">H", b, off)[0]
            cp[i] = b[off + 2:off + 2 + ln].decode("utf-8", "replace")
            off += 2 + ln
        elif tag == 7:                     # Class -> name index, resolved lazily below
            cp[i] = ("Class", struct.unpack_from(">H", b, off)[0])
            off += 2
        elif tag in (5, 6):                # Long/Double take two slots
            off += 8
            i += 1
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18):
            off += 4
        elif tag in (8, 16, 19, 20):
            off += 2
        elif tag == 15:
            off += 3
        else:
            raise ValueError(f"unknown constant pool tag {tag}")
        i += 1
    return cp, off


def _skip_attrs(b, off):
    n = struct.unpack_from(">H", b, off)[0]
    off += 2
    for _ in range(n):
        ln = struct.unpack_from(">I", b, off + 2)[0]
        off += 6 + ln
    return off


def _skip_members(b, off):
    n = struct.unpack_from(">H", b, off)[0]
    off += 2
    for _ in range(n):
        off = _skip_attrs(b, off + 6)
    return off


def _ev(b, off, cp, out):
    """Walk one element_value, collecting every class/string it mentions."""
    tag = chr(b[off])
    off += 1
    if tag == "@":
        return _annotation(b, off, cp, out)
    if tag == "[":
        n = struct.unpack_from(">H", b, off)[0]
        off += 2
        for _ in range(n):
            off = _ev(b, off, cp, out)
        return off
    if tag == "e":
        return off + 4
    idx = struct.unpack_from(">H", b, off)[0]
    if tag in ("c", "s"):
        out.append(cp.get(idx))
    return off + 2


def _annotation(b, off, cp, out):
    type_idx, npairs = struct.unpack_from(">HH", b, off)
    off += 4
    mine = cp.get(type_idx) == "Lorg/spongepowered/asm/mixin/Mixin;"
    for _ in range(npairs):
        off += 2                            # element_name_index
        off = _ev(b, off, cp, out if mine else [])
    return off


def mixin_targets(class_bytes):
    """The classes a shipped mixin's @Mixin annotation actually points at, from bytecode.

    Reads the CLASS-level annotations only, so a mod class merely referenced in a handler body
    (AMStateAccess, say) is never mistaken for a target. Covers both `value = X.class` (a `c`
    element, a descriptor) and `targets = "a.b.C"` (an `s` element, a dotted name).
    """
    cp, off = _parse_cp(class_bytes)
    off += 6                                              # access_flags, this_class, super_class
    off += 2 + 2 * struct.unpack_from(">H", class_bytes, off)[0]   # interfaces
    off = _skip_members(class_bytes, off)                  # fields
    off = _skip_members(class_bytes, off)                  # methods
    n = struct.unpack_from(">H", class_bytes, off)[0]      # class attributes
    off += 2
    found = []
    for _ in range(n):
        name_idx, ln = struct.unpack_from(">HI", class_bytes, off)
        body = off + 6
        if cp.get(name_idx) in ("RuntimeInvisibleAnnotations", "RuntimeVisibleAnnotations"):
            cnt = struct.unpack_from(">H", class_bytes, body)[0]
            p = body + 2
            for _ in range(cnt):
                p = _annotation(class_bytes, p, cp, found)
        off = body + ln
    return [t.lstrip("L").rstrip(";").replace(".", "/") for t in found if t]


def main():
    ver = None if "--all-versions" in sys.argv[1:] else mod_version()
    pat = os.path.join(ROOT, "versions", "*", "build", "libs", "*.jar")
    jars = sorted(p for p in glob.glob(pat)
                  if not p.endswith(("-sources.jar", "-javadoc.jar"))
                  and (ver is None or f"-{ver}-" in os.path.basename(p)))
    if not jars:
        raise SystemExit(f"No jars found for version {ver or '(any)'} under versions/*/build/libs/")

    bad = 0
    for jar in jars:
        node = jar.split(os.sep + "versions" + os.sep)[1].split(os.sep)[0]
        with zipfile.ZipFile(jar) as z:
            names = set(z.namelist())
            cfgs = [n for n in names if re.fullmatch(r"[^/]+\.mixins\.json", n)]
            if not cfgs:
                print(f"FAIL {node:20} no *.mixins.json in jar")
                bad += 1
                continue
            for cfg in cfgs:
                j = json.loads(z.read(cfg))
                pkg = j.get("package", "")
                declared = list(j.get("mixins", [])) + list(j.get("client", [])) \
                    + list(j.get("server", []))
                missing, self_targeted = [], []
                for c in declared:
                    entry = f"{pkg}.{c}".replace(".", "/") + ".class"
                    if entry not in names:
                        missing.append(c)
                        continue
                    for tgt in mixin_targets(z.read(entry)):
                        if tgt.startswith(MOD_PKG):
                            self_targeted.append(f"{c} -> {tgt}")
                status = "FAIL" if (missing or self_targeted) else "ok  "
                print(f"{status} {node:20} {cfg}  declared={len(declared):2} missing={len(missing)}"
                      f" self-targeted={len(self_targeted)}"
                      + (f"  missing -> {missing}" if missing else "")
                      + (f"  self -> {self_targeted}" if self_targeted else ""))
                if missing or self_targeted:
                    bad += 1
    print(f"\njars={len(jars)}  problems={bad}")
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())

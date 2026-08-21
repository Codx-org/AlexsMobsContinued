#!/usr/bin/env python3
"""Find SILENTLY-DEAD OVERRIDES: a mod method that used to override a vanilla one and no
longer does, because the vanilla signature changed on some MC version.

This is the failure mode porting rule 10 warns about, in its non-mixin form. Upstream Alex's
Mobs almost never writes `@Override`, so when Mojang widens a parameter the subclass method
quietly becomes an unrelated *overload*: it compiles clean on every node, the compiler has
nothing to say, no gate fails, and the behaviour just stops happening. It cost three blocks at
once when 1.21.5 widened `Block#fallOn`'s fall distance from float to double -- the leafcutter
anthill stopped angering its ants, and both egg blocks stopped being trampled, on 27 nodes.

The check: for every compiled mod class, walk its superclass chain into vanilla. If the mod
declares (name, descriptor) and some ancestor declares that NAME with a different DESCRIPTOR,
while NO ancestor declares that exact (name, descriptor) pair, the method overrides nothing --
report it.

    ./gradlew :26.2-neoforge:compileJava            # the classes must exist first
    python3 scripts/verify_overrides.py 26.2-neoforge [more nodes...]

Deliberate overloads are legitimate and will show up too, so this is a REVIEW list, not a
pass/fail gate -- read every hit and decide. Exit status is nonzero when anything is reported.

BLIND SPOT: this only sees same-NAME/different-DESCRIPTOR pairs, so a vanilla method that changes
its NAME is invisible -- the mod's version just looks like a method it invented. That is how
`MobEffect#isDurationEffectTick` -> `shouldApplyEffectTickThisTick` (1.20.2) escaped it, and that
one returns FALSE by default, so it did not merely lose an override, it switched 18 potion effects
off. Treat the output as a floor, not a ceiling; renames still need the descriptor diff (sigdiff.py)
or a hand read. See docs/notes/bug-reports.md #66.
"""
import sys, os, glob, struct, zipfile, re

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
MOD_PKG = "com/github/alexthe666/"

# Names that are overloaded on purpose all over vanilla and this mod, where a same-name
# different-descriptor pair says nothing. Keeping them would drown the real hits.
BENIGN = {"<init>", "<clinit>", "valueOf", "values", "equals", "hashCode", "toString"}


def _parse_class(b):
    """(this_name, super_name, [(mname, mdesc, access)]) straight out of the class file."""
    n = struct.unpack_from(">H", b, 8)[0]
    off, cp, i = 10, {}, 1
    while i < n:
        tag = b[off]
        off += 1
        if tag == 1:
            ln = struct.unpack_from(">H", b, off)[0]
            cp[i] = b[off + 2:off + 2 + ln].decode("utf-8", "replace")
            off += 2 + ln
        elif tag == 7:
            cp[i] = ("Class", struct.unpack_from(">H", b, off)[0])
            off += 2
        elif tag in (5, 6):
            off += 8
            i += 1
        elif tag in (15,):
            off += 3
        elif tag in (16, 19, 20):
            off += 2
        elif tag in (8,):
            off += 2
        else:
            off += 4
        i += 1

    def cls(idx):
        v = cp.get(idx)
        return cp.get(v[1]) if isinstance(v, tuple) else None

    off += 2                                             # access_flags
    this_name = cls(struct.unpack_from(">H", b, off)[0]); off += 2
    super_name = cls(struct.unpack_from(">H", b, off)[0]); off += 2
    ifc = struct.unpack_from(">H", b, off)[0]; off += 2 + 2 * ifc

    def skip_attrs(off):
        cnt = struct.unpack_from(">H", b, off)[0]; off += 2
        for _ in range(cnt):
            ln = struct.unpack_from(">I", b, off + 2)[0]
            off += 6 + ln
        return off

    fcnt = struct.unpack_from(">H", b, off)[0]; off += 2
    for _ in range(fcnt):
        off = skip_attrs(off + 6)

    methods = []
    mcnt = struct.unpack_from(">H", b, off)[0]; off += 2
    for _ in range(mcnt):
        acc = struct.unpack_from(">H", b, off)[0]
        nm = cp.get(struct.unpack_from(">H", b, off + 2)[0])
        ds = cp.get(struct.unpack_from(">H", b, off + 4)[0])
        methods.append((nm, ds, acc))
        off = skip_attrs(off + 6)
    return this_name, super_name, methods


def load_dir(path):
    out = {}
    for f in glob.glob(os.path.join(path, "**", "*.class"), recursive=True):
        with open(f, "rb") as fh:
            try:
                name, sup, ms = _parse_class(fh.read())
            except Exception:
                continue
        if name:
            out[name] = (sup, ms)
    return out


def load_jar(path, index):
    try:
        z = zipfile.ZipFile(path)
    except Exception:
        return
    for n in z.namelist():
        if not n.endswith(".class"):
            continue
        try:
            name, sup, ms = _parse_class(z.read(n))
        except Exception:
            continue
        if name and name not in index:
            index[name] = (sup, ms)


def mc_of(node):
    return node.rsplit("-", 1)[0]


def vanilla_jars(mc):
    """Every NAMED merged jar loom cached for this MC version.

    The artifact id moved over the range -- `minecraft-merged` with a mappings-stamped version
    directory on the older half, `minecraft-merged-deobf/<mc>` on the newer -- so match on the
    version directory rather than the id, and drop the intermediary/srg twins, which carry
    obfuscated member names and would make every method look absent.
    """
    base = os.path.expanduser("~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft")
    out = []
    for j in glob.glob(f"{base}/*/*/*.jar"):
        vdir = os.path.basename(os.path.dirname(j))
        if vdir != mc and not vdir.startswith(mc + "-"):
            continue
        b = os.path.basename(j)
        if "sources" in b or "-intermediary" in b or "-srg" in b:
            continue
        out.append(j)
    return out


def check(node):
    cdir = os.path.join(ROOT, "versions", node, "build", "classes", "java", "main")
    if node == active_node():
        alt = os.path.join(ROOT, "build", "classes", "java", "main")
        if not os.path.isdir(cdir) and os.path.isdir(alt):
            cdir = alt
    if not os.path.isdir(cdir):
        print(f"  {node}: SKIP (no compiled classes -- run :{node}:compileJava)")
        return None

    mod = load_dir(cdir)
    index = dict(mod)
    jars = vanilla_jars(mc_of(node))
    if not jars:
        print(f"  {node}: SKIP (no cached mapped jar for MC {mc_of(node)})")
        return None
    for j in jars:
        load_jar(j, index)

    hits = {}
    for name, (sup, ms) in sorted(mod.items()):
        if not name.startswith(MOD_PKG):
            continue
        chain, cur, seen = [], sup, set()
        while cur and cur in index and cur not in seen:
            seen.add(cur)
            chain.append(cur)
            cur = index[cur][0]
        if not chain:
            continue
        anc = {}
        for c in chain:
            for mn, md, acc in index[c][1]:
                if acc & 0x0008 or acc & 0x0002:         # skip static and private
                    continue
                anc.setdefault(mn, set()).add(params(md))

        # Descriptors this class itself declares, so the >=X gated form that DOES override and
        # delegates to the legacy one (Item#getUseDuration is the pattern) is not a false hit.
        own = {}
        for mn, md, acc in ms:
            own.setdefault(mn, set()).add(params(md))

        for mn, md, acc in ms:
            if mn in BENIGN or acc & 0x0008 or acc & 0x0002 or acc & 0x1000:
                continue
            if mn not in anc:
                continue
            p = params(md)
            if any(compatible(p, a) for a in anc[mn]):
                continue
            if any(any(compatible(o, a) for a in anc[mn]) for o in own[mn]):
                continue                                  # a sibling overload covers it
            hits[(name, mn, p)] = sorted(anc[mn])
    return hits


def params(desc):
    """Parameter list only -- a covariant return type is still a real override."""
    return desc[:desc.rindex(")") + 1]


def compatible(a, b):
    """Same arity, and every differing pair explained by generic erasure to Object."""
    if a == b:
        return True
    ta, tb = split_params(a), split_params(b)
    if len(ta) != len(tb):
        return False
    return all(x == y or y == "Ljava/lang/Object;" or x == "Ljava/lang/Object;"
               for x, y in zip(ta, tb))


def split_params(p):
    out, i, s = [], 1, p[1:-1]
    i = 0
    while i < len(s):
        j = i
        while s[j] == "[":
            j += 1
        if s[j] == "L":
            k = s.index(";", j)
            out.append(s[i:k + 1])
            i = k + 1
        else:
            out.append(s[i:j + 1])
            i = j + 1
    return out


def active_node():
    try:
        with open(os.path.join(ROOT, "stonecutter.gradle.kts")) as f:
            m = re.search(r'stonecutter\s+active\s+"([^"]+)"', f.read())
            return m.group(1) if m else None
    except Exception:
        return None


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    baseline = None
    for a in sys.argv[1:]:
        if a.startswith("--baseline="):
            baseline = a.split("=", 1)[1]
    if not args:
        raise SystemExit(__doc__)

    base_hits = None
    if baseline:
        print(f"== baseline {baseline}")
        base_hits = check(baseline)
        print(f"  -> {len(base_hits or {})} dead override(s) upstream already had\n")

    total = 0
    for node in args:
        print(f"== {node}")
        hits = check(node)
        if hits is None:
            continue
        if base_hits is not None:
            # A method dead on the 1.20.1 baseline too is upstream's own vestige, not a port
            # regression -- the causeFallDamage(float,float) family has been dead since 1.17.
            hits = {k: v for k, v in hits.items() if k not in base_hits}
        for (cls, mn, p), others in sorted(hits.items()):
            print(f"  {cls}")
            print(f"      declares  {mn}{p}")
            for o in others:
                print(f"      ancestor  {mn}{o}")
        total += len(hits)
        print(f"  -> {len(hits)} regression(s)")
    print(f"\nnodes={len(args)} regressions={total}")
    return 1 if total else 0


if __name__ == "__main__":
    sys.exit(main())

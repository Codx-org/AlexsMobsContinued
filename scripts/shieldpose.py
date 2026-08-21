#!/usr/bin/env python3
"""Live-tune the shield of the deep's held pose, with a running client watching.

Edits the `display` entries of the shield models — both the >=1.21.4 rebuilt models
in src/ and the <1.21.4 originals, and every node's already-built build/resources
copy, so a running dev client picks the change up on F3+T.

    python3 scripts/shieldpose.py show
    python3 scripts/shieldpose.py show     ctx=third
    python3 scripts/shieldpose.py normal   rot=0,89,0 trans=2,-1,0 scale=1
    python3 scripts/shieldpose.py blocking trans=0.75,2.25,0
    python3 scripts/shieldpose.py blocking ctx=third rot=0,90,0
    python3 scripts/shieldpose.py both     rot=0,-91,0

`normal` is the pose while simply holding it, `blocking` while holding right-click.
Any key you leave out keeps its current value. Units: rotation in degrees,
translation in 1/16 of a block (so 16 = one block), scale is a multiplier.

`ctx=` picks the display context, default `first`:

    first (default)  what you see holding it yourself, BOTH hands at once
    firstright / firstleft   ONE first-person hand, written raw (see below)
    third            what others see / F5 — the RIGHT hand only
    thirdleft        the third-person off-hand
    gui              the inventory / hotbar icon
    ground, fixed, head

⚠️ First and third person share one set of `elements`, so **a fault in one context
and not the other is in the `display` block by construction** — establish which
context your screenshot is of before touching geometry. That is what cost the #33
investigation four blind client launches.

WHAT `ctx=first` WRITES, AND WHY IT IS NOT WHAT YOU TYPED
--------------------------------------------------------
You give one pose. It has to land in four places — two hands x two model files —
and vanilla applies a *different* transform to each of the four before your
`display` block gets a say. So the four written entries are all different, and
none of them need equal what you typed. They are solved, not copied:

  * ItemTransform.apply negates translation.x / rotation.y / rotation.z for the
    OFF-HAND (verified in the decompiled source). Writing one rotation into both
    hands therefore gives a MIRRORED pose in the off-hand, not the same one — on
    this shield that turns the spikes to face the player.
  * From **1.21.4** vanilla also wraps the whole blocking pose of any item that is
    not a `ShieldItem` in a hardcoded arm transform, mirrored between hands:
        translate(invert * -0.14142136, 0.08, 0.14142136)
        Rx(-102.25) Ry(invert * 13.365) Rz(invert * 78.05)
    Below 1.21.4 that branch is a bare `applyItemArmTransform` — no rotation at
    all — so the same numbers mean two different poses on the two sides of that
    line. See ARM below.

So: `ctx=first` treats your input as the pose in the frame of `PRIMARY` (the
>=1.21.4 model, since that is the era a modern dev client renders), converts it to
a hand-independent world pose, and solves each of the four entries back out of it.
Tune on the client you have; the other era follows for free.

`firstright` / `firstleft` skip all of that and write literally what you type into
that one key of both files — the escape hatch for a deliberately asymmetric pose.

Workflow: run this, alt-tab into the game, press F3+T. No rebuild, no relaunch.

There is also an in-game twin, `/shieldpose`, in client/command/AMShieldPoseCommand — same files,
same context map, same solve, same on-disk format (verified byte-identical), and it reloads
resources itself so it needs no alt-tab and no F3+T. It self-disables outside a checkout, so it
never ships. This script is still the one that prints the projected on-screen box, and the one that
works with no client running. See docs/notes/client-settings.md.
"""
import glob
import json
import math
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# (pose name) -> the models that carry it. Two per pose: the >=1.21.4 rebuilt model
# and the <1.21.4 builtin/entity one, which still owns the display block there.
MODELS = {
    "normal":   ["shield_of_the_deep_3d.json", "shield_of_the_deep.json"],
    "blocking": ["shield_of_the_deep_3d_blocking.json", "shield_of_the_deep_blocking.json"],
}
# Index into MODELS[pose] of the file the client you are tuning against actually renders.
# 0 is the >=1.21.4 rebuilt model. Flip to 1 only if you are tuning on a <1.21.4 client.
PRIMARY = 0
SRC = os.path.join(ROOT, "src/main/resources/assets/alexsmobs/models/item")
BUILT = os.path.join(ROOT, "versions/*/build/resources/main/assets/alexsmobs/models/item")

# Vanilla's own first-person arm transform, per model file: (rotation xyz, translation xyz-in-blocks),
# both given for the RIGHT hand. Only the >=1.21.4 blocking model gets one — that is the
# `if (!(itemStack.getItem() instanceof ShieldItem))` branch of ItemInHandRenderer's `case BLOCK`,
# which does not exist below 1.21.4 (checked against every 1.20.1..26.2 jar; the boundary is exact).
# The shield of the deep is not a ShieldItem on any loader, so it always takes that branch.
# The off-hand form negates the y and z angles and translation.x, which is a mirror, not a rotation
# of the pose — hence the solve below.
ARM = {
    "shield_of_the_deep_3d_blocking.json": ([-102.25, 13.365, 78.05], [-0.14142136, 0.08, 0.14142136]),
}
# The first-person arm offset every item gets, in every era (applyItemArmTransform, equip height 0).
ARM_BASE = (0.56, -0.52, -0.72)

# Short name -> the display keys it writes. Only `first` writes both hands, and it is the
# only context that solves rather than copies; everything else is written verbatim.
CONTEXTS = {
    "first":      ("firstperson_righthand", "firstperson_lefthand"),
    "firstright": ("firstperson_righthand",),
    "firstleft":  ("firstperson_lefthand",),
    "third":      ("thirdperson_righthand",),
    "thirdleft":  ("thirdperson_lefthand",),
    "gui":        ("gui",),
    "ground":     ("ground",),
    "fixed":      ("fixed",),
    "head":       ("head",),
}
# Contexts whose geometry the on-screen projection in predict() is valid for.
FIRSTPERSON = ("first", "firstright", "firstleft")


# --- 3x3 rotation helpers -------------------------------------------------------------------
# Everything here matches what the game does: PoseStack.mulPose right-multiplies, and
# ItemTransform.apply rotates by JOML's Quaternionf.rotationXYZ, which is Rx*Ry*Rz (confirmed by
# running JOML, not by recall). Angles are degrees throughout.

def _rx(a):
    a = math.radians(a); c, s = math.cos(a), math.sin(a)
    return [[1, 0, 0], [0, c, -s], [0, s, c]]


def _ry(a):
    a = math.radians(a); c, s = math.cos(a), math.sin(a)
    return [[c, 0, s], [0, 1, 0], [-s, 0, c]]


def _rz(a):
    a = math.radians(a); c, s = math.cos(a), math.sin(a)
    return [[c, -s, 0], [s, c, 0], [0, 0, 1]]


def _mul(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]


def _mm(*ms):
    out = [[1, 0, 0], [0, 1, 0], [0, 0, 1]]
    for m in ms:
        out = _mul(out, m)
    return out


def _t(m):
    return [[m[j][i] for j in range(3)] for i in range(3)]


def _mv(m, v):
    return [sum(m[i][k] * v[k] for k in range(3)) for i in range(3)]


def rot_xyz(r):
    """The rotation a `display` block's `rotation` array produces: Rx*Ry*Rz."""
    return _mm(_rx(r[0]), _ry(r[1]), _rz(r[2]))


def euler_xyz(m):
    """Inverse of rot_xyz. The gimbal branch never fires on any pose we write."""
    b = math.asin(max(-1.0, min(1.0, m[0][2])))
    if abs(m[0][2]) < 0.999999:
        a = math.atan2(-m[1][2], m[2][2])
        c = math.atan2(-m[0][1], m[0][0])
    else:
        a = math.atan2(m[2][1], m[1][1])
        c = 0.0
    return [math.degrees(v) for v in (a, b, c)]


def arm(filename, right):
    """Vanilla's arm transform for one model file and one hand, as (rotation matrix, offset)."""
    rot, trans = ARM.get(filename, ([0, 0, 0], [0, 0, 0]))
    invert = 1 if right else -1
    m = _mm(_rx(rot[0]), _ry(invert * rot[1]), _rz(invert * rot[2]))
    return m, [invert * trans[0], trans[1], trans[2]]


def solve(entry, filename):
    """The (right, left) `display` entries for one model file, from a canonical world pose.

    `entry` is the pose as authored against MODELS[pose][PRIMARY]'s right hand. It is first turned
    into a world pose — the orientation and offset the shield actually ends up with, independent of
    hand and era — and then each of the four entries is solved back out of that:

        world orientation  W = A_f * Q_f          => Q_f = A_f^T * W
        world offset       P = a_f + A_f * T_f    => T_f = A_f^T * (P - a_f)

    The off-hand needs no separate translation: its arm transform is the right hand's conjugated by
    the x-mirror, so the JSON translation that lands the mirrored position is the *same* one, and
    ItemTransform's own negation of translation.x does the mirroring. Its rotation is not free that
    way, because we want the orientation IDENTICAL rather than mirrored, so it is solved and then
    y/z pre-negated to cancel ItemTransform.
    """
    rot = list(entry.get("rotation", [0, 0, 0]))
    trans = list(entry.get("translation", [0, 0, 0]))
    primary = MODELS_BY_FILE[filename]
    ap, aoff = arm(primary, True)
    world = _mul(ap, rot_xyz(rot))
    pos = [aoff[i] + _mv(ap, [v / 16.0 for v in trans])[i] for i in range(3)]

    right_m, right_off = arm(filename, True)
    left_m, _ = arm(filename, False)
    rot_r = euler_xyz(_mul(_t(right_m), world))
    rot_l = euler_xyz(_mul(_t(left_m), world))
    rot_l = [rot_l[0], -rot_l[1], -rot_l[2]]
    trans_r = [v * 16.0 for v in _mv(_t(right_m), [pos[i] - right_off[i] for i in range(3)])]

    def entry_for(rotation):
        out = {"rotation": _round(rotation), "translation": _round(trans_r)}
        if "scale" in entry:
            out["scale"] = entry["scale"]
        return out

    return entry_for(rot_r), entry_for(rot_l)


def _round(values):
    out = []
    for v in values:
        v = round(v, 4)
        out.append(int(v) if v == int(v) else v)
    return out


# filename -> the primary file of the pose it belongs to, so solve() knows which frame its
# input was authored in.
MODELS_BY_FILE = {f: files[PRIMARY] for files in MODELS.values() for f in files}


def targets(filename):
    """Every copy of one model: the source of truth plus each node's built copy."""
    yield os.path.join(SRC, filename)
    yield from sorted(glob.glob(os.path.join(BUILT, filename)))


def load_current(pose, keys):
    p = os.path.join(SRC, MODELS[pose][PRIMARY])
    return json.load(open(p)).get("display", {}).get(keys[0], {})


def predict(pose, entry, aspect=1.754, fov=70.0):
    """Where the shield's face lands on screen. Calibrated against measured shots to ~0.1 block.

    v = armBase + a + A*(T + R*(s*(E/16 - 0.5))); perspective-divide by |Z|*tan(fov/2), where
    (A, a) is vanilla's own arm transform for this model file — identity below 1.21.4 and for the
    non-blocking pose.

    First-person, right hand only.
    """
    filename = MODELS[pose][PRIMARY]
    model = json.load(open(os.path.join(SRC, filename)))
    elements = model.get("elements")
    if not elements:
        return None
    plate = elements[0]  # element 1 is the shield face
    r = rot_xyz(entry.get("rotation", [0, 0, 0]))
    tr = [v / 16.0 for v in entry.get("translation", [0, 0, 0])]
    sc = entry.get("scale", [1, 1, 1])
    if not isinstance(sc, list):
        sc = [sc] * 3
    am, ao = arm(filename, True)

    def xform(v):
        v = [(v[i] / 16.0 - 0.5) * sc[i] for i in range(3)]
        v = _mv(r, v)
        v = [v[i] + tr[i] for i in range(3)]
        v = _mv(am, v)
        return [v[i] + ao[i] + ARM_BASE[i] for i in range(3)]

    pts = [xform([x, y, z])
           for x in (plate["from"][0], plate["to"][0])
           for y in (plate["from"][1], plate["to"][1])
           for z in (plate["from"][2], plate["to"][2])]
    t = math.tan(math.radians(fov) / 2)
    xs = [p[0] / (abs(p[2]) * t * aspect) for p in pts]
    ys = [p[1] / (abs(p[2]) * t) for p in pts]
    depth = [abs(p[2]) for p in pts]
    return (min(xs), max(xs), min(ys), max(ys), min(depth), max(depth))


def describe(pose, ctx, keys):
    e = load_current(pose, keys)
    scale = e.get("scale", [1, 1, 1])
    print(f"  {pose:9} [{ctx}] rot={','.join(str(v) for v in e.get('rotation', [0, 0, 0]))}"
          f"  trans={','.join(str(v) for v in e.get('translation', [0, 0, 0]))}"
          f"  scale={scale[0] if isinstance(scale, list) else scale}")
    if ctx not in FIRSTPERSON:
        return  # the projection below is first-person geometry only
    p = predict(pose, e)
    if p:
        x0, x1, y0, y1, d0, d1 = p
        print(f"            on screen: x {x0:+.2f}..{x1:+.2f}  y {y0:+.2f}..{y1:+.2f}"
              f"   (centre is 0, edges +-1)   depth {d0:.2f}..{d1:.2f} blocks")


def apply(pose, keys, changes):
    entry = dict(load_current(pose, keys))
    entry.update(changes)
    entry = {k: entry[k] for k in ("rotation", "translation", "scale") if k in entry}
    paired = len(keys) == 2  # only `ctx=first` solves; every other context is written verbatim
    written = 0
    for filename in MODELS[pose]:
        solved = solve(entry, filename) if paired else None
        for path in targets(filename):
            if not os.path.exists(path):
                continue
            d = json.load(open(path))
            if "display" not in d:
                continue  # the <1.21.4 file is emptied by the >=1.21.4 migration
            if paired:
                d["display"][keys[0]], d["display"][keys[1]] = (dict(solved[0]), dict(solved[1]))
            else:
                for key in keys:
                    d["display"][key] = dict(entry)
            indent = "\t" if path.startswith(SRC) else None
            with open(path, "w") as fh:
                json.dump(d, fh, indent=indent)
                if indent:
                    fh.write("\n")
            written += 1
    print(f"{pose} [{'+'.join(keys)}]: updated {written} file(s)")
    if paired:
        for filename in MODELS[pose]:
            r, l = solve(entry, filename)
            print(f"    {filename:36} right rot={r['rotation']} trans={r['translation']}")
            print(f"    {'':36} left  rot={l['rotation']} trans={l['translation']}")


def parse(args):
    out, ctx, explicit = {}, "first", False
    for a in args:
        if "=" not in a:
            sys.exit(f"expected key=value, got {a!r}")
        k, v = a.split("=", 1)
        if k in ("ctx", "context", "display"):
            if v not in CONTEXTS:
                sys.exit(f"unknown context {v!r} (use: {', '.join(CONTEXTS)})")
            ctx, explicit = v, True
            continue
        nums = [float(x) for x in v.replace(" ", "").split(",") if x != ""]
        nums = [int(n) if n == int(n) else n for n in nums]
        if k in ("rot", "rotation"):
            out["rotation"] = nums
        elif k in ("trans", "translation", "pos"):
            out["translation"] = nums
        elif k == "scale":
            out["scale"] = nums * 3 if len(nums) == 1 else nums
        else:
            sys.exit(f"unknown key {k!r} (use rot=, trans=, scale=, ctx=)")
    return out, ctx, explicit


def main():
    if len(sys.argv) < 2 or sys.argv[1] in ("-h", "--help", "help"):
        print(__doc__)
        return
    cmd = sys.argv[1]
    changes, ctx, explicit = parse(sys.argv[2:])
    keys = CONTEXTS[ctx]
    if cmd == "show":
        # A bare `show` dumps every context, so an asymmetric hand can't hide.
        shown = [(ctx, keys)] if explicit else \
                [(c, k) for c, k in CONTEXTS.items() if c not in ("firstright", "firstleft")]
        for pose in MODELS:
            for c, k in shown:
                describe(pose, c, k)
        return
    poses = list(MODELS) if cmd == "both" else [cmd]
    for pose in poses:
        if pose not in MODELS:
            sys.exit(f"unknown pose {pose!r} (use: normal, blocking, both, show)")
    if not changes:
        sys.exit("nothing to change — give rot=, trans= and/or scale=")
    for pose in poses:
        apply(pose, keys, changes)
    print()
    for pose in MODELS:
        describe(pose, ctx, keys)
    print("\nalt-tab into the game and press F3+T.")


if __name__ == "__main__":
    main()

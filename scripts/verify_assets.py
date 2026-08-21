#!/usr/bin/env python3
"""Every asset a source file NAMES must be an asset the mod SHIPS.

Vendoring Citadel moved its *code* into this mod but left four of its *textures* behind, and the
four `ResourceLocation`s in `citadel/client/gui/GuiBasicBook` kept pointing at the `citadel:`
namespace. With Citadel no longer installed nothing mounts that namespace, so all four resolved to
Minecraft's missing-texture checkerboard and the Animal Dictionary rendered as black/magenta
quadrants — on every node, in every release from 1.0.2 to 1.0.7.

Nothing caught it, and it is worth understanding why, because it is a different blind spot from the
mixin ones:

  - It compiles. A ResourceLocation is just a string.
  - It is not fatal, so no crash report, and the boot/client gates still go green.
  - The only runtime signal is `TextureManager: Failed to load texture: …` at **WARN**, emitted
    lazily *when the screen is first drawn* — so a gate that reaches the title screen and greps for
    `/ERROR]` cannot see it. You have to actually open the GUI.

Hence a static check. This scans `src/main/java` for string literals shaped like an asset path
(`<namespace>:<path>.<ext>`) and requires each to exist under `src/main/resources/{assets,data}/`.
`minecraft:` is skipped (vanilla ships it); everything else must be ours.

Limitation to keep in mind: only whole literals are checked. A path built by concatenation
(`"alexsmobs:textures/entity/" + name + ".png"`) is invisible here.

    python3 scripts/verify_assets.py

Exit status is nonzero if any referenced asset is missing.
"""
import os, re, sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
SRC = os.path.join(ROOT, "src", "main", "java")
RES = os.path.join(ROOT, "src", "main", "resources")

# "<ns>:<path>.<ext>" — the extension is what distinguishes an asset path from a registry id.
LITERAL = re.compile(r'"([a-z0-9_.-]+):([a-z0-9_/.-]+\.(?:png|json|txt|tbl|ogg|mcmeta))"')


def main():
    refs = {}
    for dirpath, _, files in os.walk(SRC):
        for f in files:
            if not f.endswith(".java"):
                continue
            p = os.path.join(dirpath, f)
            with open(p, encoding="utf-8", errors="replace") as fh:
                for i, line in enumerate(fh, 1):
                    for ns, path in LITERAL.findall(line):
                        refs.setdefault((ns, path), []).append(
                            f"{os.path.relpath(p, ROOT)}:{i}")

    missing = []
    for (ns, path), where in sorted(refs.items()):
        if ns == "minecraft":
            continue
        if not any(os.path.exists(os.path.join(RES, d, ns, path)) for d in ("assets", "data")):
            missing.append((ns, path, where))

    for ns, path, where in missing:
        print(f"MISSING  {ns}:{path}")
        for w in where:
            print(f"         {w}")
    print(f"\nasset literals={len(refs)}  missing={len(missing)}")
    return 1 if missing else 0


if __name__ == "__main__":
    sys.exit(main())

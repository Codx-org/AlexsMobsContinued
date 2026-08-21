#!/usr/bin/env python3
"""Bake the farseer static noise into the shaped mask textures — the >=1.21.5 fix for #53.

Upstream draws its "TV static" effects in two passes: a shaped mask texture
(entityTranslucent) that writes depth, then static.png re-drawn over the same geometry
with an EQUAL depth test and glint-scroll texturing (AMRenderTypes.STATIC_PORTAL /
STATIC_PARTICLE).  1.21.5 removed the custom composite render types, and the port's
fallback (plain entityTranslucent(static.png)) drew the *whole quad* as an opaque static
square — the farseer's emergence portal became a giant square of noise (#53).

This script bakes the two passes into one texture per (shape, noise variant):
RGB sampled from static.png, alpha copied from the mask.  On >=1.21.5 the renderer
draws a single entityTranslucent pass over these, cycling variants every 2 ticks for
the flicker the scroll shard used to provide.  Below 1.21.5 nothing references them
(and the opaque EQUAL-depth overlay would cover the mask's RGB anyway, so the baked
files are era-neutral by construction).

Masks:
  - portal_{0..3}.png (32x32, shipped)      -> portal_static_{s}_{v}.png, 64x64, 4 variants
  - vanilla generic_{0..7}.png (8x8, poof)  -> static_spark_{n}_{v}.png, 32x32, 4 variants
    (pass the directory holding extracted vanilla generic_N.png as argv[1])

Deterministic: noise crops come from a fixed offset table, no RNG.  Re-running is a
no-op byte-for-byte.
"""
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/alexsmobs/textures"
STATIC = Image.open(ASSETS / "static.png").convert("RGBA")  # 64x256, fully opaque noise
VARIANTS = 4

# Fixed crop origins into static.png per variant; a second table offsets per shape so no
# two baked files share a crop.
VARIANT_ORIGINS = [(0, 0), (0, 64), (0, 128), (0, 192)]
SHAPE_NUDGE = [(0, 0), (7, 13), (17, 29), (11, 47), (23, 5), (3, 37), (29, 19), (13, 53)]


def bake(mask: Image.Image, out_size: int, shape_idx: int, variant: int, shear: int = 0) -> Image.Image:
    """`shear` offsets each row's crop in x; only needed when out_size exceeds static.png's
    64px width, where a straight wrap would tile the same column band visibly."""
    mask = mask.convert("RGBA").resize((out_size, out_size), Image.NEAREST)
    alpha = mask.getchannel("A")
    ox, oy = VARIANT_ORIGINS[variant]
    nx, ny = SHAPE_NUDGE[shape_idx % len(SHAPE_NUDGE)]
    # Wrap-around crop of the noise sheet.
    noise = Image.new("RGBA", (out_size, out_size))
    for y in range(out_size):
        for x in range(out_size):
            sx = (ox + nx + x + y * shear) % STATIC.width
            sy = (oy + ny + y) % STATIC.height
            noise.putpixel((x, y), STATIC.getpixel((sx, sy)))
    noise.putalpha(alpha)
    return noise


def main() -> None:
    farseer = ASSETS / "entity/farseer"
    for s in range(4):
        mask = Image.open(farseer / f"portal_{s}.png")
        for v in range(VARIANTS):
            out = farseer / f"portal_static_{s}_{v}.png"
            bake(mask, 64, s, v).save(out)
            print(f"wrote {out.relative_to(ROOT)}")

    # The shattered void portal (dimensional carver / void worm) is the same shape as the farseer's
    # emergence portal: 13 masks that are pure black + alpha, whose entire visible content was the
    # STATIC_PORTAL pass drawn over them at EQUAL depth. Without that pass they are a black disc
    # (#90). 3 idle frames + 10 growth frames, 64x64, 4 noise variants each.
    shattered = ASSETS / "entity/void_worm/portal/shattered"
    for i, name in enumerate([f"portal_idle_{s}" for s in range(3)] + [f"portal_grow_{n}" for n in range(10)]):
        mask = Image.open(shattered / f"{name}.png")
        for v in range(VARIANTS):
            out = shattered / f"{name}_static_{v}.png"
            bake(mask, 64, i, v).save(out)
            print(f"wrote {out.relative_to(ROOT)}")

    # The farseer's own eye and scars overlays are the same shape of site again: 128x128 masks that
    # are pure black with an alpha cut-out, drawn over model geometry. Without the EQUAL-depth static
    # pass they are a black eye and black scars on the mob (#90). Sheared crops because 128 > the
    # noise sheet's 64px width.
    for name, idx in (("farseer_eye", 4), ("farseer_scars", 5)):
        mask = Image.open(farseer / f"{name}.png")
        for v in range(VARIANTS):
            out = farseer / f"{name}_static_{v}.png"
            bake(mask, 128, idx, v, shear=13).save(out)
            print(f"wrote {out.relative_to(ROOT)}")

    if len(sys.argv) > 1:
        vanilla = Path(sys.argv[1])
        particle = ASSETS / "particle"
        for n in range(8):
            mask = Image.open(vanilla / f"generic_{n}.png")
            for v in range(VARIANTS):
                out = particle / f"static_spark_{n}_{v}.png"
                bake(mask, 32, n, v).save(out)
                print(f"wrote {out.relative_to(ROOT)}")
    else:
        print("no vanilla-particle dir given - skipped static_spark bakes")


if __name__ == "__main__":
    main()

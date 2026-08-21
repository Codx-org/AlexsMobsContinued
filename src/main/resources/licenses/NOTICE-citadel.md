# Bundled Citadel code

This mod bundles a subset of **Citadel** by Alex the 666 / Alex Modguy.

- Upstream: https://github.com/Alex-the-666/Citadel
- Licence: **LGPL-3.0-only** (full text: `licenses/LGPL-3.0-only.txt`)
- Version the subset was taken from: Citadel **2.6.3** for Minecraft 1.20.1

Alex's Mobs has always required Citadel as a hard dependency. Citadel has no Forge build
above Minecraft 1.20.1, so Alex's Mobs Continued carries the parts it actually uses
(animation system, Tabula/AdvancedEntityModel rendering, entity data store, raycoms
pathfinding, spawn-biome config, book GUI, collision hooks) instead.

The bundled classes live under `com.github.alexthe666.alexsmobs.citadel` — **relocated** from
their original `com.github.alexthe666.citadel` package, so that installing the real Citadel
mod alongside this one cannot cause duplicate classes on the same classloader.

Modifications made to the bundled code:

- package relocated as described above;
- the Citadel mod main class replaced by a small shim (`citadel/Citadel.java`) that routes the
  two Citadel packets over Alex's Mobs' own network channel;
- features Alex's Mobs never used removed (patreon capes, shaders/post effects, tick-rate
  control, video/audio players, the Tetris easter egg, world-gen surface rules, guide book
  chrome);
- the five mixins Alex's Mobs relies on rewritten into `com.github.alexthe666.alexsmobs.mixin`.

Alex's Mobs itself is GPL-3.0-only; incorporating LGPL-3.0 code into a GPL-3.0 work is
expressly permitted by section 2(b)/3 of the LGPL. Sources for this mod remain public.

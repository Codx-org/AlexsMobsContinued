# Alex's Mobs Continued

A multi-version forward-port of **Alex's Mobs** — the Minecraft mod that adds 80+ new creatures
to the game — to Minecraft **1.20.1 through 26.2**, on **Forge**, **NeoForge** and **Fabric**.

Upstream Alex's Mobs stopped at Minecraft 1.20.1 / Forge. This project starts from that source
and carries it forward, one Minecraft version at a time, across 49 build targets driven by
[Stonecutter](https://stonecutter.kikugie.dev/).

## Downloads

- [Modrinth](https://modrinth.com/mod/alexs-mobs-continued)
- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/alexs-mobs-continued)

Every build requires [CodxLib](https://github.com/Codx-org/codxlib) as a separate mod.

## Credits and attribution

**Alex's Mobs** was created by **Alexthe666 / AlexModGuy**
([github.com/AlexModGuy/AlexsMobs](https://github.com/AlexModGuy/AlexsMobs)) — all of the
original code, models, textures, sounds and creature designs are theirs, and this project is a
derivative work of it.

**Citadel**, also by Alexthe666 / AlexModGuy
([github.com/AlexModGuy/Citadel](https://github.com/AlexModGuy/Citadel)), is bundled here in
relocated form because it has no build above Minecraft 1.20.1. See
[`src/main/resources/licenses/NOTICE-citadel.md`](src/main/resources/licenses/NOTICE-citadel.md)
for what was taken and what was changed.

The port to the later Minecraft versions and to NeoForge and Fabric, and the fixes that came
with it, are by **2Lynk**.

## Licence

Licensed under the **GNU Lesser General Public License, version 3** — the full text is in
[`LICENSE`](LICENSE).

Upstream Alex's Mobs declares its licence as "GNU LESSER GENERAL PUBLIC LICENSE" in its mod
manifest, without naming a version and without shipping a licence file; this project reads that
as LGPL-3 and ships the text accordingly.

Because it is copyleft, you are free to use, modify and redistribute this code, including in
your own mod — provided that you keep it under the LGPL, publish your source, and preserve the
attribution above.

## Building

Builds resolve **CodxLib** from the local Maven repository, so it has to be installed first:

```bash
git clone https://github.com/Codx-org/codxlib
cd codxlib && python3 scripts/install_maven_local.py
```

Then, from this repository:

```bash
./gradlew ":1.20.1-forge:build"     # one target
```

Build targets are named `<minecraft-version>-<loader>`; the full list is in
`settings.gradle.kts`. `stonecutter.gradle.kts` names the *active* target, whose sources are the
ones in the root `src/` directory — every other target is generated from it.

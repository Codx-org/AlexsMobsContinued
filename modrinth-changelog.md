# 2.1.6

Three fixes. The first one is serious — if you play on Forge, please update.

- **Forge only, 1.20.4 and newer: a crash that could make a world unloadable.** Around twenty of
  this mod's entities — the cachalot echo, thrown items, the straddleboard, the vine lasso, all the
  multipart bits of the big mobs — could not be sent to the client at all. If one of them was in
  your world it crashed on every single load. This has been broken since 2.0.0 and it is fixed now.
  NeoForge and Fabric were never affected.
- The mariachi cockroach's sombrero was sinking into its body instead of sitting on its head, on
  1.21.1 and older. It was also being drawn three-quarters size. Both fixed — the hat now looks the
  same on every version.
- Alex's Mobs Continued no longer crashes on startup when Sea Life is installed. All three catfish
  buckets were claiming the same fish, which Sea Life could not make sense of. The medium and large
  buckets are now their own kind of item; they behave exactly as before.

One change that is not a fix: **the command is now `/amc`** (Alex's Mobs Continued). `/amc menu`
opens the settings screen and `/amc config` does everything it did before. The old `/aac` spelling
still works, so nothing you have set up will break.

Also: if Oculus fails to load with a "MixinLevelRenderer_EntityListSorting" error when you add this
mod, that is an Oculus bug on older builds — update Oculus to one based on Iris 1.7 or newer and it
goes away.

Needs CodxLib 1.4.0 or newer. Fabric also needs Fabric API.

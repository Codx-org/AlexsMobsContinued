# The vendored Citadel subset

> Read when touching anything under `citadel/`, the mod's own mixins, or the advancement icon items.
>
> Part of the Alex's Mobs Continued porting notes.

### The Citadel constraint

Alex's Mobs hard-depends on **Citadel**. **189 of its ~745 files** import it, but the API
surface is narrow and heavily concentrated in rendering:

| Citadel API | files |
|---|---|
| `client.model.{AdvancedEntityModel, AdvancedModelBox, basic.BasicModelPart, ModelAnimator}` | ~130 |
| `animation.{IAnimatedEntity, Animation, AnimationHandler}` | ~71 |
| `server.entity.CitadelEntityData` + `server.message.PropertiesMessage` / `PacketBufferUtils` | 6 |
| `server.entity.pathfinding.raycoms.*` (AdvancedPathNavigate + stuck handler) | 3 |
| `server.entity.collision.ICustomCollisions` | 3 |
| `config.biome.{SpawnBiomeData, SpawnBiomeConfig, BiomeEntryType}` | 3 |
| `client.event.Event*` (4 client hooks), `client.gui.GuiBasicBook`, `server.block.LecternBooks` | 6 |

**Actual availability** (Modrinth API, checked 2026-07-22 — do not trust older notes):

| | Forge | NeoForge |
|---|---|---|
| 1.20.1 | ✅ 2.6.3 | ✅ 2.6.3 |
| 1.20.4 / 1.20.6 | ✗ | ✗ |
| 1.21 → 1.21.11 | ✗ | ✅ **2.7.1** (one jar declaring all 12 versions, published 2026-07-18) |

So **Forge has no Citadel above 1.20.1 at all** — bundling is not a preference there, it is the
only option. NeoForge can depend on the real mod for the entire 1.21 line.

**Design (implemented 2026-07-22, per the user's directive "wherever citadel is used, build it
into our mod so we don't need that dependency"):** Citadel is vendored **on every node,
unconditionally**, into `src/main/java/com/github/alexthe666/alexsmobs/citadel/` — i.e. **package-
relocated**, *not* kept under `com.github.alexthe666.citadel`. Relocation is mandatory: a player
who also installs the real Citadel would otherwise get duplicate classes on one FML classloader.
The cost is that all 189 importing files had their imports rewritten (done, mechanically). There
is now **no Citadel dependency on any node and no conditional source set** — one code path for
all 28 nodes, which also removes NeoForge/Forge divergence.

What the vendored subset looks like (**87 files**), and the pieces that needed real work:

- **`citadel/Citadel.java`** — a *shim* replacing Citadel's mod main class. Holds `LOGGER`, a
  `PROXY` (`DistExecutor` → `CitadelProxy` / `CitadelClientProxy`), and `sendMSGToServer` /
  `sendMSGToAll` / `sendNonLocal` that forward to **Alex's Mobs' own `NETWORK_WRAPPER`**.
  `PropertiesMessage` + `AnimationMessage` are registered on that channel in
  `AlexsMobs.setup(FMLCommonSetupEvent)` — Citadel used to register them on its own channel.
- **The 5 mixins Alex's Mobs actually needs** were rewritten into
  `com.github.alexthe666.alexsmobs.mixin{,.client}` and declared in `alexsmobs.mixins.json`
  (previously an empty placeholder): `LivingEntityMixin` (the synced+persisted `CompoundTag`
  entity-data store behind `CitadelEntityData`) plus client `ClientLevelMixin` (star brightness),
  `HumanoidModelMixin` (arm pose), `ItemBlockRenderTypesMixin` (fluid render type),
  `LevelRendererMixin` (outline colour). Citadel's other mixins (world-gen, tick rate, smithing,
  title screen, sound engine) are **not** needed and were dropped.
- **Citadel's 4 access-transformer entries were merged** into
  `META-INF/accesstransformer.cfg` (SRG names: `NodeEvaluator.entityWidth/Height/Depth`,
  `ChunkMap.getVisibleChunkIfPresent`). Without them the raycoms navigator does not compile —
  this was the *only* compile failure of the whole vendoring pass.
- **`citadel:fancy_item` / `citadel:effect_item`** — Citadel's advancement-icon items, referenced
  by 9 of Alex's Mobs' advancement JSONs. Re-registered as **`alexsmobs:fancy_item` /
  `alexsmobs:effect_item`** (`item/ItemCustomRender`, hidden from the creative tab via
  `CustomTabBehavior`), their render branches folded into the existing `AMItemstackRenderer`,
  and the 9 JSONs rewritten. Miss these and the advancements silently fail to load.
- **Dropped as unused:** patreon capes, shaders/post effects, tick-rate control, video/audio
  players, the Tetris easter egg, world-gen surface rules, guide-book chrome (and with it
  `LecternBooks.init()`, whose only reference was `Citadel.CITADEL_BOOK`).

**No refmap.** `alexsmobs.mixins.json` deliberately has **no `"refmap"` key**. Architectury Loom
remaps the mixin annotations **in-place at `remapJar`** (verified: the shipped
`LivingEntityMixin.class` carries `m_8097_`/`m_7380_`, not Mojmap names), so no refmap is
generated and declaring one would be a dangling reference. Do **not** copy Citadel's
`"refmap": "citadel.refmap.json"` — that was ForgeGradle.

The `client.*` mixins (four here, five once `client.GuiMixin` joined in Milestone 4) log
`RuntimeDistCleaner: Attempted to load class … for invalid dist DEDICATED_SERVER` on a dedicated
server. That is **benign and faithful** — Citadel's own config has the identical shape and produces
the identical warnings; Forge's `RuntimeDistCleaner` blocks the client class and the mixin is simply
not applied. They are reported at ERROR level, so any boot-gate grep on `/ERROR]` must filter them.
(Reason they are reached at all despite living in the `client` list: Fletching Table also copies them
into `mixins` — see "Fletching Table writes the `mixins` array" in [`mixins.md`](mixins.md).)

**Licence — checked, no blocker.** Citadel is **LGPL-3.0-only**
(github.com/Alex-the-666/Citadel); Alex's Mobs is **GPL-3.0-only**. LGPL-3 code may be
incorporated into a GPL-3 work, so vendoring is fine as long as Citadel's copyright/licence
notice ships with the vendored files and this repo stays GPL-3 with sources public.


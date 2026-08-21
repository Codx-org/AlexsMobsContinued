import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import java.io.File

/**
 * Data-pack migrations that Stonecutter cannot express.
 *
 * Stonecutter only preprocesses source files — a `//? if` block inside a `.json` is copied through
 * verbatim (verified), and vanilla parses data-pack JSON with a strict Gson reader that rejects
 * comments. So the era-dependent parts of the shipped data pack are rewritten here, after
 * `processResources` has staged them, instead of being duplicated per MC version.
 *
 * Everything below is a no-op on files that are already in the target shape, so re-running is safe.
 */
object DataPackMigration {

	private val json = Json { prettyPrint = true }

	/**
	 * MC 1.20.5 replaced the `{"item": …, "nbt": "<snbt>"}` item-stack JSON with the
	 * component-based `{"id": …, "components": {…}}`. That hits two places in this mod's data:
	 * every crafting `result`, and every advancement `display.icon`.
	 */
	fun migrateTo1205(resourcesRoot: File): Int {
		val data = resourcesRoot.resolve("data")
		if (!data.isDirectory) return 0
		var changed = 0
		data.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { file ->
			val dirs = file.invariantSeparatorsPath
			// Both spellings are matched so this stays correct whichever side of the 1.21
			// singular-folder rename it runs on.
			val transform: (JsonObject) -> JsonObject = when {
				// capsid_recipes is this mod's own recipe type, but its result goes through
				// ItemStack.CODEC too (see CapsidRecipe.Deserializer).
				dirs.contains("/recipe/") || dirs.contains("/recipes/") ||
					dirs.contains("/capsid_recipes/") -> ::migrateRecipe
				dirs.contains("/advancement/") || dirs.contains("/advancements/") -> ::migrateAdvancement
				dirs.contains("/loot_table/") || dirs.contains("/loot_tables/") -> ::migrateLootTable
				else -> return@forEach
			}
			val original = runCatching { json.parseToJsonElement(file.readText()) as? JsonObject }
				.getOrNull() ?: return@forEach
			val migrated = transform(original)
			if (migrated != original) {
				file.writeText(json.encodeToString(JsonObject.serializer(), migrated))
				changed++
			}
		}
		return changed
	}

	/**
	 * MC 1.21 renamed every data-pack registry folder to the singular form of its registry key.
	 * Nothing inside the files changes — only where they live.
	 */
	fun migrateTo121(resourcesRoot: File): Int {
		val data = resourcesRoot.resolve("data")
		if (!data.isDirectory) return 0
		var moved = 0
		data.listFiles().orEmpty().filter { it.isDirectory }.forEach { namespace ->
			singularFolders.forEach { (plural, singular) ->
				moved += relocate(namespace.resolve(plural), namespace.resolve(singular))
			}
			// tags/<registry> was pluralised one level deeper.
			val tags = namespace.resolve("tags")
			singularTagFolders.forEach { (plural, singular) ->
				moved += relocate(tags.resolve(plural), tags.resolve(singular))
			}
		}
		return moved
	}

	/**
	 * Alex's Mobs' armour: material name -> (texture base name, equipment layer type).
	 *
	 * The texture name differs from the material name for over half of them (the material is named
	 * after the mob, the texture after the item), which is why this is a table and not a rule. The
	 * layer type is the slot the material's one and only item occupies — everything renders through
	 * the humanoid armour layer, including the tarantula-hawk elytra, whose custom model is a
	 * HumanoidModel handed to Forge's `getHumanoidArmorModel` hook.
	 */
	private val armorEquipment = listOf(
		Triple("roadrunner", "roadrunner_boots", "humanoid"),
		Triple("crocodile", "crocodile_chestplate", "humanoid"),
		Triple("centipede", "centipede_leggings", "humanoid_leggings"),
		Triple("moose", "moose_headgear", "humanoid"),
		Triple("raccoon", "frontier_cap", "humanoid"),
		Triple("sombrero", "sombrero", "humanoid"),
		Triple("spiked_turtle_shell", "spiked_turtle_shell", "humanoid"),
		Triple("fedora", "fedora", "humanoid"),
		Triple("emu", "emu_leggings", "humanoid_leggings"),
		Triple("tarantula_hawk_elytra", "tarantula_hawk_elytra", "humanoid"),
		Triple("froststalker", "froststalker_helmet", "humanoid"),
		Triple("rocky_roller", "rocky_chestplate", "humanoid"),
		Triple("flying_fish", "flying_fish_boots", "humanoid"),
		Triple("novelty_hat", "novelty_hat", "humanoid"),
		Triple("kimono", "unsettling_kimono", "humanoid"),
	)

	/**
	 * MC 1.21.2 deleted Forge's `getArmorTexture` hook along with `ArmorMaterial.Layer`. An armour
	 * texture is now named indirectly: the ArmorMaterial carries an equipment-model id, resolved to
	 * `assets/<ns>/models/equipment/<id>.json`, whose layers name textures under
	 * `assets/<ns>/textures/entity/equipment/<layer type>/<texture>.png`.
	 *
	 * Alex's Mobs keeps its armour textures at `textures/armor/<item>.png` — one file per item,
	 * used for both armour layers. Rather than duplicate them into the source tree for the sake of
	 * the upper nodes, the model JSON and the relocated texture are both derived here, so
	 * `textures/armor/` stays the single source of truth.
	 */
	fun migrateEquipmentTo12102(resourcesRoot: File, modId: String): Int {
		val assets = resourcesRoot.resolve("assets/$modId")
		if (!assets.isDirectory) return 0
		val models = assets.resolve("models/equipment")
		var written = 0
		armorEquipment.forEach { (material, texture, layer) ->
			val source = assets.resolve("textures/armor/$texture.png")
			if (!source.isFile) return@forEach
			val relocated = assets.resolve("textures/entity/equipment/$layer/$texture.png")
			relocated.parentFile.mkdirs()
			source.copyTo(relocated, overwrite = true)
			models.mkdirs()
			models.resolve("$material.json").writeText(
				"""{"layers":{"$layer":[{"texture":"$modId:$texture"}]}}"""
			)
			written++
		}
		return written
	}

	/**
	 * 1.21.4 moved equipment definitions out of the model tree: `assets/<ns>/models/equipment/<id>.json`
	 * became `assets/<ns>/equipment/<id>.json`. Verified by listing the shipped client jars — 1.21.2
	 * has only `assets/minecraft/models/equipment/`, 1.21.4 has only `assets/minecraft/equipment/`.
	 *
	 * [migrateEquipmentTo12102] writes the 1.21.2 layout on every node from 1.21.2 up, so this runs
	 * after it and moves the whole folder on the nodes that want the newer one. Missing the move is
	 * silent in the worst way: an armour item whose `asset_id` resolves to nothing does not warn and
	 * does not draw a missing texture — **the layer is skipped and the armour is simply invisible**.
	 * That is report #38, seen as "the crocodile chestplate is equipped but not visible".
	 *
	 * On Fabric the twelve items with a hand-built model are drawn by `FabricArmorRenderers`, which
	 * names its own texture, so only the three model-less ones (crocodile chestplate, centipede and
	 * emu leggings) went missing. Forge and NeoForge have no such renderer and take the vanilla
	 * armour layer for all fifteen.
	 */
	fun relocateEquipmentTo1214(resourcesRoot: File, modId: String): Int {
		val assets = resourcesRoot.resolve("assets/$modId")
		if (!assets.isDirectory) return 0
		return relocate(assets.resolve("models/equipment"), assets.resolve("equipment"))
	}

	/**
	 * 1.21.5 changed how an advancement tab's background is addressed. It used to be the texture file
	 * itself — `"minecraft:textures/gui/advancements/backgrounds/stone.png"` — and is now a bare id,
	 * `"minecraft:gui/advancements/backgrounds/stone"`, which the client expands back into
	 * `textures/<path>.png` when it loads it. Read out of the shipped `story/root.json` in each jar
	 * rather than recalled: 1.21.4 still has the old form, 1.21.5 has the new one.
	 *
	 * Alex's Mobs' own tab names `alexsmobs:textures/advancement_background.png`, which the expansion
	 * turns into `alexsmobs:textures/textures/advancement_background.png.png` — the exact string the
	 * client logged as missing. A missing background is not fatal, so from 1.21.5 up the advancement
	 * screen just drew the magenta-and-black missing texture behind every Alex's Mobs advancement
	 * (report #39).
	 *
	 * The rewrite is the inverse of the client's expansion — drop a leading `textures/` and a trailing
	 * `.png` — so a value already in the new form is left alone.
	 */
	fun migrateAdvancementBackgroundsTo1215(resourcesRoot: File): Int {
		val data = resourcesRoot.resolve("data")
		if (!data.isDirectory) return 0
		var changed = 0
		data.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { file ->
			val root = runCatching { json.parseToJsonElement(file.readText()) }.getOrNull() as? JsonObject
				?: return@forEach
			val display = root["display"] as? JsonObject ?: return@forEach
			val background = (display["background"] as? JsonPrimitive)?.takeIf { it.isString }?.content
				?: return@forEach
			val namespace = background.substringBefore(':', missingDelimiterValue = "")
			val path = background.substringAfter(':')
			if (!path.startsWith("textures/") || !path.endsWith(".png")) return@forEach
			val rewritten = path.removePrefix("textures/").removeSuffix(".png")
			file.writeText(
				json.encodeToString(
					JsonElement.serializer(),
					root.replacing(
						"display",
						display.replacing(
							"background",
							JsonPrimitive(if (namespace.isEmpty()) rewritten else "$namespace:$rewritten"),
						),
					),
				)
			)
			changed++
		}
		return changed
	}

	/**
	 * `regItem("spawn_egg_<mob>", () -> AMCompat.spawnEgg(TYPE, 0X<background>, 0X<highlight>, …))`
	 *
	 * The two colours are only ever spelled at the registration site, and from 1.21.4 the client
	 * needs them in JSON rather than on the item — see [writeItemModelDefinitions].
	 */
	private val spawnEggRegistration = Regex(
		"""regItem\("(spawn_egg_[a-z0-9_]+)",\s*\(\)\s*->\s*AMCompat\.spawnEgg\([^,]+,\s*0[Xx]([0-9A-Fa-f]+)\s*,\s*0[Xx]([0-9A-Fa-f]+)"""
	)

	/**
	 * The four `ItemDisplayContext`s `AMItemstackRenderer` treated as "held", i.e. the ones it swapped
	 * to the cuboid `<id>_hand` model in. Unnamespaced, because `minecraft:display_context` selects on
	 * a `StringRepresentable` enum — the same spelling vanilla's own `spyglass` definition uses.
	 */
	private const val HAND_DISPLAY_CONTEXTS =
		"""["thirdperson_lefthand","thirdperson_righthand","firstperson_lefthand","firstperson_righthand"]"""

	/** The parent MC 1.21.4 deleted along with the whole BER-item mechanism. */
	private const val BUILTIN_ENTITY = "builtin/entity"

	/**
	 * MC 1.21.4 stopped resolving an item's model by convention.
	 *
	 * Up to 1.21.3 the client looked for `assets/<ns>/models/item/<id>.json` and that was the whole
	 * contract. 1.21.4 introduced **item model definitions**: `assets/<ns>/items/<id>.json`, a
	 * `ClientItem` naming an [ItemModel] — the old model file is still needed, but it is now only
	 * reachable *through* a definition. An item with no definition is not an error, it just renders
	 * as the missing-model cube, and the client logs one `Missing item model for location <id>` per
	 * item at every resource reload.
	 *
	 * Alex's Mobs was authored against 1.20.1 and has 280 items with nothing but the legacy model, so
	 * on every node from 1.21.4 up **every item in the mod rendered as the missing model**. Rather
	 * than commit 280 near-identical files that only 20 of the 32 nodes read, one definition is
	 * derived here per legacy item model file (⚠️ do not write that path with a glob — a `slash-star`
	 * inside a KDoc opens a NESTED Kotlin block comment and swallows the rest of the file):
	 *
	 *     {"model":{"type":"minecraft:model","model":"<ns>:item/<id>"}}
	 *
	 * A handful of those models are override targets (`*_blocking`, `tarantula_hawk_elytra_broken`,
	 * `blood_gun_empty`, …) that no item is registered under. 1.21.4 deleted the `overrides` mechanism
	 * that reached them — an accepted regression since Milestone 6 — so the definitions written for
	 * them are inert: nothing ever looks them up, and an unreferenced definition is not validated.
	 *
	 * Spawn eggs are the one shape that needs more than the base model. `SpawnEggItem` no longer
	 * carries colours (its 1.21.4+ constructor takes only `Item.Properties`); the two tints that used
	 * to be Java arguments are `minecraft:constant` tint sources on the definition, applied to
	 * vanilla's `item/template_spawn_egg` layers. They are read back out of [spawnEggSource], which is
	 * where the mod spells them, so the registration stays the single source of truth.
	 *
	 * The other shape needing more is the 16 models parented to `builtin/entity`, which 1.21.4 deleted
	 * along with the ISTER mechanism that drew them — a definition pointing at one of those resolves to
	 * nothing, so the item stays the missing-model cube even once it has a definition. Four of them
	 * (`falconry_glove`, `skelewag_sword`, `stink_ray`, `vine_lasso`) exist only to let
	 * `AMItemstackRenderer` swap between a cuboid `<id>_hand` model in the four held contexts and a flat
	 * `<id>_inventory` sprite everywhere else — which is precisely what `minecraft:select` over
	 * `minecraft:display_context` expresses natively, so those get their in-hand look back rather than
	 * staying an accepted regression. See [repairBuiltinEntityModel] for the rest — except the
	 * [LIVE_ICON_ITEMS], whose definitions dispatch to the mod's own `minecraft:special` renderer.
	 */
	fun writeItemModelDefinitions(resourcesRoot: File, modId: String, spawnEggSource: File?): Int {
		val assets = resourcesRoot.resolve("assets/$modId")
		val models = assets.resolve("models/item")
		if (!models.isDirectory) return 0
		val tints: Map<String, Pair<String, String>> = spawnEggSource
			?.takeIf { it.isFile }
			?.let { source ->
				spawnEggRegistration.findAll(source.readText()).associate { match ->
					val (id, background, highlight) = match.destructured
					id to (background.toInt(16).toString() to highlight.toInt(16).toString())
				}
			}
			.orEmpty()
		val definitions = assets.resolve("items")
		definitions.mkdirs()
		var written = 0
		models.listFiles().orEmpty().filter { it.isFile && it.extension == "json" }.forEach { model ->
			val id = model.nameWithoutExtension
			val dead = model.readText().contains(BUILTIN_ENTITY)
			val paired = dead &&
				models.resolve("${id}_hand.json").isFile &&
				models.resolve("${id}_inventory.json").isFile
			val rebuilt = REBUILT_MODELS[id]?.takeIf { models.resolve("$it.json").isFile }
			val live = id in LIVE_ICON_ITEMS
			val body = if (live) {
				"""{"type":"minecraft:special","base":"$modId:item/$id",""" +
					""""model":{"type":"$modId:icon"}}"""
			} else if (rebuilt != null) {
				if (models.resolve("${rebuilt}_blocking.json").isFile) {
					"""{"type":"minecraft:condition","property":"minecraft:using_item",""" +
						""""on_true":{"type":"minecraft:model","model":"$modId:item/${rebuilt}_blocking"},""" +
						""""on_false":{"type":"minecraft:model","model":"$modId:item/$rebuilt"}}"""
				} else {
					"""{"type":"minecraft:model","model":"$modId:item/$rebuilt"}"""
				}
			} else if (paired) {
				"""{"type":"minecraft:select","property":"minecraft:display_context",""" +
					""""cases":[{"when":$HAND_DISPLAY_CONTEXTS,""" +
					""""model":{"type":"minecraft:model","model":"$modId:item/${id}_hand"}}],""" +
					""""fallback":{"type":"minecraft:model","model":"$modId:item/${id}_inventory"}}"""
			} else {
				val custom = CUSTOM_TINTS[id]?.joinToString(",", ""","tints":[""", "]") { source ->
					"""{"type":"$modId:$source"}"""
				}
				val tint = custom ?: tints[id]?.let { (background, highlight) ->
					""","tints":[{"type":"minecraft:constant","value":$background},""" +
						"""{"type":"minecraft:constant","value":$highlight}]"""
				}.orEmpty()
				"""{"type":"minecraft:model","model":"$modId:item/$id"$tint}"""
			}
			definitions.resolve("$id.json").writeText("""{"model":$body}""")
			written++
			if (dead) {
				// A live icon item's base model is still *referenced*: a `minecraft:special` model
				// never draws it, but ModelRenderProperties.fromResolvedModel reads its `gui_light`,
				// its particle sprite and its `display` transforms off it (bytecode-checked). So
				// that one keeps everything it authored and loses only the dead parent — which is
				// precisely the file the client saw below 1.21.4. The other bypassed kinds name a
				// different model entirely and this file is unreferenced, so `{}` still stands.
				repairBuiltinEntityModel(assets, model, modId, id, live, paired || rebuilt != null)
			}
		}
		return written
	}

	/** The template MC 1.21.5 deleted when it gave every vanilla spawn egg its own painted sprite. */
	private const val SPAWN_EGG_TEMPLATE = "item/template_spawn_egg"

	/**
	 * MC 1.21.5 deleted `item/template_spawn_egg` and both of the greyscale textures it layered.
	 *
	 * Vanilla stopped tinting: each of its ~80 eggs now has a hand-painted sprite and a plain
	 * `item/generated` model. A mod's eggs, whose colours are two ints at the registration site,
	 * have nothing to move to — and every model in this mod that named the deleted parent resolves
	 * to nothing, so all 89 spawn eggs have rendered as the missing-model cube on every node from
	 * 1.21.5 up (reported against 26.1.2/Fabric, and true of 27 nodes).
	 *
	 * Tinting itself still works — `minecraft:constant` is unchanged and [writeItemModelDefinitions]
	 * already writes the pair of tints onto the definition. Only the two layers went away, so they
	 * ship in this mod's own namespace (`textures/item/spawn_egg{,_overlay}.png`, copied from 1.21.4)
	 * and the model is re-pointed at them. Below 1.21.5 the vanilla parent is left alone.
	 */
	fun retemplateSpawnEggs(resourcesRoot: File, modId: String): Int {
		val models = resourcesRoot.resolve("assets/$modId/models/item")
		if (!models.isDirectory) return 0
		var rewritten = 0
		models.listFiles().orEmpty().filter { it.isFile && it.extension == "json" }.forEach { model ->
			if (!model.readText().contains(SPAWN_EGG_TEMPLATE)) return@forEach
			model.writeText(
				"""{"parent":"minecraft:item/generated","textures":""" +
					"""{"layer0":"$modId:item/spawn_egg","layer1":"$modId:item/spawn_egg_overlay"}}"""
			)
			rewritten++
		}
		return rewritten
	}

	/**
	 * Rewrites one `builtin/entity` item model into something 1.21.4+ can actually load.
	 *
	 * The parent is gone, so the model resolves to nothing and every reference to it renders as the
	 * missing-model cube. The model manager also loads every file under `models/` whether or not a
	 * definition points at it, so leaving the dead parent in place logs a resolution failure per file
	 * on top of that. In descending order of fidelity:
	 *
	 * - [live] — the definition written for it is a `minecraft:special`, which does not draw this file
	 *   but *does* read its `gui_light`, particle sprite and `display` transforms. Only the dead parent
	 *   is removed, so the base is byte-for-byte what upstream authored minus the one line 1.21.4
	 *   deleted. (`tab_icon` is `{"parent":…}` and nothing else, so it still ends up as `{}`.)
	 * - [bypassed] — the definition written for it names other models instead (a `display_context`
	 *   select over `<id>_hand`/`<id>_inventory`, or a [REBUILT_MODELS] replacement), so this file is
	 *   unreferenced. Emptied rather than deleted,
	 *   because `models/item` is also the directory the definition loop enumerates.
	 * - a `models/block/<id>` exists (block items whose ISTER branch drew the block entity) — parent it
	 *   to the block model, which is what a plain block item would have used all along.
	 * - the block's **blockstate** names someone else's model — the three End Pirate Ship blocks
	 *   (`end_pirate_anchor`, `end_pirate_anchor_winch`, `end_pirate_ship_wheel`) are drawn in-world by
	 *   a block-entity renderer, so upstream never authored a block model for them and pointed each
	 *   blockstate at a **vanilla** one as a placeholder (`block/crying_obsidian` twice,
	 *   `block/end_rod` once). The branch above misses them because it looks for the *mod's* model
	 *   file, so all three fell through to the empty default and their creative-menu icons were
	 *   invisible from 1.21.4 up — reported alongside the shattered dimensional carver as "a couple
	 *   others are missing icons". Parenting the item model to whatever the blockstate already names
	 *   is what vanilla does for those same two blocks: `assets/minecraft/items/crying_obsidian.json`
	 *   and `end_rod.json` are each nothing but a `minecraft:model` pointing at the block model, and
	 *   both carry the `display` transforms an item needs (`end_rod` spells its own, `crying_obsidian`
	 *   inherits `block/block`'s through `cube_all`). In-world rendering is unaffected either way —
	 *   that was always the BER's job and it still runs.
	 * - a `textures/item/<id>` exists — the ordinary flat sprite via `item/generated`.
	 * - otherwise emptied: the `*_blocking` override targets no item is registered under, which
	 *   1.21.4's removal of `overrides` had already made unreachable.
	 */
	private fun repairBuiltinEntityModel(assets: File, model: File, modId: String, id: String, live: Boolean, bypassed: Boolean) {
		val borrowed = blockstateModel(assets, modId, id)
		val replacement = when {
			live -> withoutParent(model.readText())
			bypassed -> "{}"
			assets.resolve("models/block/$id.json").isFile -> """{"parent":"$modId:block/$id"}"""
			borrowed != null -> """{"parent":"$borrowed"}"""
			assets.resolve("textures/item/$id.png").isFile ->
				"""{"parent":"minecraft:item/generated","textures":{"layer0":"$modId:item/$id"}}"""
			else -> "{}"
		}
		model.writeText(replacement)
	}

	/**
	 * The same model with its `parent` member removed — and nothing else touched.
	 *
	 * `builtin/entity` is the only parent any of these files names, and from 1.21.4 it resolves to
	 * nothing, which fails the whole model. Everything else in the file (`display`, `gui_light`,
	 * `textures.particle`) is still read, so it must survive verbatim.
	 */
	private fun withoutParent(json: String): String =
		json.replace(parentField, "")
			.replace(danglingComma, "$1")
			.trim()
			.let { if (it.removeSurrounding("{", "}").isBlank()) "{}" else it }

	/** A `"parent": "…"` member, with the comma on whichever side it has one. */
	private val parentField = Regex(""""parent"\s*:\s*"[^"]*"\s*,?""")

	/** A comma left with nothing after it once [parentField] was cut out. */
	private val danglingComma = Regex(""",(\s*[}\]])""")

	/**
	 * The model a block's own blockstate already names, fully qualified, or `null` if there is none.
	 *
	 * Only the first variant is read: the callers are placeholder blockstates with a single unconditional
	 * variant, and an item can only have one model anyway. A blockstate path with no namespace resolves
	 * to `minecraft` exactly as the client resolves it — which is the whole point here, since the
	 * placeholders name vanilla blocks. A model in the **mod's** namespace is only borrowed if its file
	 * is really there, so this can never trade an empty icon for a missing-model cube.
	 */
	private fun blockstateModel(assets: File, modId: String, id: String): String? {
		val blockstate = assets.resolve("blockstates/$id.json").takeIf { it.isFile } ?: return null
		val named = blockstateModelField.find(blockstate.readText())?.groupValues?.get(1) ?: return null
		val qualified = if (named.contains(':')) named else "minecraft:$named"
		val (namespace, path) = qualified.split(':', limit = 2)
		if (namespace == modId && !assets.resolve("models/$path.json").isFile) return null
		return qualified
	}

	/** `"model": "<path>"` inside a blockstate variant. */
	private val blockstateModelField = Regex(""""model"\s*:\s*"([^"]+)"""")

	/**
	 * Bake the ghostly pickaxe's see-through look into its texture, for the nodes that cannot get it
	 * from a render type (#69).
	 *
	 * Upstream's pickaxe is **fully opaque** — its PNG has exactly two alpha values, 0 and 255. What
	 * makes it look ghostly is the render type: `GhostlyPickaxeBakedModel` (a Forge `BakedModelWrapper`)
	 * answers `getRenderTypes` with `AMRenderTypes.getGhostPickaxe`, whose transparency shard is
	 * `LIGHTNING_TRANSPARENCY` — additive blending, so the tool washes over whatever is behind it.
	 *
	 * That wrapper only exists on `<1.21.4 && !fabric`: 1.21.4 deleted the `BakedModel` hook it wraps,
	 * and Forge's `getRenderTypes` was never a thing on Fabric at all. On the other 37 nodes nothing
	 * ever selects the custom type, so vanilla's own choice stands — and for a non-`BlockItem`
	 * `ItemBlockRenderTypes.getRenderType` returns `Sheets.translucentItemSheet()` (bytecode-checked
	 * 1.20.1→26.2), i.e. ordinary alpha blending. An opaque texture through an alpha-blending render
	 * type is simply a solid pickaxe, which is what the reporter saw.
	 *
	 * So on exactly those nodes the alpha is lowered here instead. Alpha blending reaches the same
	 * place additive blending did — the tool reads as translucent — without needing the custom render
	 * type, which on `>=1.21.5` no longer exists at all (see the header of `AMRenderTypes`: every
	 * custom `RenderType` there falls back to a vanilla one, and rebuilding one now means registering
	 * a custom `RenderPipeline` per loader).
	 *
	 * The 12 nodes that **do** have the wrapper are deliberately left alone: their texture must stay
	 * opaque, because additive blending multiplies by the source alpha and lowering it would only dim
	 * upstream's own look.
	 */
	fun ghostifyPickaxeTexture(resourcesRoot: File, modId: String): Boolean {
		val texture = resourcesRoot.resolve("assets/$modId/textures/item/ghostly_pickaxe.png")
		if (!texture.isFile) return false
		val source = javax.imageio.ImageIO.read(texture) ?: return false
		// Read into a known ARGB raster first: setRGB on a TYPE_3BYTE_BGR image silently drops the
		// alpha byte, which would write the file back unchanged and report success.
		val image = java.awt.image.BufferedImage(source.width, source.height, java.awt.image.BufferedImage.TYPE_INT_ARGB)
		image.createGraphics().let { it.drawImage(source, 0, 0, null); it.dispose() }
		var changed = false
		for (y in 0 until image.height) {
			for (x in 0 until image.width) {
				val argb = image.getRGB(x, y)
				val alpha = argb ushr 24
				// Fully transparent pixels stay transparent, and anything already fainter than the
				// target keeps its own value — so this is idempotent and never *raises* opacity.
				if (alpha == 0 || alpha <= GHOST_PICKAXE_ALPHA) continue
				image.setRGB(x, y, (GHOST_PICKAXE_ALPHA shl 24) or (argb and 0x00FFFFFF))
				changed = true
			}
		}
		if (changed) javax.imageio.ImageIO.write(image, "png", texture)
		return changed
	}

	/**
	 * How opaque the ghostly pickaxe is left on the nodes without the additive render type.
	 *
	 * ~55%: enough to still read as a pickaxe against a bright sky, faint enough to read as a ghost's.
	 */
	private const val GHOST_PICKAXE_ALPHA = 140

	/**
	 * The ISTER-only icon items whose renderer is **alive again** on 1.21.4+ (#45/#48).
	 *
	 * ⚠️ These three are **registered items**, not the inert override targets they sat next to in the
	 * legacy model directory — an earlier version of this comment said otherwise and that is why they
	 * went unfixed for a release. `AMItemstackRenderer` draws each one from the stack's own NBT:
	 * `tab_icon` cycles through a mob every two seconds (or shows the one `DisplayEntityType` names),
	 * `fancy_item` draws whatever item `DisplayItem` names, and `effect_item` draws the icon of the
	 * effect in `DisplayEffect`. 1.21.4 deleted the ISTER mechanism, so from `2.0.3` to `2.0.8` these
	 * were substituted statically — the advancement icons repointed at spawn eggs by a
	 * `restaticAdvancementIcons` pass here, the creative tab and the dictionary's index page borrowing
	 * the dictionary's flat sprite. `AMIconSpecialRenderer` (a `minecraft:special` model renderer,
	 * type id `alexsmobs:icon`, registered on every ≥1.21.4 node by ClientProxy) now routes them back
	 * into the live renderer, so [writeItemModelDefinitions] emits a special-model definition for
	 * them, strips the dead parent off their base model (leaving the identity display transforms the
	 * old `builtin/entity` parent gave them), and the advancements keep their authored `custom_data`
	 * untouched.
	 *
	 * <p><b>`transmutation_table` joined them for #67.</b> It is a block item, not an icon, but it
	 * qualifies on the one property that matters here: its `AMItemstackRenderer` branch never reads
	 * the display context, so the 26.x arm of the special renderer — which has no context to pass and
	 * hardcodes GUI — draws it identically in hand, on the ground and in a frame. The in-hand items
	 * above (carver, shield of the deep, stink ray, …) each branch on the context and would be
	 * flattened by that, which is why they keep their static rebuilds. Before this it fell through to
	 * the block-model branch of [repairBuiltinEntityModel] and drew as `block/transmutation_table` —
	 * a plain obsidian cube, since the real table is the BER's job — on every node ≥1.21.4.
	 *
	 * <p><b>`mysterious_worm` joined them for the fifteenth pass</b>, on the same test: its branch of
	 * the legacy renderer builds `MYTERIOUS_WORM_MODEL`, calls `animateStack` and draws it, without
	 * ever reading the display context — so the 26.x arm's hardcoded GUI costs it nothing. Before
	 * this it fell through to the last branch of [repairBuiltinEntityModel] and became the flat
	 * `textures/item/mysterious_worm` sprite, i.e. **the wriggling worm stopped moving and went 2D**
	 * on every node ≥1.21.4. Reported as "the mysterious worm is missing its animation".
	 *
	 * <p><b>`shattered_dimensional_carver` joined them for #92</b>, and it is the one member that
	 * *does* read the display context — so the membership test above needed sharpening rather than
	 * bending. What the 26.x arm cannot survive is a context read that picks a **different model**
	 * (`falconry_glove`, `stink_ray`, `skelewag_sword` and `vine_lasso` swap between an `<id>_hand`
	 * cuboid and an `<id>_inventory` sprite, and flattening that to GUI would lose the in-hand look
	 * outright — they keep their `minecraft:select` definitions). The carver's two reads are
	 * cosmetic *nudges* to one unchanging assembly: a left-hand-only translate/scale/rotate, and
	 * `GROUND ? combinedLightIn : 240`. On 1.21.4 → 1.21.11 the real context was passed through and
	 * both applied; the six 26.x nodes lost it, which #96 restored — `ItemStackRenderStateMixin`
	 * lends the renderer the context 26.1 dropped from the `submit` signature, so the sentence above
	 * about "the 26.x arm hardcodes GUI" is now historical on every era.
	 *
	 * <p>Which was: the carver has **no sprite and no model of its own** — its entire appearance is
	 * `AMItemstackRenderer` drawing the eleven `dimensional_carver_shard_*` items (each an ordinary
	 * flat sprite of a real registered item) one on top of another, each offset by its own sine so
	 * they drift apart and back. From `2.0.3` this was substituted by a `minecraft:composite` of
	 * those same eleven models — the render **minus the drift**, which is the only part that made it
	 * a *shattered* carver. Stacked at zero offset the shards reassemble into the intact tool, so on
	 * all 30 nodes ≥1.21.4 the two dimensional carvers looked identical and neither moved. Reported
	 * as "the texture for the shattered dimensional carver is the same as the dimensional carver"
	 * and "the two dimensional pickaxes used to have 3D animations". The composite mechanism had
	 * exactly this one user and is deleted with it, so nothing can be filed back under it.
	 */
	private val LIVE_ICON_ITEMS =
		setOf(
			"tab_icon", "fancy_item", "effect_item", "transmutation_table", "mysterious_worm",
			"shattered_dimensional_carver",
		)

	/**
	 * ISTER items rebuilt as an ordinary `elements` model, keyed by the dead model they replace.
	 *
	 * The shield of the deep is the one ISTER item with neither a sprite, a block model, nor a stack of
	 * existing models to rebuild from: its renderer drew a Java `ModelShieldOfTheDeep`, four cuboids
	 * UV-mapped onto `textures/armor/shield_of_the_deep.png`. So it was invisible from 1.21.4 up,
	 * alongside the carver and for the same reason.
	 *
	 * Those four cuboids are plain axis-aligned boxes, which is exactly what a block-model `element` is,
	 * so `models/item/shield_of_the_deep_3d.json` is that model converted by hand — geometry, box UVs
	 * and `AMItemstackRenderer`'s pose folded together, then the original file's `display` block and
	 * `gui_light` reused verbatim, because those were authored against this exact geometry. Two
	 * conventions had to line up and both were read out of the decompiled sources rather than recalled:
	 * `ModelPart.Cube`'s box unwrap, and which extreme of an element `FaceBakery.defaultFaceUV` anchors
	 * each `uv` corner to. The pose mirrors X and Z, which is why most faces come out with reversed
	 * `uv` pairs. The derivation is reproducible — see the generator quoted in docs/notes/bug-reports.md.
	 *
	 * **The conversion is `E = 16 * pose`, with no half-block term** — verified twice over, once from
	 * the bytecode and once from a client. `ItemTransform.apply` ends with `translate(-0.5, -0.5, -0.5)`
	 * (it is the *last* call in the method, so it is the *first* thing applied to a vertex), and the
	 * ISTER ran inside that same translated frame, exactly as an element's `E/16` does. A `2.0.6`
	 * working-tree attempt to add `+ 8` on the theory that the ISTER escaped it broke third person, which
	 * had been correct, and did not fix first person — so the frames really are the same and the fold's
	 * geometry was never the fault in report #33. Do not reintroduce the offset.
	 *
	 * The one real geometry error was element 3 (the boss), which carried a stray `+1` on X: its
	 * `addBox(-4, -1, -3, 3, 6, 6)` under `setPos(-2, 16, 0)` gives `9.4 .. 12.4`, not `10.4 .. 13.4`.
	 * With that corrected all four elements match `ModelShieldOfTheDeep`'s `addBox` calls run through
	 * `Axis.YP.rotationDegrees(-180)` and `translate(0.4, -0.75, 0.5)` exactly. The `display` block is
	 * upstream's, untouched.
	 *
	 * The texture is copied to `textures/item/shield_of_the_deep_3d.png` because only `textures/item`
	 * and `textures/block` are stitched into the atlas an `elements` model samples; `textures/armor` is
	 * not, and the model would draw the missing-texture checker. The separate name also keeps
	 * [repairBuiltinEntityModel]'s "a `textures/item/<id>` exists" branch from firing on the dead model
	 * and flattening the whole 64×64 sheet into a sprite.
	 *
	 * A `_blocking` sibling gets a `minecraft:condition` on `minecraft:using_item`, which is what 1.21.4
	 * replaced the `blocking` item property with — restoring an override that had been dead since the
	 * `overrides` list went away.
	 */
	private val REBUILT_MODELS = mapOf("shield_of_the_deep" to "shield_of_the_deep_3d")

	/**
	 * Item tint sources of the mod's own, keyed by item id, in `tintIndex` order.
	 *
	 * 1.21.4 deleted `RegisterColorHandlersEvent.Item` (and Fabric's `ColorProviderRegistry.ITEM`
	 * with it): a tint is now a `tints` array on the item's model definition, one entry per
	 * `tintIndex`, each naming a registered `ItemTintSource`. The straddleboard is the mod's only
	 * tinted item and it had nothing to move to, so its dye had been a cosmetic loss on all 30 nodes
	 * ≥1.21.4 since `2.0.0` — layer0 (the wooden base) and layer1 (the grey panel) both drew
	 * untinted, and dyeing a board changed only the entity, never the icon.
	 *
	 * `AMStraddleboardTint` supplies both indices, so the dye comes back and the two contrast
	 * settings (`straddleboardBaseColor`, `straddleboardPanelColor`) reach the icon on every node.
	 * A `minecraft:constant` would not do: it is read once when the definition is parsed, and these
	 * have to answer per draw.
	 */
	private val CUSTOM_TINTS: Map<String, List<String>> =
		mapOf("straddleboard" to listOf("straddleboard_base", "straddleboard_panel"))

	/**
	 * Removes the render-state mixins from the mod's mixin config on nodes that cannot compile them.
	 *
	 * They mix into `EntityRenderState`, which only exists from 1.21.2, so below that their source
	 * package is excluded from the compile (see `ModPlatformPlugin.configureJava`) and no `.class`
	 * reaches the jar. But **Fletching Table populates the config's `mixins` array itself**, by
	 * scanning for `@Mixin`-annotated sources — and that scan does not honour the source-set
	 * `exclude`, so it lists them on every node, including the ones that dropped them. A mixin
	 * config naming an absent class is a hard load failure, not a warning:
	 *
	 *     InvalidMixinException: The specified mixin '…renderstate.EntityRenderStateMixin'
	 *     was not found
	 *
	 * — the game never reaches mod loading. (This is also why the whole config's `client` list turns
	 * up duplicated into `mixins` in the built jar: same scan, and harmless because the classes are
	 * present, just dist-cleaned on a server.)
	 *
	 * So the entries are pruned here rather than added: the `mixins`/`client` arrays are rewritten
	 * without them, dropping the now-dangling comma if the array empties out.
	 */
	fun pruneRenderStateMixins(resourcesRoot: File, modId: String): Int =
		pruneMixinEntries(resourcesRoot, modId, listOf("renderstate.EntityRenderStateMixin", "renderstate.EntityRendererMixin"))

	/**
	 * The general form of the above: drop [entries] from the mixin config's arrays, for any node
	 * whose source set excludes the classes that back them.
	 */
	fun pruneMixinEntries(resourcesRoot: File, modId: String, entries: List<String>): Int {
		val config = resourcesRoot.resolve("$modId.mixins.json")
		if (!config.isFile) return 0
		val original = config.readText()
		var text = original
		var removed = 0
		entries.forEach { entry ->
			// Match the quoted entry plus whatever separator sits on either side of it, so the
			// remaining array stays valid JSON whether the entry was first, last or in the middle.
			val pattern = Regex(""",\s*"${Regex.escape(entry)}"|"${Regex.escape(entry)}"\s*,|"${Regex.escape(entry)}"""")
			if (pattern.containsMatchIn(text)) {
				text = pattern.replace(text, "")
				removed++
			}
		}
		if (text == original) return 0
		config.writeText(text)
		return removed
	}

	/**
	 * Drops every entry in a whole mixin **package** from the config's arrays.
	 *
	 * [pruneMixinEntries] names classes one by one, which is right when the reason a class is absent
	 * is specific to that class. `mixin.fabric.**` is the other shape: the entire package exists only
	 * because Fabric has no event bus, and it is excluded wholesale from the compile on Forge and
	 * NeoForge — so listing the classes here would just be a second place to forget to update when
	 * one is added. [prefix] is matched against the entry as written in the config, i.e. relative to
	 * the config's `package` (`"fabric."`).
	 *
	 * Rewritten via the parser rather than spliced, so it is idempotent and cannot leave a dangling
	 * comma; returns the number of entries removed across both arrays.
	 */
	fun pruneMixinPackage(resourcesRoot: File, modId: String, prefix: String): Int {
		val config = resourcesRoot.resolve("$modId.mixins.json")
		if (!config.isFile) return 0
		val root = runCatching { json.parseToJsonElement(config.readText()) }.getOrNull() as? JsonObject ?: return 0

		var removed = 0
		val rebuilt = buildJsonObject {
			root.forEach { (key, value) ->
				val array = value as? JsonArray
				if ((key == "mixins" || key == "client") && array != null) {
					val keep = array.filterNot { (it as? JsonPrimitive)?.content?.startsWith(prefix) == true }
					removed += array.size - keep.size
					put(key, JsonArray(keep))
				} else {
					put(key, value)
				}
			}
		}
		if (removed == 0) return 0
		config.writeText(json.encodeToString(JsonObject.serializer(), rebuilt))
		return removed
	}

	/**
	 * Mixin packages in this mod whose targets are CLIENT classes, and which Fletching Table
	 * nevertheless lists in the common `mixins` array — see [partitionClientMixins].
	 *
	 * The `mixin.client` package is obvious from the name. `mixin.renderstate` is the non-obvious
	 * one: it mixes into `EntityRenderer` / `EntityRenderState`, which are just as client-only, and
	 * it is the reason a `>=1.21.2` node's dist-cleaner noise is nine lines rather than five.
	 *
	 * `mixin.fabric.client` needs its own entry rather than riding on `client.`: these are matched
	 * with `startsWith` against the package path relative to `mixin`, so a nested package does not
	 * match its own leaf name. It is the Fabric-only half of the dispatcher (`mixin.fabric` proper
	 * is server/common and must stay in `mixins`), and it exists only on Fabric — the other two
	 * loaders drop the whole `fabric.` prefix in [pruneMixinPackage] before this pass would see it.
	 *
	 * (Written without a trailing glob on purpose — a `/` followed by two asterisks inside a KDoc
	 * opens a NESTED Kotlin block comment and swallows the rest of the file.)
	 */
	private val clientMixinPackages = listOf("client.", "renderstate.", "fabric.client.")

	/**
	 * Moves every client-only entry out of the mixin config's `mixins` array and into `client`.
	 *
	 * **Fabric only, and it is not cosmetic — it is the difference between a dedicated server that
	 * boots and one that does not.** Fletching Table populates `mixins` by scanning for `@Mixin`
	 * sources, and that scan knows nothing about dists, so client mixins land in the common list
	 * (the `client` list is authored separately, which is why four of them appear twice — see
	 * [pruneRenderStateMixins]). On Forge and NeoForge that is harmless: `RuntimeDistCleaner` /
	 * `NeoForgeDevDistCleaner` refuse to hand a client class to the transformer on a server, which
	 * is exactly where this repo's documented benign `/ERROR]` lines come from.
	 *
	 * **Fabric has no dist cleaner.** An entry under `mixins` is applied on both dists, so a
	 * dedicated server would try to apply e.g. `client.GuiMixin` to `net.minecraft.client.gui.Gui`
	 * — a class that is not on a server's classpath at all — and mixin aborts the launch. The
	 * `client` array is precisely the mechanism for saying "client dist only", so this makes it
	 * the only place those entries appear.
	 *
	 * Returns the number of entries moved. The config is re-emitted rather than spliced, so the
	 * pass is idempotent and cannot leave a dangling comma.
	 */
	fun partitionClientMixins(resourcesRoot: File, modId: String): Int {
		val config = resourcesRoot.resolve("$modId.mixins.json")
		if (!config.isFile) return 0
		val root = runCatching { json.parseToJsonElement(config.readText()) }.getOrNull() as? JsonObject ?: return 0
		fun arrayAt(key: String) = (root[key] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.content }.orEmpty()

		val common = arrayAt("mixins")
		val client = arrayAt("client")
		val (moved, keep) = common.partition { entry ->
			entry in client || clientMixinPackages.any { entry.startsWith(it) }
		}
		if (moved.isEmpty()) return 0

		val rebuilt = buildJsonObject {
			root.forEach { (key, value) ->
				when (key) {
					"mixins" -> put(key, JsonArray(keep.map { JsonPrimitive(it) }))
					"client" -> put(key, JsonArray((client + moved.filterNot { it in client }).map { JsonPrimitive(it) }))
					else -> put(key, value)
				}
			}
		}
		config.writeText(json.encodeToString(JsonObject.serializer(), rebuilt))
		return moved.size
	}

	/**
	 * MC 1.21 turned looting into an enchantment *effect*, and deleted the two loot symbols that
	 * named the looting level directly: the `looting_enchant` function and the
	 * `random_chance_with_looting` condition. Both have exact replacements that name the enchantment
	 * explicitly, so this is a shape rewrite and not a behaviour change.
	 *
	 * It is not optional: an unknown loot function or condition id fails the *whole* table to parse
	 * (logged, not thrown), so leaving them in silently deletes every affected mob's entire drop
	 * table — 55 entries across 41 tables here.
	 */
	fun migrateLootTo121(resourcesRoot: File): Int {
		val data = resourcesRoot.resolve("data")
		if (!data.isDirectory) return 0
		var changed = 0
		data.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { file ->
			val dirs = file.invariantSeparatorsPath
			if (!dirs.contains("/loot_table/") && !dirs.contains("/item_modifier/")) return@forEach
			val original = runCatching { json.parseToJsonElement(file.readText()) }.getOrNull()
				?: return@forEach
			val migrated = migrateLootingNode(original)
			if (migrated != original) {
				file.writeText(json.encodeToString(JsonElement.serializer(), migrated))
				changed++
			}
		}
		return changed
	}

	private const val LOOTING = "minecraft:looting"

	private fun migrateLootingNode(node: JsonElement): JsonElement = when (node) {
		is JsonArray -> JsonArray(node.map(::migrateLootingNode))
		is JsonObject -> {
			val mapped = JsonObject(LinkedHashMap<String, JsonElement>().also { out ->
				node.forEach { (key, value) -> out[key] = migrateLootingNode(value) }
			})
			when {
				mapped.idOf("function") == "minecraft:looting_enchant" -> lootingEnchant(mapped)
				mapped.idOf("condition") == "minecraft:random_chance_with_looting" ->
					randomChanceWithLooting(mapped)
				else -> mapped
			}
		}
		else -> node
	}

	/** The `minecraft:` prefix is optional in data-pack ids, and these tables leave it off. */
	private fun JsonObject.idOf(key: String): String? =
		(this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
			?.let { if (it.contains(':')) it else "minecraft:$it" }

	/** `looting_enchant` -> `enchanted_count_increase`; `count` and `limit` carry over unchanged. */
	private fun lootingEnchant(node: JsonObject): JsonObject = buildJsonObject {
		put("function", JsonPrimitive("minecraft:enchanted_count_increase"))
		// The enchantment used to be implicit in the function's name.
		put("enchantment", JsonPrimitive(LOOTING))
		node.forEach { (key, value) -> if (key != "function") put(key, value) }
	}

	private fun randomChanceWithLooting(node: JsonObject): JsonObject {
		val chance = node["chance"] as? JsonPrimitive ?: return node
		val multiplier = (node["looting_multiplier"] as? JsonPrimitive)?.doubleOrNull ?: return node
		val base = chance.doubleOrNull?.let { round6(it + multiplier) } ?: return node
		return buildJsonObject {
			put("condition", JsonPrimitive("minecraft:random_chance_with_enchanted_bonus"))
			put("unenchanted_chance", chance)
			// The old condition was a flat `chance + level * multiplier`. A linear level-based value
			// whose base is the level-1 result reproduces that exactly for every level >= 1, and
			// level 0 is what unenchanted_chance covers — so the drop rates are unchanged.
			put("enchanted_chance", buildJsonObject {
				put("type", JsonPrimitive("minecraft:linear"))
				put("base", JsonPrimitive(base))
				put("per_level_above_first", JsonPrimitive(multiplier))
			})
			put("enchantment", JsonPrimitive(LOOTING))
			node.forEach { (key, value) ->
				if (key != "condition" && key != "chance" && key != "looting_multiplier") put(key, value)
			}
		}
	}

	/** Keeps 0.2 + 0.1 from being written out as 0.30000000000000004. */
	private fun round6(value: Double): Double = Math.round(value * 1_000_000.0) / 1_000_000.0

	private val singularFolders = mapOf(
		"advancements" to "advancement",
		"recipes" to "recipe",
		"loot_tables" to "loot_table",
		"structures" to "structure",
		"predicates" to "predicate",
		"item_modifiers" to "item_modifier",
		"functions" to "function",
	)

	private val singularTagFolders = mapOf(
		"blocks" to "block",
		"items" to "item",
		"entity_types" to "entity_type",
		"fluids" to "fluid",
		"game_events" to "game_event",
		"functions" to "function",
	)

	/**
	 * MC 1.21.2 rewrote the recipe `Ingredient` JSON. `Ingredient` became a `HolderSet<Item>`, whose
	 * codec accepts only a **string** (`"minecraft:paper"` for a single item, `"#forge:rods/wooden"`
	 * for a tag) or a **JSON array** of those — the old `{"item": …}` / `{"tag": …}` object forms are
	 * gone. An unrecognised ingredient shape fails the *whole* recipe to parse (logged, not thrown), so
	 * every crafting/cooking recipe in this mod silently vanished on >= 1.21.2 until this ran.
	 *
	 * Only the ingredient-bearing fields are touched (`ingredient`, `ingredients`, `key`, and the
	 * smithing `base`/`addition`/`template`), never `result`, so this is independent of the 1.20.5
	 * result migration.
	 */
	fun migrateIngredientsTo1212(resourcesRoot: File): Int {
		val data = resourcesRoot.resolve("data")
		if (!data.isDirectory) return 0
		var changed = 0
		data.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { file ->
			val dirs = file.invariantSeparatorsPath
			// capsid_recipes is this mod's own recipe type; its `ingredients` go through Ingredient.CODEC too.
			if (!dirs.contains("/recipe/") && !dirs.contains("/recipes/") &&
				!dirs.contains("/capsid_recipes/")) return@forEach
			val original = runCatching { json.parseToJsonElement(file.readText()) as? JsonObject }
				.getOrNull() ?: return@forEach
			val migrated = migrateRecipeIngredients(original)
			if (migrated != original) {
				file.writeText(json.encodeToString(JsonObject.serializer(), migrated))
				changed++
			}
		}
		return changed
	}

	private val ingredientFields = listOf("ingredient", "ingredients", "base", "addition", "template")

	private fun migrateRecipeIngredients(recipe: JsonObject): JsonObject {
		val out = LinkedHashMap(recipe)
		ingredientFields.forEach { field ->
			recipe[field]?.let { out[field] = convertIngredient(it) }
		}
		// crafting_shaped: `key` is a char -> ingredient map.
		(recipe["key"] as? JsonObject)?.let { key ->
			out["key"] = JsonObject(key.mapValues { convertIngredient(it.value) })
		}
		return JsonObject(out)
	}

	/**
	 * Vanilla item tags that vanilla itself deleted at 1.21.2 have no bootstrap `TagKey`, so an
	 * `Ingredient` referencing `#<that tag>` cannot be decoded during `SimpleJsonResourceReloadListener`
	 * `prepare()` (the reload's tag-bound provider doesn't know an unregistered tag) — it fails with
	 * "Missing tag" and the whole recipe silently drops. This mod's only such reference is
	 * `minecraft:music_discs`, consumed solely by the `music_disc_daze` capsid recipe; the tag contains
	 * exactly these two mod discs. `Ingredient.CODEC` accepts a bare array of item ids as a direct
	 * holder set (no tag lookup), so we inline the members here and sidestep the binding-timing problem
	 * entirely. (`data/minecraft/tags/item/music_discs.json` is still shipped for the <1.21.2 path, which
	 * uses the object `{"tag":…}` form vanilla still resolves there.)
	 */
	private val expandedTags = mapOf(
		"minecraft:music_discs" to listOf("alexsmobs:music_disc_daze", "alexsmobs:music_disc_thime")
	)

	/**
	 * `{"item": x}` -> `"x"`, `{"tag": y}` -> `"#y"` (or an inlined item array for an expanded tag),
	 * an array -> each element converted; else unchanged.
	 */
	private fun convertIngredient(node: JsonElement): JsonElement = when (node) {
		is JsonArray -> JsonArray(node.map(::convertIngredient))
		is JsonObject -> {
			val item = (node["item"] as? JsonPrimitive)?.takeIf { it.isString }
			val tag = (node["tag"] as? JsonPrimitive)?.takeIf { it.isString }
			when {
				node.size == 1 && item != null -> item
				node.size == 1 && tag != null ->
					expandedTags[tag.content]?.let { members ->
						JsonArray(members.map { JsonPrimitive(it) })
					} ?: JsonPrimitive("#" + tag.content)
				else -> node
			}
		}
		else -> node
	}

	private fun migrateRecipe(recipe: JsonObject): JsonObject = when (val result = recipe["result"]) {
		is JsonObject -> recipe.replacing("result", toComponentStack(result))
		// Cooking recipes (smelting/smoking/campfire) used to name their result as a bare item id;
		// 1.20.5 made every recipe result a full item stack.
		is JsonPrimitive -> if (result.isString) {
			recipe.replacing("result", buildJsonObject { put("id", result) })
		} else recipe
		else -> recipe
	}

	private fun migrateAdvancement(advancement: JsonObject): JsonObject {
		var out = advancement
		val display = out["display"] as? JsonObject
		val icon = display?.get("icon") as? JsonObject
		if (display != null && icon != null) {
			out = out.replacing("display", display.replacing("icon", toComponentStack(icon)))
		}
		val criteria = out["criteria"] as? JsonObject ?: return out
		return out.replacing("criteria", JsonObject(LinkedHashMap<String, JsonElement>().also { map ->
			criteria.forEach { (name, criterion) ->
				map[name] = (criterion as? JsonObject)?.let { c ->
					(c["conditions"] as? JsonObject)
						?.let { c.replacing("conditions", migrateItemPredicateFields(it)) } ?: c
				} ?: criterion
			}
		}))
	}

	/**
	 * Criterion condition fields whose value is an `ItemPredicate` (or a list of them).
	 *
	 * An allowlist for the same reason [entityPredicateFields] is one — `items` also names a plain
	 * id list elsewhere, and a shape heuristic would rewrite those too.
	 */
	private val itemPredicateFields = setOf("item", "items")

	/**
	 * MC 1.20.5 rebuilt `ItemPredicate`: the mutually exclusive `item`/`tag` pair became a single
	 * `items` holder set, spelled `"#tag"` for a tag and a bare id (or an array of ids) for items.
	 *
	 * The dangerous half is that the old spellings are not *rejected* — `ItemPredicate.CODEC` is a
	 * record codec of optional fields, so an unknown key is simply dropped and what is left decodes
	 * as a predicate with **no** conditions, which matches every stack. So `alexsmobs:banana`'s
	 * `{"tag": "alexsmobs:bananas"}` turned into "the player has any item at all" and the
	 * `inventory_changed` trigger granted "Gone Bananas" the instant a world was entered (report
	 * #31). `alexsmobs:mantis_shrimp_bucket`'s `{"item": "minecraft:water_bucket"}` had the same
	 * shape and granted on interacting with a mantis shrimp holding anything.
	 *
	 * Nothing logs, nothing fails to load, and no gate can see it — the advancement still exists and
	 * still fires, it just fires on the wrong thing.
	 *
	 * Idempotent: an already-migrated predicate has neither legacy key. Vanilla kept `count`,
	 * `components` and `predicates` alongside `items`, so those ride through untouched.
	 */
	private fun migrateItemPredicateFields(conditions: JsonObject): JsonObject =
		JsonObject(LinkedHashMap<String, JsonElement>().also { out ->
			conditions.forEach { (key, value) ->
				out[key] = if (key in itemPredicateFields) migrateItemPredicate(value) else value
			}
		})

	private fun migrateItemPredicate(node: JsonElement): JsonElement = when (node) {
		is JsonArray -> JsonArray(node.map(::migrateItemPredicate))
		is JsonObject -> {
			val item = (node["item"] as? JsonPrimitive)?.takeIf { it.isString }
			val tag = (node["tag"] as? JsonPrimitive)?.takeIf { it.isString }
			when {
				node.containsKey("items") -> node
				item != null -> node.without("item").replacing("items", item)
				tag != null -> node.without("tag").replacing("items", JsonPrimitive("#" + tag.content))
				else -> node
			}
		}
		else -> node
	}

	/** Items vanilla renamed in 1.20.5. Only the ones this mod's data actually references. */
	private val renamedItems = mapOf("minecraft:scute" to "minecraft:turtle_scute")

	/**
	 * Loot tables were componentised alongside item stacks: the NBT-shaped functions grew
	 * `custom_data` names, potions became their own function, and a nested-table entry names its
	 * target with `value` rather than `name`.
	 */
	private fun migrateLootTable(node: JsonObject): JsonObject {
		val out = LinkedHashMap<String, JsonElement>()
		val isNestedTableEntry = (node["type"] as? JsonPrimitive)?.content == "minecraft:loot_table"
		node.forEach { (key, value) ->
			val newKey = if (key == "name" && isNestedTableEntry) "value" else key
			out[newKey] = when {
				key == "name" && value is JsonPrimitive && value.isString ->
					JsonPrimitive(renamedItems[value.content] ?: value.content)
				else -> migrateLootNode(value)
			}
		}
		return migrateLootFunction(JsonObject(out))
	}

	private fun migrateLootNode(node: JsonElement): JsonElement = when (node) {
		is JsonObject -> migrateLootTable(node)
		is JsonArray -> JsonArray(node.map(::migrateLootNode))
		else -> node
	}

	private fun migrateLootFunction(node: JsonObject): JsonObject {
		val tag = (node["tag"] as? JsonPrimitive)?.takeIf { it.isString }?.content
		return when ((node["function"] as? JsonPrimitive)?.content) {
			"minecraft:set_nbt" -> {
				val parsed = tag?.let { Snbt.parse(it) as? JsonObject } ?: return node
				// The `Potion` tag became the potion_contents component, which set_custom_data
				// cannot reach — vanilla split it out into its own function.
				val potion = (parsed["Potion"] as? JsonPrimitive)?.takeIf { it.isString }
				if (potion != null && parsed.size == 1) {
					buildJsonObject {
						put("function", JsonPrimitive("minecraft:set_potion"))
						put("id", potion)
					}
				} else {
					// set_custom_data's tag is still SNBT (TagParser.LENIENT_CODEC), so it is
					// carried over as-is; only the function name moved.
					node.replacing("function", JsonPrimitive("minecraft:set_custom_data"))
				}
			}
			"minecraft:copy_nbt" -> node.replacing("function", JsonPrimitive("minecraft:copy_custom_data"))
			else -> node
		}
	}

	/** `{"item": x, "nbt": "<snbt>"}` -> `{"id": x, "components": {"minecraft:custom_data": {…}}}`. */
	private fun toComponentStack(stack: JsonObject): JsonObject {
		val item = stack["item"] ?: return stack
		return buildJsonObject {
			put("id", item)
			stack.forEach { (key, value) ->
				when (key) {
					"item", "nbt" -> {}
					else -> put(key, value)
				}
			}
			val nbt = (stack["nbt"] as? JsonPrimitive)?.takeIf { it.isString }?.content
			if (nbt != null) {
				// CustomData.CODEC is CompoundTag.CODEC.xmap(…) — it takes a JSON object, not an
				// SNBT string (checked against the 1.20.6 jar), so the tag is converted here.
				put("components", buildJsonObject { put("minecraft:custom_data", Snbt.parse(nbt)) })
			}
		}
	}

	private fun JsonObject.replacing(key: String, value: JsonElement): JsonObject =
		JsonObject(LinkedHashMap(this).also { it[key] = value })

	private fun JsonObject.without(key: String): JsonObject =
		JsonObject(LinkedHashMap(this).also { it.remove(key) })

	// ------------------------------------------------------- 26.2 entity predicates

	/**
	 * Fields of the legacy flat `EntityPredicate` record, mapped to the id they are registered under
	 * in 26.2's `ENTITY_SUB_PREDICATE_TYPE` registry.
	 *
	 * Every one of those registered codecs is an `xmap` over exactly the codec the old flat field
	 * used (verified against 26.2's `EntitySubPredicates.bootstrap` and each predicate class), so
	 * apart from the `type` -> `entity_type` rename this is a pure key rewrite — no value shape
	 * changes. `location`/`stepping_on`/`movement_affected_by` used to be inlined by the
	 * `LocationWrapper` sub-codec and `components`/`predicates` by `DataComponentMatchers`, but at
	 * the same names, so they map straight through too.
	 */
	private val entitySubPredicateKeys = mapOf(
		"type" to "entity_type",
		"distance" to "distance",
		"movement" to "movement",
		"location" to "location",
		"stepping_on" to "stepping_on",
		"movement_affected_by" to "movement_affected_by",
		"effects" to "effects",
		"nbt" to "nbt",
		"flags" to "flags",
		"equipment" to "equipment",
		"periodic_tick" to "periodic_tick",
		"vehicle" to "vehicle",
		"passenger" to "passenger",
		"targeted_entity" to "targeted_entity",
		"team" to "team",
		"slots" to "slots",
		"components" to "components",
		"predicates" to "predicates",
	)

	/** The sub-predicate values that are themselves an `EntityPredicate`, so recurse into them. */
	private val nestedEntityPredicateKeys = setOf("vehicle", "passenger", "targeted_entity")

	/**
	 * Advancement-criterion condition fields whose value is an `EntityPredicate`.
	 *
	 * Deliberately an allowlist rather than a shape heuristic: sibling fields of a criterion carry
	 * other predicate types with overlapping key names (`minecraft:effects_changed` has an `effects`
	 * field that is a `MobEffectsPredicate`, and an `ItemPredicate` has `components`/`predicates`),
	 * so anything structural would rewrite them too. `entity`, `child` and `player` are what this
	 * mod uses; the rest are the other vanilla trigger fields, listed so a new advancement does not
	 * quietly go unmigrated.
	 */
	private val entityPredicateFields = setOf(
		"entity", "player", "child", "parent", "partner", "victim", "attacker", "source",
		"zombie", "villager", "lightning", "bystander", "projectile", "shooter", "owner",
		"source_entity", "direct_entity",
	)

	/**
	 * MC 26.2 rewrote `EntityPredicate` from a flat record into
	 * `Codec.dispatchedMap(BuiltInRegistries.ENTITY_SUB_PREDICATE_TYPE.byNameCodec(), c -> c)` —
	 * every key of the object is now a sub-predicate *registry id*, and the value is that
	 * sub-predicate's own payload.
	 *
	 * So the old `"type"` field is read as a sub-predicate named `minecraft:type`, which does not
	 * exist, and the whole file fails to decode:
	 *
	 *     Couldn't parse data file 'alexsmobs:alexsmobs/alligator_snapping_turtle' …
	 *     Unknown registry key in ResourceKey[minecraft:root / minecraft:entity_sub_predicate_type]:
	 *     minecraft:type
	 *
	 * That is logged and not thrown, so the server still reaches `Done (` while 42 of this mod's
	 * advancements silently do not exist. `type` becomes `entity_type`; every other legacy field
	 * keeps its name (see [entitySubPredicateKeys]).
	 *
	 * The one structural change is `type_specific`, which used to be a nested dispatch on its own
	 * `type` field and is now flattened into the outer map under `type_specific/<type>` — e.g.
	 * `{"type_specific": {"type": "player", "looking_at": …}}` becomes
	 * `{"type_specific/player": {"looking_at": …}}`.
	 *
	 * Entity predicates are reached from two places, both of which this mod uses: an advancement
	 * criterion's condition fields, and the `predicate` of a `minecraft:entity_properties` loot
	 * condition (which is how the two spyglass advancements express "looking at a bison"). Both are
	 * matched by context rather than by shape — see [entityPredicateFields].
	 *
	 * Idempotent: only the legacy spellings are rewritten, and every key except `type` and
	 * `type_specific` is its own replacement.
	 */
	/**
	 * What `#minecraft:dirt` lost when MC 26.1 split it, and the tags that hold those blocks now.
	 *
	 * Membership diffed straight out of the vanilla client jars (1.21.11 -> 26.2): the tag went from
	 * ten blocks to three, and the seven that left were re-homed into `#minecraft:grass_blocks`
	 * (grass_block, podzol, mycelium), `#minecraft:moss_blocks` (moss_block, pale_moss_block) and
	 * `#minecraft:mud` (mud, muddy_mangrove_roots). Nothing was deleted and nothing was gained — it
	 * is purely a re-partition, so re-adding these three restores the old membership exactly.
	 */
	private val dirtSplitTo261 = listOf("#minecraft:grass_blocks", "#minecraft:moss_blocks", "#minecraft:mud")

	/**
	 * MC 26.1 split `#minecraft:dirt`, and this mod's `#alexsmobs:am_spawns` is built on it (#70).
	 *
	 * `am_spawns` is the ground list behind fourteen mob spawn tags *and* `leafcutter_pupa_usable_on`,
	 * so on 26.1+ the grass block silently stopped counting as ground: the leafcutter pupa refused to
	 * be placed on grass (which is how this was reported), and fourteen mobs stopped spawning on it.
	 *
	 * A tag reference to a *missing* tag is a hard load error rather than an ignored line, and
	 * `#minecraft:grass_blocks` / `#minecraft:moss_blocks` do not exist below 26.1 — hence a migration
	 * pass gated on the version rather than three more entries in the source file. It rewrites every
	 * mod tag that references `#minecraft:dirt`, not just `am_spawns`, so a future one is covered too.
	 */
	fun migrateDirtTagTo261(resourcesRoot: File): Int {
		val data = resourcesRoot.resolve("data")
		if (!data.isDirectory) return 0
		var changed = 0
		data.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { file ->
			// Both spellings, so this is independent of the 1.21 singular-folder rename ordering.
			if (!file.invariantSeparatorsPath.contains("/tags/")) return@forEach
			val original = runCatching { json.parseToJsonElement(file.readText()) as? JsonObject }
				.getOrNull() ?: return@forEach
			val values = original["values"] as? JsonArray ?: return@forEach
			val present = values.mapNotNull { (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.content }
			if ("#minecraft:dirt" !in present) return@forEach
			val missing = dirtSplitTo261.filter { it !in present }
			if (missing.isEmpty()) return@forEach
			val migrated = original.replacing("values", JsonArray(values + missing.map { JsonPrimitive(it) }))
			file.writeText(json.encodeToString(JsonObject.serializer(), migrated))
			changed++
		}
		return changed
	}

	fun migrateEntityPredicatesTo262(resourcesRoot: File): Int {
		val data = resourcesRoot.resolve("data")
		if (!data.isDirectory) return 0
		var changed = 0
		data.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { file ->
			val original = runCatching { json.parseToJsonElement(file.readText()) }.getOrNull()
				?: return@forEach
			val migrated = rewriteEntityPredicateHosts(original)
			if (migrated != original) {
				file.writeText(json.encodeToString(JsonElement.serializer(), migrated))
				changed++
			}
		}
		return changed
	}

	/** Walks a whole document, migrating every entity predicate it can positively identify. */
	private fun rewriteEntityPredicateHosts(node: JsonElement): JsonElement = when (node) {
		is JsonArray -> JsonArray(node.map(::rewriteEntityPredicateHosts))
		is JsonObject -> {
			val mapped = JsonObject(LinkedHashMap<String, JsonElement>().also { out ->
				node.forEach { (key, value) -> out[key] = rewriteEntityPredicateHosts(value) }
			})
			when {
				// A loot condition. `predicate` names an ItemPredicate on match_tool and a
				// DamageSourcePredicate on damage_source_properties, so the id has to be checked.
				mapped.idOf("condition") == "minecraft:entity_properties" ->
					(mapped["predicate"] as? JsonObject)
						?.let { mapped.replacing("predicate", migrateEntityPredicate(it)) } ?: mapped
				// An advancement criterion.
				mapped["trigger"] is JsonPrimitive && mapped["conditions"] is JsonObject ->
					mapped.replacing("conditions", migrateCriterionConditions(mapped["conditions"] as JsonObject))
				else -> mapped
			}
		}
		else -> node
	}

	private fun migrateCriterionConditions(conditions: JsonObject): JsonObject =
		JsonObject(LinkedHashMap<String, JsonElement>().also { out ->
			conditions.forEach { (key, value) ->
				// A JsonArray here is a ContextAwarePredicate (a list of loot conditions), which the
				// entity_properties branch above has already handled.
				out[key] = if (key in entityPredicateFields && value is JsonObject)
					migrateEntityPredicate(value) else value
			}
		})

	private fun migrateEntityPredicate(predicate: JsonObject): JsonObject =
		JsonObject(LinkedHashMap<String, JsonElement>().also { out ->
			predicate.forEach { (key, value) ->
				when {
					key == "type_specific" && value is JsonObject -> {
						// The nested dispatch key moves into the outer map's key and out of the value.
						val type = (value["type"] as? JsonPrimitive)?.takeIf { it.isString }?.content
							?.substringAfter(':')
						if (type == null) {
							out[key] = value
						} else {
							out["type_specific/$type"] = migrateTypeSpecific(type, value)
						}
					}
					key in nestedEntityPredicateKeys && value is JsonObject ->
						out[entitySubPredicateKeys.getValue(key)] = migrateEntityPredicate(value)
					else -> out[entitySubPredicateKeys[key] ?: key] = value
				}
			}
		})

	/** Strips the dispatch key, and migrates the one sub-predicate field that nests a predicate. */
	private fun migrateTypeSpecific(type: String, value: JsonObject): JsonObject =
		JsonObject(LinkedHashMap<String, JsonElement>().also { out ->
			value.forEach { (key, nested) ->
				when {
					key == "type" -> {}
					// PlayerPredicate#lookingAt is an EntityPredicate.
					type == "player" && key == "looking_at" && nested is JsonObject ->
						out[key] = migrateEntityPredicate(nested)
					else -> out[key] = nested
				}
			}
		})

	// ---------------------------------------------------------------- NeoForge

	/**
	 * The source tree is written for Forge, so everything Forge-namespaced has to be re-pointed for
	 * a NeoForge node. Three separate things, only the last of which is version-dependent:
	 *
	 *  - NeoForge reads its registries out of `data/<ns>/neoforge/…` and its global loot modifiers
	 *    out of `data/neoforge/loot_modifiers/…`. Left under `forge/` they are simply never read —
	 *    silently, with no log line — so the biome/structure modifiers (i.e. all mob spawning) and
	 *    the four global loot modifiers do nothing.
	 *  - Its loot condition is registered as `neoforge:loot_table_id`.
	 *  - NeoForge 1.20.5 moved the cross-mod convention tags from `forge:` to the loader-neutral
	 *    `c:` namespace, renaming a handful of them on the way.
	 *
	 * @param conventionTags whether this node wants the `c:` tag namespace (NeoForge >= 1.20.5).
	 * @param indexedLootModifiers whether this node still reads the `global_loot_modifiers.json`
	 *   index (NeoForge < 26). NeoForge 26's `LootModifierManager` dropped it: it is a plain
	 *   `SimpleJsonResourceReloadListener` over every json under `loot_modifiers` in every namespace,
	 *   decoded with `IGlobalLootModifier.DIRECT_CODEC`. So the index file is itself scanned as a
	 *   modifier and fails to parse ("No key type in MapLike[…]"), and shipping it does nothing but
	 *   log that error — the four modifiers are picked up directly, ordered by their `priority`
	 *   field (optional, default 1000). Forge 26 still reads the index, so this is NeoForge-only.
	 */
	fun migrateNeoForge(resourcesRoot: File, conventionTags: Boolean, indexedLootModifiers: Boolean = true): Int {
		val data = resourcesRoot.resolve("data")
		if (!data.isDirectory) return 0
		var changed = 0

		if (!indexedLootModifiers) {
			val index = data.resolve("forge/loot_modifiers/global_loot_modifiers.json")
			if (index.isFile && index.delete()) {
				changed++
				index.parentFile.takeIf { it.listFiles().isNullOrEmpty() }?.delete()
			}
		}

		// data/forge/tags -> data/c/tags, everything else under data/forge -> data/neoforge.
		// Before 1.20.5 the convention tags still lived in `forge:`, so they stay put there.
		data.resolve("forge").listFiles().orEmpty().forEach { dir ->
			val target = when {
				dir.name != "tags" -> "neoforge"
				conventionTags -> "c"
				else -> return@forEach
			}
			changed += relocate(dir, data.resolve(target).resolve(dir.name))
		}
		data.resolve("forge").takeIf { it.isDirectory && it.listFiles().isNullOrEmpty() }?.delete()

		// data/<namespace>/forge/<registry> -> data/<namespace>/neoforge/<registry>
		data.listFiles().orEmpty().filter { it.isDirectory && it.name != "forge" }.forEach { namespace ->
			val forgeDir = namespace.resolve("forge")
			if (!forgeDir.isDirectory) return@forEach
			forgeDir.listFiles().orEmpty().forEach { registry ->
				changed += relocate(registry, namespace.resolve("neoforge").resolve(registry.name))
			}
			forgeDir.delete()
		}

		data.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { file ->
			val original = file.readText()
			var text = original.replace("forge:loot_table_id", "neoforge:loot_table_id")
			if (conventionTags) {
				text = forgeNamespace.replace(text) { "c:" + (renamedTags[it.groupValues[1]] ?: it.groupValues[1]) }
			}
			if (text != original) {
				file.writeText(text)
				changed++
			}
		}
		return changed
	}

	/** Matches a `forge:` resource location but not the `neoforge:` one it is a suffix of. */
	private val forgeNamespace = Regex("""(?<![A-Za-z0-9_])forge:([a-z0-9_./-]+)""")

	/**
	 * Convention tags this mod references that did not keep their path when they moved to `c:`.
	 * Anything absent from this map keeps its path (`ores`, `seeds`, `eggs`, `ingots/iron`, …), as do
	 * the tags this mod defines itself (`heart`, `armors/boots`, `crops/rice`, …) — those are only ever
	 * read back by this mod, so they just follow the definition into `c:`.
	 */
	private val renamedTags = mapOf(
		"sand" to "sands",
		"string" to "strings",
		"glass" to "glass_blocks",
		"gravel" to "gravels",
		"is_dense/overworld" to "is_dense_vegetation/overworld",
		"is_coniferous" to "is_tree/coniferous",
	)

	/**
	 * **Forge 26 followed NeoForge into the `c:` namespace.** Its `Tags` class is almost entirely
	 * `cTag(...)` now — only a handful of genuinely Forge-specific entries (`enderman_place_on_blacklist`,
	 * `needs_wood_tool`, …) are still `forgeTag(...)` — and the tag names match NeoForge's exactly,
	 * renames included (`sands`, `strings`, `glass_blocks`, `gravels`, `is_dense_vegetation/overworld`,
	 * `is_tree/coniferous`, `dyes/green` via `DyeColor#getTag`). So a Forge >= 26 node needs the same
	 * convention-tag pass a NeoForge >= 1.20.5 node gets — and ONLY that half: `forge:loot_table_id`,
	 * `data/forge/loot_modifiers/global_loot_modifiers.json` and `data/<ns>/forge/<registry>` are all
	 * still read under `forge:` there, which is why this cannot just call [migrateNeoForge].
	 *
	 * Without it every `#forge:` reference silently resolves to nothing. Found by the boot gate on
	 * `26.1.2-forge` (Milestone 13): 25 `Couldn't load tag` lines (`forge:ores`, `forge:seeds`,
	 * `forge:sand`, `forge:is_sandy`, …, cascading into every `*_spawns` tag) and 11
	 * `Couldn't parse data file` lines (`Missing tag: 'forge:rods/wooden'`), i.e. most of the mod's
	 * spawning and a tenth of its recipes, gone. Compiles and boots clean either way.
	 */
	fun migrateConventionTags(resourcesRoot: File): Int {
		val data = resourcesRoot.resolve("data")
		if (!data.isDirectory) return 0
		var changed = relocate(data.resolve("forge/tags"), data.resolve("c/tags"))
		data.resolve("forge").takeIf { it.isDirectory && it.listFiles().isNullOrEmpty() }?.delete()

		data.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { file ->
			val original = file.readText()
			val text = forgeNamespace.replace(original) { match ->
				val path = match.groupValues[1]
				// Only the convention TAGS moved — Forge's own loot condition id is unchanged.
				if (path == "loot_table_id") match.value else "c:" + (renamedTags[path] ?: path)
			}
			if (text != original) {
				file.writeText(text)
				changed++
			}
		}
		return changed
	}

	/**
	 * **`c:` is a namespace, not a library — on Fabric NOBODY is obliged to define the tag you read.**
	 *
	 * On Forge and NeoForge the loader itself ships every `Tags` entry, so a `#forge:`/`#c:` reference
	 * always resolves. On Fabric the convention tags come from an *optional* Fabric API module, and which
	 * ones exist depends on the fabric-api build: v1 (the only module below 1.20.6) defines 156 `c:` tags
	 * and none of the seven below; v2 grew the rest in over a year of releases, so `c:is_sandy` first
	 * appears somewhere between the 1.21 and 1.21.1 pins. Referencing one that does not exist is not a
	 * crash — it is a logged `Couldn't load tag`, and the referencing tag loads EMPTY, which then cascades
	 * (`c:sands` alone takes out `alexsmobs:am_spawns` and the fifteen `*_spawns` tags built on it).
	 * The boot gate caught it on `1.20.1-fabric`; `scripts/verify_convention_tags.py` diffs every Fabric
	 * node's references against its pinned fabric-api jar so the next one is caught before a run.
	 *
	 * So the mod defines them itself, on **every** Fabric node — not just the ones whose pinned fabric-api
	 * is missing them. A shipped jar meets whatever fabric-api the player installed, which may be older
	 * than the pin, and tag JSONs *merge*: where the module already defines the tag the two are unioned,
	 * and the values here are copied from fabric-api's own v2 definitions (flattened past the `#c:sands/…`
	 * sub-tag indirection, which is itself version-dependent), so the union is the module's own set.
	 *
	 * Written in the PLURAL folders and before [migrateTo121], so the singular rename picks them up.
	 */
	fun backfillFabricConventionTags(resourcesRoot: File): Int {
		val tags = resourcesRoot.resolve("data/c/tags")
		var written = 0
		fabricConventionBackfill.forEach { (path, values) ->
			val file = tags.resolve("$path.json")
			if (file.exists()) return@forEach
			file.parentFile.mkdirs()
			file.writeText(values.joinToString(
				separator = ",\n    ",
				prefix = "{\n  \"values\": [\n    ",
				postfix = "\n  ]\n}\n",
			) { "\"$it\"" })
			written++
		}
		return written
	}

	/** Tag path (under `data/c/tags/`) to the vanilla ids fabric-api's own v2 module puts in it. */
	private val fabricConventionBackfill = mapOf(
		"blocks/sands" to listOf("minecraft:sand", "minecraft:red_sand"),
		"blocks/gravels" to listOf("minecraft:gravel"),
		"items/seeds" to listOf(
			"minecraft:wheat_seeds", "minecraft:beetroot_seeds", "minecraft:melon_seeds",
			"minecraft:pumpkin_seeds", "minecraft:torchflower_seeds", "minecraft:pitcher_pod",
		),
		"items/crops/carrot" to listOf("minecraft:carrot"),
		"worldgen/biome/is_sandy" to listOf(
			"minecraft:desert", "minecraft:badlands", "minecraft:wooded_badlands",
			"minecraft:eroded_badlands", "minecraft:beach",
		),
		"worldgen/biome/is_swamp" to listOf("minecraft:swamp", "minecraft:mangrove_swamp"),
		"worldgen/biome/is_snowy" to listOf(
			"minecraft:snowy_beach", "minecraft:snowy_plains", "minecraft:ice_spikes",
			"minecraft:snowy_taiga", "minecraft:grove", "minecraft:snowy_slopes",
			"minecraft:jagged_peaks", "minecraft:frozen_peaks",
		),
		// ── The eleven the mob-spawn defaults read (#85) ───────────────────────────────────
		// These are NOT referenced by any data-pack file, so verify_convention_tags.py could
		// never see them: DefaultBiomes names them as plain Java strings and SpawnBiomeEntry
		// #matches compares them against the tags each biome CARRIES. That is why they were
		// missed when the seven above were added — and why four of the six nodes whose pinned
		// fabric-api is missing some of them still spawned everything else fine.
		//
		// Flattened to concrete vanilla ids on purpose: the writer emits plain strings, i.e.
		// REQUIRED entries, and a required entry naming something that does not exist makes the
		// whole tag fail to load. So fabric-api's optional `#c:climate_*`/`#c:vegetation_dense`
		// aliases are dropped (where fabric-api defines the tag too, its own copy still carries
		// them and the two merge), and `minecraft:pale_garden` is dropped from is_rare and
		// is_dense_vegetation/overworld — it does not exist below 1.21.4, and every node that
		// has it also has a fabric-api that already lists it.
		"worldgen/biome/is_hot/overworld" to listOf(
			"minecraft:jungle", "minecraft:bamboo_jungle", "minecraft:sparse_jungle",
			"minecraft:desert", "minecraft:badlands", "minecraft:wooded_badlands",
			"minecraft:eroded_badlands", "minecraft:savanna", "minecraft:savanna_plateau",
			"minecraft:windswept_savanna", "minecraft:stony_peaks", "minecraft:mushroom_fields",
			"minecraft:warm_ocean",
		),
		"worldgen/biome/is_cold/overworld" to listOf(
			"minecraft:taiga", "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga",
			"minecraft:windswept_hills", "minecraft:windswept_gravelly_hills", "minecraft:windswept_forest",
			"minecraft:snowy_plains", "minecraft:ice_spikes", "minecraft:grove",
			"minecraft:snowy_slopes", "minecraft:jagged_peaks", "minecraft:frozen_peaks",
			"minecraft:stony_shore", "minecraft:snowy_beach", "minecraft:snowy_taiga",
			"minecraft:frozen_river", "minecraft:cold_ocean", "minecraft:frozen_ocean",
			"minecraft:deep_cold_ocean", "minecraft:deep_frozen_ocean",
		),
		// fabric-api's is_cold is the union of its three dimension sub-tags; is_cold/nether is
		// empty upstream, so it is simply absent here.
		"worldgen/biome/is_cold" to listOf(
			"#c:is_cold/overworld",
			"minecraft:the_end", "minecraft:small_end_islands", "minecraft:end_midlands",
			"minecraft:end_highlands", "minecraft:end_barrens",
		),
		"worldgen/biome/is_plains" to listOf("minecraft:plains", "minecraft:sunflower_plains"),
		"worldgen/biome/is_mushroom" to listOf("minecraft:mushroom_fields"),
		"worldgen/biome/is_dry/overworld" to listOf(
			"minecraft:desert", "minecraft:badlands", "minecraft:wooded_badlands",
			"minecraft:eroded_badlands", "minecraft:savanna", "minecraft:savanna_plateau",
			"minecraft:windswept_savanna",
		),
		"worldgen/biome/is_rare" to listOf(
			"minecraft:sunflower_plains", "minecraft:flower_forest", "minecraft:old_growth_birch_forest",
			"minecraft:old_growth_spruce_taiga", "minecraft:bamboo_jungle", "minecraft:sparse_jungle",
			"minecraft:eroded_badlands", "minecraft:savanna_plateau", "minecraft:windswept_savanna",
			"minecraft:ice_spikes", "minecraft:windswept_gravelly_hills", "minecraft:mushroom_fields",
			"minecraft:deep_dark",
		),
		"worldgen/biome/is_dense_vegetation/overworld" to listOf(
			"minecraft:dark_forest", "minecraft:old_growth_birch_forest",
			"minecraft:old_growth_spruce_taiga", "minecraft:jungle", "minecraft:bamboo_jungle",
			"minecraft:mangrove_swamp",
		),
		"worldgen/biome/is_plateau" to listOf(
			"minecraft:wooded_badlands", "minecraft:savanna_plateau", "minecraft:cherry_grove",
			"minecraft:meadow",
		),
		// #minecraft:is_taiga is vanilla's own and exists on every node, so it can stay a
		// reference rather than being flattened.
		"worldgen/biome/is_tree/coniferous" to listOf("#minecraft:is_taiga", "minecraft:grove"),
		// The ONE entry here copied from FORGE's definition rather than fabric-api's, because
		// fabric-api defines `c:is_wasteland` as literally `{"values": []}` on every version while
		// Forge's `forge:is_wasteland` is `[minecraft:snowy_plains]` (both read out of the jars).
		// It is referenced once — the moose's first spawn pool, is_overworld AND is_snowy AND
		// is_wasteland — so an empty tag silently costs the moose its snowy-plains half; the second
		// pool (… AND #minecraft:is_taiga) is why nobody reported moose missing outright.
		// Out of scope, noted here so it is not re-derived: NeoForge >=1.20.6 aliases `c:is_wasteland`
		// to an *optional* `#forge:is_wasteland` it no longer ships, and NeoForge 26.1 drops even
		// that — so those nodes have the same empty tag, loader-side, and this Fabric-only backfill
		// does not reach them.
		"worldgen/biome/is_wasteland" to listOf("minecraft:snowy_plains"),
		// Read negated ("do not spawn here"), so an undefined tag fails OPEN — this one is not
		// about mobs failing to spawn but about them spawning where upstream excludes them.
		"worldgen/biome/no_default_monsters" to listOf("minecraft:mushroom_fields", "minecraft:deep_dark"),
	)

	/** Moves [from] onto [to], merging into an existing directory. Returns the number of files moved. */
	private fun relocate(from: File, to: File): Int {
		if (!from.exists()) return 0
		var moved = 0
		from.walkTopDown().filter { it.isFile }.toList().forEach { file ->
			val destination = to.resolve(file.toRelativeString(from))
			destination.parentFile.mkdirs()
			file.copyTo(destination, overwrite = true)
			file.delete()
			moved++
		}
		from.walkBottomUp().filter { it.isDirectory }.forEach { it.delete() }
		return moved
	}
}

/**
 * Just enough SNBT to convert the string-form tags in this mod's data pack into JSON. Type suffixes
 * are dropped: every numeric tag here is read back through `CompoundTag#getInt`/`getString`, and
 * NBT's numeric tags are mutually convertible, so the distinction does not survive anyway.
 */
private object Snbt {
	fun parse(text: String): JsonElement = Reader(text).let {
		val value = it.value()
		it.skipWhitespace()
		require(it.atEnd()) { "Trailing input in SNBT: $text" }
		value
	}

	private class Reader(private val text: String) {
		private var pos = 0

		fun atEnd() = pos >= text.length

		fun skipWhitespace() {
			while (!atEnd() && text[pos].isWhitespace()) pos++
		}

		fun value(): JsonElement {
			skipWhitespace()
			return when (text[pos]) {
				'{' -> compound()
				'[' -> list()
				'"', '\'' -> JsonPrimitive(quoted())
				else -> scalar()
			}
		}

		private fun compound(): JsonObject {
			expect('{')
			val entries = LinkedHashMap<String, JsonElement>()
			skipWhitespace()
			while (text[pos] != '}') {
				skipWhitespace()
				val key = if (text[pos] == '"' || text[pos] == '\'') quoted() else unquoted()
				skipWhitespace()
				expect(':')
				entries[key] = value()
				skipWhitespace()
				if (text[pos] == ',') pos++ else break
				skipWhitespace()
			}
			expect('}')
			return JsonObject(entries)
		}

		private fun list(): JsonArray {
			expect('[')
			// Typed array prefixes (B;, I;, L;) carry no meaning once this is JSON.
			if (pos + 1 < text.length && text[pos + 1] == ';') pos += 2
			val items = mutableListOf<JsonElement>()
			skipWhitespace()
			while (text[pos] != ']') {
				items += value()
				skipWhitespace()
				if (text[pos] == ',') pos++ else break
				skipWhitespace()
			}
			expect(']')
			return JsonArray(items)
		}

		private fun quoted(): String {
			val quote = text[pos++]
			val out = StringBuilder()
			while (text[pos] != quote) {
				if (text[pos] == '\\') pos++
				out.append(text[pos++])
			}
			pos++
			return out.toString()
		}

		private fun unquoted(): String {
			val start = pos
			while (!atEnd() && (text[pos].isLetterOrDigit() || text[pos] in "_-.+")) pos++
			require(pos > start) { "Empty SNBT token at $pos in $text" }
			return text.substring(start, pos)
		}

		private fun scalar(): JsonElement {
			val token = unquoted()
			return when {
				token == "true" -> JsonPrimitive(true)
				token == "false" -> JsonPrimitive(false)
				else -> {
					val number = token.dropLastWhile { it in "bBsSlLfFdD" }
					number.toLongOrNull()?.let { return JsonPrimitive(it) }
					number.toDoubleOrNull()?.let { return JsonPrimitive(it) }
					JsonPrimitive(token)
				}
			}
		}

		private fun expect(char: Char) {
			skipWhitespace()
			require(!atEnd() && text[pos] == char) { "Expected '$char' at $pos in $text" }
			pos++
		}
	}
}

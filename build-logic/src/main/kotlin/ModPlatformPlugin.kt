@file:Suppress("unused", "DuplicatedCode")

import dev.kikugie.fletching_table.extension.FletchingTableExtension
import dev.kikugie.stonecutter.StonecutterExperimentalAPI
import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.extensions.stdlib.toDefaultLowerCase
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.plugins.ide.idea.model.IdeaModel
import javax.inject.Inject

val Project.sc: StonecutterBuildExtension
	get() = extensions.getByType<StonecutterBuildExtension>()

@OptIn(StonecutterExperimentalAPI::class)
fun Project.prop(name: String): String = (project.sc.properties.get<String>(name))

/**
 * For pins that only some nodes have. Stonecutter throws rather than returning null for an absent
 * key, which is the right default — a missing `deps.forge` should fail loudly — so opting out is
 * per-call-site.
 */
@OptIn(StonecutterExperimentalAPI::class)
fun Project.propOrNull(name: String): String? =
	runCatching { project.sc.properties.get<String>(name) }.getOrNull()

/**
 * The exact-one-version Maven range to declare for `minecraft`, e.g. `[1.20.1]` / `[1.21.0]`.
 *
 * Two things conspire here:
 *
 *  1. A **bare** version (`"1.21"`) is a Maven *soft* requirement, which Forge/NeoForge read as
 *     `[1.21,)` — the jar then claims to run on every later MC. Hence the brackets.
 *  2. Modrinth's upload auto-detect (`apps/frontend/src/helpers/infer/version-ranges.ts`) turns
 *     `[X]` into the **semver range** `X` and feeds it to node-semver's `satisfies`. A
 *     two-component range like `1.21` is a semver *X-range* meaning `1.21.x`, so it preselects
 *     1.21 **and** 1.21.1 … 1.21.11. Padding to three components (`[1.21.0]`) makes it an exact
 *     semver version that matches only MC 1.21.
 *
 * Padding is safe for the loaders: Maven's `ComparableVersion` normalises trailing zero
 * components, so `1.21` and `1.21.0` compare **equal** and `[1.21.0]` is satisfied by MC 1.21.
 */
fun exactMcRange(mc: String): String {
	val padded = when (mc.count { it == '.' }) {
		0 -> "$mc.0.0"
		1 -> "$mc.0"
		else -> mc
	}
	return "[$padded]"
}

/**
 * The MC range a node's manifest declares. **Exact by default** (see [exactMcRange]); a node may
 * widen it with an optional `deps.minecraft-range` key in its `stonecutter.properties.toml` section.
 *
 * One node per MC version is the tree's normal shape, but MC sometimes ships patch releases that
 * are API-identical, and then a single node genuinely serves several. `26.1.2` is the first: it
 * runs 26.1, 26.1.1 and 26.1.2.
 *
 * ⚠️ **This is the authority, not the store listing.** A store can advertise a file against any MC
 * version it likes, but the loader reads *this* range out of the jar — so tagging a file 26.1 on
 * Modrinth/CurseForge without widening the range here ships a jar the launcher installs and the
 * loader then refuses, which is the same failure the `fabric-loader` floor bug produced. Widen
 * here **first**, then tag the store to match.
 *
 * The value is written in the syntax of the section's own loader family, because the two are not
 * interchangeable: Forge/NeoForge want a Maven range (`[26.1, 26.1.3)`) and Fabric wants semver
 * (`>=26.1 <=26.1.2`). Sections are already per-loader, so one key name covers both.
 */
fun Project.declaredMcRange(fabricLike: Boolean): String =
	propOrNull("deps.minecraft-range")
		?: if (fabricLike) prop("deps.minecraft") else exactMcRange(prop("deps.minecraft"))

fun Project.env(variable: String): String? = providers.environmentVariable(variable).orNull

fun Project.envTrue(variable: String): Boolean = env(variable)?.toDefaultLowerCase() == "true"

fun RepositoryHandler.strictMaven(
	url: String, vararg groups: String, configure: MavenArtifactRepository.() -> Unit = {}
) = exclusiveContent {
	forRepository { maven(url) { configure() } }
	filter { groups.forEach(::includeGroup) }
}

abstract class GenerateModManifestTask : DefaultTask() {
	@get:Input
	abstract val content: Property<String>

	@get:OutputFile
	abstract val outputFile: RegularFileProperty

	@TaskAction
	fun generate() {
		val file = outputFile.get().asFile
		file.parentFile.mkdirs()
		file.writeText(content.get())
	}
}

abstract class ModPlatformPlugin @Inject constructor() : Plugin<Project> {
	override fun apply(project: Project) = with(project) {
		val inferredLoader = Loader.of(project.buildFile.name.substringAfter('.').replace(".gradle.kts", ""))

		val extension = extensions.create("platform", ModPlatformExtension::class.java).apply {
			loader.convention(inferredLoader.id)
		}

		when (inferredLoader) {
			is Loader.Fabric, is Loader.Forge -> {
				// arch-loom builds both; remapped output is remapJar/remapSourcesJar. On the
				// NO-REMAP variant (unobfuscated 26.x) those tasks don't exist → fall back to
				// jar/sourcesJar. Resolved lazily so the Loom tasks are created by query time.
				extension.jarTask.convention(providers.provider {
					if (tasks.findByName("remapJar") != null) "remapJar" else "jar"
				})
				extension.sourcesJarTask.convention(providers.provider {
					if (tasks.findByName("remapSourcesJar") != null) "remapSourcesJar" else "sourcesJar"
				})
			}
			else -> {
				extension.jarTask.convention("jar")
				extension.sourcesJarTask.convention("sourcesJar")
			}
		}

		listOf("org.jetbrains.kotlin.jvm", "com.google.devtools.ksp", "dev.kikugie.fletching-table").forEach {
			apply(
				plugin = it
			)
		}

		afterEvaluate {
			val ctx = Context(
				project = this,
				extension = extension,
				loader = Loader.of(extension.loader.get()),
				stonecutter = project.sc
			)
			configureProject(ctx)
		}
	}

	private fun Project.configureProject(ctx: Context) {
		listOf("java", "me.modmuss50.mod-publish-plugin", "idea").forEach { apply(plugin = it) }

		version = ctx.fullVersion
		ctx.extension.requiredJava.set(ctx.javaVersion)

		if (ctx.loader.isFabricLike) {
			ctx.extension.dependencies {
				required("java") { fabricLikeVersionRange = ">=${ctx.javaVersion.majorVersion}" }
			}
		}

		configureFletchingTable(ctx)
		registerGenerateManifestTask(ctx)
		configureJarTask(ctx)
		configureIdea()
		configureProcessResources(ctx)
		configureJava(ctx)
		registerBuildAndCollectTask(ctx)

		configureModPublishing(ctx)

		if (envTrue("PUB_MAVEN_ENABLE")) {
			configureMavenPublishing(ctx)
		}
	}

	private fun Project.configureJava(ctx: Context) {
		extensions.configure<JavaPluginExtension>("java") {
			withSourcesJar()
			withJavadocJar()
			// Select a real per-node toolchain (17/21/25) so javac runs on the
			// matching JDK — Gradle auto-provisions (foojay) or uses a system JDK —
			// instead of the daemon JVM. Fixes "invalid source release: 25".
			toolchain {
				languageVersion.set(
					org.gradle.jvm.toolchain.JavaLanguageVersion.of(ctx.javaVersion.majorVersion.toInt())
				)
			}
		}

		// JEI does not exist for every MC version this tree covers — it skipped 1.21.2 and 1.21.3
		// entirely, and stopped publishing a Forge flavour after 1.21.1. compat/jei/** is only ever
		// reached through JEI's own @JeiPlugin classpath scan (nothing in the mod references it), so
		// on a node with no `deps.jei` pin it is simply left out of the compile.
		if (propOrNull("deps.jei") == null) {
			extensions.getByType<org.gradle.api.tasks.SourceSetContainer>()
				.named("main").get().java.exclude("**/compat/jei/**")
			logger.lifecycle("No JEI for this version — compat/jei is excluded from the build")
		}

		// The Fabric-only half of the mod: entrypoints and everything that speaks the Fabric API.
		// It lives inside the shared source tree rather than in its own Gradle source set because
		// Stonecutter projects root src/ wholesale — a second source set would need its own
		// projection. Excluding by path is equivalent and costs nothing.
		//
		// Note this is a LOADER gate, not a version one: net.fabricmc.** is simply absent from a
		// Forge/NeoForge node's classpath, so leaving it in is a compile error, not dead code.
		if (ctx.loader !is Loader.Fabric) {
			extensions.getByType<org.gradle.api.tasks.SourceSetContainer>()
				.named("main").get().java.exclude(
					"**/alexsmobs/fabric/**",
					// The other half of the same story, in the one place it could not live under
					// alexsmobs/fabric/: a mixin's package must sit under the config's declared
					// `package` (com.github.alexthe666.alexsmobs.mixin) or Mixin cannot resolve it.
					// mixin/fabric/** exists purely because Fabric has no event bus — Forge and
					// NeoForge fire every one of those hooks from @SubscribeEvent — so on those two
					// loaders the classes are not merely redundant, they would fire ServerEvents a
					// second time. Excluded here and pruned back out of the mixin config in
					// processResources, because Fletching Table's @Mixin scan ignores this exclude
					// (see DataPackMigration.pruneMixinPackage).
					"**/mixin/fabric/**",
				)
		}

		// Both of the big event files now compile unchanged on Fabric — ServerEvents since wave 1,
		// ClientEvents since wave 3 — by the same route: the net.minecraftforge.**.event types they
		// name are stubbed under fabric/forge/ and the !fab-fe-* replacement rules point the files'
		// imports at them. Keeping them byte-identical across all three loaders is the whole point;
		// adding a third arm to every loader gate inside them would have made the two working
		// loaders unreadable.
		//
		// Compiling is not reacting. The stubs are inert until something constructs them — see
		// fabric/event/FabricServerEvents and fabric/client/FabricClientEvents — and what is not
		// wired there yet is listed as a divergence in docs/notes/fabric.md, not hidden here.
		//
		// What remains excluded below is excluded for reasons that a gate cannot express.
		if (ctx.loader is Loader.Fabric) {
			extensions.getByType<org.gradle.api.tasks.SourceSetContainer>()
				.named("main").get().java.exclude(
					// Same remedy, different reason: onAddLayers' body already holds seven
					// Stonecutter blocks (six of them arms for enumerating player skins across four
					// MC eras), and blocks never nest, so it cannot be wrapped in !fabric. Fabric
					// API's LivingEntityRenderLayerRegistrationCallback covers the player renderer
					// too, so fabric/client/FabricClientLayers replaces the file with one lambda.
					"**/client/ClientLayerRegistry.java",
					// The lava-vision fluid renderer is Forge-family through and through: it reads
					// the fluid's sprites and tint colour through IClientFluidTypeExtensions /
					// FluidSpriteCache and calls BlockState#shouldDisplayFluidOverlay, none of which
					// Fabric has. It is already excluded on >=26 for an unrelated reason (its
					// supertype was deleted), and its ONLY call site is the renderer swap in
					// ClientEvents, whose renderer-swap block is gated `<26 && !fabric` for exactly
					// this reason, so on Fabric it is unreachable either way.
					// Restoring it means a Fabric fluid-render mixin, tracked in docs/notes/fabric.md.
					"**/client/render/LavaVisionFluidRenderer.java",
				)
			logger.lifecycle("Fabric: ClientLayerRegistry + LavaVisionFluidRenderer are excluded; everything else compiles")
		}

		// client/render/compat/** re-implements the pre-1.21.2 entity-renderer API on top of the
		// render-state architecture 1.21.2 introduced. It extends classes that do not exist below
		// 1.21.2 (EntityRenderState & friends), so it cannot compile there — and nothing below
		// 1.21.2 references it, because the Stonecutter import rules that point the ~123 renderers
		// at it are themselves gated on >=1.21.2.
		// mixin/renderstate/** is the same story: it mixes into EntityRenderState, which does not
		// exist below 1.21.2. Excluding it here keeps the .class files out of the jar, but Fletching
		// Table still declares them in alexsmobs.mixins.json (its @Mixin scan does not honour this
		// exclude), and a config naming an absent class is a hard load failure — so they are pruned
		// back out in processResources (see DataPackMigration.pruneRenderStateMixins).
		if (!ctx.stonecutter.eval(ctx.currentMcVersion, ">=1.21.2")) {
			extensions.getByType<org.gradle.api.tasks.SourceSetContainer>()
				.named("main").get().java.exclude("**/client/render/compat/**", "**/mixin/renderstate/**")
		}

		// GuiRenderer and the whole picture-in-picture package arrived with the deferred GUI in
		// 1.21.6. The mixin that pools its shared entity renderer, and the pool it calls, name
		// those classes directly, so neither can compile below that — and as with the render-state
		// pair above the mixin has to be pruned back out of the config as well (the pool is not a
		// mixin, so it just goes). The atlas mixin joins them for the same era reason rather than a
		// naming one: the GUI item atlas it keeps our icons out of (#107) arrived in 1.21.6 too.
		if (!ctx.stonecutter.eval(ctx.currentMcVersion, ">=1.21.6")) {
			extensions.getByType<org.gradle.api.tasks.SourceSetContainer>()
				.named("main").get().java.exclude(
					"**/client/render/AMGuiEntityPipPool.java",
					"**/mixin/client/GuiRendererMixin.java",
					"**/mixin/client/ItemStackRenderStateAtlasMixin.java",
				)
		}

		// The mirror image: a mixin that exists ONLY on 26.x. 26.1 dropped the ItemDisplayContext
		// parameter from SpecialModelRenderer#submit, so the icon renderer lost the one thing the
		// shattered dimensional carver's eleven shards need to pick their in-hand pose (#96). The
		// context is still on ItemStackRenderState, one frame up the stack, and this mixin lends it
		// to the renderer. Named directly, so it cannot compile below 26 — excluded and pruned.
		if (!ctx.stonecutter.eval(ctx.currentMcVersion, ">=26")) {
			extensions.getByType<org.gradle.api.tasks.SourceSetContainer>()
				.named("main").get().java.exclude(
					"**/mixin/client/ItemStackRenderStateMixin.java",
				)
		}

		// Four client classes whose SUPERTYPES were deleted in MC 26.1, with no successor to port
		// them onto. A file whose whole body is a Stonecutter block is not an option here — each
		// already contains /* */-commented arms from earlier waves, and those cannot nest.
		//
		//   LavaVisionFluidRenderer   extends LiquidBlockRenderer, which became FluidRenderer and
		//                             is constructed per-SectionCompiler — there is no longer a
		//                             renderer instance on the dispatcher to subclass and swap in.
		//   {Tabula,Vanilla,Baked}*   the vanilla-block-model half of the vendored Tabula loader.
		//                             @Deprecated(since = "2.6.2") upstream and entirely unreachable
		//                             here (only loadTabulaModel/getCubeByName/getAllCubes are
		//                             live); its BlockElement / ItemTransform(s) / UnbakedModel
		//                             dependencies are all gone or moved in 26.1.
		//
		// None is a mixin, so unlike the renderstate pair above there is nothing to prune back out
		// of alexsmobs.mixins.json. Their few call sites are gated <26 in source.
		if (ctx.stonecutter.eval(ctx.currentMcVersion, ">=26")) {
			extensions.getByType<org.gradle.api.tasks.SourceSetContainer>()
				.named("main").get().java.exclude(
					"**/client/render/LavaVisionFluidRenderer.java",
					"**/citadel/client/model/container/TabulaModelBlock.java",
					"**/citadel/client/model/container/VanillaTabulaModel.java",
					"**/citadel/client/model/container/BakedTabulaModel.java",
					// ItemBlockRenderTypes is gone in 26.1 — fluid render layers come off
					// FluidStateModelSet now, so there is no host method to inject into. Unlike the
					// four above this IS a mixin, so it is also pruned from the config (below).
					"**/mixin/client/ItemBlockRenderTypesMixin.java",
				)
		}
	}

	private fun Project.registerGenerateManifestTask(ctx: Context) {
		val manifestOutputDir = layout.buildDirectory.dir("generated/modManifest")
		val generateTask = tasks.register<GenerateModManifestTask>("generateModManifest") {
			content.set(ctx.loader.generateManifest(ctx))
			outputFile.set(layout.buildDirectory.file("generated/modManifest/${ctx.loader.manifestPathFor(ctx)}"))
		}

		the<JavaPluginExtension>().sourceSets.named("main") { resources.srcDir(manifestOutputDir) }
		tasks.named<ProcessResources>("processResources") { dependsOn(generateTask) }
	}

	// Data-pack format per MC version (from each version.json `pack_version.data`).
	private fun packFormatFor(mc: String): Int = when (mc) {
		"1.20.1" -> 15
		"1.20.2" -> 18
		"1.20.3", "1.20.4" -> 26
		"1.20.5", "1.20.6" -> 41
		"1.21", "1.21.1" -> 48
		"1.21.2", "1.21.3" -> 57
		"1.21.4" -> 61
		"1.21.5" -> 71
		"1.21.6" -> 80
		"1.21.7", "1.21.8" -> 81
		"1.21.9", "1.21.10" -> 88
		"1.21.11" -> 94
		"26.1", "26.1.1", "26.1.2" -> 101
		"26.2" -> 107
		else -> 48
	}
	private fun packMinorFor(mc: String): Int = when (mc) {
		"1.21.11", "26.1", "26.1.1", "26.1.2", "26.2" -> 1
		else -> 0
	}
	// mcmeta schema changed at 1.21.9 (data-format 88): <=1.21.8 needs a `pack_format` int and
	// rejects min_format/max_format; >=1.21.9 requires min_format/max_format as [major, minor].
	private fun packMetaFieldsFor(mc: String): String {
		val f = packFormatFor(mc)
		val m = packMinorFor(mc)
		return if (f <= 81) "\"pack_format\": $f"
		else "\"pack_format\": $f, \"min_format\": [$f, $m], \"max_format\": [$f, $m]"
	}

	private fun Project.configureProcessResources(ctx: Context) {
		tasks.named<ProcessResources>("processResources") {
			dependsOn(tasks.named("stonecutterGenerate"), "kspKotlin")
			// codxlib currently ships no mixins ("mixins": []). Forge bundles an older Mixin
			// library than Fabric/NeoForge for the same MC (e.g. Forge 50.x on 1.20.6 does not
			// recognise JAVA_21), so pin Forge's mixin compatibilityLevel to JAVA_17 — a level
			// every bundled Mixin across 1.20.1–26.2 accepts. Harmless while the mixin list is
			// empty; revisit if real mixins compiled to newer bytecode are ever added.
			val mixinJava = if (ctx.loader is Loader.Forge) "JAVA_17" else "JAVA_${ctx.javaVersion.majorVersion}"
			filesMatching("*.mixins.json") {
				expand("java" to mixinJava)
			}
			// pack.mcmeta must carry a per-version pack_format int (MC < 1.21.11 requires it and
			// rejects the min_format/max_format-only schema). Forge keeps pack.mcmeta in the jar,
			// so stamp the right value from ${pack_format}.
			filesMatching(listOf("pack.mcmeta", "**/pack.mcmeta")) {
				expand("pack_meta" to packMetaFieldsFor(ctx.currentMcVersion))
			}
			exclude(ctx.loader.excludedResourcesFor(ctx))

			// Fletching Table fills the mixin config's `mixins` array from an @Mixin source scan that
			// ignores the source-set exclude, so it lists mixin/renderstate/** even on the nodes that
			// cannot compile it. A config naming an absent class is a hard load failure — prune them.
			if (!ctx.stonecutter.eval(ctx.currentMcVersion, ">=1.21.2")) doLast {
				val pruned = DataPackMigration.pruneRenderStateMixins(destinationDir, ctx.modId)
				logger.lifecycle("Pruned $pruned render-state mixins from ${ctx.modId}.mixins.json")
			}

			// Same story on >=26: ItemBlockRenderTypes was deleted outright (fluid render layers are
			// data-driven off FluidStateModelSet now), so its mixin is excluded from the compile and
			// must not stay declared.
			if (ctx.stonecutter.eval(ctx.currentMcVersion, ">=26")) doLast {
				val pruned = DataPackMigration.pruneMixinEntries(
					destinationDir, ctx.modId, listOf("client.ItemBlockRenderTypesMixin"))
				logger.lifecycle("Pruned $pruned MC-26-absent mixins from ${ctx.modId}.mixins.json")
			}

			// And once more below 1.21.6, where GuiRenderer — the deferred GUI renderer the entity
			// picture-in-picture pool mixes into — does not exist yet.
			if (!ctx.stonecutter.eval(ctx.currentMcVersion, ">=1.21.6")) doLast {
				val pruned = DataPackMigration.pruneMixinEntries(
					destinationDir, ctx.modId,
					listOf("client.GuiRendererMixin", "client.ItemStackRenderStateAtlasMixin"))
				logger.lifecycle("Pruned $pruned pre-1.21.6 GUI mixins from ${ctx.modId}.mixins.json")
			}

			// And the mirror image below 26, where SpecialModelRenderer#submit still carries the
			// display context itself and the borrow-it mixin has nothing to do (see configureJava).
			if (!ctx.stonecutter.eval(ctx.currentMcVersion, ">=26")) doLast {
				val pruned = DataPackMigration.pruneMixinEntries(
					destinationDir, ctx.modId, listOf("client.ItemStackRenderStateMixin"))
				logger.lifecycle("Pruned $pruned pre-26 item-render-state mixins from ${ctx.modId}.mixins.json")
			}

			// And the same story once more for mixin/fabric/**, which is excluded from the compile on
			// Forge and NeoForge (see configureJava). Prefix-pruned rather than listed class by class:
			// the whole package is Fabric-only by construction, so there is nothing to keep in sync.
			if (ctx.loader !is Loader.Fabric) doLast {
				val pruned = DataPackMigration.pruneMixinPackage(destinationDir, ctx.modId, "fabric.")
				logger.lifecycle("Pruned $pruned Fabric-only mixins from ${ctx.modId}.mixins.json")
			}

			// That same scan also puts CLIENT mixins in the common `mixins` array. On Forge/NeoForge
			// the dist cleaner blocks those classes on a server (that is where this repo's benign
			// `/ERROR]` lines come from), but FABRIC HAS NO DIST CLEANER — a client mixin left in
			// `mixins` is applied on a dedicated server, whose classpath has no client classes at
			// all, and mixin aborts the launch. The `client` array already says "client dist only".
			if (ctx.loader is Loader.Fabric) doLast {
				val moved = DataPackMigration.partitionClientMixins(destinationDir, ctx.modId)
				logger.lifecycle("Moved $moved client-only mixins into ${ctx.modId}.mixins.json's client list")
			}

			// Data-pack shapes that changed with the MC version. Stonecutter cannot do this —
			// it leaves `//?` markers in JSON, which vanilla's strict parser rejects.
			val migrateTo1205 = ctx.stonecutter.eval(ctx.currentMcVersion, ">=1.20.5")
			if (migrateTo1205) doLast {
				val changed = DataPackMigration.migrateTo1205(destinationDir)
				logger.lifecycle("Migrated $changed data-pack files to the 1.20.5 item-stack format")
			}
			// The data pack is authored Forge-side; NeoForge reads its own namespaces.
			if (ctx.loader is Loader.NeoForge) doLast {
				val changed = DataPackMigration.migrateNeoForge(
					destinationDir,
					conventionTags = migrateTo1205,
					indexedLootModifiers = !ctx.stonecutter.eval(ctx.currentMcVersion, ">=26"),
				)
				logger.lifecycle("Re-namespaced $changed data-pack files for NeoForge")
			}
			// Forge 26 moved the convention tags to `c:` as well — the tag half of the pass above,
			// and only that half. See DataPackMigration.migrateConventionTags.
			//
			// Fabric takes the same pass on EVERY node: `c:` IS the Fabric convention namespace and
			// always has been, so a Fabric jar shipping data/forge/tags loads none of them. The other
			// halves of the NeoForge pass are deliberately skipped — biome/structure modifiers and
			// global loot modifiers are Forge-family datapack mechanisms with no Fabric reader at
			// all, so re-namespacing them would buy nothing; both become Java-side work instead.
			val cTags = ctx.loader is Loader.Fabric ||
				(ctx.loader is Loader.Forge && ctx.stonecutter.eval(ctx.currentMcVersion, ">=26"))
			if (cTags) doLast {
				val changed = DataPackMigration.migrateConventionTags(destinationDir)
				logger.lifecycle("Re-namespaced $changed data-pack files into the c: convention tags")
			}
			// …and on Fabric the `c:` tags the mod READS are only as complete as the player's
			// fabric-api, so seven of them have to be defined here. See the long note on
			// DataPackMigration.backfillFabricConventionTags.
			if (ctx.loader is Loader.Fabric) doLast {
				val written = DataPackMigration.backfillFabricConventionTags(destinationDir)
				logger.lifecycle("Backfilled $written c: convention tags Fabric API may not define")
			}
			// Last, so the two passes above still see the folder names they were written against.
			if (ctx.stonecutter.eval(ctx.currentMcVersion, ">=1.21")) doLast {
				val moved = DataPackMigration.migrateTo121(destinationDir)
				logger.lifecycle("Moved $moved data-pack files into the 1.21 singular folders")
				// After the move, so it can look for the singular loot_table/ folder.
				val relooted = DataPackMigration.migrateLootTo121(destinationDir)
				logger.lifecycle("Rewrote looting functions/conditions in $relooted loot tables")
			}
			// 1.21.2 resolves armour textures through an equipment model instead of Forge's
			// deleted getArmorTexture hook; both the model and its texture are derived from
			// the textures/armor/ files the older nodes use directly.
			if (ctx.stonecutter.eval(ctx.currentMcVersion, ">=1.21.2")) doLast {
				val written = DataPackMigration.migrateEquipmentTo12102(destinationDir, ctx.modId)
				logger.lifecycle("Wrote $written equipment models for the 1.21.2 armour textures")
				// 1.21.2 replaced the `{"item": …}` / `{"tag": …}` recipe ingredient objects with a
				// bare string / `#tag` string (Ingredient is a HolderSet<Item> now).
				val reing = DataPackMigration.migrateIngredientsTo1212(destinationDir)
				logger.lifecycle("Rewrote ingredients in $reing recipes to the 1.21.2 string format")
			}
			// 1.21.4 made an item's model indirect: assets/<ns>/items/<id>.json is now what binds an
			// item to a model, and the legacy models/item/<id>.json alone renders nothing. Logged
			// per item and not thrown, so every item in the mod silently became the missing-model
			// cube — see DataPackMigration.writeItemModelDefinitions.
			if (ctx.stonecutter.eval(ctx.currentMcVersion, ">=1.21.4")) doLast {
				val written = DataPackMigration.writeItemModelDefinitions(
					destinationDir,
					ctx.modId,
					// The spawn-egg tints are only spelled at their registration site. Read from the
					// root source set, not the node's projection — it is not version-gated.
					rootProject.file("src/main/java/com/github/alexthe666/alexsmobs/item/AMItemRegistry.java"),
				)
				logger.lifecycle("Wrote $written item model definitions for the 1.21.4 item format")
				// 1.21.4 also deleted the ISTER, but the 59 advancement icons keep their authored
				// custom_data: AMIconSpecialRenderer (registered on every >=1.21.4 node) draws them
				// live again, through the minecraft:special definitions written above. The
				// restaticAdvancementIcons pass that used to repoint them at spawn eggs is gone —
				// re-adding it would freeze the icons AND strip the NBT the live renderer reads.
				// …and moved equipment definitions out of the model tree. An armour item whose
				// asset_id resolves to nothing is not a missing texture, it is no layer at all — the
				// piece renders invisible, silently. Runs after the ≥1.21.2 pass that writes them.
				val requipped = DataPackMigration.relocateEquipmentTo1214(destinationDir, ctx.modId)
				logger.lifecycle("Moved $requipped equipment definitions into the 1.21.4 equipment/ folder")
			}
			// 1.21.5 deleted item/template_spawn_egg and its two greyscale layers, which every one
			// of this mod's 89 spawn-egg models parents to — see DataPackMigration.retemplateSpawnEggs.
			if (ctx.stonecutter.eval(ctx.currentMcVersion, ">=1.21.5")) doLast {
				val retemplated = DataPackMigration.retemplateSpawnEggs(destinationDir, ctx.modId)
				logger.lifecycle("Re-pointed $retemplated spawn-egg models at this mod's own egg layers")
				// 1.21.5 also made an advancement tab's background a bare id rather than the texture
				// file, and the client's expansion of the old value names a file that cannot exist —
				// so the tab drew the missing texture. Expect exactly 1: this mod has one root.
				val rebacked = DataPackMigration.migrateAdvancementBackgroundsTo1215(destinationDir)
				logger.lifecycle("Rewrote $rebacked advancement backgrounds to the 1.21.5 bare-id form")
			}
			// The ghostly pickaxe's transparency is a *render type*, not a texture — and the only
			// thing that ever selects it is a Forge BakedModelWrapper that exists on `<1.21.4 &&
			// !fabric`. Everywhere else the tool draws solid, so the see-through look is baked into
			// the alpha channel instead. Condition mirrors that gate exactly; see
			// DataPackMigration.ghostifyPickaxeTexture.
			if (ctx.stonecutter.eval(ctx.currentMcVersion, ">=1.21.4") || ctx.loader.isFabricLike) doLast {
				val ghosted = DataPackMigration.ghostifyPickaxeTexture(destinationDir, ctx.modId)
				logger.lifecycle(
					if (ghosted) "Lowered the ghostly pickaxe's alpha (no additive render type on this node)"
					else "Ghostly pickaxe texture already translucent — nothing to do"
				)
			}
			// 26.1 split `#minecraft:dirt` three ways, taking the grass block out from under this
			// mod's `am_spawns` ground list. Expect exactly 1 file: only am_spawns names that tag.
			// See DataPackMigration.migrateDirtTagTo261.
			if (ctx.stonecutter.eval(ctx.currentMcVersion, ">=26.1")) doLast {
				val redirt = DataPackMigration.migrateDirtTagTo261(destinationDir)
				logger.lifecycle("Re-joined the 26.1 #minecraft:dirt split in $redirt tag files")
			}
			// 26.2 made EntityPredicate a dispatched map over the sub-predicate registry, so the
			// flat `"type": …` field no longer decodes. Logged and not thrown, i.e. the advancement
			// just vanishes — see DataPackMigration.migrateEntityPredicatesTo262.
			if (ctx.stonecutter.eval(ctx.currentMcVersion, ">=26.2")) doLast {
				val repred = DataPackMigration.migrateEntityPredicatesTo262(destinationDir)
				logger.lifecycle("Rewrote entity predicates in $repred files to the 26.2 dispatched map")
			}
		}
	}

	private fun Project.configureJarTask(ctx: Context) {
		val generateTask = tasks.named("generateModManifest")
		tasks.withType<Jar>().configureEach {
			archiveBaseName.set(ctx.modId)
			dependsOn(generateTask)
			if (ctx.loader is Loader.Forge) {
				manifest.attributes(ctx.loader.mixinConfigAttribute to "${ctx.modId}.mixins.json")
			}
		}
	}

	private fun Project.configureIdea() {
		extensions.configure<IdeaModel>("idea") {
			module {
				isDownloadJavadoc = true
				isDownloadSources = true
			}
		}
	}

	private fun Project.configureFletchingTable(ctx: Context) {
		extensions.configure<FletchingTableExtension> {
			mixins.create("main") { mixin("default", "${ctx.modId}.mixins.json") }
			j52j.register("main") { extension("json", "**/*.json5") }
		}
	}

	private fun Project.registerBuildAndCollectTask(ctx: Context) {
		tasks.register<Copy>("buildAndCollect") {
			from(
				tasks.named(ctx.extension.jarTask.get()),
				tasks.named(ctx.extension.sourcesJarTask.get()),
				tasks.named("javadocJar")
			)
			into(rootProject.layout.buildDirectory.file("libs/${ctx.basicVersion}"))
			dependsOn("build")
			group = "build"
		}
	}
}

// Forge on the UNOBFUSCATED 26.x line, via Architectury Loom's NO-REMAP variant.
//
// From MC 26.1 the game ships unobfuscated, so there is no SRG namespace, no mappings tree
// and nothing for remapJar to do — arch-loom's plain `dev.architectury.loom` would still
// demand `mappings(...)` and try to remap. `dev.architectury.loom-no-remap` is the same
// toolchain with that whole layer switched off.
//
// The node id stays "<mc>-forge", so every `//? if forge` gate in the shared source applies
// here exactly as it does on the classic nodes. Adapted from OneBlock's build.forgenr.gradle.kts,
// which is where every workaround below was first paid for.
plugins {
	id("mod-platform")
	id("dev.architectury.loom-no-remap")
}

stonecutter {
	val (version, loader) = current.project.split('-', limit = 2)
	properties.tags(version, loader)
}

platform {
	loader = "forge"
	dependencies {
		required("minecraft") {
			// Exact range, NOT a bare version — see build.forgeg.gradle.kts. exactMcRange pads
			// two-component versions, which matters again here: "26.2" -> "[26.2.0]".
			// Exact is only the DEFAULT — see declaredMcRange / `deps.minecraft-range`.
			forgeLikeVersionRange = declaredMcRange(fabricLike = false)
		}
		required("forge") {
			forgeLikeVersionRange.set("[1,)")
		}
		required("codxlib") {
			// The settings framework (api.settings) landed in 1.4.0; anything older has no
			// CodxSettings at all, so this floor is not negotiable.
			forgeLikeVersionRange.set("[1.4.0,)")
		}
		// NOTE: no Citadel dependency — the subset Alex's Mobs uses is bundled into the mod
		// under com.github.alexthe666.alexsmobs.citadel (see docs/citadel-bundling.md).
	}
}

// 26.x is unobfuscated, so the runtime member names ARE the Mojang ones — this node reads the
// SAME Mojmap access transformer the NeoForge nodes do, not the SRG accesstransformer.cfg the
// classic Forge nodes read. processResources below ships it as META-INF/accesstransformer.cfg.
// The ACTIVE Stonecutter node compiles root src/ and gets no generated copy in versions/<node>/,
// so fall back to the root file.
val accessTransformerFile = file("src/main/resources/META-INF/accesstransformer_mojmap.cfg")
	.takeIf { it.exists() }
	?: rootProject.file("src/main/resources/META-INF/accesstransformer_mojmap.cfg")

loom {
	forge {
		mixinConfig("alexsmobs.mixins.json")
		accessTransformer(accessTransformerFile)
	}

	// ⚠️ loom-no-remap does NOT forward `forge { mixinConfig(...) }` to the dev run's program
	// args the way classic loom does, and Forge 26 (64.x) has no mixin-config discovery of its
	// own at all — there is no `mixin.config`/`MixinConfigs` handling anywhere in fmlloader,
	// fmlcore, forge-transformers or the universal jar. Mixin itself reads the `MixinConfigs`
	// MANIFEST attribute (which the built jar carries, from ModPlatformPlugin) and the
	// `-mixin.config` ModLauncher argument (which is how the classic Forge nodes get it).
	// The exploded dev output has neither, so on 26.x **no mixin applied at all** in runServer /
	// runClient: the first symptom was `NullPointerException: Block id not set` out of
	// BlockBehaviour.<init>, i.e. BlockPropertiesMixin silently not being there.
	// Compare the two logs to spot this: a healthy node's `ModLauncher running: args [...]`
	// line contains `-mixin.config, <modId>.mixins.json`.
	runs.configureEach {
		programArgs("-mixin.config", "${prop("mod.id")}.mixins.json")
	}
}

repositories {
	mavenCentral()
	mavenLocal()   // CodxLib per-node jars (codxlib/scripts/install_maven_local.py puts them here)
	maven("https://maven.minecraftforge.net/") { name = "MinecraftForge" }
	maven("https://cursemaven.com") {
		name = "CurseMaven"
		content { includeGroup("curse.maven") }
	}
	maven("https://maven.blamejared.com") { name = "BlameJared (JEI)" }
}

dependencies {
	minecraft("com.mojang:minecraft:${prop("deps.minecraft")}")
	// NO mappings(...) — loom-no-remap against the unobfuscated 26.x jar.
	"forge"("net.minecraftforge:forge:${prop("deps.minecraft")}-${prop("deps.forge")}")

	// CodxLib — per-node jar from mavenLocal (codx:codxlib:<ver>-forge+<mc>). Plain
	// `implementation`, like everything else here: loom-no-remap does no remapping on the
	// unobfuscated 26.x jar, so there is nothing for `modImplementation` to do.
	implementation("codx:codxlib:${prop("deps.codxlib")}-forge+${prop("deps.minecraft")}")

	// MixinExtras — see the same block in build.forgeg.gradle.kts. Forge bundles and bootstraps it
	// but keeps it off the compile classpath, so `@ModifyExpressionValue` in mixin/EntityMixin
	// needs it here too. `compileOnly`: javac only, never shaded, supplied at runtime by Forge.
	compileOnly("io.github.llamalad7:mixinextras-common:0.5.4")

	// JEI has no 26.x release; with no deps.jei pin the convention plugin drops compat/jei/**
	// from the compile entirely, so this branch is dead on every node that uses this buildscript.
	// It is `compileOnly`, not `modCompileOnly` — loom-no-remap has no remapping configurations.
	val jei = propOrNull("deps.jei")
	if (jei != null) {
		val jeiMc = prop("deps.jei-mc")
		compileOnly("mezz.jei:jei-$jeiMc-common-api:$jei")
		compileOnly("mezz.jei:jei-$jeiMc-forge-api:$jei")
	}
}

// loom-no-remap still pulls in Architectury's dev naming + mixin-remapper services, which exist
// only to remap mixin refmaps between namespaces. On unobfuscated 26.x there is no remapping to
// do, yet those services always run and abort the dev run demanding a mappings tree /
// `architectury.naming.sourceNamespace` that loom-no-remap never sets ("Missing required system
// property"). Keep them off the run classpath entirely.
configurations.configureEach {
	exclude(group = "dev.architectury", module = "architectury-naming-service")
	exclude(group = "dev.architectury", module = "architectury-mixin-remapper-service")
}

// Ship the Mojang-named AT as the jar's META-INF/accesstransformer.cfg and drop the SRG one —
// same as the NeoForge nodes, for the same reason (26.x has no SRG namespace).
tasks.named<ProcessResources>("processResources") {
	exclude("META-INF/accesstransformer.cfg")
	rename("accesstransformer_mojmap.cfg", "accesstransformer.cfg")

	// Forge 62+ (the 26.x fork) uses a stricter securemodules that, in a dev run, scans each
	// classpath entry for transformer services and derives an automatic module name for it. The
	// mod's *exploded* dev output (build/classes/**, build/resources/main) has no module-info and
	// no Automatic-Module-Name, so its filename heuristic yields an EMPTY name → "Invalid module
	// name: '' is not a Java identifier" and the run aborts before loading. Drop a MANIFEST.MF
	// carrying an explicit name into the dev resources. Written as processResources' own final
	// step (not a separate task) so the run tasks, which consume build/resources/main, don't trip
	// Gradle's implicit-dependency validation.
	val mfFile = layout.buildDirectory.file("resources/main/META-INF/MANIFEST.MF")
	val moduleName = prop("mod.id")
	doLast {
		val f = mfFile.get().asFile
		f.parentFile.mkdirs()
		f.writeText("Manifest-Version: 1.0\nAutomatic-Module-Name: $moduleName\n")
	}
}

// The built jar generates its own MANIFEST.MF; don't let the dev copy above collide.
tasks.withType<Jar>().configureEach { duplicatesStrategy = DuplicatesStrategy.EXCLUDE }

// javac reports at most 100 errors by default, which makes "how far off is this node?" a lie
// during a version migration (the real count on a fresh node is in the thousands).
tasks.withType<JavaCompile>().configureEach {
	options.compilerArgs.addAll(listOf("-Xmaxerrs", "9999"))
}

// Alex's Mobs carries essentially no javadoc comments — see build.forgeg.gradle.kts.
tasks.named<Javadoc>("javadoc") { isEnabled = false }

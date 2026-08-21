// Forge via Architectury Loom (builds Forge on modern Gradle 9 — no ForgeGradle, so it
// avoids the FG6/Gradle-8 wall). Applied ONLY to Forge nodes; NeoForge (MDG) keeps its own.
plugins {
	id("mod-platform")
	id("dev.architectury.loom")  // version pinned in settings.gradle.kts resolutionStrategy
}

stonecutter {
	val (version, loader) = current.project.split('-', limit = 2)
	properties.tags(version, loader)
}

platform {
	loader = "forge"
	dependencies {
		required("minecraft") {
			// Exact range, NOT a bare version: "1.20.1" is a Maven *soft* requirement that Forge
			// reads as "[1.20.1,)", so the jar claims to run on every later MC and Modrinth's
			// upload auto-detect cannot pin a game version. See exactMcRange — it also pads
			// two-component versions ("1.21" -> "[1.21.0]") so Modrinth doesn't read the range
			// as the semver X-range "1.21.x".
			// Exact is only the DEFAULT — a node whose MC patch releases are API-identical can
			// widen it with `deps.minecraft-range` in its toml section (26.1.2 does). See
			// declaredMcRange: widen there BEFORE tagging a store, never after.
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

// Alex's Mobs ships Forge access transformers (widens ~60 vanilla members).
// Stonecutter node projects live in versions/<node>/, but the ACTIVE node compiles the
// root src/ directly and never gets a generated copy — so fall back to the root file.
val accessTransformerFile = file("src/main/resources/META-INF/accesstransformer.cfg")
	.takeIf { it.exists() }
	?: rootProject.file("src/main/resources/META-INF/accesstransformer.cfg")

loom {
	silentMojangMappingsLicense()
	forge {
		mixinConfig("alexsmobs.mixins.json")
		accessTransformer(accessTransformerFile)
	}
	// Lets a test harness drive the dev client without editing this file — e.g.
	// AM_CLIENT_ARGS="--quickPlayMultiplayer 127.0.0.1:25565" to join a local dedicated
	// server straight from the launch, skipping the title screen. Whitespace-separated;
	// appended, so nothing loom sets is lost. Mirrors the hook in build.neoforge.gradle.kts.
	runs.named("client") {
		System.getenv("AM_CLIENT_ARGS")?.split(Regex("\\s+"))?.filter { it.isNotBlank() }
			?.forEach { programArg(it) }
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
	mappings(loom.officialMojangMappings())
	"forge"("net.minecraftforge:forge:${prop("deps.minecraft")}-${prop("deps.forge")}")

	// CodxLib — per-node jar from mavenLocal (codx:codxlib:<ver>-forge+<mc>). `modImplementation`
	// rather than plain `implementation` for the same reason JEI uses `modCompileOnly` below: this
	// is a mod jar and Architectury Loom has to remap it into this node's namespace. It is
	// separately distributed, never shaded, so the player needs its jar alongside ours.
	modImplementation("codx:codxlib:${prop("deps.codxlib")}-forge+${prop("deps.minecraft")}")

	// JEI is optional at runtime; only compat/jei/** compiles against it. A node without a
	// deps.jei pin has no JEI for its MC version at all (JEI published nothing for 1.21.2/1.21.3
	// and no Forge flavour after 1.21.1) — the convention plugin drops compat/jei from the
	// compile there, so there is nothing to resolve.
	val jei = propOrNull("deps.jei")
	if (jei != null) {
		val jeiMc = prop("deps.jei-mc")
		modCompileOnly("mezz.jei:jei-$jeiMc-common-api:$jei")
		modCompileOnly("mezz.jei:jei-$jeiMc-forge-api:$jei")
	}
	// NOTE: the full JEI jar is deliberately NOT on the dev runtime classpath. From 1.20.2 on it
	// and the *-api jars both export mezz.jei.api.*, and Forge's stricter module resolution
	// aborts the dev launch with "Modules jei and jei.…api export package … to module minecraft".
	// JEI compat is compile-only; test it by dropping a JEI jar into the run's mods folder.

	// MixinExtras. Forge BUNDLES it and bootstraps it itself (every build in this range), but it
	// does not put it on the compile classpath the way NeoForge and Fabric Loader do — so
	// `@ModifyExpressionValue` in mixin/EntityMixin fails to compile on Forge alone with
	// "package com.llamalad7.mixinextras.injector does not exist". `compileOnly` is therefore
	// exactly right: the annotation is needed by javac, must NOT be shaded into the jar, and is
	// supplied at runtime by Forge. The pinned version only has to carry the annotation's shape;
	// the transformer that reads it is whichever one Forge ships on that node.
	compileOnly("io.github.llamalad7:mixinextras-common:0.5.4")

	// Forge 51.0.x (MC 1.21) ships a userdev POM that forgets jopt-simple, even though
	// cpw.mods.modlauncher's module-info `requires jopt.simple`, so its dev server aborts
	// with "Module jopt.simple not found" (Forge 52+ include it). Add it to loom's Forge
	// runtime library set so it lands on the dev module path. Harmless on nodes that
	// already provide it (duplicate classpath entry).
	"forgeRuntimeLibrary"("net.sf.jopt-simple:jopt-simple:5.0.4")
}

// javac reports at most 100 errors by default, which makes "how far off is this node?" a lie
// during a version migration (the real count on a fresh node is in the thousands).
tasks.withType<JavaCompile>().configureEach {
	options.compilerArgs.addAll(listOf("-Xmaxerrs", "9999"))
}

// Alex's Mobs carries essentially no javadoc comments, so generating javadoc for ~745 files
// costs minutes per node (× 27 nodes) and emits 100 "no comment" warnings for zero value.
// mod-platform still wires javadocJar for publishing; it just packs nothing.
tasks.named<Javadoc>("javadoc") { isEnabled = false }

// modlauncher `requires jopt.simple` — the automatic module name of jopt-simple 5.0.4.
// jopt-simple 6.0-alpha-* ships a real module-info named `joptsimple` (no dot), which does
// NOT satisfy that requires. Forge 51's constraints try to pull 6.0-alpha-3, so pin 5.0.4.
configurations.configureEach {
	resolutionStrategy { force("net.sf.jopt-simple:jopt-simple:5.0.4") }
}

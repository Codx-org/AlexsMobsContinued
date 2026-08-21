// Fabric on the OBFUSCATED line (<= 1.21.11), via Architectury Loom's normal (remapping) variant.
//
// The 26.x Fabric nodes use build.fabricnr.gradle.kts (loom-no-remap) because from MC 26.1 the game
// ships with Mojang names at runtime — there is no namespace to remap to. Everything at 1.21.11 and
// below is obfuscated, so the full mappings machinery comes back and with it three differences from
// that file, all of them load-bearing:
//
//   1. `mappings(loom.officialMojangMappings())` — the whole source tree is written in Mojmap, the
//      same names the Forge/NeoForge nodes compile against, so "named" here IS Mojmap. Loom remaps
//      named -> intermediary on the way into the jar.
//   2. `modImplementation` / `modCompileOnly` instead of plain `implementation` / `compileOnly`:
//      fabric-loader, fabric-api and JEI are themselves intermediary-namespace mod jars and have to
//      be remapped onto the compile classpath.
//   3. The output artifact is `remapJar`, not `jar` — and the access widener is remapped with it,
//      which is why this node reads a "named"-namespace widener where 26.x reads an "official" one.
//      ModPlatformPlugin already resolves the jar task lazily, so nothing else changes.
//
// The node id is "<mc>-fabric", so every `//? if fabric` gate in the shared source keys off it.
plugins {
	id("mod-platform")
	id("dev.architectury.loom")
}

stonecutter {
	val (version, loader) = current.project.split('-', limit = 2)
	properties.tags(version, loader)
}

platform {
	loader = "fabric"
	dependencies {
		required("minecraft") {
			// Fabric ranges are semver, not Maven — the exactMcRange() bracket trick the
			// Forge/NeoForge scripts need does not apply, and a bare version IS exact here.
			// A node may still widen it with `deps.minecraft-range` (semver syntax here,
			// e.g. ">=26.1 <=26.1.2") — see declaredMcRange.
			fabricLikeVersionRange = declaredMcRange(fabricLike = true)
		}
		required("fabric-api") {
			slug("fabric-api")
			fabricLikeVersionRange = ">=${prop("deps.fabric-api")}"
		}
		required("fabricloader") {
			// Per-MC floor, NOT the build-time pin (deps.fabric-loader) — see the long note in
			// stonecutter.properties.toml. Shipping the pin here makes launchers with an
			// MC-appropriate (older) loader refuse to start; it crashed codxlib 1.3.3.
			fabricLikeVersionRange = ">=${prop("deps.fabric-loader-min")}"
		}
		required("codxlib") {
			slug("codxlib")
			// The settings framework (api.settings) landed in 1.4.0; anything older has no
			// CodxSettings at all, so this floor is not negotiable.
			fabricLikeVersionRange = ">=1.4.0"
		}
		// NOTE: no Citadel dependency — the subset Alex's Mobs uses is bundled into the mod
		// under com.github.alexthe666.alexsmobs.citadel. Fabric has no Citadel at all, so
		// that vendoring is what makes a Fabric node possible in the first place.
	}
}

// Fabric's answer to the Forge access transformer, EXPANDED PER NODE from the single predicated
// template at src/main/accesswidener/. ⚠️ Unlike an AT entry, an AW entry naming an absent member is
// a HARD error rather than a silent no-op, and both the header namespace ("named" here, "official"
// on the unobfuscated 26.x nodes) and eight individual entries differ across the Fabric range — so
// there is no one file that serves every node. See build-logic/AccessWidener.kt and the template's
// own header. This node's arm gets `named` and the pre-1.21.11 descriptors automatically.
val accessWidenerFile = generateAccessWidener(prop("mod.fabric.access_widener"))

loom {
	silentMojangMappingsLicense()
	accessWidenerPath = accessWidenerFile

	runs {
		named("server") {
			server()
			runDir = "run/"
		}
		named("client") {
			client()
			runDir = "run/"
			// Lets a test harness drive the dev client without editing this file — e.g.
			// AM_CLIENT_ARGS="--quickPlayMultiplayer 127.0.0.1:25565" to join a local dedicated
			// server straight from the launch, skipping the title screen. Whitespace-separated;
			// appended, so nothing loom sets is lost. Mirrors build.forgeg/build.neoforge.
			System.getenv("AM_CLIENT_ARGS")?.split(Regex("\\s+"))?.filter { it.isNotBlank() }
				?.forEach { programArg(it) }
		}
	}
}

repositories {
	mavenCentral()
	mavenLocal()   // CodxLib per-node jars (codxlib/scripts/install_maven_local.py puts them here)
}

configurations.all {
	resolutionStrategy {
		force("net.fabricmc:fabric-loader:${prop("deps.fabric-loader")}")
	}
}

dependencies {
	minecraft("com.mojang:minecraft:${prop("deps.minecraft")}")
	mappings(loom.officialMojangMappings())
	modImplementation("net.fabricmc:fabric-loader:${prop("deps.fabric-loader")}")
	modImplementation("net.fabricmc.fabric-api:fabric-api:${prop("deps.fabric-api")}")

	// CodxLib — per-node jar from mavenLocal (codx:codxlib:<ver>-fabric+<mc>). `modImplementation`
	// rather than plain `implementation` so loom remaps it into this node's mapping namespace;
	// it is a separately-distributed mod, never shaded, so the player needs its jar too.
	modImplementation("codx:codxlib:${prop("deps.codxlib")}-fabric+${prop("deps.minecraft")}")

	// JSR-305 (@Nullable / @Nonnull). Upstream Alex's Mobs annotates ~120 files with it and gets it
	// for free — Forge and NeoForge both put it on the compile classpath transitively. Fabric does
	// not, so without this every one of those files fails with "package javax.annotation does not
	// exist". compileOnly is enough: the annotations are CLASS-retention and nothing reads them at
	// runtime, so there is no reason to make players' installs carry the jar.
	compileOnly("com.google.code.findbugs:jsr305:3.0.2")

	// Unlike the 26.x buildscript this branch is LIVE: JEI ships a Fabric flavour for 1.20.1
	// through 1.21.1, which are exactly the nodes that carry a deps.jei pin. With no pin the
	// convention plugin drops compat/jei/** from the compile entirely and this is skipped.
	val jei = propOrNull("deps.jei")
	if (jei != null) {
		val jeiMc = prop("deps.jei-mc")
		modCompileOnly("mezz.jei:jei-$jeiMc-common-api:$jei")
		modCompileOnly("mezz.jei:jei-$jeiMc-fabric-api:$jei")
	}
}

tasks.withType<Jar>().configureEach { duplicatesStrategy = DuplicatesStrategy.EXCLUDE }

// javac reports at most 100 errors by default, which makes "how far off is this node?" a lie
// during a migration.
tasks.withType<JavaCompile>().configureEach {
	options.compilerArgs.addAll(listOf("-Xmaxerrs", "9999"))
}

// Alex's Mobs carries essentially no javadoc comments — see build.forgeg.gradle.kts.
tasks.named<Javadoc>("javadoc") { isEnabled = false }

// Fabric on the UNOBFUSCATED 26.x line, via Architectury Loom's NO-REMAP variant.
//
// Same reasoning as build.forgenr.gradle.kts: from MC 26.1 the game ships with Mojang names at
// runtime, so there is no mappings tree and nothing for remapJar to do. Plain loom would still
// demand `mappings(...)`; `dev.architectury.loom-no-remap` is the same toolchain with that
// layer switched off, so the output artifact is `jar` and mod deps are used as-is
// (plain `implementation`, never `modImplementation`).
//
// The node id is "26.2-fabric", so every `//? if fabric` gate in the shared source keys off it.
// Fabric is being built newest-first and back-filled downward — see settings.gradle.kts.
plugins {
	id("mod-platform")
	id("dev.architectury.loom-no-remap")
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
			// A node may still widen it with `deps.minecraft-range` — see declaredMcRange.
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
// template at src/main/accesswidener/ — same widening set as META-INF/accesstransformer_mojmap.cfg,
// different syntax, and every FIELD entry additionally carries a descriptor the AT does not have.
// ⚠️ Unlike an AT entry, an AW entry naming an absent member is a HARD error rather than a silent
// no-op, which is why the template is gated at all. This unobfuscated node's arm gets the
// `official` header (loom-no-remap has no mappings tree to resolve a `named` one against) and
// drops the sub-26 entries. See build-logic/AccessWidener.kt.
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
	// NO mappings(...) and plain implementation (not modImplementation) — loom-no-remap skips
	// all remapping on the unobfuscated 26.x jar.
	implementation("net.fabricmc:fabric-loader:${prop("deps.fabric-loader")}")
	implementation("net.fabricmc.fabric-api:fabric-api:${prop("deps.fabric-api")}")

	// CodxLib — per-node jar from mavenLocal (codx:codxlib:<ver>-fabric+<mc>). Plain
	// `implementation`, like everything else here: loom-no-remap does no remapping on the
	// unobfuscated 26.x jar, so there is nothing for `modImplementation` to do.
	implementation("codx:codxlib:${prop("deps.codxlib")}-fabric+${prop("deps.minecraft")}")

	// JSR-305 (@Nullable / @Nonnull). Upstream Alex's Mobs annotates ~120 files with it and gets it
	// for free — Forge and NeoForge both put it on the compile classpath transitively. Fabric does
	// not, so without this every one of those files fails with "package javax.annotation does not
	// exist". compileOnly is enough: the annotations are CLASS-retention and nothing reads them at
	// runtime, so there is no reason to make players' installs carry the jar.
	compileOnly("com.google.code.findbugs:jsr305:3.0.2")

	// JEI publishes no 26.x build, and with no deps.jei pin the convention plugin drops
	// compat/jei/** from the compile entirely — so this branch is dead on every node that
	// uses this buildscript. Kept for when Fabric is back-filled onto a node that has one.
	val jei = propOrNull("deps.jei")
	if (jei != null) {
		val jeiMc = prop("deps.jei-mc")
		compileOnly("mezz.jei:jei-$jeiMc-common-api:$jei")
		compileOnly("mezz.jei:jei-$jeiMc-fabric-api:$jei")
	}
}

// loom-no-remap still pulls in Architectury's dev naming + mixin-remapper services, which exist
// only to remap mixin refmaps between namespaces. On unobfuscated 26.x there is no remapping to
// do, yet those services always run and abort the dev run demanding a mappings tree /
// `architectury.naming.sourceNamespace` that loom-no-remap never sets.
configurations.configureEach {
	exclude(group = "dev.architectury", module = "architectury-naming-service")
	exclude(group = "dev.architectury", module = "architectury-mixin-remapper-service")
}

tasks.withType<Jar>().configureEach { duplicatesStrategy = DuplicatesStrategy.EXCLUDE }

// javac reports at most 100 errors by default, which makes "how far off is this node?" a lie
// during a migration — and this node starts from ~185 files importing net.minecraftforge.**.
tasks.withType<JavaCompile>().configureEach {
	options.compilerArgs.addAll(listOf("-Xmaxerrs", "9999"))
}

// Alex's Mobs carries essentially no javadoc comments — see build.forgeg.gradle.kts.
tasks.named<Javadoc>("javadoc") { isEnabled = false }

plugins {
	id("mod-platform")
	id("net.neoforged.moddev")
}

stonecutter {
	val (version, loader) = current.project.split('-', limit = 2)
	properties.tags(version, loader)
}

platform {
	loader = "neoforge"
	dependencies {
		required("minecraft") {
			// Exact range, NOT a bare version: "1.20.4" is a Maven *soft* requirement that
			// NeoForge reads as "[1.20.4,)", so the jar claims to run on every later MC and
			// Modrinth's upload auto-detect cannot pin a game version. See exactMcRange —
			// it also pads two-component versions ("1.21" -> "[1.21.0]") so Modrinth doesn't
			// read the range as the semver X-range "1.21.x".
			// Exact is only the DEFAULT — see declaredMcRange / `deps.minecraft-range`.
			forgeLikeVersionRange = declaredMcRange(fabricLike = false)
		}
		required("neoforge") {
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

neoForge {
	version = prop("deps.neoforge")

	// Alex's Mobs ships Forge access transformers, written in SRG names. NeoForge dropped SRG
	// in 1.20.2 and only understands Mojang names, so NeoForge nodes get the parallel
	// accesstransformer_mojmap.cfg (processResources below renames it into place in the jar).
	// The ACTIVE Stonecutter node compiles root src/ and gets no generated copy in
	// versions/<node>/, so fall back to the root file.
	accessTransformers.from(
		file("src/main/resources/META-INF/accesstransformer_mojmap.cfg")
			.takeIf { it.exists() }
			?: rootProject.file("src/main/resources/META-INF/accesstransformer_mojmap.cfg")
	)

	runs {
		register("client") {
			client()
			gameDirectory = file("run/")
			ideName = "NeoForge Client (${stonecutter.current.version})"
			programArgument("--username=Dev")
			// Lets a test harness drive the dev client without editing this file — e.g.
			// AM_CLIENT_ARGS="--quickPlayMultiplayer 127.0.0.1:25565" to join a local dedicated
			// server straight from the launch, skipping the title screen. Whitespace-separated;
			// appended, so nothing MDG sets is lost (unlike Gradle's own --args, which replaces).
			System.getenv("AM_CLIENT_ARGS")?.split(Regex("\\s+"))?.filter { it.isNotBlank() }
				?.forEach { programArgument(it) }
		}
		register("server") {
			server()
			gameDirectory = file("run/")
			ideName = "NeoForge Server (${stonecutter.current.version})"
		}
	}

	mods {
		register(prop("mod.id")) {
			sourceSet(sourceSets["main"])
		}
	}
}

repositories {
	mavenCentral()
	mavenLocal()   // CodxLib per-node jars (codxlib/scripts/install_maven_local.py puts them here)
	maven("https://cursemaven.com") {
		name = "CurseMaven"
		content { includeGroup("curse.maven") }
	}
	maven("https://maven.blamejared.com") { name = "BlameJared (JEI)" }

	// The NeoForged maven serves a ZERO-BYTE maven-metadata.xml for org.apache.logging.log4j
	// (HTTP 200, empty body — not a 404), and neoforge 20.4/20.6 pull in
	// net.minecraftforge:unsafe:0.2.0, which asks for the dynamic version log4j:2.11.+.
	// Listing a dynamic version queries every repo, Gradle fails to parse the empty XML and
	// aborts the whole resolution instead of falling through to Maven Central. log4j has no
	// business coming from there anyway, so take it off that repo's menu.
	// (The moddev plugin adds the NeoForged repo itself, hence configureEach rather than a
	// declaration here.)
	withType<MavenArtifactRepository>().configureEach {
		if (url.toString().contains("maven.neoforged.net")) {
			content { excludeGroupByRegex("org\\.apache\\.logging\\.log4j.*") }
		}
	}
}

dependencies {
	// CodxLib — per-node jar from mavenLocal (codx:codxlib:<ver>-neoforge+<mc>). Plain
	// `implementation`: the NeoForge moddev toolchain compiles and runs against Mojmap, the same
	// namespace the published jar carries, so there is nothing to remap. Separately
	// distributed, never shaded — the player needs its jar alongside ours.
	implementation("codx:codxlib:${prop("deps.codxlib")}-neoforge+${prop("deps.minecraft")}")

	// JEI is optional at runtime; only compat/jei/** compiles against it. Same split-API
	// artifacts as the Forge nodes, just the neoforge flavour of the loader-specific one.
	// A node without a deps.jei pin has no JEI for its MC version (JEI published nothing at all
	// for 1.21.2/1.21.3); the convention plugin drops compat/jei from the compile there.
	val jei = propOrNull("deps.jei")
	if (jei != null) {
		val jeiMc = prop("deps.jei-mc")
		compileOnly("mezz.jei:jei-$jeiMc-common-api:$jei")
		compileOnly("mezz.jei:jei-$jeiMc-neoforge-api:$jei")
	}
	// NOTE: the full JEI jar is deliberately NOT on the dev runtime classpath (see the Forge
	// buildscript for why). JEI compat is compile-only.
}

tasks.named("createMinecraftArtifacts") {
	dependsOn(tasks.named("stonecutterGenerate"))
}

// Ship the Mojang-named AT as the jar's META-INF/accesstransformer.cfg and drop the SRG one.
tasks.named<ProcessResources>("processResources") {
	exclude("META-INF/accesstransformer.cfg")
	rename("accesstransformer_mojmap.cfg", "accesstransformer.cfg")
}

// javac reports at most 100 errors by default, which makes "how far off is this node?" a lie
// during a version migration.
tasks.withType<JavaCompile>().configureEach {
	options.compilerArgs.addAll(listOf("-Xmaxerrs", "9999"))
}

// Alex's Mobs carries essentially no javadoc comments — see build.forgeg.gradle.kts.
tasks.named<Javadoc>("javadoc") { isEnabled = false }

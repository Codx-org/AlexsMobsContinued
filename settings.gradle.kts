pluginManagement {
	repositories {
		mavenLocal()
		mavenCentral()
		gradlePluginPortal()
		maven("https://maven.fabricmc.net/") { name = "Fabric" }
		maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
		maven("https://maven.minecraftforge.net/") { name = "MinecraftForge" }
		maven("https://maven.architectury.dev/") { name = "Architectury" }  // architectury-loom (Forge on Gradle 9)
		maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
		maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
		maven("https://maven.parchmentmc.org") { name = "ParchmentMC" }
	}
	// architectury-loom has no plugin marker on the maven; map the id → artifact so the
	// plugins { id("dev.architectury.loom") version "..." } request resolves (incl. snapshots).
	resolutionStrategy {
		eachPlugin {
			if (requested.id.id.startsWith("dev.architectury.loom")) {
				useModule("dev.architectury:architectury-loom:1.17-SNAPSHOT")
			}
		}
	}
	includeBuild("build-logic")
}

plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
	id("dev.kikugie.stonecutter") version "0.9.2"
}

rootProject.name = "AlexsMobsContinued"

// Alex's Mobs is a Forge mod; this tree carries the ORIGINAL Forge source forward across
// MC versions on Forge + NeoForge — and, since the Forge/NeoForge line reached its ceiling
// at 26.2, on Fabric too. Fabric ownership moved HERE (2026-07-30, user's call): the sibling
// AlexsMobsFP repo, which reached a single MC version via a net.minecraftforge.** shim layer,
// is now a reference/archive. Its shim architecture is deliberately NOT copied — the real
// Forge classes are first-class in this tree and would collide with stubs of the same name.
//
// Node availability follows what codxlib empirically got building on Gradle 9:
//   Forge     — no upstream build at all for 1.20.5 / 1.21.2; 1.20.3's userdev is dead
//               (bootstrap-dev:2.0.0 is gone from the NeoForge maven); 1.20.2 skipped by choice
//   NeoForge  — no usable modern bundle below 1.20.4 (1.20.1 NeoForge is the legacy toolchain)
// A version gets a node only once it actually compiles, so the tree never carries a
// known-broken node.
stonecutter {
	create(rootProject) {
		fun forge(version: String) =
			version("$version-forge", version).apply { buildscript = "build.forgeg.gradle.kts" }

		fun neoforge(version: String) =
			version("$version-neoforge", version).apply { buildscript = "build.neoforge.gradle.kts" }

		// MC 26.x ships UNOBFUSCATED — there is no SRG namespace, so its Forge nodes build on
		// arch-loom's no-remap variant. The node id stays "<mc>-forge", so every `//? if forge`
		// gate in the shared source applies unchanged. NeoForge stays on MDG either way.
		fun forgeNoRemap(version: String) =
			version("$version-forge", version).apply { buildscript = "build.forgenr.gradle.kts" }

		// Fabric, same story: on the unobfuscated 26.x line there is nothing to remap, so it
		// uses arch-loom's no-remap variant too. Fabric is being built newest-first and then
		// back-filled downward, because the loader deltas are stable across MC versions while
		// the vanilla API deltas (already paid, node by node, above) are not.
		fun fabricNoRemap(version: String) =
			version("$version-fabric", version).apply { buildscript = "build.fabricnr.gradle.kts" }

		// …and below 26.1 the game is obfuscated again, so Fabric goes back to classic loom with
		// a mappings tree (build.fabric.gradle.kts). The one visible consequence in the tree is
		// the access widener: it has to be declared in the "named" namespace there, which is a
		// second file, selected per node by mod.fabric.access_widener in stonecutter.properties.toml.
		fun fabric(version: String) =
			version("$version-fabric", version).apply { buildscript = "build.fabric.gradle.kts" }

		// ── ported ────────────────────────────────────────────────────────────
		forge("1.20.1")   // upstream baseline: Alex's Mobs 1.22.9, + vendored Citadel
		forge("1.20.4")
		neoforge("1.20.4")
		forge("1.20.6")   // ← 1.20.5 DataComponents break
		neoforge("1.20.6")
		forge("1.21");    neoforge("1.21")        // ← data folders renamed to singular
		forge("1.21.1");  neoforge("1.21.1")

		                  neoforge("1.21.2")    // Forge: no 1.21.2 build upstream
		forge("1.21.3");  neoforge("1.21.3")

		forge("1.21.4");  neoforge("1.21.4")    // ← item model definitions
		forge("1.21.5");  neoforge("1.21.5")    // ← CompoundTag→Optional + RenderPipeline rewrite
		forge("1.21.6");  neoforge("1.21.6")    // ← ValueInput/ValueOutput; Forge ships EventBus 7
		forge("1.21.7");  neoforge("1.21.7")    // ← NeoForge: sendToServer moves, playBidirectional splits
		forge("1.21.8");  neoforge("1.21.8")    // ← NeoForge: mixin-added EntityDataAccessor rejected

		forge("1.21.9");  neoforge("1.21.9")    // ← SubmitNodeCollector submission pipeline

		forge("1.21.10"); neoforge("1.21.10")   // ← vanilla-free; loader-side only

		forge("1.21.11"); neoforge("1.21.11")   // ← ResourceLocation→Identifier, RenderTypes split, 37 package moves

		forgeNoRemap("26.1.2"); neoforge("26.1.2")  // ← unobfuscated 26.x line; Java 25
		forgeNoRemap("26.2");   neoforge("26.2")    // ← Gui→Hud, EntityType→EntityTypes constants

		// Fabric back-fill, newest-first. Every node's access widener is expanded from the one
		// predicated template in accesswidener/ (build-logic/AccessWidener.kt) — the header
		// namespace and eight entries differ across this range, and unlike an AT a wrong entry is
		// a hard build failure, so pre-flight with scripts/aw_check.py before adding a node.
		fabric("1.20.1")
		fabric("1.20.4")
		fabric("1.20.6")
		fabric("1.21");    fabric("1.21.1")
		fabric("1.21.2");  fabric("1.21.3")
		fabric("1.21.4");  fabric("1.21.5")
		fabric("1.21.6");  fabric("1.21.7")
		fabric("1.21.8");  fabric("1.21.9")
		fabric("1.21.10")
		fabric("1.21.11")                           // ← first OBFUSCATED Fabric node (named AW)
		fabricNoRemap("26.1.2")                     // ← back-fill; still unobfuscated, same buildscript
		fabricNoRemap("26.2")                       // ← first Fabric node; back-fill downward from here

		// ── planned (uncomment as each version is ported) ─────────────────────

		vcsVersion = "1.20.1-forge"
	}
}

// arch-loom must know its platform BEFORE its plugin applies. Every project is configured
// on any task, so Forge nodes must declare loom.platform=forge or their loom{forge{}} block
// aborts the whole build. NeoForge uses MDG and ignores this.
gradle.beforeProject {
	if (name.endsWith("-forge")) {
		extensions.extraProperties["loom.platform"] = "forge"
	}
}

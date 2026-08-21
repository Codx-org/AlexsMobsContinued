@file:OptIn(dev.kikugie.stonecutter.StonecutterExperimentalAPI::class)

plugins {
	alias(libs.plugins.stonecutter)
	alias(libs.plugins.dotenv)
	alias(libs.plugins.loom.back.compat).apply(false)
	alias(libs.plugins.neoforged.moddev).apply(false)
	alias(libs.plugins.jsonlang.postprocess).apply(false)
	alias(libs.plugins.mod.publish.plugin).apply(false)
	alias(libs.plugins.kotlin.jvm).apply(false)
	alias(libs.plugins.devtools.ksp).apply(false)
	alias(libs.plugins.fletching.table).apply(false)
	alias(libs.plugins.legacyforge.moddev).apply(false)
}

stonecutter active "1.20.1-forge"

tasks.register("runActiveClient") {
	group = "stonecutter"
	description = "Run client of the active Stonecutter version"
	dependsOn(stonecutter.current!!.project + ":runClient")
}

tasks.register("runActiveServer") {
	group = "stonecutter"
	description = "Run server of the active Stonecutter version"
	dependsOn(stonecutter.current!!.project + ":runServer")
}

stonecutter parameters {
	// ⚠️ Every name a `//? if <loader>` gate can mention must appear here — an unlisted one is an
	// "unknown constant" failure, not a silent false. "fabric" joined in the Fabric wave; note that
	// adding it does NOT retro-fit the tree's existing gates, which are all two-arm forge/neoforge
	// dichotomies that a third loader falls straight through (see docs/notes/fabric.md) — each pair
	// has to be widened by hand.
	constants.match(current.project.substringAfterLast('-'), "neoforge", "forge", "fabric")
	swaps["mod_version"] = "\"${properties.get<String>("mod.version")}\";"
	swaps["mod_id"] = "\"${properties.get<String>("mod.id")}\";"
	swaps["mod_name"] = "\"${properties.get<String>("mod.name")}\";"
	swaps["mod_group"] = "\"${properties.get<String>("mod.group")}\";"
	swaps["minecraft"] = "\"${current.version}\";"
	constants["release"] = properties.get<String>("mod.id") != "modtemplate"

	// ⚠️ A Stonecutter rule sees the ORIGINAL text at every offset an earlier rule has already
	// rewritten — rules do not chain. So where 26.1 moves a symbol that 1.21.11 ALREADY moves, the
	// >=26 group cannot key on the >=1.21.11 group's output; the 1.21.11 rule has to stand down and
	// a >=26 rule take the whole hop from the 1.20.1 spelling in one go. This flag marks the five
	// such rules below (three RenderTypes factories, VillagerTrades, and the button hook).
	val mc26 = eval(current.version, ">=26")
	// …and the same again one MC version later. 26.2 moves four symbols that an EARLIER group
	// already moves (the advancement predicate package, Camera#getMainCamera, getLightColor and
	// ItemTags.FLOWERS), so those rules stand down and the >=26.2 group takes each whole hop itself.
	val mc262 = eval(current.version, ">=26.2")

	// ── Forge → NeoForge package renames ────────────────────────────────────────
	// The source of truth in src/ is the ORIGINAL Forge source, so every NeoForge node
	// gets the whole net.minecraftforge.* namespace rewritten at generation time. Doing
	// it here keeps ~225 files free of loader conditionals; only genuine API divergences
	// (networking, capabilities, registry handles) get //? if forge / //? if neoforge.
	//
	// Rules are ordered longest-prefix-first: the catch-all must run last, after the
	// three namespaces that did NOT move under net.neoforged.neoforge.
	//
	// NOTE: never make a *-neoforge node the ACTIVE version — activation rewrites root
	// src/ in place. Build/run non-active nodes instead (:1.20.4-neoforge:build).
	if (current.project.endsWith("-neoforge")) replacements {
		string("!nf-distmarker", true) { replace("net.minecraftforge.api.distmarker.", "net.neoforged.api.distmarker.") }
		string("!nf-eventbus", true) { replace("net.minecraftforge.eventbus.api.", "net.neoforged.bus.api.") }
		string("!nf-fml", true) { replace("net.minecraftforge.fml.", "net.neoforged.fml.") }
		string("!nf-rest", true) { replace("net.minecraftforge.", "net.neoforged.neoforge.") }

		// Classes NeoForge renamed but kept API-compatible. Longest name first so a shorter
		// rule can't eat a prefix of a longer one (ForgeHooksClient before ForgeHooks).
		string("!nf-cls-hooksclient", true) { replace("ForgeHooksClient", "ClientHooks") }
		string("!nf-cls-hooks", true) { replace("ForgeHooks", "CommonHooks") }
		string("!nf-cls-spawnegg", true) { replace("ForgeSpawnEggItem", "DeferredSpawnEggItem") }
		string("!nf-cls-shearable", true) { replace("IForgeShearable", "IShearable") }
		string("!nf-cls-registries", true) { replace("ForgeRegistries", "NeoForgeRegistries") }
		string("!nf-cls-eventfactory", true) { replace("ForgeEventFactory", "EventHooks") }
		string("!nf-cls-forgemod", true) { replace("ForgeMod", "NeoForgeMod") }
		string("!nf-cls-minecraftforge", true) { replace("MinecraftForge", "NeoForge") }
	}

	// ── Forge → Fabric: the ONE namespace that maps 1:1 ─────────────────────────
	// The NeoForge group above works because every net.minecraftforge.* name has a
	// net.neoforged.* twin. Fabric has no such target namespace, which is the whole reason a
	// Fabric node is a rewrite rather than a rename — so this group is deliberately TINY and
	// must stay that way. Do not grow it by inventing mappings: anything that is not a literal
	// 1:1 rename belongs behind AMPlatform/AMCompat or in the fabric source set.
	//
	// @OnlyIn is the exception. Fabric's @Environment/EnvType is the same annotation with a
	// different name — same targets (TYPE, METHOD, FIELD), same "strip this on the other side"
	// semantics, applied by the loader's own transformer. 135 sites, 88 files, no behaviour
	// change. Doing it here rather than dropping the annotations keeps Fabric's dist stripping
	// working, which matters MORE on Fabric than on the other two loaders: Fabric has no
	// RuntimeDistCleaner-style backstop, so an un-annotated client class that a server path
	// happens to touch is a NoClassDefFoundError rather than a caught, logged block.
	//
	// The two annotation rules cannot eat each other — "@OnlyIn(Dist.CLIENT)" is not a
	// substring of "@OnlyIn(value = Dist.CLIENT)". Keep the tree's two-spelling convention (see
	// !fg26-onlyin-class) intact; these rules do not depend on it, but the Forge 26 ones do.
	// They also PRE-EMPT the later >=26 !mc26-onlyin-member rule on a 26.x Fabric node, which
	// is what we want — rules do not chain, and the first one to claim an offset wins.
	if (current.project.endsWith("-fabric")) replacements {
		string("!fab-onlyin-import", true) {
			replace("import net.minecraftforge.api.distmarker.OnlyIn;", "import net.fabricmc.api.Environment;")
		}
		string("!fab-dist-import", true) {
			replace("import net.minecraftforge.api.distmarker.Dist;", "import net.fabricmc.api.EnvType;")
		}
		string("!fab-onlyin-class", true) { replace("@OnlyIn(Dist.CLIENT)", "@Environment(EnvType.CLIENT)") }
		string("!fab-onlyin-member", true) { replace("@OnlyIn(value = Dist.CLIENT)", "@Environment(EnvType.CLIENT)") }

		// There used to be a third redirect here, for ForgeConfigSpec: the Fabric arm pointed the
		// import at a JSON-backed reimplementation in the mod's own fabric source set. It is gone
		// because that reimplementation won — it is now config/AMConfigSpec and every loader uses
		// it, so there is no loader-dependent spelling left to rewrite. See that class for why.

		// DeferredRegister is the second wholesale redirect, and the same justification applies:
		// the destination is the mod's own fabric source set, not an invented mapping onto a
		// Fabric API — Fabric has NO deferred-registration API, its registries are immediate.
		// fabric/registries/DeferredRegister reproduces create/register/getEntries plus a no-arg
		// register() flush that AlexsMobsFabric calls per registry, in AlexsMobs's order.
		//
		// Keyed on the FULLY-QUALIFIED name, not the import line, because that covers both: the
		// import statements in 25 files AND AMAdvancementTriggerRegistry, which spells the type
		// out in full inside a //? if >=1.20.2 block (a gated line cannot carry an import).
		string("!fab-deferredregister", true) {
			replace(
				"net.minecraftforge.registries.DeferredRegister",
				"com.github.alexthe666.alexsmobs.fabric.registries.DeferredRegister",
			)
		}

		// EntityType.Builder: three of the four methods AMEntityRegistry chains are NeoForge/Forge
		// PATCHES on the vanilla class (verified against the patched sources jar — vanilla declares
		// clientTrackingRange/updateInterval right beside them). Fabric's jar is unpatched, so the
		// 117 entity declarations need the vanilla spellings.
		//
		// setShouldReceiveVelocityUpdates has no vanilla counterpart and simply goes away. Dropping
		// it is not a regression: vanilla's ServerEntity already sends a velocity packet whenever an
		// entity's motion changes, and the Forge flag only forces that on for entities vanilla would
		// otherwise skip — none of these 13 need it (they all pass true for mobs that move under
		// their own power anyway).
		//
		// ⚠️ It is deleted by SWALLOWING IT INTO THE NEIGHBOURING CALL, never by an empty "to".
		// A reversible rule's reverse direction is "to" → "from", and an empty "to" matches at every
		// offset in the file — reversing it onto root src/ would splice the call in between every
		// pair of characters. The two other escapes are closed as well: a /* … */ "to" text would
		// close the enclosing Stonecutter arm early (see docs/notes/stonecutter.md), and a // one
		// would comment out the rest of the builder chain. So each of the two chain shapes gets its own rule whose
		// "to" is the neighbour alone, and whose reverse cannot match anything in root src/.
		//
		// ORDER MATTERS: replacements do not chain and the first rule to claim an offset wins, so
		// the 12-site combined rule must come BEFORE the generic setUpdateInterval rename or that
		// rename claims the tail of the match first and this one never fires.
		string("!fab-etb-velocity-mid", true) {
			replace(
				".setShouldReceiveVelocityUpdates(true).setUpdateInterval(1)",
				".updateInterval(1)",
			)
		}
		// The 13th site (straddleboard) already uses the vanilla clientTrackingRange spelling and
		// ends the chain, so it is anchored on the registry name that follows it — otherwise the
		// reverse would match the bare ".clientTrackingRange(10)" that root src/ genuinely contains.
		string("!fab-etb-velocity-tail", true) {
			replace(
				".clientTrackingRange(10).setShouldReceiveVelocityUpdates(true), \"straddleboard\"",
				".clientTrackingRange(10), \"straddleboard\"",
			)
		}
		string("!fab-etb-trackingrange", true) { replace(".setTrackingRange(", ".clientTrackingRange(") }
		string("!fab-etb-updateinterval", true) { replace(".setUpdateInterval(", ".updateInterval(") }

		// Multipart entities. Forge/NeoForge supply net.minecraftforge.entity.PartEntity AND patch
		// vanilla Entity with isMultipartEntity()/getParts(); Fabric has neither. The base class is
		// vendored (fabric/entity/PartEntity — 25 lines, same relocated-compat-namespace pattern as
		// ForgeConfigSpec and DeferredRegister) and the parent-side patch is replaced by the
		// loader-neutral IMultipartOwner interface, which the three parents implement on EVERY
		// loader — so the three part classes and the three parents all stay ungated.
		//
		// Keyed on the fully-qualified name so one rule covers both the three import lines and the
		// inline "net.minecraftforge.entity.PartEntity<?>[]" return types in the three parents and
		// in IMultipartOwner.
		//
		// ⚠️ This restores the TYPE, not the level plumbing — see the note in the vendored class.
		string("!fab-partentity", true) {
			replace(
				"net.minecraftforge.entity.PartEntity",
				"com.github.alexthe666.alexsmobs.fabric.entity.PartEntity",
			)
		}

		// IClientItemExtensions is used in this tree ONLY as an opaque type token — every one of the
		// ~13 IClientExtensionItem implementors has the identical body
		// "consumer.accept((IClientItemExtensions) AlexsMobs.PROXY.getISTERProperties())", and the
		// consumer comes from ClientProxy's NeoForge-only RegisterClientExtensionsEvent handler. So
		// the Fabric side is an EMPTY interface and no call site changes; see the vendored class for
		// what that costs (nothing on >=1.21.4 for the BEWLR half; the armour half moves to Fabric
		// API's ArmorRenderer when the client event layer is restored).
		//
		// Keyed on the fully-qualified name so the one rule covers every import line as well as the
		// two "implements" clauses and the Consumer<> type arguments.
		string("!fab-clientitemext", true) {
			replace(
				"net.minecraftforge.client.extensions.common.IClientItemExtensions",
				"com.github.alexthe666.alexsmobs.fabric.client.IClientItemExtensions",
			)
		}

		// Forge's convention tags. Every use in the tree is unqualified (Tags.Items.X /
		// Tags.Blocks.X), so the FQN appears only on the import line and this one rule covers all
		// ten call sites. The Fabric class DELEGATES to Fabric API's ConventionalItemTags /
		// ConventionalBlockTags rather than re-declaring the c: ids, so the tags are actually
		// populated — see the vendored class. Note this rule is reached with the ORIGINAL 1.20.1
		// spellings (Tags.Items.SHEARS): the TOOLS_SHEAR renames live in the forge/neoforge groups.
		string("!fab-tags", true) {
			replace(
				"net.minecraftforge.common.Tags",
				"com.github.alexthe666.alexsmobs.fabric.common.Tags",
			)
		}

		// The four global loot modifiers implement this interface directly (none extends Forge's
		// LootModifier). Vendoring it keeps their doApply bodies — the actual behaviour — compiling
		// and ready for a Fabric loot-table callback, instead of gating the classes away. See the
		// vendored interface for what is inert until that wiring lands.
		string("!fab-lootmodifier", true) {
			replace(
				"net.minecraftforge.common.loot.IGlobalLootModifier",
				"com.github.alexthe666.alexsmobs.fabric.common.loot.IGlobalLootModifier",
			)
		}

		// The biome/structure spawn modifiers. Same treatment as the loot modifiers and for the
		// same reason: the classes carry AMWorldRegistry's ~88-entry spawn table plus the four
		// structure spawn overrides, which are loader-neutral DATA worth keeping compiled even
		// though Forge's datapack-driven modifier pipeline has no Fabric counterpart. All four
		// names are used unqualified everywhere, so each rule only ever rewrites an import line.
		// See the vendored classes for exactly what is inert until the Fabric wiring lands.
		string("!fab-modbiomeinfo", true) {
			replace(
				"net.minecraftforge.common.world.ModifiableBiomeInfo",
				"com.github.alexthe666.alexsmobs.fabric.common.world.ModifiableBiomeInfo",
			)
		}
		string("!fab-modstructinfo", true) {
			replace(
				"net.minecraftforge.common.world.ModifiableStructureInfo",
				"com.github.alexthe666.alexsmobs.fabric.common.world.ModifiableStructureInfo",
			)
		}
		string("!fab-biomemodifier", true) {
			replace(
				"net.minecraftforge.common.world.BiomeModifier",
				"com.github.alexthe666.alexsmobs.fabric.common.world.BiomeModifier",
			)
		}
		string("!fab-structuremodifier", true) {
			replace(
				"net.minecraftforge.common.world.StructureModifier",
				"com.github.alexthe666.alexsmobs.fabric.common.world.StructureModifier",
			)
		}
		string("!fab-brewingrecipe", true) {
			replace(
				"net.minecraftforge.common.brewing.BrewingRecipe",
				"com.github.alexthe666.alexsmobs.fabric.common.brewing.BrewingRecipe",
			)
		}
		string("!fab-attribevent", true) {
			replace(
				"net.minecraftforge.event.entity.EntityAttributeCreationEvent",
				"com.github.alexthe666.alexsmobs.fabric.entity.EntityAttributeCreationEvent",
			)
		}
		// One call site (AlexsMobs#sendMSGToAll). Fabric API has no static server accessor, so the
		// stand-in forwards to the handle AlexsMobsFabric captures from ServerLifecycleEvents.
		string("!fab-serverlifecycle", true) {
			replace(
				"net.minecraftforge.server.ServerLifecycleHooks",
				"com.github.alexthe666.alexsmobs.fabric.server.ServerLifecycleHooks",
			)
		}

		// Item handlers — the one capability this mod both consumes and exposes. Both names are
		// used unqualified everywhere, so each rule only ever rewrites an import line, plus the
		// return type of AMItemHandlers#find. Unlike the other vendored Forge types above these
		// are NOT inert: AMItemHandlers implements the interface over Fabric API's transfer API.
		// Neither name is a prefix of the other, so their order does not matter.
		string("!fab-itemhandler", true) {
			replace(
				"net.minecraftforge.items.IItemHandler",
				"com.github.alexthe666.alexsmobs.fabric.items.IItemHandler",
			)
		}
		string("!fab-itemhandlerhelper", true) {
			replace(
				"net.minecraftforge.items.ItemHandlerHelper",
				"com.github.alexthe666.alexsmobs.fabric.items.ItemHandlerHelper",
			)
		}

		// ── The Forge event bus, as consumed by ServerEvents ────────────────────────
		// The third and largest wholesale redirect, and the same justification as ForgeConfigSpec
		// and DeferredRegister above: the destination is the mod's own fabric/forge/ source set,
		// not an invented mapping onto a Fabric API. ServerEvents is 1,117 lines of handlers that
		// are already thin adapters over loader-neutral logic — redirecting the ~20 event TYPES it
		// names is what lets that file stay byte-identical on all three loaders instead of being
		// forked. fabric/forge/EventDispatcher fires the stubs from Fabric API callbacks.
		//
		// Every rule is keyed on `import <fqn>` — NOT on the bare FQN — for two reasons:
		//  1. The stub classes' own javadoc quotes the Forge names it stands in for. A bare-FQN
		//     rule would rewrite those {@code} spans and leave each stub documented as standing in
		//     for itself.
		//  2. !fab-attribevent above is keyed on a bare FQN under net.minecraftforge.event.entity.
		//     Rules do not chain and the first to claim an offset wins, so a broad prefix rule here
		//     would race it. Keying on the import keyword sidesteps the question — but note the
		//     rules below still deliberately avoid a bare `net.minecraftforge.event.entity.`
		//     prefix, spelling out the three types in that package instead.
		//
		// The one exception is !fab-fe-explosion: ExplosionEvent.Detonate has no import, it is
		// spelled fully-qualified in the handler signature (a gated line cannot carry an import).
		string("!fab-fe-eventbus", true) {
			replace(
				"import net.minecraftforge.eventbus.api.",
				"import com.github.alexthe666.alexsmobs.fabric.forge.eventbus.api.",
			)
		}
		string("!fab-fe-reload", true) {
			replace(
				"import net.minecraftforge.event.AddReloadListenerEvent;",
				"import com.github.alexthe666.alexsmobs.fabric.forge.event.AddReloadListenerEvent;",
			)
		}
		string("!fab-fe-tick", true) {
			replace(
				"import net.minecraftforge.event.TickEvent;",
				"import com.github.alexthe666.alexsmobs.fabric.forge.event.TickEvent;",
			)
		}
		string("!fab-fe-entityevent", true) {
			replace(
				"import net.minecraftforge.event.entity.EntityEvent;",
				"import com.github.alexthe666.alexsmobs.fabric.forge.event.entity.EntityEvent;",
			)
		}
		string("!fab-fe-lightning", true) {
			replace(
				"import net.minecraftforge.event.entity.EntityStruckByLightningEvent;",
				"import com.github.alexthe666.alexsmobs.fabric.forge.event.entity.EntityStruckByLightningEvent;",
			)
		}
		string("!fab-fe-projectile", true) {
			replace(
				"import net.minecraftforge.event.entity.ProjectileImpactEvent;",
				"import com.github.alexthe666.alexsmobs.fabric.forge.event.entity.ProjectileImpactEvent;",
			)
		}
		// A star import, so this one MUST be a package prefix — a wildcard is not something a
		// per-type rule can follow. Every living.* type ServerEvents names is stubbed.
		string("!fab-fe-living", true) {
			replace(
				"import net.minecraftforge.event.entity.living.",
				"import com.github.alexthe666.alexsmobs.fabric.forge.event.entity.living.",
			)
		}
		string("!fab-fe-player", true) {
			replace(
				"import net.minecraftforge.event.entity.player.",
				"import com.github.alexthe666.alexsmobs.fabric.forge.event.entity.player.",
			)
		}
		string("!fab-fe-village", true) {
			replace(
				"import net.minecraftforge.event.village.",
				"import com.github.alexthe666.alexsmobs.fabric.forge.event.village.",
			)
		}
		// A package prefix rather than a per-type rule, because ClientEvents imports this package
		// with a STAR — `import net.minecraftforge.client.event.*;` — which no per-type rule can
		// follow. It also subsumes what used to be the ComputeFovModifierEvent-only rule.
		// Every client.event type either has a stub or is gated out of the Fabric compile:
		// RenderLevelStageEvent's arms are all `forge &&`/`neoforge &&`, and RenderGuiOverlayEvent
		// is `<1.20.5 && !fabric`. A star import of a package does not require any of its members.
		string("!fab-fe-clientevent", true) {
			replace(
				"import net.minecraftforge.client.event.",
				"import com.github.alexthe666.alexsmobs.fabric.forge.client.event.",
			)
		}
		string("!fab-fe-mcforge", true) {
			replace(
				"import net.minecraftforge.common.MinecraftForge;",
				"import com.github.alexthe666.alexsmobs.fabric.forge.common.MinecraftForge;",
			)
		}
		string("!fab-fe-explosion", true) {
			replace(
				"net.minecraftforge.event.level.ExplosionEvent.",
				"com.github.alexthe666.alexsmobs.fabric.forge.event.level.ExplosionEvent.",
			)
		}
	}

	// ── Fabric API's OWN renames, below 26 ──────────────────────────────────────
	// Distinct from the group above: those map Forge names onto this mod's fabric source set,
	// these track Fabric API renaming ITSELF between the 0.155.x line (MC 26.x) and the 0.141.x
	// line (MC 1.21.11 and below). The shared source is written against the NEWER spelling because
	// 26.2 was the first Fabric node; every back-filled node hops back to the older one here.
	//
	// Each pair was javap-verified to be a pure rename — same arity, same parameter types, same
	// semantics — against fabric-api-0.155.2+26.2 and fabric-api-0.141.6+1.21.11. Anything whose
	// SHAPE differs is deliberately NOT here: BlockColorRegistry takes a List<BlockTintSource> and
	// has no pre-26 counterpart at all, so it gets a source-level arm in ClientProxy instead.
	//
	// ⚠️ Two of these must stay narrowly keyed rather than bare:
	//   • ".getMobSpawnSettings()" also names a method on the mod's OWN vendored
	//     ModifiableBiomeInfo.BiomeInfo.Builder, which AMWorldRegistry calls ~88 times. Keyed on the
	//     "context." receiver so only FabricBiomeModifications' call site can match.
	//   • "ModelLayerRegistry" is a SUBSTRING of its own destination, EntityModelLayerRegistry, so it
	//     is keyed on the package prefix — which the prose comments naming the new class lack.
	if (current.project.endsWith("-fabric") && !eval(current.version, ">=26")) replacements {
		// Longest first: the nested type must be claimed before the bare enclosing name, since rules
		// do not chain and the first rule to claim an offset wins.
		string("!fabapi-particle-pending", true) {
			replace("ParticleProviderRegistry.PendingParticleProvider", "ParticleFactoryRegistry.PendingParticleFactory")
		}
		string("!fabapi-particle-reg", true) { replace("ParticleProviderRegistry", "ParticleFactoryRegistry") }
		string("!fabapi-modellayer", true) {
			replace("rendering.v1.ModelLayerRegistry", "rendering.v1.EntityModelLayerRegistry")
		}
		string("!fabapi-livinglayer-cb", true) {
			replace("LivingEntityRenderLayerRegistrationCallback", "LivingEntityFeatureRendererRegistrationCallback")
		}
		string("!fabapi-itemgroup", true) {
			replace(
				"net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab",
				"net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup",
			)
		}
		string("!fabapi-biome-holder", true) { replace("selection.getBiomeHolder()", "selection.getBiomeRegistryEntry()") }
		string("!fabapi-biome-spawns", true) { replace("context.getMobSpawnSettings()", "context.getSpawnSettings()") }
		// The two payload-registry accessors, renamed in the 0.155.x line. javap-verified pure
		// renames: both spellings are static, no-arg, and return PayloadTypeRegistry<
		// RegistryFriendlyByteBuf>. They are keyed on the receiver so the bare words playS2C/playC2S
		// can never be claimed elsewhere. Below 1.20.5 there is no payload registry at all, and the
		// AMFabricNetwork arm naming these is commented out there — a rewrite inside a commented arm
		// is harmless, so this group does not need a second version bound.
		string("!fabapi-payload-s2c", true) {
			replace("PayloadTypeRegistry.clientboundPlay()", "PayloadTypeRegistry.playS2C()")
		}
		string("!fabapi-payload-c2s", true) {
			replace("PayloadTypeRegistry.serverboundPlay()", "PayloadTypeRegistry.playC2S()")
		}
	}

	// Entity#getStepHeight is a FORGE PATCH below 1.20.5 — vanilla's accessor there is maxUpStep().
	// The root source is Forge 1.20.1, so it says getStepHeight(); the >=1.20.5 group below rewrites
	// it to maxUpStep() because that is the name vanilla kept until 1.21.2 gave the attribute-backed
	// getStepHeight() to everyone. Fabric below 1.20.5 needs the SAME rewrite for the opposite
	// reason — no Forge patch — so it gets its own group rather than widening that one, which would
	// then also fire on Fabric nodes it must not touch.
	if (current.project.endsWith("-fabric") && !eval(current.version, ">=1.20.5")) replacements {
		string("!fab-stepheight", true) { replace(".getStepHeight()", ".maxUpStep()") }
	}

	// ── 1.20.5 "component-ification" renames ────────────────────────────────────
	// Vanilla renamed a pile of symbols in 1.20.5 without changing their shape. Doing
	// those here rather than with //? if conditionals keeps ~300 call sites clean; only
	// changes that alter a *signature* or *semantics* get a source-level conditional.
	//
	// These never touch the active node (always 1.20.1-forge), so src/ is never rewritten.
	if (eval(current.version, ">=1.20.5")) replacements {
		// pathfinder: the enum and two lookup helpers were renamed, same package + shape
		string("!mc205-pathtype-static", true) { replace("getBlockPathTypeStatic", "getPathTypeStatic") }
		string("!mc205-pathtype-enum", true) { replace("BlockPathTypes", "PathType") }
		// attribute modifier operations
		string("!mc205-attr-add", true) { replace("Operation.ADDITION", "Operation.ADD_VALUE") }
		string("!mc205-attr-mulbase", true) { replace("Operation.MULTIPLY_BASE", "Operation.ADD_MULTIPLIED_BASE") }
		string("!mc205-attr-multotal", true) { replace("Operation.MULTIPLY_TOTAL", "Operation.ADD_MULTIPLIED_TOTAL") }
		// food builder
		string("!mc205-food-sat", true) { replace(".saturationMod(", ".saturationModifier(") }
		string("!mc205-food-always", true) { replace(".alwaysEat()", ".alwaysEdible()") }
		// misc 1:1 renames
		string("!mc205-ignite", true) { replace("setSecondsOnFire(", "igniteForSeconds(") }
		string("!mc205-samecomps", true) { replace("isSameItemSameTags", "isSameItemSameComponents") }
		string("!mc205-mulpose", true) { replace(".mulPoseMatrix(", ".mulPose(") }
		string("!mc205-stepheight", true) { replace(".getStepHeight()", ".maxUpStep()") }
		// enchantments lost the "which action does it apply to" name prefix
		string("!mc205-ench-fortune", true) { replace("Enchantments.BLOCK_FORTUNE", "Enchantments.FORTUNE") }
		string("!mc205-ench-looting", true) { replace("Enchantments.MOB_LOOTING", "Enchantments.LOOTING") }
		// MobEffect: the "does this tick fire" gate was renamed (same (int,int)->boolean shape)
		string("!mc205-effect-tick", true) { replace("isDurationEffectTick", "shouldApplyEffectTickThisTick") }
		// FoodProperties dropped the meat flag entirely — AMCompat#isMeat reads a list instead
		string("!mc205-food-meat-build", true) { replace(".meat().build()", ".build()") }
		string("!mc205-food-meat-effect", true) { replace(".meat().effect(", ".effect(") }
		// ChunkStatus moved into its own package
		string("!mc205-chunkstatus", true) { replace("world.level.chunk.ChunkStatus", "world.level.chunk.status.ChunkStatus") }
		// Entity: "the block I'm standing on" got a clearer name
		string("!mc205-feetstate", true) { replace("getFeetBlockState()", "getBlockStateOn()") }
		// LivingEntity#getDimensions(Pose) became final; the overridable hook is getDefaultDimensions
		string("!mc205-dims-decl", true) { replace("EntityDimensions getDimensions(Pose", "EntityDimensions getDefaultDimensions(Pose") }
		string("!mc205-dims-super", true) { replace("super.getDimensions(", "super.getDefaultDimensions(") }
		// (Forge moving its persisted-NBT key constant from Player down to ServerPlayer used to be
		// a rule here. It is now a three-arm constant in AMCompat instead — Fabric has NEITHER
		// spelling, so a third destination was needed, and the rule could not have provided one:
		// its pattern is a substring of its own replacement, so it cannot be shadowed per-loader
		// without rewriting the shadowing arm too.)
		// AbstractProjectileDispenseBehavior is gone; we ship our own under the same simple name
		string("!mc205-projdispense", true) {
			replace(
				"import net.minecraft.core.dispenser.AbstractProjectileDispenseBehavior;",
				"import com.github.alexthe666.alexsmobs.misc.AbstractProjectileDispenseBehavior;",
			)
		}
	}

	// Minecraft#getFrameTime moved onto the new DeltaTracker. The `true` argument is what makes it
	// identical to the old method: `false` additionally returns 1.0 while the tick loop is frozen,
	// which getFrameTime never did. The accessor was then renamed getTimer -> getDeltaTracker in
	// 1.21.2, so this is two mutually exclusive one-shot rules rather than one rule chained into
	// another: **every replacement group matches against the ORIGINAL source**, never against an
	// earlier group's output, so a follow-up rule keyed on `getTimer()` would find nothing.
	if (eval(current.version, ">=1.21") && !eval(current.version, ">=1.21.2")) replacements {
		string("!mc121-frametime", true) {
			replace(".getFrameTime()", ".getTimer().getGameTimeDeltaPartialTick(true)")
		}
	}
	if (eval(current.version, ">=1.21.2")) replacements {
		string("!mc2102-frametime", true) {
			replace(".getFrameTime()", ".getDeltaTracker().getGameTimeDeltaPartialTick(true)")
		}
	}

	// ── 1.21 renames ────────────────────────────────────────────────────────────
	if (eval(current.version, ">=1.21")) replacements {
		// VertexConsumer's fluent builder was renamed wholesale in 1.21: every setter gained a
		// set*/add* prefix and endVertex() is gone (a vertex is committed when the next one
		// starts, or when the buffer is built). All of these are pure renames with identical
		// argument shapes, and every call site in this mod is a vertex chain — checked.
		//
		// NOT included: `.normal(x, y, z)`, because `PoseStack.Pose#normal()` is a same-named
		// no-arg getter that must NOT be renamed. Vertex normals go through AMVertex instead.
		string("!mc121-vtx-add", true) { replace(".vertex(", ".addVertex(") }
		string("!mc121-vtx-color", true) { replace(".color(", ".setColor(") }
		// Every uv2 call in this mod passes a packed light value; the two-int overload is unused.
		string("!mc121-vtx-light", true) { replace(".uv2(", ".setLight(") }
		string("!mc121-vtx-uv", true) { replace(".uv(", ".setUv(") }
		string("!mc121-vtx-overlay", true) { replace(".overlayCoords(", ".setOverlay(") }
		string("!mc121-vtx-end", true) { replace(".endVertex();", ";") }

		// The position/colour/texture vertex format swapped its colour and UV elements around and
		// was renamed to match. Only the plain three-element form is used here — the *_LIGHTMAP
		// variants kept their old names, so a blanket rename would break them.
		string("!mc121-fmt-postexcolor", true) {
			replace("DefaultVertexFormat.POSITION_COLOR_TEX", "DefaultVertexFormat.POSITION_TEX_COLOR")
		}
		string("!mc121-shader-postexcolor", true) {
			replace("getPositionColorTexShader", "getPositionTexColorShader")
		}

		// Entity#getAddEntityPacket gained a ServerEntity argument. 24 entity classes override it
		// and hand `this` straight to AMPlatform, so both halves are a plain textual thread-through.
		string("!mc121-addentity-decl", true) {
			replace(
				"Packet<ClientGamePacketListener> getAddEntityPacket()",
				"Packet<ClientGamePacketListener> getAddEntityPacket(net.minecraft.server.level.ServerEntity amServerEntity)",
			)
		}
		// EquipmentSlot.Type.ARMOR was split into HUMANOID_ARMOR/ANIMAL_ARMOR. The kangaroo's two
		// switches only ever mean "a worn armour slot", which is the humanoid one.
		string("!mc121-armorslottype", true) { replace("case ARMOR ->", "case HUMANOID_ARMOR ->") }

		string("!mc121-addentity-call", true) {
			replace("AMPlatform.getEntitySpawningPacket(this)", "AMPlatform.getEntitySpawningPacket(this, amServerEntity)")
		}

		// Forge dropped its teleportToWithTicket extension (the chunk ticket it forced is issued by
		// the teleport itself now). Vanilla's plain teleportTo(x, y, z) is what is left.
		string("!mc121-teleportticket", true) { replace(".teleportToWithTicket(", ".teleportTo(") }

		// A handful of SoundEvents constants became Holders in 1.21 (the ones vanilla needs to put
		// into data components). playSound still wants the SoundEvent itself. Only the two crossbow
		// loading sounds the straddler uses are affected — checked against the whole source.
		string("!mc121-snd-crossbow-mid", true) {
			replace("SoundEvents.CROSSBOW_LOADING_MIDDLE", "SoundEvents.CROSSBOW_LOADING_MIDDLE.value()")
		}
		string("!mc121-snd-crossbow-end", true) {
			replace("SoundEvents.CROSSBOW_LOADING_END", "SoundEvents.CROSSBOW_LOADING_END.value()")
		}
	}

	// NeoForge renamed its tool tags in 1.20.5 (SHEARS -> TOOLS_SHEARS) and again in 1.21, to the
	// singular TOOLS_SHEAR; Forge kept the original name the whole way.
	if (current.project.endsWith("-neoforge") && eval(current.version, ">=1.20.5") && !eval(current.version, ">=1.21")) replacements {
		string("!nf205-shears", true) { replace("Tags.Items.SHEARS", "Tags.Items.TOOLS_SHEARS") }
	}

	// ── NeoForge 1.21 renames ───────────────────────────────────────────────────
	// All pure renames with identical shapes; anything that changed a signature or the shape of
	// an event gets a //? if neoforge conditional at the call site instead.
	if (current.project.endsWith("-neoforge") && eval(current.version, ">=1.21")) replacements {
		string("!nf21-shears", true) { replace("Tags.Items.SHEARS", "Tags.Items.TOOLS_SHEAR") }
		// "tool action" became "item ability" (same constants, same canPerformAction methods).
		// Plural first so the shorter rule can't eat its prefix.
		string("!nf21-toolactions", true) { replace("ToolActions", "ItemAbilities") }
		string("!nf21-toolaction", true) { replace("ToolAction", "ItemAbility") }
		// Entity's "am I in a level yet" pair finally dropped the Forge-era "world" wording.
		string("!nf21-addedtolevel-is", true) { replace("isAddedToWorld()", "isAddedToLevel()") }
		string("!nf21-addedtolevel-on", true) { replace("onAddedToWorld()", "onAddedToLevel()") }
		// The spawn-placement mod-bus event was renamed to read as a verb; same register() overloads,
		// same Operation enum. Matched from "event.entity." so it survives the namespace rewrite above.
		string("!nf21-spawnplacements", true) {
			replace("event.entity.SpawnPlacementRegisterEvent", "event.entity.RegisterSpawnPlacementsEvent")
		}
		// LivingChangeTargetEvent spells out that the target is only *about to be* set.
		string("!nf21-newtarget-get", true) { replace(".getNewTarget()", ".getNewAboutToBeSetTarget()") }
		string("!nf21-newtarget-set", true) { replace(".setNewTarget(", ".setNewAboutToBeSetTarget(") }
	}

	// ── MC 1.21.2 vanilla renames (BOTH loaders — these are Mojang's, not a loader's) ──────────
	// Only same-package, same-shape renames belong here. Anything whose signature or semantics
	// changed gets a conditional at the call site instead — notably getMinBuildHeight/
	// getMaxBuildHeight -> getMinY/getMaxY, where the old max was EXCLUSIVE and getMaxY() is
	// INCLUSIVE, so a blind rename would be an off-by-one.
	if (eval(current.version, ">=1.21.2")) replacements {
		// net.minecraft.world.entity.MobSpawnType -> …entity.EntitySpawnReason. Same package, so
		// this rewrites the imports too, and the name appears nowhere else as a substring.
		// The SPAWN_EGG enum value was ALSO renamed to SPAWN_ITEM_USE; that rule must precede the
		// type rename so it can still see the original `MobSpawnType.` prefix (CreativeModeTabs.
		// SPAWN_EGGS is plural and unaffected).
		string("!mc2102-spawnegg", true) { replace("MobSpawnType.SPAWN_EGG", "EntitySpawnReason.SPAWN_ITEM_USE") }
		string("!mc2102-spawnreason", true) { replace("MobSpawnType", "EntitySpawnReason") }

		// LootContext#getParamOrNull -> getOptionalParameter (BlossomLootModifier, BananaLootModifier).
		string("!mc2102-loot-paramornull", true) { replace("getParamOrNull(", "getOptionalParameter(") }

		// Entity#walkDist was deleted in 1.21.2; walkAnimation.position() is the accumulated-distance
		// replacement (only EntityAnaconda uses `this.walkDist`; the render `entity.walkDist` sites are
		// handled separately in the render wave).
		string("!mc2102-walkdist", true) { replace("this.walkDist", "this.walkAnimation.position()") }

		// ── The ServerLevel thread ──────────────────────────────────────────────────────────
		// 1.21.2 pushed a ServerLevel into the front of every server-only entity hook, so the
		// method can no longer reach for `level()` and cast. That is a pure parameter insertion
		// with an identical body, and it lands on ~120 overrides here — far too many to spell
		// out as per-file conditionals, so the declaration and its matching super-call are
		// rewritten as text instead.
		//
		// Each pair is safe because the two halves cannot collide: the declaration rule keys off
		// the parameter's TYPE (`(DamageSource ` / `(Entity `), which only ever appears in a
		// signature, and the super rule keys off `super.`, which only appears inside the override
		// it belongs to. Call sites are NOT rewritten here — they go through AMCompat, which is
		// what puts a ServerLevel in scope where there is none.
		//
		// `amLevel` is the injected parameter name; it appears nowhere in the source otherwise.
		string("!mc2102-invuln-decl", true) {
			replace("isInvulnerableTo(DamageSource ", "isInvulnerableTo(net.minecraft.server.level.ServerLevel amLevel, DamageSource ")
		}
		string("!mc2102-invuln-super", true) { replace("super.isInvulnerableTo(", "super.isInvulnerableTo(amLevel, ") }
		string("!mc2102-dohurt-decl", true) {
			replace("doHurtTarget(Entity ", "doHurtTarget(net.minecraft.server.level.ServerLevel amLevel, Entity ")
		}
		string("!mc2102-dohurt-super", true) { replace("super.doHurtTarget(", "super.doHurtTarget(amLevel, ") }
		string("!mc2102-dropequip-decl", true) {
			replace("dropEquipment() {", "dropEquipment(net.minecraft.server.level.ServerLevel amLevel) {")
		}
		string("!mc2102-dropequip-super", true) { replace("super.dropEquipment()", "super.dropEquipment(amLevel)") }
		// Mob#pickUpItem gained a leading ServerLevel at 1.21.2. Two entities override it
		// (Underminer, Catfish); the decl rule keys on the parameter type so it never hits a call
		// site, and the super rule uses the amLevel the decl introduced.
		string("!mc2102-pickup-decl", true) {
			replace("pickUpItem(ItemEntity ", "pickUpItem(net.minecraft.server.level.ServerLevel amLevel, ItemEntity ")
		}
		string("!mc2102-pickup-super", true) { replace("super.pickUpItem(itemEntity)", "super.pickUpItem(amLevel, itemEntity)") }
		// dropEquipment is protected, so its four internal call sites cannot go through AMCompat.
		// All of them sit inside an already-server-side block (a death drop or a mob conversion
		// behind !level().isClientSide), so casting level() here is exactly what the callee used
		// to do for itself.
		string("!mc2102-dropequip-this", true) {
			replace("this.dropEquipment()", "this.dropEquipment((net.minecraft.server.level.ServerLevel) this.level())")
		}
		// EntityVoidWorm reimplements the death-drop sequence by hand (it splits on death), so it
		// calls these two protected methods directly. Both gained a leading ServerLevel at 1.21.2;
		// same server-side-block reasoning as dropEquipment above. dropExperience only carries the
		// killer Entity from 1.21 onward, so its short form only appears in the >=1.21 loader blocks.
		string("!mc2102-dropfromloot-this", true) {
			replace("this.dropFromLootTable(source, flag)", "this.dropFromLootTable((net.minecraft.server.level.ServerLevel) this.level(), source, flag)")
		}
		string("!mc2102-dropexp-this", true) {
			replace("this.dropExperience(entity)", "this.dropExperience((net.minecraft.server.level.ServerLevel) this.level(), entity)")
		}
		string("!mc2102-aistep-decl", true) {
			replace("customServerAiStep() {", "customServerAiStep(net.minecraft.server.level.ServerLevel amLevel) {")
		}
		string("!mc2102-aistep-super", true) { replace("super.customServerAiStep()", "super.customServerAiStep(amLevel)") }
		// EntityVoidWorm is the one class that overrides spawnAtLocation itself.
		string("!mc2102-spawnat-decl", true) {
			replace("ItemEntity spawnAtLocation(ItemStack ", "ItemEntity spawnAtLocation(net.minecraft.server.level.ServerLevel amLevel, ItemStack ")
		}
		string("!mc2102-kill-decl", true) {
			replace("void kill() {", "void kill(net.minecraft.server.level.ServerLevel amLevel) {")
		}

		// hurt() split in two: Entity#hurt is now `public final void`, and the override point moved
		// to `hurtServer(ServerLevel, DamageSource, float)`. So this is a rename on top of the
		// parameter insertion. Call sites that want the old boolean go through AMCompat.hurt,
		// which routes to hurtOrSimulate.
		string("!mc2102-hurt-decl", true) {
			replace("public boolean hurt(DamageSource ", "public boolean hurtServer(net.minecraft.server.level.ServerLevel amLevel, DamageSource ")
		}
		string("!mc2102-hurt-super", true) { replace("super.hurt(", "super.hurtServer(amLevel, ") }

		// ── InteractionResultHolder ─────────────────────────────────────────────────────
		// 1.21.2 folded InteractionResultHolder<ItemStack> back into InteractionResult. The
		// factory calls all go through AMCompat (which declares itself twice, once per era);
		// what is left is the `use` return type and the import, which no helper can express.
		//
		// These two are deliberately NOT reversible: the reverse of the type rule would turn
		// every plain `InteractionResult` in the tree into a holder. Root src/ is the 1.20.1
		// node and stays that way, so the reverse direction is never needed.
		string("!mc2102-irh-import", true) {
			replace("import net.minecraft.world.InteractionResultHolder;", "import net.minecraft.world.InteractionResult;")
		}
		string("!mc2102-irh-type", true) { replace("InteractionResultHolder<ItemStack>", "InteractionResult") }
		// ItemInteractionResult only ever existed for 1.20.5–1.21.1; useItemOn returns a plain
		// InteractionResult again from 1.21.2, and the one constant this mod uses (SUCCESS) is
		// spelled the same on both.
		string("!mc2102-itemir", true) {
			replace("net.minecraft.world.ItemInteractionResult", "net.minecraft.world.InteractionResult")
		}

		// UseAnim -> ItemUseAnimation, same package, same constants. Matched with a trailing
		// `;`/`.`/space rather than bare, because the accessor is called getUseAnimation and a
		// bare match would mangle it into getItemUseAnimationation.
		string("!mc2102-useanim-import", true) {
			replace("import net.minecraft.world.item.UseAnim;", "import net.minecraft.world.item.ItemUseAnimation;")
		}
		string("!mc2102-useanim-const", true) { replace("UseAnim.", "ItemUseAnimation.") }
		string("!mc2102-useanim-type", true) { replace("UseAnim ", "ItemUseAnimation ") }

		// Only the two SoundEvents constants that item components reference became
		// Holder<SoundEvent> in 1.21.2; the rest of SoundEvents is still bare SoundEvent, so
		// this is a two-constant fix rather than a sweep.
		string("!mc2102-sound-eat", true) { replace("SoundEvents.GENERIC_EAT", "SoundEvents.GENERIC_EAT.value()") }
		string("!mc2102-sound-honey", true) { replace("SoundEvents.HONEY_DRINK", "SoundEvents.HONEY_DRINK.value()") }

		// ── The render-state rewrite ────────────────────────────────────────────────────────
		// 1.21.2 replaced the entity type parameter of the renderer/model/layer hierarchy with a
		// per-frame render state, which lands on ~123 renderers, 63 layers and ~130 models — far
		// too many to rewrite by hand, and the shape of the change is identical in every one.
		//
		// client/render/compat instead reproduces the pre-1.21.2 hierarchy on top of the new one:
		// its classes carry the SAME SIMPLE NAMES as the vanilla ones they stand in for, so the
		// entire migration is these five import swaps and the ~200 declarations below them keep
		// their old type parameters, their old overrides and their old bodies.
		//
		// That package uses the modern API directly and therefore carries no conditionals of its
		// own; ModPlatformPlugin.configureJava excludes it from the compile below 1.21.2.
		//
		// Keyed on the whole import statement including the trailing `;`, which is what makes the
		// rules safe: EntityRendererProvider / EntityRenderers / EntityRenderDispatcher share the
		// prefix but not the statement, and the compat classes themselves refer to their vanilla
		// counterparts fully-qualified rather than by import.
		string("!mc2102-render-import-entity", true) {
			replace(
				"import net.minecraft.client.renderer.entity.EntityRenderer;",
				"import com.github.alexthe666.alexsmobs.client.render.compat.EntityRenderer;",
			)
		}
		string("!mc2102-render-import-living", true) {
			replace(
				"import net.minecraft.client.renderer.entity.LivingEntityRenderer;",
				"import com.github.alexthe666.alexsmobs.client.render.compat.LivingEntityRenderer;",
			)
		}
		string("!mc2102-render-import-mob", true) {
			replace(
				"import net.minecraft.client.renderer.entity.MobRenderer;",
				"import com.github.alexthe666.alexsmobs.client.render.compat.MobRenderer;",
			)
		}
		string("!mc2102-render-import-layer", true) {
			replace(
				"import net.minecraft.client.renderer.entity.layers.RenderLayer;",
				"import com.github.alexthe666.alexsmobs.client.render.compat.RenderLayer;",
			)
		}
		// ~13 files name EntityModel<SomeEntity> as a renderer's or layer's model type. The compat
		// class is the base of this mod's whole model hierarchy on 1.21.2+ (BasicEntityModel
		// extends it), so those declarations stay valid with nothing but this swap.
		string("!mc2102-render-import-model", true) {
			replace(
				"import net.minecraft.client.model.EntityModel;",
				"import com.github.alexthe666.alexsmobs.client.render.compat.EntityModel;",
			)
		}

		// LlamaSpitModel lost its type parameter when its entity became a render state (the two
		// projectiles that borrow it — mosquito spit, sand shot — never touch that state). Both
		// call sites are in this tree only.
		string("!mc2102-llamaspitmodel-decl", true) { replace("LlamaSpitModel<LlamaSpit>", "LlamaSpitModel") }
		string("!mc2102-llamaspitmodel-new", true) { replace("new LlamaSpitModel<>(", "new LlamaSpitModel(") }

		// RenderType#entityGlintDirect folded into #entityGlint (the "direct" variants — those that
		// draw straight to the frame buffer rather than through an intermediate — were merged). One
		// call site (the tendon segment's glint pass).
		string("!mc2102-entityglint", true) { replace(".entityGlintDirect()", ".entityGlint()") }

		// RenderLivingEvent gained a third type parameter (the render state) in 1.21.2. Only the
		// three private helpers in ClientEvents take the wildcard form `RenderLivingEvent<?, ?>`;
		// the concrete `.Pre`/`.Post` subtypes are unaffected (they don't spell the type args).
		string("!mc2102-renderlivingevent", true) { replace("RenderLivingEvent<?, ?>", "RenderLivingEvent<?, ?, ?>") }

		// ── Mechanical vanilla renames ──────────────────────────────────────────────────────
		// Direction#getNormal -> #getUnitVec3i (it returns a Vec3i, and Direction picked up a
		// getUnitVec3 returning a Vec3 alongside it, hence the disambiguating name). All eight
		// call sites in this tree are on a Direction.
		string("!mc2102-unitvec3i", true) { replace(".getNormal()", ".getUnitVec3i()") }

		// Entity#checkInsideBlocks -> #applyEffectsFromBlocks. The method also grew a two-Vec3
		// overload for the movement span; the no-arg one still walks the entity's current AABB,
		// which is what all four call sites want.
		string("!mc2102-inside-blocks", true) { replace(".checkInsideBlocks()", ".applyEffectsFromBlocks()") }

		// Tier/Tiers -> ToolMaterial, a record carrying the same six numbers. Only the IRON
		// constant is used here. The ctor arity changes too, which is why the three tool items
		// still need their own 1.21.2 branch on top of this rename.
		string("!mc2102-toolmaterial-import", true) {
			replace(
				"import net.minecraft.world.item.Tiers;",
				"import net.minecraft.world.item.ToolMaterial;",
			)
		}
		string("!mc2102-toolmaterial", true) { replace("Tiers.IRON", "ToolMaterial.IRON") }

		// Armour moved into net.minecraft.world.item.equipment: ArmorMaterial became a plain
		// record there (no Holder any more) and ArmorItem.Type became the top-level ArmorType.
		// Fully qualifying ArmorType keeps `import net.minecraft.world.item.ArmorItem;` — which
		// several of these files still need for the class itself — valid and unambiguous.
		string("!mc2102-armormaterial-import", true) {
			replace(
				"import net.minecraft.world.item.ArmorMaterial;",
				"import net.minecraft.world.item.equipment.ArmorMaterial;",
			)
		}
		string("!mc2102-armortype", true) {
			replace("ArmorItem.Type", "net.minecraft.world.item.equipment.ArmorType")
		}

		// FastColor.ARGB32 -> ARGB. Not a plain rename: the nesting is gone as well, so both
		// halves have to move at once. Every use in this tree is fully qualified already.
		string("!mc2102-argb", true) { replace("net.minecraft.util.FastColor.ARGB32.", "net.minecraft.util.ARGB.") }

		// 1.21.2 dropped the separate entity_translucent_cull shader; entity_translucent is the
		// culled one now (the composite state's default cull is CULL either way), so this is the
		// same pipeline under a shorter name.
		string("!mc2102-transcull-shader", true) {
			replace("RENDERTYPE_ENTITY_TRANSLUCENT_CULL_SHADER", "RENDERTYPE_ENTITY_TRANSLUCENT_SHADER")
		}

		// Registry#getHolderOrThrow -> #getOrThrow (the HolderGetter method it always shadowed),
		// and RegistryAccess#registry -> #lookup. Both still return what the call sites expect:
		// a Holder.Reference<T> and an Optional<Registry<E>>.
		string("!mc2102-getholderorthrow", true) { replace(".getHolderOrThrow(", ".getOrThrow(") }
		string("!mc2102-registry-lookup", true) { replace(".registryAccess().registry(", ".registryAccess().lookup(") }

		// RegistryAccess#registryOrThrow -> #lookupOrThrow. RegistryAccess narrows the return
		// type back to Registry<E>, so the call sites keep working unchanged — this is only a
		// rename there. (HolderLookup.Provider's own lookupOrThrow returns a RegistryLookup;
		// nothing in this mod calls that one.)
		string("!mc2102-lookuporthrow", true) { replace(".registryOrThrow(", ".lookupOrThrow(") }

		// 1.21.2 renamed the whole Registry lookup surface around holders:
		//   get(ResourceLocation)   used to return T          -> now Optional<Holder.Reference<T>>
		//   getValue(...)           is the new name for the T-returning one
		//   getHolder(...)          is gone; get(...) IS the holder lookup
		// Keyed on the exact `BuiltInRegistries.<REGISTRY>.get(` prefix rather than a bare
		// `.get(`, which would hit every map, Optional and Supplier in the tree. Every one of the
		// 18 call sites passes a ResourceLocation.
		// MOB_EFFECT is deliberately NOT in this list: it has both `.get(` (value) and `.getHolder(`
		// (holder) call sites, and the holder rename's target (`MOB_EFFECT.get(`) is this rule's own
		// source — two reversible rules colliding on the same text, which makes neither apply. Those
		// three MOB_EFFECT sites use per-site `//? if >=1.21.2` blocks instead (getValue / get).
		listOf("ITEM", "BLOCK", "ENTITY_TYPE").forEach { registry ->
			string("!mc2102-registry-getvalue-${registry.lowercase()}", true) {
				replace("BuiltInRegistries.$registry.get(", "BuiltInRegistries.$registry.getValue(")
			}
		}
		// The holder form. Spelled out per receiver rather than as a bare `.getHolder(` so the
		// rule's own reverse stays narrow — reversing a bare `.get(` would hit every map and
		// Optional in the tree.
		string("!mc2102-getholder-registry", true) { replace("registry.getHolder(", "registry.get(") }
		string("!mc2102-getholder-damagetype", true) {
			replace("Registries.DAMAGE_TYPE).getHolder(", "Registries.DAMAGE_TYPE).get(")
		}

		// WalkAnimationState#update grew a third argument (the scale vanilla passes 3.0F for a
		// baby and 1.0F otherwise). All 13 call sites are the same literal line, copied from
		// LivingEntity#aiStep, and none of them is on a baby-scaled mob.
		string("!mc2102-walkanim", true) {
			replace("walkAnimation.update(f2, 0.4F)", "walkAnimation.update(f2, 0.4F, 1.0F)")
		}

		// DirectionProperty was folded into EnumProperty<Direction>, so the import breaks too.
		// All eight files that declare one already import net.minecraft.core.Direction, and the
		// type only ever appears in a `public static final` field declaration here.
		string("!mc2102-dirprop-import", true) {
			replace(
				"import net.minecraft.world.level.block.state.properties.DirectionProperty;",
				"import net.minecraft.world.level.block.state.properties.EnumProperty;",
			)
		}
		string("!mc2102-dirprop-type", true) {
			replace("public static final DirectionProperty ", "public static final EnumProperty<Direction> ")
		}

		// Entity#isAlliedTo(Entity) became public final in 1.21.2; the overridable hook is now
		// the protected considersEntityAsAlly(Entity), which the final method calls in both
		// directions. Every override here reads only the candidate, so redirecting the declaration
		// (and its super. call) onto considersEntityAsAlly preserves the behaviour. The declaration
		// rule keys on the full `public boolean isAlliedTo(Entity` signature so it can't hit a call
		// site, and the super. rule keys on `super.`, which only appears inside the override.
		string("!mc2102-alliedto-decl", true) {
			replace("public boolean isAlliedTo(Entity", "protected boolean considersEntityAsAlly(Entity")
		}
		string("!mc2102-alliedto-super", true) {
			replace("super.isAlliedTo(", "super.considersEntityAsAlly(")
		}

		// LivingEntity#getScale() is final from 1.21.2 (it multiplies the age scale by the SCALE
		// attribute); the overridable hook is getAgeScale(). The three overrides here return a bare
		// size factor with no attribute in play, so getScale() still hands the same value back to
		// every render/hitbox caller. Keyed on the full `public float getScale() {` signature so it
		// only ever hits the override, never a `.getScale()` call site (those have no return type).
		string("!mc2102-getscale-decl", true) {
			replace("public float getScale() {", "public float getAgeScale() {")
		}

		// Shearable#shear grew a ServerLevel and the shearing ItemStack in 1.21.2. The four
		// implementors here only touch level()/random, so the two new parameters are named and
		// ignored. Keyed on the exact old signature, which only appears on the override.
		string("!mc2102-shear-decl", true) {
			replace(
				"public void shear(SoundSource category) {",
				"public void shear(net.minecraft.server.level.ServerLevel amShearLevel, SoundSource category, ItemStack amShearStack) {",
			)
		}

		// SimpleCraftingRecipeSerializer<T> was folded into CustomRecipe.Serializer<T> in 1.21.2
		// (same (CraftingBookCategory)->T factory shape). The import and the two `new …<>(…)` sites
		// live only in AMRecipeRegistry; two rules so the import line isn't double-packaged.
		string("!mc2102-simplecraftserializer-import", true) {
			replace(
				"import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;",
				"import net.minecraft.world.item.crafting.CustomRecipe;",
			)
		}
		string("!mc2102-simplecraftserializer-new", true) {
			replace("new SimpleCraftingRecipeSerializer<>(", "new CustomRecipe.Serializer<>(")
		}
	}

	// ── MC 1.21.4 vanilla renames (BOTH loaders) ───────────────────────────────────────────
	// Same-package/same-shape renames from the 1.21.4 sweep. Anything whose signature or
	// semantics changed (item model definitions, EquipmentModel, metadata sections, the spawn-egg
	// and mob-bucket ctors, TicketType, the NeoForge reload-listener event) gets a source-level
	// conditional or an AMCompat helper instead — those cannot be expressed as a text rename.
	if (eval(current.version, ">=1.21.4")) replacements {
		// Leashable#dropLeash(boolean broadcast, boolean dropItem) lost its arguments: dropLeash()
		// now always broadcasts AND drops the physical lead item, while removeLeash() broadcasts
		// without dropping it. So (true,true) -> dropLeash(), (true,false) -> removeLeash(). The
		// two rules match disjoint substrings; neither reverse hits root src (it has no bare
		// dropLeash()/removeLeash()).
		string("!mc2104-dropleash-item", true) { replace("dropLeash(true, true)", "dropLeash()") }
		string("!mc2104-dropleash-keep", true) { replace("dropLeash(true, false)", "removeLeash()") }

		// Entity#awardKillScore dropped its int score argument (scoreboard kill tracking moved to a
		// criterion). The four overrides here only forward score to super, so dropping it is exact.
		// Two decl rules (Entity / LivingEntity receiver) + the shared super call; each keys on text
		// that only appears in a signature or a super. call, never on a bare call site.
		string("!mc2104-killscore-decl-entity", true) {
			replace("awardKillScore(Entity entity, int score, DamageSource src)", "awardKillScore(Entity entity, DamageSource src)")
		}
		string("!mc2104-killscore-decl-living", true) {
			replace("awardKillScore(LivingEntity entity, int score, DamageSource src)", "awardKillScore(LivingEntity entity, DamageSource src)")
		}
		string("!mc2104-killscore-super", true) {
			replace("super.awardKillScore(entity, score, src)", "super.awardKillScore(entity, src)")
		}

		// RenderShape.ENTITYBLOCK_ANIMATED was removed; a block with a BlockEntityRenderer now just
		// renders through it, so the "don't draw a baked model, the BER does it" role folds into
		// INVISIBLE. All four pirate blocks are fully BER-rendered, so INVISIBLE is the faithful map.
		string("!mc2104-rendershape-animated", true) {
			replace("RenderShape.ENTITYBLOCK_ANIMATED", "RenderShape.INVISIBLE")
		}

		// ItemTags.FLOWERS was removed in favour of SMALL_FLOWERS (the only flower item tag left).
		// This drops tall flowers (sunflower, lilac, rose bush, peony) from what a tamed flutter will
		// eat/accept — a minor content difference limited to 1.21.4+, accepted.
		// (stands down on >=26.2, where !mc262-smallflowers takes both hops itself — rules do not chain)
		if (!mc262) string("!mc2104-itemtag-flowers", true) { replace("ItemTags.FLOWERS", "ItemTags.SMALL_FLOWERS") }

		// 1.21.4 split EquipmentModel: the layer-type enum moved to client.resources.model.
		// EquipmentClientInfo, and the equipment id an armour points at became a
		// ResourceKey<EquipmentAsset> (Equippable#model() → #assetId()). These sites live inside
		// already-active >=1.21.2 render blocks, so token replacements are the cleanest reach.
		string("!mc2104-equip-layertype", true) {
			replace("net.minecraft.world.item.equipment.EquipmentModel.LayerType", "net.minecraft.client.resources.model.EquipmentClientInfo.LayerType")
		}
		// Longer (namespaced) assetId decl first so the shorter rule below can't eat its match.
		string("!mc2104-equip-assetid-fq", true) {
			replace("net.minecraft.resources.ResourceLocation assetId = equippable.model().get();", "net.minecraft.resources.ResourceKey<net.minecraft.world.item.equipment.EquipmentAsset> assetId = equippable.assetId().get();")
		}
		string("!mc2104-equip-assetid", true) {
			replace("ResourceLocation assetId = equippable.model().get();", "net.minecraft.resources.ResourceKey<net.minecraft.world.item.equipment.EquipmentAsset> assetId = equippable.assetId().get();")
		}
		// The remaining equippable.model() uses (the isEmpty() guards).
		string("!mc2104-equip-model", true) { replace("equippable.model()", "equippable.assetId()") }
	}

	// ── 1.21.5 mechanical renames ────────────────────────────────────────────
	// A wide vanilla sweep. The pure renames live here; semantic changes (CompoundTag→Optional
	// getters, ArmorItem/SwordItem removal, the render-pipeline rewrite, owner EntityReference)
	// are handled via AMCompat helpers / per-site conditionals.
	if (eval(current.version, ">=1.21.5")) replacements {
		// Vanilla entities moved into per-mob sub-packages.
		string("!mc2105-pkg-wolf", true) { replace("net.minecraft.world.entity.animal.Wolf", "net.minecraft.world.entity.animal.wolf.Wolf") }
		string("!mc2105-pkg-sheep", true) { replace("net.minecraft.world.entity.animal.Sheep", "net.minecraft.world.entity.animal.sheep.Sheep") }

		// Entity#moveTo (every overload) and #absMoveTo were renamed to snapTo/absSnapTo — but ONLY
		// on Entity. PathNavigation#moveTo (168 of the mod's 199 call sites, all via getNavigation())
		// keeps its name, so a blanket `.moveTo(` rename is WRONG. Enumerate the Entity receivers
		// instead (every non-getNavigation() moveTo in the tree). `entity.moveTo(` as a substring of
		// `turtleentity.moveTo(` still yields the correct `turtleentity.snapTo(`, so overlap is benign.
		string("!mc2105-absmoveto", true) { replace("absMoveTo(", "absSnapTo(") }
		string("!mc2105-mt-this",   true) { replace("this.moveTo(", "this.snapTo(") }
		string("!mc2105-mt-tame",   true) { replace("this.tameable.moveTo(", "this.tameable.snapTo(") }
		string("!mc2105-mt-crow",   true) { replace("this.crow.moveTo(", "this.crow.snapTo(") }
		// NB: no super.moveTo rule — the tree's only `super.moveTo(` is AdvancedPathNavigate calling
		// PathNavigation#moveTo(Path,double), which KEEPS its name in 1.21.5. Renaming it broke the build.
		string("!mc2105-mt-whale",  true) { replace("whale.moveTo(", "whale.snapTo(") }
		string("!mc2105-mt-warden", true) { replace("warden.moveTo(", "warden.snapTo(") }
		string("!mc2105-mt-turtle", true) { replace("turtleentity.moveTo(", "turtleentity.snapTo(") }
		string("!mc2105-mt-teleport", true) { replace("teleportedEntity.moveTo(", "teleportedEntity.snapTo(") }
		string("!mc2105-mt-tadpole", true) { replace("tadpole.moveTo(", "tadpole.snapTo(") }
		string("!mc2105-mt-squid",  true) { replace("squid.moveTo(", "squid.snapTo(") }
		string("!mc2105-mt-lvt",    true) { replace("lvt_4_1_.moveTo(", "lvt_4_1_.snapTo(") }
		string("!mc2105-mt-fromtype", true) { replace("fromType.moveTo(", "fromType.snapTo(") }
		string("!mc2105-mt-entity", true) { replace("entity.moveTo(", "entity.snapTo(") }
		string("!mc2105-mt-croc",   true) { replace("croc.moveTo(", "croc.snapTo(") }
		string("!mc2105-mt-animal", true) { replace("animal.moveTo(", "animal.snapTo(") }

		// isInWaterOrBubble()/isInWaterRainOrBubble() folded into isInWater()/isInWaterOrRain().
		// Most call sites are bare (implicit this.), so no leading dot; the longer name is not a
		// superstring of the shorter (isInWater*Rain*OrBubble), so order is immaterial.
		string("!mc2105-waterrain", true) { replace("isInWaterRainOrBubble()", "isInWaterOrRain()") }
		string("!mc2105-water", true) { replace("isInWaterOrBubble()", "isInWater()") }

		// EntityDataSerializers.OPTIONAL_UUID was deleted (vanilla owner refs became EntityReference).
		// ~20 entities still sync plain Optional<UUID> fields; route them to a rebuilt+registered
		// serializer in AMCompat. Fully-qualified target so no import is needed at any site.
		string("!mc2105-optuuid", true) { replace("EntityDataSerializers.OPTIONAL_UUID", "com.github.alexthe666.alexsmobs.misc.AMCompat.OPTIONAL_UUID") }

		// SoundEvents.SHIELD_BREAK / ITEM_BREAK became Holder<SoundEvent> at 1.21.5 (most constants
		// are still bare SoundEvent). playSound wants the SoundEvent, so unwrap with .value() — only
		// these two constants error, and only when passed to a SoundEvent parameter.
		string("!mc2105-snd-shieldbreak", true) { replace("SoundEvents.SHIELD_BREAK,", "SoundEvents.SHIELD_BREAK.value(),") }
		string("!mc2105-snd-itembreak", true) { replace("SoundEvents.ITEM_BREAK,", "SoundEvents.ITEM_BREAK.value(),") }

		// MobEffects constants renamed at 1.21.5 (scoped by the MobEffects. prefix so Attributes.MOVEMENT_*
		// are untouched). NAUSEA<-CONFUSION, STRENGTH<-DAMAGE_BOOST, SLOWNESS<-MOVEMENT_SLOWDOWN, SPEED<-MOVEMENT_SPEED.
		string("!mc2105-eff-confusion", true) { replace("MobEffects.CONFUSION", "MobEffects.NAUSEA") }
		string("!mc2105-eff-damageboost", true) { replace("MobEffects.DAMAGE_BOOST", "MobEffects.STRENGTH") }
		string("!mc2105-eff-slowdown", true) { replace("MobEffects.MOVEMENT_SLOWDOWN", "MobEffects.SLOWNESS") }
		string("!mc2105-eff-speed", true) { replace("MobEffects.MOVEMENT_SPEED", "MobEffects.SPEED") }

		// Level#isDay() -> isBrightOutside(); Entity#isControlledByLocalInstance() -> isLocalInstanceAuthoritative().
		string("!mc2105-isday", true) { replace(".isDay()", ".isBrightOutside()") }
		string("!mc2105-localauth", true) { replace(".isControlledByLocalInstance()", ".isLocalInstanceAuthoritative()") }

		// LivingEntity#lastHurtByPlayerTime field renamed to lastHurtByPlayerMemoryTime.
		string("!mc2105-lasthurt", true) { replace("lastHurtByPlayerTime", "lastHurtByPlayerMemoryTime") }

		// 1.21.5 fixed the long-standing vanilla typo Shapes.blockOccudes -> blockOccludes.
		string("!mc2105-occludes", true) { replace(".blockOccudes(", ".blockOccludes(") }
		// 1.21.5 removed TagParser.parseTag(String); parseCompoundFully(String) is the drop-in
		// (returns CompoundTag, throws CommandSyntaxException — same callsite contract).
		string("!mc2105-parsetag", true) { replace("TagParser.parseTag(", "TagParser.parseCompoundFully(") }
		// NB: ArmorItem.Type -> equipment.ArmorType, the ArmorMaterial import move, and Tiers.IRON ->
		// ToolMaterial.IRON are already handled by the >=1.21.2 block above (which covers 1.21.5). At
		// 1.21.5 the ArmorItem/SwordItem/PickaxeItem CLASSES themselves are gone; the extends/ctor
		// changes (Properties#humanoidArmor/sword/pickaxe on a plain Item) are gated per file instead.
	}

	// ── MC 1.21.6: ValueInput / ValueOutput replace CompoundTag on every save/load hook ────────
	// The two interfaces keep 1.21.5's put*/get*Or method NAMES, so a body that only reads and
	// writes primitives compiles unchanged once the parameter's type flips — which is all these
	// rules do. Everything the interfaces DROPPED (nested tags, lists, contains, UUIDs, raw Tags)
	// goes through the AMCompat overload family, whose members share the CompoundTag versions'
	// names and arities so the call-site text is era-agnostic and needs no conditional at all.
	if (eval(current.version, ">=1.21.6")) replacements {
		// Entity#add/readAdditionalSaveData — ~100 overrides each. Keyed on the parameter TYPE, so
		// all three parameter names (compound / tag / p_...) are covered by one rule apiece, and a
		// call site can never match (it has no type in front of the argument). No `super.` rule is
		// needed either: the super call passes the same variable, whose type flipped with the decl.
		string("!mc2106-esave-decl", true) {
			replace("addAdditionalSaveData(CompoundTag ", "addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput ")
		}
		string("!mc2106-eload-decl", true) {
			replace("readAdditionalSaveData(CompoundTag ", "readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput ")
		}

		// BlockEntity#save/loadAdditional ALSO lost their HolderLookup.Provider parameter — the
		// ValueInput carries the registry context itself. Rather than rewrite the 12 bodies that
		// use `provider`, each rule re-declares it as a LOCAL on the same line, so the bodies are
		// untouched. Load reads it off the input; save has no input to read, so it goes through the
		// block entity's own level (AMCompat.lookupOf). Full signatures, longest match first.
		string("!mc2106-beload-decl-nbt", true) {
			replace(
				"loadAdditional(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider provider) {",
				"loadAdditional(net.minecraft.world.level.storage.ValueInput nbt) { net.minecraft.core.HolderLookup.Provider provider = nbt.lookup();",
			)
		}
		string("!mc2106-beload-decl-compound", true) {
			replace(
				"loadAdditional(CompoundTag compound, net.minecraft.core.HolderLookup.Provider provider) {",
				"loadAdditional(net.minecraft.world.level.storage.ValueInput compound) { net.minecraft.core.HolderLookup.Provider provider = compound.lookup();",
			)
		}
		string("!mc2106-beload-decl-tag", true) {
			replace(
				"loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {",
				"loadAdditional(net.minecraft.world.level.storage.ValueInput tag) { net.minecraft.core.HolderLookup.Provider provider = tag.lookup();",
			)
		}
		string("!mc2106-besave-decl-compound", true) {
			replace(
				"saveAdditional(CompoundTag compound, net.minecraft.core.HolderLookup.Provider provider) {",
				"saveAdditional(net.minecraft.world.level.storage.ValueOutput compound) { net.minecraft.core.HolderLookup.Provider provider = com.github.alexthe666.alexsmobs.misc.AMCompat.lookupOf(this);",
			)
		}
		string("!mc2106-besave-decl-tag", true) {
			replace(
				"saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {",
				"saveAdditional(net.minecraft.world.level.storage.ValueOutput tag) { net.minecraft.core.HolderLookup.Provider provider = com.github.alexthe666.alexsmobs.misc.AMCompat.lookupOf(this);",
			)
		}

		// …and the matching super calls drop the provider argument. Keyed on `super.`, which only
		// ever appears inside the override, so no call site can be hit.
		string("!mc2106-besuper-load-nbt", true) { replace("super.loadAdditional(nbt, provider);", "super.loadAdditional(nbt);") }
		string("!mc2106-besuper-load-compound", true) { replace("super.loadAdditional(compound, provider);", "super.loadAdditional(compound);") }
		string("!mc2106-besuper-load-tag", true) { replace("super.loadAdditional(tag, provider);", "super.loadAdditional(tag);") }
		string("!mc2106-besuper-save-compound", true) { replace("super.saveAdditional(compound, provider);", "super.saveAdditional(compound);") }
		string("!mc2106-besuper-save-tag", true) { replace("super.saveAdditional(tag, provider);", "super.saveAdditional(tag);") }

		// 1.21.6 renamed Mob's "restriction" (the leash-less home area a mob will not wander out of)
		// to "home" throughout. Pure renames — every use here is a call site, nothing overrides them.
		string("!mc2106-home-center", true) { replace("getRestrictCenter()", "getHomePosition()") }
		string("!mc2106-home-radius", true) { replace("getRestrictRadius()", "getHomeRadius()") }
		string("!mc2106-home-has", true) { replace("hasRestriction()", "hasHome()") }
		string("!mc2106-home-clear", true) { replace("clearRestriction()", "clearHome()") }
		string("!mc2106-home-within", true) { replace("isWithinRestriction(", "isWithinHome(") }
		string("!mc2106-home-set", true) { replace("restrictTo(", "setHomeTo(") }

		// 1.21.6 gave Entity#canBeCollidedWith the colliding entity. There are 9 overrides; the
		// decl rule is keyed on "() {" so it can never hit a call site (AMCompat.canBeCollidedWith
		// covers the single one), and the super rule passes the injected parameter through.
		string("!mc2106-collide-super", true) {
			replace("super.canBeCollidedWith()", "super.canBeCollidedWith(amCollider)")
		}
		string("!mc2106-collide-decl", true) {
			replace("canBeCollidedWith() {", "canBeCollidedWith(net.minecraft.world.entity.Entity amCollider) {")
		}

		// GuiGraphics#pose() is an org.joml.Matrix3x2fStack now (GUI transforms lost their depth),
		// so push/pop are named after the matrix rather than the pose. Only push/pop can be a blind
		// rename — translate/scale changed ARITY, so those go through AMRenderCompat.translateGui /
		// scaleGui at the call site. Keyed on `.pose().` so nothing else in the tree can match; all
		// `.pose().` usage is confined to the four GUI classes.
		string("!mc2106-gui-push", true) { replace(".pose().pushPose()", ".pose().pushMatrix()") }
		string("!mc2106-gui-pop", true) { replace(".pose().popPose()", ".pose().popMatrix()") }
	}

	// NeoForge moved TriState out of its own common.util into vanilla (net.minecraft.util) at 1.21.5.
	// Forge still ships net.minecraftforge.common.util.TriState, so this is NeoForge-only. The two
	// call sites (name-tag veto) are already inside //? if neoforge blocks; this only fixes the package.
	if (current.project.endsWith("-neoforge") && eval(current.version, ">=1.21.5")) replacements {
		string("!nf2105-tristate", true) { replace("net.neoforged.neoforge.common.util.TriState", "net.minecraft.util.TriState") }
	}

	// NeoForge 21.7 (MC 1.21.7) DROPPED the runtime member-stripping behaviour of @OnlyIn and added
	// OnlyInWarningsHandler, which puts a full-screen "Warning while loading mods" page in front of
	// every player of any mod that still carries the annotation. Absent through 21.6.20-beta,
	// present in 21.7.25-beta and every build after it (swept in the cached universal jars).
	// Upstream Alex's Mobs annotates 138 client members this way, so every NeoForge player from
	// 1.21.7 up met that screen on startup.
	//
	// Removing it is a behaviour no-op on exactly the versions that warn -- their own warning text
	// says the stripping "is no longer present" -- so the gate is the warning's own boundary and no
	// wider. Below it the annotation is kept, because there the stripping is real.
	//
	// Every one of the 138 sites is alone on its line, in both spellings (@OnlyIn(Dist.CLIENT) and
	// @OnlyIn(value = Dist.CLIENT)), so prefixing the annotation comments the whole line out. The
	// now-unused imports stay and are a javac warning, not an error.
	if (current.project.endsWith("-neoforge") && eval(current.version, ">=1.21.7")) replacements {
		string("!nf2107-onlyin", true) { replace("@OnlyIn(", "//@OnlyIn(") }
	}

	// Forge 1.20.5 moved two event fire helpers off ForgeHooks onto ForgeEventFactory.
	// NeoForge kept both on CommonHooks (its ForgeHooks), so this is Forge-only.
	if (current.project.endsWith("-forge") && eval(current.version, ">=1.20.5")) replacements {
		string("!fg205-knockback", true) { replace("common.ForgeHooks.onLivingKnockBack", "event.ForgeEventFactory.onLivingKnockBack") }
		string("!fg205-livingdrops", true) { replace("common.ForgeHooks.onLivingDrops", "event.ForgeEventFactory.onLivingDrops") }
	}

	// IGlobalLootModifier#apply gained the source LootTable as its first argument in 1.21.2 on
	// FORGE ONLY. NeoForge kept the 2-arg (generatedLoot, LootContext) shape, so this rule must not
	// touch NeoForge nodes. The four modifiers delegate straight to doApply and never touch the
	// table, so it is named and ignored. Keyed on the exact override signature (identical in all four).
	if (current.project.endsWith("-forge") && eval(current.version, ">=1.21.2")) replacements {
		string("!mc2102-lootmod-apply", true) {
			replace(
				"public ObjectArrayList<ItemStack> apply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {",
				"public ObjectArrayList<ItemStack> apply(net.minecraft.world.level.storage.loot.LootTable amLootTable, ObjectArrayList<ItemStack> generatedLoot, LootContext context) {",
			)
		}
	}

	// Forge 56.0.0 (MC 1.21.6) ships EventBus 7, a ground-up rewrite: the api classes moved into
	// sub-packages, EventPriority became a class of byte constants named Priority, Event.Result moved
	// to net.minecraftforge.common.util.Result, and events are posted through a per-event static BUS
	// field instead of MinecraftForge.EVENT_BUS (whose post() is simply gone).
	//
	// Only the pure text renames live here; everything with a shape change (cancellation via a
	// boolean-returning handler, MutableEvent bases, BUS.post call sites) is a source-level
	// Stonecutter gate, because the shape cannot be reached by a search-and-replace.
	//
	// NeoForge is unaffected — it has had its own bus API since 1.20.6, already mapped by !nf-eventbus.
	if (current.project.endsWith("-forge") && eval(current.version, ">=1.21.6")) replacements {
		string("!fg2106-eb-subscribe", true) {
			replace("net.minecraftforge.eventbus.api.SubscribeEvent", "net.minecraftforge.eventbus.api.listener.SubscribeEvent")
		}
		// Keyed on the trailing ';' so it can only ever hit the import, never a usage.
		string("!fg2106-eb-prio-import", true) {
			replace("net.minecraftforge.eventbus.api.EventPriority;", "net.minecraftforge.eventbus.api.listener.Priority;")
		}
		// A bare "EventPriority." -> "Priority." rule would be UNSAFE: these rules are reversible, and
		// the reverse applied to root src/ would turn EventPriority.LOWEST (which *contains*
		// "Priority.") into EventEventPriority.LOWEST. Keying on the full annotation text is safe
		// because it does not contain its own replacement. There are exactly two usages in the tree.
		string("!fg2106-eb-prio-high", true) { replace("priority = EventPriority.HIGH)", "priority = Priority.HIGH)") }
		string("!fg2106-eb-prio-lowest", true) { replace("priority = EventPriority.LOWEST)", "priority = Priority.LOWEST)") }
		string("!fg2106-eb-result", true) { replace("Event.Result.DENY", "net.minecraftforge.common.util.Result.DENY") }
	}

	// ── MC 1.21.9: the mechanical half of the submit-pipeline wave ─────────────────────────────
	// The two subsystem rewrites (particles: extract/submit; renderers: SubmitNodeCollector) are
	// source-level work; these are the pure renames that came with them.
	if (eval(current.version, ">=1.21.9")) replacements {
		// BlockBehaviour.Properties finally lost Mojang's double-s typo. Keyed on the literal "()"
		// so the reverse cannot touch the 14 CollisionGetter#noCollision(entity, aabb) call sites,
		// which always pass arguments.
		string("!mc2109-nocollision", true) { replace(".noCollission()", ".noCollision()") }

		// Entity#startRiding(Entity) became final; the override point is the new
		// startRiding(Entity, boolean force, boolean postGameEvent). Three part-entity classes
		// override it, all with the identical "refuse to ride minecarts and boats" body, so the
		// declaration and its super. call are rewritten in place. Call sites go through
		// AMCompat.startRiding instead — the new parameter is not in scope at most of them.
		string("!mc2109-startriding-decl", true) {
			replace("public boolean startRiding(Entity entityIn) {", "public boolean startRiding(Entity entityIn, boolean amForce, boolean amPostEvent) {")
		}
		string("!mc2109-startriding-super", true) {
			replace("super.startRiding(entityIn)", "super.startRiding(entityIn, amForce, amPostEvent)")
		}

		// The armour-stand layer set collapsed from INNER/OUTER pairs into one ArmorModelSet record
		// keyed by slot. This used to be a blanket rename to chest(), on the assumption that both
		// usages only wanted *a* humanoid layer — but a slot's mesh keeps ONLY that slot's parts from
		// 1.21.9 on, so the two helmet models it fed drew nothing (bug #58). It is now a per-slot
		// choice behind AMRenderCompat#armorStandArmorLayer, which needs no replacement rule.

		// ParticleProvider#createParticle gained a trailing RandomSource (the engine now hands the
		// factory the random it seeded the particle limit with, instead of each factory reaching for
		// level.random). 22 overrides, in three upstream parameter-naming variants — hence three
		// rules rather than one. Each is keyed on the whole final-parameter triple, which appears
		// nowhere else in the tree (verified: every match is a createParticle declaration).
		string("!mc2109-particle-factory-named", true) {
			replace("double xSpeed, double ySpeed, double zSpeed) {", "double xSpeed, double ySpeed, double zSpeed, net.minecraft.util.RandomSource amRandom) {")
		}
		string("!mc2109-particle-factory-p107", true) {
			replace("double p_107544_, double p_107545_, double p_107546_) {", "double p_107544_, double p_107545_, double p_107546_, net.minecraft.util.RandomSource amRandom) {")
		}
		string("!mc2109-particle-factory-p199", true) {
			replace("double p_199234_9_, double p_199234_11_, double p_199234_13_) {", "double p_199234_9_, double p_199234_11_, double p_199234_13_, net.minecraft.util.RandomSource amRandom) {")
		}

		// BlockEntityRenderer<T> gained a render-state type parameter and swapped render(...) for
		// submit(state, pose, collector, camera) — the same extract/submit split entity renderers got
		// in 1.21.2, and absorbed the same way: point the eight tile renderers' import at the compat
		// interface of the same simple name, which reconstructs the old call from the state.
		string("!mc2109-tile-import", true) {
			replace("import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;", "import com.github.alexthe666.alexsmobs.client.render.compat.BlockEntityRenderer;")
		}
		// …and shouldRenderOffScreen lost its block-entity argument in the same rewrite. Two spellings,
		// two rules; neither body reads the parameter. They carry no @Override, so without these the
		// method would silently stop being one.
		string("!mc2109-tile-offscreen-p112", true) {
			replace("public boolean shouldRenderOffScreen(T p_112306_) {", "public boolean shouldRenderOffScreen() {")
		}
		string("!mc2109-tile-offscreen-named", true) {
			replace("public boolean shouldRenderOffScreen(T entity) {", "public boolean shouldRenderOffScreen() {")
		}

		// Entity#lerpMotion takes one Vec3 now. Twelve overrides, all with identical bodies that use
		// only x/y/z — so the parameters are re-declared as locals on the same line and no body moves.
		// ELEVEN of the twelve carry no @Override (upstream never added one), which means without this
		// rule they would compile clean and silently stop overriding anything.
		string("!mc2109-lerpmotion-xyz", true) {
			replace("public void lerpMotion(double x, double y, double z) {", "public void lerpMotion(net.minecraft.world.phys.Vec3 amVec) { double x = amVec.x, y = amVec.y, z = amVec.z;")
		}
		string("!mc2109-lerpmotion-lerp", true) {
			replace("public void lerpMotion(double lerpX, double lerpY, double lerpZ) {", "public void lerpMotion(net.minecraft.world.phys.Vec3 amVec) { double lerpX = amVec.x, lerpY = amVec.y, lerpZ = amVec.z;")
		}

		// LivingEntity#shouldDropLoot gained the ServerLevel, like the rest of the death path did at
		// 1.21.2. Both live call sites sit inside EntityVoidWorm's >=1.21 arms, which already declare
		// `serverLevel`; the third is in the <1.21 arm and is commented out on these nodes.
		string("!mc2109-shoulddroploot", true) {
			replace("this.shouldDropLoot()", "this.shouldDropLoot(serverLevel)")
		}

		// Container#startOpen/stopOpen take a ContainerUser (a Player is one) instead of a Player.
		// Three overrides, no body reads the parameter. EntityKangaroo's is an anonymous-class
		// stopOpen with no @Override — same silent-dead-code trap as lerpMotion above.
		string("!mc2109-container-startopen", true) {
			replace("public void startOpen(Player player) {", "public void startOpen(net.minecraft.world.entity.ContainerUser player) {")
		}
		string("!mc2109-container-stopopen", true) {
			replace("public void stopOpen(Player player) {", "public void stopOpen(net.minecraft.world.entity.ContainerUser player) {")
		}

		// AbstractButton#onPress now receives the input that triggered it (modifier keys are readable
		// from the handler). One override, in ButtonTransmute, which just forwards to super.
		string("!mc2109-onpress-decl", true) {
			replace("public void onPress() {", "public void onPress(net.minecraft.client.input.InputWithModifiers amInput) {")
		}
		string("!mc2109-onpress-super", true) {
			replace("super.onPress()", "super.onPress(amInput)")
		}

		// EquipmentLayerRenderer#renderLayers went generic over the render state and joined the submit
		// pipeline: (LayerType, assetId, Model<? super S>, S state, ItemStack, PoseStack,
		// SubmitNodeCollector, int light, int outlineColor) — the state is inserted after the model,
		// the buffer source becomes the collector, and an outline colour is appended. Both call sites
		// already hold the neutral HumanoidRenderState they feed to setupAnim one line earlier, and
		// both are handed an AMSubmitBuffers by the compat layer, so the collector comes back out of
		// it. Outline colour 0 = none, matching AMSubmitBuffers' documented no-outline limitation.
		// Keyed on each call's own trailing text — the leading part is rewritten by the !mc2104-equip-*
		// rules earlier in this file, so it must not be matched on.
		//
		// The kangaroo layer makes this call three times (helmet, chestplate arms, chestplate body).
		// All three are written with the same `armorModel` / `armorState` local names precisely so
		// that one replacement covers them: a Stonecutter replace rewrites every occurrence, and
		// three identical strings invert identically, which is what keeps the rule reversible.
		string("!mc2109-equip-renderlayers-kangaroo", true) {
			replace(
				", armorModel, stack, poseStack, bufferSource, packedLight);",
				", armorModel, armorState, stack, poseStack, com.github.alexthe666.alexsmobs.client.render.compat.AMSubmitBuffers.collectorOf(bufferSource), packedLight, 0);"
			)
		}
		string("!mc2109-equip-renderlayers-mimicube", true) {
			replace(
				", defaultBipedModel, itemstack, matrixStackIn, bufferIn, clampedLight);",
				", defaultBipedModel, neutralArmorState, itemstack, matrixStackIn, com.github.alexthe666.alexsmobs.client.render.compat.AMSubmitBuffers.collectorOf(bufferIn), clampedLight, 0);"
			)
		}
	}

	// ── MC 1.21.11: the mechanical half ───────────────────────────────────────────────────────
	// Neither loader contributed a break this wave; all of it is vanilla. Three shapes: one
	// class-wide rename, one class split, and 37 plain package moves. Everything that needs a
	// real decision (game rules, the NeutralMob anger API, Camera, getCurrentDifficultyAt) is
	// source-level work in AMCompat and gated arms, not here.

	if (eval(current.version, ">=1.21.11")) replacements {
		// ResourceLocation was renamed Identifier, in the same package. 926 sites across 218
		// files, so a regex is the only sane tool — and the FIRST regex rule in this file, hence
		// the note: regex() takes an EXPLICIT reverse pattern, which is what makes a
		// word-boundary rename exact in both directions (a plain string rule cannot express one).
		//
		// Checked before writing, because a blanket identifier rename is unforgiving:
		//   · no string literal in the tree contains "ResourceLocation" (so nothing that is
		//     serialised or reflected on can move),
		//   · no member is *named* ResourceLocation — in particular the Tabula containers only
		//     ever use it as a TYPE. Their Gson-deserialised field names are the .tbl JSON keys
		//     and must never be renamed; \b keeps this rule off them and off
		//     ModelResourceLocation (whose sole occurrence is a comment),
		//   · the tree contains no standalone "Identifier" token, so the reverse is a no-op on
		//     root src/ rather than a corruption.
		regex("!mc2111-identifier", true) {
			replace("\\bResourceLocation\\b", "Identifier", "\\bIdentifier\\b", "ResourceLocation")
		}

		// RenderType split in two: the class is now just the render-layer type, and every static
		// factory moved to a new sibling class RenderTypes. (Both then moved package — that is
		// the !mc2111-pkg-rendertype rule below.)
		//
		// ⚠️ This wants to be ONE regex with a captured factory name. It cannot be:
		// Stonecutter's regex() splices a "$1" group reference out of the ORIGINAL text at the
		// ORIGINAL offset while writing into the already-shifted buffer, so as soon as the
		// replacement changes length every later group on the file comes back as garbage sliced
		// from the wrong place ("RenderTypes.t.renderer.()"). A group-FREE regex is fine — see
		// !mc2111-identifier — so the bug is specifically group references. One plain string()
		// rule per factory instead; the trailing "(" anchors each name, so entityCutout cannot
		// eat entityCutoutNoCull and no ordering is required.
		//
		// The rules only ever see the QUALIFIED spelling because AMRenderTypes.java was edited
		// to qualify the 30 factory calls it used to inherit statically from its own superclass
		// (it `extends RenderType`). That inheritance is exactly what the split breaks — and
		// silently, since RenderType still exists — so relying on it any longer is a trap.
		string("!mc2111-rt-armorcutoutnocull", true) { replace("RenderType.armorCutoutNoCull(", "net.minecraft.client.renderer.rendertype.RenderTypes.armorCutoutNoCull(") }
		string("!mc2111-rt-endportal", true) { replace("RenderType.endPortal(", "net.minecraft.client.renderer.rendertype.RenderTypes.endPortal(") }
		// These two, and itemEntityTranslucentCull below, are ALSO renamed at 26.1 — and two of them
		// swap meanings with each other. Rules do not chain, so on >=26 they stand down in favour of
		// the !mc26-rt-* rules, which do both hops at once.
		if (!mc26) string("!mc2111-rt-entitycutout", true) { replace("RenderType.entityCutout(", "net.minecraft.client.renderer.rendertype.RenderTypes.entityCutout(") }
		if (!mc26) string("!mc2111-rt-entitycutoutnocull", true) { replace("RenderType.entityCutoutNoCull(", "net.minecraft.client.renderer.rendertype.RenderTypes.entityCutoutNoCull(") }
		string("!mc2111-rt-entityglint", true) { replace("RenderType.entityGlint(", "net.minecraft.client.renderer.rendertype.RenderTypes.entityGlint(") }
		string("!mc2111-rt-entitytranslucent", true) { replace("RenderType.entityTranslucent(", "net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(") }
		string("!mc2111-rt-entitytranslucentemissive", true) { replace("RenderType.entityTranslucentEmissive(", "net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucentEmissive(") }
		string("!mc2111-rt-eyes", true) { replace("RenderType.eyes(", "net.minecraft.client.renderer.rendertype.RenderTypes.eyes(") }
		if (!mc26) string("!mc2111-rt-itementitytranslucentcull", true) { replace("RenderType.itemEntityTranslucentCull(", "net.minecraft.client.renderer.rendertype.RenderTypes.itemEntityTranslucentCull(") }
		string("!mc2111-rt-leash", true) { replace("RenderType.leash(", "net.minecraft.client.renderer.rendertype.RenderTypes.leash(") }
		string("!mc2111-rt-outline", true) { replace("RenderType.outline(", "net.minecraft.client.renderer.rendertype.RenderTypes.outline(") }
		string("!mc2111-rt-text", true) { replace("RenderType.text(", "net.minecraft.client.renderer.rendertype.RenderTypes.text(") }

		// 37 pure package moves. Verified class-by-class against 1.21.11's own mappings — each
		// destination exists and each source is gone — and checked for prefix collisions against
		// every net.minecraft FQN the tree names (zero: nothing here is a prefix of a longer type,
		// which is what would otherwise turn monster.Zombie into monster.zombie.ZombieVillager).
		string("!mc2111-pkg-blockutil", true) { replace("net.minecraft.BlockUtil", "net.minecraft.util.BlockUtil") }
		string("!mc2111-pkg-util", true) { replace("net.minecraft.Util", "net.minecraft.util.Util") }
		string("!mc2111-pkg-llamaspitmodel", true) { replace("net.minecraft.client.model.LlamaSpitModel", "net.minecraft.client.model.animal.llama.LlamaSpitModel") }
		string("!mc2111-pkg-villagermodel", true) { replace("net.minecraft.client.model.VillagerModel", "net.minecraft.client.model.npc.VillagerModel") }
		// The trailing ";" is deliberate. All 76 occurrences are import lines, and without it this
		// rule's REVERSE ("…rendertype.RenderType" -> "…renderer.RenderType") would prefix-match
		// the "…rendertype.RenderTypes." the !mc2111-rt-* rules emit and shear the "s." off.
		string("!mc2111-pkg-rendertype", true) { replace("net.minecraft.client.renderer.RenderType;", "net.minecraft.client.renderer.rendertype.RenderType;") }
		string("!mc2111-pkg-abstractfish", true) { replace("net.minecraft.world.entity.animal.AbstractFish", "net.minecraft.world.entity.animal.fish.AbstractFish") }
		string("!mc2111-pkg-abstractschoolingfish", true) { replace("net.minecraft.world.entity.animal.AbstractSchoolingFish", "net.minecraft.world.entity.animal.fish.AbstractSchoolingFish") }
		string("!mc2111-pkg-bee", true) { replace("net.minecraft.world.entity.animal.Bee", "net.minecraft.world.entity.animal.bee.Bee") }
		string("!mc2111-pkg-cat", true) { replace("net.minecraft.world.entity.animal.Cat", "net.minecraft.world.entity.animal.feline.Cat") }
		string("!mc2111-pkg-chicken", true) { replace("net.minecraft.world.entity.animal.Chicken", "net.minecraft.world.entity.animal.chicken.Chicken") }
		string("!mc2111-pkg-dolphin", true) { replace("net.minecraft.world.entity.animal.Dolphin", "net.minecraft.world.entity.animal.dolphin.Dolphin") }
		string("!mc2111-pkg-fox", true) { replace("net.minecraft.world.entity.animal.Fox", "net.minecraft.world.entity.animal.fox.Fox") }
		string("!mc2111-pkg-irongolem", true) { replace("net.minecraft.world.entity.animal.IronGolem", "net.minecraft.world.entity.animal.golem.IronGolem") }
		string("!mc2111-pkg-ocelot", true) { replace("net.minecraft.world.entity.animal.Ocelot", "net.minecraft.world.entity.animal.feline.Ocelot") }
		string("!mc2111-pkg-pufferfish", true) { replace("net.minecraft.world.entity.animal.Pufferfish", "net.minecraft.world.entity.animal.fish.Pufferfish") }
		string("!mc2111-pkg-rabbit", true) { replace("net.minecraft.world.entity.animal.Rabbit", "net.minecraft.world.entity.animal.rabbit.Rabbit") }
		string("!mc2111-pkg-squid", true) { replace("net.minecraft.world.entity.animal.Squid", "net.minecraft.world.entity.animal.squid.Squid") }
		string("!mc2111-pkg-turtle", true) { replace("net.minecraft.world.entity.animal.Turtle", "net.minecraft.world.entity.animal.turtle.Turtle") }
		string("!mc2111-pkg-wateranimal", true) { replace("net.minecraft.world.entity.animal.WaterAnimal", "net.minecraft.world.entity.animal.fish.WaterAnimal") }
		string("!mc2111-pkg-paintingvariant", true) { replace("net.minecraft.world.entity.decoration.PaintingVariant", "net.minecraft.world.entity.decoration.painting.PaintingVariant") }
		string("!mc2111-pkg-abstractskeleton", true) { replace("net.minecraft.world.entity.monster.AbstractSkeleton", "net.minecraft.world.entity.monster.skeleton.AbstractSkeleton") }
		string("!mc2111-pkg-drowned", true) { replace("net.minecraft.world.entity.monster.Drowned", "net.minecraft.world.entity.monster.zombie.Drowned") }
		string("!mc2111-pkg-pillager", true) { replace("net.minecraft.world.entity.monster.Pillager", "net.minecraft.world.entity.monster.illager.Pillager") }
		string("!mc2111-pkg-spider", true) { replace("net.minecraft.world.entity.monster.Spider", "net.minecraft.world.entity.monster.spider.Spider") }
		string("!mc2111-pkg-witherskeleton", true) { replace("net.minecraft.world.entity.monster.WitherSkeleton", "net.minecraft.world.entity.monster.skeleton.WitherSkeleton") }
		string("!mc2111-pkg-zombie", true) { replace("net.minecraft.world.entity.monster.Zombie", "net.minecraft.world.entity.monster.zombie.Zombie") }
		string("!mc2111-pkg-abstractvillager", true) { replace("net.minecraft.world.entity.npc.AbstractVillager", "net.minecraft.world.entity.npc.villager.AbstractVillager") }
		string("!mc2111-pkg-villagerprofession", true) { replace("net.minecraft.world.entity.npc.VillagerProfession", "net.minecraft.world.entity.npc.villager.VillagerProfession") }
		// Moved again at 26.1 (to world.item.trading) — see !mc26-pkg-villagertrades.
		if (!mc26) string("!mc2111-pkg-villagertrades", true) { replace("net.minecraft.world.entity.npc.VillagerTrades", "net.minecraft.world.entity.npc.villager.VillagerTrades") }
		string("!mc2111-pkg-wanderingtrader", true) { replace("net.minecraft.world.entity.npc.WanderingTrader", "net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader") }
		string("!mc2111-pkg-abstractarrow", true) { replace("net.minecraft.world.entity.projectile.AbstractArrow", "net.minecraft.world.entity.projectile.arrow.AbstractArrow") }
		string("!mc2111-pkg-arrow", true) { replace("net.minecraft.world.entity.projectile.Arrow", "net.minecraft.world.entity.projectile.arrow.Arrow") }
		string("!mc2111-pkg-throwableitemprojectile", true) { replace("net.minecraft.world.entity.projectile.ThrowableItemProjectile", "net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile") }
		string("!mc2111-pkg-throwntrident", true) { replace("net.minecraft.world.entity.projectile.ThrownTrident", "net.minecraft.world.entity.projectile.arrow.ThrownTrident") }
		string("!mc2111-pkg-abstractminecart", true) { replace("net.minecraft.world.entity.vehicle.AbstractMinecart", "net.minecraft.world.entity.vehicle.minecart.AbstractMinecart") }
		string("!mc2111-pkg-boat", true) { replace("net.minecraft.world.entity.vehicle.Boat", "net.minecraft.world.entity.vehicle.boat.Boat") }
		string("!mc2111-pkg-gamerules", true) { replace("net.minecraft.world.level.GameRules", "net.minecraft.world.level.gamerules.GameRules") }
		string("!mc2111-pkg-polarbear", true) { replace("net.minecraft.world.entity.animal.PolarBear", "net.minecraft.world.entity.animal.polarbear.PolarBear") }
		// The advancement predicate package lost its long-standing typo: critereon -> criterion.
		// …and 26.2 then split it in two (triggers / predicates), so there this rule stands down
		// and the per-class !mc262-adv-* rules take the whole hop — `criterion` does not exist there.
		if (!mc262) string("!mc2111-pkg-critereon", true) { replace("net.minecraft.advancements.critereon.", "net.minecraft.advancements.criterion.") }

		// The RenderTypes split again, this time reached by METHOD REFERENCE, which the "(" -anchored
		// rules above cannot see. Only one factory is used that way (three sites).
		string("!mc2111-rtref-entitycutoutnocull", true) { replace("RenderType::entityCutoutNoCull", "net.minecraft.client.renderer.rendertype.RenderTypes::entityCutoutNoCull") }

		// Entity.hasImpulse -> Entity.needsSync. All 25 sites in the tree are the identical statement
		// "<expr>.hasImpulse = true;", so one rule keyed on the whole assignment is exact — and its
		// reverse cannot touch root src/, which contains no "needsSync" token at all.
		string("!mc2111-needssync", true) { replace(".hasImpulse = true;", ".needsSync = true;") }

		// ResourceKey.location() -> identifier(). NOT a blanket rule: TagKey.location() survives
		// unchanged (SpawnBiomeData uses it), so each ResourceKey site is named individually.
		string("!mc2111-rk-ref", true) { replace("ResourceKey::location", "ResourceKey::identifier") }
		string("!mc2111-rk-exitdim", true) { replace("this.exitDimension.location()", "this.exitDimension.identifier()") }
		string("!mc2111-rk-dimension", true) { replace(".dimension().location()", ".dimension().identifier()") }
		string("!mc2111-rk-local", true) { replace("resourceKey.location()", "resourceKey.identifier()") }
		string("!mc2111-rk-mending", true) { replace("Enchantments.MENDING.location()", "Enchantments.MENDING.identifier()") }

		// Camera dropped its get- prefixes. Also not blanket-able: 1.21.10's Camera already declares
		// BOTH getPosition() and position(), so a bare rename would be a silent no-op there and could
		// not be reversed. "live" is the local the compat camera builder uses and appears nowhere else.
		// (26.2 renames getMainCamera itself, so there this rule stands down for !mc262-cam-mainpos.)
		if (!mc262) string("!mc2111-cam-mainpos", true) { replace("getMainCamera().getPosition()", "getMainCamera().position()") }
		string("!mc2111-cam-blockpos", true) { replace("live.getBlockPosition()", "live.blockPosition()") }
		string("!mc2111-cam-pos", true) { replace("live.getPosition()", "live.position()") }
		string("!mc2111-cam-entity", true) { replace("live.getEntity()", "live.entity()") }

		string("!mc2111-armpose-spear", true) { replace("ArmPose.THROW_SPEAR", "ArmPose.SPEAR") }
		// …and the unqualified form inside ModelUnderminerDwarf's two switches over ArmPose.
		string("!mc2111-armpose-spear-case", true) { replace("case THROW_SPEAR:", "case SPEAR:") }

		// VillagerTrades.ItemListing#getOffer gained a leading ServerLevel. Neither implementation
		// here reads it, and nothing in the mod calls getOffer, so a declaration-only rewrite is
		// enough. Keyed on the whole leading signature so it cannot hit anything else.
		string("!mc2111-itemlisting-getoffer", true) {
			replace("public MerchantOffer getOffer(Entity ", "public MerchantOffer getOffer(net.minecraft.server.level.ServerLevel amLevel, Entity ")
		}

		// EntityRenderState lost its hitboxesRenderState field (hitbox rendering moved out of the
		// per-entity state). The site sits inside AMRenderCompat's >=1.21.9 arm and Stonecutter
		// blocks are siblings, never nested, so it cannot be gated in place.
		string("!mc2111-hitboxstate", true) {
			// A line comment, never a block one: the arm is still /* */-commented when replacements
			// run, and a nested /* … */ would close the outer comment early.
			replace("state.hitboxesRenderState = null;", "// hitboxesRenderState was removed in 1.21.11")
		}

		// AbstractButton.renderWidget is final now and delegates to a new abstract renderContents
		// with the identical parameter list, so the four buttons here just change the name they
		// declare (and the one that calls super).
		// Renamed once more at 26.1 (renderContents -> extractContents), so >=26 takes the whole hop
		// in !mc26-extractcontents-*.
		if (!mc26) string("!mc2111-renderwidget-decl", true) { replace("void renderWidget(GuiGraphics", "void renderContents(GuiGraphics") }
		if (!mc26) string("!mc2111-renderwidget-super", true) { replace("super.renderWidget(", "super.renderContents(") }

		// Vanilla Bee's anger API moved to absolute end times; these three sites read/write it on a
		// vanilla Bee (the mod's own NeutralMobs keep the old accessors — see the gated arms there).
		string("!mc2111-bee-anger-set", true) { replace("bee.setRemainingPersistentAngerTime(100)", "bee.setTimeToRemainAngry(100)") }
		string("!mc2111-bee-anger-pred", true) { replace("p_apply_1_.getRemainingPersistentAngerTime() > 0", "p_apply_1_.isAngry()") }
		string("!mc2111-bee-anger-tick", true) { replace("closestLivingEntity.getRemainingPersistentAngerTime() <= 0", "!closestLivingEntity.isAngry()") }
	}

	// ── MC 26.1: the mechanical half ──────────────────────────────────────────────────────────
	// 26.x ships unobfuscated and reorganised heavily. Everything here is a rename, a package
	// move or a call-site redirect into AMCompat; the real decisions (the GUI extract/submit
	// split, the classes that have no successor at all) are source-level gates.
	//
	// ⚠️ These rules run AFTER the >=1.21.11 group on the same file, so they see that group's
	// output — the RenderType factories are already spelled `…rendertype.RenderTypes.x(` and the
	// button hook is already `renderContents`. Key on that, not on the 1.20.1 spelling.
	// ── Forge 26 renames ────────────────────────────────────────────────────────
	// Forge 64 finally adopted three renames NeoForge made back in 1.21, plus one of its own.
	// All pure renames; anything that changed a signature or an event's shape gets a source-level
	// //? if forge && >=26 conditional at the call site instead.
	if (current.project.endsWith("-forge") && eval(current.version, ">=26")) replacements {
		string("!fg26-shears", true) { replace("Tags.Items.SHEARS", "Tags.Items.TOOLS_SHEAR") }
		string("!fg26-addedtolevel-is", true) { replace("isAddedToWorld()", "isAddedToLevel()") }
		string("!fg26-addedtolevel-on", true) { replace("onAddedToWorld()", "onAddedToLevel()") }
		// Forge 64 deleted PlayerInteractEvent.EntityInteract outright: Player#interactOn now fires
		// only EntityInteractSpecific, at the same hook point and before Entity#interact, so it is
		// the faithful successor for a handler that only reads getTarget()/getItemStack() and
		// cancels the whole interaction. Bytecode-verified in the patched Player.interactOn.
		string("!fg26-entityinteract", true) {
			replace("PlayerInteractEvent.EntityInteract ", "PlayerInteractEvent.EntityInteractSpecific ")
		}

		// ── @OnlyIn on a CLASS is fatal on Forge 64 too ─────────────────────────────────────
		// The !mc26-onlyin-member rule below covers members; this is the SAME throw from the SAME
		// method, and it is NOT a wrong-dist check. RuntimeDistCleaner#processClassWithFlags first
		// removes any @OnlyIn naming the *other* dist (that is the familiar "invalid dist" error),
		// and then refuses ANY @OnlyIn that survives on a non-vanilla class — so a client class
		// annotated @OnlyIn(Dist.CLIENT) dies on the CLIENT. That is exactly what failed the
		// 26.1.2-forge client gate, out of AlexsMobs.<clinit>'s PROXY supplier:
		//   UnsupportedOperationException: Mod class …/ClientProxy is annotated with @OnlyIn,
		//   this is no longer supported as it slowed down startup times
		// So on Forge >=26 the annotation cannot block a mod class either — the loader throws
		// instead of blocking — and dropping it loses nothing. Gated to Forge: NeoForge 26 still
		// honours class-level @OnlyIn in dev (NeoForgeDevDistCleaner) and merely logs a warning,
		// and that node is already gate-green, so there is nothing to buy by churning it.
		// The tree spells every class-level site `@OnlyIn(Dist.CLIENT)` and every member-level one
		// `@OnlyIn(value = Dist.CLIENT)` (see below), so the two rules cannot collide, and neither
		// can match the other's text. Reverse is harmless — root src/ has no `//@OnlyIn` token.
		string("!fg26-onlyin-class", true) {
			replace("@OnlyIn(Dist.CLIENT)", "//@OnlyIn(Dist.CLIENT)")
		}
	}

	if (eval(current.version, ">=26")) replacements {
		// ── @OnlyIn on a MEMBER is fatal on Forge 64 ────────────────────────────────────────
		// Forge 64 (MC >=26) hard-throws from RuntimeDistCleaner for a method/constructor/field
		// annotated @OnlyIn inside a mod class ("this is no longer supported as it slowed down
		// startup times") and refuses to load the mod. NeoForge 26 only logs an ERROR-level
		// warning for the same thing (OnlyInWarningsHandler). Either way the annotation is INERT
		// on >=26 — neither loader strips members any more — so deleting it there is faithful.
		//
		// CLASS-level @OnlyIn is kept HERE, i.e. on NeoForge 26, where RuntimeDistCleaner's
		// counterpart still uses it to block client classes on a dedicated dev server (that is
		// where this project's benign /ERROR] lines come from). On Forge >=26 it is fatal for the
		// same reason a member is, and the !fg26-onlyin-class rule above comments it out.
		//
		// Class and member sites are textually identical, so the tree spells the 77 member-level
		// ones `@OnlyIn(value = Dist.CLIENT)` — the standard explicit form, semantically identical
		// on <26 — purely so this rule can single them out. A line gate could not: 3 of them sit
		// INSIDE a Stonecutter block, and blocks are siblings, never nested. Commenting the line
		// out (rather than deleting it) keeps the rule reversible and harmless in reverse: root
		// src/ contains no `//@OnlyIn` token.
		string("!mc26-onlyin-member", true) {
			replace("@OnlyIn(value = Dist.CLIENT)", "//@OnlyIn(value = Dist.CLIENT)")
		}

		// AbstractButton's abstract render hook changed name again: renderWidget (<=1.21.10) →
		// renderContents (1.21.11) → extractContents (26.1). Same parameter list each time.
		// Keyed on the 1.20.1 spelling and carrying the parameter type across in the same rule —
		// the !mc2111-renderwidget-* pair stands down on this node, and !mc26-guigraphics below
		// cannot reach a region this rule has already rewritten.
		string("!mc26-extractcontents-decl", true) { replace("void renderWidget(GuiGraphics", "void extractContents(GuiGraphicsExtractor") }
		string("!mc26-extractcontents-super", true) { replace("super.renderWidget(", "super.extractContents(") }

		// 26.1's GUI is an extract/submit split all the way up: Screen#render became
		// extractRenderState, Screen#renderBackground became extractBackground, and
		// AbstractContainerScreen#renderLabels became extractLabels. Same parameter lists.
		// Each decl rule carries the parameter type across itself, because !mc26-guigraphics
		// below cannot reach a region an earlier rule has already claimed (rules do not chain).
		// The `super.`/`this.` receiver anchors the call-site rules so no same-named mod method
		// (renderBg, renderLabels on a non-screen, …) can be hit.
		string("!mc26-gui-render-decl", true) { replace("void render(GuiGraphics ", "void extractRenderState(GuiGraphicsExtractor ") }
		string("!mc26-gui-render-super", true) { replace("super.render(guiGraphics", "super.extractRenderState(guiGraphics") }
		string("!mc26-gui-bg-decl", true) { replace("void renderBackground(GuiGraphics ", "void extractBackground(GuiGraphicsExtractor ") }
		string("!mc26-gui-bg-super", true) { replace("super.renderBackground(guiGraphics", "super.extractBackground(guiGraphics") }
		string("!mc26-gui-bg-this", true) { replace("this.renderBackground(guiGraphics", "this.extractBackground(guiGraphics") }
		string("!mc26-gui-labels-decl", true) { replace("void renderLabels(GuiGraphics ", "void extractLabels(GuiGraphicsExtractor ") }

		// GuiGraphics was renamed GuiGraphicsExtractor, in the same package — a screen no longer
		// draws, it extracts a render state that the GUI renderer replays. \b keeps the reverse
		// off the new name's own prefix, and root src/ contains no "GuiGraphicsExtractor" token,
		// so the reverse is a no-op there rather than a corruption.
		regex("!mc26-guigraphics", true) {
			replace("\\bGuiGraphics\\b", "GuiGraphicsExtractor", "\\bGuiGraphicsExtractor\\b", "GuiGraphics")
		}

		// …and its draw methods were renamed to match the extract vocabulary. Every call site in
		// the tree names its receiver `guiGraphics`, which is what anchors these (a bare
		// `.renderItem(` would also hit ItemInHandRenderer#renderItem in AMRenderCompat).
		string("!mc26-gui-drawstring", true) { replace("guiGraphics.drawString(", "guiGraphics.text(") }
		string("!mc26-gui-renderitem", true) { replace("guiGraphics.renderItem(", "guiGraphics.item(") }
		string("!mc26-gui-rendertooltip", true) { replace("guiGraphics.renderTooltip(", "guiGraphics.setTooltipForNextFrame(") }
		string("!mc26-gui-submitentity", true) { replace("guiGraphics.submitEntityRenderState(", "guiGraphics.entity(") }

		// ⚠️ THE RENDER-TYPE SWAP — the single most dangerous rule in this file.
		// 26.1 did not just rename these factories, it SWAPPED two of them: what 1.21.11 calls
		// entityCutout (culled) is now entityCutoutCull, and what it calls entityCutoutNoCull is
		// now plain entityCutout. So the names survive but MEAN THE OPPOSITE THING, and a
		// one-rule "fix" silently flips the backface culling of every mob in the mod.
		//
		// Each keys on the 1.20.1 spelling and emits the final FQN, so the three matching
		// !mc2111-rt-* rules stand down (rules do not chain) and the swap can never be applied
		// twice. Verify after any edit here by grepping the GENERATED sources — the two names
		// still exist in both eras, so getting it wrong flips backface culling silently.
		string("!mc26-rt-entitycutout-cull", true) { replace("RenderType.entityCutout(", "net.minecraft.client.renderer.rendertype.RenderTypes.entityCutoutCull(") }
		string("!mc26-rt-entitycutout-nocull", true) { replace("RenderType.entityCutoutNoCull(", "net.minecraft.client.renderer.rendertype.RenderTypes.entityCutout(") }
		string("!mc26-rt-itementitytranslucentcull", true) { replace("RenderType.itemEntityTranslucentCull(", "net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucentCullItemTarget(") }
		// …and the one method-reference spelling, which has no trailing `(` for the rules above to
		// match (client/render/compat/EntityModel.java).
		string("!mc26-rt-entitycutout-nocull-ref", true) { replace("RenderType::entityCutoutNoCull", "net.minecraft.client.renderer.rendertype.RenderTypes::entityCutout") }

		// FollowBoatGoal was generalised to any player-ridden entity (vanilla's Dolphin now adds it
		// twice, for boats and nautiluses). Passing AbstractBoat.class reproduces the old behaviour
		// exactly. FQN because none of the four call sites imports the boat class.
		string("!mc26-followboat", true) {
			replace("new FollowBoatGoal(this)", "new FollowPlayerRiddenEntityGoal(this, net.minecraft.world.entity.vehicle.boat.AbstractBoat.class)")
		}

		// PathType's two fire constants were renamed to say what they mean (the "danger" one is
		// the neighbour-of-fire cost, the "damage" one is fire itself).
		//
		// ⚠️ Keyed on the CONSTANT ONLY, not on `PathType.DANGER_FIRE` — rules do not chain, and
		// the source spelling here is `BlockPathTypes.DANGER_FIRE`, whose `BlockPathTypes` token
		// the >=1.20.5 rule !mc205-pathtype-enum has already claimed. A rule spanning the claimed
		// offset never matches. `.FIRE,` / `.FIRE ` have zero occurrences in root src, so the
		// reverse is a no-op.
		string("!mc26-pathtype-dangerfire-c", true) { replace(".DANGER_FIRE,", ".FIRE_IN_NEIGHBOR,") }
		string("!mc26-pathtype-dangerfire-s", true) { replace(".DANGER_FIRE ", ".FIRE_IN_NEIGHBOR ") }
		string("!mc26-pathtype-damagefire-c", true) { replace(".DAMAGE_FIRE,", ".FIRE,") }
		string("!mc26-pathtype-damagefire-s", true) { replace(".DAMAGE_FIRE ", ".FIRE ") }

		// Player#displayClientMessage split into sendSystemMessage / sendOverlayMessage. All 19
		// sites name their receiver `player`; AMCompat picks the half from the boolean.
		string("!mc26-displayclientmessage", true) {
			replace("player.displayClientMessage(", "com.github.alexthe666.alexsmobs.misc.AMCompat.displayClientMessage(player, ")
		}

		// Cat and cow sounds moved behind per-variant sound sets.
		string("!mc26-cat-eat", true) { replace("SoundEvents.CAT_EAT", "com.github.alexthe666.alexsmobs.misc.AMCompat.catEatSound()") }
		string("!mc26-cow-step", true) { replace("SoundEvents.COW_STEP", "com.github.alexthe666.alexsmobs.misc.AMCompat.cowStepSound()") }

		// ItemParticleOption carries an ItemStackTemplate now, not an ItemStack. Safe despite the
		// nested parentheses in every call's argument — only the opening text is rewritten.
		string("!mc26-itemparticle", true) {
			replace("new ItemParticleOption(ParticleTypes.ITEM, ", "com.github.alexthe666.alexsmobs.misc.AMCompat.itemParticle(ParticleTypes.ITEM, ")
		}

		// ── package moves ────────────────────────────────────────────────────────────────────
		string("!mc26-pkg-camerastate", true) { replace("net.minecraft.client.renderer.state.CameraRenderState", "net.minecraft.client.renderer.state.level.CameraRenderState") }
		string("!mc26-pkg-skystate", true) { replace("net.minecraft.client.renderer.state.SkyRenderState", "net.minecraft.client.renderer.state.level.SkyRenderState") }
		// Straight from the 1.20.1 spelling — !mc2111-pkg-villagertrades stands down here.
		string("!mc26-pkg-villagertrades", true) { replace("net.minecraft.world.entity.npc.VillagerTrades", "net.minecraft.world.item.trading.VillagerTrades") }

		// ── classes renamed outright ─────────────────────────────────────────────────────────
		// The saved-data store dropped its "dimension" framing.
		string("!mc26-datastorage", true) { replace("DimensionDataStorage", "SavedDataStorage") }

		// LightTexture's packing helpers moved to a plain util class in a different package, so
		// the import and the use site need separate rules. Only pack() is used here.
		string("!mc26-lighttexture-import", true) { replace("import net.minecraft.client.renderer.LightTexture;", "import net.minecraft.util.LightCoordsUtil;") }
		string("!mc26-lighttexture-pack", true) { replace("LightTexture.pack(", "LightCoordsUtil.pack(") }

		// The immediate-mode block renderer became a model *resolver*.
		string("!mc26-pkg-blockrenderdispatcher", true) { replace("net.minecraft.client.renderer.block.BlockRenderDispatcher", "net.minecraft.client.renderer.block.BlockModelResolver") }
		string("!mc26-blockrenderdispatcher-type", true) { replace("BlockRenderDispatcher blockrendererdispatcher", "BlockModelResolver blockrendererdispatcher") }
		string("!mc26-getblockrenderer", true) { replace("Minecraft.getInstance().getBlockRenderer()", "Minecraft.getInstance().getBlockModelResolver()") }

		// The glint/foil buffer helper moved off ItemRenderer (which is gone) onto the new
		// feature-renderer for items.
		string("!mc26-pkg-itemfoil-import", true) { replace("import net.minecraft.client.renderer.entity.ItemRenderer;", "import net.minecraft.client.renderer.feature.ItemFeatureRenderer;") }
		string("!mc26-itemfoil", true) { replace("ItemRenderer.getFoilBuffer(", "ItemFeatureRenderer.getFoilBuffer(") }

		// ── ChunkPos became a record ─────────────────────────────────────────────────────────
		// Field reads become accessor calls, asLong is pack, and the BlockPos constructor is the
		// static `containing`. Each field rule is anchored on its whole call so no Vec3 `.x` can
		// match.
		string("!mc26-chunkpos-aslong", true) { replace("ChunkPos.asLong(", "ChunkPos.pack(") }
		string("!mc26-chunkpos-forced", true) { replace("setChunkForced(pos.x, pos.z,", "setChunkForced(pos.x(), pos.z(),") }
		string("!mc26-chunkpos-getchunknow", true) { replace("getChunkNow(chunkPos.x, chunkPos.z)", "getChunkNow(chunkPos.x(), chunkPos.z())") }
		string("!mc26-chunkpos-isloaded", true) { replace("isChunkLoaded(world, pos.x, pos.z)", "isChunkLoaded(world, pos.x(), pos.z())") }
		string("!mc26-chunkpos-pupfishx", true) { replace("this.pupfishChunk.x)", "this.pupfishChunk.x())") }
		string("!mc26-chunkpos-pupfishz", true) { replace("this.pupfishChunk.z)", "this.pupfishChunk.z())") }
		string("!mc26-chunkpos-ctor-eagle", true) { replace("new ChunkPos(this.blockPosition().offset(", "ChunkPos.containing(this.blockPosition().offset(") }
		string("!mc26-chunkpos-ctor-endpoint", true) { replace("new ChunkPos(endpoint)", "ChunkPos.containing(endpoint)") }
		string("!mc26-chunkpos-ctor-trader", true) { replace("new ChunkPos(trader.blockPosition())", "ChunkPos.containing(trader.blockPosition())") }
		string("!mc26-chunkpos-echox", true) { replace("getPupfishChunk().x,", "getPupfishChunk().x(),") }
		string("!mc26-chunkpos-echoz", true) { replace("getPupfishChunk().z)", "getPupfishChunk().z())") }

		// ── "light colour" is "light coords" everywhere now ──────────────────────────────────
		// Blanket, deliberately: it renames the two private helpers in RenderMurmurHead /
		// RenderTendonSegment as well as the vanilla overrides and LevelRenderer's static, which
		// keeps declaration and call sites consistent. `getLightCoords(` does not occur in root
		// src, so the reverse is a no-op.
		// (26.2 also moves LevelRenderer's static off the class, so there this rule stands down for
		// the !mc262-lightcoords pair — the specific one first, then this same blanket.)
		if (!mc262) string("!mc26-lightcoords", true) { replace("getLightColor(", "getLightCoords(") }

		// ── time & weather moved off their old hosts ─────────────────────────────────────────
		// Level#getDayTime became getOverworldClockTime (it always was the overworld's clock).
		string("!mc26-daytime", true) { replace(".getDayTime()", ".getOverworldClockTime()") }
		// isRaining/isThundering moved from LevelData onto Level. Both spawn predicates spell the
		// guard identically, so one rule covers all four call sites.
		string("!mc26-weatherpredicate", true) {
			replace("worldIn.getLevelData() != null && (worldIn.getLevelData().isThundering() || worldIn.getLevelData().isRaining())",
				"com.github.alexthe666.alexsmobs.misc.AMCompat.isRainingOrThundering(worldIn)")
		}
		// setWeatherParameters moved from ServerLevel up to MinecraftServer (same four arguments).
		string("!mc26-setweather", true) { replace("serverLevel.setWeatherParameters(", "serverLevel.getServer().setWeatherParameters(") }

		// ── entity interaction gained the hit location ───────────────────────────────────────
		// Entity#interact and Player#interactOn both take a Vec3 now. Two declaration rules
		// because EntityAnacondaPart still carries upstream's obfuscated parameter names.
		string("!mc26-interact-decl", true) {
			replace("InteractionResult interact(Player player, InteractionHand hand)",
				"InteractionResult interact(Player player, InteractionHand hand, net.minecraft.world.phys.Vec3 amLocation)")
		}
		string("!mc26-interact-decl-obf", true) {
			replace("InteractionResult interact(Player p_19978_, InteractionHand p_19979_)",
				"InteractionResult interact(Player p_19978_, InteractionHand p_19979_, net.minecraft.world.phys.Vec3 amLocation)")
		}
		string("!mc26-interact-parent", true) { replace("parent.interact(player, hand)", "parent.interact(player, hand, amLocation)") }
		// EntityAnacondaPart delegates both ways on one line, still with upstream's obfuscated names.
		string("!mc26-interact-obf-call", true) {
			replace("super.interact(p_19978_, p_19979_) : this.getParent().interact(p_19978_, p_19979_)",
				"super.interact(p_19978_, p_19979_, amLocation) : this.getParent().interact(p_19978_, p_19979_, amLocation)")
		}
		string("!mc26-interacton", true) {
			replace("player.interactOn(parent, message.offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND)",
				"player.interactOn(parent, message.offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, parent.position())")
		}

		// ── humanoid models lost the bulk visibility setter ──────────────────────────────────
		string("!mc26-setallvisible", true) {
			replace("model.setAllVisible(false)", "com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.setAllVisible(model, false)")
		}

		// ── the boss bar wants an explicit id ────────────────────────────────────────────────
		string("!mc26-bossevent", true) {
			replace("new ServerBossEvent(this.getDisplayName()", "new ServerBossEvent(java.util.UUID.randomUUID(), this.getDisplayName()")
		}
	}


// ═══════════════════════════════════════════════════════════════════════════════════════════
// MC 26.2
// ═══════════════════════════════════════════════════════════════════════════════════════════
// Same house rule as the >=26 group above, for the same reason: REPLACEMENTS DO NOT CHAIN, so
// every rule here is keyed on the 1.20.1 spelling and emits the FINAL 26.2 spelling, taking each
// hop itself. Three earlier rules that would otherwise claim an offset a rule here needs whole
// stand down behind `val mc262` (see !mc2111-pkg-critereon, !mc2111-cam-mainpos, !mc26-lightcoords).
if (eval(current.version, ">=26.2")) replacements {

	// ── three vanilla types were DELETED; they are vendored in this mod ──────────────────
	// See the header of each vendored file for why it is worth keeping rather than rewriting
	// the ~130 call sites. One rule apiece on the fully qualified name covers every import
	// and every inline use.
	string("!mc262-multibuffersource", true) {
		replace("net.minecraft.client.renderer.MultiBufferSource", "com.github.alexthe666.alexsmobs.client.render.compat.MultiBufferSource")
	}
	string("!mc262-tuple", true) {
		replace("net.minecraft.util.Tuple", "com.github.alexthe666.alexsmobs.misc.Tuple")
	}
	string("!mc262-flyinganimal", true) {
		replace("net.minecraft.world.entity.animal.FlyingAnimal", "com.github.alexthe666.alexsmobs.misc.FlyingAnimal")
	}

	// Minecraft#renderBuffers() went with MultiBufferSource. There is no frame-global immediate
	// buffer to fall back on, so the five callers get the discarding instance — four of them are
	// already dead code on this node; the fifth is a documented cosmetic regression.
	string("!mc262-buffersource", true) {
		replace("Minecraft.getInstance().renderBuffers().bufferSource()", "com.github.alexthe666.alexsmobs.client.render.compat.MultiBufferSource.noop()")
	}

	// ── pure package moves ──────────────────────────────────────────────────────────────
	string("!mc262-bucketable", true) {
		replace("net.minecraft.world.entity.animal.Bucketable", "net.minecraft.world.entity.Bucketable")
	}

	// 26.2 finally broke `advancements.critereon` apart — the triggers to `advancements.triggers`,
	// the predicates to `advancements.predicates` — so 1.21.11's typo fix (critereon -> criterion)
	// is stood down here and these take the whole hop. AMAdvancementTrigger reaches
	// SimpleCriterionTrigger through a wildcard import, which no rule can follow, so the wildcard
	// itself is pointed at `triggers` and the two predicate types it also needs are imported
	// explicitly in that file purely so these rules have a line to rewrite.
	string("!mc262-adv-criteriatriggers", true) {
		replace("net.minecraft.advancements.CriteriaTriggers", "net.minecraft.advancements.triggers.CriteriaTriggers")
	}
	string("!mc262-adv-criteriontrigger", true) {
		replace("net.minecraft.advancements.CriterionTrigger", "net.minecraft.advancements.triggers.CriterionTrigger")
	}
	string("!mc262-adv-contextaware", true) {
		replace("net.minecraft.advancements.critereon.ContextAwarePredicate", "net.minecraft.advancements.predicates.ContextAwarePredicate")
	}
	string("!mc262-adv-entitypredicate", true) {
		replace("net.minecraft.advancements.critereon.EntityPredicate", "net.minecraft.advancements.predicates.entity.EntityPredicate")
	}
	string("!mc262-adv-wildcard", true) {
		replace("net.minecraft.advancements.critereon.*", "net.minecraft.advancements.triggers.*")
	}

	// ── the vanilla entity-type constants moved off EntityType onto EntityTypes ──────────
	// Spelled out one by one on purpose: a bare `EntityType.` rule would also hit the 122
	// `EntityType.Builder` uses in this tree.
	string("!mc262-et-drowned", true) { replace("EntityType.DROWNED", "net.minecraft.world.entity.EntityTypes.DROWNED") }
	string("!mc262-et-enderdragon", true) { replace("EntityType.ENDER_DRAGON", "net.minecraft.world.entity.EntityTypes.ENDER_DRAGON") }
	string("!mc262-et-hoglin", true) { replace("EntityType.HOGLIN", "net.minecraft.world.entity.EntityTypes.HOGLIN") }
	string("!mc262-et-player", true) { replace("EntityType.PLAYER", "net.minecraft.world.entity.EntityTypes.PLAYER") }
	string("!mc262-et-shulker", true) { replace("EntityType.SHULKER", "net.minecraft.world.entity.EntityTypes.SHULKER") }
	string("!mc262-et-squid", true) { replace("EntityType.SQUID", "net.minecraft.world.entity.EntityTypes.SQUID") }
	string("!mc262-et-trader", true) { replace("EntityType.WANDERING_TRADER", "net.minecraft.world.entity.EntityTypes.WANDERING_TRADER") }
	string("!mc262-et-warden", true) { replace("EntityType.WARDEN", "net.minecraft.world.entity.EntityTypes.WARDEN") }

	// ── the camera lost its get- prefix one version after Camera itself did ──────────────
	// Specific first: the combined rule claims the whole expression so the bare one below
	// cannot half-rewrite it.
	string("!mc262-cam-mainpos", true) {
		replace("getMainCamera().getPosition()", "mainCamera().position()")
	}
	string("!mc262-cam-main", true) {
		replace("gameRenderer.getMainCamera();", "gameRenderer.mainCamera();")
	}

	// ── light coords: 26.1 renamed the accessor, 26.2 moved the static off LevelRenderer ──
	// Specific first, for the same reason as the camera pair.
	string("!mc262-lightcoords-levelrenderer", true) {
		replace("LevelRenderer.getLightColor(", "net.minecraft.util.LightCoordsUtil.getLightCoords(")
	}
	string("!mc262-lightcoords", true) {
		replace("getLightColor(", "getLightCoords(")
	}

	// ── odds and ends ───────────────────────────────────────────────────────────────────
	// Screens are shown explicitly now (Minecraft#setScreen is gone).
	string("!mc262-setscreen", true) {
		replace("Minecraft.getInstance().setScreen(", "Minecraft.getInstance().setScreenAndShow(")
	}
	// The HUD class is `Hud`; `Gui` is gone.
	string("!mc262-mobeffectsprite", true) {
		replace("net.minecraft.client.gui.Gui.getMobEffectSprite(", "net.minecraft.client.gui.Hud.getMobEffectSprite(")
	}
	// Block and item tags that share a name are declared once as a BlockItemTagId pair now —
	// `BlockItemTags.SMALL_FLOWERS` is a record of the two TagKeys, so the item half is `.item()`.
	// Keyed on the 1.20.1 spelling `ItemTags.FLOWERS` (see !mc2104-itemtag-flowers, which stands
	// down here) because rules do not chain.
	string("!mc262-smallflowers", true) {
		replace("ItemTags.FLOWERS", "net.minecraft.tags.BlockItemTags.SMALL_FLOWERS.item()")
	}

	// EntityType.Builder#immuneTo takes a TagKey<Block> now, not a Block varargs. Vanilla ships one
	// tag per mob family (BlockTags.POLAR_BEAR_IMMUNE_TO, …); this mod gets its own rather than
	// borrowing one, since a datapack retuning the polar bear should not silently retune three
	// Alex's Mobs mobs. The tag is at data/alexsmobs/tags/blocks/powder_snow_immune_to.json and is
	// inert on every node below 26.2.
	string("!mc262-immuneto", true) {
		replace(".immuneTo(Blocks.POWDER_SNOW)",
			".immuneTo(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK, com.github.alexthe666.alexsmobs.misc.AMCompat.rl(\"alexsmobs\", \"powder_snow_immune_to\")))")
	}

	// BlockBehaviour.Properties#emissiveRendering went from the three-argument StatePredicate to a
	// plain Predicate<BlockState>. (StatePredicate itself survives — isRedstoneConductor and the
	// rest still take it — and #postProcess is untouched, so only these four calls move.) Keyed on
	// each call's whole text so the lambda arity changes with it; the last one loses its method
	// reference because `yes` is a three-argument StatePredicate shape.
	string("!mc262-emissive-abc", true) {
		replace(".emissiveRendering((a, b, c) -> true)", ".emissiveRendering(a -> true)")
	}
	string("!mc262-emissive-ijk", true) {
		replace(".emissiveRendering((i, j, k) -> true)", ".emissiveRendering(i -> true)")
	}
	string("!mc262-emissive-bwp", true) {
		replace(".emissiveRendering((block, world, pos) -> true)", ".emissiveRendering(block -> true)")
	}
	string("!mc262-emissive-ref", true) {
		replace(".emissiveRendering(BlockRainbowGlass::yes)", ".emissiveRendering(state -> true)")
	}
}
}

for (version in stonecutter.versions.map { it.version }.distinct()) tasks.register("publish$version") {
	group = "publishing"
	dependsOn(stonecutter.tasks.named("publishMods") { metadata.version == version })
}

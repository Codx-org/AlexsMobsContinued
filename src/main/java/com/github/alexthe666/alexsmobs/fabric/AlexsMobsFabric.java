package com.github.alexthe666.alexsmobs.fabric;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.config.BiomeConfig;
import com.github.alexthe666.alexsmobs.config.ConfigHolder;
import com.github.alexthe666.alexsmobs.fabric.event.FabricServerEvents;
import com.github.alexthe666.alexsmobs.fabric.network.AMFabricNetwork;
import com.github.alexthe666.alexsmobs.fabric.world.FabricBiomeModifications;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;

/**
 * Fabric common entrypoint, named by {@code mod.fabric.entrypoint_main} in
 * stonecutter.properties.toml and emitted into fabric.mod.json by the mod-platform plugin.
 *
 * <p>This whole package is excluded from the compile on every non-Fabric node
 * ({@code ModPlatformPlugin.configureJava}), because {@code net.fabricmc.**} is simply absent
 * from a Forge/NeoForge classpath — so nothing in here needs a Stonecutter loader gate.
 *
 * <p><b>All this does is order the two things Fabric has no event for</b>: read and bake the config,
 * then construct {@code AlexsMobs}. That constructor is shared with the other two loaders and is
 * the single source of truth for registration <b>order</b> — which matters here, because Fabric's
 * registries are immediate (see {@link com.github.alexthe666.alexsmobs.fabric.registries.DeferredRegister}).
 * Nothing is duplicated in this class on purpose.
 *
 * <p><b>Known gap, deliberate for now:</b> {@code client/event/ClientEvents} is still excluded from
 * the Fabric compile, so this loader has no client event behaviour at all. {@code event/ServerEvents}
 * is no longer excluded and is now wired by {@link FabricServerEvents} — Fabric API callbacks for the
 * six hooks that have one, {@code mixin/fabric/**} for the eleven that do not — but loot modifiers,
 * brewing and structure spawns still do nothing. Biome spawns are closed, by
 * {@link FabricBiomeModifications}. The live list is in {@code docs/notes/fabric.md}.
 */
public class AlexsMobsFabric implements ModInitializer {

    public static final Logger LOGGER = LogManager.getLogger("alexsmobs");

    private static volatile MinecraftServer server;

    /**
     * The running server, or {@code null} on a client that has not joined a world.
     *
     * <p>Stands in for Forge's {@code LogicalSidedProvider.WORKQUEUE.get(LogicalSide.SERVER)} and
     * NeoForge's {@code ServerLifecycleHooks.getCurrentServer()} — both of which are equally
     * nullable, so callers need no extra guarding on this loader. Its only consumer today is the
     * vendored Citadel pathfinding thread factory, which reads the server's context classloader.
     *
     * <p>{@code volatile} because that factory runs on the pathfinding worker threads, not on the
     * server thread that writes the field.
     */
    @Nullable
    public static MinecraftServer getServer() {
        return server;
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Alex's Mobs Continued: Fabric common init");

        ServerLifecycleEvents.SERVER_STARTING.register(started -> server = started);
        ServerLifecycleEvents.SERVER_STOPPED.register(stopped -> server = null);

        // The same two calls the AlexsMobs constructor makes on the other two loaders, hoisted
        // ahead of it here because that constructor's Fabric arm registers content immediately and
        // some of it reads baked config. Without BiomeConfig.init() the per-mob spawn-biome files
        // under config/alexsmobs/ are never written or read, so nothing spawns.
        ConfigHolder.load();
        BiomeConfig.init();

        // Everything else. On Forge/NeoForge the loader news this up from the @Mod annotation; here
        // it is an ordinary constructor call, and it does the whole job in one go because Fabric's
        // registries are immediate: its ~20 DeferredRegister flushes run in place, and its Fabric
        // tail then does attributes, spawn placements and setup() directly rather than waiting for
        // mod-bus events that do not exist. Config first — item and block properties read it.
        new AlexsMobs();

        // The channel. Must come AFTER the constructor: AlexsMobs#setup is what calls
        // AMFabricNetwork#register for each of the ~22 messages, and the receiver dispatches on
        // that registration order.
        AMFabricNetwork.init();

        // Natural spawns + the leafcutter anthill feature. Forge/NeoForge get these from a datapack
        // biome modifier; Fabric's equivalent is a one-time registration, and it has to come after
        // BiomeConfig.init() (it reads the per-mob biome files) and after AlexsMobs (it names entity
        // types that the registries above have just filled).
        FabricBiomeModifications.init();

        // The game-bus half. Forge/NeoForge do this with MinecraftForge.EVENT_BUS.register(new
        // ServerEvents()) inside the AlexsMobs constructor (gated !fabric); Fabric has no bus, so
        // every hook is wired by hand. After AlexsMobs for the same reason FabricBiomeModifications
        // is: the handlers name registry objects.
        FabricServerEvents.init();

        // The capsid's recipe list (docs/notes/bug-reports.md #84). Forge and NeoForge get this
        // from AddReloadListenerEvent; on Fabric that event is one of the net.minecraftforge.**
        // stubs in fabric/forge/** and NOTHING EVER FIRES IT, so ServerEvents#onAddReloadListener
        // is dead code here and CapsidRecipeManager#apply was never called — the recipe list stayed
        // empty on all 17 Fabric nodes since Milestone 15, and TileEntityCapsid#getRecipeFor
        // therefore always returned null. That is the whole "the item just sits in the capsid"
        // report: no recipe matches, so the capsid never starts a cycle.
        //
        // ResourceManagerHelper is the supported seam. The registries-taking overload exists from
        // MC 1.21 (fabric-resource-loader 1.3.0) and is the one to use wherever the manager has a
        // registries-taking constructor at all — i.e. >=1.21.2 — so the split below never lands on
        // a node where only one of the two is available.
        //? if >=1.21.2 {
        /*net.fabricmc.fabric.api.resource.ResourceManagerHelper.get(net.minecraft.server.packs.PackType.SERVER_DATA)
                .registerReloadListener(com.github.alexthe666.alexsmobs.misc.AMCompat.rl("alexsmobs", "capsid_recipes"),
                        registries -> AlexsMobs.PROXY.getCapsidRecipeManager(registries));
        *///?} else {
        net.fabricmc.fabric.api.resource.ResourceManagerHelper.get(net.minecraft.server.packs.PackType.SERVER_DATA)
                .registerReloadListener(AlexsMobs.PROXY.getCapsidRecipeManager());
        //?}

        // The tarantula-hawk elytra's glide seam on this loader. Below 1.21.2 there is no item
        // hook and no glider component, only this Fabric API event; the handler mirrors what
        // ItemTarantulaHawkElytra#elytraFlightTick does on Forge/NeoForge (durability every 20
        // flight ticks) plus the ELYTRA_GLIDE game event vanilla and FabricElytraItem both emit
        // every 10 server ticks — that is what lets sculk sensors hear a glider. From 1.21.2 the
        // vanilla minecraft:glider component (attached in AMItemRegistry via AMCompat.glider)
        // covers Fabric too, and this event no longer exists in fabric-api.
        //? if <1.21.2 {
        net.fabricmc.fabric.api.entity.event.v1.EntityElytraEvents.CUSTOM.register((entity, tickElytra) -> {
            net.minecraft.world.item.ItemStack chest = entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
            if (chest.getItem() == com.github.alexthe666.alexsmobs.item.AMItemRegistry.TARANTULA_HAWK_ELYTRA.get()
                    && com.github.alexthe666.alexsmobs.item.ItemTarantulaHawkElytra.isUsable(chest)) {
                if (tickElytra) {
                    int flightTicks = entity.getFallFlyingTicks();
                    if (!entity.level().isClientSide()) {
                        if ((flightTicks + 1) % 20 == 0) {
                            com.github.alexthe666.alexsmobs.misc.AMCompat.hurtAndBreak(chest, 1, entity, net.minecraft.world.entity.EquipmentSlot.CHEST);
                        }
                        if ((flightTicks + 1) % 10 == 0) {
                            entity.gameEvent(net.minecraft.world.level.gameevent.GameEvent.ELYTRA_GLIDE);
                        }
                    }
                }
                return true;
            }
            return false;
        });
        //?}
    }
}

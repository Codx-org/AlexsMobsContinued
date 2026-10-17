package com.github.alexthe666.alexsmobs.fabric;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.fabric.client.FabricClientNetwork;
import net.fabricmc.api.ClientModInitializer;

/**
 * Fabric client entrypoint, named by {@code mod.fabric.entrypoint_client}.
 *
 * <p>Fabric runs this only on the client dist, which is the loader's replacement for the
 * {@code CommonProxy}/{@code ClientProxy} split this mod uses on Forge — so the client half of
 * registration (model layers, renderers, particle providers, colour handlers, key binds) lands
 * here rather than behind a {@code DistExecutor}-style indirection.
 *
 * <p>The split between the two halves is the same as everywhere else in this mod:
 * <ul>
 *   <li>{@code ClientProxy.init()} — colour handlers, render layers, model layers, particle
 *       providers. It already runs, from {@code AlexsMobs}' constructor, which is shared code;
 *       its {@code elif fabric} arm calls each registry directly because Fabric's client
 *       registries stay open through client init and there is no mod-bus event to hang them off.
 *   <li>{@code ClientProxy.clientInit()} — the ~130 {@code EntityRenderers.register} calls, the
 *       three {@code BlockEntityRenderers.register} calls and the transmutation-table screen.
 *       On Forge/NeoForge that runs from {@code FMLClientSetupEvent}, which Fabric has no
 *       equivalent of — so it runs <b>here</b>. Nothing in it needs a deferred work queue: every
 *       registry it touches is a plain static map that is safe to write during client init.
 * </ul>
 *
 * <p>⚠️ Without this call the mod loads and every mob spawns, but nothing has a renderer, which
 * neither gate can see — {@code bootgate.sh} is a dedicated server and {@code clientgate.sh}
 * stops at the title screen. See {@link AlexsMobsFabric} for the rest of the Fabric wiring.
 */
public class AlexsMobsFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        AlexsMobsFabric.LOGGER.info("Alex's Mobs Continued: Fabric client init");
        AlexsMobs.PROXY.clientInit();
        // The clientbound receiver and the serverbound send sink. AlexsMobsFabric#onInitialize has
        // already run (Fabric orders main entrypoints before client ones), so the message registry
        // and the payload types are in place by now.
        FabricClientNetwork.init();
    }
}

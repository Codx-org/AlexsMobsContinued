package com.github.alexthe666.alexsmobs.citadel;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Stand-in for Citadel's mod main class, for the vendored Citadel subset.
 * <p>
 * Alex's Mobs Continued bundles the parts of Citadel it needs instead of depending on the
 * Citadel mod (there is no Citadel for Forge above MC 1.20.1). The bundled classes live under
 * {@code com.github.alexthe666.alexsmobs.citadel} so that they can never clash with a real
 * Citadel installation sharing the same classloader.
 * <p>
 * The vendored code only ever used its main class for a logger, the network channel and the
 * side proxy, so this shim provides exactly those three things and routes networking through
 * Alex's Mobs' own channel.
 */
public class Citadel {

    public static final Logger LOGGER = LogManager.getLogger("alexsmobs-citadel");

    // See AlexsMobs#PROXY — NeoForge 1.21 dropped DistExecutor, and the Supplier hop is what keeps
    // the client class off the dedicated server's verifier.
    // Forge 64 deleted DistExecutor as well; its FMLEnvironment.dist is still a public field.
    //? if forge && >=26 {
    /*public static final CitadelProxy PROXY = net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()
            ? ((java.util.function.Supplier<CitadelProxy>) CitadelClientProxy::new).get()
            : new CitadelProxy();
    *///?} elif neoforge && >=1.21.9 {
    /*public static final CitadelProxy PROXY = net.neoforged.fml.loading.FMLEnvironment.getDist().isClient()
            ? ((java.util.function.Supplier<CitadelProxy>) CitadelClientProxy::new).get()
            : new CitadelProxy();
    *///?} elif neoforge && >=1.21 {
    /*public static final CitadelProxy PROXY = net.neoforged.fml.loading.FMLEnvironment.dist.isClient()
            ? ((java.util.function.Supplier<CitadelProxy>) CitadelClientProxy::new).get()
            : new CitadelProxy();
    *///?} elif fabric {
    /*public static final CitadelProxy PROXY =
            net.fabricmc.loader.api.FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.CLIENT
            ? ((java.util.function.Supplier<CitadelProxy>) CitadelClientProxy::new).get()
            : new CitadelProxy();
    *///?} else {
    public static final CitadelProxy PROXY = net.minecraftforge.fml.DistExecutor.runForDist(() -> CitadelClientProxy::new, () -> CitadelProxy::new);
    //?}

    public static <MSG> void sendMSGToServer(MSG message) {
        AlexsMobs.sendMSGToServer(message);
    }

    public static <MSG> void sendMSGToAll(MSG message) {
        AlexsMobs.sendMSGToAll(message);
    }

    public static <MSG> void sendNonLocal(MSG msg, ServerPlayer player) {
        AlexsMobs.sendNonLocal(msg, player);
    }
}

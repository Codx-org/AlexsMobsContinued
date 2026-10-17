package com.github.alexthe666.alexsmobs.fabric.client;

import com.github.alexthe666.alexsmobs.fabric.network.AMFabricNetwork;
import com.github.alexthe666.alexsmobs.message.AMNetContext;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;

/**
 * The client half of {@link AMFabricNetwork}: the serverbound send and the clientbound receiver.
 *
 * <p>Split out of {@code AMFabricNetwork} purely because {@code ClientPlayNetworking} is client-only
 * API — Fabric strips it from a dedicated server, so merely <i>naming</i> it from a class the server
 * loads is a {@code NoClassDefFoundError}. This class is reached only from
 * {@code AlexsMobsFabricClient}, and it installs itself as the serverbound sink so that the shared
 * {@code AlexsMobs#sendMSGToServer} has something to call.
 */
public final class FabricClientNetwork {

    private FabricClientNetwork() {
    }

    public static void init() {
        AMFabricNetwork.setServerboundSink(FabricClientNetwork::sendToServer);
        //? if >=1.20.5 {
        /*ClientPlayNetworking.registerGlobalReceiver(AMFabricNetwork.TYPE, (payload, context) ->
                AMFabricNetwork.handle(payload.index(), payload.message(), clientContext(context.client())));
        *///?} else {
        // Netty thread, buffer valid only for this call — decode now, hop in enqueueWork.
        ClientPlayNetworking.registerGlobalReceiver(AMFabricNetwork.CHANNEL, (client, listener, buf, responseSender) -> {
            int index = AMFabricNetwork.readIndex(buf);
            AMFabricNetwork.handle(index, AMFabricNetwork.decodeBody(index, buf), clientContext(client));
        });
        //?}
    }

    private static void sendToServer(Object message) {
        //? if >=1.20.5 {
        /*ClientPlayNetworking.send(AMFabricNetwork.wrap(message));
        *///?} else {
        ClientPlayNetworking.send(AMFabricNetwork.CHANNEL, AMFabricNetwork.encode(message));
        //?}
    }

    private static AMNetContext clientContext(Minecraft client) {
        return new AMNetContext() {
            public void setPacketHandled(boolean handled) {
                // Fabric has no per-packet "handled" flag; unhandled payloads simply do nothing.
            }

            public void enqueueWork(Runnable work) {
                client.execute(work);
            }

            public ServerPlayer getSender() {
                // Clientbound: there is no sender. Matches Forge, whose context returns null here
                // for PLAY_TO_CLIENT, and every handler already branches on isClientSide first.
                return null;
            }

            public boolean isClientSide() {
                return true;
            }
        };
    }
}

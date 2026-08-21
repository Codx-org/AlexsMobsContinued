package com.github.alexthe666.alexsmobs.message;

// Holds the two NeoForge networking calls that 1.21.7 changed, because both live inside
// AMNeoNetwork's body — which is itself already wrapped in a `neoforge && >=1.20.6` block, and
// Stonecutter blocks are siblings, never nested. Duplicating the whole ~115-line class to change
// two calls would be worse than this, so the choice lives here and AMNeoNetwork just delegates.
//
// 1. PacketDistributor#sendToServer was REMOVED; serverbound sending moved onto the client-only
//    net.neoforged.neoforge.client.network.ClientPacketDistributor. (Verified against the bundles:
//    1.20.6 through 1.21.6 have the former and no ClientPacketDistributor at all; 1.21.7 and 1.21.8
//    have exactly the reverse.)
//
// 2. playBidirectional SPLIT ITS HANDLER IN TWO. 1.21.7 added a second
//    playBidirectional(type, codec, serverHandler, clientHandler) overload and quietly redefined the
//    old three-argument form to mean `(handler, null)` — i.e. serverbound only. NetworkRegistry now
//    keeps SERVERBOUND_HANDLERS and CLIENTBOUND_HANDLERS as separate maps; a null clientbound
//    handler is legal at registration because the new RegisterClientPayloadHandlersEvent may supply
//    it later, and the brand-new ClientNetworkRegistry#setup then HARD-FAILS the client if nothing
//    ever did: "Some clientbound payloads are missing client-side handlers: [alexsmobs:main_channel]"
//    — a mod-loading crash during the first client resource reload. The server is unaffected, which
//    is exactly why the boot gate could not see it.
//
//    Passing the same handler twice reproduces the pre-1.21.7 semantics exactly, and doing it from
//    here (rather than from the client event) is safe on a dedicated server: NetworkRegistry#register
//    fills CLIENTBOUND_HANDLERS from common code with no dist check, and AMNeoNetwork#handle names no
//    client-only type.
//
// Like AMNeoNetwork, the whole body is loader-gated: on Forge nodes (and on NeoForge <1.20.6, which
// still has the PacketDistributor.SERVER.noArg() shape and calls it inline) this compilation unit
// is just a package declaration.
//? if neoforge && >=1.20.6 && <1.21.7 {
/*import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class AMNeoSend {

    public static void toServer(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    public static <T extends CustomPacketPayload> void registerPlay(PayloadRegistrar registrar, CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, IPayloadHandler<T> handler) {
        registrar.playBidirectional(type, codec, handler);
    }
}
*///?}
//? if neoforge && >=1.21.7 {
/*import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class AMNeoSend {

    // ClientPacketDistributor lives in NeoForge's client package, but naming it here is safe on a
    // dedicated server: it is only ever reached from client code, and the invokestatic resolves
    // lazily on first execution rather than when AMNeoNetwork is loaded.
    public static void toServer(CustomPacketPayload payload) {
        ClientPacketDistributor.sendToServer(payload);
    }

    // The argument order is (serverHandler, clientHandler) — verified from the bytecode, since the
    // names are not obvious: playToServer passes (handler, null) and playToClient passes
    // (null, handler), and register() throws IllegalArgumentException only for a null SERVERBOUND
    // handler.
    public static <T extends CustomPacketPayload> void registerPlay(PayloadRegistrar registrar, CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, IPayloadHandler<T> handler) {
        registrar.playBidirectional(type, codec, handler, handler);
    }
}
*///?}

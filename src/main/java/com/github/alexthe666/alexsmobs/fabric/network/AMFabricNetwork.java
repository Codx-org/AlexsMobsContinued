package com.github.alexthe666.alexsmobs.fabric.network;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.message.AMNetContext;
import com.github.alexthe666.alexsmobs.misc.AMCompat;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

//? if >=1.20.5 {
/*import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
*///?} else {
import io.netty.buffer.Unpooled;
//?}

/**
 * The Fabric transport for Alex's Mobs' ~22 messages — the counterpart of Forge's
 * {@code SimpleChannel} and of {@link com.github.alexthe666.alexsmobs.message.AMNeoNetwork}.
 *
 * <p><b>Until this class existed, Fabric had no networking at all.</b> The three hooks in
 * {@code AlexsMobs} ({@code sendMSGToServer}, {@code sendNonLocal}, {@code registerMessage}) only
 * had Forge and NeoForge arms, so on all 17 Fabric nodes they compiled to an empty method — every
 * packet this mod sends was silently dropped, on both sides, since Milestone 15. Nothing in the
 * gates could see it: an empty method compiles and boots green.
 *
 * <p>The design is the same wrapper-payload trick NeoForge gets: a single channel carries
 * {index, message}, the index being the registration order from {@code AlexsMobs#setup}, which is
 * identical on both sides. That keeps all ~22 message classes byte-identical across all three
 * loaders — only this file and the loader hooks differ.
 *
 * <p>Two eras, one seam:
 * <ul>
 *   <li><b>1.20.5+</b> — Fabric API's payload registry. The payload holds the <i>message object</i>
 *       and encodes late, straight into the connection's own buffer, because that buffer is a
 *       {@code RegistryFriendlyByteBuf} and {@code AMCompat#writeItem} casts to one. Staging into a
 *       hand-allocated {@code FriendlyByteBuf} first is exactly the bug that crashed NeoForge
 *       (report #24); this side must not repeat it.
 *   <li><b>below 1.20.5</b> — the raw-buffer channel API, which predates payloads. There is no
 *       {@code RegistryFriendlyByteBuf} on those nodes, so staging a buffer is the only option and
 *       is also correct.
 * </ul>
 *
 * <p>The clientbound half lives in {@code fabric/client/FabricClientNetwork} — {@code
 * ClientPlayNetworking} is client-only API and would be a {@code NoClassDefFoundError} on a
 * dedicated server if it were named from here. Serverbound sends therefore go through a sink that
 * the client entrypoint installs.
 */
public class AMFabricNetwork {

    public static final ResourceLocation CHANNEL = AMCompat.rl(AlexsMobs.MODID, "main_channel");

    private static final List<Registration<?>> REGISTRATIONS = new ArrayList<>();
    private static final Map<Class<?>, Integer> INDEX_BY_TYPE = new HashMap<>();

    /** Installed by the client entrypoint; stays null on a dedicated server, where nothing sends. */
    private static volatile Consumer<Object> toServer;

    private record Registration<MSG>(BiConsumer<MSG, FriendlyByteBuf> encoder,
                                     Function<FriendlyByteBuf, MSG> decoder,
                                     BiConsumer<MSG, AMNetContext> handler) {

        @SuppressWarnings("unchecked")
        void encode(Object message, FriendlyByteBuf buf) {
            encoder.accept((MSG) message, buf);
        }

        Object decode(FriendlyByteBuf buf) {
            return decoder.apply(buf);
        }

        @SuppressWarnings("unchecked")
        void handle(Object message, AMNetContext context) {
            handler.accept((MSG) message, context);
        }
    }

    // Called once per message from AlexsMobs#setup, in the same order on both sides, so the
    // index doubles as the on-wire discriminator.
    public static <MSG> void register(Class<MSG> type, BiConsumer<MSG, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, MSG> decoder, BiConsumer<MSG, AMNetContext> handler) {
        INDEX_BY_TYPE.put(type, REGISTRATIONS.size());
        REGISTRATIONS.add(new Registration<>(encoder, decoder, handler));
    }

    public static void setServerboundSink(Consumer<Object> sink) {
        toServer = sink;
    }

    public static void sendToServer(Object message) {
        Consumer<Object> sink = toServer;
        if (sink == null) {
            // A dedicated server reaching this means a clientbound-only code path ran server-side;
            // dropping it is what the empty method used to do, but say so rather than stay silent.
            AlexsMobs.LOGGER.warn("Dropped serverbound Alex's Mobs message {} — no client connection", message.getClass());
            return;
        }
        sink.accept(message);
    }

    public static int indexOf(Object message) {
        Integer index = INDEX_BY_TYPE.get(message.getClass());
        if (index == null) {
            throw new IllegalArgumentException("Unregistered Alex's Mobs message: " + message.getClass());
        }
        return index;
    }

    public static void encodeInto(int index, Object message, FriendlyByteBuf buf) {
        buf.writeVarInt(index);
        REGISTRATIONS.get(index).encode(message, buf);
    }

    // The rest of the buffer is only parseable by the message the index names, so an unknown index
    // is a protocol mismatch (the two sides disagree on registration order) rather than something
    // to warn about and skip — fail the packet, like vanilla.
    public static int readIndex(FriendlyByteBuf buf) {
        int index = buf.readVarInt();
        if (index < 0 || index >= REGISTRATIONS.size()) {
            throw new IllegalArgumentException("Received Alex's Mobs packet with unknown index " + index);
        }
        return index;
    }

    public static Object decodeBody(int index, FriendlyByteBuf buf) {
        return REGISTRATIONS.get(index).decode(buf);
    }

    public static void handle(int index, Object message, AMNetContext context) {
        REGISTRATIONS.get(index).handle(message, context);
    }

    public static AMNetContext serverContext(MinecraftServer server, ServerPlayer sender) {
        return new AMNetContext() {
            public void setPacketHandled(boolean handled) {
                // Fabric has no per-packet "handled" flag; unhandled payloads simply do nothing.
            }

            public void enqueueWork(Runnable work) {
                // Correct from either thread: the payload API hands us the server thread (where
                // execute runs the task inline) and the pre-1.20.5 raw channel hands us a netty
                // thread (where it queues).
                server.execute(work);
            }

            public ServerPlayer getSender() {
                return sender;
            }

            public boolean isClientSide() {
                return false;
            }
        };
    }

    //? if >=1.20.5 {
    /*public static final CustomPacketPayload.Type<AMPayload> TYPE = new CustomPacketPayload.Type<>(CHANNEL);

    public static final StreamCodec<RegistryFriendlyByteBuf, AMPayload> CODEC =
            StreamCodec.of(AMFabricNetwork::encodePayload, AMFabricNetwork::decodePayload);

    // Both directions are registered here, from the COMMON entrypoint: PayloadTypeRegistry is
    // common API, and a client that only registered the clientbound half could not send.
    public static void init() {
        PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC);
        PayloadTypeRegistry.serverboundPlay().register(TYPE, CODEC);
        ServerPlayNetworking.registerGlobalReceiver(TYPE, (payload, context) ->
                handle(payload.index(), payload.message(), serverContext(context.server(), context.player())));
    }

    public static void sendToPlayer(Object message, ServerPlayer player) {
        ServerPlayNetworking.send(player, wrap(message));
    }

    public static AMPayload wrap(Object message) {
        return new AMPayload(indexOf(message), message);
    }

    private static void encodePayload(RegistryFriendlyByteBuf buf, AMPayload payload) {
        encodeInto(payload.index(), payload.message(), buf);
    }

    private static AMPayload decodePayload(RegistryFriendlyByteBuf buf) {
        int index = readIndex(buf);
        return new AMPayload(index, decodeBody(index, buf));
    }

    // `message` is the live Alex's Mobs message object on the sending side and the freshly decoded
    // one on the receiving side; CODEC is what turns it into bytes and back.
    public record AMPayload(int index, Object message) implements CustomPacketPayload {

        public CustomPacketPayload.Type<AMPayload> type() {
            return TYPE;
        }
    }
    *///?} else {
    // Raw-channel era. The handler runs on the netty thread and the buffer is only valid for the
    // duration of the call, so decode here and let AMNetContext#enqueueWork do the hop — which is
    // also what Forge's own pre-1.20.2 channel did.
    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(CHANNEL, (server, player, listener, buf, responseSender) -> {
            int index = readIndex(buf);
            handle(index, decodeBody(index, buf), serverContext(server, player));
        });
    }

    public static void sendToPlayer(Object message, ServerPlayer player) {
        ServerPlayNetworking.send(player, CHANNEL, encode(message));
    }

    public static FriendlyByteBuf encode(Object message) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        encodeInto(indexOf(message), message, buf);
        return buf;
    }
    //?}
}

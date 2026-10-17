package com.github.alexthe666.alexsmobs.message;

import com.github.alexthe666.alexsmobs.misc.AMCompat;

// NeoForge replaced Forge's SimpleChannel with typed CustomPacketPayloads. Alex's Mobs has ~22
// plain-object messages with static write/read/handle methods, so rather than turning each one
// into a payload record, a single wrapper payload carries {index, bytes} and dispatches back to
// the original handler. That keeps every message class byte-identical across both loaders; only
// this file (NeoForge-only) and the three send/register hooks in AlexsMobs differ.
//
// The whole body is loader-gated: on Forge nodes this compilation unit is just a package
// declaration, and the AlexsMobs call sites that reference it are commented out too.
//? if neoforge && <1.20.6 {
/*import com.github.alexthe666.alexsmobs.AlexsMobs;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class AMNeoNetwork {

    public static final ResourceLocation CHANNEL = AMCompat.rl(AlexsMobs.MODID, "main_channel");

    private static final List<Registration<?>> REGISTRATIONS = new ArrayList<>();
    private static final Map<Class<?>, Integer> INDEX_BY_TYPE = new HashMap<>();

    private record Registration<MSG>(BiConsumer<MSG, FriendlyByteBuf> encoder,
                                     Function<FriendlyByteBuf, MSG> decoder,
                                     BiConsumer<MSG, AMNetContext> handler) {

        @SuppressWarnings("unchecked")
        void encode(Object message, FriendlyByteBuf buf) {
            encoder.accept((MSG) message, buf);
        }

        void decodeAndHandle(FriendlyByteBuf buf, AMNetContext context) {
            handler.accept(decoder.apply(buf), context);
        }
    }

    // Called once per message from AlexsMobs#setup, in the same order on both sides, so the
    // index doubles as the on-wire discriminator.
    public static <MSG> void register(Class<MSG> type, BiConsumer<MSG, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, MSG> decoder, BiConsumer<MSG, AMNetContext> handler) {
        INDEX_BY_TYPE.put(type, REGISTRATIONS.size());
        REGISTRATIONS.add(new Registration<>(encoder, decoder, handler));
    }

    public static void onRegisterPayloads(RegisterPayloadHandlerEvent event) {
        event.registrar(AlexsMobs.MODID).play(CHANNEL, AMPayload::new, AMNeoNetwork::handle);
    }

    public static void sendToServer(Object message) {
        PacketDistributor.SERVER.noArg().send(wrap(message));
    }

    public static void sendToPlayer(Object message, ServerPlayer player) {
        PacketDistributor.PLAYER.with(player).send(wrap(message));
    }

    private static AMPayload wrap(Object message) {
        Integer index = INDEX_BY_TYPE.get(message.getClass());
        if (index == null) {
            throw new IllegalArgumentException("Unregistered Alex's Mobs message: " + message.getClass());
        }
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        REGISTRATIONS.get(index).encode(message, buf);
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        return new AMPayload(index, data);
    }

    // Forge's <1.20.2 channel ran handlers on the network thread and every Alex's Mobs handler
    // enqueues its own main-thread work, so this mirrors that: decode here, let the handler
    // schedule itself.
    private static void handle(AMPayload payload, PlayPayloadContext context) {
        if (payload.index() < 0 || payload.index() >= REGISTRATIONS.size()) {
            AlexsMobs.LOGGER.warn("Received Alex's Mobs packet with unknown index {}", payload.index());
            return;
        }
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(payload.data()));
        REGISTRATIONS.get(payload.index()).decodeAndHandle(buf, adapt(context));
    }

    private static AMNetContext adapt(PlayPayloadContext context) {
        return new AMNetContext() {
            public void setPacketHandled(boolean handled) {
                // NeoForge has no per-packet "handled" flag; unhandled payloads simply do nothing.
            }

            public void enqueueWork(Runnable work) {
                context.workHandler().submitAsync(work);
            }

            public ServerPlayer getSender() {
                return context.player().orElse(null) instanceof ServerPlayer serverPlayer ? serverPlayer : null;
            }

            public boolean isClientSide() {
                return context.flow().isClientbound();
            }
        };
    }

    public record AMPayload(int index, byte[] data) implements CustomPacketPayload {

        public AMPayload(FriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readByteArray());
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(index);
            buf.writeByteArray(data);
        }

        public ResourceLocation id() {
            return CHANNEL;
        }
    }
}
*///?}

// NeoForge 20.6 rewrote payload registration: RegisterPayloadHandlerEvent -> ...HandlersEvent,
// the channel key became a CustomPacketPayload.Type with a StreamCodec instead of an id() +
// write(), PlayPayloadContext collapsed into IPayloadContext, and PacketDistributor's
// direction constants became plain static send methods. Everything else is the wrapper-payload
// design described above.
//? if neoforge && >=1.20.6 {
/*import com.github.alexthe666.alexsmobs.AlexsMobs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class AMNeoNetwork {

    public static final ResourceLocation CHANNEL = AMCompat.rl(AlexsMobs.MODID, "main_channel");
    public static final CustomPacketPayload.Type<AMPayload> TYPE = new CustomPacketPayload.Type<>(CHANNEL);
    // The payload carries the MESSAGE OBJECT and encodes it straight into the outgoing packet
    // buffer — it does NOT pre-render it into a byte[]. That buffer belongs to the connection, and
    // on the PLAY protocol it is a RegistryFriendlyByteBuf, which is what AMCompat#writeItem casts
    // to from 1.20.5 on. Staging through `new FriendlyByteBuf(Unpooled.buffer())` first therefore
    // threw ClassCastException for every message carrying an ItemStack — the kangaroo's inventory
    // sync (reported), its eat packet, and Citadel's PropertiesMessage. Encoding late is also what
    // Forge does: its ForgePayload holds the encoder Consumer until vanilla writes the packet.
    //
    // The mirror change moves decoding onto the netty thread, where Forge's SimpleChannel and
    // vanilla both already decode; no Alex's Mobs decoder touches the level, the client or a
    // registry beyond the buffer's own. Handlers still hop to the main thread via AMNetContext.
    public static final StreamCodec<RegistryFriendlyByteBuf, AMPayload> CODEC =
            StreamCodec.of(AMNeoNetwork::encodePayload, AMNeoNetwork::decodePayload);

    private static final List<Registration<?>> REGISTRATIONS = new ArrayList<>();
    private static final Map<Class<?>, Integer> INDEX_BY_TYPE = new HashMap<>();

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

    // Via AMNeoSend: 1.21.7 split playBidirectional's single handler into a serverbound and a
    // clientbound one, and this block cannot hold a nested Stonecutter branch.
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        AMNeoSend.registerPlay(event.registrar(AlexsMobs.MODID), TYPE, CODEC, AMNeoNetwork::handle);
    }

    // Via AMNeoSend: 1.21.7 moved serverbound sending off PacketDistributor, and this block
    // cannot hold a nested Stonecutter branch.
    public static void sendToServer(Object message) {
        AMNeoSend.toServer(wrap(message));
    }

    public static void sendToPlayer(Object message, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, wrap(message));
    }

    private static AMPayload wrap(Object message) {
        Integer index = INDEX_BY_TYPE.get(message.getClass());
        if (index == null) {
            throw new IllegalArgumentException("Unregistered Alex's Mobs message: " + message.getClass());
        }
        return new AMPayload(index, message);
    }

    private static void encodePayload(RegistryFriendlyByteBuf buf, AMPayload payload) {
        buf.writeVarInt(payload.index());
        REGISTRATIONS.get(payload.index()).encode(payload.message(), buf);
    }

    // Unlike the old byte[] form this cannot warn-and-ignore: the rest of the buffer is only
    // parseable by the message the index names. An unknown index means the two sides disagree on
    // the registration order, which is a protocol mismatch, so it fails the packet like vanilla.
    private static AMPayload decodePayload(RegistryFriendlyByteBuf buf) {
        int index = buf.readVarInt();
        if (index < 0 || index >= REGISTRATIONS.size()) {
            throw new IllegalArgumentException("Received Alex's Mobs packet with unknown index " + index);
        }
        return new AMPayload(index, REGISTRATIONS.get(index).decode(buf));
    }

    // The registrar already hands us the main thread, and every Alex's Mobs handler enqueues its
    // own work anyway — on the main thread enqueueWork just runs the task inline.
    private static void handle(AMPayload payload, IPayloadContext context) {
        REGISTRATIONS.get(payload.index()).handle(payload.message(), adapt(context));
    }

    private static AMNetContext adapt(IPayloadContext context) {
        return new AMNetContext() {
            public void setPacketHandled(boolean handled) {
                // NeoForge has no per-packet "handled" flag; unhandled payloads simply do nothing.
            }

            public void enqueueWork(Runnable work) {
                context.enqueueWork(work);
            }

            public ServerPlayer getSender() {
                return context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
            }

            public boolean isClientSide() {
                return context.flow().isClientbound();
            }
        };
    }

    // `message` is the live Alex's Mobs message object on the sending side and the freshly decoded
    // one on the receiving side; CODEC is what turns it into bytes and back.
    public record AMPayload(int index, Object message) implements CustomPacketPayload {

        public CustomPacketPayload.Type<AMPayload> type() {
            return TYPE;
        }
    }
}
*///?}

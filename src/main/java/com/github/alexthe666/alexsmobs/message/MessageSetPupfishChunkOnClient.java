package com.github.alexthe666.alexsmobs.message;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

public class MessageSetPupfishChunkOnClient {

    public int chunkX;
    public int chunkZ;

    public MessageSetPupfishChunkOnClient(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public MessageSetPupfishChunkOnClient() {
    }

    public static MessageSetPupfishChunkOnClient read(FriendlyByteBuf buf) {
        return new MessageSetPupfishChunkOnClient(buf.readInt(), buf.readInt());
    }

    public static void write(MessageSetPupfishChunkOnClient message, FriendlyByteBuf buf) {
        buf.writeInt(message.chunkX);
        buf.writeInt(message.chunkZ);
    }

    public static class Handler {
        public Handler() {
        }

        public static void handle(MessageSetPupfishChunkOnClient message, AMNetContext context) {
            context.setPacketHandled(true);
            context.enqueueWork(() -> {
                AlexsMobs.PROXY.setPupfishChunkForItem(message.chunkX, message.chunkZ);
            });
        }
    }
}
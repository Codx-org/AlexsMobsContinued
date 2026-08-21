package com.github.alexthe666.alexsmobs.citadel.server.message;

import com.github.alexthe666.alexsmobs.message.AMNetContext;

import com.github.alexthe666.alexsmobs.citadel.Citadel;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

public class AnimationMessage {

    private final int entityID;
    private final int index;

    public AnimationMessage(int entityID, int index) {
        this.entityID = entityID;
        this.index = index;
    }

    public static class Handler {
        public Handler() {
        }

        public static void handle(AnimationMessage message, AMNetContext context) {
            Citadel.PROXY.handleAnimationPacket(message.entityID, message.index);
            context.setPacketHandled(true);
        }
    }

    public static AnimationMessage read(FriendlyByteBuf buf) {
        return new AnimationMessage(buf.readInt(), buf.readInt());
    }

    public static void write(AnimationMessage message, FriendlyByteBuf buf) {
        buf.writeInt(message.entityID);
        buf.writeInt(message.index);
    }
}

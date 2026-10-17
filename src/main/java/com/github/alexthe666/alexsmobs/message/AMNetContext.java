package com.github.alexthe666.alexsmobs.message;

import net.minecraft.server.level.ServerPlayer;

/**
 * Version-neutral view of a Forge network-handler context.
 * <p>
 * Forge 1.20.2 rewrote the networking layer: {@code Supplier<NetworkEvent.Context>} became
 * {@code CustomPayloadEvent.Context} (no supplier), and {@code NetworkEvent} was deleted
 * outright. Every message handler in this mod takes this interface instead, so the ~23
 * message classes compile unchanged on every node; the single adapter lives in
 * {@link com.github.alexthe666.alexsmobs.AlexsMobs}.
 */
public interface AMNetContext {

    void setPacketHandled(boolean handled);

    void enqueueWork(Runnable work);

    /** The sending player — non-null only for serverbound packets. */
    ServerPlayer getSender();

    /** True when this packet is being received on the client. */
    boolean isClientSide();
}

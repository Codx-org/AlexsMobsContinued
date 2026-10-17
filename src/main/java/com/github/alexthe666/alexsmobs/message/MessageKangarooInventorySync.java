package com.github.alexthe666.alexsmobs.message;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.entity.EntityKangaroo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;
import com.github.alexthe666.alexsmobs.misc.AMCompat;

public class MessageKangarooInventorySync {

    public int kangaroo;
    public int slotId;
    public ItemStack stack;

    public MessageKangarooInventorySync(int kangaroo, int slotId, ItemStack stack) {
        this.kangaroo = kangaroo;
        this.slotId = slotId;
        this.stack = stack;
    }

    public MessageKangarooInventorySync() {
    }

    public static MessageKangarooInventorySync read(FriendlyByteBuf buf) {
        return new MessageKangarooInventorySync(buf.readInt(), buf.readInt(), com.github.alexthe666.alexsmobs.misc.AMCompat.readItem(buf));
    }

    public static void write(MessageKangarooInventorySync message, FriendlyByteBuf buf) {
        buf.writeInt(message.kangaroo);
        buf.writeInt(message.slotId);
        com.github.alexthe666.alexsmobs.misc.AMCompat.writeItem(buf, message.stack);
    }

    public static class Handler {
        public Handler() {
        }

        public static void handle(MessageKangarooInventorySync message, AMNetContext context) {
            context.setPacketHandled(true);
            context.enqueueWork(() -> {
                Player player = context.getSender();
                if (context.isClientSide()) {
                    player = AlexsMobs.PROXY.getClientSidePlayer();
                }

                if (player != null) {
                    if (player.level() != null) {
                        Entity entity = player.level().getEntity(message.kangaroo);
                        if (entity instanceof EntityKangaroo && ((EntityKangaroo) entity).kangarooInventory != null) {
                            if (message.slotId < 0) {

                            } else {
                                ((EntityKangaroo) entity).kangarooInventory.setItem(message.slotId, message.stack);
                            }
                        }
                    }
                }
            });
        }
    }
}
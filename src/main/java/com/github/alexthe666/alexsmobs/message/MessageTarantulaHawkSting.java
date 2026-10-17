package com.github.alexthe666.alexsmobs.message;

import com.github.alexthe666.alexsmobs.misc.AMCompat;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.effect.AMEffectRegistry;
import com.github.alexthe666.alexsmobs.entity.EntityTarantulaHawk;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
//? if <1.20.5
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.player.Player;

import java.util.function.Supplier;

public class MessageTarantulaHawkSting {

    public int hawk;
    public int spider;

    public MessageTarantulaHawkSting(int rider, int mount) {
        this.hawk = rider;
        this.spider = mount;
    }

    public MessageTarantulaHawkSting() {
    }

    public static MessageTarantulaHawkSting read(FriendlyByteBuf buf) {
        return new MessageTarantulaHawkSting(buf.readInt(), buf.readInt());
    }

    public static void write(MessageTarantulaHawkSting message, FriendlyByteBuf buf) {
        buf.writeInt(message.hawk);
        buf.writeInt(message.spider);
    }

    public static class Handler {
        public Handler() {
        }

        public static void handle(MessageTarantulaHawkSting message, AMNetContext context) {
            context.setPacketHandled(true);
            context.enqueueWork(() -> {
                Player player = context.getSender();
                if (context.isClientSide()) {
                    player = AlexsMobs.PROXY.getClientSidePlayer();
                }

                if (player != null) {
                    if (player.level() != null) {
                        Entity entity = player.level().getEntity(message.hawk);
                        Entity spider = player.level().getEntity(message.spider);
                        if (entity instanceof EntityTarantulaHawk && spider instanceof LivingEntity && AMCompat.isArthropod(((LivingEntity) spider))) {
                            ((LivingEntity) spider).addEffect(new MobEffectInstance(AMCompat.effect(AMEffectRegistry.DEBILITATING_STING.get()), EntityTarantulaHawk.STING_DURATION));
                        }
                    }
                }
            });
        }
    }
}
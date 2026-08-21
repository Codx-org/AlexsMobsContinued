package com.github.alexthe666.alexsmobs.message;

import com.github.alexthe666.alexsmobs.misc.AMCompat;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.entity.IHurtableMultipart;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.function.Supplier;

public class MessageHurtMultipart {

    public int part;
    public int parent;
    public float damage;
    public String damageType;

    public MessageHurtMultipart(int part, int parent, float damage) {
        this.part = part;
        this.parent = parent;
        this.damage = damage;
        this.damageType = "";
    }

    public MessageHurtMultipart(int part, int parent, float damage, String damageType) {
        this.part = part;
        this.parent = parent;
        this.damage = damage;
        this.damageType = damageType;
    }

    public MessageHurtMultipart() {
    }

    public static MessageHurtMultipart read(FriendlyByteBuf buf) {
        return new MessageHurtMultipart(buf.readInt(), buf.readInt(), buf.readFloat(), buf.readUtf());
    }

    public static void write(MessageHurtMultipart message, FriendlyByteBuf buf) {
        buf.writeInt(message.part);
        buf.writeInt(message.parent);
        buf.writeFloat(message.damage);
        buf.writeUtf(message.damageType);
    }

    public static class Handler {
        public Handler() {
        }

        public static void handle(MessageHurtMultipart message, AMNetContext context) {
            context.setPacketHandled(true);
            context.enqueueWork(() -> {
                Player player = context.getSender();
                if (context.isClientSide()) {
                    player = AlexsMobs.PROXY.getClientSidePlayer();
                }

                if (player != null) {
                    if (player.level() != null) {
                        Entity part = player.level().getEntity(message.part);
                        Entity parent = player.level().getEntity(message.parent);
                        Holder<DamageType> holder = AMCompat.damageTypeHolder(player.level(), message.damageType);
                        if (holder != null) {
                            DamageSource source = new DamageSource(holder);
                            if (part instanceof IHurtableMultipart && parent instanceof LivingEntity) {
                                ((IHurtableMultipart) part).onAttackedFromServer((LivingEntity) parent, message.damage, source);
                            }
                            if (part == null && parent != null && AMCompat.isMultipart(parent)) {
                                parent.hurt(source, message.damage);
                            }
                        }

                        //? if fabric {
                        /*// Fabric-only. Parts live in no level's entity storage on ANY loader; Forge
                        // and NeoForge patch getEntity to reach their side map, Fabric has no such
                        // map, so a part id never resolves here. FabricMultiPlayerGameModeMixin
                        // therefore cancels the vanilla attack and sends this instead, carrying no
                        // damage type — and `holder == null` is already how this handler spells
                        // "nothing here is a damage relay", so no sentinel value has to be invented.
                        // Run a real player attack on the parent: cooldown, knockback, enchantments
                        // and sweep all apply, and no client-supplied damage number is trusted.
                        //
                        // ⚠️ GATED because this file is shared source on all 49 nodes and the C2S
                        // path is ALREADY live on Forge — EntityCachalotPart and EntityGiantSquidPart
                        // send real damage and a real damage type, and there the part id does
                        // resolve. Ungating would also change the Forge/NeoForge "part despawned
                        // between send and receive" fallback directly above.
                        if (holder == null && !context.isClientSide()
                                && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                            Entity parentEntity = serverPlayer.level().getEntity(message.parent);
                            if (parentEntity != null && AMCompat.isMultipart(parentEntity)) {
                                serverPlayer.attack(parentEntity);
                            }
                        }
                        *///?}
                    }
                }
            });
        }
    }
}
package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.misc.AMCompat;

import com.github.alexthe666.alexsmobs.misc.AMPlatform;

import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class EntitySharkToothArrow extends Arrow {

    public EntitySharkToothArrow(EntityType type, Level worldIn) {
        super(type, worldIn);
    }

    public EntitySharkToothArrow(EntityType type, double x, double y, double z, Level worldIn) {
        this(type, worldIn);
        this.setPos(x, y, z);
    }

    public EntitySharkToothArrow(Level worldIn, LivingEntity shooter) {
        this(AMEntityRegistry.SHARK_TOOTH_ARROW.get(), shooter.getX(), shooter.getEyeY() - (double)0.1F, shooter.getZ(), worldIn);
        this.setOwner(shooter);
        if (shooter instanceof Player) {
            this.pickup = AbstractArrow.Pickup.ALLOWED;
        }
    }

    // 1.20.5 folded setEffectsFromItem away: an arrow's potion contents now ride on the ammo
    // stack it was fired with, which vanilla hands to the constructor.
    //? if >=1.20.5 {
    /*public EntitySharkToothArrow(Level worldIn, LivingEntity shooter, ItemStack ammo) {
        this(worldIn, shooter);
        this.setPickupItemStack(ammo);
    }
    *///?}

    // 1.21.5 made AbstractArrow#baseDamage private with no getter; capture it as vanilla sets it.
    //? if >=1.21.5 {
    /*private double amBaseDamage = 2.0D;

    public void setBaseDamage(double d) {
        super.setBaseDamage(d);
        this.amBaseDamage = d;
    }
    *///?}

    protected void damageShield(Player player, float damage) {
        if (damage >= 3.0F && AMCompat.canShieldBlock(player.getUseItem())) {
            ItemStack copyBeforeUse = player.getUseItem().copy();
            int i = 1 + Mth.floor(damage);
            AMCompat.hurtAndBreak(player.getUseItem(), i, player, EquipmentSlot.CHEST);

            if (player.getUseItem().isEmpty()) {
                InteractionHand Hand = player.getUsedItemHand();
                // Fabric has no item-destroyed event.
                //? if !fabric
                net.minecraftforge.event.ForgeEventFactory.onPlayerDestroyItem(player, copyBeforeUse, Hand);

                // Upstream called this on the arrow, where Entity#setItemSlot was an inherited
                // no-op; 1.20.5 moved that method to LivingEntity. The player is plainly the
                // intended target (this is vanilla's broken-shield cleanup) and the slot is
                // already an empty stack by now, so clearing it here is equivalent.
                if (Hand == net.minecraft.world.InteractionHand.MAIN_HAND) {
                    player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                } else {
                    player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                }
                player.stopUsingItem();
                this.playSound(SoundEvents.SHIELD_BREAK, 0.8F, 0.8F + this.level().getRandom().nextFloat() * 0.4F);
            }
        }
    }

    protected void doPostHurtEffects(LivingEntity living) {
        if (living instanceof Player) {
            //? if >=1.21.5 {
            /*this.damageShield((Player) living, (float) this.amBaseDamage);
            *///?} else {
            this.damageShield((Player) living, (float) this.getBaseDamage());
            //?}
        }
        Entity entity1 = this.getOwner();
        if(AMCompat.isAquatic(living) || living instanceof Drowned || !AMCompat.isUndead(living) && living.canBreatheUnderwater()){
            DamageSource damagesource;
            if (entity1 == null) {
                damagesource = damageSources().arrow(this, this);
            } else {
                damagesource = damageSources().arrow(this, entity1);
            }
            living.hurt(damagesource, 7);
        }
    }


    public boolean isInWater() {
        return false;
    }


    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return AMPlatform.getEntitySpawningPacket(this);
    }


    @Override
    protected ItemStack getPickupItem() {
        return new ItemStack(AMItemRegistry.SHARK_TOOTH_ARROW.get());
    }

}

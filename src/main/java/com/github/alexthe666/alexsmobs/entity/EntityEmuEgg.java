package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.misc.AMPlatform;

import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.github.alexthe666.alexsmobs.misc.AMCompat;

public class EntityEmuEgg extends ThrowableItemProjectile {

    public EntityEmuEgg(EntityType p_i50154_1_, Level p_i50154_2_) {
        super(p_i50154_1_, p_i50154_2_);
    }

    public EntityEmuEgg(Level worldIn, LivingEntity throwerIn) {
        //? if >=1.21.2 {
        /*super(AMEntityRegistry.EMU_EGG.get(), throwerIn, worldIn, new net.minecraft.world.item.ItemStack(AMItemRegistry.EMU_EGG.get()));
        *///?} else {
        super(AMEntityRegistry.EMU_EGG.get(), throwerIn, worldIn);
        //?}
    }

    public EntityEmuEgg(Level worldIn, double x, double y, double z) {
        //? if >=1.21.2 {
        /*super(AMEntityRegistry.EMU_EGG.get(), x, y, z, worldIn, new net.minecraft.world.item.ItemStack(AMItemRegistry.EMU_EGG.get()));
        *///?} else {
        super(AMEntityRegistry.EMU_EGG.get(), x, y, z, worldIn);
        //?}
    }


    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return AMPlatform.getEntitySpawningPacket(this);
    }

    @OnlyIn(value = Dist.CLIENT)
    public void handleEntityEvent(byte id) {
        if (id == 3) {
            for (int i = 0; i < 8; ++i) {
                this.level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, this.getItem()), this.getX(), this.getY(), this.getZ(), ((double) this.random.nextFloat() - 0.5D) * 0.08D, ((double) this.random.nextFloat() - 0.5D) * 0.08D, ((double) this.random.nextFloat() - 0.5D) * 0.08D);
            }
        }

    }

    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide()) {
            if (this.random.nextInt(8) == 0) {
                int lvt_2_1_ = 1;
                if (this.random.nextInt(32) == 0) {
                    lvt_2_1_ = 4;
                }
                for (int lvt_3_1_ = 0; lvt_3_1_ < lvt_2_1_; ++lvt_3_1_) {
                    EntityEmu lvt_4_1_ = AMCompat.create(AMEntityRegistry.EMU.get(), this.level());
                    if(this.random.nextInt(50) == 0){
                        lvt_4_1_.setVariant(2);
                    }else if(random.nextInt(3) == 0){
                        lvt_4_1_.setVariant(1);
                    }
                    lvt_4_1_.setAge(-24000);
                    lvt_4_1_.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
                    this.level().addFreshEntity(lvt_4_1_);
                }
            }
            this.level().broadcastEntityEvent(this, (byte) 3);
            this.remove(RemovalReason.DISCARDED);
        }

    }

    protected Item getDefaultItem() {
        return AMItemRegistry.EMU_EGG.get();
    }
}

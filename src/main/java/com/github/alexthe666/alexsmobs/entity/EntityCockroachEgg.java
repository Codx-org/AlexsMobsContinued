package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.misc.AMPlatform;

import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.github.alexthe666.alexsmobs.misc.AMCompat;

public class EntityCockroachEgg extends ThrowableItemProjectile {

    public EntityCockroachEgg(EntityType p_i50154_1_, Level p_i50154_2_) {
        super(p_i50154_1_, p_i50154_2_);
    }

    public EntityCockroachEgg(Level worldIn, LivingEntity throwerIn) {
        //? if >=1.21.2 {
        /*super(AMEntityRegistry.COCKROACH_EGG.get(), throwerIn, worldIn, new net.minecraft.world.item.ItemStack(AMItemRegistry.COCKROACH_OOTHECA.get()));
        *///?} else {
        super(AMEntityRegistry.COCKROACH_EGG.get(), throwerIn, worldIn);
        //?}
    }

    public EntityCockroachEgg(Level worldIn, double x, double y, double z) {
        //? if >=1.21.2 {
        /*super(AMEntityRegistry.COCKROACH_EGG.get(), x, y, z, worldIn, new net.minecraft.world.item.ItemStack(AMItemRegistry.COCKROACH_OOTHECA.get()));
        *///?} else {
        super(AMEntityRegistry.COCKROACH_EGG.get(), x, y, z, worldIn);
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
                this.level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, this.getItem()), this.getX(), this.getY(), this.getZ(), ((double)this.random.nextFloat() - 0.5D) * 0.08D, ((double)this.random.nextFloat() - 0.5D) * 0.08D, ((double)this.random.nextFloat() - 0.5D) * 0.08D);
            }
        }

    }

    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide()) {
            this.level().broadcastEntityEvent(this, (byte)3);
            int i = random.nextInt(3);
            for (int j = 0; j < i; ++j) {
                final EntityCockroach croc = AMCompat.create(AMEntityRegistry.COCKROACH.get(), this.level());
                croc.setAge(-24000);
                croc.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
                //? if >=1.20.5 {
                /*croc.finalizeSpawn((ServerLevel)level(), AMCompat.difficultyAt(level(), this.blockPosition()), MobSpawnType.TRIGGERED, (SpawnGroupData)null);
                *///?} else {
                croc.finalizeSpawn((ServerLevel)level(), AMCompat.difficultyAt(level(), this.blockPosition()), MobSpawnType.TRIGGERED, (SpawnGroupData)null, (CompoundTag)null);
                //?}
                croc.restrictTo(this.blockPosition(), 20);
                this.level().addFreshEntity(croc);
            }
            this.level().broadcastEntityEvent(this, (byte)3);
            this.remove(RemovalReason.DISCARDED);
        }

    }

    protected Item getDefaultItem() {
        return AMItemRegistry.COCKROACH_OOTHECA.get();
    }
}

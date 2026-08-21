package com.github.alexthe666.alexsmobs.mixin;
import com.github.alexthe666.alexsmobs.misc.AMCompat;

import com.github.alexthe666.alexsmobs.citadel.server.entity.ICitadelDataEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Backs the bundled Citadel entity-data store: a synched, persisted {@link CompoundTag} on every
 * living entity. Vendored from Citadel (LGPL-3.0-only).
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements ICitadelDataEntity, com.github.alexthe666.alexsmobs.misc.IEntitySaveDataAccessor {

    // On neoforge >=1.21.8 the store is a data attachment instead — see AMCitadelDataAttachment for
    // why. There, this accessor, its defineSynchedData inject and the two CitadelData save hooks
    // below are all absent; the platform serializes and syncs the attachment on its own.
    // 1.21.9 also deleted EntityDataSerializers.COMPOUND_TAG, so Forge takes AMCompat's rebuilt one
    // (NeoForge is on the attachment by then and needs no serializer at all).
    //
    // The `neoforge` arm is the ONLY one that opts out, so every gate in this file spells the other
    // side as `forge || fabric`. It used to say just `forge`, which silently switched the whole
    // store off on the seven Fabric nodes >=1.21.8: the two ICitadelDataEntity methods vanished
    // from an abstract mixin (so it still compiled) and six items — vine lasso, tendon whip, squid
    // grapple, rainbow dye, rocky chestplate, flying-fish boots — would have hit AbstractMethodError
    // the first time they were used. Attachments are a NeoForge mechanism; Fabric belongs on the
    // synched-data path with Forge.
    //? if (forge || fabric) && >=1.21.9 {
    /*private static final EntityDataAccessor<CompoundTag> ALEXSMOBS_CITADEL_DATA = SynchedEntityData.defineId(LivingEntity.class, AMCompat.COMPOUND_TAG);
    *///?} elif forge || fabric || <1.21.8 {
    private static final EntityDataAccessor<CompoundTag> ALEXSMOBS_CITADEL_DATA = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.COMPOUND_TAG);
    //?}

    protected LivingEntityMixin(EntityType<? extends Entity> entityType, Level world) {
        super(entityType, world);
    }

    // 1.20.5 moved synched-data registration onto a builder passed into defineSynchedData.
    //? if >=1.20.5 && (forge || fabric || <1.21.8) {
    /*@Inject(at = @At("TAIL"), method = "Lnet/minecraft/world/entity/LivingEntity;defineSynchedData(Lnet/minecraft/network/syncher/SynchedEntityData$Builder;)V")
    private void alexsmobs_registerCitadelData(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(ALEXSMOBS_CITADEL_DATA, new CompoundTag());
    }
    *///?} elif <1.20.5 {
    @Inject(at = @At("TAIL"), method = "Lnet/minecraft/world/entity/LivingEntity;defineSynchedData()V")
    private void alexsmobs_registerCitadelData(CallbackInfo ci) {
        entityData.define(ALEXSMOBS_CITADEL_DATA, new CompoundTag());
    }
    //?}

    // 1.21.6 swapped the CompoundTag parameter of both save hooks for ValueOutput / ValueInput,
    // which changes the injection target's descriptor as well as the handler's signature. The
    // interfaces have no put(String, Tag) / getCompound, so the nested store goes through a codec.
    //? if >=1.21.6 && (forge || fabric || <1.21.8) {
    /*@Inject(at = @At("TAIL"), method = "Lnet/minecraft/world/entity/LivingEntity;addAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueOutput;)V")
    private void alexsmobs_writeCitadelData(net.minecraft.world.level.storage.ValueOutput output, CallbackInfo ci) {
        CompoundTag citadelDat = getCitadelEntityData();
        if (citadelDat != null) {
            output.store("CitadelData", CompoundTag.CODEC, citadelDat);
        }
    }

    @Inject(at = @At("TAIL"), method = "Lnet/minecraft/world/entity/LivingEntity;readAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueInput;)V")
    private void alexsmobs_readCitadelData(net.minecraft.world.level.storage.ValueInput input, CallbackInfo ci) {
        input.read("CitadelData", CompoundTag.CODEC).ifPresent(this::setCitadelEntityData);
    }
    *///?} elif <1.21.6 {
    @Inject(at = @At("TAIL"), method = "Lnet/minecraft/world/entity/LivingEntity;addAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V")
    private void alexsmobs_writeCitadelData(CompoundTag compoundNBT, CallbackInfo ci) {
        CompoundTag citadelDat = getCitadelEntityData();
        if (citadelDat != null) {
            compoundNBT.put("CitadelData", citadelDat);
        }
    }

    @Inject(at = @At("TAIL"), method = "Lnet/minecraft/world/entity/LivingEntity;readAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V")
    private void alexsmobs_readCitadelData(CompoundTag compoundNBT, CallbackInfo ci) {
        if (compoundNBT.contains("CitadelData")) {
            setCitadelEntityData(AMCompat.getCompound(compoundNBT, "CitadelData"));
        }
    }
    //?}

    // AMCompat#saveAdditionalTo / #readAdditionalFrom route through these. See
    // IEntitySaveDataAccessor for why they live in the mixin rather than behind an AT.
    //? if >=1.21.6 {
    /*@Override
    public void am_writeSaveData(CompoundTag tag) {
        net.minecraft.world.level.storage.TagValueOutput out = net.minecraft.world.level.storage.TagValueOutput
                .createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, this.registryAccess());
        this.addAdditionalSaveData(out);
        tag.merge(out.buildResult());
    }

    @Override
    public void am_readSaveData(CompoundTag tag) {
        this.readAdditionalSaveData(net.minecraft.world.level.storage.TagValueInput
                .create(net.minecraft.util.ProblemReporter.DISCARDING, this.registryAccess(), tag));
    }
    *///?} else {
    @Override
    public void am_writeSaveData(CompoundTag tag) {
        this.addAdditionalSaveData(tag);
    }

    @Override
    public void am_readSaveData(CompoundTag tag) {
        this.readAdditionalSaveData(tag);
    }
    //?}

    //? if neoforge && >=1.21.8 {
    /*public CompoundTag getCitadelEntityData() {
        return com.github.alexthe666.alexsmobs.misc.AMCitadelDataAttachment.get(this);
    }

    public void setCitadelEntityData(CompoundTag nbt) {
        com.github.alexthe666.alexsmobs.misc.AMCitadelDataAttachment.set(this, nbt);
    }
    *///?} elif forge || fabric || <1.21.8 {
    public CompoundTag getCitadelEntityData() {
        return entityData.get(ALEXSMOBS_CITADEL_DATA);
    }

    public void setCitadelEntityData(CompoundTag nbt) {
        entityData.set(ALEXSMOBS_CITADEL_DATA, nbt);
    }
    //?}
}

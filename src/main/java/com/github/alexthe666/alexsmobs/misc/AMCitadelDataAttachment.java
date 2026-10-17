package com.github.alexthe666.alexsmobs.misc;

// NeoForge 21.8 added CommonHooks#verifyEntityDataAccessorRegistration, called from
// SynchedEntityData#defineId. It rejects any EntityDataAccessor that a MIXIN added to an entity
// class the caller does not own — which is exactly what the vendored Citadel entity-data store is
// (LivingEntityMixin merges a static EntityDataAccessor<CompoundTag> into LivingEntity). The check
// finds the merged field by its @MixinMerged annotation, so there is no way to keep a synched
// accessor there; it throws IllegalStateException when SharedConstants.IS_RUNNING_IN_IDE and
// otherwise logs a WARN, i.e. it kills any dev launch and merely nags in production.
//
// NeoForge's sanctioned replacement is a data attachment, which is strictly better here: it is
// serialized and synced by the platform, and it carries no cross-side numeric id to desync.
// So on neoforge >=1.21.8 the store becomes an AttachmentType and LivingEntityMixin drops its
// accessor, its defineSynchedData inject and its two CitadelData save hooks. Every other node
// keeps the SynchedEntityData implementation unchanged — Forge has no attachments, and reworking
// 21 already-verified nodes to fix one would be the wrong trade.
//? if neoforge && >=1.21.8 {
/*import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class AMCitadelDataAttachment {

    public static final DeferredRegister<AttachmentType<?>> DEF_REG =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, "alexsmobs");

    // serialize's predicate skips empty tags, so entities that never touch the store (i.e. almost
    // all of them) add nothing to the save. sync() makes setData push to tracking players by
    // itself, and NeoForge sends the current value when a player starts tracking the entity.
    public static final Supplier<AttachmentType<CompoundTag>> CITADEL_DATA = DEF_REG.register(
            "citadel_data",
            // An explicit zero-arg lambda, not CompoundTag::new — the method reference fits both
            // builder(Supplier<T>) and builder(Function<IAttachmentHolder, T>) and is ambiguous.
            () -> AttachmentType.<CompoundTag>builder(() -> new CompoundTag())
                    .serialize(CompoundTag.CODEC.fieldOf("data"), tag -> !tag.isEmpty())
                    .sync(ByteBufCodecs.COMPOUND_TAG)
                    .build());

    public static CompoundTag get(Entity entity) {
        return entity.getData(CITADEL_DATA);
    }

    // Entity#syncData returns early off a ServerLevel, so a client-side set (CitadelClientProxy and
    // the clientbound half of PropertiesMessage both do this) stays local — the same behaviour
    // SynchedEntityData#set had.
    public static void set(Entity entity, CompoundTag tag) {
        entity.setData(CITADEL_DATA, tag);
    }
}
*///?}

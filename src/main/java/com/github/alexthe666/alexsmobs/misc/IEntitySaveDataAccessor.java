package com.github.alexthe666.alexsmobs.misc;

import net.minecraft.nbt.CompoundTag;

/**
 * Public access to {@code LivingEntity}'s protected save/load hooks, implemented by
 * {@code LivingEntityMixin}.
 *
 * <p>A handful of places here drive an entity's save/load by hand with a CompoundTag they own
 * (bucketable fish, the mobs that stash a swallowed entity, the "clear DeathLootTable so this kill
 * drops nothing" trick). {@code addAdditionalSaveData}/{@code readAdditionalSaveData} are
 * {@code protected}, and whether the loader's own access transformer happens to widen them varies
 * by loader and version — NeoForge 1.21.6 does not. Calling them from inside a mixin on
 * LivingEntity is plain intra-class access, so it needs no access transformer at all, and it is
 * also where the 1.21.6 ValueOutput/ValueInput adaptation belongs.
 *
 * @see AMCompat#saveAdditionalTo
 * @see AMCompat#readAdditionalFrom
 */
public interface IEntitySaveDataAccessor {

    void am_writeSaveData(CompoundTag tag);

    void am_readSaveData(CompoundTag tag);
}

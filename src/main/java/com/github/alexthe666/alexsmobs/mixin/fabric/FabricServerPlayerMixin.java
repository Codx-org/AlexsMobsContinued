package com.github.alexthe666.alexsmobs.mixin.fabric;

import com.github.alexthe666.alexsmobs.misc.AMCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Carries the persistent player tag across a respawn. Fabric-only — see {@link FabricLivingEntityMixin}
 * for why this package is excluded and pruned on the other loaders.
 *
 * <p>This is not an event hook like the rest of the package; it closes a gap in
 * {@link AMCompat#getPersistentData}. Forge patches {@code restoreFrom} to copy the
 * {@code PERSISTED_NBT_TAG} sub-tag from the dying player onto the fresh one, unconditionally —
 * that is the whole reason the sub-tag exists. Fabric's stand-in store is the vendored Citadel
 * {@code LivingEntity} tag, which is saved and loaded but belongs to the old entity, so without this
 * a player who dies is handed a second Animal Dictionary on their next login.
 *
 * <p>Copies only that one sub-tag, matching Forge exactly: the rest of the Citadel tag is per-entity
 * state that is meant to die with the entity. Runs regardless of {@code keepEverything} for the same
 * reason Forge's does — surviving a death is the point.
 *
 * <p>{@code restoreFrom(ServerPlayer, boolean)} is byte-identical on all 17 Fabric nodes, so no arms.
 * TAIL is safe: vanilla's body never reloads the new player from NBT, it only copies fields across.
 */
@Mixin(ServerPlayer.class)
public abstract class FabricServerPlayerMixin {

    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void alexsmobs$copyPersistentData(ServerPlayer that, boolean keepEverything, CallbackInfo ci) {
        CompoundTag old = AMCompat.getPersistentData(that);
        if (AMCompat.contains(old, AMCompat.PERSISTED_NBT_TAG)) {
            CompoundTag fresh = AMCompat.getPersistentData((ServerPlayer) (Object) this);
            AMCompat.put(fresh, AMCompat.PERSISTED_NBT_TAG, AMCompat.getCompound(old, AMCompat.PERSISTED_NBT_TAG));
        }
    }
}

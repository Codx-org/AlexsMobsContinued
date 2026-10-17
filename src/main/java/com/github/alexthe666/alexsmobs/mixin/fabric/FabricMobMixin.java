package com.github.alexthe666.alexsmobs.mixin.fabric;

import com.github.alexthe666.alexsmobs.fabric.event.FabricServerEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The three {@code Mob}-scoped hooks: retargeting, despawning and spawn finalisation. Fabric-only —
 * see {@link FabricLivingEntityMixin} for why the package is excluded and pruned elsewhere, and
 * {@code FabricServerEvents} for what each hook is for.
 */
@Mixin(Mob.class)
public abstract class FabricMobMixin {

    // Cancelling here means the field is never assigned, which is exactly Forge's veto: the mob
    // keeps whatever target it had. Both listeners already exempt a mob that was actually struck.
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void alexsmobs$changeTarget(LivingEntity target, CallbackInfo ci) {
        if (FabricServerEvents.fireChangeTarget((Mob) (Object) this, target)) {
            ci.cancel();
        }
    }

    @Inject(method = "checkDespawn", at = @At("HEAD"), cancellable = true)
    private void alexsmobs$allowDespawn(CallbackInfo ci) {
        if (FabricServerEvents.fireAllowDespawn((Mob) (Object) this)) {
            ci.cancel();
        }
    }

    // Two arms, not three: 1.20.5 dropped the trailing CompoundTag, and 1.21.2's rename of
    // MobSpawnType to EntitySpawnReason is done for us by the !mc2102-spawnreason replacement rule
    // (which rewrites the bare type name everywhere in the tree), so the >=1.20.5 arm covers both
    // of the later eras with one spelling.
    //? if >=1.20.5 {
    /*@Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void alexsmobs$finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, SpawnGroupData data, CallbackInfoReturnable<SpawnGroupData> cir) {
        FabricServerEvents.fireFinalizeSpawn((Mob) (Object) this, level);
    }
    *///?} else {
    @Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void alexsmobs$finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, SpawnGroupData data, net.minecraft.nbt.CompoundTag tag, CallbackInfoReturnable<SpawnGroupData> cir) {
        FabricServerEvents.fireFinalizeSpawn((Mob) (Object) this, level);
    }
    //?}
}

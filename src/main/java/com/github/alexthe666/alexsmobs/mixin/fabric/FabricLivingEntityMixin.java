package com.github.alexthe666.alexsmobs.mixin.fabric;

import com.github.alexthe666.alexsmobs.fabric.event.FabricServerEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The four {@code LivingEntity}-scoped hooks Forge gives this mod for free and Fabric has no
 * callback for. Every method here does nothing but call {@code FabricServerEvents} — see that class
 * for what each hook means and {@code docs/notes/fabric.md} for the divergences.
 *
 * <p><b>This whole package is Fabric-only.</b> It is excluded from the compile on Forge/NeoForge
 * (which fire all of this from their own event bus) and pruned back out of {@code
 * alexsmobs.mixins.json} there, because Fletching Table's {@code @Mixin} scan ignores source-set
 * excludes and a config naming an absent class is a hard load failure.
 *
 * <p>Selectors are <b>name-only</b> on purpose: none of these method names is overloaded in its
 * declaring class on any node from 1.20.1 to 26.2 (javap-verified across all 17 Fabric nodes), and
 * a name-only selector still remaps correctly into intermediary on the 15 obfuscated ones. Where a
 * signature genuinely changed the handler is split into Stonecutter arms below — the parameter list
 * of an {@code @Inject} handler has to match the target's, and {@code defaultRequire: 1} turns a
 * mismatch into a crash at mixin apply that no compiler and no server gate can see.
 */
@Mixin(LivingEntity.class)
public abstract class FabricLivingEntityMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void alexsmobs$livingTick(CallbackInfo ci) {
        FabricServerEvents.fireLivingTick((LivingEntity) (Object) this);
    }

    // 1.21.2 split hurt() into hurtServer/hurtClient and threaded the ServerLevel through. The
    // pre-mitigation hook is the server half on both sides of that line: Forge fires
    // LivingAttackEvent from the one place a hit is decided, which is never the client.
    //? if >=1.21.2 {
    /*@Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void alexsmobs$livingAttack(net.minecraft.server.level.ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (FabricServerEvents.fireLivingAttack((LivingEntity) (Object) this, source, amount)) {
            cir.setReturnValue(false);
        }
    }
    *///?} else {
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void alexsmobs$livingAttack(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (FabricServerEvents.fireLivingAttack((LivingEntity) (Object) this, source, amount)) {
            cir.setReturnValue(false);
        }
    }
    //?}

    // ⚠️ HEAD, so `amount` is the raw incoming damage — Forge fires LivingDamageEvent further in,
    // after armour and magic absorption. The two cancelling listeners (mimic octopus, emu leggings)
    // are boolean decisions and are unaffected; Soulsteal heals from the amount, so on Fabric it
    // heals off the pre-armour figure. Its result is clamped to `2 + 2 * level` either way, which
    // is what keeps the divergence small. Recorded in docs/notes/fabric.md.
    //? if >=1.21.2 {
    /*@Inject(method = "actuallyHurt", at = @At("HEAD"), cancellable = true)
    private void alexsmobs$livingDamage(net.minecraft.server.level.ServerLevel level, DamageSource source, float amount, CallbackInfo ci) {
        if (FabricServerEvents.fireLivingDamage((LivingEntity) (Object) this, source, amount)) {
            ci.cancel();
        }
    }
    *///?} else {
    @Inject(method = "actuallyHurt", at = @At("HEAD"), cancellable = true)
    private void alexsmobs$livingDamage(DamageSource source, float amount, CallbackInfo ci) {
        if (FabricServerEvents.fireLivingDamage((LivingEntity) (Object) this, source, amount)) {
            ci.cancel();
        }
    }
    //?}

    // HEAD rather than TAIL: vanilla shrinks or replaces the stack in this method, and the handler
    // needs to see what was actually finished (a chorus fruit) rather than what is left.
    @Inject(method = "completeUsingItem", at = @At("HEAD"))
    private void alexsmobs$useItemFinish(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        FabricServerEvents.fireUseItemFinish(self, self.getUseItem());
    }

    // 1.21 threaded the ServerLevel into dropAllDeathLoot. TAIL on both arms, so the mod's extra
    // drop lands alongside the loot table's rather than ahead of it.
    //? if >=1.21 {
    /*@Inject(method = "dropAllDeathLoot", at = @At("TAIL"))
    private void alexsmobs$livingDrops(net.minecraft.server.level.ServerLevel level, DamageSource source, CallbackInfo ci) {
        FabricServerEvents.fireLivingDrops((LivingEntity) (Object) this, source);
    }
    *///?} else {
    @Inject(method = "dropAllDeathLoot", at = @At("TAIL"))
    private void alexsmobs$livingDrops(DamageSource source, CallbackInfo ci) {
        FabricServerEvents.fireLivingDrops((LivingEntity) (Object) this, source);
    }
    //?}
}

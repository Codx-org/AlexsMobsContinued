package com.github.alexthe666.alexsmobs.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies the Earthquake effect's camera shake on <b>NeoForge {@code >=1.21}</b>, where the event the
 * mod normally uses cannot carry it.
 *
 * <p><b>Why this exists.</b> {@code ClientEvents#onCameraSetup} answers
 * {@code ViewportEvent.ComputeCameraAngles} and calls {@code Camera#move(...)}. Forge posts that
 * event from {@code GameRenderer}, <i>after</i> {@code Camera#setup} has finished, so the move
 * sticks. From NeoForge 21.0 (MC 1.21) the post was moved <i>into</i> {@code Camera#setup} itself —
 * read out of every NeoForge sources jar in the matrix, absent on 1.20.4/1.20.6 and present from
 * 1.21 on — and it sits on the line immediately <b>before</b>
 * {@code this.setPosition(Mth.lerp(...), ...)}, which recomputes the position from the entity and
 * throws away anything {@code move()} did. The event's yaw/pitch/roll are still honoured (they are
 * consumed by the {@code setRotation} call one line earlier); only a positional nudge is lost. So on
 * those 14 nodes the shake has been silently discarded every frame since the nodes existed — the
 * effect applied, the screen never moved (#106).
 *
 * <p><b>Why the target is {@code Camera}</b> and not {@code GameRenderer}: same reasoning as
 * {@code mixin/fabric/client/FabricCameraMixin}, which does the equivalent job for the whole event.
 * The caller moves three times across the range while the callee stays put, and
 * {@code Camera#setup}/{@code #update} has exactly one caller in the client on every version.
 *
 * <p><b>Why TAIL.</b> The point is to run after the camera has its final position. Each targeted
 * body has exactly one {@code return} (javap-checked on the NeoForge-patched jars), so TAIL is the
 * end of the method and not one branch of it.
 *
 * <p>Empty on every other node — Forge and NeoForge {@code <1.21} fire the event late enough, and
 * Fabric's own camera mixin already injects at TAIL.
 */
@Mixin(net.minecraft.client.Camera.class)
public abstract class CameraMixin {

    // Three arms, and they are siblings rather than nested because Stonecutter blocks never nest.
    // The level parameter widens BlockGetter -> Level at 1.21.11; at 26 setup is gone and the work
    // moved into the private alignWithEntity(float), which update(DeltaTracker) calls. 26 is
    // targeted at alignWithEntity rather than at update on purpose: update goes on to build the cull
    // frustum from this.position, and vanilla's own move() calls (detached camera, sleeping) are
    // inside alignWithEntity too, so that is where a positional nudge belongs.

    //? if neoforge && >=1.21 && <1.21.11 {
    /*@Inject(method = "setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V",
            at = @At("TAIL"))
    private void alexsmobs$earthquakeShake(net.minecraft.world.level.BlockGetter level,
                                           net.minecraft.world.entity.Entity entity,
                                           boolean detached,
                                           boolean thirdPersonReverse,
                                           float partialTick,
                                           CallbackInfo ci) {
        com.github.alexthe666.alexsmobs.client.event.ClientEvents.applyEarthquakeShake(
                (net.minecraft.client.Camera) (Object) this);
    }
    *///?}

    //? if neoforge && >=1.21.11 && <26 {
    /*@Inject(method = "setup(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;ZZF)V",
            at = @At("TAIL"))
    private void alexsmobs$earthquakeShake(net.minecraft.world.level.Level level,
                                           net.minecraft.world.entity.Entity entity,
                                           boolean detached,
                                           boolean thirdPersonReverse,
                                           float partialTick,
                                           CallbackInfo ci) {
        com.github.alexthe666.alexsmobs.client.event.ClientEvents.applyEarthquakeShake(
                (net.minecraft.client.Camera) (Object) this);
    }
    *///?}

    //? if neoforge && >=26 {
    /*@Inject(method = "alignWithEntity(F)V", at = @At("TAIL"))
    private void alexsmobs$earthquakeShake(float partialTick, CallbackInfo ci) {
        com.github.alexthe666.alexsmobs.client.event.ClientEvents.applyEarthquakeShake(
                (net.minecraft.client.Camera) (Object) this);
    }
    *///?}
}

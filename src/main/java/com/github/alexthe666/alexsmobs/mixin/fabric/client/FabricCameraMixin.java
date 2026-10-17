package com.github.alexthe666.alexsmobs.mixin.fabric.client;

import com.github.alexthe666.alexsmobs.fabric.client.FabricClientEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires {@code ViewportEvent.ComputeCameraAngles} once per frame, right after vanilla has positioned
 * the camera — the same moment Forge fires it. Drives two unrelated things, which is why one hook
 * pays for both: the Earthquake effect's camera shake ({@code ClientEvents#onCameraSetup}) and the
 * per-frame state sweep in {@code doWorldLastFrame()} (bald-eagle camera return, lava-vision chunk
 * refresh), which Forge {@code >=1.21.3} already moved onto this same event when
 * {@code RenderLevelStageEvent} was deleted.
 *
 * <p><b>Why the target is {@code Camera} and not {@code GameRenderer}.</b> The call site moves three
 * times across the range — {@code GameRenderer#renderLevel} up to 1.21.9, {@code #updateCamera} at
 * 1.21.11, {@code #update} at 26 — while the callee stays put. Verified by scanning every
 * {@code net/minecraft/client/**} class in each era's jar: {@code Camera#setup}/{@code #update} has
 * <b>exactly one caller in the whole client</b>, {@code GameRenderer}, on every version checked. So
 * injecting into the callee is both once-per-frame and immune to the caller moving.
 */
@Mixin(net.minecraft.client.Camera.class)
public abstract class FabricCameraMixin {

    // ── Descriptors, and why TAIL ──
    // Three arms: the level parameter widens BlockGetter -> Level at 1.21.11, and at 26 the whole
    // method is gone in favour of update(DeltaTracker) (Camera stopped being handed the world and
    // reads it from a field it now owns). Full descriptors as usual — cheap here, and they make
    // verify_mixin_targets.py prove each arm against the node's real bytecode.
    //
    // TAIL, not RETURN: nothing else injects into this method, but the whole point is to run AFTER
    // the camera has its final position, and TAIL is the only @At that means that. Confirmed by
    // javap on 1.20.4 → 26.2 that the body has exactly ONE return, so TAIL is the end of the body
    // and not one branch of it.

    //? if <1.21.11 {
    @Inject(method = "setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V",
            at = @At("TAIL"))
    private void alexsmobs$computeCameraAngles(net.minecraft.world.level.BlockGetter level,
                                               net.minecraft.world.entity.Entity entity,
                                               boolean detached,
                                               boolean thirdPersonReverse,
                                               float partialTick,
                                               CallbackInfo ci) {
        FabricClientEvents.fireComputeCameraAngles((net.minecraft.client.Camera) (Object) this, partialTick);
    }
    //?}

    //? if >=1.21.11 && <26 {
    /*@Inject(method = "setup(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;ZZF)V",
            at = @At("TAIL"))
    private void alexsmobs$computeCameraAngles(net.minecraft.world.level.Level level,
                                               net.minecraft.world.entity.Entity entity,
                                               boolean detached,
                                               boolean thirdPersonReverse,
                                               float partialTick,
                                               CallbackInfo ci) {
        FabricClientEvents.fireComputeCameraAngles((net.minecraft.client.Camera) (Object) this, partialTick);
    }
    *///?}

    //? if >=26 {
    /*@Inject(method = "update(Lnet/minecraft/client/DeltaTracker;)V", at = @At("TAIL"))
    private void alexsmobs$computeCameraAngles(net.minecraft.client.DeltaTracker deltaTracker,
                                               CallbackInfo ci) {
        // false, not true: the event's partial tick is the render-frame one, and it must keep
        // running while the game is paused — the handler's own pause guard decides that, not this.
        FabricClientEvents.fireComputeCameraAngles((net.minecraft.client.Camera) (Object) this,
                deltaTracker.getGameTimeDeltaPartialTick(false));
    }
    *///?}
}

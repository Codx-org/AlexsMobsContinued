package com.github.alexthe666.alexsmobs.mixin.fabric;

import com.github.alexthe666.alexsmobs.citadel.server.entity.collision.ICustomCollisions;
import com.github.alexthe666.alexsmobs.fabric.event.FabricServerEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
//? if <1.20.2
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The {@code Entity}-scoped hooks. Fabric-only — see {@link FabricLivingEntityMixin} for why this
 * package is excluded and pruned on the other loaders, and {@code FabricServerEvents} for what the
 * event-firing ones do.
 */
@Mixin(Entity.class)
public abstract class FabricEntityMixin {

    // thunderHit's descriptor is byte-identical on all 17 Fabric nodes, so no arms. HEAD-cancel is
    // exactly Forge's veto: the mod converts a struck squid into a giant squid itself and then
    // suppresses the vanilla burn/damage that would otherwise kill the replacement's source entity.
    @Inject(method = "thunderHit", at = @At("HEAD"), cancellable = true)
    private void alexsmobs$struckByLightning(ServerLevel level, LightningBolt bolt, CallbackInfo ci) {
        if (FabricServerEvents.fireStruckByLightning((Entity) (Object) this, bolt)) {
            ci.cancel();
        }
    }

    /**
     * {@code ICustomCollisions} on Fabric — the tiger walking through bamboo and leaves, and the
     * rocky roller doing the same while rolled up.
     *
     * <p>On Forge and NeoForge those two entities simply <b>override</b> {@code collide}, because
     * both loaders patch vanilla to make it overridable. Vanilla's is <b>private</b>, and an access
     * widener cannot substitute: the AW spec makes a widened private method {@code final}, precisely
     * to preserve {@code invokespecial} semantics. So on this loader the delegation happens here
     * instead, and those two overrides stay gated {@code //? if !fabric}.
     *
     * <p>Descriptor is byte-identical on all 17 Fabric nodes (checked, rule 10), so no arms.
     *
     * <p>No recursion: {@code getAllowedMovementForEntity} runs the vendored
     * {@code collideBoundingBox2} and never calls {@code collide} back. Worth confirming rather than
     * assuming — that exact shape is what made {@code getEntitySpawningPacket} recurse into its own
     * callers on NeoForge.
     */
    @Inject(method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
            at = @At("HEAD"), cancellable = true)
    private void alexsmobs$customCollisions(Vec3 movement, CallbackInfoReturnable<Vec3> cir) {
        if ((Object) this instanceof ICustomCollisions) {
            cir.setReturnValue(
                    ICustomCollisions.getAllowedMovementForEntity((Entity) (Object) this, movement));
        }
    }

    //? if <1.20.2 {
    @Shadow
    private float eyeHeight;

    @Shadow
    private net.minecraft.world.entity.EntityDimensions dimensions;

    /**
     * Forge deleted {@code EntityEvent.Size} in 1.20.2, so the handler behind this is gated
     * {@code <1.20.2} in {@code ServerEvents} and 1.20.1-fabric is the only node that has it. Its
     * one job is the Clinging effect's upside-down camera.
     *
     * <p>TAIL, so both shadowed fields already hold the freshly computed values — which makes
     * {@code eyeHeight} identical to what Forge passes as the event's <i>old</i> eye height (Forge
     * computes it the same way, from the new dimensions). {@code dimensions} is the one small
     * divergence: Forge hands the event the <i>previous</i> dimensions, this hands it the new ones.
     * They differ only on the single tick a pose actually changes, and the handler's
     * {@code height - eyeHeight} plainly wants the hitbox the eyes are being placed in, so the new
     * one is if anything the more correct of the two. Recorded in docs/notes/fabric.md.
     */
    @Inject(method = "refreshDimensions", at = @At("TAIL"))
    private void alexsmobs$entitySize(CallbackInfo ci) {
        this.eyeHeight = FabricServerEvents.fireEntitySize((Entity) (Object) this, this.dimensions, this.eyeHeight);
    }
    //?}
}

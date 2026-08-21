package com.github.alexthe666.alexsmobs.mixin.fabric;

import com.github.alexthe666.alexsmobs.fabric.event.FabricServerEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The dodging-emu hook. Fabric-only — see {@link FabricLivingEntityMixin} for why this package is
 * excluded and pruned on the other loaders.
 *
 * <p>Forge fires {@code ProjectileImpactEvent} from the first line of {@code Projectile#onHit} and
 * returns early when a listener vetoes; HEAD-cancel here is the same thing. The descriptor is
 * byte-identical on all 17 Fabric nodes, so there are no arms.
 */
@Mixin(Projectile.class)
public abstract class FabricProjectileMixin {

    @Inject(method = "onHit", at = @At("HEAD"), cancellable = true)
    private void alexsmobs$projectileImpact(HitResult ray, CallbackInfo ci) {
        if (FabricServerEvents.fireProjectileImpact((Entity) (Object) this, ray)) {
            ci.cancel();
        }
    }
}

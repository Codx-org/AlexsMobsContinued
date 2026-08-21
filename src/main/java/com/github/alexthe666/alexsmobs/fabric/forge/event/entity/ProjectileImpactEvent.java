package com.github.alexthe666.alexsmobs.fabric.forge.event.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;

/**
 * Fabric stand-in for {@code net.minecraftforge.event.entity.ProjectileImpactEvent}.
 *
 * <p>The mod cancels this to make an emu dodge arrows — the arrow is deflected sideways and the
 * impact is skipped. Forge {@code >=1.20.4} prefers {@code setImpactResult(SKIP_ENTITY)} over
 * cancelling, but that arm is gated {@code forge &&}, so Fabric takes the {@code setCanceled(true)}
 * else-arm and {@code ImpactResult} is never referenced on this loader. It is therefore not
 * stubbed — an unused enum would be one more thing to keep in step for no call site.
 *
 * <p>{@link #getEntity()} stays {@code Entity}-typed exactly as Forge has it: the handler
 * {@code instanceof}-checks it against both {@code AbstractArrow} and {@code Projectile}.
 */
public class ProjectileImpactEvent extends EntityEvent {

    private final HitResult ray;

    public ProjectileImpactEvent(Entity projectile, HitResult ray) {
        super(projectile);
        this.ray = ray;
    }

    public HitResult getRayTraceResult() {
        return ray;
    }
}

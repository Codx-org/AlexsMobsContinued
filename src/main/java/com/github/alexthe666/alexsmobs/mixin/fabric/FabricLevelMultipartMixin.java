package com.github.alexthe666.alexsmobs.mixin.fabric;

import com.github.alexthe666.alexsmobs.entity.IMultipartOwner;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * The multipart level plumbing, which is the half {@code fabric/entity/PartEntity} does not vendor.
 *
 * <p>Part entities are never added to any level's entity storage on <i>any</i> loader. Forge and
 * NeoForge patch vanilla to keep them in a side map that the tracking callbacks feed, and then fold
 * that map into the world queries. Vanilla has the identical mechanism but hard-typed to
 * {@code EnderDragonPart}, so on Fabric it is closed to us and the equivalent is this: after
 * {@code Level#getEntities} has produced its vanilla answer, append the in-range parts of any nearby
 * {@link IMultipartOwner}. That one query backs entity picking, {@code getEntityCollisions} and most
 * range lookups, so hooking it makes the cachalot's, giant squid's and laviathan's segments
 * pickable, attackable and collidable in one place.
 *
 * <p>Ported from AlexsMobsFP's {@code LevelMultipartMixin}, which has been running this in
 * production. Descriptor-checked across all 17 Fabric nodes: both
 * {@code Level#getEntities(Entity, AABB, Predicate)} and the plain-{@code Consumer} overload of
 * {@code LevelEntityGetter#get} are byte-identical on every one, so there are no era arms. (The
 * {@code get} check was not idle — vanilla moved the other {@code get} overloads to
 * {@code AbortableIterationConsumer}, and only the one used here kept its shape.)
 */
@Mixin(Level.class)
public abstract class FabricLevelMultipartMixin {

    /**
     * ⚠️ <b>Declared {@code @Shadow} and {@code public} on purpose — do not "clean this up".</b>
     * Without {@code @Shadow}, Mixin reads this as an implicit overwrite of {@code Level#getEntities()}
     * and conforms its visibility against the target. The Mojmap dev jar has it {@code protected} on
     * every version this tree spans, but at runtime a <i>coexisting mod's</i> access widener can
     * promote it to {@code public}, and a {@code protected} overwrite then hard-crashes the game
     * with "cannot reduce visibiliy of PUBLIC target method" before the main menu. AlexsMobsFP paid
     * for this one on 2026-07-26; it costs nothing to keep.
     */
    @Shadow
    public abstract LevelEntityGetter<Entity> getEntities();

    @Inject(
            method = "getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;",
            at = @At("RETURN"),
            cancellable = true)
    private void alexsmobs$includeParts(Entity except, AABB area, Predicate<? super Entity> predicate,
                                        CallbackInfoReturnable<List<Entity>> cir) {
        // Generous margin: the query is for PARENTS, whose own hitbox can sit well outside `area`
        // while a tentacle reaches into it. Parts are then filtered against the real `area` below,
        // so this only widens who gets considered, never what gets returned.
        final double partReach = 16.0D;
        List<Entity> added = new ArrayList<>(0);
        this.getEntities().get(area.inflate(partReach), candidate -> {
            if (candidate instanceof IMultipartOwner owner && owner.isMultipartEntity() && candidate != except) {
                com.github.alexthe666.alexsmobs.fabric.entity.PartEntity<?>[] parts = owner.getParts();
                if (parts != null) {
                    for (com.github.alexthe666.alexsmobs.fabric.entity.PartEntity<?> part : parts) {
                        if (part != except && part.getBoundingBox().intersects(area)
                                && (predicate == null || predicate.test(part))) {
                            added.add(part);
                        }
                    }
                }
            }
        });
        if (!added.isEmpty()) {
            List<Entity> combined = new ArrayList<>(cir.getReturnValue());
            combined.addAll(added);
            cir.setReturnValue(combined);
        }
    }
}

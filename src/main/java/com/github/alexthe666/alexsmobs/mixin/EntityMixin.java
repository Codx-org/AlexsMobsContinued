package com.github.alexthe666.alexsmobs.mixin;

import com.github.alexthe666.alexsmobs.misc.AMCompat;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Re-permits this mod's seven player-riding mobs to mount a player. See
 * docs/notes/bug-reports.md #81.
 *
 * <p><b>1.21.2 added a server-side guard to {@code Entity#startRiding} that no entity in this mod
 * can pass when the vehicle is a player:</b>
 *
 * <pre>
 *     if (!this.level().isClientSide() &amp;&amp; !vehicle.type.canSerialize()) return false;
 * </pre>
 *
 * {@code EntityType.PLAYER} is built with {@code noSave()}, so {@code canSerialize()} is
 * {@code false} for it and the call is rejected — silently, by return value, with no log line.
 * Neither Forge nor NeoForge patches the check out (bytecode-checked on both, 1.21.2 → 26.2), so
 * all three loaders need this.
 *
 * <p>The guard is skipped on the client (that is what the {@code isClientSide} half is for), which
 * is what makes the symptom so odd: the server broadcasts {@code MessageMosquitoMountPlayer} /
 * {@code MessageCrowMountPlayer} unconditionally after its own {@code startRiding} has already
 * failed, every client obeys it, and so the mob is drawn latched on for good while the server
 * believes it is still flying. {@code EntityCrimsonMosquito#rideTick} — which is where the whole
 * blood-drinking, damage and dismount loop lives — never runs, so it never drinks and never lets
 * go. Same shape for the five shoulder-riders: capuchin monkey, potoo, sugar glider, crow and bald
 * eagle all fail to perch, which also takes the bald eagle's whole falconry loop with it.
 *
 * <p>35 nodes (everything &gt;=1.21.2), all three loaders, since {@code 2.0.0}.
 *
 * <p><b>{@code @ModifyExpressionValue}, not {@code @Redirect} — and that is not a style choice.</b>
 * This started life as a {@code @Redirect} on the reasoning that one obscure call in one vanilla
 * method would never be contested, so the usual "redirects are exclusive" objection cost nothing.
 * It cost a hard crash (#99): <b>MCA Reborn redirects the very same call</b>, for the very same
 * reason — {@code mca.mixins.json:MixinEntity->mca$allowCarriedVillagersToRidePlayers}, so its
 * carried baby/toddler/child villagers can ride a player. Two {@code @Redirect}s on one call site
 * cannot coexist: Mixin logs {@code @Redirect conflict}, skips the loser, and the loser's
 * {@code defaultRequire: 1} then turns the skip into {@code Critical injection failure ...
 * Mixin transformation of net.minecraft.world.entity.Entity failed} during {@code Bootstrap}.
 * MCA declares {@code defaultRequire: 1} as well, so the pack fails to launch whichever way the
 * application order falls — it is only <i>which mod</i> is blamed that varies.
 * {@code @ModifyExpressionValue} wraps the expression's value instead of replacing the call, which
 * is explicitly stackable: MCA's redirect still runs, and this handler sees its result as
 * {@code original} and ORs our own answer into it, so both mods keep their behaviour. MixinExtras
 * ships inside Fabric Loader, Forge and NeoForge on every node this arm covers, and the annotation
 * is written fully-qualified so nothing is imported on the nodes where the arms are commented out.
 */
@Mixin(Entity.class)
public class EntityMixin {

    // The enclosing method is startRiding(Entity, boolean) through 1.21.8; 1.21.9 made the
    // two-argument form final, deleted it, and moved the body into a three-argument
    // startRiding(Entity, boolean, boolean) (see AMCompat#startRiding, which splits at the same
    // boundary). The call this redirects is unchanged across that move.
    // Below 1.21.2 the guard does not exist at all, so there is nothing to redirect and
    // `defaultRequire: 1` would abort the launch — hence the arm ends here rather than being
    // pruned out of alexsmobs.mixins.json: an @Mixin class with no injectors is perfectly valid.
    //? if >=1.21.9 {
    /*@com.llamalad7.mixinextras.injector.ModifyExpressionValue(
            method = "Lnet/minecraft/world/entity/Entity;startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;canSerialize()Z"))
    private boolean alexsmobs_allowRidingUnsaveableVehicle(boolean original) {
        return original || AMCompat.ridesUnsaveableVehicles((Entity) (Object) this);
    }
    *///?} elif >=1.21.2 {
    /*@com.llamalad7.mixinextras.injector.ModifyExpressionValue(
            method = "Lnet/minecraft/world/entity/Entity;startRiding(Lnet/minecraft/world/entity/Entity;Z)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;canSerialize()Z"))
    private boolean alexsmobs_allowRidingUnsaveableVehicle(boolean original) {
        return original || AMCompat.ridesUnsaveableVehicles((Entity) (Object) this);
    }
    *///?}
}

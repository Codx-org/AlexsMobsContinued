package com.github.alexthe666.alexsmobs.mixin.client;

import com.github.alexthe666.alexsmobs.citadel.client.event.EventGetOutlineColor;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fires {@link EventGetOutlineColor} — Alex's Mobs tints the glowing outline of blue jays and the
 * void worm. Vendored from Citadel (LGPL-3.0-only).
 *
 * <p><b>This is an {@code @Inject} on the getter, not a {@code @Redirect} on the caller, and that
 * is not a style choice.</b> It used to be {@code client/LevelRendererMixin}, a {@code @Redirect}
 * on the single {@code Entity#getTeamColor()I} call inside the outline branch of the level
 * renderer. <b>Citadel — the real, installable mod — redirects that exact call, for exactly this
 * reason</b> ({@code citadel.mixins.json:client.LevelRendererMixin->citadel_getTeamColor}, same
 * upstream ancestor as this file). A {@code @Redirect} <i>replaces</i> the call, so two of them on
 * one call site are mutually exclusive: Mixin keeps the first and skips the second, and both mod
 * configs declare {@code "injectors": {"defaultRequire": 1}}, so the skip is a hard
 * {@code Mixin transformation of net.minecraft.client.renderer.LevelRenderer failed}. Whichever
 * mod loses the race fails to load, which is why the pack that reported this (#128) saw Rats — a
 * mod that depends on Citadel — stop working when Alex's Mobs Continued was installed. Same shape
 * as #99 (MCA Reborn), one call site over.
 *
 * <p>The {@code @ModifyExpressionValue} that fixed #99 is <b>not available here</b>: classic Forge
 * only began bundling MixinExtras at build 60.x (MC 1.21.10) — read out of every cached Forge
 * installer's {@code version.json} — and this mixin has to work on all 49 nodes including
 * {@code 1.20.1-forge}. A plain {@code @Inject} does not have the problem in the first place:
 * injects stack, so this one and Citadel's redirect both apply and both events fire.
 *
 * <p>Moving to the getter also deletes three fragile era arms. The call site moved twice
 * ({@code LevelRenderer#renderLevel} through 1.21.1, {@code #renderEntities} from 1.21.2,
 * {@code EntityRenderer#extractRenderState} from 1.21.9) while {@code Entity#getTeamColor()I} is
 * public and identical on every version 1.20.1 → 26.2. Its only invokers anywhere in the game are
 * that one outline site and {@code Display#getTeamColor}'s {@code super} call (javap-swept), so
 * the event fires exactly as often as it did before.
 */
@Mixin(Entity.class)
public class EntityOutlineColorMixin {

    // ⚠ RETURN, never HEAD. The event carries the colour vanilla would have returned, and at
    // HEAD the only way to get that is to call `getTeamColor()` -- which re-enters this very
    // handler, i.e. unbounded recursion the first time anything glows. At RETURN the value is
    // already computed and `cir.getReturnValueI()` hands it over for free. (`getTeamColor` has
    // one return site on 26.2 and two on 1.20.1/1.21.1/1.21.5/1.21.9/1.21.11 -- javap-counted, the
    // ternary folds differently -- and injecting at every RETURN is correct either way, since each
    // one still sees the value that site was about to return.)
    @Inject(method = "getTeamColor", at = @At("RETURN"), cancellable = true)
    private void alexsmobs_getOutlineColor(CallbackInfoReturnable<Integer> cir) {
        Entity self = (Entity) (Object) this;
        EventGetOutlineColor event = new EventGetOutlineColor(self, cir.getReturnValueI());
        event.post();
        if (event.isHandled()) {
            cir.setReturnValue(event.getColor());
        }
    }
}

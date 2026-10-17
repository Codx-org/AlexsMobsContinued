package com.github.alexthe666.alexsmobs.mixin.client;

import com.github.alexthe666.alexsmobs.citadel.client.event.EventGetStarBrightness;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fires {@link EventGetStarBrightness} — Alex's Mobs dims the stars in the void worm's
 * boss fight. Vendored from Citadel (LGPL-3.0-only).
 *
 * <p>1.21.11 deleted {@code ClientLevel#getStarBrightness(float)}: star brightness is an
 * {@code EnvironmentAttribute} now, sampled once per frame by {@code SkyRenderer} into
 * {@code SkyRenderState#starBrightness}. The host <em>class</em> therefore moves too, which is why
 * the class-level {@code @Mixin} is gated as well. Its target is spelled fully qualified and
 * deliberately never imported — a {@code replacements} rule can silently retarget an imported
 * simple name (that is exactly what bit {@code renderstate.EntityRendererMixin}).
 */
//? if >=1.21.11 {
/*@Mixin(net.minecraft.client.renderer.SkyRenderer.class)
*///?} else {
@Mixin(ClientLevel.class)
//?}
public abstract class ClientLevelMixin {

    //? if >=1.21.11 {
    /*@Inject(at = @At("RETURN"), method = "extractRenderState")
    private void alexsmobs_getStarBrightness(ClientLevel level, float partialTicks, net.minecraft.client.Camera camera,
                                             net.minecraft.client.renderer.state.SkyRenderState state, CallbackInfo ci) {
        EventGetStarBrightness event = new EventGetStarBrightness(level, state.starBrightness, partialTicks);
        event.post();
        if (event.isHandled()) {
            state.starBrightness = event.getBrightness();
        }
    }
    *///?} else {
    @Inject(at = @At("RETURN"), method = "Lnet/minecraft/client/multiplayer/ClientLevel;getStarBrightness(F)F", cancellable = true)
    private void alexsmobs_getStarBrightness(float partialTicks, CallbackInfoReturnable<Float> cir) {
        EventGetStarBrightness event = new EventGetStarBrightness((ClientLevel) (Object) this, cir.getReturnValue(), partialTicks);
        event.post();
        if (event.isHandled()) {
            cir.setReturnValue(event.getBrightness());
        }
    }
    //?}
}

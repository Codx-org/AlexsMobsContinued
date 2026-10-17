package com.github.alexthe666.alexsmobs.mixin.client;

import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
//? if forge && >=1.21 && <26 {
/*import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
*///?}

/**
 * Draws the farseer's static screen on top of the HUD.
 *
 * <p>Every other node registers this through the loader: Forge's {@code RenderGuiOverlayEvent}
 * below 1.20.5, its {@code AddGuiOverlayLayersEvent} on 1.20.5/1.20.6, and NeoForge's
 * {@code RegisterGuiLayersEvent} throughout. Forge 51.x (MC 1.21) is the one gap — it dropped
 * the overlay system with the vanilla rewrite and ships <em>no</em> HUD event or layer registry
 * at all — so there the overlay is drawn from here instead. Forge 64 (MC 26) restores
 * {@code AddGuiOverlayLayersEvent}, and 26 renamed {@code Gui#render} to
 * {@code extractRenderState} anyway, so this closes at 26. The class stays (inert) on the other
 * nodes so the mixin config does not have to vary per node; JSON is not preprocessed.
 */
@Mixin(Gui.class)
public abstract class GuiMixin {

    //? if forge && >=1.21 && <26 {
    /*@Inject(at = @At("RETURN"), method = "render")
    private void alexsmobs_renderFarseerStatic(net.minecraft.client.gui.GuiGraphics guiGraphics, net.minecraft.client.DeltaTracker deltaTracker, CallbackInfo ci) {
        com.github.alexthe666.alexsmobs.client.event.ClientEvents.renderStaticOverlay(guiGraphics, deltaTracker.getGameTimeDeltaPartialTick(false));
    }
    *///?}
}

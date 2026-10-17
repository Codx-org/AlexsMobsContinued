package com.github.alexthe666.alexsmobs.mixin.client;

import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Pools the picture-in-picture renderer vanilla shares between every GUI entity of a frame, so the
 * animal dictionary's index pages stop drawing the same mob in every slot. The whole explanation,
 * and why this is unreachable on NeoForge, is on
 * {@link com.github.alexthe666.alexsmobs.client.render.AMGuiEntityPipPool}.
 *
 * <p>The body is empty on NeoForge, whose {@code GuiRenderer} pools these renderers itself and
 * whose {@code preparePictureInPictureState} has a different signature because of it. The class
 * still applies there, inert, so the mixin config does not have to vary per loader; it is excluded
 * from the compile below 1.21.6, where {@code GuiRenderer} does not exist at all, and pruned back
 * out of the config there.
 */
@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin {

    //? if !neoforge {
    @Inject(method = "preparePictureInPicture()V", at = @At("HEAD"))
    private void alexsmobs_beginPictureInPictureFrame(CallbackInfo ci) {
        com.github.alexthe666.alexsmobs.client.render.AMGuiEntityPipPool.beginFrame();
    }

    // The lookup is the map read in preparePictureInPictureState; its descriptor is java.util.Map,
    // which is the one thing about that method that has not moved between 1.21.6 and 26.2 (the
    // state type it is keyed by changed package). There is exactly one Map.get in the method.
    @Redirect(method = "preparePictureInPictureState",
            at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object alexsmobs_poolGuiEntityRenderers(java.util.Map<Object, Object> renderers, Object stateClass) {
        return com.github.alexthe666.alexsmobs.client.render.AMGuiEntityPipPool.substitute(renderers.get(stateClass));
    }

    @Inject(method = "close()V", at = @At("HEAD"))
    private void alexsmobs_closePictureInPicturePool(CallbackInfo ci) {
        com.github.alexthe666.alexsmobs.client.render.AMGuiEntityPipPool.close();
    }
    //?}
}

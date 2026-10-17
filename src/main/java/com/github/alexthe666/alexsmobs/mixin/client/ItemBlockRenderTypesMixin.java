package com.github.alexthe666.alexsmobs.mixin.client;

import com.github.alexthe666.alexsmobs.citadel.client.event.EventGetFluidRenderType;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fires {@link EventGetFluidRenderType} — Alex's Mobs swaps the lava render type for the
 * Lava Vision effect. Vendored from Citadel (LGPL-3.0-only).
 */
@Mixin(ItemBlockRenderTypes.class)
public class ItemBlockRenderTypesMixin {

    // 1.21.6 changed getRenderLayer's return type to ChunkSectionLayer, which is part of the
    // descriptor the injector matches on — a stale descriptor is a hard mixin-apply failure.
    //? if >=1.21.6 {
    /*@Inject(at = @At("TAIL"), cancellable = true,
            method = "Lnet/minecraft/client/renderer/ItemBlockRenderTypes;getRenderLayer(Lnet/minecraft/world/level/material/FluidState;)Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;")
    private static void alexsmobs_getFluidRenderLayer(FluidState fluidState, CallbackInfoReturnable<net.minecraft.client.renderer.chunk.ChunkSectionLayer> cir) {
        EventGetFluidRenderType event = new EventGetFluidRenderType(fluidState, cir.getReturnValue());
        event.post();
        if (event.isHandled()) {
            cir.setReturnValue(event.getRenderType());
        }
    }
    *///?} else {
    @Inject(at = @At("TAIL"), cancellable = true,
            method = "Lnet/minecraft/client/renderer/ItemBlockRenderTypes;getRenderLayer(Lnet/minecraft/world/level/material/FluidState;)Lnet/minecraft/client/renderer/RenderType;")
    private static void alexsmobs_getFluidRenderLayer(FluidState fluidState, CallbackInfoReturnable<RenderType> cir) {
        EventGetFluidRenderType event = new EventGetFluidRenderType(fluidState, cir.getReturnValue());
        event.post();
        if (event.isHandled()) {
            cir.setReturnValue(event.getRenderType());
        }
    }
    //?}
}

package com.github.alexthe666.alexsmobs.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.21.6+ only — the whole file is excluded from the compile and pruned from the mixin config below
 * 1.21.6 (see ModPlatformPlugin.configureJava), because the GUI item atlas this exists for does not
 * exist there.
 *
 * <p>#107. From 1.21.6 the GUI draws each item once into a shared atlas texture and blits from it on
 * later frames, keyed by the item's <b>model identity</b> ({@code GuiRenderer.atlasPositions}; on 26.x
 * a {@code DynamicAtlasAllocator} inside {@code GuiItemAtlas}). An item is exempted from that cache —
 * re-rendered every frame, in place, into the slot it already owns — only when the render state says
 * {@code isAnimated()}, and vanilla sets that in exactly one place: the {@code if (stack.hasFoil())}
 * branch of {@code SpecialModelWrapper.update} (javap on 1.21.6/1.21.8/1.21.9/1.21.11/26.2 — the same
 * {@code hasFoil() -> ifeq} shape on every one of them). This mod's icons are animated and have no
 * foil, so they need to ask for it themselves.
 *
 * <p>The seam is {@code appendModelIdentityElement}, which {@code SpecialModelWrapper.update} calls
 * with whatever {@code AMIconSpecialRenderer.extractArgument} returned: recognising our own canonical
 * argument there is equivalent to "this render state is one of our icons", and it needs no era arms —
 * the descriptor {@code (Ljava/lang/Object;)V} is identical on every version in range.
 *
 * <p>Note the pairing: this alone would be a no-op cost, and the canonical argument alone would freeze
 * the icons. Before #107 they animated only because each frame's fresh {@code ItemStack} copy missed
 * the cache by identity, which allocated a new atlas slot per icon per frame and periodically forced
 * vanilla to destroy and rebuild the whole atlas.
 */
@Mixin(net.minecraft.client.renderer.item.ItemStackRenderState.class)
public abstract class ItemStackRenderStateAtlasMixin {

    @Inject(method = "appendModelIdentityElement", at = @At("HEAD"))
    private void alexsmobs$markIconAnimated(Object element, CallbackInfo ci) {
        if (com.github.alexthe666.alexsmobs.client.render.AMIconSpecialRenderer.isCanonicalArgument(element)) {
            ((net.minecraft.client.renderer.item.ItemStackRenderState) (Object) this).setAnimated();
        }
    }
}

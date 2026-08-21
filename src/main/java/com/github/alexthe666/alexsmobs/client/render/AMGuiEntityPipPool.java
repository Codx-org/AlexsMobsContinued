package com.github.alexthe666.alexsmobs.client.render;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.GuiEntityRenderer;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;

/**
 * Gives every entity drawn into the GUI in one frame its own picture-in-picture renderer.
 *
 * <p>From 1.21.6 a GUI entity is not drawn where it is submitted: {@code submitEntityRenderState}
 * files a {@code GuiEntityRenderState} into the frame's render state, and {@code GuiRenderer}
 * renders them all at flush. It looks the renderer up in a {@code Map} keyed by the state's class,
 * so <em>every</em> entity in the frame gets the same {@link GuiEntityRenderer} instance — and a
 * {@code PictureInPictureRenderer} owns exactly one texture, which it re-renders per state while
 * the blits it already queued still point at it. The blits are drawn afterwards, so they all
 * sample whatever was rendered last: every mob comes out as the final one submitted.
 *
 * <p>The animal dictionary submits one entity per index button plus the big mob on the left page,
 * so its index pages showed the same mob in every slot. Vanilla never notices because no vanilla
 * screen draws two entities at once. NeoForge fixed it in the loader (21.6+ pools the renderers,
 * and its patch comment describes this exact symptom), so this is only reachable on Forge and
 * Fabric — where the fix has to come from a mod.
 *
 * <p>The pool hands out the vanilla renderer for the first entity of the frame, so the ordinary
 * one-entity screen allocates nothing, and a private renderer for each one after that. Assignment
 * is by position in the frame, which is stable for a given screen, so a pooled renderer keeps
 * being asked for the same texture size instead of reallocating.
 */
public final class AMGuiEntityPipPool {

    private static final List<PictureInPictureRenderer<?>> EXTRA = new ArrayList<>();
    private static int used;

    private AMGuiEntityPipPool() {
    }

    /** Called once per frame, before the frame's picture-in-picture states are prepared. */
    public static void beginFrame() {
        used = 0;
    }

    /**
     * Swaps in a free renderer for the second and later GUI entities of the frame. Anything that
     * is not a {@link GuiEntityRenderer} — signs, skins, banners, the profiler chart — is handed
     * back untouched, since only one of each of those is ever on screen at a time.
     */
    public static Object substitute(Object renderer) {
        if (!(renderer instanceof GuiEntityRenderer)) {
            return renderer;
        }
        int index = used++;
        if (index == 0) {
            return renderer;
        }
        while (EXTRA.size() < index) {
            EXTRA.add(create());
        }
        return EXTRA.get(index - 1);
    }

    /** Releases the pooled renderers' GPU textures with the {@code GuiRenderer} that used them. */
    public static void close() {
        EXTRA.forEach(PictureInPictureRenderer::close);
        EXTRA.clear();
        used = 0;
    }

    private static PictureInPictureRenderer<?> create() {
        // The buffer source is the one vanilla hands its own picture-in-picture renderers:
        // GameRenderer builds them from the RenderBuffers it was constructed with, which is
        // Minecraft's. 26.2 dropped the parameter.
        //? if >=26.2 {
        /*return new GuiEntityRenderer(Minecraft.getInstance().getEntityRenderDispatcher());
        *///?} else {
        return new GuiEntityRenderer(Minecraft.getInstance().renderBuffers().bufferSource(),
                Minecraft.getInstance().getEntityRenderDispatcher());
        //?}
    }
}

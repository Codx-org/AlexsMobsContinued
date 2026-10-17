package com.github.alexthe666.alexsmobs.client.render;

/**
 * The animal dictionary's three "pose the mob for its page" flags, as one bitmask that can survive
 * a deferred draw.
 *
 * <p>{@code GUIAnimalDictionary#render} sets {@link RenderUnderminer#renderWithPickaxe},
 * {@link RenderLaviathan#renderWithoutShaking} and {@link RenderMurmurBody#renderWithHead} around
 * its {@code super.render}, and clears them again as soon as it returns. That works only while the
 * book's entity is drawn inside that call. From <b>1.21.6</b> it is not: {@code
 * GuiGraphics#submitEntityRenderState} files a picture-in-picture element into the frame's render
 * state and {@code GuiRenderer} draws it after the screen's {@code render()} has returned — by
 * which time all three flags are back to {@code false}, so the underminer loses its ghostly
 * pickaxe, the laviathan shakes on its page and the murmur's fake head is gone.
 *
 * <p>The extract pass still runs inside the screen's own call, so the shim renderers snapshot the
 * mask there ({@code AMRenderState#bookFlags}) and re-apply it for the duration of the draw. In the
 * world, and on every version below 1.21.6, extraction and drawing are back to back and the mask is
 * whatever it already was, so this is a no-op everywhere else.
 */
public final class AMBookPose {

    public static final int PICKAXE = 1;
    public static final int NO_SHAKE = 2;
    public static final int FAKE_HEAD = 4;

    private AMBookPose() {
    }

    /** The flags as they stand right now. */
    public static int capture() {
        return (RenderUnderminer.renderWithPickaxe ? PICKAXE : 0)
                | (RenderLaviathan.renderWithoutShaking ? NO_SHAKE : 0)
                | (RenderMurmurBody.renderWithHead ? FAKE_HEAD : 0);
    }

    /** Applies {@code flags} and returns what was there before, for a {@code finally} restore. */
    public static int swap(int flags) {
        int previous = capture();
        RenderUnderminer.renderWithPickaxe = (flags & PICKAXE) != 0;
        RenderLaviathan.renderWithoutShaking = (flags & NO_SHAKE) != 0;
        RenderMurmurBody.renderWithHead = (flags & FAKE_HEAD) != 0;
        return previous;
    }
}

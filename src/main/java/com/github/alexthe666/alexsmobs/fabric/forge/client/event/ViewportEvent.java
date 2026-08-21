package com.github.alexthe666.alexsmobs.fabric.forge.client.event;

import com.github.alexthe666.alexsmobs.fabric.forge.eventbus.api.Event;
import net.minecraft.client.Camera;

/**
 * Fabric stand-in for {@code net.minecraftforge.client.event.ViewportEvent} — the three per-frame
 * camera hooks {@code client/event/ClientEvents} listens to.
 *
 * <p>Same relocated-compat-namespace pattern, and the same reasoning, as
 * {@link com.github.alexthe666.alexsmobs.fabric.forge.eventbus.api.Event}: the handlers are
 * loader-neutral once the event is unpacked, so reproducing the accessors is cheaper — and far
 * harder to get silently wrong — than forking the file.
 *
 * <p><b>Only the fields the handlers actually read are carried.</b> Forge's real events also expose
 * the renderer, the level and the frustum; nothing here asks for them, and a field that no call site
 * checks is a field that can quietly hold the wrong value.
 */
public abstract class ViewportEvent extends Event {

    private final Camera camera;
    private final double partialTick;

    protected ViewportEvent(Camera camera, double partialTick) {
        this.camera = camera;
        this.partialTick = partialTick;
    }

    public Camera getCamera() {
        return camera;
    }

    public double getPartialTick() {
        return partialTick;
    }

    /**
     * The fog colour vanilla computed for this frame. Power Down blacks it out.
     *
     * <p>Forge's version carries the alpha too; the handler only ever writes the three colour
     * channels, so the dispatcher reads back exactly those.
     */
    public static class ComputeFogColor extends ViewportEvent {

        private float red;
        private float green;
        private float blue;

        public ComputeFogColor(Camera camera, double partialTick, float red, float green, float blue) {
            super(camera, partialTick);
            this.red = red;
            this.green = green;
            this.blue = blue;
        }

        public float getRed() {
            return red;
        }

        public void setRed(float red) {
            this.red = red;
        }

        public float getGreen() {
            return green;
        }

        public void setGreen(float green) {
            this.green = green;
        }

        public float getBlue() {
            return blue;
        }

        public void setBlue(float blue) {
            this.blue = blue;
        }
    }

    /**
     * The near/far fog planes. Lava Vision pushes them out so lava stops being opaque; Power Down
     * pulls the far plane in as the effect ramps up.
     *
     * <p>The handler calls {@code setCanceled(true)} below 1.21.6 — inherited from {@link Event} —
     * and the dispatcher treats that as "the mod owns these values this frame", which is what Forge's
     * cancel means here too. From 1.21.6 the source stops cancelling because the setters write
     * straight into the {@code FogData} the shaders read; the Fabric mixin is written to the same
     * contract, so both spellings land in the same place.
     */
    public static class RenderFog extends ViewportEvent {

        private float nearPlaneDistance;
        private float farPlaneDistance;

        public RenderFog(Camera camera, double partialTick, float nearPlaneDistance, float farPlaneDistance) {
            super(camera, partialTick);
            this.nearPlaneDistance = nearPlaneDistance;
            this.farPlaneDistance = farPlaneDistance;
        }

        public float getNearPlaneDistance() {
            return nearPlaneDistance;
        }

        public void setNearPlaneDistance(float nearPlaneDistance) {
            this.nearPlaneDistance = nearPlaneDistance;
        }

        public float getFarPlaneDistance() {
            return farPlaneDistance;
        }

        public void setFarPlaneDistance(float farPlaneDistance) {
            this.farPlaneDistance = farPlaneDistance;
        }
    }

    /**
     * Fires once per frame after the camera is positioned. The handler shakes the camera for the
     * Earthquake effect by calling {@code getCamera().move(...)} — it never reads or writes the
     * pitch/yaw/roll Forge's event also carries, so those are not reproduced.
     *
     * <p>It is also where the Fabric dispatcher runs {@code doWorldLastFrame}, following the
     * precedent Forge {@code >=1.21.3} already set in the source when {@code RenderLevelStageEvent}
     * was deleted: that body does per-frame state updates only, so any once-per-frame hook will do.
     */
    public static class ComputeCameraAngles extends ViewportEvent {

        public ComputeCameraAngles(Camera camera, double partialTick) {
            super(camera, partialTick);
        }
    }
}

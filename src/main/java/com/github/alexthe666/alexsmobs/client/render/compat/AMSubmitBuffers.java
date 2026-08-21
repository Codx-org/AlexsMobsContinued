package com.github.alexthe666.alexsmobs.client.render.compat;

// 1.21.9 replaced the immediate-mode MultiBufferSource every entity renderer used to be handed
// with a deferred SubmitNodeCollector: a renderer no longer writes vertices, it *submits* a node
// (a model, a name tag, a leash) that a later feature-render pass turns into vertices. The
// submitModel node is useless to this mod — it calls Model#renderToBuffer, which 1.21.2 made
// final, so it walks the empty root that the compat EntityModel hands vanilla and draws nothing.
//
// SubmitNodeCollector#submitCustomGeometry is the escape hatch: it stores a callback keyed by
// RenderType, and CustomFeatureRenderer later does bufferSource.getBuffer(type) and invokes it.
// So this class is a MultiBufferSource that records what the legacy render bodies draw and, on
// flush(), hands one callback per RenderType to the collector to replay it. The ~130 renderers,
// ~37 layers and ~130 models therefore keep their pre-1.21.2 shape, exactly as they did across
// the 1.21.2 render-state rewrite.
//
// Cost and fidelity, both accepted deliberately:
//   * one extra copy of every vertex, plus one array-backed recorder per (entity, RenderType).
//     Custom geometry lands in the SAME MultiBufferSource.BufferSource, batched by the same
//     RenderType, as vanilla's own submitModel output, so draw order and batching are unchanged.
//   * no outline support — submitModel carries an outlineColor, submitCustomGeometry does not. A
//     glowing (spectated / team-glow) mob of this mod draws normally but without its outline.
//
// It also carries the collector and the CameraRenderState, so the handful of call sites that have
// to reach the modern API (nested entity renders, name tags, in-hand items) can downcast the
// MultiBufferSource they were given instead of every signature in the tree growing two arguments.
//? if >=1.21.9 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AMSubmitBuffers implements MultiBufferSource {

    // Shared: submitCustomGeometry only reads it (Storage#add copies last()), and the recorded
    // vertices already have the legacy body's PoseStack baked in, so the pose handed over is
    // deliberately the identity.
    private static final PoseStack IDENTITY = new PoseStack();

    private final SubmitNodeCollector collector;
    private CameraRenderState camera;
    private final Map<RenderType, Recorder> recorders = new LinkedHashMap<>();

    public AMSubmitBuffers(SubmitNodeCollector collector, CameraRenderState camera) {
        this.collector = collector;
        this.camera = camera;
    }

    // Some entry points hand out a collector but no camera state — the two RenderLivingEvent
    // flavours on NeoForge, RenderHandEvent on both loaders, and every render layer. The frame's
    // own CameraRenderState lives on LevelRenderer#levelRenderState, which is private, so those
    // sites pass null and camera() rebuilds an equivalent one on demand from the live Camera.
    public AMSubmitBuffers(SubmitNodeCollector collector) {
        this(collector, null);
    }

    // Pull the collector back out of a MultiBufferSource a legacy body was handed, or null if it
    // is not one of ours (nothing in this mod should hand a legacy body anything else, but the
    // helpers that use this stay defensive and fall back to their pre-1.21.9 behaviour).
    public static AMSubmitBuffers of(MultiBufferSource source) {
        return source instanceof AMSubmitBuffers buffers ? buffers : null;
    }

    // The collector behind a MultiBufferSource a legacy body was handed, or null if it is not one
    // of ours. For the call sites that only need the collector and never the camera.
    public static SubmitNodeCollector collectorOf(MultiBufferSource source) {
        AMSubmitBuffers buffers = of(source);
        return buffers == null ? null : buffers.collector();
    }

    public SubmitNodeCollector collector() {
        return this.collector;
    }

    // Every field of CameraRenderState is a straight copy of something the live Camera exposes,
    // so reconstructing it is exact rather than approximate. Built once per instance, i.e. at most
    // once per rendered entity, and only for the sites that actually need it.
    public CameraRenderState camera() {
        if (this.camera == null) {
            net.minecraft.client.Camera live = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera();
            CameraRenderState state = new CameraRenderState();
            state.blockPos = live.getBlockPosition();
            state.pos = live.getPosition();
            state.orientation = new org.joml.Quaternionf(live.rotation());
            //? if <26
            state.entityPos = live.getEntity() == null ? live.getPosition() : live.getEntity().position();
            state.initialized = true;
            this.camera = state;
        }
        return this.camera;
    }

    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        return this.recorders.computeIfAbsent(renderType, type -> new Recorder());
    }

    // Hand everything recorded so far to the collector. Insertion-ordered so the relative order
    // of the render types a single entity used is preserved.
    public void flush() {
        if (this.recorders.isEmpty()) {
            return;
        }
        for (Map.Entry<RenderType, Recorder> entry : this.recorders.entrySet()) {
            Recorder recorder = entry.getValue();
            if (recorder.count > 0) {
                this.collector.submitCustomGeometry(IDENTITY, entry.getKey(), recorder);
            }
        }
        // The recorders are replayed later in the frame, so they are handed off, never reused or
        // cleared — drop the references and start fresh.
        this.recorders.clear();
    }

    // One growable vertex buffer, in the six attributes VertexConsumer exposes. Replaying all six
    // unconditionally is safe: BufferBuilder#beginElement returns -1 for an element the RenderType's
    // format does not have, so the setter is a silent no-op, and endLastVertex only complains about
    // a *required* element left unfilled — which the legacy body must already have filled.
    private static final class Recorder implements VertexConsumer, SubmitNodeCollector.CustomGeometryRenderer {

        private static final int INITIAL = 64;

        private float[] pos = new float[INITIAL * 3];
        private float[] uv = new float[INITIAL * 2];
        private float[] normal = new float[INITIAL * 3];
        private int[] color = new int[INITIAL];
        private int[] overlay = new int[INITIAL];
        private int[] light = new int[INITIAL];
        int count;

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            if (this.count == this.color.length) {
                int capacity = this.count * 2;
                this.pos = Arrays.copyOf(this.pos, capacity * 3);
                this.uv = Arrays.copyOf(this.uv, capacity * 2);
                this.normal = Arrays.copyOf(this.normal, capacity * 3);
                this.color = Arrays.copyOf(this.color, capacity);
                this.overlay = Arrays.copyOf(this.overlay, capacity);
                this.light = Arrays.copyOf(this.light, capacity);
            }
            int i = this.count++;
            this.pos[i * 3] = x;
            this.pos[i * 3 + 1] = y;
            this.pos[i * 3 + 2] = z;
            // Defaults for whatever the caller does not set before the next addVertex.
            this.uv[i * 2] = 0.0F;
            this.uv[i * 2 + 1] = 0.0F;
            this.normal[i * 3] = 0.0F;
            this.normal[i * 3 + 1] = 1.0F;
            this.normal[i * 3 + 2] = 0.0F;
            this.color[i] = -1;
            this.overlay[i] = OverlayTexture.NO_OVERLAY;
            this.light[i] = 0;
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            if (this.count > 0) {
                // Packed by hand rather than through ARGB: the `!mc121-vtx-color` replacement rewrites
                // every `.color(` in the tree to `.setColor(`, so `ARGB.color(...)` cannot be spelled here.
                this.color[this.count - 1] = (alpha & 255) << 24 | (red & 255) << 16 | (green & 255) << 8 | blue & 255;
            }
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            if (this.count > 0) {
                this.uv[(this.count - 1) * 2] = u;
                this.uv[(this.count - 1) * 2 + 1] = v;
            }
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            if (this.count > 0) {
                this.overlay[this.count - 1] = u & 65535 | (v & 65535) << 16;
            }
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            if (this.count > 0) {
                this.light[this.count - 1] = u & 65535 | (v & 65535) << 16;
            }
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            if (this.count > 0) {
                this.normal[(this.count - 1) * 3] = x;
                this.normal[(this.count - 1) * 3 + 1] = y;
                this.normal[(this.count - 1) * 3 + 2] = z;
            }
            return this;
        }

        // setColor(int) was a default on VertexConsumer until 1.21.11 made it abstract. Implementing
        // it directly is correct on every node — the default it replaces just unpacked the ARGB and
        // called the four-int form above, which lands in the same slot. No @Override, for the same
        // reason as setLineWidth below.
        public VertexConsumer setColor(int argb) {
            if (this.count > 0) {
                this.color[this.count - 1] = argb;
            }
            return this;
        }

        // 1.21.11 added setLineWidth to VertexConsumer. Deliberately declared WITHOUT @Override:
        // this whole file already sits inside a >=1.21.9 block and Stonecutter blocks are siblings,
        // never nested, so it cannot be gated in place. On 1.21.9/1.21.10 it is simply an unused
        // extra method; on 1.21.11 it satisfies the new abstract. Recording it would be pointless —
        // it only matters for LINES formats, and replaying it would not compile below 1.21.11.
        public VertexConsumer setLineWidth(float width) {
            return this;
        }

        @Override
        public void render(PoseStack.Pose pose, VertexConsumer out) {
            for (int i = 0; i < this.count; i++) {
                out.addVertex(this.pos[i * 3], this.pos[i * 3 + 1], this.pos[i * 3 + 2])
                        .setColor(this.color[i])
                        .setUv(this.uv[i * 2], this.uv[i * 2 + 1])
                        .setOverlay(this.overlay[i])
                        .setLight(this.light[i])
                        .setNormal(this.normal[i * 3], this.normal[i * 3 + 1], this.normal[i * 3 + 2]);
            }
        }
    }
}
*///?}

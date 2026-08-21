package com.github.alexthe666.alexsmobs.client.render.compat;

// MC 26.2 deleted net.minecraft.client.renderer.MultiBufferSource outright — the last vanilla
// caller went away when 1.21.9 moved rendering to the extract/submit pipeline, and 26.2 finished
// the job by removing the interface and Minecraft#renderBuffers() with it.
//
// This mod still has ~130 renderers, ~37 layers and ~130 models written against the immediate-mode
// signature, and AMSubmitBuffers already stands between them and SubmitNodeCollector (see its
// header). All that is missing on 26.2 is the *type* those bodies name, so it is vendored here —
// the same two methods, in the compat package that already shadows EntityRenderer, MobRenderer,
// RenderLayer and friends — and the `!mc262-multibuffersource` replacement points the fully
// qualified vanilla name at it. That one rule covers all 109 imports and every inline FQN use.
//
// Nothing outside this mod ever sees one: every entry point that used to hand a legacy body a
// vanilla MultiBufferSource has, since 1.21.9, handed it an AMSubmitBuffers instead.
//
// Below 26.2 this compilation unit is just a package declaration, so the vanilla interface is what
// every node resolves. (client/render/compat/** is excluded from the compile below 1.21.2.)
//? if >=26.2 {
/*import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;

public interface MultiBufferSource {

    VertexConsumer getBuffer(RenderType renderType);

    // Vanilla's batching sub-interface. The five sites that named it all reached it through
    // Minecraft.getInstance().renderBuffers().bufferSource(), which 26.2 also deleted, so on this
    // node they get the discarding instance below instead — see noop().
    interface BufferSource extends MultiBufferSource {

        void endBatch();

        void endBatch(RenderType renderType);
    }

    // Minecraft#renderBuffers() is gone and there is no frame-global immediate buffer to fall back
    // on, so the five callers of bufferSource() are pointed here. Four of them are already dead on
    // this node — three custom-geometry particles that return NO_RENDER from 1.21.9 on, and
    // AMItemstackRenderer's ISTER path, unreachable since 1.21.4. The fifth, GuiBasicBook's Tabula
    // model preview, is a genuine cosmetic regression on 26.2: the animal dictionary's 3D model
    // preview draws nothing. The book itself, its text and its item icons are unaffected.
    static BufferSource noop() {
        return Noop.INSTANCE;
    }

    final class Noop implements BufferSource {

        static final Noop INSTANCE = new Noop();
        private static final VertexConsumer SINK = new Sink();

        @Override
        public VertexConsumer getBuffer(RenderType renderType) {
            return SINK;
        }

        @Override
        public void endBatch() {
        }

        @Override
        public void endBatch(RenderType renderType) {
        }
    }

    // Swallows everything. Shared and stateless — it records nothing, so it is safe to hand the
    // same instance to every render type.
    final class Sink implements VertexConsumer {

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            return this;
        }

        @Override
        public VertexConsumer setColor(int argb) {
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            return this;
        }
    }
}
*///?}

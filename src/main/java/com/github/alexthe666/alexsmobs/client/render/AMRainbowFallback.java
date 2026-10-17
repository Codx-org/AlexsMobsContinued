package com.github.alexthe666.alexsmobs.client.render;

import com.github.alexthe666.alexsmobs.entity.util.RainbowUtil;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * The rainbow glint for entities whose renderer could not be given {@code LayerRainbow}.
 *
 * <p>See {@link AMRainbowLayers} for why that set is not empty: a renderer that extends
 * {@code EntityRenderer} rather than {@code LivingEntityRenderer} takes no vanilla render layer at
 * all, and GeckoLib's {@code GeoEntityRenderer} is one — so every GeckoLib mob (Born in Chaos among
 * them) silently lost the effect. It cannot be fixed by attaching a layer harder, either:
 * {@code LayerRainbow} re-draws {@code getParentModel()}, a vanilla {@code EntityModel} that a
 * GeckoLib renderer does not have.
 *
 * <p><b>How it works.</b> {@code EntityRenderDispatcherMixin} calls {@link #begin} at the TAIL of
 * {@code EntityRenderDispatcher#render} and, if it gets a buffer source back, runs the whole render
 * a second time through it. The wrapper routes every {@code NEW_ENTITY}-format request — i.e. the
 * model — to one glint layer, and discards everything else, so the second pass draws the glint and
 * nothing but the glint: no second name tag, no second hitbox.
 *
 * <p>The entity's <i>shadow</i> has to be named separately, because it is the one thing the
 * dispatcher draws that is also {@code NEW_ENTITY}-format: left alone it comes back through the
 * glint layer as a rainbow square on the ground under the mob. Seen in the client screenshot for
 * #150, not by any verifier — {@code isModelType} excludes it by render-type name.
 *
 * <p><b>Why a second pass and not a fan-out.</b> Handing the renderer a
 * {@code VertexMultiConsumer.create(base, glint)} looks cheaper and does not work: this mod's glint
 * layers are not in {@code RenderBuffers.fixedBuffers}, so both they and the entity's own layer
 * resolve to the one shared {@code BufferBuilder}, and asking for the second {@code endBatch}es the
 * first mid-model. Drawing sequentially is also what {@code LayerRainbow} has always done — the
 * glint request flushes the model, writing depth, and the glint's {@code EQUAL_DEPTH_TEST} then
 * lands exactly on it.
 *
 * <p><b>Excluded from the compile on >=1.21.9</b> ({@code ModPlatformPlugin.configureJava}). From
 * that version the dispatcher submits draw nodes to a {@code SubmitNodeCollector} and there is no
 * {@code MultiBufferSource} anywhere on the path to wrap; see docs/notes/mixins.md.
 */
public final class AMRainbowFallback {

    /**
     * A consumer that swallows everything. {@code VertexMultiConsumer}'s varargs factory over an
     * empty array is a no-op on every version in range, which beats hand-writing a
     * {@code VertexConsumer} whose interface changed wholesale at 1.21. Defensive because the value
     * is worth nothing if it ever throws: the fallback draws the discarded pass normally instead,
     * which is visually near-identical and certainly safe.
     */
    private static final VertexConsumer DISCARD = discarding();

    /** Guards against the second pass triggering a third through a passenger or a nested render. */
    private static boolean glinting;

    private AMRainbowFallback() {
    }

    /**
     * @return a buffer source to re-render {@code entity} through, or {@code null} to do nothing.
     *         A non-null return leaves this class armed — the caller MUST pair it with {@link #end}
     *         in a finally block.
     */
    public static MultiBufferSource begin(Entity entity, MultiBufferSource delegate) {
        if (glinting || delegate == null || !(entity instanceof LivingEntity living)) {
            return null;
        }
        // Cheapest test first: almost every entity on screen is a vanilla one that already carries
        // the layer, and that is a set lookup rather than an NBT read.
        if (AMRainbowLayers.hasLayer(entity.getType())) {
            return null;
        }
        int type = RainbowUtil.getRainbowType(living);
        if (type <= 0) {
            return null;
        }
        glinting = true;
        return new GlintOnly(delegate, AMRenderTypes.glintFor(type));
    }

    public static void end() {
        glinting = false;
    }

    /** Render-thread only, so no synchronisation. */
    private static final Map<RenderType, Boolean> MODEL_TYPES = new IdentityHashMap<>();

    private static VertexConsumer discarding() {
        try {
            return VertexMultiConsumer.create(new VertexConsumer[0]);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * True for the render types that make up the entity's MODEL. Everything the dispatcher draws
     * around the model is either a different vertex format (name tag, hitbox, leash) or — in the
     * shadow's case alone — {@code NEW_ENTITY} as well, so the shadow is excluded by name. The
     * answer is cached per {@code RenderType} instance: they are singletons, this runs once per
     * entity per frame, and {@code RenderType#toString} on a composite type is not cheap.
     */
    private static boolean isModelType(RenderType type) {
        Boolean cached = MODEL_TYPES.get(type);
        if (cached == null) {
            cached = type.format() == DefaultVertexFormat.NEW_ENTITY && !type.toString().contains("entity_shadow");
            MODEL_TYPES.put(type, cached);
        }
        return cached;
    }

    private static final class GlintOnly implements MultiBufferSource {

        private final MultiBufferSource delegate;
        private final RenderType glint;

        private GlintOnly(MultiBufferSource delegate, RenderType glint) {
            this.delegate = delegate;
            this.glint = glint;
        }

        @Override
        public VertexConsumer getBuffer(RenderType type) {
            if (isModelType(type)) {
                return this.delegate.getBuffer(this.glint);
            }
            return DISCARD != null ? DISCARD : this.delegate.getBuffer(type);
        }
    }
}

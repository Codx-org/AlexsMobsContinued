package com.github.alexthe666.alexsmobs.client.render;

import com.github.alexthe666.alexsmobs.misc.AMCompat;

//? if <1.21.5 {
import com.mojang.blaze3d.platform.GlStateManager;
//?}
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.MultiBufferSource;
// RenderStateShard was deleted in 1.21.11 (RenderSetup/LayeringTransform/OutputTarget/
// TextureTransform replace it). It is only ever named inside the <1.21.5 block below, so
// gating the import there costs nothing and keeps 1.21.11 compiling.
//? if <1.21.5 {
import net.minecraft.client.renderer.RenderStateShard;
//?}
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.TheEndPortalRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.function.Consumer;

// 1.21.5 removed the whole custom render-pipeline surface this class was built on:
// RenderType.create(name, VertexFormat, Mode, int, bool, bool, CompositeState), the
// RENDERTYPE_*_SHADER / *_TRANSPARENCY / NO_CULL / *_DEPTH_TEST / COLOR_*_WRITE
// RenderStateShard constants, and GlStateManager blend funcs. Per the 1.21.4 precedent
// (gate deep cosmetic client render out, flag the regression), on >=1.21.5 every custom
// RenderType falls back to the nearest vanilla one. COSMETIC REGRESSION on >=1.21.5:
// the rainbow/comb-jelly glint scroll, the static-TV portal/particle shimmer, the custom
// ghost/underminer/farseer additive blends and the flickering-eye alpha are replaced by
// plain vanilla glint/translucent/eyes layers. Gameplay, spawning and data are unaffected.
// 1.21.11 went further still: RenderType moved to net.minecraft.client.renderer.rendertype, became
// a CONCRETE class with a PRIVATE constructor, and every factory moved off it onto RenderTypes. So
// `extends RenderType` no longer compiles at all there — the class simply stops being a RenderType
// subclass, and the handful of vanilla factories that callers reached through static inheritance are
// re-declared as explicit delegates further down.
//? if >=1.21.11 {
/*public class AMRenderTypes {
*///?} else {
public class AMRenderTypes extends RenderType {
//?}

    public static final ResourceLocation STATIC_TEXTURE = AMCompat.rl("alexsmobs:textures/static.png");


    // --- Everything from here to the end of the shader/texturing/transparency block is
    // --- <1.21.5-only: it references RenderType.create's old signature and RenderStateShard
    // --- constants / GlStateManager that 1.21.5 deleted. On >=1.21.5 the public factories
    // --- below fall back to vanilla RenderTypes and none of this is reachable.
    //? if <1.21.5 {
    // TextureStateShard's `blur` argument became a vanilla net.minecraft.util.TriState in 1.21.2 so a
    // texture can inherit the pack's blur setting instead of forcing one. Both loaders use the same
    // vanilla TriState here. All 27 call sites in this file pass a literal, so DEFAULT is never
    // wanted and the mapping is exact.
    //? if >=1.21.2 {
    /*private static RenderStateShard.TextureStateShard texState(ResourceLocation texture, boolean blur, boolean mipmap) {
        return new RenderStateShard.TextureStateShard(texture, blur ? net.minecraft.util.TriState.TRUE : net.minecraft.util.TriState.FALSE, mipmap);
    }
    *///?} else {
    private static RenderStateShard.TextureStateShard texState(ResourceLocation texture, boolean blur, boolean mipmap) {
        return new RenderStateShard.TextureStateShard(texture, blur, mipmap);
    }
    //?}

    protected static final RenderStateShard.TexturingStateShard RAINBOW_TEXTURING = new RenderStateShard.TexturingStateShard("entity_glint_texturing", () -> {
        setupRainbowTexturing(1.2F, 4L);
    }, () -> {
        RenderSystem.resetTextureMatrix();
    });
    protected static final RenderStateShard.TexturingStateShard COMB_JELLY_TEXTURING = new RenderStateShard.TexturingStateShard("entity_glint_texturing", () -> {
        setupRainbowTexturing(2F, 16L);
    }, () -> {
        RenderSystem.resetTextureMatrix();
    });
    protected static final RenderStateShard.TexturingStateShard RAINBOW_TEXTURING_LARGE = new RenderStateShard.TexturingStateShard("entity_glint_texturing", () -> {
        setupRainbowTexturing2(5F, 14L);
    }, () -> {
        RenderSystem.resetTextureMatrix();
    });
    protected static final RenderStateShard.TexturingStateShard WEEZER_TEXTURING = new RenderStateShard.TexturingStateShard("entity_glint_texturing", () -> {
        setupRainbowTexturing2(7F, 16L);
    }, () -> {
        RenderSystem.resetTextureMatrix();
    });
    protected static final RenderStateShard.TexturingStateShard STATIC_PORTAL_TEXTURING = new RenderStateShard.TexturingStateShard("entity_glint_texturing", () -> {
        setupStaticTexturing(1.1F, 12L);
    }, () -> {
        RenderSystem.resetTextureMatrix();
    });
    protected static final RenderStateShard.TexturingStateShard STATIC_PARTICLE_TEXTURING = new RenderStateShard.TexturingStateShard("entity_glint_texturing", () -> {
        setupStaticTexturing(0.1F, 12L);
    }, () -> {
        RenderSystem.resetTextureMatrix();
    });
    protected static final RenderStateShard.TexturingStateShard STATIC_ENTITY_TEXTURING = new RenderStateShard.TexturingStateShard("entity_glint_texturing", () -> {
        setupStaticTexturing(3F, 12L);
    }, () -> {
        RenderSystem.resetTextureMatrix();
    });

    protected static final RenderStateShard.TransparencyStateShard WORM_TRANSPARANCY = new RenderStateShard.TransparencyStateShard("translucent_transparency", () -> {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    }, () -> {
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    });

    protected static final RenderStateShard.TransparencyStateShard MIMICUBE_TRANSPARANCY = new RenderStateShard.TransparencyStateShard("mimicube_transparency", () -> {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    }, () -> {
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    });

    protected static final RenderStateShard.TransparencyStateShard GHOST_TRANSPARANCY = new RenderStateShard.TransparencyStateShard("translucent_ghost_transparency", () -> {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    }, () -> {
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    });

    private static void setupRainbowTexturing(float in, long time) {
        long i = Util.getMillis() * time;
        float f = (float)(i % 110000L) / 110000.0F;
        float f1 = (float)(i % 30000L) / 30000.0F;
        Matrix4f matrix4f = (new Matrix4f()).translation(0, f1, 0.0F);
        matrix4f.scale(in);
        RenderSystem.setTextureMatrix(matrix4f);
    }

    private static void setupRainbowTexturing2(float in, long time){
        long i = Util.getMillis() * time;
        float f = (float)(i % 110000L) / 110000.0F;
        float f1 = (float)(i % 30000L) / 30000.0F;
        float f2 = (float)Math.sin(i / 30000F);
        Matrix4f matrix4f = (new Matrix4f()).translation(f1, f2, 0.0F);
        matrix4f.scale(in);
        RenderSystem.setTextureMatrix(matrix4f);
    }

    private static void setupStaticTexturing(float in, long time){
        long i = Util.getMillis() * time;
        float f = (float)(i % 110000L) / 110000.0F;
        float f1 = (float)(i % 30000L) / 30000.0F;
        float f2 = (float)Math.floor((i % 3000L) / 3000.0F * 4.0F);
        float f3 = (float)Math.sin(i / 30000F) * 0.05F;
        Matrix4f matrix4f = (new Matrix4f()).translation(f1, f2 * 0.25F + f3, 0.0F);
        matrix4f.scale(in * 1.5F, in * 0.25F, in);
        RenderSystem.setTextureMatrix(matrix4f);
    }

    //?}

    // Never instantiated (all RenderTypes come from the static factories) but RenderType has no
    // no-arg constructor, so an explicit one is required per era or javac emits a bad default ctor.
    //? if >=1.21.11 {
    /*// 1.21.11: no superclass any more, so the implicit no-arg constructor is fine and there is
    // nothing abstract left to stub out.
    *///?} elif >=1.21.9 {
    /*public AMRenderTypes(String name, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setup, Runnable clear) {
        super(name, bufferSize, affectsCrumbling, sortOnUpload, setup, clear);
    }

    // 1.21.9 re-added an abstract pipeline() on top of 1.21.6's draw/format/mode.
    @Override public void draw(com.mojang.blaze3d.vertex.MeshData meshData) { throw new UnsupportedOperationException(); }
    @Override public VertexFormat format() { throw new UnsupportedOperationException(); }
    @Override public VertexFormat.Mode mode() { throw new UnsupportedOperationException(); }
    @Override public com.mojang.blaze3d.pipeline.RenderPipeline pipeline() { throw new UnsupportedOperationException(); }
    *///?} elif >=1.21.6 {
    /*public AMRenderTypes(String name, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setup, Runnable clear) {
        super(name, bufferSize, affectsCrumbling, sortOnUpload, setup, clear);
    }

    // 1.21.6 narrowed RenderType's abstract surface back down to draw/format/mode — getRenderTarget
    // and getRenderPipeline are gone.
    @Override public void draw(com.mojang.blaze3d.vertex.MeshData meshData) { throw new UnsupportedOperationException(); }
    @Override public VertexFormat format() { throw new UnsupportedOperationException(); }
    @Override public VertexFormat.Mode mode() { throw new UnsupportedOperationException(); }
    *///?} elif >=1.21.5 {
    /*public AMRenderTypes(String name, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setup, Runnable clear) {
        super(name, bufferSize, affectsCrumbling, sortOnUpload, setup, clear);
    }

    // RenderType became fully abstract in 1.21.5 (draw/getRenderTarget/getRenderPipeline/format/mode).
    // AMRenderTypes is never instantiated on any node — all render types come from the static
    // factories above — but the class must be concrete, so these stubs satisfy the compiler only.
    @Override public void draw(com.mojang.blaze3d.vertex.MeshData meshData) { throw new UnsupportedOperationException(); }
    @Override public com.mojang.blaze3d.pipeline.RenderTarget getRenderTarget() { throw new UnsupportedOperationException(); }
    @Override public com.mojang.blaze3d.pipeline.RenderPipeline getRenderPipeline() { throw new UnsupportedOperationException(); }
    @Override public VertexFormat format() { throw new UnsupportedOperationException(); }
    @Override public VertexFormat.Mode mode() { throw new UnsupportedOperationException(); }
    *///?} else {
    public AMRenderTypes(String p_173178_, VertexFormat p_173179_, VertexFormat.Mode p_173180_, int p_173181_, boolean p_173182_, boolean p_173183_, Runnable p_173184_, Runnable p_173185_) {
        super(p_173178_, p_173179_, p_173180_, p_173181_, p_173182_, p_173183_, p_173184_, p_173185_);
    }
    //?}

    //? if >=1.21.5 {
    /*// 1.21.5 removed RenderType.entityGlintDirect(); RenderType.entityGlint() is the surviving equivalent.
    public static RenderType entityGlintDirect() {
        return RenderType.entityGlint();
    }
    *///?}

    // Callers say AMRenderTypes.entityCutoutNoCull(...) / .entityTranslucent(...) / .entityGlint().
    // Below 1.21.11 those resolve through static inheritance from RenderType; at 1.21.11 the class
    // has no superclass, so they are re-declared here as delegates. The bodies are written in the
    // 1.20.1 spelling on purpose — the !mc2111-rt-* replacements re-point them at RenderTypes.
    //? if >=1.21.11 {
    /*public static RenderType entityCutoutNoCull(ResourceLocation texture) {
        return RenderType.entityCutoutNoCull(texture);
    }

    public static RenderType entityTranslucent(ResourceLocation texture) {
        return RenderType.entityTranslucent(texture);
    }

    public static RenderType entityGlint() {
        return RenderType.entityGlint();
    }
    *///?}

    //? if >=1.21.5 {
    /*public static final RenderType COMBJELLY_RAINBOW_GLINT = RenderType.entityGlint();
    public static final RenderType RAINBOW_GLINT = RenderType.entityGlint();
    public static final RenderType TRANS_GLINT = RenderType.entityGlint();
    public static final RenderType NONBI_GLINT = RenderType.entityGlint();
    public static final RenderType BI_GLINT = RenderType.entityGlint();
    public static final RenderType ACE_GLINT = RenderType.entityGlint();
    public static final RenderType BRAZIL_GLINT = RenderType.entityGlint();
    public static final RenderType WEEZER_GLINT = RenderType.entityGlint();
    public static final RenderType STATIC_PORTAL = RenderType.entityTranslucent(STATIC_TEXTURE);
    public static final RenderType STATIC_PARTICLE = RenderType.entityTranslucent(STATIC_TEXTURE);
    public static final RenderType STATIC_ENTITY = RenderType.entityTranslucent(STATIC_TEXTURE);
    public static final RenderType VOID_WORM_PORTAL_OVERLAY = RenderType.endPortal();
    *///?} else {
    public static final RenderType COMBJELLY_RAINBOW_GLINT = create("cj_rainbow_glint", DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, 256, false, false, RenderType.CompositeState.builder().setShaderState(RENDERTYPE_ENTITY_GLINT_SHADER).setTextureState(texState(AMCompat.rl("alexsmobs:textures/entity/rainbow_jelly_overlays/glint_rainbow.png"), true, false)).setWriteMaskState(COLOR_DEPTH_WRITE).setCullState(NO_CULL).setDepthTestState(EQUAL_DEPTH_TEST).setTransparencyState(NO_TRANSPARENCY).setTexturingState(COMB_JELLY_TEXTURING).createCompositeState(false));
    public static final RenderType RAINBOW_GLINT = create("rainbow_glint", DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, 256, true, true, RenderType.CompositeState.builder().setShaderState(RENDERTYPE_ENTITY_GLINT_SHADER).setTextureState(texState(AMCompat.rl("alexsmobs:textures/entity/rainbow_jelly_overlays/glint_rainbow.png"), true, false)).setWriteMaskState(COLOR_DEPTH_WRITE).setCullState(NO_CULL).setDepthTestState(EQUAL_DEPTH_TEST).setTransparencyState(GLINT_TRANSPARENCY).setTexturingState(RAINBOW_TEXTURING).setOverlayState(OVERLAY).createCompositeState(true));
    public static final RenderType TRANS_GLINT = create("trans_glint", DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, 256, false, false, RenderType.CompositeState.builder().setShaderState(RENDERTYPE_ENTITY_GLINT_SHADER).setTextureState(texState(AMCompat.rl("alexsmobs:textures/entity/rainbow_jelly_overlays/glint_trans.png"), true, false)).setWriteMaskState(COLOR_DEPTH_WRITE).setCullState(NO_CULL).setDepthTestState(EQUAL_DEPTH_TEST).setTransparencyState(GLINT_TRANSPARENCY).setTexturingState(RAINBOW_TEXTURING).setOverlayState(OVERLAY).createCompositeState(true));
    public static final RenderType NONBI_GLINT = create("nonbi_glint", DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, 256, false, false, RenderType.CompositeState.builder().setShaderState(RENDERTYPE_ENTITY_GLINT_SHADER).setTextureState(texState(AMCompat.rl("alexsmobs:textures/entity/rainbow_jelly_overlays/glint_nonbi.png"), true, false)).setWriteMaskState(COLOR_DEPTH_WRITE).setCullState(NO_CULL).setDepthTestState(EQUAL_DEPTH_TEST).setTransparencyState(GLINT_TRANSPARENCY).setTexturingState(RAINBOW_TEXTURING).setOverlayState(OVERLAY).createCompositeState(true));
    public static final RenderType BI_GLINT = create("bi_glint", DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, 256, false, false, RenderType.CompositeState.builder().setShaderState(RENDERTYPE_ENTITY_GLINT_SHADER).setTextureState(texState(AMCompat.rl("alexsmobs:textures/entity/rainbow_jelly_overlays/glint_bi.png"), true, false)).setWriteMaskState(COLOR_DEPTH_WRITE).setCullState(NO_CULL).setDepthTestState(EQUAL_DEPTH_TEST).setTransparencyState(GLINT_TRANSPARENCY).setTexturingState(RAINBOW_TEXTURING).setOverlayState(OVERLAY).createCompositeState(true));
    public static final RenderType ACE_GLINT = create("ace_glint", DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, 256, false, false, RenderType.CompositeState.builder().setShaderState(RENDERTYPE_ENTITY_GLINT_SHADER).setTextureState(texState(AMCompat.rl("alexsmobs:textures/entity/rainbow_jelly_overlays/glint_ace.png"), true, false)).setWriteMaskState(COLOR_DEPTH_WRITE).setCullState(NO_CULL).setDepthTestState(EQUAL_DEPTH_TEST).setTransparencyState(GLINT_TRANSPARENCY).setTexturingState(RAINBOW_TEXTURING).setOverlayState(OVERLAY).createCompositeState(true));
    public static final RenderType BRAZIL_GLINT = create("brazil_glint", DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, 256, false, false, RenderType.CompositeState.builder().setShaderState(RENDERTYPE_ENTITY_GLINT_SHADER).setTextureState(texState(AMCompat.rl("alexsmobs:textures/entity/rainbow_jelly_overlays/glint_brazil.png"), true, false)).setWriteMaskState(COLOR_DEPTH_WRITE).setCullState(NO_CULL).setDepthTestState(EQUAL_DEPTH_TEST).setTransparencyState(GLINT_TRANSPARENCY).setTexturingState(RAINBOW_TEXTURING_LARGE).setOverlayState(OVERLAY).createCompositeState(true));
    public static final RenderType WEEZER_GLINT = create("weezer_glint", DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, 256, false, false, RenderType.CompositeState.builder().setShaderState(RENDERTYPE_ENTITY_GLINT_SHADER).setTextureState(texState(AMCompat.rl("alexsmobs:textures/entity/rainbow_jelly_overlays/glint_weezer.png"), false, false)).setWriteMaskState(COLOR_DEPTH_WRITE).setCullState(NO_CULL).setDepthTestState(EQUAL_DEPTH_TEST).setTransparencyState(GLINT_TRANSPARENCY).setTexturingState(WEEZER_TEXTURING).setOverlayState(OVERLAY).createCompositeState(true));
    public static final RenderType STATIC_PORTAL = create("static_portal", DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, 256, false, false, RenderType.CompositeState.builder().setShaderState(RENDERTYPE_ENTITY_GLINT_SHADER).setTextureState(texState(STATIC_TEXTURE, false, false)).setWriteMaskState(COLOR_DEPTH_WRITE).setCullState(NO_CULL).setDepthTestState(EQUAL_DEPTH_TEST).setTexturingState(STATIC_PORTAL_TEXTURING).setOverlayState(OVERLAY).setTransparencyState(TRANSLUCENT_TRANSPARENCY).createCompositeState(true));
    public static final RenderType STATIC_PARTICLE = create("static_particle", DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, 256, false, false, RenderType.CompositeState.builder().setShaderState(RENDERTYPE_ENTITY_GLINT_SHADER).setTextureState(texState(STATIC_TEXTURE, false, false)).setWriteMaskState(COLOR_DEPTH_WRITE).setCullState(NO_CULL).setDepthTestState(EQUAL_DEPTH_TEST).setTexturingState(STATIC_PARTICLE_TEXTURING).setOverlayState(OVERLAY).setTransparencyState(TRANSLUCENT_TRANSPARENCY).createCompositeState(true));
    public static final RenderType STATIC_ENTITY = create("static_entity", DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, 256, false, false, RenderType.CompositeState.builder().setShaderState(RENDERTYPE_ENTITY_GLINT_SHADER).setTextureState(texState(STATIC_TEXTURE, false, false)).setWriteMaskState(COLOR_DEPTH_WRITE).setCullState(NO_CULL).setDepthTestState(EQUAL_DEPTH_TEST).setTexturingState(STATIC_ENTITY_TEXTURING).setOverlayState(OVERLAY).setTransparencyState(TRANSLUCENT_TRANSPARENCY).createCompositeState(true));
    public static final RenderType VOID_WORM_PORTAL_OVERLAY = create("void_worm_portal_overlay", DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, 256, false, false, RenderType.CompositeState.builder().setShaderState(RENDERTYPE_END_PORTAL_SHADER).setDepthTestState(EQUAL_DEPTH_TEST).setCullState(NO_CULL).setTransparencyState(NO_TRANSPARENCY).setTextureState(MultiTextureStateShard.builder().add(TheEndPortalRenderer.END_SKY_LOCATION, false, false).add(TheEndPortalRenderer.END_PORTAL_LOCATION, false, false).build()).createCompositeState(false));
    //?}

    public static RenderType getTransparentMimicube(ResourceLocation texture) {
        //? if >=1.21.5 {
        /*return RenderType.entityTranslucent(texture);
        *///?} else {
        RenderType.CompositeState lvt_1_1_ = RenderType.CompositeState.builder().setTextureState(texState(texture, false, false)).setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_CULL_SHADER).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setOverlayState(OVERLAY).setOutputState(TRANSLUCENT_TARGET).setCullState(CULL).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE).setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST).createCompositeState(true);
        return create("mimicube", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, true, lvt_1_1_);
        //?}
    }

    public static RenderType getEyesFlickering(ResourceLocation p_228652_0_, float lightLevel) {
        //? if >=1.21.5 {
        /*return RenderType.eyes(p_228652_0_);
        *///?} else {
        RenderStateShard.TextureStateShard lvt_1_1_ = texState(p_228652_0_, false, false);
        return create("eye_flickering", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true, RenderType.CompositeState.builder().setTextureState(lvt_1_1_).setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_CULL_SHADER).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).createCompositeState(false));
        //?}
    }

    // The >=1.21.5 stand-in was RenderType.eyes(), which is ADDITIVE — it adds the texture's colour
    // to whatever is behind it, so every dark pixel becomes invisible and nothing ever occludes.
    // Upstream's full_bright is an ordinary *translucent* type that merely ignores nothing but its
    // own (always 240) lightmap coord, so the right substitute is entityTranslucentEmissive: same
    // blend, unlit. All three callers (void portal, void worm shot, pollen ball) are dark textures
    // over open sky, i.e. exactly the case additive erases. Part of #90.
    public static RenderType getFullBright(ResourceLocation p_228652_0_) {
        //? if >=1.21.5 {
        /*return RenderType.entityTranslucentEmissive(p_228652_0_);
        *///?} else {
        RenderStateShard.TextureStateShard lvt_1_1_ = texState(p_228652_0_, false, false);
        return create("full_bright", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true, RenderType.CompositeState.builder().setTextureState(lvt_1_1_).setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_CULL_SHADER).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).createCompositeState(false));
        //?}
    }

    public static RenderType getFreddy(ResourceLocation p_228652_0_) {
        //? if >=1.21.5 {
        /*return RenderType.eyes(p_228652_0_);
        *///?} else {
        RenderStateShard.TextureStateShard lvt_1_1_ = texState(p_228652_0_, false, false);
        return create("freddy", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true, RenderType.CompositeState.builder().setTextureState(lvt_1_1_).setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_CULL_SHADER).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setLightmapState(RenderStateShard.NO_LIGHTMAP).setCullState(NO_CULL).setOverlayState(OVERLAY).createCompositeState(true));
        //?}
    }


    public static RenderType getFrilledSharkTeeth(ResourceLocation p_228652_0_) {
        //? if >=1.21.5 {
        /*return RenderType.entityCutoutNoCull(p_228652_0_);
        *///?} else {
        RenderStateShard.TextureStateShard lvt_1_1_ = texState(p_228652_0_, false, false);
        return create("sharkteeth", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true, RenderType.CompositeState.builder().setTextureState(lvt_1_1_).setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_CULL_SHADER).setTransparencyState(NO_TRANSPARENCY).setCullState(NO_CULL).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).createCompositeState(false));
        //?}
    }

    public static RenderType getEyesNoCull(ResourceLocation p_228652_0_) {
        //? if >=1.21.5 {
        /*return RenderType.eyes(p_228652_0_);
        *///?} else {
        TextureStateShard lvt_1_1_ = texState(p_228652_0_, false, false);
        return create("eyes_no_cull", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true, RenderType.CompositeState.builder().setTextureState(lvt_1_1_).setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_CULL_SHADER).setTransparencyState(ADDITIVE_TRANSPARENCY).setWriteMaskState(COLOR_WRITE).setCullState(NO_CULL).createCompositeState(false));
        //?}
    }

    public static RenderType getSpectreBones(ResourceLocation p_228652_0_) {
        //? if >=1.21.5 {
        /*return RenderType.eyes(p_228652_0_);
        *///?} else {
        TextureStateShard lvt_1_1_ = texState(p_228652_0_, false, false);
        return create("spectre_bones", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true, RenderType.CompositeState.builder().setTextureState(lvt_1_1_).setShaderState(RENDERTYPE_EYES_SHADER).setTransparencyState(GHOST_TRANSPARANCY).setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_DEPTH_WRITE).setCullState(NO_CULL).setLightmapState(NO_LIGHTMAP).setOverlayState(OVERLAY).createCompositeState(false));
        //?}
    }

    public static RenderType getGhost(ResourceLocation p_228652_0_) {
        //? if >=1.21.5 {
        /*return RenderType.entityTranslucent(p_228652_0_);
        *///?} else {
        TextureStateShard lvt_1_1_ = texState(p_228652_0_, false, false);
        return create("ghost_am", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 262144, false, true, RenderType.CompositeState.builder().setTextureState(lvt_1_1_).setShaderState(RENDERTYPE_EYES_SHADER).setWriteMaskState(COLOR_DEPTH_WRITE).setDepthTestState(EQUAL_DEPTH_TEST).setLightmapState(NO_LIGHTMAP).setOverlayState(OVERLAY).setTransparencyState(GHOST_TRANSPARANCY).setCullState(RenderStateShard.NO_CULL).createCompositeState(true));
        //?}
    }

    public static RenderType getEyesAlphaEnabled(ResourceLocation locationIn) {
        //? if >=1.21.5 {
        /*return RenderType.eyes(locationIn);
        *///?} else {
        RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder().setShaderState(RENDERTYPE_EYES_SHADER).setTextureState(texState(locationIn, false, false)).setTransparencyState(WORM_TRANSPARANCY).setCullState(NO_CULL).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).setDepthTestState(EQUAL_DEPTH_TEST).createCompositeState(true);
        return create("eye_alpha", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, false, rendertype$compositestate);
        //?}
    }

    public static RenderType getEyesNoFog(ResourceLocation locationIn) {
        //? if >=1.21.5 {
        /*return RenderType.eyes(locationIn);
        *///?} else {
        RenderStateShard.TextureStateShard renderstateshard$texturestateshard = texState(locationIn, false, false);
        return create("eyes_nofog", DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 256, true, false, RenderType.CompositeState.builder().setShaderState(RENDERTYPE_OUTLINE_SHADER).setTextureState(renderstateshard$texturestateshard).setTransparencyState(LIGHTNING_TRANSPARENCY).setWriteMaskState(COLOR_DEPTH_WRITE).setCullState(NO_CULL).setDepthTestState(LEQUAL_DEPTH_TEST).setOverlayState(OVERLAY).createCompositeState(true));
        //?}
    }

    public static RenderType getSunbirdShine() {
        //? if >=1.21.5 {
        /*return RenderType.entityGlint();
        *///?} else {
        return create("sunbird_shine", DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, 256, true, true, RenderType.CompositeState.builder().setShaderState(RENDERTYPE_ENTITY_GLINT_SHADER).setTextureState(texState(AMCompat.rl("alexsmobs:textures/entity/sunbird_shine.png"), true, true)).setLightmapState(LIGHTMAP).setCullState(RenderStateShard.NO_CULL).setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY).setOverlayState(OVERLAY).setDepthTestState(LEQUAL_DEPTH_TEST).createCompositeState(true));
        //?}
    }

    public static RenderType getSkulkBoom() {
        //? if >=1.21.5 {
        /*return RenderType.entityTranslucent(AMCompat.rl("alexsmobs:textures/particle/skulk_boom.png"));
        *///?} else {
        CompositeState renderState = CompositeState.builder()
                .setShaderState(RENDERTYPE_ENERGY_SWIRL_SHADER)
                .setCullState(NO_CULL)
                .setTextureState(texState(AMCompat.rl("alexsmobs:textures/particle/skulk_boom.png"), true, true))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setWriteMaskState(COLOR_WRITE)
                .setDepthTestState(LEQUAL_DEPTH_TEST)
                .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                .createCompositeState(false);
        return create("skulk_boom", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, true, renderState);
        //?}
    }

    public static RenderType getUnderminer(ResourceLocation texture) {
        //? if >=1.21.5 {
        /*return RenderType.entityTranslucent(texture);
        *///?} else {
        CompositeState renderState = CompositeState.builder()
                .setShaderState(RENDERTYPE_ENERGY_SWIRL_SHADER)
                .setCullState(NO_CULL)
                .setTextureState(texState(texture, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setWriteMaskState(COLOR_DEPTH_WRITE)
                .setDepthTestState(LEQUAL_DEPTH_TEST)
                .setLayeringState(NO_LAYERING)
                .createCompositeState(false);
        return create("underminer", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, true, renderState);
        //?}
    }


    public static RenderType getGhostPickaxe(ResourceLocation texture) {
        //? if >=1.21.5 {
        /*return RenderType.itemEntityTranslucentCull(texture);
        *///?} else {
        CompositeState renderState = CompositeState.builder()
                .setShaderState(RENDERTYPE_ITEM_ENTITY_TRANSLUCENT_CULL_SHADER)
                .setCullState(NO_CULL)
                .setOutputState(ITEM_ENTITY_TARGET)
                .setTextureState(texState(texture, false, false))
                .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setWriteMaskState(COLOR_DEPTH_WRITE)
                .setDepthTestState(LEQUAL_DEPTH_TEST)
                .setLayeringState(NO_LAYERING)
                .createCompositeState(false);
        return create("ghost_pickaxe", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, true, renderState);
        //?}
    }

    public static RenderType getGhostCrumbling(ResourceLocation texture) {
        //? if >=1.21.5 {
        /*return RenderType.entityTranslucent(texture);
        *///?} else {
        TextureStateShard lvt_1_1_ = texState(texture, false, false);
        return create("ghost_crumbling_am", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 262144, false, true, RenderType.CompositeState.builder().setTextureState(lvt_1_1_).setShaderState(RenderStateShard.RENDERTYPE_ENERGY_SWIRL_SHADER).setTransparencyState(LIGHTNING_TRANSPARENCY).setCullState(NO_CULL).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).setLayeringState(VIEW_OFFSET_Z_LAYERING).setDepthTestState(LEQUAL_DEPTH_TEST).setCullState(RenderStateShard.NO_CULL).createCompositeState(true));
        //?}
    }

    public static RenderType getFarseerBeam() {
        //? if >=1.21.5 {
        /*return RenderType.entityTranslucent(STATIC_TEXTURE);
        *///?} else {
        CompositeState renderState = CompositeState.builder()
                .setShaderState(RENDERTYPE_ENERGY_SWIRL_SHADER)
                .setCullState(CULL)
                .setTextureState(texState(STATIC_TEXTURE, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setWriteMaskState(COLOR_WRITE)
                .setDepthTestState(LEQUAL_DEPTH_TEST)
                .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                .createCompositeState(false);
        return create("farseer_beam", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, true, renderState);
        //?}
    }

    /**
     * Emit the same geometry into two render types.
     *
     * <p>Upstream merged the two buffers with {@code VertexMultiConsumer} and wrote every vertex
     * once — {@code createMergedVertexConsumer(source.getBuffer(a), source.getBuffer(b))}. That is a
     * <b>hard crash from 1.21 on</b>. {@code MultiBufferSource.BufferSource} gives every render type
     * that is not one of vanilla's handful of <i>fixed</i> buffers a fresh {@code BufferBuilder} over
     * one <i>shared</i> {@code ByteBufferBuilder}, so asking for the second buffer calls
     * {@code endBatch} on the first — and the already-captured consumer throws
     * {@code IllegalStateException: Not building!} at its next vertex. On 1.20.1 the two render types
     * shared a single reusable {@code BufferBuilder} that was simply re-{@code begin}ed, so the same
     * code degraded silently (geometry merged into one batch) instead of crashing.
     *
     * <p>Two passes are equivalent — {@code VertexMultiConsumer} duplicated every vertex to both
     * consumers anyway — and are correct on every version, so this is deliberately <b>not</b>
     * version-gated: one code path for all nodes. It is also the shape the modern submit pipeline
     * forces (see {@code AlexsMobsFP}'s {@code LayerCombJellyRainbow}, which is two submits).
     *
     * <p>The buffer is fetched immediately before each pass and never held across the other, which is
     * the whole point — do not hoist the {@code getBuffer} calls back out.
     */
    public static void renderMerged(MultiBufferSource source, RenderType first, RenderType second, Consumer<VertexConsumer> geometry) {
        geometry.accept(source.getBuffer(first));
        geometry.accept(source.getBuffer(second));
    }

    /**
     * The static-over-a-shaped-mask pair (farseer emergence portal, static spark particle) — quad
     * geometry whose intended shape is the <i>mask texture's alpha</i>, not the quad.
     *
     * <p>Below 1.21.5 this is {@link #renderMerged} with the real composite {@code STATIC_*} types:
     * the mask pass writes depth, the static pass overlays it at EQUAL depth with glint-scroll UVs.
     * From 1.21.5 those composites are gone and the fallbacks are plain
     * {@code entityTranslucent(static.png)} — <b>no depth mask, no scroll</b> — which drew the whole
     * quad as an opaque square of noise (#53, the farseer's "square summoning particle" on 26.2).
     * There the pre-baked texture (static noise masked by the shape's alpha, generated by
     * {@code scripts/bake_static_textures.py}) is drawn in a single pass instead; callers cycle the
     * baked variant every couple of ticks to keep the flicker the scroll shard used to provide.
     */
    public static void renderStaticMasked(MultiBufferSource source, RenderType staticType, ResourceLocation maskTexture, ResourceLocation bakedTexture, Consumer<VertexConsumer> geometry) {
        //? if <1.21.5 {
        renderMerged(source, staticType, RenderType.entityTranslucent(maskTexture), geometry);
        //?} else {
        /*geometry.accept(source.getBuffer(RenderType.entityTranslucent(bakedTexture)));
        *///?}
    }

    /**
     * As {@link #renderStaticMasked} but for a mask whose shaped pass is not
     * {@code entityTranslucent} — the shattered void portal draws its mask through
     * {@code entityCutoutNoCull}, and below 1.21.5 that spelling has to be preserved exactly.
     *
     * <p>This is the same <i>shape</i> of site as {@link #renderStaticMasked} and the opposite of
     * {@link #renderStaticOverlay}: the geometry is a bare quad and the mask texture is pure black
     * with an alpha cut-out, so it has no content of its own. Routing it through the overlay helper
     * — whose >=1.21.5 arm drops the static pass and draws only the shaped one — left a solid black
     * disc where the portal should be (#90, "the shattered dimensional carver portal is a black
     * circle"), on every node >=1.21.5.
     */
    public static void renderStaticMasked(MultiBufferSource source, RenderType staticType, RenderType shaped, ResourceLocation bakedTexture, Consumer<VertexConsumer> geometry) {
        //? if <1.21.5 {
        renderMerged(source, staticType, shaped, geometry);
        //?} else {
        /*geometry.accept(source.getBuffer(RenderType.entityTranslucent(bakedTexture)));
        *///?}
    }

    /**
     * The static-over-model-geometry pair — geometry that is already the intended shape, drawn from
     * a texture that has content of its own, with static layered on top. After #90 the only site
     * left here is the transmutation table's overlay.
     *
     * <p>Below 1.21.5: {@link #renderMerged}, as upstream. From 1.21.5 the {@code STATIC_*}
     * fallbacks would re-draw the same geometry as opaque unscrolled noise at LEQUAL depth —
     * z-fighting static draped over the model — so the static pass is dropped and only the shaped
     * pass draws. A clean degrade, not a substitute.
     *
     * <p><b>Check the mask before using this helper.</b> If its RGB is all zero, the shaped pass has
     * nothing to draw and dropping the static pass leaves a black silhouette, not a degraded effect
     * — that is a {@link #renderStaticMasked} site however model-shaped its geometry looks. Three
     * sites were mis-classified this way (the shattered void portal, the farseer's eye and its
     * scars) and shipped black from {@code 2.0.0} to {@code 2.0.15}; the transmutation table's
     * overlay is genuinely textured, which is what makes it the one that belongs here.
     */
    public static void renderStaticOverlay(MultiBufferSource source, RenderType staticType, RenderType shaped, Consumer<VertexConsumer> geometry) {
        //? if <1.21.5 {
        renderMerged(source, staticType, shaped, geometry);
        //?} else {
        /*geometry.accept(source.getBuffer(shaped));
        *///?}
    }
}

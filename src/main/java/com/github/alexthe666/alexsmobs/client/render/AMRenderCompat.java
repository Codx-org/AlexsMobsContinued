package com.github.alexthe666.alexsmobs.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;

/**
 * The client-side twin of {@code AMCompat}: rendering entry points whose shape changed between
 * versions but whose meaning did not. Kept separate so nothing on the server touches a client
 * class.
 */
public class AMRenderCompat {

    /**
     * 1.21 replaced {@code Model#renderToBuffer}'s four float colour channels with one packed
     * ARGB int. This mod's own models still expose the float form (see Citadel's
     * {@code BasicEntityModel}), but plenty of call sites hold a plain vanilla {@code Model},
     * so the tint goes through here.
     */
    public static void renderToBuffer(Model model, PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        //? if >=1.21.2 {
        /*// 1.21.2 made BOTH of Model#renderToBuffer's overloads final, and the compat EntityModel
        // hands vanilla an empty root — so going through the vanilla entry point with one of this
        // mod's own models emits no vertices at all. The hierarchy's real draw call is the compat
        // model's eight-float overload (compat/RenderLayer#renderColoredModel says the same thing
        // one level down). Without this dispatch every caller of this method that holds one of the
        // mod's models drew NOTHING from 1.21.2 up: the rainbow layer, the basic glow layer, the
        // void worm's glow, the underminer, the sand shot and the mosquito spit.
        if (model instanceof com.github.alexthe666.alexsmobs.client.render.compat.EntityModel<?> amModel) {
            amModel.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        } else {
            model.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, packColor(red, green, blue, alpha));
        }
        *///?} elif >=1.21 {
        /*model.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, packColor(red, green, blue, alpha));
        *///?} else {
        model.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        //?}
    }

    /**
     * Packs float RGBA into the ARGB int 1.21 renderers take. Only used from that branch, but it
     * has to compile everywhere because Stonecutter comments out bodies, not whole methods.
     */
    public static int packColor(float red, float green, float blue, float alpha) {
        return ((int) (alpha * 255.0F) & 255) << 24
                | ((int) (red * 255.0F) & 255) << 16
                | ((int) (green * 255.0F) & 255) << 8
                | ((int) (blue * 255.0F) & 255);
    }

    /**
     * 1.21 replaced {@code Sheep#getColorArray} with {@code Sheep#getColor}, which hands back the
     * same wool tint packed into an int. 1.21.6 removed that static too — {@code Sheep#getColor} is
     * an instance method returning the mob's own {@code DyeColor} — so the tint comes straight off
     * the colour, which is what the deleted static did internally.
     */
    public static float[] dyeColorArray(net.minecraft.world.item.DyeColor color) {
        //? if >=1.21.6 {
        /*int packed = color.getTextureDiffuseColor();
        return new float[]{
                net.minecraft.util.ARGB.red(packed) / 255.0F,
                net.minecraft.util.ARGB.green(packed) / 255.0F,
                net.minecraft.util.ARGB.blue(packed) / 255.0F};
        *///?} elif >=1.21 {
        /*int packed = net.minecraft.world.entity.animal.Sheep.getColor(color);
        return new float[]{
                net.minecraft.util.FastColor.ARGB32.red(packed) / 255.0F,
                net.minecraft.util.FastColor.ARGB32.green(packed) / 255.0F,
                net.minecraft.util.FastColor.ARGB32.blue(packed) / 255.0F};
        *///?} else {
        return net.minecraft.world.entity.animal.Sheep.getColorArray(color);
        //?}
    }

    /**
     * The baked model of whatever renderer is registered for an entity, or {@code null} if that
     * renderer has no model. Six call sites reach for another entity's model — the falconry glove
     * arm, the raccoon a blue jay perches on, the passenger in a kangaroo's pouch — and all of them
     * want the *vanilla* renderer, not this mod's {@code client/render/compat} stand-in, which is
     * what the same-simple-name import swap would otherwise give them.
     */
    @SuppressWarnings("rawtypes")
    public static net.minecraft.client.model.EntityModel<?> rendererModel(net.minecraft.world.entity.Entity entity) {
        net.minecraft.client.renderer.entity.EntityRenderer render =
                net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
        if (render instanceof net.minecraft.client.renderer.entity.LivingEntityRenderer living) {
            return (net.minecraft.client.model.EntityModel<?>) living.getModel();
        }
        return null;
    }

    /**
     * Renders one entity inside another's renderer, at the pose stack's current position and with
     * the given body yaw. Four places do this (the kangaroo/anteater pouch, the squid in a cachalot
     * whale's mouth, and the falconry bird in first person) and each carried its own copy of the
     * vanilla dispatcher's crash-report wrapping.
     *
     * <p>From 1.21.2 the renderer no longer takes the entity or a yaw: it extracts a render state
     * first and renders that, so the yaw is applied by overwriting the state's rotations. Building
     * the state here also runs {@code EntityRendererMixin}, which is what keeps
     * {@code AMStateAccess} valid for anything these nested renders trigger.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <E extends net.minecraft.world.entity.Entity> void renderEntity(E entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        Object render = null;
        try {
            render = net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
            if (render != null) {
                //? if >=1.21.9 {
                /*net.minecraft.client.renderer.entity.EntityRenderer raw = (net.minecraft.client.renderer.entity.EntityRenderer) render;
                net.minecraft.client.renderer.entity.state.EntityRenderState state = raw.createRenderState(entity, partialTick);
                if (state instanceof net.minecraft.client.renderer.entity.state.LivingEntityRenderState living) {
                    living.bodyRot = yaw;
                    living.yRot = yaw;
                }
                // 1.21.9's submit(state, pose, collector, camera) has NO light parameter: the light
                // travels in the state, where extractRenderState has just filled it in from the
                // entity's own block position. Every caller here passes a light of its own -- the
                // enclosing renderer's for a nested in-world render, full bright for a GUI one --
                // and dropping it is what made the advancement, dictionary and creative-tab mobs
                // render black on 1.21.9+. Writing it back reproduces the pre-1.21.9 call exactly.
                state.lightCoords = packedLight;
                // A nested render has to reach the modern submit API; unwrap the collector out of the
                // recording buffer source the enclosing legacy body was handed.
                com.github.alexthe666.alexsmobs.client.render.compat.AMSubmitBuffers submit = com.github.alexthe666.alexsmobs.client.render.compat.AMSubmitBuffers.of(buffers);
                if (submit != null) {
                    submit.flush();
                    raw.submit(state, poseStack, submit.collector(), submit.camera());
                }
                *///?} elif >=1.21.2 {
                /*net.minecraft.client.renderer.entity.EntityRenderer raw = (net.minecraft.client.renderer.entity.EntityRenderer) render;
                net.minecraft.client.renderer.entity.state.EntityRenderState state = raw.createRenderState(entity, partialTick);
                if (state instanceof net.minecraft.client.renderer.entity.state.LivingEntityRenderState living) {
                    living.bodyRot = yaw;
                    living.yRot = yaw;
                }
                raw.render(state, poseStack, buffers, packedLight);
                *///?} else {
                ((net.minecraft.client.renderer.entity.EntityRenderer) render).render(entity, yaw, partialTick, poseStack, buffers, packedLight);
                //?}
            }
        } catch (Throwable throwable) {
            net.minecraft.CrashReport crashreport = net.minecraft.CrashReport.forThrowable(throwable, "Rendering entity in world");
            entity.fillCrashReportCategory(crashreport.addCategory("Entity being rendered"));
            net.minecraft.CrashReportCategory category = crashreport.addCategory("Renderer details");
            category.setDetail("Assigned renderer", render);
            category.setDetail("Rotation", Float.valueOf(yaw));
            category.setDetail("Delta", Float.valueOf(partialTick));
            throw new net.minecraft.ReportedException(crashreport);
        }
    }

    /**
     * 1.21.9 removed {@code EntityRenderDispatcher#cameraOrientation()} — the orientation lives on
     * the per-frame {@code CameraRenderState} now. The dispatcher still holds the {@code Camera}
     * itself, and {@code Camera#rotation()} is exactly what the old accessor returned.
     */
    public static org.joml.Quaternionf cameraOrientation(net.minecraft.client.renderer.entity.EntityRenderDispatcher dispatcher) {
        //? if >=1.21.9 {
        /*return dispatcher.camera.rotation();
        *///?} else {
        return dispatcher.cameraOrientation();
        //?}
    }

    // Stateless token BlockModelResolver#update wants; vanilla holds one static field per renderer.
    //? if >=26 {
    /*private static final net.minecraft.client.renderer.block.model.BlockDisplayContext BLOCK_DISPLAY_CONTEXT =
            net.minecraft.client.renderer.block.model.BlockDisplayContext.create();
    *///?}

    /**
     * 26.1 deleted {@code BlockRenderDispatcher#renderSingleBlock}. A block model is resolved into a
     * {@code BlockModelRenderState} by {@code BlockModelResolver} and then submitted — the shape
     * vanilla's own {@code MushroomCowRenderer} + {@code MushroomCowMushroomLayer} use.
     */
    public static void renderSingleBlock(net.minecraft.world.level.block.state.BlockState state, PoseStack poseStack,
                                         MultiBufferSource buffers, int packedLight, int packedOverlay) {
        //? if >=26 {
        /*net.minecraft.client.renderer.SubmitNodeCollector collector =
                com.github.alexthe666.alexsmobs.client.render.compat.AMSubmitBuffers.collectorOf(buffers);
        if (collector == null) {
            return;
        }
        net.minecraft.client.renderer.block.BlockModelRenderState renderState =
                new net.minecraft.client.renderer.block.BlockModelRenderState();
        // Minecraft#getBlockModelResolver is a NeoForge patch — on Forge the field is private. The
        // class is a stateless public-ctor wrapper over ModelManager (which both loaders expose),
        // so building one per call is equivalent and keeps a single code path.
        new net.minecraft.client.renderer.block.BlockModelResolver(net.minecraft.client.Minecraft.getInstance().getModelManager())
                .update(renderState, state, BLOCK_DISPLAY_CONTEXT);
        renderState.submit(poseStack, collector, packedLight, packedOverlay, 0);
        *///?} else {
        net.minecraft.client.Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, poseStack, buffers, packedLight, packedOverlay);
        //?}
    }

    /**
     * 26.1 deleted {@code BlockColors#getColor(BlockState, BlockAndTintGetter, BlockPos, int)}; a
     * block's tint is now a {@code BlockTintSource} per layer, {@code null} when the block has none.
     * The old method's "no colour handler" answer was {@code -1}, so reproduce that.
     */
    public static int blockTintColor(net.minecraft.world.level.block.state.BlockState state, int layer) {
        //? if >=26 {
        /*net.minecraft.client.color.block.BlockTintSource source =
                net.minecraft.client.Minecraft.getInstance().getBlockColors().getTintSource(state, layer);
        // ⚠️ The space after the dot is LOAD-BEARING: the !mc121-vtx-color rule rewrites every
        // ".color(" in the tree to ".setColor(" for the vertex-consumer sweep, and BlockTintSource's
        // method is genuinely named color(). Java ignores the whitespace; the rule cannot see past it.
        return source == null ? -1 : source. color(state);
        *///?} else {
        return net.minecraft.client.Minecraft.getInstance().getBlockColors().getColor(state, null, null, layer);
        //?}
    }

    /**
     * 26.1 removed {@code BlockRenderDispatcher} (and with it {@code getBlockModelShaper}); the
     * particle sprite of a block state now comes off the model manager's {@code BlockStateModelSet},
     * which is what vanilla's own {@code TerrainParticle} uses.
     */
    public static net.minecraft.client.renderer.texture.TextureAtlasSprite blockParticleSprite(net.minecraft.world.level.block.state.BlockState state) {
        //? if >=26 {
        /*return net.minecraft.client.Minecraft.getInstance().getModelManager().getBlockStateModelSet().getParticleMaterial(state).sprite();
        *///?} elif >=1.21.5 {
        /*return net.minecraft.client.Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getBlockModel(state).particleIcon();
        *///?} else {
        return net.minecraft.client.Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getBlockModel(state).getParticleIcon();
        //?}
    }

    /**
     * 1.21.2 removed {@code RenderSystem.runAsFancy(Runnable)} (fancy graphics is no longer a toggle
     * the caller flips around a draw). Just run the body.
     */
    public static void runAsFancy(Runnable runnable) {
        //? if >=1.21.2 {
        /*runnable.run();
        *///?} else {
        com.mojang.blaze3d.systems.RenderSystem.runAsFancy(runnable);
        //?}
    }

    /**
     * 1.21.2 gave {@code GuiGraphics#blit} a leading {@code Function<ResourceLocation, RenderType>}
     * and made the u/v offsets floats. The old seven-argument {@code blit(rl, x, y, u, v, w, h)}
     * assumed a 256×256 texture; reproduce it through {@link RenderType#guiTextured}.
     */
    public static void blit(net.minecraft.client.gui.GuiGraphics guiGraphics, net.minecraft.resources.ResourceLocation texture, int x, int y, int uOffset, int vOffset, int uWidth, int vHeight) {
        //? if >=1.21.6 {
        /*guiGraphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, texture, x, y, (float) uOffset, (float) vOffset, uWidth, vHeight, 256, 256);
        *///?} elif >=1.21.2 {
        /*guiGraphics.blit(RenderType::guiTextured, texture, x, y, (float) uOffset, (float) vOffset, uWidth, vHeight, 256, 256);
        *///?} else {
        guiGraphics.blit(texture, x, y, uOffset, vOffset, uWidth, vHeight);
        //?}
    }

    /**
     * 1.21.6 replaced {@code GuiGraphics#pose()}'s {@code PoseStack} with a 2D
     * {@code org.joml.Matrix3x2fStack} — GUI transforms have no depth any more, so the z offset is
     * dropped. (push/pop are handled by Stonecutter rules; only the arity-3 calls need a helper.)
     */
    public static void translateGui(net.minecraft.client.gui.GuiGraphics guiGraphics, double x, double y, double z) {
        //? if >=1.21.6 {
        /*guiGraphics.pose().translate((float) x, (float) y);
        *///?} else {
        guiGraphics.pose().translate(x, y, z);
        //?}
    }

    /** The {@code scale} half of {@link #translateGui}. */
    public static void scaleGui(net.minecraft.client.gui.GuiGraphics guiGraphics, float x, float y, float z) {
        //? if >=1.21.6 {
        /*guiGraphics.pose().scale(x, y);
        *///?} else {
        guiGraphics.pose().scale(x, y, z);
        //?}
    }

    //? if >=1.21.6 {
    /*/^*
     * 1.21.6 removed the immediate-mode entity-in-GUI path outright: {@code GuiGraphics} has no
     * {@code PoseStack}, no {@code flush()}, and {@code RenderSystem.setShaderLights} is gone. A GUI
     * entity is a deferred picture-in-picture submission now — its viewport rectangle is in absolute
     * screen coordinates and the entity's origin lands at that rectangle's centre, with the Y flip
     * and the {@code ENTITY_IN_UI} lighting applied by vanilla. That is exactly what the old
     * "translate to (x,y), scale by s, rotate, render at origin" sequence did, so the only new input
     * is a box big enough not to clip the mob.
     ^/
    public static void submitGuiEntity(net.minecraft.client.gui.GuiGraphics guiGraphics, net.minecraft.world.entity.Entity entity,
                                       int centerX, int centerY, float scale, float partialTick,
                                       org.joml.Quaternionf rotation, org.joml.Quaternionf cameraAngle) {
        net.minecraft.client.renderer.entity.EntityRenderer<? super net.minecraft.world.entity.Entity, ?> renderer =
                net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
        net.minecraft.client.renderer.entity.state.EntityRenderState state = renderer.createRenderState(entity, partialTick);
        state.hitboxesRenderState = null;
        guiEntityFullBright(state);
        // That box is a hard VIEWPORT, not a hint — vanilla's own callers hand it the widget
        // rectangle precisely so the inventory doll gets cut off at the panel edge. Both callers
        // here want the whole mob, the way the pre-1.21.6 immediate path drew it, so size it from
        // the entity rather than guess. It has to stay CENTRED on the anchor (the origin lands at
        // the centre), so one symmetric half-extent must clear the model in every direction: the
        // full bbHeight straight up, and, once the book's 30 degrees of pitch have tilted it, about
        // 0.71*bbWidth + 0.5*bbHeight sideways. The 1.5x + 0.5 on top of that is headroom for the
        // many models that overhang their hitbox, and the 2.5 floor is the old constant, so no mob
        // that fitted before can start clipping now.
        //
        // #62 ("some mobs in the dictionary are missing parts of their body"): a 3.1x3.5 elephant
        // needs 3.5 blocks of headroom and got 2.5, so its back and head were sliced off in its
        // index slot AND on its own page, on every node from 1.21.6 up. Every mob taller or wider
        // than 5 blocks across shared it; below 1.21.6 nothing clips, which is why it reads as a
        // "some versions" fault.
        float bbH = entity.getBbHeight();
        float bbW = entity.getBbWidth();
        float blocks = Math.max(2.5F, Math.max(bbH, 0.71F * bbW + 0.5F * bbH) * 1.5F + 0.5F);
        int half = Math.min(512, Math.max(1, Math.round(Math.abs(scale) * blocks)));
        guiGraphics.submitEntityRenderState(state, Math.abs(scale), new org.joml.Vector3f(),
                rotation, cameraAngle, centerX - half, centerY - half, centerX + half, centerY + half);
    }
    *///?}

    /**
     * Makes a freshly-extracted render state fit to be drawn in a GUI, which is not what
     * {@code extractRenderState} leaves it as. A separate method because Stonecutter blocks never
     * nest and its body's boundary (1.21.9) is inside {@link #submitGuiEntity}'s (1.21.6).
     *
     * <p>1.21.9 moved three per-frame values that used to be arguments onto the state itself, so a
     * state built for a GUI now carries the values the entity happens to have <em>in the world</em>:
     * the light of whatever block it is standing in, its team outline, and its ground shadow. On
     * 1.21.6&ndash;1.21.8 none of that reached here — {@code GuiEntityRenderer} passed
     * {@code 15728880} to {@code EntityRenderDispatcher#render} itself.
     *
     * <p>Symptom of the light half: <b>every mob drawn in the animal dictionary, on an advancement
     * icon and in the animated creative-tab icon rendered black</b> whenever the fake entity's
     * position was unlit — i.e. at night or underground, which is most of the time. Vanilla's own
     * {@code InventoryScreen#extractRenderState} sets exactly these three, and this mirrors it.
     */
    public static void guiEntityFullBright(Object renderState) {
        //? if >=1.21.9 {
        /*net.minecraft.client.renderer.entity.state.EntityRenderState state =
                (net.minecraft.client.renderer.entity.state.EntityRenderState) renderState;
        state.lightCoords = 15728880;
        state.outlineColor = 0;
        state.shadowPieces.clear();
        *///?}
    }

    /**
     * The tinted, explicitly-sized form of the above. 1.21.6 replaced {@code blit}'s leading
     * {@code Function<ResourceLocation, RenderType>} with a {@code RenderPipeline}
     * ({@code RenderType#guiTextured} is gone), so every tinted GUI blit in the tree goes through
     * here rather than naming either API at the call site.
     */
    public static void blitTinted(net.minecraft.client.gui.GuiGraphics guiGraphics, net.minecraft.resources.ResourceLocation texture, int x, int y, float u, float v, int width, int height, int texWidth, int texHeight, int argb) {
        //? if >=1.21.6 {
        /*guiGraphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, texWidth, texHeight, argb);
        *///?} elif >=1.21.2 {
        /*guiGraphics.blit(RenderType::guiTextured, texture, x, y, u, v, width, height, texWidth, texHeight, argb);
        *///?} else {
        // Pre-1.21.2 GuiGraphics has no tinted blit; nothing calls this on those nodes.
        guiGraphics.blit(texture, x, y, (int) u, (int) v, width, height, texWidth, texHeight);
        //?}
    }

    /**
     * A tinted blit given NORMALISED uv corners, where the destination rectangle is NOT the same
     * size as the sampled source region. {@link #blitTinted} cannot express that — its {@code blit}
     * overload collapses source size into destination size — which silently broke the animal
     * dictionary: {@code GuiBasicBook} blits a 512×512 page texture across the book's whole
     * 390×320 frame, so renormalising through the 1:1 form asked for u1 = 390/256 and sampled well
     * past the texture edge.
     * <p>
     * The 13-argument overload separates the two (source {@code srcWidth}/{@code srcHeight} vs
     * destination {@code width}/{@code height}). It still wants integers in texel space, so the
     * normalised corners are expressed against an arbitrary reference size — big enough that the
     * rounding is far below one texel of any GUI texture in the mod.
     */
    public static void blitTintedUV(net.minecraft.client.gui.GuiGraphics guiGraphics, net.minecraft.resources.ResourceLocation texture, int startX, int startY, int endX, int endY, float u0, float u1, float v0, float v1, int argb) {
        final int ref = 4096;
        float u = u0 * ref;
        float v = v0 * ref;
        int srcWidth = Math.round((u1 - u0) * ref);
        int srcHeight = Math.round((v1 - v0) * ref);
        int width = endX - startX;
        int height = endY - startY;
        //? if >=1.21.6 {
        /*guiGraphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, texture, startX, startY, u, v, width, height, srcWidth, srcHeight, ref, ref, argb);
        *///?} elif >=1.21.2 {
        /*guiGraphics.blit(RenderType::guiTextured, texture, startX, startY, u, v, width, height, srcWidth, srcHeight, ref, ref, argb);
        *///?} else {
        // Pre-1.21.2 GuiGraphics has no tinted blit; nothing calls this on those nodes.
        guiGraphics.blit(texture, startX, startY, width, height, u, v, srcWidth, srcHeight, ref, ref);
        //?}
    }

    /**
     * 1.21 dropped {@code getArmorFoilBuffer}'s "no entity" flag; every call here passed
     * {@code false} for it anyway. 1.21.9 then removed the armour-specific variant altogether and
     * unified it with the item one — {@code getFoilBuffer}'s first flag picks the block-style glint
     * over the entity one, so {@code false} is the armour/entity behaviour the old method had.
     * 26.2 then deleted the public helper AND {@code VertexMultiConsumer} with it, so the two-way
     * consumer is vendored (see {@code compat/AMVertexMultiConsumer}) and the body below is
     * 26.1's {@code getFoilBuffer(source, renderType, false, withGlint)} written out. The
     * {@code sheeted=false} branch is the only one this mod ever took, and vanilla's
     * fabulous-graphics {@code glintTranslucent} variant only applied to item-entity-target
     * output, which none of these four call sites produce.
     */
    public static VertexConsumer armorFoilBuffer(MultiBufferSource source, RenderType renderType, boolean withGlint) {
        //? if >=26.2 {
        /*if (!withGlint) {
            return source.getBuffer(renderType);
        }
        return new com.github.alexthe666.alexsmobs.client.render.compat.AMVertexMultiConsumer(
                source.getBuffer(RenderType.entityGlint()), source.getBuffer(renderType));
        *///?} elif >=1.21.9 {
        /*return ItemRenderer.getFoilBuffer(source, renderType, false, withGlint);
        *///?} elif >=1.21 {
        /*return ItemRenderer.getArmorFoilBuffer(source, renderType, withGlint);
        *///?} else {
        return ItemRenderer.getArmorFoilBuffer(source, renderType, false, withGlint);
        //?}
    }

    /**
     * {@code Font#drawInBatch}, which 26.2 deleted along with the rest of the immediate-mode text
     * path — {@code Font} only exposes {@code prepareText} and a glyph-visitor model now, and the
     * submit pipeline is what draws. The replacement is
     * {@code OrderedSubmitNodeCollector#submitText}, whose parameters line up one for one; it takes
     * the {@code PoseStack} rather than a {@code Matrix4f}, which is the same transform (the matrix
     * every caller passed was {@code poseStack.last().pose()}, a live reference into the stack).
     * Only the seal's tears easter-egg name tag calls this.
     */
    public static void drawTextInBatch(net.minecraft.client.gui.Font font, String text, float x, float y, int color, boolean dropShadow,
                                       PoseStack poseStack, MultiBufferSource buffer, int backgroundColor, int packedLight) {
        //? if >=26.2 {
        /*com.github.alexthe666.alexsmobs.client.render.compat.AMSubmitBuffers submit = com.github.alexthe666.alexsmobs.client.render.compat.AMSubmitBuffers.of(buffer);
        if (submit == null) {
            return;
        }
        submit.flush();
        submit.collector().submitText(poseStack, x, y,
                net.minecraft.util.FormattedCharSequence.forward(text, net.minecraft.network.chat.Style.EMPTY),
                dropShadow, net.minecraft.client.gui.Font.DisplayMode.NORMAL, packedLight, color, backgroundColor, 0);
        *///?} else {
        font.drawInBatch(text, x, y, color, dropShadow, poseStack.last().pose(), buffer,
                net.minecraft.client.gui.Font.DisplayMode.NORMAL, backgroundColor, packedLight);
        //?}
    }

    /**
     * 1.21.5 dropped the {@code boolean leftHand} parameter from
     * {@code ItemInHandRenderer#renderItem}. Call sites still pass it; this swallows it on
     * >=1.21.5 and forwards it below.
     */
    public static void renderItemInHand(net.minecraft.client.renderer.ItemInHandRenderer renderer,
                                        net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.item.ItemStack stack,
                                        net.minecraft.world.item.ItemDisplayContext ctx, boolean left, PoseStack ps,
                                        MultiBufferSource buf, int light) {
        //? if >=1.21.9 {
        /*com.github.alexthe666.alexsmobs.client.render.compat.AMSubmitBuffers submit = com.github.alexthe666.alexsmobs.client.render.compat.AMSubmitBuffers.of(buf);
        if (submit != null) {
            submit.flush();
            renderer.renderItem(entity, stack, ctx, ps, submit.collector(), light);
        }
        *///?} elif >=1.21.5 {
        /*renderer.renderItem(entity, stack, ctx, ps, buf, light);
        *///?} else {
        renderer.renderItem(entity, stack, ctx, left, ps, buf, light);
        //?}
    }

    /**
     * {@code ItemRenderer#renderStatic}, which 1.21.9 deleted along with the rest of the
     * immediate-mode item path. The replacement is to resolve the stack into an
     * {@code ItemStackRenderState} and submit that; the argument list is deliberately identical to
     * the old method's so the ~16 call sites read the same on every node.
     */
    public static void renderItemStatic(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.ItemDisplayContext ctx,
                                        int packedLight, int packedOverlay, PoseStack ps, MultiBufferSource buf,
                                        net.minecraft.world.level.Level level, int seed) {
        //? if >=1.21.9 {
        /*com.github.alexthe666.alexsmobs.client.render.compat.AMSubmitBuffers submit = com.github.alexthe666.alexsmobs.client.render.compat.AMSubmitBuffers.of(buf);
        if (submit == null) {
            return;
        }
        submit.flush();
        net.minecraft.client.renderer.item.ItemStackRenderState state = new net.minecraft.client.renderer.item.ItemStackRenderState();
        net.minecraft.client.Minecraft.getInstance().getItemModelResolver().updateForTopItem(state, stack, ctx, level, null, seed);
        // Last argument is the tint; 0 is "no tint", which is what renderStatic did.
        state.submit(ps, submit.collector(), packedLight, packedOverlay, 0);
        *///?} else {
        net.minecraft.client.Minecraft.getInstance().getItemRenderer().renderStatic(stack, ctx, packedLight, packedOverlay, ps, buf, level, seed);
        //?}
    }

    /**
     * 26.1 removed {@code HumanoidModel#setAllVisible}. It only ever set the seven standard parts,
     * so spelling them out reproduces it exactly.
     */
    public static void setAllVisible(net.minecraft.client.model.HumanoidModel<?> model, boolean visible) {
        //? if >=26 {
        /*model.head.visible = visible;
        model.hat.visible = visible;
        model.body.visible = visible;
        model.rightArm.visible = visible;
        model.leftArm.visible = visible;
        model.rightLeg.visible = visible;
        model.leftLeg.visible = visible;
        *///?} else {
        model.setAllVisible(visible);
        //?}
    }

    /**
     * Which armour-stand layer to bake a plain {@code HumanoidModel} from, for the slot that model
     * is going to draw.
     *
     * <p>Through 1.21.8 there was one humanoid armour mesh per size (INNER/OUTER) and it carried
     * every part, so any slot could be drawn from either — which is why both call sites used to ask
     * for OUTER and sort the rest out with {@code visible} flags. 1.21.9 replaced that pair with a
     * per-slot {@code ArmorModelSet}, and each slot's mesh keeps ONLY that slot's parts: the others
     * survive as cube-less shells at the same pose (bytecode: {@code PartDefinition#retainExactParts}
     * re-adds them with an empty {@code CubeListBuilder}). So a helmet baked from the chest mesh
     * constructs, poses and submits perfectly and draws nothing at all, with no exception to point
     * at and nothing for the compiler to see.
     */
    public static net.minecraft.client.model.geom.ModelLayerLocation armorStandArmorLayer(net.minecraft.world.entity.EquipmentSlot slot) {
        //? if >=1.21.9 {
        /*return net.minecraft.client.model.geom.ModelLayers.ARMOR_STAND_ARMOR.get(slot);
        *///?} else {
        return slot == net.minecraft.world.entity.EquipmentSlot.LEGS
                ? net.minecraft.client.model.geom.ModelLayers.ARMOR_STAND_INNER_ARMOR
                : net.minecraft.client.model.geom.ModelLayers.ARMOR_STAND_OUTER_ARMOR;
        //?}
    }
}

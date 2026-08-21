package com.github.alexthe666.alexsmobs.client.render;

import com.github.alexthe666.alexsmobs.misc.AMCompat;

import com.github.alexthe666.alexsmobs.client.model.ModelUnderminerDwarf;
import com.github.alexthe666.alexsmobs.client.model.ModelUnderminerHumanoid;
import com.github.alexthe666.alexsmobs.client.model.layered.AMModelLayers;
import com.github.alexthe666.alexsmobs.client.render.layer.LayerUnderminerItem;
import com.github.alexthe666.alexsmobs.entity.EntityUnderminer;
import com.mojang.blaze3d.vertex.PoseStack;
//? if <26
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
// The only use of this type is in the <1.21.2 arm below, which no Fabric node ever
// reaches; Fabric has no such event, so the import itself has to go.
//? if !fabric
import net.minecraftforge.client.event.RenderNameTagEvent;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RenderUnderminer extends MobRenderer<EntityUnderminer, EntityModel<EntityUnderminer>> {
    private static final ResourceLocation TEXTURE_DWARF = AMCompat.rl("alexsmobs:textures/entity/underminer_dwarf.png");
    private static final ResourceLocation TEXTURE_0 = AMCompat.rl("alexsmobs:textures/entity/underminer_0.png");
    private static final ResourceLocation TEXTURE_1 = AMCompat.rl("alexsmobs:textures/entity/underminer_1.png");
    public static final List<ResourceLocation> BREAKING_LOCATIONS = IntStream.range(0, 10).mapToObj((destroyStage) -> AMCompat.rl("alexsmobs:textures/block/ghostly_pickaxe/destroy_stage_" + destroyStage + ".png")).collect(Collectors.toList());
    private static final ModelUnderminerDwarf DWARF_MODEL = new ModelUnderminerDwarf();
    // The tall (non-dwarf) form, 30% of spawns. ModelUnderminerHumanoid is upstream's plain
    // HumanoidModel below 1.21.2 and a compat-model wrapper around one above it — see #57, where
    // dropping this on 1.21.2+ left those underminers wearing their own skin on dwarf geometry.
    private static ModelUnderminerHumanoid NORMAL_MODEL = null;
    private static final List<RenderType> DESTROY_TYPES = BREAKING_LOCATIONS.stream().map(AMRenderTypes::getGhostCrumbling).collect(Collectors.toList());
    public static boolean renderWithPickaxe = false;

    public RenderUnderminer(EntityRendererProvider.Context renderManagerIn) {
        super(renderManagerIn, DWARF_MODEL, 0.4F);
        NORMAL_MODEL = new ModelUnderminerHumanoid(renderManagerIn.bakeLayer(AMModelLayers.UNDERMINER));
        this.addLayer(new LayerUnderminerItem(this));
    }

    protected void scale(EntityUnderminer entitylivingbaseIn, PoseStack matrixStackIn, float partialTickTime) {
        matrixStackIn.scale(0.925F, 0.925F, 0.925F);
    }

    public boolean shouldRender(EntityUnderminer livingEntityIn, Frustum camera, double camX, double camY, double camZ) {
        if (super.shouldRender(livingEntityIn, camera, camX, camY, camZ)) {
            return true;
        } else {
            if (livingEntityIn.getMiningPos() != null) {
                BlockPos pos = livingEntityIn.getMiningPos();
                if (pos != null) {
                    Vec3 vector3d = Vec3.atLowerCornerOf(pos);
                    Vec3 vector3dCorner = Vec3.atLowerCornerOf(pos).add(1, 1, 1);
                    return camera.isVisible(new AABB(vector3d.x, vector3d.y, vector3d.z, vector3dCorner.x, vector3dCorner.y, vector3dCorner.z));
                }
            }
            return false;
        }
    }

    protected float getFlipDegrees(EntityUnderminer entityUnderminer) {
        return 0.0F;
    }

    public void render(EntityUnderminer entityIn, float entityYaw, float partialTicks, PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn) {
        //? if >=1.21.2 {
        /*if (com.github.alexthe666.alexsmobs.client.render.compat.AMRenderEventCompat.firePre(this.renderingState, this, partialTicks, matrixStackIn, bufferIn, packedLightIn))
        *///?} elif fabric {
        /*// Fabric has no RenderLivingEvent, so nothing can cancel this render. The `if (false)` is
        // what keeps the shared `return;` on the line below — that line is outside the gate — legal on
        // every arm without duplicating the whole method body per loader.
        if (false)
        *///?} else {
        if (com.github.alexthe666.alexsmobs.misc.AMPlatform.postCancelled(new net.minecraftforge.client.event.RenderLivingEvent.Pre<EntityUnderminer, EntityModel<EntityUnderminer>>(entityIn, this, partialTicks, matrixStackIn, bufferIn, packedLightIn)))
        //?}
            return;
        matrixStackIn.pushPose();
        this.model.attackTime = this.getAttackAnim(entityIn, partialTicks);

        boolean shouldSit = entityIn.isPassenger() && (entityIn.getVehicle() != null && com.github.alexthe666.alexsmobs.misc.AMCompat.shouldRiderSit(entityIn.getVehicle()));
        this.model.riding = shouldSit;
        this.model.young = entityIn.isBaby();
        float f = Mth.rotLerp(partialTicks, entityIn.yBodyRotO, entityIn.yBodyRot);
        float f1 = Mth.rotLerp(partialTicks, entityIn.yHeadRotO, entityIn.yHeadRot);
        float f2 = f1 - f;
        if (shouldSit && entityIn.getVehicle() instanceof LivingEntity) {
            LivingEntity livingentity = (LivingEntity) entityIn.getVehicle();
            f = Mth.rotLerp(partialTicks, livingentity.yBodyRotO, livingentity.yBodyRot);
            f2 = f1 - f;
            float f3 = Mth.wrapDegrees(f2);
            if (f3 < -85.0F) {
                f3 = -85.0F;
            }

            if (f3 >= 85.0F) {
                f3 = 85.0F;
            }

            f = f1 - f3;
            if (f3 * f3 > 2500.0F) {
                f += f3 * 0.2F;
            }

            f2 = f1 - f;
        }

        float f6 = Mth.lerp(partialTicks, entityIn.xRotO, entityIn.getXRot());
        if (entityIn.getPose() == Pose.SLEEPING) {
            Direction direction = entityIn.getBedOrientation();
            if (direction != null) {
                float f4 = entityIn.getEyeHeight(Pose.STANDING) - 0.1F;
                matrixStackIn.translate((float) (-direction.getStepX()) * f4, 0.0D, (float) (-direction.getStepZ()) * f4);
            }
        }

        float f7 = this.getBob(entityIn, partialTicks);
        // 1.20.5 added the entity's scale as a sixth setupRotations argument.
        //? if >=1.20.5
        //this.setupRotations(entityIn, matrixStackIn, f7, f, partialTicks, entityIn.getScale());
        //? if <1.20.5
        this.setupRotations(entityIn, matrixStackIn, f7, f, partialTicks);
        matrixStackIn.scale(-1.0F, -1.0F, 1.0F);
        this.scale(entityIn, matrixStackIn, partialTicks);
        matrixStackIn.translate(0.0D, -1.501F, 0.0D);
        float f8 = 0.0F;
        float f5 = 0.0F;
        if (!shouldSit && entityIn.isAlive()) {
            f8 = entityIn.walkAnimation.speed(partialTicks);
            f5 = entityIn.walkAnimation.position(partialTicks);
            if (entityIn.isBaby()) {
                f5 *= 3.0F;
            }

            if (f8 > 1.0F) {
                f8 = 1.0F;
            }
        }
        if (entityIn.isDwarf()) {
            this.model = DWARF_MODEL;
        } else {
            this.model = NORMAL_MODEL;
        }
        this.model.prepareMobModel(entityIn, f5, f8, partialTicks);
        this.model.setupAnim(entityIn, f5, f8, f7, f2, f6);
        Minecraft minecraft = Minecraft.getInstance();
        boolean flag = this.isBodyVisible(entityIn);
        boolean flag1 = !flag && !entityIn.isInvisibleTo(minecraft.player);
        boolean flag2 = minecraft.shouldEntityAppearGlowing(entityIn);
        RenderType rendertype = this.getRenderType(entityIn, flag, flag1, flag2);
        if (rendertype != null && !entityIn.isFullyHidden()) {
            float hide = (entityIn.prevHidingProgress + (entityIn.hidingProgress - entityIn.prevHidingProgress) * partialTicks) * 0.1F;
            float alpha = (1F - hide) * 0.6F;
            this.shadowRadius = 0.9F * alpha;
            int i = getOverlayCoords(entityIn, this.getWhiteOverlayProgress(entityIn, partialTicks));
            this.renderUnderminerModel(matrixStackIn, bufferIn, rendertype, partialTicks, packedLightIn, i, flag1 ? 0.15F : Mth.clamp(alpha, 0, 1), entityIn);
        } else {
            this.shadowRadius = 0;
        }
        if (!entityIn.isSpectator()) {
            // Never cast the elements of this.layers — LayerRainbow and other mods' layers live
            // there too. See LivingEntityRenderer#renderAttachedLayers.
            //? if >=1.21.2 {
            /*this.renderAttachedLayers(matrixStackIn, bufferIn, packedLightIn);
            *///?} else {
            for (RenderLayer layerrenderer : this.layers) {
                layerrenderer.render(matrixStackIn, bufferIn, packedLightIn, entityIn, f5, f8, partialTicks, f7, f2, f6);
            }
            //?}
        }

        matrixStackIn.popPose();
        //? if >=1.21.2 {
        /*net.minecraft.network.chat.Component amName = com.github.alexthe666.alexsmobs.client.render.compat.AMRenderEventCompat.nameTagContent(entityIn, entityIn.getDisplayName(), this, this.renderingState, matrixStackIn, bufferIn, packedLightIn, partialTicks, this.shouldShowName(entityIn));
        if (amName != null) {
            this.renderNameTag(entityIn, amName, matrixStackIn, bufferIn, packedLightIn);
        }
        com.github.alexthe666.alexsmobs.client.render.compat.AMRenderEventCompat.firePost(this.renderingState, this, partialTicks, matrixStackIn, bufferIn, packedLightIn);
        *///?} elif fabric && >=1.20.5 {
        /*// Fabric has neither RenderNameTagEvent nor RenderLivingEvent.Post: no mod can suppress or
        // restyle this nameplate and there is no post hook to fire, so vanilla's own shouldShowName is
        // the whole condition. Two arms because 1.20.5 added partialTick to renderNameTag and a
        // Stonecutter block cannot nest inside another.
        if (this.shouldShowName(entityIn)) {
            this.renderNameTag(entityIn, entityIn.getDisplayName(), matrixStackIn, bufferIn, packedLightIn, partialTicks);
        }
        *///?} elif fabric {
        /*if (this.shouldShowName(entityIn)) {
            this.renderNameTag(entityIn, entityIn.getDisplayName(), matrixStackIn, bufferIn, packedLightIn);
        }
        *///?} else {
        RenderNameTagEvent renderNameplateEvent = new RenderNameTagEvent(entityIn, entityIn.getDisplayName(), this, matrixStackIn, bufferIn, packedLightIn, partialTicks);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(renderNameplateEvent);
        // NeoForge 20.6 swapped this event's Event.Result for a TriState.
        //? if neoforge && >=1.20.6 {
        /*if (renderNameplateEvent.canRender().isTrue() || (renderNameplateEvent.canRender().isDefault() && this.shouldShowName(entityIn))) {
        *///?} else {
        if (renderNameplateEvent.getResult() != net.minecraftforge.eventbus.api.Event.Result.DENY && (renderNameplateEvent.getResult() == net.minecraftforge.eventbus.api.Event.Result.ALLOW || this.shouldShowName(entityIn))) {
        //?}
            // 1.20.5 added partialTick to renderNameTag.
            //? if >=1.20.5 {
            /*this.renderNameTag(entityIn, renderNameplateEvent.getContent(), matrixStackIn, bufferIn, packedLightIn, partialTicks);
            *///?} else {
            this.renderNameTag(entityIn, renderNameplateEvent.getContent(), matrixStackIn, bufferIn, packedLightIn);
            //?}
        }
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.RenderLivingEvent.Post<EntityUnderminer, EntityModel<EntityUnderminer>>(entityIn, this, partialTicks, matrixStackIn, bufferIn, packedLightIn));
        //?}

        BlockPos miningPos = entityIn.getMiningPos();
        if (miningPos != null) {
            matrixStackIn.pushPose();
            double d0 = Mth.lerp(partialTicks, entityIn.xo, entityIn.getX());
            double d1 = Mth.lerp(partialTicks, entityIn.yo, entityIn.getY());
            double d2 = Mth.lerp(partialTicks, entityIn.zo, entityIn.getZ());

            matrixStackIn.translate((double) miningPos.getX() - d0, (double) miningPos.getY() - d1, (double) miningPos.getZ() - d2);
            int progress = (int) Math.round((DESTROY_TYPES.size() - 1) * (float) Mth.clamp(entityIn.getMiningProgress(), 0F, 1.0F));
            renderBreaking(matrixStackIn, bufferIn, entityIn, miningPos, progress);
            matrixStackIn.popPose();
        }
    }

    /**
     * 26.1 removed BlockRenderDispatcher (and its renderBreakingTexture); the destroy animation is a
     * submit node now, exactly as LevelRenderer#submitBlockDestroyAnimation does it. Split out of the
     * caller because the pre-26 body carries Stonecutter blocks of its own and blocks never nest.
     */
    private void renderBreaking(PoseStack matrixStackIn, MultiBufferSource bufferIn, EntityUnderminer entityIn, BlockPos miningPos, int progress) {
        // 26.2 pushed the model resolution out of the collector: it now takes the already-collected
        // BlockStateModelParts (and no seed, since the caller does the seeding). This is vanilla's
        // own body in LevelRenderer's block-destroy pass, minus the per-state loop.
        //? if >=26.2 {
        /*net.minecraft.client.renderer.SubmitNodeCollector collector262 =
                com.github.alexthe666.alexsmobs.client.render.compat.AMSubmitBuffers.collectorOf(bufferIn);
        if (collector262 != null) {
            net.minecraft.world.level.block.state.BlockState breaking = entityIn.level().getBlockState(miningPos);
            java.util.List<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart> parts = new java.util.ArrayList<>();
            net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.createThreadLocalInstance();
            random.setSeed(breaking.getSeed(miningPos));
            collectParts262(entityIn.level(), miningPos, breaking, random, parts);
            collector262.submitBreakingBlockModel(matrixStackIn, java.util.List.copyOf(parts), progress);
        }
        *///?} elif >=26 {
        /*net.minecraft.client.renderer.SubmitNodeCollector collector =
                com.github.alexthe666.alexsmobs.client.render.compat.AMSubmitBuffers.collectorOf(bufferIn);
        if (collector != null) {
            net.minecraft.world.level.block.state.BlockState breaking = entityIn.level().getBlockState(miningPos);
            collector.submitBreakingBlockModel(matrixStackIn,
                    Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(breaking),
                    breaking.getSeed(miningPos), progress);
        }
        *///?} else {
        renderBreakingLegacy(matrixStackIn, bufferIn, entityIn, miningPos, progress);
        //?}
    }

    // Vanilla 26.2's BlockStateModel only declares the context-free collectParts(RandomSource, List),
    // and the two loaders added *different* context-aware overloads on top of it: NeoForge extends the
    // interface with collectParts(BlockAndTintGetter, BlockPos, BlockState, RandomSource, List), while
    // Forge's IForgeBlockStateModel takes (RandomSource, List, ModelData). Split out of renderBreaking
    // because blocks never nest.
    //? if forge && >=26.2 {
    /*private static void collectParts262(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state, net.minecraft.util.RandomSource random, java.util.List<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart> out) {
        net.minecraftforge.client.model.data.ModelDataManager manager = level.getModelDataManager();
        net.minecraftforge.client.model.data.ModelData data = manager == null ? net.minecraftforge.client.model.data.ModelData.EMPTY : manager.getAtOrEmpty(pos);
        Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state).collectParts(random, out, data);
    }
    *///?} elif fabric && >=26.2 {
    /*private static void collectParts262(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state, net.minecraft.util.RandomSource random, java.util.List<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart> out) {
        // Fabric adds no context-aware overload, so only vanilla's context-free one is available:
        // a model whose parts vary with position renders its default variant in the destroy overlay.
        Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state).collectParts(random, out);
    }
    *///?} elif >=26.2 {
    /*private static void collectParts262(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state, net.minecraft.util.RandomSource random, java.util.List<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart> out) {
        // BlockAndTintGetter moved client-side in 26.2 and Level no longer implements it; ClientLevel
        // does, and this is render code, so the level is always one.
        Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state).collectParts((net.minecraft.client.multiplayer.ClientLevel) level, pos, state, random, out);
    }
    *///?}

    /** Pre-26 destroy animation. Its own Stonecutter blocks live here because blocks never nest. */
    private void renderBreakingLegacy(PoseStack matrixStackIn, MultiBufferSource bufferIn, EntityUnderminer entityIn, BlockPos miningPos, int progress) {
        PoseStack.Pose posestack$pose = matrixStackIn.last();
        // 1.20.5 takes the whole PoseStack.Pose instead of its two matrices.
        //? if >=1.20.5 && <26
        //VertexConsumer vertexconsumer1 = new SheetedDecalTextureGenerator(bufferIn.getBuffer(DESTROY_TYPES.get(progress)), posestack$pose, 1.0F);
        //? if <1.20.5
        VertexConsumer vertexconsumer1 = new SheetedDecalTextureGenerator(bufferIn.getBuffer(DESTROY_TYPES.get(progress)), posestack$pose.pose(), posestack$pose.normal(), 1.0F);

        //? if (neoforge && >=1.21.5 && <26) || (fabric && <26) {
        /*// NeoForge 1.21.5 dropped the ModelData overload of renderBreakingTexture (5-arg form now).
        // The 5-arg form IS vanilla's — the 6-arg ModelData one is the loader extension — so this is
        // also the Fabric arm: ModelData and getModelDataManager are both Forge-family and Fabric
        // has no per-position model data to pass. Block-breaking overlays on modded blocks with
        // connected-texture model data will render from the default model there, which is what
        // vanilla itself does.
        Minecraft.getInstance().getBlockRenderer().renderBreakingTexture(entityIn.level().getBlockState(miningPos), miningPos, entityIn.level(), matrixStackIn, vertexconsumer1);
        *///?} elif <26 {
        net.minecraftforge.client.model.data.ModelData modelData = entityIn.level().getModelDataManager().getAt(miningPos);
        Minecraft.getInstance().getBlockRenderer().renderBreakingTexture(entityIn.level().getBlockState(miningPos), miningPos, entityIn.level(), matrixStackIn, vertexconsumer1, modelData == null ? net.minecraftforge.client.model.data.ModelData.EMPTY : modelData);
        //?}
    }

    private void renderUnderminerModel(PoseStack matrixStackIn, MultiBufferSource source, RenderType defRenderType, float partialTicks, int packedLightIn, int overlayColors, float alphaIn, EntityUnderminer entityIn) {
        boolean hurt = Math.max(entityIn.hurtTime, entityIn.deathTime) > 0;
        com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.renderToBuffer(this.model, matrixStackIn, source.getBuffer(defRenderType), packedLightIn, LivingEntityRenderer.getOverlayCoords(entityIn, 0.0F), hurt ? 0.4F : 1.0F, hurt ? 0.8F : 1.0F, hurt ? 0.7F : 1.0F, alphaIn);
    }


    @Nullable
    protected RenderType getRenderType(EntityUnderminer farseer, boolean normal, boolean invis, boolean outline) {
        ResourceLocation resourcelocation = this.getTextureLocation(farseer);
        return outline ? RenderType.outline(resourcelocation) : AMRenderTypes.getUnderminer(resourcelocation);
    }

    public ResourceLocation getTextureLocation(EntityUnderminer entity) {
        return entity.isDwarf() ? TEXTURE_DWARF : entity.getVariant() == 0 ? TEXTURE_0 : TEXTURE_1;
    }

}

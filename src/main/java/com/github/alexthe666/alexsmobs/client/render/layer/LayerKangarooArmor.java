package com.github.alexthe666.alexsmobs.client.render.layer;

import com.github.alexthe666.alexsmobs.misc.AMCompat;

import com.github.alexthe666.alexsmobs.client.model.ModelKangaroo;
import com.github.alexthe666.alexsmobs.client.render.RenderKangaroo;
import com.github.alexthe666.alexsmobs.entity.EntityKangaroo;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
//? if <1.21.5 {
import net.minecraft.world.item.ArmorItem;
//?}
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;

import java.util.Map;

public class LayerKangarooArmor extends RenderLayer<EntityKangaroo, ModelKangaroo> {

    private static final Map<String, ResourceLocation> ARMOR_TEXTURE_RES_MAP = Maps.newHashMap();
    private final HumanoidModel defaultBipedModel;
    private final RenderKangaroo renderer;
    //? if >=1.21.2 {
    /*// 1.21.2 replaced the per-material texture-layer lookup with the equipment-asset system: an
    // EquipmentLayerRenderer resolves textures (and reads the stack's dye component itself) and
    // draws the armour model.
    //
    // Upstream does not dress the kangaroo in a plain biped chestplate: it lays the body part flat
    // along the torso and moves both arms onto the kangaroo's own arm pivots, then draws twice —
    // the arms alone, then the body alone stretched 1.1 x 1.65 x 1.1. Keeping that here has to
    // satisfy two different pipelines. Below 1.21.9 renderLayers calls Model#renderToBuffer
    // straight away, so posing the model just before the call is enough. From 1.21.9 the call only
    // SUBMITS the (model, state) pair and ModelFeatureRenderer re-runs setupAnim(state) at flush,
    // which overwrites any pose written onto the model beforehand; the parts' visible flags are
    // read then too. So both halves are made flush-safe: the pose travels in the render state and
    // is applied by an overridden setupAnim, and each pass owns its own model instance so its
    // visible flags are set once and cannot be clobbered by the next pass. The state is allocated
    // per call rather than shared because a deferred flush would otherwise hand every kangaroo on
    // screen the last one's arms.
    private final net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer equipmentRenderer;
    private final KangarooArmorModel headArmorModel;
    private final KangarooArmorModel armsArmorModel;
    private final KangarooArmorModel bodyArmorModel;
    *///?}

    public LayerKangarooArmor(RenderKangaroo render, EntityRendererProvider.Context context) {
        super(render);
        defaultBipedModel = new HumanoidModel(context.bakeLayer(com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.armorStandArmorLayer(EquipmentSlot.CHEST)));
        this.renderer = render;
        //? if >=1.21.2 {
        /*this.equipmentRenderer = context.getEquipmentRenderer();
        // Three bakes, not three references to one: visible lives on ModelPart, so the passes have
        // to own separate part trees for their flags to mean anything at flush time. Each bake also
        // asks for ITS OWN slot's layer — from 1.21.9 a slot's armour mesh carries only that slot's
        // parts, so a head baked from the chest mesh is geometry-less (see armorStandArmorLayer).
        this.headArmorModel = new KangarooArmorModel(context.bakeLayer(com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.armorStandArmorLayer(EquipmentSlot.HEAD)), false);
        this.armsArmorModel = new KangarooArmorModel(context.bakeLayer(com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.armorStandArmorLayer(EquipmentSlot.CHEST)), true);
        this.bodyArmorModel = new KangarooArmorModel(context.bakeLayer(com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.armorStandArmorLayer(EquipmentSlot.CHEST)), true);
        this.setModelSlotVisible(this.headArmorModel, EquipmentSlot.HEAD);
        this.setModelSlotVisible(this.armsArmorModel, EquipmentSlot.CHEST);
        this.armsArmorModel.body.visible = false;
        this.setModelSlotVisible(this.bodyArmorModel, EquipmentSlot.CHEST);
        this.bodyArmorModel.rightArm.visible = false;
        this.bodyArmorModel.leftArm.visible = false;
        *///?}
    }

    // 1.20.5 moved armour textures onto the material's layer list: index 0 is the (dyeable)
    // base and, where the material has one, index 1 is the white overlay this used to build by
    // appending "_overlay" to the texture path. The two loaders' hook takes the same arguments
    // in a different order, so each gets its own copy.
    //? if forge && >=1.20.5 && <1.21.2 {
    /*public static ResourceLocation getArmorResource(net.minecraft.world.entity.Entity entity, ItemStack stack, EquipmentSlot slot, @javax.annotation.Nullable String type) {
        java.util.List<net.minecraft.world.item.ArmorMaterial.Layer> layers = ((ArmorItem) stack.getItem()).getMaterial().value().layers();
        net.minecraft.world.item.ArmorMaterial.Layer layer = layers.get(type == null || layers.size() < 2 ? 0 : 1);
        return net.minecraftforge.client.ForgeHooksClient.getArmorTexture(entity, stack, slot, layer, false);
    }
    *///?}

    //? if neoforge && >=1.20.5 && <1.21.2 {
    /*public static ResourceLocation getArmorResource(net.minecraft.world.entity.Entity entity, ItemStack stack, EquipmentSlot slot, @javax.annotation.Nullable String type) {
        java.util.List<net.minecraft.world.item.ArmorMaterial.Layer> layers = ((ArmorItem) stack.getItem()).getMaterial().value().layers();
        net.minecraft.world.item.ArmorMaterial.Layer layer = layers.get(type == null || layers.size() < 2 ? 0 : 1);
        return net.neoforged.neoforge.client.ClientHooks.getArmorTexture(entity, stack, layer, false, slot);
    }
    *///?}

    //? if fabric && >=1.20.5 && <1.21.2 {
    /*// Fabric has no getArmorTexture hook at all, so this is the loaders' hook minus its one job:
    // letting a third mod re-point another mod's armour texture. The layer already carries the
    // resolved path, and texture(false) is the outer (non-leggings) model — the same `false` the two
    // arms above pass. Consequence on Fabric: an armour-texture-replacing mod has no seam here.
    public static ResourceLocation getArmorResource(net.minecraft.world.entity.Entity entity, ItemStack stack, EquipmentSlot slot, @javax.annotation.Nullable String type) {
        java.util.List<net.minecraft.world.item.ArmorMaterial.Layer> layers = ((ArmorItem) stack.getItem()).getMaterial().value().layers();
        net.minecraft.world.item.ArmorMaterial.Layer layer = layers.get(type == null || layers.size() < 2 ? 0 : 1);
        return layer.texture(false);
    }
    *///?}

    //? if <1.20.5 && !fabric {
    public static ResourceLocation getArmorResource(net.minecraft.world.entity.Entity entity, ItemStack stack, EquipmentSlot slot, @javax.annotation.Nullable String type) {
        ArmorItem item = (ArmorItem) stack.getItem();
        String texture = item.getMaterial().getName();
        String domain = "minecraft";
        int idx = texture.indexOf(':');
        if (idx != -1) {
            domain = texture.substring(0, idx);
            texture = texture.substring(idx + 1);
        }
        String s1 = String.format("%s:textures/models/armor/%s_layer_%d%s.png", domain, texture, (1), type == null ? "" : String.format("_%s", type));

        s1 = net.minecraftforge.client.ForgeHooksClient.getArmorTexture(entity, stack, s1, slot, type);
        ResourceLocation resourcelocation = ARMOR_TEXTURE_RES_MAP.get(s1);

        if (resourcelocation == null) {
            resourcelocation = AMCompat.rl(s1);
            ARMOR_TEXTURE_RES_MAP.put(s1, resourcelocation);
        }

        return resourcelocation;
    }
    //?}

    //? if fabric && <1.20.5 {
    /*
    public static ResourceLocation getArmorResource(net.minecraft.world.entity.Entity entity, ItemStack stack, EquipmentSlot slot, @javax.annotation.Nullable String type) {
        ArmorItem item = (ArmorItem) stack.getItem();
        String texture = item.getMaterial().getName();
        String domain = "minecraft";
        int idx = texture.indexOf(':');
        if (idx != -1) {
            domain = texture.substring(0, idx);
            texture = texture.substring(idx + 1);
        }
        String s1 = String.format("%s:textures/models/armor/%s_layer_%d%s.png", domain, texture, (1), type == null ? "" : String.format("_%s", type));

        // The Forge arm above runs the built path through ForgeHooksClient.getArmorTexture so another
        // mod can redirect it. Fabric has no such hook, so the vanilla-convention path is used as built.
        ResourceLocation resourcelocation = ARMOR_TEXTURE_RES_MAP.get(s1);

        if (resourcelocation == null) {
            resourcelocation = AMCompat.rl(s1);
            ARMOR_TEXTURE_RES_MAP.put(s1, resourcelocation);
        }

        return resourcelocation;
    }
    *///?}

    public void render(PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn, EntityKangaroo roo, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        matrixStackIn.pushPose();
        if(roo.isRoger()){
            ItemStack haloStack = new ItemStack(AMItemRegistry.HALO.get());
            matrixStackIn.pushPose();
            translateToHead(matrixStackIn);
            float f = 0.1F * (float) Math.sin((roo.tickCount + partialTicks) * 0.1F) + (roo.isBaby() ? 0.2F : 0F);
            matrixStackIn.translate(0.0F, -0.75F - f, -0.2F);
            matrixStackIn.mulPose(Axis.XP.rotationDegrees(90F));
            matrixStackIn.scale(1.3F, 1.3F, 1.3F);
            ItemInHandRenderer renderer = Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer();
            com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.renderItemInHand(renderer, roo, haloStack, ItemDisplayContext.GROUND, false, matrixStackIn, bufferIn, packedLightIn);
            matrixStackIn.popPose();
        }
        if(!roo.isBaby()) {
            //? if >=1.21.2 {
            /*renderArmorPiece(matrixStackIn, bufferIn, packedLightIn, roo, roo.getItemBySlot(EquipmentSlot.HEAD), EquipmentSlot.HEAD, partialTicks);
            renderArmorPiece(matrixStackIn, bufferIn, packedLightIn, roo, roo.getItemBySlot(EquipmentSlot.CHEST), EquipmentSlot.CHEST, partialTicks);
            *///?} else {
            {
                matrixStackIn.pushPose();
                ItemStack itemstack = roo.getItemBySlot(EquipmentSlot.HEAD);
                if (itemstack.getItem() instanceof ArmorItem) {
                    ArmorItem armoritem = (ArmorItem) itemstack.getItem();
                    // ItemStack#canEquip is a Forge IItemStackExtension patch. The CHEST branch below
                    // already asks the loader-neutral question through AMCompat, so ask it the same way
                    // here — for a vanilla ArmorItem the two answers are identical.
                    if (com.github.alexthe666.alexsmobs.misc.AMCompat.equipmentSlotFor(itemstack) == EquipmentSlot.HEAD) {
                        HumanoidModel a = defaultBipedModel;
                        a = getArmorModelHook(roo, itemstack, EquipmentSlot.HEAD, a);
                        final boolean notAVanillaModel = a != defaultBipedModel;
                        this.setModelSlotVisible(a, EquipmentSlot.HEAD);
                        translateToHead(matrixStackIn);
                        matrixStackIn.translate(0, 0.015F, -0.05F);
                        if(itemstack.getItem() == AMItemRegistry.FEDORA.get()){
                            matrixStackIn.translate(0, 0.05F, 0F);

                        }
                        matrixStackIn.scale(0.7F, 0.7F, 0.7F);
                        final boolean flag1 = itemstack.hasFoil();
                        int clampedLight = packedLightIn;
                        if (com.github.alexthe666.alexsmobs.misc.AMCompat.hasCustomColor(itemstack)) { // Allow this for anything, not only cloth
                            final int i = com.github.alexthe666.alexsmobs.misc.AMCompat.getDyedColor(itemstack, 0XFFFFFF);
                            final float f = (float) (i >> 16 & 255) / 255.0F;
                            final float f1 = (float) (i >> 8 & 255) / 255.0F;
                            final float f2 = (float) (i & 255) / 255.0F;
                            renderHelmet(roo, matrixStackIn, bufferIn, clampedLight, flag1, a, f, f1, f2, getArmorResource(roo, itemstack, EquipmentSlot.HEAD, null), notAVanillaModel);
                            renderHelmet(roo, matrixStackIn, bufferIn, clampedLight, flag1, a, 1.0F, 1.0F, 1.0F, getArmorResource(roo, itemstack, EquipmentSlot.HEAD, "overlay"), notAVanillaModel);
                        } else {
                            renderHelmet(roo, matrixStackIn, bufferIn, clampedLight, flag1, a, 1.0F, 1.0F, 1.0F, getArmorResource(roo, itemstack, EquipmentSlot.HEAD, null), notAVanillaModel);
                        }
                    }
                }else{
                    translateToHead(matrixStackIn);
                    matrixStackIn.translate(0, -0.2, -0.1F);
                    matrixStackIn.mulPose((new Quaternionf()).rotateX(Mth.PI));
                    matrixStackIn.mulPose((new Quaternionf()).rotateY(Mth.PI));
                    matrixStackIn.scale(1.0F, 1.0F, 1.0F);
                    com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.renderItemStatic(itemstack, ItemDisplayContext.FIXED, packedLightIn, OverlayTexture.NO_OVERLAY, matrixStackIn, bufferIn, roo.level(), 0);
                }
                matrixStackIn.popPose();
            }
            {
                matrixStackIn.pushPose();
                ItemStack itemstack = roo.getItemBySlot(EquipmentSlot.CHEST);
                if (itemstack.getItem() instanceof ArmorItem) {
                    ArmorItem armoritem = (ArmorItem) itemstack.getItem();
                    if (com.github.alexthe666.alexsmobs.misc.AMCompat.equipmentSlotFor(itemstack) == EquipmentSlot.CHEST) {
                        HumanoidModel a = defaultBipedModel;
                        a = getArmorModelHook(roo, itemstack, EquipmentSlot.CHEST, a);
                        boolean notAVanillaModel = a != defaultBipedModel;
                        this.setModelSlotVisible(a, EquipmentSlot.CHEST);
                        translateToChest(matrixStackIn);
                        matrixStackIn.translate(0, 0.25F, 0F);
                        matrixStackIn.scale(1F, 1F, 1F);
                        boolean flag1 = itemstack.hasFoil();
                        int clampedLight = packedLightIn;
                        if (com.github.alexthe666.alexsmobs.misc.AMCompat.hasCustomColor(itemstack)) { // Allow this for anything, not only cloth
                            int i = com.github.alexthe666.alexsmobs.misc.AMCompat.getDyedColor(itemstack, 0XFFFFFF);
                            float f = (float) (i >> 16 & 255) / 255.0F;
                            float f1 = (float) (i >> 8 & 255) / 255.0F;
                            float f2 = (float) (i & 255) / 255.0F;
                            renderChestplate(roo, matrixStackIn, bufferIn, clampedLight, flag1, a, f, f1, f2, getArmorResource(roo, itemstack, EquipmentSlot.CHEST, null), notAVanillaModel);
                            renderChestplate(roo, matrixStackIn, bufferIn, clampedLight, flag1, a, 1.0F, 1.0F, 1.0F, getArmorResource(roo, itemstack, EquipmentSlot.CHEST, "overlay"), notAVanillaModel);
                        } else {
                            renderChestplate(roo, matrixStackIn, bufferIn, clampedLight, flag1, a, 1.0F, 1.0F, 1.0F, getArmorResource(roo, itemstack, EquipmentSlot.CHEST, null), notAVanillaModel);
                        }

                    }
                }
                matrixStackIn.popPose();
            }
            //?}
        }
        matrixStackIn.popPose();

    }

    //? if >=1.21.2 {
    /*private void renderArmorPiece(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, EntityKangaroo roo, ItemStack stack, EquipmentSlot slot, float partialTicks) {
        net.minecraft.world.item.equipment.Equippable equippable = stack.get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
        if (equippable == null || equippable.slot() != slot || equippable.model().isEmpty()) {
            return;
        }
        net.minecraft.resources.ResourceLocation assetId = equippable.model().get();
        KangarooArmorState armorState = new KangarooArmorState();
        armorState.capture(this.renderer.getModel(), roo.prevSitProgress + (roo.sitProgress - roo.prevSitProgress) * partialTicks);
        if (slot == EquipmentSlot.HEAD) {
            KangarooArmorModel armorModel = this.headArmorModel;
            poseStack.pushPose();
            translateToHead(poseStack);
            poseStack.translate(0, 0.015F, -0.05F);
            if (stack.getItem() == AMItemRegistry.FEDORA.get()) {
                poseStack.translate(0, 0.05F, 0F);
            }
            poseStack.scale(0.7F, 0.7F, 0.7F);
            armorModel.setupAnim(armorState);
            this.equipmentRenderer.renderLayers(net.minecraft.world.item.equipment.EquipmentModel.LayerType.HUMANOID, assetId, armorModel, stack, poseStack, bufferSource, packedLight);
            poseStack.popPose();
        } else {
            // Upstream's two passes. Both sit under the same chest transform and only the second is
            // stretched — that stretch is what widens the plate over the kangaroo's barrel torso,
            // and applying it to the arms as well is what made them splay out.
            KangarooArmorModel armorModel = this.armsArmorModel;
            poseStack.pushPose();
            translateToChest(poseStack);
            poseStack.translate(0, 0.25F, 0F);
            armorModel.setupAnim(armorState);
            this.equipmentRenderer.renderLayers(net.minecraft.world.item.equipment.EquipmentModel.LayerType.HUMANOID, assetId, armorModel, stack, poseStack, bufferSource, packedLight);
            poseStack.popPose();
            armorModel = this.bodyArmorModel;
            poseStack.pushPose();
            translateToChest(poseStack);
            poseStack.translate(0, 0.25F, 0F);
            poseStack.scale(1.1F, 1.65F, 1.1F);
            armorModel.setupAnim(armorState);
            this.equipmentRenderer.renderLayers(net.minecraft.world.item.equipment.EquipmentModel.LayerType.HUMANOID, assetId, armorModel, stack, poseStack, bufferSource, packedLight);
            poseStack.popPose();
        }
    }

    // The pose upstream copies off the kangaroo, carried per render call so that a deferred flush
    // cannot hand one kangaroo's arms to another. Extending HumanoidRenderState is what lets it be
    // the state argument renderLayers passes back into setupAnim from 1.21.9 on.
    private static class KangarooArmorState extends net.minecraft.client.renderer.entity.state.HumanoidRenderState {
        float rightArmX, rightArmY, rightArmZ, rightArmXRot, rightArmYRot, rightArmZRot;
        float leftArmX, leftArmY, leftArmZ, leftArmXRot, leftArmYRot, leftArmZRot;

        void capture(ModelKangaroo model, float sitProgress) {
            this.rightArmX = model.arm_right.rotationPointX;
            this.rightArmY = model.arm_right.rotationPointY - 4 + (sitProgress * 0.25F);
            this.rightArmZ = model.arm_right.rotationPointZ - 0.5F;
            this.rightArmXRot = model.arm_right.rotateAngleX;
            this.rightArmYRot = model.arm_right.rotateAngleY;
            this.rightArmZRot = model.arm_right.rotateAngleZ;
            this.leftArmX = model.arm_left.rotationPointX;
            this.leftArmY = model.arm_left.rotationPointY - 4 + (sitProgress * 0.25F);
            this.leftArmZ = model.arm_left.rotationPointZ - 0.5F;
            this.leftArmXRot = model.arm_left.rotateAngleX;
            this.leftArmYRot = model.arm_left.rotateAngleY;
            this.leftArmZRot = model.arm_left.rotateAngleZ;
        }
    }

    // setupAnim is the one hook that runs on both sides of the 1.21.9 split — immediately, when
    // renderArmorPiece calls it, and again at flush from ModelFeatureRenderer. Applying the pose
    // here rather than to the model from outside is what makes it survive that second call.
    private static class KangarooArmorModel extends HumanoidModel<KangarooArmorState> {
        private final boolean poseToKangaroo;

        KangarooArmorModel(net.minecraft.client.model.geom.ModelPart root, boolean poseToKangaroo) {
            super(root);
            this.poseToKangaroo = poseToKangaroo;
        }

        @Override
        public void setupAnim(KangarooArmorState state) {
            super.setupAnim(state);
            if (!this.poseToKangaroo) {
                return;
            }
            this.body.xRot = 90 * 0.017453292F;
            this.body.yRot = 0;
            this.body.zRot = 0;
            this.body.x = 0;
            this.body.y = 0.25F;
            this.body.z = -7.6F;
            this.rightArm.x = state.rightArmX;
            this.rightArm.y = state.rightArmY;
            this.rightArm.z = state.rightArmZ;
            this.rightArm.xRot = state.rightArmXRot;
            this.rightArm.yRot = state.rightArmYRot;
            this.rightArm.zRot = state.rightArmZRot;
            this.leftArm.x = state.leftArmX;
            this.leftArm.y = state.leftArmY;
            this.leftArm.z = state.leftArmZ;
            this.leftArm.xRot = state.leftArmXRot;
            this.leftArm.yRot = state.leftArmYRot;
            this.leftArm.zRot = state.leftArmZRot;
        }
    }
    *///?}

    private void translateToHead(PoseStack matrixStackIn) {
        translateToChest(matrixStackIn);
        this.renderer.getModel().neck.translateAndRotate(matrixStackIn);
        this.renderer.getModel().head.translateAndRotate(matrixStackIn);
    }

    private void translateToChest(PoseStack matrixStackIn) {
        this.renderer.getModel().root.translateAndRotate(matrixStackIn);
        this.renderer.getModel().body.translateAndRotate(matrixStackIn);
        this.renderer.getModel().chest.translateAndRotate(matrixStackIn);
    }


    // These two manual armour draws are superseded by EquipmentLayerRenderer from 1.21.2 on (the
    // renderArmorPiece path above), and they lean on hooks that era removed, so they compile only
    // below it.
    //? if <1.21.2 {
    private void renderChestplate(EntityKangaroo entity, PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn, boolean glintIn, HumanoidModel modelIn, float red, float green, float blue, ResourceLocation armorResource, boolean notAVanillaModel) {
        VertexConsumer ivertexbuilder = ItemRenderer.getFoilBuffer(bufferIn, RenderType.entityCutoutNoCull(armorResource), false, glintIn);
        renderer.getModel().copyPropertiesTo(modelIn);
        float sitProgress = entity.prevSitProgress + (entity.sitProgress - entity.prevSitProgress) * Minecraft.getInstance().getFrameTime();
        modelIn.body.xRot = 90 * 0.017453292F;
        modelIn.body.yRot = 0;
        modelIn.body.zRot = 0;
        modelIn.body.x = 0;
        modelIn.body.y = 0.25F;
        modelIn.body.z = -7.6F;
        modelIn.rightArm.x = renderer.getModel().arm_right.rotationPointX;
        modelIn.rightArm.y = renderer.getModel().arm_right.rotationPointY;
        modelIn.rightArm.z = renderer.getModel().arm_right.rotationPointZ;
        modelIn.rightArm.xRot = renderer.getModel().arm_right.rotateAngleX;
        modelIn.rightArm.yRot = renderer.getModel().arm_right.rotateAngleY;
        modelIn.rightArm.zRot = renderer.getModel().arm_right.rotateAngleZ;
        modelIn.leftArm.x = renderer.getModel().arm_left.rotationPointX;
        modelIn.leftArm.y = renderer.getModel().arm_left.rotationPointY;
        modelIn.leftArm.z = renderer.getModel().arm_left.rotationPointZ;
        modelIn.leftArm.xRot = renderer.getModel().arm_left.rotateAngleX;
        modelIn.leftArm.yRot = renderer.getModel().arm_left.rotateAngleY;
        modelIn.leftArm.zRot = renderer.getModel().arm_left.rotateAngleZ;
        modelIn.leftArm.y = renderer.getModel().arm_left.rotationPointY - 4 + (sitProgress * 0.25F);
        modelIn.rightArm.y = renderer.getModel().arm_right.rotationPointY - 4 + (sitProgress * 0.25F);
        modelIn.leftArm.z = renderer.getModel().arm_left.rotationPointZ - 0.5F;
        modelIn.rightArm.z = renderer.getModel().arm_right.rotationPointZ - 0.5F;
        modelIn.body.visible = false;
        com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.renderToBuffer(modelIn, matrixStackIn, ivertexbuilder, packedLightIn, OverlayTexture.NO_OVERLAY, red, green, blue, 1.0F);
        modelIn.body.visible = true;
        modelIn.rightArm.visible = false;
        modelIn.leftArm.visible = false;
        matrixStackIn.pushPose();
        matrixStackIn.scale(1.1F, 1.65F, 1.1F);
        com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.renderToBuffer(modelIn, matrixStackIn, ivertexbuilder, packedLightIn, OverlayTexture.NO_OVERLAY, red, green, blue, 1.0F);
        matrixStackIn.popPose();
        modelIn.rightArm.visible = true;
        modelIn.leftArm.visible = true;

    }

    private void renderHelmet(EntityKangaroo entity, PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn, boolean glintIn, HumanoidModel modelIn, float red, float green, float blue, ResourceLocation armorResource, boolean notAVanillaModel) {
        VertexConsumer ivertexbuilder = ItemRenderer.getFoilBuffer(bufferIn, RenderType.entityCutoutNoCull(armorResource), false, glintIn);
        renderer.getModel().copyPropertiesTo(modelIn);
        modelIn.head.xRot = 0F;
        modelIn.head.yRot = 0F;
        modelIn.head.zRot = 0F;
        modelIn.hat.xRot = 0F;
        modelIn.hat.yRot = 0F;
        modelIn.hat.zRot = 0F;
        modelIn.head.x = 0F;
        modelIn.head.y = 0F;
        modelIn.head.z = 0F;
        modelIn.hat.x = 0F;
        modelIn.hat.y = 0F;
        modelIn.hat.z = 0F;
        com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.renderToBuffer(modelIn, matrixStackIn, ivertexbuilder, packedLightIn, OverlayTexture.NO_OVERLAY, red, green, blue, 1.0F);

    }
    //?}


    protected void setModelSlotVisible(HumanoidModel p_188359_1_, EquipmentSlot slotIn) {
        this.setModelVisible(p_188359_1_);
        switch (slotIn) {
            case HEAD -> {
                p_188359_1_.head.visible = true;
                p_188359_1_.hat.visible = true;
            }
            case CHEST -> {
                p_188359_1_.body.visible = true;
                p_188359_1_.rightArm.visible = true;
                p_188359_1_.leftArm.visible = true;
            }
            case LEGS -> {
                p_188359_1_.body.visible = true;
                p_188359_1_.rightLeg.visible = true;
                p_188359_1_.leftLeg.visible = true;
            }
            case FEET -> {
                p_188359_1_.rightLeg.visible = true;
                p_188359_1_.leftLeg.visible = true;
            }
        }
    }

    protected void setModelVisible(HumanoidModel model) {
        model.setAllVisible(false);

    }


    // The custom-armour-model hook took the entity through 1.21.1; from 1.21.2 it takes a render
    // state, and the renderArmorPiece path no longer routes through it (EquipmentLayerRenderer owns
    // model selection), so it compiles only below 1.21.2.
    //? if <1.21.2 && !fabric {
    protected HumanoidModel<?> getArmorModelHook(LivingEntity entity, ItemStack itemStack, EquipmentSlot slot, HumanoidModel model) {
         Model basicModel = net.minecraftforge.client.ForgeHooksClient.getArmorModel(entity, itemStack, slot, model);
         return basicModel instanceof HumanoidModel ? (HumanoidModel<?>) basicModel : model;
    }
    //?}

    //? if fabric && <1.21.2 {
    /*// ForgeHooksClient.getArmorModel lets another mod swap in a custom armour model. Fabric's
    // nearest equivalent (ArmorRenderer) replaces the whole render call rather than the model, and
    // nothing in this layer routes through it — so on Fabric a modded armour model always renders
    // as the default biped, exactly as it already does on every loader from 1.21.2 up.
    protected HumanoidModel<?> getArmorModelHook(LivingEntity entity, ItemStack itemStack, EquipmentSlot slot, HumanoidModel model) {
        return model;
    }
    *///?}
}

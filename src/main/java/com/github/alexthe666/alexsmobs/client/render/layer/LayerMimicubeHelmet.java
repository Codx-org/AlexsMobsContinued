package com.github.alexthe666.alexsmobs.client.render.layer;

import com.github.alexthe666.alexsmobs.misc.AMCompat;

import com.github.alexthe666.alexsmobs.client.model.ModelMimicube;
import com.github.alexthe666.alexsmobs.client.render.RenderMimicube;
import com.github.alexthe666.alexsmobs.entity.EntityMimicube;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
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
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public class LayerMimicubeHelmet extends RenderLayer<EntityMimicube, ModelMimicube> {

    private static final Map<String, ResourceLocation> ARMOR_TEXTURE_RES_MAP = Maps.newHashMap();
    private final HumanoidModel defaultBipedModel;
    private final RenderMimicube renderer;
    //? if >=1.21.2 {
    /*// 1.21.2 replaced the per-material texture-layer lookup with the equipment-asset system: an
    // EquipmentLayerRenderer resolves textures (and reads the stack's dye component itself) and
    // draws the armour model. See LayerKangarooArmor for the full rationale — the mob's own render
    // state can't pose a HumanoidModel, so a neutral one is used and placement stays on the PoseStack.
    private final net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer equipmentRenderer;
    private final net.minecraft.client.renderer.entity.state.HumanoidRenderState neutralArmorState = new net.minecraft.client.renderer.entity.state.HumanoidRenderState();
    *///?}

    public LayerMimicubeHelmet(RenderMimicube render, EntityRendererProvider.Context renderManagerIn) {
        super(render);
        this.renderer = render;
        // HEAD, not a generic humanoid layer: this model only ever draws the stolen helmet, and from
        // 1.21.9 each slot's armour mesh carries only that slot's parts (see
        // AMRenderCompat#armorStandArmorLayer).
        defaultBipedModel = new HumanoidModel(renderManagerIn.bakeLayer(com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.armorStandArmorLayer(EquipmentSlot.HEAD)));
        //? if >=1.21.2 {
        /*this.equipmentRenderer = renderManagerIn.getEquipmentRenderer();
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

    public void render(PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn, EntityMimicube cube, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        matrixStackIn.pushPose();
        ItemStack itemstack = cube.getItemBySlot(EquipmentSlot.HEAD);
        float helmetSwap = Mth.lerp(partialTicks, cube.prevHelmetSwapProgress, cube.helmetSwapProgress) * 0.2F;
        if (com.github.alexthe666.alexsmobs.misc.AMCompat.isArmor(itemstack)) {
            //? if >=1.21.2 {
            /*renderHelmetPiece(matrixStackIn, bufferIn, packedLightIn, cube, itemstack, helmetSwap);
            *///?} else {
            ArmorItem armoritem = (ArmorItem) itemstack.getItem();
            if (com.github.alexthe666.alexsmobs.misc.AMCompat.equipmentSlotFor(itemstack) == EquipmentSlot.HEAD) {
                HumanoidModel a = defaultBipedModel;
                a = getArmorModelHook(cube, itemstack, EquipmentSlot.HEAD, a);
                boolean notAVanillaModel = a != defaultBipedModel;

                this.setModelSlotVisible(a, EquipmentSlot.HEAD);
                boolean flag = false;
                this.renderer.getModel().root.translateAndRotate(matrixStackIn);
                this.renderer.getModel().innerbody.translateAndRotate(matrixStackIn);
                matrixStackIn.translate(0,  notAVanillaModel ? 0.25F : -0.75F, 0F);
                matrixStackIn.scale(1F + 0.3F * (1 - helmetSwap), 1F + 0.3F * (1 - helmetSwap), 1F + 0.3F * (1 - helmetSwap));
                boolean flag1 = itemstack.hasFoil();
                int clampedLight = helmetSwap > 0 ? (int) (-100 * helmetSwap) : packedLightIn;
                matrixStackIn.mulPose(Axis.YP.rotationDegrees(360 * helmetSwap));
                if (com.github.alexthe666.alexsmobs.misc.AMCompat.hasCustomColor(itemstack)) { // Allow this for anything, not only cloth
                    int i = com.github.alexthe666.alexsmobs.misc.AMCompat.getDyedColor(itemstack, 0XFFFFFF);
                    float f = (float) (i >> 16 & 255) / 255.0F;
                    float f1 = (float) (i >> 8 & 255) / 255.0F;
                    float f2 = (float) (i & 255) / 255.0F;
                    renderArmor(cube, matrixStackIn, bufferIn, clampedLight, flag1, a, f, f1, f2, getArmorResource(cube, itemstack, EquipmentSlot.HEAD, null), notAVanillaModel);
                    renderArmor(cube, matrixStackIn, bufferIn, clampedLight, flag1, a, 1.0F, 1.0F, 1.0F, getArmorResource(cube, itemstack, EquipmentSlot.HEAD, "overlay"), notAVanillaModel);
                } else {
                    renderArmor(cube, matrixStackIn, bufferIn, clampedLight, flag1, a, 1.0F, 1.0F, 1.0F, getArmorResource(cube, itemstack, EquipmentSlot.HEAD, null), notAVanillaModel);
                }

            }
            //?}
        }
        matrixStackIn.popPose();
    }

    //? if >=1.21.2 {
    /*private void renderHelmetPiece(PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn, EntityMimicube cube, ItemStack itemstack, float helmetSwap) {
        if (com.github.alexthe666.alexsmobs.misc.AMCompat.equipmentSlotFor(itemstack) != EquipmentSlot.HEAD) {
            return;
        }
        net.minecraft.world.item.equipment.Equippable equippable = itemstack.get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
        if (equippable == null || equippable.model().isEmpty()) {
            return;
        }
        ResourceLocation assetId = equippable.model().get();
        defaultBipedModel.setupAnim(neutralArmorState);
        this.setModelSlotVisible(defaultBipedModel, EquipmentSlot.HEAD);
        this.renderer.getModel().root.translateAndRotate(matrixStackIn);
        this.renderer.getModel().innerbody.translateAndRotate(matrixStackIn);
        matrixStackIn.translate(0, -0.75F, 0F);
        matrixStackIn.scale(1F + 0.3F * (1 - helmetSwap), 1F + 0.3F * (1 - helmetSwap), 1F + 0.3F * (1 - helmetSwap));
        int clampedLight = helmetSwap > 0 ? (int) (-100 * helmetSwap) : packedLightIn;
        matrixStackIn.mulPose(Axis.YP.rotationDegrees(360 * helmetSwap));
        this.equipmentRenderer.renderLayers(net.minecraft.world.item.equipment.EquipmentModel.LayerType.HUMANOID, assetId, defaultBipedModel, itemstack, matrixStackIn, bufferIn, clampedLight);
    }
    *///?}

    // Superseded by the EquipmentLayerRenderer path (renderHelmetPiece) from 1.21.2 on; leans on the
    // getFoilBuffer/copyPropertiesTo draw that era reworked, so it compiles only below it.
    //? if <1.21.2 {
    private void renderArmor(EntityMimicube entity, PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn, boolean glintIn, HumanoidModel modelIn, float red, float green, float blue, ResourceLocation armorResource, boolean notAVanillaModel) {
        VertexConsumer ivertexbuilder = ItemRenderer.getFoilBuffer(bufferIn, RenderType.entityCutoutNoCull(armorResource), false, glintIn);
        if(notAVanillaModel){
            renderer.getModel().copyPropertiesTo(modelIn);
            modelIn.body.y = 0;
            modelIn.head.setPos(0.0F, 1.0F, 0.0F);
            modelIn.hat.y = 0;
            modelIn.head.xRot = renderer.getModel().body.rotateAngleX;
            modelIn.head.yRot = renderer.getModel().body.rotateAngleY;
            modelIn.head.zRot = renderer.getModel().body.rotateAngleZ;
            modelIn.head.x = renderer.getModel().body.rotationPointX;
            modelIn.head.y = renderer.getModel().body.rotationPointY;
            modelIn.head.z = renderer.getModel().body.rotationPointZ;
            modelIn.hat.copyFrom(modelIn.head);
            modelIn.body.copyFrom(modelIn.head);
        }
        com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.renderToBuffer(modelIn, matrixStackIn, ivertexbuilder, packedLightIn, OverlayTexture.NO_OVERLAY, red, green, blue, 1.0F);
    }
    //?}

    protected void setModelSlotVisible(HumanoidModel p_188359_1_, EquipmentSlot slotIn) {
        this.setModelVisible(p_188359_1_);
        switch (slotIn) {
            case HEAD:
                p_188359_1_.head.visible = true;
                p_188359_1_.hat.visible = true;
                break;
            case CHEST:
                p_188359_1_.body.visible = true;
                p_188359_1_.rightArm.visible = true;
                p_188359_1_.leftArm.visible = true;
                break;
            case LEGS:
                p_188359_1_.body.visible = true;
                p_188359_1_.rightLeg.visible = true;
                p_188359_1_.leftLeg.visible = true;
                break;
            case FEET:
                p_188359_1_.rightLeg.visible = true;
                p_188359_1_.leftLeg.visible = true;
        }
    }

    protected void setModelVisible(HumanoidModel model) {
        model.setAllVisible(false);

    }


    // The custom-armour-model hook took the entity through 1.21.1; from 1.21.2 it takes a render
    // state and the renderHelmetPiece path no longer routes through it, so it compiles only below.
    //? if <1.21.2 && !fabric {
    protected HumanoidModel<?> getArmorModelHook(LivingEntity entity, ItemStack itemStack, EquipmentSlot slot, HumanoidModel model) {
        try{
            Model basicModel = net.minecraftforge.client.ForgeHooksClient.getArmorModel(entity, itemStack, slot, model);
            return basicModel instanceof HumanoidModel ? (HumanoidModel<?>) basicModel : model;
        }catch (Exception e){
            return model;
        }
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

package com.github.alexthe666.alexsmobs.fabric.client;

import com.github.alexthe666.alexsmobs.client.render.item.CustomArmorRenderProperties;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.github.alexthe666.alexsmobs.misc.AMCompat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

//? if >=1.21.9 {
/*import com.github.alexthe666.alexsmobs.client.render.AMRenderCompat;
import com.github.alexthe666.alexsmobs.client.render.compat.AMSubmitBuffers;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
*///?} elif >=1.21.2 {
/*import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
*///?} else {
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
//?}

/**
 * The Fabric stand-in for {@code IClientItemExtensions#getHumanoidArmorModel} — the Forge hook that
 * lets a worn item swap in a custom {@link HumanoidModel}. Fifteen of this mod's armour items go
 * through it; thirteen of them have a hand-built model.
 *
 * <p>On Fabric {@code IClientItemExtensions} is a vendored empty type token, so
 * {@link CustomArmorRenderProperties} compiles but is never consulted. Fabric API's
 * {@code ArmorRenderer} is the loader's own seam for the same job: it is asked to draw one armour
 * piece, and is handed the armour model vanilla would have drawn. That model is exactly the
 * {@code _default} argument the Forge hook receives, so the two hooks take the same lookup —
 * {@code CustomArmorRenderProperties#getHumanoidArmorModel}, called with a {@code null} wearer above
 * 1.21.2 where the mod's {@code withAnimations} calls are already gated out.
 *
 * <p><b>This also fixes the armour textures below 1.21.2.</b> Alex's Mobs keeps its armour skins at
 * {@code textures/armor/<item>.png} rather than the vanilla {@code textures/models/armor/} layout,
 * and redirects vanilla to them through Forge's {@code getArmorTexture} — which Fabric never had, so
 * every one of these items rendered untextured there ({@code ItemModArmor}'s two overrides are
 * {@code !fabric}). A renderer registered here names the texture itself, so the redirect stops being
 * needed at all. From 1.21.2 vanilla resolves the same PNG through the generated equipment model
 * ({@code DataPackMigration#migrateEquipmentTo12102}), so on those nodes this only adds the models.
 *
 * <p>Registered from {@code ClientProxy#clientInit}'s Fabric arm, next to
 * {@link FabricClientEvents#register()}.
 */
public final class FabricArmorRenderers {

    // One shared instance: getHumanoidArmorModel is a pure item-to-model lookup with no per-item
    // state, and it bakes the thirteen models on first use.
    private static final CustomArmorRenderProperties MODELS = new CustomArmorRenderProperties();

    private FabricArmorRenderers() {
    }

    /**
     * The twelve items with a hand-built model, plus — below 1.21.2 only — the three without one.
     *
     * <p>{@code DataPackMigration#armorEquipment} carries all fifteen (item, texture) pairs for the
     * {@code >=1.21.2} equipment models; this table is deliberately the shorter one rather than a
     * shared fifteen, because <b>the three model-less items must not be registered from 1.21.2 up</b>.
     * Registering an {@code ArmorRenderer} <i>replaces</i> vanilla's own drawing of that piece, and
     * from 1.21.2 vanilla already draws them correctly from the generated equipment model — texture
     * and all. Below 1.21.2 it cannot (Alex's Mobs keeps its skins outside the vanilla armour folder
     * and reaches them through a Forge-only texture redirect), so there they are registered and drawn
     * against a plain vanilla armour model baked here.
     *
     * <p>Registering them above 1.21.2 is what caused report #32: see {@link Renderer}.
     */
    public static void register() {
        register(AMItemRegistry.ROADDRUNNER_BOOTS, "roadrunner_boots");
        register(AMItemRegistry.MOOSE_HEADGEAR, "moose_headgear");
        register(AMItemRegistry.FRONTIER_CAP, "frontier_cap");
        register(AMItemRegistry.SOMBRERO, "sombrero");
        register(AMItemRegistry.SPIKED_TURTLE_SHELL, "spiked_turtle_shell");
        register(AMItemRegistry.FEDORA, "fedora");
        register(AMItemRegistry.TARANTULA_HAWK_ELYTRA, "tarantula_hawk_elytra");
        register(AMItemRegistry.FROSTSTALKER_HELMET, "froststalker_helmet");
        register(AMItemRegistry.ROCKY_CHESTPLATE, "rocky_chestplate");
        register(AMItemRegistry.FLYING_FISH_BOOTS, "flying_fish_boots");
        register(AMItemRegistry.NOVELTY_HAT, "novelty_hat");
        register(AMItemRegistry.UNSETTLING_KIMONO, "unsettling_kimono");
        //? if <1.21.2 {
        register(AMItemRegistry.CROCODILE_CHESTPLATE, "crocodile_chestplate");
        register(AMItemRegistry.CENTIPEDE_LEGGINGS, "centipede_leggings");
        register(AMItemRegistry.EMU_LEGGINGS, "emu_leggings");
        //?}
    }

    private static void register(Supplier<Item> item, String texture) {
        ArmorRenderer.register(new Renderer(AMCompat.rl("alexsmobs:textures/armor/" + texture + ".png")), item.get());
    }

    //? if <1.21.2 {
    // The plain vanilla armour shape, for the three items that have no model of their own. Vanilla
    // splits it in two: the leggings layer is the slimmer "inner" bake, everything else the "outer"
    // one (HumanoidArmorLayer#usesInnerModel is exactly `slot == LEGS`). Baked lazily because the
    // model set does not exist until the resource reload that builds it.
    private static HumanoidModel<LivingEntity> innerArmor;
    private static HumanoidModel<LivingEntity> outerArmor;

    private static HumanoidModel<LivingEntity> vanillaArmorModel(EquipmentSlot slot) {
        if (innerArmor == null) {
            innerArmor = new HumanoidModel<>(net.minecraft.client.Minecraft.getInstance().getEntityModels()
                    .bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER_INNER_ARMOR));
            outerArmor = new HumanoidModel<>(net.minecraft.client.Minecraft.getInstance().getEntityModels()
                    .bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER_OUTER_ARMOR));
        }
        return slot == EquipmentSlot.LEGS ? innerArmor : outerArmor;
    }
    //?}

    /**
     * Vanilla's {@code HumanoidArmorLayer#setPartVisibility}, which is private on every version.
     *
     * <p>{@code HumanoidModel#setAllVisible(false)} would do the first half, but 26.1 deleted it —
     * the seven parts are {@code public final} fields on all seventeen Fabric nodes, so setting them
     * by hand is the one spelling that needs no arm. Every custom model adds its cubes as children
     * of one of the seven (the elytra's wings hang off {@code body}, the boots' feathers off the
     * legs), so hiding a part hides what a model attached to it.
     */
    private static void setPartVisibility(HumanoidModel<?> model, EquipmentSlot slot) {
        model.head.visible = false;
        model.hat.visible = false;
        model.body.visible = false;
        model.rightArm.visible = false;
        model.leftArm.visible = false;
        model.rightLeg.visible = false;
        model.leftLeg.visible = false;
        switch (slot) {
            case HEAD -> {
                model.head.visible = true;
                model.hat.visible = true;
            }
            case CHEST -> {
                model.body.visible = true;
                model.rightArm.visible = true;
                model.leftArm.visible = true;
            }
            case LEGS -> {
                model.body.visible = true;
                model.rightLeg.visible = true;
                model.leftLeg.visible = true;
            }
            case FEET -> {
                model.rightLeg.visible = true;
                model.leftLeg.visible = true;
            }
            default -> {
            }
        }
    }

    /**
     * One instance per registered item, holding that item's texture. The three {@code render} arms
     * are the three shapes Fabric API's interface has had:
     *
     * <ul>
     *   <li>{@code <1.21.2} — the wearer is a live {@code LivingEntity}.</li>
     *   <li>{@code 1.21.2 – 1.21.8} — the render-state rewrite swapped it for a
     *       {@code HumanoidRenderState}; everything else is unchanged.</li>
     *   <li>{@code >=1.21.9} — the {@code MultiBufferSource} became a {@code SubmitNodeCollector},
     *       and {@code ArmorRenderer.renderPart} was <b>deleted</b> with it. Its replacement,
     *       {@code ArmorRenderer.submitTransformCopyingModel}, only arrived in 1.21.10, so this arm
     *       cannot use either and draws through {@code AMSubmitBuffers} — the same recorder the
     *       ~130 legacy render bodies in this tree already go through. That also side-steps a real
     *       hazard of {@code SubmitNodeCollector#submitModel}: it renders the model object later in
     *       the frame, and these thirteen models are shared statics whose part poses and visibility
     *       are rewritten per wearer.</li>
     * </ul>
     *
     * <p>{@code copyPropertiesTo} is what carries the wearer's pose onto the custom model below
     * 1.21.9; 26.1 deleted it, and from 1.21.2 {@code setupAnim(state)} recomputes the same pose
     * from the render state, so the top arm uses that instead.
     *
     * <p><b>{@code contextModel} is the wearer's own model, never an armour model.</b> Fabric API
     * documents it as {@code RenderLayer#getParentModel()} on every version of this interface — i.e.
     * the {@code PlayerModel} being drawn — and hands it over so a renderer can copy the pose off it.
     * That is the opposite of Forge's {@code _default}, which really is a spare armour model, so the
     * shared lookup returning {@code _default} for an item with no custom model returned <i>the
     * player</i> here, and {@link #setPartVisibility} then switched the player's head, hat and limbs
     * off. Below 1.21.9 nothing showed, because armour layers draw after the body and
     * {@code PlayerRenderer} calls {@code setAllVisible(true)} again next frame; 1.21.9 made the body
     * a deferred submission drawn <i>after</i> its layers, so the flags landed on the frame that was
     * still to be drawn and the player lost their head (report #32). The three items that hit it are
     * no longer registered from 1.21.2 up, and the guard below is the belt to that braces.
     */
    private static final class Renderer implements ArmorRenderer {

        private final ResourceLocation texture;

        private Renderer(ResourceLocation texture) {
            this.texture = texture;
        }

        //? if >=1.21.9 {
        /*@Override
        @SuppressWarnings("unchecked")
        public void render(PoseStack pose, SubmitNodeCollector collector, ItemStack stack, HumanoidRenderState state,
                           EquipmentSlot slot, int light, HumanoidModel<HumanoidRenderState> contextModel) {
            HumanoidModel<HumanoidRenderState> model =
                    (HumanoidModel<HumanoidRenderState>) MODELS.getHumanoidArmorModel(null, stack, slot, contextModel);
            if (model == contextModel) {
                return;
            }
            model.setupAnim(state);
            setPartVisibility(model, slot);
            AMSubmitBuffers buffers = new AMSubmitBuffers(collector);
            VertexConsumer consumer = AMRenderCompat.armorFoilBuffer(
                    buffers, RenderType.armorCutoutNoCull(this.texture), stack.hasFoil());
            model.renderToBuffer(pose, consumer, light, OverlayTexture.NO_OVERLAY, -1);
            buffers.flush();
        }
        *///?} elif >=1.21.2 {
        /*@Override
        @SuppressWarnings("unchecked")
        public void render(PoseStack pose, MultiBufferSource buffers, ItemStack stack, HumanoidRenderState state,
                           EquipmentSlot slot, int light, HumanoidModel<HumanoidRenderState> contextModel) {
            HumanoidModel<HumanoidRenderState> model =
                    (HumanoidModel<HumanoidRenderState>) MODELS.getHumanoidArmorModel(null, stack, slot, contextModel);
            if (model == contextModel) {
                return;
            }
            // Below 1.21.9 nothing calls setupAnim on an armour model, so without this the frontier
            // cap's tail and the elytra's wings sit in their default pose (report #60). It runs
            // BEFORE copyPropertiesTo deliberately: setupAnim recomputes the seven standard parts
            // from the state, and the wearer's real pose should win over that -- while the parts
            // these models animate are children (tail off the hat, wings off the body), which
            // copyPropertiesTo does not touch, so the animation survives.
            model.setupAnim(state);
            contextModel.copyPropertiesTo(model);
            setPartVisibility(model, slot);
            ArmorRenderer.renderPart(pose, buffers, light, stack, model, this.texture);
        }
        *///?} else {
        @Override
        @SuppressWarnings("unchecked")
        public void render(PoseStack pose, MultiBufferSource buffers, ItemStack stack, LivingEntity entity,
                           EquipmentSlot slot, int light, HumanoidModel<LivingEntity> contextModel) {
            HumanoidModel<LivingEntity> custom =
                    (HumanoidModel<LivingEntity>) MODELS.getHumanoidArmorModel(entity, stack, slot, contextModel);
            // The three model-less items land here, and only here — see register().
            HumanoidModel<LivingEntity> model = custom == contextModel ? vanillaArmorModel(slot) : custom;
            contextModel.copyPropertiesTo(model);
            setPartVisibility(model, slot);
            ArmorRenderer.renderPart(pose, buffers, light, stack, model, this.texture);
        }
        //?}
    }
}

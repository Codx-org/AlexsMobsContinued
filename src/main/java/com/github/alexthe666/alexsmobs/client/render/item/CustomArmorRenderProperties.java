package com.github.alexthe666.alexsmobs.client.render.item;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.client.model.layered.*;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public class CustomArmorRenderProperties implements IClientItemExtensions {

    private static boolean init;

    public static ModelAMElytra ELYTRA_MODEL;
    public static ModelRoadrunnerBoots ROADRUNNER_BOOTS_MODEL;
    public static ModelMooseHeadgear MOOSE_HEADGEAR_MODEL;
    public static ModelFrontierCap FRONTIER_CAP_MODEL;
    public static ModelSpikedTurtleShell SPIKED_TURTLE_SHELL_MODEL;
    public static ModelFedora FEDORA_MODEL;
    public static ModelSombrero SOMBRERO_MODEL;
    public static ModelSombrero SOMBRERO_GOOFY_FASHION_MODEL;
    public static ModelFroststalkerHelmet FROSTSTALKER_HELMET_MODEL;
    public static ModelRockyChestplate ROCKY_CHESTPLATE_MODEL;
    public static ModelFlyingFishBoots FLYING_FISH_BOOTS_MODEL;
    public static ModelNoveltyHat NOVELTY_HAT_MODEL;
    public static ModelUnsettlingKimono UNSETTLING_KIMONO_MODEL;

    public static void initializeModels() {
        init = true;
        ROADRUNNER_BOOTS_MODEL = new ModelRoadrunnerBoots(Minecraft.getInstance().getEntityModels().bakeLayer(AMModelLayers.ROADRUNNER_BOOTS));
        MOOSE_HEADGEAR_MODEL = new ModelMooseHeadgear(Minecraft.getInstance().getEntityModels().bakeLayer(AMModelLayers.MOOSE_HEADGEAR));
        FRONTIER_CAP_MODEL = new ModelFrontierCap(Minecraft.getInstance().getEntityModels().bakeLayer(AMModelLayers.FRONTIER_CAP));
        FEDORA_MODEL = new ModelFedora(Minecraft.getInstance().getEntityModels().bakeLayer(AMModelLayers.FEDORA));
        SPIKED_TURTLE_SHELL_MODEL = new ModelSpikedTurtleShell(Minecraft.getInstance().getEntityModels().bakeLayer(AMModelLayers.SPIKED_TURTLE_SHELL));
        SOMBRERO_MODEL = new ModelSombrero(Minecraft.getInstance().getEntityModels().bakeLayer(AMModelLayers.SOMBRERO));
        SOMBRERO_GOOFY_FASHION_MODEL = new ModelSombrero(Minecraft.getInstance().getEntityModels().bakeLayer(AMModelLayers.SOMBRERO_GOOFY_FASHION));
        FROSTSTALKER_HELMET_MODEL = new ModelFroststalkerHelmet(Minecraft.getInstance().getEntityModels().bakeLayer(AMModelLayers.FROSTSTALKER_HELMET));
        ELYTRA_MODEL = new ModelAMElytra(Minecraft.getInstance().getEntityModels().bakeLayer(AMModelLayers.AM_ELYTRA));
        ROCKY_CHESTPLATE_MODEL = new ModelRockyChestplate(Minecraft.getInstance().getEntityModels().bakeLayer(AMModelLayers.ROCKY_CHESTPLATE));
        FLYING_FISH_BOOTS_MODEL = new ModelFlyingFishBoots(Minecraft.getInstance().getEntityModels().bakeLayer(AMModelLayers.FLYING_FISH_BOOTS));
        NOVELTY_HAT_MODEL = new ModelNoveltyHat(Minecraft.getInstance().getEntityModels().bakeLayer(AMModelLayers.NOVELTY_HAT));
        UNSETTLING_KIMONO_MODEL = new ModelUnsettlingKimono(Minecraft.getInstance().getEntityModels().bakeLayer(AMModelLayers.UNSETTLING_KIMONO));
    }

    // 1.21.2 swapped the wearer argument from the live LivingEntity to its LivingEntityRenderState.
    // The port originally dropped the entity-driven withAnimations() calls here rather than port
    // them, so from 1.21.2 up the custom models rendered in their default pose — the raccoon tail on
    // the frontier cap stopped swaying and the elytra's wings stopped opening (report #60). Both
    // animations only ever needed numbers the render state also carries (walkAnimationPos/Speed, and
    // elytraRotX/Y/Z), so they are back — see ModelFrontierCap / ModelAMElytra. Note the wearer is
    // only reachable on FORGE here: NeoForge's >=1.21.2 hook is handed the stack and the layer type
    // and nothing else, so on that loader the pose can only be applied from the models' own
    // setupAnim(state), which vanilla re-runs at flush from 1.21.9.
    // The flying-fish boots stay neutral above 1.21.2 on purpose: their flap is driven by the
    // wearer's citadel-NBT boost timer, which no render state carries.
    // NeoForge NOTE: EquipmentModel.LayerType is renamed to EquipmentClientInfo.LayerType at 1.21.4 - split the neoforge branch again there.
    // On Fabric IClientItemExtensions is an empty type token (see the vendored class), so there is
    // nothing to override — the method stays as ordinary dead code rather than being gated away, so
    // the 13 armour models are still here to hand to Fabric API's ArmorRenderer later.
    //? if !fabric {
    @Override
    //?}
    //? if forge && >=1.21.2 {
    /*public HumanoidModel<?> getHumanoidArmorModel(net.minecraft.client.renderer.entity.state.LivingEntityRenderState entityLiving, ItemStack itemStack, EquipmentSlot armorSlot, HumanoidModel<?> _default) {
    *///?} elif neoforge && >=1.21.2 {
    /*public net.minecraft.client.model.Model getHumanoidArmorModel(ItemStack itemStack, net.minecraft.world.item.equipment.EquipmentModel.LayerType armorSlot, net.minecraft.client.model.Model _default) {
    *///?} else {
    public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entityLiving, ItemStack itemStack, EquipmentSlot armorSlot, HumanoidModel<?> _default) {
    //?}
        if(!init){
            initializeModels();
        }
        final var item = itemStack.getItem();
        if(item == AMItemRegistry.TARANTULA_HAWK_ELYTRA.get()){
            //? if forge && >=1.21.2 {
            /*return ELYTRA_MODEL.withAnimations(entityLiving);
            *///?} elif >=1.21.2 {
            /*return ELYTRA_MODEL;
            *///?} else {
            return ELYTRA_MODEL.withAnimations(entityLiving);
            //?}
        }
        if(item == AMItemRegistry.ROADDRUNNER_BOOTS.get()){
            return ROADRUNNER_BOOTS_MODEL;
        }
        if(item == AMItemRegistry.MOOSE_HEADGEAR.get()){
            return MOOSE_HEADGEAR_MODEL;
        }
        if(item == AMItemRegistry.FRONTIER_CAP.get()){
            //? if forge && >=1.21.2 {
            /*return FRONTIER_CAP_MODEL.withAnimations(entityLiving);
            *///?} elif >=1.21.2 {
            /*return FRONTIER_CAP_MODEL;
            *///?} else {
            return FRONTIER_CAP_MODEL.withAnimations(entityLiving);
            //?}
        }
        if(item == AMItemRegistry.FEDORA.get()){
            return FEDORA_MODEL;
        }
        if(item == AMItemRegistry.SPIKED_TURTLE_SHELL.get()){
            return SPIKED_TURTLE_SHELL_MODEL;
        }
        if(item == AMItemRegistry.SOMBRERO.get()){
            return AlexsMobs.isAprilFools() ? SOMBRERO_GOOFY_FASHION_MODEL : SOMBRERO_MODEL;
        }
        if(item == AMItemRegistry.FROSTSTALKER_HELMET.get()){
            return FROSTSTALKER_HELMET_MODEL;
        }
        if(item == AMItemRegistry.ROCKY_CHESTPLATE.get()){
            return ROCKY_CHESTPLATE_MODEL;
        }
        if(item == AMItemRegistry.FLYING_FISH_BOOTS.get()){
            //? if >=1.21.2 {
            /*return FLYING_FISH_BOOTS_MODEL;
            *///?} else {
            return FLYING_FISH_BOOTS_MODEL.withAnimations(entityLiving);
            //?}
        }
        if(item == AMItemRegistry.NOVELTY_HAT.get()){
            return NOVELTY_HAT_MODEL;
        }
        if(item == AMItemRegistry.UNSETTLING_KIMONO.get()){
            return UNSETTLING_KIMONO_MODEL;
        }
        return _default;
    }
}

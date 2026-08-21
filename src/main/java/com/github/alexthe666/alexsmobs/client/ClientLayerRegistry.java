package com.github.alexthe666.alexsmobs.client;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.client.render.layer.LayerRainbow;
import com.google.common.collect.ImmutableList;
// ⚠️ LivingEntityRenderer is deliberately NOT imported and is spelled fully qualified below. The
// `!mc2102-render-import-living` rule rewrites that exact import statement to this mod's compat
// class, and the cast at the bottom of this file would then narrow every VANILLA renderer to a
// type it is not — which is precisely why 90 mobs logged "has custom renderer that is not
// LivingEntityRenderer" and lost the rainbow layer on every node from 1.21.2 up.
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.core.registries.BuiltInRegistries;

@OnlyIn(Dist.CLIENT)
public class ClientLayerRegistry {

    @SubscribeEvent
    @OnlyIn(value = Dist.CLIENT)
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        List<EntityType<? extends LivingEntity>> entityTypes = ImmutableList.copyOf(
                BuiltInRegistries.ENTITY_TYPE.stream()
                        .filter(DefaultAttributes::hasSupplier)
                        .map(entityType -> (EntityType<? extends LivingEntity>) entityType)
                        .collect(Collectors.toList()));
        entityTypes.forEach((entityType -> {
            addLayerIfApplicable(entityType, event);
        }));
        // 1.20.2 replaced the player-skin string keys ("default"/"slim") with the
        // PlayerSkin.Model enum.
        // Forge 51 (1.21) also renamed getSkin to getPlayerSkin; NeoForge kept getSkin.
        // 1.21.9 pulled PlayerSkin.Model out into the top-level PlayerModelType enum (skin data is
        // server-visible now) and renamed PlayerRenderer to AvatarRenderer. Forge also renamed the
        // accessors to getModelTypes/getPlayerRenderer; NeoForge kept getSkins and uses
        // getPlayerRenderer too.
        //? if forge && >=1.21.9 {
        /*for (net.minecraft.world.entity.player.PlayerModelType skinType : event.getModelTypes()){
            net.minecraft.client.renderer.entity.player.AvatarRenderer skin = event.getPlayerRenderer(skinType);
            skin.addLayer(new LayerRainbow(skin));
        }
        *///?}
        //? if neoforge && >=1.21.9 {
        /*for (net.minecraft.world.entity.player.PlayerModelType skinType : event.getSkins()){
            net.minecraft.client.renderer.entity.player.AvatarRenderer skin = event.getPlayerRenderer(skinType);
            skin.addLayer(new LayerRainbow(skin));
        }
        *///?}
        //? if forge && >=1.21 && <1.21.9 {
        /*for (net.minecraft.client.resources.PlayerSkin.Model skinType : event.getSkins()){
            net.minecraft.client.renderer.entity.player.PlayerRenderer skin = event.getPlayerSkin(skinType);
            skin.addLayer(new LayerRainbow(skin));
        }
        *///?}
        //? if forge && >=1.20.2 && <1.21 {
        /*for (net.minecraft.client.resources.PlayerSkin.Model skinType : event.getSkins()){
            net.minecraft.client.renderer.entity.player.PlayerRenderer skin = event.getSkin(skinType);
            skin.addLayer(new LayerRainbow(skin));
        }
        *///?}
        //? if neoforge && >=1.20.2 && <1.21.9 {
        /*for (net.minecraft.client.resources.PlayerSkin.Model skinType : event.getSkins()){
            // NeoForge made getSkin generic, so the target type has to name the renderer.
            net.minecraft.client.renderer.entity.player.PlayerRenderer skin = event.getSkin(skinType);
            skin.addLayer(new LayerRainbow(skin));
        }
        *///?}
        //? if <1.20.2 {
        for (String skinType : event.getSkins()){
            event.getSkin(skinType).addLayer(new LayerRainbow(event.getSkin(skinType)));
        }
        //?}
    }

    private static void addLayerIfApplicable(EntityType<? extends LivingEntity> entityType, EntityRenderersEvent.AddLayers event) {
        net.minecraft.client.renderer.entity.LivingEntityRenderer renderer = null;
        if(entityType != EntityType.ENDER_DRAGON){
            try{
                // Staged through EntityRenderer because NeoForge's getRenderer can't infer a
                // LivingEntityRenderer target directly; the cast is what the catch below guards.
                // Two axes: the getter is getEntityRenderer only on Forge >=1.21 (else getRenderer),
                // and EntityRenderer gained a second (render-state) type parameter in 1.21.2.
                //? if forge && >=1.21.2 {
                /*net.minecraft.client.renderer.entity.EntityRenderer<?, ?> found = event.getEntityRenderer(entityType);
                *///?} elif forge && >=1.21 {
                /*net.minecraft.client.renderer.entity.EntityRenderer<?> found = event.getEntityRenderer(entityType);
                *///?} elif >=1.21.2 {
                /*net.minecraft.client.renderer.entity.EntityRenderer<?, ?> found = event.getRenderer(entityType);
                *///?} else {
                net.minecraft.client.renderer.entity.EntityRenderer<?> found = event.getRenderer(entityType);
                //?}
                renderer = (net.minecraft.client.renderer.entity.LivingEntityRenderer) found;
            }catch (Exception e){
                AlexsMobs.LOGGER.warn("Could not apply rainbow color layer to " + BuiltInRegistries.ENTITY_TYPE.getKey(entityType) + ", has custom renderer that is not LivingEntityRenderer.");
            }
            if(renderer != null){
                renderer.addLayer(new LayerRainbow(renderer));
            }
        }
    }
}

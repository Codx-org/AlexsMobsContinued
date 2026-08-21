package com.github.alexthe666.alexsmobs;

import com.github.alexthe666.alexsmobs.misc.AMCompat;

import com.github.alexthe666.alexsmobs.block.AMBlockRegistry;
// ClientLayerRegistry is excluded from the Fabric compile too — fabric/client/FabricClientLayers
// stands in for it. See AlexsMobsFabric's javadoc.
//? if !fabric {
import com.github.alexthe666.alexsmobs.client.ClientLayerRegistry;
//?}
// client/event/ClientEvents is excluded from the Fabric compile — see AlexsMobsFabric's javadoc.
//? if !fabric {
import com.github.alexthe666.alexsmobs.client.event.ClientEvents;
//?}
import com.github.alexthe666.alexsmobs.client.gui.GUIAnimalDictionary;
import com.github.alexthe666.alexsmobs.client.gui.GUITransmutationTable;
import com.github.alexthe666.alexsmobs.client.particle.*;
import com.github.alexthe666.alexsmobs.client.render.*;
import com.github.alexthe666.alexsmobs.client.render.item.AMItemRenderProperties;
import com.github.alexthe666.alexsmobs.client.render.item.CustomArmorRenderProperties;
import com.github.alexthe666.alexsmobs.client.render.item.GhostlyPickaxeBakedModel;
import com.github.alexthe666.alexsmobs.client.render.tile.RenderCapsid;
import com.github.alexthe666.alexsmobs.client.render.tile.RenderTransmutationTable;
import com.github.alexthe666.alexsmobs.client.render.tile.RenderVoidWormBeak;
import com.github.alexthe666.alexsmobs.client.sound.SoundBearMusicBox;
import com.github.alexthe666.alexsmobs.client.sound.SoundLaCucaracha;
import com.github.alexthe666.alexsmobs.client.sound.SoundWormBoss;
import com.github.alexthe666.alexsmobs.entity.*;
import com.github.alexthe666.alexsmobs.entity.util.RainbowUtil;
import com.github.alexthe666.alexsmobs.inventory.AMMenuRegistry;
import com.github.alexthe666.alexsmobs.item.*;
import com.github.alexthe666.alexsmobs.tileentity.AMTileEntityRegistry;
import com.mojang.blaze3d.vertex.BufferBuilder;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
//? if <1.21.4 {
import net.minecraft.client.renderer.item.ItemProperties;
//?}
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
// Fabric has no counterpart for any of these — every method they annotate or parameterise gets
// its own `fabric` arm below, wired straight onto the Fabric API registries instead of a bus.
//? if !fabric {
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.common.MinecraftForge;
//?}
// EventBus 7 (Forge 1.21.6) deleted IEventBus outright — the mod bus is a BusGroup there.
//? if (forge && >=1.21.6) || fabric {
/*
*///?} else {
import net.minecraftforge.eventbus.api.IEventBus;
//?}
//? if !fabric {
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?}

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
// No @EventBusSubscriber: the colour handlers below are mod-bus events and init() already adds
// them there. The annotation put them on the game bus, which NeoForge rejects outright.
public class ClientProxy extends CommonProxy {

    public static final Int2ObjectMap<SoundBearMusicBox> BEAR_MUSIC_BOX_SOUND_MAP = new Int2ObjectOpenHashMap<>();
    public static final Int2ObjectMap<SoundLaCucaracha> COCKROACH_SOUND_MAP = new Int2ObjectOpenHashMap<>();
    public static final Int2ObjectMap<SoundWormBoss> WORMBOSS_SOUND_MAP = new Int2ObjectOpenHashMap<>();
    public static final List<UUID> currentUnrenderedEntities = new ArrayList<>();
    public static int voidPortalCreationTime = 0;
    public CameraType prevPOV = CameraType.FIRST_PERSON;
    public boolean initializedRainbowBuffers = false;
    private int pupfishChunkX = 0;
    private int pupfishChunkZ = 0;
    private int singingBlueJayId = -1;
    private final ItemStack[] transmuteStacks = new ItemStack[3];

    // 1.21.4 removed RegisterColorHandlersEvent.Item — item tints are declared in the item model
    // definition JSON now, so from there the straddleboard's two tints are AMStraddleboardTint
    // instead (registered below, written into the model definition by DataPackMigration). Both eras
    // ask that class for the colour, which is what makes the two settings mean the same thing on
    // all 49 nodes. The tint carries alpha on both sides: renderQuadList started unpacking the
    // handler's top byte at 1.20.6, so an alpha-less value draws the layer invisible from there up
    // (bug #108, which is why upstream's own 0xADC3D7 fallback vanished on those nodes).
    //? if <1.21.4 && !fabric {
    @SubscribeEvent
    @OnlyIn(value = Dist.CLIENT)
    public static void onItemColors(RegisterColorHandlersEvent.Item event) {

        AlexsMobs.LOGGER.info("loaded in item colorizer");
        if(AMItemRegistry.STRADDLEBOARD.get() != null){
            event.register((stack, colorIn) -> com.github.alexthe666.alexsmobs.client.render.AMStraddleboardTint.tintOf(stack, colorIn >= 1), AMItemRegistry.STRADDLEBOARD.get());
        }else{
            AlexsMobs.LOGGER.warn("Could not add straddleboard item to colorizer...");
        }
    }
    //?} elif <1.21.4 {
    /*// Same seam as onBlockColors' pre-26 Fabric arm, on the item side: no bus, no event, the
    // registry is simply open during client init. The lambda body is the Forge arm's verbatim.
    public static void onItemColors() {
        AlexsMobs.LOGGER.info("loaded in item colorizer");
        if(AMItemRegistry.STRADDLEBOARD.get() != null){
            net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry.ITEM.register((stack, colorIn) -> com.github.alexthe666.alexsmobs.client.render.AMStraddleboardTint.tintOf(stack, colorIn >= 1), AMItemRegistry.STRADDLEBOARD.get());
        }else{
            AlexsMobs.LOGGER.warn("Could not add straddleboard item to colorizer...");
        }
    }
    *///?}

    // 26.1 replaced the single lambda `BlockColor` with a List<BlockTintSource>, and NeoForge renamed
    // the nested event to BlockTintSources while Forge kept `Block` — so the two loaders need separate
    // arms above 26. The tint itself is unchanged: BlockTintSource#colorInWorld hands over exactly the
    // (state, level, pos) the old BlockColor did, and the position-only noise function ports verbatim.
    // It cannot be a lambda — the position-aware method is the *default* one, not the abstract one.
    //? if forge && >=26 {
    /*@SubscribeEvent
    @OnlyIn(value = Dist.CLIENT)
    public static void onBlockColors(RegisterColorHandlersEvent.Block event) {
        AlexsMobs.LOGGER.info("loaded in block colorizer");
        event.register(List.of(rainbowGlassTint()), AMBlockRegistry.RAINBOW_GLASS.get());
    }
    *///?} elif neoforge && >=26 {
    /*@SubscribeEvent
    @OnlyIn(value = Dist.CLIENT)
    public static void onBlockColors(RegisterColorHandlersEvent.BlockTintSources event) {
        AlexsMobs.LOGGER.info("loaded in block colorizer");
        event.register(List.of(rainbowGlassTint()), AMBlockRegistry.RAINBOW_GLASS.get());
    }
    *///?} elif fabric && >=26 {
    /*public static void onBlockColors() {
        AlexsMobs.LOGGER.info("loaded in block colorizer");
        net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry.register(List.of(rainbowGlassTint()), AMBlockRegistry.RAINBOW_GLASS.get());
    }
    *///?} elif fabric {
    /*// Below 26 the tint is still a single BlockColor lambda, and Fabric API's seam for it is the
    // older ColorProviderRegistry.BLOCK rather than BlockColorRegistry (which does not exist yet).
    // The lambda body is the same one the pre-26 Forge arm below registers.
    public static void onBlockColors() {
        AlexsMobs.LOGGER.info("loaded in block colorizer");
        net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry.BLOCK.register((state, tintGetter, pos, tint) -> {
            return tintGetter != null && pos != null ? RainbowUtil.calculateGlassColor(pos) : -1;
        }, AMBlockRegistry.RAINBOW_GLASS.get());
    }
    *///?} else {
    @SubscribeEvent
    @OnlyIn(value = Dist.CLIENT)
    public static void onBlockColors(RegisterColorHandlersEvent.Block event) {
        AlexsMobs.LOGGER.info("loaded in block colorizer");
        event.register((state, tintGetter, pos, tint) -> {
            return tintGetter != null && pos != null ? RainbowUtil.calculateGlassColor(pos) : -1;
        }, AMBlockRegistry.RAINBOW_GLASS.get());
    }
    //?}

    //? if >=26 {
    /*private static net.minecraft.client.color.block.BlockTintSource rainbowGlassTint() {
        return new net.minecraft.client.color.block.BlockTintSource() {
            // The abstract, position-free form: vanilla uses it for inventory/item rendering, where
            // there is no world position to derive the rainbow from. -1 is "no tint".
            @Override
            public int color(net.minecraft.world.level.block.state.BlockState state) {
                return -1;
            }

            @Override
            public int colorInWorld(net.minecraft.world.level.block.state.BlockState state, net.minecraft.client.renderer.block.BlockAndTintGetter level, net.minecraft.core.BlockPos pos) {
                return level != null && pos != null ? RainbowUtil.calculateGlassColor(pos) : -1;
            }
        };
    }
    *///?}

    public void init() {
        // NeoForge 1.21 deleted FMLJavaModLoadingContext; the mod bus now hangs off the container.
        // init() runs during mod construction, so the active container is ours.
        // Forge 1.21.6's EventBus 7 has no IEventBus and no group-level addListener; each event
        // class hands out its own bus. See AlexsMobs' constructor for the same shape.
        //? if neoforge && >=1.21 {
        /*IEventBus bus = net.neoforged.fml.ModLoadingContext.get().getActiveContainer().getEventBus();
        *///?} elif forge && >=1.21.6 {
        /*net.minecraftforge.eventbus.api.bus.BusGroup bus = net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModBusGroup();
        *///?} elif fabric {
        /*// Fabric has no mod bus at all: there is nothing to declare here, and everything below
        // registers straight against the Fabric API registries instead of a listener.
        *///?} else {
        IEventBus bus = net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();
        //?}
        // onBakingCompleted (baked-model wrap) and onItemColors (item tint handler) target APIs removed
        // in 1.21.4; both are gated off there along with the methods they reference.
        //? if <1.21.4 && !fabric {
        bus.addListener(ClientProxy::onBakingCompleted);
        bus.addListener(ClientProxy::onItemColors);
        //?} elif <1.21.4 {
        /*// Fabric gets the item tint but NOT the baked-model wrap: onBakingCompleted mutates
        // ModelEvent.ModifyBakingResult's model map, and Fabric's equivalent seam is a whole
        // different architecture (a ModelLoadingPlugin registered before resource reload, not a
        // post-bake mutation). The ghostly pickaxe's fullbright wrap is therefore a cosmetic loss
        // on Fabric — the same loss every loader already takes on >=1.21.4.
        onItemColors();
        *///?}
        //? if forge && >=1.21.6 && <26 {
        /*RegisterColorHandlersEvent.Block.getBus(bus).addListener(ClientProxy::onBlockColors);
        EntityRenderersEvent.AddLayers.getBus(bus).addListener(ClientLayerRegistry::onAddLayers);
        RegisterParticleProvidersEvent.getBus(bus).addListener(ClientProxy::setupParticles);
        *///?} elif forge && >=26 {
        /*// Forge 64 took all three off the mod bus — each is a plain event with its own static BUS.
        RegisterColorHandlersEvent.Block.BUS.addListener(ClientProxy::onBlockColors);
        EntityRenderersEvent.AddLayers.BUS.addListener(ClientLayerRegistry::onAddLayers);
        RegisterParticleProvidersEvent.BUS.addListener(ClientProxy::setupParticles);
        *///?} elif fabric {
        /*// No listeners: Fabric's client registries are open all the way through client init, so
        // each of these just runs now. ⚠️ This arm MUST come before the version-only `elif >=26`
        // below — a bare version arm would otherwise swallow Fabric and hand it Forge code.
        onBlockColors();
        com.github.alexthe666.alexsmobs.fabric.client.FabricClientLayers.register();
        com.github.alexthe666.alexsmobs.client.model.layered.AMModelLayers.register();
        setupParticles(new com.github.alexthe666.alexsmobs.fabric.client.ParticleRegistry());
        *///?} elif >=26 {
        /*bus.addListener(ClientProxy::onBlockColors);
        bus.addListener(ClientLayerRegistry::onAddLayers);
        bus.addListener(ClientProxy::setupParticles);
        *///?} else {
        bus.addListener(ClientProxy::onBlockColors);
        bus.addListener(ClientLayerRegistry::onAddLayers);
        bus.addListener(ClientProxy::setupParticles);
        //?}
        // Forge 64's AddGuiOverlayLayersEvent is not a mod-bus event: like the rest of EventBus 7
        // it hands out its own static BUS.
        //? if forge && >=26 {
        /*net.minecraftforge.client.event.AddGuiOverlayLayersEvent.BUS.addListener(ClientProxy::onRegisterGuiLayers);
        *///?} elif neoforge && >=1.20.5 || forge && >=1.20.5 && <1.21 {
        /*bus.addListener(ClientProxy::onRegisterGuiLayers);
        *///?}
        //? if neoforge && >=1.20.6
        //bus.addListener(ClientProxy::onRegisterMenuScreens);
        //? if neoforge && >=1.21.2
        //bus.addListener(ClientProxy::onRegisterClientExtensions);
        // #45: the animated tab/advancement icons on >=1.21.4 render through AMIconSpecialRenderer,
        // the ISTER's replacement. Vanilla keeps SpecialModelRenderers.ID_MAPPER private on every
        // era (the public reading in Fabric dev jars is fabric-api's transitive access widener);
        // NeoForge has a dedicated mod-bus event, and Forge/Fabric get a name-free reflective put —
        // see the note on AMIconSpecialRenderer#register. init() runs during mod construction, so
        // both happen before the first resource reload can parse an item model.
        //? if neoforge && >=1.21.4
        //bus.addListener(ClientProxy::onRegisterSpecialModelRenderers);
        //? if (forge || fabric) && >=1.21.4
        //com.github.alexthe666.alexsmobs.client.render.AMIconSpecialRenderer.register();
        // The straddleboard's two configurable tints, same split for the same reason: NeoForge has
        // RegisterColorHandlersEvent.ItemTintSources on every node from 1.21.4, classic Forge has no
        // equivalent nested event at all (checked in the 1.21.4 and 26.2 universal jars), so Forge
        // shares Fabric's reflective put.
        //? if neoforge && >=1.21.4
        //bus.addListener(ClientProxy::onRegisterItemTintSources);
        //? if (forge || fabric) && >=1.21.4
        //com.github.alexthe666.alexsmobs.client.render.AMStraddleboardTint.register();
    }

    //? if neoforge && >=1.21.4 {
    /*public static void onRegisterSpecialModelRenderers(net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent event) {
        event.register(AMCompat.rl(AlexsMobs.MODID, "icon"),
                com.github.alexthe666.alexsmobs.client.render.AMIconSpecialRenderer.Unbaked.MAP_CODEC);
    }
    *///?}

    //? if neoforge && >=1.21.4 {
    /*public static void onRegisterItemTintSources(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.ItemTintSources event) {
        event.register(AMCompat.rl(AlexsMobs.MODID, "straddleboard_base"),
                com.github.alexthe666.alexsmobs.client.render.AMStraddleboardTint.BASE_CODEC);
        event.register(AMCompat.rl(AlexsMobs.MODID, "straddleboard_panel"),
                com.github.alexthe666.alexsmobs.client.render.AMStraddleboardTint.PANEL_CODEC);
    }
    *///?}

    //? if neoforge && >=1.20.6 {
    /*public static void onRegisterMenuScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(AMMenuRegistry.TRANSMUTATION_TABLE.get(), GUITransmutationTable::new);
    }
    *///?}

    // NeoForge 1.21.2 removed IItemExtension.initializeClient; per-item client extensions are now
    // registered through this mod-bus event. Each of our items that carried an initializeClient
    // implements IClientExtensionItem, so we can drive them all from one loop.
    //? if neoforge && >=1.21.2 {
    /*public static void onRegisterClientExtensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
        for (net.minecraft.world.item.Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            if (item instanceof com.github.alexthe666.alexsmobs.item.IClientExtensionItem ext) {
                ext.initializeClient(e -> event.registerItem(e, item));
            }
        }
    }
    *///?}

    // 1.20.5 turned the HUD into a vanilla LayeredDraw stack, so the farseer static screen is
    // now a registered layer sitting just above the camera/helmet overlay instead of a hook on
    // Forge's old per-overlay render event. The two loaders spell the mod-bus event differently.
    // Forge 51.x (1.21) ships neither the old event nor a layer registry — that node draws the
    // overlay from client.GuiMixin instead.
    //? if forge && >=1.20.5 && <1.21 {
    /*public static void onRegisterGuiLayers(net.minecraftforge.client.event.AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().addAbove(net.minecraftforge.client.gui.overlay.ForgeLayeredDraw.CAMERA_OVERLAY,
                AMCompat.rl(AlexsMobs.MODID, "farseer_static"),
                (guiGraphics, partialTick) -> com.github.alexthe666.alexsmobs.client.event.ClientEvents.renderStaticOverlay(guiGraphics, partialTick));
    }
    *///?}

    // Forge 64 brought the layer registry back, so 1.21's GuiMixin workaround is only needed
    // below 26. The layer callback is 26's extract phase (GuiGraphicsExtractor + DeltaTracker),
    // and addAbove's argument order is (newLayer, otherLayer, layer) here — not the
    // (otherLayer, newLayer, layer) of the 1.20.5/1.20.6 form.
    //? if forge && >=26 {
    /*public static void onRegisterGuiLayers(net.minecraftforge.client.event.AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().addAbove(AMCompat.rl(AlexsMobs.MODID, "farseer_static"),
                net.minecraftforge.client.gui.overlay.ForgeLayeredDraw.CAMERA_OVERLAY,
                (guiGraphics, deltaTracker) -> com.github.alexthe666.alexsmobs.client.event.ClientEvents.renderStaticOverlay(guiGraphics, deltaTracker.getGameTimeDeltaPartialTick(false)));
    }
    *///?}

    //? if neoforge && >=1.20.5 && <1.21 {
    /*public static void onRegisterGuiLayers(net.neoforged.neoforge.client.event.RegisterGuiLayersEvent event) {
        event.registerAbove(net.neoforged.neoforge.client.gui.VanillaGuiLayers.CAMERA_OVERLAYS,
                AMCompat.rl(AlexsMobs.MODID, "farseer_static"),
                (guiGraphics, partialTick) -> com.github.alexthe666.alexsmobs.client.event.ClientEvents.renderStaticOverlay(guiGraphics, partialTick));
    }
    *///?}

    // 1.21 handed HUD layers a DeltaTracker rather than a bare partial tick.
    //? if neoforge && >=1.21 {
    /*public static void onRegisterGuiLayers(net.neoforged.neoforge.client.event.RegisterGuiLayersEvent event) {
        event.registerAbove(net.neoforged.neoforge.client.gui.VanillaGuiLayers.CAMERA_OVERLAYS,
                AMCompat.rl(AlexsMobs.MODID, "farseer_static"),
                (guiGraphics, deltaTracker) -> com.github.alexthe666.alexsmobs.client.event.ClientEvents.renderStaticOverlay(guiGraphics, deltaTracker.getGameTimeDeltaPartialTick(false)));
    }
    *///?}

    public void clientInit() {
        // Fabric has no bus to scan for @SubscribeEvent, so the same handlers are called by name
        // from fabric/client/FabricClientEvents — which also holds the one shared instance.
        //? if !fabric {
        MinecraftForge.EVENT_BUS.register(new ClientEvents());
        //?} else {
        /*com.github.alexthe666.alexsmobs.fabric.client.FabricClientEvents.register();
        // The other half of what IClientItemExtensions does on the two loaders: the custom armour
        // models, which on Fabric are handed to Fabric API's ArmorRenderer instead.
        com.github.alexthe666.alexsmobs.fabric.client.FabricArmorRenderers.register();
        // ...and the first half, the ISTER, which below 1.21.4 goes to Fabric API's
        // BuiltinItemRendererRegistry. Without it the eleven items whose model is builtin/entity
        // drew nothing at all on Fabric. A no-op from 1.21.4 up, where vanilla deleted the whole
        // mechanism and DataPackMigration rebuilds those models instead.
        com.github.alexthe666.alexsmobs.fabric.client.FabricItemRenderers.register();
        // The "render_type" field in this mod's block models is a Forge/NeoForge model-loader
        // extension that vanilla and Fabric silently ignore, so thirteen blocks drew as SOLID.
        // A no-op from 26.1 up, where the layer is derived from the texture itself.
        com.github.alexthe666.alexsmobs.fabric.client.FabricBlockRenderLayers.register();
        *///?}
        // Forge calls clientInit from FMLClientSetupEvent, long after Minecraft's constructor; Fabric
        // calls it from ClientModInitializer, which runs INSIDE that constructor (Minecraft.<init> ->
        // Hooks.startClient), before `renderBuffers` is assigned — so on <1.20.2 this NPEs. Skipping it
        // leaves Fabric 1.20.1 with the same shared-builder fallback every >=1.20.2 node already has,
        // which the note on initRainbowBuffers records as fine for the handful of types AM uses.
        //? if !fabric
        initRainbowBuffers();
        //? if <26
        ItemRenderer itemRendererIn = Minecraft.getInstance().getItemRenderer();
        EntityRenderers.register(AMEntityRegistry.GRIZZLY_BEAR.get(), RenderGrizzlyBear::new);
        EntityRenderers.register(AMEntityRegistry.ROADRUNNER.get(), RenderRoadrunner::new);
        EntityRenderers.register(AMEntityRegistry.BONE_SERPENT.get(), RenderBoneSerpent::new);
        EntityRenderers.register(AMEntityRegistry.BONE_SERPENT_PART.get(), RenderBoneSerpentPart::new);
        EntityRenderers.register(AMEntityRegistry.GAZELLE.get(), RenderGazelle::new);
        EntityRenderers.register(AMEntityRegistry.CROCODILE.get(), RenderCrocodile::new);
        EntityRenderers.register(AMEntityRegistry.FLY.get(), RenderFly::new);
        EntityRenderers.register(AMEntityRegistry.HUMMINGBIRD.get(), RenderHummingbird::new);
        EntityRenderers.register(AMEntityRegistry.ORCA.get(), RenderOrca::new);
        EntityRenderers.register(AMEntityRegistry.SUNBIRD.get(), RenderSunbird::new);
        EntityRenderers.register(AMEntityRegistry.GORILLA.get(), RenderGorilla::new);
        EntityRenderers.register(AMEntityRegistry.CRIMSON_MOSQUITO.get(), RenderCrimsonMosquito::new);
        EntityRenderers.register(AMEntityRegistry.MOSQUITO_SPIT.get(), RenderMosquitoSpit::new);
        EntityRenderers.register(AMEntityRegistry.RATTLESNAKE.get(), RenderRattlesnake::new);
        EntityRenderers.register(AMEntityRegistry.ENDERGRADE.get(), RenderEndergrade::new);
        EntityRenderers.register(AMEntityRegistry.HAMMERHEAD_SHARK.get(), RenderHammerheadShark::new);
        EntityRenderers.register(AMEntityRegistry.SHARK_TOOTH_ARROW.get(), RenderSharkToothArrow::new);
        EntityRenderers.register(AMEntityRegistry.LOBSTER.get(), RenderLobster::new);
        EntityRenderers.register(AMEntityRegistry.KOMODO_DRAGON.get(), RenderKomodoDragon::new);
        EntityRenderers.register(AMEntityRegistry.CAPUCHIN_MONKEY.get(), RenderCapuchinMonkey::new);
        EntityRenderers.register(AMEntityRegistry.TOSSED_ITEM.get(), RenderTossedItem::new);
        EntityRenderers.register(AMEntityRegistry.CENTIPEDE_HEAD.get(), RenderCentipedeHead::new);
        EntityRenderers.register(AMEntityRegistry.CENTIPEDE_BODY.get(), RenderCentipedeBody::new);
        EntityRenderers.register(AMEntityRegistry.CENTIPEDE_TAIL.get(), RenderCentipedeTail::new);
        EntityRenderers.register(AMEntityRegistry.WARPED_TOAD.get(), RenderWarpedToad::new);
        EntityRenderers.register(AMEntityRegistry.MOOSE.get(), RenderMoose::new);
        EntityRenderers.register(AMEntityRegistry.MIMICUBE.get(), RenderMimicube::new);
        EntityRenderers.register(AMEntityRegistry.RACCOON.get(), RenderRaccoon::new);
        EntityRenderers.register(AMEntityRegistry.BLOBFISH.get(), RenderBlobfish::new);
        EntityRenderers.register(AMEntityRegistry.SEAL.get(), RenderSeal::new);
        EntityRenderers.register(AMEntityRegistry.COCKROACH.get(), RenderCockroach::new);
        EntityRenderers.register(AMEntityRegistry.COCKROACH_EGG.get(), (render) -> {
            return new ThrownItemRenderer<>(render, 0.75F, true);
        });
        EntityRenderers.register(AMEntityRegistry.SHOEBILL.get(), RenderShoebill::new);
        EntityRenderers.register(AMEntityRegistry.ELEPHANT.get(), RenderElephant::new);
        EntityRenderers.register(AMEntityRegistry.SOUL_VULTURE.get(), RenderSoulVulture::new);
        EntityRenderers.register(AMEntityRegistry.SNOW_LEOPARD.get(), RenderSnowLeopard::new);
        EntityRenderers.register(AMEntityRegistry.SPECTRE.get(), RenderSpectre::new);
        EntityRenderers.register(AMEntityRegistry.CROW.get(), RenderCrow::new);
        EntityRenderers.register(AMEntityRegistry.ALLIGATOR_SNAPPING_TURTLE.get(), RenderAlligatorSnappingTurtle::new);
        EntityRenderers.register(AMEntityRegistry.MUNGUS.get(), RenderMungus::new);
        EntityRenderers.register(AMEntityRegistry.MANTIS_SHRIMP.get(), RenderMantisShrimp::new);
        EntityRenderers.register(AMEntityRegistry.GUSTER.get(), RenderGuster::new);
        EntityRenderers.register(AMEntityRegistry.SAND_SHOT.get(), RenderSandShot::new);
        EntityRenderers.register(AMEntityRegistry.GUST.get(), RenderGust::new);
        EntityRenderers.register(AMEntityRegistry.WARPED_MOSCO.get(), RenderWarpedMosco::new);
        EntityRenderers.register(AMEntityRegistry.HEMOLYMPH.get(), RenderHemolymph::new);
        EntityRenderers.register(AMEntityRegistry.STRADDLER.get(), RenderStraddler::new);
        EntityRenderers.register(AMEntityRegistry.STRADPOLE.get(), RenderStradpole::new);
        EntityRenderers.register(AMEntityRegistry.STRADDLEBOARD.get(), RenderStraddleboard::new);
        EntityRenderers.register(AMEntityRegistry.EMU.get(), RenderEmu::new);
        EntityRenderers.register(AMEntityRegistry.EMU_EGG.get(), (render) -> {
            return new ThrownItemRenderer<>(render, 0.75F, true);
        });
        EntityRenderers.register(AMEntityRegistry.PLATYPUS.get(), RenderPlatypus::new);
        EntityRenderers.register(AMEntityRegistry.DROPBEAR.get(), RenderDropBear::new);
        EntityRenderers.register(AMEntityRegistry.TASMANIAN_DEVIL.get(), RenderTasmanianDevil::new);
        EntityRenderers.register(AMEntityRegistry.KANGAROO.get(), RenderKangaroo::new);
        EntityRenderers.register(AMEntityRegistry.CACHALOT_WHALE.get(), RenderCachalotWhale::new);
        EntityRenderers.register(AMEntityRegistry.CACHALOT_ECHO.get(), RenderCachalotEcho::new);
        EntityRenderers.register(AMEntityRegistry.LEAFCUTTER_ANT.get(), RenderLeafcutterAnt::new);
        EntityRenderers.register(AMEntityRegistry.ENDERIOPHAGE.get(), RenderEnderiophage::new);
        EntityRenderers.register(AMEntityRegistry.ENDERIOPHAGE_ROCKET.get(), (render) -> {
            return new ThrownItemRenderer<>(render, 0.75F, true);
        });
        EntityRenderers.register(AMEntityRegistry.BALD_EAGLE.get(), RenderBaldEagle::new);
        EntityRenderers.register(AMEntityRegistry.TIGER.get(), RenderTiger::new);
        EntityRenderers.register(AMEntityRegistry.TARANTULA_HAWK.get(), RenderTarantulaHawk::new);
        EntityRenderers.register(AMEntityRegistry.VOID_WORM.get(), RenderVoidWormHead::new);
        EntityRenderers.register(AMEntityRegistry.VOID_WORM_PART.get(), RenderVoidWormBody::new);
        EntityRenderers.register(AMEntityRegistry.VOID_WORM_SHOT.get(), RenderVoidWormShot::new);
        EntityRenderers.register(AMEntityRegistry.VOID_PORTAL.get(), RenderVoidPortal::new);
        EntityRenderers.register(AMEntityRegistry.FRILLED_SHARK.get(), RenderFrilledShark::new);
        EntityRenderers.register(AMEntityRegistry.MIMIC_OCTOPUS.get(), RenderMimicOctopus::new);
        EntityRenderers.register(AMEntityRegistry.SEAGULL.get(), RenderSeagull::new);
        EntityRenderers.register(AMEntityRegistry.FROSTSTALKER.get(), RenderFroststalker::new);
        EntityRenderers.register(AMEntityRegistry.ICE_SHARD.get(), RenderIceShard::new);
        EntityRenderers.register(AMEntityRegistry.TUSKLIN.get(), RenderTusklin::new);
        EntityRenderers.register(AMEntityRegistry.LAVIATHAN.get(), RenderLaviathan::new);
        EntityRenderers.register(AMEntityRegistry.COSMAW.get(), RenderCosmaw::new);
        EntityRenderers.register(AMEntityRegistry.TOUCAN.get(), RenderToucan::new);
        EntityRenderers.register(AMEntityRegistry.MANED_WOLF.get(), RenderManedWolf::new);
        EntityRenderers.register(AMEntityRegistry.ANACONDA.get(), RenderAnaconda::new);
        EntityRenderers.register(AMEntityRegistry.ANACONDA_PART.get(), RenderAnacondaPart::new);
        EntityRenderers.register(AMEntityRegistry.VINE_LASSO.get(), RenderVineLasso::new);
        EntityRenderers.register(AMEntityRegistry.ANTEATER.get(), RenderAnteater::new);
        EntityRenderers.register(AMEntityRegistry.ROCKY_ROLLER.get(), RenderRockyRoller::new);
        EntityRenderers.register(AMEntityRegistry.FLUTTER.get(), RenderFlutter::new);
        EntityRenderers.register(AMEntityRegistry.POLLEN_BALL.get(), RenderPollenBall::new);
        EntityRenderers.register(AMEntityRegistry.GELADA_MONKEY.get(), RenderGeladaMonkey::new);
        EntityRenderers.register(AMEntityRegistry.JERBOA.get(), RenderJerboa::new);
        EntityRenderers.register(AMEntityRegistry.TERRAPIN.get(), RenderTerrapin::new);
        EntityRenderers.register(AMEntityRegistry.COMB_JELLY.get(), RenderCombJelly::new);
        EntityRenderers.register(AMEntityRegistry.COSMIC_COD.get(), RenderCosmicCod::new);
        EntityRenderers.register(AMEntityRegistry.BUNFUNGUS.get(), RenderBunfungus::new);
        EntityRenderers.register(AMEntityRegistry.BISON.get(), RenderBison::new);
        EntityRenderers.register(AMEntityRegistry.GIANT_SQUID.get(), RenderGiantSquid::new);
        EntityRenderers.register(AMEntityRegistry.SQUID_GRAPPLE.get(), RenderSquidGrapple::new);
        EntityRenderers.register(AMEntityRegistry.SEA_BEAR.get(), RenderSeaBear::new);
        EntityRenderers.register(AMEntityRegistry.DEVILS_HOLE_PUPFISH.get(), RenderDevilsHolePupfish::new);
        EntityRenderers.register(AMEntityRegistry.CATFISH.get(), RenderCatfish::new);
        EntityRenderers.register(AMEntityRegistry.FLYING_FISH.get(), RenderFlyingFish::new);
        EntityRenderers.register(AMEntityRegistry.SKELEWAG.get(), RenderSkelewag::new);
        EntityRenderers.register(AMEntityRegistry.RAIN_FROG.get(), RenderRainFrog::new);
        EntityRenderers.register(AMEntityRegistry.POTOO.get(), RenderPotoo::new);
        EntityRenderers.register(AMEntityRegistry.MUDSKIPPER.get(), RenderMudskipper::new);
        EntityRenderers.register(AMEntityRegistry.MUD_BALL.get(), RenderMudBall::new);
        EntityRenderers.register(AMEntityRegistry.RHINOCEROS.get(), RenderRhinoceros::new);
        EntityRenderers.register(AMEntityRegistry.SUGAR_GLIDER.get(), RenderSugarGlider::new);
        EntityRenderers.register(AMEntityRegistry.FARSEER.get(), RenderFarseer::new);
        EntityRenderers.register(AMEntityRegistry.SKREECHER.get(), RenderSkreecher::new);
        EntityRenderers.register(AMEntityRegistry.UNDERMINER.get(), RenderUnderminer::new);
        EntityRenderers.register(AMEntityRegistry.MURMUR.get(), RenderMurmurBody::new);
        EntityRenderers.register(AMEntityRegistry.MURMUR_HEAD.get(), RenderMurmurHead::new);
        EntityRenderers.register(AMEntityRegistry.TENDON_SEGMENT.get(), RenderTendonSegment::new);
        EntityRenderers.register(AMEntityRegistry.SKUNK.get(), RenderSkunk::new);
        EntityRenderers.register(AMEntityRegistry.FART.get(), RenderFart::new);
        EntityRenderers.register(AMEntityRegistry.BANANA_SLUG.get(), RenderBananaSlug::new);
        EntityRenderers.register(AMEntityRegistry.BLUE_JAY.get(), RenderBlueJay::new);
        EntityRenderers.register(AMEntityRegistry.CAIMAN.get(), RenderCaiman::new);
        EntityRenderers.register(AMEntityRegistry.TRIOPS.get(), RenderTriops::new);
        // 1.21.4 removed ItemProperties (predicate-driven model overrides). These 8 dynamic item states
        // — blocking shields, empty sprayers, broken elytra, in-chunk locator, etc. — are expressed as
        // range_dispatch/condition item model definitions now; gated off here and flagged in the porting notes.
        //? if <1.21.4 {
        try {
            ItemProperties.register(AMItemRegistry.BLOOD_SPRAYER.get(), AMCompat.rl("empty"), (stack, p_239428_1_, p_239428_2_, j) -> {
                return !ItemBloodSprayer.isUsable(stack) || p_239428_2_ instanceof Player && AMCompat.isOnCooldown(((Player) p_239428_2_).getCooldowns(), AMItemRegistry.BLOOD_SPRAYER.get()) ? 1.0F : 0.0F;
            });
            ItemProperties.register(AMItemRegistry.HEMOLYMPH_BLASTER.get(), AMCompat.rl("empty"), (stack, p_239428_1_, p_239428_2_, j) -> {
                return !ItemHemolymphBlaster.isUsable(stack) || p_239428_2_ instanceof Player && AMCompat.isOnCooldown(((Player) p_239428_2_).getCooldowns(), AMItemRegistry.HEMOLYMPH_BLASTER.get()) ? 1.0F : 0.0F;
            });
            ItemProperties.register(AMItemRegistry.TARANTULA_HAWK_ELYTRA.get(), AMCompat.rl("broken"), (stack, p_239428_1_, p_239428_2_, j) -> {
                return ItemTarantulaHawkElytra.isUsable(stack) ? 0.0F : 1.0F;
            });
            ItemProperties.register(AMItemRegistry.SHIELD_OF_THE_DEEP.get(), AMCompat.rl("blocking"), (stack, p_239421_1_, p_239421_2_, j) -> {
                return p_239421_2_ != null && p_239421_2_.isUsingItem() && p_239421_2_.getUseItem() == stack ? 1.0F : 0.0F;
            });
            ItemProperties.register(AMItemRegistry.SOMBRERO.get(), AMCompat.rl("silly"), (stack, p_239421_1_, p_239421_2_, j) -> {
                return AlexsMobs.isAprilFools() ? 1.0F : 0.0F;
            });
            ItemProperties.register(AMItemRegistry.TENDON_WHIP.get(), AMCompat.rl("active"), (stack, p_239421_1_, holder, j) -> {
                return ItemTendonWhip.isActive(stack, holder) ? 1.0F : 0.0F;
            });
            ItemProperties.register(AMItemRegistry.PUPFISH_LOCATOR.get(), AMCompat.rl("in_chunk"), (stack, world, entity, j) -> {
                int x = pupfishChunkX * 16;
                int z = pupfishChunkZ * 16;
                if (entity != null && entity.getX() >= x && entity.getX() <= x + 16 && entity.getZ() >= z && entity.getZ() <= z + 16) {
                    return 1.0F;
                }
                return 0.0F;
            });
            ItemProperties.register(AMItemRegistry.SKELEWAG_SWORD.get(), AMCompat.rl("blocking"), (stack, p_239421_1_, p_239421_2_, j) -> {
                return p_239421_2_ != null && p_239421_2_.isUsingItem() && p_239421_2_.getUseItem() == stack ? 1.0F : 0.0F;
            });
        } catch (Exception e) {
            AlexsMobs.LOGGER.warn("Could not load item models for weapons");
        }
        //?}
        BlockEntityRenderers.register(AMTileEntityRegistry.CAPSID.get(), RenderCapsid::new);
        BlockEntityRenderers.register(AMTileEntityRegistry.VOID_WORM_BEAK.get(), RenderVoidWormBeak::new);
        BlockEntityRenderers.register(AMTileEntityRegistry.TRANSMUTATION_TABLE.get(), RenderTransmutationTable::new);
        // NeoForge 20.6 closed MenuScreens.register off; screens go through a mod-bus event.
        // Fabric has no such event — the method is private in VANILLA (Forge patches it public),
        // so alexsmobs.accesswidener widens it and its ScreenConstructor there.
        //? if forge || fabric || <1.20.6
        MenuScreens.register(AMMenuRegistry.TRANSMUTATION_TABLE.get(), GUITransmutationTable::new);
    }

    private void initRainbowBuffers() {
        // 1.20.2 replaced RenderBuffers.fixedBuffers with SectionBufferBuilderPack and no longer
        // lets mods pre-register a fixed buffer; MultiBufferSource falls back to the shared
        // builder for these render types, which is fine for the handful AM uses.
//? if <1.20.2 {
        Minecraft.getInstance().renderBuffers().fixedBuffers.put(AMRenderTypes.COMBJELLY_RAINBOW_GLINT, new BufferBuilder(AMRenderTypes.COMBJELLY_RAINBOW_GLINT.bufferSize()));
        Minecraft.getInstance().renderBuffers().fixedBuffers.put(AMRenderTypes.VOID_WORM_PORTAL_OVERLAY, new BufferBuilder(AMRenderTypes.VOID_WORM_PORTAL_OVERLAY.bufferSize()));
        Minecraft.getInstance().renderBuffers().fixedBuffers.put(AMRenderTypes.STATIC_PORTAL, new BufferBuilder(AMRenderTypes.STATIC_PORTAL.bufferSize()));
        Minecraft.getInstance().renderBuffers().fixedBuffers.put(AMRenderTypes.STATIC_PARTICLE, new BufferBuilder(AMRenderTypes.STATIC_PARTICLE.bufferSize()));
        Minecraft.getInstance().renderBuffers().fixedBuffers.put(AMRenderTypes.STATIC_ENTITY, new BufferBuilder(AMRenderTypes.STATIC_ENTITY.bufferSize()));
//?}
        initializedRainbowBuffers = true;
    }

    // 1.21.4 removed ModelEvent.ModifyBakingResult#getModels — the baked-model registry is no longer
    // mutable this way. The ghostly-pickaxe fullbright wrap is a cosmetic loss on >=1.21.4 (flagged in the porting notes).
    // Fabric takes the same loss on every version: see the listener block in init() for why.
    //? if <1.21.4 && !fabric {
    private static void onBakingCompleted(final ModelEvent.ModifyBakingResult e) {
        String ghostlyPickaxe = "alexsmobs:ghostly_pickaxe";
        // 1.21 re-typed the baking result's keys from ResourceLocation to ModelResourceLocation.
        for (var id : e.getModels().keySet()) {
            if (id.toString().contains(ghostlyPickaxe)) {
                e.getModels().put(id, new GhostlyPickaxeBakedModel(e.getModels().get(id)));
            }
        }
    }
    //?}

    public void openBookGUI(ItemStack itemStackIn) {
        Minecraft.getInstance().setScreen(new GUIAnimalDictionary(itemStackIn));
    }

    public void openBookGUI(ItemStack itemStackIn, String page) {
        Minecraft.getInstance().setScreen(new GUIAnimalDictionary(itemStackIn, page));
    }

    public Player getClientSidePlayer() {
        return Minecraft.getInstance().player;
    }

    @OnlyIn(value = Dist.CLIENT)
    public Object getArmorModel(int armorId, LivingEntity entity) {
        switch (armorId) {
            /*
            case 0:
                return ROADRUNNER_BOOTS_MODEL;
            case 1:
                return MOOSE_HEADGEAR_MODEL;
            case 2:
                return FRONTIER_CAP_MODEL.withAnimations(entity);
            case 3:
                return SOMBRERO_MODEL;
            case 4:
                return SPIKED_TURTLE_SHELL_MODEL;
            case 5:
                return FEDORA_MODEL;
            case 6:
                return ELYTRA_MODEL.withAnimations(entity);

             */
            default:
                return null;
        }
    }

    @OnlyIn(value = Dist.CLIENT)
    public void onEntityStatus(Entity entity, byte updateKind) {
        if (updateKind == 67) {
            if (entity instanceof EntityCockroach && entity.isAlive()) {
                SoundLaCucaracha sound;
                if (COCKROACH_SOUND_MAP.get(entity.getId()) == null) {
                    sound = new SoundLaCucaracha((EntityCockroach) entity);
                    COCKROACH_SOUND_MAP.put(entity.getId(), sound);
                } else {
                    sound = COCKROACH_SOUND_MAP.get(entity.getId());
                }
                if (!Minecraft.getInstance().getSoundManager().isActive(sound) && sound.canPlaySound() && sound.isOnlyCockroach()) {
                    Minecraft.getInstance().getSoundManager().play(sound);
                }
            } else if (entity instanceof EntityVoidWorm && entity.isAlive()) {
                final float f2 = Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MUSIC);
                if (f2 <= 0) {
                    WORMBOSS_SOUND_MAP.clear();
                } else {
                    SoundWormBoss sound;
                    if (WORMBOSS_SOUND_MAP.get(entity.getId()) == null) {
                        sound = new SoundWormBoss((EntityVoidWorm) entity);
                        WORMBOSS_SOUND_MAP.put(entity.getId(), sound);
                    } else {
                        sound = WORMBOSS_SOUND_MAP.get(entity.getId());
                    }
                    if (!Minecraft.getInstance().getSoundManager().isActive(sound) && sound.isNearest()) {
                        Minecraft.getInstance().getSoundManager().play(sound);
                    }
                }
            } else if (entity instanceof EntityGrizzlyBear && entity.isAlive()) {
                SoundBearMusicBox sound;
                if (BEAR_MUSIC_BOX_SOUND_MAP.get(entity.getId()) == null) {
                    sound = new SoundBearMusicBox((EntityGrizzlyBear) entity);
                    BEAR_MUSIC_BOX_SOUND_MAP.put(entity.getId(), sound);
                } else {
                    sound = BEAR_MUSIC_BOX_SOUND_MAP.get(entity.getId());
                }
                if (!Minecraft.getInstance().getSoundManager().isActive(sound) && sound.canPlaySound() && sound.isOnlyMusicBox()) {
                    Minecraft.getInstance().getSoundManager().play(sound);
                }
            } else if (entity instanceof EntityBlueJay && entity.isAlive()) {
                singingBlueJayId = entity.getId();
            }
        }
        if (entity instanceof EntityBlueJay && entity.isAlive() && updateKind == 68) {
            singingBlueJayId = -1;
        }
    }

    public void updateBiomeVisuals(int x, int z) {
        // MC 26.2 moved section invalidation off LevelRenderer onto ClientLevel (which forwards to
        // the new LevelExtractor), and the surviving entry point takes SECTION coordinates where
        // the old one took block coordinates and converted internally. The odd argument list is
        // upstream's — it passes x where z belongs — and is reproduced verbatim so this node
        // refreshes exactly the same volume every other node does.
        //? if >=26.2 {
        /*Minecraft.getInstance().level.setSectionRangeDirty(
                net.minecraft.core.SectionPos.blockToSectionCoord(x - 33), net.minecraft.core.SectionPos.blockToSectionCoord(-1), net.minecraft.core.SectionPos.blockToSectionCoord(x - 33),
                net.minecraft.core.SectionPos.blockToSectionCoord(z + 33), net.minecraft.core.SectionPos.blockToSectionCoord(256), net.minecraft.core.SectionPos.blockToSectionCoord(z + 33));
        *///?}
        //? if <26.2 {
        Minecraft.getInstance().levelRenderer.setBlocksDirty(x - 32, 0, x - 32, z + 32, 255, z + 32);
        //?}
    }

    // Only the SIGNATURE is gated: fabric/client/ParticleRegistry deliberately carries Forge's two
    // method names, so the 22-line body below is shared verbatim by all three loaders.
    //? if fabric {
    /*public static void setupParticles(com.github.alexthe666.alexsmobs.fabric.client.ParticleRegistry registry) {
    *///?} else {
    public static void setupParticles(RegisterParticleProvidersEvent registry) {
    //?}
        AlexsMobs.LOGGER.debug("Registered particle factories");
        registry.registerSpriteSet(AMParticleRegistry.GUSTER_SAND_SPIN.get(), ParticleGusterSandSpin.Factory::new);
        registry.registerSpriteSet(AMParticleRegistry.GUSTER_SAND_SHOT.get(), ParticleGusterSandShot.Factory::new);
        registry.registerSpriteSet(AMParticleRegistry.GUSTER_SAND_SPIN_RED.get(), ParticleGusterSandSpin.FactoryRed::new);
        registry.registerSpriteSet(AMParticleRegistry.GUSTER_SAND_SHOT_RED.get(), ParticleGusterSandShot.FactoryRed::new);
        registry.registerSpriteSet(AMParticleRegistry.GUSTER_SAND_SPIN_SOUL.get(), ParticleGusterSandSpin.FactorySoul::new);
        registry.registerSpriteSet(AMParticleRegistry.GUSTER_SAND_SHOT_SOUL.get(), ParticleGusterSandShot.FactorySoul::new);
        registry.registerSpriteSet(AMParticleRegistry.HEMOLYMPH.get(), ParticleHemolymph.Factory::new);
        registry.registerSpriteSet(AMParticleRegistry.PLATYPUS_SENSE.get(), ParticlePlatypus.Factory::new);
        registry.registerSpriteSet(AMParticleRegistry.WHALE_SPLASH.get(), ParticleWhaleSplash.Factory::new);
        registry.registerSpriteSet(AMParticleRegistry.DNA.get(), ParticleDna.Factory::new);
        registry.registerSpriteSet(AMParticleRegistry.SHOCKED.get(), ParticleSimpleHeart.Factory::new);
        registry.registerSpriteSet(AMParticleRegistry.WORM_PORTAL.get(), ParticleWormPortal.Factory::new);
        registry.registerSpriteSet(AMParticleRegistry.INVERT_DIG.get(), ParticleInvertDig.Factory::new);
        registry.registerSpriteSet(AMParticleRegistry.TEETH_GLINT.get(), ParticleTeethGlint.Factory::new);
        registry.registerSpriteSet(AMParticleRegistry.SMELLY.get(), ParticleSmelly.Factory::new);
        registry.registerSpriteSet(AMParticleRegistry.BUNFUNGUS_TRANSFORMATION.get(), ParticleBunfungusTransformation.Factory::new);
        registry.registerSpriteSet(AMParticleRegistry.FUNGUS_BUBBLE.get(), ParticleFungusBubble.Factory::new);
        registry.registerSpecial(AMParticleRegistry.BEAR_FREDDY.get(), new ParticleBearFreddy.Factory());
        registry.registerSpriteSet(AMParticleRegistry.SUNBIRD_FEATHER.get(), ParticleSunbirdFeather.Factory::new);
        registry.registerSpecial(AMParticleRegistry.STATIC_SPARK.get(), new ParticleStaticSpark.Factory());
        registry.registerSpecial(AMParticleRegistry.SKULK_BOOM.get(), new ParticleSkulkBoom.Factory());
        registry.registerSpriteSet(AMParticleRegistry.BIRD_SONG.get(), ParticleBirdSong.Factory::new);
    }


    public void setRenderViewEntity(Entity entity) {
        prevPOV = Minecraft.getInstance().options.getCameraType();
        Minecraft.getInstance().setCameraEntity(entity);
        Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_BACK);
    }

    public void resetRenderViewEntity() {
        Minecraft.getInstance().setCameraEntity(Minecraft.getInstance().player);
    }

    public int getPreviousPOV() {
        return prevPOV.ordinal();
    }

    public boolean isFarFromCamera(double x, double y, double z) {
        Minecraft lvt_1_1_ = Minecraft.getInstance();
        return lvt_1_1_.gameRenderer.getMainCamera().getPosition().distanceToSqr(x, y, z) >= 256.0D;
    }

    public void resetVoidPortalCreation(Player player) {

    }

    // Dead everywhere (the body has been empty since upstream), and its parameter type is Forge-only.
    //? if !fabric {
    @OnlyIn(value = Dist.CLIENT)
    public void onRegisterEntityRenders(EntityRenderersEvent.RegisterLayerDefinitions event) {
    }
    //?}

    @Override
    public Object getISTERProperties() {
        return new AMItemRenderProperties();
    }

    @Override
    public Object getArmorRenderProperties() {
        return new CustomArmorRenderProperties();
    }

    public void spawnSpecialParticle(int type) {
        if (type == 0) {
            Minecraft.getInstance().level.addParticle(AMParticleRegistry.BEAR_FREDDY.get(), Minecraft.getInstance().player.getX(), Minecraft.getInstance().player.getY(), Minecraft.getInstance().player.getZ(), 0, 0, 0);
        }
    }

    public void processVisualFlag(Entity entity, int flag) {
        if (entity == Minecraft.getInstance().player && flag == 87) {
            // No ClientEvents on Fabric, so the farseer's static-screen effect is inert there.
            //? if !fabric
            ClientEvents.renderStaticScreenFor = 60;
        }
    }

    public void setPupfishChunkForItem(int chunkX, int chunkZ) {
        this.pupfishChunkX = chunkX;
        this.pupfishChunkZ = chunkZ;
    }

    public void setDisplayTransmuteResult(int slot, ItemStack stack){
        transmuteStacks[Mth.clamp(slot, 0, 2)] = stack;
    }

    public ItemStack getDisplayTransmuteResult(int slot){
        ItemStack stack = transmuteStacks[Mth.clamp(slot, 0, 2)];
        return stack == null ? ItemStack.EMPTY : stack;
    }

    public int getSingingBlueJayId() {
        return singingBlueJayId;
    }

}

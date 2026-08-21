package com.github.alexthe666.alexsmobs;

import com.github.alexthe666.alexsmobs.misc.AMCompat;

import com.github.alexthe666.alexsmobs.block.AMBlockRegistry;
import com.github.alexthe666.alexsmobs.citadel.server.message.AnimationMessage;
import com.github.alexthe666.alexsmobs.citadel.server.message.PropertiesMessage;
import com.github.alexthe666.alexsmobs.client.model.layered.AMModelLayers;
import com.github.alexthe666.alexsmobs.client.particle.AMParticleRegistry;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.config.BiomeConfig;
import com.github.alexthe666.alexsmobs.config.ConfigHolder;
import com.github.alexthe666.alexsmobs.effect.AMEffectRegistry;
import com.github.alexthe666.alexsmobs.enchantment.AMEnchantmentRegistry;
import com.github.alexthe666.alexsmobs.entity.AMEntityRegistry;
// The Forge game-bus handlers are excluded from the compile on Fabric — see ModPlatformPlugin.
//? if !fabric
import com.github.alexthe666.alexsmobs.event.ServerEvents;
import com.github.alexthe666.alexsmobs.inventory.AMMenuRegistry;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.github.alexthe666.alexsmobs.message.*;
import com.github.alexthe666.alexsmobs.misc.*;
import com.github.alexthe666.alexsmobs.tileentity.AMTileEntityRegistry;
import com.github.alexthe666.alexsmobs.world.AMFeatureRegistry;
import com.github.alexthe666.alexsmobs.world.AMLeafcutterAntBiomeModifier;
import com.github.alexthe666.alexsmobs.world.AMMobSpawnBiomeModifier;
import com.github.alexthe666.alexsmobs.world.AMMobSpawnStructureModifier;
import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
//? if !fabric {
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
//?}
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.StructureModifier;
// EventBus 7 (Forge 1.21.6) has no IEventBus: the mod bus is a BusGroup, and the only use of the
// type here is the constructor parameter, which that node does not take. Fabric has no bus at all
// — its constructor takes the empty fabric/ModBus token instead.
//? if (forge && >=1.21.6) || fabric {
/*
*///?} else {
import net.minecraftforge.eventbus.api.IEventBus;
//?}
//? if !fabric {
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
//?}
//? if forge && >=1.20.2 {
/*import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;
*///?}
//? if forge && <1.20.2 {
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
//?}
import net.minecraftforge.registries.DeferredRegister;
// Only referenced from the biome/structure-modifier serializer block, which is Fabric-gated.
//? if !fabric
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Calendar;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.network.FriendlyByteBuf;
import java.util.Date;

// No @EventBusSubscriber: nothing here is annotated for automatic subscription, and NeoForge
// errors on an empty subscriber class. Fabric declares its entrypoint in fabric.mod.json instead
// of annotating the class — AlexsMobsFabric is what news this up.
//? if !fabric
@Mod(AlexsMobs.MODID)
public class AlexsMobs {
    public static final Logger LOGGER = LogManager.getLogger();
    public static final String MODID = "alexsmobs";
    // NeoForge has no SimpleChannel — see AMNeoNetwork for the payload-based equivalent.
    //? if forge
    public static final SimpleChannel NETWORK_WRAPPER;
    private static final String PROTOCOL_VERSION = Integer.toString(1);
    // NeoForge 1.21 removed DistExecutor; FMLEnvironment.dist is the documented replacement. The
    // method reference is NOT decoration: a plain `dist.isClient() ? new ClientProxy() : …` makes
    // the verifier check ClientProxy against CommonProxy, which loads the class and trips
    // RuntimeDistCleaner on a dedicated server. Going through a Supplier keeps ClientProxy out of
    // every verified type — it only exists as a method handle the invokedynamic resolves when the
    // client branch actually runs. This is exactly the indirection DistExecutor used to provide.
    // FML loader 10 (the 1.21.9 line) made the `dist` field private behind getDist().
    // Forge 64 deleted DistExecutor as well; its FMLEnvironment.dist is still a public field.
    //? if forge && >=26 {
    /*public static final CommonProxy PROXY = net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()
            ? ((java.util.function.Supplier<CommonProxy>) ClientProxy::new).get()
            : new CommonProxy();
    *///?} elif neoforge && >=1.21.9 {
    /*public static final CommonProxy PROXY = net.neoforged.fml.loading.FMLEnvironment.getDist().isClient()
            ? ((java.util.function.Supplier<CommonProxy>) ClientProxy::new).get()
            : new CommonProxy();
    *///?} elif neoforge && >=1.21 {
    /*public static final CommonProxy PROXY = net.neoforged.fml.loading.FMLEnvironment.dist.isClient()
            ? ((java.util.function.Supplier<CommonProxy>) ClientProxy::new).get()
            : new CommonProxy();
    *///?} elif fabric {
    /*public static final CommonProxy PROXY = net.fabricmc.loader.api.FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.CLIENT
            ? ((java.util.function.Supplier<CommonProxy>) ClientProxy::new).get()
            : new CommonProxy();
    *///?} else {
    public static final CommonProxy PROXY = net.minecraftforge.fml.DistExecutor.runForDist(() -> ClientProxy::new, () -> CommonProxy::new);
    //?}
    private static int packetsRegistered;
    private static boolean isAprilFools = false;
    private static boolean isHalloween = false;

    //? if forge && >=1.20.2 {
    /*static {
        NETWORK_WRAPPER = ChannelBuilder.named(AMCompat.rl(MODID, "main_channel"))
                .networkProtocolVersion(Integer.parseInt(PROTOCOL_VERSION))
                .clientAcceptedVersions((status, version) -> version == Integer.parseInt(PROTOCOL_VERSION))
                .serverAcceptedVersions((status, version) -> version == Integer.parseInt(PROTOCOL_VERSION))
                .simpleChannel();
    }
    *///?}
    //? if forge && <1.20.2 {
    static {
        NetworkRegistry.ChannelBuilder channel = NetworkRegistry.ChannelBuilder.named(AMCompat.rl(MODID, "main_channel"));
        channel = channel.clientAcceptedVersions(PROTOCOL_VERSION::equals);
        NETWORK_WRAPPER = channel.serverAcceptedVersions(PROTOCOL_VERSION::equals).networkProtocolVersion(() -> PROTOCOL_VERSION).simpleChannel();
    }
    //?}

    // NeoForge 1.21 deleted FMLJavaModLoadingContext: a mod constructor is handed its own event
    // bus and ModContainer instead. Forge still discovers a no-arg constructor and serves the bus
    // through a thread-local context. The ModContainer parameter is unused — it was how config was
    // registered with the loader, which this mod no longer does (see ConfigHolder) — but it stays
    // because it is part of the constructor shape NeoForge looks for.
    // Forge 1.21.6's EventBus 7 replaced the mod's IEventBus with a BusGroup, which has no
    // addListener at all: listeners go through each event class's own static getBus(BusGroup)
    // (or IModBusEvent.getBus(group, Class) for the ones that only implement the marker).
    // DeferredRegister#register still takes the group directly, so the ~20 registry calls below
    // are unchanged — which is why `modBusEvent` keeps its name here.
    //? if neoforge && >=1.21 {
    /*public AlexsMobs(IEventBus modBusEvent, net.neoforged.fml.ModContainer modLoadingContext) {
    *///?} elif forge && >=1.21.6 {
    /*public AlexsMobs() {
        net.minecraftforge.eventbus.api.bus.BusGroup modBusEvent = net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModBusGroup();
    *///?} elif fabric {
    /*public AlexsMobs() {
        // Fabric has no mod bus and no ModLoadingContext. The token is empty (see fabric/ModBus);
        // it exists only so the ~20 DeferredRegister#register(modBusEvent) calls below — which are
        // the actual, immediate registration on this loader — stay identical across all three.
        final com.github.alexthe666.alexsmobs.fabric.ModBus modBusEvent = new com.github.alexthe666.alexsmobs.fabric.ModBus();
    *///?} else {
    public AlexsMobs() {
        IEventBus modBusEvent = net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();
    //?}
        //? if forge && >=1.21.6 {
        /*FMLCommonSetupEvent.getBus(modBusEvent).addListener(this::setup);
        FMLClientSetupEvent.getBus(modBusEvent).addListener(this::setupClient);
        *///?} elif fabric {
        /*// Nothing to subscribe: setup() is called straight from the tail of this constructor, the
        // client half from AlexsMobsFabricClient, and the config is already loaded and baked by
        // AlexsMobsFabric#onInitialize before it gets here.
        *///?} else {
        modBusEvent.addListener(this::setup);
        modBusEvent.addListener(this::setupClient);
        //?}
        // Forge 64 took the whole sealed EntityRenderersEvent hierarchy off the mod bus (it no
        // longer implements IModBusEvent) and gave each member a static BUS, so getBus(BusGroup)
        // is gone here as well — same shape as SpawnPlacementRegisterEvent at Forge 59. Split out
        // of the block above because Stonecutter blocks are siblings and never nest.
        //? if forge && >=26 {
        /*EntityRenderersEvent.RegisterLayerDefinitions.BUS.addListener(this::setupEntityModelLayers);
        *///?} elif forge && >=1.21.6 {
        /*EntityRenderersEvent.RegisterLayerDefinitions.getBus(modBusEvent).addListener(this::setupEntityModelLayers);
        *///?} elif fabric {
        /*// Model layers go through Fabric API's EntityModelLayerRegistry, from the client entrypoint.
        *///?} else {
        modBusEvent.addListener(this::setupEntityModelLayers);
        //?}
        // Forge 59 (1.21.9) moved these two off the mod bus onto plain default-bus EventBus
        // fields, so AMEntityRegistry can no longer carry an @EventBusSubscriber(bus = MOD) —
        // see the note on that class. They are the only two listeners it had.
        //? if forge && >=1.21.9 {
        /*net.minecraftforge.event.entity.SpawnPlacementRegisterEvent.BUS.addListener(AMEntityRegistry::onRegisterSpawnPlacements);
        net.minecraftforge.event.entity.EntityAttributeCreationEvent.BUS.addListener(AMEntityRegistry::initializeAttributes);
        *///?}
        //? if neoforge {
        /*modBusEvent.addListener(AMNeoNetwork::onRegisterPayloads);
        modBusEvent.addListener(com.github.alexthe666.alexsmobs.misc.AMItemHandlers::onRegisterCapabilities);
        *///?}
        // NeoForge >=1.21.5: the rebuilt entity-data serializers (Optional<UUID>, plus CompoundTag
        // from 1.21.9) must go through NeoForge's own registry rather than the vanilla static list.
        // See AMCompat.OPTIONAL_UUID.
        //? if neoforge && >=1.21.5
        /*com.github.alexthe666.alexsmobs.misc.AMCompat.DATA_SERIALIZER_DEF_REG.register(modBusEvent);*/
        // NeoForge 1.21.8 only: the Citadel entity-data store is a data attachment there, because
        // 21.8 rejects a mixin-added SynchedEntityData accessor. See AMCitadelDataAttachment.
        //? if neoforge && >=1.21.8
        /*com.github.alexthe666.alexsmobs.misc.AMCitadelDataAttachment.DEF_REG.register(modBusEvent);*/
        AMBlockRegistry.DEF_REG.register(modBusEvent);
        AMEntityRegistry.DEF_REG.register(modBusEvent);
        // Effects and sounds are flushed BEFORE items on purpose. On Fabric each of these lines is
        // the registration itself (fabric/registries/DeferredRegister runs the suppliers on the
        // spot), so a registry has to be filled before anything that dereferences it while being
        // constructed — the cosmic cod's food component resolves AMEffectRegistry.ENDER_FLU inside
        // the item's own supplier. Inert on Forge/NeoForge, where the loader picks the order and
        // these calls only subscribe.
        AMEffectRegistry.EFFECT_DEF_REG.register(modBusEvent);
        AMEffectRegistry.POTION_DEF_REG.register(modBusEvent);
        AMSoundRegistry.DEF_REG.register(modBusEvent);
        AMItemRegistry.DEF_REG.register(modBusEvent);
        AMTileEntityRegistry.DEF_REG.register(modBusEvent);
        AMPointOfInterestRegistry.DEF_REG.register(modBusEvent);
        AMFeatureRegistry.DEF_REG.register(modBusEvent);
        AMParticleRegistry.DEF_REG.register(modBusEvent);
        // 1.21 turned painting variants into datapack content too.
        //? if <1.21
        AMPaintingRegistry.DEF_REG.register(modBusEvent);
        // 1.21 turned enchantments into datapack content — nothing to register from code.
        //? if <1.21
        AMEnchantmentRegistry.DEF_REG.register(modBusEvent);
        AMMenuRegistry.DEF_REG.register(modBusEvent);
        AMRecipeRegistry.DEF_REG.register(modBusEvent);
        // Fabric's arm of AMLootRegistry has no DeferredRegister to register — see that class.
        //? if !fabric
        AMLootRegistry.DEF_REG.register(modBusEvent);
        //? if <1.20.5
        AMBannerRegistry.DEF_REG.register(modBusEvent);
        AMCreativeTabRegistry.DEF_REG.register(modBusEvent);
        // 1.20.2 made criterion triggers a registry, so they register on the mod bus now.
        //? if >=1.20.2
        //com.github.alexthe666.alexsmobs.misc.AMAdvancementTriggerRegistry.DEF_REG.register(modBusEvent);
        // The biome/structure modifier serializers are the one registry pair with NO Fabric
        // counterpart at all: Forge/NeoForge reach the modifiers through these registries plus
        // data/alexsmobs/{forge,neoforge}/biome_modifier/*.json, whereas Fabric's equivalent
        // (Fabric API's BiomeModifications) is a registration, not a datapack entry. So this is a
        // gate, not a redirect — the three modifier classes stay compiled but are never dispatched.
        // fabric/common/world/ModifiableBiomeInfo records what that costs and how to wire it up.
        //? if fabric {
        /*// nothing to register on this loader.
        *///?} elif >=1.20.5 {
        /*final DeferredRegister<com.mojang.serialization.MapCodec<? extends BiomeModifier>> biomeModifiers = DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, AlexsMobs.MODID);
        biomeModifiers.register(modBusEvent);
        AMMobSpawnBiomeModifier.SERIALIZER = biomeModifiers.register("am_mob_spawns", AMMobSpawnBiomeModifier::makeCodec);
        AMLeafcutterAntBiomeModifier.SERIALIZER = biomeModifiers.register("am_leafcutter_ant_spawns", AMLeafcutterAntBiomeModifier::makeCodec);
        final DeferredRegister<com.mojang.serialization.MapCodec<? extends StructureModifier>> structureModifiers = DeferredRegister.create(ForgeRegistries.Keys.STRUCTURE_MODIFIER_SERIALIZERS, AlexsMobs.MODID);
        structureModifiers.register(modBusEvent);
        AMMobSpawnStructureModifier.SERIALIZER = structureModifiers.register("am_structure_spawns", AMMobSpawnStructureModifier::makeCodec);
        *///?} else {
        final DeferredRegister<Codec<? extends BiomeModifier>> biomeModifiers = DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, AlexsMobs.MODID);
        biomeModifiers.register(modBusEvent);
        AMMobSpawnBiomeModifier.SERIALIZER = biomeModifiers.register("am_mob_spawns", AMMobSpawnBiomeModifier::makeCodec);
        AMLeafcutterAntBiomeModifier.SERIALIZER = biomeModifiers.register("am_leafcutter_ant_spawns", AMLeafcutterAntBiomeModifier::makeCodec);
        final DeferredRegister<Codec<? extends StructureModifier>> structureModifiers = DeferredRegister.create(ForgeRegistries.Keys.STRUCTURE_MODIFIER_SERIALIZERS, AlexsMobs.MODID);
        structureModifiers.register(modBusEvent);
        AMMobSpawnStructureModifier.SERIALIZER = structureModifiers.register("am_structure_spawns", AMMobSpawnStructureModifier::makeCodec);
        //?}
        // The config is this mod's own file (config/amc.json) on every loader, so it is read here
        // rather than registered with FML — AlexsMobsFabric#onInitialize does the same two calls
        // before it constructs this class, which is why they are gated off on that loader.
        //? if !fabric {
        ConfigHolder.load();
        BiomeConfig.init();
        //?}
        PROXY.init();
        // This class itself has no game-bus listeners left (its only @SubscribeEvent was the
        // mod-bus config event, wired with addListener above), so only ServerEvents registers.
        // ServerEvents is excluded from the Fabric compile entirely for now — see the deferred
        // event-behaviour work in docs/notes/fabric.md.
        //? if !fabric
        MinecraftForge.EVENT_BUS.register(new ServerEvents());
        // The two things Forge's mod bus fires after the entity registry, called straight through:
        // Fabric's registries are immediate, so "after" here is literally the next statement. Both
        // depend on the entity registry having been flushed above, which is the whole reason the
        // flush order lives in this constructor.
        //? if fabric {
        /*AMEntityRegistry.initializeAttributes(new com.github.alexthe666.alexsmobs.fabric.entity.EntityAttributeCreationEvent());
        AMEntityRegistry.registerSpawnPlacements();
        setup();
        *///?}
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        isAprilFools = calendar.get(Calendar.MONTH) + 1 == 4 && calendar.get(Calendar.DATE) == 1;
        isHalloween = calendar.get(Calendar.MONTH) + 1 == 10 && calendar.get(Calendar.DATE) >= 29 && calendar.get(Calendar.DATE) <= 31;
    }

    public static boolean isAprilFools() {
        return isAprilFools || AMConfig.superSecretSettings;
    }

    public static boolean isHalloween() {
        return isHalloween || AMConfig.superSecretSettings;
    }

    // A Forge/NeoForge mod-bus callback with no Fabric counterpart: model layers are registered
    // from AlexsMobsFabricClient through Fabric API's EntityModelLayerRegistry.
    //? if !fabric {
    private void setupEntityModelLayers(final EntityRenderersEvent.RegisterLayerDefinitions event) {
        AMModelLayers.register(event);
    }
    //?}

    public static <MSG> void sendMSGToServer(MSG message) {
        //? if forge && >=1.20.2 {
        /*NETWORK_WRAPPER.send(message, net.minecraftforge.network.PacketDistributor.SERVER.noArg());
        *///?}
        //? if forge && <1.20.2
        NETWORK_WRAPPER.sendToServer(message);
        //? if neoforge
        /*AMNeoNetwork.sendToServer(message);*/
        //? if fabric
        /*com.github.alexthe666.alexsmobs.fabric.network.AMFabricNetwork.sendToServer(message);*/
    }

    public static <MSG> void sendMSGToAll(MSG message) {
        for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            sendNonLocal(message, player);
        }
    }

    public static <MSG> void sendNonLocal(MSG msg, ServerPlayer player) {
        //? if forge && >=1.20.2 {
        /*// 1.20.2 moved the raw Connection behind the (protected) common listener; use the getter.
        NETWORK_WRAPPER.send(msg, player.connection.getConnection());
        *///?}
        //? if forge && <1.20.2
        NETWORK_WRAPPER.sendTo(msg, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
        //? if neoforge
        /*AMNeoNetwork.sendToPlayer(msg, player);*/
        //? if fabric
        /*com.github.alexthe666.alexsmobs.fabric.network.AMFabricNetwork.sendToPlayer(msg, player);*/
    }

    /**
     * Registers one message on {@link #NETWORK_WRAPPER}. Forge 1.20.2 replaced
     * {@code registerMessage} with a builder and changed the handler's context type, so both
     * the call shape and the {@link AMNetContext} adapter are version-gated here — the message
     * classes themselves stay identical across nodes.
     */
    private static <MSG> void registerMessage(Class<MSG> clazz, BiConsumer<MSG, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, MSG> decoder, BiConsumer<MSG, AMNetContext> handler) {
        //? if forge && >=1.20.2 {
        /*NETWORK_WRAPPER.messageBuilder(clazz, packetsRegistered++)
                .encoder(encoder)
                .decoder(decoder)
                .consumerMainThread((msg, ctx) -> handler.accept(msg, wrapContext(ctx)))
                .add();
        *///?}
        //? if forge && <1.20.2
        NETWORK_WRAPPER.registerMessage(packetsRegistered++, clazz, encoder, decoder, (msg, ctx) -> handler.accept(msg, wrapContext(ctx.get())));
        //? if neoforge
        /*AMNeoNetwork.register(clazz, encoder, decoder, handler);*/
        //? if fabric
        /*com.github.alexthe666.alexsmobs.fabric.network.AMFabricNetwork.register(clazz, encoder, decoder, handler);*/
    }

    //? if forge && >=1.20.2 {
    /*private static AMNetContext wrapContext(net.minecraftforge.event.network.CustomPayloadEvent.Context ctx) {
        return new AMNetContext() {
            public void setPacketHandled(boolean handled) { ctx.setPacketHandled(handled); }
            public void enqueueWork(Runnable work) { ctx.enqueueWork(work); }
            public ServerPlayer getSender() { return ctx.getSender(); }
            public boolean isClientSide() { return ctx.isClientSide(); }
        };
    }
    *///?}
    //? if forge && <1.20.2 {
    private static AMNetContext wrapContext(net.minecraftforge.network.NetworkEvent.Context ctx) {
        return new AMNetContext() {
            public void setPacketHandled(boolean handled) { ctx.setPacketHandled(handled); }
            public void enqueueWork(Runnable work) { ctx.enqueueWork(work); }
            public ServerPlayer getSender() { return ctx.getSender(); }
            public boolean isClientSide() { return ctx.getDirection().getReceptionSide() == net.minecraftforge.fml.LogicalSide.CLIENT; }
        };
    }
    //?}

    // Fabric has no common-setup event: this is called straight from the tail of the constructor,
    // which is already the "everything is registered" point on a loader whose registries are
    // immediate. The body is otherwise shared — only the two enqueueWork calls below differ.
    //? if fabric {
    /*private void setup() {
    *///?} else {
    private void setup(final FMLCommonSetupEvent event) {
    //?}
        registerMessage(MessageMosquitoMountPlayer.class, MessageMosquitoMountPlayer::write, MessageMosquitoMountPlayer::read, MessageMosquitoMountPlayer.Handler::handle);
        registerMessage(MessageMosquitoDismount.class, MessageMosquitoDismount::write, MessageMosquitoDismount::read, MessageMosquitoDismount.Handler::handle);
        registerMessage(MessageHurtMultipart.class, MessageHurtMultipart::write, MessageHurtMultipart::read, MessageHurtMultipart.Handler::handle);
        registerMessage(MessageCrowMountPlayer.class, MessageCrowMountPlayer::write, MessageCrowMountPlayer::read, MessageCrowMountPlayer.Handler::handle);
        registerMessage(MessageCrowDismount.class, MessageCrowDismount::write, MessageCrowDismount::read, MessageCrowDismount.Handler::handle);
        registerMessage(MessageMungusBiomeChange.class, MessageMungusBiomeChange::write, MessageMungusBiomeChange::read, MessageMungusBiomeChange.Handler::handle);
        registerMessage(MessageKangarooInventorySync.class, MessageKangarooInventorySync::write, MessageKangarooInventorySync::read, MessageKangarooInventorySync.Handler::handle);
        registerMessage(MessageKangarooEat.class, MessageKangarooEat::write, MessageKangarooEat::read, MessageKangarooEat.Handler::handle);
        registerMessage(MessageUpdateCapsid.class, MessageUpdateCapsid::write, MessageUpdateCapsid::read, MessageUpdateCapsid.Handler::handle);
        registerMessage(MessageSwingArm.class, MessageSwingArm::write, MessageSwingArm::read, MessageSwingArm.Handler::handle);
        registerMessage(MessageUpdateEagleControls.class, MessageUpdateEagleControls::write, MessageUpdateEagleControls::read, MessageUpdateEagleControls.Handler::handle);
        registerMessage(MessageSyncEntityPos.class, MessageSyncEntityPos::write, MessageSyncEntityPos::read, MessageSyncEntityPos.Handler::handle);
        registerMessage(MessageTarantulaHawkSting.class, MessageTarantulaHawkSting::write, MessageTarantulaHawkSting::read, MessageTarantulaHawkSting.Handler::handle);
        registerMessage(MessageStartDancing.class, MessageStartDancing::write, MessageStartDancing::read, MessageStartDancing.Handler::handle);
        registerMessage(MessageInteractMultipart.class, MessageInteractMultipart::write, MessageInteractMultipart::read, MessageInteractMultipart.Handler::handle);
        registerMessage(MessageSendVisualFlagFromServer.class, MessageSendVisualFlagFromServer::write, MessageSendVisualFlagFromServer::read, MessageSendVisualFlagFromServer.Handler::handle);
        registerMessage(MessageSetPupfishChunkOnClient.class, MessageSetPupfishChunkOnClient::write, MessageSetPupfishChunkOnClient::read, MessageSetPupfishChunkOnClient.Handler::handle);
        registerMessage(MessageUpdateTransmutablesToDisplay.class, MessageUpdateTransmutablesToDisplay::write, MessageUpdateTransmutablesToDisplay::read, MessageUpdateTransmutablesToDisplay.Handler::handle);
        registerMessage(MessageTransmuteFromMenu.class, MessageTransmuteFromMenu::write, MessageTransmuteFromMenu::read, MessageTransmuteFromMenu.Handler::handle);
        // Packets belonging to the bundled Citadel subset — Citadel used to register these on its own channel.
        registerMessage(PropertiesMessage.class, PropertiesMessage::write, PropertiesMessage::read, PropertiesMessage.Handler::handle);
        registerMessage(AnimationMessage.class, AnimationMessage::write, AnimationMessage::read, AnimationMessage.Handler::handle);
        // enqueueWork defers onto the loader's own synchronous-work queue; on Fabric there is no
        // such queue and no parallel mod loading to guard against, so these just run here.
        //? if fabric {
        /*AMItemRegistry.init();
        AMItemRegistry.initDispenser();
        *///?} else {
        event.enqueueWork(AMItemRegistry::init);
        event.enqueueWork(AMItemRegistry::initDispenser);
        //?}
        AMAdvancementTriggerRegistry.init();
        // 1.20.5 moved brewing to a per-server PotionBrewing.Builder — see ServerEvents.
        //? if <1.20.5
        AMEffectRegistry.init();
        AMRecipeRegistry.init();
        PROXY.initPathfinding();
    }

    // Fabric's client entrypoint (AlexsMobsFabricClient) calls PROXY.clientInit() itself — there is
    // no client-setup event to hang this off, and no work queue to enqueue onto.
    //? if !fabric {
    private void setupClient(FMLClientSetupEvent event) {
        event.enqueueWork(PROXY::clientInit);
    }
    //?}

}

package com.github.alexthe666.alexsmobs.client.event;

import com.github.alexthe666.alexsmobs.misc.AMCompat;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.ClientProxy;
import com.github.alexthe666.alexsmobs.client.model.ModelRockyChestplateRolling;
import com.github.alexthe666.alexsmobs.client.model.ModelWanderingVillagerRider;
import com.github.alexthe666.alexsmobs.client.model.layered.AMModelLayers;
import com.github.alexthe666.alexsmobs.client.render.AMItemstackRenderer;
import com.github.alexthe666.alexsmobs.client.render.AMRenderTypes;
// …and it is excluded from the Fabric source set outright (ModPlatformPlugin.configureJava): it
// subclasses the vanilla fluid renderer, which would need three more access-widener entries for a
// feature that is cosmetic and already gone on >=26. See the tail of accesswidener/.
//? if <26 && !fabric {
import com.github.alexthe666.alexsmobs.client.render.LavaVisionFluidRenderer;
//?}
import com.github.alexthe666.alexsmobs.client.render.RenderVineLasso;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.effect.AMEffectRegistry;
import com.github.alexthe666.alexsmobs.effect.EffectClinging;
import com.github.alexthe666.alexsmobs.effect.EffectPowerDown;
import com.github.alexthe666.alexsmobs.entity.EntityBaldEagle;
import com.github.alexthe666.alexsmobs.entity.EntityBlueJay;
import com.github.alexthe666.alexsmobs.entity.EntityElephant;
import com.github.alexthe666.alexsmobs.entity.IFalconry;
import com.github.alexthe666.alexsmobs.entity.util.Maths;
import com.github.alexthe666.alexsmobs.entity.util.RockyChestplateUtil;
import com.github.alexthe666.alexsmobs.entity.util.VineLassoUtil;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.github.alexthe666.alexsmobs.item.ItemDimensionalCarver;
import com.github.alexthe666.alexsmobs.message.MessageUpdateEagleControls;
import com.github.alexthe666.alexsmobs.misc.AMTagRegistry;
import com.github.alexthe666.alexsmobs.citadel.client.event.EventGetFluidRenderType;
import com.github.alexthe666.alexsmobs.citadel.client.event.EventGetOutlineColor;
import com.github.alexthe666.alexsmobs.citadel.client.event.EventGetStarBrightness;
import com.github.alexthe666.alexsmobs.citadel.client.event.EventPosePlayerHand;
import com.google.common.base.MoreObjects;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
//? if <26 && !fabric {
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
//?}
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
// Unused since the 1.21.4 in-hand-model wave; MC 26.1 deletes the class outright.
//? if <26 {
import net.minecraft.client.renderer.entity.ItemRenderer;
//?}
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.*;
// 1.20.5 replaced Forge's per-overlay HUD events with the vanilla LayeredDraw stack. Fabric never
// had the per-overlay events at all, so its HUD hook is a Fabric API callback in ClientProxy on
// every node and the whole RenderGuiOverlayEvent arm below is gated off with it.
//? if <1.20.5 && !fabric
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
// NeoForge 20.6 split TickEvent into net.neoforged.neoforge.event.tick.* / client.event and
// deleted Event.Result; both imports survive everywhere else. Fabric keeps the phase-tagged shape
// on every node — the split arms are all loader-gated, so it always lands on the else.
//? if forge || fabric || <1.20.6
import net.minecraftforge.event.TickEvent;
// …and Forge's own EventBus 7 (1.21.6) deleted api.Event outright; the one use here is
// Event.Result.DENY, fully-qualified to common.util.Result by the !fg2106-eb-result rule.
// `fabric` must track the Event.Result.DENY gate in onRenderNameplate exactly: on Fabric this
// import resolves to the shim at fabric/forge/eventbus/api/Event through the net.minecraftforge.**
// relocation, and Wave 3b-6 made that the live veto path on all seventeen Fabric nodes. Widening
// the *statement* without widening this *import* is what broke the first 3b-6 compile — the two
// conditions have to move together, and they are far enough apart in the file to forget.
//? if forge && >=1.21.6 {
/*
*///?} elif forge || fabric || <1.20.6 {
import net.minecraftforge.eventbus.api.Event;
//?}
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@OnlyIn(Dist.CLIENT)
public class ClientEvents {

    private static final ResourceLocation ROCKY_CHESTPLATE_TEXTURE = AMCompat.rl("alexsmobs:textures/armor/rocky_chestplate.png");
    private static final ModelRockyChestplateRolling ROCKY_CHESTPLATE_MODEL = new ModelRockyChestplateRolling();

    private boolean previousLavaVision = false;
    //? if <26 && !fabric {
    private LiquidBlockRenderer previousFluidRenderer;
    //?}
    public static long lastStaticTick = -1;
    public static int renderStaticScreenFor = 0;

    @SubscribeEvent
    public void onOutlineEntityColor(EventGetOutlineColor event) {
        if(event.getEntityIn() instanceof Enemy && AlexsMobs.PROXY.getSingingBlueJayId() != -1){
            Entity entity = event.getEntityIn().level().getEntity(AlexsMobs.PROXY.getSingingBlueJayId());
            if(entity instanceof EntityBlueJay jay && jay.isAlive() && jay.isMakingMonstersBlue()){
                event.setColor(0X4B95FE);
                event.setHandled(true);
            }
        }
        if (event.getEntityIn() instanceof ItemEntity && ((ItemEntity) event.getEntityIn()).getItem().is(AMTagRegistry.VOID_WORM_DROPS)){
            int fromColor = 0;
            int toColor = 0X21E5FF;
            float startR = (float) (fromColor >> 16 & 255) / 255.0F;
            float startG = (float) (fromColor >> 8 & 255) / 255.0F;
            float startB = (float) (fromColor & 255) / 255.0F;
            float endR = (float) (toColor >> 16 & 255) / 255.0F;
            float endG = (float) (toColor >> 8 & 255) / 255.0F;
            float endB = (float) (toColor & 255) / 255.0F;
            float f = (float) (Math.cos(0.4F * (event.getEntityIn().tickCount + Minecraft.getInstance().getFrameTime())) + 1.0F) * 0.5F;
            float r = (endR - startR) * f + startR;
            float g = (endG - startG) * f + startG;
            float b = (endB - startB) * f + startB;
            int j = ((((int) (r * 255)) & 0xFF) << 16) |
                    ((((int) (g * 255)) & 0xFF) << 8) |
                    ((((int) (b * 255)) & 0xFF) << 0);
            event.setColor(j);
            event.setHandled(true);
        }
    }

    @SubscribeEvent
    @OnlyIn(value = Dist.CLIENT)
    public void onGetStarBrightness(EventGetStarBrightness event) {
        if (Minecraft.getInstance().player.hasEffect(AMCompat.effect(AMEffectRegistry.POWER_DOWN.get()))) {
            if (Minecraft.getInstance().player.getEffect(AMCompat.effect(AMEffectRegistry.POWER_DOWN.get())) != null) {
                MobEffectInstance instance = Minecraft.getInstance().player.getEffect(AMCompat.effect(AMEffectRegistry.POWER_DOWN.get()));
                // Must go through AMCompat.rawEffect: from 1.20.5 getEffect() returns a
                // Holder<MobEffect>, and casting that straight to EffectPowerDown compiles (Holder
                // is an interface, EffectPowerDown is not final) but throws at runtime.
                EffectPowerDown powerDown = (EffectPowerDown) AMCompat.rawEffect(instance);
                int duration = instance.getDuration();
                float partialTicks = Minecraft.getInstance().getFrameTime();
                float f = (Math.min(powerDown.getActiveTime(), duration) + partialTicks) * 0.1F;
                event.setBrightness(0);
                event.setHandled(true);
            }

        }
    }

    @SubscribeEvent
    @OnlyIn(value = Dist.CLIENT)
    public void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (Minecraft.getInstance().player.hasEffect(AMCompat.effect(AMEffectRegistry.POWER_DOWN.get()))) {
            if (Minecraft.getInstance().player.getEffect(AMCompat.effect(AMEffectRegistry.POWER_DOWN.get())) != null) {
                event.setBlue(0);
                event.setRed(0);
                event.setGreen(0);
            }

        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    @OnlyIn(value = Dist.CLIENT)
    public void onFogDensity(ViewportEvent.RenderFog event) {
        FogType fogType = event.getCamera().getFluidInCamera();
        if (Minecraft.getInstance().player.hasEffect(AMCompat.effect(AMEffectRegistry.LAVA_VISION.get())) && fogType == FogType.LAVA) {
            event.setNearPlaneDistance(-8.0F);
            event.setFarPlaneDistance(50.0F);
            // 1.21.6's RenderFog is no longer cancellable — the setters write straight into the
            // FogData the shaders read, so the values apply without claiming the event.
            //? if <1.21.6
            event.setCanceled(true);
        }
        if (Minecraft.getInstance().player.hasEffect(AMCompat.effect(AMEffectRegistry.POWER_DOWN.get())) && fogType == FogType.NONE) {
            if (Minecraft.getInstance().player.getEffect(AMCompat.effect(AMEffectRegistry.POWER_DOWN.get())) != null) {
                float initEnd = event.getFarPlaneDistance();
                MobEffectInstance instance = Minecraft.getInstance().player.getEffect(AMCompat.effect(AMEffectRegistry.POWER_DOWN.get()));
                // Must go through AMCompat.rawEffect: from 1.20.5 getEffect() returns a
                // Holder<MobEffect>, and casting that straight to EffectPowerDown compiles (Holder
                // is an interface, EffectPowerDown is not final) but throws at runtime.
                EffectPowerDown powerDown = (EffectPowerDown) AMCompat.rawEffect(instance);
                int duration = instance.getDuration();
                float partialTicks = Minecraft.getInstance().getFrameTime();
                float f = Math.min(20, (Math.min(powerDown.getActiveTime() + partialTicks, duration + partialTicks))) * 0.05F;
                event.setNearPlaneDistance(-8.0F);
                float f1 = 8.0F + (1 - f) * Math.max(0, initEnd - 8.0F);
                event.setFarPlaneDistance(f1);
                //? if <1.21.6
                event.setCanceled(true);
            }

        }
    }

    // Cancelling does NOT end this handler — it takes over the render itself (the rolling rocky
    // chestplate), so EventBus 7's return-value cancellation is bridged rather than inlined.
    @SubscribeEvent
    @OnlyIn(value = Dist.CLIENT)
    //? if forge && >=1.21.6 {
    /*public boolean onPreRenderEntity(RenderLivingEvent.Pre event) {
        return AMCompat.cancelIf(() -> onPreRenderEntity0(event));
    }

    private void onPreRenderEntity0(RenderLivingEvent.Pre event) {
    *///?} else {
    public void onPreRenderEntity(RenderLivingEvent.Pre event) {
    //?}
        LivingEntity amEntity = renderedEntity(event);
        float amPartialTick = renderedPartialTick(event);
        if (amEntity == null) {
            return;
        }
        if (RockyChestplateUtil.isRockyRolling(amEntity)) {
            //? if forge && >=1.21.6 {
            /*AMCompat.cancelEvent();
            *///?} else {
            event.setCanceled(true);
            //?}
            event.getPoseStack().pushPose();
            float limbSwing = amEntity.walkAnimation.position() - amEntity.walkAnimation.speed() * (1.0F - amPartialTick);
            float limbSwingAmount = amEntity.walkAnimation.speed(renderedLight(event));
            float yRot = amEntity.yBodyRotO + (amEntity.yBodyRot - amEntity.yBodyRotO) * amPartialTick;
            //? if >=1.21.2 {
            /*// 1.21.2 deleted Entity#walkDist and its previous-tick twin. moveDist is the same
            // accumulated travel counter (it just also counts vertical movement, which a rolling
            // entity on the ground has none of), and the missing walkDistO is reconstructed from
            // the limb speed: moveDist grows by 0.6 x distance per tick, and WalkAnimationState's
            // speed is 4 x distance, so one tick of it is 0.15 x speed.
            float roll = amEntity.moveDist - amEntity.walkAnimation.speed() * 0.15F * (1.0F - amPartialTick);
            *///?} else {
            float roll = amEntity.walkDistO + (amEntity.walkDist - amEntity.walkDistO) * amPartialTick;
            //?}
            MultiBufferSource amBuffers = renderedBuffers(event);
            VertexConsumer vertexconsumer = com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.armorFoilBuffer(amBuffers, RenderType.armorCutoutNoCull(ROCKY_CHESTPLATE_TEXTURE), amEntity.getItemBySlot(EquipmentSlot.CHEST).hasFoil());
            event.getPoseStack().translate(0.0D, amEntity.getBbHeight() - amEntity.getBbHeight() * 0.5F, 0.0D);
            event.getPoseStack().mulPose(Axis.YN.rotationDegrees(180F + yRot));
            event.getPoseStack().mulPose(Axis.ZP.rotationDegrees(180.0F));
            event.getPoseStack().mulPose(Axis.XP.rotationDegrees(100F * roll));
            ROCKY_CHESTPLATE_MODEL.setupAnim(amEntity, limbSwing, limbSwingAmount, amEntity.tickCount + amPartialTick, 0, 0);
            ROCKY_CHESTPLATE_MODEL.renderToBuffer(event.getPoseStack(), vertexconsumer, renderedLight(event), OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            event.getPoseStack().popPose();
            flushBuffers(amBuffers);
            //? if forge && >=1.21.9 {
            /*// 1.21.9 swapped the Post record's buffer source + light for the collector and the
            // frame's camera state, both of which the Pre event carries verbatim.
            RenderLivingEvent.Post.BUS.post(new RenderLivingEvent.Post(event.getState(), event.getRenderer(), event.getPoseStack(), event.getNodeCollector(), event.getCameraState()));
            *///?} elif neoforge && >=1.21.9 {
            /*MinecraftForge.EVENT_BUS.post(new RenderLivingEvent.Post(event.getRenderState(), event.getRenderer(), event.getPartialTick(), event.getPoseStack(), event.getSubmitNodeCollector()));
            *///?} elif forge && >=1.21.6 {
            /*// …and EventBus 7 moved posting onto the event's own bus.
            RenderLivingEvent.Post.BUS.post(new RenderLivingEvent.Post(event.getState(), event.getRenderer(), event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight()));
            *///?} elif forge && >=1.21.2 {
            /*// The Post event lost its partial-tick argument along with the entity: it carries the
            // render state now, which already holds both.
            MinecraftForge.EVENT_BUS.post(new RenderLivingEvent.Post(event.getState(), event.getRenderer(), event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight()));
            *///?} elif neoforge && >=1.21.2 {
            /*MinecraftForge.EVENT_BUS.post(new RenderLivingEvent.Post(event.getRenderState(), event.getRenderer(), event.getPartialTick(), event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight()));
            *///?} elif fabric && >=1.21.2 {
            /*// Fabric's stand-in carries the whole union — entity, partial tick, buffers, light AND
            // the render state — so this is the else arm plus the state. Passing it matters: from
            // 1.21.2 flipUpsideDown mutates the state, and the Post handler must not be handed one
            // that is null.
            MinecraftForge.EVENT_BUS.post(new RenderLivingEvent.Post(amEntity, event.getRenderer(), amPartialTick, event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), event.getState()));
            *///?} else {
            MinecraftForge.EVENT_BUS.post(new RenderLivingEvent.Post(amEntity, event.getRenderer(), amPartialTick, event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight()));
            //?}
            return;
        }
        if (amEntity instanceof WanderingTrader && amEntity.getType() == EntityType.WANDERING_TRADER) {
            if (amEntity.getVehicle() instanceof EntityElephant) {
                //? if >=1.21.2 {
                /*// The renderer's model type is bound to its own render state from 1.21.2 on, so the
                // swap can only be expressed through the raw type. ModelWanderingVillagerRider still
                // extends VillagerModel, which is what this renderer's model field is declared as.
                net.minecraft.client.renderer.entity.LivingEntityRenderer rawRenderer = event.getRenderer();
                if (!(rawRenderer.getModel() instanceof ModelWanderingVillagerRider)) {
                    rawRenderer.model = new ModelWanderingVillagerRider(Minecraft.getInstance().getEntityModels().bakeLayer(AMModelLayers.SITTING_WANDERING_VILLAGER));
                }
                *///?} else {
                if (!(event.getRenderer().model instanceof ModelWanderingVillagerRider)) {
                    event.getRenderer().model = new ModelWanderingVillagerRider(Minecraft.getInstance().getEntityModels().bakeLayer(AMModelLayers.SITTING_WANDERING_VILLAGER));
                }
                //?}
            }
        }
        if (EffectClinging.isFlippedUpsideDown(amEntity) || amEntity.hasEffect(AMCompat.effect(AMEffectRegistry.DEBILITATING_STING.get())) && AMCompat.isArthropod(amEntity) && amEntity.getBbWidth() > amEntity.getBbHeight()) {
            event.getPoseStack().pushPose();
            event.getPoseStack().translate(0.0D, amEntity.getBbHeight() + 0.1F, 0.0D);
            event.getPoseStack().mulPose(Axis.ZP.rotationDegrees(180.0F));
            flipUpsideDown(event, amEntity);
        }
        if (amEntity.hasEffect(AMCompat.effect(AMEffectRegistry.ENDER_FLU.get()))) {
            event.getPoseStack().pushPose();
            event.getPoseStack().mulPose(Axis.YP.rotationDegrees((float) (Math.cos((double) amEntity.tickCount * 7F) * Math.PI * (double) 1.2F)));
            float vibrate = 0.05F;
            event.getPoseStack().translate((amEntity.getRandom().nextFloat() - 0.5F) * vibrate, (amEntity.getRandom().nextFloat() - 0.5F) * vibrate, (amEntity.getRandom().nextFloat() - 0.5F) * vibrate);
        }
    }

    /**
     * The entity a {@code RenderLivingEvent} is about. 1.21.2 replaced the event's entity with the
     * render state extracted from it; {@code AMStateAccess} carries the entity back across.
     */
    private static LivingEntity renderedEntity(RenderLivingEvent<?, ?> event) {
        //? if forge && >=1.21.2 {
        /*return com.github.alexthe666.alexsmobs.client.render.compat.AMStateAccess.entity(event.getState()) instanceof LivingEntity living ? living : null;
        *///?} elif neoforge && >=1.21.2 {
        /*return com.github.alexthe666.alexsmobs.client.render.compat.AMStateAccess.entity(event.getRenderState()) instanceof LivingEntity living ? living : null;
        *///?} else {
        return event.getEntity();
        //?}
    }

    /** Likewise for the frame's partial tick, which the event no longer carries either. */
    private static float renderedPartialTick(RenderLivingEvent<?, ?> event) {
        //? if forge && >=1.21.2 {
        /*return com.github.alexthe666.alexsmobs.client.render.compat.AMStateAccess.partialTick(event.getState());
        *///?} elif neoforge && >=1.21.2 {
        /*return com.github.alexthe666.alexsmobs.client.render.compat.AMStateAccess.partialTick(event.getRenderState());
        *///?} else {
        return event.getPartialTick();
        //?}
    }

    /**
     * The buffer source a {@code RenderLivingEvent} hands out. 1.21.9 replaced it with a
     * {@code SubmitNodeCollector}; {@code AMSubmitBuffers} records what the legacy body draws and
     * replays it through the collector, so the recorded instance has to be handed to
     * {@link #flushBuffers} once the handler is done with it.
     */
    private static MultiBufferSource renderedBuffers(RenderLivingEvent<?, ?> event) {
        //? if forge && >=1.21.9 {
        /*return new com.github.alexthe666.alexsmobs.client.render.compat.AMSubmitBuffers(event.getNodeCollector(), event.getCameraState());
        *///?} elif neoforge && >=1.21.9 {
        /*return new com.github.alexthe666.alexsmobs.client.render.compat.AMSubmitBuffers(event.getSubmitNodeCollector());
        *///?} else {
        return event.getMultiBufferSource();
        //?}
    }

    /**
     * Same for {@code RenderHandEvent}, which lost its buffer source to the collector too. It
     * carries no camera state on either loader, so {@code AMSubmitBuffers} rebuilds one if asked.
     */
    private static MultiBufferSource handBuffers(RenderHandEvent event) {
        //? if forge && >=1.21.9 {
        /*return new com.github.alexthe666.alexsmobs.client.render.compat.AMSubmitBuffers(event.getNodeCollector());
        *///?} elif neoforge && >=1.21.9 {
        /*return new com.github.alexthe666.alexsmobs.client.render.compat.AMSubmitBuffers(event.getSubmitNodeCollector());
        *///?} else {
        return event.getMultiBufferSource();
        //?}
    }

    /** Replays whatever {@link #renderedBuffers} recorded. A no-op below 1.21.9. */
    private static void flushBuffers(MultiBufferSource buffers) {
        //? if >=1.21.9 {
        /*com.github.alexthe666.alexsmobs.client.render.compat.AMSubmitBuffers submit =
                com.github.alexthe666.alexsmobs.client.render.compat.AMSubmitBuffers.of(buffers);
        if (submit != null) {
            submit.flush();
        }
        *///?}
    }

    /**
     * The packed light of the entity being rendered. 1.21.9 dropped it from the event — the render
     * state has carried it since 1.21.2, which is where the event read it from anyway.
     */
    private static int renderedLight(RenderLivingEvent<?, ?> event) {
        //? if forge && >=1.21.9 {
        /*return event.getState().lightCoords;
        *///?} elif neoforge && >=1.21.9 {
        /*return event.getRenderState().lightCoords;
        *///?} else {
        return event.getPackedLight();
        //?}
    }

    /**
     * Turns a clinging/stung entity upside down by negating its body and head yaw.
     *
     * <p>Up to 1.21.1 the model read those straight off the entity, so flipping the entity's own
     * fields in the Pre event (and back in Post) was enough. From 1.21.2 the model only sees the
     * render state, which was extracted before this event fires — so the state's copies are what
     * has to be flipped. The entity is left alone there; nothing else reads it this frame.
     */
    private static void flipUpsideDown(RenderLivingEvent<?, ?> event, LivingEntity entity) {
        //? if forge && >=1.21.2 {
        /*net.minecraft.client.renderer.entity.state.LivingEntityRenderState state = event.getState();
        state.bodyRot = -state.bodyRot;
        state.yRot = -state.yRot;
        *///?} elif neoforge && >=1.21.2 {
        /*net.minecraft.client.renderer.entity.state.LivingEntityRenderState state = event.getRenderState();
        state.bodyRot = -state.bodyRot;
        state.yRot = -state.yRot;
        *///?} elif fabric && >=1.21.2 {
        /*// Byte-identical to the forge arm above — the Fabric stub's accessor is named getState()
        // for exactly that reason, so the two cannot drift apart unnoticed.
        net.minecraft.client.renderer.entity.state.LivingEntityRenderState state = event.getState();
        state.bodyRot = -state.bodyRot;
        state.yRot = -state.yRot;
        *///?} else {
        entity.yBodyRotO = -entity.yBodyRotO;
        entity.yBodyRot = -entity.yBodyRot;
        entity.yHeadRotO = -entity.yHeadRotO;
        entity.yHeadRot = -entity.yHeadRot;
        //?}
    }

    @SubscribeEvent
    @OnlyIn(value = Dist.CLIENT)
    public void onPostRenderEntity(RenderLivingEvent.Post event) {
        LivingEntity amEntity = renderedEntity(event);
        float amPartialTick = renderedPartialTick(event);
        if (amEntity == null || RockyChestplateUtil.isRockyRolling(amEntity)) {
            return;
        }
        if (amEntity.hasEffect(AMCompat.effect(AMEffectRegistry.ENDER_FLU.get()))) {
            event.getPoseStack().popPose();
        }
        if (EffectClinging.isFlippedUpsideDown(amEntity) || amEntity.hasEffect(AMCompat.effect(AMEffectRegistry.DEBILITATING_STING.get())) && AMCompat.isArthropod(amEntity) && amEntity.getBbWidth() > amEntity.getBbHeight()) {
            event.getPoseStack().popPose();
            flipUpsideDown(event, amEntity);
        }
        if (VineLassoUtil.hasLassoData(amEntity) && !(amEntity instanceof Player)) {
            Entity lassoedOwner = VineLassoUtil.getLassoedTo(amEntity);
            if (lassoedOwner instanceof LivingEntity && lassoedOwner != amEntity) {
                double d0 = Mth.lerp(amPartialTick, amEntity.xOld, amEntity.getX());
                double d1 = Mth.lerp(amPartialTick, amEntity.yOld, amEntity.getY());
                double d2 = Mth.lerp(amPartialTick, amEntity.zOld, amEntity.getZ());
                event.getPoseStack().pushPose();
                event.getPoseStack().translate(-d0, -d1, -d2);
                MultiBufferSource amBuffers = renderedBuffers(event);
                RenderVineLasso.renderVine(amEntity, amPartialTick, event.getPoseStack(), amBuffers, (LivingEntity) lassoedOwner, ((LivingEntity) lassoedOwner).getMainArm() == HumanoidArm.LEFT, 0.1F);
                flushBuffers(amBuffers);
                event.getPoseStack().popPose();
            }
        }
    }

    @SubscribeEvent
    @OnlyIn(value = Dist.CLIENT)
    public void onPoseHand(EventPosePlayerHand event) {
        LivingEntity player = (LivingEntity) event.getEntityIn();
        float f = Minecraft.getInstance().getFrameTime();
        boolean leftHand = false;
        boolean usingLasso = player.isUsingItem() && player.getUseItem().is(AMItemRegistry.VINE_LASSO.get());
        if (player.getItemInHand(InteractionHand.MAIN_HAND).getItem() == AMItemRegistry.VINE_LASSO.get()) {
            leftHand = player.getMainArm() == HumanoidArm.LEFT;
        } else if (player.getItemInHand(InteractionHand.OFF_HAND).getItem() == AMItemRegistry.VINE_LASSO.get()) {
            leftHand = player.getMainArm() != HumanoidArm.LEFT;
        }
        if (leftHand && event.isLeftHand() && usingLasso) {
            //float swing = (float) Math.sin(player.tickCount + f) * 0.5F;
            event.setHandled(true);
            event.getModel().leftArm.xRot = Maths.rad(-120F) + Mth.sin(player.tickCount + f) * 0.5F;
            event.getModel().leftArm.yRot = Maths.rad(-20F) + Mth.cos(player.tickCount + f) * 0.5F;
        }
        if (!leftHand && !event.isLeftHand() && usingLasso) {
            event.setHandled(true);
            event.getModel().rightArm.xRot = Maths.rad(-120F) + Mth.sin(player.tickCount + f) * 0.5F;
            event.getModel().rightArm.yRot = Maths.rad(20F) - Mth.cos(player.tickCount + f) * 0.5F;
        }
    }

    @SubscribeEvent
    @OnlyIn(value = Dist.CLIENT)
    //? if forge && >=1.21.6 {
    /*public boolean onRenderHand(RenderHandEvent event) {
        return AMCompat.cancelIf(() -> onRenderHand0(event));
    }

    private void onRenderHand0(RenderHandEvent event) {
    *///?} else {
    public void onRenderHand(RenderHandEvent event) {
    //?}
        if (Minecraft.getInstance().getCameraEntity() instanceof IFalconry) {
            //? if forge && >=1.21.6 {
            /*AMCompat.cancelEvent();
            *///?} else {
            event.setCanceled(true);
            //?}
        }
        if (!Minecraft.getInstance().player.getPassengers().isEmpty() && event.getHand() == InteractionHand.MAIN_HAND) {
            Player player = Minecraft.getInstance().player;
            boolean leftHand = false;
            if (player.getItemInHand(InteractionHand.MAIN_HAND).getItem() == AMItemRegistry.FALCONRY_GLOVE.get()) {
                leftHand = player.getMainArm() == HumanoidArm.LEFT;
            } else if (player.getItemInHand(InteractionHand.OFF_HAND).getItem() == AMItemRegistry.FALCONRY_GLOVE.get()) {
                leftHand = player.getMainArm() != HumanoidArm.LEFT;
            }
            for (Entity entity : player.getPassengers()) {
                if (entity instanceof IFalconry falconry) {
                    float yaw = player.yBodyRotO + (player.yBodyRot - player.yBodyRotO) * event.getPartialTick();
                    ClientProxy.currentUnrenderedEntities.remove(entity.getUUID());
                    PoseStack matrixStackIn = event.getPoseStack();
                    matrixStackIn.pushPose();
                    matrixStackIn.scale(0.5F, 0.5F, 0.5F);
                    matrixStackIn.translate(leftHand ? -falconry.getHandOffset() : falconry.getHandOffset(), -0.6F, -1F);
                    matrixStackIn.mulPose(Axis.YP.rotationDegrees(yaw));
                    if (leftHand) {
                        matrixStackIn.mulPose(Axis.YP.rotationDegrees(90));
                    } else {
                        matrixStackIn.mulPose(Axis.YN.rotationDegrees(90));
                    }
                    MultiBufferSource amBuffers = handBuffers(event);
                    renderEntity(entity, 0, 0, 0, 0, event.getPartialTick(), matrixStackIn, amBuffers, event.getPackedLight());
                    flushBuffers(amBuffers);
                    matrixStackIn.popPose();
                    ClientProxy.currentUnrenderedEntities.add(entity.getUUID());
                }
            }
        }
        if (Minecraft.getInstance().player.getUseItem().getItem() instanceof ItemDimensionalCarver && event.getItemStack().getItem() instanceof ItemDimensionalCarver) {
            PoseStack matrixStackIn = event.getPoseStack();
            matrixStackIn.pushPose();
            ItemInHandRenderer renderer = Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer();
            InteractionHand hand = MoreObjects.firstNonNull(Minecraft.getInstance().player.swingingArm, InteractionHand.MAIN_HAND);
            float f = Minecraft.getInstance().player.getAttackAnim(event.getPartialTick());
            //float f1 = Mth.lerp(event.getPartialTick(), Minecraft.getInstance().player.xRotO, Minecraft.getInstance().player.getXRot());
            float f5 = -0.4F * Mth.sin(Mth.sqrt(f) * Mth.PI);
            float f6 = 0.2F * Mth.sin(Mth.sqrt(f) * Mth.TWO_PI);
            float f10 = -0.2F * Mth.sin(f * Mth.PI);
            HumanoidArm handside = hand == InteractionHand.MAIN_HAND ? Minecraft.getInstance().player.getMainArm() : Minecraft.getInstance().player.getMainArm().getOpposite();
            boolean flag3 = handside == HumanoidArm.RIGHT;
            int l = flag3 ? 1 : -1;
            matrixStackIn.translate((float) l * f5, f6, f10);
        }
    }

    public <E extends Entity> void renderEntity(E entityIn, double x, double y, double z, float yaw, float partialTicks, PoseStack matrixStack, MultiBufferSource bufferIn, int packedLight) {
        com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.renderEntity(entityIn, yaw, partialTicks, matrixStack, bufferIn, packedLight);
    }

    @SubscribeEvent
    @OnlyIn(value = Dist.CLIENT)
    // 1.21.2 made NeoForge's RenderNameTagEvent abstract: the veto moved to a CanRender subclass
    // that carries the entity via its render state and a TriState decision. Forge kept a single
    // concrete event (render state + setResult), so the two loaders need different handler shapes.
    // Forge 64 made RenderNameTagEvent Cancellable and removed setResult, so on EventBus 7 the
    // veto is the listener's own boolean return value.
    //? if forge && >=26 {
    /*public boolean onRenderNameplate(RenderNameTagEvent event) {
        Entity nameTagEntity = com.github.alexthe666.alexsmobs.client.render.compat.AMStateAccess.entity(event.getState());
        return (Minecraft.getInstance().getCameraEntity() instanceof EntityBaldEagle)
                && (nameTagEntity == Minecraft.getInstance().player)
                && Minecraft.getInstance().hasSingleplayerServer();
    }
    *///?} elif neoforge && >=1.21.2 {
    /*public void onRenderNameplate(RenderNameTagEvent.CanRender event) {
        Entity nameTagEntity = com.github.alexthe666.alexsmobs.client.render.compat.AMStateAccess.entity(event.getEntityRenderState());
        if ((Minecraft.getInstance().getCameraEntity() instanceof EntityBaldEagle)
                && (nameTagEntity == Minecraft.getInstance().player)
                && Minecraft.getInstance().hasSingleplayerServer()) {
            event.setCanRender(net.neoforged.neoforge.common.util.TriState.FALSE);
        }
    }
    *///?} else {
    public void onRenderNameplate(RenderNameTagEvent event) {
        // 1.21.2 replaced this event's entity with the render state it was extracted from.
        //? if forge && >=1.21.2 {
        /*Entity nameTagEntity = com.github.alexthe666.alexsmobs.client.render.compat.AMStateAccess.entity(event.getState());
        *///?} else {
        Entity nameTagEntity = event.getEntity();
        //?}
        // One reason to suppress a nameplate, kept as a single condition because the veto below is
        // spelled two different ways across the loaders and there must stay exactly ONE copy of
        // that pair — see the ⚠️ note on it.
        if ((Minecraft.getInstance().getCameraEntity() instanceof EntityBaldEagle)
                && (nameTagEntity == Minecraft.getInstance().player)
                && Minecraft.getInstance().hasSingleplayerServer()) {
            // NeoForge 20.6 swapped this event's Event.Result for a TriState.
            //? if neoforge && >=1.20.6
            //event.setCanRender(net.neoforged.neoforge.common.util.TriState.FALSE);
            // Fabric keeps the 1.20.1-shaped Result on the stub event at every MC version, so it
            // rides this arm rather than the TriState one above (which is a NeoForge type and must
            // not be reached here). Without `fabric` on this line a Fabric node at >=1.20.6 emits
            // NEITHER arm, leaving an empty guard body and making FabricNameTagMixin inert.
            // ⚠️ This condition is spelled identically to the `import ...eventbus.api.Event` gate
            // near the top of the file ON PURPOSE — emitting this line needs that import, so grep
            // the string and change both or neither.
            //? if forge || fabric || <1.20.6
            event.setResult(Event.Result.DENY);
        }
    }
    //?}

    // This mod registers no PLAYER-facing client command; `/aac nameplates` lived here from 2.0.2
    // to 2.0.4 and went away in 2.0.5 once the nameplate bug it papered over was fixed at the
    // source (docs/notes/client-settings.md). What is left is the development tool below.
    //
    // `/shieldpose` is a CLIENT command — it edits model JSON on disk and reloads resources, so it
    // needs nothing from the server. Forge and NeoForge both expose the registration as the same
    // game-bus event with the same getDispatcher() signature on every node from 1.20.1 to 26.2;
    // only the package differs, so the two arms are the same text twice. Fabric has no such event
    // and registers ClientCommandRegistrationCallback from FabricClientEvents instead — hence no
    // `else` arm here.
    //
    // The `available()` guard is what keeps this out of a shipped jar: it answers false unless the
    // game directory sits inside a checkout of this repo, so a player never sees the command in
    // tab-completion, and the registration is simply skipped.
    //? if forge {
    @SubscribeEvent
    @OnlyIn(value = Dist.CLIENT)
    public void onRegisterClientCommands(net.minecraftforge.client.event.RegisterClientCommandsEvent event) {
        if (com.github.alexthe666.alexsmobs.client.command.AMShieldPoseCommand.available()) {
            com.github.alexthe666.alexsmobs.client.command.AMShieldPoseCommand.register(event.getDispatcher(),
                    (source, message) -> source.sendSuccess(() -> message, false));
        }
    }
    //?} elif neoforge {
    /*@SubscribeEvent
    @OnlyIn(value = Dist.CLIENT)
    public void onRegisterClientCommands(net.neoforged.neoforge.client.event.RegisterClientCommandsEvent event) {
        if (com.github.alexthe666.alexsmobs.client.command.AMShieldPoseCommand.available()) {
            com.github.alexthe666.alexsmobs.client.command.AMShieldPoseCommand.register(event.getDispatcher(),
                    (source, message) -> source.sendSuccess(() -> message, false));
        }
    }
    *///?}

    // Forge 53.x (1.21.3) removed RenderLevelStageEvent with no replacement; this handler does
    // only per-frame state updates (no pose-stack rendering), so on Forge >=1.21.3 it rides the
    // surviving per-frame camera hook instead. NeoForge 21.6 kept the event but split its Stage
    // enum into one subclass per stage, so the stage guard becomes the subscribed type. Older
    // NeoForge and Forge <1.21.3 keep the single event with getStage().
    //? if forge && >=1.21.3 {
    /*@SubscribeEvent
    @OnlyIn(value = Dist.CLIENT)
    public void onRenderWorldLastEvent(net.minecraftforge.client.event.ViewportEvent.ComputeCameraAngles event) {
        doWorldLastFrame();
    }
    *///?}
    // Fabric never had RenderLevelStageEvent either, so it takes the same way out Forge >=1.21.3
    // did. The short name works here because the star import is redirected at the package level.
    //? if fabric {
    /*@SubscribeEvent
    @OnlyIn(value = Dist.CLIENT)
    public void onRenderWorldLastEvent(ViewportEvent.ComputeCameraAngles event) {
        doWorldLastFrame();
    }
    *///?}
    //? if neoforge && >=1.21.6 {
    /*@SubscribeEvent
    @OnlyIn(value = Dist.CLIENT)
    public void onRenderWorldLastEvent(RenderLevelStageEvent.AfterSky event) {
        doWorldLastFrame();
    }
    *///?}
    //? if (forge && <1.21.3) || (neoforge && <1.21.6) {
    @SubscribeEvent
    @OnlyIn(value = Dist.CLIENT)
    public void onRenderWorldLastEvent(RenderLevelStageEvent event) {
        if(event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY){
            doWorldLastFrame();
        }
    }
    //?}

    @OnlyIn(value = Dist.CLIENT)
    private void doWorldLastFrame() {
        {
            if (!AMConfig.shadersCompat) {
                // MC 26.1 renamed LiquidBlockRenderer -> FluidRenderer AND stopped keeping one on
                // the block renderer: each SectionCompiler constructs its own, so there is no
                // instance to swap out. The clear-lava effect of the Lava Vision potion is
                // therefore lost on >=26 (cosmetic only — the effect's other half, the fog
                // removal, still works). Same class of accepted regression as the 1.21.4 in-hand
                // model losses.
                //
                // Fabric loses the same half on EVERY node, for a different reason: the renderer
                // subclass is excluded from that source set (see the import), and swapping the
                // field would need access-widener entries for it and for ViewArea's sections.
                //? if <26 && !fabric {
                if (Minecraft.getInstance().player.hasEffect(AMCompat.effect(AMEffectRegistry.LAVA_VISION.get()))) {
                    if (!previousLavaVision) {
                        previousFluidRenderer = Minecraft.getInstance().getBlockRenderer().liquidBlockRenderer;
                        Minecraft.getInstance().getBlockRenderer().liquidBlockRenderer = new LavaVisionFluidRenderer();
                        updateAllChunks();
                    }
                } else {
                    if (previousLavaVision) {
                        if (previousFluidRenderer != null) {
                            Minecraft.getInstance().getBlockRenderer().liquidBlockRenderer = previousFluidRenderer;
                        }
                        updateAllChunks();
                    }
                }
                //?}
                previousLavaVision = Minecraft.getInstance().player.hasEffect(AMCompat.effect(AMEffectRegistry.LAVA_VISION.get()));
                // 1.21.2 reworked the post-processing pipeline: GameRenderer#loadEffect/currentEffect/
                // shutdownEffect are gone (a PostChain is now driven differently). The clinging screen-
                // flip is cosmetic; reimplementing it on the new pipeline is deferred, so it no-ops here.
                //? if <1.21.2 {
                if (AMConfig.clingingFlipEffect) {
                    if (EffectClinging.isFlippedUpsideDown(Minecraft.getInstance().player)) {
                        Minecraft.getInstance().gameRenderer.loadEffect(AMCompat.rl("shaders/post/flip.json"));
                    } else if (Minecraft.getInstance().gameRenderer.currentEffect() != null && Minecraft.getInstance().gameRenderer.currentEffect().getName().equals("minecraft:shaders/post/flip.json")) {
                        Minecraft.getInstance().gameRenderer.shutdownEffect();
                    }
                }
                //?}
            }
            if (Minecraft.getInstance().getCameraEntity() instanceof EntityBaldEagle) {
                EntityBaldEagle eagle = (EntityBaldEagle) Minecraft.getInstance().getCameraEntity();
                LocalPlayer playerEntity = Minecraft.getInstance().player;

                if (((EntityBaldEagle) Minecraft.getInstance().getCameraEntity()).shouldHoodedReturn() || eagle.isRemoved()) {
                    Minecraft.getInstance().setCameraEntity(playerEntity);
                    Minecraft.getInstance().options.setCameraType(CameraType.values()[AlexsMobs.PROXY.getPreviousPOV()]);
                } else {
                    float rotX = Mth.wrapDegrees(playerEntity.getYRot() + playerEntity.yHeadRot);
                    float rotY = playerEntity.getXRot();
                    Entity over = null;
                    if (Minecraft.getInstance().hitResult instanceof EntityHitResult) {
                        over = ((EntityHitResult) Minecraft.getInstance().hitResult).getEntity();
                    } else {
                        Minecraft.getInstance().hitResult = null;
                    }
                    boolean loadChunks = playerEntity.level().getDayTime() % 10 == 0;
                    ((EntityBaldEagle) Minecraft.getInstance().getCameraEntity()).directFromPlayer(rotX, rotY, false, over);
                    AlexsMobs.sendMSGToServer(new MessageUpdateEagleControls(Minecraft.getInstance().getCameraEntity().getId(), rotX, rotY, loadChunks, over == null ? -1 : over.getId()));
                }
            }
        }
    }

    // MC 26.2 made ViewArea#sections private and turned it from an array into a
    // RotatingSectionStorage, and nothing public marks every section dirty at once. There is no
    // need for one either: the only caller is the Lava Vision fluid-renderer swap, which is itself
    // gated out on >=26 (see the note at its call site), so on 26.2 this method is unreachable and
    // its body is simply empty.
    // The null guard is repeated inside each arm rather than wrapping them: Stonecutter blocks
    // never nest, and `!fabric` has to reach the guard too — ViewArea's section array is not in the
    // Fabric access widener, and adding it would buy nothing while the only caller is gated off.
    private void updateAllChunks() {
        //? if !fabric && >=1.20.2 && <26.2 {
        /*if (Minecraft.getInstance().levelRenderer.viewArea != null) {
            int length = Minecraft.getInstance().levelRenderer.viewArea.sections.length;
            for (int i = 0; i < length; i++) {
                Minecraft.getInstance().levelRenderer.viewArea.sections[i].setDirty(false);
            }
        }
        *///?}
        //? if !fabric && <1.20.2 {
        if (Minecraft.getInstance().levelRenderer.viewArea != null) {
            int length = Minecraft.getInstance().levelRenderer.viewArea.chunks.length;
            for (int i = 0; i < length; i++) {
                Minecraft.getInstance().levelRenderer.viewArea.chunks[i].dirty = true;
            }
        }
        //?}
    }

    @SubscribeEvent
    @OnlyIn(value = Dist.CLIENT)
    public void onGetFluidRenderType(EventGetFluidRenderType event) {
        if (Minecraft.getInstance().player.hasEffect(AMCompat.effect(AMEffectRegistry.LAVA_VISION.get())) && (event.getFluidState().is(Fluids.LAVA) || event.getFluidState().is(Fluids.FLOWING_LAVA))) {
            //? if >=1.21.6 {
            /*event.setRenderType(net.minecraft.client.renderer.chunk.ChunkSectionLayer.TRANSLUCENT);
            *///?} else {
            event.setRenderType(RenderType.translucent());
            //?}
            event.setHandled(true);
        }
    }

    // NeoForge 20.6 replaced the phase-tagged TickEvent with separate Pre/Post classes.
    //? if neoforge && >=1.20.6 {
    /*@SubscribeEvent
    public void clientTick(net.neoforged.neoforge.client.event.ClientTickEvent.Pre event) {
        AMItemstackRenderer.incrementTick();
        tickClinging();
    }
    *///?} elif forge && >=1.21.9 {
    /*// Forge 1.21.9 dissolved the phase-tagged TickEvent the same way NeoForge did: TickEvent is
    // a sealed interface now and each kind has its own Pre/Post record. START becomes Pre.
    @SubscribeEvent
    public void clientTick(TickEvent.ClientTickEvent.Pre event) {
        AMItemstackRenderer.incrementTick();
        tickClinging();
    }
    *///?} else {
    @SubscribeEvent
    public void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            AMItemstackRenderer.incrementTick();
            tickClinging();
        }
    }
    //?}

    /**
     * The Clinging effect's upward push, applied to the LOCAL player — see
     * docs/notes/bug-reports.md #82.
     *
     * <p><b>Why this exists at all.</b> Clinging is entirely a movement effect: everything it does
     * is {@code setDeltaMovement} inside {@code EffectClinging#applyEffectTick}. Through 1.21.1 that
     * ran on <i>both</i> sides — {@code MobEffectInstance#tick} was called from
     * {@code LivingEntity#tickEffects} on the client too — so the client player was pushed up and
     * clung. <b>1.21.2 made effect ticking server-only</b> (the method now takes a
     * {@code ServerLevel}, and {@code MobEffectInstance#tick} only calls it when the level is one),
     * and a player's movement is client-authoritative, so from there the push landed on the server
     * copy and was overwritten by the very next position packet. The effect did nothing at all for
     * a player on the 35 nodes >=1.21.2, all three loaders, since `2.0.0`.
     *
     * <p>Below 1.21.2 the effect still ticks client-side by itself, so this must NOT run there or
     * the push is applied twice. Mobs are unaffected either way — they are server-authoritative, so
     * {@code applyEffectTick} still moves them on every version.
     */
    private static void tickClinging() {
        //? if >=1.21.2 {
        /*LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.hasEffect(AMCompat.effect(AMEffectRegistry.CLINGING.get()))) {
            return;
        }
        // Mirrors EffectClinging#applyEffectTick exactly, minus refreshDimensions() — the eye
        // height it used to poke has been part of EntityDimensions since 1.20.2 and nothing bends
        // it any more (see EffectClinging#isFlippedUpsideDown).
        player.setNoGravity(false);
        if (EffectClinging.isUpsideDown(player)) {
            player.fallDistance = 0;
            if (!player.isShiftKeyDown()) {
                if (!player.horizontalCollision) {
                    player.setDeltaMovement(player.getDeltaMovement().add(0, 0.3F, 0));
                }
                player.setDeltaMovement(player.getDeltaMovement().multiply(0.998F, 1F, 0.998F));
            }
        }
        *///?}
    }

    // The Earthquake shake, and the one place in this file that does NOT ride
    // ViewportEvent.ComputeCameraAngles everywhere. From NeoForge 21.0 (MC 1.21) that event is
    // posted from INSIDE Camera#setup, on the line before setPosition(Mth.lerp(...)) recomputes the
    // position from the entity — so a Camera#move() from a handler is thrown away a statement later
    // and the screen never moves (#106). The event's yaw/pitch/roll survive, because setRotation is
    // called from the event's own values; only a positional nudge is lost, which is why nothing else
    // in this file noticed. On those 14 nodes mixin/client/CameraMixin calls the body below at TAIL
    // instead, and this handler is gated away rather than left to do discarded work.
    //? if !(neoforge && >=1.21) {
    @SubscribeEvent
    public void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        applyEarthquakeShake(event.getCamera());
    }
    //?}

    /** @see #onCameraSetup — extracted so the NeoForge {@code >=1.21} camera mixin can reach it. */
    public static void applyEarthquakeShake(net.minecraft.client.Camera camera) {
        if (Minecraft.getInstance().player == null) {
            return;
        }
        if (Minecraft.getInstance().player.getEffect(AMCompat.effect(AMEffectRegistry.EARTHQUAKE.get())) != null && !Minecraft.getInstance().isPaused()) {
            int duration = Minecraft.getInstance().player.getEffect(AMCompat.effect(AMEffectRegistry.EARTHQUAKE.get())).getDuration();
            float f = (Math.min(10, duration) + Minecraft.getInstance().getFrameTime()) * 0.1F;
            // Camera#move takes floats from 1.21 on; it was doubles before, which accepts these too.
            float intensity = f * Minecraft.getInstance().options.screenEffectScale().get().floatValue();
            RandomSource rng = Minecraft.getInstance().player.getRandom();
            camera.move(rng.nextFloat() * 0.1F * intensity, rng.nextFloat() * 0.2F * intensity, rng.nextFloat() * 0.4F * intensity);
        }
    }

    // 1.20.5 deleted RenderGuiOverlayEvent: the HUD became a vanilla LayeredDraw stack that
    // mods add their own layer to at mod-bus time (Forge: AddGuiOverlayLayersEvent, NeoForge:
    // RegisterGuiLayersEvent — both wired in ClientProxy#init). Either way the drawing itself
    // is the same, so it lives in renderStaticOverlay below, which every loader reaches by its own
    // route. The `!fabric` here is because Fabric has no RenderGuiOverlayEvent to subscribe to.
    //
    // ⚠️ Fabric's "own route" is FabricClientEvents#registerFarseerStatic — NOT anything in this
    // file and NOT ClientProxy, whose registration arms are all forge/neoforge (as is
    // mixin/client/GuiMixin, gated `forge && >=1.21 && <26`). It rides Fabric API's HudRenderCallback
    // below 26 and HudElementRegistry at/above, so on Fabric the static draws OVER the hotbar
    // rather than under it — the divergence is written up in docs/notes/fabric.md.
    // Until Wave 3b-2 wired that method the effect did not draw on Fabric at all, while this comment
    // claimed every loader had a route. If a loader is named here, check it before believing it.
    //? if <1.20.5 && !fabric {
    @SubscribeEvent
    public void onPostGameOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay().id().equals(VanillaGuiOverlay.HELMET.id())) {
            renderStaticOverlay(event.getGuiGraphics(), event.getPartialTick());
        }
    }
    //?}

    public static void renderStaticOverlay(net.minecraft.client.gui.GuiGraphics guiGraphics, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (renderStaticScreenFor <= 0 || mc.player == null || mc.level == null) {
            return;
        }
        if (mc.player.isAlive() && lastStaticTick != mc.level.getGameTime()) {
            renderStaticScreenFor--;
        }
        float staticLevel = (renderStaticScreenFor / 60F);
        //? if >=1.21.5 {
        /*// 1.21.5 removed the whole immediate-mode RenderSystem/BufferUploader path this used to draw
        // through. Reimplement the farseer static as a modern tinted GuiGraphics.blit: the scrolling
        // UV tiling is dropped (a cosmetic regression), but the full-screen static tint is preserved.
        int guiW = mc.getWindow().getGuiScaledWidth();
        int guiH = mc.getWindow().getGuiScaledHeight();
        int alpha = net.minecraft.util.Mth.clamp((int) (staticLevel * 255.0F), 0, 255);
        com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.blitTinted(guiGraphics, AMRenderTypes.STATIC_TEXTURE,
                0, 0, 0.0F, 0.0F, guiW, guiH, guiW, guiH, net.minecraft.util.ARGB.color(alpha, 255, 255, 255));
        lastStaticTick = mc.level.getGameTime();
        *///?} else {
        float screenWidth = mc.getWindow().getScreenWidth();
        float screenHeight = mc.getWindow().getScreenHeight();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        float ageInTicks = mc.level.getGameTime() + partialTick;
        float staticIndexX = (float) Math.sin(ageInTicks * 0.2F) * 2;
        float staticIndexY = (float) Math.cos(ageInTicks * 0.2F + 3F) * 2;
        RenderSystem.defaultBlendFunc();
        // 1.21.2 replaced the GameRenderer::get*Shader supplier refs with static ShaderProgram
        // handles in CoreShaders, and setShader now takes the program directly.
        //? if >=1.21.2 {
        /*RenderSystem.setShader(net.minecraft.client.renderer.CoreShaders.POSITION_TEX);
        *///?} else {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        //?}
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, staticLevel);
        RenderSystem.setShaderTexture(0, AMRenderTypes.STATIC_TEXTURE);
        Tesselator tesselator = Tesselator.getInstance();
        // 1.21 folded getBuilder()+begin() into Tesselator#begin, and a finished buffer is now an
        // explicit MeshData handed to BufferUploader rather than Tesselator#end drawing implicitly.
        //? if >=1.21 {
        /*BufferBuilder bufferbuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        *///?} else {
        BufferBuilder bufferbuilder = tesselator.getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        //?}
        float minU = 10 * staticIndexX * 0.125F;
        float maxU = 10 * (0.5F + staticIndexX * 0.125F);
        float minV = 10 * staticIndexY * 0.125F;
        float maxV = 10 * (0.125F + staticIndexY * 0.125F);
        bufferbuilder.vertex(0.0F, screenHeight, -190.0F).uv(minU, maxV).endVertex();
        bufferbuilder.vertex(screenWidth, screenHeight, -190.0F).uv(maxU, maxV).endVertex();
        bufferbuilder.vertex(screenWidth, 0.0F, -190.0F).uv(maxU, minV).endVertex();
        bufferbuilder.vertex(0.0F, 0.0F, -190.0F).uv(minU, minV).endVertex();
        //? if >=1.21 {
        /*BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
        *///?} else {
        tesselator.end();
        //?}
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        lastStaticTick = mc.level.getGameTime();
        //?}
    }
}

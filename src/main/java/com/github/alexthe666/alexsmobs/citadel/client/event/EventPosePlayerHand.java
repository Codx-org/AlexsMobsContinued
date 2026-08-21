package com.github.alexthe666.alexsmobs.citadel.client.event;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
// Forge's EventBus 7 (1.21.6) deleted the api.Event base class: an event now extends
// MutableEvent and is posted through its own static bus rather than MinecraftForge.EVENT_BUS.
// Fabric has no bus of any kind, so it takes the same shape against the mod's own AMEvent/AMEventBus.
//? if (forge && >=1.21.6) || fabric {
/*
*///?} else {
import net.minecraftforge.eventbus.api.Event;
//?}

@OnlyIn(Dist.CLIENT)
//? if forge && >=1.21.6 {
/*public class EventPosePlayerHand extends net.minecraftforge.eventbus.api.event.MutableEvent {
    public static final net.minecraftforge.eventbus.api.bus.EventBus<EventPosePlayerHand> BUS =
            net.minecraftforge.eventbus.api.bus.EventBus.create(EventPosePlayerHand.class);
*///?} elif fabric {
/*public class EventPosePlayerHand extends com.github.alexthe666.alexsmobs.fabric.event.AMEvent {
    public static final com.github.alexthe666.alexsmobs.fabric.event.AMEventBus<EventPosePlayerHand> BUS =
            com.github.alexthe666.alexsmobs.fabric.event.AMEventBus.create(EventPosePlayerHand.class);
*///?} else {
public class EventPosePlayerHand extends Event {
//?}
    private final LivingEntity entityIn;
    private final HumanoidModel model;
    private final boolean left;

    public EventPosePlayerHand(LivingEntity entityIn, HumanoidModel model, boolean left) {
        this.entityIn = entityIn;
        this.model = model;
        this.left = left;
    }

    public Entity getEntityIn() {
        return entityIn;
    }

    public HumanoidModel getModel() {
        return model;
    }

    public boolean isLeftHand() {
        return left;
    }

    // NeoForge 20.6 deleted Event.Result/@HasResult from the bus, and these four events are
    // fired and consumed entirely inside this mod, so they carry their own "a listener took
    // this over" flag instead of the bus one. Same behaviour on every loader/version.
    private boolean handled;

    public boolean isHandled() {
        return handled;
    }

    public void setHandled(boolean handled) {
        this.handled = handled;
    }

    // Where this event is fired from differs by loader AND version: EventBus 7 (Forge 1.21.6)
    // dropped MinecraftForge.EVENT_BUS.post entirely in favour of the event's own bus. Keeping the
    // choice here means the mixins/handlers that fire it stay one line on every node — which also
    // matters because a caller already inside a //? if block cannot nest another one.
    public void post() {
        //? if (forge && >=1.21.6) || fabric {
        /*BUS.post(this);
        *///?} else {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(this);
        //?}
    }
}

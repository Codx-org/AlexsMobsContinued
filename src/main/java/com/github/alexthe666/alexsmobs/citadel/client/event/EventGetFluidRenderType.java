package com.github.alexthe666.alexsmobs.citadel.client.event;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.material.FluidState;
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
/*public class EventGetFluidRenderType extends net.minecraftforge.eventbus.api.event.MutableEvent {
    public static final net.minecraftforge.eventbus.api.bus.EventBus<EventGetFluidRenderType> BUS =
            net.minecraftforge.eventbus.api.bus.EventBus.create(EventGetFluidRenderType.class);
*///?} elif fabric {
/*public class EventGetFluidRenderType extends com.github.alexthe666.alexsmobs.fabric.event.AMEvent {
    public static final com.github.alexthe666.alexsmobs.fabric.event.AMEventBus<EventGetFluidRenderType> BUS =
            com.github.alexthe666.alexsmobs.fabric.event.AMEventBus.create(EventGetFluidRenderType.class);
*///?} else {
public class EventGetFluidRenderType extends Event {
//?}
    private final FluidState fluidState;

    public FluidState getFluidState() {
        return fluidState;
    }

    // 1.21.6 replaced the per-fluid RenderType with the ChunkSectionLayer enum —
    // ItemBlockRenderTypes#getRenderLayer returns one of those now, so this event carries it.
    //? if >=1.21.6 {
    /*private net.minecraft.client.renderer.chunk.ChunkSectionLayer renderType;

    public EventGetFluidRenderType(FluidState fluidState, net.minecraft.client.renderer.chunk.ChunkSectionLayer renderType) {
        this.fluidState = fluidState;
        this.renderType = renderType;
    }

    public net.minecraft.client.renderer.chunk.ChunkSectionLayer getRenderType() {
        return renderType;
    }

    public void setRenderType(net.minecraft.client.renderer.chunk.ChunkSectionLayer renderType) {
        this.renderType = renderType;
    }
    *///?} else {
    private RenderType renderType;

    public EventGetFluidRenderType(FluidState fluidState, RenderType renderType) {
        this.fluidState = fluidState;
        this.renderType = renderType;
    }

    public RenderType getRenderType() {
        return renderType;
    }

    public void setRenderType(RenderType renderType) {
        this.renderType = renderType;
    }
    //?}

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

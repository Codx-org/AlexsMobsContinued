package com.github.alexthe666.alexsmobs.citadel.animation;

import net.minecraft.world.entity.Entity;
// EventBus 7 (Forge 1.21.6) has no api.Event and no @Cancelable: the base is MutableEvent,
// a parent with event subclasses declares InheritableEvent, and cancellability is the
// Cancellable marker interface — the same shape NeoForge has had since 1.20.6.
//? if forge && <1.21.6
import net.minecraftforge.eventbus.api.Cancelable;
//? if (forge && >=1.21.6) || fabric {
/*
*///?} else {
import net.minecraftforge.eventbus.api.Event;
//?}

//? if forge && >=1.21.6 {
/*public class AnimationEvent<T extends Entity & IAnimatedEntity> extends net.minecraftforge.eventbus.api.event.MutableEvent
        implements net.minecraftforge.eventbus.api.event.InheritableEvent {
*///?} elif fabric {
/*public class AnimationEvent<T extends Entity & IAnimatedEntity> extends com.github.alexthe666.alexsmobs.fabric.event.AMEvent {
*///?} else {
public class AnimationEvent<T extends Entity & IAnimatedEntity> extends Event {
//?}
    protected Animation animation;
    private final T entity;

    AnimationEvent(T entity, Animation animation) {
        this.entity = entity;
        this.animation = animation;
    }

    public T getEntity() {
        return this.entity;
    }

    public Animation getAnimation() {
        return this.animation;
    }

    // Cancellability moved from an annotation (Forge < 1.21.6) to a marker interface — NeoForge's
    // ICancellableEvent, and EventBus 7's Cancellable.
    //? if forge && <1.21.6
    @Cancelable
    public static class Start<T extends Entity & IAnimatedEntity> extends AnimationEvent<T>
            //? if neoforge
            /*implements net.neoforged.bus.api.ICancellableEvent*/
            //? if forge && >=1.21.6
            /*implements net.minecraftforge.eventbus.api.event.characteristic.Cancellable*/
    {
        //? if forge && >=1.21.6 {
        /*public static final net.minecraftforge.eventbus.api.bus.CancellableEventBus<Start> BUS =
                net.minecraftforge.eventbus.api.bus.CancellableEventBus.create(Start.class);

        *///?} elif fabric {
        /*public static final com.github.alexthe666.alexsmobs.fabric.event.AMEventBus<Start> BUS =
                com.github.alexthe666.alexsmobs.fabric.event.AMEventBus.create(Start.class);

        *///?}
        public Start(T entity, Animation animation) {
            super(entity, animation);
        }

        public void setAnimation(Animation animation) {
            this.animation = animation;
        }
    }

    public static class Tick<T extends Entity & IAnimatedEntity> extends AnimationEvent<T> {
        //? if forge && >=1.21.6 {
        /*public static final net.minecraftforge.eventbus.api.bus.EventBus<Tick> BUS =
                net.minecraftforge.eventbus.api.bus.EventBus.create(Tick.class);

        *///?} elif fabric {
        /*public static final com.github.alexthe666.alexsmobs.fabric.event.AMEventBus<Tick> BUS =
                com.github.alexthe666.alexsmobs.fabric.event.AMEventBus.create(Tick.class);

        *///?}
        protected int tick;

        public Tick(T entity, Animation animation, int tick) {
            super(entity, animation);
            this.tick = tick;
        }

        public int getTick() {
            return this.tick;
        }
    }
}
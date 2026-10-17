package com.github.alexthe666.alexsmobs.citadel.animation;

import com.github.alexthe666.alexsmobs.citadel.Citadel;
import com.github.alexthe666.alexsmobs.citadel.server.message.AnimationMessage;
import net.minecraft.world.entity.Entity;
// EventBus 7 and Fabric both post on the event's own bus; MinecraftForge.EVENT_BUS is neither
// imported nor reachable there.
//? if (forge && >=1.21.6) || fabric {
/*
*///?} else {
import net.minecraftforge.common.MinecraftForge;
//?}
import org.apache.commons.lang3.ArrayUtils;

/**
 * @author iLexiconn
 * @since 1.0.0
 */
public enum AnimationHandler {
    INSTANCE;

    /**
     * Sends an animation packet to all clients, notifying them of a changed animation
     *
     * @param entity    the entity with an animation to be updated
     * @param animation the animation to be updated
     * @param <T>       the entity type
     */
    public <T extends Entity & IAnimatedEntity> void sendAnimationMessage(T entity, Animation animation) {
        if (entity.level().isClientSide()) {
            return;
        }
        entity.setAnimation(animation);
        Citadel.sendMSGToAll(new AnimationMessage(entity.getId(), ArrayUtils.indexOf(entity.getAnimations(), animation)));
    }

    /**
     * Updates all animations for a given entity
     *
     * @param entity the entity with an animation to be updated
     * @param <T>    the entity type
     */
    public <T extends Entity & IAnimatedEntity> void updateAnimations(T entity) {
        if (entity.getAnimation() == null) {
            entity.setAnimation(IAnimatedEntity.NO_ANIMATION);
        } else {
            if (entity.getAnimation() != IAnimatedEntity.NO_ANIMATION) {
                if (entity.getAnimationTick() == 0) {
                    AnimationEvent.Start event = new AnimationEvent.Start<>(entity, entity.getAnimation());
                    // Forge's post() returns "was cancelled"; NeoForge's returns the event itself.
                    // EventBus 7 (Forge 1.21.6) went back to a boolean, but posts on the event's own
                    // bus — MinecraftForge.EVENT_BUS has no post() at all any more.
                    //? if (forge && >=1.21.6) || fabric {
                    /*if (!AnimationEvent.Start.BUS.post(event)) {
                    *///?} elif forge {
                    if (!MinecraftForge.EVENT_BUS.post(event)) {
                    //?} else {
                    /*if (!MinecraftForge.EVENT_BUS.post(event).isCanceled()) {
                    *///?}
                        this.sendAnimationMessage(entity, event.getAnimation());
                    }
                }
                if (entity.getAnimationTick() < entity.getAnimation().getDuration()) {
                    entity.setAnimationTick(entity.getAnimationTick() + 1);
                    //? if (forge && >=1.21.6) || fabric {
                    /*AnimationEvent.Tick.BUS.post(new AnimationEvent.Tick<>(entity, entity.getAnimation(), entity.getAnimationTick()));
                    *///?} else {
                    MinecraftForge.EVENT_BUS.post(new AnimationEvent.Tick<>(entity, entity.getAnimation(), entity.getAnimationTick()));
                    //?}
                }
                if (entity.getAnimationTick() == entity.getAnimation().getDuration()) {
                    entity.setAnimationTick(0);
                    entity.setAnimation(IAnimatedEntity.NO_ANIMATION);
                }
            }
        }
    }
}
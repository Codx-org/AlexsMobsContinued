package com.github.alexthe666.alexsmobs.fabric.forge.client.event;

import com.github.alexthe666.alexsmobs.fabric.forge.eventbus.api.Event;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Fabric stand-in for {@code net.minecraftforge.client.event.RenderLivingEvent} — the Pre/Post pair
 * {@code client/event/ClientEvents} wraps every living-entity render in (rocky-chestplate roll,
 * clinging flip, ender-flu shake, vine lasso, wandering-trader model swap).
 *
 * <p><b>This carries the union of every era's payload, not one era's.</b> Forge's own event lost
 * its entity and partial tick at 1.21.2 (replaced by the render state), then its buffer source and
 * packed light at 1.21.9 (replaced by a submit-node collector) — which is why {@code ClientEvents}
 * reads all six through the {@code rendered*} helpers. On a Fabric node <em>every</em> {@code forge
 * &&}/{@code neoforge &&} arm of those helpers is false, so the {@code else} arm — the plain 1.20.1
 * getters — is what runs on all seventeen nodes. Making this event able to answer them everywhere
 * is what keeps {@code ClientEvents} arm-free on Fabric: the Fabric mixin is only a <em>where</em>,
 * and the version differences live in what it passes to this constructor.
 *
 * <p>So on 1.21.9+ the mixin hands {@code multiBufferSource} an {@code AMSubmitBuffers} recording
 * into the frame's collector, and {@code packedLight} the render state's {@code lightCoords} — the
 * same two substitutions the Forge {@code >=1.21.9} arms make, just made one level down.
 *
 * <p>The render state is the exception that cannot be papered over: from 1.21.2 the model reads its
 * rotations off the state, not the entity, so {@code flipUpsideDown} has to mutate the state. It is
 * therefore carried <em>as well as</em> the entity, and only exists on nodes where the class does.
 *
 * <p>Type parameters are declared without bounds. Forge's are {@code <T extends LivingEntity, M
 * extends EntityModel<T>>}; naming {@code EntityModel} here would be a rule-5 hazard (it is one of
 * the five compat-shadowed names) and nothing needs the bound — the handlers take the raw types and
 * the helpers take {@code RenderLivingEvent<?, ?>}.
 *
 * <p>The <b>count</b> of them is not free, though: Forge added a third (the render state) at
 * 1.21.2, and the {@code !mc2102-renderlivingevent} replacement rule rewrites the helpers'
 * {@code RenderLivingEvent<?, ?>} to {@code <?, ?, ?>} on every loader — Fabric included, since
 * that rule is keyed on the MC version alone. So the arity has to be gated here to match. The
 * parameters stay unused either way.
 */
//? if >=1.21.2 {
/*public class RenderLivingEvent<T, M, S> extends Event {
*///?} else {
public class RenderLivingEvent<T, M> extends Event {
//?}

    private final LivingEntity entity;
    /**
     * Deliberately the <b>raw, fully-qualified</b> vanilla renderer. Fully-qualified because a
     * {@code import net.minecraft.client.renderer.entity.LivingEntityRenderer;} is rewritten to the
     * mod's compat class by a replacement rule; raw because the class gained a third type parameter
     * at 1.21.2 and because the one caller assigns into its {@code model} field through erasure.
     */
    private final net.minecraft.client.renderer.entity.LivingEntityRenderer renderer;
    private final float partialTick;
    private final PoseStack poseStack;
    private final MultiBufferSource multiBufferSource;
    private final int packedLight;
    //? if >=1.21.2 {
    /*private final net.minecraft.client.renderer.entity.state.LivingEntityRenderState state;
    *///?}

    protected RenderLivingEvent(LivingEntity entity,
                                net.minecraft.client.renderer.entity.LivingEntityRenderer renderer,
                                float partialTick,
                                PoseStack poseStack,
                                MultiBufferSource multiBufferSource,
                                int packedLight
            //? if >=1.21.2 {
            /*, net.minecraft.client.renderer.entity.state.LivingEntityRenderState state
            *///?}
    ) {
        this.entity = entity;
        this.renderer = renderer;
        this.partialTick = partialTick;
        this.poseStack = poseStack;
        this.multiBufferSource = multiBufferSource;
        this.packedLight = packedLight;
        //? if >=1.21.2 {
        /*this.state = state;
        *///?}
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public net.minecraft.client.renderer.entity.LivingEntityRenderer getRenderer() {
        return renderer;
    }

    public float getPartialTick() {
        return partialTick;
    }

    public PoseStack getPoseStack() {
        return poseStack;
    }

    public MultiBufferSource getMultiBufferSource() {
        return multiBufferSource;
    }

    public int getPackedLight() {
        return packedLight;
    }

    //? if >=1.21.2 {
    /*/^*
     * The render state the entity was extracted into this frame. Named {@code getState} to match
     * Forge's own accessor, so the {@code fabric && >=1.21.2} arms of {@code flipUpsideDown} and of
     * the rocky-chestplate repost are byte-identical to the {@code forge && >=1.21.2} ones above
     * them — two arms that read the same cannot drift apart unnoticed.
     ^/
    public net.minecraft.client.renderer.entity.state.LivingEntityRenderState getState() {
        return state;
    }
    *///?}

    /** Fires before the renderer draws. Cancelling it means the handler drew something instead. */
    //? if >=1.21.2 {
    /*public static class Pre<T, M, S> extends RenderLivingEvent<T, M, S> {
    *///?} else {
    public static class Pre<T, M> extends RenderLivingEvent<T, M> {
    //?}

        public Pre(LivingEntity entity,
                   net.minecraft.client.renderer.entity.LivingEntityRenderer renderer,
                   float partialTick,
                   PoseStack poseStack,
                   MultiBufferSource multiBufferSource,
                   int packedLight
                //? if >=1.21.2 {
                /*, net.minecraft.client.renderer.entity.state.LivingEntityRenderState state
                *///?}
        ) {
            super(entity, renderer, partialTick, poseStack, multiBufferSource, packedLight
                    //? if >=1.21.2 {
                    /*, state
                    *///?}
            );
        }
    }

    /**
     * Fires after. {@code ClientEvents} both listens to this and posts one itself — the rocky-roll
     * branch cancels the Pre and reposts a Post by hand so the pose stack it pushed gets popped.
     */
    //? if >=1.21.2 {
    /*public static class Post<T, M, S> extends RenderLivingEvent<T, M, S> {
    *///?} else {
    public static class Post<T, M> extends RenderLivingEvent<T, M> {
    //?}

        public Post(LivingEntity entity,
                    net.minecraft.client.renderer.entity.LivingEntityRenderer renderer,
                    float partialTick,
                    PoseStack poseStack,
                    MultiBufferSource multiBufferSource,
                    int packedLight
                //? if >=1.21.2 {
                /*, net.minecraft.client.renderer.entity.state.LivingEntityRenderState state
                *///?}
        ) {
            super(entity, renderer, partialTick, poseStack, multiBufferSource, packedLight
                    //? if >=1.21.2 {
                    /*, state
                    *///?}
            );
        }
    }
}

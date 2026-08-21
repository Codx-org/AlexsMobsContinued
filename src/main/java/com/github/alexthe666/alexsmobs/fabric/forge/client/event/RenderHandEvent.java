package com.github.alexthe666.alexsmobs.fabric.forge.client.event;

import com.github.alexthe666.alexsmobs.fabric.forge.eventbus.api.Event;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * Fabric stand-in for {@code net.minecraftforge.client.event.RenderHandEvent} — the first-person
 * arm/item pass. {@code client/event/ClientEvents} uses it for three things: suppressing the hands
 * entirely while riding a bald eagle's camera, drawing a perched falconry bird on the glove, and
 * nudging the dimensional carver during its swing.
 *
 * <p>Cancelling it means "the hands were not drawn"; the handler does exactly that for the eagle
 * POV, which is why the dispatcher has to read {@link #isCanceled()} back and skip vanilla's pass.
 *
 * <p>Like {@link RenderLivingEvent} the buffer source is carried unconditionally even though Forge
 * swapped it for a submit-node collector at 1.21.9 — {@code ClientEvents#handBuffers} falls through
 * to the plain getter on every Fabric node, and the mixin passes an {@code AMSubmitBuffers} there
 * on the nodes that need one.
 */
public class RenderHandEvent extends Event {

    private final InteractionHand hand;
    private final ItemStack itemStack;
    private final float partialTick;
    private final PoseStack poseStack;
    private final MultiBufferSource multiBufferSource;
    private final int packedLight;

    public RenderHandEvent(InteractionHand hand,
                           ItemStack itemStack,
                           float partialTick,
                           PoseStack poseStack,
                           MultiBufferSource multiBufferSource,
                           int packedLight) {
        this.hand = hand;
        this.itemStack = itemStack;
        this.partialTick = partialTick;
        this.poseStack = poseStack;
        this.multiBufferSource = multiBufferSource;
        this.packedLight = packedLight;
    }

    public InteractionHand getHand() {
        return hand;
    }

    public ItemStack getItemStack() {
        return itemStack;
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
}

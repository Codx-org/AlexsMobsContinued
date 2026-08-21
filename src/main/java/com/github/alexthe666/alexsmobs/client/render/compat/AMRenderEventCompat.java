package com.github.alexthe666.alexsmobs.client.render.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

/**
 * Loader-split shim for the Forge/NeoForge render events that the three fully-overridden mob
 * renderers ({@code RenderTiger}, {@code RenderUnderminer}, {@code RenderFarseer}) fire by hand.
 *
 * <p>1.21.2 diverged the two loaders here:
 * <ul>
 *   <li>{@code RenderLivingEvent.Pre}/{@code .Post} gained a {@code float partialTick} argument on
 *       NeoForge but not on Forge.</li>
 *   <li>{@code RenderNameTagEvent} stayed a plain, always-renders event on Forge (the content can
 *       be edited but not vetoed), while NeoForge made it abstract and moved the veto decision to a
 *       {@code CanRender} subclass carrying a {@link net.neoforged.neoforge.common.util.TriState}.</li>
 * </ul>
 *
 * <p>This class only exists on {@code >=1.21.2} — the whole {@code client/render/compat} package is
 * excluded from the compile below that (see {@code ModPlatformPlugin}).
 */
public class AMRenderEventCompat {

    /** Fire the pre-render living event; returns {@code true} if a listener cancelled rendering. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static boolean firePre(AMRenderState state, LivingEntityRenderer renderer, float partialTick, PoseStack pose, MultiBufferSource buffer, int light) {
        // EventBus 7 (Forge 1.21.6): MinecraftForge.EVENT_BUS lost post() — every event carries its
        // own bus, and a cancellable one's post() still reports "was cancelled".
        //
        // 1.21.9 swapped the packed light for the submit collector on both loaders (and Forge also
        // took the camera state), so unwrap the shim the caller was handed.
        //? if forge && >=1.21.9 {
        /*AMSubmitBuffers buffers = AMSubmitBuffers.of(buffer);
        if (buffers == null) {
            return false;
        }
        return net.minecraftforge.client.event.RenderLivingEvent.Pre.BUS.post(new net.minecraftforge.client.event.RenderLivingEvent.Pre(state, renderer, pose, buffers.collector(), buffers.camera()));
        *///?} elif forge && >=1.21.6 {
        /*return net.minecraftforge.client.event.RenderLivingEvent.Pre.BUS.post(new net.minecraftforge.client.event.RenderLivingEvent.Pre(state, renderer, pose, buffer, light));
        *///?} elif forge {
        /*return net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.RenderLivingEvent.Pre(state, renderer, pose, buffer, light));
        *///?} elif fabric {
        /*// Fabric has no equivalent of this event, so nothing can veto the render.
        return false;
        *///?} elif >=1.21.9 {
        /*AMSubmitBuffers buffers = AMSubmitBuffers.of(buffer);
        if (buffers == null) {
            return false;
        }
        net.neoforged.neoforge.client.event.RenderLivingEvent.Pre event = new net.neoforged.neoforge.client.event.RenderLivingEvent.Pre(state, renderer, partialTick, pose, buffers.collector());
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
        return event.isCanceled();
        *///?} else {
        net.neoforged.neoforge.client.event.RenderLivingEvent.Pre event = new net.neoforged.neoforge.client.event.RenderLivingEvent.Pre(state, renderer, partialTick, pose, buffer, light);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
        return event.isCanceled();
        //?}
    }

    /** Fire the post-render living event. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void firePost(AMRenderState state, LivingEntityRenderer renderer, float partialTick, PoseStack pose, MultiBufferSource buffer, int light) {
        //? if forge && >=1.21.9 {
        /*AMSubmitBuffers buffers = AMSubmitBuffers.of(buffer);
        if (buffers != null) {
            net.minecraftforge.client.event.RenderLivingEvent.Post.BUS.post(new net.minecraftforge.client.event.RenderLivingEvent.Post(state, renderer, pose, buffers.collector(), buffers.camera()));
        }
        *///?} elif forge && >=1.21.6 {
        /*net.minecraftforge.client.event.RenderLivingEvent.Post.BUS.post(new net.minecraftforge.client.event.RenderLivingEvent.Post(state, renderer, pose, buffer, light));
        *///?} elif forge {
        /*net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.RenderLivingEvent.Post(state, renderer, pose, buffer, light));
        *///?} elif fabric {
        /*// Fabric has no equivalent of this event; nothing to notify.
        *///?} elif >=1.21.9 {
        /*AMSubmitBuffers buffers = AMSubmitBuffers.of(buffer);
        if (buffers != null) {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.client.event.RenderLivingEvent.Post(state, renderer, partialTick, pose, buffers.collector()));
        }
        *///?} else {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.client.event.RenderLivingEvent.Post(state, renderer, partialTick, pose, buffer, light));
        //?}
    }

    /**
     * Fire the name-tag event and return the (possibly listener-modified) content to draw, or
     * {@code null} if the name should not be drawn this frame.
     */
    public static Component nameTagContent(Entity entity, Component displayName, net.minecraft.client.renderer.entity.EntityRenderer<?, ?> renderer, AMRenderState state, PoseStack pose, MultiBufferSource buffer, int light, float partialTick, boolean shouldShowName) {
        // 1.21.6 moved the bus's three-state Result onto common.util (HasResult), so the veto check
        // reads the same but off a different type. 1.21.9 then swapped the buffer + light for the
        // submit collector (Forge; plus the camera state) and moved NeoForge's TriState into vanilla.
        // Forge 64 dropped the displayName ctor argument (the event seeds its content from
        // state.nameTag) and replaced the three-state Result with plain cancellation, so there is
        // no force-allow any more: a listener can only veto, and !shouldShowName simply means no
        // name. setContent restores the content this mod computed, which state.nameTag may not
        // carry for the entities it renders by hand.
        //? if forge && >=26 {
        /*AMSubmitBuffers buffers = AMSubmitBuffers.of(buffer);
        if (buffers == null) {
            return shouldShowName ? displayName : null;
        }
        net.minecraftforge.client.event.RenderNameTagEvent event = new net.minecraftforge.client.event.RenderNameTagEvent(state, renderer, pose, buffers.collector(), buffers.camera());
        event.setContent(displayName);
        if (net.minecraftforge.client.event.RenderNameTagEvent.BUS.post(event) || !shouldShowName) {
            return null;
        }
        return event.getContent();
        *///?} elif forge && >=1.21.9 {
        /*AMSubmitBuffers buffers = AMSubmitBuffers.of(buffer);
        if (buffers == null) {
            return shouldShowName ? displayName : null;
        }
        net.minecraftforge.client.event.RenderNameTagEvent event = new net.minecraftforge.client.event.RenderNameTagEvent(state, displayName, renderer, pose, buffers.collector(), buffers.camera());
        net.minecraftforge.client.event.RenderNameTagEvent.BUS.post(event);
        net.minecraftforge.common.util.Result result = event.getResult();
        if (!result.isDenied() && (result.isAllowed() || shouldShowName)) {
            return event.getContent();
        }
        return null;
        *///?} elif forge && >=1.21.6 {
        /*net.minecraftforge.client.event.RenderNameTagEvent event = new net.minecraftforge.client.event.RenderNameTagEvent(state, displayName, renderer, pose, buffer, light);
        net.minecraftforge.client.event.RenderNameTagEvent.BUS.post(event);
        net.minecraftforge.common.util.Result result = event.getResult();
        if (!result.isDenied() && (result.isAllowed() || shouldShowName)) {
            return event.getContent();
        }
        return null;
        *///?} elif forge {
        /*net.minecraftforge.client.event.RenderNameTagEvent event = new net.minecraftforge.client.event.RenderNameTagEvent(state, displayName, renderer, pose, buffer, light);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
        net.minecraftforge.eventbus.api.Event.Result result = event.getResult();
        if (result != net.minecraftforge.eventbus.api.Event.Result.DENY && (result == net.minecraftforge.eventbus.api.Event.Result.ALLOW || shouldShowName)) {
            return event.getContent();
        }
        return null;
        *///?} elif fabric {
        /*// Fabric has no equivalent of this event, so the name is drawn exactly when vanilla says.
        return shouldShowName ? displayName : null;
        *///?} elif >=1.21.9 {
        /*net.neoforged.neoforge.client.event.RenderNameTagEvent.CanRender event = new net.neoforged.neoforge.client.event.RenderNameTagEvent.CanRender(entity, state, displayName, renderer, partialTick);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
        net.minecraft.util.TriState can = event.canRender();
        if (can.isTrue() || (can.isDefault() && shouldShowName)) {
            return event.getContent();
        }
        return null;
        *///?} else {
        net.neoforged.neoforge.client.event.RenderNameTagEvent.CanRender event = new net.neoforged.neoforge.client.event.RenderNameTagEvent.CanRender(entity, state, displayName, renderer, partialTick);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
        net.neoforged.neoforge.common.util.TriState can = event.canRender();
        if (can.isTrue() || (can.isDefault() && shouldShowName)) {
            return event.getContent();
        }
        return null;
        //?}
    }
}

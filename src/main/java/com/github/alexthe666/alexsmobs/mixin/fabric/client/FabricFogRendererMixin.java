package com.github.alexthe666.alexsmobs.mixin.fabric.client;

import com.github.alexthe666.alexsmobs.fabric.client.FabricClientEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Fires both of the fog hooks {@code ClientEvents} listens to:
 * {@code ViewportEvent.ComputeFogColor}, which the Power Down effect uses to black the fog out, and
 * {@code ViewportEvent.RenderFog}, which Lava Vision uses to see through lava and Power Down uses to
 * close the far plane in. One class because they are one vanilla class on every version — but two
 * independent sets of arms, because the colour and the planes do not move together.
 *
 * <p>The colour hook is documented immediately below; the plane hook has its own block further down.
 *
 * <p><b>This is the widest row in the wave: five arms, and no two boundaries agree.</b> Three
 * independent things move across 1.20.1 → 26.2, and picking the arms off any one of them alone
 * produces a mixin that loads fine and hooks nothing:
 * <ul>
 *   <li><b>The class moves at 1.21.6</b>, {@code client.renderer.FogRenderer} →
 *       {@code client.renderer.fog.FogRenderer}, which is why the {@code @Mixin} annotation itself is
 *       gated. It <i>moved</i>; it was never deleted, so a "does FogRenderer still exist" check says
 *       yes on every version and tells you nothing.</li>
 *   <li><b>The method changes shape three times</b>: {@code setupColor} writes three static fields
 *       and returns void up to 1.21.1; from 1.21.2 it is {@code computeFogColor} <i>returning</i> a
 *       {@code Vector4f}; 1.21.6 adds a trailing boolean and 1.21.11 drops it again — so 1.21.2 and
 *       1.21.11 share a descriptor while sharing nothing else. At 26 it goes back to void and fills
 *       a {@code Vector4f} <b>out-parameter</b> instead.</li>
 *   <li><b>It stops being static at 1.21.6.</b> The fog package rewrite made {@code FogRenderer} an
 *       object {@code LevelRenderer} owns. This is the one that does not show up in a descriptor
 *       diff at all, and javac cannot see it either: a static handler on an instance target (or the
 *       reverse) compiles clean and throws at mixin apply time. Checked against the bytecode with
 *       {@code scripts/}-side javap, same as the descriptors.</li>
 * </ul>
 *
 * <p><b>Why the shape of the write differs per arm.</b> Below 1.21.2 there is no return value to
 * modify, so the arm {@code @Shadow}s the three private statics and assigns them at TAIL — which is
 * in time for {@code levelFogColor()} to push them to the shader, the read that actually matters.
 * One knock-on: vanilla's own {@code RenderSystem.clearColor} call is the last statement of
 * {@code setupColor}, so on those versions the framebuffer <i>clear</i> colour keeps vanilla's value
 * where Forge's patch also overrides it. Under Power Down the fog is black either way; the
 * difference is confined to the sky beyond the far plane. It is not worth an {@code @ModifyArgs} on
 * a call site whose ordinal varies by version.
 */
//? if <1.21.6 {
@Mixin(net.minecraft.client.renderer.FogRenderer.class)
//?}
//? if >=1.21.6 {
/*@Mixin(net.minecraft.client.renderer.fog.FogRenderer.class)
*///?}
public abstract class FabricFogRendererMixin {

    //? if <1.21.2 {
    @Shadow
    private static float fogRed;
    @Shadow
    private static float fogGreen;
    @Shadow
    private static float fogBlue;

    @Inject(method = "setupColor(Lnet/minecraft/client/Camera;FLnet/minecraft/client/multiplayer/ClientLevel;IF)V",
            at = @At("TAIL"))
    private static void alexsmobs$computeFogColor(net.minecraft.client.Camera camera,
                                                  float partialTick,
                                                  net.minecraft.client.multiplayer.ClientLevel level,
                                                  int renderDistance,
                                                  float darkenWorldAmount,
                                                  CallbackInfo ci) {
        com.github.alexthe666.alexsmobs.fabric.forge.client.event.ViewportEvent.ComputeFogColor event =
                FabricClientEvents.fireComputeFogColor(camera, partialTick, fogRed, fogGreen, fogBlue);
        fogRed = event.getRed();
        fogGreen = event.getGreen();
        fogBlue = event.getBlue();
    }
    //?}

    //? if >=1.21.2 && <1.21.6 {
    /*@Inject(method = "computeFogColor(Lnet/minecraft/client/Camera;FLnet/minecraft/client/multiplayer/ClientLevel;IF)Lorg/joml/Vector4f;",
            at = @At("RETURN"))
    private static void alexsmobs$computeFogColor(net.minecraft.client.Camera camera,
                                                  float partialTick,
                                                  net.minecraft.client.multiplayer.ClientLevel level,
                                                  int renderDistance,
                                                  float darkenWorldAmount,
                                                  CallbackInfoReturnable<org.joml.Vector4f> cir) {
        alexsmobs$applyFogColor(camera, partialTick, cir.getReturnValue());
    }
    *///?}

    //? if >=1.21.6 && <1.21.11 {
    /*@Inject(method = "computeFogColor(Lnet/minecraft/client/Camera;FLnet/minecraft/client/multiplayer/ClientLevel;IFZ)Lorg/joml/Vector4f;",
            at = @At("RETURN"))
    private void alexsmobs$computeFogColor(net.minecraft.client.Camera camera,
                                           float partialTick,
                                           net.minecraft.client.multiplayer.ClientLevel level,
                                           int renderDistance,
                                           float darkenWorldAmount,
                                           boolean isFoggy,
                                           CallbackInfoReturnable<org.joml.Vector4f> cir) {
        alexsmobs$applyFogColor(camera, partialTick, cir.getReturnValue());
    }
    *///?}

    //? if >=1.21.11 && <26 {
    /*@Inject(method = "computeFogColor(Lnet/minecraft/client/Camera;FLnet/minecraft/client/multiplayer/ClientLevel;IF)Lorg/joml/Vector4f;",
            at = @At("RETURN"))
    private void alexsmobs$computeFogColor(net.minecraft.client.Camera camera,
                                           float partialTick,
                                           net.minecraft.client.multiplayer.ClientLevel level,
                                           int renderDistance,
                                           float darkenWorldAmount,
                                           CallbackInfoReturnable<org.joml.Vector4f> cir) {
        alexsmobs$applyFogColor(camera, partialTick, cir.getReturnValue());
    }
    *///?}

    //? if >=26 {
    /*// The colour is an out-parameter here, not a return value — vanilla fills the caller's vector
    // and returns void. TAIL is therefore the same moment RETURN is on the arms above.
    @Inject(method = "computeFogColor(Lnet/minecraft/client/Camera;FLnet/minecraft/client/multiplayer/ClientLevel;IFLorg/joml/Vector4f;)V",
            at = @At("TAIL"))
    private void alexsmobs$computeFogColor(net.minecraft.client.Camera camera,
                                           float partialTick,
                                           net.minecraft.client.multiplayer.ClientLevel level,
                                           int renderDistance,
                                           float darkenWorldAmount,
                                           org.joml.Vector4f target,
                                           CallbackInfo ci) {
        alexsmobs$applyFogColor(camera, partialTick, target);
    }
    *///?}

    //? if >=1.21.2 {
    /*// Shared by the four Vector4f arms. Alpha is deliberately untouched: the event carries only the
    // three colour channels, because the handler only ever writes those.
    @org.spongepowered.asm.mixin.Unique
    private static void alexsmobs$applyFogColor(net.minecraft.client.Camera camera, float partialTick,
                                                org.joml.Vector4f colour) {
        if (colour == null) {
            return;
        }
        com.github.alexthe666.alexsmobs.fabric.forge.client.event.ViewportEvent.ComputeFogColor event =
                FabricClientEvents.fireComputeFogColor(camera, partialTick, colour.x, colour.y, colour.z);
        colour.x = event.getRed();
        colour.y = event.getGreen();
        colour.z = event.getBlue();
    }
    *///?}

    // ══ ViewportEvent.RenderFog — the near/far planes ═════════════════════════════════════════
    //
    // Lava Vision (see through lava) and Power Down (fog closing in) both live here. Five arms
    // again, and they do NOT fall on the same boundaries as the colour arms above: the planes'
    // storage changes at 1.21.2 and 1.21.6 and 26, the enclosing method's descriptor at 1.21.6 and
    // 1.21.11.
    //
    // The near/far -> field mapping on 1.21.6+ is not a guess. NeoForge's own
    // ViewportEvent$RenderFog was read out of the merged jar: setNearPlaneDistance writes
    // FogData.environmentalStart and setFarPlaneDistance writes FogData.environmentalEnd. Since the
    // shared handler already runs against that mapping on NeoForge, reproducing it is the whole job
    // — the renderDistance*/sky/cloud fields are deliberately left alone.
    //
    // No arm captures a local or relies on surplus handler parameters, because a wrong injector
    // SIGNATURE (as opposed to a wrong target) fails at mixin-apply time on the client, which no
    // gate step in this tree can reach. Where the injector does not already have the camera in
    // hand it asks GameRenderer#getMainCamera() — which exists on exactly the versions that need
    // it, 1.21.6 through 1.21.11, and is gone by 26 where the camera is an argument again.
    //
    // Cancellation needs no special handling on any arm: every one of them applies the mod's values
    // after vanilla has computed its own, which is what the handler's `setCanceled(true)` means
    // here. That call is itself gated `<1.21.6` in the shared source.

    //? if <1.21.2 {
    // The planes are shader state, not a value in flight — vanilla has already pushed them by TAIL,
    // so this reads them back out and re-pushes. RenderSystem is the only place they exist.
    @Inject(method = "setupFog(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/FogRenderer$FogMode;FZF)V",
            at = @At("TAIL"))
    private static void alexsmobs$renderFog(net.minecraft.client.Camera camera,
                                            net.minecraft.client.renderer.FogRenderer.FogMode fogMode,
                                            float renderDistance,
                                            boolean isFoggy,
                                            float partialTick,
                                            CallbackInfo ci) {
        com.github.alexthe666.alexsmobs.fabric.forge.client.event.ViewportEvent.RenderFog event =
                FabricClientEvents.fireRenderFog(camera,
                        com.mojang.blaze3d.systems.RenderSystem.getShaderFogStart(),
                        com.mojang.blaze3d.systems.RenderSystem.getShaderFogEnd());
        com.mojang.blaze3d.systems.RenderSystem.setShaderFogStart(event.getNearPlaneDistance());
        com.mojang.blaze3d.systems.RenderSystem.setShaderFogEnd(event.getFarPlaneDistance());
    }
    //?}

    //? if >=1.21.2 && <1.21.6 {
    /*// FogParameters is a RECORD, so there is nothing to mutate — the return value is rebuilt. Only
    // start and end change; shape and the four colour components are carried through untouched.
    @Inject(method = "setupFog(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/FogRenderer$FogMode;Lorg/joml/Vector4f;FZF)Lnet/minecraft/client/renderer/FogParameters;",
            at = @At("RETURN"), cancellable = true)
    private static void alexsmobs$renderFog(net.minecraft.client.Camera camera,
                                            net.minecraft.client.renderer.FogRenderer.FogMode fogMode,
                                            org.joml.Vector4f colour,
                                            float renderDistance,
                                            boolean isFoggy,
                                            float partialTick,
                                            CallbackInfoReturnable<net.minecraft.client.renderer.FogParameters> cir) {
        net.minecraft.client.renderer.FogParameters params = cir.getReturnValue();
        if (params == null) {
            return;
        }
        com.github.alexthe666.alexsmobs.fabric.forge.client.event.ViewportEvent.RenderFog event =
                FabricClientEvents.fireRenderFog(camera, params.start(), params.end());
        if (event.getNearPlaneDistance() != params.start() || event.getFarPlaneDistance() != params.end()) {
            cir.setReturnValue(new net.minecraft.client.renderer.FogParameters(
                    event.getNearPlaneDistance(), event.getFarPlaneDistance(), params.shape(),
                    params.red(), params.green(), params.blue(), params.alpha()));
        }
    }
    *///?}

    //? if >=1.21.6 && <1.21.11 {
    /*@ModifyArgs(method = "setupFog(Lnet/minecraft/client/Camera;IZLnet/minecraft/client/DeltaTracker;FLnet/minecraft/client/multiplayer/ClientLevel;)Lorg/joml/Vector4f;",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/fog/FogRenderer;updateBuffer(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"))
    private void alexsmobs$renderFog(Args args) {
        alexsmobs$applyFogPlanes(args);
    }
    *///?}

    //? if >=1.21.11 && <26 {
    /*@ModifyArgs(method = "setupFog(Lnet/minecraft/client/Camera;ILnet/minecraft/client/DeltaTracker;FLnet/minecraft/client/multiplayer/ClientLevel;)Lorg/joml/Vector4f;",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/fog/FogRenderer;updateBuffer(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"))
    private void alexsmobs$renderFog(Args args) {
        alexsmobs$applyFogPlanes(args);
    }
    *///?}

    //? if >=1.21.6 && <26 {
    /*// The six trailing floats of updateBuffer are the FogData fields, passed positionally. Order
    // read straight off the getfields that feed the call: 3 environmentalStart, 4 environmentalEnd,
    // 5/6 renderDistanceStart/End, 7 skyEnd, 8 cloudEnd. Only 3 and 4 are the event's planes.
    //
    // The handler takes a bare Args and nothing else — the canonical @ModifyArgs shape — which is
    // why the camera comes from the game renderer here rather than from the enclosing call.
    @org.spongepowered.asm.mixin.Unique
    private static void alexsmobs$applyFogPlanes(Args args) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.gameRenderer == null) {
            return;
        }
        net.minecraft.client.Camera camera = mc.gameRenderer.getMainCamera();
        if (camera == null) {
            return;
        }
        float near = args.<Float>get(3);
        float far = args.<Float>get(4);
        com.github.alexthe666.alexsmobs.fabric.forge.client.event.ViewportEvent.RenderFog event =
                FabricClientEvents.fireRenderFog(camera, near, far);
        args.set(3, event.getNearPlaneDistance());
        args.set(4, event.getFarPlaneDistance());
    }
    *///?}

    //? if >=26 {
    /*// setupFog hands the FogData back to its caller, which writes it to the uniform buffer only
    // afterwards — verified there is no updateBuffer call left inside setupFog at 26 — so mutating
    // the returned object at RETURN is still in time.
    @Inject(method = "setupFog(Lnet/minecraft/client/Camera;ILnet/minecraft/client/DeltaTracker;FLnet/minecraft/client/multiplayer/ClientLevel;)Lnet/minecraft/client/renderer/fog/FogData;",
            at = @At("RETURN"))
    private void alexsmobs$renderFog(net.minecraft.client.Camera camera,
                                     int renderDistance,
                                     net.minecraft.client.DeltaTracker deltaTracker,
                                     float darkenWorldAmount,
                                     net.minecraft.client.multiplayer.ClientLevel level,
                                     CallbackInfoReturnable<net.minecraft.client.renderer.fog.FogData> cir) {
        net.minecraft.client.renderer.fog.FogData data = cir.getReturnValue();
        if (data == null) {
            return;
        }
        com.github.alexthe666.alexsmobs.fabric.forge.client.event.ViewportEvent.RenderFog event =
                FabricClientEvents.fireRenderFog(camera, data.environmentalStart, data.environmentalEnd);
        data.environmentalStart = event.getNearPlaneDistance();
        data.environmentalEnd = event.getFarPlaneDistance();
    }
    *///?}
}

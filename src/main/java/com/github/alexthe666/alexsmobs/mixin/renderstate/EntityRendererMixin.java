package com.github.alexthe666.alexsmobs.mixin.renderstate;

import com.github.alexthe666.alexsmobs.client.render.compat.AMStateAccess;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Records the entity a render state was extracted from — see {@link AMStateAccess}.
 *
 * <p>{@code EntityRenderer#extractRenderState} is the single choke point: every subclass override
 * calls up to it, and it is reached exactly once per entity per frame from
 * {@code EntityRenderer#createRenderState(Entity, float)}.
 *
 * <p>⚠️ <b>HEAD, not TAIL.</b> {@code extractRenderState} calls {@code extractNameTags} partway
 * through its own body, and that is where the loader posts {@code RenderNameTagEvent.CanRender} —
 * so a TAIL capture happens <i>after</i> the one listener that needs it. The state is freshly
 * allocated per entity per frame, so {@link AMStateAccess#entity} handed the nameplate hook
 * {@code null} every time and {@code /aac nameplates} silently did nothing on every node from
 * 1.21.2 up (shipped that way in {@code 2.0.2}). Nothing else reads the duck before extraction
 * finishes, so capturing at HEAD is strictly earlier and safe for every other caller.
 */
// ⚠️ The target is spelled out FULLY QUALIFIED and this file must NEVER
// `import net.minecraft.client.renderer.entity.EntityRenderer;` — the `!mc2102-render-import-entity`
// replacement in stonecutter.gradle.kts rewrites exactly that statement to
// client.render.compat.EntityRenderer on every >=1.21.2 node. It does not know this is a mixin, so the
// import retargeted @Mixin at the mod's own compat class, whose extractRenderState takes an
// AMRenderState — a descriptor mismatch, i.e. a hard mixin-apply crash on all nine >=1.21.2 nodes.
// It compiles clean either way: @Mixin accepts any class and a handler's parameters are only checked at
// apply time. The same trap waits for LivingEntityRenderer, MobRenderer, RenderLayer and EntityModel.
@Mixin(net.minecraft.client.renderer.entity.EntityRenderer.class)
public abstract class EntityRendererMixin {

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void alexsmobs$captureEntity(Entity entity, EntityRenderState state, float partialTick, CallbackInfo ci) {
        ((AMStateAccess) state).alexsmobs$capture(entity, partialTick);
    }
}

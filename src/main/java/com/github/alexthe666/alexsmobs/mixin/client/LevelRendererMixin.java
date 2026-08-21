package com.github.alexthe666.alexsmobs.mixin.client;

import com.github.alexthe666.alexsmobs.citadel.client.event.EventGetOutlineColor;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Fires {@link EventGetOutlineColor} — Alex's Mobs tints the glowing outline of blue jays and
 * the void worm. Vendored from Citadel (LGPL-3.0-only).
 */
// ⚠️ On >=1.21.9 the host is EntityRenderer, spelled out FULLY QUALIFIED — this file must NEVER
// `import net.minecraft.client.renderer.entity.EntityRenderer;`, because the
// `!mc2102-render-import-entity` replacement rewrites exactly that statement to the mod's own
// client.render.compat.EntityRenderer and would silently retarget the mixin. See the identical
// note on mixin/renderstate/EntityRendererMixin.
//? if >=1.21.9 {
/*@Mixin(net.minecraft.client.renderer.entity.EntityRenderer.class)
*///?} else {
@Mixin(LevelRenderer.class)
//?}
public class LevelRendererMixin {

    // Selected by method NAME ONLY, deliberately. A descriptor-pinned selector matched 1.20.1 alone
    // — renderLevel's parameter list changed in almost every version since — and a @Redirect whose
    // selector matches nothing is a hard mixin-apply CRASH (InvalidInjectionException), not a silent
    // no-op. Neither host method is overloaded in any supported version, so the bare name is
    // unambiguous and immune to further signature drift. The host itself did move: Entity#getTeamColor
    // is called from LevelRenderer#renderLevel up to 1.21.1 and from #renderEntities from 1.21.2 on.
    // 1.21.9 moved the outline colour out of the level renderer entirely: it is now baked into the
    // render state by EntityRenderer#extractRenderState, which is a different class, so the @Mixin
    // target above moves with it. extractRenderState is not overloaded either.
    //? if >=1.21.9 {
    /*@Redirect(
            method = "extractRenderState",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getTeamColor()I")
    )
    *///?} elif >=1.21.2 {
    /*@Redirect(
            method = "renderEntities",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getTeamColor()I")
    )
    *///?} else {
    @Redirect(
            method = "renderLevel",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getTeamColor()I")
    )
    //?}
    private int alexsmobs_getOutlineColor(Entity entity) {
        EventGetOutlineColor event = new EventGetOutlineColor(entity, entity.getTeamColor());
        event.post();
        int color = entity.getTeamColor();
        if (event.isHandled()) {
            color = event.getColor();
        }
        return color;
    }
}

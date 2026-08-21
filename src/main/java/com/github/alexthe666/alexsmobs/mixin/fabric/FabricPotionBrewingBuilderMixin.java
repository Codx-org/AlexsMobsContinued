package com.github.alexthe666.alexsmobs.mixin.fabric;

import net.minecraft.world.item.alchemy.PotionBrewing;
import org.spongepowered.asm.mixin.Mixin;
//? if >=1.20.5 {
/*import com.github.alexthe666.alexsmobs.effect.AMEffectRegistry;
import com.github.alexthe666.alexsmobs.fabric.common.brewing.FabricBrewing;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
*///?}

/**
 * Refills {@link com.github.alexthe666.alexsmobs.fabric.common.brewing.FabricBrewing} each time the
 * server builds its brewing tables — the Fabric stand-in for the {@code RegisterBrewingRecipesEvent}
 * that {@code ServerEvents} handles on the other two loaders.
 *
 * <p>Not Fabric API's {@code FabricPotionBrewingBuilder.BUILD}, deliberately: that interface only
 * exists from 26.1.2 on (before it the same event lived on {@code FabricBrewingRecipeRegistryBuilder},
 * and on 1.20.1/1.20.4 it does not exist at all), whereas {@code PotionBrewing.Builder.build()} has
 * carried the same descriptor since 1.20.6. Nothing is lost by not using the API hook — the mod's
 * recipes cannot go through the builder anyway, which is the whole reason {@code FabricBrewing}
 * exists.
 *
 * <p><b>Below 1.20.5 this mixin does nothing and targets {@code PotionBrewing} only so the class has
 * a target that exists.</b> There was no per-server builder then; brewing is a set of global static
 * tables, and the mod registers into {@code FabricBrewing} once from
 * {@code AlexsMobs}'s {@code AMEffectRegistry.init()} instead.
 */
//? if >=1.20.5 {
/*@Mixin(targets = "net.minecraft.world.item.alchemy.PotionBrewing$Builder")
*///?} else {
@Mixin(PotionBrewing.class)
//?}
public class FabricPotionBrewingBuilderMixin {

    //? if >=1.20.5 {
    /*@Inject(method = "build()Lnet/minecraft/world/item/alchemy/PotionBrewing;", at = @At("HEAD"))
    private void alexsmobs$collectRecipes(CallbackInfoReturnable<PotionBrewing> cir) {
        // reset() first: build() runs once for PotionBrewing.EMPTY and again for every server, and
        // the recipe list is global, so without it the list grows a copy per world load.
        FabricBrewing.reset();
        AMEffectRegistry.registerBrewingRecipes((PotionBrewing.Builder) (Object) this);
    }
    *///?}
}

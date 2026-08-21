package com.github.alexthe666.alexsmobs.mixin.fabric;

import com.github.alexthe666.alexsmobs.fabric.common.brewing.FabricBrewing;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets the brewing stand's three bottom slots accept this mod's non-potion inputs — lava_bottle,
 * poison_bottle and komodo_spit_bottle. Vanilla's {@code PotionSlot.mayPlaceItem} hardcodes
 * potion/splash/lingering/glass_bottle, so without this the recipes that start from one of those
 * three could never be set up in the GUI at all.
 *
 * <p>{@code targets = }, because {@code BrewingStandMenu$PotionSlot} is package-private and cannot
 * be named from here. The static {@code mayPlaceItem(ItemStack)Z} is byte-for-byte the same
 * declaration on all seventeen Fabric nodes, so there are no arms; the instance {@code mayPlace}
 * that calls it is a one-liner and is left alone, which keeps this working if a later version
 * inlines it.
 *
 * <p>Forge instead patches the instance {@code mayPlace}. Same effect from the player's side; the
 * static method is the better seam here because it is the one whose signature has never moved.
 */
@Mixin(targets = "net.minecraft.world.inventory.BrewingStandMenu$PotionSlot")
public class FabricBrewingStandMenuMixin {

    @Inject(method = "mayPlaceItem(Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At("RETURN"), cancellable = true)
    private static void alexsmobs$mayPlaceInput(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && FabricBrewing.isInput(stack)) {
            cir.setReturnValue(true);
        }
    }
}

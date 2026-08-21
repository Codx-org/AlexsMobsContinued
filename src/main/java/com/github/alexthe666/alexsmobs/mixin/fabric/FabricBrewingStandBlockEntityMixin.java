package com.github.alexthe666.alexsmobs.mixin.fabric;

import com.github.alexthe666.alexsmobs.fabric.common.brewing.FabricBrewing;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The hopper half of {@link FabricBrewingStandMenuMixin}. {@code canPlaceItem} does not delegate to
 * {@code PotionSlot.mayPlaceItem} — it inlines the same four-item check, on every version from
 * 1.20.1 to 26.2 — so accepting this mod's non-potion inputs has to be done twice, once per copy.
 * {@code canPlaceItemThroughFace} just forwards here, so automation is covered too.
 *
 * <p>Forge patches this method as well, for the same reason.
 *
 * <p>The {@code slot < 3} guard keeps this off the ingredient (3) and fuel (4) slots, and the
 * emptiness check reproduces the condition vanilla applies to the bottle slots — a brewing stand
 * bottle slot only ever takes an item when it is empty.
 */
@Mixin(BrewingStandBlockEntity.class)
public class FabricBrewingStandBlockEntityMixin {

    @Inject(method = "canPlaceItem(ILnet/minecraft/world/item/ItemStack;)Z",
            at = @At("RETURN"), cancellable = true)
    private void alexsmobs$canPlaceInput(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && slot < 3 && FabricBrewing.isInput(stack)
                && ((BrewingStandBlockEntity) (Object) this).getItem(slot).isEmpty()) {
            cir.setReturnValue(true);
        }
    }
}

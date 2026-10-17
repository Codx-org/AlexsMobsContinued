package com.github.alexthe666.alexsmobs.mixin.fabric;

import com.github.alexthe666.alexsmobs.fabric.common.brewing.FabricBrewing;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes the seventeen recipes collected by {@link FabricBrewing} actually brew, by answering for
 * them in the three vanilla methods every brewing-stand code path funnels through.
 *
 * <p>Disassembling {@code BrewingStandBlockEntity} on both eras shows the funnel is exactly three
 * methods and no more: {@code isBrewable} asks {@code isIngredient} then {@code hasMix},
 * {@code doBrew} asks {@code mix}, and {@code canPlaceItem} asks {@code isIngredient} again. The
 * fourth thing Forge patches — "may this stack go in a bottom slot" — is <i>not</i> on this class;
 * see {@link FabricBrewingStandMenuMixin} and {@link FabricBrewingStandBlockEntityMixin}.
 *
 * <p><b>The two argument orders disagree, and vanilla is the one that is inconsistent.</b>
 * {@code hasMix(input, ingredient)} takes the bottle first, {@code mix(ingredient, input)} takes the
 * reagent first — confirmed from the bytecode of {@code isBrewable} and {@code doBrew} on 1.20.1 and
 * 1.21.5 alike. {@link FabricBrewing#mix} follows Forge's order (input first), so the {@code mix}
 * handler below swaps its parameters and the {@code hasMix} one does not.
 *
 * <p>The era split is static-versus-instance, nothing more: 1.20.5 turned the global brewing tables
 * into a per-server object, so the same three methods stopped being static. A Mixin handler has to
 * match its target's staticness, hence two arms with identical bodies.
 */
@Mixin(PotionBrewing.class)
public class FabricPotionBrewingMixin {

    //? if <1.20.5 {
    @Inject(method = "isIngredient(Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At("RETURN"), cancellable = true)
    private static void alexsmobs$isIngredient(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && FabricBrewing.isIngredient(stack)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "hasMix(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At("RETURN"), cancellable = true)
    private static void alexsmobs$hasMix(ItemStack input, ItemStack ingredient,
                                         CallbackInfoReturnable<Boolean> cir) {
        // Vanilla bails out of hasMix before looking at anything when the bottle is not one of its
        // own three potion items, which is precisely the case for lava_bottle, poison_bottle and
        // komodo_spit_bottle — so this has to be a RETURN override, not a tail-end refinement.
        if (!cir.getReturnValueZ() && !FabricBrewing.mix(input, ingredient).isEmpty()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mix(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("RETURN"), cancellable = true)
    private static void alexsmobs$mix(ItemStack ingredient, ItemStack input,
                                      CallbackInfoReturnable<ItemStack> cir) {
        ItemStack output = FabricBrewing.mix(input, ingredient);
        if (!output.isEmpty()) {
            cir.setReturnValue(output);
        }
    }
    //?} else {
    /*@Inject(method = "isIngredient(Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At("RETURN"), cancellable = true)
    private void alexsmobs$isIngredient(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && FabricBrewing.isIngredient(stack)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "hasMix(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At("RETURN"), cancellable = true)
    private void alexsmobs$hasMix(ItemStack input, ItemStack ingredient,
                                  CallbackInfoReturnable<Boolean> cir) {
        // Vanilla bails out of hasMix before looking at anything when the bottle is not one of its
        // own three potion items, which is precisely the case for lava_bottle, poison_bottle and
        // komodo_spit_bottle — so this has to be a RETURN override, not a tail-end refinement.
        if (!cir.getReturnValueZ() && !FabricBrewing.mix(input, ingredient).isEmpty()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mix(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("RETURN"), cancellable = true)
    private void alexsmobs$mix(ItemStack ingredient, ItemStack input,
                               CallbackInfoReturnable<ItemStack> cir) {
        ItemStack output = FabricBrewing.mix(input, ingredient);
        if (!output.isEmpty()) {
            cir.setReturnValue(output);
        }
    }
    *///?}
}

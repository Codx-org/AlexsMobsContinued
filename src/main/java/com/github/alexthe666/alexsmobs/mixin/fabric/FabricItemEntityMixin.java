package com.github.alexthe666.alexsmobs.mixin.fabric;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The per-tick dropped-item hook. Fabric-only — see {@link FabricLivingEntityMixin} for why this
 * package is excluded and pruned on the other loaders.
 *
 * <p>{@code IItemExtension#onEntityItemUpdate} is a Forge-family extension with no Fabric equivalent
 * and, in this tree, no caller at all outside Forge's own patch — so the one item that implements it,
 * {@link com.github.alexthe666.alexsmobs.item.ItemMysteriousWorm}, never fired on any of the 17 Fabric
 * nodes and the void worm could not be summoned there at all (#93). Forge calls it from the very first
 * line of {@code ItemEntity#tick} and returns early when it answers true; a cancellable HEAD inject is
 * exactly that. {@code tick()V} is byte-identical on every Fabric node, so there are no arms.
 *
 * <p>Dispatched off the item rather than through an interface so that nothing else in the mod pays for
 * it: this is the only implementor, and adding a second one means adding a branch here on purpose.
 */
@Mixin(ItemEntity.class)
public abstract class FabricItemEntityMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void alexsmobs$entityItemUpdate(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        ItemStack stack = self.getItem();
        if (stack.getItem() instanceof com.github.alexthe666.alexsmobs.item.ItemMysteriousWorm worm
                && worm.onEntityItemUpdate(stack, self)) {
            ci.cancel();
        }
    }
}

package com.github.alexthe666.alexsmobs.mixin.fabric;

import com.github.alexthe666.alexsmobs.fabric.event.FabricServerEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fires {@code PlayerEvent.HarvestCheck} on Fabric, which is how the ghostly pickaxe suppresses
 * block drops so an underminer can store what it mined inside its ghost instead.
 *
 * <p><b>Only the one-argument overload exists.</b> The
 * {@code hasCorrectToolForDrops(BlockState, Level, BlockPos)} that shows up from 1.20.6 is a
 * <i>NeoForge patch</i>, not a vanilla addition — it reads as vanilla when javap'd against the
 * NeoForge merged bundle, which is the jar the usual era probe picks. Checked again against the
 * unpatched loom jars the Fabric nodes actually compile on: one overload, 1.20.1 through 26.2,
 * unchanged. So no arms, and no second injection.
 */
@Mixin(Player.class)
public class FabricPlayerMixin {

    @Inject(method = "hasCorrectToolForDrops(Lnet/minecraft/world/level/block/state/BlockState;)Z",
            at = @At("RETURN"), cancellable = true)
    private void alexsmobs$harvestCheck(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        // Fires on both sides, as Forge's does: the client uses the same answer to decide whether
        // to show the slow-mining break animation, and a disagreement there is visible.
        cir.setReturnValue(FabricServerEvents.fireHarvestCheck((Player) (Object) this, cir.getReturnValue()));
    }
}

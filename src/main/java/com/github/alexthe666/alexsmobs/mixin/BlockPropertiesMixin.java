package com.github.alexthe666.alexsmobs.mixin;

import net.minecraft.world.level.block.state.BlockBehaviour;
//? if >=1.21.2 {
/*import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import com.github.alexthe666.alexsmobs.misc.RegistrationContext;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
*///?}
import org.spongepowered.asm.mixin.Mixin;

/**
 * MC 1.21.2 requires {@link BlockBehaviour.Properties} to carry their registry id before the block is
 * built — {@code BlockBehaviour.<init>} reads it via {@code effectiveDrops()} / {@code effectiveDescriptionId()}
 * (both inline {@code requireNonNull(id, "Block id not set")}). Our registration builds blocks from plain
 * suppliers whose {@code Properties} are created inside the constructor, so we stamp the id — published via
 * {@link com.github.alexthe666.alexsmobs.misc.RegistrationContext} by {@code AMBlockRegistry.regBlock} — at
 * those id-requiring getters, which run from inside the constructor during the registry flush.
 *
 * <p>Empty (no injects, no {@code setId} reference) on nodes below 1.21.2, where that API does not exist.
 */
@Mixin(BlockBehaviour.Properties.class)
public class BlockPropertiesMixin {
    //? if >=1.21.2 {
    /*@Inject(method = "effectiveDrops", at = @At("HEAD"))
    private void am$setBlockIdFromDrops(CallbackInfoReturnable<Optional<ResourceKey<LootTable>>> cir) {
        am$stampBlockId();
    }

    @Inject(method = "effectiveDescriptionId", at = @At("HEAD"))
    private void am$setBlockIdFromDesc(CallbackInfoReturnable<String> cir) {
        am$stampBlockId();
    }

    @SuppressWarnings("unchecked")
    private void am$stampBlockId() {
        ResourceKey<?> id = RegistrationContext.CURRENT_ID.get();
        if (id != null && id.isFor(Registries.BLOCK)) {
            ((BlockBehaviour.Properties) (Object) this).setId((ResourceKey<Block>) id);
        }
    }
    *///?}
}

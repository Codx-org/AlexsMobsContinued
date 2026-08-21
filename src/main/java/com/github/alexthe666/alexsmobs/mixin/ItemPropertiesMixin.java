package com.github.alexthe666.alexsmobs.mixin;

import net.minecraft.world.item.Item;
//? if >=1.21.2 {
/*import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import com.github.alexthe666.alexsmobs.misc.RegistrationContext;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
*///?}
import org.spongepowered.asm.mixin.Mixin;

/**
 * MC 1.21.2 requires {@link Item.Properties} to carry their registry id before the item is built —
 * {@code Item.<init>} reads it via {@code effectiveDescriptionId()} (which inlines
 * {@code requireNonNull(id, "Item id not set")}). Our registration builds items from plain suppliers
 * whose {@code Properties} are created inside the constructor, so we stamp the id — published via
 * {@link com.github.alexthe666.alexsmobs.misc.RegistrationContext} by {@code AMItemRegistry.regItem} —
 * at that getter, which runs from inside the constructor during the registry flush.
 *
 * <p>Empty (no injects, no {@code setId} reference) on nodes below 1.21.2, where that API does not exist.
 */
@Mixin(Item.Properties.class)
public class ItemPropertiesMixin {
    //? if >=1.21.2 {
    /*@Inject(method = "effectiveDescriptionId", at = @At("HEAD"))
    private void am$setItemId(CallbackInfoReturnable<String> cir) {
        ResourceKey<?> id = RegistrationContext.CURRENT_ID.get();
        if (id != null && id.isFor(Registries.ITEM)) {
            ((Item.Properties) (Object) this).setId((ResourceKey<Item>) id);
        }
    }
    *///?}
}

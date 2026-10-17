package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.misc.AMCompat;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;

public class ItemTabIcon extends ItemInventoryOnly implements IClientExtensionItem {
    public ItemTabIcon(Item.Properties properties) {
        super(properties);
    }

    public static boolean hasCustomEntityDisplay(ItemStack stack){
        return AMCompat.getTag(stack) != null && AMCompat.getTag(stack).contains("DisplayEntityType");
    }

    public static String getCustomDisplayEntityString(ItemStack stack){
        return AMCompat.getString(AMCompat.getTag(stack), "DisplayEntityType");
    }

    @Override
    public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer) {
        consumer.accept((IClientItemExtensions)AlexsMobs.PROXY.getISTERProperties());
    }

    @Nullable
    public static EntityType getEntityType(@Nullable CompoundTag tag) {
        if (tag != null && AMCompat.contains(tag, "DisplayEntityType")) {
            String entityType = AMCompat.getString(tag, "DisplayEntityType");
           return BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.tryParse(entityType));
        }
        return null;
    }
}

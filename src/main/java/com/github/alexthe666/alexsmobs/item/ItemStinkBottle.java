package com.github.alexthe666.alexsmobs.item;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import java.util.function.Supplier;

public class ItemStinkBottle extends AMBlockItem {

    public ItemStinkBottle(Supplier<Block> blockSupplier, Item.Properties props) {
        super(blockSupplier, props);
    }

    public InteractionResult place(BlockPlaceContext context) {
        InteractionResult result = super.place(context);
        if(result.consumesAction()){
            ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
            if(context.getPlayer() == null){
                context.getLevel().addFreshEntity(new ItemEntity(context.getLevel(),context.getClickedPos().getX() + 0.5F, context.getClickedPos().getY() + 0.5F, context.getClickedPos().getZ() + 0.5F, bottle));
            }else if(!context.getPlayer().addItem(bottle)){
                context.getPlayer().drop(bottle, false);
            }
        }
        return result;
    }
    //? if <1.21.2 {
    public String getDescriptionId() {
        return this.getOrCreateDescriptionId();
    }
    //?}
    // Item#getDescriptionId became final in 1.21.2 and this override merely returned the default, so drop it.
}

package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.entity.EntitySharkToothArrow;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemModArrow extends ArrowItem {
    public ItemModArrow(Item.Properties group) {
        super(group);
    }

    // 1.21 added the firing weapon as a fourth argument, so the arrow can read its enchantments.
    //? if >=1.21 {
    /*public AbstractArrow createArrow(Level worldIn, ItemStack stack, LivingEntity shooter, ItemStack weapon) {
        if(this == AMItemRegistry.SHARK_TOOTH_ARROW.get()){
            return new EntitySharkToothArrow(worldIn, shooter, stack.copyWithCount(1));
        }else {
            return super.createArrow(worldIn, stack, shooter, weapon);
        }
    }
    *///?}
    //? if >=1.20.5 && <1.21 {
    /*public AbstractArrow createArrow(Level worldIn, ItemStack stack, LivingEntity shooter) {
        if(this == AMItemRegistry.SHARK_TOOTH_ARROW.get()){
            return new EntitySharkToothArrow(worldIn, shooter, stack.copyWithCount(1));
        }else {
            return super.createArrow(worldIn, stack, shooter);
        }
    }
    *///?}
    //? if <1.20.5 {
    public AbstractArrow createArrow(Level worldIn, ItemStack stack, LivingEntity shooter) {
        if(this == AMItemRegistry.SHARK_TOOTH_ARROW.get()){
            Arrow arrowentity = new EntitySharkToothArrow(worldIn, shooter);
            arrowentity.setEffectsFromItem(stack);
            return arrowentity;
        }else {
            return super.createArrow(worldIn, stack, shooter);
        }
    }
    //?}

}

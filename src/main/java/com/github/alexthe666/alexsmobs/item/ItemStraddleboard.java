package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.misc.AMCompat;

import com.github.alexthe666.alexsmobs.entity.EntityStraddleboard;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
//? if <1.20.5
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Predicate;

public class ItemStraddleboard extends Item
        //? if <1.20.5
        implements DyeableLeatherItem
{

    private static final Predicate<Entity> ENTITY_PREDICATE = EntitySelector.NO_SPECTATORS.and(Entity::isPickable);

    public ItemStraddleboard(Item.Properties properties) {
        super(properties);
    }

    public int getColor(ItemStack p_200886_1_) {
        //? if >=1.20.5 {
        /*return net.minecraft.world.item.component.DyedItemColor.getOrDefault(p_200886_1_, 0XADC3D7);
        *///?} else {
        CompoundTag lvt_2_1_ = AMCompat.getTagElement(p_200886_1_, "display");
        return lvt_2_1_ != null && lvt_2_1_.contains("color", 99) ? lvt_2_1_.getInt("color") : 0XADC3D7;
        //?}
    }

    // From 1.21 the enchanting table is driven purely by each enchantment's supported_items tag.
    // The board is only in alexsmobs:straddleboard_enchantable, and Unbreaking/Mending want
    // minecraft:durability_enchantable, so the exclusion below happens by itself.
    // Fabric has no such hook at any version, so below 1.21 the board there IS enchantable with
    // Unbreaking and Mending — a small Fabric-only divergence rather than a crash.
    //? if <1.21 && !fabric {
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return super.canApplyAtEnchantingTable(stack, enchantment) && enchantment != Enchantments.UNBREAKING && enchantment != Enchantments.MENDING;
    }
    //?}

    public int getEnchantmentValue() {
        return 1;
    }

    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
        ItemStack itemstack = playerIn.getItemInHand(handIn);
        HitResult raytraceresult = getPlayerPOVHitResult(worldIn, playerIn, ClipContext.Fluid.ANY);
        if (raytraceresult.getType() == HitResult.Type.MISS) {
            return AMCompat.pass(itemstack);
        } else {
            Vec3 vector3d = playerIn.getViewVector(1.0F);
            double d0 = 5.0D;
            List<Entity> list = worldIn.getEntities(playerIn, playerIn.getBoundingBox().expandTowards(vector3d.scale(5.0D)).inflate(1.0D), ENTITY_PREDICATE);
            if (!list.isEmpty()) {
                Vec3 vector3d1 = playerIn.getEyePosition(1.0F);

                for (Entity entity : list) {
                    AABB axisalignedbb = entity.getBoundingBox().inflate(entity.getPickRadius());
                    if (axisalignedbb.contains(vector3d1)) {
                        return AMCompat.pass(itemstack);
                    }
                }
            }

            if (raytraceresult.getType() == HitResult.Type.BLOCK) {
                EntityStraddleboard boatentity = new EntityStraddleboard(worldIn, raytraceresult.getLocation().x, raytraceresult.getLocation().y, raytraceresult.getLocation().z);
                boatentity.setDefaultColor(!AMCompat.hasCustomColor(itemstack));
                boatentity.setItemStack(itemstack.copy());
                boatentity.setColor(this.getColor(itemstack));
                boatentity.setYRot(playerIn.getYRot());
                if (!worldIn.noCollision(boatentity, boatentity.getBoundingBox().inflate(-0.1D))) {
                    return AMCompat.fail(itemstack);
                } else {
                    if (!worldIn.isClientSide()) {
                        worldIn.addFreshEntity(boatentity);
                        if (!playerIn.getAbilities().instabuild) {
                            itemstack.shrink(1);
                        }
                    }

                    playerIn.awardStat(Stats.ITEM_USED.get(this));
                    return AMCompat.sidedSuccess(itemstack, worldIn.isClientSide());
                }
            } else {
                return AMCompat.pass(itemstack);
            }
        }
    }
}

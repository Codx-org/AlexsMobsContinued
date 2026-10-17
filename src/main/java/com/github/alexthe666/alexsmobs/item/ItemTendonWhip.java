package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.misc.AMCompat;

import com.github.alexthe666.alexsmobs.entity.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.entity.EntityTendonSegment;
import com.github.alexthe666.alexsmobs.entity.util.TendonWhipUtil;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
//? if <1.21.5 {
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
//?}
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
//? if !fabric {
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
//?}

public class ItemTendonWhip extends
//? if >=1.21.5 {
/*Item
*///?} else {
SwordItem
//?}
implements ILeftClick {

    //? if <1.20.5 {
    private final ImmutableMultimap<Attribute, AttributeModifier> tendonModifiers;
    //?}

    public ItemTendonWhip(Item.Properties props) {
        //? if >=1.21.5 {
        /*// 1.21.5 removed SwordItem: sword stats are now a Properties#sword component on a plain Item.
        // #95: the repair tag has to ride on the material — Properties#sword applies it and would
        // otherwise overwrite anything repairable() set beforehand with iron's own tag.
        super(props.sword(AMCompat.repairMaterial(net.minecraft.world.item.ToolMaterial.IRON, "tendon_whip"), 2.0F, -3.0F));
        *///?} elif >=1.21.2 {
        /*// 1.21.2 gave SwordItem a (material, damage, speed, props) ctor again. The damage
        // argument is a bonus on top of the material's own, so 4.0 total is 2.0 over iron's 2.0.
        super(AMCompat.repairMaterial(Tiers.IRON, "tendon_whip"), 2.0F, -3.0F, props);
        *///?} elif >=1.20.5 {
        /*// 1.20.5 moved item attributes into a data component set at construction time.
        super(Tiers.IRON, props.attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, AMCompat.attributeModifier(AMCompat.BASE_ATTACK_DAMAGE_ID, "Weapon modifier", (double) 4F, AttributeModifier.Operation.ADDITION), net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, AMCompat.attributeModifier(AMCompat.BASE_ATTACK_SPEED_ID, "Weapon modifier", (double) -3.0F, AttributeModifier.Operation.ADDITION), net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .build()));
        *///?} else {
        super(Tiers.IRON, 3, 0, props);
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE, AMCompat.attributeModifier(AMCompat.BASE_ATTACK_DAMAGE_ID, "Weapon modifier", (double)4F, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_SPEED, AMCompat.attributeModifier(AMCompat.BASE_ATTACK_SPEED_ID, "Weapon modifier", (double)-3.0F, AttributeModifier.Operation.ADDITION));
        this.tendonModifiers = builder.build();
        //?}
    }

    public static boolean isActive(ItemStack stack, LivingEntity holder) {
        if (holder != null && (holder.getMainHandItem() == stack || holder.getOffhandItem() == stack)) {
            return !TendonWhipUtil.canLaunchTendons(holder.level(), holder);
        }
        return false;
    }


    //? if <1.20.5 {
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? this.tendonModifiers : super.getDefaultAttributeModifiers(slot);
    }
    //?}

    //? if >=1.21.5 {
    /*// 1.21.5: Item#hurtEnemy returns void.
    public void hurtEnemy(ItemStack stack, LivingEntity entity, LivingEntity player) {
        launchTendonsAt(stack, player, entity);
        super.hurtEnemy(stack, entity, player);
    }
    *///?} else {
    public boolean hurtEnemy(ItemStack stack, LivingEntity entity, LivingEntity player) {
        launchTendonsAt(stack, player, entity);
        return super.hurtEnemy(stack, entity, player);
    }
    //?}

    private boolean isCharged(Player player, ItemStack stack){
        return player.getAttackStrengthScale(0.5F) > 0.9F;
    }

    public boolean onLeftClick(ItemStack stack, LivingEntity playerIn){
        if(stack.is(AMItemRegistry.TENDON_WHIP.get()) && (!(playerIn instanceof Player) || isCharged((Player)playerIn, stack))){
            Level worldIn = playerIn.level();
            Entity closestValid = null;
            Vec3 playerEyes = playerIn.getEyePosition(1.0F);
            HitResult hitresult = worldIn.clip(new ClipContext(playerEyes, playerEyes.add(playerIn.getLookAngle().scale(12.0D)), ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, playerIn));
            if (hitresult instanceof EntityHitResult) {
                Entity entity = ((EntityHitResult) hitresult).getEntity();
                if (!entity.equals(playerIn) && !playerIn.isAlliedTo(entity) && !entity.isAlliedTo(playerIn) && entity instanceof Mob && playerIn.hasLineOfSight(entity)) {
                    closestValid = entity;
                }
            } else {
                for (Entity entity : worldIn.getEntitiesOfClass(LivingEntity.class, playerIn.getBoundingBox().inflate(12.0D))) {
                    if (!entity.equals(playerIn) && !playerIn.isAlliedTo(entity) && !entity.isAlliedTo(playerIn) && entity instanceof Mob && playerIn.hasLineOfSight(entity)) {
                        if (closestValid == null || playerIn.distanceTo(entity) < playerIn.distanceTo(closestValid)) {
                            closestValid = entity;
                        }
                    }
                }
            }
            if(closestValid != null){
                AMCompat.hurtAndBreak(stack, 1, playerIn, playerIn.getUsedItemHand());
            }
            return launchTendonsAt(stack, playerIn, closestValid);
        }
        return false;
    }

    public boolean launchTendonsAt(ItemStack stack, LivingEntity playerIn, Entity closestValid) {
        Level worldIn = playerIn.level();
        if (TendonWhipUtil.canLaunchTendons(worldIn, playerIn)) {
            TendonWhipUtil.retractFarTendons(worldIn, playerIn);
            if (!worldIn.isClientSide()) {
                if (closestValid != null) {
                    EntityTendonSegment segment = AMCompat.create(AMEntityRegistry.TENDON_SEGMENT.get(), worldIn);
                    segment.copyPosition(playerIn);
                    worldIn.addFreshEntity(segment);
                    segment.setCreatorEntityUUID(playerIn.getUUID());
                    segment.setFromEntityID(playerIn.getId());
                    segment.setToEntityID(closestValid.getId());
                    segment.copyPosition(playerIn);
                    segment.setProgress(0.0F);
                    segment.setHasGlint(stack.hasFoil());
                    TendonWhipUtil.setLastTendon(playerIn, segment);
                    return true;
                }
            }
        }
        return false;
    }

    // "This whip does not sweep" — a Forge/NeoForge item-extension hook. Fabric has no equivalent
    // query (sweep attacks are decided by vanilla alone), so the whip sweeps like any other sword
    // there. Cosmetic-only: the sweep particle and the extra 1-damage arc.
    //? if !fabric {
    public boolean canPerformAction(ItemStack stack, ToolAction toolAction) {
        return toolAction != ToolActions.SWORD_SWEEP && super.canPerformAction(stack, toolAction);
    }
    //?}

    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return !ItemStack.isSameItem(oldStack, newStack);
    }

    public int getMaxDamage(ItemStack stack) {
        return 450;
    }

    //? if <1.21.2 {
    // >=1.21.2: declared as the alexsmobs:repairs/tendon_whip tag instead (#95).
    public boolean isValidRepairItem(ItemStack pickaxe, ItemStack stack) {
        return stack.is(AMItemRegistry.ELASTIC_TENDON.get());
    }
    //?}

}

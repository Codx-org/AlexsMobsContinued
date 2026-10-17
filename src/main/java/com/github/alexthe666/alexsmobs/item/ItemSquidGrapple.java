package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.misc.AMCompat;

import com.github.alexthe666.alexsmobs.entity.EntitySquidGrapple;
import com.github.alexthe666.alexsmobs.entity.util.SquidGrappleUtil;
import com.github.alexthe666.alexsmobs.misc.AMSoundRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

public class ItemSquidGrapple extends Item {

    public ItemSquidGrapple(Item.Properties properties) {
        super(properties);
    }

    // 1.21 added the LivingEntity param to Item#getUseDuration. Upstream never wrote @Override
    // here, so from 1.21 up the 1-arg form below quietly stopped overriding anything and every
    // startUsingItem-driven item read vanilla's default instead. It stays as the <1.21 override
    // and as this class's own helper; this delegates to it. See docs/notes/api-eras.md.
    //? if >=1.21 {
    /*@Override
    public int getUseDuration(ItemStack stack, net.minecraft.world.entity.LivingEntity user) {
        return this.getUseDuration(stack);
    }
    *///?}
    public int getUseDuration(ItemStack p_40680_) {
        return 72000;
    }

    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    public InteractionResultHolder<ItemStack> use(Level p_40672_, Player p_40673_, InteractionHand p_40674_) {
        ItemStack itemstack = p_40673_.getItemInHand(p_40674_);
        p_40673_.startUsingItem(p_40674_);

        return AMCompat.pass(itemstack);
    }

    public void onUseTick(Level worldIn, LivingEntity livingEntityIn, ItemStack stack, int count) {

    }

    //? if >=1.21.2 {
    /*public boolean releaseUsing(ItemStack stack, Level worldIn, LivingEntity livingEntityIn, int i) { releaseUsingImpl(stack, worldIn, livingEntityIn, i); return true; }
    *///?} else {
    public void releaseUsing(ItemStack stack, Level worldIn, LivingEntity livingEntityIn, int i) { releaseUsingImpl(stack, worldIn, livingEntityIn, i); }
    //?}
    private void releaseUsingImpl(ItemStack stack, Level worldIn, LivingEntity livingEntityIn, int i) {
        if(livingEntityIn.isFallFlying()){
            return;
        }
        livingEntityIn.playSound(AMSoundRegistry.GIANT_SQUID_TENTACLE.get(),1.0F, 1.0F + (livingEntityIn.getRandom().nextFloat() - livingEntityIn.getRandom().nextFloat()) * 0.2F);
        livingEntityIn.gameEvent(GameEvent.ITEM_INTERACT_FINISH);
        if (!worldIn.isClientSide()) {
            boolean left = false;
            if (livingEntityIn.getUsedItemHand() == InteractionHand.OFF_HAND && livingEntityIn.getMainArm() == HumanoidArm.RIGHT || livingEntityIn.getUsedItemHand() == InteractionHand.MAIN_HAND && livingEntityIn.getMainArm() == HumanoidArm.LEFT) {
                left = true;
            }
            int power = this.getUseDuration(stack) - i;
            EntitySquidGrapple hook = new EntitySquidGrapple(worldIn, livingEntityIn, !left);
            Vec3 vector3d = livingEntityIn.getViewVector(1.0F);
            hook.shoot((double) vector3d.x(), (double) vector3d.y(), (double) vector3d.z(), getPowerForTime(power) * 3, 1);
            hook.setXRot(livingEntityIn.getXRot());
            hook.setYRot(livingEntityIn.getYRot());
            if (!worldIn.isClientSide()) {
                worldIn.addFreshEntity(hook);
            }
            AMCompat.hurtAndBreak(stack, 1, livingEntityIn, livingEntityIn.getUsedItemHand());
            SquidGrappleUtil.onFireHook(livingEntityIn, hook.getUUID());
        }
    }

    //? if <1.21.2 {
    // >=1.21.2: declared as the alexsmobs:repairs/squid_grapple tag instead (#95).
    public boolean isValidRepairItem(ItemStack s, ItemStack s1) {
        return s1.is(AMItemRegistry.LOST_TENTACLE.get());
    }
    //?}

    public static float getPowerForTime(int p) {
        float f = (float)p / 20.0F;
        f = (f * f + f + f * 2.0F) / 4.0F;
        if (f > 1.0F) {
            f = 1.0F;
        }

        return f;
    }

    @Override
    // 1.20.5 replaced the nullable Level with a TooltipContext; nothing here reads it.
    //? if >=1.21.5 {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> amTooltip, TooltipFlag flagIn) {
        java.util.List<Component> tooltip = new java.util.ArrayList<Component>() { public boolean add(Component amC) { amTooltip.accept(amC); return true; } };
    *///?} elif >=1.20.5 {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
    *///?} else {
    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
    //?}
        tooltip.add(Component.translatable("item.alexsmobs.squid_grapple.desc").withStyle(ChatFormatting.GRAY));

    }
}

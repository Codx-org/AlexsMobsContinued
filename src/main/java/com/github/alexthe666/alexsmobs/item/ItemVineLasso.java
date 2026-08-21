package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.misc.AMCompat;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.entity.EntityVineLasso;
import com.github.alexthe666.alexsmobs.misc.AMSoundRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public class ItemVineLasso extends Item implements IClientExtensionItem {

    public ItemVineLasso(Properties props) {
        super(props);
    }

    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    public static boolean isItemInUse(ItemStack stack){
        return AMCompat.getTag(stack) != null && AMCompat.getTag(stack).contains("Swinging") && AMCompat.getBoolean(AMCompat.getTag(stack), "Swinging");
    }

    // 1.21.5 reshaped Item#inventoryTick to (stack, ServerLevel, entity, EquipmentSlot). The old
    // 5-arg form kept its name, so with no @Override it silently stopped running from 1.21.5 up
    // and the lasso's "Swinging" flag was never written. ItemGhostlyPickaxe was gated for this
    // at the time and this one was missed.
    //? if >=1.21.5 {
    /*@Override
    public void inventoryTick(ItemStack stack, net.minecraft.server.level.ServerLevel world, Entity entity, net.minecraft.world.entity.EquipmentSlot amSlot) {
        inventoryTickImpl(stack, entity);
    }
    *///?} else {
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int i, boolean b) {
        inventoryTickImpl(stack, entity);
    }
    //?}

    private void inventoryTickImpl(ItemStack stack, Entity entity) {
        if(entity instanceof LivingEntity){
            CompoundTag tag = AMCompat.getTag(stack);
            if(tag != null){
                // AMCompat.getTag hands back a COPY on >=1.20.5, so this has to be written back.
                boolean swinging = ((LivingEntity) entity).getUseItem() == stack && ((LivingEntity) entity).isUsingItem();
                if(swinging != AMCompat.getBoolean(tag, "Swinging")){
                    tag.putBoolean("Swinging", swinging);
                    AMCompat.setTag(stack, tag);
                }
            }else{
                AMCompat.setTag(stack, new CompoundTag());
            }
        }
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

    public InteractionResultHolder<ItemStack> use(Level p_40672_, Player p_40673_, InteractionHand p_40674_) {
        ItemStack itemstack = p_40673_.getItemInHand(p_40674_);
        p_40673_.startUsingItem(p_40674_);

        return AMCompat.success(itemstack);
    }

    public void onUseTick(Level worldIn, LivingEntity livingEntityIn, ItemStack stack, int count) {
        if(count % 7 == 0){
            livingEntityIn.gameEvent(GameEvent.ITEM_INTERACT_START);
            livingEntityIn.playSound(AMSoundRegistry.VINE_LASSO.get(),1.0F, 1.0F + (livingEntityIn.getRandom().nextFloat() - livingEntityIn.getRandom().nextFloat()) * 0.2F);
        }
    }

    //? if >=1.21.2 {
    /*public boolean releaseUsing(ItemStack stack, Level worldIn, LivingEntity livingEntityIn, int i) { releaseUsingImpl(stack, worldIn, livingEntityIn, i); return true; }
    *///?} else {
    public void releaseUsing(ItemStack stack, Level worldIn, LivingEntity livingEntityIn, int i) { releaseUsingImpl(stack, worldIn, livingEntityIn, i); }
    //?}
    private void releaseUsingImpl(ItemStack stack, Level worldIn, LivingEntity livingEntityIn, int i) {
        if (!worldIn.isClientSide()) {
            boolean left = false;
            if (livingEntityIn.getUsedItemHand() == InteractionHand.OFF_HAND && livingEntityIn.getMainArm() == HumanoidArm.RIGHT || livingEntityIn.getUsedItemHand() == InteractionHand.MAIN_HAND && livingEntityIn.getMainArm() == HumanoidArm.LEFT) {
                left = true;
            }
            int power = this.getUseDuration(stack) - i;
            EntityVineLasso lasso = new EntityVineLasso(worldIn, livingEntityIn);
            Vec3 vector3d = livingEntityIn.getViewVector(1.0F);
            lasso.shoot((double) vector3d.x(), (double) vector3d.y(), (double) vector3d.z(), getPowerForTime(power), 1);
            if (!worldIn.isClientSide()) {
                worldIn.addFreshEntity(lasso);
            }
            stack.shrink(1);
        }
        //livingEntityIn.awardStat(Stats.ITEM_USED.get(this));
    }

    public static float getPowerForTime(int p) {
        float f = (float)p / 20.0F;
        f = (f * f + f * 2.0F) / 3.0F;
        if (f > 1.0F) {
            f = 1.0F;
        }

        return f;
    }


    @Override
    public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer) {
        consumer.accept((IClientItemExtensions) AlexsMobs.PROXY.getISTERProperties());
    }
}

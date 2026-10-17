package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
// Only used by the <1.21.5 canPerformAction below; NeoForge 1.21.5 deleted the shield action set
// and Fabric never had one.
//? if <1.21.5 && !fabric {
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
//?}
import com.github.alexthe666.alexsmobs.misc.AMCompat;

public class ItemShieldOfTheDeep extends Item implements IClientExtensionItem {
    public ItemShieldOfTheDeep(Item.Properties group) {
        super(AMCompat.shieldProperties(group));
    }

    //? if <1.21.5 && !fabric {
    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction toolAction) {
        return ToolActions.DEFAULT_SHIELD_ACTIONS.contains(toolAction);
    }
    //?}
    // On >=1.21.5 the shield-action set is gone; AMCompat.shieldProperties stamps the
    // BLOCKS_ATTACKS data component in the ctor above instead. Fabric below 1.21.5 has neither,
    // and declares itself a shield the only way vanilla understands there: the BLOCK use
    // animation below, which is what LivingEntity#isBlocking and AMCompat.canShieldBlock read.

    public UseAnim getUseAnimation(ItemStack p_77661_1_) {
        return UseAnim.BLOCK;
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
    public int getUseDuration(ItemStack p_77626_1_) {
        return 72000;
    }

    public InteractionResultHolder<ItemStack> use(Level p_77659_1_, Player p_77659_2_, InteractionHand p_77659_3_) {
        ItemStack lvt_4_1_ = p_77659_2_.getItemInHand(p_77659_3_);
        p_77659_2_.startUsingItem(p_77659_3_);
        return AMCompat.consume(lvt_4_1_);
    }

    //? if <1.21.2 {
    public boolean isValidRepairItem(ItemStack p_82789_1_, ItemStack p_82789_2_) {
        return AMItemRegistry.SERRATED_SHARK_TOOTH.get() == p_82789_2_.getItem() || super.isValidRepairItem(p_82789_1_, p_82789_2_);
    }
    //?}
    // >=1.21.2: Item#isValidRepairItem is gone (repair is the Repairable data component now), so the
    // serrated shark tooth AND ShieldItem's own #minecraft:planks are declared together in the
    // alexsmobs:repairs/shield_of_the_deep tag, wired by AMCompat.repairableWith at registration (#95).

    @Override
    public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer) {
        consumer.accept((IClientItemExtensions) AlexsMobs.PROXY.getISTERProperties());
    }
}

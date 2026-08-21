package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.misc.AMCompat;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
// Only used by the <1.21.5 canPerformAction below; NeoForge 1.21.5 deleted the shield action set
// and Fabric never had one.
//? if <1.21.5 && !fabric {
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
//?}

public class ItemSkelewagSword extends
//? if >=1.21.5 {
/*Item
*///?} else {
SwordItem
//?}
implements IClientExtensionItem {

    //? if <1.20.5 {
    private final ImmutableMultimap<Attribute, AttributeModifier> skelewagModifiers;
    //?}

    public ItemSkelewagSword(Item.Properties props) {
        //? if >=1.21.5 {
        /*// 1.21.5 removed SwordItem: sword stats are now a Properties#sword component on a plain Item.
        // The skelewag also blocks like a shield, which is a data component here too.
        // #95: the repair tag has to ride on the material — Properties#sword applies it and would
        // otherwise overwrite anything repairable() set beforehand with iron's own tag.
        super(AMCompat.shieldProperties(props.sword(AMCompat.repairMaterial(net.minecraft.world.item.ToolMaterial.IRON, "skelewag_sword"), 1.5F, 0F)));
        *///?} elif >=1.21.2 {
        /*// 1.21.2 gave SwordItem a (material, damage, speed, props) ctor again. The damage
        // argument is a bonus on top of the material's own, so 3.5 total is 1.5 over iron's 2.0.
        super(AMCompat.repairMaterial(Tiers.IRON, "skelewag_sword"), 1.5F, 0F, props);
        *///?} elif >=1.20.5 {
        /*// 1.20.5 moved item attributes into a data component set at construction time, so the
        // damage/speed numbers that used to come from the SwordItem(tier, dmg, speed) ctor and
        // from the getDefaultAttributeModifiers override now both live in this one builder.
        super(Tiers.IRON, props.attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, AMCompat.attributeModifier(AMCompat.BASE_ATTACK_DAMAGE_ID, "Weapon modifier", (double) 3.5F, AttributeModifier.Operation.ADDITION), net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, AMCompat.attributeModifier(AMCompat.BASE_ATTACK_SPEED_ID, "Weapon modifier", (double) 0, AttributeModifier.Operation.ADDITION), net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .build()));
        *///?} else {
        super(Tiers.IRON, 2, 0, props);
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE, AMCompat.attributeModifier(AMCompat.BASE_ATTACK_DAMAGE_ID, "Weapon modifier", (double)3.5F, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_SPEED, AMCompat.attributeModifier(AMCompat.BASE_ATTACK_SPEED_ID, "Weapon modifier", (double)0, AttributeModifier.Operation.ADDITION));
        this.skelewagModifiers = builder.build();
        //?}
    }

    public float getDamage() {
        return 3.5F;
    }

    //? if <1.21.5 && !fabric {
    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction toolAction) {
        return ToolActions.DEFAULT_SHIELD_ACTIONS.contains(toolAction);
    }
    //?}
    // On >=1.21.5 the shield-action set is gone; AMCompat.shieldProperties stamps the
    // BLOCKS_ATTACKS data component in the ctor above instead.

    public UseAnim getUseAnimation(ItemStack stack) {
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
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer) {
        consumer.accept((IClientItemExtensions) AlexsMobs.PROXY.getISTERProperties());
    }

    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack lvt_4_1_ = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return AMCompat.consume(lvt_4_1_);
    }

    //? if <1.21.2 {
    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairStack) {
        return repairStack.is(Items.BONE);
    }
    //?}
    // >=1.21.2: Item#isValidRepairItem is gone (Repairable data component now); the bone repair is
    // declared as the alexsmobs:repairs/skelewag_sword tag instead (#95).

    //? if <1.20.5 {
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? this.skelewagModifiers : super.getDefaultAttributeModifiers(slot);
    }
    //?}

}

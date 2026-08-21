package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.misc.AMCompat;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
//? if <1.21.5 {
import net.minecraft.world.item.ArmorItem;
//?}
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;

public class ItemTarantulaHawkElytra extends
//? if >=1.21.5 {
/*Item
*///?} else {
ArmorItem
//?}
implements IClientExtensionItem {

    public ItemTarantulaHawkElytra(Item.Properties props, AMArmorMaterial mat) {
        //? if >=1.21.5 {
        /*super(props.humanoidArmor(mat.material(), net.minecraft.world.item.equipment.ArmorType.CHESTPLATE));
        *///?} elif >=1.21.2 {
        /*super(mat.material(), ArmorItem.Type.CHESTPLATE, props);
        *///?} elif >=1.20.5 {
        /*super(mat.holder(), Type.CHESTPLATE, props);
        *///?} else {
        super(mat, Type.CHESTPLATE, props);
        //?}
    }

    @Override
    public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer) {
        consumer.accept((IClientItemExtensions) AlexsMobs.PROXY.getArmorRenderProperties());
    }

    public static boolean isUsable(ItemStack stack) {
        return stack.getDamageValue() < stack.getMaxDamage() - 1;
    }

    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
        return super.use(worldIn, playerIn, handIn);
    }

    // 1.21.2 deleted the canElytraFly / elytraFlightTick item hooks on BOTH Forge and NeoForge —
    // Forge still declares them but no patched class calls them (bytecode-swept, every ≥1.21.2
    // era), so gliding there is the vanilla minecraft:glider data component, attached at
    // registration via AMCompat.glider, and vanilla drains the durability itself. These two
    // overrides exist only where the hooks are actually alive: Forge/NeoForge below 1.21.2.
    // Fabric below 1.21.2 has no item hook at all; its glide seam is the EntityElytraEvents.CUSTOM
    // handler registered in AlexsMobsFabric, which mirrors elytraFlightTick's 20-tick drain.
    //? if !fabric && <1.21.2 {
    @Override
    public boolean canElytraFly(ItemStack stack, net.minecraft.world.entity.LivingEntity entity) {
        return isUsable(stack);
    }

    @Override
    public boolean elytraFlightTick(ItemStack stack, net.minecraft.world.entity.LivingEntity entity, int flightTicks) {
        if (!entity.level().isClientSide() && (flightTicks + 1) % 20 == 0) {
            AMCompat.hurtAndBreak(stack, 1, entity, net.minecraft.world.entity.EquipmentSlot.CHEST);
        }
        return true;
    }
    //?}

    //? if <1.21.2 {
    // >=1.21.2: Item#isValidRepairItem is gone; this one is already covered there by AMArmorMaterial,
    // whose material carries repairable(alexsmobs:repairs/tarantula_hawk_elytra). See #95.
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.getItem() == AMItemRegistry.TARANTULA_HAWK_WING_FRAGMENT.get();
    }
    //?}

    public EquipmentSlot getEquipmentSlot(ItemStack stack) {
        return EquipmentSlot.CHEST;
    }

    //? if <1.20.5 {
    @Nullable
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return "alexsmobs:textures/armor/tarantula_hawk_elytra.png";
    }
    //?}
}

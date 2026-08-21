package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.misc.AMCompat;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

public class ItemGhostlyPickaxe extends
//? if >=1.21.5 {
/*Item
*///?} else {
PickaxeItem
//?}
{

    public ItemGhostlyPickaxe(Properties props) {
        //? if >=1.21.5 {
        /*// 1.21.5 removed PickaxeItem: pickaxe stats are now a Properties#pickaxe component on a plain Item.
        // #95: the material must carry the repair tag — Properties#pickaxe applies it and would
        // otherwise overwrite anything repairable() set beforehand with iron's own tag.
        super(props.pickaxe(AMCompat.repairMaterial(net.minecraft.world.item.ToolMaterial.IRON, "ghostly_pickaxe"), 1, -2.8F));
        *///?} elif >=1.21.2 {
        /*// 1.21.2 gave PickaxeItem the (material, damage, speed, props) ctor back. Same #95 note as
        // above: here it is the DiggerItem super constructor that applies (and would overwrite) it.
        super(AMCompat.repairMaterial(Tiers.IRON, "ghostly_pickaxe"), 1, -2.8F, props);
        *///?} elif >=1.20.5 {
        /*// 1.20.5 moved the damage/speed numbers out of the ctor into an attributes component.
        super(Tiers.IRON, props.attributes(PickaxeItem.createAttributes(Tiers.IRON, 1, -2.8F)));
        *///?} else {
        super(Tiers.IRON, 1, -2.8F, props);
        //?}
    }

    public static boolean shouldStoreInGhost(LivingEntity player, ItemStack stack){
        return player instanceof Player && ((Player)player).getInventory().getFreeSlot() == -1 ;
    }

    public float getDestroySpeed(ItemStack stack, BlockState blockState) {
        return blockState.is(BlockTags.MINEABLE_WITH_PICKAXE) ? 20.0F : 1.0F;
    }

    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity user) {
        if(shouldStoreInGhost(user, stack)){
            if(user instanceof Player){
                Player player = (Player)user;
                player.awardStat(Stats.BLOCK_MINED.get(state.getBlock()));
                player.causeFoodExhaustion(0.005F);
            }
            if(!level.isClientSide()){
                BlockEntity blockentity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
                Block.getDrops(state, (ServerLevel)level, pos, blockentity, user, stack).forEach((item) -> {
                    putItemInGhostInventoryOrDrop(user, stack, item);
                });
                state.spawnAfterBreak((ServerLevel)level, pos, stack, true);
                // NeoForge 1.21 reshaped getExpDrop: fortune and silk touch became enchantment effect
                // components, so it reads them off the tool itself and wants the breaker + stack
                // instead of a pre-computed pair of levels.
                // Forge patches spawnAfterBreak to drop no XP and adds getExpDrop; vanilla's own
                // spawnAfterBreak(..., true) above already popped it, so Fabric must NOT do it again.
                //? if fabric {
                /*int exp = 0;
                *///?} elif neoforge && >=1.21 {
                /*int exp = state.getExpDrop(level, pos, blockentity, user, stack);
                *///?} else {
                int fortuneLevel = AMCompat.enchantLevel(Enchantments.BLOCK_FORTUNE, stack, level);
                int silkTouchLevel = AMCompat.enchantLevel(Enchantments.SILK_TOUCH, stack, level);
                int exp = state.getExpDrop((ServerLevel)level, level.getRandom(), pos, fortuneLevel, silkTouchLevel);
                //?}
                if(exp > 0){
                    state.getBlock().popExperience((ServerLevel)level, pos, exp);
                }
            }
        }
        return super.mineBlock(stack, level, state, pos, user);
    }

    private static void putItemInGhostInventoryOrDrop(LivingEntity user, ItemStack pickaxe, ItemStack item) {
        net.minecraft.core.HolderLookup.Provider provider = user.level().registryAccess();
        CompoundTag compoundtag = AMCompat.getOrCreateTag(pickaxe);
        SimpleContainer container = new SimpleContainer(9);
        if(compoundtag.contains("Items")){
            AMCompat.fromTag(provider, container, AMCompat.getList(compoundtag, "Items", 10));
        }
        if(user instanceof Player){
            Player player = (Player) user;
            if(player.getInventory().add(item)){
                return;
            }else if(container.canAddItem(item)){
                ItemStack leftover = container.addItem(item);
                compoundtag.put("Items", AMCompat.createTag(provider, container));
                AMCompat.setTag(pickaxe, compoundtag);
                item = leftover;

            }
        }
        if(!item.isEmpty()){
            AMCompat.spawnAtLocation(user, item);
        }
    }

    //? if >=1.21.5 {
    /*public void inventoryTick(ItemStack stack, net.minecraft.server.level.ServerLevel level, Entity entity, net.minecraft.world.entity.EquipmentSlot amSlot) {
        super.inventoryTick(stack, level, entity, amSlot);
    *///?} else {
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int i, boolean offhand) {
        super.inventoryTick(stack, level, entity, i, offhand);
    //?}
        if(entity instanceof Player){
            Player player = (Player) entity;
            if(player.tickCount % 3 == 0){
                CompoundTag compoundtag = AMCompat.getOrCreateTag(stack);
                SimpleContainer container = new SimpleContainer(9);
                boolean flag = false;
                if(compoundtag.contains("Items")){
                    AMCompat.fromTag(level.registryAccess(), container, AMCompat.getList(compoundtag, "Items", 10));
                }
                for(int slot = 0; slot < container.getContainerSize(); slot++) {
                    ItemStack stackAt = container.getItem(slot);
                    if(!stackAt.isEmpty() && player.addItem(stackAt)){
                        container.removeItem(slot, stack.getCount());
                        flag = true;
                        break;
                    }
                }
                if (flag) {
                    compoundtag.put("Items", AMCompat.createTag(level.registryAccess(), container));
                    AMCompat.setTag(stack, compoundtag);
                }
            }
        }
    }

    //? if <1.21.2 {
    // >=1.21.2: Item#isValidRepairItem is gone; the phantom-membrane repair is declared as the
    // alexsmobs:repairs/ghostly_pickaxe tag via AMCompat.repairableWith at registration (#95).
    public boolean isValidRepairItem(ItemStack pickaxe, ItemStack stack) {
        return stack.is(Items.PHANTOM_MEMBRANE);
    }
    //?}

    @Override
    //? if >=1.21.5 {
    /*// 1.21.5 replaced the List<Component> with a Consumer<Component> and added a TooltipDisplay.
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> amTooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, context, display, amTooltip, flagIn);
        java.util.List<Component> tooltip = new java.util.ArrayList<Component>() { public boolean add(Component amC) { amTooltip.accept(amC); return true; } };
        net.minecraft.core.HolderLookup.Provider provider = context.registries();
    *///?} elif >=1.20.5 {
    /*// 1.20.5 replaced the nullable Level with a TooltipContext that carries the registries.
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, context, tooltip, flagIn);
        net.minecraft.core.HolderLookup.Provider provider = context.registries();
    *///?} else {
    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        net.minecraft.core.HolderLookup.Provider provider = worldIn == null ? null : worldIn.registryAccess();
    //?}
        CompoundTag compoundtag = AMCompat.getTag(stack);
        if (compoundtag != null && AMCompat.contains(compoundtag, "Items", 9)) {
            SimpleContainer container = new SimpleContainer(9);
            AMCompat.fromTag(provider, container, AMCompat.getList(compoundtag, "Items", 10));
            int i = 0;
            int j = 0;

            for(int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack itemstack = container.getItem(slot);
                if (!itemstack.isEmpty()) {
                    ++j;
                    if (i <= 4) {
                        ++i;
                        MutableComponent mutablecomponent = itemstack.getHoverName().copy();
                        mutablecomponent.append(" x").append(String.valueOf(itemstack.getCount()));
                        tooltip.add(mutablecomponent.withStyle(ChatFormatting.DARK_AQUA));
                    }
                }
            }

            if (j - i > 0) {
                tooltip.add(Component.translatable("container.shulkerBox.more", j - i).withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
            }
        }
    }

    // Forge 1.21 has no damageItem hook left, so ServerEvents calls this from the destroy event.
    public void dropAllContents(Level level, Vec3 vec3, ItemStack pickaxe){
        net.minecraft.core.HolderLookup.Provider provider = level.registryAccess();
        CompoundTag compoundtag = AMCompat.getTag(pickaxe);
        if (compoundtag != null && AMCompat.contains(compoundtag, "Items", 9)) {
            SimpleContainer container = new SimpleContainer(9);
            AMCompat.fromTag(provider, container, AMCompat.getList(compoundtag, "Items", 10));
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack itemstack = container.getItem(slot);
                if (!itemstack.isEmpty()) {
                    ItemEntity itemEntity = new ItemEntity(level, vec3.x, vec3.y, vec3.z, itemstack.copy());
                    if(level.addFreshEntity(itemEntity)){
                        container.removeItem(slot, itemstack.getCount());
                    }
                }
            }
            compoundtag.put("Items", AMCompat.createTag(provider, container));
            AMCompat.setTag(pickaxe, compoundtag);
        }
    }

    public void onDestroyed(ItemEntity itemEntity) {
        dropAllContents(itemEntity.level(), itemEntity.position(), itemEntity.getItem());
    }

    // NeoForge 20.6 kept the generic LivingEntity parameter but swapped the Consumer for a
    // plain Runnable and dropped the RandomSource.
    //? if neoforge && >=1.21 {
    /*// NeoForge 1.21 swapped the Runnable back for the Consumer<Item> vanilla now passes.
    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<net.minecraft.world.item.Item> onBroken) {
        int i = super.damageItem(stack, amount, entity, onBroken);
        if(i + stack.getDamageValue() >= stack.getMaxDamage() && entity != null){
            dropAllContents(entity.level(), entity.position(), stack);
        }
        return i;
    }
    *///?}
    //? if neoforge && >=1.20.6 && <1.21 {
    /*@Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Runnable onBroken) {
        int i = super.damageItem(stack, amount, entity, onBroken);
        if(i + stack.getDamageValue() >= stack.getMaxDamage() && entity != null){
            dropAllContents(entity.level(), entity.position(), stack);
        }
        return i;
    }
    *///?}
    // Forge deleted IForgeItem#damageItem outright in 1.21; ServerEvents#onDestroyItem covers it.
    //? if forge && >=1.20.5 && <1.21 {
    /*// Forge 1.20.5 narrowed IForgeItem#damageItem to a ServerPlayer plus a plain Runnable.
    @Override
    public int damageItem(ItemStack stack, int amount, net.minecraft.util.RandomSource random, @Nullable net.minecraft.server.level.ServerPlayer entity, Runnable onBroken) {
        int i = super.damageItem(stack, amount, random, entity, onBroken);
        if(i + stack.getDamageValue() >= stack.getMaxDamage() && entity != null){
            dropAllContents(entity.level(), entity.position(), stack);
        }
        return i;
    }
    *///?}
    //? if (forge && <1.20.5) || (neoforge && <1.20.6) {
    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<T> onBroken) {
        int i = super.damageItem(stack, amount, entity, onBroken);
        if(i + stack.getDamageValue() >= stack.getMaxDamage() && entity != null){
            dropAllContents(entity.level(), entity.position(), stack);
        }
        return i;
    }
    //?}

    public int getMaxDamage(ItemStack stack) {
        return 700;
    }
}

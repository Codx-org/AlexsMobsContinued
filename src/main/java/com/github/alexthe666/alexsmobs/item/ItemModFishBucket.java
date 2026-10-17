package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.misc.AMCompat;

import com.github.alexthe666.alexsmobs.entity.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.entity.EntityCatfish;
import com.github.alexthe666.alexsmobs.entity.EntityLobster;
import com.github.alexthe666.alexsmobs.entity.util.TerrapinTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

public class ItemModFishBucket extends MobBucketItem {

    // NeoForge 20.6 dropped Forge's deferred MobBucketItem constructor and its protected
    // getFishType(), so the type is held here instead. Entity types are registered before items,
    // so resolving the supplier while building the item is safe.
    // Vanilla's MobBucketItem ctor takes a bare EntityType/Fluid/SoundEvent; only Forge patches in the
    // supplier-taking overload the else arm uses, so Fabric takes the NeoForge arm.
    // (&& binds tighter than ||, so this reads "(neoforge && >=1.20.6) || fabric".)
    //? if (neoforge && >=1.20.6) || fabric {
    /*private final Supplier<? extends EntityType<?>> fishTypeSupplier;

    public ItemModFishBucket(Supplier<? extends EntityType<? extends net.minecraft.world.entity.Mob>> fishTypeIn, Fluid fluid, Item.Properties builder) {
        super(fishTypeIn.get(), fluid, SoundEvents.BUCKET_EMPTY_FISH, builder.stacksTo(1));
        this.fishTypeSupplier = fishTypeIn;
    }

    protected EntityType<?> getFishType() {
        return fishTypeSupplier.get();
    }
    *///?} else {
    public ItemModFishBucket(Supplier<? extends EntityType<? extends net.minecraft.world.entity.Mob>> fishTypeIn, Fluid fluid, Item.Properties builder) {
        super(fishTypeIn, () -> fluid, () -> SoundEvents.BUCKET_EMPTY_FISH, builder.stacksTo(1));
    }
    //?}

    @OnlyIn(value = Dist.CLIENT)
    // 1.20.5 replaced the nullable Level with a TooltipContext; nothing here reads it.
    //? if >=1.21.5 {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> amTooltip, TooltipFlag flagIn) {
        java.util.List<Component> tooltip = new java.util.ArrayList<Component>() { public boolean add(Component amC) { amTooltip.accept(amC); return true; } };
    *///?} elif >=1.20.5 {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
    *///?} else {
    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
    //?}
        EntityType fishType = getFishType();
        if (fishType == AMEntityRegistry.LOBSTER.get()) {
            CompoundTag compoundnbt = AMCompat.getTag(stack);
            if (compoundnbt != null && AMCompat.contains(compoundnbt, "BucketVariantTag", 3)) {
                int i = AMCompat.getInt(compoundnbt, "BucketVariantTag");
                String s = "entity.alexsmobs.lobster.variant_" + EntityLobster.getVariantName(i);
                tooltip.add((Component.translatable(s)).withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC));
            }
        }
        if (fishType == AMEntityRegistry.TERRAPIN.get()) {
            CompoundTag compoundnbt = AMCompat.getTag(stack);
            if (compoundnbt != null && compoundnbt.contains("TerrapinData")) {
                int i = AMCompat.getInt(AMCompat.getCompound(compoundnbt, "TerrapinData"), "TurtleType");
                tooltip.add((Component.translatable(TerrapinTypes.values()[Mth.clamp(i, 0, TerrapinTypes.values().length - 1)].getTranslationName())).withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC));
            }
        }
        if (fishType == AMEntityRegistry.COMB_JELLY.get()) {
            CompoundTag compoundnbt = AMCompat.getTag(stack);
            if (compoundnbt != null && AMCompat.contains(compoundnbt, "BucketVariantTag", 3)) {
                int i = AMCompat.getInt(compoundnbt, "BucketVariantTag");
                String s = "entity.alexsmobs.comb_jelly.variant_" + i;
                tooltip.add((Component.translatable(s)).withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC));
            }
        }
    }

    @Override
    //? if >=1.21.5 {
    /*public void checkExtraContent(net.minecraft.world.entity.LivingEntity player, Level level, ItemStack stack, BlockPos pos) {
    *///?} else {
    public void checkExtraContent(@Nullable Player player, Level level, ItemStack stack, BlockPos pos) {
    //?}
        if (level instanceof ServerLevel) {
            this.spawnFish((ServerLevel)level, stack, pos);
            level.gameEvent(player, GameEvent.ENTITY_PLACE, pos);
        }
    }

    private void spawnFish(ServerLevel serverLevel, ItemStack stack, BlockPos pos) {
        spawnFish(getFishType(), serverLevel, stack, pos);
    }

    /**
     * Shared with {@link ItemCatfishBucket}, which deliberately does NOT extend this class — see
     * #111. Everything the two have in common lives here so the split costs no duplicated logic.
     */
    static void spawnFish(EntityType<?> fishType, ServerLevel serverLevel, ItemStack stack, BlockPos pos) {
        Entity entity = fishType.spawn(serverLevel, stack, (Player)null, pos, MobSpawnType.BUCKET, true, false);
        if (entity instanceof Bucketable) {
            Bucketable bucketable = (Bucketable)entity;
            bucketable.loadFromBucketTag(AMCompat.getOrCreateTag(stack));
            bucketable.setFromBucket(true);
        }
        addExtraAttributes(entity, stack);
    }

    private static void addExtraAttributes(Entity entity, ItemStack stack) {
        if(entity instanceof EntityCatfish catfish){
            if(stack.is(AMItemRegistry.SMALL_CATFISH_BUCKET.get())){
                catfish.setCatfishSize(0);
            }else if(stack.is(AMItemRegistry.MEDIUM_CATFISH_BUCKET.get())){
                catfish.setCatfishSize(1);
            }else if(stack.is(AMItemRegistry.LARGE_CATFISH_BUCKET.get())){
                catfish.setCatfishSize(2);
            }
        }
    }


}

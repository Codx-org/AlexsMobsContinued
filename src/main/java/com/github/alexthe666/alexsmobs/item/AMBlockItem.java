package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.block.AMBlockRegistry;
import com.github.alexthe666.alexsmobs.misc.AMCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.phys.BlockHitResult;
import java.util.function.Supplier;

public class AMBlockItem extends BlockItem implements CustomTabBehavior {

    private final Supplier<Block> blockSupplier;

    public AMBlockItem(Supplier<Block> blockSupplier, Item.Properties props) {
        super((Block)null, props);
        this.blockSupplier = blockSupplier;
    }

    @Override
    public Block getBlock() {
        return blockSupplier.get();
    }

    // On 1.20.1 BlockItem restored the "BlockEntityTag" sub-tag onto the placed block entity.
    // 1.20.5 replaced that with the block_entity_data component, which this mod does not write
    // (see AMCompat#getBlockEntityData), so the same copy is done here instead. Without it the
    // leafcutter anthill loses its ants and the terrapin egg its parents when replaced.
    //? if >=1.20.5 {
    /*@Override
    protected boolean updateCustomBlockEntityTag(net.minecraft.core.BlockPos pos, Level level, @javax.annotation.Nullable Player player, ItemStack stack, net.minecraft.world.level.block.state.BlockState state) {
        boolean applied = super.updateCustomBlockEntityTag(pos, level, player, stack, state);
        CompoundTag stashed = AMCompat.getTagElement(stack, "BlockEntityTag");
        if (stashed == null || level.isClientSide()) {
            return applied;
        }
        net.minecraft.world.level.block.entity.BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return applied;
        }
        net.minecraft.core.HolderLookup.Provider provider = level.registryAccess();
        CompoundTag merged = blockEntity.saveCustomOnly(provider);
        merged.merge(stashed);
        AMCompat.loadCustomOnly(blockEntity, merged, provider);
        blockEntity.setChanged();
        return true;
    }
    *///?}

    public boolean canFitInsideCraftingRemainingItems() {
        return !(blockSupplier.get() instanceof ShulkerBoxBlock);
    }

    public void onDestroyed(ItemEntity p_150700_) {
        if (this.blockSupplier.get() instanceof ShulkerBoxBlock) {
            ItemStack itemstack = p_150700_.getItem();
            CompoundTag compoundtag = AMCompat.getBlockEntityData(itemstack);
            if (compoundtag != null && AMCompat.contains(compoundtag, "Items", 9)) {
                ListTag listtag = AMCompat.getList(compoundtag, "Items", 10);
                net.minecraft.core.HolderLookup.Provider provider = p_150700_.level().registryAccess();
                java.util.stream.Stream<ItemStack> contents = listtag.stream().map(CompoundTag.class::cast).map(tag -> AMCompat.loadItem(provider, tag));
                // 1.20.5 widened ItemUtils#onContainerDestroyed's second parameter from a Stream to
                // an Iterable.
                //? if >=1.20.5 && <26
                //ItemUtils.onContainerDestroyed(p_150700_, contents::iterator);
                //? if >=26 || <1.20.5
                ItemUtils.onContainerDestroyed(p_150700_, contents);
            }
        }
    }


    // 1.20.5 removed IForgeItem#canBeHurtBy. The one thing it protected here — the
    // transmutation table surviving explosions — is now done by dropping its ItemEntity out of
    // ExplosionEvent.Detonate's affected-entity list; see ServerEvents#onExplosionDetonate.
    //? if <1.20.5 {
    public boolean canBeHurtBy(DamageSource damage) {
        return super.canBeHurtBy(damage) && (this != AMBlockRegistry.TRANSMUTATION_TABLE.get().asItem() || !damage.is(DamageTypeTags.IS_EXPLOSION));
    }
    //?}

    @Override
    public void fillItemCategory(CreativeModeTab.Output contents) {
        if(blockSupplier.equals(AMBlockRegistry.SAND_CIRCLE) || blockSupplier.equals(AMBlockRegistry.RED_SAND_CIRCLE)){

        }else{
            contents.accept(this);
        }
    }

    public InteractionResult useOn(UseOnContext context) {
        return blockSupplier.equals(AMBlockRegistry.TRIOPS_EGGS) ? InteractionResult.PASS : super.useOn(context);
    }

    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if(blockSupplier.equals(AMBlockRegistry.TRIOPS_EGGS)){
            BlockHitResult blockhitresult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
            BlockHitResult blockhitresult1 = blockhitresult.withPosition(blockhitresult.getBlockPos().above());
            InteractionResult interactionresult = super.useOn(new UseOnContext(player, hand, blockhitresult1));
            return AMCompat.holder(interactionresult, player.getItemInHand(hand));
        }else{
            return super.use(level, player, hand);
        }
    }
}

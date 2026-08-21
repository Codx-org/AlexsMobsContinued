package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.entity.AMEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;

import javax.annotation.Nullable;

/**
 * The <b>medium</b> and <b>large</b> catfish buckets — deliberately a {@link BucketItem} rather
 * than a {@code MobBucketItem}, which is what every other bucket in this mod (including the
 * <b>small</b> catfish bucket) still is.
 *
 * <p>Catfish are the one entity in the mod with three buckets, and {@code MobBucketItem} holds the
 * entity type in a field that third parties read as a <i>key</i>: Sea Life's
 * {@code HatcheryBlock.onLoadComplete} walks {@code BuiltInRegistries.ITEM}, filters
 * {@code instanceof MobBucketItem} and does {@code ImmutableMap.Builder.put(bucket.type, item)}
 * (read out of the 21.11.0 bytecode). A guava builder rejects a duplicate key, so three buckets
 * sharing one entity type are a hard startup crash for everyone with both mods installed — and
 * the exception prints {@code entity.alexsmobs.catfish} because {@code EntityType#toString}
 * returns {@code getDescriptionId()}, which is why it reads like a translation-key clash and is
 * not one. Upstream Alex's Mobs has the same three-buckets-one-type shape, so the item keys were
 * never the lever. Keeping the <i>small</i> bucket as the {@code MobBucketItem} preserves the
 * catfish → bucket mapping that such integrations actually want. See #111.
 *
 * <p>{@code MobBucketItem} only adds {@code checkExtraContent} and {@code playEmptySound} over
 * {@code BucketItem} (javap, every version 1.20.1 → 26.2), and this mod already overrode the
 * first, so both are re-declared here and nothing else is lost. The spawn itself is
 * {@link ItemModFishBucket#spawnFish}, whose size branch keys off the stack, not the class.
 */
public class ItemCatfishBucket extends BucketItem {

    public ItemCatfishBucket(Fluid fluid, Item.Properties builder) {
        super(fluid, builder.stacksTo(1));
    }

    @Override
    // 1.21.5 widened the player parameter from Player to LivingEntity — the same boundary
    // ItemModFishBucket gates, checked in the vanilla AND the Forge-patched bytecode.
    //? if >=1.21.5 {
    /*public void checkExtraContent(net.minecraft.world.entity.LivingEntity player, Level level, ItemStack stack, BlockPos pos) {
    *///?} else {
    public void checkExtraContent(@Nullable Player player, Level level, ItemStack stack, BlockPos pos) {
    //?}
        if (level instanceof ServerLevel) {
            ItemModFishBucket.spawnFish(AMEntityRegistry.CATFISH.get(), (ServerLevel)level, stack, pos);
            level.gameEvent(player, GameEvent.ENTITY_PLACE, pos);
        }
    }

    @Override
    // Vanilla's MobBucketItem plays its emptySound here; BucketItem would play the water one, so
    // the fish sound is restored by hand. LevelAccessor#playSound takes Player below and Entity
    // above, so the same call text is valid on both arms.
    //? if >=1.21.5 {
    /*protected void playEmptySound(net.minecraft.world.entity.LivingEntity player, LevelAccessor level, BlockPos pos) {
    *///?} else {
    protected void playEmptySound(@Nullable Player player, LevelAccessor level, BlockPos pos) {
    //?}
        level.playSound(player, pos, SoundEvents.BUCKET_EMPTY_FISH, SoundSource.NEUTRAL, 1.0F, 1.0F);
    }

}

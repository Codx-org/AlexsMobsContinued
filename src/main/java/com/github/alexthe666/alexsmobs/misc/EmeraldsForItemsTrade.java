package com.github.alexthe666.alexsmobs.misc;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
//? if <26
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.ItemLike;

// 26.1 made villager trades registry entries (ResourceKey<VillagerTrade>), datapack-driven:
// VillagerTrades.ItemListing is gone and so are NeoForge's VillagerTradesEvent /
// WandererTradesEvent. The mod's trades are dropped there — see the porting notes.
//? if >=26
/*public class EmeraldsForItemsTrade {*/
//? if <26
public class EmeraldsForItemsTrade implements VillagerTrades.ItemListing {
    private final Item tradeItem;
    private final int count;
    private final int maxUses;
    private final int xpValue;
    private final float priceMultiplier;

    public EmeraldsForItemsTrade(ItemLike p_i50539_1_, int p_i50539_2_, int p_i50539_3_, int p_i50539_4_) {
        this.tradeItem = p_i50539_1_.asItem();
        this.count = p_i50539_2_;
        this.maxUses = p_i50539_3_;
        this.xpValue = p_i50539_4_;
        this.priceMultiplier = 0.05F;
    }

    public MerchantOffer getOffer(Entity p_221182_1_, RandomSource p_221182_2_) {
        // 1.20.5 typed an offer's cost side as ItemCost instead of a plain ItemStack.
        //? if >=1.20.5
        //return new MerchantOffer(new net.minecraft.world.item.trading.ItemCost(this.tradeItem, 1), new ItemStack(Items.EMERALD, this.count), this.maxUses, this.xpValue, this.priceMultiplier);
        //? if <1.20.5
        return new MerchantOffer(new ItemStack(this.tradeItem, 1), new ItemStack(Items.EMERALD, this.count), this.maxUses, this.xpValue, this.priceMultiplier);
    }
}
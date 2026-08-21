package com.github.alexthe666.alexsmobs.fabric.forge.event.village;

// Gated out on >=26 for the same reason as VillagerTradesEvent — see the note there.
//? if <26 {
import com.github.alexthe666.alexsmobs.fabric.forge.eventbus.api.Event;
import java.util.List;
import net.minecraft.world.entity.npc.VillagerTrades;

/**
 * Fabric stand-in for {@code net.minecraftforge.event.village.WandererTradesEvent} — the wandering
 * trader's fourteen-odd Alex's Mobs offers (animal dictionary, acacia blossom, oothecae, buckets,
 * eggs, furs, scutes …), all behind {@code AMConfig.wanderingTraderOffers}.
 *
 * <p>Only the two-getter shape is stubbed. Forge {@code >=1.21.5} replaced them with a
 * {@code getPools()} list of rolls-plus-entries — but that arm is gated {@code forge && >=1.21.5 &&
 * <26}, so Fabric takes the {@code elif <26} arm on every node and never needs {@code Pool}.
 * Deliberately not stubbing it keeps the unreachable half of the API from looking supported.
 *
 * <p>Both lists are live and mutable, and like the villager event this fires once at server start —
 * firing it per trader would multiply the offers.
 */
public class WandererTradesEvent extends Event {

    private final List<VillagerTrades.ItemListing> genericTrades;
    private final List<VillagerTrades.ItemListing> rareTrades;

    public WandererTradesEvent(List<VillagerTrades.ItemListing> genericTrades, List<VillagerTrades.ItemListing> rareTrades) {
        this.genericTrades = genericTrades;
        this.rareTrades = rareTrades;
    }

    public List<VillagerTrades.ItemListing> getGenericTrades() {
        return genericTrades;
    }

    public List<VillagerTrades.ItemListing> getRareTrades() {
        return rareTrades;
    }
}
//?}

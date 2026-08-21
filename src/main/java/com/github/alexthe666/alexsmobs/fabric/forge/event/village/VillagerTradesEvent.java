package com.github.alexthe666.alexsmobs.fabric.forge.event.village;

// Three arms, whole-class, because Stonecutter blocks are siblings and never nest — the version
// split below could not be expressed inside a single class body that is itself gated.
//
//  <1.21.5  getType() is the profession itself.
//  <26      1.21.5 moved villager professions into a registry, so the profession constants (and
//           Forge's event accessor with them) became ResourceKey<VillagerProfession>.
//  >=26     MC made villager trades datapack-driven registry entries and deleted
//           VillagerTrades.ItemListing, so there is nothing left for this event to carry. The file
//           keeps only its package declaration, which is a legal (and empty) compilation unit. Its
//           one handler in ServerEvents is gated `<26` to match.
//? if <1.21.5 {
import com.github.alexthe666.alexsmobs.fabric.forge.eventbus.api.Event;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.List;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;

/**
 * Fabric stand-in for {@code net.minecraftforge.event.village.VillagerTradesEvent} — the fisherman's
 * ambergris-for-emeralds offer.
 *
 * <p>Fired <b>once per profession at server start</b>, not per villager: the map it hands out is
 * the profession's shared trade table. The Fabric dispatcher must therefore fire it exactly once
 * for each profession, from server-start, or the trade is appended twice and the fisherman offers
 * it in duplicate.
 *
 * <p>{@link #getTrades()} is keyed by career level (1–5); the handler reads level 2, appends, and
 * puts it back. It is live and mutable.
 */
public class VillagerTradesEvent extends Event {

    private final VillagerProfession type;
    private final Int2ObjectMap<List<VillagerTrades.ItemListing>> trades;

    public VillagerTradesEvent(VillagerProfession type, Int2ObjectMap<List<VillagerTrades.ItemListing>> trades) {
        this.type = type;
        this.trades = trades;
    }

    public VillagerProfession getType() {
        return type;
    }

    public Int2ObjectMap<List<VillagerTrades.ItemListing>> getTrades() {
        return trades;
    }
}
//?} elif <26 {
/*import com.github.alexthe666.alexsmobs.fabric.forge.eventbus.api.Event;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;

/^*
 * Fabric stand-in for {@code net.minecraftforge.event.village.VillagerTradesEvent} — the fisherman's
 * ambergris-for-emeralds offer.
 *
 * <p>Fired <b>once per profession at server start</b>, not per villager: the map it hands out is
 * the profession's shared trade table. The Fabric dispatcher must therefore fire it exactly once
 * for each profession, from server-start, or the trade is appended twice and the fisherman offers
 * it in duplicate.
 *
 * <p>{@link #getTrades()} is keyed by career level (1–5); the handler reads level 2, appends, and
 * puts it back. It is live and mutable.
 ^/
public class VillagerTradesEvent extends Event {

    private final ResourceKey<VillagerProfession> type;
    private final Int2ObjectMap<List<VillagerTrades.ItemListing>> trades;

    public VillagerTradesEvent(ResourceKey<VillagerProfession> type, Int2ObjectMap<List<VillagerTrades.ItemListing>> trades) {
        this.type = type;
        this.trades = trades;
    }

    public ResourceKey<VillagerProfession> getType() {
        return type;
    }

    public Int2ObjectMap<List<VillagerTrades.ItemListing>> getTrades() {
        return trades;
    }
}
*///?}

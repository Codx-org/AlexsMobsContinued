package com.github.alexthe666.alexsmobs.misc;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The Fabric driver for this mod's four global loot modifiers.
 *
 * <p>Forge and NeoForge run them from a datapack: {@code data/forge/loot_modifiers/
 * global_loot_modifiers.json} lists the four entries, each entry's own json carries a
 * {@code forge:loot_table_id} condition, and Forge's loot machinery evaluates that condition
 * against an id it patches onto {@code LootContext}. None of those three pieces exists on Fabric —
 * not the dispatch file, not the condition type, and not the queried-table-id on the context — so
 * this class supplies all three itself.
 *
 * <p><b>The condition is not re-implemented, it is inverted.</b> Every one of the four conditions
 * is a {@code loot_table_id} test and nothing else, i.e. "am I this table?". Rather than teach
 * Fabric to answer that question about a context, this class keys a map on the table and asks it
 * up front — so by the time an appender runs, the only condition those modifiers have is already
 * satisfied. That is why {@link #resolve} hands back the {@code doApply} body rather than the
 * modifier's {@code apply}: {@code apply} would re-test a {@code conditions} array that is
 * necessarily empty here, and an empty or-of-conditions is <b>always false</b>, which would
 * silently drop every drop.
 *
 * <p>The four ids are the source of truth in {@code data/alexsmobs/loot_modifiers/*.json} for the
 * other two loaders, and are restated here rather than parsed out of them: those files are Forge
 * datapack entries whose {@code conditions} array cannot be deserialized on this loader at all
 * (the {@code forge:loot_table_id} condition type is unregistered), so reading them would mean
 * hand-parsing a format only to recover four strings. <b>If a fifth modifier is ever added, or a
 * table id changes, this map must be updated alongside the json</b> — there is no test that pairs
 * them, because on Forge the json is data and here it is code.
 *
 * <p>Compiled on all 49 nodes and referenced only from {@code mixin/fabric/FabricLootTableMixin},
 * which is stripped everywhere else. Nothing on Forge or NeoForge loads this class, so its static
 * initialiser never runs there.
 *
 * @see com.github.alexthe666.alexsmobs.fabric.common.loot.IGlobalLootModifier
 */
public final class AMLootModifiers {

    /**
     * The half of a loot modifier that actually does something. Each of the four modifier classes
     * declares {@code doApply} {@code protected}, which is reachable from here only because this
     * class shares their package — that is the reason it lives in {@code misc} rather than under
     * {@code fabric/}.
     */
    @FunctionalInterface
    public interface Appender {
        ObjectArrayList<ItemStack> append(ObjectArrayList<ItemStack> generatedLoot, LootContext context);
    }

    /**
     * Each modifier's constructor wants the {@code conditions} array it was decoded with. Nothing
     * on this loader decodes one, and nothing reads it back — see the class javadoc for why the
     * conditions are answered by the map key instead.
     */
    private static final LootItemCondition[] NO_CONDITIONS = new LootItemCondition[0];

    private static final Map<ResourceLocation, Appender> BY_TABLE = byTable();

    private static Map<ResourceLocation, Appender> byTable() {
        // LinkedHashMap, not Map.of: resolve() iterates this, and a stable order keeps a
        // mis-resolution reproducible rather than differing run to run.
        Map<ResourceLocation, Appender> map = new LinkedHashMap<>(4);
        map.put(AMCompat.rl("minecraft", "blocks/jungle_leaves"),
                new BananaLootModifier(NO_CONDITIONS)::doApply);
        map.put(AMCompat.rl("minecraft", "blocks/acacia_leaves"),
                new BlossomLootModifier(NO_CONDITIONS)::doApply);
        map.put(AMCompat.rl("minecraft", "gameplay/piglin_bartering"),
                new PigshoesLootModifier(NO_CONDITIONS)::doApply);
        // ancient_dart.json lists jungle_temple AND jungle_temple_dispenser; on Forge the two are
        // two conditions on one modifier, or-ed together. Two map entries is the same thing.
        AncientDartLootModifier dart = new AncientDartLootModifier(NO_CONDITIONS);
        map.put(AMCompat.rl("minecraft", "chests/jungle_temple"), dart::doApply);
        map.put(AMCompat.rl("minecraft", "chests/jungle_temple_dispenser"), dart::doApply);
        return map;
    }

    private AMLootModifiers() {
    }

    /**
     * Identifies {@code table} by resolving each of the four ids through the running server and
     * comparing instances. Returns {@code null} when this table is none of them, which is the
     * answer for all but a handful of the hundreds of tables in a game.
     *
     * <p>Callers are expected to cache the result <b>per {@code LootTable} instance</b>. That makes
     * this run at most once per table per reload and, more importantly, makes the cache
     * self-invalidating: {@code /reload} builds fresh {@code LootTable} objects, so a stale answer
     * cannot outlive the tables it was about.
     *
     * <p>Instance comparison, not id comparison, because the id is exactly what is unavailable
     * here. Fabric API solves the same problem the other way round — a reverse lookup of the table
     * in the registry to recover its {@code Holder} — but that only exists from 1.21.6, needs
     * MixinExtras, and this tree bundles neither.
     */
    public static Appender resolve(LootTable table, LootContext context) {
        net.minecraft.server.level.ServerLevel level = context.getLevel();
        if (level == null || level.getServer() == null) {
            return null;
        }
        for (Map.Entry<ResourceLocation, Appender> entry : BY_TABLE.entrySet()) {
            LootTable found = AMPlatform.lootTableById(level.getServer(), entry.getKey());
            // From 1.20.5 a miss returns LootTable.EMPTY rather than null. Without this guard a
            // roll of the empty table would match the first id that failed to resolve.
            if (found == null || found == LootTable.EMPTY) {
                continue;
            }
            if (found == table) {
                return entry.getValue();
            }
        }
        return null;
    }
}

package com.github.alexthe666.alexsmobs.fabric.common;

// Fabric API's convention tags were re-namespaced from v1 to v2 for MC 1.20.5. The two packages
// are not a rename — several constants changed name across the move as well (see each field).
//? if <1.20.5 {
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalBlockTags;
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalItemTags;
//?} else {
/*import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
*///?}
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Fabric stand-in for {@code net.minecraftforge.common.Tags}, reached by the Fabric-only
 * {@code !fab-tags} replacement rule — the same relocated compat-namespace pattern as
 * {@code fabric/registries/DeferredRegister} and {@code fabric/client/IClientItemExtensions}.
 *
 * <p>Every use in this tree is unqualified ({@code Tags.Items.X} / {@code Tags.Blocks.X}), so the
 * fully-qualified name appears only on the import line and one rule covers all ten call sites.
 *
 * <p><b>These delegate; they do not re-declare ids.</b> Fabric API ships the same convention tags
 * under the same {@code c:} namespace that Forge 26 and NeoForge 1.20.5+ moved to, and — unlike a
 * hand-written {@code TagKey.create(...)} — a delegate is <i>populated</i>: Fabric API's own data
 * pack fills them, so {@code #c:tools/shear} really does contain shears. Writing the ids out by
 * hand would compile and then silently match nothing.
 *
 * <p>Only the four tags this mod actually names are here. Adding one means checking Fabric API's
 * {@code ConventionalItemTags}/{@code ConventionalBlockTags} for the real constant rather than
 * guessing the path — the names are not a mechanical translation of Forge's
 * ({@code SHEARS} → {@code SHEAR_TOOLS}, {@code CHESTS_WOODEN} → {@code WOODEN_CHESTS}).
 */
public final class Tags {

    private Tags() {
    }

    public static final class Items {

        private Items() {
        }

        /**
         * {@code c:seeds} — the jerboa's beg goal.
         *
         * <p>⚠️ The boundary here is the pinned <b>fabric-api</b>, not the MC version:
         * {@code ConventionalItemTags.SEEDS} only exists from convention-tags-v2 {@code 2.10}-ish, and
         * the newest fabric-api published for 1.20.6 / 1.21 / 1.21.2 / 1.21.3 all predate it (1.21.1 is
         * the lone exception — its pin is a late backport). Rather than gate on four scattered nodes,
         * every pre-1.21.4 node delegates to the vanilla tag, which is exactly the set a beg goal
         * wants and is genuinely populated on all of them. The cost is modded seeds, which {@code
         * c:seeds} would have covered.
         */
        //? if >=1.21.4 {
        /*public static final TagKey<Item> SEEDS = ConventionalItemTags.SEEDS;
        *///?} else {
        public static final TagKey<Item> SEEDS = net.minecraft.tags.ItemTags.VILLAGER_PLANTABLE_SEEDS;
        //?}

        /**
         * {@code c:tools/shear}. Forge spelled this {@code SHEARS}, then {@code TOOLS_SHEARS}
         * (1.20.5–1.20.6 only), then {@code TOOLS_SHEAR}; Fabric has always called it
         * {@code SHEAR_TOOLS}. The rename rules for the other two loaders live in loader-specific
         * replacement groups, so the source Fabric sees is still the original {@code Tags.Items.SHEARS}.
         */
        //? if >=1.21 {
        /*public static final TagKey<Item> SHEARS = ConventionalItemTags.SHEAR_TOOLS;
        *///?} elif >=1.20.5 {
        /*// v2 shipped it as the plural SHEARS_TOOLS first and settled on SHEAR_TOOLS for 1.21.
        public static final TagKey<Item> SHEARS = ConventionalItemTags.SHEARS_TOOLS;
        *///?} else {
        // v1 called it plainly SHEARS, which is also what Forge called it — same id, {@code c:shears}.
        public static final TagKey<Item> SHEARS = ConventionalItemTags.SHEARS;
        //?}

        /** {@code c:chests/wooden} — what an elephant will accept as a howdah. */
        //? if >=1.20.5 {
        /*public static final TagKey<Item> CHESTS_WOODEN = ConventionalItemTags.WOODEN_CHESTS;
        *///?} else {
        // v1 had no wooden-only split, so this widens to {@code c:chests} on 1.20.1/1.20.4: an
        // elephant there will also take an ender/trapped chest as a howdah. Vanilla has no narrower
        // tag to delegate to, and a hand-written c:chests/wooden would be empty on those nodes.
        public static final TagKey<Item> CHESTS_WOODEN = ConventionalItemTags.CHESTS;
        //?}
    }

    public static final class Blocks {

        private Blocks() {
        }

        /** {@code c:ores} — what the underminer digs for. */
        public static final TagKey<Block> ORES = ConventionalBlockTags.ORES;
    }
}

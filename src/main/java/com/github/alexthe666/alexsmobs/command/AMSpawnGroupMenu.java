package com.github.alexthe666.alexsmobs.command;

import codx.codxlib.api.ui.menu.CodxMenu;
import codx.codxlib.api.ui.menu.CodxMenuButton;
import codx.codxlib.api.ui.menu.CodxMenuClick;
import codx.codxlib.api.ui.menu.CodxMenuLayout;
import codx.codxlib.api.ui.menu.PagedMenuBuilder;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.config.ConfigHolder;
import com.github.alexthe666.alexsmobs.misc.AMCompat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * The editor page behind {@code spawnGroupSizes} — how many of a mob one natural spawn places.
 *
 * <p><b>Why this file exists.</b> {@code spawnGroupSizes} is a list of {@code "<id>|<min>|<max>"}
 * strings, and a chest menu has no keyboard, so CodxLib's generated settings menu can only show a
 * list read-only and point at {@code /amc config set}. That is a fine answer for a list of ids and a
 * poor one here: the entries are two numbers per mob, and the mobs and their stock pack sizes are
 * something this mod knows and the library cannot. {@link
 * codx.codxlib.api.ui.menu.CodxSettingsMenu.Builder#editor} is the seam that lets a mod hand one
 * setting a page of its own; this is that page, wired up in {@link AMConfigCommand#openMenu}.
 *
 * <p>Two adjusters per mob rather than a drill-down page per mob: they are CodxLib's stock
 * {@code adjustInt} widget, so left is −, right is +, shift is the big step, and there is no
 * click convention here that is not already true everywhere else in the menu.
 *
 * <p><b>The mob list is not written down anywhere.</b> It is whatever passed through {@link
 * AMConfig#spawnGroupSize} while the world's registries were built, which is exactly the set of
 * spawn entries this mod adds. A mob added to {@code AMWorldRegistry} appears here on its own.
 */
public final class AMSpawnGroupMenu {

    /** Ceiling for the adjusters. Vanilla imposes none; this is "no sane pack wants more". */
    private static final int MAX_GROUP = 64;

    /** 14 mobs to a page — two tiles each in the same inset 7×4 grid the settings menu uses. */
    private static final int MOBS_PER_PAGE = 14;

    private static final int SLOT_BACK = 45;
    private static final int SLOT_CLEAR = 46;

    private AMSpawnGroupMenu() {
    }

    /**
     * Opens the editor. Matches {@code CodxSettingsMenu.Editor}, so it can be passed as a method
     * reference; {@code back} returns the player to the settings page they clicked from.
     */
    public static void open(ServerPlayer player, Runnable back) {
        List<String> mobs = new ArrayList<>(AMConfig.spawnGroupDefaults().keySet());
        PagedMenuBuilder menu = CodxMenu.paged("§6§lSpawn Group Sizes")
                .onChange(ConfigHolder::save);

        if (mobs.isEmpty()) {
            // Only reachable before a world's registries have been built, which a player cannot
            // normally be present for. Say so rather than showing an empty page.
            menu.page("Spawn Group Sizes", layout -> layout.info(22, Items.BARRIER,
                    "§cNo spawn entries yet",
                    "§7This mod's spawn entries are collected as a",
                    "§7world loads. Re-open this once one has."));
        } else {
            int pages = (mobs.size() + MOBS_PER_PAGE - 1) / MOBS_PER_PAGE;
            for (int page = 0; page < pages; page++) {
                List<String> slice = mobs.subList(page * MOBS_PER_PAGE,
                        Math.min(mobs.size(), (page + 1) * MOBS_PER_PAGE));
                menu.page("Spawn Group Sizes", layout -> {
                    for (int i = 0; i < slice.size(); i++) {
                        place(layout, i * 2, slice.get(i));
                    }
                });
            }
        }

        menu.decorate(layout -> {
            layout.action(SLOT_BACK, Items.ARROW, "§a§l← Back", click -> back.run(), "§7Go back");
            layout.action(SLOT_CLEAR, Items.BARRIER, "§c§lClear All Overrides",
                    click -> {
                        AMConfig.clearSpawnGroupOverrides();
                        click.markChanged();
                    },
                    "§7Puts every mob on this page back on",
                    "§7the pack size this mod ships with.",
                    "",
                    "§8Lines you added by hand for another",
                    "§8mod's mobs are left alone.");
        });
        menu.open(player);
    }

    /** One mob's pair of adjusters, at the next two slots of the inset grid. */
    private static void place(CodxMenuLayout layout, int index, String id) {
        String label = prettify(id);
        Item icon = spawnEgg(id);
        int[] def = AMConfig.spawnGroupDefaults().get(id);

        layout.adjustInt(slotFor(index), icon, label + " §7Min", () -> current(id)[0],
                value -> setMin(id, value), CodxMenuButton.Step.of(1, 5).min(1).max(MAX_GROUP),
                lore(id, def));
        layout.adjustInt(slotFor(index + 1), Items.REPEATER, label + " §7Max", () -> current(id)[1],
                value -> setMax(id, value), CodxMenuButton.Step.of(1, 5).min(1).max(MAX_GROUP),
                lore(id, def));
    }

    /**
     * ⚠️ Everything here must be true for the life of the menu. The widget re-renders its icon from
     * the getter on every click, but its <b>lore is captured once</b> when the page is built, so a
     * line reporting the current value would freeze at whatever it said when the menu was opened.
     * The widget already prints {@code Current:} live above this; what belongs here is the mod's own
     * number, which never moves, so the player can see at a glance what they have changed.
     */
    private static String[] lore(String id, int[] def) {
        return new String[]{
                "§8" + id,
                "§7This mod ships: §f" + (def == null ? "?" : range(def)),
                "",
                "§7How many spawn together in one group.",
                "§7Spawn weight decides how §foften§7 a",
                "§7group appears; this is how §fbig§7 it is.",
                "",
                "§6Takes effect when a world next loads."};
    }

    /** The size in force for a mob: its override if it has one, else what this mod ships. */
    private static int[] current(String id) {
        int[] override = AMConfig.spawnGroupOverride(id);
        if (override != null) {
            return override;
        }
        int[] def = AMConfig.spawnGroupDefaults().get(id);
        return def == null ? new int[]{1, 1} : new int[]{def[0], def[1]};
    }

    // Min pushes max up ahead of it and max pushes min down, so the pair can never be crossed and a
    // click never silently does nothing. Landing back on the mod's own numbers removes the entry
    // rather than writing one that says what the default already said.
    private static void setMin(String id, int min) {
        int[] now = current(id);
        store(id, min, Math.max(now[1], min));
    }

    private static void setMax(String id, int max) {
        int[] now = current(id);
        store(id, Math.min(now[0], max), max);
    }

    private static void store(String id, int min, int max) {
        int[] def = AMConfig.spawnGroupDefaults().get(id);
        boolean isDefault = def != null && def[0] == min && def[1] == max;
        AMConfig.setSpawnGroupOverride(id, isDefault ? null : new int[]{min, max});
    }

    private static String range(int[] size) {
        return size[0] == size[1] ? String.valueOf(size[0]) : size[0] + "-" + size[1];
    }

    /**
     * This mod's spawn eggs are registered as {@code spawn_egg_<mob>}, not the vanilla
     * {@code <mob>_spawn_egg}, so the id has to be rebuilt rather than guessed at. Anything without
     * one — another mod's mob named in the list — falls back to a plain egg.
     */
    private static Item spawnEgg(String id) {
        int colon = id.indexOf(':');
        String namespace = colon < 0 ? "alexsmobs" : id.substring(0, colon);
        String path = colon < 0 ? id : id.substring(colon + 1);
        return BuiltInRegistries.ITEM.getOptional(AMCompat.rl(namespace, "spawn_egg_" + path))
                .orElse(Items.EGG);
    }

    /** {@code alexsmobs:bald_eagle} → {@code Bald Eagle}. */
    private static String prettify(String id) {
        String path = id.substring(id.indexOf(':') + 1);
        StringBuilder out = new StringBuilder(path.length());
        boolean upper = true;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '_') {
                out.append(' ');
                upper = true;
            } else {
                out.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
        }
        return out.toString();
    }

    /** Index → slot in the inset grid: rows 1–4, columns 1–7, the same shape as the settings menu. */
    private static int slotFor(int index) {
        return 10 + (index / 7) * 9 + (index % 7);
    }
}

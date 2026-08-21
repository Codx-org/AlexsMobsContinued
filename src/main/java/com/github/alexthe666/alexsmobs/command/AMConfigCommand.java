package com.github.alexthe666.alexsmobs.command;

import codx.codxlib.api.settings.CodxSettings;
import codx.codxlib.api.settings.CodxSettingsCommand;
import codx.codxlib.api.ui.menu.CodxSettingsMenu;
import com.github.alexthe666.alexsmobs.config.ConfigHolder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

import java.util.Locale;
import java.util.Set;

/**
 * {@code /amc config} — reads and writes {@code config/amc.json} from inside the game, as a command
 * or as a chest menu.
 *
 * <p><b>This file declares presentation only.</b> The whole tree — {@code list}, {@code search},
 * {@code get}, {@code set}, {@code reset}, {@code reload}, {@code save}, {@code menu}, with
 * tab-completion, type checking and range clamping on all 249 options — is generated from the spec
 * by {@link CodxSettingsCommand#node}, and the menu by {@link CodxSettingsMenu}. Adding an option to
 * {@code config/CommonConfig.java} makes it appear in the file, the command <i>and</i> the menu with
 * no change here; the three things this class supplies that a library cannot guess are the restart
 * note, the per-mob grouping of the {@code spawning} category, and the icons.
 *
 * <p><b>Why it exists at all.</b> All 249 settings used to be reachable only by editing a file next
 * to a stopped server. An owner who wanted to turn one mob's spawns off had to take the server down,
 * and on a client install there was no way to see what the options even were.
 *
 * <p><b>Operators only</b>, on every node — {@link CodxSettingsCommand#isOperator}, which also lets
 * anyone in singleplayer through. It is registered on the server command tree (not the client one),
 * so it behaves the same in singleplayer, on a LAN world and on a dedicated server, and a non-op
 * sees nothing in tab-completion.
 */
public final class AMConfigCommand {

    /**
     * The options in {@code spawning}/{@code uniqueSpawning} that <b>are</b> read live, despite
     * their category. Everything else in those two categories is consumed once per world load.
     *
     * <p>Each was checked at its read site: the farseer pair is read per spawn attempt in
     * {@code EntityFarseer#isFarseerArea}, and the beached-whale trio per tick in
     * {@code world/BeachedCachalotWhaleSpawner}. The rest of both categories reaches the game only
     * through {@code world/AMWorldRegistry}, which runs while a world is being built.
     */
    private static final Set<String> LIVE_SPAWN_OPTIONS = Set.of(
            "restrictfarseerspawns",
            "farseerborderspawndistance",
            "beachedcachalotwhales",
            "beachedcachalotwhalespawnchance",
            "beachedcachalotwhalespawndelay");

    /**
     * Printed under an edit that will not take effect until a world loads. {@code null} — the note
     * is skipped — for anything read live, and shown unconditionally for a bulk edit, which is
     * handed a null value and can hardly avoid touching a spawn setting.
     */
    private static final CodxSettingsCommand.ChangeNote RELOAD_NOTE = changed ->
            changed == null || needsWorldReload(changed)
                    ? "Spawn settings are applied when a world loads — restart the server "
                      + "(or quit to the title screen and re-enter) for those to take effect."
                    : null;

    private AMConfigCommand() {
    }

    /**
     * Builds the tree. Both loaders' registration seams hand out the same
     * {@code CommandDispatcher<CommandSourceStack>} on every node from 1.20.1 to 26.2 — Forge and
     * NeoForge from {@code RegisterCommandsEvent}, Fabric from {@code CommandRegistrationCallback}
     * — so unlike {@code client/command/AMShieldPoseCommand} this one needs no type parameter.
     *
     * <p>Registered as {@code /amc config} rather than through
     * {@link CodxSettingsCommand#register} so that {@code /amc} stays this mod's own root, ready for
     * whatever else wants to live under it.
     *
     * <p>{@code /amc} is the name: the mod is Alex's <b>M</b>obs <b>C</b>ontinued. It was {@code
     * /aac} from {@code 2.0.15} through {@code 2.1.5} — a spelling nobody guesses, and one letter
     * from the sibling mod's {@code /acc} — so the two swapped round in {@code 2.1.6}.
     *
     * <p>{@code /aac} survives as a Brigadier <b>redirect</b> onto the root, not a second tree —
     * one node, so the two spellings can never drift apart, and everything under {@code /amc}
     * answers to either. It is kept only so that a server script or macro written against the old
     * name keeps working; it is deliberately undocumented. ⚠️ A redirect has no executor of its
     * own, so bare {@code /aac} is a usage error while {@code /aac menu} works — which is the
     * intended shape, since bare {@code /amc} does nothing either.
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> root = dispatcher.register(Commands.literal("amc")
                .requires(CodxSettingsCommand::isOperator)
                .then(Commands.literal("menu").executes(AMConfigCommand::menuCommand))
                .then(CodxSettingsCommand.node("config", ConfigHolder.COMMON_SPEC, RELOAD_NOTE,
                        AMConfigCommand::openMenu)));
        dispatcher.register(Commands.literal("aac")
                .requires(CodxSettingsCommand::isOperator)
                .redirect(root));
    }

    /**
     * {@code /amc menu} — the same screen {@code /amc config menu} opens, one word shorter. Opening
     * the menu is the thing an admin does most often, and it was the longest path in the tree.
     *
     * <p>Declared here rather than in CodxLib so the shortcut costs no library bump: the generated
     * {@code config menu} node is untouched and both spellings open the same builder.
     */
    private static int menuCommand(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.literal("Only a player can open the settings menu."));
            return 0;
        }
        openMenu(player);
        return 1;
    }

    /**
     * The chest menu, tuned for this mod's shape. Reached from {@code /amc config menu}.
     *
     * <p>The one thing it must not do is show {@code spawning} as a flat list: that category is 176
     * numbers, six pages of anonymous repeaters. {@link #spawnGroup} folds it into one page per mob
     * — the mob's weight and rolls together, which is how an admin actually thinks about it.
     */
    private static void openMenu(ServerPlayer player) {
        CodxSettingsMenu.builder(ConfigHolder.COMMON_SPEC)
                .title("§6§lAlex's Mobs Continued")
                .command("/amc config")
                .note(RELOAD_NOTE)
                .icon("general", Items.BOOK)
                .icon("spawning", Items.EGG)
                .icon("uniqueSpawning", Items.END_PORTAL_FRAME)
                .icon("dangerZone", Items.TNT)
                .group("spawning", AMConfigCommand::spawnGroup)
                .open(player);
    }

    /**
     * The mob a {@code spawning} option belongs to: the option name with its trailing
     * {@code Spawn…} stripped, so {@code grizzlyBearSpawnWeight} and {@code grizzlyBearSpawnRolls}
     * land on one page.
     *
     * <p>⚠️ It does <b>not</b> come out at exactly one group per mob, and must not be "fixed" to.
     * Two of upstream's option names are misspelled — {@code boneSerpentSpawnWeight} against
     * {@code boneSeprentSpawnRolls}, {@code crocodileSpawnWeight} against {@code crocSpawnRolls} —
     * so 176 options fold into 90 groups, four of which hold one setting each. Renaming them would
     * silently drop an existing server's tuning for those two mobs on the next load, which is a far
     * worse trade than two mobs appearing twice in a menu.
     */
    private static String spawnGroup(CodxSettings.ConfigValue<?> value) {
        String name = value.name();
        int spawn = name.indexOf("Spawn");
        return spawn > 0 ? name.substring(0, spawn) : name;
    }

    /** See {@link #LIVE_SPAWN_OPTIONS} for why this is category-plus-exceptions and not a flag. */
    private static boolean needsWorldReload(CodxSettings.ConfigValue<?> value) {
        String category = value.category();
        return ("spawning".equals(category) || "uniqueSpawning".equals(category))
                && !LIVE_SPAWN_OPTIONS.contains(value.name().toLowerCase(Locale.ROOT));
    }
}

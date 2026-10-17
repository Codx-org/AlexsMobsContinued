package com.github.alexthe666.alexsmobs.command;

import codx.codxlib.api.ui.menu.CodxMenu;
import codx.codxlib.api.ui.menu.CodxMenuLayout;
import codx.codxlib.api.ui.menu.PagedMenuBuilder;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.config.ConfigHolder;
import com.github.alexthe666.alexsmobs.entity.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.misc.AMCompat;
import com.github.alexthe666.alexsmobs.misc.AMTagRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The editor page behind {@code roadrunnerTargets}: one on/off switch per mob, so a pack can let
 * roadrunners hunt other mods' small animals without typing ids into {@code /amc config set}.
 *
 * <p>The list is every registered mob with attributes, i.e. every living entity type, from this
 * mod, vanilla and anything else installed. Mobs already in the {@code alexsmobs:roadrunner_targets}
 * tag show as locked on, since the tag is not something this page can take away.
 */
public final class AMRoadrunnerTargetsMenu {

    /** The inset 7×4 grid the settings menu uses. */
    private static final int PER_PAGE = 28;

    private static final int SLOT_BACK = 45;
    private static final int SLOT_CLEAR = 46;

    private AMRoadrunnerTargetsMenu() {
    }

    public static void open(ServerPlayer player, Runnable back) {
        List<EntityType<?>> types = livingTypes();
        PagedMenuBuilder menu = CodxMenu.paged("§6§lRoadrunner Targets")
                .onChange(ConfigHolder::save);

        int pages = Math.max(1, (types.size() + PER_PAGE - 1) / PER_PAGE);
        for (int page = 0; page < pages; page++) {
            List<EntityType<?>> slice = types.subList(Math.min(types.size(), page * PER_PAGE),
                    Math.min(types.size(), (page + 1) * PER_PAGE));
            menu.page("Roadrunner Targets", layout -> {
                for (int i = 0; i < slice.size(); i++) {
                    place(layout, slotFor(i), slice.get(i));
                }
            });
        }

        menu.decorate(layout -> {
            layout.action(SLOT_BACK, Items.ARROW, "§a§l← Back", click -> back.run(), "§7Go back");
            layout.action(SLOT_CLEAR, Items.BARRIER, "§c§lTurn All Off",
                    click -> {
                        AMConfig.clearRoadrunnerTargets();
                        click.markChanged();
                    },
                    "§7Roadrunners go back to hunting",
                    "§7rattlesnakes only.",
                    "",
                    "§8Mobs in the roadrunner_targets",
                    "§8tag stay on.");
        });
        menu.open(player);
    }

    private static void place(CodxMenuLayout layout, int slot, EntityType<?> type) {
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
        String label = type.getDescription().getString();
        Item icon = spawnEgg(id);
        if (type.builtInRegistryHolder().is(AMTagRegistry.ROADRUNNER_TARGETS)) {
            layout.info(slot, icon, "§a" + label,
                    "§8" + id,
                    "§7Always hunted: this mob is in the",
                    "§7alexsmobs:roadrunner_targets tag.");
            return;
        }
        layout.toggle(slot, icon, label, () -> AMConfig.isRoadrunnerTargetId(id),
                hunt -> AMConfig.setRoadrunnerTarget(id, hunt),
                "§8" + id,
                "",
                "§7When on, roadrunners chase and",
                "§7attack this mob like a rattlesnake.",
                "§7Best kept to small animals.");
    }

    /** Every living mob but players, armour stands and the roadrunner itself: vanilla first, then this mod, then the rest. */
    private static List<EntityType<?>> livingTypes() {
        EntityType<?> roadrunner = AMEntityRegistry.ROADRUNNER.get();
        List<EntityType<?>> out = new ArrayList<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            String id = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
            if (type == roadrunner || id.equals("minecraft:player") || id.equals("minecraft:armor_stand")) {
                continue;
            }
            if (DefaultAttributes.hasSupplier(type)) {
                out.add(type);
            }
        }
        out.sort(Comparator.comparingInt((EntityType<?> type) -> namespaceRank(BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace()))
                .thenComparing(type -> BuiltInRegistries.ENTITY_TYPE.getKey(type).toString()));
        return out;
    }

    private static int namespaceRank(String namespace) {
        return namespace.equals("minecraft") ? 0 : namespace.equals("alexsmobs") ? 1 : 2;
    }

    /** This mod names its eggs {@code spawn_egg_<mob>}, everything else {@code <mob>_spawn_egg}. */
    private static Item spawnEgg(String id) {
        int colon = id.indexOf(':');
        String namespace = id.substring(0, colon);
        String path = id.substring(colon + 1);
        return BuiltInRegistries.ITEM.getOptional(AMCompat.rl(namespace, "spawn_egg_" + path))
                .or(() -> BuiltInRegistries.ITEM.getOptional(AMCompat.rl(namespace, path + "_spawn_egg")))
                .orElse(Items.EGG);
    }

    /** Index → slot in the inset grid: rows 1–4, columns 1–7. */
    private static int slotFor(int index) {
        return 10 + (index / 7) * 9 + (index % 7);
    }
}

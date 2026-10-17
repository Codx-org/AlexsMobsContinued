package com.github.alexthe666.alexsmobs.config;

import codx.codxlib.api.settings.CodxSettings;

/**
 * Holds the one spec and the one config instance.
 *
 * <p>{@link #COMMON} is the 249 live handles; {@link AMConfig} is the 249 plain static fields the
 * game reads every tick. {@link #COMMON_SPEC} owns {@code config/amc.json}. Nothing outside this
 * package should need any of them — read {@code AMConfig} instead.
 *
 * <p><b>The backend is CodxLib's</b> ({@code codx.codxlib.api.settings.CodxSettings}) rather than
 * this mod's own former {@code AMConfigSpec}, and rather than {@code ForgeConfigSpec}. Upstream
 * registered a {@code ForgeConfigSpec} and got {@code config/alexsmobs.toml}; the Fabric port could
 * not, so it wrote {@code config/alexsmobs.json} instead — the same 249 options in two formats at
 * two paths depending on the loader, neither changeable without a restart. CodxLib gives one JSON
 * format on all 49 nodes, values writable at runtime, and — the reason it is the library's job and
 * not this mod's — the {@code /amc config} tree and the settings chest menu are both generated from
 * this one declaration, so a new option added to {@link CommonConfig} appears in all three surfaces
 * (file, command, menu) without another line being written anywhere.
 *
 * <p>{@link CodxSettings.Builder#legacyFiles} keeps an existing server's tuning: on the first run
 * with no {@code amc.json} the spec reads whichever of the two old files is present. Both are tried
 * on every loader, not just the one that wrote them, so a pack that switched from Forge to Fabric
 * carries its settings across. The old files are left on disk untouched and ignored from then on.
 */
public final class ConfigHolder {

    public static final CodxSettings COMMON_SPEC;
    public static final CommonConfig COMMON;

    static {
        // onChange is AMConfig::bake, so every write the command or the menu makes re-bakes the
        // static fields the game reads. That is what makes an edit take effect without a restart,
        // and it is why nothing outside this package calls bake() itself.
        CodxSettings.Configured<CommonConfig> configured = CodxSettings.builder("alexsmobs")
                .fileName("amc.json")
                .legacyFiles("alexsmobs.toml", "alexsmobs.json")
                .onChange(AMConfig::bake)
                .configure(CommonConfig::new);
        COMMON = configured.holder();
        COMMON_SPEC = configured.settings();
    }

    private ConfigHolder() {
    }

    /**
     * Reads {@code config/amc.json} from disk and copies it into {@link AMConfig}. Called once per
     * launch from each loader's entrypoint, and again by {@code /amc config reload}.
     */
    public static void load() {
        COMMON_SPEC.load();
    }

    /**
     * Writes the current values to disk and copies them into {@link AMConfig}. Returns false if the
     * file could not be written, in which case the edit is still live for this session but will not
     * survive a restart.
     *
     * <p>Not every option takes effect from here. Anything read per-tick or per-spawn-attempt does
     * (the farseer's border restriction, mob behaviour toggles, damage numbers); anything consumed
     * once during registration does not (spawn weights and rolls, and the per-mob biome lists,
     * which are folded into biome modifiers at world load). {@link
     * com.github.alexthe666.alexsmobs.command.AMConfigCommand} says which it just edited.
     */
    public static boolean save() {
        return COMMON_SPEC.apply();
    }
}

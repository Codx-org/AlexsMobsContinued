package com.github.alexthe666.alexsmobs.entity;

/**
 * The parent half of the multipart contract, spelled so that it costs nothing on any loader.
 *
 * <p>Forge and NeoForge do not just supply {@code PartEntity} — they <b>patch vanilla
 * {@code Entity}</b> with {@code isMultipartEntity()} and {@code getParts()}. Fabric's jar is
 * unpatched, so the three parents here ({@code EntityCachalotWhale}, {@code EntityGiantSquid},
 * {@code EntityLaviathan}) have nothing to override and their {@code @Override}s stop compiling.
 *
 * <p>A mixin cannot fix that: mixin-merged members are invisible to javac. So instead the two
 * methods are declared here with <b>exactly the signatures the platform patch uses</b> and the
 * three parents implement this interface <b>unconditionally, on every loader</b>. On Forge/NeoForge
 * their existing declarations satisfy both this interface and the vanilla patch at once, so
 * {@code @Override} stays valid everywhere and not one of the three files needs a Stonecutter gate.
 *
 * <p>The array type is written fully qualified as {@code net.minecraftforge.entity.PartEntity} —
 * the NeoForge namespace rewrite and the Fabric {@code !fab-partentity} redirect each re-point it,
 * the same way the three parents already spell their own return type.
 *
 * <p>Call sites holding a bare {@code Entity} cannot use this directly and go through
 * {@code AMCompat.isMultipart(Entity)} instead, which keeps the vanilla-ender-dragon case that the
 * platform patch covered.
 */
public interface IMultipartOwner {

    boolean isMultipartEntity();

    net.minecraftforge.entity.PartEntity<?>[] getParts();
}

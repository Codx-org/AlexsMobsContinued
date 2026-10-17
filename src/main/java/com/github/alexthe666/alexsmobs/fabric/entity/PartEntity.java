package com.github.alexthe666.alexsmobs.fabric.entity;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;

/**
 * Fabric stand-in for {@code net.minecraftforge.entity.PartEntity}, reproduced from NeoForge's
 * copy (LGPL-2.1-only, Forge Development LLC) — it is four methods, so vendoring it is cheaper
 * than any abstraction over it.
 *
 * <p>Three classes extend it here — {@code EntityCachalotPart}, {@code EntityGiantSquidPart} and
 * {@code EntityLaviathanPart} — and a Fabric-only {@code replacements} rule
 * ({@code !fab-partentity}) re-points the type name, so those files stay byte-identical across
 * all three loaders. Same "relocated compat namespace" pattern as
 * {@code fabric/registries/DeferredRegister} and {@code fabric/registries/DeferredRegister}.
 *
 * <p><b>⚠️ The CLASS is the easy half; the LEVEL PLUMBING is not vendored here.</b> Forge does not
 * just supply this base class — it patches vanilla so that part entities are visible to the world:
 * {@code Level#getEntities}/{@code getEntityCollisions}/{@code getNearestEntity} all fold in
 * {@code Level#dragonParts()}, {@code ServerLevel#getEntity(int)} falls back to its part map, and
 * {@code ServerLevel}/{@code ClientLevel}'s tracking callbacks populate those collections from
 * {@code Entity#getParts()}. Vanilla has the identical mechanism but hard-typed to
 * {@code EnderDragonPart}, so on Fabric it is closed to us and the equivalent has to be built with
 * mixins. Until it is, a whale/squid/laviathan on Fabric renders and ticks but its segments are not
 * pickable, attackable or collidable — the parent's own hitbox still is. See the multipart note in
 * docs/notes/fabric.md.
 *
 * <p>{@code isMultipartEntity()}/{@code getParts()} are Forge patches on vanilla {@code Entity},
 * so they have no home on Fabric either. Rather than gate the three parents, they implement
 * {@link com.github.alexthe666.alexsmobs.entity.IMultipartOwner} — whose two methods are spelled
 * exactly like the patched ones, so the existing {@code @Override}s satisfy the interface on
 * Fabric and the vanilla patch everywhere else, with no source gate at all. Call sites that hold a
 * bare {@code Entity} go through {@code AMCompat.getParts}/{@code AMCompat.isMultipartEntity}.
 */
public abstract class PartEntity<T extends Entity> extends Entity {

    private final T parent;

    public PartEntity(T parent) {
        super(parent.getType(), parent.level());
        this.parent = parent;
    }

    public T getParent() {
        return parent;
    }

    // 1.21 gave getAddEntityPacket a ServerEntity parameter. Overriding the wrong arity would still
    // compile — it is just a new method on this class — and the part would then get a real spawn
    // packet instead of the intended hard failure, so the arity is gated rather than left to chance.
    //? if >=1.21 {
    /*@Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity entity) {
        throw new UnsupportedOperationException();
    }
    *///?} else {
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        throw new UnsupportedOperationException();
    }
    //?}
}

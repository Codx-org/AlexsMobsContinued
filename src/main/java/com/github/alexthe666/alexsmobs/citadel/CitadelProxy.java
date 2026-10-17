package com.github.alexthe666.alexsmobs.citadel;

import net.minecraft.nbt.CompoundTag;

/**
 * Server-side (and common) half of the vendored Citadel proxy. Both packet handlers are
 * client-only behaviour, so here they are no-ops — exactly as in Citadel's own ServerProxy.
 */
public class CitadelProxy {

    public void handleAnimationPacket(int entityId, int index) {
    }

    public void handlePropertiesPacket(String propertyID, CompoundTag compound, int entityID) {
    }
}

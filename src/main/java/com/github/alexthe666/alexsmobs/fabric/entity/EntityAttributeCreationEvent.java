package com.github.alexthe666.alexsmobs.fabric.entity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

/**
 * Fabric stand-in for {@code net.minecraftforge.event.entity.EntityAttributeCreationEvent}, reached
 * by the Fabric-only {@code !fab-attribevent} replacement rule.
 *
 * <p>{@code AMEntityRegistry.initializeAttributes} builds ~96 attribute suppliers and hands each to
 * {@code event.put(type, supplier)}. Fabric API's equivalent registrar —
 * {@link FabricDefaultAttributeRegistry} — is a plain static call with no event around it, so rather
 * than gate all ~96 call sites this class supplies the {@code put} they already spell and forwards.
 *
 * <p>Unlike the loaders' event, nothing fires this: Fabric has no attribute-creation phase. The
 * mod's Fabric initializer has to construct one and call {@code initializeAttributes} itself, after
 * the entity registry has been flushed (the suppliers dereference {@code EntityType} holders).
 */
public final class EntityAttributeCreationEvent {

    public void put(EntityType<? extends LivingEntity> type, AttributeSupplier supplier) {
        FabricDefaultAttributeRegistry.register(type, supplier);
    }
}

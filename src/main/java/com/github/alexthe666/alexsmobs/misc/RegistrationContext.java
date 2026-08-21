package com.github.alexthe666.alexsmobs.misc;

import net.minecraft.resources.ResourceKey;

/**
 * Carries the registry id of the object currently being constructed during a {@code DeferredRegister}
 * flush, so the {@code BlockBehaviour.Properties} / {@code Item.Properties} mixins can call
 * {@code Properties.setId(...)} on it.
 *
 * <p>MC 1.21.2 made an id mandatory on Block/Item {@code Properties} <i>before</i> construction —
 * {@code BlockBehaviour.<init>} reads it via {@code effectiveDrops()} and {@code Item.<init>} via
 * {@code effectiveDescriptionId()} (both inline {@code requireNonNull(id, "… id not set")}). Our
 * registration path builds objects from plain suppliers whose {@code Properties} are created inside
 * the constructor, so the id cannot be stamped at {@code Properties} construction. Instead the
 * {@code AMBlockRegistry.regBlock} / {@code AMItemRegistry.regItem} wrappers publish the key here
 * around the supplier call, and the getter-HEAD mixins stamp it during construction. Harmless (never
 * read) on nodes below 1.21.2, where the mixins are compiled out.
 */
public final class RegistrationContext {
    public static final ThreadLocal<ResourceKey<?>> CURRENT_ID = new ThreadLocal<>();

    private RegistrationContext() {}
}

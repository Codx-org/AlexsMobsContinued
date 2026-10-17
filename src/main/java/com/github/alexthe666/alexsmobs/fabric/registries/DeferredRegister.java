package com.github.alexthe666.alexsmobs.fabric.registries;

import com.github.alexthe666.alexsmobs.fabric.ModBus;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Fabric stand-in for {@code net.minecraftforge.registries.DeferredRegister}.
 *
 * <p><b>Why this exists.</b> 26 files declare a {@code DeferredRegister} and between them make
 * 247 {@code register(name, supplier)} calls — every block, item, entity type, sound, particle,
 * effect and menu in the mod. Fabric has no deferred-registration API at all (its registries are
 * immediate: you call {@code Registry.register} and the entry is live), so there is nothing to
 * rename those declarations onto. This class reproduces the slice of Forge's API the mod uses,
 * under the mod's own package, and a Fabric-only {@code replacements} rule
 * ({@code !fab-deferredregister}) re-points the type name. All 26 consumers stay byte-identical
 * across all three loaders — same trick as {@code fabric/common/Tags}, and the same
 * "relocated compat namespace" pattern as {@code client/render/compat/**}.
 *
 * <p><b>The API surface is deliberately closed</b>, exactly as wide as the mod's call sites:
 * {@link #create}, {@link #register(String, Supplier)}, {@link #getEntries()}, the no-arg
 * {@link #register()} flush and its {@link #register(ModBus)} bus-taking twin. Nothing in this
 * tree ever asks a handle for its registry key — the
 * handles are all declared as plain {@code java.util.function.Supplier}, never Forge's
 * {@code RegistryObject} or NeoForge's {@code DeferredHolder} (the two places that would have
 * wanted {@code getKey()} spell the {@code ResourceKey} out by hand instead; see
 * {@code AMPointOfInterestRegistry}). If a future call site needs more, add it here — do not
 * widen the Stonecutter rule.
 *
 * <p><b>The one behavioural difference that matters: ORDER.</b> On Forge/NeoForge the loader
 * decides when each registry is filled and resolves the cross-references for you. Here
 * {@link #register()} runs the suppliers immediately, so a registry must be flushed after
 * anything it dereferences — item suppliers call {@code AMBlockRegistry.X.get()}, POI suppliers
 * call it too, and the creative tab walks the item registry. The order is exactly the one
 * {@code AlexsMobs}'s constructor already registers them in — that constructor runs on Fabric too
 * (the Fabric entrypoint just news it up), and {@link #register(ModBus)} makes each of its ~20
 * lines the flush, so there is no second copy of the order to keep in sync. A handle used before
 * its flush throws with the name in the message rather than returning null, so getting it wrong
 * fails loudly instead of at some later NPE.
 *
 * <p>Written against {@code ResourceLocation}, not {@code Identifier}: the {@code !mc2111-identifier}
 * rule renames it on 1.21.11+, so this file needs no change when Fabric nodes are back-filled
 * downward.
 */
public final class DeferredRegister<T> {

    private final ResourceKey<? extends Registry<T>> registryKey;
    private final String modid;
    private final List<Entry<? extends T>> entries = new ArrayList<>();
    private boolean flushed;

    private DeferredRegister(ResourceKey<? extends Registry<T>> registryKey, String modid) {
        this.registryKey = registryKey;
        this.modid = modid;
    }

    public static <T> DeferredRegister<T> create(ResourceKey<? extends Registry<T>> registryKey, String modid) {
        return new DeferredRegister<>(registryKey, modid);
    }

    /**
     * Generic shape copied from Forge's, so a {@code DeferredRegister<EntityType<?>>} still infers
     * {@code Supplier<EntityType<EntityGrizzlyBear>>} at the call site and the 247 declarations
     * keep their precise types.
     */
    public <I extends T> Supplier<I> register(String name, Supplier<? extends I> supplier) {
        if (flushed) {
            throw new IllegalStateException("Registered " + modid + ":" + name + " after " + registryKey + " was flushed");
        }
        Entry<I> entry = new Entry<>(name, supplier);
        entries.add(entry);
        return entry;
    }

    /**
     * Performs the real registration. Called once per registry from the Fabric entrypoint — the
     * moment Forge's mod bus would have fired its {@code RegisterEvent}.
     *
     * <p>A registry with no entries is skipped without ever being looked up. That is not just an
     * optimisation: several {@code DeferredRegister}s in this tree are declared unconditionally
     * but only populated below some MC version, because vanilla turned their registry into
     * datapack content ({@code BANNER_PATTERN} at 1.20.5, {@code ENCHANTMENT} and
     * {@code PAINTING_VARIANT} at 1.21). Those keys are not in {@link BuiltInRegistries} at all on
     * a modern node, so looking them up eagerly would throw for a registry the mod deliberately
     * no longer uses.
     */
    public void register() {
        if (flushed || entries.isEmpty()) {
            flushed = true;
            return;
        }
        flushed = true;
        Registry<T> registry = lookupRegistry();
        for (Entry<? extends T> entry : entries) {
            entry.resolve(registry, modid);
        }
    }

    /**
     * The shape {@code AlexsMobs}' constructor spells ~20 times, once per registry. On
     * Forge/NeoForge the argument is the mod event bus and the call only schedules the fill; here
     * the token carries nothing (see {@link ModBus}) and the fill happens on the spot. Taking it
     * anyway is what keeps those call sites identical across all three loaders — and it makes the
     * constructor the single place the load-bearing flush order is written down.
     */
    public void register(ModBus bus) {
        register();
    }

    @SuppressWarnings("unchecked")
    public Collection<Supplier<T>> getEntries() {
        return Collections.unmodifiableCollection((List<Supplier<T>>) (List<?>) entries);
    }

    /**
     * {@code BuiltInRegistries} exposes no key-to-registry lookup that keeps the element type, so
     * this walks the registry-of-registries and matches on {@link Registry#key()}. Cheap enough:
     * it happens once per {@code DeferredRegister}, i.e. under twenty times per launch.
     */
    @SuppressWarnings("unchecked")
    private Registry<T> lookupRegistry() {
        for (Registry<?> candidate : BuiltInRegistries.REGISTRY) {
            if (candidate.key().equals(registryKey)) {
                return (Registry<T>) candidate;
            }
        }
        throw new IllegalStateException("No built-in registry for " + registryKey
                + " — it is datapack content on this version, so nothing may be registered to it from code");
    }

    private static final class Entry<I> implements Supplier<I> {

        private final String name;
        private final Supplier<? extends I> factory;
        private I value;

        private Entry(String name, Supplier<? extends I> factory) {
            this.name = name;
            this.factory = factory;
        }

        @Override
        public I get() {
            if (value == null) {
                throw new IllegalStateException("Used " + name + " before its registry was flushed — "
                        + "check the flush order in the AlexsMobs constructor");
            }
            return value;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private void resolve(Registry<?> registry, String modid) {
            I created = factory.get();
            Registry.register((Registry) registry, com.github.alexthe666.alexsmobs.misc.AMCompat.rl(modid, name), created);
            this.value = created;
        }
    }
}

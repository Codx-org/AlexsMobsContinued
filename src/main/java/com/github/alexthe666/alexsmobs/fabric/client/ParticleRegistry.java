package com.github.alexthe666.alexsmobs.fabric.client;

import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;

/**
 * Fabric stand-in for Forge's {@code RegisterParticleProvidersEvent}, so that
 * {@code ClientProxy#setupParticles}'s 22-line body is shared verbatim across all three loaders and
 * only its <em>signature</em> line is gated. Same trick the {@code -decl} replacement rules use
 * throughout this tree: change the receiver's type, keep the calls.
 *
 * <p>Both method names are Forge's, deliberately — this class exists to be call-compatible, not to
 * be idiomatic. What each maps to:
 *
 * <ul>
 *   <li>{@code registerSpriteSet} → the {@code PendingParticleProvider} overload. Fabric has no
 *       {@code registerSpriteSet}; instead the provider is built lazily from a
 *       {@code FabricSpriteSet}, which <b>extends vanilla {@code SpriteSet}</b> — which is why the
 *       mod's existing {@code Factory::new} constructor references type-check here unchanged.</li>
 *   <li>{@code registerSpecial} → the plain {@code ParticleProvider} overload.</li>
 * </ul>
 */
public final class ParticleRegistry {

    public <T extends ParticleOptions> void registerSpriteSet(ParticleType<T> type, ParticleProviderRegistry.PendingParticleProvider<T> factory) {
        ParticleProviderRegistry.getInstance().register(type, factory);
    }

    public <T extends ParticleOptions> void registerSpecial(ParticleType<T> type, ParticleProvider<T> provider) {
        ParticleProviderRegistry.getInstance().register(type, provider);
    }
}

package com.github.alexthe666.alexsmobs.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;

/**
 * Base for this mod's sprite-sheet particles, absorbing 1.21.9's particle rewrite.
 *
 * <p>1.21.9 deleted {@code TextureSheetParticle} outright: everything it added over
 * {@code SingleQuadParticle} (the {@code sprite} field, {@code setSprite}, the four UV getters,
 * {@code setSpriteFromAge}) moved <em>into</em> {@code SingleQuadParticle}, and what did not move is
 * {@code pickSprite}. So on {@code >=1.21.9} this extends {@code SingleQuadParticle} directly and
 * re-supplies {@code pickSprite}; below that it is {@code TextureSheetParticle} and adds nothing.
 *
 * <p>Passing {@code null} for {@code SingleQuadParticle}'s new sprite constructor parameter is safe:
 * that constructor only assigns the field, and every subclass here sets a real sprite immediately
 * afterwards (via {@code pickSprite} or {@code setSpriteFromAge}), exactly as it did when
 * {@code TextureSheetParticle} left the field null.
 *
 * <p>Deliberately <em>not</em> named {@code TextureSheetParticle}: a compat class sharing a vanilla
 * class's simple name is what silently retargeted {@code mixin/renderstate/EntityRendererMixin}
 * through a Stonecutter import rule — see docs/notes/mixins.md.
 */
//? if >=1.21.9 {
/*public abstract class AMQuadParticle extends net.minecraft.client.particle.SingleQuadParticle {

    protected AMQuadParticle(ClientLevel world, double x, double y, double z) {
        super(world, x, y, z, null);
    }

    protected AMQuadParticle(ClientLevel world, double x, double y, double z, double xd, double yd, double zd) {
        super(world, x, y, z, xd, yd, zd, null);
    }

    // TextureSheetParticle declared this public, and several nested Factory classes call it on an
    // instance — setSprite alone would not do, it is protected and a Factory is not a subclass.
    public void pickSprite(SpriteSet sprites) {
        this.setSprite(sprites.get(this.random));
    }

    // getLayer() is abstract on SingleQuadParticle. TRANSLUCENT is what TextureSheetParticle's
    // default PARTICLE_SHEET_TRANSLUCENT meant; subclasses that returned the opaque sheet override.
    public net.minecraft.client.particle.SingleQuadParticle.Layer getLayer() {
        return net.minecraft.client.particle.SingleQuadParticle.Layer.TRANSLUCENT;
    }
}
*///?} else {
public abstract class AMQuadParticle extends net.minecraft.client.particle.TextureSheetParticle {

    protected AMQuadParticle(ClientLevel world, double x, double y, double z) {
        super(world, x, y, z);
    }

    protected AMQuadParticle(ClientLevel world, double x, double y, double z, double xd, double yd, double zd) {
        super(world, x, y, z, xd, yd, zd);
    }
}
//?}

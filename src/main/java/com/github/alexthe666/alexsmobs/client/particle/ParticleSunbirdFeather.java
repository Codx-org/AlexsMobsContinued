package com.github.alexthe666.alexsmobs.client.particle;

import com.github.alexthe666.alexsmobs.entity.util.Maths;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class ParticleSunbirdFeather extends SimpleAnimatedParticle {

    private float initialRoll = 0;

    private ParticleSunbirdFeather(ClientLevel world, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
        super(world, x, y, z, sprites, 0.0F);
        this.friction = 0.96F;
        this.speedUpWhenYMotionIsBlocked = true;
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.quadSize = 0.15F + this.random.nextFloat() * 0.2F;
        this.lifetime = 20 + this.random.nextInt(20);
        this.gravity = 0.02F;
        // pickSprite lived on TextureSheetParticle, which 1.21.9 deleted; SimpleAnimatedParticle
        // never had it. setSprite is protected on SingleQuadParticle — fine from a subclass body.
        //? if >=1.21.9 {
        /*this.setSprite(sprites.get(this.random));
        *///?} else {
        this.pickSprite(sprites);
        //?}
        float f = Mth.sqrt((float) (xd * xd + zd * zd));
        float f1 = -(float) Mth.atan2(yd, f) + Maths.rad(135);
        this.roll = f1 * 2F;
    }

    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.xd *= 0.8;
        this.yd *= 0.8;
        this.zd *= 0.8;
        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            this.oRoll = this.roll;
            if (!this.onGround) {
                //float dist = -initialRoll / (this.lifetime - 6) * Math.min(this.age, this.lifetime - 6);
                this.roll += 0 + (float)Math.sin(age * 0.3F) * 0.5F * (this.age / (float)lifetime);
            }
            this.move(this.xd, this.yd, this.zd);
            this.yd -= (double)this.gravity;
        }
    }

    public int getLightColor(float p_189214_1_) {
        int lvt_2_1_ = super.getLightColor(p_189214_1_);
        int lvt_4_1_ = lvt_2_1_ >> 16 & 255;
        return 240 | lvt_4_1_ << 16;
    }

    // 1.21.9 replaced Particle#getRenderType with a two-tier split: getGroup() names the
    // submission group (SINGLE_QUADS here, from SingleQuadParticle) and getLayer() names the atlas
    // + pipeline the quad goes to. PARTICLE_SHEET_TRANSLUCENT is Layer.TRANSLUCENT.
    //? if >=1.21.9 {
    /*@Override
    public net.minecraft.client.particle.SingleQuadParticle.Layer getLayer() {
        return net.minecraft.client.particle.SingleQuadParticle.Layer.TRANSLUCENT;
    }
    *///?} else {
    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
    //?}

    public static class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Factory(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new ParticleSunbirdFeather(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, spriteSet);
        }
    }
}

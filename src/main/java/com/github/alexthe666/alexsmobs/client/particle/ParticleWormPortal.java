package com.github.alexthe666.alexsmobs.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class ParticleWormPortal extends SimpleAnimatedParticle {

    private ParticleWormPortal(ClientLevel world, double x, double y, double z, double motionX, double motionY, double motionZ, SpriteSet sprites) {
        super(world, x, y, z, sprites, 0.0F);
        this.xd = (float) motionX;
        this.yd = (float) motionY;
        this.zd = (float) motionZ;
        this.quadSize = 0.35F;
        this.lifetime = 10 + this.random.nextInt(12);
        this.gravity = 0;
        this.setSpriteFromAge(sprites);
    }

    public int getLightColor(float p_189214_1_) {
        int lvt_2_1_ = super.getLightColor(p_189214_1_);
        int lvt_4_1_ = lvt_2_1_ >> 16 & 255;
        return 240 | lvt_4_1_ << 16;
    }

    public void tick() {
        super.tick();
        this.oRoll = this.roll;
        this.xd *= 0.8D;
        this.yd *= 0.8D;
        this.zd *= 0.8D;
//        double lvt_1_1_ = this.x - this.xo;
//        double lvt_5_1_ = this.z - this.zo;
        this.roll += 0.25F;
        this.setSpriteFromAge(this.sprites);
        this.quadSize = 0.35F * (1F - (this.age / (float)this.lifetime));
        this.setAlpha(1F - (this.age / (float)this.lifetime));
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

    @OnlyIn(Dist.CLIENT)
    public static class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Factory(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            ParticleWormPortal p = new ParticleWormPortal(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, spriteSet);
            return p;
        }
    }
}

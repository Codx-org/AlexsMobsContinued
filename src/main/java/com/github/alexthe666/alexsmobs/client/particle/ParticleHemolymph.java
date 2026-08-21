package com.github.alexthe666.alexsmobs.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class ParticleHemolymph extends AMQuadParticle {

    private static final int[] POSSIBLE_COLORS = {0X70FFF8, 0X3BFFD0, 0X08DED9};

    private ParticleHemolymph(ClientLevel world, double x, double y, double z, double motionX, double motionY, double motionZ) {
        super(world, x, y, z);
        int color = POSSIBLE_COLORS[random.nextInt(POSSIBLE_COLORS.length - 1)];
        float lvt_18_1_ = (float)(color >> 16 & 255) / 255.0F;
        float lvt_19_1_ = (float)(color >> 8 & 255) / 255.0F;
        float lvt_20_1_ = (float)(color & 255) / 255.0F;
        setColor(lvt_18_1_, lvt_19_1_, lvt_20_1_);
        this.xd = (float) motionX;
        this.yd = (float) motionY;
        this.zd = (float) motionZ;
        this.quadSize *= 0.6F + this.random.nextFloat() * 1.4F;
        this.lifetime = 10 + this.random.nextInt(15);
        this.gravity = 0.5F;

    }

    public void tick() {
        super.tick();
        this.yd -= 0.004D + 0.04D * (double)this.gravity;
    }

    // 1.21.9 replaced Particle#getRenderType with a two-tier split: getGroup() names the
    // submission group (SINGLE_QUADS here, from SingleQuadParticle) and getLayer() names the atlas
    // + pipeline the quad goes to. PARTICLE_SHEET_OPAQUE is Layer.OPAQUE.
    //? if >=1.21.9 {
    /*@Override
    public net.minecraft.client.particle.SingleQuadParticle.Layer getLayer() {
        return net.minecraft.client.particle.SingleQuadParticle.Layer.OPAQUE;
    }
    *///?} else {
    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }
    //?}

    @OnlyIn(Dist.CLIENT)
    public static class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Factory(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            ParticleHemolymph p = new ParticleHemolymph(worldIn, x, y, z, xSpeed, ySpeed, zSpeed);
            p.pickSprite(spriteSet);
            return p;
        }
    }
}

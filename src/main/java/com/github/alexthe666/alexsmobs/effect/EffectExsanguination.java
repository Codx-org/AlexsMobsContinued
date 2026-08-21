package com.github.alexthe666.alexsmobs.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class EffectExsanguination extends MobEffect {

    private int lastDuration = -1;

    protected EffectExsanguination() {
        super(MobEffectCategory.HARMFUL, 0XED5151);
    }

    // 1.20.5 made applyEffectTick return boolean; 1.21.2 then PREFIXED it with the ServerLevel.
    // Upstream writes no @Override, so an un-updated form is a silently dead overload and the
    // effect simply stops doing anything. See docs/notes/bug-reports.md #66.
    //? if >=1.21.2 {
    /*public boolean applyEffectTick(net.minecraft.server.level.ServerLevel level, LivingEntity entity, int amplifier) {
    *///?} elif >=1.20.5 {
    /*public boolean applyEffectTick(LivingEntity entity, int amplifier) {
    *///?} else {
    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
    //?}
        entity.hurt(entity.damageSources().magic(), Math.min(amplifier + 1, Math.round(lastDuration / 20F)));
        for(int i = 0; i < 3; i++){
            entity.level().addParticle(ParticleTypes.DAMAGE_INDICATOR, entity.getRandomX(1.0), entity.getRandomY(), entity.getRandomZ(1.0), 0, 0, 0);
        }
        //? if >=1.20.5
        //return true;
    }

    // 1.20.2 renamed isDurationEffectTick to shouldApplyEffectTickThisTick, and MobEffect's base
    // implementation returns FALSE -- so keeping the old name does not merely lose an override,
    // it stops the effect ticking at all. (javap: 1.20.1 has the old name, 1.20.2 the new one --
    // NOT 1.20.5, which is where applyEffectTick's boolean return arrived.)
    // See docs/notes/bug-reports.md #66.
    //? if >=1.20.2 {
    /*public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
    *///?} else {
    public boolean isDurationEffectTick(int duration, int amplifier) {
    //?}
        lastDuration = duration;
        return duration > 0 && duration % 20 == 0;
    }

    public String getDescriptionId() {
        return "alexsmobs.potion.exsanguination";
    }

}

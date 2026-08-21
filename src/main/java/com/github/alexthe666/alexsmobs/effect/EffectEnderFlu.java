package com.github.alexthe666.alexsmobs.effect;

import com.github.alexthe666.alexsmobs.entity.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.entity.EntityEnderiophage;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import com.github.alexthe666.alexsmobs.misc.AMCompat;

public class EffectEnderFlu extends MobEffect {

    private int lastDuration = -1;

    public EffectEnderFlu() {
        super(MobEffectCategory.HARMFUL, 0X6836AA);
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
        if (lastDuration == 1) {
            int phages = amplifier + 1;
            entity.hurt(entity.damageSources().magic(), phages * 10);
            for (int i = 0; i < phages; i++) {
                EntityEnderiophage phage = AMCompat.create(AMEntityRegistry.ENDERIOPHAGE.get(), entity.level());
                phage.copyPosition(entity);
                phage.onSpawnFromEffect();
                phage.setSkinForDimension();
                if (!entity.level().isClientSide()) {
                    phage.setStandardFleeTime();
                    entity.level().addFreshEntity(phage);
                }
            }
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
        return duration > 0;
    }

    public String getDescriptionId() {
        return "alexsmobs.potion.ender_flu";
    }

}
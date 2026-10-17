package com.github.alexthe666.alexsmobs.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class EffectOiled extends MobEffect {

    public EffectOiled() {
        super(MobEffectCategory.BENEFICIAL, 0XFFE89C);
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
       if(entity.isInWaterRainOrBubble()){
           if(!entity.isShiftKeyDown()){
               entity.setDeltaMovement(entity.getDeltaMovement().add(0, 0.1D, 0));
           }else{
               entity.fallDistance = 0;
           }
           if (!entity.onGround()) {
               Vec3 vector3d = entity.getDeltaMovement();
               entity.setDeltaMovement(vector3d.multiply(1.0D, 0.9D, 1.0D));

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
        return duration > 0;
    }

    public String getDescriptionId() {
        return "alexsmobs.potion.oiled";
    }

}
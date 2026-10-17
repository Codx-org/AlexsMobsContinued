package com.github.alexthe666.alexsmobs.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import com.github.alexthe666.alexsmobs.misc.AMCompat;

public class EffectFear extends MobEffect {

    protected EffectFear() {
        super(MobEffectCategory.NEUTRAL, 0X7474F7);
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, com.github.alexthe666.alexsmobs.misc.AMCompat.attrModId("7107DE5E-7CE8-4030-940E-514C1F160890", "fear_speed"), (double)-1.0F, AttributeModifier.Operation.MULTIPLY_BASE);
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
        if(entity.getDeltaMovement().y > 0 && !entity.isInWaterOrBubble()){
            entity.setDeltaMovement(entity.getDeltaMovement().multiply(1, 0, 1));
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
        return "alexsmobs.potion.fear";
    }
}

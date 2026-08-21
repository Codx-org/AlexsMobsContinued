package com.github.alexthe666.alexsmobs.effect;

import com.github.alexthe666.alexsmobs.misc.AMPlatform;

import com.github.alexthe666.alexsmobs.misc.AMSoundRegistry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import com.github.alexthe666.alexsmobs.misc.AMCompat;

public class EffectPowerDown extends MobEffect {

    private int lastDuration = -1;
    private int firstDuration = -1;

    protected EffectPowerDown() {
        super(MobEffectCategory.NEUTRAL, 0x00000);
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, com.github.alexthe666.alexsmobs.misc.AMCompat.attrModId("7107DE5E-7CE8-4030-940E-514C1F160890", "power_down_speed"), (double)-1.0F, AttributeModifier.Operation.MULTIPLY_BASE);
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
        if(firstDuration == lastDuration){
            entity.playSound(AMSoundRegistry.APRIL_FOOLS_POWER_OUTAGE.get(), 1.5F, 1);
            entity.gameEvent(AMPlatform.ENTITY_ACTION);
        }
        //? if >=1.20.5
        //return true;
    }

    public int getActiveTime(){
        return firstDuration - lastDuration;
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
        if(duration <= 0){
            lastDuration = -1;
            firstDuration = -1;
        }
        if(firstDuration == -1){
            firstDuration = duration;
        }
        return duration > 0;
    }

    // 1.20.2 dropped the LivingEntity (and, for remove, the amplifier) from these two hooks.
    //? if >=1.20.2 {
    /*public void removeAttributeModifiers(AttributeMap map) {
        lastDuration = -1;
        firstDuration = -1;
        super.removeAttributeModifiers(map);
    }

    public void addAttributeModifiers(AttributeMap map, int i) {
        lastDuration = -1;
        firstDuration = -1;
        super.addAttributeModifiers(map, i);
    }
    *///?}
    //? if <1.20.2 {
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap map, int i) {
        lastDuration = -1;
        firstDuration = -1;
        super.removeAttributeModifiers(entity, map, i);
    }

    public void addAttributeModifiers(LivingEntity entity, AttributeMap map, int i) {
        lastDuration = -1;
        firstDuration = -1;
        super.addAttributeModifiers(entity, map, i);
    }
    //?}

    public String getDescriptionId() {
        return "alexsmobs.potion.power_down";
    }
}

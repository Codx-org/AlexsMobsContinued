package com.github.alexthe666.alexsmobs.effect;

import com.github.alexthe666.alexsmobs.misc.AMCompat;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

public class EffectFleetFooted extends MobEffect {

    //? if >=1.21 {
    /*private static final net.minecraft.resources.ResourceLocation SPRINT_JUMP_SPEED_MODIFIER = AMCompat.rl("alexsmobs", "fleet_footed_speed");
    *///?} else {
    private static final UUID SPRINT_JUMP_SPEED_MODIFIER = UUID.fromString("7E0292F2-9434-48D5-A29F-9583AF7DF29A");
    //?}
    private static final AttributeModifier SPRINT_JUMP_SPEED_BONUS = AMCompat.attributeModifier(SPRINT_JUMP_SPEED_MODIFIER, "fleetfooted speed bonus", 0.2F, AttributeModifier.Operation.ADDITION);
    private int lastDuration = -1;
    private int removeEffectAfter = 0;

    public EffectFleetFooted() {
        super(MobEffectCategory.BENEFICIAL, 0X685441);
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
        AttributeInstance modifiableattributeinstance = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        boolean applyEffect = entity.isSprinting() && !entity.onGround() && lastDuration > 2;
        if(removeEffectAfter > 0){
            removeEffectAfter--;
        }
        if (applyEffect) {
            if(!AMCompat.hasModifier(modifiableattributeinstance, SPRINT_JUMP_SPEED_MODIFIER)){
                modifiableattributeinstance.addPermanentModifier(SPRINT_JUMP_SPEED_BONUS);
            }
            removeEffectAfter = 5;
        }
        if (removeEffectAfter <= 0 || lastDuration < 2) {
            modifiableattributeinstance.removeModifier(SPRINT_JUMP_SPEED_MODIFIER);
        }
        //? if >=1.20.5
        //return true;
    }

    // 1.20.2 dropped the LivingEntity from MobEffect#removeAttributeModifiers (and the amplifier),
    // so the modern branch clears the bonus straight off the AttributeMap instead of the entity.
    //? if >=1.20.2 {
    /*public void removeAttributeModifiers(AttributeMap attributeMap) {
        AttributeInstance modifiableattributeinstance = attributeMap.getInstance(Attributes.MOVEMENT_SPEED);
        amRemoveBonus(modifiableattributeinstance);
        super.removeAttributeModifiers(attributeMap);
    }
    *///?}
    //? if <1.20.2 {
    public void removeAttributeModifiers(LivingEntity livingEntity, AttributeMap attributeMap, int level) {
        amRemoveBonus(livingEntity.getAttribute(Attributes.MOVEMENT_SPEED));
    }
    //?}

    private void amRemoveBonus(AttributeInstance modifiableattributeinstance) {
        if (modifiableattributeinstance != null && modifiableattributeinstance.getModifier(SPRINT_JUMP_SPEED_MODIFIER) != null) {
            modifiableattributeinstance.removeModifier(SPRINT_JUMP_SPEED_MODIFIER);
        }
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
        return "alexsmobs.potion.fleet_footed";
    }

}
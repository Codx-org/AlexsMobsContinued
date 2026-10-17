package com.github.alexthe666.alexsmobs.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import com.github.alexthe666.alexsmobs.misc.AMCompat;

public class EffectOrcaMight extends MobEffect {

    public EffectOrcaMight() {
        super(MobEffectCategory.BENEFICIAL, 0X4A4A52);
        this.addAttributeModifier(Attributes.ATTACK_SPEED, com.github.alexthe666.alexsmobs.misc.AMCompat.attrModId("03C3C89D-7037-4B42-869F-B146BCB64D3A", "orca_might_attack_speed"), 3D, AttributeModifier.Operation.ADDITION);
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
        return "alexsmobs.potion.orcas_might";
    }

}
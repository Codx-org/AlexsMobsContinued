package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityCapuchinMonkey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import com.github.alexthe666.alexsmobs.misc.AMCompat;

public class CapuchinAIMelee extends MeleeAttackGoal {

    private final EntityCapuchinMonkey monkey;

    public CapuchinAIMelee(EntityCapuchinMonkey monkey, double speedIn, boolean useLongMemory) {
        super(monkey, speedIn, useLongMemory);
        this.monkey = monkey;
    }

    public boolean canUse() {
        return super.canUse() && !monkey.attackDecision;
    }

    public boolean canContinueToUse() {
        return super.canContinueToUse() && !monkey.attackDecision;
    }

    // 1.20.2 dropped the distance argument from MeleeAttackGoal#checkAndPerformAttack (and
    // getAttackReachSqr with it), so the real body lives in a mod-owned method both shapes call.
//? if >=1.20.2 {
    /*protected void checkAndPerformAttack(LivingEntity enemy) {
        amCheckAndPerformAttack(enemy, this.mob.distanceToSqr(enemy.getX(), enemy.getY(), enemy.getZ()));
    }
*///?}
//? if <1.20.2 {
    protected void checkAndPerformAttack(LivingEntity enemy, double distToEnemySqr) {
        amCheckAndPerformAttack(enemy, distToEnemySqr);
    }
//?}

    protected void amCheckAndPerformAttack(LivingEntity enemy, double distToEnemySqr) {
        double d0 = com.github.alexthe666.alexsmobs.misc.AMPlatform.attackReachSqr(this.mob, enemy);
        if (distToEnemySqr <= d0) {
            this.resetAttackCooldown();
            this.mob.swing(InteractionHand.MAIN_HAND);
            AMCompat.doHurtTarget(this.mob, enemy);
        }

    }

}

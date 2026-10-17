package com.github.alexthe666.alexsmobs.effect;

import com.github.alexthe666.alexsmobs.misc.AMCompat;

import com.github.alexthe666.alexsmobs.entity.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.entity.EntityTarantulaHawk;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.function.Predicate;

public class EffectDebilitatingSting extends MobEffect {

    //? if >=1.21 {
    /*private static final net.minecraft.resources.ResourceLocation PARALYSIS_MODIFIER = AMCompat.rl("alexsmobs", "paralysis");
    *///?} else {
    private static final java.util.UUID PARALYSIS_MODIFIER = java.util.UUID.fromString("7107DE5E-7CE8-4030-940E-514C1F160890");
    //?}
    private int lastDuration = -1;

    protected EffectDebilitatingSting() {
        super(MobEffectCategory.NEUTRAL, 0XFFF385);
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, AMCompat.attrModId(PARALYSIS_MODIFIER.toString(), "paralysis"), -1.0F, AttributeModifier.Operation.MULTIPLY_BASE);
    }

    // The total-paralysis speed modifier is meant for arthropods only. Up to 1.20.1 that gate
    // lived in add/removeAttributeModifiers, which took the LivingEntity; 1.20.2 removed it from
    // both signatures, so on modern versions the modifier is applied to everyone and stripped
    // again each tick for non-arthropods (see applyEffectTick).
    //? if <1.20.2 {
    public void removeAttributeModifiers(LivingEntity entityLivingBaseIn, AttributeMap attributeMapIn, int amplifier) {
        if (AMCompat.isArthropod(entityLivingBaseIn)) {
            super.removeAttributeModifiers(entityLivingBaseIn, attributeMapIn, amplifier);
        }
    }

    public void addAttributeModifiers(LivingEntity entityLivingBaseIn, AttributeMap attributeMapIn, int amplifier) {
        if (AMCompat.isArthropod(entityLivingBaseIn)) {
            super.addAttributeModifiers(entityLivingBaseIn, attributeMapIn, amplifier);
        }
    }
    //?}

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
        if (!AMCompat.isArthropod(entity)) {
            //? if >=1.20.2 {
            /*net.minecraft.world.entity.ai.attributes.AttributeInstance speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null && speed.getModifier(PARALYSIS_MODIFIER) != null) {
                speed.removeModifier(PARALYSIS_MODIFIER);
            }
            *///?}
            if (entity.getHealth() > entity.getMaxHealth() * 0.5F) {
                entity.hurt(entity.damageSources().magic(), 1.0F);
            }
        } else {
            boolean suf = isEntityInsideOpaqueBlock(entity);
            if (suf) {
                entity.setDeltaMovement(Vec3.ZERO);
                entity.noPhysics = true;
            }
            entity.setNoGravity(suf);
            entity.setJumping(false);
            if (!entity.isPassenger() && entity instanceof Mob && !(((Mob) entity).getMoveControl().getClass() == MoveControl.class)) {
                entity.setDeltaMovement(new Vec3(0, -1, 0));
            }
            if (lastDuration == 1) {
                entity.hurt(entity.damageSources().magic(), (amplifier + 1) * 30);
                if (amplifier > 0) {
                    BlockPos surface = entity.blockPosition();
                    while (!entity.level().isEmptyBlock(surface) && surface.getY() < 256) {
                        surface = surface.above();
                    }
                    EntityTarantulaHawk baby = AMCompat.create(AMEntityRegistry.TARANTULA_HAWK.get(), entity.level());
                    baby.setBaby(true);
                    baby.setPos(entity.getX(), surface.getY() + 0.1F, entity.getZ());
                    if (!entity.level().isClientSide()) {
                        //? if >=1.20.5 {
                        /*baby.finalizeSpawn((ServerLevelAccessor) entity.level(), AMCompat.difficultyAt(entity.level(), entity.blockPosition()), MobSpawnType.BREEDING, null);
                        *///?} else {
                        baby.finalizeSpawn((ServerLevelAccessor) entity.level(), AMCompat.difficultyAt(entity.level(), entity.blockPosition()), MobSpawnType.BREEDING, null, null);
                        //?}
                        entity.level().addFreshEntity(baby);
                    }
                }
                entity.setNoGravity(false);
                entity.noPhysics = false;
            }
        }
        //? if >=1.20.5
        //return true;
    }

    public boolean isEntityInsideOpaqueBlock(Entity entity) {
        Vec3 vec3 = entity.getEyePosition();
        float f = com.github.alexthe666.alexsmobs.misc.AMCompat.width(entity.getDimensions(entity.getPose())) * 0.8F;
        AABB axisalignedbb = AABB.ofSize(vec3, (double)f, 1.0E-6D, (double)f);
        return entity.level().getBlockStates(axisalignedbb).filter(Predicate.not(BlockBehaviour.BlockStateBase::isAir)).anyMatch((p_185969_) -> {
            BlockPos blockpos = AMBlockPos.fromVec3(vec3);
            return p_185969_.isSuffocating(entity.level(), blockpos) && Shapes.joinIsNotEmpty(p_185969_.getCollisionShape(entity.level(), blockpos).move(vec3.x, vec3.y, vec3.z), Shapes.create(axisalignedbb), BooleanOp.AND);
        });
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
        return "alexsmobs.potion.debilitating_sting";
    }
}

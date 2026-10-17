package com.github.alexthe666.alexsmobs.effect;

import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.level.block.state.BlockState;

public class EffectClinging extends MobEffect {

    public EffectClinging() {
        super(MobEffectCategory.BENEFICIAL, 0XBD4B4B);
    }

    private static BlockPos getPositionUnderneath(Entity e) {
        return AMBlockPos.fromCoords(e.getX(), e.getBoundingBox().maxY + 1.51F, e.getZ());
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
        entity.refreshDimensions();
        entity.setNoGravity(false);

        if (isUpsideDown(entity)) {
            entity.fallDistance = 0;
            if (!entity.isShiftKeyDown()) {
                if (!entity.horizontalCollision) {
                    entity.setDeltaMovement(entity.getDeltaMovement().add(0, 0.3F, 0));
                }
                entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.998F, 1F, 0.998F));
            }
        }
        //? if >=1.20.5
        //return true;
    }

    public static boolean isUpsideDown(LivingEntity entity){
        BlockPos pos = getPositionUnderneath(entity);
        BlockState ground = entity.level().getBlockState(pos);
        return (entity.verticalCollision || ground.isFaceSturdy(entity.level(), pos, Direction.DOWN)) && !entity.onGround();
    }

    /**
     * Is this entity currently hanging from a ceiling under Clinging — i.e. should it be drawn (and,
     * below 1.21.2, viewed) upside down?
     *
     * <p><b>UPSTREAM FIX (#82).</b> Every caller used to ask this as
     * {@code hasEffect(CLINGING) && getEyeHeight() < getBbHeight() * 0.45F}, using the dropped eye
     * height as a proxy for "hanging". Nothing ever dropped it: the only writer was
     * {@code ServerEvents#onEntityResize}, whose guard is
     * {@code getActiveEffectsMap().containsKey(AMEffectRegistry.CLINGING)} — a {@code Supplier},
     * never a key of a map of effects, so it is unconditionally false. That is upstream's own line,
     * so the flip has never fired in Alex's Mobs on any version; the port then lost the event
     * outright at 1.20.2 (eye height moved into EntityDimensions and Forge deleted
     * {@code EntityEvent.Size}). Asking the real question instead needs no dimension surgery and no
     * mixin, and it is what the proxy was standing in for.
     */
    public static boolean isFlippedUpsideDown(LivingEntity entity) {
        return entity.hasEffect(com.github.alexthe666.alexsmobs.misc.AMCompat.effect(AMEffectRegistry.CLINGING.get()))
                && isUpsideDown(entity);
    }
    // Only needed to undo the upside-down eye height, which Forge's EntityEvent.Size drove — that
    // event is gone from 1.20.2 on (see ServerEvents#onEntityResize), so there is nothing to refresh.
    //? if <1.20.2 {
    public void removeAttributeModifiers(LivingEntity entityLivingBaseIn, AttributeMap attributeMapIn, int amplifier) {
        super.removeAttributeModifiers(entityLivingBaseIn, attributeMapIn, amplifier);
        entityLivingBaseIn.refreshDimensions();
    }
    //?}

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
        return "alexsmobs.potion.clinging";
    }

}
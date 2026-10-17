package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.entity.util.AnacondaPartIndex;
import com.github.alexthe666.alexsmobs.message.MessageHurtMultipart;
import com.github.alexthe666.alexsmobs.misc.AMBlockPos;
import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import com.github.alexthe666.alexsmobs.misc.AMCompat;

public class EntityAnacondaPart extends LivingEntity implements IHurtableMultipart {
    private static final EntityDataAccessor<Integer> BODYINDEX = SynchedEntityData.defineId(EntityAnacondaPart.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BODY_TYPE = SynchedEntityData.defineId(EntityAnacondaPart.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> TARGET_YAW = SynchedEntityData.defineId(EntityAnacondaPart.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Optional<UUID>> CHILD_UUID = SynchedEntityData.defineId(EntityAnacondaPart.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> PARENT_UUID = SynchedEntityData.defineId(EntityAnacondaPart.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Float> SWELL = SynchedEntityData.defineId(EntityAnacondaPart.class, EntityDataSerializers.FLOAT);
    public EntityDimensions multipartSize;
    private float strangleProgess;
    private float prevSwell;
    private float prevStrangleProgess;
    private int headEntityId = -1;
    private double prevHeight = 0;
//    public Vec3[] stranglePosition = new Vec3[]{
//            new Vec3(0.5, 0, 0),
//            new Vec3(-0.5, 0, 0),
//            new Vec3(-1, 0, 0),
//            new Vec3(0, 0, 0),
//            new Vec3(1, 0, 0),
//            new Vec3(0, 0, 0),
//            new Vec3(-1, 0, 0),
//    };
    private static final EntityDataAccessor<Boolean> YELLOW = SynchedEntityData.defineId(EntityAnacondaPart.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SHEDDING = SynchedEntityData.defineId(EntityAnacondaPart.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> BABY = SynchedEntityData.defineId(EntityAnacondaPart.class, EntityDataSerializers.BOOLEAN);

    public EntityAnacondaPart(EntityType t, Level world) {
        super(t, world);
        multipartSize = t.getDimensions();
    }

    public EntityAnacondaPart(EntityType t, LivingEntity parent) {
        super(t, parent.level());
        this.setParent(parent);
    }

    @Override
    public InteractionResult interact(Player p_19978_, InteractionHand p_19979_) {
        return this.getParent() == null ? super.interact(p_19978_, p_19979_) : this.getParent().interact(p_19978_, p_19979_);
    }

    public static AttributeSupplier.Builder bakeAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 10.0D).add(Attributes.MOVEMENT_SPEED, 0.15F);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.is(DamageTypes.IN_WALL)  || super.isInvulnerableTo(source);
    }

    public boolean isNoGravity() {
        return false;
    }


    @Override
    public void tick() {
        super.tick();

        prevStrangleProgess = strangleProgess;
        prevSwell = this.getSwell();
        // 1.21 replaced Entity's isInsidePortal/portalTime fields with a nullable PortalProcessor.
        //? if >=1.21 {
        /*portalProcess = null;
        *///?} else {
        isInsidePortal = false;
        //?}
        this.setDeltaMovement(Vec3.ZERO);
        if (this.tickCount > 1) {
            final Entity parent = getParent();
            refreshDimensions();
            if (!this.level().isClientSide()) {
                if (parent == null) {
                    this.remove(RemovalReason.DISCARDED);
                }
                if (parent != null) {
                    if (parent instanceof final LivingEntity livingEntityParent) {
                        if (livingEntityParent.hurtTime > 0 || livingEntityParent.deathTime > 0) {
                            AlexsMobs.sendMSGToAll(new MessageHurtMultipart(this.getId(), parent.getId(), 0));
                            this.hurtTime = livingEntityParent.hurtTime;
                            this.deathTime = livingEntityParent.deathTime;
                        }
                    }
                    if (parent.isRemoved()) {
                        this.remove(RemovalReason.DISCARDED);
                    }
                } else if (tickCount > 20) {
                    remove(RemovalReason.DISCARDED);
                }
                if (this.getSwell() > 0) {
                    final float swellInc = 0.25F;
                    if (parent instanceof EntityAnaconda || parent instanceof EntityAnacondaPart && ((EntityAnacondaPart) parent).getSwell() == 0) {
                        if (this.getChild() != null) {
                            final EntityAnacondaPart child = (EntityAnacondaPart) this.getChild();
                            if (child.getPartType() == AnacondaPartIndex.TAIL) {
                                if (this.getSwell() == swellInc) {
                                    this.feedAnaconda();
                                }
                            } else {
                                child.setSwell(child.getSwell() + swellInc);
                            }
                        }
                        this.setSwell(this.getSwell() - swellInc);
                    }
                }
            }
        }
    }

    private void feedAnaconda() {
        Entity e = this.getParent();
        while (e instanceof EntityAnacondaPart) {
            e = ((EntityAnacondaPart) e).getParent();
        }

        if (e instanceof EntityAnaconda)
            ((EntityAnaconda) e).feed();
    }

    public Vec3 tickMultipartPosition(int headId, AnacondaPartIndex parentIndex, Vec3 parentPosition, float parentXRot, float parentYRot, float ourYRot, boolean doHeight) {
        final Vec3 parentButt = parentPosition.add(calcOffsetVec(-parentIndex.getBackOffset() * this.getScale(), parentXRot, parentYRot));
        final Vec3 ourButt = parentButt.add(calcOffsetVec((-this.getPartType().getBackOffset() - 0.5F * this.getBbWidth()) * this.getScale(), this.getXRot(), ourYRot));
        final Vec3 avg = new Vec3((parentButt.x + ourButt.x) / 2F, (parentButt.y + ourButt.y) / 2F, (parentButt.z + ourButt.z) / 2F);
        final double d0 = parentButt.x - ourButt.x;
//        final double d1 = parentButt.y - ourButt.y;
        final double d2 = parentButt.z - ourButt.z;
        final double d3 = Math.sqrt(d0 * d0 + d2 * d2);
        final double hgt = doHeight ? (getLowPartHeight(parentButt.x, parentButt.y, parentButt.z) + getHighPartHeight(ourButt.x, ourButt.y, ourButt.z)) : 0;
        if (Math.abs(hgt - prevHeight) > 0.2F) {
            prevHeight = hgt;
        }
        final double partYDest = Mth.clamp(this.getScale() * prevHeight, -0.6F, 0.6F);
        final float f = (float) (Mth.atan2(d2, d0) * 57.2957763671875D) - 90.0F;
        final float rawAngle = Mth.wrapDegrees((float) (-(Mth.atan2(partYDest, d3) * Mth.RAD_TO_DEG)));
        final float f2 = this.limitAngle(this.getXRot(), rawAngle, 10F);
        this.setXRot(f2);
        this.setYRot(f);
        this.yHeadRot = f;
        final Vec3 grounded = new Vec3(avg.x, liftOutOfGround(avg.x, avg.y, avg.z), avg.z);
        this.moveTo(grounded.x, grounded.y, grounded.z, f, f2);
        headEntityId = headId;
        return grounded;
    }

    /**
     * Keeps a body segment out of the floor. The pitch chain above reads the terrain in coarse
     * 0.2-block steps, so a segment trailing a head that has been knocked slightly upwards gets
     * aimed far enough down that its feet end up inside the block it is lying on — the reported
     * "anaconda's tail clipped through the ground after I hit it" (measured: parts at y 150.79
     * on a floor whose top face is y 151.0).
     *
     * Deliberately a point test rather than the 1x1 slab {@link #isOpaqueBlockAt} uses: that one
     * would report "solid" for a wall standing beside the snake and lift a segment that is
     * perfectly fine. Only ever raises, and is a no-op once the segment is on the surface.
     */
    private double liftOutOfGround(double x, double y, double z) {
        if (this.noPhysics || isFluidAt(x, y, z)) {
            return y;
        }
        final double probe = y + 1.0E-4D;
        final BlockPos pos = AMBlockPos.fromCoords(x, probe, z);
        final BlockState state = this.level().getBlockState(pos);
        if (state.isAir() || !state.isSuffocating(this.level(), pos)) {
            return y;
        }
        final VoxelShape shape = state.getCollisionShape(this.level(), pos);
        if (shape.isEmpty()) {
            return y;
        }
        final AABB point = AABB.ofSize(new Vec3(x, probe, z), 1.0E-6D, 1.0E-6D, 1.0E-6D);
        if (!Shapes.joinIsNotEmpty(shape.move(pos.getX(), pos.getY(), pos.getZ()), Shapes.create(point), BooleanOp.AND)) {
            return y;
        }
        return Math.max(y, pos.getY() + shape.max(Direction.Axis.Y));
    }

    public double getLowPartHeight(double x, double yIn, double z) {
        if (isFluidAt(x, yIn, z))
            return 0.0D;

        double checkAt = 0D;
        while (checkAt > -3D && !isOpaqueBlockAt(x,yIn + checkAt, z)) {
            checkAt -= 0.2D;
        }

        return checkAt;
    }

    public double getHighPartHeight(double x, double yIn, double z) {
        if (isFluidAt(x, yIn, z))
            return 0.0D;

        double checkAt = 0D;
        while (checkAt <= 3D) {
            if (isOpaqueBlockAt(x, yIn + checkAt, z)) {
                checkAt += 0.2D;
            } else {
                break;
            }
        }

        return checkAt;
    }


    public boolean isOpaqueBlockAt(double x, double y, double z) {
        if (this.noPhysics) {
            return false;
        } else {
            final double d = 1D;
            final Vec3 vec3 = new Vec3(x, y, z);
            final AABB axisAlignedBB = AABB.ofSize(vec3, d, 1.0E-6D, d);
            return this.level().getBlockStates(axisAlignedBB).filter(Predicate.not(BlockBehaviour.BlockStateBase::isAir)).anyMatch((p_185969_) -> {
                BlockPos blockpos = AMBlockPos.fromVec3(vec3);
                return p_185969_.isSuffocating(this.level(), blockpos) && Shapes.joinIsNotEmpty(p_185969_.getCollisionShape(this.level(), blockpos).move(vec3.x, vec3.y, vec3.z), Shapes.create(axisAlignedBB), BooleanOp.AND);
            });
        }
    }

    //? if <1.20.2 {
    public boolean canBreatheUnderwater() {
        return true;
    }
    //?}

    public boolean isPushedByFluid() {
        return false;
    }

    public boolean isFluidAt(double x, double y, double z) {
        if (this.noPhysics) {
            return false;
        } else {
            return !level().getFluidState(AMBlockPos.fromCoords(x, y, z)).isEmpty();
        }
    }

    public boolean hurtHeadId(DamageSource source, float f) {
        if (headEntityId != -1) {
            Entity e = level().getEntity(headEntityId);
            if (e instanceof EntityAnaconda) {
               return AMCompat.hurt(e, source, f);
            }
        }
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float damage) {
        return hurtHeadId(source, damage);
    }

    @Override
    //? if >=1.20.5 {
    /*protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
    *///?} else {
    protected void defineSynchedData() {
        super.defineSynchedData();
        SynchedEntityData builder = this.entityData;
    //?}
        builder.define(CHILD_UUID, Optional.empty());
        builder.define(PARENT_UUID, Optional.empty());
        builder.define(BODYINDEX, 0);
        builder.define(BODY_TYPE, AnacondaPartIndex.NECK.ordinal());
        builder.define(TARGET_YAW, 0F);
        builder.define(SWELL, 0F);
        builder.define(YELLOW, false);
        builder.define(SHEDDING, false);
        builder.define(BABY, false);
    }


    public void pushEntities() {
        final List<Entity> entities = this.level().getEntities(this, this.getBoundingBox().expandTowards(0.2D, 0.0D, 0.2D));
        final Entity parent = this.getParent();
        if (parent != null) {
            entities.stream().filter(entity -> !entity.is(parent) && !(entity instanceof EntityAnacondaPart || entity instanceof EntityAnaconda) && entity.isPushable()).forEach(entity -> entity.push(parent));
        }
    }

    //? if <1.21.5 {
    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return ImmutableList.of();
    }
    //?}

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slotIn) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(EquipmentSlot p_21036_, ItemStack p_21037_) {

    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public void onAttackedFromServer(LivingEntity parent, float damage, DamageSource damageSource) {
        if (parent.deathTime > 0)
            this.deathTime = parent.deathTime;

        if (parent.hurtTime > 0)
            this.hurtTime = parent.hurtTime;
    }

    public Entity getParent() {
        if (!this.level().isClientSide()) {
            final UUID id = getParentId();
            if (id != null) {
                return ((ServerLevel) level()).getEntity(id);
            }
        }

        return null;
    }

    public void setParent(Entity entity) {
        this.setParentId(entity.getUUID());
    }

    @Nullable
    public UUID getParentId() {
        return this.entityData.get(PARENT_UUID).orElse(null);
    }

    public void setParentId(@Nullable UUID uniqueId) {
        this.entityData.set(PARENT_UUID, Optional.ofNullable(uniqueId));
    }

    public Entity getChild() {
        if (!this.level().isClientSide()) {
            final UUID id = getChildId();
            if (id != null) {
                return ((ServerLevel) level()).getEntity(id);
            }
        }

        return null;
    }

    @Nullable
    public UUID getChildId() {
        return this.entityData.get(CHILD_UUID).orElse(null);
    }

    public void setChildId(@Nullable UUID uniqueId) {
        this.entityData.set(CHILD_UUID, Optional.ofNullable(uniqueId));
    }

    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        if (this.getParentId() != null) {
            AMCompat.putUUID(compound, "ParentUUID", this.getParentId());
        }
        if (this.getChildId() != null) {
            AMCompat.putUUID(compound, "ChildUUID", this.getChildId());
        }
        compound.putInt("BodyModel", getPartType().ordinal());
        compound.putInt("BodyIndex", getBodyIndex());
    }

    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (AMCompat.hasUUID(compound, "ParentUUID")) {
            this.setParentId(AMCompat.getUUID(compound, "ParentUUID"));
        }
        if (AMCompat.hasUUID(compound, "ChildUUID")) {
            this.setChildId(AMCompat.getUUID(compound, "ChildUUID"));
        }
        this.setPartType(AnacondaPartIndex.fromOrdinal(AMCompat.getInt(compound, "BodyModel")));
        this.setBodyIndex(AMCompat.getInt(compound, "BodyIndex"));
    }

    @Override
    public boolean is(net.minecraft.world.entity.Entity entity) {
        return this == entity || this.getParent() == entity;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Nullable
    public ItemStack getPickResult() {
        Entity parent = this.getParent();
        return parent != null ? parent.getPickResult() : ItemStack.EMPTY;
    }

    public int getBodyIndex() {
        return this.entityData.get(BODYINDEX);
    }

    public void setBodyIndex(int index) {
        this.entityData.set(BODYINDEX, index);
    }

    public AnacondaPartIndex getPartType() {
        return AnacondaPartIndex.fromOrdinal(this.entityData.get(BODY_TYPE));
    }

    public void setPartType(AnacondaPartIndex index) {
        this.entityData.set(BODY_TYPE, index.ordinal());
    }

    public void setTargetYaw(float f) {
        this.entityData.set(TARGET_YAW, f);
    }

    public void setSwell(float f) {
        this.entityData.set(SWELL, f);
    }

    public float getSwell(){
        return Math.min(this.entityData.get(SWELL), 5);
    }


    public float getSwellLerp(float partialTick) {
        return this.prevSwell + (Math.max(this.getSwell(), 0) - this.prevSwell) * partialTick;
    }


    @Override
    public float getYRot() {
        return super.getYRot();
    }

    public void setStrangleProgress(float f){
        this.strangleProgess = f;
    }

    public float getStrangleProgress(float partialTick){
        return this.prevStrangleProgess + (this.strangleProgess - this.prevStrangleProgess) * partialTick;
    }

    public void copyDataFrom(EntityAnaconda anaconda) {
        this.entityData.set(YELLOW, anaconda.isYellow());
        this.entityData.set(SHEDDING, anaconda.isShedding());
        this.entityData.set(BABY, anaconda.isBaby());
    }

    public boolean isYellow(){
        return this.entityData.get(YELLOW);
    }

    public boolean isShedding(){
        return this.entityData.get(SHEDDING);
    }

    @Override
    public boolean isBaby(){
        return this.entityData.get(BABY);
    }
}

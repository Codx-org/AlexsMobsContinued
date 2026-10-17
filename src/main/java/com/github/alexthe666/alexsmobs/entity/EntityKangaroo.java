package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.misc.AMCompat;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.message.MessageKangarooEat;
import com.github.alexthe666.alexsmobs.message.MessageKangarooInventorySync;
import com.github.alexthe666.alexsmobs.misc.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.misc.AMTagRegistry;
import com.github.alexthe666.alexsmobs.citadel.animation.Animation;
import com.github.alexthe666.alexsmobs.citadel.animation.AnimationHandler;
import com.github.alexthe666.alexsmobs.citadel.animation.IAnimatedEntity;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.JumpControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DispenserMenu;
//? if <1.21.5 {
import net.minecraft.world.item.ArmorItem;
//?}
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.Map;

// 26.1 deleted net.minecraft.world.ContainerListener and SimpleContainer#addListener with it.
// The listener's only job here was to call resetKangarooSlots() on every change, which the
// container's own setChanged() override below reproduces exactly.
//? if >=26 {
/*public class EntityKangaroo extends TamableAnimal implements IAnimatedEntity, IFollower {
*///?} else {
public class EntityKangaroo extends TamableAnimal implements ContainerListener, IAnimatedEntity, IFollower {
//?}

    public static final Animation ANIMATION_EAT_GRASS = Animation.create(30);
    public static final Animation ANIMATION_KICK = Animation.create(15);
    public static final Animation ANIMATION_PUNCH_R = Animation.create(13);
    public static final Animation ANIMATION_PUNCH_L = Animation.create(13);
    // Not upstream (#59): the wind-up thrust a kangaroo uses in place of a jab when its main hand
    // holds something in AMTagRegistry#KANGAROO_SPEARS. 21 of the 22 ticks are spent in the model's
    // keyframes — 5 raising the weapon over the shoulder, 5 holding it there, 3 thrusting, 2 on the
    // follow-through and 6 returning — so the hit lands on tick 13, when the thrust finishes.
    public static final Animation ANIMATION_STAB_R = Animation.create(22);
    public static final Animation ANIMATION_STAB_L = Animation.create(22);
    public static final int STAB_HIT_TICK = 13;
    private static final EntityDataAccessor<Boolean> STANDING = SynchedEntityData.defineId(EntityKangaroo.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SITTING = SynchedEntityData.defineId(EntityKangaroo.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> COMMAND = SynchedEntityData.defineId(EntityKangaroo.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> VISUAL_FLAG = SynchedEntityData.defineId(EntityKangaroo.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> POUCH_TICK = SynchedEntityData.defineId(EntityKangaroo.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HELMET_INDEX = SynchedEntityData.defineId(EntityKangaroo.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SWORD_INDEX = SynchedEntityData.defineId(EntityKangaroo.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CHEST_INDEX = SynchedEntityData.defineId(EntityKangaroo.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FORCED_SIT = SynchedEntityData.defineId(EntityKangaroo.class, EntityDataSerializers.BOOLEAN);
    public float prevPouchProgress;
    public float pouchProgress;
    public float sitProgress;
    public float prevSitProgress;
    public float standProgress;
    public float prevStandProgress;
    public float totalMovingProgress;
    public float prevTotalMovingProgress;
    public int maxStandTime = 75;
    public SimpleContainer kangarooInventory;
    private int animationTick;
    private Animation currentAnimation;
    private int jumpTicks;
    private int jumpDuration;
    private boolean wasOnGround;
    private int currentMoveTypeDuration;
    private int standingTime = 0;
    private int sittingTime = 0;
    private int maxSitTime = 75;
    private int eatCooldown = 0;
    private int carrotFeedings = 0;
    private int clientArmorCooldown = 0;

    protected EntityKangaroo(EntityType type, Level world) {
        super(type, world);
        initKangarooInventory();
        this.jumpControl = new JumpHelperController(this);
        this.moveControl = new EntityKangaroo.MoveHelperController(this);

    }

    public static <T extends Mob> boolean canKangarooSpawn(EntityType<? extends Animal> animal, LevelAccessor worldIn, MobSpawnType reason, BlockPos pos, RandomSource random) {
        boolean spawnBlock = worldIn.getBlockState(pos.below()).is(AMTagRegistry.KANGAROO_SPAWNS);
        return spawnBlock && worldIn.getRawBrightness(pos, 0) > 8;
    }

    public static AttributeSupplier.Builder bakeAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 22.0D).add(Attributes.FOLLOW_RANGE, 32.0D).add(Attributes.MOVEMENT_SPEED, 0.5F).add(Attributes.ATTACK_DAMAGE, 4F);
    }

    @Nullable
    public LivingEntity getControllingPassenger() {
        return null;
    }
    // 1.21 moved leashing into the Leashable interface: Mob#tickLeash is gone and the per-mob
    // hook is handleLeashAtDistance, which the (static) tick calls once a live holder is known.
    // Returning false there means "I handled it, skip the default pull", which is exactly what
    // this override always meant.
    //
    // 1.21.6 split that one hook into a staged set (leashTooFarBehaviour beyond the snap distance,
    // onElasticLeashPull in the elastic band, closeRangeLeashBehaviour inside it). Overriding the
    // two per-tick ones covers every tick the lead is live and unbroken, which is what the single
    // hook did. The only difference is that vanilla now snaps the lead at 12 blocks rather than
    // the kangaroo's own 10 — amTickLeash's drop-at-10 still fires first in practice.
    //? if >=1.21.6 {
    /*@Override
    public void closeRangeLeashBehaviour(Entity holder) {
        amTickLeash();
    }

    @Override
    public void onElasticLeashPull() {
        amTickLeash();
    }
    *///?} elif >=1.21 {
    /*@Override
    public boolean handleLeashAtDistance(Entity holder, float distance) {
        amTickLeash();
        return false;
    }
    *///?} else {
    protected void tickLeash() {
        super.tickLeash();
        amTickLeash();
    }
    //?}

    private void amTickLeash() {
        Entity lvt_1_1_ = this.getLeashHolder();
        if (lvt_1_1_ != null && lvt_1_1_.level() == this.level()) {
            this.restrictTo(lvt_1_1_.blockPosition(), 5);
            float lvt_2_1_ = this.distanceTo(lvt_1_1_);
            if (this.isSitting()) {
                if (lvt_2_1_ > 10.0F) {
                    this.dropLeash(true, true);
                }

                return;
            }

            // Forge's onLeashDistance hook is what 1.21's handleLeashAtDistance replaced.
            //? if <1.21
            this.onLeashDistance(lvt_2_1_);
            if (lvt_2_1_ > 10.0F) {
                this.dropLeash(true, true);
                this.goalSelector.disableControlFlag(Goal.Flag.MOVE);
            } else if (lvt_2_1_ > 6.0F) {
                double lvt_3_1_ = (lvt_1_1_.getX() - this.getX()) / (double) lvt_2_1_;
                double lvt_5_1_ = (lvt_1_1_.getY() - this.getY()) / (double) lvt_2_1_;
                double lvt_7_1_ = (lvt_1_1_.getZ() - this.getZ()) / (double) lvt_2_1_;
                this.setDeltaMovement(this.getDeltaMovement().add(Math.copySign(lvt_3_1_ * lvt_3_1_ * 0.4D, lvt_3_1_), Math.copySign(lvt_5_1_ * lvt_5_1_ * 0.4D, lvt_5_1_), Math.copySign(lvt_7_1_ * lvt_7_1_ * 0.4D, lvt_7_1_)));
            } else {
                this.goalSelector.enableControlFlag(Goal.Flag.MOVE);
                float lvt_3_2_ = 2.0F;
                try {
                    Vec3 lvt_4_1_ = (new Vec3(lvt_1_1_.getX() - this.getX(), lvt_1_1_.getY() - this.getY(), lvt_1_1_.getZ() - this.getZ())).normalize().scale(Math.max(lvt_2_1_ - 2.0F, 0.0F));
                    this.getNavigation().moveTo(this.getX() + lvt_4_1_.x, this.getY() + lvt_4_1_.y, this.getZ() + lvt_4_1_.z, this.followLeashSpeed());
                } catch (Exception e) {

                }
            }
        }

    }

    public boolean forcedSit() {
        return entityData.get(FORCED_SIT);
    }

    public boolean isRoger() {
        String s = ChatFormatting.stripFormatting(this.getName().getString());
        return s != null && s.equalsIgnoreCase("roger");
    }

    public boolean checkSpawnRules(LevelAccessor worldIn, MobSpawnType spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.kangarooSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.KANGAROO_IDLE.get();
    }

    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.KANGAROO_IDLE.get();
    }

    private void initKangarooInventory() {
        SimpleContainer animalchest = this.kangarooInventory;
        this.kangarooInventory = new SimpleContainer(9) {
            public void stopOpen(Player player) {
                EntityKangaroo.this.entityData.set(POUCH_TICK, 10);
                EntityKangaroo.this.resetKangarooSlots();
            }

            //? if >=26 {
            /*public void setChanged() {
                super.setChanged();
                EntityKangaroo.this.resetKangarooSlots();
            }
            *///?}

            public boolean stillValid(Player player) {
                //? if >=1.21 {
                /*return EntityKangaroo.this.isAlive() && EntityKangaroo.this.portalProcess == null;
                *///?} else {
                return EntityKangaroo.this.isAlive() && !EntityKangaroo.this.isInsidePortal;
                //?}
            }
        };
        //? if <26 {
        kangarooInventory.addListener(this);
        //?}
        if (animalchest != null) {
            int i = Math.min(animalchest.getContainerSize(), this.kangarooInventory.getContainerSize());
            for (int j = 0; j < i; ++j) {
                ItemStack itemstack = animalchest.getItem(j);
                if (!itemstack.isEmpty()) {
                    this.kangarooInventory.setItem(j, itemstack.copy());
                }
            }
            resetKangarooSlots();
        }

    }


    protected void dropEquipment() {
        super.dropEquipment();
        for (int i = 0; i < kangarooInventory.getContainerSize(); i++) {
            AMCompat.spawnAtLocation(this, kangarooInventory.getItem(i));
        }
        kangarooInventory.clearContent();
    }

    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        Item item = itemstack.getItem();
        InteractionResult type = super.mobInteract(player, hand);
        if (!isTame() && itemstack.is(AMTagRegistry.KANGAROO_TAMEABLES)) {
            this.usePlayerItem(player, hand, itemstack);
            this.gameEvent(GameEvent.EAT);
            this.playSound(SoundEvents.HORSE_EAT, this.getSoundVolume(), this.getVoicePitch());
            carrotFeedings++;
            if (carrotFeedings > 10 && getRandom().nextInt(2) == 0 || carrotFeedings > 15) {
                this.tame(player);
                this.level().broadcastEntityEvent(this, (byte) 7);
            } else {
                this.level().broadcastEntityEvent(this, (byte) 6);
            }
            return InteractionResult.SUCCESS;
        }
        if (isTame() && this.getHealth() < this.getMaxHealth() && AMCompat.isEdible(item) && AMCompat.getFoodProperties(item) != null && !AMCompat.isMeat(item)) {
            this.usePlayerItem(player, hand, itemstack);
            this.gameEvent(GameEvent.EAT);
            this.playSound(SoundEvents.HORSE_EAT, this.getSoundVolume(), this.getVoicePitch());
            this.heal(AMCompat.nutrition(AMCompat.getFoodProperties(item)));
            return InteractionResult.SUCCESS;
        }
        InteractionResult interactionresult = itemstack.interactLivingEntity(player, this, hand);
        if (interactionresult != InteractionResult.SUCCESS && type != InteractionResult.SUCCESS && isTame() && isOwnedBy(player) && !isFood(itemstack)) {
            if (player.isShiftKeyDown()) {
                if(!this.isBaby()){
                    this.openGUI(player);
                    this.ejectPassengers();
                    this.entityData.set(POUCH_TICK, -1);
                }
                return InteractionResult.SUCCESS;
            } else {
                this.setCommand(this.getCommand() + 1);
                if (this.getCommand() == 3) {
                    this.setCommand(0);
                }
                player.displayClientMessage(Component.translatable("entity.alexsmobs.all.command_" + this.getCommand(), this.getName()), true);
                boolean sit = this.getCommand() == 2;
                if (sit) {
                    this.entityData.set(FORCED_SIT, true);
                    this.setOrderedToSit(true);
                    return InteractionResult.SUCCESS;
                } else {
                    this.entityData.set(FORCED_SIT, false);
                    maxSitTime = 0;
                    this.setOrderedToSit(false);
                    return InteractionResult.SUCCESS;
                }
            }
        }

        return type;
    }

    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("KangarooSitting", this.isSitting());
        compound.putBoolean("KangarooSittingForced", this.forcedSit());
        compound.putBoolean("Standing", this.isStanding());
        compound.putInt("Command", this.getCommand());
        compound.putInt("HelmetInvIndex", this.entityData.get(HELMET_INDEX));
        compound.putInt("SwordInvIndex", this.entityData.get(SWORD_INDEX));
        compound.putInt("ChestInvIndex", this.entityData.get(CHEST_INDEX));
        if (kangarooInventory != null) {
            ListTag nbttaglist = new ListTag();
            for (int i = 0; i < this.kangarooInventory.getContainerSize(); ++i) {
                ItemStack itemstack = this.kangarooInventory.getItem(i);
                if (!itemstack.isEmpty()) {
                    CompoundTag CompoundNBT = new CompoundTag();
                    CompoundNBT.putByte("Slot", (byte) i);
                    AMCompat.saveInto(this.level().registryAccess(), itemstack, CompoundNBT);
                    nbttaglist.add(CompoundNBT);
                }
            }
            AMCompat.put(compound, "Items", nbttaglist);
        }
    }

    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.setOrderedToSit(AMCompat.getBoolean(compound, "KangarooSitting"));
        this.entityData.set(FORCED_SIT, com.github.alexthe666.alexsmobs.misc.AMCompat.getBoolean(compound, "KangarooSittingForced"));
        this.setStanding(AMCompat.getBoolean(compound, "Standing"));
        this.setCommand(AMCompat.getInt(compound, "Command"));
        this.entityData.set(HELMET_INDEX, com.github.alexthe666.alexsmobs.misc.AMCompat.getInt(compound, "HelmetInvIndex"));
        this.entityData.set(SWORD_INDEX, com.github.alexthe666.alexsmobs.misc.AMCompat.getInt(compound, "SwordInvIndex"));
        this.entityData.set(CHEST_INDEX, com.github.alexthe666.alexsmobs.misc.AMCompat.getInt(compound, "ChestInvIndex"));
        if (kangarooInventory != null) {
            ListTag nbttaglist = AMCompat.getList(compound, "Items", 10);
            this.initKangarooInventory();
            for (int i = 0; i < nbttaglist.size(); ++i) {
                CompoundTag CompoundNBT = AMCompat.getCompound(nbttaglist, i);
                int j = AMCompat.getByte(CompoundNBT, "Slot") & 255;
                this.kangarooInventory.setItem(j, com.github.alexthe666.alexsmobs.misc.AMCompat.loadItem(this.level().registryAccess(), CompoundNBT));
            }
        } else {
            ListTag nbttaglist = AMCompat.getList(compound, "Items", 10);
            this.initKangarooInventory();
            for (int i = 0; i < nbttaglist.size(); ++i) {
                CompoundTag CompoundNBT = AMCompat.getCompound(nbttaglist, i);
                int j = AMCompat.getByte(CompoundNBT, "Slot") & 255;
                this.initKangarooInventory();
                this.kangarooInventory.setItem(j, com.github.alexthe666.alexsmobs.misc.AMCompat.loadItem(this.level().registryAccess(), CompoundNBT));
            }
        }
        resetKangarooSlots();
    }

    public void openGUI(Player playerEntity) {
        if (!this.level().isClientSide() && (!this.hasPassenger(playerEntity))) {
            ((ServerPlayer) playerEntity).openMenu(new MenuProvider() {
                @Override
                public AbstractContainerMenu createMenu(int p_createMenu_1_, Inventory p_createMenu_2_, Player p_createMenu_3_) {
                    return new DispenserMenu(p_createMenu_1_, p_createMenu_2_, kangarooInventory);
                }

                @Override
                public Component getDisplayName() {
                    return Component.translatable("entity.alexsmobs.kangaroo.pouch");
                }
            });
        }
    }

    public boolean isSitting() {
        return this.entityData.get(SITTING);
    }

    public void setOrderedToSit(boolean sit) {
        this.entityData.set(SITTING, Boolean.valueOf(sit));
    }

    public boolean isStanding() {
        return this.entityData.get(STANDING);
    }

    public void setStanding(boolean standing) {
        this.entityData.set(STANDING, Boolean.valueOf(standing));
    }

    public int getCommand() {
        return this.entityData.get(COMMAND);
    }

    public void setCommand(int command) {
        this.entityData.set(COMMAND, Integer.valueOf(command));
    }

    public int getVisualFlag() {
        return this.entityData.get(VISUAL_FLAG);
    }

    public void setVisualFlag(int visualFlag) {
        this.entityData.set(VISUAL_FLAG, Integer.valueOf(visualFlag));
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
        builder.define(STANDING, false);
        builder.define(SITTING, false);
        builder.define(FORCED_SIT, false);
        builder.define(COMMAND, Integer.valueOf(0));
        builder.define(VISUAL_FLAG, Integer.valueOf(0));
        builder.define(POUCH_TICK, Integer.valueOf(0));
        builder.define(CHEST_INDEX, Integer.valueOf(-1));
        builder.define(HELMET_INDEX, Integer.valueOf(-1));
        builder.define(SWORD_INDEX, Integer.valueOf(-1));
    }

    @Override
    protected PathNavigation createNavigation(Level worldIn) {
        return new GroundPathNavigatorWide(this, worldIn, 2F);
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(0, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(1, new KangarooAIMelee(this, 1.2D, false));
        this.goalSelector.addGoal(2, new FloatGoal(this));
        this.goalSelector.addGoal(2, new TameableAIFollowOwner(this, 1.2D, 5.0F, 2.0F, false));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1D));
        this.goalSelector.addGoal(4, new AnimalAIRideParent(this, 1.25D));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.2D, AMCompat.ingredientOf(AMTagRegistry.KANGAROO_TAMEABLES), false));
        this.goalSelector.addGoal(5, new AnimalAIWanderRanged(this, 110, 1.2D, 10, 7));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, (new AnimalAIHurtByTargetNotBaby(this)));
    }

    protected boolean canAddPassenger(Entity passenger) {
        return super.canAddPassenger(passenger) && this.entityData.get(POUCH_TICK) == 0;
    }


    // The joey rides in the pouch (AnimalAIRideParent). 1.20.2 deleted
    // Entity#getPassengersRidingOffset, and the EntityAttachment.PASSENGER fallback that
    // replaced it is AT_HEIGHT — the full 1.5, not 0.35 of it — so without this the joey sits
    // a block above its mother's head instead of in the pouch.
    //? if >=1.20.2 {
    /*@Override
    public Vec3 getPassengerRidingPosition(Entity passenger) {
        return new Vec3(this.getX(), this.getY() + this.getPassengersRidingOffset(), this.getZ());
    }
    *///?}

    public double getPassengersRidingOffset() {
        return (double) this.getBbHeight() * 0.35F;
    }

    // onAddedToWorld is a Forge IForgeEntity hook with no vanilla equivalent. On Fabric the
    // kangaroo's inventory is therefore not pushed to clients at spawn time; it still syncs on
    // every later change, so the only loss is the first tick after the chunk loads.
    //? if !fabric {
    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        updateClientInventory();
    }
    //?}

    public void tick() {
        super.tick();
        boolean moving = this.getDeltaMovement().lengthSqr() > 0.03D;
        int pouchTick = this.entityData.get(POUCH_TICK);
        this.prevTotalMovingProgress = totalMovingProgress;
        this.prevPouchProgress = pouchProgress;
        this.prevSitProgress = sitProgress;
        this.prevStandProgress = standProgress;

        if (this.isSitting()) {
            if (sitProgress < 5F) {
                sitProgress++;
            }
        } else {
            if (sitProgress > 0F)
                sitProgress--;
        }

        if (eatCooldown > 0) {
            eatCooldown--;
        }

        if (this.isStanding()) {
            if (standProgress < 5F)
                standProgress++;
        } else {
            if (standProgress > 0F)
                standProgress--;
        }

        if (moving) {
            if (totalMovingProgress < 5F) {
                totalMovingProgress++;
            }
        } else {
            if (totalMovingProgress > 0F)
                totalMovingProgress--;
        }

        if (pouchTick != 0 && pouchProgress < 5) {
            pouchProgress += 1;
        }
        if (pouchTick == 0 && pouchProgress > 0) {
            pouchProgress -= 1;
        }
        if (pouchTick > 0) {
            this.entityData.set(POUCH_TICK, pouchTick - 1);
        }
        if (isStanding() && ++standingTime > maxStandTime) {
            this.setStanding(false);
            standingTime = 0;
            maxStandTime = 75 + random.nextInt(50);
        }
        if (isSitting() && !forcedSit() && ++sittingTime > maxSitTime) {
            this.setOrderedToSit(false);
            sittingTime = 0;
            maxSitTime = 75 + random.nextInt(50);
        }
        if (!this.level().isClientSide() && this.getAnimation() == NO_ANIMATION && this.getCommand() != 1 && !this.isStanding() && !this.isSitting() && random.nextInt(1500) == 0) {
            maxSitTime = 500 + random.nextInt(350);
            this.setOrderedToSit(true);
        }
        if (!forcedSit() && this.isSitting() && (this.getTarget() != null || this.isStanding())) {
            this.setOrderedToSit(false);
        }
        if (this.getAnimation() == NO_ANIMATION && !this.isStanding() && !this.isSitting() && random.nextInt(1500) == 0) {
            maxStandTime = 75 + random.nextInt(50);
            this.setStanding(true);
        }
        if (this.forcedSit() && !this.isVehicle() && this.isTame()) {
            this.setOrderedToSit(true);
        }
        if (!this.level().isClientSide()) {
            if (tickCount == 1) {
                updateClientInventory();
            }

            if (!moving && this.getAnimation() == NO_ANIMATION && !this.isSitting() && !this.isStanding()) {
                if ((getRandom().nextInt(180) == 0 || this.getHealth() < this.getMaxHealth() && getRandom().nextInt(40) == 0) && level().getBlockState(this.blockPosition().below()).is(Blocks.GRASS_BLOCK)) {
                    this.setAnimation(ANIMATION_EAT_GRASS);
                }
            }
            if (this.getAnimation() == ANIMATION_EAT_GRASS && this.getAnimationTick() == 20 && this.getHealth() < this.getMaxHealth() && level().getBlockState(this.blockPosition().below()).is(Blocks.GRASS_BLOCK)) {
                this.heal(6);
                this.level().levelEvent(2001, blockPosition().below(), Block.getId(Blocks.GRASS_BLOCK.defaultBlockState()));
                this.level().setBlock(blockPosition().below(), Blocks.DIRT.defaultBlockState(), 2);
            }
            if (this.getHealth() < this.getMaxHealth() && this.isTame() && eatCooldown == 0) {
                eatCooldown = 20 + random.nextInt(40);
                if (!this.kangarooInventory.isEmpty()) {
                    ItemStack foodStack = ItemStack.EMPTY;
                    for (int i = 0; i < this.kangarooInventory.getContainerSize(); i++) {
                        ItemStack stack = this.kangarooInventory.getItem(i);
                        if (AMCompat.isEdible(stack.getItem()) && AMCompat.getFoodProperties(stack.getItem()) != null && !AMCompat.isMeat(stack.getItem())) {
                            foodStack = stack;
                        }
                    }
                    if (!foodStack.isEmpty() && AMCompat.getFoodProperties(foodStack.getItem()) != null) {
                        AlexsMobs.sendMSGToAll(new MessageKangarooEat(this.getId(), foodStack));
                        this.heal(AMCompat.nutrition(AMCompat.getFoodProperties(foodStack.getItem())) * 2);
                        foodStack.shrink(1);
                        this.gameEvent(GameEvent.EAT);
                        this.playSound(SoundEvents.GENERIC_EAT, this.getSoundVolume(), this.getVoicePitch());
                    }
                }
            }
        }
        if (this.jumpTicks < this.jumpDuration) {
            ++this.jumpTicks;
        } else if (this.jumpDuration != 0) {
            this.jumpTicks = 0;
            this.jumpDuration = 0;
            this.setJumping(false);
        }
        LivingEntity attackTarget = this.getTarget();
        if (attackTarget != null && this.hasLineOfSight(attackTarget)) {
            if (distanceTo(attackTarget) < attackTarget.getBbWidth() + this.getBbWidth() + 1) {
                if (this.getAnimation() == ANIMATION_KICK && this.getAnimationTick() == 8) {
                    AMCompat.knockback(attackTarget, 1.3F, Mth.sin(this.getYRot() * Mth.DEG_TO_RAD), -Mth.cos(this.getYRot() * Mth.DEG_TO_RAD));
                    AMCompat.doHurtTarget(this, this.getTarget());
                }
                if ((this.getAnimation() == ANIMATION_PUNCH_L) && this.getAnimationTick() == 6) {
                    float rot = getYRot() + 90;
                    AMCompat.knockback(attackTarget, 0.85F, Mth.sin(rot * Mth.DEG_TO_RAD), -Mth.cos(rot * Mth.DEG_TO_RAD));
                    AMCompat.doHurtTarget(this, this.getTarget());
                }
                if ((this.getAnimation() == ANIMATION_PUNCH_R) && this.getAnimationTick() == 6) {
                    float rot = getYRot() - 90;
                    AMCompat.knockback(attackTarget, 0.85F, Mth.sin(rot * Mth.DEG_TO_RAD), -Mth.cos(rot * Mth.DEG_TO_RAD));
                    AMCompat.doHurtTarget(this, this.getTarget());
                }
                // A thrust drives the target straight back, unlike the punches' sideways hooks.
                if ((this.getAnimation() == ANIMATION_STAB_R || this.getAnimation() == ANIMATION_STAB_L) && this.getAnimationTick() == STAB_HIT_TICK) {
                    float rot = getYRot();
                    AMCompat.knockback(attackTarget, 1.0F, Mth.sin(rot * Mth.DEG_TO_RAD), -Mth.cos(rot * Mth.DEG_TO_RAD));
                    AMCompat.doHurtTarget(this, this.getTarget());
                }
            }
            this.lookAt(attackTarget, 360, 360);
        }
        if (this.isBaby() && attackTarget != null) {
            this.setTarget(null);
        }
        if (this.isVehicle()) {
            this.entityData.set(POUCH_TICK, 10);
            this.setStanding(true);
            maxStandTime = 25;
        }

        if (this.isPassenger()) {
            if (this.isBaby() && this.getVehicle() instanceof EntityKangaroo) {
                EntityKangaroo mount = (EntityKangaroo) this.getVehicle();
                this.setYRot(mount.yBodyRot);
                this.yHeadRot = mount.yBodyRot;
                this.yBodyRot = mount.yBodyRot;
            }
            if (this.getVehicle() instanceof EntityKangaroo && !this.isBaby()) {
                this.removeVehicle();
            }
        }
        if (clientArmorCooldown > 0) {
            clientArmorCooldown--;
        }
        if (tickCount > 5 && !this.level().isClientSide() && clientArmorCooldown == 0 && this.isTame()) {
            this.updateClientInventory();
            clientArmorCooldown = 20;
        }
        AnimationHandler.INSTANCE.updateAnimations(this);
    }

    public boolean doHurtTarget(Entity entityIn) {
        boolean prev = super.doHurtTarget(entityIn);
        if (prev) {
            if (!this.getMainHandItem().isEmpty()) {
                damageItem(this.getMainHandItem(), EquipmentSlot.MAINHAND);
            }
        }
        return prev;
    }

    public boolean hurt(DamageSource src, float amount) {
        boolean prev = super.hurt(src, amount);
        if (prev) {
            if (!this.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
                damageItem(this.getItemBySlot(EquipmentSlot.HEAD), EquipmentSlot.HEAD);
            }
            if (!this.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) {
                damageItem(this.getItemBySlot(EquipmentSlot.CHEST), EquipmentSlot.CHEST);
            }
        }
        return prev;
    }

    // Upstream called ItemStack#hurt(1, random, null) here and then consumed the stack whenever its
    // damage value was still <= 0 — its way of saying "this item can't take damage, so eat it".
    // AMCompat#hurtItem cannot serve that: from 1.21 the vanilla call it wraps needs a ServerLevel,
    // which it can only reach through the ServerPlayer it is handed, and a mob has none. It became a
    // no-op, the damage value stayed 0, and upstream's next line then destroyed the item on the FIRST
    // hit. hurtAndBreak takes the holding entity instead, so it works for a mob on every version, and
    // it retires the shrink: breaking at zero durability is what it does internally.
    private void damageItem(ItemStack stack, EquipmentSlot slot) {
        if (stack != null && !stack.isEmpty()) {
            AMCompat.hurtAndBreak(stack, 1, this, slot);
        }
    }

    public boolean isInvulnerableTo(DamageSource source) {
        return super.isInvulnerableTo(source) || source.is(DamageTypes.IN_WALL);
    }

    public boolean isAlliedTo(Entity entityIn) {
        if (this.isTame()) {
            LivingEntity livingentity = this.getOwner();
            if (entityIn == livingentity) {
                return true;
            }
            if (entityIn instanceof TamableAnimal) {
                return ((TamableAnimal) entityIn).isOwnedBy(livingentity);
            }
            if (livingentity != null) {
                return livingentity.isAlliedTo(entityIn);
            }
        }

        return super.isAlliedTo(entityIn);
    }

    public MoveControl getMoveControl() {
        return this.moveControl;
    }
    public void travel(Vec3 vec3d) {
        if (this.isSitting() || this.getAnimation() == ANIMATION_EAT_GRASS) {
            if (this.getNavigation().getPath() != null) {
                this.getNavigation().stop();
            }
            vec3d = Vec3.ZERO;
        }
        super.travel(vec3d);
    }


    private void checkLandingDelay() {
        this.updateMoveTypeDuration();
        this.disableJumpControl();
    }

    public PathNavigation getNavigation() {
        return this.navigation;
    }

    @Nullable
    public Entity getControlledVehicle() {
        return this.getVehicle() instanceof EntityKangaroo ? null : super.getControlledVehicle();
    }

    private void enableJumpControl() {
        if (jumpControl instanceof EntityKangaroo.JumpHelperController) {
            ((EntityKangaroo.JumpHelperController) this.jumpControl).setCanJump(true);
        }
    }

    private void disableJumpControl() {
        if (jumpControl instanceof EntityKangaroo.JumpHelperController) {
            ((EntityKangaroo.JumpHelperController) this.jumpControl).setCanJump(false);
        }
    }

    private void updateMoveTypeDuration() {
        if (this.moveControl.getSpeedModifier() < 2D) {
            this.currentMoveTypeDuration = 2;
        } else {
            this.currentMoveTypeDuration = 1;
        }

    }

    private void calculateRotationYaw(double x, double z) {
        this.setYRot( (float) (Mth.atan2(z - this.getZ(), x - this.getX()) * (double) Mth.RAD_TO_DEG) - 90.0F);
    }

    public boolean canSpawnSprintParticle() {
        return false;
    }

    public void customServerAiStep() {
        super.customServerAiStep();

        if (this.currentMoveTypeDuration > 0) {
            --this.currentMoveTypeDuration;
        }

        if (this.onGround()) {
            if (!this.wasOnGround) {
                this.setJumping(false);
                this.checkLandingDelay();
            }

            if (this.currentMoveTypeDuration == 0) {
                LivingEntity livingentity = this.getTarget();
                if (livingentity != null && this.distanceToSqr(livingentity) < 16.0D) {
                    this.calculateRotationYaw(livingentity.getX(), livingentity.getZ());
                    this.moveControl.setWantedPosition(livingentity.getX(), livingentity.getY(), livingentity.getZ(), this.moveControl.getSpeedModifier());
                    this.startJumping();
                    this.wasOnGround = true;
                }
            }
            if (this.jumpControl instanceof EntityKangaroo.JumpHelperController) {
                EntityKangaroo.JumpHelperController rabbitController = (EntityKangaroo.JumpHelperController) this.jumpControl;
                if (!rabbitController.getIsJumping()) {
                    if (this.moveControl.hasWanted() && this.currentMoveTypeDuration == 0) {
                        Path path = this.navigation.getPath();
                        Vec3 vector3d = new Vec3(this.moveControl.getWantedX(), this.moveControl.getWantedY(), this.moveControl.getWantedZ());
                        if (path != null && !path.isDone()) {
                            vector3d = path.getNextEntityPos(this);
                        }
                        this.startJumping();
                    }
                } else if (!rabbitController.canJump()) {
                    this.enableJumpControl();
                }
            }
        }

        this.wasOnGround = this.onGround();
    }

    public float getJumpCompletion(float partialTicks) {
        return this.jumpDuration == 0 ? 0.0F : ((float) this.jumpTicks + partialTicks) / (float) this.jumpDuration;
    }

    @Override
    public int getAnimationTick() {
        return animationTick;
    }

    @Override
    public void setAnimationTick(int i) {
        animationTick = i;
    }

    @Override
    public Animation getAnimation() {
        return currentAnimation;
    }

    @Override
    public void setAnimation(Animation animation) {
        currentAnimation = animation;
        if (animation == ANIMATION_KICK) {
            this.setStanding(true);
            maxStandTime = 30;
        } else if (animation == ANIMATION_PUNCH_R) {
            this.setStanding(true);
            maxStandTime = 15;
        } else if (animation == ANIMATION_PUNCH_L) {
            this.setStanding(true);
            maxStandTime = 15;
        } else if (animation == ANIMATION_STAB_R || animation == ANIMATION_STAB_L) {
            this.setStanding(true);
            maxStandTime = 25;
        }

    }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{ANIMATION_EAT_GRASS, ANIMATION_KICK, ANIMATION_PUNCH_L, ANIMATION_PUNCH_R, ANIMATION_STAB_L, ANIMATION_STAB_R};
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel serverWorld, AgeableMob ageableEntity) {
        return AMCompat.create(AMEntityRegistry.KANGAROO.get(), serverWorld);
    }

    public void setMovementSpeed(double newSpeed) {
        this.getNavigation().setSpeedModifier(newSpeed);
        this.moveControl.setWantedPosition(this.moveControl.getWantedX(), this.moveControl.getWantedY(), this.moveControl.getWantedZ(), newSpeed);
    }

    protected float getJumpPower() {
        return 0.5F;
    }

    public boolean causeFallDamage(float distance, float damageMultiplier) {
        return false;
    }

    protected void checkFallDamage(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    public boolean isFood(ItemStack stack) {
        Item item = stack.getItem();
        return stack.is(AMTagRegistry.KANGAROO_BREEDABLES);
    }

    public void resetKangarooSlots() {
        if (!this.level().isClientSide()) {
            int swordIndex = -1;
            double swordDamage = 0;
            int helmetIndex = -1;
            double helmetArmor = 0;
            int chestplateIndex = -1;
            double chestplateArmor = 0;
            for (int i = 0; i < this.kangarooInventory.getContainerSize(); ++i) {
                ItemStack stack = this.kangarooInventory.getItem(i);
                if (!stack.isEmpty()) {
                    double dmg = getDamageForItem(stack);
                    if (dmg > 0 && dmg > swordDamage) {
                        swordDamage = dmg;
                        swordIndex = i;
                    }
                    // Item#canEquip is a Forge IForgeItem hook whose default answers from the same
                    // place AMCompat.equipmentSlotFor reads, so the Fabric arm is the vanilla path.
                    //? if !fabric
                    if (stack.getItem().canEquip(stack, EquipmentSlot.HEAD, this)  && !this.isBaby() && helmetIndex == -1) {
                    //? if fabric
                    /*if (com.github.alexthe666.alexsmobs.misc.AMCompat.equipmentSlotFor(stack) == EquipmentSlot.HEAD && !this.isBaby() && helmetIndex == -1) {*/
                        helmetIndex = i;
                    }
                    if (com.github.alexthe666.alexsmobs.misc.AMCompat.isArmor(stack) && !this.isBaby()) {
                        if (com.github.alexthe666.alexsmobs.misc.AMCompat.equipmentSlotFor(stack) == EquipmentSlot.HEAD) {
                            double prot = getProtectionForItem(stack, EquipmentSlot.HEAD);
                            if (prot > 0 && prot > helmetArmor) {
                                helmetArmor = prot;
                                helmetIndex = i;
                            }
                        }
                        if (com.github.alexthe666.alexsmobs.misc.AMCompat.equipmentSlotFor(stack) == EquipmentSlot.CHEST) {
                            double prot = getProtectionForItem(stack, EquipmentSlot.CHEST);
                            if (prot > 0 && prot > chestplateArmor) {
                                chestplateArmor = prot;
                                chestplateIndex = i;
                            }
                        }
                    }
                }
            }
            this.entityData.set(SWORD_INDEX, swordIndex);
            this.entityData.set(CHEST_INDEX, chestplateIndex);
            this.entityData.set(HELMET_INDEX, helmetIndex);
            updateClientInventory();
        }
    }

    private void updateClientInventory() {
        if (!this.level().isClientSide()) {
            for (int i = 0; i < 9; i++) {
                AlexsMobs.sendMSGToAll(new MessageKangarooInventorySync(this.getId(), i, kangarooInventory.getItem(i)));
            }
        }
    }

    @Nullable
    private Map<EquipmentSlot, ItemStack> collectEquipmentChanges() {
        Map<EquipmentSlot, ItemStack> map = null;

        for (EquipmentSlot equipmentslottype : EquipmentSlot.values()) {
            ItemStack itemstack;
            switch (equipmentslottype.getType()) {
                case HAND -> itemstack = this.getItemInHand(equipmentslottype);
                case ARMOR -> itemstack = this.getArmorInSlot(equipmentslottype);
                default -> {
                    continue;
                }
            }

            ItemStack itemstack1 = this.getItemBySlot(equipmentslottype);
            if (!ItemStack.matches(itemstack1, itemstack)) {
                // EventBus 7 (Forge 1.21.6) posts on the event's own bus; MinecraftForge.EVENT_BUS
                // kept register() but lost post() entirely.
                //? if forge && >=1.21.6 {
                /*net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent.BUS.post(new net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent(this, equipmentslottype, itemstack, itemstack1));
                *///?} elif fabric {
                /*// Fabric has no equipment-change event; nothing observes this on that loader.
                *///?} else {
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent(this, equipmentslottype, itemstack, itemstack1));
                //?}
                if (map == null) {
                    map = Maps.newEnumMap(EquipmentSlot.class);
                }

                map.put(equipmentslottype, itemstack1);
                if (!itemstack.isEmpty()) {
                    AMCompat.removeItemModifiers(this, itemstack, equipmentslottype);
                }

                if (!itemstack1.isEmpty()) {
                    AMCompat.addItemModifiers(this, itemstack1, equipmentslottype);
                }
            }
        }

        return map;
    }

    public ItemStack getItemBySlot(EquipmentSlot slotIn) {
        return switch (slotIn.getType()) {
            case HAND -> getItemInHand(slotIn);
            case ARMOR -> getArmorInSlot(slotIn);
            default -> ItemStack.EMPTY;
        };
    }

    private ItemStack getArmorInSlot(EquipmentSlot slot) {
        int helmIndex = entityData.get(HELMET_INDEX);
        int chestIndex = entityData.get(CHEST_INDEX);
        return slot == EquipmentSlot.HEAD && helmIndex >= 0 ? kangarooInventory.getItem(helmIndex) : slot == EquipmentSlot.CHEST && chestIndex >= 0 ? kangarooInventory.getItem(chestIndex) : ItemStack.EMPTY;
    }

    private ItemStack getItemInHand(EquipmentSlot slot) {
        int index = entityData.get(SWORD_INDEX);
        return slot == EquipmentSlot.MAINHAND && index >= 0 ? kangarooInventory.getItem(index) : ItemStack.EMPTY;
    }

    public double getDamageForItem(ItemStack itemStack) {
        return AMCompat.attackDamageOf(itemStack, EquipmentSlot.MAINHAND);
    }


    public double getProtectionForItem(ItemStack itemStack, EquipmentSlot type) {
        return AMCompat.armorOf(itemStack, type);
    }

    public void jumpFromGround() {
        super.jumpFromGround();
        double d0 = this.moveControl.getSpeedModifier();
        if (d0 > 0.0D) {
            double d1 = this.getDeltaMovement().horizontalDistanceSqr();
            if (d1 < 0.01D) {
            }
        }

        if (!this.level().isClientSide()) {
            this.level().broadcastEntityEvent(this, (byte) 1);
        }

    }

    public boolean hasJumper() {
        return jumpControl instanceof JumpHelperController;
    }

    public void startJumping() {
        if (!this.isSitting() || this.isInWater()) {
            this.setJumping(true);
            this.jumpDuration = 16;
            this.jumpTicks = 0;
        }

    }

    @OnlyIn(value = Dist.CLIENT)
    public void handleEntityEvent(byte id) {
        if (id == 1) {
            this.spawnSprintParticle();
            this.jumpDuration = 16;
            this.jumpTicks = 0;
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public boolean shouldFollow() {
        return this.getCommand() == 1;
    }

    //? if <26 {
    @Override
    public void containerChanged(Container iInventory) {
        this.resetKangarooSlots();
    }
    //?}

    static class MoveHelperController extends MoveControl {
        private final EntityKangaroo kangaroo;
        private double nextJumpSpeed;

        public MoveHelperController(EntityKangaroo kangaroo) {
            super(kangaroo);
            this.kangaroo = kangaroo;
        }

        public void tick() {
            if (this.kangaroo.hasJumper() && this.kangaroo.onGround() && !this.kangaroo.jumping && !((EntityKangaroo.JumpHelperController) this.kangaroo.jumpControl).getIsJumping()) {
                this.kangaroo.setMovementSpeed(0.0D);
            } else if (this.hasWanted()) {
                this.kangaroo.setMovementSpeed(this.nextJumpSpeed);
            }

            super.tick();
        }

        /**
         * Sets the speed and location to move to
         */
        public void setWantedPosition(double x, double y, double z, double speedIn) {
            if (this.kangaroo.isInWater()) {
                speedIn = 1.5D;
            }

            super.setWantedPosition(x, y, z, speedIn);
            if (speedIn > 0.0D) {
                this.nextJumpSpeed = speedIn;
            }

        }
    }

    public static class JumpHelperController extends JumpControl {
        private final EntityKangaroo kangaroo;
        private boolean canJump;

        public JumpHelperController(EntityKangaroo kangaroo) {
            super(kangaroo);
            this.kangaroo = kangaroo;
        }

        public boolean getIsJumping() {
            return this.jump;
        }

        public boolean canJump() {
            return this.canJump;
        }

        public void setCanJump(boolean canJumpIn) {
            this.canJump = canJumpIn;
        }

        public void tick() {
            if (this.jump) {
                this.kangaroo.startJumping();
                this.jump = false;
            }

        }
    }
}

package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.misc.AMCompat;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.AnimalAIFleeLight;
import com.github.alexthe666.alexsmobs.entity.ai.CreatureAITargetItems;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.github.alexthe666.alexsmobs.misc.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.misc.AMTagRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Forge 65 (MC 26.2) deleted net.minecraftforge.common.IForgeShearable. Vanilla Shearable is
// the entire shearing contract there — see the note on isShearable below. FABRIC takes that
// same arm at EVERY version: it has no loader shearing interface at all, so the arm that
// drops the Forge one is already the right shape. (The second gate is `neoforge || forge &&
// <26.2)` rather than `neoforge || <26.2` for exactly that reason — a bare version term would
// pull Fabric back onto net.minecraftforge below 26.2.)
//? if fabric || (forge && >=26.2)
//public class EntityCockroach extends Animal implements Shearable, ITargetsDroppedItems {
//? if neoforge || (forge && <26.2)
public class EntityCockroach extends Animal implements Shearable, net.minecraftforge.common.IForgeShearable, ITargetsDroppedItems {

    public static final ResourceLocation MARACA_LOOT = AMCompat.rl("alexsmobs", "entities/cockroach_maracas");
    public static final ResourceLocation MARACA_HEADLESS_LOOT = AMCompat.rl("alexsmobs", "entities/cockroach_maracas_headless");
    protected static final EntityDimensions STAND_SIZE = EntityDimensions.fixed(0.7F, 0.9F);
    private static final EntityDataAccessor<Boolean> DANCING = SynchedEntityData.defineId(EntityCockroach.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> HEADLESS = SynchedEntityData.defineId(EntityCockroach.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> MARACAS = SynchedEntityData.defineId(EntityCockroach.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<UUID>> NEAREST_MUSICIAN = SynchedEntityData.defineId(EntityCockroach.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> BREADED = SynchedEntityData.defineId(EntityCockroach.class, EntityDataSerializers.BOOLEAN);
    public int randomWingFlapTick = 0;
    public float prevDanceProgress;
    public float danceProgress;
    private boolean prevStand = false;
    private boolean isJukeboxing;
    private BlockPos jukeboxPosition;
    private int laCucarachaTimer = 0;
    public int timeUntilNextEgg = this.random.nextInt(24000) + 24000;

    public EntityCockroach(EntityType type, Level world) {
        super(type, world);
    }

    public static AttributeSupplier.Builder bakeAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 6.0D).add(Attributes.MOVEMENT_SPEED, 0.35F);
    }

    public static boolean isValidLightLevel(ServerLevelAccessor p_223323_0_, BlockPos p_223323_1_, RandomSource p_223323_2_) {
        if (p_223323_0_.getBrightness(LightLayer.SKY, p_223323_1_) > p_223323_2_.nextInt(32)) {
            return false;
        } else {
            int lvt_3_1_ = p_223323_0_.getLevel().isThundering() ? p_223323_0_.getMaxLocalRawBrightness(p_223323_1_, 10) : p_223323_0_.getMaxLocalRawBrightness(p_223323_1_);
            return lvt_3_1_ <= p_223323_2_.nextInt(8);
        }
    }

    public boolean checkSpawnRules(LevelAccessor worldIn, MobSpawnType spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.cockroachSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public static boolean canMonsterSpawnInLight(EntityType<? extends EntityCockroach> p_223325_0_, ServerLevelAccessor p_223325_1_, MobSpawnType p_223325_2_, BlockPos p_223325_3_, RandomSource p_223325_4_) {
        return isValidLightLevel(p_223325_1_, p_223325_3_, p_223325_4_) && checkMobSpawnRules(p_223325_0_, p_223325_1_, p_223325_2_, p_223325_3_, p_223325_4_);
    }

    public static <T extends Mob> boolean canCockroachSpawn(EntityType<EntityCockroach> entityType, ServerLevelAccessor iServerWorld, MobSpawnType reason, BlockPos pos, RandomSource random) {
        return reason == MobSpawnType.SPAWNER || !iServerWorld.canSeeSky(pos) && pos.getY() <= 64 && canMonsterSpawnInLight(entityType, iServerWorld, reason, pos, random);
    }

    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return !requiresCustomPersistence();
    }

    public boolean requiresCustomPersistence() {
        return super.requiresCustomPersistence() || this.hasCustomName() || this.isBreaded() || this.isDancing() || this.hasMaracas() || this.isHeadless();
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.COCKROACH_HURT.get();
    }

    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.COCKROACH_HURT.get();
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.1D));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.0D, AMCompat.ingredientOf(AMTagRegistry.COCKROACH_FOODSTUFFS), false));
        this.goalSelector.addGoal(4, new AvoidEntityGoal(this, EntityCentipedeHead.class, 16, 1.3D, 1.0D));
        this.goalSelector.addGoal(4, new AvoidEntityGoal(this, Player.class, 8, 1.3D, 1.0D) {
            public boolean canUse() {
                return !EntityCockroach.this.isBreaded() && super.canUse();
            }
        });
        this.goalSelector.addGoal(5, new AnimalAIFleeLight(this, 1.0D) {
            public boolean canUse() {
                return !EntityCockroach.this.isBreaded() && super.canUse();
            }
        });
        this.goalSelector.addGoal(6, new RandomStrollGoal(this, 1.0D, 80));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new CreatureAITargetItems(this, false));
    }

    public boolean hurt(DamageSource source, float amount) {
        boolean prev = super.hurt(source, amount);
        if(prev){
            randomWingFlapTick = 5 + random.nextInt(15);
            if (this.getHealth() <= 1.0F && amount > 0 && !this.isHeadless() && this.getRandom().nextInt(3) == 0) {
                this.setHeadless(true);
                if (!this.level().isClientSide()) {
                    final ServerLevel serverLevel = (ServerLevel) this.level();
                    for (int i = 0; i < 3; i++) {
                        serverLevel.sendParticles(ParticleTypes.SNEEZE, this.getRandomX(0.52F), this.getY(1D), this.getRandomZ(0.52F), 1, 0.0D, 0.0D, 0.0D, 0.0D);
                    }
                }
            }
        }
        return prev;
    }

    public boolean isFood(ItemStack stack) {
        return stack.is(AMTagRegistry.COCKROACH_BREEDABLES);
    }

    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("Maracas", this.hasMaracas());
        compound.putBoolean("Dancing", this.isDancing());
        compound.putBoolean("Breaded", this.isBreaded());
        compound.putInt("EggTime", this.timeUntilNextEgg);
    }

    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.setMaracas(AMCompat.getBoolean(compound, "Maracas"));
        this.setDancing(AMCompat.getBoolean(compound, "Dancing"));
        this.setBreaded(AMCompat.getBoolean(compound, "Breaded"));
        if (AMCompat.contains(compound, "EggTime")) {
            this.timeUntilNextEgg = AMCompat.getInt(compound, "EggTime");
        }
    }

    //? if <1.21.2
    @Nullable
    //? if (>=1.21.2 && !forge) {
/*// #104: 1.21.2 moved the default loot table onto EntityType and deleted getDefaultLootTable(),
    // but LivingEntity#dropFromLootTable still calls getLootTable() VIRTUALLY, so the per-variant
    // selection survives as an override of that instead. Mob#getLootTable() is `final` in vanilla
    // on every version; what makes the override legal here is the mod's own widening -- the
    // accesswidener on Fabric, the access transformer on NeoForge (MDG applies it to the
    // project-local compile artifacts). Neither reaches classic Forge, hence the arm below.
    // super.getLootTable() still returns the spawner-set table when there is one.
    @Override
    public java.util.Optional<net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable>> getLootTable() {
        return this.hasMaracas() ? this.isHeadless() ? AMCompat.lootOpt(MARACA_HEADLESS_LOOT) : AMCompat.lootOpt(MARACA_LOOT) : super.getLootTable();
    }
    *///?} elif >=1.21.2 {
    /*// Forge >=1.21.3, the one band that cannot have the override above: arch-loom applies the
    // mod's accesstransformer.cfg to neither the shared compile jar nor the running game (the
    // Forge manifest's `accessTransformers = [  ]` makes it inert there too), so
    // Mob#getLootTable() is still `final` and these mobs fall back to their entity type's default
    // table. Measured with javap on all 49 nodes' compile jars, not inferred from a sibling.
    *///?} elif >=1.20.5 {
    /*// 1.20.5 keys loot tables by ResourceKey instead of raw ResourceLocation.
    protected net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> getDefaultLootTable() {
        return this.hasMaracas() ? this.isHeadless() ? AMCompat.lootKey(MARACA_HEADLESS_LOOT) : AMCompat.lootKey(MARACA_LOOT) : super.getDefaultLootTable();
    }
    *///?} else {
    protected ResourceLocation getDefaultLootTable() {
        return this.hasMaracas() ? this.isHeadless() ? MARACA_HEADLESS_LOOT : MARACA_LOOT : super.getDefaultLootTable();
    }
    //?}

    public float getWalkTargetValue(BlockPos pos, LevelReader worldIn) {
        return 0.5F - Math.max(worldIn.getBrightness(LightLayer.BLOCK, pos), worldIn.getBrightness(LightLayer.SKY, pos));
    }

    //? if <1.20.5 {
    public MobType getMobType() {
        return MobType.ARTHROPOD;
    }
    //?}

    public EntityDimensions getDimensions(Pose poseIn) {
        return isDancing() ? STAND_SIZE.scale(this.getScale()) : super.getDimensions(poseIn);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.is(DamageTypes.FALL) || source.is(DamageTypes.DROWN) || source.is(DamageTypes.IN_WALL)  || source.is(DamageTypeTags.IS_EXPLOSION) || source.getMsgId().equals("anvil") || super.isInvulnerableTo(source);
    }

    public InteractionResult mobInteract(Player p_230254_1_, InteractionHand p_230254_2_) {
        ItemStack lvt_3_1_ = p_230254_1_.getItemInHand(p_230254_2_);
        // Fabric has no loader shears hook and vanilla has no generic one — see
        // AMCompat#shearWithShears. Ahead of the maraca branches, whose "held item is not a
        // maraca" arm would otherwise answer the shears by dropping the roach's maracas.
        //? if fabric {
        /*if (AMCompat.shearWithShears(this, p_230254_1_, p_230254_2_, lvt_3_1_)) {
            return AMCompat.sidedSuccess(this.level().isClientSide());
        }
        *///?}
       if (lvt_3_1_.getItem() == AMItemRegistry.MARACA.get() && this.isAlive() && !this.hasMaracas()) {
            this.setMaracas(true);
            lvt_3_1_.shrink(1);
            return AMCompat.sidedSuccess(this.level().isClientSide());
        } else if (lvt_3_1_.getItem() != AMItemRegistry.MARACA.get() && this.isAlive() && this.hasMaracas()) {
            this.setMaracas(false);
            this.setDancing(false);
            AMCompat.spawnAtLocation(this, new ItemStack(AMItemRegistry.MARACA.get()));
            return InteractionResult.SUCCESS;
        } else {
            return super.mobInteract(p_230254_1_, p_230254_2_);
        }
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
        builder.define(DANCING, false);
        builder.define(HEADLESS, false);
        builder.define(MARACAS, false);
        builder.define(NEAREST_MUSICIAN, Optional.empty());
        builder.define(BREADED, false);
    }

    public boolean isDancing() {
        return this.entityData.get(DANCING);
    }

    public void setDancing(boolean dancing) {
        this.entityData.set(DANCING, dancing);
    }

    public boolean isHeadless() {
        return this.entityData.get(HEADLESS);
    }

    public void setHeadless(boolean head) {
        this.entityData.set(HEADLESS, head);
    }

    public boolean hasMaracas() {
        return this.entityData.get(MARACAS);
    }

    public void setMaracas(boolean head) {
        this.entityData.set(MARACAS, head);
    }

    public boolean isBreaded() {
        return this.entityData.get(BREADED);
    }

    public void setBreaded(boolean breaded) {
        this.entityData.set(BREADED, breaded);
    }

    @Nullable
    public UUID getNearestMusicianId() {
        return this.entityData.get(NEAREST_MUSICIAN).orElse(null);
    }

    public void tick() {
        super.tick();
        prevDanceProgress = danceProgress;
        final boolean dance = this.isJukeboxing || isDancing();
        if (this.jukeboxPosition == null || !this.jukeboxPosition.closerToCenterThan(this.position(), 3.46D) || !this.level().getBlockState(this.jukeboxPosition).is(Blocks.JUKEBOX)) {
            this.isJukeboxing = false;
            this.jukeboxPosition = null;
        }
        if (this.getEyeHeight() > this.getBbHeight()) {
            this.refreshDimensions();
        }

        if (dance) {
            if (danceProgress < 5F)
                danceProgress++;
        } else {
            if (danceProgress > 0F)
                danceProgress--;
        }

        if (!this.onGround() || random.nextInt(200) == 0) {
            randomWingFlapTick = 5 + random.nextInt(15);
        }
        if (randomWingFlapTick > 0) {
            randomWingFlapTick--;
        }
        if (prevStand != dance) {
            if (hasMaracas()) {
                tellOthersImPlayingLaCucaracha();
            }
            this.refreshDimensions();
        }
        if (!hasMaracas()) {
            Entity musician = this.getNearestMusician();
            if (musician != null) {
                if (!musician.isAlive() || this.distanceTo(musician) > 10 || musician instanceof EntityCockroach && !((EntityCockroach) musician).hasMaracas()) {
                    this.setNearestMusician(null);
                    this.setDancing(false);
                } else {
                    this.setDancing(true);
                }
            }
        }
        if (hasMaracas()) {
            laCucarachaTimer++;
            if (laCucarachaTimer % 20 == 0 && random.nextFloat() < 0.3F) {
                tellOthersImPlayingLaCucaracha();
            }
            this.setDancing(true);
            if (!this.isSilent()) {
                this.level().broadcastEntityEvent(this, (byte) 67);
            }
        } else {
            laCucarachaTimer = 0;
        }
        if (!this.level().isClientSide() && this.isAlive() && !this.isBaby() && --this.timeUntilNextEgg <= 0) {
           ItemEntity dropped = AMCompat.spawnAtLocation(this, AMItemRegistry.COCKROACH_OOTHECA.get());
           if(dropped != null){
               dropped.setDefaultPickUpDelay();
           }
            this.timeUntilNextEgg = this.random.nextInt(24000) + 24000;

        }
        prevStand = dance;
    }

    private void tellOthersImPlayingLaCucaracha() {
        List<EntityCockroach> list = this.level().getEntitiesOfClass(EntityCockroach.class, this.getMusicianDistance(), EntitySelector.NO_SPECTATORS);
        for (EntityCockroach roach : list) {
            if (!roach.hasMaracas()) {
                roach.setNearestMusician(this.getUUID());
            }
        }
    }

    private AABB getMusicianDistance() {
        return this.getBoundingBox().inflate(10, 10, 10);
    }

    @OnlyIn(value = Dist.CLIENT)
    public void handleEntityEvent(byte id) {
        if (id == 67) {
            AlexsMobs.PROXY.onEntityStatus(this, id);
        } else {
            super.handleEntityEvent(id);
        }
    }

    public Entity getNearestMusician() {
        final UUID id = getNearestMusicianId();
        if (id != null && !this.level().isClientSide()) {
            return ((ServerLevel) level()).getEntity(id);
        }
        return null;
    }

    public void setNearestMusician(@Nullable UUID uniqueId) {
        this.entityData.set(NEAREST_MUSICIAN, Optional.ofNullable(uniqueId));
    }

    @OnlyIn(value = Dist.CLIENT)
    public void setRecordPlayingNearby(BlockPos pos, boolean isPartying) {
        this.jukeboxPosition = pos;
        this.isJukeboxing = isPartying;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel serverWorld, AgeableMob ageableEntity) {
        final EntityCockroach roach = AMCompat.create(AMEntityRegistry.COCKROACH.get(), serverWorld);
        roach.setBreaded(true);
        return roach;
    }

    public boolean readyForShearing() {
        return this.isAlive() && !this.isBaby() && !isHeadless();
    }

    // Forge 65 (MC 26.2) deleted IForgeShearable — there, vanilla Shearable is the whole
    // contract, because Forge's patched shears item dispatches on it. So this and onSheared
    // below stop being overrides and nothing calls them; they are kept rather than deleted so
    // no other node is disturbed, and the shearing behaviour itself lives in shear(...), which
    // every node uses.
    // ⚠️ FABRIC takes this same arm and has NO such patch. Vanilla has never had a generic
    // shears-on-a-mob hook on any version in this range — it shears its own mobs from inside
    // each one's mobInteract — so Fabric needs that call site too; see the mobInteract above
    // and AMCompat#shearWithShears. This comment used to claim the shears item reached
    // readyForShearing/shear by itself, which is why none of the four mobs implementing
    // Shearable here could be sheared on any Fabric node from 2.0.0 to 2.1.1.
    //? if neoforge || (forge && <26.2)
    @Override
    // NeoForge 20.6 reshaped IShearable: isShearable took a player argument and onSheared
    // dropped the (unused here) fortune level.
    //? if neoforge && >=1.20.6
    //public boolean isShearable(@javax.annotation.Nullable Player player, @javax.annotation.Nonnull ItemStack item, Level world, BlockPos pos) {
    //? if forge || fabric || <1.20.6
    public boolean isShearable(@javax.annotation.Nonnull ItemStack item, Level world, BlockPos pos) {
        return readyForShearing();
    }

    @Override
    public void shear(SoundSource category) {
        this.hurt(damageSources().generic(), 0F);
        level().playSound(null, this, SoundEvents.SHEEP_SHEAR, category, 1.0F, 1.0F);
        this.gameEvent(GameEvent.ENTITY_INTERACT);
        this.setHeadless(true);
    }

    @javax.annotation.Nonnull
    //? if neoforge || (forge && <26.2)
    @Override
    //? if neoforge && >=1.20.6
    //public java.util.List<ItemStack> onSheared(@javax.annotation.Nullable Player player, @javax.annotation.Nonnull ItemStack item, Level world, BlockPos pos) {
    //? if forge || fabric || <1.20.6
    public java.util.List<ItemStack> onSheared(@javax.annotation.Nullable Player player, @javax.annotation.Nonnull ItemStack item, Level world, BlockPos pos, int fortune) {
        world.playSound(null, this, SoundEvents.SHEEP_SHEAR, player == null ? SoundSource.BLOCKS : SoundSource.PLAYERS, 1.0F, 1.0F);
        this.gameEvent(GameEvent.ENTITY_INTERACT);
        this.hurt(damageSources().generic(), 0F);
        if (!world.isClientSide()) {
            for (int i = 0; i < 3; i++) {
                ((ServerLevel) this.level()).sendParticles(ParticleTypes.SNEEZE, this.getRandomX(0.52F), this.getY(1D), this.getRandomZ(0.52F), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
        this.setHeadless(true);
        return java.util.Collections.emptyList();
    }

    @Override
    public boolean canTargetItem(ItemStack stack) {
        return AMCompat.isEdible(stack.getItem()) || stack.is(AMTagRegistry.COCKROACH_BREEDABLES);
    }

    public void travel(Vec3 vec3d) {
        if (this.isDancing() || danceProgress > 0) {
            if (this.getNavigation().getPath() != null) {
                this.getNavigation().stop();
            }
            vec3d = Vec3.ZERO;
        }
        super.travel(vec3d);
    }


    @Override
    public void onGetItem(ItemEntity e) {
        if (e.getItem().getItem() == AMItemRegistry.MARACA.get()) {
            this.setMaracas(true);
        } else {
            if (AMCompat.hasCraftingRemainder(e.getItem())) {
                AMCompat.spawnAtLocation(this, AMCompat.craftingRemainder(e.getItem()).copy());
            }
            this.heal(5);
            if (e.getItem().is(AMTagRegistry.COCKROACH_FOODSTUFFS)) {
                this.setBreaded(true);
            }
        }
    }
}

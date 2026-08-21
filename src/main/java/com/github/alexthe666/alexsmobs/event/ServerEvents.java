package com.github.alexthe666.alexsmobs.event;

import com.github.alexthe666.alexsmobs.misc.AMCompat;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.block.AMBlockRegistry;
import com.github.alexthe666.alexsmobs.client.particle.AMParticleRegistry;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.effect.AMEffectRegistry;
import com.github.alexthe666.alexsmobs.effect.EffectClinging;
import com.github.alexthe666.alexsmobs.entity.*;
import com.github.alexthe666.alexsmobs.entity.util.FlyingFishBootsUtil;
import com.github.alexthe666.alexsmobs.entity.util.RainbowUtil;
import com.github.alexthe666.alexsmobs.entity.util.RockyChestplateUtil;
import com.github.alexthe666.alexsmobs.entity.util.VineLassoUtil;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.github.alexthe666.alexsmobs.item.ILeftClick;
import com.github.alexthe666.alexsmobs.item.ItemGhostlyPickaxe;
import com.github.alexthe666.alexsmobs.message.MessageSwingArm;
import com.github.alexthe666.alexsmobs.misc.AMAdvancementTriggerRegistry;
import com.github.alexthe666.alexsmobs.misc.AMTagRegistry;
import com.github.alexthe666.alexsmobs.misc.EmeraldsForItemsTrade;
import com.github.alexthe666.alexsmobs.misc.ItemsForEmeraldsTrade;
import com.github.alexthe666.alexsmobs.world.AMWorldData;
import com.github.alexthe666.alexsmobs.world.BeachedCachalotWhaleSpawner;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NonTameRandomTargetGoal;
import net.minecraft.world.entity.animal.*;
// Explicit single-type imports for the six vanilla animals used below. They are already
// covered by the wildcard above on every node — they exist ONLY so the !mc2111-pkg-* rules
// have a line to rewrite: 1.21.11 scattered these classes into per-mob sub-packages, and a
// wildcard import is not something a replacement rule can follow.
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.animal.Rabbit;
//? if >=1.21.5
/*import net.minecraft.world.entity.animal.wolf.Wolf;*/
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Spider;
//? if <26
import net.minecraft.world.entity.npc.VillagerProfession;
//? if <26
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.*;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
// NeoForge 1.21.4 renamed this to AddServerReloadListenersEvent (used fully-qualified below);
// Forge keeps AddReloadListenerEvent on every version. Every `forge || …` gate in this file needs
// a `fabric` term too: the compat stubs in fabric/forge/ are modelled on the FORGE shapes, so
// Fabric always wants the Forge arm, never the NeoForge one it would otherwise fall into.
//? if forge || fabric || <1.21.4
import net.minecraftforge.event.AddReloadListenerEvent;
// NeoForge 20.6 dissolved TickEvent into net.neoforged.neoforge.event.tick.* and dropped
// Event.Result from the bus.
//? if forge || fabric || <1.20.6
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityStruckByLightningEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
//? if <26
import net.minecraftforge.event.village.VillagerTradesEvent;
//? if <26
import net.minecraftforge.event.village.WandererTradesEvent;
// Forge's EventBus 7 (1.21.6) has no api.Event at all — the sole use of this import is
// Event.Result.DENY, which the !fg2106-eb-result rule fully-qualifies to common.util.Result.
//? if forge && >=1.21.6 {
/*
*///?} elif forge || fabric || <1.20.6 {
import net.minecraftforge.eventbus.api.Event;
//?}
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
// Import-only — nothing in this file is annotated @Mod, and Fabric has no such annotation.
//? if !fabric
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemHandlerHelper;
// antlr4 left the Minecraft classpath in 1.20.5; commons-lang3 has an equivalent 3-tuple.
import org.apache.commons.lang3.tuple.Triple;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

// Upstream registered this class twice: the annotation picked up the static handlers, and
// AlexsMobs registered an instance for the rest. NeoForge's bus rejects a class that mixes the
// two, so every handler is an instance method now and the instance registration is the only path.
public class ServerEvents {

    public static final UUID ALEX_UUID = UUID.fromString("71363abe-fd03-49c9-940d-aae8b8209b7c");
    public static final UUID CARRO_UUID = UUID.fromString("98905d4a-1cbc-41a4-9ded-2300404e2290");
    //? if >=1.21 {
    /*private static final net.minecraft.resources.ResourceLocation SAND_SPEED_MODIFIER = AMCompat.rl("alexsmobs", "roadrunner_speed");
    private static final net.minecraft.resources.ResourceLocation SNEAK_SPEED_MODIFIER = AMCompat.rl("alexsmobs", "frontier_cap_speed");
    *///?} else {
    private static final UUID SAND_SPEED_MODIFIER = UUID.fromString("7E0292F2-9434-48D5-A29F-9583AF7DF28E");
    private static final UUID SNEAK_SPEED_MODIFIER = UUID.fromString("7E0292F2-9434-48D5-A29F-9583AF7DF28F");
    //?}
    private static final AttributeModifier SAND_SPEED_BONUS = AMCompat.attributeModifier(SAND_SPEED_MODIFIER, "roadrunner speed bonus", 0.1F, AttributeModifier.Operation.ADDITION);
    private static final AttributeModifier SNEAK_SPEED_BONUS = AMCompat.attributeModifier(SNEAK_SPEED_MODIFIER, "frontier cap speed bonus", 0.1F, AttributeModifier.Operation.ADDITION);
    private static final Map<ServerLevel, BeachedCachalotWhaleSpawner> BEACHED_CACHALOT_WHALE_SPAWNER_MAP = new HashMap<>();

    // 1.20.5 replaced the static BrewingRecipeRegistry with a per-server PotionBrewing.Builder
    // handed out by a game-bus event. Forge and NeoForge name that event differently, but both
    // expose getBuilder(), so the body is identical.
    //? if forge && >=1.20.5 {
    /*@SubscribeEvent
    public void onRegisterBrewingRecipes(net.minecraftforge.event.brewing.BrewingRecipeRegisterEvent event) {
        AMEffectRegistry.registerBrewingRecipes(event.getBuilder());
    }
    *///?}
    //? if neoforge && >=1.20.5 {
    /*@SubscribeEvent
    public void onRegisterBrewingRecipes(net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent event) {
        AMEffectRegistry.registerBrewingRecipes(event.getBuilder());
    }
    *///?}

    // NeoForge 20.6 replaced the phase-tagged TickEvent with separate Pre/Post classes. Forge's
    // LevelTickEvent fires in both phases and this listener never filtered on phase, so both
    // NeoForge classes are subscribed to keep the tick rate identical.
    //? if neoforge && >=1.20.6 {
    /*@SubscribeEvent
    public void onServerTickPre(net.neoforged.neoforge.event.tick.LevelTickEvent.Pre tick) {
        onLevelTick(tick.getLevel());
    }

    @SubscribeEvent
    public void onServerTickPost(net.neoforged.neoforge.event.tick.LevelTickEvent.Post tick) {
        onLevelTick(tick.getLevel());
    }
    *///?} elif forge && >=1.21.9 {
    /*// Forge 1.21.9 split its own LevelTickEvent into Pre/Post records too. The parent interface
    // has no bus of its own, so — exactly as on NeoForge — both halves are subscribed, which is
    // what the single phase-agnostic listener below used to be.
    @SubscribeEvent
    public void onServerTickPre(TickEvent.LevelTickEvent.Pre tick) {
        onLevelTick(tick.level());
    }

    @SubscribeEvent
    public void onServerTickPost(TickEvent.LevelTickEvent.Post tick) {
        onLevelTick(tick.level());
    }
    *///?} else {
    @SubscribeEvent
    public void onServerTick(TickEvent.LevelTickEvent tick) {
        onLevelTick(tick.level);
    }
    //?}

    private void onLevelTick(Level level) {
        if (!level.isClientSide() && level instanceof ServerLevel serverWorld) {
            BEACHED_CACHALOT_WHALE_SPAWNER_MAP.computeIfAbsent(serverWorld,
                k -> new BeachedCachalotWhaleSpawner(serverWorld));
            BeachedCachalotWhaleSpawner spawner = BEACHED_CACHALOT_WHALE_SPAWNER_MAP.get(serverWorld);
            spawner.tick();

            if (!com.github.alexthe666.alexsmobs.misc.AMTeleportQueue.PLAYERS.isEmpty()) {
                for (final var triple : com.github.alexthe666.alexsmobs.misc.AMTeleportQueue.PLAYERS) {
                    ServerPlayer player = triple.getLeft();
                    ServerLevel endpointWorld = triple.getMiddle();
                    BlockPos endpoint = triple.getRight();
                    final int heightFromMap = endpointWorld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, endpoint.getX(), endpoint.getZ());
                    endpoint = new BlockPos(endpoint.getX(), Math.max(heightFromMap, endpoint.getY()), endpoint.getZ());
                    //? if >=1.21.2 {
                    /*player.teleportTo(endpointWorld, endpoint.getX() + 0.5D, endpoint.getY() + 0.5D, endpoint.getZ() + 0.5D, java.util.Set.of(), player.getYRot(), player.getXRot(), false);
                    *///?} else {
                    player.teleportTo(endpointWorld, endpoint.getX() + 0.5D, endpoint.getY() + 0.5D, endpoint.getZ() + 0.5D, player.getYRot(), player.getXRot());
                    //?}
                    ChunkPos chunkpos = new ChunkPos(endpoint);
                    //? if >=1.21.5 {
                    /*// 1.21.5 rewrote the ticket system: addRegionTicket is gone; addTicketWithRadius
                    // is the timed-ticket equivalent (TicketType.UNKNOWN keeps the chunk loaded briefly).
                    endpointWorld.getChunkSource().addTicketWithRadius(TicketType.UNKNOWN, chunkpos, 1);
                    *///?} elif >=1.21.4 {
                    /*// 1.21.4 removed TicketType.POST_TELEPORT (a TicketType<Integer>); UNKNOWN is the
                    // generic timed TicketType<ChunkPos> that keeps the destination chunk loaded briefly.
                    endpointWorld.getChunkSource().addRegionTicket(TicketType.UNKNOWN, chunkpos, 1, chunkpos);
                    *///?} else {
                    endpointWorld.getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, chunkpos, 1, player.getId());
                    //?}
                    player.connection.send(new ClientboundSetExperiencePacket(player.experienceProgress, player.totalExperience, player.experienceLevel));

                }
                com.github.alexthe666.alexsmobs.misc.AMTeleportQueue.PLAYERS.clear();
            }
        }
        AMWorldData data = AMWorldData.get(level);
        if (data != null) {
            data.tickPupfish();
        }
    }

    protected static BlockHitResult rayTrace(Level worldIn, Player player, ClipContext.Fluid fluidMode) {
        final float x = player.getXRot();
        final float y = player.getYRot();
        Vec3 vector3d = player.getEyePosition(1.0F);
        final float f0 = -y * Mth.DEG_TO_RAD - Mth.PI;
        final float f1 = -x * Mth.DEG_TO_RAD;
        final float f2 = Mth.cos(f0);
        final float f3 = Mth.sin(f0);
        final float f4 = -Mth.cos(f1);
        final float f5 = Mth.sin(f1);
        final float f6 = f3 * f4;
        final float f7 = f2 * f4;
        // Upstream reads the block-reach attribute unguarded, which is safe on Forge/NeoForge (the
        // loader adds ForgeMod.BLOCK_REACH to every player) but NOT on Fabric below 1.20.5, where
        // vanilla has no such attribute at all and AMPlatform.blockReach() returns null — see the
        // comment on its Fabric arms. Both nulls have to be handled: the attribute may not exist,
        // and even where it does the player may not carry an instance of it.
        // 5.0 is what vanilla itself uses on those versions — Item#getPlayerPOVHitResult hardcodes
        // it (verified in 1.20.1 and 1.20.4 bytecode), which is exactly the call this method is a
        // copy of, so the fallback reproduces vanilla glass-bottle reach rather than guessing.
        final var reachAttribute = com.github.alexthe666.alexsmobs.misc.AMPlatform.blockReach();
        final var reachInstance = reachAttribute == null ? null : player.getAttribute(reachAttribute);
        final double d0 = reachInstance == null ? 5.0D : reachInstance.getValue();
        Vec3 vector3d1 = vector3d.add(f6 * d0, f5 * d0, f7 * d0);
        return worldIn.clip(new ClipContext(vector3d, vector3d1, ClipContext.Block.OUTLINE, fluidMode, player));
    }


    private static final Random RAND = new Random();

    @SubscribeEvent
    public void onItemUseLast(LivingEntityUseItemEvent.Finish event) {
        if (event.getItem().getItem() == Items.CHORUS_FRUIT && RAND.nextInt(3) == 0
            && event.getEntity().hasEffect(AMCompat.effect(AMEffectRegistry.ENDER_FLU.get()))) {
            event.getEntity().removeEffect(AMCompat.effect(AMEffectRegistry.ENDER_FLU.get()));
        }
    }

    // Forge deleted EntityEvent.Size in 1.20.2 — eye height became part of EntityDimensions and
    // there is no per-tick hook to bend it, so the Clinging upside-down camera drop is 1.20.1-only.
    //? if <1.20.2 {
    @SubscribeEvent
    public void onEntityResize(EntityEvent.Size event) {
        if (event.getEntity() instanceof Player entity) {
            final var potions = entity.getActiveEffectsMap();
            if (event.getEntity().level() != null && potions != null && !potions.isEmpty()
                && potions.containsKey(AMEffectRegistry.CLINGING)) {
                if (EffectClinging.isUpsideDown(entity)) {
                    float minus = event.getOldSize().height - event.getOldEyeHeight();
                    event.setNewEyeHeight(minus);
                }
            }
        }

    }
    //?}

    // 1.20.5 dropped IForgeItem#canBeHurtBy, which AMBlockItem used to keep a dropped
    // transmutation table from being destroyed by explosions. Take it out of the blast's
    // affected-entity list instead — same net effect, one node-conditional handler.
    //? if >=1.20.5 {
    /*@SubscribeEvent
    public void onExplosionDetonate(net.minecraftforge.event.level.ExplosionEvent.Detonate event) {
        event.getAffectedEntities().removeIf(affected -> affected instanceof ItemEntity itemEntity
                && itemEntity.getItem().is(AMBlockRegistry.TRANSMUTATION_TABLE.get().asItem()));
    }
    *///?}

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (AMConfig.giveBookOnStartup) {
            // Forge patches getPersistentData onto Entity; Fabric has no such patch, so AMCompat
            // routes it to the vendored Citadel tag instead. Gated rather than always-AMCompat so
            // the two working loaders keep calling the platform method directly.
            //? if fabric {
            /*CompoundTag playerData = AMCompat.getPersistentData(event.getEntity());
            *///?} else {
            CompoundTag playerData = event.getEntity().getPersistentData();
            //?}
            // AMCompat.PERSISTED_NBT_TAG is the platform constant on Forge/NeoForge and Forge's
            // literal value on Fabric — one spelling for all three, see AMCompat.
            CompoundTag data = AMCompat.getCompound(playerData, AMCompat.PERSISTED_NBT_TAG);
            if (data != null && !AMCompat.getBoolean(data, "alexsmobs_has_book")) {
                ItemHandlerHelper.giveItemToPlayer(event.getEntity(), new ItemStack(AMItemRegistry.ANIMAL_DICTIONARY.get()));
                final boolean isAlex = Objects.equals(event.getEntity().getUUID(), ALEX_UUID);
                if (isAlex || Objects.equals(event.getEntity().getUUID(), CARRO_UUID)) {
                    ItemHandlerHelper.giveItemToPlayer(event.getEntity(), new ItemStack(AMItemRegistry.BEAR_DUST.get()));
                }
                if (isAlex) {
                    ItemHandlerHelper.giveItemToPlayer(event.getEntity(), new ItemStack(AMItemRegistry.NOVELTY_HAT.get()));
                }
                data.putBoolean("alexsmobs_has_book", true);
                playerData.put(AMCompat.PERSISTED_NBT_TAG, data);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLeftClick(PlayerInteractEvent.LeftClickEmpty event) {
        boolean flag = false;
        ItemStack leftItem = event.getEntity().getOffhandItem();
        ItemStack rightItem = event.getEntity().getMainHandItem();
        if(leftItem.getItem() instanceof final ILeftClick iLeftClick){
            iLeftClick.onLeftClick(leftItem, event.getEntity());
            flag = true;
        }
        if(rightItem.getItem() instanceof final ILeftClick iLeftClick){
            iLeftClick.onLeftClick(rightItem, event.getEntity());
            flag = true;
        }
        if (flag && event.getLevel().isClientSide()) {
            AlexsMobs.sendMSGToServer(MessageSwingArm.INSTANCE);
        }
    }

    // EventBus 7 (Forge 1.21.6) cancels by RETURNING true, and this handler keeps working after it
    // cancels (it converts the squid), so the body is wrapped rather than rewritten. Same shape for
    // every cancelling handler below.
    @SubscribeEvent
    //? if forge && >=1.21.6 {
    /*public boolean onStruckByLightning(EntityStruckByLightningEvent event) {
        return AMCompat.cancelIf(() -> onStruckByLightning0(event));
    }

    private void onStruckByLightning0(EntityStruckByLightningEvent event) {
    *///?} else {
    public void onStruckByLightning(EntityStruckByLightningEvent event) {
    //?}
        if (event.getEntity().getType() == EntityType.SQUID && !event.getEntity().level().isClientSide()) {
            ServerLevel level = (ServerLevel) event.getEntity().level();
            //? if forge && >=1.21.6 {
            /*AMCompat.cancelEvent();
            *///?} else {
            event.setCanceled(true);
            //?}
            EntityGiantSquid squid = AMCompat.create(AMEntityRegistry.GIANT_SQUID.get(), level);
            squid.moveTo(event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), event.getEntity().getYRot(), event.getEntity().getXRot());
            //? if >=1.20.5 {
            /*squid.finalizeSpawn(level, level.getCurrentDifficultyAt(squid.blockPosition()), MobSpawnType.CONVERSION, null);
            *///?} else {
            squid.finalizeSpawn(level, level.getCurrentDifficultyAt(squid.blockPosition()), MobSpawnType.CONVERSION, null, null);
            //?}
            if (event.getEntity().hasCustomName()) {
                squid.setCustomName(event.getEntity().getCustomName());
                squid.setCustomNameVisible(event.getEntity().isCustomNameVisible());
            }
            squid.setBlue(true);
            squid.setPersistenceRequired();
            level.addFreshEntityWithPassengers(squid);
            event.getEntity().discard();
        }
    }

    // Forge 1.21 deleted IForgeItem#damageItem, which is where the ghostly pickaxe used to spill
    // its inventory as it broke. The destroy event covers the same case: damageItem only ever ran
    // for a ServerPlayer on that branch anyway.
    //? if forge && >=1.21 {
    /*@SubscribeEvent
    public void onDestroyItem(net.minecraftforge.event.entity.player.PlayerDestroyItemEvent event) {
        ItemStack original = event.getOriginal();
        if (original.getItem() instanceof ItemGhostlyPickaxe pickaxe) {
            pickaxe.dropAllContents(event.getEntity().level(), event.getEntity().position(), original);
        }
    }
    *///?}

    @SubscribeEvent
    public void onProjectileHit(ProjectileImpactEvent event) {
        if (event.getRayTraceResult() instanceof EntityHitResult hitResult
            && hitResult.getEntity() instanceof EntityEmu emu && !event.getEntity().level().isClientSide()) {
            if (event.getEntity() instanceof AbstractArrow arrow) {
                //fixes soft crash with vanilla
                arrow.setPierceLevel((byte) 0);
            }
            if ((emu.getAnimation() == EntityEmu.ANIMATION_DODGE_RIGHT || emu.getAnimation() == EntityEmu.ANIMATION_DODGE_LEFT) && emu.getAnimationTick() < 7) {
                // Forge's ProjectileImpactEvent stopped being @Cancelable at **1.20.4** (verified by
                // javap on every node's merged jar: Cancelable is present only on forge 1.20.1) — it
                // grew a richer ImpactResult instead, and SKIP_ENTITY is what the old cancel meant
                // here: the projectile ignores the dodging emu and keeps flying. Calling
                // setCanceled on it COMPILES and throws UnsupportedOperationException at runtime,
                // crashing the server ("Ticking entity") the first time an emu is shot at — this
                // gate said `forge && >=1.21.6` and shipped that crash on eight Forge nodes.
                // NeoForge still implements ICancellableEvent on every node, so it keeps setCanceled.
                //? if forge && >=1.20.4 {
                /*event.setImpactResult(net.minecraftforge.event.entity.ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);
                *///?} else {
                event.setCanceled(true);
                //?}
            }
            if (emu.getAnimation() != EntityEmu.ANIMATION_DODGE_RIGHT && emu.getAnimation() != EntityEmu.ANIMATION_DODGE_LEFT) {
                boolean left = true;
                Vec3 arrowPos = event.getEntity().position();
                Vec3 rightVector = emu.getLookAngle().yRot(0.5F * Mth.PI).add(emu.position());
                Vec3 leftVector = emu.getLookAngle().yRot(-0.5F * Mth.PI).add(emu.position());
                if (arrowPos.distanceTo(rightVector) < arrowPos.distanceTo(leftVector)) {
                    left = false;
                } else if (arrowPos.distanceTo(rightVector) > arrowPos.distanceTo(leftVector)) {
                    left = true;
                } else {
                    left = emu.getRandom().nextBoolean();
                }
                Vec3 vector3d2 = event.getEntity().getDeltaMovement().yRot((float) ((left ? -0.5F : 0.5F) * Math.PI)).normalize();
                emu.setAnimation(left ? EntityEmu.ANIMATION_DODGE_LEFT : EntityEmu.ANIMATION_DODGE_RIGHT);
                emu.hasImpulse = true;
                if (!emu.horizontalCollision) {
                    emu.move(MoverType.SELF, new Vec3(vector3d2.x() * 0.25F, 0.1F, vector3d2.z() * 0.25F));
                }
                if (!event.getEntity().level().isClientSide()) {
                    if (event.getEntity() instanceof Projectile projectile) {
                        if (projectile.getOwner() instanceof ServerPlayer serverPlayer) {
                            AMAdvancementTriggerRegistry.EMU_DODGE.trigger(serverPlayer);
                        }
                    }
                }
                emu.setDeltaMovement(emu.getDeltaMovement().add(vector3d2.x() * 0.5F, 0.32F, vector3d2.z() * 0.5F));
                //? if forge && >=1.20.4 {
                /*event.setImpactResult(net.minecraftforge.event.entity.ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);
                *///?} else {
                event.setCanceled(true);
                //?}
            }
        }
    }

    // NeoForge 20.6 promoted MobSpawnEvent.AllowDespawn to a top-level MobDespawnEvent carrying
    // its own Result enum instead of the bus one.
    //? if neoforge && >=1.20.6 {
    /*@SubscribeEvent
    public void onEntityDespawnAttempt(net.neoforged.neoforge.event.entity.living.MobDespawnEvent event) {
        if (event.getEntity().hasEffect(AMCompat.effect(AMEffectRegistry.DEBILITATING_STING.get())) && event.getEntity().getEffect(AMCompat.effect(AMEffectRegistry.DEBILITATING_STING.get())) != null && event.getEntity().getEffect(AMCompat.effect(AMEffectRegistry.DEBILITATING_STING.get())).getAmplifier() > 0) {
            event.setResult(net.neoforged.neoforge.event.entity.living.MobDespawnEvent.Result.DENY);
        }
    }
    *///?} else {
    @SubscribeEvent
    public void onEntityDespawnAttempt(MobSpawnEvent.AllowDespawn event) {
        if (event.getEntity().hasEffect(AMCompat.effect(AMEffectRegistry.DEBILITATING_STING.get())) && event.getEntity().getEffect(AMCompat.effect(AMEffectRegistry.DEBILITATING_STING.get())) != null && event.getEntity().getEffect(AMCompat.effect(AMEffectRegistry.DEBILITATING_STING.get())).getAmplifier() > 0) {
            event.setResult(Event.Result.DENY);
        }
    }
    //?}

    // 26.1 turned villager trades into datapack-driven registry entries: VillagerTrades.ItemListing
    // is gone, and with it both Forge/NeoForge trade events. The fisherman's ambergris trade and the
    // wandering trader's whole offer list are DROPPED on >=26 — restoring them means authoring trade
    // JSON against the new registry, which is a behaviour port, not a signature fix.
    //? if <26 {
    @SubscribeEvent
    public void onTradeSetup(VillagerTradesEvent event) {
        if (event.getType() == VillagerProfession.FISHERMAN) {
            VillagerTrades.ItemListing ambergrisTrade = new EmeraldsForItemsTrade(AMItemRegistry.AMBERGRIS.get(), 20, 3, 4);
            final var list = event.getTrades().get(2);
            list.add(ambergrisTrade);
            event.getTrades().put(2, list);
        }
    }
    //?}

    // Forge 1.21.5 dropped getGenericTrades()/getRareTrades() for a getPools() list (each Pool
    // carries rolls + entries); NeoForge KEPT the old getters. Split out of the handler so the
    // handler body carries no Stonecutter block of its own and can be gated whole on >=26 —
    // blocks are siblings, never nested.
    //? if forge && >=1.21.5 && <26 {
    /*private static List<VillagerTrades.ItemListing> amGenericPool(WandererTradesEvent event) {
        return event.getPools().stream().max(java.util.Comparator.comparingInt(net.minecraftforge.event.village.WandererTradesEvent.Pool::getRolls)).get().getEntries();
    }

    private static List<VillagerTrades.ItemListing> amRarePool(WandererTradesEvent event) {
        return event.getPools().stream().min(java.util.Comparator.comparingInt(net.minecraftforge.event.village.WandererTradesEvent.Pool::getRolls)).get().getEntries();
    }
    *///?} elif <26 {
    private static List<VillagerTrades.ItemListing> amGenericPool(WandererTradesEvent event) {
        return event.getGenericTrades();
    }

    private static List<VillagerTrades.ItemListing> amRarePool(WandererTradesEvent event) {
        return event.getRareTrades();
    }
    //?}

    //? if <26 {
    @SubscribeEvent
    public void onWanderingTradeSetup(WandererTradesEvent event) {
        if (AMConfig.wanderingTraderOffers) {
            List<VillagerTrades.ItemListing> genericTrades = amGenericPool(event);
            List<VillagerTrades.ItemListing> rareTrades = amRarePool(event);
            genericTrades.add(new ItemsForEmeraldsTrade(AMItemRegistry.ANIMAL_DICTIONARY.get(), 4, 1, 2, 1));
            genericTrades.add(new ItemsForEmeraldsTrade(AMItemRegistry.ACACIA_BLOSSOM.get(), 3, 2, 2, 1));
            if (AMConfig.cockroachSpawnWeight > 0) {
                genericTrades.add(new ItemsForEmeraldsTrade(AMItemRegistry.COCKROACH_OOTHECA.get(), 2, 1, 2, 1));
            }
            if (AMConfig.blobfishSpawnWeight > 0) {
                genericTrades.add(new ItemsForEmeraldsTrade(AMItemRegistry.BLOBFISH_BUCKET.get(), 4, 1, 3, 1));
            }
            if (AMConfig.crocodileSpawnWeight > 0) {
                genericTrades.add(new ItemsForEmeraldsTrade(AMBlockRegistry.CROCODILE_EGG.get().asItem(), 6, 1, 2, 1));
            }
            genericTrades.add(new ItemsForEmeraldsTrade(AMItemRegistry.BEAR_FUR.get(), 1, 1, 2, 1));
            genericTrades.add(new ItemsForEmeraldsTrade(AMItemRegistry.CROCODILE_SCUTE.get(), 5, 1, 2, 1));
            genericTrades.add(new ItemsForEmeraldsTrade(AMItemRegistry.ROADRUNNER_FEATHER.get(), 1, 2, 2, 2));
            genericTrades.add(new ItemsForEmeraldsTrade(AMItemRegistry.MOSQUITO_LARVA.get(), 1, 3, 5, 1));
            rareTrades.add(new ItemsForEmeraldsTrade(AMItemRegistry.SOMBRERO.get(), 20, 1, 1, 1));
            rareTrades.add(new ItemsForEmeraldsTrade(AMBlockRegistry.BANANA_PEEL.get(), 1, 2, 1, 1));
            rareTrades.add(new ItemsForEmeraldsTrade(AMItemRegistry.BLOOD_SAC.get(), 5, 2, 3, 1));
        }
    }
    //?}

    // The snow leopard's "+2 looting on its own kills" bonus. NeoForge 1.21 deleted
    // LootingLevelEvent along with the integer looting level itself — looting became an
    // enchantment *effect component*, and there is no event that can bump it for one killer.
    // Reproducing it would mean a datapack enchantment applied to the leopard, which is a
    // behaviour change; the bonus is simply dropped on that loader instead.
    //? if forge || <1.21 {
    @SubscribeEvent
    public void onLootLevelEvent(LootingLevelEvent event) {
        DamageSource src = event.getDamageSource();
        if (src != null) {
            if (src.getEntity() instanceof EntitySnowLeopard) {
                event.setLootingLevel(event.getLootingLevel() + 2);
            }
        }

    }
    //?}

    @SubscribeEvent
    public void onUseItem(PlayerInteractEvent.RightClickItem event) {
        final var player = event.getEntity();
        if (event.getItemStack().getItem() == Items.WHEAT && player.getVehicle() instanceof EntityElephant elephant) {
            if (elephant.triggerCharge(event.getItemStack())) {
                player.swing(event.getHand());
                if (!player.isCreative()) {
                    event.getItemStack().shrink(1);
                }
            }
        }
        if (event.getItemStack().getItem() == Items.GLASS_BOTTLE && AMConfig.lavaBottleEnabled) {
            HitResult raytraceresult = rayTrace(event.getLevel(), player, ClipContext.Fluid.SOURCE_ONLY);
            if (raytraceresult.getType() == HitResult.Type.BLOCK) {
                BlockPos blockpos = ((BlockHitResult) raytraceresult).getBlockPos();
                if (event.getLevel().mayInteract(player, blockpos)) {
                    if (event.getLevel().getFluidState(blockpos).is(FluidTags.LAVA)) {
                        player.gameEvent(GameEvent.ITEM_INTERACT_START);
                        event.getLevel().playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1.0F, 1.0F);
                        player.awardStat(Stats.ITEM_USED.get(Items.GLASS_BOTTLE));
                        player.setSecondsOnFire(6);
                        if (!player.addItem(new ItemStack(AMItemRegistry.LAVA_BOTTLE.get()))) {
                            AMCompat.spawnAtLocation(player, new ItemStack(AMItemRegistry.LAVA_BOTTLE.get()));
                        }
                        player.swing(event.getHand());
                        if (!player.isCreative()) {
                            event.getItemStack().shrink(1);
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    //? if forge && >=1.21.6 {
    /*public boolean onInteractWithEntity(PlayerInteractEvent.EntityInteract event) {
        return AMCompat.cancelIf(() -> interactWithEntity(event.getEntity(), event.getItemStack(), event.getLevel(), event.getTarget(), () -> {
            AMCompat.cancelEvent();
            event.setCancellationResult(InteractionResult.SUCCESS);
        }));
    }
    *///?} else {
    public void onInteractWithEntity(PlayerInteractEvent.EntityInteract event) {
        interactWithEntity(event.getEntity(), event.getItemStack(), event.getLevel(), event.getTarget(), () -> {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        });
    }
    //?}

    /**
     * The same body again, one interaction phase earlier.
     *
     * <p>Vanilla sends {@code INTERACT_AT} first and only falls through to {@code INTERACT} when
     * nothing consumed it, and the loaders fire {@code PlayerInteractEvent.EntityInteractSpecific}
     * in the first branch and {@link PlayerInteractEvent.EntityInteract} in the second. Any mod
     * whose entity consumes the click in {@code Entity#interactAt} therefore makes the second
     * event unreachable — MCA Reborn's villagers open their conversation GUI there, from a
     * {@code final} override, so a lassoed villager could never be freed, ender flu could never be
     * cured with a chorus fruit and a rainbow one could never be wiped off with a sponge.
     * Listening to both phases fixes it without touching anyone else's interaction: the body only
     * acts on its own four conditions, and cancelling here stops the client sending the second
     * packet at all, so nothing runs twice.
     *
     * <p>Only where the two phases actually exist, which is not everywhere:
     * <ul>
     *   <li>Fabric never fires the stub — its {@code UseEntityCallback} already runs ahead of
     *       {@code Entity#interactAt} for both packet actions.</li>
     *   <li>Forge 26 deleted {@code EntityInteract} and kept the Specific one, so a rename rule in
     *       {@code stonecutter.gradle.kts} points the handler above at it and this one would be a
     *       duplicate registration.</li>
     *   <li>NeoForge deleted {@code EntityInteractSpecific} in {@code 26.2.0.43-beta} (the merge in
     *       neoforged/NeoForge#3339) — and 26.2 vanilla merged the phases too: {@code interactAt}
     *       is gone and there is one {@code Entity#interact(Player, InteractionHand, Vec3)} that
     *       the surviving event precedes, so the handler above is complete on its own. Compiling
     *       against an older build and shipping the class anyway is a hard
     *       {@code NoClassDefFoundError} at listener registration.</li>
     * </ul>
     */
    //? if forge && >=1.21.6 && <26 {
    /*@SubscribeEvent
    public boolean onInteractWithEntitySpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        return AMCompat.cancelIf(() -> interactWithEntity(event.getEntity(), event.getItemStack(), event.getLevel(), event.getTarget(), () -> {
            AMCompat.cancelEvent();
            event.setCancellationResult(InteractionResult.SUCCESS);
        }));
    }
    *///?}

    //? if (forge && <1.21.6) || (neoforge && <26.2) {
    @SubscribeEvent
    public void onInteractWithEntitySpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        interactWithEntity(event.getEntity(), event.getItemStack(), event.getLevel(), event.getTarget(), () -> {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        });
    }
    //?}

    private void interactWithEntity(Player player, ItemStack held, Level level, Entity targeted, Runnable consume) {
        if (targeted instanceof LivingEntity living) {
            if (!player.isShiftKeyDown() && VineLassoUtil.hasLassoData(living)) {
                if (!player.level().isClientSide()) {
                    AMCompat.spawnAtLocation(targeted, new ItemStack(AMItemRegistry.VINE_LASSO.get()));
                }
                VineLassoUtil.lassoTo(null, living);
                consume.run();
            }
            if (!(targeted instanceof Player) && !(targeted instanceof EntityEndergrade)
                    && living.hasEffect(AMCompat.effect(AMEffectRegistry.ENDER_FLU.get()))) {
                if (held.getItem() == Items.CHORUS_FRUIT) {
                    if (!player.isCreative()) {
                        held.shrink(1);
                    }
                    targeted.gameEvent(GameEvent.EAT);
                    targeted.playSound(SoundEvents.GENERIC_EAT, 1.0F, 0.5F + player.getRandom().nextFloat());
                    if (player.getRandom().nextFloat() < 0.4F) {
                        living.removeEffect(AMCompat.effect(AMEffectRegistry.ENDER_FLU.get()));
                        Items.CHORUS_FRUIT.finishUsingItem(held.copy(), level, living);
                    }
                    consume.run();
                }
            }
            if (RainbowUtil.getRainbowType(living) > 0 && (held.getItem() == Items.SPONGE)) {
                consume.run();
                RainbowUtil.setRainbowType(living, 0);
                if (!player.isCreative()) {
                    held.shrink(1);
                }
                ItemStack wetSponge = new ItemStack(Items.WET_SPONGE);
                if (!player.addItem(wetSponge)) {
                    player.drop(wetSponge, true);
                }
            }
            if (living instanceof Rabbit rabbit && held.getItem() == AMItemRegistry.MUNGAL_SPORES.get()
                    && AMConfig.bunfungusTransformation) {
                final var random = ThreadLocalRandom.current();
                if (!player.level().isClientSide() && random.nextFloat() < 0.15F) {
                    //? if >=1.21.2 {
                    /*// convertTo now finalises the spawn and removes the old entity itself.
                    rabbit.convertTo(AMEntityRegistry.BUNFUNGUS.get(), net.minecraft.world.entity.ConversionParams.single(rabbit, false, false), bunfungus -> bunfungus.setTransformsIn(EntityBunfungus.MAX_TRANSFORM_TIME));
                    *///?} else {
                    final EntityBunfungus bunfungus = rabbit.convertTo(AMEntityRegistry.BUNFUNGUS.get(), true);
                    if (bunfungus != null) {
                        player.level().addFreshEntity(bunfungus);
                        bunfungus.setTransformsIn(EntityBunfungus.MAX_TRANSFORM_TIME);
                    }
                    //?}
                } else {
                    for (int i = 0; i < 2 + random.nextInt(2); i++) {
                        final double d0 = random.nextGaussian() * 0.02D;
                        final double d1 = 0.05F + random.nextGaussian() * 0.02D;
                        final double d2 = random.nextGaussian() * 0.02D;
                        targeted.level().addParticle(AMParticleRegistry.BUNFUNGUS_TRANSFORMATION.get(), targeted.getRandomX(0.7F), targeted.getY(0.6F), targeted.getRandomZ(0.7F), d0, d1, d2);
                    }
                }
                if (!player.isCreative()) {
                    held.shrink(1);
                }
                consume.run();
            }
        }
    }

    @SubscribeEvent
    public void onUseItemAir(PlayerInteractEvent.RightClickEmpty event) {
        ItemStack stack = event.getEntity().getItemInHand(event.getHand());
        if (stack.isEmpty()) {
            stack = event.getEntity().getItemBySlot(EquipmentSlot.MAINHAND);
        }
        if (RainbowUtil.getRainbowType(event.getEntity()) > 0 && (stack.is(Items.SPONGE))) {
            event.getEntity().swing(InteractionHand.MAIN_HAND);
            RainbowUtil.setRainbowType(event.getEntity(), 0);
            if (!event.getEntity().isCreative()) {
                stack.shrink(1);
            }
            ItemStack wetSponge = new ItemStack(Items.WET_SPONGE);
            if (!event.getEntity().addItem(wetSponge)) {
                event.getEntity().drop(wetSponge, true);
            }
        }
    }

    @SubscribeEvent
    //? if forge && >=1.21.6 {
    /*public boolean onUseItemOnBlock(PlayerInteractEvent.RightClickBlock event) {
        return AMCompat.cancelIf(() -> onUseItemOnBlock0(event));
    }

    private void onUseItemOnBlock0(PlayerInteractEvent.RightClickBlock event) {
    *///?} else {
    public void onUseItemOnBlock(PlayerInteractEvent.RightClickBlock event) {
    //?}
        if (AlexsMobs.isAprilFools() && event.getItemStack().is(Items.STICK)
            && !com.github.alexthe666.alexsmobs.misc.AMCompat.isOnCooldown(event.getEntity().getCooldowns(), Items.STICK)) {
            BlockState state = event.getEntity().level().getBlockState(event.getPos());
            boolean flag = false;
            if (state.is(Blocks.SAND)) {
                flag = true;
                event.getEntity().level().setBlockAndUpdate(event.getPos(), AMBlockRegistry.SAND_CIRCLE.get().defaultBlockState());
            } else if (state.is(Blocks.RED_SAND)) {
                flag = true;
                event.getEntity().level().setBlockAndUpdate(event.getPos(), AMBlockRegistry.RED_SAND_CIRCLE.get().defaultBlockState());
            }
            if (flag) {
                //? if forge && >=1.21.6 {
                /*AMCompat.cancelEvent();
                *///?} else {
                event.setCanceled(true);
                //?}
                event.getEntity().gameEvent(GameEvent.BLOCK_PLACE);
                event.getEntity().playSound(SoundEvents.SAND_BREAK, 1, 1);
                com.github.alexthe666.alexsmobs.misc.AMCompat.addCooldown(event.getEntity().getCooldowns(), Items.STICK, 30);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
        }
    }

    @SubscribeEvent
    public void onEntityDrops(LivingDropsEvent event) {
        if (VineLassoUtil.hasLassoData(event.getEntity())) {
            VineLassoUtil.lassoTo(null, event.getEntity());
            event.getDrops().add(new ItemEntity(event.getEntity().level(), event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), new ItemStack(AMItemRegistry.VINE_LASSO.get())));
        }
    }

    @SubscribeEvent
    // NeoForge 20.6 promoted MobSpawnEvent.FinalizeSpawn to a top-level FinalizeSpawnEvent.
    //? if neoforge && >=1.20.6
    //public void onEntityFinalizeSpawn(net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent event) {
    //? if forge || fabric || <1.20.6
    public void onEntityFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        final var entity = event.getEntity();
        if (entity instanceof WanderingTrader trader && AMConfig.elephantTraderSpawnChance > 0) {
            Biome biome = event.getLevel().getBiome(entity.blockPosition()).value();
            if (RAND.nextFloat() <= AMConfig.elephantTraderSpawnChance && (!AMConfig.limitElephantTraderBiomes || biome.getBaseTemperature() >= 1.0F)) {
                ChunkPos chunkPos = new ChunkPos(trader.blockPosition());
                if(event.getLevel().getChunkSource().getChunkNow(chunkPos.x, chunkPos.z) != null) {
                    EntityElephant elephant = AMCompat.create(AMEntityRegistry.ELEPHANT.get(), trader.level());
                    elephant.copyPosition(trader);
                    if (elephant.canSpawnWithTraderHere()) {
                        elephant.setTrader(true);
                        elephant.setChested(true);
                        if (!event.getLevel().isClientSide()) {
                            trader.level().addFreshEntity(elephant);
                            AMCompat.startRiding(trader, elephant, true);
                        }
                        elephant.addElephantLoot(null, RAND.nextInt());
                    }
                }
            }
        }
        try {
            if (AMConfig.spidersAttackFlies && entity instanceof final Spider spider) {
                spider.targetSelector.addGoal(4,
                    new NearestAttackableTargetGoal<>(spider, EntityFly.class, 1, true, false, null));
            }
            else if (AMConfig.wolvesAttackMoose && entity instanceof final Wolf wolf) {
                wolf.targetSelector.addGoal(6, new NonTameRandomTargetGoal<>(wolf, EntityMoose.class, false, null));
            }
            else if (AMConfig.polarBearsAttackSeals && entity instanceof final PolarBear bear) {
                bear.targetSelector.addGoal(6,
                    new NearestAttackableTargetGoal<>(bear, EntitySeal.class, 15, true, true, null));
            }
            else if (entity instanceof final Creeper creeper) {
                creeper.targetSelector.addGoal(3, new AvoidEntityGoal<>(creeper, EntitySnowLeopard.class, 6.0F, 1.0D, 1.2D));
                creeper.targetSelector.addGoal(3, new AvoidEntityGoal<>(creeper, EntityTiger.class, 6.0F, 1.0D, 1.2D));
            }
            else if (AMConfig.catsAndFoxesAttackJerboas
                    && (entity instanceof Fox || entity instanceof Cat || entity instanceof Ocelot)) {
                Mob mb = (Mob) entity;
                mb.targetSelector.addGoal(6,
                    new NearestAttackableTargetGoal<>(mb, EntityJerboa.class, 45, true, true, null));
            }
            else if (AMConfig.bunfungusTransformation && entity instanceof final Rabbit rabbit) {
                rabbit.goalSelector.addGoal(3, new TemptGoal(rabbit, 1.0D, Ingredient.of(AMItemRegistry.MUNGAL_SPORES.get()), false));
            }
            else if (AMConfig.dolphinsAttackFlyingFish && entity instanceof final Dolphin dolphin) {
                dolphin.targetSelector.addGoal(2,
                    new NearestAttackableTargetGoal<>(dolphin, EntityFlyingFish.class, 70, true, true, null));
            }
        } catch (Exception e) {
            AlexsMobs.LOGGER.warn("Tried to add unique behaviors to vanilla mobs and encountered an error");
        }
    }

    @SubscribeEvent
    public void onPlayerAttackEntityEvent(AttackEntityEvent event) {
        if (event.getTarget() instanceof LivingEntity living) {
            if (event.getEntity().getItemBySlot(EquipmentSlot.HEAD).getItem() == AMItemRegistry.MOOSE_HEADGEAR.get()) {
                AMCompat.knockback(living, 1F, Mth.sin(event.getEntity().getYRot() * Mth.DEG_TO_RAD),
                        -Mth.cos(event.getEntity().getYRot() * Mth.DEG_TO_RAD));
            }
            if (event.getEntity().hasEffect(AMCompat.effect(AMEffectRegistry.TIGERS_BLESSING.get()))
                    && !event.getTarget().isAlliedTo(event.getEntity()) && !(event.getTarget() instanceof EntityTiger)) {
                AABB bb = new AABB(event.getEntity().getX() - 32, event.getEntity().getY() - 32, event.getEntity().getZ() - 32, event.getEntity().getZ() + 32, event.getEntity().getY() + 32, event.getEntity().getZ() + 32);
                final var tigers = event.getEntity().level().getEntitiesOfClass(EntityTiger.class, bb,
                        EntitySelector.ENTITY_STILL_ALIVE);
                for (EntityTiger tiger : tigers) {
                    if (!tiger.isBaby()) {
                        tiger.setTarget(living);
                    }
                }
            }
        }
    }

    // NeoForge 1.21 split the old LivingDamageEvent in two: LivingIncomingDamageEvent (cancellable,
    // fires before armour/absorption) and LivingDamageEvent.Pre/.Post (not cancellable). Everything
    // here either cancels the hit or reacts to the raw incoming amount, so the incoming event is the
    // faithful mapping — getSource/getAmount/setCanceled all keep their meaning.
    //? if neoforge && >=1.21 {
    /*@SubscribeEvent
    public void onLivingDamageEvent(LivingIncomingDamageEvent event) {
    *///?} elif forge && >=1.21.6 {
    /*@SubscribeEvent
    public boolean onLivingDamageEvent(LivingDamageEvent event) {
        return AMCompat.cancelIf(() -> onLivingDamageEvent0(event));
    }

    private void onLivingDamageEvent0(LivingDamageEvent event) {
    *///?} else {
    @SubscribeEvent
    public void onLivingDamageEvent(LivingDamageEvent event) {
    //?}
        if (event.getSource().getEntity() instanceof final LivingEntity attacker) {
            if (event.getAmount() > 0 && attacker.hasEffect(AMCompat.effect(AMEffectRegistry.SOULSTEAL.get())) && attacker.getEffect(AMCompat.effect(AMEffectRegistry.SOULSTEAL.get())) != null) {
                final int level = attacker.getEffect(AMCompat.effect(AMEffectRegistry.SOULSTEAL.get())).getAmplifier() + 1;
                if (attacker.getHealth() < attacker.getMaxHealth()
                    && ThreadLocalRandom.current().nextFloat() < (0.25F + (level * 0.25F))) {
                    attacker.heal(Math.min(event.getAmount() / 2F * level, 2 + 2 * level));
                }
            }

            if (event.getEntity() instanceof final Player player) {
                if (attacker instanceof final EntityMimicOctopus octupus && octupus.isOwnedBy(player)) {
                    //? if forge && >=1.21.6 {
                    /*AMCompat.cancelEvent();
                    *///?} else {
                    event.setCanceled(true);
                    //?}
                    return;
                }
                if (player.getItemBySlot(EquipmentSlot.HEAD).getItem() == AMItemRegistry.SPIKED_TURTLE_SHELL.get()) {
                    if (attacker.distanceTo(player) < attacker.getBbWidth() + player.getBbWidth() + 0.5F) {
                        attacker.hurt(attacker.damageSources().thorns(player), 1F);
                        AMCompat.knockback(attacker, 0.5F, Mth.sin((attacker.getYRot() + 180) * Mth.DEG_TO_RAD),
                            -Mth.cos((attacker.getYRot() + 180) * Mth.DEG_TO_RAD));
                    }
                }
            }
        }
        if (!event.getEntity().getItemBySlot(EquipmentSlot.LEGS).isEmpty() && event.getEntity().getItemBySlot(EquipmentSlot.LEGS).getItem() == AMItemRegistry.EMU_LEGGINGS.get()) {
            if (event.getSource().is(DamageTypeTags.IS_PROJECTILE) && event.getEntity().getRandom().nextFloat() < AMConfig.emuPantsDodgeChance) {
                //? if forge && >=1.21.6 {
                /*AMCompat.cancelEvent();
                *///?} else {
                event.setCanceled(true);
                //?}
            }
        }
    }

    @SubscribeEvent
    //? if forge && >=1.21.6 {
    /*public boolean onLivingSetTargetEvent(LivingChangeTargetEvent event) {
        return AMCompat.cancelIf(() -> onLivingSetTargetEvent0(event));
    }

    private void onLivingSetTargetEvent0(LivingChangeTargetEvent event) {
    *///?} else {
    public void onLivingSetTargetEvent(LivingChangeTargetEvent event) {
    //?}
        if (event.getNewTarget() != null && event.getEntity() instanceof Mob mob) {
            if (AMCompat.isArthropod(mob)) {
                if (event.getNewTarget().hasEffect(AMCompat.effect(AMEffectRegistry.BUG_PHEROMONES.get())) && event.getEntity().getLastHurtByMob() != event.getNewTarget()) {
                    //? if forge && >=1.21.6 {
                    /*AMCompat.cancelEvent();
                    *///?} else {
                    event.setCanceled(true);
                    //?}
                    return;
                }
            }
            if (AMCompat.isUndead(mob) && !mob.getType().builtInRegistryHolder().is(AMTagRegistry.IGNORES_KIMONO)) {
                if (event.getNewTarget().getItemBySlot(EquipmentSlot.CHEST).is(AMItemRegistry.UNSETTLING_KIMONO.get()) && event.getEntity().getLastHurtByMob() != event.getNewTarget()) {
                    //? if forge && >=1.21.6 {
                    /*AMCompat.cancelEvent();
                    *///?} else {
                    event.setCanceled(true);
                    //?}
                    return;
                }
            }
        }
    }

    // NeoForge 20.6 folded LivingEvent.LivingTickEvent into the generic EntityTickEvent.
    //? if neoforge && >=1.20.6 {
    /*@SubscribeEvent
    public void onLivingUpdateEvent(net.neoforged.neoforge.event.tick.EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof LivingEntity living) {
            onLivingUpdate(living);
        }
    }
    *///?} else {
    @SubscribeEvent
    public void onLivingUpdateEvent(LivingEvent.LivingTickEvent event) {
        onLivingUpdate(event.getEntity());
    }
    //?}

    private void onLivingUpdate(LivingEntity entity) {
        if (entity instanceof Player player) {
            if (player.getEyeHeight() < player.getBbHeight() * 0.5D) {
                player.refreshDimensions();
            }
            if(entity.getAttributes().hasAttribute(Attributes.MOVEMENT_SPEED)){
                final var attributes = entity.getAttribute(Attributes.MOVEMENT_SPEED);
                if (player.getItemBySlot(EquipmentSlot.FEET).getItem() == AMItemRegistry.ROADDRUNNER_BOOTS.get()
                        || AMCompat.hasModifier(attributes, SAND_SPEED_MODIFIER)) {
                    final boolean sand = player.level().getBlockState(getDownPos(player.blockPosition(), player.level()))
                            .is(BlockTags.SAND);
                    if (sand && !AMCompat.hasModifier(attributes, SAND_SPEED_MODIFIER)) {
                        attributes.addPermanentModifier(SAND_SPEED_BONUS);
                    }
                    if (player.tickCount % 25 == 0
                            && (player.getItemBySlot(EquipmentSlot.FEET).getItem() != AMItemRegistry.ROADDRUNNER_BOOTS.get()
                            || !sand)
                            && AMCompat.hasModifier(attributes, SAND_SPEED_MODIFIER)) {
                        attributes.removeModifier(SAND_SPEED_MODIFIER);
                    }
                }
                if (player.getItemBySlot(EquipmentSlot.HEAD).getItem() == AMItemRegistry.FRONTIER_CAP.get()
                        || AMCompat.hasModifier(attributes, SNEAK_SPEED_MODIFIER)) {
                    final var shift = player.isShiftKeyDown();
                    if (shift && !AMCompat.hasModifier(attributes, SNEAK_SPEED_MODIFIER)) {
                        attributes.addPermanentModifier(SNEAK_SPEED_BONUS);
                    }
                    if ((!shift || player.getItemBySlot(EquipmentSlot.HEAD).getItem() != AMItemRegistry.FRONTIER_CAP.get())
                            && AMCompat.hasModifier(attributes, SNEAK_SPEED_MODIFIER)) {
                        attributes.removeModifier(SNEAK_SPEED_MODIFIER);
                    }
                }
            }
            if (player.getItemBySlot(EquipmentSlot.HEAD).getItem() == AMItemRegistry.SPIKED_TURTLE_SHELL.get()) {
                if (!player.isEyeInFluid(FluidTags.WATER)) {
                    player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 310, 0, false, false, true));
                }
            }
        }
        final ItemStack boots = entity.getItemBySlot(EquipmentSlot.FEET);
        if (!boots.isEmpty() && AMCompat.hasTag(boots) && AMCompat.getOrCreateTag(boots).contains("BisonFur") && AMCompat.getBoolean(AMCompat.getOrCreateTag(boots), "BisonFur")) {
            BlockPos posBelow = new BlockPos((int) entity.getX(), (int) (entity.getBoundingBox().minY - 0.1F), (int) entity.getZ());
            if (entity.level().getBlockState(posBelow).is(Blocks.POWDER_SNOW)) {
                entity.setOnGround(true);
                entity.setTicksFrozen(0);
                entity.setPos(entity.getX(), Math.max(entity.getY(), posBelow.getY() + 1F), entity.getZ());
            }
            if (entity.isInPowderSnow) {
                entity.setOnGround(true);
                entity.setDeltaMovement(entity.getDeltaMovement().add(0, 0.1F, 0));
            }
        }
        if (entity.getItemBySlot(EquipmentSlot.LEGS).getItem() == AMItemRegistry.CENTIPEDE_LEGGINGS.get()) {
            if (entity.horizontalCollision && !entity.isInWater()) {
                entity.fallDistance = 0.0F;
                Vec3 motion = entity.getDeltaMovement();
                double d2 = 0.1D;
                // isScaffolding is a Forge BlockState extension — AMCompat reproduces its default
                // plus this mod's one override on Fabric. Same gate-don't-reroute reasoning as the
                // getPersistentData call above.
                //? if fabric {
                /*if (entity.isShiftKeyDown() || !AMCompat.isScaffolding(entity.getFeetBlockState(), entity) && entity.isSuppressingSlidingDownLadder()) {
                *///?} else {
                if (entity.isShiftKeyDown() || !entity.getFeetBlockState().isScaffolding(entity) && entity.isSuppressingSlidingDownLadder()) {
                //?}
                    d2 = 0.0D;
                }
                motion = new Vec3(Mth.clamp(motion.x, -0.15F, 0.15F), d2, Mth.clamp(motion.z, -0.15F, 0.15F));
                entity.setDeltaMovement(motion);
            }
        }
        if (entity.getItemBySlot(EquipmentSlot.HEAD).getItem() == AMItemRegistry.SOMBRERO.get() && !entity.level().isClientSide() && AlexsMobs.isAprilFools() && entity.isInWaterOrBubble()) {
            RandomSource random = entity.getRandom();
            if (random.nextInt(245) == 0 && !EntitySeaBear.isMobSafe(entity)) {
                final int dist = 32;
                final var nearbySeabears = entity.level().getEntitiesOfClass(EntitySeaBear.class,
                    entity.getBoundingBox().inflate(dist, dist, dist));
                if (nearbySeabears.isEmpty()) {
                    final EntitySeaBear bear = AMCompat.create(AMEntityRegistry.SEA_BEAR.get(), entity.level());
                    final BlockPos at = entity.blockPosition();
                    BlockPos farOff = null;
                    for (int i = 0; i < 15; i++) {
                        final int f1 = (int) Math.signum(random.nextInt() - 0.5F);
                        final int f2 = (int) Math.signum(random.nextInt() - 0.5F);
                        final BlockPos pos1 = at.offset(f1 * (10 + random.nextInt(dist - 10)), random.nextInt(1),
                            f2 * (10 + random.nextInt(dist - 10)));
                        if (entity.level().isWaterAt(pos1)) {
                            farOff = pos1;
                        }
                    }
                    if (farOff != null) {
                        bear.setPos(farOff.getX() + 0.5F, farOff.getY() + 0.5F, farOff.getZ() + 0.5F);
                        bear.setYRot(random.nextFloat() * 360F);
                        bear.setTarget(entity);
                        entity.level().addFreshEntity(bear);
                    }
                } else {
                    for (EntitySeaBear bear : nearbySeabears) {
                        bear.setTarget(entity);
                    }
                }
            }
        }
        if (VineLassoUtil.hasLassoData(entity)) {
            VineLassoUtil.tickLasso(entity);
        }
        if (RockyChestplateUtil.isWearing(entity)) {
            RockyChestplateUtil.tickRockyRolling(entity);
        }
        if (FlyingFishBootsUtil.isWearing(entity)) {
            FlyingFishBootsUtil.tickFlyingFishBoots(entity);
        }
    }

    private BlockPos getDownPos(BlockPos entered, LevelAccessor world) {
        int i = 0;
        while (world.isEmptyBlock(entered) && i < 3) {
            entered = entered.below();
            i++;
        }
        return entered;
    }

    @SubscribeEvent
    public void onFOVUpdate(ComputeFovModifierEvent event) {
        if (event.getPlayer().hasEffect(AMCompat.effect(AMEffectRegistry.FEAR.get())) || event.getPlayer().hasEffect(AMCompat.effect(AMEffectRegistry.POWER_DOWN.get()))) {
            event.setNewFovModifier(1.0F);
        }
    }

    // LivingAttackEvent was folded into LivingIncomingDamageEvent on NeoForge 1.21 — same "a hit is
    // about to land, before reductions" point in the pipeline.
    //? if neoforge && >=1.21 {
    /*@SubscribeEvent
    public void onLivingAttack(LivingIncomingDamageEvent event) {
    *///?} else {
    @SubscribeEvent
    public void onLivingAttack(LivingAttackEvent event) {
    //?}
        if (!event.getEntity().getUseItem().isEmpty() && event.getSource() != null && event.getSource().getEntity() != null) {
            if (event.getEntity().getUseItem().getItem() == AMItemRegistry.SHIELD_OF_THE_DEEP.get()) {
                if (event.getSource().getEntity() instanceof LivingEntity living) {
                    boolean flag = false;
                    if (living.distanceTo(event.getEntity()) <= 4
                        && !living.hasEffect(AMCompat.effect(AMEffectRegistry.EXSANGUINATION.get()))) {
                        living.addEffect(new MobEffectInstance(AMCompat.effect(AMEffectRegistry.EXSANGUINATION.get()), 60, 2));
                        flag = true;
                    }
                    if (event.getEntity().isInWaterOrBubble()) {
                        event.getEntity().setAirSupply(Math.min(event.getEntity().getMaxAirSupply(), event.getEntity().getAirSupply() + 150));
                        flag = true;
                    }
                    if (flag) {
                        AMCompat.hurtAndBreak(event.getEntity().getUseItem(), 1, event.getEntity(), event.getEntity().getUsedItemHand());
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        CompoundTag tag = AMCompat.getTag(event.getItemStack());
        if (tag != null && AMCompat.contains(tag, "BisonFur") && AMCompat.getBoolean(tag, "BisonFur")) {
            event.getToolTip().add(Component.translatable("item.alexsmobs.insulated_with_fur").withStyle(ChatFormatting.AQUA));
        }
    }

    //? if neoforge && >=1.21.4 {
    /*@SubscribeEvent
    public void onAddReloadListener(net.neoforged.neoforge.event.AddServerReloadListenersEvent event){
        AlexsMobs.LOGGER.info("Adding datapack listener capsid_recipes");
        // 1.21.4's AddServerReloadListenersEvent#addListener is keyed by a ResourceLocation, and its
        // getRegistryAccess() is the tag-bound provider the codec needs to resolve item/tag refs.
        event.addListener(AMCompat.rl("alexsmobs", "capsid_recipes"), AlexsMobs.PROXY.getCapsidRecipeManager(event.getRegistryAccess()));
    }
    *///?} else {
    @SubscribeEvent
    public void onAddReloadListener(AddReloadListenerEvent event){
        AlexsMobs.LOGGER.info("Adding datapack listener capsid_recipes");
        //? if forge && >=1.21.2 {
        /*// Forge's getRegistries() is the reload's tag-bound provider; getRegistryAccess() is a
        // pre-tag snapshot, so a tag ingredient decoded in prepare() sees the tag as missing.
        event.addListener(AlexsMobs.PROXY.getCapsidRecipeManager(event.getRegistries()));
        *///?} elif (neoforge || fabric) && >=1.21.2 {
        /*// NOTE (#84): on Fabric this whole method is DEAD — AddReloadListenerEvent is one of the
        // net.minecraftforge.** stubs in fabric/forge/**, and nothing on that loader ever
        // constructs or fires it. The real Fabric registration is ResourceManagerHelper, in
        // AlexsMobsFabric#onInitialize. This arm stays only so the stub keeps compiling.
        // Fabric joins this arm rather than the no-arg one below: from 1.21.2 the capsid codec
        // resolves item/tag ingredients against the provider, and the no-arg fallback in
        // CommonProxy passes RegistryAccess.EMPTY — which decodes every tag ingredient as empty.
        event.addListener(AlexsMobs.PROXY.getCapsidRecipeManager(event.getRegistryAccess()));
        *///?} else {
        event.addListener(AlexsMobs.PROXY.getCapsidRecipeManager());
        //?}
    }
    //?}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onHarvestCheck(PlayerEvent.HarvestCheck event){
        if(event.getEntity() != null && event.getEntity().isHolding(AMItemRegistry.GHOSTLY_PICKAXE.get()) && ItemGhostlyPickaxe.shouldStoreInGhost(event.getEntity(), event.getEntity().getMainHandItem())){
            //stops drops from being spawned
            event.setCanHarvest(false);
        }
    }

    // `/amc config` — the operator-facing view of config/amc.json. A SERVER command (unlike
    // /shieldpose in ClientEvents, which is a client one): it edits settings the server owns, so it
    // must run there and be gated on a permission level.
    //
    // Forge and NeoForge expose the same event with the same getDispatcher() signature on every
    // node from 1.20.1 to 26.2; only the package differs, so the two arms are the same text twice.
    // Fabric has no such event and registers CommandRegistrationCallback from FabricServerEvents
    // instead — hence no `else` arm here. The fully-qualified type is deliberate: an import would
    // have to be gated too, and this file's import block is already the busiest in the tree.
    //? if forge {
    @SubscribeEvent
    public void onRegisterCommands(net.minecraftforge.event.RegisterCommandsEvent event) {
        com.github.alexthe666.alexsmobs.command.AMConfigCommand.register(event.getDispatcher());
    }
    //?} elif neoforge {
    /*@SubscribeEvent
    public void onRegisterCommands(net.neoforged.neoforge.event.RegisterCommandsEvent event) {
        com.github.alexthe666.alexsmobs.command.AMConfigCommand.register(event.getDispatcher());
    }
    *///?}

}

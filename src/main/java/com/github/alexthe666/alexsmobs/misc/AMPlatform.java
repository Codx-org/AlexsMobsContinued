package com.github.alexthe666.alexsmobs.misc;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Forge APIs that moved between the MC versions this mod spans, funnelled through one place
 * so the per-version Stonecutter conditionals live here instead of in ~25 entity classes.
 */
public class AMPlatform {

    /**
     * Spawn packet for an entity. Every one of the 21 callers overrides
     * {@code Entity#getAddEntityPacket} purely to obtain a correct add-entity packet, so on
     * every loader and every version this is simply vanilla's own packet, built directly.
     *
     * <p><b>Do NOT route Forge through {@code ForgeHooks.getEntitySpawnPacket}.</b> That helper
     * opens with {@code if (!(entity instanceof IEntityAdditionalSpawnData add)) throw new
     * IllegalArgumentException(...)} on every Forge build from 1.20.2 up (read out of the
     * userdev sources for 49.2.8, 50.2.9 and 60.1.11) — it is a hook for entities that carry
     * extra spawn data, not a general-purpose builder, and nothing in this mod implements that
     * interface. 1.20.1's {@code NetworkHooks.getEntitySpawningPacket} guards the same test with
     * an {@code if/else} instead, which is the only reason that one node ever worked. See #109.
     *
     * <p><b>The NeoForge branches must NOT call {@code entity.getAddEntityPacket(…)}.</b> Every
     * one of the 21 callers reaches this method <i>from</i> its own override of exactly that
     * method, so delegating back to it is a virtual dispatch straight into the caller —
     * unbounded mutual recursion, i.e. a {@code StackOverflowError} the instant the entity is
     * first sent to a client. (This is why the squid grapple, the tossed item, the void-worm
     * parts, … all "did nothing" on NeoForge.) Build the vanilla packet directly instead; that
     * is precisely what {@code Entity#getAddEntityPacket} does, and NeoForge appends the
     * {@code IEntityWithComplexSpawn} payload as a separate packet from {@code ServerEntity}.
     */
    @SuppressWarnings("unchecked")
    //? if <1.21 {
    public static Packet<ClientGamePacketListener> getEntitySpawningPacket(Entity entity) {
        //?} else {
        /*public static Packet<ClientGamePacketListener> getEntitySpawningPacket(Entity entity, net.minecraft.server.level.ServerEntity serverEntity) {
        *///?}
        // 1.21 gave Entity#getAddEntityPacket a ServerEntity argument; the overriding entity
        // classes get it threaded in by a Stonecutter replacement (see stonecutter.gradle.kts).
        // All three loaders build vanilla's own packet. Nothing in this mod implements
        // IEntityAdditionalSpawnData / IEntityWithComplexSpawn and neither writeSpawnData nor
        // readSpawnData exists anywhere in the tree (measured, not assumed: the only mentions of
        // either interface are the comments in this file), and no entity type registers a
        // setCustomClientFactory, so there is no extra payload for a loader hook to carry.
        // NeoForge appends an IEntityWithComplexSpawn payload from ServerEntity by itself.
        // ⚠️ The gap becomes real the moment an entity gains genuine spawn data — at which point
        // Fabric needs a custom payload and NeoForge is already covered by ServerEntity.
        //? if (neoforge || fabric) && <1.21 {
        /*return new net.minecraft.network.protocol.game.ClientboundAddEntityPacket(entity);
        *///?}
        //? if forge && >=1.20.2 && <1.21 {
        /*return new net.minecraft.network.protocol.game.ClientboundAddEntityPacket(entity);
        *///?}
        //? if >=1.21 {
        /*return new net.minecraft.network.protocol.game.ClientboundAddEntityPacket(entity, serverEntity);
        *///?}
        //? if forge && <1.20.2
        return (Packet<ClientGamePacketListener>) net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(entity);
    }

    /**
     * Posts an event and reports whether a listener cancelled it. Forge's {@code post} returns
     * that boolean directly; NeoForge's returns the event and moved cancellation onto the
     * {@code ICancellableEvent} interface.
     *
     * <p>Only exists below 1.21.2. Its three callers ({@code RenderTiger}, {@code RenderUnderminer},
     * {@code RenderFarseer}) fire the render-living events by hand, and from 1.21.2 up they all go
     * through {@code AMRenderEventCompat} instead — which has to name the concrete event type
     * anyway, because EventBus 7 posts on a per-event bus and has no common {@code Event} base to
     * take as a parameter.
     */
    //? if forge && <1.21.2 {
    public static boolean postCancelled(net.minecraftforge.eventbus.api.Event event) {
        return net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
    }
    //?} elif neoforge && <1.21.2 {
    /*public static boolean postCancelled(net.minecraftforge.eventbus.api.Event event) {
        return net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event) instanceof net.neoforged.bus.api.ICancellableEvent cancellable && cancellable.isCanceled();
    }
    *///?}

    /**
     * The passenger's own seating offset. 1.20.2 reworked entity attachment points:
     * {@code getMyRidingOffset()} gained the vehicle parameter (and returns float).
     */
    public static double myRidingOffset(Entity passenger, Entity vehicle) {
        // 1.20.5 finished the job: the offset became an entity attachment point, subtracted
        // from the vehicle's seat position, so the old scalar is its negated Y.
        //? if >=1.20.5
        //return -passenger.getVehicleAttachmentPoint(vehicle).y;
        //? if >=1.20.2 && <1.20.5
        //return passenger.getMyRidingOffset(vehicle);
        //? if <1.20.2
        return passenger.getMyRidingOffset();
    }

    /**
     * Falling sand with a custom dust colour. 1.20.2 renamed {@code SandBlock} to
     * {@code ColoredFallingBlock} and wrapped the packed colour in a {@code ColorRGBA}.
     */
    public static Block coloredSand(int rgb, BlockBehaviour.Properties props) {
        //? if >=1.20.2
        //return new net.minecraft.world.level.block.ColoredFallingBlock(new net.minecraft.util.ColorRGBA(rgb), props);
        //? if <1.20.2
        return new net.minecraft.world.level.block.SandBlock(rgb, props);
    }

    /**
     * 1.20.3 collapsed {@code ENTITY_ROAR}/{@code ENTITY_SHAKE} and friends into one
     * {@code ENTITY_ACTION} game event.
     */
    //? if >=1.20.5 {
    /*public static final net.minecraft.core.Holder<GameEvent> ENTITY_ACTION = GameEvent.ENTITY_ACTION;
    *///?}
    //? if >=1.20.3 && <1.20.5 {
    /*public static final GameEvent ENTITY_ACTION = GameEvent.ENTITY_ACTION;
    *///?}
    //? if <1.20.3 {
    public static final GameEvent ENTITY_ACTION = GameEvent.ENTITY_ROAR;
    //?}

    /**
     * 1.20.3 made {@code Block.codec()} abstract so blocks can be described in datapacks.
     * Alex's Mobs' blocks are never codec-serialised (no worldgen feature references them by
     * value), so they all return this placeholder rather than growing a real codec each.
     */
    @SuppressWarnings("unchecked")
    public static <B extends Block> com.mojang.serialization.MapCodec<B> unsupportedBlockCodec() {
        return (com.mojang.serialization.MapCodec<B>) UNSUPPORTED_BLOCK_CODEC;
    }

    private static final com.mojang.serialization.MapCodec<?> UNSUPPORTED_BLOCK_CODEC =
            com.mojang.serialization.MapCodec.unit(() -> {
                throw new UnsupportedOperationException("Alex's Mobs blocks are not codec-serializable");
            });

    // NeoForge exposes ForgeMod's built-in fluid types and attributes as vanilla Holders,
    // where Forge still hands out RegistryObjects — same value, different accessor.

    // Fabric has no FluidType at all — the concept is Forge's — so these two do not exist there and
    // neither does their only caller (EntityLaviathan#getFluidMotionScale, a Forge extension point).
    // Three sibling arms rather than line gates inside one method body: a Fabric arm cannot merely
    // change the body, the RETURN TYPE is what is missing.
    //? if forge {
    public static net.minecraftforge.fluids.FluidType waterType() {
        return net.minecraftforge.common.ForgeMod.WATER_TYPE.get();
    }
    //?} elif neoforge {
    /*public static net.minecraftforge.fluids.FluidType waterType() {
        return net.minecraftforge.common.ForgeMod.WATER_TYPE.value();
    }
    *///?}

    // These three read a fluid depth / presence. Forge and NeoForge answer them through FluidType,
    // which 26 deleted in favour of the vanilla tag-based accessors — and Fabric never had it at
    // all. So the ">=26" arm is also the FABRIC arm on every node: Entity#getFluidHeight(TagKey),
    // #isInWater and #isInLava are plain vanilla and exist unchanged all the way down (javap-verified
    // on the unpatched named 1.21.11 jar). The only behavioural difference is that the FluidType
    // form also counts MODDED fluids tagged as water/lava-like, which nothing on Fabric can provide.
    public static double fluidHeightWater(net.minecraft.world.entity.Entity entity) {
        //? if >=26 || fabric
        /*return entity.getFluidHeight(net.minecraft.tags.FluidTags.WATER);*/
        //? if <26 && !fabric
        return entity.getFluidTypeHeight(waterType());
    }

    public static double fluidHeightLava(net.minecraft.world.entity.Entity entity) {
        //? if >=26 || fabric
        /*return entity.getFluidHeight(net.minecraft.tags.FluidTags.LAVA);*/
        //? if <26 && !fabric
        return entity.getFluidTypeHeight(lavaType());
    }

    public static boolean isInAnyFluid(net.minecraft.world.entity.Entity entity) {
        //? if >=26 || fabric
        /*return entity.isInWater() || entity.isInLava();*/
        //? if <26 && !fabric
        return entity.isInFluidType();
    }

    //? if forge {
    public static net.minecraftforge.fluids.FluidType lavaType() {
        return net.minecraftforge.common.ForgeMod.LAVA_TYPE.get();
    }
    //?} elif neoforge {
    /*public static net.minecraftforge.fluids.FluidType lavaType() {
        return net.minecraftforge.common.ForgeMod.LAVA_TYPE.value();
    }
    *///?}

    // 1.20.5 put every attribute behind a Holder, and vanilla absorbed Forge's two reach
    // attributes as minecraft:block_interaction_range / minecraft:entity_interaction_range.
    // Four flat blocks rather than nested conditionals — Stonecutter does not nest them.

    //? if forge && <1.20.5 {
    public static net.minecraft.world.entity.ai.attributes.Attribute swimSpeed() {
        return net.minecraftforge.common.ForgeMod.SWIM_SPEED.get();
    }

    public static net.minecraft.world.entity.ai.attributes.Attribute blockReach() {
        return net.minecraftforge.common.ForgeMod.BLOCK_REACH.get();
    }

    public static net.minecraft.world.entity.ai.attributes.Attribute entityReach() {
        return net.minecraftforge.common.ForgeMod.ENTITY_REACH.get();
    }
    //?}

    //? if neoforge && <1.20.5 {
    /*public static net.minecraft.world.entity.ai.attributes.Attribute swimSpeed() {
        return net.minecraftforge.common.ForgeMod.SWIM_SPEED.value();
    }

    public static net.minecraft.world.entity.ai.attributes.Attribute blockReach() {
        return net.minecraftforge.common.ForgeMod.BLOCK_REACH.value();
    }

    public static net.minecraft.world.entity.ai.attributes.Attribute entityReach() {
        return net.minecraftforge.common.ForgeMod.ENTITY_REACH.value();
    }
    *///?}

    //? if forge && >=1.20.5 {
    /*public static net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> swimSpeed() {
        return net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.wrapAsHolder(net.minecraftforge.common.ForgeMod.SWIM_SPEED.get());
    }

    public static net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> blockReach() {
        return net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_INTERACTION_RANGE;
    }

    public static net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> entityReach() {
        return net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE;
    }
    *///?}

    //? if neoforge && >=1.20.5 {
    /*public static net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> swimSpeed() {
        return net.minecraftforge.common.ForgeMod.SWIM_SPEED;
    }

    public static net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> blockReach() {
        return net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_INTERACTION_RANGE;
    }

    public static net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> entityReach() {
        return net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE;
    }
    *///?}

    // Fabric has no ForgeMod, so only what vanilla itself absorbed is available — and it absorbed
    // the two reach attributes at 1.20.5 and a swim-speed analogue (WATER_MOVEMENT_EFFICIENCY, the
    // depth-strider replacement) at 1.21. Where there is no vanilla attribute these return NULL, so
    // EVERY call site must null-check — an armour piece silently loses that one modifier rather than
    // the whole item failing to build. Two arms because the swim answer changes at 1.21 while the
    // reach answer does not.
    //
    // ⚠️ This comment used to claim every call site already did. It was wrong by one, and that one
    // hard-crashed the client: ServerEvents#rayTrace dereferenced blockReach() unguarded, so on
    // Fabric 1.20.1/1.20.4 every right-click with a glass bottle threw (report #29, fixed in 2.0.6).
    // A returns-null-on-some-nodes helper is invisible to the compiler and to all 49 build gates —
    // when adding a call site, grep the others; when adding an arm, re-check the call sites.
    //? if fabric && >=1.21 {
    /*public static net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> swimSpeed() {
        return net.minecraft.world.entity.ai.attributes.Attributes.WATER_MOVEMENT_EFFICIENCY;
    }

    public static net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> blockReach() {
        return net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_INTERACTION_RANGE;
    }

    public static net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> entityReach() {
        return net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE;
    }
    *///?}

    //? if fabric && >=1.20.5 && <1.21 {
    /*// 1.20.6: reach exists, swim speed does not. The crocodile/flying-fish swim bonus is dropped.
    public static net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> swimSpeed() {
        return null;
    }

    public static net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> blockReach() {
        return net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_INTERACTION_RANGE;
    }

    public static net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> entityReach() {
        return net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE;
    }
    *///?}

    //? if fabric && <1.20.5 {
    /*// 1.20.1 / 1.20.4: vanilla has none of the three — reach is hardcoded and swim speed is the
    // depth-strider enchantment — so the unsettling kimono's reach bonus and the crocodile /
    // flying-fish swim bonus are both dropped on those two nodes.
    public static net.minecraft.world.entity.ai.attributes.Attribute swimSpeed() {
        return null;
    }

    public static net.minecraft.world.entity.ai.attributes.Attribute blockReach() {
        return null;
    }

    public static net.minecraft.world.entity.ai.attributes.Attribute entityReach() {
        return null;
    }
    *///?}

    /**
     * The mobGriefing gamerule, filtered through the loader's per-entity override event.
     * NeoForge renamed the hook to {@code canEntityGrief} in 1.20.5.
     */
    public static boolean mobGriefing(net.minecraft.world.level.Level level, Entity entity) {
        //? if forge && >=1.21.2 {
        /*return net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent((net.minecraft.server.level.ServerLevel) level, entity);
        *///?}
        //? if forge && <1.21.2 {
        return net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(level, entity);
        //?}
        //? if neoforge && <1.20.5 {
        /*return net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(level, entity);
        *///?}
        //? if neoforge && >=1.20.5 && <1.21.2 {
        /*return net.minecraftforge.event.ForgeEventFactory.canEntityGrief(level, entity);
        *///?}
        //? if neoforge && >=1.21.2 {
        /*return net.minecraftforge.event.ForgeEventFactory.canEntityGrief((net.minecraft.server.level.ServerLevel) level, entity);
        *///?}
        // Fabric API has no per-entity mobGriefing override hook, so the gamerule IS the answer —
        // which is also what both loaders' events default to when nothing overrides them.
        //? if fabric {
        /*return AMCompat.gameRule(level, AMCompat.Rule.MOB_GRIEFING);
        *///?}
    }

    /**
     * 1.20.2 dropped {@code MeleeAttackGoal#getAttackReachSqr}; melee reach is now decided by
     * {@code Mob#isWithinMeleeAttackRange}. Alex's Mobs relies on the old, wider formula for a
     * dozen goals, so it lives here (it is just the vanilla 1.20.1 body).
     */
    public static double attackReachSqr(net.minecraft.world.entity.Mob mob, net.minecraft.world.entity.LivingEntity target) {
        return mob.getBbWidth() * 2.0F * mob.getBbWidth() * 2.0F + target.getBbWidth();
    }

    /**
     * 1.20.3 replaced {@code BlockBehaviour.Properties.copy(Block)} with
     * {@code ofFullCopy(BlockBehaviour)}.
     */
    public static BlockBehaviour.Properties copyProperties(BlockBehaviour from) {
        //? if >=1.20.3
        //return BlockBehaviour.Properties.ofFullCopy(from);
        //? if <1.20.3
        return BlockBehaviour.Properties.copy((Block) from);
    }

    /**
     * 1.20.2 changed {@code LootItemConditions.orConditions} from a varargs array to a
     * {@code List}. Used by the four global loot modifiers.
     */
    public static <T> java.util.function.Predicate<T> orConditions(java.util.function.Predicate<T>[] conditions) {
        // 1.20.5 dropped the loot-specific helper; the generic one on Util does the same thing.
        //? if >=1.20.5
        //return net.minecraft.Util.anyOf(java.util.List.of(conditions));
        //? if >=1.20.2 && <1.20.5
        //return net.minecraft.world.level.storage.loot.predicates.LootItemConditions.orConditions(java.util.List.of(conditions));
        //? if <1.20.2
        return net.minecraft.world.level.storage.loot.predicates.LootItemConditions.orConditions(conditions);
    }

    /**
     * 1.20.2 gave {@code BucketPickup#pickupBlock} a (nullable) {@code Player} so bucket-empty
     * game events can be attributed. Alex's Mobs drains fluids without a player.
     */
    public static net.minecraft.world.item.ItemStack pickupBlock(net.minecraft.world.level.block.BucketPickup block, net.minecraft.world.level.LevelAccessor level, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        //? if >=1.20.2
        //return block.pickupBlock(null, level, pos, state);
        //? if <1.20.2
        return block.pickupBlock(level, pos, state);
    }

    /**
     * Resolves a loot table by id from a running server. Used only by {@link AMLootModifiers},
     * which has to identify loot tables by instance because Fabric has no equivalent of the
     * queried-table-id Forge patches onto {@code LootContext}.
     *
     * <p><b>The boundary is 1.20.5, not 1.21</b> — measured at 1.20.6, where {@code LootDataManager}
     * is already gone and {@code MinecraftServer.reloadableRegistries()} has replaced it. 1.21 is
     * the version that reworked most other registry access, so guessing it here would have compiled
     * on every node but 1.20.6 and left exactly one node calling a method that no longer exists.
     */
    public static net.minecraft.world.level.storage.loot.LootTable lootTableById(
            net.minecraft.server.MinecraftServer server, net.minecraft.resources.ResourceLocation id) {
        //? if >=1.20.5 {
        /*return server.reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.LOOT_TABLE, id));
        *///?} else {
        return server.getLootData().getElement(new net.minecraft.world.level.storage.loot.LootDataId<>(
                net.minecraft.world.level.storage.loot.LootDataType.TABLE, id));
        //?}
    }

    /**
     * 1.20.2 deleted {@code new AABB(BlockPos, BlockPos)} in favour of
     * {@code AABB.encapsulatingFullBlocks}.
     */
    public static net.minecraft.world.phys.AABB encapsulating(net.minecraft.core.BlockPos a, net.minecraft.core.BlockPos b) {
        //? if >=1.20.2
        //return net.minecraft.world.phys.AABB.encapsulatingFullBlocks(a, b);
        //? if <1.20.2
        return new net.minecraft.world.phys.AABB(a, b);
    }
}

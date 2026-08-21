package com.github.alexthe666.alexsmobs.fabric.event;

import com.github.alexthe666.alexsmobs.event.ServerEvents;
import com.github.alexthe666.alexsmobs.fabric.forge.event.TickEvent;
import com.github.alexthe666.alexsmobs.fabric.forge.event.entity.EntityEvent;
import com.github.alexthe666.alexsmobs.fabric.forge.event.entity.EntityStruckByLightningEvent;
import com.github.alexthe666.alexsmobs.fabric.forge.event.entity.ProjectileImpactEvent;
import com.github.alexthe666.alexsmobs.fabric.forge.event.entity.living.LivingAttackEvent;
import com.github.alexthe666.alexsmobs.fabric.forge.event.entity.living.LivingChangeTargetEvent;
import com.github.alexthe666.alexsmobs.fabric.forge.event.entity.living.LivingDamageEvent;
import com.github.alexthe666.alexsmobs.fabric.forge.event.entity.living.LivingDropsEvent;
import com.github.alexthe666.alexsmobs.fabric.forge.event.entity.living.LivingEntityUseItemEvent;
import com.github.alexthe666.alexsmobs.fabric.forge.event.entity.living.LivingEvent;
import com.github.alexthe666.alexsmobs.fabric.forge.event.entity.living.MobSpawnEvent;
import com.github.alexthe666.alexsmobs.fabric.forge.event.entity.player.AttackEntityEvent;
import com.github.alexthe666.alexsmobs.fabric.forge.event.entity.player.PlayerEvent;
import com.github.alexthe666.alexsmobs.fabric.forge.event.entity.player.PlayerInteractEvent;
import com.github.alexthe666.alexsmobs.fabric.forge.eventbus.api.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;

/**
 * The Fabric half of {@link ServerEvents}: it fires the Forge event objects that
 * {@code fabric/forge/**} stands in for, from Fabric API callbacks and from this mod's own mixins.
 *
 * <p>On Forge and NeoForge the loader scans {@code ServerEvents} for {@code @SubscribeEvent} and
 * routes events by parameter type. Fabric has no bus, so <b>every hook is wired by hand here</b> and
 * the annotation is inert. That is the whole reason {@code ServerEvents} was made to compile before
 * anything fired it: the compiler now pins each handler's signature, so a hook that is wired wrongly
 * is a build error rather than a mob that quietly stops reacting.
 *
 * <p><b>One handler per hook</b>, so there is nothing to order and {@code EventPriority} is ignored.
 *
 * <h2>Cancellation</h2>
 * Forge's interaction events carry a cancel flag <i>and</i> a {@code cancellationResult}; Fabric's
 * callbacks express both by returning a non-{@code PASS} {@link InteractionResult}. {@link #result}
 * is the single translation point — return {@code PASS} when the handler did not cancel, so vanilla
 * (and every other mod's callback) still runs.
 */
public final class FabricServerEvents {

    /**
     * The same single instance the other two loaders register on the game bus. Held statically
     * because the mixin-fired hooks have no other way to reach it, and because {@code ServerEvents}
     * keeps per-level state (the beached-whale spawner map) that must not be duplicated.
     */
    private static final ServerEvents HANDLER = new ServerEvents();

    private FabricServerEvents() {
    }

    /**
     * Called from {@code AlexsMobsFabric#onInitialize}, after {@code new AlexsMobs()} — the handler
     * bodies dereference registry objects, and registering the callbacks before those exist would
     * only widen the window in which one could fire against a half-built registry.
     */
    public static void init() {
        registerTick();
        registerPlayerLogin();
        registerInteractions();
        registerBlockBreak();
        registerCommands();
    }

    /**
     * Fabric's route to {@code /amc config}. Forge and NeoForge get the same tree from
     * {@code ServerEvents#onRegisterCommands}, a game-bus event this loader has no equivalent of.
     *
     * <p>Unlike {@code /shieldpose} — a client command whose source type is the one thing that
     * differs per loader — this is a server command, so all three loaders hand out a
     * {@code CommandDispatcher<CommandSourceStack>} and the call is identical on each. The
     * callback's three-argument shape has been stable in fabric-command-api-v2 since MC 1.19, so
     * no Stonecutter arm is needed across the 1.20.1→26.2 range.
     */
    private static void registerCommands() {
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) ->
                        com.github.alexthe666.alexsmobs.command.AMConfigCommand.register(dispatcher));
    }

    // ── Block break ────────────────────────────────────────────────────────────
    // Not a ServerEvents hook at all: on Forge and NeoForge the transmutation table's
    // explode-on-break is an override of the loader's Block#onDestroyedByPlayer, which vanilla has
    // no equivalent of. Fabric API's PlayerBlockBreakEvents is the nearest thing, and it is
    // server-side only — which is what an explosion wants anyway.

    private static void registerBlockBreak() {
        net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.AFTER.register(
                (level, player, pos, state, blockEntity) -> {
                    if (state.getBlock() instanceof com.github.alexthe666.alexsmobs.block.BlockTransmutationTable) {
                        com.github.alexthe666.alexsmobs.block.BlockTransmutationTable
                                .fabricOnDestroyedByPlayer(level, pos);
                    }
                });
    }

    // ── Level tick ─────────────────────────────────────────────────────────────
    // Forge posts LevelTickEvent in BOTH phases and this mod's listener never filtered on phase, so
    // its body runs twice per level per tick there. The NeoForge and Forge >=1.21.9 arms of
    // ServerEvents subscribe Pre AND Post for exactly that reason; Fabric matches by registering
    // both halves. Firing once would silently halve the beached-cachalot spawn rate and add up to a
    // tick of latency to the ender-residue teleport queue.

    private static void registerTick() {
        //? if >=26 {
        /*ServerTickEvents.START_LEVEL_TICK.register(level -> HANDLER.onServerTick(new TickEvent.LevelTickEvent(level)));
        ServerTickEvents.END_LEVEL_TICK.register(level -> HANDLER.onServerTick(new TickEvent.LevelTickEvent(level)));
        *///?} else {
        // Renamed to START_LEVEL_TICK/END_LEVEL_TICK in the fabric-api that ships with 26.1.
        ServerTickEvents.START_WORLD_TICK.register(level -> HANDLER.onServerTick(new TickEvent.LevelTickEvent(level)));
        ServerTickEvents.END_WORLD_TICK.register(level -> HANDLER.onServerTick(new TickEvent.LevelTickEvent(level)));
        //?}
    }

    // ── Player login ───────────────────────────────────────────────────────────
    // JOIN fires once the play connection is ready, which is later than Forge's PlayerLoggedInEvent
    // but is the first point at which the player can be given an item — and handing out the Animal
    // Dictionary is all this handler does.

    private static void registerPlayerLogin() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                HANDLER.onPlayerLoggedIn(new PlayerEvent.PlayerLoggedInEvent(handler.player)));
    }

    // ── Interactions ───────────────────────────────────────────────────────────
    // Fabric's four interaction callbacks line up with Forge's phases one-for-one, and all four fire
    // on both sides before vanilla acts — the same position Forge's do.
    //
    // NOT wired here: LeftClickEmpty and RightClickEmpty. Forge fires those from the CLIENT's
    // swing/use handling, so they are registered from FabricClientEvents instead and arrive at
    // {@link #fireEmptyLeftClick} below. RightClickEmpty is still unfired — see docs/notes/fabric.md.

    private static void registerInteractions() {
        //? if >=1.21.2 {
        /*UseItemCallback.EVENT.register((player, level, hand) -> {
            PlayerInteractEvent.RightClickItem event = new PlayerInteractEvent.RightClickItem(player, hand);
            HANDLER.onUseItem(event);
            return result(event);
        });
        *///?} else {
        // Below 1.21.2 the callback returns the item alongside the result.
        UseItemCallback.EVENT.register((player, level, hand) -> {
            PlayerInteractEvent.RightClickItem event = new PlayerInteractEvent.RightClickItem(player, hand);
            HANDLER.onUseItem(event);
            return new net.minecraft.world.InteractionResultHolder<>(result(event), player.getItemInHand(hand));
        });
        //?}

        UseEntityCallback.EVENT.register((player, level, hand, target, hit) -> {
            PlayerInteractEvent.EntityInteract event = new PlayerInteractEvent.EntityInteract(player, hand, target);
            HANDLER.onInteractWithEntity(event);
            return result(event);
        });

        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            PlayerInteractEvent.RightClickBlock event =
                    new PlayerInteractEvent.RightClickBlock(player, hand, hit.getBlockPos(), hit.getDirection());
            HANDLER.onUseItemOnBlock(event);
            return result(event);
        });

        AttackEntityCallback.EVENT.register((player, level, hand, target, hit) -> {
            // This handler never cancels — it knocks back with the moose headgear and sics tigers on
            // the target — so the result is always PASS and vanilla's attack proceeds.
            HANDLER.onPlayerAttackEntityEvent(new AttackEntityEvent(player, target));
            return InteractionResult.PASS;
        });
    }

    /**
     * Swinging at nothing — Forge's {@code LeftClickEmpty}, and the only thing that runs this mod's
     * {@link com.github.alexthe666.alexsmobs.item.ILeftClick} items: the falconry glove (which is how
     * a bald eagle or potoo is launched off your arm, and the whole of the mob's falconry loop) and
     * the tendon whip. Neither did anything at all on Fabric until this was wired.
     *
     * <p>Called from {@code FabricClientEvents}, not from {@link #registerInteractions}: Forge fires
     * this one from {@code Minecraft#startAttack} on the client only, and the handler's own tail
     * sends {@code MessageSwingArm} so the server runs the same item hook. Firing it here as well
     * would run the launch twice.
     *
     * <p>Hand is always MAIN_HAND, as Forge's is; the handler reads both hands off the player.
     */
    public static void fireEmptyLeftClick(Player player) {
        HANDLER.onPlayerLeftClick(new PlayerInteractEvent.LeftClickEmpty(player, InteractionHand.MAIN_HAND));
    }

    /**
     * Forge's "cancelled, and here is what the interaction should report" pair, expressed the way
     * Fabric wants it. An uncancelled event must come back {@code PASS} even if something set a
     * cancellation result, or the interaction is swallowed and never reaches vanilla.
     */
    private static InteractionResult result(PlayerInteractEvent event) {
        return event.isCanceled() ? event.getCancellationResult() : InteractionResult.PASS;
    }

    // ══ Mixin-fired hooks ═══════════════════════════════════════════════════════
    //
    // Everything below has no Fabric API callback at all, so `mixin/fabric/**` calls in here from
    // the exact vanilla method Forge patches. Each entry point is deliberately thin — construct the
    // event, hand it to the one handler, report back whether it was cancelled — so that the mixin
    // stays a pure "where", the handler stays the only "what", and neither file has to know about
    // the other's MC-version splits. The injection points and their per-era descriptors are listed
    // in docs/notes/fabric.md; the classes themselves are compiled only on Fabric and pruned out of
    // alexsmobs.mixins.json everywhere else.
    //
    // Every `fireX` that can be vetoed returns `true` when the handler cancelled, and the caller
    // turns that into the right vanilla refusal (ci.cancel(), or a false return value).

    /**
     * {@code LivingEntity#tick} — the mod's busiest hook, and the one whose absence was most
     * visible on Fabric. Fires on <b>both</b> sides, exactly as Forge's does: several of the
     * behaviours it drives (rainbow tint, clinging, shoulder mobs) are client-visual.
     */
    public static void fireLivingTick(LivingEntity entity) {
        HANDLER.onLivingUpdateEvent(new LivingEvent.LivingTickEvent(entity));
    }

    /**
     * {@code LivingEntity#hurt} / {@code #hurtServer} — pre-mitigation. The mod's handler only adds
     * the Shield of the Deep's counter-effects and never cancels, but the veto is wired anyway so
     * the stub's shape stays honest.
     */
    public static boolean fireLivingAttack(LivingEntity entity, DamageSource source, float amount) {
        LivingAttackEvent event = new LivingAttackEvent(entity, source, amount);
        HANDLER.onLivingAttack(event);
        return event.isCanceled();
    }

    /**
     * {@code LivingEntity#actuallyHurt} — post-mitigation, the amount that is about to be applied.
     *
     * <p>⚠️ {@code setAmount} is <b>not</b> read back: an {@code @Inject} cannot rewrite the target's
     * float parameter, and this mod only ever cancels here (mimic octopus, emu leggings) or reads
     * the amount (Soulsteal). If a future handler starts rescaling, this needs a
     * {@code @ModifyVariable} beside the inject, not a wider event.
     */
    public static boolean fireLivingDamage(LivingEntity entity, DamageSource source, float amount) {
        LivingDamageEvent event = new LivingDamageEvent(entity, source, amount);
        HANDLER.onLivingDamageEvent(event);
        return event.isCanceled();
    }

    /**
     * {@code LivingEntity#completeUsingItem} — a chorus fruit finishing its use is what clears
     * Ender Flu. The stack must be read <i>before</i> vanilla consumes it, so the mixin injects at
     * HEAD and passes a copy.
     */
    public static void fireUseItemFinish(LivingEntity entity, ItemStack item) {
        HANDLER.onItemUseLast(new LivingEntityUseItemEvent.Finish(entity, item));
    }

    /**
     * {@code LivingEntity#dropAllDeathLoot} — Forge hands the listener a live drop list that it
     * spawns afterwards; there is no such list on Fabric, so this passes an empty one and spawns
     * whatever the handler appended. Same net effect for the one listener there is (a lassoed mob
     * dropping its Vine Lasso back).
     */
    public static void fireLivingDrops(LivingEntity entity, DamageSource source) {
        List<ItemEntity> drops = new ArrayList<>();
        HANDLER.onEntityDrops(new LivingDropsEvent(entity, source, drops, 0));
        for (ItemEntity drop : drops) {
            entity.level().addFreshEntity(drop);
        }
    }

    /**
     * {@code Mob#setTarget} — Bug Pheromones and the Unsettling Kimono cancel it. Cancelling means
     * the target is simply not assigned, which is what Forge's veto does too.
     */
    public static boolean fireChangeTarget(Mob mob, LivingEntity newTarget) {
        LivingChangeTargetEvent event = new LivingChangeTargetEvent(mob, newTarget);
        HANDLER.onLivingSetTargetEvent(event);
        return event.isCanceled();
    }

    /**
     * {@code Mob#checkDespawn} — a mob under Debilitating Sting II must not escape by despawning.
     *
     * <p>Returns {@code true} for Forge's {@code Result.DENY}. The caller cancels the whole
     * {@code checkDespawn}, which is stronger than Forge's veto on paper (it also skips the
     * persistence bookkeeping vanilla does in the same method) but identical in effect: the only
     * thing that method can do to a mob it does not despawn is reset {@code noActionTime}, and a
     * stung mob is by definition being kept alive on purpose.
     */
    public static boolean fireAllowDespawn(Mob mob) {
        if (!(mob.level() instanceof ServerLevelAccessor level)) {
            return false;
        }
        MobSpawnEvent.AllowDespawn event = new MobSpawnEvent.AllowDespawn(mob, level);
        HANDLER.onEntityDespawnAttempt(event);
        return event.getResult() == Event.Result.DENY;
    }

    /**
     * {@code Mob#finalizeSpawn} — the elephant-riding wandering trader and the six config-gated
     * extra goals on vanilla mobs.
     *
     * <p>Fired at RETURN so a subclass override that delegates up (which is nearly all of them) has
     * already run its own setup. Forge instead patches the <i>call sites</i>, so a mob whose class
     * overrides {@code finalizeSpawn} without calling {@code super} is skipped here and would not be
     * on Forge — no vanilla mob this handler touches does that, and it is recorded as a divergence.
     */
    public static void fireFinalizeSpawn(Mob mob, ServerLevelAccessor level) {
        HANDLER.onEntityFinalizeSpawn(new MobSpawnEvent.FinalizeSpawn(mob, level));
    }

    /**
     * {@code Entity#thunderHit} — a struck squid becomes a Giant Squid, and the handler cancels so
     * vanilla does not also set the original on fire.
     */
    public static boolean fireStruckByLightning(Entity entity, LightningBolt bolt) {
        EntityStruckByLightningEvent event = new EntityStruckByLightningEvent(entity, bolt);
        HANDLER.onStruckByLightning(event);
        return event.isCanceled();
    }

    /**
     * {@code Projectile#onHit} — the emu's dodge. Cancelling means the projectile keeps flying,
     * which is what Forge's old cancel (and the newer {@code SKIP_ENTITY} impact result) mean.
     */
    public static boolean fireProjectileImpact(Entity projectile, HitResult ray) {
        ProjectileImpactEvent event = new ProjectileImpactEvent(projectile, ray);
        HANDLER.onProjectileHit(event);
        return event.isCanceled();
    }

    /**
     * {@code Player#hasCorrectToolForDrops} — the ghostly pickaxe stores what it mines inside the
     * underminer's ghost instead of dropping it, and does that by answering "no" here.
     *
     * <p>Mixed into the method rather than into {@code ServerPlayerGameMode#destroyBlock}, because
     * that is what both other loaders do: Forge and NeoForge patch the body of this very method to
     * call their {@code doPlayerHarvestCheck}, so every caller — drops, mining speed, the client's
     * break-progress preview — sees the same answer. Redirecting one call site would leave the
     * others disagreeing with it.
     */
    public static boolean fireHarvestCheck(net.minecraft.world.entity.player.Player player, boolean canHarvest) {
        PlayerEvent.HarvestCheck event = new PlayerEvent.HarvestCheck(player, canHarvest);
        HANDLER.onHarvestCheck(event);
        return event.canHarvest();
    }

    // Forge deleted EntityEvent.Size in 1.20.2, so the handler behind this is gated <1.20.2 in
    // ServerEvents and 1.20.1-fabric is the only node that has it. Naming it unconditionally here
    // would not compile anywhere else.
    //? if <1.20.2 {
    /**
     * {@code Entity#refreshDimensions} — a clinging player's eyes move to the bottom of the hitbox
     * so the upside-down view lines up. Called at TAIL, so the "old" values it is handed are the
     * ones vanilla has just computed; the caller writes {@link EntityEvent.Size#getNewEyeHeight()}
     * back over the field.
     */
    public static float fireEntitySize(Entity entity, net.minecraft.world.entity.EntityDimensions size, float eyeHeight) {
        EntityEvent.Size event = new EntityEvent.Size(entity, size, eyeHeight);
        HANDLER.onEntityResize(event);
        return event.getNewEyeHeight();
    }
    //?}
}

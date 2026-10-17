package com.github.alexthe666.alexsmobs.fabric.forge.eventbus.api;

/**
 * Fabric stand-in for {@code net.minecraftforge.eventbus.api.Event} — the base class of every event
 * {@code event/ServerEvents} and {@code client/event/ClientEvents} <b>listen to</b>.
 *
 * <p>⚠️ Not to be confused with {@code fabric/event/AMEvent}, which is the base of the handful of
 * events this mod <b>publishes</b> (Citadel's four client hooks and {@code AnimationEvent}). That
 * one exists because the mod posts them; this one exists because the mod receives them. They are
 * deliberately separate types: {@code AMEvent} is shaped after EventBus 7's {@code MutableEvent}
 * and is posted through {@code AMEventBus}, whereas everything here is constructed by
 * {@code fabric/event/FabricServerEvents} from a Fabric API callback or a mixin and handed to one
 * handler method directly. Merging them would tie the mod's own event shape to Forge's.
 *
 * <p><b>Why stub Forge's hierarchy at all rather than rewrite the handlers.</b> The two event files
 * are ~1,900 lines of game logic against ~36 distinct hooks, and every line of it is
 * loader-neutral once the event object has been unpacked — the handlers are already thin adapters
 * over private logic methods ({@code onServerTick(tick) -> onLevelTick(tick.level)}). Reproducing
 * that logic on Fabric would put every future bug fix on two axes at once. Reproducing the ~40
 * accessors it reads is a few hundred lines of data holder that cannot drift, because the compiler
 * checks it against the same call sites the other two loaders use. Same reasoning, and the same
 * relocated-compat-namespace pattern, as {@code fabric/registries/DeferredRegister} and
 * {@code fabric/registries/DeferredRegister}.
 *
 * <p><b>What is deliberately missing:</b> {@code @Cancelable}. Forge uses it to reject
 * {@code setCanceled} on events that are not cancellable, which is a bus-side check with no bus
 * here; the handlers only ever call it on events that really are cancellable, so enforcing it would
 * buy nothing and cost an annotation-processing story. {@link #setCanceled} is therefore available
 * on every event, and the caller that reads it back is the dispatcher that constructed it.
 */
public class Event {

    /**
     * Forge's tri-state "veto / abstain / force" answer, used here by exactly one listener —
     * {@code MobSpawnEvent.AllowDespawn}, which returns {@link Result#DENY} to keep a mob loaded.
     *
     * <p>Forge 1.21.6 moved this to {@code net.minecraftforge.common.util.Result} and the
     * {@code !fg2106-eb-result} rule fully-qualifies it there; on Fabric the nested spelling is the
     * one in the source text, so it lives here.
     */
    public enum Result {
        DENY,
        DEFAULT,
        ALLOW,
    }

    private boolean canceled;
    private Result result = Result.DEFAULT;

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }
}

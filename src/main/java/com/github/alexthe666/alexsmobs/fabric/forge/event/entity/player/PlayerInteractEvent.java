package com.github.alexthe666.alexsmobs.fabric.forge.event.entity.player;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Fabric stand-in for {@code net.minecraftforge.event.entity.player.PlayerInteractEvent}. Five of
 * its six phases are used here; only {@code LeftClickBlock} is not.
 *
 * <p>Forge's real class carries the whole interaction context on the base and lets each phase
 * populate the parts that apply, which is why {@code getPos()}/{@code getFace()} live here and read
 * as null for the "empty" phases. That shape is kept deliberately — the handlers were written
 * against it, and diverging would mean editing call sites rather than adding a type.
 *
 * <p>{@link #setCancellationResult} is the "and here is the result the interaction should report"
 * companion to cancelling. On Fabric the dispatcher returns it from the callback it fired inside;
 * a cancelled interaction that reported {@code PASS} would fall through to the next hand.
 */
public class PlayerInteractEvent extends PlayerEvent {

    private final InteractionHand hand;
    private final BlockPos pos;
    private final Direction face;
    private InteractionResult cancellationResult = InteractionResult.PASS;

    public PlayerInteractEvent(Player player, InteractionHand hand, BlockPos pos, Direction face) {
        super(player);
        this.hand = hand;
        this.pos = pos;
        this.face = face;
    }

    public InteractionHand getHand() {
        return hand;
    }

    /** The targeted block, or null for the phases that have no block (the two "empty" clicks). */
    public BlockPos getPos() {
        return pos;
    }

    /** The targeted face, or null when there is no block or the face is unknown. */
    public Direction getFace() {
        return face;
    }

    public Level getLevel() {
        return getEntity().level();
    }

    public ItemStack getItemStack() {
        return getEntity().getItemInHand(hand);
    }

    public InteractionResult getCancellationResult() {
        return cancellationResult;
    }

    public void setCancellationResult(InteractionResult result) {
        this.cancellationResult = result;
    }

    /** Swinging at nothing. Used to run {@link com.github.alexthe666.alexsmobs.item.ILeftClick} items. */
    public static class LeftClickEmpty extends PlayerInteractEvent {

        public LeftClickEmpty(Player player, InteractionHand hand) {
            super(player, hand, null, null);
        }
    }

    /** Right-clicking with an item, not aimed at a block or entity — the lava-bottle fill and the elephant-charge wheat. */
    public static class RightClickItem extends PlayerInteractEvent {

        public RightClickItem(Player player, InteractionHand hand) {
            super(player, hand, null, null);
        }
    }

    /** Right-clicking nothing at all — how a rainbow sponge is wrung out. */
    public static class RightClickEmpty extends PlayerInteractEvent {

        public RightClickEmpty(Player player, InteractionHand hand) {
            super(player, hand, null, null);
        }
    }

    /** Right-clicking a block — the April Fools sand-circle stick. */
    public static class RightClickBlock extends PlayerInteractEvent {

        public RightClickBlock(Player player, InteractionHand hand, BlockPos pos, Direction face) {
            super(player, hand, pos, face);
        }
    }

    /** Right-clicking an entity — how a lassoed mob is released. */
    public static class EntityInteract extends PlayerInteractEvent {

        private final Entity target;

        public EntityInteract(Player player, InteractionHand hand, Entity target) {
            super(player, hand, target.blockPosition(), null);
            this.target = target;
        }

        public Entity getTarget() {
            return target;
        }
    }

    /**
     * Right-clicking an entity, in the earlier {@code INTERACT_AT} phase — Forge and NeoForge fire
     * this one before {@code Entity#interactAt} and {@link EntityInteract} only afterwards, so a
     * mod that consumes the click there (MCA Reborn's villagers do) hides the later event.
     *
     * <p><b>Never fired on Fabric</b>, deliberately. {@code UseEntityCallback} already runs ahead
     * of {@code Entity#interactAt} for both packet actions, so {@link EntityInteract} is dispatched
     * early enough on its own and firing this too would apply every handler twice. The type exists
     * so the shared handler in {@code ServerEvents} compiles here.
     */
    public static class EntityInteractSpecific extends PlayerInteractEvent {

        private final Entity target;

        public EntityInteractSpecific(Player player, InteractionHand hand, Entity target) {
            super(player, hand, target.blockPosition(), null);
            this.target = target;
        }

        public Entity getTarget() {
            return target;
        }
    }
}

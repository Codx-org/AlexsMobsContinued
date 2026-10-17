package com.github.alexthe666.alexsmobs.fabric.forge.event.entity.player;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Fabric stand-in for {@code net.minecraftforge.event.entity.player.PlayerDestroyItemEvent} — the
 * Ghostly Pickaxe spilling its stored contents at the moment it breaks.
 *
 * <p>⚠️ <b>Unreachable on Fabric today.</b> Its handler sits in a {@code forge && >=1.21} arm: only
 * Forge 1.21+ needed it, because that is where {@code IForgeItem#damageItem} — the hook the mod
 * originally used — was deleted. Every other loader/version still spills the inventory from
 * {@code damageItem}. The stub exists so the type resolves if that arm is ever widened, and so the
 * Fabric dispatcher has something to fire if Fabric is given the same treatment.
 *
 * <p>{@link #getOriginal()} is the stack <i>as it was before</i> being destroyed — by the time this
 * fires the player's slot holds {@code ItemStack.EMPTY}, so the contents can only be read from here.
 */
public class PlayerDestroyItemEvent extends PlayerEvent {

    private final ItemStack original;
    private final InteractionHand hand;

    public PlayerDestroyItemEvent(Player player, ItemStack original, InteractionHand hand) {
        super(player);
        this.original = original;
        this.hand = hand;
    }

    public ItemStack getOriginal() {
        return original;
    }

    /** The hand the item was in, or null if it broke in an armour/other slot. */
    public InteractionHand getHand() {
        return hand;
    }
}

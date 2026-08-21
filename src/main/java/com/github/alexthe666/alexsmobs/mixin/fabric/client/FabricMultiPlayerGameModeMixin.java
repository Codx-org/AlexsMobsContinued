package com.github.alexthe666.alexsmobs.mixin.fabric.client;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.message.MessageHurtMultipart;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The client half of the multipart plumbing: once {@code FabricLevelMultipartMixin} makes part
 * hitboxes pickable, the player can aim at one — but the vanilla attack packet cannot carry it,
 * because the part's entity id does not exist server-side (parts live in no level's entity storage).
 *
 * <p>So intercept the attack, cancel it, and report the hit through {@code MessageHurtMultipart}
 * with the <b>parent's</b> networked id. The server-side branch that consumes this is gated
 * {@code //? if fabric} in that message's handler, and runs a full vanilla player attack on the
 * parent — which is why nothing here sends a damage number. Cooldown, knockback, enchantments and
 * sweep are all decided server-side, and no client-supplied damage value is trusted.
 *
 * <p>{@code part.getId()} below is only readable at all because {@code AMCompat.assignClientPartId}
 * hands every client-side part one in its constructor. 26.2 stopped assigning ids client-side and made
 * {@code Entity#getId()} throw at id 0, and a part never gets a spawn packet — this line was the crash
 * players reported. Do not "simplify" that away; see the multipart section of {@code bug-reports.md}.
 * The value itself stays server-meaningless, as it has always been: the handler's lookup of it misses
 * and the {@code //? if fabric} branch works off the parent id.
 *
 * <p>Ported from AlexsMobsFP's {@code MultiPlayerGameModeMixin}.
 * {@code MultiPlayerGameMode#attack(Player, Entity)V} is byte-identical on all 17 Fabric nodes, so
 * there are no era arms.
 *
 * <p>The type is spelled fully qualified rather than imported. {@code PartEntity} is rewritten by
 * the {@code !fab-partentity} replacement rule, and the same reasoning behind rule 5 applies to any
 * replaced name: an import is the one place where a silent retarget would not fail to compile.
 */
@Mixin(MultiPlayerGameMode.class)
public class FabricMultiPlayerGameModeMixin {

    @Inject(
            method = "attack(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void alexsmobs$attackMultipart(Player player, Entity target, CallbackInfo ci) {
        if (target instanceof com.github.alexthe666.alexsmobs.fabric.entity.PartEntity<?> part
                && part.getParent() != null) {
            AlexsMobs.sendMSGToServer(
                    new MessageHurtMultipart(part.getId(), part.getParent().getId(), 0.0F, ""));
            player.swing(InteractionHand.MAIN_HAND);
            ci.cancel();
        }
    }
}

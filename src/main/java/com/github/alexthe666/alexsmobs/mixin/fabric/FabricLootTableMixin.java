package com.github.alexthe666.alexsmobs.mixin.fabric;

import com.github.alexthe666.alexsmobs.misc.AMLootModifiers;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Runs this mod's four global loot modifiers on Fabric — bananas from jungle leaves, acacia
 * blossoms from acacia leaves, ancient darts in jungle-temple chests, and pigshoes from piglin
 * bartering. Forge and NeoForge get all four from a datapack; see {@link AMLootModifiers} for why
 * that mechanism has no counterpart here.
 *
 * <h2>Why this injection point</h2>
 *
 * <p>{@code getRandomItems(LootContext)} is <b>private</b>, and deliberately targeted anyway: it is
 * the single point the three call paths that matter converge on. Measured on 1.20.1 and unchanged
 * since — {@code getRandomItems(LootParams)}, {@code getRandomItems(LootParams, long)} and
 * {@code fill(Container, LootParams, long)} all delegate to it, which is respectively how block
 * drops, piglin bartering and chest loot are rolled. The public {@code Consumer}-taking overloads
 * do <b>not</b>; they reach {@code getRandomItemsRaw} directly.
 *
 * <p><b>That is a real limit, not an oversight.</b> Entity death drops go through the
 * {@code Consumer} path, so a modifier targeting an entity loot table would not fire here even
 * though the equivalent Forge modifier would. None of the four does — all four target block, chest
 * and gameplay tables. A fifth one that targets an entity table needs a second injection into
 * {@code getRandomItemsRaw}, and would then need care not to double-apply, since the private
 * method below funnels into it.
 *
 * <p>The descriptor is byte-identical on all 17 Fabric nodes, 1.20.1 through 26.2 (checked, rule
 * 10), so no arms.
 *
 * <h2>Why not Fabric API</h2>
 *
 * <p>{@code LootTableEvents.MODIFY_DROPS} is the natural fit and is what {@code AlexsMobsFP} uses,
 * but it <b>only exists from 1.21.6</b> — measured across all 17 pinned {@code fabric-api}
 * versions. Below that there is no drop-time hook at all: {@code MODIFY} fires when a table is
 * loaded and hands out a builder, which cannot express "roll a fortune-scaled chance against the
 * tool that broke this block". Nor is there one API version that spans the range: {@code loot v2}
 * is gone by 26.1.2 and {@code v3} is absent before 1.21. Fabric API's own implementation is also
 * a {@code LootTable} mixin, but reaches its injection point with MixinExtras
 * {@code @WrapOperation}, which this tree bundles nowhere on purpose.
 */
@Mixin(LootTable.class)
public class FabricLootTableMixin {

    /**
     * Resolution is cached per table instance because it costs four server-side registry lookups,
     * and every loot roll in the game passes through here. Two fields rather than one so that
     * "resolved to nothing" — overwhelmingly the common answer — is cached too.
     *
     * <p>No invalidation is needed: {@code /reload} constructs new {@code LootTable} objects, so
     * these fields cannot outlive the tables they describe.
     */
    @Unique
    private boolean alexsmobs$modifierResolved;

    @Unique
    private AMLootModifiers.Appender alexsmobs$modifier;

    @Inject(method = "getRandomItems(Lnet/minecraft/world/level/storage/loot/LootContext;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;",
            at = @At("RETURN"), cancellable = true)
    private void alexsmobs$applyGlobalLootModifiers(LootContext context,
                                                    CallbackInfoReturnable<ObjectArrayList<ItemStack>> cir) {
        if (!this.alexsmobs$modifierResolved) {
            this.alexsmobs$modifierResolved = true;
            this.alexsmobs$modifier = AMLootModifiers.resolve((LootTable) (Object) this, context);
        }
        if (this.alexsmobs$modifier != null) {
            // setReturnValue rather than mutating in place: all four appenders happen to add to the
            // list they were handed and return it, but the contract they inherit from Forge's
            // IGlobalLootModifier is that the returned list is the result, and one of them growing
            // a copy later should not quietly stop working.
            cir.setReturnValue(this.alexsmobs$modifier.append(cir.getReturnValue(), context));
        }
    }
}

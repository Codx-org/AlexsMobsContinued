package com.github.alexthe666.alexsmobs.fabric.common.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;

/**
 * Fabric stand-in for {@code net.minecraftforge.common.loot.IGlobalLootModifier}, reached by the
 * Fabric-only {@code !fab-lootmodifier} replacement rule.
 *
 * <p>The four modifiers in this mod ({@code Banana}, {@code Blossom}, {@code AncientDart},
 * {@code Pigshoes}) implement it directly rather than extending Forge's {@code LootModifier}, and
 * each supplies its own {@code conditions} array plus an {@code apply} that tests them before
 * delegating to {@code doApply}. So the only members they actually need from the interface are the
 * two below — everything else Forge declares ({@code DIRECT_CODEC}, {@code getJson}) exists purely
 * to serve Forge's own {@code global_loot_modifiers.json} dispatch, which has no Fabric analogue.
 *
 * <p><b>All four now run on Fabric</b>, but not through this interface. Forge/NeoForge drive them
 * from a serializer registry plus that json file; vendoring the interface kept all four modifiers —
 * and their {@code doApply} bodies, which are the actual behaviour — compiling and ready to be
 * wired up, instead of gating them out and losing them. That wiring is
 * {@link com.github.alexthe666.alexsmobs.misc.AMLootModifiers}, driven by
 * {@code mixin/fabric/FabricLootTableMixin}, and it calls {@code doApply} directly.
 *
 * <p>It deliberately does <b>not</b> call {@link #apply}: every condition in the four jsons is a
 * {@code forge:loot_table_id} test, a condition type that cannot even be deserialized here, so a
 * Fabric-side modifier is necessarily built with an empty {@code conditions} array — and an empty
 * or-of-conditions is always false. Going through {@code apply} would therefore compile, register,
 * run, and drop every item. The table identity those conditions were testing is instead answered
 * by the map key in {@code AMLootModifiers}, before the appender is ever reached.
 *
 * <p>{@code apply} is declared with the pre-26 two-argument shape because that is what the Fabric
 * arm of each modifier sees: the {@code LootTable} parameter Forge 26 added is inserted by a
 * {@code forge}-gated replacement rule, so it never reaches this loader.
 */
public interface IGlobalLootModifier {

    /**
     * Identical to Forge's, and deliberately so — it decodes the same {@code "conditions"} array
     * out of the same json the other two loaders read, so a modifier's codec is loader-neutral.
     */
    // Four arms because vanilla moved the single-condition codec three times, and below 1.20.2 there
    // was no codec at all — conditions were Gson-serialized. Each arm is what Forge's own
    // IGlobalLootModifier uses on that era, so the json a modifier reads is identical on every node.
    //? if >=1.21 {
    /*Codec<LootItemCondition[]> LOOT_CONDITIONS_CODEC =
            LootItemCondition.DIRECT_CODEC.listOf().xmap(list -> list.toArray(LootItemCondition[]::new), List::of);
    *///?} elif >=1.20.5 {
    /*// 1.21 hoisted DIRECT_CODEC off the LootItemConditions registry class onto the interface itself.
    // Same codec, same json.
    Codec<LootItemCondition[]> LOOT_CONDITIONS_CODEC =
            net.minecraft.world.level.storage.loot.predicates.LootItemConditions.DIRECT_CODEC.listOf()
                    .xmap(list -> list.toArray(LootItemCondition[]::new), List::of);
    *///?} elif >=1.20.2 {
    /*// 1.20.5 wrapped LootItemConditions.CODEC in a Holder and renamed the unwrapped one DIRECT_CODEC.
    // Below that boundary CODEC *is* the direct one.
    Codec<LootItemCondition[]> LOOT_CONDITIONS_CODEC =
            net.minecraft.world.level.storage.loot.predicates.LootItemConditions.CODEC.listOf()
                    .xmap(list -> list.toArray(LootItemCondition[]::new), List::of);
    *///?} else {
    // 1.20.1 predates codec-based loot conditions entirely — LootItemConditions registers Gson
    // Serializers, not Codecs. Forge bridges the two by round-tripping through a JsonElement, and
    // this is that bridge copied verbatim, so the "conditions" array parses to the same thing here.
    Codec<LootItemCondition[]> LOOT_CONDITIONS_CODEC = net.minecraft.util.ExtraCodecs.JSON.xmap(
            json -> net.minecraft.world.level.storage.loot.Deserializers.createConditionSerializer().create()
                    .fromJson(json, LootItemCondition[].class),
            conditions -> net.minecraft.world.level.storage.loot.Deserializers.createConditionSerializer().create()
                    .toJsonTree(conditions));
    //?}

    ObjectArrayList<ItemStack> apply(ObjectArrayList<ItemStack> generatedLoot, LootContext context);

    // 1.20.5 turned every serializer registry MapCodec-based; below it the four modifiers' codec()
    // overrides return a plain Codec, so the declaration has to change shape with them.
    //? if >=1.20.5 {
    /*MapCodec<? extends IGlobalLootModifier> codec();
    *///?} else {
    Codec<? extends IGlobalLootModifier> codec();
    //?}
}

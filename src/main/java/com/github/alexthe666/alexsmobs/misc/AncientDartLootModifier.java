package com.github.alexthe666.alexsmobs.misc;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditions;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;
import java.util.function.Supplier;

public class AncientDartLootModifier implements IGlobalLootModifier {

    // 1.20.5 turned every serializer registry MapCodec-based, so the modifier codec is
    // built with RecordCodecBuilder#mapCodec instead of #create.
    //? if >=1.20.5 {
    /*public static final Supplier<com.mojang.serialization.MapCodec<AncientDartLootModifier>> CODEC = () ->
            RecordCodecBuilder.mapCodec(inst ->
                    inst.group(
                                    LOOT_CONDITIONS_CODEC.fieldOf("conditions").forGetter(lm -> lm.conditions)
                            )
                            .apply(inst, AncientDartLootModifier::new));
    *///?} else {
    public static final Supplier<Codec<AncientDartLootModifier>> CODEC = () ->
            RecordCodecBuilder.create(inst ->
                    inst.group(
                                    LOOT_CONDITIONS_CODEC.fieldOf("conditions").forGetter(lm -> lm.conditions)
                            )
                            .apply(inst, AncientDartLootModifier::new));
    //?}

    // NeoForge 26.1 added priority() to IGlobalLootModifier as an abstract method (LootModifier,
    // which these do not extend, supplies it from a codec field). Nothing here needs to run in a
    // particular order relative to other mods' modifiers, so it takes the default. Forge 64's
    // IGlobalLootModifier has no priority concept at all — hence the loader gate, not a bare >=26.
    //? if neoforge && >=26 {
    /*public int priority() {
        return IGlobalLootModifier.DEFAULT_PRIORITY;
    }
    *///?}

    private final LootItemCondition[] conditions;

    private final Predicate<LootContext> orConditions;

    public AncientDartLootModifier(LootItemCondition[] conditionsIn) {
        this.conditions = conditionsIn;
        this.orConditions = com.github.alexthe666.alexsmobs.misc.AMPlatform.orConditions(conditionsIn);
    }

    @NotNull
    @Override
    public ObjectArrayList<ItemStack> apply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        return this.orConditions.test(context) ? this.doApply(generatedLoot, context) : generatedLoot;
    }

    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (AMConfig.addLootToChests) {
            if (context.getRandom().nextInt(1) == 0) {
                generatedLoot.add(new ItemStack(AMItemRegistry.ANCIENT_DART.get()));
            }
        }
        return generatedLoot;
    }


    //? if >=1.20.5 {
    /*@Override
    public com.mojang.serialization.MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
    *///?} else {
    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
    //?}

}
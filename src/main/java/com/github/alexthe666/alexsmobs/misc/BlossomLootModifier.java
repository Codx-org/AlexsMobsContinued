package com.github.alexthe666.alexsmobs.misc;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditions;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;
import java.util.function.Supplier;
import com.github.alexthe666.alexsmobs.misc.AMCompat;

public class BlossomLootModifier implements IGlobalLootModifier {

    // 1.20.5 turned every serializer registry MapCodec-based, so the modifier codec is
    // built with RecordCodecBuilder#mapCodec instead of #create.
    //? if >=1.20.5 {
    /*public static final Supplier<com.mojang.serialization.MapCodec<BlossomLootModifier>> CODEC = () ->
            RecordCodecBuilder.mapCodec(inst ->
                    inst.group(
                                    LOOT_CONDITIONS_CODEC.fieldOf("conditions").forGetter(lm -> lm.conditions)
                            )
                            .apply(inst, BlossomLootModifier::new));
    *///?} else {
    public static final Supplier<Codec<BlossomLootModifier>> CODEC = () ->
            RecordCodecBuilder.create(inst ->
                    inst.group(
                                    LOOT_CONDITIONS_CODEC.fieldOf("conditions").forGetter(lm -> lm.conditions)
                            )
                            .apply(inst, BlossomLootModifier::new));
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

    public BlossomLootModifier(LootItemCondition[] conditionsIn) {
        this.conditions = conditionsIn;
        this.orConditions = com.github.alexthe666.alexsmobs.misc.AMPlatform.orConditions(conditionsIn);
    }

    @NotNull
    @Override
    public ObjectArrayList<ItemStack> apply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        return this.orConditions.test(context) ? this.doApply(generatedLoot, context) : generatedLoot;
    }

    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (AMConfig.acaciaBlossomsDropFromLeaves) {
            ItemStack ctxTool = AMCompat.asItemStack(context.getParamOrNull(LootContextParams.TOOL));
            RandomSource random = context.getRandom();
            if (ctxTool != null) {
                int silkTouch = AMCompat.enchantLevel(Enchantments.SILK_TOUCH, ctxTool, context.getLevel());
                if (silkTouch > 0 || ctxTool.getItem() instanceof ShearsItem) {
                    return generatedLoot;
                }
            }
            int bonusLevel = ctxTool != null ? AMCompat.enchantLevel(Enchantments.BLOCK_FORTUNE, ctxTool, context.getLevel()) : 0;
            int blossomStep = (int) Math.floor(AMConfig.acaciaBlossomChance * 0.1F);
            int blossomRarity = AMConfig.acaciaBlossomChance - (bonusLevel * blossomStep);
            if (blossomRarity < 1 || random.nextInt(blossomRarity) == 0) {
                generatedLoot.add(new ItemStack(AMItemRegistry.ACACIA_BLOSSOM.get()));
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
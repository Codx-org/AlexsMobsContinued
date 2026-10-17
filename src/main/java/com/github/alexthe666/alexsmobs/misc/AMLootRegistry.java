package com.github.alexthe666.alexsmobs.misc;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.mojang.serialization.Codec;
import net.minecraftforge.common.loot.IGlobalLootModifier;
// Fabric has no loot-modifier serializer registry to defer into — see the fabric arm below.
//? if !fabric {
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
//?}
import java.util.function.Supplier;

public class AMLootRegistry {

    // 1.20.5: the loot-modifier serializer registry holds MapCodecs, not Codecs.
    // Fabric: ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS has no counterpart — the whole
    // mechanism (a serializer registry dispatched from data/<ns>/loot_modifiers/global_loot_modifiers.json)
    // is Forge's, and Fabric's equivalent is a LootTableEvents.MODIFY callback, i.e. a registration
    // rather than a datapack entry. So there is nothing to register into here; the four codecs are
    // still declared so the modifiers stay reachable for that wiring. Until it lands the modifiers
    // never run on this loader — see fabric/common/loot/IGlobalLootModifier for what that costs.
    // Two Fabric arms for the same reason the other loaders have two: the modifiers' CODEC fields are
    // MapCodec from 1.20.5 and plain Codec below it, and a Supplier's type argument is invariant.
    //? if fabric && >=1.20.5 {
    /*public static final Supplier<com.mojang.serialization.MapCodec<? extends IGlobalLootModifier>> BANANA_DROP = BananaLootModifier.CODEC::get;
    public static final Supplier<com.mojang.serialization.MapCodec<? extends IGlobalLootModifier>> BLOSSOM_DROP = BlossomLootModifier.CODEC::get;
    public static final Supplier<com.mojang.serialization.MapCodec<? extends IGlobalLootModifier>> ANCIENT_DART = AncientDartLootModifier.CODEC::get;
    public static final Supplier<com.mojang.serialization.MapCodec<? extends IGlobalLootModifier>> PIGSHOES = PigshoesLootModifier.CODEC::get;
    *///?} elif fabric {
    /*public static final Supplier<Codec<? extends IGlobalLootModifier>> BANANA_DROP = BananaLootModifier.CODEC::get;
    public static final Supplier<Codec<? extends IGlobalLootModifier>> BLOSSOM_DROP = BlossomLootModifier.CODEC::get;
    public static final Supplier<Codec<? extends IGlobalLootModifier>> ANCIENT_DART = AncientDartLootModifier.CODEC::get;
    public static final Supplier<Codec<? extends IGlobalLootModifier>> PIGSHOES = PigshoesLootModifier.CODEC::get;
    *///?} elif >=1.20.5 {
    /*public static final DeferredRegister<com.mojang.serialization.MapCodec<? extends IGlobalLootModifier>> DEF_REG = DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, AlexsMobs.MODID);
    public static final Supplier<com.mojang.serialization.MapCodec<? extends IGlobalLootModifier>> BANANA_DROP = DEF_REG.register("banana_drop", BananaLootModifier.CODEC);
    public static final Supplier<com.mojang.serialization.MapCodec<? extends IGlobalLootModifier>> BLOSSOM_DROP = DEF_REG.register("blossom_drop", BlossomLootModifier.CODEC);
    public static final Supplier<com.mojang.serialization.MapCodec<? extends IGlobalLootModifier>> ANCIENT_DART = DEF_REG.register("ancient_dart", AncientDartLootModifier.CODEC);
    public static final Supplier<com.mojang.serialization.MapCodec<? extends IGlobalLootModifier>> PIGSHOES = DEF_REG.register("pigshoes", PigshoesLootModifier.CODEC);
    *///?} else {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> DEF_REG = DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, AlexsMobs.MODID);
    public static final Supplier<Codec<? extends IGlobalLootModifier>> BANANA_DROP = DEF_REG.register("banana_drop", BananaLootModifier.CODEC);
    public static final Supplier<Codec<? extends IGlobalLootModifier>> BLOSSOM_DROP = DEF_REG.register("blossom_drop", BlossomLootModifier.CODEC);
    public static final Supplier<Codec<? extends IGlobalLootModifier>> ANCIENT_DART = DEF_REG.register("ancient_dart", AncientDartLootModifier.CODEC);
    public static final Supplier<Codec<? extends IGlobalLootModifier>> PIGSHOES = DEF_REG.register("pigshoes", PigshoesLootModifier.CODEC);
    //?}
}

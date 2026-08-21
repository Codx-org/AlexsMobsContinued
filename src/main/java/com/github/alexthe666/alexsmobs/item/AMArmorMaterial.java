package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.misc.AMCompat;

import net.minecraft.sounds.SoundEvent;
//? if <1.21.5 {
import net.minecraft.world.item.ArmorItem;
//?}
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

// Alex's Mobs' armour stats. Up to 1.20.4 this IS the ArmorMaterial (an interface back then).
// 1.20.5 turned ArmorMaterial into a record, so from there on this class just holds the raw
// numbers and hands out a lazily built holder() for ArmorItem to consume — same values, so
// armour behaves identically on every version.
public class AMArmorMaterial
        //? if <1.20.5
        implements ArmorMaterial
{

    protected static final int[] MAX_DAMAGE_ARRAY = new int[]{13, 15, 16, 11};
    private final String name;
    private final int durability;
    private final int[] damageReduction;
    private final int encantability;
    //? if >=1.20.5 {
    /*private final net.minecraft.core.Holder<SoundEvent> sound;
    *///?} else {
    private final SoundEvent sound;
    //?}
    private final float toughness;
    private java.util.function.Supplier<Ingredient> ingredient = null;
    public float knockbackResistance = 0.0F;

    //? if >=1.20.5 {
    /*public AMArmorMaterial(String name, int durability, int[] damageReduction, int encantability, net.minecraft.core.Holder<SoundEvent> sound, float toughness) {
    *///?} else {
    public AMArmorMaterial(String name, int durability, int[] damageReduction, int encantability, SoundEvent sound, float toughness) {
    //?}
        this.name = name;
        this.durability = durability;
        this.damageReduction = damageReduction;
        this.encantability = encantability;
        this.sound = sound;
        this.toughness = toughness;
        this.knockbackResistance = 0;
    }

    //? if >=1.20.5 {
    /*public AMArmorMaterial(String name, int durability, int[] damageReduction, int encantability, net.minecraft.core.Holder<SoundEvent> sound, float toughness, float knockbackResist) {
    *///?} else {
    public AMArmorMaterial(String name, int durability, int[] damageReduction, int encantability, SoundEvent sound, float toughness, float knockbackResist) {
    //?}
        this.name = name;
        this.durability = durability;
        this.damageReduction = damageReduction;
        this.encantability = encantability;
        this.sound = sound;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResist;
    }


    //? if <1.20.5
    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return (type.ordinal() < MAX_DAMAGE_ARRAY.length ? MAX_DAMAGE_ARRAY[type.ordinal()] : 1) * this.durability;
    }

    //? if <1.20.5
    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return type.ordinal() < this.damageReduction.length ? this.damageReduction[type.ordinal()] : 0;
    }

    //? if <1.20.5
    @Override
    public int getEnchantmentValue() {
        return this.encantability;
    }

    //? if >=1.20.5 {
    /*public net.minecraft.core.Holder<SoundEvent> getEquipSound() {
    *///?} else {
    @Override
    public SoundEvent getEquipSound() {
    //?}
        return this.sound;
    }

    //? if <1.20.5
    @Override
    public Ingredient getRepairIngredient() {
        //? if >=1.21.2 {
        /*return this.ingredient == null ? null : this.ingredient.get();
        *///?} else {
        return this.ingredient == null ? Ingredient.EMPTY : this.ingredient.get();
        //?}
    }

    public void setRepairMaterial(Ingredient ingredient) {
        this.ingredient = () -> ingredient;
    }

    // Tag-backed repair ingredients must be built lazily: from 1.21.2 resolving a tag into an
    // Ingredient forces the tag to bind, which throws if done during setup (before tags load).
    public void setRepairMaterial(java.util.function.Supplier<Ingredient> ingredient) {
        this.ingredient = AMCompat.lazyIngredient(ingredient);
    }


    //? if <1.20.5
    @Override
    public String getName() {
        return name;
    }

    //? if <1.20.5
    @Override
    public float getToughness() {
        return toughness;
    }

    //? if <1.20.5
    @Override
    public float getKnockbackResistance() {
        return knockbackResistance;
    }

    // The base durability multiplier. Pre-1.20.5 it was folded into getDurabilityForType;
    // from 1.20.5 durability is an item property, so ItemModArmor reads it here and feeds
    // ArmorItem.Type#getDurability — which uses the very same {13, 15, 16, 11} table.
    public int getDurability() {
        return this.durability;
    }

    //? if >=1.21.2 {
    /*private ArmorMaterial material;

    // 1.21.2 moved ArmorMaterial to world.item.equipment, dropped the Holder wrapper, replaced the
    // texture Layer list with an equipment-model id (assets/alexsmobs/models/equipment/<name>.json)
    // and the repair Ingredient with a TagKey (data/alexsmobs/tags/items/repairs/<name>.json).
    // ArmorItem's constructor feeds this straight through ArmorMaterial#humanoidProperties, which
    // is where the durability/attributes/enchantability/equip-sound/repairable components come from.
    public ArmorMaterial material() {
        if (this.material == null) {
            java.util.EnumMap<ArmorItem.Type, Integer> defense = new java.util.EnumMap<>(ArmorItem.Type.class);
            for (ArmorItem.Type type : ArmorItem.Type.values()) {
                defense.put(type, type.ordinal() < this.damageReduction.length ? this.damageReduction[type.ordinal()] : 0);
            }
            net.minecraft.tags.TagKey<net.minecraft.world.item.Item> repairTag =
                    net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, AMCompat.rl("alexsmobs", "repairs/" + this.name));
            // Registering the ArmorItem resolves this tag via getOrThrow; bind it to empty first so
            // NeoForge's registry freeze doesn't reject it as unbound (datapack rebinds it at reload).
            AMCompat.bindItemTagEmptyForFreeze(repairTag);
            this.material = new ArmorMaterial(
                    this.durability,
                    defense,
                    this.encantability,
                    this.sound,
                    this.toughness,
                    this.knockbackResistance,
                    repairTag,
                    AMCompat.equipmentAsset(this.name));
        }
        return this.material;
    }
    *///?} elif >=1.20.5 {
    /*private net.minecraft.core.Holder<ArmorMaterial> holder;

    // A direct (unregistered) holder: Alex's Mobs never references its materials from a
    // datapack, and the registry only matters for codec serialisation and armour trims.
    public net.minecraft.core.Holder<ArmorMaterial> holder() {
        if (this.holder == null) {
            java.util.EnumMap<ArmorItem.Type, Integer> defense = new java.util.EnumMap<>(ArmorItem.Type.class);
            for (ArmorItem.Type type : ArmorItem.Type.values()) {
                defense.put(type, type.ordinal() < this.damageReduction.length ? this.damageReduction[type.ordinal()] : 0);
            }
            this.holder = net.minecraft.core.Holder.direct(new ArmorMaterial(
                    defense,
                    this.encantability,
                    this.sound,
                    () -> this.ingredient == null ? Ingredient.EMPTY : this.ingredient.get(),
                    java.util.List.of(new ArmorMaterial.Layer(AMCompat.rl("alexsmobs", this.name))),
                    this.toughness,
                    this.knockbackResistance));
        }
        return this.holder;
    }
    *///?}
}

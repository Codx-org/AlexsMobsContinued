package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.misc.AMCompat;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.misc.AMPlatform;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
//? if <1.21.5 {
import net.minecraft.world.item.ArmorItem;
//?}
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class ItemModArmor extends
//? if >=1.21.5 {
/*Item
*///?} else {
ArmorItem
//?}
implements IClientExtensionItem {
    //? if >=1.21 {
    /*private static final net.minecraft.resources.ResourceLocation[] ARMOR_MODIFIERS = new net.minecraft.resources.ResourceLocation[]{AMCompat.rl("alexsmobs", "armor_modifier_0"), AMCompat.rl("alexsmobs", "armor_modifier_1"), AMCompat.rl("alexsmobs", "armor_modifier_2"), AMCompat.rl("alexsmobs", "armor_modifier_3")};
    *///?} else {
    private static final UUID[] ARMOR_MODIFIERS = new UUID[]{UUID.fromString("2AD3F246-FEE1-4E67-B886-69FD380BB150"), UUID.fromString("9F3D476D-C118-4544-8365-64846904B48E"), UUID.fromString("D8499B04-0E66-4726-AB29-64469D734E0D"), UUID.fromString("845DB27C-C624-495F-8C9F-6020A9A58B6B")};
    //?}

    // 1.20.5 replaced the ArmorMaterial interface with a record behind a Holder, so `this.material`
    // is no longer the AM material object the tooltip/texture switches compare against. Keep our
    // own reference — it reads identically on every version.
    protected final AMArmorMaterial amMaterial;

    // 1.21.2 stripped ArmorItem of its fields, so `this.type` is gone from there on. Keep our own.
    protected final ArmorItem.Type amType;

    private Multimap<Attribute, AttributeModifier> attributeMapCroc;
    private Multimap<Attribute, AttributeModifier> attributeMapMoose;
    private Multimap<Attribute, AttributeModifier> attributeMapFlyingFish;
    private Multimap<Attribute, AttributeModifier> attributeMapKimono;

    public ItemModArmor(AMArmorMaterial armorMaterial, ArmorItem.Type slot) {
        //? if >=1.21.5 {
        /*// 1.21.5 removed ArmorItem: the equippable/attributes/durability come from Properties#humanoidArmor,
        // fed the vanilla ArmorMaterial record AMArmorMaterial#material() builds (slot is an equipment.ArmorType).
        super(new Item.Properties().humanoidArmor(armorMaterial.material(), slot));
        *///?} elif >=1.21.2 {
        /*// ArmorMaterial#humanoidProperties (called by the ArmorItem constructor) sets durability itself.
        super(armorMaterial.material(), slot, new Item.Properties());
        *///?} elif >=1.20.5 {
        /*super(armorMaterial.holder(), slot, new Item.Properties().durability(slot.getDurability(armorMaterial.getDurability())));
        *///?} else {
        super(armorMaterial, slot, new Item.Properties());
        //?}
        this.amMaterial = armorMaterial;
        this.amType = slot;
    }

    @Override
    public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer) {
        consumer.accept((IClientItemExtensions) AlexsMobs.PROXY.getArmorRenderProperties());
    }


    @Override
    //? if >=1.21.5 {
    /*public void appendHoverText(ItemStack stack, Item.TooltipContext worldIn, net.minecraft.world.item.component.TooltipDisplay amDisplay, java.util.function.Consumer<Component> amTooltip, TooltipFlag flagIn) {
        java.util.List<Component> tooltip = new java.util.ArrayList<Component>() { public boolean add(Component amC) { amTooltip.accept(amC); return true; } };
    *///?} elif >=1.20.5 {
    /*public void appendHoverText(ItemStack stack, Item.TooltipContext worldIn, List<Component> tooltip, TooltipFlag flagIn) {
    *///?} else {
    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
    //?}
        if (this.amMaterial == AMItemRegistry.CENTIPEDE_ARMOR_MATERIAL) {
            tooltip.add(Component.translatable("item.alexsmobs.centipede_leggings.desc").withStyle(ChatFormatting.GRAY));
        }
        if (this.amMaterial == AMItemRegistry.EMU_ARMOR_MATERIAL) {
            tooltip.add(Component.translatable("item.alexsmobs.emu_leggings.desc").withStyle(ChatFormatting.GRAY));
        }
        //? if >=1.21.5 {
        /*super.appendHoverText(stack, worldIn, amDisplay, amTooltip, flagIn);
        *///?} else {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        //?}
        if (this.amMaterial == AMItemRegistry.ROADRUNNER_ARMOR_MATERIAL) {
            tooltip.add(Component.translatable("item.alexsmobs.roadrunner_boots.desc").withStyle(ChatFormatting.BLUE));
        }
        if (this.amMaterial == AMItemRegistry.RACCOON_ARMOR_MATERIAL) {
            tooltip.add(Component.translatable("item.alexsmobs.frontier_cap.desc").withStyle(ChatFormatting.BLUE));
        }
        if (this.amMaterial == AMItemRegistry.FROSTSTALKER_ARMOR_MATERIAL) {
            tooltip.add(Component.translatable("item.alexsmobs.froststalker_helmet.desc").withStyle(ChatFormatting.AQUA));
        }
        if (this.amMaterial == AMItemRegistry.ROCKY_ARMOR_MATERIAL) {
            tooltip.add(Component.translatable("item.alexsmobs.rocky_chestplate.desc").withStyle(ChatFormatting.GRAY));
        }
        if (this.amMaterial == AMItemRegistry.SOMBRERO_ARMOR_MATERIAL && AlexsMobs.isAprilFools()) {
            tooltip.add(Component.translatable("item.alexsmobs.sombrero.special_desc").withStyle(ChatFormatting.GRAY));
        }
        if (this.amMaterial == AMItemRegistry.FLYING_FISH_MATERIAL) {
            tooltip.add(Component.translatable("item.alexsmobs.flying_fish_boots.desc").withStyle(ChatFormatting.GRAY));
        }
        if (this.amMaterial == AMItemRegistry.NOVELTY_HAT_MATERIAL) {
            tooltip.add(Component.translatable("item.alexsmobs.novelty_hat.desc").withStyle(ChatFormatting.GRAY));
        }
        if (this.amMaterial == AMItemRegistry.KIMONO_MATERIAL) {
            tooltip.add(Component.translatable("item.alexsmobs.unsettling_kimono.desc").withStyle(ChatFormatting.GRAY));
        }
    }

    //? if <1.20.5 {
    private void buildCrocAttributes(AMArmorMaterial materialIn) {
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        var uuid = ARMOR_MODIFIERS[type.ordinal()];
        builder.put(Attributes.ARMOR, AMCompat.attributeModifier(uuid, "Armor modifier", materialIn.getDefenseForType(this.type), AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ARMOR_TOUGHNESS, AMCompat.attributeModifier(uuid, "Armor toughness", materialIn.getToughness(), AttributeModifier.Operation.ADDITION));
        // AMPlatform.swimSpeed()/blockReach()/entityReach() return null on loaders where vanilla has
        // no such attribute and there is no loader-added one either (Fabric below 1.21 / 1.20.5 — see
        // AMPlatform). ImmutableMultimap.put would NPE on a null key, so the one modifier is dropped
        // and the rest of the armour piece still builds. A plain if, not a Stonecutter gate: these
        // bodies are already inside a //? block and blocks do not nest.
        var swimSpeed = AMPlatform.swimSpeed();
        if (swimSpeed != null) {
            builder.put(swimSpeed, AMCompat.attributeModifier(uuid, "Swim speed", 1, AttributeModifier.Operation.ADDITION));
        }
        if (this.knockbackResistance > 0) {
            builder.put(Attributes.KNOCKBACK_RESISTANCE, AMCompat.attributeModifier(uuid, "Armor knockback resistance", this.knockbackResistance, AttributeModifier.Operation.ADDITION));
        }
        attributeMapCroc = builder.build();
    }

    private void buildFlyingFishAttributes(AMArmorMaterial materialIn) {
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        var uuid = ARMOR_MODIFIERS[type.ordinal()];
        builder.put(Attributes.ARMOR, AMCompat.attributeModifier(uuid, "Armor modifier", materialIn.getDefenseForType(this.type), AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ARMOR_TOUGHNESS, AMCompat.attributeModifier(uuid, "Armor toughness", materialIn.getToughness(), AttributeModifier.Operation.ADDITION));
        var swimSpeed = AMPlatform.swimSpeed();
        if (swimSpeed != null) {
            builder.put(swimSpeed, AMCompat.attributeModifier(uuid, "Swim speed", 0.5, AttributeModifier.Operation.ADDITION));
        }
        attributeMapFlyingFish = builder.build();
    }

    private void buildMooseAttributes(AMArmorMaterial materialIn) {
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        var uuid = ARMOR_MODIFIERS[type.ordinal()];
        builder.put(Attributes.ARMOR, AMCompat.attributeModifier(uuid, "Armor modifier", materialIn.getDefenseForType(this.type), AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ARMOR_TOUGHNESS, AMCompat.attributeModifier(uuid, "Armor toughness", materialIn.getToughness(), AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_KNOCKBACK, AMCompat.attributeModifier(uuid, "Knockback", 2, AttributeModifier.Operation.ADDITION));
        if (this.knockbackResistance > 0) {
            builder.put(Attributes.KNOCKBACK_RESISTANCE, AMCompat.attributeModifier(uuid, "Armor knockback resistance", this.knockbackResistance, AttributeModifier.Operation.ADDITION));
        }
        attributeMapMoose = builder.build();
    }

    private void buildKimonoAttributes(AMArmorMaterial materialIn) {
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        var uuid = ARMOR_MODIFIERS[type.ordinal()];
        builder.put(Attributes.ARMOR, AMCompat.attributeModifier(uuid, "Armor modifier", materialIn.getDefenseForType(this.type), AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ARMOR_TOUGHNESS, AMCompat.attributeModifier(uuid, "Armor toughness", materialIn.getToughness(), AttributeModifier.Operation.ADDITION));
        var blockReach = AMPlatform.blockReach();
        var entityReach = AMPlatform.entityReach();
        if (blockReach != null) {
            builder.put(blockReach, AMCompat.attributeModifier(uuid, "Block Reach distance", 2, AttributeModifier.Operation.ADDITION));
        }
        if (entityReach != null) {
            builder.put(entityReach, AMCompat.attributeModifier(uuid, "Entity Reach distance", 2, AttributeModifier.Operation.ADDITION));
        }
        attributeMapKimono = builder.build();
    }

    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot equipmentSlot) {
        if (this.amMaterial == AMItemRegistry.CROCODILE_ARMOR_MATERIAL && equipmentSlot == this.type.getSlot()) {
            if (attributeMapCroc == null) {
                buildCrocAttributes(AMItemRegistry.CROCODILE_ARMOR_MATERIAL);
            }
            return attributeMapCroc;
        }
        if (this.amMaterial == AMItemRegistry.MOOSE_ARMOR_MATERIAL && equipmentSlot == this.type.getSlot()) {
            if (attributeMapMoose == null) {
                buildMooseAttributes(AMItemRegistry.MOOSE_ARMOR_MATERIAL);
            }
            return attributeMapMoose;
        }
        if (this.amMaterial == AMItemRegistry.FLYING_FISH_MATERIAL && equipmentSlot == this.type.getSlot()) {
            if (attributeMapFlyingFish == null) {
                buildFlyingFishAttributes(AMItemRegistry.FLYING_FISH_MATERIAL);
            }
            return attributeMapFlyingFish;
        }
        if (this.amMaterial == AMItemRegistry.KIMONO_MATERIAL && equipmentSlot == this.type.getSlot()) {
            if (attributeMapKimono == null) {
                buildKimonoAttributes(AMItemRegistry.KIMONO_MATERIAL);
            }
            return attributeMapKimono;
        }
        return super.getDefaultAttributeModifiers(equipmentSlot);
    }
    //?}

    // SLICE (>=1.21.2): the four special materials' BONUS attributes (croc/flying-fish swim speed,
    // moose knockback, kimono reach) are dropped. The ArmorItem ctor unconditionally runs
    // ArmorMaterial#humanoidProperties, which calls .attributes(createAttributes(type)) and REPLACES
    // the ATTRIBUTE_MODIFIERS component with base-armour-only values after our Properties are built;
    // the component map is then immutable, so there is no seam to add the extras. Base armour/toughness
    // survive (they come from the material record). Faithful restoration would need an equipped-tick
    // attribute applier — a new subsystem, deferred.
    //? if >=1.20.5 && <1.21.2 {
    /*// 1.20.5 turned armour attributes into an item component: one immutable
    // ItemAttributeModifiers built once, holding the same values the four Multimaps did.
    private net.minecraft.world.item.component.ItemAttributeModifiers amModifiers;

    @Override
    public net.minecraft.world.item.component.ItemAttributeModifiers getDefaultAttributeModifiers() {
        if (this.amMaterial == AMItemRegistry.CROCODILE_ARMOR_MATERIAL
                || this.amMaterial == AMItemRegistry.MOOSE_ARMOR_MATERIAL
                || this.amMaterial == AMItemRegistry.FLYING_FISH_MATERIAL
                || this.amMaterial == AMItemRegistry.KIMONO_MATERIAL) {
            if (this.amModifiers == null) {
                this.amModifiers = buildAMModifiers();
            }
            return this.amModifiers;
        }
        return super.getDefaultAttributeModifiers();
    }

    private net.minecraft.world.item.component.ItemAttributeModifiers buildAMModifiers() {
        net.minecraft.world.entity.EquipmentSlotGroup group = net.minecraft.world.entity.EquipmentSlotGroup.bySlot(this.amType.getSlot());
        var uuid = ARMOR_MODIFIERS[this.amType.ordinal()];
        net.minecraft.world.item.component.ItemAttributeModifiers.Builder builder = net.minecraft.world.item.component.ItemAttributeModifiers.builder();
        builder.add(Attributes.ARMOR, AMCompat.attributeModifier(uuid, "Armor modifier", this.amMaterial.getDefenseForType(this.amType), AttributeModifier.Operation.ADD_VALUE), group);
        builder.add(Attributes.ARMOR_TOUGHNESS, AMCompat.attributeModifier(uuid, "Armor toughness", this.amMaterial.getToughness(), AttributeModifier.Operation.ADD_VALUE), group);
        if (this.amMaterial.knockbackResistance > 0 && this.amMaterial != AMItemRegistry.FLYING_FISH_MATERIAL) {
            builder.add(Attributes.KNOCKBACK_RESISTANCE, AMCompat.attributeModifier(uuid, "Armor knockback resistance", this.amMaterial.knockbackResistance, AttributeModifier.Operation.ADD_VALUE), group);
        }
        // Null-guarded for the same reason as buildCrocAttributes above: on Fabric 1.20.6 there is no
        // vanilla swim-speed attribute, so that one modifier is dropped rather than the item failing.
        var swimSpeed = AMPlatform.swimSpeed();
        var blockReach = AMPlatform.blockReach();
        var entityReach = AMPlatform.entityReach();
        if (this.amMaterial == AMItemRegistry.CROCODILE_ARMOR_MATERIAL) {
            if (swimSpeed != null) {
                builder.add(swimSpeed, AMCompat.attributeModifier(uuid, "Swim speed", 1, AttributeModifier.Operation.ADD_VALUE), group);
            }
        } else if (this.amMaterial == AMItemRegistry.FLYING_FISH_MATERIAL) {
            if (swimSpeed != null) {
                builder.add(swimSpeed, AMCompat.attributeModifier(uuid, "Swim speed", 0.5, AttributeModifier.Operation.ADD_VALUE), group);
            }
        } else if (this.amMaterial == AMItemRegistry.MOOSE_ARMOR_MATERIAL) {
            builder.add(Attributes.ATTACK_KNOCKBACK, AMCompat.attributeModifier(uuid, "Knockback", 2, AttributeModifier.Operation.ADD_VALUE), group);
        } else if (this.amMaterial == AMItemRegistry.KIMONO_MATERIAL) {
            if (blockReach != null) {
                builder.add(blockReach, AMCompat.attributeModifier(uuid, "Block Reach distance", 2, AttributeModifier.Operation.ADD_VALUE), group);
            }
            if (entityReach != null) {
                builder.add(entityReach, AMCompat.attributeModifier(uuid, "Entity Reach distance", 2, AttributeModifier.Operation.ADD_VALUE), group);
            }
        }
        return builder.build();
    }
    *///?}

    // AM ships its armour textures under textures/armor/ rather than the vanilla
    // textures/models/armor/ convention, so the loader hook keeps redirecting on every version.
    @Nullable
    private String armorTexturePath() {
        if (this.amMaterial == AMItemRegistry.CROCODILE_ARMOR_MATERIAL) {
            return "alexsmobs:textures/armor/crocodile_chestplate.png";
        } else if (this.amMaterial == AMItemRegistry.ROADRUNNER_ARMOR_MATERIAL) {
            return "alexsmobs:textures/armor/roadrunner_boots.png";
        } else if (this.amMaterial == AMItemRegistry.CENTIPEDE_ARMOR_MATERIAL) {
            return "alexsmobs:textures/armor/centipede_leggings.png";
        } else if (this.amMaterial == AMItemRegistry.MOOSE_ARMOR_MATERIAL) {
            return "alexsmobs:textures/armor/moose_headgear.png";
        } else if (this.amMaterial == AMItemRegistry.RACCOON_ARMOR_MATERIAL) {
            return "alexsmobs:textures/armor/frontier_cap.png";
        } else if (this.amMaterial == AMItemRegistry.SOMBRERO_ARMOR_MATERIAL) {
            return "alexsmobs:textures/armor/sombrero.png";
        } else if (this.amMaterial == AMItemRegistry.SPIKED_TURTLE_SHELL_ARMOR_MATERIAL) {
            return "alexsmobs:textures/armor/spiked_turtle_shell.png";
        } else if (this.amMaterial == AMItemRegistry.FEDORA_ARMOR_MATERIAL) {
            return "alexsmobs:textures/armor/fedora.png";
        } else if (this.amMaterial == AMItemRegistry.EMU_ARMOR_MATERIAL) {
            return "alexsmobs:textures/armor/emu_leggings.png";
        } else if (this.amMaterial == AMItemRegistry.FROSTSTALKER_ARMOR_MATERIAL) {
            return "alexsmobs:textures/armor/froststalker_helmet.png";
        } else if (this.amMaterial == AMItemRegistry.ROCKY_ARMOR_MATERIAL) {
            return "alexsmobs:textures/armor/rocky_chestplate.png";
        } else if (this.amMaterial == AMItemRegistry.FLYING_FISH_MATERIAL) {
            return "alexsmobs:textures/armor/flying_fish_boots.png";
        } else if (this.amMaterial == AMItemRegistry.NOVELTY_HAT_MATERIAL) {
            return "alexsmobs:textures/armor/novelty_hat.png";
        } else if (this.amMaterial == AMItemRegistry.KIMONO_MATERIAL) {
            return "alexsmobs:textures/armor/unsettling_kimono.png";
        }
        return null;
    }

    // Both overrides below are IItemExtension hooks the loaders patch onto Item; Fabric has neither,
    // and the super call is what makes them uncompilable there rather than merely dead. Fabric's own
    // seam is ArmorRenderer/EquipmentModel-shaped, so the redirect is a known Fabric-only loss below
    // 1.21.2 — the armour renders with the vanilla-convention path and shows as missing texture.
    //? if <1.20.5 && !fabric {
    @Nullable
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        String path = armorTexturePath();
        return path == null ? super.getArmorTexture(stack, entity, slot, type) : path;
    }
    //?}

    // 1.21.2 deleted the Forge hook outright: armour textures are resolved from the equipment
    // model the ArmorMaterial names, i.e. assets/alexsmobs/models/equipment/<material>.json.
    //? if >=1.20.5 && <1.21.2 && !fabric {
    /*@Nullable
    public net.minecraft.resources.ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, net.minecraft.world.item.ArmorMaterial.Layer layer, boolean innerModel) {
        String path = armorTexturePath();
        return path == null ? super.getArmorTexture(stack, entity, slot, layer, innerModel) : net.minecraft.resources.ResourceLocation.tryParse(path);
    }
    *///?}
}

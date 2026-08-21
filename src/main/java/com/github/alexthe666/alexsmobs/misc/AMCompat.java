package com.github.alexthe666.alexsmobs.misc;
import com.github.alexthe666.alexsmobs.misc.AMCompat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * Vanilla APIs that 1.20.5 "component-ified" away, funnelled through one place.
 *
 * <p>Every method keeps the pre-1.20.5 signatures so call sites read the same on all versions.
 * The one place where the <em>semantics</em> could not be preserved is stack NBT: on >=1.20.5
 * {@link #getTag}/{@link #getOrCreateTag} hand back a <em>copy</em> of the custom_data component,
 * so anything that mutates the returned tag must finish with {@link #setTag} — see the note above
 * those methods.
 */
public class AMCompat {

    // ── Synched Optional<UUID> ─────────────────────────────────────────────────
    // 1.21.5 deleted EntityDataSerializers.OPTIONAL_UUID (owner refs became EntityReference).
    // This mod still syncs plain Optional<UUID> fields (parent/child/feeder refs on ~20 entities),
    // so rebuild the identical serializer from the surviving codec primitives and register it.
    // SynchedEntityData looks a serializer up by its registered id when packing, so registration is
    // mandatory; the static block runs on first reference (an entity's defineId) — long before spawn.
    //? if >=1.21.5 {
    /*public static final net.minecraft.network.syncher.EntityDataSerializer<java.util.Optional<java.util.UUID>> OPTIONAL_UUID =
            net.minecraft.network.syncher.EntityDataSerializer.forValueType(net.minecraft.network.codec.ByteBufCodecs.optional(net.minecraft.core.UUIDUtil.STREAM_CODEC));
    *///?}

    // ── Synched CompoundTag ────────────────────────────────────────────────────
    // 1.21.9 deleted EntityDataSerializers.COMPOUND_TAG. Two things here still sync a raw tag:
    // the vendored Citadel entity-data store (mixin/LivingEntityMixin) and the catfish's swallowed-
    // entity snapshot. Rebuilt verbatim from vanilla 1.21.8's definition — TRUSTED_COMPOUND_TAG
    // (unlimited NbtAccounter, i.e. no size cap) and a copy() on read. It is NOT
    // EntityDataSerializer.forValueType: that yields ForValueType, whose copy() returns the SAME
    // instance, which for a mutable CompoundTag would let SynchedEntityData hand out an aliased tag.
    //? if >=1.21.9 {
    /*public static final net.minecraft.network.syncher.EntityDataSerializer<CompoundTag> COMPOUND_TAG =
            new net.minecraft.network.syncher.EntityDataSerializer<CompoundTag>() {
                public net.minecraft.network.codec.StreamCodec<? super net.minecraft.network.RegistryFriendlyByteBuf, CompoundTag> codec() {
                    return net.minecraft.network.codec.ByteBufCodecs.TRUSTED_COMPOUND_TAG;
                }

                public CompoundTag copy(CompoundTag tag) {
                    return tag.copy();
                }
            };
    *///?}

    // …but the two loaders disagree on WHERE these are registered, from 1.21.5 on.
    // NeoForge 1.21.5 makes EntityDataSerializers.registerSerializer hard-throw for any non-vanilla
    // caller (a mod adding to the vanilla list would desync serializer ids between client and
    // server), and demands its own NeoForgeRegistries.ENTITY_DATA_SERIALIZERS registry instead.
    // Deferring is safe: SynchedEntityData.defineId only stores the serializer *object*; the numeric
    // id is looked up later, at pack time, via getSerializedId. Forge still accepts the vanilla
    // static call on every node (its EntityDataSerializers carries no caller check — checked on
    // 59.0.5), so it keeps the pre-1.21.5 shape.
    // Four arms rather than two because Stonecutter blocks are siblings, never nested: the
    // COMPOUND_TAG entry only exists from 1.21.9, so each loader needs a pre/post-1.21.9 pair.
    //? if neoforge && >=1.21.9 {
    /*public static final net.neoforged.neoforge.registries.DeferredRegister<net.minecraft.network.syncher.EntityDataSerializer<?>> DATA_SERIALIZER_DEF_REG =
            net.neoforged.neoforge.registries.DeferredRegister.create(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.ENTITY_DATA_SERIALIZERS, "alexsmobs");

    static {
        DATA_SERIALIZER_DEF_REG.register("optional_uuid", () -> OPTIONAL_UUID);
        DATA_SERIALIZER_DEF_REG.register("compound_tag", () -> COMPOUND_TAG);
    }
    *///?} elif neoforge && >=1.21.5 {
    /*public static final net.neoforged.neoforge.registries.DeferredRegister<net.minecraft.network.syncher.EntityDataSerializer<?>> DATA_SERIALIZER_DEF_REG =
            net.neoforged.neoforge.registries.DeferredRegister.create(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.ENTITY_DATA_SERIALIZERS, "alexsmobs");

    static {
        DATA_SERIALIZER_DEF_REG.register("optional_uuid", () -> OPTIONAL_UUID);
    }
    *///?} elif fabric && >=26 {
    /*// Fabric API takes the same position as NeoForge, in the SAME MC line — the guard arrived with
    // object-builder 21.1.2, shipped in the 1.21.5 fabric-api. It just changed its mind about the
    // name of the replacement API: `FabricTrackedDataRegistry` (Yarn-flavoured) from 1.21.5 through
    // 1.21.11, `FabricEntityDataRegistry` from the 23.x module (MC 26.1). Same package, same
    // `register(ResourceLocation, EntityDataSerializer<?>)` signature — only the class name moved,
    // so this is a rename, not a behaviour change. Verified by listing
    // net/fabricmc/fabric/api/object/builder/v1/entity/ in every pinned fabric-api jar.
    static {
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityDataRegistry.register(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("alexsmobs", "optional_uuid"), OPTIONAL_UUID);
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityDataRegistry.register(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("alexsmobs", "compound_tag"), COMPOUND_TAG);
    }
    *///?} elif fabric && >=1.21.9 {
    /*static {
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricTrackedDataRegistry.register(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("alexsmobs", "optional_uuid"), OPTIONAL_UUID);
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricTrackedDataRegistry.register(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("alexsmobs", "compound_tag"), COMPOUND_TAG);
    }
    *///?} elif fabric && >=1.21.5 {
    /*static {
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricTrackedDataRegistry.register(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("alexsmobs", "optional_uuid"), OPTIONAL_UUID);
    }
    *///?} elif >=1.21.9 {
    /*static {
        net.minecraft.network.syncher.EntityDataSerializers.registerSerializer(OPTIONAL_UUID);
        net.minecraft.network.syncher.EntityDataSerializers.registerSerializer(COMPOUND_TAG);
    }
    *///?} elif >=1.21.5 {
    /*static {
        net.minecraft.network.syncher.EntityDataSerializers.registerSerializer(OPTIONAL_UUID);
    }
    *///?}

    // ── TamableAnimal owner ────────────────────────────────────────────────────
    // 1.21.5 replaced the owner UUID with an EntityReference: getOwnerUUID()/setOwnerUUID(UUID)
    // became getOwnerReference()/setOwnerReference(EntityReference). Keep the UUID-shaped call sites.
    @Nullable
    public static java.util.UUID getOwnerUUID(net.minecraft.world.entity.TamableAnimal animal) {
        //? if >=1.21.5 {
        /*net.minecraft.world.entity.EntityReference<net.minecraft.world.entity.LivingEntity> ref = animal.getOwnerReference();
        return ref == null ? null : ref.getUUID();
        *///?} else {
        return animal.getOwnerUUID();
        //?}
    }

    public static void setOwnerUUID(net.minecraft.world.entity.TamableAnimal animal, @Nullable java.util.UUID uuid) {
        // 1.21.9 made both EntityReference constructors private; the factories are the way in.
        //? if >=1.21.9 {
        /*animal.setOwnerReference(uuid == null ? null : net.minecraft.world.entity.EntityReference.of(uuid));
        *///?} elif >=1.21.5 {
        /*animal.setOwnerReference(uuid == null ? null : new net.minecraft.world.entity.EntityReference<>(uuid));
        *///?} else {
        animal.setOwnerUUID(uuid);
        //?}
    }

    // ── Biome mob-spawn entries ────────────────────────────────────────────────
    // 1.21.5 dropped the weight from SpawnerData (now EntityType,min,max) and moved the weight
    // onto Builder.addSpawn(cat, weight, data); pre-1.21.5 the weight lives in SpawnerData and
    // Builder.addSpawn(cat, data) takes the (EntityType,weight,min,max) form.
    public static void addSpawn(net.minecraft.world.level.biome.MobSpawnSettings.Builder b, net.minecraft.world.entity.MobCategory cat, net.minecraft.world.entity.EntityType<?> type, int weight, int min, int max) {
        //? if >=1.21.5 {
        /*b.addSpawn(cat, weight, new net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData(type, min, max));
        *///?} else {
        b.addSpawn(cat, new net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData(type, weight, min, max));
        //?}
    }

    // ── Shield-blocking test ───────────────────────────────────────────────────
    // Forge's ToolActions.SHIELD_BLOCK "can this item block?" query: NeoForge 1.21.5 *removed* the
    // shield constants from ItemAbilities (Forge 1.21.5 only deprecates them), because blocking
    // became the vanilla BLOCKS_ATTACKS data component. Gate on the version rather than the loader
    // so both take the same path: 1.21.5+ tests for the component, earlier eras use the ToolAction.
    // Fabric has no ToolAction system at all, so below 1.21.5 it falls back to the test vanilla
    // itself uses to decide whether a held item is guarding — LivingEntity#isBlocking asks for
    // UseAnim.BLOCK. That is what both of this mod's shield-ish items already return.
    public static boolean canShieldBlock(net.minecraft.world.item.ItemStack stack) {
        //? if >=1.21.5 {
        /*return stack.has(net.minecraft.core.component.DataComponents.BLOCKS_ATTACKS);
        *///?} elif fabric {
        /*return stack.getUseAnimation() == net.minecraft.world.item.UseAnim.BLOCK;
        *///?} else {
        return stack.canPerformAction(net.minecraftforge.common.ToolActions.SHIELD_BLOCK);
        //?}
    }

    // The declaration half of the same break. Up to 1.21.3 an item announced "I am a shield" by
    // overriding canPerformAction to accept the DEFAULT_SHIELD_ACTIONS set; 1.21.5 removed both the
    // ItemAbility constants and the set, and blocking became the BLOCKS_ATTACKS data component. So
    // on >=1.21.5 the two shield-ish items stamp that component onto their Properties instead, with
    // vanilla Items.SHIELD's exact numbers (0.25s block delay, 1.0 disable-cooldown scale, a 90°
    // blocking arc reducing 100% of damage, 3.0/1.0/1.0 item damage, bypassed by BYPASSES_SHIELD).
    public static net.minecraft.world.item.Item.Properties shieldProperties(net.minecraft.world.item.Item.Properties props) {
        // 26.1 retyped BlocksAttacks#bypassedBy from a TagKey to a HolderSet, which can only be
        // resolved with the registry context — so it goes through delayedComponent, which is
        // exactly what vanilla's own SHIELD does.
        //? if >=26 {
        /*return props.delayedComponent(net.minecraft.core.component.DataComponents.BLOCKS_ATTACKS,
                context -> new net.minecraft.world.item.component.BlocksAttacks(
                        0.25F,
                        1.0F,
                        java.util.List.of(new net.minecraft.world.item.component.BlocksAttacks.DamageReduction(90.0F, java.util.Optional.empty(), 0.0F, 1.0F)),
                        new net.minecraft.world.item.component.BlocksAttacks.ItemDamageFunction(3.0F, 1.0F, 1.0F),
                        java.util.Optional.of(context.getOrThrow(net.minecraft.tags.DamageTypeTags.BYPASSES_SHIELD)),
                        java.util.Optional.of(net.minecraft.sounds.SoundEvents.SHIELD_BLOCK),
                        java.util.Optional.of(net.minecraft.sounds.SoundEvents.SHIELD_BREAK)));
        *///?}
        //? if >=1.21.5 && <26 {
        /*return props.component(net.minecraft.core.component.DataComponents.BLOCKS_ATTACKS,
                new net.minecraft.world.item.component.BlocksAttacks(
                        0.25F,
                        1.0F,
                        java.util.List.of(new net.minecraft.world.item.component.BlocksAttacks.DamageReduction(90.0F, java.util.Optional.empty(), 0.0F, 1.0F)),
                        new net.minecraft.world.item.component.BlocksAttacks.ItemDamageFunction(3.0F, 1.0F, 1.0F),
                        java.util.Optional.of(net.minecraft.tags.DamageTypeTags.BYPASSES_SHIELD),
                        java.util.Optional.of(net.minecraft.sounds.SoundEvents.SHIELD_BLOCK),
                        java.util.Optional.of(net.minecraft.sounds.SoundEvents.SHIELD_BREAK)));
        *///?}
        //? if <1.21.5 {
        return props;
        //?}
    }

    // 1.21.2 deleted the Forge canElytraFly/elytraFlightTick item hooks in favour of the vanilla
    // minecraft:glider data component — and Forge kept declaring the hooks but stopped calling
    // them (zero call sites in every ≥1.21.2 patched jar, verified by bytecode sweep), so the
    // component is the ONLY working glide seam on ≥1.21.2, on all three loaders. Vanilla drains
    // durability itself and stops the glide when the next damage would break the item, which is
    // exactly ItemTarantulaHawkElytra#isUsable's rule. The chest EQUIPPABLE component the glide
    // check also needs comes from the armor-item constructor, so nothing else is required here.
    public static net.minecraft.world.item.Item.Properties glider(net.minecraft.world.item.Item.Properties props) {
        //? if >=1.21.2
        //return props.component(net.minecraft.core.component.DataComponents.GLIDER, net.minecraft.util.Unit.INSTANCE);
        //? if <1.21.2
        return props;
    }

    // ── ResourceLocation ───────────────────────────────────────────────────────
    // 1.21 made ResourceLocation's two-argument constructor private and deleted the
    // single-argument one outright; fromNamespaceAndPath/parse replace them. Those factories do
    // not exist below 1.21, and an access transformer cannot bring back a constructor that was
    // removed, so all ~520 construction sites in this mod go through here instead.

    public static ResourceLocation rl(String namespace, String path) {
        //? if >=1.21 {
        /*return ResourceLocation.fromNamespaceAndPath(namespace, path);
        *///?} else {
        return new ResourceLocation(namespace, path);
        //?}
    }

    // The equipment-asset id an ArmorMaterial points at. Up to 1.21.3 this was a bare ResourceLocation;
    // 1.21.4 replaced it with a ResourceKey<EquipmentAsset> into the equipment-asset registry. Only used
    // from AMArmorMaterial#material(), which exists solely on >=1.21.2.
    //? if >=1.21.4 {
    /*public static net.minecraft.resources.ResourceKey<net.minecraft.world.item.equipment.EquipmentAsset> equipmentAsset(String name) {
        return net.minecraft.resources.ResourceKey.create(net.minecraft.world.item.equipment.EquipmentAssets.ROOT_ID, rl("alexsmobs", name));
    }
    *///?} elif >=1.21.2 {
    /*public static ResourceLocation equipmentAsset(String name) {
        return rl("alexsmobs", name);
    }
    *///?}

    public static ResourceLocation rl(String location) {
        //? if >=1.21 {
        /*return ResourceLocation.parse(location);
        *///?} else {
        return new ResourceLocation(location);
        //?}
    }

    // ── Attribute modifiers ────────────────────────────────────────────────────
    // 1.21 re-keyed attribute modifiers from UUID to ResourceLocation and dropped the
    // human-readable name from the constructor. AttributeInstance#removeModifier takes whichever
    // of the two is current, so call sites only need the construction funnelled — but every id
    // constant in this mod has to be declared with a version-gated type (see ItemModArmor,
    // EffectFleetFooted, EffectDebilitatingSting, ServerEvents).

    //? if >=1.21 {
    /*public static final ResourceLocation BASE_ATTACK_DAMAGE_ID = net.minecraft.world.item.Item.BASE_ATTACK_DAMAGE_ID;
    public static final ResourceLocation BASE_ATTACK_SPEED_ID = net.minecraft.world.item.Item.BASE_ATTACK_SPEED_ID;

    public static net.minecraft.world.entity.ai.attributes.AttributeModifier attributeModifier(ResourceLocation id, String name, double amount, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation operation) {
        return new net.minecraft.world.entity.ai.attributes.AttributeModifier(id, amount, operation);
    }

    public static net.minecraft.world.entity.ai.attributes.AttributeModifier attributeModifier(String name, double amount, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation operation) {
        return new net.minecraft.world.entity.ai.attributes.AttributeModifier(nameAsId(name), amount, operation);
    }

    private static ResourceLocation nameAsId(String name) {
        return rl("alexsmobs", name.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_]+", "_"));
    }
    *///?} else {
    // Item's two BASE_ATTACK_* UUIDs are protected (1.21 made the ResourceLocation replacements
    // public). They are fixed vanilla constants, so they are spelled out rather than widened with
    // an access transformer, whose SRG name would have to be pinned per MC version.
    public static final java.util.UUID BASE_ATTACK_DAMAGE_ID = java.util.UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
    public static final java.util.UUID BASE_ATTACK_SPEED_ID = java.util.UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3");

    public static net.minecraft.world.entity.ai.attributes.AttributeModifier attributeModifier(java.util.UUID id, String name, double amount, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation operation) {
        return new net.minecraft.world.entity.ai.attributes.AttributeModifier(id, name, amount, operation);
    }

    public static net.minecraft.world.entity.ai.attributes.AttributeModifier attributeModifier(String name, double amount, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation operation) {
        return new net.minecraft.world.entity.ai.attributes.AttributeModifier(name, amount, operation);
    }
    //?}

    /**
     * MobEffect#addAttributeModifier's id argument: a UUID string up to 1.20.6, a ResourceLocation
     * from 1.21. The two are unrelated values, so each call site names its modifier and keeps its
     * historical UUID for the older nodes.
     */
    //? if >=1.21 {
    /*public static ResourceLocation attrModId(String uuid, String name) {
        return rl("alexsmobs", name);
    }
    *///?} else {
    public static String attrModId(String uuid, String name) {
        return uuid;
    }
    //?}

    /**
     * "Is this exact modifier already on the attribute?" 1.21 re-keyed the lookup from the
     * modifier object to its id, so every caller asks by id on all versions.
     */
    //? if >=1.21 {
    /*public static boolean hasModifier(@Nullable net.minecraft.world.entity.ai.attributes.AttributeInstance instance, ResourceLocation id) {
        return instance != null && instance.hasModifier(id);
    }
    *///?} else {
    public static boolean hasModifier(@Nullable net.minecraft.world.entity.ai.attributes.AttributeInstance instance, java.util.UUID id) {
        return instance != null && instance.getModifier(id) != null;
    }
    //?}

    // ── ItemStack NBT ──────────────────────────────────────────────────────────
    // 1.20.5 replaced the free-form stack tag with the custom_data component.
    //
    // ⚠️ THE RETURNED TAG IS A COPY on >=1.20.5, so mutating it does NOT touch the stack —
    // every caller that changes anything must finish with setTag(stack, tag). 1.20.5-1.21.8
    // also had CustomData#getUnsafe(), which handed back the live tag and let callers get away
    // without that; 1.21.9 DELETED it (along with size()/CODEC_WITH_ID), leaving copyTag() as
    // the only reader. Rather than keep two aliasing behaviours, every node now takes the copy
    // path — so a call site that works on 1.20.1 works everywhere, and there is no era where
    // "it happens to persist" hides a missing write-back.

    @Nullable
    public static CompoundTag getTag(ItemStack stack) {
        //? if >=1.20.5 {
        /*net.minecraft.world.item.component.CustomData data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        return data == null ? null : data.copyTag();
        *///?} else {
        return stack.getTag();
        //?}
    }

    public static CompoundTag getOrCreateTag(ItemStack stack) {
        //? if >=1.20.5 {
        /*net.minecraft.world.item.component.CustomData data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
        *///?} else {
        return stack.getOrCreateTag();
        //?}
    }

    public static void setTag(ItemStack stack, @Nullable CompoundTag tag) {
        //? if >=1.20.5 {
        /*if (tag == null) {
            stack.remove(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        } else {
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
        }
        *///?} else {
        stack.setTag(tag);
        //?}
    }

    public static boolean hasTag(ItemStack stack) {
        //? if >=1.20.5 {
        /*net.minecraft.world.item.component.CustomData data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        return data != null && !data.isEmpty();
        *///?} else {
        return stack.hasTag();
        //?}
    }

    @Nullable
    public static CompoundTag getTagElement(ItemStack stack, String key) {
        CompoundTag tag = getTag(stack);
        return tag != null && AMCompat.contains(tag, key, Tag.TAG_COMPOUND) ? AMCompat.getCompound(tag, key) : null;
    }

    public static void addTagElement(ItemStack stack, String key, Tag value) {
        CompoundTag tag = getOrCreateTag(stack);
        tag.put(key, value);
        setTag(stack, tag);
    }

    // ── Custom names ───────────────────────────────────────────────────────────

    public static ItemStack setHoverName(ItemStack stack, @Nullable Component name) {
        //? if >=1.20.5 {
        /*stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, name);
        return stack;
        *///?} else {
        return stack.setHoverName(name);
        //?}
    }

    // ── Food ───────────────────────────────────────────────────────────────────

    public static boolean isEdible(Item item) {
        //? if >=1.20.5 {
        /*return item.components().has(net.minecraft.core.component.DataComponents.FOOD);
        *///?} else {
        return item.isEdible();
        //?}
    }

    public static boolean isEdible(ItemStack stack) {
        //? if >=1.20.5 {
        /*return stack.has(net.minecraft.core.component.DataComponents.FOOD);
        *///?} else {
        return stack.isEdible();
        //?}
    }

    @Nullable
    public static FoodProperties getFoodProperties(Item item) {
        //? if >=1.20.5 {
        /*return item.components().get(net.minecraft.core.component.DataComponents.FOOD);
        *///?} else {
        return item.getFoodProperties();
        //?}
    }

    /**
     * 1.20.5 dropped {@code FoodProperties#isMeat()} along with the whole notion, so the
     * carnivore checks fall back to an explicit list of the meats a vanilla+AM world has.
     */
    /**
     * {@code Entity#shouldRiderSit()} is a Forge {@code IForgeEntity} extension with no vanilla
     * equivalent, so on Fabric the four mobs that override it are asked for by name. All four
     * return {@code false}; every other vehicle takes Forge's {@code true} default.
     */
    public static boolean shouldRiderSit(net.minecraft.world.entity.Entity vehicle) {
        //? if !fabric {
        return vehicle.shouldRiderSit();
        //?} else {
        /*return !(vehicle instanceof com.github.alexthe666.alexsmobs.entity.EntityCosmaw
                || vehicle instanceof com.github.alexthe666.alexsmobs.entity.EntityWarpedMosco
                || vehicle instanceof com.github.alexthe666.alexsmobs.entity.EntityCrocodile
                || vehicle instanceof com.github.alexthe666.alexsmobs.entity.EntityStraddleboard);
        *///?}
    }

    public static boolean isMeat(Item item) {
        //? if >=1.20.5 {
        /*return Meats.SET.contains(item);
        *///?} else {
        FoodProperties food = item.getFoodProperties();
        return food != null && food.isMeat();
        //?}
    }

    /**
     * The meat list, in a holder class so it is built on first use rather than in {@code
     * AMCompat.<clinit>}.
     *
     * <p>⚠️ It MUST stay lazy. AMCompat's own static init is reached from {@code
     * LivingEntity.<clinit>} (the vendored Citadel data store defines a static {@code
     * EntityDataAccessor} there), and from 1.21.9 vanilla's bootstrap chain is {@code
     * Items.<clinit>} → {@code Item.Properties} → {@code DataComponents} → {@code EntityType} →
     * {@code LivingEntity}. A direct {@code Set.of(Items.BEEF, …)} field therefore observes a
     * half-initialised {@code Items} whose fields are all still null and dies with
     * {@code NullPointerException: Cannot invoke "Object.hashCode()"} inside {@code Set.of},
     * killing the server during {@code Bootstrap.bootStrap}. Nothing calls {@link #isMeat} until
     * long after registration, so the holder is only loaded once {@code Items} is complete.
     */
    private static final class Meats {
        static final Set<Item> SET = Set.of(
                Items.BEEF, Items.COOKED_BEEF, Items.PORKCHOP, Items.COOKED_PORKCHOP,
                Items.CHICKEN, Items.COOKED_CHICKEN, Items.MUTTON, Items.COOKED_MUTTON,
                Items.RABBIT, Items.COOKED_RABBIT, Items.COD, Items.COOKED_COD,
                Items.SALMON, Items.COOKED_SALMON, Items.TROPICAL_FISH, Items.PUFFERFISH,
                Items.ROTTEN_FLESH, Items.SPIDER_EYE);
    }

    // ── Loot tables ────────────────────────────────────────────────────────────
    // 1.20.5 moved loot tables into a reloadable registry keyed by ResourceKey.

    public static net.minecraft.world.level.storage.loot.LootTable lootTable(net.minecraft.server.MinecraftServer server, net.minecraft.resources.ResourceLocation id) {
        //? if >=1.20.5
        //return server.reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, id));
        //? if <1.20.5
        return server.getLootData().getLootTable(id);
    }

    //? if >=1.20.5 {
    /*public static net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> lootKey(net.minecraft.resources.ResourceLocation id) {
        return net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, id);
    }
    *///?}

    // #104: 1.21.2 moved the default loot table onto EntityType and replaced the overridable
    // Mob#getDefaultLootTable() with Entity/Mob#getLootTable() returning an Optional<ResourceKey>.
    // That method is NOT final -- read out of 1.21.2, 1.21.3, 26.1.2 and 26.2 sources -- and
    // LivingEntity#dropFromLootTable calls it virtually, so a per-variant override still works.
    // Nine of this mod's entities had theirs deleted on the strength of a comment claiming
    // otherwise, which silently dropped 14 conditional loot tables on every node >=1.21.2.
    //? if >=1.21.2 {
    /*public static java.util.Optional<net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable>> lootOpt(net.minecraft.resources.ResourceLocation id) {
        return java.util.Optional.of(lootKey(id));
    }
    *///?}

    public static net.minecraft.world.level.storage.loot.LootTable fishingLoot(net.minecraft.server.MinecraftServer server) {
        //? if >=1.20.5
        //return server.reloadableRegistries().getLootTable(net.minecraft.world.level.storage.loot.BuiltInLootTables.FISHING);
        //? if <1.20.5
        return server.getLootData().getLootTable(net.minecraft.world.level.storage.loot.BuiltInLootTables.FISHING);
    }

    // ── ItemStack <-> NBT ──────────────────────────────────────────────────────
    // 1.20.5 needs the registries around to (de)serialise a stack's components, and an
    // empty stack no longer round-trips through a tag — hence the empty checks here.

    public static ItemStack loadItem(net.minecraft.core.HolderLookup.Provider provider, CompoundTag tag) {
        //? if >=1.21.6 {
        /*// 1.21.6 removed ItemStack.parse/save outright — only the codecs are left, and they need a
        // registry-aware DynamicOps to resolve the item id and its components.
        return ItemStack.CODEC.parse(provider.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), tag)
                .result().orElse(ItemStack.EMPTY);
        *///?} elif >=1.21.5 {
        /*// 1.21.5 removed ItemStack.parseOptional; parse(...) returns Optional<ItemStack>.
        return ItemStack.parse(provider, tag).orElse(ItemStack.EMPTY);
        *///?} elif >=1.20.5 {
        /*return ItemStack.parseOptional(provider, tag);
        *///?} else {
        return ItemStack.of(tag);
        //?}
    }

    public static CompoundTag saveItem(net.minecraft.core.HolderLookup.Provider provider, ItemStack stack) {
        //? if >=1.21.6 {
        /*// See loadItem — codec-only from 1.21.6. CODEC still refuses an empty stack.
        if (stack.isEmpty()) {
            return new CompoundTag();
        }
        return (CompoundTag) ItemStack.CODEC
                .encodeStart(provider.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), stack).getOrThrow();
        *///?} elif >=1.21.5 {
        /*// 1.21.5 removed ItemStack.saveOptional; save(provider) returns a Tag (throws on empty).
        return stack.isEmpty() ? new CompoundTag() : (CompoundTag) stack.save(provider);
        *///?} elif >=1.20.5 {
        /*return (CompoundTag) stack.saveOptional(provider);
        *///?} else {
        return stack.save(new CompoundTag());
        //?}
    }

    // 1.21.6 dropped ContainerHelper's CompoundTag overloads entirely, so a caller that owns a raw
    // tag (the capsid's update packet) has to go through the ValueOutput/ValueInput adapters.
    public static void saveAllItems(net.minecraft.core.HolderLookup.Provider provider, CompoundTag tag, net.minecraft.core.NonNullList<ItemStack> items) {
        //? if >=1.21.6 {
        /*net.minecraft.world.level.storage.TagValueOutput out = net.minecraft.world.level.storage.TagValueOutput
                .createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, provider);
        net.minecraft.world.ContainerHelper.saveAllItems(out, items);
        tag.merge(out.buildResult());
        *///?} elif >=1.20.5 {
        /*net.minecraft.world.ContainerHelper.saveAllItems(tag, items, provider);
        *///?} else {
        net.minecraft.world.ContainerHelper.saveAllItems(tag, items);
        //?}
    }

    public static void loadAllItems(net.minecraft.core.HolderLookup.Provider provider, CompoundTag tag, net.minecraft.core.NonNullList<ItemStack> items) {
        //? if >=1.21.6 {
        /*net.minecraft.world.ContainerHelper.loadAllItems(net.minecraft.world.level.storage.TagValueInput
                .create(net.minecraft.util.ProblemReporter.DISCARDING, provider, tag), items);
        *///?} elif >=1.20.5 {
        /*net.minecraft.world.ContainerHelper.loadAllItems(tag, items, provider);
        *///?} else {
        net.minecraft.world.ContainerHelper.loadAllItems(tag, items);
        //?}
    }

    // 1.21.6 replaced SimpleContainer#createTag/#fromTag with storeAsItemList/fromItemList, which
    // speak ValueOutput/ValueInput typed lists. Callers here still want a plain ListTag to drop into
    // a component/tag of their own, so the list is round-tripped through a throwaway adapter.
    public static net.minecraft.nbt.ListTag createTag(net.minecraft.core.HolderLookup.Provider provider, net.minecraft.world.SimpleContainer container) {
        //? if >=1.21.6 {
        /*net.minecraft.world.level.storage.TagValueOutput out = net.minecraft.world.level.storage.TagValueOutput
                .createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, provider);
        container.storeAsItemList(out.list("Items", ItemStack.CODEC));
        net.minecraft.nbt.Tag stored = out.buildResult().get("Items");
        return stored instanceof net.minecraft.nbt.ListTag list ? list : new net.minecraft.nbt.ListTag();
        *///?} elif >=1.20.5 {
        /*return container.createTag(provider);
        *///?} else {
        return container.createTag();
        //?}
    }

    public static void fromTag(net.minecraft.core.HolderLookup.Provider provider, net.minecraft.world.SimpleContainer container, net.minecraft.nbt.ListTag list) {
        //? if >=1.21.6 {
        /*CompoundTag wrapper = new CompoundTag();
        wrapper.put("Items", list);
        container.fromItemList(net.minecraft.world.level.storage.TagValueInput
                .create(net.minecraft.util.ProblemReporter.DISCARDING, provider, wrapper)
                .listOrEmpty("Items", ItemStack.CODEC));
        *///?} elif >=1.20.5 {
        /*container.fromTag(list, provider);
        *///?} else {
        container.fromTag(list);
        //?}
    }

    // ── Dyed leather ───────────────────────────────────────────────────────────
    // 1.20.5 replaced the DyeableLeatherItem interface with the dyed_color component.

    public static int getDyedColor(ItemStack stack, int fallback) {
        //? if >=1.20.5
        //return net.minecraft.world.item.component.DyedItemColor.getOrDefault(stack, fallback);
        //? if <1.20.5
        return stack.getItem() instanceof net.minecraft.world.item.DyeableLeatherItem dyeable ? dyeable.getColor(stack) : fallback;
    }

    public static boolean hasCustomColor(ItemStack stack) {
        //? if >=1.20.5
        //return stack.has(net.minecraft.core.component.DataComponents.DYED_COLOR);
        //? if <1.20.5
        return stack.getItem() instanceof net.minecraft.world.item.DyeableLeatherItem dyeable && dyeable.hasCustomColor(stack);
    }

    // ── BlockPos in NBT ────────────────────────────────────────────────────────
    // 1.20.5 made NbtUtils#readBlockPos optional-returning and key-based.

    @Nullable
    public static net.minecraft.core.BlockPos readBlockPos(CompoundTag tag, String key) {
        //? if >=1.21.5
        //return tag.read(key, net.minecraft.core.BlockPos.CODEC).orElse(null);
        //? if >=1.20.5 && <1.21.5
        //return net.minecraft.nbt.NbtUtils.readBlockPos(tag, key).orElse(null);
        //? if <1.20.5
        return contains(tag, key, Tag.TAG_COMPOUND) ? net.minecraft.nbt.NbtUtils.readBlockPos(getCompound(tag, key)) : null;
    }

    // ── CompoundTag getters (1.21.5 Optional rewrite) ──────────────────────────
    // 1.21.5 made every CompoundTag getX(key) return Optional<X>; the *Or(key,default) forms
    // restore the old primitive-with-zero-default behaviour. getCompound/getList lost their
    // non-optional forms (→ getCompoundOrEmpty / getListOrEmpty, and getList dropped its type
    // arg), and the 2-arg contains(key,type) collapsed to 1-arg contains(key). Every erroring
    // call site is routed through here so it stays era-agnostic — and 1.21.6's ValueInput can
    // reuse the same helper shape.

    public static int getInt(CompoundTag t, String k) {
        //? if >=1.21.5
        //return t.getIntOr(k, 0);
        //? if <1.21.5
        return t.getInt(k);
    }

    public static boolean getBoolean(CompoundTag t, String k) {
        //? if >=1.21.5
        //return t.getBooleanOr(k, false);
        //? if <1.21.5
        return t.getBoolean(k);
    }

    public static float getFloat(CompoundTag t, String k) {
        //? if >=1.21.5
        //return t.getFloatOr(k, 0.0F);
        //? if <1.21.5
        return t.getFloat(k);
    }

    public static double getDouble(CompoundTag t, String k) {
        //? if >=1.21.5
        //return t.getDoubleOr(k, 0.0D);
        //? if <1.21.5
        return t.getDouble(k);
    }

    public static String getString(CompoundTag t, String k) {
        //? if >=1.21.5
        //return t.getStringOr(k, "");
        //? if <1.21.5
        return t.getString(k);
    }

    public static byte getByte(CompoundTag t, String k) {
        //? if >=1.21.5
        //return t.getByteOr(k, (byte) 0);
        //? if <1.21.5
        return t.getByte(k);
    }

    public static long getLong(CompoundTag t, String k) {
        //? if >=1.21.5
        //return t.getLongOr(k, 0L);
        //? if <1.21.5
        return t.getLong(k);
    }

    public static CompoundTag getCompound(CompoundTag t, String k) {
        //? if >=1.21.5
        //return t.getCompoundOrEmpty(k);
        //? if <1.21.5
        return t.getCompound(k);
    }

    // ListTag element accessors also went Optional at 1.21.5 (getCompound(int)/getString(int)/…).
    public static CompoundTag getCompound(net.minecraft.nbt.ListTag t, int i) {
        //? if >=1.21.5
        //return t.getCompoundOrEmpty(i);
        //? if <1.21.5
        return t.getCompound(i);
    }

    public static String getString(net.minecraft.nbt.ListTag t, int i) {
        //? if >=1.21.5
        //return t.getStringOr(i, "");
        //? if <1.21.5
        return t.getString(i);
    }

    public static double getDouble(net.minecraft.nbt.ListTag t, int i) {
        //? if >=1.21.5
        //return t.getDoubleOr(i, 0.0D);
        //? if <1.21.5
        return t.getDouble(i);
    }

    public static float getFloat(net.minecraft.nbt.ListTag t, int i) {
        //? if >=1.21.5
        //return t.getFloatOr(i, 0.0F);
        //? if <1.21.5
        return t.getFloat(i);
    }

    public static int getInt(net.minecraft.nbt.ListTag t, int i) {
        //? if >=1.21.5
        //return t.getIntOr(i, 0);
        //? if <1.21.5
        return t.getInt(i);
    }

    // Old getList(key, type); the type arg is meaningless on >=1.21.5 (getListOrEmpty drops it)
    // but kept in the signature so call sites don't have to change arity.
    public static net.minecraft.nbt.ListTag getList(CompoundTag t, String k, int type) {
        //? if >=1.21.5
        //return t.getListOrEmpty(k);
        //? if <1.21.5
        return t.getList(k, type);
    }

    // 2-arg contains(key, type) → 1-arg contains(key); the type arg is ignored on >=1.21.5.
    public static boolean contains(CompoundTag t, String k, int type) {
        //? if >=1.21.5
        //return t.contains(k);
        //? if <1.21.5
        return t.contains(k, type);
    }

    // ── UUID in NBT (1.21.5 removed CompoundTag put/get/hasUUID) ────────────────
    // Stored via the UUID codec now. Callers historically guard getUUID with hasUUID, so a
    // null default on absence matches the old "only read when present" usage.

    @Nullable
    public static java.util.UUID getUUID(CompoundTag t, String k) {
        //? if >=1.21.5
        //return t.read(k, net.minecraft.core.UUIDUtil.CODEC).orElse(null);
        //? if <1.21.5
        return t.getUUID(k);
    }

    public static void putUUID(CompoundTag t, String k, java.util.UUID uuid) {
        //? if >=1.21.5
        //t.store(k, net.minecraft.core.UUIDUtil.CODEC, uuid);
        //? if <1.21.5
        t.putUUID(k, uuid);
    }

    public static boolean hasUUID(CompoundTag t, String k) {
        //? if >=1.21.5
        //return t.read(k, net.minecraft.core.UUIDUtil.CODEC).isPresent();
        //? if <1.21.5
        return t.hasUUID(k);
    }

    // NbtUtils.createUUID(UUID)/loadUUID(Tag) were removed at 1.21.5 — use the UUID codec.
    public static net.minecraft.nbt.Tag createUUID(java.util.UUID uuid) {
        //? if >=1.21.5
        //return net.minecraft.core.UUIDUtil.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, uuid).getOrThrow();
        //? if <1.21.5
        return net.minecraft.nbt.NbtUtils.createUUID(uuid);
    }

    public static java.util.UUID loadUUID(net.minecraft.nbt.Tag tag) {
        //? if >=1.21.5
        //return net.minecraft.core.UUIDUtil.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, tag).getOrThrow();
        //? if <1.21.5
        return net.minecraft.nbt.NbtUtils.loadUUID(tag);
    }

    // NbtUtils.writeBlockPos(BlockPos) was removed at 1.21.5 — use the BlockPos codec.
    public static net.minecraft.nbt.Tag writeBlockPos(net.minecraft.core.BlockPos pos) {
        //? if >=1.21.5
        //return net.minecraft.core.BlockPos.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, pos).getOrThrow();
        //? if <1.21.5
        return net.minecraft.nbt.NbtUtils.writeBlockPos(pos);
    }

    // ── Raw-tag accessors (era-neutral so the ValueInput/ValueOutput overloads below can shadow) ──
    // CompoundTag#contains(String), #put(String, Tag) and #get(String) exist in every era, but
    // ValueInput/ValueOutput (1.21.6) have no equivalent at all, so the call sites inside a
    // save/load body have to be routed through a helper to stay era-agnostic. Overload resolution
    // does the era switch for free: the parameter's static type flips with the node.

    public static boolean contains(CompoundTag t, String k) {
        return t.contains(k);
    }

    public static void put(CompoundTag t, String k, net.minecraft.nbt.Tag v) {
        t.put(k, v);
    }

    @Nullable
    public static net.minecraft.nbt.Tag getTag(CompoundTag t, String k) {
        return t.get(k);
    }

    // ── ValueInput / ValueOutput (1.21.6) ──────────────────────────────────────
    // 1.21.6 replaced the CompoundTag parameter of every save/load hook with the ValueOutput /
    // ValueInput interfaces. Their put*/get*Or method names are IDENTICAL to 1.21.5's CompoundTag,
    // so a body that already writes `compound.putInt(...)` needs no change; what does need work is
    // everything CompoundTag-shaped that the interfaces dropped — nested tags, lists, contains,
    // UUIDs — which is what these overloads cover. The declarations themselves are rewritten by
    // the `!mc2106-*-decl` Stonecutter rules in stonecutter.gradle.kts.
    //? if >=1.21.6 {
    /*// ValueInput/Output speak codecs only, so a raw Tag round-trips through PASSTHROUGH (both
    // sides are NbtOps-backed, so the convert is a no-op in practice).
    private static final com.mojang.serialization.Codec<net.minecraft.nbt.Tag> AM_TAG_CODEC =
            com.mojang.serialization.Codec.PASSTHROUGH.xmap(
                    d -> d.convert(net.minecraft.nbt.NbtOps.INSTANCE).getValue(),
                    t -> new com.mojang.serialization.Dynamic<>(net.minecraft.nbt.NbtOps.INSTANCE, t));

    public static int getInt(net.minecraft.world.level.storage.ValueInput t, String k) {
        return t.getIntOr(k, 0);
    }

    public static boolean getBoolean(net.minecraft.world.level.storage.ValueInput t, String k) {
        return t.getBooleanOr(k, false);
    }

    public static float getFloat(net.minecraft.world.level.storage.ValueInput t, String k) {
        return t.getFloatOr(k, 0.0F);
    }

    public static double getDouble(net.minecraft.world.level.storage.ValueInput t, String k) {
        return t.getDoubleOr(k, 0.0D);
    }

    public static String getString(net.minecraft.world.level.storage.ValueInput t, String k) {
        return t.getStringOr(k, "");
    }

    public static byte getByte(net.minecraft.world.level.storage.ValueInput t, String k) {
        return t.getByteOr(k, (byte) 0);
    }

    public static long getLong(net.minecraft.world.level.storage.ValueInput t, String k) {
        return t.getLongOr(k, 0L);
    }

    // Nested data is still handed around as a CompoundTag inside this mod (swallowed mobs, bucket
    // tags, per-player transmutation state), so these read the raw tag back out rather than
    // returning ValueInput#childOrEmpty.
    public static CompoundTag getCompound(net.minecraft.world.level.storage.ValueInput t, String k) {
        return t.read(k, CompoundTag.CODEC).orElseGet(CompoundTag::new);
    }

    public static net.minecraft.nbt.ListTag getList(net.minecraft.world.level.storage.ValueInput t, String k, int type) {
        net.minecraft.nbt.Tag raw = t.read(k, AM_TAG_CODEC).orElse(null);
        return raw instanceof net.minecraft.nbt.ListTag list ? list : new net.minecraft.nbt.ListTag();
    }

    public static boolean contains(net.minecraft.world.level.storage.ValueInput t, String k) {
        return t.read(k, AM_TAG_CODEC).isPresent();
    }

    public static boolean contains(net.minecraft.world.level.storage.ValueInput t, String k, int type) {
        return contains(t, k);
    }

    @Nullable
    public static net.minecraft.nbt.Tag getTag(net.minecraft.world.level.storage.ValueInput t, String k) {
        return t.read(k, AM_TAG_CODEC).orElse(null);
    }

    public static void put(net.minecraft.world.level.storage.ValueOutput t, String k, net.minecraft.nbt.Tag v) {
        t.store(k, AM_TAG_CODEC, v);
    }

    @Nullable
    public static java.util.UUID getUUID(net.minecraft.world.level.storage.ValueInput t, String k) {
        return t.read(k, net.minecraft.core.UUIDUtil.CODEC).orElse(null);
    }

    public static boolean hasUUID(net.minecraft.world.level.storage.ValueInput t, String k) {
        return t.read(k, net.minecraft.core.UUIDUtil.CODEC).isPresent();
    }

    public static void putUUID(net.minecraft.world.level.storage.ValueOutput t, String k, java.util.UUID uuid) {
        t.store(k, net.minecraft.core.UUIDUtil.CODEC, uuid);
    }

    @Nullable
    public static net.minecraft.core.BlockPos readBlockPos(net.minecraft.world.level.storage.ValueInput t, String k) {
        return t.read(k, net.minecraft.core.BlockPos.CODEC).orElse(null);
    }

    // ContainerHelper lost its HolderLookup.Provider parameter — the ValueOutput carries the
    // registry context itself. The provider stays in the signature so call sites don't change.
    public static void saveAllItems(net.minecraft.core.HolderLookup.Provider provider, net.minecraft.world.level.storage.ValueOutput t, net.minecraft.core.NonNullList<ItemStack> items) {
        net.minecraft.world.ContainerHelper.saveAllItems(t, items);
    }

    public static void loadAllItems(net.minecraft.core.HolderLookup.Provider provider, net.minecraft.world.level.storage.ValueInput t, net.minecraft.core.NonNullList<ItemStack> items) {
        net.minecraft.world.ContainerHelper.loadAllItems(t, items);
    }
    *///?}

    // ── CompoundTag <-> ValueInput/ValueOutput bridges ─────────────────────────
    // A handful of places drive an entity's save/load by hand with a CompoundTag they own: the
    // bucketable fish, the mobs that stash a swallowed entity, the "clear DeathLootTable so this
    // kill drops nothing" trick, and the platypus/terrapin/… nested "<Mob>Data" sub-tags. From
    // 1.21.6 the hooks want a ValueOutput/ValueInput, so wrap the tag in the vanilla adapters
    // (ProblemReporter.DISCARDING — these are throwaway round-trips, not world saves).

    public static void saveAdditionalTo(net.minecraft.world.entity.LivingEntity e, CompoundTag tag) {
        ((IEntitySaveDataAccessor) e).am_writeSaveData(tag);
    }

    public static void readAdditionalFrom(net.minecraft.world.entity.LivingEntity e, CompoundTag tag) {
        ((IEntitySaveDataAccessor) e).am_readSaveData(tag);
    }

    /**
     * Whole-entity save/load — {@code Entity#save} and {@code Entity#load} are public in every era,
     * so unlike the two above these need no mixin. The leafcutter anthill stores its ants this way.
     */
    public static boolean saveEntity(net.minecraft.world.entity.Entity e, CompoundTag tag) {
        //? if >=1.21.6 {
        /*net.minecraft.world.level.storage.TagValueOutput out = net.minecraft.world.level.storage.TagValueOutput
                .createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, e.registryAccess());
        boolean saved = e.save(out);
        tag.merge(out.buildResult());
        return saved;
        *///?} else {
        return e.save(tag);
        //?}
    }

    public static void loadEntity(net.minecraft.world.entity.Entity e, CompoundTag tag) {
        //? if >=1.21.6 {
        /*e.load(net.minecraft.world.level.storage.TagValueInput
                .create(net.minecraft.util.ProblemReporter.DISCARDING, e.registryAccess(), tag));
        *///?} else {
        e.load(tag);
        //?}
    }

    /**
     * 1.21.6 gave {@code Entity#canBeCollidedWith} the entity doing the colliding, so a mob can
     * answer per-collider. The overrides take the parameter through a Stonecutter decl rule; this
     * covers the one call site, where the collider is in scope but the era is not.
     */
    public static boolean canBeCollidedWith(net.minecraft.world.entity.Entity target, net.minecraft.world.entity.Entity collider) {
        //? if >=1.21.6 {
        /*return target.canBeCollidedWith(collider);
        *///?} else {
        return target.canBeCollidedWith();
        //?}
    }

    /**
     * Whether this entity is far enough through its own constructor to have synched data.
     *
     * <p>Vanilla never asks an entity whether it can be collided with before it is built — the
     * 26.2 {@code Entity} constructor contains no call to {@code canBeCollidedWith} at all, and
     * assigns {@code entityData} near its end. <b>Moonrise</b> does: its chunk-system patch caches
     * a "hard colliding" flag from {@code ChunkSystemEntity#isHardCollidingUncached}, injected at
     * {@code Entity.<init>} <i>before</i> that assignment. Every override of ours that reads
     * synched state — {@code isAlive()} reads health — then dereferences a null
     * {@code SynchedEntityData} and takes the client down with it. Reported against {@code 2.0.7}
     * on 26.2/NeoForge: opening the animal dictionary built a display laviathan and crashed on the
     * spot.</p>
     *
     * <p>Returning {@code false} that early is not a behaviour change. Health is assigned in
     * {@code LivingEntity}'s constructor, i.e. after {@code Entity}'s has returned, so an
     * {@code isAlive()} that survived would have read zero and answered {@code false} anyway.</p>
     *
     * <p>⚠️ This guard belongs on every {@code canBeCollidedWith} override that reads entity
     * state. Nothing in the compiler or the 49-node gates can see a missing one — it needs
     * Moonrise installed to fire at all.</p>
     */
    public static boolean isFullyConstructed(net.minecraft.world.entity.Entity entity) {
        return entity.getEntityData() != null;
    }

    /**
     * {@code BlockEntity#loadCustomOnly} took a (CompoundTag, Provider) pair until 1.21.6 folded
     * both into a ValueInput. Used by AMBlockItem to re-apply a stashed BlockEntityTag.
     */
    public static void loadCustomOnly(net.minecraft.world.level.block.entity.BlockEntity be, CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        //? if >=1.21.6 {
        /*be.loadCustomOnly(net.minecraft.world.level.storage.TagValueInput
                .create(net.minecraft.util.ProblemReporter.DISCARDING, provider, tag));
        *///?} elif >=1.20.5 {
        /*be.loadCustomOnly(tag, provider);
        *///?} else {
        be.load(tag);
        //?}
    }

    /**
     * The registry context a BlockEntity can reach on its own. 1.21.6 dropped the Provider
     * parameter from saveAdditional, so the `provider` local the Stonecutter decl rule injects has
     * to come from the block entity's level. Falls back to the empty registry access for a
     * level-less block entity, which cannot happen on the vanilla save path.
     */
    public static net.minecraft.core.HolderLookup.Provider lookupOf(net.minecraft.world.level.block.entity.BlockEntity be) {
        return be.getLevel() != null ? be.getLevel().registryAccess() : net.minecraft.core.RegistryAccess.EMPTY;
    }

    /**
     * "Which slot would this stack be worn in." {@code LivingEntity#getEquipmentSlotForItem} was
     * static until 1.21 turned it into an instance method (so a mob can override where it wears
     * things). No caller here has an entity to ask, so 1.21 goes through {@code Equipable}, which
     * is exactly what the old static did.
     */
    public static net.minecraft.world.entity.EquipmentSlot equipmentSlotFor(ItemStack stack) {
        //? if >=1.21.2 {
        /*net.minecraft.world.item.equipment.Equippable equippable = stack.get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
        return equippable == null ? net.minecraft.world.entity.EquipmentSlot.MAINHAND : equippable.slot();
        *///?} elif >=1.21 {
        /*net.minecraft.world.item.Equipable equipable = net.minecraft.world.item.Equipable.get(stack);
        return equipable == null ? net.minecraft.world.entity.EquipmentSlot.MAINHAND : equipable.getEquipmentSlot();
        *///?} else {
        return LivingEntity.getEquipmentSlotForItem(stack);
        //?}
    }

    /**
     * 1.21.5 removed the ArmorItem class, so `getItem() instanceof ArmorItem` no longer compiles.
     * An item is humanoid armour iff its equippable component targets a head/chest/legs/feet slot.
     */
    public static boolean isArmor(ItemStack stack) {
        //? if >=1.21.5 {
        /*net.minecraft.world.entity.EquipmentSlot slot = equipmentSlotFor(stack);
        return slot == net.minecraft.world.entity.EquipmentSlot.HEAD || slot == net.minecraft.world.entity.EquipmentSlot.CHEST
                || slot == net.minecraft.world.entity.EquipmentSlot.LEGS || slot == net.minecraft.world.entity.EquipmentSlot.FEET;
        *///?} else {
        return stack.getItem() instanceof net.minecraft.world.item.ArmorItem;
        //?}
    }

    /**
     * {@code ItemCooldowns#addCooldown} took an {@code Item} until 1.21.2, when it moved to an
     * {@code ItemStack} (cooldowns became per-stack via a cooldown-group component).
     */
    public static void addCooldown(net.minecraft.world.item.ItemCooldowns cooldowns, net.minecraft.world.item.Item item, int ticks) {
        //? if >=1.21.2 {
        /*cooldowns.addCooldown(new ItemStack(item), ticks);
        *///?} else {
        cooldowns.addCooldown(item, ticks);
        //?}
    }

    /**
     * {@code ItemCooldowns#isOnCooldown} took an {@code Item} until 1.21.2, when it moved to an
     * {@code ItemStack} (same per-stack cooldown-group change as {@link #addCooldown}).
     */
    public static boolean isOnCooldown(net.minecraft.world.item.ItemCooldowns cooldowns, net.minecraft.world.item.Item item) {
        //? if >=1.21.2 {
        /*return cooldowns.isOnCooldown(new ItemStack(item));
        *///?} else {
        return cooldowns.isOnCooldown(item);
        //?}
    }

    // ── Creature types ─────────────────────────────────────────────────────────
    // 1.20.5 deleted MobType in favour of entity-type tags. AM's own mobs join those tags
    // through data/minecraft/tags/entity_types/*.json instead of a getMobType() override.

    public static boolean isUndead(LivingEntity entity) {
        //? if >=1.20.5 {
        /*return entity.getType().builtInRegistryHolder().is(net.minecraft.tags.EntityTypeTags.UNDEAD);
        *///?} else {
        return entity.getMobType() == net.minecraft.world.entity.MobType.UNDEAD;
        //?}
    }

    public static boolean isArthropod(LivingEntity entity) {
        //? if >=1.20.5 {
        /*return entity.getType().builtInRegistryHolder().is(net.minecraft.tags.EntityTypeTags.ARTHROPOD);
        *///?} else {
        return entity.getMobType() == net.minecraft.world.entity.MobType.ARTHROPOD;
        //?}
    }

    public static boolean isAquatic(LivingEntity entity) {
        //? if >=1.20.5 {
        /*return entity.getType().builtInRegistryHolder().is(net.minecraft.tags.EntityTypeTags.AQUATIC);
        *///?} else {
        return entity.getMobType() == net.minecraft.world.entity.MobType.WATER;
        //?}
    }

    /**
     * Sharpness/Bane-of-Arthropods style bonus damage. 1.21 folded it into the enchantment
     * effect system: there is no "what would this weapon add" query any more, only "apply the
     * whole attack". The whip already deals its own damage, so on 1.21 the bonus is zero and
     * {@link #enchantDamageEffects} runs the post-attack effects instead.
     */
    public static float getDamageBonus(net.minecraft.world.item.ItemStack weapon, LivingEntity target) {
        //? if >=1.21 {
        /*return 0.0F;
        *///?}
        //? if >=1.20.5 && <1.21 {
        /*return net.minecraft.world.item.enchantment.EnchantmentHelper.getDamageBonus(weapon, target.getType());
        *///?}
        //? if <1.20.5
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getDamageBonus(weapon, target.getMobType());
    }

    // ── Enchantments ───────────────────────────────────────────────────────────
    // 1.21 made enchantments datapack-driven: Enchantment became a final record, the
    // Enchantments constants are ResourceKeys, and EnchantmentHelper takes a Holder that only a
    // level's registry access can resolve. Alex's Mobs only ever asks "what level is X here",
    // so every such question goes through these two.

    //? if >=1.21 {
    /*public static int enchantLevel(net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key, ItemStack stack, net.minecraft.world.level.LevelReader level) {
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(enchantHolder(key, level), stack);
    }

    public static int enchantLevel(net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key, LivingEntity entity) {
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(enchantHolder(key, entity.level()), entity);
    }

    private static net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> enchantHolder(net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key, net.minecraft.world.level.LevelReader level) {
        return level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getHolderOrThrow(key);
    }
    *///?} else {
    public static int enchantLevel(net.minecraft.world.item.enchantment.Enchantment enchantment, ItemStack stack, net.minecraft.world.level.LevelReader level) {
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack);
    }

    public static int enchantLevel(net.minecraft.world.item.enchantment.Enchantment enchantment, LivingEntity entity) {
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(enchantment, entity);
    }
    //?}

    /**
     * Runs an attacker's on-hit enchantment effects. 1.21 moved this off {@code LivingEntity}
     * onto {@code EnchantmentHelper}, which needs the server level and the damage source.
     */
    public static void enchantDamageEffects(LivingEntity attacker, Entity target) {
        //? if >=1.21 {
        /*if (attacker.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            net.minecraft.world.item.enchantment.EnchantmentHelper.doPostAttackEffects(serverLevel, target, attacker.damageSources().mobAttack(attacker));
        }
        *///?} else {
        attacker.doEnchantDamageEffects(attacker, target);
        //?}
    }

    // ── Registry holders ───────────────────────────────────────────────────────
    // 1.20.5 put effects behind Holders everywhere, but AM's DeferredRegister handles
    // still hand out the bare MobEffect, so wrap it on the way out of the registry.

    //? if >=1.20.5 {
    /*public static net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect(net.minecraft.world.effect.MobEffect effect) {
        return net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);
    }
    *///?} else {
    public static net.minecraft.world.effect.MobEffect effect(net.minecraft.world.effect.MobEffect effect) {
        return effect;
    }
    //?}

    // …and the inverse, for the places that want the bare MobEffect back out of an instance.
    public static net.minecraft.world.effect.MobEffect rawEffect(net.minecraft.world.effect.MobEffectInstance instance) {
        //? if >=1.20.5 {
        /*return instance.getEffect().value();
        *///?} else {
        return instance.getEffect();
        //?}
    }

    // ── Taming ─────────────────────────────────────────────────────────────────
    // 1.20.5 split the taming side effects (max health / attack damage) out into a flag.
    // Passing true keeps the pre-1.20.5 behaviour, which applied them unconditionally.

    public static void setTame(net.minecraft.world.entity.TamableAnimal animal, boolean tame) {
        //? if >=1.20.5 {
        /*animal.setTame(tame, true);
        *///?} else {
        animal.setTame(tame);
        //?}
    }

    // ── Riding ─────────────────────────────────────────────────────────────────
    // 1.21.9 made Entity#startRiding(Entity) final and DELETED the two-argument
    // startRiding(Entity, boolean force) outright; the one override point left is
    // startRiding(Entity, boolean force, boolean postGameEvent). Passing true for the new flag
    // reproduces the old behaviour — the two-arg form used to post the mount game event too.
    // (The three part-entity classes that OVERRIDE the single-argument form are handled by the
    // !mc2109-startriding-decl/-super replacement rules instead: an override has to change
    // signature in place and cannot be delegated to a helper.)

    public static boolean startRiding(Entity rider, Entity vehicle, boolean force) {
        //? if >=1.21.9 {
        /*return rider.startRiding(vehicle, force, true);
        *///?} else {
        return rider.startRiding(vehicle, force);
        //?}
    }

    /**
     * The mobs this mod lets ride a <b>player</b>, and therefore the ones that have to be waved
     * past 1.21.2's {@code !vehicle.type.canSerialize()} guard in {@code Entity#startRiding} —
     * {@code EntityType.PLAYER} is {@code noSave()}, so that guard rejects every one of them on
     * the server. Consumed by {@code mixin/EntityMixin}, which is where the whole story is
     * written up; see also docs/notes/bug-reports.md #81.
     *
     * <p>Two latchers (crimson mosquito, enderiophage) and five shoulder-riders (bald eagle,
     * crow, capuchin monkey, potoo, sugar glider). Every other {@code startRiding} call in this
     * mod puts a rider on one of <i>our</i> entities, which serialize normally, so they are not
     * listed and stay subject to the vanilla check.
     */
    public static boolean ridesUnsaveableVehicles(Entity rider) {
        return rider instanceof com.github.alexthe666.alexsmobs.entity.EntityCrimsonMosquito
                || rider instanceof com.github.alexthe666.alexsmobs.entity.EntityEnderiophage
                || rider instanceof com.github.alexthe666.alexsmobs.entity.EntityBaldEagle
                || rider instanceof com.github.alexthe666.alexsmobs.entity.EntityCrow
                || rider instanceof com.github.alexthe666.alexsmobs.entity.EntityCapuchinMonkey
                || rider instanceof com.github.alexthe666.alexsmobs.entity.EntityPotoo
                || rider instanceof com.github.alexthe666.alexsmobs.entity.EntitySugarGlider;
    }

    // ── Pathfinding ────────────────────────────────────────────────────────────
    // 1.20.5 dropped the level/pos arguments: a block state alone decides now.

    public static boolean isPathfindable(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos, net.minecraft.world.level.pathfinder.PathComputationType type) {
        //? if >=1.20.5 {
        /*return state.isPathfindable(type);
        *///?} else {
        return state.isPathfindable(level, pos, type);
        //?}
    }

    // ── Entity dimensions ──────────────────────────────────────────────────────
    // 1.20.5 turned EntityDimensions into a record, so the fields became accessors.

    public static float width(net.minecraft.world.entity.EntityDimensions dimensions) {
        //? if >=1.20.5 {
        /*return dimensions.width();
        *///?} else {
        return dimensions.width;
        //?}
    }

    public static float height(net.minecraft.world.entity.EntityDimensions dimensions) {
        //? if >=1.20.5 {
        /*return dimensions.height();
        *///?} else {
        return dimensions.height;
        //?}
    }

    // ── Step height ────────────────────────────────────────────────────────────
    // 1.20.5 turned step height into the minecraft:step_height attribute.

    public static void setMaxUpStep(Entity entity, float value) {
        //? if >=1.20.5 {
        /*if (entity instanceof LivingEntity living) {
            net.minecraft.world.entity.ai.attributes.AttributeInstance instance = living.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT);
            if (instance != null) {
                instance.setBaseValue(value);
            }
        }
        *///?} else {
        entity.setMaxUpStep(value);
        //?}
    }

    // 1.20.5 rewrote WalkNodeEvaluator's static lookup to take a PathfindingContext instead
    // of a level. Every Alex's Mobs caller has the mob to hand, so route through the
    // mob-based overload, which builds the context itself.
    public static net.minecraft.world.level.pathfinder.BlockPathTypes pathTypeStatic(net.minecraft.world.entity.Mob mob, net.minecraft.core.BlockPos pos) {
        //? if >=1.20.5 {
        /*return net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getPathTypeStatic(mob, pos);
        *///?} else {
        return net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getBlockPathTypeStatic(mob.level(), pos.mutable());
        //?}
    }

    // ── Item attribute modifiers ───────────────────────────────────────────────
    // 1.20.5 turned an item's attribute modifiers into a data component keyed by
    // EquipmentSlotGroup and made Attributes.* holders, so the old
    // ItemStack#getAttributeModifiers(EquipmentSlot) multimap is gone. These four helpers
    // cover every Alex's Mobs use: summing one attribute for a slot, and swapping an
    // equipped item's modifiers on and off a mob.

    public static double attackDamageOf(net.minecraft.world.item.ItemStack stack, net.minecraft.world.entity.EquipmentSlot slot) {
        //? if >=1.20.5 {
        /*double[] total = {0.0D};
        stack.forEachModifier(slot, (attribute, modifier) -> {
            if (attribute == net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) {
                total[0] += modifier.amount();
            }
        });
        return total[0];
        *///?} else {
        double total = 0.0D;
        for (net.minecraft.world.entity.ai.attributes.AttributeModifier modifier : stack.getAttributeModifiers(slot).get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)) {
            total += modifier.getAmount();
        }
        return total;
        //?}
    }

    public static double armorOf(net.minecraft.world.item.ItemStack stack, net.minecraft.world.entity.EquipmentSlot slot) {
        //? if >=1.20.5 {
        /*double[] total = {0.0D};
        stack.forEachModifier(slot, (attribute, modifier) -> {
            if (attribute == net.minecraft.world.entity.ai.attributes.Attributes.ARMOR) {
                total[0] += modifier.amount();
            }
        });
        return total[0];
        *///?} else {
        double total = 0.0D;
        for (net.minecraft.world.entity.ai.attributes.AttributeModifier modifier : stack.getAttributeModifiers(slot).get(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR)) {
            total += modifier.getAmount();
        }
        return total;
        //?}
    }

    public static void removeItemModifiers(LivingEntity entity, net.minecraft.world.item.ItemStack stack, net.minecraft.world.entity.EquipmentSlot slot) {
        //? if >=1.20.5 {
        /*stack.forEachModifier(slot, (attribute, modifier) -> {
            net.minecraft.world.entity.ai.attributes.AttributeInstance instance = entity.getAttributes().getInstance(attribute);
            if (instance != null) {
                instance.removeModifier(modifier.id());
            }
        });
        *///?} else {
        entity.getAttributes().removeAttributeModifiers(stack.getAttributeModifiers(slot));
        //?}
    }

    public static void addItemModifiers(LivingEntity entity, net.minecraft.world.item.ItemStack stack, net.minecraft.world.entity.EquipmentSlot slot) {
        //? if >=1.20.5 {
        /*stack.forEachModifier(slot, (attribute, modifier) -> {
            net.minecraft.world.entity.ai.attributes.AttributeInstance instance = entity.getAttributes().getInstance(attribute);
            if (instance != null) {
                instance.removeModifier(modifier.id());
                instance.addTransientModifier(modifier);
            }
        });
        *///?} else {
        entity.getAttributes().addTransientAttributeModifiers(stack.getAttributeModifiers(slot));
        //?}
    }

    // ── Item damage ────────────────────────────────────────────────────────────
    // 1.20.5 replaced the "here is what to run when it breaks" consumer with an
    // EquipmentSlot: the break event and Forge's destroy hook both fire internally now.

    public static void hurtAndBreak(net.minecraft.world.item.ItemStack stack, int amount, LivingEntity entity, net.minecraft.world.entity.EquipmentSlot slot) {
        //? if >=1.20.5 {
        /*stack.hurtAndBreak(amount, entity, slot);
        *///?} else {
        stack.hurtAndBreak(amount, entity, breaker -> breaker.broadcastBreakEvent(slot));
        //?}
    }

    public static void hurtAndBreak(net.minecraft.world.item.ItemStack stack, int amount, LivingEntity entity, net.minecraft.world.InteractionHand hand) {
        hurtAndBreak(stack, amount, entity, hand == net.minecraft.world.InteractionHand.MAIN_HAND
                ? net.minecraft.world.entity.EquipmentSlot.MAINHAND
                : net.minecraft.world.entity.EquipmentSlot.OFFHAND);
    }

    // ── Shearing ───────────────────────────────────────────────────────────────
    // Vanilla has never had a generic "shears used on a mob" hook on any version in this range:
    // the entity hook on the shears item is a Forge/NeoForge patch (present on every loader
    // build here — below Forge 65 it dispatches on the loader's own shearable interface, from
    // Forge 65 on vanilla's Shearable), and vanilla shears its own mobs from inside each one's
    // mobInteract. Fabric has neither, so the four mobs here that implement Shearable need the
    // same call site Sheep has. Mirrors Sheep#mobInteract: the PLAYERS sound source and one
    // point of damage to the shears, which hurtAndBreak already skips in creative.
    //
    // 1.21.2 grew a ServerLevel and the shearing stack onto Shearable#shear; the
    // !mc2102-shear-decl rule rewrites the four declarations, and this is their only call.
    // Answers true on the client without doing anything, so the caller can hand back a sided
    // success and get the arm swing.
    public static <T extends net.minecraft.world.entity.Mob & net.minecraft.world.entity.Shearable> boolean shearWithShears(
            T mob, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand, ItemStack stack) {
        if (!stack.is(net.minecraftforge.common.Tags.Items.SHEARS) || !mob.readyForShearing()) {
            return false;
        }
        if (mob.level().isClientSide()) {
            return true;
        }
        //? if >=1.21.2 {
        /*mob.shear((net.minecraft.server.level.ServerLevel) mob.level(), net.minecraft.sounds.SoundSource.PLAYERS, stack);
        *///?} else {
        mob.shear(net.minecraft.sounds.SoundSource.PLAYERS);
        //?}
        hurtAndBreak(stack, 1, player, hand);
        return true;
    }

    // 1.20.5 folded ItemStack#hurt(int, RandomSource, ServerPlayer) into hurtAndBreak, which
    // takes an "it broke" Runnable instead of returning a boolean. Every caller here ignored
    // the return value and had nothing to do on break, so an empty Runnable is faithful.
    // 1.21 swapped the RandomSource for a ServerLevel and the Runnable for a Consumer<Item>.
    // 1.21.6 removed ServerPlayer#serverLevel — level() is covariant and returns ServerLevel now.
    public static void hurtItem(net.minecraft.world.item.ItemStack stack, int amount, net.minecraft.util.RandomSource random, @Nullable net.minecraft.server.level.ServerPlayer player) {
        //? if >=1.21.6 {
        /*if (player != null) {
            stack.hurtAndBreak(amount, player.level(), player, item -> {});
        }
        *///?}
        //? if >=1.21 && <1.21.6 {
        /*if (player != null) {
            stack.hurtAndBreak(amount, player.serverLevel(), player, item -> {});
        }
        *///?}
        //? if >=1.20.5 && <1.21 {
        /*stack.hurtAndBreak(amount, random, player, () -> {});
        *///?}
        //? if <1.20.5
        stack.hurt(amount, random, player);
    }

    // 1.20.5 moved the block-entity payload BlockItem used to stash in the "BlockEntityTag"
    // NBT key into the block_entity_data data component, and dropped the static accessor.
    //
    // This mod keeps *writing* that payload as a "BlockEntityTag" sub-tag (addTagElement /
    // getOrCreateTagElement, which land in custom_data on 1.20.5+) so the source and the data pack
    // stay in one shape across every node, so read it back from there first. A stack that came
    // from vanilla — e.g. a shulker box picked up with a vanilla loot table — still carries the
    // real component, hence the fallback. AMBlockItem re-applies the sub-tag on placement.
    @Nullable
    public static CompoundTag getBlockEntityData(ItemStack stack) {
        // 1.21.9 retyped BLOCK_ENTITY_DATA from CustomData to TypedEntityData<BlockEntityType<?>>,
        // which carries the block-entity id separately — copyTagWithoutId() is the closest analogue
        // of copyTag() and is what vanilla itself reads the stashed data with.
        //? if >=1.21.9 {
        /*CompoundTag stashed = getTagElement(stack, "BlockEntityTag");
        if (stashed != null) {
            return stashed;
        }
        net.minecraft.world.item.component.TypedEntityData<net.minecraft.world.level.block.entity.BlockEntityType<?>> data =
                stack.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
        if (data == null) {
            return null;
        }
        CompoundTag copied = data.copyTagWithoutId();
        return copied.isEmpty() ? null : copied;
        *///?} elif >=1.20.5 {
        /*CompoundTag stashed = getTagElement(stack, "BlockEntityTag");
        if (stashed != null) {
            return stashed;
        }
        net.minecraft.world.item.component.CustomData data = stack.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
        return data == null || data.isEmpty() ? null : data.copyTag();
        *///?} else {
        return net.minecraft.world.item.BlockItem.getBlockEntityData(stack);
        //?}
    }

    // ── ItemStack over the wire ────────────────────────────────────────────────
    // 1.20.5 removed FriendlyByteBuf#readItem/writeItem: components need the registries, so
    // stacks now go through ItemStack.OPTIONAL_STREAM_CODEC over a RegistryFriendlyByteBuf.
    // Every call site here is a PLAY-protocol packet, whose payload buffer *is* one.

    public static ItemStack readItem(net.minecraft.network.FriendlyByteBuf buf) {
        //? if >=1.20.5 {
        /*return ItemStack.OPTIONAL_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf) buf);
        *///?} else {
        return buf.readItem();
        //?}
    }

    public static void writeItem(net.minecraft.network.FriendlyByteBuf buf, ItemStack stack) {
        //? if >=1.20.5 {
        /*ItemStack.OPTIONAL_STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf) buf, stack);
        *///?} else {
        buf.writeItem(stack);
        //?}
    }

    // 1.20.5 needs the registries to write a stack's components, and ItemStack#save gained a
    // Provider parameter. Used for the "one CompoundTag per inventory slot" pattern.
    //
    // #104: that Provider overload does NOT write into the tag it is handed. It encodes through
    // ItemStack.CODEC with the tag as the codec's *prefix*, and NbtOps#mergeToMap shallowCopy()s
    // a CompoundTag prefix and returns the copy -- read out of 1.21.1's NbtOps, and vanilla's own
    // AbstractChestedHorse#addAdditionalSaveData adds the RETURN value to its list for exactly
    // that reason. Discarding it left every slot tag holding nothing but its "Slot" byte, so the
    // kangaroo's, elephant's and catfish's inventories and the straddleboard's own stack came
    // back empty from a save on every node 1.20.5..1.21.5. Merging an unprefixed encode back into
    // the caller's tag is what the >=1.21.6 arm above already does.
    public static void saveInto(net.minecraft.core.HolderLookup.Provider provider, ItemStack stack, CompoundTag tag) {
        //? if >=1.21.6 {
        /*tag.merge((CompoundTag) ItemStack.CODEC
                .encodeStart(provider.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), stack).getOrThrow());
        *///?} elif >=1.20.5 {
        /*tag.merge((CompoundTag) stack.save(provider, new CompoundTag()));
        *///?} else {
        stack.save(tag);
        //?}
    }

    // 1.20.5 removed GoalSelector#getRunningGoals; the running set is now filtered off
    // getAvailableGoals. Copied first because Goal#stop can mutate the selector.
    public static void stopRunningGoals(net.minecraft.world.entity.ai.goal.GoalSelector selector) {
        //? if >=1.20.5 {
        /*for (net.minecraft.world.entity.ai.goal.WrappedGoal wrapped : java.util.List.copyOf(selector.getAvailableGoals())) {
            if (wrapped.isRunning()) {
                wrapped.stop();
            }
        }
        *///?} else {
        selector.getRunningGoals().forEach(net.minecraft.world.entity.ai.goal.Goal::stop);
        //?}
    }

    // FoodProperties became a record in 1.20.5.
    public static int nutrition(FoodProperties food) {
        //? if >=1.20.5
        //return food.nutrition();
        //? if <1.20.5
        return food.getNutrition();
    }

    // 1.20.5 turned SpawnPlacements.Type into the SpawnPlacementType interface and moved the
    // position check off NaturalSpawner onto it.
    public static boolean isSpawnPositionOnGround(net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos, net.minecraft.world.entity.EntityType<?> type) {
        //? if >=1.20.5
        //return net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND.isSpawnPositionOk(level, pos, type);
        //? if <1.20.5
        return net.minecraft.world.level.NaturalSpawner.isSpawnPositionOk(net.minecraft.world.entity.SpawnPlacements.Type.ON_GROUND, level, pos, type);
    }

    // ── The 1.21.2 ServerLevel thread ──────────────────────────────────────────
    // 1.21.2 pushed a ServerLevel parameter into the front of the server-only entity hooks.
    // The *overrides* get their signature rewritten by a Stonecutter replacement (see the
    // "!mc2102-*" rules in stonecutter.gradle.kts); these helpers exist for the *call sites*,
    // which mostly live in AI goals and item code where no ServerLevel is in scope.
    //
    // Every one of them is a server-side action, so the pre-1.21.2 behaviour is preserved by
    // simply doing nothing off-server — which is where the old code would have been a no-op or
    // a desync anyway.

    @Nullable
    private static net.minecraft.server.level.ServerLevel serverLevel(Entity entity) {
        return entity.level() instanceof net.minecraft.server.level.ServerLevel level ? level : null;
    }

    public static boolean isInvulnerableTo(Entity entity, net.minecraft.world.damagesource.DamageSource source) {
        //? if >=1.21.2 {
        /*net.minecraft.server.level.ServerLevel level = serverLevel(entity);
        if (level != null && entity instanceof net.minecraft.world.entity.LivingEntity living) {
            return living.isInvulnerableTo(level, source);
        }
        // 1.21.2 took the public isInvulnerableTo off Entity — only LivingEntity has one, and it
        // needs a ServerLevel. Entity#isInvulnerableToBase is the remaining check but it is
        // protected, so it is spelled out here for the multiparts/projectiles and for the
        // client-side callers, where there is no ServerLevel to hand at all.
        return entity.isRemoved() || entity.isInvulnerable() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY) && !source.isCreativePlayer();
        *///?} else {
        return entity.isInvulnerableTo(source);
        //?}
    }

    // ── InteractionResultHolder ────────────────────────────────────────────────
    // 1.21.2 deleted InteractionResultHolder<ItemStack> and folded it into InteractionResult:
    // Item#use returns a bare InteractionResult, and the "and here is the stack that should end
    // up in the hand" half became InteractionResult.Success#heldItemTransformedTo. PASS and FAIL
    // have no item slot at all now — which matches how the old holder was used, since a passing
    // or failing use never swapped the stack.
    //
    // The return type is the thing that differs, so these helpers are declared twice rather than
    // branching inside one body. Their call sites read identically on every node; the `use`
    // signatures themselves are rewritten by the "!mc2102-irh-*" replacement rules.
    //? if >=1.21.2 {
    /*public static net.minecraft.world.InteractionResult sidedSuccess(ItemStack stack, boolean isClientSide) {
        return net.minecraft.world.InteractionResult.SUCCESS.heldItemTransformedTo(stack);
    }

    public static net.minecraft.world.InteractionResult success(ItemStack stack) {
        return net.minecraft.world.InteractionResult.SUCCESS.heldItemTransformedTo(stack);
    }

    public static net.minecraft.world.InteractionResult consume(ItemStack stack) {
        return net.minecraft.world.InteractionResult.CONSUME.heldItemTransformedTo(stack);
    }

    public static net.minecraft.world.InteractionResult pass(ItemStack stack) {
        return net.minecraft.world.InteractionResult.PASS;
    }

    public static net.minecraft.world.InteractionResult fail(ItemStack stack) {
        return net.minecraft.world.InteractionResult.FAIL;
    }

    public static net.minecraft.world.InteractionResult holder(net.minecraft.world.InteractionResult result, ItemStack stack) {
        return result;
    }
    *///?} else {
    public static net.minecraft.world.InteractionResultHolder<ItemStack> sidedSuccess(ItemStack stack, boolean isClientSide) {
        return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, isClientSide);
    }

    public static net.minecraft.world.InteractionResultHolder<ItemStack> success(ItemStack stack) {
        return net.minecraft.world.InteractionResultHolder.success(stack);
    }

    public static net.minecraft.world.InteractionResultHolder<ItemStack> consume(ItemStack stack) {
        return net.minecraft.world.InteractionResultHolder.consume(stack);
    }

    public static net.minecraft.world.InteractionResultHolder<ItemStack> pass(ItemStack stack) {
        return net.minecraft.world.InteractionResultHolder.pass(stack);
    }

    public static net.minecraft.world.InteractionResultHolder<ItemStack> fail(ItemStack stack) {
        return net.minecraft.world.InteractionResultHolder.fail(stack);
    }

    public static net.minecraft.world.InteractionResultHolder<ItemStack> holder(net.minecraft.world.InteractionResult result, ItemStack stack) {
        return new net.minecraft.world.InteractionResultHolder<>(result, stack);
    }
    //?}

    // InteractionResult#sidedSuccess(boolean) went with it. SUCCESS is now the "swing on the
    // client, tell the server" result, which is exactly what sidedSuccess produced.
    public static net.minecraft.world.InteractionResult sidedSuccess(boolean isClientSide) {
        //? if >=1.21.2
        //return net.minecraft.world.InteractionResult.SUCCESS;
        //? if <1.21.2
        return net.minecraft.world.InteractionResult.sidedSuccess(isClientSide);
    }

    // ── Block right-click results ─────────────────────────────────────────────
    // 1.20.5 split BlockBehaviour#use into useItemOn (same position in the dispatch as the old
    // method) and useWithoutItem (a new second stage). Every block in this mod keeps its upstream
    // body in a mod-owned amUse returning a plain InteractionResult; this helper maps that onto
    // whatever useItemOn is declared to return on the node being built.
    //
    // 1.20.5-1.21.1 has a separate ItemInteractionResult enum with no plain PASS. The faithful
    // stand-in is SKIP_DEFAULT_BLOCK_INTERACTION: like upstream's PASS it does not consume and
    // does not add a block stage that 1.20.4 never had, so the held item still gets its useOn.
    // From 1.21.2 the enum is gone again and the mapping is the identity.
    //? if >=1.21.2 {
    /*public static net.minecraft.world.InteractionResult itemResult(net.minecraft.world.InteractionResult result) {
        return result;
    }
    *///?}
    //? if >=1.20.5 && <1.21.2 {
    /*public static net.minecraft.world.ItemInteractionResult itemResult(net.minecraft.world.InteractionResult result) {
        if (result == net.minecraft.world.InteractionResult.SUCCESS) {
            return net.minecraft.world.ItemInteractionResult.SUCCESS;
        }
        if (result == net.minecraft.world.InteractionResult.CONSUME) {
            return net.minecraft.world.ItemInteractionResult.CONSUME;
        }
        if (result == net.minecraft.world.InteractionResult.FAIL) {
            return net.minecraft.world.ItemInteractionResult.FAIL;
        }
        return net.minecraft.world.ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
    }
    *///?}

    // ── Build height ──────────────────────────────────────────────────────────
    // NOT a plain rename: the old getMaxBuildHeight() was *exclusive*, getMaxY() is *inclusive*,
    // so the modern branch has to add one to mean the same thing.
    public static int minBuildHeight(net.minecraft.world.level.LevelHeightAccessor level) {
        //? if >=1.21.2
        //return level.getMinY();
        //? if <1.21.2
        return level.getMinBuildHeight();
    }

    public static int maxBuildHeight(net.minecraft.world.level.LevelHeightAccessor level) {
        //? if >=1.21.2
        //return level.getMaxY() + 1;
        //? if <1.21.2
        return level.getMaxBuildHeight();
    }

    // ── Crafting remainder ────────────────────────────────────────────────────
    // 1.21.2 replaced Forge's ItemStack#hasCraftingRemainingItem/getCraftingRemainingItem pair
    // with a single vanilla ItemStack#getCraftingRemainder that returns EMPTY for "none".
    // ⚠️ The >=1.21.2 && <26 spelling `ItemStack#getCraftingRemainder()` is a LOADER extension, not
    // vanilla — javap on the unpatched named jar shows only `Item#getCraftingRemainder()` there. So
    // Fabric goes through the item. The difference is real but does not affect this mod: the loader
    // form lets an item vary its remainder per stack (a fluid-aware bucket), and every remainder
    // Alex's Mobs consumes is a plain item-level one.
    public static boolean hasCraftingRemainder(ItemStack stack) {
        //? if >=26
        //return stack.getItem().getCraftingRemainder() != null;
        //? if fabric && >=1.21.2 && <26
        //return !stack.getItem().getCraftingRemainder().isEmpty();
        //? if !fabric && >=1.21.2 && <26
        //return !stack.getCraftingRemainder().isEmpty();
        //? if fabric && <1.21.2
        //return stack.getItem().hasCraftingRemainingItem();
        //? if !fabric && <1.21.2
        return stack.hasCraftingRemainingItem();
    }

    // 26.1 moved the remainder onto Item and made it a nullable ItemStackTemplate; `create()` is
    // what vanilla's own crafting code calls on it.
    public static ItemStack craftingRemainder(ItemStack stack) {
        //? if >=26 {
        /*net.minecraft.world.item.ItemStackTemplate remainder = stack.getItem().getCraftingRemainder();
        return remainder != null ? remainder.create() : ItemStack.EMPTY;
        *///?}
        //? if fabric && >=1.21.2 && <26
        //return stack.getItem().getCraftingRemainder();
        //? if !fabric && >=1.21.2 && <26
        //return stack.getCraftingRemainder();
        //? if fabric && <1.21.2 {
        /*// Vanilla's pre-1.21.2 remainder is Item-level and NULLABLE; Forge's stack-level patch
        // returned EMPTY for "none". Normalise to EMPTY so every caller sees one contract.
        net.minecraft.world.item.Item remainder = stack.getItem().getCraftingRemainingItem();
        return remainder == null ? ItemStack.EMPTY : new ItemStack(remainder);
        *///?}
        //? if !fabric && <1.21.2
        return stack.getCraftingRemainingItem();
    }

    // ── Ingredient from a tag ─────────────────────────────────────────────────
    // Ingredient became HolderSet-backed in 1.21.2, so a tag has to be resolved to its holder
    // set first. BuiltInRegistries is the right lookup here: every tag this mod builds an
    // ingredient from is an item tag it (or vanilla) defines, never a datapack-only one.
    // A memoized, lazily-built ingredient. From 1.21.2 Ingredient is HolderSet-backed and building one
    // from a tag forces the tag to resolve (BuiltInRegistries.ITEM.getOrThrow) — which throws "Tags not
    // bound" if it happens at class-init, before tags load. The few entities that cached their tempt
    // ingredient in a static field must therefore defer construction to first use (always in-world, so
    // tags are bound). Harmless on older nodes, where the tag resolves lazily anyway.
    public static java.util.function.Supplier<net.minecraft.world.item.crafting.Ingredient> lazyIngredient(java.util.function.Supplier<net.minecraft.world.item.crafting.Ingredient> factory) {
        return new java.util.function.Supplier<>() {
            private net.minecraft.world.item.crafting.Ingredient cached;
            public net.minecraft.world.item.crafting.Ingredient get() {
                if (cached == null) cached = factory.get();
                return cached;
            }
        };
    }

    public static net.minecraft.world.item.crafting.Ingredient ingredientOf(net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tag) {
        //? if >=1.21.2
        //return net.minecraft.world.item.crafting.Ingredient.of(net.minecraft.core.registries.BuiltInRegistries.ITEM.getOrThrow(tag));
        //? if <1.21.2
        return net.minecraft.world.item.crafting.Ingredient.of(tag);
    }

    //? if >=1.21.2 {
    /*// From 1.21.2 an armour material's repair ingredient is a TagKey, and building an ArmorItem calls
    // ArmorMaterial#humanoidProperties -> Item.Properties#repairable(tag) -> ITEM.getOrThrow(tag), which
    // registers an (unbound) tag in the ITEM registry. Vanilla binds every such bootstrap-created tag to
    // empty before its registry freeze (BuiltInRegistries#bindBootstrappedTagsToEmpty); our items register
    // AFTER that pass, so the alexsmobs:repairs/<name> tags stay unbound and NeoForge's registry freeze
    // ("Unbound tags in registry minecraft:item") aborts server load. Bind the tag to empty here, during
    // RegisterEvent while the registry is still writable — the datapack (data/alexsmobs/tags/item/repairs/
    // <name>.json) rebinds the real contents at reload, so anvil repair still works. Best-effort: if the
    // registry is already frozen for some reason, swallow rather than trade one crash for another.
    public static void bindItemTagEmptyForFreeze(net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tag) {
        bindItemTagEmptyForFreeze0(tag);
    }
    *///?}

    // #95: the non-armour half of the same 1.21.2 change. Item#isValidRepairItem was deleted, so the
    // five items that overrode it (ghostly pickaxe, tendon whip, squid grapple, shield of the deep,
    // skelewag sword) silently took NO anvil material repair on every node >=1.21.2. Declare it the way
    // AMArmorMaterial already does — a data-driven tag, alexsmobs:repairs/<name>, bound empty here so
    // NeoForge's registry freeze accepts it and rebound from the datapack at reload. Below 1.21.2 this
    // is a no-op and the surviving (gated) overrides do the job.
    public static net.minecraft.world.item.Item.Properties repairableWith(net.minecraft.world.item.Item.Properties props, String name) {
        //? if >=1.21.2 {
        /*net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tag = net.minecraft.tags.TagKey.create(
                net.minecraft.core.registries.Registries.ITEM, rl("alexsmobs", "repairs/" + name));
        bindItemTagEmptyForFreeze(tag);
        return props.repairable(tag);
        *///?} else {
        return props;
        //?}
    }

    // ...and the correction 2.0.16 needed. For the three items whose Properties are built by a
    // ToolMaterial (ghostly pickaxe, tendon whip, skelewag sword) the call above is DISCARDED:
    // ToolMaterial#applyToolProperties / #applySwordProperties call Item.Properties#repairable
    // (repairItems) themselves, so iron's tag overwrites ours a moment later. Both bands funnel
    // through that call — 1.21.2-1.21.4 inside the DiggerItem/SwordItem super constructor,
    // 1.21.5+ inside Item.Properties#pickaxe/#sword — so the fix is to hand them a COPY of the
    // material carrying our tag instead of stamping the Properties beforehand. The record's six
    // components are identical 1.21.2 -> 26.2 (javap-checked on 1.21.2/.4/.5/.11 and 26.1.2/26.2),
    // so this needs no gate of its own beyond the one that makes the type exist.
    //? if >=1.21.2 {
    /*public static net.minecraft.world.item.ToolMaterial repairMaterial(net.minecraft.world.item.ToolMaterial base, String name) {
        net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tag = net.minecraft.tags.TagKey.create(
                net.minecraft.core.registries.Registries.ITEM, rl("alexsmobs", "repairs/" + name));
        bindItemTagEmptyForFreeze(tag);
        return new net.minecraft.world.item.ToolMaterial(base.incorrectBlocksForDrops(), base.durability(),
                base.speed(), base.attackDamageBonus(), base.enchantmentValue(), tag);
    }
    *///?}

    // 26.1 replaced MappedRegistry#bindTag(tag, values) with a batched bindTags(Map) — same
    // per-tag semantics (each entry binds one tag), so a single-entry map is the direct successor.
    // Split out of the method above because Stonecutter blocks are siblings and never nest.
    //? if >=26 {
    /*private static void bindItemTagEmptyForFreeze0(net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tag) {
        if (net.minecraft.core.registries.BuiltInRegistries.ITEM instanceof net.minecraft.core.MappedRegistry<net.minecraft.world.item.Item> mr) {
            try {
                mr.bindTags(java.util.Map.of(tag, java.util.List.<net.minecraft.core.Holder<net.minecraft.world.item.Item>>of()));
            } catch (Throwable ignored) {
            }
        }
    }
    *///?} elif >=1.21.2 {
    /*private static void bindItemTagEmptyForFreeze0(net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tag) {
        if (net.minecraft.core.registries.BuiltInRegistries.ITEM instanceof net.minecraft.core.MappedRegistry<net.minecraft.world.item.Item> mr) {
            try {
                mr.bindTag(tag, java.util.List.<net.minecraft.core.Holder<net.minecraft.world.item.Item>>of());
            } catch (Throwable ignored) {
            }
        }
    }
    *///?}

    // An ingredient covering several item tags at once. Before 1.21.2 that was a stream of
    // Ingredient.TagValue; now that Ingredient wraps a single HolderSet the tags are resolved and
    // concatenated into one direct set. Duplicates across two tags are harmless — Ingredient only
    // ever asks whether a stack's item is in the set.
    @SafeVarargs
    public static net.minecraft.world.item.crafting.Ingredient ingredientOfTags(net.minecraft.tags.TagKey<net.minecraft.world.item.Item>... tags) {
        //? if >=1.21.2 {
        /*java.util.List<net.minecraft.core.Holder<net.minecraft.world.item.Item>> holders = new java.util.ArrayList<>();
        for (net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tag : tags) {
            net.minecraft.core.registries.BuiltInRegistries.ITEM.getOrThrow(tag).forEach(holders::add);
        }
        return net.minecraft.world.item.crafting.Ingredient.of(net.minecraft.core.HolderSet.direct(holders));
        *///?} else {
        return net.minecraft.world.item.crafting.Ingredient.fromValues(
                java.util.Arrays.stream(tags).map(net.minecraft.world.item.crafting.Ingredient.TagValue::new));
        //?}
    }

    // The display stacks of an ingredient. Pre-1.21.2 Ingredient cached an ItemStack[]; from
    // 1.21.2 it is a HolderSet<Item> and both getItems() and isEmpty() are gone, so the stacks
    // are built from the holders. Only the guide book uses this, purely to cycle through the
    // acceptable items of a recipe slot.
    public static net.minecraft.world.item.ItemStack[] ingredientStacks(net.minecraft.world.item.crafting.Ingredient ingredient) {
        //? if >=1.21.4 {
        /*// 1.21.4 changed Ingredient#items() from List<Holder<Item>> to Stream<Holder<Item>>.
        java.util.List<net.minecraft.core.Holder<net.minecraft.world.item.Item>> holders = ingredient.items().toList();
        net.minecraft.world.item.ItemStack[] stacks = new net.minecraft.world.item.ItemStack[holders.size()];
        for (int i = 0; i < stacks.length; i++) {
            stacks[i] = new net.minecraft.world.item.ItemStack(holders.get(i));
        }
        return stacks;
        *///?} elif >=1.21.2 {
        /*java.util.List<net.minecraft.core.Holder<net.minecraft.world.item.Item>> holders = ingredient.items();
        net.minecraft.world.item.ItemStack[] stacks = new net.minecraft.world.item.ItemStack[holders.size()];
        for (int i = 0; i < stacks.length; i++) {
            stacks[i] = new net.minecraft.world.item.ItemStack(holders.get(i));
        }
        return stacks;
        *///?} else {
        return ingredient.isEmpty() ? new net.minecraft.world.item.ItemStack[0] : ingredient.getItems();
        //?}
    }

    // Game rules moved off Level onto ServerLevel in 1.21.2 — they are server state and the client
    // only ever sees a stale copy. Every call site in this mod is server-side logic (loot drops,
    // mob griefing, weather), so the client branch is unreachable in practice; `false` is the
    // conservative answer there, since it is the "don't do the world-changing thing" side of every
    // rule asked about here.
    // 1.21.11 additionally renamed every rule AND retyped the constants (GameRules.Key<BooleanValue>
    // became GameRule<Boolean>, read with get() instead of getBoolean()), so a rule can no longer be
    // named at a call site at all without an era gate per site. This enum is the era-neutral name;
    // the switch below is the only place a vanilla constant is spelled.
    //
    // ⚠️ It must stay a *nested enum*, never static fields on AMCompat holding GameRules constants:
    // AMCompat.<clinit> is reachable from vanilla's own bootstrap (LivingEntityMixin defines a static
    // EntityDataAccessor there), so a static field touching a registry dies in Bootstrap#bootStrap.
    public enum Rule { MOB_LOOT, MOB_GRIEFING, BLOCK_DROPS, MOB_SPAWNING, UNIVERSAL_ANGER, WEATHER_CYCLE }

    public static boolean gameRule(net.minecraft.world.level.Level level, Rule rule) {
        //? if >=1.21.11 {
        /*if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) return false;
        return switch (rule) {
            case MOB_LOOT -> serverLevel.getGameRules().get(net.minecraft.world.level.GameRules.MOB_DROPS);
            case MOB_GRIEFING -> serverLevel.getGameRules().get(net.minecraft.world.level.GameRules.MOB_GRIEFING);
            case BLOCK_DROPS -> serverLevel.getGameRules().get(net.minecraft.world.level.GameRules.BLOCK_DROPS);
            case MOB_SPAWNING -> serverLevel.getGameRules().get(net.minecraft.world.level.GameRules.SPAWN_MOBS);
            case UNIVERSAL_ANGER -> serverLevel.getGameRules().get(net.minecraft.world.level.GameRules.UNIVERSAL_ANGER);
            case WEATHER_CYCLE -> serverLevel.getGameRules().get(net.minecraft.world.level.GameRules.ADVANCE_WEATHER);
        };
        *///?} elif >=1.21.2 {
        /*if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) return false;
        return switch (rule) {
            case MOB_LOOT -> serverLevel.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBLOOT);
            case MOB_GRIEFING -> serverLevel.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING);
            case BLOCK_DROPS -> serverLevel.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOBLOCKDROPS);
            case MOB_SPAWNING -> serverLevel.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING);
            case UNIVERSAL_ANGER -> serverLevel.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_UNIVERSAL_ANGER);
            case WEATHER_CYCLE -> serverLevel.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_WEATHER_CYCLE);
        };
        *///?} else {
        return switch (rule) {
            case MOB_LOOT -> level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBLOOT);
            case MOB_GRIEFING -> level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING);
            case BLOCK_DROPS -> level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOBLOCKDROPS);
            case MOB_SPAWNING -> level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING);
            case UNIVERSAL_ANGER -> level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_UNIVERSAL_ANGER);
            case WEATHER_CYCLE -> level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_WEATHER_CYCLE);
        };
        //?}
    }

    // Level.getTimeOfDay(partialTick) was deleted in 1.21.11 along with LevelTimeAccess; the day
    // fraction is now the SUN_ANGLE environment attribute in degrees. Vanilla's own item-property
    // "time" does exactly this division (client/renderer/item/properties/numeric/Time).
    public static float timeOfDay(net.minecraft.world.level.LevelAccessor level, net.minecraft.core.BlockPos pos) {
        //? if >=1.21.11 {
        /*return level.environmentAttributes().getValue(net.minecraft.world.attribute.EnvironmentAttributes.SUN_ANGLE, pos) / 360.0F;
        *///?} else {
        return level.getTimeOfDay(1.0F);
        //?}
    }

    // getCurrentDifficultyAt left Level in 1.21.11 — it survives only on ServerLevelAccessor. Every
    // call site here already sits behind an isClientSide check, so the cast is safe; a client-side
    // caller gets vanilla's PEACEFUL-equivalent default rather than a ClassCastException.
    public static net.minecraft.world.DifficultyInstance difficultyAt(net.minecraft.world.level.LevelAccessor level, net.minecraft.core.BlockPos pos) {
        //? if >=1.21.11 {
        /*if (level instanceof net.minecraft.world.level.ServerLevelAccessor serverLevel) {
            return serverLevel.getCurrentDifficultyAt(pos);
        }
        return new net.minecraft.world.DifficultyInstance(net.minecraft.world.Difficulty.PEACEFUL, 0L, 0L, 0.0F);
        *///?} else {
        return level.getCurrentDifficultyAt(pos);
        //?}
    }

    // Looking a damage type up by name: 1.21.2 renamed RegistryAccess#registryOrThrow to
    // lookupOrThrow and Registry#getHolder(ResourceKey) to get(ResourceKey).
    @Nullable
    public static net.minecraft.core.Holder<net.minecraft.world.damagesource.DamageType> damageTypeHolder(net.minecraft.world.level.Level level, String id) {
        net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> key =
                net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE, rl(id));
        //? if >=1.21.2
        //return level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).get(key).map(h -> (net.minecraft.core.Holder<net.minecraft.world.damagesource.DamageType>) h).orElse(null);
        //? if <1.21.2
        return level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolder(key).map(h -> (net.minecraft.core.Holder<net.minecraft.world.damagesource.DamageType>) h).orElse(null);
    }

    public static boolean doHurtTarget(net.minecraft.world.entity.Mob mob, Entity target) {
        //? if >=1.21.2 {
        /*net.minecraft.server.level.ServerLevel level = serverLevel(mob);
        return level != null && mob.doHurtTarget(level, target);
        *///?} else {
        return mob.doHurtTarget(target);
        //?}
    }

    // Entity#hurt is `public final void` from 1.21.2 on; hurtOrSimulate is the boolean-returning
    // form, and it is what vanilla itself calls where the old return value mattered.
    public static boolean hurt(Entity entity, net.minecraft.world.damagesource.DamageSource source, float amount) {
        //? if >=1.21.2
        //return entity.hurtOrSimulate(source, amount);
        //? if <1.21.2
        return entity.hurt(source, amount);
    }

    public static void kill(Entity entity) {
        //? if >=1.21.2 {
        /*net.minecraft.server.level.ServerLevel level = serverLevel(entity);
        if (level != null) {
            entity.kill(level);
        }
        *///?} else {
        entity.kill();
        //?}
    }

    @Nullable
    public static net.minecraft.world.entity.item.ItemEntity spawnAtLocation(Entity entity, net.minecraft.world.level.ItemLike item) {
        //? if >=1.21.2 {
        /*net.minecraft.server.level.ServerLevel level = serverLevel(entity);
        return level == null ? null : entity.spawnAtLocation(level, item);
        *///?} else {
        return entity.spawnAtLocation(item);
        //?}
    }

    @Nullable
    public static net.minecraft.world.entity.item.ItemEntity spawnAtLocation(Entity entity, ItemStack stack) {
        //? if >=1.21.2 {
        /*net.minecraft.server.level.ServerLevel level = serverLevel(entity);
        return level == null ? null : entity.spawnAtLocation(level, stack);
        *///?} else {
        return entity.spawnAtLocation(stack);
        //?}
    }

    @Nullable
    public static net.minecraft.world.entity.item.ItemEntity spawnAtLocation(Entity entity, ItemStack stack, float yOffset) {
        //? if >=1.21.2 {
        /*net.minecraft.server.level.ServerLevel level = serverLevel(entity);
        return level == null ? null : entity.spawnAtLocation(level, stack, yOffset);
        *///?} else {
        return entity.spawnAtLocation(stack, yOffset);
        //?}
    }

    // EntityType#create(Level) gained an EntitySpawnReason. Everything in this mod that calls it
    // is summoning a mob from code — an egg, a conversion, a mob spawning its own babies — which
    // is exactly what MOB_SUMMONED means, so no call site changes behaviour.
    @Nullable
    public static <T extends Entity> T create(net.minecraft.world.entity.EntityType<T> type, net.minecraft.world.level.Level level) {
        //? if >=1.21.2
        //return type.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        //? if <1.21.2
        return type.create(level);
    }

    /**
     * An entity built only to be *drawn* — the animal dictionary's page and index icons, and the
     * fancy/effect item renderers. It is never added to a level, so nothing ever assigns it a
     * network id, and from MC 26.2 {@code Entity#getId()} THROWS while the id is still its default
     * {@code 0} ("Tried to access entity ID before ID assignment"). Every living render state goes
     * through {@code ItemModelResolver#updateForLiving}, which reads {@code getId()} purely as a
     * per-entity model seed — so the whole GUI died with an {@code IllegalStateException} the
     * moment the book was opened.
     *
     * <p>Stamping {@code -1} is exactly what vanilla's own display-only entity does
     * ({@code BaseSpawner#getOrCreateDisplayEntity}). It is unconditional rather than gated: on
     * every other node the id is only ever a render seed for an entity that is not in any level,
     * so there is nothing for it to collide with.</p>
     */
    @Nullable
    public static <T extends Entity> T createForDisplay(net.minecraft.world.entity.EntityType<T> type, net.minecraft.world.level.Level level) {
        T entity = create(type, level);
        if (entity != null) {
            entity.setId(-1);
        }
        return entity;
    }

    // ── Targeting ──────────────────────────────────────────────────────────────────────────
    // 1.21.2 replaced the Predicate<LivingEntity> that TargetingConditions and the target goals
    // took with TargetingConditions.Selector, whose test also receives the ServerLevel. Nothing
    // in this mod wants the level — every one of these predicates reads the candidate alone — so
    // the wrapper just drops it. Declared twice because the return type is what differs; the
    // parameter is raw so the ~8 predicates typed to a specific mob still fit.
    @SuppressWarnings("unchecked")
    //? if >=1.21.2 {
    /*public static <T extends LivingEntity> net.minecraft.world.entity.ai.targeting.TargetingConditions.Selector selector(java.util.function.Predicate<T> predicate) {
        return predicate == null ? null : (candidate, level) -> ((java.util.function.Predicate<LivingEntity>) predicate).test(candidate);
    }
    *///?} else {
    public static <T extends LivingEntity> java.util.function.Predicate<T> selector(java.util.function.Predicate<T> predicate) {
        return predicate;
    }
    //?}

    /**
     * The nearest player matching some {@link net.minecraft.world.entity.ai.targeting.TargetingConditions},
     * or {@code null} client-side. 1.21.2 moved every conditions-based lookup off {@code Level}
     * onto {@code ServerEntityGetter}, which only {@code ServerLevel} implements — and each of
     * these eight call sites is inside a goal that only ticks on the server anyway.
     */
    @Nullable
    public static net.minecraft.world.entity.player.Player getNearestPlayer(net.minecraft.world.level.Level level, net.minecraft.world.entity.ai.targeting.TargetingConditions conditions, LivingEntity around) {
        //? if >=1.21.2 {
        /*return level instanceof net.minecraft.server.level.ServerLevel serverLevel ? serverLevel.getNearestPlayer(conditions, around) : null;
        *///?} else {
        return level.getNearestPlayer(conditions, around);
        //?}
    }

    /** Likewise for the entity form; empty client-side. */
    public static <T extends LivingEntity> java.util.List<T> getNearbyEntities(net.minecraft.world.level.Level level, Class<T> type, net.minecraft.world.entity.ai.targeting.TargetingConditions conditions, LivingEntity around, net.minecraft.world.phys.AABB box) {
        //? if >=1.21.2 {
        /*return level instanceof net.minecraft.server.level.ServerLevel serverLevel ? serverLevel.getNearbyEntities(type, conditions, around, box) : java.util.List.of();
        *///?} else {
        return level.getNearbyEntities(type, conditions, around, box);
        //?}
    }

    // Spawn eggs. Forge (all) has ForgeSpawnEggItem(Supplier,...); NeoForge < 1.21.3 has the same
    // shape via DeferredSpawnEggItem (the !nf-cls-spawnegg rule renames the FQN in the else branch).
    // NeoForge 1.21.3 deleted the deferred variant, so we resolve the supplier and hand vanilla
    // SpawnEggItem the concrete EntityType (safe: the ENTITY_TYPE registry is populated before ITEM).
    // Level#addParticle gained a second boolean (alwaysShow, i.e. ignore the client particle limit)
    // in 1.21.4, between the force flag and the coordinates. Call sites pass force = true.
    public static void addParticle(net.minecraft.world.level.Level level, net.minecraft.core.particles.ParticleOptions particle, boolean force, double x, double y, double z, double dx, double dy, double dz) {
        //? if >=1.21.4 {
        /*level.addParticle(particle, force, false, x, y, z, dx, dy, dz);
        *///?} else {
        level.addParticle(particle, force, x, y, z, dx, dy, dz);
        //?}
    }

    public static net.minecraft.world.item.SpawnEggItem spawnEgg(java.util.function.Supplier<? extends net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.Mob>> type, int bg, int fg, net.minecraft.world.item.Item.Properties props) {
        // 1.21.9 dropped the EntityType from the constructor: the egg's mob is now a data component,
        // stamped by Item.Properties#spawnEgg (which is also how SpawnEggItem's BY_ID map is filled).
        //? if >=1.21.9 {
        /*return new net.minecraft.world.item.SpawnEggItem(props.spawnEgg(type.get()));
        *///?} elif >=1.21.4 {
        /*// 1.21.4 dropped the two colour ints from SpawnEggItem — egg tint now comes from the
        // item model / spawn-egg data, not the constructor. Both loaders use the vanilla item.
        return new net.minecraft.world.item.SpawnEggItem(type.get(), props);
        *///?} elif neoforge && >=1.21.3 {
        /*return new net.minecraft.world.item.SpawnEggItem(type.get(), bg, fg, props);
        *///?} elif fabric {
        /*// Same vanilla constructor as the NeoForge arm above. ForgeSpawnEggItem only exists to
        // defer the EntityType lookup past registry-freeze ordering; Fabric has no equivalent and
        // needs none — the >=1.21.4 arm above already resolves the supplier eagerly on every node.
        return new net.minecraft.world.item.SpawnEggItem(type.get(), bg, fg, props);
        *///?} else {
        return new net.minecraftforge.common.ForgeSpawnEggItem(type, bg, fg, props);
        //?}
    }

    // ── Event cancellation (Forge EventBus 7, MC 1.21.6) ───────────────────────
    // EventBus 7 deleted setCanceled/isCanceled outright: a listener cancels by *returning true*,
    // and MutableEvent carries no cancellation flag at all. That inverts control — the decision has
    // to travel out through the return value rather than being stamped onto the event.
    //
    // A blanket "rewrite setCanceled(true) to return true" is NOT faithful here: several handlers in
    // this mod keep doing work after cancelling (onStruckByLightning still spawns a giant squid,
    // onPreRenderEntity still renders the rolling entity itself). So instead each such handler keeps
    // its body verbatim and gains a thin boolean-returning bridge that runs it and reports whether
    // cancelEvent() was called.
    //
    // The flag is thread-local (events fire on both the server and the render thread) and the
    // previous value is saved and restored, so a handler that re-enters the bus cannot clobber an
    // outer post's verdict.

    private static final ThreadLocal<boolean[]> CANCEL_FLAG = ThreadLocal.withInitial(() -> new boolean[1]);

    /** Runs an event handler body and returns whether it asked for the event to be cancelled. */
    public static boolean cancelIf(Runnable body) {
        boolean[] cell = CANCEL_FLAG.get();
        boolean prev = cell[0];
        cell[0] = false;
        try {
            body.run();
            return cell[0];
        } finally {
            cell[0] = prev;
        }
    }

    /** The EventBus-7 stand-in for {@code event.setCanceled(true)}; only meaningful inside {@link #cancelIf}. */
    public static void cancelEvent() {
        CANCEL_FLAG.get()[0] = true;
    }

    // ── MC 26.1 ────────────────────────────────────────────────────────────────
    // ⚠️ Everything below is a METHOD, never a static field. AMCompat is reachable from vanilla's
    // own bootstrap (LivingEntity.<clinit> touches the vendored Citadel data accessor), so a static
    // field that reads Items/Blocks/SoundEvents observes a half-initialised registry and throws —
    // see the 1.21.9-forge Bootstrap crash recorded in docs/notes/porting-log.md.

    /**
     * 26.1 replaced the flat per-mob sound constants with per-variant sound sets: a cat's sounds now
     * live in a CatSoundVariant looked up by SoundSet. CLASSIC is the vanilla-default variant, so
     * this is the exact same SoundEvent {@code SoundEvents.CAT_EAT} used to name.
     */
    public static net.minecraft.sounds.SoundEvent catEatSound() {
        //? if >=26 {
        /*return net.minecraft.sounds.SoundEvents.CAT_SOUNDS
                .get(net.minecraft.world.entity.animal.feline.CatSoundVariants.SoundSet.CLASSIC)
                .adultSounds().eatSound().value();
        *///?} else {
        return net.minecraft.sounds.SoundEvents.CAT_EAT;
        //?}
    }

    /** As {@link #catEatSound()}, for the cow's step sound. */
    public static net.minecraft.sounds.SoundEvent cowStepSound() {
        //? if >=26 {
        /*return net.minecraft.sounds.SoundEvents.COW_SOUNDS
                .get(net.minecraft.world.entity.animal.cow.CowSoundVariants.SoundSet.CLASSIC)
                .stepSound().value();
        *///?} else {
        return net.minecraft.sounds.SoundEvents.COW_STEP;
        //?}
    }

    /**
     * 26.1 split {@code Player#displayClientMessage(Component, boolean)} into two methods.
     * Every call site in this mod passes {@code true} (the tamed-mob command toast), but the
     * boolean is kept so the helper stays a drop-in for the old signature.
     */
    public static void displayClientMessage(net.minecraft.world.entity.player.Player player, net.minecraft.network.chat.Component message, boolean overlay) {
        //? if >=26 {
        /*if (overlay) {
            player.sendOverlayMessage(message);
        } else {
            player.sendSystemMessage(message);
        }
        *///?} else {
        player.displayClientMessage(message, overlay);
        //?}
    }

    /**
     * 26.1 dropped {@code ItemParticleOption(ParticleType, ItemStack)} — the particle carries an
     * ItemStackTemplate (an immutable item + component patch) instead of a live stack.
     * {@code fromNonEmptyStack} throws on an empty stack, so an empty one falls back to the bare
     * item form, which is what an empty stack would have rendered as anyway.
     */
    public static net.minecraft.core.particles.ItemParticleOption itemParticle(net.minecraft.core.particles.ParticleType<net.minecraft.core.particles.ItemParticleOption> type, net.minecraft.world.item.ItemStack stack) {
        //? if >=26 {
        /*if (stack.isEmpty()) {
            return new net.minecraft.core.particles.ItemParticleOption(type, stack.getItem());
        }
        return new net.minecraft.core.particles.ItemParticleOption(type, net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(stack));
        *///?} else {
        return new net.minecraft.core.particles.ItemParticleOption(type, stack);
        //?}
    }

    /**
     * 26.1 moved {@code isRaining}/{@code isThundering} off {@code LevelData} onto {@code Level}.
     * Both spawn predicates that ask this take a {@code LevelAccessor}, which has neither method
     * below 26 nor above it — so the two eras reach the same answer by different routes.
     */
    //? if >=26 {
    /*public static ItemStack asItemStack(net.minecraft.world.item.ItemInstance instance) {
        if (instance instanceof ItemStack stack) {
            return stack;
        }
        if (instance instanceof net.minecraft.world.item.ItemStackTemplate template) {
            return template.create();
        }
        return null;
    }
    *///?} else {
    public static ItemStack asItemStack(ItemStack stack) {
        return stack;
    }
    //?}

    public static boolean isRainingOrThundering(net.minecraft.world.level.LevelAccessor worldIn) {
        //? if >=26 {
        /*net.minecraft.world.level.Level level = worldIn instanceof net.minecraft.world.level.Level lvl ? lvl
                : worldIn instanceof net.minecraft.world.level.ServerLevelAccessor sla ? sla.getLevel() : null;
        return level != null && (level.isThundering() || level.isRaining());
        *///?} else {
        return worldIn.getLevelData() != null && (worldIn.getLevelData().isThundering() || worldIn.getLevelData().isRaining());
        //?}
    }

    // MC 26.2 deleted LivingEntity#knockback(double, double, double); the two survivors both take a
    // trailing DamageSource + damage amount. Both IGNORE them (verified in LivingEntity: neither the
    // 5- nor the 6-argument body reads `source` or `damage` — they exist only so the NeoForge
    // LivingKnockBackEvent and the knockback-resistance attribute have the attack in hand), so a
    // generic source and 0 damage reproduce the old behaviour exactly.
    public static void knockback(LivingEntity entity, double power, double xd, double zd) {
        //? if >=26.2 {
        /*entity.knockback(power, xd, zd, entity.damageSources().generic(), 0.0F);
        *///?} else {
        entity.knockback(power, xd, zd);
        //?}
    }

    /* MC 1.21.2 rewrote ServerboundPlayerInputPacket to carry a boolean
     * net.minecraft.world.entity.player.Input record, and ServerGamePacketListenerImpl#handlePlayerInput
     * stopped writing ServerPlayer.xxa/zza entirely — it only calls setLastClientInput + setShiftKeyDown.
     * So on a server those two fields are permanently 0 from 1.21.2 up, and any SERVER-side code that
     * steers something off a rider's movement keys silently does nothing (vanilla moved its own vehicles
     * to LivingEntity#getRiddenInput, which runs client-side on the controlling player).
     *
     * These two rebuild the old float impulses from the input record, using the same sign convention as
     * vanilla's ServerPlayer#getLastClientMoveIntent (forward and LEFT are positive). On the client the
     * LocalPlayer still sets xxa/zza, so the fallback keeps working there and on every node below 1.21.2. */
    public static float riderForward(LivingEntity player) {
        //? if >=1.21.2 {
        /*if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            net.minecraft.world.entity.player.Input input = serverPlayer.getLastClientInput();
            return input.forward() == input.backward() ? 0.0F : (input.forward() ? 1.0F : -1.0F);
        }
        *///?}
        return player.zza;
    }

    /* "Is this entity made of parts?", for the two call sites that hold a bare Entity and so cannot
     * use IMultipartOwner directly (EntityVoidPortal's teleport filter and MessageHurtMultipart's
     * fallback). isMultipartEntity()/getParts() are Forge/NeoForge PATCHES on vanilla Entity, so on
     * Fabric there is nothing to call.
     *
     * The Fabric arm names EnderDragon explicitly on purpose: on Forge the patch reports true for it
     * (its subEntities predate the Forge API), and EntityVoidPortal relies on that to refuse to
     * teleport one. Vanilla still has the dragon's parts, just hard-typed to EnderDragonPart and not
     * reachable through any shared supertype — so it has to be spelled out to stay faithful. */
    public static boolean isMultipart(Entity entity) {
        //? if fabric {
        /*return entity instanceof com.github.alexthe666.alexsmobs.entity.IMultipartOwner owner
                ? owner.getParts() != null
                : entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon;
        *///?} else {
        return entity.getParts() != null;
        //?}
    }

    /* Client-side part entities have no id of their own, and on 26.2 that turned fatal.
     *
     * Through 26.1.2 Entity's constructor took an id from a global counter, so a part built on the
     * client got a unique (if server-meaningless) one. 26.2 moved assignment to
     * Level#getNextEntityId — which returns 0 on the client, because ids now arrive from the server
     * with the spawn packet — and made Entity#getId THROW while the id is still 0
     * ("Tried to access entity ID before ID assignment"). A PartEntity never gets a spawn packet on
     * any loader, so every client-side part is stuck at 0 and the first read of it crashes:
     *   - Forge indexes ClientLevel.partEntities by part.getId() in the tracking callbacks, so the
     *     client dies as soon as a cachalot/giant squid/laviathan comes into view. (NeoForge does
     *     NOT — its ClientLevel keeps a plain List<PartEntity> and never reads the id. Verified by
     *     disassembling both patched jars; don't assume the two loaders match here.)
     *   - vanilla MultiPlayerGameMode#attack reads getId() before anything of ours runs;
     *   - FabricMultiPlayerGameModeMixin reads it to build MessageHurtMultipart — the crash this
     *     was reported as.
     * Entity#equals compares the raw id field, so at 0 every part is also "equal" to every other.
     *
     * Restore the pre-26.2 invariant rather than dodging each read: give every client-side part a
     * unique id at construction. NEGATIVE and counting down, because unlike the old shared counter
     * it then cannot collide with a real entity id — the server does resolve these ids
     * (MessageHurtMultipart's part field, ServerboundAttackPacket) and both paths already treat
     * "no such entity" as the normal answer for a part, which is exactly what a client-local part
     * id has always produced. Server-side parts keep the id ServerLevel handed them.
     *
     * Applied on every version rather than gated to >=26.2: it is the same unique-id invariant
     * everywhere, and it retires the collision risk on the versions that did use the shared
     * counter. */
    private static final java.util.concurrent.atomic.AtomicInteger CLIENT_PART_ID =
            new java.util.concurrent.atomic.AtomicInteger();

    public static void assignClientPartId(Entity part) {
        if (part.level().isClientSide()) {
            part.setId(CLIENT_PART_ID.decrementAndGet());
        }
    }

    public static float riderStrafe(LivingEntity player) {
        //? if >=1.21.2 {
        /*if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            net.minecraft.world.entity.player.Input input = serverPlayer.getLastClientInput();
            return input.left() == input.right() ? 0.0F : (input.left() ? 1.0F : -1.0F);
        }
        *///?}
        return player.xxa;
    }

    /* "Can a mob climb this block?" — Forge/NeoForge's IForgeBlock(State)#isLadder extension, which
     * the vendored Citadel pathfinder asks four times and which vanilla has no equivalent method for.
     *
     * The Fabric arm uses BlockTags.CLIMBABLE, which is vanilla's OWN answer to the same question
     * (LivingEntity#onClimbable tests exactly that tag), plus the one block in this mod that
     * overrides isLadder — the end-pirate anchor's chain — so its climb assist still shows up in
     * pathfinding there. Forge's extension additionally lets any third-party block opt in; nothing
     * on Fabric can, which is a fidelity loss confined to modded ladders from OTHER mods. */
    public static boolean isLadder(net.minecraft.world.level.block.state.BlockState state,
                                   net.minecraft.world.level.LevelReader world,
                                   net.minecraft.core.BlockPos pos,
                                   LivingEntity entity) {
        //? if fabric {
        /*return state.is(net.minecraft.tags.BlockTags.CLIMBABLE)
                || state.getBlock() instanceof com.github.alexthe666.alexsmobs.block.BlockEndPirateAnchor anchor
                   && anchor.isLadder(state, world, pos, entity);
        *///?} else {
        return state.isLadder(world, pos, entity);
        //?}
    }

    /* The isLadder story again, for the sibling extension: "does standing on this block stop me
     * sliding down?" — IForgeBlockState#isScaffolding. Its one call site is the centipede leggings'
     * wall-climb, which suppresses the slide only when the block underfoot is scaffolding-like.
     *
     * Vanilla has no tag for this one (unlike CLIMBABLE), so the Fabric arm reproduces Forge's
     * DEFAULT — scaffolding and nothing else — plus the end-pirate anchor's chain, this mod's only
     * override. Same confined fidelity loss: a third-party block that opts in on Forge cannot on
     * Fabric. */
    public static boolean isScaffolding(net.minecraft.world.level.block.state.BlockState state, LivingEntity entity) {
        //? if fabric {
        /*if (state.getBlock() instanceof com.github.alexthe666.alexsmobs.block.BlockEndPirateAnchor anchor) {
            return anchor.isScaffolding(state, entity.level(), entity.blockPosition(), entity);
        }
        return state.is(net.minecraft.world.level.block.Blocks.SCAFFOLDING);
        *///?} else {
        return state.isScaffolding(entity);
        //?}
    }

    // ── Persistent entity NBT ──────────────────────────────────────────────────
    // Forge patches Entity with a save-persisted CompoundTag (getPersistentData) and gives Player a
    // PERSISTED_NBT_TAG sub-key inside it that additionally survives death. One thing in this mod
    // uses either: the "has this player already been handed the Animal Dictionary" flag.
    //
    // The key's value is Forge's own, verbatim, so the two loaders read and write the same sub-tag.
    //
    // This used to be the `!mc205-persistednbt` replacement rule (Player -> ServerPlayer at 1.20.5).
    // It became a constant because Fabric needs a THIRD destination and that rule could not supply
    // one: its search text is a substring of its own replacement, so a Fabric-only rule declared
    // ahead of it does not win the offset, and any arm spelling the Forge name would be rewritten
    // in turn. Binding the platform constant here keeps Forge/NeoForge reading the real field.
    //? if fabric {
    /*public static final String PERSISTED_NBT_TAG = "PlayerPersisted";
    *///?} elif >=1.20.5 {
    /*public static final String PERSISTED_NBT_TAG = net.minecraft.server.level.ServerPlayer.PERSISTED_NBT_TAG;
    *///?} else {
    public static final String PERSISTED_NBT_TAG = net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG;
    //?}

    /* Fabric has no persistent-data patch, so this borrows the store the vendored Citadel mixin
     * already puts on every LivingEntity — synched, and written into addAdditionalSaveData, which
     * ServerPlayer inherits. That covers logout/login.
     *
     * Death is covered separately: a respawn builds a fresh ServerPlayer and nothing would copy the
     * Citadel tag across, so mixin/fabric/FabricServerPlayerMixin carries the PERSISTED_NBT_TAG
     * sub-tag over in restoreFrom — which is exactly what Forge's patch to that method does. */
    public static CompoundTag getPersistentData(Entity entity) {
        //? if fabric {
        /*if (entity instanceof com.github.alexthe666.alexsmobs.citadel.server.entity.ICitadelDataEntity dataEntity) {
            CompoundTag data = dataEntity.getCitadelEntityData();
            if (data == null) {
                data = new CompoundTag();
                dataEntity.setCitadelEntityData(data);
            }
            return data;
        }
        return new CompoundTag();
        *///?} else {
        return entity.getPersistentData();
        //?}
    }
}

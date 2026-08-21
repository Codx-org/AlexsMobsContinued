package com.github.alexthe666.alexsmobs.effect;

import com.github.alexthe666.alexsmobs.misc.AMCompat;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
//? if >=1.20.5 {
/*import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
*///?} else {
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
//?}
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.registries.DeferredRegister;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;

public class AMEffectRegistry {
    public static final DeferredRegister<MobEffect> EFFECT_DEF_REG = DeferredRegister.create(Registries.MOB_EFFECT, AlexsMobs.MODID);
    public static final DeferredRegister<Potion> POTION_DEF_REG = DeferredRegister.create(Registries.POTION, AlexsMobs.MODID);

    public static final Supplier<MobEffect> KNOCKBACK_RESISTANCE = EFFECT_DEF_REG.register("knockback_resistance", ()-> new EffectKnockbackResistance());
    public static final Supplier<MobEffect> LAVA_VISION = EFFECT_DEF_REG.register("lava_vision", ()-> new EffectLavaVision());
    public static final Supplier<MobEffect> SUNBIRD_BLESSING = EFFECT_DEF_REG.register("sunbird_blessing", ()-> new EffectSunbird(false));
    public static final Supplier<MobEffect> SUNBIRD_CURSE = EFFECT_DEF_REG.register("sunbird_curse", ()-> new EffectSunbird(true));
    public static final Supplier<MobEffect> POISON_RESISTANCE = EFFECT_DEF_REG.register("poison_resistance", ()-> new EffectPoisonResistance());
    public static final Supplier<MobEffect> OILED = EFFECT_DEF_REG.register("oiled", ()-> new EffectOiled());
    public static final Supplier<MobEffect> ORCAS_MIGHT = EFFECT_DEF_REG.register("orcas_might", ()-> new EffectOrcaMight());
    public static final Supplier<MobEffect> BUG_PHEROMONES = EFFECT_DEF_REG.register("bug_pheromones", ()-> new EffectBugPheromones());
    public static final Supplier<MobEffect> SOULSTEAL = EFFECT_DEF_REG.register("soulsteal", ()-> new EffectSoulsteal());
    public static final Supplier<MobEffect> CLINGING = EFFECT_DEF_REG.register("clinging", ()-> new EffectClinging());
    public static final Supplier<MobEffect> ENDER_FLU = EFFECT_DEF_REG.register("ender_flu", ()-> new EffectEnderFlu());
    public static final Supplier<MobEffect> FEAR = EFFECT_DEF_REG.register("fear", ()-> new EffectFear());
    public static final Supplier<MobEffect> TIGERS_BLESSING = EFFECT_DEF_REG.register("tigers_blessing", ()-> new EffectTigersBlessing());
    public static final Supplier<MobEffect> DEBILITATING_STING = EFFECT_DEF_REG.register("debilitating_sting", ()-> new EffectDebilitatingSting());
    public static final Supplier<MobEffect> EXSANGUINATION = EFFECT_DEF_REG.register("exsanguination", ()-> new EffectExsanguination());
    public static final Supplier<MobEffect> EARTHQUAKE = EFFECT_DEF_REG.register("earthquake", ()-> new EffectEarthquake());
    public static final Supplier<MobEffect> FLEET_FOOTED = EFFECT_DEF_REG.register("fleet_footed", ()-> new EffectFleetFooted());
    public static final Supplier<MobEffect> POWER_DOWN = EFFECT_DEF_REG.register("power_down", ()-> new EffectPowerDown());

    public static final Supplier<MobEffect> MOSQUITO_REPELLENT = EFFECT_DEF_REG.register("mosquito_repellent", ()-> new EffectMosquitoRepellent());

    // 1.21.2 gave Potion an explicit name, and that name — not the registry id, as before — is
    // what builds the item.minecraft.potion.effect.* translation key. Every call below passes the
    // registry id it is being registered under, so every existing lang key keeps working.
    //? if >=1.21.2 {
    /*private static Potion potion(String name, MobEffectInstance... effects) {
        return new Potion(name, effects);
    }
    *///?} else {
    private static Potion potion(String name, MobEffectInstance... effects) {
        return new Potion(effects);
    }
    //?}
    public static final Supplier<Potion> KNOCKBACK_RESISTANCE_POTION = POTION_DEF_REG.register("knockback_resistance", ()-> potion("knockback_resistance", new MobEffectInstance(AMCompat.effect(KNOCKBACK_RESISTANCE.get()), 3600)));
    public static final Supplier<Potion> LONG_KNOCKBACK_RESISTANCE_POTION = POTION_DEF_REG.register("long_knockback_resistance", ()-> potion("long_knockback_resistance", new MobEffectInstance(AMCompat.effect(KNOCKBACK_RESISTANCE.get()), 9600)));
    public static final Supplier<Potion> STRONG_KNOCKBACK_RESISTANCE_POTION = POTION_DEF_REG.register("strong_knockback_resistance", ()-> potion("strong_knockback_resistance", new MobEffectInstance(AMCompat.effect(KNOCKBACK_RESISTANCE.get()), 1800, 1)));
    public static final Supplier<Potion> LAVA_VISION_POTION = POTION_DEF_REG.register("lava_vision", ()-> potion("lava_vision", new MobEffectInstance(AMCompat.effect(LAVA_VISION.get()), 3600)));
    public static final Supplier<Potion> LONG_LAVA_VISION_POTION = POTION_DEF_REG.register("long_lava_vision", ()-> potion("long_lava_vision", new MobEffectInstance(AMCompat.effect(LAVA_VISION.get()), 9600)));
    public static final Supplier<Potion> SPEED_III_POTION = POTION_DEF_REG.register("speed_iii", ()-> potion("speed_iii", new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 2200, 2)));
    public static final Supplier<Potion> POISON_RESISTANCE_POTION = POTION_DEF_REG.register("poison_resistance", ()-> potion("poison_resistance", new MobEffectInstance(AMCompat.effect(POISON_RESISTANCE.get()), 3600)));
    public static final Supplier<Potion> LONG_POISON_RESISTANCE_POTION = POTION_DEF_REG.register("long_poison_resistance", ()-> potion("long_poison_resistance", new MobEffectInstance(AMCompat.effect(POISON_RESISTANCE.get()), 9600)));
    public static final Supplier<Potion> BUG_PHEROMONES_POTION = POTION_DEF_REG.register("bug_pheromones", ()-> potion("bug_pheromones", new MobEffectInstance(AMCompat.effect(BUG_PHEROMONES.get()), 3600)));
    public static final Supplier<Potion> LONG_BUG_PHEROMONES_POTION = POTION_DEF_REG.register("long_bug_pheromones", ()-> potion("long_bug_pheromones", new MobEffectInstance(AMCompat.effect(BUG_PHEROMONES.get()), 9600)));
    public static final Supplier<Potion> SOULSTEAL_POTION = POTION_DEF_REG.register("soulsteal", ()-> potion("soulsteal", new MobEffectInstance(AMCompat.effect(SOULSTEAL.get()), 3600)));
    public static final Supplier<Potion> LONG_SOULSTEAL_POTION = POTION_DEF_REG.register("long_soulsteal", ()-> potion("long_soulsteal", new MobEffectInstance(AMCompat.effect(SOULSTEAL.get()), 9600)));
    public static final Supplier<Potion> STRONG_SOULSTEAL_POTION = POTION_DEF_REG.register("strong_soulsteal", ()-> potion("strong_soulsteal", new MobEffectInstance(AMCompat.effect(SOULSTEAL.get()), 1800, 1)));
    public static final Supplier<Potion> CLINGING_POTION = POTION_DEF_REG.register("clinging", ()-> potion("clinging", new MobEffectInstance(AMCompat.effect(CLINGING.get()), 3600)));
    public static final Supplier<Potion> LONG_CLINGING_POTION = POTION_DEF_REG.register("long_clinging", ()-> potion("long_clinging", new MobEffectInstance(AMCompat.effect(CLINGING.get()), 9600)));

    //? if >=1.20.5 {
    /*// 1.20.5 turned "which potion is in this bottle" into the POTION_CONTENTS data component
    // and made every Potions.* constant a Holder.
    public static ItemStack createPotion(Supplier<Potion> potion){
        return createPotion(BuiltInRegistries.POTION.wrapAsHolder(potion.get()));
    }

    public static ItemStack createPotion(Holder<Potion> potion){
        return PotionContents.createItemStack(Items.POTION, potion);
    }

    // The global BrewingRecipeRegistry is gone: brewing is built per-server from a
    // PotionBrewing.Builder handed out by the loader's brewing-registration event
    // (ServerEvents#onRegisterBrewingRecipes).
    // Wrapped, because this is not only called by the loader at the point the loader promises. Any
    // mod may call PotionBrewing.bootstrap()/Builder.build() itself, and one does: Create (the Fly
    // port) builds a PotionBrewing from RecipeManager.prepare, on a worker thread, long before the
    // item registry's data components are bound. Every createPotion below then dies inside
    // ItemStack's constructor with "Components not bound yet", and because that runs under the
    // datapack reload the whole server refuses to start — a hard incompatibility out of a mod that
    // never touched this mod (report #37). Skipping that early pass costs nothing: the real
    // bootstrap runs later with everything bound, and rebuilds the list from scratch.
    public static void registerBrewingRecipes(PotionBrewing.Builder builder){
        try {
            registerBrewingRecipes0(builder);
        } catch (Throwable t) {
            brewingRegistrationFailed(t);
        }
    }

    private static void registerBrewingRecipes0(PotionBrewing.Builder builder){
        addBrewing(builder, new ProperBrewingRecipe(createPotion(Potions.STRENGTH), Ingredient.of(AMItemRegistry.BEAR_FUR.get()), createPotion(KNOCKBACK_RESISTANCE_POTION)));
        addBrewing(builder, new ProperBrewingRecipe(createPotion(KNOCKBACK_RESISTANCE_POTION), Ingredient.of(Items.REDSTONE), createPotion(LONG_KNOCKBACK_RESISTANCE_POTION)));
        addBrewing(builder, new ProperBrewingRecipe(createPotion(KNOCKBACK_RESISTANCE_POTION), Ingredient.of(Items.GLOWSTONE_DUST), createPotion(STRONG_KNOCKBACK_RESISTANCE_POTION)));
        addBrewing(builder, new ProperBrewingRecipe(new ItemStack(AMItemRegistry.LAVA_BOTTLE.get()), Ingredient.of(AMItemRegistry.BONE_SERPENT_TOOTH.get()), createPotion(LAVA_VISION_POTION)));
        addBrewing(builder, new ProperBrewingRecipe(createPotion(LAVA_VISION_POTION), Ingredient.of(Items.REDSTONE), createPotion(LONG_LAVA_VISION_POTION)));
        addBrewing(builder, new ProperBrewingRecipe(createPotion(Potions.POISON), Ingredient.of(AMItemRegistry.RATTLESNAKE_RATTLE.get()), new ItemStack(AMItemRegistry.POISON_BOTTLE.get())));
        addBrewing(builder, new ProperBrewingRecipe(new ItemStack(AMItemRegistry.POISON_BOTTLE.get()), Ingredient.of(AMItemRegistry.CENTIPEDE_LEG.get()), createPotion(POISON_RESISTANCE_POTION)));
        addBrewing(builder, new ProperBrewingRecipe(new ItemStack(AMItemRegistry.KOMODO_SPIT_BOTTLE.get()), Ingredient.of(AMItemRegistry.CENTIPEDE_LEG.get()), createPotion(POISON_RESISTANCE_POTION)));
        addBrewing(builder, new ProperBrewingRecipe(createPotion(POISON_RESISTANCE_POTION), Ingredient.of(AMItemRegistry.KOMODO_SPIT.get()), createPotion(LONG_POISON_RESISTANCE_POTION)));
        addBrewing(builder, new ProperBrewingRecipe(createPotion(Potions.STRONG_SWIFTNESS), Ingredient.of(AMItemRegistry.GAZELLE_HORN.get()), createPotion(SPEED_III_POTION)));
        addBrewing(builder, new ProperBrewingRecipe(createPotion(Potions.AWKWARD), Ingredient.of(AMItemRegistry.COCKROACH_WING.get()), createPotion(BUG_PHEROMONES_POTION)));
        addBrewing(builder, new ProperBrewingRecipe(createPotion(BUG_PHEROMONES_POTION), Ingredient.of(Items.REDSTONE), createPotion(LONG_BUG_PHEROMONES_POTION)));
        addBrewing(builder, new ProperBrewingRecipe(createPotion(Potions.AWKWARD), Ingredient.of(AMItemRegistry.SOUL_HEART.get()), createPotion(SOULSTEAL_POTION)));
        addBrewing(builder, new ProperBrewingRecipe(createPotion(SOULSTEAL_POTION), Ingredient.of(Items.REDSTONE), createPotion(LONG_SOULSTEAL_POTION)));
        addBrewing(builder, new ProperBrewingRecipe(createPotion(SOULSTEAL_POTION), Ingredient.of(Items.GLOWSTONE_DUST), createPotion(STRONG_SOULSTEAL_POTION)));
        addBrewing(builder, new ProperBrewingRecipe(createPotion(Potions.AWKWARD), Ingredient.of(AMItemRegistry.DROPBEAR_CLAW.get()), createPotion(CLINGING_POTION)));
        addBrewing(builder, new ProperBrewingRecipe(createPotion(CLINGING_POTION), Ingredient.of(Items.REDSTONE), createPotion(LONG_CLINGING_POTION)));
    }
    *///?} else {
    public static ItemStack createPotion(Supplier<Potion> potion){
        return  PotionUtils.setPotion(new ItemStack(Items.POTION), potion.get());
    }

    public static ItemStack createPotion(Potion potion){
        return  PotionUtils.setPotion(new ItemStack(Items.POTION), potion);
    }

    public static void init(){
        BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(createPotion(Potions.STRENGTH), Ingredient.of(AMItemRegistry.BEAR_FUR.get()), createPotion(KNOCKBACK_RESISTANCE_POTION)));
        BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(createPotion(KNOCKBACK_RESISTANCE_POTION), Ingredient.of(Items.REDSTONE), createPotion(LONG_KNOCKBACK_RESISTANCE_POTION)));
        BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(createPotion(KNOCKBACK_RESISTANCE_POTION), Ingredient.of(Items.GLOWSTONE_DUST), createPotion(STRONG_KNOCKBACK_RESISTANCE_POTION)));
        BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(new ItemStack(AMItemRegistry.LAVA_BOTTLE.get()), Ingredient.of(AMItemRegistry.BONE_SERPENT_TOOTH.get()), createPotion(LAVA_VISION_POTION)));
        BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(createPotion(LAVA_VISION_POTION), Ingredient.of(Items.REDSTONE), createPotion(LONG_LAVA_VISION_POTION)));
        BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(createPotion(Potions.POISON), Ingredient.of(AMItemRegistry.RATTLESNAKE_RATTLE.get()), new ItemStack(AMItemRegistry.POISON_BOTTLE.get())));
        BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(new ItemStack(AMItemRegistry.POISON_BOTTLE.get()), Ingredient.of(AMItemRegistry.CENTIPEDE_LEG.get()), createPotion(POISON_RESISTANCE_POTION)));
        BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(new ItemStack(AMItemRegistry.KOMODO_SPIT_BOTTLE.get()), Ingredient.of(AMItemRegistry.CENTIPEDE_LEG.get()), createPotion(POISON_RESISTANCE_POTION)));
        BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(createPotion(POISON_RESISTANCE_POTION), Ingredient.of(AMItemRegistry.KOMODO_SPIT.get()), createPotion(LONG_POISON_RESISTANCE_POTION)));
        BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(createPotion(Potions.STRONG_SWIFTNESS), Ingredient.of(AMItemRegistry.GAZELLE_HORN.get()), createPotion(SPEED_III_POTION)));
        BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(createPotion(Potions.AWKWARD), Ingredient.of(AMItemRegistry.COCKROACH_WING.get()), createPotion(BUG_PHEROMONES_POTION)));
        BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(createPotion(BUG_PHEROMONES_POTION), Ingredient.of(Items.REDSTONE), createPotion(LONG_BUG_PHEROMONES_POTION)));
        BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(createPotion(Potions.AWKWARD), Ingredient.of(AMItemRegistry.SOUL_HEART.get()), createPotion(SOULSTEAL_POTION)));
        BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(createPotion(SOULSTEAL_POTION), Ingredient.of(Items.REDSTONE), createPotion(LONG_SOULSTEAL_POTION)));
        BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(createPotion(SOULSTEAL_POTION), Ingredient.of(Items.GLOWSTONE_DUST), createPotion(STRONG_SOULSTEAL_POTION)));
        BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(createPotion(Potions.AWKWARD), Ingredient.of(AMItemRegistry.DROPBEAR_CLAW.get()), createPotion(CLINGING_POTION)));
        BrewingRecipeRegistry.addRecipe(new ProperBrewingRecipe(createPotion(CLINGING_POTION), Ingredient.of(Items.REDSTONE), createPotion(LONG_CLINGING_POTION)));


    }
    //?}

    // Both loaders let a mod push an IBrewingRecipe into the builder, but they named the
    // method differently: Forge kept `add`, NeoForge went with `addRecipe`.
    //? if forge && >=1.20.5 {
    /*private static void addBrewing(PotionBrewing.Builder builder, ProperBrewingRecipe recipe){
        builder.add(recipe);
    }
    *///?}
    //? if neoforge && >=1.20.5 {
    /*private static void addBrewing(PotionBrewing.Builder builder, ProperBrewingRecipe recipe){
        builder.addRecipe(recipe);
    }
    *///?}
    // Fabric API's builder takes only vanilla's two mix shapes, and four of the recipes above are
    // neither, so nothing here can go into the builder at all. The recipes are collected instead,
    // and Fabric mixins consult them from vanilla's brewing methods — see FabricBrewing. The
    // builder argument is unused on this loader; it is the mixin on Builder.build() that calls
    // this method, so the arm still receives the real one.
    //? if fabric && >=1.20.5 {
    /*private static void addBrewing(PotionBrewing.Builder builder, ProperBrewingRecipe recipe){
        com.github.alexthe666.alexsmobs.fabric.common.brewing.FabricBrewing.register(recipe);
    }
    *///?}

    // The recovery half of the guard above. On Fabric the recipes go into a global list rather than
    // into the builder, so a run that died part-way has to drop what it managed to add — the mixin
    // resets the list before every call, but nothing would clear a partial one until the next.
    //? if fabric && >=1.20.5 {
    /*private static void brewingRegistrationFailed(Throwable t){
        com.github.alexthe666.alexsmobs.fabric.common.brewing.FabricBrewing.reset();
        warnBrewingFailure(t);
    }
    *///?}
    //? if !fabric && >=1.20.5 {
    /*private static void brewingRegistrationFailed(Throwable t){
        warnBrewingFailure(t);
    }
    *///?}
    //? if >=1.20.5 {
    /*private static boolean warnedBrewing = false;

    private static void warnBrewingFailure(Throwable t){
        if (!warnedBrewing) {
            warnedBrewing = true;
            AlexsMobs.LOGGER.warn("Alex's Mobs skipped a brewing-recipe registration pass: something built a PotionBrewing before the item registry was ready. Brewing is registered again later and should be unaffected.", t);
        }
    }
    *///?}
}

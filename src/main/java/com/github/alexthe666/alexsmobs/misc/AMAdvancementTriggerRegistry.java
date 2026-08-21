package com.github.alexthe666.alexsmobs.misc;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.resources.ResourceLocation;
import com.github.alexthe666.alexsmobs.misc.AMCompat;

public class AMAdvancementTriggerRegistry {

    public static final AMAdvancementTrigger MOSQUITO_SICK = new AMAdvancementTrigger(AMCompat.rl("alexsmobs:mosquito_sick"));
    public static final AMAdvancementTrigger EMU_DODGE = new AMAdvancementTrigger(AMCompat.rl("alexsmobs:emu_dodge"));
    public static final AMAdvancementTrigger STOMP_LEAFCUTTER_ANTHILL = new AMAdvancementTrigger(AMCompat.rl("alexsmobs:stomp_leafcutter_anthill"));
    public static final AMAdvancementTrigger BALD_EAGLE_CHALLENGE = new AMAdvancementTrigger(AMCompat.rl("alexsmobs:bald_eagle_challenge"));
    public static final AMAdvancementTrigger VOID_WORM_SUMMON = new AMAdvancementTrigger(AMCompat.rl("alexsmobs:void_worm_summon"));
    public static final AMAdvancementTrigger VOID_WORM_SPLIT = new AMAdvancementTrigger(AMCompat.rl("alexsmobs:void_worm_split"));
    public static final AMAdvancementTrigger VOID_WORM_SLAY_HEAD = new AMAdvancementTrigger(AMCompat.rl("alexsmobs:void_worm_kill"));
    public static final AMAdvancementTrigger SEAGULL_STEAL = new AMAdvancementTrigger(AMCompat.rl("alexsmobs:seagull_steal"));
    public static final AMAdvancementTrigger LAVIATHAN_FOUR_PASSENGERS = new AMAdvancementTrigger(AMCompat.rl("alexsmobs:laviathan_four_passengers"));
    public static final AMAdvancementTrigger TRANSMUTE_1000_ITEMS = new AMAdvancementTrigger(AMCompat.rl("alexsmobs:transmute_1000_items"));
    public static final AMAdvancementTrigger UNDERMINE_UNDERMINER = new AMAdvancementTrigger(AMCompat.rl("alexsmobs:undermine_underminer"));

    public static final AMAdvancementTrigger ELEPHANT_SWAG = new AMAdvancementTrigger(AMCompat.rl("alexsmobs:elephant_swag"));
    public static final AMAdvancementTrigger SKUNK_SPRAY = new AMAdvancementTrigger(AMCompat.rl("alexsmobs:skunk_spray"));

    private static final AMAdvancementTrigger[] ALL = new AMAdvancementTrigger[]{
            MOSQUITO_SICK, EMU_DODGE, STOMP_LEAFCUTTER_ANTHILL, BALD_EAGLE_CHALLENGE, VOID_WORM_SUMMON,
            VOID_WORM_SPLIT, VOID_WORM_SLAY_HEAD, SEAGULL_STEAL, LAVIATHAN_FOUR_PASSENGERS,
            TRANSMUTE_1000_ITEMS, UNDERMINE_UNDERMINER, ELEPHANT_SWAG, SKUNK_SPRAY
    };

    // 1.20.2 turned criterion triggers into a real registry, so they have to be registered on the
    // mod bus (the registry is frozen by the time common-setup's init() runs) instead of by
    // calling CriteriaTriggers.register during setup.
    //? if >=1.20.2 {
    /*public static final net.minecraftforge.registries.DeferredRegister<net.minecraft.advancements.CriterionTrigger<?>> DEF_REG =
            net.minecraftforge.registries.DeferredRegister.create(net.minecraft.core.registries.Registries.TRIGGER_TYPE, "alexsmobs");

    static {
        for (AMAdvancementTrigger trigger : ALL) {
            DEF_REG.register(trigger.resourceLocation.getPath(), () -> trigger);
        }
    }

    public static void init(){
    }
    *///?}
    //? if <1.20.2 {
    public static void init(){
        for (AMAdvancementTrigger trigger : ALL) {
            CriteriaTriggers.register(trigger);
        }
    }
    //?}

}

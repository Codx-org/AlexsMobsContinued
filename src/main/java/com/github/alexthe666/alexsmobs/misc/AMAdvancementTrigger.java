package com.github.alexthe666.alexsmobs.misc;

import com.google.gson.JsonObject;
// MC 26.2 broke `advancements.critereon` apart: the triggers went to `advancements.triggers` and
// the predicates to `advancements.predicates`, so the wildcard below can no longer name every type
// this file uses. A replacement rule cannot follow a wildcard (it has no per-class text to rewrite),
// so the two predicate types are ALSO imported explicitly — redundant on every node below 26.2,
// load-bearing on it. Same trick as ServerEvents' `animal.*` fix in Milestone 12.
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class AMAdvancementTrigger extends SimpleCriterionTrigger<AMAdvancementTrigger.Instance> {
    public final ResourceLocation resourceLocation;

    public AMAdvancementTrigger(ResourceLocation resourceLocation) {
        this.resourceLocation = resourceLocation;
    }

    public void trigger(ServerPlayer p_192180_1_) {
        this.trigger(p_192180_1_, (p_226308_1_) -> {
            return true;
        });
    }

    // 1.20.2 rewrote the criteria system: triggers live in a registry (so they no longer carry
    // their own id) and their instances are codec-driven records instead of hand-written
    // JSON (de)serializers. Alex's Mobs' triggers carry no data beyond the player predicate.
    // 1.20.5 made every codec field strict by default, so the ExtraCodecs helper went away.
    //? if >=1.20.5 {
    /*@Override
    public com.mojang.serialization.Codec<Instance> codec() {
        return Instance.CODEC;
    }

    public record Instance(java.util.Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance {
        public static final com.mojang.serialization.Codec<Instance> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(
                instance -> instance.group(
                        EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player)
                ).apply(instance, Instance::new));
    }
    *///?}
    //? if >=1.20.2 && <1.20.5 {
    /*@Override
    public com.mojang.serialization.Codec<Instance> codec() {
        return Instance.CODEC;
    }

    public record Instance(java.util.Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance {
        public static final com.mojang.serialization.Codec<Instance> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(
                instance -> instance.group(
                        net.minecraft.util.ExtraCodecs.strictOptionalField(EntityPredicate.ADVANCEMENT_CODEC, "player").forGetter(Instance::player)
                ).apply(instance, Instance::new));
    }
    *///?}
    //? if <1.20.2 {
    public AMAdvancementTrigger.Instance createInstance(JsonObject p_230241_1_, ContextAwarePredicate p_230241_2_, DeserializationContext p_230241_3_) {
        return new AMAdvancementTrigger.Instance(p_230241_2_, resourceLocation);
    }

    @Override
    public ResourceLocation getId() {
        return resourceLocation;
    }

    public static class Instance extends AbstractCriterionTriggerInstance {

        public Instance(ContextAwarePredicate p_i231507_1_, ResourceLocation res) {
            super(res, p_i231507_1_);
        }

        public static ConstructBeaconTrigger.TriggerInstance forLevel(MinMaxBounds.Ints p_203912_0_) {
            return new ConstructBeaconTrigger.TriggerInstance(ContextAwarePredicate.ANY, p_203912_0_);
        }

        public JsonObject serializeToJson(SerializationContext p_230240_1_) {
            JsonObject lvt_2_1_ = super.serializeToJson(p_230240_1_);
            return lvt_2_1_;
        }
    }
    //?}
}

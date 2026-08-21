package com.github.alexthe666.alexsmobs.misc;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.Level;

import java.util.List;
import java.util.Map;

// Fabric has no AddReloadListenerEvent, so this manager is handed to Fabric API's
// ResourceManagerHelper instead — which only accepts an IdentifiableResourceReloadListener.
// See docs/notes/bug-reports.md #84 and AlexsMobsFabric#onInitialize.
//? if fabric && >=1.21.2 {
/*public class CapsidRecipeManager extends SimpleJsonResourceReloadListener<CapsidRecipe> implements net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener {
*///?} elif fabric {
/*public class CapsidRecipeManager extends SimpleJsonResourceReloadListener implements net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener {
*///?} elif >=1.21.2 {
/*public class CapsidRecipeManager extends SimpleJsonResourceReloadListener<CapsidRecipe> {
*///?} else {
public class CapsidRecipeManager extends SimpleJsonResourceReloadListener {
//?}
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().registerTypeAdapter(CapsidRecipe.class, new CapsidRecipe.Deserializer()).create();
    private static final RandomSource RANDOM = RandomSource.create();

    // 1.21.2 made SimpleJsonResourceReloadListener generic over a Codec<T> instead of a Gson reader;
    // apply() now receives the already-decoded map. Same three fields the Gson deserializer read.
    //
    // The two arms differ in ONE field — the result — but a Stonecutter block cannot be nested
    // inside another, and everything from 1.21.2 up already lives in one, so the codec is spelled
    // twice rather than gated in place. Keep them in sync.
    //? if >=26 {
    /*// MC 26 made item components datapack-driven and binds them in
    // ReloadableServerResources#updateComponentsAndStaticRegistryTags — which runs AFTER the whole
    // reload instance, i.e. after every listener's prepare() AND apply(). So NO reload listener can
    // decode ItemStack.CODEC: it goes through Item.CODEC_WITH_BOUND_COMPONENTS, which hard-errors
    // with "Item <id> does not have components yet" and drops the recipe (logged, not thrown).
    // Vanilla's own recipes moved to ItemStackTemplate for exactly this reason — it decodes to a
    // description (holder + count + patch) and materialises the stack on first use, by which time
    // components are bound. CapsidRecipe#getResult does that lazily on this arm.
    private static final com.mojang.serialization.Codec<CapsidRecipe> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(inst -> inst.group(
            net.minecraft.world.item.crafting.Ingredient.CODEC.listOf().fieldOf("ingredients").forGetter(CapsidRecipe::getIngredients),
            net.minecraft.world.item.ItemStackTemplate.CODEC.fieldOf("result").forGetter(CapsidRecipe::getResultTemplate),
            com.mojang.serialization.Codec.INT.fieldOf("time").forGetter(CapsidRecipe::getTime)
    ).apply(inst, (ings, result, time) -> {
        // Do NOT filter on ing.items() here — see the note on the other arm.
        net.minecraft.core.NonNullList<net.minecraft.world.item.crafting.Ingredient> list = net.minecraft.core.NonNullList.create();
        list.addAll(ings);
        return new CapsidRecipe(list, result, time);
    }));
    *///?} elif >=1.21.2 {
    /*private static final com.mojang.serialization.Codec<CapsidRecipe> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(inst -> inst.group(
            net.minecraft.world.item.crafting.Ingredient.CODEC.listOf().fieldOf("ingredients").forGetter(CapsidRecipe::getIngredients),
            net.minecraft.world.item.ItemStack.CODEC.fieldOf("result").forGetter(CapsidRecipe::getResult),
            com.mojang.serialization.Codec.INT.fieldOf("time").forGetter(CapsidRecipe::getTime)
    ).apply(inst, (ings, result, time) -> {
        // Do NOT filter on ing.items() here: this lambda runs in SimpleJsonResourceReloadListener#prepare()
        // on a worker thread, BEFORE tags are bound. Forcing a tag-backed Ingredient to resolve its item
        // set there throws "Trying to access unbound tag". An empty ingredient matches nothing at runtime
        // (test() is false), so keeping it is harmless; tag resolution is deferred to matches().
        net.minecraft.core.NonNullList<net.minecraft.world.item.crafting.Ingredient> list = net.minecraft.core.NonNullList.create();
        list.addAll(ings);
        return new CapsidRecipe(list, result, time);
    }));
    *///?}

    private final List<CapsidRecipe> capsidRecipes = Lists.newArrayList();

    //? if >=1.21.4 {
    /*// 1.21.4 turned the third arg from a String into a ResourceKey<Registry<T>> (the DynamicOps
    // ctor is private now). That ctor builds a RegistryOps from the provider internally and derives
    // the data directory from the key's location — a minecraft-namespaced "capsid_recipes" key keeps
    // the directory as data/<ns>/capsid_recipes/*.json, exactly as the String form did before.
    public CapsidRecipeManager(net.minecraft.core.HolderLookup.Provider registries) {
        super(registries, CODEC, net.minecraft.resources.ResourceKey.createRegistryKey(net.minecraft.resources.ResourceLocation.withDefaultNamespace("capsid_recipes")));
    }
    *///?} elif >=1.21.2 {
    /*// The codec form decodes item/tag references, which needs registry access; a bare Codec+name
    // constructor decodes with plain JsonOps and fails ("Can't decode element … without registry").
    public CapsidRecipeManager(net.minecraft.core.HolderLookup.Provider registries) {
        super(registries, CODEC, "capsid_recipes");
    }
    *///?} else {
    public CapsidRecipeManager() {
        super(GSON, "capsid_recipes");
    }
    //?}

    //? if >=1.21.2 {
    /*protected void apply(Map<ResourceLocation, CapsidRecipe> jsonMap, ResourceManager resourceManager, ProfilerFiller profile) {
        this.capsidRecipes.clear();
        AlexsMobs.LOGGER.log(Level.ALL, "Loading in capsid_recipes jsons...");
        jsonMap.forEach((resourceLocation, capsidRecipe) -> capsidRecipes.add(capsidRecipe));
    }
    *///?} else {
    protected void apply(Map<ResourceLocation, JsonElement> jsonMap, ResourceManager resourceManager, ProfilerFiller profile) {
        this.capsidRecipes.clear();
        ImmutableMap.Builder<ResourceLocation, CapsidRecipe> builder = ImmutableMap.builder();
        AlexsMobs.LOGGER.log(Level.ALL, "Loading in capsid_recipes jsons...");
        jsonMap.forEach((resourceLocation, jsonElement) -> {
            try {
                CapsidRecipe capsidRecipe = GSON.fromJson(jsonElement, CapsidRecipe.class);
                builder.put(resourceLocation, capsidRecipe);
            } catch (Exception exception) {
                AlexsMobs.LOGGER.error("Couldn't parse capsid recipe {}", resourceLocation, exception);
            }
        });
        ImmutableMap<ResourceLocation, CapsidRecipe> immutablemap = builder.build();
        immutablemap.forEach((resourceLocation, capsidRecipe) -> {
            capsidRecipes.add(capsidRecipe);
        });
    }
    //?}

    public CapsidRecipe getRecipeFor(ItemStack stack){
        for(CapsidRecipe recipe : capsidRecipes){
            if(recipe.matches(stack)){
                return recipe;
            }
        }

        return null;
    }

    public List<CapsidRecipe> getCapsidRecipes() {
        return capsidRecipes;
    }

    @Override
    public String getName() {
        return "CapsidRecipeManager";
    }

    //? if fabric {
    /*// Fabric API orders reload listeners by this id (and by getFabricDependencies(), which we do
    // not need — nothing else in this mod reloads, and vanilla's tag/registry binding is already
    // ordered ahead of every mod listener).
    @Override
    public ResourceLocation getFabricId() {
        return AMCompat.rl("alexsmobs", "capsid_recipes");
    }
    *///?}
}

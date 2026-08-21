package com.github.alexthe666.alexsmobs.misc;

import com.github.alexthe666.alexsmobs.citadel.client.model.container.JsonUtils;
import com.google.gson.*;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;

import java.lang.reflect.Type;

public class CapsidRecipe {
    private final NonNullList<Ingredient> ingredients;
    private ItemStack result = ItemStack.EMPTY;
    private int time = 0;

    public CapsidRecipe(NonNullList<Ingredient> ingredients, ItemStack result, int time) {
        this.result = result;
        this.ingredients = ingredients;
        this.time = time;
    }

    // MC 26 binds item components in ReloadableServerResources#updateComponentsAndStaticRegistryTags,
    // which runs AFTER the whole reload instance — i.e. after every listener's prepare() AND apply().
    // ItemStack.CODEC goes through Item.CODEC_WITH_BOUND_COMPONENTS and therefore CANNOT be decoded
    // anywhere inside a reload ("Item <id> does not have components yet", logged not thrown, recipe
    // silently dropped). Vanilla's own recipes moved to ItemStackTemplate for exactly this reason: it
    // decodes to holder + count + patch with plain Item.CODEC and materialises the stack on demand.
    // The template is kept and the ItemStack built lazily on the first getResult(), by which time the
    // reload has finished and components are bound.
    //? if >=26 {
    /*private net.minecraft.world.item.ItemStackTemplate resultTemplate = null;

    public CapsidRecipe(NonNullList<Ingredient> ingredients, net.minecraft.world.item.ItemStackTemplate result, int time) {
        this.resultTemplate = result;
        this.ingredients = ingredients;
        this.time = time;
    }

    public net.minecraft.world.item.ItemStackTemplate getResultTemplate() {
        return resultTemplate;
    }
    *///?}

    private static NonNullList<Ingredient> readIngredients(JsonArray ingredientArray) {
        NonNullList<Ingredient> nonnulllist = NonNullList.create();

        for (int i = 0; i < ingredientArray.size(); ++i) {
            //? if >=1.20.2 {
            /*// 1.20.2 replaced the hand-written recipe JSON readers with codecs.
            Ingredient ingredient = Ingredient.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, ingredientArray.get(i))
                    .result().orElseThrow(() -> new JsonParseException("Malformed capsid recipe ingredient"));
            *///?}
            //? if <1.20.2 {
            Ingredient ingredient = Ingredient.fromJson(ingredientArray.get(i));
            //?}
            //? if >=1.21.4 {
            /*// 1.21.4 changed Ingredient#items to return a Stream<Holder<Item>>.
            if (ingredient.items().findAny().isPresent()) {
            *///?} elif >=1.21.2 {
            /*// Ingredient#isEmpty was removed in 1.21.2; an ingredient with no items matches nothing.
            if (!ingredient.items().isEmpty()) {
            *///?} else {
            if (!ingredient.isEmpty()) {
            //?}
                nonnulllist.add(ingredient);
            }
        }
        return nonnulllist;
    }

    public ItemStack getResult() {
        //? if >=26 {
        /*// Materialise the decoded template on first use — see the note on the constructor.
        if (resultTemplate != null && result.isEmpty()) {
            result = resultTemplate.create();
        }
        *///?}
        return result;
    }

    public NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    public int getTime() {
        return time;
    }

    public boolean matches(ItemStack... stacks) {
        IntList taken = new IntArrayList();
        ItemStack[] copy = new ItemStack[stacks.length];
        for (int j = 0; j < copy.length; j++) {
            copy[j] = stacks[j].copy();
            for (int i = 0; i < ingredients.size(); i++) {
                if (ingredients.get(i).test(copy[j])) {
                    taken.add(j);
                    copy[j].shrink(1);
                }
            }
        }
        return taken.size() >= ingredients.size();
    }

    public static class Deserializer implements JsonDeserializer<CapsidRecipe> {

        @Override
        public CapsidRecipe deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonobject = json.getAsJsonObject();
            int time = JsonUtils.getInt(jsonobject, "time");
            ItemStack result = ItemStack.EMPTY;
            if (jsonobject.has("result")) {
                //? if >=1.20.5 {
                /*result = ItemStack.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, JsonUtils.getJsonObject(jsonobject, "result"))
                        .result().orElseThrow(() -> new JsonParseException("Malformed capsid recipe result"));
                *///?}
                //? if >=1.20.2 && <1.20.5 {
                /*result = ItemStack.ITEM_WITH_COUNT_CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, JsonUtils.getJsonObject(jsonobject, "result"))
                        .result().orElseThrow(() -> new JsonParseException("Malformed capsid recipe result"));
                *///?}
                //? if <1.20.2 {
                result = ShapedRecipe.itemStackFromJson(JsonUtils.getJsonObject(jsonobject, "result"));
                //?}
            }
            NonNullList<Ingredient> nonnulllist = readIngredients(JsonUtils.getJsonArray(jsonobject, "ingredients"));
            return new CapsidRecipe(nonnulllist, result, time);
        }

    }
}

package com.github.alexthe666.alexsmobs.citadel.client.gui;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.misc.AMCompat;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A crafting recipe, read straight off the classpath from {@code data/<ns>/recipe/<path>.json}
 * (or the pre-1.21 {@code recipes/} folder).
 *
 * <p>1.21.2 removed every client-reachable way to look a recipe up by id: {@code Recipe.getIngredients()}
 * and {@code getResultItem()} are gone, {@code RecipeManager.byKey} is server-only, and the client's
 * {@code ClientRecipeBook} is keyed by network display ids and only holds recipes the player has
 * unlocked. The book only ever shows this mod's own recipes, so it parses the shipped JSON instead.
 * Only the two vanilla crafting types are handled — the book never references a smelting, campfire or
 * custom-type recipe. Because it is pure JSON + registry lookups it works unchanged on every node.
 */
public class BookRecipe {

    private static final Map<String, BookRecipe> CACHE = new HashMap<>();

    /** One entry per grid slot, in row-major order. Each entry holds the options that slot cycles through. */
    private final List<ItemStack[]> ingredients;
    private final ItemStack result;
    private final boolean shapeless;

    private BookRecipe(List<ItemStack[]> ingredients, ItemStack result, boolean shapeless) {
        this.ingredients = ingredients;
        this.result = result;
        this.shapeless = shapeless;
    }

    public List<ItemStack[]> getIngredients() {
        return ingredients;
    }

    public ItemStack getResult() {
        return result;
    }

    public boolean isShapeless() {
        return shapeless;
    }

    /** @return the parsed recipe, or null when it is missing or of an unhandled type. */
    public static BookRecipe get(String id) {
        if (CACHE.containsKey(id)) {
            return CACHE.get(id);
        }
        BookRecipe recipe = null;
        try {
            recipe = load(id);
        } catch (Exception e) {
            AlexsMobs.LOGGER.warn("Could not read book recipe {}", id, e);
        }
        CACHE.put(id, recipe);
        return recipe;
    }

    private static BookRecipe load(String id) throws Exception {
        ResourceLocation res = AMCompat.rl(id);
        // 1.21 renamed the data folder recipes/ -> recipe/; try the modern name first and fall back.
        InputStream stream = BookRecipe.class.getResourceAsStream("/data/" + res.getNamespace() + "/recipe/" + res.getPath() + ".json");
        if (stream == null) {
            stream = BookRecipe.class.getResourceAsStream("/data/" + res.getNamespace() + "/recipes/" + res.getPath() + ".json");
        }
        if (stream == null) {
            return null;
        }
        try (InputStream in = stream) {
            JsonObject json = JsonParser.parseReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            String type = json.has("type") ? json.get("type").getAsString() : "";
            List<ItemStack[]> ingredients = new ArrayList<>();
            boolean shapeless;
            if ("minecraft:crafting_shaped".equals(type)) {
                shapeless = false;
                JsonArray pattern = json.getAsJsonArray("pattern");
                JsonObject key = json.getAsJsonObject("key");
                for (JsonElement rowElement : pattern) {
                    String row = rowElement.getAsString();
                    for (int i = 0; i < row.length(); i++) {
                        char c = row.charAt(i);
                        ingredients.add(c == ' ' ? new ItemStack[0] : resolve(key.get(String.valueOf(c))));
                    }
                }
            } else if ("minecraft:crafting_shapeless".equals(type)) {
                shapeless = true;
                for (JsonElement element : json.getAsJsonArray("ingredients")) {
                    ingredients.add(resolve(element));
                }
            } else {
                return null;
            }
            return new BookRecipe(ingredients, readResult(json.get("result")), shapeless);
        }
    }

    private static ItemStack readResult(JsonElement element) {
        if (element == null) {
            return ItemStack.EMPTY;
        }
        if (element.isJsonPrimitive()) {
            return stackOf(element.getAsString(), 1);
        }
        JsonObject object = element.getAsJsonObject();
        String itemId = object.has("id") ? object.get("id").getAsString() : object.get("item").getAsString();
        int count = object.has("count") ? object.get("count").getAsInt() : 1;
        return stackOf(itemId, count);
    }

    /**
     * Resolves one ingredient entry. An entry may be a plain item id, a {@code #tag} reference, an object
     * wrapping either, or a list of any of those (which the book renders by cycling through the options).
     */
    private static ItemStack[] resolve(JsonElement element) {
        List<ItemStack> stacks = new ArrayList<>();
        collect(element, stacks);
        return stacks.toArray(new ItemStack[0]);
    }

    private static void collect(JsonElement element, List<ItemStack> out) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                collect(child, out);
            }
        } else if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("item")) {
                collect(object.get("item"), out);
            } else if (object.has("tag")) {
                collect(object.get("tag"), out);
            } else if (object.has("id")) {
                collect(object.get("id"), out);
            }
        } else {
            String value = element.getAsString();
            if (value.startsWith("#")) {
                TagKey<Item> tag = TagKey.create(Registries.ITEM, AMCompat.rl(value.substring(1)));
                for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
                    out.add(new ItemStack(holder.value()));
                }
            } else {
                ItemStack stack = stackOf(value, 1);
                if (!stack.isEmpty()) {
                    out.add(stack);
                }
            }
        }
    }

    private static ItemStack stackOf(String id, int count) {
        // BuiltInRegistries.ITEM.get(rl) is rewritten to getValue(rl) on >=1.21.2; both return the
        // defaulted AIR item for an unknown id, so an empty ItemStack falls out naturally.
        Item item = BuiltInRegistries.ITEM.get(AMCompat.rl(id));
        return new ItemStack(item, count);
    }
}

package com.github.alexthe666.alexsmobs.citadel.server.block;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;

public class LecternBooks {

    public static Map<ResourceLocation, BookData> BOOKS = new HashMap<>();

    public static boolean isLecternBook(ItemStack stack) {
        return BOOKS.containsKey(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    public static class BookData {
        int bindingColor;
        int pageColor;

        public BookData(int bindingColor, int pageColor) {
            this.bindingColor = bindingColor;
            this.pageColor = pageColor;
        }

        public int getBindingColor() {
            return bindingColor;
        }

        public int getPageColor() {
            return pageColor;
        }
    }
}

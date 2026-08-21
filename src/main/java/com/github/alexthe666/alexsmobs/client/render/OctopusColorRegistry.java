package com.github.alexthe666.alexsmobs.client.render;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.awt.*;
import java.awt.image.BufferedImage;

@OnlyIn(Dist.CLIENT)
public class OctopusColorRegistry {

    public static final BlockState FALLBACK_BLOCK = Blocks.SAND.defaultBlockState();
    public static Object2IntMap<String> TEXTURES_TO_COLOR = new Object2IntOpenHashMap<>();;

    public static int getBlockColor(BlockState stack) {
        String blockName = stack.toString();
        if (TEXTURES_TO_COLOR.containsKey(blockName)) {
            return TEXTURES_TO_COLOR.getInt(blockName);
        } else {
            int colorizer = -1;
            try{
                colorizer = AMRenderCompat.blockTintColor(stack, 0);
            }catch (Exception e){
                AlexsMobs.LOGGER.warn("Another mod did not use block colorizers correctly.");
            }
            int color = 0XFFFFFF;
            if(colorizer == -1){
                BufferedImage texture = null;
                try {
                    Color texColour = getAverageColour(getTextureAtlas(stack));
                    color = texColour.getRGB();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }else{
                color = colorizer;
            }
            TEXTURES_TO_COLOR.put(blockName, color);
            return color;
        }
    }

    private static Color getAverageColour(TextureAtlasSprite image) {
        float red = 0;
        float green = 0;
        float blue = 0;
        float count = 0;
        int uMax = image.contents().width();
        int vMax = image.contents().height();
        for (float i = 0; i < uMax; i++)
            for (float j = 0; j < vMax; j++) {
                int alpha = pixelABGR(image, (int) i, (int) j) >> 24 & 0xFF;
                if (alpha == 0) {
                    continue;
                }
                red += pixelABGR(image, (int) i, (int) j) >> 0 & 0xFF;
                green += pixelABGR(image, (int) i, (int) j) >> 8 & 0xFF;
                blue += pixelABGR(image, (int) i, (int) j) >> 16 & 0xFF;
                count++;
            }
        //Average color
        return new Color((int) (red / count), (int) (green / count), (int) (blue / count));
    }

    /**
     * Forge's {@code TextureAtlasSprite#getPixelRGBA(frame, x, y)} hands back the NATIVE ABGR
     * value — red in the low byte — which is the layout the caller above unpacks. Fabric has no
     * such extension, so there the sprite's backing {@code NativeImage} is read directly. From
     * 1.21.2 that accessor is {@code getPixel} and returns ARGB, so red and blue are swapped back to
     * keep the bit layout identical on both loaders; below it, {@code getPixelRGBA} already returns
     * the native ABGR word and no swap is wanted.
     * <p>
     * This helper deliberately lives here rather than on {@code AMRenderCompat}: that whole
     * package is excluded from the compile below 1.21.2, and this file is not.
     */
    private static int pixelABGR(TextureAtlasSprite image, int x, int y) {
        //? if !fabric {
        return image.getPixelRGBA(0, x, y);
        //?} elif >=1.21.2 {
        /*int argb = image.contents().originalImage.getPixel(x, y);
        return (argb & 0xFF00FF00) | ((argb >> 16) & 0xFF) | ((argb & 0xFF) << 16);
        *///?} else {
        /*// 1.21.2 renamed NativeImage#getPixelRGBA to #getPixel AND changed what it returns: the old
        // one hands back the native ABGR word, which is already the layout this returns, so below the
        // boundary there is nothing to swap. Same value Forge's sprite extension produces.
        return image.contents().originalImage.getPixelRGBA(x, y);
        *///?}
    }

    private static TextureAtlasSprite getTextureAtlas(BlockState state) {
        return AMRenderCompat.blockParticleSprite(state);
    }
}

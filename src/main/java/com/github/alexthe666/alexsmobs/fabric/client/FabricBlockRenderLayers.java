package com.github.alexthe666.alexsmobs.fabric.client;

import com.github.alexthe666.alexsmobs.block.AMBlockRegistry;
import net.minecraft.world.level.block.Block;

/**
 * The Fabric stand-in for the {@code "render_type"} field this mod's block models carry.
 *
 * <p>{@code render_type} is <b>not vanilla</b> — it is a Forge/NeoForge extension to the block-model
 * loader, and both vanilla and Fabric parse the model, find a key they do not know, and ignore it.
 * Without it a block falls back to the {@code SOLID} chunk layer, which draws every texel opaque:
 * rainbow glass became a solid pane, the bison carpet and the triops eggs grew an opaque black border
 * where their cutout should have been. Reported against 2.0.5 on Fabric 1.21.11 as "some block items
 * have weird texture(?)" (report #35), and true on every Fabric node since Milestone 15 — the
 * Forge/NeoForge nodes were never affected.
 *
 * <p>Three eras, measured against the {@code fabric-api} jar each node actually pins, not assumed:
 * <ul>
 *   <li><b>{@code <1.21.6}</b> — {@code fabric-blockrenderlayer-v1}, the instance API
 *       {@code BlockRenderLayerMap.INSTANCE.putBlocks(RenderType, Block...)}.</li>
 *   <li><b>{@code 1.21.6 .. 1.21.11}</b> — the module was folded into {@code fabric-rendering-v1} and
 *       the class moved to {@code api.client.rendering.v1}, became {@code final} with <i>static</i>
 *       methods, and takes vanilla's new {@code ChunkSectionLayer} enum instead of a
 *       {@code RenderType}.</li>
 *   <li><b>{@code >=26.1}</b> — <b>nothing to do</b>. The class is absent from Fabric API there
 *       because 26.x deleted the whole per-block mapping: {@code ItemBlockRenderTypes} is gone and
 *       {@code FaceBakery} now derives each quad's layer from the sprite's own pixels
 *       ({@code SpriteContents.computeTransparency}), so a cutout or translucent texture is detected
 *       rather than declared. These blocks come out right on 26.x with no registration at all.</li>
 * </ul>
 *
 * <p>Called from {@code ClientProxy#clientInit}'s Fabric arm alongside {@link FabricItemRenderers}.
 */
public final class FabricBlockRenderLayers {

    private FabricBlockRenderLayers() {
    }

    /**
     * The thirteen blocks whose models declare a non-solid {@code render_type}, resolved through the
     * blockstate files and every model {@code parent} (the hummingbird feeder inherits it, and only
     * inherits it, from {@code block/bird_feeder}).
     *
     * <p>The banana slug slime block is the one block that wants both — its body is translucent and
     * its bubble overlay is cutout — but the mapping is per <i>block</i>, not per model, so it takes
     * translucent, exactly as vanilla's own slime block does.
     */
    private static Block[] cutout() {
        return new Block[]{
                AMBlockRegistry.BANANA_PEEL.get(),
                AMBlockRegistry.HUMMINGBIRD_FEEDER.get(),
                AMBlockRegistry.VOID_WORM_BEAK.get(),
                AMBlockRegistry.BISON_FUR_BLOCK.get(),
                AMBlockRegistry.BISON_CARPET.get(),
                AMBlockRegistry.TRIOPS_EGGS.get()
        };
    }

    private static Block[] translucent() {
        return new Block[]{
                AMBlockRegistry.CAPSID.get(),
                AMBlockRegistry.RAINBOW_GLASS.get(),
                AMBlockRegistry.ENDER_RESIDUE.get(),
                AMBlockRegistry.TRANSMUTATION_TABLE.get(),
                AMBlockRegistry.SKUNK_SPRAY.get(),
                AMBlockRegistry.BANANA_SLUG_SLIME_BLOCK.get(),
                AMBlockRegistry.CRYSTALIZED_BANANA_SLUG_MUCUS.get()
        };
    }

    public static void register() {
        //? if <1.21.6 {
        net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlocks(
                net.minecraft.client.renderer.RenderType.cutout(), cutout());
        net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlocks(
                net.minecraft.client.renderer.RenderType.translucent(), translucent());
        //?} else if <26 {
        /*net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap.putBlocks(
                net.minecraft.client.renderer.chunk.ChunkSectionLayer.CUTOUT, cutout());
        net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap.putBlocks(
                net.minecraft.client.renderer.chunk.ChunkSectionLayer.TRANSLUCENT, translucent());
        *///?}
    }
}

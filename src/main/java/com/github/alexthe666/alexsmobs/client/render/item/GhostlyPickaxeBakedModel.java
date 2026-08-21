package com.github.alexthe666.alexsmobs.client.render.item;

// Fabric is on the >=1.21.4 side of this gate on EVERY version: BakedModelWrapper and the
// render-pass hooks below are Forge-family additions, and ClientProxy.onBakingCompleted — the
// only thing that ever constructs this — is itself !fabric-gated.
//? if <1.21.4 && !fabric {
import com.github.alexthe666.alexsmobs.client.render.AMRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class GhostlyPickaxeBakedModel extends BakedModelWrapper {

    public GhostlyPickaxeBakedModel(BakedModel bakedModel) {
        super(bakedModel);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return transformQuads(super.getQuads(state, side, rand));
    }

    //? if neoforge && >=1.21.2 {
    /*@Override
    public List<RenderType> getRenderTypes(ItemStack itemStack) {
        return List.of(AMRenderTypes.getGhostPickaxe(TextureAtlas.LOCATION_BLOCKS));
    }
    *///?} else {
    @Override
    public List<RenderType> getRenderTypes(ItemStack itemStack, boolean fabulous) {
        return List.of(AMRenderTypes.getGhostPickaxe(TextureAtlas.LOCATION_BLOCKS));
    }
    //?}

    @Override
    public BakedModel applyTransform(ItemDisplayContext cameraTransformType, PoseStack poseStack, boolean applyLeftHandTransform) {
        this.getTransforms().getTransform(cameraTransformType).apply(applyLeftHandTransform, poseStack);
        return this;
    }

    @Override
    public List<BakedQuad> getQuads(@org.jetbrains.annotations.Nullable BlockState state, @org.jetbrains.annotations.Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData extraData, @Nullable RenderType renderType) {
        return transformQuads(originalModel.getQuads(state, side, rand, extraData, renderType));
    }

    private static List<BakedQuad> transformQuads(List<BakedQuad> oldQuads) {
        List<BakedQuad> quads = new ArrayList<>();
        for(BakedQuad quad : oldQuads){
            quads.add(setFullbright(quad));
        }
        return quads;
    }

    private static BakedQuad setFullbright(BakedQuad quad) {
        int[] vertexData = quad.getVertices().clone();
        int step = vertexData.length / 4;

        vertexData[6] = 0x00F000F0;
        vertexData[6 + step] = 0x00F000F0;
        vertexData[6 + 2 * step] = 0x00F000F0;
        vertexData[6 + 3 * step] = 0x00F000F0;
        //? if >=1.21.2 {
        /*return new BakedQuad(vertexData, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade(), 0);
        *///?} else {
        return new BakedQuad(vertexData, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade());
        //?}
    }

    //? if neoforge && >=1.21.2 {
    /*@Override
    public List<BakedModel> getRenderPasses(ItemStack itemStack) {
        return List.of(this);
    }
    *///?} else {
    @Override
    public List<BakedModel> getRenderPasses(ItemStack itemStack, boolean fabulous) {
        return List.of(this);
    }
    //?}
}
//?}
//? if >=1.21.4 || fabric {
/*// 1.21.4's item-model-definition rework removed the BakedModelWrapper / render-pass hooks this used.
// The fullbright ghostly-pickaxe wrap is a cosmetic loss on >=1.21.4 (flagged in the porting notes). The class
// is kept as an inert stub so the (version-gated) references in ClientProxy still resolve.
public class GhostlyPickaxeBakedModel {
    public GhostlyPickaxeBakedModel(Object bakedModel) {
    }
}
*///?}

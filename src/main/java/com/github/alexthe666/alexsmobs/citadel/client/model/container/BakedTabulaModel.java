package com.github.alexthe666.alexsmobs.citadel.client.model.container;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.BakedQuad;
//? if <1.21.2 {
import net.minecraft.client.renderer.block.model.ItemOverrides;
//?}
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
// 1.21.5 removed net.minecraft.client.resources.model.BakedModel (replaced by BlockStateModel).
//? if <1.21.5 {
import net.minecraft.client.resources.model.BakedModel;
//?}
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

//? if >=1.21.5 {
/*// BakedModel was removed in 1.21.5 (replaced by BlockStateModel). This class is referenced only
// from the commented-out Tabula geometry loader, so on >=1.21.5 it degrades to an inert stub
// rather than tracking the new model-baking API.
public class BakedTabulaModel {
}
*///?} else {
public class BakedTabulaModel implements BakedModel {
    private final ImmutableList<BakedQuad> quads;
    private final TextureAtlasSprite particle;
    private final ImmutableMap<ItemDisplayContext, Transformation> transforms;

    public BakedTabulaModel(ImmutableList<BakedQuad> quads, TextureAtlasSprite particle, ImmutableMap<ItemDisplayContext, Transformation> transforms) {
        this.quads = quads;
        this.particle = particle;
        this.transforms = transforms;
    }

    @Override
    public List<BakedQuad> getQuads(@org.jetbrains.annotations.Nullable BlockState p_235039_, @org.jetbrains.annotations.Nullable Direction p_235040_, RandomSource p_235041_) {
        return this.quads;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return false;
    }

    @Override
    public boolean usesBlockLight() {
        return false;
    }

    // 1.21.4 removed BakedModel#isCustomRenderer (custom item renderers → SpecialModelRenderer).
    //? if <1.21.4 {
    @Override
    public boolean isCustomRenderer() {
        return false;
    }
    //?}

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return this.particle;
    }

    @Override
    public ItemTransforms getTransforms() {
        return ItemTransforms.NO_TRANSFORMS;
    }

    // 1.21.4 removed item overrides from BakedModel entirely (item model definitions replace them).
    //? if <1.21.2 {
    @Override
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }
    //?}
    //? if >=1.21.2 && <1.21.4 {
    /*@Override
    public net.minecraft.client.renderer.block.model.BakedOverrides overrides() {
        return net.minecraft.client.renderer.block.model.BakedOverrides.EMPTY;
    }
    *///?}
}
//?}

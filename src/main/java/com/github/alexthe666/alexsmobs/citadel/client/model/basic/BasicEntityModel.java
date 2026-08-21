package com.github.alexthe666.alexsmobs.citadel.client.model.basic;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.function.Function;

// Three eras of Model/EntityModel, kept as SIBLING branches rather than nested ones: commenting
// out a branch that itself contains a commented-out branch would close the block comment early.
//
//   <1.21     renderToBuffer takes four float colour channels — the vanilla signature.
//   1.21+     it takes one packed ARGB int; the float form stays this hierarchy's hook and the
//             vanilla entry point unpacks into it.
//   1.21.2+   EntityModel's type parameter became a render state, so the entity-parameterised base
//             moved to client.render.compat.EntityModel, which carries setupAnim/prepareMobModel
//             and the draw call. All that is left here is the parts() traversal.
//? if >=1.21.2 {
/*public abstract class BasicEntityModel<T extends Entity> extends com.github.alexthe666.alexsmobs.client.render.compat.EntityModel<T> {
    public int textureWidth = 64;
    public int textureHeight = 32;

    protected BasicEntityModel() {
        super();
    }

    protected BasicEntityModel(Function<ResourceLocation, RenderType> p_102613_) {
        super(p_102613_);
    }

    @Override
    public void renderToBuffer(PoseStack p_103013_, VertexConsumer p_103014_, int p_103015_, int p_103016_, float p_103017_, float p_103018_, float p_103019_, float p_103020_) {
        this.parts().forEach((p_103030_) -> {
            p_103030_.render(p_103013_, p_103014_, p_103015_, p_103016_, p_103017_, p_103018_, p_103019_, p_103020_);
        });
    }

    public abstract Iterable<BasicModelPart> parts();
}
*///?} elif >=1.21 {
/*public abstract class BasicEntityModel<T extends Entity> extends EntityModel<T> {
    public int textureWidth = 64;
    public int textureHeight = 32;

    protected BasicEntityModel() {
        this(RenderType::entityCutoutNoCull);
    }

    protected BasicEntityModel(Function<ResourceLocation, RenderType> p_102613_) {
        super(p_102613_);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        this.renderToBuffer(poseStack, buffer, packedLight, packedOverlay,
                net.minecraft.util.FastColor.ARGB32.red(color) / 255.0F,
                net.minecraft.util.FastColor.ARGB32.green(color) / 255.0F,
                net.minecraft.util.FastColor.ARGB32.blue(color) / 255.0F,
                net.minecraft.util.FastColor.ARGB32.alpha(color) / 255.0F);
    }

    public void renderToBuffer(PoseStack p_103013_, VertexConsumer p_103014_, int p_103015_, int p_103016_, float p_103017_, float p_103018_, float p_103019_, float p_103020_) {
        this.parts().forEach((p_103030_) -> {
            p_103030_.render(p_103013_, p_103014_, p_103015_, p_103016_, p_103017_, p_103018_, p_103019_, p_103020_);
        });
    }

    public abstract Iterable<BasicModelPart> parts();

    @Override
    public abstract void setupAnim(T p_102618_, float p_102619_, float p_102620_, float p_102621_, float p_102622_, float p_102623_);

    @Override
    public void prepareMobModel(T p_102614_, float p_102615_, float p_102616_, float p_102617_) {
    }
}
*///?} else {
public abstract class BasicEntityModel<T extends Entity> extends EntityModel<T> {
    public int textureWidth = 64;
    public int textureHeight = 32;

    protected BasicEntityModel() {
        this(RenderType::entityCutoutNoCull);
    }

    protected BasicEntityModel(Function<ResourceLocation, RenderType> p_102613_) {
        super(p_102613_);
    }

    @Override
    public void renderToBuffer(PoseStack p_103013_, VertexConsumer p_103014_, int p_103015_, int p_103016_, float p_103017_, float p_103018_, float p_103019_, float p_103020_) {
        this.parts().forEach((p_103030_) -> {
            p_103030_.render(p_103013_, p_103014_, p_103015_, p_103016_, p_103017_, p_103018_, p_103019_, p_103020_);
        });
    }

    public abstract Iterable<BasicModelPart> parts();

    @Override
    public abstract void setupAnim(T p_102618_, float p_102619_, float p_102620_, float p_102621_, float p_102622_, float p_102623_);

    @Override
    public void prepareMobModel(T p_102614_, float p_102615_, float p_102616_, float p_102617_) {
    }
}
//?}

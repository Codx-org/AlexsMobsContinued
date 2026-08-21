package com.github.alexthe666.alexsmobs.client.render;

import com.github.alexthe666.alexsmobs.misc.AMCompat;

import com.github.alexthe666.alexsmobs.entity.EntitySharkToothArrow;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class RenderSharkToothArrow extends ArrowRenderer<EntitySharkToothArrow
        //? if >=1.21.2 {
        /*, net.minecraft.client.renderer.entity.state.ArrowRenderState*/
        //?}
        > {
    private static final ResourceLocation TEXTURE = AMCompat.rl("alexsmobs:textures/entity/shark_tooth_arrow.png");

    public RenderSharkToothArrow(EntityRendererProvider.Context renderManagerIn) {
        super(renderManagerIn);
    }

    //? if >=1.21.2 {
    /*@Override
    public net.minecraft.client.renderer.entity.state.ArrowRenderState createRenderState() {
        return new net.minecraft.client.renderer.entity.state.ArrowRenderState();
    }

    @Override
    protected ResourceLocation getTextureLocation(net.minecraft.client.renderer.entity.state.ArrowRenderState state) {
        return TEXTURE;
    }
    *///?} else {
    @Override
    public ResourceLocation getTextureLocation(EntitySharkToothArrow entity) {
        return TEXTURE;
    }
    //?}
}

package com.github.alexthe666.alexsmobs.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;

// Vertex normals, which moved twice.
//
// 1.20.5 dropped VertexConsumer#normal(Matrix3f, x, y, z) in favour of a PoseStack.Pose overload.
// Alex's Mobs' hand-written quad emitters pass the normal matrix around, so this applies it by
// hand and falls through to the plain three-float normal.
//
// 1.21 then renamed that to setNormal. It is the one VertexConsumer rename that cannot be a
// blanket Stonecutter replacement (see stonecutter.gradle.kts): PoseStack.Pose#normal() is a
// no-arg getter with the same name, used all over the same files.
public class AMVertex {

    public static VertexConsumer normal(VertexConsumer consumer, Matrix3f normalMatrix, float x, float y, float z) {
        //? if >=1.20.5 {
        /*org.joml.Vector3f transformed = normalMatrix.transform(new org.joml.Vector3f(x, y, z)).normalize();
        return normal(consumer, transformed.x(), transformed.y(), transformed.z());
        *///?} else {
        return consumer.normal(normalMatrix, x, y, z);
        //?}
    }

    /**
     * The all-in-one "whole vertex" call used by the hand-written quad emitters in the vendored
     * Citadel model classes. 1.21 renamed it to addVertex and packed the four colour floats into
     * one ARGB int; everything else about it is unchanged.
     */
    public static void addVertex(VertexConsumer consumer, float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int packedOverlay, int packedLight, float nx, float ny, float nz) {
        //? if >=1.21 {
        /*consumer.addVertex(x, y, z, AMRenderCompat.packColor(red, green, blue, alpha), u, v, packedOverlay, packedLight, nx, ny, nz);
        *///?} else {
        consumer.vertex(x, y, z, red, green, blue, alpha, u, v, packedOverlay, packedLight, nx, ny, nz);
        //?}
    }

    public static VertexConsumer normal(VertexConsumer consumer, float x, float y, float z) {
        //? if >=1.21 {
        /*return consumer.setNormal(x, y, z);
        *///?} else {
        return consumer.normal(x, y, z);
        //?}
    }
}

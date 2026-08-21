package com.github.alexthe666.alexsmobs.client.render.compat;

// 1.21.9 gave block-entity renderers the same extract/submit split the entity renderers got in
// 1.21.2: BlockEntityRenderer<T> became BlockEntityRenderer<T, S extends BlockEntityRenderState>,
// and the draw call sees only the state. This is the state the compat BlockEntityRenderer uses —
// it carries the block entity itself plus the two other things the legacy render body was handed,
// so the eight tile renderers in this mod keep their pre-1.21.9 bodies verbatim.
//
// Holding the BlockEntity across the extract/submit boundary is exactly what AMRenderState does for
// entities: it is only read during the same frame, on the render thread, and the alternative is
// rewriting eight renderers to copy every field they touch into a state object.
//? if >=1.21.9 {
/*public class AMBlockEntityRenderState extends net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState {

    public net.minecraft.world.level.block.entity.BlockEntity tile;
    public float partialTick;
    public net.minecraft.world.phys.Vec3 camPos = net.minecraft.world.phys.Vec3.ZERO;
}
*///?}

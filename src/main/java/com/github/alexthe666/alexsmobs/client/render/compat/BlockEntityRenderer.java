package com.github.alexthe666.alexsmobs.client.render.compat;

// Pre-1.21.9 BlockEntityRenderer<T>, on top of 1.21.9's extract/submit architecture.
//
// The eight tile renderers in this mod keep their single type parameter and their
// render(T, float, PoseStack, MultiBufferSource, int, int, Vec3) body; the >=1.21.9
// `!mc2109-tile-import` replacement points their `import …blockentity.BlockEntityRenderer;` at this
// interface instead of the vanilla one, exactly the way the 1.21.2 render-state rewrite was absorbed
// for entity renderers. Below 1.21.9 this file is just a package declaration and their import
// resolves to vanilla as before.
//
// NOTE the simple name deliberately matches vanilla's — that is what makes the import swap work, and
// it is also the trap that silently retargeted mixin/renderstate/EntityRendererMixin once. No mixin
// may `import` this name; a mixin that must name a vanilla renderer has to spell it out fully.
//? if >=1.21.9 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public interface BlockEntityRenderer<T extends BlockEntity>
        extends net.minecraft.client.renderer.blockentity.BlockEntityRenderer<T, AMBlockEntityRenderState> {

    @Override
    default AMBlockEntityRenderState createRenderState() {
        return new AMBlockEntityRenderState();
    }

    @Override
    default void extractRenderState(T tile, AMBlockEntityRenderState state, float partialTick, Vec3 camPos, ModelFeatureRenderer.CrumblingOverlay crumbling) {
        net.minecraft.client.renderer.blockentity.BlockEntityRenderer.super.extractRenderState(tile, state, partialTick, camPos, crumbling);
        state.tile = tile;
        state.partialTick = partialTick;
        state.camPos = camPos;
    }

    @Override
    default void submit(AMBlockEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        AMSubmitBuffers buffers = new AMSubmitBuffers(collector, camera);
        @SuppressWarnings("unchecked")
        T tile = (T) state.tile;
        // The old signature's packedOverlay was NO_OVERLAY at every vanilla call site, and the new
        // one does not carry one at all.
        this.render(tile, state.partialTick, poseStack, buffers, state.lightCoords, OverlayTexture.NO_OVERLAY, state.camPos);
        buffers.flush();
    }

    void render(T tile, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, Vec3 camPos);
}
*///?}

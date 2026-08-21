package com.github.alexthe666.alexsmobs.citadel.client.gui;

import com.github.alexthe666.alexsmobs.misc.AMCompat;

import com.github.alexthe666.alexsmobs.citadel.client.gui.data.EntityLinkData;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;


public class EntityLinkButton extends Button {

    private static final Map<String, Entity> renderedEntites = new HashMap<>();
    private static final Quaternionf ENTITY_ROTATION = (new Quaternionf()).rotationXYZ((float) Math.toRadians(30), (float) Math.toRadians(130), (float) Math.PI);
    private final EntityLinkData data;
    private final GuiBasicBook bookGUI;

    public EntityLinkButton(GuiBasicBook bookGUI, EntityLinkData linkData, int k, int l, Button.OnPress o) {
        super(k + linkData.getX() - 12, l + linkData.getY(), (int) (24 * linkData.getScale()), (int) (24 * linkData.getScale()), CommonComponents.EMPTY, o, DEFAULT_NARRATION);
        this.data = linkData;
        this.bookGUI = bookGUI;
    }

    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        int lvt_5_1_ = 0;
        int lvt_6_1_ = 30;
        float f = (float) data.getScale();
        guiGraphics.pose().pushPose();
        com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.translateGui(guiGraphics, this.getX(), this.getY(), 0);
        com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.scaleGui(guiGraphics, f, f, 1);
        this.drawBtn(false, guiGraphics, 0, 0, lvt_5_1_, lvt_6_1_, 24, 24);
        Entity model = null;
        EntityType type = BuiltInRegistries.ENTITY_TYPE.get(AMCompat.rl(data.getEntity()));
        if (type != null) {
            // Same upstream `putIfAbsent` bug GuiBasicBook had: it returns the PREVIOUS mapping, so
            // the icon was blank on the frame it was first needed, and its argument is eager, so a
            // throwaway entity was built every single frame this button was on screen.
            model = renderedEntites.computeIfAbsent(data.getEntity(),
                    key -> AMCompat.createForDisplay(type, Minecraft.getInstance().level));
        }

        // MC 1.21.4 made enableScissor transform its rectangle by the current GUI pose — which here
        // already carries this method's translate(getX(), getY()) + scale(f, f). Handing it the
        // absolute rectangle from then on scissors to pose × that rectangle, i.e. roughly twice as
        // far from the origin as the button, so every icon in the animal dictionary was clipped away
        // while the two drawBtn calls (outside the scissor) kept drawing the empty frames. From
        // 1.21.4 the same window is spelled in the local space the pose is about to map. Below it,
        // the rectangle is raw screen coordinates and the absolute form is the correct one.
        //? if >=1.21.4 {
        /*guiGraphics.enableScissor(4, 4, 20, 20);
        *///?} else {
        guiGraphics.enableScissor(this.getX() + Math.round(f * 4), this.getY() + Math.round(f * 4), this.getX() + Math.round(f * 20), this.getY() + Math.round(f * 20));
        //?}
        if (model != null) {
            model.tickCount = Minecraft.getInstance().player.tickCount;
            // Upstream anchored the icon at a fixed point and then nudged it by the per-mob
            // offset_x/offset_y from the book JSON, each multiplied by that mob's entity_scale. The
            // laviathan asks for (-65, -28) at entity_scale 0.8 — 52px left of an anchor that sits
            // 11px inside a 24px frame — so it landed entirely outside the 4..20 scissor window and
            // its slot in the animal dictionary looked empty (report #34). The offsets are only
            // coherent at entity_scale 1; anything else walks out of its frame. Centre the mob from
            // its own bounding box instead and ignore them, the way AlexsMobsFP already does. That
            // also drops upstream's second error here: the frame scale f was multiplied into the
            // entity size AND applied again by the enclosing scale(f, f), squaring it.
            float widest = Math.max(model.getBbHeight(), model.getBbWidth() * 1.4F);
            float renderScale = Math.min(10F * (float) data.getEntityScale(), 16F / Math.max(widest, 0.01F));
            renderEntityInInventory(guiGraphics, 12, Math.round(12F + model.getBbHeight() * renderScale * 0.5F), renderScale, ENTITY_ROTATION, model);
        }
        guiGraphics.disableScissor();
        if (this.isHovered) {
            bookGUI.setEntityTooltip(this.data.getHoverText());
            lvt_5_1_ = 48;
        } else {
            lvt_5_1_ = 24;
        }
        this.drawBtn(!this.isHovered, guiGraphics, 0, 0, lvt_5_1_, lvt_6_1_, 24, 24);
        guiGraphics.pose().popPose();
    }

    public void drawBtn(boolean color, GuiGraphics guiGraphics, int p_238474_2_, int p_238474_3_, int p_238474_4_, int p_238474_5_, int p_238474_6_, int p_238474_7_) {
        if (color) {
            int widgetColor = bookGUI.getWidgetColor();
            int r = (widgetColor & 0xFF0000) >> 16;
            int g = (widgetColor & 0xFF00) >> 8;
            int b = (widgetColor & 0xFF);
            BookBlit.blitWithColor(guiGraphics, bookGUI.getBookWidgetTexture(), p_238474_2_, p_238474_3_, 0, (float) p_238474_4_, (float) p_238474_5_, p_238474_6_, p_238474_7_, 256, 256, r, g, b, 255);
        } else {
            // RenderType::guiTextured is gone at 1.21.6 (a RenderPipeline takes its place), so the
            // era split lives in AMRenderCompat rather than here.
            com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.blit(guiGraphics, bookGUI.getBookWidgetTexture(), p_238474_2_, p_238474_3_, p_238474_4_, p_238474_5_, p_238474_6_, p_238474_7_);
        }
    }


    public void renderEntityInInventory(GuiGraphics guiGraphics, int xPos, int yPos, float scale, Quaternionf rotation, Entity entity) {
        // 1.21.6: a GUI entity is a deferred picture-in-picture submission whose viewport rectangle
        // is in ABSOLUTE screen coordinates — the GUI matrix stack does not transform it. The only
        // enclosing transform is renderWidget's translate(getX(), getY()) + scale(f, f), so fold
        // that in by hand here. Vanilla's GuiEntityRenderer supplies the Y flip, the ENTITY_IN_UI
        // lighting and the shadow suppression that the block below did explicitly.
        //? if >=1.21.6 {
        /*float amScale = (float) this.data.getScale();
        com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.submitGuiEntity(guiGraphics, entity,
                this.getX() + Math.round(amScale * xPos), this.getY() + Math.round(amScale * yPos),
                amScale * scale, 0.0F, rotation, null);
        *///?} else {
        guiGraphics.pose().pushPose();
        com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.translateGui(guiGraphics, xPos, yPos, 50.0D);
        guiGraphics.pose().mulPoseMatrix((new Matrix4f()).scaling(scale, scale, (-scale)));
        guiGraphics.pose().mulPose(rotation);

        Vector3f light0 = new Vector3f(1, -1.0F, -1.0F).normalize();
        Vector3f light1 = new Vector3f(-1, 1.0F, 1.0F).normalize();
        RenderSystem.setShaderLights(light0, light1);
        EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        entityrenderdispatcher.setRenderShadow(false);
        //? if >=1.21.2 {
        /*entityrenderdispatcher.render(entity, 0.0D, 0.0D, 0.0D, 1.0F, guiGraphics.pose(), Minecraft.getInstance().renderBuffers().bufferSource(), 15728880);
        *///?} else {
        RenderSystem.runAsFancy(() -> {
            entityrenderdispatcher.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, guiGraphics.pose(), guiGraphics.bufferSource(), 15728880);
        });
        //?}
        guiGraphics.flush();
        entityrenderdispatcher.setRenderShadow(true);
        guiGraphics.pose().popPose();
        Lighting.setupFor3DItems();
        //?}
    }

}

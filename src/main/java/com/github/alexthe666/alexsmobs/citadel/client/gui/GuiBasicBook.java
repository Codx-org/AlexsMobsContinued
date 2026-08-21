package com.github.alexthe666.alexsmobs.citadel.client.gui;

import com.github.alexthe666.alexsmobs.misc.AMCompat;

import com.github.alexthe666.alexsmobs.citadel.Citadel;
import com.github.alexthe666.alexsmobs.citadel.client.gui.data.*;
import com.github.alexthe666.alexsmobs.citadel.client.model.TabulaModel;
import com.github.alexthe666.alexsmobs.citadel.client.model.TabulaModelHandler;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import org.apache.commons.io.IOUtils;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;

public abstract class GuiBasicBook extends Screen {

    // ⚠️ These MUST stay in the `alexsmobs` namespace. Citadel is vendored, so no `citadel:` resource
    // pack is mounted at runtime and a `citadel:` path silently resolves to the missing-texture
    // checkerboard — which is exactly what the Animal Dictionary rendered (black/magenta quadrants) on
    // every node from Milestone 1 until this was found. The four PNGs are vendored alongside the code,
    // from Citadel 2.6.3, under assets/alexsmobs/textures/gui/book/.
    private static final ResourceLocation BOOK_PAGE_TEXTURE = AMCompat.rl("alexsmobs:textures/gui/book/book_pages.png");
    private static final ResourceLocation BOOK_BINDING_TEXTURE = AMCompat.rl("alexsmobs:textures/gui/book/book_binding.png");
    private static final ResourceLocation BOOK_WIDGET_TEXTURE = AMCompat.rl("alexsmobs:textures/gui/book/widgets.png");
    private static final ResourceLocation BOOK_BUTTONS_TEXTURE = AMCompat.rl("alexsmobs:textures/gui/book/link_buttons.png");
    protected final List<LineData> lines = new ArrayList<>();
    protected final List<LinkData> links = new ArrayList<>();
    protected final List<ItemRenderData> itemRenders = new ArrayList<>();
    protected final List<RecipeData> recipes = new ArrayList<>();
    protected final List<TabulaRenderData> tabulaRenders = new ArrayList<>();
    protected final List<EntityRenderData> entityRenders = new ArrayList<>();
    protected final List<EntityLinkData> entityLinks = new ArrayList<>();
    protected final List<ImageData> images = new ArrayList<>();
    protected final List<Whitespace> yIndexesToSkip = new ArrayList<>();
    private final Map<String, TabulaModel> renderedTabulaModels = new HashMap<>();
    private final Map<String, Entity> renderedEntites = new HashMap<>();
    private final Map<String, ResourceLocation> textureMap = new HashMap<>();
    protected ItemStack bookStack;
    protected int xSize = 390;
    protected int ySize = 320;
    /**
     * Usable pixel width of each text column, matching how {@link #writePageText} lays them out:
     * text starts at {@code x + 10} and the second column at {@code x + 210}, so the left column
     * has 190 px before it runs into the right one and the right column has 170 px before the page
     * edge. Upstream wraps by CHARACTER COUNT alone, which for a proportional font is only an
     * approximation of either — see {@link #readInPageText}.
     */
    private static final int COLUMN_WIDTH_LEFT = 190;
    private static final int COLUMN_WIDTH_RIGHT = 170;
    protected int currentPageCounter = 0;
    protected int maxPagesFromPrinting = 0;
    protected int linesFromJSON = 0;
    protected int linesFromPrinting = 0;
    protected ResourceLocation prevPageJSON;
    protected ResourceLocation currentPageJSON;
    protected ResourceLocation currentPageText = null;
    protected BookPageButton buttonNextPage;
    protected BookPageButton buttonPreviousPage;
    protected BookPage internalPage = null;
    protected String writtenTitle = "";
    protected int preservedPageIndex = 0;
    protected String entityTooltip;
    private int mouseX;
    private int mouseY;

    public GuiBasicBook(ItemStack bookStack, Component title) {
        super(title);
        this.bookStack = bookStack;
        this.currentPageJSON = getRootPage();
    }

    public static void drawTabulaModelOnScreen(GuiGraphics guiGraphics, TabulaModel model, ResourceLocation tex, int posX, int posY, float scale, boolean follow, double xRot, double yRot, double zRot, float mouseX, float mouseY) {
        float f = (float) Math.atan(mouseX / 40.0F);
        float f1 = (float) Math.atan(mouseY / 40.0F);
        PoseStack matrixstack = new PoseStack();
        matrixstack.translate((float) posX, (float) posY, 120.0D);
        matrixstack.scale(scale, scale, scale);
        Quaternionf quaternion = Axis.ZP.rotationDegrees(0.0F);
        Quaternionf quaternion1 = Axis.XP.rotationDegrees(f1 * 20.0F);
        if (follow) {
            quaternion.mul(quaternion1);
        }
        matrixstack.mulPose(quaternion);
        if (follow) {
            matrixstack.mulPose(Axis.YP.rotationDegrees(180.0F + f * 40.0F));
        }
        matrixstack.mulPose(Axis.XP.rotationDegrees((float) -xRot));
        matrixstack.mulPose(Axis.YP.rotationDegrees((float) yRot));
        matrixstack.mulPose(Axis.ZP.rotationDegrees((float) zRot));
        EntityRenderDispatcher entityrenderermanager = Minecraft.getInstance().getEntityRenderDispatcher();
        quaternion1.conjugate();
        // 1.21.9 removed both dispatcher-wide overrides: the camera orientation is a field of the
        // per-frame CameraRenderState now, and the shadow is a property of each entity's render state
        // (shadowRadius / shadowPieces). Neither is read by this method — it draws a Tabula model
        // straight into the buffer source, not through an entity renderer — so they are simply dropped.
        //? if <1.21.9 {
        entityrenderermanager.overrideCameraOrientation(quaternion1);
        entityrenderermanager.setRenderShadow(false);
        //?}
        MultiBufferSource.BufferSource irendertypebuffer$impl = Minecraft.getInstance().renderBuffers().bufferSource();
        com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.runAsFancy(() -> {
            VertexConsumer ivertexbuilder = irendertypebuffer$impl.getBuffer(RenderType.entityCutoutNoCull(tex));
            model.resetToDefaultPose();
            model.renderToBuffer(matrixstack, ivertexbuilder, 15728880, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        });
        // 1.21.6 deleted Lighting.setupFor3DItems — GUI lighting is a property of the submit pass now.
        //? if <1.21.6 {
        Lighting.setupFor3DItems();
        //?}
    }

    public void drawEntityOnScreen(GuiGraphics guiGraphics, MultiBufferSource bufferSource, int posX, int posY, float zOff, float scale, boolean follow, double xRot, double yRot, double zRot, float mouseX, float mouseY, Entity entity) {
        float customYaw = posX - mouseX;
        float customPitch = posY - mouseY;
        float f = (float) Math.atan(customYaw / 40.0F);
        float f1 = (float) Math.atan(customPitch / 40.0F);

        if (follow) {
            float setX = f1 * 20.0F;
            float setY = f * 20.0F;
            entity.setXRot(setX);
            entity.setYRot(setY);
            if (entity instanceof LivingEntity) {
                ((LivingEntity) entity).yBodyRot = setY;
                ((LivingEntity) entity).yBodyRotO = setY;
                ((LivingEntity) entity).yHeadRot = setY;
                ((LivingEntity) entity).yHeadRotO = setY;
            }
        } else {
            f = 0;
            f1 = 0;
        }

        Quaternionf quaternion = Axis.ZP.rotationDegrees(180F);
        Quaternionf quaternion1 = Axis.XP.rotationDegrees(f1 * 20.0F);
        quaternion.mul(quaternion1);
        quaternion.mul(Axis.XN.rotationDegrees((float) xRot));
        quaternion.mul(Axis.YP.rotationDegrees((float) yRot));
        quaternion.mul(Axis.ZP.rotationDegrees((float) zRot));

        // 1.21.6 removed the immediate-mode entity-in-GUI path (no PoseStack on GuiGraphics, no
        // flush(), no RenderSystem.setShaderLights). A GUI entity is a deferred picture-in-picture
        // submission now, whose viewport rectangle is in absolute screen coordinates and puts the
        // entity's origin at the rectangle's centre with the Y flip and the ENTITY_IN_UI lighting
        // already applied — i.e. exactly what the translate/scale/rotate sequence below did by hand.
        // Vanilla's GuiEntityRenderer also suppresses the shadow and conjugates the camera angle
        // itself, so quaternion1 goes in un-conjugated.
        //? if >=1.21.6 {
        /*com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.submitGuiEntity(guiGraphics, entity, posX, posY, scale, 0.0F, quaternion, quaternion1);
        *///?} else {
        guiGraphics.pose().pushPose();
        com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.translateGui(guiGraphics, posX, posY, zOff);
        guiGraphics.pose().mulPoseMatrix((new Matrix4f()).scaling(scale, scale, -scale));
        guiGraphics.pose().mulPose(quaternion);

        Vector3f light0 = new Vector3f(1, -1.0F, -1.0F).normalize();
        Vector3f light1 = new Vector3f(-1, 1.0F, 1.0F).normalize();
        RenderSystem.setShaderLights(light0, light1);
        EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        quaternion1.conjugate();
        entityrenderdispatcher.overrideCameraOrientation(quaternion1);
        entityrenderdispatcher.setRenderShadow(false);
        com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.runAsFancy(() -> {
            // 1.21.2 dropped EntityRenderDispatcher#render's partialTicks argument.
            //? if >=1.21.2 {
            /*entityrenderdispatcher.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, guiGraphics.pose(), bufferSource, 240);
            *///?} else {
            entityrenderdispatcher.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, guiGraphics.pose(), bufferSource, 240);
            //?}
        });
        entityrenderdispatcher.setRenderShadow(true);
        guiGraphics.flush();
        guiGraphics.pose().popPose();
        Lighting.setupFor3DItems();
        //?}

        entity.setYRot(0);
        entity.setXRot(0);
        if (entity instanceof LivingEntity) {
            ((LivingEntity) entity).yBodyRot = 0;
            ((LivingEntity) entity).yHeadRotO = 0;
            ((LivingEntity) entity).yHeadRot = 0;
        }
    }

    protected void init() {
        super.init();
        playBookOpeningSound();
        addNextPreviousButtons();
        addLinkButtons();
    }

    private void addNextPreviousButtons() {
        int k = (this.width - this.xSize) / 2;
        int l = (this.height - this.ySize + 128) / 2;
        this.buttonPreviousPage = this.addRenderableWidget(new BookPageButton(this, k + 10, l + 180, false, (p_214208_1_) -> {
            this.onSwitchPage(false);
        }, true));
        this.buttonNextPage = this.addRenderableWidget(new BookPageButton(this, k + 365, l + 180, true, (p_214205_1_) -> {
            this.onSwitchPage(true);
        }, true));
    }

    private void addLinkButtons() {
        this.renderables.clear();
        this.clearWidgets();
        addNextPreviousButtons();
        int k = (this.width - this.xSize) / 2;
        int l = (this.height - this.ySize + 128) / 2;

        for (LinkData linkData : links) {
            if (linkData.getPage() == this.currentPageCounter) {
                int maxLength = Math.max(100, Minecraft.getInstance().font.width(linkData.getTitleText()) + 20);
                yIndexesToSkip.add(new Whitespace(linkData.getPage(), linkData.getX() - maxLength / 2, linkData.getY(), 100, 20));
                this.addRenderableWidget(new LinkButton(this, k + linkData.getX() - maxLength / 2, l + linkData.getY(), maxLength, 20, Component.translatable(linkData.getTitleText()), linkData.getDisplayItem(), (p_213021_1_) -> {
                    prevPageJSON = this.currentPageJSON;
                    currentPageJSON = AMCompat.rl(getTextFileDirectory() + linkData.getLinkedPage());
                    preservedPageIndex = this.currentPageCounter;
                    currentPageCounter = 0;
                    addNextPreviousButtons();
                }));
            }
            if (linkData.getPage() > this.maxPagesFromPrinting) {
                this.maxPagesFromPrinting = linkData.getPage();
            }
        }

        for (EntityLinkData linkData : entityLinks) {
            if (linkData.getPage() == this.currentPageCounter) {
                yIndexesToSkip.add(new Whitespace(linkData.getPage(), linkData.getX() - 12, linkData.getY(), 100, 20));
                this.addRenderableWidget(new EntityLinkButton(this, linkData, k, l, (p_213021_1_) -> {
                    prevPageJSON = this.currentPageJSON;
                    currentPageJSON = AMCompat.rl(getTextFileDirectory() + linkData.getLinkedPage());
                    preservedPageIndex = this.currentPageCounter;
                    currentPageCounter = 0;
                    addNextPreviousButtons();
                }));
            }
            if (linkData.getPage() > this.maxPagesFromPrinting) {
                this.maxPagesFromPrinting = linkData.getPage();
            }
        }
    }

    private void onSwitchPage(boolean next) {
        if (next) {
            if (currentPageCounter < maxPagesFromPrinting) {
                currentPageCounter++;
            }
        } else {
            if (currentPageCounter > 0) {
                currentPageCounter--;
            } else {
                if (this.internalPage != null && !this.internalPage.getParent().isEmpty()) {
                    prevPageJSON = this.currentPageJSON;
                    currentPageJSON = AMCompat.rl(getTextFileDirectory() + this.internalPage.getParent());
                    currentPageCounter = preservedPageIndex;
                    preservedPageIndex = 0;
                }
            }
        }
        refreshSpacing();
    }

    /**
     * True only while {@code Screen#render} is running, which makes its own background pass a
     * no-op. See the block comment in {@link #render} for why that pass is harmful here.
     */
    private boolean suppressBackground;

    //? if >=1.20.2 {
    /*@Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if (suppressBackground) {
            return;
        }
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
    }
    *///?}

    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, float partialTicks) {
        this.mouseX = x;
        this.mouseY = y;
        int bindingColor = getBindingColor();
        int bindingR = bindingColor >> 16 & 255;
        int bindingG = bindingColor >> 8 & 255;
        int bindingB = bindingColor & 255;
        // Draw the screen background exactly ONCE, and first. Where vanilla does it moved twice
        // (javap-verified on the mojmap jars):
        //   1.20.1         Screen#render draws only the widgets — the screen calls it itself.
        //   1.20.2-1.21.5  Screen#render calls renderBackground FIRST, then the widgets.
        //   1.21.6+        Screen#renderWithTooltip calls it, before render() is entered.
        // Upstream Citadel was written against the first shape. From 1.20.2 up that made the
        // super.render() call below re-run the background AFTER the page texture and every line
        // of page text had been drawn — renderBlurredBackground blurs the main render target in
        // place and the in-world menu texture darkens it, so the book blurred and washed out its
        // own contents while the widgets and tooltips drawn afterwards stayed crisp. That is the
        // reported "the text is now not really visible". Hence: the explicit call below is gated
        // to the versions where vanilla does not make it for us, and the one inside super.render()
        // is suppressed (see renderBackground).
        // 1.20.2 also gave Screen#renderBackground the mouse position + partial tick.
        //? if >=1.20.2 && <1.21.6
        //this.renderBackground(guiGraphics, x, y, partialTicks);
        //? if <1.20.2
        this.renderBackground(guiGraphics);
        int k = (this.width - this.xSize) / 2;
        int l = (this.height - this.ySize + 128) / 2;
        BookBlit.blitWithColor(guiGraphics, getBookBindingTexture(), k, l, 0, 0, xSize, ySize, xSize, ySize, bindingR, bindingG, bindingB, 255);
        BookBlit.blitWithColor(guiGraphics, getBookPageTexture(), k, l, 0, 0, xSize, ySize, xSize, ySize, 255, 255, 255, 255);
        if (internalPage == null || currentPageJSON != prevPageJSON || prevPageJSON == null) {
            internalPage = generatePage(currentPageJSON);
            if (internalPage != null) {
                refreshSpacing();
            }
        }
        if (internalPage != null) {
            writePageText(guiGraphics, x, y);
        }
        // Widgets only — see the note above; the background pass inside Screen#render would
        // otherwise blur and darken the page and text this method has already drawn.
        suppressBackground = true;
        super.render(guiGraphics, x, y, partialTicks);
        suppressBackground = false;
        prevPageJSON = currentPageJSON;
        if (internalPage != null) {
            guiGraphics.pose().pushPose();
            renderOtherWidgets(guiGraphics, x, y, internalPage);
            guiGraphics.pose().popPose();
        }
        if (this.entityTooltip != null) {
            guiGraphics.pose().pushPose();
            com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.translateGui(guiGraphics, 0, 0, 550);
            // 1.21.6 renamed the deferred-tooltip entry point; tooltips are drawn after the rest of
            // the screen either way, so the z nudge above is now purely decorative.
            //? if >=1.21.6 {
            /*guiGraphics.setTooltipForNextFrame(font, Minecraft.getInstance().font.split(Component.translatable(entityTooltip), Math.max(this.width / 2 - 43, 170)), x, y);
            *///?} else {
            guiGraphics.renderTooltip(font, Minecraft.getInstance().font.split(Component.translatable(entityTooltip), Math.max(this.width / 2 - 43, 170)), x, y);
            //?}
            entityTooltip = null;
            guiGraphics.pose().popPose();
        }
    }

    private void refreshSpacing() {
        if (internalPage != null) {
            String lang = Minecraft.getInstance().getLanguageManager().getSelected().toLowerCase();
            currentPageText = AMCompat.rl(getTextFileDirectory() + lang + "/" + internalPage.getTextFileToReadFrom());
            boolean invalid = false;
            try {
                //test if it exists. if no exception, then the language is supported
                InputStream is = Minecraft.getInstance().getResourceManager().open(currentPageText);
                is.close();
            } catch (Exception e) {
                invalid = true;
                Citadel.LOGGER.warn("Could not find language file for translation, defaulting to english");
                currentPageText = AMCompat.rl(getTextFileDirectory() + "en_us/" + internalPage.getTextFileToReadFrom());
            }

            readInPageWidgets(internalPage);
            addWidgetSpacing();
            addLinkButtons();
            readInPageText(currentPageText);
        }
    }

    private Item getItemByRegistryName(String registryName) {
        return BuiltInRegistries.ITEM.get(AMCompat.rl(registryName));
    }

    // 1.21.2 removed every client-side recipe-by-id lookup, so the book parses its own shipped
    // recipe JSON (BookRecipe) on all nodes. See BookRecipe for why. The former SpecialRecipeInGuideBook
    // display path is dropped — nothing in the mod implements that interface.
    private BookRecipe getRecipeByName(String registryName) {
        return BookRecipe.get(registryName);
    }

    private void addWidgetSpacing() {
        yIndexesToSkip.clear();
        for (ItemRenderData itemRenderData : itemRenders) {
            Item item = getItemByRegistryName(itemRenderData.getItem());
            if (item != null) {
                yIndexesToSkip.add(new Whitespace(itemRenderData.getPage(), itemRenderData.getX(), itemRenderData.getY(), (int) (itemRenderData.getScale() * 17), (int) (itemRenderData.getScale() * 15)));

            }
        }
        for (RecipeData recipeData : recipes) {
            BookRecipe recipe = getRecipeByName(recipeData.getRecipe());
            if (recipe != null) {
                yIndexesToSkip.add(new Whitespace(recipeData.getPage(), recipeData.getX(), recipeData.getY() - (int) (recipeData.getScale() * 15), (int) (recipeData.getScale() * 35), (int) (recipeData.getScale() * 60), true));
            }
        }
        for (ImageData imageData : images) {
            if (imageData != null) {
                yIndexesToSkip.add(new Whitespace(imageData.getPage(), imageData.getX(), imageData.getY(), (int) (imageData.getScale() * imageData.getWidth()), (int) (imageData.getScale() * imageData.getHeight() * 0.8F)));
            }
        }
        if (!writtenTitle.isEmpty()) {
            yIndexesToSkip.add(new Whitespace(0, 20, 5, 70, 15));
        }
    }

    private void renderOtherWidgets(GuiGraphics guiGraphics, int x, int y, BookPage page) {
        int color = getBindingColor();
        int r = (color & 0xFF0000) >> 16;
        int g = (color & 0xFF00) >> 8;
        int b = (color & 0xFF);

        int k = (this.width - this.xSize) / 2;
        int l = (this.height - this.ySize + 128) / 2;

        for (ImageData imageData : images) {
            if (imageData.getPage() == this.currentPageCounter) {
                if (imageData != null) {
                    ResourceLocation tex = textureMap.get(imageData.getTexture());
                    if (tex == null) {
                        tex = AMCompat.rl(imageData.getTexture());
                        textureMap.put(imageData.getTexture(), tex);
                    }
                    // yIndexesToSkip.put(imageData.getPage(), new Whitespace(imageData.getX(), imageData.getY(),(int) (imageData.getScale() * imageData.getWidth()), (int) (imageData.getScale() * imageData.getHeight() * 0.8F)));
                    float scale = (float) imageData.getScale();
                    guiGraphics.pose().pushPose();
                    com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.translateGui(guiGraphics, k + imageData.getX(), l + imageData.getY(), 0);
                    com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.scaleGui(guiGraphics, scale, scale, scale);
                    com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.blit(guiGraphics, tex, 0, 0, imageData.getU(), imageData.getV(), imageData.getWidth(), imageData.getHeight());
                    guiGraphics.pose().popPose();
                }
            }
        }
        for (RecipeData recipeData : recipes) {
            if (recipeData.getPage() == this.currentPageCounter) {
                guiGraphics.pose().pushPose();
                com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.translateGui(guiGraphics, k + recipeData.getX(), l + recipeData.getY(), 0);
                float scale = (float) recipeData.getScale();
                com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.scaleGui(guiGraphics, scale, scale, scale);
                com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.blit(guiGraphics, getBookWidgetTexture(), 0, 0, 0, 88, 116, 53);
                guiGraphics.pose().popPose();
            }
        }

        for (TabulaRenderData tabulaRenderData : tabulaRenders) {
            if (tabulaRenderData.getPage() == this.currentPageCounter) {
                TabulaModel model = null;
                ResourceLocation texture;
                // Upstream wrote `texture = textureMap.put(key, value)`, but Map#put returns the
                // PREVIOUS mapping — null on the very first insert — so the model was skipped on the
                // frame it was first needed. computeIfAbsent returns the value that ends up in the map.
                texture = textureMap.computeIfAbsent(tabulaRenderData.getTexture(), AMCompat::rl);
                if (renderedTabulaModels.get(tabulaRenderData.getModel()) != null) {
                    model = renderedTabulaModels.get(tabulaRenderData.getModel());
                } else {
                    try {
                        model = new TabulaModel(TabulaModelHandler.INSTANCE.loadTabulaModel("/assets/" + tabulaRenderData.getModel().split(":")[0] + "/" + tabulaRenderData.getModel().split(":")[1]));
                    } catch (Exception e) {
                        Citadel.LOGGER.warn("Could not load in tabula model for book at " + tabulaRenderData.getModel());
                    }
                    renderedTabulaModels.put(tabulaRenderData.getModel(), model);
                }

                if (model != null && texture != null) {
                    float scale = (float) tabulaRenderData.getScale();
                    drawTabulaModelOnScreen(guiGraphics, model, texture, k + tabulaRenderData.getX(), l + tabulaRenderData.getY(), 30 * scale, tabulaRenderData.isFollow_cursor(), tabulaRenderData.getRot_x(), tabulaRenderData.getRot_y(), tabulaRenderData.getRot_z(), mouseX, mouseY);
                }
            }
        }
        for (EntityRenderData data : entityRenders) {
            if (data.getPage() == this.currentPageCounter) {
                Entity model = null;
                EntityType type = BuiltInRegistries.ENTITY_TYPE.get(AMCompat.rl(data.getEntity()));
                if (type != null) {
                    // Upstream wrote `putIfAbsent(key, create(...))`, which is wrong twice over: it
                    // returns the PREVIOUS mapping (null the first time, so the entity never rendered
                    // on the frame it was first needed), and its argument is evaluated eagerly, so a
                    // fresh entity was constructed on EVERY frame the page was open and immediately
                    // thrown away. computeIfAbsent fixes both. A failed construction leaves the map
                    // untouched, so it is simply retried next frame rather than caching a null.
                    model = renderedEntites.computeIfAbsent(data.getEntity(),
                            key -> AMCompat.createForDisplay(type, Minecraft.getInstance().level));
                }
                if (model != null) {
                    float scale = (float) data.getScale();
                    model.tickCount = Minecraft.getInstance().player.tickCount;
                    if (data.getEntityData() != null) {
                        try {
                            CompoundTag tag = TagParser.parseTag(data.getEntityData());
                            AMCompat.loadEntity(model, tag);
                        } catch (CommandSyntaxException e) {
                            e.printStackTrace();
                        }
                    }
                    //? if >=1.21.2 {
                    /*drawEntityOnScreen(guiGraphics, Minecraft.getInstance().renderBuffers().bufferSource(), k + data.getX(), l + data.getY(), 1050F, 30 * scale, data.isFollow_cursor(), data.getRot_x(), data.getRot_y(), data.getRot_z(), mouseX, mouseY, model);
                    *///?} else {
                    drawEntityOnScreen(guiGraphics, guiGraphics.bufferSource(), k + data.getX(), l + data.getY(), 1050F, 30 * scale, data.isFollow_cursor(), data.getRot_x(), data.getRot_y(), data.getRot_z(), mouseX, mouseY, model);
                    //?}
                }
            }
        }
        for (RecipeData recipeData : recipes) {
            if (recipeData.getPage() == this.currentPageCounter) {
                BookRecipe recipe = getRecipeByName(recipeData.getRecipe());
                if (recipe != null) {
                    renderRecipe(guiGraphics, recipe, recipeData, k, l);
                }
            }
        }
        for (ItemRenderData itemRenderData : itemRenders) {
            if (itemRenderData.getPage() == this.currentPageCounter) {
                Item item = getItemByRegistryName(itemRenderData.getItem());
                if (item != null) {
                    float scale = (float) itemRenderData.getScale();
                    ItemStack stack = new ItemStack(item);
                    if (itemRenderData.getItemTag() != null && !itemRenderData.getItemTag().isEmpty()) {
                        CompoundTag tag = null;
                        try {
                            tag = TagParser.parseTag(itemRenderData.getItemTag());
                        } catch (CommandSyntaxException e) {
                            e.printStackTrace();
                        }
                        AMCompat.setTag(stack, tag);
                    }
                    guiGraphics.pose().pushPose();
                    com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.translateGui(guiGraphics, k, l, 0);
                    com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.scaleGui(guiGraphics, scale, scale, scale);
                    guiGraphics.renderItem(stack, itemRenderData.getX(), itemRenderData.getY());
                    guiGraphics.pose().popPose();
                }
            }
        }
    }

    protected void renderRecipe(GuiGraphics guiGraphics, BookRecipe recipe, RecipeData recipeData, int k, int l) {
        int playerTicks = Minecraft.getInstance().player.tickCount;
        float scale = (float) recipeData.getScale();
        List<ItemStack[]> ingredients = recipe.getIngredients();

        for (int i = 0; i < ingredients.size(); i++) {
            ItemStack[] options = ingredients.get(i);
            if (options.length == 0) {
                continue;
            }
            // Slots that accept several items cycle through them once a second.
            ItemStack stack = options.length > 1 ? options[(int) ((playerTicks / 20F) % options.length)] : options[0];
            if (!stack.isEmpty()) {
                guiGraphics.pose().pushPose();
                com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.translateGui(guiGraphics, k, l, 32.0F);
                com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.translateGui(guiGraphics, (int) (recipeData.getX() + (i % 3) * 20 * scale), (int) (recipeData.getY() + (i / 3) * 20 * scale), 0);
                com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.scaleGui(guiGraphics, scale, scale, scale);
                guiGraphics.renderItem(stack, 0, 0);
                guiGraphics.pose().popPose();
            }
        }
        ItemStack result = recipe.getResult();
        if (!result.isEmpty()) {
            guiGraphics.pose().pushPose();
            com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.translateGui(guiGraphics, k, l, 32.0F);
            float finScale = scale * 1.5F;
            com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.translateGui(guiGraphics, recipeData.getX() + 70 * finScale, recipeData.getY() + 10 * finScale, 0);
            com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.scaleGui(guiGraphics, finScale, finScale, finScale);
            com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.translateGui(guiGraphics, 0.0F, 0.0F, 100.0F);
            guiGraphics.renderItem(result, 0, 0);
            guiGraphics.pose().popPose();
        }
    }

    protected void writePageText(GuiGraphics guiGraphics, int x, int y) {
        Font font = this.font;
        int k = (this.width - this.xSize) / 2;
        int l = (this.height - this.ySize + 128) / 2;
        for (LineData line : this.lines) {
            if (line.getPage() == this.currentPageCounter) {
                guiGraphics.drawString(font, line.getText(), k + 10 + line.getxIndex(), l + 10 + line.getyIndex() * 12, getTextColor(), false);
            }
        }
        if (this.currentPageCounter == 0 && !writtenTitle.isEmpty()) {
            String actualTitle = I18n.get(writtenTitle);
            guiGraphics.pose().pushPose();
            float scale = 2F;
            if (font.width(actualTitle) > 80) {
                scale = 2.0F - Mth.clamp((font.width(actualTitle) - 80) * 0.011F, 0, 1.95F);
            }
            com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.translateGui(guiGraphics, k + 10, l + 10, 0);
            com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.scaleGui(guiGraphics, scale, scale, scale);
            guiGraphics.drawString(font, actualTitle, 0, 0, getTitleColor(), false);
            guiGraphics.pose().popPose();
        }
        this.buttonNextPage.visible = currentPageCounter < maxPagesFromPrinting;
        this.buttonPreviousPage.visible = currentPageCounter > 0 || !currentPageJSON.equals(this.getRootPage());
    }

    public boolean isPauseScreen() {
        return false;
    }

    protected void playBookOpeningSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
    }

    protected void playBookClosingSound() {
    }

    protected abstract int getBindingColor();

    protected int getWidgetColor() {
        return getBindingColor();
    }

    /**
     * ⚠️ The alpha byte is <b>mandatory</b>. Through 1.21.1 {@code Font} opened with
     * {@code if ((color & 0xFC000000) == 0) color |= 0xFF000000;} — so upstream's bare
     * {@code 0x303030} was silently promoted to opaque. <b>1.21.2 deleted that guard</b>
     * (bytecode-verified: the constant pair is in 1.20.4/1.20.6/1.21.1's {@code Font} and absent
     * from 1.21.2 onward), so an alpha-less colour now draws completely transparent — the whole
     * left-hand page of the animal dictionary was blank on every node from 1.21.2 up. Spelling the
     * alpha out is correct on <i>all</i> versions, which is why this needs no version gate.
     */
    protected int getTextColor() {
        return 0XFF303030;
    }

    /**
     * Citadel's own value here is {@code 0xBAAC98} — a light tan. Against {@code book_pages.png},
     * which is near-white ({@code 253,248,236}) everywhere the title lands, that is a contrast ratio
     * of <b>1.6:1</b>, i.e. the title is effectively invisible. (Verified by compositing the tinted
     * binding and the page texture and sampling under the glyph positions; the body text's
     * {@code 0x303030} is 12.6:1 and is fine.) Upstream shipped it that way, so this is a deliberate
     * deviation, not a porting regression — dark sepia reads as ink on the cream page.
     * <p>
     * The leading {@code FF} is required from 1.21.2 — see {@link #getTextColor()}.
     */
    protected int getTitleColor() {
        return 0XFF3F3222;
    }

    public abstract ResourceLocation getRootPage();

    public abstract String getTextFileDirectory();

    protected ResourceLocation getBookPageTexture() {
        return BOOK_PAGE_TEXTURE;
    }

    protected ResourceLocation getBookBindingTexture() {
        return BOOK_BINDING_TEXTURE;
    }

    protected ResourceLocation getBookWidgetTexture() {
        return BOOK_WIDGET_TEXTURE;
    }

    protected void playPageFlipSound() {
    }

    @Nullable
    protected BookPage generatePage(ResourceLocation res) {
        Optional<Resource> resource = null;
        BookPage page = null;
        try {
            resource = Minecraft.getInstance().getResourceManager().getResource(res);
            try {
                resource = Minecraft.getInstance().getResourceManager().getResource(res);
                if (resource.isPresent()) {
                    BufferedReader inputstream = resource.get().openAsReader();
                    page = BookPage.deserialize(inputstream);
                }

            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } catch (Exception e) {
            return null;
        }
        return page;
    }

    protected void readInPageWidgets(BookPage page) {
        links.clear();
        itemRenders.clear();
        recipes.clear();
        tabulaRenders.clear();
        entityRenders.clear();
        images.clear();
        entityLinks.clear();
        links.addAll(page.getLinkedButtons());
        entityLinks.addAll(page.getLinkedEntities());
        itemRenders.addAll(page.getItemRenders());
        recipes.addAll(page.getRecipes());
        tabulaRenders.addAll(page.getTabulaRenders());
        entityRenders.addAll(page.getEntityRenders());
        images.addAll(page.getImages());
        writtenTitle = page.generateTitle();
    }

    protected void readInPageText(ResourceLocation res) {
        Resource resource = null;
        int xIndex = 0;
        int actualTextX = 0;
        int yIndex = 0;
        try {
            BufferedReader bufferedreader = Minecraft.getInstance().getResourceManager().openAsReader(res);
            try {
                List<String> readStrings = IOUtils.readLines(bufferedreader);
                this.linesFromJSON = readStrings.size();
                this.lines.clear();
                Font pageFont = Minecraft.getInstance().font;
                List<String> splitBySpaces = new ArrayList<>();
                for (String line : readStrings) {
                    for (String word : line.split(" ")) {
                        // A "word" here is whatever sits between two spaces, and languages that do
                        // not space their words (zh/ja/ko) therefore hand us a whole source line as
                        // one token — far wider than a column. Break those up front so the wrap
                        // loop below always has something it can fit.
                        splitBySpaces.addAll(hardSplitToWidth(pageFont, word, COLUMN_WIDTH_RIGHT - 10));
                    }
                }
                String lineToPrint = "";
                linesFromPrinting = 0;
                int page = 0;
                for (int i = 0; i < splitBySpaces.size(); i++) {
                    String word = splitBySpaces.get(i);
                    int cutoffPoint = xIndex > 100 ? 30 : 35;
                    // The column/page switch below happens at COMMIT time, so a line accumulated
                    // in the wide left column can end up DRAWN in the narrow right one. Predict
                    // that switch here, or the measurement guards the wrong column — 31 English
                    // lines overflowed in exactly that way.
                    int effectiveX = yIndex > 13 ? (xIndex > 0 ? 0 : 200) : xIndex;
                    int columnWidth = effectiveX > 100 ? COLUMN_WIDTH_RIGHT : COLUMN_WIDTH_LEFT;
                    boolean newline = word.equals("<NEWLINE>");
                    for (Whitespace indexes : yIndexesToSkip) {
                        int indexPage = indexes.getPage();
                        if (indexPage == page) {
                            int buttonX = indexes.getX();
                            int buttonY = indexes.getY();
                            int width = indexes.getWidth();
                            int height = indexes.getHeight();
                            if (indexes.isDown()) {
                                if (yIndex >= (buttonY) / 12F && yIndex <= (buttonY + height) / 12F) {
                                    if (buttonX < 90 && xIndex < 90 || buttonX >= 90 && xIndex >= 90) {
                                        yIndex += 2;
                                    }
                                }
                            } else {
                                if (yIndex >= (buttonY - height) / 12F && yIndex <= (buttonY + height) / 12F) {
                                    if (buttonX < 90 && xIndex < 90 || buttonX >= 90 && xIndex >= 90) {
                                        yIndex++;
                                    }
                                }
                            }
                        }
                    }
                    boolean last = i == splitBySpaces.size() - 1;
                    actualTextX += word.length() + 1;
                    // Upstream breaks purely on character count, which for a proportional font
                    // lets a line of wide glyphs run past its column and overlap the neighbouring
                    // one (17 lines of the English dictionary do exactly that). Measuring is an
                    // ADDITIONAL break condition, never a weaker one, so any line that already fit
                    // keeps its authored position and only the overflowing ones wrap a word early.
                    boolean tooWide = !lineToPrint.isEmpty() && pageFont.width(lineToPrint + " " + word) > columnWidth;
                    if (lineToPrint.length() + word.length() + 1 >= cutoffPoint || tooWide || newline) {
                        linesFromPrinting++;
                        if (yIndex > 13) {
                            if (xIndex > 0) {
                                page++;
                                xIndex = 0;
                                yIndex = 0;
                            } else {
                                xIndex = 200;
                                yIndex = 0;
                            }
                        }
                        // Upstream folds the final word into the line it was about to break away
                        // from, which puts it back over the column when the break was a width
                        // break (12 English lines end that way). Give it its own line instead.
                        if (last && !tooWide) {
                            lineToPrint = lineToPrint + " " + word;
                        }
                        this.lines.add(new LineData(xIndex, yIndex, lineToPrint, page));
                        yIndex++;
                        actualTextX = 0;
                        if (newline) {
                            yIndex++;
                        }
                        lineToPrint = word.equals("<NEWLINE>") ? "" : word;
                        if (last && tooWide) {
                            linesFromPrinting++;
                            this.lines.add(new LineData(xIndex, yIndex, word, page));
                            yIndex++;
                        }
                    } else {
                        lineToPrint = lineToPrint + " " + word;
                        if (last) {
                            linesFromPrinting++;
                            this.lines.add(new LineData(xIndex, yIndex, lineToPrint, page));
                            yIndex++;
                            actualTextX = 0;
                            if (newline) {
                                yIndex++;
                            }
                        }
                    }
                }
                maxPagesFromPrinting = page;
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        } catch (Exception e) {
            Citadel.LOGGER.warn("Could not load in page .txt from json from page, page: " + res);
        }
    }

    /**
     * Splits a single unbreakable token into pieces no wider than {@code maxWidth}, character by
     * character. Returns the token unchanged (the overwhelmingly common case) when it already
     * fits, so Latin-script pages are untouched.
     */
    private static List<String> hardSplitToWidth(Font font, String word, int maxWidth) {
        if (word.isEmpty() || font.width(word) <= maxWidth) {
            return Collections.singletonList(word);
        }
        List<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if (current.length() > 0 && font.width(current.toString() + c) > maxWidth) {
                out.add(current.toString());
                current.setLength(0);
            }
            current.append(c);
        }
        if (current.length() > 0) {
            out.add(current.toString());
        }
        return out;
    }

    public void setEntityTooltip(String hoverText) {
        this.entityTooltip = hoverText;
    }

    public ResourceLocation getBookButtonsTexture() {
        return BOOK_BUTTONS_TEXTURE;
    }
}

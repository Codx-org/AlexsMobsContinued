package com.github.alexthe666.alexsmobs.client.gui;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class ButtonTransmute extends Button {
    private final Screen parent;

    public ButtonTransmute(Screen parent, int x, int y, OnPress onPress) {
        super(x, y, 117, 19, CommonComponents.EMPTY, onPress, DEFAULT_NARRATION);
        this.parent = parent;
    }


    @Override
    public void renderWidget(GuiGraphics guiGraphics, int x, int y, float partialTick) {
        // All three carry an explicit alpha byte: 1.21.2 deleted Font's "promote an alpha-less colour
        // to opaque" guard, so upstream's bare 0x80FF20 / 0xFF6060 / 0xC7FFD0 drew nothing at all from
        // there up. See GuiBasicBook#getTextColor.
        int color = 8453920 | 0XFF000000;
        int cost = AMConfig.transmutingExperienceCost;
        if(!canBeTransmuted(cost)){
            color = 16736352 | 0XFF000000;
        }else if (this.active && this.isHovered) {
            com.github.alexthe666.alexsmobs.client.render.AMRenderCompat.blit(guiGraphics, GUITransmutationTable.TEXTURE, this.getX(), this.getY(), 0, 201, 117, 19);
            color = 0XC7FFD0 | 0XFF000000;
        }
        guiGraphics.pose().pushPose();
        guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("alexsmobs.container.transmutation_table.cost").append(" " + cost), this.getX() + 21, this.getY() + (this.height - 8) / 2, color, false);
        guiGraphics.pose().popPose();
    }

    public boolean canBeTransmuted(int cost){
        return Minecraft.getInstance().player.experienceLevel >= cost || Minecraft.getInstance().player.getAbilities().instabuild;
    }

    public void playDownSound(SoundManager sounds) {
        if(canBeTransmuted(AMConfig.transmutingExperienceCost)){
            super.playDownSound(sounds);
        }
    }

    public void onPress() {
        if(canBeTransmuted(AMConfig.transmutingExperienceCost)){
            super.onPress();
        }
        this.isHovered = false;
        this.setFocused(false);
    }
}
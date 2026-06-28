package com.brandonitaly.bedrockskins.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class SkinDialogScreen extends Screen {
    protected static final int PADDING_SIDE = 12;
    protected static final int CONTENT_TOP = 30;
    protected static final int SPACING_V = 4;
    protected static final int ELEMENT_HEIGHT = 20;
    protected static final int BUTTON_GAP_H = 4;

    protected final SkinSelectionScreen parent;
    private final int popupWidth;
    private final int popupHeight;

    protected SkinDialogScreen(SkinSelectionScreen parent, Component title, int popupWidth, int popupHeight) {
        super(title);
        this.parent = parent;
        this.popupWidth = popupWidth;
        this.popupHeight = popupHeight;
    }

    protected final int popupX() {
        return (this.width - popupWidth) / 2;
    }

    protected final int popupY() {
        return (this.height - popupHeight) / 2;
    }

    protected final int popupWidth() {
        return popupWidth;
    }

    protected final int popupHeight() {
        return popupHeight;
    }

    protected final int contentLeft() {
        return popupX() + PADDING_SIDE;
    }

    protected final int contentWidth() {
        return popupWidth() - (PADDING_SIDE * 2);
    }

    protected final int contentTopY() {
        return popupY() + CONTENT_TOP;
    }

    protected final int nextY(int currentY) {
        return currentY + ELEMENT_HEIGHT + SPACING_V;
    }

    protected final int nextY(int currentY, int customHeight) {
        return currentY + customHeight + SPACING_V;
    }

    protected final int splitButtonWidth() {
        return (contentWidth() - BUTTON_GAP_H) / 2;
    }

    protected final int splitButtonRightX() {
        return contentLeft() + splitButtonWidth() + BUTTON_GAP_H;
    }

    protected void captureDialogState() {}

    protected void restoreDialogState() {}

    //? if <1.21.11 {
    /*@Override
    public void resize(net.minecraft.client.Minecraft minecraft, int width, int height) {
        captureDialogState();
        super.resize(minecraft, width, height);
        restoreDialogState();
    }*/
    //?} else {
    @Override
    public void resize(int width, int height) {
        captureDialogState();
        super.resize(width, height);
        restoreDialogState();
    }
    //?}

    @Override
    public void extractRenderState(GuiGraphicsExtractor gui, int mouseX, int mouseY, float delta) {
        if (this.parent != null) {
            this.parent.extractRenderState(gui, Integer.MIN_VALUE, Integer.MIN_VALUE, delta);
        }

        gui.fill(0, 0, this.width, this.height, 0xAA000000);

        GuiUtils.drawPanelChrome(gui, popupX(), popupY(), popupWidth, popupHeight, this.title, this.font);

        super.extractRenderState(gui, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }
}
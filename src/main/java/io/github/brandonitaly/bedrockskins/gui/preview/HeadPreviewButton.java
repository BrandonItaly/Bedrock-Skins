package io.github.brandonitaly.bedrockskins.gui.preview;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Keeps the existing head visible while providing normal button input and accessibility. */
public final class HeadPreviewButton extends Button {
    public HeadPreviewButton(Component label, OnPress action) {
        super(0, 0, 24, 24, label, action, DEFAULT_NARRATION);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor headGraphics, int mouseX, int mouseY, float delta) {
        if (!isHoveredOrFocused()) return;
        int x = getX(), y = getY();
        headGraphics.fill(x, y, x + 24, y + 1, 0xFFFFFFFF);
        headGraphics.fill(x, y + 23, x + 24, y + 24, 0xFFFFFFFF);
        headGraphics.fill(x, y, x + 1, y + 24, 0xFFFFFFFF);
        headGraphics.fill(x + 23, y, x + 24, y + 24, 0xFFFFFFFF);
    }
}

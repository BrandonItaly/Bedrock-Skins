package com.brandonitaly.bedrockskins.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import com.brandonitaly.bedrockskins.util.BedrockSkinsSprites;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class SkinPackListWidget extends ObjectSelectionList<SkinPackListWidget.SkinPackEntry> {
    private static final int MIN_VISUAL_ROW_HEIGHT = 20;
    private static final int ROW_LEFT_PADDING = 2;
    private static final int ROW_RIGHT_PADDING = 10;
    private final int rowSlotHeight;

    public SkinPackListWidget(Minecraft client, int width, int height, int y, int itemHeight) {
        super(client, width, height, y, itemHeight);
        this.rowSlotHeight = itemHeight;
    }

    @Override
    public int getRowWidth() { return Math.max(10, getWidth() - ROW_RIGHT_PADDING); }

    @Override
    public int getRowLeft() { return getX() + ROW_LEFT_PADDING; }

    @Override
    protected int scrollBarX() { return getX() + getWidth() - 6; }

    @Override
    protected void extractSelection(GuiGraphicsExtractor context, SkinPackEntry entry, int color) {}

    public void addEntryPublic(SkinPackEntry entry) { super.addEntry(entry); }
    public void clear() { super.clearEntries(); }

    public class SkinPackEntry extends ObjectSelectionList.Entry<SkinPackEntry> {
        private final String packId;
        private final String translationKey;
        private final String fallbackName;
        private final Consumer<String> onSelect;
        private final Consumer<String> onEdit;
        private final Supplier<Boolean> isSelectedFn;
        private final Font textRenderer;

        public SkinPackEntry(String packId, String translationKey, String fallbackName,
                             Consumer<String> onSelect,
                             Consumer<String> onEdit,
                             Supplier<Boolean> isSelectedFn,
                             Font textRenderer) {
            this.packId = packId;
            this.translationKey = translationKey;
            this.fallbackName = fallbackName;
            this.onSelect = onSelect;
            this.onEdit = onEdit;
            this.isSelectedFn = isSelectedFn;
            this.textRenderer = textRenderer;
        }

        private void renderCommon(GuiGraphicsExtractor context, int x, int y, int mouseX, int mouseY, boolean hovered) {
            boolean isSelected = Boolean.TRUE.equals(isSelectedFn.get());
            String translated = GuiSkinUtils.translatedOrFallback(translationKey, fallbackName);

            int rowWidth = Math.max(10, SkinPackListWidget.this.getRowWidth());
            int rowHeight = Math.max(MIN_VISUAL_ROW_HEIGHT, rowSlotHeight - 2);
            int rowY = y + Math.max(0, (rowSlotHeight - rowHeight) / 2);

            GuiUtils.renderPackCard(context, textRenderer, translated, x, rowY, rowWidth, rowHeight, hovered, isSelected, mouseX, mouseY);
        }

        private boolean clickCommon() {
            onSelect.accept(packId);
            GuiUtils.playButtonClickSound();
            return true;
        }

        public void extractContent(GuiGraphicsExtractor context, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            renderCommon(context, getX(), getY(), mouseX, mouseY, hovered);
        }

        public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
            if (click.button() == 1 && onEdit != null) {
                onEdit.accept(packId);
                GuiUtils.playButtonClickSound();
                return true;
            }
            return clickCommon();
        }

        public Component getNarration() { return Component.literal(packId); }
    }
}
package io.github.brandonitaly.bedrockskins.gui.widget;

import io.github.brandonitaly.bedrockskins.gui.preview.GuiUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.util.function.Supplier;

/** Shared selectable sidebar used by cosmetics, emote slots, and cape sources. */
public final class SidebarListWidget extends ObjectSelectionList<SidebarListWidget.SidebarEntry> {
    private final int rowSlotHeight;
    private final Font font;

    public SidebarListWidget(Minecraft client, int width, int height, int y, int itemHeight, Font font) {
        super(client, width, height, y, itemHeight);
        this.rowSlotHeight = itemHeight;
        this.font = font;
    }

    protected void extractListSeparators(GuiGraphicsExtractor graphics) {}
    @Override public int getRowWidth() { return getWidth() - 4; }
    @Override public int getRowLeft() { return getX() + 2; }
    @Override protected int scrollBarX() { return getX() + getWidth() - 6; }
    @Override protected void extractSelection(GuiGraphicsExtractor graphics, SidebarEntry entry, int color) {}

    public void add(Component name, Runnable onSelect, Supplier<Boolean> selected) {
        addEntry(new SidebarEntry(name, onSelect, selected));
    }

    public final class SidebarEntry extends ObjectSelectionList.Entry<SidebarEntry> {
        private final Component name;
        private final Runnable onSelect;
        private final Supplier<Boolean> selected;

        private SidebarEntry(Component name, Runnable onSelect, Supplier<Boolean> selected) {
            this.name = name;
            this.onSelect = onSelect;
            this.selected = selected;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                   boolean hovered, float delta) {
            int rowWidth = Math.max(10, SidebarListWidget.this.getRowWidth());
            int rowHeight = Math.max(20, rowSlotHeight - 2);
            int rowY = getY() + (rowSlotHeight - rowHeight) / 2;
            GuiUtils.renderPackCard(graphics, font, name.getString(), getRowLeft(), rowY,
                rowWidth, rowHeight, hovered, selected.get(), mouseX, mouseY);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
            onSelect.run();
            GuiUtils.playButtonClickSound();
            return true;
        }

        @Override public Component getNarration() { return name; }
    }
}

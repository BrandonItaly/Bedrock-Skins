package io.github.brandonitaly.bedrockskins.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Shared row layout, viewport hit-testing, input dispatch, and cleanup for card grids. */
abstract class CardGridWidget<C> extends ObjectSelectionList<CardGridWidget<C>.CardRow> {
    private final int cellWidth;
    private final int cellHeight;
    private final int cellPadding;

    protected CardGridWidget(Minecraft client, int width, int height, int y, int itemHeight,
                             int cellWidth, int cellHeight, int cellPadding) {
        super(client, width, height, y, itemHeight);
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
        this.cellPadding = cellPadding;
    }

    protected final int cellWidth() { return cellWidth; }
    protected final int cellHeight() { return cellHeight; }

    protected final void addCellsRow(List<C> cells) { addEntry(new CardRow(cells)); }

    protected abstract void renderCell(C cell, GuiGraphicsExtractor graphics, int x, int y,
                                       boolean hovered, int mouseX, int mouseY);
    protected abstract boolean clickCell(C cell, MouseButtonEvent click, boolean doubled);
    protected void cleanupCell(C cell) {}
    protected void extractListSeparators(GuiGraphicsExtractor graphics) {}
    protected void extractListBackground(GuiGraphicsExtractor graphics) {}
    protected void extractSelection(GuiGraphicsExtractor graphics, CardRow entry, int color) {}

    @Override
    public final int getRowWidth() { return width - 10; }

    @Override
    protected final int scrollBarX() { return getX() + width - 6; }

    @Override
    public int getRowTop(int index) { return super.getRowTop(index) - 4; }

    public final void clear() {
        for (CardRow row : children()) row.cleanup();
        clearEntries();
    }

    protected final class CardRow extends ObjectSelectionList.Entry<CardRow> {
        private final List<C> cells;

        private CardRow(List<C> cells) { this.cells = List.copyOf(cells); }
        private void cleanup() { cells.forEach(CardGridWidget.this::cleanupCell); }

        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                   boolean hovered, float delta) {
            boolean mouseInGrid = mouseX >= CardGridWidget.this.getX()
                && mouseX < CardGridWidget.this.getX() + CardGridWidget.this.width
                && mouseY >= CardGridWidget.this.getY()
                && mouseY < CardGridWidget.this.getY() + CardGridWidget.this.height;
            for (int i = 0; i < cells.size(); i++) {
                int x = getX() + i * (cellWidth + cellPadding);
                boolean cellHovered = mouseInGrid && mouseX >= x && mouseX < x + cellWidth
                    && mouseY >= getY() && mouseY < getY() + cellHeight;
                renderCell(cells.get(i), graphics, x, getY(), cellHovered, mouseX, mouseY);
            }
        }

        public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
            int localX = (int) click.x() - getX();
            if (localX < 0) return false;
            int stride = cellWidth + cellPadding;
            int index = localX / stride;
            if (index < 0 || index >= cells.size() || localX % stride >= cellWidth) return false;
            return clickCell(cells.get(index), click, doubled);
        }

        @Override
        public Component getNarration() { return Component.empty(); }
    }
}

package io.github.brandonitaly.bedrockskins.gui.widget;

import io.github.brandonitaly.bedrockskins.gui.preview.GuiUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

/** Shared row layout, viewport hit-testing, input dispatch, and cleanup for card grids. */
abstract class CardGridWidget<C> extends PanelListWidget<CardGridWidget<C>.CardRow> {
    private final int cellWidth;
    private final int cellHeight;
    private static final int CELL_PADDING = GuiUtils.PANEL_CONTENT_PADDING;
    private List<?> displayedValues = List.of();
    private int displayedColumns = -1;
    private int displayedCellWidth = -1;
    private int displayedCellHeight;
    private int layoutWidth = -1;
    private CardGridLayout layout;

    protected CardGridWidget(Minecraft client, int width, int height, int y, int itemHeight,
                             int cellWidth, int cellHeight) {
        super(client, width, height, y, itemHeight);
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
    }

    private CardGridLayout layout() {
        int available = Math.max(1, getRowWidth());
        if (layoutWidth != available) {
            layout = CardGridLayout.fit(available, cellWidth, cellHeight, CELL_PADDING);
            layoutWidth = available;
        }
        return layout;
    }

    protected final int cellWidth() { return layout().cardWidth(); }
    protected final int cellHeight() { return layout().cardHeight(); }

    protected final void addCellsRow(List<C> cells) { addEntry(new CardRow(cells), cellHeight() + CELL_PADDING); }

    protected abstract void renderCell(C cell, GuiGraphicsExtractor graphics, int x, int y,
                                       boolean hovered, int mouseX, int mouseY);
    protected abstract boolean clickCell(C cell, MouseButtonEvent click, boolean doubled);
    protected void cleanupCell(C cell) {}
    protected void extractListSeparators(GuiGraphicsExtractor graphics) {}
    protected void extractListBackground(GuiGraphicsExtractor graphics) {}
    protected void extractSelection(GuiGraphicsExtractor graphics, CardRow entry, int color) {}

    public final void clear() {
        for (CardRow row : children()) row.cleanup();
        clearEntries();
        displayedValues = List.of();
        displayedColumns = -1;
        displayedCellWidth = -1;
    }

    /** Rebuilds rows when contents or dimensions change, retaining the viewed row on resize. */
    public final <T> void refreshRows(List<T> values, Consumer<List<T>> addRow) {
        int columns = layout().columns();
        boolean sameContents = displayedValues.equals(values);
        if (displayedColumns == columns && displayedCellWidth == cellWidth() && sameContents) return;
        double scroll = sameContents && displayedCellHeight > 0
            ? scrollAmount() * (cellHeight() + CELL_PADDING) / (displayedCellHeight + CELL_PADDING) : 0;
        clear();
        for (int i = 0; i < values.size(); i += columns) {
            addRow.accept(values.subList(i, Math.min(i + columns, values.size())));
        }
        displayedValues = List.copyOf(values);
        displayedColumns = columns;
        displayedCellWidth = cellWidth();
        displayedCellHeight = cellHeight();
        setScrollAmount(scroll);
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
                int x = getX() + layout().columnX(i, CELL_PADDING);
                boolean cellHovered = mouseInGrid && mouseX >= x && mouseX < x + cellWidth()
                    && mouseY >= getY() && mouseY < getY() + cellHeight();
                renderCell(cells.get(i), graphics, x, getY(), cellHovered, mouseX, mouseY);
            }
        }

        public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
            int localX = (int) click.x() - getX();
            if (localX < 0 || click.y() < getY() || click.y() >= getY() + cellHeight()) return false;
            for (int i = 0; i < cells.size(); i++) {
                int x = layout().columnX(i, CELL_PADDING);
                if (localX >= x && localX < x + cellWidth()) return clickCell(cells.get(i), click, doubled);
            }
            return false;
        }

        @Override
        public Component getNarration() { return Component.empty(); }
    }
}

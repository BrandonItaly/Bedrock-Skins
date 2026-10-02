package io.github.brandonitaly.bedrockskins.gui.widget;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.input.MouseButtonEvent;

/** Lists whose scrollbar fits in the panel's existing three-pixel edge inset. */
abstract class PanelListWidget<E extends ObjectSelectionList.Entry<E>> extends ObjectSelectionList<E> {
    private boolean draggingScrollbar;

    protected PanelListWidget(Minecraft client, int width, int height, int y, int itemHeight) {
        super(client, width, height, y, itemHeight);
    }

    @Override public final int getRowWidth() { return Math.max(1, getWidth()); }
    @Override public final int getRowLeft() { return getX(); }
    @Override public int getRowTop(int index) { return super.getRowTop(index) - 4; }
    @Override protected final int scrollBarX() { return getRight() + 1; }

    @Override
    protected boolean isOverScrollbar(double x, double y) {
        // Include the gap beside the one-pixel bar without overlapping cards or the border.
        return x >= getRight() && x < scrollBarX() + 1 && y >= getY() && y < getBottom();
    }

    @Override
    public boolean isMouseOver(double x, double y) {
        return super.isMouseOver(x, y)
            || visible && maxScrollAmount() > 0 && isOverScrollbar(x, y);
    }

    @Override
    public boolean updateScrolling(MouseButtonEvent event) {
        draggingScrollbar = super.updateScrolling(event);
        return draggingScrollbar;
    }

    @Override
    public void onRelease(MouseButtonEvent event) {
        super.onRelease(event);
        draggingScrollbar = false;
    }

    @Override
    protected void extractScrollbar(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (maxScrollAmount() <= 0) return;
        int x = scrollBarX();
        int y = scrollBarY();
        boolean hovered = isOverScrollbar(mouseX, mouseY);
        graphics.fill(x, getY(), x + 1, getBottom(), 0xFF393939);
        graphics.fill(x, y, x + 1, y + scrollerHeight(),
            hovered || draggingScrollbar ? 0xFFFFFFFF : 0xFFB8B8B8);
        if (draggingScrollbar) graphics.requestCursor(CursorTypes.RESIZE_NS);
        else if (hovered) graphics.requestCursor(CursorTypes.POINTING_HAND);
    }
}

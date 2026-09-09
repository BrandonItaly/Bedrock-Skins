package io.github.brandonitaly.bedrockskins.gui.widget;

import io.github.brandonitaly.bedrockskins.gui.preview.GuiUtils;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Native widget containing a responsive Persona color palette. */
public final class ColorPaletteWidget extends AbstractWidget {
    private static final int COLUMNS = 6;
    private static final int GAP = 4;
    private final int[] colors;
    private final IntConsumer onSelect;
    private final IntSupplier selectedColor;

    public ColorPaletteWidget(int[] colors, IntConsumer onSelect, IntSupplier selectedColor) {
        super(0, 0, 1, 1, Component.translatable("bedrockskins.persona.color.button"));
        this.colors = colors.clone();
        this.onSelect = onSelect;
        this.selectedColor = selectedColor;
        this.visible = false;
    }

    public void setBounds(int x, int y, int width, int height) {
        setPosition(x, y);
        setWidth(Math.max(1, width));
        setHeight(Math.max(1, height));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (!visible || !active || !isMouseOver(event.x(), event.y())
                || event.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;
        Layout layout = layout();
        int index = indexAt(event.x(), event.y(), layout);
        if (index < 0) return false;
        onSelect.accept(colors[index]);
        GuiUtils.playButtonClickSound();
        return true;
    }

    //~ if >=26.1 'renderWidget' -> 'extractWidgetRenderState' {
    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) { //~}
        Layout layout = layout();
        int selected = selectedColor.getAsInt();
        int hovered = indexAt(mouseX, mouseY, layout);
        for (int i = 0; i < colors.length; i++) {
            int x = layout.startX + (i % COLUMNS) * (layout.size + GAP);
            int y = layout.startY + (i / COLUMNS) * (layout.size + GAP);
            int border = colors[i] == selected ? 0xFF20B52B : i == hovered ? 0xFFFFFFFF : 0xFF555555;
            graphics.fill(x - 1, y - 1, x + layout.size + 1, y + layout.size + 1, border);
            graphics.fill(x, y, x + layout.size, y + layout.size, 0xFF000000 | colors[i]);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }

    private Layout layout() {
        int rows = (colors.length + COLUMNS - 1) / COLUMNS;
        int horizontalSize = Math.max(1, (getWidth() - (COLUMNS - 1) * GAP) / COLUMNS);
        int verticalSize = Math.max(1, (getHeight() - (rows - 1) * GAP) / rows);
        int size = Math.min(horizontalSize, verticalSize);
        int gridWidth = COLUMNS * size + (COLUMNS - 1) * GAP;
        int gridHeight = rows * size + (rows - 1) * GAP;
        return new Layout(size, getX() + (getWidth() - gridWidth) / 2,
            getY() + (getHeight() - gridHeight) / 2);
    }

    private int indexAt(double mouseX, double mouseY, Layout layout) {
        if (mouseX < layout.startX || mouseY < layout.startY) return -1;
        int stride = layout.size + GAP;
        int column = (int) (mouseX - layout.startX) / stride;
        int row = (int) (mouseY - layout.startY) / stride;
        int localX = (int) (mouseX - layout.startX) % stride;
        int localY = (int) (mouseY - layout.startY) % stride;
        if (column < 0 || column >= COLUMNS || row < 0 || localX >= layout.size || localY >= layout.size) return -1;
        int index = row * COLUMNS + column;
        return index < colors.length ? index : -1;
    }

    private record Layout(int size, int startX, int startY) {}
}

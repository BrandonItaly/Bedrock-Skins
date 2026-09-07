package io.github.brandonitaly.bedrockskins.gui.widget;

import io.github.brandonitaly.bedrockskins.gui.preview.*;
import io.github.brandonitaly.bedrockskins.gui.screen.*;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Shared grid implementation for Persona content rendered on preview players. */
abstract class PersonaPreviewGridWidget<T> extends ObjectSelectionList<PersonaPreviewGridWidget<T>.Row> {
    private static final int CELL_WIDTH = 60;
    private static final int CELL_HEIGHT = 85;
    private static final int CELL_PADDING = 5;

    private final Consumer<T> onSelect;
    private final Supplier<T> selected;
    private final Font font;

    PersonaPreviewGridWidget(Minecraft client, int width, int height, int y, int itemHeight,
                             Consumer<T> onSelect, Supplier<T> selected, Font font) {
        super(client, width, height, y, itemHeight);
        this.onSelect = onSelect;
        this.selected = selected;
        this.font = font;
    }

    protected abstract String id(T value);

    protected abstract Component name(T value);

    protected abstract void initializePreview(UUID uuid, T value);

    protected abstract void cleanupPreview(UUID uuid, T value);

    protected void beforeRender(UUID uuid, T value) {
    }

    protected boolean isEquipped(T value) {
        return false;
    }

    protected void extractListSeparators(GuiGraphicsExtractor graphics) {
    }

    protected void extractSelection(GuiGraphicsExtractor graphics, Row entry, int color) {
    }

    @Override
    public int getRowWidth() {
        return width - 10;
    }

    @Override
    protected int scrollBarX() {
        return getX() + width - 6;
    }

    protected final void addValuesRow(List<T> values) {
        addEntry(new Row(values));
    }

    public final void clear() {
        for (Row row : children()) {
            row.cleanup();
        }
        clearEntries();
    }

    public final class Row extends ObjectSelectionList.Entry<Row> {
        private final List<Cell> cells = new ArrayList<>();

        private Row(List<T> values) {
            values.forEach(value -> cells.add(new Cell(value)));
        }

        private void cleanup() {
            cells.forEach(Cell::cleanup);
        }

        public void extractContent(GuiGraphicsExtractor gui, int mouseX, int mouseY, boolean hovered, float delta) {
            for (int i = 0; i < cells.size(); i++) {
                int x = getX() + i * (CELL_WIDTH + CELL_PADDING);
                boolean over = mouseX >= x && mouseX < x + CELL_WIDTH
                    && mouseY >= getY() && mouseY < getY() + CELL_HEIGHT;
                cells.get(i).render(gui, x, getY(), over, mouseX, mouseY);
            }
        }

        public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
            int localX = (int) click.x() - getX();
            if (localX < 0) {
                return false;
            }

            int index = localX / (CELL_WIDTH + CELL_PADDING);
            if (index >= cells.size() || localX % (CELL_WIDTH + CELL_PADDING) > CELL_WIDTH) {
                return false;
            }

            onSelect.accept(cells.get(index).value);
            GuiUtils.playButtonClickSound();
            return true;
        }

        @Override
        public Component getNarration() {
            return Component.empty();
        }

        private final class Cell {
            private final T value;
            private final UUID uuid = UUID.randomUUID();
            private final PreviewPlayer player = new PreviewPlayer(new GameProfile(uuid, ""));

            private Cell(T value) {
                this.value = value;
                GuiSkinUtils.applyCurrentEquippedSkin(Minecraft.getInstance(), player, uuid);
                initializePreview(uuid, value);
            }

            private void cleanup() {
                cleanupPreview(uuid, value);
                GuiSkinUtils.cleanupPreview(uuid);
            }

            private void render(GuiGraphicsExtractor graphics, int x, int y, boolean hovered,
                                int mouseX, int mouseY) {
                beforeRender(uuid, value);
                T current = selected.get();
                boolean valueSelected = current != null && id(current).equals(id(value));
                GuiUtils.renderSkinCard(graphics, font, name(value), x, y, CELL_WIDTH, CELL_HEIGHT,
                    hovered, valueSelected, isEquipped(value), player, 0, mouseX, mouseY);
            }
        }
    }
}

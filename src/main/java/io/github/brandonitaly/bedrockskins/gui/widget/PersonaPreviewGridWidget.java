package io.github.brandonitaly.bedrockskins.gui.widget;

import io.github.brandonitaly.bedrockskins.gui.preview.GuiSkinUtils;
import io.github.brandonitaly.bedrockskins.gui.preview.GuiUtils;
import io.github.brandonitaly.bedrockskins.gui.preview.PreviewPlayer;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Shared preview and lifecycle behavior for Persona cosmetic and emote grids. */
abstract class PersonaPreviewGridWidget<T>
        extends CardGridWidget<PersonaPreviewGridWidget.PreviewCell<T>> {
    private static final int CELL_WIDTH = 60;
    private static final int CELL_HEIGHT = 85;
    private static final int CELL_PADDING = 5;
    private final Consumer<T> onSelect;
    private final Supplier<T> selected;
    private final Font font;

    PersonaPreviewGridWidget(Minecraft client, int width, int height, int y, int itemHeight,
                             Consumer<T> onSelect, Supplier<T> selected, Font font) {
        super(client, width, height, y, itemHeight, CELL_WIDTH, CELL_HEIGHT, CELL_PADDING);
        this.onSelect = onSelect;
        this.selected = selected;
        this.font = font;
    }

    protected abstract String id(T value);
    protected abstract Component name(T value);
    protected abstract void initializePreview(UUID uuid, T value);
    protected abstract void cleanupPreview(UUID uuid, T value);
    protected void beforeRender(UUID uuid, T value) {}
    protected boolean isEquipped(T value) { return false; }

    protected final void addValuesRow(List<T> values) {
        addCellsRow(values.stream().map(PreviewCell::new).toList());
    }

    @Override
    protected final void renderCell(PreviewCell<T> cell, GuiGraphicsExtractor graphics, int x, int y,
                                    boolean hovered, int mouseX, int mouseY) {
        PreviewPlayer preview = cell.player(this);
        beforeRender(cell.uuid, cell.value);
        T current = selected.get();
        boolean valueSelected = current != null && id(current).equals(id(cell.value));
        GuiUtils.renderSkinCard(graphics, font, name(cell.value), x, y, cellWidth(), cellHeight(),
            hovered, valueSelected, isEquipped(cell.value), preview, 0, mouseX, mouseY);
    }

    @Override
    protected final boolean clickCell(PreviewCell<T> cell, MouseButtonEvent click, boolean doubled) {
        onSelect.accept(cell.value);
        GuiUtils.playButtonClickSound();
        return true;
    }

    @Override
    protected final void cleanupCell(PreviewCell<T> cell) {
        if (cell.player == null) return;
        cleanupPreview(cell.uuid, cell.value);
        GuiSkinUtils.cleanupPreview(cell.uuid);
        cell.player = null;
    }

    protected static final class PreviewCell<T> {
        private final T value;
        private final UUID uuid = UUID.randomUUID();
        private PreviewPlayer player;
        private PreviewCell(T value) { this.value = value; }

        private PreviewPlayer player(PersonaPreviewGridWidget<T> grid) {
            if (player == null) {
                player = new PreviewPlayer(new GameProfile(uuid, ""));
                GuiSkinUtils.applyCurrentEquippedSkin(Minecraft.getInstance(), player, uuid);
                grid.initializePreview(uuid, value);
            }
            return player;
        }
    }
}

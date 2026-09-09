package io.github.brandonitaly.bedrockskins.gui.widget;

import io.github.brandonitaly.bedrockskins.gui.preview.GuiSkinUtils;
import io.github.brandonitaly.bedrockskins.gui.preview.GuiUtils;
import io.github.brandonitaly.bedrockskins.gui.preview.PreviewPlayer;
import io.github.brandonitaly.bedrockskins.pack.model.LoadedSkin;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SkinGridWidget extends CardGridWidget<SkinGridWidget.SkinCell> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int CELL_WIDTH = 60;
    private static final int CELL_HEIGHT = 85;
    private static final int CELL_PADDING = 5;
    private final Consumer<LoadedSkin> onSelectSkin;
    private final Consumer<LoadedSkin> onEditSkin;
    private final Supplier<LoadedSkin> getSelectedSkin;
    private final Font font;

    public SkinGridWidget(Minecraft client, int width, int height, int y, int itemHeight,
                          Consumer<LoadedSkin> onSelectSkin, Consumer<LoadedSkin> onEditSkin,
                          Supplier<LoadedSkin> getSelectedSkin, Font font) {
        super(client, width, height, y, itemHeight, CELL_WIDTH, CELL_HEIGHT, CELL_PADDING);
        this.onSelectSkin = onSelectSkin;
        this.onEditSkin = onEditSkin;
        this.getSelectedSkin = getSelectedSkin;
        this.font = font;
    }

    public void addSkinsRow(List<LoadedSkin> skins) {
        addCellsRow(skins.stream().map(SkinCell::new).toList());
    }

    public void addActionRow(Component label, Runnable onClick) {
        addCellsRow(List.of(new SkinCell(label, onClick)));
    }

    @Override
    protected void renderCell(SkinCell cell, GuiGraphicsExtractor graphics, int x, int y,
                              boolean hovered, int mouseX, int mouseY) {
        if (cell.actionCell) {
            GuiUtils.renderActionCard(graphics, font, cell.label, x, y, cellWidth(), cellHeight(),
                hovered, mouseX, mouseY);
            return;
        }
        LoadedSkin selected = getSelectedSkin.get();
        boolean isSelected = selected != null && selected.equals(cell.skin);
        PreviewPlayer preview = cell.player();
        if (preview != null) {
            long now = Util.getMillis();
            long elapsed = Math.max(0, now - cell.lastHoverTime);
            cell.lastHoverTime = now;
            if (hovered) cell.hoverYaw = (cell.hoverYaw + elapsed * 0.03F) % 360.0F;
            else cell.hoverYaw = 0.0F;
        }
        GuiUtils.renderSkinCard(graphics, font, cell.displayName, x, y, cellWidth(), cellHeight(),
            hovered, isSelected, GuiSkinUtils.isSkinCurrentlyEquipped(cell.skin), preview,
            cell.hoverYaw, mouseX, mouseY);
    }

    @Override
    protected boolean clickCell(SkinCell cell, MouseButtonEvent click, boolean doubled) {
        if (click.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
            if (cell.actionCell || cell.skin == null || onEditSkin == null) return false;
            onEditSkin.accept(cell.skin);
            GuiUtils.playButtonClickSound();
            return true;
        }
        cell.activate(onSelectSkin);
        GuiUtils.playButtonClickSound();
        if (doubled && !cell.actionCell) cell.activate(onSelectSkin);
        return true;
    }

    @Override protected void cleanupCell(SkinCell cell) { cell.cleanup(); }

    protected static final class SkinCell {
        private final LoadedSkin skin;
        private final Runnable onClick;
        private final Component label;
        private final Component displayName;
        private final UUID uuid = UUID.randomUUID();
        private final boolean actionCell;
        private PreviewPlayer player;
        private float hoverYaw;
        private long lastHoverTime = Util.getMillis();

        private SkinCell(LoadedSkin skin) {
            this.skin = skin;
            this.onClick = null;
            this.label = null;
            this.actionCell = false;
            this.displayName = Component.literal(GuiSkinUtils.getSkinDisplayNameText(skin));
        }

        private SkinCell(Component label, Runnable onClick) {
            this.skin = null;
            this.onClick = onClick;
            this.label = label;
            this.actionCell = true;
            this.displayName = label != null ? label : Component.empty();
        }

        private void activate(Consumer<LoadedSkin> onSelectSkin) {
            if (onClick != null) onClick.run();
            else if (skin != null) onSelectSkin.accept(skin);
        }

        private PreviewPlayer player() {
            if (player != null || actionCell) return player;
            player = new PreviewPlayer(new GameProfile(uuid, ""));
            try {
                GuiSkinUtils.applyLoadedSkinPreview(player, uuid, skin);
            } catch (Exception exception) {
                LOGGER.warn("Failed to apply skin preview for {}", skin != null ? skin.skinId : null, exception);
            }
            return player;
        }

        private void cleanup() {
            if (actionCell || player == null) return;
            GuiSkinUtils.cleanupPreview(uuid);
            player = null;
        }
    }
}

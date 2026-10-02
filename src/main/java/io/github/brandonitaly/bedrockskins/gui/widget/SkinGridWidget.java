package io.github.brandonitaly.bedrockskins.gui.widget;

import io.github.brandonitaly.bedrockskins.gui.preview.GuiSkinUtils;
import io.github.brandonitaly.bedrockskins.gui.preview.GuiUtils;
import io.github.brandonitaly.bedrockskins.gui.preview.PreviewPlayer;
import io.github.brandonitaly.bedrockskins.gui.preview.MinecraftAccountSkin;
import io.github.brandonitaly.bedrockskins.pack.model.LoadedSkin;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SkinGridWidget extends CardGridWidget<SkinGridWidget.SkinCell> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int CELL_WIDTH = 60;
    private static final int CELL_HEIGHT = 85;
    private final Consumer<LoadedSkin> onSelectSkin;
    private final Supplier<LoadedSkin> getSelectedSkin;
    private final Font font;

    public SkinGridWidget(Minecraft client, int width, int height, int y, int itemHeight,
                          Consumer<LoadedSkin> onSelectSkin,
                          Supplier<LoadedSkin> getSelectedSkin, Font font) {
        super(client, width, height, y, itemHeight, CELL_WIDTH, CELL_HEIGHT);
        this.onSelectSkin = onSelectSkin;
        this.getSelectedSkin = getSelectedSkin;
        this.font = font;
    }

    public void addSkinsRow(List<LoadedSkin> skins) {
        addCellsRow(skins.stream().map(SkinCell::new).toList());
    }

    @Override
    protected void renderCell(SkinCell cell, GuiGraphicsExtractor graphics, int x, int y,
                              boolean hovered, int mouseX, int mouseY) {
        LoadedSkin selected = getSelectedSkin.get();
        boolean isSelected = selected != null && selected.equals(cell.skin);
        PreviewPlayer preview = cell.player();
        if (preview != null) {
            if (MinecraftAccountSkin.is(cell.skin)) {
                GuiSkinUtils.refreshAutoSelectedProfileSkin(minecraft, preview);
            }
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
        onSelectSkin.accept(cell.skin);
        GuiUtils.playButtonClickSound();
        if (doubled) onSelectSkin.accept(cell.skin);
        return true;
    }

    @Override protected void cleanupCell(SkinCell cell) { cell.cleanup(); }

    protected static final class SkinCell {
        private final LoadedSkin skin;
        private final Component displayName;
        private PreviewPlayer player;
        private float hoverYaw;
        private long lastHoverTime = Util.getMillis();

        private SkinCell(LoadedSkin skin) {
            this.skin = skin;
            this.displayName = Component.literal(GuiSkinUtils.getSkinDisplayNameText(skin));
        }

        private PreviewPlayer player() {
            if (player != null) return player;
            player = new PreviewPlayer("");
            try {
                GuiSkinUtils.applyLoadedSkinPreview(player, skin);
            } catch (Exception exception) {
                LOGGER.warn("Failed to apply skin preview for {}", skin != null ? skin.skinId : null, exception);
            }
            return player;
        }

        private void cleanup() {
            if (player == null) return;
            player.close();
            player = null;
        }
    }
}

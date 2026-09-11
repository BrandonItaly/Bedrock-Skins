package io.github.brandonitaly.bedrockskins.gui.widget;

import io.github.brandonitaly.bedrockskins.client.appearance.cape.CapeManager.MinecraftCape;
import io.github.brandonitaly.bedrockskins.gui.preview.GuiSkinUtils;
import io.github.brandonitaly.bedrockskins.gui.preview.GuiUtils;
import io.github.brandonitaly.bedrockskins.gui.preview.PreviewPlayer;
import io.github.brandonitaly.bedrockskins.util.BedrockSkinsSprites;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CapeGridWidget extends CardGridWidget<CapeGridWidget.CapeCell> {
    private static final int CELL_WIDTH = 60;
    private static final int CELL_HEIGHT = 60;
    private static final int CELL_PADDING = 5;
    private final Consumer<MinecraftCape> onSelectCape;
    private final Supplier<MinecraftCape> getSelectedCape;
    private final Font font;

    public CapeGridWidget(Minecraft client, int width, int height, int y, int itemHeight,
                          Consumer<MinecraftCape> onSelectCape,
                          Supplier<MinecraftCape> getSelectedCape, Font font) {
        super(client, width, height, y, itemHeight, CELL_WIDTH, CELL_HEIGHT, CELL_PADDING);
        this.onSelectCape = onSelectCape;
        this.getSelectedCape = getSelectedCape;
        this.font = font;
    }

    public void addCapesRow(List<MinecraftCape> capes) {
        addCellsRow(capes.stream().map(CapeCell::new).toList());
    }

    @Override
    protected void renderCell(CapeCell cell, GuiGraphicsExtractor graphics, int x, int y,
                              boolean hovered, int mouseX, int mouseY) {
        MinecraftCape selected = getSelectedCape.get();
        boolean isSelected = selected != null && selected.id.equals(cell.cape.id);
        boolean isEquipped = cell.cape.state.equals("ACTIVE");
        var cardSprite = isSelected ? BedrockSkinsSprites.CARD_SELECTED
            : hovered ? BedrockSkinsSprites.CARD_HOVER : BedrockSkinsSprites.CARD_IDLE;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, cardSprite, x, y, cellWidth(), cellHeight());
        GuiUtils.renderCapeInRect(graphics, cell.player(), 0.0F,
            x + 1, y + 1, x + cellWidth() - 1, y + cellHeight() - 1);
        if (isEquipped) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, GuiUtils.EQUIPPED_BORDER,
                x, y, cellWidth(), cellHeight());
        }
        if (hovered && cell.name != null) {
            graphics.setTooltipForNextFrame(font, Component.translatable(cell.name), mouseX, mouseY);
        }
    }

    @Override
    protected boolean clickCell(CapeCell cell, MouseButtonEvent click, boolean doubled) {
        onSelectCape.accept(cell.cape);
        GuiUtils.playButtonClickSound();
        return true;
    }

    @Override protected void cleanupCell(CapeCell cell) { cell.cleanup(); }

    protected static final class CapeCell {
        private final MinecraftCape cape;
        private final String name;
        private PreviewPlayer player;
        private CapeCell(MinecraftCape cape) { this.cape = cape; this.name = cape.alias; }

        private PreviewPlayer player() {
            if (player == null) {
                player = new PreviewPlayer("");
                GuiSkinUtils.applyCurrentEquippedSkin(Minecraft.getInstance(), player);
                player.setForcedCape("none".equals(cape.id) ? null : cape.textureIdentifier);
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

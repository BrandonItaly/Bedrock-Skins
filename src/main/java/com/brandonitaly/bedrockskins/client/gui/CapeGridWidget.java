package com.brandonitaly.bedrockskins.client.gui;

import com.brandonitaly.bedrockskins.client.CapeManager.MinecraftCape;
import com.brandonitaly.bedrockskins.client.gui.PreviewPlayer.PreviewPlayerPool;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.RenderPipelines;
import com.brandonitaly.bedrockskins.util.BedrockSkinsSprites;

public class CapeGridWidget extends ObjectSelectionList<CapeGridWidget.CapeRowEntry> {
    public static final int CELL_WIDTH = 60;
    public static final int CELL_HEIGHT = 60;
    public static final int CELL_PADDING = 5;

    private final Consumer<MinecraftCape> onSelectCape;
    private final Supplier<MinecraftCape> getSelectedCape;
    private final Font textRenderer;

    public CapeGridWidget(
            Minecraft client, int width, int height, int y, int itemHeight,
            Consumer<MinecraftCape> onSelectCape, Supplier<MinecraftCape> getSelectedCape, Font textRenderer
    ) {
        super(client, width, height, y, itemHeight);
        this.onSelectCape = onSelectCape;
        this.getSelectedCape = getSelectedCape;
        this.textRenderer = textRenderer;
    }

    protected void extractListSeparators(GuiGraphicsExtractor graphics) {}

    @Override
    public int getRowWidth() { return this.width - 10; }

    @Override
    protected void extractSelection(GuiGraphicsExtractor context, CapeRowEntry entry, int color) {}

    @Override
    protected int scrollBarX() { return this.getX() + this.width - 6; }

    public void addEntryPublic(CapeRowEntry entry) { super.addEntry(entry); }

    public void addCapesRow(List<MinecraftCape> capes) { addEntryPublic(new CapeRowEntry(capes)); }

    public void clear() {
        for (CapeRowEntry row : this.children()) row.cleanup();
        super.clearEntries();
    }

    public class CapeRowEntry extends ObjectSelectionList.Entry<CapeRowEntry> {
        private final List<CapeCell> cells = new ArrayList<>();

        public CapeRowEntry(List<MinecraftCape> capes) {
            for (MinecraftCape cape : capes) cells.add(new CapeCell(cape));
        }

        public void cleanup() {
            for (CapeCell cell : cells) cell.cleanup();
        }

        private void renderCommon(GuiGraphicsExtractor gui, int x, int y, int mouseX, int mouseY) {
            int gridLeft = CapeGridWidget.this.getX(), gridTop = CapeGridWidget.this.getY();
            int gridRight = gridLeft + CapeGridWidget.this.width, gridBottom = gridTop + CapeGridWidget.this.height;
            boolean mouseInGrid = mouseX >= gridLeft && mouseX < gridRight && mouseY >= gridTop && mouseY < gridBottom;
            
            for (int i = 0; i < cells.size(); i++) {
                int cx = x + (i * (CELL_WIDTH + CELL_PADDING));
                boolean isHovered = mouseInGrid && mouseX >= cx && mouseX < cx + CELL_WIDTH && mouseY >= y && mouseY < y + CELL_HEIGHT;
                cells.get(i).extractRenderState(gui, cx, y, CELL_WIDTH, CELL_HEIGHT, isHovered, mouseX, mouseY);
            }
        }

        private boolean clickCommon(int localX, boolean doubled) {
            if (localX < 0) return false;
            int index = localX / (CELL_WIDTH + CELL_PADDING);

            if (index < cells.size()) {
                int cellStart = index * (CELL_WIDTH + CELL_PADDING);
                if (localX >= cellStart && localX <= cellStart + CELL_WIDTH) {
                    CapeCell cell = cells.get(index);
                    cell.activate();
                    GuiUtils.playButtonClickSound();
                    return true;
                }
            }
            return false;
        }

        public void extractContent(GuiGraphicsExtractor gui, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            renderCommon(gui, getX(), getY(), mouseX, mouseY);
        }

        public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent click, boolean doubled) {
            return clickCommon((int) (click.x() - getX()), doubled);
        }

        @Override
        public Component getNarration() { return Component.empty(); }

        public class CapeCell {
            public final MinecraftCape cape;
            private PreviewPlayer player;
            private final UUID uuid = UUID.randomUUID();
            private final String name;

            public CapeCell(MinecraftCape cape) {
                this.cape = cape;
                this.name = cape.alias;
                this.player = PreviewPlayerPool.get(new GameProfile(uuid, ""));
                player.clearForcedProfileSkin();
                player.clearForcedBody();
                if (!cape.id.equals("none")) {
                    player.setForcedCape(cape.textureIdentifier);
                } else {
                    player.setForcedCape(null);
                }
            }

            public void activate() {
                if (cape != null) onSelectCape.accept(cape);
            }

            public void cleanup() {
                GuiSkinUtils.cleanupPreview(uuid);
            }

            public void extractRenderState(GuiGraphicsExtractor context, int x, int y, int w, int h, boolean hovered, int mouseX, int mouseY) {
                MinecraftCape selected = getSelectedCape.get();
                boolean isSelected = selected != null && selected.id.equals(cape.id);
                boolean isEquipped = cape.state.equals("ACTIVE");

                var cardSprite = isSelected ? BedrockSkinsSprites.CARD_SELECTED : (hovered ? BedrockSkinsSprites.CARD_HOVER : BedrockSkinsSprites.CARD_IDLE);
                context.blitSprite(RenderPipelines.GUI_TEXTURED, cardSprite, x, y, w, h);

                if (player != null) {
                    GuiUtils.renderCapeInRect(context, player, 0.0F, x, y, x + w, y + h);
                }

                if (isEquipped) {
                    context.blitSprite(RenderPipelines.GUI_TEXTURED, GuiUtils.EQUIPPED_BORDER, x, y, w, h);
                }

                if (hovered && name != null) {
                    context.setTooltipForNextFrame(textRenderer, Component.translatable(name), mouseX, mouseY);
                }
            }
        }
    }
}

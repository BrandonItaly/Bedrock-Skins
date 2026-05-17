package com.brandonitaly.bedrockskins.client.gui;

import com.brandonitaly.bedrockskins.pack.LoadedSkin;
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
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import com.brandonitaly.bedrockskins.util.BedrockSkinsSprites;

public class SkinGridWidget extends ObjectSelectionList<SkinGridWidget.SkinRowEntry> {

    public static final int CELL_WIDTH = 60;
    public static final int CELL_HEIGHT = 85;
    public static final int CELL_PADDING = 5;

    private final Consumer<LoadedSkin> onSelectSkin;
    private final Consumer<LoadedSkin> onEditSkin;
    private final Supplier<LoadedSkin> getSelectedSkin;
    private final Font textRenderer;

    public SkinGridWidget(
            Minecraft client, int width, int height, int y, int itemHeight,
            Consumer<LoadedSkin> onSelectSkin, Consumer<LoadedSkin> onEditSkin, Supplier<LoadedSkin> getSelectedSkin, Font textRenderer
    ) {
        super(client, width, height, y, itemHeight);
        this.onSelectSkin = onSelectSkin;
        this.onEditSkin = onEditSkin;
        this.getSelectedSkin = getSelectedSkin;
        this.textRenderer = textRenderer;
    }

    @Override
    public int getRowWidth() { return this.width - 10; }

    @Override
    protected int scrollBarX() { return this.getX() + this.width - 6; }

    protected void extractSelection(GuiGraphicsExtractor context, SkinRowEntry entry, int color) {}

    public void addEntryPublic(SkinRowEntry entry) { super.addEntry(entry); }

    public void addSkinsRow(List<LoadedSkin> skins) { addEntryPublic(new SkinRowEntry(skins)); }

    public void addActionRow(Component label, Runnable onClick) { addEntryPublic(new SkinRowEntry(label, onClick)); }

    public void clear() {
        for (SkinRowEntry row : this.children()) row.cleanup();
        super.clearEntries();
    }

    public class SkinRowEntry extends ObjectSelectionList.Entry<SkinRowEntry> {
        private final List<SkinCell> cells = new ArrayList<>();

        public SkinRowEntry(List<LoadedSkin> skins) {
            for (LoadedSkin skin : skins) cells.add(new SkinCell(skin));
        }

        public SkinRowEntry(Component label, Runnable onClick) {
            cells.add(new SkinCell(label, onClick));
        }

        public void cleanup() {
            for (SkinCell cell : cells) cell.cleanup();
        }

        private void renderCommon(GuiGraphicsExtractor gui, int x, int y, int mouseX, int mouseY) {
            int gridLeft = SkinGridWidget.this.getX(), gridTop = SkinGridWidget.this.getY();
            int gridRight = gridLeft + SkinGridWidget.this.width, gridBottom = gridTop + SkinGridWidget.this.height;
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
                    SkinCell cell = cells.get(index);
                    cell.activate();
                    GuiUtils.playButtonClickSound();
                    if (doubled && !cell.isActionCell()) cell.activate();
                    return true;
                }
            }
            return false;
        }

        public void extractContent(GuiGraphicsExtractor gui, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            renderCommon(gui, getX(), getY(), mouseX, mouseY);
        }

        public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent click, boolean doubled) {
            if (click.button() == 1) {
                int localX = (int) (click.x() - getX());
                if (localX < 0) return false;
                int index = localX / (CELL_WIDTH + CELL_PADDING);
                if (index < cells.size()) {
                    SkinCell cell = cells.get(index);
                    if (!cell.isActionCell() && cell.skin != null && onEditSkin != null) {
                        onEditSkin.accept(cell.skin);
                        GuiUtils.playButtonClickSound();
                        return true;
                    }
                }
            }
            return clickCommon((int) (click.x() - getX()), doubled);
        }

        @Override
        public Component getNarration() { return Component.empty(); }

        public class SkinCell {
            public final LoadedSkin skin;
            private final Runnable onClick;
            private final Component label;
            private PreviewPlayer player;
            private final UUID uuid = UUID.randomUUID();
            private final String name;
            private final boolean actionCell;

            private float hoverYaw = 0f; 
            private long lastHoverTime = Util.getMillis();

            private static final Identifier EQUIPPED_BORDER = Identifier.fromNamespaceAndPath("bedrockskins", "container/equipped_item_border");

            public SkinCell(LoadedSkin skin) {
                this.skin = skin;
                this.onClick = null;
                this.label = null;
                this.actionCell = false;
                this.name = GuiSkinUtils.getSkinDisplayNameText(skin);
                this.player = PreviewPlayerPool.get(new GameProfile(uuid, ""));

                try {
                    GuiSkinUtils.applyLoadedSkinPreview(this.player, this.uuid, skin);
                } catch (Exception e) { e.printStackTrace(); }
            }

            public SkinCell(Component label, Runnable onClick) {
                this.skin = null;
                this.onClick = onClick;
                this.label = label;
                this.actionCell = true;
                this.name = label.getString();
            }

            public boolean isActionCell() { return actionCell; }

            public void activate() {
                if (onClick != null) onClick.run();
                else if (skin != null) onSelectSkin.accept(skin);
            }

            public void cleanup() {
                if (!actionCell) GuiSkinUtils.cleanupPreview(uuid);
            }

            public void extractRenderState(GuiGraphicsExtractor context, int x, int y, int w, int h, boolean hovered, int mouseX, int mouseY) {
                boolean isSelected = !actionCell && (getSelectedSkin.get() != null && getSelectedSkin.get().equals(skin));
                var cardSprite = isSelected ? BedrockSkinsSprites.CARD_SELECTED : (hovered ? BedrockSkinsSprites.CARD_HOVER : BedrockSkinsSprites.CARD_IDLE);

                context.blitSprite(RenderPipelines.GUI_TEXTURED, cardSprite, x, y, w, h);

                if (actionCell) {
                    int plusCenterX = x + (w / 2);
                    int plusCenterY = y + (h / 2) - 2;
                    int arm = 13;
                    int thickness = 4;
                    context.fill(plusCenterX - arm, plusCenterY - (thickness / 2), plusCenterX + arm, plusCenterY + (thickness / 2) + 1, 0xFFFFFFFF);
                    context.fill(plusCenterX - (thickness / 2), plusCenterY - arm, plusCenterX + (thickness / 2) + 1, plusCenterY + arm, 0xFFFFFFFF);
                    if (hovered && label != null) context.setTooltipForNextFrame(textRenderer, label, mouseX, mouseY);
                    return;
                }

                if (player != null) {
                    long now = Util.getMillis();
                    long dt = Math.max(0, now - lastHoverTime);
                    lastHoverTime = now;
                    if (hovered) {
                        hoverYaw += dt * 0.03f;
                        if (hoverYaw > 360f) hoverYaw -= 360f;
                    } else {
                        hoverYaw = 0f;
                    }
                    GuiUtils.renderEntityInRect(context, player, hoverYaw, x, y, x + w, y + h, 72);
                }

                if (GuiSkinUtils.isSkinCurrentlyEquipped(skin)) {
                    context.blitSprite(RenderPipelines.GUI_TEXTURED, EQUIPPED_BORDER, x, y, w, h);
                }

                if (hovered) context.setTooltipForNextFrame(textRenderer, Component.literal(name), mouseX, mouseY);
            }
        }
    }
}
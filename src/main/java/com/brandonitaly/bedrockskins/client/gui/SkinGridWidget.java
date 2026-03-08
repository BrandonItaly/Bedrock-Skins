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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources./*? if <1.21.11 {*//*ResourceLocation*//*?} else {*/Identifier/*?}*/;
import net.minecraft./*? if <1.21.11 {*//**//*?} else {*/util./*?}*/Util;

public class SkinGridWidget extends ObjectSelectionList<SkinGridWidget.SkinRowEntry> {

    public static final int CELL_WIDTH = 60;
    public static final int CELL_HEIGHT = 85;
    public static final int CELL_PADDING = 5;

    private final Consumer<LoadedSkin> onSelectSkin;
    private final Supplier<LoadedSkin> getSelectedSkin;
    private final Font textRenderer;
    private final Consumer<String> registerTextureFor;
    private final PreviewSkinSetter setPreviewSkin;

    // Functional interface for the 3-argument lambda (uuid, pack, skin) -> Unit
    @FunctionalInterface
    public interface PreviewSkinSetter {
        void set(String uuid, String pack, String skin);
    }

    public SkinGridWidget(
            Minecraft client,
            int width,
            int height,
            int y,
            int itemHeight,
            Consumer<LoadedSkin> onSelectSkin,
            Supplier<LoadedSkin> getSelectedSkin,
            Font textRenderer,
            Consumer<String> registerTextureFor,
            PreviewSkinSetter setPreviewSkin
    ) {
        super(client, width, height, y, itemHeight);
        this.onSelectSkin = onSelectSkin;
        this.getSelectedSkin = getSelectedSkin;
        this.textRenderer = textRenderer;
        this.registerTextureFor = registerTextureFor;
        this.setPreviewSkin = setPreviewSkin;
    }

    @Override
    public int getRowWidth() {
        return this.width - 10;
    }

    @Override
    protected int scrollBarX() {
        return this.getX() + this.width - 6;
    }

    @Override
    protected void renderSelection(GuiGraphics context, SkinRowEntry entry, int color) {}

    public void addEntryPublic(SkinRowEntry entry) {
        super.addEntry(entry);
    }

    public void addSkinsRow(List<LoadedSkin> skins) {
        addEntryPublic(new SkinRowEntry(skins));
    }

    public void clear() {
        for (SkinRowEntry row : this.children()) {
            row.cleanup();
        }
        super.clearEntries();
    }

    public class SkinRowEntry extends ObjectSelectionList.Entry<SkinRowEntry> {
        private final List<SkinCell> cells = new ArrayList<>();

        public SkinRowEntry(List<LoadedSkin> skins) {
            for (LoadedSkin skin : skins) {
                cells.add(new SkinCell(skin));
            }
        }

        public void cleanup() {
            for (SkinCell cell : cells) {
                cell.cleanup();
            }
        }

        // --- Shared Logic ---

        private void renderCommon(GuiGraphics context, int x, int y, int mouseX, int mouseY) {
            // Calculate grid widget bounds
            int gridLeft = SkinGridWidget.this.getX();
            int gridTop = SkinGridWidget.this.getY();
            int gridRight = gridLeft + SkinGridWidget.this.width;
            int gridBottom = gridTop + SkinGridWidget.this.height;
            boolean mouseInGrid = mouseX >= gridLeft && mouseX < gridRight && mouseY >= gridTop && mouseY < gridBottom;
            for (int i = 0; i < cells.size(); i++) {
                SkinCell cell = cells.get(i);
                int cx = x + (i * (CELL_WIDTH + CELL_PADDING));
                // Only allow hover if mouse is inside grid widget
                boolean isHovered = mouseInGrid && mouseX >= cx && mouseX < cx + CELL_WIDTH && mouseY >= y && mouseY < y + CELL_HEIGHT;
                cell.render(context, cx, y, CELL_WIDTH, CELL_HEIGHT, isHovered, mouseX, mouseY);
            }
        }

        private boolean clickCommon(int localX, boolean doubled) {
            if (localX < 0) return false;
            int index = localX / (CELL_WIDTH + CELL_PADDING);

            if (index < cells.size()) {
                int cellStart = index * (CELL_WIDTH + CELL_PADDING);
                if (localX >= cellStart && localX <= cellStart + CELL_WIDTH) {
                    SkinCell cell = cells.get(index);
                    onSelectSkin.accept(cell.skin);
                    GuiUtils.playButtonClickSound();

                    if (doubled) {
                        onSelectSkin.accept(cell.skin);
                    }
                    return true;
                }
            }
            return false;
        }

        // --- Version Specific Wrappers ---

        public void renderContent(GuiGraphics context, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            renderCommon(context, getX(), getY(), mouseX, mouseY);
        }

        public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent click, boolean doubled) {
            return clickCommon((int) (click.x() - getX()), doubled);
        }

        @Override
        public Component getNarration() {
            return Component.empty();
        }

        public class SkinCell {
            private final LoadedSkin skin;
            private PreviewPlayer player;
            private final UUID uuid;
            private final String name;

            // Hover rotation state
            private float hoverYaw = 0f; // degrees
            private long lastHoverTime = Util.getMillis();

            private static final /*? if <1.21.11 {*//*ResourceLocation*//*?} else {*/Identifier/*?}*/ EQUIPPED_BORDER = /*? if <1.21.11 {*//*ResourceLocation*//*?} else {*/Identifier/*?}*/.fromNamespaceAndPath("bedrockskins", "container/equipped_item_border");

            public SkinCell(LoadedSkin skin) {
                this.skin = skin;
                this.uuid = UUID.randomUUID();
                this.name = GuiSkinUtils.getSkinDisplayNameText(skin);

                var id = skin.getSkinId();
                if (id != null) {
                    try {
                        registerTextureFor.accept(id.toString());
                        setPreviewSkin.set(uuid.toString(), id.getPack(), id.getName());
                        this.player = PreviewPlayerPool.get(new GameProfile(uuid, ""));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            public void cleanup() {
                GuiSkinUtils.cleanupPreview(uuid);
            }

            public void render(GuiGraphics context, int x, int y, int w, int h, boolean hovered, int mouseX, int mouseY) {
                LoadedSkin selected = getSelectedSkin.get();
                boolean isSelected = (selected != null && selected.equals(skin));

                int borderColor;
                if (isSelected) {
                    borderColor = 0xFFFFFF00;
                } else if (hovered) {
                    borderColor = 0xFFFFFFFF;
                } else {
                    borderColor = 0xFF000000;
                }

                int bgColor;
                if (isSelected) {
                    bgColor = 0x80555555;
                } else {
                    bgColor = 0x40000000;
                }

                context.fill(x, y, x + w, y + h, bgColor);
                drawBorder(context, x, y, w, h, borderColor);

                if (player != null) {
                    // Update hover rotation state (increment while hovered, reset instantly when not hovered)
                    //? if >=1.21.11 {
                    long now = Util.getMillis();
                    //?} else {
                    /*long now = Util.getMillis();*/
                    //?}
                    long dt = Math.max(0, now - lastHoverTime);
                    lastHoverTime = now;
                    if (hovered) {
                        // rotate clockwise at ~30 deg/sec
                        hoverYaw += dt * 0.03f;
                        if (hoverYaw > 360f) hoverYaw -= 360f;
                    } else {
                        // reset instantly
                        hoverYaw = 0f;
                    }
                    GuiUtils.renderEntityInRect(context, player, hoverYaw, x, y, x + w, y + h, 72);
                }

                // If this skin is currently equipped by the local player, draw the nine-sliced equipped border on top
                boolean isEquipped = GuiSkinUtils.isSkinCurrentlyEquipped(skin);
                if (isEquipped) {
                    context.blitSprite(RenderPipelines.GUI_TEXTURED, EQUIPPED_BORDER, x, y, w, h);
                }

                if (hovered) {
                    context.setTooltipForNextFrame(textRenderer, Component.literal(name), mouseX, mouseY);
                }
            }

            private void drawBorder(GuiGraphics context, int x, int y, int width, int height, int color) {
                context.fill(x, y, x + width, y + 1, color);
                context.fill(x, y + height - 1, x + width, y + height, color);
                context.fill(x, y + 1, x + 1, y + height - 1, color);
                context.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
            }
        }
    }
}
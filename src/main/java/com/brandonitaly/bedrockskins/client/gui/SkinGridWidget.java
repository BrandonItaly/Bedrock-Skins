package com.brandonitaly.bedrockskins.client.gui;

import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.pack.LoadedSkin;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import com.brandonitaly.bedrockskins.client.gui.PreviewPlayer.PreviewPlayerPool;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SkinGridWidget extends AlwaysSelectedEntryListWidget<SkinGridWidget.SkinRowEntry> {

    public static final int CELL_WIDTH = 60;
    public static final int CELL_HEIGHT = 85;
    public static final int CELL_PADDING = 5;

    private final Consumer<LoadedSkin> onSelectSkin;
    private final Supplier<LoadedSkin> getSelectedSkin;
    private final TextRenderer textRenderer;
    private final Consumer<String> registerTextureFor;
    private final PreviewSkinSetter setPreviewSkin;
    private final Consumer<String> resetPreviewSkin;

    // Functional interface for the 3-argument lambda (uuid, pack, skin) -> Unit
    @FunctionalInterface
    public interface PreviewSkinSetter {
        void set(String uuid, String pack, String skin);
    }

    public SkinGridWidget(
            MinecraftClient client,
            int width,
            int height,
            int y,
            int itemHeight,
            Consumer<LoadedSkin> onSelectSkin,
            Supplier<LoadedSkin> getSelectedSkin,
            TextRenderer textRenderer,
            Consumer<String> registerTextureFor,
            PreviewSkinSetter setPreviewSkin,
            Consumer<String> resetPreviewSkin
    ) {
        super(client, width, height, y, itemHeight);
        this.onSelectSkin = onSelectSkin;
        this.getSelectedSkin = getSelectedSkin;
        this.textRenderer = textRenderer;
        this.registerTextureFor = registerTextureFor;
        this.setPreviewSkin = setPreviewSkin;
        this.resetPreviewSkin = resetPreviewSkin;
    }

    @Override
    public int getRowWidth() {
        return this.width - 10;
    }

    @Override
    protected int getScrollbarX() {
        return this.getX() + this.width - 6;
    }

    //? if <=1.21.8 {
    /*
    @Override
    public void drawSelectionHighlight(DrawContext context, int startX, int startY, int width, int height, int color) {}
    */
    //?} else {
    @Override
    public void drawSelectionHighlight(DrawContext context, SkinRowEntry entry, int color) {}
    //?}

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

    public class SkinRowEntry extends AlwaysSelectedEntryListWidget.Entry<SkinRowEntry> {
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

        private void renderCommon(DrawContext context, int x, int y, int mouseX, int mouseY, float tickDelta) {
            for (int i = 0; i < cells.size(); i++) {
                SkinCell cell = cells.get(i);
                int cx = x + (i * (CELL_WIDTH + CELL_PADDING));
                boolean isHovered = mouseX >= cx && mouseX < cx + CELL_WIDTH && mouseY >= y && mouseY < y + CELL_HEIGHT;
                cell.render(context, cx, y, CELL_WIDTH, CELL_HEIGHT, isHovered, tickDelta, mouseX, mouseY);
            }
        }

        private boolean clickCommon(int localX, boolean doubled) {
            if (localX < 0) return false;
            int index = localX / (CELL_WIDTH + CELL_PADDING);

            if (index >= 0 && index < cells.size()) {
                int cellStart = index * (CELL_WIDTH + CELL_PADDING);
                if (localX >= cellStart && localX <= cellStart + CELL_WIDTH) {
                    SkinCell cell = cells.get(index);
                    onSelectSkin.accept(cell.skin);

                    if (MinecraftClient.getInstance().getSoundManager() != null) {
                        MinecraftClient.getInstance().getSoundManager().play(
                                PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f)
                        );
                    }

                    if (doubled) {
                        onSelectSkin.accept(cell.skin);
                    }
                    return true;
                }
            }
            return false;
        }

        // --- Version Specific Wrappers ---

        //? if <=1.21.8 {
        /*
        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            renderCommon(context, x, y, mouseX, mouseY, tickDelta);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return clickCommon((int)(mouseX - SkinGridWidget.this.getX()), false);
        }
        */
        //?} else {
        public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            renderCommon(context, getX(), getY(), mouseX, mouseY, tickDelta);
        }

        public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
            return clickCommon((int) (click.x() - getX()), doubled);
        }
        //?}

        @Override
        public Text getNarration() {
            return Text.empty();
        }

        public class SkinCell {
            private final LoadedSkin skin;
            private PreviewPlayer player;
            private final UUID uuid = UUID.randomUUID();
            private final String name;

            public SkinCell(LoadedSkin skin) {
                this.skin = skin;
                // Assumes getters exist for safeSkinName and skinDisplayName
                String translated = SkinPackLoader.getTranslation(skin.getSafeSkinName()); 
                this.name = translated != null ? translated : skin.getSkinDisplayName();

                ClientWorld world = MinecraftClient.getInstance().world;
                if (world != null) {
                    // Assumes getter exists for key
                    String[] parts = skin.getKey().split(":", 2); 
                    if (parts.length == 2) {
                        try {
                            registerTextureFor.accept(skin.getKey());
                            setPreviewSkin.set(uuid.toString(), parts[0], parts[1]);
                            this.player = PreviewPlayerPool.get(world, new GameProfile(uuid, ""));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            public void cleanup() {
                try {
                    resetPreviewSkin.accept(uuid.toString());
                } catch (Exception ignored) {
                }
                PreviewPlayerPool.remove(uuid);
            }

            public void render(DrawContext context, int x, int y, int w, int h, boolean hovered, float delta, int mouseX, int mouseY) {
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
                    float pX = x + w / 2.0f;
                    float pY = y + h / 2.0f;
                    // Draw entity with simplified args
                    InventoryScreen.drawEntity(context, x + 2, y + 2, x + w - 2, y + h - 4, 30, 0.0625f, pX, pY, player);
                }

                if (hovered) {
                    context.drawTooltip(textRenderer, Text.literal(name), mouseX, mouseY);
                }
            }

            private void drawBorder(DrawContext context, int x, int y, int width, int height, int color) {
                context.fill(x, y, x + width, y + 1, color);
                context.fill(x, y + height - 1, x + width, y + height, color);
                context.fill(x, y + 1, x + 1, y + height - 1, color);
                context.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
            }
        }
    }
}
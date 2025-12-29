package com.brandonitaly.bedrockskins.client.gui;

import com.brandonitaly.bedrockskins.BedrockSkinsNetworking;
import com.brandonitaly.bedrockskins.client.FavoritesManager;
import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.client.StateManager;
import com.brandonitaly.bedrockskins.pack.AssetSource;
import com.brandonitaly.bedrockskins.pack.LoadedSkin;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

import java.awt.Desktop;
import java.io.File;
import java.nio.file.Files;
import java.util.*;

public class SkinSelectionScreen extends Screen {

    // UI State
    private SkinPackListWidget packList;
    private SkinGridWidget skinGrid;
    private ButtonWidget favoriteButton;
    private String selectedPack;
    private LoadedSkin selectedSkin;

    // Preview State
    private PreviewPlayer dummyPlayer;
    private UUID dummyUuid = UUID.randomUUID();

    // Design Constants
    private static final int PANEL_HEADER_HEIGHT = 20;
    private static final int PANEL_BG_COLOR = 0xD0101010; // Dark, high opacity
    private static final int PANEL_HEADER_COLOR = 0xFF202020; // Slightly lighter
    private static final int BORDER_COLOR = 0xFF404040; // Subtle gray
    private static final int TITLE_COLOR = 0xFFFFFFFF;

    // Layout
    private static final class Rect {
        int x, y, w, h;

        Rect(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        int cx() { return x + w / 2; }
        int cy() { return y + h / 2; }
    }

    private Rect packBounds = new Rect(0, 0, 0, 0);
    private Rect skinBounds = new Rect(0, 0, 0, 0);
    private Rect previewBounds = new Rect(0, 0, 0, 0);

    public SkinSelectionScreen(Screen parent) {
        super(Text.translatable("bedrockskins.gui.title"));
    }

    @Override
    protected void init() {
        super.init();
        FavoritesManager.load();

        setupLayout();
        setupWidgets();

        String currentKey = SkinManager.getLocalSelectedKey();
        Object currentPlayer = client.player;

        if (currentKey != null && !currentKey.isEmpty()) {
            updatePreviewModel(UUID.randomUUID(), currentKey);
        } else if (currentPlayer != null) {
            // Try to get the player's UUID reflectively across mappings
            try {
                java.lang.reflect.Method gm = currentPlayer.getClass().getMethod("getUuid");
                UUID uuid = (UUID) gm.invoke(currentPlayer);
                updatePreviewModel(uuid, null);
            } catch (Exception e) {
                try {
                    java.lang.reflect.Field f = currentPlayer.getClass().getField("uuid");
                    UUID uuid = (UUID) f.get(currentPlayer);
                    updatePreviewModel(uuid, null);
                } catch (Exception ignored) {
                    updatePreviewModel(UUID.randomUUID(), null);
                }
            }
        } else {
            updatePreviewModel(UUID.randomUUID(), null);
        }

        refreshPackList();
    }

    private void setupLayout() {
        int topMargin = 40; // Space for Main Title
        int bottomMargin = 40; // Space for Footer
        int sideMargin = 15;
        int gap = 8;

        int availableHeight = this.height - topMargin - bottomMargin;
        int availableWidth = this.width - (sideMargin * 2);

        // Requirements: 
        // 1. Pack List (Left) and Preview Panel (Right) MUST be same size.
        // 2. Skins Panel (Center) takes remaining space (effectively centered).
        
        // Set side panels to roughly 25% width each
        int sidePanelWidth = (int) (availableWidth * 0.25);
        
        // Enforce limits to ensure panels remain usable
        sidePanelWidth = Math.max(sidePanelWidth, 130); // Min width to fit buttons
        sidePanelWidth = Math.min(sidePanelWidth, 220); // Max width prevents sides from dominating

        // Calculate center width based on remaining space
        int centerPanelWidth = availableWidth - (sidePanelWidth * 2) - (gap * 2);
        
        // Safety fallback: if center gets too small on very narrow screens, shrink sides equally
        if (centerPanelWidth < 100) {
            sidePanelWidth = (availableWidth - (gap * 2) - 100) / 2;
            centerPanelWidth = availableWidth - (sidePanelWidth * 2) - (gap * 2);
        }

        packBounds = new Rect(sideMargin, topMargin, sidePanelWidth, availableHeight);
        skinBounds = new Rect(packBounds.x + packBounds.w + gap, topMargin, centerPanelWidth, availableHeight);
        previewBounds = new Rect(skinBounds.x + skinBounds.w + gap, topMargin, sidePanelWidth, availableHeight);
    }

    private void setupWidgets() {
        MinecraftClient mc = this.client;
        if (mc == null) return;

        // Pack List
        // Offset Y and Height to account for the panel header
        int listY = packBounds.y + PANEL_HEADER_HEIGHT + 2;
        int listH = packBounds.h - PANEL_HEADER_HEIGHT - 4;
        
        packList = new SkinPackListWidget(mc, packBounds.w - 4, listH, listY, 24,
                this::selectPack,
                id -> Objects.equals(selectedPack, id),
                textRenderer);
        setWidgetLeft(packList, packBounds.x + 2, listY);
        addDrawableChild(packList);

        // Skin Grid
        // Offset Y and Height to account for the panel header
        int gridY = skinBounds.y + PANEL_HEADER_HEIGHT + 2;
        int gridH = skinBounds.h - PANEL_HEADER_HEIGHT - 4;

        skinGrid = new SkinGridWidget(mc, skinBounds.w - 4, gridH, gridY, 90,
                this::selectSkin,
                () -> selectedSkin,
                textRenderer,
                this::safeRegisterTexture,
                SkinManager::setPreviewSkin,
                this::safeResetPreview);
        setWidgetLeft(skinGrid, skinBounds.x + 2, gridY);
        addDrawableChild(skinGrid);

        setupButtons();
    }

    private void setupButtons() {
        // Preview Panel Buttons
        int btnW = Math.min(previewBounds.w - 16, 150);
        int btnX = previewBounds.cx() - btnW / 2;
        int startY = previewBounds.y + previewBounds.h - 26;

        addDrawableChild(ButtonWidget.builder(Text.translatable("bedrockskins.button.reset"), b -> resetSkin())
                .dimensions(btnX, startY, btnW, 20).build());

        startY -= 24;
        addDrawableChild(ButtonWidget.builder(Text.translatable("bedrockskins.button.select"), b -> applySkin())
                .dimensions(btnX, startY, btnW, 20).build());

        startY -= 24;
        favoriteButton = ButtonWidget.builder(Text.translatable("bedrockskins.button.favorite"), b -> toggleFavorite())
                .dimensions(btnX, startY, btnW, 20).build();
        favoriteButton.active = false;
        addDrawableChild(favoriteButton);

        // Footer Buttons (Open Packs / Done)
        // Matching exact positioning from original Kotlin implementation
        int footW = 150;
        int footGap = 8;
        int footY = this.height - 28;
        int totalFootW = (footW * 2) + footGap;
        // Calculation: width / 2 - (footW * 2 + footGap) / 2
        int footX = (this.width / 2) - (totalFootW / 2);

        addDrawableChild(ButtonWidget.builder(Text.translatable("bedrockskins.button.open_packs"), b -> openSkinPacksFolder())
                .dimensions(footX, footY, footW, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> close())
                .dimensions(footX + footW + footGap, footY, footW, 20).build());
    }

    /**
     * Set the left position of a widget using multiple fallbacks so it works across mappings.
     */
    private void setWidgetLeft(Object widget, int left, int top) {
        try {
            try {
                java.lang.reflect.Method m = widget.getClass().getMethod("setLeftPos", int.class);
                m.invoke(widget, left);
                try { java.lang.reflect.Method my = widget.getClass().getMethod("setTopPos", int.class); my.invoke(widget, top); } catch (Exception ignored) {}
                return;
            } catch (NoSuchMethodException ignored) {}

            try {
                java.lang.reflect.Method m = widget.getClass().getMethod("setX", int.class);
                m.invoke(widget, left);
                try { java.lang.reflect.Method my = widget.getClass().getMethod("setY", int.class); my.invoke(widget, top); } catch (Exception ignored) {}
                return;
            } catch (NoSuchMethodException ignored) {}

            try {
                java.lang.reflect.Method m = widget.getClass().getMethod("setLeft", int.class);
                m.invoke(widget, left);
                return;
            } catch (NoSuchMethodException ignored) {}

            try {
                java.lang.reflect.Method m = widget.getClass().getMethod("setPosition", int.class, int.class);
                m.invoke(widget, left, top);
                return;
            } catch (NoSuchMethodException ignored) {}

            // Last resort: write to field named "x"
            try {
                java.lang.reflect.Field f = widget.getClass().getDeclaredField("x");
                f.setAccessible(true);
                f.setInt(widget, left);
            } catch (Exception ignored) {}
        } catch (Exception e) {
            // best-effort: ignore
        }
    }

    // --- Logic & State Management ---

    private void updatePreviewModel(UUID uuid, String skinKey) {
        ClientWorld world = client.world;
        if (world == null) return;

        // Cleanup old
        safeResetPreview(dummyUuid.toString());
        dummyUuid = uuid;

        if (skinKey != null) {
            String[] parts = skinKey.split(":", 2);
            if (parts.length == 2) {
                SkinManager.setPreviewSkin(uuid.toString(), parts[0], parts[1]);
                safeRegisterTexture(skinKey);
            }
        }

        String name = client.player != null ? client.player.getName().getString() : "Preview";
        GameProfile profile = new GameProfile(uuid, name);
        // Using the static inner class imported from PreviewPlayer
        dummyPlayer = PreviewPlayer.PreviewPlayerPool.get(world, profile);
    }

    private void selectPack(String packId) {
        selectedPack = packId;
        if (skinGrid != null) {
            skinGrid.clear();
            skinGrid.setScrollY(0.0);
        }

        // Logic to preserve order from skins.json:
        // We iterate the main collection and filter. We do NOT use stream().collect() blindly
        // because it might not respect insertion order depending on Java version/implementation.
        List<LoadedSkin> targetSkins = new ArrayList<>();

        if ("skinpack.Favorites".equals(packId)) {
            // Favorites order is determined by the FavoritesManager list order
            List<String> favKeys = FavoritesManager.getFavoriteKeys();
            for (String key : favKeys) {
                LoadedSkin skin = SkinPackLoader.loadedSkins.get(key);
                if (skin != null) targetSkins.add(skin);
            }
        } else {
            // Normal pack: Iterate the values of the Map.
            // Requirement: SkinPackLoader.loadedSkins MUST be a LinkedHashMap for this to work perfectly.
            Collection<LoadedSkin> allSkins = SkinPackLoader.loadedSkins.values();
            for (LoadedSkin skin : allSkins) {
                if (Objects.equals(skin.getId(), packId)) {
                    targetSkins.add(skin);
                }
            }
        }

        int cols = Math.max(1, (skinBounds.w - 20) / 65);
        for (int i = 0; i < targetSkins.size(); i += cols) {
            List<LoadedSkin> row = targetSkins.subList(i, Math.min(i + cols, targetSkins.size()));
            skinGrid.addSkinsRow(row);
        }
    }

    private void selectSkin(LoadedSkin skin) {
        this.selectedSkin = skin;
        updateFavoriteButton();
        if (skin != null && !skin.getKey().isEmpty()) {
            updatePreviewModel(UUID.randomUUID(), skin.getKey());
        }
    }

    private void applySkin() {
        LoadedSkin skin = this.selectedSkin;
        if (skin == null || skin.getKey().isEmpty()) return;
        try {
            byte[] textureData = loadTextureData(skin);
            String[] parts = skin.getKey().split(":", 2);
            String pack = parts.length == 2 ? parts[0] : "Remote";
            String name = parts.length == 2 ? parts[1] : skin.getKey();

            safeRegisterTexture(skin.getKey());

            if (client.player != null) {
                SkinManager.setSkin(client.player.getUuid().toString(), pack, name);
                if (textureData.length > 0) {
                    ClientPlayNetworking.send(new BedrockSkinsNetworking.SetSkinPayload(skin.getKey(), skin.getGeometryData().toString(), textureData));
                }
                String tName = SkinPackLoader.getTranslation(skin.getSafeSkinName());
                if (tName == null) tName = skin.getSkinDisplayName();
                client.player.sendMessage(Text.translatable("bedrockskins.message.set_skin", tName).formatted(Formatting.GREEN), true);
            } else {
                StateManager.saveState(FavoritesManager.getFavoriteKeys(), skin.getKey());
                updatePreviewModel(dummyUuid, skin.getKey());
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (client.player != null)
                client.player.sendMessage(Text.literal("Error: " + e.getMessage()).formatted(Formatting.RED), false);
        }
    }

    private void resetSkin() {
        selectedSkin = null;
        if (client.player != null) {
            SkinManager.resetSkin(client.player.getUuid().toString());
            ClientPlayNetworking.send(new BedrockSkinsNetworking.SetSkinPayload("RESET", "", new byte[0]));
            client.player.sendMessage(Text.translatable("bedrockskins.message.reset_default").formatted(Formatting.YELLOW), true);
            updatePreviewModel(client.player.getUuid(), null);
        } else {
            StateManager.saveState(FavoritesManager.getFavoriteKeys(), null);
            safeResetPreview(dummyUuid.toString());
        }
    }

    // --- Helpers ---

    private void refreshPackList() {
        if (packList == null) return;
        packList.clear();

        // Group skins by Pack ID to find available packs
        // We use a temporary map, but we will sort the keys afterwards using packOrder
        Set<String> packIds = new HashSet<>();
        for (LoadedSkin s : SkinPackLoader.loadedSkins.values()) {
            packIds.add(s.getId());
        }

        List<String> sortedPacks = new ArrayList<>(packIds);

        // Custom Sort: Favorites first, then defined order, then alphabetical
        sortedPacks.sort((k1, k2) -> {
            int i1 = SkinPackLoader.packOrder.indexOf(k1);
            int i2 = SkinPackLoader.packOrder.indexOf(k2);
            if (i1 != -1 && i2 != -1) return Integer.compare(i1, i2);
            if (i1 != -1) return -1;
            if (i2 != -1) return 1;
            return k1.compareTo(k2);
        });

        // Add Favorites at the very top if it exists
        if (!FavoritesManager.getFavoriteKeys().isEmpty()) {
            sortedPacks.add(0, "skinpack.Favorites");
        }

        for (String pid : sortedPacks) {
            String displayName = pid;
            String internalName = pid;

            if ("skinpack.Favorites".equals(pid)) {
                displayName = "Favorites";
                internalName = "Favorites";
            } else {
                // Find first skin in this pack to get the pack's display name
                // Again, iterating values implies relying on LinkedHashMap for speed,
                // but for finding *one* match, any order is fine.
                for (LoadedSkin s : SkinPackLoader.loadedSkins.values()) {
                    if (Objects.equals(s.getId(), pid)) {
                        displayName = s.getSafePackName();
                        internalName = s.getPackDisplayName();
                        break;
                    }
                }
            }

            packList.addEntryPublic(new SkinPackListWidget.SkinPackEntry(
                    pid,
                    displayName,
                    internalName,
                    this::selectPack,
                    () -> Objects.equals(selectedPack, pid),
                    textRenderer
            ));
        }
    }

    private byte[] loadTextureData(LoadedSkin skin) {
        try {
            AssetSource src = skin.getTexture();
            if (src instanceof AssetSource.Resource) {
                var resOpt = client.getResourceManager().getResource(((AssetSource.Resource) src).getId());
                if (resOpt.isPresent()) return resOpt.get().getInputStream().readAllBytes();
                return new byte[0];
            } else if (src instanceof AssetSource.File) {
                File f = new File(((AssetSource.File) src).getPath());
                return Files.readAllBytes(f.toPath());
            } else {
                return new byte[0];
            }
        } catch (Exception e) {
            return new byte[0];
        }
    }

    private void toggleFavorite() {
        if (selectedSkin != null) {
            if (FavoritesManager.isFavorite(selectedSkin)) FavoritesManager.removeFavorite(selectedSkin);
            else FavoritesManager.addFavorite(selectedSkin);
            updateFavoriteButton();
            refreshPackList();
            if ("skinpack.Favorites".equals(selectedPack)) selectPack("skinpack.Favorites");
        }
    }

    private void updateFavoriteButton() {
        if (favoriteButton != null) favoriteButton.active = selectedSkin != null;
        boolean isFav = selectedSkin != null && FavoritesManager.isFavorite(selectedSkin);
        if (favoriteButton != null)
            favoriteButton.setMessage(Text.translatable(isFav ? "bedrockskins.button.unfavorite" : "bedrockskins.button.favorite"));
    }

    private void openSkinPacksFolder() {
        File dir = new File(client.runDirectory, "skin_packs");
        if (!dir.exists()) dir.mkdirs();
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(dir);
            else Util.getOperatingSystem().open(dir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void safeRegisterTexture(String key) {
        try { SkinPackLoader.registerTextureFor(key); } catch (Exception e) { e.printStackTrace(); }
    }

    private void safeResetPreview(String uuid) {
        try { SkinManager.resetPreviewSkin(uuid); } catch (Exception ignored) {}
    }

    // --- Rendering ---

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 2. Main Title centered at top
        context.drawCenteredTextWithShadow(textRenderer, this.title, this.width / 2, 8, TITLE_COLOR);

        // 3. Draw Stylized Panels
        drawPanel(context, packBounds, Text.translatable("bedrockskins.gui.packs"));
        drawPanel(context, skinBounds, Text.translatable("bedrockskins.gui.skins"));
        drawPanel(context, previewBounds, Text.translatable("bedrockskins.gui.preview"));

        // 4. Draw Widgets and Buttons
        super.render(context, mouseX, mouseY, delta);

        // 5. Draw Entity (Pops over backgrounds)
        renderPreviewEntity(context, mouseX, mouseY);

        // 6. Render Preview Skin Info (Drawn LAST to overlay the preview/entity)
        if (selectedSkin != null) {
            String name = SkinPackLoader.getTranslation(selectedSkin.getSafeSkinName());
            if (name == null) name = selectedSkin.getSkinDisplayName();
            // Draw skin name just below the preview header, overlaying the entity area if needed
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(name), previewBounds.cx(), previewBounds.y + PANEL_HEADER_HEIGHT + 10, 0xFFAAAAAA);
        }
    }

    private void drawPanel(DrawContext context, Rect r, Text title) {
        // Main Background
        context.fill(r.x, r.y, r.x + r.w, r.y + r.h, PANEL_BG_COLOR);

        // Header Strip
        context.fill(r.x, r.y, r.x + r.w, r.y + PANEL_HEADER_HEIGHT, PANEL_HEADER_COLOR);
        
        // Header Separator Line
        context.fill(r.x, r.y + PANEL_HEADER_HEIGHT, r.x + r.w, r.y + PANEL_HEADER_HEIGHT + 1, BORDER_COLOR);

        // Outer Borders
        context.fill(r.x, r.y, r.x + r.w, r.y + 1, BORDER_COLOR); // Top
        context.fill(r.x, r.y + r.h - 1, r.x + r.w, r.y + r.h, BORDER_COLOR); // Bottom
        context.fill(r.x, r.y + 1, r.x + 1, r.y + r.h - 1, BORDER_COLOR); // Left
        context.fill(r.x + r.w - 1, r.y + 1, r.x + r.w, r.y + r.h - 1, BORDER_COLOR); // Right

        // Header Title
        context.drawCenteredTextWithShadow(textRenderer, title, r.cx(), r.y + 6, TITLE_COLOR);
    }

    private void renderPreviewEntity(DrawContext context, int mouseX, int mouseY) {
        // Calculate rendering area within the panel (below the header)
        int x = previewBounds.x;
        int y = previewBounds.y + PANEL_HEADER_HEIGHT;
        int w = previewBounds.w;
        // Adjust height to stop BEFORE the buttons area (bottom ~90px)
        int buttonsHeight = 90;
        int h = previewBounds.h - PANEL_HEADER_HEIGHT - buttonsHeight;

        // Ensure we have a valid drawing area
        int availableHeight = Math.max(h, 50);
        
        // Scale calculation
        int scale = Math.min((int)(availableHeight / 2.5), 80);
        float sensitivity = 0.25f;
        
        float centerX = x + w / 2.0f;
        float centerY = y + availableHeight / 2.0f + 20;
        
        float adjustedMouseX = centerX + (mouseX - centerX) * sensitivity;
        float adjustedMouseY = centerY + (mouseY - centerY) * sensitivity;

        if (dummyPlayer != null) {
            dummyPlayer.age = (int)(net.minecraft.util.Util.getMeasuringTimeMs() / 50L);
            InventoryScreen.drawEntity(
                context, x + 5, y + 20, x + w - 5, y + availableHeight,
                scale, 0.0625f, adjustedMouseX, adjustedMouseY, dummyPlayer
            );
        } else if (client.player == null) {
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable("bedrockskins.preview.unavailable"), x + w / 2, y + availableHeight / 2, 0xFFAAAAAA);
        }
    }

    @Override
    public void close() {
        if (skinGrid != null) skinGrid.clear();
        safeResetPreview(dummyUuid.toString());
        client.setScreen(null);
    }
}
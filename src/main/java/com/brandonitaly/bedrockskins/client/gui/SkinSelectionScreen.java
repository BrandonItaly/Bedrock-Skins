package com.brandonitaly.bedrockskins.client.gui;

import com.brandonitaly.bedrockskins.client.FavoritesManager;
import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.pack.LoadedSkin;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import com.brandonitaly.bedrockskins.pack.SkinId;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
//?} else {
/*import net.minecraft.resources.ResourceLocation;
import net.minecraft.Util;*/
//?}
import net.minecraft.world.entity.player.PlayerModelPart;
import com.brandonitaly.bedrockskins.client.BedrockSkinsConfig;

import java.io.File;
import java.util.*;

public class SkinSelectionScreen extends Screen {
    //? if >=1.21.11 {
    public static final Identifier TAB_HEADER_BACKGROUND = Identifier.withDefaultNamespace("textures/gui/tab_header_background.png");
    //?} else {
    /*public static final ResourceLocation TAB_HEADER_BACKGROUND = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/tab_header_background.png");*/
    //?}

    // --- Widgets & State ---
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private final TabManager tabManager = new TabManager(this::addRenderableWidget, this::removeWidget);
    private TabNavigationBar tabNavigationBar;
    
    private SkinPackListWidget packList;
    private SkinGridWidget skinGrid;
    private SkinPreviewPanel previewPanel;
    private final Screen parent; 

    // tracked active tab: 0=skins, 1=customization
    private int activeTab = 0;
    
    private String selectedPackId;
    private boolean wasMousePressed = false;
    
    private final Map<String, List<LoadedSkin>> skinCache = new HashMap<>();

    // Layout
    private final Rect rPacks = new Rect();
    private final Rect rSkins = new Rect();
    private final Rect rPreview = new Rect();

    // Widgets for the customization tab (created/removed dynamically)
    private final java.util.List<AbstractWidget> customizationWidgets = new ArrayList<>();

    // Footer buttons
    private Button openPacksButton;
    private Button doneButton;

    public SkinSelectionScreen(Screen parent) {
        super(Component.translatable("bedrockskins.gui.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        FavoritesManager.load();
        
        buildSkinCache();
        calculateLayout();
        
        // Initialize tab navigation
        this.tabNavigationBar = TabNavigationBar.builder(this.tabManager, this.width)
            .addTabs(new SkinsTab(), new SkinCustomizationTab())
            .build();
        this.addRenderableWidget(this.tabNavigationBar);
        
        // Initialize footer buttons
        int btnW = 150;
        int btnH = 20;
        int btnY = this.height - 28;
        int leftX = this.width / 2 - 154;
        int rightX = this.width / 2 + 4;

        // Open Skin Packs Folder button
        this.openPacksButton = Button.builder(Component.translatable("bedrockskins.button.open_packs"), b -> openSkinPacksFolder())
                .bounds(leftX, btnY, btnW, btnH).build();
        this.addRenderableWidget(this.openPacksButton);

        // Done button
        this.doneButton = Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(rightX, btnY, btnW, btnH).build();
        this.addRenderableWidget(this.doneButton);
        
        this.tabNavigationBar.selectTab(0, false);
        this.repositionElements();
    }
    
    @Override
    public void repositionElements() {
        if (this.tabNavigationBar != null) {
            this.tabNavigationBar.setWidth(this.width);
            this.tabNavigationBar.arrangeElements();
            int tabAreaTop = this.tabNavigationBar.getRectangle().bottom();
            ScreenRectangle tabArea = new ScreenRectangle(0, tabAreaTop, this.width, this.height - this.layout.getFooterHeight() - tabAreaTop);
            this.tabManager.setTabArea(tabArea);
            this.layout.setHeaderHeight(tabAreaTop);
            this.layout.arrangeElements();
            
            // Recalculate layout when screen is resized
            calculateLayout();
            // Ensure widgets are positioned according to the final layout
            initWidgets();
            // Reposition footer buttons
            if (this.openPacksButton != null && this.doneButton != null) {
                int btnW = 150;
                int btnH = 20;
                int btnY = this.height - 28;
                int leftX = this.width / 2 - 154;
                int rightX = this.width / 2 + 4;
                this.openPacksButton.setX(leftX);
                this.openPacksButton.setY(btnY);
                this.openPacksButton.setWidth(btnW);
                this.openPacksButton.setHeight(btnH);
                this.doneButton.setX(rightX);
                this.doneButton.setY(btnY);
                this.doneButton.setWidth(btnW);
                this.doneButton.setHeight(btnH);
            }
        }
    }
    
    private void buildSkinCache() {
        skinCache.clear();
        for (LoadedSkin skin : SkinPackLoader.loadedSkins.values()) {
            skinCache.computeIfAbsent(skin.getId(), k -> new ArrayList<>()).add(skin);
        }
        
        List<LoadedSkin> favs = new ArrayList<>();
        for (String key : FavoritesManager.getFavoriteKeys()) {
            LoadedSkin s = SkinPackLoader.getLoadedSkin(SkinId.parse(key));
            if (s != null) favs.add(s);
        }
        skinCache.put("skinpack.Favorites", favs);
    }

    private void calculateLayout() {
        // deprecated, kept for compatibility - prefer calculateLayout(tabArea)
        calculateLayout(null);
    }

    private void calculateLayout(net.minecraft.client.gui.navigation.ScreenRectangle tabArea) {
        int hMargin = 10;
        int gap = 6;
        int topY;
        int areaW;
        int areaH;
        if (tabArea != null) {
            // Use accessor methods on ScreenRectangle
            topY = tabArea.top();
            areaW = tabArea.width();
            areaH = tabArea.height();
        } else {
            int tabTop = this.tabNavigationBar != null ? this.tabNavigationBar.getRectangle().bottom() : 32;
            int tabFooter = this.layout.getFooterHeight() > 0 ? this.layout.getFooterHeight() : 32;
            int fullH = this.height - tabTop - tabFooter;
            topY = tabTop;
            areaW = this.width;
            areaH = fullH;
        }

        // Reserve a small padding so panels don't touch the tab bar or footer
        int topPadding = 8;
        int bottomPadding = 8;
        int innerH = Math.max(50, areaH - topPadding - bottomPadding);
        
        int fullW = areaW - (hMargin * 2);
        int sideW = Math.max(130, Math.min(200, (int)(fullW * 0.22)));
        int centerW = fullW - (sideW * 2) - (gap * 2);
        
        if (centerW < 100) {
            sideW = (fullW - 100 - (gap * 2)) / 2;
            centerW = 100;
        }

        int top = topY + topPadding;
        rPacks.set(hMargin, top, sideW, innerH);
        rSkins.set(rPacks.right() + gap, top, centerW, innerH);
        rPreview.set(rSkins.right() + gap, top, sideW, innerH);
    }

    private void initWidgets() {
        if (minecraft == null) return;
        int PANEL_HEADER_HEIGHT = 24;
        int PANEL_PADDING = 4;

        // -- Pack List --
        int plY = rPacks.y + PANEL_HEADER_HEIGHT + PANEL_PADDING;
        int plH = rPacks.h - PANEL_HEADER_HEIGHT - (PANEL_PADDING * 2);

        if (packList == null) {
            packList = new SkinPackListWidget(minecraft, rPacks.w - (PANEL_PADDING * 2), plH, plY, 24,
                    this::selectPack,
                    id -> Objects.equals(selectedPackId, id),
                    font);
            packList.setX(rPacks.x + PANEL_PADDING);
            packList.setY(plY);
            addRenderableWidget(packList);
        } else {
            packList.setX(rPacks.x + PANEL_PADDING);
            packList.setY(plY);
            packList.setWidth(Math.max(10, rPacks.w - (PANEL_PADDING * 2)));
            packList.setHeight(Math.max(10, plH));
        }

        // -- Preview Panel --
        if (previewPanel == null) {
            previewPanel = new SkinPreviewPanel(minecraft, font, this::onFavoritesChanged);
            previewPanel.init(rPreview.x, rPreview.y, rPreview.w, rPreview.h, this::addRenderableWidget);
        } else {
            previewPanel.reposition(rPreview.x, rPreview.y, rPreview.w, rPreview.h);
        }

        // -- Skin Grid --
        int sgY = rSkins.y + PANEL_HEADER_HEIGHT + PANEL_PADDING;
        int sgH = rSkins.h - PANEL_HEADER_HEIGHT - (PANEL_PADDING * 2);

        if (skinGrid == null) {
            skinGrid = new SkinGridWidget(minecraft, rSkins.w - (PANEL_PADDING * 2), sgH, sgY, 90,
                    skin -> previewPanel.setSelectedSkin(skin),
                    () -> previewPanel != null ? previewPanel.getSelectedSkin() : null,
                    font,
                    this::safeRegisterTexture,
                    SkinManager::setPreviewSkin,
                    this::safeResetPreview);
            skinGrid.setX(rSkins.x + PANEL_PADDING);
            skinGrid.setY(sgY);
            addRenderableWidget(skinGrid);
        } else {
            skinGrid.setX(rSkins.x + PANEL_PADDING);
            skinGrid.setY(sgY);
            skinGrid.setWidth(Math.max(10, rSkins.w - (PANEL_PADDING * 2)));
            skinGrid.setHeight(Math.max(10, sgH));
        }

        // Refresh pack list entries and selection
        refreshPackList();
        if (selectedPackId != null) selectPack(selectedPackId);
    }
    
    private void onFavoritesChanged() {
        buildSkinCache();
        refreshPackList();
        if ("skinpack.Favorites".equals(selectedPackId)) {
            selectPack("skinpack.Favorites");
        }
    }

    private void clearCustomizationWidgets() {
        for (AbstractWidget w : customizationWidgets) {
            this.removeWidget(w);
        }
        customizationWidgets.clear();
    }

    private void createCustomizationWidgets(ScreenRectangle tabArea) {
        int PANEL_HEADER_HEIGHT = 24;
        int PANEL_PADDING = 4;
        
        // Define the content area within rSkins
        int contentX = rSkins.x + PANEL_PADDING;
        int contentY = rSkins.y + PANEL_HEADER_HEIGHT + PANEL_PADDING;
        int contentW = rSkins.w - (PANEL_PADDING * 2);
        int contentH = rSkins.h - PANEL_HEADER_HEIGHT - (PANEL_PADDING * 2);
        
        // Button settings
        int btnHeight = 20;
        int gapX = 8;
        int gapY = 6;
        int maxButtonWidth = 150;
        int singleColMaxWidth = 310; // Similar to vanilla options width
        
        // Calculate layout strategy
        // We prefer 2 columns if width allows (e.g., > 320px for two 150px buttons + gap)
        // If not, fall back to 1 column.
        
        int columns;
        int btnWidth;
        
        // Check if we can fit 2 columns of at least 100px
        if (contentW > 210) { 
             columns = 2;
             // Calculate width for 2 columns: (Available - Gap) / 2
             int w = (contentW - gapX) / 2;
             // Cap at maxButtonWidth
             btnWidth = Math.min(maxButtonWidth, w);
        } else {
             columns = 1;
             btnWidth = Math.min(singleColMaxWidth, contentW);
        }
        
        // Calculate starting X to center the grid/list
        int totalGridWidth;
        if (columns == 1) {
            totalGridWidth = btnWidth;
        } else {
            totalGridWidth = (btnWidth * 2) + gapX;
        }
        
        int startX = contentX + (contentW - totalGridWidth) / 2;
        int startY = contentY;

        var options = Minecraft.getInstance().options;
        PlayerModelPart[] parts = PlayerModelPart.values();
        int total = parts.length + 1; // include main hand

        for (int i = 0; i < total; i++) {
            int col, row;
            if (columns == 1) {
                col = 0;
                row = i;
            } else {
                col = i % 2;
                row = i / 2;
            }
            
            int x = startX + col * (btnWidth + gapX);
            int y = startY + row * (btnHeight + gapY);

            // Prevent buttons from overlapping the bottom of the skins panel
            if (y + btnHeight > contentY + contentH) break;

            if (i < parts.length) {
                PlayerModelPart part = parts[i];
                CycleButton<Boolean> btn = CycleButton.onOffBuilder(options.isModelPartEnabled(part))
                        .create(x, y, btnWidth, btnHeight, part.getName(), (button, value) -> options.setModelPart(part, value));
                this.addRenderableWidget(btn);
                customizationWidgets.add(btn);
            } else {
                // main hand button
                var mainHandBtn = options.mainHand().createButton(options);
                mainHandBtn.setX(x);
                mainHandBtn.setY(y);
                mainHandBtn.setWidth(btnWidth);
                mainHandBtn.setHeight(btnHeight);
                this.addRenderableWidget(mainHandBtn);
                customizationWidgets.add(mainHandBtn);
            }
        }
    }

    private Component getSkinsPanelTitle() {
        if (selectedPackId == null) return Component.translatable("bedrockskins.gui.skins");
        List<LoadedSkin> skins = skinCache.get(selectedPackId);
        int count = skins == null ? 0 : skins.size();
        String display;
        if ("skinpack.Favorites".equals(selectedPackId)) {
            display = Component.translatable("bedrockskins.gui.favorites").getString();
        } else if (skins != null && !skins.isEmpty()) {
            String safe = skins.get(0).getSafePackName();
            String t = SkinPackLoader.getTranslation(safe);
            display = t != null ? t : skins.get(0).getPackDisplayName();
        } else {
            display = selectedPackId;
        }
        return Component.literal(display + " (" + count + ")");
    }

    private void refreshPackList() {
        if (packList == null) return;
        packList.clear();

        List<String> sortedPacks = new ArrayList<>(skinCache.keySet());
        sortedPacks.remove("skinpack.Favorites"); 
        
        sortedPacks.sort((k1, k2) -> {
            int i1 = SkinPackLoader.packOrder.indexOf(k1);
            int i2 = SkinPackLoader.packOrder.indexOf(k2);
            if (i1 != -1 && i2 != -1) return Integer.compare(i1, i2);
            if (i1 != -1) return -1;
            if (i2 != -1) return 1;
            return k1.compareToIgnoreCase(k2);
        });

        if (!FavoritesManager.getFavoriteKeys().isEmpty()) {
            sortedPacks.add(0, "skinpack.Favorites");
        }

        for (String pid : sortedPacks) {
            String display = pid;
            String internal = pid;
            
            List<LoadedSkin> skins = skinCache.get(pid);
            if (skins != null && !skins.isEmpty()) {
                LoadedSkin first = skins.get(0);
                display = first.getSafePackName();
                internal = first.getPackDisplayName();
            }
            
            if ("skinpack.Favorites".equals(pid)) {
                String fav = Component.translatable("bedrockskins.gui.favorites").getString();
                display = fav;
                internal = fav;
            }

            packList.addEntryPublic(new SkinPackListWidget.SkinPackEntry(
                    pid, display, internal,
                    this::selectPack,
                    () -> Objects.equals(selectedPackId, pid),
                    font
            ));
        }

        // If no pack is currently selected, open the first pack by default
        if (selectedPackId == null && !sortedPacks.isEmpty()) {
            selectPack(sortedPacks.get(0));
        }
    }

    private void selectPack(String packId) {
        this.selectedPackId = packId;
        if (skinGrid != null) {
            skinGrid.clear();
            skinGrid.setScrollAmount(0.0);
        }

        List<LoadedSkin> skins = skinCache.getOrDefault(packId, Collections.emptyList());
        int itemWidth = 65;
        int totalWidth = rSkins.w - (4 * 2) - 10;
        int cols = Math.max(1, totalWidth / itemWidth);
        
        for (int i = 0; i < skins.size(); i += cols) {
            List<LoadedSkin> row = skins.subList(i, Math.min(i + cols, skins.size()));
            skinGrid.addSkinsRow(row);
        }
    }

    private void openSkinPacksFolder() {
        File dir = new File(minecraft.gameDirectory, "skin_packs");
        if (!dir.exists()) dir.mkdirs();
        //? if >=1.21.11 {
        Util.getPlatform().openFile(dir);
        //?} else {
        /*Util.getPlatform().openFile(dir);*/
        //?}
    }

    private void safeRegisterTexture(String key) { GuiUtils.safeRegisterTexture(key); }
    private void safeResetPreview(String uuid) { GuiUtils.safeResetPreview(uuid); }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float delta) {
        // Only draw the pack & skins panels for the Skins tab
        if (activeTab == 0) {
            GuiUtils.drawPanelChrome(gui, rPacks.x, rPacks.y, rPacks.w, rPacks.h, Component.translatable("bedrockskins.gui.packs"), font);
            GuiUtils.drawPanelChrome(gui, rSkins.x, rSkins.y, rSkins.w, rSkins.h, getSkinsPanelTitle(), font);
        }
        
        // Handle mouse events for preview panel
        boolean mousePressed = org.lwjgl.glfw.GLFW.glfwGetMouseButton(
            org.lwjgl.glfw.GLFW.glfwGetCurrentContext(),
            org.lwjgl.glfw.GLFW.glfwGetCurrentContext() != 0 ? org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT : 0
        ) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
            
        if (previewPanel != null) {
            // Detect mouse click
            if (mousePressed && !wasMousePressed) {
                previewPanel.mouseClicked(mouseX, mouseY, 0);
            }
            // Detect mouse release
            if (!mousePressed && wasMousePressed) {
                previewPanel.mouseReleased(mouseX, mouseY, 0);
            }
            wasMousePressed = mousePressed;
            
            previewPanel.render(gui, mouseX, mouseY);
        }
        
        super.render(gui, mouseX, mouseY, delta);
        
        // Render the full heart sprite AFTER buttons have rendered (only if preview panel is visible)
        if (previewPanel != null) {
            previewPanel.renderSprites(gui);
        }
        
        // Draw footer separator
        gui.blit(RenderPipelines.GUI_TEXTURED, Screen.FOOTER_SEPARATOR, 0, this.height - this.layout.getFooterHeight() - 2, 0.0F, 0.0F, this.width, 2, 32, 2);
    }
    
    @Override
    protected void renderMenuBackground(GuiGraphics graphics) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TAB_HEADER_BACKGROUND, 0, 0, 0.0F, 0.0F, this.width, this.layout.getHeaderHeight(), 16, 16);
        this.renderMenuBackground(graphics, 0, this.layout.getHeaderHeight(), this.width, this.height);
    }

    private static class Rect {
        int x, y, w, h;
        void set(int x, int y, int w, int h) { this.x = x; this.y = y; this.w = w; this.h = h; }
        int right() { return x + w; }
        int bottom() { return y + h; }
        int centerX() { return x + (w / 2); }
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }
    
    // ========== TAB CLASSES ==========
    
    private class SkinsTab extends GridLayoutTab {
        private static final Component TITLE = Component.translatable("bedrockskins.gui.skins");
        
        public SkinsTab() {
            super(TITLE);
        }
        
        @Override
        public void doLayout(ScreenRectangle tabArea) {
            // Activate skins tab
            SkinSelectionScreen.this.activeTab = 0;
            // Layout using the tab area so panels are shown only when this tab is active
            SkinSelectionScreen.this.calculateLayout(tabArea);
            // Ensure customization widgets (if any) are removed when entering Skins tab
            SkinSelectionScreen.this.clearCustomizationWidgets();
            // Initialize or reposition the skins widgets
            SkinSelectionScreen.this.initWidgets();
            // Ensure the skins widgets are visible
            if (SkinSelectionScreen.this.packList != null) SkinSelectionScreen.this.packList.visible = true;
            if (SkinSelectionScreen.this.skinGrid != null) SkinSelectionScreen.this.skinGrid.visible = true;
            if (SkinSelectionScreen.this.previewPanel != null) {
                SkinSelectionScreen.this.previewPanel.reposition(SkinSelectionScreen.this.rPreview.x, SkinSelectionScreen.this.rPreview.y, SkinSelectionScreen.this.rPreview.w, SkinSelectionScreen.this.rPreview.h);
                SkinSelectionScreen.this.previewPanel.setButtonsVisible(true);
            }
        }
    }
    
    private class SkinCustomizationTab extends GridLayoutTab {
        private static final Component TITLE = Component.translatable("options.skinCustomisation.title");
        
        public SkinCustomizationTab() {
            super(TITLE);
        }
        
        @Override
        public void doLayout(ScreenRectangle tabArea) {
            // Activate customization tab
            SkinSelectionScreen.this.activeTab = 1;
            // Layout using the tab area
            SkinSelectionScreen.this.calculateLayout(tabArea);

            // Hide skin pack/skins widgets
            if (SkinSelectionScreen.this.packList != null) SkinSelectionScreen.this.packList.visible = false;
            if (SkinSelectionScreen.this.skinGrid != null) SkinSelectionScreen.this.skinGrid.visible = false;

            // Ensure preview panel remains visible and placed in preview area
            if (SkinSelectionScreen.this.previewPanel != null) {
                SkinSelectionScreen.this.previewPanel.reposition(SkinSelectionScreen.this.rPreview.x, SkinSelectionScreen.this.rPreview.y, SkinSelectionScreen.this.rPreview.w, SkinSelectionScreen.this.rPreview.h);
                SkinSelectionScreen.this.previewPanel.setButtonsVisible(true);
            }

            // Remove any existing customization widgets then build new ones positioned within the skins panel
            SkinSelectionScreen.this.clearCustomizationWidgets();
            SkinSelectionScreen.this.createCustomizationWidgets(tabArea);
        }
    }
}


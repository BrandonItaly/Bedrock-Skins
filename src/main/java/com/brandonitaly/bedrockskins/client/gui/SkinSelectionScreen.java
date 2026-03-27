package com.brandonitaly.bedrockskins.client.gui;

import com.brandonitaly.bedrockskins.client.ContentManager;
import com.brandonitaly.bedrockskins.client.FavoritesManager;
import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.client.BedrockSkinsConfig;
import com.brandonitaly.bedrockskins.pack.LoadedSkin;
import com.brandonitaly.bedrockskins.pack.SkinId;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import com.brandonitaly.bedrockskins.util.BedrockSkinsSprites;
import com.brandonitaly.bedrockskins.util.PackSortUtil;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.PlayerModelPart;

import java.io.File;
import java.util.*;

public class SkinSelectionScreen extends Screen {
    private static final String STORE_CATEGORY_ID = "bedrock_skins";
    private static final String STORE_FOLDER = "skin_packs";
    private static final String FAVORITES_PACK_ID = "skinpack.Favorites";

    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private final TabManager tabManager = new TabManager(this::addRenderableWidget, this::removeWidget);
    private TabNavigationBar tabNavigationBar;
    
    private SkinPackListWidget packList;
    private SkinGridWidget skinGrid;
    private SkinPreviewPanel previewPanel;
    private final Screen parent; 

    private int activeTab = 0; // 0=skins, 1=customization, 2=store
    private String selectedPackId;
    private final Map<String, List<LoadedSkin>> skinCache = new HashMap<>();

    private final Rect rPacks = new Rect(), rSkins = new Rect(), rPreview = new Rect();
    private final List<AbstractWidget> customizationWidgets = new ArrayList<>();
    private Button openPacksButton, doneButton;

    // --- Store Tab Elements
    private ContentPackList downloadList;
    private Button downloadButton;
    private boolean isDownloading = false;
    private boolean needsReload = false; // Flag to track if packs were modified

    public SkinSelectionScreen(Screen parent) {
        super(Component.translatable("bedrockskins.gui.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        FavoritesManager.load();
        buildSkinCache();
        openToCurrentSkin();
        calculateLayout(null);
        
        tabNavigationBar = TabNavigationBar.builder(tabManager, width)
            .addTabs(new SkinsTab(), new SkinCustomizationTab(), new DownloadTab()).build();
        addRenderableWidget(tabNavigationBar);
        
        updateFooterButtons();
        tabNavigationBar.selectTab(activeTab, false);
        setDownloadTabActive(ContentManager.getCategory(STORE_CATEGORY_ID).isPresent());
        repositionElements();
    }

    private void openToCurrentSkin() {
        SkinId selectedSkin = SkinManager.getLocalSelectedKey();
        if (selectedSkin == null) return;

        LoadedSkin loadedSkin = SkinPackLoader.getLoadedSkin(selectedSkin);
        String packId = loadedSkin != null ? loadedSkin.packId : ("skinpack." + selectedSkin.pack());
        if (packId != null && skinCache.containsKey(packId)) {
            selectedPackId = packId;
        }
    }
    
    @Override
    public void repositionElements() {
        if (tabNavigationBar != null) {
            //~ if >26.0 '.setWidth' -> '.updateWidth' {
            tabNavigationBar.setWidth(width);//~}
            tabNavigationBar.arrangeElements();
            int top = tabNavigationBar.getRectangle().bottom();
            tabManager.setTabArea(new ScreenRectangle(0, top, width, height - layout.getFooterHeight() - top));
            layout.setHeaderHeight(top);
            layout.arrangeElements();
            updateFooterButtons();
        }
    }
    
    private void applyTabState(ScreenRectangle tabArea, int tabIndex) {
        if (this.activeTab == 2 && tabIndex != 2) {
            triggerReloadIfNeeded();
        }
        
        activeTab = tabIndex;
        calculateLayout(tabArea);
        clearCustomizationWidgets();
        initWidgets();
        
        boolean isSkins = activeTab == 0;
        if (packList != null) packList.visible = isSkins;
        if (skinGrid != null) skinGrid.visible = isSkins;
        if (previewPanel != null) {
            previewPanel.reposition(rPreview.x, rPreview.y, rPreview.w, rPreview.h);
            previewPanel.setButtonsVisible(true);
        }

        boolean isDownload = activeTab == 2;
        if (downloadList != null) downloadList.visible = isDownload;
        if (downloadButton != null) downloadButton.visible = isDownload;
        
        if (activeTab == 1) createCustomizationWidgets();
    }

    private void setDownloadTabActive(boolean active) {
        if (tabNavigationBar == null) return;
        tabNavigationBar.setTabActiveState(2, active);
        if (!active && activeTab == 2) {
            tabNavigationBar.selectTab(0, false);
        }
    }

    private void updateFooterButtons() {
        int btnW = 150, btnH = 20, btnY = height - 28;
        
        if (openPacksButton == null) {
            openPacksButton = Button.builder(Component.translatable("bedrockskins.button.open_packs"), b -> openSkinPacksFolder()).build();
            addRenderableWidget(openPacksButton);
        }
        openPacksButton.setX(width / 2 - 154); openPacksButton.setY(btnY);
        openPacksButton.setWidth(btnW); openPacksButton.setHeight(btnH);
        openPacksButton.setTooltip(Tooltip.create(Component.translatable("pack.folderInfo")));

        if (doneButton == null) {
            doneButton = Button.builder(CommonComponents.GUI_DONE, b -> onClose()).build();
            addRenderableWidget(doneButton);
        }
        doneButton.setX(width / 2 + 4); doneButton.setY(btnY);
        doneButton.setWidth(btnW); doneButton.setHeight(btnH);
    }

    private void buildSkinCache() {
        skinCache.clear();
        for (LoadedSkin skin : SkinPackLoader.loadedSkins.values()) {
            skinCache.computeIfAbsent(skin.packId, k -> new ArrayList<>()).add(skin);
        }

        injectAutoSelectedIntoStandardPack();
        
        List<LoadedSkin> favs = new ArrayList<>();
        for (String key : FavoritesManager.getFavoriteKeys()) {
            SkinId id = SkinId.parse(key);
            LoadedSkin s = SkinPackLoader.getLoadedSkin(id);
            if (s == null && GuiSkinUtils.isAutoSelectedSkinId(id)) {
                s = resolveAutoSelectedSkinForFavorites();
            }
            if (s != null) favs.add(s);
        }
        skinCache.put(FAVORITES_PACK_ID, favs);
    }

    private void injectAutoSelectedIntoStandardPack() {
        List<LoadedSkin> standardSkins = skinCache.get(GuiSkinUtils.STANDARD_PACK_ID);
        if (standardSkins == null || standardSkins.isEmpty()) return;
        skinCache.put(GuiSkinUtils.STANDARD_PACK_ID, GuiSkinUtils.withAutoSelectedStandardFirst(standardSkins));
    }

    private LoadedSkin resolveAutoSelectedSkinForFavorites() {
        List<LoadedSkin> standardSkins = skinCache.get(GuiSkinUtils.STANDARD_PACK_ID);
        return GuiSkinUtils.resolveAutoSelectedFromStandard(standardSkins);
    }

    private void calculateLayout(ScreenRectangle tabArea) {
        int topY = tabArea != null ? tabArea.top() : (tabNavigationBar != null ? tabNavigationBar.getRectangle().bottom() : 32);
        int areaH = tabArea != null ? tabArea.height() : (height - topY - Math.max(layout.getFooterHeight(), 32));

        int innerH = Math.max(50, areaH - 16); 
        int fullW = width - 20; 
        int sideW = Math.max(130, Math.min(200, (int)(fullW * 0.22)));
        int centerW = fullW - (sideW * 2) - 12; 
        
        if (centerW < 100) {
            sideW = (fullW - 112) / 2;
            centerW = 100;
        }

        int top = topY + 8;
        rPacks.set(10, top, sideW, innerH);
        rSkins.set(rPacks.right() + 6, top, centerW, innerH);
        rPreview.set(rSkins.right() + 6, top, sideW, innerH);
    }

    private void initWidgets() {
        int pHead = 24, pPad = 4;

        int plY = rPacks.y + pHead + pPad, plH = rPacks.h - pHead - (pPad * 2);
        if (packList == null) {
            packList = new SkinPackListWidget(minecraft, rPacks.w - pPad * 2, plH, plY, 28);
            addRenderableWidget(packList);
        }
        packList.setX(rPacks.x + pPad); packList.setY(plY);
        packList.setWidth(Math.max(10, rPacks.w - pPad * 2)); packList.setHeight(Math.max(10, plH));

        if (previewPanel == null) {
            previewPanel = new SkinPreviewPanel(minecraft, font, this::onFavoritesChanged);
            previewPanel.init(rPreview.x, rPreview.y, rPreview.w, rPreview.h, this::addRenderableWidget);
        } else {
            previewPanel.reposition(rPreview.x, rPreview.y, rPreview.w, rPreview.h);
        }

        int sgY = rSkins.y + pHead + pPad, sgH = rSkins.h - pHead - (pPad * 2);
        if (skinGrid == null) {
            skinGrid = new SkinGridWidget(minecraft, rSkins.w - pPad * 2, sgH, sgY, 90,
                    skin -> previewPanel.setSelectedSkin(skin), () -> previewPanel != null ? previewPanel.getSelectedSkin() : null, font,
                    GuiUtils::safeRegisterTexture, SkinManager::setPreviewSkin);
            addRenderableWidget(skinGrid);
        }
        skinGrid.setX(rSkins.x + pPad); skinGrid.setY(sgY);
        skinGrid.setWidth(Math.max(10, rSkins.w - pPad * 2)); skinGrid.setHeight(Math.max(10, sgH));

        refreshPackList();
        if (selectedPackId != null) selectPack(selectedPackId);
    }
    
    private void onFavoritesChanged() {
        buildSkinCache();
        refreshPackList();
        if ("skinpack.Favorites".equals(selectedPackId)) selectPack("skinpack.Favorites");
    }

    private void clearCustomizationWidgets() {
        for (AbstractWidget w : customizationWidgets) removeWidget(w);
        customizationWidgets.clear();
    }

    private void createCustomizationWidgets() {
        int contentX = rSkins.x + 4, contentY = rSkins.y + 28, contentW = rSkins.w - 8, contentH = rSkins.h - 32;
        int btnH = 20, gapX = 8, gapY = 6;
        
        int cols = contentW > 210 ? 2 : 1;
        int btnW = Math.min(cols == 2 ? (contentW - gapX) / 2 : contentW, 150);
        int totalW = cols == 2 ? (btnW * 2) + gapX : btnW;
        int startX = contentX + (contentW - totalW) / 2;

        var options = Minecraft.getInstance().options;
        PlayerModelPart[] parts = PlayerModelPart.values();

        for (int i = 0; i <= parts.length; i++) {
            int col = cols == 1 ? 0 : i % 2;
            int row = cols == 1 ? i : i / 2;
            int x = startX + col * (btnW + gapX);
            int y = contentY + row * (btnH + gapY);

            if (y + btnH > contentY + contentH) break;

            AbstractWidget btn;
            if (i < parts.length) {
                PlayerModelPart part = parts[i];
                btn = CycleButton.onOffBuilder(options.isModelPartEnabled(part))
                        .create(x, y, btnW, btnH, part.getName(), (b, val) -> {
                            options.setModelPart(part, val);
                            options.save();
                        });
            } else {
                btn = options.mainHand().createButton(options);
                btn.setX(x); btn.setY(y); btn.setWidth(btnW); btn.setHeight(btnH);
            }
            addRenderableWidget(btn);
            customizationWidgets.add(btn);
        }
    }

    private Component getSkinsPanelTitle() {
        if (selectedPackId == null) return Component.translatable("bedrockskins.gui.skins");
        List<LoadedSkin> skins = skinCache.get(selectedPackId);
        int count = skins == null ? 0 : skins.size();

        LoadedSkin firstSkin = (skins != null && !skins.isEmpty()) ? skins.getFirst() : null;
        String display = GuiSkinUtils.getPackDisplayName(selectedPackId, firstSkin);
        return Component.literal(display + " (" + count + ")");
    }

    private void refreshPackList() {
        if (packList == null) return;
        packList.clear();

        List<String> sortedPacks = new ArrayList<>(skinCache.keySet());
        sortedPacks.remove("skinpack.Favorites");
        sortedPacks.remove("skinpack.Remote");

        sortedPacks.sort(buildPackComparator());

        if (!FavoritesManager.getFavoriteKeys().isEmpty()) sortedPacks.addFirst("skinpack.Favorites");

        for (String pid : sortedPacks) {
            List<LoadedSkin> skins = skinCache.get(pid);
            LoadedSkin firstSkin = (skins != null && !skins.isEmpty()) ? skins.getFirst() : null;
            String translationKey = GuiSkinUtils.getPackTranslationKey(pid, firstSkin);
            String fallbackName = GuiSkinUtils.getPackFallbackName(pid, firstSkin);

                packList.addEntryPublic(packList.new SkinPackEntry(
                    pid, translationKey, fallbackName, this::selectPack, () -> Objects.equals(selectedPackId, pid), font
            ));
        }

        if (selectedPackId == null && !sortedPacks.isEmpty()) selectPack(sortedPacks.getFirst());
    }

    private Comparator<String> buildPackComparator() {
        return PackSortUtil.buildPackComparator(BedrockSkinsConfig.getPackSortOrder(), this::resolveSortDisplayName);
    }

    private String resolveSortDisplayName(String packId) {
        List<LoadedSkin> skins = skinCache.get(packId);
        LoadedSkin firstSkin = (skins != null && !skins.isEmpty()) ? skins.getFirst() : null;
        return GuiSkinUtils.getPackDisplayName(packId, firstSkin);
    }

    private void selectPack(String packId) {
        this.selectedPackId = packId;
        if (skinGrid != null) {
            skinGrid.clear();
            skinGrid.setScrollAmount(0.0);
        }

        List<LoadedSkin> skins = skinCache.getOrDefault(packId, Collections.emptyList());
        int cols = Math.max(1, (rSkins.w - 18) / 65);
        
        for (int i = 0; i < skins.size(); i += cols) {
            skinGrid.addSkinsRow(skins.subList(i, Math.min(i + cols, skins.size())));
        }
    }

    private void openSkinPacksFolder() {
        File dir = new File(minecraft.gameDirectory, STORE_FOLDER);
        if (!dir.exists()) dir.mkdirs();
        Util.getPlatform().openFile(dir);
    }

    // --- Pack Management Logic ---

    private void downloadPack(ContentManager.Pack pack) {
        if (isDownloading) return;
        isDownloading = true;
        downloadButton.active = false;
        downloadButton.setMessage(Component.translatable("bedrockskins.status.downloading"));

        ContentManager.downloadPack(pack, STORE_FOLDER, () -> minecraft.execute(() -> {
            isDownloading = false;
            downloadButton.setMessage(Component.translatable("bedrockskins.button.delete"));
            downloadButton.active = true;
            needsReload = true;
        }));
    }

    private void deletePack(ContentManager.Pack pack) {
        if (isDownloading) return;

        com.brandonitaly.bedrockskins.util.ExternalAssetUtil.deletePack(pack.id(), STORE_FOLDER);

        minecraft.execute(() -> {
            needsReload = true; 
            
            if (downloadList != null) {
                downloadList.setSelected(downloadList.getSelected());
            }
        });
    }

    private void triggerReloadIfNeeded() {
        if (needsReload) {
            SkinPackLoader.loadPacks();
            buildSkinCache();
            refreshPackList();
            minecraft.reloadResourcePacks();
            needsReload = false;
        }
    }

    // --- Render and Input ---

    public void render(GuiGraphics gui, int mouseX, int mouseY, float delta) {
        if (activeTab == 0) {
            GuiUtils.drawPanelChrome(gui, rPacks.x, rPacks.y, rPacks.w, rPacks.h, Component.translatable("bedrockskins.gui.packs"), font);
            GuiUtils.drawPanelChrome(gui, rSkins.x, rSkins.y, rSkins.w, rSkins.h, getSkinsPanelTitle(), font);
        }
            
        if (previewPanel != null && activeTab != 2) previewPanel.renderPreview(gui, mouseX);
        super.render(gui, mouseX, mouseY, delta);
        if (previewPanel != null && activeTab != 2) previewPanel.renderSprites(gui);
        
        gui.blit(RenderPipelines.GUI_TEXTURED, Screen.FOOTER_SEPARATOR, 0, height - layout.getFooterHeight() - 2, 0.0F, 0.0F, width, 2, 32, 2);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        return (!handled && previewPanel != null && activeTab != 2 && previewPanel.mouseClicked(event.x(), event.y(), event.button())) 
            || super.mouseClicked(event, handled);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return (previewPanel != null && activeTab != 2 && previewPanel.mouseReleased(event.button()))
            || super.mouseReleased(event);
    }
    
    protected void renderMenuBackground(GuiGraphics graphics) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, BedrockSkinsSprites.TAB_HEADER_BACKGROUND, 0, 0, 0.0F, 0.0F, width, layout.getHeaderHeight(), 16, 16);
        super.renderMenuBackground(graphics);
    }

    private static class Rect {
        int x, y, w, h;
        void set(int x, int y, int w, int h) { this.x = x; this.y = y; this.w = w; this.h = h; }
        int right() { return x + w; }
    }

    @Override
    public void onClose() {
        triggerReloadIfNeeded(); 
        if (skinGrid != null) skinGrid.clear();
        if (previewPanel != null) previewPanel.cleanup();
        minecraft.setScreen(parent);
    }
    
    // --- Tabs ---

    private class SkinsTab extends GridLayoutTab {
        public SkinsTab() { super(Component.translatable("bedrockskins.gui.skins")); }
        @Override public void doLayout(ScreenRectangle tabArea) { applyTabState(tabArea, 0); }
    }
    
    private class SkinCustomizationTab extends GridLayoutTab {
        public SkinCustomizationTab() { super(Component.translatable("options.skinCustomisation.title")); }
        @Override public void doLayout(ScreenRectangle tabArea) { applyTabState(tabArea, 1); }
    }

    private class DownloadTab extends GridLayoutTab {
        public DownloadTab() {
            super(Component.translatable("bedrockskins.gui.download"));
        }

        @Override
        public void doLayout(ScreenRectangle tabArea) {
            activeTab = 2;
            calculateLayout(tabArea);
            clearCustomizationWidgets();

            if (downloadList == null) {
                downloadList = new ContentPackList(minecraft, tabArea.width(), tabArea.height() - 40, tabArea.top(), 36);
                addRenderableWidget(downloadList);

                var category = ContentManager.getCategory(STORE_CATEGORY_ID);
                if (category.isPresent()) {
                    ContentManager.fetchIndex(category.get().indexUrl()).thenAccept(packs -> minecraft.execute(() -> {
                        if (downloadList == null) return;
                        for (ContentManager.Pack pack : packs) downloadList.addPack(new ContentPackEntry(pack));
                        setDownloadTabActive(!packs.isEmpty());
                    })).exceptionally(e -> {
                        minecraft.execute(() -> setDownloadTabActive(false));
                        return null;
                    });
                } else {
                    setDownloadTabActive(false);
                }
            }
            
            downloadList.setX(tabArea.left());
            downloadList.setY(tabArea.top());
            downloadList.setWidth(tabArea.width());
            downloadList.setHeight(tabArea.height() - 40);
            downloadList.visible = true;
            
            if (downloadButton == null) {
                downloadButton = Button.builder(Component.translatable("bedrockskins.button.download"), btn -> {
                    ContentPackEntry selected = downloadList.getSelected();
                    if (selected != null) {
                        if (ContentManager.isPackInstalled(selected.pack, STORE_FOLDER)) {
                            deletePack(selected.pack);
                        } else {
                            downloadPack(selected.pack);
                        }
                    }
                }).bounds(0, 0, 150, 20).build();
                addRenderableWidget(downloadButton);
            }
            
            downloadButton.setX(tabArea.left() + (tabArea.width() - 150) / 2);
            downloadButton.setY(tabArea.bottom() - 30);
            downloadButton.visible = true;
            downloadList.setSelected(downloadList.getSelected());
            
            if (packList != null) packList.visible = false;
            if (skinGrid != null) skinGrid.visible = false;
            if (previewPanel != null) previewPanel.setButtonsVisible(false);
        }
    }

    // --- Inner Classes for Store List ---

    class ContentPackList extends ObjectSelectionList<ContentPackEntry> {
        public ContentPackList(Minecraft mc, int w, int h, int y, int itemH) {
            super(mc, w, h, y, itemH);
        }

        public void addPack(ContentPackEntry entry) {
            super.addEntry(entry);
        }

        @Override
        public int getRowWidth() { return 300; }

        @Override
        public void setSelected(ContentPackEntry entry) {
            super.setSelected(entry);
            if (downloadButton != null) {
                if (entry != null) {
                    downloadButton.active = true;
                    if (ContentManager.isPackInstalled(entry.pack, STORE_FOLDER)) {
                        downloadButton.setMessage(Component.translatable("bedrockskins.button.delete"));
                    } else {
                        downloadButton.setMessage(Component.translatable("bedrockskins.button.download"));
                    }
                } else {
                    downloadButton.active = false;
                    downloadButton.setMessage(Component.translatable("bedrockskins.button.download"));
                }
            }
        }
    }

    class ContentPackEntry extends ObjectSelectionList.Entry<ContentPackEntry> {
        final ContentManager.Pack pack;

        public ContentPackEntry(ContentManager.Pack pack) {
            this.pack = pack;
        }

        public void renderContent(GuiGraphics gui, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            int index = downloadList.children().indexOf(this);
            if (index == -1) return;

            int rowLeft = downloadList.getRowLeft();
            int y = downloadList.getRowTop(index);
            int width = downloadList.getRowWidth();
            int maxTextWidth = width - 10;

            String name = pack.name();
            
            if (ContentManager.isPackInstalled(pack, STORE_FOLDER)) {
                name += " (Installed)";
            }

            if (font.width(name) > maxTextWidth) {
                name = font.plainSubstrByWidth(name, maxTextWidth - font.width("...")) + "...";
            }

            String desc = pack.description() != null ? pack.description().replace("\n", " ").replace("\r", "") : "";
            if (font.width(desc) > maxTextWidth) {
                desc = font.plainSubstrByWidth(desc, maxTextWidth - font.width("...")) + "...";
            }

            gui.drawString(font, name, rowLeft + 5, y + 5, 0xFFFFFFFF, false);
            gui.drawString(font, desc, rowLeft + 5, y + 17, 0xFF808080, false);
        }

        @Override
        public Component getNarration() { return Component.literal(pack.name()); }
    }
}
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
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
//? if >=26.2-pre-3
// import net.minecraft.client.gui.components.tabs.MenuTabBar;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

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

    private int activeTab = 0; // 0=skins, 1=store
    private String selectedPackId;
    private final Map<String, List<LoadedSkin>> skinCache = new HashMap<>();

    private final Rect rPacks = new Rect(), rSkins = new Rect(), rPreview = new Rect();
    private Button openPacksButton, doneButton;
    private SpriteIconButton createPackButton;

    // --- Store Tab Elements
    private ContentPackList downloadList;
    private Button downloadButton;
    private boolean isDownloading = false;
    private boolean needsReload = false;

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
        
        //~ if >=26.2-pre-3 'TabNavigationBar.' -> 'MenuTabBar.' {
        tabNavigationBar = TabNavigationBar.builder(tabManager, width)
            .addTabs(new SkinsTab(), new DownloadTab()).build();//~}
        
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
        if (packId != null && skinCache.containsKey(packId)) selectedPackId = packId;
    }
    
    @Override
    public void repositionElements() {
        if (tabNavigationBar != null) {
            //? if >=26.2-pre-3 {
            /*tabNavigationBar.arrangeElements(width);*/
            //?} else if >26.1 {
            tabNavigationBar.updateWidth(width);
            tabNavigationBar.arrangeElements();
            //?} else {
            /*tabNavigationBar.setWidth(width);
            tabNavigationBar.arrangeElements();*/
            //?}
            int top = tabNavigationBar.getRectangle().bottom();
            tabManager.setTabArea(new ScreenRectangle(0, top, width, height - layout.getFooterHeight() - top));
            layout.setHeaderHeight(top);
            layout.arrangeElements();
            updateFooterButtons();
        }
    }
    
    private void applyTabState(ScreenRectangle tabArea, int tabIndex) {
        if (this.activeTab == 1 && tabIndex != 1) triggerReloadIfNeeded();
        if (this.activeTab != tabIndex) selectedPackId = null;
        
        activeTab = tabIndex;
        calculateLayout(tabArea);
        initWidgets(tabArea);
        
        boolean isSkins = activeTab == 0;
        if (packList != null) packList.visible = isSkins;
        if (skinGrid != null) skinGrid.visible = isSkins;
        if (createPackButton != null) createPackButton.visible = isSkins;
        if (previewPanel != null) {
            previewPanel.reposition(rPreview.x, rPreview.y, rPreview.w, rPreview.h);
            previewPanel.setButtonsVisible(activeTab == 0);
        }

        boolean isDownload = activeTab == 1;
        if (downloadList != null) downloadList.visible = isDownload;
        if (downloadButton != null) downloadButton.visible = isDownload;
        
        updateFooterButtons();
    }

    private void setDownloadTabActive(boolean active) {
        if (tabNavigationBar == null) return;
        tabNavigationBar.setTabActiveState(1, active);
        if (!active && activeTab == 1) tabNavigationBar.selectTab(0, false);
        
        this.removeWidget(tabNavigationBar);
        if (active) {
            this.addRenderableWidget(tabNavigationBar);
        }
    }

    private void updateFooterButtons() {
        int btnW = 150, btnH = 20, btnY = height - 28;
        boolean isDownload = (activeTab == 1);
        
        if (openPacksButton == null) {
            openPacksButton = Button.builder(Component.translatable("bedrockskins.button.open_packs"), b -> openSkinPacksFolder()).build();
            addRenderableWidget(openPacksButton);
        }
        openPacksButton.setTooltip(Tooltip.create(Component.translatable("pack.folderInfo")));

        if (doneButton == null) {
            doneButton = Button.builder(CommonComponents.GUI_DONE, b -> onClose()).build();
            addRenderableWidget(doneButton);
        }

        if (downloadButton == null) {
            downloadButton = Button.builder(Component.translatable("bedrockskins.button.download"), btn -> {
                ContentPackEntry selected = downloadList.getSelected();
                if (selected != null) {
                    if (ContentManager.isPackInstalled(selected.pack, STORE_FOLDER)) deletePack(selected.pack);
                    else downloadPack(selected.pack);
                }
            }).build();
            addRenderableWidget(downloadButton);
            if (downloadList != null) {
                downloadList.setSelected(downloadList.getSelected());
            }
        }

        if (isDownload) {
            openPacksButton.visible = false;
            
            downloadButton.setX(width / 2 - 154); downloadButton.setY(btnY);
            downloadButton.setWidth(btnW); downloadButton.setHeight(btnH);
            downloadButton.visible = true;

            doneButton.setX(width / 2 + 4); doneButton.setY(btnY);
            doneButton.setWidth(btnW); doneButton.setHeight(btnH);
        } else {
            openPacksButton.setX(width / 2 - 154); openPacksButton.setY(btnY);
            openPacksButton.setWidth(btnW); openPacksButton.setHeight(btnH);
            openPacksButton.visible = true;

            downloadButton.visible = false;

            doneButton.setX(width / 2 + 4); doneButton.setY(btnY);
            doneButton.setWidth(btnW); doneButton.setHeight(btnH);
        }
    }

    private void buildSkinCache() {
        skinCache.clear();
        for (String packId : SkinPackLoader.packTypesByPackId.keySet()) {
            skinCache.put(packId, new ArrayList<>());
        }
        synchronized (SkinPackLoader.loadedSkins) {
            for (LoadedSkin skin : SkinPackLoader.loadedSkins.values()) {
                skinCache.computeIfAbsent(skin.packId, k -> new ArrayList<>()).add(skin);
            }
        }
        
        List<LoadedSkin> favs = FavoritesManager.getFavoriteKeys().stream()
                .map(SkinId::parse)
                .map(SkinPackLoader::getLoadedSkin)
                .filter(Objects::nonNull)
                .toList();
        skinCache.put(FAVORITES_PACK_ID, favs);
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

    private void initWidgets(ScreenRectangle tabArea) {
        int pHead = 24, pPad = 4;

        // Skins Widgets
        int plY = rPacks.y + pHead + pPad, plH = rPacks.h - pHead - (pPad * 2);
        if (packList == null) {
            packList = new SkinPackListWidget(minecraft, rPacks.w - pPad * 2, plH, plY, 28);
            addRenderableWidget(packList);
        }
        packList.setX(rPacks.x + pPad); packList.setY(plY);
        packList.setWidth(Math.max(10, rPacks.w - pPad * 2)); packList.setHeight(Math.max(10, plH));

        if (createPackButton == null) {
            createPackButton = SpriteIconButton.builder(Component.empty(), b -> {
                minecraft.setScreen(new CreateSkinPackScreen(this));
            }, true).size(20, 20).sprite(BedrockSkinsSprites.ADDON_ICON, 16, 16).build();
            createPackButton.setTooltip(Tooltip.create(Component.translatable("bedrockskins.button.create_skin_pack")));
            addRenderableWidget(createPackButton);
        }
        createPackButton.setX(rPacks.x + rPacks.w - 22);
        createPackButton.setY(rPacks.y + 2);

        if (previewPanel == null) {
            previewPanel = new SkinPreviewPanel(minecraft, font, this::onFavoritesChanged);
            previewPanel.init(rPreview.x, rPreview.y, rPreview.w, rPreview.h, this, this::addRenderableWidget);
        }

        int sgY = rSkins.y + pHead + pPad, sgH = rSkins.h - pHead - (pPad * 2);
        if (skinGrid == null) {
            skinGrid = new SkinGridWidget(minecraft, rSkins.w - pPad * 2, sgH, sgY, 90,
                    skin -> previewPanel.setSelectedSkin(skin), 
                    this::editSkin,
                    () -> previewPanel != null ? previewPanel.getSelectedSkin() : null, font);
            addRenderableWidget(skinGrid);
        }
        skinGrid.setX(rSkins.x + pPad); skinGrid.setY(sgY);
        skinGrid.setWidth(Math.max(10, rSkins.w - pPad * 2)); skinGrid.setHeight(Math.max(10, sgH));

        // Store Widgets
        if (tabArea != null && downloadList == null) {
            downloadList = new ContentPackList(minecraft, tabArea.width(), tabArea.height() - 3, tabArea.top(), 36);
            addRenderableWidget(downloadList);

            ContentManager.getCategory(STORE_CATEGORY_ID).ifPresent(category -> 
                ContentManager.fetchIndex(category.indexUrl()).thenAccept(packs -> minecraft.execute(() -> {
                    if (downloadList == null) return;
                    for (ContentManager.Pack pack : packs) downloadList.addPack(new ContentPackEntry(pack));
                    setDownloadTabActive(!packs.isEmpty());
                })).exceptionally(e -> {
                    minecraft.execute(() -> setDownloadTabActive(false));
                    return null;
                })
            );
        }

        if (downloadList != null && tabArea != null) {
            downloadList.setX(tabArea.left());
            downloadList.setY(tabArea.top());
            downloadList.setWidth(tabArea.width());
            downloadList.setHeight(tabArea.height() - 2);
        }

        refreshPackList();
    }
    
    private void onFavoritesChanged() {
        buildSkinCache();
        refreshPackList();
        if (FAVORITES_PACK_ID.equals(selectedPackId)) selectPack(FAVORITES_PACK_ID);
    }


    private Component getSkinsPanelTitle() {
        if (selectedPackId == null) return Component.translatable("bedrockskins.gui.skins");
        List<LoadedSkin> skins = skinCache.get(selectedPackId);
        int count = skins == null ? 0 : skins.size();

        LoadedSkin firstSkin = (skins != null && !skins.isEmpty()) ? skins.getFirst() : null;
        return Component.literal(GuiSkinUtils.getPackDisplayName(selectedPackId, firstSkin) + " (" + count + ")");
    }

    private void refreshPackList() {
        if (packList == null) return;
        packList.clear();

        Set<String> packIds = new LinkedHashSet<>(SkinPackLoader.packTypesByPackId.keySet());
        packIds.addAll(skinCache.keySet());

        List<String> sortedPacks = packIds.stream()
                .filter(pid -> !FAVORITES_PACK_ID.equals(pid) && !"skinpack.Remote".equals(pid))
                .sorted(PackSortUtil.buildPackComparator(BedrockSkinsConfig.getPackSortOrder(), pid -> {
                    List<LoadedSkin> s = skinCache.get(pid);
                    return GuiSkinUtils.getPackDisplayName(pid, (s != null && !s.isEmpty()) ? s.getFirst() : null);
                }))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        if (!FavoritesManager.getFavoriteKeys().isEmpty()) sortedPacks.addFirst(FAVORITES_PACK_ID);

        for (String pid : sortedPacks) {
            List<LoadedSkin> skins = skinCache.get(pid);
            LoadedSkin firstSkin = (skins != null && !skins.isEmpty()) ? skins.getFirst() : null;
            packList.addEntryPublic(packList.new SkinPackEntry(
                pid, GuiSkinUtils.getPackTranslationKey(pid, firstSkin), GuiSkinUtils.getPackFallbackName(pid, firstSkin),
                this::selectPack, this::editPack, () -> Objects.equals(selectedPackId, pid), font
            ));
        }

        if (selectedPackId == null && !sortedPacks.isEmpty()) selectPack(sortedPacks.getFirst());
        else if (selectedPackId != null) selectPack(selectedPackId); // Force re-render of grid
    }

    private void selectPack(String packId) {
        this.selectedPackId = packId;
        if (skinGrid != null) {
            skinGrid.clear();
            skinGrid.setScrollAmount(0.0);
            
            List<LoadedSkin> skins = skinCache.getOrDefault(packId, List.of());
            int cols = Math.max(1, (rSkins.w - 18) / 65);
            for (int i = 0; i < skins.size(); i += cols) {
                skinGrid.addSkinsRow(skins.subList(i, Math.min(i + cols, skins.size())));
            }
        }
    }

    private void openSkinPacksFolder() {
        File dir = new File(minecraft.gameDirectory, STORE_FOLDER);
        if (!dir.exists()) dir.mkdirs();
        Util.getPlatform().openFile(dir);
    }

    private boolean isExternalPack(String packId) {
        if (packId == null || packId.equals(FAVORITES_PACK_ID)) return false;
        
        List<LoadedSkin> skins = skinCache.get(packId);
        if (skins != null && !skins.isEmpty()) {
            return !(skins.getFirst().texture instanceof com.brandonitaly.bedrockskins.pack.AssetSource.Resource);
        }
        
        return true;
    }

    private void editPack(String packId) {
        if (isExternalPack(packId)) {
            minecraft.setScreen(new EditSkinPackScreen(this, packId));
        }
    }

    private void editSkin(LoadedSkin skin) {
        if (isExternalPack(skin.packId)) {
            minecraft.setScreen(new EditSkinScreen(this, skin.packId, skin));
        }
    }

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
            if (downloadList != null) downloadList.setSelected(downloadList.getSelected());
        });
    }

    public void markNeedsReload() {
        this.needsReload = true;
    }

    public void triggerReloadIfNeeded() {
        if (needsReload) {
            SkinPackLoader.loadPacks();
            buildSkinCache();
            refreshPackList();
            minecraft.reloadResourcePacks();
            needsReload = false;
        }
    }

    public void extractRenderState(GuiGraphicsExtractor gui, int mouseX, int mouseY, float delta) {
        if (activeTab == 0) {
            GuiUtils.drawPanelChrome(gui, rPacks.x, rPacks.y, rPacks.w, rPacks.h, Component.translatable("bedrockskins.gui.packs"), font);
            GuiUtils.drawPanelChrome(gui, rSkins.x, rSkins.y, rSkins.w, rSkins.h, getSkinsPanelTitle(), font);
        }
            
        if (previewPanel != null && activeTab != 1) previewPanel.renderPreview(gui, mouseX);
        super.extractRenderState(gui, mouseX, mouseY, delta);
        if (previewPanel != null && activeTab != 1) previewPanel.renderSprites(gui);
        
        gui.blit(RenderPipelines.GUI_TEXTURED, Screen.FOOTER_SEPARATOR, 0, height - layout.getFooterHeight() - 2, 0.0F, 0.0F, width, 2, 32, 2);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        return (!handled && previewPanel != null && activeTab != 1 && previewPanel.mouseClicked(event.x(), event.y(), event.button())) 
            || super.mouseClicked(event, handled);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return (previewPanel != null && activeTab != 1 && previewPanel.mouseReleased(event.button()))
            || super.mouseReleased(event);
    }
    
    protected void renderMenuBackground(GuiGraphicsExtractor graphics) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, BedrockSkinsSprites.TAB_HEADER_BACKGROUND, 0, 0, 0.0F, 0.0F, width, layout.getHeaderHeight(), 16, 16);
        super.extractMenuBackground(graphics);
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

    private class DownloadTab extends GridLayoutTab {
        public DownloadTab() { super(Component.translatable("bedrockskins.gui.download")); }
        @Override public void doLayout(ScreenRectangle tabArea) { applyTabState(tabArea, 1); }
    }

    // --- Inner Classes for Store List ---

    class ContentPackList extends ObjectSelectionList<ContentPackEntry> {
        public ContentPackList(Minecraft mc, int w, int h, int y, int itemH) { super(mc, w, h, y, itemH); }
        public void addPack(ContentPackEntry entry) { super.addEntry(entry); }
        @Override public int getRowWidth() { return 300; }

        protected void extractListSeparators(GuiGraphicsExtractor graphics) {}

        @Override
        public void setSelected(ContentPackEntry entry) {
            super.setSelected(entry);
            if (downloadButton != null) {
                if (entry != null) {
                    downloadButton.active = true;
                    downloadButton.setMessage(Component.translatable(ContentManager.isPackInstalled(entry.pack, STORE_FOLDER) 
                        ? "bedrockskins.button.delete" : "bedrockskins.button.download"));
                } else {
                    downloadButton.active = false;
                    downloadButton.setMessage(Component.translatable("bedrockskins.button.download"));
                }
            }
        }
    }

    class ContentPackEntry extends ObjectSelectionList.Entry<ContentPackEntry> {
        final ContentManager.Pack pack;

        public ContentPackEntry(ContentManager.Pack pack) { this.pack = pack; }

        public void extractContent(GuiGraphicsExtractor gui, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            int index = downloadList.children().indexOf(this);
            if (index == -1) return;

            int rowLeft = downloadList.getRowLeft(), y = downloadList.getRowTop(index);
            
            // Draw Icon
            net.minecraft.resources.Identifier iconId = com.brandonitaly.bedrockskins.client.StoreIconManager.getIcon(pack.id(), pack.imageUrl());
            if (iconId != null) {
                gui.blit(RenderPipelines.GUI_TEXTURED, iconId, rowLeft + 2, y + 2, 0.0F, 0.0F, 32, 32, 32, 32);
            } else {
                gui.blitSprite(RenderPipelines.GUI_TEXTURED, com.brandonitaly.bedrockskins.util.BedrockSkinsSprites.MY_CHARACTERS_ICON, rowLeft + 2, y + 2, 32, 32);
            }

            int textOffset = 37;
            int maxTextWidth = downloadList.getRowWidth() - textOffset - 10;

            String name = pack.name() + (ContentManager.isPackInstalled(pack, STORE_FOLDER) ? " (Installed)" : "");
            if (font.width(name) > maxTextWidth) name = font.plainSubstrByWidth(name, maxTextWidth - font.width("...")) + "...";

            String desc = pack.description() != null ? pack.description().replace("\n", " ").replace("\r", "") : "";
            if (font.width(desc) > maxTextWidth) desc = font.plainSubstrByWidth(desc, maxTextWidth - font.width("...")) + "...";

            gui.text(font, name, rowLeft + textOffset, y + 5, 0xFFFFFFFF, false);
            gui.text(font, desc, rowLeft + textOffset, y + 17, 0xFF808080, false);
        }

        @Override
        public Component getNarration() { return Component.literal(pack.name()); }
    }
}
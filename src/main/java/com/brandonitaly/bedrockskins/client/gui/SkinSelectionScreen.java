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
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
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

import java.io.File;
import java.util.*;

public class SkinSelectionScreen extends Screen {
    //? if >=1.21.11 {
    public static final Identifier TAB_HEADER_BACKGROUND = Identifier.withDefaultNamespace("textures/gui/tab_header_background.png");
    //?} else {
    /*public static final ResourceLocation TAB_HEADER_BACKGROUND = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/tab_header_background.png");*/
    //?}

    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private final TabManager tabManager = new TabManager(this::addRenderableWidget, this::removeWidget);
    private TabNavigationBar tabNavigationBar;
    
    private SkinPackListWidget packList;
    private SkinGridWidget skinGrid;
    private SkinPreviewPanel previewPanel;
    private final Screen parent; 

    private int activeTab = 0; // 0=skins, 1=customization
    private String selectedPackId;
    private final Map<String, List<LoadedSkin>> skinCache = new HashMap<>();

    private final Rect rPacks = new Rect(), rSkins = new Rect(), rPreview = new Rect();
    private final List<AbstractWidget> customizationWidgets = new ArrayList<>();
    private Button openPacksButton, doneButton;

    public SkinSelectionScreen(Screen parent) {
        super(Component.translatable("bedrockskins.gui.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        FavoritesManager.load();
        buildSkinCache();
        calculateLayout(null);
        
        tabNavigationBar = TabNavigationBar.builder(tabManager, width)
            .addTabs(new SkinsTab(), new SkinCustomizationTab()).build();
        addRenderableWidget(tabNavigationBar);
        
        updateFooterButtons();
        tabNavigationBar.selectTab(0, false);
        repositionElements();
    }
    
    @Override
    public void repositionElements() {
        if (tabNavigationBar != null) {
            tabNavigationBar.setWidth(width);
            tabNavigationBar.arrangeElements();
            int top = tabNavigationBar.getRectangle().bottom();
            tabManager.setTabArea(new ScreenRectangle(0, top, width, height - layout.getFooterHeight() - top));
            layout.setHeaderHeight(top);
            layout.arrangeElements();
            updateFooterButtons();
        }
    }
    
    private void applyTabState(ScreenRectangle tabArea, int tabIndex) {
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
        
        if (activeTab == 1) createCustomizationWidgets();
    }

    private void updateFooterButtons() {
        int btnW = 150, btnH = 20, btnY = height - 28;
        
        if (openPacksButton == null) {
            openPacksButton = Button.builder(Component.translatable("bedrockskins.button.open_packs"), b -> openSkinPacksFolder()).build();
            addRenderableWidget(openPacksButton);
        }
        openPacksButton.setX(width / 2 - 154); openPacksButton.setY(btnY);
        openPacksButton.setWidth(btnW); openPacksButton.setHeight(btnH);

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
            skinCache.computeIfAbsent(skin.getId(), k -> new ArrayList<>()).add(skin);
        }
        
        List<LoadedSkin> favs = new ArrayList<>();
        for (String key : FavoritesManager.getFavoriteKeys()) {
            LoadedSkin s = SkinPackLoader.getLoadedSkin(SkinId.parse(key));
            if (s != null) favs.add(s);
        }
        skinCache.put("skinpack.Favorites", favs);
    }

    private void calculateLayout(ScreenRectangle tabArea) {
        int topY = tabArea != null ? tabArea.top() : (tabNavigationBar != null ? tabNavigationBar.getRectangle().bottom() : 32);
        int areaH = tabArea != null ? tabArea.height() : (height - topY - Math.max(layout.getFooterHeight(), 32));

        int innerH = Math.max(50, areaH - 16); // 8px padding top and bottom
        int fullW = width - 20; // 10px margins
        int sideW = Math.max(130, Math.min(200, (int)(fullW * 0.22)));
        int centerW = fullW - (sideW * 2) - 12; // 6px gaps
        
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
        if (minecraft == null) return;
        int pHead = 24, pPad = 4;

        // Pack List
        int plY = rPacks.y + pHead + pPad, plH = rPacks.h - pHead - (pPad * 2);
        if (packList == null) {
            packList = new SkinPackListWidget(minecraft, rPacks.w - pPad * 2, plH, plY, 24,
                    this::selectPack, id -> Objects.equals(selectedPackId, id), font);
            addRenderableWidget(packList);
        }
        packList.setX(rPacks.x + pPad); packList.setY(plY);
        packList.setWidth(Math.max(10, rPacks.w - pPad * 2)); packList.setHeight(Math.max(10, plH));

        // Preview Panel
        if (previewPanel == null) {
            previewPanel = new SkinPreviewPanel(minecraft, font, this::onFavoritesChanged);
            previewPanel.init(rPreview.x, rPreview.y, rPreview.w, rPreview.h, this::addRenderableWidget);
        } else {
            previewPanel.reposition(rPreview.x, rPreview.y, rPreview.w, rPreview.h);
        }

        // Skin Grid
        int sgY = rSkins.y + pHead + pPad, sgH = rSkins.h - pHead - (pPad * 2);
        if (skinGrid == null) {
            skinGrid = new SkinGridWidget(minecraft, rSkins.w - pPad * 2, sgH, sgY, 90,
                    skin -> previewPanel.setSelectedSkin(skin), () -> previewPanel != null ? previewPanel.getSelectedSkin() : null, font,
                    this::safeRegisterTexture, SkinManager::setPreviewSkin, this::safeResetPreview);
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
        int btnW = Math.min(cols == 2 ? (contentW - gapX) / 2 : Math.min(310, contentW), 150);
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
        
        String display = selectedPackId;
        if ("skinpack.Favorites".equals(selectedPackId)) {
            display = Component.translatable("bedrockskins.gui.favorites").getString();
        } else if (skins != null && !skins.isEmpty()) {
            String t = SkinPackLoader.getTranslation(skins.get(0).getSafePackName());
            display = t != null ? t : skins.get(0).getPackDisplayName();
        }
        return Component.literal(display + " (" + count + ")");
    }

    private void refreshPackList() {
        if (packList == null) return;
        packList.clear();

        List<String> sortedPacks = new ArrayList<>(skinCache.keySet());
        sortedPacks.remove("skinpack.Favorites"); 
        
        sortedPacks.sort(Comparator.comparing((String k) -> {
            int idx = SkinPackLoader.packOrder.indexOf(k);
            return idx == -1 ? Integer.MAX_VALUE : idx;
        }).thenComparing(String::compareToIgnoreCase));

        if (!FavoritesManager.getFavoriteKeys().isEmpty()) sortedPacks.add(0, "skinpack.Favorites");

        for (String pid : sortedPacks) {
            String display = pid, internal = pid;
            List<LoadedSkin> skins = skinCache.get(pid);
            
            if (skins != null && !skins.isEmpty()) {
                display = skins.get(0).getSafePackName();
                internal = skins.get(0).getPackDisplayName();
            }
            if ("skinpack.Favorites".equals(pid)) {
                display = internal = Component.translatable("bedrockskins.gui.favorites").getString();
            }

            packList.addEntryPublic(new SkinPackListWidget.SkinPackEntry(
                    pid, display, internal, this::selectPack, () -> Objects.equals(selectedPackId, pid), font
            ));
        }

        if (selectedPackId == null && !sortedPacks.isEmpty()) selectPack(sortedPacks.get(0));
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
        if (activeTab == 0) {
            GuiUtils.drawPanelChrome(gui, rPacks.x, rPacks.y, rPacks.w, rPacks.h, Component.translatable("bedrockskins.gui.packs"), font);
            GuiUtils.drawPanelChrome(gui, rSkins.x, rSkins.y, rSkins.w, rSkins.h, getSkinsPanelTitle(), font);
        }
            
        if (previewPanel != null) previewPanel.render(gui, mouseX, mouseY);
        super.render(gui, mouseX, mouseY, delta);
        if (previewPanel != null) previewPanel.renderSprites(gui);
        
        gui.blit(RenderPipelines.GUI_TEXTURED, Screen.FOOTER_SEPARATOR, 0, height - layout.getFooterHeight() - 2, 0.0F, 0.0F, width, 2, 32, 2);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        return (!handled && previewPanel != null && previewPanel.mouseClicked(event.x(), event.y(), event.button())) 
            || super.mouseClicked(event, handled);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return (previewPanel != null && previewPanel.mouseReleased(event.x(), event.y(), event.button())) 
            || super.mouseReleased(event);
    }
    
    @Override
    protected void renderMenuBackground(GuiGraphics graphics) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TAB_HEADER_BACKGROUND, 0, 0, 0.0F, 0.0F, width, layout.getHeaderHeight(), 16, 16);
        this.renderMenuBackground(graphics, 0, layout.getHeaderHeight(), width, height);
    }

    private static class Rect {
        int x, y, w, h;
        void set(int x, int y, int w, int h) { this.x = x; this.y = y; this.w = w; this.h = h; }
        int right() { return x + w; }
    }

    @Override
    public void onClose() {
        if (skinGrid != null) skinGrid.clear();
        if (previewPanel != null) previewPanel.cleanup();
        if (minecraft != null) minecraft.setScreen(parent);
    }
    
    private class SkinsTab extends GridLayoutTab {
        public SkinsTab() { super(Component.translatable("bedrockskins.gui.skins")); }
        @Override public void doLayout(ScreenRectangle tabArea) { applyTabState(tabArea, 0); }
    }
    
    private class SkinCustomizationTab extends GridLayoutTab {
        public SkinCustomizationTab() { super(Component.translatable("options.skinCustomisation.title")); }
        @Override public void doLayout(ScreenRectangle tabArea) { applyTabState(tabArea, 1); }
    }
}
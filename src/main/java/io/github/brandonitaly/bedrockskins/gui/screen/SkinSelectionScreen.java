package io.github.brandonitaly.bedrockskins.gui.screen;

import io.github.brandonitaly.bedrockskins.gui.preview.*;
import io.github.brandonitaly.bedrockskins.gui.widget.*;

import io.github.brandonitaly.bedrockskins.client.appearance.skin.FavoritesManager;
import io.github.brandonitaly.bedrockskins.client.appearance.skin.SkinManager;
import io.github.brandonitaly.bedrockskins.client.persistence.BedrockSkinsConfig;
import io.github.brandonitaly.bedrockskins.pack.model.LoadedSkin;
import io.github.brandonitaly.bedrockskins.pack.model.LoadedCosmetic;
import io.github.brandonitaly.bedrockskins.client.appearance.persona.PersonaManager;
import io.github.brandonitaly.bedrockskins.client.appearance.persona.PersonaTypeNames;
import io.github.brandonitaly.bedrockskins.client.appearance.emote.EmoteManager;
import io.github.brandonitaly.bedrockskins.pack.model.LoadedEmote;
import io.github.brandonitaly.bedrockskins.pack.model.SkinId;
import io.github.brandonitaly.bedrockskins.pack.loader.SkinPackLoader;
import io.github.brandonitaly.bedrockskins.util.BedrockSkinsSprites;
import io.github.brandonitaly.bedrockskins.util.PackSortUtil;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
//? if >=26.2
import net.minecraft.client.gui.components.tabs.MenuTabBar;
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
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import io.github.brandonitaly.bedrockskins.client.appearance.cape.CapeManager;
import io.github.brandonitaly.bedrockskins.client.appearance.cape.CapeManager.MinecraftCape;
import net.minecraft.resources.Identifier;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;

public class SkinSelectionScreen extends Screen {
    private static final int[] PERSONA_COLORS = {
        0xEDE5F8, 0xEFE1BB, 0xFBE28A, 0xDFBE7A, 0xC7962D, 0xAD7A49,
        0xEB983F, 0xE99027, 0x8A6294, 0xA1570B, 0x933F1E, 0x6E2800,
        0x441600, 0x7B5028, 0x654529, 0x442711, 0x2E180E, 0x2F2F2F,
        0x2C1A1A, 0x202832, 0x281928, 0x1B110D, 0x646775, 0x9698A2,
        0xED8DAC, 0xBD44B3, 0x792AAC, 0x35399D, 0x3AAFD9, 0x158991,
        0x546D1B, 0x70B919, 0xF8C627, 0xD87D3E, 0xA12722
    };
    private static final org.slf4j.Logger MOD_LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static final String STORE_FOLDER = "skin_packs";
    private static final String FAVORITES_PACK_ID = "skinpack.Favorites";
    private static final int SEARCH_WIDTH = 108;
    private static final int SEARCH_HEIGHT = 16;
    private static final int SEARCH_ICON_WIDTH = 16;
    private static final int SEARCH_CLOSE_WIDTH = 14;
    private static final int SEARCH_SPRITE_SIZE = 9;
    private static final int SEARCH_HORIZONTAL_INSET = 3;
    private static final int SEARCH_VERTICAL_INSET = 3;
    private static final int PANEL_CONTENT_INSET = 2;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private final TabManager tabManager = new TabManager(this::addRenderableWidget, this::removeWidget);
    private TabNavigationBar tabNavigationBar;
    
    private SidebarListWidget packList;
    private SkinGridWidget skinGrid;
    private SkinPreviewPanel previewPanel;
    private final Screen parent; 

    private AppearanceTab activeTab = AppearanceTab.SKINS;
    private String selectedPackId;
    private final Map<String, List<LoadedSkin>> skinCache = new HashMap<>();

    private final Rect rPacks = new Rect(), rSkins = new Rect(), rPreview = new Rect();
    private final Rect rCosmeticCategories = new Rect(), rCosmeticOptions = new Rect();
    private Button openPacksButton, doneButton;
    private SpriteIconButton colorPickerButton;
    private ColorPaletteWidget colorPalette;
    private Button previousSideButton, nextSideButton;
    private boolean colorPickerOpen;

    private boolean needsReload = false;

    // --- Cosmetics and Capes Tab Elements
    private CapeGridWidget capeGrid;
    private SidebarListWidget capeSidebar;
    private SidebarListWidget cosmeticSidebar;
    private SidebarListWidget emoteSidebar;
    private CosmeticGridWidget cosmeticGrid;
    private EmoteGridWidget emoteGrid;
    private EditBox searchBox;
    private boolean searchExpanded;
    private List<LoadedSkin> displayedSkins = List.of();
    private List<LoadedCosmetic> displayedCosmetics = List.of();
    private List<LoadedEmote> displayedEmotes = List.of();
    private int displayedSkinColumns = -1;
    private int displayedCosmeticColumns = -1;
    private int displayedEmoteColumns = -1;
    private String selectedCosmeticType = "all";
    private String selectedCapesCategory = "owned";
    private List<MinecraftCape> ownedCapes = null;
    private boolean isFetchingCapes = false;
    private String capeFetchError = null;
    private int visibleCosmeticCount;

    public AppearanceTab getActiveTab() { return activeTab; }
    public String getSelectedCapesCategory() { return selectedCapesCategory; }

    void restorePreviewAfterFullScreen(float rotation) {
        if (previewPanel != null) previewPanel.restoreAfterFullScreen(rotation);
    }

    void restorePreviewAfterChildScreen() {
        if (previewPanel != null) previewPanel.initPreviewState();
    }

    public SkinSelectionScreen(Screen parent) {
        this(parent, AppearanceTab.SKINS);
    }

    public SkinSelectionScreen(Screen parent, AppearanceTab initialTab) {
        super(Component.translatable("bedrockskins.gui.title"));
        this.parent = parent;
        this.activeTab = initialTab != null ? initialTab : AppearanceTab.SKINS;
    }

    @Override
    protected void init() {
        super.init();
        FavoritesManager.load();
        buildSkinCache();
        openToCurrentSkin();
        calculateLayout(null);

        if (previewPanel == null) {
            previewPanel = new SkinPreviewPanel(minecraft, font, this::onFavoritesChanged);
        }
        previewPanel.init(rPreview.x, rPreview.y, rPreview.w, rPreview.h, this, this::addRenderableWidget);
        
        //~ if >=26.2 'TabNavigationBar.' -> 'MenuTabBar.' {
        tabNavigationBar = MenuTabBar.builder(tabManager, width)
            .addTabs(Arrays.stream(AppearanceTab.values()).map(AppearanceGridTab::new).toArray(GridLayoutTab[]::new))
            .build();//~}
        
        this.addRenderableWidget(tabNavigationBar);

        updateFooterButtons();
        tabNavigationBar.selectTab(activeTab.ordinal(), false);
        repositionElements();
    }

    @Override
    public void removed() {
        if (previewPanel != null) previewPanel.cleanup();
        super.removed();
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
            //? if >=26.2 {
            tabNavigationBar.arrangeElements(width);
            //?} else if >26.1 {
            /*tabNavigationBar.updateWidth(width);
            tabNavigationBar.arrangeElements();
            *///?} else {
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
    
    private void applyTabState(ScreenRectangle tabArea, AppearanceTab tab) {
        if (this.activeTab != tab) {
            selectedPackId = null;
            if (previewPanel != null) {
                previewPanel.setSelectedCape(null);
                previewPanel.setSelectedCosmetic(null);
                previewPanel.setSelectedEmote(null);
                previewPanel.setSelectedSkin(null);
                previewPanel.initPreviewState();
            }
        }
        
        activeTab = tab;
        calculateLayout(tabArea);
        initWidgets(tabArea);
        
        boolean isSkins = activeTab == AppearanceTab.SKINS;
        setVisible(isSkins, packList, skinGrid);
        if (previewPanel != null) {
            previewPanel.reposition(rPreview.x, rPreview.y, rPreview.w, rPreview.h);
            previewPanel.updateButtonsForTab(activeTab);
        }
        positionCosmeticControls();

        boolean isCosmetics = activeTab == AppearanceTab.COSMETICS;
        setVisible(isCosmetics, cosmeticGrid, cosmeticSidebar);
        if (isCosmetics) refreshCosmeticGrid();

        boolean isCapes = activeTab == AppearanceTab.CAPES;
        setVisible(isCapes, capeGrid, capeSidebar);
        if (isCapes) {
            refreshCapeGrid();
            if ("owned".equals(selectedCapesCategory)) {
                if (ownedCapes == null && !isFetchingCapes) fetchCapes();
                else if (ownedCapes != null) autoSelectActiveCape();
            }
        }

        boolean isEmotes = activeTab == AppearanceTab.EMOTES;
        setVisible(isEmotes, emoteGrid, emoteSidebar);
        if (isEmotes) {
            refreshEmoteGrid();
            if (previewPanel != null && previewPanel.getSelectedEmote() == null
                    && !EmoteManager.all().isEmpty()) {
                previewPanel.setSelectedEmote(EmoteManager.all().getFirst());
            }
        }

        if (!isCosmetics) colorPickerOpen = false;
        updateCosmeticControls();
        
        updateFooterButtons();
    }

    private void updateFooterButtons() {
        int btnW = 150, btnH = 20, btnY = height - 28;
        
        if (openPacksButton == null) {
            openPacksButton = Button.builder(Component.translatable("bedrockskins.button.open_packs"), b -> openSkinPacksFolder()).build();
            addRenderableWidget(openPacksButton);
        }
        openPacksButton.setTooltip(Tooltip.create(Component.translatable("pack.folderInfo")));

        if (doneButton == null) {
            doneButton = Button.builder(CommonComponents.GUI_DONE, b -> onClose()).build();
            addRenderableWidget(doneButton);
        }

        openPacksButton.setPosition(width / 2 - 154, btnY);
        openPacksButton.setWidth(btnW); openPacksButton.setHeight(btnH);
        openPacksButton.visible = true;

        doneButton.setPosition(width / 2 + 4, btnY);
        doneButton.setWidth(btnW); doneButton.setHeight(btnH);
    }

    private void buildSkinCache() {
        skinCache.clear();
        for (String packId : SkinPackLoader.packTypesByPackId.keySet()) {
            skinCache.put(packId, new ArrayList<>());
        }
        for (LoadedSkin skin : SkinPackLoader.loadedSkinsSnapshot()) {
            skinCache.computeIfAbsent(skin.packId, k -> new ArrayList<>()).add(skin);
        }

        // Keep the account skin first and the importer action last, with user imports between them.
        List<LoadedSkin> imports = skinCache.computeIfAbsent(
            MinecraftAccountSkin.PACK_ID, ignored -> new ArrayList<>());
        LoadedSkin importAction = imports.stream().filter(ImportSkinAction::is).findFirst().orElse(null);
        imports.removeIf(skin -> MinecraftAccountSkin.is(skin) || ImportSkinAction.is(skin));
        imports.addFirst(MinecraftAccountSkin.INSTANCE);
        if (importAction != null) imports.add(importAction);
        
        List<LoadedSkin> favs = FavoritesManager.getFavoriteKeys().stream()
            .map(SkinId::parse)
            .map(id -> id != null && id.equals(MinecraftAccountSkin.INSTANCE.skinId)
                ? MinecraftAccountSkin.INSTANCE : SkinPackLoader.getLoadedSkin(id))
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
        rCosmeticCategories.set(rPacks.x, rPacks.y, rPacks.w, rPacks.h);
        rCosmeticOptions.set(rPacks.x, rPacks.y + rPacks.h, rPacks.w, 0);
    }

    private void initWidgets(ScreenRectangle tabArea) {
        int contentY = panelContentY(rSkins);
        int contentH = panelContentHeight(rSkins);
        int contentWidth = panelContentWidth(rSkins);
        int sidebarWidth = panelContentWidth(rPacks);

        // Skins Widgets
        int plY = contentY, plH = contentH;
        if (packList == null) {
            packList = new SidebarListWidget(minecraft, sidebarWidth, plH, plY, 28, font);
            addRenderableWidget(packList);
        }
        positionPanelContent(packList, rPacks);

        int sgY = contentY, sgH = contentH;
        if (skinGrid == null) {
            skinGrid = new SkinGridWidget(minecraft, contentWidth, sgH, sgY, 90,
                    skin -> previewPanel.setSelectedSkin(skin), 
                    () -> previewPanel != null ? previewPanel.getSelectedSkin() : null, font);
            addRenderableWidget(skinGrid);
        }
        positionPanelContent(skinGrid, rSkins);

        // Cosmetics Widgets
        int cgY = contentY;
        int cgH = contentH;
        if (cosmeticSidebar == null) {
            cosmeticSidebar = new SidebarListWidget(minecraft, sidebarWidth, cgH, cgY, 28, font);
            addCosmeticCategory(PersonaTypeNames.EQUIPPED);
            addCosmeticCategory(PersonaTypeNames.ALL);
            addCosmeticCategory("persona_hair");
            addCosmeticCategory("persona_facial_hair");
            addCosmeticCategory("persona_arms");
            addCosmeticCategory("persona_legs");
            addCosmeticCategory("persona_top");
            addCosmeticCategory(PersonaTypeNames.BOTTOMS);
            addCosmeticCategory("persona_outerwear");
            addCosmeticCategory("persona_head");
            addCosmeticCategory("persona_hand");
            addCosmeticCategory("persona_feet");
            addCosmeticCategory("persona_face_accessory");
            addCosmeticCategory("persona_back");
            addRenderableWidget(cosmeticSidebar);
        }
        positionPanelContent(cosmeticSidebar, rPacks);
        cosmeticSidebar.visible = activeTab == AppearanceTab.COSMETICS;

        if (cosmeticGrid == null) {
            cosmeticGrid = new CosmeticGridWidget(minecraft, contentWidth, cgH, cgY, 65,
                this::selectCosmetic,
                () -> previewPanel != null ? previewPanel.getSelectedCosmetic() : null, font);
            addRenderableWidget(cosmeticGrid);
        }
        positionPanelContent(cosmeticGrid, rSkins);
        cosmeticGrid.visible = activeTab == AppearanceTab.COSMETICS;

        if (searchBox == null) {
            searchBox = new EditBox(font, 0, 0,
                SEARCH_WIDTH - SEARCH_ICON_WIDTH - SEARCH_CLOSE_WIDTH
                    - SEARCH_HORIZONTAL_INSET * 2,
                SEARCH_HEIGHT - SEARCH_VERTICAL_INSET * 2,
                Component.translatable("bedrockskins.gui.search"));
            searchBox.setHint(Component.translatable("bedrockskins.gui.search"));
            searchBox.setBordered(false);
            searchBox.setMaxLength(64);
            searchBox.setResponder(ignored -> refreshActiveGrid());
            addRenderableWidget(searchBox);
        }
        int searchWidth = searchWidth();
        int searchY = searchTop();
        int searchX = rSkins.right() - 4 - searchWidth;
        searchBox.setPosition(
            searchX + SEARCH_ICON_WIDTH + SEARCH_HORIZONTAL_INSET,
            searchY + SEARCH_VERTICAL_INSET + 1);
        searchBox.setWidth(Math.max(4, searchWidth - SEARCH_ICON_WIDTH
            - SEARCH_CLOSE_WIDTH - SEARCH_HORIZONTAL_INSET * 2));
        searchBox.setHeight(SEARCH_HEIGHT - SEARCH_VERTICAL_INSET * 2);

        if (colorPickerButton == null) {
            colorPickerButton = SpriteIconButton.builder(Component.empty(), button -> {
                colorPickerOpen = !colorPickerOpen;
                updateCosmeticCustomizationLayout();
                updateCosmeticControls();
            }, true).size(20, 20).sprite(BedrockSkinsSprites.COLOR_PICKER_ICON, 16, 16).build();
            colorPickerButton.setTooltip(Tooltip.create(Component.translatable("bedrockskins.persona.color.button")));
            addRenderableWidget(colorPickerButton);
        }
        if (colorPalette == null) {
            colorPalette = new ColorPaletteWidget(PERSONA_COLORS, color -> {
                LoadedCosmetic cosmetic = previewPanel == null ? null : previewPanel.getSelectedCosmetic();
                if (!PersonaManager.isColorSelectable(cosmetic)) return;
                PersonaManager.setTintColor(cosmetic, color);
                refreshSelectedCosmeticPreview(cosmetic);
            }, () -> PersonaManager.localTintColor(
                previewPanel == null ? null : previewPanel.getSelectedCosmetic()));
            addRenderableWidget(colorPalette);
        }
        if (previousSideButton == null) {
            previousSideButton = Button.builder(Component.literal("<"), button -> cycleSelectedLimbSide(-1))
                .bounds(0, 0, 20, 20).build();
            nextSideButton = Button.builder(Component.literal(">"), button -> cycleSelectedLimbSide(1))
                .bounds(0, 0, 20, 20).build();
            addRenderableWidget(previousSideButton);
            addRenderableWidget(nextSideButton);
        }
        positionCosmeticControls();

        // Capes Widgets
        if (capeSidebar == null) {
            capeSidebar = new SidebarListWidget(minecraft, sidebarWidth, cgH, cgY, 28, font);
            capeSidebar.add(Component.translatable("bedrockskins.capes.owned"),
                () -> selectCapesCategory("owned"), () -> "owned".equals(selectedCapesCategory));
            capeSidebar.add(Component.translatable("bedrockskins.capes.skinpack"),
                () -> selectCapesCategory("skinpack"), () -> "skinpack".equals(selectedCapesCategory));
            addRenderableWidget(capeSidebar);
        }
        positionPanelContent(capeSidebar, rPacks);
        capeSidebar.visible = activeTab == AppearanceTab.CAPES;

        if (capeGrid == null) {
            capeGrid = new CapeGridWidget(minecraft, contentWidth, cgH, cgY, 65,
                cape -> {
                    if (previewPanel != null) {
                        previewPanel.setSelectedCape(cape);
                        previewPanel.playCapeSelectionAnimation();
                    }
                },
                () -> previewPanel != null ? previewPanel.getSelectedCape() : null, font);
            addRenderableWidget(capeGrid);
        }
        positionPanelContent(capeGrid, rSkins);
        capeGrid.visible = activeTab == AppearanceTab.CAPES;

        // Emotes Widgets
        if (emoteSidebar == null) {
            emoteSidebar = new SidebarListWidget(minecraft, sidebarWidth, cgH, cgY, 28, font);
            emoteSidebar.add(Component.translatable("bedrockskins.emotes.all"), () -> {}, () -> true);
            addRenderableWidget(emoteSidebar);
        }
        positionPanelContent(emoteSidebar, rPacks);
        emoteSidebar.visible = activeTab == AppearanceTab.EMOTES;

        if (emoteGrid == null) {
            emoteGrid = new EmoteGridWidget(minecraft, contentWidth, cgH, cgY, 90,
                emote -> {
                    if (previewPanel != null) previewPanel.setSelectedEmote(emote);
                }, () -> previewPanel != null ? previewPanel.getSelectedEmote() : null, font);
            addRenderableWidget(emoteGrid);
        }
        positionPanelContent(emoteGrid, rSkins);
        emoteGrid.visible = activeTab == AppearanceTab.EMOTES;

        updateCosmeticCustomizationLayout();
        updateCosmeticControls();
        refreshPackList();
    }

    private static int panelContentY(Rect panel) {
        return panel.y + GuiUtils.PANEL_HEADER_HEIGHT - 1;
    }

    private static int panelContentHeight(Rect panel) {
        return Math.max(10, panel.h - GuiUtils.PANEL_HEADER_HEIGHT - 1);
    }

    private static int panelContentWidth(Rect panel) {
        return Math.max(10, panel.w - PANEL_CONTENT_INSET * 2);
    }

    private static void positionPanelContent(AbstractWidget widget, Rect panel) {
        widget.setPosition(panel.x + PANEL_CONTENT_INSET, panelContentY(panel));
        widget.setWidth(panelContentWidth(panel));
        widget.setHeight(panelContentHeight(panel));
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

        return Component.literal(GuiSkinUtils.getPackDisplayName(selectedPackId) + " (" + count + ")");
    }

    private void refreshPackList() {
        if (packList == null) return;
        packList.clear();

        Set<String> packIds = new LinkedHashSet<>(SkinPackLoader.packTypesByPackId.keySet());
        packIds.addAll(skinCache.keySet());

        List<String> sortedPacks = packIds.stream()
                .filter(pid -> !FAVORITES_PACK_ID.equals(pid) && !"skinpack.Remote".equals(pid))
                .sorted(PackSortUtil.buildPackComparator(BedrockSkinsConfig.getPackSortOrder(), pid -> {
                    return GuiSkinUtils.getPackDisplayName(pid);
                }))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        if (!FavoritesManager.getFavoriteKeys().isEmpty()) sortedPacks.addFirst(FAVORITES_PACK_ID);

        for (String pid : sortedPacks) {
            Component name = Component.literal(GuiSkinUtils.translatedOrFallback(pid, pid));
            packList.add(name, () -> selectPack(pid), () -> Objects.equals(selectedPackId, pid));
        }

        if (selectedPackId == null && !sortedPacks.isEmpty()) selectPack(sortedPacks.getFirst());
        else if (selectedPackId != null) selectPack(selectedPackId); // Force re-render of grid
    }

    private void selectPack(String packId) {
        this.selectedPackId = packId;
        if (skinGrid != null) {
            List<LoadedSkin> skins = skinCache.getOrDefault(packId, List.of()).stream()
                .filter(skin -> matchesSearch(GuiSkinUtils.getSkinDisplayNameText(skin),
                    skin.skinDisplayName, skin.safeSkinName,
                    skin.serializeName, skin.packDisplayName))
                .toList();
            int cols = Math.max(1, (rSkins.w - 18) / 65);
            if (displayedSkinColumns == cols && displayedSkins.equals(skins)) return;
            displayedSkins = List.copyOf(skins);
            displayedSkinColumns = cols;
            skinGrid.clear();
            skinGrid.setScrollAmount(0.0);
            addGridRows(skins, cols, skinGrid::addSkinsRow);
        }
    }

    private void openSkinPacksFolder() {
        File dir = new File(minecraft.gameDirectory, STORE_FOLDER);
        if (!dir.exists()) dir.mkdirs();
        Util.getPlatform().openFile(dir);
    }

    public void markNeedsReload() {
        this.needsReload = true;
    }

    public void triggerReloadIfNeeded() {
        if (needsReload) {
            io.github.brandonitaly.bedrockskins.client.BedrockSkinsClient.reloadResources(minecraft);
            needsReload = false;
        }
    }

    public void onResourcesReloaded() {
        buildSkinCache();
        refreshPackList();
        refreshCosmeticGrid();
        refreshEmoteGrid();
        if (previewPanel != null) {
            LoadedSkin oldSelected = previewPanel.getSelectedSkin();
            if (oldSelected != null) {
                LoadedSkin newSelected = SkinPackLoader.getLoadedSkin(oldSelected.skinId);
                previewPanel.setSelectedSkin(newSelected);
            } else {
                previewPanel.initPreviewState();
            }
        }
    }

    public void extractRenderState(GuiGraphicsExtractor gui, int mouseX, int mouseY, float delta) {
        if (activeTab == AppearanceTab.SKINS) {
            GuiUtils.drawPanelChrome(gui, rPacks.x, rPacks.y, rPacks.w, rPacks.h, Component.translatable("bedrockskins.gui.packs"), font);
            GuiUtils.drawPanelChrome(gui, rSkins.x, rSkins.y, rSkins.w, rSkins.h, getSkinsPanelTitle(), font);
        } else if (activeTab == AppearanceTab.COSMETICS) {
            GuiUtils.drawPanelChrome(gui, rCosmeticCategories.x, rCosmeticCategories.y,
                rCosmeticCategories.w, rCosmeticCategories.h, Component.translatable("bedrockskins.gui.categories"), font);
            if (colorPickerOpen) {
                renderCosmeticCustomization(gui, mouseX, mouseY);
            } else {
                Component gridTitle = PersonaTypeNames.EQUIPPED.equals(selectedCosmeticType)
                    ? PersonaTypeNames.displayName(PersonaTypeNames.EQUIPPED)
                    : "all".equals(selectedCosmeticType)
                        ? Component.translatable("bedrockskins.cosmetics.all")
                        : PersonaTypeNames.displayName(selectedCosmeticType);
                GuiUtils.drawPanelChrome(gui, rSkins.x, rSkins.y, rSkins.w, rSkins.h, gridTitle, font);
                if (visibleCosmeticCount == 0) gui.centeredText(font, Component.translatable("bedrockskins.cosmetics.none"),
                    rSkins.x + rSkins.w / 2, rSkins.y + rSkins.h / 2, 0xFFAAAAAA);
            }
        } else if (activeTab == AppearanceTab.CAPES) {
            GuiUtils.drawPanelChrome(gui, rPacks.x, rPacks.y, rPacks.w, rPacks.h, Component.translatable("bedrockskins.gui.categories"), font);
            Component gridTitle = "owned".equals(selectedCapesCategory)
                ? Component.translatable("bedrockskins.capes.owned")
                : Component.translatable("bedrockskins.capes.skinpack");
            GuiUtils.drawPanelChrome(gui, rSkins.x, rSkins.y, rSkins.w, rSkins.h, gridTitle, font);

            int centerX = rSkins.x + rSkins.w / 2;
            int centerY = rSkins.y + rSkins.h / 2;
            if ("owned".equals(selectedCapesCategory)) {
                if (capeFetchError != null) {
                    gui.centeredText(font, Component.literal(capeFetchError), centerX, centerY, 0xFFFF5555);
                } else if (isFetchingCapes) {
                    gui.centeredText(font, Component.translatable("bedrockskins.status.loading"), centerX, centerY, 0xFFFFAA00);
                } else if (ownedCapes != null && ownedCapes.isEmpty()) {
                    gui.centeredText(font, Component.translatable("bedrockskins.capes.none_owned"), centerX, centerY, 0xFFAAAAAA);
                }
            }
        } else if (activeTab == AppearanceTab.EMOTES) {
            GuiUtils.drawPanelChrome(gui, rPacks.x, rPacks.y, rPacks.w, rPacks.h,
                Component.translatable("bedrockskins.gui.categories"), font);
            GuiUtils.drawPanelChrome(gui, rSkins.x, rSkins.y, rSkins.w, rSkins.h,
                Component.translatable("bedrockskins.gui.emotes"), font);
            if (EmoteManager.all().isEmpty()) {
                gui.centeredText(font, Component.translatable("bedrockskins.emotes.none"),
                    rSkins.x + rSkins.w / 2, rSkins.y + rSkins.h / 2, 0xFFAAAAAA);
            }
        }

        renderSearchChrome(gui, mouseX, mouseY);
            
        if (previewPanel != null) previewPanel.renderPreview(gui, mouseX);
        if (activeTab == AppearanceTab.COSMETICS
                && PersonaManager.isSideSelectable(previewPanel == null ? null : previewPanel.getSelectedCosmetic())) {
            gui.centeredText(font, selectedLimbSideLabel(), rPreview.x + rPreview.w / 2,
                rPreview.y + Math.max(0, (GuiUtils.PANEL_HEADER_HEIGHT - font.lineHeight) / 2),
                0xFFFFFFFF);
        }
        super.extractRenderState(gui, mouseX, mouseY, delta);
        if (previewPanel != null) previewPanel.renderSprites(gui);
        
        gui.blit(RenderPipelines.GUI_TEXTURED, Screen.FOOTER_SEPARATOR, 0, height - layout.getFooterHeight() - 2, 0.0F, 0.0F, width, 2, 32, 2);
    }

    private void renderSearchChrome(GuiGraphicsExtractor gui, int mouseX, int mouseY) {
        if (!isSearchAvailable()) return;

        int width = searchExpanded ? searchWidth() : SEARCH_HEIGHT;
        int height = SEARCH_HEIGHT;
        int x = searchLeft();
        int y = searchTop();
        boolean hovered = isOverSearch(mouseX, mouseY);
        int borderColor = searchBox.isFocused() ? 0xFF8A8D90 : hovered ? 0xFF777C82 : 0xFF4A4E52;

        gui.fill(x + 1, y + 1, x + width + 1, y + height + 1, 0xD0000000);
        gui.fill(x, y, x + width, y + height, borderColor);
        gui.fill(x + 1, y + 1, x + width - 1, y + height - 1,
            searchExpanded ? 0xFF292A2C : hovered ? 0xFF343638 : 0xFF2D2F31);

        gui.blitSprite(RenderPipelines.GUI_TEXTURED, BedrockSkinsSprites.SEARCH_ICON,
            x + (SEARCH_ICON_WIDTH - SEARCH_SPRITE_SIZE) / 2,
            y + (SEARCH_HEIGHT - SEARCH_SPRITE_SIZE) / 2,
            SEARCH_SPRITE_SIZE, SEARCH_SPRITE_SIZE);
        if (searchExpanded) {
            gui.blitSprite(RenderPipelines.GUI_TEXTURED, BedrockSkinsSprites.CLOSE_ICON,
                x + width - SEARCH_CLOSE_WIDTH
                    + (SEARCH_CLOSE_WIDTH - SEARCH_SPRITE_SIZE) / 2,
                y + (SEARCH_HEIGHT - SEARCH_SPRITE_SIZE) / 2,
                SEARCH_SPRITE_SIZE, SEARCH_SPRITE_SIZE);
        }
    }

    private boolean isOverSearch(double mouseX, double mouseY) {
        if (!isSearchAvailable()) return false;
        int width = searchExpanded ? searchWidth() : SEARCH_HEIGHT;
        int x = searchLeft();
        int y = searchTop();
        int height = SEARCH_HEIGHT;
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private boolean isOverSearchClose(double mouseX, double mouseY) {
        return searchExpanded && isOverSearch(mouseX, mouseY)
            && mouseX >= searchLeft() + searchWidth() - SEARCH_CLOSE_WIDTH;
    }

    private boolean isSearchAvailable() {
        return searchBox != null && !(activeTab == AppearanceTab.COSMETICS && colorPickerOpen);
    }

    private int searchWidth() {
        return Math.min(SEARCH_WIDTH, Math.max(SEARCH_HEIGHT, rSkins.w - 8));
    }

    private int searchLeft() {
        int width = searchExpanded ? searchWidth() : SEARCH_HEIGHT;
        return rSkins.right() - 4 - width;
    }

    private int searchTop() {
        return rSkins.y - 1 + (GuiUtils.PANEL_HEADER_HEIGHT - SEARCH_HEIGHT) / 2;
    }

    private void renderCosmeticCustomization(GuiGraphicsExtractor gui, int mouseX, int mouseY) {
        LoadedCosmetic cosmetic = previewPanel == null ? null : previewPanel.getSelectedCosmetic();
        boolean colors = colorPickerOpen && PersonaManager.isColorSelectable(cosmetic);
        if (!colors || rCosmeticOptions.h <= 0) return;

        Component heading = Component.translatable("bedrockskins.persona.color.title");
        GuiUtils.drawPanelChrome(gui, rCosmeticOptions.x, rCosmeticOptions.y,
            rCosmeticOptions.w, rCosmeticOptions.h, heading, font);
    }

    private void refreshSelectedCosmeticPreview(LoadedCosmetic cosmetic) {
        if (previewPanel != null && previewPanel.getSelectedCosmetic() == cosmetic) {
            previewPanel.refreshSelectedCosmeticPreview();
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && isOverSearch(event.x(), event.y())) {
            if (!searchExpanded) {
                GuiUtils.playButtonClickSound();
                searchExpanded = true;
                updateCosmeticControls();
            } else if (isOverSearchClose(event.x(), event.y())) {
                GuiUtils.playButtonClickSound();
                searchBox.setValue("");
                searchExpanded = false;
                searchBox.setFocused(false);
                setFocused(null);
                updateCosmeticControls();
                return true;
            }
            setFocused(searchBox);
            searchBox.setFocused(true);
            if (!searchBox.isMouseOver(event.x(), event.y())) return true;
        }
        return super.mouseClicked(event, doubled)
            || (previewPanel != null && previewPanel.mouseClicked(event.x(), event.y(), event.button()));
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return (previewPanel != null && previewPanel.mouseReleased(event.button()))
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
        if (cosmeticGrid != null) cosmeticGrid.clear();
        if (emoteGrid != null) emoteGrid.clear();
        if (previewPanel != null) previewPanel.cleanup();
        minecraft.gui.setScreen(parent);
    }
    
    // --- Tabs ---

    private class AppearanceGridTab extends GridLayoutTab {
        private final AppearanceTab tab;

        AppearanceGridTab(AppearanceTab tab) {
            super(tab.title());
            this.tab = tab;
        }

        @Override
        public void doLayout(ScreenRectangle tabArea) {
            applyTabState(tabArea, tab);
        }
    }

    private void refreshActiveGrid() {
        switch (activeTab) {
            case SKINS -> {
                if (selectedPackId != null) selectPack(selectedPackId);
            }
            case COSMETICS -> refreshCosmeticGrid();
            case EMOTES -> refreshEmoteGrid();
            case CAPES -> refreshCapeGrid();
        }
    }

    private static void setVisible(boolean visible, AbstractWidget... widgets) {
        for (AbstractWidget widget : widgets) {
            if (widget != null) widget.visible = visible;
        }
    }

    private static <T> void addGridRows(List<T> values, int columns, Consumer<List<T>> addRow) {
        for (int i = 0; i < values.size(); i += columns) {
            addRow.accept(values.subList(i, Math.min(i + columns, values.size())));
        }
    }

    private String searchQuery() {
        return searchBox == null ? "" : searchBox.getValue().strip().toLowerCase(Locale.ROOT);
    }

    private boolean matchesSearch(String... values) {
        String query = searchQuery();
        if (query.isEmpty()) return true;
        for (String value : values) {
            if (value != null && value.toLowerCase(Locale.ROOT).contains(query)) return true;
        }
        return false;
    }

    private void refreshEmoteGrid() {
        if (emoteGrid == null) return;
        List<LoadedEmote> emotes = EmoteManager.all().stream()
            .filter(emote -> matchesSearch(emote.displayName(), emote.id(), emote.animationName()))
            .toList();
        int columns = Math.max(1, (rSkins.w - 18) / 65);
        if (displayedEmoteColumns == columns && displayedEmotes.equals(emotes)) return;
        displayedEmotes = List.copyOf(emotes);
        displayedEmoteColumns = columns;
        emoteGrid.clear();
        emoteGrid.setScrollAmount(0.0);
        addGridRows(emotes, columns, emoteGrid::addEmotesRow);
    }

    public void openEmoteSlotPicker(LoadedEmote emote) {
        if (emote == null) return;
        minecraft.gui.setScreen(new EmoteWheelScreen(this, emote));
    }

    private void addCosmeticCategory(String type) {
        cosmeticSidebar.add(PersonaTypeNames.displayName(type),
            () -> {
                selectedCosmeticType = type;
                refreshCosmeticGrid();
            }, () -> type.equals(selectedCosmeticType));
    }

    private void selectCosmetic(LoadedCosmetic cosmetic) {
        LoadedCosmetic previous = previewPanel == null ? null : previewPanel.getSelectedCosmetic();
        if (previewPanel != null) previewPanel.setSelectedCosmetic(cosmetic);
        if (previous != cosmetic) colorPickerOpen = false;
        updateCosmeticCustomizationLayout();
        updateCosmeticControls();
    }

    private void positionCosmeticControls() {
        if (colorPickerButton != null) {
            int colorX = previewPanel != null ? previewPanel.floatingControlX() : rPreview.x;
            int colorY = previewPanel != null ? previewPanel.floatingControlY() : rPreview.y;
            colorPickerButton.setPosition(colorX, colorY);
        }
        int sideY = rPreview.y + (GuiUtils.PANEL_HEADER_HEIGHT - 20) / 2;
        if (previousSideButton != null) previousSideButton.setPosition(rPreview.x + 8, sideY);
        if (nextSideButton != null) nextSideButton.setPosition(rPreview.right() - 28, sideY);
    }

    private void updateCosmeticControls() {
        LoadedCosmetic cosmetic = previewPanel == null ? null : previewPanel.getSelectedCosmetic();
        boolean cosmeticsTab = activeTab == AppearanceTab.COSMETICS;
        boolean sideSelectable = cosmeticsTab && PersonaManager.isSideSelectable(cosmetic);
        boolean colorUsable = cosmeticsTab && PersonaManager.isColorSelectable(cosmetic);
        if (!colorUsable) colorPickerOpen = false;
        if (colorPickerButton != null) {
            colorPickerButton.visible = colorUsable;
            colorPickerButton.active = colorUsable;
        }
        if (searchBox != null) {
            searchBox.visible = isSearchAvailable() && searchExpanded;
        }
        if (cosmeticGrid != null) cosmeticGrid.visible = cosmeticsTab && !colorPickerOpen;
        boolean colorsVisible = cosmeticsTab && colorPickerOpen && colorUsable;
        if (colorPalette != null) {
            colorPalette.visible = colorsVisible;
            colorPalette.active = colorsVisible;
            colorPalette.setBounds(rCosmeticOptions.x + 4, customizationContentY(),
                rCosmeticOptions.w - 8,
                rCosmeticOptions.h - GuiUtils.PANEL_HEADER_HEIGHT - 8);
        }
        if (previousSideButton != null) previousSideButton.visible = sideSelectable;
        if (nextSideButton != null) nextSideButton.visible = sideSelectable;
    }

    private void cycleSelectedLimbSide(int direction) {
        LoadedCosmetic cosmetic = previewPanel == null ? null : previewPanel.getSelectedCosmetic();
        if (!PersonaManager.isSideSelectable(cosmetic)) return;
        PersonaManager.EquipSide[] order = {
            PersonaManager.EquipSide.LEFT, PersonaManager.EquipSide.BOTH, PersonaManager.EquipSide.RIGHT
        };
        PersonaManager.EquipSide selected = PersonaManager.localSide(cosmetic);
        int index = 0;
        for (int i = 0; i < order.length; i++) if (order[i] == selected) index = i;
        index = Math.floorMod(index + direction, order.length);
        PersonaManager.setLocalSide(cosmetic, order[index]);
        refreshSelectedCosmeticPreview(cosmetic);
    }

    private Component selectedLimbSideLabel() {
        LoadedCosmetic cosmetic = previewPanel == null ? null : previewPanel.getSelectedCosmetic();
        if (!PersonaManager.isSideSelectable(cosmetic)) return Component.empty();
        String limb = "persona_arms".equals(cosmetic.type) ? "arm" : "leg";
        String side = PersonaManager.localSide(cosmetic).name().toLowerCase(Locale.ROOT);
        return Component.translatable("bedrockskins.persona.side." + side + "_" + limb);
    }

    private void updateCosmeticCustomizationLayout() {
        if (cosmeticSidebar == null) return;
        LoadedCosmetic cosmetic = previewPanel == null ? null : previewPanel.getSelectedCosmetic();
        boolean colors = colorPickerOpen && PersonaManager.isColorSelectable(cosmetic);
        rCosmeticCategories.set(rPacks.x, rPacks.y, rPacks.w, rPacks.h);
        rCosmeticOptions.set(rSkins.x, rSkins.y, rSkins.w, colors ? rSkins.h : 0);
        positionPanelContent(cosmeticSidebar, rCosmeticCategories);
    }

    private int customizationContentY() {
        return rCosmeticOptions.y + GuiUtils.PANEL_HEADER_HEIGHT + 4;
    }

    private void refreshCosmeticGrid() {
        if (cosmeticGrid == null) return;
        Set<String> equippedIds = PersonaManager.localEquipped().stream()
            .map(cosmetic -> cosmetic.id)
            .collect(java.util.stream.Collectors.toSet());
        List<LoadedCosmetic> shown = PersonaManager.all().stream()
            .filter(cosmetic -> PersonaTypeNames.EQUIPPED.equals(selectedCosmeticType)
                ? equippedIds.contains(cosmetic.id)
                : PersonaTypeNames.belongsTo(cosmetic.type, selectedCosmeticType))
            .filter(cosmetic -> matchesSearch(cosmetic.displayName, cosmetic.id,
                PersonaTypeNames.displayName(cosmetic.type).getString()))
            .toList();
        visibleCosmeticCount = shown.size();
        int columns = Math.max(1, (rSkins.w - 18) / 65);
        if (displayedCosmeticColumns == columns && displayedCosmetics.equals(shown)) return;
        displayedCosmetics = List.copyOf(shown);
        displayedCosmeticColumns = columns;
        cosmeticGrid.clear();
        cosmeticGrid.setScrollAmount(0.0);
        addGridRows(shown, columns, cosmeticGrid::addCosmeticsRow);
    }

    private void fetchCapes() {
        String token = minecraft.getUser().getAccessToken();
        if (token == null || token.isEmpty() || "0".equals(token) || token.length() < 10) {
            capeFetchError = "Offline/invalid session. Log in to a Minecraft account.";
            return;
        }

        isFetchingCapes = true;
        capeFetchError = null;
        if (capeGrid != null) capeGrid.clear();

        CapeManager.fetchOwnedCapes(token).thenAccept(capes -> minecraft.execute(() -> {
            isFetchingCapes = false;
            ownedCapes = new ArrayList<>();
            ownedCapes.add(new MinecraftCape("none", "INACTIVE", "", "bedrockskins.capes.none"));
            ownedCapes.addAll(capes);
            
            boolean hasActive = false;
            for (MinecraftCape c : capes) {
                if (c.state.equals("ACTIVE")) {
                    hasActive = true;
                    break;
                }
            }
            if (!hasActive) {
                ownedCapes.set(0, new MinecraftCape("none", "ACTIVE", "", "bedrockskins.capes.none"));
            }

            autoSelectActiveCape();

            for (MinecraftCape cape : capes) {
                CapeManager.downloadAndRegisterCape(cape, () -> {
                    if (activeTab == AppearanceTab.CAPES) {
                        refreshCapeGrid();
                    }
                });
            }

            if (activeTab == AppearanceTab.CAPES) {
                refreshCapeGrid();
            }
        })).exceptionally(e -> {
            minecraft.execute(() -> {
                isFetchingCapes = false;
                capeFetchError = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                MOD_LOGGER.error("Failed to fetch Minecraft account capes", e);
            });
            return null;
        });
    }

    private void autoSelectActiveCape() {
        if (ownedCapes == null || previewPanel == null) return;
        MinecraftCape activeCape = null;
        Identifier accountOverride = SkinManager.getLocalAccountCapeOverride();
        if (accountOverride != null) {
            if (accountOverride.equals(SkinManager.CAPE_NONE)) {
                for (MinecraftCape c : ownedCapes) {
                    if ("none".equals(c.id)) {
                        activeCape = c;
                        break;
                    }
                }
            } else {
                for (MinecraftCape c : ownedCapes) {
                    if (c.textureIdentifier.equals(accountOverride)) {
                        activeCape = c;
                        break;
                    }
                }
            }
        } else {
            for (MinecraftCape c : ownedCapes) {
                if (c.state.equals("ACTIVE")) {
                    activeCape = c;
                    break;
                }
            }
        }
        if (activeCape != null) {
            previewPanel.setSelectedCape(activeCape);
        }
    }

    private void selectCapesCategory(String category) {
        this.selectedCapesCategory = category;
        refreshCapeGrid();
        if ("owned".equals(category)) {
            if (ownedCapes == null && !isFetchingCapes) {
                fetchCapes();
            } else if (ownedCapes != null) {
                autoSelectActiveCape();
            }
        }
    }

    private String getActiveLocalCapeId() {
        SkinId override = SkinManager.getLocalCapeOverride();
        if (override != null) {
            if (override.equals(SkinManager.CAPE_NONE_SKIN_ID)) {
                return "none";
            }
            var capeSkin = SkinPackLoader.getLoadedSkin(override);
            if (capeSkin != null && capeSkin.capeIdentifier != null) {
                return capeSkin.capeIdentifier.toString();
            }
        }
        SkinId equippedSkinId = SkinManager.getLocalSelectedKey();
        if (equippedSkinId != null) {
            var equippedSkin = SkinPackLoader.getLoadedSkin(equippedSkinId);
            if (equippedSkin != null && equippedSkin.capeIdentifier != null) {
                return equippedSkin.capeIdentifier.toString();
            }
        }
        return null;
    }

    private void refreshCapeGrid() {
        if (capeGrid == null) return;
        capeGrid.clear();
        capeGrid.setScrollAmount(0.0);

        List<MinecraftCape> capesToShow = new ArrayList<>();
        if ("owned".equals(selectedCapesCategory)) {
            if (ownedCapes != null) {
                capesToShow.addAll(ownedCapes);
            }
        } else if ("skinpack".equals(selectedCapesCategory)) {
            Set<String> uniqueCapePaths = new HashSet<>();
            String activeCapeId = getActiveLocalCapeId();
            
            boolean hasActive = false;
            capesToShow.add(new MinecraftCape("none", (activeCapeId == null || "none".equals(activeCapeId)) ? "ACTIVE" : "INACTIVE", "", "bedrockskins.capes.none"));

            for (List<LoadedSkin> skins : skinCache.values()) {
                for (LoadedSkin skin : skins) {
                    if (skin.cape != null) {
                        SkinPackLoader.registerTextureFor(skin.skinId);
                        if (skin.capeIdentifier != null) {
                            String pathStr = skin.capeIdentifier.toString();
                            if (uniqueCapePaths.add(pathStr)) {
                                String state = (activeCapeId != null && activeCapeId.equals(pathStr)) ? "ACTIVE" : "INACTIVE";
                                if (state.equals("ACTIVE")) hasActive = true;
                                
                                MinecraftCape cape = new MinecraftCape(
                                    "skinpack:" + skin.skinId.toString(),
                                    state,
                                    "",
                                    skin.safeSkinName,
                                    skin.capeIdentifier
                                );
                                capesToShow.add(cape);
                            }
                        }
                    }
                }
            }
            if (hasActive) {
                capesToShow.set(0, new MinecraftCape("none", "INACTIVE", "", "bedrockskins.capes.none"));
            }
        }

        capesToShow.removeIf(cape -> !matchesSearch(
            GuiSkinUtils.translatedOrFallback(cape.alias, cape.alias), cape.alias, cape.id));
        
        int cols = Math.max(1, (rSkins.w - 18) / 65);
        addGridRows(capesToShow, cols, capeGrid::addCapesRow);
    }

    public void onCapeChanged(String capeId) {
        if ("owned".equals(selectedCapesCategory)) {
            if (ownedCapes != null) {
                List<MinecraftCape> updated = new ArrayList<>();
                for (MinecraftCape cape : ownedCapes) {
                    String state = cape.id.equals(capeId) ? "ACTIVE" : "INACTIVE";
                    updated.add(new MinecraftCape(cape.id, state, cape.url, cape.alias));
                }
                ownedCapes = updated;
                refreshCapeGrid();
            }
        } else if ("skinpack".equals(selectedCapesCategory)) {
            refreshCapeGrid();
        }
    }

}

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
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import io.github.brandonitaly.bedrockskins.client.appearance.cape.CapeManager;
import io.github.brandonitaly.bedrockskins.client.appearance.cape.CapeManager.MinecraftCape;
import net.minecraft.resources.Identifier;

import java.io.File;
import java.util.*;

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
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private final TabManager tabManager = new TabManager(this::addRenderableWidget, this::removeWidget);
    private TabNavigationBar tabNavigationBar;
    
    private SkinPackListWidget packList;
    private SkinGridWidget skinGrid;
    private SkinPreviewPanel previewPanel;
    private final Screen parent; 

    private AppearanceTab activeTab = AppearanceTab.SKINS;
    private String selectedPackId;
    private final Map<String, List<LoadedSkin>> skinCache = new HashMap<>();

    private final Rect rPacks = new Rect(), rSkins = new Rect(), rPreview = new Rect();
    private final Rect rCosmeticCategories = new Rect(), rCosmeticOptions = new Rect();
    private Button openPacksButton, doneButton;
    private SpriteIconButton createPackButton;
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
    private EditBox cosmeticSearchBox;
    private EmoteGridWidget emoteGrid;
    private List<LoadedSkin> displayedSkins = List.of();
    private List<LoadedCosmetic> displayedCosmetics = List.of();
    private List<LoadedEmote> displayedEmotes = List.of();
    private int displayedSkinColumns = -1;
    private int displayedCosmeticColumns = -1;
    private int displayedEmoteColumns = -1;
    private int selectedEmoteSlot = 0;
    private String selectedCosmeticType = "all";
    private String selectedCapesCategory = "owned";
    private List<MinecraftCape> ownedCapes = null;
    private boolean isFetchingCapes = false;
    private String capeFetchError = null;
    private int visibleCosmeticCount;

    public AppearanceTab getActiveTab() { return activeTab; }
    public String getSelectedCapesCategory() { return selectedCapesCategory; }

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
        if (packList != null) packList.visible = isSkins;
        if (skinGrid != null) skinGrid.visible = isSkins;
        if (createPackButton != null) createPackButton.visible = isSkins;
        if (previewPanel != null) {
            previewPanel.reposition(rPreview.x, rPreview.y, rPreview.w, rPreview.h);
            previewPanel.updateButtonsForTab(activeTab);
        }
        positionCosmeticControls();

        boolean isCosmetics = activeTab == AppearanceTab.COSMETICS;
        if (cosmeticGrid != null) cosmeticGrid.visible = isCosmetics;
        if (cosmeticSidebar != null) cosmeticSidebar.visible = isCosmetics;
        if (isCosmetics) refreshCosmeticGrid();

        boolean isCapes = activeTab == AppearanceTab.CAPES;
        if (capeGrid != null) capeGrid.visible = isCapes;
        if (capeSidebar != null) capeSidebar.visible = isCapes;
        if (isCapes) {
            refreshCapeGrid();
            if ("owned".equals(selectedCapesCategory)) {
                if (ownedCapes == null && !isFetchingCapes) fetchCapes();
                else if (ownedCapes != null) autoSelectActiveCape();
            }
        }

        boolean isEmotes = activeTab == AppearanceTab.EMOTES;
        if (emoteGrid != null) emoteGrid.visible = isEmotes;
        if (emoteSidebar != null) emoteSidebar.visible = isEmotes;
        if (isEmotes) {
            refreshEmoteGrid();
            selectEmoteSlot(selectedEmoteSlot);
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
        rCosmeticCategories.set(rPacks.x, rPacks.y, rPacks.w, rPacks.h);
        rCosmeticOptions.set(rPacks.x, rPacks.y + rPacks.h, rPacks.w, 0);
    }

    private void initWidgets(ScreenRectangle tabArea) {
        int pHead = 24, pPad = 4;

        // Skins Widgets
        int plY = rPacks.y + pHead + pPad, plH = rPacks.h - pHead - (pPad * 2);
        if (packList == null) {
            packList = new SkinPackListWidget(minecraft, rPacks.w - pPad * 2, plH, plY, 28);
            addRenderableWidget(packList);
        }
        packList.setPosition(rPacks.x + pPad, plY);
        packList.setWidth(Math.max(10, rPacks.w - pPad * 2)); packList.setHeight(Math.max(10, plH));

        if (createPackButton == null) {
            createPackButton = SpriteIconButton.builder(Component.empty(), b -> {
                minecraft.gui.setScreen(new CreateSkinPackScreen(this));
            }, true).size(20, 20).sprite(BedrockSkinsSprites.ADDON_ICON, 16, 16).build();
            createPackButton.setTooltip(Tooltip.create(Component.translatable("bedrockskins.button.create_skin_pack")));
            addRenderableWidget(createPackButton);
        }
        createPackButton.setPosition(rPacks.x + rPacks.w - 22, rPacks.y + 2);


        int sgY = rSkins.y + pHead + pPad, sgH = rSkins.h - pHead - (pPad * 2);
        if (skinGrid == null) {
            skinGrid = new SkinGridWidget(minecraft, rSkins.w - pPad * 2, sgH, sgY, 90,
                    skin -> previewPanel.setSelectedSkin(skin), 
                    this::editSkin,
                    () -> previewPanel != null ? previewPanel.getSelectedSkin() : null, font);
            addRenderableWidget(skinGrid);
        }
        skinGrid.setPosition(rSkins.x + pPad, sgY);
        skinGrid.setWidth(Math.max(10, rSkins.w - pPad * 2)); skinGrid.setHeight(Math.max(10, sgH));

        // Cosmetics Widgets
        int cgY = rSkins.y + pHead + pPad;
        int cgH = rSkins.h - pHead - (pPad * 2);
        if (cosmeticSidebar == null) {
            cosmeticSidebar = new SidebarListWidget(minecraft, rPacks.w - pPad * 2, cgH, cgY, 28, font);
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
        cosmeticSidebar.setPosition(rPacks.x + pPad, cgY);
        cosmeticSidebar.setWidth(Math.max(10, rPacks.w - pPad * 2));
        cosmeticSidebar.setHeight(Math.max(10, cgH));
        cosmeticSidebar.visible = activeTab == AppearanceTab.COSMETICS;

        if (cosmeticGrid == null) {
            cosmeticGrid = new CosmeticGridWidget(minecraft, rSkins.w - pPad * 2, cgH, cgY, 90,
                this::selectCosmetic,
                () -> previewPanel != null ? previewPanel.getSelectedCosmetic() : null, font);
            addRenderableWidget(cosmeticGrid);
        }
        cosmeticGrid.setPosition(rSkins.x + pPad, cgY);
        cosmeticGrid.setWidth(Math.max(10, rSkins.w - pPad * 2));
        cosmeticGrid.setHeight(Math.max(10, cgH));
        cosmeticGrid.visible = activeTab == AppearanceTab.COSMETICS;

        if (cosmeticSearchBox == null) {
            cosmeticSearchBox = new EditBox(font, 0, 0, 120, 20,
                Component.translatable("bedrockskins.cosmetics.search"));
            cosmeticSearchBox.setHint(Component.translatable("bedrockskins.cosmetics.search"));
            cosmeticSearchBox.setMaxLength(64);
            cosmeticSearchBox.setResponder(ignored -> refreshCosmeticGrid());
            addRenderableWidget(cosmeticSearchBox);
        }
        int searchWidth = Math.min(120, Math.max(10, rSkins.w - pPad * 2));
        cosmeticSearchBox.setPosition(rSkins.right() - pPad - searchWidth, rSkins.y + 2);
        cosmeticSearchBox.setWidth(searchWidth);

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
            capeSidebar = new SidebarListWidget(minecraft, rPacks.w - pPad * 2, cgH, cgY, 28, font);
            capeSidebar.add(Component.translatable("bedrockskins.capes.owned"),
                () -> selectCapesCategory("owned"), () -> "owned".equals(selectedCapesCategory));
            capeSidebar.add(Component.translatable("bedrockskins.capes.skinpack"),
                () -> selectCapesCategory("skinpack"), () -> "skinpack".equals(selectedCapesCategory));
            addRenderableWidget(capeSidebar);
        }
        capeSidebar.setPosition(rPacks.x + pPad, cgY);
        capeSidebar.setWidth(Math.max(10, rPacks.w - pPad * 2));
        capeSidebar.setHeight(Math.max(10, cgH));
        capeSidebar.visible = activeTab == AppearanceTab.CAPES;

        if (capeGrid == null) {
            capeGrid = new CapeGridWidget(minecraft, rSkins.w - pPad * 2, cgH, cgY, 65,
                cape -> {
                    if (previewPanel != null) {
                        previewPanel.setSelectedCape(cape);
                        previewPanel.playCapeSelectionAnimation();
                    }
                },
                () -> previewPanel != null ? previewPanel.getSelectedCape() : null, font);
            addRenderableWidget(capeGrid);
        }
        capeGrid.setPosition(rSkins.x + pPad, cgY);
        capeGrid.setWidth(Math.max(10, rSkins.w - pPad * 2));
        capeGrid.setHeight(Math.max(10, cgH));
        capeGrid.visible = activeTab == AppearanceTab.CAPES;

        // Emotes Widgets
        if (emoteSidebar == null) {
            emoteSidebar = new SidebarListWidget(minecraft, rPacks.w - pPad * 2, cgH, cgY, 28, font);
            for (int slot = 0; slot < EmoteManager.SLOT_COUNT; slot++) {
                int slotIndex = slot;
                emoteSidebar.add(Component.translatable("bedrockskins.emotes.slot", slot + 1),
                    () -> selectEmoteSlot(slotIndex), () -> selectedEmoteSlot == slotIndex);
            }
            addRenderableWidget(emoteSidebar);
        }
        emoteSidebar.setPosition(rPacks.x + pPad, cgY);
        emoteSidebar.setWidth(Math.max(10, rPacks.w - pPad * 2));
        emoteSidebar.setHeight(Math.max(10, cgH));
        emoteSidebar.visible = activeTab == AppearanceTab.EMOTES;

        if (emoteGrid == null) {
            emoteGrid = new EmoteGridWidget(minecraft, rSkins.w - pPad * 2, cgH, cgY, 90,
                emote -> {
                    if (previewPanel != null) previewPanel.setSelectedEmote(emote);
                }, () -> previewPanel != null ? previewPanel.getSelectedEmote() : null, font);
            addRenderableWidget(emoteGrid);
        }
        emoteGrid.setPosition(rSkins.x + pPad, cgY);
        emoteGrid.setWidth(Math.max(10, rSkins.w - pPad * 2));
        emoteGrid.setHeight(Math.max(10, cgH));
        emoteGrid.visible = activeTab == AppearanceTab.EMOTES;

        updateCosmeticCustomizationLayout();
        updateCosmeticControls();
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
            packList.addEntryPublic(packList.new SkinPackEntry(
                pid, pid, pid,
                this::selectPack, this::editPack, () -> Objects.equals(selectedPackId, pid), font
            ));
        }

        if (selectedPackId == null && !sortedPacks.isEmpty()) selectPack(sortedPacks.getFirst());
        else if (selectedPackId != null) selectPack(selectedPackId); // Force re-render of grid
    }

    private void selectPack(String packId) {
        this.selectedPackId = packId;
        if (skinGrid != null) {
            List<LoadedSkin> skins = skinCache.getOrDefault(packId, List.of());
            int cols = Math.max(1, (rSkins.w - 18) / 65);
            if (displayedSkinColumns == cols && displayedSkins.equals(skins)) return;
            displayedSkins = List.copyOf(skins);
            displayedSkinColumns = cols;
            skinGrid.clear();
            skinGrid.setScrollAmount(0.0);
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
            return !(skins.getFirst().texture instanceof io.github.brandonitaly.bedrockskins.pack.model.AssetSource.Resource);
        }
        
        return true;
    }

    private void editPack(String packId) {
        if (isExternalPack(packId)) {
            minecraft.gui.setScreen(new EditSkinPackScreen(this, packId));
        }
    }

    private void editSkin(LoadedSkin skin) {
        if (MinecraftAccountSkin.is(skin) || ImportSkinAction.is(skin)) return;
        if (isExternalPack(skin.packId)) {
            minecraft.gui.setScreen(new EditSkinScreen(this, skin.packId, skin));
        }
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
                Component.translatable("bedrockskins.emotes.slots"), font);
            GuiUtils.drawPanelChrome(gui, rSkins.x, rSkins.y, rSkins.w, rSkins.h,
                Component.translatable("bedrockskins.gui.emotes"), font);
            if (EmoteManager.all().isEmpty()) {
                gui.centeredText(font, Component.translatable("bedrockskins.emotes.none"),
                    rSkins.x + rSkins.w / 2, rSkins.y + rSkins.h / 2, 0xFFAAAAAA);
            }
        }
            
        if (previewPanel != null) previewPanel.renderPreview(gui, mouseX);
        if (activeTab == AppearanceTab.COSMETICS
                && PersonaManager.isSideSelectable(previewPanel == null ? null : previewPanel.getSelectedCosmetic())) {
            gui.centeredText(font, selectedLimbSideLabel(), rPreview.x + rPreview.w / 2,
                rPreview.y + 34, 0xFFFFFFFF);
        }
        super.extractRenderState(gui, mouseX, mouseY, delta);
        if (previewPanel != null) previewPanel.renderSprites(gui);
        
        gui.blit(RenderPipelines.GUI_TEXTURED, Screen.FOOTER_SEPARATOR, 0, height - layout.getFooterHeight() - 2, 0.0F, 0.0F, width, 2, 32, 2);
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

    private void refreshEmoteGrid() {
        if (emoteGrid == null) return;
        List<LoadedEmote> emotes = EmoteManager.all();
        int columns = Math.max(1, (rSkins.w - 18) / 65);
        if (displayedEmoteColumns == columns && displayedEmotes.equals(emotes)) return;
        displayedEmotes = List.copyOf(emotes);
        displayedEmoteColumns = columns;
        emoteGrid.clear();
        emoteGrid.setScrollAmount(0.0);
        for (int i = 0; i < emotes.size(); i += columns) {
            emoteGrid.addEmotesRow(emotes.subList(i, Math.min(i + columns, emotes.size())));
        }
    }

    private void selectEmoteSlot(int slot) {
        selectedEmoteSlot = Math.max(0, Math.min(EmoteManager.SLOT_COUNT - 1, slot));
        LoadedEmote emote = EmoteManager.slot(selectedEmoteSlot);
        if (previewPanel != null) previewPanel.setSelectedEmote(emote);
    }

    public boolean isSelectedEmoteEquipped(LoadedEmote emote) {
        if (emote == null) return false;
        LoadedEmote equipped = EmoteManager.slot(selectedEmoteSlot);
        return equipped != null && equipped.id().equals(emote.id());
    }

    public void toggleSelectedEmote(LoadedEmote emote) {
        if (emote == null) return;
        if (isSelectedEmoteEquipped(emote)) {
            EmoteManager.unequip(selectedEmoteSlot);
        } else {
            EmoteManager.equip(selectedEmoteSlot, emote);
        }
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
        int sideY = rPreview.y + 28;
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
        if (cosmeticSearchBox != null) cosmeticSearchBox.visible = cosmeticsTab && !colorPickerOpen;
        if (cosmeticGrid != null) cosmeticGrid.visible = cosmeticsTab && !colorPickerOpen;
        boolean colorsVisible = cosmeticsTab && colorPickerOpen && colorUsable;
        if (colorPalette != null) {
            colorPalette.visible = colorsVisible;
            colorPalette.active = colorsVisible;
            colorPalette.setBounds(rCosmeticOptions.x + 4, customizationContentY(),
                rCosmeticOptions.w - 8, rCosmeticOptions.h - 32);
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
        int pad = 4;
        rCosmeticCategories.set(rPacks.x, rPacks.y, rPacks.w, rPacks.h);
        rCosmeticOptions.set(rSkins.x, rSkins.y, rSkins.w, colors ? rSkins.h : 0);
        int listY = rCosmeticCategories.y + 28;
        cosmeticSidebar.setPosition(rCosmeticCategories.x + pad, listY);
        cosmeticSidebar.setWidth(Math.max(10, rCosmeticCategories.w - pad * 2));
        cosmeticSidebar.setHeight(Math.max(10, rCosmeticCategories.h - 32));
    }

    private int customizationContentY() {
        return rCosmeticOptions.y + 28;
    }

    private void refreshCosmeticGrid() {
        if (cosmeticGrid == null) return;
        String query = cosmeticSearchBox == null ? "" : cosmeticSearchBox.getValue().strip().toLowerCase(Locale.ROOT);
        Set<String> equippedIds = PersonaManager.localEquipped().stream()
            .map(cosmetic -> cosmetic.id)
            .collect(java.util.stream.Collectors.toSet());
        List<LoadedCosmetic> shown = PersonaManager.all().stream()
            .filter(cosmetic -> PersonaTypeNames.EQUIPPED.equals(selectedCosmeticType)
                ? equippedIds.contains(cosmetic.id)
                : PersonaTypeNames.belongsTo(cosmetic.type, selectedCosmeticType))
            .filter(cosmetic -> query.isEmpty()
                || cosmetic.displayName.toLowerCase(Locale.ROOT).contains(query)
                || cosmetic.id.toLowerCase(Locale.ROOT).contains(query)
                || PersonaTypeNames.displayName(cosmetic.type).getString().toLowerCase(Locale.ROOT).contains(query))
            .toList();
        visibleCosmeticCount = shown.size();
        int columns = Math.max(1, (rSkins.w - 18) / 65);
        if (displayedCosmeticColumns == columns && displayedCosmetics.equals(shown)) return;
        displayedCosmetics = List.copyOf(shown);
        displayedCosmeticColumns = columns;
        cosmeticGrid.clear();
        cosmeticGrid.setScrollAmount(0.0);
        for (int i = 0; i < shown.size(); i += columns) {
            cosmeticGrid.addCosmeticsRow(shown.subList(i, Math.min(i + columns, shown.size())));
        }
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
        
        int cols = Math.max(1, (rSkins.w - 18) / 65);
        for (int i = 0; i < capesToShow.size(); i += cols) {
            capeGrid.addCapesRow(capesToShow.subList(i, Math.min(i + cols, capesToShow.size())));
        }
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

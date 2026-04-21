package com.brandonitaly.bedrockskins.client.gui.legacy;

//? if <26.0 {
/*import com.brandonitaly.bedrockskins.client.BedrockSkinsClient;
import com.brandonitaly.bedrockskins.client.ClientSkinSync;
import com.brandonitaly.bedrockskins.client.BedrockSkinsConfig;
import com.brandonitaly.bedrockskins.client.FavoritesManager;
import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.client.StateManager;
import com.brandonitaly.bedrockskins.client.gui.GuiSkinUtils;
import com.brandonitaly.bedrockskins.util.BedrockSkinsSprites;
import com.brandonitaly.bedrockskins.util.ExternalAssetUtil;
import com.brandonitaly.bedrockskins.util.PackSortUtil;
import com.brandonitaly.bedrockskins.pack.AssetSource;
import com.brandonitaly.bedrockskins.pack.LoadedSkin;
import com.brandonitaly.bedrockskins.pack.SkinId;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import wily.legacy.client.ControlType;
import wily.legacy.client.controller.BindingState;
import wily.legacy.client.controller.Controller;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.client.screen.LegacyScrollRenderer;
import wily.legacy.client.screen.Panel;
import wily.legacy.client.screen.PanelVListScreen;
import wily.legacy.client.screen.ScrollableRenderer;
import wily.legacy.init.LegacyRegistries;
import wily.legacy.util.client.LegacyRenderUtil;
import net.minecraft.client.renderer.RenderPipelines;

import java.util.*;

public class Legacy4JChangeSkinScreen extends PanelVListScreen implements Controller.Event, ControlTooltip.Event {
    private static final String STANDARD_PACK_ID = "skinpack.Standard";
    private static final String FAVORITES_PACK_ID = "skinpack.Favorites";

    protected final Minecraft minecraft = Minecraft.getInstance();
    protected final Panel tooltipBox = Panel.tooltipBoxOf(panel, 400);
    protected final ScrollableRenderer scrollableRenderer = new ScrollableRenderer(new LegacyScrollRenderer());
    
    private String focusedPackId;
    private SkinPackAdapter focusedPack;
    private PlayerSkinWidgetList playerSkinWidgetList;
    private final Map<String, Button> packButtons = new LinkedHashMap<>();
    private final Map<String, SkinPackAdapter> allPacks = new HashMap<>();
    
    private final Map<String, Identifier> dynamicIconCache = new HashMap<>();
    private Identifier createId(String ns, String path) { return Identifier.fromNamespaceAndPath(ns, path); }
    
    private boolean queuedChangeSkinPack = false;
    private boolean hasScrolledToInitial = false;
    private boolean isDraggingPreview = false;
    private double lastMouseX = 0;

    public Legacy4JChangeSkinScreen(Screen parent) {
        super(parent, 180, 290, Component.translatable("bedrockskins.gui.title"));
        renderableVList.layoutSpacing(l -> 0);
        
        allPacks.putAll(SkinPackAdapter.getAllPacks());
        injectAutoSelectedIntoStandardPack();
        rebuildFavoritesPack();
        
        List<String> sortedPackIds = new ArrayList<>(allPacks.keySet());
        sortedPackIds.sort(buildPackComparator());
        
        sortedPackIds.remove(FAVORITES_PACK_ID);
        sortedPackIds.add(1, FAVORITES_PACK_ID);
        
        for (String packId : sortedPackIds) {
            SkinPackAdapter pack = allPacks.get(packId);
            if (pack == null || (!FAVORITES_PACK_ID.equals(packId) && pack.isEmpty())) continue;
            
            Button button = Button.builder(Component.literal(resolvePackDisplayName(packId, pack)), b -> {
                if (!packId.equals(focusedPackId)) {
                    this.focusedPackId = packId;
                    this.focusedPack = allPacks.get(packId);
                    this.queuedChangeSkinPack = true;
                }
            }).pos(0, 0).size(260, 20).build();
            
            packButtons.put(packId, button);
            renderableVList.addRenderable(button);
        }
        
        openToCurrentSkin();
    }

    private Comparator<String> buildPackComparator() {
        return PackSortUtil.buildPackComparator(BedrockSkinsConfig.getPackSortOrder(), this::resolveSortDisplayName);
    }

    private String resolveSortDisplayName(String packId) {
        SkinPackAdapter pack = allPacks.get(packId);
        return resolvePackDisplayName(packId, pack);
    }

    private String resolvePackDisplayName(String packId, SkinPackAdapter pack) {
        LoadedSkin firstSkin = (pack != null && !pack.isEmpty()) ? pack.getSkin(0) : null;
        return GuiSkinUtils.getPackDisplayName(packId, firstSkin);
    }

    private void rebuildFavoritesPack() {
        List<LoadedSkin> favs = FavoritesManager.getFavoriteKeys().stream()
            .map(SkinId::parse).filter(Objects::nonNull)
            .map(id -> {
                LoadedSkin s = SkinPackLoader.getLoadedSkin(id);
                return (s == null && GuiSkinUtils.isAutoSelectedSkinId(id)) ? resolveAutoSelectedSkinForFavorites() : s;
            }).filter(Objects::nonNull).toList();
            
        allPacks.put(FAVORITES_PACK_ID, new SkinPackAdapter(FAVORITES_PACK_ID, favs));
    }

    private void injectAutoSelectedIntoStandardPack() {
        SkinPackAdapter standardPack = allPacks.get(STANDARD_PACK_ID);
        if (standardPack == null || standardPack.isEmpty()) return;

        List<LoadedSkin> merged = GuiSkinUtils.withAutoSelectedStandardFirst(standardPack.skins());
        allPacks.put(STANDARD_PACK_ID, new SkinPackAdapter(STANDARD_PACK_ID, merged, standardPack.packType()));
    }

    private LoadedSkin resolveAutoSelectedSkinForFavorites() {
        SkinPackAdapter standardPack = getPackForUi(STANDARD_PACK_ID);
        return standardPack == null ? null : GuiSkinUtils.resolveAutoSelectedFromStandard(standardPack.skins());
    }

    private SkinPackAdapter getPackForUi(String packId) {
        if (STANDARD_PACK_ID.equals(packId)) injectAutoSelectedIntoStandardPack();
        return allPacks.getOrDefault(packId, SkinPackAdapter.getPack(packId));
    }

    private boolean hasSelectedSkinWidget() {
        return playerSkinWidgetList != null && playerSkinWidgetList.element3 != null;
    }

    private void selectSkin() {
        if (!hasSelectedSkinWidget()) return;
        LoadedSkin skin = playerSkinWidgetList.element3.getCurrentSkin();
        if (skin == null) return;
        if (BedrockSkinsClient.blockUnfairSkins && skin.unfair) return;

        try {
            GuiSkinUtils.applySelectedSkin(minecraft, skin);
            playUISound();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void resetSkin() {
        GuiSkinUtils.resetSelectedSkin(minecraft);
        playUISound();
    }

    private void openLegacySkinMenu() {
        minecraft.setScreen(new wily.legacy.client.screen.OptionsScreen(this, (wily.legacy.client.screen.OptionsScreen.Section) wily.legacy.client.screen.HelpAndOptionsScreen.CHANGE_SKIN));
        playUISound();
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int keyCode = InputConstants.getKey(event).getValue();

        boolean isUp = keyCode == InputConstants.KEY_UP || keyCode == InputConstants.KEY_W;
        boolean isDown = keyCode == InputConstants.KEY_DOWN || keyCode == InputConstants.KEY_S;
        boolean isLeft = keyCode == InputConstants.KEY_LEFT || keyCode == InputConstants.KEY_A;
        boolean isRight = keyCode == InputConstants.KEY_RIGHT || keyCode == InputConstants.KEY_D;

        if (focusedPackId != null && !allPacks.isEmpty()) {
            List<String> keys = packButtons.keySet().stream().toList();
            int currentIndex = keys.indexOf(focusedPackId);
            if ((isUp || isDown) && currentIndex != -1) {
                int newIndex = isUp ? (currentIndex > 0 ? currentIndex - 1 : keys.size() - 1) 
                                    : (currentIndex < keys.size() - 1 ? currentIndex + 1 : 0);
                
                focusedPackId = keys.get(newIndex);
                focusedPack = allPacks.get(focusedPackId);
                queuedChangeSkinPack = true;
                
                Button btn = packButtons.get(focusedPackId);
                if (btn != null) {
                    setFocused(btn);
                    ensureButtonVisible(btn);
                }
                playFocusSound();
                return true;
            }
        }

        if (hasSelectedSkinWidget()) {
            Button focused = packButtons.get(focusedPackId);
            if (focused != null) setFocused(focused);
        }

        if (keyCode == InputConstants.KEY_RETURN) { selectSkin(); return true; }
        if (keyCode == InputConstants.KEY_F) { favorite(); return true; }
        if (keyCode == InputConstants.KEY_H) { openLegacySkinMenu(); return true; }
        if (control(keyCode == InputConstants.KEY_LBRACKET, keyCode == InputConstants.KEY_RBRACKET)) return true;
        if (control(isLeft, isRight)) return true;
        if (handlePoseChange(keyCode == InputConstants.KEY_RSHIFT, keyCode == InputConstants.KEY_LSHIFT)) return true;

        return super.keyPressed(event);
    }

    private void ensureButtonVisible(Button button) {
        if (!packButtons.containsValue(button)) return;
        int scrolls = 0;
        while (!children().contains(button) && scrolls++ < 100) renderableVList.mouseScrolled(true);
        scrolls = 0;
        while (!children().contains(button) && scrolls++ < 200) renderableVList.mouseScrolled(false);
    }

    private boolean isInBounds(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    boolean handlePoseChange(boolean left, boolean right) {
        if (!(left || right) || !hasSelectedSkinWidget() || playerSkinWidgetList.element3.isInterpolating()) return false;
        playerSkinWidgetList.element3.cyclePose(right);
        return true;
    }

    boolean control(boolean left, boolean right) {
        if ((left || right) && playerSkinWidgetList != null) {
            if (playerSkinWidgetList.widgets.stream().anyMatch(a -> a.progress <= 1)) return true;
            playerSkinWidgetList.sortForIndex(playerSkinWidgetList.index + (left ? -1 : 1));
            playScrollSound();
            return true;
        }
        return false;
    }

    @Override
    public void bindingStateTick(BindingState state) {
        if (state.is(ControllerBinding.UP_BUTTON) && state.released) favorite();
        if (state.is(ControllerBinding.BACK) && state.released) { openLegacySkinMenu(); return; }
        if (state.is(ControllerBinding.RIGHT_STICK_DOWN) && state.justPressed) { if (handlePoseChange(false, true)) return; }
        else if (state.is(ControllerBinding.RIGHT_STICK_UP) && state.justPressed) { if (handlePoseChange(true, false)) return; }
        
        if (hasSelectedSkinWidget() && state.is(ControllerBinding.RIGHT_STICK) && state instanceof BindingState.Axis stick) {
            double deltaX = stick.getDeadZone() > Math.abs(stick.x) ? 0 : -(double)stick.x * 0.15d;
            if (Math.abs(deltaX) > 0.01) playerSkinWidgetList.element3.onDrag(deltaX);
            state.block();
        }
    }
    
    private void favorite() {
        if (!hasSelectedSkinWidget()) return;
        LoadedSkin skin = playerSkinWidgetList.element3.getCurrentSkin();
        if (skin == null) return;

        boolean wasFavorite = FavoritesManager.isFavorite(skin);
        int oldIndex = playerSkinWidgetList.index;
        
        if (wasFavorite) FavoritesManager.removeFavorite(skin);
        else FavoritesManager.addFavorite(skin);
        
        rebuildFavoritesPack();

        if (FAVORITES_PACK_ID.equals(focusedPackId)) {
            focusedPack = allPacks.get(FAVORITES_PACK_ID);
            int favCount = focusedPack.size();
            updateSkinPack(!wasFavorite ? favCount - 1 : (favCount == 0 ? 0 : Math.min(oldIndex, favCount - 1)));
        }
        playUISound();
    }

    @Override
    public void addControlTooltips(ControlTooltip.Renderer renderer) {
        boolean isKbm = ControlType.getActiveType().isKbm();
        
        // Select
        renderer.add(() -> isKbm ? ControlTooltip.getKeyIcon(InputConstants.KEY_RETURN) : ControllerBinding.DOWN_BUTTON.bindingState.getIcon(), () -> Component.translatable("bedrockskins.button.select"));
        
        // Cancel
        renderer.add(() -> isKbm ? ControlTooltip.getKeyIcon(InputConstants.KEY_ESCAPE) : ControllerBinding.RIGHT_BUTTON.bindingState.getIcon(), () -> Component.translatable("gui.cancel"));
        
        // Favorite/Unfavorite
        renderer.add(() -> isKbm ? ControlTooltip.getKeyIcon(InputConstants.KEY_F) : ControllerBinding.UP_BUTTON.bindingState.getIcon(), () -> {
            if (hasSelectedSkinWidget()) {
                LoadedSkin s = playerSkinWidgetList.element3.getCurrentSkin();
                if (s != null) return Component.translatable(FavoritesManager.isFavorite(s) ? "bedrockskins.button.unfavorite" : "bedrockskins.button.favorite");
            }
            return Component.translatable("bedrockskins.button.favorite");
        });

        // Compound left/right navigation icon
        renderer.add(() -> isKbm ? ControlTooltip.COMPOUND_ICON_FUNCTION.apply(new ControlTooltip.Icon[]{ControlTooltip.getKeyIcon(InputConstants.KEY_LEFT), ControlTooltip.SPACE_ICON, ControlTooltip.getKeyIcon(InputConstants.KEY_RIGHT)}) : ControllerBinding.LEFT_STICK.bindingState.getIcon(), () -> Component.translatable("bedrockskins.menu.navigate"));

        // Open Skin Customization
        renderer.add(() -> isKbm ? ControlTooltip.getKeyIcon(InputConstants.KEY_H) : ControllerBinding.BACK.bindingState.getIcon(), () -> Component.translatable("options.skinCustomisation.title"));
    }

    @Override
    public void renderDefaultBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        LegacyRenderUtil.renderDefaultBackground(wily.factoryapi.base.client.UIAccessor.of(this), guiGraphics, false);

        if (queuedChangeSkinPack) {
            queuedChangeSkinPack = false;
            updateSkinPack();
        }
        
        // Render background panels
        renderBackgroundPanels(guiGraphics);
        // Render pack name
        if (focusedPackId != null) renderPackName(guiGraphics);
        // Render skin name and info
        if (hasSelectedSkinWidget()) renderSkinInfo(guiGraphics);
    }
    
    private void renderBackgroundPanels(GuiGraphicsExtractor guiGraphics) {
        int tx = tooltipBox.getX(), ty = panel.getY(), tw = tooltipBox.getWidth(), th = tooltipBox.getHeight();
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, BedrockSkinsSprites.SKIN_PANEL, tx - 10, ty + 7, tw, th - 2);
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, BedrockSkinsSprites.PANEL_FILLER, tx - 5, ty + th - 64, tw - 14, 60);
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, BedrockSkinsSprites.SQUARE_RECESSED_PANEL , tx - 1, ty + th - 59, tw - 55, 55);
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, BedrockSkinsSprites.ICON_HOLDER, tx + tw - 50, ty + th - 57, 24, 24);
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, BedrockSkinsSprites.ICON_HOLDER, tx + tw - 50, ty + th - 30, 24, 24);
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, BedrockSkinsSprites.PACK_NAME_BOX, tx - 5, ty + 20, tw - 18, 40);
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, BedrockSkinsSprites.SKIN_BOX, tx - 5, ty + 16, tw - 14, th - 80);
    }

    private Identifier resolveFocusedPackIconTexture() {
        if (FAVORITES_PACK_ID.equals(focusedPackId)) return createId("bedrockskins", "skin_packs/vanilla/pack_icon.png");

        if (focusedPack != null && !focusedPack.isEmpty()) {
            LoadedSkin firstSkin = focusedPack.getSkin(0);
            if (firstSkin != null) {
                AssetSource src = firstSkin.texture;
                if (src instanceof AssetSource.Resource res) {
                    String path = res.id().getPath();
                    int lastSlash = path.lastIndexOf('/');
                    if (lastSlash != -1) {
                        var packIcon = createId(res.id().getNamespace(), path.substring(0, lastSlash) + "/pack_icon.png");
                        if (minecraft.getResourceManager().getResource(packIcon).isPresent()) return packIcon;
                    }
                } else {
                    var cached = dynamicIconCache.get(focusedPackId);
                    if (dynamicIconCache.containsKey(focusedPackId)) {
                        return cached != null ? cached : createId("bedrockskins", "skin_packs/vanilla/pack_icon.png");
                    }
                    NativeImage img = ExternalAssetUtil.loadPackIcon(src);
                    if (img != null) {
                        var dynamicId = createId("bedrockskins", "dynamic_icon_" + UUID.randomUUID().toString().replace("-", ""));
                        minecraft.getTextureManager().register(dynamicId, new net.minecraft.client.renderer.texture.DynamicTexture(() -> "dynamic_icon", img));
                        dynamicIconCache.put(focusedPackId, dynamicId);
                        return dynamicId;
                    }
                    dynamicIconCache.put(focusedPackId, null);
                }
            }
        }
        return createId("bedrockskins", "skin_packs/vanilla/pack_icon.png");
    }
    
    private void renderPackName(GuiGraphicsExtractor guiGraphics) {
        int middle = tooltipBox.getX() - 5 + (tooltipBox.getWidth() - 18) / 2;
        String packDisplayName = resolvePackDisplayName(focusedPackId, focusedPack);

        drawScaledCenteredString(guiGraphics, Component.literal(packDisplayName), middle, panel.getY() + 27, 1.5f, 0xffffffff);

        // Draw subtitle below pack name
        if (focusedPack != null && focusedPack.packType() != null && !focusedPack.packType().isEmpty()) {
            drawScaledCenteredString(guiGraphics, Component.translatable("bedrockskins.packType." + focusedPack.packType()), middle, panel.getY() + 45, 1.0f, 0xffffffff);
        }
    }
    
    private void renderSkinInfo(GuiGraphicsExtractor guiGraphics) {
        LoadedSkin skin = playerSkinWidgetList.element3.getCurrentSkin();
        if (skin == null) return;
        
        int middle = tooltipBox.getX() - 5 + (tooltipBox.getWidth() - 18) / 2;
        Component skinNameComp = GuiSkinUtils.getSkinDisplayName(skin);

        drawScaledCenteredString(guiGraphics, skinNameComp, middle, panel.getY() + tooltipBox.getHeight() - 49, 1.5f, 0xffffffff);

        // Render description if available
        String desc = GuiSkinUtils.getSkinDescriptionText(skin).orElse(null);
        if (desc != null) {
            drawScaledCenteredString(guiGraphics, Component.literal(desc), middle, panel.getY() + tooltipBox.getHeight() - 24, 1.5f, 0xffffffff);
        }
        
        int holderX = tooltipBox.getX() + tooltipBox.getWidth() - 50;
        int holderY = panel.getY() + tooltipBox.getHeight() - 57;

        Identifier iconSprite = null;

        // Determine which sprite to draw
        if (BedrockSkinsClient.blockUnfairSkins && skin.unfair) {
            iconSprite = BedrockSkinsSprites.SKIN_DENY;
        } else if (GuiSkinUtils.isSkinCurrentlyEquipped(skin)) {
            iconSprite = BedrockSkinsSprites.BEACON_CONFIRM;
        }

        // Draw sprite centered (20x20 inside the 24x24 holder requires a +2 offset)
        if (iconSprite != null) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, iconSprite, holderX + 2, holderY + 2, 20, 20);
        }
        
        // Render heart if this skin is favorited
        if (FavoritesManager.isFavorite(skin)) {
            int hx = tooltipBox.getX() + tooltipBox.getWidth() - 46, hy = panel.getY() + tooltipBox.getHeight() - 26;
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, BedrockSkinsSprites.HEART_CONTAINER, hx, hy, 16, 16);
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, BedrockSkinsSprites.HEART_FULL, hx, hy, 16, 16);
        }
    }

    private void drawScaledCenteredString(GuiGraphicsExtractor guiGraphics, Component text, int x, int y, float scale, int color) {
        var stack = guiGraphics.pose();
        stack.pushMatrix();
        stack.translate(x, y);
        stack.scale(scale, scale);
        guiGraphics.centeredText(minecraft.font, text, 0, 0, color);
        stack.popMatrix();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (playerSkinWidgetList != null && isInBounds(mouseX, mouseY, tooltipBox.getX() - 5, panel.getY() + 16, tooltipBox.getWidth() - 14, tooltipBox.getHeight() - 80)) {
            control(scrollY > 0, scrollY < 0);
            return true;
        }

        if ((!ControlType.getActiveType().isKbm() || isInBounds(mouseX, mouseY, tooltipBox.getX(), panel.getY(), tooltipBox.getWidth(), tooltipBox.getHeight())) && scrollableRenderer.mouseScrolled(scrollY)) {
            return true;
        }
        
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void renderableVListInit() {
        addRenderableOnly((guiGraphics, i, j, f) -> {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, BedrockSkinsSprites.SQUARE_RECESSED_PANEL, panel.getX() + 7, panel.getY() + 7 + 130 - 8, panel.getWidth() - 14, panel.getHeight() - 14 - 135 + 1 + 8);
            
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, BedrockSkinsSprites.SQUARE_RECESSED_PANEL, panel.getX() + 34, panel.getY() + 10, 112, 112);
            
            if (focusedPack != null) {
                var icon = resolveFocusedPackIconTexture();
                if (icon != null) {
                    int[] d = new int[]{128, 128};
                    var pose = guiGraphics.pose();
                    pose.pushMatrix();
                    pose.translate(panel.getX() + 35.4f, panel.getY() + 11.4f);
                    pose.scale(109f / d[0], 109f / d[1]);
                    guiGraphics.blit(RenderPipelines.GUI_TEXTURED, icon, 0, 0, 0, 0, d[0], d[1], d[0], d[1]);
                    pose.popMatrix();
                }
            }
        });

        tooltipBox.init("tooltipBox");
        getRenderableVList().init("renderableVList", panel.getX() + 11, panel.getY() + 132, panel.getWidth() - 22, panel.getHeight() - 147);
    }

    void openToCurrentSkin() {
        injectAutoSelectedIntoStandardPack();
        SkinId currentSkinKey = SkinManager.getLocalSelectedKey();
        
        if (currentSkinKey == null || SkinPackLoader.getLoadedSkin(currentSkinKey) == null) {
            String targetPackId = getPackForUi(STANDARD_PACK_ID) != null ? STANDARD_PACK_ID : (allPacks.isEmpty() ? null : allPacks.keySet().iterator().next());
            if (targetPackId != null) {
                focusedPackId = targetPackId;
                focusedPack = getPackForUi(targetPackId);
                queuedChangeSkinPack = true;
                updateSkinPack(0);
            }
            return;
        }

        LoadedSkin currentSkin = SkinPackLoader.getLoadedSkin(currentSkinKey);
        SkinPackAdapter pack = getPackForUi(currentSkin.packId);
        if (pack != null) {
            focusedPackId = currentSkin.packId;
            focusedPack = pack;
            queuedChangeSkinPack = true;
            updateSkinPack(pack.indexOf(currentSkin));
            if (packButtons.containsKey(focusedPackId)) setFocused(packButtons.get(focusedPackId));
        }
    }

    void updateSkinPack() { updateSkinPack(0); }
    
    private void scrollToFocusedPack() {
        if (focusedPackId != null && packButtons.containsKey(focusedPackId)) ensureButtonVisible(packButtons.get(focusedPackId));
    }
    
    void updateSkinPack(int index) {
        queuedChangeSkinPack = false;
        if (focusedPackId != null && packButtons.containsKey(focusedPackId)) setFocused(packButtons.get(focusedPackId));

        // Capture the current pose and rotation before clearing the old widgets
        PlayerSkinWidget.PreviewPose savedPose = PlayerSkinWidget.PreviewPose.STANDING;
        float savedRotX = 0f, savedRotY = 0f;
        if (hasSelectedSkinWidget()) {
            savedPose = playerSkinWidgetList.element3.getPreviewPose();
            savedRotX = playerSkinWidgetList.element3.getRotationX();
            savedRotY = playerSkinWidgetList.element3.getRotationY();
        }

        if (playerSkinWidgetList != null) playerSkinWidgetList.widgets.forEach(w -> { w.cleanup(); removeWidget(w); });

        if (focusedPack == null || focusedPack.isEmpty()) {
            playerSkinWidgetList = null;
            return;
        }

        int boxX = tooltipBox.getX() - 5, boxY = panel.getY() + 16, boxW = tooltipBox.getWidth() - 14, boxH = tooltipBox.getHeight() - 80;
        addRenderableOnly((guiGraphics, i, j, f) -> {
            if (playerSkinWidgetList != null)
                guiGraphics.enableScissor(boxX + 7, boxY + 4, boxX + boxW - 5, boxY + boxH - 4);
        });

        List<PlayerSkinWidget> widgets = new ArrayList<>();
        for (int i = 0; i < focusedPack.size(); i++) {
            final LoadedSkin skin = focusedPack.getSkin(i);
            final SkinReference ref = (FAVORITES_PACK_ID.equals(focusedPackId) && skin != null) 
                ? new SkinReference(skin.packId, SkinPackAdapter.getPack(skin.packId).indexOf(skin)) : new SkinReference(focusedPackId, i);
            widgets.add(addRenderableWidget(new PlayerSkinWidget(130, 160, minecraft.getEntityModels(), () -> ref, () -> skin)));
        }

        playerSkinWidgetList = PlayerSkinWidgetList.of(boxX + boxW / 2 - 65, boxY + boxH / 2 - 55, widgets.toArray(new PlayerSkinWidget[0]));
        int targetIndex = ((index % widgets.size()) + widgets.size()) % widgets.size();
        
        playerSkinWidgetList.index = targetIndex; 
        playerSkinWidgetList.element3 = widgets.get(targetIndex); 
        playerSkinWidgetList.element3.setPreviewPose(savedPose);
        playerSkinWidgetList.sortForIndex(targetIndex, savedRotX, savedRotY);

        addRenderableOnly((guiGraphics, i, j, f) -> {
            if (playerSkinWidgetList != null) guiGraphics.disableScissor();
        });
    }

    @Override
    protected void init() {
        super.init();
        
        if (queuedChangeSkinPack) {
            updateSkinPack(0);
        } else if (hasSelectedSkinWidget()) {
            updateSkinPack(playerSkinWidgetList.index);
        } else if (focusedPackId != null) {
            updateSkinPack(0);
        }
        
        if (!hasScrolledToInitial) { 
            scrollToFocusedPack(); 
            hasScrolledToInitial = true; 
        }
    }
    
    private void playUISound() { minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f)); }
    private void playScrollSound() { minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(LegacyRegistries.SCROLL.get(), 1.0f)); }
    private void playFocusSound() { minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(LegacyRegistries.FOCUS.get(), 1.0f)); }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubled) {
        if (event.button() == 0 && playerSkinWidgetList != null) {
            double mouseX = event.x(), mouseY = event.y();

            // Check if clicking the action icons
            if (hasSelectedSkinWidget()) {
                int iconX = tooltipBox.getX() + tooltipBox.getWidth() - 50, ty = panel.getY() + tooltipBox.getHeight();
                
                // Clicked Select/Checkmark icon holder
                if (isInBounds(mouseX, mouseY, iconX, ty - 57, 24, 24)) {
                    selectSkin();
                    return true;
                }
                // Clicked Favorite/Heart icon holder
                if (isInBounds(mouseX, mouseY, iconX, ty - 30, 24, 24)) {
                    favorite();
                    return true;
                }
            }

            // Calculate the visible scissor bounds
            int boxX = tooltipBox.getX() - 5, boxY = panel.getY() + 16, boxW = tooltipBox.getWidth() - 14, boxH = tooltipBox.getHeight() - 80;
            if (mouseX >= boxX + 7 && mouseX <= boxX + boxW - 5 && mouseY >= boxY + 4 && mouseY <= boxY + boxH - 4) {
                if (hasSelectedSkinWidget() && isInBounds(mouseX, mouseY, playerSkinWidgetList.element3.getX(), playerSkinWidgetList.element3.getY(), playerSkinWidgetList.element3.getWidth(), playerSkinWidgetList.element3.getHeight())) {
                    isDraggingPreview = true;
                    lastMouseX = mouseX;
                    return true;
                }

                if (playerSkinWidgetList != null && playerSkinWidgetList.widgets.stream().noneMatch(w -> w.progress <= 1)) {
                    for (int i = 0; i < playerSkinWidgetList.widgets.size(); i++) {
                        PlayerSkinWidget w = playerSkinWidgetList.widgets.get(i);
                        if (w.visible && w != playerSkinWidgetList.element3 && isInBounds(mouseX, mouseY, w.getX(), w.getY(), w.getWidth(), w.getHeight())) {
                            playScrollSound();
                            playerSkinWidgetList.sortForIndex(i);
                            return true;
                        }
                    }
                }
            }
        }
        return super.mouseClicked(event, doubled);
    }

    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        if (event.button() == 0 && isDraggingPreview) { isDraggingPreview = false; return true; }
        return super.mouseReleased(event);
    }

    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double deltaX, double deltaY) {
        if (isDraggingPreview && hasSelectedSkinWidget()) {
            double delta = lastMouseX - event.x();
            if (Math.abs(delta) > 0.01) playerSkinWidgetList.element3.onDrag(delta);
            lastMouseX = event.x();
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public void onClose() {
        if (playerSkinWidgetList != null) playerSkinWidgetList.widgets.forEach(PlayerSkinWidget::cleanup);
        
        for (Identifier id : dynamicIconCache.values()) {
            if (id != null) minecraft.getTextureManager().release(id);
        }

        dynamicIconCache.clear();
        super.onClose();
    }
}
*///?}
package com.brandonitaly.bedrockskins.client.gui.legacy;

//? if legacy4j {
/*
import com.brandonitaly.bedrockskins.client.ClientSkinSync;
import com.brandonitaly.bedrockskins.client.FavoritesManager;
import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.client.StateManager;
import com.brandonitaly.bedrockskins.client.gui.BedrockSkinsSprites;
import com.brandonitaly.bedrockskins.pack.AssetSource;
import com.brandonitaly.bedrockskins.pack.LoadedSkin;
import com.brandonitaly.bedrockskins.pack.SkinId;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import com.mojang.blaze3d.platform.InputConstants;
//? if <1.21.11 {
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
//?} else {
import net.minecraft.resources.Identifier;
//?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
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
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyRenderUtil;
import net.minecraft.client.renderer.RenderPipelines;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Legacy4JChangeSkinScreen extends PanelVListScreen implements Controller.Event, ControlTooltip.Event {
    private static final String STANDARD_PACK_ID = "skinpack.Standard";
    private static final String FAVORITES_PACK_ID = "skinpack.Favorites";
    private static final String AUTO_SELECTED_TRANSLATION_KEY = "bedrockskins.skin.auto_selected";
    private static final String AUTO_SELECTED_INTERNAL_NAME = "__auto_selected__";

    protected final Minecraft minecraft;
    protected final Panel tooltipBox = Panel.tooltipBoxOf(panel, 400);
    protected ScrollableRenderer scrollableRenderer = new ScrollableRenderer(new LegacyScrollRenderer());
    
    private String focusedPackId;
    private SkinPackAdapter focusedPack;
    private List<String> sortedPackIds = new ArrayList<>();
    private PlayerSkinWidgetList playerSkinWidgetList;
    private final Map<String, Button> packButtons = new HashMap<>();
    private final Map<String, SkinPackAdapter> allPacks = new HashMap<>();
    
    private boolean queuedChangeSkinPack = false;
    private Renderable scissorStart = null;
    private Renderable scissorEnd = null;
    private boolean hasScrolledToInitial = false;

    private boolean isDraggingPreview = false;
    private double lastMouseX = 0;
    private double lastMouseY = 0;

    public Legacy4JChangeSkinScreen(Screen parent) {
        super(parent, 180, 290, Component.translatable("bedrockskins.gui.title"));
        renderableVList.layoutSpacing(l -> 0);
        minecraft = Minecraft.getInstance();
        
        allPacks.putAll(SkinPackAdapter.getAllPacks());
        injectAutoSelectedIntoStandardPack();
        
        sortedPackIds = new ArrayList<>(allPacks.keySet());
        sortedPackIds.sort((k1, k2) -> {
            int i1 = SkinPackLoader.packOrder.indexOf(k1);
            int i2 = SkinPackLoader.packOrder.indexOf(k2);
            if (i1 != -1 && i2 != -1) return Integer.compare(i1, i2);
            return (i1 != -1) ? -1 : (i2 != -1) ? 1 : k1.compareToIgnoreCase(k2);
        });
        
        sortedPackIds.add(1, FAVORITES_PACK_ID);
        rebuildFavoritesPack();
        
        for (String packId : sortedPackIds) {
            SkinPackAdapter pack = allPacks.get(packId);
            if (pack == null || (!FAVORITES_PACK_ID.equals(packId) && pack.isEmpty())) continue;
            
            String displayName = resolvePackDisplayName(packId, pack);
            
            Button button = Button.builder(Component.literal(displayName), b -> {
                if (focusedPackId != null && focusedPackId.equals(packId)) return;
                this.focusedPackId = packId;
                this.focusedPack = this.allPacks.get(packId);
                queuedChangeSkinPack = true;
            }).pos(0, 0).size(260, 20).build();
            
            packButtons.put(packId, button);
            renderableVList.addRenderable(button);
        }
        
        openToCurrentSkin();
        isDraggingPreview = false;
    }

    private String resolvePackDisplayName(String packId, SkinPackAdapter pack) {
        if (FAVORITES_PACK_ID.equals(packId)) return Component.translatable("bedrockskins.gui.favorites").getString();
        
        String translationKey = packId, fallbackName = packId;
        if (pack != null && pack.size() > 0 && pack.getSkin(0) != null) {
            LoadedSkin firstSkin = pack.getSkin(0);
            translationKey = firstSkin.getSafePackName();
            fallbackName = firstSkin.getPackDisplayName();
        }
        String displayName = SkinPackLoader.getTranslation(translationKey);
        return displayName != null ? displayName : fallbackName;
    }

    private void rebuildFavoritesPack() {
        List<LoadedSkin> favs = new ArrayList<>();
        for (String key : FavoritesManager.getFavoriteKeys()) {
            SkinId skinId = SkinId.parse(key);
            LoadedSkin s = SkinPackLoader.getLoadedSkin(skinId);
            if (s == null && isAutoSelectedSkinId(skinId)) s = resolveAutoSelectedSkinForFavorites();
            if (s != null) favs.add(s);
        }
        allPacks.put(FAVORITES_PACK_ID, new SkinPackAdapter(FAVORITES_PACK_ID, favs));
    }

    private void injectAutoSelectedIntoStandardPack() {
        SkinPackAdapter standardPack = allPacks.get(STANDARD_PACK_ID);
        if (standardPack == null || standardPack.isEmpty()) return;

        List<LoadedSkin> merged = new ArrayList<>();
        LoadedSkin autoSkin = createAutoSelectedSkin(standardPack);
        if (autoSkin != null) merged.add(autoSkin);

        for (LoadedSkin skin : standardPack.getSkins()) {
            if (!isAutoSelectedSkin(skin)) merged.add(skin);
        }
        allPacks.put(STANDARD_PACK_ID, new SkinPackAdapter(STANDARD_PACK_ID, merged, standardPack.getPackType()));
    }

    private LoadedSkin createAutoSelectedSkin(SkinPackAdapter standardPack) {
        LoadedSkin template = standardPack.getSkin(0);
        return template == null ? null : new LoadedSkin("Standard", "Standard", AUTO_SELECTED_INTERNAL_NAME, 
                template.getGeometryData(), template.getTexture(), null, false);
    }

    private boolean isAutoSelectedSkin(LoadedSkin skin) {
        return skin != null && "Standard".equals(skin.getSerializeName()) && AUTO_SELECTED_INTERNAL_NAME.equals(skin.getSkinDisplayName());
    }

    private boolean isAutoSelectedSkinId(SkinId skinId) {
        return skinId != null && "Standard".equals(skinId.getPack()) && AUTO_SELECTED_INTERNAL_NAME.equals(skinId.getName());
    }

    private LoadedSkin resolveAutoSelectedSkinForFavorites() {
        SkinPackAdapter standardPack = getPackForUi(STANDARD_PACK_ID);
        if (standardPack == null) return null;
        return standardPack.getSkins().stream().filter(this::isAutoSelectedSkin).findFirst().orElseGet(() -> createAutoSelectedSkin(standardPack));
    }

    private SkinPackAdapter getPackForUi(String packId) {
        if (STANDARD_PACK_ID.equals(packId)) injectAutoSelectedIntoStandardPack();
        return allPacks.getOrDefault(packId, SkinPackAdapter.getPack(packId));
    }

    private boolean hasSelectedSkinWidget() {
        return this.playerSkinWidgetList != null && this.playerSkinWidgetList.element3 != null;
    }

    private void selectSkin() {
        if (!hasSelectedSkinWidget()) return;
        LoadedSkin skin = this.playerSkinWidgetList.element3.getCurrentSkin();
        if (skin == null) return;

        if (isAutoSelectedSkin(skin)) {
            resetSkin();
            return;
        }

        try {
            SkinId skinId = skin.getSkinId() != null ? skin.getSkinId() : SkinId.of(skin.getSerializeName(), skin.getSkinDisplayName());
            if (minecraft.player != null) {
                SkinManager.setSkin(minecraft.player.getUUID(), skin.getSerializeName(), skin.getSkinDisplayName());
                ClientSkinSync.sendSetSkinPayload(skinId, skin.getGeometryData().toString(), loadTextureData(skin));
            } else {
                StateManager.saveState(FavoritesManager.getFavoriteKeys(), skinId.toString());
            }
            playUISound();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void resetSkin() {
        if (minecraft.player != null) {
            SkinManager.resetSkin(minecraft.player.getUUID());
            ClientSkinSync.sendResetSkinPayload();
        } else {
            StateManager.saveState(FavoritesManager.getFavoriteKeys(), null);
        }
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

        if (focusedPackId != null && !sortedPackIds.isEmpty()) {
            int currentIndex = sortedPackIds.indexOf(focusedPackId);
            if (isUp || isDown) {
                int newIndex = isUp ? (currentIndex > 0 ? currentIndex - 1 : sortedPackIds.size() - 1)
                                    : (currentIndex < sortedPackIds.size() - 1 ? currentIndex + 1 : 0);
                
                focusedPackId = sortedPackIds.get(newIndex);
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

        if (playerSkinWidgetList != null && focusedPackId != null) {
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
        
        int MAX_SCROLLS = 100, scrolls = 0;
        while (!this.children().contains(button) && scrolls++ < MAX_SCROLLS) {
            renderableVList.mouseScrolled(true);
        }
        scrolls = 0;
        while (!this.children().contains(button) && scrolls++ < MAX_SCROLLS * 2) {
            renderableVList.mouseScrolled(false);
        }
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
        if ((left || right) && this.playerSkinWidgetList != null) {
            if (this.playerSkinWidgetList.widgets.stream().anyMatch(a -> a.progress <= 1)) return true;
            this.playerSkinWidgetList.sortForIndex(this.playerSkinWidgetList.index + (left ? -1 : 1));
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
            if (Math.abs(deltaX) > 0.01) playerSkinWidgetList.element3.onDrag(playerSkinWidgetList.element3.getX(), playerSkinWidgetList.element3.getY(), deltaX, 0);
            state.block();
        }
    }
    
    private void favorite() {
        if (!hasSelectedSkinWidget()) return;
        LoadedSkin skin = this.playerSkinWidgetList.element3.getCurrentSkin();
        if (skin == null) return;

        boolean wasFavorite = FavoritesManager.isFavorite(skin);
        int oldIndex = playerSkinWidgetList != null ? playerSkinWidgetList.index : 0;
        
        if (wasFavorite) FavoritesManager.removeFavorite(skin);
        else FavoritesManager.addFavorite(skin);
        
        rebuildFavoritesPack();

        if (FAVORITES_PACK_ID.equals(focusedPackId)) {
            this.focusedPack = allPacks.get(FAVORITES_PACK_ID);
            int favCount = this.focusedPack.size();
            int newIndex = !wasFavorite ? favCount - 1 : (favCount == 0 ? 0 : Math.min(oldIndex, favCount - 1));
            updateSkinPack(newIndex);
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

        // Open Skin Customization
        renderer.add(() -> isKbm ? ControlTooltip.getKeyIcon(InputConstants.KEY_H) : ControllerBinding.BACK.bindingState.getIcon(), () -> Component.translatable("options.skinCustomisation.title"));

        // Compound left/right navigation icon
        renderer.add(() -> isKbm ? ControlTooltip.COMPOUND_ICON_FUNCTION.apply(new ControlTooltip.Icon[]{ControlTooltip.getKeyIcon(InputConstants.KEY_LEFT), ControlTooltip.SPACE_ICON, ControlTooltip.getKeyIcon(InputConstants.KEY_RIGHT)}) : ControllerBinding.LEFT_STICK.bindingState.getIcon(), () -> Component.translatable("bedrockskins.menu.navigate"));
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
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
    
    private void renderBackgroundPanels(GuiGraphics guiGraphics) {
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, BedrockSkinsSprites.SKIN_PANEL, tooltipBox.getX() - 10, panel.getY() + 7, tooltipBox.getWidth(), tooltipBox.getHeight() - 2);
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, BedrockSkinsSprites.PANEL_FILLER, tooltipBox.getX() - 5, panel.getY() + 16 + tooltipBox.getHeight() - 80, tooltipBox.getWidth() - 14, 60);
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, LegacySprites.SQUARE_RECESSED_PANEL , tooltipBox.getX() - 1, panel.getY() + tooltipBox.getHeight() - 59, tooltipBox.getWidth() - 55, 55);

        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, LegacySprites.ICON_HOLDER, tooltipBox.getX() + tooltipBox.getWidth() - 50, panel.getY() + tooltipBox.getHeight() - 60 + 3, 24, 24);
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, LegacySprites.ICON_HOLDER, tooltipBox.getX() + tooltipBox.getWidth() - 50, panel.getY() + tooltipBox.getHeight() - 60 + 30, 24, 24);

        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, BedrockSkinsSprites.PACK_NAME_BOX, tooltipBox.getX() - 5, panel.getY() + 16 + 4, tooltipBox.getWidth() - 18, 40);
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, BedrockSkinsSprites.SKIN_BOX, tooltipBox.getX() - 5, panel.getY() + 16, tooltipBox.getWidth() - 14, tooltipBox.getHeight() - 80);
    }

    private String getFocusedPackSerializeNameLower() {
        if (FAVORITES_PACK_ID.equals(focusedPackId)) return "standard";

        if (focusedPack != null && focusedPack.size() > 0) {
            LoadedSkin firstSkin = focusedPack.getSkin(0);
            if (firstSkin != null && firstSkin.getSerializeName() != null && !firstSkin.getSerializeName().isEmpty()) {
                return firstSkin.getSerializeName().toLowerCase(Locale.ROOT);
            }
        }
        if (focusedPackId != null && !focusedPackId.isEmpty()) {
            int split = focusedPackId.lastIndexOf('.');
            String guessedName = split >= 0 ? focusedPackId.substring(split + 1) : focusedPackId;
            if (!guessedName.isEmpty()) return guessedName.toLowerCase(Locale.ROOT);
        }
        return "standard";
    }

    //? if >=1.21.11 {
    private Identifier resolveFocusedPackIconSprite() {
        Identifier packIcon = Identifier.fromNamespaceAndPath("bedrockskins", "icons/" + getFocusedPackSerializeNameLower());
        return hasSpriteTexture(packIcon) ? packIcon : Identifier.fromNamespaceAndPath("bedrockskins", "icons/standard");
    }
    private boolean hasSpriteTexture(Identifier spriteId) {
        return minecraft.getResourceManager().getResource(Identifier.fromNamespaceAndPath(spriteId.getNamespace(), "textures/gui/sprites/" + spriteId.getPath() + ".png")).isPresent();
    }
    //?} else {
    private ResourceLocation resolveFocusedPackIconSprite() {
        ResourceLocation packIcon = ResourceLocation.fromNamespaceAndPath("bedrockskins", "icons/" + getFocusedPackSerializeNameLower());
        return hasSpriteTexture(packIcon) ? packIcon : ResourceLocation.fromNamespaceAndPath("bedrockskins", "icons/standard");
    }
    private boolean hasSpriteTexture(ResourceLocation spriteId) {
        return minecraft.getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath(spriteId.getNamespace(), "textures/gui/sprites/" + spriteId.getPath() + ".png")).isPresent();
    }
    //?}
    
    private void renderPackName(GuiGraphics guiGraphics) {
        int middle = tooltipBox.getX() - 5 + (tooltipBox.getWidth() - 18) / 2;
        String packDisplayName = resolvePackDisplayName(focusedPackId, focusedPack);

        var stack = guiGraphics.pose();

        // Render skin pack name
        stack.pushMatrix();
        stack.translate(middle, panel.getY() + 16 + 4 + 7);
        stack.scale(1.5f, 1.5f);
        guiGraphics.drawCenteredString(minecraft.font, Component.literal(packDisplayName), 0, 0, 0xffffffff);
        stack.popMatrix();

        // Draw subtitle below pack name
        if (focusedPack != null && focusedPack.getPackType() != null && !focusedPack.getPackType().isEmpty()) {
            stack.pushMatrix();
            stack.translate(middle, panel.getY() + 16 + 4 + 25);
            guiGraphics.drawCenteredString(minecraft.font, Component.translatable("bedrockskins.packType." + focusedPack.getPackType()), 0, 0, 0xffffffff);
            stack.popMatrix();
        }
    }
    
    private void renderSkinInfo(GuiGraphics guiGraphics) {
        LoadedSkin skin = playerSkinWidgetList.element3.getCurrentSkin();
        if (skin == null) return;
        
        int middle = tooltipBox.getX() - 5 + (tooltipBox.getWidth() - 18) / 2;
        Component skinNameComponent = isAutoSelectedSkin(skin) ? Component.translatable(AUTO_SELECTED_TRANSLATION_KEY) : Component.literal(SkinPackLoader.getTranslation(skin.getSafeSkinName()) != null ? SkinPackLoader.getTranslation(skin.getSafeSkinName()) : skin.getSkinDisplayName());

        var stack = guiGraphics.pose();
        // Render skin name
        stack.pushMatrix();
        stack.translate(middle, panel.getY() + tooltipBox.getHeight() - 59 + 10);
        stack.scale(1.5f, 1.5f);
        guiGraphics.drawCenteredString(minecraft.font, skinNameComponent, 0, 0, 0xffffffff);
        stack.popMatrix();

        // Render description if available
        String desc = SkinPackLoader.getTranslation(skin.getSafeSkinName() + ".description");
        if (desc != null && !desc.isEmpty()) {
            stack.pushMatrix();
            stack.translate(middle, panel.getY() + tooltipBox.getHeight() - 59 + 35);
            stack.scale(1.5f, 1.5f);
            guiGraphics.drawCenteredString(minecraft.font, Component.literal(desc), 0, 0, 0xffffffff);
            stack.popMatrix();
        }
        
        // Render checkmark if this skin is currently selected
        SkinId currentSkinKey = SkinManager.getLocalSelectedKey();
        if (isAutoSelectedSkin(skin) ? currentSkinKey == null : Objects.equals(currentSkinKey, skin.getSkinId())) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, LegacySprites.BEACON_CONFIRM, tooltipBox.getX() + tooltipBox.getWidth() - 50, panel.getY() + tooltipBox.getHeight() - 60 + 3, 24, 24);
        }
        
        // Render heart if this skin is favorited
        if (FavoritesManager.isFavorite(skin)) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, BedrockSkinsSprites.HEART_CONTAINER, tooltipBox.getX() + tooltipBox.getWidth() - 50 + 4, panel.getY() + tooltipBox.getHeight() - 60 + 30 + 4, 16, 16);
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, BedrockSkinsSprites.HEART_FULL, tooltipBox.getX() + tooltipBox.getWidth() - 50 + 4, panel.getY() + tooltipBox.getHeight() - 60 + 30 + 4, 16, 16);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if ((isInBounds(mouseX, mouseY, tooltipBox.getX(), panel.getY(), tooltipBox.getWidth(), tooltipBox.getHeight()) || !ControlType.getActiveType().isKbm()) && scrollableRenderer.mouseScrolled(scrollY)) return true;
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void renderableVListInit() {
        addRenderableOnly((guiGraphics, i, j, f) -> {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, LegacySprites.SQUARE_RECESSED_PANEL , panel.getX() + 7, panel.getY() + 7 + 130 - 8, panel.getWidth() - 14, panel.getHeight() - 14 - 135 + 1 + 8);
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, LegacySprites.SQUARE_RECESSED_PANEL , panel.getX() + 34, panel.getY() + 10, 112, 112);
            if (this.focusedPack != null) {
                guiGraphics.pose().pushMatrix(); 
                guiGraphics.pose().translate(panel.getX() + 35.4f, panel.getY() + 11.4f);
                guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, resolveFocusedPackIconSprite(), 0, 0, 109, 109);
                guiGraphics.pose().popMatrix();
            }
        });

        tooltipBox.init("tooltipBox");
        getRenderableVList().init("renderableVList", panel.getX() + 11, panel.getY() + 132, panel.getWidth() - 22, panel.getHeight() - 135 + 10 - 22);
    }

    void openToCurrentSkin() {
        injectAutoSelectedIntoStandardPack();
        SkinId currentSkinKey = SkinManager.getLocalSelectedKey();
        
        if (currentSkinKey == null || SkinPackLoader.getLoadedSkin(currentSkinKey) == null) {
            String targetPackId = getPackForUi(STANDARD_PACK_ID) != null ? STANDARD_PACK_ID : (SkinPackAdapter.getAllPacks().isEmpty() ? null : SkinPackAdapter.getAllPacks().keySet().iterator().next());
            if (targetPackId != null) {
                focusedPackId = targetPackId;
                focusedPack = getPackForUi(targetPackId);
                queuedChangeSkinPack = true;
                updateSkinPack(0);
            }
            return;
        }

        LoadedSkin currentSkin = SkinPackLoader.getLoadedSkin(currentSkinKey);
        SkinPackAdapter pack = getPackForUi(currentSkin.getId());
        if (pack != null) {
            focusedPackId = currentSkin.getId();
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
        this.queuedChangeSkinPack = false;
        if (focusedPackId != null && packButtons.containsKey(focusedPackId)) setFocused(packButtons.get(focusedPackId));

        // Capture the current pose and rotation before clearing the old widgets
        PlayerSkinWidget.PreviewPose savedPose = PlayerSkinWidget.PreviewPose.STANDING;
        float savedRotX = 0f;
        float savedRotY = 0f;
        if (hasSelectedSkinWidget()) {
            savedPose = playerSkinWidgetList.element3.getPreviewPose();
            savedRotX = playerSkinWidgetList.element3.getRotationX();
            savedRotY = playerSkinWidgetList.element3.getRotationY();
        }

        if (playerSkinWidgetList != null) {
            playerSkinWidgetList.widgets.forEach(w -> { w.cleanup(); removeWidget(w); });
        }

        if (focusedPack == null || focusedPack.isEmpty()) {
            playerSkinWidgetList = null;
            return;
        }

        int boxX = tooltipBox.getX() - 5, boxWidth = tooltipBox.getWidth() - 14;
        int boxY = panel.getY() + 16, boxHeight = tooltipBox.getHeight() - 80;
        int centerX = boxX + boxWidth / 2, centerY = boxY + boxHeight / 2;

        scissorStart = addRenderableOnly((guiGraphics, i, j, f) -> {
            if (playerSkinWidgetList != null) guiGraphics.enableScissor(boxX + 7, boxY + 4, boxX + boxWidth - 5, boxY + boxHeight - 4);
        });

        List<PlayerSkinWidget> widgets = new ArrayList<>();
        for (int i = 0; i < focusedPack.size(); i++) {
            final LoadedSkin skin = focusedPack.getSkin(i);
            final SkinReference finalRef = (FAVORITES_PACK_ID.equals(focusedPackId) && skin != null) 
                ? new SkinReference(skin.getId(), SkinPackAdapter.getPack(skin.getId()).indexOf(skin)) 
                : new SkinReference(focusedPackId, i);
            
            widgets.add(addRenderableWidget(new PlayerSkinWidget(130, 160, minecraft.getEntityModels(), () -> finalRef, () -> skin)));
        }

        playerSkinWidgetList = PlayerSkinWidgetList.of(centerX - 65, centerY - 65, widgets.toArray(new PlayerSkinWidget[0]));
        
        int n = widgets.size();
        int targetIndex = ((index % n) + n) % n;
        
        playerSkinWidgetList.index = targetIndex; 
        
        // Pre-set the center element and its pose
        playerSkinWidgetList.element3 = widgets.get(targetIndex); 
        playerSkinWidgetList.element3.setPreviewPose(savedPose);

        playerSkinWidgetList.sortForIndex(targetIndex, savedRotX, savedRotY);

        scissorEnd = addRenderableOnly((guiGraphics, i, j, f) -> { if (playerSkinWidgetList != null) guiGraphics.disableScissor(); });
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

    private byte[] loadTextureData(LoadedSkin skin) {
        try {
            AssetSource src = skin.getTexture();
            if (src instanceof AssetSource.Resource res) {
                return minecraft.getResourceManager().getResource(res.getId()).map(r -> {
                    try { return r.open().readAllBytes(); } catch (Exception e) { return new byte[0]; }
                }).orElse(new byte[0]);
            } else if (src instanceof AssetSource.File fSrc) {
                return Files.readAllBytes(new File(fSrc.getPath()).toPath());
            } else if (src instanceof AssetSource.Zip z) {
                try (ZipFile zip = new ZipFile(z.getZipPath())) {
                    ZipEntry entry = zip.getEntry(z.getInternalPath());
                    if (entry != null) {
                        try (var is = zip.getInputStream(entry)) { return is.readAllBytes(); }
                    }
                }
            }
        } catch (Exception ignored) {}
        return new byte[0];
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubled) {
        if (event.button() == 0 && playerSkinWidgetList != null) {
            double mouseX = event.x();
            double mouseY = event.y();

            // Check if clicking the action icons
            if (hasSelectedSkinWidget()) {
                int iconX = tooltipBox.getX() + tooltipBox.getWidth() - 50;
                int checkY = panel.getY() + tooltipBox.getHeight() - 60 + 3;
                int heartY = panel.getY() + tooltipBox.getHeight() - 60 + 30;

                // Clicked Select/Checkmark icon holder
                if (isInBounds(mouseX, mouseY, iconX, checkY, 24, 24)) {
                    selectSkin();
                    return true;
                }

                // Clicked Favorite/Heart icon holder
                if (isInBounds(mouseX, mouseY, iconX, heartY, 24, 24)) {
                    favorite();
                    return true;
                }
            }

            // Calculate the visible scissor bounds
            int boxX = tooltipBox.getX() - 5;
            int boxWidth = tooltipBox.getWidth() - 14;
            int boxY = panel.getY() + 16;
            int boxHeight = tooltipBox.getHeight() - 80;

            int scissorLeft = boxX + 7;
            int scissorRight = boxX + boxWidth - 5;
            int scissorTop = boxY + 4;
            int scissorBottom = boxY + boxHeight - 4;

            // Only process skin clicks if the mouse is inside the  panel
            if (mouseX >= scissorLeft && mouseX <= scissorRight && mouseY >= scissorTop && mouseY <= scissorBottom) {
                
                // Check if clicking the center widget (for rotation drag)
                if (hasSelectedSkinWidget() && isInBounds(mouseX, mouseY, playerSkinWidgetList.element3.getX(), playerSkinWidgetList.element3.getY(), playerSkinWidgetList.element3.getWidth(), playerSkinWidgetList.element3.getHeight())) {
                    isDraggingPreview = true;
                    lastMouseX = mouseX;
                    lastMouseY = mouseY;
                    return true;
                }

                // Check if clicking any other visible widget in the carousel
                if (playerSkinWidgetList.widgets.stream().noneMatch(w -> w.progress <= 1)) {
                    for (int i = 0; i < playerSkinWidgetList.widgets.size(); i++) {
                        PlayerSkinWidget w = playerSkinWidgetList.widgets.get(i);
                        
                        if (w.visible && w != playerSkinWidgetList.element3) {
                            if (isInBounds(mouseX, mouseY, w.getX(), w.getY(), w.getWidth(), w.getHeight())) {
                                playScrollSound();
                                playerSkinWidgetList.sortForIndex(i);
                                return true;
                            }
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
            if (Math.abs(delta) > 0.01) playerSkinWidgetList.element3.onDrag(event.x(), 0, delta, 0);
            lastMouseX = event.x();
            lastMouseY = event.y();
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public void onClose() {
        if (playerSkinWidgetList != null) playerSkinWidgetList.widgets.forEach(PlayerSkinWidget::cleanup);
        super.onClose();
    }
}
*/
//?}
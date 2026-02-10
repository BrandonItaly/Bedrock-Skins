package com.brandonitaly.bedrockskins.client.gui.legacy;

//? if legacy4j {
/*
import com.brandonitaly.bedrockskins.BedrockSkinsNetworking;
import com.brandonitaly.bedrockskins.client.FavoritesManager;
import com.brandonitaly.bedrockskins.client.SkinManager;
import com.brandonitaly.bedrockskins.pack.AssetSource;
import com.brandonitaly.bedrockskins.pack.LoadedSkin;
import com.brandonitaly.bedrockskins.pack.SkinId;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import com.brandonitaly.bedrockskins.pack.SkinId;
import com.brandonitaly.bedrockskins.pack.StringUtils;
import com.mojang.blaze3d.platform.InputConstants;
//? if fabric {
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
//? }
//? if <1.21.11 {
import net.minecraft.Util;
//?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} else {
import net.minecraft.resources.ResourceLocation;
//?}
import net.minecraft.sounds.SoundEvents;
import net.minecraft.ChatFormatting;
import wily.legacy.Legacy4J;
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

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Legacy4JChangeSkinScreen extends PanelVListScreen implements Controller.Event, ControlTooltip.Event {
    protected final Minecraft minecraft;
    protected final Panel tooltipBox = Panel.tooltipBoxOf(panel, 350 + 50);
    protected ScrollableRenderer scrollableRenderer = new ScrollableRenderer(new LegacyScrollRenderer());
    private String focusedPackId;
    private SkinPackAdapter focusedPack;
    private List<String> sortedPackIds = new ArrayList<>();
    private PlayerSkinWidgetList playerSkinWidgetList;
    private final Map<String, Button> packButtons = new HashMap<>();
    private boolean queuedChangeSkinPack = false;
    private Renderable scissorStart = null;
    private Renderable scissorEnd = null;
    
    // Maintain a map of all packs so we can update Favorites dynamically
    private final Map<String, SkinPackAdapter> allPacks = new HashMap<>();

    // --- Rotation state for preview ---
    private boolean isDraggingPreview = false;
    private double lastMouseX = 0;
    private double lastMouseY = 0;

    public Legacy4JChangeSkinScreen(Screen parent) {
        super(parent, 180, 290, Component.translatable("bedrockskins.gui.title"));
        renderableVList.layoutSpacing(l -> 0);
        minecraft = Minecraft.getInstance();
        
        // Load initial packs
        Map<String, SkinPackAdapter> initialPacks = SkinPackAdapter.getAllPacks();
        this.allPacks.putAll(initialPacks);
        
        // Sort packs using packOrder
        sortedPackIds = new ArrayList<>(allPacks.keySet());
        sortedPackIds.sort((k1, k2) -> {
            int i1 = SkinPackLoader.packOrder.indexOf(k1);
            int i2 = SkinPackLoader.packOrder.indexOf(k2);
            if (i1 != -1 && i2 != -1) return Integer.compare(i1, i2);
            if (i1 != -1) return -1;
            if (i2 != -1) return 1;
            return k1.compareToIgnoreCase(k2);
        });
        // Always add Favorites pack after the default skin pack
        sortedPackIds.add(1, "skinpack.Favorites");
        
        // Create favorites pack by collecting favorited skins
        rebuildFavoritesPack();
        
        for (String packId : sortedPackIds) {
            SkinPackAdapter pack;
            String displayName;
            String translationKey;
            String fallbackName;
            
            // We use a supplier or look up from allPacks dynamically in the button action
            // to ensure we always get the latest version of the pack (crucial for Favorites)
            
            if ("skinpack.Favorites".equals(packId)) {
                pack = allPacks.get(packId); // Initial get
                displayName = Component.translatable("bedrockskins.gui.favorites").getString();
                translationKey = displayName;
                fallbackName = displayName;
            } else {
                pack = allPacks.get(packId);
                if (pack == null || pack.isEmpty()) continue;
                
                // Use the safe pack name from the first skin for translation
                translationKey = packId;
                fallbackName = packId;
                if (pack.size() > 0) {
                    LoadedSkin firstSkin = pack.getSkin(0);
                    if (firstSkin != null) {
                        translationKey = firstSkin.getSafePackName();
                        fallbackName = firstSkin.getPackDisplayName();
                    }
                }
                displayName = SkinPackLoader.getTranslation(translationKey);
                if (displayName == null) displayName = fallbackName;
            }
            
            final String btnLabel = displayName;
            final String currentPackId = packId;
            
            Button button = Button.builder(Component.literal(btnLabel), b -> {
                if (focusedPackId != null && focusedPackId.equals(currentPackId)) return;
                
                Legacy4JChangeSkinScreen.this.focusedPackId = currentPackId;
                // Look up the pack from the map to ensure we get the freshest version (for Favorites)
                Legacy4JChangeSkinScreen.this.focusedPack = Legacy4JChangeSkinScreen.this.allPacks.get(currentPackId);
                
                queuedChangeSkinPack = true;
            })
                .pos(0, 0)
                .size(260, 20)
                .build();
            packButtons.put(packId, button);
            renderableVList.addRenderable(button);
        }
        
        openToCurrentSkin();
        // Mouse drag state reset
        isDraggingPreview = false;
    }
    
    private void rebuildFavoritesPack() {
        List<LoadedSkin> favs = new ArrayList<>();
        for (String key : FavoritesManager.getFavoriteKeys()) {
            LoadedSkin s = SkinPackLoader.getLoadedSkin(SkinId.parse(key));
            if (s != null) favs.add(s);
        }
        allPacks.put("skinpack.Favorites", new SkinPackAdapter("skinpack.Favorites", favs));
    }

    // Keyboard input handled by default; controller handling via bindingStateTick.

    private void selectSkin() {
        if (this.playerSkinWidgetList != null && this.playerSkinWidgetList.element3 != null) {
            SkinReference ref = this.playerSkinWidgetList.element3.skinRef.get();
            if (ref == null) return;

            // Always update focused pack and packId to match the selected skin
            String newPackId = ref.packId();
            SkinPackAdapter newPack = SkinPackAdapter.getPack(newPackId);
            if (newPack == null) return;

            LoadedSkin skin = newPack.getSkin(ref.ordinal());
            if (skin == null) return;

            try {
                SkinId skinId = skin.getSkinId();
                SkinManager.setSkin(minecraft.player.getUUID().toString(), skin.getSerializeName(), skin.getSkinDisplayName());

                // Load texture data
                byte[] textureData = loadTextureData(skin);

                //? if fabric {
                ClientPlayNetworking.send(new BedrockSkinsNetworking.SetSkinPayload(
                    skinId,
                    skin.getGeometryData().toString(),
                    textureData
                ));
                //? } else if neoforge {
                net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(
                    new BedrockSkinsNetworking.SetSkinPayload(
                        skinId,
                        skin.getGeometryData().toString(),
                        textureData
                    )
                );
                //? }

                playUISound();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void resetSkin() {
        if (minecraft.player != null) {
            SkinManager.resetSkin(minecraft.player.getUUID().toString());
            //? if fabric {
            ClientPlayNetworking.send(new BedrockSkinsNetworking.SetSkinPayload(null, "", new byte[0]));
            //? } else if neoforge {
            net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(
                new BedrockSkinsNetworking.SetSkinPayload(null, "", new byte[0])
            );
            //? }
            playUISound();
        }
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int keyCode = InputConstants.getKey(event).getValue();

        // Handle pack list navigation (up/down) with legacy parity
        if (focusedPackId != null && !sortedPackIds.isEmpty()) {
            int currentIndex = sortedPackIds.indexOf(focusedPackId);
            if (keyCode == InputConstants.KEY_UP) {
                int newIndex = (currentIndex > 0) ? currentIndex - 1 : sortedPackIds.size() - 1; // loop to bottom
                String newPackId = sortedPackIds.get(newIndex);
                focusedPackId = newPackId;
                focusedPack = allPacks.get(newPackId);
                queuedChangeSkinPack = true;
                Button btn = packButtons.get(newPackId);
                if (btn != null) {
                    setFocused(btn);
                    ensureButtonVisible(btn);
                }
                playFocusSound();
                return true;
            } else if (keyCode == InputConstants.KEY_DOWN) {
                int newIndex = (currentIndex < sortedPackIds.size() - 1) ? currentIndex + 1 : 0; // loop to top
                String newPackId = sortedPackIds.get(newIndex);
                focusedPackId = newPackId;
                focusedPack = allPacks.get(newPackId);
                queuedChangeSkinPack = true;
                Button btn = packButtons.get(newPackId);
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

        if (keyCode == InputConstants.KEY_RETURN) {
            selectSkin();
            return true;
        }
        if (keyCode == InputConstants.KEY_F) {
            favorite();
            return true;
        }
        if (keyCode == InputConstants.KEY_R) {
            resetSkin();
            return true;
        }
        if (control(keyCode == InputConstants.KEY_LBRACKET, keyCode == InputConstants.KEY_RBRACKET)) return true;
        if (control(keyCode == InputConstants.KEY_LEFT, keyCode == InputConstants.KEY_RIGHT)) return true;
        if (handlePoseChange(keyCode == InputConstants.KEY_LSHIFT, keyCode == InputConstants.KEY_RSHIFT)) return true;

        return super.keyPressed(event);
    }

    private void ensureButtonVisible(Button button) {
        // Ensure the highlighted button is always visible, including when looping
        int safety = 0;
        if (!packButtons.containsValue(button)) return;
        // Try to scroll down until visible, then up if not found, then loop
        boolean found = false;
        for (int i = 0; i < 500; i++) {
            if (this.children().contains(button)) {
                found = true;
                break;
            }
            renderableVList.mouseScrolled(true);
        }
        if (!found) {
            // Try scrolling up if not found
            for (int i = 0; i < 500; i++) {
                if (this.children().contains(button)) {
                    found = true;
                    break;
                }
                renderableVList.mouseScrolled(false);
            }
        }
        // If still not found, forcibly scroll to top or bottom and try again (looping)
        if (!found) {
            // Scroll to top
            for (int i = 0; i < 500; i++) renderableVList.mouseScrolled(false);
            // Try to scroll down to find the button
            for (int i = 0; i < 500; i++) {
                if (this.children().contains(button)) break;
                renderableVList.mouseScrolled(true);
            }
        }
    }

    // Click handling moved to keyboard/controller; avoids API mismatch on 1.21.10
    
    private boolean isInBounds(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    boolean handlePoseChange(boolean left, boolean right) {
        if (!(left || right)) return false;
        if (playerSkinWidgetList == null || playerSkinWidgetList.element3 == null) return false;
        if (playerSkinWidgetList.element3.isInterpolating()) return false;

        // Toggle pose (stand/crouch) on the focused widget
        playerSkinWidgetList.element3.togglePose();
        return true;
    }

    boolean control(boolean left, boolean right) {
        if ((left || right)) {
            if (this.playerSkinWidgetList != null) {
                if (this.playerSkinWidgetList.widgets.stream().anyMatch(a -> a.progress <= 1)) return true;
                int offset = 0;
                if (left) offset--;
                if (right) offset++;
                this.playerSkinWidgetList.sortForIndex(this.playerSkinWidgetList.index + offset);
                playScrollSound();
                return true;
            }
        }
        return false;
    }

    @Override
    public void bindingStateTick(BindingState state) {
        if (state.is(ControllerBinding.UP_BUTTON) && state.released) {
            favorite();
        }
        if (state.is(ControllerBinding.RIGHT_STICK_DOWN) && state.justPressed) {
            if (handlePoseChange(true, false)) return;
        }
        if (state.is(ControllerBinding.RIGHT_STICK_BUTTON) && state.justPressed) {
            resetSkin();
            return;
        }
        // --- Controller right stick rotation for preview ---
        if (playerSkinWidgetList != null && playerSkinWidgetList.element3 != null) {
            PlayerSkinWidget widget = playerSkinWidgetList.element3;
            // Use BindingState.Axis for stick input
            if (state.is(ControllerBinding.RIGHT_STICK) && state instanceof BindingState.Axis stick) {
                double sensitivity = 0.15d;
                double deltaX = stick.getDeadZone() > Math.abs(stick.x) ? 0 : -(double)stick.x * sensitivity;
                // Only rotate if stick is moved
                if (Math.abs(deltaX) > 0.01) {
                    widget.onDrag(widget.getX(), widget.getY(), deltaX, 0);
                }
                state.block();
            }
        }
    }

    private void favorite() {
        if (this.playerSkinWidgetList != null && this.playerSkinWidgetList.element3 != null) {
            SkinReference ref = this.playerSkinWidgetList.element3.skinRef.get();
            if (ref == null) return;
            
            // Always resolve from the original pack to ensure we get the correct skin to favorite
            SkinPackAdapter originalPack = SkinPackAdapter.getPack(ref.packId());
            if (originalPack == null) return;
            
            LoadedSkin skin = originalPack.getSkin(ref.ordinal());
            if (skin == null) return;

            boolean wasFavorite = FavoritesManager.isFavorite(skin);
            int oldIndex = playerSkinWidgetList != null ? playerSkinWidgetList.index : 0;
            if (wasFavorite) {
                FavoritesManager.removeFavorite(skin);
            } else {
                FavoritesManager.addFavorite(skin);
            }
            
            // Rebuild the favorites pack in the map
            rebuildFavoritesPack();

            // Instantly update the Favorites tab if it's currently active
            if ("skinpack.Favorites".equals(focusedPackId)) {
                // Update our local reference to the new pack
                this.focusedPack = allPacks.get("skinpack.Favorites");
                
                int favCount = this.focusedPack.size();
                int newIndex = oldIndex;
                
                if (!wasFavorite) {
                    // If we just added a favorite, we might want to select it
                    // But usually staying put or going to end is fine. 
                    // Logic: If added, it goes to end of list.
                    newIndex = favCount - 1;
                } else {
                    // If we just removed, try to keep the same index, but clamp
                    if (favCount == 0) {
                        newIndex = 0;
                    } else if (newIndex >= favCount) {
                        newIndex = favCount - 1;
                    }
                }
                updateSkinPack(newIndex);
            }

            playUISound();
        }
    }

    @Override
    public void addControlTooltips(ControlTooltip.Renderer renderer) {
        // Select
        renderer.add(
            () -> ControlType.getActiveType().isKbm() ? 
                ControlTooltip.getKeyIcon(InputConstants.KEY_RETURN) : 
                ControllerBinding.DOWN_BUTTON.bindingState.getIcon(), 
            () -> Component.translatable("bedrockskins.button.select")
        );
        // Cancel
        renderer.add(
            () -> ControlType.getActiveType().isKbm() ? 
                ControlTooltip.getKeyIcon(InputConstants.KEY_ESCAPE) : 
                ControllerBinding.RIGHT_BUTTON.bindingState.getIcon(), 
            () -> Component.translatable("gui.cancel")
        );
        // Favorite/Unfavorite
        renderer.add(
            () -> ControlType.getActiveType().isKbm() ? 
                ControlTooltip.getKeyIcon(InputConstants.KEY_F) : 
                ControllerBinding.UP_BUTTON.bindingState.getIcon(), 
            () -> {
                if (playerSkinWidgetList != null && playerSkinWidgetList.element3 != null) {
                    SkinReference ref = playerSkinWidgetList.element3.skinRef.get();
                    if (ref != null) {
                        SkinPackAdapter p = SkinPackAdapter.getPack(ref.packId());
                        if (p != null) {
                            LoadedSkin s = p.getSkin(ref.ordinal());
                            if (s != null) {
                                if (FavoritesManager.isFavorite(s)) {
                                    return Component.translatable("bedrockskins.button.unfavorite");
                                } else {
                                    return Component.translatable("bedrockskins.button.favorite");
                                }
                            }
                        }
                    }
                }
                return Component.translatable("bedrockskins.button.favorite");
            }
        );
        // Compound left/right navigation icon
        renderer.add(
            () -> ControlType.getActiveType().isKbm() ? 
                ControlTooltip.COMPOUND_ICON_FUNCTION.apply(new ControlTooltip.Icon[]{
                    ControlTooltip.getKeyIcon(InputConstants.KEY_LEFT),
                    ControlTooltip.SPACE_ICON,
                    ControlTooltip.getKeyIcon(InputConstants.KEY_RIGHT)
                }) : ControllerBinding.LEFT_STICK.bindingState.getIcon(),
            () -> Component.translatable("bedrockskins.menu.navigate")
        );
        // Reset skin
        renderer.add(
            () -> ControlType.getActiveType().isKbm() ? 
                ControlTooltip.getKeyIcon(InputConstants.KEY_R) : 
                ControllerBinding.RIGHT_STICK_BUTTON.bindingState.getIcon(), 
            () -> Component.translatable("bedrockskins.button.reset")
        );
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
        if (focusedPackId != null) {
            renderPackName(guiGraphics);
        }
        
        // Render skin name and info
        if (playerSkinWidgetList != null && playerSkinWidgetList.element3 != null) {
            renderSkinInfo(guiGraphics);
        }
    }
    
    private void renderBackgroundPanels(GuiGraphics guiGraphics) {
        // Render using blitSprite for nine-slice sprite atlas
        //? if >=1.21.11 {
        var skinPanel = Identifier.fromNamespaceAndPath("bedrockskins", "tiles/skin_panel");
        var panelFiller = Identifier.fromNamespaceAndPath("bedrockskins", "tiles/panel_filler");
        var recessedPanel = Identifier.fromNamespaceAndPath(Legacy4J.MOD_ID, "tiles/square_recessed_panel");
        var packNameBox = Identifier.fromNamespaceAndPath("bedrockskins", "tiles/pack_name_box");
        var skinBox = Identifier.fromNamespaceAndPath("bedrockskins", "tiles/skin_box");
        //?} else {
        var skinPanel = ResourceLocation.fromNamespaceAndPath("bedrockskins", "tiles/skin_panel");
        var panelFiller = ResourceLocation.fromNamespaceAndPath("bedrockskins", "tiles/panel_filler");
        var recessedPanel = ResourceLocation.fromNamespaceAndPath(Legacy4J.MOD_ID, "tiles/square_recessed_panel");
        var packNameBox = ResourceLocation.fromNamespaceAndPath("bedrockskins", "tiles/pack_name_box");
        var skinBox = ResourceLocation.fromNamespaceAndPath("bedrockskins", "tiles/skin_box");
        //?}

        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, skinPanel,
            tooltipBox.getX() - 10, panel.getY() + 7,
            tooltipBox.getWidth(), tooltipBox.getHeight() - 2);
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, panelFiller,
            tooltipBox.getX() - 5, panel.getY() + 16 + tooltipBox.getHeight() - 80,
            tooltipBox.getWidth() - 14, 60);
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, recessedPanel,
            tooltipBox.getX() - 1, panel.getY() + tooltipBox.getHeight() - 59,
            tooltipBox.getWidth() - 55, 55);

        // Icons background
        //? if >=1.21.11 {
        var iconHolder = Identifier.fromNamespaceAndPath(Legacy4J.MOD_ID, "container/sizeable_icon_holder");
        //?} else {
        var iconHolder = ResourceLocation.fromNamespaceAndPath(Legacy4J.MOD_ID, "container/sizeable_icon_holder");
        //?}
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, iconHolder,
            tooltipBox.getX() + tooltipBox.getWidth() - 50,
            panel.getY() + tooltipBox.getHeight() - 60 + 3,
            24, 24);
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, iconHolder,
            tooltipBox.getX() + tooltipBox.getWidth() - 50,
            panel.getY() + tooltipBox.getHeight() - 60 + 30,
            24, 24);

        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, packNameBox,
            tooltipBox.getX() - 5, panel.getY() + 16 + 4,
            tooltipBox.getWidth() - 18, 40);
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, skinBox,
            tooltipBox.getX() - 5, panel.getY() + 16,
            tooltipBox.getWidth() - 14, tooltipBox.getHeight() - 80);
    }
    
    private void renderPackName(GuiGraphics guiGraphics) {
        int x = tooltipBox.getX() - 5;
        int width = tooltipBox.getWidth() - 18;
        int middle = x + width / 2;
        
        String packDisplayName;
        
        // Special handling for Favorites pack
        if ("skinpack.Favorites".equals(focusedPackId)) {
            packDisplayName = Component.translatable("bedrockskins.gui.favorites").getString();
        } else {
            String translationKey = focusedPackId;
            String fallbackName = focusedPackId;
            if (focusedPack != null && focusedPack.size() > 0) {
                LoadedSkin firstSkin = focusedPack.getSkin(0);
                if (firstSkin != null) {
                    translationKey = firstSkin.getSafePackName();
                    fallbackName = firstSkin.getPackDisplayName();
                }
            }
            packDisplayName = SkinPackLoader.getTranslation(translationKey);
            if (packDisplayName == null) packDisplayName = fallbackName;
        }

        int nameY = panel.getY() + 16 + 4 + 7;
        guiGraphics.drawCenteredString(
            minecraft.font,
            Component.literal(packDisplayName),
            middle,
            nameY,
            0xffffffff
        );

        // Draw subtitle below pack name using Component.translatable so Minecraft handles translation
        if (focusedPack != null && focusedPack.getPackType() != null && !focusedPack.getPackType().isEmpty()) {
            String key = "bedrockskins.packType." + focusedPack.getPackType();
            guiGraphics.drawCenteredString(minecraft.font, Component.translatable(key), middle, nameY + 12, 0xFFAAAAAA);
        }
    }
    
    private void renderSkinInfo(GuiGraphics guiGraphics) {
        SkinReference ref = playerSkinWidgetList.element3.skinRef.get();
        if (ref == null) return;
        
        // FIX: Retrieve the skin using the pack ID from the reference, not the focusedPack
        // This ensures mismatch does not occur when looking at Favorites
        SkinPackAdapter pack = SkinPackAdapter.getPack(ref.packId());
        if (pack == null) return;

        LoadedSkin skin = pack.getSkin(ref.ordinal());
        if (skin == null) return;
        
        // Render skin name
        int x = tooltipBox.getX() - 5;
        int width = tooltipBox.getWidth() - 18;
        int middle = x + width / 2;
        
        String skinName = SkinPackLoader.getTranslation(skin.getSafeSkinName());
        if (skinName == null) skinName = skin.getSkinDisplayName();
        guiGraphics.drawCenteredString(minecraft.font, Component.literal(skinName), middle, panel.getY() + tooltipBox.getHeight() - 59 + 10, 0xffffffff);
        
        // Render checkmark if this skin is currently selected
        SkinId currentSkinKey = SkinManager.getLocalSelectedKey();
        if (currentSkinKey != null && java.util.Objects.equals(currentSkinKey, skin.getSkinId())) {
            //? if >=1.21.11 {
            var beaconCheck = Identifier.fromNamespaceAndPath(Legacy4J.MOD_ID, "container/beacon_check");
            //?} else {
            var beaconCheck = ResourceLocation.fromNamespaceAndPath(Legacy4J.MOD_ID, "container/beacon_check");
            //?}
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, beaconCheck,
                tooltipBox.getX() + tooltipBox.getWidth() - 50,
                panel.getY() + tooltipBox.getHeight() - 60 + 3,
                24, 24);
        }
        
        // Render heart if this skin is favorited
        if (FavoritesManager.isFavorite(skin)) {
            //? if >=1.21.11 {
            var heartContainer = Identifier.fromNamespaceAndPath("minecraft", "hud/heart/container");
            var heartFull = Identifier.fromNamespaceAndPath("minecraft", "hud/heart/full");
            //?} else {
            var heartContainer = ResourceLocation.fromNamespaceAndPath("minecraft", "hud/heart/container");
            var heartFull = ResourceLocation.fromNamespaceAndPath("minecraft", "hud/heart/full");
            //?}
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, heartContainer,
                tooltipBox.getX() + tooltipBox.getWidth() - 50 + 4,
                panel.getY() + tooltipBox.getHeight() - 60 + 30 + 4,
                16, 16);
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, heartFull,
                tooltipBox.getX() + tooltipBox.getWidth() - 50 + 4,
                panel.getY() + tooltipBox.getHeight() - 60 + 30 + 4,
                16, 16);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        boolean hovered = isInBounds(mouseX, mouseY, tooltipBox.getX(), panel.getY(), tooltipBox.getWidth(), tooltipBox.getHeight());
        if ((hovered || !ControlType.getActiveType().isKbm()) && 
            scrollableRenderer.mouseScrolled(scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void renderableVListInit() {
        addRenderableOnly((guiGraphics, i, j, f) ->
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED,
                //? if >=1.21.11 {
                Identifier.fromNamespaceAndPath(Legacy4J.MOD_ID, "tiles/square_recessed_panel"),
                //?} else {
                ResourceLocation.fromNamespaceAndPath(Legacy4J.MOD_ID, "tiles/square_recessed_panel"),
                //?}
                panel.getX() + 7, panel.getY() + 7 + 130 - 8,
                panel.getWidth() - 14, panel.getHeight() - 14 - 135 + 1 + 8));
        addRenderableOnly((guiGraphics, i, j, f) ->
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED,
                //? if >=1.21.11 {
                Identifier.fromNamespaceAndPath(Legacy4J.MOD_ID, "tiles/square_recessed_panel"),
                //?} else {
                ResourceLocation.fromNamespaceAndPath(Legacy4J.MOD_ID, "tiles/square_recessed_panel"),
                //?}
                panel.getX() + 34, panel.getY() + 10, 112, 112));
        // Removed pack icon rendering

        tooltipBox.init("tooltipBox");
        getRenderableVList().init("renderableVList", 
            panel.getX() + 11, panel.getY() + 132, 
            panel.getWidth() - 22, panel.getHeight() - 135 + 10 - 22);
    }

    void openToCurrentSkin() {
        SkinId currentSkinKey = SkinManager.getLocalSelectedKey();
        if (currentSkinKey == null) {
            // Default to Standard pack if available, else first available pack
            String defaultPackId = "skinpack.Standard";
            SkinPackAdapter defaultPack = SkinPackAdapter.getPack(defaultPackId);
            if (defaultPack != null) {
                focusedPackId = defaultPackId;
                focusedPack = defaultPack;
                queuedChangeSkinPack = true;
                updateSkinPack(0);
            } else if (!SkinPackAdapter.getAllPacks().isEmpty()) {
                String firstPackId = SkinPackAdapter.getAllPacks().keySet().iterator().next();
                focusedPackId = firstPackId;
                focusedPack = SkinPackAdapter.getPack(firstPackId);
                queuedChangeSkinPack = true;
                updateSkinPack(0);
            }
            return;
        }

        LoadedSkin currentSkin = SkinPackLoader.getLoadedSkin(currentSkinKey);
        if (currentSkin == null) {
            // Default to Standard pack if available, else first available pack
            String defaultPackId = "skinpack.Standard";
            SkinPackAdapter defaultPack = SkinPackAdapter.getPack(defaultPackId);
            if (defaultPack != null) {
                focusedPackId = defaultPackId;
                focusedPack = defaultPack;
                queuedChangeSkinPack = true;
                updateSkinPack(0);
            } else if (!SkinPackAdapter.getAllPacks().isEmpty()) {
                String firstPackId = SkinPackAdapter.getAllPacks().keySet().iterator().next();
                focusedPackId = firstPackId;
                focusedPack = SkinPackAdapter.getPack(firstPackId);
                queuedChangeSkinPack = true;
                updateSkinPack(0);
            }
            return;
        }

        String packId = currentSkin.getId();
        SkinPackAdapter pack = SkinPackAdapter.getPack(packId);
        if (pack == null) {
            // Default to Standard pack if available, else first available pack
            String defaultPackId = "skinpack.Standard";
            SkinPackAdapter defaultPack = SkinPackAdapter.getPack(defaultPackId);
            if (defaultPack != null) {
                focusedPackId = defaultPackId;
                focusedPack = defaultPack;
                queuedChangeSkinPack = true;
                updateSkinPack(0);
            } else if (!SkinPackAdapter.getAllPacks().isEmpty()) {
                String firstPackId = SkinPackAdapter.getAllPacks().keySet().iterator().next();
                focusedPackId = firstPackId;
                focusedPack = SkinPackAdapter.getPack(firstPackId);
                queuedChangeSkinPack = true;
                updateSkinPack(0);
            }
            return;
        }

        int skinIndex = pack.indexOf(currentSkin);

        focusedPackId = packId;
        focusedPack = pack;
        queuedChangeSkinPack = true;
        updateSkinPack(skinIndex);

        if (packButtons.containsKey(packId)) {
            setFocused(packButtons.get(packId));
        }
    }

    void updateSkinPack() {
        updateSkinPack(0);
    }
    
    void updateSkinPack(int index) {
        this.queuedChangeSkinPack = false;

        // Always ensure focusedPackId matches the selected button if possible
        if (focusedPackId != null && packButtons.containsKey(focusedPackId)) {
            setFocused(packButtons.get(focusedPackId));
        }

        // Clear existing widgets
        if (playerSkinWidgetList != null) {
            for (PlayerSkinWidget widget : playerSkinWidgetList.widgets) {
                widget.cleanup();
                removeWidget(widget);
            }
        }

        if (focusedPack == null || focusedPack.isEmpty()) {
            playerSkinWidgetList = null;
            return;
        }

        // Calculate visual bounds based on skinBox (the background panel)
        int boxX = tooltipBox.getX() - 5;
        int boxWidth = tooltipBox.getWidth() - 14;
        int boxY = panel.getY() + 16;
        int boxHeight = tooltipBox.getHeight() - 80;

        // The absolute visual center for the widget list
        int centerX = boxX + boxWidth / 2;
        int centerY = boxY + boxHeight / 2;

        // Determine Scissor area
        int scissorLeft = boxX + 7;
        int scissorRight = boxX + boxWidth - 5;
        int scissorTop = boxY + 4;
        int scissorBottom = boxY + boxHeight - 4;

        // Add scissor start - enable clipping for player models
        scissorStart = addRenderableOnly((guiGraphics, i, j, f) -> {
            if (playerSkinWidgetList != null) {
                guiGraphics.enableScissor(scissorLeft, scissorTop, scissorRight, scissorBottom);
            }
        });

        // Create skin widgets
        List<PlayerSkinWidget> widgets = new ArrayList<>();
        for (int i = 0; i < focusedPack.size(); i++) {
            final int skinIndex = i;
            final LoadedSkin skin = focusedPack.getSkin(i);

            // For favorites pack, we need to get the actual skin from its original pack
            // For normal packs, we can use the ordinal directly
            final SkinReference finalRef;
            if ("skinpack.Favorites".equals(focusedPackId) && skin != null) {
                // Get the skin's original pack ID and find its ordinal in that pack
                String originalPackId = skin.getId();
                SkinPackAdapter originalPack = SkinPackAdapter.getPack(originalPackId);
                int originalOrdinal = originalPack.indexOf(skin);
                finalRef = new SkinReference(originalPackId, originalOrdinal);
            } else {
                finalRef = new SkinReference(focusedPackId, skinIndex);
            }

            PlayerSkinWidget widget = addRenderableWidget(new PlayerSkinWidget(
                130, 160,
                minecraft.getEntityModels(),
                () -> finalRef
            ));
            widgets.add(widget);
        }

        // Initialize the list using the precise visual center
        // -130/2 ensures the center widget is exactly centered at centerX
        playerSkinWidgetList = PlayerSkinWidgetList.of(
            centerX - 130 / 2,
            centerY - 130 / 2,
            widgets.toArray(new PlayerSkinWidget[0])
        );
        playerSkinWidgetList.sortForIndex(index);

        // After updating the skin pack, ensure the focused button matches the pack
        if (focusedPackId != null && packButtons.containsKey(focusedPackId)) {
            setFocused(packButtons.get(focusedPackId));
        }

        // Add scissor end - disable clipping
        scissorEnd = addRenderableOnly((guiGraphics, i, j, f) -> {
            if (playerSkinWidgetList != null) {
                guiGraphics.disableScissor();
            }
        });
    }

    @Override
    protected void init() {
        super.init();
        
        if (playerSkinWidgetList != null && playerSkinWidgetList.element3 != null) {
            SkinReference ref = playerSkinWidgetList.element3.skinRef.get();
            if (ref != null) {
                updateSkinPack(ref.ordinal());
            }
        }
    }
    
    private void playUISound() {
        minecraft.getSoundManager().play(
            net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                SoundEvents.UI_BUTTON_CLICK.value(), 1.0f
            )
        );
    }
    
    private void playScrollSound() {
        minecraft.getSoundManager().play(
            net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                LegacyRegistries.SCROLL.get(), 1.0f
            )
        );
    }

    private void playFocusSound() {
        minecraft.getSoundManager().play(
            net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                LegacyRegistries.FOCUS.get(), 1.0f
            )
        );
    }

    private byte[] loadTextureData(LoadedSkin skin) {
        try {
            AssetSource src = skin.getTexture();
            if (src instanceof AssetSource.Resource) {
                var resOpt = minecraft.getResourceManager().getResource(((AssetSource.Resource) src).getId());
                if (resOpt.isPresent()) {
                    return resOpt.get().open().readAllBytes();
                }
                return new byte[0];
            } else if (src instanceof AssetSource.File) {
                File f = new File(((AssetSource.File) src).getPath());
                return Files.readAllBytes(f.toPath());
            } else if (src instanceof AssetSource.Zip) {
                AssetSource.Zip z = (AssetSource.Zip) src;
                try (ZipFile zip = new ZipFile(z.getZipPath())) {
                    ZipEntry entry = zip.getEntry(z.getInternalPath());
                    if (entry != null) {
                        try (var is = zip.getInputStream(entry)) {
                            return is.readAllBytes();
                        }
                    }
                }
            } else {
                return new byte[0];
            }
        } catch (Exception ignored) {}
        return new byte[0];
    }

    // --- Mouse drag for preview rotation ---
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubled) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (button == 0 && playerSkinWidgetList != null && playerSkinWidgetList.element3 != null) {
            PlayerSkinWidget widget = playerSkinWidgetList.element3;
            if (mouseX >= widget.getX() && mouseX < widget.getX() + widget.getWidth() &&
                mouseY >= widget.getY() && mouseY < widget.getY() + widget.getHeight()) {
                isDraggingPreview = true;
                lastMouseX = mouseX;
                lastMouseY = mouseY;
                return true;
            }
        }
        return super.mouseClicked(event, doubled);
    }

    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        int button = event.button();
        if (button == 0 && isDraggingPreview) {
            isDraggingPreview = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double deltaX, double deltaY) {
        double mouseX = event.x();
        double mouseY = event.y();
            if (isDraggingPreview && playerSkinWidgetList != null && playerSkinWidgetList.element3 != null) {
                PlayerSkinWidget widget = playerSkinWidgetList.element3;
                double delta = lastMouseX - mouseX;
                // Ignore tiny deltas to prevent drift
                if (Math.abs(delta) > 0.01) {
                    widget.onDrag(mouseX, 0, delta, 0);
                }
            lastMouseX = mouseX;
            lastMouseY = mouseY;
                return true;
            }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public void onClose() {
        if (playerSkinWidgetList != null) {
            for (PlayerSkinWidget widget : playerSkinWidgetList.widgets) {
                widget.cleanup();
            }
        }
        super.onClose();
    }
}*/
//?}
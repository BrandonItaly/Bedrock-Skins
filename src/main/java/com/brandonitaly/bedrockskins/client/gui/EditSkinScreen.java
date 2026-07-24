package com.brandonitaly.bedrockskins.client.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.brandonitaly.bedrockskins.pack.AssetSource;
import com.brandonitaly.bedrockskins.pack.LoadedSkin;
import com.brandonitaly.bedrockskins.pack.SkinPackLoader;
import com.brandonitaly.bedrockskins.util.BedrockSkinsSprites;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.RenderPipelines;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.regex.Pattern;

public class EditSkinScreen extends SkinDialogScreen {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final String packId;
    private final LoadedSkin existingSkin;
    
    private EditBox skinNameBox;
    private Button selectTextureBtn;
    private Button selectCapeBtn;

    private String selectedGeometry;
    private String newTexturePath = null;
    private String newCapePath = null;

    private LoadedSkin customGeometryPreview;
    private LoadedSkin customSlimGeometryPreview;
    private LoadedSkin currentGeometryPreview;
    
    private PreviewPlayer customGeometryPlayer;
    private PreviewPlayer customSlimGeometryPlayer;
    private PreviewPlayer currentGeometryPlayer;
    
    private final UUID customGeometryUuid = UUID.randomUUID();
    private final UUID customSlimUuid = UUID.randomUUID();
    private final UUID currentGeometryUuid = UUID.randomUUID();

    private final String widePreviewName = "Wide_" + UUID.randomUUID().toString().substring(0, 6);
    private final String slimPreviewName = "Slim_" + UUID.randomUUID().toString().substring(0, 6);
    private final String currentPreviewName = "Current_" + UUID.randomUUID().toString().substring(0, 6);

    private boolean hasCustomCurrentGeometry;
    private String currentGeometryId;

    private String skinNameValue = "";
    private Component textureButtonLabel = Component.translatable("bedrockskins.button.update_texture");
    private Component capeButtonLabel = Component.translatable("bedrockskins.button.update_cape");
    private boolean capeRemoved = false;

    private int geometrySectionX;
    private int geometrySectionY;
    private final int geometryCardsTopPadding = 18;

    public EditSkinScreen(SkinSelectionScreen parent, String packId, LoadedSkin existingSkin) {
        super(parent, Component.translatable("bedrockskins.gui.edit_skin.title"), 224, 296);
        this.packId = packId;
        this.existingSkin = existingSkin;
        this.skinNameValue = GuiSkinUtils.getSkinDisplayNameText(existingSkin);
        
        try {
            String identifier = existingSkin.geometryData.getAsJsonArray("minecraft:geometry").get(0)
                    .getAsJsonObject().getAsJsonObject("description").get("identifier").getAsString();
            this.selectedGeometry = identifier;
            this.currentGeometryId = identifier;
        } catch (Exception e) {
            this.selectedGeometry = "geometry.humanoid.custom";
            this.currentGeometryId = "geometry.humanoid.custom";
        }

        this.hasCustomCurrentGeometry = !currentGeometryId.equals("geometry.humanoid.custom") 
                                     && !currentGeometryId.equals("geometry.humanoid.customSlim");
    }

    private void refreshPreview() {
        customGeometryPreview = null;
        customSlimGeometryPreview = null;
        currentGeometryPreview = null;
        ensureGeometryOptions();
        registerGeometryPreviews();
        setupGeometryPlayers();
    }

    @Override
    protected void init() {
        int y = contentTopY();

        this.skinNameBox = new EditBox(this.font, contentLeft(), y, contentWidth(), ELEMENT_HEIGHT, Component.translatable("bedrockskins.gui.add_skin.name"));
        this.skinNameBox.setMaxLength(32);
        this.skinNameBox.setValue(skinNameValue);
        this.addRenderableWidget(this.skinNameBox);
        
        y = nextY(y); 

        ensureGeometryOptions();
        registerGeometryPreviews();
        setupGeometryPlayers();
        geometrySectionX = contentLeft();
        geometrySectionY = y;
        
        y = nextY(y, geometryCardsTopPadding + 120); 

        this.selectTextureBtn = Button.builder(Component.translatable("bedrockskins.button.update_texture"), b -> {
            String path = openFileDialog("Select New Skin Texture", "*.png");
            if (path != null) {
                newTexturePath = path;
                textureButtonLabel = Component.literal(Path.of(path).getFileName().toString());
                b.setMessage(textureButtonLabel);
                refreshPreview();
            }
        }).bounds(contentLeft(), y, contentWidth(), ELEMENT_HEIGHT).build();
        this.selectTextureBtn.setMessage(textureButtonLabel);
        this.addRenderableWidget(this.selectTextureBtn);
        
        y = nextY(y);

        if (existingSkin.cape != null) {
            int capeButtonWidth = splitButtonWidth();

            this.selectCapeBtn = Button.builder(Component.translatable("bedrockskins.button.update_cape"), b -> {
                String path = openFileDialog("Select New Cape Texture", "*.png");
                if (path != null) {
                    newCapePath = path;
                    capeRemoved = false;
                    capeButtonLabel = Component.literal(Path.of(path).getFileName().toString());
                    b.setMessage(capeButtonLabel);
                    refreshPreview();
                }
            }).bounds(contentLeft(), y, capeButtonWidth, ELEMENT_HEIGHT).build();
            this.selectCapeBtn.setMessage(capeButtonLabel);
            this.addRenderableWidget(this.selectCapeBtn);

            this.addRenderableWidget(Button.builder(Component.translatable("bedrockskins.button.remove_cape"), b -> {
                capeRemoved = true;
                newCapePath = null;
                capeButtonLabel = Component.translatable("bedrockskins.button.update_cape");
                this.selectCapeBtn.setMessage(capeButtonLabel);
                refreshPreview();
            }).bounds(splitButtonRightX(), y, capeButtonWidth, ELEMENT_HEIGHT).build());
        } else {
            this.selectCapeBtn = Button.builder(Component.translatable("bedrockskins.button.update_cape"), b -> {
                String path = openFileDialog("Select New Cape Texture", "*.png");
                if (path != null) {
                    newCapePath = path;
                    capeRemoved = false;
                    capeButtonLabel = Component.literal(Path.of(path).getFileName().toString());
                    b.setMessage(capeButtonLabel);
                    refreshPreview();
                }
            }).bounds(contentLeft(), y, contentWidth(), ELEMENT_HEIGHT).build();
            this.selectCapeBtn.setMessage(capeButtonLabel);
            this.addRenderableWidget(this.selectCapeBtn);
        }
        
        y = nextY(y); 
        
        this.addRenderableWidget(Button.builder(Component.translatable("bedrockskins.gui.edit_skin.delete"), b -> deleteSkin())
                .bounds(contentLeft(), y, contentWidth(), ELEMENT_HEIGHT).build());

        y = nextY(y); 

        int buttonWidth = splitButtonWidth();

        this.addRenderableWidget(Button.builder(Component.translatable("bedrockskins.button.cancel"), b -> this.onClose())
                .bounds(contentLeft(), y, buttonWidth, ELEMENT_HEIGHT).build());

        this.addRenderableWidget(Button.builder(Component.translatable("bedrockskins.button.save"), b -> saveSkin())
                .bounds(splitButtonRightX(), y, buttonWidth, ELEMENT_HEIGHT).build());
    }

    @Override
    public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor gui, int mouseX, int mouseY, float delta) {
        super.extractRenderState(gui, mouseX, mouseY, delta);
        renderGeometrySelector(gui, mouseX, mouseY);
    }

    private String openFileDialog(String title, String filter) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = stack.mallocPointer(1);
            filters.put(stack.UTF8(filter)).flip();
            String path = TinyFileDialogs.tinyfd_openFileDialog(title, "", filters, filter + " files", false);
            Minecraft.getInstance().execute(() -> {
                long handle = Minecraft.getInstance().getWindow().handle();
                if (handle != 0L) {
                    //? if >=26.3-fabric {
                    /*org.lwjgl.sdl.SDLVideo.SDL_RestoreWindow(handle);
                    org.lwjgl.sdl.SDLVideo.SDL_RaiseWindow(handle);*/
                    //?} else {
                    org.lwjgl.glfw.GLFW.glfwRestoreWindow(handle);
                    org.lwjgl.glfw.GLFW.glfwFocusWindow(handle);
                    //?}
                }
            });
            return path;
        }
    }

    private void saveSkin() {
        String newName = skinNameBox.getValue().trim();
        if (newName.isEmpty()) return;

        try {
            Path storeDir = SkinPackLoader.getSkinPacksDir().toPath().resolve(packId.replace("skinpack.", ""));
            if (!Files.exists(storeDir)) return;

            String internalSkinId = existingSkin.skinDisplayName; 
            String fallbackId = internalSkinId.replaceAll("[^a-zA-Z0-9_-]", "").toLowerCase();

            String textureName = null;
            if (newTexturePath != null) {
                textureName = fallbackId + ".png";
                if (existingSkin.texture instanceof AssetSource.File f) textureName = Path.of(f.path()).getFileName().toString();
                Path targetTexture = storeDir.resolve(textureName);
                Files.copy(Path.of(newTexturePath), targetTexture, StandardCopyOption.REPLACE_EXISTING);
            }

            String capeName = null;
            if (capeRemoved) {
                // Cape removed, capeName remains null
            } else if (newCapePath != null) {
                capeName = fallbackId + "_cape.png";
                if (existingSkin.cape instanceof AssetSource.File f) capeName = Path.of(f.path()).getFileName().toString();
                Path targetCape = storeDir.resolve(capeName);
                Files.copy(Path.of(newCapePath), targetCape, StandardCopyOption.REPLACE_EXISTING);
            }

            Path skinsJsonFile = storeDir.resolve("skins.json");
            if (Files.exists(skinsJsonFile)) {
                JsonObject rootObj;
                try (var reader = Files.newBufferedReader(skinsJsonFile)) {
                    rootObj = JsonParser.parseReader(reader).getAsJsonObject();
                }

                if (rootObj.has("skins")) {
                    JsonArray skinsArray = rootObj.getAsJsonArray("skins");
                    for (int i = 0; i < skinsArray.size(); i++) {
                        JsonObject skin = skinsArray.get(i).getAsJsonObject();
                        if (skin.has("localization_name") && skin.get("localization_name").getAsString().equals(internalSkinId)) {
                            skin.addProperty("geometry", selectedGeometry);
                            if (textureName != null) skin.addProperty("texture", textureName);
                            if (capeRemoved) {
                                skin.remove("cape");
                                if (existingSkin.cape instanceof AssetSource.File f) {
                                    Files.deleteIfExists(Path.of(f.path()));
                                }
                            } else if (capeName != null) {
                                skin.addProperty("cape", capeName);
                            }
                            break;
                        }
                    }
                    Files.writeString(skinsJsonFile, rootObj.toString());
                }
            }

            Path langFile = storeDir.resolve("texts").resolve("en_us.lang");
            if (Files.exists(langFile)) {
                String content = Files.readString(langFile);
                String safePackId = packId.replace("skinpack.", "");
                String langKey = "skin." + safePackId + "." + internalSkinId;
                
                if (content.matches("(?s).*^" + Pattern.quote(langKey) + "=.*")) {
                    content = content.replaceAll("(?m)^" + Pattern.quote(langKey) + "=.*$", langKey + "=" + newName);
                } else {
                    content += "\n" + langKey + "=" + newName;
                }
                Files.writeString(langFile, content);
            }

            closeAndReload();
        } catch (IOException e) {
            LOGGER.error("Failed to save skin {} in pack {}", existingSkin.skinId, packId, e);
        }
    }

    private void deleteSkin() {
        try {
            Path storeDir = SkinPackLoader.getSkinPacksDir().toPath().resolve(packId.replace("skinpack.", ""));
            if (!Files.exists(storeDir)) return;
            
            String internalSkinId = existingSkin.skinDisplayName;

            Path skinsJsonFile = storeDir.resolve("skins.json");
            if (Files.exists(skinsJsonFile)) {
                JsonObject rootObj;
                try (var reader = Files.newBufferedReader(skinsJsonFile)) {
                    rootObj = JsonParser.parseReader(reader).getAsJsonObject();
                }
                if (rootObj.has("skins")) {
                    JsonArray skinsArray = rootObj.getAsJsonArray("skins");
                    JsonArray updatedArray = new JsonArray();
                    for (JsonElement el : skinsArray) {
                        JsonObject skin = el.getAsJsonObject();
                        if (!skin.has("localization_name") || !skin.get("localization_name").getAsString().equals(internalSkinId)) {
                            updatedArray.add(skin);
                        } else {
                            if (skin.has("texture")) {
                                Files.deleteIfExists(storeDir.resolve(skin.get("texture").getAsString()));
                            }
                            if (skin.has("cape")) {
                                Files.deleteIfExists(storeDir.resolve(skin.get("cape").getAsString()));
                            }
                        }
                    }
                    rootObj.add("skins", updatedArray);
                    Files.writeString(skinsJsonFile, rootObj.toString());
                }
            }

            Path langFile = storeDir.resolve("texts").resolve("en_us.lang");
            if (Files.exists(langFile)) {
                String content = Files.readString(langFile);
                String safePackId = packId.replace("skinpack.", "");
                String langKey = "skin." + safePackId + "." + internalSkinId;
                
                content = content.replaceAll("(?m)^" + Pattern.quote(langKey) + "=.*\n?", "");
                Files.writeString(langFile, content);
            }

            closeAndReload();
        } catch (IOException e) {
            LOGGER.error("Failed to delete skin {} from pack {}", existingSkin.skinId, packId, e);
        }
    }

    private void closeAndReload() {
        this.onClose();
        Minecraft.getInstance().execute(() -> {
            if (parent instanceof SkinSelectionScreen s) {
                s.markNeedsReload();
                s.triggerReloadIfNeeded();
            }
        });
    }

    private void ensureGeometryOptions() {
        if (customGeometryPreview == null) customGeometryPreview = createGeometryPreview(widePreviewName, createGeometryData("geometry.humanoid.custom"));
        if (customSlimGeometryPreview == null) customSlimGeometryPreview = createGeometryPreview(slimPreviewName, createGeometryData("geometry.humanoid.customSlim"));
        
        if (hasCustomCurrentGeometry && currentGeometryPreview == null) {
            currentGeometryPreview = createGeometryPreview(currentPreviewName, existingSkin.geometryData.deepCopy());
        }
    }

    private LoadedSkin createGeometryPreview(String displayName, JsonObject geoData) {
        AssetSource tex = existingSkin.texture;
        if (newTexturePath != null) tex = new AssetSource.File(newTexturePath);
        AssetSource capeSource = null;
        if (!capeRemoved) {
            if (newCapePath != null) {
                capeSource = new AssetSource.File(newCapePath);
            } else {
                capeSource = existingSkin.cape;
            }
        }
        return new LoadedSkin("geometry", "Geometry", displayName, geoData, tex, capeSource);
    }

    private void selectGeometry(LoadedSkin skin) {
        if (skin == null) return;
        
        if (skin.skinDisplayName.equals(currentPreviewName)) {
            selectedGeometry = currentGeometryId;
        } else if (skin.skinDisplayName.equals(slimPreviewName)) {
            selectedGeometry = "geometry.humanoid.customSlim";
        } else {
            selectedGeometry = "geometry.humanoid.custom";
        }
        
        GuiUtils.playButtonClickSound();
        setupGeometryPlayers();
    }

    private void registerGeometryPreviews() {
        synchronized (SkinPackLoader.loadedSkins) {
            cleanupGeometryPreviews();
            SkinPackLoader.loadedSkins.put(customGeometryPreview.skinId, customGeometryPreview);
            SkinPackLoader.loadedSkins.put(customSlimGeometryPreview.skinId, customSlimGeometryPreview);
            
            if (hasCustomCurrentGeometry) {
                SkinPackLoader.loadedSkins.put(currentGeometryPreview.skinId, currentGeometryPreview);
            }
        }
        SkinPackLoader.registerTextureFor(customGeometryPreview.skinId);
        SkinPackLoader.registerTextureFor(customSlimGeometryPreview.skinId);
        if (hasCustomCurrentGeometry) SkinPackLoader.registerTextureFor(currentGeometryPreview.skinId);
    }

    private void setupGeometryPlayers() {
        if (customGeometryPlayer == null) customGeometryPlayer = PreviewPlayer.PreviewPlayerPool.get(new GameProfile(customGeometryUuid, "Wide"));
        if (customSlimGeometryPlayer == null) customSlimGeometryPlayer = PreviewPlayer.PreviewPlayerPool.get(new GameProfile(customSlimUuid, "Slim"));
        
        GuiSkinUtils.applyLoadedSkinPreview(customGeometryPlayer, customGeometryUuid, customGeometryPreview);
        GuiSkinUtils.applyLoadedSkinPreview(customSlimGeometryPlayer, customSlimUuid, customSlimGeometryPreview);

        if (hasCustomCurrentGeometry) {
            if (currentGeometryPlayer == null) currentGeometryPlayer = PreviewPlayer.PreviewPlayerPool.get(new GameProfile(currentGeometryUuid, "Current"));
            GuiSkinUtils.applyLoadedSkinPreview(currentGeometryPlayer, currentGeometryUuid, currentGeometryPreview);
        }
    }

    private void renderGeometrySelector(net.minecraft.client.gui.GuiGraphicsExtractor gui, int mouseX, int mouseY) {
        int cardY = geometrySectionY + geometryCardsTopPadding;
        int cardH = 120;
        
        int cardW = hasCustomCurrentGeometry ? 60 : 94;
        int gap = hasCustomCurrentGeometry ? 10 : 12; 
        
        int currentX = geometrySectionX;

        gui.text(font, Component.translatable("bedrockskins.gui.geometry"), geometrySectionX, geometrySectionY, 0xFFDADADA, false);
        gui.fill(geometrySectionX, geometrySectionY + 11, geometrySectionX + contentWidth(), geometrySectionY + 12, 0x33FFFFFF);

        if (hasCustomCurrentGeometry) {
            renderGeometryCard(gui, currentX, cardY, cardW, cardH, currentGeometryPlayer, Component.translatable("bedrockskins.gui.geometry.current"), selectedGeometry.equals(currentGeometryId), mouseX, mouseY);
            currentX += cardW + gap;
        }

        renderGeometryCard(gui, currentX, cardY, cardW, cardH, customGeometryPlayer, Component.translatable("bedrockskins.gui.geometry.wide"), "geometry.humanoid.custom".equals(selectedGeometry), mouseX, mouseY);
        currentX += cardW + gap;
        
        renderGeometryCard(gui, currentX, cardY, cardW, cardH, customSlimGeometryPlayer, Component.translatable("bedrockskins.gui.geometry.slim"), "geometry.humanoid.customSlim".equals(selectedGeometry), mouseX, mouseY);
    }

    private void renderGeometryCard(net.minecraft.client.gui.GuiGraphicsExtractor gui, int x, int y, int w, int h, PreviewPlayer player, Component label, boolean selected, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        var sprite = selected ? BedrockSkinsSprites.CARD_SELECTED : hovered ? BedrockSkinsSprites.CARD_HOVER : BedrockSkinsSprites.CARD_IDLE;
        gui.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, w, h);
        
        if (player != null) {
            GuiUtils.renderEntityInRect(gui, player, 0.0F, x + 2, y - 10, x + w - 2, y + 130, 112);
        }
        
        gui.centeredText(font, label, x + (w / 2), y + h - 14, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        if (handleGeometryClick((int) event.x(), (int) event.y())) return true;
        return super.mouseClicked(event, handled);
    }

    private boolean handleGeometryClick(int mouseX, int mouseY) {
        int cardY = geometrySectionY + geometryCardsTopPadding;
        int cardH = 120;
        int cardW = hasCustomCurrentGeometry ? 60 : 94;
        int gap = hasCustomCurrentGeometry ? 10 : 12;
        int currentX = geometrySectionX;

        if (hasCustomCurrentGeometry) {
            if (mouseX >= currentX && mouseX < currentX + cardW && mouseY >= cardY && mouseY < cardY + cardH) { 
                selectGeometry(currentGeometryPreview); 
                return true; 
            }
            currentX += cardW + gap;
        }

        if (mouseX >= currentX && mouseX < currentX + cardW && mouseY >= cardY && mouseY < cardY + cardH) { 
            selectGeometry(customGeometryPreview); 
            return true; 
        }
        currentX += cardW + gap;

        if (mouseX >= currentX && mouseX < currentX + cardW && mouseY >= cardY && mouseY < cardY + cardH) { 
            selectGeometry(customSlimGeometryPreview); 
            return true; 
        }
        return false;
    }

    @Override
    public void onClose() {
        cleanupGeometryPreviews();
        super.onClose();
    }

    @Override
    protected void captureDialogState() { if (skinNameBox != null) skinNameValue = skinNameBox.getValue(); }

    @Override
    protected void restoreDialogState() {
        if (skinNameBox != null) skinNameBox.setValue(skinNameValue);
        if (selectTextureBtn != null) selectTextureBtn.setMessage(textureButtonLabel);
        if (selectCapeBtn != null) selectCapeBtn.setMessage(capeButtonLabel);
        setupGeometryPlayers();
    }

    private void cleanupGeometryPreviews() {
        synchronized (SkinPackLoader.loadedSkins) {
            if (customGeometryPreview != null) { SkinPackLoader.releaseSkinAssets(customGeometryPreview.skinId); SkinPackLoader.loadedSkins.remove(customGeometryPreview.skinId); }
            if (customSlimGeometryPreview != null) { SkinPackLoader.releaseSkinAssets(customSlimGeometryPreview.skinId); SkinPackLoader.loadedSkins.remove(customSlimGeometryPreview.skinId); }
            if (hasCustomCurrentGeometry && currentGeometryPreview != null) { SkinPackLoader.releaseSkinAssets(currentGeometryPreview.skinId); SkinPackLoader.loadedSkins.remove(currentGeometryPreview.skinId); }
        }
        GuiSkinUtils.cleanupPreview(customGeometryUuid);
        GuiSkinUtils.cleanupPreview(customSlimUuid);
        GuiSkinUtils.cleanupPreview(currentGeometryUuid);
    }

    private static JsonObject createGeometryData(String geometryId) {
        JsonObject source = SkinPackLoader.vanillaGeometryJson;
        if (source != null && source.has("minecraft:geometry")) {
            JsonArray geometries = source.getAsJsonArray("minecraft:geometry");
            for (JsonElement element : geometries) {
                JsonObject geometry = element.getAsJsonObject();
                JsonObject description = geometry.getAsJsonObject("description");
                if (description != null && geometryId.equals(description.get("identifier").getAsString())) {
                    JsonObject result = new JsonObject();
                    result.addProperty("format_version", source.has("format_version") ? source.get("format_version").getAsString() : "1.12.0");
                    JsonArray selected = new JsonArray();
                    selected.add(geometry.deepCopy());
                    result.add("minecraft:geometry", selected);
                    return result;
                }
            }
        }
        return source != null ? source.deepCopy() : new JsonObject();
    }
}
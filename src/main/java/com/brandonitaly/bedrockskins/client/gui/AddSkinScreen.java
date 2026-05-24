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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.RenderPipelines;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class AddSkinScreen extends SkinDialogScreen {
    private final String packId;
    private final String texturePath;
    
    private EditBox skinNameBox;
    private Button selectCapeBtn;

    private String selectedGeometry = "geometry.humanoid.custom";
    private String capePath = null;

    private LoadedSkin customGeometryPreview;
    private LoadedSkin customSlimGeometryPreview;
    private PreviewPlayer customGeometryPlayer;
    private PreviewPlayer customSlimGeometryPlayer;
    private final UUID customGeometryUuid = UUID.randomUUID();
    private final UUID customSlimUuid = UUID.randomUUID();
    private String skinNameValue = "";
    private Component capeButtonLabel = Component.translatable("bedrockskins.button.select_cape");

    private int geometrySectionX;
    private int geometrySectionY;
    private final int geometryCardsTopPadding = 18;

    public AddSkinScreen(SkinSelectionScreen parent, String packId, String texturePath) {
        super(parent, Component.translatable("bedrockskins.gui.import_skin"), 224, 248);
        this.packId = packId;
        this.texturePath = texturePath;
    }

    @Override
    protected void init() {
        int y = contentTopY();

        this.skinNameBox = new EditBox(this.font, contentLeft(), y, contentWidth(), ELEMENT_HEIGHT, Component.translatable("bedrockskins.gui.add_skin.name"));
        this.skinNameBox.setMaxLength(32);
        this.skinNameBox.setHint(Component.translatable("bedrockskins.gui.add_skin.name.hint"));
        if (!skinNameValue.isEmpty()) {
            this.skinNameBox.setValue(skinNameValue);
        }
        this.addRenderableWidget(this.skinNameBox);
        
        y = nextY(y); 

        ensureGeometryOptions();
        registerGeometryPreviews();
        setupGeometryPlayers();
        geometrySectionX = contentLeft();
        geometrySectionY = y;
        
        y = nextY(y, geometryCardsTopPadding + 120); 

        this.selectCapeBtn = Button.builder(Component.translatable("bedrockskins.button.select_cape"), b -> {
            String path = openFileDialog("Select Cape Texture", "*.png");
            if (path != null) {
                capePath = path;
                capeButtonLabel = Component.literal(new File(path).getName());
                b.setMessage(capeButtonLabel);
            }
        }).bounds(contentLeft(), y, contentWidth(), ELEMENT_HEIGHT).build();
        this.selectCapeBtn.setMessage(capeButtonLabel);
        this.addRenderableWidget(this.selectCapeBtn);
        
        y = nextY(y); 

        int buttonWidth = splitButtonWidth();

        this.addRenderableWidget(Button.builder(Component.translatable("bedrockskins.button.cancel"), b -> this.onClose())
                .bounds(contentLeft(), y, buttonWidth, ELEMENT_HEIGHT).build());

        this.addRenderableWidget(Button.builder(Component.translatable("bedrockskins.button.confirm"), b -> addSkin())
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
            return TinyFileDialogs.tinyfd_openFileDialog(title, "", filters, filter + " files", false);
        }
    }

    private void addSkin() {
        String skinName = skinNameBox.getValue().trim();
        if (skinName.isEmpty() || texturePath == null) return;

        try {
            Path storeDir = Minecraft.getInstance().gameDirectory.toPath().resolve("skin_packs").resolve(packId.replace("skinpack.", ""));
            if (!Files.exists(storeDir)) return;

            String safeSkinId = skinName.replaceAll("[^a-zA-Z0-9_-]", "").toLowerCase();
            
            Path targetTexture = storeDir.resolve(safeSkinId + ".png");
            Files.copy(Path.of(texturePath), targetTexture, StandardCopyOption.REPLACE_EXISTING);

            String geoName = selectedGeometry;

            String capeFileName = "";
            if (capePath != null) {
                Path targetCape = storeDir.resolve(safeSkinId + "_cape.png");
                Files.copy(Path.of(capePath), targetCape, StandardCopyOption.REPLACE_EXISTING);
                capeFileName = targetCape.getFileName().toString();
            }

            File skinsJsonFile = storeDir.resolve("skins.json").toFile();
            JsonObject rootObj = new JsonObject();
            JsonArray skinsArray = new JsonArray();
            
            if (skinsJsonFile.exists()) {
                try (FileReader reader = new FileReader(skinsJsonFile)) {
                    rootObj = JsonParser.parseReader(reader).getAsJsonObject();
                    if (rootObj.has("skins")) {
                        skinsArray = rootObj.getAsJsonArray("skins");
                    }
                }
            }

            JsonObject newSkin = new JsonObject();
            newSkin.addProperty("localization_name", skinName);
            newSkin.addProperty("geometry", geoName);
            newSkin.addProperty("texture", targetTexture.getFileName().toString());
            newSkin.addProperty("type", "free");
            if (!capeFileName.isEmpty()) {
                newSkin.addProperty("cape", capeFileName);
            }
            
            skinsArray.add(newSkin);
            rootObj.add("skins", skinsArray);
            Files.writeString(skinsJsonFile.toPath(), rootObj.toString());

            Path textsDir = storeDir.resolve("texts");
            Path langFile = textsDir.resolve("en_us.lang");
            String newLangEntry = String.format("\nskin.%s.%s=%s", packId.replace("skinpack.", ""), safeSkinId, skinName);
            if (Files.exists(langFile)) {
                Files.writeString(langFile, Files.readString(langFile) + newLangEntry);
            }

            this.onClose();
            Minecraft.getInstance().execute(() -> {
                if (parent instanceof SkinSelectionScreen s) {
                    s.markNeedsReload();
                    s.triggerReloadIfNeeded();
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void ensureGeometryOptions() {
        if (customGeometryPreview == null) {
            customGeometryPreview = createGeometryPreview("Wide", "geometry.humanoid.custom");
        }
        if (customSlimGeometryPreview == null) {
            customSlimGeometryPreview = createGeometryPreview("Slim", "geometry.humanoid.customSlim");
        }
    }

    private LoadedSkin createGeometryPreview(String displayName, String geometryId) {
        return new LoadedSkin("geometry", "Geometry", displayName, createGeometryData(geometryId), new AssetSource.File(texturePath));
    }

    private void selectGeometry(LoadedSkin skin) {
        if (skin == null) return;
        selectedGeometry = "Slim".equals(skin.skinDisplayName) ? "geometry.humanoid.customSlim" : "geometry.humanoid.custom";
        GuiUtils.playButtonClickSound();
        setupGeometryPlayers();
    }

    private void registerGeometryPreviews() {
        synchronized (SkinPackLoader.loadedSkins) {
            cleanupGeometryPreviews();
            SkinPackLoader.loadedSkins.put(customGeometryPreview.skinId, customGeometryPreview);
            SkinPackLoader.loadedSkins.put(customSlimGeometryPreview.skinId, customSlimGeometryPreview);
        }
        SkinPackLoader.registerTextureFor(customGeometryPreview.skinId);
        SkinPackLoader.registerTextureFor(customSlimGeometryPreview.skinId);
    }

    private void setupGeometryPlayers() {
        if (customGeometryPlayer == null) {
            customGeometryPlayer = PreviewPlayer.PreviewPlayerPool.get(new GameProfile(customGeometryUuid, "Wide"));
        }
        if (customSlimGeometryPlayer == null) {
            customSlimGeometryPlayer = PreviewPlayer.PreviewPlayerPool.get(new GameProfile(customSlimUuid, "Slim"));
        }

        GuiSkinUtils.applyLoadedSkinPreview(customGeometryPlayer, customGeometryUuid, customGeometryPreview);
        GuiSkinUtils.applyLoadedSkinPreview(customSlimGeometryPlayer, customSlimUuid, customSlimGeometryPreview);
    }

    private void renderGeometrySelector(net.minecraft.client.gui.GuiGraphicsExtractor gui, int mouseX, int mouseY) {
        int cardY = geometrySectionY + geometryCardsTopPadding;
        int cardW = 94;
        int cardH = 120;
        int gap = 12; 
        int leftX = geometrySectionX;
        int rightX = geometrySectionX + cardW + gap;

        gui.text(font, Component.translatable("bedrockskins.gui.geometry"), geometrySectionX, geometrySectionY, 0xFFDADADA, false);
        gui.fill(geometrySectionX, geometrySectionY + 11, geometrySectionX + contentWidth(), geometrySectionY + 12, 0x33FFFFFF);

        renderGeometryCard(gui, leftX, cardY, cardW, cardH, customGeometryPlayer, Component.translatable("bedrockskins.gui.geometry.wide"), "geometry.humanoid.custom".equals(selectedGeometry), mouseX, mouseY);
        renderGeometryCard(gui, rightX, cardY, cardW, cardH, customSlimGeometryPlayer, Component.translatable("bedrockskins.gui.geometry.slim"), "geometry.humanoid.customSlim".equals(selectedGeometry), mouseX, mouseY);
    }

    private void renderGeometryCard(net.minecraft.client.gui.GuiGraphicsExtractor gui, int x, int y, int w, int h, PreviewPlayer player, Component label, boolean selected, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        var sprite = selected ? BedrockSkinsSprites.CARD_SELECTED : hovered ? BedrockSkinsSprites.CARD_HOVER : BedrockSkinsSprites.CARD_IDLE;
        gui.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, w, h);

        if (player != null) {
            int modelTop = y - 10; 
            int modelBottom = y + 130;
            GuiUtils.renderEntityInRect(gui, player, 0.0F, x + 2, modelTop, x + w - 2, modelBottom, 112);
        }

        gui.centeredText(font, label, x + (w / 2), y + h - 14, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        if (!handled && handleGeometryClick((int) event.x(), (int) event.y())) return true;
        return super.mouseClicked(event, handled);
    }

    private boolean handleGeometryClick(int mouseX, int mouseY) {
        int cardY = geometrySectionY + geometryCardsTopPadding;
        int cardW = 94;
        int cardH = 120;
        int gap = 12;
        int leftX = geometrySectionX;
        int rightX = geometrySectionX + cardW + gap;

        if (mouseX >= leftX && mouseX < leftX + cardW && mouseY >= cardY && mouseY < cardY + cardH) {
            selectGeometry(customGeometryPreview);
            return true;
        }

        if (mouseX >= rightX && mouseX < rightX + cardW && mouseY >= cardY && mouseY < cardY + cardH) {
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
    protected void captureDialogState() {
        if (skinNameBox != null) {
            skinNameValue = skinNameBox.getValue();
        }
    }

    @Override
    protected void restoreDialogState() {
        if (skinNameBox != null) {
            skinNameBox.setValue(skinNameValue);
        }
        if (selectCapeBtn != null) {
            selectCapeBtn.setMessage(capeButtonLabel);
        }
        setupGeometryPlayers();
    }

    private void cleanupGeometryPreviews() {
        synchronized (SkinPackLoader.loadedSkins) {
            if (customGeometryPreview != null) {
                SkinPackLoader.releaseSkinAssets(customGeometryPreview.skinId);
                SkinPackLoader.loadedSkins.remove(customGeometryPreview.skinId);
            }
            if (customSlimGeometryPreview != null) {
                SkinPackLoader.releaseSkinAssets(customSlimGeometryPreview.skinId);
                SkinPackLoader.loadedSkins.remove(customSlimGeometryPreview.skinId);
            }
        }
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
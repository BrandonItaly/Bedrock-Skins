package com.brandonitaly.bedrockskins.client.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

public class ImportSkinChoiceScreen extends SkinDialogScreen {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final String packId;
    private EditBox usernameBox;
    private String usernameValue = "";
    private Button importBtn;
    private Button selectFileBtn;
    
    private boolean loading = false;
    private String statusMessage = "";
    private int statusColor = 0xFFA0A0A0;

    public ImportSkinChoiceScreen(SkinSelectionScreen parent, String packId) {
        super(parent, Component.translatable("bedrockskins.gui.import_skin.choice_title"), 208, 116);
        this.packId = packId;
    }

    @Override
    protected void init() {
        int y = contentTopY();

        // Button: Select File
        this.selectFileBtn = Button.builder(Component.translatable("bedrockskins.gui.import_skin.select_file"), b -> importFromFile())
                .bounds(contentLeft(), y, contentWidth(), ELEMENT_HEIGHT).build();
        this.addRenderableWidget(this.selectFileBtn);

        // Move layout cursor past Select File button and the status text space
        y = nextY(y) + 14;

        // EditBox: Username
        this.usernameBox = new EditBox(this.font, contentLeft(), y, contentWidth(), ELEMENT_HEIGHT, Component.translatable("bedrockskins.gui.import_skin.username"));
        this.usernameBox.setMaxLength(16);
        this.usernameBox.setHint(Component.translatable("bedrockskins.gui.import_skin.username.hint"));
        if (!usernameValue.isEmpty()) {
            this.usernameBox.setValue(usernameValue);
        }
        this.addRenderableWidget(this.usernameBox);

        y = nextY(y);

        // Bottom buttons: Cancel and Import
        int buttonWidth = splitButtonWidth();

        this.addRenderableWidget(Button.builder(Component.translatable("bedrockskins.button.cancel"), b -> this.onClose())
                .bounds(contentLeft(), y, buttonWidth, ELEMENT_HEIGHT).build());

        this.importBtn = Button.builder(Component.translatable("bedrockskins.button.import"), b -> importFromUsername())
                .bounds(splitButtonRightX(), y, buttonWidth, ELEMENT_HEIGHT).build();
        this.addRenderableWidget(this.importBtn);
        
        updateWidgetStates();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.usernameBox != null) {
            boolean hasUsername = !this.usernameBox.getValue().trim().isEmpty();
            if (this.importBtn != null) {
                this.importBtn.active = hasUsername && !loading;
            }
        }
    }

    private void updateWidgetStates() {
        if (this.selectFileBtn != null) this.selectFileBtn.active = !loading;
        if (this.usernameBox != null) this.usernameBox.setEditable(!loading);
        if (this.importBtn != null) {
            this.importBtn.active = !loading && this.usernameBox != null && !this.usernameBox.getValue().trim().isEmpty();
        }
    }

    private void importFromFile() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = stack.mallocPointer(1);
            filters.put(stack.UTF8("*.png")).flip();
            String path = TinyFileDialogs.tinyfd_openFileDialog("Select Skin Texture", "", filters, "PNG files", false);
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
            if (path != null) {
                this.minecraft.gui.setScreen(new AddSkinScreen((SkinSelectionScreen) parent, packId, path));
            }
        }
    }

    private void importFromUsername() {
        if (this.usernameBox == null) return;
        String username = this.usernameBox.getValue().trim();
        if (username.isEmpty()) return;

        this.loading = true;
        this.statusMessage = "Resolving " + username + "...";
        this.statusColor = 0xFFE0E0E0;
        updateWidgetStates();

        CompletableFuture.runAsync(() -> {
            Path tempSkin = null;
            Path tempCape = null;
            try {
                HttpClient client = HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build();

                // 1. Resolve UUID
                String profileUrl = "https://api.mojang.com/users/profiles/minecraft/" + username;
                HttpRequest profileRequest = HttpRequest.newBuilder().uri(URI.create(profileUrl)).GET().build();
                HttpResponse<String> profileResponse = client.send(profileRequest, HttpResponse.BodyHandlers.ofString());

                if (profileResponse.statusCode() == 204 || profileResponse.statusCode() == 404) {
                    throw new Exception("Player not found!");
                } else if (profileResponse.statusCode() != 200) {
                    throw new Exception("Mojang API error: " + profileResponse.statusCode());
                }

                JsonObject profileJson = JsonParser.parseString(profileResponse.body()).getAsJsonObject();
                String uuid = profileJson.get("id").getAsString();
                String resolvedName = profileJson.get("name").getAsString();

                // 2. Resolve textures
                updateStatus("Fetching skin details...", 0xFFE0E0E0);
                String sessionUrl = "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid;
                HttpRequest sessionRequest = HttpRequest.newBuilder().uri(URI.create(sessionUrl)).GET().build();
                HttpResponse<String> sessionResponse = client.send(sessionRequest, HttpResponse.BodyHandlers.ofString());

                if (sessionResponse.statusCode() != 200) {
                    throw new Exception("Session API error: " + sessionResponse.statusCode());
                }

                JsonObject sessionJson = JsonParser.parseString(sessionResponse.body()).getAsJsonObject();
                JsonArray properties = sessionJson.getAsJsonArray("properties");
                String base64Value = null;
                for (JsonElement propEl : properties) {
                    JsonObject prop = propEl.getAsJsonObject();
                    if ("textures".equals(prop.get("name").getAsString())) {
                        base64Value = prop.get("value").getAsString();
                        break;
                    }
                }

                if (base64Value == null) {
                    throw new Exception("No textures property found!");
                }

                byte[] decodedBytes = Base64.getDecoder().decode(base64Value);
                String decodedJson = new String(decodedBytes, StandardCharsets.UTF_8);
                JsonObject texturesObj = JsonParser.parseString(decodedJson).getAsJsonObject()
                        .getAsJsonObject("textures");

                if (texturesObj == null || !texturesObj.has("SKIN")) {
                    throw new Exception("No skin texture found!");
                }

                JsonObject skinObj = texturesObj.getAsJsonObject("SKIN");
                String skinUrl = skinObj.get("url").getAsString();
                boolean isSlim = false;
                if (skinObj.has("metadata")) {
                    JsonObject metadata = skinObj.getAsJsonObject("metadata");
                    if (metadata.has("model") && "slim".equals(metadata.get("model").getAsString())) {
                        isSlim = true;
                    }
                }

                String capeUrl = null;
                if (texturesObj.has("CAPE")) {
                    JsonObject capeObj = texturesObj.getAsJsonObject("CAPE");
                    if (capeObj.has("url")) {
                        capeUrl = capeObj.get("url").getAsString();
                    }
                }

                // 3. Download skin
                updateStatus("Downloading skin texture...", 0xFFE0E0E0);
                tempSkin = downloadToTempFile(client, skinUrl, ".png");

                // Download cape
                if (capeUrl != null) {
                    updateStatus("Downloading cape texture...", 0xFFE0E0E0);
                    try {
                        tempCape = downloadToTempFile(client, capeUrl, ".png");
                    } catch (Exception e) {
                        LOGGER.warn("Failed to download cape for " + resolvedName, e);
                    }
                }

                // Transition to AddSkinScreen
                final Path finalTempSkin = tempSkin;
                final Path finalTempCape = tempCape;
                final boolean finalIsSlim = isSlim;
                minecraft.execute(() -> {
                    this.minecraft.gui.setScreen(new AddSkinScreen(
                            (SkinSelectionScreen) parent,
                            packId,
                            finalTempSkin.toAbsolutePath().toString(),
                            finalTempCape != null ? finalTempCape.toAbsolutePath().toString() : null,
                            resolvedName,
                            finalIsSlim,
                            true
                    ));
                });

            } catch (Exception e) {
                LOGGER.error("Failed to import skin from username", e);
                if (tempSkin != null) {
                    try { Files.deleteIfExists(tempSkin); } catch (IOException ignored) {}
                }
                if (tempCape != null) {
                    try { Files.deleteIfExists(tempCape); } catch (IOException ignored) {}
                }
                minecraft.execute(() -> {
                    this.loading = false;
                    this.statusMessage = e.getMessage();
                    this.statusColor = 0xFFFF5555;
                    updateWidgetStates();
                });
            }
        });
    }

    private void updateStatus(String message, int color) {
        minecraft.execute(() -> {
            this.statusMessage = message;
            this.statusColor = color;
        });
    }

    private Path downloadToTempFile(HttpClient client, String url, String extension) throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        Path temp = Files.createTempFile("bedrock_skins_temp_", extension);
        HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(temp));
        if (response.statusCode() != 200) {
            Files.deleteIfExists(temp);
            throw new IOException("Failed to download texture: HTTP " + response.statusCode());
        }
        return temp;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gui, int mouseX, int mouseY, float delta) {
        super.extractRenderState(gui, mouseX, mouseY, delta);
        Component text;
        int color;
        if (!statusMessage.isEmpty()) {
            text = Component.literal(statusMessage);
            color = statusColor;
        } else {
            text = Component.translatable("bedrockskins.gui.import_skin.username.default_status");
            color = 0xFFA0A0A0;
        }
        int textY = popupY() + 56;
        gui.centeredText(this.font, text, popupX() + (popupWidth() / 2), textY, color);
    }

    @Override
    protected void captureDialogState() {
        if (usernameBox != null) {
            usernameValue = usernameBox.getValue();
        }
    }

    @Override
    protected void restoreDialogState() {
        if (usernameBox != null) {
            usernameBox.setValue(usernameValue);
        }
    }
}

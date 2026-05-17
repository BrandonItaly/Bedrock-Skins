package com.brandonitaly.bedrockskins.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class EditSkinPackScreen extends SkinDialogScreen {
    private final String packId;
    private EditBox packNameBox;
    private String packNameValue = "";

    public EditSkinPackScreen(SkinSelectionScreen parent, String packId) {
        super(parent, Component.translatable("bedrockskins.gui.edit_skin_pack.title"), 208, 130);
        this.packId = packId;
    }

    @Override
    protected void init() {
        if (packNameValue.isEmpty()) {
            this.packNameValue = GuiSkinUtils.getPackDisplayName(packId, null);
        }

        int startX = popupX();
        int startY = popupY();
        int contentLeft = startX + 12;
        int contentWidth = popupWidth() - 24;
        int yOffset = startY + 28;

        this.packNameBox = new EditBox(this.font, contentLeft, yOffset, contentWidth, 20, Component.translatable("bedrockskins.gui.edit_skin_pack.name"));
        this.packNameBox.setMaxLength(32);
        this.packNameBox.setValue(packNameValue);
        this.addRenderableWidget(this.packNameBox);
        
        yOffset += 26;

        this.addRenderableWidget(Button.builder(Component.translatable("bedrockskins.gui.import_skin"), b -> importSkin())
            .bounds(contentLeft, yOffset, contentWidth, 20).build());

        yOffset += 26;

        this.addRenderableWidget(Button.builder(Component.translatable("bedrockskins.gui.edit_skin_pack.delete"), b -> deletePack())
            .bounds(contentLeft, yOffset, contentWidth, 20).build());

        yOffset += 26;

        int buttonWidth = (contentWidth - 6) / 2;
        this.addRenderableWidget(Button.builder(Component.translatable("bedrockskins.button.cancel"), b -> this.onClose())
            .bounds(contentLeft, yOffset, buttonWidth, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("bedrockskins.button.save"), b -> savePack())
            .bounds(contentLeft + buttonWidth + 6, yOffset, buttonWidth, 20).build());
    }

    private void importSkin() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = stack.mallocPointer(1);
            filters.put(stack.UTF8("*.png")).flip();
            String path = TinyFileDialogs.tinyfd_openFileDialog("Select Skin Texture", "", filters, "PNG files", false);
            if (path != null) {
                this.minecraft.setScreen(new AddSkinScreen((SkinSelectionScreen) parent, packId, path));
            }
        }
    }

    private void savePack() {
        String newName = packNameBox.getValue().trim();
        if (newName.isEmpty()) return;

        try {
            Path storeDir = Minecraft.getInstance().gameDirectory.toPath().resolve("skin_packs").resolve(packId.replace("skinpack.", ""));
            if (!Files.exists(storeDir)) return;

            Path langFile = storeDir.resolve("texts").resolve("en_us.lang");
            if (Files.exists(langFile)) {
                String content = Files.readString(langFile);
                String safeId = packId.replace("skinpack.", "");
                String langKey = "skinpack." + safeId;
                
                if (content.matches("(?s).*^" + Pattern.quote(langKey) + "=.*")) {
                    content = content.replaceAll("(?m)^" + Pattern.quote(langKey) + "=.*$", langKey + "=" + newName);
                } else {
                    content += "\n" + langKey + "=" + newName;
                }
                Files.writeString(langFile, content);
            }

            closeAndReload();
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void deletePack() {
        try {
            Path storeDir = Minecraft.getInstance().gameDirectory.toPath().resolve("skin_packs").resolve(packId.replace("skinpack.", ""));
            if (Files.exists(storeDir)) {
                try (Stream<Path> walk = Files.walk(storeDir)) {
                    walk.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
                }
            }
            closeAndReload();
        } catch (IOException e) { e.printStackTrace(); }
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
    
    @Override protected void captureDialogState() { if (packNameBox != null) packNameValue = packNameBox.getValue(); }
    @Override protected void restoreDialogState() { if (packNameBox != null) packNameBox.setValue(packNameValue); }
}
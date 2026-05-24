package com.brandonitaly.bedrockskins.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CreateSkinPackScreen extends SkinDialogScreen {
    private EditBox packNameBox;
    private String packNameValue = "";

    public CreateSkinPackScreen(SkinSelectionScreen parent) {
        super(parent, Component.translatable("bedrockskins.gui.create_skin_pack.title"), 208, 82);
    }

    @Override
    protected void init() {
        int y = contentTopY();

        this.packNameBox = new EditBox(this.font, contentLeft(), y, contentWidth(), ELEMENT_HEIGHT, Component.translatable("bedrockskins.gui.create_skin_pack.name"));
        this.packNameBox.setMaxLength(32);
        this.packNameBox.setHint(Component.translatable("bedrockskins.gui.create_skin_pack.name.hint"));
        if (!packNameValue.isEmpty()) {
            this.packNameBox.setValue(packNameValue);
        }
        this.addRenderableWidget(this.packNameBox);
        
        y = nextY(y);

        int buttonWidth = splitButtonWidth(); 
        
        this.addRenderableWidget(Button.builder(Component.translatable("bedrockskins.button.cancel"), b -> this.onClose())
            .bounds(contentLeft(), y, buttonWidth, ELEMENT_HEIGHT).build());

        this.addRenderableWidget(Button.builder(Component.translatable("bedrockskins.button.create"), b -> createPack())
            .bounds(splitButtonRightX(), y, buttonWidth, ELEMENT_HEIGHT).build());
    }

    private void createPack() {
        String packName = packNameBox.getValue().trim();
        if (packName.isEmpty()) return;

        try {
            String safePackId = packName.replaceAll("[^a-zA-Z0-9_-]", "").toLowerCase();
            Path storeDir = Minecraft.getInstance().gameDirectory.toPath().resolve("skin_packs").resolve(safePackId);
            
            if (!Files.exists(storeDir)) {
                Files.createDirectories(storeDir);

                String skinsJson = String.format("""
                {
                    "skins": [],
                    "serialize_name": "%s",
                    "localization_name": "%s",
                    "pack_type": "custom"
                }
                """, safePackId, packName);
                
                Files.writeString(storeDir.resolve("skins.json"), skinsJson);

                Path textsDir = storeDir.resolve("texts");
                Files.createDirectories(textsDir);
                Files.writeString(textsDir.resolve("en_us.lang"), String.format("skinpack.%s=%s\n", safePackId, packName));
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

    @Override
    protected void captureDialogState() {
        if (packNameBox != null) {
            packNameValue = packNameBox.getValue();
        }
    }

    @Override
    protected void restoreDialogState() {
        if (packNameBox != null) {
            packNameBox.setValue(packNameValue);
        }
    }
}
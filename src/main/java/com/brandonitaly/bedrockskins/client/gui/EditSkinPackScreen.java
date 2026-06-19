package com.brandonitaly.bedrockskins.client.gui;

import com.mojang.logging.LogUtils;
import com.brandonitaly.bedrockskins.util.ExternalAssetUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

public class EditSkinPackScreen extends SkinDialogScreen {
    private static final Logger LOGGER = LogUtils.getLogger();

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

        int y = contentTopY();

        this.packNameBox = new EditBox(this.font, contentLeft(), y, contentWidth(), ELEMENT_HEIGHT, Component.translatable("bedrockskins.gui.edit_skin_pack.name"));
        this.packNameBox.setMaxLength(32);
        this.packNameBox.setValue(packNameValue);
        this.addRenderableWidget(this.packNameBox);
        
        y = nextY(y);

        this.addRenderableWidget(Button.builder(Component.translatable("bedrockskins.gui.import_skin"), b -> importSkin())
            .bounds(contentLeft(), y, contentWidth(), ELEMENT_HEIGHT).build());

        y = nextY(y);

        this.addRenderableWidget(Button.builder(Component.translatable("bedrockskins.gui.edit_skin_pack.delete"), b -> deletePack())
            .bounds(contentLeft(), y, contentWidth(), ELEMENT_HEIGHT).build());

        y = nextY(y);

        int buttonWidth = splitButtonWidth();
        this.addRenderableWidget(Button.builder(Component.translatable("bedrockskins.button.cancel"), b -> this.onClose())
            .bounds(contentLeft(), y, buttonWidth, ELEMENT_HEIGHT).build());

        this.addRenderableWidget(Button.builder(Component.translatable("bedrockskins.button.save"), b -> savePack())
            .bounds(splitButtonRightX(), y, buttonWidth, ELEMENT_HEIGHT).build());
    }

    private void importSkin() {
        this.minecraft.setScreen(new ImportSkinChoiceScreen((SkinSelectionScreen) parent, packId));
    }

    private void savePack() {
        String newName = packNameBox.getValue().trim();
        if (newName.isEmpty()) return;

        try {
            Path storeDir = com.brandonitaly.bedrockskins.pack.SkinPackLoader.getSkinPacksDir().toPath().resolve(packId.replace("skinpack.", ""));
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
        } catch (IOException e) {
            LOGGER.error("Failed to save skin pack {}", packId, e);
        }
    }

    private void deletePack() {
        ExternalAssetUtil.deletePack(packId);
        closeAndReload();
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
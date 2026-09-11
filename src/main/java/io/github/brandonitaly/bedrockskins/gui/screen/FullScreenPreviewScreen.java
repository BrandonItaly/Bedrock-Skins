package io.github.brandonitaly.bedrockskins.gui.screen;

import com.mojang.authlib.GameProfile;
import io.github.brandonitaly.bedrockskins.client.appearance.cape.CapeManager.MinecraftCape;
import io.github.brandonitaly.bedrockskins.client.appearance.emote.EmoteManager;
import io.github.brandonitaly.bedrockskins.client.appearance.persona.PersonaManager;
import io.github.brandonitaly.bedrockskins.gui.preview.GuiSkinUtils;
import io.github.brandonitaly.bedrockskins.gui.preview.GuiUtils;
import io.github.brandonitaly.bedrockskins.gui.preview.PreviewPlayer;
import io.github.brandonitaly.bedrockskins.pack.model.LoadedCosmetic;
import io.github.brandonitaly.bedrockskins.pack.model.LoadedEmote;
import io.github.brandonitaly.bedrockskins.pack.model.LoadedSkin;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.UUID;

/** Full-window, rotatable view of the appearance currently shown in the wardrobe preview. */
public final class FullScreenPreviewScreen extends Screen {
    private final Screen parent;
    private final LoadedSkin selectedSkin;
    private final LoadedCosmetic selectedCosmetic;
    private final MinecraftCape selectedCape;
    private final LoadedEmote selectedEmote;
    private final UUID previewUuid = UUID.randomUUID();
    private PreviewPlayer previewPlayer;
    private float rotation;
    private int lastMouseX;
    private boolean dragging;
    private boolean cleanedUp;

    public FullScreenPreviewScreen(Screen parent, LoadedSkin selectedSkin,
                                   LoadedCosmetic selectedCosmetic, MinecraftCape selectedCape,
                                   LoadedEmote selectedEmote, float rotation) {
        super(Component.translatable("bedrockskins.gui.preview"));
        this.parent = parent;
        this.selectedSkin = selectedSkin;
        this.selectedCosmetic = selectedCosmetic;
        this.selectedCape = selectedCape;
        this.selectedEmote = selectedEmote;
        this.rotation = rotation;
    }

    @Override
    protected void init() {
        super.init();
        String name = minecraft.player != null ? minecraft.player.getName().getString() : "Preview";
        previewPlayer = new PreviewPlayer(new GameProfile(previewUuid, name));

        if (selectedSkin != null) {
            GuiSkinUtils.applyLoadedSkinPreview(previewPlayer, previewUuid, selectedSkin, false);
        } else {
            GuiSkinUtils.applyCurrentEquippedSkin(minecraft, previewPlayer, previewUuid);
        }
        if (selectedCosmetic != null) PersonaManager.setPreviewWithEquipped(previewUuid, selectedCosmetic);
        else PersonaManager.setPreviewFromLocal(previewUuid);

        if (selectedCape != null) {
            previewPlayer.setForcedCape("none".equals(selectedCape.id) ? null : selectedCape.textureIdentifier);
        }

        if (selectedEmote != null) EmoteManager.play(previewUuid, selectedEmote);
        else if (selectedCosmetic != null) EmoteManager.playDressingRoom(previewUuid, selectedCosmetic.type);
        else if (selectedCape != null) EmoteManager.playDressingRoom(previewUuid, "persona_back");

        int buttonWidth = Math.min(160, Math.max(80, width - 32));
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
            .bounds((width - buttonWidth) / 2, height - 28, buttonWidth, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gui, int mouseX, int mouseY, float delta) {
        if (dragging) rotation -= (mouseX - lastMouseX) * 0.5F;
        lastMouseX = mouseX;

        gui.centeredText(font, title, width / 2, 10, 0xFFFFFFFF);
        if (previewPlayer != null) {
            int horizontalMargin = Math.max(8, width / 12);
            GuiUtils.renderEntityInRect(gui, previewPlayer, rotation * 3.0F,
                horizontalMargin, 24, width - horizontalMargin, height - 34,
                Math.max(80, Math.min(width, height)));
        }
        super.extractRenderState(gui, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (isLeftButton(event.button()) && event.y() >= 24 && event.y() < height - 34) {
            dragging = true;
            lastMouseX = (int) event.x();
            return true;
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (isLeftButton(event.button()) && dragging) {
            dragging = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    private static boolean isLeftButton(int button) {
        return button == 0 || button == InputConstants.MOUSE_BUTTON_LEFT;
    }

    @Override
    public void onClose() {
        if (parent instanceof SkinSelectionScreen wardrobe) {
            wardrobe.restorePreviewAfterFullScreen(rotation);
        }
        minecraft.gui.setScreen(parent);
    }

    @Override
    public void removed() {
        if (!cleanedUp) {
            cleanedUp = true;
            GuiSkinUtils.cleanupPreview(previewUuid);
            PersonaManager.clearPreview(previewUuid);
            EmoteManager.stop(previewUuid);
            previewPlayer = null;
        }
        super.removed();
    }
}

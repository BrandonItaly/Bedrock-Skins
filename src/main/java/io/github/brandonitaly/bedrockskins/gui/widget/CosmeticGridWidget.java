package io.github.brandonitaly.bedrockskins.gui.widget;

import io.github.brandonitaly.bedrockskins.gui.preview.*;
import io.github.brandonitaly.bedrockskins.gui.screen.*;

import io.github.brandonitaly.bedrockskins.client.appearance.persona.PersonaManager;
import io.github.brandonitaly.bedrockskins.pack.model.LoadedCosmetic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import io.github.brandonitaly.bedrockskins.gui.preview.PreviewPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class CosmeticGridWidget extends PersonaPreviewGridWidget<LoadedCosmetic> {
    private static final Identifier PERSONA_PREVIEW_TEXTURE = Identifier.fromNamespaceAndPath(
        "bedrockskins", "textures/entity/persona.png");

    public CosmeticGridWidget(Minecraft client, int width, int height, int y, int itemHeight,
                              Consumer<LoadedCosmetic> onSelect, Supplier<LoadedCosmetic> selected, Font font) {
        super(client, width, height, y, itemHeight, 60, onSelect, selected, font);
    }

    public void addCosmeticsRow(List<LoadedCosmetic> cosmetics) {
        addValuesRow(cosmetics);
    }

    @Override
    protected String id(LoadedCosmetic cosmetic) {
        return cosmetic.id;
    }

    @Override
    protected Component name(LoadedCosmetic cosmetic) {
        return Component.literal(cosmetic.displayName);
    }

    @Override
    protected void initializePreview(PreviewPlayer player, LoadedCosmetic cosmetic) {
        PersonaManager.setPreview(player.getUuid(), cosmetic);
    }

    @Override
    protected void initializePlayerAppearance(LoadedCosmetic cosmetic, PreviewPlayer player) {
        player.clearForcedProfileSkin();
        player.setForcedBody(PERSONA_PREVIEW_TEXTURE);
        player.setForcedModel(PlayerModelType.WIDE);
        player.setForcedCape(null);
    }

    @Override
    protected boolean isEquipped(LoadedCosmetic cosmetic) {
        return PersonaManager.isLocallyEquipped(cosmetic);
    }

    @Override
    protected void renderPaperDoll(LoadedCosmetic cosmetic, PreviewPlayer preview,
                                   GuiGraphicsExtractor graphics, int x, int y,
                                   int width, int height) {
        GuiUtils.renderCosmeticInRect(graphics, preview, cosmetic.type,
            x + 1, y + 1, x + width - 1, y + height - 1);
    }
}

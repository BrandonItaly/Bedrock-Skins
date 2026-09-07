package io.github.brandonitaly.bedrockskins.gui.widget;

import io.github.brandonitaly.bedrockskins.gui.preview.*;
import io.github.brandonitaly.bedrockskins.gui.screen.*;

import io.github.brandonitaly.bedrockskins.client.appearance.persona.PersonaManager;
import io.github.brandonitaly.bedrockskins.pack.model.LoadedCosmetic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class CosmeticGridWidget extends PersonaPreviewGridWidget<LoadedCosmetic> {
    public CosmeticGridWidget(Minecraft client, int width, int height, int y, int itemHeight,
                              Consumer<LoadedCosmetic> onSelect, Supplier<LoadedCosmetic> selected, Font font) {
        super(client, width, height, y, itemHeight, onSelect, selected, font);
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
    protected void initializePreview(UUID uuid, LoadedCosmetic cosmetic) {
        PersonaManager.setPreview(uuid, cosmetic);
    }

    @Override
    protected void cleanupPreview(UUID uuid, LoadedCosmetic cosmetic) {
        PersonaManager.clearPreview(uuid);
    }

    @Override
    protected boolean isEquipped(LoadedCosmetic cosmetic) {
        return PersonaManager.isLocallyEquipped(cosmetic);
    }
}

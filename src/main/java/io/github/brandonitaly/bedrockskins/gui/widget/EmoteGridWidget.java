package io.github.brandonitaly.bedrockskins.gui.widget;

import io.github.brandonitaly.bedrockskins.gui.preview.*;
import io.github.brandonitaly.bedrockskins.gui.screen.*;

import io.github.brandonitaly.bedrockskins.client.appearance.emote.EmoteManager;
import io.github.brandonitaly.bedrockskins.pack.model.LoadedEmote;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** A grid of looping paper-doll previews for discovered Persona emotes. */
public final class EmoteGridWidget extends PersonaPreviewGridWidget<LoadedEmote> {
    public EmoteGridWidget(Minecraft client, int width, int height, int y, int itemHeight,
                           Consumer<LoadedEmote> onSelect, Supplier<LoadedEmote> selected, Font font) {
        super(client, width, height, y, itemHeight, onSelect, selected, font);
    }

    public void addEmotesRow(List<LoadedEmote> emotes) {
        addValuesRow(emotes);
    }

    @Override
    protected String id(LoadedEmote emote) {
        return emote.id();
    }

    @Override
    protected Component name(LoadedEmote emote) {
        return Component.literal(emote.displayName());
    }

    @Override
    protected void initializePreview(UUID uuid, LoadedEmote emote) {
        EmoteManager.play(uuid, emote);
    }

    @Override
    protected void cleanupPreview(UUID uuid, LoadedEmote emote) {
        EmoteManager.stop(uuid);
    }

    @Override
    protected void beforeRender(UUID uuid, LoadedEmote emote) {
        if (!EmoteManager.isPlaying(uuid)) {
            EmoteManager.play(uuid, emote);
        }
    }
}

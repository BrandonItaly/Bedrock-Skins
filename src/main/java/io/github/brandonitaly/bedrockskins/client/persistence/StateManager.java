package io.github.brandonitaly.bedrockskins.client.persistence;

import java.io.File;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;

public final class StateManager {
    private StateManager() {}

    private static final File stateFile = new File(Minecraft.getInstance().gameDirectory, "bedrock_skins_state.json");
    private static LocalSkinConfig cachedState;

    public static synchronized LocalSkinConfig readState() {
        if (cachedState == null) {
            cachedState = JsonCodecFileStore.read(stateFile.toPath(), LocalSkinConfig.CODEC, LocalSkinConfig.DEFAULT, "StateManager");
        }
        return cachedState;
    }

    public static synchronized void updateFavorites(List<String> favorites) {
        LocalSkinConfig existing = readState();
        save(existing.withFavorites(favorites));
    }

    public static synchronized void updateSelectedSkin(String selected) {
        LocalSkinConfig existing = readState();
        save(existing.withSelectedSkin(selected));
    }

    public static synchronized void updateSelection(String selected, String selectedCape) {
        LocalSkinConfig existing = readState();
        save(existing.withSelection(selected, selectedCape));
    }

    public static synchronized void updateCosmetics(List<String> cosmetics) {
        save(readState().withSelectedCosmetics(cosmetics));
    }

    public static synchronized void updatePersonaColors(Map<String, Integer> colors) {
        save(readState().withPersonaColors(colors));
    }

    public static synchronized void updateSelectedEmote(String emote) {
        save(readState().withSelectedEmote(emote));
    }

    public static synchronized void updateEmoteSlots(List<String> slots) {
        save(readState().withEmoteSlots(slots));
    }

    private static void save(LocalSkinConfig state) {
        cachedState = state;
        JsonCodecFileStore.write(stateFile.toPath(), LocalSkinConfig.CODEC, state, "StateManager");
    }
}

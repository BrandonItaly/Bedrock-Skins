package com.brandonitaly.bedrockskins.client;

import java.io.File;
import java.util.List;
import net.minecraft.client.Minecraft;

public final class StateManager {
    private StateManager() {}

    private static final File stateFile = new File(Minecraft.getInstance().gameDirectory, "bedrock_skins_state.json");
    private static volatile LocalSkinConfig cachedState = null;

    public static LocalSkinConfig readState() {
        if (cachedState == null) {
            cachedState = JsonCodecFileStore.read(stateFile.toPath(), LocalSkinConfig.CODEC, LocalSkinConfig.DEFAULT, "StateManager");
        }
        return cachedState;
    }

    public static void saveState(List<String> favorites, String selected) {
        LocalSkinConfig existing = readState();
        saveState(favorites, selected, existing.selectedCape());
    }

    public static void saveState(List<String> favorites, String selected, String selectedCape) {
        LocalSkinConfig state = new LocalSkinConfig(favorites, selected, selectedCape);
        cachedState = state;
        JsonCodecFileStore.writeAtomic(stateFile.toPath(), LocalSkinConfig.CODEC, state, "StateManager");
    }
}

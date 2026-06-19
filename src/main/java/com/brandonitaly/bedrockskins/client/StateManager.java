package com.brandonitaly.bedrockskins.client;

import java.io.File;
import java.util.List;
import net.minecraft.client.Minecraft;

public final class StateManager {
    private StateManager() {}

    private static final File stateFile = new File(Minecraft.getInstance().gameDirectory, "bedrock_skins_state.json");

    public static LocalSkinConfig readState() {
        return JsonCodecFileStore.read(stateFile.toPath(), LocalSkinConfig.CODEC, LocalSkinConfig.DEFAULT, "StateManager");
    }

    public static void saveState(List<String> favorites, String selected) {
        LocalSkinConfig existing = readState();
        LocalSkinConfig state = new LocalSkinConfig(favorites, selected, existing.selectedCape());
        JsonCodecFileStore.writeAtomic(stateFile.toPath(), LocalSkinConfig.CODEC, state, "StateManager");
    }

    public static void saveState(List<String> favorites, String selected, String selectedCape) {
        LocalSkinConfig state = new LocalSkinConfig(favorites, selected, selectedCape);
        JsonCodecFileStore.writeAtomic(stateFile.toPath(), LocalSkinConfig.CODEC, state, "StateManager");
    }
}

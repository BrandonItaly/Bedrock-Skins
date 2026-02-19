package com.brandonitaly.bedrockskins.client;

import java.io.File;
import java.util.List;
import net.minecraft.client.Minecraft;

public final class StateManager {
    private StateManager() {}

    private static final File stateFile = new File(Minecraft.getInstance().gameDirectory, "bedrock_skins_state.json");

    public static BedrockSkinsState readState() {
        return JsonCodecFileStore.read(stateFile.toPath(), BedrockSkinsState.CODEC, BedrockSkinsState.DEFAULT, "StateManager");
    }

    public static void saveState(List<String> favorites, String selected) {
        BedrockSkinsState state = new BedrockSkinsState(favorites, selected);
        JsonCodecFileStore.writeAtomic(stateFile.toPath(), BedrockSkinsState.CODEC, state, "StateManager");
    }
}

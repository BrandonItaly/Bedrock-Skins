package com.brandonitaly.bedrockskins.client;

import com.google.gson.Gson;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Collections;
import java.util.List;

public final class StateManager {
    private StateManager() {}

    private static final Gson gson = new Gson();
    private static final File stateFile = new File(MinecraftClient.getInstance().runDirectory, "bedrock_skins_state.json");

    public static BedrockSkinsState readState() {
        try {
            if (!stateFile.exists()) return new BedrockSkinsState(Collections.emptyList(), null);
            try (FileReader reader = new FileReader(stateFile)) {
                BedrockSkinsState st = gson.fromJson(reader, BedrockSkinsState.class);
                return st == null ? new BedrockSkinsState(Collections.emptyList(), null) : st;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new BedrockSkinsState(Collections.emptyList(), null);
        }
    }

    public static void saveState(List<String> favorites, String selected) {
        try {
            File tmp = new File(stateFile.getAbsolutePath() + ".tmp");
            try (FileWriter writer = new FileWriter(tmp)) {
                gson.toJson(new BedrockSkinsState(favorites, selected), writer);
            }
            if (tmp.exists()) {
                if (stateFile.exists()) stateFile.delete();
                tmp.renameTo(stateFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

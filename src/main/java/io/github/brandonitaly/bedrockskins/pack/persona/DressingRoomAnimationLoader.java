package io.github.brandonitaly.bedrockskins.pack.persona;

import io.github.brandonitaly.bedrockskins.pack.model.LoadedEmote;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Loads the vanilla Bedrock dressing-room reaction animations bundled with the mod. */
public final class DressingRoomAnimationLoader {
    private static final String ROOT = "/assets/bedrockskins/animations/dressing_room/";
    private static final Map<String, List<String>> FILES = Map.of(
        "arm", List.of("dressing_room_react_arm_1.anim.json", "dressing_room_react_arm_2.anim.json"),
        "back", List.of("dressing_room_react_back_1.anim.json", "dressing_room_react_back_2.anim.json"),
        "bottom", List.of("dressing_room_react_bottom_1.anim.json", "dressing_room_react_bottom_2.anim.json",
            "dressing_room_react_bottom_3.anim.json"),
        "head", List.of("dressing_room_react_head_1.anim.json", "dressing_room_react_head_2.anim.json"),
        "torso", List.of("dressing_room_react_torso_1.anim.json", "dressing_room_react_torso_2.anim.json")
    );

    private DressingRoomAnimationLoader() {}

    public static Map<String, List<LoadedEmote>> load() {
        Map<String, List<LoadedEmote>> result = new LinkedHashMap<>();
        FILES.forEach((group, files) -> {
            List<LoadedEmote> animations = new ArrayList<>();
            for (String file : files) {
                LoadedEmote animation = load(group, file);
                if (animation != null) animations.add(animation);
            }
            if (!animations.isEmpty()) result.put(group, List.copyOf(animations));
        });
        return Map.copyOf(result);
    }

    private static LoadedEmote load(String group, String file) {
        try (var stream = DressingRoomAnimationLoader.class.getResourceAsStream(ROOT + file)) {
            if (stream == null) return null;
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                .getAsJsonObject();
            JsonObject animations = root.getAsJsonObject("animations");
            if (animations == null || animations.isEmpty()) return null;
            String name = animations.keySet().iterator().next();
            JsonObject animation = animations.getAsJsonObject(name);
            float duration = animation.has("animation_length")
                ? animation.get("animation_length").getAsFloat() : maxTime(animation);
            if (duration <= 0.0F) return null;
            return new LoadedEmote("dressing_room/" + file, group, name, animation.deepCopy(), duration);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static float maxTime(JsonElement value) {
        float max = 0.0F;
        if (value == null) return max;
        if (value.isJsonObject()) {
            for (var entry : value.getAsJsonObject().entrySet()) {
                try { max = Math.max(max, Float.parseFloat(entry.getKey())); }
                catch (NumberFormatException ignored) {}
                max = Math.max(max, maxTime(entry.getValue()));
            }
        } else if (value.isJsonArray()) {
            for (JsonElement child : value.getAsJsonArray()) max = Math.max(max, maxTime(child));
        }
        return max;
    }
}

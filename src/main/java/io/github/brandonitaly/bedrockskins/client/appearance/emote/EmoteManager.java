package io.github.brandonitaly.bedrockskins.client.appearance.emote;

import io.github.brandonitaly.bedrockskins.client.appearance.skin.ClientSkinSync;
import io.github.brandonitaly.bedrockskins.client.persistence.LocalSkinConfig;
import io.github.brandonitaly.bedrockskins.client.persistence.StateManager;
import io.github.brandonitaly.bedrockskins.client.render.model.BedrockPlayerModel;

import io.github.brandonitaly.bedrockskins.pack.model.LoadedEmote;
import io.github.brandonitaly.bedrockskins.pack.persona.PersonaEmoteLoader;
import io.github.brandonitaly.bedrockskins.pack.persona.PersonaResourceLoader;
import io.github.brandonitaly.bedrockskins.pack.persona.DressingRoomAnimationLoader;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;
import org.slf4j.Logger;

/** Discovers, selects, and evaluates Bedrock Persona emotes. */
public final class EmoteManager {
    public static final int SLOT_COUNT = 6;
    private static final float PLAYER_GROUND_PIVOT_Y = 24.0F;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static final Map<String, LoadedEmote> EMOTES = new LinkedHashMap<>();
    private static final Map<String, Thumbnail> THUMBNAILS = new LinkedHashMap<>();
    private static final Map<UUID, Playback> PLAYING = new ConcurrentHashMap<>();
    private static Map<String, List<LoadedEmote>> DRESSING_ROOM = Map.of();
    private static final Map<ModelPart, Delta> LAST_DELTAS = new IdentityHashMap<>();
    private static final String[] EQUIPPED_SLOTS = new String[SLOT_COUNT];

    private EmoteManager() {}

    public static void reload() {
        var textureManager = Minecraft.getInstance().getTextureManager();
        THUMBNAILS.values().forEach(thumbnail -> textureManager.release(thumbnail.texture()));
        THUMBNAILS.clear();
        EMOTES.clear();
        DRESSING_ROOM = DressingRoomAnimationLoader.load();
        Path personaDirectory = Minecraft.getInstance().gameDirectory.toPath().resolve("persona");
        try {
            Files.createDirectories(personaDirectory);
        } catch (Exception e) {
            LOGGER.warn("Failed to create Persona directory {}", personaDirectory, e);
        }
        scan(personaDirectory);
        PersonaResourceLoader.forEachBundledRoot(Minecraft.getInstance().getResourceManager(),
            EmoteManager::scan);
        EMOTES.values().forEach(EmoteManager::registerThumbnail);
        LocalSkinConfig state = StateManager.readState();
        java.util.Arrays.fill(EQUIPPED_SLOTS, null);
        List<String> savedSlots = state.emoteSlots();
        if (!savedSlots.isEmpty()) {
            for (int i = 0; i < Math.min(SLOT_COUNT, savedSlots.size()); i++) {
                String id = savedSlots.get(i);
                if (id != null && !id.isBlank() && EMOTES.containsKey(id)) EQUIPPED_SLOTS[i] = id;
            }
        } else if (!EMOTES.isEmpty()) {
            int slot = 0;
            for (String id : EMOTES.keySet()) {
                if (slot >= SLOT_COUNT) break;
                EQUIPPED_SLOTS[slot++] = id;
            }
            persistSlots();
        }
        LOGGER.debug("Loaded {} Persona emote(s)", EMOTES.size());
    }

    private static void scan(Path root) {
        if (!Files.isDirectory(root)) return;
        try (Stream<Path> paths = Files.walk(root, 3)) {
            paths.filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".meta.json"))
                .map(Path::getParent)
                .distinct()
                .forEach(path -> PersonaEmoteLoader.load(path.toFile())
                    .ifPresent(emote -> EMOTES.putIfAbsent(emote.id(), emote)));
        } catch (Exception ignored) {}
    }

    public static List<LoadedEmote> all() { return List.copyOf(EMOTES.values()); }
    public static Thumbnail thumbnail(LoadedEmote emote) {
        return emote == null ? null : THUMBNAILS.get(emote.id());
    }

    public record Thumbnail(Identifier texture, int width, int height) {}

    private static void registerThumbnail(LoadedEmote emote) {
        byte[] data = emote.thumbnailData();
        if (data.length == 0) return;
        try {
            NativeImage image = NativeImage.read(new ByteArrayInputStream(data));
            int width = image.getWidth();
            int height = image.getHeight();
            Identifier id = Identifier.fromNamespaceAndPath("bedrockskins",
                "persona/emote_thumbnail/" + emote.id().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/._-]", "_"));
            DynamicTexture texture = new DynamicTexture(() -> "persona_emote_thumbnail", image);
            Runnable registerTask = () -> {
                texture.upload();
                Minecraft.getInstance().getTextureManager().register(id, texture);
                THUMBNAILS.put(emote.id(), new Thumbnail(id, width, height));
            };
            if (Minecraft.getInstance().isSameThread()) {
                registerTask.run();
            } else {
                Minecraft.getInstance().execute(registerTask);
            }
        } catch (Exception exception) {
            LOGGER.warn("Failed to register Persona emote thumbnail {}", emote.displayName(), exception);
        }
    }

    public static LoadedEmote slot(int index) {
        return index >= 0 && index < SLOT_COUNT ? EMOTES.get(EQUIPPED_SLOTS[index]) : null;
    }

    public static void equip(int index, LoadedEmote emote) {
        if (index < 0 || index >= SLOT_COUNT || emote == null || !EMOTES.containsKey(emote.id())) return;
        int previousSlot = -1;
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (emote.id().equals(EQUIPPED_SLOTS[i])) {
                previousSlot = i;
                break;
            }
        }
        if (previousSlot >= 0 && previousSlot != index) {
            EQUIPPED_SLOTS[previousSlot] = EQUIPPED_SLOTS[index];
        }
        EQUIPPED_SLOTS[index] = emote.id();
        persistSlots();
    }

    public static void unequip(int index) {
        if (index < 0 || index >= SLOT_COUNT || EQUIPPED_SLOTS[index] == null) return;
        EQUIPPED_SLOTS[index] = null;
        persistSlots();
    }

    private static void persistSlots() {
        List<String> ids = new ArrayList<>(SLOT_COUNT);
        for (String id : EQUIPPED_SLOTS) ids.add(id == null ? "" : id);
        StateManager.updateEmoteSlots(ids);
    }

    public static void playLocal(LoadedEmote emote) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || emote == null) {
            LOGGER.warn("Cannot play Persona emote: player={}, emote={}", client.player != null, emote);
            return;
        }
        play(client.player.getUUID(), emote);
        ClientSkinSync.sendEmote(emote);
    }

    public static void play(UUID playerId, LoadedEmote emote) {
        if (playerId != null && emote != null) {
            PLAYING.put(playerId, new Playback(emote, System.nanoTime()));
        }
    }

    /** Plays a local paper-doll reaction without selecting, equipping, or syncing an emote. */
    public static void playDressingRoom(UUID playerId, String cosmeticType) {
        String group = dressingRoomGroup(cosmeticType);
        List<LoadedEmote> reactions = group == null ? null : DRESSING_ROOM.get(group);
        if (playerId == null || reactions == null || reactions.isEmpty()) return;
        play(playerId, reactions.get(ThreadLocalRandom.current().nextInt(reactions.size())));
    }

    private static String dressingRoomGroup(String type) {
        if (type == null) return null;
        return switch (type) {
            case "persona_arms", "persona_hand" -> "arm";
            case "persona_back" -> "back";
            case "persona_bottom", "persona_high_pants", "persona_legs", "persona_feet" -> "bottom";
            case "persona_head", "persona_hood", "persona_hair", "persona_facial_hair",
                 "persona_eyes", "persona_mouth", "persona_face_accessory" -> "head";
            case "persona_top", "persona_outerwear", "persona_skin" -> "torso";
            default -> null;
        };
    }

    public static boolean isPlaying(UUID playerId) { return playerId != null && PLAYING.containsKey(playerId); }
    public static void stop(UUID playerId) { if (playerId != null) PLAYING.remove(playerId); }

    public static void stopLocal() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && PLAYING.remove(client.player.getUUID()) != null) {
            ClientSkinSync.sendStopEmote();
        }
    }

    public static void tick() {
        long now = System.nanoTime();
        PLAYING.entrySet().removeIf(entry ->
            (now - entry.getValue().startedNanos) / 1_000_000_000.0f > entry.getValue().emote.duration());
    }

    public static void playRemote(UUID playerId, String id, String name, String animationName,
                                  String animationJson, float duration) {
        if (io.github.brandonitaly.bedrockskins.network.BedrockSkinsNetworking.STOP_EMOTE_ID.equals(id)) {
            stop(playerId);
            return;
        }
        try {
            JsonObject animation = GSON.fromJson(animationJson, JsonObject.class);
            if (animation != null && duration > 0.0f)
                play(playerId, new LoadedEmote(id, name, animationName, animation, duration));
        } catch (Exception ignored) {}
    }

    public static void clear() { PLAYING.clear(); }

    /** Remove an emote delta before an external caller resets or copies this model's pose. */
    public static void restorePoseDeltas(HumanoidModel<?> model) {
        if (model != null) restoreAnimatedParts(model);
    }

    /** Called after vanilla pose setup; removes the last entity's delta first. */
    public static void apply(HumanoidModel<?> model, UUID playerId) {
        restoreAnimatedParts(model);
        Playback playback = playerId == null ? null : PLAYING.get(playerId);
        if (playback == null) return;
        float time = (System.nanoTime() - playback.startedNanos) / 1_000_000_000.0f;
        if (time > playback.emote.duration()) {
            PLAYING.remove(playerId, playback);
            return;
        }
        JsonObject bones = playback.emote.animation().getAsJsonObject("bones");
        if (bones == null) return;
        for (var entry : bones.entrySet()) {
            if (!entry.getValue().isJsonObject()) continue;
            JsonObject channels = entry.getValue().getAsJsonObject();
            float[] position = sample(channels.get("position"), time, new float[] {0, 0, 0});
            float[] rotation = sample(channels.get("rotation"), time, new float[] {0, 0, 0});
            float[] scale = sample(channels.get("scale"), time, new float[] {1, 1, 1});
            Delta delta = new Delta(position[0], -position[1], position[2],
                (float) Math.toRadians(rotation[0]), (float) Math.toRadians(rotation[1]),
                (float) Math.toRadians(rotation[2]), scale[0], scale[1], scale[2]);
            applyBone(model, entry.getKey(), delta);
        }
    }

    private static ModelPart part(HumanoidModel<?> model, String name) {
        if (model instanceof BedrockPlayerModel bedrock) {
            return switch (name.toLowerCase(Locale.ROOT)) {
                // The Bedrock geometry's named root is only one child of the baked
                // Java root. Emote root motion must move every model part.
                case "root" -> model.root();
                case "head" -> bedrock.customHead;
                case "body" -> bedrock.customBody;
                case "rightarm", "right_arm" -> bedrock.customRightArm;
                case "leftarm", "left_arm" -> bedrock.customLeftArm;
                case "rightleg", "right_leg" -> bedrock.customRightLeg;
                case "leftleg", "left_leg" -> bedrock.customLeftLeg;
                default -> null;
            };
        }
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "root" -> model.root();
            case "head" -> model.head;
            case "body" -> model.body;
            case "rightarm", "right_arm" -> model.rightArm;
            case "leftarm", "left_arm" -> model.leftArm;
            case "rightleg", "right_leg" -> model.rightLeg;
            case "leftleg", "left_leg" -> model.leftLeg;
            default -> null;
        };
    }

    /**
     * Java's humanoid model flattens the Bedrock root -> waist -> body hierarchy:
     * body, head and arms are siblings. Recreate the parent transforms so a hip or
     * body animation cannot rotate the torso cube away from the rest of the player.
     */
    private static void applyBone(HumanoidModel<?> model, String name, Delta delta) {
        String normalized = name.toLowerCase(Locale.ROOT);
        if (normalized.equals("root")) {
            ModelPart root = part(model, name);
            if (root != null) apply(root, groundRootTransform(delta));
            return;
        }
        if (normalized.equals("hip") || normalized.equals("waist")) {
            applyVirtualParent(delta, 0.0f, 12.0f, 0.0f,
                model.body, model.head, model.rightArm, model.leftArm);
            return;
        }
        if (normalized.equals("body")) {
            applyVirtualParent(delta, model.body.x, model.body.y, model.body.z,
                model.body, model.head, model.rightArm, model.leftArm);
            return;
        }
        ModelPart part = part(model, name);
        if (part != null) apply(part, delta);
    }

    /**
     * Bedrock's Persona root rotates/scales around the player's feet. Java's
     * synthetic humanoid root is at the top of the 24-pixel model. Convert the
     * authored transform to an equivalent origin-pivoted transform so prone and
     * floor-contact emotes do not lift the whole player into the air.
     */
    private static Delta groundRootTransform(Delta root) {
        Quaternionf rotation = new Quaternionf().rotationZYX(root.zRot, root.yRot, root.xRot);
        Vector3f transformedPivot = new Vector3f(0.0F, PLAYER_GROUND_PIVOT_Y, 0.0F)
            .mul(root.xScale, root.yScale, root.zScale)
            .rotate(rotation);
        return new Delta(
            root.x - transformedPivot.x,
            root.y + PLAYER_GROUND_PIVOT_Y - transformedPivot.y,
            root.z - transformedPivot.z,
            root.xRot, root.yRot, root.zRot,
            root.xScale, root.yScale, root.zScale
        );
    }

    private static void applyVirtualParent(Delta parent, float pivotX, float pivotY, float pivotZ,
                                           ModelPart... children) {
        Quaternionf parentRotation = new Quaternionf()
            .rotationZYX(parent.zRot, parent.yRot, parent.xRot);
        for (ModelPart child : children) {
            if (child == null) continue;

            Vector3f offset = new Vector3f(
                child.x - pivotX, child.y - pivotY, child.z - pivotZ)
                .mul(parent.xScale, parent.yScale, parent.zScale)
                .rotate(parentRotation);
            float targetX = pivotX + parent.x + offset.x;
            float targetY = pivotY + parent.y + offset.y;
            float targetZ = pivotZ + parent.z + offset.z;

            Matrix3f composedRotation = new Matrix3f()
                .rotationZYX(parent.zRot, parent.yRot, parent.xRot)
                .rotateZYX(child.zRot, child.yRot, child.xRot);
            Vector3f targetRotation = composedRotation.getEulerAnglesZYX(new Vector3f());

            apply(child, new Delta(
                targetX - child.x, targetY - child.y, targetZ - child.z,
                targetRotation.x - child.xRot,
                targetRotation.y - child.yRot,
                targetRotation.z - child.zRot,
                parent.xScale, parent.yScale, parent.zScale));
        }
    }

    private static void restoreAnimatedParts(HumanoidModel<?> model) {
        restore(part(model, "root"));
        restore(part(model, "head"));
        restore(part(model, "body"));
        restore(part(model, "rightArm"));
        restore(part(model, "leftArm"));
        restore(part(model, "rightLeg"));
        restore(part(model, "leftLeg"));
    }

    private static float[] sample(JsonElement channel, float time, float[] fallback) {
        if (channel == null) return fallback;
        if (channel.isJsonArray()) return vector(channel, fallback);
        if (!channel.isJsonObject()) return fallback;
        float beforeTime = Float.NEGATIVE_INFINITY, afterTime = Float.POSITIVE_INFINITY;
        JsonElement before = null, after = null;
        for (var entry : channel.getAsJsonObject().entrySet()) {
            float key;
            try { key = Float.parseFloat(entry.getKey()); } catch (NumberFormatException ignored) { continue; }
            if (key <= time && key > beforeTime) { beforeTime = key; before = entry.getValue(); }
            if (key >= time && key < afterTime) { afterTime = key; after = entry.getValue(); }
        }
        if (before == null) return vector(after, fallback);
        if (after == null || afterTime == beforeTime) return vector(before, fallback);
        float[] a = vector(before, fallback), b = vector(after, fallback);
        float alpha = (time - beforeTime) / (afterTime - beforeTime);
        return new float[] {lerp(a[0], b[0], alpha), lerp(a[1], b[1], alpha), lerp(a[2], b[2], alpha)};
    }

    private static float[] vector(JsonElement value, float[] fallback) {
        if (value != null && value.isJsonObject()) {
            JsonObject object = value.getAsJsonObject();
            value = object.has("post") ? object.get("post") : object.get("pre");
        }
        if (value == null || !value.isJsonArray() || value.getAsJsonArray().size() < 3) return fallback;
        float[] result = new float[3];
        for (int i = 0; i < 3; i++) {
            try { result[i] = value.getAsJsonArray().get(i).getAsFloat(); }
            catch (Exception ignored) { result[i] = fallback[i]; }
        }
        return result;
    }

    private static float lerp(float a, float b, float alpha) { return a + (b - a) * alpha; }

    private static void restore(ModelPart part) {
        if (part == null) return;
        Delta previous = LAST_DELTAS.remove(part);
        if (previous == null) return;
        part.x -= previous.x; part.y -= previous.y; part.z -= previous.z;
        part.xRot -= previous.xRot; part.yRot -= previous.yRot; part.zRot -= previous.zRot;
        if (previous.xScale != 0) part.xScale /= previous.xScale;
        if (previous.yScale != 0) part.yScale /= previous.yScale;
        if (previous.zScale != 0) part.zScale /= previous.zScale;
    }

    private static void apply(ModelPart part, Delta delta) {
        part.x += delta.x; part.y += delta.y; part.z += delta.z;
        part.xRot += delta.xRot; part.yRot += delta.yRot; part.zRot += delta.zRot;
        part.xScale *= delta.xScale; part.yScale *= delta.yScale; part.zScale *= delta.zScale;
        Delta prior = LAST_DELTAS.get(part);
        LAST_DELTAS.put(part, prior == null ? delta : new Delta(
            prior.x + delta.x, prior.y + delta.y, prior.z + delta.z,
            prior.xRot + delta.xRot, prior.yRot + delta.yRot, prior.zRot + delta.zRot,
            prior.xScale * delta.xScale, prior.yScale * delta.yScale, prior.zScale * delta.zScale));
    }

    private record Playback(LoadedEmote emote, long startedNanos) {}
    private record Delta(float x, float y, float z, float xRot, float yRot, float zRot,
                         float xScale, float yScale, float zScale) {}
}

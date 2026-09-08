package io.github.brandonitaly.bedrockskins.client.appearance.persona;

import io.github.brandonitaly.bedrockskins.client.appearance.skin.ClientSkinSync;
import io.github.brandonitaly.bedrockskins.client.persistence.StateManager;
import io.github.brandonitaly.bedrockskins.client.render.model.BedrockPlayerModel;

import io.github.brandonitaly.bedrockskins.network.BedrockSkinsNetworking;
import io.github.brandonitaly.bedrockskins.bedrock.BedrockFile;
import io.github.brandonitaly.bedrockskins.pack.model.AssetSource;
import io.github.brandonitaly.bedrockskins.pack.model.LoadedCosmetic;
import io.github.brandonitaly.bedrockskins.pack.persona.PersonaPieceLoader;
import io.github.brandonitaly.bedrockskins.pack.persona.PersonaResourceLoader;
import io.github.brandonitaly.bedrockskins.pack.loader.SkinPackLoader;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/** Loads, equips, and renders Character Creator Persona pieces independently of skins. */
public final class PersonaManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static final Map<String, LoadedCosmetic> COSMETICS = new LinkedHashMap<>();
    private static final Map<String, BedrockPlayerModel> MODELS = new ConcurrentHashMap<>();
    private static final Map<BedrockPlayerModel, LoadedCosmetic> MODEL_COSMETICS = new ConcurrentHashMap<>();
    private static final Map<UUID, CosmeticAssignment> ASSIGNMENTS = new ConcurrentHashMap<>();
    private static final Map<UUID, List<String>> REMOTE_KEYS = new ConcurrentHashMap<>();
    private static final Set<String> REMOTE_COSMETIC_IDS = ConcurrentHashMap.newKeySet();
    /** The persisted local loadout also exists when no world/player is loaded. */
    private static final LinkedHashMap<String, String> LOCAL_SELECTION = new LinkedHashMap<>();
    /** Tint choices are shared by every local cosmetic of the same Persona piece type. */
    private static final Map<String, Integer> LOCAL_COLORS = new ConcurrentHashMap<>();
    private static final Map<String, EquipSide> LOCAL_SIDES = new ConcurrentHashMap<>();

    public enum EquipSide {
        BOTH, LEFT, RIGHT;

        public boolean includesLeft() { return this != RIGHT; }
        public boolean includesRight() { return this != LEFT; }

        public static EquipSide parse(String value) {
            if (value == null) return BOTH;
            try { return valueOf(value.toUpperCase(java.util.Locale.ROOT)); }
            catch (IllegalArgumentException ignored) { return BOTH; }
        }
    }

    private static final class CosmeticAssignment {
        private LinkedHashMap<String, String> equipped;
        private LinkedHashMap<String, String> preview;
        private Map<String, Integer> equippedColors;
        private Map<String, Integer> previewColors;
        private Map<String, EquipSide> equippedSides;
        private Map<String, EquipSide> previewSides;

        private Map<String, String> effective() {
            return preview != null ? preview : equipped;
        }

        private Map<String, Integer> effectiveColors() {
            return preview != null ? previewColors : equippedColors;
        }

        private Map<String, EquipSide> effectiveSides() {
            return preview != null ? previewSides : equippedSides;
        }
    }

    private static CosmeticAssignment assignment(UUID playerId) {
        return ASSIGNMENTS.computeIfAbsent(playerId, ignored -> new CosmeticAssignment());
    }

    private static void removeIfEmpty(UUID playerId, CosmeticAssignment value) {
        if (value.equipped == null && value.preview == null) ASSIGNMENTS.remove(playerId);
    }

    private PersonaManager() {}

    public static void reload(JsonObject vanillaGeometry) {
        clearOtherPlayers();
        PersonaPieceLoader.clearCaches();
        PersonaTextureManager.clear();
        COSMETICS.clear();
        MODELS.clear();
        MODEL_COSMETICS.clear();
        REMOTE_COSMETIC_IDS.clear();

        Path personaDirectory = Minecraft.getInstance().gameDirectory.toPath().resolve("persona");
        try {
            Files.createDirectories(personaDirectory);
        } catch (Exception e) {
            LOGGER.warn("Failed to create Persona directory {}", personaDirectory, e);
        }
        loadDirectoryTree(personaDirectory, vanillaGeometry);
        PersonaResourceLoader.forEachBundledRoot(Minecraft.getInstance().getResourceManager(),
            root -> loadDirectoryTree(root, vanillaGeometry));
        applySavedSelection();
    }

    public static List<LoadedCosmetic> all() {
        return COSMETICS.entrySet().stream()
            .filter(entry -> !REMOTE_COSMETIC_IDS.contains(entry.getKey()))
            .map(Map.Entry::getValue)
            .toList();
    }

    public static Collection<LoadedCosmetic> equipped(UUID playerId) {
        if (playerId == null) return List.of();
        CosmeticAssignment assignment = ASSIGNMENTS.get(playerId);
        Map<String, String> selected = assignment != null ? assignment.effective() : null;
        if (selected == null) return List.of();
        List<LoadedCosmetic> result = new ArrayList<>();
        for (String id : selected.values()) {
            LoadedCosmetic cosmetic = COSMETICS.get(id);
            if (cosmetic != null) result.add(cosmetic);
        }
        return result;
    }

    public static boolean isEquipped(UUID playerId, LoadedCosmetic cosmetic) {
        return cosmetic != null && equipped(playerId).stream().anyMatch(value -> value.id.equals(cosmetic.id));
    }

    public record Occlusion(
        boolean head, boolean hat,
        boolean body, boolean jacket,
        boolean rightArm, boolean rightSleeve,
        boolean leftArm, boolean leftSleeve,
        boolean rightLeg, boolean rightPants,
        boolean leftLeg, boolean leftPants
    ) {
        public static final Occlusion NONE = new Occlusion(false, false, false, false, false, false, false, false, false, false, false, false);

        public boolean any() {
            return head || hat || body || jacket || rightArm || rightSleeve || leftArm || leftSleeve || rightLeg || rightPants || leftLeg || leftPants;
        }

        public boolean isEmpty() {
            return !any();
        }
    }

    public static Occlusion occlusion(UUID playerId) {
        Collection<LoadedCosmetic> cosmetics = playerId != null ? equipped(playerId) : localEquipped();
        return occlusion(cosmetics, cosmetic -> playerId != null ? side(playerId, cosmetic) : localSide(cosmetic));
    }

    public static Occlusion occlusion(Collection<LoadedCosmetic> cosmetics) {
        return occlusion(cosmetics, ignored -> EquipSide.BOTH);
    }

    private static Occlusion occlusion(Collection<LoadedCosmetic> cosmetics,
                                       java.util.function.Function<LoadedCosmetic, EquipSide> sideResolver) {
        if (cosmetics == null || cosmetics.isEmpty()) return Occlusion.NONE;

        boolean head = false;
        boolean hat = false;
        boolean body = false;
        boolean jacket = false;
        boolean rightArm = false;
        boolean rightSleeve = false;
        boolean leftArm = false;
        boolean leftSleeve = false;
        boolean rightLeg = false;
        boolean rightPants = false;
        boolean leftLeg = false;
        boolean leftPants = false;

        for (LoadedCosmetic cosmetic : cosmetics) {
            if (cosmetic == null) continue;
            EquipSide side = isSideSelectable(cosmetic) ? sideResolver.apply(cosmetic) : EquipSide.BOTH;
            boolean includeLeft = side.includesLeft();
            boolean includeRight = side.includesRight();
            Set<String> zones = cosmetic.zones;
            if (zones != null) {
                if (zones.contains("head")) {
                    head = true;
                    hat = true;
                }
                if (zones.contains("head_clothing") || zones.contains("over_hair")) {
                    hat = true;
                }
                if (zones.contains("body")) {
                    body = true;
                    jacket = true;
                }
                if (zones.contains("body_clothing")) {
                    jacket = true;
                }
                if (includeRight && zones.contains("right_arm")) {
                    rightArm = true;
                    rightSleeve = true;
                }
                if (includeRight && zones.contains("right_arm_clothing")) {
                    rightSleeve = true;
                }
                if (includeLeft && zones.contains("left_arm")) {
                    leftArm = true;
                    leftSleeve = true;
                }
                if (includeLeft && zones.contains("left_arm_clothing")) {
                    leftSleeve = true;
                }
                if (includeRight && zones.contains("right_leg")) {
                    rightLeg = true;
                    rightPants = true;
                }
                if (includeRight && zones.contains("right_leg_clothing")) {
                    rightPants = true;
                }
                if (includeLeft && zones.contains("left_leg")) {
                    leftLeg = true;
                    leftPants = true;
                }
                if (includeLeft && zones.contains("left_leg_clothing")) {
                    leftPants = true;
                }
            }

            if ("persona_arms".equals(cosmetic.type)) {
                if (includeRight) {
                    rightArm = true;
                    rightSleeve = true;
                }
                if (includeLeft) {
                    leftArm = true;
                    leftSleeve = true;
                }
            }
        }

        return new Occlusion(head, hat, body, jacket, rightArm, rightSleeve, leftArm, leftSleeve, rightLeg, rightPants, leftLeg, leftPants);
    }

    public static Collection<LoadedCosmetic> localEquipped() {
        List<LoadedCosmetic> result = new ArrayList<>();
        synchronized (LOCAL_SELECTION) {
            for (String id : LOCAL_SELECTION.values()) {
                LoadedCosmetic cosmetic = COSMETICS.get(id);
                if (cosmetic != null) result.add(cosmetic);
            }
        }
        return List.copyOf(result);
    }

    public static boolean isLocallyEquipped(LoadedCosmetic cosmetic) {
        if (cosmetic == null) return false;
        synchronized (LOCAL_SELECTION) {
            return Objects.equals(LOCAL_SELECTION.get(cosmetic.type), cosmetic.id);
        }
    }

    public static boolean isSideSelectable(LoadedCosmetic cosmetic) {
        return cosmetic != null && ("persona_arms".equals(cosmetic.type) || "persona_legs".equals(cosmetic.type));
    }

    public static boolean isColorSelectable(LoadedCosmetic cosmetic) {
        return cosmetic != null && cosmetic.tintable
            && ("persona_hair".equals(cosmetic.type) || "persona_facial_hair".equals(cosmetic.type));
    }

    public static EquipSide localSide(LoadedCosmetic cosmetic) {
        return cosmetic == null ? EquipSide.BOTH : LOCAL_SIDES.getOrDefault(cosmetic.type, EquipSide.BOTH);
    }

    public static EquipSide side(UUID playerId, LoadedCosmetic cosmetic) {
        if (cosmetic == null || !isSideSelectable(cosmetic)) return EquipSide.BOTH;
        CosmeticAssignment assignment = playerId == null ? null : ASSIGNMENTS.get(playerId);
        Map<String, EquipSide> sides = assignment == null ? null : assignment.effectiveSides();
        return sides == null ? EquipSide.BOTH : sides.getOrDefault(cosmetic.id, EquipSide.BOTH);
    }

    public static void setLocalSide(LoadedCosmetic cosmetic, EquipSide side) {
        if (!isSideSelectable(cosmetic)) return;
        LOCAL_SIDES.put(cosmetic.type, side == null ? EquipSide.BOTH : side);
        boolean typeEquipped;
        synchronized (LOCAL_SELECTION) {
            typeEquipped = LOCAL_SELECTION.containsKey(cosmetic.type);
            applyLocalSelectionToPlayer(new LinkedHashMap<>(LOCAL_SELECTION));
            if (typeEquipped) saveLocalSelection(LOCAL_SELECTION);
        }
        if (localPlayerId() != null && typeEquipped) ClientSkinSync.syncCurrentCosmetics();
    }

    public static void applySideVisibility(BedrockPlayerModel model, UUID playerId, LoadedCosmetic cosmetic) {
        if (model == null || !isSideSelectable(cosmetic)) return;
        EquipSide side = side(playerId, cosmetic);
        if ("persona_arms".equals(cosmetic.type)) {
            model.customLeftArm.visible = side.includesLeft();
            model.customRightArm.visible = side.includesRight();
        } else {
            model.customLeftLeg.visible = side.includesLeft();
            model.customRightLeg.visible = side.includesRight();
        }
    }

    /** Applies the side selection belonging to a deferred cosmetic model submission. */
    public static void applySideVisibility(BedrockPlayerModel model, UUID playerId) {
        applySideVisibility(model, playerId, MODEL_COSMETICS.get(model));
    }

    public static boolean rendersArm(UUID playerId, LoadedCosmetic cosmetic, boolean rightArm) {
        if (!isSideSelectable(cosmetic) || !"persona_arms".equals(cosmetic.type)) return true;
        EquipSide side = side(playerId, cosmetic);
        return rightArm ? side.includesRight() : side.includesLeft();
    }

    public static boolean toggleLocal(LoadedCosmetic cosmetic) {
        if (cosmetic == null) return false;
        boolean equipped;
        LinkedHashMap<String, String> selected;
        synchronized (LOCAL_SELECTION) {
            if (Objects.equals(LOCAL_SELECTION.get(cosmetic.type), cosmetic.id)) {
                LOCAL_SELECTION.remove(cosmetic.type);
                equipped = false;
            } else {
                LOCAL_SELECTION.put(cosmetic.type, cosmetic.id);
                equipped = true;
            }
            selected = new LinkedHashMap<>(LOCAL_SELECTION);
        }
        applyLocalSelectionToPlayer(selected);
        saveLocalSelection(selected);
        if (localPlayerId() != null) ClientSkinSync.syncCurrentCosmetics();
        return equipped;
    }

    public static void setTintColor(LoadedCosmetic cosmetic, int color) {
        if (!isColorSelectable(cosmetic)) return;
        int selectedColor = color & 0xFFFFFF;
        LOCAL_COLORS.put(cosmetic.type, selectedColor);
        StateManager.updatePersonaColors(Map.copyOf(LOCAL_COLORS));
        synchronized (LOCAL_SELECTION) {
            applyLocalSelectionToPlayer(new LinkedHashMap<>(LOCAL_SELECTION));
        }
        for (CosmeticAssignment assignment : ASSIGNMENTS.values()) {
            String previewId = assignment.preview == null ? null : assignment.preview.get(cosmetic.type);
            if (previewId != null && assignment.previewColors != null) {
                assignment.previewColors = withColor(assignment.previewColors, previewId, selectedColor);
            }
        }
        if (localPlayerId() != null) ClientSkinSync.syncCurrentCosmetics();
    }

    public static int localTintColor(LoadedCosmetic cosmetic) {
        return cosmetic == null ? 0xFFFFFF : isColorSelectable(cosmetic)
            ? LOCAL_COLORS.getOrDefault(cosmetic.type, cosmetic.defaultTintColor)
            : cosmetic.defaultTintColor;
    }

    public static int tintColor(UUID playerId, LoadedCosmetic cosmetic) {
        if (cosmetic == null) return 0xFFFFFF;
        CosmeticAssignment assignment = playerId == null ? null : ASSIGNMENTS.get(playerId);
        Map<String, Integer> colors = assignment == null ? null : assignment.effectiveColors();
        return colors == null ? cosmetic.defaultTintColor : colors.getOrDefault(cosmetic.id, cosmetic.defaultTintColor);
    }

    public static net.minecraft.resources.Identifier texture(UUID playerId, LoadedCosmetic cosmetic) {
        return PersonaTextureManager.texture(cosmetic, tintColor(playerId, cosmetic));
    }

    public static void clearLocal() {
        LOCAL_COLORS.clear();
        LOCAL_SIDES.clear();
        synchronized (LOCAL_SELECTION) {
            LOCAL_SELECTION.clear();
        }
        UUID playerId = localPlayerId();
        if (playerId != null) setEquipped(playerId, null);
        StateManager.updateCosmetics(List.of());
        StateManager.updatePersonaColors(Map.of());
        if (playerId != null) ClientSkinSync.syncCurrentCosmetics();
    }

    public static void setPreview(UUID playerId, LoadedCosmetic cosmetic) {
        if (playerId == null) return;
        CosmeticAssignment assignment = assignment(playerId);
        if (cosmetic == null) {
            assignment.preview = null;
            assignment.previewColors = null;
            assignment.previewSides = null;
        }
        else {
            LinkedHashMap<String, String> selected = new LinkedHashMap<>();
            selected.put(cosmetic.type, cosmetic.id);
            assignment.preview = selected;
            assignment.previewColors = Map.of(cosmetic.id, localTintColor(cosmetic));
            assignment.previewSides = Map.of(cosmetic.id, localSide(cosmetic));
        }
        removeIfEmpty(playerId, assignment);
    }

    public static void setPreviewWithEquipped(UUID playerId, LoadedCosmetic cosmetic) {
        if (playerId == null) return;
        LinkedHashMap<String, String> selected;
        synchronized (LOCAL_SELECTION) {
            selected = new LinkedHashMap<>(LOCAL_SELECTION);
        }
        if (cosmetic != null) selected.put(cosmetic.type, cosmetic.id);
        CosmeticAssignment assignment = assignment(playerId);
        assignment.preview = selected.isEmpty() ? null : selected;
        assignment.previewColors = selected.isEmpty() ? null : colorsFor(selected);
        assignment.previewSides = selected.isEmpty() ? null : sidesFor(selected);
        removeIfEmpty(playerId, assignment);
    }

    /** Makes a GUI preview render the current local loadout, including on the title screen. */
    public static void setPreviewFromLocal(UUID playerId) {
        setPreviewWithEquipped(playerId, null);
    }

    public static void clearPreview(UUID playerId) {
        if (playerId == null) return;
        CosmeticAssignment assignment = ASSIGNMENTS.get(playerId);
        if (assignment == null) return;
        assignment.preview = null;
        assignment.previewColors = null;
        assignment.previewSides = null;
        removeIfEmpty(playerId, assignment);
    }

    public static BedrockPlayerModel model(LoadedCosmetic cosmetic, boolean slim) {
        if (cosmetic == null) return null;
        String key = cosmetic.id + (slim ? "#slim" : "#wide");
        return MODELS.computeIfAbsent(key, ignored -> {
            try {
                JsonObject geoData = slim ? cosmetic.slimGeometryData : cosmetic.geometryData;
                BedrockFile file = GSON.fromJson(geoData, BedrockFile.class);
                if (file.getGeometries() == null || file.getGeometries().isEmpty()) return null;
                BedrockPlayerModel model = BedrockPlayerModel.create(file.getGeometries().getFirst(), slim, true);
                MODEL_COSMETICS.put(model, cosmetic);
                return model;
            } catch (Exception e) {
                LOGGER.warn("Failed to build Persona cosmetic model {} (slim={})", cosmetic.displayName, slim, e);
                return null;
            }
        });
    }

    /** Advances vertically stacked Persona texture strips. */
    public static void prepareTexture(UUID playerId, LoadedCosmetic cosmetic) {
        PersonaTextureManager.prepare(cosmetic, tintColor(playerId, cosmetic));
    }

    private static void loadDirectoryTree(Path root, JsonObject vanillaGeometry) {
        if (!Files.isDirectory(root)) return;
        try (Stream<Path> paths = Files.walk(root, 3)) {
            paths.filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".meta.json"))
                .map(Path::getParent)
                .distinct()
                .forEach(path ->
                PersonaPieceLoader.load(path.toFile(), vanillaGeometry).ifPresent(cosmetic -> {
                    if (COSMETICS.containsKey(cosmetic.id)) return;
                    PersonaTextureManager.register(cosmetic);
                    COSMETICS.put(cosmetic.id, cosmetic);
                }));
        } catch (Exception e) {
            LOGGER.warn("Failed to scan Persona cosmetics in {}", root, e);
        }
    }

    public static void applySavedSelection() {
        LinkedHashMap<String, String> selected = new LinkedHashMap<>();
        LOCAL_COLORS.clear();
        StateManager.readState().personaColors().forEach((type, color) ->
            LOCAL_COLORS.put(type, color & 0xFFFFFF));
        LOCAL_SIDES.clear();
        for (String saved : StateManager.readState().selectedCosmetics()) {
            int sideMarker = saved.lastIndexOf("|side=");
            String id = sideMarker < 0 ? saved : saved.substring(0, sideMarker);
            LoadedCosmetic cosmetic = COSMETICS.get(id);
            if (cosmetic != null) {
                selected.put(cosmetic.type, cosmetic.id);
                if (sideMarker >= 0 && isSideSelectable(cosmetic)) {
                    LOCAL_SIDES.put(cosmetic.type, EquipSide.parse(saved.substring(sideMarker + 6)));
                }
            }
        }
        synchronized (LOCAL_SELECTION) {
            LOCAL_SELECTION.clear();
            LOCAL_SELECTION.putAll(selected);
        }
        applyLocalSelectionToPlayer(selected);
    }

    /** Installs cosmetics received for another player without exposing them in the picker. */
    public static void applyRemote(UUID playerId, List<BedrockSkinsNetworking.CosmeticData> data) {
        if (playerId == null || Objects.equals(playerId, localPlayerId())) return;
        removeRemote(playerId);
        if (data == null || data.isEmpty()) return;

        LinkedHashMap<String, String> selected = new LinkedHashMap<>();
        List<String> keys = new ArrayList<>();
        Map<String, Integer> colors = new LinkedHashMap<>();
        Map<String, EquipSide> sides = new LinkedHashMap<>();
        for (BedrockSkinsNetworking.CosmeticData value : data) {
            try {
                String key = "remote/" + playerId + "/" + sanitize(value.id());
                JsonObject geometry = JsonParser.parseString(value.geometry()).getAsJsonObject();
                JsonObject slimGeometry = (value.slimGeometry() != null && !value.slimGeometry().isEmpty())
                    ? JsonParser.parseString(value.slimGeometry()).getAsJsonObject()
                    : geometry;
                LoadedCosmetic cosmetic = new LoadedCosmetic(key, value.name(), value.type(), value.zones(),
                    geometry, slimGeometry, new AssetSource.Memory(value.textureData()),
                    false, value.tintColor());
                PersonaTextureManager.register(cosmetic);
                COSMETICS.put(key, cosmetic);
                REMOTE_COSMETIC_IDS.add(key);
                keys.add(key);
                colors.put(key, value.tintColor());
                sides.put(key, EquipSide.parse(value.side()));
                selected.put(cosmetic.type, key);
            } catch (Exception e) {
                LOGGER.warn("Failed to load remote Persona cosmetic {}", value.id(), e);
            }
        }
        if (!keys.isEmpty()) {
            REMOTE_KEYS.put(playerId, List.copyOf(keys));
            setEquipped(playerId, selected, colors, sides);
        }
    }

    public static List<BedrockSkinsNetworking.CosmeticData> localPayload() {
        List<BedrockSkinsNetworking.CosmeticData> result = new ArrayList<>();
        for (LoadedCosmetic cosmetic : localEquipped()) {
            if (!(cosmetic.texture instanceof AssetSource.Memory memory)) continue;
            result.add(new BedrockSkinsNetworking.CosmeticData(
                cosmetic.id,
                cosmetic.type,
                cosmetic.displayName,
                cosmetic.zones,
                cosmetic.geometryData.toString(),
                cosmetic.slimGeometryData != null ? cosmetic.slimGeometryData.toString() : cosmetic.geometryData.toString(),
                memory.data(),
                localTintColor(cosmetic),
                localSide(cosmetic).name().toLowerCase(java.util.Locale.ROOT)
            ));
        }
        return List.copyOf(result);
    }

    public static void clearOtherPlayers() {
        UUID localId = localPlayerId();
        for (UUID playerId : List.copyOf(REMOTE_KEYS.keySet())) {
            if (!Objects.equals(playerId, localId)) removeRemote(playerId);
        }
    }

    private static void removeRemote(UUID playerId) {
        List<String> keys = REMOTE_KEYS.remove(playerId);
        if (keys != null) {
            for (String key : keys) {
                LoadedCosmetic cosmetic = COSMETICS.remove(key);
                REMOTE_COSMETIC_IDS.remove(key);
                MODELS.remove(key);
                MODELS.remove(key + "#wide");
                MODELS.remove(key + "#slim");
                PersonaTextureManager.remove(key);
            }
        }
        ASSIGNMENTS.remove(playerId);
    }

    private static void saveLocalSelection(Map<String, String> selected) {
        List<String> saved = new ArrayList<>();
        for (String id : selected.values()) {
            LoadedCosmetic cosmetic = COSMETICS.get(id);
            EquipSide side = localSide(cosmetic);
            saved.add(isSideSelectable(cosmetic) && side != EquipSide.BOTH
                ? id + "|side=" + side.name().toLowerCase(java.util.Locale.ROOT)
                : id);
        }
        StateManager.updateCosmetics(List.copyOf(saved));
    }

    private static void applyLocalSelectionToPlayer(Map<String, String> selected) {
        UUID playerId = localPlayerId();
        if (playerId == null) return;
        setEquipped(playerId, selected);
    }

    private static void setEquipped(UUID playerId, Map<String, String> selected) {
        setEquipped(playerId, selected, colorsFor(selected), sidesFor(selected));
    }

    private static void setEquipped(UUID playerId, Map<String, String> selected, Map<String, Integer> colors,
                                    Map<String, EquipSide> sides) {
        CosmeticAssignment assignment = assignment(playerId);
        assignment.equipped = selected == null || selected.isEmpty() ? null : new LinkedHashMap<>(selected);
        assignment.equippedColors = assignment.equipped == null ? null : Map.copyOf(colors);
        assignment.equippedSides = assignment.equipped == null ? null : Map.copyOf(sides);
        removeIfEmpty(playerId, assignment);
    }

    private static Map<String, Integer> colorsFor(Map<String, String> selected) {
        if (selected == null || selected.isEmpty()) return Map.of();
        Map<String, Integer> colors = new LinkedHashMap<>();
        for (String id : selected.values()) {
            LoadedCosmetic cosmetic = COSMETICS.get(id);
            if (cosmetic != null) colors.put(id, localTintColor(cosmetic));
        }
        return colors;
    }

    private static Map<String, EquipSide> sidesFor(Map<String, String> selected) {
        if (selected == null || selected.isEmpty()) return Map.of();
        Map<String, EquipSide> sides = new LinkedHashMap<>();
        for (String id : selected.values()) {
            LoadedCosmetic cosmetic = COSMETICS.get(id);
            if (cosmetic != null) sides.put(id, LOCAL_SIDES.getOrDefault(cosmetic.type, EquipSide.BOTH));
        }
        return sides;
    }

    private static Map<String, Integer> withColor(Map<String, Integer> source, String id, int color) {
        Map<String, Integer> result = new LinkedHashMap<>(source);
        result.put(id, color);
        return Map.copyOf(result);
    }

    private static UUID localPlayerId() {
        var player = Minecraft.getInstance().player;
        return player == null ? null : player.getUUID();
    }

    private static String sanitize(String value) {
        return value.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9/._-]", "_");
    }

}

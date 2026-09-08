package io.github.brandonitaly.bedrockskins.pack.persona;

import io.github.brandonitaly.bedrockskins.pack.model.AssetSource;
import io.github.brandonitaly.bedrockskins.pack.model.LoadedCosmetic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Loads one Bedrock Character Creator (Persona) piece as an independently rendered cosmetic. */
public final class PersonaPieceLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int ATLAS_SIZE = 1024;
    private static final int PERSONA_SKIN_SIZE = 128;
    private static final int SLIM_SKIN_ATLAS_U = 64;
    private static final String BASE_GEOMETRY = "geometry.humanoid.custom";
    private static final String BASE_GEOMETRY_SLIM = "geometry.humanoid.customSlim";

    private PersonaPieceLoader() {}

    public static void clearCaches() {
        PersonaLocalization.clearCache();
    }

    public static Optional<LoadedCosmetic> load(File directory, JsonObject vanillaGeometry) {
        File[] metadataFiles = directory.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".meta.json"));
        if (metadataFiles == null || metadataFiles.length == 0) return Optional.empty();

        try {
            JsonObject metadata = readJson(metadataFiles[0]);
            String pieceType = string(metadata, "piece_type");
            if (pieceType == null || !pieceType.startsWith("persona_") || "persona_emote".equals(pieceType)) {
                LOGGER.debug("Skipping non-wearable Persona entry {}", directory.getName());
                return Optional.empty();
            }

            String pieceName = string(metadata, "piece_name");
            if (pieceName == null || pieceName.isBlank()) pieceName = directory.getName();
            String displayName = PersonaLocalization.displayName(directory, pieceName);

            JsonObject base = resolveBaseGeometry(vanillaGeometry, BASE_GEOMETRY);
            if (base == null) throw new IllegalArgumentException("Vanilla player geometry is unavailable");
            JsonObject baseSlim = resolveBaseGeometry(vanillaGeometry, BASE_GEOMETRY_SLIM);
            if (baseSlim == null) baseSlim = base;

            TintSpec tint = tintSpec(metadata);
            boolean tintable = hasTintMap(metadata);
            BufferedImage atlas = new BufferedImage(ATLAS_SIZE, ATLAS_SIZE, BufferedImage.TYPE_INT_ARGB);
            BufferedImage tintAtlas = tintable
                ? new BufferedImage(ATLAS_SIZE, ATLAS_SIZE, BufferedImage.TYPE_INT_ARGB) : null;
            PersonaTextureLayout textureLayout = drawTextureLayers(directory, metadata, atlas, tintAtlas, tint.channel());

            PersonaAtlasPacker packer = new PersonaAtlasPacker(atlas, tintAtlas, 0, PERSONA_SKIN_SIZE + 1);
            packer.include(PERSONA_SKIN_SIZE, PERSONA_SKIN_SIZE);
            JsonArray geometrySources = metadata.getAsJsonArray("geometry_sources");
            List<JsonObject> selectedSources = geometrySources == null
                ? List.of() : selectGeometrySources(geometrySources, false);
            List<JsonObject> slimSources = geometrySources == null
                ? List.of() : selectGeometrySources(geometrySources, true);
            Map<String, JsonObject> geometries = loadGeometries(directory);
            Map<String, BufferedImage> textures = new HashMap<>();
            Map<String, BufferedImage> tintMaps = new HashMap<>();
            JsonObject geometry = mergeGeometry(directory, metadata, base.deepCopy(), packer, geometries,
                textures, tintMaps, selectedSources, textureLayout, tint.channel(), false);
            JsonObject slimGeometry = mergeGeometry(directory, metadata, baseSlim.deepCopy(), packer, geometries,
                textures, tintMaps, slimSources, textureLayout, tint.channel(), true);
            int atlasWidth = packer.usedWidth();
            int atlasHeight = packer.usedHeight();
            setTextureSize(geometry, atlasWidth, atlasHeight);
            setTextureSize(slimGeometry, atlasWidth, atlasHeight);
            BufferedImage compactAtlas = atlas.getSubimage(0, 0, atlasWidth, atlasHeight);
            BufferedImage compactTintAtlas = tintAtlas == null ? null
                : tintAtlas.getSubimage(0, 0, atlasWidth, atlasHeight);
            byte[] png = encodeTexturePayload(compactAtlas, compactTintAtlas, tint.baseColor(), packer);
            Set<String> zones = collectZones(metadata, selectedSources);

            String pieceId = string(metadata, "piece_id");
            if (pieceId == null || pieceId.isBlank()) pieceId = pieceName;
            return Optional.of(new LoadedCosmetic(pieceId, displayName, pieceType, zones,
                geometry, slimGeometry, new AssetSource.Memory(png), tintable, tint.defaultColor()));
        } catch (Exception e) {
            LOGGER.warn("Failed to load Persona cosmetic from {}", directory.getName(), e);
            return Optional.empty();
        }
    }

    private record PersonaTextureLayout(boolean body, boolean face, boolean tint) {}
    private record TintSpec(int channel, int baseColor, int defaultColor) {}

    private static PersonaTextureLayout drawTextureLayers(File directory, JsonObject metadata, BufferedImage atlas,
                                                           BufferedImage tintAtlas, int tintChannel) throws Exception {
        JsonArray sources = metadata.getAsJsonArray("texture_sources");
        if (sources == null) return new PersonaTextureLayout(false, false, false);

        boolean hasBody = false;
        boolean hasFace = false;
        boolean hasTint = false;
        Graphics2D graphics = atlas.createGraphics();
        Graphics2D tintGraphics = tintAtlas == null ? null : tintAtlas.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.SrcOver);
            if (tintGraphics != null) tintGraphics.setComposite(AlphaComposite.SrcOver);
            for (JsonElement element : sources) {
                if (!element.isJsonObject()) continue;
                JsonObject source = element.getAsJsonObject();
                String name = string(source, "texture");
                BufferedImage layer = readImage(directory, name);
                if (layer == null) continue;
                BufferedImage tintLayer = readTintMap(directory, string(source, "tint_map"), tintChannel);
                boolean face = source.has("use_face_uv") && source.get("use_face_uv").getAsBoolean();
                if (face) drawPersonaFaceLayer(graphics, layer);
                else drawPersonaBodyLayer(graphics, layer);
                if (tintLayer != null && tintGraphics != null) {
                    if (face) drawPersonaFaceLayer(tintGraphics, tintLayer);
                    else drawPersonaBodyLayer(tintGraphics, tintLayer);
                    hasTint = true;
                }
                hasFace |= face;
                hasBody |= !face;
            }
        } finally {
            graphics.dispose();
            if (tintGraphics != null) tintGraphics.dispose();
        }
        return new PersonaTextureLayout(hasBody, hasFace, hasTint);
    }

    /**
     * Persona body textures contain several body/arm variants in a 128x128
     * sheet. Extract the medium/wide variant into the ordinary 64x64 player
     * layout used by geometry.humanoid.custom. The Persona mesh stores
     * normalized V coordinates from the bottom, while PNG rows start at the
     * top; after that conversion each selected box already uses vanilla's cap
     * row followed by its side strip.
     */
    private static void drawPersonaBodyLayer(Graphics2D graphics, BufferedImage source) {
        if (source.getWidth() < 128 || source.getHeight() < 128) {
            graphics.drawImage(source, 0, 0, null);
            graphics.drawImage(source, SLIM_SKIN_ATLAS_U, 0, null);
            return;
        }
        // Medium/wide layout.
        copyPersonaBox(graphics, source, 16, 0, 16, 16, 8, 12, 4);  // body
        copyPersonaBox(graphics, source, 16, 16, 16, 32, 8, 12, 4); // jacket
        copyPersonaBox(graphics, source, 0, 0, 40, 16, 4, 12, 4);   // right arm
        copyPersonaBox(graphics, source, 0, 16, 40, 32, 4, 12, 4);  // right sleeve
        copyPersonaBox(graphics, source, 40, 0, 32, 48, 4, 12, 4);  // left arm
        copyPersonaBox(graphics, source, 40, 16, 48, 48, 4, 12, 4); // left sleeve
        copyPersonaBox(graphics, source, 16, 32, 0, 16, 4, 12, 4);  // right leg
        copyPersonaBox(graphics, source, 0, 32, 0, 32, 4, 12, 4);   // right pants
        copyPersonaBox(graphics, source, 32, 32, 16, 48, 4, 12, 4); // left leg
        copyPersonaBox(graphics, source, 48, 32, 0, 48, 4, 12, 4);  // left pants

        // These source rectangles come from persona/pieces/body/medium/
        // medium.geometry.json. Bedrock keeps the shared torso and legs in
        // their wide-layout locations, then packs all four slim arm layers
        // across y=48: right clothing, right arm, left arm, left clothing.
        // Store the resulting vanilla-shaped layout on a separate atlas page.
        int u = SLIM_SKIN_ATLAS_U;
        copyPersonaBox(graphics, source, 16, 0, u + 16, 16, 8, 12, 4);   // body
        copyPersonaBox(graphics, source, 16, 16, u + 16, 32, 8, 12, 4);  // jacket
        copyPersonaBox(graphics, source, 14, 48, u + 40, 16, 3, 12, 4);  // right arm
        copyPersonaBox(graphics, source, 0, 48, u + 40, 32, 3, 12, 4);   // right sleeve
        copyPersonaBox(graphics, source, 28, 48, u + 32, 48, 3, 12, 4);  // left arm
        copyPersonaBox(graphics, source, 42, 48, u + 48, 48, 3, 12, 4);  // left sleeve
        copyPersonaBox(graphics, source, 16, 32, u, 16, 4, 12, 4);       // right leg
        copyPersonaBox(graphics, source, 0, 32, u, 32, 4, 12, 4);        // right pants
        copyPersonaBox(graphics, source, 32, 32, u + 16, 48, 4, 12, 4);  // left leg
        copyPersonaBox(graphics, source, 48, 32, u, 48, 4, 12, 4);       // left pants
    }

    /** Maps Persona's independent 32x32 face UV space onto vanilla head UVs. */
    private static void drawPersonaFaceLayer(Graphics2D graphics, BufferedImage source) {
        if (source.getWidth() < 32 || source.getHeight() < 32) {
            graphics.drawImage(source, 0, 0, null);
            graphics.drawImage(source, SLIM_SKIN_ATLAS_U, 0, null);
            return;
        }
        copyPersonaBox(graphics, source, 0, 0, 0, 0, 8, 8, 8);   // head
        copyPersonaBox(graphics, source, 0, 16, 32, 0, 8, 8, 8); // hat
        copyPersonaBox(graphics, source, 0, 0, SLIM_SKIN_ATLAS_U, 0, 8, 8, 8);
        copyPersonaBox(graphics, source, 0, 16, SLIM_SKIN_ATLAS_U + 32, 0, 8, 8, 8);
    }

    private static void copyPersonaBox(Graphics2D graphics, BufferedImage source,
                                       int sourceU, int sourceV, int targetU, int targetV,
                                       int width, int height, int depth) {
        int boxWidth = depth + width + depth + width;
        graphics.drawImage(source,
            targetU, targetV, targetU + boxWidth, targetV + depth + height,
            sourceU, sourceV, sourceU + boxWidth, sourceV + depth + height, null);
    }

    private static JsonObject mergeGeometry(File directory, JsonObject metadata, JsonObject base,
                                            PersonaAtlasPacker packer, Map<String, JsonObject> geometries,
                                            Map<String, BufferedImage> textures, Map<String, BufferedImage> tintMaps,
                                            List<JsonObject> sources,
                                            PersonaTextureLayout textureLayout, int tintChannel,
                                            boolean slim) throws Exception {
        JsonObject target = base.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        JsonObject description = target.getAsJsonObject("description");
        description.addProperty("identifier", "geometry.persona." + sanitize(string(metadata, "piece_name")));
        description.addProperty("texture_width", ATLAS_SIZE);
        description.addProperty("texture_height", ATLAS_SIZE);

        JsonArray targetBones = target.getAsJsonArray("bones");
        applyPersonaTextureUvs(targetBones, textureLayout, string(metadata, "piece_type"), slim);
        Map<String, JsonObject> bonesByName = new LinkedHashMap<>();
        for (JsonElement element : targetBones) {
            JsonObject bone = element.getAsJsonObject();
            bonesByName.put(string(bone, "name"), bone);
        }

        int extraBoneStart = packer.extraBones.size();
        for (JsonObject source : sources) {
            String geometryName = string(source, "geometry");
            if (geometryName == null) continue;
            JsonObject sourceGeometry = geometries.get(geometryName);
            if (sourceGeometry == null || !sourceGeometry.has("bones")) continue;

            String textureName = string(source, "texture");
            BufferedImage texture = textures.computeIfAbsent(textureName == null ? "" : textureName,
                ignored -> readImageUnchecked(directory, textureName));
            String tintMapName = string(source, "tint_map");
            BufferedImage tintMap = tintMaps.computeIfAbsent(tintMapName == null ? "" : tintMapName,
                ignored -> readTintMapUnchecked(directory, tintMapName, tintChannel));
            boolean animated = source.has("animated") && source.get("animated").getAsBoolean();
            int frames = animated ? Math.max(1, intValue(source, "frames", 1)) : 1;
            int frameHeight = texture == null ? 0 : Math.max(1, texture.getHeight() / Math.max(1, frames));

            for (JsonElement boneElement : sourceGeometry.getAsJsonArray("bones")) {
                JsonObject incoming = boneElement.getAsJsonObject().deepCopy();
                rewriteCubes(incoming, texture, tintMap, frameHeight, frames, packer);
                rewritePolyMesh(incoming, texture, tintMap, frameHeight, frames, packer);
                mergeBone(targetBones, bonesByName, incoming);
            }
        }
        for (int i = extraBoneStart; i < packer.extraBones.size(); i++) {
            mergeBone(targetBones, bonesByName, packer.extraBones.get(i));
        }
        return base;
    }

    private static Set<String> collectZones(JsonObject metadata, List<JsonObject> selectedSources) {
        Set<String> zones = new LinkedHashSet<>();
        addZones(zones, metadata.get("zone"));
        for (JsonObject source : selectedSources) addZones(zones, source.get("zone"));
        return Set.copyOf(zones);
    }

    private static void addZones(Set<String> target, JsonElement value) {
        if (value == null || value.isJsonNull()) return;
        if (value.isJsonArray()) {
            for (JsonElement element : value.getAsJsonArray()) addZones(target, element);
        } else if (value.isJsonPrimitive()) {
            String zone = value.getAsString().trim().toLowerCase(Locale.ROOT);
            if (!zone.isEmpty()) target.add(zone);
        }
    }

    private static boolean hasTintMap(JsonObject metadata) {
        for (String sourceName : List.of("texture_sources", "geometry_sources")) {
            JsonArray sources = metadata.getAsJsonArray(sourceName);
            if (sources == null) continue;
            for (JsonElement element : sources) {
                if (!element.isJsonObject()) continue;
                String tintMap = string(element.getAsJsonObject(), "tint_map");
                if (tintMap != null && !tintMap.isBlank()) return true;
            }
        }
        return false;
    }

    private static List<JsonObject> selectGeometrySources(JsonArray sources, boolean slim) {
        List<JsonObject> preferred = new ArrayList<>();
        List<JsonObject> medium = new ArrayList<>();
        List<JsonObject> fallback = new ArrayList<>();
        for (JsonElement element : sources) {
            if (!element.isJsonObject()) continue;
            JsonObject source = element.getAsJsonObject();
            String body = string(source, "body_size");
            String arm = string(source, "arm_size");
            if (body == null || "medium".equals(body)) {
                medium.add(source);
                if (arm == null || (slim ? "slim" : "wide").equals(arm)) preferred.add(source);
            }
            fallback.add(source);
        }
        return !preferred.isEmpty() ? preferred : !medium.isEmpty() ? medium : fallback;
    }

    /** Retains the vanilla player scaffold parts used by this texture source. */
    private static void applyPersonaTextureUvs(JsonArray bones, PersonaTextureLayout layout, String pieceType,
                                               boolean slim) {
        Set<String> bodyBones = Set.of("body", "jacket", "rightarm", "rightsleeve",
            "leftarm", "leftsleeve", "rightleg", "rightpants", "leftleg", "leftpants");
        for (JsonElement boneElement : bones) {
            JsonObject bone = boneElement.getAsJsonObject();
            JsonArray cubes = bone.getAsJsonArray("cubes");
            if (cubes == null) continue;
            String boneName = string(bone, "name");
            String key = boneName == null ? "" : boneName.toLowerCase(Locale.ROOT);
            boolean head = "head".equals(key);
            boolean hat = "hat".equals(key);
            boolean leg = key.contains("leg") || key.contains("pants");
            boolean bodyAllowed = layout.body() && bodyBones.contains(key)
                && !("persona_top".equals(pieceType) && leg);

            if (!bodyAllowed && ((!head && !hat) || !layout.face())) {
                bone.add("cubes", new JsonArray());
                continue;
            }
            for (JsonElement cubeElement : cubes) {
                JsonObject cube = cubeElement.getAsJsonObject();
                if (slim && cube.has("uv") && cube.get("uv").isJsonArray()) {
                    JsonArray uv = cube.getAsJsonArray("uv");
                    if (uv.size() >= 2) {
                        uv.set(0, number(uv.get(0).getAsFloat() + SLIM_SKIN_ATLAS_U));
                    }
                }
                float inflate = cube.has("inflate") ? cube.get("inflate").getAsFloat() : 0.0f;
                cube.addProperty("inflate", inflate + 0.01f);
            }
        }
    }

    private static void rewriteCubes(JsonObject bone, BufferedImage texture, BufferedImage tintMap, int frameHeight,
                                     int frameCount, PersonaAtlasPacker packer) {
        JsonArray cubes = bone.getAsJsonArray("cubes");
        if (cubes == null) return;
        JsonArray rewritten = new JsonArray();
        for (JsonElement element : cubes) {
            JsonObject cube = element.getAsJsonObject().deepCopy();
            JsonElement uv = cube.get("uv");
            if (uv != null && uv.isJsonObject() && isPerFaceUv(uv.getAsJsonObject())) {
                if (texture != null) {
                    PersonaAtlasPacker.Box tile = packer.tile(texture, tintMap, frameHeight, frameCount);
                    offsetFaceUvs(uv.getAsJsonObject(), tile.x(), tile.y());
                }
            } else if (uv != null && texture != null) {
                PersonaAtlasPacker.Box tile = packer.tile(texture, tintMap, frameHeight, frameCount);
                offsetUv(uv, tile.x(), tile.y());
            }

            if (cube.has("rotation") && cube.get("rotation").isJsonArray()) {
                JsonObject child = new JsonObject();
                String parentName = string(bone, "name");
                child.addProperty("name", parentName + "_persona_cube_" + packer.nextCubeId++);
                child.addProperty("parent", parentName);
                child.add("pivot", cube.has("pivot") ? cube.remove("pivot") : inferredPivot(bone));
                child.add("rotation", cube.remove("rotation"));
                JsonArray childCubes = new JsonArray();
                childCubes.add(cube);
                child.add("cubes", childCubes);
                packer.extraBones.add(child);
            } else {
                rewritten.add(cube);
            }
        }
        bone.add("cubes", rewritten);
    }

    /** Converts a legacy singular poly_mesh into the mergeable form consumed by the Java model baker. */
    private static void rewritePolyMesh(JsonObject bone, BufferedImage texture, BufferedImage tintMap, int frameHeight,
                                        int frameCount, PersonaAtlasPacker packer) {
        JsonObject mesh = bone.has("poly_mesh") && bone.get("poly_mesh").isJsonObject()
            ? bone.remove("poly_mesh").getAsJsonObject() : null;
        if (mesh == null) return;

        JsonArray uvs = mesh.getAsJsonArray("uvs");
        if (uvs != null && texture != null) {
            PersonaAtlasPacker.Box tile = packer.tile(texture, tintMap, frameHeight, frameCount);
            float sourceHeight = frameHeight > 0 ? frameHeight : texture.getHeight();
            boolean normalized = !mesh.has("normalized_uvs") || mesh.get("normalized_uvs").getAsBoolean();
            for (JsonElement uvElement : uvs) {
                if (!uvElement.isJsonArray() || uvElement.getAsJsonArray().size() < 2) continue;
                JsonArray uv = uvElement.getAsJsonArray();
                float u = uv.get(0).getAsFloat();
                float v = uv.get(1).getAsFloat();
                if (normalized) {
                    u *= texture.getWidth();
                    v *= sourceHeight;
                }
                uv.set(0, number(tile.x() + u));
                // Legacy poly_mesh UVs use a bottom-left origin, while the Java
                // texture atlas uses PNG's top-left origin.
                uv.set(1, number(tile.y() + sourceHeight - v));
            }
            mesh.addProperty("normalized_uvs", false);
        }

        JsonArray meshes = new JsonArray();
        meshes.add(mesh);
        bone.add("poly_meshes", meshes);
    }

    private static void mergeBone(JsonArray targetBones, Map<String, JsonObject> byName, JsonObject incoming) {
        String name = string(incoming, "name");
        JsonObject existing = byName.get(name);
        if (existing == null) {
            targetBones.add(incoming);
            byName.put(name, incoming);
        } else {
            JsonArray targetCubes = existing.getAsJsonArray("cubes");
            if (targetCubes == null) {
                targetCubes = new JsonArray();
                existing.add("cubes", targetCubes);
            }
            JsonArray incomingCubes = incoming.getAsJsonArray("cubes");
            if (incomingCubes != null) for (JsonElement cube : incomingCubes) targetCubes.add(cube);
            JsonArray incomingMeshes = incoming.getAsJsonArray("poly_meshes");
            if (incomingMeshes != null) {
                JsonArray targetMeshes = existing.getAsJsonArray("poly_meshes");
                if (targetMeshes == null) {
                    targetMeshes = new JsonArray();
                    existing.add("poly_meshes", targetMeshes);
                }
                for (JsonElement mesh : incomingMeshes) targetMeshes.add(mesh);
            }
        }
    }

    private static void offsetFaceUvs(JsonObject faces, int x, int y) {
        for (String face : List.of("down", "up", "west", "north", "east", "south")) {
            if (!faces.has(face) || !faces.get(face).isJsonObject()) continue;
            JsonObject data = faces.getAsJsonObject(face);
            if (data.has("uv")) offsetUv(data.get("uv"), x, y);
        }
    }

    private static void offsetUv(JsonElement uv, int x, int y) {
        JsonArray values = uv.isJsonArray() ? uv.getAsJsonArray()
            : uv.isJsonObject() ? uv.getAsJsonObject().getAsJsonArray("uv") : null;
        if (values != null && values.size() >= 2) {
            values.set(0, number(values.get(0).getAsFloat() + x));
            values.set(1, number(values.get(1).getAsFloat() + y));
        }
    }

    private static JsonElement inferredPivot(JsonObject bone) {
        return bone.has("pivot") ? bone.get("pivot").deepCopy() : array(0, 0, 0);
    }

    private static boolean isPerFaceUv(JsonObject uv) {
        return uv.has("north") || uv.has("south") || uv.has("east") || uv.has("west") || uv.has("up") || uv.has("down");
    }

    private static JsonObject findGeometry(JsonObject file, String name) {
        if (file.has(name) && file.get(name).isJsonObject()) return file.getAsJsonObject(name);
        JsonArray geometries = file.getAsJsonArray("minecraft:geometry");
        if (geometries != null) for (JsonElement element : geometries) {
            JsonObject geometry = element.getAsJsonObject();
            JsonObject description = geometry.getAsJsonObject("description");
            if (description != null && name.equals(string(description, "identifier"))) return geometry;
        }
        return null;
    }

    private static JsonObject resolveBaseGeometry(JsonObject vanillaGeometry, String identifier) {
        JsonObject geometry = findGeometry(vanillaGeometry, identifier);
        if (geometry == null) return null;
        JsonObject wrapper = new JsonObject();
        wrapper.addProperty("format_version", "1.12.0");
        JsonArray geometries = new JsonArray();
        geometries.add(geometry.deepCopy());
        wrapper.add("minecraft:geometry", geometries);
        return wrapper;
    }

    private static void setTextureSize(JsonObject file, int width, int height) {
        JsonArray geometries = file == null ? null : file.getAsJsonArray("minecraft:geometry");
        if (geometries == null || geometries.isEmpty()) return;
        JsonObject description = geometries.get(0).getAsJsonObject().getAsJsonObject("description");
        if (description == null) return;
        description.addProperty("texture_width", width);
        description.addProperty("texture_height", height);
    }

    private static Map<String, JsonObject> loadGeometries(File directory) throws Exception {
        Map<String, JsonObject> result = new HashMap<>();
        File[] files = directory.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".geometry.json"));
        if (files == null) return result;
        for (File file : files) {
            JsonObject fileJson = readJson(file);
            JsonArray entries = fileJson.getAsJsonArray("minecraft:geometry");
            if (entries != null) {
                for (JsonElement element : entries) {
                    if (!element.isJsonObject()) continue;
                    JsonObject geometry = element.getAsJsonObject();
                    JsonObject description = geometry.getAsJsonObject("description");
                    String identifier = string(description, "identifier");
                    if (identifier != null) result.putIfAbsent(identifier, geometry);
                }
            }
            for (Map.Entry<String, JsonElement> entry : fileJson.entrySet()) {
                if (entry.getValue().isJsonObject() && entry.getValue().getAsJsonObject().has("bones")) {
                    result.putIfAbsent(entry.getKey(), entry.getValue().getAsJsonObject());
                }
            }
        }
        return result;
    }

    private static BufferedImage readImage(File directory, String name) throws Exception {
        if (name == null) return null;
        File exact = new File(directory, name);
        if (exact.isFile()) return readImageFile(exact);
        File[] matches = directory.listFiles((dir, child) -> child.equalsIgnoreCase(name));
        return matches != null && matches.length > 0 ? readImageFile(matches[0]) : null;
    }

    private static BufferedImage readImageFile(File file) throws Exception {
        BufferedImage image = ImageIO.read(file);
        if (image != null) return image;
        if (file.getName().toLowerCase(Locale.ROOT).endsWith(".tga")) return readTga(file);
        return null;
    }

    /** Reads the uncompressed/RLE true-color TGA tint maps used by Persona pieces. */
    private static BufferedImage readTga(File file) throws Exception {
        try (DataInputStream input = new DataInputStream(new FileInputStream(file))) {
            int idLength = input.readUnsignedByte();
            int colorMapType = input.readUnsignedByte();
            int imageType = input.readUnsignedByte();
            input.skipNBytes(9);
            int width = readUnsignedShortLE(input);
            int height = readUnsignedShortLE(input);
            int bits = input.readUnsignedByte();
            int descriptor = input.readUnsignedByte();
            if (colorMapType != 0 || (imageType != 2 && imageType != 10) || (bits != 24 && bits != 32)
                    || width <= 0 || height <= 0) {
                throw new IllegalArgumentException("Unsupported Persona TGA format in " + file.getName());
            }
            input.skipNBytes(idLength);
            int[] pixels = new int[width * height];
            int written = 0;
            while (written < pixels.length) {
                int count = 1;
                boolean repeated = false;
                if (imageType == 10) {
                    int packet = input.readUnsignedByte();
                    count = (packet & 0x7F) + 1;
                    repeated = (packet & 0x80) != 0;
                }
                if (repeated) {
                    int pixel = readTgaPixel(input, bits);
                    for (int i = 0; i < count && written < pixels.length; i++) pixels[written++] = pixel;
                } else {
                    for (int i = 0; i < count && written < pixels.length; i++) pixels[written++] = readTgaPixel(input, bits);
                }
            }
            BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            boolean topOrigin = (descriptor & 0x20) != 0;
            boolean rightOrigin = (descriptor & 0x10) != 0;
            for (int sourceY = 0; sourceY < height; sourceY++) {
                int y = topOrigin ? sourceY : height - 1 - sourceY;
                for (int sourceX = 0; sourceX < width; sourceX++) {
                    int x = rightOrigin ? width - 1 - sourceX : sourceX;
                    result.setRGB(x, y, pixels[sourceY * width + sourceX]);
                }
            }
            return result;
        }
    }

    private static int readUnsignedShortLE(DataInputStream input) throws Exception {
        int low = input.readUnsignedByte();
        return low | (input.readUnsignedByte() << 8);
    }

    private static int readTgaPixel(DataInputStream input, int bits) throws Exception {
        int blue = input.readUnsignedByte();
        int green = input.readUnsignedByte();
        int red = input.readUnsignedByte();
        int alpha = bits == 32 ? input.readUnsignedByte() : 0xFF;
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private static BufferedImage readImageUnchecked(File directory, String name) {
        try { return readImage(directory, name); }
        catch (Exception e) { return null; }
    }

    private static BufferedImage readTintMap(File directory, String name, int channel) throws Exception {
        BufferedImage source = readImage(directory, name);
        if (source == null) return null;
        BufferedImage mask = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int shift = switch (channel) {
            case 1 -> 8;
            case 2 -> 0;
            case 3 -> 24;
            default -> 16;
        };
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int amount = (source.getRGB(x, y) >>> shift) & 0xFF;
                mask.setRGB(x, y, 0xFF000000 | (amount << 16));
            }
        }
        return mask;
    }

    private static BufferedImage readTintMapUnchecked(File directory, String name, int channel) {
        try { return readTintMap(directory, name, channel); }
        catch (Exception e) { return null; }
    }

    private static JsonObject readJson(File file) throws Exception {
        try (var reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static byte[] encodePng(BufferedImage image) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "png", output)) throw new IllegalStateException("PNG writer is unavailable");
        return output.toByteArray();
    }

    private static byte[] encodeTexturePayload(BufferedImage atlas, BufferedImage tintAtlas, int tintBaseColor,
                                               PersonaAtlasPacker packer) throws Exception {
        byte[] base = encodePng(atlas);
        byte[] tintMask = tintAtlas == null ? new byte[0] : encodePng(tintAtlas);
        if (packer.animations.isEmpty() && tintMask.length == 0) return base;
        List<PersonaTexturePayload.AnimationRegion> regions = new ArrayList<>();
        for (PersonaAtlasPacker.AnimationTile animation : packer.animations) {
            regions.add(new PersonaTexturePayload.AnimationRegion(
                animation.box().x(), animation.box().y(), animation.source().getWidth(),
                animation.frameHeight(), animation.frameCount(), encodePng(animation.source()),
                animation.tintMask() == null ? new byte[0] : encodePng(animation.tintMask())));
        }
        return PersonaTexturePayload.encode(base, tintMask, tintBaseColor, regions);
    }

    private static TintSpec tintSpec(JsonObject metadata) {
        JsonObject base = metadata.has("tint_base_color") && metadata.get("tint_base_color").isJsonObject()
            ? metadata.getAsJsonObject("tint_base_color") : null;
        JsonObject selected = metadata.has("tint_color") && metadata.get("tint_color").isJsonObject()
            ? metadata.getAsJsonObject("tint_color") : null;
        String[] names = {"r_color", "g_color", "b_color", "a_color"};
        for (int channel = 0; channel < names.length; channel++) {
            Integer baseColor = parseColor(string(base, names[channel]));
            Integer selectedColor = parseColor(string(selected, names[channel]));
            if (baseColor != null || selectedColor != null) {
                int authoredColor = baseColor != null ? baseColor : selectedColor;
                int defaultColor = selectedColor != null ? selectedColor : authoredColor;
                return new TintSpec(channel, authoredColor, defaultColor);
            }
        }
        return new TintSpec(0, 0xFFFFFF, 0xFFFFFF);
    }

    private static Integer parseColor(String value) {
        if (value == null) return null;
        try {
            return (int) (Long.parseLong(value.startsWith("#") ? value.substring(1) : value, 16) & 0xFFFFFFL);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String string(JsonObject object, String key) {
        JsonElement value = object == null ? null : object.get(key);
        return value == null || value.isJsonNull() ? null : value.getAsString();
    }

    private static int intValue(JsonObject object, String key, int fallback) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? fallback : value.getAsInt();
    }

    private static String sanitize(String value) {
        return value == null ? "piece" : value.replaceAll("[^a-zA-Z0-9_.-]", "_");
    }

    private static JsonArray array(Number... values) {
        JsonArray array = new JsonArray();
        for (Number value : values) array.add(value);
        return array;
    }

    private static JsonElement number(float value) {
        return new com.google.gson.JsonPrimitive(value);
    }

}

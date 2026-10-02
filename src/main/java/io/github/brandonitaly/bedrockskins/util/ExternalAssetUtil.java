package io.github.brandonitaly.bedrockskins.util;

import io.github.brandonitaly.bedrockskins.pack.model.AssetSource;
import io.github.brandonitaly.bedrockskins.pack.model.LoadedSkin;
import io.github.brandonitaly.bedrockskins.pack.loader.SkinPackLoader;
import io.github.brandonitaly.bedrockskins.pack.editor.LangFileEditor;
import io.github.brandonitaly.bedrockskins.pack.editor.SkinManifestEditor;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

public class ExternalAssetUtil {

    /**
     * Helper to load texture data directly from a LoadedSkin object.
     */
    public static byte[] loadTextureData(LoadedSkin skin, Minecraft minecraft) {
        if (skin == null || skin.texture == null) return new byte[0];
        return loadTextureData(skin.texture, minecraft);
    }

    /**
     * Reads the raw byte array of a skin texture, supporting built-in, folder, and ZIP assets.
     */
    public static byte[] loadTextureData(AssetSource src, Minecraft minecraft) {
        if (src == null) return new byte[0];

        try {
            return switch (src) {
                case AssetSource.Resource(Identifier id) -> minecraft.getResourceManager().getResource(id).map(r -> {
                    try (InputStream in = r.open()) { 
                        return in.readAllBytes(); 
                    } catch (Exception e) { return new byte[0]; }
                }).orElse(new byte[0]);
                case AssetSource.File(String path) -> Files.readAllBytes(Path.of(path));
                case AssetSource.Memory(byte[] data) -> data;
                case AssetSource.Remote ignored -> new byte[0];
            };
        } catch (Exception ignored) {}
        return new byte[0];
    }

    public static boolean deleteImportedSkinFiles(LoadedSkin skin) {
        if (skin == null || !"skinpack.Imports".equals(skin.packId)) return false;

        Path packDir = SkinPackLoader.getSkinPacksDir().toPath().resolve("Imports");
        Path manifest = packDir.resolve("skins.json");
        try {
            if (SkinManifestEditor.remove(manifest, skin.skinDisplayName).isEmpty()) return false;

            deleteAsset(skin.texture, packDir);
            deleteAsset(skin.cape, packDir);
            LangFileEditor.remove(packDir.resolve("texts").resolve("en_us.lang"), skin.safeSkinName);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static void deleteAsset(AssetSource source, Path packDir) throws IOException {
        if (source instanceof AssetSource.File(String path)) {
            Path asset = Path.of(path).toAbsolutePath().normalize();
            Path root = packDir.toAbsolutePath().normalize();
            if (asset.startsWith(root)) Files.deleteIfExists(asset);
        }
    }
}

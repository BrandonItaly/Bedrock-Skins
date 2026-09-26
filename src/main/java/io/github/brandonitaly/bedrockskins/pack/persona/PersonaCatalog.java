package io.github.brandonitaly.bedrockskins.pack.persona;

import com.mojang.logging.LogUtils;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.HexFormat;
import java.security.MessageDigest;
import java.util.stream.Stream;

/** Discovers Persona piece directories once and shares the result with all Persona loaders. */
public final class PersonaCatalog {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static volatile List<Path> pieceDirectories = List.of();
    private static Map<Path, String> contentSnapshot = Map.of();
    private static long revision;
    private PersonaCatalog() {}

    public static void reload(Path externalRoot, ResourceManager resources) {
        boolean bundledComplete = PersonaResourceLoader.beginReload(resources);
        Set<Path> discovered = new LinkedHashSet<>();
        try {
            Files.createDirectories(externalRoot);
        } catch (Exception exception) {
            LOGGER.warn("Failed to create Persona directory {}", externalRoot, exception);
        }
        Map<Path, String> snapshot = new HashMap<>();
        boolean complete = scan(externalRoot, discovered, snapshot);
        Map<Path, String> bundled = PersonaResourceLoader.contentSnapshot();
        snapshot.putAll(bundled);
        bundled.keySet().stream().filter(PersonaCatalog::isMetadata)
            .map(Path::getParent).sorted().forEach(discovered::add);
        if (!complete || !bundledComplete || !snapshot.equals(contentSnapshot)) revision++;
        contentSnapshot = snapshot;
        pieceDirectories = List.copyOf(discovered);
    }

    public static List<Path> pieceDirectories() { return pieceDirectories; }
    public static long revision() { return revision; }

    private static boolean scan(Path root, Set<Path> discovered, Map<Path, String> snapshot) {
        if (!Files.isDirectory(root)) return true;
        boolean complete = true;
        try (Stream<Path> paths = Files.walk(root)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            for (Path path : paths.filter(Files::isRegularFile).sorted().toList()) {
                if (isMetadata(path)) discovered.add(path.getParent());
                try (var input = Files.newInputStream(path)) {
                    digest.reset();
                    int count;
                    while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
                    snapshot.put(path, HexFormat.of().formatHex(digest.digest()));
                } catch (Exception exception) {
                    complete = false;
                    LOGGER.warn("Failed to read Persona asset {}", path, exception);
                }
            }
        } catch (Exception exception) {
            LOGGER.warn("Failed to scan Persona content in {}", root, exception);
            return false;
        }
        return complete;
    }

    private static boolean isMetadata(Path path) {
        return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".meta.json");
    }
}

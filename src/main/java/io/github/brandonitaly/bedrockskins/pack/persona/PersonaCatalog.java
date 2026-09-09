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
import java.util.stream.Stream;

/** Discovers Persona piece directories once and shares the result with all Persona loaders. */
public final class PersonaCatalog {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static volatile List<Path> pieceDirectories = List.of();
    private PersonaCatalog() {}

    public static void reload(Path externalRoot, ResourceManager resources) {
        PersonaResourceLoader.beginReload(resources);
        Set<Path> discovered = new LinkedHashSet<>();
        try {
            Files.createDirectories(externalRoot);
        } catch (Exception exception) {
            LOGGER.warn("Failed to create Persona directory {}", externalRoot, exception);
        }
        scan(externalRoot, discovered);
        PersonaResourceLoader.forEachBundledRoot(resources, root -> scan(root, discovered));
        pieceDirectories = List.copyOf(discovered);
    }

    public static List<Path> pieceDirectories() { return pieceDirectories; }

    private static void scan(Path root, Set<Path> discovered) {
        if (!Files.isDirectory(root)) return;
        try (Stream<Path> paths = Files.walk(root, 3)) {
            paths.filter(Files::isRegularFile)
                .filter(PersonaCatalog::isMetadata)
                .map(Path::getParent)
                .forEach(discovered::add);
        } catch (Exception exception) {
            LOGGER.warn("Failed to scan Persona content in {}", root, exception);
        }
    }

    private static boolean isMetadata(Path path) {
        return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".meta.json");
    }
}

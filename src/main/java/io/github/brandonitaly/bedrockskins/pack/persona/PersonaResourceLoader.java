package io.github.brandonitaly.bedrockskins.pack.persona;

import com.mojang.logging.LogUtils;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.function.Consumer;

/** Exposes bundled Persona packs as directories for the existing Bedrock file loaders. */
public final class PersonaResourceLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String RESOURCE_PREFIX = "persona/";
    private static Path extractedRoot;

    private PersonaResourceLoader() {}

    /** Extracts the active resource view once for both cosmetic and emote loading. */
    public static synchronized void beginReload(ResourceManager manager) {
        deleteTree(extractedRoot);
        extractedRoot = null;
        Path extracted = null;
        try {
            extracted = Files.createTempDirectory("bedrockskins-bundled-persona-");
            final Path targetRoot = extracted;
            final boolean[] copied = {false};

            manager.listResources("persona", id ->
                "bedrockskins".equals(id.getNamespace())).forEach((id, resource) -> {
                String path = id.getPath();
                if (!path.startsWith(RESOURCE_PREFIX)) return;
                Path target = safeTarget(targetRoot, path.substring(RESOURCE_PREFIX.length()));
                if (target == null) return;
                try {
                    Files.createDirectories(target.getParent());
                    try (var input = resource.open()) {
                        Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                    copied[0] = true;
                } catch (Exception exception) {
                    LOGGER.warn("Failed to extract bundled Persona resource {}", id, exception);
                }
            });
            if (copied[0]) {
                extractedRoot = targetRoot;
                extracted = null;
            }
        } catch (Exception exception) {
            LOGGER.warn("Failed to discover bundled Persona resources", exception);
        } finally {
            deleteTree(extracted);
        }
    }

    public static void forEachBundledRoot(ResourceManager manager, Consumer<Path> consumer) {
        Path root;
        synchronized (PersonaResourceLoader.class) {
            if (extractedRoot == null) beginReload(manager);
            root = extractedRoot;
        }
        if (root != null) consumer.accept(root);
    }

    private static Path safeTarget(Path root, String relative) {
        Path target = root.resolve(relative).normalize();
        return target.startsWith(root) ? target : null;
    }

    private static void deleteTree(Path root) {
        if (root == null || !Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (Exception ignored) {
                }
            });
        } catch (Exception ignored) {
        }
    }
}

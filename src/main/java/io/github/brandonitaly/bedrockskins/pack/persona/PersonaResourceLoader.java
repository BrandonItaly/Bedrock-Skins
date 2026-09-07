package io.github.brandonitaly.bedrockskins.pack.persona;

import com.mojang.logging.LogUtils;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.function.Consumer;

/** Exposes bundled Persona packs as directories for the existing Bedrock file loaders. */
public final class PersonaResourceLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String RESOURCE_ROOT = "assets/bedrockskins/persona";
    private static final String RESOURCE_PREFIX = "persona/";

    private PersonaResourceLoader() {}

    public static void forEachBundledRoot(ResourceManager manager, Consumer<Path> consumer) {
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

            Enumeration<URL> roots = PersonaResourceLoader.class.getClassLoader().getResources(RESOURCE_ROOT);
            while (roots.hasMoreElements()) {
                URL url = roots.nextElement();
                if ("file".equalsIgnoreCase(url.getProtocol())) {
                    consumer.accept(Path.of(url.toURI()));
                } else if (url.openConnection() instanceof JarURLConnection connection) {
                    copied[0] |= copyJarRoot(connection.getJarFile(), targetRoot);
                }
            }

            if (copied[0]) consumer.accept(targetRoot);
        } catch (Exception exception) {
            LOGGER.warn("Failed to discover bundled Persona resources", exception);
        } finally {
            deleteTree(extracted);
        }
    }

    private static boolean copyJarRoot(JarFile jar, Path targetRoot) {
        boolean copied = false;
        String prefix = RESOURCE_ROOT + "/";
        try {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().startsWith(prefix)) continue;
                Path target = safeTarget(targetRoot, entry.getName().substring(prefix.length()));
                if (target == null) continue;
                Files.createDirectories(target.getParent());
                try (var input = jar.getInputStream(entry)) {
                    Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
                }
                copied = true;
            }
        } catch (Exception exception) {
            LOGGER.warn("Failed to extract bundled Persona files from {}", jar.getName(), exception);
        }
        return copied;
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

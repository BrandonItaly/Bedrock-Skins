package io.github.brandonitaly.bedrockskins.pack.persona;

import com.mojang.logging.LogUtils;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.security.MessageDigest;

/** Exposes bundled Persona packs as directories for the existing Bedrock file loaders. */
public final class PersonaResourceLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String RESOURCE_PREFIX = "persona/";
    private static Path extractedRoot;
    private static final Map<Path, byte[]> fingerprints = new HashMap<>();

    private PersonaResourceLoader() {}

    /** Extracts the active resource view once for both cosmetic and emote loading. */
    public static synchronized boolean beginReload(ResourceManager manager) {
        try {
            if (extractedRoot == null) extractedRoot = Files.createTempDirectory("bedrockskins-bundled-persona-");
            Set<Path> present = new HashSet<>();
            boolean complete = true;

            var resources = manager.listResources("persona", id -> "bedrockskins".equals(id.getNamespace()));
            for (var entry : resources.entrySet()) {
                var id = entry.getKey();
                String path = id.getPath();
                if (!path.startsWith(RESOURCE_PREFIX)) continue;
                Path target = safeTarget(extractedRoot, path.substring(RESOURCE_PREFIX.length()));
                if (target == null) continue;
                present.add(target);
                try (var input = entry.getValue().open()) {
                    byte[] data = input.readAllBytes();
                    byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
                    if (!Arrays.equals(digest, fingerprints.get(target)) || !Files.isRegularFile(target)) {
                        Files.createDirectories(target.getParent());
                        Files.write(target, data);
                        fingerprints.put(target, digest);
                    }
                } catch (Exception exception) {
                    complete = false;
                    fingerprints.remove(target);
                    try { Files.deleteIfExists(target); } catch (Exception ignored) {}
                    LOGGER.warn("Failed to extract bundled Persona resource {}", id, exception);
                }
            }
            for (Path stale : Set.copyOf(fingerprints.keySet())) {
                if (!present.contains(stale)) {
                    Files.deleteIfExists(stale);
                    fingerprints.remove(stale);
                }
            }
            return complete;
        } catch (Exception exception) {
            LOGGER.warn("Failed to discover bundled Persona resources", exception);
            return false;
        }
    }

    private static Path safeTarget(Path root, String relative) {
        Path target = root.resolve(relative).normalize();
        return target.startsWith(root) ? target : null;
    }

    static synchronized Map<Path, String> contentSnapshot() {
        Map<Path, String> snapshot = new HashMap<>();
        fingerprints.forEach((path, digest) -> snapshot.put(path, java.util.HexFormat.of().formatHex(digest)));
        return snapshot;
    }
}

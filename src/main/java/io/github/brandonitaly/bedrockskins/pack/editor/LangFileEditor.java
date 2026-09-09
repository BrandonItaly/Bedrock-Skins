package io.github.brandonitaly.bedrockskins.pack.editor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Exact-key updates for Bedrock .lang files without duplicated regex rewrites. */
public final class LangFileEditor {
    private LangFileEditor() {}

    public static void put(Path file, String key, String value) throws IOException {
        List<String> lines = read(file);
        String prefix = key + "=";
        String replacement = prefix + value;
        boolean replaced = false;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith(prefix)) {
                lines.set(i, replacement);
                replaced = true;
                break;
            }
        }
        if (!replaced) lines.add(replacement);
        write(file, lines);
    }

    public static void remove(Path file, String key) throws IOException {
        if (!Files.isRegularFile(file)) return;
        String prefix = key + "=";
        List<String> lines = read(file);
        lines.removeIf(line -> line.startsWith(prefix));
        write(file, lines);
    }

    private static List<String> read(Path file) throws IOException {
        return Files.isRegularFile(file)
            ? new ArrayList<>(Files.readAllLines(file, StandardCharsets.UTF_8)) : new ArrayList<>();
    }

    private static void write(Path file, List<String> lines) throws IOException {
        Files.createDirectories(file.getParent());
        String content = String.join("\n", lines);
        if (!content.isEmpty()) content += "\n";
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }
}

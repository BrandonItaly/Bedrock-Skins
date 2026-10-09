package io.github.brandonitaly.packaging;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.*;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.jar.Manifest;
import java.util.zip.*;

/** Combines loader release jars without changing their Minecraft mappings. */
public abstract class UniversalJar extends DefaultTask {
    static final String ORIGINAL = "io/github/brandonitaly/bedrockskins";
    static final String NEOFORGE = "io/github/brandonitaly/bedrockskins_neoforge";
    private static final String MANIFEST = "META-INF/MANIFEST.MF";

    @InputFile @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getFabricJar();

    @InputFile @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getNeoForgeJar();

    @OutputFile
    public abstract RegularFileProperty getOutputJar();

    @TaskAction
    public void assemble() throws IOException {
        merge(getFabricJar().get().getAsFile().toPath(), getNeoForgeJar().get().getAsFile().toPath(),
            getOutputJar().get().getAsFile().toPath());
    }

    static void merge(Path fabric, Path neoForge, Path output) throws IOException {
        Map<String, byte[]> entries = new TreeMap<>();
        read(fabric, entries, false);
        read(neoForge, entries, true);
        for (String required : List.of("fabric.mod.json", "META-INF/neoforge.mods.toml",
                "bedrockskins.mixins.json", "bedrockskins-neoforge.mixins.json",
                ORIGINAL + "/BedrockSkins.class", NEOFORGE + "/BedrockSkins.class")) {
            if (!entries.containsKey(required)) throw new IOException("Missing combined jar entry: " + required);
        }
        Files.createDirectories(output.toAbsolutePath().getParent());
        try (var zip = new ZipOutputStream(Files.newOutputStream(output))) {
            for (var entry : entries.entrySet()) {
                ZipEntry file = new ZipEntry(entry.getKey());
                file.setTime(0);
                zip.putNextEntry(file);
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
    }

    private static void read(Path source, Map<String, byte[]> entries, boolean neoForge) throws IOException {
        try (var zip = new ZipFile(source.toFile())) {
            Map<String, String> resources = new LinkedHashMap<>();
            if (neoForge) {
                zip.stream().filter(entry -> !entry.isDirectory()).forEach(entry -> {
                    String name = entry.getName();
                    if (name.endsWith(".mixins.json") || name.endsWith(".refmap.json") || name.endsWith(".accesswidener")) {
                        int dot = name.indexOf('.');
                        resources.put(name, name.substring(0, dot) + "-neoforge" + name.substring(dot));
                    }
                });
            }
            var files = zip.entries();
            while (files.hasMoreElements()) {
                ZipEntry file = files.nextElement();
                String name = file.getName();
                if (file.isDirectory() || name.matches("META-INF/.*\\.(SF|RSA|DSA)")) continue;
                byte[] data;
                try (var stream = zip.getInputStream(file)) { data = stream.readAllBytes(); }
                if (neoForge) {
                    if (name.endsWith(".class")) {
                        data = relocateClass(data);
                        name = relocate(name);
                    } else if (name.equals("META-INF/neoforge.mods.toml") || name.equals(MANIFEST)
                            || resources.containsKey(name) || name.startsWith("META-INF/services/")) {
                        String text = relocate(new String(data, StandardCharsets.UTF_8));
                        for (var resource : resources.entrySet()) text = text.replace(resource.getKey(), resource.getValue());
                        data = text.getBytes(StandardCharsets.UTF_8);
                        name = relocate(resources.getOrDefault(name, name));
                    }
                    if (name.equals(MANIFEST)) {
                        // NeoForge needs its module manifest; Fabric also needs its mapping metadata.
                        Manifest manifest = new Manifest(new ByteArrayInputStream(data));
                        byte[] fabricManifest = entries.get(MANIFEST);
                        if (fabricManifest != null) {
                            new Manifest(new ByteArrayInputStream(fabricManifest)).getMainAttributes().forEach((key, value) -> {
                                if (key.toString().startsWith("Fabric-")) manifest.getMainAttributes().put(key, value);
                            });
                        }
                        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                        manifest.write(bytes);
                        entries.put(name, bytes.toByteArray());
                        continue;
                    }
                }
                byte[] existing = entries.putIfAbsent(name, data);
                if (existing != null && !Arrays.equals(existing, data)) {
                    throw new IOException("Conflicting resource in combined jar: " + name);
                }
            }
        }
    }

    static String relocate(String value) {
        return value.replace(ORIGINAL + "/", NEOFORGE + "/")
            .replace(ORIGINAL.replace('/', '.') + ".", NEOFORGE.replace('/', '.') + ".");
    }

    private static byte[] relocateClass(byte[] source) {
        ClassWriter writer = new ClassWriter(0);
        Remapper remapper = new Remapper(Opcodes.ASM9) {
            @Override public String map(String name) { return relocate(name); }
            @Override public Object mapValue(Object value) {
                return value instanceof String text ? relocate(text) : super.mapValue(value);
            }
        };
        new ClassReader(source).accept(new ClassRemapper(writer, remapper), 0);
        return writer.toByteArray();
    }
}

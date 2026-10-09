package io.github.brandonitaly.packaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.jar.Manifest;
import java.util.zip.*;

import static org.junit.jupiter.api.Assertions.*;

class UniversalJarTest {
    @TempDir Path directory;

    private byte[] exampleClass(boolean neoForge) {
        ClassWriter writer = new ClassWriter(0);
        String owner = UniversalJar.ORIGINAL + "/BedrockSkins";
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, owner, null, "java/lang/Object", null);
        writer.visitField(Opcodes.ACC_PUBLIC, "self", "L" + owner + ";", null, null).visitEnd();
        writer.visitField(Opcodes.ACC_PUBLIC, "minecraft", neoForge
            ? "Lnet/minecraft/client/Minecraft;" : "Lnet/minecraft/class_310;", null, null).visitEnd();
        var method = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "name", "()Ljava/lang/String;", null, null);
        method.visitCode();
        method.visitLdcInsn(owner.replace('/', '.'));
        method.visitInsn(Opcodes.ARETURN);
        method.visitMaxs(1, 0);
        method.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private Path input(String name, boolean neoForge, String asset) throws IOException {
        Path path = directory.resolve(name);
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put(UniversalJar.ORIGINAL + "/BedrockSkins.class", exampleClass(neoForge));
        entries.put(neoForge ? "META-INF/neoforge.mods.toml" : "fabric.mod.json",
            (neoForge ? "config=\"bedrockskins.mixins.json\"" : "{\"id\":\"bedrockskins\"}").getBytes(StandardCharsets.UTF_8));
        entries.put("bedrockskins.mixins.json", ("{\"package\":\"" + UniversalJar.ORIGINAL.replace('/', '.')
            + ".mixin\",\"refmap\":\"bedrockskins.refmap.json\"}").getBytes(StandardCharsets.UTF_8));
        entries.put("bedrockskins.refmap.json", ("{\"mappings\":{\"" + UniversalJar.ORIGINAL
            + "/mixin/Example\":{}}}").getBytes(StandardCharsets.UTF_8));
        entries.put("META-INF/MANIFEST.MF", ("Manifest-Version: 1.0\r\n" + (neoForge
            ? "Automatic-Module-Name: bedrockskins\r\n"
            : "Fabric-Loom-Mixin-Remap-Type: static\r\nFabric-Mapping-Namespace: intermediary\r\n")
            + "\r\n").getBytes(StandardCharsets.UTF_8));
        entries.put("assets/bedrockskins/test.txt", asset.getBytes(StandardCharsets.UTF_8));
        try (var zip = new ZipOutputStream(Files.newOutputStream(path))) {
            for (var entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
        return path;
    }

    private static byte[] read(ZipFile zip, String name) throws IOException {
        try (var stream = zip.getInputStream(zip.getEntry(name))) { return stream.readAllBytes(); }
    }

    @Test
    void combinesMappedLoaderClassesWithoutChangingMinecraftReferences() throws IOException {
        Path output = directory.resolve("universal.jar");
        UniversalJar.merge(input("fabric.jar", false, "shared"), input("neo.jar", true, "shared"), output);
        try (var zip = new ZipFile(output.toFile())) {
            assertArrayEquals(exampleClass(false), read(zip, UniversalJar.ORIGINAL + "/BedrockSkins.class"));
            new ClassReader(read(zip, UniversalJar.NEOFORGE + "/BedrockSkins.class")).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override public void visit(int version, int access, String name, String signature, String parent, String[] interfaces) {
                    assertEquals(UniversalJar.NEOFORGE + "/BedrockSkins", name);
                }
                @Override public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                    assertEquals(name.equals("self") ? "L" + UniversalJar.NEOFORGE + "/BedrockSkins;"
                        : "Lnet/minecraft/client/Minecraft;", descriptor);
                    return null;
                }
                @Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override public void visitLdcInsn(Object value) {
                            assertEquals(UniversalJar.NEOFORGE.replace('/', '.') + ".BedrockSkins", value);
                        }
                    };
                }
            }, 0);
            assertTrue(new String(read(zip, "META-INF/neoforge.mods.toml"), StandardCharsets.UTF_8)
                .contains("bedrockskins-neoforge.mixins.json"));
            String config = new String(read(zip, "bedrockskins-neoforge.mixins.json"), StandardCharsets.UTF_8);
            assertTrue(config.contains("bedrockskins_neoforge.mixin"));
            assertTrue(config.contains("bedrockskins-neoforge.refmap.json"));
            assertTrue(new String(read(zip, "bedrockskins-neoforge.refmap.json"), StandardCharsets.UTF_8)
                .contains(UniversalJar.NEOFORGE + "/mixin/Example"));
            var manifest = new Manifest(new ByteArrayInputStream(read(zip, "META-INF/MANIFEST.MF")));
            assertEquals("bedrockskins", manifest.getMainAttributes().getValue("Automatic-Module-Name"));
            assertEquals("static", manifest.getMainAttributes().getValue("Fabric-Loom-Mixin-Remap-Type"));
            assertEquals("intermediary", manifest.getMainAttributes().getValue("Fabric-Mapping-Namespace"));
            assertEquals(1, zip.stream().filter(entry -> entry.getName().equals("assets/bedrockskins/test.txt")).count());
            assertNull(zip.getEntry("META-INF/mods.toml"));
        }
    }

    @Test
    void rejectsConflictingAssetsBeforeWritingOutput() throws IOException {
        Path output = directory.resolve("universal.jar");
        Path fabric = input("fabric.jar", false, "fabric");
        Path neoForge = input("neo.jar", true, "neo");
        var error = assertThrows(IOException.class, () -> UniversalJar.merge(fabric, neoForge, output));
        assertTrue(error.getMessage().contains("Conflicting resource"));
        assertFalse(Files.exists(output));
    }

    @Test
    void producesDeterministicArchives() throws IOException {
        Path fabric = input("fabric.jar", false, "shared");
        Path neoForge = input("neo.jar", true, "shared");
        Path first = directory.resolve("first.jar");
        Path second = directory.resolve("second.jar");
        UniversalJar.merge(fabric, neoForge, first);
        UniversalJar.merge(fabric, neoForge, second);
        assertArrayEquals(Files.readAllBytes(first), Files.readAllBytes(second));
    }
}

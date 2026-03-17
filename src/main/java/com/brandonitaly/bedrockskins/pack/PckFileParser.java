package com.brandonitaly.bedrockskins.pack;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parser for Minecraft Legacy Console .pck containers.
 */
public final class PckFileParser {
    private static final String XML_VERSION_KEY = "XMLVERSION";

    private PckFileParser() {}

    public static PckArchive parse(byte[] bytes) throws IOException {
        ByteBuffer buf = ByteBuffer.wrap(bytes);

        buf.order(ByteOrder.BIG_ENDIAN);
        int pckType = buf.getInt(0);
        if (pckType > 0x00_F0_00_00 || pckType < 3) {
            buf.order(ByteOrder.LITTLE_ENDIAN);
            pckType = buf.getInt(0);
            if (pckType > 0x00_F0_00_00 || pckType < 3) {
                throw new IOException("Invalid PCK type: " + pckType);
            }
        }
        buf.position(4);

        String[] propertyLookup = readPropertyLookup(buf);
        
        int xmlVersion = 0;
        for (String prop : propertyLookup) {
            if (XML_VERSION_KEY.equals(prop)) {
                xmlVersion = readInt(buf, "xml version");
                break;
            }
        }

        List<AssetEntry> entries = readAssetEntries(buf);
        List<PckAsset> assets = readAssetContents(entries, propertyLookup, buf);

        return new PckArchive(pckType, xmlVersion, assets);
    }

    private static String[] readPropertyLookup(ByteBuffer buf) throws IOException {
        int count = readInt(buf, "property count");
        if (count < 0 || count > 1_000_000) {
            throw new IOException("Invalid property count: " + count);
        }

        String[] lookup = new String[count];
        for (int i = 0; i < count; i++) {
            int index = readInt(buf, "property index");
            if (index < 0 || index >= count) throw new IOException("Invalid property index: " + index);
            lookup[index] = readString(buf);
        }
        return lookup;
    }

    private static List<AssetEntry> readAssetEntries(ByteBuffer buf) throws IOException {
        int count = readInt(buf, "asset count");
        if (count < 0 || count > 2_000_000) {
            throw new IOException("Invalid asset count: " + count);
        }

        List<AssetEntry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int size = readInt(buf, "asset size");
            if (size < 0) throw new IOException("Invalid asset size: " + size);

            int type = readInt(buf, "asset type");
            String filename = readString(buf).replace('\\', '/');
            entries.add(new AssetEntry(filename, type, size));
        }
        return entries;
    }

    private static List<PckAsset> readAssetContents(List<AssetEntry> entries, String[] propertyLookup, ByteBuffer buf) throws IOException {
        List<PckAsset> assets = new ArrayList<>(entries.size());

        for (AssetEntry entry : entries) {
            int propCount = readInt(buf, "asset property count");
            if (propCount < 0 || propCount > 1_000_000) {
                throw new IOException("Invalid asset property count: " + propCount);
            }

            List<Map.Entry<String, String>> properties = new ArrayList<>(propCount);
            for (int i = 0; i < propCount; i++) {
                int keyIndex = readInt(buf, "property key index");
                if (keyIndex < 0 || keyIndex >= propertyLookup.length) {
                    throw new IOException("Invalid property key index: " + keyIndex);
                }
                
                properties.add(Map.entry(propertyLookup[keyIndex], readString(buf)));
            }

            byte[] data = readBytes(buf, entry.size(), "asset data");
            assets.add(new PckAsset(entry.filename(), entry.type(), List.copyOf(properties), data));
        }

        return assets;
    }

    private static String readString(ByteBuffer buf) throws IOException {
        int len = readInt(buf, "string length");
        if (len < 0 || len > 1_000_000) throw new IOException("Invalid string length: " + len);
        if (buf.remaining() < len * 2 + 4) throw new IOException("Unexpected EOF while reading string data");

        char[] chars = new char[len];
        for (int i = 0; i < len; i++) {
            chars[i] = buf.getChar();
        }
        
        buf.getInt();
        return new String(chars);
    }

    private static int readInt(ByteBuffer buf, String context) throws IOException {
        if (buf.remaining() < Integer.BYTES) {
            throw new IOException("Unexpected EOF while reading " + context);
        }
        return buf.getInt();
    }

    private static byte[] readBytes(ByteBuffer buf, int length, String context) throws IOException {
        if (length < 0 || buf.remaining() < length) {
            throw new IOException("Unexpected EOF while reading " + context + " (requested " + length + " bytes)");
        }
        byte[] out = new byte[length];
        buf.get(out);
        return out;
    }

    private record AssetEntry(String filename, int type, int size) {}

    public record PckArchive(int type, int xmlVersion, List<PckAsset> assets) {}

    public record PckAsset(String filename, int type, List<Map.Entry<String, String>> properties, byte[] data) {
        public String getFirstProperty(String... keys) {
            if (properties == null || keys == null) return null;
            for (String key : keys) {
                if (key == null) continue;
                for (Map.Entry<String, String> property : properties) {
                    if (key.equalsIgnoreCase(property.getKey())) return property.getValue();
                }
            }
            return null;
        }
    }
}
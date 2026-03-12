package com.brandonitaly.bedrockskins.pack;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class PckLocalizationSupport {
    private PckLocalizationSupport() {}

    static String cleanLocText(String in) {
        if (in == null) return null;
        int nul = in.indexOf('\0');
        return (nul >= 0 ? in.substring(0, nul) : in).trim();
    }

    static String findPackDisplayToken(Map<String, Map<String, String>> pckTranslations, String currentLang) {
        return getTranslationFromMap(pckTranslations, "ids_display_name", currentLang) != null ? "IDS_DISPLAY_NAME" : null;
    }

    static String deriveSkinThemeToken(PckFileParser.PckAsset asset, String skinDisplayToken, String skinKey, Map<String, Map<String, String>> pckTranslations, String currentLang) {
        String explicit = firstNonBlank(getFirstPropertyValue(asset, "THEMENAMEID"), getFirstPropertyValue(asset, "THEMENAME"));
        if (explicit != null && !explicit.isBlank()) return explicit;

        if (skinDisplayToken != null && !skinDisplayToken.isBlank()) {
            String normalized = skinDisplayToken.trim();
            if (normalized.toUpperCase(Locale.ROOT).endsWith("_DISPLAYNAME")) {
                String candidate = normalized.substring(0, normalized.length() - 12) + "_THEMENAME";
                if (getTranslationFromMap(pckTranslations, candidate.toLowerCase(Locale.ROOT), currentLang) != null) return candidate;
            }
        }

        if (skinKey != null && !skinKey.isBlank()) {
            String candidate = "IDS_" + skinKey.toUpperCase(Locale.ROOT) + "_THEMENAME";
            if (getTranslationFromMap(pckTranslations, candidate.toLowerCase(Locale.ROOT), currentLang) != null) return candidate;
        }

        return null;
    }

    static void copyLocalizedValueToTranslations(String sourceToken, String targetKey, String fallbackValue, Map<String, Map<String, String>> pckTranslations, Map<String, Map<String, String>> globalTranslations) {
        String sourceKey = cleanLocText(sourceToken);
        if (sourceKey != null) sourceKey = sourceKey.toLowerCase(Locale.ROOT);

        String target = targetKey == null ? null : targetKey.trim().toLowerCase(Locale.ROOT);
        if (target == null || target.isBlank()) return;

        boolean copiedAny = false;
        if (sourceKey != null && !sourceKey.isBlank()) {
            for (Map.Entry<String, Map<String, String>> langEntry : pckTranslations.entrySet()) {
                String value = langEntry.getValue().get(sourceKey);
                if (value != null && !value.isBlank()) {
                    globalTranslations.computeIfAbsent(langEntry.getKey(), k -> new HashMap<>()).put(target, value);
                    copiedAny = true;
                }
            }
        }

        if (!copiedAny && fallbackValue != null && !fallbackValue.isBlank()) {
            globalTranslations.computeIfAbsent("en_us", k -> new HashMap<>()).put(target, fallbackValue);
        }
    }

    static String resolvePckLocalizedToken(String token, Map<String, Map<String, String>> pckTranslations, String currentLang) {
        if (token == null) return null;
        String normalized = cleanLocText(token).toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) return null;

        String translated = getTranslationFromMap(pckTranslations, normalized, currentLang);
        return (translated != null && !translated.isBlank()) ? translated : token;
    }

    static String getTranslationFromMap(Map<String, Map<String, String>> source, String key, String currentLang) {
        if (source == null || source.isEmpty() || key == null) return null;

        String normalizedKey = cleanLocText(key).toLowerCase(Locale.ROOT);

        Map<String, String> currentLangMap = source.get(currentLang);
        if (currentLangMap != null && currentLangMap.containsKey(normalizedKey)) return currentLangMap.get(normalizedKey);

        Map<String, String> enMap = source.get("en_us");
        if (enMap != null && enMap.containsKey(normalizedKey)) return enMap.get(normalizedKey);

        for (Map<String, String> map : source.values()) {
            if (map.containsKey(normalizedKey)) return map.get(normalizedKey);
        }
        return null;
    }

    static Map<String, Map<String, String>> loadPckLocalisations(List<PckFileParser.PckAsset> allAssets) {
        Map<String, Map<String, String>> outTranslations = new HashMap<>();
        if (allAssets == null || allAssets.isEmpty()) return outTranslations;

        for (PckFileParser.PckAsset asset : allAssets) {
            if (!isLikelyPckLocAsset(asset)) continue;

            try {
                Map<String, Map<String, String>> parsed = parseLocFile(asset.data());
                for (Map.Entry<String, Map<String, String>> languageEntry : parsed.entrySet()) {
                    if (languageEntry.getValue().isEmpty()) continue;
                    outTranslations.computeIfAbsent(languageEntry.getKey(), k -> new HashMap<>()).putAll(languageEntry.getValue());
                }
            } catch (Exception ignored) {}
        }
        return outTranslations;
    }

    private static boolean isLikelyPckLocAsset(PckFileParser.PckAsset asset) {
        if (asset == null || asset.filename() == null || asset.data() == null || asset.data().length < 20) return false;
        String name = asset.filename().toLowerCase(Locale.ROOT);
        return name.endsWith(".loc");
    }

    private static Map<String, Map<String, String>> parseLocFile(byte[] data) {
        Map<String, Map<String, String>> out = new HashMap<>();
        if (data == null || data.length < 20) return out;

        LocReader r = new LocReader(data);
        int version = r.readBEInt();
        int languageCount = r.readBEInt();
        r.readBEInt();
        int keyCount = r.readU8();
        r.skipOptionalNul();

        if (version <= 0 || keyCount <= 0 || languageCount <= 0) return out;

        List<String> keys = new ArrayList<>(keyCount);
        for (int i = 0; i < keyCount && r.hasRemaining(); i++) {
            keys.add(r.readUtf8(r.readU8()).toLowerCase(Locale.ROOT));
            r.skipOptionalNul();
        }

        List<String> directoryLanguages = new ArrayList<>(languageCount);
        for (int i = 0; i < languageCount && r.hasRemaining(); i++) {
            directoryLanguages.add(normalizeLocLanguage(r.readUtf8(r.readU8())));
            r.skipOptionalNul();
            r.skip(4);
        }

        for (int i = 0; i < directoryLanguages.size() && r.hasRemaining(); i++) {
            if (r.remaining() < 8) break;

            int blockStart = r.position();
            if (r.peekBEInt() == 0x00000200) r.skip(4);
            r.skipOptionalNul();

            String blockLang = normalizeLocLanguage(r.readUtf8(r.readU8()));
            r.skipOptionalNul();

            int blockKeyCount = r.readBEU24();
            r.skipOptionalNul();
            int pairs = Math.min(blockKeyCount, keys.size());

            if (!looksLikeLocLanguage(blockLang) || pairs <= 0) {
                if (!resyncToNextLocBlock(r, blockStart)) break;
                continue;
            }

            Map<String, String> map = out.computeIfAbsent(blockLang, k -> new HashMap<>((int) (pairs / 0.75f) + 1));
            boolean corrupted = false;

            for (int k = 0; k < pairs && r.hasRemaining(); k++) {
                if (r.remaining() < 1) break;
                int valueLen = r.readU8();

                if (valueLen > r.remaining()) {
                    corrupted = true;
                    break;
                }

                String value = r.readUtf8(valueLen);
                r.skipOptionalNul();
                if (!value.isBlank()) map.put(keys.get(k), value);
            }

            if (corrupted && !resyncToNextLocBlock(r, blockStart)) break;
        }

        return out;
    }

    private static String normalizeLocLanguage(String raw) {
        if (raw == null || raw.isBlank()) return "en_us";
        String lang = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        return switch (lang) {
            case "en_en" -> "en_us";
            case "zh_hans" -> "zh_cn";
            case "zh_hant" -> "zh_tw";
            default -> lang;
        };
    }

    private static boolean looksLikeLocLanguage(String lang) {
        return lang != null && lang.toLowerCase(Locale.ROOT).matches("[a-z]{2}_[a-z0-9]{2,4}");
    }

    private static boolean resyncToNextLocBlock(LocReader r, int fromPos) {
        int next = r.findNextHeader(Math.max(fromPos + 1, r.position()), 0x00000200, 8192);
        if (next < 0) return false;
        r.setPosition(next + 4);
        return true;
    }

    private static String getFirstPropertyValue(PckFileParser.PckAsset asset, String... keys) {
        if (asset == null || asset.properties() == null || keys == null) return null;

        for (String key : keys) {
            if (key == null) continue;
            for (Map.Entry<String, String> property : asset.properties()) {
                if (key.equalsIgnoreCase(property.getKey())) return property.getValue();
            }
        }
        return null;
    }

    private static String firstNonBlank(String first, String fallback) {
        return first != null && !first.isBlank() ? first : (fallback == null ? "" : fallback);
    }

    private static final class LocReader {
        private final byte[] data;
        private int pos;

        LocReader(byte[] data) { this.data = data; this.pos = 0; }

        boolean hasRemaining() { return pos < data.length; }
        int remaining() { return data.length - pos; }
        int position() { return pos; }
        void setPosition(int newPos) { pos = Math.max(0, Math.min(data.length, newPos)); }

        int readU8() { return remaining() < 1 ? 0 : (data[pos++] & 0xFF); }

        int readBEU24() {
            if (remaining() < 3) { pos = data.length; return 0; }
            int v = ((data[pos] & 0xFF) << 16) | ((data[pos + 1] & 0xFF) << 8) | (data[pos + 2] & 0xFF);
            pos += 3;
            return v;
        }

        int readBEInt() {
            if (remaining() < 4) { pos = data.length; return 0; }
            int v = ((data[pos] & 0xFF) << 24) | ((data[pos + 1] & 0xFF) << 16) | ((data[pos + 2] & 0xFF) << 8) | (data[pos + 3] & 0xFF);
            pos += 4;
            return v;
        }

        int peekBEInt() {
            if (remaining() < 4) return 0;
            return ((data[pos] & 0xFF) << 24) | ((data[pos + 1] & 0xFF) << 16) | ((data[pos + 2] & 0xFF) << 8) | (data[pos + 3] & 0xFF);
        }

        String readUtf8(int len) {
            int n = Math.min(len, remaining());
            if (n <= 0) return "";

            int end = pos + n;
            int strLen = 0;
            while (pos + strLen < end && data[pos + strLen] != 0) strLen++;

            String s = new String(data, pos, strLen, StandardCharsets.UTF_8);
            pos = end;
            return s;
        }

        void skip(int bytes) { pos = Math.min(data.length, pos + Math.max(bytes, 0)); }
        void skipOptionalNul() { if (remaining() > 0 && data[pos] == 0) pos++; }

        int findNextHeader(int start, int header, int maxScan) {
            int end = Math.min(data.length - 3, start + Math.max(0, maxScan));
            byte b0 = (byte)(header >>> 24), b1 = (byte)(header >>> 16), b2 = (byte)(header >>> 8), b3 = (byte)header;

            for (int i = Math.max(0, start); i <= end; i++) {
                if (data[i] == b0 && data[i + 1] == b1 && data[i + 2] == b2 && data[i + 3] == b3) return i;
            }
            return -1;
        }
    }
}
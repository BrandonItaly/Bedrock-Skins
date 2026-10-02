package io.github.brandonitaly.bedrockskins.pack;

import java.util.Map;

/** Looks up a normalized key in the selected language, English, then any available language. */
public final class TranslationLookup {
    private TranslationLookup() {}

    public static String find(Map<String, Map<String, String>> translations, String key, String language) {
        String value = inLanguage(translations, key, language);
        if (value != null) return value;
        value = inLanguage(translations, key, "en_us");
        if (value != null) return value;
        for (Map<String, String> entries : translations.values()) {
            value = entries.get(key);
            if (value != null) return value;
        }
        return null;
    }

    private static String inLanguage(Map<String, Map<String, String>> translations, String key, String language) {
        Map<String, String> entries = translations.get(language);
        return entries == null ? null : entries.get(key);
    }
}

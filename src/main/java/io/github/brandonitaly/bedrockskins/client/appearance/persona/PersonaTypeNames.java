package io.github.brandonitaly.bedrockskins.client.appearance.persona;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

/** Localized display names and GUI grouping for Bedrock Persona piece types. */
public final class PersonaTypeNames {
    public static final String ALL = "all";
    public static final String EQUIPPED = "equipped";
    public static final String BOTTOMS = "bottoms";
    public static final String OUTERWEAR = "persona_outerwear";

    private PersonaTypeNames() {}

    public static String category(String pieceType) {
        if ("persona_bottom".equals(pieceType) || "persona_high_pants".equals(pieceType)) return BOTTOMS;
        if ("persona_hood".equals(pieceType)) return OUTERWEAR;
        return pieceType;
    }

    public static boolean belongsTo(String pieceType, String category) {
        return ALL.equals(category) || category.equals(category(pieceType));
    }

    public static Component displayName(String pieceTypeOrCategory) {
        if (pieceTypeOrCategory == null || pieceTypeOrCategory.isBlank()) {
            return Component.translatable("bedrockskins.gui.cosmetics");
        }
        String key = "bedrockskins.persona.type." + pieceTypeOrCategory;
        if (Language.getInstance().has(key)) return Component.translatable(key);

        String name = pieceTypeOrCategory.startsWith("persona_")
            ? pieceTypeOrCategory.substring("persona_".length())
            : pieceTypeOrCategory;
        name = name.replace('_', ' ');
        return Component.literal(Character.toUpperCase(name.charAt(0)) + name.substring(1));
    }
}

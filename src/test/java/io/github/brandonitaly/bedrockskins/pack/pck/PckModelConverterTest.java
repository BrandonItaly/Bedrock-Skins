package io.github.brandonitaly.bedrockskins.pack.pck;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class PckModelConverterTest {
    @Test void animationAndGameFlagsAcceptTheSameUnsignedFormats() {
        Map<String, Long> values = Map.of(
            "0", 0L,
            " 42 ", 42L,
            "+42", 42L,
            "0x2a", 42L,
            "0X2A", 42L,
            "18446744073709551615", -1L,
            "0xffffffffffffffff", -1L);
        values.forEach((value, expected) -> {
            assertEquals(expected, PckModelConverter.parseAnimMask(asset("ANIM", value)), value);
            assertEquals(expected, PckModelConverter.parseGameFlags(asset("GAME_FLAGS", value)), value);
            assertEquals(expected, PckModelConverter.parseGameFlags(asset("GAMEFLAGS", value)), value);
        });
    }

    @Test void missingBlankInvalidAndOverflowingFlagsRemainAbsent() {
        var empty = new PckFileParser.PckAsset("skin.png", 0, List.of(), new byte[0]);
        assertNull(PckModelConverter.parseAnimMask(empty));
        assertNull(PckModelConverter.parseGameFlags(empty));
        for (String value : List.of("", " \t", "-1", "0x", "invalid", "18446744073709551616", "0x10000000000000000")) {
            assertNull(PckModelConverter.parseAnimMask(asset("ANIM", value)), value);
            assertNull(PckModelConverter.parseGameFlags(asset("GAME_FLAGS", value)), value);
        }
    }

    @Test void preferredGameFlagsPropertyStillWinsOverAlias() {
        var asset = new PckFileParser.PckAsset("skin.png", 0,
            List.of(Map.entry("GAMEFLAGS", "2"), Map.entry("game_flags", "1")), new byte[0]);
        assertEquals(Long.valueOf(1), PckModelConverter.parseGameFlags(asset));
        var invalidPreferred = new PckFileParser.PckAsset("skin.png", 0,
            List.of(Map.entry("GAMEFLAGS", "2"), Map.entry("GAME_FLAGS", "invalid")), new byte[0]);
        assertNull(PckModelConverter.parseGameFlags(invalidPreferred));
    }

    private static PckFileParser.PckAsset asset(String key, String value) {
        return new PckFileParser.PckAsset("skin.png", 0, List.of(Map.entry(key, value)), new byte[0]);
    }
}

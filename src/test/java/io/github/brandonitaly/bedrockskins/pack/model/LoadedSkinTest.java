package io.github.brandonitaly.bedrockskins.pack.model;

import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LoadedSkinTest {
    @Test void remoteGeometryLookupUsesNetworkIdentityRatherThanDisplayNames() {
        var networkId = SkinId.parse("geyser:persona:original");
        var geometry = new JsonObject();
        var skin = new LoadedSkin(networkId, "Remote", "Remote", networkId.toString(),
            geometry, AssetSource.Remote.INSTANCE, null, false);
        var registry = java.util.Map.of(networkId, skin);
        assertSame(skin, registry.get(skin.skinId));
        assertSame(geometry, registry.get(skin.skinId).geometryData);
        // Remote entries stay excluded from the local wardrobe pack list.
        assertEquals("skinpack.Remote", skin.packId);
    }
    @Test void returningToReleasedRemoteSkinRequiresTextureDownloadDespiteMatchingHash() {
        var skin = new LoadedSkin("geyser", "Remote", "persona", new JsonObject(), AssetSource.Remote.INSTANCE);
        skin.hash = "original";
        skin.identifier = Identifier.fromNamespaceAndPath("bedrockskins", "test/persona");
        assertTrue(skin.hasAvailableContent("original"));
        assertFalse(skin.hasAvailableContent("different"));
        // Switching to B releases A's texture but retains its geometry and content hash.
        skin.identifier = null;
        assertFalse(skin.hasAvailableContent("original"));
        skin.identifier = Identifier.fromNamespaceAndPath("bedrockskins", "test/persona");
        assertTrue(skin.hasAvailableContent("original"));
    }
    @Test void missingRemoteCapeAlsoRequiresDownload() {
        var skin = new LoadedSkin("geyser", "Remote", "persona", new JsonObject(), AssetSource.Remote.INSTANCE, AssetSource.Remote.INSTANCE);
        skin.hash = "original";
        skin.identifier = Identifier.fromNamespaceAndPath("bedrockskins", "test/persona");
        assertFalse(skin.hasAvailableContent("original"));
        skin.capeIdentifier = Identifier.fromNamespaceAndPath("bedrockskins", "test/cape");
        assertTrue(skin.hasAvailableContent("original"));
    }
}

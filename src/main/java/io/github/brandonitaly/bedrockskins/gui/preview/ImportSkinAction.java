package io.github.brandonitaly.bedrockskins.gui.preview;

import io.github.brandonitaly.bedrockskins.pack.model.LoadedSkin;
import io.github.brandonitaly.bedrockskins.pack.model.SkinId;

/** Identifies the bundled dummy-texture card that launches the skin importer. */
public final class ImportSkinAction {
    public static final SkinId ID = SkinId.of("Imports", "Import Skin");

    private ImportSkinAction() {}

    public static boolean is(LoadedSkin skin) {
        return skin != null && ID.equals(skin.skinId);
    }
}

package io.github.brandonitaly.bedrockskins.client.appearance.persona;

import io.github.brandonitaly.bedrockskins.client.render.model.BedrockPlayerModel;
import io.github.brandonitaly.bedrockskins.pack.model.LoadedCosmetic;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Owns both model lookup indexes so eviction cannot leave cosmetic references behind. */
final class PersonaModelCache {
    private record Key(String cosmeticId, boolean slim) {}

    private final Map<Key, BedrockPlayerModel> models = new HashMap<>();
    private final Map<BedrockPlayerModel, LoadedCosmetic> cosmetics = new IdentityHashMap<>();

    synchronized BedrockPlayerModel getOrCreate(LoadedCosmetic cosmetic, boolean slim,
                                               Supplier<BedrockPlayerModel> factory) {
        Key key = new Key(cosmetic.id, slim);
        BedrockPlayerModel model = models.get(key);
        if (model == null) {
            model = factory.get();
            if (model != null) {
                models.put(key, model);
                cosmetics.put(model, cosmetic);
            }
        }
        return model;
    }

    synchronized LoadedCosmetic cosmetic(BedrockPlayerModel model) {
        return cosmetics.get(model);
    }

    synchronized void remove(String cosmeticId) {
        for (boolean slim : new boolean[] {false, true}) {
            BedrockPlayerModel model = models.remove(new Key(cosmeticId, slim));
            if (model != null) cosmetics.remove(model);
        }
    }

    synchronized void clear() {
        models.clear();
        cosmetics.clear();
    }
}

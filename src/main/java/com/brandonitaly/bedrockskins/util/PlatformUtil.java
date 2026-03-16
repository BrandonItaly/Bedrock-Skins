package com.brandonitaly.bedrockskins.util;

//? if fabric {
import net.fabricmc.loader.api.FabricLoader;
//?}

public class PlatformUtil {
    public static boolean isModLoaded(String modId) {
        //? if fabric {
        return FabricLoader.getInstance().isModLoaded(modId);
        //?} else if neoforge {
        /*return net.neoforged.fml.ModList.get().isLoaded(modId);*/
        //?} else {
        /*return false;*/
        //?}
    }
}
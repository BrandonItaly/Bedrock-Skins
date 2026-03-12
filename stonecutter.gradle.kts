plugins {
    id("dev.kikugie.stonecutter")
    id("dev.architectury.loom") version "1.13.+" apply false
}

stonecutter active "1.21.11-fabric" /* [SC] DO NOT EDIT */

stonecutter parameters {
    val loader = node.project.property("loom.platform").toString()
    constants.match(loader, "fabric", "neoforge")

    replacements {
        string(stonecutter.eval(current.version, ">=1.21.11")) {
            replace("ResourceLocation", "Identifier")
            replace("ResourceKey::location", "ResourceKey::identifier")
            replace("net.minecraft.Util", "net.minecraft.util.Util")
            replace("net.minecraft.client.model.PlayerModel", "net.minecraft.client.model.player.PlayerModel")
        }
    }
}

plugins {
    id("dev.kikugie.stonecutter")
    id("net.fabricmc.fabric-loom") version "1.15-SNAPSHOT" apply false
    id("net.neoforged.moddev") version "2.0.140" apply false
}

stonecutter active "26.1-fabric" /* [SC] DO NOT EDIT */

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

        string(eval(current.version, ">=26.0")) {
            replace("accessWidener v2 named", "accessWidener v2 official")
            replace("keybinding.v1.KeyBindingHelper", "keymapping.v1.KeyMappingHelper")
            replace("KeyBindingHelper", "KeyMappingHelper")
            replace(".registerKeyBinding", ".registerKeyMapping")
            replace(".playS2C()", ".clientboundPlay()")
            replace(".playC2S()", ".serverboundPlay()")            
            replace("state.CameraRenderState", "state.level.CameraRenderState")
            replace("DimensionType.CardinalLightType", "net.minecraft.world.level.CardinalLighting.Type")
            replace("GuiGraphics", "GuiGraphicsExtractor")
            replace(".drawString(", ".text(")
            replace(".drawCenteredString(", ".centeredText(")
            replace("renderContent", "extractContent")
            replace("renderSelection", "extractSelection")
            replace("renderWidget", "extractWidgetRenderState")
            replace(".renderTooltipBackground", ".extractTooltipBackground")
            replace(".renderMenuBackground", ".extractMenuBackground")
            replace("void render(", "void extractRenderState(")
            replace(".render(gui", ".extractRenderState(gui")
        }
    }
}

plugins {
    id("dev.kikugie.stonecutter")
    id("net.fabricmc.fabric-loom") version "1.15-SNAPSHOT" apply false
    id("net.neoforged.moddev") version "2.0.140" apply false
}

stonecutter active "26.2-fabric" /* [SC] DO NOT EDIT */

stonecutter parameters {
    val loader = node.project.property("loom.platform").toString()
    constants.match(loader, "fabric", "neoforge")

    val legacy4jVersion = node.project.findProperty("legacy4j_version")
        ?.toString()
        ?.takeUnless { it.isBlank() || it == "[VERSIONED]" }
    val legacy4jFlag = if (legacy4jVersion != null) "legacy4j" else ""
    constants.match(legacy4jFlag, "legacy4j")

    val modmenuVersion = node.project.findProperty("modmenu_version")
        ?.toString()
        ?.takeUnless { it.isBlank() || it == "[VERSIONED]" }
    val modmenuFlag = if (modmenuVersion != null) "modmenu" else ""
    constants.match(modmenuFlag, "modmenu")

    replacements {
        string(stonecutter.eval(current.version, ">=1.21.11")) {
            replace("ResourceLocation", "Identifier")
            replace("ResourceKey::location", "ResourceKey::identifier")
            replace("net.minecraft.Util", "net.minecraft.util.Util")
            replace("net.minecraft.client.model.PlayerModel", "net.minecraft.client.model.player.PlayerModel")
        }

        string(eval(current.version, ">=26.1")) {
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

        string(eval(current.version, ">=26.2")) {
            replace(".setScreen(", ".gui.setScreen(")
            replace("::setScreen", ".gui::setScreen")
            replace("client.screen)", "client.gui.screen())")
            replace("client.screen instanceof", "client.gui.screen() instanceof")
            replace("this.minecraft.screen", "this.minecraft.gui.screen()")
        }

        string(eval(current.version, ">=26.3-fabric")) {
            replace("InputConstants.Type.KEYSYM", "InputConstants.Type.KEYBOARD")
            replace("GLFW.GLFW_", "InputConstants.")
        }
    }
}

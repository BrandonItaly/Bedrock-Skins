plugins {
    id("dev.kikugie.stonecutter")
    id("net.fabricmc.fabric-loom") version "1.15-SNAPSHOT" apply false
    id("net.neoforged.moddev") version "2.0.147" apply false
}

apply(from = "gradle/universal.gradle")

stonecutter active "26.2-fabric" /* [SC] DO NOT EDIT */

stonecutter parameters {
    val loader = node.project.property("loom.platform").toString()
    constants.match(loader, "fabric", "neoforge")

    for (mod in listOf("legacy4j", "modmenu")) {
        val version = node.project.findProperty("${mod}_version")?.toString()
        val enabled = !version.isNullOrBlank() && version != "[VERSIONED]"
        constants.match(if (enabled) mod else "", mod)
    }

    replacements {
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
            replace("net.minecraft.client.gui.components.PlayerFaceRenderer", "net.minecraft.client.gui.components.PlayerFaceExtractor")
            replace("@Mixin(PlayerFaceRenderer.class)", "@Mixin(PlayerFaceExtractor.class)")
            replace("PlayerFaceRenderer.draw(", "PlayerFaceExtractor.extractRenderState(")
            replace("draw(Lnet/minecraft/client/gui/", "extractRenderState(Lnet/minecraft/client/gui/")
            replace("armor::renderType", "armor.renderType()")
            replace("renderContent", "extractContent")
            replace("renderSelection", "extractSelection")
            replace("renderScrollbar", "extractScrollbar")
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
            replace("client.screen,", "client.gui.screen(),")
            replace("client.screen instanceof", "client.gui.screen() instanceof")
            replace("this.minecraft.screen", "this.minecraft.gui.screen()")
        }

        string(eval(current.version, ">26.2")) {
            replace("InputConstants.Type.KEYSYM", "InputConstants.Type.KEYBOARD")
            replace("GLFW.GLFW_KEY", "InputConstants.KEY")
        }
    }
}

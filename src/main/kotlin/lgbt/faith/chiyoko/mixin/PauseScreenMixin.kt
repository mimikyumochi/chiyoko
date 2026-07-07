package lgbt.faith.chiyoko.mixin

import lgbt.faith.chiyoko.Chiyoko
import lgbt.faith.chiyoko.gui.ChiyokoConfigScreen
import lgbt.faith.chiyoko.gui.ChiyokoRenderer
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.Button.builder
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.client.gui.screens.PauseScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Invoker
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(Screen::class)
interface ScreenAccessor {
    @Invoker("addRenderableWidget")
    fun <T> callAddRenderableWidget(widget: T): T
            where T : GuiEventListener, T : Renderable, T : NarratableEntry
}

@Mixin(PauseScreen::class)
abstract class PauseScreenMixin {

    @Inject(
        method = ["extractRenderState"],
        at = [At("HEAD")]
    )
    private fun dropseed(
        graphics: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        a: Float,
        ci: CallbackInfo
    ) {
        ChiyokoRenderer().render(graphics)

        val text = Component.literal("chiyoko")
        val width = 60
        val height = 20
        val x = graphics.guiWidth() - width - 5
        val y = graphics.guiHeight() - height - 5

        val button = builder(text) {
            /*? if >=26.2 {*/
            /*Minecraft.getInstance().gui.setScreen(ChiyokoConfigScreen())
            *//*?} else {*/
            Minecraft.getInstance().setScreen(ChiyokoConfigScreen())
            /*?}*/
        }.bounds(x, y, width, height).build()

        (this as ScreenAccessor).callAddRenderableWidget(button)
    }
}
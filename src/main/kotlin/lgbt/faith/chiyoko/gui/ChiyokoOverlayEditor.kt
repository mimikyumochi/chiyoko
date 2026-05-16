package lgbt.faith.chiyoko.gui

import lgbt.faith.chiyoko.Chiyoko
import lgbt.faith.chiyoko.config.ChiyokoConfigManager
import lgbt.faith.chiyoko.config.OverlayConfig
import lgbt.faith.chiyoko.config.OverlayRotation
import lgbt.faith.chiyoko.sequences.Fishing
import lgbt.faith.chiyoko.sequences.Gravel
import lgbt.faith.chiyoko.sequences.PiglinBartering
import lgbt.faith.chiyoko.sequences.Vault
import lgbt.faith.chiyoko.sequences.WitherSkeleton
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.ContainerObjectSelectionList
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent


class ChiyokoOverlayEditor : Screen(Component.literal("chiyoko overlay editor")) {

    private val configManager = Chiyoko.configManager
    private val worldName = Chiyoko.worldName

    private lateinit var list: OverlayList

    override fun init() {
        list = OverlayList(minecraft, width, height-60, 20, 25)
        val sequences = configManager.config.worlds[worldName]?.sequences ?: emptyMap()

        sequences.forEach { (key, data) ->
            val overlay = configManager.config.getOverlay(key)
            val sequenceType = Chiyoko.sequences.map[key]

            list.addEntry(OverlayList.CategoryEntry(key, list))
            list.addEntry(OverlayList.VisibleEntry(key, overlay, configManager, worldName))
            list.addEntry(OverlayList.RotationEntry(key, overlay, configManager, worldName))
            list.addEntry(OverlayList.ReversedEntry(key, overlay, configManager, worldName))

            if (sequenceType is WitherSkeleton) {
                list.addEntry(OverlayList.RollTypeEntry(key, configManager.config.getOverlay(key), configManager, worldName))
            }
            if (sequenceType is Fishing || sequenceType is PiglinBartering || sequenceType is Gravel || sequenceType is Vault) {
                list.addEntry(OverlayList.AdvancesEntry(key, configManager.config.getOverlay(key), configManager, worldName, font, list))
            }
        }
        addRenderableWidget(list)
        addRenderableWidget(Button.builder(Component.literal("done")) {
            configManager.save()
            this.minecraft.setScreen(ChiyokoConfigScreen())
        }.bounds(width / 2 - 100, height - 27, 200, 20).build())
    }

    override fun onClose() {
        configManager.save()
        this.minecraft.setScreen(null)
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        list.extractWidgetRenderState(graphics, mouseX, mouseY, a)
        super.extractRenderState(graphics, mouseX, mouseY, a)
    }
}
class OverlayList(mc: Minecraft, width: Int, height: Int, y0: Int, itemHeight: Int) : ContainerObjectSelectionList<OverlayList.Entry>(mc, width, height, y0, itemHeight) {
    abstract class Entry : ContainerObjectSelectionList.Entry<Entry>() {
    }

    public override fun addEntry(entry: Entry): Int {
        return super.addEntry(entry)
    }

    class CategoryEntry(private val key: String, private val list: OverlayList) : Entry() {
        private var _focused = false
        override fun extractContent(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, hovered: Boolean, a: Float) {
            val mc = Minecraft.getInstance()
            graphics.centeredText(mc.font, Component.literal(key.replace("minecraft:", "")).withStyle(ChatFormatting.YELLOW),
                contentXMiddle, contentYMiddle - mc.font.lineHeight / 2, 0xFFFFFFFF.toInt())
        }
        override fun children(): List<GuiEventListener> = emptyList()
        override fun setFocused(focused: Boolean) {_focused = focused}
        override fun isFocused(): Boolean {return _focused}
        override fun narratables(): List<NarratableEntry> = emptyList()
    }
    class VisibleEntry(val key: String, val overlay: OverlayConfig, val configManager: ChiyokoConfigManager, val worldName: String) : Entry() {
        private var _focused = false

        private val button = Button.builder(visibleLabel()) {
            overlay.visible = !overlay.visible
            it.message = visibleLabel()
            configManager.config.updateOverlay(key) { visible = overlay.visible }
        }.bounds(0, 0, 150, 20).build()

        override fun extractContent(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, hovered: Boolean, a: Float) {
            val mc = Minecraft.getInstance()
            graphics.text(mc.font, "visibility", contentX, contentYMiddle - mc.font.lineHeight / 2, 0xFFFFFFFF.toInt())
            button.setPosition(contentRight - 150, contentY)
            button.extractRenderState(graphics, mouseX, mouseY, a)
        }
        private fun visibleLabel(): Component {
            return if (overlay.visible) {
                Component.literal("shown").withStyle(ChatFormatting.GREEN)
            } else {
                Component.literal("hidden").withStyle(ChatFormatting.RED)
            }
        }

        override fun children(): List<GuiEventListener> = listOf(button)
        override fun narratables(): List<NarratableEntry> = listOf(button)

        override fun setFocused(focused: Boolean) {_focused = focused}
        override fun isFocused(): Boolean {return _focused}
    }

    class RotationEntry(val key: String, val overlay: OverlayConfig, val configManager: ChiyokoConfigManager, val worldName: String) : Entry() {
        private var _focused = false

        private val button = Button.builder(rotationLabel()) {
            overlay.rotation = when (overlay.rotation) {
                OverlayRotation.HORIZONTAL -> OverlayRotation.VERTICAL
                OverlayRotation.VERTICAL -> OverlayRotation.HORIZONTAL
            }
            it.message = rotationLabel()
            configManager.config.updateOverlay(key) { rotation = overlay.rotation }
        }.bounds(0, 0, 150, 20).build()

        override fun extractContent(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, hovered: Boolean, a: Float) {
            val mc = Minecraft.getInstance()
            graphics.text(mc.font, "rotation", contentX, contentYMiddle - mc.font.lineHeight / 2, 0xFFFFFFFF.toInt())
            button.setPosition(contentRight - 150, contentY)
            button.extractRenderState(graphics, mouseX, mouseY, a)
        }
        private fun rotationLabel(): MutableComponent {
            return when (overlay.rotation) {
                OverlayRotation.VERTICAL -> Component.literal("vertical ↑")
                OverlayRotation.HORIZONTAL -> Component.literal("horizontal →")
            }
        }

        override fun children(): List<GuiEventListener> = listOf(button)
        override fun narratables(): List<NarratableEntry> = listOf(button)

        override fun setFocused(focused: Boolean) {_focused = focused}
        override fun isFocused(): Boolean {return _focused}
    }
    class ReversedEntry(val key: String, val overlay: OverlayConfig, val configManager: ChiyokoConfigManager, val worldName: String) : Entry() {
        private var _focused = false

        private val button = Button.builder(reversedLabel()) {
            overlay.reversed = !overlay.reversed
            it.message = reversedLabel()
            configManager.config.updateOverlay(key) { reversed = overlay.reversed }
        }.bounds(0, 0, 150, 20).build()

        override fun extractContent(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, hovered: Boolean, a: Float) {
            val mc = Minecraft.getInstance()
            graphics.text(mc.font, "reversed", contentX, contentYMiddle - mc.font.lineHeight / 2, 0xFFFFFFFF.toInt())
            button.setPosition(contentRight - 150, contentY)
            button.extractRenderState(graphics, mouseX, mouseY, a)
        }
        private fun reversedLabel(): Component {
            return if (overlay.reversed) {
                Component.literal("true").withStyle(ChatFormatting.GREEN)
            } else {
                Component.literal("false").withStyle(ChatFormatting.RED)
            }
        }

        override fun children(): List<GuiEventListener> = listOf(button)
        override fun narratables(): List<NarratableEntry> = listOf(button)

        override fun setFocused(focused: Boolean) {_focused = focused}
        override fun isFocused(): Boolean {return _focused}
    }
    class RollTypeEntry(val key: String, val overlay: OverlayConfig, val configManager: ChiyokoConfigManager, val worldName: String) : Entry() {
        private var _focused = false

        private val button = Button.builder(rollTypeLabel()) {
            overlay.rollType = when (overlay.rollType) {
                WitherSkeleton.RollType.NextDrop -> WitherSkeleton.RollType.KillsUntilSkull
                WitherSkeleton.RollType.KillsUntilSkull -> WitherSkeleton.RollType.NextDrop
            }
            it.message = rollTypeLabel()
            configManager.config.updateOverlay(key) { rollType = overlay.rollType }
        }.bounds(0, 0, 150, 20).build()

        override fun extractContent(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, hovered: Boolean, a: Float) {
            val mc = Minecraft.getInstance()
            graphics.text(mc.font, "roll type", contentX, contentYMiddle - mc.font.lineHeight / 2, 0xFFFFFFFF.toInt())
            button.setPosition(contentRight - 150, contentY)
            button.extractRenderState(graphics, mouseX, mouseY, a)
        }
        private fun rollTypeLabel() = Component.literal(overlay.rollType.name.replace(Regex("([a-z])([A-Z])"), "$1 $2").lowercase())

        override fun children(): List<GuiEventListener> = listOf(button)
        override fun narratables(): List<NarratableEntry> = listOf(button)

        override fun setFocused(focused: Boolean) {_focused = focused}
        override fun isFocused(): Boolean {return _focused}
    }
    class AdvancesEntry(
        private val key: String,
        private val overlay: OverlayConfig,
        private val configManager: ChiyokoConfigManager,
        private val worldName: String,
        private val font: Font,
        private val list: OverlayList
    ) : Entry() {
        private var _focused = false
        private val editBox = EditBox(font, 0, 0, 150, 20, Component.literal("advances")).also {
            it.value = overlay.advances.toString()
            it.setResponder { s ->
                if (s.isNotEmpty() && !s.matches(Regex("-?\\d*"))) {
                    it.value = s.replace(Regex("[^0-9-]"), "").toInt().coerceAtLeast(1).toString()
                }

                val v = s.toIntOrNull() ?: return@setResponder
                overlay.advances = v
                configManager.config.updateOverlay(key) { advances = v }
            }
        }
        override fun extractContent(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, hovered: Boolean, a: Float) {
            val mc = Minecraft.getInstance()
            graphics.text(mc.font, "advances", contentX, contentYMiddle - mc.font.lineHeight / 2, 0xFFFFFFFF.toInt())
            editBox.setPosition(contentRight - 150, contentY)
            editBox.extractRenderState(graphics, mouseX, mouseY, a)
        }

        override fun children(): List<GuiEventListener> = listOf(editBox)
        override fun narratables(): List<NarratableEntry> = listOf(editBox)

        override fun setFocused(focused: Boolean) {_focused = focused}
        override fun isFocused(): Boolean {return _focused}
    }
}
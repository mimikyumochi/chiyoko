package lgbt.faith.chiyoko.gui

import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.ContainerObjectSelectionList
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import lgbt.faith.chiyoko.Chiyoko
import lgbt.faith.chiyoko.config.ChiyokoConfigManager
import lgbt.faith.chiyoko.config.OverlayConfig
import lgbt.faith.chiyoko.config.OverlayRotation
import lgbt.faith.chiyoko.config.RollType
import lgbt.faith.chiyoko.sequences.*
import kotlin.math.min

class ChiyokoOverlayEditor : Screen(Component.literal("chiyoko overlay editor")) {

    private val configManager = Chiyoko.configManager

    private lateinit var list: OverlayList
    private val tabButtons = mutableListOf<Pair<String, Button>>()
    var selectedKey: String? = null

    private val sequenceKeys: List<String>
        get() = Chiyoko.sequences.map.keys.filter { configManager.config.getOverlay(it).tracked == true }

    fun refreshUI() {
        val keys = sequenceKeys
        if (selectedKey !in keys) {
            selectedKey = keys.firstOrNull()
        }
        buildTabs(keys)
        buildList()
    }

    override fun init() {
        val keys = sequenceKeys
        selectedKey = selectedKey?.takeIf { it in keys } ?: keys.firstOrNull()

        buildTabs(keys)
        buildList()

        addRenderableWidget(Button.builder(Component.literal("done")) {
            configManager.save()
            /*? if >=26.2 {*/
            /*this.minecraft.gui.setScreen(ChiyokoConfigScreen())
            *//*?} else {*/
            this.minecraft.setScreen(ChiyokoConfigScreen())
            /*?}*/
        }.bounds(width / 2 - 100, height - 27, 200, 20).build())
    }

    private fun buildTabs(keys: List<String>) {
        tabButtons.forEach { (_, button) -> removeWidget(button) }
        tabButtons.clear()

        val untracked = Chiyoko.sequences.map.keys.filter { configManager.config.getOverlay(it).tracked != true }
        val showPlus = untracked.isNotEmpty()

        val tabSize = 20
        val spacing = 2
        val totalTabs = keys.size + if (showPlus) 1 else 0
        val totalWidth = totalTabs * tabSize + (totalTabs - 1).coerceAtLeast(0) * spacing
        var x = (width - totalWidth) / 2
        val y = 22

        keys.forEach { key ->
            val readableName = key.replace("minecraft:", "")
            val button = Button.builder(Component.empty()) {
                selectedKey = key
                buildList()
            }.bounds(x, y, tabSize, tabSize)
                .tooltip(Tooltip.create(Component.literal(readableName)))
                .build()

            tabButtons += key to button
            addRenderableWidget(button)
            x += tabSize + spacing
        }

        if (showPlus) {
            val plusButton = Button.builder(Component.literal("+")) {
                /*? if >=26.2 {*/
                /*this.minecraft.gui.setScreen(ChiyokoAddTrackerScreen(this))
                *//*?} else {*/
                this.minecraft.setScreen(ChiyokoAddTrackerScreen(this))
                /*?}*/
            }.bounds(x, y, tabSize, tabSize)
                .tooltip(Tooltip.create(Component.literal("add tracker")))
                .build()

            tabButtons += "+" to plusButton
            addRenderableWidget(plusButton)
        }
    }

    fun getItemForSequence(key: String): ItemStack {
        val sequenceType = Chiyoko.sequences.map[key]
        val item = when (sequenceType) {
            is Fishing -> Items.COD
            is WitherSkeleton -> Items.WITHER_SKELETON_SKULL
            is PiglinBartering -> Items.PIGLIN_HEAD
            is Shulker -> Items.SHULKER_SHELL
            is Gravel -> Items.FLINT
            is Vault -> {
                if (key.contains("ominous", ignoreCase = true)) {
                    Items.OMINOUS_TRIAL_KEY
                } else {
                    Items.TRIAL_KEY
                }
            }
            else -> Items.BARRIER
        }
        return ItemStack(item)
    }

    private fun buildList() {
        if (::list.isInitialized) {
            removeWidget(list)
        }

        list = OverlayList(minecraft, width, height - 80, 50, 25)
        val key = selectedKey

        if (key != null) {
            val overlay = configManager.config.getOverlay(key)
            val sequenceType = Chiyoko.sequences.map[key]

            list.addEntry(OverlayList.UntrackEntry(key, configManager, this))
            list.addEntry(OverlayList.VisibleEntry(key, overlay, configManager))
            list.addEntry(OverlayList.RotationEntry(key, overlay, configManager))
            list.addEntry(OverlayList.ReversedEntry(key, overlay, configManager))

            if (sequenceType is WitherSkeleton || sequenceType is Shulker) {
                list.addEntry(OverlayList.RollTypeEntry(key, overlay, configManager))
            }
            if (sequenceType is Fishing || sequenceType is PiglinBartering || sequenceType is Gravel || sequenceType is Vault) {
                list.addEntry(OverlayList.AdvancesEntry(key, overlay, configManager, font))
            }
            if (sequenceType is Vault) {
                list.addEntry(OverlayList.SplitEntry(key, overlay, configManager))
            }
        } else {
            list.addEntry(OverlayList.InfoEntry("no trackers active. click '+' above to add one!"))
        }

        addRenderableWidget(list)
    }

    override fun onClose() {
        configManager.save()
        /*? if >=26.2 {*/
        /*this.minecraft.gui.setScreen(null)
        *//*?} else {*/
        this.minecraft.setScreen(null)
        /*?}*/
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        list.extractWidgetRenderState(graphics, mouseX, mouseY, a)
        super.extractRenderState(graphics, mouseX, mouseY, a)

        tabButtons.forEach { (key, button) ->
            if (key != "+") {
                val stack = getItemForSequence(key)
                graphics.item(stack, button.x + 2, button.y + 2)
            }

            if (key == selectedKey && key != "+") {
                val indicator = Component.literal("●").withStyle(ChatFormatting.YELLOW)
                val textX = button.x + (button.width - font.width(indicator)) / 2
                graphics.text(font, indicator, textX + 1, button.y + button.height - 1, 0xFFFFFFFF.toInt())
            }
        }
    }
}

class ChiyokoAddTrackerScreen(private val parent: ChiyokoOverlayEditor) : Screen(Component.literal("add tracker selection")) {

    private val configManager = Chiyoko.configManager
    private val gridButtons = mutableListOf<Pair<String, Button>>()

    override fun init() {
        val untracked = Chiyoko.sequences.map.keys.filter { configManager.config.getOverlay(it).tracked != true }

        val buttonSize = 20
        val spacing = 4
        val maxColumns = 10

        val columns = min(untracked.size, maxColumns)
        val gridWidth = columns * buttonSize + (columns - 1).coerceAtLeast(0) * spacing
        val startX = (width - gridWidth) / 2
        val startY = height / 4

        untracked.forEachIndexed { index, key ->
            val col = index % maxColumns
            val row = index / maxColumns

            val x = startX + col * (buttonSize + spacing)
            val y = startY + row * (buttonSize + spacing)
            val readableName = key.replace("minecraft:", "")

            val btn = Button.builder(Component.empty()) {
                configManager.config.updateOverlay(key) { tracked = true }
                parent.selectedKey = key
                /*? if >=26.2 {*/
                /*this.minecraft.gui.setScreen(parent)
                *//*?} else {*/
                this.minecraft.setScreen(parent)
                /*?}*/
                parent.refreshUI()
            }.bounds(x, y, buttonSize, buttonSize)
                .tooltip(Tooltip.create(Component.literal("track $readableName")))
                .build()

            gridButtons += key to btn
            addRenderableWidget(btn)
        }

        addRenderableWidget(Button.builder(Component.literal("cancel")) {
            /*? if >=26.2 {*/
            /*this.minecraft.gui.setScreen(parent)
            *//*?} else {*/
            this.minecraft.setScreen(parent)
            /*?}*/
        }.bounds(width / 2 - 50, height - 35, 100, 20).build())
    }

    override fun onClose() {
        /*? if >=26.2 {*/
        /*this.minecraft.gui.setScreen(parent)
        *//*?} else {*/
        this.minecraft.setScreen(parent)
        /*?}*/
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, a)

        gridButtons.forEach { (key, button) ->
            val stack = parent.getItemForSequence(key)
            graphics.item(stack, button.x + 2, button.y + 2)
        }
    }
}

class OverlayList(mc: Minecraft, width: Int, height: Int, y0: Int, itemHeight: Int) : ContainerObjectSelectionList<OverlayList.Entry>(mc, width, height, y0, itemHeight) {
    abstract class Entry : ContainerObjectSelectionList.Entry<Entry>()

    public override fun addEntry(entry: Entry): Int {
        return super.addEntry(entry)
    }

    class UntrackEntry(val key: String, val configManager: ChiyokoConfigManager, val editor: ChiyokoOverlayEditor) : Entry() {
        private var _focused = false
        private val button = Button.builder(Component.literal("untrack").withStyle(ChatFormatting.RED)) {
            configManager.config.updateOverlay(key) { tracked = false }
            editor.selectedKey = null
            editor.refreshUI()
        }.bounds(0, 0, 150, 20).build()

        override fun extractContent(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, hovered: Boolean, a: Float) {
            val mc = Minecraft.getInstance()
            graphics.text(mc.font, "tracking", contentX, contentYMiddle - mc.font.lineHeight / 2, 0xFFFFFFFF.toInt())
            button.setPosition(contentRight - 150, contentY)
            button.extractRenderState(graphics, mouseX, mouseY, a)
        }
        override fun children(): List<GuiEventListener> = listOf(button)
        override fun narratables(): List<NarratableEntry> = listOf(button)
        override fun setFocused(focused: Boolean) {_focused = focused}
        override fun isFocused(): Boolean {return _focused}
    }

    class InfoEntry(val text: String) : Entry() {
        override fun extractContent(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, hovered: Boolean, a: Float) {
            val mc = Minecraft.getInstance()
            val textComponent = Component.literal(text).withStyle(ChatFormatting.GRAY)
            graphics.text(mc.font, textComponent, contentX + (contentWidth - mc.font.width(textComponent)) / 2, contentYMiddle - mc.font.lineHeight / 2, 0xFFFFFFFF.toInt())
        }
        override fun children(): List<GuiEventListener> = emptyList()
        override fun narratables(): List<NarratableEntry> = emptyList()
        override fun setFocused(focused: Boolean) {}
        override fun isFocused(): Boolean = false
    }

    class VisibleEntry(val key: String, val overlay: OverlayConfig, val configManager: ChiyokoConfigManager) : Entry() {
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

    class RotationEntry(val key: String, val overlay: OverlayConfig, val configManager: ChiyokoConfigManager) : Entry() {
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

    class ReversedEntry(val key: String, val overlay: OverlayConfig, val configManager: ChiyokoConfigManager) : Entry() {
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

    class RollTypeEntry(val key: String, val overlay: OverlayConfig, val configManager: ChiyokoConfigManager) : Entry() {
        private var _focused = false

        private val button = Button.builder(rollTypeLabel()) {
            overlay.rollType = when (overlay.rollType ?: RollType.KillsUntilItem) {
                RollType.NextDrop -> RollType.KillsUntilItem
                RollType.KillsUntilItem -> RollType.NextDrop
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
        private fun rollTypeLabel() = Component.literal((overlay.rollType ?: RollType.KillsUntilItem).name.replace(Regex("([a-z])([A-Z])"), "$1 $2").lowercase())

        override fun children(): List<GuiEventListener> = listOf(button)
        override fun narratables(): List<NarratableEntry> = listOf(button)

        override fun setFocused(focused: Boolean) {_focused = focused}
        override fun isFocused(): Boolean {return _focused}
    }

    class AdvancesEntry(
        private val key: String,
        private val overlay: OverlayConfig,
        private val configManager: ChiyokoConfigManager,
        font: Font
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

    class SplitEntry(val key: String, val overlay: OverlayConfig, val configManager: ChiyokoConfigManager) : Entry() {
        private var _focused = false

        private val button = Button.builder(splitLabel()) {
            overlay.split = !overlay.split
            it.message = splitLabel()
            configManager.config.updateOverlay(key) { split = overlay.split }
        }.bounds(0, 0, 150, 20).build()

        override fun extractContent(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, hovered: Boolean, a: Float) {
            val mc = Minecraft.getInstance()
            graphics.text(mc.font, "split", contentX, contentYMiddle - mc.font.lineHeight / 2, 0xFFFFFFFF.toInt())
            button.setPosition(contentRight - 150, contentY)
            button.extractRenderState(graphics, mouseX, mouseY, a)
        }
        private fun splitLabel(): Component {
            return if (overlay.split) {
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
}
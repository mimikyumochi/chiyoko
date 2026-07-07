package lgbt.faith.chiyoko.gui

import lgbt.faith.chiyoko.Chiyoko
import lgbt.faith.chiyoko.config.GridPosition
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component

class ChiyokoLayoutEditor : Screen(Component.literal("chiyoko layout editor")) {

    private var dragging: String? = null
    private var dragOffsetX: Int = 0
    private var dragOffsetY: Int = 0

    private val gridSize = 20
    private val border = 1

    private val gridCols get() = (width - border * 2) / gridSize
    private val gridRows get() = (height - border * 2) / gridSize

    private fun pixelToGrid(px: Double) = ((px - border) / gridSize).toInt()
    private fun gridToPixel(cell: Int) = (cell * gridSize) + border

    private val configManager = Chiyoko.configManager

    private lateinit var doneButton: Button
    private var buttonAtBottom = true

    override fun init() {
        doneButton = Button.builder(Component.literal("Done")) {
            configManager.save()
            /*? if >=26.2 {*/
            /*this.minecraft.gui.setScreen(ChiyokoConfigScreen())
            *//*?} else {*/
            this.minecraft.setScreen(ChiyokoConfigScreen())
            /*?}*/
        }.bounds(width / 2 - 100, height - 27, 200, 20).build()
        addRenderableWidget(doneButton)
        buttonAtBottom = true

    }
    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        ChiyokoRenderer().render(graphics)
        super.extractRenderState(graphics, mouseX, mouseY, a)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        val mouseGridX = pixelToGrid(event.x)
        val mouseGridY = pixelToGrid(event.y)

        for ((key, pos) in configManager.config.hudSlots) {
            if (mouseGridX == pos.gridX && mouseGridY == pos.gridY) {
                dragging = key
                dragOffsetX = pixelToGrid(event.x) - pos.gridX
                dragOffsetY = pixelToGrid(event.y) - pos.gridY
                return true
            }
        }

        return super.mouseClicked(event, doubleClick)
    }
    override fun mouseDragged(event: MouseButtonEvent, dx: Double, dy: Double): Boolean {
        dragging?.let { key ->
            configManager.config.hudSlots[key] = GridPosition(
                (pixelToGrid(event.x) - dragOffsetX).coerceIn(0, gridCols),
                (pixelToGrid(event.y) - dragOffsetY).coerceIn(0, gridRows)
            )

            updateButtonPosition()
        }
        return super.mouseDragged(event, dx, dy)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        dragging = null
        configManager.save()
        return super.mouseReleased(event)
    }

    private fun isAreaOccupied(startX: Int, startY: Int, endX: Int, endY: Int): Boolean {
        return configManager.config.hudSlots.values.any { pos ->
            pos.gridX in startX..endX && pos.gridY in startY..endY
        }
    }

    private fun updateButtonPosition() {
        val btnGridWidth = 10
        val startX = pixelToGrid((width/2-100).toDouble())
        val endX = startX + btnGridWidth

        var currentGridY = pixelToGrid((height - 27).toDouble())

        while (currentGridY > 0 && isAreaOccupied(startX, currentGridY, endX, currentGridY)) {
            currentGridY -= 1
        }
        val finalPixelY = gridToPixel(currentGridY)

        doneButton.setPosition(width / 2 - 100, finalPixelY)

    }

    override fun removed() {
        configManager.save()
        super.removed()
    }
}
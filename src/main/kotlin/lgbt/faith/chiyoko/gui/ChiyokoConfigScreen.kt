package lgbt.faith.chiyoko.gui

import lgbt.faith.chiyoko.Chiyoko
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.PauseScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

class ChiyokoConfigScreen : Screen(Component.literal("chiyoko config")) {

    override fun init() {
        val btnWidth = 40
        val btnHeight = 20
        val inputWidth = 130
        val gap = 10
        val centerX = this.width / 2
        val yPos = (this.height / 2) - 30


        // row 1: [seedInput (130px)] [save (50px)] - total 150px + (gap*2)
        val row1TotalWidth = inputWidth + (gap*2) + btnWidth
        val row1StartX = centerX - row1TotalWidth / 2

        val seedInput = EditBox(
            this.font,
            centerX - (inputWidth/2) - (gap*2),
            yPos,
            inputWidth,
            btnHeight,
            Component.literal("")
        )
        seedInput.setResponder { text ->
            if (text.isNotEmpty() && !text.matches(Regex("-?\\d*"))) {
                seedInput.value = text.replace(Regex("[^0-9-]"), "")
            }
        }
        seedInput.value = Chiyoko.seed.toString()
        seedInput.setMaxLength(20)
        this.addRenderableWidget(seedInput)

        this.addRenderableWidget(
            Button.builder(Component.literal("save")) {

                val s = seedInput.value
                Chiyoko.seed = s.toLong()
                Chiyoko.changeWorldSeed()

                /*? if >=26.2 {*/
                this.minecraft.gui.setScreen(null)
                /*?} else {*/
                /*this.minecraft.setScreen(null)
                *//*?}*/
            }
            .bounds(row1StartX + inputWidth+gap, yPos, btnWidth, btnHeight)
            .build()
        )

        // row 1: [edit layout (75px)] [edit layout (75px)] - total 150px + gap
        val editBtnWidth = btnWidth + 35
        val row2TotalWidth = editBtnWidth + gap + editBtnWidth
        val row2StartX = centerX - row2TotalWidth / 2
        val row2Y = yPos + btnHeight + gap

        this.addRenderableWidget(
            Button.builder(Component.literal("edit layout")) {
                /*? if >=26.2 {*/
                this.minecraft.gui.setScreen(ChiyokoLayoutEditor())
                /*?} else {*/
                /*this.minecraft.setScreen(ChiyokoLayoutEditor())
                *//*?}*/
            }
            .bounds(row2StartX, row2Y, editBtnWidth, btnHeight)
            .build()
        )
        this.addRenderableWidget(
            Button.builder(Component.literal("edit overlays")) {
                /*? if >=26.2 {*/
                this.minecraft.gui.setScreen(ChiyokoOverlayEditor())
                /*?} else {*/
                /*this.minecraft.setScreen(ChiyokoOverlayEditor())
                *//*?}*/
            }
            .bounds(row2StartX + editBtnWidth + gap, row2Y, editBtnWidth, btnHeight)
            .build()
        )
        // row 3 [close (75px)] - total 75px, centred
        val row3totalWidth = row2TotalWidth
        val row3StartX = centerX - (editBtnWidth/2)
        val row3Y = row2Y + btnHeight + gap

        this.addRenderableWidget(
            Button.builder(Component.literal("close")) {
                /*? if >=26.2 {*/
                this.minecraft.gui.setScreen(PauseScreen(true))
                /*?} else {*/
                /*this.minecraft.setScreen(PauseScreen(true))
                *//*?}*/
            }
                .bounds(row3StartX, row3Y, editBtnWidth, btnHeight)
                .build()
        )
    }
}
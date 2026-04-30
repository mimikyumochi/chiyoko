package lgbt.faith.chiyoko.gui

import lgbt.faith.chiyoko.Chiyoko
import lgbt.faith.chiyoko.config.OverlayRotation
import lgbt.faith.chiyoko.keys
import lgbt.faith.chiyoko.sequences.Fishing
import lgbt.faith.chiyoko.sequences.Gravel
import lgbt.faith.chiyoko.sequences.PiglinBartering
import lgbt.faith.chiyoko.sequences.WitherSkeleton
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.tags.BiomeTags
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.enchantment.Enchantments

class ChiyokoRenderer {
    val mc = Minecraft.getInstance()
    val registries = mc.player?.level()?.registryAccess()!!
    val enchantLookup = registries.lookupOrThrow(Registries.ENCHANTMENT)
    val lootingHolder = enchantLookup.getOrThrow(Enchantments.LOOTING)
    val fortuneHolder = enchantLookup.getOrThrow(Enchantments.FORTUNE)
    val SLOT_SPRITE = Identifier.parse("minecraft:container/slot")
    val font = mc.font

    private fun gridToPixel(cell: Int) = (cell * gridSize) + border

    private val gridSize = 20
    private val border = 2

    fun render(graphics: GuiGraphicsExtractor) {
        if (!Chiyoko.loaded) return

        val mouseX = mc.mouseHandler.xpos() * mc.window.guiScaledWidth / mc.window.screenWidth
        val mouseY = mc.mouseHandler.ypos() * mc.window.guiScaledHeight / mc.window.screenHeight

        val mx = mouseX.toInt()
        val my = mouseY.toInt()

        val rod = mc.player?.fishing
        val rodPos = rod?.blockPosition()
        val playerPos = mc.player?.blockPosition()
        val luck = (mc.player?.luck ?: 0.0f).toInt()

        val isOpenWater = rod?.isOpenWaterFishing ?: true
        val level = mc.level
        val isJungle =
            if (rodPos != null && level != null) level.getBiome(rodPos).`is`(BiomeTags.IS_JUNGLE)
            else if (playerPos != null && level != null) level.getBiome(playerPos).`is`(BiomeTags.IS_JUNGLE)
            else false


        val mainhand = mc.player?.getItemInHand(InteractionHand.MAIN_HAND) ?: return
        val offhand = mc.player?.getItemInHand(InteractionHand.OFF_HAND) ?: return

        val lootingLevel = listOf(mainhand, offhand).maxOf { stack -> EnchantmentHelper.getItemEnchantmentLevel(lootingHolder, stack) }
        val fortuneLevel = listOf(mainhand, offhand).maxOf { stack -> EnchantmentHelper.getItemEnchantmentLevel(fortuneHolder, stack) }

        val configManager = Chiyoko.configManager

        keys.forEachIndexed { index, key ->
            val sequence = Chiyoko.sequences.map[key] ?: return@forEachIndexed
            val overlay = configManager.config.getOverlay(key)

            if (!overlay.visible) return@forEachIndexed

            val pos = configManager.config.getSlotPosition(key, index)

            val x = gridToPixel(pos.gridX)
            val y = gridToPixel(pos.gridY)

            val itemList = when (sequence) {
                is PiglinBartering -> sequence.roll(overlay.advances)
                is WitherSkeleton -> {
                    val drops = sequence.roll(overlay.rollType, true, lootingLevel)
                    drops.ifEmpty { listOf(ItemStack.EMPTY) }
                }
                is Fishing -> sequence.roll(overlay.advances, luck, isOpenWater, isJungle)
                is Gravel -> sequence.roll(overlay.advances, fortuneLevel)
                else -> emptyList()
            }

            val vector = when {
                overlay.rotation == OverlayRotation.HORIZONTAL && overlay.reversed  -> intArrayOf(-1, 0)
                overlay.rotation == OverlayRotation.HORIZONTAL                      -> intArrayOf(1, 0)
                overlay.reversed                                                    -> intArrayOf(0, -1)
                else                                                                -> intArrayOf(0, 1)
            }
            for ((index, item) in itemList.withIndex()) {
                val step = (gridSize - 2) * index
                val itemX = x + step * vector[0]
                val itemY = y + step * vector[1]

                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, itemX, itemY, gridSize-2, gridSize-2)
                graphics.item(item, itemX+1, itemY+1)
                graphics.itemDecorations(font, item, itemX+1, itemY+1)
                val hovered = mx in itemX until (itemX + gridSize) && my in itemY until (itemY + gridSize)
                if (hovered) {
                    graphics.setTooltipForNextFrame(
                        mc.font,
                        item,
                        mx,
                        my
                    )
                }
            }
        }
    }
}
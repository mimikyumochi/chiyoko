package lgbt.faith.chiyoko.gui

import lgbt.faith.chiyoko.Chiyoko
import lgbt.faith.chiyoko.config.OverlayRotation
import lgbt.faith.chiyoko.keys
import lgbt.faith.chiyoko.sequences.*
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
    data class SubList(val xOffset: Int, val yOffset: Int, val items: List<ItemStack>)

    val mc = Minecraft.getInstance()

    val SLOT_SPRITE = Identifier.parse("minecraft:container/slot")
    val font = mc.font

    private fun gridToPixel(cell: Int) = (cell * gridSize) + border

    private val gridSize = 20
    private val border = 1

    fun render(graphics: GuiGraphicsExtractor) {
        if (!Chiyoko.loaded) return
        if (mc.options.hideGui) return

        val mc = Minecraft.getInstance()

        val player = mc.player ?: return  // bail early if player is null
        val level = mc.level ?: return

        val registries = player.level().registryAccess()
        val enchantLookup = registries.lookupOrThrow(Registries.ENCHANTMENT)
        val lootingHolder = enchantLookup.getOrThrow(Enchantments.LOOTING)
        val fortuneHolder = enchantLookup.getOrThrow(Enchantments.FORTUNE)


        val mouseX = mc.mouseHandler.xpos() * mc.window.guiScaledWidth / mc.window.screenWidth
        val mouseY = mc.mouseHandler.ypos() * mc.window.guiScaledHeight / mc.window.screenHeight

        val mx = mouseX.toInt()
        val my = mouseY.toInt()

        val rod = mc.player?.fishing
        val rodPos = rod?.blockPosition()
        val playerPos = mc.player?.blockPosition()
        val luck = (mc.player?.luck ?: 0.0f).toInt()

        val isOpenWater = rod?.isOpenWaterFishing ?: true
        val isJungle =
            if (rodPos != null) level.getBiome(rodPos).`is`(BiomeTags.IS_JUNGLE)
            else if (playerPos != null) level.getBiome(playerPos).`is`(BiomeTags.IS_JUNGLE)
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

            val vector = when {
                overlay.rotation == OverlayRotation.HORIZONTAL && overlay.reversed  -> intArrayOf(-1, 0)
                overlay.rotation == OverlayRotation.HORIZONTAL                      -> intArrayOf(1, 0)
                overlay.reversed                                                    -> intArrayOf(0, -1)
                else                                                                -> intArrayOf(0, 1)
            }
            val perpendicular = when {
                overlay.rotation == OverlayRotation.HORIZONTAL -> intArrayOf(0, 1)
                else -> intArrayOf(1, 0)
            }

            val subLists: List<SubList> = when (sequence) {
                is Vault -> if (overlay.split) {
                    sequence.rollEach(overlay.advances).mapIndexed { i, items ->
                        SubList((gridSize - 2) * i * perpendicular[0], (gridSize - 2) * i * perpendicular[1], items)
                    }
                } else {
                    listOf(SubList(0, 0, sequence.roll(overlay.advances)))
                }
                is PiglinBartering -> listOf(SubList(0, 0, sequence.roll(overlay.advances)))
                is WitherSkeleton -> {
                    val drops = sequence.roll(overlay.rollType, true, lootingLevel)
                    listOf(SubList(0, 0, drops.ifEmpty { listOf(ItemStack.EMPTY) }))
                }
                is Fishing -> listOf(SubList(0, 0, sequence.roll(overlay.advances, luck, isOpenWater, isJungle)))
                is Gravel  -> listOf(SubList(0, 0, sequence.roll(overlay.advances, fortuneLevel)))
                else -> emptyList()
            }

            for (subList in subLists) {
                for ((itemIndex, item) in subList.items.withIndex()) {
                    val step = (gridSize - 2) * itemIndex
                    val itemX = x + subList.xOffset + step * vector[0]
                    val itemY = y + subList.yOffset + step * vector[1]

                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, itemX, itemY, gridSize - 2, gridSize - 2)
                    graphics.item(item, itemX + 1, itemY + 1)
                    graphics.itemDecorations(font, item, itemX + 1, itemY + 1)
                    val hovered = mx in itemX until (itemX + gridSize) && my in itemY until (itemY + gridSize)
                    if (hovered) {
                        graphics.setTooltipForNextFrame(mc.font, item, mx, my)
                    }
                }
            }
        }
    }
}
package lgbt.faith.chiyoko.gui

import lgbt.faith.chiyoko.Chiyoko
import lgbt.faith.chiyoko.config.OverlayRotation
import lgbt.faith.chiyoko.config.RollType
import lgbt.faith.chiyoko.keys
import lgbt.faith.chiyoko.sequences.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.core.Holder
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.tags.BiomeTags
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.level.Level

class ChiyokoRenderer {
    data class SubList(val xOffset: Int, val yOffset: Int, val items: List<ItemStack>)

    private data class RollCacheKey(
        val advances: Int,
        val rngLo: Long,
        val rngHi: Long,
        val luck: Int,
        val isOpenWater: Boolean,
        val isJungle: Boolean,
        val fortuneLevel: Int,
        val lootingLevel: Int,
        val split: Boolean,
        val rollType: Any?,
    )

    private val rollCache = HashMap<String, Pair<RollCacheKey, List<SubList>>>()

    private var cachedRegistryLevel: Level? = null
    private var cachedLootingHolder: Holder<Enchantment>? = null
    private var cachedFortuneHolder: Holder<Enchantment>? = null

    val mc = Minecraft.getInstance()

    val SLOT_SPRITE = Identifier.parse("minecraft:container/slot")
    val font = mc.font

    private fun gridToPixel(cell: Int) = (cell * gridSize) + border

    private val gridSize = 20
    private val border = 1

    private fun enchantHolders(level: Level): Pair<Holder<Enchantment>, Holder<Enchantment>> {
        if (cachedRegistryLevel === level && cachedLootingHolder != null && cachedFortuneHolder != null) {
            return cachedLootingHolder!! to cachedFortuneHolder!!
        }
        val enchantLookup = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
        val looting = enchantLookup.getOrThrow(Enchantments.LOOTING)
        val fortune = enchantLookup.getOrThrow(Enchantments.FORTUNE)
        cachedRegistryLevel = level
        cachedLootingHolder = looting
        cachedFortuneHolder = fortune
        return looting to fortune
    }

    private fun enchantLevel(holder: Holder<Enchantment>, player: Player): Int {
        val mainhand = player.getItemInHand(InteractionHand.MAIN_HAND)
        val offhand = player.getItemInHand(InteractionHand.OFF_HAND)
        return maxOf(
            EnchantmentHelper.getItemEnchantmentLevel(holder, mainhand),
            EnchantmentHelper.getItemEnchantmentLevel(holder, offhand),
        )
    }

    fun render(graphics: GuiGraphicsExtractor) {
        if (!Chiyoko.loaded) return


        if (
        /*? if >=26.2 {*/
        /*mc.gui.hud.isHidden
        *//*?} else {*/
            mc.options.hideGui
        /*?}*/
            ) return
        var hoveredItem: ItemStack? = null
        val mc = Minecraft.getInstance()

        val player = mc.player ?: return
        val level = mc.level ?: return

        val (lootingHolder, fortuneHolder) = enchantHolders(level)

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

        val lootingLevel = enchantLevel(lootingHolder, player)
        val fortuneLevel = enchantLevel(fortuneHolder, player)

        val configManager = Chiyoko.configManager

        rollCache.keys.retainAll(keys.toSet())

        keys.forEachIndexed { index, key ->
            val sequence = Chiyoko.sequences.map[key] ?: return@forEachIndexed
            val overlay = configManager.config.getOverlay(key)

            if (overlay.tracked != true) return@forEachIndexed
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

            val rng = sequence.getRngCopy()
            val cacheKey = RollCacheKey(
                advances = overlay.advances,
                rngLo = rng.seedLo,
                rngHi = rng.seedHi,
                luck = luck,
                isOpenWater = isOpenWater,
                isJungle = isJungle,
                fortuneLevel = fortuneLevel,
                lootingLevel = lootingLevel,
                split = overlay.split,
                rollType = if (sequence is WitherSkeleton || sequence is Shulker) overlay.rollType else null,
            )

            val cached = rollCache[key]
            val subLists: List<SubList> = if (cached != null && cached.first == cacheKey) {
                cached.second
            } else {
                val rolled: List<SubList> = when (sequence) {
                    is Vault -> if (overlay.split) {
                        sequence.peekEach(overlay.advances).mapIndexed { i, items ->
                            SubList((gridSize - 2) * i * perpendicular[0], (gridSize - 2) * i * perpendicular[1], items)
                        }
                    } else {
                        listOf(SubList(0, 0, sequence.peek(overlay.advances)))
                    }
                    is PiglinBartering -> listOf(SubList(0, 0, sequence.roll(overlay.advances)))
                    is WitherSkeleton -> {
                        val drops = sequence.roll(overlay.rollType ?: RollType.KillsUntilItem, true, lootingLevel)
                        listOf(SubList(0, 0, drops.ifEmpty { listOf(ItemStack.EMPTY) }))
                    }
                    is Shulker -> {
                        val drops = sequence.roll(overlay.rollType ?: RollType.KillsUntilItem, lootingLevel)
                        listOf(SubList(0, 0, drops.ifEmpty { listOf(ItemStack.EMPTY) }))
                    }
                    is Fishing -> listOf(SubList(0, 0, sequence.peek(overlay.advances, luck, isOpenWater, isJungle)))
                    is Gravel  -> listOf(SubList(0, 0, sequence.roll(overlay.advances, fortuneLevel)))

                    else -> emptyList()
                }
                rollCache[key] = cacheKey to rolled
                rolled
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
                        hoveredItem = item
                    }
                }
            }
        }
        if (hoveredItem != null) {
            val tickDelta = mc.deltaTracker.gameTimeDeltaTicks
            graphics.setTooltipForNextFrame(mc.font, hoveredItem, mx, my)
            graphics.extractDeferredElements(mx, my, tickDelta)
        }
    }
}
package lgbt.faith.chiyoko.functions

import lgbt.faith.chiyoko.ItemEnchantData
import lgbt.faith.chiyoko.rand.LCG
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items

data class EnchantTarget(
    val enchantment: Enchantment,
    val level: Int
)

object EnchantPredictor {
    var entityLCG: LCG? = null

    data class Result(val drops: Int, val bookshelves: Int, val slot: Int)

    fun predict(item: Item, targets: List<EnchantTarget>, maxDrops: Int = 1024): Result? {
        val base = entityLCG?.copy() ?: return null
        val (enchantability, eligible) = ItemEnchantData.of(item)

        for (n in 0..maxDrops) {
            val entity = base.copy()

            // n item throws
            repeat(n) {
                repeat(4) { entity.nextFloat() }
            }


            val xpSeed = entity.nextInt()
            val xpSeedLong = xpSeed.toLong()

            for (bookshelves in 15 downTo 0) {
                val costRng = LCG()
                costRng.setSeed(xpSeedLong)

                val costs = IntArray(3)
                for (i in 0..2) {
                    var c = EnchantFunctions.getSimulatedCost(costRng, i, bookshelves, enchantability)
                    if (c < i + 1) c = 0
                    costs[i] = c
                }

                for (slot in 2 downTo 0) {
                    val cost = costs[slot]
                    if (cost <= 0) continue

                    val rng = LCG()
                    rng.setSeed((xpSeed + slot).toLong())
                    @Suppress("UNCHECKED_CAST")
                    val predicted = EnchantFunctions.enchantWithLevels(
                        rng, enchantability, eligible as Set<Nothing>, cost
                    ).toMutableList()
                    if (predicted.size > 1 && item == Items.BOOK) {
                        predicted.removeAt(rng.nextInt(predicted.size))
                    }

                    val matched = predicted.size >= targets.size && targets.all { target ->
                        predicted.any {
                            it.enchantment.registeredName.replace("minecraft:", "") == target.enchantment.id &&
                                    it.level == target.level
                        }
                    }
                    if (matched) return Result(n, bookshelves, slot)
                }
            }
        }
        return null
    }
}
package lgbt.faith.chiyoko.functions

import lgbt.faith.chiyoko.Chiyoko
import lgbt.faith.chiyoko.rand.Rand
import lgbt.faith.chiyoko.sendOverlay
import net.minecraft.client.Minecraft
import net.minecraft.core.registries.Registries
import net.minecraft.world.inventory.EnchantmentMenu

object EnchantmentCracker {
    var lastLower16: Int = -1
    var possibleSeeds: List<Int> = emptyList()
    var cachedCrackedSeed: Int = 0

    private fun getSimulatedCost(rand: Rand, slot: Int, bookshelves: Int, enchantability: Int): Int {
        if (enchantability <= 0) return 0
        val b = if (bookshelves > 15) 15 else bookshelves
        val baseCost = rand.nextInt(8) + 1 + (b shr 1) + rand.nextInt(b + 1)

        var finalCost = when (slot) {
            0 -> maxOf(baseCost / 3, 1)
            1 -> (baseCost * 2) / 3 + 1
            else -> maxOf(baseCost, b * 2)
        }

        if (finalCost < slot + 1) {
            finalCost = 0
        }
        return finalCost
    }

    fun getOrCrackSeed(
        menu: EnchantmentMenu,
        enchantability: Int,
        eligibleEnchantments: Set<String>,
        isBook: Boolean
    ): Int {
        val lower16 = Chiyoko.xpSeed

        if (lower16 != lastLower16) {
            lastLower16 = lower16
            possibleSeeds = emptyList()
            cachedCrackedSeed = 0
        }

        if (possibleSeeds.size == 1) {
            return possibleSeeds.first()
        }

        val base16 = lower16 and 0xFFFF
        val validSlots = (0..2).filter { menu.costs[it] > 0 && menu.enchantClue[it] != -1 }

        if (validSlots.isEmpty()) {
            return lower16
        }

        val mc = Minecraft.getInstance()
        val level = mc.level ?: return lower16
        val registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).asHolderIdMap()

        val candidatesToTest = possibleSeeds.ifEmpty {
            (0..65535).map { (it shl 16) or base16 }
        }

        val newMatches = mutableListOf<Pair<Int, Int>>() // Pair<candidateSeed, bookshelfCount>

        for (candidateSeed in candidatesToTest) {
            var bestB = -1

            for (b in 15 downTo 0) {
                val costRand = Rand()
                costRand.setSeed(candidateSeed.toLong())

                val c0 = getSimulatedCost(costRand, 0, b, enchantability)
                val c1 = getSimulatedCost(costRand, 1, b, enchantability)
                val c2 = getSimulatedCost(costRand, 2, b, enchantability)

                if (c0 == menu.costs[0] && c1 == menu.costs[1] && c2 == menu.costs[2]) {
                    bestB = b
                    break
                }
            }

            if (bestB == -1) continue

            var isMatch = true
            for (i in validSlots) {
                val rand = Rand()
                rand.setSeed((candidateSeed + i).toLong())

                val results = EnchantFunctions.enchantWithLevels(
                    rng = rand,
                    enchantability = enchantability,
                    eligibleIds = eligibleEnchantments,
                    baseCost = menu.costs[i]
                ).toMutableList()

                if (results.isEmpty()) {
                    isMatch = false
                    break
                }

                if (results.size > 1 && isBook) {
                    results.removeAt(rand.nextInt(results.size))
                }

                val clueIndex = rand.nextInt(results.size)
                val predictedClue = results[clueIndex]
                val predictedId = registry.getId(predictedClue.enchantment)

                if (predictedId != menu.enchantClue[i] || predictedClue.level != menu.levelClue[i]) {
                    isMatch = false
                    break
                }
            }

            if (isMatch) {
                newMatches.add(candidateSeed to bestB)
            }
        }

        possibleSeeds = newMatches.sortedByDescending { it.second }.map { it.first }

        return when {
            possibleSeeds.isEmpty() -> {
                sendOverlay("failed to crack (0 matches).")
                lastLower16 = -1
                lower16
            }
            possibleSeeds.size == 1 -> {
                sendOverlay("cracked successfully!")
                cachedCrackedSeed = possibleSeeds.first()
                possibleSeeds.first()
            }
            else -> {
                sendOverlay("found ${possibleSeeds.size} possible seeds. swap items to narrow down!")
                possibleSeeds.first() // returns the seed with the highest bookshelf match
            }
        }
    }
}
package lgbt.faith.chiyoko.functions

import lgbt.faith.chiyoko.rand.LCG
import lgbt.faith.chiyoko.sendOverlay
import net.minecraft.client.Minecraft
import net.minecraft.core.registries.Registries
import net.minecraft.world.inventory.EnchantmentMenu

object XpSeedCracker {
    var lastLower16: Int = -1
    var possibleSeeds: List<Int> = emptyList()
    var cachedCrackedSeed: Int = 0

    fun getOrCrackSeed(
        partialSeed: Int,
        menu: EnchantmentMenu,
        enchantability: Int,
        eligibleEnchantments: Set<String>,
        isBook: Boolean
    ): Int? {
        val lower16 = partialSeed

        if (lower16 != lastLower16) {
            lastLower16 = lower16
            possibleSeeds = emptyList()
        }

        if (possibleSeeds.size == 1) {
            return possibleSeeds.first()
        }

        val base16 = lower16 and 0xFFFF
        val validSlots = (0..2).filter { menu.costs[it] > 0 && menu.enchantClue[it] != -1 }

        if (validSlots.isEmpty()) {
            return null
        }

        val mc = Minecraft.getInstance()
        val level = mc.level ?: return null
        val registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).asHolderIdMap()

        val candidatesToTest = possibleSeeds.ifEmpty {
            (0..65535).map { (it shl 16) or base16 }
        }

        val newMatches = mutableListOf<Pair<Int, Int>>()

        for (candidateSeed in candidatesToTest) {
            var bestB = -1

            for (b in 15 downTo 0) {
                val costRand = LCG()
                costRand.setSeed(candidateSeed.toLong())

                val c0 = EnchantFunctions.getSimulatedCost(costRand, 0, b, enchantability)
                val c1 = EnchantFunctions.getSimulatedCost(costRand, 1, b, enchantability)
                val c2 = EnchantFunctions.getSimulatedCost(costRand, 2, b, enchantability)

                if (c0 == menu.costs[0] && c1 == menu.costs[1] && c2 == menu.costs[2]) {
                    bestB = b
                    break
                }
            }

            if (bestB == -1) continue

            var isMatch = true
            for (i in validSlots) {
                val rand = LCG()
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
                lastLower16 = -1
                null
            }
            possibleSeeds.size == 1 -> {
                possibleSeeds.first()
            }
            else -> {
                null
            }
        }
    }
}
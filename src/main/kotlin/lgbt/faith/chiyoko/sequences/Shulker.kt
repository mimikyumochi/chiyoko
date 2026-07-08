package lgbt.faith.chiyoko.sequences

import lgbt.faith.chiyoko.config.RollType
import lgbt.faith.chiyoko.rand.RandomSupport
import lgbt.faith.chiyoko.rand.Xoroshiro128PlusPlus
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

class Shulker : Sequence {
    private lateinit var xoroshiro: Xoroshiro128PlusPlus

    override val key = "minecraft:entities/shulker"

    override fun init(worldSeed: Long) {
        xoroshiro = RandomSupport().createSequence(worldSeed, key)
    }
    override fun loadState(seedLo: Long, seedHi: Long) {
        xoroshiro = Xoroshiro128PlusPlus(seedLo, seedHi)
    }
    override fun getRngCopy(): Xoroshiro128PlusPlus {
        return xoroshiro.copy()
    }


    fun advance(n: Int, looting: Int) {
        repeat(n) {
            nextDrops(xoroshiro, looting)
        }
    }

    fun roll(type: RollType, looting: Int): List<ItemStack> {
        val rng = xoroshiro.copy()

        return when (type) {
            RollType.NextDrop -> {
                nextDrops(rng, looting)
            }
            RollType.KillsUntilItem -> {
                killsUntilShell(rng, looting)
            }
        }
    }

    fun killsUntilShell(rng: Xoroshiro128PlusPlus, looting: Int): List<ItemStack> {
        val chance = if (looting == 0) 0.5f else 0.5625f + (looting - 1) * 0.0625f

        var kills = 0
        while (true) {
            kills++
            if (rng.nextFloat() < chance) break
        }

        return listOf(ItemStack(Items.SHULKER_SHELL, kills))

    }

    fun nextDrops(rng: Xoroshiro128PlusPlus, looting: Int): MutableList<ItemStack> {
        val drops = mutableListOf<ItemStack>()

        val chance = if (looting == 0) 0.5f else 0.5625f + (looting - 1) * 0.0625f

        if (rng.nextFloat() < chance) {
            drops += ItemStack(Items.SHULKER_SHELL)
        }
        return drops
    }

    val lootTable = listOf(Items.SHULKER_SHELL)
}
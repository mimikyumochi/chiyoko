package lgbt.faith.chiyoko.sequences

import lgbt.faith.chiyoko.rand.Xoroshiro128PlusPlus
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

class Gravel : Sequence {
    override lateinit var xoroshiro: Xoroshiro128PlusPlus
    override val key = "minecraft:blocks/gravel"


    fun advance(advances: Int) {
        repeat(advances) {
            xoroshiro.nextFloat()
        }
    }

    val weights = listOf(0.1f, 0.14285715f, 0.25f, 1.0f) // fortune 0, 1, 2, 3

    fun roll(advances: Int = 1, fortuneLevel: Int = 0): List<ItemStack> {
        val rng = xoroshiro.copy()
        val drops = mutableListOf<ItemStack>()
        repeat(advances) {
            val item = rng.nextFloat()
            if (item < weights[fortuneLevel.coerceAtMost(weights.lastIndex)]) {
                drops.add(ItemStack(Items.FLINT))
            } else {
                drops.add(ItemStack(Items.GRAVEL))
            }
        }
        return drops
    }
}
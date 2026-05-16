package lgbt.faith.chiyoko.functions

import lgbt.faith.chiyoko.rand.Xoroshiro128PlusPlus

class ItemFunctions {
    companion object {
        fun applyDamage(rng: Xoroshiro128PlusPlus, min: Float, max: Float): Float {
            val t = rng.nextFloat() // 0.0 - 1.0
            return min + (max - min) * t
        }

        fun setCount(rng: Xoroshiro128PlusPlus, min: Int, max: Int): Int {
            return rng.nextInt(max - min + 1) + min
        }
    }
}
package lgbt.faith.chiyoko.sequences

import lgbt.faith.chiyoko.rand.Xoroshiro128PlusPlus

interface Sequence {
    val key: String

    fun init(worldSeed: Long)
    fun loadState(seedLo: Long, seedHi: Long)
    fun getRngCopy(): Xoroshiro128PlusPlus
}
package lgbt.faith.chiyoko.rand

class LCG(var seed: Long = 0) {

    private val multiplier = 0x5DEECE66DL
    private val addend = 0xBL
    private val mask = (1L shl 48) - 1

    fun copy() = LCG(seed)

    fun next(bits: Int): Int {
        seed = (seed * multiplier + addend) and mask
        return (seed ushr (48-bits)).toInt()
    }
    fun nextFloat(): Float {
        return next(24) / (1 shl 24).toFloat()
    }
    fun nextInt(): Int {
        return next(32)
    }

    fun setSeed(newSeed: Long): Long {
        seed = (newSeed xor multiplier) and mask
        return seed
    }

    fun nextInt(bound: Int): Int {
        require(bound > 0)
        if ((bound and -bound) == bound) {
            // power of two
            return ((bound.toLong() * next(31)) shr 31).toInt()
        }
        var bits: Int
        var value: Int
        do {
            bits = next(31)
            value = bits % bound
        } while (bits - value + (bound - 1) < 0)

        return value
    }
}
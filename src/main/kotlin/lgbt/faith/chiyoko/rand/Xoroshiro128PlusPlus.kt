package lgbt.faith.chiyoko.rand


class Xoroshiro128PlusPlus(var seedLo: Long, var seedHi: Long) {
    data class State(val seedLo: Long, val seedHi: Long)

    fun copy() = Xoroshiro128PlusPlus(seedLo, seedHi)

    fun toState() = State(seedLo, seedHi)

    fun nextLong(): Long {
        var s0 = seedLo
        var s1 = seedHi

        val result = java.lang.Long.rotateLeft(s0 + s1, 17) + s0

        s1 = s1 xor s0

        seedLo = s0.rotateLeft(49) xor s1 xor (s1 shl 21)
        seedHi = s1.rotateLeft(28)

        return result
    }

    fun nextBits(bits: Int): Long {
        return nextLong() ushr 64 - bits
    }

    fun nextInt(): Int {
        return nextLong().toInt()
    }
    fun nextInt(bound: Int): Int {
        require(bound >= 0) { "bound must be non-negative but was $bound" }

        var randomBits = Integer.toUnsignedLong(nextInt())
        var multipliedRandomBits = randomBits * bound
        var fractionalPart = multipliedRandomBits and 4294967295L

        if (fractionalPart < bound.toLong()) {
            val unbiasedBucketsStartIndex = Integer.remainderUnsigned(bound.inv() + 1, bound)

            while (fractionalPart < unbiasedBucketsStartIndex.toLong()) {
                randomBits = Integer.toUnsignedLong(nextInt())
                multipliedRandomBits = randomBits * bound
                fractionalPart = multipliedRandomBits and 4294967295L

            }
        }
        val integerPart = multipliedRandomBits ushr 32
        return integerPart.toInt()
    }

    fun nextFloat(): Float {
        return nextBits(24) * 5.9604645E-8F
    }
    fun advance(n: Int) {
        repeat(n) { nextInt() }
    }

}
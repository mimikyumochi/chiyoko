package lgbt.faith.chiyoko.rand

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

object RandomSupport {

    private const val GOLDEN_RATIO_64 = 7640891576956012809L
    private const val SILVER_RATIO_64 = -7046029254386353131L

    data class Seed128Bit(val seedLo: Long, val seedHi: Long) {
        fun xor(lo: Long, hi: Long): Seed128Bit = Seed128Bit(seedLo xor lo, seedHi xor hi)
        fun xor(other: Seed128Bit): Seed128Bit = xor(other.seedLo, other.seedHi)
        fun mixed(): Seed128Bit = Seed128Bit(mixStafford13(seedLo), mixStafford13(seedHi))

        companion object {
            private fun mixStafford13(z: Long): Long {
                var temp = (z xor (z ushr 30)) * -4658895280553007687L
                temp = (temp xor (temp ushr 27)) * -7723592293110705685L
                return temp xor (temp ushr 31)
            }
        }
    }

    fun upgradeSeedTo128bitUnmixed(legacySeed: Long): Seed128Bit {
        val lowBits = legacySeed xor GOLDEN_RATIO_64
        val highBits = lowBits + SILVER_RATIO_64
        return Seed128Bit(lowBits, highBits)
    }

    fun upgradeSeedTo128Bit(legacySeed: Long): Seed128Bit {
        return upgradeSeedTo128bitUnmixed(legacySeed).mixed()
    }

    fun seedFromHashOf(input: String): Seed128Bit {
        val md = MessageDigest.getInstance("MD5")
        val hashCode = md.digest(input.toByteArray(StandardCharsets.UTF_8))

        val buffer = ByteBuffer.wrap(hashCode)
        return Seed128Bit(buffer.long, buffer.long)
    }

    fun createSequence(worldSeed: Long, key: String): Xoroshiro128PlusPlus {
        val seed128bit = upgradeSeedTo128bitUnmixed(worldSeed)
            .xor(seedFromHashOf(key))
            .mixed()

        return Xoroshiro128PlusPlus(seed128bit.seedLo, seed128bit.seedHi)
    }
}
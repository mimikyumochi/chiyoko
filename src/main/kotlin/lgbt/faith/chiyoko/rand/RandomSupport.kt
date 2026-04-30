package lgbt.faith.chiyoko.rand

import com.google.common.hash.HashFunction
import com.google.common.hash.Hashing
import com.google.common.primitives.Longs
import java.nio.charset.StandardCharsets

class RandomSupport {

    private val MD5_128: HashFunction = Hashing.md5()

    data class Seed128Bit(val seedLo: Long, var seedHi: Long) {
        fun xor(lo: Long, hi: Long): Seed128Bit {
            return Seed128Bit(seedLo xor lo, seedHi xor hi)
        }
        fun xor (other: Seed128Bit): Seed128Bit {
            return xor(other.seedLo, other.seedHi)
        }
        fun mixed(): Seed128Bit {
            return Seed128Bit(mixStafford13(seedLo), mixStafford13(seedHi))
        }
        fun mixStafford13(z: Long): Long {
            var temp = (z xor (z ushr 30)) * -4658895280553007687L
            temp = (temp xor (temp ushr 27)) * -7723592293110705685L
            return temp xor (temp ushr 31)
        }
    }


    fun upgradeSeedTo128bitUnmixed(legacySeed: Long): Seed128Bit {
        val lowBits = legacySeed xor 7640891576956012809L
        val highBits = lowBits + -7046029254386353131L

        return Seed128Bit(lowBits, highBits)
    }
    fun upgradeSeedTo128Bit(legacySeed: Long): Seed128Bit {
        return upgradeSeedTo128bitUnmixed(legacySeed).mixed()
    }
    fun seedFromHashOf(input: String): Seed128Bit {
        val hashCode = MD5_128.hashString(input, StandardCharsets.UTF_8).asBytes()
        val hashLo = Longs.fromBytes(
            hashCode[0],
            hashCode[1],
            hashCode[2],
            hashCode[3],
            hashCode[4],
            hashCode[5],
            hashCode[6],
            hashCode[7]
        )
        val hashHi = Longs.fromBytes(
            hashCode[8],
            hashCode[9],
            hashCode[10],
            hashCode[11],
            hashCode[12],
            hashCode[13],
            hashCode[14],
            hashCode[15]
        )
        return Seed128Bit(hashLo, hashHi)
    }

    fun createSequence(worldSeed: Long, key: String): Xoroshiro128PlusPlus {
        var seed128bit = upgradeSeedTo128bitUnmixed(worldSeed)

        seed128bit = seed128bit.xor(seedFromHashOf(key))
        val mixedSeed = seed128bit.mixed()

        return Xoroshiro128PlusPlus(mixedSeed.seedLo, mixedSeed.seedHi)
    }

}
package lgbt.faith.chiyoko.sequences

import lgbt.faith.chiyoko.rand.RandomSupport
import lgbt.faith.chiyoko.rand.Xoroshiro128PlusPlus
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import kotlin.math.roundToInt

class WitherSkeleton : Sequence {
    private lateinit var xoroshiro: Xoroshiro128PlusPlus

    override val key = "minecraft:entities/wither_skeleton"

    override fun init(worldSeed: Long) {
        xoroshiro = RandomSupport().createSequence(worldSeed, key)
    }

    override fun loadState(seedLo: Long, seedHi: Long) {
        xoroshiro = Xoroshiro128PlusPlus(seedLo, seedHi)
    }
    override fun getRngCopy(): Xoroshiro128PlusPlus {
        return xoroshiro.copy()
    }

    fun advance(amount: Int, playerKilled: Boolean, lootingLevel: Int) {
        repeat(amount) {
            nextDrops(xoroshiro, playerKilled, lootingLevel)
        }
    }

    enum class RollType { NextDrop, KillsUntilSkull }

    fun killsUntilDrop(rng: Xoroshiro128PlusPlus, looting: Int): List<ItemStack> {

        val chance = if (looting == 0) 0.025f else 0.035f + (looting - 1) * 0.01f
        var kills = 0
        while (true) {
            kills++
            rng.advance(2) // coal and bone drops
            if (looting > 0) rng.advance(2) // coal and bone looting bonus
            if (rng.nextFloat() < chance) break
        }
        return listOf(ItemStack(Items.WITHER_SKELETON_SKULL, kills))
    }

    fun nextDrops(rng: Xoroshiro128PlusPlus, playerKilled: Boolean, lootingLevel: Int): List<ItemStack> {
        val drops = mutableListOf<ItemStack>()

        val coalBase = rng.nextInt(3)-1
        val coalBonus = if (lootingLevel>0) (lootingLevel*rng.nextFloat() * 1.0f).roundToInt() else 0
        val totalCoal = maxOf(0, coalBase) + coalBonus
        if (totalCoal > 0) drops.add(ItemStack(Items.COAL, totalCoal))

        val boneBase = rng.nextInt(3)
        val boneBonus = if (lootingLevel > 0) (lootingLevel * rng.nextFloat() * 1.0f).roundToInt() else 0
        val totalBones = boneBase + boneBonus
        if (totalBones > 0) drops.add(ItemStack(Items.BONE, totalBones))

        if (playerKilled) {
            val chance = if (lootingLevel == 0) 0.025f else 0.035f + (lootingLevel - 1) * 0.01f
            if (rng.nextFloat() < chance) drops.add(ItemStack(Items.WITHER_SKELETON_SKULL, 1))
        }

        return drops
    }

    fun roll(type: RollType, playerKilled: Boolean, lootingLevel: Int): List<ItemStack> {
        val rng = xoroshiro.copy()
        return when (type) {
            RollType.NextDrop -> nextDrops(rng, playerKilled, lootingLevel)
            RollType.KillsUntilSkull -> killsUntilDrop(rng, lootingLevel)
        }
    }


}
package lgbt.faith.chiyoko.sequences

import lgbt.faith.chiyoko.functions.EligibleEnchantments
import lgbt.faith.chiyoko.functions.EnchantFunctions
import lgbt.faith.chiyoko.functions.Enchantability
import lgbt.faith.chiyoko.functions.ItemFunctions
import lgbt.faith.chiyoko.rand.Xoroshiro128PlusPlus
import net.minecraft.core.component.DataComponents
import net.minecraft.util.Mth
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.alchemy.PotionContents
import net.minecraft.world.item.alchemy.Potions
import net.minecraft.world.item.enchantment.ItemEnchantments

class Fishing : Sequence {
    enum class LootTable { FISH, JUNK, TREASURE }
    override lateinit var xoroshiro: Xoroshiro128PlusPlus
    override val key = "minecraft:gameplay/fishing"


    fun advance(amount: Int, luck: Int, isOpenWater: Boolean, isJungle: Boolean) {
        repeat(amount) {
            nextCatch(xoroshiro, luck, isOpenWater, isJungle)
        }
    }

    fun peek(amount: Int, luck: Int = 0, isOpenWater: Boolean = true, isJungle: Boolean = false): List<ItemStack> {
        val rng = xoroshiro.copy()
        return List(amount) { nextCatch(rng, luck, isOpenWater, isJungle) }.filterNotNull()
    }

    private fun nextCatch(rng: Xoroshiro128PlusPlus, luck: Int, isOpenWater: Boolean, isJungle: Boolean): ItemStack? {
        val table = getLootTable(rng, luck, isOpenWater) ?: return null
        val pool = getPool(table, isJungle)

        val roll = rng.nextInt(pool.last().end)
        val stack = pool.first { roll in it.start until it.end }.item.copy()

        applyFunctions(rng, stack, table)
        return stack
    }

    private fun applyFunctions(rng: Xoroshiro128PlusPlus, stack: ItemStack, table: LootTable) {

        if (table == LootTable.JUNK) {
            when (stack.item) {
                Items.LEATHER_BOOTS,
                Items.FISHING_ROD -> stack.damageValue = Mth.floor((1f - ItemFunctions.applyDamage(rng, 0f, 0.9f)) * stack.maxDamage)
            }
        }
        else if (table == LootTable.TREASURE) {
            when (stack.item) {
                Items.BOW -> {
                    stack.damageValue = Mth.floor((1f - ItemFunctions.applyDamage(rng, 0f, 0.25f)) * stack.maxDamage)
                    val enchants = EnchantFunctions.enchantWithLevels(rng, Enchantability.BOW, EligibleEnchantments.BOW, 30)
                    enchants.forEach { stack.enchant(it.enchantment, it.level) }
                }

                Items.ENCHANTED_BOOK -> {
                    val enchants = EnchantFunctions.enchantWithLevels(rng, Enchantability.BOOK, EligibleEnchantments.FISHING, 30)
                    val stored = ItemEnchantments.Mutable(ItemEnchantments.EMPTY)
                    enchants.forEach { stored.set(it.enchantment, it.level) }
                    stack.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable())
                }

                Items.FISHING_ROD -> {
                    stack.damageValue = Mth.floor((1f - ItemFunctions.applyDamage(rng, 0f, 0.25f)) * stack.maxDamage)
                    val enchants = EnchantFunctions.enchantWithLevels(rng, Enchantability.FISHING_ROD, EligibleEnchantments.FISHING_ROD, 30)
                    enchants.forEach { stack.enchant(it.enchantment, it.level) }
                }
            }
        }
    }

    companion object {

        fun fishTable() = listOf(
            Sequence.Entry(ItemStack(Items.COD), 0, 60),
            Sequence.Entry(ItemStack(Items.SALMON), 60, 85),
            Sequence.Entry(ItemStack(Items.TROPICAL_FISH), 85, 87),
            Sequence.Entry(ItemStack(Items.PUFFERFISH), 87, 100)
        )

        fun junkTable(isJungle: Boolean) = listOf(
            Sequence.Entry(ItemStack(Items.LILY_PAD), 0, 17),
            Sequence.Entry(ItemStack(Items.LEATHER_BOOTS), 17, 27),
            Sequence.Entry(ItemStack(Items.LEATHER), 27, 37),
            Sequence.Entry(ItemStack(Items.BONE), 37, 47),
            Sequence.Entry(ItemStack(Items.POTION).apply { set(DataComponents.POTION_CONTENTS, PotionContents(Potions.WATER)) }, 47, 57),
            Sequence.Entry(ItemStack(Items.STRING), 57, 62),
            Sequence.Entry(ItemStack(Items.FISHING_ROD), 62, 64),
            Sequence.Entry(ItemStack(Items.BOWL), 64, 74),
            Sequence.Entry(ItemStack(Items.STICK), 74, 79),
            Sequence.Entry(ItemStack(Items.INK_SAC), 79, 80),
            Sequence.Entry(ItemStack(Items.TRIPWIRE_HOOK), 80, 90),
            Sequence.Entry(ItemStack(Items.ROTTEN_FLESH), 90, 100),
            Sequence.Entry(ItemStack(Items.BAMBOO), 100, 110).takeIf { isJungle } ?: Sequence.Entry(ItemStack(Items.AIR), 0, 0)
        )

        fun treasureTable() = listOf(
            Sequence.Entry(ItemStack(Items.NAME_TAG), 0, 1),
            Sequence.Entry(ItemStack(Items.SADDLE), 1, 2),
            Sequence.Entry(ItemStack(Items.BOW), 2, 3),
            Sequence.Entry(ItemStack(Items.FISHING_ROD), 3, 4),
            Sequence.Entry(ItemStack(Items.ENCHANTED_BOOK), 4, 5),
            Sequence.Entry(ItemStack(Items.NAUTILUS_SHELL), 5, 6)
        )

        fun getLootTable(rng: Xoroshiro128PlusPlus, luck: Int, isOpenWater: Boolean): LootTable? {

            val entries = listOf(
                Triple(LootTable.JUNK, 10, -2),
                Triple(LootTable.TREASURE, if (isOpenWater) 5 else 0, 2),
                Triple(LootTable.FISH, 85, -1)
            )
            val valid = entries.filter { it.second > 0 }
            val weighted = valid.map { (type, weight, quality) ->
                val w = (weight + quality * luck).coerceAtLeast(0)
                type to w
            }
            val total = weighted.sumOf { it.second }
            val roll = rng.nextInt(total)

            var acc = 0
            for ((type, w) in weighted) {
                acc += w
                if (roll < acc) return type
            }

            return null
        }

        fun getPool(table: LootTable, isJungle: Boolean): List<Sequence.Entry> {
            val pool = when (table) {
                LootTable.FISH -> fishTable()
                LootTable.JUNK -> junkTable(isJungle)
                LootTable.TREASURE -> treasureTable()
            }.filter { it.item.item != Items.AIR }
            return pool
        }
    }
}
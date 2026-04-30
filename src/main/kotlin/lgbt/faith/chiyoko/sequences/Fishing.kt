package lgbt.faith.chiyoko.sequences

import lgbt.faith.chiyoko.rand.RandomSupport
import lgbt.faith.chiyoko.rand.Xoroshiro128PlusPlus
import net.minecraft.client.Minecraft
import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.Registries
import net.minecraft.tags.EnchantmentTags
import net.minecraft.util.Mth
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.alchemy.PotionContents
import net.minecraft.world.item.alchemy.Potions
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.EnchantmentInstance
import net.minecraft.world.item.enchantment.ItemEnchantments

class Fishing : Sequence {
    private lateinit var xoroshiro: Xoroshiro128PlusPlus

    override val key = "minecraft:gameplay/fishing"

    override fun init(worldSeed: Long) {
        xoroshiro = RandomSupport().createSequence(worldSeed, key)
    }
    override fun loadState(seedLo: Long, seedHi: Long) {
        xoroshiro = Xoroshiro128PlusPlus(seedLo, seedHi)
    }
    override fun getRngCopy(): Xoroshiro128PlusPlus {
        return xoroshiro.copy()
    }

    fun advance(amount: Int, luck: Int, isOpenWater: Boolean, isJungle: Boolean) {
        val rng = xoroshiro

        repeat(amount) {
            val table = getLootTable(rng, luck, isOpenWater)
            val pool = when (table) {
                LootTable.FISH -> fishTable()
                LootTable.JUNK -> junkTable(isJungle)
                LootTable.TREASURE -> treasureTable()
                null -> return@repeat
            }.filter { it.item.item != Items.AIR }
            val roll = rng.nextInt(pool.last().end)
            val itemStack = pool.first { roll in it.start until it.end }.item
            applyFunctions(rng, itemStack, table)

        }
    }

    private data class Entry(val item: ItemStack, val start: Int, val end: Int)
    private enum class LootTable { FISH, JUNK, TREASURE }

    private fun getLootTable(rng: Xoroshiro128PlusPlus, luck: Int, isOpenWater: Boolean): LootTable? {

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

    private fun fishTable() = listOf(
        Entry(ItemStack(Items.COD), 0, 60),
        Entry(ItemStack(Items.SALMON), 60, 85),
        Entry(ItemStack(Items.TROPICAL_FISH), 85, 87),
        Entry(ItemStack(Items.PUFFERFISH), 87, 100)
    )

    private fun junkTable(isJungle: Boolean) = listOf(
        Entry(ItemStack(Items.LILY_PAD), 0, 17),
        Entry(ItemStack(Items.LEATHER_BOOTS), 17, 27),
        Entry(ItemStack(Items.LEATHER), 27, 37),
        Entry(ItemStack(Items.BONE), 37, 47),
        Entry(getWaterBottle(), 47, 57),
        Entry(ItemStack(Items.STRING), 57, 62),
        Entry(ItemStack(Items.FISHING_ROD), 62, 64),
        Entry(ItemStack(Items.BOWL), 64, 74),
        Entry(ItemStack(Items.STICK), 74, 79),
        Entry(ItemStack(Items.INK_SAC), 79, 80),
        Entry(ItemStack(Items.TRIPWIRE_HOOK), 80, 90),
        Entry(ItemStack(Items.ROTTEN_FLESH), 90, 100),
        Entry(ItemStack(Items.BAMBOO), 100, 110).takeIf { isJungle } ?: Entry(ItemStack(Items.AIR), 0, 0)
    )

    private fun treasureTable() = listOf(
        Entry(ItemStack(Items.NAME_TAG), 0, 1),
        Entry(ItemStack(Items.SADDLE), 1, 2),
        Entry(ItemStack(Items.BOW), 2, 3),
        Entry(ItemStack(Items.FISHING_ROD), 3, 4),
        Entry(ItemStack(Items.ENCHANTED_BOOK), 4, 5),
        Entry(ItemStack(Items.NAUTILUS_SHELL), 5, 6)
    )
    private fun getWaterBottle(): ItemStack {
        val stack = ItemStack(Items.POTION)
        stack.set(DataComponents.POTION_CONTENTS, PotionContents(Potions.WATER))
        return stack
    }

    fun roll(amount: Int, luck: Int = 0, isOpenWater: Boolean = true, isJungle: Boolean = false): List<ItemStack> {
        val catches = mutableListOf<ItemStack>()
        val rng = xoroshiro.copy()

        repeat(amount) {
            val table = getLootTable(rng, luck, isOpenWater)
            val pool = when (table) {
                LootTable.FISH -> fishTable()
                LootTable.JUNK -> junkTable(isJungle)
                LootTable.TREASURE -> treasureTable()
                null -> return@repeat
            }.filter { it.item.item != Items.AIR }

            val roll = rng.nextInt(pool.last().end)
            val itemStack = pool.first { roll in it.start until it.end }.item

            val stack = itemStack.copy()
            applyFunctions(rng, stack, table)

            catches.add(stack)
        }

        return catches
    }



    private fun applyFunctions(rng: Xoroshiro128PlusPlus, stack: ItemStack, table: LootTable) {

        if (table == LootTable.JUNK) {
            when (stack.item) {
                Items.LEATHER_BOOTS,
                Items.FISHING_ROD -> applyDamage(rng, stack, 0.9f)
            }
        }
        else if (table == LootTable.TREASURE) {
            when (stack.item) {
                Items.BOW -> {
                    applyDamage(rng, stack, 0.25f)
                    val enchants = applyEnchant(rng, stack)
                    enchants.forEach { stack.enchant(it.enchantment, it.level) }
                }

                Items.ENCHANTED_BOOK -> {
                    val enchants = applyEnchant(rng, stack)
                    val stored = ItemEnchantments.Mutable(ItemEnchantments.EMPTY)
                    enchants.forEach { stored.set(it.enchantment, it.level) }
                    stack.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable())
                }

                Items.FISHING_ROD -> {
                    applyDamage(rng, stack, 0.25f)
                    val enchants = applyEnchant(rng, stack)
                    enchants.forEach { stack.enchant(it.enchantment, it.level) }
                }
            }
        }

    }

    private fun applyDamage(rng: Xoroshiro128PlusPlus, stack: ItemStack, max: Float) {
        val pct = 1f - (rng.nextFloat() * max).coerceIn(0f, 1f)
        stack.damageValue = Mth.floor(pct * stack.maxDamage)
    }

    fun applyEnchant(rng: Xoroshiro128PlusPlus, stack: ItemStack, baseCost: Int = 30): List<EnchantmentInstance> {
        val mc = Minecraft.getInstance()
        val registries = mc.player?.level()?.registryAccess() ?: return emptyList()
        val results = mutableListOf<EnchantmentInstance>()

        val enchantStream = registries.lookupOrThrow(Registries.ENCHANTMENT)
            .getOrThrow(EnchantmentTags.ON_RANDOM_LOOT)
            .stream()
            .toList()


        val isBook = stack.`is`(Items.ENCHANTED_BOOK)
        val lookupStack = if (isBook) ItemStack(Items.BOOK) else stack

        val enchantable = lookupStack.get(DataComponents.ENCHANTABLE) ?: return results

        var cost = baseCost + (1 + rng.nextInt(enchantable.value / 4 + 1) + rng.nextInt(enchantable.value / 4 + 1))

        val randomSpan = (rng.nextFloat() + rng.nextFloat() - 1.0f) * 0.15f

        cost = Mth.clamp(Math.round(cost + cost * randomSpan), 1, Int.MAX_VALUE)

        val available = getAvailableEnchantments(cost, lookupStack, enchantStream).toMutableList()

        if (available.isNotEmpty()) {
            val first = weightedPick(rng, available)
            results.add(first)

            while (rng.nextInt(50) <= cost) {
                if (!results.isEmpty()) filterCompatible(available, results.last())
                if (available.isEmpty()) break

                val next = weightedPick(rng, available)
                results.add(next)
                cost /= 2
            }
        }

        return results
    }

    private fun getAvailableEnchantments(
        cost: Int,
        fakeStack: ItemStack,
        source: List<Holder<Enchantment>>
    ): List<EnchantmentInstance> {
        val isBook = fakeStack.`is`(Items.BOOK)
        return source
            .filter { holder -> holder.value().isPrimaryItem(fakeStack) || isBook }
            .mapNotNull { holder ->
                val enchant = holder.value()
                (enchant.maxLevel downTo enchant.minLevel)
                    .firstOrNull { level -> cost >= enchant.getMinCost(level) && cost <= enchant.getMaxCost(level) }
                    ?.let { level -> EnchantmentInstance(holder, level) }
            }

    }

    private fun weightedPick(rng: Xoroshiro128PlusPlus, list: List<EnchantmentInstance>): EnchantmentInstance {

        val total = list.sumOf { it.enchantment.value().weight }
        val roll = rng.nextInt(total)
        var acc = 0
        for (e in list) {
            acc += e.enchantment.value().weight
            if (roll < acc) return e
        }
        error("unreachable")
    }

    private fun filterCompatible(list: MutableList<EnchantmentInstance>, last: EnchantmentInstance) {
        list.removeIf { e -> !Enchantment.areCompatible(last.enchantment, e.enchantment) }
    }

}
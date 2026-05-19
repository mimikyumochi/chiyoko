package lgbt.faith.chiyoko.sequences

import lgbt.faith.chiyoko.ChiyokoComponents
import lgbt.faith.chiyoko.functions.EnchantFunctions
import lgbt.faith.chiyoko.functions.ItemFunctions
import lgbt.faith.chiyoko.rand.RandomSupport
import lgbt.faith.chiyoko.rand.Xoroshiro128PlusPlus
import net.minecraft.core.component.DataComponents
import net.minecraft.util.Mth
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.alchemy.PotionContents
import net.minecraft.world.item.alchemy.Potions
import net.minecraft.world.item.component.OminousBottleAmplifier
import net.minecraft.world.item.enchantment.ItemEnchantments

class Vault(val isOminous: Boolean) : Sequence {
    private lateinit var xoroshiro: Xoroshiro128PlusPlus

    override val key = if (isOminous) "minecraft:chests/trial_chambers/reward_ominous" else "minecraft:chests/trial_chambers/reward"

    override fun init(worldSeed: Long) {
        xoroshiro = RandomSupport().createSequence(worldSeed, key)
    }
    override fun loadState(seedLo: Long, seedHi: Long) {
        xoroshiro = Xoroshiro128PlusPlus(seedLo, seedHi)
    }
    override fun getRngCopy(): Xoroshiro128PlusPlus {
        return xoroshiro.copy()
    }

    private val COMMON = if (isOminous) OMINOUS_COMMON else NORMAL_COMMON
    private val RARE = if (isOminous) OMINOUS_RARE else NORMAL_RARE
    private val UNIQUE = if (isOminous) OMINOUS_UNIQUE else NORMAL_UNIQUE

    fun advance(amount: Int) {
        repeat(amount) {
            val useRare = xoroshiro.nextInt(10) < 8
            val item = if (useRare) { rollWeighted(xoroshiro, RARE).copy() } else rollWeighted(xoroshiro, COMMON).copy()
            applyFunctions(xoroshiro, item)

            val rolls = xoroshiro.nextInt(3) + 1
            repeat(rolls) {
                applyFunctions(xoroshiro, rollWeighted(xoroshiro, COMMON).copy())
            }
            val chance = if (isOminous) 0.75f else 0.25f
            if (xoroshiro.nextFloat() < chance) {
                applyFunctions(xoroshiro, rollWeighted(xoroshiro, UNIQUE).copy())
            }

        }
    }

    fun roll(amount: Int, merged: Boolean = true): List<ItemStack>  {
        val drops = mutableListOf<ItemStack>()
        val rng = xoroshiro.copy()
        repeat(amount) {
            val local = mutableListOf<ItemStack>()

            val useRare = rng.nextInt(10) < 8
            val item = if (useRare) { rollWeighted(rng, RARE).copy() } else rollWeighted(rng, COMMON).copy()
            local += applyFunctions(rng, item)

            val rolls = rng.nextInt(3) + 1
            repeat(rolls) {
                local += applyFunctions(rng, rollWeighted(rng, COMMON).copy())
            }
            val chance = if (isOminous) 0.75f else 0.25f
            if (rng.nextFloat() < chance) {
                local += applyFunctions(rng, rollWeighted(rng, UNIQUE).copy())
            }

            drops += if (merged) mergeStacks(local) else local
        }
        return drops
    }

    fun rollEach(amount: Int): List<List<ItemStack>> {
        val rng = xoroshiro.copy()
        return List(amount) {
            val local = mutableListOf<ItemStack>()

            val useRare = rng.nextInt(10) < 8
            val item = if (useRare) rollWeighted(rng, RARE).copy() else rollWeighted(rng, COMMON).copy()
            local += applyFunctions(rng, item)

            val rolls = rng.nextInt(3) + 1
            repeat(rolls) {
                local += applyFunctions(rng, rollWeighted(rng, COMMON).copy())
            }
            val chance = if (isOminous) 0.75f else 0.25f
            if (rng.nextFloat() < chance) {
                local += applyFunctions(rng, rollWeighted(rng, UNIQUE).copy())
            }

            mergeStacks(local)
        }
    }


    private fun rollWeighted(rng: Xoroshiro128PlusPlus, table: List<Pair<ItemStack, Int>>): ItemStack {
        var roll = rng.nextInt(table.sumOf { it.second })
        for ((item, weight) in table) {
            roll -= weight
            if (roll < 0) return item
        }
        return table.last().first
    }

    private fun applyFunctions(rng: Xoroshiro128PlusPlus, item: ItemStack): ItemStack {
        if (isOminous) {
            when (item.item) {

                Items.EMERALD -> item.count = ItemFunctions.setCount(rng, 4, 10)
                Items.WIND_CHARGE -> item.count = ItemFunctions.setCount(rng, 8, 12)
                Items.TIPPED_ARROW -> item.count = ItemFunctions.setCount(rng, 4, 12)
                Items.DIAMOND -> item.count = ItemFunctions.setCount(rng, 2, 3)

                Items.OMINOUS_BOTTLE -> item.set(
                    DataComponents.OMINOUS_BOTTLE_AMPLIFIER,
                    OminousBottleAmplifier(rng.nextInt(3) + 2)
                ) // 2-4

                Items.CROSSBOW -> {
                    val levels = ItemFunctions.setCount(rng, 5, 20)
                    EnchantFunctions.enchantWithLevels(rng, item, levels).forEach {
                        item.enchant(it.enchantment, it.level)
                    }
                }

                Items.DIAMOND_AXE -> {
                    val levels = ItemFunctions.setCount(rng, 10, 20)
                    EnchantFunctions.enchantWithLevels(rng, item, levels).forEach {
                        item.enchant(it.enchantment, it.level)
                    }
                }

                Items.DIAMOND_CHESTPLATE -> {
                    val levels = ItemFunctions.setCount(rng, 10, 20)
                    EnchantFunctions.enchantWithLevels(rng, item, levels).forEach {
                        item.enchant(it.enchantment, it.level)
                    }
                }

                Items.ENCHANTED_BOOK -> {
                    when (item.get(ChiyokoComponents.VARIANT)) {
                        1 -> {
                            val enchant = EnchantFunctions.enchantRandomly(rng, listOf("breach", "density"))
                            if (enchant != null) {
                                val stored = ItemEnchantments.Mutable(ItemEnchantments.EMPTY)
                                stored.set(enchant.enchantment, enchant.level)
                                item.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable())
                            }
                        }

                        2 -> {
                            val stored = ItemEnchantments.Mutable(ItemEnchantments.EMPTY)
                            val windBurst = EnchantFunctions.getEnchantment("wind_burst")
                            if (windBurst != null) {
                                stored.set(windBurst, 1)
                                item.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable())
                            }
                        }

                        else -> {
                            val enchant = EnchantFunctions.enchantRandomly(
                                rng,
                                listOf("knockback", "punch", "smite", "looting", "multishot")
                            )
                            if (enchant != null) {
                                val stored = ItemEnchantments.Mutable(ItemEnchantments.EMPTY)
                                stored.set(enchant.enchantment, enchant.level)
                                item.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable())
                            }
                        }
                    }
                }
            }
        }
        else {
            when (item.item) {
                Items.ARROW         -> item.count = ItemFunctions.setCount(rng, 2, 8)
                Items.TIPPED_ARROW  -> item.count = ItemFunctions.setCount(rng, 2, 8)
                Items.IRON_INGOT    -> item.count = ItemFunctions.setCount(rng, 1, 4)
                Items.HONEY_BOTTLE  -> item.count = ItemFunctions.setCount(rng, 1, 2)
                Items.DIAMOND       -> item.count = ItemFunctions.setCount(rng, 1, 2)
                Items.GOLDEN_CARROT -> item.count = ItemFunctions.setCount(rng, 1, 2)
                Items.EMERALD       -> item.count = ItemFunctions.setCount(rng, 2, 4)

                Items.WIND_CHARGE  -> {
                    item.count = when (item.get(ChiyokoComponents.VARIANT)) {
                        1    ->  ItemFunctions.setCount(rng, 4, 12)
                        else -> ItemFunctions.setCount(rng, 1, 3)
                    }
                }


                Items.OMINOUS_BOTTLE -> item.set(DataComponents.OMINOUS_BOTTLE_AMPLIFIER, OminousBottleAmplifier(rng.nextInt(2)))

                Items.SHIELD -> item.damageValue = Mth.floor(1f - ItemFunctions.applyDamage(rng, 0.5f, 1f) * item.maxDamage)

                Items.BOW -> {
                    val levels = ItemFunctions.setCount(rng, 5, 15)
                    EnchantFunctions.enchantWithLevels(rng, item, levels).forEach {
                        item.enchant(it.enchantment, it.level)
                    }
                }
                Items.CROSSBOW -> {
                    val levels = ItemFunctions.setCount(rng, 5, 20)
                    EnchantFunctions.enchantWithLevels(rng, item, levels).forEach {
                        item.enchant(it.enchantment, it.level)
                    }
                }
                Items.IRON_AXE -> {
                    val levels = ItemFunctions.setCount(rng, 0, 10)
                    EnchantFunctions.enchantWithLevels(rng, item, levels).forEach {
                        item.enchant(it.enchantment, it.level)
                    }
                }
                Items.IRON_CHESTPLATE -> {
                    val levels = ItemFunctions.setCount(rng, 0, 10)
                    EnchantFunctions.enchantWithLevels(rng, item, levels).forEach {
                        item.enchant(it.enchantment, it.level)
                    }
                }
                Items.DIAMOND_CHESTPLATE -> {
                    val levels = ItemFunctions.setCount(rng, 5, 15)
                    EnchantFunctions.enchantWithLevels(rng, item, levels).forEach {
                        item.enchant(it.enchantment, it.level)
                    }
                }
                Items.DIAMOND_AXE -> {
                    val levels = ItemFunctions.setCount(rng, 5, 15)
                    EnchantFunctions.enchantWithLevels(rng, item, levels).forEach {
                        item.enchant(it.enchantment, it.level)
                    }
                }
                Items.ENCHANTED_BOOK -> {
                    val options = when (item.get(ChiyokoComponents.VARIANT)) {
                        1    -> listOf("riptide", "loyalty", "channeling", "impaling", "mending")
                        else -> listOf("sharpness", "bane_of_arthropods", "efficiency", "fortune", "silk_touch", "feather_falling")
                    }

                    val enchant = EnchantFunctions.enchantRandomly(rng, options)
                    if (enchant != null) {
                        val stored = ItemEnchantments.Mutable(ItemEnchantments.EMPTY)
                        stored.set(enchant.enchantment, enchant.level)
                        item.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable())
                    }

                }
            }
        }
        item.remove(ChiyokoComponents.VARIANT)
        return item
    }

    private fun mergeStacks(input: List<ItemStack>): List<ItemStack> {
        val result = mutableListOf<ItemStack>()

        for (stack in input) {
            if (stack.isEmpty) continue

            var remaining = stack.count

            for (existing in result) {
                if (ItemStack.isSameItemSameComponents(existing, stack)) {
                    val space = existing.maxStackSize - existing.count
                    if (space > 0) {
                        val toMove = minOf(space, remaining)
                        existing.grow(toMove)
                        remaining -= toMove
                        if (remaining <= 0) break
                    }
                }
            }

            if (remaining > 0) {
                val newStack = stack.copy()
                newStack.count = remaining
                result.add(newStack)
            }
        }

        return result
    }

    // tables
    companion object {
        val NORMAL_COMMON = listOf(
            ItemStack(Items.ARROW) to 4,
            ItemStack(Items.TIPPED_ARROW).apply { set(DataComponents.POTION_CONTENTS, PotionContents(Potions.POISON)) } to 4,
            ItemStack(Items.EMERALD) to 4,
            ItemStack(Items.WIND_CHARGE) to 3,
            ItemStack(Items.IRON_INGOT) to 3,
            ItemStack(Items.HONEY_BOTTLE) to 3,
            ItemStack(Items.OMINOUS_BOTTLE) to 2,
            ItemStack(Items.WIND_CHARGE).apply { set(ChiyokoComponents.VARIANT, 1) } to 1,
            ItemStack(Items.DIAMOND) to 1
        )
        val NORMAL_RARE = listOf(
            ItemStack(Items.EMERALD) to 3,
            ItemStack(Items.SHIELD) to 3,
            ItemStack(Items.BOW) to 3,
            ItemStack(Items.CROSSBOW) to 2,
            ItemStack(Items.IRON_AXE) to 2,
            ItemStack(Items.IRON_CHESTPLATE) to 2,
            ItemStack(Items.GOLDEN_CARROT) to 2,
            ItemStack(Items.ENCHANTED_BOOK) to 2,
            ItemStack(Items.ENCHANTED_BOOK).apply { set(ChiyokoComponents.VARIANT, 1) } to 2,
            ItemStack(Items.DIAMOND_CHESTPLATE) to 1,
            ItemStack(Items.DIAMOND_AXE) to 1
        )
        val NORMAL_UNIQUE = listOf(
            ItemStack(Items.GOLDEN_APPLE) to 4,
            ItemStack(Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE) to 3,
            ItemStack(Items.GUSTER_BANNER_PATTERN) to 2,
            ItemStack(Items.MUSIC_DISC_PRECIPICE) to 2,
            ItemStack(Items.TRIDENT) to 1
        )
        val OMINOUS_COMMON = listOf(
            ItemStack(Items.EMERALD) to 5,
            ItemStack(Items.WIND_CHARGE) to 4,
            ItemStack(Items.TIPPED_ARROW).apply { set(DataComponents.POTION_CONTENTS, PotionContents(Potions.STRONG_SLOWNESS)) } to 3,
            ItemStack(Items.DIAMOND) to 2,
            ItemStack(Items.OMINOUS_BOTTLE) to 1
        )
        val OMINOUS_RARE = listOf(
            ItemStack(Items.EMERALD_BLOCK) to 5,
            ItemStack(Items.IRON_BLOCK) to 4,
            ItemStack(Items.CROSSBOW) to 4,
            ItemStack(Items.GOLDEN_APPLE) to 3,
            ItemStack(Items.DIAMOND_AXE) to 3,
            ItemStack(Items.DIAMOND_CHESTPLATE) to 3,
            ItemStack(Items.ENCHANTED_BOOK) to 2,
            ItemStack(Items.ENCHANTED_BOOK).apply { set(ChiyokoComponents.VARIANT, 1)} to 2,
            ItemStack(Items.ENCHANTED_BOOK).apply { set(ChiyokoComponents.VARIANT, 2)} to 2,
            ItemStack(Items.DIAMOND_BLOCK) to 1,
        )

        val OMINOUS_UNIQUE = listOf(
            ItemStack(Items.ENCHANTED_GOLDEN_APPLE) to 3,
            ItemStack(Items.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE) to 3,
            ItemStack(Items.FLOW_BANNER_PATTERN) to 2,
            ItemStack(Items.MUSIC_DISC_CREATOR) to 1,
            ItemStack(Items.HEAVY_CORE) to 1
        )
    }
}
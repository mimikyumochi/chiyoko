package lgbt.faith.chiyoko.sequences

import lgbt.faith.chiyoko.functions.EnchantFunctions
import lgbt.faith.chiyoko.functions.ItemFunctions
import lgbt.faith.chiyoko.rand.RandomSupport
import lgbt.faith.chiyoko.rand.Xoroshiro128PlusPlus
import net.minecraft.client.Minecraft
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.Registries
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.alchemy.PotionContents
import net.minecraft.world.item.alchemy.Potions
import net.minecraft.world.item.enchantment.Enchantments


class PiglinBartering : Sequence {
    private lateinit var xoroshiro: Xoroshiro128PlusPlus

    override val key = "minecraft:gameplay/piglin_bartering"

    override fun init(worldSeed: Long) {
        xoroshiro = RandomSupport().createSequence(worldSeed, key)
    }

    override fun loadState(seedLo: Long, seedHi: Long) {
        xoroshiro = Xoroshiro128PlusPlus(seedLo, seedHi)
    }
    override fun getRngCopy(): Xoroshiro128PlusPlus {
        return xoroshiro.copy()
    }

    fun advance(amount: Int = 1) {
        repeat(amount) {
            val roll = xoroshiro.nextInt(lootTable.last().end)
            val item = lootTable.first { roll in it.start until it.end }.item
            val count = applyFunctions(xoroshiro, item)
        }
    }



    data class Entry(val item: ItemStack, val start: Int, val end: Int)

    val lootTable = listOf(
        Entry(ItemStack(Items.ENCHANTED_BOOK), 0, 5),
        Entry(ItemStack(Items.IRON_BOOTS), 5, 13),
        Entry(ItemStack(Items.POTION).apply { set(DataComponents.POTION_CONTENTS, PotionContents(Potions.FIRE_RESISTANCE)) }, 13, 21),
        Entry(ItemStack(Items.SPLASH_POTION).apply { set(DataComponents.POTION_CONTENTS, PotionContents(Potions.FIRE_RESISTANCE)) }, 21, 29),
        Entry(ItemStack(Items.POTION).apply { set(DataComponents.POTION_CONTENTS, PotionContents(Potions.WATER)) }, 29, 39),
        Entry(ItemStack(Items.IRON_NUGGET), 39, 49),
        Entry(ItemStack(Items.ENDER_PEARL), 49, 59),
        Entry(ItemStack(Items.DRIED_GHAST), 59, 69),
        Entry(ItemStack(Items.STRING), 69, 89),
        Entry(ItemStack(Items.QUARTZ), 89, 109),
        Entry(ItemStack(Items.OBSIDIAN), 109, 149),
        Entry(ItemStack(Items.CRYING_OBSIDIAN), 149, 189),
        Entry(ItemStack(Items.FIRE_CHARGE), 189, 229),
        Entry(ItemStack(Items.LEATHER), 229, 269),
        Entry(ItemStack(Items.SOUL_SAND), 269, 309),
        Entry(ItemStack(Items.NETHER_BRICK), 309, 349),
        Entry(ItemStack(Items.SPECTRAL_ARROW), 349, 389),
        Entry(ItemStack(Items.GRAVEL), 389, 429),
        Entry(ItemStack(Items.BLACKSTONE), 429, 469),
    )

    fun roll(amount: Int): List<ItemStack> {
        val itemList = mutableListOf<ItemStack>()
        val rng = xoroshiro.copy()

        repeat(amount) {
            val roll = rng.nextInt(lootTable.last().end)

            val itemStack = lootTable.first { roll in it.start until it.end }.item
            val info = applyFunctions(rng, itemStack)

            val item = itemStack.item
            val stack = itemStack.copy()
            when (item) {
                Items.ENCHANTED_BOOK, Items.IRON_BOOTS -> {
                    val registryAccess =
                        Minecraft.getInstance().level?.registryAccess()?.lookupOrThrow(Registries.ENCHANTMENT)
                    stack.enchant(registryAccess!!.getOrThrow(Enchantments.SOUL_SPEED), info)
                }

                else -> {
                    stack.count = info
                }
            }
            itemList.add(stack)
        }
        return itemList
    }
    fun applyFunctions(rng: Xoroshiro128PlusPlus, itemStack: ItemStack): Int {
        val item = itemStack.item
        return when (item) {
            Items.ENCHANTED_BOOK, Items.IRON_BOOTS -> {
                EnchantFunctions.enchantRandomly(rng, listOf("soul_speed"))?.level ?: 1
            }
            Items.IRON_NUGGET      -> ItemFunctions.setCount(rng, 10, 36) // 10-36
            Items.ENDER_PEARL      -> ItemFunctions.setCount(rng, 2, 4)    // 2-4
            Items.STRING           -> ItemFunctions.setCount(rng, 3, 9)    // 3-9
            Items.QUARTZ           -> ItemFunctions.setCount(rng, 5, 12)   // 5-12
            Items.CRYING_OBSIDIAN  -> ItemFunctions.setCount(rng, 1, 3)    // 1-3
            Items.LEATHER          -> ItemFunctions.setCount(rng, 2, 4)    // 2-4
            Items.SOUL_SAND        -> ItemFunctions.setCount(rng, 2, 8)    // 2-8
            Items.NETHER_BRICK     -> ItemFunctions.setCount(rng, 2, 8)    // 2-8
            Items.SPECTRAL_ARROW   -> ItemFunctions.setCount(rng, 6, 12)   // 6-12
            Items.GRAVEL           -> ItemFunctions.setCount(rng, 8, 16)   // 8-16
            Items.BLACKSTONE       -> ItemFunctions.setCount(rng, 8, 16)   // 8-16

            else -> 1
        }

    }
}
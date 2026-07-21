package lgbt.faith.chiyoko.functions

import net.minecraft.world.item.Item
import net.minecraft.world.item.Items

object Enchantability {
    const val WOOD        = 15
    const val STONE       = 5
    const val IRON        = 9
    const val CHAIN       = 9
    const val GOLD        = 25
    const val DIAMOND     = 10
    const val NETHERITE   = 15
    const val LEATHER     = 15
    const val BOOK        = 1
    const val TURTLE      = 9
    const val TRIDENT     = 1
    const val BOW         = 1
    const val CROSSBOW    = 1
    const val FISHING_ROD = 1
    fun getEnchantability(item: Item): Int {
        return when (item) {
            Items.WOODEN_SWORD,
            Items.WOODEN_AXE,
            Items.WOODEN_PICKAXE,
            Items.WOODEN_SHOVEL,
            Items.WOODEN_HOE -> WOOD

            Items.STONE_SWORD,
            Items.STONE_AXE,
            Items.STONE_PICKAXE,
            Items.STONE_SHOVEL,
            Items.STONE_HOE -> STONE

            Items.IRON_SWORD,
            Items.IRON_AXE,
            Items.IRON_PICKAXE,
            Items.IRON_SHOVEL,
            Items.IRON_HOE,
            Items.IRON_HELMET,
            Items.IRON_CHESTPLATE,
            Items.IRON_LEGGINGS,
            Items.IRON_BOOTS -> IRON

            Items.CHAINMAIL_HELMET,
            Items.CHAINMAIL_CHESTPLATE,
            Items.CHAINMAIL_LEGGINGS,
            Items.CHAINMAIL_BOOTS -> CHAIN

            Items.GOLDEN_SWORD,
            Items.GOLDEN_AXE,
            Items.GOLDEN_PICKAXE,
            Items.GOLDEN_SHOVEL,
            Items.GOLDEN_HOE,
            Items.GOLDEN_HELMET,
            Items.GOLDEN_CHESTPLATE,
            Items.GOLDEN_LEGGINGS,
            Items.GOLDEN_BOOTS -> GOLD

            Items.DIAMOND_SWORD,
            Items.DIAMOND_AXE,
            Items.DIAMOND_PICKAXE,
            Items.DIAMOND_SHOVEL,
            Items.DIAMOND_HOE,
            Items.DIAMOND_HELMET,
            Items.DIAMOND_CHESTPLATE,
            Items.DIAMOND_LEGGINGS,
            Items.DIAMOND_BOOTS -> DIAMOND

            Items.NETHERITE_SWORD,
            Items.NETHERITE_AXE,
            Items.NETHERITE_PICKAXE,
            Items.NETHERITE_SHOVEL,
            Items.NETHERITE_HOE,
            Items.NETHERITE_HELMET,
            Items.NETHERITE_CHESTPLATE,
            Items.NETHERITE_LEGGINGS,
            Items.NETHERITE_BOOTS -> NETHERITE

            Items.LEATHER_HELMET,
            Items.LEATHER_CHESTPLATE,
            Items.LEATHER_LEGGINGS,
            Items.LEATHER_BOOTS -> LEATHER

            Items.TURTLE_HELMET -> TURTLE

            Items.BOW -> BOW
            Items.CROSSBOW -> CROSSBOW
            Items.TRIDENT -> TRIDENT
            Items.FISHING_ROD -> FISHING_ROD

            Items.BOOK,
            Items.ENCHANTED_BOOK -> BOOK

            else -> 0
        }
    }
}

data class EnchantmentInstance(val def: Enchantment, val level: Int)

data class Enchantment(
    val id: String,
    val weight: Int,
    val maxLevel: Int,
    val minCostBase: Int,
    val minCostPerLevel: Int,
    val maxCostBase: Int,
    val maxCostPerLevel: Int,
    val exclusiveSet: String? = null,
) {

    val minLevel: Int get() = 1

    fun getMinCost(level: Int): Int {
        return minCostBase + minCostPerLevel * (level - 1)
    }

    fun getMaxCost(level: Int): Int {
        return maxCostBase + maxCostPerLevel * (level - 1)
    }

    companion object {
        val ALL: List<Enchantment> = listOf(
            Enchantment("aqua_affinity", weight = 2, maxLevel = 1, minCostBase = 1, minCostPerLevel = 0, maxCostBase = 41, maxCostPerLevel = 0, exclusiveSet = null),
            Enchantment("bane_of_arthropods", weight = 5, maxLevel = 5, minCostBase = 5, minCostPerLevel = 8, maxCostBase = 25, maxCostPerLevel = 8, exclusiveSet = "damage"),
            Enchantment("binding_curse", weight = 1, maxLevel = 1, minCostBase = 25, minCostPerLevel = 0, maxCostBase = 50, maxCostPerLevel = 0, exclusiveSet = null),
            Enchantment("blast_protection", weight = 2, maxLevel = 4, minCostBase = 5, minCostPerLevel = 8, maxCostBase = 13, maxCostPerLevel = 8, exclusiveSet = "armor"),
            Enchantment("breach", weight = 2, maxLevel = 4, minCostBase = 15, minCostPerLevel = 9, maxCostBase = 65, maxCostPerLevel = 9, exclusiveSet = "damage"),
            Enchantment("channeling", weight = 1, maxLevel = 1, minCostBase = 25, minCostPerLevel = 0, maxCostBase = 50, maxCostPerLevel = 0, exclusiveSet = null),
            Enchantment("density", weight = 5, maxLevel = 5, minCostBase = 5, minCostPerLevel = 8, maxCostBase = 25, maxCostPerLevel = 8, exclusiveSet = "damage"),
            Enchantment("depth_strider", weight = 2, maxLevel = 3, minCostBase = 10, minCostPerLevel = 10, maxCostBase = 25, maxCostPerLevel = 10, exclusiveSet = "boots"),
            Enchantment("efficiency", weight = 10, maxLevel = 5, minCostBase = 1, minCostPerLevel = 10, maxCostBase = 51, maxCostPerLevel = 10, exclusiveSet = null),
            Enchantment("feather_falling", weight = 5, maxLevel = 4, minCostBase = 5, minCostPerLevel = 6, maxCostBase = 11, maxCostPerLevel = 6, exclusiveSet = null),
            Enchantment("fire_aspect", weight = 2, maxLevel = 2, minCostBase = 10, minCostPerLevel = 20, maxCostBase = 60, maxCostPerLevel = 20, exclusiveSet = null),
            Enchantment("fire_protection", weight = 5, maxLevel = 4, minCostBase = 10, minCostPerLevel = 8, maxCostBase = 18, maxCostPerLevel = 8, exclusiveSet = "armor"),
            Enchantment("flame", weight = 2, maxLevel = 1, minCostBase = 20, minCostPerLevel = 0, maxCostBase = 50, maxCostPerLevel = 0, exclusiveSet = null),
            Enchantment("fortune", weight = 2, maxLevel = 3, minCostBase = 15, minCostPerLevel = 9, maxCostBase = 65, maxCostPerLevel = 9, exclusiveSet = "mining"),
            Enchantment("frost_walker", weight = 2, maxLevel = 2, minCostBase = 10, minCostPerLevel = 10, maxCostBase = 25, maxCostPerLevel = 10, exclusiveSet = "boots"),
            Enchantment("impaling", weight = 2, maxLevel = 5, minCostBase = 1, minCostPerLevel = 8, maxCostBase = 21, maxCostPerLevel = 8, exclusiveSet = "damage"),
            Enchantment("infinity", weight = 1, maxLevel = 1, minCostBase = 20, minCostPerLevel = 0, maxCostBase = 50, maxCostPerLevel = 0, exclusiveSet = "mending_infinity"),
            Enchantment("knockback", weight = 5, maxLevel = 2, minCostBase = 5, minCostPerLevel = 20, maxCostBase = 55, maxCostPerLevel = 20, exclusiveSet = null),
            Enchantment("looting", weight = 2, maxLevel = 3, minCostBase = 15, minCostPerLevel = 9, maxCostBase = 65, maxCostPerLevel = 9, exclusiveSet = null),
            Enchantment("loyalty", weight = 5, maxLevel = 3, minCostBase = 12, minCostPerLevel = 7, maxCostBase = 50, maxCostPerLevel = 0, exclusiveSet = "riptide"),
            Enchantment("luck_of_the_sea", weight = 2, maxLevel = 3, minCostBase = 15, minCostPerLevel = 9, maxCostBase = 65, maxCostPerLevel = 9, exclusiveSet = null),
            Enchantment("lunge", weight = 5, maxLevel = 3, minCostBase = 5, minCostPerLevel = 8, maxCostBase = 25, maxCostPerLevel = 8, exclusiveSet = null),
            Enchantment("lure", weight = 2, maxLevel = 3, minCostBase = 15, minCostPerLevel = 9, maxCostBase = 65, maxCostPerLevel = 9, exclusiveSet = null),
            Enchantment("mending", weight = 2, maxLevel = 1, minCostBase = 25, minCostPerLevel = 25, maxCostBase = 75, maxCostPerLevel = 25, exclusiveSet = "mending_infinity"),
            Enchantment("multishot", weight = 2, maxLevel = 1, minCostBase = 20, minCostPerLevel = 0, maxCostBase = 50, maxCostPerLevel = 0, exclusiveSet = "crossbow"),
            Enchantment("piercing", weight = 10, maxLevel = 4, minCostBase = 1, minCostPerLevel = 10, maxCostBase = 50, maxCostPerLevel = 0, exclusiveSet = "crossbow"),
            Enchantment("power", weight = 10, maxLevel = 5, minCostBase = 1, minCostPerLevel = 10, maxCostBase = 16, maxCostPerLevel = 10, exclusiveSet = null),
            Enchantment("projectile_protection", weight = 5, maxLevel = 4, minCostBase = 3, minCostPerLevel = 6, maxCostBase = 9, maxCostPerLevel = 6, exclusiveSet = "armor"),
            Enchantment("protection", weight = 10, maxLevel = 4, minCostBase = 1, minCostPerLevel = 11, maxCostBase = 12, maxCostPerLevel = 11, exclusiveSet = "armor"),
            Enchantment("punch", weight = 2, maxLevel = 2, minCostBase = 12, minCostPerLevel = 20, maxCostBase = 37, maxCostPerLevel = 20, exclusiveSet = null),
            Enchantment("quick_charge", weight = 5, maxLevel = 3, minCostBase = 12, minCostPerLevel = 20, maxCostBase = 50, maxCostPerLevel = 0, exclusiveSet = null),
            Enchantment("respiration", weight = 2, maxLevel = 3, minCostBase = 10, minCostPerLevel = 10, maxCostBase = 40, maxCostPerLevel = 10, exclusiveSet = null),
            Enchantment("riptide", weight = 2, maxLevel = 3, minCostBase = 17, minCostPerLevel = 7, maxCostBase = 50, maxCostPerLevel = 0, exclusiveSet = "riptide"),
            Enchantment("sharpness", weight = 10, maxLevel = 5, minCostBase = 1, minCostPerLevel = 11, maxCostBase = 21, maxCostPerLevel = 11, exclusiveSet = "damage"),
            Enchantment("silk_touch", weight = 1, maxLevel = 1, minCostBase = 15, minCostPerLevel = 0, maxCostBase = 65, maxCostPerLevel = 0, exclusiveSet = "mining"),
            Enchantment("smite", weight = 5, maxLevel = 5, minCostBase = 5, minCostPerLevel = 8, maxCostBase = 25, maxCostPerLevel = 8, exclusiveSet = "damage"),
            Enchantment("soul_speed", weight = 1, maxLevel = 3, minCostBase = 10, minCostPerLevel = 10, maxCostBase = 25, maxCostPerLevel = 10, exclusiveSet = null),
            Enchantment("sweeping_edge", weight = 2, maxLevel = 3, minCostBase = 5, minCostPerLevel = 9, maxCostBase = 20, maxCostPerLevel = 9, exclusiveSet = null),
            Enchantment("swift_sneak", weight = 1, maxLevel = 3, minCostBase = 25, minCostPerLevel = 25, maxCostBase = 75, maxCostPerLevel = 25, exclusiveSet = null),
            Enchantment("thorns", weight = 1, maxLevel = 3, minCostBase = 10, minCostPerLevel = 20, maxCostBase = 60, maxCostPerLevel = 20, exclusiveSet = null),
            Enchantment("unbreaking", weight = 5, maxLevel = 3, minCostBase = 5, minCostPerLevel = 8, maxCostBase = 55, maxCostPerLevel = 8, exclusiveSet = null),
            Enchantment("vanishing_curse", weight = 1, maxLevel = 1, minCostBase = 25, minCostPerLevel = 0, maxCostBase = 50, maxCostPerLevel = 0, exclusiveSet = null),
            Enchantment("wind_burst", weight = 2, maxLevel = 3, minCostBase = 15, minCostPerLevel = 9, maxCostBase = 65, maxCostPerLevel = 9, exclusiveSet = null)
        )
        val BY_ID: Map<String, Enchantment> = ALL.associateBy { it.id }
        operator fun get(id: String): Enchantment? = BY_ID[id]
        fun areCompatible(a: Enchantment, b: Enchantment): Boolean {
            if (a.id == b.id) return false
            if (a.exclusiveSet != null && a.exclusiveSet == b.exclusiveSet) return false
            return true
        }
    }
}



object EligibleEnchantments {
    val LEGACY_REGISTRY_ORDER = listOf(
        "protection", "fire_protection", "feather_falling", "blast_protection",
        "projectile_protection", "respiration", "aqua_affinity", "thorns",
        "depth_strider", "sharpness", "smite", "bane_of_arthropods",
        "knockback", "fire_aspect", "looting", "sweeping_edge",
        "efficiency", "silk_touch", "unbreaking", "fortune",
        "power", "punch", "flame", "infinity",
        "luck_of_the_sea", "lure", "loyalty", "impaling",
        "riptide", "channeling", "multishot", "quick_charge",
        "piercing", "density", "breach", "lunge"
    )

    val SWORD = setOf(
        "sharpness", "smite", "bane_of_arthropods", "knockback",
        "fire_aspect", "looting", "sweeping_edge", "unbreaking", "mending", "vanishing_curse",
    )
    val AXE = setOf(
        "efficiency", "fortune", "silk_touch", "unbreaking", "mending", "vanishing_curse",
    )
    val PICKAXE = setOf(
        "efficiency", "fortune", "silk_touch", "unbreaking", "mending", "vanishing_curse",
    )
    val SHOVEL = setOf(
        "efficiency", "fortune", "silk_touch", "unbreaking", "mending", "vanishing_curse",
    )
    val HOE = setOf(
        "efficiency", "fortune", "silk_touch", "unbreaking", "mending", "vanishing_curse",
    )
    val HELMET = setOf(
        "protection", "fire_protection", "blast_protection", "projectile_protection",
        "respiration", "aqua_affinity", "thorns", "unbreaking", "mending", "vanishing_curse", "binding_curse",
    )
    val CHESTPLATE = setOf(
        "protection", "fire_protection", "blast_protection", "projectile_protection",
        "thorns", "unbreaking", "mending", "vanishing_curse", "binding_curse",
    )
    val LEGGINGS = setOf(
        "protection", "fire_protection", "blast_protection", "projectile_protection",
        "swift_sneak", "thorns", "unbreaking", "mending", "vanishing_curse", "binding_curse",
    )
    val BOOTS = setOf(
        "protection", "fire_protection", "blast_protection", "projectile_protection",
        "feather_falling", "depth_strider", "frost_walker", "soul_speed",
        "thorns", "unbreaking", "mending", "vanishing_curse", "binding_curse",
    )
    val BOW = setOf(
        "power", "punch", "flame", "infinity", "unbreaking", "mending", "vanishing_curse",
    )
    val CROSSBOW = setOf(
        "multishot", "piercing", "quick_charge", "unbreaking", "mending", "vanishing_curse",
    )
    val TRIDENT = setOf(
        "loyalty", "channeling", "riptide", "impaling", "unbreaking", "mending", "vanishing_curse",
    )
    val FISHING_ROD = setOf(
        "luck_of_the_sea", "lure", "unbreaking", "mending", "vanishing_curse",
    )
    val MACE = setOf(
        "density", "breach", "wind_burst", "fire_aspect", "smite", "bane_of_arthropods",
        "knockback", "unbreaking", "mending", "vanishing_curse",
    )
    val BOOK = Enchantment.ALL.map { it.id }.toSet()
    val FISHING = BOOK.filter { it !in listOf("wind_burst", "soul_speed", "swift_sneak")}.toSet() // these are exclusive to certain structures
    val ENCHANT_TABLE = BOOK.filter { it !in listOf("wind_burst", "soul_speed", "swift_sneak", "mending", "vanishing_curse", "binding_curse", "frost_walker")}.toSet() // these are treasure

    fun getEligibleEnchantments(item: Item): Set<String> {
        return when (item) {
            Items.ENCHANTED_BOOK,
            Items.BOOK -> BOOK

            Items.WOODEN_SWORD,
            Items.STONE_SWORD,
            Items.IRON_SWORD,
            Items.GOLDEN_SWORD,
            Items.DIAMOND_SWORD,
            Items.NETHERITE_SWORD -> SWORD

            Items.WOODEN_AXE,
            Items.STONE_AXE,
            Items.IRON_AXE,
            Items.GOLDEN_AXE,
            Items.DIAMOND_AXE,
            Items.NETHERITE_AXE -> AXE

            Items.WOODEN_PICKAXE,
            Items.STONE_PICKAXE,
            Items.IRON_PICKAXE,
            Items.GOLDEN_PICKAXE,
            Items.DIAMOND_PICKAXE,
            Items.NETHERITE_PICKAXE -> PICKAXE

            Items.WOODEN_SHOVEL,
            Items.STONE_SHOVEL,
            Items.IRON_SHOVEL,
            Items.GOLDEN_SHOVEL,
            Items.DIAMOND_SHOVEL,
            Items.NETHERITE_SHOVEL -> SHOVEL

            Items.WOODEN_HOE,
            Items.STONE_HOE,
            Items.IRON_HOE,
            Items.GOLDEN_HOE,
            Items.DIAMOND_HOE,
            Items.NETHERITE_HOE -> HOE

            Items.BOW -> BOW
            Items.CROSSBOW -> CROSSBOW
            Items.TRIDENT -> TRIDENT
            Items.FISHING_ROD -> FISHING_ROD
            Items.MACE -> MACE

            Items.LEATHER_HELMET,
            Items.CHAINMAIL_HELMET,
            Items.IRON_HELMET,
            Items.GOLDEN_HELMET,
            Items.DIAMOND_HELMET,
            Items.NETHERITE_HELMET,
            Items.TURTLE_HELMET -> HELMET

            Items.LEATHER_CHESTPLATE,
            Items.CHAINMAIL_CHESTPLATE,
            Items.IRON_CHESTPLATE,
            Items.GOLDEN_CHESTPLATE,
            Items.DIAMOND_CHESTPLATE,
            Items.NETHERITE_CHESTPLATE -> CHESTPLATE

            Items.LEATHER_LEGGINGS,
            Items.CHAINMAIL_LEGGINGS,
            Items.IRON_LEGGINGS,
            Items.GOLDEN_LEGGINGS,
            Items.DIAMOND_LEGGINGS,
            Items.NETHERITE_LEGGINGS -> LEGGINGS

            Items.LEATHER_BOOTS,
            Items.CHAINMAIL_BOOTS,
            Items.IRON_BOOTS,
            Items.GOLDEN_BOOTS,
            Items.DIAMOND_BOOTS,
            Items.NETHERITE_BOOTS -> BOOTS

            else -> emptySet()
        }
    }
}
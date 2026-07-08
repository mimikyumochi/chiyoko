package lgbt.faith.chiyoko.functions

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
    val ENCHANT_TABLE = BOOK.filter { it !in listOf("wind_burst", "soul_speed", "swift_sneak")}.toSet() // these are exclusive to certain structures
}
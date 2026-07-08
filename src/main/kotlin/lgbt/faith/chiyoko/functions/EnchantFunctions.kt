package lgbt.faith.chiyoko.functions

import lgbt.faith.chiyoko.rand.Xoroshiro128PlusPlus
import lgbt.faith.chiyoko.sendOverlay
import net.minecraft.client.Minecraft
import net.minecraft.core.Holder
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.tags.ItemTags
import net.minecraft.world.item.Items
import net.minecraft.world.item.enchantment.Enchantment as MinecraftEnchantment
import net.minecraft.world.item.enchantment.EnchantmentInstance as MinecraftEnchantmentInstance

object EnchantFunctions {

    fun logRegistryOrderForHeldItem() {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        val registries = player.level().registryAccess()

        val enchantmentLookup = registries.lookupOrThrow(Registries.ENCHANTMENT)
        val heldItem = player.mainHandItem

        if (heldItem.isEmpty) return

        println("// --- Valid Enchantments for ${heldItem.item} (Exact Registry Order) ---")

        enchantmentLookup.listElements().forEach { holder ->
            val enchantment = holder.value()
            val key = holder.key().identifier()

            val supported = enchantment.definition().supportedItems()
            val validForItem = heldItem.`is`(supported)
                    || heldItem.`is`(Items.BOOK)
                    || heldItem.`is`(Items.ENCHANTED_BOOK)

            if (validForItem) {
                println("$key (max level: ${enchantment.getMaxLevel()})")
            }
        }
    }


    fun enchantmentIdentifierToHolder(id: String): Holder<MinecraftEnchantment>? {
        val mc = Minecraft.getInstance()
        val registries = mc.player?.level()?.registryAccess() ?: return null
        val enchantmentRegistry = registries.lookupOrThrow(Registries.ENCHANTMENT)

        val identifier = Identifier.tryParse(id)  ?: return null

        return enchantmentRegistry.get(identifier).orElse(null)
    }

    // returns a minecraft enchantment object holder
    fun getEnchantment(id: String) = enchantmentIdentifierToHolder(id)

    // returns custom enchantment object
    private fun getEnchantmentObject(id: String) =  Enchantment.ALL.find { it.id == id }

    fun enchantRandomly(rng: Xoroshiro128PlusPlus, options: List<String>): MinecraftEnchantmentInstance? {

        val validHolders: List<Enchantment> =
            options.mapNotNull { getEnchantmentObject(it) }
        sendOverlay("$validHolders")
        if (validHolders.isEmpty()) return null

        val holder = validHolders[rng.nextInt(validHolders.size)]

        val min = holder.minLevel
        val max = holder.maxLevel
        val level = if (min >= max) min else min + rng.nextInt(max - min + 1)

        val mcHolder = enchantmentIdentifierToHolder(holder.id) ?: return null

        return MinecraftEnchantmentInstance(mcHolder, level)
    }

    fun enchantWithLevels(rng: Xoroshiro128PlusPlus, enchantability: Int, eligibleIds: Set<String>, baseCost: Int = 30): List<MinecraftEnchantmentInstance> {

        var cost = baseCost + (1 + rng.nextInt(enchantability / 4 + 1) + rng.nextInt(enchantability / 4 + 1))
        val randomSpan = (rng.nextFloat() + rng.nextFloat() - 1.0f) * 0.15f

        cost = Math.min(Math.max(Math.round(cost + cost * randomSpan), 1), Int.MAX_VALUE);

        val available = getAvailableEnchantments(cost, eligibleIds).toMutableList()
        val results = mutableListOf<EnchantmentInstance>()

        if (available.isNotEmpty()) {
            results.add(weightedPick(rng, available))

            while (rng.nextInt(50) <= cost) {
                if (results.isNotEmpty()) filterCompatible(available, results.last())
                if (available.isEmpty()) break
                results.add(weightedPick(rng, available))
                cost /= 2
            }
        }

        return results.mapNotNull { instance ->
            val mcHolder = enchantmentIdentifierToHolder(instance.def.id)
            if (mcHolder != null) MinecraftEnchantmentInstance(mcHolder, instance.level) else null
        }
    }

    private fun getAvailableEnchantments(
        cost: Int,
        eligibleIds: Set<String>,
    ): List<EnchantmentInstance> {
        return Enchantment.ALL
            .filter { it.id in eligibleIds }
            .mapNotNull { def ->
                (def.maxLevel downTo def.minLevel)
                    .firstOrNull { level -> cost >= def.getMinCost(level) && cost <= def.getMaxCost(level) }
                    ?.let { level -> EnchantmentInstance(def, level) }
            }
    }

    private fun weightedPick(
        rng: Xoroshiro128PlusPlus,
        list: List<EnchantmentInstance>,
    ): EnchantmentInstance {
        val total = list.sumOf { it.def.weight }
        val roll = rng.nextInt(total)
        var acc = 0
        for (e in list) {
            acc += e.def.weight
            if (roll < acc) return e
        }
        error("weightedPick: unreachable (total=$total)")
    }

    private fun filterCompatible(
        list: MutableList<EnchantmentInstance>,
        last: EnchantmentInstance,
    ) {
        list.removeIf { e -> !Enchantment.areCompatible(last.def, e.def) }
    }

}
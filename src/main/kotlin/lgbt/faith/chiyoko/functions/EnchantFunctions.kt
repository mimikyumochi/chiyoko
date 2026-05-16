package lgbt.faith.chiyoko.functions

import lgbt.faith.chiyoko.rand.Xoroshiro128PlusPlus
import net.minecraft.client.Minecraft
import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.tags.EnchantmentTags
import net.minecraft.util.Mth
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.EnchantmentInstance

class EnchantFunctions {

    companion object {

        fun getEnchantment(id: String): Holder<Enchantment>? {
            val mc = Minecraft.getInstance()
            val registries = mc.player?.level()?.registryAccess() ?: return null
            val enchantmentRegistry = registries.lookupOrThrow(Registries.ENCHANTMENT)

            val identifier = Identifier.tryParse(id)  ?: return null

            return enchantmentRegistry.get(identifier).orElse(null)

        }

        fun enchantRandomly(rng: Xoroshiro128PlusPlus, options: List<String>): EnchantmentInstance? {
            val mc = Minecraft.getInstance()
            val registries = mc.player?.level()?.registryAccess() ?: return null
            val enchantmentRegistry = registries.lookupOrThrow(Registries.ENCHANTMENT)

            val validHolders = options.mapNotNull { id ->
                val identifier = Identifier.tryParse(id)
                if (identifier != null) {
                    enchantmentRegistry.get(identifier).orElse(null)
                } else null
            }

            if (validHolders.isEmpty()) return null

            val holder = validHolders[rng.nextInt(validHolders.size)]

            val min = holder.value().minLevel
            val max = holder.value().maxLevel
            val level = if (min >= max) min else min + rng.nextInt(max - min + 1)

            return EnchantmentInstance(holder, level)
        }

        fun enchantWithLevels(rng: Xoroshiro128PlusPlus, stack: ItemStack, baseCost: Int): List<EnchantmentInstance> {
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
}
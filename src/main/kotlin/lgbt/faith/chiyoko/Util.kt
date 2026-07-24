package lgbt.faith.chiyoko

import com.mojang.brigadier.context.CommandContext
import com.mojang.serialization.Codec
import lgbt.faith.chiyoko.functions.EligibleEnchantments
import lgbt.faith.chiyoko.functions.EligibleEnchantments.LEGACY_REGISTRY_ORDER
import lgbt.faith.chiyoko.functions.Enchantability
import lgbt.faith.chiyoko.mixin.BiomeManagerAccessor
import lgbt.faith.chiyoko.rand.LCG
import lgbt.faith.chiyoko.sequences.Vault
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.core.component.DataComponentType
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentUtils
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.biome.BiomeManager

data class ItemEnchantData(
    val enchantability: Int,
    val eligibleEnchantments: Set<*>
) {
    companion object {
        fun of(item: Item): ItemEnchantData {
            val enchantability = Enchantability.getEnchantability(item)
            val eligible = EligibleEnchantments.getEligibleEnchantments(item)
                .intersect(EligibleEnchantments.ENCHANT_TABLE)
                .sortedBy { id ->
                    val idx = LEGACY_REGISTRY_ORDER.indexOf(id)
                    if (idx == -1) 999 else idx
                }
                .toSet()

            return ItemEnchantData(enchantability, eligible)
        }
    }
}

object ChiyokoComponents {
    val VARIANT: DataComponentType<Int> = DataComponentType.builder<Int>()
        .persistent(Codec.INT)
        .build()
}

fun isMatchingSeed(): Boolean {
    val mc = Minecraft.getInstance()
    val level = mc.level ?: return false

    val worldSeed = mc.singleplayerServer?.worldGenSettings?.options()?.seed()
    val worldHash = (level.biomeManager as BiomeManagerAccessor).biomeZoomSeed

    return worldSeed == Chiyoko.seed || worldHash == BiomeManager.obfuscateSeed(Chiyoko.seed)
}

fun crackEntitySeed(xpSeed1: Int, xpSeed2: Int): MutableList<LCG> {
    val matches = mutableListOf<LCG>()
    val highBits = (xpSeed1.toLong() and 0xFFFFFFFFL) shl 16

    for (lowBits in 0..65535) {
        val candidateState1 = highBits or lowBits.toLong()
        val rand = LCG(candidateState1)
        val simulatedXpSeed2 = rand.nextInt()

        if (simulatedXpSeed2 == xpSeed2) {
            matches.add(rand)
        }
    }
    return matches
}

fun sendOverlay(text: String, color: ChatFormatting = ChatFormatting.WHITE) {
    val mc = Minecraft.getInstance()
    mc.execute { mc.player?.sendOverlayMessage(Component.literal(text).withStyle(color)) }
}

fun handleVaultDesync(actual: ItemStack, isOminous: Boolean) {

    val sequences = Chiyoko.sequences.map
    val vault = if (isOminous) sequences["minecraft:chests/trial_chambers/reward_ominous"] as? Vault ?: return
    else           sequences["minecraft:chests/trial_chambers/reward"] as? Vault ?: return

    var advances = 0L
    val maxAdvances = 1000
    do {
        val predicted = vault.peek(1)
        vault.advance(1)
        advances++
    } while((predicted.lastOrNull()?.item != actual.item ||
            predicted.lastOrNull()?.count != actual.count) && advances < maxAdvances)

    if (advances > 0) {
        Chiyoko.configManager.updateSequence(Chiyoko.worldName, Chiyoko.seed, vault.getRngCopy(), vault.key, advances)
        sendOverlay("advanced $advances times to account for desync")
    }
}

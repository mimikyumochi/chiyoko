package lgbt.faith.chiyoko

import com.mojang.serialization.Codec
import lgbt.faith.chiyoko.mixin.BiomeManagerAccessor
import lgbt.faith.chiyoko.sequences.Vault
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponentType
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.biome.BiomeManager
import java.util.concurrent.ConcurrentLinkedQueue

object ChiyokoComponents {
    val VARIANT: DataComponentType<Int> = DataComponentType.builder<Int>()
        .persistent(Codec.INT)
        .build()
}

object DropCapture {
    val pendingDrops = mutableMapOf<Int, MutableList<ItemStack>>()
}
data class PendingVault(
    val pos: BlockPos,
    val predictedItems: List<ItemStack>,
    val vault: Vault,
    val ticksWaited: Int = 0,
)
object VaultInteractionState {
    val pendingVaults: ConcurrentLinkedQueue<PendingVault> = ConcurrentLinkedQueue()
}

fun isMatchingSeed(): Boolean {
    val mc = Minecraft.getInstance()
    val level = mc.level ?: return false

    val worldSeed = mc.singleplayerServer?.worldGenSettings?.options()?.seed()
    val worldHash = (level.biomeManager as BiomeManagerAccessor).biomeZoomSeed

    return worldSeed == Chiyoko.seed || worldHash == BiomeManager.obfuscateSeed(Chiyoko.seed)
}

fun handleVaultDesync(actual: ItemStack, isOminous: Boolean) {

    val sequences = Chiyoko.sequences.map
    val vault = if (isOminous) sequences["minecraft:chests/trial_chambers/reward_ominous"] as? Vault ?: return
    else           sequences["minecraft:chests/trial_chambers/reward"] as? Vault ?: return

    var advances = 0
    do {
        val predicted = vault.roll(1)
        vault.advance(1)

        val xoroshiro = vault.getRngCopy()
        Chiyoko.configManager.updateSequence(Chiyoko.worldName, Chiyoko.seed, xoroshiro, vault.key)
        advances++
    } while(predicted.lastOrNull()?.item == actual.item &&
            predicted.lastOrNull()?.count == actual.count)

    if (advances > 0) {
        val mc = Minecraft.getInstance()
        mc.execute {
            mc.player?.sendOverlayMessage(
                Component.literal("advanced $advances times to account for desync")
            )
        }

    }


}
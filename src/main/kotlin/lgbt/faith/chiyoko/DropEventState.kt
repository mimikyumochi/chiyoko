package lgbt.faith.chiyoko

import lgbt.faith.chiyoko.sequences.Vault
import net.minecraft.core.BlockPos
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import java.util.concurrent.ConcurrentLinkedQueue


class PendingGravelBreak(val pos: Vec3, val fortune: Int) {
    var ticksWaited = 0
    val collectedItems = mutableListOf<ItemStack>()
    var collectingSince = -1
    companion object { const val MAX_TICKS = 40; const val COLLECT_WINDOW = 5; const val RADIUS = 4.0 }
}

class PendingWitherDeath(val pos: Vec3, val looting: Int, val playerKilled: Boolean) {
    var ticksWaited = 0
    val collectedItems = mutableListOf<ItemStack>()
    var collectingSince = -1
    companion object { const val MAX_TICKS = 80; const val COLLECT_WINDOW = 10; const val RADIUS = 5.0 }
}

class PendingFishingReel(val pos: Vec3, val luck: Int, val isOpenWater: Boolean, val isJungle: Boolean) {
    var ticksWaited = 0
    val collectedItems = mutableListOf<ItemStack>()
    var collectingSince = -1
    companion object { const val MAX_TICKS = 60; const val COLLECT_WINDOW = 5; const val RADIUS = 8.0 }
}

class PendingPiglinBarter(val pos: Vec3) {
    var ticksWaited = 0
    val collectedItems = mutableListOf<ItemStack>()
    var collectingSince = -1
    companion object { const val MAX_TICKS = 500; const val COLLECT_WINDOW = 10; const val RADIUS = 8.0 }
}

object DropEventState {
    val pendingGravels  = mutableListOf<PendingGravelBreak>()
    val pendingWithers  = mutableListOf<PendingWitherDeath>()
    val pendingFishing  = mutableListOf<PendingFishingReel>()
    val pendingBarters  = mutableListOf<PendingPiglinBarter>()

    // filled and drained by MinecraftMixin every tick
    val newItemEntities = mutableListOf<Pair<Vec3, ItemStack>>()
    val knownItemEntityIds = mutableSetOf<Int>()

    // so LivingEntityMixin can return if not player killed
    val recentlyAttackedWithers = mutableSetOf<Int>()

    // so LevelMixin can return if not broken by local player
    val selfBrokenBlocks: MutableMap<BlockPos, Int> = mutableMapOf()
}

// unchanged vault stuff - if it isnt broke, dont fix it
data class PendingVault(
    val pos: BlockPos,
    val predictedItems: List<ItemStack>,
    val vault: Vault,
    val ticksWaited: Int = 0,
)
object VaultInteractionState {
    val pendingVaults: ConcurrentLinkedQueue<PendingVault> = ConcurrentLinkedQueue()
}
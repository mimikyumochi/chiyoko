package lgbt.faith.chiyoko.mixin

import lgbt.faith.chiyoko.*
import lgbt.faith.chiyoko.functions.EnchantFunctions
import lgbt.faith.chiyoko.functions.Enchantment
import lgbt.faith.chiyoko.rand.Xoroshiro128PlusPlus
import lgbt.faith.chiyoko.sequences.*
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.monster.piglin.Piglin
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.VaultBlock
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity
import net.minecraft.world.level.block.entity.vault.VaultState
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

private const val MAX_CATCH_HISTORY = 1

@Mixin(Minecraft::class)
class MinecraftMixin {

    // tracks each piglins previous gold-holding state to detect the transition
    private val piglinGoldState = mutableMapOf<Int, Boolean>()


    private val recentCatches = ArrayDeque<ItemStack>(MAX_CATCH_HISTORY)

    private inline fun <T> nearestEligible(
        list: List<T>,
        radius: Double,
        itemPos: net.minecraft.world.phys.Vec3,
        isEligible: (T) -> Boolean,
        posOf: (T) -> net.minecraft.world.phys.Vec3,
    ): T? {
        var best: T? = null
        var bestDist = radius
        for (candidate in list) {
            if (!isEligible(candidate)) continue
            val d = posOf(candidate).distanceTo(itemPos)
            if (d <= bestDist) {
                best = candidate
                bestDist = d
            }
        }
        return best
    }

    @Inject(method = ["tick"], at = [At("HEAD")])
    private fun onTick(ci: CallbackInfo) {
        val mc = Minecraft.getInstance()
        val level = mc.level ?: return

//        val player = mc.player
//        if (player != null) {
//            EnchantFunctions.logRegistryOrderForHeldItem()
//        }

        processVaults(mc, level)
        scanEntities(level)
        routeNewItemEntities(level)
        processGravels()
        processWithers()
        processFishing()
        processBarters()
        ageSelfBrokenBlocks()
    }

    // vaults
    private fun ageSelfBrokenBlocks() {
        val iter = DropEventState.selfBrokenBlocks.entries.iterator()
        while (iter.hasNext()) {
            val entry = iter.next()
            val newAge = entry.value + 1
            if (newAge > 2) iter.remove() else entry.setValue(newAge)
        }
    }

    private fun processVaults(mc: Minecraft, level: ClientLevel) {
        if (VaultInteractionState.pendingVaults.isEmpty()) return
        val snapshot = VaultInteractionState.pendingVaults.toList()
        VaultInteractionState.pendingVaults.clear()

        for (pending in snapshot) {
            val waited = pending.ticksWaited + 1
            val blockState = level.getBlockState(pending.pos)
            val currentState = blockState.getValue(VaultBlock.STATE)
            val isOminous = blockState.getValue(VaultBlock.OMINOUS)

            if (currentState == VaultState.EJECTING) {
                val blockEntity = level.getBlockEntity(pending.pos) as? VaultBlockEntity
                val displayItem = blockEntity?.sharedData?.displayItem ?: ItemStack.EMPTY

                if (!pending.vault.lootTable.any { it.first.item == displayItem.item }) return

                if (!displayItem.isEmpty &&
                    (pending.predictedItems.lastOrNull()?.item != displayItem.item ||
                            pending.predictedItems.lastOrNull()?.count != displayItem.count) &&
                    isMatchingSeed()
                ) {
                    handleVaultDesync(displayItem, isOminous)
                }
            } else if (waited < 200) {
                VaultInteractionState.pendingVaults.add(pending.copy(ticksWaited = waited))
            }
        }
    }

    // piglin - detect when picks up gold ingot

    // piglin gold pickup + new item entity discovery, single pass

    private fun scanEntities(level: ClientLevel) {
        val livePiglinIds = mutableSetOf<Int>()
        val trackItems = DropEventState.pendingGravels.isNotEmpty() ||
                DropEventState.pendingFishing.isNotEmpty() ||
                DropEventState.pendingWithers.isNotEmpty() ||
                DropEventState.pendingBarters.isNotEmpty()

        for (entity in level.entitiesForRendering()) {
            when {
                entity is Piglin -> {
                    livePiglinIds.add(entity.id)
                    val holdingGold = entity.offhandItem.`is`(Items.GOLD_INGOT)
                    val wasHolding = piglinGoldState[entity.id] ?: false

                    if (!wasHolding && holdingGold) {
                        DropEventState.pendingBarters.add(PendingPiglinBarter(entity.id))
                    }
                    piglinGoldState[entity.id] = holdingGold
                }
                trackItems && entity is net.minecraft.world.entity.item.ItemEntity && !entity.item.isEmpty -> {
                    if (DropEventState.knownItemEntityIds.add(entity.id)) {
                        DropEventState.newItemEntities.add(entity.position() to entity.item.copy())
                    }
                }
            }
        }
        piglinGoldState.keys.retainAll(livePiglinIds)

        if (!trackItems) {
            DropEventState.knownItemEntityIds.clear()
            DropEventState.newItemEntities.clear()
        }
    }

    // route newly arrived item entities to the nearest pending event

    private fun routeNewItemEntities(level: ClientLevel) {
        if (DropEventState.newItemEntities.isEmpty()) return

        DropEventState.knownItemEntityIds.retainAll { level.getEntity(it) != null }

        val newItems = DropEventState.newItemEntities.toList()
        DropEventState.newItemEntities.clear()

        for ((itemPos, itemStack) in newItems) {
            // gravel and fishing each drop exactly 1 item, so fill those first.
            val gravel = nearestEligible(
                DropEventState.pendingGravels, PendingGravelBreak.RADIUS, itemPos,
                isEligible = { it.collectedItems.isEmpty() },
                posOf = { it.pos },
            )

            if (gravel != null) {
                gravel.collectedItems.add(itemStack)
                if (gravel.collectingSince == -1) gravel.collectingSince = 0
                continue
            }

            val fishing = nearestEligible(
                DropEventState.pendingFishing, PendingFishingReel.RADIUS, itemPos,
                isEligible = { it.collectedItems.isEmpty() },
                posOf = { it.pos },
            )

            if (fishing != null) {
                fishing.collectedItems.add(itemStack)
                if (fishing.collectingSince == -1) fishing.collectingSince = 0
                continue
            }

            val wither = nearestEligible(
                DropEventState.pendingWithers, PendingWitherDeath.RADIUS, itemPos,
                isEligible = { true },
                posOf = { it.pos },
            )

            if (wither != null) {
                wither.collectedItems.add(itemStack)
                if (wither.collectingSince == -1) wither.collectingSince = 0
                continue
            }

            val barter = nearestEligible(
                DropEventState.pendingBarters, PendingPiglinBarter.RADIUS, itemPos,
                isEligible = { it.collectedItems.isEmpty() && it.ticksWaited >= 115 },
                posOf = { pending ->
                    level.getEntity(pending.piglinId)?.position() ?: net.minecraft.world.phys.Vec3.ZERO
                },
            )

            if (barter != null) {
                barter.collectedItems.add(itemStack)
                if (barter.collectingSince == -1) barter.collectingSince = 0
            }
        }
    }

    // gravel

    private fun processGravels() {
        val iter = DropEventState.pendingGravels.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            if (p.collectingSince >= 0) p.collectingSince++
            p.ticksWaited++

            val ready = p.collectingSince >= PendingGravelBreak.COLLECT_WINDOW
            val expired = p.ticksWaited >= PendingGravelBreak.MAX_TICKS
            if (!ready && !expired) continue

            if (p.collectedItems.isNotEmpty()) resolveGravel(p)
            iter.remove()
        }
    }

    private fun resolveGravel(p: PendingGravelBreak) {
        val gravel = Chiyoko.sequences.map["minecraft:blocks/gravel"] as? Gravel ?: return

        val actual = p.collectedItems.first()
        // avoid potential misroutes which will cause the game to hang as it infinitely writes to the config file for desyncs.
        if (actual.item != Items.GRAVEL && actual.item != Items.FLINT) {
            return
        }

        var predicted = gravel.roll(1, p.fortune)
        gravel.advance(1)
        var desynced = actual.item != predicted.firstOrNull()?.item
        if (!desynced || !isMatchingSeed()) {
            Chiyoko.configManager.updateSequence(Chiyoko.worldName, Chiyoko.seed, gravel.getRngCopy(), gravel.key)
            return
        }

        var advances = 0L
        val maxAdvances = 1000
        while (desynced && advances < maxAdvances) {
            advances++
            predicted = gravel.roll(1, p.fortune)
            gravel.advance(1)
            desynced = actual.item != predicted.firstOrNull()?.item
        }
        Chiyoko.configManager.updateSequence(Chiyoko.worldName, Chiyoko.seed, gravel.getRngCopy(), gravel.key, advances)

        sendOverlay("advanced $advances times to account for desync")
    }

    // wither skeleton

    private fun processWithers() {
        val iter = DropEventState.pendingWithers.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            if (p.collectingSince >= 0) p.collectingSince++
            p.ticksWaited++

            val ready = p.collectingSince >= PendingWitherDeath.COLLECT_WINDOW
            val expired = p.ticksWaited >= PendingWitherDeath.MAX_TICKS
            if (!ready && !expired) continue

            resolveWither(p)
            iter.remove()
        }
    }

    private fun resolveWither(p: PendingWitherDeath) {
        val witherSeq = Chiyoko.sequences.map["minecraft:entities/wither_skeleton"]
                as? lgbt.faith.chiyoko.sequences.WitherSkeleton ?: return

        val predictedDrops = witherSeq.roll(
            lgbt.faith.chiyoko.sequences.WitherSkeleton.RollType.NextDrop,
            p.playerKilled, p.looting
        )
        witherSeq.advance(1, p.playerKilled, p.looting)
        Chiyoko.configManager.updateSequence(Chiyoko.worldName, Chiyoko.seed, witherSeq.getRngCopy(), witherSeq.key)

        val actualDrops = p.collectedItems.filter { it.item != Items.AIR }
        if (matchesPrediction(actualDrops, predictedDrops) || !isMatchingSeed()) return

        val result = findMatchingState(witherSeq, actualDrops, looting = p.looting)
        if (result != null) {
            val (found, advancements) = result
            Chiyoko.configManager.updateSequence(
                Chiyoko.worldName, Chiyoko.seed, found, witherSeq.key, advancements.toLong()
            )
            sendOverlay("advanced $advancements times to account for desync")
        }
    }

    // fishing
    private fun processFishing() {
        val iter = DropEventState.pendingFishing.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            if (p.collectingSince >= 0) p.collectingSince++
            p.ticksWaited++

            val ready = p.collectingSince >= PendingFishingReel.COLLECT_WINDOW
            val expired = p.ticksWaited >= PendingFishingReel.MAX_TICKS
            if (!ready && !expired) continue

            if (p.collectedItems.isNotEmpty()) {
                for (item in p.collectedItems) {
                    if (recentCatches.size >= MAX_CATCH_HISTORY) recentCatches.removeFirst()
                    recentCatches.addLast(item)
                }
                resolveFishing(p)
            }
            iter.remove()
        }
    }
    private fun resolveFishing(p: PendingFishingReel) {
        val fishing = Chiyoko.sequences.map["minecraft:gameplay/fishing"] as? Fishing ?: return

        val actual = p.collectedItems.first()

        // avoid potential misroutes which will cause the game to hang as it infinitely writes to the config file for desyncs.
        val isFishDrop = fishing.fishTable().any { it.item.item == actual.item } ||
                         fishing.junkTable(true).any { it.item.item == actual.item } ||
                         fishing.treasureTable().any { it.item.item == actual.item }

        if (!isFishDrop) return

        var predicted = fishing.roll(1, p.luck, p.isOpenWater, p.isJungle)
        fishing.advance(1, p.luck, p.isOpenWater, p.isJungle)
        Chiyoko.configManager.updateSequence(Chiyoko.worldName, Chiyoko.seed, fishing.getRngCopy(), fishing.key)

        var desynced = actual.item != predicted.first().item


        if (!desynced || !isMatchingSeed()) return

        val catchList = recentCatches.toList() // snapshot the history once

        var advances = 0L
        var rngAdvances = 0L
        val maxAdvances = 1000
        while (desynced && advances < maxAdvances) {
            advances++

            val matched = tryMatchCatchSequence(fishing, catchList, p)
            if (matched != null) {
                rngAdvances += matched
                desynced = false
            } else {
                fishing.advance(1, p.luck, p.isOpenWater, p.isJungle)
                rngAdvances++
            }
        }

        if (rngAdvances > 0) {
            Chiyoko.configManager.updateSequence(Chiyoko.worldName, Chiyoko.seed, fishing.getRngCopy(), fishing.key, rngAdvances)
        }

        sendOverlay("advanced $advances times to account for desync (matched ${catchList.size} items)")
    }

    private fun tryMatchCatchSequence(fishing: Fishing, catchList: List<ItemStack>, p: PendingFishingReel): Int? {
        val snapshot = fishing.getRngCopy()
        for (expected in catchList) {
            val pred = fishing.roll(1, p.luck, p.isOpenWater, p.isJungle)

            if (expected.item != pred.first().item) {
                fishing.loadState(snapshot.seedLo, snapshot.seedHi)
                return null
            }

            fishing.advance(1, p.luck, p.isOpenWater, p.isJungle)
        }
        return catchList.size
    }

    // piglin bartering

    private fun processBarters() {
        val iter = DropEventState.pendingBarters.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.ticksWaited++
            if (p.collectingSince >= 0) p.collectingSince++

            val ready = p.collectingSince >= PendingPiglinBarter.COLLECT_WINDOW
            val expired = p.ticksWaited >= PendingPiglinBarter.MAX_TICKS
            if (!ready && !expired) continue

            if (p.collectedItems.isNotEmpty()) resolveBarter(p)
            iter.remove()
        }
    }

    private fun resolveBarter(p: PendingPiglinBarter) {
        val actual = p.collectedItems.firstOrNull() ?: return
        val barter = Chiyoko.sequences.map["minecraft:gameplay/piglin_bartering"] as? PiglinBartering ?: return

        if (!barter.lootTable.any { it.item.item == actual.item }) return

        var predicted = barter.roll(1)
        barter.advance(1)
        Chiyoko.configManager.updateSequence(Chiyoko.worldName, Chiyoko.seed, barter.getRngCopy(), barter.key)

        var desynced = actual.item != predicted.firstOrNull()?.item
        if (!desynced || !isMatchingSeed()) return

        var advances = 0L
        val maxAdvances = 1000
        while (desynced && advances < maxAdvances) {
            advances++
            predicted = barter.roll(1)
            barter.advance(1)
            desynced = actual.item != predicted.firstOrNull()?.item
        }
        Chiyoko.configManager.updateSequence(Chiyoko.worldName, Chiyoko.seed, barter.getRngCopy(), barter.key, advances)
        sendOverlay("advanced $advances times to account for desync")

    }


    private fun matchesPrediction(actual: List<ItemStack>, predicted: List<ItemStack>): Boolean {
        fun List<ItemStack>.toDropMap() = associate { it.item to it.count }
        return actual.toDropMap() == predicted.toDropMap()
    }

    private fun findMatchingState(
        witherSkeleton: lgbt.faith.chiyoko.sequences.WitherSkeleton,
        actualDrops: List<ItemStack>,
        maxDepth: Int = 50,
        looting: Int,
    ): Pair<Xoroshiro128PlusPlus, Int>? {
        val queue = ArrayDeque<Triple<Xoroshiro128PlusPlus, Int, Int>>()
        val visited = HashSet<Xoroshiro128PlusPlus.State>()
        val startXoro = witherSkeleton.getRngCopy()
        queue.add(Triple(startXoro.copy(), 0, 0))
        visited.add(startXoro.toState())

        while (queue.isNotEmpty()) {
            val (current, depth, advancements) = queue.removeFirst()
            for ((playerKilled, hasLooting) in listOf(false to false, true to false, true to true)) {
                val next = current.copy()
                val predicted = witherSkeleton.nextDrops(next, playerKilled, if (hasLooting) looting else 0)
                val state = next.toState()
                if (state in visited) continue
                visited.add(state)
                if (matchesPrediction(actualDrops, predicted)) return next to (advancements + 1)
                if (depth + 1 < maxDepth) queue.add(Triple(next, depth + 1, advancements + 1))
            }
        }
        return null
    }
}
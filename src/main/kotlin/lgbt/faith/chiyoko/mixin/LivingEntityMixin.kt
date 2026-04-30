package lgbt.faith.chiyoko.mixin

import lgbt.faith.chiyoko.Chiyoko
import lgbt.faith.chiyoko.DropCapture
import lgbt.faith.chiyoko.rand.Xoroshiro128PlusPlus
import net.minecraft.client.Minecraft
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.enchantment.Enchantments
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import kotlin.collections.set


@Mixin(LivingEntity::class)
class LivingEntityMixin {


    fun matchesPrediction(actual: List<ItemStack>, predicted: List<ItemStack>): Boolean {
        fun List<ItemStack>.toDropMap() = associate { it.item to it.count }
        return actual.toDropMap() == predicted.toDropMap()
    }

    @Inject(
        method = ["dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;Z)V"],
        at = [At("HEAD")]
    )
    private fun onDropFromLootTableHead(level: ServerLevel, source: DamageSource, playerKilled: Boolean, ci: CallbackInfo) {
        val entity = this as LivingEntity
        if (entity !is WitherSkeleton) return
        DropCapture.pendingDrops[entity.id] = mutableListOf()
    }

    @Inject(
        method = ["dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;Z)V"],
        at = [At("RETURN")]
    )
    private fun onDropFromLootTableReturn(level: ServerLevel, source: DamageSource, playerKilled: Boolean, ci: CallbackInfo) {
        val mc = Minecraft.getInstance()

        val registries = mc.player?.level()?.registryAccess() ?: return
        val enchantLookup = registries.lookupOrThrow(Registries.ENCHANTMENT)
        val lootingHolder = enchantLookup.getOrThrow(Enchantments.LOOTING)

        val attacker = source.entity
        val looting = if (attacker is LivingEntity) {
            EnchantmentHelper.getEnchantmentLevel(lootingHolder, source.entity as LivingEntity)
        } else {
            0
        }

        val entity = this as LivingEntity

        if (entity is WitherSkeleton) {
            val configManager = Chiyoko.configManager
            val sequences = Chiyoko.sequences.map

            val witherSkeleton = sequences["minecraft:entities/wither_skeleton"] as? lgbt.faith.chiyoko.sequences.WitherSkeleton ?: return
            val predictedDrops = witherSkeleton.roll(lgbt.faith.chiyoko.sequences.WitherSkeleton.RollType.NextDrop, playerKilled, looting)


            witherSkeleton.advance(1, playerKilled, looting)
            val xoroshiro = witherSkeleton.getRngCopy()
            configManager.updateSequence(Chiyoko.worldName, Chiyoko.seed, xoroshiro, witherSkeleton.key)

            val actualDrops = DropCapture.pendingDrops.remove(entity.id)?.filter { it.item != Items.AIR } ?: return

            if(!matchesPrediction(actualDrops, predictedDrops)) {
                val result = findMatchingState(witherSkeleton, actualDrops, looting=looting)
                if (result != null) {
                    val (found, advancements) = result
                    configManager.updateSequence(Chiyoko.worldName, Chiyoko.seed, found, witherSkeleton.key)
                    configManager.advanceSequence(Chiyoko.worldName, witherSkeleton.key, advancements.toLong())

                    val mc = Minecraft.getInstance()
                    mc.execute {
                        mc.player?.sendOverlayMessage(
                            Component.literal("advanced $advancements times to account for desync")
                        )
                    }
                }
            }
        }
    }
    fun findMatchingState(witherSkeleton: lgbt.faith.chiyoko.sequences.WitherSkeleton, actualDrops: List<ItemStack>, maxDepth: Int = 50, looting: Int): Pair<Xoroshiro128PlusPlus, Int>? {
        val queue = ArrayDeque<Triple<Xoroshiro128PlusPlus, Int, Int>>()
        val visited = HashSet<Xoroshiro128PlusPlus.State>()

        val startXoro = witherSkeleton.getRngCopy()
        queue.add(Triple(startXoro.copy(), 0, 0))
        visited.add(startXoro.toState())

        while (queue.isNotEmpty()) {
            val (current, depth, advancements) = queue.removeFirst()

            for ((playerKilled, hasLooting) in listOf(
                false to false,
                true  to false,
                true  to true,
            )) {
                val next = current.copy()
                val effectiveLooting = if (hasLooting) looting else 0
                val predicted = witherSkeleton.nextDrops(next, playerKilled, effectiveLooting)

                val state = next.toState()
                if (state in visited) continue
                visited.add(state)

                if (matchesPrediction(actualDrops, predicted)) return next to (advancements + 1)
                if (depth + 1 < maxDepth) {
                    queue.add(Triple(next, depth + 1, advancements + 1))
                }
            }
        }
        return null
    }
}
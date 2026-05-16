package lgbt.faith.chiyoko.mixin

import lgbt.faith.chiyoko.Chiyoko
import lgbt.faith.chiyoko.isMatchingSeed
import lgbt.faith.chiyoko.sequences.Fishing
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.tags.BiomeTags
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.projectile.FishingHook
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.Redirect
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(FishingHook::class)
class FishingHookMixin {

    @Shadow private var luck: Int = 0
    @Shadow private var nibble: Int = 0

    private val capturedFishingDrops = mutableListOf<ItemStack>()

    @Inject(
        method = ["retrieve"],
        at = [At(value = "HEAD")]
    )
    private fun onRetrieveHead(rod: ItemStack, cir: CallbackInfoReturnable<Int>) {
        capturedFishingDrops.clear()
    }

    @Redirect(
        method = ["retrieve"],
        at = At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z",
            ordinal = 0  // first addFreshEntity call is the item, second is the orb
        )
    )
    private fun captureAndSpawnItem(level: Level, entity: Entity): Boolean {
        if (entity is ItemEntity) {
            capturedFishingDrops.add(entity.item.copy())
        }
        return level.addFreshEntity(entity)
    }

    @Inject(method = ["retrieve"], at = [At(value = "RETURN")])
    private fun onRetrieve(rod: ItemStack, cir: CallbackInfoReturnable<Int>) {
        val hook = this as FishingHook
        if (hook.playerOwner == null) return
        if (nibble <= 0) return
        if (capturedFishingDrops.isEmpty()) return

        val actualDrops = capturedFishingDrops.toList()
        capturedFishingDrops.clear()

        val level = hook.level()
        val pos = hook.blockPosition()
        val isOpenWater = hook.isOpenWaterFishing
        val isJungle = level.getBiome(pos).`is`(BiomeTags.IS_JUNGLE)


        val configManager = Chiyoko.configManager
        val sequences = Chiyoko.sequences.map

        val fishing = sequences["minecraft:gameplay/fishing"] as? Fishing ?: return
        var predictedRoll = fishing.roll(1, luck, isOpenWater, isJungle)

        fishing.advance(1, luck, isOpenWater, isJungle)
        val xoroshiro = fishing.getRngCopy()
        configManager.updateSequence(Chiyoko.worldName, Chiyoko.seed, xoroshiro, fishing.key)

        var isDesynced = actualDrops.firstOrNull()?.item != predictedRoll.first().item

        if (isDesynced && isMatchingSeed()) {
            var advancements = 0
            while (isDesynced) {
                advancements++
                predictedRoll = fishing.roll(1, luck, isOpenWater, isJungle)
                fishing.advance(1, luck, isOpenWater, isJungle)
                val xoroshiro = fishing.getRngCopy()
                configManager.updateSequence(Chiyoko.worldName, Chiyoko.seed, xoroshiro, fishing.key)

                isDesynced = actualDrops.firstOrNull()?.item != predictedRoll.first().item
            }
            val message = Component.literal("advanced $advancements times to account for desync")
            Minecraft.getInstance().execute {
                Minecraft.getInstance().player?.sendOverlayMessage(message)
            }
        }
    }
}
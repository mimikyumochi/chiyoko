package lgbt.faith.chiyoko.mixin

import lgbt.faith.chiyoko.Chiyoko
import lgbt.faith.chiyoko.isMatchingSeed
import lgbt.faith.chiyoko.sequences.PiglinBartering
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.monster.piglin.Piglin
import net.minecraft.world.entity.monster.piglin.PiglinAi
import net.minecraft.world.item.ItemStack
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(PiglinAi::class)
class PiglinAiMixin {
    private companion object {
        private fun matchesPrediction(actual: List<ItemStack>, predicted: List<ItemStack>): Boolean {
            return actual.first().item == predicted.first().item
        }

        @JvmStatic
        @Inject(
            method = ["getBarterResponseItems"],
            at = [At("RETURN")]
        )
        private fun onGetBarterResponse(
            piglin: Piglin,
            cir: CallbackInfoReturnable<List<ItemStack>>
        ) {
            val actualDrops = cir.returnValue

            val configManager = Chiyoko.configManager
            val sequences = Chiyoko.sequences.map

            val barter = sequences["minecraft:gameplay/piglin_bartering"] as? PiglinBartering ?: return

            var predictedRoll = barter.roll(1)

            barter.advance(1)
            val xoroshiro = barter.getRngCopy()
            configManager.updateSequence(Chiyoko.worldName, Chiyoko.seed, xoroshiro, barter.key)

            var isDesynced = !matchesPrediction(actualDrops, predictedRoll)

            if (isDesynced && isMatchingSeed()) {
                var advancements = 0
                while (isDesynced) {
                    advancements++
                    predictedRoll = barter.roll(1)

                    barter.advance(1)
                    val xoroshiro = barter.getRngCopy()
                    configManager.updateSequence(Chiyoko.worldName, Chiyoko.seed, xoroshiro, barter.key)

                    isDesynced = !matchesPrediction(actualDrops, predictedRoll)
                }

                val message = Component.literal("advanced $advancements times to account for desync")

                Minecraft.getInstance().execute {
                    Minecraft.getInstance().player?.sendOverlayMessage(message)
                }
            }
        }
    }
}

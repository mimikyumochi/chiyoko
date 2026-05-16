package lgbt.faith.chiyoko.mixin

import lgbt.faith.chiyoko.Chiyoko
import lgbt.faith.chiyoko.isMatchingSeed
import lgbt.faith.chiyoko.sequences.Gravel
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemInstance
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(Block::class)
class BlockMixin {

    private companion object {

        @JvmStatic
        @Inject(
            method = ["getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemInstance;)Ljava/util/List;"],
            at = [At("RETURN")]
        )
        private fun onGetDrops(
            state: BlockState,
            level: ServerLevel,
            pos: BlockPos,
            blockEntity: BlockEntity?,
            entity: Entity?,
            tool: ItemInstance,
            cir: CallbackInfoReturnable<List<ItemStack>>
        ) {
            if (state.block != Blocks.GRAVEL) return

            val actualDrops = cir.returnValue
            if (actualDrops.isEmpty()) return

            val registries = level.registryAccess()
            val enchantLookup = registries.lookupOrThrow(Registries.ENCHANTMENT)

            val fortune =
                EnchantmentHelper.getItemEnchantmentLevel(enchantLookup.getOrThrow(Enchantments.FORTUNE), tool)
            val silk =
                EnchantmentHelper.getItemEnchantmentLevel(enchantLookup.getOrThrow(Enchantments.SILK_TOUCH), tool)
            if (silk > 0) return

            val configManager = Chiyoko.configManager
            val gravel = Chiyoko.sequences.map["minecraft:blocks/gravel"] as? Gravel ?: return

            var predicted = gravel.roll(1, fortune)
            gravel.advance(1)

            val xoroshiro = gravel.getRngCopy()
            configManager.updateSequence(Chiyoko.worldName, Chiyoko.seed, xoroshiro, gravel.key)


            var desynced = actualDrops.first().item != predicted.first().item

            if (desynced && isMatchingSeed()) {
                var advances = 0
                while (desynced) {
                    advances++
                    predicted = gravel.roll(1, fortune)
                    gravel.advance(1)

                    val xoroshiro = gravel.getRngCopy()
                    configManager.updateSequence(Chiyoko.worldName, Chiyoko.seed, xoroshiro, gravel.key)


                    desynced = actualDrops.first().item != predicted.first().item
                }

                Minecraft.getInstance().player?.sendOverlayMessage(
                    Component.literal("advanced $advances times to account for desync")
                )
            }
        }
    }
}
package lgbt.faith.chiyoko.mixin

import lgbt.faith.chiyoko.VaultInteractionState
import lgbt.faith.chiyoko.handleVaultDesync
import lgbt.faith.chiyoko.isMatchingSeed
import net.minecraft.client.Minecraft
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.VaultBlock
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity
import net.minecraft.world.level.block.entity.vault.VaultState
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(Minecraft::class)
class MinecraftMixin {

    @Inject(method = ["tick"], at =  [At("HEAD")],)
    private fun onTick(ci: CallbackInfo) {
        val mc = Minecraft.getInstance()
        val level = mc.level ?: return

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

                if (!displayItem.isEmpty && !pending.predictedItems.any { it.item == displayItem.item } && isMatchingSeed()) {
                    handleVaultDesync(displayItem, isOminous)
                }
            }
            else if (waited < 200) VaultInteractionState.pendingVaults.add(pending.copy(ticksWaited = waited))
        }

    }
}
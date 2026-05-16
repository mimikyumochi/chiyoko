package lgbt.faith.chiyoko.mixin

import lgbt.faith.chiyoko.Chiyoko
import lgbt.faith.chiyoko.PendingVault
import lgbt.faith.chiyoko.VaultInteractionState
import lgbt.faith.chiyoko.sequences.Vault
import net.minecraft.client.multiplayer.MultiPlayerGameMode
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.VaultBlock
import net.minecraft.world.level.block.entity.vault.VaultState
import net.minecraft.world.phys.BlockHitResult
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable


@Mixin(MultiPlayerGameMode::class)
class MultiPlayerGameModeMixin {

    @Inject(
        method = ["useItemOn"],
        at = [At("HEAD")],
    )
    private fun onUseItemOn(
        player: LocalPlayer,
        hand: InteractionHand,
        hitResult: BlockHitResult,
        ci: CallbackInfoReturnable<InteractionResult>,
    ) {
        val level = player.level()
        val pos = hitResult.blockPos
        val blockState = level.getBlockState(pos)

        if (!blockState.`is`(Blocks.VAULT)) return
        if (player.isCrouching) return

        val isOminous = blockState.getValue(VaultBlock.OMINOUS)
        val expectedKey = if (isOminous) Items.OMINOUS_TRIAL_KEY else Items.TRIAL_KEY

        val heldItem = player.getItemInHand(hand)
        if (!heldItem.`is`(expectedKey)) return


        val vaultState = blockState.getValue(VaultBlock.STATE)
        if (vaultState != VaultState.ACTIVE) return

        val sequences = Chiyoko.sequences.map
        val vault = if (isOminous) sequences["minecraft:chests/trial_chambers/reward_ominous"] as? Vault ?: return
                    else           sequences["minecraft:chests/trial_chambers/reward"] as? Vault ?: return

        val predictedItems = vault.roll(1)
        vault.advance(1)

        val xoroshiro = vault.getRngCopy()
        Chiyoko.configManager.updateSequence(Chiyoko.worldName, Chiyoko.seed, xoroshiro, vault.key)

        VaultInteractionState.pendingVaults.add(PendingVault(pos.immutable(), predictedItems, vault))

    }
}


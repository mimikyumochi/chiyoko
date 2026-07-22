package lgbt.faith.chiyoko.mixin

import lgbt.faith.chiyoko.*
import lgbt.faith.chiyoko.sequences.Vault
import net.minecraft.client.multiplayer.MultiPlayerGameMode
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.tags.BiomeTags
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Items
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.VaultBlock
import net.minecraft.world.level.block.entity.vault.VaultState
import net.minecraft.world.phys.BlockHitResult
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(MultiPlayerGameMode::class)
class MultiPlayerGameModeMixin {


    // vault - detect opening vaults
    @Inject(method = ["destroyBlock"], at = [At("HEAD")])
    private fun onDestroyBlock(pos: BlockPos, ci: CallbackInfoReturnable<Boolean>) {
        DropEventState.selfBrokenBlocks[pos.immutable()] = 0
    }

    @Inject(method = ["useItemOn"], at = [At("HEAD")])
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
        if (!player.getItemInHand(hand).`is`(expectedKey)) return
        if (blockState.getValue(VaultBlock.STATE) != VaultState.ACTIVE) return

        val vault = if (isOminous) Chiyoko.sequences.map["minecraft:chests/trial_chambers/reward_ominous"] as? Vault ?: return
        else           Chiyoko.sequences.map["minecraft:chests/trial_chambers/reward"] as? Vault ?: return

        val predictedItems = vault.peek(1)
        vault.advance(1)
        Chiyoko.configManager.updateSequence(Chiyoko.worldName, Chiyoko.seed, vault.getRngCopy(), vault.key)
        VaultInteractionState.pendingVaults.add(PendingVault(pos.immutable(), predictedItems, vault))
    }

    // fishing - detect reel in client side
    @Inject(method = ["useItem"], at = [At("HEAD")])
    private fun onUseItem(
        player: Player,
        hand: InteractionHand,
        ci: CallbackInfoReturnable<InteractionResult>,
    ) {
        val level = player.level()
        if (!level.isClientSide) return

        val rod = player.getItemInHand(hand)
        if (!rod.`is`(Items.FISHING_ROD)) return
        val hook = player.fishing ?: return

        // biting is synced to the client via SynchedEntityData
        if (!(hook as FishingHookAccessor).biting()) return
        val pos = hook.blockPosition()

        val enchantRegistry = level.registryAccess().lookup(Registries.ENCHANTMENT).orElse(null) ?: return

        val luckOfTheSea = enchantRegistry.get(Enchantments.LUCK_OF_THE_SEA)
            .map { EnchantmentHelper.getItemEnchantmentLevel(it, rod) }
            .orElse(0) ?: return

        val luck = player.getAttributeValue(Attributes.LUCK).toInt() + luckOfTheSea
        val isOpenWater = (hook as FishingHookAccessor).isOpenWater()
        val isJungle = level.getBiome(pos).`is`(BiomeTags.IS_JUNGLE)

        DropEventState.pendingFishing.add(
            PendingFishingReel(hook.position(), luck, isOpenWater, isJungle)
        )
    }

    // track which wither skeletons the player has hit

    @Inject(method = ["attack"], at = [At("HEAD")])
    private fun onAttack(player: Player, target: Entity, ci: CallbackInfo) {
        if (target is WitherSkeleton) {
            DropEventState.recentlyAttackedWithers.add(target.id)
        }
    }
}
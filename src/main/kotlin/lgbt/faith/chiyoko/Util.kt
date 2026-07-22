package lgbt.faith.chiyoko

import com.mojang.brigadier.context.CommandContext
import com.mojang.serialization.Codec
import lgbt.faith.chiyoko.mixin.BiomeManagerAccessor
import lgbt.faith.chiyoko.sequences.Vault
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.Minecraft
import net.minecraft.core.component.DataComponentType
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentUtils
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.biome.BiomeManager

object ChiyokoComponents {
    val VARIANT: DataComponentType<Int> = DataComponentType.builder<Int>()
        .persistent(Codec.INT)
        .build()
}

fun isMatchingSeed(): Boolean {
    val mc = Minecraft.getInstance()
    val level = mc.level ?: return false

    val worldSeed = mc.singleplayerServer?.worldGenSettings?.options()?.seed()
    val worldHash = (level.biomeManager as BiomeManagerAccessor).biomeZoomSeed

    return worldSeed == Chiyoko.seed || worldHash == BiomeManager.obfuscateSeed(Chiyoko.seed)
}

fun validateSeed(context: CommandContext<FabricClientCommandSource?>?): Int {
    if (context == null) return 0

    val source = context.source ?: return 0

    val seedText = ComponentUtils.copyOnClickText(Chiyoko.seed.toString())

    if (isMatchingSeed()) {
        source.sendFeedback(
            Component.literal("§a✔§f ")
                .append(seedText)
                .append(" is the correct world seed")
        )
        return 1
    } else {
        source.sendFeedback(
            Component.literal("§c✘§f ")
                .append(seedText)
                .append(" is not the correct world seed")
        )
        return 0
    }
}

fun sendOverlay(text: String) {
    val mc = Minecraft.getInstance()
    mc.execute { mc.player?.sendOverlayMessage(Component.literal(text)) }
}

fun handleVaultDesync(actual: ItemStack, isOminous: Boolean) {

    val sequences = Chiyoko.sequences.map
    val vault = if (isOminous) sequences["minecraft:chests/trial_chambers/reward_ominous"] as? Vault ?: return
    else           sequences["minecraft:chests/trial_chambers/reward"] as? Vault ?: return

    var advances = 0L
    val maxAdvances = 1000
    do {
        val predicted = vault.peek(1)
        vault.advance(1)
        advances++
    } while((predicted.lastOrNull()?.item != actual.item ||
            predicted.lastOrNull()?.count != actual.count) && advances < maxAdvances)

    if (advances > 0) {
        Chiyoko.configManager.updateSequence(Chiyoko.worldName, Chiyoko.seed, vault.getRngCopy(), vault.key, advances)
        sendOverlay("advanced $advances times to account for desync")
    }
}
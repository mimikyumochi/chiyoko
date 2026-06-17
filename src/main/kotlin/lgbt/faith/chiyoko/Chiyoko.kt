package lgbt.faith.chiyoko

import lgbt.faith.chiyoko.config.ChiyokoConfigManager
import lgbt.faith.chiyoko.rand.RandomSupport
import lgbt.faith.chiyoko.sequences.*
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.toasts.SystemToast
import net.minecraft.network.chat.Component
import net.minecraft.world.level.storage.LevelResource
import kotlin.io.path.name


val keys = listOf(
    "minecraft:chests/trial_chambers/reward_ominous",
    "minecraft:chests/trial_chambers/reward",
    "minecraft:gameplay/piglin_bartering",
    "minecraft:gameplay/fishing",
    "minecraft:entities/wither_skeleton",
    "minecraft:blocks/gravel"
)
data class Sequences(
    val map: MutableMap<String, lgbt.faith.chiyoko.sequences.Sequence> = mutableMapOf()
)
private fun createSequence(key: String): lgbt.faith.chiyoko.sequences.Sequence? {
    return when (key) {
        "minecraft:chests/trial_chambers/reward_ominous" -> Vault(true)
        "minecraft:chests/trial_chambers/reward" -> Vault(false)
        "minecraft:gameplay/piglin_bartering" -> PiglinBartering()
        "minecraft:gameplay/fishing" -> Fishing()
        "minecraft:entities/wither_skeleton" -> WitherSkeleton()
        "minecraft:blocks/gravel" -> Gravel()
        else -> null
    }
}

class Chiyoko : ClientModInitializer {
    companion object {
        val mc = Minecraft.getInstance()

        var loaded = false
        var seed: Long = 0
        var worldName: String = ""
        lateinit var configManager: ChiyokoConfigManager

        val sequences = Sequences()

        fun changeWorldSeed() {
            val worldData = configManager.config.worlds[worldName] ?: return

            for ((key, sequence) in sequences.map) {

                val seqData = worldData.sequences[key] ?: continue
                val advances = seqData.advances.toInt()

                val rng = RandomSupport().createSequence(seed, key)
                rng.advance(advances)

                sequence.loadState(rng.seedLo, rng.seedHi)

                configManager.updateSequence(worldName, seed, rng, key, 0)
            }
        }
    }


    override fun onInitializeClient() {
        ChiyokoComponents // force component to register

        configManager = ChiyokoConfigManager()
        configManager.load()

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, registryAccess  ->
            dispatcher.register(
                ClientCommands.literal("validateseed")
                    .executes { context ->
                        validateSeed(context)
                    }
            )
        }

        ClientTickEvents.END_CLIENT_TICK.register { client ->

            if (configManager.wasReset) {
                SystemToast.add(
                    client.toastManager,
                    SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                    Component.literal("chiyoko config reset"),
                    Component.literal("bug that affects advancements was found, world config purged.")
                )
                configManager.wasReset = false
            }

            val player = mc.player
            if (player == null) {
                loaded = false
                return@register
            }
            if (loaded) return@register
            loaded = true
            
            var s = 0L
            var w = ""
            if (mc.currentServer == null) {
                s = mc.singleplayerServer!!.worldGenSettings.options().seed()
                w = mc.singleplayerServer!!.getWorldPath(LevelResource.ROOT).parent.name
            }
            else {
                w = mc.currentServer!!.ip
                s = configManager.config.worlds[w]?.worldSeed ?: 0
            }

            seed = s
            worldName = w
            val worldData = configManager.config.worlds[worldName]

            for (key in keys) {

                val sequence = createSequence(key) ?: continue
                sequences.map[key] = sequence

                val saved = worldData?.sequences?.get(key)

                if (saved != null) {
                    sequence.loadState(saved.seedLo, saved.seedHi)
                } else {
                    sequence.init(seed)
                    configManager.addSequence(worldName, seed, sequence.getRngCopy(), key)
                }
            }
        }
    }
}

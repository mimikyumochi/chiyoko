package lgbt.faith.chiyoko

import lgbt.faith.chiyoko.config.ChiyokoConfigManager
import lgbt.faith.chiyoko.rand.RandomSupport
import lgbt.faith.chiyoko.sequences.Fishing
import lgbt.faith.chiyoko.sequences.Gravel
import lgbt.faith.chiyoko.sequences.PiglinBartering
import lgbt.faith.chiyoko.sequences.WitherSkeleton
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.storage.LevelResource
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.io.path.name

object DropCapture {
    val pendingDrops = mutableMapOf<Int, MutableList<ItemStack>>()
}

val keys = listOf(
    "minecraft:gameplay/piglin_bartering",
    "minecraft:entities/wither_skeleton",
    "minecraft:gameplay/fishing",
    "minecraft:blocks/gravel"
)
data class Sequences(
    val map: MutableMap<String, lgbt.faith.chiyoko.sequences.Sequence> = mutableMapOf()
)
private fun createSequence(key: String): lgbt.faith.chiyoko.sequences.Sequence? {
    return when (key) {
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

                configManager.updateSequence(worldName, seed, rng, key)
            }
        }
    }


    override fun onInitializeClient() {
        configManager = ChiyokoConfigManager()
        configManager.load()

        ClientTickEvents.END_CLIENT_TICK.register {

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

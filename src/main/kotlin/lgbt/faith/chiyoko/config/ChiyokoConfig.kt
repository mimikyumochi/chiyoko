package lgbt.faith.chiyoko.config

import com.google.gson.GsonBuilder
import lgbt.faith.chiyoko.rand.Xoroshiro128PlusPlus
import lgbt.faith.chiyoko.sequences.WitherSkeleton
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files

enum class OverlayRotation { HORIZONTAL, VERTICAL }

data class OverlayConfig(
    var visible: Boolean = true,
    var rotation: OverlayRotation = OverlayRotation.VERTICAL,
    var reversed: Boolean = false,
    var advances: Int = 1,
    var rollType: WitherSkeleton.RollType = WitherSkeleton.RollType.KillsUntilSkull,
    var split: Boolean = false
)
data class SequenceData(
    var seedLo: Long,
    var seedHi: Long,
    var advances: Long = 0,
)
data class WorldData(
    var worldSeed: Long,
    var sequences: MutableMap<String, SequenceData> = mutableMapOf()
)
data class GridPosition(
    var gridX: Int = 0,
    var gridY: Int = 0,
)
data class ChiyokoConfig(
    var worlds: MutableMap<String, WorldData> = mutableMapOf(),
    var hudSlots: MutableMap<String, GridPosition> = mutableMapOf(),
    var overlays: MutableMap<String, OverlayConfig> = mutableMapOf()
) {
    fun getSlotPosition(key: String, index: Int): GridPosition {
        return hudSlots.getOrPut(key) { GridPosition(index, 0) }
    }
    fun getOverlay(sequenceName: String): OverlayConfig {
        return overlays.getOrPut(sequenceName) { OverlayConfig() }
    }

    fun updateOverlay(sequenceName: String, update: OverlayConfig.() -> Unit) {
        getOverlay(sequenceName).update()
    }
}
class ChiyokoConfigManager {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val configPath = FabricLoader.getInstance().configDir.resolve("chiyoko.json")

    var config = ChiyokoConfig()

    fun load() {
        if (!Files.exists(configPath)) {
            save()
            return
        }
        runCatching {
            Files.newBufferedReader(configPath).use { reader ->
                gson.fromJson(reader, ChiyokoConfig::class.java)
            }
        }.onSuccess { loaded ->
            config = loaded ?: ChiyokoConfig()
            @Suppress("SENSELESS_COMPARISON")
            if (config.worlds == null) config.worlds = mutableMapOf()
            @Suppress("SENSELESS_COMPARISON")
            if (config.hudSlots == null) config.hudSlots = mutableMapOf()
            @Suppress("SENSELESS_COMPARISON")
            if (config.overlays == null) config.overlays = mutableMapOf()

            config.overlays.forEach { (name, overlay) ->
                @Suppress("SENSELESS_COMPARISON")
                if (overlay == null) config.overlays[name] = OverlayConfig()
            }
        }.onFailure {
            config = ChiyokoConfig()
            save()
        }
    }
    fun save() {
        Files.createDirectories(configPath.parent)
        Files.newBufferedWriter(configPath).use { writer ->
            gson.toJson(config, writer)
        }
    }

    fun addSequence(worldName: String, worldSeed: Long, xoroshiro: Xoroshiro128PlusPlus,  sequenceName: String) {
        val world = config.worlds.getOrPut(worldName) { WorldData(worldSeed) }

        if (!world.sequences.containsKey(sequenceName)) {
            world.sequences[sequenceName] = SequenceData(xoroshiro.seedLo, xoroshiro.seedHi)
            save()
        }
    }
    fun updateSequence(
        worldName: String,
        worldSeed: Long,
        xoroshiro: Xoroshiro128PlusPlus,
        sequenceName: String,
        advanceBy: Long = 1
    ) {
        val world = config.worlds.getOrPut(worldName) { WorldData(worldSeed) }

        world.worldSeed = worldSeed

        val existing = world.sequences[sequenceName]

        world.sequences[sequenceName] = SequenceData(
            seedLo = xoroshiro.seedLo,
            seedHi = xoroshiro.seedHi,
            advances = (existing?.advances ?: 0) + advanceBy
        )

        save()
    }

}
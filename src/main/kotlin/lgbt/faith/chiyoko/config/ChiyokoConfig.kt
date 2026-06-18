package lgbt.faith.chiyoko.config

import com.google.gson.GsonBuilder
import lgbt.faith.chiyoko.Chiyoko
import lgbt.faith.chiyoko.rand.Xoroshiro128PlusPlus
import lgbt.faith.chiyoko.sequences.WitherSkeleton
import net.fabricmc.loader.api.FabricLoader
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.reader
import kotlin.io.path.writer

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

// DTO for safe nullable deserialization
data class ChiyokoConfigDisk(
    var version: Int? = null,
    var worlds: MutableMap<String, WorldData>? = null,
    var hudSlots: MutableMap<String, GridPosition>? = null,
    var overlays: MutableMap<String, OverlayConfig>? = null
)

data class ChiyokoConfig(
    var version: Int = ChiyokoConfigManager.CURRENT_CONFIG_VERSION,
    var worlds: MutableMap<String, WorldData> = mutableMapOf(),
    var hudSlots: MutableMap<String, GridPosition> = mutableMapOf(),
    var overlays: MutableMap<String, OverlayConfig> = mutableMapOf()
) {
    fun getSlotPosition(key: String, index: Int): GridPosition {
        val position = hudSlots.getOrPut(key) { GridPosition(index, 0) }
        Chiyoko.configManager.save()
        return position
    }

    fun getOverlay(sequenceName: String): OverlayConfig {
        val overlay = overlays.getOrPut(sequenceName) { OverlayConfig() }
        Chiyoko.configManager.save()
        return overlay
    }


    fun updateOverlay(sequenceName: String, update: OverlayConfig.() -> Unit) {
        getOverlay(sequenceName).apply(update)
        Chiyoko.configManager.save()
    }
}

class ChiyokoConfigManager {
    companion object {
        const val CURRENT_CONFIG_VERSION = 1
    }

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val configPath = FabricLoader.getInstance().configDir.resolve("chiyoko.json")

    var config = ChiyokoConfig()
    var wasReset = false

    private fun migrate(raw: ChiyokoConfigDisk): ChiyokoConfig {
        val isOutdated = (raw.version ?: 0) < CURRENT_CONFIG_VERSION
        wasReset = isOutdated

        return ChiyokoConfig(
            version = CURRENT_CONFIG_VERSION,
            worlds = if (isOutdated) mutableMapOf() else raw.worlds ?: mutableMapOf(),
            hudSlots = raw.hudSlots ?: mutableMapOf(),
            overlays = raw.overlays ?: mutableMapOf()
        )
    }

    fun load() {
        if (!configPath.exists()) {
            save()
            return
        }

        val loadedConfig = runCatching {
            configPath.reader().use { gson.fromJson(it, ChiyokoConfigDisk::class.java) }
        }.getOrNull()

        if (loadedConfig != null) {
            println("LOADED VERSION!!!! ${loadedConfig.version}")
            config = migrate(loadedConfig)
        } else {
            config = ChiyokoConfig()
        }

        save()
    }

    fun save() {
        configPath.parent.createDirectories()
        configPath.writer().use { gson.toJson(config, it) }
    }

    fun addSequence(worldName: String, worldSeed: Long, xoroshiro: Xoroshiro128PlusPlus, sequenceName: String) {
        val world = config.worlds.getOrPut(worldName) { WorldData(worldSeed) }

        if (sequenceName !in world.sequences) {
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

        val existingAdvances = world.sequences[sequenceName]?.advances ?: 0L

        world.sequences[sequenceName] = SequenceData(
            seedLo = xoroshiro.seedLo,
            seedHi = xoroshiro.seedHi,
            advances = existingAdvances + advanceBy
        )

        save()
    }
}
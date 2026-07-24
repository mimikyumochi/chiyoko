package lgbt.faith.chiyoko.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import lgbt.faith.chiyoko.Chiyoko
import lgbt.faith.chiyoko.isMatchingSeed
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentUtils

object ValidateSeed {

    fun register(dispatcher: CommandDispatcher<FabricClientCommandSource>) {
        dispatcher.register(
            ClientCommands.literal("validateseed")
                .executes { ctx ->
                    validate(ctx.source)
                    1
                }
        )
    }

    fun validate(source: FabricClientCommandSource): Int {
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
}
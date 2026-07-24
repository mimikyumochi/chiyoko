package lgbt.faith.chiyoko.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import lgbt.faith.chiyoko.functions.EnchantPredictor
import lgbt.faith.chiyoko.functions.EnchantTarget
import lgbt.faith.chiyoko.functions.EligibleEnchantments
import lgbt.faith.chiyoko.functions.Enchantment
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.ChatFormatting
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import java.util.concurrent.CompletableFuture

object PredictEnchant {

    fun register(dispatcher: CommandDispatcher<FabricClientCommandSource>) {

        dispatcher.register(
            ClientCommands.literal("predict")
                .then(
                    ClientCommands.argument(
                        "item",
                        StringArgumentType.word()
                    )
                        .suggests { _, builder ->
                            itemSuggestions(builder)
                        }
                        .then(
                            ClientCommands.argument(
                                "enchant1",
                                StringArgumentType.word()
                            )
                                .suggests { ctx, builder ->
                                    enchantSuggestions(ctx, builder)
                                }
                                .then(
                                    ClientCommands.argument(
                                        "level1",
                                        IntegerArgumentType.integer(1)
                                    )
                                        .suggests { ctx, builder ->
                                            levelSuggestions(ctx, builder, "enchant1")
                                        }
                                        .executes { ctx ->
                                            execute(ctx.source, 1, ctx)
                                        }
                                        .then(
                                            ClientCommands.argument(
                                                "enchant2",
                                                StringArgumentType.word()
                                            )
                                                .suggests { ctx, builder ->
                                                    enchantSuggestions(ctx, builder)
                                                }
                                                .then(
                                                    ClientCommands.argument(
                                                        "level2",
                                                        IntegerArgumentType.integer(1)
                                                    )
                                                        .suggests { ctx, builder ->
                                                            levelSuggestions(ctx, builder, "enchant2")
                                                        }
                                                        .executes { ctx ->
                                                            execute(ctx.source, 2, ctx)
                                                        }
                                                        .then(
                                                            ClientCommands.argument(
                                                                "enchant3",
                                                                StringArgumentType.word()
                                                            )
                                                                .suggests { ctx, builder ->
                                                                    enchantSuggestions(ctx, builder)
                                                                }
                                                                .then(
                                                                    ClientCommands.argument(
                                                                        "level3",
                                                                        IntegerArgumentType.integer(1)
                                                                    )
                                                                        .suggests { ctx, builder ->
                                                                            levelSuggestions(
                                                                                ctx,
                                                                                builder,
                                                                                "enchant3"
                                                                            )
                                                                        }
                                                                        .executes { ctx ->
                                                                            execute(ctx.source, 3, ctx)
                                                                        }
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        )
    }


    private fun itemSuggestions(
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {

        val items = BuiltInRegistries.ITEM.keySet()
            .filter { id ->

                val item = BuiltInRegistries.ITEM
                    .get(id)
                    .map { it.value() }
                    .orElse(null)

                item != null &&
                        EligibleEnchantments.getEligibleEnchantments(item)
                            .intersect(EligibleEnchantments.ENCHANT_TABLE)
                            .isNotEmpty()
            }
            .map { it.path }

        return SharedSuggestionProvider.suggest(
            items,
            builder
        )
    }


    private fun enchantSuggestions(
        ctx: CommandContext<FabricClientCommandSource>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {

        val itemName = StringArgumentType.getString(ctx, "item")

        val item = BuiltInRegistries.ITEM
            .get(Identifier.withDefaultNamespace(itemName))
            .map { it.value() }
            .orElse(null)
            ?: return builder.buildFuture()


        val used = buildSet {

            try {
                add(StringArgumentType.getString(ctx, "enchant1"))
            } catch (_: Exception) {
            }

            try {
                add(StringArgumentType.getString(ctx, "enchant2"))
            } catch (_: Exception) {
            }
        }


        val enchants = EligibleEnchantments.getEligibleEnchantments(item)
            .intersect(EligibleEnchantments.ENCHANT_TABLE)
            .filter { it !in used }


        return SharedSuggestionProvider.suggest(
            enchants,
            builder
        )
    }


    private fun levelSuggestions(
        ctx: CommandContext<FabricClientCommandSource>,
        builder: SuggestionsBuilder,
        enchantArgument: String
    ): CompletableFuture<Suggestions> {

        val enchantName = StringArgumentType.getString(ctx, enchantArgument)

        val enchant = Enchantment[enchantName]
            ?: return builder.buildFuture()

        return SharedSuggestionProvider.suggest(
            (1..enchant.maxLevel)
                .map { it.toString() },
            builder
        )
    }


    private fun execute(
        source: FabricClientCommandSource,
        count: Int,
        ctx: CommandContext<FabricClientCommandSource>
    ): Int {
        val itemName = StringArgumentType.getString(ctx, "item")
        val item = BuiltInRegistries.ITEM
            .get(Identifier.withDefaultNamespace(itemName))
            .map { it.value() }
            .orElse(null)

        if (item == null) {
            source.sendError(
                Component.literal("Unknown item: $itemName")
                    .withStyle(ChatFormatting.RED)
            )
            return 0
        }

        val targets = buildList {
            add(
                EnchantTarget(
                    Enchantment[StringArgumentType.getString(ctx, "enchant1")]!!,
                    IntegerArgumentType.getInteger(ctx, "level1")
                )
            )
            if (count >= 2) {
                add(
                    EnchantTarget(
                        Enchantment[StringArgumentType.getString(ctx, "enchant2")]!!,
                        IntegerArgumentType.getInteger(ctx, "level2")
                    )
                )
            }
            if (count >= 3) {
                add(
                    EnchantTarget(
                        Enchantment[StringArgumentType.getString(ctx, "enchant3")]!!,
                        IntegerArgumentType.getInteger(ctx, "level3")
                    )
                )
            }
        }

        if (targets.size != targets.toSet().size) {
            source.sendError(
                Component.literal("duplicate enchantments are not allowed")
                    .withStyle(ChatFormatting.RED)
            )
            return 0
        }

        val result = EnchantPredictor.predict(item, targets)

        if (result == null) {
            source.sendFeedback(
                Component.literal("no result found")
                    .withStyle(ChatFormatting.RED)
            )
        } else {
            val stacks = result.drops / 64
            val remainder = result.drops % 64
            val dropsText = when {
                stacks == 0 -> "$remainder"
                remainder == 0 -> "$stacks stack${if (stacks != 1) "s" else ""}"
                else -> "$stacks stack${if (stacks != 1) "s" else ""} and $remainder"
            }

            val message = Component.literal("1. ").withStyle(ChatFormatting.DARK_GREEN)
                .append(Component.literal("drop ").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(dropsText).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" items\n").withStyle(ChatFormatting.GREEN))

                .append(Component.literal("2. ").withStyle(ChatFormatting.DARK_GREEN))
                .append(Component.literal("use ").withStyle(ChatFormatting.GREEN))
                .append(Component.literal("${result.bookshelves}").withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" bookshelves\n").withStyle(ChatFormatting.GREEN))

                .append(Component.literal("3. ").withStyle(ChatFormatting.DARK_GREEN))
                .append(Component.literal("enchant any item once\n").withStyle(ChatFormatting.GREEN))

                .append(Component.literal("4. ").withStyle(ChatFormatting.DARK_GREEN))
                .append(Component.literal("enchant on ").withStyle(ChatFormatting.GREEN))
                .append(Component.literal("slot ${result.slot + 1} ").withStyle(ChatFormatting.AQUA))
                .append(Component.literal("for your chosen enchants\n").withStyle(ChatFormatting.GREEN))

                .append(Component.literal("⚠ moving or taking damage will make this inaccurate").withStyle(ChatFormatting.YELLOW))


            source.sendFeedback(message)
        }

        return 1
    }
}
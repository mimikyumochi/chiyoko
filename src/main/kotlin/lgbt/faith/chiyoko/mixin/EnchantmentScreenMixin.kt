package lgbt.faith.chiyoko.mixin

import lgbt.faith.chiyoko.Chiyoko
import lgbt.faith.chiyoko.ItemEnchantData
import lgbt.faith.chiyoko.crackEntitySeed
import lgbt.faith.chiyoko.functions.EnchantFunctions
import lgbt.faith.chiyoko.functions.EnchantPredictor
import lgbt.faith.chiyoko.functions.XpSeedCracker
import lgbt.faith.chiyoko.rand.LCG
import lgbt.faith.chiyoko.sendOverlay
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.EnchantmentMenu
import net.minecraft.world.item.Items
import net.minecraft.world.item.enchantment.Enchantment
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.LocalCapture
import java.util.*

@Mixin(AbstractContainerScreen::class)
interface AbstractContainerScreenAccessor {
    @Accessor("menu")
    fun chiyoko_getMenu(): AbstractContainerMenu
}

@Mixin(AbstractContainerMenu::class)
abstract class AbstractContainerMenuMixin {

    @Inject(method = ["setData"], at = [At("TAIL")])
    private fun onSetData(id: Int, data: Int, ci: CallbackInfo) {
        val menu = (this as? AbstractContainerMenu) as? EnchantmentMenu ?: return

        if (id == 3 && data != 0) {
            Chiyoko.partialXpSeed = data
        }

        if (id == 9 && Chiyoko.partialXpSeed != null) {
            val itemStack = menu.getSlot(0).item
            if (itemStack.isEmpty) return

            if (menu.costs.all { it == 0 } || menu.enchantClue.all { it == -1 }) return

            val (enchantability, eligibleEnchantments) = ItemEnchantData.of(itemStack.item)

            @Suppress("UNCHECKED_CAST")
            val crackedSeed = XpSeedCracker.getOrCrackSeed(
                partialSeed = Chiyoko.partialXpSeed!!,
                menu = menu,
                enchantability = enchantability,
                eligibleEnchantments = eligibleEnchantments as Set<Nothing>,
                isBook = itemStack.item == Items.BOOK
            )

            if (crackedSeed != null && crackedSeed != Chiyoko.xpSeed) {
                val oldSeed = Chiyoko.xpSeed
                Chiyoko.xpSeed = crackedSeed

                if (oldSeed != null) {
                    val matches = crackEntitySeed(oldSeed, crackedSeed)
                    EnchantPredictor.entityLCG = if (matches.size == 1) {
                        matches.first()
                    } else {
                        null
                    }

                    if (EnchantPredictor.entityLCG != null) {
                        sendOverlay("entity seed cracked! ready to predict", ChatFormatting.GREEN)
                    }
                }

            }
        }
    }
}

@Mixin(EnchantmentScreen::class)
abstract class EnchantmentScreenMixin {

    @Inject(
        method = ["extractRenderState"],
        at = [At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;setComponentTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;II)V", shift = At.Shift.BEFORE)],
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private fun injectFullEnchantData(
        graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTicks: Float, ci: CallbackInfo,
        a: Float, infiniteMaterials: Boolean, gold: Int, i: Int, minLevel: Int, enchant: Optional<*>, enchantLevel: Int, cost: Int, texts: MutableList<Component>
    ) {
        val mc = Minecraft.getInstance()
        val xpSeed = Chiyoko.xpSeed ?: return

        val menu = (this as AbstractContainerScreenAccessor).chiyoko_getMenu() as EnchantmentMenu
        val itemStack = menu.getSlot(0).item
        if (itemStack.isEmpty) return

        val (enchantability, eligibleEnchantments) = ItemEnchantData.of(itemStack.item)

        val rand = LCG()
        rand.setSeed((xpSeed + i).toLong())

        @Suppress("UNCHECKED_CAST")
        val results = EnchantFunctions.enchantWithLevels(
            rng = rand,
            enchantability = enchantability,
            eligibleIds = eligibleEnchantments as Set<Nothing>,
            baseCost = menu.costs[i]
        ).toMutableList()

        if (results.size > 1 && itemStack.item == Items.BOOK) {
            results.removeAt(rand.nextInt(results.size))
        }

        texts.add(CommonComponents.EMPTY)

        val seedCount = XpSeedCracker.possibleSeeds.size
        when {
            seedCount > 1 -> {
                texts.add(
                    Component.literal("⚠ $seedCount possible seeds found")
                        .withStyle(ChatFormatting.YELLOW)
                )
                texts.add(
                    Component.literal("swap item to narrow down!")
                        .withStyle(ChatFormatting.RED)
                )
                texts.add(
                    Component.literal("best guess:")
                        .withStyle(ChatFormatting.DARK_PURPLE)
                )
            }
            seedCount == 0 -> {
                texts.add(
                    Component.literal("failed to crack seed.")
                        .withStyle(ChatFormatting.DARK_RED)
                )
                return
            }
            else -> {
                texts.add(
                    Component.literal("predicted:")
                        .withStyle(ChatFormatting.DARK_PURPLE)
                )
            }
        }

        results.forEach {
            texts.add(
                Component.translatable(
                    "container.enchant.clue",
                    Enchantment.getFullname(it.enchantment, it.level)
                ).withStyle(ChatFormatting.GRAY)
            )
        }
    }
}
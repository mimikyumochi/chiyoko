package lgbt.faith.chiyoko.mixin

import lgbt.faith.chiyoko.Chiyoko
import lgbt.faith.chiyoko.functions.EligibleEnchantments
import lgbt.faith.chiyoko.functions.EligibleEnchantments.LEGACY_REGISTRY_ORDER
import lgbt.faith.chiyoko.functions.EnchantFunctions
import lgbt.faith.chiyoko.functions.Enchantability
import lgbt.faith.chiyoko.functions.EnchantmentCracker
import lgbt.faith.chiyoko.rand.Rand
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

@Mixin(EnchantmentMenu::class)
abstract class EnchantmentMenuMixin {

    @Inject(
        method = ["slotsChanged"],
        at = [At("HEAD")]
    )
    private fun getXpSeed(ci: CallbackInfo) {
        Chiyoko.xpSeed = (this as EnchantmentMenu).enchantmentSeed
        Chiyoko.firstEnchant = false
    }
}

@Mixin(EnchantmentScreen::class)
abstract class EnchantmentScreenMixin  {


    @Inject(
        method = ["extractRenderState"],
        at = [At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;setComponentTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;II)V", shift = At.Shift.BEFORE)],
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private fun injectFullEnchantData(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTicks: Float, ci: CallbackInfo, // parameters
                                      a: Float, infiniteMaterials: Boolean, gold: Int, i: Int, minLevel: Int, enchant: Optional<*>, enchantLevel: Int, cost: Int, texts: MutableList<Component> // variables
        ) {
        val mc = Minecraft.getInstance()

        if (Chiyoko.xpSeed == 0 && mc.currentServer == null) return


        val menu = (this as AbstractContainerScreenAccessor).chiyoko_getMenu() as EnchantmentMenu

        val itemStack = menu.getSlot(0).item
        if (itemStack.isEmpty) return

        val enchantability = Enchantability.getEnchantability(itemStack.item)

        val intersected = EligibleEnchantments.getEligibleEnchantments(itemStack.item)
            .intersect(EligibleEnchantments.ENCHANT_TABLE)
            .toMutableList()

        intersected.sortWith(Comparator { a, b ->
            val idxA = LEGACY_REGISTRY_ORDER.indexOf(a).let { if (it == -1) 999 else it }
            val idxB = LEGACY_REGISTRY_ORDER.indexOf(b).let { if (it == -1) 999 else it }
            idxA.compareTo(idxB)
        })

        val xpSeed = if (mc.currentServer == null) {
            Chiyoko.xpSeed
        } else {
            EnchantmentCracker.getOrCrackSeed(
                menu = menu,
                enchantability = enchantability,
                eligibleEnchantments = intersected.toSet(),
                isBook = itemStack.item == Items.BOOK
            )
        }


        val rand = Rand()
        rand.setSeed((xpSeed + i).toLong())




        val results = EnchantFunctions.enchantWithLevels(
            rng = rand,
            enchantability = enchantability,
            eligibleIds = intersected.toSet(),
            baseCost = menu.costs[i]
        ).toMutableList()

        if (results.size > 1 && itemStack.item == Items.BOOK) {
            results.removeAt(rand.nextInt(results.size))
        }

        texts.add(CommonComponents.EMPTY)
        texts.add(CommonComponents.EMPTY)

        if (mc.currentServer != null || Chiyoko.firstEnchant) {
            val seedCount = EnchantmentCracker.possibleSeeds.size
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
        } else {
            texts.add(
                Component.literal("predicted:")
                    .withStyle(ChatFormatting.DARK_PURPLE)
            )
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
package lgbt.faith.chiyoko.mixin

import lgbt.faith.chiyoko.DropEventState
import lgbt.faith.chiyoko.PendingShulkerDeath
import lgbt.faith.chiyoko.PendingWitherDeath
import net.minecraft.client.Minecraft
import net.minecraft.core.registries.Registries
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.monster.Shulker
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.enchantment.Enchantments
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(LivingEntity::class)
class LivingEntityMixin {

    @Inject(method = ["setHealth"], at = [At("HEAD")])
    private fun onSetHealth(health: Float, ci: CallbackInfo) {
        val entity = this as LivingEntity
        if (!entity.level().isClientSide) return
        if (entity.health <= 0f || health > 0f) return

        val mc = Minecraft.getInstance()
        val player = mc.player ?: return

        val enchantLookup = entity.level().registryAccess().lookup(Registries.ENCHANTMENT).orElse(null) ?: return
        val lootingHolder = enchantLookup.get(Enchantments.LOOTING).orElse(null)
        val lootingLevel = if (lootingHolder != null) EnchantmentHelper.getItemEnchantmentLevel(lootingHolder, player.mainHandItem) else 0

        if (entity is WitherSkeleton) {
            val playerKilled = DropEventState.recentlyAttackedWithers.remove(entity.id)
            if (!playerKilled) return
            DropEventState.pendingWithers.add(PendingWitherDeath(entity.position(), lootingLevel, playerKilled))

        }
        if (entity is Shulker) {
            DropEventState.pendingShulkers.add(PendingShulkerDeath(entity.position(), lootingLevel))

        }
    }
}
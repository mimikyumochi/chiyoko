package lgbt.faith.chiyoko.mixin

import lgbt.faith.chiyoko.DropCapture
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import kotlin.collections.set

@Mixin(Entity::class)
class EntityMixin {

    @Inject(
        method = ["spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;"],
        at = [At("HEAD")]
    )
    private fun onSpawnAtLocation(level: ServerLevel, stack: ItemStack, cir: CallbackInfoReturnable<ItemEntity>) {
        val entity = this as Entity
        val list = DropCapture.pendingDrops[entity.id]

        if (list == null) {
            DropCapture.pendingDrops[entity.id] = mutableListOf(stack.copy())
        }
        else {
            DropCapture.pendingDrops[entity.id]?.add(stack.copy())
        }

    }
}
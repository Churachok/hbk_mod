package dev.kirill.hbk.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.kirill.hbk.mechanic.FurryBreeding;
import dev.kirill.hbk.registry.ModItems;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemEntity.class)
public abstract class ItemEntityFurryPlugMixin {
	@WrapOperation(method = "playerTouch", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/player/Inventory;add(Lnet/minecraft/world/item/ItemStack;)Z"))
	private boolean hbk$useOneFreshlyPickedUpPlug(Inventory inventory, ItemStack stack,
			Operation<Boolean> original, Player player) {
		if (!stack.is(ModItems.FURRY_PLUG) || !FurryBreeding.canEnterLove(player)) return original.call(inventory, stack);
		// This native call is already guarded by the item's pickup delay and target UUID.
		// If vanilla could not pick up even one item, do not turn mere contact into activation.
		boolean hasSpace = player.hasInfiniteMaterials() || inventory.getFreeSlot() >= 0
				|| inventory.getSlotWithRemainingSpace(stack) >= 0;
		if (hasSpace && FurryBreeding.consumeGroundPickup(player, stack)) {
			if (!stack.isEmpty()) original.call(inventory, stack);
			return true;
		}
		return original.call(inventory, stack);
	}
}

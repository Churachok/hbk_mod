package dev.kirill.hbk.mixin;

import dev.kirill.hbk.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Uranium burns for ten minutes but gives every furnace recipe four times the cook time. */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {
	@Shadow protected NonNullList<ItemStack> items;
	@Shadow private int litTimeRemaining;
	@Shadow private int litTotalTime;
	@Shadow private int cookingTimer;
	@Shadow private int cookingTotalTime;
	@Shadow @Final private RecipeManager.CachedCheck<SingleRecipeInput, ? extends AbstractCookingRecipe> quickCheck;

	@Inject(method = "serverTick", at = @At("HEAD"))
	private static void hbk$slowUranium(ServerLevel level, BlockPos pos, BlockState state,
			AbstractFurnaceBlockEntity furnace, CallbackInfo ci) {
		AbstractFurnaceBlockEntityMixin self = (AbstractFurnaceBlockEntityMixin) (Object) furnace;
		ItemStack input = self.items.get(0);
		if (input.isEmpty()) return;
		var recipe = self.quickCheck.getRecipeFor(new SingleRecipeInput(input), level);
		if (recipe.isEmpty()) return;
		int base = recipe.get().value().cookingTime();
		boolean uranium = self.litTimeRemaining > 1 && self.litTotalTime == 600 * 20
				|| self.litTimeRemaining <= 1 && self.items.get(1).is(ModItems.URANIUM_235);
		int desired = base * (uranium ? 4 : 1);
		if (self.cookingTotalTime != desired) {
			self.cookingTimer = self.cookingTotalTime > 0
					? self.cookingTimer * desired / self.cookingTotalTime : 0;
			self.cookingTotalTime = desired;
		}
	}
}

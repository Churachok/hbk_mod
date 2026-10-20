package dev.kirill.hbk.mixin;

import dev.kirill.hbk.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {
	@Inject(method = "getDrops", at = @At("HEAD"), cancellable = true)
	private void hbk$requireOreTool(net.minecraft.world.level.storage.loot.LootParams.Builder params,
			CallbackInfoReturnable<java.util.List<net.minecraft.world.item.ItemStack>> cir) {
		var tool = params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL);
		if (!dev.kirill.hbk.universe.UniverseMining.permits(params.getLevel(), (net.minecraft.world.level.block.state.BlockState) (Object) this,
				tool == null ? net.minecraft.world.item.ItemStack.EMPTY : tool)) cir.setReturnValue(java.util.List.of());
	}
	@Inject(method = "getDrops", at = @At("RETURN"), cancellable = true)
	private void hbk$smeltAlloyLoot(net.minecraft.world.level.storage.loot.LootParams.Builder params,
			CallbackInfoReturnable<java.util.List<net.minecraft.world.item.ItemStack>> cir) {
		cir.setReturnValue(dev.kirill.hbk.universe.AlloyLoot.smelt(params.getLevel(), (net.minecraft.world.level.block.state.BlockState) (Object) this,
				params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL), cir.getReturnValue()));
	}
	@Inject(method = "getDestroyProgress", at = @At("HEAD"), cancellable = true)
	private void hbk$instantBareHandBreaking(Player player, BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
		if (player.hasEffect(ModEffects.HAND_IMMORTALITY) && player.getMainHandItem().isEmpty()) {
			cir.setReturnValue(1.0f);
		}
	}
}

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
	@Inject(method = "getDestroyProgress", at = @At("HEAD"), cancellable = true)
	private void hbk$instantBareHandBreaking(Player player, BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
		if (player.hasEffect(ModEffects.HAND_IMMORTALITY) && player.getMainHandItem().isEmpty()) {
			cir.setReturnValue(1.0f);
		}
	}
}

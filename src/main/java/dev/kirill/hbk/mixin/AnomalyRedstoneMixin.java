package dev.kirill.hbk.mixin;

import dev.kirill.hbk.world.AnomalyZones;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class AnomalyRedstoneMixin {
	@Inject(method = {"getSignal", "getDirectSignal"}, at = @At("HEAD"), cancellable = true)
	private void hbk$disableAnomalySignal(BlockGetter level, BlockPos pos, Direction direction, CallbackInfoReturnable<Integer> cir) {
		if (level instanceof Level world && AnomalyZones.contains(world, pos)) cir.setReturnValue(0);
	}

	@Inject(method = "getOwnSignal", at = @At("HEAD"), cancellable = true)
	private void hbk$disableAnomalyOwnSignal(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
		if (level instanceof Level world && AnomalyZones.contains(world, pos)) cir.setReturnValue(0);
	}

	@Inject(method = "triggerEvent", at = @At("HEAD"), cancellable = true)
	private void hbk$stopQueuedAnomalyPiston(Level level, BlockPos pos, int type, int data, CallbackInfoReturnable<Boolean> cir) {
		var state = level.getBlockState(pos);
		if ((state.is(Blocks.PISTON) || state.is(Blocks.STICKY_PISTON) || state.is(Blocks.MOVING_PISTON))
				&& AnomalyZones.contains(level, pos)) cir.setReturnValue(false);
	}
}

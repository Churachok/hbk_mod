package dev.kirill.hbk.mixin;

import dev.kirill.hbk.world.AnomalyZones;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SignalGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SignalGetter.class)
public interface AnomalySignalMixin {
	@Inject(method = {"getSignal", "getDirectSignal"}, at = @At("HEAD"), cancellable = true)
	private void hbk$rejectAnomalyInput(BlockPos pos, Direction direction, CallbackInfoReturnable<Integer> cir) {
		if ((Object) this instanceof Level level && AnomalyZones.contains(level, pos)) cir.setReturnValue(0);
	}

	@Inject(method = {"getBestNeighborSignal", "getDirectSignalTo", "getBestOwnOrNeighbourSignal"}, at = @At("HEAD"), cancellable = true)
	private void hbk$rejectAnomalyNeighborInput(BlockPos pos, CallbackInfoReturnable<Integer> cir) {
		if ((Object) this instanceof Level level && AnomalyZones.contains(level, pos)) cir.setReturnValue(0);
	}

	@Inject(method = "hasNeighborSignal", at = @At("HEAD"), cancellable = true)
	private void hbk$rejectAnomalyPower(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof Level level && AnomalyZones.contains(level, pos)) cir.setReturnValue(false);
	}

	@Inject(method = "hasSignal", at = @At("HEAD"), cancellable = true)
	private void hbk$rejectAnomalyDirectionalPower(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof Level level && AnomalyZones.contains(level, pos)) cir.setReturnValue(false);
	}
}

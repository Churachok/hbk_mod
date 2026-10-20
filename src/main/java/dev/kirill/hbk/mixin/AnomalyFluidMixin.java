package dev.kirill.hbk.mixin;

import dev.kirill.hbk.world.AnomalyZones;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FlowingFluid.class)
public abstract class AnomalyFluidMixin {
	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void hbk$freezeAnomalyFluid(ServerLevel level, BlockPos pos, BlockState block, FluidState fluid, CallbackInfo ci) {
		if (AnomalyZones.contains(level, pos)) ci.cancel();
	}

	@Inject(method = "spreadTo", at = @At("HEAD"), cancellable = true)
	private void hbk$stopFlowAtAnomalyBoundary(LevelAccessor level, BlockPos pos, BlockState state, Direction direction, FluidState fluid, CallbackInfo ci) {
		if (level instanceof net.minecraft.world.level.Level world && AnomalyZones.contains(world, pos)) ci.cancel();
	}
}

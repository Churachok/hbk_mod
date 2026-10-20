package dev.kirill.hbk.mixin;

import dev.kirill.hbk.universe.HiddenParameter;
import dev.kirill.hbk.universe.UniverseMaterials;
import dev.kirill.hbk.universe.UniverseProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FlowingFluid.class)
public abstract class UniverseFluidMixin {
	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void hbk$stableReservoir(ServerLevel level, BlockPos pos, BlockState block, FluidState fluid, CallbackInfo ci) {
		if (block.is(UniverseMaterials.NATURAL_WATER)) ci.cancel();
	}
	@Inject(method = "getSpreadDelay", at = @At("RETURN"), cancellable = true)
	private void hbk$liquidRate(Level level, BlockPos pos, FluidState before, FluidState after, CallbackInfoReturnable<Integer> cir) {
		var p = UniverseProfile.of(level);
		if (p == null) return;
		double speed = new double[]{.05, .25, .4, .6, .8, 1, 1.2, 1.5, 2, 3}[p.get(HiddenParameter.LIQUID_SPEED)];
		cir.setReturnValue(Math.max(1, (int) Math.ceil(cir.getReturnValue() / speed)));
	}
}

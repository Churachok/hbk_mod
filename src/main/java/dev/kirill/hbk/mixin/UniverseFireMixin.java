package dev.kirill.hbk.mixin;

import dev.kirill.hbk.universe.HiddenParameter;
import dev.kirill.hbk.universe.UniverseProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireBlock.class)
public abstract class UniverseFireMixin {
	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;scheduleTick(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;I)V"))
	private void hbk$spreadDelay(ServerLevel level, BlockPos pos, net.minecraft.world.level.block.Block block, int delay) {
		var p = UniverseProfile.of(level);
		double speed = p == null ? 1 : new double[]{1, .2, .4, .6, .8, 1, 1.25, 1.5, 2, 3}[p.get(HiddenParameter.FIRE_SPEED)];
		level.scheduleTick(pos, block, Math.max(1, (int) Math.ceil(delay / speed)));
	}

	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void hbk$fireRate(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
		var p = UniverseProfile.of(level);
		if (p == null) return;
		int selected = p.get(HiddenParameter.FIRE_SPEED);
		if (selected == 0) { level.scheduleTick(pos, Blocks.FIRE, 100); ci.cancel(); return; }
		
	}
}

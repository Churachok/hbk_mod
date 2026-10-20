package dev.kirill.hbk.mixin;

import dev.kirill.hbk.world.AnomalyZones;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Monster.class)
public abstract class AnomalyMonsterSpawnMixin {
	@Inject(method = "isDarkEnoughToSpawn", at = @At("HEAD"), cancellable = true)
	private static void hbk$spawnInAnomalyLight(ServerLevelAccessor level, BlockPos pos, RandomSource random,
			CallbackInfoReturnable<Boolean> cir) {
		if (AnomalyZones.contains(level.getLevel(), pos)) cir.setReturnValue(true);
	}
}

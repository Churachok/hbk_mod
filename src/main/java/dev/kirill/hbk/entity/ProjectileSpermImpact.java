package dev.kirill.hbk.entity;

import dev.kirill.hbk.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Replaces the struck ordinary block without erasing containers or unbreakable blocks. */
final class ProjectileSpermImpact {
	private ProjectileSpermImpact() {
	}

	static BlockPos eligiblePosition(ServerLevel level, HitResult hit) {
		if (!(hit instanceof BlockHitResult blockHit)) {
			return null;
		}
		BlockPos pos = blockHit.getBlockPos();
		BlockState state = level.getBlockState(pos);
		if (state.isAir() || state.is(ModBlocks.SPERM) || state.getDestroySpeed(level, pos) < 0
				|| level.getBlockEntity(pos) != null) {
			return null;
		}
		return pos;
	}

	static void replace(ServerLevel level, BlockPos pos) {
		if (pos == null) return;
		level.setBlockAndUpdate(pos, ModBlocks.SPERM.defaultBlockState());
	}
}

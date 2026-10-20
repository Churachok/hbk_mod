package dev.kirill.hbk.universe;

import dev.kirill.hbk.block.TimeFurnaceBlock;
import dev.kirill.hbk.block.entity.TimeFurnaceBlockEntity;
import dev.kirill.hbk.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** A usable return machine at every arrival; existing blocks/containers are never replaced. */
public final class UniverseArrival {
	private UniverseArrival() {}
	public static BlockPos ensureReturnFurnace(ServerLevel level, BlockPos arrival) {
		BlockPos existing = findReturnFurnace(level, arrival);
		if (existing != null) return existing;
		return createReturnFurnace(level, arrival);
	}
	public static BlockPos findReturnFurnace(ServerLevel level, BlockPos arrival) {
		for (int r = 1; r <= 5; r++) for (int dy = -1; dy <= 1; dy++)
			for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
				if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
				BlockPos pos = arrival.offset(dx, dy, dz);
				var upper = level.getBlockState(pos.above());
				if (pos.distSqr(arrival) <= 16 && level.getBlockEntity(pos) instanceof TimeFurnaceBlockEntity
						&& upper.is(ModBlocks.TIME_FURNACE) && upper.getValue(TimeFurnaceBlock.HALF) == DoubleBlockHalf.UPPER) return pos;
			}
		return null;
	}
	private static BlockPos createReturnFurnace(ServerLevel level, BlockPos arrival) {
		for (int r = 2; r <= 4; r++) for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
			if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
			BlockPos pos = arrival.offset(dx, 0, dz);
			if (clear(level, pos) && level.getBlockState(pos.below()).isCollisionShapeFullBlock(level, pos.below())) return place(level, pos, arrival);
		}
		// Narrow ledges and suspended worlds: extend a two-block walkway into empty space only.
		var profile = UniverseProfile.of(level);
		if (profile == null) return null;
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			boolean clear = true;
			for (int step = 1; step <= 2; step++) {
				BlockPos pos = arrival.relative(direction, step);
				var below = level.getBlockState(pos.below());
				if (!clear(level, pos) || !below.isAir() && !below.isCollisionShapeFullBlock(level, pos.below())) { clear = false; break; }
			}
			if (!clear) continue;
			for (int step = 1; step <= 2; step++) {
				BlockPos floor = arrival.relative(direction, step).below();
				if (level.getBlockState(floor).isAir()) level.setBlock(floor, UniverseMaterials.state(profile.geometry(), 0, 0), 3);
			}
			return place(level, arrival.relative(direction, 2), arrival);
		}
		return null;
	}
	private static boolean clear(ServerLevel level, BlockPos pos) {
		return pos.getY() > level.getMinY() && pos.getY() + 1 <= level.getMaxY()
				&& level.getWorldBorder().isWithinBounds(pos) && level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir();
	}
	private static BlockPos place(ServerLevel level, BlockPos pos, BlockPos arrival) {
		Direction facing = Math.abs(arrival.getX() - pos.getX()) > Math.abs(arrival.getZ() - pos.getZ())
				? arrival.getX() > pos.getX() ? Direction.EAST : Direction.WEST
				: arrival.getZ() > pos.getZ() ? Direction.SOUTH : Direction.NORTH;
		var lower = ModBlocks.TIME_FURNACE.defaultBlockState().setValue(TimeFurnaceBlock.FACING, facing);
		level.setBlock(pos, lower, 2);
		level.setBlock(pos.above(), lower.setValue(TimeFurnaceBlock.HALF, DoubleBlockHalf.UPPER), 3);
		return pos;
	}
}

package dev.kirill.hbk.universe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;

/** Worldgen reservoir: real swimmable, bucketable water with no automatic update cascade.
 * Water poured by a player remains ordinary flowing water and obeys the profile's tick delay.
 */
public final class UniverseWaterBlock extends LiquidBlock {
	public UniverseWaterBlock(Properties properties) { super(Fluids.WATER, properties); }
	@Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {}
	@Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, Orientation orientation, boolean moved) {}
	@Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
			Direction direction, BlockPos neighbour, BlockState neighbourState, RandomSource random) { return state; }
}

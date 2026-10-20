package dev.kirill.hbk.universe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;

/** No obtainable block item or drops: a spool cannot manufacture free ordinary ladders. */
public final class AlloyRopeLadder extends LadderBlock {
	public AlloyRopeLadder(Properties properties) { super(properties); }
	@Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		var above = level.getBlockState(pos.above());
		return super.canSurvive(state, level, pos) || above.is(this) && above.getValue(FACING) == state.getValue(FACING);
	}
	@Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
			Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		return canSurvive(state, level, pos) ? super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random) : Blocks.AIR.defaultBlockState();
	}
}

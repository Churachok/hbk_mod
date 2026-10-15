package dev.kirill.hbk.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Daylight-tolerant mushroom with a stepped cap matching the concept. */
public final class RoflMushroomBlock extends BushBlock {
	public static final MapCodec<BushBlock> CODEC = simpleCodec(RoflMushroomBlock::new);
	private static final VoxelShape SHAPE = Shapes.or(
			box(6, 0, 6, 10, 8, 10), box(1, 8, 1, 15, 11, 15),
			box(3, 11, 3, 13, 13, 13), box(5, 13, 5, 11, 15, 11));

	public RoflMushroomBlock(Properties properties) {
		super(properties);
	}

	@Override
	public MapCodec<BushBlock> codec() {
		return CODEC;
	}

	@Override
	protected boolean mayPlaceOn(BlockState floor, BlockGetter level, BlockPos pos) {
		// Any solid surface also permits patches in deserts, snowy and mountain biomes.
		return floor.isFaceSturdy(level, pos, net.minecraft.core.Direction.UP);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}
}

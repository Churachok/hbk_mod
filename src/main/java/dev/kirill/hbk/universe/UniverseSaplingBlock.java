package dev.kirill.hbk.universe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Regrows the family's original tree, including inverted trees when attached to a ceiling. */
public final class UniverseSaplingBlock extends VegetationBlock implements BonemealableBlock {
	public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 1);
	public static final BooleanProperty HANGING = BooleanProperty.create("hanging");
	public static final MapCodec<UniverseSaplingBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			com.mojang.serialization.Codec.intRange(0, 9).fieldOf("family").forGetter(b -> b.family), propertiesCodec()).apply(i, UniverseSaplingBlock::new));
	private final int family;
	public UniverseSaplingBlock(int family, BlockBehaviour.Properties properties) {
		super(properties); this.family = family; registerDefaultState(stateDefinition.any().setValue(STAGE, 0).setValue(HANGING, false));
	}
	@Override public MapCodec<UniverseSaplingBlock> codec() { return CODEC; }
	@Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(STAGE, HANGING); }
	@Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.box(.1, 0, .1, .9, 1, .9); }
	@Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(HANGING, context.getClickedFace() == Direction.DOWN); }
	@Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return state.getValue(HANGING) ? level.getBlockState(pos.above()).isFaceSturdy(level, pos.above(), Direction.DOWN) : super.canSurvive(state, level, pos);
	}
	@Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (random.nextInt(7) == 0 && (family == 3 || family == 8 || level.getMaxLocalRawBrightness(pos) >= 9)) advance(level, pos, state);
	}
	public boolean growTree(ServerLevel level, BlockPos pos, BlockState state) {
		UniverseProfile profile = UniverseProfile.of(level);
		if (profile == null || profile.geometry() != family) profile = UniverseProfile.of(new UniverseCode(family + "0011"));
		var tree = new UniverseEcology(profile).tree(pos.getX(), pos.getY(), pos.getZ(), state.getValue(HANGING) ? -1 : 1,
				UniverseNoise.hash(profile.stream(44), pos.getX(), pos.getY(), pos.getZ()));
		for (var target : tree.keySet()) {
			if (target.getY() <= level.getMinY() || target.getY() >= level.getMaxY() || !level.hasChunkAt(target)) return false;
			BlockState existing = level.getBlockState(target);
			if (!target.equals(pos) && !existing.canBeReplaced() && !existing.is(BlockTags.LEAVES)) return false;
		}
		for (var entry : tree.entrySet()) level.setBlock(entry.getKey(), entry.getValue(), 2);
		level.sendBlockUpdated(pos, state, level.getBlockState(pos), 3);
		return true;
	}
	private void advance(ServerLevel level, BlockPos pos, BlockState state) {
		if (state.getValue(STAGE) == 0) level.setBlock(pos, state.setValue(STAGE, 1), 3); else growTree(level, pos, state);
	}
	@Override public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) { return true; }
	@Override public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) { return true; }
	@Override public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) { advance(level, pos, state); }
}

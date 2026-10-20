package dev.kirill.hbk.universe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Renewable, harvestable flora; the same crop can be cultivated on soil or beneath a ceiling. */
public final class UniverseCropBlock extends VegetationBlock implements BonemealableBlock {
	public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 3);
	public static final BooleanProperty HANGING = BooleanProperty.create("hanging");
	public static final MapCodec<UniverseCropBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			com.mojang.serialization.Codec.intRange(0, 9).fieldOf("family").forGetter(b -> b.family), propertiesCodec()).apply(i, UniverseCropBlock::new));
	private final int family;
	public UniverseCropBlock(int family, BlockBehaviour.Properties properties) {
		super(properties); this.family = family; registerDefaultState(stateDefinition.any().setValue(AGE, 0).setValue(HANGING, false));
	}
	@Override public MapCodec<UniverseCropBlock> codec() { return CODEC; }
	@Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(AGE, HANGING); }
	@Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		double height = .3 + state.getValue(AGE) * .2;
		return state.getValue(HANGING) ? Shapes.box(.15, 1 - height, .15, .85, 1, .85) : Shapes.box(.15, 0, .15, .85, height, .85);
	}
	@Override public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(HANGING, context.getClickedFace() == Direction.DOWN);
	}
	@Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		if (!state.getValue(HANGING)) return super.canSurvive(state, level, pos);
		BlockState above = level.getBlockState(pos.above());
		return above.isFaceSturdy(level, pos.above(), Direction.DOWN) || above.is(BlockTags.LEAVES)
				|| above.getBlock() instanceof UniverseCropBlock && above.getValue(HANGING);
	}
	@Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.getValue(AGE) < 3 && random.nextInt(5) == 0) grow(level, pos, state);
	}
	private static void grow(ServerLevel level, BlockPos pos, BlockState state) { level.setBlock(pos, state.setValue(AGE, Math.min(3, state.getValue(AGE) + 1)), 3); }
	@Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (state.getValue(AGE) < 3) return InteractionResult.PASS;
		if (level instanceof ServerLevel server) {
			popResource(level, pos, new ItemStack(UniverseBotany.fruit(family), 2 + server.getRandom().nextInt(2)));
			server.setBlock(pos, state.setValue(AGE, 1), 3);
			server.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, .8f, 1);
			server.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, state));
		}
		return InteractionResult.SUCCESS;
	}
	@Override public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) { return state.getValue(AGE) < 3; }
	@Override public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) { return true; }
	@Override public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) { grow(level, pos, state); }
}

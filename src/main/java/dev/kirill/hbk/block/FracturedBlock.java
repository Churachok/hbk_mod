package dev.kirill.hbk.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** All phases share one block identity and an outline, so a held mining target survives phasing. */
public final class FracturedBlock extends Block {
	public static final MapCodec<FracturedBlock> CODEC = simpleCodec(FracturedBlock::new);
	public static final EnumProperty<Phase> PHASE = EnumProperty.create("phase", Phase.class);

	public enum Phase implements StringRepresentable {
		MATERIAL("material"), PHANTOM("phantom"), DISPLACED("displaced");
		private final String name;
		Phase(String name) { this.name = name; }
		@Override public String getSerializedName() { return name; }
		public Phase next() { return values()[(ordinal() + 1) % values().length]; }
	}

	public FracturedBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(PHASE, Phase.MATERIAL));
	}

	@Override public MapCodec<FracturedBlock> codec() { return CODEC; }

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(PHASE);
	}

	public static boolean material(BlockState state) {
		return state.getBlock() instanceof FracturedBlock && state.getValue(PHASE) == Phase.MATERIAL;
	}

	public static int duration(Phase phase, RandomSource random) {
		return switch (phase) {
			case MATERIAL -> 80 + random.nextInt(81);
			case PHANTOM -> 40 + random.nextInt(41);
			case DISPLACED -> 30 + random.nextInt(31);
		};
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
		if (!old.is(this) && level instanceof ServerLevel server) {
			server.scheduleTick(pos, this, duration(state.getValue(PHASE), server.getRandom()));
		}
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		Phase next = state.getValue(PHASE).next();
		// Do not solidify through a player or a mob standing in the phantom cube.
		if (state.getValue(PHASE) == Phase.PHANTOM
				&& !level.getEntitiesOfClass(LivingEntity.class, new AABB(pos)).isEmpty()) {
			level.scheduleTick(pos, this, 10);
			return;
		}
		level.setBlock(pos, state.setValue(PHASE, next), Block.UPDATE_ALL);
		level.scheduleTick(pos, this, duration(next, random));
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(PHASE) == Phase.PHANTOM ? Shapes.empty() : Shapes.block();
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.block();
	}

	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		return material(state) && player.getMainHandItem().is(dev.kirill.hbk.registry.ModItems.REDSTONE_PICKAXE)
				? super.getDestroyProgress(state, player, level, pos) : 0;
	}
}

package dev.kirill.hbk.universe;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** Vanilla block models/menus, common server-side machine logic. */
public final class AlloyDeviceBlock extends BaseEntityBlock {
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	public final String kind;
	public AlloyDeviceBlock(String kind, Properties properties) { super(properties); this.kind = kind; registerDefaultState(stateDefinition.any().setValue(POWERED, false)); }
	@Override protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(p -> new AlloyDeviceBlock(kind, p)); }
	@Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(POWERED); }
	@Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new AlloyDeviceBlockEntity(pos, state); }
	@Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
	@Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, UniverseAlloys.DEVICE_TYPE, AlloyDeviceBlockEntity::tick);
	}
	@Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(level instanceof ServerLevel)) return InteractionResult.SUCCESS;
		if (level.getBlockEntity(pos) instanceof AlloyDeviceBlockEntity machine) machine.use(player);
		return InteractionResult.SUCCESS_SERVER;
	}
	@Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return kind.equals("lift_platform") || kind.equals("transition_plate") ? Block.column(16, 0, 2) : super.getShape(state, level, pos, context);
	}
	@Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return kind.equals("phase_glass") && state.getValue(POWERED) ? Shapes.empty() : getShape(state, level, pos, context);
	}
	@Override protected boolean isSignalSource(BlockState state) { return kind.equals("wind_vane") || kind.equals("anomaly_sensor") || kind.equals("rhythm_controller"); }
	@Override protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return level.getBlockEntity(pos) instanceof AlloyDeviceBlockEntity machine ? machine.signal() : 0;
	}
}

package dev.kirill.hbk.block;

import com.mojang.serialization.MapCodec;
import dev.kirill.hbk.network.ModNetworking;
import dev.kirill.hbk.world.ModWorldData;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;

public final class StrangeChestBlock extends Block {
	public static final MapCodec<StrangeChestBlock> CODEC = simpleCodec(StrangeChestBlock::new);
	private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 14.0, 15.0);

	public StrangeChestBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}

		if (ModWorldData.get(serverLevel).isStrangeChestAnswered(pos.asLong())) {
			serverPlayer.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.hbk.strange_chest.empty"));
			return InteractionResult.SUCCESS;
		}

		serverLevel.playSound(null, pos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 1.0f, 0.75f);
		ServerPlayNetworking.send(serverPlayer, new ModNetworking.OpenStrangeChestPayload(pos));
		return InteractionResult.SUCCESS;
	}
}

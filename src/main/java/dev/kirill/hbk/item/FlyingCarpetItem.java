package dev.kirill.hbk.item;

import dev.kirill.hbk.entity.FlyingCarpetEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class FlyingCarpetItem extends Item {
	private static final float BREAK_CHANCE = 0.1f;

	public FlyingCarpetItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}

		ItemStack stack = player.getItemInHand(hand);
		player.awardStat(Stats.ITEM_USED.get(this));

		if (serverPlayer.isSpectator()) {
			serverPlayer.sendOverlayMessage(Component.translatable("message.hbk.flying_carpet.creative"));
			return InteractionResult.SUCCESS_SERVER;
		}

		if (!serverPlayer.isCreative() && serverLevel.getRandom().nextFloat() < BREAK_CHANCE) {
			stack.shrink(1);
			serverLevel.playSound(
					null,
					serverPlayer.getX(),
					serverPlayer.getY(),
					serverPlayer.getZ(),
					SoundEvents.ITEM_BREAK,
					SoundSource.PLAYERS,
					1.0f,
					1.0f
			);
			serverPlayer.sendOverlayMessage(Component.translatable("message.hbk.flying_carpet.broken"));
			return InteractionResult.SUCCESS_SERVER;
		}

		FlyingCarpetEntity carpet = new FlyingCarpetEntity(serverLevel, serverPlayer, !serverPlayer.isCreative());
		if (!serverLevel.addFreshEntity(carpet)) {
			return InteractionResult.FAIL;
		}
		if (!serverPlayer.isCreative()) {
			stack.shrink(1);
		}
		serverPlayer.startRiding(carpet, true, false);
		serverPlayer.sendOverlayMessage(Component.translatable("message.hbk.flying_carpet.enabled"));

		return InteractionResult.SUCCESS_SERVER;
	}
}

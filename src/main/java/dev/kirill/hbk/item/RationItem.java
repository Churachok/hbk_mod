package dev.kirill.hbk.item;

import dev.kirill.hbk.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class RationItem extends Item {
	public RationItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		ItemStack ration = player.getItemInHand(hand);
		if (!player.isCreative()) {
			ration.shrink(1);
		}
		ItemStack reward = new ItemStack(player.getRandom().nextBoolean() ? ModItems.SUSHKA : ModItems.GOLDEN_CROWN);
		if (!player.getInventory().add(reward)) {
			player.drop(reward, false);
		}
		player.playSound(SoundEvents.BUNDLE_DROP_CONTENTS, 1.0f, 0.9f + player.getRandom().nextFloat() * 0.2f);
		if (player instanceof ServerPlayer serverPlayer) {
			serverPlayer.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.hbk.ration.opened", reward.getHoverName()));
		}
		return InteractionResult.SUCCESS;
	}
}

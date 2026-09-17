package dev.kirill.hbk.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class BandageItem extends Item {
	public BandageItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player.getHealth() >= player.getMaxHealth()) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			player.heal(2.0f);
			ItemStack stack = player.getItemInHand(hand);
			if (!player.isCreative()) {
				stack.shrink(1);
			}
			player.playSound(SoundEvents.WOOL_PLACE, 0.8f, 1.2f);
		}
		return InteractionResult.SUCCESS;
	}
}

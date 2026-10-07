package dev.kirill.hbk.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
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
		return heal(level, player, player.getItemInHand(hand), player);
	}

	@Override
	public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
		return heal(player.level(), player, stack, target);
	}

	private static InteractionResult heal(Level level, Player player, ItemStack stack, LivingEntity target) {
		if (target.getHealth() >= target.getMaxHealth() || !target.isAlive()) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			target.heal(2.0f);
			if (!player.isCreative()) {
				stack.shrink(1);
			}
			target.playSound(SoundEvents.WOOL_PLACE, 0.8f, 1.2f);
		}
		return InteractionResult.SUCCESS;
	}
}

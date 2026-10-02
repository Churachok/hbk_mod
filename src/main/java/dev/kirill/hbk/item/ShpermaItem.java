package dev.kirill.hbk.item;

import dev.kirill.hbk.entity.AttackingMemberBulletEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

/** A quick release restores the player; a one-second hold fires Kirill's bullet. */
public final class ShpermaItem extends Item {
	public static final int CHARGE_TICKS = 20;
	private static final int USE_DURATION_TICKS = 72_000;

	public ShpermaItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player.isSpectator()) {
			return InteractionResult.FAIL;
		}
		player.startUsingItem(hand);
		return InteractionResult.CONSUME;
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		return ItemUseAnimation.BOW;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity user) {
		return USE_DURATION_TICKS;
	}

	@Override
	public boolean releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
		if (!(user instanceof Player player) || player.isSpectator()) {
			return false;
		}

		int useTicks = getUseDuration(stack, user) - timeLeft;
		if (useTicks >= CHARGE_TICKS) {
			fireBullet(stack, level, player);
		} else {
			restorePlayer(stack, level, player);
		}
		return true;
	}

	private static void restorePlayer(ItemStack stack, Level level, Player player) {
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}

		boolean needsHealth = player.getHealth() < player.getMaxHealth();
		boolean needsFood = player.getFoodData().needsFood();
		if (!needsHealth && !needsFood) {
			return;
		}

		if (needsHealth) {
			player.heal(2.0f);
		}
		if (needsFood) {
			player.getFoodData().eat(2, 0.0f);
		}
		consumeOne(stack, player);
		player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
		serverLevel.playSound(null, player.getX(), player.getEyeY(), player.getZ(),
				SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, 1.25f);
	}

	private static void fireBullet(ItemStack stack, Level level, Player player) {
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}

		AttackingMemberBulletEntity bullet = new AttackingMemberBulletEntity(
				serverLevel, player, AttackingMemberItem.BULLET_DAMAGE);
		bullet.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f,
				MemberWeaponItem.BULLET_SPEED, 0.15f);
		serverLevel.addFreshEntity(bullet);
		consumeOne(stack, player);
		player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
		serverLevel.playSound(null, player.getX(), player.getEyeY(), player.getZ(),
				SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.8f,
				0.75f + serverLevel.getRandom().nextFloat() * 0.15f);
	}

	private static void consumeOne(ItemStack stack, Player player) {
		if (!player.hasInfiniteMaterials()) {
			stack.shrink(1);
		}
	}
}

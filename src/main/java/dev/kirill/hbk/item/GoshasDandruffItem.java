package dev.kirill.hbk.item;

import dev.kirill.hbk.entity.GoshasDandruffEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class GoshasDandruffItem extends Item {
	public GoshasDandruffItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player.isSpectator()) return InteractionResult.FAIL;
		ItemStack stack = player.getItemInHand(hand);
		if (level instanceof ServerLevel serverLevel) {
			var projectile = new GoshasDandruffEntity(level, player);
			projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 1.5f, 1.0f);
			serverLevel.addFreshEntity(projectile);
			level.playSound(null, player.blockPosition(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS,
					0.5f, 0.8f + player.getRandom().nextFloat() * 0.4f);
			player.awardStat(Stats.ITEM_USED.get(this));
			if (!player.hasInfiniteMaterials()) stack.shrink(1);
		}
		return InteractionResult.SUCCESS;
	}
}

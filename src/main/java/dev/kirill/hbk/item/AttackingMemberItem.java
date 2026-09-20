package dev.kirill.hbk.item;

import dev.kirill.hbk.entity.AttackingMemberBulletEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** The original white-and-magenta member weapon. */
public class AttackingMemberItem extends MemberWeaponItem {
	public static final float BULLET_DAMAGE = 10.0f;

	public AttackingMemberItem(Properties properties) {
		super(properties, BULLET_DAMAGE);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		var stack = player.getItemInHand(hand);
		if (player.isSpectator() || player.getCooldowns().isOnCooldown(stack)) {
			return InteractionResult.FAIL;
		}
		if (!(level instanceof ServerLevel serverLevel)) {
			return InteractionResult.SUCCESS;
		}
		var bullet = new AttackingMemberBulletEntity(serverLevel, player, BULLET_DAMAGE);
		bullet.setExplosionPower(3.0f);
		bullet.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, BULLET_SPEED, 0.15f);
		serverLevel.addFreshEntity(bullet);
		player.getCooldowns().addCooldown(stack, 8 * 20);
		serverLevel.playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH,
				SoundSource.PLAYERS, 1.0f, 0.8f);
		player.swing(hand, true);
		return InteractionResult.SUCCESS_SERVER;
	}
}

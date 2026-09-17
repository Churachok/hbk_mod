package dev.kirill.hbk.item;

import dev.kirill.hbk.entity.AttackingMemberBulletEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;

/** Shared single-projectile behaviour for the member weapon family. */
public class MemberWeaponItem extends Item {
	public static final int DEFAULT_COOLDOWN_TICKS = 6;
	public static final float BULLET_SPEED = 2.7f;

	private final float bulletDamage;
	private final int cooldownTicks;

	public MemberWeaponItem(Properties properties, float bulletDamage) {
		this(properties, bulletDamage, DEFAULT_COOLDOWN_TICKS);
	}

	public MemberWeaponItem(Properties properties, float bulletDamage, int cooldownTicks) {
		super(properties);
		this.bulletDamage = bulletDamage;
		this.cooldownTicks = cooldownTicks;
	}

	/** Called only on the server after the selected item has been validated. */
	public void fire(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel level)) {
			return;
		}

		var weapon = player.getMainHandItem();
		if (weapon.getItem() != this || player.getCooldowns().isOnCooldown(weapon)) {
			return;
		}

		AttackingMemberBulletEntity bullet = new AttackingMemberBulletEntity(level, player, this.bulletDamage);
		bullet.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, BULLET_SPEED, 0.15f);
		level.addFreshEntity(bullet);
		level.playSound(null, player.getX(), player.getEyeY(), player.getZ(),
				SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.8f,
				0.75f + level.getRandom().nextFloat() * 0.15f);
		player.getCooldowns().addCooldown(weapon, this.cooldownTicks);
		player.swing(InteractionHand.MAIN_HAND, true);
	}
}

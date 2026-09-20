package dev.kirill.hbk.item;

import dev.kirill.hbk.entity.AttackingMemberBulletEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Pink-and-red weapon with an explosive RMB and a 360-degree projectile burst on LMB. */
public final class ColossalMemberItem extends MemberWeaponItem {
	private static final int PROJECTILE_COUNT = 100;
	private static final float PROJECTILE_DAMAGE = 40.0f;
	private static final float PROJECTILE_SPEED = 2.2f;
	private static final int BURST_COOLDOWN_TICKS = 60 * 20;
	private static final int EXPLOSION_COOLDOWN_TICKS = 40 * 20;
	public static final float EXPLOSION_POWER = 16.0f;

	private final Map<UUID, Integer> burstReadyAt = new HashMap<>();
	private final Map<UUID, Integer> explosionReadyAt = new HashMap<>();

	public ColossalMemberItem(Properties properties) {
		super(properties, PROJECTILE_DAMAGE);
	}

	@Override
	public void fire(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel level) || player.getMainHandItem().getItem() != this) {
			return;
		}
		if (player.isSpectator()) {
			return;
		}

		int now = level.getServer().getTickCount();
		UUID playerId = player.getUUID();
		if (now < this.burstReadyAt.getOrDefault(playerId, 0)) {
			return;
		}
		this.burstReadyAt.put(playerId, now + BURST_COOLDOWN_TICKS);

		for (int i = 0; i < PROJECTILE_COUNT; i++) {
			double angle = Math.PI * 2.0 * i / PROJECTILE_COUNT;
			Vec3 direction = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
			AttackingMemberBulletEntity bullet = new AttackingMemberBulletEntity(level, player, PROJECTILE_DAMAGE);
			bullet.setPos(
					player.getX() + direction.x * 0.8,
					player.getEyeY() - 0.3,
					player.getZ() + direction.z * 0.8
			);
			bullet.shoot(direction.x, 0.0, direction.z, PROJECTILE_SPEED, 0.0f);
			level.addFreshEntity(bullet);
		}

		level.playSound(null, player.getX(), player.getEyeY(), player.getZ(),
				SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 1.4f, 0.7f);
		player.swing(InteractionHand.MAIN_HAND, true);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack weapon = player.getItemInHand(hand);
		if (weapon.getItem() != this || player.isSpectator()) {
			return InteractionResult.PASS;
		}

		if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}

		int now = serverLevel.getServer().getTickCount();
		UUID playerId = serverPlayer.getUUID();
		if (now < this.explosionReadyAt.getOrDefault(playerId, 0)) {
			return InteractionResult.FAIL;
		}
		this.explosionReadyAt.put(playerId, now + EXPLOSION_COOLDOWN_TICKS);

		Vec3 blastPos = MemberDestruction.aimedPosition(serverLevel, player, 48.0);
		MemberDestruction.explode(serverLevel, player, blastPos, EXPLOSION_POWER);
		serverPlayer.swing(hand, true);
		return InteractionResult.SUCCESS_SERVER;
	}
}

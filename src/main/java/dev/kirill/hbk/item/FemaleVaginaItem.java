package dev.kirill.hbk.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** A compact black-concrete sword with a radial launch ability. */
public final class FemaleVaginaItem extends Item {
	private static final double EFFECT_RADIUS = 16.0;
	// Tuned for roughly ten blocks of horizontal travel and a five-block apex
	// under vanilla living-entity drag and gravity.
	private static final double HORIZONTAL_KNOCKBACK = 0.9;
	private static final double UPWARD_KNOCKBACK = 0.9;
	private static final int ABILITY_COOLDOWN_TICKS = 45 * 20;
	private final Map<UUID, Integer> abilityReadyAt = new HashMap<>();

	public FemaleVaginaItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}

		int now = serverLevel.getServer().getTickCount();
		UUID playerId = serverPlayer.getUUID();
		if (now < this.abilityReadyAt.getOrDefault(playerId, 0)) {
			return InteractionResult.FAIL;
		}
		this.abilityReadyAt.put(playerId, now + ABILITY_COOLDOWN_TICKS);

		AABB area = serverPlayer.getBoundingBox().inflate(EFFECT_RADIUS);
		for (LivingEntity target : serverLevel.getEntities(
				EntityTypeTest.forClass(LivingEntity.class), area,
				entity -> entity != serverPlayer && entity.distanceToSqr(serverPlayer) <= EFFECT_RADIUS * EFFECT_RADIUS)) {
			Vec3 away = target.position().subtract(serverPlayer.position());
			Vec3 horizontal = new Vec3(away.x, 0.0, away.z);
			if (horizontal.lengthSqr() < 1.0E-4) {
				horizontal = new Vec3(0.0, 0.0, 1.0);
			} else {
				horizontal = horizontal.normalize();
			}
			target.setDeltaMovement(horizontal.x * HORIZONTAL_KNOCKBACK,
					UPWARD_KNOCKBACK, horizontal.z * HORIZONTAL_KNOCKBACK);
			target.hurtMarked = true;
			target.needsSync = true;
		}

		serverLevel.sendParticles(ParticleTypes.GUST_EMITTER_LARGE,
				serverPlayer.getX(), serverPlayer.getY() + 0.5, serverPlayer.getZ(),
				3, 1.0, 0.25, 1.0, 0.0);
		serverLevel.playSound(null, serverPlayer.blockPosition(), SoundEvents.WIND_CHARGE_BURST.value(),
				SoundSource.PLAYERS, 1.4f, 0.7f);
		serverPlayer.swing(hand, true);
		return InteractionResult.SUCCESS_SERVER;
	}
}

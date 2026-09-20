package dev.kirill.hbk.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** A sword that summons a sheep on RMB. */
public final class BeastlikeMemberItem extends DestructiveMemberItem {
	private static final int SUMMON_COOLDOWN_TICKS = 120 * 20;
	private final Map<UUID, Integer> summonReadyAt = new HashMap<>();

	public BeastlikeMemberItem(Properties properties) {
		super(properties, Ability.ROAR);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player.isShiftKeyDown()) {
			return useDestruction(level, player, hand);
		}
		if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}

		int now = serverLevel.getServer().getTickCount();
		UUID playerId = serverPlayer.getUUID();
		if (now < this.summonReadyAt.getOrDefault(playerId, 0)) {
			return InteractionResult.FAIL;
		}

		Sheep sheep = EntityTypes.SHEEP.create(serverLevel, EntitySpawnReason.MOB_SUMMONED);
		if (sheep == null) {
			return InteractionResult.FAIL;
		}
		Vec3 look = serverPlayer.getLookAngle();
		double horizontalLength = Math.sqrt(look.x * look.x + look.z * look.z);
		double x = serverPlayer.getX() + (horizontalLength > 0.001 ? look.x / horizontalLength * 2.0 : 0.0);
		double z = serverPlayer.getZ() + (horizontalLength > 0.001 ? look.z / horizontalLength * 2.0 : 0.0);
		sheep.snapTo(x, serverPlayer.getY(), z, serverPlayer.getYRot(), 0.0f);
		if (!serverLevel.addFreshEntity(sheep)) {
			return InteractionResult.FAIL;
		}

		this.summonReadyAt.put(playerId, now + SUMMON_COOLDOWN_TICKS);
		serverLevel.sendParticles(ParticleTypes.POOF, sheep.getX(), sheep.getY() + 0.5, sheep.getZ(),
				18, 0.4, 0.5, 0.4, 0.04);
		serverLevel.playSound(null, sheep.blockPosition(), SoundEvents.SHEEP_AMBIENT,
				SoundSource.NEUTRAL, 1.0f, 1.0f);
		serverPlayer.swing(hand, true);
		return InteractionResult.SUCCESS_SERVER;
	}
}

package dev.kirill.hbk.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** A storm confined to the encounter area; it never changes the level's global weather. */
public final class BossLocalWeather {
	private static final double RADIUS = 32.0;
	private static final Set<UUID> STORM_PLAYERS = new HashSet<>();

	private BossLocalWeather() {
	}

	public static boolean isStormAt(ServerLevel level, BlockPos pos) {
		return !level.getEntitiesOfClass(Mob.class,
				new net.minecraft.world.phys.AABB(pos).inflate(RADIUS), mob ->
					mob.isAlive() && isBoss(mob) && mob.blockPosition().distSqr(pos) <= RADIUS * RADIUS).isEmpty();
	}

	private static boolean isBoss(Mob mob) {
		return mob instanceof StalinEntity || mob instanceof CjEntity
				|| mob instanceof KirillDoomEntity || mob instanceof MadLiberalEntity;
	}

	public static void updatePlayer(ServerLevel level, ServerPlayer player) {
		if (level.getGameTime() % 10 != 0) return;
		boolean storm = isStormAt(level, player.blockPosition());
		boolean wasStorm = STORM_PLAYERS.contains(player.getUUID());
		if (storm) {
			STORM_PLAYERS.add(player.getUUID());
			if (!wasStorm) {
				player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.START_RAINING, 0));
			}
			player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, 1.0f));
			player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, 1.0f));
		} else if (wasStorm) {
			STORM_PLAYERS.remove(player.getUUID());
			player.connection.send(new ClientboundGameEventPacket(
					level.isRaining() ? ClientboundGameEventPacket.START_RAINING
							: ClientboundGameEventPacket.STOP_RAINING, 0));
			player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE,
					level.getRainLevel(1.0f)));
			player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE,
					level.getThunderLevel(1.0f)));
		}
	}

	public static void tick(ServerLevel level, Mob boss) {
		if (boss.tickCount % 5 == 0) {
			for (var player : level.players()) {
				if (player.distanceToSqr(boss) > RADIUS * RADIUS) continue;
				level.sendParticles(ParticleTypes.RAIN, player.getX(), player.getY() + 7.0,
						player.getZ(), 32, 7.0, 1.0, 7.0, 0.3);
				if (boss instanceof StalinEntity) {
					level.sendParticles(new DustParticleOptions(0xD4211D, 1.0f), player.getX(),
							player.getY() + 2.0, player.getZ(), 13, 7.0, 3.0, 7.0, 0.02);
					level.sendParticles(ParticleTypes.ASH, player.getX(), player.getY() + 3.0,
							player.getZ(), 12, 7.0, 3.0, 7.0, 0.01);
				} else if (boss instanceof CjEntity) {
					level.sendParticles(new DustParticleOptions(0x24E25B, 1.5f), player.getX(),
							player.getY() + 2.0, player.getZ(), 16, 7.0, 3.0, 7.0, 0.08);
					level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 2.0,
							player.getZ(), 8, 7.0, 3.0, 7.0, 0.02);
				} else if (boss instanceof KirillDoomEntity) {
					level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 2.0,
							player.getZ(), 20, 7.0, 3.0, 7.0, 0.03);
					level.sendParticles(new DustParticleOptions(0x9634D7, 1.2f), player.getX(),
							player.getY() + 2.0, player.getZ(), 12, 7.0, 3.0, 7.0, 0.02);
				} else if (boss instanceof MadLiberalEntity) {
					level.sendParticles(new DustParticleOptions(0xE7C22D, 1.5f), player.getX(),
							player.getY() + 2.0, player.getZ(), 25, 7.0, 3.0, 7.0, 0.02);
					if (boss.tickCount % 20 == 0) {
						level.sendParticles(ParticleTypes.GUST, player.getX(), player.getY() + 1.0,
								player.getZ(), 6, 6.0, 0.5, 6.0, 0.05);
					}
				}
			}
		}
		if (boss.tickCount % 100 == 0 && level.players().stream()
				.anyMatch(player -> player.distanceToSqr(boss) <= RADIUS * RADIUS)) {
			BlockPos nearby = boss.blockPosition().offset(
					level.getRandom().nextInt(25) - 12, 0, level.getRandom().nextInt(25) - 12);
			BlockPos strike = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, nearby);
			LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
			if (bolt != null) {
				bolt.snapTo(strike.getX() + 0.5, strike.getY(), strike.getZ() + 0.5);
				level.addFreshEntity(bolt);
			}
			boss.playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 3.0f, 0.8f);
		}
	}
}

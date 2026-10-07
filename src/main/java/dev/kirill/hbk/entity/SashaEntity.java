package dev.kirill.hbk.entity;

import dev.kirill.hbk.registry.ModEntityTypes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Marks Sasha as hostile for vanilla targeting and the mod's other mechanics. */
public final class SashaEntity extends ReferenceNpcEntity implements Enemy {
	private int shieldCooldown = 20 * 6;
	private int shieldTicks;

	public SashaEntity(EntityType<? extends SashaEntity> type, Level level) {
		super(type, level);
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,
			DifficultyInstance difficulty, EntitySpawnReason reason,
			SpawnGroupData data) {
		var result = super.finalizeSpawn(level, difficulty, reason, data);
		if (reason == EntitySpawnReason.NATURAL && level instanceof ServerLevel server
				&& this.random.nextFloat() < 0.5f) {
			for (var pos : java.util.List.of(this.blockPosition().east(2), this.blockPosition().west(2),
					this.blockPosition().north(2), this.blockPosition().south(2))) {
				if (!server.hasChunkAt(pos) || !server.getBlockState(pos).isAir()
						|| !server.getBlockState(pos.above()).isAir() || !server.getBlockState(pos.below()).isSolid()
						|| server.players().stream().anyMatch(player -> !player.isSpectator() && player.distanceToSqr(
								pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) < 24 * 24)) continue;
				var lesha = ModEntityTypes.LESHA.create(server, reason);
				if (lesha == null) break;
				lesha.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, this.getYRot(), 0);
				if (!server.noCollision(lesha)) continue;
				lesha.finalizeSpawn(server, difficulty, reason, null);
				server.addFreshEntity(lesha);
				break;
			}
		}
		return result;
	}

	void tickShield(ServerLevel level) {
		if (this.shieldTicks > 0) {
			this.shieldTicks--;
			if (this.tickCount % 8 == 0) {
				level.sendParticles(ParticleTypes.ENCHANTED_HIT, this.getX(), this.getY(0.6), this.getZ(),
						12, 0.7, 0.8, 0.7, 0.05);
			}
		} else if (this.getTarget() != null && --this.shieldCooldown <= 0) {
			this.shieldTicks = 20 * 4;
			this.shieldCooldown = 20 * 10;
			this.playSound(SoundEvents.SHIELD_BLOCK.value(), 1.0f, 0.7f);
		}
	}

	boolean breakShield(ServerLevel level, DamageSource source) {
		if (this.shieldTicks <= 0 || !(source.getEntity() instanceof Player player)) {
			return false;
		}
		this.shieldTicks = 0;
		Vec3 away = player.position().subtract(this.position());
		if (away.horizontalDistanceSqr() > 0.01) {
			Vec3 impulse = new Vec3(away.x, 0, away.z).normalize().scale(0.8);
			player.setDeltaMovement(player.getDeltaMovement().add(impulse.x, 0.2, impulse.z));
			player.hurtMarked = true;
		}
		level.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY(0.6), this.getZ(),
				20, 0.7, 0.7, 0.7, 0.1);
		this.playSound(SoundEvents.SHIELD_BREAK.value(), 1.0f, 0.9f);
		return true;
	}
}

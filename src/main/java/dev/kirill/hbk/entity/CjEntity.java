package dev.kirill.hbk.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class CjEntity extends GiantBossEntity {
	public static final float EXPLOSION_POWER = StalinEntity.EXPLOSION_POWER * 2.0f;
	public static final double ATTACK_DAMAGE = StalinEntity.ATTACK_DAMAGE * 2.0;
	private static final float VOLLEY_MULTIPLIER = 2.5f;

	public CjEntity(EntityType<? extends CjEntity> type, Level level) {
		super(type, level, BossEvent.BossBarColor.GREEN);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createBaseAttributes(300.0, ATTACK_DAMAGE);
	}

	@Override
	public float getExplosionPower() {
		return EXPLOSION_POWER;
	}

	public void launchFireworkVolley(Vec3 target) {
		if (!(this.level() instanceof ServerLevel serverLevel)) {
			return;
		}

		float power = this.getExplosionPower() * VOLLEY_MULTIPLIER;
		Vec3 muzzle = this.getEyePosition().add(this.getLookAngle().scale(3.0));
		Vec3 base = target.subtract(muzzle);
		if (base.lengthSqr() < 0.001) {
			base = this.getLookAngle();
		} else {
			base = base.normalize();
		}

		for (int i = 0; i < 10; i++) {
			double yaw = (i - 4.5) * 0.09;
			Vec3 dir = yawVector(base, yaw).add(0.0, (this.getRandom().nextDouble() - 0.5) * 0.18, 0.0).normalize();
			GiantRocketEntity rocket = new GiantRocketEntity(serverLevel, this, dir.scale(1.45), power);
			rocket.setPos(muzzle.x, muzzle.y, muzzle.z);
			serverLevel.addFreshEntity(rocket);
		}
		this.playSound(SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 4.0f, 0.75f);
		this.playSound(SoundEvents.FIREWORK_ROCKET_LAUNCH, 4.0f, 0.5f);
	}

	private static Vec3 yawVector(Vec3 vector, double angle) {
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		return new Vec3(vector.x * cos - vector.z * sin, vector.y, vector.x * sin + vector.z * cos);
	}
}

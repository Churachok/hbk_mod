package dev.kirill.hbk.effect;

import dev.kirill.hbk.registry.ModEffects;
import dev.kirill.hbk.registry.ModEntityTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.EntityBasedExplosionDamageCalculator;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;

/** Amplifier 1 remembers that the mob was hostile when the button was pressed. */
public final class FunnySpinEffect extends MobEffect {
	public static final int DURATION_TICKS = 5 * 20;

	public FunnySpinEffect() {
		super(MobEffectCategory.HARMFUL, 0xF8CF22);
	}

	public static boolean isProtectedBoss(Entity entity) {
		var type = entity.getType();
		return type == ModEntityTypes.KIRILL_DOOM || type == ModEntityTypes.MAD_LIBERAL
				|| type == ModEntityTypes.STALIN || type == ModEntityTypes.CJ;
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
		if (!(entity instanceof Mob mob) || !mob.isAlive()) return true;
		float angle = mob.tickCount * 72.0f;
		mob.setYRot(angle);
		mob.setYBodyRot(angle);
		mob.setYHeadRot(angle);
		var effect = mob.getEffect(ModEffects.FUNNY_SPIN);
		if (amplifier > 0 && !isProtectedBoss(mob) && effect != null && effect.getDuration() == 1) {
			// Nearby victims must not bypass boss protection through explosion damage or knockback.
			var calculator = new EntityBasedExplosionDamageCalculator(mob) {
				@Override
				public boolean shouldDamageEntity(Explosion explosion, Entity victim) {
					return !isProtectedBoss(victim) && super.shouldDamageEntity(explosion, victim);
				}

				@Override
				public float getKnockbackMultiplier(Entity victim) {
					return isProtectedBoss(victim) ? 0.0f : super.getKnockbackMultiplier(victim);
				}
			};
			level.explode(mob, null, calculator, mob.getX(), mob.getY(0.5), mob.getZ(),
					2.0f, false, Level.ExplosionInteraction.NONE);
			mob.kill(level);
		}
		return true;
	}
}

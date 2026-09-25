package dev.kirill.hbk.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** Hydrophobia with a literal weakness to water. */
public final class GoshasRageEffect extends MobEffect {
	public static final int DURATION_TICKS = 2 * 60 * 20;
	private static final int WATER_DAMAGE_INTERVAL = 20;
	private static final float WATER_DAMAGE = 1.0f;

	public GoshasRageEffect(int color) {
		super(MobEffectCategory.HARMFUL, color);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public void onEffectStarted(LivingEntity entity, int amplifier) {
		if (entity.getHealth() > entity.getMaxHealth()) {
			entity.setHealth(entity.getMaxHealth());
		}
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
		if (entity.getHealth() > entity.getMaxHealth()) {
			entity.setHealth(entity.getMaxHealth());
		}
		if (entity.tickCount % WATER_DAMAGE_INTERVAL == 0 && entity.isInWaterOrRain()) {
			entity.hurtServer(level, entity.damageSources().drown(), WATER_DAMAGE);
		}
		return true;
	}
}

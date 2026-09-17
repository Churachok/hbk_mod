package dev.kirill.hbk.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public final class DiabetesEffect extends MobEffect {
	private static final int DAMAGE_INTERVAL = 20 * 10;

	public DiabetesEffect(int color) {
		super(MobEffectCategory.HARMFUL, color);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
		if (entity.tickCount % DAMAGE_INTERVAL == 0) {
			entity.hurtServer(level, entity.damageSources().magic(), 1.0f + amplifier);
		}
		return true;
	}
}

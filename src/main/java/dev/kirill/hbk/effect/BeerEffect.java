package dev.kirill.hbk.effect;

import dev.kirill.hbk.registry.ModEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/** The beer timer owns the nausea pulses; no independent player timers survive milk or death. */
public final class BeerEffect extends MobEffect {
	public static final int DURATION_TICKS = 2 * 60 * 20;
	public static final int NAUSEA_INTERVAL_TICKS = 20 * 20;
	public static final int NAUSEA_DURATION_TICKS = 8 * 20;

	public BeerEffect(int color) {
		super(MobEffectCategory.NEUTRAL, color);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return duration > 0 && duration < DURATION_TICKS
				&& duration >= NAUSEA_DURATION_TICKS && duration % NAUSEA_INTERVAL_TICKS == 0;
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
		var beer = entity.getEffect(ModEffects.BEER);
		if (beer != null) {
			entity.addEffect(new MobEffectInstance(MobEffects.NAUSEA,
					Math.min(NAUSEA_DURATION_TICKS, beer.getDuration()), 0));
		}
		return true;
	}
}

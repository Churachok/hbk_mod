package dev.kirill.hbk.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public final class HeartyLunchEffect extends MobEffect {
	private static final int HEAL_INTERVAL = 50;

	public HeartyLunchEffect(int color) {
		super(MobEffectCategory.BENEFICIAL, color);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return duration % Math.max(10, HEAL_INTERVAL >> amplifier) == 0;
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
		if (entity.getHealth() < entity.getMaxHealth()) {
			entity.heal(1.0f + amplifier);
		}
		return true;
	}
}

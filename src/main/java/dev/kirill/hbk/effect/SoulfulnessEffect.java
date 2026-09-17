package dev.kirill.hbk.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

public final class SoulfulnessEffect extends MobEffect {
	public SoulfulnessEffect(int color) {
		super(MobEffectCategory.BENEFICIAL, color);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
		if (entity instanceof Mob mob) {
			mob.setTarget(null);
			mob.getNavigation().stop();
			float danceRotation = (mob.tickCount * 24.0f) % 360.0f;
			mob.setYRot(danceRotation);
			mob.setYBodyRot(danceRotation);
			mob.setYHeadRot(danceRotation + (float) Math.sin(mob.tickCount * 0.5) * 35.0f);
		}
		return true;
	}
}

package dev.kirill.hbk.effect;

import dev.kirill.hbk.registry.ModEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

/** Amplifier 1 remembers that the mob was hostile when the button was pressed. */
public final class FunnySpinEffect extends MobEffect {
	public static final int DURATION_TICKS = 5 * 20;

	public FunnySpinEffect() {
		super(MobEffectCategory.HARMFUL, 0xF8CF22);
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
		if (amplifier > 0 && effect != null && effect.getDuration() == 1) {
			level.explode(mob, mob.getX(), mob.getY(0.5), mob.getZ(), 2.0f, Level.ExplosionInteraction.NONE);
			mob.kill(level);
		}
		return true;
	}
}

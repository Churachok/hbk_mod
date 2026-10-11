package dev.kirill.hbk.effect;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.registry.ModEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/** One effect owns every beer mechanic; milk/death cannot leave independent timers behind. */
public final class BeerEffect extends MobEffect {
	public static final int DURATION_TICKS = 2 * 60 * 20;
	public static final int NAUSEA_INTERVAL_TICKS = 20 * 20;
	public static final int NAUSEA_DURATION_TICKS = 8 * 20;

	public BeerEffect(int color) {
		super(MobEffectCategory.NEUTRAL, color);
		this.addAttributeModifier(Attributes.ATTACK_DAMAGE, HbkMod.id("beer_strength"),
				3.0, AttributeModifier.Operation.ADD_VALUE);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
		var beer = entity.getEffect(ModEffects.BEER);
		if (beer != null && beer.getDuration() % 50 == 0 && !entity.hasEffect(MobEffects.REGENERATION)
				&& entity.getHealth() < entity.getMaxHealth()) {
			entity.heal(1.0f);
		}
		if (entity instanceof Player player) {
			// Vanilla Hunger II consumes 0.005 * (1 + amplifier) exhaustion each tick.
			player.causeFoodExhaustion(0.01f);
		}
		return true;
	}

	public static boolean isNauseaActive(int remainingTicks) {
		int elapsed = DURATION_TICKS - remainingTicks;
		return remainingTicks > 0 && elapsed >= NAUSEA_INTERVAL_TICKS
				&& elapsed % NAUSEA_INTERVAL_TICKS < NAUSEA_DURATION_TICKS;
	}

	/** The client uses the same timer for camera distortion and the accessibility overlay. */
	public static float getNauseaBlend(int remainingTicks, float partialTick) {
		if (!isNauseaActive(remainingTicks)) return 0;
		float elapsedInPulse = (DURATION_TICKS - remainingTicks) % NAUSEA_INTERVAL_TICKS + partialTick;
		return Mth.clamp(Math.min(elapsedInPulse, NAUSEA_DURATION_TICKS - elapsedInPulse) / 20.0f, 0, 1);
	}
}

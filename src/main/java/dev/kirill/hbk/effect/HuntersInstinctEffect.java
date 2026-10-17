package dev.kirill.hbk.effect;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.registry.ModEffects;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** One saved timer owns the drink's buffs, vision and renewable death protection. */
public final class HuntersInstinctEffect extends MobEffect {
	public static final int DURATION_TICKS = 3 * 60 * 20;
	public static final int RESCUE_COST_TICKS = 2 * 60 * 20;
	public static final int NAUSEA_DURATION_TICKS = 8 * 20;
	public static final double HIGHLIGHT_RADIUS = 30.0;
	private static final Identifier STRENGTH_MODIFIER = HbkMod.id("hunters_instinct_strength");

	public HuntersInstinctEffect() {
		super(MobEffectCategory.BENEFICIAL, 0xD6A54A);
		addAttributeModifier(Attributes.ATTACK_DAMAGE, STRENGTH_MODIFIER, 6.0, AttributeModifier.Operation.ADD_VALUE);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
		syncStrengthModifier(entity);
		var effect = entity.getEffect(ModEffects.HUNTERS_INSTINCT);
		int regenPhase = effect != null && effect.isInfiniteDuration() ? entity.tickCount
				: effect == null ? -1 : effect.getDuration();
		if (effect != null && regenPhase % 50 == 0 && !entity.hasEffect(MobEffects.REGENERATION)
				&& entity.getHealth() < entity.getMaxHealth()) {
			entity.heal(1.0f);
		}
		return true;
	}

	/** Supplement weaker vanilla Strength rather than adding a second full Strength II bonus. */
	public static void syncStrengthModifier(LivingEntity entity) {
		var damage = entity.getAttribute(Attributes.ATTACK_DAMAGE);
		if (damage == null) return;
		var current = damage.getModifier(STRENGTH_MODIFIER);
		var vanillaStrength = entity.getEffect(MobEffects.STRENGTH);
		double vanillaBonus = vanillaStrength == null ? 0.0 : 3.0 * (vanillaStrength.getAmplifier() + 1);
		double bonus = entity.hasEffect(ModEffects.HUNTERS_INSTINCT) ? Math.max(0.0, 6.0 - vanillaBonus) : 0.0;
		if (bonus == 0.0) {
			if (current != null) damage.removeModifier(STRENGTH_MODIFIER);
		} else if (current == null || Double.compare(current.amount(), bonus) != 0) {
			// Native MobEffect modifiers are saved as permanent; keep both maps consistent across reloads.
			damage.addOrReplacePermanentModifier(new AttributeModifier(STRENGTH_MODIFIER, bonus, AttributeModifier.Operation.ADD_VALUE));
		}
	}
}

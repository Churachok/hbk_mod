package dev.kirill.hbk.mechanic;

import dev.kirill.hbk.effect.HuntersInstinctEffect;
import dev.kirill.hbk.network.JagermeisterRescuePayload;
import dev.kirill.hbk.registry.ModEffects;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

public final class HuntersInstinct {
	private HuntersInstinct() {
	}

	/** Each sip adds three minutes, never another effect level or another attribute modifier. */
	public static void drink(LivingEntity entity) {
		if (entity.level().isClientSide()) return;
		var current = entity.getEffect(ModEffects.HUNTERS_INSTINCT);
		int duration = current == null ? HuntersInstinctEffect.DURATION_TICKS
				: current.isInfiniteDuration() ? -1
				: (int) Math.min(Integer.MAX_VALUE, (long) current.getDuration() + HuntersInstinctEffect.DURATION_TICKS);
		if (current != null && current.getAmplifier() != 0) entity.removeEffect(ModEffects.HUNTERS_INSTINCT);
		entity.addEffect(new MobEffectInstance(ModEffects.HUNTERS_INSTINCT, duration, 0, false, true, true));
		HuntersInstinctEffect.syncStrengthModifier(entity);
		entity.addEffect(new MobEffectInstance(MobEffects.NAUSEA, HuntersInstinctEffect.NAUSEA_DURATION_TICKS, 0));
	}

	public static void register() {
		// Fabric's death veto runs before vanilla checks held totems: leave their native path in charge.
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, damageAmount) -> {
			var effect = entity.getEffect(ModEffects.HUNTERS_INSTINCT);
			if (effect == null || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
					|| !(entity.level() instanceof ServerLevel level)) return true;
			for (var hand : InteractionHand.values()) {
				if (entity.getItemInHand(hand).get(DataComponents.DEATH_PROTECTION) != null) return true;
			}
			int remaining = effect.isInfiniteDuration() ? -1
					: Math.max(0, effect.getDuration() - HuntersInstinctEffect.RESCUE_COST_TICKS);
			entity.removeEffect(ModEffects.HUNTERS_INSTINCT);
			if (remaining != 0) {
				entity.addEffect(new MobEffectInstance(ModEffects.HUNTERS_INSTINCT, remaining, 0,
						effect.isAmbient(), effect.isVisible(), effect.showIcon()));
			}
			entity.setHealth(Math.min(2.0f, entity.getMaxHealth()));
			entity.fallDistance = 0;
			level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5,
					entity.getZ(), 40, 0.5, 0.7, 0.5, 0.2);
			level.playSound(null, entity.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
			if (entity instanceof ServerPlayer player) JagermeisterRescuePayload.sendTo(player);
			return false;
		});
	}
}

package dev.kirill.hbk.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.kirill.hbk.effect.HuntersInstinctEffect;
import dev.kirill.hbk.mechanic.FurryTransformation;
import dev.kirill.hbk.registry.ModEffects;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class HuntersInstinctMixin {
	@WrapOperation(method = "getDamageAfterMagicAbsorb", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/LivingEntity;hasEffect(Lnet/minecraft/core/Holder;)Z"))
	private boolean hbk$hasEffectiveResistance(LivingEntity entity, Holder<MobEffect> effect, Operation<Boolean> original) {
		return original.call(entity, effect)
				|| effect == MobEffects.RESISTANCE && entity.hasEffect(ModEffects.HUNTERS_INSTINCT);
	}

	@WrapOperation(method = "getDamageAfterMagicAbsorb", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/LivingEntity;getEffect(Lnet/minecraft/core/Holder;)Lnet/minecraft/world/effect/MobEffectInstance;"))
	private MobEffectInstance hbk$getEffectiveResistance(LivingEntity entity, Holder<MobEffect> effect,
			Operation<MobEffectInstance> original) {
		var resistance = original.call(entity, effect);
		if (effect == MobEffects.RESISTANCE && entity.hasEffect(ModEffects.HUNTERS_INSTINCT)
				&& (resistance == null || resistance.getAmplifier() < 1)) {
			// Let vanilla apply its own damage tags, enchantments and resisted-damage statistics.
			return new MobEffectInstance(MobEffects.RESISTANCE, 1, 1, false, false, false);
		}
		return resistance;
	}

	@Inject(method = "tickEffects", at = @At("HEAD"))
	private void hbk$applyFurryRegenerationBeforeNativeEffects(CallbackInfo ci) {
		if ((Object) this instanceof ServerPlayer player) FurryTransformation.tickRegeneration(player);
	}

	@Inject(method = {"onEffectAdded", "onEffectUpdated", "onEffectsRemoved"}, at = @At("TAIL"))
	private void hbk$keepOnlyTheStrongerStrengthBonus(CallbackInfo ci) {
		HuntersInstinctEffect.syncStrengthModifier((LivingEntity) (Object) this);
	}
}

package dev.kirill.hbk.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.kirill.hbk.mechanic.FurryTransformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Keep native potion duration/hidden effects, supplementing only a weaker regeneration interval. */
@Mixin(MobEffectInstance.class)
public abstract class FurryRegenerationMixin {
	@WrapOperation(method = "tickServer", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/effect/MobEffect;shouldApplyEffectTickThisTick(II)Z"))
	private boolean hbk$useTheStrongerRegenerationInterval(MobEffect effect, int phase, int amplifier,
			Operation<Boolean> original, ServerLevel level, LivingEntity entity, Runnable onChanged) {
		if (amplifier < 1 && ((MobEffectInstance) (Object) this).is(MobEffects.REGENERATION)
				&& FurryTransformation.hasLoveRegeneration(entity)) {
			amplifier = 1;
		}
		return original.call(effect, phase, amplifier);
	}
}

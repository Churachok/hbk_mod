package dev.kirill.hbk.client.mixin;

import dev.kirill.hbk.effect.BeerEffect;
import dev.kirill.hbk.registry.ModEffects;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Feed vanilla nausea rendering without adding another status effect or HUD icon. */
@Mixin(LivingEntity.class)
public abstract class BeerNauseaMixin {
	@Inject(method = "getEffectBlendFactor", at = @At("RETURN"), cancellable = true)
	private void hbk$blendIntoxicationNausea(Holder<MobEffect> effect, float partialTick,
			CallbackInfoReturnable<Float> cir) {
		if (effect == MobEffects.NAUSEA && (Object) this instanceof LocalPlayer player) {
			var beer = player.getEffect(ModEffects.BEER);
			if (beer != null) {
				cir.setReturnValue(Math.max(cir.getReturnValue(),
						BeerEffect.getNauseaBlend(beer.getDuration(), partialTick)));
			}
		}
	}
}

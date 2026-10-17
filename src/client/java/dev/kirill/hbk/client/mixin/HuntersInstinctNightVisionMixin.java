package dev.kirill.hbk.client.mixin;

import dev.kirill.hbk.client.HuntersInstinctClient;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class HuntersInstinctNightVisionMixin {
	@Inject(method = "nightVisionScale", at = @At("HEAD"), cancellable = true)
	private static void hbk$fullHuntersNightVision(LivingEntity viewer, float partialTick, CallbackInfoReturnable<Float> cir) {
		// Vanilla otherwise dereferences getEffect(NIGHT_VISION), which intentionally remains absent.
		if (HuntersInstinctClient.hasNightVision(viewer)) cir.setReturnValue(1.0f);
	}
}

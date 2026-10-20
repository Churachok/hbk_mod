package dev.kirill.hbk.client.mixin;

import dev.kirill.hbk.client.FracturedBlockClient;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class FracturedFovMixin {
	@Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
	private void hbk$widenNearFractures(float partialTick, CallbackInfoReturnable<Float> cir) {
		if (((Camera) (Object) this).isPanoramicMode()) return;
		float strength = FracturedBlockClient.strength(partialTick) * Minecraft.getInstance().options.screenEffectScale().get().floatValue();
		float base = cir.getReturnValue();
		if (strength > 0) cir.setReturnValue(Math.max(base, Math.min(150, base + 45 * strength)));
	}
}

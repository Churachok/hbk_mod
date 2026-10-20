package dev.kirill.hbk.client.mixin;

import dev.kirill.hbk.client.AnomalyClient;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapRenderStateExtractor.class)
public abstract class AnomalyLightmapMixin {
	@Shadow private boolean needsUpdate;
	@Unique private boolean hbk$wasBright;

	@Inject(method = "extract", at = @At("HEAD"))
	private void hbk$refreshAtBoundary(LightmapRenderState state, float partialTick, CallbackInfo ci) {
		boolean active = AnomalyClient.active();
		if (active || hbk$wasBright) needsUpdate = true;
		hbk$wasBright = active;
	}

	@Inject(method = "extract", at = @At("RETURN"))
	private void hbk$fullAnomalyLight(LightmapRenderState state, float partialTick, CallbackInfo ci) {
		if (!AnomalyClient.active()) return;
		state.needsUpdate = true;
		state.ambientColor = LightmapRenderStateExtractor.WHITE;
		state.darknessEffectScale = 0;
		state.bossOverlayWorldDarkening = 0;
	}
}

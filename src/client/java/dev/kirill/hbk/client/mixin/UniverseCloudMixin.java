package dev.kirill.hbk.client.mixin;

import dev.kirill.hbk.universe.HiddenParameter;
import dev.kirill.hbk.universe.UniverseProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CloudRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CloudRenderer.class)
public abstract class UniverseCloudMixin {
	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void hbk$noInvisibleClouds(CallbackInfo ci) {
		var level = Minecraft.getInstance().level;
		var p = level == null ? null : UniverseProfile.of(level);
		if (p != null && (!p.openSky() || p.get(HiddenParameter.CLOUD_COVER) == 0)) ci.cancel();
	}
}

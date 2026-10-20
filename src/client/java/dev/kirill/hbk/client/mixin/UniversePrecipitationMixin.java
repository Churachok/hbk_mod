package dev.kirill.hbk.client.mixin;

import dev.kirill.hbk.universe.HiddenParameter;
import dev.kirill.hbk.universe.UniverseProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.client.renderer.state.level.WeatherRenderState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WeatherEffectRenderer.class)
public abstract class UniversePrecipitationMixin {
	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void hbk$particleWeather(ClientLevel level, float partial, Vec3 camera, WeatherRenderState state, CallbackInfo ci) {
		var p = UniverseProfile.of(level);
		if (p != null && p.get(HiddenParameter.PRECIPITATION_TYPE) != 0) { state.rainColumns.clear(); state.snowColumns.clear(); }
	}
}

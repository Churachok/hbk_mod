package dev.kirill.hbk.mixin;

import dev.kirill.hbk.universe.UniverseProfile;
import dev.kirill.hbk.universe.UniverseWeather;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.WeatherData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class UniverseWeatherMixin {
	@Inject(method = "getWeatherData", at = @At("RETURN"), cancellable = true)
	private void hbk$ownWeather(CallbackInfoReturnable<WeatherData> cir) {
		var level = (ServerLevel) (Object) this;
		if (UniverseProfile.of(level) != null) cir.setReturnValue(level.getDataStorage().computeIfAbsent(WeatherData.TYPE));
	}
	@Inject(method = "advanceWeatherCycle", at = @At("HEAD"), cancellable = true)
	private void hbk$weatherCycle(CallbackInfo ci) {
		var level = (ServerLevel) (Object) this;
		var profile = UniverseProfile.of(level);
		if (profile != null) { UniverseWeather.tick(level, profile); ci.cancel(); }
	}
}

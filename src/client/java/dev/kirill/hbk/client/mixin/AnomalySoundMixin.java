package dev.kirill.hbk.client.mixin;

import dev.kirill.hbk.client.AnomalyAudio;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public abstract class AnomalySoundMixin {
	@Inject(method = "calculateVolume(FLnet/minecraft/sounds/SoundSource;)F", at = @At("RETURN"), cancellable = true)
	private void hbk$auditoryOutage(float volume, SoundSource source, CallbackInfoReturnable<Float> cir) {
		float gain = AnomalyAudio.volumeMultiplier();
		if (gain < 1) cir.setReturnValue(cir.getReturnValue() * gain);
	}
}

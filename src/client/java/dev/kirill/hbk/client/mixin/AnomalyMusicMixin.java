package dev.kirill.hbk.client.mixin;

import dev.kirill.hbk.client.AnomalyClient;
import dev.kirill.hbk.registry.ModSounds;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public abstract class AnomalyMusicMixin {
	@Inject(method = "play", at = @At("HEAD"), cancellable = true)
	private void hbk$silenceAnomalyMusic(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
		if ((sound.getSource() == SoundSource.MUSIC || sound.getSource() == SoundSource.RECORDS) && AnomalyClient.active()
				&& !sound.getIdentifier().equals(ModSounds.ANOMALY_AMBIENT.location())) {
			cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
		}
	}
}

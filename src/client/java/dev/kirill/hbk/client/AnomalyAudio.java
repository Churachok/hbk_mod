package dev.kirill.hbk.client;

import dev.kirill.hbk.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundSource;

/** One shared fade for auditory outages and desaturation; player volume settings stay intact. */
public final class AnomalyAudio {
	private static final int FADE_TICKS = 40;
	private static final int SILENCE_TICKS = 200;
	private static ClientLevel level;
	private static long nextEvent;
	private static long fadeAt;
	private static long resumeAt;
	private static long elapsedTicks;
	private static volatile float outageStrength;
	private static float previousStrength;

	private AnomalyAudio() {}

	private static boolean eligible(Minecraft client) {
		// Death-screen audio may start before the end-of-tick cleanup runs.
		return client.level != null && client.level == level && client.player != null
				&& client.player.isAlive() && AnomalyClient.active();
	}

	public static float volumeMultiplier() {
		return outageStrength > 0 && eligible(Minecraft.getInstance()) ? 1 - outageStrength : 1;
	}

	public static float desaturation(float partialTick) {
		if (!eligible(Minecraft.getInstance())) return 0;
		float delta = Math.clamp(partialTick, 0, 1);
		return previousStrength + (outageStrength - previousStrength) * delta;
	}

	public static void clear() {
		boolean refresh = outageStrength > 0;
		outageStrength = previousStrength = 0;
		level = null;
		nextEvent = fadeAt = resumeAt = elapsedTicks = 0;
		if (refresh) Minecraft.getInstance().getSoundManager().refreshCategoryVolume(SoundSource.MASTER);
	}

	public static void tick(Minecraft client) {
		if (client.level != level) { clear(); level = client.level; }
		if (!eligible(client)) {
			clear();
			return;
		}
		if (client.isPaused()) return;
		// A local clock cannot rewind when a server time-synchronization packet arrives.
		long now = ++elapsedTicks;
		previousStrength = outageStrength;
		if (nextEvent == 0) nextEvent = now + 20 * (45 + client.level.getRandom().nextInt(76));
		if (fadeAt == 0 && now >= nextEvent) {
			client.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.ANOMALY_GLITCH, 1, 0.7f));
			fadeAt = now + 14; // Let the complete 0.65-second prelude play before fading.
			resumeAt = fadeAt + FADE_TICKS + SILENCE_TICKS;
		}
		if (fadeAt != 0 && now >= resumeAt + FADE_TICKS) {
			fadeAt = resumeAt = 0;
			nextEvent = now + 20 * (45 + client.level.getRandom().nextInt(76));
		}
		float strength = 0;
		if (fadeAt != 0) {
			// Keep ten full seconds between the two fades, with flat endpoints for both senses.
			float ramp = Math.clamp(Math.min((now - fadeAt) / (float) FADE_TICKS,
					1 - (now - resumeAt) / (float) FADE_TICKS), 0, 1);
			strength = ramp * ramp * (3 - 2 * ramp);
		}
		if (strength != outageStrength) {
			outageStrength = strength;
			// Recalculate ongoing sounds as well as newly started ones, including the ambient loop.
			client.getSoundManager().refreshCategoryVolume(SoundSource.MASTER);
		}
	}
}

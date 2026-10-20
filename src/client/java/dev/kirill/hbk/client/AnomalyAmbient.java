package dev.kirill.hbk.client;

import dev.kirill.hbk.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** One streamed music loop per client, kept running silently during auditory outages. */
public final class AnomalyAmbient {
	private static final int FADE_TICKS = 40;
	private static ClientLevel level;
	private static AmbientLoop current;
	private static int retryTicks;

	private AnomalyAmbient() {}

	private static boolean hasListener(Minecraft client) {
		return client.level != null && client.player != null && client.player.isAlive();
	}

	public static void clear(Minecraft client) {
		if (current != null) {
			current.finish();
			client.getSoundManager().stop(current);
			current = null;
		}
		level = null;
		retryTicks = 0;
	}

	public static void tick(Minecraft client) {
		if (client.level != level) { clear(client); level = client.level; }
		if (!hasListener(client)) { clear(client); return; }
		if (current != null && (current.isStopped() || current.started && !client.getSoundManager().isActive(current))) {
			current.finish();
			client.getSoundManager().stop(current);
			current = null;
		}
		if (current != null) return;
		if (!AnomalyClient.active()) { retryTicks = 0; return; }
		if (retryTicks > 0) { retryTicks--; return; }
		// Remove music already playing when entering; subsequent music starts are filtered by the engine mixin.
		client.getMusicManager().stopPlaying();
		client.getSoundManager().stop(null, SoundSource.MUSIC);
		client.getSoundManager().stop(null, SoundSource.RECORDS);
		var loop = new AmbientLoop(client);
		if (client.getSoundManager().play(loop) != SoundEngine.PlayResult.NOT_STARTED) current = loop;
		else retryTicks = 20;
	}

	private static final class AmbientLoop extends AbstractTickableSoundInstance {
		private final Minecraft client;
		private final ClientLevel sourceLevel;
		private boolean started;
		private int fadeTicks;

		private AmbientLoop(Minecraft client) {
			super(ModSounds.ANOMALY_AMBIENT, SoundSource.MUSIC, RandomSource.create());
			this.client = client;
			this.sourceLevel = client.level;
			this.volume = 0;
			this.pitch = 1;
			this.looping = true;
			this.delay = 0;
			this.relative = true;
			this.attenuation = SoundInstance.Attenuation.NONE;
		}

		@Override
		public boolean canStartSilent() { return true; }

		@Override
		public void tick() {
			started = true;
			if (client.level != sourceLevel || !hasListener(client)) { stop(); return; }
			boolean inside = AnomalyClient.active();
			// Auditory outages use the shared engine gain; this envelope only follows zone boundaries.
			if (inside) {
				fadeTicks = Math.min(FADE_TICKS, fadeTicks + 1);
			} else {
				fadeTicks = Math.max(0, fadeTicks - 1);
			}
			float progress = fadeTicks / (float) FADE_TICKS;
			this.volume = progress * progress * (3 - 2 * progress);
			if (!inside && fadeTicks == 0) stop();
		}

		private void finish() { stop(); }
	}
}

package dev.kirill.hbk.client;

import dev.kirill.hbk.entity.KirillDoomEntity;
import dev.kirill.hbk.registry.ModSounds;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public final class KirillDoomMusic {
	private static final double MAX_DISTANCE_SQUARED = 96.0 * 96.0;
	private static KirillDoomMusicInstance activeMusic;

	private KirillDoomMusic() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(KirillDoomMusic::tick);
	}

	private static void tick(Minecraft client) {
		if (client.level == null || client.player == null) {
			stop(client);
			return;
		}
		if (activeMusic != null && (activeMusic.isStopped()
				|| activeMusic.hasStarted() && !client.getSoundManager().isActive(activeMusic))) {
			activeMusic = null;
		}
		if (activeMusic != null) {
			client.getMusicManager().stopPlaying();
			return;
		}

		KirillDoomEntity boss = null;
		double closestDistance = MAX_DISTANCE_SQUARED;
		for (var entity : client.level.entitiesForRendering()) {
			if (entity instanceof KirillDoomEntity kirill && kirill.isAlive()) {
				double distance = client.player.distanceToSqr(kirill);
				if (distance <= closestDistance) {
					boss = kirill;
					closestDistance = distance;
				}
			}
		}
		if (boss != null) {
			client.getMusicManager().stopPlaying();
			activeMusic = new KirillDoomMusicInstance(client, boss);
			client.getSoundManager().play(activeMusic);
		}
	}

	private static void stop(Minecraft client) {
		if (activeMusic != null) {
			client.getSoundManager().stop(activeMusic);
			activeMusic = null;
		}
	}

	private static final class KirillDoomMusicInstance extends AbstractTickableSoundInstance {
		private final Minecraft client;
		private final KirillDoomEntity boss;
		private boolean started;

		private KirillDoomMusicInstance(Minecraft client, KirillDoomEntity boss) {
			super(ModSounds.KIRILL_DOOM_MUSIC, SoundSource.MUSIC, RandomSource.create());
			this.client = client;
			this.boss = boss;
			this.volume = 1.0f;
			this.pitch = 1.0f;
			this.looping = true;
			this.attenuation = SoundInstance.Attenuation.NONE;
			this.relative = true;
		}

		@Override
		public boolean canStartSilent() {
			return true;
		}

		@Override
		public void tick() {
			this.started = true;
			if (this.client.level == null || this.client.player == null || !this.boss.isAlive()
					|| this.boss.isRemoved()
					|| this.client.player.distanceToSqr(this.boss) > MAX_DISTANCE_SQUARED) {
				this.stop();
			}
		}

		private boolean hasStarted() {
			return this.started;
		}
	}
}

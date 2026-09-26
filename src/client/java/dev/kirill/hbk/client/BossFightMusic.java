package dev.kirill.hbk.client;

import dev.kirill.hbk.entity.KirillDoomEntity;
import dev.kirill.hbk.entity.MadLiberalEntity;
import dev.kirill.hbk.registry.ModSounds;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;

public final class BossFightMusic {
	private static final double MAX_DISTANCE_SQUARED = 96.0 * 96.0;
	private static BossMusicInstance activeMusic;

	private BossFightMusic() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(BossFightMusic::tick);
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

		LivingEntity closestBoss = null;
		SoundEvent closestMusic = null;
		double closestDistance = MAX_DISTANCE_SQUARED;
		for (var entity : client.level.entitiesForRendering()) {
			SoundEvent music = musicFor(entity);
			if (music != null && entity instanceof LivingEntity boss && boss.isAlive()) {
				double distance = client.player.distanceToSqr(boss);
				if (distance <= closestDistance) {
					closestBoss = boss;
					closestMusic = music;
					closestDistance = distance;
				}
			}
		}
		if (closestBoss != null) {
			client.getMusicManager().stopPlaying();
			activeMusic = new BossMusicInstance(client, closestBoss, closestMusic);
			client.getSoundManager().play(activeMusic);
		}
	}

	private static SoundEvent musicFor(net.minecraft.world.entity.Entity entity) {
		if (entity instanceof KirillDoomEntity) {
			return ModSounds.KIRILL_DOOM_MUSIC;
		}
		if (entity instanceof MadLiberalEntity) {
			return ModSounds.MAD_LIBERAL_MUSIC;
		}
		return null;
	}

	private static void stop(Minecraft client) {
		if (activeMusic != null) {
			client.getSoundManager().stop(activeMusic);
			activeMusic = null;
		}
	}

	private static final class BossMusicInstance extends AbstractTickableSoundInstance {
		private final Minecraft client;
		private final LivingEntity boss;
		private boolean started;

		private BossMusicInstance(Minecraft client, LivingEntity boss, SoundEvent music) {
			super(music, SoundSource.MUSIC, RandomSource.create());
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

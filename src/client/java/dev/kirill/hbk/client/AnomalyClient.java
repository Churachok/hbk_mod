package dev.kirill.hbk.client;

import dev.kirill.hbk.network.ModNetworking;
import dev.kirill.hbk.world.AnomalyZones;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

public final class AnomalyClient {
	private static final int[] COLORS = {0xFF3A89, 0x27D9FF, 0xA34DFF, 0xFFE438, 0x48FF91, 0xFF7634};
	private AnomalyClient() {}

	public static boolean active() {
		var player = Minecraft.getInstance().player;
		return player != null && AnomalyZones.contains(player);
	}

	public static void register() {
		net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.attachElementAfter(
				net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements.MISC_OVERLAYS,
				dev.kirill.hbk.HbkMod.id("anomaly_proximity"), AnomalyProximityOverlay::render);
		ClientPlayNetworking.registerGlobalReceiver(ModNetworking.AnomalyLayoutPayload.TYPE,
				(payload, context) -> context.client().execute(() -> AnomalyZones.setClientSeed(payload.seed())));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			AnomalyAmbient.clear(client);
			AnomalyZones.setClientSeed(null);
			AnomalyGlitchRenderer.clear();
			AnomalyAudio.clear();
			AnomalyScreenEffects.clear();
			FracturedBlockClient.clear();
		});
		LevelRenderEvents.END_MAIN.register(AnomalyCeilingRenderer::render);
		LevelRenderEvents.END_MAIN.register(AnomalyGlitchRenderer::render);
		LevelRenderEvents.END_MAIN.register(FracturedBlockRenderer::render);
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			AnomalyAudio.tick(client);
			AnomalyAmbient.tick(client);
			AnomalyScreenEffects.tick(client);
			FracturedBlockClient.tick(client);
			if (client.level == null || client.player == null || !active()) return;
			client.getMusicManager().stopPlaying();
			client.getSoundManager().stop(null, SoundSource.RECORDS);
			var random = client.level.getRandom();
			for (int i = 0; i < 16; i++) {
				double x = client.player.getX() + (random.nextDouble() - 0.5) * 64;
				double y = client.player.getY() + (random.nextDouble() - 0.4) * 40;
				double z = client.player.getZ() + (random.nextDouble() - 0.5) * 64;
				if (AnomalyZones.at(client.level, x, z) != null) client.level.addParticle(
						new DustParticleOptions(COLORS[random.nextInt(COLORS.length)], 0.8f + random.nextFloat()),
						x, y, z, (random.nextDouble() - 0.5) * 0.04, 0.025, (random.nextDouble() - 0.5) * 0.04);
			}
		});
	}
}

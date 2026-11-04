package dev.kirill.hbk.client;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.network.ModNetworking;
import dev.kirill.hbk.registry.ModSounds;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public final class GodEncounterClient {
	private static boolean active;
	private static double distance;
	private static int titleTicks;
	private static ClientLevel sourceLevel;
	private static GodLoop loop;
	private GodEncounterClient() {}
	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(ModNetworking.GodEncounterPayload.TYPE, (payload, context) -> context.client().execute(() -> {
			active = payload.active();
			distance = payload.distance();
			sourceLevel = context.client().level;
			if (payload.announce()) titleTicks = 40;
			if (!active) clear(context.client());
		}));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear(client));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.level != sourceLevel || client.player == null || !client.player.isAlive()) { clear(client); return; }
			if (client.isPaused()) return;
			if (titleTicks > 0) titleTicks--;
			if (!active) return;
			if (loop != null && (loop.isStopped() || loop.started && !client.getSoundManager().isActive(loop))) {
				client.getSoundManager().stop(loop);
				loop = null;
			}
			if (loop == null) {
				var next = new GodLoop();
				if (client.getSoundManager().play(next) != SoundEngine.PlayResult.NOT_STARTED) loop = next;
			}
		});
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, HbkMod.id("god_arrival"), (graphics, delta) -> {
			if (titleTicks <= 0) return;
			var client = Minecraft.getInstance();
			Component message = Component.translatable("message.hbk.god_here");
			int textWidth = client.font.width(message);
			float scale = Math.min(graphics.guiWidth() * 0.88f / Math.max(1, textWidth), graphics.guiHeight() / 20f);
			graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), 0xA0000000);
			graphics.pose().pushMatrix();
			graphics.pose().translate(graphics.guiWidth() / 2f, graphics.guiHeight() / 2f);
			graphics.pose().scale(scale, scale);
			graphics.text(client.font, message, -textWidth / 2, -client.font.lineHeight / 2, 0xFFFFFFFF, true);
			graphics.pose().popMatrix();
		});
	}
	public static boolean active() { return active; }
	public static boolean soundPlaying() { return loop != null && Minecraft.getInstance().getSoundManager().isActive(loop); }
	public static int titleTicks() { return titleTicks; }
	public static float volumeAt(double blocks) {
		double proximity = 1 - Math.clamp(blocks / 200, 0, 1);
		return (float) (0.03 + 0.97 * proximity * proximity);
	}
	private static void clear(Minecraft client) {
		active = false; titleTicks = 0;
		if (loop != null) { loop.finish(); client.getSoundManager().stop(loop); loop = null; }
		sourceLevel = null;
	}
	private static final class GodLoop extends AbstractTickableSoundInstance {
		private boolean started;
		private GodLoop() {
			super(ModSounds.GOD_LOOP, SoundSource.HOSTILE, RandomSource.create());
			volume = volumeAt(distance); pitch = 1; looping = true; delay = 0;
			relative = true; attenuation = SoundInstance.Attenuation.NONE;
		}
		@Override public void tick() {
			started = true;
			if (!active) { stop(); return; }
			volume = volumeAt(distance);
		}
		private void finish() { stop(); }
	}
}

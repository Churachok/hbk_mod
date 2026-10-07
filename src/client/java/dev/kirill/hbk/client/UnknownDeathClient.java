package dev.kirill.hbk.client;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.network.ModNetworking;
import dev.kirill.hbk.registry.ModSounds;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;

/** Plays the supplied 25 fps death clip on top of the death screen. */
public final class UnknownDeathClient {
	private static final Identifier ATLAS = HbkMod.id("textures/gui/unknown_death_atlas.png");
	private static final int FRAME_COUNT = 20;
	private static final int FRAMES_PER_SECOND = 25;
	private static boolean queued;
	private static long queuedAtNanos;
	private static long startNanos;

	private UnknownDeathClient() {
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(ModNetworking.UnknownDeathPayload.TYPE, (payload, context) ->
				context.client().execute(() -> {
					queued = true;
					queuedAtNanos = System.nanoTime();
					startNanos = 0;
				})
		);
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.level == null || queued && startNanos == 0
					&& System.nanoTime() - queuedAtNanos > 5_000_000_000L) {
				reset();
			}
		});
	}

	public static void render(GuiGraphicsExtractor graphics, int width, int height) {
		if (!queued) return;
		if (startNanos == 0) {
			startNanos = System.nanoTime();
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.UNKNOWN_DEATH, 1.0f, 1.0f));
		}
		long elapsed = System.nanoTime() - startNanos;
		int frame = (int) (elapsed * FRAMES_PER_SECOND / 1_000_000_000L);
		if (frame >= FRAME_COUNT) {
			queued = false;
			return;
		}

		// Letterbox the 16:9 source instead of stretching it on other monitors.
		graphics.fill(0, 0, width, height, 0xFF000000);
		double scale = Math.min(width / 16.0, height / 9.0);
		int drawWidth = (int) Math.round(scale * 16.0);
		int drawHeight = (int) Math.round(scale * 9.0);
		int x = (width - drawWidth) / 2;
		int y = (height - drawHeight) / 2;
		int column = frame % 4;
		int row = frame / 4;
		graphics.blit(ATLAS, x, y, x + drawWidth, y + drawHeight,
				column / 4.0f, row / 5.0f, (column + 1) / 4.0f, (row + 1) / 5.0f);
	}

	public static void reset() {
		queued = false;
		queuedAtNanos = 0;
		startNanos = 0;
	}
}

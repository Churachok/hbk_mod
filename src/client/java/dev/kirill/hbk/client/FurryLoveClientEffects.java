package dev.kirill.hbk.client;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.entity.PinkFurryWolfEntity;
import dev.kirill.hbk.mechanic.FurryBreeding;
import dev.kirill.hbk.mechanic.FurryTransformation;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import java.util.ArrayList;
import java.util.List;

/** Owner-only love vision: a steady red wash and render outlines, never world glowing flags. */
public final class FurryLoveClientEffects {
	public static final int OVERLAY_COLOR = 0x30D2263E;
	private static final int HEART_COUNT = 8;
	private static final int[] HEART_ROWS = {0b0110110, 0b1111111, 0b1111111, 0b0111110, 0b0011100, 0b0001000};

	private FurryLoveClientEffects() {
	}

	public static void register() {
		HudElementRegistry.attachElementBefore(VanillaHudElements.CROSSHAIR,
				HbkMod.id("furry_love"), FurryLoveClientEffects::render);
	}

	public static boolean isActive() {
		var viewer = Minecraft.getInstance().player;
		return viewer != null && viewer.isAlive() && FurryTransformation.isActive(viewer)
				&& FurryBreeding.isInLove(viewer);
	}

	public static boolean shouldHighlight(Entity entity) {
		var viewer = Minecraft.getInstance().player;
		return isActive() && entity != viewer && entity instanceof LivingEntity living && living.isAlive()
				&& !entity.isRemoved() && entity.level() == viewer.level()
				&& (entity instanceof PinkFurryWolfEntity
						|| entity instanceof Player player && FurryTransformation.isActive(player));
	}

	private static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		if (!isActive()) return;
		graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), OVERLAY_COLOR);
		float ticks = Minecraft.getInstance().player.tickCount + deltaTracker.getGameTimeDeltaPartialTick(false);
		for (Heart heart : heartsForHud(graphics.guiWidth(), graphics.guiHeight(), ticks)) {
			drawHeart(graphics, heart);
		}
	}

	/** Deterministic game-time animation; opening an inventory or ending love hides every heart immediately. */
	public static List<Heart> heartsForHud(int width, int height, float ticks) {
		if (!isActive() || Minecraft.getInstance().gui.screen() != null || width < 100 || height < 100) return List.of();
		var hearts = new ArrayList<Heart>(HEART_COUNT);
		float top = 28;
		float bottom = height - 54;
		for (int index = 0; index < HEART_COUNT; index++) {
			float phase = ((ticks + index * 30) % 240) / 240.0f;
			float inset = 8 + (index / 2 % 2) * 14;
			float x = (index % 2 == 0 ? inset : width - 7 - inset)
					+ (float) Math.sin(ticks * 0.04f + index * 1.7f) * 2.5f;
			float y = bottom - phase * (bottom - top);
			// Fade continuously at the loop seam instead of blinking at respawn.
			float alpha = 0.85f * Math.min(1.0f, Math.min(phase, 1.0f - phase) * 10.0f);
			hearts.add(new Heart(x, y, alpha));
		}
		return List.copyOf(hearts);
	}

	private static void drawHeart(GuiGraphicsExtractor graphics, Heart heart) {
		int alpha = Math.round(heart.alpha() * 255) << 24;
		if (alpha == 0) return;
		graphics.pose().pushMatrix();
		graphics.pose().translate(heart.x(), heart.y());
		for (int y = 0; y < HEART_ROWS.length; y++) {
			for (int x = 0; x < 7; x++) {
				if (!heartPixel(x, y)) continue;
				boolean edge = !heartPixel(x - 1, y) || !heartPixel(x + 1, y) || !heartPixel(x, y + 1) || y == 0;
				int rgb = edge ? 0xB22A50 : 0xF5769B;
				if (y == 1 && (x == 1 || x == 2)) rgb = 0xFFD4E2;
				graphics.fill(x, y, x + 1, y + 1, alpha | rgb);
			}
		}
		graphics.pose().popMatrix();
	}

	private static boolean heartPixel(int x, int y) {
		return x >= 0 && x < 7 && y >= 0 && y < HEART_ROWS.length && (HEART_ROWS[y] & 1 << (6 - x)) != 0;
	}

	public record Heart(float x, float y, float alpha) {
	}
}

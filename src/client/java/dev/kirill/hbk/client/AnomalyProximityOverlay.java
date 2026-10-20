package dev.kirill.hbk.client;

import dev.kirill.hbk.world.AnomalyZones;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

/** A growing purple frost border, drawn behind the HUD using the vanilla snow mask. */
public final class AnomalyProximityOverlay {
	private static final Identifier MASK = Identifier.withDefaultNamespace("textures/misc/powder_snow_outline.png");
	private static final int MASK_SIZE = 256;
	private static final double WARNING_DISTANCE = 144;

	private AnomalyProximityOverlay() {}

	public static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		var client = Minecraft.getInstance();
		var player = client.player;
		Long seed = AnomalyZones.clientSeed();
		if (player == null || client.level == null || seed == null || !player.isAlive() || player.isSpectator()
				|| !client.level.dimension().equals(Level.OVERWORLD)) return;

		var position = player.getPosition(delta.getGameTimeDeltaPartialTick(false));
		int cellX = Math.floorDiv((int) Math.floor(position.x), AnomalyZones.SPACING);
		int cellZ = Math.floorDiv((int) Math.floor(position.z), AnomalyZones.SPACING);
		double vertical = Math.max(0, Math.max(AnomalyZones.CEILING_Y - position.y - player.getBbHeight(),
				position.y - AnomalyZones.CEILING_Y));
		double nearestSquared = Double.POSITIVE_INFINITY;
		for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
			for (var zone : AnomalyZones.zonesInCell(seed, cellX + dx, cellZ + dz)) {
				double horizontal = zone.horizontalDistance(position.x, position.z);
				nearestSquared = Math.min(nearestSquared, horizontal * horizontal + vertical * vertical);
			}
		float proximity = (float) Math.max(0, 1 - Math.sqrt(nearestSquared) / WARNING_DISTANCE);
		float strength = proximity * proximity * (3 - 2 * proximity);
		// A faint edge at ground level establishes the visual cue before the warning grows.
		if (AnomalyZones.contains(player)) strength = Math.max(0.06f, strength);
		if (strength <= 0) return;

		int width = graphics.guiWidth(), height = graphics.guiHeight();
		int side = Math.max(1, Math.round(width * (0.035f + 0.325f * strength)));
		int cap = Math.max(1, Math.round(height * (0.025f + 0.21f * strength)));
		int color = Math.round(255 * (0.1f + 0.55f * strength)) << 24 | 0xA75AF0;
		// Stretch each outer quarter separately: increasing opacity alone would not grow the frame.
		blit(graphics, 0, 0, side, height, 0, 0, 64, MASK_SIZE, color);
		blit(graphics, width - side, 0, side, height, 192, 0, 64, MASK_SIZE, color);
		blit(graphics, 0, 0, width, cap, 0, 0, MASK_SIZE, 64, color);
		blit(graphics, 0, height - cap, width, cap, 0, 192, MASK_SIZE, 64, color);
	}

	private static void blit(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
			int u, int v, int sourceWidth, int sourceHeight, int color) {
		graphics.blit(RenderPipelines.GUI_TEXTURED, MASK, x, y, (float) u, (float) v,
				width, height, sourceWidth, sourceHeight, MASK_SIZE, MASK_SIZE, color);
	}
}

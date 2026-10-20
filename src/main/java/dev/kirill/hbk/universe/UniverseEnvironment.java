package dev.kirill.hbk.universe;

import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import static dev.kirill.hbk.universe.UniverseNoise.*;
import static net.minecraft.world.attribute.EnvironmentAttributes.*;

/** Added after biome/timeline layers, on both physical sides. No per-tick profile networking. */
public final class UniverseEnvironment {
	private static final double[] FAMILY_HUE = {.56, .88, .48, .71, .37, .08, .17, .62, .77, .04};
	private static final int[] LIGHT = {0xffffff, 0xe1ebff, 0xcbdcff, 0xc8f2ed, 0xd8efce, 0xfff2c8, 0xffdfbb, 0xffd0ce, 0xffd5ee, 0xe5d8ff};
	private static final float[] VISIBILITY_DISTANCE = {96, 112, 128, 160, 192, 224, 256, 320, 384, 512};
	private static final double[] BRIGHTNESS_LEVEL = {.35, .42, .49, .56, .63, .70, .78, .86, .93, 1};
	private static final double[] AMBIENT_LEVEL = {.10, .12, .14, .16, .18, .20, .24, .28, .32, .40};
	private UniverseEnvironment() {}
	public static void addLayers(EnvironmentAttributeSystem.Builder builder, Level level) {
		UniverseProfile p = UniverseProfile.of(level);
		if (p == null) return;
		builder.addTimeBasedLayer(SKY_COLOR, (old, tick) -> skyColor(p, level.getGameTime()));
		builder.addTimeBasedLayer(FOG_COLOR, (old, tick) -> fogColor(p, level.getGameTime()));
		builder.addConstantLayer(WATER_FOG_COLOR, old -> palette(p, true));
		if (UniverseCompatibility.resolve(p).fog()) {
			builder.addPositionalLayer(FOG_END_DISTANCE, (old, pos, probe) -> fogEnd(p, pos, level.getGameTime()));
			builder.addPositionalLayer(SKY_FOG_END_DISTANCE, (old, pos, probe) -> fogEnd(p, pos, level.getGameTime()) * 2);
			builder.addPositionalLayer(FOG_START_DISTANCE, (old, pos, probe) -> fogEnd(p, pos, level.getGameTime()) * (9 - p.get(HiddenParameter.FOG_DENSITY)) / 12);
		}
		builder.addPositionalLayer(FOG_COLOR, (old, pos, probe) -> hazardFogColor(p, pos, old));
		builder.addPositionalLayer(FOG_END_DISTANCE, (old, pos, probe) -> hazardFogEnd(p, pos, old));
		builder.addPositionalLayer(FOG_START_DISTANCE, (old, pos, probe) -> Math.min(old, hazardFogEnd(p, pos, Float.MAX_VALUE) * .15f));
		builder.addTimeBasedLayer(SUN_ANGLE, (old, tick) -> sunAngle(p, level.getGameTime()));
		builder.addTimeBasedLayer(MOON_ANGLE, (old, tick) -> sunAngle(p, level.getGameTime()) + 180);
		builder.addTimeBasedLayer(STAR_ANGLE, (old, tick) -> sunAngle(p, level.getGameTime()));
		builder.addTimeBasedLayer(STAR_BRIGHTNESS, (old, tick) -> p.openSky() ? p.get(HiddenParameter.STARS) / 9f * (1 - daylight(p, level.getGameTime()) * .85f) : 0f);
		builder.addConstantLayer(BLOCK_LIGHT_TINT, old -> LIGHT[p.get(HiddenParameter.LIGHT_COLOR)]);
		builder.addConstantLayer(SKY_LIGHT_COLOR, old -> LIGHT[p.get(HiddenParameter.LIGHT_COLOR)]);
		builder.addTimeBasedLayer(SKY_LIGHT_FACTOR, (old, tick) -> illumination(p, level.getGameTime()));
		builder.addTimeBasedLayer(SKY_LIGHT_LEVEL, (old, tick) -> illumination(p, level.getGameTime()) * 15);
		builder.addTimeBasedLayer(AMBIENT_LIGHT_COLOR, (old, tick) -> ambientColor(p, level.getGameTime()));
		builder.addConstantLayer(CLOUD_COLOR, old -> (p.get(HiddenParameter.CLOUD_COVER) * 23 << 24) | palette(p, true));
		builder.addConstantLayer(CLOUD_HEIGHT, old -> new float[]{24, 48, 80, 112, 152, 192.33f, 216, 244, 280, 312}[p.get(HiddenParameter.CLOUD_HEIGHT)]);
		builder.addConstantLayer(SUNRISE_SUNSET_COLOR, old -> (p.get(HiddenParameter.HORIZON) == 0 ? 0 : 0x60000000) | palette(p, true));
	}
	/** Continuous seed-derived palette: the decimal selectors no longer include a pure black entry. */
	public static int palette(UniverseProfile p, boolean fog) {
		double hue = FAMILY_HUE[p.geometry()] + p.get(HiddenParameter.SKY_COLOR) * .071 + unit(p.stream(45)) * .055;
		if (fog) hue += (p.get(HiddenParameter.FOG_COLOR) - 4.5) * .023;
		double saturation = fog ? .16 + unit(p.stream(46)) * .22 : .32 + unit(p.stream(47)) * .30;
		double value = .83 + unit(p.stream(48)) * .16;
		double h = (hue - Math.floor(hue)) * 6, c = value * saturation, v = value - c, x = c * (1 - Math.abs(h % 2 - 1));
		double r = 0, g = 0, b = 0;
		switch ((int) h) {
			case 0 -> { r = c; g = x; } case 1 -> { r = x; g = c; } case 2 -> { g = c; b = x; }
			case 3 -> { g = x; b = c; } case 4 -> { r = x; b = c; } default -> { r = c; b = x; }
		}
		return (int) ((r + v) * 255) << 16 | (int) ((g + v) * 255) << 8 | (int) ((b + v) * 255);
	}
	public static int skyColor(UniverseProfile p, long time) { return shade(palette(p, false), .38 + .62 * daylight(p, time)); }
	public static int fogColor(UniverseProfile p, long time) { return shade(palette(p, true), .55 + .45 * daylight(p, time)); }
	private static int shade(int color, double amount) {
		return ((int) ((color >> 16 & 255) * clamp(amount, 0, 1)) << 16)
				| ((int) ((color >> 8 & 255) * clamp(amount, 0, 1)) << 8) | (int) ((color & 255) * clamp(amount, 0, 1));
	}
	public static float sunAngle(UniverseProfile p, long time) {
		double t = switch (p.get(HiddenParameter.DAY_CYCLE)) {
			case 1 -> time * 2.0; case 2 -> time * 4.0; case 3 -> time * .5; case 4 -> time * .25;
			case 5 -> 6000; case 6 -> 18000; case 7 -> 23000; case 8 -> 12500;
			case 9 -> time + Math.sin(time / 19000.0 + unit(p.stream(21)) * 6) * 4200;
			default -> time;
		};
		return (float) ((t % 24000 / 24000.0 - .25) * 360);
	}
	public static float daylight(UniverseProfile p, long time) { return (float) clamp(Math.cos(Math.toRadians(sunAngle(p, time))) * 1.1 + .15, 0, 1); }
	public static float illumination(UniverseProfile p, long time) {
		double base = BRIGHTNESS_LEVEL[p.get(HiddenParameter.BRIGHTNESS)];
		double flicker = p.get(HiddenParameter.LIGHT_FLICKER) / 9.0 * .15 * Math.sin(time / (100.0 - p.get(HiddenParameter.LIGHT_FLICKER) * 9));
		return (float) clamp(base * (.65 + daylight(p, time) * .35) * (1 + flicker * instability(p, time)), .18, 1);
	}
	public static int ambientColor(UniverseProfile p, long time) {
		// Brightness also reaches caves and lower faces; skylight alone cannot light a ceiling world.
		double flicker = p.get(HiddenParameter.LIGHT_FLICKER) / 9.0 * .15 * Math.sin(time / (100.0 - p.get(HiddenParameter.LIGHT_FLICKER) * 9));
		return shade(LIGHT[p.get(HiddenParameter.LIGHT_COLOR)], Math.max(p.openSky() ? .10 : .16, AMBIENT_LEVEL[p.get(HiddenParameter.BRIGHTNESS)])
				* (1 + flicker * instability(p, time)));
	}
	public static double instability(UniverseProfile p, long time) { return 1 + p.get(HiddenParameter.INSTABILITY) / 18.0 * Math.sin(time / 97.0) * Math.sin(time / 413.0); }
	public static float fogEnd(UniverseProfile p, Vec3 pos, long time) {
		if (p.get(HiddenParameter.FOG_DENSITY) == 0) return Float.MAX_VALUE;
		double altitude = switch (p.get(HiddenParameter.FOG_ALTITUDE)) {
			case 1 -> 1 - Math.min(1, Math.abs(pos.y - 88) / 120); case 2 -> pos.y < 64 ? 1 : .25;
			case 3 -> pos.y < 100 ? 1 : .3; case 4 -> 1 - Math.min(.8, Math.abs(pos.y - 130) / 120);
			case 5 -> pos.y > 130 ? 1 : .3; case 6 -> pos.y > 220 ? 1 : .1;
			case 7 -> .55 + .45 * Math.cos(pos.y / 24); case 8 -> clamp((320 - pos.y) / 350, .1, 1);
			case 9 -> clamp((pos.y + 64) / 350, .1, 1); default -> 1;
		};
		double drift = time * p.get(HiddenParameter.FOG_MOTION) * .00015;
		double patch = noise(p.stream(22), pos.x / 120 + drift, pos.y / 160, pos.z / 120) * p.get(HiddenParameter.FOG_PATCHINESS) / 12.0;
		double wave = Math.sin(time / 210.0 + pos.x / 90) * (p.get(HiddenParameter.FOG_OSCILLATION) + p.get(HiddenParameter.FOG_MOTION)) / 55.0;
		double strength = clamp(altitude * (1 + patch + wave * instability(p, time)), .08, 2);
		return (float) clamp(VISIBILITY_DISTANCE[p.get(HiddenParameter.VISIBILITY)] / strength * (1 - p.get(HiddenParameter.FOG_DENSITY) * .035), 64, 768);
	}
	public static double gravity(UniverseProfile p, Vec3 pos) {
		double local = noise(p.stream(23), pos.x / 45, pos.y / 70, pos.z / 45) * p.get(HiddenParameter.LOCAL_GRAVITY) * .045;
		return clamp(p.gravity() * (1 + local) * UniverseHazards.gravity(p, pos), .15, 2.5);
	}
	public static float hazardFogEnd(UniverseProfile p, Vec3 pos, float old) {
		var zone = UniverseHazards.at(p, pos);
		if (zone == null || !zone.kind().gas) return old;
		// Interpolate extinction, so the boundary fades from the actual surrounding atmosphere.
		return (float) (1 / (1.0 / old + zone.strength(pos) * (1.0 / 18 - 1.0 / old)));
	}
	public static int hazardFogColor(UniverseProfile p, Vec3 pos, int old) {
		var zone = UniverseHazards.at(p, pos);
		if (zone == null || !zone.kind().gas) return old;
		double blend = zone.strength(pos) * .72; int color = zone.kind().color;
		return (int) lerp(old >> 16 & 255, color >> 16 & 255, blend) << 16
				| (int) lerp(old >> 8 & 255, color >> 8 & 255, blend) << 8 | (int) lerp(old & 255, color & 255, blend);
	}
}

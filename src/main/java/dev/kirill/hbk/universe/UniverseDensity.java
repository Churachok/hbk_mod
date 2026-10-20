package dev.kirill.hbk.universe;

import static dev.kirill.hbk.universe.UniverseNoise.*;

/** Positive = solid. Every function uses absolute coordinates, including at chunk edges. */
public final class UniverseDensity {
	public static final int MIN_Y = -64, HEIGHT = 384;
	private final UniverseProfile p;
	private final UniverseCompatibility rules;
	private final long geometrySeed, reliefSeed, geologySeed, matterSeed, waterSeed;
	private final double scale, amplitude, phase;
	public UniverseDensity(UniverseProfile profile) {
		p = profile; rules = UniverseCompatibility.resolve(profile); scale = p.scale(); amplitude = p.amplitude(); phase = unit(p.stream(120)) * Math.PI * 2;
		geometrySeed = p.stream(1); reliefSeed = p.stream(2); geologySeed = p.stream(3); matterSeed = p.stream(4); waterSeed = p.stream(5);
	}
	private double n(long seed, double x, double y, double z, double size) { return noise(seed, x / size, y / size, z / size); }
	public double sample(double x, double y, double z) {
		double broad = n(geometrySeed, x, 0, z, scale * 1.8);
		double warp = n(geometrySeed + 1, x, y, z, scale) * 19;
		double u = (x + warp) / scale, v = (z - warp) / scale;
		double relief = relief(x, y, z, p.relief());
		double body = switch (p.geometry()) {
			case 0 -> 88 + broad * 36 - y;
			case 1 -> 24 + n(geometrySeed + 2, x, y * 1.8, z, scale * .65) * 65
					- Math.abs(y - (126 + broad * 54)) * .72;
			case 2 -> 20 - Math.hypot(Math.sin(u * 2.7 + phase), Math.sin(v * 2.5 - phase)) * 31
					+ broad * 13 + n(geometrySeed + 3, x, y, z, 40) * 10;
			case 3 -> Math.max(44 + broad * 25 - y, y - 233 - broad * 23);
			case 4 -> 17 - Math.abs(Math.sin(u * 2.1) * Math.cos(y / 48.0 + phase)
					+ Math.sin(v * 2.2) * Math.sin(y / 51.0) + Math.cos(u * 1.7 + v * 1.6)) * 36;
			case 5 -> 101 + broad * 28 - y - Math.pow(Math.max(0, n(geometrySeed + 4, x, 0, z, scale * .7) + .12), 2) * 180;
			case 6 -> 14 - Math.abs(Math.sin(u * 3 + n(geometrySeed + 5, x, y * .2, z, 95) * 3)) * 37
					+ Math.min(0, 210 - y) + Math.min(0, y + 32);
			case 7 -> n(geometrySeed + 6, x, y * 1.05, z, 33) * 52 - 16;
			case 8 -> y - (210 + broad * 40);
			case 9 -> 15 - Math.abs(Math.sin((y + broad * 24) / 37.0 + phase)) * 37;
			default -> throw new IllegalStateException();
		};
		body += relief + matter(x, y, z);
		// Thin margins keep all shapes inside the supported vertical range; no chunk-relative caps.
		body = Math.min(body, Math.min((y - MIN_Y - 3) * 2, (MIN_Y + HEIGHT - 4 - y) * 2));
		double channel = Math.abs(n(waterSeed, x, 0, z, 130));
		if (p.hydrology() == 3 && channel < .055 && !p.suspended()) body = Math.min(body, Math.abs(y - 82) - (10 - channel * 120));
		if (p.hydrology() == 8) body = Math.min(body, Math.abs(Math.sin(x / 29 + n(waterSeed, x, y, z, 100))) * 30 + Math.abs(y - 68) - 12);
		if (body > 4) body = Math.min(body, geology(x, y, z, p.geology()));
		// Monolithic aquifers are explicitly bounded cavities, not an all-world cave override.
		if (p.hydrology() == 5 && y < 80 && body > 4) body = Math.min(body, 8 - n(waterSeed + 1, x, y, z, 37) * 13);
		if (p.get(HiddenParameter.RIFTS) > 0 && body > 3) {
			double rift = Math.abs(n(p.stream(6), x + y * .4, 0, z, 60));
			body = Math.min(body, (rift - .002 * p.get(HiddenParameter.RIFTS)) * 190);
		}
		return body;
	}
	private double relief(double x, double y, double z, int mode) {
		double a = n(reliefSeed, x, y * .28, z, scale * .55);
		double b = n(reliefSeed + 1, x, y * .4, z, 29);
		return switch (mode) {
			case 0 -> a * 2;
			case 1 -> a * amplitude;
			case 2 -> (a * .7 + Math.abs(b) * .6) * amplitude * 2.5;
			case 3 -> Math.pow(Math.max(0, 1 - Math.abs(b) * 2), 7) * amplitude * 2 - 12;
			case 4 -> Math.floor(a * 5) * amplitude * .35;
			case 5 -> Math.sin(x / 34 + z / 47 + a) * amplitude * .7;
			case 6 -> a * amplitude - Math.pow(Math.max(0, 1 - Math.abs(b) * 6), 2) * amplitude * 1.5;
			case 7 -> clamp(a * 4, -.8, .8) * amplitude;
			case 8 -> -a * amplitude;
			case 9 -> relief(x, y, z, (int) clamp(Math.floor((n(reliefSeed + 9, x, 0, z, 420) + 1) * 4.5), 0, 8));
			default -> throw new IllegalStateException();
		};
	}
	private double matter(double x, double y, double z) {
		double c = n(matterSeed, x, y * .3, z, 180);
		return switch (p.distribution()) {
			case 0 -> 0;
			case 1 -> (100 - y) * .16;
			case 2 -> (y - 130) * .16;
			case 3 -> 15 - Math.abs(y - 130) * .24;
			case 4 -> Math.cos(y / 26.0 + phase) * 17;
			case 5 -> c * 64 - 6;
			case 6 -> c * 38 - 22;
			case 7 -> Math.sin(x / 1100.0 + z / 1700.0 + phase) * 28;
			case 8 -> Math.sin(x / 90.0 + z / 140.0 + phase) * 26;
			case 9 -> n(matterSeed + 1, x, y, z, 80) * 48;
			default -> throw new IllegalStateException();
		};
	}
	private double geology(double x, double y, double z, int mode) {
		double a = n(geologySeed, x, y, z, 34), b = n(geologySeed + 1, x, y, z, 38);
		double cx = Math.floor(x / 240) * 240 + 120, cz = Math.floor(z / 240) * 240 + 120;
		double radius = Math.hypot(x - cx, z - cz), angle = Math.atan2(z - cz, x - cx);
		return switch (mode) {
			case 0 -> 100;
			case 1 -> (Math.hypot(a, b) - .12) * 80;
			case 2 -> (Math.abs(a) - .18) * 62;
			case 3 -> ( .48 - n(geologySeed + 2, x, y, z, 90)) * 80;
			case 4 -> (Math.abs(n(geologySeed + 3, x, y, z, 18)) - .24) * 42;
			case 5 -> (Math.abs(n(geologySeed + 4, x + y * .8, 0, z, 54)) - .055) * 110;
			case 6 -> (Math.abs(Math.sin(y / 13.0 + a * 2)) - .2) * 25;
			case 7 -> Math.min(radius - 19, Math.max((Math.abs(Math.sin(angle * 6)) - .12) * 30, Math.abs(y - 90 - b * 25) - 6));
			case 8 -> Math.max((Math.abs(Math.sin(angle * 2 + radius / 24 - y / 26)) - .18) * 22, Math.abs(radius - 68) - 28);
			case 9 -> geology(x, y, z, (int) clamp(Math.floor((n(geologySeed + 9, x, 0, z, 450) + 1) * 4.5), 0, 8));
			default -> throw new IllegalStateException();
		};
	}
	/** Natural water is emitted without scheduled ticks. Suspended worlds require rock below. */
	public int waterPlane(int y) {
		return p.hydrology() == 7 ? 212 : p.hydrology() == 4 ? 103 : p.hydrology() == 6 ? Math.floorDiv(y + 14, 72) * 72 + 50 : 82;
	}
	public int waterSupportY(int y) { return rules.supportedReservoirs() ? waterPlane(y) - 9 : y - 8; }
	public boolean water(int x, int y, int z, double support) {
		int mode = p.hydrology();
		if (mode == 0 || y < MIN_Y + 5) return false;
		double wet = n(waterSeed + 2, x, 0, z, 160);
		int plane = waterPlane(y);
		boolean region = switch (mode) {
			case 1 -> wet > .48;
			case 2 -> wet > .05;
			case 3 -> Math.abs(n(waterSeed, x, 0, z, 130)) < .075;
			case 4, 6, 7 -> true;
			case 5 -> y < 60 && support > 0;
			case 8 -> Math.abs(Math.sin(x / 29.0 + n(waterSeed, x, y, z, 100))) < .3;
			case 9 -> wet > -.05;
			default -> false;
		};
		if (y > plane || !region) return false;
		if (rules.supportedReservoirs()) return support > 0 && plane - y < 9;
		return mode != 5 || y < 60;
	}
}

package dev.kirill.hbk.world;

/** A continuous funnel sampled at block centers, independent of chunk generation order. */
final class AnomalyCrater {
	static final int UNCHANGED = Integer.MAX_VALUE;
	private static final double RADIUS = 88;
	private static final double THROAT = 22;

	private AnomalyCrater() {}

	static int floor(double x, double z, int surfaceY, int minY) {
		double angle = Math.atan2(z, x);
		// Slightly scalloped walls and narrow ledges keep the crater from looking like a perfect cone.
		double radius = Math.hypot(x, z) / (1 + 0.018 * Math.sin(5 * angle) + 0.014 * Math.sin(9 * angle));
		if (radius >= RADIUS) return UNCHANGED;
		if (radius <= THROAT) return minY - 1;
		double t = (radius - THROAT) / (RADIUS - THROAT);
		double floor = minY - 10 + (surfaceY - minY + 10) * Math.pow(t, 1.15);
		floor += 2 * Math.sin(radius * 0.4 + Math.sin(7 * angle)) * Math.sin(Math.PI * t);
		return (int) Math.floor(floor);
	}
}

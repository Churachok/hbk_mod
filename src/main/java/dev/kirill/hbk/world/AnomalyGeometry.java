package dev.kirill.hbk.world;

/** Signed fields for intersecting impossible terrain. Horizontal work is cached once per column. */
final class AnomalyGeometry {
	private AnomalyGeometry() {}

	static Column column(AnomalyZones.Zone zone, int worldX, int worldZ) {
		double x = worldX - zone.minX() - AnomalyZones.SIZE / 2.0;
		double z = worldZ - zone.minZ() - AnomalyZones.SIZE / 2.0;
		long variant = (long) zone.minX() * 341873128712L ^ (long) zone.minZ() * 132897987541L;
		int turn = (int) (variant ^ (variant >>> 29)) & 3;
		return new Column(x, z, turn);
	}

	static final class Column {
		private final double x;
		private final double z;
		private final double ringX;
		private final double ringZ;
		private final double radius;
		private final double ribbonRadius;
		private final double ribbonSin;
		private final double ribbonCos;
		private final double helixX;
		private final double helixY;

		private Column(double x, double z, int turn) {
			this.ringX = x;
			this.ringZ = z;
			this.radius = Math.hypot(x, z);
			this.x = turn == 1 ? z : turn == 2 ? -x : turn == 3 ? -z : x;
			this.z = turn == 1 ? -x : turn == 2 ? -z : turn == 3 ? x : z;
			x = this.x;
			z = this.z;
			this.ribbonRadius = Math.hypot(x - 12, z + 5) - 48;
			double halfTwist = Math.atan2(z + 5, x - 12) * 0.5;
			this.ribbonSin = Math.sin(halfTwist);
			this.ribbonCos = Math.cos(halfTwist);
			double phase = z * Math.PI / 48;
			this.helixX = 19 * Math.sin(phase);
			this.helixY = 19 * Math.cos(phase);
		}

		double distance(int y) {
			// Keep the original three enormous rings as the backbone of the zone.
			double distance = Math.hypot(Math.hypot(ringX, y - 135) - 57, ringZ + 30) - 12;
			distance = Math.min(distance, Math.hypot(Math.hypot(ringZ, y - 192) - 43, ringX - 24) - 10);
			distance = Math.min(distance, Math.hypot(radius - 64, y - 211) - 9);

			// A thick ribbon that makes a half-turn as it closes, like a giant Mobius strip.
			if (Math.abs(ribbonRadius) < 16 && Math.abs(y - 171) < 16) {
				double across = ribbonRadius * ribbonCos + (y - 171) * ribbonSin;
				double normal = -ribbonRadius * ribbonSin + (y - 171) * ribbonCos;
				distance = Math.min(distance, Math.max(Math.abs(across) - 13, Math.abs(normal) - 3.5));
			}

			// An inclined square frame whose plane buckles in and out instead of staying flat.
			if (Math.abs(x + 40) < 53 && Math.abs(z - 22) < 16 && Math.abs(y - 207) < 53) {
				double u = (x + 40) * 0.8660254 + (y - 207) * 0.5;
				double v = -(x + 40) * 0.5 + (y - 207) * 0.8660254;
				double depth = z - 22 - 7 * Math.sin((y - 207) / 22.0 + x / 32.0);
				double frame = Math.max(Math.abs(Math.max(Math.abs(u) - 26, Math.abs(v) - 31)) - 4.5,
						Math.abs(depth) - 5);
				distance = Math.min(distance, frame);
			}

			// A hollow, faceted octahedron with three tunnels cut through its shell.
			double ox = x - 46, oy = y - 233, oz = z + 45;
			if (Math.abs(ox) + Math.abs(oz) < 32 && Math.abs(oy) < 32) {
				double shell = Math.abs((Math.abs(ox) + Math.abs(oy) + Math.abs(oz) - 25) * 0.57735027) - 3;
				double holes = Math.min(Math.hypot(ox, oy), Math.min(Math.hypot(oy, oz), Math.hypot(ox, oz))) - 7.5;
				distance = Math.min(distance, Math.max(shell, -holes));
			}

			// Two intertwined spiral bridges, with space between the strands to fly through.
			if (Math.abs(z) < 76 && Math.abs(x + 32) < 25 && Math.abs(y - 144) < 25) {
				double strand = Math.min(Math.hypot(x + 32 - helixX, y - 144 - helixY),
						Math.hypot(x + 32 + helixX, y - 144 + helixY)) - 4.5;
				distance = Math.min(distance, Math.max(strand, Math.abs(z) - 73));
			}

			// Floating cube skeletons at different heights and intersections.
			distance = Math.min(distance, cubeFrame(x + 58, y - 119, z + 54));
			distance = Math.min(distance, cubeFrame(x - 58, y - 155, z - 52));
			return Math.min(distance, cubeFrame(x + 22, y - 259, z - 23));
		}
	}

	private static double cubeFrame(double x, double y, double z) {
		if (Math.abs(x) > 27 || Math.abs(y) > 27 || Math.abs(z) > 27) return Double.POSITIVE_INFINITY;
		double u = Math.abs((x + z) * 0.70710678);
		double depth = (z - x) * 0.70710678;
		double v = Math.abs(y * 0.8660254 - depth * 0.5);
		double w = Math.abs(y * 0.5 + depth * 0.8660254);
		double outer = Math.max(u, Math.max(v, w)) - 15;
		double tunnels = Math.min(Math.max(u, v), Math.min(Math.max(v, w), Math.max(u, w))) - 9;
		return Math.max(outer, -tunnels);
	}
}

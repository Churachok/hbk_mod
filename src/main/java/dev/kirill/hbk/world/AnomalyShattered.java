package dev.kirill.hbk.world;

/** Sparse, independently raised chunks of land; the mask is continuous across chunk boundaries. */
final class AnomalyShattered {
	private static final int GRID = 20;
	record Fragment(double radius, int lift, int depth) {}
	private AnomalyShattered() {}

	static Fragment fragment(long seed, int x, int z) {
		int gx = Math.floorDiv(x, GRID), gz = Math.floorDiv(z, GRID);
		Fragment nearest = null;
		for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
			int cx = gx + dx, cz = gz + dz;
			long hash = mix(seed ^ (long) cx * 341873128712L ^ (long) cz * 132897987541L);
			double centerX = cx * GRID + 6 + (hash >>> 8 & 7);
			double centerZ = cz * GRID + 6 + (hash >>> 16 & 7);
			double angle = Math.atan2(z + 0.5 - centerZ, x + 0.5 - centerX);
			double r = (4 + (hash >>> 24 & 3)) * (1 + 0.1 * Math.sin(angle * 5 + (hash & 31)));
			double distance = Math.hypot(x + 0.5 - centerX, z + 0.5 - centerZ) / r;
			if (distance >= 1 || nearest != null && distance >= nearest.radius()) continue;
			int depth = 3 + (int) ((24 + (hash >>> 32 & 23)) * Math.pow(1 - distance, 0.65));
			nearest = new Fragment(distance, -24 + (int) Math.floorMod(hash >>> 40, 113), depth);
		}
		return nearest;
	}

	private static long mix(long value) {
		value = (value ^ value >>> 30) * 0xbf58476d1ce4e5b9L;
		value = (value ^ value >>> 27) * 0x94d049bb133111ebL;
		return value ^ value >>> 31;
	}
}

package dev.kirill.hbk.world;

/** The same local coordinates address every tile, including negative world coordinates. */
final class AnomalyTileLayout {
	private AnomalyTileLayout() {}
	static int blockIndex(int x, int y, int z, int minY) {
		return (y - minY) * 256 + (z & 15) * 16 + (x & 15);
	}
	static int biomeIndex(int x, int y, int z, int minY, int height) {
		return Math.clamp(y - (minY >> 2), 0, height / 4 - 1) * 16 + (z & 3) * 4 + (x & 3);
	}
}

package dev.kirill.hbk.world;

/** Move complete native columns: the surface reverses, while ores, caves, rivers and plants travel with it. */
final class AnomalyInvertedHeights {
	private AnomalyInvertedHeights() {}

	static int targetHeight(int original, int reference, int minY, int maxY) {
		return Math.clamp(2 * reference - original, minY + 16, maxY - 64);
	}
}

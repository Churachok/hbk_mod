package dev.kirill.hbk.universe;

/** Stateless, versioned value noise; negative coordinates use floor, not truncation. */
public final class UniverseNoise {
	private UniverseNoise() {}
	public static long mix(long n) {
		n = (n ^ (n >>> 30)) * 0xbf58476d1ce4e5b9L;
		n = (n ^ (n >>> 27)) * 0x94d049bb133111ebL;
		return n ^ (n >>> 31);
	}
	public static long hash(long seed, int x, int y, int z) {
		return mix(seed ^ x * 0x632be59bd9b4e019L ^ y * 0x9e3779b97f4a7c15L ^ z * 0x85157af5L);
	}
	public static double unit(long n) { return (mix(n) >>> 11) * 0x1.0p-53; }
	private static double corner(long seed, int x, int y, int z) { return (hash(seed, x, y, z) >>> 11) * 0x1.0p-52 - 1; }
	public static double lerp(double a, double b, double t) { return a + (b - a) * t; }
	private static double smooth(double t) { return t * t * t * (t * (t * 6 - 15) + 10); }
	public static double noise(long seed, double x, double y, double z) {
		int ix = (int) Math.floor(x), iy = (int) Math.floor(y), iz = (int) Math.floor(z);
		double fx = smooth(x - ix), fy = smooth(y - iy), fz = smooth(z - iz);
		return lerp(lerp(lerp(corner(seed, ix, iy, iz), corner(seed, ix + 1, iy, iz), fx),
				lerp(corner(seed, ix, iy + 1, iz), corner(seed, ix + 1, iy + 1, iz), fx), fy),
				lerp(lerp(corner(seed, ix, iy, iz + 1), corner(seed, ix + 1, iy, iz + 1), fx),
				lerp(corner(seed, ix, iy + 1, iz + 1), corner(seed, ix + 1, iy + 1, iz + 1), fx), fy), fz);
	}
	public static double clamp(double n, double min, double max) { return Math.max(min, Math.min(max, n)); }
}

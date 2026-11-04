package dev.kirill.hbk.universe;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

import static dev.kirill.hbk.universe.UniverseNoise.*;

/** Large analytic formations; one plan per 2048-block cell, sliced by the receiving chunk. */
public final class UniverseLandmarks {
	public static final int SPACING = 2048;
	public enum Kind { FISSURE, MONOLITHS, CRYSTAL_FOREST, SKELETON, PETRIFIED_WAVE, WORLD_EYE }
	public record Site(Kind kind, BlockPos center, boolean rotated, long seed) {
		public int radiusX() { return kind == Kind.FISSURE ? 640 : kind == Kind.SKELETON ? 160 : 128; }
		public int radiusZ() { return kind == Kind.FISSURE ? 96 : 128; }
		public boolean intersects(int x, int z, int margin) {
			int rx = rotated ? radiusZ() : radiusX(), rz = rotated ? radiusX() : radiusZ();
			return x + margin >= center.getX() - rx && x - margin <= center.getX() + rx
					&& z + margin >= center.getZ() - rz && z - margin <= center.getZ() + rz;
		}
	}
	private final UniverseProfile profile;
	private final UniverseDensity terrain;
	private final java.util.concurrent.ConcurrentHashMap<Long, Site> plans = new java.util.concurrent.ConcurrentHashMap<>();
	public UniverseLandmarks(UniverseProfile profile) { this.profile = profile; terrain = new UniverseDensity(profile); }
	public Site site(int cx, int cz) {
		if (plans.size() > 256) plans.clear();
		return plans.computeIfAbsent(((long) cx << 32) ^ (cz & 0xffffffffL), ignored -> plan(cx, cz));
	}
	private Site plan(int cx, int cz) {
		long h = hash(profile.stream(84), cx, 0, cz);
		int x = cx * SPACING + 704 + (int) Long.remainderUnsigned(mix(h), 640);
		int z = cz * SPACING + 704 + (int) Long.remainderUnsigned(mix(h + 1), 640);
		int y = 80;
		for (int candidate = 190; candidate > -20; candidate -= 2) {
			if (terrain.sample(x, candidate, z) > 0 && terrain.sample(x, candidate + 6, z) < 0) { y = candidate; break; }
		}
		return new Site(Kind.values()[(int) Long.remainderUnsigned(h, Kind.values().length)], new BlockPos(x, y, z), (h & 16) != 0, h);
	}
	public List<Site> forChunk(int x, int z) {
		var sites = new ArrayList<Site>();
		for (int cx = Math.floorDiv(x - 640, SPACING); cx <= Math.floorDiv(x + 655, SPACING); cx++)
			for (int cz = Math.floorDiv(z - 640, SPACING); cz <= Math.floorDiv(z + 655, SPACING); cz++) {
				Site site = site(cx, cz); if (site.intersects(x + 8, z + 8, 8)) sites.add(site);
			}
		return sites;
	}
	public Site nearest(BlockPos pos, Kind kind) {
		Site result = null; double distance = Double.MAX_VALUE;
		int cx = Math.floorDiv(pos.getX(), SPACING), cz = Math.floorDiv(pos.getZ(), SPACING);
		for (int dx = -5; dx <= 5; dx++) for (int dz = -5; dz <= 5; dz++) {
			Site site = site(cx + dx, cz + dz); if (site.kind != kind) continue;
			double d = site.center.distSqr(pos); if (d < distance) { result = site; distance = d; }
		}
		return result;
	}
	public BlockState material(List<Site> sites, int x, int y, int z) {
		for (var site : sites) {
			if (!site.intersects(x, z, 0)) continue;
			int dx = x - site.center.getX(), dz = z - site.center.getZ();
			double u = site.rotated ? dz : dx, v = site.rotated ? dx : dz, h = y - site.center.getY();
			BlockState rock = UniverseMaterials.state(profile.geometry(), 0, (int) (site.seed & 3));
			switch (site.kind) {
				case FISSURE -> {
					double curve = Math.sin(u / 103 + unit(site.seed) * 6) * 25 + Math.sin(u / 37) * 5;
					double width = (12 + 10 * (1 + Math.sin(u / 81))) * Math.sqrt(Math.max(0, 1 - u * u / (620 * 620)));
					if (Math.abs(u) < 620 && Math.abs(v - curve) < width && y < 270) return Blocks.AIR.defaultBlockState();
				}
				case MONOLITHS -> {
					for (int n = -1; n <= 1; n++) {
						double yy = y - (158 + n * 22), xx = u - n * 58 - yy * .12, zz = v - Math.sin(n * 3 + 1) * 38;
						if (Math.abs(yy) < 68 && Math.abs(xx) + Math.abs(zz) < 23 * (1 - Math.abs(yy) / 90))
							return Math.abs(yy) < 3 ? UniverseMaterials.state(profile.geometry(), 3, n & 3) : rock;
					}
				}
				case CRYSTAL_FOREST -> {
					if (u * u + v * v > 116 * 116) break;
					int gx = Math.floorDiv((int) u, 28), gz = Math.floorDiv((int) v, 28);
					long k = hash(site.seed, gx, 0, gz);
					double height = 34 + Long.remainderUnsigned(k, 62), px = u - (gx * 28 + 10 + (k & 7)), pz = v - (gz * 28 + 10 + (k >>> 3 & 7));
					double radius = 7 * Math.min(1, (height - h) / 14);
					if (h >= -12 && h < height && Math.abs(px - h * .075) + Math.abs(pz) < radius)
						return ((int) h + gx + gz) % 9 == 0 ? Blocks.AMETHYST_BLOCK.defaultBlockState() : UniverseMaterials.state(profile.geometry(), 3, (int) (k & 3));
				}
				case SKELETON -> {
					double spine = 16 + Math.sin(u / 45) * 7, side = Math.sin(u / 61) * 9;
					if (Math.abs(u) < 143 && Math.hypot(v - side, h - spine) < 4) return Blocks.BONE_BLOCK.defaultBlockState();
					if (Math.abs(u) < 120 && Math.abs(Math.IEEEremainder(u, 19)) < 2.5) {
						double rib = Math.pow((v - side) / 27, 2) + Math.pow((h - spine + 13) / 28, 2);
						if (h < spine + 3 && Math.abs(rib - 1) < .16) return Blocks.BONE_BLOCK.defaultBlockState();
					}
					// Hollow skull, eye sockets and two long tusks form an explorable head.
					double skull = Math.pow((u - 137) / 17, 2) + Math.pow(v / 20, 2) + Math.pow((h - 19) / 17, 2);
					if (u > 120 && skull < 1 && skull > .68 && !(u > 139 && Math.abs(v) > 9 && h > 19 && h < 29)) return Blocks.BONE_BLOCK.defaultBlockState();
				}
				case PETRIFIED_WAVE -> {
					if (Math.abs(v) < 91 && u > -90 && u < 80) {
						double yy = 52 + Math.sin(v / 33) * 9;
						double ring = Math.hypot((u + 15) * .9, h - yy);
						if (h >= 0 && u < 15 + (h - yy) * .6 && ring < 65 && ring > 56)
							return ring > 62 ? UniverseMaterials.state(profile.geometry(), 1, 0) : rock;
					}
				}
				case WORLD_EYE -> {
					double radius = Math.hypot(u, h - 72), slab = Math.abs(v + u * .22);
					if (slab < 7 && radius > 48 && radius < 88 && (radius < 55 || radius > 80 || Math.abs(Math.sin(Math.atan2(h - 72, u) * 12)) < .13))
						return radius < 55 ? UniverseMaterials.state(profile.geometry(), 3, 0) : rock;
				}
			}
		}
		return null;
	}
}

package dev.kirill.hbk.universe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static dev.kirill.hbk.universe.UniverseNoise.*;

/** Pure, world-aligned caves and mine plans. Writes are clipped to the chunk being generated. */
public final class UniverseMines {
	private static final int SPACING = 128;
	private final UniverseProfile profile;
	private final UniverseDensity terrain;
	private final long seed;
	private final ConcurrentHashMap<Long, Optional<Site>> plans = new ConcurrentHashMap<>();
	public record Site(BlockPos entrance, Direction direction, int slope, long seed, boolean spiders) {
		public BlockPos pos(int u, int dy, int v) {
			return entrance.relative(direction, u).relative(direction.getClockWise(), v).offset(0, dy, 0);
		}
		public int floor(int u) { return entrance.getY() + slope * Math.max(0, Math.min(12, u)); }
		public BlockPos supplies() { return pos(25, slope * 12, 3); }
		public BlockPos cache() { return pos(40, slope * 12, 2); }
		public BlockPos nest() { return pos(24, slope * 12, 14); }
	}
	public UniverseMines(UniverseProfile profile) {
		this.profile = profile; terrain = new UniverseDensity(profile); seed = profile.stream(71);
	}
	/** Sampled on the generator's 4-block lattice: tunnels open naturally onto existing rock faces. */
	public double caveDensity(double x, double y, double z) {
		double a = noise(seed, x / 48, y / 34, z / 48);
		double b = noise(seed + 1, x / 49, y / 37, z / 49);
		double radius = profile.geometry() == 2 ? .095 : profile.geometry() == 7 ? .09 : .125;
		double tunnel = (Math.hypot(a, b) - radius) * 76;
		double chamber = (.64 - noise(seed + 2, x / 73, y / 47, z / 73)) * 62;
		return Math.min(tunnel, chamber);
	}
	public Optional<Site> site(int cellX, int cellZ) {
		long key = ((long) cellX << 32) ^ (cellZ & 0xffffffffL);
		// Eviction only affects computation time; a plan never depends on cache contents or neighbours.
		if (plans.size() > 512) plans.clear();
		return plans.computeIfAbsent(key, ignored -> plan(cellX, cellZ));
	}
	private Optional<Site> plan(int cx, int cz) {
		long choice = hash(seed + 3, cx, 0, cz);
		Direction[] directions = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
		int slope = profile.geometry() == 8 ? 1 : -1;
		for (int attempt = 0; attempt < 7; attempt++) {
			Direction direction = directions[(int) (choice + attempt & 3)];
			int x = cx * SPACING + 48 + (int) Long.remainderUnsigned(mix(choice + attempt * 31L), 32);
			int z = cz * SPACING + 48 + (int) Long.remainderUnsigned(mix(choice + attempt * 67L + 1), 32);
			for (int y = UniverseDensity.MIN_Y + UniverseDensity.HEIGHT - 24; y > UniverseDensity.MIN_Y + 24; y--) {
				int rockY = slope == -1 ? y - 1 : y + 4;
				if (terrain.sample(x, rockY, z) <= 0 || terrain.sample(x, y + (slope > 0 ? 2 : 0), z) >= 0) continue;
				if (terrain.sample(x, rockY + slope * 7, z) <= 2) continue;
				var candidate = new Site(new BlockPos(x, y, z), direction, slope, choice, (choice & 7) < 3);
				int rock = 0;
				for (int u : new int[]{12, 18, 24, 30, 38}) {
					BlockPos p = candidate.pos(u, slope * 12 + 1, 0);
					if (terrain.sample(p.getX(), p.getY(), p.getZ()) > 2) rock++;
				}
				BlockPos face = candidate.pos(44, slope * 12 + 1, 0);
				if (rock >= 3 && terrain.sample(face.getX(), face.getY(), face.getZ()) > 3) return Optional.of(candidate);
			}
		}
		return Optional.empty();
	}
	public List<Site> sitesForChunk(int x, int z) {
		List<Site> result = new ArrayList<>();
		for (int cx = Math.floorDiv(x - 48, SPACING); cx <= Math.floorDiv(x + 63, SPACING); cx++)
			for (int cz = Math.floorDiv(z - 48, SPACING); cz <= Math.floorDiv(z + 63, SPACING); cz++)
				site(cx, cz).ifPresent(result::add);
		return result;
	}
	/** Null means ordinary terrain. Main corridor, side passage and two chambers form one route. */
	public BlockState material(List<Site> sites, int x, int y, int z) {
		for (Site site : sites) {
			int dx = x - site.entrance.getX(), dz = z - site.entrance.getZ();
			int u = dx * site.direction.getStepX() + dz * site.direction.getStepZ();
			Direction side = site.direction.getClockWise();
			int v = dx * side.getStepX() + dz * side.getStepZ();
			if (u < -3 || u > 46 || Math.abs(v) > 18) continue;
			int floor = site.floor(u), h = y - floor;
			if (h < -1 || h > 5) continue;
			boolean main = u >= 0 && u <= 40 && Math.abs(v) <= 1;
			boolean entrance = u >= -2 && u <= 1 && Math.abs(v) <= 3;
			boolean junction = u >= 21 && u <= 27 && Math.abs(v) <= 4;
			boolean cross = u >= 23 && u <= 25 && Math.abs(v) <= 15;
			boolean chamber = u >= 36 && u <= 42 && Math.abs(v) <= 4;
			boolean passage = main || entrance || junction || cross || chamber;
			boolean frame = u == 0 || u >= 12 && u <= 36 && u % 6 == 0 && u != 24;
			if (passage && h == -1) {
				if (u > 0 && u < 12 && Math.abs(v) <= 1) return UniverseBotany.block(profile.geometry(), "stairs").defaultBlockState()
						.setValue(StairBlock.FACING, site.slope > 0 ? site.direction : site.direction.getOpposite());
				return UniverseBotany.block(profile.geometry(), "planks").defaultBlockState();
			}
			if (frame && Math.abs(v) == 2 && h >= 0 && h <= 2)
				return UniverseBotany.block(profile.geometry(), "log").defaultBlockState();
			if (frame && Math.abs(v) <= 2 && h == 3)
				return UniverseBotany.block(profile.geometry(), "log").defaultBlockState().setValue(RotatedPillarBlock.AXIS, side.getAxis());
			if (passage && h >= 0 && h <= 3) {
				if (y == site.supplies().getY() && (x == site.supplies().getX() && z == site.supplies().getZ()
						|| x == site.cache().getX() && z == site.cache().getZ()))
					return Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, site.direction.getOpposite());
				if (site.spiders && x == site.nest().getX() && y == site.nest().getY() && z == site.nest().getZ()) return Blocks.SPAWNER.defaultBlockState();
				if (h == 2 && frame && v == -1 && main) return Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
				if (site.spiders && Math.abs(v) >= 10 && (hash(site.seed, u, h, v) & 7) < 3) return Blocks.COBWEB.defaultBlockState();
				if (h == 0 && u >= 14 && u <= 34 && v == 0) return Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE,
						site.direction.getAxis() == Direction.Axis.X ? RailShape.EAST_WEST : RailShape.NORTH_SOUTH);
				return Blocks.CAVE_AIR.defaultBlockState();
			}
			// Large irregular seams visible in the working face, with an occasional luminous formation.
			if (u >= 43 && u <= 45 && Math.abs(v) <= 3 && h >= 0 && h <= 4
					&& terrain.sample(x, y, z) > 0) {
				long vein = hash(site.seed + 9, u, h, v);
				if ((vein & 7) < 4) return UniverseMaterials.state(profile.geometry(), 4, (int) (vein >>> 4 & 3));
				return UniverseMaterials.state(profile.geometry(), (vein & 7) == 4 ? 3 : 0, (int) (vein >>> 4 & 3));
			}
		}
		return null;
	}
	/** Place pending block entities in the owning protochunk, so they survive promotion and reload. */
	public void decorate(WorldGenLevel level, ChunkAccess chunk) {
		for (Site site : sitesForChunk(chunk.getPos().getMinBlockX(), chunk.getPos().getMinBlockZ())) {
			for (BlockPos pos : List.of(site.supplies(), site.cache())) {
				if (!inside(chunk, pos) || !chunk.getBlockState(pos).is(Blocks.CHEST)) continue;
				CompoundTag data = new CompoundTag();
				data.putString("id", "minecraft:chest"); data.putInt("x", pos.getX()); data.putInt("y", pos.getY()); data.putInt("z", pos.getZ());
				data.putString("LootTable", "hbk:chests/parallel_mine_" + UniverseMaterials.FAMILIES.get(profile.geometry()));
				data.putLong("LootTableSeed", hash(site.seed + 10, pos.getX(), pos.getY(), pos.getZ()));
				chunk.setBlockEntityNbt(data);
			}
			BlockPos nest = site.nest();
			if (site.spiders && inside(chunk, nest) && chunk.getBlockState(nest).is(Blocks.SPAWNER)) {
				var spawner = new SpawnerBlockEntity(nest, chunk.getBlockState(nest));
				spawner.setEntityId(EntityTypes.CAVE_SPIDER, net.minecraft.util.RandomSource.create(site.seed));
				chunk.setBlockEntityNbt(spawner.saveWithFullMetadata(level.registryAccess()));
			}
		}
	}
	private static boolean inside(ChunkAccess chunk, BlockPos pos) {
		return chunk.getPos().x() == Math.floorDiv(pos.getX(), 16) && chunk.getPos().z() == Math.floorDiv(pos.getZ(), 16);
	}
}

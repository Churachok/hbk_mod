package dev.kirill.hbk.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

/** Chunk-local writes only: no neighbour loading, world edits or dependence on generation order. */
public final class AnomalyTerrain {
	private AnomalyTerrain() {}

	public static void decorate(WorldGenLevel level, ChunkAccess chunk) {
		if (RepeatedAnomaly.capturing()) return;
		if (!level.getLevel().dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) return;
		int startX = chunk.getPos().getMinBlockX(), startZ = chunk.getPos().getMinBlockZ();
		var zone = AnomalyZones.forChunk(AnomalyZones.layoutSeed(level.getSeed()), startX, startZ);
		if (zone == null) return;
		if (zone.kind() >= 5) {
			if (zone.kind() != 6) NewAnomalyTerrain.decorate(level, chunk, zone);
			return;
		}
		var pos = new BlockPos.MutableBlockPos();
		int minY = chunk.getMinY();
		int maxY = zone.kind() == 4 ? chunk.getMaxY() : Math.min(chunk.getMaxY(), AnomalyZones.CEILING_Y - 16);
		// Snapshot before transforming so one column never samples another transformed column.
		BlockState[][] columns = new BlockState[256][];
		int[] surfaces = new int[256];
		for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
			int index = x * 16 + z;
			columns[index] = new BlockState[maxY - minY];
			for (int y = minY; y < maxY; y++) columns[index][y - minY] = chunk.getBlockState(pos.set(startX + x, y, startZ + z));
			int top = Math.min(maxY - (zone.kind() == 4 ? 1 : 25), chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z));
			top = Math.max(minY, top);
			while (top > minY && (columns[index][top - minY].is(BlockTags.LOGS)
					|| columns[index][top - minY].is(BlockTags.LEAVES)
					|| !columns[index][top - minY].isSolid())) top--;
			surfaces[index] = top;
		}
		for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
			int wx = startX + x, wz = startZ + z, index = x * 16 + z, top = surfaces[index];
			BlockState[] column = columns[index];
			BlockState surface = column[top - minY];
			BlockState under = column[Math.max(0, top - minY - 3)];
			BlockState rock = column[Math.max(0, top - minY - 12)];
			if (!rock.isSolid() || rock.hasBlockEntity() || rock.is(Blocks.BEDROCK)) rock = Blocks.STONE.defaultBlockState();
			if (!surface.isSolid() || surface.hasBlockEntity()) surface = rock;
			if (!under.isSolid() || under.hasBlockEntity()) under = rock;
			if (zone.kind() == 1) {
				// A second copy of the actual terrain, caves and vegetation, reflected above the first.
				int mirror = Math.min(maxY - 28, Math.max(top + 48, 312 - top));
				for (int source = Math.max(minY, top - 28); source <= top + 24 && source < maxY; source++) {
					int target = mirror - (source - top);
					BlockState state = column[source - minY];
					if (target < maxY && target > top + 20 && !state.isAir() && !state.is(Blocks.BEDROCK) && !state.hasBlockEntity()) {
						put(chunk, pos.set(wx, target, wz), stable(state));
					}
				}
			} else if (zone.kind() == 2) {
				var geometry = AnomalyGeometry.column(zone, wx, wz);
				int highest = top;
				for (int y = Math.max(minY + 10, top + 1); y < maxY; y++) {
					double distance = geometry.distance(y);
					if (distance < 0) {
						put(chunk, pos.set(wx, y, wz), distance > -1.5 ? stable(surface) : distance > -4 ? under : rock);
						highest = y;
					}
				}
				if (highest > top + 8) copyVegetation(chunk, pos, column, minY, maxY, top, highest, wx, wz);
			} else if (zone.kind() == 4) {
				int floor = AnomalyCrater.floor(wx - zone.minX() - AnomalyZones.SIZE / 2.0 + 0.5,
						wz - zone.minZ() - AnomalyZones.SIZE / 2.0 + 0.5, top, minY);
				if (floor != AnomalyCrater.UNCHANGED) {
					// Cut the entire column, including fluids, vegetation and the world's bedrock floor.
					// The center is open to the void; exposed walls retain native stone, ores and caves.
					for (int y = Math.max(minY, floor + 1); y < maxY; y++) {
						if (!chunk.getBlockState(pos.set(wx, y, wz)).isAir()) put(chunk, pos, Blocks.AIR.defaultBlockState());
					}
				}
			} else {
				// Fold the native column upward into a sheer, corrugated wall with a curved foot.
				double dx = wx - zone.minX();
				double bend = Math.min(1, Math.max(0, (dx - 20) / 100.0));
				int lift = (int) Math.round(145 * (1 - Math.sqrt(1 - bend * bend)));
				int raised = Math.min(maxY - 25, top + lift);
				for (int y = top + 1; y <= raised; y++) {
					put(chunk, pos.set(wx, y, wz), y == raised ? stable(surface) : y > raised - 4 ? under : rock);
				}
				if (lift > 0) copyVegetation(chunk, pos, column, minY, maxY, top, raised, wx, wz);
				// Rotate the local native canopy from X/Y into Y/-X, keeping its real species and silhouette.
				if (dx >= 96 && dx < 120) {
					int offset = 120 - (int) dx;
					for (int y = top + 32; y < Math.min(maxY, top + 128); y++) {
						int sourceIndex = Math.floorMod(y - top - 32, 32) * 16 + z;
						if (sourceIndex >= columns.length) continue;
						int sourceY = surfaces[sourceIndex] + offset;
						if (sourceY >= maxY) continue;
						BlockState state = columns[sourceIndex][sourceY - minY];
						if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)) {
							if (state.hasProperty(BlockStateProperties.AXIS)) {
								var axis = state.getValue(BlockStateProperties.AXIS);
								state = state.setValue(BlockStateProperties.AXIS, axis == Direction.Axis.Y ? Direction.Axis.X
										: axis == Direction.Axis.X ? Direction.Axis.Y : axis);
							}
							put(chunk, pos.set(wx, y, wz), stable(state));
						}
					}
				}
			}
			floatingBlocks(chunk, pos, zone, wx, wz, top, maxY, rock, surface);
			int localX = wx - zone.minX(), localZ = wz - zone.minZ();
			if (localX < 2 || localZ < 2 || localX >= AnomalyZones.SIZE - 2 || localZ >= AnomalyZones.SIZE - 2) {
				int height = 1 + Math.floorMod(wx * 31 + wz * 17, 2);
				int ground = Math.min(maxY - 3, chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z));
				// In the inverted zone the border belongs on the ground, below the floating copy.
				if (zone.kind() == 1) ground = top;
				for (int y = 0; y < height; y++) put(chunk, pos.set(wx, ground + 1 + y, wz), Blocks.BEDROCK.defaultBlockState());
			}
		}
		FracturedClusters.generate(level, chunk, zone, surfaces[8 * 16 + 8]);
	}

	private static void floatingBlocks(ChunkAccess chunk, BlockPos.MutableBlockPos pos, AnomalyZones.Zone zone,
			int x, int z, int top, int maxY, BlockState rock, BlockState surface) {
		int localX = x - zone.minX(), localZ = z - zone.minZ();
		if (localX < 8 || localZ < 8 || localX >= AnomalyZones.SIZE - 8 || localZ >= AnomalyZones.SIZE - 8) return;
		if (zone.kind() == 4 && Math.hypot(localX + 0.5 - AnomalyZones.SIZE / 2.0,
				localZ + 0.5 - AnomalyZones.SIZE / 2.0) < 30) return;
		int gridX = Math.floorDiv(x, 12), gridZ = Math.floorDiv(z, 12);
		long hash = (long) gridX * 341873128712L ^ (long) gridZ * 132897987541L;
		hash = (hash ^ hash >>> 30) * 0xbf58476d1ce4e5b9L;
		hash = (hash ^ hash >>> 27) * 0x94d049bb133111ebL;
		hash ^= hash >>> 31;
		if (Math.floorMod(hash, 3) != 0) return;
		int dx = x - (gridX * 12 + 4 + (int) (hash >>> 4 & 3));
		int dz = z - (gridZ * 12 + 4 + (int) (hash >>> 8 & 3));
		if (Math.abs(dx) + Math.abs(dz) > 1 || (dx != 0 || dz != 0) && (hash >>> (12 + dx + 3 * dz) & 1) == 0) return;
		int y = Math.max(top + 12, 96 + (int) Math.floorMod(hash >>> 20, 160));
		if (y >= Math.min(maxY - 1, AnomalyZones.CEILING_Y - 20)) return;
		if (chunk.getBlockState(pos.set(x, y, z)).isAir()) put(chunk, pos, stable((hash & 1) == 0 ? rock : surface));
	}

	private static void copyVegetation(ChunkAccess chunk, BlockPos.MutableBlockPos pos, BlockState[] column,
			int minY, int maxY, int top, int targetTop, int x, int z) {
		for (int offset = 1; offset <= 24 && top + offset < maxY && targetTop + offset < maxY; offset++) {
			BlockState state = column[top + offset - minY];
			if (!state.isAir() && !state.hasBlockEntity() && state.getFluidState().isEmpty()) {
				put(chunk, pos.set(x, targetTop + offset, z), stable(state));
			}
		}
	}

	private static BlockState stable(BlockState state) {
		return state.hasProperty(BlockStateProperties.PERSISTENT) ? state.setValue(BlockStateProperties.PERSISTENT, true) : state;
	}

	private static void put(ChunkAccess chunk, BlockPos pos, BlockState state) {
		// Never replace a structure container, including in newly generated chunks.
		if (!chunk.getBlockState(pos).hasBlockEntity()) chunk.setBlockState(pos, state);
	}
}

package dev.kirill.hbk.world;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

/** New shapes operate only on the currently generated chunk, never on an explored neighbour. */
public final class NewAnomalyTerrain {
	private NewAnomalyTerrain() {}

	public static void decorate(WorldGenLevel level, ChunkAccess chunk, AnomalyZones.Zone zone) {
		// Claim the chunk before taking the snapshot, so neighbouring features cannot race the transform.
		((AnomalyChunkMarker) chunk).hbk$markAnomalyDecorated();
		int minY = chunk.getMinY(), maxY = chunk.getMaxY();
		int wx = chunk.getPos().getMinBlockX(), wz = chunk.getPos().getMinBlockZ();
		var pos = new BlockPos.MutableBlockPos();
		BlockState[][] columns = new BlockState[256][maxY - minY];
		int[] surfaces = new int[256];
		for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
			int index = x * 16 + z;
			for (int y = minY; y < maxY; y++) columns[index][y - minY] = chunk.getBlockState(pos.set(wx + x, y, wz + z));
			surfaces[index] = surface(chunk, wx + x, wz + z);
		}
		var source = level.getLevel().getChunkSource();
		int reference = zone.kind() == 7 ? source.getGenerator().getBaseHeight(zone.centerX(), zone.centerZ(),
				Heightmap.Types.OCEAN_FLOOR_WG, chunk, source.randomState()) - 1 : 0;
		long seed = AnomalyZones.layoutSeed(level.getSeed()) ^ (long) zone.minX() * 31 ^ zone.minZ();
		for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
			int px = wx + x, pz = wz + z, index = x * 16 + z, top = surfaces[index];
			if (!zone.contains(px + 0.5, pz + 0.5)) continue;
			var column = columns[index];
			if (zone.kind() == 5) {
				var fragment = AnomalyShattered.fragment(seed, px, pz);
				int lift = fragment == null ? 0 : Math.clamp(top + fragment.lift(), minY + 56,
						Math.min(maxY - 64, AnomalyZones.CEILING_Y - 64)) - top;
				for (int y = minY; y < maxY; y++) {
					int original = y - lift;
					BlockState state = Blocks.AIR.defaultBlockState();
					if (fragment != null && original >= Math.max(minY, top - fragment.depth())
							&& original < Math.min(maxY, top + 49)) {
						state = column[original - minY];
						if (state.is(Blocks.BEDROCK) || state.hasBlockEntity()) state = Blocks.AIR.defaultBlockState();
					}
					put(chunk, pos.set(px, y, pz), stable(state));
				}
			} else if (zone.kind() == 7) {
				int lift = AnomalyInvertedHeights.targetHeight(top, reference, minY, maxY) - top;
				BlockState foundation = column[Math.min(maxY - minY - 1, 12)];
				if (!foundation.isSolid() || foundation.is(Blocks.BEDROCK) || foundation.hasBlockEntity()) foundation = Blocks.DEEPSLATE.defaultBlockState();
				// The original basal bedrock is retained in this type, including in deep valleys.
				for (int y = minY + 5; y < maxY; y++) {
					int original = y - lift;
					BlockState state = original < minY + 5 ? foundation : original >= maxY
							? Blocks.AIR.defaultBlockState() : column[original - minY];
					if (state.is(Blocks.BEDROCK)) state = foundation;
					if (state.hasBlockEntity()) state = Blocks.AIR.defaultBlockState();
					put(chunk, pos.set(px, y, pz), stable(state));
				}
			}
			if (zone.border(px, pz)) border(chunk, pos, zone, px, pz, zone.kind() == 5 ? top : surface(chunk, px, pz));
		}
		FracturedClusters.generate(level, chunk, zone, surfaces[8 * 16 + 8]);
	}

	public static int surface(ChunkAccess chunk, int x, int z) {
		int y = Math.min(chunk.getMaxY() - 1, chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x & 15, z & 15));
		var pos = new BlockPos.MutableBlockPos();
		while (y > chunk.getMinY()) {
			var state = chunk.getBlockState(pos.set(x, y, z));
			if (state.isSolid() && !state.is(BlockTags.LOGS) && !state.is(BlockTags.LEAVES)) break;
			y--;
		}
		return y;
	}

	public static void border(ChunkAccess chunk, BlockPos.MutableBlockPos pos, AnomalyZones.Zone zone, int x, int z, int ground) {
		if (!zone.border(x, z)) return;
		ground = Math.clamp(ground, chunk.getMinY(), chunk.getMaxY() - 3);
		int height = 1 + Math.floorMod(x * 31 + z * 17, 2);
		for (int offset = 1; offset <= height; offset++) put(chunk, pos.set(x, ground + offset, z), Blocks.BEDROCK.defaultBlockState());
	}

	static BlockState stable(BlockState state) {
		return state.hasProperty(BlockStateProperties.PERSISTENT) ? state.setValue(BlockStateProperties.PERSISTENT, true) : state;
	}

	static void put(ChunkAccess chunk, BlockPos pos, BlockState state) {
		var old = chunk.getBlockState(pos);
		if (old != state && !old.hasBlockEntity()) chunk.setBlockState(pos, state);
	}
}

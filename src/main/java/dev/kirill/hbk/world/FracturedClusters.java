package dev.kirill.hbk.world;

import dev.kirill.hbk.block.FracturedBlock;
import dev.kirill.hbk.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.ticks.ScheduledTick;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

/** Whole 5–20 block clusters, contained in one chunk and placed only in empty space. */
public final class FracturedClusters {
	private FracturedClusters() {}

	public static List<BlockPos> shape(int kind, int count) {
		var points = new LinkedHashSet<BlockPos>();
		if (kind == 0) {
			// Two broken lanes; the missing spans remain actual gaps rather than ghost duplicates.
			for (int step = 0; step < 12; step++) {
				if (step == 4 || step == 8) continue;
				for (int lane = 0; lane < 2; lane++) points.add(new BlockPos(step - 6, step / 5, lane));
			}
		} else if (kind == 1) {
			var island = new ArrayList<BlockPos>();
			for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
				if (Math.abs(x) + Math.abs(z) <= 3) island.add(new BlockPos(x, 0, z));
			}
			island.sort(Comparator.comparingInt(p -> p.getX() * p.getX() + p.getZ() * p.getZ()));
			points.addAll(island);
		} else {
			for (int step = 0; step < 80 && points.size() < count; step++) {
				points.add(new BlockPos((int) Math.round(Math.cos(step * 0.75) * 3), step / 4,
						(int) Math.round(Math.sin(step * 0.75) * 3)));
			}
		}
		return points.stream().limit(count).toList();
	}

	public static void generate(WorldGenLevel level, ChunkAccess chunk, AnomalyZones.Zone zone, int surfaceY) {
		long seed = level.getSeed() ^ (long) chunk.getPos().x() * 341873128712L
				^ (long) chunk.getPos().z() * 132897987541L ^ 0xF4AC741L;
		var random = RandomSource.create(seed);
		if (random.nextInt(3) != 0 && zone.kind() != 6) return;
		int x = chunk.getPos().getMinBlockX() + 8, z = chunk.getPos().getMinBlockZ() + 8;
		if (zone.kind() != 6 && (x - zone.minX() < 12 || z - zone.minZ() < 12
				|| x - zone.minX() >= zone.size() - 12 || z - zone.minZ() >= zone.size() - 12)) return;
		if (zone.kind() == 4 && Math.hypot(x - zone.minX() - 96, z - zone.minZ() - 96) < 40) return;
		int kind = random.nextInt(3), count = 5 + random.nextInt(16), rotation = random.nextInt(4);
		var offsets = shape(kind, count);
		int bottom = Math.max(chunk.getMinY() + 8, Math.min(240,
				surfaceY + 1 + (kind == 0 ? 0 : 8 + random.nextInt(25))));
		int ceiling = Math.min(chunk.getMaxY() - 8, AnomalyZones.CEILING_Y - 20);
		for (int y = bottom; y < ceiling - 8; y += 4) {
			var cluster = new ArrayList<BlockPos>();
			for (var offset : offsets) {
				int ox = offset.getX(), oz = offset.getZ();
				for (int turn = 0; turn < rotation; turn++) { int oldX = ox; ox = -oz; oz = oldX; }
				var pos = new BlockPos(x + ox, y + offset.getY(), z + oz);
				if (zone.kind() != 6 && (!zone.contains(pos.getX() + 0.5, pos.getZ() + 0.5) || zone.border(pos.getX(), pos.getZ()))) break;
				if (!chunk.getBlockState(pos).isAir()) break;
				cluster.add(pos);
			}
			if (cluster.size() != count) continue;
			for (var pos : cluster) {
				var phase = FracturedBlock.Phase.values()[random.nextInt(3)];
				chunk.setBlockState(pos, ModBlocks.FRACTURED_BLOCK.defaultBlockState().setValue(FracturedBlock.PHASE, phase));
				// Proto-chunk ticks are persisted and activated on load; worldgen does not invoke onPlace.
				chunk.getBlockTicks().schedule(new ScheduledTick<>(ModBlocks.FRACTURED_BLOCK, pos,
						level.getGameTime() + FracturedBlock.duration(phase, random), 0));
			}
			return;
		}
	}
}

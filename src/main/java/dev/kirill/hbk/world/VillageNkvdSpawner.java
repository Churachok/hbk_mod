package dev.kirill.hbk.world;

import dev.kirill.hbk.util.NkvdSpawning;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.List;

public final class VillageNkvdSpawner {
	private VillageNkvdSpawner() {
	}

	public static void trySpawnInChunk(ServerLevel level, ChunkAccess chunk) {
		ChunkPos chunkPos = chunk.getPos();
		List<StructureStart> villages = level.structureManager().startsForStructure(
				chunkPos,
				structure -> isVillageStructure(level, structure)
		);
		if (villages.isEmpty()) {
			return;
		}

		ModWorldData data = ModWorldData.get(level);
		RandomSource random = level.getRandom();

		for (StructureStart village : villages) {
			if (!village.isValid()) {
				continue;
			}

			BoundingBox box = village.getBoundingBox();
			long key = ModWorldData.villageKey(box.minX(), box.minY(), box.minZ());
			if (data.isVillageRaided(key)) {
				continue;
			}

			BlockPos center = new BlockPos(
					(box.minX() + box.maxX()) / 2,
					box.minY(),
					(box.minZ() + box.maxZ()) / 2
			);
			if (!level.hasChunkAt(center)) {
				continue;
			}

			data.markVillageRaided(key);
			if (random.nextFloat() >= 0.50f) {
				continue;
			}

			BlockPos spawnPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, center);
			if (!level.hasChunkAt(spawnPos)) {
				continue;
			}
			NkvdSpawning.spawnVillageRaider(level, spawnPos);
		}
	}

	private static boolean isVillageStructure(ServerLevel level, Structure structure) {
		return level.registryAccess().lookupOrThrow(Registries.STRUCTURE)
				.getResourceKey(structure)
				.flatMap(key -> level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(key))
				.map(holder -> holder.is(StructureTags.VILLAGE))
				.orElse(false);
	}
}

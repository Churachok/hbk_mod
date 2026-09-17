package dev.kirill.hbk.world;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.registry.ModBlocks;
import dev.kirill.hbk.registry.ModEntityTypes;
import dev.kirill.hbk.util.NkvdSpawning;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.List;

/** Adds the one-time inhabitants and special block after a native structure is first visited. */
public final class ModStructurePopulator {
	private static final Identifier KIRILL_HOUSE = HbkMod.id("kirill_house");
	private static final Identifier KIRILL_HOUSE_HBK = HbkMod.id("kirill_house_hbk");
	private static final Identifier STALINKA = HbkMod.id("stalinka");
	private static final Identifier GULAG = HbkMod.id("gulag");
	private static final int VILLAGER_COUNT = 8;
	private static final float VILLAGER_CHANCE = 0.60f;
	private static final float LIZA_CHANCE = 0.60f;
	private static final int NKVD_COUNT = 10;

	private ModStructurePopulator() {
	}

	public static void tryPopulateInChunk(ServerLevel level, ChunkAccess chunk) {
		List<StructureStart> starts = level.structureManager().startsForStructure(
				chunk.getPos(),
				structure -> getModStructureId(level, structure) != null
		);
		if (starts.isEmpty()) {
			return;
		}

		ModWorldData data = ModWorldData.get(level);
		for (StructureStart start : starts) {
			if (!start.isValid()) {
				continue;
			}

			Identifier id = getModStructureId(level, start.getStructure());
			if (id == null) {
				continue;
			}

			String populationKey = id + "@" + start.getChunkPos().pack();
			BoundingBox box = start.getBoundingBox();
			if (!level.hasChunksAt(box.minX(), box.minZ(), box.maxX(), box.maxZ())) {
				continue;
			}

			if (!data.isStructurePopulated(populationKey)) {
				if (id.equals(KIRILL_HOUSE)) {
					spawnNurse(level, box);
				} else if (id.equals(STALINKA)) {
					if (level.getRandom().nextFloat() < VILLAGER_CHANCE) {
						spawnVillagers(level, box);
					}
				} else if (id.equals(GULAG)) {
					populateGulag(level, box);
				}
				data.markStructurePopulated(populationKey);
			}

			if (id.equals(KIRILL_HOUSE) || id.equals(KIRILL_HOUSE_HBK)) {
				String kirillPopulationKey = "kirill@" + id + "@" + start.getChunkPos().pack();
				if (!data.isStructurePopulated(kirillPopulationKey) && spawnKirillNear(level, box)) {
					data.markStructurePopulated(kirillPopulationKey);
				}
			}
			if (id.equals(STALINKA)) {
				String lizaPopulationKey = "liza@" + id + "@" + start.getChunkPos().pack();
				if (!data.isStructurePopulated(lizaPopulationKey)) {
					if (level.getRandom().nextFloat() < LIZA_CHANCE) {
						spawnLiza(level, box);
					}
					data.markStructurePopulated(lizaPopulationKey);
				}
			}
		}
	}

	private static Identifier getModStructureId(ServerLevel level, Structure structure) {
		return level.registryAccess().lookupOrThrow(Registries.STRUCTURE)
				.getResourceKey(structure)
				.map(key -> key.identifier())
				.filter(id -> id.getNamespace().equals(HbkMod.MOD_ID))
				.filter(id -> id.equals(KIRILL_HOUSE) || id.equals(KIRILL_HOUSE_HBK)
						|| id.equals(STALINKA) || id.equals(GULAG))
				.orElse(null);
	}

	private static void spawnNurse(ServerLevel level, BoundingBox box) {
		BlockPos pos = findStandingPosition(level, box, 200);
		if (pos == null || ModEntityTypes.NURSE.spawn(level, pos, EntitySpawnReason.STRUCTURE) == null) {
			HbkMod.LOGGER.warn("Could not spawn the nurse in Kirill's house at {}", box);
		}
	}

	private static boolean spawnKirillNear(ServerLevel level, BoundingBox box) {
		BlockPos pos = findPositionNearStructure(level, box, 120);
		if (pos == null || ModEntityTypes.KIRILL.spawn(level, pos, EntitySpawnReason.STRUCTURE) == null) {
			HbkMod.LOGGER.warn("Could not spawn Kirill near his house at {}", box);
			return false;
		}
		return true;
	}

	private static void spawnLiza(ServerLevel level, BoundingBox box) {
		BlockPos pos = findStandingPosition(level, box, 200);
		if (pos == null || ModEntityTypes.LIZA.spawn(level, pos, EntitySpawnReason.STRUCTURE) == null) {
			HbkMod.LOGGER.warn("Could not spawn Liza in the Stalinka at {}", box);
		}
	}

	private static BlockPos findPositionNearStructure(ServerLevel level, BoundingBox box, int attempts) {
		RandomSource random = level.getRandom();
		for (int attempt = 0; attempt < attempts; attempt++) {
			int x = random.nextInt(box.minX() - 10, box.maxX() + 11);
			int z = random.nextInt(box.minZ() - 10, box.maxZ() + 11);
			if (x >= box.minX() - 2 && x <= box.maxX() + 2
					&& z >= box.minZ() - 2 && z <= box.maxZ() + 2) {
				continue;
			}

			BlockPos column = new BlockPos(x, box.maxY(), z);
			if (!level.hasChunkAt(column)) {
				continue;
			}
			BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
			if (level.getBlockState(pos).isAir()
					&& level.getBlockState(pos.above()).isAir()
					&& level.getBlockState(pos.below()).isSolid()) {
				return pos;
			}
		}
		return null;
	}

	private static void spawnVillagers(ServerLevel level, BoundingBox box) {
		RandomSource random = level.getRandom();
		int spawned = 0;
		for (int attempt = 0; attempt < VILLAGER_COUNT * 30 && spawned < VILLAGER_COUNT; attempt++) {
			BlockPos spawnPos = findStandingPosition(level, box, 1);
			if (spawnPos == null) {
				continue;
			}
			Villager villager = EntityTypes.VILLAGER.create(level, EntitySpawnReason.STRUCTURE);
			if (villager == null) {
				continue;
			}
			villager.snapTo(spawnPos, random.nextFloat() * 360.0f, 0.0f);
			villager.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), EntitySpawnReason.STRUCTURE, null);
			villager.setPersistenceRequired();
			if (level.addFreshEntity(villager)) {
				spawned++;
			}
		}
	}

	private static void populateGulag(ServerLevel level, BoundingBox box) {
		BlockPos chestColumn = new BlockPos((box.minX() + box.maxX()) / 2, 0, box.minZ() - 2);
		BlockPos chestPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, chestColumn);
		if (level.getBlockState(chestPos).canBeReplaced()) {
			level.setBlockAndUpdate(chestPos, ModBlocks.STRANGE_CHEST.defaultBlockState());
		}

		BlockPos origin = new BlockPos(box.minX(), box.minY(), box.minZ());
		Vec3i size = new Vec3i(box.getXSpan(), box.getYSpan(), box.getZSpan());
		NkvdSpawning.spawnSquadInArea(level, origin, size, NKVD_COUNT);
	}

	private static BlockPos findStandingPosition(ServerLevel level, BoundingBox box, int attempts) {
		RandomSource random = level.getRandom();
		int minX = box.minX() + 1;
		int maxX = box.maxX() - 1;
		int minZ = box.minZ() + 1;
		int maxZ = box.maxZ() - 1;
		if (minX > maxX || minZ > maxZ) {
			return null;
		}

		for (int attempt = 0; attempt < attempts; attempt++) {
			int x = random.nextInt(minX, maxX + 1);
			int z = random.nextInt(minZ, maxZ + 1);
			for (int y = box.minY() + 1; y < box.maxY(); y++) {
				BlockPos pos = new BlockPos(x, y, z);
				if (level.getBlockState(pos).isAir()
						&& level.getBlockState(pos.above()).isAir()
						&& level.getBlockState(pos.below()).isSolid()) {
					return pos;
				}
			}
		}
		return null;
	}
}

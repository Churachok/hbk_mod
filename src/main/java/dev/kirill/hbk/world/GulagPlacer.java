package dev.kirill.hbk.world;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.registry.ModBlocks;
import dev.kirill.hbk.util.NkvdSpawning;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.Optional;

public final class GulagPlacer {
	private static final int NKVD_COUNT = 10;

	private GulagPlacer() {
	}

	public static BlockPos placeGulag(ServerLevel level, BlockPos anchor) {
		Optional<StructureTemplate> template = level.getStructureManager().get(HbkMod.id("gulag"));
		if (template.isEmpty()) {
			HbkMod.LOGGER.warn("Gulag structure template hbk:gulag is missing");
			return null;
		}

		int chunkX = anchor.getX() >> 4;
		int chunkZ = anchor.getZ() >> 4;
		if (!level.hasChunk(chunkX, chunkZ)) {
			return null;
		}

		StructureTemplate gulag = template.get();
		Vec3i size = gulag.getSize();
		BlockPos surface = findTerrainSurface(level, anchor);
		BlockPos origin = new BlockPos(
				surface.getX() - size.getX() / 2,
				Math.max(level.getMinY() + 1, surface.getY() - 1),
				surface.getZ() - size.getZ() / 2
		);

		StructurePlaceSettings settings = new StructurePlaceSettings()
				.setIgnoreEntities(true)
				.setKnownShape(true);
		boolean placed = gulag.placeInWorld(level, origin, origin, settings, level.getRandom(), Block.UPDATE_CLIENTS);
		if (!placed) {
			HbkMod.LOGGER.warn("Failed to place gulag at {}", origin);
			return null;
		}

		BlockPos chestColumn = new BlockPos(origin.getX() + size.getX() / 2, 0, origin.getZ() - 2);
		BlockPos strangeChestPos = findTerrainSurface(level, chestColumn);
		if (level.getBlockState(strangeChestPos).canBeReplaced()) {
			level.setBlockAndUpdate(strangeChestPos, ModBlocks.STRANGE_CHEST.defaultBlockState());
		}

		NkvdSpawning.spawnSquadInArea(level, origin, size, NKVD_COUNT);
		HbkMod.LOGGER.info("Placed gulag at {}", origin);
		return origin;
	}

	private static BlockPos findTerrainSurface(ServerLevel level, BlockPos pos) {
		BlockPos cursor = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos).below();
		while (cursor.getY() > level.getMinY()) {
			BlockState state = level.getBlockState(cursor);
			if (!state.is(BlockTags.LOGS) && !state.is(BlockTags.LEAVES)) {
				break;
			}
			cursor = cursor.below();
		}
		return cursor.above();
	}

}

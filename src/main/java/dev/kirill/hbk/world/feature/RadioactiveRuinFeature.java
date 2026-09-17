package dev.kirill.hbk.world.feature;

import dev.kirill.hbk.registry.ModBlocks;
import dev.kirill.hbk.world.ModWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Procedurally assembles a different bombed-out concrete building on every placement. */
public final class RadioactiveRuinFeature extends Feature<NoneFeatureConfiguration> {
	public RadioactiveRuinFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel level = context.level();
		RandomSource random = context.random();
		int centerX = context.origin().getX();
		int centerZ = context.origin().getZ();
		int groundY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, centerX, centerZ) - 1;
		BlockPos center = new BlockPos(centerX, groundY, centerZ);
		if (!level.getBiome(center).is(ModWorldgen.RADIOACTIVE_WASTELAND)) {
			return false;
		}

		int width = 7 + random.nextInt(4);
		int length = 8 + random.nextInt(5);
		int stories = 2 + random.nextInt(3);
		int minX = centerX - width / 2;
		int minZ = centerZ - length / 2;

		for (int x = 0; x < width; x++) {
			for (int z = 0; z < length; z++) {
				int floorY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, minX + x, minZ + z) - 1;
				for (int y = floorY; y <= groundY; y++) {
					level.setBlock(new BlockPos(minX + x, y, minZ + z), ruinStone(random), 2);
				}
			}
		}

		for (int story = 0; story < stories; story++) {
			int baseY = groundY + 1 + story * 4;
			float decay = 0.12f + story * 0.10f;
			for (int x = 0; x < width; x++) {
				for (int z = 0; z < length; z++) {
					boolean edge = x == 0 || x == width - 1 || z == 0 || z == length - 1;
					if (!edge) {
						continue;
					}
					for (int y = 0; y < 4; y++) {
						boolean doorway = story == 0 && z == 0 && x >= width / 2 - 1 && x <= width / 2 && y < 3;
						boolean window = y == 1 && ((x == 0 || x == width - 1) ? z % 3 == 1 : x % 3 == 1);
						if (doorway || random.nextFloat() < decay) {
							continue;
						}
						BlockPos pos = new BlockPos(minX + x, baseY + y, minZ + z);
						level.setBlock(pos, window ? Blocks.IRON_BARS.defaultBlockState() : ruinStone(random), 2);
					}
				}
			}

			if (story < stories - 1) {
				for (int x = 1; x < width - 1; x++) {
					for (int z = 1; z < length - 1; z++) {
						if (random.nextFloat() > 0.24f + story * 0.12f) {
							level.setBlock(new BlockPos(minX + x, baseY + 3, minZ + z), ruinStone(random), 2);
						}
					}
				}
			}
		}

		placeBrokenBeams(level, random, minX, minZ, groundY + 4, width, length);
		placeWatchPost(level, random, centerX + width / 2 + 3, centerZ, groundY);
		return true;
	}

	private static BlockState ruinStone(RandomSource random) {
		return switch (random.nextInt(7)) {
			case 0 -> ModBlocks.CRACKED_RADIOACTIVE_STONE.defaultBlockState();
			case 1 -> ModBlocks.SCORCHED_STONE.defaultBlockState();
			default -> ModBlocks.RADIOACTIVE_STONE.defaultBlockState();
		};
	}

	private static void placeBrokenBeams(WorldGenLevel level, RandomSource random, int minX, int minZ,
			int y, int width, int length) {
		for (int z = 1; z < length - 1; z += 3) {
			int reach = 2 + random.nextInt(Math.max(2, width - 3));
			for (int x = 1; x <= reach && x < width - 1; x++) {
				level.setBlock(new BlockPos(minX + x, y, minZ + z), ModBlocks.DRY_LOG.defaultBlockState()
						.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X), 2);
			}
		}
	}

	private static void placeWatchPost(WorldGenLevel level, RandomSource random, int x, int z, int baseY) {
		int height = 7 + random.nextInt(4);
		for (int y = 1; y <= height; y++) {
			for (int dx : new int[]{-1, 1}) {
				for (int dz : new int[]{-1, 1}) {
					if (random.nextFloat() > 0.10f) {
						level.setBlock(new BlockPos(x + dx, baseY + y, z + dz), ruinStone(random), 2);
					}
				}
			}
			if (y == height / 2 || y == height) {
				for (int dx = -2; dx <= 2; dx++) {
					for (int dz = -2; dz <= 2; dz++) {
						if (Math.abs(dx) == 2 || Math.abs(dz) == 2 || y == height / 2) {
							level.setBlock(new BlockPos(x + dx, baseY + y, z + dz), ruinStone(random), 2);
						}
					}
				}
			}
		}
		level.setBlock(new BlockPos(x, baseY + height + 1, z), ModBlocks.URANIUM_ORE.defaultBlockState(), 2);
	}
}

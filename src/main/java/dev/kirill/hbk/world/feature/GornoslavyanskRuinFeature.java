package dev.kirill.hbk.world.feature;

import dev.kirill.hbk.registry.ModBlocks;
import dev.kirill.hbk.world.ModWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Procedurally builds one of Gornoslavyansk's hollow, bombed-out apartment blocks. */
public final class GornoslavyanskRuinFeature extends Feature<NoneFeatureConfiguration> {
	private static final int STORY_HEIGHT = 4;

	public GornoslavyanskRuinFeature() {
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
		if (!level.getBiome(center).is(ModWorldgen.PAST_GORNOSLAVYANSK)) {
			return false;
		}

		int width = 9 + random.nextInt(5);
		int length = 9 + random.nextInt(6);
		int stories = 4 + random.nextInt(4);
		int minX = centerX - width / 2;
		int minZ = centerZ - length / 2;
		if (!terrainIsSuitable(level, minX, minZ, width, length, groundY)) {
			return false;
		}

		placeFoundation(level, random, minX, minZ, width, length, groundY);
		for (int story = 0; story < stories; story++) {
			int baseY = groundY + 1 + story * STORY_HEIGHT;
			float decay = 0.08f + story * 0.065f;
			placeStory(level, random, minX, minZ, width, length, story, baseY, decay);
		}

		placeBrokenRoof(level, random, minX, minZ, width, length, groundY + 1 + stories * STORY_HEIGHT);
		placeExteriorDebris(level, random, minX, minZ, width, length, groundY);
		return true;
	}

	private static boolean terrainIsSuitable(WorldGenLevel level, int minX, int minZ, int width, int length, int baseY) {
		int[][] samples = {
				{minX, minZ}, {minX + width - 1, minZ}, {minX, minZ + length - 1},
				{minX + width - 1, minZ + length - 1}, {minX + width / 2, minZ + length / 2}
		};
		for (int[] sample : samples) {
			int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, sample[0], sample[1]) - 1;
			BlockPos pos = new BlockPos(sample[0], y, sample[1]);
			if (Math.abs(y - baseY) > 5 || !level.getBiome(pos).is(ModWorldgen.PAST_GORNOSLAVYANSK)) {
				return false;
			}
		}
		return true;
	}

	private static void placeFoundation(WorldGenLevel level, RandomSource random, int minX, int minZ,
			int width, int length, int groundY) {
		for (int x = 0; x < width; x++) {
			for (int z = 0; z < length; z++) {
				int localGround = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, minX + x, minZ + z) - 1;
				for (int y = Math.min(localGround, groundY); y <= groundY; y++) {
					level.setBlock(new BlockPos(minX + x, y, minZ + z), ruinBlock(random), 2);
				}
			}
		}
	}

	private static void placeStory(WorldGenLevel level, RandomSource random, int minX, int minZ,
			int width, int length, int story, int baseY, float decay) {
		for (int x = 0; x < width; x++) {
			for (int z = 0; z < length; z++) {
				boolean wall = x == 0 || x == width - 1 || z == 0 || z == length - 1;
				if (!wall) {
					continue;
				}
				for (int y = 0; y < STORY_HEIGHT; y++) {
					boolean entrance = story == 0 && z == 0 && x >= width / 2 - 1 && x <= width / 2 && y < 3;
					boolean window = y == 1 || y == 2;
					window &= (x == 0 || x == width - 1) ? z % 3 == 1 : x % 3 == 1;
					if (entrance || random.nextFloat() < decay) {
						continue;
					}
					BlockPos pos = new BlockPos(minX + x, baseY + y, minZ + z);
					if (window) {
						if (random.nextInt(4) == 0) {
							level.setBlock(pos, Blocks.IRON_BARS.defaultBlockState(), 2);
						}
					} else {
						level.setBlock(pos, ruinBlock(random), 2);
					}
				}
			}
		}

		if (story > 0) {
			for (int x = 1; x < width - 1; x++) {
				for (int z = 1; z < length - 1; z++) {
					if (random.nextFloat() > 0.22f + story * 0.055f) {
						level.setBlock(new BlockPos(minX + x, baseY, minZ + z), ruinBlock(random), 2);
					}
				}
			}
		}
	}

	private static void placeBrokenRoof(WorldGenLevel level, RandomSource random, int minX, int minZ,
			int width, int length, int y) {
		for (int x = 0; x < width; x++) {
			for (int z = 0; z < length; z++) {
				if ((x == 0 || x == width - 1 || z == 0 || z == length - 1) && random.nextFloat() < 0.48f) {
					level.setBlock(new BlockPos(minX + x, y, minZ + z), ruinBlock(random), 2);
				}
			}
		}
		for (int x : new int[]{0, width - 1}) {
			for (int z : new int[]{0, length - 1}) {
				int rebarHeight = 1 + random.nextInt(3);
				for (int dy = 1; dy <= rebarHeight; dy++) {
					level.setBlock(new BlockPos(minX + x, y + dy, minZ + z), Blocks.IRON_BARS.defaultBlockState(), 2);
				}
			}
		}
	}

	private static void placeExteriorDebris(WorldGenLevel level, RandomSource random, int minX, int minZ,
			int width, int length, int groundY) {
		int pieces = 16 + random.nextInt(17);
		for (int i = 0; i < pieces; i++) {
			int x = minX - 3 + random.nextInt(width + 6);
			int z = minZ - 3 + random.nextInt(length + 6);
			boolean outside = x < minX || x >= minX + width || z < minZ || z >= minZ + length;
			if (!outside) {
				continue;
			}
			int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
			BlockPos pos = new BlockPos(x, Math.max(y, groundY), z);
			if (level.getBiome(pos).is(ModWorldgen.PAST_GORNOSLAVYANSK)
					&& level.getBlockState(pos).isAir()) {
				level.setBlock(pos, random.nextInt(4) == 0
						? ModBlocks.RUINED_CONCRETE.defaultBlockState()
						: ModBlocks.BUILDING_DEBRIS.defaultBlockState(), 2);
			}
		}
	}

	private static BlockState ruinBlock(RandomSource random) {
		return random.nextInt(5) == 0
				? ModBlocks.BUILDING_DEBRIS.defaultBlockState()
				: ModBlocks.RUINED_CONCRETE.defaultBlockState();
	}
}

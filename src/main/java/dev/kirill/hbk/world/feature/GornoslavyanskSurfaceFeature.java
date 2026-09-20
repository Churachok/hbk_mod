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

/** Floods the old city with pale sludge, blood trails, rotten soil, and unnaturally green grass. */
public final class GornoslavyanskSurfaceFeature extends Feature<NoneFeatureConfiguration> {
	public GornoslavyanskSurfaceFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel level = context.level();
		RandomSource random = context.random();
		int minX = context.origin().getX() & ~15;
		int minZ = context.origin().getZ() & ~15;
		boolean changed = false;

		for (int x = minX; x < minX + 16; x++) {
			for (int z = minZ; z < minZ + 16; z++) {
				int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
				BlockPos top = new BlockPos(x, y, z);
				if (!level.getBiome(top).is(ModWorldgen.PAST_GORNOSLAVYANSK)
						|| !isNaturalSurface(level.getBlockState(top))) {
					continue;
				}

				BlockState surface = pickSurface(random);
				level.setBlock(top, surface, 2);
				changed = true;
				for (int depth = 1; depth <= 3; depth++) {
					BlockPos below = top.below(depth);
					BlockState belowState = level.getBlockState(below);
					if (isNaturalSurface(belowState)) {
						level.setBlock(below, depth >= 2 && random.nextInt(7) == 0
								? ModBlocks.RUINED_CONCRETE.defaultBlockState()
								: ModBlocks.ROTTEN_EARTH.defaultBlockState(), 2);
					}
				}

				if (surface.is(ModBlocks.BRIGHT_GRASS) && random.nextInt(3) == 0) {
					BlockPos above = top.above();
					if (level.getBlockState(above).isAir()) {
						level.setBlock(above, Blocks.SHORT_GRASS.defaultBlockState(), 2);
					}
				} else if (surface.is(ModBlocks.ROTTEN_EARTH) && random.nextInt(15) == 0) {
					BlockPos above = top.above();
					if (level.getBlockState(above).isAir()) {
						level.setBlock(above, ModBlocks.BUILDING_DEBRIS.defaultBlockState(), 2);
					}
				}
			}
		}

		if (random.nextInt(3) != 0) {
			paintBloodTrail(level, random, minX, minZ);
		}
		if (random.nextInt(5) == 0) {
			placeDebrisMound(level, random, minX + 3 + random.nextInt(10), minZ + 3 + random.nextInt(10));
		}
		return changed;
	}

	private static BlockState pickSurface(RandomSource random) {
		int roll = random.nextInt(100);
		if (roll < 48) {
			return ModBlocks.SPERM.defaultBlockState();
		}
		if (roll < 56) {
			return ModBlocks.BLOODY_SPERM.defaultBlockState();
		}
		if (roll < 73) {
			return ModBlocks.BRIGHT_GRASS.defaultBlockState();
		}
		if (roll < 96) {
			return ModBlocks.ROTTEN_EARTH.defaultBlockState();
		}
		return ModBlocks.BUILDING_DEBRIS.defaultBlockState();
	}

	private static boolean isNaturalSurface(BlockState state) {
		return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)
				|| state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM) || state.is(Blocks.SAND)
				|| state.is(Blocks.RED_SAND) || state.is(Blocks.STONE) || state.is(Blocks.GRAVEL)
				|| state.is(Blocks.CLAY) || state.is(Blocks.MUD);
	}

	private static void paintBloodTrail(WorldGenLevel level, RandomSource random, int minX, int minZ) {
		int x = minX + random.nextInt(16);
		int z = minZ + random.nextInt(16);
		int length = 5 + random.nextInt(9);
		for (int i = 0; i < length; i++) {
			int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
			BlockPos top = new BlockPos(x, y, z);
			if (level.getBiome(top).is(ModWorldgen.PAST_GORNOSLAVYANSK)
					&& (level.getBlockState(top).is(ModBlocks.SPERM)
					|| level.getBlockState(top).is(ModBlocks.ROTTEN_EARTH))) {
				level.setBlock(top, ModBlocks.BLOODY_SPERM.defaultBlockState(), 2);
			}
			x = Math.clamp(x + random.nextInt(3) - 1, minX, minX + 15);
			z = Math.clamp(z + random.nextInt(3) - 1, minZ, minZ + 15);
		}
	}

	private static void placeDebrisMound(WorldGenLevel level, RandomSource random, int centerX, int centerZ) {
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				if (dx * dx + dz * dz > 5 || random.nextInt(4) == 0) {
					continue;
				}
				int x = centerX + dx;
				int z = centerZ + dz;
				int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
				BlockPos pos = new BlockPos(x, y, z);
				if (level.getBiome(pos).is(ModWorldgen.PAST_GORNOSLAVYANSK)
						&& level.getBlockState(pos).isAir()) {
					level.setBlock(pos, random.nextInt(3) == 0
							? ModBlocks.RUINED_CONCRETE.defaultBlockState()
							: ModBlocks.BUILDING_DEBRIS.defaultBlockState(), 2);
				}
			}
		}
	}
}

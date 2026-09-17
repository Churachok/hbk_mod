package dev.kirill.hbk.world.feature;

import com.mojang.serialization.Codec;
import dev.kirill.hbk.registry.ModBlocks;
import dev.kirill.hbk.world.ModWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Replaces the vanilla surface and decorates each generated wasteland chunk. */
public final class WastelandSurfaceFeature extends Feature<NoneFeatureConfiguration> {
	public WastelandSurfaceFeature() {
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
				if (!level.getBiome(top).is(ModWorldgen.RADIOACTIVE_WASTELAND)) {
					continue;
				}

				BlockState state = level.getBlockState(top);
				if (isNaturalSurface(state)) {
					BlockState replacement = pickSurface(random);
					level.setBlock(top, replacement, 2);
					changed = true;
					for (int depth = 1; depth <= 3; depth++) {
						BlockPos below = top.below(depth);
						BlockState belowState = level.getBlockState(below);
						if (belowState.is(Blocks.STONE) || belowState.is(Blocks.DIRT)
								|| belowState.is(Blocks.SAND) || belowState.is(Blocks.GRAVEL)) {
							level.setBlock(below, depth == 1 && random.nextInt(4) == 0
									? ModBlocks.ASH_SOIL.defaultBlockState()
									: ModBlocks.RADIOACTIVE_STONE.defaultBlockState(), 2);
						}
					}
				}

				BlockPos above = top.above();
				if (level.getBlockState(above).isAir()) {
					placeDecoration(level, random, top, above);
				}
			}
		}

		if (random.nextInt(10) == 0) {
			placeToxicPool(level, random, minX + 4 + random.nextInt(8), minZ + 4 + random.nextInt(8));
		}
		if (random.nextInt(7) == 0) {
			placeDryTree(level, random, minX + 3 + random.nextInt(10), minZ + 3 + random.nextInt(10));
		}
		return changed;
	}

	private static boolean isNaturalSurface(BlockState state) {
		return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)
				|| state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM) || state.is(Blocks.SAND)
				|| state.is(Blocks.RED_SAND) || state.is(Blocks.STONE) || state.is(Blocks.GRAVEL)
				|| state.is(Blocks.CLAY) || state.is(Blocks.MUD);
	}

	private static BlockState pickSurface(RandomSource random) {
		return switch (random.nextInt(12)) {
			case 0, 1 -> ModBlocks.ASH_SOIL.defaultBlockState();
			case 2 -> ModBlocks.SEDIMENT_MUD.defaultBlockState();
			case 3 -> ModBlocks.RADIOACTIVE_SAND.defaultBlockState();
			case 4 -> ModBlocks.SCORCHED_STONE.defaultBlockState();
			default -> ModBlocks.INFECTED_DIRT.defaultBlockState();
		};
	}

	private static void placeDecoration(WorldGenLevel level, RandomSource random, BlockPos ground, BlockPos above) {
		int roll = random.nextInt(100);
		BlockState plant = null;
		if (roll < 10) {
			plant = ModBlocks.DEAD_GRASS.defaultBlockState();
		} else if (roll < 13) {
			plant = ModBlocks.INFECTED_BUSH.defaultBlockState();
		} else if (roll == 13) {
			plant = ModBlocks.GLOWING_MUSHROOM.defaultBlockState();
		} else if (roll < 19) {
			plant = ModBlocks.MOLDY_MOSS.defaultBlockState();
		}
		if (plant != null && plant.canSurvive(level, above)) {
			level.setBlock(above, plant, 2);
		}
	}

	private static void placeToxicPool(WorldGenLevel level, RandomSource random, int centerX, int centerZ) {
		int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, centerX, centerZ) - 1;
		BlockPos center = new BlockPos(centerX, surfaceY, centerZ);
		if (!level.getBiome(center).is(ModWorldgen.RADIOACTIVE_WASTELAND)) {
			return;
		}
		int radius = 2 + random.nextInt(3);
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				double distance = dx * dx + dz * dz;
				if (distance > radius * radius + random.nextDouble() * 2.0) {
					continue;
				}
				int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, centerX + dx, centerZ + dz) - 1;
				BlockPos floor = new BlockPos(centerX + dx, Math.min(y, surfaceY), centerZ + dz);
				level.setBlock(floor, distance < (radius - 1) * (radius - 1)
						? ModBlocks.TOXIC_WATER.defaultBlockState()
						: ModBlocks.SEDIMENT_MUD.defaultBlockState(), 2);
				if (distance < (radius - 1) * (radius - 1)) {
					level.setBlock(floor.below(), ModBlocks.SEDIMENT_MUD.defaultBlockState(), 2);
				}
			}
		}
	}

	private static void placeDryTree(WorldGenLevel level, RandomSource random, int x, int z) {
		int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
		BlockPos base = new BlockPos(x, y, z);
		if (!level.getBiome(base).is(ModWorldgen.RADIOACTIVE_WASTELAND)
				|| !level.getBlockState(base).isAir()) {
			return;
		}
		int height = 4 + random.nextInt(4);
		for (int i = 0; i < height; i++) {
			level.setBlock(base.above(i), ModBlocks.DRY_LOG.defaultBlockState(), 2);
		}
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			if (random.nextBoolean()) {
				BlockPos branch = base.above(height - 2 + random.nextInt(2)).relative(direction);
				level.setBlock(branch, ModBlocks.DRY_LOG.defaultBlockState()
						.setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, direction.getAxis()), 2);
				if (random.nextBoolean()) {
					level.setBlock(branch.relative(direction), ModBlocks.DRY_LOG.defaultBlockState()
							.setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, direction.getAxis()), 2);
				}
			}
		}
	}
}

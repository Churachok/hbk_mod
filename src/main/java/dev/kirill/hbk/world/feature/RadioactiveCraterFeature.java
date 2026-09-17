package dev.kirill.hbk.world.feature;

import dev.kirill.hbk.registry.ModBlocks;
import dev.kirill.hbk.world.ModWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** A broad impact crater with a poisonous pool and exposed uranium at its core. */
public final class RadioactiveCraterFeature extends Feature<NoneFeatureConfiguration> {
	public RadioactiveCraterFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel level = context.level();
		RandomSource random = context.random();
		int centerX = context.origin().getX();
		int centerZ = context.origin().getZ();
		int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, centerX, centerZ) - 1;
		BlockPos center = new BlockPos(centerX, surfaceY, centerZ);
		if (!level.getBiome(center).is(ModWorldgen.RADIOACTIVE_WASTELAND)) {
			return false;
		}

		int radius = 11 + random.nextInt(5);
		int depth = 5 + random.nextInt(4);
		for (int dx = -radius - 2; dx <= radius + 2; dx++) {
			for (int dz = -radius - 2; dz <= radius + 2; dz++) {
				double distance = Math.sqrt(dx * dx + dz * dz);
				int x = centerX + dx;
				int z = centerZ + dz;
				int localSurface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
				if (distance <= radius) {
					double normalized = 1.0 - distance / radius;
					int bowlDepth = Math.max(1, (int) Math.round(depth * normalized * normalized));
					int floorY = Math.min(localSurface, surfaceY) - bowlDepth;
					for (int y = localSurface; y > floorY; y--) {
						level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
					}
					BlockPos floor = new BlockPos(x, floorY, z);
					BlockStateChoice choice = floorChoice(random, distance, radius);
					level.setBlock(floor, choice.state(), 2);
					if (distance < radius * 0.48 && floorY <= surfaceY - 3) {
						int waterTop = Math.min(surfaceY - 3, floorY + 2);
						for (int y = floorY + 1; y <= waterTop; y++) {
							level.setBlock(new BlockPos(x, y, z), ModBlocks.TOXIC_WATER.defaultBlockState(), 2);
						}
					}
				} else if (distance <= radius + 2.0 && random.nextFloat() < 0.72f) {
					BlockPos rim = new BlockPos(x, localSurface + (random.nextBoolean() ? 1 : 0), z);
					level.setBlock(rim, random.nextInt(4) == 0
							? ModBlocks.CRACKED_RADIOACTIVE_STONE.defaultBlockState()
							: ModBlocks.SCORCHED_STONE.defaultBlockState(), 2);
				}
			}
		}
		return true;
	}

	private static BlockStateChoice floorChoice(RandomSource random, double distance, int radius) {
		if (distance < radius * 0.38 && random.nextInt(7) == 0) {
			return new BlockStateChoice(ModBlocks.URANIUM_ORE.defaultBlockState());
		}
		if (random.nextInt(4) == 0) {
			return new BlockStateChoice(ModBlocks.CRACKED_RADIOACTIVE_STONE.defaultBlockState());
		}
		return new BlockStateChoice(ModBlocks.SCORCHED_STONE.defaultBlockState());
	}

	private record BlockStateChoice(net.minecraft.world.level.block.state.BlockState state) {
	}
}

package dev.kirill.hbk.world.feature;

import dev.kirill.hbk.registry.ModBlocks;
import dev.kirill.hbk.world.ModWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Builds the dark-red upside-down T markers visible around the ruined city. */
public final class GornoslavyanskMonumentFeature extends Feature<NoneFeatureConfiguration> {
	public GornoslavyanskMonumentFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel level = context.level();
		RandomSource random = context.random();
		int x = context.origin().getX();
		int z = context.origin().getZ();
		int groundY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
		BlockPos center = new BlockPos(x, groundY, z);
		if (!level.getBiome(center).is(ModWorldgen.PAST_GORNOSLAVYANSK)) {
			return false;
		}

		boolean alongX = random.nextBoolean();
		int arm = 3 + random.nextInt(3);
		int height = 8 + random.nextInt(6);
		BlockState crimson = ModBlocks.CRIMSON_MONUMENT.defaultBlockState();
		for (int offset = -arm; offset <= arm; offset++) {
			for (int thickness = 0; thickness < 2; thickness++) {
				BlockPos pos = alongX
						? new BlockPos(x + offset, groundY + 1, z + thickness)
						: new BlockPos(x + thickness, groundY + 1, z + offset);
				level.setBlock(pos, crimson, 2);
			}
		}
		for (int y = 2; y <= height; y++) {
			for (int dx = 0; dx < 2; dx++) {
				for (int dz = 0; dz < 2; dz++) {
					level.setBlock(new BlockPos(x + dx, groundY + y, z + dz), crimson, 2);
				}
			}
		}

		for (int dx = -2; dx <= 3; dx++) {
			for (int dz = -2; dz <= 3; dz++) {
				if (random.nextInt(4) != 0) {
					continue;
				}
				int localY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x + dx, z + dz) - 1;
				BlockPos stain = new BlockPos(x + dx, localY, z + dz);
				if (level.getBiome(stain).is(ModWorldgen.PAST_GORNOSLAVYANSK)) {
					level.setBlock(stain, ModBlocks.BLOODY_SPERM.defaultBlockState(), 2);
				}
			}
		}
		return true;
	}
}

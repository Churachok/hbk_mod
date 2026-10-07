package dev.kirill.hbk.world.structure;

import com.mojang.serialization.MapCodec;
import dev.kirill.hbk.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import java.util.Arrays;
import java.util.Optional;

/** One rigid neighbourhood on dry, gently sloping terrain, with a solid foundation. */
public final class GroveStreetStructure extends Structure {
	public static final MapCodec<GroveStreetStructure> CODEC = simpleCodec(GroveStreetStructure::new);
	public static final int WIDTH = 98;
	public static final int DEPTH = 94;
	private static final int MAX_RELIEF = 16;
	private static final int[] SAMPLE_X = {0, 16, 32, 48, 64, 80, 97};
	private static final int[] SAMPLE_Z = {0, 16, 32, 48, 64, 80, 93};
	private static final int[] SITE_OFFSETS = {0, -48, 48};

	public GroveStreetStructure(StructureSettings settings) { super(settings); }

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		// A river or biome boundary at the first site must not waste the entire placement region.
		for (int dx : SITE_OFFSETS) {
			for (int dz : SITE_OFFSETS) {
				var site = findSite(context, context.chunkPos().getMinBlockX() + dx,
						context.chunkPos().getMinBlockZ() + dz);
				if (site.isPresent()) return site;
			}
		}
		return Optional.empty();
	}

	private Optional<GenerationStub> findSite(GenerationContext context, int centerX, int centerZ) {
		int centerY = context.chunkGenerator().getBaseHeight(centerX, centerZ,
				Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
		var biome = context.biomeSource().getNoiseBiome(centerX >> 2, centerY >> 2,
				centerZ >> 2, context.randomState().sampler());
		if (!context.validBiome().test(biome)) return Optional.empty();
		int x = centerX - WIDTH / 2;
		int z = centerZ - DEPTH / 2;
		int[] heights = new int[SAMPLE_X.length * SAMPLE_Z.length];
		int index = 0;
		int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
		for (int dx : SAMPLE_X) {
			for (int dz : SAMPLE_Z) {
				int surface = context.chunkGenerator().getBaseHeight(x + dx, z + dz,
						Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
				int floor = context.chunkGenerator().getBaseHeight(x + dx, z + dz,
						Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
				if (surface != floor || surface <= context.chunkGenerator().getSeaLevel()) return Optional.empty();
				min = Math.min(min, surface);
				max = Math.max(max, surface);
				if (max - min > MAX_RELIEF) return Optional.empty();
				heights[index++] = surface;
			}
		}
		int y = foundationHeight(heights);
		if (y == Integer.MIN_VALUE || y + 40 >= context.heightAccessor().getMaxY()) return Optional.empty();
		BlockPos origin = new BlockPos(x, y, z);
		return Optional.of(new GenerationStub(origin.offset(WIDTH / 2, 0, DEPTH / 2),
				builder -> builder.addPiece(new GroveStreetPiece(context.structureTemplateManager(), origin,
						context.random().nextInt(GroveStreetPiece.CHEST_COUNT)))));
	}

	/** Reject mountains; the template's bottom layer replaces the median surface block. */
	public static int foundationHeight(int[] samples) {
		if (samples.length == 0) return Integer.MIN_VALUE;
		int[] sorted = samples.clone();
		Arrays.sort(sorted);
		return sorted[sorted.length - 1] - sorted[0] > MAX_RELIEF
				? Integer.MIN_VALUE : sorted[sorted.length / 2] - 1;
	}

	@Override
	public StructureType<?> type() { return ModStructures.GROVE_STREET; }
}

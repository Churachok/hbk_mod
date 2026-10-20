package dev.kirill.hbk.universe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.LongAdder;

/** A 4x4x4 world-aligned density lattice. No neighbour reads or mutable random sources. */
public final class UniverseChunkGenerator extends ChunkGenerator {
	public static final MapCodec<UniverseChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			BiomeSource.CODEC.fieldOf("biome_source").forGetter(UniverseChunkGenerator::getBiomeSource),
			UniverseProfile.CODEC.fieldOf("profile").forGetter(g -> g.profile)
	).apply(i, UniverseChunkGenerator::new));
	private final UniverseProfile profile;
	private final UniverseDensity density;
	private final UniverseEcology ecology;
	private final UniverseMines mines;
	private final LongAdder generated = new LongAdder(), nanos = new LongAdder();
	public UniverseChunkGenerator(BiomeSource biomes, UniverseProfile profile) {
		super(biomes); this.profile = profile; density = new UniverseDensity(profile); ecology = new UniverseEcology(profile); mines = new UniverseMines(profile);
	}
	@Override protected MapCodec<? extends ChunkGenerator> codec() { return CODEC; }
	public UniverseProfile profile() { return profile; }
	public long generatedChunks() { return generated.sum(); }
	public double averageMillis() { return generated.sum() == 0 ? 0 : nanos.sum() / 1_000_000.0 / generated.sum(); }
	@Override public int getGenDepth() { return UniverseDensity.HEIGHT; }
	@Override public int getMinY() { return UniverseDensity.MIN_Y; }
	@Override public int getSeaLevel() { return profile.hydrology() == 7 ? 212 : 82; }
	@Override public void applyCarvers(WorldGenRegion region, long seed, RandomState random, BiomeManager biomes, StructureManager structures, ChunkAccess chunk) {}
	@Override public void buildSurface(WorldGenRegion region, StructureManager structures, RandomState random, ChunkAccess chunk) { ecology.buildSurface(chunk); }
	@Override public void spawnOriginalMobs(WorldGenRegion region) {}
	@Override public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structures) { ecology.decorate(chunk, this); mines.decorate(level, chunk); }

	public UniverseMines mines() { return mines; }
	private double sample(double x, double y, double z) { return Math.min(density.sample(x, y, z), mines.caveDensity(x, y, z)); }

	/** Same lattice interpolation as fillFromNoise, also used by height/column queries. */
	public double interpolated(int x, int y, int z) {
		int bx = Math.floorDiv(x, 4) * 4, by = Math.floorDiv(y, 4) * 4, bz = Math.floorDiv(z, 4) * 4;
		double tx = Math.floorMod(x, 4) * .25, ty = Math.floorMod(y, 4) * .25, tz = Math.floorMod(z, 4) * .25;
		return UniverseNoise.lerp(UniverseNoise.lerp(UniverseNoise.lerp(sample(bx, by, bz), sample(bx + 4, by, bz), tx),
				UniverseNoise.lerp(sample(bx, by + 4, bz), sample(bx + 4, by + 4, bz), tx), ty),
				UniverseNoise.lerp(UniverseNoise.lerp(sample(bx, by, bz + 4), sample(bx + 4, by, bz + 4), tx),
				UniverseNoise.lerp(sample(bx, by + 4, bz + 4), sample(bx + 4, by + 4, bz + 4), tx), ty), tz);
	}
	private BlockState material(int x, int y, int z, double d, double support, List<UniverseMines.Site> sites) {
		BlockState mine = mines.material(sites, x, y, z);
		if (mine != null) return mine;
		if (d <= 0) return density.water(x, y, z, support) ? UniverseMaterials.NATURAL_WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
		BlockState deposit = ecology.deposit(x, y, z, d);
		if (deposit != null) return deposit;
		long vein = UniverseNoise.hash(profile.stream(7), Math.floorDiv(x, 3), Math.floorDiv(y, 3), Math.floorDiv(z, 3));
		int role = d < 3 ? 1 : d > 20 ? 2 : 0;
		if (Long.remainderUnsigned(vein, 101) < 3 + (profile.geology() == 4 ? 2 : 0)) role = 4;
		else if (Math.floorMod(y + (int) (UniverseNoise.noise(profile.stream(9), x / 40.0, 0, z / 40.0) * 8), 31) < 2 && d > 3) role = 3;
		int variant = (int) (UniverseNoise.hash(profile.stream(8), Math.floorDiv(x, 12), Math.floorDiv(y, 12), Math.floorDiv(z, 12)) & 3);
		return UniverseMaterials.state(profile.geometry(), role, variant);
	}
	@Override public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState random, StructureManager structures, ChunkAccess chunk) {
		long start = System.nanoTime();
		int x0 = chunk.getPos().getMinBlockX(), z0 = chunk.getPos().getMinBlockZ();
		List<UniverseMines.Site> sites = mines.sitesForChunk(x0, z0);
		// Extra two vertical layers allow bounded pools to test eight blocks of supporting rock.
		double[][][] lattice = new double[5][99][5];
		for (int x = 0; x < 5; x++) for (int j = 0; j < 99; j++) for (int z = 0; z < 5; z++)
			lattice[x][j][z] = sample(x0 + x * 4, getMinY() - 8 + j * 4, z0 + z * 4);
		var pos = new BlockPos.MutableBlockPos();
		var floor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
		var surface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
		for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
			int ix = x / 4, iz = z / 4;
			double tx = (x % 4) * .25, tz = (z % 4) * .25;
			double[] column = new double[99];
			for (int j = 0; j < 99; j++) column[j] = UniverseNoise.lerp(
					UniverseNoise.lerp(lattice[ix][j][iz], lattice[ix + 1][j][iz], tx),
					UniverseNoise.lerp(lattice[ix][j][iz + 1], lattice[ix + 1][j][iz + 1], tx), tz);
			for (int y = 0; y < getGenDepth(); y++) {
				int j = y / 4 + 2; double t = (y % 4) * .25;
				double d = UniverseNoise.lerp(column[j], column[j + 1], t);
				int supportY = density.waterSupportY(getMinY() + y) - getMinY() + 8;
				int supportIndex = Math.floorDiv(supportY, 4);
				double support = supportIndex >= 0 && supportIndex < 98
						? UniverseNoise.lerp(column[supportIndex], column[supportIndex + 1], Math.floorMod(supportY, 4) * .25) : -1;
				BlockState state = material(x0 + x, getMinY() + y, z0 + z, d, support, sites);
				// Preserve the mine's cave-air mask: ecology keeps these passages free of crops.
				if (state.is(Blocks.AIR)) continue;
				chunk.setBlockState(pos.set(x0 + x, getMinY() + y, z0 + z), state);
				floor.update(x, getMinY() + y, z, state); surface.update(x, getMinY() + y, z, state);
			}
		}
		generated.increment(); nanos.add(System.nanoTime() - start);
		return CompletableFuture.completedFuture(chunk);
	}
	@Override public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
		return baseColumn(x, z);
	}
	/** A world-aligned column without decoration; also lets cross-border trees query pristine terrain. */
	public NoiseColumn baseColumn(int x, int z) {
		BlockState[] states = new BlockState[getGenDepth()];
		List<UniverseMines.Site> sites = mines.sitesForChunk(Math.floorDiv(x, 16) * 16, Math.floorDiv(z, 16) * 16);
		int bx = Math.floorDiv(x, 4) * 4, bz = Math.floorDiv(z, 4) * 4;
		double tx = Math.floorMod(x, 4) * .25, tz = Math.floorMod(z, 4) * .25;
		double[] column = new double[99];
		for (int j = 0; j < column.length; j++) {
			int y = getMinY() - 8 + j * 4;
			column[j] = UniverseNoise.lerp(UniverseNoise.lerp(sample(bx, y, bz), sample(bx + 4, y, bz), tx),
					UniverseNoise.lerp(sample(bx, y, bz + 4), sample(bx + 4, y, bz + 4), tx), tz);
		}
		for (int j = 0; j < states.length; j++) {
			int y = getMinY() + j;
			int supportY = density.waterSupportY(y) - getMinY() + 8, index = Math.floorDiv(supportY, 4);
			double support = index >= 0 && index < 98 ? UniverseNoise.lerp(column[index], column[index + 1], Math.floorMod(supportY, 4) * .25) : -1;
			states[j] = material(x, y, z, UniverseNoise.lerp(column[j / 4 + 2], column[j / 4 + 3], j % 4 * .25), support, sites);
		}
		return new NoiseColumn(getMinY(), states);
	}
	@Override public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
		var column = getBaseColumn(x, z, level, random);
		for (int y = getMinY() + getGenDepth() - 1; y >= getMinY(); y--) if (type.isOpaque().test(column.getBlock(y))) return y + 1;
		return getMinY();
	}
	@Override public void addDebugScreenInfo(List<String> lines, RandomState random, BlockPos pos) {
		lines.add("HBK " + profile.code() + " v" + profile.version() + " chunks=" + generatedChunks() + " density ms=" + averageMillis());
	}
}

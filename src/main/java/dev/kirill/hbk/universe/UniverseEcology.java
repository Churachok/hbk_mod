package dev.kirill.hbk.universe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BambooLeaves;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static dev.kirill.hbk.universe.UniverseNoise.*;

/** Original family flora and timber. Only writes the chunk being generated. */
public final class UniverseEcology {
	public record Habitat(String name, Block log, Block leaves, Block soil, Block accent, Block focusOre, int spacing) {}
	public static final List<Habitat> HABITATS = List.of(
			new Habitat("meadow", UniverseBotany.block(0, "log"), UniverseBotany.block(0, "leaves"), UniverseBotany.block(0, "soil"), Blocks.ANDESITE, Blocks.IRON_ORE, 10),
			new Habitat("sky_garden", UniverseBotany.block(1, "log"), UniverseBotany.block(1, "leaves"), UniverseBotany.block(1, "soil"), Blocks.CALCITE, Blocks.COPPER_ORE, 11),
			new Habitat("spire_taiga", UniverseBotany.block(2, "log"), UniverseBotany.block(2, "leaves"), UniverseBotany.block(2, "soil"), Blocks.TUFF, Blocks.EMERALD_ORE, 10),
			new Habitat("lantern_grotto", UniverseBotany.block(3, "log"), UniverseBotany.block(3, "leaves"), UniverseBotany.block(3, "soil"), Blocks.CALCITE, Blocks.REDSTONE_ORE, 11),
			new Habitat("braided_jungle", UniverseBotany.block(4, "log"), UniverseBotany.block(4, "leaves"), UniverseBotany.block(4, "soil"), Blocks.GRANITE, Blocks.GOLD_ORE, 10),
			new Habitat("basin_marsh", UniverseBotany.block(5, "log"), UniverseBotany.block(5, "leaves"), UniverseBotany.block(5, "soil"), Blocks.CLAY, Blocks.REDSTONE_ORE, 12),
			new Habitat("maze_forest", UniverseBotany.block(6, "log"), UniverseBotany.block(6, "leaves"), UniverseBotany.block(6, "soil"), Blocks.MOSSY_COBBLESTONE, Blocks.COAL_ORE, 11),
			new Habitat("shard_birches", UniverseBotany.block(7, "log"), UniverseBotany.block(7, "leaves"), UniverseBotany.block(7, "soil"), Blocks.DIORITE, Blocks.LAPIS_ORE, 9),
			new Habitat("hanging_garden", UniverseBotany.block(8, "log"), UniverseBotany.block(8, "leaves"), UniverseBotany.block(8, "soil"), Blocks.CALCITE, Blocks.DIAMOND_ORE, 10),
			new Habitat("terrace_savanna", UniverseBotany.block(9, "log"), UniverseBotany.block(9, "leaves"), UniverseBotany.block(9, "soil"), Blocks.RED_SANDSTONE, Blocks.COPPER_ORE, 12));
	private static final int MIN = UniverseDensity.MIN_Y, MAX = MIN + UniverseDensity.HEIGHT - 1;
	// Includes off-centre branches and their crowns. Anchors outside this margin cannot touch a chunk.
	private static final int TREE_REACH = 6;
	private final UniverseProfile profile;
	private final Habitat habitat;
	private final UniverseDensity terrain;
	private final long oreSeed, surfaceSeed, plantSeed, treeSeed;
	public UniverseEcology(UniverseProfile profile) {
		this.profile = profile; terrain = new UniverseDensity(profile); habitat = HABITATS.get(profile.geometry());
		oreSeed = profile.stream(41); surfaceSeed = profile.stream(42); plantSeed = profile.stream(43); treeSeed = profile.stream(44);
	}
	public Habitat habitat() { return habitat; }

	/** Veins work at any altitude: depth inside a floating island matters more than world Y. */
	public BlockState deposit(int x, int y, int z, double depth) {
		if (depth < 2) return null;
		// Native ores sit inside connected stone lenses with a weathered transition at their edge.
		// They no longer bring isolated grey squares into every family rock face.
		double host = noise(oreSeed + 2, x / 18.0, y / 12.0, z / 18.0);
		if (host < .32) return null;
		if (host < .44) return UniverseMaterials.state(profile.geometry(), 1, (int) (hash(surfaceSeed, x >> 3, y >> 3, z >> 3) & 3));
		long vein = hash(oreSeed, Math.floorDiv(x, 4), Math.floorDiv(y, 3), Math.floorDiv(z, 4));
		int roll = (int) Long.remainderUnsigned(vein, 1000);
		// Trim the edges of the cells so deposits are broken, uneven veins rather than solid boxes.
		boolean trimmed = (hash(oreSeed + 1, x, y, z) & 7) == 0;
		Block ore = roll < 19 ? Blocks.COAL_ORE : roll < 35 ? Blocks.IRON_ORE : roll < 46 ? Blocks.COPPER_ORE
				: roll < 55 && depth > 5 ? habitat.focusOre() : roll < 58 && depth > 10 ? Blocks.GOLD_ORE
				: roll == 58 && depth > 14 ? Blocks.DIAMOND_ORE : roll == 59 && depth > 8 ? Blocks.LAPIS_ORE : null;
		if (ore != null && !trimmed) {
			if (depth > 22) {
				if (ore == Blocks.COAL_ORE) ore = Blocks.DEEPSLATE_COAL_ORE;
				else if (ore == Blocks.IRON_ORE) ore = Blocks.DEEPSLATE_IRON_ORE;
				else if (ore == Blocks.COPPER_ORE) ore = Blocks.DEEPSLATE_COPPER_ORE;
				else if (ore == Blocks.GOLD_ORE) ore = Blocks.DEEPSLATE_GOLD_ORE;
				else if (ore == Blocks.REDSTONE_ORE) ore = Blocks.DEEPSLATE_REDSTONE_ORE;
				else if (ore == Blocks.LAPIS_ORE) ore = Blocks.DEEPSLATE_LAPIS_ORE;
				else if (ore == Blocks.EMERALD_ORE) ore = Blocks.DEEPSLATE_EMERALD_ORE;
				else if (ore == Blocks.DIAMOND_ORE) ore = Blocks.DEEPSLATE_DIAMOND_ORE;
			}
			return ore.defaultBlockState();
		}
		return (depth > 22 ? Blocks.DEEPSLATE : Blocks.STONE).defaultBlockState();
	}
	private static boolean rock(BlockState state) { return state.getBlock() instanceof UniverseRockBlock; }
	private static boolean soil(BlockState state) { return state.is(BlockTags.SUPPORTS_VEGETATION); }
	private BlockState topsoil(int x, int y, int z) { return habitat.soil().defaultBlockState(); }
	/** Every exposed shelf is fertile, including internal floors of stacked/closed worlds. */
	public void buildSurface(ChunkAccess chunk) {
		int x0 = chunk.getPos().getMinBlockX(), z0 = chunk.getPos().getMinBlockZ();
		var pos = new BlockPos.MutableBlockPos();
		for (int x = x0; x < x0 + 16; x++) for (int z = z0; z < z0 + 16; z++) for (int y = MAX - 3; y > MIN + 3; y--) {
			BlockState ground = chunk.getBlockState(pos.set(x, y, z));
			if (!rock(ground) && !ground.is(habitat.accent())) continue;
			BlockState above = chunk.getBlockState(pos.set(x, y + 1, z));
			if (above.isAir() && chunk.getBlockState(pos.set(x, y + 2, z)).isAir() && profile.geometry() != 8) {
				// Buried cave floors keep their mineral host rock instead of growing a forest underground.
				if (terrain.sample(x, y + 5, z) > 3 && terrain.sample(x, y + 9, z) > 3
						&& (chunk.getBlockState(pos.set(x, y + 6, z)).isSolidRender()
						|| chunk.getBlockState(pos.set(x, y + 12, z)).isSolidRender()
						|| chunk.getBlockState(pos.set(x, y + 20, z)).isSolidRender())) continue;
				// A thin local soil grades through matching loam into the weathered family rock.
				if (ground.is(UniverseMaterials.block(profile.geometry(), 4)) || ground.is(UniverseMaterials.block(profile.geometry(), 3))) continue;
				chunk.setBlockState(pos.set(x, y, z), topsoil(x, y, z));
				int thickness = 1 + (int) (hash(surfaceSeed, Math.floorDiv(x, 5), y >> 3, Math.floorDiv(z, 5)) & 1);
				for (int d = 1; d <= thickness + 1; d++) {
					var below = chunk.getBlockState(pos.set(x, y - d, z));
					if (rock(below) && !below.is(UniverseMaterials.block(profile.geometry(), 4)) && !below.is(UniverseMaterials.block(profile.geometry(), 3)))
						chunk.setBlockState(pos, d <= thickness ? UniverseMaterials.loam(profile.geometry()).defaultBlockState()
								: UniverseMaterials.state(profile.geometry(), 1, (int) (hash(surfaceSeed, x >> 3, y >> 3, z >> 3) & 3)));
				}
			} else if (!above.getFluidState().isEmpty() && (hash(surfaceSeed, x, y, z) & 3) != 0) {
				if (ground.is(UniverseMaterials.block(profile.geometry(), 4)) || ground.is(UniverseMaterials.block(profile.geometry(), 3))) continue;
				chunk.setBlockState(pos.set(x, y, z), ((hash(surfaceSeed + 1, x / 4, y / 4, z / 4) & 3) == 0 ? Blocks.CLAY : Blocks.SAND).defaultBlockState());
			} else if (profile.geometry() == 8 && chunk.getBlockState(pos.set(x, y - 1, z)).isAir()) {
				if (ground.is(UniverseMaterials.block(8, 0)) || ground.is(UniverseMaterials.block(8, 1)))
					chunk.setBlockState(pos.set(x, y, z), habitat.soil().defaultBlockState());
			}
		}
	}

	public void decorate(ChunkAccess chunk, UniverseChunkGenerator generator) {
		int x0 = chunk.getPos().getMinBlockX(), z0 = chunk.getPos().getMinBlockZ(), spacing = habitat.spacing();
		// Reconstruct complete trees from global anchors, then slice them into this chunk. No neighbour
		// decoration reads, no writes into unloaded chunks, and no crowns cut at chunk boundaries.
		for (int gx = Math.floorDiv(x0 - TREE_REACH, spacing); gx <= Math.floorDiv(x0 + 15 + TREE_REACH, spacing); gx++)
			for (int gz = Math.floorDiv(z0 - TREE_REACH, spacing); gz <= Math.floorDiv(z0 + 15 + TREE_REACH, spacing); gz++) {
				long seed = hash(treeSeed, gx, 0, gz);
				int x = gx * spacing + 2 + (int) Long.remainderUnsigned(seed, spacing - 4);
				int z = gz * spacing + 2 + (int) Long.remainderUnsigned(mix(seed), spacing - 4);
				double forest = noise(treeSeed + 1, x / 85.0, 0, z / 85.0);
				if (unit(seed) < (forest < -.25 ? .30 : .84)) {
					NoiseColumn column = generator.baseColumn(x, z);
					int direction = profile.geometry() == 8 ? -1 : 1;
					for (int y = MIN + 16; y < MAX - 16; y++) {
						if (!column.getBlock(y).isAir() || column.getBlock(y).is(Blocks.CAVE_AIR)
								|| !column.getBlock(y - direction).isSolidRender()
								|| UniverseBotany.family(column.getBlock(y - direction).getBlock()) >= 0) continue;
						int height = treeHeight(seed ^ y);
						boolean clear = true;
						for (int h = 1; h <= height + 3; h++) if (!column.getBlock(y + direction * h).isAir()) { clear = false; break; }
						if (clear) placeTree(chunk, tree(x, y, z, direction, seed ^ y), new BlockPos(x, y - direction, z));
					}
				}
			}
		vegetation(chunk);
	}
	private int treeHeight(long seed) {
		int variation = (int) Long.remainderUnsigned(mix(seed), 3);
		return (switch (profile.geometry()) { case 2 -> 9; case 3 -> 4; case 4 -> 11; case 7 -> 7; case 8 -> 7; default -> 6; }) + variation;
	}
	/** Public for deterministic generation/loot tests; not a separate structure or saved registry entry. */
	public Map<BlockPos, BlockState> tree(int x, int y, int z, int direction, long seed) {
		Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
		int height = treeHeight(seed), family = profile.geometry();
		for (int h = 0; h < height; h++) log(blocks, x, y + h * direction, z, Direction.Axis.Y);
		if (family == 2) {
			for (int h = 3; h <= height; h++) {
				int radius = Math.min(3, (height - h + 2) / 2);
				crown(blocks, x, y + h * direction, z, radius, 1, seed);
			}
		} else if (family == 1 || family == 4 || family == 6 || family == 9) {
			int dx = (seed & 1) == 0 ? 1 : -1;
			for (int n = 1; n <= 2; n++) log(blocks, x + n * dx, y + (height - 2) * direction, z, Direction.Axis.X);
			crown(blocks, x + 2 * dx, y + (height - 1) * direction, z, 3, family == 9 ? 1 : 2, seed);
			for (int n = 1; n <= 2; n++) log(blocks, x - n * dx, y + (height - 4) * direction, z, Direction.Axis.X);
			crown(blocks, x - 2 * dx, y + (height - 3) * direction, z, 2, family == 9 ? 1 : 2, seed + 1);
			crown(blocks, x, y + (height - 1) * direction, z, 2, 2, seed + 2);
			if (family == 6) for (int h = 0; h < height - 1; h++) log(blocks, x, y + h * direction, z + 1, Direction.Axis.Y);
			if (family == 4 || family == 6) hangingVines(blocks, seed);
		} else {
			crown(blocks, x, y + (height - 1) * direction, z, family == 7 ? 2 : 3, family == 7 ? 3 : 2, seed);
			if (family == 3 || family == 8) hangingVines(blocks, seed);
		}
		if (family == 5) for (int[] offset : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
			for (int h = 0; h < 3; h++) blocks.put(new BlockPos(x + offset[0], y + h * direction, z + offset[1]), habitat.log().defaultBlockState());
		}
		// Native decay, custom sapling/fruit loot: preserve real support distances.
		List<BlockPos> logs = blocks.entrySet().stream().filter(e -> e.getValue().is(BlockTags.LOGS)).map(Map.Entry::getKey).toList();
		blocks.replaceAll((pos, state) -> state.getBlock() instanceof LeavesBlock
				? state.setValue(LeavesBlock.DISTANCE, Math.min(7, logs.stream().mapToInt(pos::distManhattan).min().orElse(7))).setValue(LeavesBlock.PERSISTENT, false) : state);
		blocks.entrySet().removeIf(e -> e.getValue().hasProperty(LeavesBlock.DISTANCE) && e.getValue().getValue(LeavesBlock.DISTANCE) > 6);
		// Pruning an unsupported edge leaf must also prune its hanging crop chain.
		boolean removed;
		do { removed = blocks.entrySet().removeIf(e -> e.getValue().getBlock() instanceof UniverseCropBlock && !blocks.containsKey(e.getKey().above())); } while (removed);
		return blocks;
	}
	private void log(Map<BlockPos, BlockState> blocks, int x, int y, int z, Direction.Axis axis) {
		blocks.put(new BlockPos(x, y, z), habitat.log().defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis));
	}
	private void crown(Map<BlockPos, BlockState> blocks, int x, int y, int z, int radius, int vertical, long seed) {
		for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) for (int dy = -vertical; dy <= vertical; dy++) {
			if ((dx * dx + dz * dz) / (double) (radius * radius + 1) + dy * dy / (double) (vertical * vertical + 1) > 1.3) continue;
			if ((hash(seed, dx, dy, dz) & 15) == 0) continue;
			Block leaves = habitat.leaves();
			blocks.putIfAbsent(new BlockPos(x + dx, y + dy, z + dz), leaves.defaultBlockState());
		}
	}
	private void hangingVines(Map<BlockPos, BlockState> blocks, long seed) {
		for (var pos : new ArrayList<>(blocks.keySet())) {
			if (!(blocks.get(pos).getBlock() instanceof LeavesBlock) || blocks.containsKey(pos.below()) || (hash(seed, pos.getX(), pos.getY(), pos.getZ()) & 7) != 0) continue;
			int length = 1 + (int) (hash(seed + 1, pos.getX(), pos.getY(), pos.getZ()) & 3);
			for (int n = 1; n <= length && !blocks.containsKey(pos.below(n)); n++) blocks.put(pos.below(n),
					UniverseBotany.block(profile.geometry(), "plant").defaultBlockState().setValue(UniverseCropBlock.HANGING, true).setValue(UniverseCropBlock.AGE, 3));
		}
	}
	private void placeTree(ChunkAccess chunk, Map<BlockPos, BlockState> tree, BlockPos root) {
		for (var entry : tree.entrySet()) {
			BlockPos pos = entry.getKey();
			if (!inside(chunk, pos) || pos.getY() <= MIN || pos.getY() >= MAX) continue;
			BlockState existing = chunk.getBlockState(pos);
			if (existing.isAir() && !existing.is(Blocks.CAVE_AIR) || existing.is(BlockTags.LEAVES)) chunk.setBlockState(pos, entry.getValue());
		}
		// Family saplings regrow on their soil pocket, including beneath ceilings.
		if (inside(chunk, root) && (rock(chunk.getBlockState(root)) || soil(chunk.getBlockState(root)) || chunk.getBlockState(root).is(habitat.accent())))
			chunk.setBlockState(root, habitat.soil().defaultBlockState());
	}
	private static boolean inside(ChunkAccess chunk, BlockPos pos) {
		return Math.floorDiv(pos.getX(), 16) == chunk.getPos().x() && Math.floorDiv(pos.getZ(), 16) == chunk.getPos().z();
	}
	private void vegetation(ChunkAccess chunk) {
		var pos = new BlockPos.MutableBlockPos();
		int family = profile.geometry(), x0 = chunk.getPos().getMinBlockX(), z0 = chunk.getPos().getMinBlockZ();
		for (int x = x0; x < x0 + 16; x++) for (int z = z0; z < z0 + 16; z++) for (int y = MIN + 2; y < MAX - 2; y++) {
			BlockState ground = chunk.getBlockState(pos.set(x, y - 1, z)), here = chunk.getBlockState(pos.set(x, y, z));
			// Planned mine passages retain their headroom instead of sprouting ceiling crops.
			if (here.is(Blocks.CAVE_AIR)) continue;
			long seed = hash(plantSeed, x, y, z); int roll = (int) Long.remainderUnsigned(seed, 1000);
			if (here.isAir() && soil(ground)) {
				BlockState plant = null;
				if (roll < 140) plant = UniverseBotany.block(family, "plant").defaultBlockState().setValue(UniverseCropBlock.AGE, 3);
				else if (roll < 260) plant = UniverseBotany.block(family, "plant").defaultBlockState().setValue(UniverseCropBlock.AGE, 1);
				else if (roll < 310) plant = (family == 2 || family == 6 ? Blocks.FERN : Blocks.SHORT_GRASS).defaultBlockState();
				if (plant != null) chunk.setBlockState(pos, plant);
				if (family == 4 && roll >= 310 && roll < 360) {
					int height = 4 + (int) (seed & 3);
					for (int h = 0; h < height && chunk.getBlockState(pos.set(x, y + h, z)).isAir(); h++)
						chunk.setBlockState(pos, Blocks.BAMBOO.defaultBlockState().setValue(BambooStalkBlock.AGE, 1)
								.setValue(BambooStalkBlock.LEAVES, h >= height - 2 ? BambooLeaves.LARGE : BambooLeaves.NONE));
				}
			} else if (here.isAir() && (family == 3 || family == 8 || family == 9)) {
				BlockState roof = chunk.getBlockState(pos.set(x, y + 1, z));
				if ((rock(roof) || soil(roof)) && roll < 100) {
					int length = 2 + (int) (seed & 3);
					for (int n = 0; n < length && chunk.getBlockState(pos.set(x, y - n, z)).isAir()
							&& !chunk.getBlockState(pos).is(Blocks.CAVE_AIR); n++)
						chunk.setBlockState(pos, UniverseBotany.block(family, "plant").defaultBlockState().setValue(UniverseCropBlock.HANGING, true).setValue(UniverseCropBlock.AGE, 3));
				} else if (rock(roof) && roll < 160) chunk.setBlockState(pos.set(x, y, z), Blocks.GLOW_LICHEN.defaultBlockState().setValue(MultifaceBlock.getFaceProperty(Direction.UP), true));
			} else if (!here.getFluidState().isEmpty() && (ground.is(Blocks.SAND) || ground.is(Blocks.CLAY)) && roll < 160) {
				if (roll < 35) {
					int height = 3 + (int) (seed & 7), available = 0;
					while (available < height && y + available < MAX && !chunk.getBlockState(pos.set(x, y + available, z)).getFluidState().isEmpty()) available++;
					for (int h = 0; h < available; h++) chunk.setBlockState(pos.set(x, y + h, z), h == available - 1
							? Blocks.KELP.defaultBlockState().setValue(GrowingPlantHeadBlock.AGE, 25) : Blocks.KELP_PLANT.defaultBlockState());
				} else if (roll < 55) {
					chunk.setBlockState(pos.set(x, y, z), Blocks.SEA_PICKLE.defaultBlockState().setValue(SeaPickleBlock.PICKLES, 3));
				} else if (roll < 75 && profile.hydrology() == 7) {
					Block[] corals = {Blocks.TUBE_CORAL_BLOCK, Blocks.BRAIN_CORAL_BLOCK, Blocks.BUBBLE_CORAL_BLOCK, Blocks.FIRE_CORAL_BLOCK, Blocks.HORN_CORAL_BLOCK};
					chunk.setBlockState(pos.set(x, y, z), corals[family % corals.length].defaultBlockState());
				} else chunk.setBlockState(pos.set(x, y, z), Blocks.SEAGRASS.defaultBlockState());
			}
		}
	}
}

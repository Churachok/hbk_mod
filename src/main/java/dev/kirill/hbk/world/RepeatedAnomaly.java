package dev.kirill.hbk.world;

import dev.kirill.hbk.block.FracturedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.ticks.ScheduledTick;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/** One immutable, persisted natural chunk per zone; no neighbour generation or waiting on chunk futures. */
public final class RepeatedAnomaly {
	private static final int CACHE_LIMIT = 8;
	private static final ThreadLocal<Boolean> CAPTURING = ThreadLocal.withInitial(() -> false);
	private static final LinkedHashMap<Key, Template> CACHE = new LinkedHashMap<>(16, 0.75f, true);
	private record Key(ServerLevel level, int x, int z) {}
	private RepeatedAnomaly() {}

	public static boolean capturing() { return CAPTURING.get(); }
	public static synchronized void clear() { CACHE.clear(); CAPTURING.remove(); }

	/** Called at the head of decoration. Exactly one chunk runs native decoration to create the blueprint. */
	public static boolean decorate(ChunkGenerator generator, WorldGenLevel world, ChunkAccess chunk, StructureManager structures) {
		if (capturing() || !world.getLevel().dimension().equals(Level.OVERWORLD)) return false;
		var zone = AnomalyZones.forChunk(AnomalyZones.layoutSeed(world.getSeed()),
				chunk.getPos().getMinBlockX(), chunk.getPos().getMinBlockZ());
		if (zone == null || zone.kind() != 6) return false;
		var template = template(generator, world, chunk, structures, zone);
		template.apply(world, chunk);
		var pos = new BlockPos.MutableBlockPos();
		for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
			int wx = chunk.getPos().getMinBlockX() + x, wz = chunk.getPos().getMinBlockZ() + z;
			if (zone.border(wx, wz)) NewAnomalyTerrain.border(chunk, pos, zone, wx, wz, NewAnomalyTerrain.surface(chunk, wx, wz));
		}
		((AnomalyChunkMarker) chunk).hbk$markAnomalyDecorated();
		return true;
	}

	private static synchronized Template template(ChunkGenerator generator, WorldGenLevel world, ChunkAccess chunk,
			StructureManager structures, AnomalyZones.Zone zone) {
		var key = new Key(world.getLevel(), zone.minX(), zone.minZ());
		var template = CACHE.get(key);
		if (template != null) return template;
		Path directory = world.getLevel().getServer().getWorldPath(LevelResource.DATA).resolve("hbk_anomaly_templates");
		Path file = directory.resolve("repeat_" + zone.minX() + "_" + zone.minZ() + ".nbt");
		try {
			if (Files.exists(file)) {
				template = Template.read(world.getLevel(), NbtIo.readCompressed(file, NbtAccounter.create(8 * 1024 * 1024)));
				if (template.minY != chunk.getMinY() || template.height != chunk.getHeight()) {
					throw new IOException("Anomaly template height does not match this dimension: " + file.getFileName());
				}
			} else {
				CAPTURING.set(true);
				try {
					generator.applyBiomeDecoration(world, chunk, structures);
				} finally {
					CAPTURING.remove();
				}
				FracturedClusters.generate(world, chunk, zone, NewAnomalyTerrain.surface(chunk,
						chunk.getPos().getMinBlockX() + 8, chunk.getPos().getMinBlockZ() + 8));
				template = Template.capture(chunk);
				Files.createDirectories(directory);
				Path temporary = Files.createTempFile(directory, "repeat-", ".tmp");
				try {
					NbtIo.writeCompressed(template.write(), temporary);
					try {
						Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE);
					} catch (AtomicMoveNotSupportedException ignored) {
						Files.move(temporary, file);
					}
				} finally {
					Files.deleteIfExists(temporary);
				}
			}
		} catch (IOException exception) {
			// Never replace a saved blueprint silently: the remaining tiles must match explored ones.
			throw new UncheckedIOException("Cannot preserve repeated anomaly " + zone.minX() + ", " + zone.minZ(), exception);
		}
		CACHE.put(key, template);
		while (CACHE.size() > CACHE_LIMIT) CACHE.remove(CACHE.keySet().iterator().next());
		return template;
	}

	private record Template(int minY, int height, BlockState[] palette, char[] blocks,
			List<Holder<Biome>> biomes, char[] biomeIds) {
		static Template capture(ChunkAccess chunk) {
			var palette = new ArrayList<BlockState>();
			var indices = new HashMap<BlockState, Integer>();
			char[] blocks = new char[chunk.getHeight() * 256];
			var pos = new BlockPos.MutableBlockPos();
			int wx = chunk.getPos().getMinBlockX(), wz = chunk.getPos().getMinBlockZ();
			for (int y = chunk.getMinY(); y < chunk.getMaxY(); y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
				var state = chunk.getBlockState(pos.set(wx + x, y, wz + z));
				// Containers and block-entity data remain unique; native copies never duplicate inventories.
				state = state.hasBlockEntity() ? Blocks.AIR.defaultBlockState() : NewAnomalyTerrain.stable(state);
				Integer id = indices.get(state);
				if (id == null) { id = palette.size(); indices.put(state, id); palette.add(state); }
				blocks[AnomalyTileLayout.blockIndex(x, y, z, chunk.getMinY())] = (char) (int) id;
			}
			var biomes = new ArrayList<Holder<Biome>>();
			char[] biomeIds = new char[chunk.getHeight() * 4];
			for (int y = 0; y < chunk.getHeight() / 4; y++) for (int z = 0; z < 4; z++) for (int x = 0; x < 4; x++) {
				var biome = chunk.getNoiseBiome((wx >> 2) + x, (chunk.getMinY() >> 2) + y, (wz >> 2) + z);
				int id = biomes.indexOf(biome);
				if (id < 0) { id = biomes.size(); biomes.add(biome); }
				biomeIds[AnomalyTileLayout.biomeIndex(x, (chunk.getMinY() >> 2) + y, z,
						chunk.getMinY(), chunk.getHeight())] = (char) id;
			}
			return new Template(chunk.getMinY(), chunk.getHeight(), palette.toArray(BlockState[]::new), blocks, List.copyOf(biomes), biomeIds);
		}

		void apply(WorldGenLevel world, ChunkAccess chunk) {
			int wx = chunk.getPos().getMinBlockX(), wz = chunk.getPos().getMinBlockZ();
			var pos = new BlockPos.MutableBlockPos();
			var random = RandomSource.create(world.getSeed() ^ chunk.getPos().pack());
			for (int y = minY; y < minY + height; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
				var state = palette[blocks[AnomalyTileLayout.blockIndex(wx + x, y, wz + z, minY)]];
				pos.set(wx + x, y, wz + z);
				NewAnomalyTerrain.put(chunk, pos, state);
				if (state.getBlock() instanceof FracturedBlock) {
					chunk.getBlockTicks().schedule(new ScheduledTick<>(state.getBlock(), pos.immutable(),
							world.getGameTime() + FracturedBlock.duration(state.getValue(FracturedBlock.PHASE), random), 0));
				}
			}
			chunk.fillBiomesFromNoise((x, y, z, sampler) -> biomes.get(biomeIds[AnomalyTileLayout.biomeIndex(x, y, z, minY, height)]),
					world.getLevel().getChunkSource().randomState().sampler());
		}

		CompoundTag write() {
			var tag = new CompoundTag();
			tag.putInt("version", 1);
			tag.putInt("min_y", minY);
			tag.putInt("height", height);
			var states = new ListTag();
			for (var state : palette) states.add(NbtUtils.writeBlockState(state));
			tag.put("palette", states);
			tag.putIntArray("blocks", expand(blocks));
			var names = new ListTag();
			for (var biome : biomes) names.add(StringTag.valueOf(biome.unwrapKey().orElseThrow().identifier().toString()));
			tag.put("biomes", names);
			tag.putIntArray("biome_ids", expand(biomeIds));
			return tag;
		}

		static Template read(ServerLevel level, CompoundTag tag) throws IOException {
			if (tag.getIntOr("version", 0) != 1) throw new IOException("Unsupported repeated anomaly template version");
			int minY = tag.getIntOr("min_y", Integer.MIN_VALUE), height = tag.getIntOr("height", 0);
			if (height != level.getHeight() || minY != level.getMinY()) throw new IOException("Invalid template height");
			var states = tag.getListOrEmpty("palette");
			if (states.isEmpty() || states.size() > 65535) throw new IOException("Invalid template palette");
			var palette = new BlockState[states.size()];
			for (int i = 0; i < palette.length; i++) palette[i] = NbtUtils.readBlockState(level.registryAccess().lookupOrThrow(Registries.BLOCK), states.getCompoundOrEmpty(i));
			var names = tag.getListOrEmpty("biomes");
			if (names.isEmpty() || names.size() > 65535) throw new IOException("Invalid template biomes");
			var biomes = new ArrayList<Holder<Biome>>();
			for (int i = 0; i < names.size(); i++) biomes.add(level.registryAccess().lookupOrThrow(Registries.BIOME)
					.getOrThrow(ResourceKey.create(Registries.BIOME, Identifier.parse(names.getStringOr(i, "minecraft:plains")))));
			return new Template(minY, height, palette, compact(tag, "blocks", height * 256, palette.length),
					List.copyOf(biomes), compact(tag, "biome_ids", height * 4, biomes.size()));
		}

		private static int[] expand(char[] data) {
			int[] expanded = new int[data.length];
			for (int i = 0; i < data.length; i++) expanded[i] = data[i];
			return expanded;
		}

		private static char[] compact(CompoundTag tag, String name, int length, int paletteSize) throws IOException {
			int[] values = tag.getIntArray(name).orElseThrow(() -> new IOException("Missing template " + name));
			if (values.length != length) throw new IOException("Invalid template " + name + " length");
			char[] result = new char[length];
			for (int i = 0; i < length; i++) {
				if (values[i] < 0 || values[i] >= paletteSize) throw new IOException("Invalid template palette index");
				result[i] = (char) values[i];
			}
			return result;
		}
	}
}

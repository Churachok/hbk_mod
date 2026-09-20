package dev.kirill.hbk.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.kirill.hbk.HbkMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ModWorldData extends SavedData {
	private final Set<Long> raidedVillages = new HashSet<>();
	private final Set<Long> gulagCells = new HashSet<>();
	private final Set<Long> answeredStrangeChests = new HashSet<>();
	private final List<BlockPos> gulagPositions = new ArrayList<>();
	private boolean stalinkaHandled;
	private final List<BlockPos> stalinkaPositions = new ArrayList<>();
	private boolean kirillHouseHandled;
	private final List<BlockPos> kirillHousePositions = new ArrayList<>();
	private final Set<String> populatedStructures = new HashSet<>();
	private final Set<Long> npcChunks = new HashSet<>();

	public static final Codec<ModWorldData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.LONG.listOf().optionalFieldOf("raided_villages", List.of()).forGetter(data -> data.raidedVillages.stream().toList()),
			Codec.LONG.listOf().optionalFieldOf("gulag_cells", List.of()).forGetter(data -> data.gulagCells.stream().toList()),
			Codec.LONG.listOf().optionalFieldOf("answered_strange_chests", List.of()).forGetter(data -> data.answeredStrangeChests.stream().toList()),
			BlockPos.CODEC.listOf().optionalFieldOf("gulag_positions", List.of()).forGetter(data -> List.copyOf(data.gulagPositions)),
			Codec.BOOL.optionalFieldOf("stalinka_handled", false).forGetter(data -> data.stalinkaHandled),
			BlockPos.CODEC.listOf().optionalFieldOf("stalinka_positions", List.of()).forGetter(data -> List.copyOf(data.stalinkaPositions)),
			Codec.BOOL.optionalFieldOf("kirill_house_handled", false).forGetter(data -> data.kirillHouseHandled),
			BlockPos.CODEC.listOf().optionalFieldOf("kirill_house_positions", List.of()).forGetter(data -> List.copyOf(data.kirillHousePositions)),
			Codec.STRING.listOf().optionalFieldOf("populated_structures", List.of()).forGetter(data -> data.populatedStructures.stream().toList()),
			Codec.LONG.listOf().optionalFieldOf("npc_chunks", List.of()).forGetter(data -> data.npcChunks.stream().toList())
	).apply(instance, ModWorldData::fromCodec));

	public static final SavedDataType<ModWorldData> TYPE = new SavedDataType<>(
			HbkMod.id("world_data"),
			ModWorldData::new,
			CODEC,
			DataFixTypes.SAVED_DATA_RAIDS
	);

	public ModWorldData() {
	}

	private ModWorldData(Set<Long> raidedVillages, Set<Long> gulagCells, Set<Long> answeredStrangeChests, List<BlockPos> gulagPositions,
			boolean stalinkaHandled, List<BlockPos> stalinkaPositions, boolean kirillHouseHandled, List<BlockPos> kirillHousePositions,
			Set<String> populatedStructures, Set<Long> npcChunks) {
		this.raidedVillages.addAll(raidedVillages);
		this.gulagCells.addAll(gulagCells);
		this.answeredStrangeChests.addAll(answeredStrangeChests);
		this.gulagPositions.addAll(gulagPositions);
		this.stalinkaHandled = stalinkaHandled;
		this.stalinkaPositions.addAll(stalinkaPositions);
		this.kirillHouseHandled = kirillHouseHandled;
		this.kirillHousePositions.addAll(kirillHousePositions);
		this.populatedStructures.addAll(populatedStructures);
		this.npcChunks.addAll(npcChunks);
	}

	private static ModWorldData fromCodec(List<Long> raidedVillages, List<Long> gulagCells, List<Long> answeredStrangeChests, List<BlockPos> gulagPositions,
			boolean stalinkaHandled, List<BlockPos> stalinkaPositions, boolean kirillHouseHandled, List<BlockPos> kirillHousePositions,
			List<String> populatedStructures, List<Long> npcChunks) {
		return new ModWorldData(new HashSet<>(raidedVillages), new HashSet<>(gulagCells), new HashSet<>(answeredStrangeChests),
				gulagPositions, stalinkaHandled, stalinkaPositions, kirillHouseHandled, kirillHousePositions,
				new HashSet<>(populatedStructures), new HashSet<>(npcChunks));
	}

	public static ModWorldData get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(TYPE);
	}

	public static long gulagGridKey(int gridX, int gridZ) {
		return ((long) gridX << 32) | (gridZ & 0xFFFFFFFFL);
	}

	public static long villageKey(int minX, int minY, int minZ) {
		return minX * 734287L ^ minY * 912271L ^ minZ * 438289L;
	}

	public boolean isGulagHandled(long key) {
		return this.gulagCells.contains(key);
	}

	public void markGulagHandled(long key) {
		this.gulagCells.add(key);
		this.setDirty();
	}

	public void addGulagPosition(BlockPos pos) {
		this.gulagPositions.add(pos.immutable());
		this.setDirty();
	}

	public BlockPos findNearestGulag(BlockPos from) {
		BlockPos nearest = null;
		double best = Double.MAX_VALUE;
		for (BlockPos pos : this.gulagPositions) {
			double dist = pos.distSqr(from);
			if (dist < best) {
				best = dist;
				nearest = pos;
			}
		}
		return nearest;
	}

	public boolean isVillageRaided(long key) {
		return this.raidedVillages.contains(key);
	}

	public boolean isStrangeChestAnswered(long position) {
		return this.answeredStrangeChests.contains(position);
	}

	public void markStrangeChestAnswered(long position) {
		this.answeredStrangeChests.add(position);
		this.setDirty();
	}

	public void markVillageRaided(long key) {
		this.raidedVillages.add(key);
		this.setDirty();
	}

	public boolean isStalinkaHandled() {
		return this.stalinkaHandled;
	}

	public void markStalinkaHandled(BlockPos pos) {
		this.stalinkaHandled = true;
		this.stalinkaPositions.clear();
		this.stalinkaPositions.add(pos.immutable());
		this.setDirty();
	}

	public BlockPos getStalinkaPosition() {
		return this.stalinkaPositions.isEmpty() ? null : this.stalinkaPositions.getFirst();
	}

	public boolean isKirillHouseHandled() {
		return this.kirillHouseHandled;
	}

	public void markKirillHouseHandled(BlockPos pos) {
		this.kirillHouseHandled = true;
		this.kirillHousePositions.clear();
		this.kirillHousePositions.add(pos.immutable());
		this.setDirty();
	}

	public BlockPos getKirillHousePosition() {
		return this.kirillHousePositions.isEmpty() ? null : this.kirillHousePositions.getFirst();
	}

	public boolean isNpcChunkHandled(long key) {
		return this.npcChunks.contains(key);
	}

	public void markNpcChunkHandled(long key) {
		this.npcChunks.add(key);
		this.setDirty();
	}

	/** The saved unique-chunk set also preserves the cadence across restarts. */
	public boolean isSashaSpawnRollDue() {
		return !this.npcChunks.isEmpty() && this.npcChunks.size() % 20 == 0;
	}

	public boolean isStructurePopulated(String key) {
		return this.populatedStructures.contains(key);
	}

	public void markStructurePopulated(String key) {
		this.populatedStructures.add(key);
		this.setDirty();
	}
}

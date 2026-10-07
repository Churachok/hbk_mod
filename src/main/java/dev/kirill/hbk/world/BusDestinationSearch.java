package dev.kirill.hbk.world;

import dev.kirill.hbk.HbkMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Polls vanilla asynchronous chunk-generation futures; never blocks/join()s the server thread. */
final class BusDestinationSearch {
	final ServerLevel level;
	final ResourceKey<Level> departureDimension;
	final int startedAt;
	Vec3 result;
	boolean failed;
	final SovietBusEvent.Destination destination;
	private final BlockPos origin;
	private final Structure structure;
	private final List<ChunkPos> candidates = new ArrayList<>();
	private int candidateIndex;
	private int ring;
	private int ringIndex;
	private CompletableFuture<ChunkResult<ChunkAccess>> structureFuture;
	private final List<CompletableFuture<ChunkResult<ChunkAccess>>> landingFutures = new ArrayList<>();
	private BlockPos landing;

	BusDestinationSearch(ServerLevel level, ServerPlayer player, SovietBusEvent.Destination destination) {
		this.level = level;
		this.destination = destination;
		this.departureDimension = player.level().dimension();
		this.startedAt = level.getServer().getTickCount();
		this.origin = player.blockPosition();
		if (destination.structure == null) {
			this.structure = null;
			this.failed = !level.getChunkSource().getGenerator().getBiomeSource().possibleBiomes()
					.stream().anyMatch(biome -> biome.is(ModWorldgen.RADIOACTIVE_WASTELAND));
			return;
		}
		var holder = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(
				ResourceKey.create(Registries.STRUCTURE, HbkMod.id(destination.structure)));
		this.structure = holder.map(value -> value.value()).orElse(null);
		if (holder.isEmpty()) {
			this.failed = true;
			return;
		}
		var state = level.getChunkSource().getGeneratorState();
		for (var placement : state.getPlacementsForStructure(holder.get())) {
			if (!(placement instanceof RandomSpreadStructurePlacement spread)) {
				continue;
			}
			int cellX = Math.floorDiv(origin.getX() >> 4, spread.spacing());
			int cellZ = Math.floorDiv(origin.getZ() >> 4, spread.spacing());
			for (int x = -8; x <= 8; x++) {
				for (int z = -8; z <= 8; z++) {
					ChunkPos pos = spread.getPotentialStructureChunk(state.getLevelSeed(),
							(cellX + x) * spread.spacing(), (cellZ + z) * spread.spacing());
					if (level.getWorldBorder().isWithinBounds(pos)) {
						candidates.add(pos);
					}
				}
			}
		}
		// Only this trip's randomly chosen structure type enters the candidate list.
		// Within that type, keep the closest candidates first (never shuffle them).
		sortNearestFirst(candidates, origin);
		this.failed = candidates.isEmpty();
	}

	static void sortNearestFirst(List<ChunkPos> candidates, BlockPos origin) {
		candidates.sort(Comparator.comparingDouble(pos -> pos.getMiddleBlockPosition(64).distSqr(origin)));
	}

	void tick() {
		if (failed || result != null) {
			return;
		}
		if (landing != null) {
			if (landingFutures.stream().anyMatch(future -> !future.isDone())) {
				return;
			}
			if (landingFutures.stream().anyMatch(future -> future.isCompletedExceptionally()
					|| future.getNow(null).orElse(null) == null)) {
				failed = true;
				return;
			}
			result = safeLanding(level, landing, destination == SovietBusEvent.Destination.WASTELAND);
			landing = null;
			landingFutures.clear();
			return;
		}
		if (structure == null) {
			searchBiome();
			return;
		}
		if (structureFuture != null) {
			if (!structureFuture.isDone()) {
				return;
			}
			if (!structureFuture.isCompletedExceptionally()) {
				ChunkAccess chunk = structureFuture.getNow(null).orElse(null);
				var start = chunk == null ? null : chunk.getStartForStructure(structure);
				if (start != null && start.isValid()) {
					loadLanding(start.getBoundingBox().getCenter());
				}
			}
			structureFuture = null;
			return;
		}
		if (candidateIndex >= candidates.size()) {
			failed = true;
			return;
		}
		ChunkPos next = candidates.get(candidateIndex++);
		structureFuture = level.getChunkSource().getChunkFuture(next.x(), next.z(), ChunkStatus.STRUCTURE_STARTS, true);
	}

	private void searchBiome() {
		var source = level.getChunkSource().getGenerator().getBiomeSource();
		var sampler = level.getChunkSource().randomState().sampler();
		for (int sample = 0; sample < 96 && ring <= 240; sample++) {
			int[] offset = ringOffset(ring, ringIndex);
			int x = origin.getX() + offset[0] * 64;
			int z = origin.getZ() + offset[1] * 64;
			if (level.getWorldBorder().isWithinBounds(new BlockPos(x, 64, z))
					&& source.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(64),
							QuartPos.fromBlock(z), sampler).is(ModWorldgen.RADIOACTIVE_WASTELAND)) {
				advanceRing();
				loadLanding(new BlockPos(x, 64, z));
				return;
			}
			advanceRing();
		}
		failed = ring > 240;
	}

	static int[] ringOffset(int ring, int index) {
		if (ring == 0) return new int[]{0, 0};
		if (index < 2 * ring) return new int[]{-ring + index, -ring};
		if (index < 4 * ring) return new int[]{ring, -ring + index - 2 * ring};
		if (index < 6 * ring) return new int[]{ring - (index - 4 * ring), ring};
		return new int[]{-ring, ring - (index - 6 * ring)};
	}

	private void advanceRing() {
		if (++ringIndex >= (ring == 0 ? 1 : 8 * ring)) {
			ring++;
			ringIndex = 0;
		}
	}

	private void loadLanding(BlockPos position) {
		landing = position;
		for (int x = -1; x <= 1; x++) {
			for (int z = -1; z <= 1; z++) {
				landingFutures.add(level.getChunkSource().getChunkFuture((position.getX() >> 4) + x,
						(position.getZ() >> 4) + z, ChunkStatus.FULL, true));
			}
		}
	}

	static Vec3 safeLanding(ServerLevel level, BlockPos target, boolean requireWasteland) {
		for (int radius = 0; radius <= 8; radius++) {
			for (int i = 0; i < (radius == 0 ? 1 : radius * 8); i++) {
				int[] offset = ringOffset(radius, i);
				int x = target.getX() + offset[0];
				int z = target.getZ() + offset[1];
				int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
				BlockPos feet = new BlockPos(x, y, z);
				var floor = level.getBlockState(feet.below());
				Vec3 position = Vec3.atBottomCenterOf(feet);
				var box = net.minecraft.world.entity.EntityTypes.PLAYER.getDimensions().makeBoundingBox(position);
				if (y >= level.getMaxY() - 1 || y <= level.getMinY() || !level.getWorldBorder().isWithinBounds(box)
						|| floor.getCollisionShape(level, feet.below()).isEmpty()
						|| !level.getFluidState(feet.below()).isEmpty()
						|| floor.is(Blocks.MAGMA_BLOCK) || floor.is(Blocks.CACTUS)
						|| floor.is(Blocks.CAMPFIRE) || floor.is(Blocks.SOUL_CAMPFIRE)
						|| floor.is(Blocks.POWDER_SNOW) || !level.noCollision(null, box)
						|| !level.getFluidState(feet).isEmpty() || !level.getFluidState(feet.above()).isEmpty()
						|| requireWasteland && !level.getBiome(feet).is(ModWorldgen.RADIOACTIVE_WASTELAND)) {
					continue;
				}
				return position;
			}
		}
		return null;
	}
}

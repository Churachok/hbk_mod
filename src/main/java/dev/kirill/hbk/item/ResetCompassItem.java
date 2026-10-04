package dev.kirill.hbk.item;

import dev.kirill.hbk.world.ModWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/** A lodestone-style compass whose target is the nearest radioactive wasteland. */
public final class ResetCompassItem extends Item {
	private static final int SEARCH_RADIUS = 6400;
	private static final int SAMPLE_SPACING = 32;
	private static final int MAX_RING = SEARCH_RADIUS / SAMPLE_SPACING;
	private static final int SAMPLES_PER_TICK = 96;
	private static final int RECHECK_DISTANCE_SQUARED = 512 * 512;
	private static final long RECHECK_INTERVAL = 1200;
	private static final Map<Player, Search> SEARCHES = new WeakHashMap<>();

	public ResetCompassItem(Properties properties) {
		super(properties);
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
		if (!(entity instanceof Player player)) {
			return;
		}
		if (!level.dimension().equals(Level.OVERWORLD)) {
			SEARCHES.remove(player);
			if (stack.has(DataComponents.LODESTONE_TRACKER)) {
				stack.remove(DataComponents.LODESTONE_TRACKER);
			}
			return;
		}

		BlockPos position = player.blockPosition();
		Search search = SEARCHES.get(player);
		if (search == null || (!search.searching()
				&& position.distSqr(search.origin) >= RECHECK_DISTANCE_SQUARED
				&& level.getGameTime() - search.checkedAt >= RECHECK_INTERVAL)) {
			search = new Search(position, search == null ? null : search.target, level.getGameTime());
			SEARCHES.put(player, search);
		}
		if (search.searching() && search.lastStepTime != level.getGameTime()) {
			search.lastStepTime = level.getGameTime();
			advanceSearch(level, search);
		}

		Optional<GlobalPos> target = search.target == null ? Optional.empty()
				: Optional.of(GlobalPos.of(level.dimension(), search.target));
		LodestoneTracker tracker = new LodestoneTracker(target, false);
		if (!tracker.equals(stack.get(DataComponents.LODESTONE_TRACKER))) {
			stack.set(DataComponents.LODESTONE_TRACKER, tracker);
		}
	}

	private static void advanceSearch(ServerLevel level, Search search) {
		var biomes = level.getChunkSource().getGenerator().getBiomeSource();
		var sampler = level.getChunkSource().randomState().sampler();
		for (int sample = 0; sample < SAMPLES_PER_TICK && search.searching(); sample++) {
			int ring = search.ring;
			int index = search.index;
			int x;
			int z;
			if (ring == 0) {
				x = 0;
				z = 0;
			} else if (index < 2 * ring) {
				x = -ring + index;
				z = -ring;
			} else if (index < 4 * ring) {
				x = ring;
				z = -ring + index - 2 * ring;
			} else if (index < 6 * ring) {
				x = ring - (index - 4 * ring);
				z = ring;
			} else {
				x = -ring;
				z = ring - (index - 6 * ring);
			}

			long distance = (long) x * x + (long) z * z;
			if (distance <= (long) MAX_RING * MAX_RING) {
				int blockX = search.origin.getX() + x * SAMPLE_SPACING;
				int blockZ = search.origin.getZ() + z * SAMPLE_SPACING;
				if (biomes.getNoiseBiome(QuartPos.fromBlock(blockX), QuartPos.fromBlock(64),
						QuartPos.fromBlock(blockZ), sampler).is(ModWorldgen.RADIOACTIVE_WASTELAND)) {
					long blockDistance = distance * SAMPLE_SPACING * SAMPLE_SPACING;
					if (blockDistance < search.bestDistance) {
						search.bestDistance = blockDistance;
						search.bestTarget = new BlockPos(blockX, 64, blockZ);
					}
				}
			}
			search.index++;
			if (search.index >= (ring == 0 ? 1 : 8 * ring)) {
				long nextRingDistance = (long) (ring + 1) * SAMPLE_SPACING;
				if (search.bestTarget != null && nextRingDistance * nextRingDistance > search.bestDistance) {
					search.target = search.bestTarget;
					search.ring = MAX_RING + 1;
				} else if (ring == MAX_RING) {
					search.target = search.bestTarget;
					search.ring = MAX_RING + 1;
				} else {
					search.ring++;
					search.index = 0;
				}
			}
		}
	}

	private static final class Search {
		private final BlockPos origin;
		private final long checkedAt;
		private BlockPos target;
		private BlockPos bestTarget;
		private long bestDistance = Long.MAX_VALUE;
		private long lastStepTime = Long.MIN_VALUE;
		private int ring;
		private int index;

		private Search(BlockPos origin, BlockPos target, long checkedAt) {
			this.origin = origin;
			this.target = target;
			this.checkedAt = checkedAt;
		}

		private boolean searching() {
			return ring <= MAX_RING;
		}
	}
}

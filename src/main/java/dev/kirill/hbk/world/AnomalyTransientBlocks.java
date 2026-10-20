package dev.kirill.hbk.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.kirill.hbk.HbkMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;

/** Timed, persisted gaps in natural terrain. No drops on removal and no forced chunk loads. */
public final class AnomalyTransientBlocks extends SavedData {
	private static final int MAX_PENDING = 64;
	private static final int UPDATE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
	private static final TagKey<Block> UNSTABLE = TagKey.create(Registries.BLOCK, HbkMod.id("anomaly_unstable"));
	private static final Direction[] DIRECTIONS = Direction.values();
	private final List<Pending> pending = new ArrayList<>();

	private record Pending(BlockPos pos, BlockState state, long restoreAt) {
		private static final Codec<Pending> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				BlockPos.CODEC.fieldOf("pos").forGetter(Pending::pos),
				BlockState.CODEC.fieldOf("state").forGetter(Pending::state),
				Codec.LONG.fieldOf("restore_at").forGetter(Pending::restoreAt)
		).apply(instance, Pending::new));
	}

	private static final Codec<AnomalyTransientBlocks> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Pending.CODEC.listOf().optionalFieldOf("pending", List.of()).forGetter(data -> List.copyOf(data.pending))
	).apply(instance, AnomalyTransientBlocks::new));
	private static final SavedDataType<AnomalyTransientBlocks> TYPE = new SavedDataType<>(
			HbkMod.id("anomaly_transient_blocks"), AnomalyTransientBlocks::new, CODEC, DataFixTypes.SAVED_DATA_RAIDS);

	private AnomalyTransientBlocks() {}
	private AnomalyTransientBlocks(List<Pending> entries) { pending.addAll(entries); }

	public static void tick(ServerLevel level) {
		restore(level, false);
		if (level.getGameTime() % 10 != 0) return;
		for (var player : level.players()) {
			if (!player.isAlive() || player.isSpectator() || !AnomalyZones.contains(player) || level.getRandom().nextInt(3) != 0) continue;
			for (int attempt = 0; attempt < 48; attempt++) {
				var pos = player.blockPosition().offset(level.getRandom().nextInt(25) - 12,
						level.getRandom().nextInt(19) - 10, level.getRandom().nextInt(25) - 12);
				if (!level.isOutsideBuildHeight(pos.getY()) && level.hasChunkAt(pos) && exposed(level, pos) && vanish(level, pos)) break;
			}
		}
	}

	private static boolean exposed(ServerLevel level, BlockPos pos) {
		for (var direction : DIRECTIONS) {
			var neighbor = pos.relative(direction);
			if (level.hasChunkAt(neighbor) && level.getBlockState(neighbor).isAir()) return true;
		}
		return false;
	}

	static boolean vanish(ServerLevel level, BlockPos pos) {
		if (!level.hasChunkAt(pos) || !AnomalyZones.contains(level, pos)) return false;
		var state = level.getBlockState(pos);
		if (!state.is(UNSTABLE) || !state.isSolidRender() || state.hasBlockEntity() || !state.getFluidState().isEmpty()) return false;
		var data = level.getDataStorage().computeIfAbsent(TYPE);
		if (data.pending.size() >= MAX_PENDING || data.pending.stream().anyMatch(entry -> entry.pos.equals(pos))) return false;
		var entry = new Pending(pos.immutable(), state, level.getGameTime() + 40);
		data.pending.add(entry);
		data.setDirty();
		if (level.setBlock(pos, Blocks.AIR.defaultBlockState(), UPDATE_FLAGS)) return true;
		data.pending.remove(entry);
		return false;
	}

	public static void restore(ServerLevel level, boolean force) {
		var data = level.getDataStorage().computeIfAbsent(TYPE);
		var iterator = data.pending.iterator();
		while (iterator.hasNext()) {
			var entry = iterator.next();
			if ((!force && level.getGameTime() < entry.restoreAt) || !level.hasChunkAt(entry.pos)) continue;
			if (level.getBlockState(entry.pos).isAir()) {
				if (!level.setBlock(entry.pos, entry.state, UPDATE_FLAGS)) continue;
			} else {
				// A player may fill the gap: preserve their replacement and return the displaced material once.
				Block.popResource(level, entry.pos, new ItemStack(entry.state.getBlock()));
			}
			iterator.remove();
			data.setDirty();
		}
	}
}

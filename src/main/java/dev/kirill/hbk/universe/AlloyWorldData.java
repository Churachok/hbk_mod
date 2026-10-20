package dev.kirill.hbk.universe;

import com.mojang.serialization.Codec;
import dev.kirill.hbk.HbkMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import java.util.HashMap;
import java.util.Map;

/** Separate saved data: no migration of addresses, terrain profiles or existing journeys. */
public final class AlloyWorldData extends SavedData {
	private final Map<String, Long> positions = new HashMap<>();
	public static final Codec<AlloyWorldData> CODEC = Codec.unboundedMap(Codec.STRING, Codec.LONG)
			.xmap(AlloyWorldData::new, d -> Map.copyOf(d.positions));
	public static final SavedDataType<AlloyWorldData> TYPE = new SavedDataType<>(HbkMod.id("alloy_landmarks"), AlloyWorldData::new, CODEC, DataFixTypes.SAVED_DATA_RAIDS);
	public AlloyWorldData() {}
	private AlloyWorldData(Map<String, Long> values) { positions.putAll(values); }
	public static AlloyWorldData get(ServerLevel level) { return level.getDataStorage().computeIfAbsent(TYPE); }
	public boolean placedLog(BlockPos pos) { return positions.containsKey("log/" + pos.asLong()); }
	public void markLog(BlockPos pos) { positions.put("log/" + pos.asLong(), pos.asLong()); setDirty(); }
	public void forgetLog(BlockPos pos) { if (positions.remove("log/" + pos.asLong()) != null) setDirty(); }
	public void rememberEntrance(ServerPlayer player, BlockPos pos) { positions.put("return/" + player.getUUID(), pos.asLong()); setDirty(); }
	public BlockPos entrance(ServerPlayer player) {
		Long position = positions.get("return/" + player.getUUID());
		return position == null ? null : BlockPos.of(position);
	}
}

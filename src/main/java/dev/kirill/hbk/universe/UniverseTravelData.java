package dev.kirill.hbk.universe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.kirill.hbk.HbkMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;

/** Lives in the Overworld. Player UUID + full address are the keys; never reuse a slot. */
public final class UniverseTravelData extends SavedData {
	public record Point(String dimension, double x, double y, double z, float yaw, float pitch) {
		public static final Codec<Point> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.fieldOf("dimension").forGetter(Point::dimension), Codec.DOUBLE.fieldOf("x").forGetter(Point::x),
				Codec.DOUBLE.fieldOf("y").forGetter(Point::y), Codec.DOUBLE.fieldOf("z").forGetter(Point::z),
				Codec.FLOAT.fieldOf("yaw").forGetter(Point::yaw), Codec.FLOAT.fieldOf("pitch").forGetter(Point::pitch)
		).apply(i, Point::new));
		public static Point of(ServerPlayer player) {
			return new Point(player.level().dimension().identifier().toString(), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
		}
		public ResourceKey<Level> key() { return ResourceKey.create(Registries.DIMENSION, Identifier.parse(dimension)); }
	}
	public static final Codec<UniverseTravelData> CODEC = Codec.unboundedMap(Codec.STRING, Point.CODEC)
			.xmap(UniverseTravelData::new, data -> Map.copyOf(data.points));
	public static final SavedDataType<UniverseTravelData> TYPE = new SavedDataType<>(HbkMod.id("universe_journeys"),
			UniverseTravelData::new, CODEC, DataFixTypes.SAVED_DATA_RAIDS);
	private final Map<String, Point> points = new HashMap<>();
	public UniverseTravelData() {}
	private UniverseTravelData(Map<String, Point> points) { this.points.putAll(points); }
	public static UniverseTravelData get(ServerLevel level) { return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE); }
	public Point get(ServerPlayer player, String address) { return points.get(player.getUUID() + "/" + address); }
	public void put(ServerPlayer player, String address) { points.put(player.getUUID() + "/" + address, Point.of(player)); setDirty(); }
}

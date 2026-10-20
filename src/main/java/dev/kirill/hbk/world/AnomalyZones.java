package dev.kirill.hbk.world;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.network.ModNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Spatial overlays: the native biome, its colors, weather and spawn tables stay intact. */
public final class AnomalyZones {
	public static final int SIZE = 192;
	public static final int SPACING = 1536;
	public static final int TYPE_COUNT = 7;
	public static final int CEILING_Y = 304;
	public static final double GRAVITY = 0.25;
	public static final double CENTER_GRAVITY = 0.04;
	private static final ResourceKey<DamageType> CONTACT = ResourceKey.create(Registries.DAMAGE_TYPE, HbkMod.id("anomaly"));
	private static volatile Long clientSeed;

	public record Zone(int minX, int minZ, int kind) {
		public int size() { return kind == 5 ? 160 : kind == 6 ? 128 : kind == 7 ? 256 : SIZE; }
		public int centerX() { return minX + size() / 2; }
		public int centerZ() { return minZ + size() / 2; }
		public boolean circular() { return kind == 5; }
		public boolean contains(double x, double z) {
			if (x < minX || x >= minX + size() || z < minZ || z >= minZ + size()) return false;
			return !circular() || Math.hypot(x - centerX(), z - centerZ()) < size() / 2.0;
		}
		public double horizontalDistance(double x, double z) {
			if (circular()) return Math.max(0, Math.hypot(x - centerX(), z - centerZ()) - size() / 2.0);
			double dx = Math.max(0, Math.max(minX - x, x - minX - size()));
			double dz = Math.max(0, Math.max(minZ - z, z - minZ - size()));
			return Math.hypot(dx, dz);
		}
		public boolean intersects(double x0, double z0, double x1, double z1) {
			if (x1 <= minX || x0 >= minX + size() || z1 <= minZ || z0 >= minZ + size()) return false;
			if (!circular()) return true;
			double x = Math.clamp(centerX(), x0, x1), z = Math.clamp(centerZ(), z0, z1);
			return Math.hypot(x - centerX(), z - centerZ()) < size() / 2.0;
		}
		public boolean border(int x, int z) {
			if (!contains(x + 0.5, z + 0.5)) return false;
			if (circular()) return Math.hypot(x + 0.5 - centerX(), z + 0.5 - centerZ()) >= size() / 2.0 - 2;
			return x - minX < 2 || z - minZ < 2 || x - minX >= size() - 2 || z - minZ >= size() - 2;
		}
		public AABB ceiling() {
			return new AABB(minX, CEILING_Y - 0.125, minZ, minX + size(), CEILING_Y + 0.125, minZ + size());
		}
		public double intensity(double x, double z) {
			double distance = Math.min(1, Math.hypot(x - centerX(), z - centerZ()) / (size() / 2.0));
			return 1 - distance * distance * (3 - 2 * distance);
		}
		public double gravity(double x, double z) {
			return GRAVITY + (CENTER_GRAVITY - GRAVITY) * intensity(x, z);
		}
	}

	private AnomalyZones() {}

	public static long layoutSeed(long worldSeed) {
		return mix(worldSeed ^ 0x641A110A7L);
	}

	private static long mix(long value) {
		value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
		value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
		return value ^ (value >>> 31);
	}

	public static Zone inCell(long seed, int cellX, int cellZ) {
		long hash = mix(seed ^ ((long) cellX * 341873128712L) ^ ((long) cellZ * 132897987541L));
		int x = cellX * SPACING + 320 + (int) Math.floorMod(hash, 704);
		int z = cellZ * SPACING + 320 + (int) Math.floorMod(hash >>> 20, 704);
		return new Zone(x & ~15, z & ~15, 1 + (int) Math.floorMod(hash >>> 40, 3));
	}

	private static Zone craterInCell(long seed, int cellX, int cellZ) {
		// Keep the original three types and their coordinates stable in existing saves.
		// The reserved corner cannot intersect the original zone (which starts at offset 320 or more).
		long hash = mix(seed ^ 0xC4A7E4L ^ ((long) cellX * 341873128712L) ^ ((long) cellZ * 132897987541L));
		if (Math.floorMod(hash, 3) != 0) return null;
		int x = cellX * SPACING + 32 + 16 * (int) Math.floorMod(hash >>> 8, 5);
		int z = cellZ * SPACING + 32 + 16 * (int) Math.floorMod(hash >>> 24, 5);
		return new Zone(x, z, 4);
	}

	public static List<Zone> zonesInCell(long seed, int cellX, int cellZ) {
		Zone original = inCell(seed, cellX, cellZ);
		Zone crater = craterInCell(seed, cellX, cellZ);
		var zones = new ArrayList<Zone>(5);
		zones.add(original);
		if (crater != null) zones.add(crater);
		for (int kind = 5; kind <= TYPE_COUNT; kind++) {
			var zone = additionalInCell(seed, cellX, cellZ, kind);
			if (zone != null) zones.add(zone);
		}
		return List.copyOf(zones);
	}

	private static Zone additionalInCell(long seed, int cellX, int cellZ, int kind) {
		long hash = mix(seed ^ 0xA110000L * kind ^ (long) cellX * 341873128712L ^ (long) cellZ * 132897987541L);
		if (Math.floorMod(hash, 3) != 0) return null;
		// Reserved strips stay beyond every possible original zone and within this layout cell.
		int dx = 16 * (int) (hash >>> 12 & 3), dz = 16 * (int) (hash >>> 20 & 3);
		int x = kind == 7 ? 32 + dx : 1248 + dx;
		int z = kind == 5 ? 64 + dz : kind == 6 ? 512 + dz : 1232 + dz;
		return new Zone(cellX * SPACING + x, cellZ * SPACING + z, kind);
	}

	/** Decoration must include chunks intersecting the circular rim, even when their corner lies outside. */
	public static Zone forChunk(long seed, int startX, int startZ) {
		int cellX = Math.floorDiv(startX, SPACING), cellZ = Math.floorDiv(startZ, SPACING);
		for (var zone : zonesInCell(seed, cellX, cellZ)) {
			if (zone.intersects(startX, startZ, startX + 16, startZ + 16)) return zone;
		}
		return null;
	}

	public static Zone at(long seed, double x, double z) {
		int cellX = Math.floorDiv((int) Math.floor(x), SPACING), cellZ = Math.floorDiv((int) Math.floor(z), SPACING);
		Zone zone = inCell(seed, cellX, cellZ);
		if (zone.contains(x, z)) return zone;
		Zone crater = craterInCell(seed, cellX, cellZ);
		if (crater != null && crater.contains(x, z)) return crater;
		for (int kind = 5; kind <= TYPE_COUNT; kind++) {
			zone = additionalInCell(seed, cellX, cellZ, kind);
			if (zone != null && zone.contains(x, z)) return zone;
		}
		return null;
	}

	public static Zone at(Level level, double x, double z) {
		if (!level.dimension().equals(Level.OVERWORLD)) return null;
		Long seed = level instanceof ServerLevel server ? layoutSeed(server.getSeed()) : clientSeed;
		return seed == null ? null : at(seed, x, z);
	}

	public static boolean contains(Level level, BlockPos pos) {
		return at(level, pos.getX() + 0.5, pos.getZ() + 0.5) != null;
	}

	public static boolean contains(Entity entity) {
		return at(entity.level(), entity.getX(), entity.getZ()) != null;
	}

	public static double gravity(Entity entity) {
		Zone zone = at(entity.level(), entity.getX(), entity.getZ());
		return zone == null ? 1 : zone.gravity(entity.getX(), entity.getZ());
	}

	public static boolean frozen(Entity entity) {
		return (entity instanceof Mob || entity instanceof net.minecraft.world.entity.item.FallingBlockEntity
				|| transport(entity)) && contains(entity);
	}

	public static boolean transport(Entity entity) {
		return entity instanceof VehicleEntity || entity instanceof dev.kirill.hbk.entity.FlyingCarpetEntity
				|| entity instanceof dev.kirill.hbk.entity.SovietBusEntity;
	}

	public static void setClientSeed(Long seed) {
		clientSeed = seed;
	}

	public static Long clientSeed() {
		return clientSeed;
	}

	public static Zone nearest(long seed, int x, int z, int kind) {
		if (kind < 1 || kind > TYPE_COUNT) return null;
		Zone nearest = null;
		double distance = Double.MAX_VALUE;
		int cellX = Math.floorDiv(x, SPACING), cellZ = Math.floorDiv(z, SPACING);
		for (int dx = -8; dx <= 8; dx++) for (int dz = -8; dz <= 8; dz++) {
			Zone zone = kind >= 5 ? additionalInCell(seed, cellX + dx, cellZ + dz, kind)
					: kind == 4 ? craterInCell(seed, cellX + dx, cellZ + dz) : inCell(seed, cellX + dx, cellZ + dz);
			if (zone == null || zone.kind != kind) continue;
			double candidate = Math.pow(zone.centerX() - x, 2) + Math.pow(zone.centerZ() - z, 2);
			if (candidate < distance) { nearest = zone; distance = candidate; }
		}
		return nearest;
	}

	public static void register() {
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> RepeatedAnomaly.clear());
		var positions = new HashMap<UUID, PreviousPosition>();
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
				positions.remove(handler.player.getUUID()));
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> positions.clear());
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING.register(server ->
				AnomalyTransientBlocks.restore(server.overworld(), true));
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			if (ServerPlayNetworking.canSend(handler.player, ModNetworking.AnomalyLayoutPayload.TYPE)) {
				sender.sendPacket(new ModNetworking.AnomalyLayoutPayload(layoutSeed(server.overworld().getSeed())));
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			AnomalyTransientBlocks.tick(server.overworld());
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				var previous = positions.put(player.getUUID(), new PreviousPosition(player.level().dimension(), player.position()));
				if (!player.isAlive() || player.isSpectator() || !player.level().dimension().equals(Level.OVERWORLD)) continue;
				Zone zone = at(player.level(), player.getX(), player.getZ());
				Vec3 from = previous != null && previous.dimension.equals(player.level().dimension())
						&& previous.position.distanceToSqr(player.position()) < 64 * 64 ? previous.position : player.position();
				if (touchesCeiling(player, from)) { kill(player); continue; }
				if (zone == null) continue;
				award(player, "anomaly_" + zone.kind);
				boolean all = true;
				for (int kind = 1; kind <= TYPE_COUNT; kind++) {
					var advancement = server.getAdvancements().get(HbkMod.id("anomaly_" + kind));
					all &= advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
				}
				if (all) award(player, "anomalist");
			}
		});
	}

	private record PreviousPosition(ResourceKey<Level> dimension, Vec3 position) {}

	public static boolean touchesCeiling(ServerPlayer player, Vec3 previousPosition) {
		// Include the previous position: fast elytra flight can cross both the plane and the border in one tick.
		AABB swept = player.getBoundingBox().minmax(player.getBoundingBox().move(
				previousPosition.x - player.getX(), previousPosition.y - player.getY(), previousPosition.z - player.getZ()));
		if (swept.maxY < CEILING_Y - 0.125 || swept.minY > CEILING_Y + 0.125) return false;
		int cellX = Math.floorDiv((int) Math.floor(player.getX()), SPACING);
		int cellZ = Math.floorDiv((int) Math.floor(player.getZ()), SPACING);
		long seed = layoutSeed(player.level().getSeed());
		for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
			for (Zone zone : zonesInCell(seed, cellX + dx, cellZ + dz)) {
				if (swept.intersects(zone.ceiling()) && zone.intersects(swept.minX, swept.minZ, swept.maxX, swept.maxZ)) return true;
			}
		}
		return false;
	}

	public static void award(ServerPlayer player, String id) {
		var advancement = player.level().getServer().getAdvancements().get(HbkMod.id(id));
		if (advancement != null) player.getAdvancements().award(advancement, "unlock");
	}

	public static void kill(ServerPlayer player) {
		DamageSource damage = new DamageSource(player.level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(CONTACT));
		for (int attempt = 0; attempt < 3 && player.isAlive(); attempt++) player.hurtServer(player.level(), damage, Float.MAX_VALUE);
		if (player.isAlive()) { player.setHealth(0); player.die(damage); }
		if (ServerPlayNetworking.canSend(player, ModNetworking.UnknownDeathPayload.TYPE)) {
			ServerPlayNetworking.send(player, ModNetworking.UnknownDeathPayload.INSTANCE);
		}
	}
}

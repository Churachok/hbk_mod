package dev.kirill.hbk.universe;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.mixin.UniverseServerAccess;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.DerivedLevelData;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class UniverseManager {
	public static final ResourceKey<DimensionType> TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, HbkMod.id("universe"));
	private static final Map<MinecraftServer, Map<ResourceKey<Level>, Long>> IDLE = new WeakHashMap<>();
	private UniverseManager() {}
	public static ResourceKey<Level> key(UniverseCode code) { return ResourceKey.create(Registries.DIMENSION, HbkMod.id("universe_" + code.value())); }
	public static void register() {
		UniverseHazards.register();
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 200 != 0) return;
			var idle = IDLE.computeIfAbsent(server, ignored -> new HashMap<>());
			for (ServerLevel level : List.copyOf(((UniverseServerAccess) server).hbk$levels().values())) {
				if (UniverseProfile.of(level) == null) continue;
				if (!level.players().isEmpty() || !level.getForceLoadedChunks().isEmpty()) { idle.remove(level.dimension()); continue; }
				long since = idle.computeIfAbsent(level.dimension(), ignored -> (long) server.getTickCount());
				if (server.getTickCount() - since >= 1200 && unload(server, level)) idle.remove(level.dimension());
			}
		});
	}
	public static ServerLevel load(MinecraftServer server, UniverseCode code) {
		if (!server.isSameThread()) throw new IllegalStateException("Universe creation must run on the server thread");
		var access = (UniverseServerAccess) server;
		var key = key(code);
		var existing = access.hbk$levels().get(key);
		if (existing != null) return existing;
		UniverseProfile profile = UniverseProfile.of(code);
		var biome = server.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(ResourceKey.create(Registries.BIOME, HbkMod.id("universe")));
		var generator = new UniverseChunkGenerator(new FixedBiomeSource(biome), profile);
		var type = server.registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE).getOrThrow(TYPE);
		var data = new DerivedLevelData(server.getWorldData(), server.getWorldData().overworldData());
		var level = new ServerLevel(server, access.hbk$executor(), access.hbk$storage(), data, key,
				new LevelStem(type, generator), false, profile.seed(), List.of(), false);
		var saved = level.getDataStorage().computeIfAbsent(UniverseState.type(profile));
		if (!saved.profile.equals(profile)) {
			try { level.close(); } catch (IOException e) { HbkMod.LOGGER.error("Closing incompatible universe", e); }
			throw new IllegalStateException("Saved profile differs for " + code + "; refusing to generate incompatible chunks");
		}
		saved.setDirty();
		level.getWorldBorder().setAbsoluteMaxSize(server.getAbsoluteMaxWorldSize());
		access.hbk$levels().put(key, level);
		// Vanilla performs this after createLevels; lazily created levels need the same step.
		level.getDataStorage().computeIfAbsent(net.minecraft.world.level.TicketStorage.TYPE).activateAllDeactivatedTickets();
		server.getPlayerList().addWorldborderListener(level);
		ServerLevelEvents.LOAD.invoker().onLevelLoad(server, level);
		HbkMod.LOGGER.info("Loaded universe {} (seed {}, generator v{})", code, profile.seed(), profile.version());
		return level;
	}
	/** Explicitly flush chunks, block entities, entities, POIs and SavedData before releasing files. */
	public static boolean unload(MinecraftServer server, ServerLevel level) {
		if (UniverseProfile.of(level) == null || !level.players().isEmpty() || !level.getForceLoadedChunks().isEmpty()) return false;
		try {
			level.getChunkSource().deactivateTicketsOnClosing();
			level.save(null, true, false);
			ServerLevelEvents.UNLOAD.invoker().onLevelUnload(server, level);
			level.close();
			((UniverseServerAccess) server).hbk$levels().remove(level.dimension());
			return true;
		} catch (IOException e) { HbkMod.LOGGER.error("Could not unload {}", level.dimension().identifier(), e); return false; }
	}
	public static boolean travel(ServerPlayer player, UniverseCode code) {
		if (player.isSpectator() || player.isPassenger() || player.isVehicle()) return false;
		UniverseProfile current = UniverseProfile.of(player.level());
		if (current != null && current.code().equals(code)) return false;
		ServerLevel destination = load(player.level().getServer(), code);
		var data = UniverseTravelData.get(player.level());
		var previous = data.get(player, code.value());
		BlockPos arrival = safeArrival(destination, previous == null ? BlockPos.ZERO : BlockPos.containing(previous.x(), previous.y(), previous.z()));
		BlockPos returnFurnace = arrival == null ? null : UniverseArrival.ensureReturnFurnace(destination, arrival);
		if (returnFurnace == null) {
			player.sendSystemMessage(Component.translatable("message.hbk.universe.no_arrival")); return false;
		}
		data.put(player, current == null ? "home" : current.code().value());
		player.closeContainer();
		boolean moved = player.teleportTo(destination, arrival.getX() + .5, arrival.getY(), arrival.getZ() + .5,
				Set.of(), previous == null ? player.getYRot() : previous.yaw(), previous == null ? player.getXRot() : previous.pitch(), true);
		if (moved) AlloyWorldData.get(destination).rememberEntrance(player, returnFurnace);
		player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO); player.resetFallDistance();
		return moved;
	}
	public static boolean returnHome(ServerPlayer player) {
		var profile = UniverseProfile.of(player.level());
		if (profile == null || player.isPassenger() || player.isVehicle()) return false;
		var data = UniverseTravelData.get(player.level());
		var home = data.get(player, "home");
		if (home == null) return false;
		var level = player.level().getServer().getLevel(home.key());
		if (level == null) return false;
		BlockPos arrival = safeArrival(level, BlockPos.containing(home.x(), home.y(), home.z()));
		if (arrival == null) return false;
		data.put(player, profile.code().value());
		player.closeContainer();
		boolean moved = player.teleportTo(level, arrival.getX() + .5, arrival.getY(), arrival.getZ() + .5, Set.of(), home.yaw(), home.pitch(), true);
		player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO); player.resetFallDistance();
		return moved;
	}
	private static boolean safe(ServerLevel level, BlockPos pos) {
		if (!level.getWorldBorder().isWithinBounds(pos) || pos.getY() <= level.getMinY() || pos.getY() + 2 > level.getMaxY()) return false;
		return level.getBlockState(pos.below()).isCollisionShapeFullBlock(level, pos.below())
				&& level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()
				&& level.getBlockState(pos.above(2)).isAir();
	}
	public static BlockPos safeArrival(ServerLevel level, BlockPos preferred) {
		if (safe(level, preferred)) return preferred.immutable();
		// A bounded search. Never generate a huge area or carve a player's existing construction.
		var arrivalProfile = UniverseProfile.of(level);
		int ceiling = arrivalProfile != null && !arrivalProfile.openSky() ? 200 : level.getMaxY() - 4;
		for (int radius = 0; radius <= 16; radius += 4) for (int dx = -radius; dx <= radius; dx += 4) for (int dz = -radius; dz <= radius; dz += 4) {
			if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
			for (int y = ceiling; y > level.getMinY() + 4; y--) {
				BlockPos pos = new BlockPos(preferred.getX() + dx, y, preferred.getZ() + dz);
				if (safe(level, pos) && safe(level, pos.north()) && safe(level, pos.south()) && safe(level, pos.east()) && safe(level, pos.west())) return pos;
			}
		}
		var profile = UniverseProfile.of(level);
		if (profile == null) return null;
		for (int y = 100; y < level.getMaxY() - 6; y += 4) {
			BlockPos pos = new BlockPos(preferred.getX(), y, preferred.getZ());
			if (!level.getWorldBorder().isWithinBounds(pos.offset(-2, 0, -2)) || !level.getWorldBorder().isWithinBounds(pos.offset(2, 0, 2))) return null;
			boolean empty = true;
			for (BlockPos p : BlockPos.betweenClosed(pos.offset(-2, -1, -2), pos.offset(2, 3, 2))) if (!level.getBlockState(p).isAir()) { empty = false; break; }
			if (!empty) continue;
			for (BlockPos p : BlockPos.betweenClosed(pos.offset(-2, -1, -2), pos.offset(2, -1, 2)))
				level.setBlock(p, UniverseMaterials.state(profile.geometry(), 0, 0), 2);
			return pos;
		}
		return null;
	}
}

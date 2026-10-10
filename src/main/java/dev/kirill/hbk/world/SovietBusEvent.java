package dev.kirill.hbk.world;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.entity.SovietBusEntity;
import dev.kirill.hbk.registry.ModEntityTypes;
import dev.kirill.hbk.registry.ModSounds;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class SovietBusEvent {
	public static final double CHANCE_PER_SECOND = 0.005;
	public static final double BOARDING_SMOKE_RADIUS = 10;
	public static final int MIN_BOARDING_FOG_TICKS = 10;
	private static final Map<UUID, BusDestinationSearch> TRIPS = new HashMap<>();
	private static final Map<UUID, SovietBusEntity> BUSES = new HashMap<>();

	private SovietBusEvent() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(SovietBusEvent::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			TRIPS.clear();
			BUSES.clear();
		});
	}

	private static void tick(MinecraftServer server) {
		cleanupBuses(server, server.getTickCount());
		var iterator = TRIPS.entrySet().iterator();
		int tripCount = TRIPS.size();
		int offset = tripCount == 0 ? 0 : Math.floorMod(server.getTickCount(), tripCount);
		int tripIndex = 0;
		while (iterator.hasNext()) {
			var entry = iterator.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			BusDestinationSearch search = entry.getValue();
			if (player == null || !player.isAlive() || player.isSpectator()
					|| !player.level().dimension().equals(search.departureDimension)) {
				iterator.remove();
				continue;
			}
			if (Math.floorMod(tripIndex++ - offset, tripCount) < 2) {
				search.tick();
			}
			if (search.result != null && server.getTickCount() - search.startedAt >= MIN_BOARDING_FOG_TICKS) {
				// Remove the route BEFORE teleporting. The old chunk must not be needed
				// to complete or release either the route or the encounter.
				iterator.remove();
				completeTrip(player, search.level, search.result);
			} else if (search.failed || server.getTickCount() - search.startedAt > 20 * 120) {
				player.sendSystemMessage(Component.translatable("message.hbk.bus.no_destination"));
				iterator.remove();
			}
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (canTriggerNaturally(player) && player.level().getGameTime() % 20 == 0
					&& !TRIPS.containsKey(player.getUUID())
					&& player.getRandom().nextDouble() < CHANCE_PER_SECOND) {
				start(player);
			}
		}
	}

	static void cleanupBuses(MinecraftServer server, int serverTick) {
		var iterator = BUSES.entrySet().iterator();
		while (iterator.hasNext()) {
			var entry = iterator.next();
			var bus = entry.getValue();
			var owner = server.getPlayerList().getPlayer(entry.getKey());
			if (bus.isRemoved() || bus.hasExpired(serverTick) || owner == null || !owner.isAlive()
					|| owner.isSpectator() || owner.level() != bus.level()) {
				bus.discard();
				iterator.remove();
			}
		}
	}

	static boolean canTriggerNaturally(ServerPlayer player) {
		return player.isAlive() && !player.isCreative() && !player.isSpectator()
				&& !player.isPassenger() && player.onGround()
				&& player.level().dimension().equals(Level.OVERWORLD)
				&& !UnknownEncounter.isActive(player);
	}

	public static StartResult start(ServerPlayer player) {
		if (!player.isAlive() || player.isSpectator() || UnknownEncounter.isActive(player)) {
			return StartResult.UNAVAILABLE;
		}
		if (isActive(player)) {
			return StartResult.ALREADY_ACTIVE;
		}
		for (ServerLevel level : player.level().getServer().getAllLevels()) {
			if (!level.getEntities(ModEntityTypes.SOVIET_BUS,
					bus -> !bus.isRemoved() && player.getUUID().equals(bus.getOwner())).isEmpty()) {
				return StartResult.ALREADY_ACTIVE;
			}
		}
		ServerLevel level = player.level();
		int preferredDirection = Math.floorMod(Math.round(player.getYRot() / 90.0f), 4);
		Vec3 forward = Vec3.directionFromRotation(0, preferredDirection * 90).horizontal().normalize();
		// Never inspect terrain, headroom, liquids or a route: this encounter can pass
		// through blocks and appear underground, in cramped rooms or above water.
		Vec3 stop = player.position().add(forward.scale(4.8));
		var bus = ModEntityTypes.SOVIET_BUS.create(level, EntitySpawnReason.EVENT);
		if (bus == null) return StartResult.UNAVAILABLE;
		bus.begin(player.getUUID(), stop, forward.scale(-1));
		if (!level.addFreshEntity(bus)) return StartResult.UNAVAILABLE;
		level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.BUS_HORN,
				SoundSource.NEUTRAL, 2.0f, 1.0f);
		BUSES.put(player.getUUID(), bus);
		return StartResult.STARTED;
	}

	public static boolean isActive(ServerPlayer player) {
		var bus = BUSES.get(player.getUUID());
		return TRIPS.containsKey(player.getUUID()) || bus != null && !bus.isRemoved();
	}

	static SovietBusEntity activeBus(ServerPlayer player) {
		return BUSES.get(player.getUUID());
	}

	public static boolean board(ServerPlayer player, SovietBusEntity bus) {
		if (!bus.isWaiting() || bus.level() != player.level() || !startTrip(player)) return false;
		sendBoardingSmoke(player.level(), bus.position());
		// The van is consumed now, not after a departure animation in a possibly unloaded chunk.
		BUSES.remove(bus.getOwner(), bus);
		bus.discard();
		return true;
	}

	/** A dense, approximately ten-block smoke bank broadcast to every nearby observer. */
	static int sendBoardingSmoke(ServerLevel level, Vec3 center) {
		int observers = 0;
		double innerRadius = BOARDING_SMOKE_RADIUS - 1;
		for (int x = -8; x <= 8; x += 2) {
			for (int z = -8; z <= 8; z += 2) {
				if (x * x + z * z > innerRadius * innerRadius) continue;
				observers = Math.max(observers, level.sendParticles(ParticleTypes.CLOUD, true, true,
						center.x + x, center.y + 1.7, center.z + z, 24, 0.8, 1.3, 0.8, 0.015));
				observers = Math.max(observers, level.sendParticles(ParticleTypes.LARGE_SMOKE, true, true,
						center.x + x, center.y + 1.7, center.z + z, 16, 0.8, 1.3, 0.8, 0.01));
			}
		}
		level.sendParticles(ParticleTypes.CLOUD, true, true, center.x, center.y + 1.7, center.z,
				320, 2.5, 1.3, 2.5, 0.015);
		return observers;
	}

	public static boolean startTrip(ServerPlayer player) {
		// Pick the TYPE before looking up any locations. Distance and availability
		// must never affect the six-way lottery or replace its result with another type.
		Destination destination = chooseDestination(player.getRandom());
		return startTrip(player, destination);
	}

	static Destination chooseDestination(RandomSource random) {
		Destination[] destinations = Destination.values();
		return destinations[random.nextInt(destinations.length)];
	}

	static BusDestinationSearch activeTrip(ServerPlayer player) {
		return TRIPS.get(player.getUUID());
	}

	static boolean startTrip(ServerPlayer player, Destination destination) {
		if (!player.isAlive() || player.isSpectator() || UnknownEncounter.isActive(player)
				|| TRIPS.containsKey(player.getUUID())) {
			return false;
		}
		ServerLevel overworld = player.level().getServer().overworld();
		if (overworld == null) {
			return false;
		}
		TRIPS.put(player.getUUID(), new BusDestinationSearch(overworld, player, destination));
		player.sendSystemMessage(Component.translatable("message.hbk.bus.traveling", destination.title()));
		return true;
	}

	static void completeTrip(ServerPlayer player, ServerLevel level, Vec3 position) {
		TRIPS.remove(player.getUUID());
		var bus = BUSES.remove(player.getUUID());
		if (bus != null) bus.discard();
		player.stopRiding();
		player.teleportTo(level, position.x, position.y, position.z, Set.of(),
				player.getYRot(), player.getXRot(), false);
		player.setDeltaMovement(Vec3.ZERO);
		player.fallDistance = 0;
		// Also cover the passenger's new camera position; departure particles alone
		// would stay behind and disappear from their view as soon as they teleport.
		sendBoardingSmoke(level, position);
		var advancement = level.getServer().getAdvancements().get(HbkMod.id("bus_fare"));
		if (advancement != null) {
			player.getAdvancements().award(advancement, "ride");
		}
	}

	public enum StartResult { STARTED, BLOCKED, ALREADY_ACTIVE, UNAVAILABLE }

	public enum Destination {
		STALIN_DACHA("stalin_dacha"), GULAG("gulag"), STALINKA("stalinka"),
		KIRILL_HOUSE("kirill_house"), KIRILL_HOUSE_HBK("kirill_house_hbk"), WASTELAND(null);
		final String structure;

		Destination(String structure) {
			this.structure = structure;
		}

		Component title() {
			return Component.translatable("destination.hbk.bus." + (structure == null ? "wasteland" : structure));
		}
	}
}

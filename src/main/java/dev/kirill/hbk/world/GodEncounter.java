package dev.kirill.hbk.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.entity.GodEntity;
import dev.kirill.hbk.network.ModNetworking;
import dev.kirill.hbk.registry.ModEntityTypes;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/** Persistent, collision-free pursuit, independent of the pursuer's chunk being loaded. */
public final class GodEncounter extends SavedData {
	public static final double SPAWN_DISTANCE = 200;
	public static final double EXTRA_BLOCKS_PER_SECOND = 4;
	public static final double CATCH_DISTANCE = 1.25;
	public static final ResourceKey<DamageType> GOD_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, HbkMod.id("god"));
	public record Chase(String dimension, Vec3 position, Vec3 lastTarget) {
		public static final Codec<Chase> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.fieldOf("dimension").forGetter(Chase::dimension),
				Vec3.CODEC.fieldOf("position").forGetter(Chase::position),
				Vec3.CODEC.fieldOf("last_target").forGetter(Chase::lastTarget)).apply(i, Chase::new));
	}
	public static final Codec<GodEncounter> CODEC = Codec.unboundedMap(Codec.STRING, Chase.CODEC)
			.xmap(GodEncounter::new, data -> Map.copyOf(data.chases));
	public static final SavedDataType<GodEncounter> TYPE = new SavedDataType<>(HbkMod.id("god_chases"),
			GodEncounter::new, CODEC, DataFixTypes.SAVED_DATA_RAIDS);
	private final Map<String, Chase> chases = new HashMap<>();
	private final Map<String, GodEntity> visuals = new HashMap<>();

	public GodEncounter() {}
	private GodEncounter(Map<String, Chase> chases) { this.chases.putAll(chases); }
	public static GodEncounter get(MinecraftServer server) { return server.overworld().getDataStorage().computeIfAbsent(TYPE); }
	public static void register() { ServerTickEvents.END_SERVER_TICK.register(GodEncounter::tick); }
	public static boolean isActive(ServerPlayer player) { return get(player.level().getServer()).chases.containsKey(key(player)); }
	public Chase chase(ServerPlayer player) { return chases.get(key(player)); }
	private static String key(ServerPlayer player) { return player.getUUID().toString(); }
	private static String dimension(ServerPlayer player) { return player.level().dimension().identifier().toString(); }
	private static Vec3 target(ServerPlayer player) { return player.getBoundingBox().getCenter(); }

	public static StartResult start(ServerPlayer player) {
		if (!player.isAlive() || player.isSpectator() || UnknownEncounter.isActive(player) || SovietBusEvent.isActive(player)) {
			return StartResult.UNAVAILABLE;
		}
		var data = get(player.level().getServer());
		if (data.chases.containsKey(key(player))) return StartResult.ALREADY_ACTIVE;
		// Choose an inward direction near a world border without changing the 200-block radius.
		for (int attempt = 0; attempt < 16; attempt++) {
			Vec3 direction = Vec3.directionFromRotation(0, player.getYRot() + attempt * 22.5f);
			Vec3 position = target(player).add(direction.scale(SPAWN_DISTANCE));
			if (player.level().getWorldBorder().isWithinBounds(net.minecraft.core.BlockPos.containing(position))) {
				var chase = new Chase(dimension(player), position, target(player));
				data.chases.put(key(player), chase);
				data.setDirty();
				data.updateVisual(player, chase);
				sendState(player, true, true, SPAWN_DISTANCE);
				return StartResult.STARTED;
			}
		}
		return StartResult.UNAVAILABLE;
	}

	private static void tick(MinecraftServer server) {
		var data = get(server);
		// Offline owners retain their saved chase, but never retain live visual anchors.
		data.visuals.entrySet().removeIf(entry -> {
			ServerPlayer owner = server.getPlayerList().getPlayer(java.util.UUID.fromString(entry.getKey()));
			if (owner != null && data.chases.containsKey(entry.getKey())) return false;
			entry.getValue().discard();
			return true;
		});
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (data.chases.containsKey(key(player))) data.advance(player);
			else if (UnknownEncounter.canTriggerNaturally(player) && !UnknownEncounter.isActive(player)
					&& player.getRandom().nextDouble() < UnknownEncounter.WALKING_TRIGGER_CHANCE) start(player);
		}
	}

	/** Also used by game tests: one authoritative step, never an entity AI tick. */
	void advance(ServerPlayer player) {
		Chase chase = chases.get(key(player));
		if (chase == null) return;
		if (!player.isAlive()) {
			removeVisual(player);
			sendState(player, false, false, 0);
			return;
		}
		// Spectator is an administrative pause, not an escape from a saved chase.
		if (player.isSpectator()) {
			removeVisual(player);
			sendState(player, false, false, 0);
			return;
		}
		Vec3 position = chase.position();
		if (!chase.dimension().equals(dimension(player))) {
			// Follow through portals without restarting at a fresh 200-block distance.
			position = target(player).add(position.subtract(chase.lastTarget()));
		}
		Vec3 offset = target(player).subtract(position);
		double step = chaseStep(player);
		if (offset.length() <= CATCH_DISTANCE + step) {
			kill(player);
			finish(player);
			return;
		}
		Chase next = new Chase(dimension(player), position.add(offset.normalize().scale(step)), target(player));
		chases.put(key(player), next);
		setDirty();
		updateVisual(player, next);
		sendState(player, true, false, offset.length() - step);
	}

	/** Vanilla ground travel converts movement speed 0.1 to approximately 4.317 blocks/s.
	 * Known movement also covers elytra, vehicles, flight and external acceleration. */
	public static double chaseStep(ServerPlayer player) {
		double groundStep = player.getAttributeValue(Attributes.MOVEMENT_SPEED) * 2.1585;
		return Math.max(groundStep, player.getKnownMovement().length()) + EXTRA_BLOCKS_PER_SECOND / 20;
	}

	private void updateVisual(ServerPlayer player, Chase chase) {
		GodEntity god = visuals.get(key(player));
		if (god == null || god.isRemoved() || god.level() != player.level()) {
			removeVisual(player);
			god = ModEntityTypes.GOD.create(player.level(), EntitySpawnReason.EVENT);
			if (god == null) return;
			god.updatePursuit(player.position(), chase.position());
			if (!player.level().addFreshEntity(god)) return;
			visuals.put(key(player), god);
		} else god.updatePursuit(player.position(), chase.position());
	}

	private void removeVisual(ServerPlayer player) {
		GodEntity visual = visuals.remove(key(player));
		if (visual != null) visual.discard();
	}
	private void finish(ServerPlayer player) {
		chases.remove(key(player));
		removeVisual(player);
		setDirty();
		sendState(player, false, false, 0);
	}
	private static void kill(ServerPlayer player) {
		var level = player.level();
		DamageSource damage = new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(GOD_DAMAGE));
		player.hurtServer(level, damage, Float.MAX_VALUE);
		if (player.isAlive()) {
			// A loading client/portal transition can reject even bypass damage. Preserve
			// the event's death message when the guaranteed-kill fallback is needed.
			player.getCombatTracker().recordDamage(damage, player.getHealth());
			player.setHealth(0);
			player.die(damage);
		}
		var advancement = level.getServer().getAdvancements().get(HbkMod.id("god"));
		if (advancement != null) player.getAdvancements().award(advancement, "unlock");
	}
	private static void sendState(ServerPlayer player, boolean active, boolean announce, double distance) {
		if (ServerPlayNetworking.canSend(player, ModNetworking.GodEncounterPayload.TYPE))
			ServerPlayNetworking.send(player, new ModNetworking.GodEncounterPayload(active, announce, distance));
	}
	public enum StartResult { STARTED, ALREADY_ACTIVE, UNAVAILABLE }
}

package dev.kirill.hbk.world;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.entity.KirillSecondEntity;
import dev.kirill.hbk.network.ModNetworking;
import dev.kirill.hbk.registry.ModEntityTypes;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** The fatal dialogue encounter with the Unknown. */
public final class UnknownEncounter {
	public static final ResourceKey<DamageType> LOST_IN_TIME_DAMAGE = ResourceKey.create(
			Registries.DAMAGE_TYPE, HbkMod.id("lost_in_time"));
	private static final double WALKING_TRIGGER_CHANCE = 0.0001;
	private static final double SPAWN_DISTANCE = 4.0;
	private static final int PORTAL_OPEN_TICKS = 60;
	private static final int PORTAL_EXIT_TICKS = 20;
	private static final double PORTAL_EXIT_DISTANCE = 0.8;
	private static final int DEPARTURE_PORTAL_OPEN_TICKS = 40;
	private static final int PORTAL_RETURN_TICKS = 40;
	private static final int PORTAL_CLOSE_TICKS = 30;
	private static final double APPROACH_STEP = 0.06;
	private static final double GRAB_DISTANCE = 1.0;
	private static final double GRAB_TRIGGER_DISTANCE = GRAB_DISTANCE + 0.15;
	private static final double CAMERA_TARGET_VERTICAL_OFFSET = -0.3;
	private static final double PLAYER_LIFT_HEIGHT = 0.65;
	private static final int PLAYER_LIFT_TICKS = 10;
	private static final int POST_FINAL_LEAD_DELAY_TICKS = 40;
	private static final int TICKS_PER_CHARACTER = 3;
	private static final String FINAL_MESSAGE = "ТЫ ВООБЩЕ НЕ ДОЛЖЕН БЫЛ СУЩЕСТВОВАТЬ";
	private static final int FINAL_MESSAGE_TICKS = FINAL_MESSAGE.codePointCount(0, FINAL_MESSAGE.length())
			* TICKS_PER_CHARACTER;
	private static final Map<UUID, Encounter> ACTIVE = new HashMap<>();

	private UnknownEncounter() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(UnknownEncounter::tick);
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			Encounter encounter = ACTIVE.remove(oldPlayer.getUUID());
			if (encounter != null) {
				restorePlayerState(newPlayer, encounter);
				removeUnknown(newPlayer.level().getServer(), encounter);
				sendRemoveUnknown(newPlayer, encounter.unknownEntityId, encounter.unknownUuid);
				sendClientState(newPlayer, -1, false);
			}
			removeOrphanedUnknowns(newPlayer);
			sendMusicState(newPlayer, false);
		});
	}

	public static StartResult start(ServerPlayer player) {
		if (!player.isAlive()) {
			return StartResult.UNAVAILABLE;
		}
		if (ACTIVE.containsKey(player.getUUID())) {
			return StartResult.ALREADY_ACTIVE;
		}

		ServerLevel level = player.level();
		Vec3 forward = horizontalLook(player);
		Vec3 spawnPosition = player.position().add(forward.scale(SPAWN_DISTANCE));
		if (!hasClearSpace(level, player, forward, spawnPosition)) {
			return StartResult.BLOCKED;
		}

		KirillSecondEntity unknown = ModEntityTypes.KIRILL_V2.create(level, EntitySpawnReason.EVENT);
		if (unknown == null) {
			return StartResult.UNAVAILABLE;
		}
		unknown.snapTo(spawnPosition.x, spawnPosition.y, spawnPosition.z,
				player.getYRot() + 180.0f, 0.0f);
		unknown.setNoAi(true);
		unknown.setNoGravity(true);
		unknown.setInvulnerable(true);
		unknown.markEncounterEntity(player.getUUID());
		unknown.setPortalHidden(true);
		if (!level.noCollision(unknown) || !level.addFreshEntity(unknown)) {
			unknown.discard();
			return StartResult.BLOCKED;
		}
		facePlayer(unknown, player);
		unknown.setOldRot();

		boolean wasInvulnerable = player.isInvulnerable();
		boolean wasNoGravity = player.isNoGravity();
		player.stopRiding();
		player.setInvulnerable(true);
		player.setDeltaMovement(Vec3.ZERO);
		player.fallDistance = 0.0;
		lockCameraOnUnknown(player, unknown);
		ACTIVE.put(player.getUUID(), new Encounter(
				level.dimension(), player.position(), spawnPosition, unknown.getUUID(), unknown.getId(),
				wasInvulnerable, wasNoGravity));
		sendClientState(player, unknown.getId(), true);
		sendMusicState(player, true);
		return StartResult.STARTED;
	}

	public static boolean isActive(ServerPlayer player) {
		return ACTIVE.containsKey(player.getUUID());
	}

	private static void tick(MinecraftServer server) {
		Iterator<Map.Entry<UUID, Encounter>> iterator = ACTIVE.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, Encounter> entry = iterator.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			Encounter encounter = entry.getValue();
			if (player == null) {
				removeUnknown(server, encounter);
				iterator.remove();
				continue;
			}

			ServerLevel encounterLevel = server.getLevel(encounter.dimension);
			Entity foundUnknown = encounterLevel == null
					? null
					: encounterLevel.getEntityInAnyDimension(encounter.unknownUuid);
			if (encounter.isDeparting()) {
				if (encounterLevel == null) {
					removeUnknown(server, encounter);
					iterator.remove();
					continue;
				}
				KirillSecondEntity departingUnknown = foundUnknown instanceof KirillSecondEntity candidate
						&& candidate.isAlive() ? candidate : null;
				if (encounter.phase != Phase.PORTAL_CLOSING && departingUnknown == null) {
					iterator.remove();
					continue;
				}
				if (tickDeparture(encounterLevel, player, departingUnknown, encounter)) {
					iterator.remove();
				}
				continue;
			}
			if (!player.isAlive()) {
				restorePlayerState(player, encounter);
				sendClientState(player, -1, false);
				removeUnknown(server, encounter);
				iterator.remove();
				continue;
			}
			if (encounterLevel == null || !(foundUnknown instanceof KirillSecondEntity unknown)
					|| !unknown.isAlive()) {
				restorePlayerState(player, encounter);
				sendClientState(player, -1, false);
				sendMusicState(player, false);
				iterator.remove();
				continue;
			}

			Vec3 lockedPosition = lockedPlayerPosition(encounter);
			if (player.level() != encounterLevel) {
				player.teleportTo(encounterLevel, lockedPosition.x, lockedPosition.y,
						lockedPosition.z, Set.of(), player.getYRot(), player.getXRot(), false);
			} else if (player.position().distanceToSqr(lockedPosition) > 1.0E-6) {
				player.teleportTo(lockedPosition.x, lockedPosition.y, lockedPosition.z);
			}
			player.setDeltaMovement(Vec3.ZERO);
			player.fallDistance = 0.0;
			if (encounter.phase == Phase.GRABBING || encounter.phase == Phase.TYPING) {
				player.setNoGravity(true);
			}
			facePlayer(unknown, player);
			lockCameraOnUnknown(player, unknown);

			if (encounter.phase == Phase.PORTAL || encounter.phase == Phase.EMERGING) {
				if (tickScene(server, encounterLevel, player, unknown, encounter)) {
					iterator.remove();
				}
				continue;
			}

			encounter.elapsedTicks++;
			advanceDialogue(player, unknown, encounter);
			if (tickScene(server, encounterLevel, player, unknown, encounter)) {
				iterator.remove();
			}
		}

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!ACTIVE.containsKey(player.getUUID()) && canTriggerNaturally(player)
					&& player.getRandom().nextDouble() < WALKING_TRIGGER_CHANCE) {
				start(player);
			}
		}
	}

	private static void advanceDialogue(ServerPlayer player, KirillSecondEntity unknown, Encounter encounter) {
		switch (encounter.elapsedTicks) {
			case 40 -> sendUnknownMessage(player, "Ты явно не тот кто мне нужен");
			case 100 -> sendUnknownMessage(player, "Но тогда почему я оказался здесь?");
			case 160 -> {
				sendUnknownMessage(player, "Хотя если посмотреть на тебя ");
				encounter.phase = Phase.APPROACH;
			}
			case 200 -> {
				sendUnknownMessage(player, "то всё встаёт на свои места");
				encounter.finalLeadDelivered = true;
			}
			default -> {
			}
		}
	}

	private static boolean tickScene(MinecraftServer server, ServerLevel level, ServerPlayer player,
			KirillSecondEntity unknown, Encounter encounter) {
		if (encounter.phase == Phase.PORTAL) {
			unknown.setPortalHidden(true);
			renderPortal(level, encounter);
			encounter.phaseTicks++;
			if (encounter.phaseTicks >= PORTAL_OPEN_TICKS) {
				unknown.setPortalHidden(false);
				encounter.phase = Phase.EMERGING;
				encounter.phaseTicks = 0;
			}
		} else if (encounter.phase == Phase.EMERGING) {
			renderPortal(level, encounter);
			Vec3 exitDirection = encounter.anchor.subtract(encounter.portalPosition).horizontal().normalize();
			Vec3 exitTarget = encounter.portalPosition.add(exitDirection.scale(PORTAL_EXIT_DISTANCE));
			Vec3 movement = exitTarget.subtract(unknown.position());
			if (movement.lengthSqr() > 1.0E-8) {
				unknown.setPos(unknown.position().add(movement.normalize().scale(
						Math.min(PORTAL_EXIT_DISTANCE / PORTAL_EXIT_TICKS, movement.length()))));
				facePlayer(unknown, player);
			}
			encounter.phaseTicks++;
			if (encounter.phaseTicks >= PORTAL_EXIT_TICKS) {
				unknown.setPos(exitTarget);
				unknown.setDeltaMovement(Vec3.ZERO);
				encounter.phase = Phase.DIALOGUE;
				encounter.phaseTicks = 0;
				encounter.elapsedTicks = 0;
				sendUnknownMessage(player, "хмм");
			}
		} else if (encounter.phase == Phase.APPROACH) {
			Vec3 towardPlayer = player.position().subtract(unknown.position());
			double distance = towardPlayer.length();
			if (distance > GRAB_DISTANCE) {
				double step = Math.min(APPROACH_STEP, distance - GRAB_DISTANCE);
				Vec3 movement = towardPlayer.scale(step / distance);
				if (level.noCollision(unknown, unknown.getBoundingBox().move(movement))) {
					unknown.setPos(unknown.position().add(movement));
					facePlayer(unknown, player);
				}
			}
			if (encounter.finalLeadDelivered
					&& encounter.elapsedTicks >= 200 + POST_FINAL_LEAD_DELAY_TICKS
					&& unknown.position().distanceTo(player.position()) <= GRAB_TRIGGER_DISTANCE) {
				unknown.getNavigation().stop();
				unknown.setNoAi(true);
				unknown.setNoGravity(true);
				unknown.setDeltaMovement(Vec3.ZERO);
				unknown.startGrabbing();
				player.setNoGravity(true);
				sendUnknownMessage(player, "ты можешь помешать мне в будущем");
				encounter.phase = Phase.GRABBING;
				encounter.phaseTicks = 0;
			}
		} else if (encounter.phase == Phase.GRABBING) {
			encounter.phaseTicks++;
			if (encounter.phaseTicks >= 20) {
				beginFinalMessage(player);
				encounter.phase = Phase.TYPING;
				encounter.phaseTicks = 0;
			}
		} else if (encounter.phase == Phase.TYPING) {
			encounter.phaseTicks++;
			if (encounter.phaseTicks >= FINAL_MESSAGE_TICKS) {
				finishEncounter(server, level, player, unknown, encounter);
			}
		}
		return false;
	}

	private static boolean tickDeparture(ServerLevel level, ServerPlayer player, KirillSecondEntity unknown,
			Encounter encounter) {
		if (encounter.phase == Phase.DEPARTURE_PORTAL) {
			renderPortal(level, encounter);
			encounter.phaseTicks++;
			if (encounter.phaseTicks >= DEPARTURE_PORTAL_OPEN_TICKS) {
				encounter.phase = Phase.RETURNING;
				encounter.phaseTicks = 0;
				encounter.departureStartPosition = unknown.position();
			}
		} else if (encounter.phase == Phase.RETURNING) {
			renderPortal(level, encounter);
			encounter.phaseTicks++;
			double progress = Mth.clamp(encounter.phaseTicks / (double) PORTAL_RETURN_TICKS, 0.0, 1.0);
			double easedProgress = progress * progress * (3.0 - 2.0 * progress);
			unknown.setPos(encounter.departureStartPosition.lerp(encounter.portalPosition, easedProgress));
			unknown.setDeltaMovement(Vec3.ZERO);
			facePortal(unknown, encounter.portalPosition);
			if (encounter.phaseTicks >= PORTAL_RETURN_TICKS) {
				unknown.setPos(encounter.portalPosition);
				unknown.setPortalHidden(true);
				sendRemoveUnknown(player, unknown.getId(), unknown.getUUID());
				unknown.discard();
				encounter.phase = Phase.PORTAL_CLOSING;
				encounter.phaseTicks = 0;
			}
		} else if (encounter.phase == Phase.PORTAL_CLOSING) {
			renderPortal(level, encounter);
			encounter.phaseTicks++;
			if (encounter.phaseTicks >= PORTAL_CLOSE_TICKS) {
				return true;
			}
		}
		return false;
	}

	private static void renderPortal(ServerLevel level, Encounter encounter) {
		Vec3 center = encounter.portalPosition.add(0.0, 1.15, 0.0);
		Vec3 towardPlayer = encounter.anchor.subtract(encounter.portalPosition).horizontal().normalize();
		Vec3 right = new Vec3(-towardPlayer.z, 0.0, towardPlayer.x);
		int animationTick = encounter.phaseTicks;
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z,
				14, 0.5, 0.85, 0.12, 0.03);
		if ((animationTick & 1) == 0) {
			for (int i = 0; i < 12; i++) {
				double angle = Math.PI * 2.0 * i / 12.0 + animationTick * 0.09;
				Vec3 point = center.add(right.scale(Math.cos(angle) * 0.72))
						.add(0.0, Math.sin(angle) * 1.08, 0.0);
				level.sendParticles(i % 3 == 0 ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.REVERSE_PORTAL,
						point.x, point.y, point.z, 1, 0.025, 0.025, 0.025, 0.01);
			}
		}
		if (animationTick % 3 == 0) {
			for (int i = 0; i < 4; i++) {
				double sideways = (level.getRandom().nextDouble() - 0.5) * 1.65;
				double vertical = level.getRandom().nextDouble() * 2.2 - 1.1;
				double depth = (level.getRandom().nextDouble() - 0.5) * 0.3;
				Vec3 glitch = center.add(right.scale(sideways)).add(towardPlayer.scale(depth))
						.add(0.0, vertical, 0.0);
				level.sendParticles(ParticleTypes.ELECTRIC_SPARK, glitch.x, glitch.y, glitch.z,
						2, 0.16, 0.015, 0.16, 0.02);
			}
		}
	}

	private static void finishEncounter(MinecraftServer server, ServerLevel level, ServerPlayer player,
			KirillSecondEntity unknown, Encounter encounter) {
		restorePlayerState(player, encounter);
		DamageSource damage = new DamageSource(level.registryAccess()
				.lookupOrThrow(Registries.DAMAGE_TYPE)
				.getOrThrow(LOST_IN_TIME_DAMAGE));
		for (int attempt = 0; attempt < 3 && player.isAlive(); attempt++) {
			player.hurtServer(level, damage, Float.MAX_VALUE);
		}
		if (player.isAlive()) {
			player.setHealth(0.0f);
			player.die(damage);
		}
		var advancement = server.getAdvancements().get(HbkMod.id("unknown"));
		if (advancement != null) {
			player.getAdvancements().award(advancement, "unlock");
		}
		unknown.stopGrabbing();
		unknown.setPortalHidden(false);
		unknown.setDeltaMovement(Vec3.ZERO);
		encounter.departureStartPosition = unknown.position();
		encounter.phase = Phase.DEPARTURE_PORTAL;
		encounter.phaseTicks = 0;
		sendClientState(player, -1, false);
	}

	private static boolean canTriggerNaturally(ServerPlayer player) {
		if (!player.isAlive() || player.isCreative() || player.isSpectator()
				|| player.isPassenger() || !player.onGround()) {
			return false;
		}
		Input input = player.getLastClientInput();
		boolean hasWalkingInput = input.forward() || input.backward() || input.left() || input.right();
		return hasWalkingInput && player.getKnownMovement().horizontalDistanceSqr() > 1.0E-5;
	}

	private static Vec3 horizontalLook(ServerPlayer player) {
		Vec3 look = player.getLookAngle().horizontal();
		if (look.lengthSqr() < 1.0E-6) {
			look = Vec3.directionFromRotation(0.0f, player.getYRot()).horizontal();
		}
		return look.normalize();
	}

	private static Vec3 lockedPlayerPosition(Encounter encounter) {
		double liftProgress = switch (encounter.phase) {
			case GRABBING -> Math.min(1.0, encounter.phaseTicks / (double) PLAYER_LIFT_TICKS);
			case TYPING -> 1.0;
			default -> 0.0;
		};
		return encounter.anchor.add(0.0, PLAYER_LIFT_HEIGHT * liftProgress, 0.0);
	}

	private static void lockCameraOnUnknown(ServerPlayer player, KirillSecondEntity unknown) {
		player.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(
				unknown.getX(), unknown.getEyeY() + CAMERA_TARGET_VERTICAL_OFFSET, unknown.getZ()));
	}

	private static void facePlayer(KirillSecondEntity unknown, ServerPlayer player) {
		Vec3 offset = player.getEyePosition().subtract(unknown.getEyePosition());
		double horizontalDistance = Math.sqrt(offset.x * offset.x + offset.z * offset.z);
		float yaw = (float) (Mth.atan2(offset.z, offset.x) * Mth.RAD_TO_DEG) - 90.0f;
		float pitch = (float) -(Mth.atan2(offset.y, horizontalDistance) * Mth.RAD_TO_DEG);
		unknown.setYRot(yaw);
		unknown.setYHeadRot(yaw);
		unknown.setYBodyRot(yaw);
		unknown.setXRot(pitch);
	}

	private static void facePortal(KirillSecondEntity unknown, Vec3 portalPosition) {
		Vec3 offset = portalPosition.subtract(unknown.position());
		if (offset.horizontalDistanceSqr() < 1.0E-8) {
			return;
		}
		float yaw = (float) (Mth.atan2(offset.z, offset.x) * Mth.RAD_TO_DEG) - 90.0f;
		unknown.setYRot(yaw);
		unknown.setYHeadRot(yaw);
		unknown.setYBodyRot(yaw);
		unknown.setXRot(0.0f);
	}

	private static boolean hasClearSpace(ServerLevel level, ServerPlayer player, Vec3 forward,
			Vec3 spawnPosition) {
		for (double distance = 0.75; distance <= SPAWN_DISTANCE; distance += 0.5) {
			Vec3 sample = player.position().add(forward.scale(distance));
			if (!level.noBlockCollision(null, ModEntityTypes.KIRILL_V2.getDimensions().makeBoundingBox(sample))) {
				return false;
			}
		}
		return level.getWorldBorder().isWithinBounds(
				ModEntityTypes.KIRILL_V2.getDimensions().makeBoundingBox(spawnPosition));
	}

	private static void removeUnknown(MinecraftServer server, Encounter encounter) {
		ServerLevel level = server.getLevel(encounter.dimension);
		if (level != null) {
			Entity unknown = level.getEntityInAnyDimension(encounter.unknownUuid);
			if (unknown != null) {
				unknown.discard();
			}
		}
	}

	private static void removeOrphanedUnknowns(ServerPlayer player) {
		String playerTag = KirillSecondEntity.encounterTag(player.getUUID());
		for (ServerLevel level : player.level().getServer().getAllLevels()) {
			for (KirillSecondEntity unknown : level.getEntities(ModEntityTypes.KIRILL_V2, candidate -> {
				boolean belongsToPlayer = candidate.entityTags().contains(playerTag);
				boolean belongsToAnotherEncounter = candidate.entityTags().stream()
						.anyMatch(tag -> tag.startsWith(KirillSecondEntity.ENCOUNTER_TAG_PREFIX));
				boolean legacyOrphan = !belongsToAnotherEncounter && candidate.isNoAi()
						&& candidate.isInvulnerable() && candidate.isNoGravity();
				return belongsToPlayer || legacyOrphan;
			})) {
				if (level == player.level()) {
					sendRemoveUnknown(player, unknown.getId(), unknown.getUUID());
				}
				unknown.discard();
			}
		}
	}

	private static void restorePlayerState(ServerPlayer player, Encounter encounter) {
		player.setInvulnerable(encounter.wasInvulnerable);
		player.setNoGravity(encounter.wasNoGravity);
		player.setDeltaMovement(Vec3.ZERO);
	}

	private static void sendUnknownMessage(ServerPlayer player, String message) {
		player.sendSystemMessage(Component.literal("<???> " + message));
	}

	private static void beginFinalMessage(ServerPlayer player) {
		if (ServerPlayNetworking.canSend(player, ModNetworking.UnknownTypingPayload.TYPE)) {
			ServerPlayNetworking.send(player, new ModNetworking.UnknownTypingPayload(FINAL_MESSAGE));
		} else {
			sendUnknownMessage(player, FINAL_MESSAGE);
		}
	}

	private static void sendClientState(ServerPlayer player, int entityId, boolean active) {
		if (ServerPlayNetworking.canSend(player, ModNetworking.UnknownEncounterPayload.TYPE)) {
			ServerPlayNetworking.send(player, new ModNetworking.UnknownEncounterPayload(entityId, active));
		}
	}

	private static void sendMusicState(ServerPlayer player, boolean playing) {
		if (ServerPlayNetworking.canSend(player, ModNetworking.UnknownMusicPayload.TYPE)) {
			ServerPlayNetworking.send(player, new ModNetworking.UnknownMusicPayload(playing));
		}
	}

	private static void sendRemoveUnknown(ServerPlayer player, int entityId, UUID entityUuid) {
		if (ServerPlayNetworking.canSend(player, ModNetworking.UnknownRemovePayload.TYPE)) {
			ServerPlayNetworking.send(player, new ModNetworking.UnknownRemovePayload(entityId, entityUuid));
		}
	}

	public enum StartResult {
		STARTED,
		BLOCKED,
		ALREADY_ACTIVE,
		UNAVAILABLE
	}

	private enum Phase {
		PORTAL,
		EMERGING,
		DIALOGUE,
		APPROACH,
		GRABBING,
		TYPING,
		DEPARTURE_PORTAL,
		RETURNING,
		PORTAL_CLOSING
	}

	private static final class Encounter {
		private final ResourceKey<Level> dimension;
		private final Vec3 anchor;
		private final Vec3 portalPosition;
		private final UUID unknownUuid;
		private final int unknownEntityId;
		private final boolean wasInvulnerable;
		private final boolean wasNoGravity;
		private int elapsedTicks;
		private int phaseTicks;
		private boolean finalLeadDelivered;
		private Vec3 departureStartPosition;
		private Phase phase = Phase.PORTAL;

		private Encounter(ResourceKey<Level> dimension, Vec3 anchor, Vec3 portalPosition, UUID unknownUuid,
				int unknownEntityId,
				boolean wasInvulnerable, boolean wasNoGravity) {
			this.dimension = dimension;
			this.anchor = anchor;
			this.portalPosition = portalPosition;
			this.unknownUuid = unknownUuid;
			this.unknownEntityId = unknownEntityId;
			this.wasInvulnerable = wasInvulnerable;
			this.wasNoGravity = wasNoGravity;
		}

		private boolean isDeparting() {
			return this.phase == Phase.DEPARTURE_PORTAL
					|| this.phase == Phase.RETURNING
					|| this.phase == Phase.PORTAL_CLOSING;
		}
	}
}

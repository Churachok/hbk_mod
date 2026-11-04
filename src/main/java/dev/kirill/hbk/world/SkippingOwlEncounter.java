package dev.kirill.hbk.world;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.entity.SkippingOwlEntity;
import dev.kirill.hbk.network.SkippingOwlScenePayload;
import dev.kirill.hbk.registry.ModEntityTypes;
import dev.kirill.hbk.registry.ModEffects;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** An escalating curse whose final hit waits for the card reveal and laughter to finish. */
public final class SkippingOwlEncounter {
	/** The accelerating jump prelude, not the lifetime of the final card reveal. */
	public static final int DURATION_TICKS = 360;
	public static final int CARD_HOLD_TICKS = 40;
	public static final int LAUGH_DURATION_TICKS = 160;
	public static final int FINALE_TICKS = DURATION_TICKS + CARD_HOLD_TICKS + LAUGH_DURATION_TICKS;
	public static final int CLIENT_FINISH_GRACE_TICKS = 100;
	public static final int MAX_DURATION_TICKS = FINALE_TICKS + CLIENT_FINISH_GRACE_TICKS;
	public static final int OMEN_TICKS = 240;
	public static final int PULSE_INTERVAL_TICKS = 40;
	/** Prelude waves are visual only: the owl must not kill before its laughter ends. */
	public static final float PULSE_DAMAGE = 0.0f;
	public static final float FINALE_DAMAGE = 1000.0f;
	public static final ResourceKey<DamageType> DAMAGE_TYPE = ResourceKey.create(
			Registries.DAMAGE_TYPE, HbkMod.id("skipping_owl"));
	public static final double WORLD_TRIGGER_DISTANCE = 8.0;
	public static final double MAX_OWNER_DISTANCE = 40.0;
	public static final double CLEARANCE_WIDTH = 2.5;
	public static final double CLEARANCE_HEIGHT = 2.7;
	private static final float[] ANGLES = {0, 30, -30, 60, -60, 90, -90};
	private static final int[] HEIGHT_OFFSETS = {0, 1, -1, 2, -2};
	private static final Map<UUID, Encounter> ACTIVE = new HashMap<>();

	private SkippingOwlEncounter() { }

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(SkippingOwlEncounter::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			ACTIVE.values().forEach(encounter -> encounter.owl.discard());
			ACTIVE.clear();
		});
	}

	private static void tick(MinecraftServer server) {
		cleanupEncounters(server, server.getTickCount());
	}

	static boolean canTriggerNaturally(ServerPlayer player) {
		GameType mode = player.gameMode.getGameModeForPlayer();
		return (mode == GameType.SURVIVAL || mode == GameType.ADVENTURE)
				&& player.isAlive() && !player.isPassenger()
				&& player.level().dimension().equals(Level.OVERWORLD) && !UnknownEncounter.isActive(player);
	}

	/** An existing world inhabitant starts its scene in place; no random per-player event is rolled. */
	public static boolean tryStartWorldEncounter(ServerPlayer player, SkippingOwlEntity owl) {
		if (!canTriggerNaturally(player) || owl.isRemoved() || !owl.isAlive() || !owl.isWaitingForPlayer()
				|| owl.level() != player.level()
				|| player.distanceToSqr(owl) > WORLD_TRIGGER_DISTANCE * WORLD_TRIGGER_DISTANCE) return false;
		ServerLevel level = player.level();
		Vec3 eyes = player.getEyePosition();
		Vec3 owlChest = owl.position().add(0.0, 1.0, 0.0);
		if (!loadedArea(level, new AABB(eyes, owlChest).inflate(0.01))
				|| level.clip(new ClipContext(eyes, owlChest, ClipContext.Block.COLLIDER,
						ClipContext.Fluid.NONE, player)).getType() != HitResult.Type.MISS) return false;
		cleanupEncounters(level.getServer(), level.getServer().getTickCount());
		if (isActive(player) || !owl.isWaitingForPlayer()) return false;
		begin(player, owl);
		return true;
	}

	/** Operators may still stage an immediate scene in front of a player, including in creative. */
	public static StartResult start(ServerPlayer player) {
		if (!player.isAlive() || player.isSpectator() || player.isPassenger() || UnknownEncounter.isActive(player)) {
			return StartResult.UNAVAILABLE;
		}
		// Retire stale props before allowing an immediate explicit replacement.
		cleanupEncounters(player.level().getServer(), player.level().getServer().getTickCount());
		if (isActive(player)) return StartResult.ALREADY_ACTIVE;
		ServerLevel level = player.level();
		var owl = ModEntityTypes.SKIPPING_OWL.create(level, EntitySpawnReason.EVENT);
		if (owl == null) return StartResult.UNAVAILABLE;
		Vec3 position = findSafePosition(player);
		if (position == null) {
			owl.discard();
			return StartResult.BLOCKED;
		}
		owl.snapTo(position.x, position.y, position.z, 0.0f, 0.0f);
		if (!level.addFreshEntity(owl)) {
			owl.discard();
			return StartResult.BLOCKED;
		}
		begin(player, owl);
		return StartResult.STARTED;
	}

	private static void begin(ServerPlayer player, SkippingOwlEntity owl) {
		owl.configureOwner(player);
		var encounter = new Encounter(owl, player.level().getServer().getTickCount(), player.level().getGameTime());
		ACTIVE.put(player.getUUID(), encounter);
		sendSnapshot(encounter, player, true);
		player.sendSystemMessage(Component.translatable("message.hbk.skipping_owl.intro"));
	}

	/** Only this player's current owl can be acknowledged; client packets never bypass the server deadline. */
	public static boolean acknowledgeLaughFinished(ServerPlayer player, UUID encounterId) {
		var encounter = ACTIVE.get(player.getUUID());
		if (encounter == null || !encounter.owl.getUUID().equals(encounterId)
				|| encounter.owl.isRemoved() || !player.isAlive() || player.isSpectator()
				|| player.level() != encounter.owl.level()) return false;
		encounter.laughFinishedAcknowledged = true;
		return true;
	}

	static boolean isFinaleReady(long elapsed, boolean waitsForClient, boolean laughFinished) {
		return elapsed >= FINALE_TICKS
				&& (!waitsForClient || laughFinished || elapsed >= MAX_DURATION_TICKS);
	}

	/** Integral of a linear acceleration from one to three rope turns per second. */
	public static float jumpCycles(float ageTicks) {
		float age = Mth.clamp(ageTicks, 0.0f, DURATION_TICKS);
		return age / 20.0f + age * age / (20.0f * DURATION_TICKS);
	}

	public static boolean isActive(ServerPlayer player) {
		return activeOwl(player) != null;
	}

	public static SkippingOwlEntity activeOwl(ServerPlayer player) {
		var encounter = ACTIVE.get(player.getUUID());
		return encounter != null && !encounter.owl.isRemoved() ? encounter.owl : null;
	}

	/** Runs on the global server clock, including when the owl's entity section is no longer ticking. */
	static void cleanupEncounters(MinecraftServer server, long now) {
		var iterator = ACTIVE.entrySet().iterator();
		while (iterator.hasNext()) {
			var entry = iterator.next();
			var encounter = entry.getValue();
			var owl = encounter.owl;
			var owner = server.getPlayerList().getPlayer(entry.getKey());
			ServerLevel level = (ServerLevel) owl.level();
			long elapsed = now - encounter.startedAt;
			if (owl.isRemoved() || !owl.isAlive() || owner == null || !owner.isAlive() || owner.isSpectator()
					|| owner.level() != level || owner.distanceToSqr(owl) > MAX_OWNER_DISTANCE * MAX_OWNER_DISTANCE
					|| UnknownEncounter.isActive(owner) || !level.hasChunkAt(owl.blockPosition())) {
				finish(encounter);
				iterator.remove();
				continue;
			}
			if (isFinaleReady(elapsed, encounter.waitForClientAck, encounter.laughFinishedAcknowledged)) {
				// Retire before dealing damage: a totem or Hunter's Instinct gets exactly one chance
				// to save the owner, with no second hit or leftover encounter on the next tick.
				finish(encounter);
				iterator.remove();
				strike(level, owl, owner, FINALE_DAMAGE, true);
				continue;
			}
			if (elapsed - encounter.lastSnapshotAt >= 20) {
				encounter.lastSnapshotAt = elapsed;
				sendSnapshot(encounter, owner, true);
			}
			// The last jump holds the fully opaque card. No more jumps, waves or percussion
			// are emitted during its two-second silence or the following eight-second laugh.
			if (elapsed >= DURATION_TICKS) continue;
			if (!encounter.omenDelivered && elapsed >= OMEN_TICKS) {
				encounter.omenDelivered = true;
				owner.sendSystemMessage(Component.translatable("message.hbk.skipping_owl.omen"));
				if (!owner.isCreative()) owner.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60));
				if (owner.connection != null) {
					owner.connection.send(new ClientboundSoundPacket(SoundEvents.AMBIENT_CAVE, SoundSource.AMBIENT,
							owner.getX(), owner.getY(), owner.getZ(), 0.3f, 0.9f, owner.getRandom().nextLong()));
				}
			}
			if (elapsed >= OMEN_TICKS) {
				int pulse = (int) ((elapsed - OMEN_TICKS) / PULSE_INTERVAL_TICKS);
				if (pulse > encounter.lastPulse) {
					// A delayed cleanup skips missed pulses instead of bursting several at once.
					encounter.lastPulse = pulse;
					strike(level, owl, owner, PULSE_DAMAGE, false);
				}
			}
			int cycle = Mth.floor(jumpCycles(elapsed));
			if (cycle > encounter.lastSoundCycle) {
				encounter.lastSoundCycle = cycle;
				level.playSound(null, owl.getX(), owl.getY(), owl.getZ(), SoundEvents.NOTE_BLOCK_HAT,
						SoundSource.NEUTRAL, 0.25f, 1.1f + 0.5f * elapsed / DURATION_TICKS);
			}
		}
	}

	private static void strike(ServerLevel level, SkippingOwlEntity owl, ServerPlayer owner,
			float damageAmount, boolean finale) {
		// This is only a visual/sound wave, not an explosion: no terrain, drops or bystander damage.
		level.playSound(null, owl.getX(), owl.getY(), owl.getZ(), SoundEvents.WARDEN_SONIC_BOOM,
				SoundSource.HOSTILE, finale ? 1.2f : 0.6f, finale ? 0.6f : 1.2f);
		for (int i = 0; i < 32; i++) {
			double angle = i * Math.PI * 2.0 / 32;
			level.sendParticles(finale ? ParticleTypes.SOUL : ParticleTypes.WITCH,
					owl.getX() + Math.cos(angle) * 1.4, owl.getY() + 0.15,
					owl.getZ() + Math.sin(angle) * 1.4, 1, 0.0, 0.05, 0.0, 0.02);
		}
		level.sendParticles(ParticleTypes.SONIC_BOOM, owner.getX(), owner.getY() + 0.8, owner.getZ(),
				1, 0.0, 0.0, 0.0, 0.0);
		GameType mode = owner.gameMode.getGameModeForPlayer();
		if (damageAmount > 0 && !owner.hasEffect(ModEffects.HAND_IMMORTALITY)
				&& (mode == GameType.SURVIVAL || mode == GameType.ADVENTURE)) {
			DamageSource damage = new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
					.getOrThrow(DAMAGE_TYPE));
			owner.hurtServer(level, damage, damageAmount);
		}
	}

	private static void finish(Encounter encounter) {
		var owl = encounter.owl;
		ServerLevel level = (ServerLevel) owl.level();
		var owner = level.getServer().getPlayerList().getPlayer(owl.getOwnerUUID());
		if (owner != null) sendSnapshot(encounter, owner, false);
		if (!owl.isRemoved() && level.hasChunkAt(owl.blockPosition())) {
			level.sendParticles(ParticleTypes.POOF, owl.getX(), owl.getY() + 0.75, owl.getZ(),
					18, 0.5, 0.6, 0.5, 0.02);
		}
		// Keep the cached reference: an unsaved entity can remain in a hidden section
		// after unload, where an ordinary live-entity UUID lookup no longer finds it.
		owl.discard();
	}

	private static void sendSnapshot(Encounter encounter, ServerPlayer owner, boolean active) {
		boolean sent = new SkippingOwlScenePayload(encounter.owl.getUUID(), encounter.owl.getId(),
				encounter.startedAtGameTime, active).sendTo(owner);
		if (active && sent) encounter.waitForClientAck = true;
	}

	private static Vec3 findSafePosition(ServerPlayer player) {
		ServerLevel level = player.level();
		for (double distance : new double[]{5.0, 4.0, 6.0}) {
			for (float angle : ANGLES) {
				float yaw = (player.getYRot() + angle) * Mth.DEG_TO_RAD;
				double x = player.getX() - Mth.sin(yaw) * distance;
				double z = player.getZ() + Mth.cos(yaw) * distance;
				for (int height : HEIGHT_OFFSETS) {
					Vec3 position = new Vec3(x, Mth.floor(player.getY()) + height, z);
					if (!isSafeWorldPosition(level, position)) continue;
					Vec3 eyes = player.getEyePosition();
					Vec3 owlChest = position.add(0.0, 1.0, 0.0);
					// Clip can query intervening chunks too, so prove the complete sight corridor
					// is loaded before tracing it, rather than merely checking the destination.
					if (!loadedArea(level, new AABB(eyes, owlChest).inflate(0.01))
							|| level.clip(new ClipContext(eyes, owlChest, ClipContext.Block.COLLIDER,
									ClipContext.Fluid.NONE, player)).getType() != HitResult.Type.MISS) continue;
					return position;
				}
			}
		}
		return null;
	}

	/** Shared by ordinary world spawning and the operator command; never loads candidate chunks. */
	public static boolean isSafeWorldPosition(LevelAccessor level, Vec3 feet) {
		AABB clearance = new AABB(feet.x - CLEARANCE_WIDTH / 2, feet.y, feet.z - CLEARANCE_WIDTH / 2,
				feet.x + CLEARANCE_WIDTH / 2, feet.y + CLEARANCE_HEIGHT, feet.z + CLEARANCE_WIDTH / 2);
		return feet.y > level.getMinY() && clearance.maxY <= level.getMaxY()
				// Native collision iteration also samples one block beyond the queried box.
				&& loadedArea(level, clearance.inflate(1.0)) && level.getWorldBorder().isWithinBounds(clearance)
				&& safeGround(level, feet) && clearAndDry(level, clearance) && level.noCollision(null, clearance);
	}

	private static boolean loadedArea(LevelAccessor level, AABB box) {
		int minChunkX = Mth.floor(box.minX) >> 4;
		int maxChunkX = Mth.floor(box.maxX - 1.0E-7) >> 4;
		int minChunkZ = Mth.floor(box.minZ) >> 4;
		int maxChunkZ = Mth.floor(box.maxZ - 1.0E-7) >> 4;
		for (int x = minChunkX; x <= maxChunkX; x++) {
			for (int z = minChunkZ; z <= maxChunkZ; z++) {
				if (level instanceof ServerLevel serverLevel) {
					if (serverLevel.getChunkSource().getChunkNow(x, z) == null) return false;
				} else if (!level.hasChunk(x, z)) return false;
			}
		}
		return true;
	}

	private static boolean safeGround(LevelAccessor level, Vec3 feet) {
		for (double x : new double[]{-0.45, 0.0, 0.45}) {
			for (double z : new double[]{-0.45, 0.0, 0.45}) {
				BlockPos groundPos = BlockPos.containing(feet.x + x, feet.y - 0.01, feet.z + z);
				BlockState ground = level.getBlockState(groundPos);
				if (!ground.isFaceSturdy(level, groundPos, Direction.UP) || hazardous(ground)
						|| !ground.getFluidState().isEmpty()) return false;
			}
		}
		return true;
	}

	private static boolean clearAndDry(LevelAccessor level, AABB box) {
		for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ),
				BlockPos.containing(box.maxX - 1.0E-7, box.maxY - 1.0E-7, box.maxZ - 1.0E-7))) {
			BlockState state = level.getBlockState(pos);
			if (hazardous(state) || !state.getFluidState().isEmpty()) return false;
		}
		return true;
	}

	private static boolean hazardous(BlockState state) {
		return state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)
				|| state.is(Blocks.CACTUS) || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.WITHER_ROSE)
				|| state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE) || state.is(Blocks.POWDER_SNOW)
				|| state.is(Blocks.POINTED_DRIPSTONE) || state.is(Blocks.END_PORTAL)
				|| state.is(Blocks.END_GATEWAY) || state.is(Blocks.NETHER_PORTAL);
	}

	public enum StartResult { STARTED, BLOCKED, ALREADY_ACTIVE, UNAVAILABLE }

	private static final class Encounter {
		private final SkippingOwlEntity owl;
		private final long startedAt;
		private final long startedAtGameTime;
		private boolean omenDelivered;
		private boolean waitForClientAck;
		private boolean laughFinishedAcknowledged;
		private long lastSnapshotAt;
		private int lastPulse = -1;
		private int lastSoundCycle;

		private Encounter(SkippingOwlEntity owl, long startedAt, long startedAtGameTime) {
			this.owl = owl;
			this.startedAt = startedAt;
			this.startedAtGameTime = startedAtGameTime;
		}
	}
}

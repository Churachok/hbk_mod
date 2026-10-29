package dev.kirill.hbk.entity;

import dev.kirill.hbk.world.SkippingOwlEncounter;
import dev.kirill.hbk.world.UnknownEncounter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** A stationary world inhabitant which becomes a temporary curse when a player approaches. */
public final class SkippingOwlEntity extends PathfinderMob {
	public static final double ACTIVATION_DISTANCE = SkippingOwlEncounter.WORLD_TRIGGER_DISTANCE;
	private static final EntityDataAccessor<Long> START_TIME = SynchedEntityData.defineId(
			SkippingOwlEntity.class, EntityDataSerializers.LONG);
	private UUID ownerUUID;

	public SkippingOwlEntity(EntityType<? extends SkippingOwlEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
		this.setNoAi(true);
		this.setNoGravity(true);
		this.setInvulnerable(true);
		this.setSilent(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 20.0)
				.add(Attributes.MOVEMENT_SPEED, 0.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
	}

	/** Native creature spawning supplies the cadence and cap; there is no additional random roll. */
	public static boolean checkSpawnRules(EntityType<? extends SkippingOwlEntity> type, ServerLevelAccessor level,
			EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		return level.getLevel().dimension().equals(Level.OVERWORLD)
				&& SkippingOwlEncounter.isSafeWorldPosition(level, Vec3.atBottomCenterOf(pos))
				&& Mob.checkMobSpawnRules(type, level, reason, pos, random);
	}

	@Override
	public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason reason) {
		// Initial chunk generation can place the mob between block centers after its static predicate.
		return super.checkSpawnRules(level, reason) && SkippingOwlEncounter.isSafeWorldPosition(level, this.position());
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(START_TIME, -1L);
	}

	@Override
	protected void registerGoals() {
		// Skipping is a client-side animation; neither navigation nor random looking may move the prop.
	}

	public void configureOwner(ServerPlayer player) {
		this.ownerUUID = player.getUUID();
		this.entityData.set(START_TIME, this.level().getGameTime());
		Vec3 direction = player.getEyePosition().subtract(this.getEyePosition());
		float yaw = (float) (Mth.atan2(direction.z, direction.x) * Mth.RAD_TO_DEG) - 90.0f;
		float pitch = (float) -(Mth.atan2(direction.y, direction.horizontalDistance()) * Mth.RAD_TO_DEG);
		this.setYRot(yaw);
		this.setYBodyRot(yaw);
		this.setYHeadRot(yaw);
		this.setXRot(pitch);
		this.setOldRot();
	}

	public UUID getOwnerUUID() {
		return this.ownerUUID;
	}

	public boolean isWaitingForPlayer() {
		return this.entityData.get(START_TIME) < 0;
	}

	public ServerPlayer getEncounterOwner() {
		return this.ownerUUID != null && this.level() instanceof ServerLevel level
				? level.getServer().getPlayerList().getPlayer(this.ownerUUID) : null;
	}

	/** World time keeps every observer in phase, even when they start tracking the owl late. */
	public float getPerformanceAge(float partialTick) {
		long startedAt = this.entityData.get(START_TIME);
		return startedAt < 0 ? 0.0f : Mth.clamp((float) (this.level().getGameTime() - startedAt) + partialTick,
				0.0f, SkippingOwlEncounter.DURATION_TICKS);
	}

	@Override
	public void tick() {
		this.setDeltaMovement(Vec3.ZERO);
		super.tick();
		this.setDeltaMovement(Vec3.ZERO);
		if (!this.isRemoved() && this.isWaitingForPlayer() && this.tickCount % 10 == 0
				&& this.level() instanceof ServerLevel level) {
			ServerPlayer nearest = null;
			double bestDistance = ACTIVATION_DISTANCE * ACTIVATION_DISTANCE;
			for (var player : level.players()) {
				GameType mode = player.gameMode.getGameModeForPlayer();
				double distance = this.distanceToSqr(player);
				if ((mode != GameType.SURVIVAL && mode != GameType.ADVENTURE)
						|| !player.isAlive() || player.isSpectator() || player.isPassenger()
						|| distance > bestDistance || UnknownEncounter.isActive(player)
						|| SkippingOwlEncounter.isActive(player) || !this.hasClearSight(level, player)) continue;
				nearest = player;
				bestDistance = distance;
			}
			if (nearest != null) SkippingOwlEncounter.tryStartWorldEncounter(nearest, this);
		}
	}

	private boolean hasClearSight(ServerLevel level, ServerPlayer player) {
		Vec3 from = this.getEyePosition();
		Vec3 to = player.getEyePosition();
		AABB sight = new AABB(from, to).inflate(0.01);
		for (int x = Mth.floor(sight.minX) >> 4; x <= Mth.floor(sight.maxX) >> 4; x++) {
			for (int z = Mth.floor(sight.minZ) >> 4; z <= Mth.floor(sight.maxZ) >> 4; z++) {
				if (level.getChunkSource().getChunkNow(x, z) == null) return false;
			}
		}
		return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS;
	}

	// Do not mark waiting owls PersistenceRequired: they must still count against the creature cap.
	@Override public boolean shouldBeSaved() { return this.isWaitingForPlayer() && super.shouldBeSaved(); }
	@Override public boolean removeWhenFarAway(double distance) { return false; }
	@Override public boolean isPickable() { return false; }
	@Override public boolean isAttackable() { return false; }
	@Override public boolean canBeHitByProjectile() { return false; }
	@Override public boolean isPushable() { return false; }
	@Override public boolean canBeLeashed() { return false; }
	@Override public boolean canAttack(LivingEntity target) { return false; }
	@Override public boolean skipAttackInteraction(Entity attacker) { return true; }
	@Override public boolean isInvulnerableTo(ServerLevel level, DamageSource source) { return true; }
	@Override public boolean hurtServer(ServerLevel level, DamageSource source, float damage) { return false; }
	@Override protected boolean shouldDropLoot(ServerLevel level) { return false; }
	@Override public boolean shouldDropExperience() { return false; }
	@Override public void push(Entity other) { }
	@Override public void push(Vec3 movement) { }
	@Override public void push(double x, double y, double z) { }
}

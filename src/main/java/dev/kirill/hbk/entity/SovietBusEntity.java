package dev.kirill.hbk.entity;

import dev.kirill.hbk.world.SovietBusEvent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** Temporary, single-use encounter vehicle. Travel is a collision-free visual animation. */
public final class SovietBusEntity extends Entity {
	public static final int APPROACH_TICKS = 3 * 20;
	public static final double APPROACH_DISTANCE = 24;
	public static final int WAIT_TICKS = 20 * 20;
	public static final int DEPART_TICKS = 20 * 20;
	public static final int MAX_EVENT_TICKS = APPROACH_TICKS + WAIT_TICKS + DEPART_TICKS;
	public static final double DEPART_DISTANCE = 20 * 16;
	public static final float MODEL_SCALE = 1.0f;
	public static final double WIDTH = 2.9 * MODEL_SCALE;
	public static final double LENGTH = 6.7 * MODEL_SCALE;
	public static final double HEIGHT = 3.2 * MODEL_SCALE;
	private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(
			SovietBusEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> ANIMATION_TICKS = SynchedEntityData.defineId(
			SovietBusEntity.class, EntityDataSerializers.INT);
	private UUID owner;
	private int phaseTicks;
	private int startedAt;

	public SovietBusEntity(EntityType<? extends SovietBusEntity> type, Level level) {
		super(type, level);
		this.setNoGravity(true);
		this.noPhysics = true;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(PHASE, 0);
		builder.define(ANIMATION_TICKS, 0);
	}

	public void begin(UUID owner, Vec3 stop, Vec3 direction) {
		this.owner = owner;
		if (this.level() instanceof ServerLevel level) this.startedAt = level.getServer().getTickCount();
		this.setYRot((float) Math.toDegrees(Math.atan2(-direction.x, direction.z)));
		this.setPos(stop);
		this.setOldRot();
		this.phaseTicks = 0;
		this.entityData.set(PHASE, 0);
		this.entityData.set(ANIMATION_TICKS, 0);
	}

	public UUID getOwner() {
		return this.owner;
	}

	public boolean hasExpired(int serverTick) {
		return serverTick - this.startedAt >= MAX_EVENT_TICKS;
	}

	public boolean isWaiting() {
		return !this.isRemoved() && this.entityData.get(PHASE) == 1;
	}

	public boolean isApproaching() {
		return this.entityData.get(PHASE) == 0;
	}

	public void depart() {
		if (!isWaiting()) return;
		this.entityData.set(PHASE, 2);
		this.phaseTicks = 0;
		this.entityData.set(ANIMATION_TICKS, 0);
	}

	/** Quadratic ease-in: zero initial speed, 320 blocks covered in twenty seconds. */
	public static double departureDistance(double ticks) {
		double progress = Math.clamp(ticks / DEPART_TICKS, 0.0, 1.0);
		return DEPART_DISTANCE * progress * progress;
	}

	public Vec3 getDepartureOffset(float partialTick) {
		if (isWaiting() || isApproaching()) return Vec3.ZERO;
		double ticks = animationTicks(partialTick);
		if (ticks == 0) return Vec3.ZERO;
		return Vec3.directionFromRotation(0, this.getYRot()).horizontal().normalize()
				.scale(departureDistance(ticks));
	}

	private double animationTicks(float partialTick) {
		return Math.max(0, this.entityData.get(ANIMATION_TICKS) - 1 + partialTick);
	}

	public Vec3 getAnimationOffset(float partialTick) {
		if (!isApproaching()) return getDepartureOffset(partialTick);
		// Ease-out approach from 24 blocks away, braking gently into the stop.
		double remaining = 1.0 - Math.clamp(animationTicks(partialTick) / APPROACH_TICKS, 0.0, 1.0);
		return Vec3.directionFromRotation(0, this.getYRot()).horizontal().normalize()
				.scale(-APPROACH_DISTANCE * remaining * remaining);
	}

	public float getWheelRotation(float partialTick) {
		double distance = isApproaching() ? APPROACH_DISTANCE - getAnimationOffset(partialTick).length()
				: APPROACH_DISTANCE + getDepartureOffset(partialTick).length();
		return (float) (distance / (0.5 * MODEL_SCALE));
	}

	@Override
	public void tick() {
		super.tick();
		if (!(this.level() instanceof ServerLevel level)) {
			return;
		}
		// Keep the network entity in the stop's loaded chunk. Only its rendered position
		// travels: real movement would unload/despawn it well before twenty chunks.
		// Departure no longer depends on the owner remaining nearby after teleporting.
		if (!isWaiting() && !isApproaching()) {
			this.entityData.set(ANIMATION_TICKS, ++this.phaseTicks);
			if (this.phaseTicks >= DEPART_TICKS) this.discard();
			return;
		}
		ServerPlayer player = this.owner == null ? null : level.getServer().getPlayerList().getPlayer(this.owner);
		if (player == null || !player.isAlive() || player.level() != level
				|| player.distanceToSqr(this) > 96 * 96) {
			this.discard();
			return;
		}
		if (isApproaching()) {
			this.entityData.set(ANIMATION_TICKS, ++this.phaseTicks);
			if (this.phaseTicks >= APPROACH_TICKS) {
				this.entityData.set(PHASE, 1);
				this.entityData.set(ANIMATION_TICKS, 0);
				this.phaseTicks = 0;
			}
		} else if (++this.phaseTicks >= WAIT_TICKS) {
			this.depart();
		}
	}

	public static AABB footprint(Vec3 position, float yaw) {
		double angle = Math.toRadians(yaw);
		double halfX = (Math.abs(Math.cos(angle)) * WIDTH + Math.abs(Math.sin(angle)) * LENGTH) / 2;
		double halfZ = (Math.abs(Math.sin(angle)) * WIDTH + Math.abs(Math.cos(angle)) * LENGTH) / 2;
		return new AABB(position.x - halfX, position.y, position.z - halfZ,
				position.x + halfX, position.y + HEIGHT, position.z + halfZ);
	}

	@Override
	protected AABB makeBoundingBox(Vec3 position) {
		return footprint(position, this.getYRot());
	}

	@Override
	public boolean isPickable() {
		return isWaiting();
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand, Vec3 hitLocation) {
		if (!this.isWaiting() || player.isSpectator()) {
			return InteractionResult.PASS;
		}
		if (player instanceof ServerPlayer serverPlayer) {
			SovietBusEvent.board(serverPlayer, this);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	@Override
	public boolean shouldBeSaved() {
		return false; // An encounter must not leave abandoned vehicles after unloading/restarting.
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}
}

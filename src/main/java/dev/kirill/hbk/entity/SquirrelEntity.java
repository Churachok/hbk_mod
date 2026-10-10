package dev.kirill.hbk.entity;

import dev.kirill.hbk.registry.ModSounds;
import dev.kirill.hbk.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import java.util.EnumSet;

/** A temporary animal sent only to the player whose encounter created it. */
public final class SquirrelEntity extends PathfinderMob {
	public static final int LIFETIME_TICKS = 20 * 60;
	public static final int GIFT_HANDOVER_TICKS = 24;
	public static final int GOODBYE_DURATION_TICKS = 48;
	public static final int MAX_GOODBYE_APPROACH_TICKS = 20 * 15;
	private static final EntityDataAccessor<Integer> GIVING_TICKS = SynchedEntityData.defineId(
			SquirrelEntity.class, EntityDataSerializers.INT);
	private static final double FOLLOW_START_DISTANCE = 7.0;
	private static final double FOLLOW_STOP_DISTANCE = 6.0;
	private UUID ownerUuid;
	private int soundCooldown = 40;
	private float uprightAmount;
	private float previousUprightAmount;
	private boolean giftGiven;

	public SquirrelEntity(EntityType<? extends SquirrelEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
		this.setInvulnerable(true);
		// Suppress vanilla broadcast sounds; our calls are sent directly to the owner.
		this.setSilent(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 8.0)
				.add(Attributes.MOVEMENT_SPEED, 0.30)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
	}

	public void setEncounterOwner(ServerPlayer player) {
		this.ownerUuid = player.getUUID();
	}

	public ServerPlayer getEncounterOwner() {
		return this.level() instanceof ServerLevel level && this.ownerUuid != null
				? level.getServer().getPlayerList().getPlayer(this.ownerUuid) : null;
	}

	public float getUprightAmount(float tickProgress) {
		return Mth.lerp(tickProgress, this.previousUprightAmount, this.uprightAmount);
	}

	public boolean isSayingGoodbye() {
		return this.tickCount >= LIFETIME_TICKS;
	}

	public boolean isGivingBeer() {
		return this.entityData.get(GIVING_TICKS) >= 0;
	}

	public float getGiveProgress(float tickProgress) {
		return this.isGivingBeer() ? Mth.clamp((this.entityData.get(GIVING_TICKS) + tickProgress) / 12.0f, 0, 1) : 0;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(GIVING_TICKS, -1);
	}

	@Override
	public boolean broadcastToPlayer(ServerPlayer player) {
		return player.getUUID().equals(this.ownerUuid) && super.broadcastToPlayer(player);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new GoodbyeGoal());
		this.goalSelector.addGoal(2, new FollowEncounterOwnerGoal());
		this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.6) {
			@Override
			public boolean canUse() {
				ServerPlayer owner = getEncounterOwner();
				return owner != null && distanceToSqr(owner) < FOLLOW_START_DISTANCE * FOLLOW_START_DISTANCE
						&& super.canUse();
			}

			@Override
			protected Vec3 getPosition() {
				ServerPlayer owner = getEncounterOwner();
				Vec3 destination = super.getPosition();
				return owner != null && destination != null
						&& destination.distanceToSqr(owner.position()) <= FOLLOW_START_DISTANCE * FOLLOW_START_DISTANCE
						? destination : null;
			}
		});
		this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
	}

	@Override
	public void tick() {
		if (this.level() instanceof ServerLevel level) {
			ServerPlayer owner = this.getEncounterOwner();
			if (owner == null || !owner.isAlive() || owner.isSpectator() || owner.level() != level
					|| this.distanceToSqr(owner) > 32.0 * 32.0
					|| this.tickCount >= LIFETIME_TICKS + MAX_GOODBYE_APPROACH_TICKS + GOODBYE_DURATION_TICKS) {
				this.discard();
				return;
			}
			if (--this.soundCooldown <= 0) {
				this.playOwnerAmbientSound(owner);
				this.soundCooldown = 80 + this.random.nextInt(81);
			}
		}
		double oldX = this.getX();
		double oldZ = this.getZ();
		super.tick();
		if (this.isRemoved()) return;
		this.previousUprightAmount = this.uprightAmount;
		double movement = Mth.square(this.getX() - oldX) + Mth.square(this.getZ() - oldZ);
		boolean resting = this.onGround() && !this.isInWater()
				&& (this.isGivingBeer()
						|| movement < 1.0E-4 && this.getDeltaMovement().horizontalDistanceSqr() < 1.0E-4);
		this.uprightAmount = Mth.clamp(this.uprightAmount + (resting ? 0.125f : -0.25f), 0, 1);
	}

	private void playOwnerAmbientSound(ServerPlayer owner) {
		owner.connection.send(new ClientboundSoundPacket(
				BuiltInRegistries.SOUND_EVENT.wrapAsHolder(ModSounds.SQUIRREL_AMBIENT), SoundSource.NEUTRAL,
				this.getX(), this.getY(), this.getZ(), 0.7f, 0.94f + this.random.nextFloat() * 0.12f,
				this.random.nextLong()));
	}

	/** Reserves movement/look so wandering cannot interrupt the farewell or restart a gift. */
	private final class GoodbyeGoal extends Goal {
		private int pathCooldown;
		private Vec3 destination;

		private GoodbyeGoal() { this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
		@Override public boolean canUse() { return isSayingGoodbye(); }
		@Override public boolean canContinueToUse() { return isSayingGoodbye(); }
		@Override public boolean requiresUpdateEveryTick() { return true; }
		@Override public void start() { this.pathCooldown = 0; }
		@Override public void stop() { getNavigation().stop(); }

		@Override
		public void tick() {
			ServerPlayer owner = getEncounterOwner();
			if (owner == null) return;
			if (isGivingBeer()) {
				// If the player leaves before receiving the beer, approach again instead of giving it remotely.
				if (!giftGiven && distanceToSqr(owner) > 3.0 * 3.0) {
					entityData.set(GIVING_TICKS, -1);
					setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
					this.pathCooldown = 0;
					return;
				}
				getNavigation().stop();
				setDeltaMovement(0, getDeltaMovement().y, 0);
				faceOwner(owner);
				int elapsed = entityData.get(GIVING_TICKS) + 1;
				entityData.set(GIVING_TICKS, elapsed);
				if (!giftGiven && elapsed >= GIFT_HANDOVER_TICKS) {
					giveBeer(owner);
				}
				if (elapsed >= GOODBYE_DURATION_TICKS) discard();
				return;
			}
			if (--this.pathCooldown <= 0) {
				this.destination = findGoodbyePosition(owner);
				this.pathCooldown = 10;
				if (this.destination == null) getNavigation().stop();
				else getNavigation().moveTo(this.destination.x, this.destination.y, this.destination.z, 1.2);
			}
			getLookControl().setLookAt(owner, 30, 20);
			// Navigation can finish at the last block's edge; cover the final step to the actual point.
			if (this.destination != null && getNavigation().isDone()
					&& position().distanceToSqr(this.destination) < 2.0 * 2.0) {
				getMoveControl().setWantedPosition(this.destination.x, this.destination.y, this.destination.z, 0.8);
			}
			Vec3 fromOwner = position().subtract(owner.position());
			float ownerYaw = owner.getYRot() * Mth.DEG_TO_RAD;
			boolean inFront = fromOwner.x * -Mth.sin(ownerYaw) + fromOwner.z * Mth.cos(ownerYaw) > 0.6;
			if (this.destination != null && position().distanceToSqr(this.destination) < 0.8 * 0.8
					&& inFront && distanceToSqr(owner) < 2.8 * 2.8 && onGround() && !isInWater()) {
				getNavigation().stop();
				setDeltaMovement(0, getDeltaMovement().y, 0);
				faceOwner(owner);
				entityData.set(GIVING_TICKS, 0);
				setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.BEER_BOTTLE));
			}
		}
	}

	private Vec3 findGoodbyePosition(ServerPlayer owner) {
		for (float offset : new float[]{0, 25, -25, 50, -50}) {
			float yaw = (owner.getYRot() + offset) * Mth.DEG_TO_RAD;
			double x = owner.getX() - Mth.sin(yaw) * 1.6;
			double z = owner.getZ() + Mth.cos(yaw) * 1.6;
			BlockPos column = BlockPos.containing(x, owner.getY() + 1, z);
			if (!this.level().hasChunkAt(column)) continue;
			for (int down = 0; down <= 3; down++) {
				BlockPos feet = column.below(down);
				var ground = this.level().getBlockState(feet.below());
				if (!ground.isFaceSturdy(this.level(), feet.below(), Direction.UP)
						|| ground.is(Blocks.MAGMA_BLOCK) || ground.is(Blocks.CAMPFIRE)
						|| ground.is(Blocks.SOUL_CAMPFIRE) || ground.is(Blocks.CACTUS)
						|| !this.level().getFluidState(feet).isEmpty()) continue;
				Vec3 point = new Vec3(x, feet.getY(), z);
				var box = this.getBoundingBox().move(point.subtract(this.position()));
				if (this.level().getWorldBorder().isWithinBounds(box) && this.level().noCollision(this, box)) return point;
			}
		}
		return null;
	}

	private void faceOwner(ServerPlayer owner) {
		double x = owner.getX() - this.getX();
		double z = owner.getZ() - this.getZ();
		float yaw = (float) (Mth.atan2(z, x) * Mth.RAD_TO_DEG) - 90;
		this.setYRot(yaw);
		this.setYBodyRot(yaw);
		this.setYHeadRot(yaw);
		this.getLookControl().setLookAt(owner.getX(), owner.getY() + 1.1, owner.getZ(), 30, 20);
	}

	private void giveBeer(ServerPlayer owner) {
		this.giftGiven = true;
		this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		ItemStack beer = new ItemStack(ModItems.BEER_BOTTLE);
		// A full creative inventory silently deletes items in add(); check space first.
		boolean hasSpace = owner.getInventory().getFreeSlot() >= 0
				|| owner.getInventory().getSlotWithRemainingSpace(beer) >= 0;
		if (!hasSpace || !owner.getInventory().add(beer)) {
			var dropped = owner.drop(beer, false);
			if (dropped != null) {
				dropped.setTarget(owner.getUUID());
				dropped.setNoPickUpDelay();
			}
		}
	}

	private final class FollowEncounterOwnerGoal extends Goal {
		private int pathCooldown;

		private FollowEncounterOwnerGoal() {
			this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			ServerPlayer owner = getEncounterOwner();
			return owner != null && owner.isAlive() && owner.level() == level()
					&& distanceToSqr(owner) > FOLLOW_START_DISTANCE * FOLLOW_START_DISTANCE;
		}

		@Override
		public boolean canContinueToUse() {
			ServerPlayer owner = getEncounterOwner();
			return owner != null && owner.isAlive() && owner.level() == level()
					&& distanceToSqr(owner) > FOLLOW_STOP_DISTANCE * FOLLOW_STOP_DISTANCE;
		}

		@Override public boolean requiresUpdateEveryTick() { return true; }
		@Override public void start() { this.pathCooldown = 0; }
		@Override public void stop() { getNavigation().stop(); }

		@Override
		public void tick() {
			ServerPlayer owner = getEncounterOwner();
			if (owner == null) return;
			getLookControl().setLookAt(owner, 30, 30);
			if (--this.pathCooldown <= 0) {
				getNavigation().moveTo(owner, 1.2);
				this.pathCooldown = 10;
			}
		}
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public void push(Entity entity) {
		// An invisible encounter must not move other players or animals.
	}

	@Override
	public boolean canAttack(LivingEntity target) {
		return false;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	@Override public boolean isInvulnerableTo(ServerLevel level, DamageSource source) { return true; }
	@Override public boolean isPickable() { return false; }
	@Override public boolean isAttackable() { return false; }
	@Override public boolean skipAttackInteraction(Entity attacker) { return true; }
}

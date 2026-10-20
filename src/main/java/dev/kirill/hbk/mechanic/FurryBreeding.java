package dev.kirill.hbk.mechanic;

import dev.kirill.hbk.HbkMod;
import dev.kirill.hbk.entity.PinkFurryWolfEntity;
import dev.kirill.hbk.registry.ModAttachments;
import dev.kirill.hbk.registry.ModItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Vanilla-style courtship: a baby NPC, or a harmless item burst for a player pairing. */
public final class FurryBreeding {
	public static final int LOVE_TICKS = 60 * 20;
	public static final int BREED_COOLDOWN_TICKS = 5 * 60 * 20;
	public static final int BABY_AGE_TICKS = -20 * 60 * 20;
	public static final int COURTSHIP_TICKS = 60;
	public static final double MATE_SEARCH_RANGE = 8.0;
	public static final double BREED_DISTANCE = 3.0;
	public static final double FAMILY_FOLLOW_RANGE = 32.0;
	public static final double PULL_SPEED_PER_TICK = 1.0 / 20.0;
	public static final double PULL_STOP_DISTANCE = 1.5;
	public static final int BURST_ITEM_COUNT = 12;

	private FurryBreeding() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) tickPlayer(player);
		});
	}

	private static long now(LivingEntity entity) {
		return entity.level() instanceof ServerLevel level
				? level.getServer().overworld().getGameTime() : entity.level().getGameTime();
	}

	private static boolean validPlayer(Player player) {
		return player.isAlive() && !player.isSpectator() && FurryTransformation.isActive(player)
				&& (!(player.level() instanceof ServerLevel)
						|| player.getAttachedOrElse(ModAttachments.FURRY_EXPIRES_AT, 0L) > now(player));
	}

	public static boolean isInLove(LivingEntity entity) {
		if (entity instanceof PinkFurryWolfEntity furry) return furry.isInLove();
		return entity instanceof Player player && validPlayer(player)
				&& player.getAttachedOrElse(ModAttachments.FURRY_LOVE_UNTIL, 0L) > now(player)
				&& player.getAttachedOrElse(ModAttachments.FURRY_BREED_COOLDOWN_UNTIL, 0L) <= now(player);
	}

	public static boolean canEnterLove(LivingEntity entity) {
		if (entity instanceof PinkFurryWolfEntity furry) {
			// Native client getAge() is only -1/1; the server alone knows the adult cooldown.
			boolean adultReady = furry.level().isClientSide() ? !furry.isBaby() : furry.getAge() == 0;
			return furry.isAlive() && adultReady && !furry.isFleeing() && !furry.isInLove();
		}
		return entity instanceof Player player && validPlayer(player) && !isInLove(player)
				&& player.getAttachedOrElse(ModAttachments.FURRY_BREED_COOLDOWN_UNTIL, 0L) <= now(player);
	}

	/** Shared by client outlines and movement; targets need not themselves be ready to breed. */
	public static boolean isAttractionTarget(Player player, LivingEntity candidate) {
		if (candidate == player || candidate.level() != player.level() || !candidate.isAlive()) return false;
		if (candidate instanceof PinkFurryWolfEntity furry) return !furry.isBaby();
		return candidate instanceof Player other && validPlayer(other);
	}

	/** Server lookup; clients supply their currently tracked/renderable entities to the overload below. */
	public static LivingEntity findAttractionTarget(Player player) {
		return player.level() instanceof ServerLevel level ? findAttractionTarget(player, level.getAllEntities()) : null;
	}

	/** Queries existing entities only, without a radius limit, chunk requests or another dimension. */
	public static LivingEntity findAttractionTarget(Player player, Iterable<? extends Entity> loadedEntities) {
		if (!isInLove(player)) return null;
		LivingEntity nearest = null;
		double closest = Double.POSITIVE_INFINITY;
		for (Entity entity : loadedEntities) {
			if (!(entity instanceof LivingEntity candidate) || !isAttractionTarget(player, candidate)) continue;
			double distance = player.distanceToSqr(candidate);
			if (distance <= closest) {
				nearest = candidate;
				closest = distance;
			}
		}
		return nearest;
	}

	/**
	 * Applied to the local player's SELF movement before vanilla collision handling, not a teleport
	 * or server-only velocity. The ordinary movement/input pipeline and vertical motion stay active.
	 */
	public static Vec3 constrainLoveMovement(Player player, Vec3 requestedMovement) {
		return constrainLoveMovementToTarget(player, requestedMovement, findAttractionTarget(player));
	}

	public static Vec3 constrainLoveMovement(Player player, Vec3 requestedMovement, Iterable<? extends Entity> loadedEntities) {
		return constrainLoveMovementToTarget(player, requestedMovement, findAttractionTarget(player, loadedEntities));
	}

	private static Vec3 constrainLoveMovementToTarget(Player player, Vec3 requestedMovement, LivingEntity target) {
		if (player.isPassenger()) return requestedMovement;
		if (target == null) return requestedMovement;
		double x = target.getX() - player.getX();
		double z = target.getZ() - player.getZ();
		double horizontalDistance = Math.sqrt(x * x + z * z);
		if (horizontalDistance <= PULL_STOP_DISTANCE) return requestedMovement;
		return new Vec3(x / horizontalDistance * PULL_SPEED_PER_TICK + requestedMovement.x,
				requestedMovement.y,
				z / horizontalDistance * PULL_SPEED_PER_TICK + requestedMovement.z);
	}

	public static boolean enterLove(LivingEntity entity) {
		if (!(entity.level() instanceof ServerLevel) || !canEnterLove(entity)) return false;
		if (entity instanceof PinkFurryWolfEntity furry) furry.setLoveTicks(LOVE_TICKS);
		else entity.setAttached(ModAttachments.FURRY_LOVE_UNTIL, now(entity) + LOVE_TICKS);
		hearts(entity, 7);
		return true;
	}

	/** Called only at vanilla's permitted ground-pickup site, never from inventory polling. */
	public static boolean consumeGroundPickup(Player player, ItemStack groundStack) {
		if (!groundStack.is(ModItems.FURRY_PLUG) || !enterLove(player)) return false;
		groundStack.shrink(1);
		return true;
	}

	public static void tickPlayer(ServerPlayer player) {
		long time = now(player);
		if (player.hasAttached(ModAttachments.FURRY_LOVE_UNTIL)) {
			if (!isInLove(player)) player.removeAttached(ModAttachments.FURRY_LOVE_UNTIL);
			else if (time % 10 == 0) hearts(player, 1);
		}
		if (player.hasAttached(ModAttachments.FURRY_BREED_COOLDOWN_UNTIL)
				&& player.getAttachedOrElse(ModAttachments.FURRY_BREED_COOLDOWN_UNTIL, 0L) <= time) {
			player.removeAttached(ModAttachments.FURRY_BREED_COOLDOWN_UNTIL);
		}
	}

	public static void hearts(LivingEntity entity, int count) {
		if (entity.level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.HEART, entity.getX(), entity.getY(0.65), entity.getZ(),
					count, entity.getBbWidth() * 0.4, entity.getBbHeight() * 0.25, entity.getBbWidth() * 0.4, 0.02);
		}
	}

	private static boolean canMate(PinkFurryWolfEntity furry, LivingEntity mate) {
		return furry != mate && furry.level() == mate.level() && isInLove(furry) && isInLove(mate);
	}

	/** Both participants are rechecked before committing exactly one of the two outcomes. */
	public static boolean tryBreed(PinkFurryWolfEntity furry, LivingEntity mate) {
		if (!(furry.level() instanceof ServerLevel level) || !canMate(furry, mate)
				|| furry.distanceToSqr(mate) > BREED_DISTANCE * BREED_DISTANCE || !furry.hasLineOfSight(mate)) return false;
		PinkFurryWolfEntity child = furry.getBreedOffspring(level, mate instanceof PinkFurryWolfEntity other ? other : furry);
		if (child == null) return false;
		child.setAge(BABY_AGE_TICKS);
		child.setPersistenceRequired();
		child.setParents(furry, mate);
		child.snapTo((furry.getX() + mate.getX()) * 0.5, Math.min(furry.getY(), mate.getY()),
				(furry.getZ() + mate.getZ()) * 0.5, 0, 0);
		// Check the shared completion spot before rolling: obstruction cannot bias the 50/50 choice.
		if (!level.noCollision(child)) return false;
		boolean burst = mate instanceof ServerPlayer player && player.getRandom().nextBoolean();
		if (!burst && !level.addFreshEntity(child)) return false;
		// Close both love states before emitting drops or the advancement: no second goal can commit again.
		furry.resetLove();
		furry.setAge(BREED_COOLDOWN_TICKS);
		if (mate instanceof PinkFurryWolfEntity other) {
			other.resetLove();
			other.setAge(BREED_COOLDOWN_TICKS);
		} else if (mate instanceof Player player) {
			player.removeAttached(ModAttachments.FURRY_LOVE_UNTIL);
			player.setAttached(ModAttachments.FURRY_BREED_COOLDOWN_UNTIL, now(player) + BREED_COOLDOWN_TICKS);
		}
		if (burst) emitBurst(level, (ServerPlayer) mate, child.position().add(0, 0.8, 0));
		hearts(furry, 7);
		hearts(mate, 7);
		return true;
	}

	/** Real material drops use the existing item model; they never deal damage or replace blocks. */
	private static void emitBurst(ServerLevel level, ServerPlayer player, Vec3 origin) {
		for (int index = 0; index < BURST_ITEM_COUNT; index++) {
			double angle = 2.0 * Math.PI * index / BURST_ITEM_COUNT;
			// Spread the starting points too, so vanilla merging does not swallow the flight on tick two.
			var item = new ItemEntity(level, origin.x + Math.cos(angle) * 1.5, origin.y,
					origin.z + Math.sin(angle) * 1.5, new ItemStack(ModItems.SHPERMA),
					Math.cos(angle) * 0.38, 0.34 + 0.04 * (index % 3), Math.sin(angle) * 0.38);
			item.setPickUpDelay(20);
			level.addFreshEntity(item);
		}
		level.playSound(null, origin.x, origin.y, origin.z, SoundEvents.SNOWBALL_THROW,
				SoundSource.PLAYERS, 0.9f, 1.0f);
		var advancement = level.getServer().getAdvancements().get(HbkMod.id("torn_ace"));
		if (advancement != null) player.getAdvancements().award(advancement, "burst");
	}

	public static final class MateGoal extends Goal {
		private final PinkFurryWolfEntity furry;
		private LivingEntity mate;
		private int closeTicks;

		public MateGoal(PinkFurryWolfEntity furry) {
			this.furry = furry;
			this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if (!(this.furry.level() instanceof ServerLevel level) || !this.furry.isInLove()) return false;
			this.mate = null;
			double closest = MATE_SEARCH_RANGE * MATE_SEARCH_RANGE;
			for (PinkFurryWolfEntity candidate : level.getEntitiesOfClass(PinkFurryWolfEntity.class,
					this.furry.getBoundingBox().inflate(MATE_SEARCH_RANGE))) {
				double distance = this.furry.distanceToSqr(candidate);
				if (canMate(this.furry, candidate) && distance < closest) {
					this.mate = candidate;
					closest = distance;
				}
			}
			for (Player candidate : level.players()) {
				double distance = this.furry.distanceToSqr(candidate);
				if (canMate(this.furry, candidate) && distance < closest) {
					this.mate = candidate;
					closest = distance;
				}
			}
			return this.mate != null;
		}

		@Override
		public boolean canContinueToUse() {
			return this.mate != null && canMate(this.furry, this.mate)
					&& this.furry.distanceToSqr(this.mate) < 16 * 16;
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void start() {
			this.closeTicks = 0;
		}

		@Override
		public void tick() {
			if (this.mate == null) return;
			this.furry.getLookControl().setLookAt(this.mate, 10.0f, this.furry.getMaxHeadXRot());
			this.furry.getNavigation().moveTo(this.mate, 1.0);
			if (this.furry.distanceToSqr(this.mate) <= BREED_DISTANCE * BREED_DISTANCE
					&& this.furry.hasLineOfSight(this.mate)) {
				if (++this.closeTicks >= COURTSHIP_TICKS) tryBreed(this.furry, this.mate);
			} else this.closeTicks = 0;
		}

		@Override
		public void stop() {
			this.mate = null;
			this.closeTicks = 0;
			this.furry.getNavigation().stop();
		}
	}
}

package dev.kirill.hbk.entity;

import java.util.EnumSet;
import java.util.UUID;

import dev.kirill.hbk.effect.GoshasRageEffect;
import dev.kirill.hbk.item.FurryPlugItem;
import dev.kirill.hbk.mechanic.FurryBreeding;
import dev.kirill.hbk.registry.ModEffects;
import dev.kirill.hbk.registry.ModEntityTypes;
import dev.kirill.hbk.registry.ModItems;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** A peaceful wanderer that only rarely swats a player before fleeing. */
public final class PinkFurryWolfEntity extends AgeableMob {
	public static final int FLEE_DURATION_TICKS = 10 * 20;
	public static final float RETALIATION_CHANCE = 0.10f;
	public static final float RETALIATION_DAMAGE = 6.0f;
	private static final EntityDataAccessor<Boolean> FLEEING = SynchedEntityData.defineId(
			PinkFurryWolfEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> FUR_COLOR = SynchedEntityData.defineId(
			PinkFurryWolfEntity.class, EntityDataSerializers.INT);
	private static final double FLEE_SPEED = 1.65;

	private LivingEntity fleeAttacker;
	private int fleeTicks;
	private int loveTicks;
	private UUID firstParentId;
	private UUID secondParentId;
	private UUID playerParentId;

	public PinkFurryWolfEntity(EntityType<? extends PinkFurryWolfEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
		this.setFurColor(randomFurColor());
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 30.0)
				.add(Attributes.MOVEMENT_SPEED, 0.25)
				.add(Attributes.FOLLOW_RANGE, 16.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new FleeAttackerGoal());
		this.goalSelector.addGoal(2, new FurryBreeding.MateGoal(this));
		this.goalSelector.addGoal(3, new FollowFamilyGoal());
		this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0));
		this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0f));
		this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
	}

	@Override
	public boolean canAttack(LivingEntity target) {
		return false;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(FLEEING, false);
		builder.define(FUR_COLOR, DyeColor.PINK.getId());
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
			EntitySpawnReason reason, SpawnGroupData data) {
		if (reason != EntitySpawnReason.NATURAL && reason != EntitySpawnReason.CHUNK_GENERATION) {
			this.setPersistenceRequired();
		}
		this.setFurColor(randomFurColor());
		// Adding native age support must not introduce babies into the previous natural spawn groups.
		return super.finalizeSpawn(level, difficulty, reason,
				data instanceof AgeableMobGroupData ? data : new AgeableMobGroupData(false));
	}

	@Override
	public PinkFurryWolfEntity getBreedOffspring(ServerLevel level, AgeableMob mate) {
		PinkFurryWolfEntity child = ModEntityTypes.PINK_FURRY_WOLF.create(level, EntitySpawnReason.BREEDING);
		if (child != null) {
			child.setFurColor(randomFurColor());
			// The native spawn-egg-on-parent path also calls this method, without finalizeSpawn.
			child.setPersistenceRequired();
		}
		return child;
	}

	@Override
	public boolean canBreed() {
		return this.getAge() == 0;
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		return stack.is(ModItems.FURRY_PLUG) ? FurryPlugItem.useOnTarget(stack, player, this)
				: super.mobInteract(player, hand);
	}

	@Override
	public boolean removeWhenFarAway(double distance) { return true; }

	private DyeColor randomFurColor() {
		return DyeColor.VALUES.get(this.random.nextInt(DyeColor.VALUES.size()));
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		boolean hurt = super.hurtServer(level, source, amount);
		if (hurt) this.resetLove();
		if (hurt && this.isAlive() && source.getEntity() instanceof LivingEntity attacker) {
			if (attacker instanceof Player player && this.random.nextFloat() < RETALIATION_CHANCE) {
				this.getLookControl().setLookAt(player, 30.0f, 30.0f);
				this.swing(InteractionHand.MAIN_HAND);
				if (player.hurtServer(level, this.damageSources().mobAttack(this), RETALIATION_DAMAGE)) {
					player.addEffect(new MobEffectInstance(ModEffects.GOSHAS_RAGE,
							GoshasRageEffect.DURATION_TICKS, 0, false, true, true), this);
				}
			}
			this.fleeAttacker = attacker;
			this.fleeTicks = FLEE_DURATION_TICKS;
			this.entityData.set(FLEEING, true);
			this.navigation.stop();
		}
		return hurt;
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (!this.level().isClientSide() && this.loveTicks > 0) {
			if (!this.isInLove()) this.resetLove();
			else {
				this.loveTicks--;
				if (this.loveTicks % 10 == 0) FurryBreeding.hearts(this, 1);
			}
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (this.fleeTicks > 0 && --this.fleeTicks == 0) {
			this.fleeAttacker = null;
			this.entityData.set(FLEEING, false);
			this.navigation.stop();
		}
	}

	public boolean isFleeing() {
		return this.entityData.get(FLEEING);
	}

	public boolean isInLove() {
		return this.loveTicks > 0 && this.isAlive() && this.getAge() == 0 && !this.isFleeing();
	}

	public int getLoveTicks() {
		return this.loveTicks;
	}

	public void setLoveTicks(int ticks) {
		this.loveTicks = Math.clamp(ticks, 0, FurryBreeding.LOVE_TICKS);
	}

	public void resetLove() {
		this.loveTicks = 0;
	}

	public void setParents(LivingEntity first, LivingEntity second) {
		this.firstParentId = first.getUUID();
		this.secondParentId = second.getUUID();
		this.playerParentId = first instanceof Player ? first.getUUID()
				: second instanceof Player ? second.getUUID() : null;
	}

	public UUID getFirstParentId() {
		return this.firstParentId;
	}

	public UUID getSecondParentId() {
		return this.secondParentId;
	}

	public UUID getPlayerParentId() {
		return this.playerParentId;
	}

	/** A player child waits for that specific player; it never adopts a nearby unrelated adult. */
	public LivingEntity getFollowParent() {
		if (!this.isBaby()) return null;
		double rangeSquared = FurryBreeding.FAMILY_FOLLOW_RANGE * FurryBreeding.FAMILY_FOLLOW_RANGE;
		if (this.playerParentId != null) {
			Player player = this.level().getPlayerByUUID(this.playerParentId);
			return player != null && player.isAlive() && !player.isSpectator()
					&& this.distanceToSqr(player) <= rangeSquared ? player : null;
		}
		PinkFurryWolfEntity nearest = null;
		double closest = rangeSquared;
		for (PinkFurryWolfEntity candidate : this.level().getEntitiesOfClass(PinkFurryWolfEntity.class,
				this.getBoundingBox().inflate(FurryBreeding.FAMILY_FOLLOW_RANGE))) {
			if (candidate == this || candidate.isBaby() || !candidate.isAlive()
					|| !(candidate.getUUID().equals(this.firstParentId) || candidate.getUUID().equals(this.secondParentId))) continue;
			double distance = this.distanceToSqr(candidate);
			if (distance <= closest) {
				nearest = candidate;
				closest = distance;
			}
		}
		return nearest;
	}

	public boolean isFleeingFrom(LivingEntity entity) {
		return this.fleeTicks > 0 && this.fleeAttacker == entity;
	}

	public DyeColor getFurColor() {
		return DyeColor.byId(this.entityData.get(FUR_COLOR));
	}

	public void setFurColor(DyeColor color) {
		this.entityData.set(FUR_COLOR, color.getId());
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("fur_color", this.getFurColor().getId());
		output.putInt("furry_love_ticks", this.loveTicks);
		if (this.firstParentId != null) output.store("furry_first_parent", UUIDUtil.CODEC, this.firstParentId);
		if (this.secondParentId != null) output.store("furry_second_parent", UUIDUtil.CODEC, this.secondParentId);
		if (this.playerParentId != null) output.store("furry_player_parent", UUIDUtil.CODEC, this.playerParentId);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.setFurColor(DyeColor.byId(input.getIntOr("fur_color", DyeColor.PINK.getId())));
		this.setLoveTicks(this.getAge() == 0 ? input.getIntOr("furry_love_ticks", 0) : 0);
		this.firstParentId = input.read("furry_first_parent", UUIDUtil.CODEC).orElse(null);
		this.secondParentId = input.read("furry_second_parent", UUIDUtil.CODEC).orElse(null);
		this.playerParentId = input.read("furry_player_parent", UUIDUtil.CODEC).orElse(null);
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean causedByPlayer) {
		super.dropCustomDeathLoot(level, source, causedByPlayer);
		if (this.random.nextFloat() < 0.2f) {
			this.spawnAtLocation(level, new ItemStack(Items.WOOL.pick(this.getFurColor()), 1 + this.random.nextInt(3)));
		}
	}

	private final class FollowFamilyGoal extends Goal {
		private LivingEntity parent;
		private int pathTicks;

		private FollowFamilyGoal() {
			this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			this.parent = getFollowParent();
			return this.parent != null && distanceToSqr(this.parent) >= 3.0 * 3.0;
		}

		@Override
		public boolean canContinueToUse() {
			return this.parent != null && this.parent == getFollowParent() && distanceToSqr(this.parent) > 2.0 * 2.0;
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void start() {
			this.pathTicks = 0;
		}

		@Override
		public void tick() {
			if (this.parent == null) return;
			getLookControl().setLookAt(this.parent, 10.0f, getMaxHeadXRot());
			if (--this.pathTicks <= 0) {
				this.pathTicks = 10;
				getNavigation().moveTo(this.parent, 1.1);
			}
		}

		@Override
		public void stop() {
			this.parent = null;
			getNavigation().stop();
		}
	}

	private final class FleeAttackerGoal extends Goal {
		private FleeAttackerGoal() {
			this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			return fleeTicks > 0 && fleeAttacker != null;
		}

		@Override
		public boolean canContinueToUse() {
			return this.canUse();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void start() {
			this.chooseEscapePosition();
		}

		@Override
		public void tick() {
			if (fleeAttacker == null) {
				return;
			}
			getLookControl().setLookAt(fleeAttacker, 30.0f, 30.0f);
			if (getNavigation().isDone() || tickCount % 10 == 0) {
				this.chooseEscapePosition();
			}
		}

		@Override
		public void stop() {
			getNavigation().stop();
		}

		private void chooseEscapePosition() {
			if (fleeAttacker == null) {
				return;
			}
			Vec3 escape = DefaultRandomPos.getPosAway(PinkFurryWolfEntity.this, 16, 7, fleeAttacker.position());
			if (escape != null) {
				getNavigation().moveTo(escape.x, escape.y, escape.z, FLEE_SPEED);
			}
		}
	}
}

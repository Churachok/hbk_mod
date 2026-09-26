package dev.kirill.hbk.entity;

import java.util.EnumSet;

import dev.kirill.hbk.effect.GoshasRageEffect;
import dev.kirill.hbk.registry.ModEffects;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
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
public final class PinkFurryWolfEntity extends PathfinderMob {
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
		this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0));
		this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0f));
		this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
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
		this.setFurColor(randomFurColor());
		return super.finalizeSpawn(level, difficulty, reason, data);
	}

	private DyeColor randomFurColor() {
		return DyeColor.VALUES.get(this.random.nextInt(DyeColor.VALUES.size()));
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		boolean hurt = super.hurtServer(level, source, amount);
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
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.setFurColor(DyeColor.byId(input.getIntOr("fur_color", DyeColor.PINK.getId())));
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean causedByPlayer) {
		super.dropCustomDeathLoot(level, source, causedByPlayer);
		if (this.random.nextFloat() < 0.2f) {
			this.spawnAtLocation(level, new ItemStack(Items.WOOL.pick(this.getFurColor()), 1 + this.random.nextInt(3)));
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

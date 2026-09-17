package dev.kirill.hbk.entity;

import dev.kirill.hbk.util.NkvdSpawning;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public final class NurseEntity extends PathfinderMob {
	private static final int HEAL_INTERVAL = 20 * 10;
	private static final double HEAL_RADIUS = 10.0;
	private static final float HEAL_AMOUNT = 4.0f;
	private boolean panicked;

	public NurseEntity(EntityType<? extends NurseEntity> type, Level level) {
		super(type, level);
		this.setPersistenceRequired();
		this.xpReward = 3;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 20.0)
				.add(Attributes.MOVEMENT_SPEED, 0.30)
				.add(Attributes.FOLLOW_RANGE, 24.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0));
		this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 10.0f));
		this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
	}

	@Override
	public void tick() {
		super.tick();
		if (!(this.level() instanceof ServerLevel level)) {
			return;
		}
		if (this.panicked) {
			if (this.tickCount % 10 == 0) {
				this.fleeNearestPlayer(level);
			}
		} else if (this.tickCount % HEAL_INTERVAL == 0) {
			this.healEveryone(level);
		}
	}

	private void healEveryone(ServerLevel level) {
		boolean healed = false;
		for (LivingEntity patient : level.getEntitiesOfClass(
				LivingEntity.class,
				this.getBoundingBox().inflate(HEAL_RADIUS),
				entity -> entity != this && entity.isAlive() && entity.getHealth() < entity.getMaxHealth())) {
			patient.heal(HEAL_AMOUNT);
			level.sendParticles(ParticleTypes.HEART, patient.getX(), patient.getY(0.7), patient.getZ(), 4, 0.25, 0.35, 0.25, 0.02);
			healed = true;
		}
		if (healed) {
			level.playSound(null, this.blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.NEUTRAL, 0.7f, 1.35f);
		}
	}

	private void fleeNearestPlayer(ServerLevel level) {
		Player player = level.getNearestPlayer(this, 24.0);
		if (player == null) {
			return;
		}
		Vec3 away = this.position().subtract(player.position());
		if (away.lengthSqr() < 0.01) {
			away = new Vec3(level.getRandom().nextDouble() - 0.5, 0.0, level.getRandom().nextDouble() - 0.5);
		}
		away = away.normalize().scale(14.0);
		this.getNavigation().moveTo(this.getX() + away.x, this.getY(), this.getZ() + away.z, 1.55);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		boolean hurt = super.hurtServer(level, source, amount);
		if (hurt && source.getEntity() instanceof Player && !this.panicked) {
			this.panicked = true;
			this.getNavigation().stop();
			NkvdSpawning.spawnSquad(level, this.blockPosition(), 1);
			this.fleeNearestPlayer(level);
		}
		return hurt;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.VILLAGER_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.VILLAGER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.VILLAGER_DEATH;
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("panicked", this.panicked);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.panicked = input.getBooleanOr("panicked", false);
	}
}

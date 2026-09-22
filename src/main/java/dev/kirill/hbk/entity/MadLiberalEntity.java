package dev.kirill.hbk.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class MadLiberalEntity extends HumanoidBossEntity {
	public static final double MAX_HEALTH = 400.0;
	private static final EntityDataAccessor<Integer> ARMOR_CRACK_STAGE = SynchedEntityData.defineId(
			MadLiberalEntity.class, EntityDataSerializers.INT);
	private static final double PHASE_TWO_HEALTH = MAX_HEALTH * 0.5;

	private int ramCooldown = 50;
	private int ramTicks;
	private int slamCooldown = 80;
	private Vec3 ramDirection = Vec3.ZERO;
	private boolean phaseTwoStarted;

	public MadLiberalEntity(EntityType<? extends MadLiberalEntity> type, Level level) {
		super(type, level, BossEvent.BossBarColor.YELLOW);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, MAX_HEALTH)
				.add(Attributes.MOVEMENT_SPEED, 0.18)
				.add(Attributes.FOLLOW_RANGE, 48.0)
				.add(Attributes.ATTACK_DAMAGE, 16.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
				.add(Attributes.STEP_HEIGHT, 1.5);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(ARMOR_CRACK_STAGE, 0);
	}

	public int getArmorCrackStage() {
		return this.entityData.get(ARMOR_CRACK_STAGE);
	}

	public boolean isPhaseTwo() {
		return this.getHealth() <= PHASE_TWO_HEALTH;
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0f));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		this.updateArmor(level);
		if (this.ramCooldown > 0) this.ramCooldown--;
		if (this.slamCooldown > 0) this.slamCooldown--;

		if (this.tickCount % 5 == 0 && this.getDeltaMovement().horizontalDistanceSqr() > 0.005) {
			this.breakWeakBlocks(level, 1, 1, 1.5f);
		}

		LivingEntity target = this.getTarget();
		if (target == null || !target.isAlive()) {
			this.ramTicks = 0;
			return;
		}

		if (this.ramTicks > 0) {
			this.continueRam(level);
			return;
		}
		if (this.ramCooldown <= 0 && this.distanceToSqr(target) >= 9.0 && this.distanceToSqr(target) <= 324.0
				&& this.getSensing().hasLineOfSight(target)) {
			this.startRam(target);
			return;
		}
		if (this.slamCooldown <= 0 && this.distanceToSqr(target) <= 64.0) {
			this.groundSlam(level);
		}
	}

	private void updateArmor(ServerLevel level) {
		int stage = Math.min(4, (int) ((1.0f - this.getHealth() / this.getMaxHealth()) * 8.0f));
		if (stage != this.getArmorCrackStage()) {
			this.entityData.set(ARMOR_CRACK_STAGE, stage);
			this.playSound(SoundEvents.ITEM_BREAK.value(), 1.6f, 0.65f + stage * 0.08f);
			level.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 1.4, this.getZ(),
					12, 0.65, 0.9, 0.65, 0.08);
		}
		if (this.isPhaseTwo() && !this.phaseTwoStarted) {
			this.phaseTwoStarted = true;
			this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.36);
			this.playSound(SoundEvents.WITHER_BREAK_BLOCK, 2.2f, 0.75f);
			level.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 1.0, this.getZ(),
					8, 0.7, 1.0, 0.7, 0.03);
		}
	}

	private void startRam(LivingEntity target) {
		Vec3 direction = target.position().subtract(this.position());
		this.ramDirection = new Vec3(direction.x, 0.0, direction.z).normalize();
		this.ramTicks = this.isPhaseTwo() ? 25 : 18;
		this.ramCooldown = this.isPhaseTwo() ? 45 : 90;
		this.playSound(SoundEvents.RAVAGER_ROAR, 1.8f, this.isPhaseTwo() ? 1.15f : 0.8f);
	}

	private void continueRam(ServerLevel level) {
		this.ramTicks--;
		double speed = this.isPhaseTwo() ? 1.25 : 0.85;
		this.setDeltaMovement(this.ramDirection.x * speed, this.getDeltaMovement().y, this.ramDirection.z * speed);
		this.breakWeakBlocks(level, 1, 1, 2.0f);
		AABB impact = this.getBoundingBox().inflate(0.8, 0.3, 0.8);
		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, impact,
				entity -> entity != this && entity.isAlive())) {
			victim.hurtServer(level, this.damageSources().mobAttack(this), this.isPhaseTwo() ? 22.0f : 17.0f);
			victim.push(this.ramDirection.x * 2.1, 0.65, this.ramDirection.z * 2.1);
			this.ramTicks = 0;
		}
	}

	private void groundSlam(ServerLevel level) {
		this.slamCooldown = this.isPhaseTwo() ? 120 : 160;
		this.playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.7f, 0.65f);
		level.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 0.2, this.getZ(),
				10, 2.5, 0.15, 2.5, 0.03);
		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class,
				this.getBoundingBox().inflate(7.0, 2.0, 7.0), entity -> entity != this && entity.isAlive())) {
			Vec3 away = victim.position().subtract(this.position());
			double horizontal = Math.max(0.1, away.horizontalDistance());
			double strength = Math.max(0.25, 1.0 - horizontal / 8.0);
			victim.hurtServer(level, this.damageSources().mobAttack(this), 12.0f);
			victim.push(away.x / horizontal * 1.8 * strength, 0.55 + strength * 0.5,
					away.z / horizontal * 1.8 * strength);
		}
		this.breakWeakBlocks(level, 3, 0, 1.0f);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (!this.isPhaseTwo() && source.getDirectEntity() instanceof AbstractArrow) {
			amount *= 0.1f;
		}
		return super.hurtServer(level, source, amount);
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hurt = super.doHurtTarget(level, target);
		if (hurt) {
			Vec3 away = target.position().subtract(this.position()).normalize();
			target.push(away.x * 1.2, 0.35, away.z * 1.2);
		}
		return hurt;
	}

	@Override
	public void die(DamageSource source) {
		this.awardKiller(source, "first_season_finished");
		super.die(source);
	}
}

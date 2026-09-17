package dev.kirill.hbk.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public abstract class GiantBossEntity extends Monster {
	public static final float MODEL_SCALE = 6.0f;
	public static final float HITBOX_WIDTH = 3.6f;
	public static final float HITBOX_HEIGHT = 12.0f;
	public static final float EYE_HEIGHT = 10.44f;

	private final ServerBossEvent bossEvent;

	protected GiantBossEntity(EntityType<? extends GiantBossEntity> type, Level level, BossEvent.BossBarColor color) {
		super(type, level);
		this.setPersistenceRequired();
		this.xpReward = 150;
		this.bossEvent = new ServerBossEvent(this.getUUID(), this.getDisplayName(), color, BossEvent.BossBarOverlay.PROGRESS);
		this.bossEvent.setPlayBossMusic(true);
	}

	public abstract float getExplosionPower();

	public static AttributeSupplier.Builder createBaseAttributes(double health, double attackDamage) {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, health)
				.add(Attributes.MOVEMENT_SPEED, 0.26)
				.add(Attributes.FOLLOW_RANGE, 64.0)
				.add(Attributes.ATTACK_DAMAGE, attackDamage)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
				.add(Attributes.STEP_HEIGHT, 3.0)
				.add(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, 1.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new LaunchRocketGoal(this));
		this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0));
		this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 32.0f));
		this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, GiantBossEntity.class, true));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
		this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false, this::isOtherPrey));
		this.targetSelector.addGoal(4, new HurtByTargetGoal(this));
	}

	protected boolean isOtherPrey(LivingEntity entity, ServerLevel level) {
		if (entity instanceof GiantBossEntity || entity instanceof Player) {
			return false;
		}
		if (this instanceof StalinEntity && entity instanceof NkvdEntity) {
			return false;
		}
		return true;
	}

	public void launchRocketAt(Vec3 target) {
		this.launchRocketAt(target, this.getExplosionPower(), 1.55);
	}

	public void launchRocketAt(Vec3 target, float explosionPower, double speed) {
		if (!(this.level() instanceof ServerLevel serverLevel)) {
			return;
		}

		Vec3 muzzle = this.getEyePosition().add(this.getLookAngle().scale(3.0));
		Vec3 direction = target.subtract(muzzle);
		double length = direction.length();
		if (length < 0.001) {
			direction = this.getLookAngle();
		} else {
			direction = direction.scale(1.0 / length);
		}

		double distance = Math.max(8.0, length);
		Vec3 velocity = direction.add(0.0, Math.min(0.35, distance * 0.012), 0.0).normalize().scale(speed);
		GiantRocketEntity rocket = new GiantRocketEntity(serverLevel, this, velocity, explosionPower);
		rocket.setPos(muzzle.x, muzzle.y, muzzle.z);
		serverLevel.addFreshEntity(rocket);
		this.playSound(SoundEvents.FIREWORK_ROCKET_LAUNCH, 3.5f, 0.55f + this.getRandom().nextFloat() * 0.15f);
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
		this.bossEvent.setName(this.getDisplayName());
	}

	@Override
	public void startSeenByPlayer(ServerPlayer player) {
		super.startSeenByPlayer(player);
		this.bossEvent.addPlayer(player);
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		this.bossEvent.removePlayer(player);
	}

	@Override
	public void setCustomName(Component name) {
		super.setCustomName(name);
		this.bossEvent.setName(this.getDisplayName());
	}

	@Override
	public void remove(Entity.RemovalReason reason) {
		super.remove(reason);
		this.bossEvent.removeAllPlayers();
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.getDirectEntity() instanceof GiantRocketEntity) {
			return false;
		}
		return super.hurtServer(level, source, amount);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.WITHER_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.RAVAGER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.RAVAGER_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		this.playSound(SoundEvents.IRON_GOLEM_STEP, 2.0f, 0.45f);
	}

	@Override
	public boolean isPreventingPlayerRest(ServerLevel level, Player player) {
		return true;
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}
}

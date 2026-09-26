package dev.kirill.hbk.entity;

import dev.kirill.hbk.registry.ModEntityTypes;
import dev.kirill.hbk.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.UUID;

public final class KirillDoomEntity extends HumanoidBossEntity {
	public static final double MAX_HEALTH = 800.0;
	private static final float EARTHQUAKE_THRESHOLD = (float) (MAX_HEALTH * 0.70);
	private static final float DESTRUCTION_THRESHOLD = (float) (MAX_HEALTH * 0.40);
	private static final float FINAL_BLAST_THRESHOLD = (float) (MAX_HEALTH * 0.10);

	private int leapCooldown = 60;
	private int throwCooldown = 100;
	private int bombCooldown = 100;
	private UUID carriedMobUuid;
	private int carryTicks;
	private UUID thrownMobUuid;
	private int thrownMobTicks;
	private UUID grabbedPlayerUuid;
	private int grabTicks;
	private boolean finalBlastTriggered;

	public KirillDoomEntity(EntityType<? extends KirillDoomEntity> type, Level level) {
		super(type, level, BossEvent.BossBarColor.PURPLE);
		this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.ATTACKING_MEMBER));
		this.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, MAX_HEALTH)
				.add(Attributes.MOVEMENT_SPEED, 0.36)
				.add(Attributes.FOLLOW_RANGE, 64.0)
				.add(Attributes.ATTACK_DAMAGE, 20.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.75)
				.add(Attributes.STEP_HEIGHT, 1.5);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.25, true));
		this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0));
		this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 24.0f));
		this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, MadLiberalEntity.class, true));
		this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (this.leapCooldown > 0) this.leapCooldown--;
		if (this.throwCooldown > 0) this.throwCooldown--;
		if (this.bombCooldown > 0) this.bombCooldown--;

		if (this.tickCount % 20 == 0 && this.getHealth() < this.getMaxHealth()) {
			this.heal(1.0f);
		}

		LivingEntity target = this.getTarget();
		this.handlePlayerGrab(level);
		if (target != null && target.isAlive()) {
			this.tryLeap(target);
			this.handleMobThrow(level, target);
		}
		this.handleThrownImpact(level, target);

		if (this.getHealth() <= EARTHQUAKE_THRESHOLD) {
			this.shakeGround(level);
			if (this.bombCooldown <= 0 && target != null) {
				this.spawnColossalBombs(level, target);
				this.bombCooldown = 20 * 14;
			}
		}
		if (this.getHealth() <= DESTRUCTION_THRESHOLD
				&& this.getDeltaMovement().horizontalDistanceSqr() > 0.004) {
			if (this.tickCount % 5 == 0) {
				this.breakWeakBlocks(level, 1, 1, 2.5f);
			}
			if (this.tickCount % 10 == 0) {
				this.movingAreaAttack(level);
			}
		}
		if (this.getHealth() <= FINAL_BLAST_THRESHOLD && !this.finalBlastTriggered) {
			this.finalBlastTriggered = true;
			this.finalBlast(level);
		}
	}

	private void handlePlayerGrab(ServerLevel level) {
		if (this.grabbedPlayerUuid == null) {
			return;
		}
		Entity entity = level.getEntityInAnyDimension(this.grabbedPlayerUuid);
		if (!(entity instanceof Player player) || !player.isAlive()) {
			this.grabbedPlayerUuid = null;
			return;
		}
		Vec3 held = this.position().add(this.getLookAngle().scale(1.1)).add(0.0, 1.5, 0.0);
		player.teleportTo(held.x, held.y, held.z);
		player.setDeltaMovement(Vec3.ZERO);
		if (--this.grabTicks <= 0) {
			Vec3 direction = this.getLookAngle();
			player.setDeltaMovement(direction.x * 2.2, 0.7, direction.z * 2.2);
			this.grabbedPlayerUuid = null;
			this.playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.5f, 0.55f);
		}
	}

	private void tryLeap(LivingEntity target) {
		double distance = this.distanceToSqr(target);
		if (this.leapCooldown > 0 || distance < 20.0 || distance > 400.0 || !this.onGround()) {
			return;
		}
		Vec3 direction = target.position().subtract(this.position());
		direction = new Vec3(direction.x, 0.0, direction.z).normalize();
		this.setDeltaMovement(direction.x * 1.15, 0.72, direction.z * 1.15);
		this.leapCooldown = 20 * 4;
		this.playSound(SoundEvents.ENDER_DRAGON_FLAP, 1.6f, 0.75f);
	}

	private void handleMobThrow(ServerLevel level, LivingEntity target) {
		if (this.carriedMobUuid != null) {
			Entity entity = level.getEntityInAnyDimension(this.carriedMobUuid);
			if (!(entity instanceof LivingEntity carried) || !carried.isAlive()) {
				this.carriedMobUuid = null;
				return;
			}
			Vec3 hand = this.position().add(this.getLookAngle().scale(1.2)).add(0.0, 1.9, 0.0);
			carried.teleportTo(hand.x, hand.y, hand.z);
			carried.setDeltaMovement(Vec3.ZERO);
			this.carryTicks--;
			if (this.carryTicks <= 0) {
				Vec3 direction = target.getEyePosition().subtract(carried.position()).normalize();
				carried.setDeltaMovement(direction.scale(1.85).add(0.0, 0.38, 0.0));
				this.thrownMobUuid = this.carriedMobUuid;
				this.thrownMobTicks = 30;
				this.carriedMobUuid = null;
				this.playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.4f, 0.7f);
			}
			return;
		}
		if (this.throwCooldown > 0) {
			return;
		}
		Mob victim = level.getEntitiesOfClass(Mob.class, this.getBoundingBox().inflate(8.0), mob ->
				mob != this && mob.isAlive() && !(mob instanceof HumanoidBossEntity)
						&& !(mob instanceof GiantBossEntity)).stream()
				.min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
		if (victim != null) {
			this.carriedMobUuid = victim.getUUID();
			this.carryTicks = 20;
			this.throwCooldown = 20 * 9;
			this.playSound(SoundEvents.RAVAGER_ROAR, 1.2f, 1.25f);
		}
	}

	private void handleThrownImpact(ServerLevel level, LivingEntity target) {
		if (this.thrownMobUuid == null || this.thrownMobTicks-- <= 0) {
			this.thrownMobUuid = null;
			return;
		}
		Entity thrown = level.getEntityInAnyDimension(this.thrownMobUuid);
		if (!(thrown instanceof LivingEntity living) || !living.isAlive()) {
			this.thrownMobUuid = null;
			return;
		}
		if (target != null && thrown.distanceToSqr(target) <= 4.0) {
			target.hurtServer(level, this.damageSources().mobAttack(this), 18.0f);
			living.hurtServer(level, this.damageSources().mobAttack(this), 8.0f);
			target.push(living.getDeltaMovement().scale(0.8));
			this.thrownMobUuid = null;
		}
	}

	private void shakeGround(ServerLevel level) {
		if (this.tickCount % 10 != 0) {
			return;
		}
		level.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 0.1, this.getZ(),
				12, 5.0, 0.15, 5.0, 0.02);
		for (Player player : level.players()) {
			if (player.isAlive() && this.distanceToSqr(player) <= 48.0 * 48.0) {
				player.push((this.getRandom().nextDouble() - 0.5) * 0.06, 0.025,
						(this.getRandom().nextDouble() - 0.5) * 0.06);
			}
		}
		if (this.tickCount % 40 == 0) {
			this.playSound(SoundEvents.WARDEN_HEARTBEAT, 2.4f, 0.55f);
		}
	}

	private void spawnColossalBombs(ServerLevel level, LivingEntity target) {
		for (int i = 0; i < 3; i++) {
			double angle = this.getRandom().nextDouble() * Math.PI * 2.0;
			double radius = 7.0 + this.getRandom().nextDouble() * 8.0;
			int x = (int) Math.floor(target.getX() + Math.cos(angle) * radius);
			int z = (int) Math.floor(target.getZ() + Math.sin(angle) * radius);
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			ColossalBombEntity bomb = new ColossalBombEntity(ModEntityTypes.COLOSSAL_BOMB, level);
			bomb.setPos(x + 0.5, y + 0.2, z + 0.5);
			level.addFreshEntity(bomb);
		}
		this.playSound(SoundEvents.WITHER_SPAWN, 2.0f, 1.25f);
	}

	private void movingAreaAttack(ServerLevel level) {
		AABB area = this.getBoundingBox().inflate(2.5, 1.0, 2.5);
		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area,
				entity -> entity != this && entity.isAlive())) {
			victim.hurtServer(level, this.damageSources().mobAttack(this), 6.0f);
			Vec3 away = victim.position().subtract(this.position()).normalize();
			victim.push(away.x * 0.65, 0.22, away.z * 0.65);
		}
		level.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 0.5, this.getZ(),
				8, 1.2, 0.3, 1.2, 0.05);
	}

	private void finalBlast(ServerLevel level) {
		ExplosionDamageCalculator calculator = new ExplosionDamageCalculator() {
			@Override
			public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
				return entity != KirillDoomEntity.this && super.shouldDamageEntity(explosion, entity);
			}
		};
		level.explode(this, this.damageSources().explosion(this, this), calculator,
				this.position(), 16.0f, false, Level.ExplosionInteraction.TNT);
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hurt = super.doHurtTarget(level, target);
		if (hurt) {
			if (target instanceof Player player && this.grabbedPlayerUuid == null
					&& this.getRandom().nextFloat() < 0.25f) {
				this.grabbedPlayerUuid = player.getUUID();
				this.grabTicks = 12;
			} else {
				Vec3 direction = target.position().subtract(this.position()).normalize();
				target.push(direction.x * 1.8, 0.55, direction.z * 1.8);
			}
		}
		return hurt;
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
		if (this.getMainHandItem().is(ModItems.ATTACKING_MEMBER)) {
			this.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
		}
		super.dropCustomDeathLoot(level, source, recentlyHit);
	}

	@Override
	public void die(DamageSource source) {
		this.awardKiller(source, "kirill_end_of_world");
		super.die(source);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("final_blast_triggered", this.finalBlastTriggered);
		output.putInt("bomb_cooldown", this.bombCooldown);
		output.putInt("throw_cooldown", this.throwCooldown);
		if (this.carriedMobUuid != null) output.putString("carried_mob", this.carriedMobUuid.toString());
		if (this.thrownMobUuid != null) output.putString("thrown_mob", this.thrownMobUuid.toString());
		if (this.grabbedPlayerUuid != null) output.putString("grabbed_player", this.grabbedPlayerUuid.toString());
		output.putInt("carry_ticks", this.carryTicks);
		output.putInt("thrown_mob_ticks", this.thrownMobTicks);
		output.putInt("grab_ticks", this.grabTicks);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.finalBlastTriggered = input.getBooleanOr("final_blast_triggered", false);
		this.bombCooldown = input.getIntOr("bomb_cooldown", 100);
		this.throwCooldown = input.getIntOr("throw_cooldown", 100);
		this.carriedMobUuid = readUuid(input.getStringOr("carried_mob", ""));
		this.thrownMobUuid = readUuid(input.getStringOr("thrown_mob", ""));
		this.grabbedPlayerUuid = readUuid(input.getStringOr("grabbed_player", ""));
		this.carryTicks = input.getIntOr("carry_ticks", 0);
		this.thrownMobTicks = input.getIntOr("thrown_mob_ticks", 0);
		this.grabTicks = input.getIntOr("grab_ticks", 0);
	}

	private static UUID readUuid(String value) {
		try {
			return value.isEmpty() ? null : UUID.fromString(value);
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}
}

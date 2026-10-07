package dev.kirill.hbk.entity;

import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.entity.SpawnGroupData;
import dev.kirill.hbk.effect.GoshasRageEffect;
import dev.kirill.hbk.registry.ModEffects;
import dev.kirill.hbk.registry.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

/** Humanoid NPCs sharing a skin model but retaining their individual temperaments. */
public class ReferenceNpcEntity extends PathfinderMob {
	private int specialCooldown = 40;
	private boolean denisLeaping;
	private float denisLeapApexY;

	public ReferenceNpcEntity(EntityType<? extends ReferenceNpcEntity> type, Level level) {
		super(type, level);
		this.xpReward = 5;
		if (this.isNpc("sasha") || this.isNpc("vlad")) {
			this.arm();
		}
		if (this.isNpc("vlad")) {
			this.setPersistenceRequired();
		}
	}

	public boolean isNpc(String name) {
		return BuiltInRegistries.ENTITY_TYPE.getKey(this.getType()).getPath().equals(name);
	}

	public static AttributeSupplier.Builder createAttributes(double health) {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, health)
				.add(Attributes.MOVEMENT_SPEED, 0.25)
				.add(Attributes.FOLLOW_RANGE, 24.0)
				.add(Attributes.ATTACK_DAMAGE, 3.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		if (this.isNpc("anton")) {
			this.goalSelector.addGoal(1, new PanicGoal(this, 1.25));
		} else if (!this.isNpc("vlad")) {
			this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.1, true));
			this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		}
		if (this.isNpc("sasha")) {
			this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
		} else if (this.isNpc("gosha")) {
			this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Sheep.class, true));
		}
		this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0));
		this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0f));
		this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (this.isNpc("sasha") && this instanceof SashaEntity sasha
				&& sasha.breakShield(level, source)) {
			return false;
		}
		boolean hurt = super.hurtServer(level, source, amount);
		if (hurt && this.isAlive() && !this.isNpc("anton") && !this.isNpc("vlad")
				&& source.getEntity() instanceof LivingEntity attacker && this.canAttack(attacker)) {
			this.arm();
			this.setTarget(attacker);
		}
		return hurt;
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (this.isNpc("sasha") && this instanceof SashaEntity sasha) {
			sasha.tickShield(level);
			return;
		}
		LivingEntity target = this.getTarget();
		if (target == null || !target.isAlive()) {
			this.denisLeaping = false;
			return;
		}
		if (this.specialCooldown > 0) {
			this.specialCooldown--;
		}
		if (this.isNpc("denis")) {
			this.tickDenisLeap(level, target);
		} else if (this.specialCooldown == 0 && this.isNpc("gosha") && this.distanceToSqr(target) < 64.0) {
			this.roar(level);
			this.specialCooldown = 20 * 12;
		} else if (this.specialCooldown == 0 && this.isNpc("grisha") && this.distanceToSqr(target) < 16.0) {
			this.retreatBehindWall(level, target);
			this.specialCooldown = 20 * 15;
		}
	}

	private void tickDenisLeap(ServerLevel level, LivingEntity target) {
		if (this.denisLeaping) {
			this.denisLeapApexY = Math.max(this.denisLeapApexY, (float) this.getY());
			if (this.distanceToSqr(target) < 4.0 && this.getDeltaMovement().y <= 0.15) {
				float fall = Math.max(0.0f, this.denisLeapApexY - (float) this.getY());
				target.hurtServer(level, this.damageSources().mobAttack(this),
						(float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) + 4.0f + 3.0f * fall);
				level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.5), target.getZ(),
						16, 0.5, 0.5, 0.5, 0.1);
				this.denisLeaping = false;
			} else if (this.onGround()) {
				this.denisLeaping = false;
			}
			return;
		}
		double distance = this.distanceToSqr(target);
		if (this.specialCooldown > 0 || !this.onGround() || distance < 16.0 || distance > 144.0) {
			return;
		}
		Vec3 toward = target.position().subtract(this.position());
		Vec3 horizontal = new Vec3(toward.x, 0, toward.z).normalize();
		this.setDeltaMovement(horizontal.scale(Math.min(1.1, Math.sqrt(distance) * 0.13)).add(0, 0.65, 0));
		this.denisLeapApexY = (float) this.getY();
		this.denisLeaping = true;
		this.specialCooldown = 20 * 8;
		this.playSound(SoundEvents.RAVAGER_ATTACK, 1.1f, 1.3f);
	}

	private void roar(ServerLevel level) {
		this.playSound(SoundEvents.RAVAGER_ROAR, 1.5f, 0.7f);
		level.sendParticles(ParticleTypes.GUST, this.getX(), this.getY(0.7), this.getZ(),
				18, 2.5, 0.6, 2.5, 0.08);
		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class,
				this.getBoundingBox().inflate(5.0), entity -> entity != this && this.canAttack(entity))) {
			Vec3 away = victim.position().subtract(this.position());
			if (away.horizontalDistanceSqr() > 0.01) {
				Vec3 push = new Vec3(away.x, 0, away.z).normalize().scale(1.2);
				victim.push(push.x, 0.35, push.z);
				victim.hurtServer(level, this.damageSources().mobAttack(this), 2.0f);
			}
		}
	}

	private void retreatBehindWall(ServerLevel level, LivingEntity target) {
		Vec3 away = this.position().subtract(target.position());
		if (away.horizontalDistanceSqr() < 0.01) return;
		away = new Vec3(away.x, 0, away.z).normalize();
		this.setDeltaMovement(this.getDeltaMovement().add(away.scale(0.65)));
		if (!level.getGameRules().get(GameRules.MOB_GRIEFING)) return;
		Direction facing = Direction.getApproximateNearest(-away.x, 0, -away.z);
		BlockPos center = this.blockPosition().relative(facing);
		Direction side = facing.getClockWise();
		for (int offset = -1; offset <= 1; offset++) {
			for (int height = 0; height < 2; height++) {
				BlockPos pos = center.relative(side, offset).above(height);
				if (level.getBlockState(pos).canBeReplaced()
						&& level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
					level.setBlockAndUpdate(pos, Blocks.BRICKS.defaultBlockState());
				}
			}
		}
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hurt = super.doHurtTarget(level, target);
		if (hurt && this.isNpc("gosha") && target instanceof Player player) {
			player.addEffect(new MobEffectInstance(ModEffects.GOSHAS_RAGE,
					GoshasRageEffect.DURATION_TICKS, 0, false, true, true), this);
		}
		return hurt;
	}

	@Override
	public boolean canAttack(LivingEntity target) {
		if (this.isNpc("vlad") || this.isNpc("anton")) {
			return false;
		}
		if (this.isNpc("sasha") && target instanceof ReferenceNpcEntity npc && npc.isNpc("lesha")) {
			return false;
		}
		return super.canAttack(target);
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,
			DifficultyInstance difficulty, EntitySpawnReason reason,
			SpawnGroupData data) {
		if (reason != EntitySpawnReason.NATURAL
				&& reason != EntitySpawnReason.CHUNK_GENERATION) this.setPersistenceRequired();
		return super.finalizeSpawn(level, difficulty, reason, data);
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return this.getType().getCategory() == MobCategory.MONSTER;
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
		Item weapon = this.getWeapon();
		if ((weapon != null && this.getMainHandItem().is(weapon))
				|| (this.isNpc("lesha") && this.getMainHandItem().is(ModItems.COLOSSAL_MEMBER))) {
			// Suppress legacy saved equipment chances; the loot table owns the only roll.
			this.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
		}
		super.dropCustomDeathLoot(level, source, recentlyHit);
	}

	private void arm() {
		Item weapon = this.getWeapon();
		if (weapon != null && !this.getMainHandItem().is(weapon)) {
			this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(weapon));
			this.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
		}
	}

	private Item getWeapon() {
		return switch (BuiltInRegistries.ENTITY_TYPE.getKey(this.getType()).getPath()) {
			case "denis" -> ModItems.JAW_MEMBER;
			case "gosha" -> ModItems.BEASTLIKE_MEMBER;
			case "grisha" -> ModItems.HAMMER_FIGHTER_MEMBER;
			case "sasha" -> ModItems.ARMORED_MEMBER;
			case "vlad" -> ModItems.CARRIER_MEMBER;
			default -> null;
		};
	}
}

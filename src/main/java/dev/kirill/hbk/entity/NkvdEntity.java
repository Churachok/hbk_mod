package dev.kirill.hbk.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedCrossbowAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class NkvdEntity extends Monster implements CrossbowAttackMob {
	private static final EntityDataAccessor<Boolean> IS_CHARGING = SynchedEntityData.defineId(NkvdEntity.class, EntityDataSerializers.BOOLEAN);
	private static final double ATTACK_DAMAGE = 12.0 * 0.4;
	private static final double ARROW_DAMAGE = 8.5 * 0.4;

	private boolean villageRaider;

	public NkvdEntity(EntityType<? extends NkvdEntity> type, Level level) {
		super(type, level);
		this.setPersistenceRequired();
		this.xpReward = 12;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 50.0)
				.add(Attributes.MOVEMENT_SPEED, 0.35)
				.add(Attributes.FOLLOW_RANGE, 32.0)
				.add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
				.add(Attributes.ARMOR, 6.0);
	}

	public void setVillageRaider(boolean villageRaider) {
		this.villageRaider = villageRaider;
	}

	public boolean isVillageRaider() {
		return this.villageRaider;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(IS_CHARGING, false);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new RangedCrossbowAttackGoal<>(this, 1.5, 18.0f));
		this.goalSelector.addGoal(2, new RaidVillageGoal(this));
		this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.4));
		this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 16.0f));
		this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Villager.class, 10, true, false, this::isVillageTarget));
		this.targetSelector.addGoal(2, new HurtByTargetGoal(this, StalinEntity.class, NkvdEntity.class));
		this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, CjEntity.class, true, this::isDefaultCombatTarget));
		this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Player.class, true, this::isDefaultCombatTarget));
		this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false, this::isDefaultMobPrey));
	}

	private boolean isVillageTarget(LivingEntity entity, ServerLevel level) {
		return this.isVillageRaider();
	}

	private boolean isDefaultCombatTarget(LivingEntity entity, ServerLevel level) {
		return !this.isVillageRaider();
	}

	private boolean isDefaultMobPrey(LivingEntity entity, ServerLevel level) {
		return !this.isVillageRaider() && isPrey(entity, level);
	}

	private static boolean isPrey(LivingEntity entity, ServerLevel level) {
		return !(entity instanceof StalinEntity) && !(entity instanceof NkvdEntity) && !(entity instanceof Player);
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData data) {
		SpawnGroupData spawned = super.finalizeSpawn(level, difficulty, reason, data);
		this.equipCrossbow();
		return spawned;
	}

	private void equipCrossbow() {
		ItemStack crossbow = new ItemStack(Items.CROSSBOW);
		var enchantments = this.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		crossbow.enchant(enchantments.getOrThrow(Enchantments.QUICK_CHARGE), 3);
		crossbow.enchant(enchantments.getOrThrow(Enchantments.PIERCING), 4);
		crossbow.enchant(enchantments.getOrThrow(Enchantments.MULTISHOT), 1);
		this.setItemSlot(EquipmentSlot.MAINHAND, crossbow);
		this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.ARROW, 64));
		this.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
		this.setDropChance(EquipmentSlot.OFFHAND, 0.0f);
	}

	@Override
	public void performRangedAttack(LivingEntity target, float pullProgress) {
		if (!(this.level() instanceof ServerLevel serverLevel)) {
			return;
		}

		ItemStack weapon = this.getMainHandItem();
		ItemStack ammo = new ItemStack(Items.ARROW);
		for (int i = 0; i < 3; i++) {
			AbstractArrow arrow = ProjectileUtil.getMobArrow(this, ammo, pullProgress, weapon);
			arrow.setBaseDamage(ARROW_DAMAGE);
			arrow.setCritArrow(true);
			double dx = target.getX() - this.getX();
			double dy = target.getY(0.333) - arrow.getY();
			double dz = target.getZ() - this.getZ();
			double horiz = Math.sqrt(dx * dx + dz * dz);
			arrow.shoot(dx, dy + horiz * 0.2, dz, 2.2f, 3.0f + Math.abs(i - 1) * 4.0f);
			serverLevel.addFreshEntity(arrow);
		}
		this.playSound(SoundEvents.CROSSBOW_SHOOT, 1.0f, 1.0f / (this.getRandom().nextFloat() * 0.4f + 0.8f));
		this.onCrossbowAttackPerformed();
	}

	@Override
	public void setChargingCrossbow(boolean charging) {
		this.entityData.set(IS_CHARGING, charging);
	}

	@Override
	public void onCrossbowAttackPerformed() {
		this.entityData.set(IS_CHARGING, false);
	}

	public boolean isChargingCrossbow() {
		return this.entityData.get(IS_CHARGING);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.PILLAGER_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.PILLAGER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.PILLAGER_DEATH;
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("village_raider", this.villageRaider);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.villageRaider = input.getBooleanOr("village_raider", false);
	}
}

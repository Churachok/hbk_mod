package dev.kirill.hbk.entity;

import dev.kirill.hbk.world.GroveStreetPopulation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Neutral street residents. Their enchanted crossbows only appear after provocation. */
public final class FellasEntity extends PathfinderMob implements CrossbowAttackMob {
	private static final EntityDataAccessor<Boolean> CHARGING = SynchedEntityData.defineId(FellasEntity.class, EntityDataSerializers.BOOLEAN);
	private BoundingBox territory;

	public FellasEntity(EntityType<? extends FellasEntity> type, Level level) {
		super(type, level);
		this.setPersistenceRequired();
		this.xpReward = 5;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createMobAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, 0.25)
				.add(Attributes.FOLLOW_RANGE, 32).add(Attributes.ATTACK_DAMAGE, 3);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(CHARGING, false);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new CrossbowGoal());
		this.goalSelector.addGoal(2, new MoveTowardsRestrictionGoal(this, 1.1));
		this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1));
		this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 12));
		this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
	}

	/** Vanilla crossbow charging/shooting on a neutral PathfinderMob (the vanilla goal requires Monster). */
	private final class CrossbowGoal extends Goal {
		private int cooldown;
		private int pathCooldown;
		private int unseenTicks;
		CrossbowGoal() { this.setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK)); }
		@Override
		public boolean canUse() {
			return getTarget() != null && getTarget().isAlive() && getMainHandItem().is(Items.CROSSBOW);
		}
		@Override
		public boolean requiresUpdateEveryTick() { return true; }
		@Override
		public void stop() {
			stopUsingItem();
			setChargingCrossbow(false);
			getNavigation().stop();
			this.unseenTicks = 0;
		}
		@Override
		public void tick() {
			LivingEntity target = getTarget();
			if (target == null) return;
			boolean visible = getSensing().hasLineOfSight(target);
			this.unseenTicks = visible ? 0 : this.unseenTicks + 1;
			if (this.unseenTicks > 200) { setTarget(null); return; }
			getLookControl().setLookAt(target, 30, 30);
			if (distanceToSqr(target) > 400 || !visible) {
				if (--this.pathCooldown <= 0) {
					getNavigation().moveTo(target, 1.1);
					this.pathCooldown = 20;
				}
			} else getNavigation().stop();
			if (this.cooldown > 0) this.cooldown--;
			if (isUsingItem()) {
				if (getTicksUsingItem() >= net.minecraft.world.item.CrossbowItem.getChargeDuration(getUseItem(), FellasEntity.this)) {
					releaseUsingItem();
					setChargingCrossbow(false);
					this.cooldown = 20 + random.nextInt(20);
				}
			} else if (visible && distanceToSqr(target) <= 400 && this.cooldown == 0) {
				if (net.minecraft.world.item.CrossbowItem.isCharged(getMainHandItem())) performRangedAttack(target, 1);
				else {
					startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
					setChargingCrossbow(true);
				}
			}
		}
	}

	public void setTerritory(BoundingBox box) {
		this.territory = box;
		this.setHomeTo(new BlockPos((box.minX() + box.maxX()) / 2, box.minY() + 1,
				(box.minZ() + box.maxZ()) / 2), Math.max(2, Math.min(box.getXSpan(), box.getZSpan()) / 2 - 8));
	}

	public boolean belongsTo(BoundingBox box) {
		return this.territory != null && this.territory.minX() == box.minX()
				&& this.territory.minY() == box.minY() && this.territory.minZ() == box.minZ();
	}

	public void provoke(LivingEntity offender) {
		if (this.canAttack(offender)) this.setTarget(offender);
	}

	@Override
	public boolean canAttack(LivingEntity target) {
		return !(target instanceof FellasEntity) && super.canAttack(target);
	}

	@Override
	public void setTarget(LivingEntity target) {
		if (target != null && !this.canAttack(target)) return;
		super.setTarget(target);
		if (target == null) {
			this.stopUsingItem();
			this.setChargingCrossbow(false);
			this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		} else if (!this.getMainHandItem().is(Items.CROSSBOW)) {
			ItemStack crossbow = new ItemStack(Items.CROSSBOW);
			var enchantments = this.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
			crossbow.enchant(enchantments.getOrThrow(Enchantments.QUICK_CHARGE), 3);
			crossbow.enchant(enchantments.getOrThrow(Enchantments.PIERCING), 4);
			this.setItemSlot(EquipmentSlot.MAINHAND, crossbow);
			this.setDropChance(EquipmentSlot.MAINHAND, 0);
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.getEntity() instanceof FellasEntity) return false;
		boolean hurt = super.hurtServer(level, source, amount);
		if (hurt && source.getEntity() instanceof LivingEntity offender) {
			GroveStreetPopulation.alertToAttack(level, this, offender);
			if (this.isAlive()) this.provoke(offender);
		}
		return hurt;
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		LivingEntity target = this.getTarget();
		if (target != null && (!target.isAlive() || target instanceof Player p && (p.isCreative() || p.isSpectator())
				|| this.territory != null && !this.territory.isInside(target.blockPosition()))) this.setTarget(null);
		if (this.territory != null && !this.territory.isInside(this.blockPosition())) {
			this.setTarget(null);
			this.getNavigation().moveTo(this.getHomePosition().getX() + 0.5, this.getHomePosition().getY(),
					this.getHomePosition().getZ() + 0.5, 1.1);
		}
	}

	@Override
	public ItemStack getProjectile(ItemStack weapon) {
		return weapon.is(Items.CROSSBOW) ? new ItemStack(Items.ARROW) : super.getProjectile(weapon);
	}
	@Override
	public void performRangedAttack(LivingEntity target, float progress) { this.performCrossbowAttack(this, 1.6f); }
	@Override
	public void setChargingCrossbow(boolean value) { this.entityData.set(CHARGING, value); }
	@Override
	public void onCrossbowAttackPerformed() { this.setChargingCrossbow(false); }
	public boolean isChargingCrossbow() { return this.entityData.get(CHARGING); }

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (this.territory != null) output.putIntArray("GroveTerritory", new int[] {this.territory.minX(),
				this.territory.minY(), this.territory.minZ(), this.territory.maxX(), this.territory.maxY(), this.territory.maxZ()});
	}
	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		input.getIntArray("GroveTerritory").filter(coords -> coords.length == 6).ifPresent(coords ->
				this.setTerritory(new BoundingBox(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5])));
		this.setTarget(null);
	}
}

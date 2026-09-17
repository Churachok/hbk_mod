package dev.kirill.hbk.entity;

import dev.kirill.hbk.registry.ModEntityTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class GiantRocketEntity extends ThrowableItemProjectile {
	private float explosionPower = StalinEntity.EXPLOSION_POWER;

	public GiantRocketEntity(EntityType<? extends GiantRocketEntity> type, Level level) {
		super(type, level);
	}

	public GiantRocketEntity(Level level, LivingEntity owner, Vec3 velocity, float explosionPower) {
		super(ModEntityTypes.GIANT_ROCKET, owner, level, new ItemStack(Items.FIREWORK_ROCKET));
		this.explosionPower = explosionPower;
		this.setDeltaMovement(velocity);
	}

	@Override
	protected Item getDefaultItem() {
		return Items.FIREWORK_ROCKET;
	}

	@Override
	protected double getDefaultGravity() {
		return 0.04;
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.level().isClientSide() && this.tickCount > 160) {
			this.detonate();
		}
	}

	@Override
	protected void onHit(HitResult result) {
		super.onHit(result);
		this.detonate();
	}

	private void detonate() {
		if (this.level().isClientSide() || this.isRemoved()) {
			return;
		}

		this.level().explode(
				this,
				this.getX(),
				this.getY(),
				this.getZ(),
				this.explosionPower,
				true,
				Level.ExplosionInteraction.TNT
		);
		this.discard();
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput valueOutput) {
		super.addAdditionalSaveData(valueOutput);
		valueOutput.putFloat("explosion_power", this.explosionPower);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput valueInput) {
		super.readAdditionalSaveData(valueInput);
		this.explosionPower = valueInput.getFloatOr("explosion_power", StalinEntity.EXPLOSION_POWER);
	}
}

package dev.kirill.hbk.entity;

import dev.kirill.hbk.registry.ModEntityTypes;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Small white projectile fired by the Attacking Member. */
public class AttackingMemberBulletEntity extends ThrowableItemProjectile {
	private static final byte IMPACT_EVENT = 3;
	private static final float DEFAULT_DAMAGE = 10.0f;
	private float damage = DEFAULT_DAMAGE;

	public AttackingMemberBulletEntity(EntityType<? extends AttackingMemberBulletEntity> type, Level level) {
		super(type, level);
	}

	public AttackingMemberBulletEntity(Level level, LivingEntity owner, float damage) {
		super(ModEntityTypes.ATTACKING_MEMBER_BULLET, owner, level, new ItemStack(Items.SNOWBALL));
		this.damage = damage;
	}

	@Override
	protected Item getDefaultItem() {
		return Items.SNOWBALL;
	}

	@Override
	protected double getDefaultGravity() {
		return 0.015;
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.level().isClientSide() && this.tickCount > 100) {
			this.discard();
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult result) {
		super.onHitEntity(result);
		if (this.level() instanceof ServerLevel level) {
			Entity owner = this.getOwner();
			result.getEntity().hurtServer(level, level.damageSources().thrown(this, owner), this.damage);
		}
	}

	@Override
	protected void onHit(HitResult result) {
		super.onHit(result);
		if (!this.level().isClientSide()) {
			this.level().broadcastEntityEvent(this, IMPACT_EVENT);
			this.discard();
		}
	}

	@Override
	public void handleEntityEvent(byte status) {
		if (status != IMPACT_EVENT) {
			super.handleEntityEvent(status);
			return;
		}

		ItemParticleOption particle = new ItemParticleOption(ParticleTypes.ITEM, Items.SNOWBALL);
		for (int i = 0; i < 8; i++) {
			this.level().addParticle(particle, this.getX(), this.getY(), this.getZ(),
					(this.random.nextDouble() - 0.5) * 0.12,
					(this.random.nextDouble() - 0.5) * 0.12,
					(this.random.nextDouble() - 0.5) * 0.12);
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putFloat("damage", this.damage);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.damage = input.getFloatOr("damage", DEFAULT_DAMAGE);
	}
}

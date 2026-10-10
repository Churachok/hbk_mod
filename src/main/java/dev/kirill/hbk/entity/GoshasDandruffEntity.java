package dev.kirill.hbk.entity;

import dev.kirill.hbk.registry.ModEntityTypes;
import dev.kirill.hbk.registry.ModItems;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public final class GoshasDandruffEntity extends ThrowableItemProjectile {
	public static final float DAMAGE = 5.0f;

	public GoshasDandruffEntity(EntityType<? extends GoshasDandruffEntity> type, Level level) {
		super(type, level);
	}

	public GoshasDandruffEntity(Level level, LivingEntity owner) {
		super(ModEntityTypes.GOSHAS_DANDRUFF, owner, level, new ItemStack(ModItems.GOSHAS_DANDRUFF));
	}

	@Override
	protected Item getDefaultItem() {
		return ModItems.GOSHAS_DANDRUFF;
	}

	@Override
	protected void onHitEntity(EntityHitResult result) {
		super.onHitEntity(result);
		if (this.level() instanceof ServerLevel level) {
			result.getEntity().hurtServer(level, level.damageSources().thrown(this, getOwner()), DAMAGE);
		}
	}

	@Override
	protected void onHit(HitResult result) {
		super.onHit(result);
		if (!this.level().isClientSide()) {
			this.level().broadcastEntityEvent(this, (byte) 3);
			this.discard();
		}
	}

	@Override
	public void handleEntityEvent(byte event) {
		if (event != 3) {
			super.handleEntityEvent(event);
			return;
		}
		var particle = new ItemParticleOption(ParticleTypes.ITEM, ModItems.GOSHAS_DANDRUFF);
		for (int i = 0; i < 8; i++) {
			this.level().addParticle(particle, getX(), getY(), getZ(),
					(random.nextDouble() - 0.5) * 0.1, (random.nextDouble() - 0.5) * 0.1,
					(random.nextDouble() - 0.5) * 0.1);
		}
	}
}

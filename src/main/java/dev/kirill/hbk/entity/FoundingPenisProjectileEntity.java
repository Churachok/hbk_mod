package dev.kirill.hbk.entity;

import dev.kirill.hbk.item.MemberDestruction;
import dev.kirill.hbk.mechanic.ProgenitorTransformation;
import dev.kirill.hbk.registry.ModEntityTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/** A large white shot that detonates with three times TNT's explosion power. */
public final class FoundingPenisProjectileEntity extends ThrowableItemProjectile {
	public FoundingPenisProjectileEntity(EntityType<? extends FoundingPenisProjectileEntity> type, Level level) {
		super(type, level);
	}

	public FoundingPenisProjectileEntity(Level level, LivingEntity owner) {
		super(ModEntityTypes.FOUNDING_PENIS_PROJECTILE, owner, level, new ItemStack(Items.SNOWBALL));
	}

	@Override
	protected Item getDefaultItem() {
		return Items.SNOWBALL;
	}

	@Override
	protected double getDefaultGravity() {
		return 0.0;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level() instanceof ServerLevel level && this.tickCount > 100) {
			detonate(level);
		} else if (this.level().isClientSide() && this.tickCount % 2 == 0) {
			this.level().addParticle(ParticleTypes.CLOUD, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
		}
	}

	@Override
	protected void onHit(HitResult result) {
		super.onHit(result);
		if (this.level() instanceof ServerLevel level) {
			detonate(level);
		}
	}

	private void detonate(ServerLevel level) {
		if (!this.isRemoved()) {
			if (this.getOwner() instanceof Player player) {
				MemberDestruction.explode(level, player, this.position(), ProgenitorTransformation.PROJECTILE_EXPLOSION_POWER);
			}
			this.discard();
		}
	}
}

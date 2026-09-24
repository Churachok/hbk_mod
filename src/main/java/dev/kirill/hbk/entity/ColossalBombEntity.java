package dev.kirill.hbk.entity;

import dev.kirill.hbk.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** A colossal member planted by Kirill that warns for ten seconds before exploding. */
public final class ColossalBombEntity extends ThrowableItemProjectile {
	public static final int FUSE_TICKS = 20 * 10;
	public static final float EXPLOSION_POWER = 8.0f;
	private int fuse;

	public ColossalBombEntity(EntityType<? extends ColossalBombEntity> type, Level level) {
		super(type, level);
		this.setNoGravity(true);
	}

	@Override
	protected Item getDefaultItem() {
		return ModItems.COLOSSAL_MEMBER;
	}

	@Override
	public void tick() {
		this.baseTick();
		if (!(this.level() instanceof ServerLevel level)) {
			return;
		}
		this.setDeltaMovement(0.0, 0.0, 0.0);
		this.fuse++;
		if (this.fuse % 20 == 0) {
			float pitch = 0.55f + 1.25f * this.fuse / FUSE_TICKS;
			this.playSound(SoundEvents.NOTE_BLOCK_HAT.value(), 2.0f, pitch);
			level.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 4.0, this.getZ(),
					8, 0.7, 2.4, 0.7, 0.02);
		}
		if (this.fuse >= FUSE_TICKS) {
			this.detonate(level);
		}
	}

	private void detonate(ServerLevel level) {
		ExplosionDamageCalculator calculator = new ExplosionDamageCalculator() {
			@Override
			public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
				return !(entity instanceof KirillDoomEntity) && !(entity instanceof ColossalBombEntity)
						&& super.shouldDamageEntity(explosion, entity);
			}
		};
		level.explode(this, this.damageSources().explosion(this, this), calculator,
				this.position(), EXPLOSION_POWER, false, Level.ExplosionInteraction.TNT);
		this.discard();
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("fuse", this.fuse);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.fuse = input.getIntOr("fuse", 0);
	}
}
